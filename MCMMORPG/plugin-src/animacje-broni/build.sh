#!/usr/bin/env bash
# Build AnimacjiBroni w kontenerze (host nie ma JDK/Gradle). Jary world1 tylko do odczytu.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
rm -f "$here"/build/libs/*.jar
rev="$(date +%Y%m%d%H%M)"
mkdir -p "$here/.gradle-home"
exec docker run --rm --name "build-animacje-broni-$$" --memory 2g -u "$(id -u):$(id -g)" \
  -v "$here:$here" -v /opt/docker/mc/data/world1/plugins:/serverplugins:ro \
  -v "$here/.gradle-home:/gradle-home" -w "$here" -e GRADLE_USER_HOME=/gradle-home \
  gradle:8.12-jdk21 gradle --no-daemon -q -PabRev="$rev" "${@:-build}"
