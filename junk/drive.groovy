// Jenkins Scripted Pipeline (Groovy) - Google Drive test via curl
// ✅ Runs inside node(){}
// ✅ Generates OAuth access token using Service Account JSON (JWT flow)
// ✅ Tests Drive access (list files + list folder contents)
// ✅ Uploads a test file to the folder

node {
  // ---- CONFIG: update these two values ----
  def SA_JSON = "/path/to/service-account.json"   // <-- service account json path on the Jenkins node
  def FOLDER_ID = "YOUR_FOLDER_ID"                // <-- Google Drive folder ID (shared with service account)

  stage("Prereq Check") {
    sh """
      set -e
      command -v curl >/dev/null
      command -v jq >/dev/null
      command -v python3 >/dev/null
      echo "OK: curl/jq/python3 present"
    """
  }

  stage("Get Access Token") {
    sh """
      set -e
      test -f "${SA_JSON}" || (echo "Service account JSON not found: ${SA_JSON}" && exit 1)

      # Create JWT using python (requires: pip install pyjwt OR python package available)
      JWT=\$(python3 - << 'EOF'
import json, time
try:
    import jwt
except Exception as e:
    raise SystemExit("Missing python module 'jwt' (pyjwt). Install it: pip3 install pyjwt")

sa_path = "${SA_JSON}"
with open(sa_path) as f:
    sa = json.load(f)

now = int(time.time())
payload = {
    "iss": sa["client_email"],
    "scope": "https://www.googleapis.com/auth/drive",
    "aud": "https://oauth2.googleapis.com/token",
    "iat": now,
    "exp": now + 3600
}

token = jwt.encode(payload, sa["private_key"], algorithm="RS256")
print(token)
EOF
)

      # Exchange JWT for OAuth access token
      ACCESS_TOKEN=\$(curl -s -X POST https://oauth2.googleapis.com/token \\
        -H "Content-Type: application/x-www-form-urlencoded" \\
        -d "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=\$JWT" \\
        | jq -r .access_token)

      if [ "\$ACCESS_TOKEN" = "null" ] || [ -z "\$ACCESS_TOKEN" ]; then
        echo "Failed to get access token. Full response:"
        curl -s -X POST https://oauth2.googleapis.com/token \\
          -H "Content-Type: application/x-www-form-urlencoded" \\
          -d "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=\$JWT"
        exit 1
      fi

      echo "\$ACCESS_TOKEN" > access_token.txt
      echo "✅ Access token generated"
    """
  }

  stage("Test: List Drive Files (Top 5)") {
    sh """
      set -e
      ACCESS_TOKEN=\$(cat access_token.txt)

      echo "Listing top 5 files visible to the service account..."
      curl -s -H "Authorization: Bearer \$ACCESS_TOKEN" \\
        "https://www.googleapis.com/drive/v3/files?pageSize=5&fields=files(id,name),nextPageToken" \\
        | jq
    """
  }

  stage("Test: List Folder Contents") {
    sh """
      set -e
      ACCESS_TOKEN=\$(cat access_token.txt)

      echo "Listing files inside folder: ${FOLDER_ID}"
      curl -s -H "Authorization: Bearer \$ACCESS_TOKEN" \\
        "https://www.googleapis.com/drive/v3/files?q='${FOLDER_ID}'+in+parents&fields=files(id,name,mimeType)" \\
        | jq
    """
  }

  stage("Test: Upload a File") {
    sh """
      set -e
      ACCESS_TOKEN=\$(cat access_token.txt)

      echo "jenkins drive test \$(date -u +%Y-%m-%dT%H:%M:%SZ)" > test.txt

      echo "Uploading test.txt to folder: ${FOLDER_ID}"
      curl -s -X POST "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart" \\
        -H "Authorization: Bearer \$ACCESS_TOKEN" \\
        -F "metadata={\\"name\\":\\"test.txt\\",\\"parents\\":[\\"${FOLDER_ID}\\"]};type=application/json;charset=UTF-8" \\
        -F "file=@test.txt" \\
        | jq
    """
  }

  stage("Done") {
    echo "✅ If you see test.txt in the Drive folder, permissions + auth are correct."
    echo "Tip: If folder listing errors with 403, share the folder with the service account email as Editor."
  }
}
