#!/bin/bash
# Wdrozenie 2026-10-18: zagroda mlodych zamiast okna startera Chowancow (galaz starter-zagroda, repo tools/ui-naprawa/src/chowance).
#  - jar 202610180202: brak okna po wejsciu; Strazniczka Ogrodu daje Pieczec Opiekuna; 5 mlodych na trawniku
#    23.5..33.5 / 101 / -53.5 (stolica_odnowa); klik mlodego = opis, z pieczecia = zwiazanie (zawsze udane)
#  - Chowance/config.yml: sekcja zagroda (latka_configu.py), DeluxeMenus chowance_menazeria: przycisk
#    "Odbierz pierwszego chowanca" (latka_menazerii.py) — obie latki zmieniaja tylko swoje fragmenty
# Przetestowane klientem QA na klonie chw-zagroda 18.10. Czeka na zero graczy. Cofniecie: cofnij.sh
set -u
W=/opt/docker/mc/data/world1; B=/opt/docker/mc/backups/chowance-zagroda-2026-10-18; P=$B/pliki
C=/opt/docker/mc/docker-compose.yml
log() { echo "$(date '+%F %T') $*"; }
JAR=$P/chowance-0.1.0-202610180202.jar; [ -f "$JAR" ] || { log "BLAD: brak jara"; exit 1; }
OCZEKIWANY=46b75813df8befd6c68d6020043fad0b   # chowance-0.1.0-202610121416 (ui-naprawa runda 2), stan z 18.10
for i in $(seq 1 1440); do
  L=$(docker exec mc-world1-1 rcon-cli "minecraft:list uuids" 2>/dev/null)
  echo "$L" | grep -q "There are 0 of" && break
  [ $((i % 30)) = 1 ] && log "czekam na zero graczy: $L"
  sleep 30
done
echo "$L" | grep -q "There are 0 of" || { log "BLAD: serwer nie opustoszal"; exit 2; }

# kontrola tuz przed podmiana: jar na serwerze to ten, na ktorym budowano (inaczej cofnelibysmy cudza galaz)
TERAZ=$(ls $W/plugins/chowance-0.1.0-*.jar)
[ "$(echo "$TERAZ" | wc -l)" = 1 ] && [ "$(md5sum < "$TERAZ" | cut -d' ' -f1)" = "$OCZEKIWANY" ] \
  || { log "BLAD: na serwerze inny jar Chowance ($TERAZ) — sprawdz, czy ktos nie wdrozyl nowszej galezi"; exit 4; }
mkdir -p $B/przed
cp -n "$TERAZ" $W/plugins/Chowance/config.yml $W/plugins/DeluxeMenus/gui_menus/chowance_menazeria.yml $B/przed/

docker compose -f $C stop world1 || { log "BLAD stop"; exit 3; }
python3 $P/latka_configu.py $W/plugins/Chowance/config.yml || { log "BLAD latki configu"; docker compose -f $C start world1; exit 5; }
python3 $P/latka_menazerii.py $W/plugins/DeluxeMenus/gui_menus/chowance_menazeria.yml || { log "BLAD latki menu"; cp $B/przed/config.yml $W/plugins/Chowance/config.yml; docker compose -f $C start world1; exit 5; }
rm -f $W/plugins/chowance-0.1.0-*.jar $W/plugins/.paper-remapped/chowance-0.1.0-*.jar && cp $JAR $W/plugins/
I0=$(stat -c %i $W/logs/latest.log)
docker compose -f $C start world1
ok=0; for i in $(seq 1 150); do I1=$(stat -c %i $W/logs/latest.log 2>/dev/null || echo 0); if [ "$I1" != "$I0" ] && grep -q 'Done (' $W/logs/latest.log; then ok=1; break; fi; sleep 4; done
[ $ok = 1 ] || { log "BLAD: world1 nie wstal"; exit 6; }
grep -aE "Enabling Chowance|Chowańce włączone|\[Chowance\].*(WARN|SEVERE)|pl\.mcmmorpg\.chowance.*Exception|DeluxeMenus.*chowance_menazeria" $W/logs/latest.log | cut -c1-220
log "wdrozono — sprawdz w grze kontem QA: Ogrod Chowancow w Stolicy, 5 mlodych przed Strazniczka"
