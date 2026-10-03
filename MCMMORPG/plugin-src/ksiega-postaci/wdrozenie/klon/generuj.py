#!/usr/bin/env python3
"""Nakładki ksiega:klon/<ns>/<ścieżka> dla klona lewej broni w off-handzie.

Właściciel 03.10: "nie duplikować w dłoni, tylko duplikować w slocie". Gdy gracz wybierze
slot 1, prawdziwa lewa broń trafia do ręki, a w off-handzie zostaje klon. Klon ma model
z tego katalogu: w GUI (hotbar, ekwipunek), na ziemi, w ramce i na półce wygląda jak broń,
w dłoniach i na głowie nie rysuje się wcale. ksiega:klon/pusty = nic nigdzie (gdy nakładki brak).

Źródła definicji: vanilla z jara klienta, zasoby packa Nexo (bez mapa/ i ksiega/)
oraz assets/nexo/items z ostatniego pack.zip (Nexo generuje je przy budowie).
Użycie: generuj.py <client.jar> <plugins/Nexo/pack> <katalog wyjściowy assets/ksiega>
"""
import json, os, shutil, sys, zipfile

WIDOCZNY = ["gui", "ground", "fixed", "on_shelf"]
POMIN = {"mapa", "ksiega"}


def nakladka(definicja):
    wynik = {k: v for k, v in definicja.items() if k != "model"}
    wynik["model"] = {
        "type": "minecraft:select",
        "property": "minecraft:display_context",
        "cases": [{"when": WIDOCZNY, "model": definicja["model"]}],
        "fallback": {"type": "minecraft:empty"},
    }
    return wynik


def definicje(klient, pack):
    with zipfile.ZipFile(klient) as z:
        for n in z.namelist():
            if n.startswith("assets/minecraft/items/") and n.endswith(".json"):
                yield "minecraft", n[len("assets/minecraft/items/"):-5], json.loads(z.read(n))
    assets = os.path.join(pack, "assets")
    for ns in sorted(os.listdir(assets)):
        katalog = os.path.join(assets, ns, "items")
        if ns in POMIN or not os.path.isdir(katalog):
            continue
        for root, _, pliki in os.walk(katalog):
            for p in sorted(pliki):
                if p.endswith(".json"):
                    sciezka = os.path.relpath(os.path.join(root, p), katalog)[:-5].replace(os.sep, "/")
                    with open(os.path.join(root, p), encoding="utf-8") as f:
                        yield ns, sciezka, json.load(f)
    zip_packa = os.path.join(pack, "pack.zip")
    if os.path.exists(zip_packa):
        with zipfile.ZipFile(zip_packa) as z:
            for n in z.namelist():
                if n.startswith("assets/nexo/items/") and n.endswith(".json"):
                    yield "nexo", n[len("assets/nexo/items/"):-5], json.loads(z.read(n))


def main(klient, pack, wyjscie):
    items = os.path.join(wyjscie, "items", "klon")
    if os.path.isdir(items):
        shutil.rmtree(items)
    licznik = {}
    widziane = set()
    for ns, sciezka, d in definicje(klient, pack):
        if "model" not in d or (ns, sciezka) in widziane:
            continue
        widziane.add((ns, sciezka))
        cel = os.path.join(items, ns, sciezka + ".json")
        os.makedirs(os.path.dirname(cel), exist_ok=True)
        with open(cel, "w", encoding="utf-8") as f:
            json.dump(nakladka(d), f, separators=(",", ":"))
        licznik[ns] = licznik.get(ns, 0) + 1
    with open(os.path.join(items, "pusty.json"), "w", encoding="utf-8") as f:
        json.dump({"model": {"type": "minecraft:empty"}}, f)
    print("nakladki:", ", ".join(f"{k}={v}" for k, v in sorted(licznik.items())))


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(*sys.argv[1:])
