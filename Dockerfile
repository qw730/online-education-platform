# Single image: Nginx serves the Vue bundle on :80 and reverse proxies /api to the
# Spring Boot process running behind it on :8080.

# ---- Stage 1: build the Vue bundle -------------------------------------------
FROM node:20-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---- Stage 2: build the Spring Boot jar --------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app
# Resolve dependencies in their own layer so source edits do not bust the cache.
COPY backend/pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY backend/src ./src
RUN mvn -B -ntp package -DskipTests

# ---- Stage 3: runtime --------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    # SQLite needs no separate database server, so the image is self-contained.
    DB_URL="jdbc:sqlite:/data/oep.db" \
    DB_DRIVER=org.sqlite.JDBC \
    DB_USERNAME= \
    DB_PASSWORD= \
    JPA_DDL_AUTO=update \
    JPA_DIALECT=org.hibernate.community.dialect.SQLiteDialect \
    INIT_DEMO_DATA=true

RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /data \
    && ln -sf /dev/stdout /var/log/nginx/access.log \
    && ln -sf /dev/stderr /var/log/nginx/error.log

WORKDIR /app
COPY --from=backend /app/target/online-education-platform.jar /app/app.jar
COPY --from=frontend /app/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/sites-available/default
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh && chown -R www-data:www-data /data

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://127.0.0.1/api/health || exit 1

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
