#!/usr/bin/env bash
# Smoke-test a Penpot frontend image on its own, with a stub backend.
#
# Usage: smoke-frontend.sh <image> [label]
#
# Starts a throwaway network with a busybox stub that answers /readyz under
# the names "penpot-backend" and "penpot-exporter" (nginx resolves both at
# start-up), then the frontend image with its defaults (PENPOT_BACKEND_URI =
# http://penpot-backend:6060). Checks the static app, the wasm renderer, the
# config.js cache policy and that /readyz is proxied to the backend, then
# prints the idle memory of the nginx container. Exits non-zero on failure.
set -euo pipefail

IMAGE=${1:?usage: smoke-frontend.sh <image> [label]}
LABEL=${2:-$IMAGE}
# busybox 1.37 (multi-arch index digest, 2026-11-09).
STUB_IMAGE=${STUB_IMAGE:-busybox:1.37@sha256:a7dbc319b1438115a207e11930dc4bb3e643dcb3ec0bdd694bb36cd8fad289ec}
RUN_ID="ds-smoke-$$-$RANDOM"
NET="$RUN_ID-net"
FAILS=0

cleanup() {
  docker rm -f "$RUN_ID-frontend" "$RUN_ID-backend" >/dev/null 2>&1 || true
  docker network rm "$NET" >/dev/null 2>&1 || true
}
trap cleanup EXIT

check() { # check <name> <command...>
  local name=$1; shift
  if "$@"; then
    echo "  PASS  $name"
  else
    echo "  FAIL  $name"
    FAILS=$((FAILS + 1))
  fi
}

docker network create "$NET" >/dev/null
docker run -d --name "$RUN_ID-backend" --network "$NET" \
  --network-alias penpot-backend --network-alias penpot-exporter \
  "$STUB_IMAGE" sh -c 'mkdir -p /www && printf OK > /www/readyz && exec httpd -f -p 6060 -h /www' >/dev/null
docker run -d --name "$RUN_ID-frontend" --network "$NET" -p 127.0.0.1::8080 "$IMAGE" >/dev/null

PORT=$(docker port "$RUN_ID-frontend" 8080/tcp | head -1 | sed 's/.*://')
BASE="http://127.0.0.1:${PORT}"

for _ in $(seq 1 60); do
  if curl -fs -o /dev/null "$BASE/"; then break; fi
  if [ "$(docker inspect -f '{{.State.Running}}' "$RUN_ID-frontend")" != true ]; then break; fi
  sleep 1
done

echo "== smoke: $LABEL ($IMAGE) on $BASE"
TMP=$(mktemp -d)

check "GET / is 200 and serves the app shell" \
  bash -c "curl -fsS '$BASE/' -o '$TMP/index.html' && grep -q 'globalThis.penpotVersion' '$TMP/index.html' && grep -q 'js/main.js' '$TMP/index.html'"
check "GET /js/config.js is 200 with no-store" \
  bash -c "curl -fsS -D '$TMP/config.h' -o /dev/null '$BASE/js/config.js' && grep -qi '^cache-control: no-store' '$TMP/config.h'"
check "GET /js/main.js is 200" \
  bash -c "curl -fsS -o /dev/null '$BASE/js/main.js'"
check "GET /js/render-wasm.wasm is 200 application/wasm" \
  bash -c "curl -fsS -D '$TMP/wasm.h' -o /dev/null '$BASE/js/render-wasm.wasm' && grep -qi '^content-type: application/wasm' '$TMP/wasm.h'"
check "GET /js/worker/main.js is 200" \
  bash -c "curl -fsS -o /dev/null '$BASE/js/worker/main.js'"
check "GET /plugins/mcp/manifest.json is 200 (MCP plugin bundled)" \
  bash -c "curl -fsS -o /dev/null '$BASE/plugins/mcp/manifest.json'"
check "GET /readyz is proxied to the backend (200 OK)" \
  bash -c "test \"\$(curl -fsS '$BASE/readyz')\" = OK"
check "SPA fallback: GET /dashboard returns index.html" \
  bash -c "curl -fsS '$BASE/dashboard' -o '$TMP/spa.html' && grep -q 'globalThis.penpotVersion' '$TMP/spa.html'"
check "nginx config test passes in the container" \
  docker exec "$RUN_ID-frontend" nginx -t -q

VERSION=$(curl -fsS "$BASE/version.txt" 2>/dev/null || echo '?')
MEM=$(docker stats --no-stream --format '{{.MemUsage}}' "$RUN_ID-frontend" 2>/dev/null || echo '?')
echo "  INFO  version.txt=${VERSION}  idle memory=${MEM}"

if [ "$FAILS" -ne 0 ]; then
  echo "== smoke FAILED for $LABEL ($FAILS check(s)); container log tail:"
  docker logs --tail 40 "$RUN_ID-frontend" 2>&1 || true
  rm -rf "$TMP"
  exit 1
fi
rm -rf "$TMP"
echo "== smoke passed for $LABEL"
