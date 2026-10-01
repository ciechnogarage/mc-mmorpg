#!/bin/bash
# mechanika.sh <test> - pomiary mechaniki na klonie ab-test klientem BronQA (mc-client-ab). Wynik: liczby HP.
# A = halabarda (zasieg 2-4,5 + przebicie w linii), B = minimalne naladowanie (mlot vs miecz),
# C = garda miecza (obrazenia od zombie bez/z PPM), D = garda gasnie przy przedmiocie w lewej rece,
# E = garda halabardy, G = kontra halabardy: 1 samo puszczenie PPM nic nie robi, 2 blok+pusc+LPM = wypad w linii
# (bez dodatkowego zwyklego pchniecia), 3 blok od razu po wypadzie, 4 w odnowieniu LPM = zwykle pchniecie.
# P = parowanie, U = uderzenie mlota, S = sztylet od tylu/przodu, K = krwawienie, A2 = pancerz.
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
   echo "A1b w linii 3 i 4 bloki: HP $(hp z3) / $(hp z4) (oba <100 = przebicie)" ;;
A2) czysc; bron HAMMER PRZ_AKO_BULAWA_1; zombi 0.5 -59 2.3 1 bezpancerza 1000; stoj 0 10; sleep 1.2
    $X xdotool click 1; sleep 0.5; b=$(hp bezpancerza); czysc
    zombi 0.5 -59 2.3 1 pancerz 1000; r "minecraft:attribute @e[tag=pancerz,limit=1] minecraft:armor base set 10" >/dev/null
    stoj 0 10; sleep 1.2; $X xdotool click 1; sleep 0.5
    echo "A2 mlot: HP bez pancerza $b, armor 10 $(hp pancerz) (redukcja czesciowo przebita)"; czysc ;;
B) for w in "SWORD PRZ_WOJ_MIECZ_1" "GREATHAMMER PRZ_AKO_MLOT_DWURECZNY_1"; do czysc; bron $w
   zombi 0.5 -59 2.3 1 t; stoj 0 15; sleep 1.6; $X xdotool click 1; sleep 0.3; h1=$(hp t); sleep 0.3; $X xdotool click 1; sleep 0.5
   echo "B $w: po pelnym ciosie HP $h1, po drugim (0,6 s) HP $(hp t) (bez zmiany = cios zablokowany przez naladowanie)"; done ;;
C|E) czysc; [ $1 = E ] && bron HALBERD PRZ_WOJ_DRZEWCOWA_1 || bron SWORD PRZ_WOJ_MIECZ_1; zombi 0.5 -59 2.5 1 atak; stoj 0 10
   for tryb in bez garda bez garda; do r "minecraft:effect give BronQA minecraft:instant_health 1 5 true" >/dev/null; sleep 1.2
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
   h1=$(hp f1); h2=$(hp f2); a1=$h1
   echo "F wypad: HP $h1 / $h2 (oba <1000 = trafil linie)"; $X xdotool mousedown 3; sleep 0.9; $X xdotool mouseup 3; sleep 0.6   # < 3 s po pierwszym
   echo "F drugi wypad w odnowieniu: HP f1 $a1 -> $(hp f1) (bez zmiany = odnowienie dziala)"; czysc ;;
G) czysc; bron HALBERD PRZ_WOJ_DRZEWCOWA_1; zombi 0.5 -59 3.0 1 g1 1000; zombi 0.5 -59 4.4 1 g2 1000; stoj 0 8; sleep 1.5
   $X xdotool mousedown 3; sleep 0.9; $X xdotool mouseup 3; sleep 0.8; echo "G1 samo puszczenie: HP $(hp g1) / $(hp g2) (1000 = nic)"
   sleep 0.6; $X xdotool mousedown 3; sleep 0.9; $X xdotool mouseup 3; sleep 0.1; $X xdotool click 1; sleep 0.6
   w1=$(hp g1); w2=$(hp g2); echo "G2 blok+LPM: HP $w1 / $w2 (oba ok. 863 = wypad x2,0, bez dodatkowego pchniecia)"
   r "minecraft:effect give BronQA minecraft:instant_health 1 5 true" >/dev/null; sleep 0.5; h0=$(r "data get entity BronQA Health" | grep -oE '[0-9.]+f' | tr -d f)
   $X xdotool mousedown 3; sleep 0.6; r "minecraft:damage BronQA 6 minecraft:mob_attack by @e[tag=g1,limit=1]" >/dev/null; sleep 0.3
   h1=$(r "data get entity BronQA Health" | grep -oE '[0-9.]+f' | tr -d f); $X xdotool mouseup 3; sleep 0.1; $X xdotool click 1; sleep 0.6
   echo "G3 blok zaraz po wypadzie: stracone $(python3 -c "print(round($h0-$h1,2))") (ok. 2,5 = blok dziala)"
   echo "G4 kontra w odnowieniu: HP g1 $w1 -> $(hp g1) (spadek ok. 98 = zwykle pchniecie, nie 137)"; czysc ;;
