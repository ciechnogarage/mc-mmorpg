# Karta stylu grafik (ikony, przedmioty, GUI)

Wzorzec: mikstura `output/warsztat/czesci/mikstura_v9_styl_v6_b.png` (zaakceptowana przez wlasciciela
2026-09-23). Kazda nowa grafika musi byc z nia spojna.

## Styl
- **Mocny ciemny obrys** dookola (`#` ~ (28,12,24)); przedmiot ma byc zwarta, ciezka bryla czytelna w slocie.
- **Masa i nasycenie** — nie "vanillowe" blade, przezroczyste formy.
- **Jedno swiatlo z lewej-gory** we wszystkich grafikach.
- **3-5 tonow na material**, sasiednie tony roznia sie jasnoscia Y o min. ~20 (bez prawie-duplikatow).
- **Cien jako luk** (nie prosta 45 st.), jasna strefa **otwarta w strone swiatla** — zamkniety ciemny
  pierscien dookola jasnego srodka czyta sie jak wypukly babel.
- **Blik** krotki albo przerwany luk, nie linijka.
- **Bez ostrych skokow w srodku bryly**: jasny element nie stoi tuz przy najciemniejszym cieniu.
- Rozmiar slotu: **16x16 rysowane natywnie**, nie skalowane z wiekszej wersji.

## Warsztat (na kazda grafike)
1. Sylwetka -> walory w szarosci -> materialy -> swiatlo.
2. Poprawki **po jednej czesci**; po kazdym kroku: zoom obok poprzedniej wersji,
   `tools/pixelart/lint.py` (sieroty, duplikaty, zlewanie z tlem), `tools/pixelart/kontrast.py`
   (skoki jasnosci > 60 wewnatrz bryly), straz sylwetki. I OGLADAC — pomiar nie lapie wszystkiego.
3. Arkusz porownawczy -> **wybiera wlasciciel**. Recenzent tylko po liste poprawek.
4. **Jedna grafika naraz**; nastepna dopiero po akceptacji poprzedniej.

Paleta materialow: `tools/pixelart/paleta.py`.
