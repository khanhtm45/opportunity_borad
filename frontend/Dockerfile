# FE for DigitalOcean App Platform (Dockerfile service, port $PORT)
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# DO FE + custom domain: leave VITE_API_URL empty → browser calls /api/v1 on same host;
# nginx proxies /api → API_PROXY_HOST (see docker-entrypoint.sh).
# Optional: set VITE_API_URL to absolute API if not using the proxy.
ARG VITE_API_URL=
RUN VITE_API_URL="${VITE_API_URL}" VITE_BASE_PATH=/ npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.sh /docker-entrypoint-ob.sh
COPY --from=build /app/dist /usr/share/nginx/html
RUN chmod +x /docker-entrypoint-ob.sh
ENV PORT=8080 \
    API_PROXY_HOST=oyster-app-eleoo.ondigitalocean.app
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint-ob.sh"]

