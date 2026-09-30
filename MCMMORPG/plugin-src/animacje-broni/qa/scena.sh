#!/bin/bash
# scena.sh <TYP> <ID> -> dane komponentow + 3 zrzuty: 1. osoba spoczynek, 1. osoba w trakcie zamachu, widok z przodu.
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
$X xdotool click 1; sleep 0.12; shot b; sleep 0.8
$X xdotool key F5; sleep 0.2; $X xdotool key F5; sleep 0.6; shot c; $X xdotool key F5; sleep 0.3
