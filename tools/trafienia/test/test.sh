#!/usr/bin/env bash
# Test Trafien na klonie klon-trafienia (narzedzia/klon.sh): bot stoi, serwer celuje nim w czesci Kosiarza i wykonuje prawdziwe ciosy.
# Mob: Kosiarz (Loch 004). Asercje na liniach "TRAF {json}" z logu + /trafienia stan + atrybuty/tagi encji. Wynik: PASS/FAIL.
set -uo pipefail
K=klon-trafienia; L=/opt/docker/mc/klony/trafienia/data/logs/latest.log; H=$(dirname "$(readlink -f "$0")")
RC() { docker exec $K rcon-cli "$@" 2>/dev/null | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
OK=0; ZLE=0
ok()  { echo "PASS $*"; OK=$((OK+1)); }
zle() { echo "FAIL $*"; ZLE=$((ZLE+1)); }
ostatni_traf() { grep -a 'TRAF {' "$L" | tail -1 | sed 's/.*TRAF //'; }
pole() { python3 -c "import json,sys; d=json.loads(sys.argv[1]); v=d$2; print(json.dumps(v) if not isinstance(v,str) else v)" "$1"; }

/opt/docker/mc/narzedzia/bot/run.sh trafienia "$H/stoi.js" TrafBot 900 > /home/dev/.claude/jobs/16b50503/tmp/bot.log 2>&1 & BOTPID=$!
  -e MC_HOST=$K -e NICK=TrafBot -e CZAS_S=900 node:20 node /h/bot.js >/dev/null
for i in $(seq 1 60); do RC "minecraft:list" | grep -q TrafBot && break; sleep 2; done
RC "minecraft:list" | grep -q TrafBot || { echo "FAIL bot nie wszedl"; tail /home/dev/.claude/jobs/16b50503/tmp/bot.log; exit 1; }
RC "minecraft:gamemode creative TrafBot" >/dev/null
RC "minecraft:time set noon" >/dev/null; RC "minecraft:gamerule doDaylightCycle false" >/dev/null
RC "minecraft:fill -6 -60 -6 6 -60 10 minecraft:stone" >/dev/null

nowy_cel() {  # swiezy Kosiarz przodem do bota, bez AI (deterministyczne celowanie)
  RC "minecraft:kill @e[type=!player,type=!item_display]" >/dev/null; RC "minecraft:kill @e[type=item_display]" >/dev/null; sleep 1
  RC "mm mobs spawn L004_KOSIARZ 1 world,0.5,-59,4.5" >/dev/null; sleep 2
  RC "minecraft:data merge entity @e[type=vindicator,limit=1] {NoAI:1b}" >/dev/null
  RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 180 0" >/dev/null
  RC "minecraft:teleport TrafBot 0.5 -59 1.3 0 0" >/dev/null; sleep 2
}
cios() {  # cios <czesc> <rodzaj|lekki|mocny|rodzaj+mocny> [bron] [pauza_s]
  # bron PRZED pauza: zmiana przedmiotu w dloni zeruje pasek naladowania ataku (cios tuz po podmianie bylby slaby)
  RC "minecraft:item replace entity TrafBot weapon.mainhand with ${3:-minecraft:air}" >/dev/null
  sleep ${4:-0.8}
  RC "trafienia celuj TrafBot $1" | tee -a /dev/stderr >/dev/null
  local n0; n0=$(grep -ac 'TRAF {' "$L"); RC "trafienia cios TrafBot $2" >/dev/null
  for i in 1 2 3 4 5 6 7 8 9 10; do [ "$(grep -ac 'TRAF {' "$L")" -gt "$n0" ] && break; sleep .3; done
  [ "$(grep -ac 'TRAF {' "$L")" -gt "$n0" ] && ostatni_traf || echo '{}'
}
MIECZ=minecraft:netherite_sword; TOPOR="minecraft:iron_axe 2.0"; BULAWA="minecraft:mace 2.4"   # zapas na wolniejszy serwer klona (pelne naladowanie)
stan() { RC "trafienia stan"; }
tagi() { RC "minecraft:tag @e[type=vindicator,limit=1] list"; }
predk() { RC "minecraft:attribute @e[type=vindicator,limit=1] minecraft:movement_speed get" | grep -oE '[0-9.]+$'; }

prawy_bok() { RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 120 0" >/dev/null; sleep 1; }   # prawy przod-bok: przedramie odsloniete (z boku zaslania je drzewce, od tylu ramie)
bokiem() { RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 0 0" >/dev/null; sleep 1; }   # plecami do bota: od przodu reke i nogi zaslania kosa trzymana nisko
py() { python3 -c "import json,sys;d=json.loads(sys.argv[1]);sys.exit(0 if ($1) else 1)" "$2" 2>/dev/null; }
# bij <czesc> <tryb> <bron> [pauza] <max> <warunek_stopu_py>: ciosy az warunek, wypisuje liczbe ciosow i ostatni TRAF
bij() { local n=0 T='{}'; for n in $(seq 1 $5); do T=$(cios $1 $2 $3 $4); py "$6" "$T" && break; done; echo "$n|$T"; }

echo "== A. CIECIE odcina: miecz w prawe przedramie przy lokciu, z prawego boku (od frontu tulow, od tylu ramie, przy dloni drzewce)"
nowy_cel; T=$(cios tulow:1 lekki $MIECZ); py "d['utknal']=='tulow'" "$T" && ok "A ciecie w piers grzeznie w snopie ($(pole "$T" "['czesci']"))" || zle "A piers: $T"
nowy_cel; prawy_bok; R=$(bij reka_r:0:0.4 lekki $MIECZ "" 6 "'reka_r' in d['odciete']"); N=${R%%|*}; T=${R#*|}; echo "  po $N: $T"
py "'reka_r' in d['odciete']" "$T" && ok "A miecz odcina reke po $N ciosach" || zle "A: $T"
S=$(stan); echo "$S" | grep -q 'kikut_reka_r=1' && echo "$S" | grep -q 'kosisko=0' && ok "A kikut, kosa poleciala z reka" || zle "A widocznosc: $S"
tagi | grep -q traf_bez_kosy && ok "A tag traf_bez_kosy (kopniak zamiast zamachu)" || zle "A brak tagu"
RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 180 0" >/dev/null; sleep 1
T=$(cios tulow:1 pchniecie $MIECZ); py "d.get('glowna') not in ('kosisko','reka_r','ostrze')" "$T" && ok "A odcietej kosy nie da sie trafic" || zle "A trafiona kosa: $T"

echo "== B. TOPOR rabie: reka jednym ciosem"
nowy_cel; prawy_bok; R=$(bij reka_r:0:0.4 topor $TOPOR 3 "'reka_r' in d['odciete']"); N=${R%%|*}; T=${R#*|}; echo "  po $N: $T"
[ "$N" = 1 ] && py "'reka_r' in d['odciete']" "$T" && ok "B topor odcina reke jednym ciosem" || zle "B po $N: $T"

echo "== C. OBUCH nie odcina: bulawa w prawa reke = zmiazdzona, zwisa, kosa wypada"
nowy_cel; R=$(bij reka_r obuch $BULAWA 6 "'reka_r' in d['zgniecione']"); N=${R%%|*}; T=${R#*|}; echo "  po $N: $T"
py "'reka_r' in d['zgniecione'] and d['odciete']==[]" "$T" && ok "C reka zmiazdzona po $N ciosach, nie odcieta" || zle "C: $T"
S=$(stan); echo "  $S" | cut -c1-400
echo "$S" | grep -q 'przedramie_seg_r=1' && echo "$S" | grep -q 'kikut_reka_r=0' && ok "C reka zostaje na miejscu (bez kikuta)" || zle "C reka znikla"
echo "$S" | grep -q 'kosisko=0' && echo "$S" | grep -q 'ostrze=0' && ok "C kosa wypadla ze zmiazdzonej dloni" || zle "C kosa w dloni"
G=$(tagi); echo "$G" | grep -q traf_zwis_reka_r && echo "$G" | grep -q traf_bez_kosy && ok "C tagi zwis + bez kosy" || zle "C tagi: $G"
T=$(cios reka_r obuch $BULAWA); py "d['odciete']==[]" "$T" && ok "C dalsze ciosy obuchem nadal nie odcinaja" || zle "C odcielo: $T"

echo "== D. PCHNIECIE ani nie odcina, ani nie lamie"
nowy_cel; for n in 1 2 3 4 5 6; do T=$(cios reka_r pchniecie $MIECZ); done; echo "  $T"
py "d['odciete']==[] and d['zgniecione']==[] and d['hp']['reka_r'][0]>=14.9 and d['hp']['reka_r'][1]>=14.9" "$T" && ok "D 6 pchniec w reke: cala, pule nietkniete" || zle "D: $T"
T=$(cios glowa pchniecie $MIECZ); echo "  $T"
py "d['glowna']=='glowa' and abs(d['po']-d['obr']*1.4)<0.3" "$T" && ok "D pchniecie w oko x1.4 (miejsce x rodzaj)" || zle "D glowa: $T"
T=$(cios tulow:1 pchniecie $MIECZ); echo "  $T"
py "d['glowna']=='tulow' and d['utknal']=='tulow' and abs(d['po']-d['obr']*0.9)<0.3" "$T" && ok "D grot w snop: utyka, x0.9" || zle "D tulow: $T"
RC "minecraft:effect clear TrafBot" >/dev/null

echo "== E. OBUCH w snop: obrazenia bez kary materialu (x0.9 miejsca); pal peka dopiero po kilku ciosach"
nowy_cel; T=$(cios tulow:1 obuch $BULAWA); echo "  $T"
py "d['glowna']=='tulow' and abs(d['po']-d['obr']*0.9)<0.3 and d['zgniecione']==[]" "$T" && ok "E bulawa w tulow x0.9, jeden cios bez skutku" || zle "E: $T"
R=$(bij tulow:1 obuch $BULAWA 6 "'tulow' in d['zgniecione']"); N=${R%%|*}; T=${R#*|}
py "'tulow' in d['zgniecione']" "$T" && tagi | grep -q traf_pal_pekniety && ok "E pal w tulowiu peka po $((N+1)) ciosach (zgarbiony)" || zle "E pal: $T"

echo "== F. OBUCH w kolano (nisko = plaski zamach z boku): noga zostaje, przyklek, kulenie x0.7"
nowy_cel; bokiem; V0=$(predk)
R=$(bij noga_l:1 obuch $BULAWA 6 "'noga_l' in d['zgniecione']"); N=${R%%|*}; T=${R#*|}; echo "  po $N: $T"
py "'noga_l' in d['zgniecione'] and d['odciete']==[]" "$T" && ok "F kolano zmiazdzone po $N ciosach" || zle "F: $T"
G=$(tagi); echo "$G" | grep -q traf_zachwiany && echo "$G" | grep -q traf_noga_chora && ok "F przyklek (zachwiany) + noga chora" || zle "F tagi: $G"
sleep 1.8   # predkosc czytana po przyklekuniu (ogluszenie zeruje ruch)
stan | grep -q 'noga_l=1' && ok "F noga na miejscu" || zle "F noga znikla"
V1=$(predk); python3 -c "import sys;a,b=float(sys.argv[1]),float(sys.argv[2]);sys.exit(0 if abs(b-a*.7)<0.01 else 1)" "$V0" "$V1" && ok "F predkosc x0.7 ($V0 -> $V1)" || zle "F predkosc $V0 -> $V1"
echo "  dalej mieczem w ta sama noge (odciecie po zmiazdzeniu)"
RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 0 0" >/dev/null; sleep 1   # od tylu: od przodu noge zaslania kosa
R=$(bij noga_l:1 lekki $MIECZ "" 5 "'noga_l' in d['odciete']"); N=${R%%|*}; T=${R#*|}
V2=$(predk); py "'noga_l' in d['odciete']" "$T" && python3 -c "import sys;a,b=float(sys.argv[1]),float(sys.argv[2]);sys.exit(0 if abs(b-a*.45)<0.01 else 1)" "$V0" "$V2" && ok "F odcieta po $N, predkosc x0.45 zastapila x0.7" || zle "F odciecie: $T, $V0 -> $V2"

echo "== G. CIECIE nogi: odcieta, x0.45"
nowy_cel; bokiem; V0=$(predk); R=$(bij noga_l:1 lekki $MIECZ "" 6 "'noga_l' in d['odciete']"); N=${R%%|*}; T=${R#*|}; V1=$(predk)
py "'noga_l' in d['odciete']" "$T" && python3 -c "import sys;a,b=float(sys.argv[1]),float(sys.argv[2]);sys.exit(0 if abs(b-a*.45)<0.01 else 1)" "$V0" "$V1" && ok "G noga odcieta po $N, x0.45" || zle "G: $T $V0 -> $V1"

echo "== H. KOSISKO: bulawa lamie, topor przerabuje, miecz ledwo nacina"
nowy_cel; R=$(bij kosisko obuch $BULAWA 5 "'kosisko' in d['odciete']"); NB=${R%%|*}; T=${R#*|}; echo "  bulawa po $NB: $T"
S=$(stan); py "'kosisko' in d['odciete']" "$T" && echo "$S" | grep -q 'kikut_kosiska=1' && echo "$S" | grep -q 'ostrze=0' && ok "H bulawa lamie kosisko po $NB (kikut w dloni, ostrze spada)" || zle "H bulawa: $T"
tagi | grep -q traf_kosa_zlamana && ok "H tag traf_kosa_zlamana (dzgniecie kikutem)" || zle "H brak tagu"
nowy_cel; R=$(bij kosisko topor $TOPOR 5 "'kosisko' in d['odciete']"); NT=${R%%|*}; T=${R#*|}
py "'kosisko' in d['odciete']" "$T" && [ "$NT" = 1 ] && ok "H topor przerabuje kosisko jednym ciosem" || zle "H topor po $NT: $T"
nowy_cel; R=$(bij kosisko pchniecie $MIECZ "" 3 "False"); T=${R#*|}
R=$(bij kosisko lekki $MIECZ "" 8 "'kosisko' in d['odciete']"); NM=${R%%|*}; T=${R#*|}
py "'kosisko' in d['odciete']" "$T" && [ "$NM" -ge 4 ] && ok "H miecz nacina kosisko dopiero po $NM ciosach" || zle "H miecz po $NM: $T"

echo "== I. ZACHWIANIE: pasek ladowany ciosami, prog rosnie"
nowy_cel; T=$(cios glowa obuch+mocny $BULAWA); tagi | grep -q traf_zachwiany && zle "I jeden cios juz zachwial" || ok "I jeden mocny cios nie wystarcza (zachw $(pole "$T" "['zachw']"))"
T=$(cios glowa obuch $BULAWA); tagi | grep -q traf_zachwiany && ok "I drugi cios w glowe: zachwiany" || zle "I brak zachwiania: $T"
sleep 1.8; tagi | grep -q traf_zachwiany && zle "I zachwianie nie minelo" || ok "I zachwianie minelo"
T=$(cios glowa obuch $BULAWA); T=$(cios glowa obuch $BULAWA); tagi | grep -q traf_zachwiany && zle "I prog nie urosl" || ok "I drugi raz 2 ciosy nie starczaja (prog x1.5)"
nowy_cel; for n in 1 2 3; do T=$(cios glowa pchniecie $MIECZ); done; tagi | grep -q traf_zachwiany && zle "I pchniecia zachwialy" || ok "I 3 pchniecia w glowe nie zachwieja"

echo "== J. kapelusz: ciecie rozcina, obuch straca, grot przebija ronda"
nowy_cel; T=$(cios kapelusz pchniecie $MIECZ); py "'kapelusz' not in d['odciete']" "$T" && ok "J grot przebija rondo, kapelusz zostaje" || zle "J pchniecie: $T"
T=$(cios kapelusz obuch $BULAWA); py "'kapelusz' in d['odciete']" "$T" && ok "J obuch straca kapelusz" || zle "J obuch: $T"

echo "== K. lancuch ciecia i utykanie (bez zmian)"
nowy_cel; T=$(cios noga_r:1 lekki minecraft:iron_sword); py "d['utknal'] is not None" "$T" && ok "K lekkie nisko z przodu utyka ($(pole "$T" "['utknal']"))" || zle "K: $T"
RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 0 0" >/dev/null; sleep 1
T=$(cios noga_r:1 mocny minecraft:iron_sword); py "'noga_r' in d['czesci'] and 'noga_l' in d['czesci'] and d['utknal'] is None" "$T" && ok "K mocne od tylu przez obie nogi" || zle "K tyl: $T"
RC "minecraft:effect clear TrafBot" >/dev/null

echo "== L. smiertelny cios konczy walke bez bledow"
nowy_cel; RC "minecraft:data merge entity @e[type=vindicator,limit=1] {Health:3f}" >/dev/null
T=$(cios reka_l lekki $MIECZ); echo "  $T"
py "d['smierc']" "$T" && ok "L cios smiertelny" || zle "L: $T"

echo "== N. ZDERZENIE broni: cios w kose nie rani moba"
nowy_cel; RC "minecraft:tag @e[type=vindicator,limit=1] add traf_zamach" >/dev/null
T=$(cios ostrze lekki $MIECZ); echo "  $T"
py "d['zderzenie']=='wybity' and d['po']==0" "$T" && ok "N w zapowiedzi zamachu: kosa wybita, 0 obrazen" || zle "N wybity: $T"
tagi | grep -q traf_wybity && ok "N tag traf_wybity (zamach przepada)" || zle "N brak traf_wybity"
RC "minecraft:tag @e[type=vindicator,limit=1] remove traf_zamach" >/dev/null; RC "minecraft:tag @e[type=vindicator,limit=1] remove traf_wybity" >/dev/null
T=$(cios ostrze lekki $MIECZ); py "d['zderzenie']=='odtracona' and d['po']==0" "$T" && tagi | grep -q traf_kosa_odtracona && ok "N poza zamachem: kosa odtracona" || zle "N odtracona: $T"
nowy_cel; RC "minecraft:tag @e[type=vindicator,limit=1] add traf_zamach" >/dev/null
T=$(cios ostrze obuch+mocny $BULAWA); echo "  $T"
py "d['zderzenie']=='wytracona'" "$T" && tagi | grep -q traf_bez_kosy && stan | grep -q 'kosisko=0' && ok "N mocny obuch w zapowiedzi wytraca kose z rak" || zle "N wytracona: $T"

echo "== N2. MythicMobs ustawia tagi zamachu (prawdziwe AI)"
nowy_cel; RC "minecraft:data merge entity @e[type=vindicator,limit=1] {NoAI:0b}" >/dev/null
RC "minecraft:gamemode survival TrafBot" >/dev/null; RC "minecraft:effect give TrafBot resistance 60 4 true" >/dev/null
RC "minecraft:teleport TrafBot 0.5 -59 2.5" >/dev/null
Z1=0; Z2=0; for i in $(seq 1 60); do G=$(tagi); echo "$G" | grep -q traf_zamach && Z1=1; echo "$G" | grep -q traf_kosa_w_ziemi && Z2=1; [ $Z2 = 1 ] && break; sleep .15; done
[ $Z1 = 1 ] && [ $Z2 = 1 ] && ok "N2 zapowiedz (traf_zamach) i kosa w ziemi widziane" || zle "N2 zamach=$Z1 w_ziemi=$Z2"
RC "minecraft:gamemode creative TrafBot" >/dev/null

echo "== P. PEDY Szczepu: slaby punkt i zrodlo zycia"
nowy_cel; T=$(cios pedy lekki $MIECZ); echo "  $T"
py "d['glowna']=='pedy' and abs(d['po']-d['obr']*1.5)<0.3" "$T" && ok "P ciecie w pedy x1.5" || zle "P: $T"
R=$(bij pedy lekki $MIECZ "" 4 "'pedy' in d['odciete']"); T=${R#*|}
sleep 0.8; stan | grep -q "odciete=\[[^]]*pedy" && tagi | grep -q traf_ospaly && ok "P przeciete pedy: Kosiarz ospaly na stale" || zle "P ospaly: $(stan | grep -oE 'odciete=[^ ]*') / $(tagi)"
nowy_cel; R=$(bij kapelusz obuch $BULAWA "" 3 "'kapelusz' in d['odciete']")   # glowa kiwa sie w spoczynku: rondo albo glowa; sleep .5; stan | grep -q 'kapelusz=0' && stan | grep -q 'pedy=1' && ok "P straca kapelusz, pedy zostaja na glowie" || zle "P kapelusz: $(stan)"

echo "== O. OGIEN: pali sie trafiona czesc, przechodzi dalej, przepala noge"
nowy_cel; RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 0 0" >/dev/null; sleep 1   # od tylu: kosa nie zaslania nog
T=$(cios noga_l:1 lekki $MIECZ); echo "  trafiona: $(pole "$T" "['glowna']")"
RC "minecraft:data merge entity @e[type=vindicator,limit=1] {Fire:240s}" >/dev/null; sleep 1.2
S=$(stan); echo "$S" | grep -oE 'plonie=\[[^]]*\]'
echo "$S" | grep -qE 'plonie=\[[^]]*noga_l' && ok "O zapala sie trafiona noga" || zle "O start: $S"
sleep 3; S=$(stan); echo "$S" | grep -qE 'plonie=\[[^]]*tulow' && ok "O ogien przeszedl na tulow" || zle "O rozprzestrzenianie: $(echo "$S" | grep -oE 'plonie=\[[^]]*\]')"
F=$(RC "minecraft:data get entity @e[type=vindicator,limit=1] Fire" | grep -oE '[-0-9]+s$'); [ "${F%s}" -le 0 ] 2>/dev/null && ok "O vanillowy ogien bazy przejety (Fire=$F), pala sie czesci" || zle "O Fire bazy: $F"
for i in $(seq 1 14); do stan | grep -q "odciete=\[[^]]*noga_l" && break; sleep .5; done
stan | grep -q "odciete=\[[^]]*noga_l" && stan | grep -q 'kikut_noga_l=1' && ok "O noga przepalona (kikut)" || zle "O przepalenie: $(stan | grep -oE 'spalenie=\{[^}]*')"
RC "minecraft:data merge entity @e[type=vindicator,limit=1] {Fire:0s}" >/dev/null

echo "== W. MROZ + deszcz: mokra slome mroz kruszy, obuch roztrzaskuje (mob jeszcze nie bity: sledzony od pojawienia)"
nowy_cel; bokiem; RC "minecraft:weather rain" >/dev/null; sleep 8   # deszcz rozkreca sie kilka sekund
RC "minecraft:data merge entity @e[type=vindicator,limit=1] {TicksFrozen:1400}" >/dev/null; sleep 1   # ok. 30 s mrozu
tagi | grep -q traf_zamarzniety && ok "W mokry i zamrozony: zamarzniety" || zle "W tagi: $(tagi)"
R=$(bij noga_l:1 obuch $BULAWA 4 "'noga_l' in d['odciete']"); N=${R%%|*}; T=${R#*|}
py "'noga_l' in d['odciete'] and d['zgniecione']==[]" "$T" && ok "W zamarznieta noga roztrzaskana po $N ciosach (zamiast zlamana)" || zle "W: $T"
RC "minecraft:weather clear" >/dev/null; RC "minecraft:data merge entity @e[type=vindicator,limit=1] {TicksFrozen:0}" >/dev/null

echo "== T. TRUCIZNA na pedach, OSLEPIENIE oczu"
nowy_cel; RC "minecraft:effect give @e[type=vindicator,limit=1] poison 5 0" >/dev/null; sleep 1.2
tagi | grep -q traf_ospaly && ok "T trucizna: pedy wiedna, ospaly" || zle "T: $(tagi)"
RC "minecraft:effect clear @e[type=vindicator,limit=1]" >/dev/null; sleep 1.2
tagi | grep -q traf_ospaly && zle "T ospalosc nie minela" || ok "T po truciznie ospalosc mija"
RC "minecraft:effect give @e[type=vindicator,limit=1] blindness 5 0" >/dev/null; sleep 1.2
tagi | grep -q traf_slepy && ok "T oslepienie: oczy przygasaja (traf_slepy)" || zle "T slepy: $(tagi)"

echo "== B. BRON MMOItems rozpoznana po TYPIE: sztylet (DAGGER) z materialu miecza = pchniecie, nie ciecie"
nowy_cel; RC "minecraft:item replace entity TrafBot weapon.mainhand with minecraft:air" >/dev/null
RC "mi give DAGGER SZTYLET_REKRUTA TrafBot 1" >/dev/null; sleep 1.8
T=$(RC "trafienia rodzaj TrafBot"); echo "  $T"
echo "$T" | grep -q 'typ=DAGGER -> pchniecie' && ok "B SZTYLET_REKRUTA (IRON_SWORD, typ DAGGER) = pchniecie" || zle "B: $T"
RC "minecraft:item replace entity TrafBot weapon.mainhand with minecraft:iron_sword" >/dev/null
RC "trafienia rodzaj TrafBot" | grep -q 'typ=null -> ciecie' && ok "B zwykly zelazny miecz = ciecie" || zle "B zwykly: $(RC "trafienia rodzaj TrafBot")"

J=/home/dev/.claude/jobs/16b50503/tmp
klik() {  # klik <czesc>: drugi bot podchodzi i klika PRAWDZIWYM pakietem ataku w encje przy czesci; wypisuje log bota
  local PKT; PKT=$(RC "trafienia punkt $1" | grep -oE '[-0-9.]+ [-0-9.]+ [-0-9.]+$')
  /opt/docker/mc/narzedzia/bot/run.sh trafienia "$H/klik.js" TrafKlik $PKT > $J/klik.log 2>&1 & local KP=$!
  for i in $(seq 1 40); do RC "minecraft:list" | grep -q TrafKlik && break; sleep 0.5; done
  RC "minecraft:teleport TrafKlik 1.5 -59 1.8 0 0" >/dev/null; RC "minecraft:gamemode creative TrafKlik" >/dev/null
  wait $KP; cat $J/klik.log
}

echo "== H2. POD-HITBOX: prawdziwe klikniecie gracza w ostrze kosy (poza hitboxem bazy)"
nowy_cel; RC "minecraft:teleport TrafBot -6 -59 -6" >/dev/null
n0=$(grep -ac 'TRAF {' "$L"); KL=$(klik ostrze); echo "$KL" | grep -E 'ENCJE|PASS|FAIL' | head -3
T=$(grep -a 'TRAF {' "$L" | tail -n +$((n0+1)) | grep -a TrafKlik | tail -1 | sed 's/.*TRAF //'); [ -n "$T" ] || T='{}'; echo "  $T" | cut -c1-200
py "d['zderzenie'] is not None and d['po']==0" "$T" && ok "H2 klik w ostrze trafia kose (zderzenie $(pole "$T" "['zderzenie']"), 0 obrazen)" || zle "H2: $T"
prawy_bok
R=$(bij reka_r:0:0.4 lekki $MIECZ "" 4 "'reka_r' in d['odciete']"); RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 180 0" >/dev/null
RC "minecraft:teleport TrafBot -6 -59 -6" >/dev/null; n0=$(grep -ac 'TRAF {' "$L"); KL=$(klik ostrze)
T=$(grep -a 'TRAF {' "$L" | tail -n +$((n0+1)) | grep -a TrafKlik | tail -1)
echo "$KL" | grep -q 'brak encji pod-hitboxu' && [ -z "$T" ] && ok "H2 po odcieciu reki pod-hitbox kosy znika (klik w puste miejsce nic nie trafia)" || zle "H2 duch kosy: $(echo "$KL" | grep ENCJE) / $T"
RC "minecraft:teleport TrafBot 0.5 -59 1.3 0 0" >/dev/null

echo "== Z. ZAKLECIA: prawdziwe trafienia Plomieni i Odmrozenia (MythicLib, rzuca gracz)"
nowy_cel; n0=$(grep -ac 'TRAF {' "$L"); RC "trafienia zaklecie TrafBot fire 4" >/dev/null; sleep 0.7   # Plomienie pala 2 s (ignite 40)
T=$(grep -a 'TRAF {' "$L" | tail -n +$((n0+1)) | tail -1 | sed 's/.*TRAF //'); [ -n "$T" ] || T='{}'; echo "  $T" | cut -c1-220
py "d['zrodlo']=='zaklecie' and 'fire' in d['zywioly'] and abs(d['po']-d['obr'])<0.01" "$T" && ok "Z Plomienie: zaklecie, ogien, bez mnoznika miejsca" || zle "Z ogien: $T"
tagi | grep -q traf_plonie && stan | grep -qE 'plonie=\[[a-z]' && ok "Z zaklecie zapalilo czesc ($(stan | grep -oE 'plonie=\[[^]]*\]' | head -1))" || zle "Z ogien nie zapalil: $(tagi)"
nowy_cel; n0=$(grep -ac 'TRAF {' "$L"); RC "trafienia zaklecie TrafBot ice 3.5" >/dev/null; sleep 1.5
T=$(grep -a 'TRAF {' "$L" | tail -n +$((n0+1)) | tail -1 | sed 's/.*TRAF //'); [ -n "$T" ] || T='{}'
py "d['zrodlo']=='zaklecie' and 'ice' in d['zywioly']" "$T" && tagi | grep -q traf_zmrozony && ok "Z Odmrozenie: lod, zmrozony (ikona w PaskiMobow)" || zle "Z lod: $T / $(tagi)"

echo "== LR. LEWA REKA (DwieBronie): drugi cios serii idzie lewa, luk od lewej"
nowy_cel; RC "mmocore admin attribute give TrafBot bieglosc-sztylet 1" >/dev/null
RC "minecraft:clear TrafBot" >/dev/null; RC "mi give DAGGER SZTYLET_REKRUTA TrafBot 1" >/dev/null
RC "minecraft:item replace entity TrafBot weapon.offhand from entity TrafBot weapon.mainhand" >/dev/null; RC "mi give DAGGER SZTYLET_REKRUTA TrafBot 1" >/dev/null; sleep 1
n0=$(grep -ac 'TRAF {' "$L"); for n in 1 2; do RC "trafienia celuj TrafBot tulow:1" >/dev/null; RC "trafienia cios TrafBot" >/dev/null; sleep 0.6; done; sleep .5
T1=$(grep -a 'TRAF {' "$L" | tail -n +$((n0+1)) | sed -n 1p | sed 's/.*TRAF //'); T2=$(grep -a 'TRAF {' "$L" | tail -n +$((n0+1)) | sed -n 2p | sed 's/.*TRAF //'); [ -n "$T1" ] || T1='{}'; [ -n "$T2" ] || T2='{}' 
py "d['lewa']" "$T2" && ! py "d['lewa']" "$T1" && ok "LR prawa, potem lewa (znacznik DwieBronie czytany)" || zle "LR: $(pole "$T1" "['lewa']") / $(pole "$T2" "['lewa']")"
RC "minecraft:clear TrafBot" >/dev/null

echo "== CH. CHOD z chorej nogi"
nowy_cel; bokiem; R=$(bij noga_l:1 obuch $BULAWA 7 "'noga_l' in d['zgniecione']"); sleep .3
stan | grep -q 'chod=walk_kolano_l' && ok "CH peknieta noga: chod walk_kolano_l" || zle "CH kolano: $(stan | grep -oE 'chod=[^ ]*')"
RC "minecraft:teleport @e[type=vindicator,limit=1] 0.5 -59 4.5 0 0" >/dev/null; sleep 1
R=$(bij noga_l:1 lekki $MIECZ "" 6 "'noga_l' in d['odciete']"); sleep .3
stan | grep -q 'chod=walk_bez_l' && ok "CH bez nogi: chod walk_bez_l" || zle "CH bez nogi: $(stan | grep -oE 'chod=[^ ]*')"

echo "== S. SMIERC z ostatniego ciosu"
nowy_cel; RC "minecraft:data merge entity @e[type=vindicator,limit=1] {Health:3f}" >/dev/null; n0=$(grep -ac 'SMIERC' "$L")
T=$(cios tulow:1 lekki $MIECZ); sleep .5
W=$(grep -a 'SMIERC' "$L" | tail -n +$((n0+1)) | tail -1); N=$(RC "execute if entity @e[type=item_display,x=0.5,y=-59,z=4.5,distance=..8]" | grep -oE '[0-9]+$')
echo "$W" | grep -q rozsypanie && [ "${N:-0}" -ge 8 ] && ok "S ciecie: rozsypuje sie na $N kawalkow (z kosa)" || zle "S ciecie: $W / kawalki ${N:-0}"
nowy_cel; RC "minecraft:data merge entity @e[type=vindicator,limit=1] {Health:3f}" >/dev/null; n0=$(grep -ac 'SMIERC' "$L")
T=$(cios tulow:1 obuch $BULAWA); sleep .5
grep -a 'SMIERC' "$L" | tail -n +$((n0+1)) | grep -q death_oklapniecie && ok "S obuch: oklapniecie" || zle "S obuch: $(grep -a SMIERC "$L" | tail -1)"
nowy_cel; RC "minecraft:data merge entity @e[type=vindicator,limit=1] {Health:2f}" >/dev/null; n0=$(grep -ac 'SMIERC' "$L")
RC "minecraft:data merge entity @e[type=vindicator,limit=1] {Fire:200s}" >/dev/null; sleep 4
grep -a 'SMIERC' "$L" | tail -n +$((n0+1)) | grep -q death_zweglenie && ok "S ogien: zweglenie" || zle "S ogien: $(grep -a SMIERC "$L" | tail -1)"

echo "== OB. OBRAZENIA OBSZAROWE: raz, bez kosy jako tarczy, pod-hitboxy nie dubluja"
nowy_cel; H0=$(RC "minecraft:data get entity @e[type=vindicator,limit=1] Health" | grep -oE '[0-9.]+f$' | tr -d f)
RC "trafienia obszar TrafBot 6 5 ml" >/dev/null; sleep 1; H1=$(RC "minecraft:data get entity @e[type=vindicator,limit=1] Health" | grep -oE '[0-9.]+f$' | tr -d f)
RC "trafienia obszar TrafBot 6 5 bukkit" >/dev/null; sleep .5; H2=$(RC "minecraft:data get entity @e[type=vindicator,limit=1] Health" | grep -oE '[0-9.]+f$' | tr -d f)
python3 -c "import sys;a,b,c=map(float,sys.argv[1:]);sys.exit(0 if abs(a-b-5)<.01 and abs(b-c-5)<.01 else 1)" "$H0" "$H1" "$H2" && ok "OB MythicLib i Bukkit: po 5 HP raz ($H0 -> $H1 -> $H2)" || zle "OB HP $H0 -> $H1 -> $H2"

echo "== M. koszt i log"
python3 - "$L" <<'PY' && ok "M czas obslugi trafienia OK" || zle "M za wolno"
import json,sys
r=[json.loads(l.split('TRAF ',1)[1]) for l in open(sys.argv[1],errors='ignore') if 'TRAF {' in l and 'us_luk' in l]
us=sorted(x['us'] for x in r); ul=sorted(x['us_luk'] for x in r)
print(f"  trafien {len(us)}: handler mediana {us[len(us)//2]} us (max {us[-1]}), luk mediana {ul[len(ul)//2]} us (max {ul[-1]})")
sys.exit(0 if us and us[len(us)//2] < 1500 and ul[len(ul)//2] < 300 else 1)
PY
E=$(grep -aE 'Trafienia|trafienia|l004|L004_KOSIARZ' "$L" | grep -aEc 'ERROR|Exception|WARN')
[ "$E" = 0 ] && ok "M 0 bledow/ostrzezen w logu" || { zle "M bledy: $E"; grep -aE -A5 '(ERROR|WARN|Exception).*(rafien|l004|L004)' "$L" | head -20; }

docker rm -f bot-trafienia-$BOTPID >/dev/null 2>&1
echo "WYNIK: $OK PASS, $ZLE FAIL"
