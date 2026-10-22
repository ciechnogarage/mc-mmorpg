#!/usr/bin/env python3
"""Niezalezna kontrola stanu ekonomii/tresci world1.
Uruchamiac w kontenerze z pyyaml:
  cd /opt/docker/mc/data/world1/plugins && docker run --rm \
    -v "$PWD:/w:ro" -v /home/dev/Desktop/Przemek/MC/tools/weryfikacja:/t:ro -w /w \
    python:3.12-alpine sh -c "pip install -q pyyaml >/dev/null 2>&1; python /t/sprawdz.py"
"""
import glob, os, re, sys
import yaml

P = "."
problemy, uwagi = [], []

def wczytaj(sciezka):
    try:
        with open(sciezka, encoding="utf-8") as f:
            return yaml.safe_load(f) or {}
    except Exception as e:
        problemy.append(f"YAML nie parsuje: {sciezka}: {str(e)[:150]}")
        return None

# --- 1. skladnia wszystkich istotnych YAML-i -------------------------------
wzorce = ["MMOItems/item/*.yml", "MMOItems/*.yml", "MMOItems/crafting-stations/*.yml",
          "MMOCore/professions/*.yml", "MMOCore/gui/*.yml",
          "MythicMobs/mobs/*.yml", "MythicMobs/droptables/*.yml",
          "MythicMobs/randomspawns/*.yml", "MythicMobs/skills/*.yml",
          "Nexo/items/*.yml", "Nexo/items/**/*.yml"]
pliki = sorted({p for w in wzorce for p in glob.glob(os.path.join(P, w), recursive=True)})
for p in pliki:
    wczytaj(p)

# --- 2. katalog naszych przedmiotow ----------------------------------------
itemy = {}   # TYP -> {id}
for p in glob.glob(os.path.join(P, "MMOItems/item/*.yml")):
    typ = os.path.basename(p)[:-4].upper()
    d = wczytaj(p) or {}
    itemy[typ] = set(d.keys())
wszystkie_id = {i for s in itemy.values() for i in s}

# --- 3. receptury warsztatow ------------------------------------------------
wytwarzane, potrzebne, stacje = set(), {}, 0
for p in glob.glob(os.path.join(P, "MMOItems/crafting-stations/*.yml")):
    d = wczytaj(p) or {}
    if "recipes" not in d:
        continue
    stacje += 1
    for rid, r in (d.get("recipes") or {}).items():
        wy = str(r.get("output", ""))
        for i in re.findall(r"id=([A-Z0-9_]+)", wy):
            wytwarzane.add(i)
            mt = re.search(r"type=([A-Z_]+)", wy)
            if mt and i not in itemy.get(mt.group(1), set()):
                problemy.append(f"{os.path.basename(p)}/{rid}: wytwarza {i}, ktorego nie ma w typie {mt.group(1)}")
        for skl in r.get("ingredients", []):
            if "vanilla{" in skl:
                problemy.append(f"vanillowy skladnik w {os.path.basename(p)} / {rid}: {skl}")
            for i in re.findall(r"id=([A-Z0-9_]+)", skl):
                potrzebne.setdefault(i, []).append(f"{os.path.basename(p)}/{rid}")
                if i not in wszystkie_id:
                    problemy.append(f"nieistniejacy skladnik {i} w {os.path.basename(p)}/{rid}")

# --- 4. dropy MMOItems (plugin ich NIE waliduje) ---------------------------
zbierane = set()
d = wczytaj(os.path.join(P, "MMOItems/drops.yml")) or {}
for sekcja in ("blocks", "monsters", "customblocks"):
    for zrodlo, tabele in (d.get(sekcja) or {}).items():
        for tn, t in (tabele or {}).items():
            for typ, poz in (t.get("items") or {}).items():
                for i in poz:
                    zbierane.add(i)
                    if i not in itemy.get(typ, set()):
                        problemy.append(f"drops.yml {sekcja}/{zrodlo}/{tn}: {i} nie istnieje w typie {typ}")

# --- 5. lowienie MMOCore ----------------------------------------------------
fish = (wczytaj(os.path.join(P, "MMOCore/professions/fishing.yml")) or {}).get("on-fish") or {}
for t in fish.values():
    for poz in t.get("items", []):
        zbierane |= set(re.findall(r"id=([A-Z0-9_]+)", poz))

