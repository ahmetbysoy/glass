#!/usr/bin/env bash
# Standard Gradle Wrapper Launcher Fix
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    echo "Gradle executable not found in PATH. Please use JDK 17+ and Gradle 8+."
    exit 1
fi
