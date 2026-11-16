# Arena przeciwników (Akt 1, Gaj) — metoda i wyniki

Wspólna z poligonem klas (sesja przebudowy klas stawia to samo na klonie z pełnym stosem i mierzy klasy).

## Pliki
- `arena.py [wklej X Y Z]` — arena 29×29: niski mur, 4 filary-osłony, podwyższenie z rampą. Zapis
  `world/generated/przeciwnicy/structures/arena_v1.nbt` (place template). W kuźni stoi w świecie `world`, środek (300,-60,300).
- `przeciwnicy.py` — paczka MythicMobs → `/opt/docker/mc/forge/levels/przeciwnicy/Przeciwnicy_Gaj{,_skills}.yml`
  (moby `GAJ_WILK`, `GAJ_KORZENIAK`, `GAJ_LUCZNIK`; wygląd zastępczy vanilli).
- `arena_bot.js` — pomiar: `MOB=... TRYB=nieuwazny|uwazny POWT=n` (node:20, sieć forge_default, `/g` = forge/gracz,
  `/h` = ten katalog z rcon.js). Wynik JSON: czas walki, utracone HP, zgony.

## Zasada strojenia
Każdy atak ma **zapowiedź** (unikalny dźwięk + cząsteczki) i **kontrę**. Bot *nieuważny* (stoi i bije) ma wyraźnie
cierpieć, bot *uważny* (reaguje na dźwięk zapowiedzi) ma wygrać prawie bez straty. Walka ze zwykłym mobem 8–15 s.
Lekcje z pomiarów:
1. Zapowiedź musi **zamykać cel** na początku (skok/strzał w miejsce sprzed zapowiedzi) — atak śledzący gracza
   (v1) sprawiał, że unik nic nie dawał.
2. Umiejętność stawiana „pod graczem” nie testuje walki wręcz — gracz i tak się rusza. Mob wręcz potrzebuje ataku
   karzącego STANIE PRZY NIM (Uścisk Korzeniaka wokół siebie).
3. Zasięg ataku wokół moba ≥ 3,5 bl.: gracz w walce wręcz stoi 3–3,5 bl. (przy r=3 uścisk nie trafiał).

## Sygnały zapowiedzi (bot uważny)
| Atak | Dźwięk | Kontra | Okno odsłony |
|---|---|---|---|
| Wilk: Skok (0,9 s) | entity.wolf.growl p0.5 | krok w bok | 1,2 s oszołomienia |
| Korzeniak: Uścisk (1 s) | block.hanging_roots.break | odskok ≥3,5 bl. | 1,5 s zakorzenienia |
| Korzeniak: Wyrwa (1,2 s, tylko >5 bl.) | block.rooted_dirt.break w miejscu gracza | wyjść z kręgu | — |
| Łucznik: Strzał (0,8 s) | item.crossbow.loading_start | unik w bok | — (odskok przy <5 bl.) |

## Wyniki (bot z kamiennym mieczem, bez klas; utracone HP z 20)
| Wersja | Wilk nieuważny / uważny | Korzeniak n / u | Łucznik n / u |
|---|---|---|---|
| v1 (ataki śledzą) | 2,5–8,5 / 5–19,5 | 0–12 / 0–12 | 2,5–10 / 5–10 |
| v2 (zamknięty cel) | 0–7 / 0–5 | 0–12 / 0–3 | 0–3 / 0 |
| v3 (stawka ↑) | 9–18 / 0–10 | 0–10 / 0 | 0–8 / 0–3 |
| v3b (Uścisk r=3,5) | — | **20–23, 2/4 zgony / 0** | — |
| v4 (Łucznik: 2. strzał w bieżącą pozycję) | — | — | 6–14 / 0–12 (bot po uniku staje - do oceny z żywym graczem) |
| poligon klas v1 (16 klas poz. 1, sesja klas) | OK: 20–60% HP, uważny wyraźnie mniej | remis 45 s u nie-wojowników (za dużo HP, okno za krótkie) | zabija wszystkich poza wojownikiem, też uważnego |
| v5 (pod ninję/akolitę poz. 1) | bez zmian | HP 40, Uścisk co 6 s, odsłona 3 s | naciąg 1,1 s, strzał 5, co 3,5 s, dobicie 3 w 50% |
| poligon v2 (poz. 5, umiejętności) na v5 | w celu | ninja 43 s (za małe obrażenia klasy, nie mob) | akolita OK; ninja 22–33 s, 2 zgony |
| v6 | bez zmian | bez zmian | HP 26, odskok co 6 s + potknięcie 1 s |
| poligon v2b (Łucznik v6, ninja/akolita) | — | — | 32/32, 0 zgonów, 9–13 s ✓; ale nieuważny = uważny (16% vs 16%) ✗ |
| v7 | bez zmian | bez zmian | strzał 7, dobicie zawsze za 4 (kara za stanie w linii) |
| poligon v2c (Łucznik v7) | — | — | ODWROTNIE: uważny traci więcej (ninja 49% vs 38%, 2 zgony) — dobicie celowało w pozycję po uniku ✗ |
| v8 | bez zmian | bez zmian | dobicie w ten sam zamknięty punkt co strzał (lekcja 1) |
| poligon v2d (Łucznik v8) | — | — | **CEL ✓** 32/32, 0 zgonów; uważny 8–15 s, śr. 2% HP; nieuważny 6–11% (3–5×) |

**Stan 30.09: Gaj v8 gotowy (Wilk, Korzeniak, Łucznik).** Łucznik solo ma niską stawkę (ginie w 5–9 s) — groźny ma być
przez POZYCJĘ w lochu (tył grupy, podwyższenie za osłoną), nie przez liczby. Lekcja 4: drugi atak po zapowiedzi też w zamknięty punkt.

