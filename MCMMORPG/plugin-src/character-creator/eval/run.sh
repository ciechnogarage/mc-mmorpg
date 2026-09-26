#!/usr/bin/env bash
# Evale kreatora postaci — bot protokolowy przeciwko ZYWEMU world1.
#
# Bot wchodzi wprost na world1, sam podpisujac dane Velocity modern forwarding
# (lib/velocity.js) i odpowiadajac na zadanie resource packa (lib/resourcepack.js).
# Bez tych dwoch rzeczy nie da sie wejsc: pierwsze konczy sie kickiem, drugie
# zawieszeniem w fazie configuration.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
secret="$(grep -A4 'velocity:' /opt/docker/mc/data/world1/config/paper-global.yml \
          | grep 'secret:' | awk '{print $2}')"
[ -n "$secret" ] || { echo "BLAD: nie znalazlem sekretu forwardingu"; exit 2; }
# skins.yml montujemy osobno (/res): scenariusz 70 porownuje skorke gracza z tym, co
# plugin ma w niej zapisane, zamiast sprawdzac sam ze soba.
exec docker run --rm --network mc_mmorpg-net -v "$here:/e" \
  -v "$here/../src/main/resources:/res:ro" -w /e \
  -e MC_FORWARDING_SECRET="$secret" -e MC_HOST="${MC_HOST:-world1}" \
  node:20 node run.js "${1:-}"
