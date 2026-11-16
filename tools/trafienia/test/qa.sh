#!/usr/bin/env bash
# Zrzuty klientem QA (prawdziwy klient 1.21.11) na trafienia-test: TrafQA patrzy z boku, TrafBot bije (serwer celuje).
# Wynik: output/trafienia/qa/*.jpg (arkusze klatek). Klient i bot usuwane na koniec.
set -uo pipefail
K=klon-trafienia; W=/opt/docker/mc/data/world1; D=$W/output/trafienia/klient; Q=$W/output/trafienia/qa; C=mc-client-trafienia
H=$(dirname "$(readlink -f "$0")"); L=/opt/docker/mc/klony/trafienia/data/logs/latest.log
RC() { docker exec $K rcon-cli "$@" 2>/dev/null | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
mkdir -p $Q $D/captures $D/native-tmp
docker rm -f $C >/dev/null 2>&1
n0=$(wc -l < $L)
docker run -d --name $C --network klon-trafienia-net --memory 2500m -u 1000:1000 -v /home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client:/mc:ro -v $D:/review mc-client-test:latest bash /review/launch.sh >/dev/null
/opt/docker/mc/narzedzia/bot/run.sh trafienia "$H/stoi.js" TrafBot 900 > /home/dev/.claude/jobs/16b50503/tmp/bot.log 2>&1 & BOTPID=$!
for i in $(seq 1 80); do tail -n +$n0 $L | grep -q "TrafQA joined" && tail -n +$n0 $L | grep -q "TrafBot joined" && break; sleep 3; done
tail -n +$n0 $L | grep -q "TrafQA joined" || { echo "FAIL klient nie wszedl"; tail -5 $D/instance/client.log; exit 1; }
sleep 20   # pack zasobow + chunk
shot() { docker exec -e DISPLAY=:99 $C sh -c "import -window root /tmp/s.png && convert /tmp/s.png -quality 88 /review/captures/$1.jpg"; cp $D/captures/$1.jpg $Q/$1.jpg; echo "  zrzut $1"; }
# arkusz <nazwa> <odstep_s> [kadr]: 12 klatek, 4x3, kadr = geometria crop (domyslnie srodek 760x560)
arkusz() { docker exec -e DISPLAY=:99 $C sh -c "for i in \$(seq 1 12); do import -window root /tmp/a\$i.png; sleep $2; done; cd /tmp; for r in 0 1 2; do a=\$((r*4+1)); convert a\$a.png a\$((a+1)).png a\$((a+2)).png a\$((a+3)).png -crop ${3:-640x560+320+60} +repage -resize 380x +append r\$r.png; done; convert r0.png r1.png r2.png -append -quality 82 /review/captures/$1.jpg"; cp $D/captures/$1.jpg $Q/$1.jpg; echo "  arkusz $1"; }
kamera() { docker exec -e DISPLAY=:99 $C xdotool mousemove 640 343 click --delay 200 1 >/dev/null 2>&1; sleep 1.5; RC "minecraft:gamemode creative TrafQA" >/dev/null;   # zabity przez moba ze spawnera: przycisk Respawn
  RC "minecraft:effect give TrafQA resistance 600 4 true" >/dev/null; RC "minecraft:teleport TrafQA $1" >/dev/null; sleep 1; }
nowy() {
  RC "minecraft:kill @e[type=!player,type=!item_display]" >/dev/null; RC "minecraft:kill @e[type=item_display]" >/dev/null; sleep 1
  RC "mm mobs spawn L004_KOSIARZ 1 world,0.5,-59,4.5" >/dev/null; sleep 2
  RC "minecraft:data merge entity @e[type=vindicator,limit=1] {NoAI:1b}" >/dev/null
  RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 180 0" >/dev/null
  RC "minecraft:teleport TrafBot 0.5 -59 1.3 0 0" >/dev/null; sleep 2
}
# bron wkladana PRZED pauza: zmiana przedmiotu zeruje pasek naladowania ataku
uzbroj() { RC "minecraft:item replace entity TrafBot weapon.mainhand with $1" >/dev/null; sleep ${2:-0.8}; }
uderz() { RC "trafienia celuj TrafBot $1" >/dev/null; RC "trafienia cios TrafBot $2" >/dev/null; }
bij() { uzbroj $3 ${4:-}; uderz $1 $2; }
MIECZ=minecraft:netherite_sword; BULAWA=minecraft:mace
bokiem() { RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 90 0" >/dev/null; sleep 1; }
trap 'kill $SPRZ 2>/dev/null; docker rm -f $C bot-trafienia-$BOTPID >/dev/null 2>&1' EXIT
RC "minecraft:time set noon" >/dev/null; RC "minecraft:gamerule doDaylightCycle false" >/dev/null; RC "minecraft:gamerule doMobSpawning false" >/dev/null
# spawnery MM z world1 dzialaja tez na klonie: co 1 s sprzatamy wszystko poza graczami, Kosiarzem i odpadami
( while :; do RC "minecraft:kill @e[type=!player,type=!vindicator,type=!item_display,type=!silverfish]" >/dev/null; sleep 1; done ) & SPRZ=$!
RC "minecraft:fill -8 -60 -8 8 -60 12 minecraft:stone" >/dev/null
for p in TrafQA TrafBot; do RC "minecraft:gamemode creative $p" >/dev/null; done
RC "minecraft:effect give TrafQA night_vision 99999 0 true" >/dev/null
BOK="-2.2 -58.2 2.6 -52 12"
BLISKO="2.0 -58.0 3.0 45 22"   # prawa strona moba (kikut, zwis)
SCENY=${SCENY:-wszystkie}
if [ "$SCENY" = wszystkie ]; then
echo "== 1. kapsuly czesci (kalibracja)"
nowy; kamera "$BOK"; RC "trafienia pokaz 6" >/dev/null; sleep 1.5; shot 1_kapsuly
sleep 5
echo "== 2. blysk tylko trafionej czesci (prawa reka, potem glowa)"
nowy; kamera "$BOK"; ( sleep 0.15; bij reka_r pchniecie $MIECZ ) & BP=$!; arkusz 2_blysk_reka 0.05; wait $BP
( sleep 0.15; bij glowa pchniecie $MIECZ ) & BP=$!; arkusz 2_blysk_glowa 0.05; wait $BP
echo "== 3. mocne ciecie nisko: obie nogi naraz"
nowy; kamera "$BOK"; ( sleep 0.15; bij noga_r:1 mocny minecraft:iron_sword ) & BP=$!; arkusz 3_ciecie_nogi 0.05; wait $BP
echo "== 3b. lekkie ciecie w piers: ostrze grzeznie w snopie"
nowy; kamera "$BOK"; ( sleep 0.15; bij tulow:1 lekki minecraft:iron_sword ) & BP=$!; arkusz 3b_utkniecie 0.05; wait $BP
echo "== 3c. buzdygan straca kapelusz"
nowy; kamera "$BOK"; uzbroj $BULAWA 1.8; ( sleep 0.15; uderz kapelusz obuch ) & BP=$!; arkusz 3c_kapelusz 0.08; wait $BP
echo "== 4. CIECIE: miecz z boku odcina prawa reke, kosa leci z nia"
nowy; bokiem; kamera "$BOK"; bij reka_r lekki $MIECZ
uzbroj $MIECZ; ( sleep 0.15; uderz reka_r lekki ) & BP=$!; arkusz 4_odciecie_reki 0.1; wait $BP
RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 180 0" >/dev/null; kamera "$BLISKO"; sleep 1; shot 4_kikut_zblizenie
echo "== 5. OBUCH: buzdygan miazdzy prawa reke - zwisa, kosa wypada, reka zostaje"
nowy; kamera "$BOK"
# bije, az reka jest o krok od zmiazdzenia (czesc ciosow lapie dlon albo bark), dopiero kolejny cios nagrywa
for n in 1 2 3 4 5 6; do bij reka_r obuch $BULAWA 1.8
  Z=$(RC "trafienia stan" | grep -oE 'zgniot=\{[^}]*' | grep -oE 'reka_r=[0-9.-]+' | cut -d= -f2)
  python3 -c "import sys;sys.exit(0 if float(sys.argv[1])<=6.5 else 1)" "${Z:-99}" && break; done
uzbroj $BULAWA 1.8; ( sleep 0.15; uderz reka_r obuch ) & BP=$!; arkusz 5_zmiazdzenie_reki 0.12; wait $BP
sleep 1.5; RC "trafienia stan" | grep -o 'zgniecione=[^ ]*'; shot 5_zwis; kamera "$BLISKO"; sleep 1; shot 5_zwis_zblizenie
echo "== 6. OBUCH w kosisko: peka, ostrze spada, kikut drzewca w dloni"
nowy; kamera "$BOK"; bij kosisko obuch $BULAWA 1.8
uzbroj $BULAWA 1.8; ( sleep 0.15; uderz kosisko obuch ) & BP=$!; arkusz 6_zlamane_kosisko 0.1; wait $BP
kamera "$BLISKO"; sleep 1; shot 6_kikut_kosiska
fi
echo "== 8. ZDERZENIE: miecz w kose w trakcie zapowiedzi zamachu"
nowy; kamera "$BOK"; RC "minecraft:tag @e[type=vindicator,limit=1] add traf_zamach" >/dev/null; uzbroj $MIECZ
( sleep 0.15; uderz ostrze lekki ) & BP=$!; arkusz 8_zderzenie 0.06; wait $BP
echo "== 9. OGIEN: podpalona noga, ogien idzie w gore, Kosiarz gasi"
nowy; RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 0 0" >/dev/null; kamera "$BOK"; bij noga_l:1 lekki $MIECZ
RC "minecraft:data merge entity @e[type=vindicator,limit=1] {Fire:240s}" >/dev/null; arkusz 9_ogien 0.6
shot 9_zweglenie
if [ "$SCENY" = wszystkie ]; then
echo "== 10. PEDY: ciecie w pedy Szczepu, potem stan ospaly"
nowy; kamera "$BLISKO"; uzbroj $MIECZ; ( sleep 0.15; uderz pedy lekki ) & BP=$!; arkusz 10_pedy 0.08; wait $BP
echo "== 11. ZACHWIANIE: dwa ciosy buzdyganem w glowe"
nowy; kamera "$BOK"; bij glowa obuch+mocny $BULAWA 1.8; uzbroj $BULAWA 1.8; ( sleep 0.15; uderz glowa obuch ) & BP=$!; arkusz 11_zachwianie 0.12; wait $BP
fi
if [ "$SCENY" = wszystkie ]; then
echo "== 7. OBUCH w kolano: przyklek, noga zostaje, potem kustyka"
nowy; kamera "$BOK"
for n in 1 2 3 4 5 6 7; do bij noga_l:1 obuch $BULAWA 1.8
  Z=$(RC "trafienia stan" | grep -oE 'zgniot=\{[^}]*' | grep -oE 'noga_l=[0-9.-]+' | cut -d= -f2)
  python3 -c "import sys;sys.exit(0 if float(sys.argv[1])<=6.5 else 1)" "${Z:-99}" && break; done
uzbroj $BULAWA 1.8; ( sleep 0.15; uderz noga_l:1 obuch ) & BP=$!; arkusz 7_kolano 0.1; wait $BP
RC "trafienia stan" | grep -o 'zgniecione=[^ ]*'
RC "minecraft:data merge entity @e[type=vindicator,limit=1] {NoAI:0b}" >/dev/null
RC "minecraft:gamemode survival TrafBot" >/dev/null; RC "minecraft:effect give TrafBot resistance 99999 4 true" >/dev/null
RC "minecraft:teleport TrafBot 7.5 -59 -2.5" >/dev/null   # bot za kamera: Kosiarz kustyka w jej strone
kamera "4.0 -58.0 1.0 45 10"; arkusz 7_kulenie_ruch 0.25 "1280x720+0+0"
fi
if [ "$SCENY" = wszystkie ] || [ "$SCENY" = v3 ] || [ "$SCENY" = v3b ]; then
if [ "$SCENY" != v3b ]; then
echo "== 12. SMIERC od ciecia: rozsypuje sie na kawalki"
nowy; kamera "$BOK"; RC "minecraft:data merge entity @e[type=vindicator,limit=1] {Health:3f}" >/dev/null; uzbroj $MIECZ
( sleep 0.15; uderz tulow:1 lekki ) & BP=$!; arkusz 12_smierc_ciecie 0.12; wait $BP
echo "== 13. SMIERC od obuchu: lamie sie w pol"
nowy; kamera "$BOK"; RC "minecraft:data merge entity @e[type=vindicator,limit=1] {Health:3f}" >/dev/null; uzbroj $BULAWA 1.8
( sleep 0.15; uderz tulow:1 obuch ) & BP=$!; arkusz 13_smierc_obuch 0.12; wait $BP
echo "== 14. CHOD z peknietym kijem w nodze"
nowy; RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 0 0" >/dev/null
for n in 1 2 3 4 5 6 7; do bij noga_l:1 obuch $BULAWA 1.8
  RC "trafienia stan" | grep -q 'zgniecione=\[[^]]*noga_l' && break; done
sleep 2; RC "minecraft:data merge entity @e[type=vindicator,limit=1] {NoAI:0b}" >/dev/null
RC "minecraft:gamemode survival TrafBot" >/dev/null; RC "minecraft:effect give TrafBot resistance 99999 4 true" >/dev/null
RC "minecraft:teleport TrafBot 0.5 -59 -6" >/dev/null; kamera "5.5 -58.0 0.5 90 8"; arkusz 14_chod_kolano 0.2 "1280x720+0+0"
fi
echo "== 15. IKONA OGNIA na pasku (PaskiMobow z lacza Trafien)"
# tylko klon: Kosiarz nie ma jeszcze wpisu w PaskiMobow (frakcja/wzorzec) - bez tego nie ma paska
sed -i "s/^wzorce: \['WIEZA_P01_\*'\]/wzorce: ['WIEZA_P01_*', 'L004_*']/" /opt/docker/mc/klony/trafienia/data/plugins/PaskiMobow/config.yml
RC "paskimobow przeladuj" >/dev/null
nowy; RC "minecraft:gamemode creative TrafBot" >/dev/null; kamera "-1.2 -58.4 2.4 -30 -6"; bij tulow:1 lekki $MIECZ
RC "minecraft:data merge entity @e[type=vindicator,limit=1] {Fire:160s}" >/dev/null; sleep 1.2; shot 15_ikona_ognia
echo "== 16. KOPNIAK bez kosy (reka odcieta)"
nowy; RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 120 0" >/dev/null
for n in 1 2 3 4; do bij reka_r:0:0.4 lekki $MIECZ; RC "trafienia stan" | grep -q 'odciete=\[[^]]*reka_r' && break; done
RC "minecraft:data merge entity @e[type=vindicator,limit=1] {NoAI:0b}" >/dev/null
RC "minecraft:gamemode survival TrafBot" >/dev/null; RC "minecraft:effect give TrafBot resistance 99999 4 true" >/dev/null
RC "minecraft:teleport TrafBot 0.5 -59 3.0 180 0" >/dev/null; kamera "-3.6 -58.4 3.8 -85 6"; sleep 1.5; arkusz 16_kopniak 0.35 "1280x720+0+0"
RC "minecraft:gamemode creative TrafBot" >/dev/null
fi
RC "trafienia stan" | head -3
kill $SPRZ 2>/dev/null; docker rm -f $C >/dev/null 2>&1; docker rm -f bot-trafienia-$BOTPID >/dev/null 2>&1; rm -rf $D/native-tmp $D/instance/downloads
ls -la $Q | awk '{print $5, $9}'
