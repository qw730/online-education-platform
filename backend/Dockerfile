# Backend only: Spring Boot executable jar.
# Built by docker-compose (services.backend.build) or on its own with:
#   docker build -t oep-backend -f backend/Dockerfile .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Dependency layer first so source edits do not bust the Maven cache.
COPY backend/pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY backend/src ./src
RUN mvn -B -ntp package -DskipTests \
    && mv target/online-education-platform.jar /app/app.jar

FROM eclipse-temurin:17-jre-jammy
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseG1GC"

# curl is needed by the container health check.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app

WORKDIR /app
COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=10 \
    CMD curl -fsS http://127.0.0.1:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
