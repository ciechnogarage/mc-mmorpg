#!/usr/bin/env python3
"""Wpina stat MODEL (klucz YAML 'model') do przedmiotow MMOItems PROF_*.

Dlaczego 'model', a nie 'item_model'/'custom_model_data_string':
  net.Indyuce.mmoitems.stat.ItemModel ma id stata "MODEL" i wola
  ItemMeta#setItemModel(NamespacedKey) -- czyli komponent minecraft:item_model z 1.21.2+.
  ItemStat wyprowadza sciezke w configu jako id.toLowerCase().replace('_','-'),
  wiec "MODEL" -> klucz 'model'. Potwierdza to shipowany default (custom-model-data).

Edycja jest LINIOWA (wstawka tuz pod '  base:'), a nie przez przeladowanie YAML-a,
zeby nie przeformatowac zywego pliku i nie zgubic komentarzy ani cudzyslowow.
"""
import argparse
import re
import sys

MATERIAL = "/opt/docker/mc/data/world1/plugins/MMOItems/item/material.yml"


def load(path):
    with open(path, encoding="utf-8") as f:
        return f.read().split("\n")


def find_item(lines, item_id):
    """Zwraca (idx_naglowka, idx_base) albo (None, None)."""
    head = None
    for i, ln in enumerate(lines):
        if ln == f"{item_id}:":
            head = i
            break
    if head is None:
        return None, None
    for j in range(head + 1, len(lines)):
        ln = lines[j]
        if re.match(r"^\S", ln):          # kolejny przedmiot - koniec bloku
            break
        if ln.strip() == "base:":
            return head, j
    return head, None


def set_model(lines, item_id, value):
    """Wstawia/aktualizuje 'model:' w sekcji base:. Zwraca (status, lines)."""
    head, base = find_item(lines, item_id)
    if head is None:
        return "BRAK_ITEMU", lines
    if base is None:
        return "BRAK_BASE", lines
    # czy 'model:' juz jest w tej sekcji base?
    for j in range(base + 1, len(lines)):
        ln = lines[j]
        if re.match(r"^\S", ln) or (ln.strip() and not ln.startswith("    ")):
            break
        m = re.match(r"^    model:\s*(.*)$", ln)
        if m:
            if m.group(1).strip() == value:
                return "JUZ_OK", lines
            lines[j] = f"    model: {value}"
            return "ZAKTUALIZOWANO", lines
    lines.insert(base + 1, f"    model: {value}")
    return "WSTAWIONO", lines


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("ids", nargs="+", help="id MMOItems, np. PROF_ORE_CHUNK")
    ap.add_argument("--value", help="wymus konkretna wartosc (kontrola negatywna)")
    ap.add_argument("--file", default=MATERIAL)
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()

    lines = load(a.file)
    for item_id in a.ids:
        val = a.value or "nexo:" + item_id.lower()
        status, lines = set_model(lines, item_id, val)
        print(f"{status:14} {item_id} -> {val}")
        if status in ("BRAK_ITEMU", "BRAK_BASE"):
            sys.exit(f"PRZERWANO: {item_id} = {status}")

    if a.dry_run:
        print("(dry-run, nie zapisano)")
        return
    with open(a.file, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    # kontrola: plik musi sie nadal parsowac jako YAML
    try:
        import yaml
        with open(a.file, encoding="utf-8") as f:
            d = yaml.safe_load(f)
        print(f"YAML OK, przedmiotow w pliku: {len(d)}")
    except ImportError:
        print("UWAGA: brak pyyaml na hoscie - walidacja YAML pominieta")


if __name__ == "__main__":
    main()
