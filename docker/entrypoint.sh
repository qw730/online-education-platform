#!/bin/sh
# Starts the Spring Boot backend in the background and Nginx in the foreground,
# so the container lives as long as the web server does and both logs interleave
# onto stdout/stderr for `docker logs`.
set -eu

echo "[entrypoint] starting backend: DB_URL=${DB_URL:-<default>}"

java ${JAVA_OPTS:-} -jar /app/app.jar &

BACKEND_PID=$!

# Forward termination to both processes.
trap 'kill -TERM "$BACKEND_PID" 2>/dev/null || true; nginx -s quit 2>/dev/null || true' INT TERM

# Give the backend a head start; Nginx answers 502 until it is listening.
nginx -g 'daemon off;' &
NGINX_PID=$!

wait "$NGINX_PID"
