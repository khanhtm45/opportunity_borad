# FE for DigitalOcean App Platform (Dockerfile service, port $PORT)
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL=
# Default "/" for DO; empty build-arg must NOT fall back to GitHub Pages path
ARG VITE_BASE_PATH=/
ENV VITE_API_URL=$VITE_API_URL
RUN VITE_BASE_PATH="${VITE_BASE_PATH:-/}" npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.sh /docker-entrypoint-ob.sh
COPY --from=build /app/dist /usr/share/nginx/html
RUN chmod +x /docker-entrypoint-ob.sh
ENV PORT=8080
EXPOSE 8080
# Explicit process for App Platform ("default process" / run command)
ENTRYPOINT ["/docker-entrypoint-ob.sh"]
