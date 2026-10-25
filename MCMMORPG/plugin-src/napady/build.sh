#!/usr/bin/env bash
# Build Napady w kontenerze (host nie ma JDK/Gradle). paper-api z Mavena, MM/LD/packetevents/MMOItems/MythicLib z serwera (libs/).
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
plugins=/opt/docker/mc/data/world1/plugins
mkdir -p "$here/libs"
for j in MythicMobs-5.11.2.jar LibsDisguises-11.0.15-Free.jar packetevents-spigot-2.11.2.jar \
         MMOItems-6.10.1-20260330.094756-58.jar MythicLib-dist-1.7.1-20260330.110510-95.jar; do
  [ -f "$here/libs/$j" ] || cp "$plugins/$j" "$here/libs/"
done
rm -f "$here"/build/libs/*.jar
rev="$(date +%Y%m%d%H%M)"
mkdir -p "$here/.gradle-home"
exec docker run --rm --name "build-napady-$$" --memory 2g -u "$(id -u):$(id -g)" \
  -v "$here:$here" \
  -v "$here/.gradle-home:/gradle-home" -w "$here" -e GRADLE_USER_HOME=/gradle-home \
  gradle:8.12-jdk21 gradle --no-daemon -q -PpmRev="$rev" "${@:-build}"
