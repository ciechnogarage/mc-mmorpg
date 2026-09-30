#!/bin/bash
# scena.sh <TYP> <ID> -> dane komponentow + zrzuty: a = 1. osoba spoczynek, b1..b4 = klatki zamachu, c = widok z przodu.
# Wymaga: ab-test + mc-client-ab (gracz BronQA na serwerze). Zrzuty w /review/captures (katalog review klienta).
T=$1; I=$2; N=$(echo "$T-$I" | tr 'A-Z' 'a-z')
X="docker exec -e DISPLAY=:99 mc-client-ab"; R="docker exec ab-test rcon-cli"
$R "minecraft:clear BronQA" >/dev/null
$R "minecraft:tp BronQA 0.5 -59 0.5 0 20" >/dev/null
$X xdotool key 1; sleep 0.3
$R "mi give $T $I BronQA" >/dev/null
sleep 2.6   # przeglad rak co 2 s
echo "$T $I: $($R "animacjebroni sprawdz BronQA")"
shot(){ $X import -window root /review/captures/$N-$1.png; }
shot a
$X xdotool click 1; for k in 1 2 3 4; do shot b$k; done; sleep 0.8
$X xdotool key F5; sleep 0.2; $X xdotool key F5; sleep 0.6; shot c; $X xdotool key F5; sleep 0.3
