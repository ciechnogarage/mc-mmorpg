#!/usr/bin/env bash
# Wizualne scenariusze mapy na klonie mapa-proba z klientem QA mc-client-mapa (gracz MapaQA).
# Kazdy krok: stan serwera (slot 7 + przedmiot w rece: map_id) + zrzut + ocena obrazu (ocena.py) -> wyniki.tsv + arkusz.
set -u
TU="$(cd "$(dirname "$0")" && pwd)"; OUT=${OUT:-/tmp/mapa-wizualne}; CAP=${CAP:?katalog captures klienta}; mkdir -p $OUT
R(){ docker exec mapa-proba rcon-cli "$1" 2>/dev/null | sed 's/\x1b\[[0-9;]*m//g'; }
X(){ docker exec -e DISPLAY=:99 mc-client-mapa xdotool "$@"; }
: > $OUT/wyniki.tsv; N=0
mapid(){ R "data get entity MapaQA $1" | grep -o '[0-9]*$' ; }
sprawdz(){ # $1 nazwa, $2.. tryby oceny
  local nazwa=$1; shift; N=$((N+1)); local f=$(printf "%02d_%s" $N "$nazwa")
  sleep ${PAUZA:-2.5}
  # seria 3 klatek co ~150 ms: mapa ma byc widoczna w KAZDEJ (wylapuje miganie / podmiane obrazu)
  for k in 1 2 3; do docker exec -e DISPLAY=:99 mc-client-mapa import -window root /review/w_${f}_$k.png; sleep 0.1; done
  cp $CAP/w_${f}_1.png $CAP/w_$f.png
  local s7=$(mapid 'Inventory[{Slot:7b}].components."minecraft:map_id"'); local reka=$(mapid 'SelectedItem.components."minecraft:map_id"')
  local wyn=""; local ok=1
  for t in "$@"; do for k in 1 2 3; do r=$(python3 $TU/ocena.py $CAP/w_${f}_$k.png $t); [[ $r == PASS* ]] || { ok=0; wyn="$wyn | klatka$k $r"; }; done
    [[ $ok = 1 ]] && wyn="$wyn | $t x3 PASS"; done
  [[ -n "$s7" ]] || ok=0
  printf "%s\t%s\tslot7=%s\treka=%s\t%s\n" "$([ $ok = 1 ] && echo PASS || echo FAIL)" "$f" "${s7:-BRAK}" "${reka:-brak}" "$wyn" | tee -a $OUT/wyniki.tsv
}
patrz_w_dol(){ R "execute as MapaQA at @s run tp @s ~ ~ ~ ~ 60" >/dev/null; }
reka_mapa(){ X key 8; sleep 0.6; patrz_w_dol; }

R "time set day" >/dev/null; R "execute in minecraft:stolica_odnowa run time set day" >/dev/null; R "minecraft:gamerule advance_time false" >/dev/null; R "weather clear" >/dev/null
R "spreadplayers 149 197 0 1 false MapaQA" >/dev/null; sleep 2
reka_mapa;                                        sprawdz start reka minimapa
X keydown w; X keydown ctrl; sleep 10; X keyup ctrl; X keyup w; patrz_w_dol;   sprawdz po_marszu_40bl reka minimapa
for z in 1 2 3 4 5 6; do X click 3; sleep 0.8; patrz_w_dol; sprawdz zoom_$z reka; done
R "minecraft:kill MapaQA" >/dev/null; sleep 2; X mousemove 640 426 click --delay 200 1; sleep 3; R "spreadplayers 149 197 0 1 false MapaQA" >/dev/null
reka_mapa;                                        sprawdz po_smierci reka minimapa
R "execute in minecraft:stolica_odnowa run tp MapaQA 0.5 101 40.5 0 60" >/dev/null; sleep 3; reka_mapa; sprawdz stolica reka minimapa
R "execute in minecraft:overworld run tp MapaQA 149 120 197 0 60" >/dev/null; R "spreadplayers 149 197 0 1 false MapaQA" >/dev/null; sleep 3; reka_mapa; sprawdz powrot_kraina reka minimapa
X keydown shift; sleep 0.4; X click 3; sleep 0.3; X keyup shift; sleep 2;      sprawdz pelna_otwarta pelna
X keydown shift; sleep 0.3; X keyup shift; sleep 1; reka_mapa;                  sprawdz po_pelnej reka
R "mapa otworz MapaQA" >/dev/null; sleep 2;       sprawdz atlas_z_panelem
X key Escape; sleep 1; reka_mapa;                 sprawdz po_atlasie reka
X key q; sleep 1; reka_mapa;                      sprawdz po_q reka
X key f; sleep 1; X key 8; reka_mapa;             sprawdz po_f reka
X key e; sleep 1; X mousemove 800 552 click --delay 200 1; sleep 2; sprawdz klik_slot7_atlas
X key Escape; sleep 1; reka_mapa;                 sprawdz po_kliku_slot7 reka
echo "== $(grep -c ^PASS $OUT/wyniki.tsv) PASS / $(grep -c ^FAIL $OUT/wyniki.tsv) FAIL"
