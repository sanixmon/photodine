#!/bin/sh
# Minimal Gradle wrapper bootstrap for Photodine.
# Reads gradle/wrapper/gradle-wrapper.properties, downloads and caches the
# distribution under $GRADLE_USER_HOME/wrapper/dists, then execs Gradle.
# (No binary wrapper jar is committed; behavior is equivalent for CI use.)
set -e

APP_HOME="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROP_FILE="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
DISTRIBUTION_URL="$(sed -n 's/^distributionUrl=//p' "$PROP_FILE" | sed 's/\\:/:/g' | tr -d '\r')"
DIST_NAME="$(basename "$DISTRIBUTION_URL" .zip)"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_DIR="$GRADLE_USER_HOME/wrapper/dists/$DIST_NAME"
GRADLE_BIN="$DIST_DIR/$(echo "$DIST_NAME" | sed 's/-bin$//;s/-all$//')/bin/gradle"
if [ ! -x "$GRADLE_BIN" ]; then
    GRADLE_BIN="$(find "$DIST_DIR" -name gradle -type f -perm -111 2>/dev/null | head -n 1)"
fi

if [ -z "$GRADLE_BIN" ] || [ ! -x "$GRADLE_BIN" ]; then
    echo "Downloading $DISTRIBUTION_URL ..." >&2
    mkdir -p "$DIST_DIR"
    TMP_ZIP="$DIST_DIR/gradle.zip"
    if command -v curl >/dev/null 2>&1; then
        curl -fSL -o "$TMP_ZIP" "$DISTRIBUTION_URL"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$TMP_ZIP" "$DISTRIBUTION_URL"
    else
        echo "ERROR: need curl or wget to bootstrap Gradle." >&2
        exit 1
    fi
    if command -v unzip >/dev/null 2>&1; then
        unzip -q -o "$TMP_ZIP" -d "$DIST_DIR"
    else
        echo "ERROR: need unzip to bootstrap Gradle." >&2
        exit 1
    fi
    rm -f "$TMP_ZIP"
fi

if [ -z "$GRADLE_BIN" ] || [ ! -x "$GRADLE_BIN" ]; then
    GRADLE_DIR_NAME="$(echo "$DIST_NAME" | sed 's/-bin$//;s/-all$//')"
    GRADLE_BIN="$DIST_DIR/$GRADLE_DIR_NAME/bin/gradle"
fi

if [ ! -x "$GRADLE_BIN" ]; then
    GRADLE_BIN="$(find "$DIST_DIR" -name gradle -type f -perm -111 2>/dev/null | head -n 1)"
fi

if [ ! -x "$GRADLE_BIN" ] && command -v gradle >/dev/null 2>&1; then
    GRADLE_BIN="$(command -v gradle)"
fi

exec "$GRADLE_BIN" "$@"
