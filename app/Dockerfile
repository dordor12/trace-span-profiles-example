# =============================================================================
# Demo Application Dockerfile
# =============================================================================
# Builds a Spring Boot demo app with OTel and Pyroscope agents
# Supports both published Maven artifacts and locally built jars
# =============================================================================

# -----------------------------------------------------------------------------
# Build Stage
# -----------------------------------------------------------------------------
FROM maven:3.8-eclipse-temurin-11 AS build
WORKDIR /app

# Copy pom and source
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn package -DskipTests -B

# -----------------------------------------------------------------------------
# Runtime Stage
# -----------------------------------------------------------------------------
FROM eclipse-temurin:11-jdk

WORKDIR /app

# Install curl for health checks and downloading agents
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Arguments for agent versions
ARG OTEL_JAVA_AGENT_VERSION=2.12.0
ARG PYROSCOPE_AGENT_VERSION=2.1.2
ARG PYROSCOPE_OTEL_VERSION=1.0.4

# Create agents directory
RUN mkdir -p /agents

# Download OpenTelemetry Java Agent
RUN curl -L -f -o /agents/opentelemetry-javaagent.jar \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar"

# Download Pyroscope Java Agent
RUN curl -L -f -o /agents/pyroscope.jar \
    "https://repo1.maven.org/maven2/io/pyroscope/agent/${PYROSCOPE_AGENT_VERSION}/agent-${PYROSCOPE_AGENT_VERSION}.jar"

# Download otel-profiling-java extension (pyroscope-otel)
RUN curl -L -f -o /agents/pyroscope-otel.jar \
    "https://repo1.maven.org/maven2/io/pyroscope/otel/${PYROSCOPE_OTEL_VERSION}/otel-${PYROSCOPE_OTEL_VERSION}.jar"

# Copy the built application (Spring Boot repackaged jar)
COPY --from=build /app/target/app.jar /app/app.jar

# Copy entrypoint script
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Expose application port
EXPOSE 8080

# Use entrypoint script to handle local vs published jars
ENTRYPOINT ["/app/entrypoint.sh"]
