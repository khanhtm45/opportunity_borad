#!/bin/sh
set -eu
PORT="${PORT:-8080}"
# DigitalOcean injects PORT — rewrite nginx listen directive
sed -i "s/listen 8080;/listen ${PORT};/g" /etc/nginx/conf.d/default.conf
sed -i "s/listen \[::\]:8080;/listen [::]:${PORT};/g" /etc/nginx/conf.d/default.conf
exec nginx -g "daemon off;"
