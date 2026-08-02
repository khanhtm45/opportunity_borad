#!/bin/sh
set -eu
PORT="${PORT:-8080}"
# BE app hostname (no scheme). Used so /api/* on this FE domain proxies to Spring.
API_PROXY_HOST="${API_PROXY_HOST:-oyster-app-eleoo.ondigitalocean.app}"

CONF=/etc/nginx/conf.d/default.conf
sed -i "s/listen 8080;/listen ${PORT};/g" "$CONF"
sed -i "s/listen \[::\]:8080;/listen [::]:${PORT};/g" "$CONF"
sed -i "s/API_PROXY_HOST_PLACEHOLDER/${API_PROXY_HOST}/g" "$CONF"

exec nginx -g "daemon off;"
