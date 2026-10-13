#!/usr/bin/env bash
#
# Writes _data/binaries.json for the release pills of the changelog page (_includes/release-pills.html): the
# GitHub releases with a download (tag → url of the release), the docker image tags on Docker Hub and the
# version folders on SourceForge with a jar (docker tag → url of the folder, both are named like 9.0.3 or 7.2).
# A tag gets them only later by `pfDev.sh publish`, `docker/push-release.sh` and the upload to SourceForge by
# hand, so they are fetched on every build (.github/workflows/github-pages.yml). Docker Hub doesn't allow
# requests from the browser (no CORS).
#
# If a request to GitHub or Docker Hub fails, no file is written: the page then shows only the pills from the
# changelog. If SourceForge fails, the file is written without its jars.
#
set -euo pipefail

cd "$(dirname "$0")"
OUT=_data/binaries.json
GITHUB_URL="https://api.github.com/repos/micromata/projectforge/releases?per_page=100"
DOCKER_URL="https://hub.docker.com/v2/repositories/micromata/projectforge/tags?page_size=100"
SOURCEFORGE_FILES="https://sourceforge.net/projects/pforge/files/ProjectForge"
SOURCEFORGE_RSS="https://sourceforge.net/projects/pforge/rss?path=/ProjectForge&limit=100"

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
# The newest files of the RSS feed (100 at most, the jars since 7.0 are among them), titled by their path, e.g.
# /ProjectForge/9.0.3/projectforge-application-9.0.3.jar.
if ! sourceforge="$(curl -fsS --retry 2 "$SOURCEFORGE_RSS" \
    | grep -o '<title><!\[CDATA\[/ProjectForge/[^/]*/projectforge-application-[^]]*\.jar' \
    | sed 's|.*/ProjectForge/\([^/]*\)/.*|\1|' | grep -vx 'Snapshots' \
    | jq -R -s --arg files "$SOURCEFORGE_FILES" 'split("\n") | map(select(length > 0))
      | map({key: ., value: "\($files)/\(.)/"}) | from_entries')"; then
  echo "SourceForge files not available, $OUT written without them." >&2
  sourceforge='{}'
fi
jq -n --argjson jar "$jar" --argjson docker "$docker" --argjson sourceforge "$sourceforge" \
  '{jar: $jar, docker: $docker, sourceforge: $sourceforge}' >"$OUT"
echo "Wrote $OUT: $(jq '.jar | length' "$OUT") GitHub releases, $(jq '.docker | length' "$OUT") docker tags," \
  "$(jq '.sourceforge | length' "$OUT") SourceForge jars."
