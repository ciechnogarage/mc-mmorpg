#!/usr/bin/env bash
# Pelna seria Bagien: wszystkie.sh <wersja> [POWT=4] [filtr regex moba]. Ustawienia aren per mob ponizej.
H=$(dirname "$(readlink -f "$0")"); W="$1"; P="${2:-4}"; F="${3:-.}"
run() { echo "$1" | grep -qE "$F" || return 0; env "${@:3}" "$H/seria.sh" "$W" "$1" "$P" "$2"; }
run BAGNA_PIJAWKA     3 SPAWN=-10,-61,0 START=-3.5,-60,0.5
run BAGNA_MULOWIEC    1
run BAGNA_KOSCIEJ     1 SPAWN=0.5,-60,7.5 START=0.5,-60,-6.5 STOJ_S=16
run BAGNA_SZEPTUCHA   1
run BAGNA_KOKONIARA   1 LIMIT_S=120
run BAGNA_KOKONIATKO  4
run BAGNA_RECHOTNIK   1 START=0.5,-60,-8.5
run BAGNA_SLIMAK      1
run BAGNA_OGNIK       1 SPAWN=0.5,-58.5,4.5
run BAGNA_KROL_ROPUCH 1 LIMIT_S=150
