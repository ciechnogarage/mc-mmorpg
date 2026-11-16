#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT"
java tools/BuildCharacterStage.java .
java --class-path build/libs/character-stage-0.1.0-SNAPSHOT.jar tools/RunCharacterStageResolverChecks.java
