#!/usr/bin/env bash
# Build w kontenerze — na hoscie nie ma ani JDK, ani Gradle'a.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(git -C "$here" rev-parse --show-toplevel 2>/dev/null || echo "$here")"
git_rev="$(git -C "$here" rev-parse --short HEAD 2>/dev/null || echo nogit)"
if [ -n "$(git -C "$here" status --porcelain -- "$here" 2>/dev/null)" ]; then
  git_rev="${git_rev}.dirty"
fi
exec docker run --rm -u "$(id -u):$(id -g)" \
  -v "$repo_root:$repo_root" \
  -v /opt/docker/mc/data/world1/plugins:/srv-plugins:ro \
  -v "$here/.gradle-home:/gradle-home" \
  -w "$here" -e GRADLE_USER_HOME=/gradle-home \
  gradle:8.12-jdk21 \
  gradle --no-daemon -PmkGitRev="$git_rev" -PmkServerPlugins=/srv-plugins "${@:-build}"
