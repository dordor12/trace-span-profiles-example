#!/bin/bash
# =============================================================================
# Build Local Jars Script
# =============================================================================
# Helper script to build local versions of pyroscope-java and otel-profiling-java
# for development and testing.
#
# Usage:
#   ./scripts/build-local.sh [options]
#
# Options:
#   --pyroscope-java PATH    Build pyroscope-java from the specified path
#   --otel-profiling PATH    Build otel-profiling-java from the specified path
#   --all                    Build both (requires paths in .env or as arguments)
#   --help                   Show this help message
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Load .env if exists
if [ -f .env ]; then
    source .env
fi

show_help() {
    head -20 "$0" | tail -18
    exit 0
}

build_pyroscope_java() {
    local path=$1
    if [ -z "$path" ]; then
        echo -e "${RED}Error: pyroscope-java path not specified${NC}"
        echo "Set PYROSCOPE_JAVA_PATH in .env or pass --pyroscope-java PATH"
        exit 1
    fi

    echo -e "${YELLOW}Building pyroscope-java from: $path${NC}"
    cd "$path"

    # Build the agent
    ./gradlew :agent:shadowJar -x test

    local jar_path="$path/agent/build/libs/pyroscope.jar"
    if [ -f "$jar_path" ]; then
        echo -e "${GREEN}Successfully built: $jar_path${NC}"
        echo ""
        echo "To use this jar, add to your .env:"
        echo "PYROSCOPE_AGENT_JAR=$jar_path"
    else
        echo -e "${RED}Build failed: jar not found at $jar_path${NC}"
        exit 1
    fi
}

build_otel_profiling() {
    local path=$1
    if [ -z "$path" ]; then
        echo -e "${RED}Error: otel-profiling-java path not specified${NC}"
        echo "Set OTEL_PROFILING_JAVA_PATH in .env or pass --otel-profiling PATH"
        exit 1
    fi

    echo -e "${YELLOW}Building otel-profiling-java from: $path${NC}"
    cd "$path"

    # Build the extension
    ./gradlew shadowJar -x test

    local jar_path="$path/build/libs/pyroscope-otel.jar"
    if [ -f "$jar_path" ]; then
        echo -e "${GREEN}Successfully built: $jar_path${NC}"
        echo ""
        echo "To use this jar, add to your .env:"
        echo "PYROSCOPE_OTEL_JAR=$jar_path"
    else
        echo -e "${RED}Build failed: jar not found at $jar_path${NC}"
        exit 1
    fi
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --pyroscope-java)
            PYROSCOPE_JAVA_PATH="$2"
            BUILD_PYROSCOPE=true
            shift 2
            ;;
        --otel-profiling)
            OTEL_PROFILING_JAVA_PATH="$2"
            BUILD_OTEL=true
            shift 2
            ;;
        --all)
            BUILD_PYROSCOPE=true
            BUILD_OTEL=true
            shift
            ;;
        --help|-h)
            show_help
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_help
            ;;
    esac
done

# Build requested projects
if [ "$BUILD_PYROSCOPE" = true ]; then
    build_pyroscope_java "$PYROSCOPE_JAVA_PATH"
fi

if [ "$BUILD_OTEL" = true ]; then
    build_otel_profiling "$OTEL_PROFILING_JAVA_PATH"
fi

if [ -z "$BUILD_PYROSCOPE" ] && [ -z "$BUILD_OTEL" ]; then
    echo "No build target specified. Use --help for usage."
    exit 1
fi

echo ""
echo -e "${GREEN}Done!${NC}"
echo "Run 'docker-compose up -d --build' to rebuild the containers with local jars."
