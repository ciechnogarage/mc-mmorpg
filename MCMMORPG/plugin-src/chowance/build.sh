#!/usr/bin/env bash
# Build Chowance w kontenerze (host nie ma JDK/Gradle — memory mc-build-w-kontenerach).
# Jary pluginow world1 montowane TYLKO DO ODCZYTU.
set -euo pipefail
rm -f "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"/build/libs/*.jar
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
rev="$(date +%Y%m%d%H%M)"
mkdir -p "$here/.gradle-home"
exec docker run --rm --name "build-chowance-$$" --memory 2g -u "$(id -u):$(id -g)" \
  -v "$here:$here" -v /opt/docker/mc/data/world1/plugins:/serverplugins:ro \
  -v "$here/.gradle-home:/gradle-home" -w "$here" -e GRADLE_USER_HOME=/gradle-home \
  gradle:8.12-jdk21 gradle --no-daemon -q -PchwRev="$rev" "${@:-build}"
