#!/bin/bash
# mechanika.sh <test> - pomiary mechaniki na klonie ab-test klientem BronQA (mc-client-ab). Wynik: liczby HP.
# A = halabarda (zasieg 2-4,5 + przebicie w linii), B = minimalne naladowanie (mlot vs miecz),
# C = garda miecza (obrazenia od zombie bez/z PPM), D = garda gasnie przy przedmiocie w lewej rece,
# E = garda halabardy, F = wypad halabardy (puszczenie PPM: dwa cele w linii, potem proba w czasie odnowienia).
X="docker exec -e DISPLAY=:99 mc-client-ab"; R="docker exec ab-test rcon-cli"
r(){ $R "$1" | sed 's/\x1b\[[0-9;]*m//g'; }
hp(){ r "data get entity @e[tag=$1,limit=1] Health" | grep -oE '[0-9.]+f' | tr -d f; }
bron(){ r "minecraft:clear BronQA" >/dev/null; $X xdotool key 1; r "mi give $1 $2 BronQA" >/dev/null; sleep 0.5; $X xdotool key 2; sleep 0.3; $X xdotool key 1; sleep 1.2; }  # zmiana slotu = MMOItems przelicza statystyki (jak u gracza)
zombi(){ z=${6:-100}; r "minecraft:summon husk $1 $2 $3 {NoAI:$4b,Tags:[\"$5\"],PersistenceRequired:1b,Silent:1b,attributes:[{id:\"minecraft:max_health\",base:$z}],Health:${z}f}" >/dev/null; }
czysc(){ r "minecraft:kill @e[type=husk]" >/dev/null; }
stoj(){ r "minecraft:tp BronQA 0.5 -59 0.5 $1 $2" >/dev/null; sleep 0.6; }
case $1 in
A) czysc; bron HALBERD PRZ_WOJ_DRZEWCOWA_1; echo "sprawdz: $(r 'animacjebroni sprawdz BronQA')"
   zombi 0.5 -59 1.6 1 blisko; stoj 0 25; sleep 1.2; $X xdotool click 1; sleep 0.6; echo "A1 cel 1 blok: HP $(hp blisko) (100 = nie trafil)"
   czysc; zombi 0.5 -59 3.5 1 z3; zombi 0.5 -59 4.4 1 z4; stoj 0 8; sleep 1.2; $X xdotool click 1; sleep 0.6
   echo "A2 w linii 3 i 4 bloki: HP $(hp z3) / $(hp z4) (oba <100 = przebicie)" ;;
B) for w in "SWORD PRZ_WOJ_MIECZ_1" "GREATHAMMER PRZ_AKO_MLOT_DWURECZNY_1"; do czysc; bron $w
   zombi 0.5 -59 2.3 1 t; stoj 0 15; sleep 1.6; $X xdotool click 1; sleep 0.3; h1=$(hp t); sleep 0.3; $X xdotool click 1; sleep 0.5
   echo "B $w: po pelnym ciosie HP $h1, po drugim (0,6 s) HP $(hp t) (bez zmiany = cios zablokowany przez naladowanie)"; done ;;
C|E) czysc; [ $1 = E ] && bron HALBERD PRZ_WOJ_DRZEWCOWA_1 || bron SWORD PRZ_WOJ_MIECZ_1; zombi 0.5 -59 2.5 1 atak; stoj 0 10
   for tryb in bez garda bez garda; do r "minecraft:effect give BronQA minecraft:instant_health 1 5 true" >/dev/null; sleep 0.8
   h0=$(r "data get entity BronQA Health" | grep -oE '[0-9.]+f' | tr -d f)
   [ $tryb = garda ] && { $X xdotool mousedown 3; sleep 0.6; }
   r "minecraft:damage BronQA 6 minecraft:mob_attack by @e[tag=atak,limit=1]" >/dev/null; sleep 0.3
   h1=$(r "data get entity BronQA Health" | grep -oE '[0-9.]+f' | tr -d f); [ $tryb = garda ] && $X xdotool mouseup 3
   echo "$1 $tryb: cios 6 od huska z przodu -> stracone HP $(python3 -c "print(round($h0-$h1,2))")"; sleep 0.8; done; czysc ;;
D) czysc; bron SWORD PRZ_WOJ_MIECZ_1; echo "D pusta lewa: $(r 'animacjebroni sprawdz BronQA' | grep -oE 'garda=[a-z]+|lewa=[A-Z_]+' | tr '\n' ' ')"
   r "minecraft:item replace entity BronQA weapon.offhand with minecraft:torch" >/dev/null; sleep 1.2
   echo "D pochodnia w lewej: $(r 'animacjebroni sprawdz BronQA' | grep -oE 'garda=[a-z]+|lewa=[A-Z_]+' | tr '\n' ' ')"
   r "minecraft:item replace entity BronQA weapon.offhand with minecraft:air" >/dev/null; sleep 1.2
   echo "D znow pusta: $(r 'animacjebroni sprawdz BronQA' | grep -oE 'garda=[a-z]+|lewa=[A-Z_]+' | tr '\n' ' ')" ;;
F) czysc; bron HALBERD PRZ_WOJ_DRZEWCOWA_1; zombi 0.5 -59 3.0 1 f1 1000; zombi 0.5 -59 4.4 1 f2 1000; stoj 0 8; sleep 1.2
   $X xdotool mousedown 3; sleep 0.9; $X import -window root /review/captures/halabarda-garda.png; $X xdotool mouseup 3; sleep 0.6
   echo "F wypad: HP $(hp f1) / $(hp f2) (oba <1000 = trafil linie), pozycja gracza z=$(r 'data get entity BronQA Pos[2]' | grep -oE '[0-9.]+d' | tr -d d)"
   a1=$(hp f1); stoj 0 8; $X xdotool mousedown 3; sleep 0.9; $X xdotool mouseup 3; sleep 0.6
   echo "F drugi wypad w odnowieniu: HP f1 $a1 -> $(hp f1) (bez zmiany = odnowienie dziala)"; czysc ;;
esac
