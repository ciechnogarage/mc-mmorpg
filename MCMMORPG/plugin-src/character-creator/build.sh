#!/usr/bin/env bash
# Build kreatora postaci w kontenerze (na hoscie nie ma ani JDK, ani Gradle'a).
#
# Rewizja gita jest liczona TU, na hoscie, i wstrzykiwana do builda: w obrazie gradle'a
# nie ma gita, a to wlasnie ta rewizja odpowiada na pytanie "ktore zrodla stoja na produkcji".
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(git -C "$here" rev-parse --show-toplevel 2>/dev/null || echo "$here")"
git_rev="$(git -C "$here" rev-parse --short HEAD 2>/dev/null || echo nogit)"
# "Dirty" obejmuje TEZ pliki nieśledzone: dopoki zrodla kreatora nie sa zacommitowane,
# sam skrot commita NIE odpowiada na pytanie "ktore zrodla stoja na produkcji" — i wersja
# ma o tym mowic wprost, zamiast udawac powtarzalny build.
if [ -n "$(git -C "$here" status --porcelain -- "$here" 2>/dev/null)" ]; then
  git_rev="${git_rev}.dirty"
fi

exec docker run --rm --name "build-character-creator-$$" --memory 2g \
  -u "$(id -u):$(id -g)" \
  -v "$repo_root:$repo_root" \
  -v "$here/.gradle-home:/gradle-home" \
  -w "$here" \
  -e GRADLE_USER_HOME=/gradle-home \
  gradle:8.12-jdk21 \
  gradle --no-daemon -PccGitRev="$git_rev" "${@:-build}"
