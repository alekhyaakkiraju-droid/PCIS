# PCIS React SPA — nginx static host with runtime API/IdP reverse proxy in Helm.
FROM node:22-alpine AS build
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./

ARG VITE_OIDC_AUTHORITY
ARG VITE_OIDC_CLIENT_ID=pcis-spa
ARG VITE_OIDC_SCOPE=openid profile email roles
ARG VITE_API_BASE_URL=/api
ARG VITE_AUTH_MODE=bearer

ENV VITE_OIDC_AUTHORITY=$VITE_OIDC_AUTHORITY \
    VITE_OIDC_CLIENT_ID=$VITE_OIDC_CLIENT_ID \
    VITE_OIDC_SCOPE=$VITE_OIDC_SCOPE \
    VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_AUTH_MODE=$VITE_AUTH_MODE

RUN npx vite build

FROM nginx:1.27-alpine
COPY frontend/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/health || exit 1