# --- 6. dropy MythicMobs ----------------------------------------------------
mm_pliki = glob.glob(os.path.join(P, "MythicMobs/mobs/*.yml")) + glob.glob(os.path.join(P, "MythicMobs/droptables/*.yml"))
for p in mm_pliki:
    tekst = open(p, encoding="utf-8", errors="replace").read()
    for i in re.findall(r"mmoitems?\{[^}]*id=([A-Za-z0-9_]+)", tekst, re.I):
        zbierane.add(i.upper())
        if i.upper() not in wszystkie_id:
            problemy.append(f"{os.path.basename(p)}: mob dropi nieistniejacy przedmiot {i}")

# --- 7. dropy Nexo ----------------------------------------------------------
# Nexo zapisuje drop MMOItems para kluczy mmoitems_id / mmoitems_type,
# a nie skladnia klamrowa - trzeba sprawdzic oba warianty.
for p in glob.glob(os.path.join(P, "Nexo/items/**/*.yml"), recursive=True):
    tekst = open(p, encoding="utf-8", errors="replace").read()
    for i in re.findall(r"mmoitems?\{[^}]*id=([A-Za-z0-9_]+)", tekst, re.I):
        zbierane.add(i.upper())
        if i.upper() not in wszystkie_id:
            problemy.append(f"{os.path.basename(p)}: blok dropi nieistniejacy przedmiot {i}")
    d = wczytaj(p)
    if not isinstance(d, dict):
        continue
    def obejdz(w, sciezka=""):
        if isinstance(w, dict):
            mid, mtyp = w.get("mmoitems_id"), w.get("mmoitems_type")
            if mid:
                zbierane.add(str(mid).upper())
                typ = str(mtyp).upper() if mtyp else None
                if typ is None:
                    problemy.append(f"{os.path.basename(p)}{sciezka}: mmoitems_id={mid} bez mmoitems_type")
                elif str(mid).upper() not in itemy.get(typ, set()):
                    problemy.append(f"{os.path.basename(p)}{sciezka}: drop {mid} nie istnieje w typie {typ}")
            for k, v in w.items():
                obejdz(v, f"{sciezka}/{k}")
        elif isinstance(w, list):
            for n, v in enumerate(w):
                obejdz(v, f"{sciezka}[{n}]")
    obejdz(d)

# --- 7b. surowce wydawane przez wezly zasobow (komenda 'mi give') ----------
# Wezly nie uzywaja drop tables tylko komendy, wiec trzeba je parsowac osobno.
for p in glob.glob(os.path.join(P, "MythicMobs/skills/*.yml")) + glob.glob(os.path.join(P, "MythicMobs/mobs/*.yml")):
    tekst = open(p, encoding="utf-8", errors="replace").read()
    # UWAGA: kolejnosc to 'mi give <TYP> <ID> <gracz> <ilosc>' - gracz jest TRZECI.
    for typ, ident in re.findall(r"mi\s+give\s+([A-Z_]+)\s+([A-Z0-9_]+)", tekst):
        zbierane.add(ident)
        if ident not in itemy.get(typ, set()):
            problemy.append(f"{os.path.basename(p)}: wezel wydaje {ident}, ktorego nie ma w typie {typ}")

# --- 8. spojnosc lancucha ---------------------------------------------------
sieroty = sorted(k for k in potrzebne if k not in zbierane and k not in wytwarzane)
for s in sieroty:
    problemy.append(f"skladnik bez zrodla: {s} (potrzebny w {', '.join(potrzebne[s][:3])})")

# --- 9. profesje MMOCore ----------------------------------------------------
for p in sorted(glob.glob(os.path.join(P, "MMOCore/professions/*.yml"))):
    d = wczytaj(p) or {}
    if "exp-curve-formula" in d:
        problemy.append(f"{os.path.basename(p)}: martwy klucz exp-curve-formula (MMOCore czyta exp-curve)")
    if "exp-curve" not in d:
        uwagi.append(f"{os.path.basename(p)}: brak exp-curve")

# --- podsumowanie -----------------------------------------------------------
print(f"plikow YAML sprawdzonych : {len(pliki)}")
print(f"przedmiotow MMOItems     : {len(wszystkie_id)} w {len(itemy)} typach")
print(f"warsztatow / receptur    : {stacje} / {sum(len(v) for v in potrzebne.values())} uzyc skladnikow")
print(f"przedmiotow zbieranych   : {len(zbierane)}")
print(f"przedmiotow wytwarzanych : {len(wytwarzane)}")
print()
if uwagi:
    print("UWAGI:")
    for u in uwagi:
        print("  -", u)
    print()
if problemy:
    print(f"PROBLEMY ({len(problemy)}):")
    for x in problemy:
        print("  !", x)
    sys.exit(1)
print("OK - brak problemow: kazdy skladnik istnieje, kazdy da sie zdobyc, zero vanilli w warsztatach")
