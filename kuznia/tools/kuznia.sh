# Wspolne sciezki kuzni lochow (odpowiednik kuznia_sciezki.py). Uzycie w skrypcie: . "<tools>/kuznia.sh"
# KUZNIA = nazwa klonu z klon.sh (domyslnie kuznia) -> kontener klon-$KUZNIA, dane /opt/docker/mc/klony/$KUZNIA/data.
TOOLS=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
KUZNIA=${KUZNIA:-kuznia}
KONTENER=klon-$KUZNIA
DATA=/opt/docker/mc/klony/$KUZNIA/data
LEVELS=${KUZNIA_LEVELS:-$(dirname "$TOOLS")/levels}
