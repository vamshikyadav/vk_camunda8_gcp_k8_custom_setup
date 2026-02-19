// Jenkins Scripted Pipeline - Upload artifact to Nexus (curl)
// Works for:
// 1) Nexus Repository Manager 3 (recommended): use "Raw" repo OR Maven repo
// 2) Nexus 2 / generic HTTP: still works if endpoint allows PUT/POST

node {
  // --------- CONFIG (edit these) ----------
  def NEXUS_BASE_URL = "https://nexus.yourcompany.com"   // no trailing slash
  def REPO_NAME      = "raw-hosted"                      // Nexus repo name (Raw hosted is easiest)
  def GROUP_PATH     = "my-app"                          // folder path inside repo (e.g., team/service)
  def VERSION        = env.BUILD_NUMBER                  // or "1.0.0"
  def FILE_PATH      = "artifacts/build.zip"             // file produced by your build
  def FILE_NAME      = "build.zip"                       // target name in Nexus (can be same as basename)
  // ----------------------------------------

  stage("Build / Prepare Artifact") {
    sh """
      set -e
      mkdir -p artifacts
      # Example artifact if you don't already have one:
      # echo "hello nexus" > ${FILE_PATH}
      test -f "${FILE_PATH}" || (echo "Artifact not found: ${FILE_PATH}" && exit 1)
      ls -lh "${FILE_PATH}"
    """
  }

  stage("Upload to Nexus (Raw repo via PUT)") {
    // Store Nexus creds in Jenkins:
    // Jenkins -> Manage Credentials -> add "Username with password"
    // ID: nexus-creds
    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
      sh """
        set -e

        UPLOAD_URL="${NEXUS_BASE_URL}/repository/${REPO_NAME}/${GROUP_PATH}/${VERSION}/${FILE_NAME}"

        echo "Uploading to: \$UPLOAD_URL"

        # --fail makes curl return non-zero on 4xx/5xx
        curl --fail -u "\$NEXUS_USER:\$NEXUS_PASS" \\
          --upload-file "${FILE_PATH}" \\
          "\$UPLOAD_URL"

        echo "✅ Upload complete"
      """
    }
  }

  stage("Verify (HEAD)") {
    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
      sh """
        set -e
        CHECK_URL="${NEXUS_BASE_URL}/repository/${REPO_NAME}/${GROUP_PATH}/${VERSION}/${FILE_NAME}"
        echo "Verifying: \$CHECK_URL"
        curl --fail -I -u "\$NEXUS_USER:\$NEXUS_PASS" "\$CHECK_URL" | head -n 20
      """
    }
  }
}
