#!/usr/bin/env bash
# Pelna regresja na klonie: jar z build/libs cc-naprawa (jeden!), wszystkie evale WYBOR + KREATOR.
set -u
W=/home/dev/.claude/jobs/2f21747c/tmp/wybor; E=$W/eval; S=$W/scena; K=$W/data
B=/home/dev/Desktop/Przemek/MC/.claude/worktrees/cc-naprawa/MCMMORPG/plugin-src/character-creator/build/libs
rm -f $K/plugins/character-creator-*.jar; cp $B/character-creator-*.jar $K/plugins/
docker stop cc-nap-wybor >/dev/null 2>&1; $W/start.sh; grep -a "Ambiguous\|Enabling CharacterCreator" $K/logs/latest.log
$E/run.sh reset.js | tail -1
grep -q "name: Elwen" $K/plugins/CharacterCreator/characters.yml || OD=2 DO=3 $E/run.sh setup.js 2>&1 | grep -E "^OK|ERR" | cut -c1-30
grep -q "name: Morth" $K/plugins/CharacterCreator/characters.yml || OD=3 DO=4 $E/run.sh setup.js 2>&1 | grep -E "^OK|ERR" | cut -c1-30
wynik() { echo "== $1: $(grep -E 'WYNIK' $2 | tail -1) FAIL=$(grep -c '^FAIL' $2)"; grep -E '^FAIL|ERR' $2 | head -5; }
sleep 6; $E/run.sh scen.js > $E/p-scen.txt 2>&1; wynik scen $E/p-scen.txt
$E/run.sh reset.js >/dev/null
for s in kosz_pusty op spacja; do sleep 6; $E/run.sh $s.js > $E/p-$s.txt 2>&1; wynik $s $E/p-$s.txt; done
sleep 6; $E/run-rw.sh reload.js > $E/p-reload.txt 2>&1; wynik reload $E/p-reload.txt
sleep 6; $E/run.sh czat.js 2>&1 | grep -v "^❤" > $E/p-czat.txt; echo "== czat:"; cat $E/p-czat.txt
for s in e1-rasy e2-istniejacy; do sleep 6; $S/run.sh $s.js > $S/p-$s.txt 2>&1; echo "== $s: PASS=$(grep -c '^PASS' $S/p-$s.txt) FAIL=$(grep -c '^FAIL' $S/p-$s.txt)"; grep -E '^FAIL|wyjatek' $S/p-$s.txt | head -5; done
sleep 6; $E/run.sh kosz_prep.js >/dev/null 2>&1
docker stop cc-nap-wybor >/dev/null
python3 - <<'PY'
import re,time
p='/home/dev/.claude/jobs/2f21747c/tmp/wybor/data/plugins/CharacterCreator/trash.yml'; s=open(p).read()
old=int((time.time()-8*86400)*1000); s2=re.sub(r'(name: Elwen\n    at: )\d+', lambda m: m.group(1)+str(old), s)
print('termin: cofnieto' if s2!=s else 'termin: BRAK Elwen w koszu'); open(p,'w').write(s2)
PY
$W/start.sh; sleep 5; $E/run.sh termin.js > $E/p-termin.txt 2>&1; wynik termin $E/p-termin.txt
echo "== Invalid hud holder w logu: $(grep -a -c 'Invalid hud holder' $K/logs/latest.log)"