Do zrobienia: Łucznik (salwa 3 strzał — dziś bieg na niego przypadkiem omija strzały), wilk krążący zamiast biec prosto,
Pniaczek (rój), Korowiec (garda z przodu → obejście), Purchawka; potem strojenie liczb pod prawdziwe klasy.

## Uwaga do liczb (30.09, sesja klas)
Obrona w MythicLib jest dziś płaska (`#damage# - #defense#`), a klasy obronne mają ~26 obrony. Przy tej formule przeciwnicy Gaju
nie zadaliby im nic. Otwarta zmiana: `#damage# * 50 / (50 + #defense#)` (malejące zwroty). Obrażenia strojone tu (bez klas,
bez zbroi) są liczbami SUROWYMI; ostatecznie dostroić po decyzji o formule i pomiarze klas na poligonie.

## Loch 001 remake — przeciwnicy L1R_* (16.11)
Karta: `docs/lochy/akt1-v2/10-loch-001-kwietna-polana.md` (§0 protokół pąków R1, §5 bestiariusz, §13).
- `przeciwnicy_l001.py` → `kuznia/levels/przeciwnicy/Przeciwnicy_L001{,_skills}.yml` (13 mobów L1R_*; Wilk/Korzeniak/Łucznik
  to kopie Gaju v8 + pąk przy śmierci, więc na klon wgraj też `Przeciwnicy_Gaj_skills.yml`).
- `arena_l001.js` (bot BASTION poz. 2, MIECZ_KAMIENNY, klon bez kreatora, difficulty easy, obrona MythicLib `#damage# * 50 / (50 + #defense#)` jak world1):
  `run.sh <klon> arena_l001.js pak` (8 sprawdzeń R1), `… garda` (Korowiec), `… walka <MOB> nieuwazny|uwazny [POWT] [debug]`.

Pułapki znalezione na klonie (MM 5.11.2, Paper 1.21.11, 16.11):
- `Baby: true` na HUSK nie działa (`IsBaby 0b`); `data merge entity <caster.uuid> {IsBaby:1b}` przy `~onSpawn` działa. Na POLAR_BEAR: `Age: -1` + `AgeLock`.
- Nośnik pąka NIE może być ARMOR_STAND (odporny na `damage … generic`, cios gracza nie wołał `~onDeath`). Jest: mały niewidzialny ZOMBIE NoAI, HP 1, kwiat na głowie, tag `l1r_pak`/`l1r_pak_duzy`.
- Opóźniony łańcuch (`delay`) biegnie dalej po śmierci rzucającego — `/kill` pąka i tak dawał Pniaczka. Bezpiecznik: `caster.dojrzewa=0` w `~onDeath` + warunek przy summon.
- `modifydamage{a=-0.8;action=COMPOUND_MULTIPLIER}` dawało UJEMNE obrażenia (leczyło). ×0,2 = `a=0.2;action=ADDITIVE_MULTIPLIER;sync=true`.
- Bot: wrogów rozpoznawaj po typie i `metadata[2]` (custom_name), bo `text_display` (paski MM) przechodził filtr i bot go bił; pąk rozpoznawaj po nazwie — wyposażenie przychodzi osobnym pakietem później.

Wyniki 16.11 (seria 2, 3 walki na wroga i styl, utracone HP, gracz ~28 HP; limit walki 75 s):

| Wróg | nieuważny: HP / czas / zgony | uważny: HP / czas / zgony | uwagi |
|---|---|---|---|
| Wilk | 9,0 / 75 s (limit) / 0 | 2,3 / 9 s / 0 | nieuważny nie depcze → pętla Pniaczek→pąk→Pniaczek |
| Korzeniak | 9,0 / 13 s / 0 | 0 / 13 s / 0 | |
| Łucznik | 2,7 / 75 s (limit) / 0 | 1,8 / 27 s / 0 | solo niska stawka (jak Gaj v8); groźny ma być pozycją |
| Siewca | 5,5 / 75 s (limit) / 0 | 0 / 9 s / 0 | pąk Siewcy = 3 Pniaczki |
| Korowiec | 22,2 / 17 s / 0 | 0 / 28 s / 0 | garda ×0,16 z przodu, odsłona po ciosie = pełne obrażenia |
| Sowiedź | 29,4 / 16 s / **3/3** | 0 / 22 s / 0 | nieuważny zawsze ginie |

Garda (`… garda`, 4 ciosy): przód 1,2 HP, tył 7,5, przód 1,2 s po zapowiedzi ciosu 6,0. Pąki (`… pak`) 8/8 PASS.
Koszt (klon, 12 pąków + 14 Pniaczków + 2 Kotwy + Sowiedź): MSPT śr. 0,3 → 0,6 ms, szczyt 10 ms przy masowym przywołaniu.
Nie sprawdzone: limit 12 pąków na instancję (kolejka), boss Strażnik Sadu, ataki w grupie, prawdziwy gracz (klient QA).

Decyzje właściciela 16.11 (po arenie): (1) pętla Pniaczek→pąk→Pniaczek ZOSTAJE (wariant A) — kara za niedeptanie;
pętla trwa bez końca, ale się nie mnoży (1 Pniaczek → 1 pąk → 1 Pniaczek; Siewca daje 3 tylko raz), górną granicę
encji ma dać limit 12 pąków na instancję (jeszcze niezrobiony);
(2) Sowiedź ZOSTAJE bez złagodzenia (łapa 6) — nieuważny ginie 3/3. Generator bez zmian (stan 0010a39).
