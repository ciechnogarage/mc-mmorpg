# Typy ekwipunku foundation v0.0.1

Ten dokument porzadkuje foundation itemization pod polski serwer i neutralne
typy ekwipunku zamiast itemow "dla jednej klasy".

## Typy broni

- Miecz jednoreczny: balans, tempo, gra z tarcza lub off-handem.
- Miecz dwureczny: nacisk, stagger, cleave.
- Topor: physical pressure, bleed, lamanie pancerza.
- Mlot: guard break, stagger, anti-armor.
- Sztylet: crit, backstab, stealth, szybkie wejscie.
- Luk: dystans, projectile pressure, precyzja.
- Rozdzka: szybszy caster weapon, spell handling.
- Kostur: ciezsza bron magiczna, mana, skill pressure.
- Tarcza: block, guard, poise.
- Fokus / katalizator / relikt / tom: off-hand albo utility magiczne.

## Typy pancerza

- Ciezki: armor, block, guard, slabsza mobilnosc.
- Lekki: ruch, unik, stealth, slabsza ochrona.
- Szaty: mana, skill handling, magic scaling, slaba ochrona fizyczna.
- Sredni: etap kolejny, kompromis miedzy obrona i mobilnoscia.

## Soft gate foundation

Foundation wdraza soft gate przez wymagania atrybutow MMOItems:

- `required-strength` dla ciezkiej broni i ciezkiego pancerza,
- `required-dexterity` dla lekkiej broni, luku i lekkiego pancerza,
- `required-intelligence` dla szat, rozdziezek, kosturow, tomow i fokusow.

To nie jest jeszcze finalny system kar klasowych. To jest pierwszy prawdziwy
krok do modelu:

- kazdy moze sprobowac roznego gearu,
- ale efektywnie nosi to, pod co inwestuje atrybuty,
- klasa preferuje konkretne typy przez swoj gameplay i rozklad atrybutow.

## Matryca foundation

- Wojownik: preferuje `strength`, bronie ciezsze, tarcze, ciezki pancerz.
- Lotrzyk: preferuje `dexterity`, sztylety, lekki pancerz, utility off-hand.
- Lowca: preferuje `dexterity`, luki, lekki pancerz, dodatki dystansowe.
- Mag: preferuje `intelligence`, rozdzki, kostury, fokusy, szaty.
- Akolita: preferuje `intelligence` w foundation; hybrydy ciezsze beda etapem
  kolejnych iteracji albo osobnych baz itemow.

## Co jeszcze brakuje

- realnego `medium armor` foundation,
- osobnych baz `mlot`, `kusza`, `relikt faith`, `tom utility`,
- prawdziwych kar klasowych za niepasujacy typ gearu,
- template baz itemow,
- puli prefiksow i sufiksow per typ,
- tabel rarity i source/drop matrix.
