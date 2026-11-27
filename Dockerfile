# Kejafy API — multi-stage image
# ---------------------------------------------------------------------------
#   build → Maven builds the jar (dependencies cached in their own layer)
#   prod  → JRE only, runs as a non-root user
#
#   docker build --target prod -t kejafy/api:latest .
# ---------------------------------------------------------------------------

# ===========================================================================
# Stage 1 — Build the jar
# ===========================================================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /build

# Dependencies first: this layer is reused until pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -q dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests package \
    && cp target/KejaFy-*.jar app.jar


# ===========================================================================
# Stage 2 — Production runtime
# ===========================================================================
FROM eclipse-temurin:21-jre AS prod

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home /app kejafy

WORKDIR /app
COPY --from=build --chown=kejafy:kejafy /build/app.jar app.jar

USER kejafy
EXPOSE 8080

# Sized for a 1–2 GB server; override JAVA_OPTS in .env on a bigger one.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
