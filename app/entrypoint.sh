#!/bin/bash
# =============================================================================
# Entrypoint Script
# =============================================================================
# Handles selecting between local and published jars for development
# =============================================================================

set -e

# Determine which Pyroscope agent to use
if [ "$USE_LOCAL_PYROSCOPE_AGENT" = "true" ] && [ -f /agents/pyroscope-local.jar ]; then
    PYROSCOPE_JAR="/agents/pyroscope-local.jar"
    echo "Using LOCAL Pyroscope agent: $PYROSCOPE_JAR"
else
    PYROSCOPE_JAR="/agents/pyroscope.jar"
    echo "Using PUBLISHED Pyroscope agent: $PYROSCOPE_JAR"
fi

# Determine which pyroscope-otel extension to use
if [ "$USE_LOCAL_PYROSCOPE_OTEL" = "true" ] && [ -f /agents/pyroscope-otel-local.jar ]; then
    PYROSCOPE_OTEL_JAR="/agents/pyroscope-otel-local.jar"
    echo "Using LOCAL pyroscope-otel extension: $PYROSCOPE_OTEL_JAR"
elif [ -f /agents/pyroscope-otel.jar ]; then
    PYROSCOPE_OTEL_JAR="/agents/pyroscope-otel.jar"
    echo "Using PUBLISHED pyroscope-otel extension: $PYROSCOPE_OTEL_JAR"
else
    PYROSCOPE_OTEL_JAR=""
    echo "WARNING: No pyroscope-otel extension found, span correlation disabled"
fi

# Build Java command
JAVA_CMD="java"
JAVA_CMD="$JAVA_CMD -javaagent:$PYROSCOPE_JAR"
JAVA_CMD="$JAVA_CMD -javaagent:/agents/opentelemetry-javaagent.jar"

if [ -n "$PYROSCOPE_OTEL_JAR" ]; then
    JAVA_CMD="$JAVA_CMD -Dotel.javaagent.extensions=$PYROSCOPE_OTEL_JAR"
fi

JAVA_CMD="$JAVA_CMD -jar /app/app.jar"

echo "Starting application with: $JAVA_CMD"
exec $JAVA_CMD