P) czysc; bron SWORD PRZ_WOJ_MIECZ_1; zombi 0.5 -59 2.5 1 parry 1000; stoj 0 10
   # okno parowania miecza: tick gardy 3..9; RCON ~0,1-0,2 s -> cios wysylany zaraz po wcisnieciu PPM trafia w okno
   for d in 0.0 0.1 1.0; do r "minecraft:effect give BronQA minecraft:instant_health 1 5 true" >/dev/null; sleep 1.0
   h0=$(r "data get entity BronQA Health" | grep -oE '[0-9.]+f' | tr -d f)
   $X xdotool mousedown 3; sleep $d; r "minecraft:damage BronQA 6 minecraft:mob_attack by @e[tag=parry,limit=1]" >/dev/null; sleep 0.2
   h1=$(r "data get entity BronQA Health" | grep -oE '[0-9.]+f' | tr -d f); $X xdotool mouseup 3; sleep 0.5
   echo "P cios ${d}s po PPM: stracone $(python3 -c "print(round($h0-$h1,2))") (0 = parowanie, ok. 2,4 = zwykly blok)"; done; czysc ;;
U) czysc; bron GREATHAMMER PRZ_AKO_MLOT_DWURECZNY_1
   zombi 2.0 -59 1.5 1 u1 1000; zombi -1.0 -59 1.5 1 u2 1000; stoj 0 10; sleep 1.2
   $X xdotool mousedown 3; sleep 1; $X xdotool mouseup 3; sleep 0.5
   a=$(hp u1); b=$(hp u2); echo "U uderzenie: HP $a / $b (oba <1000)"
   $X xdotool mousedown 3; sleep 1; $X xdotool mouseup 3; sleep 0.5
   echo "U w odnowieniu: HP $a -> $(hp u1), $b -> $(hp u2) (bez zmiany)"; czysc ;;
S) czysc; bron DAGGER PRZ_NIN_SZTYLET_1; zombi 0.5 -59 2.3 1 tyl 1000; stoj 0 10
   r "minecraft:tp @e[tag=tyl,limit=1] 0.5 -59 2.3 0 0" >/dev/null; sleep 1.2; $X xdotool click 1; sleep 0.5; t=$(hp tyl)
   czysc; zombi 0.5 -59 2.3 1 przod 1000; r "minecraft:tp @e[tag=przod,limit=1] 0.5 -59 2.3 180 0" >/dev/null
   stoj 0 10; sleep 1.2; $X xdotool click 1; sleep 0.5
   echo "S sztylet: tyl HP $t, przod HP $(hp przod) (obrazenia od tylu ok. x2)"; czysc ;;
K) czysc; bron AXE PRZ_WOJ_TOPOR_1; zombi 0.5 -59 2.3 1 krew 1000; stoj 0 10; sleep 1.2
   $X xdotool click 1; sleep 0.2; a=$(hp krew); sleep 1; b=$(hp krew); sleep 1.2; c=$(hp krew)
   echo "K krwawienie: HP zaraz $a, po 1 s $b, po 2-3 s $c (kolejne spadki)"; czysc ;;
R) czysc; bron SWORD PRZ_WOJ_MIECZ_1; zombi 0.5 -59 2.3 1 rip 1000; stoj 0 10; sleep 1.5
   $X xdotool click 1; sleep 1.2; a=$(hp rip)   # zwykly pelny cios
   $X xdotool mousedown 3; sleep 0.1; r "minecraft:damage BronQA 6 minecraft:mob_attack by @e[tag=rip,limit=1]" >/dev/null; sleep 0.15
   $X xdotool mouseup 3; sleep 0.05; $X xdotool click 1; sleep 0.5; b=$(hp rip)
   echo "R riposta: zwykly cios $(python3 -c "print(round(1000-$a,1))"), cios po parowaniu $(python3 -c "print(round($a-$b,1))") (ok. x1,5)"; czysc ;;
esac
