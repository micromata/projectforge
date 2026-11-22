#!/usr/bin/env bash
# Deletes the old binaries (assets only; releases, tags and plugin assets stay). Dry run unless called with "run".
set -euo pipefail
REPO=micromata/projectforge
KEEP_TAGS=(9.2.13-RELEASE 9.2.11-RELEASE 9.1.0-RELEASE 9.0.0-RELEASE 8.1-RELEASE 8.0-RELEASE 7.5-RELEASE 6.25.0-RELEASE)
mode="${1:-dry}"
gh api --paginate "repos/$REPO/releases" \
  --jq '.[] | select(.tag_name | startswith("plugin-") | not) | .tag_name as $t | .assets[] | "\($t)\t\(.name)\t\(.size)"' |
while IFS=$'\t' read -r tag name size; do
  for k in "${KEEP_TAGS[@]}"; do [[ "$tag" == "$k" ]] && continue 2; done
  echo "delete $tag / $name ($((size / 1048576)) MB)"
  [[ "$mode" == run ]] && gh release delete-asset "$tag" "$name" --repo "$REPO" --yes
done
echo "Done ($mode)."
