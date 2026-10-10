#!/usr/bin/env bash
#
# Writes _data/binaries.json for the release pills of the changelog page (_includes/release-pills.html): the
# GitHub releases with a download (tag → url of the release) and the docker image tags on Docker Hub. A tag
# gets them only later by `pfDev.sh publish` and `docker/push-release.sh`, so they are fetched on every build
# (.github/workflows/github-pages.yml). Docker Hub doesn't allow requests from the browser (no CORS).
#
# If a request fails, no file is written: the page then shows only the pills from the changelog.
#
set -euo pipefail

cd "$(dirname "$0")"
OUT=_data/binaries.json
GITHUB_URL="https://api.github.com/repos/micromata/projectforge/releases?per_page=100"
DOCKER_URL="https://hub.docker.com/v2/repositories/micromata/projectforge/tags?page_size=100"

# All pages of a GitHub list (next page in the Link header), one JSON array per page.
github_pages() {
  local url="$1" headers
  headers="$(mktemp)"
  while [[ -n "$url" ]]; do
    curl -fsS --retry 2 -D "$headers" ${GITHUB_TOKEN:+-H "Authorization: Bearer $GITHUB_TOKEN"} "$url"
    url="$(grep -i '^link:' "$headers" | grep -o '<[^>]*>; rel="next"' | sed 's/^<\(.*\)>.*/\1/' || true)"
  done
  rm -f "$headers"
}

# All pages of a Docker Hub list ("next" in the body), one JSON object per page.
docker_pages() {
  local url="$1" page
  while [[ -n "$url" && "$url" != null ]]; do
    page="$(curl -fsS --retry 2 "$url")"
    echo "$page"
    url="$(jq -r '.next' <<<"$page")"
  done
}

if ! jar="$(github_pages "$GITHUB_URL" | jq -s 'add | map(select(.draft | not) | select(.assets | length > 0)
    | {key: .tag_name, value: .html_url}) | from_entries')"; then
  echo "GitHub releases not available, $OUT not written." >&2
  exit 0
fi
if ! docker="$(docker_pages "$DOCKER_URL" | jq -s 'map(.results[].name) | sort')"; then
  echo "Docker Hub tags not available, $OUT not written." >&2
  exit 0
fi
jq -n --argjson jar "$jar" --argjson docker "$docker" '{jar: $jar, docker: $docker}' >"$OUT"
echo "Wrote $OUT: $(jq '.jar | length' "$OUT") GitHub releases, $(jq '.docker | length' "$OUT") docker tags."
