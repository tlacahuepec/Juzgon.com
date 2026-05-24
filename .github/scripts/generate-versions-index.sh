#!/usr/bin/env bash
set -euo pipefail

# Generate versions.json for GitHub Pages version index.
# Usage: generate-versions-index.sh <version> <output-dir> [existing-versions.json]
#
# Produces a versions.json that lists all published stable versions with
# their url, channel, and release date.

VERSION="${1:?Usage: generate-versions-index.sh <version> <output-dir> [existing-versions.json]}"
OUTPUT_DIR="${2:?Usage: generate-versions-index.sh <version> <output-dir> [existing-versions.json]}"
EXISTING="${3:-}"

RELEASED_AT=$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")
BASE_URL="/Juzgon.com"

NEW_ENTRY=$(cat <<EOF
{
  "version": "${VERSION}",
  "url": "${BASE_URL}/${VERSION}/",
  "channel": "stable",
  "releasedAt": "${RELEASED_AT}"
}
EOF
)

if [ -n "$EXISTING" ] && [ -f "$EXISTING" ]; then
  # Read existing versions array, remove any duplicate of this version, then prepend new entry
  EXISTING_VERSIONS=$(cat "$EXISTING")
  UPDATED=$(echo "$EXISTING_VERSIONS" | python3 -c "
import json, sys
data = json.load(sys.stdin)
new_entry = json.loads('${NEW_ENTRY}')
versions = [v for v in data.get('versions', []) if v.get('version') != '${VERSION}']
versions.insert(0, new_entry)
output = {'latest': '${VERSION}', 'versions': versions}
print(json.dumps(output, indent=2))
")
else
  UPDATED=$(cat <<EOF
{
  "latest": "${VERSION}",
  "versions": [
    ${NEW_ENTRY}
  ]
}
EOF
)
fi

mkdir -p "$OUTPUT_DIR"
echo "$UPDATED" > "$OUTPUT_DIR/versions.json"
echo "Generated versions.json with latest=${VERSION}"
