"""Wspolne sciezki kuzni lochow. Kuznia = klon world1 z klon.sh (`klon.sh start kuznia --z-iris`).
KUZNIA (env) = nazwa klonu (domyslnie kuznia) -> kontener klon-<KUZNIA>, dane /opt/docker/mc/klony/<KUZNIA>/data.
KUZNIA_LEVELS (env) = katalog wynikow (domyslnie kuznia/levels obok tools/ w repo MC)."""
import os

TOOLS = os.path.dirname(os.path.abspath(__file__))
KUZNIA = os.environ.get("KUZNIA", "kuznia")
KONTENER = "klon-" + KUZNIA
DATA = f"/opt/docker/mc/klony/{KUZNIA}/data"
LEVELS = os.environ.get("KUZNIA_LEVELS", os.path.join(os.path.dirname(TOOLS), "levels"))
