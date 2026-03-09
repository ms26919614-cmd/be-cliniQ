#!/bin/sh
# Maven Wrapper script for Unix/macOS/Linux
# Downloads Maven if not present, then runs it

set -e

MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"
MAVEN_HOME="${HOME}/.m2/wrapper"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    DIST_URL=$(grep 'distributionUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
else
    echo "Error: $MAVEN_WRAPPER_PROPERTIES not found"
    exit 1
fi

MAVEN_VERSION=$(echo "$DIST_URL" | grep -o 'apache-maven-[0-9.]*' | head -1)
MAVEN_DIR="${MAVEN_HOME}/${MAVEN_VERSION}"

if [ ! -d "$MAVEN_DIR" ]; then
    echo "Downloading Maven: $DIST_URL"
    mkdir -p "$MAVEN_HOME"
    TEMP_FILE=$(mktemp)
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL "$DIST_URL" -o "$TEMP_FILE"
    elif command -v wget > /dev/null 2>&1; then
        wget -q "$DIST_URL" -O "$TEMP_FILE"
    else
        echo "Error: curl or wget required"
        exit 1
    fi
    unzip -qo "$TEMP_FILE" -d "$MAVEN_HOME"
    rm -f "$TEMP_FILE"
fi

exec "$MAVEN_DIR/bin/mvn" "$@"
