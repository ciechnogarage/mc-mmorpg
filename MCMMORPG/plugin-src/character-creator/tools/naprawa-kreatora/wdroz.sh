#!/bin/bash
# Wdrozenie naprawy kreatora (agent KREATOR, 2026-09-30, galaz cc-naprawa-kreator). NIE uruchamiac bez zgody
# wlasciciela. Czeka na ZERO graczy, potem: kopie -> stop world1 -> config (apply.py: names+blocked, flyover,
# fade 3/4/6) -> karta_kreatora.py (rola/trudnosc/umiejetnosc/cechy ras do CharacterCreator/config.yml;
# generuj_klasy.py NIE jest ruszany ani uruchamiany) -> jar -> start -> kontrola logu. Cofniecie: wycofaj.sh.
# Jesli wdrazany ma byc jar scalony z galezia WYBOR, podmien jar w tym katalogu (ma byc dokladnie jeden).
set -u
W=/opt/docker/mc/data/world1; B=/opt/docker/mc/backups/kreator-naprawa-2026-09-30/kreator
JAR=$(ls $B/character-creator-*.jar); C=/opt/docker/mc/docker-compose.yml
CFG=$W/plugins/CharacterCreator/config.yml; G=$W/tools/klasy-rasowe
log() { echo "$(date '+%F %T') $*"; }
[ "$(ls $B/character-creator-*.jar | wc -l)" = 1 ] || { log "BLAD: w pakiecie ma byc dokladnie jeden jar"; exit 1; }
log "generator klas na world1: $(md5sum $G/generuj_klasy.py | cut -c1-32) (przy skladaniu pakietu: 744411de4847d8d7ce34b2daddc88865; skrypt karty tylko go importuje)"
for i in $(seq 1 1440); do
  L=$(docker exec mc-world1-1 rcon-cli "minecraft:list uuids" 2>/dev/null)
  if echo "$L" | grep -q "There are 0 of"; then break; fi
  [ $((i % 30)) = 1 ] && log "czekam na zero graczy: $L"
  sleep 30
done
echo "$L" | grep -q "There are 0 of" || { log "BLAD: serwer nie opustoszal w 12 h"; exit 2; }
log "zero graczy — wdrazam $JAR"
cp -an $W/plugins/character-creator-*.jar $B/pliki/
docker compose -f $C stop world1 || { log "BLAD stop"; exit 3; }
cp -a $CFG $B/pliki/config.yml.przed
przywroc() { cp -a $B/pliki/config.yml.przed $CFG; rm -f $G/karta_kreatora.py; docker compose -f $C start world1; }
python3 $B/apply.py $CFG || { log "BLAD apply.py — przywracam"; przywroc; exit 4; }
cp $B/generator/karta_kreatora.py $G/ && (cd $W && python3 $G/karta_kreatora.py) \
  || { log "BLAD karta_kreatora.py — przywracam"; przywroc; exit 5; }
[ "$(grep -c '^        role:' $CFG)" = 16 ] || { log "BLAD: oczekiwano 16 kart klas"; przywroc; exit 5; }
rm -f $W/plugins/character-creator-*.jar && cp $JAR $W/plugins/
I0=$(stat -c %i $W/logs/latest.log)
docker compose -f $C start world1
ok=0; for i in $(seq 1 150); do I1=$(stat -c %i $W/logs/latest.log 2>/dev/null || echo 0); if [ "$I1" != "$I0" ] && grep -q 'Done (' $W/logs/latest.log; then ok=1; break; fi; sleep 4; done
[ $ok = 1 ] || { log "BLAD: world1 nie wstal"; exit 6; }
grep -E "CharacterCreator .* wlaczony|Obejscie zmiany postaci|\[CharacterCreator\].*(WARN|SEVERE)|charactercreator.*Exception" $W/logs/latest.log | cut -c1-220
ls $W/plugins | grep -i character-creator
log "wdrozono — sprawdz klientem: nowa postac (kreator, Spacja, PPM), zmiana postaci"
