# Frontend only: build the Vue bundle, serve it with Nginx.
# Built by docker-compose (services.frontend.build) or on its own with:
#   docker build -t oep-frontend -f frontend/Dockerfile .
FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine
ENV TZ=Asia/Shanghai
COPY docker/nginx-frontend.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=5s --retries=5 \
    CMD wget -qO /dev/null http://127.0.0.1/ || exit 1
