#!/bin/bash
# Wdrozenie HUD v3 (okno przygasa, kolumna: efekty, towarzysze, druzyna; cel pod mapa; plugin HudDane) na world1 (19.10). Uzycie: wdroz.sh [--sucho]
# Kopia + cofnij.sh, kontrola md5 bazy (wdrozenie-cofa-inne-galezie), 0 graczy (rcon-list-pusty), restart przez stop/start.
set -euo pipefail
T=/home/dev/Desktop/Przemek/MC/tools/hud/l3; H=/opt/docker/mc/data/world1/plugins; C=mc-world1-1
LOG=/opt/docker/mc/data/world1/logs/latest.log; SUCHO=${1:-}
cd $H; md5sum -c --quiet $T/baza.md5 || { echo "STOP: pliki bazowe zmienione od 18.10 - scal recznie"; exit 2; }
G=$(docker exec $C rcon-cli "minecraft:list uuids" | tr -d '\033' | sed 's/\[[0-9;]*m//g')
echo "$G" | grep -qE 'There are 0 of' || { echo "STOP: gracze online: $G"; exit 3; }
B=/opt/docker/mc/data/world1/backups/hud-l3-$(date -u +%Y%m%d-%H%M%S); mkdir -p $B/stare
while read -r f; do f=${f#./}; [ -e "$H/$f" ] && { mkdir -p "$B/stare/$(dirname "$f")"; cp -p "$H/$f" "$B/stare/$f"; }; done < $T/LISTA.txt
cat > $B/cofnij.sh <<EOC
#!/bin/bash
# Cofniecie HUD v3: przywraca stare pliki i usuwa nowe. Uruchamiac; sam zatrzyma i uruchomi world1.
set -e; H=$H; docker stop $C
while read -r f; do f=\${f#./}; if [ -e "$B/stare/\$f" ]; then cp -p "$B/stare/\$f" "\$H/\$f"; else rm -f "\$H/\$f"; fi; done < $T/LISTA.txt
true
docker start $C; echo "cofnieto HUD v3 - sprawdz Done w logu"
EOC
chmod +x $B/cofnij.sh; echo "kopia: $B ($(find $B/stare -type f | wc -l) starych plikow), cofnij: $B/cofnij.sh"
[ "$SUCHO" = --sucho ] && { echo "SUCHO: bez zmian na serwerze"; exit 0; }
I0=$(stat -c %i $LOG); docker stop $C >/dev/null
cp -r $T/pliki/. $H/
docker start $C >/dev/null
for i in $(seq 150); do I1=$(stat -c %i $LOG 2>/dev/null || echo 0); [ "$I1" != "$I0" ] && grep -q 'Done (' $LOG && break; sleep 4; done
grep -q 'Done (' $LOG || { echo "UWAGA: brak Done po 10 min - sprawdz log, cofnij: $B/cofnij.sh"; exit 4; }
E=$(grep -c 'Error occurred while enabling MythicHUD' $LOG || true); P=$(grep -c 'Injected MythicHUD Pack' $LOG || true)
echo "start OK; bledy MythicHUD: $E; paczka MythicHUD w Nexo: $P"; [ "$E" = 0 ] || { echo "BLAD MythicHUD - cofnij: $B/cofnij.sh"; exit 5; }
