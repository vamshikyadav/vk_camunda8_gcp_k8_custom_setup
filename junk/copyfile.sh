#!/usr/bin/env bash

set -euo pipefail

# ==========================
# ARGUMENTS
# ==========================
SOURCE_DIR="$1"
DEST_DIR="$2"
SEARCH_STRING="$3"
DEST_FILENAME="$4"

# ==========================
# VALIDATION
# ==========================
if [[ $# -ne 4 ]]; then
  echo "Usage:"
  echo "  $0 <SOURCE_DIR> <DEST_DIR> <SEARCH_STRING> <DEST_FILENAME>"
  exit 1
fi

if [[ ! -d "$SOURCE_DIR" ]]; then
  echo "❌ Source directory does not exist: $SOURCE_DIR"
  exit 1
fi

mkdir -p "$DEST_DIR"

# ==========================
# LOGIC
# ==========================
echo "🔍 Source       : $SOURCE_DIR"
echo "📁 Destination  : $DEST_DIR"
echo "🔎 Search string: $SEARCH_STRING"
echo "✏️  Rename as   : $DEST_FILENAME"
echo "--------------------------------------"

find "$SOURCE_DIR" -type f | while read -r file; do
  if grep -q "$SEARCH_STRING" "$file"; then
    rel_path="${file#$SOURCE_DIR/}"
    rel_dir="$(dirname "$rel_path")"

    mkdir -p "$DEST_DIR/$rel_dir"
    cp "$file" "$DEST_DIR/$rel_dir/$DEST_FILENAME"

    echo "✅ Copied:"
    echo "   $file -> $DEST_DIR/$rel_dir/$DEST_FILENAME"
  fi
done

echo "🎉 Done"


# // ./copy_matching_files.sh \
# //   /path/to/source \
# //   /path/to/destination \
# //   "IMPORTANT_STRING" \
# //   renamed_file.txt

# // example:

# // ./copy_matching_files.sh \
# //   ./configs \
# //   ./filtered_configs \
# //   "enabled: true" \
# //   settings.yaml

