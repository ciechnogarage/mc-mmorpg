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
  to kopie Gaju v8 + pąk przy śmierci, więc na klon wgraj też `Przeciwnicy_Gaj_skills.yml`) **oraz datapack**
  `kuznia/levels/przeciwnicy/datapack/l1r/` → `<dane serwera>/world/datapacks/l1r` + `minecraft:reload` albo restart
  (kolejka pąków i IsBaby; bez niego pąki nie mają limitu, a Pniaczki nie są małe).
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
Nie sprawdzone: boss Strażnik Sadu, ataki w grupie, prawdziwy gracz (klient QA).

Decyzje właściciela 16.11 (po arenie): (1) pętla Pniaczek→pąk→Pniaczek ZOSTAJE (wariant A) — kara za niedeptanie;
pętla trwa bez końca, ale się nie mnoży (1 Pniaczek → 1 pąk → 1 Pniaczek; Siewca daje 3 tylko raz), górną granicę
encji daje limit 12 pąków na instancję (niżej);
(2) Sowiedź ZOSTAJE bez złagodzenia (łapa 6) — nieuważny ginie 3/3. Generator bez zmian (stan 0010a39).

### Limit 12 pąków na instancję (karta §13 w.1) — 16.11, klon l001-kolejka
Jak (od 16.11 w datapacku `l1r`, generowanym przez `przeciwnicy_l001.py`, słownik `DATAPACK`): każdy pąk przy `~onSpawn`
(`L1R_PAK_KOLEJKA`) dostaje tag `l1r_pq` i woła `execute as <uuid> at @s run function l1r:kolejka`: IsBaby, numer z rosnącego
licznika `l1r.nr` (scoreboard `l1r_pak`, posiadacz = UUID pąka), a gdy w promieniu 300 bl. (`distance=..300` = ten sam świat
i instancja) jest >12 aktywnych pąków z numerem, `l1r:najstarszy` daje najmniejszemu numerowi `l1r_usycha`; `~onTimer:5`
(`L1R_PAK_USYCHA`) zdejmuje go z kolejki, zeruje `dojrzewa` (nie da Pniaczka), szary dym 1 s, `remove`. Każde zniknięcie pąka
(zdeptany, dojrzał, usechł) woła `l1r:wypisz` (reset wyniku). Pniaczek: `l1r:maly` (IsBaby). Obiekt `l1r_pak` zakłada sam
`#minecraft:load` (`l1r:start`) przy starcie i `/reload` (klon 16.11: `scoreboard objectives list` = `[l1r_pak]`).
**Natywne `Options: Baby: true` i `Age: -1` nie działają ani na ZOMBIE, ani na HUSK** (klon 16.11: `IsBaby 0b` w 4/4), więc
`data merge` zostaje; to dziś największy pojedynczy koszt pąka (niżej).

**Dlaczego datapack, nie `command{}` z MM (pomiar 16.11, klon l001-tick, decyzja właściciela „zawsze tick i wydajność są
najważniejsze … można iść przez to co polecasz”):** komendy wewnątrz funkcji nie logują nic, jedno wywołanie z MM = 1 linia
`Running function`. Obciążenie: 60 serii × 14 pąków (840 pąków, każda seria przepełnia kolejkę o 2) pod `spark profiler
--interval 1`, warianty na przemian A/B/A/B, parser `tools/spark-report/hot.py`/`pod.py`:

| | komendy (160285a) | datapack |
|---|---|---|
| czas wątku głównego `CommandMechanic.castAtEntity` | 845 / 419 ms (≈0,5–1 ms na pąk) | 260 / 205 ms (≈0,25–0,3 ms na pąk) |
| `mspt` śr./max z 1 min (spoczynek 0,3) | 1,9/28 i 1,3/48 ms | 1,2/15 i 1,0/17 ms |
| max ticku z 10 s pod koniec | 18 i 13 ms | 7 i 6 ms |
| linie logu na 840 pąków (z 1 linią „Named entity … died” na zabity pąk) | ≈6860 | ≈2555 |

W wariancie z komendami ~połowa kosztu to samo logowanie (`CommandSourceStack.sendSuccess` → `sendSystemMessage` →
`PaperAdventure.asAdventure`: 189–311 ms); w datapacku zostaje głównie `data merge` IsBaby (`DataCommands.mergeData`
103–120 ms = zapis i odczyt całego NBT moba). Linie na pąk: komendy 5 (+ przy przepełnieniu 1 na każdy aktywny pąk),
datapack 2, bez przepełnieniowych. Linia „Named entity … died” to vanilla dla każdego nazwanego moba (nie kolejka).
Przeliczenie: bieg ~40–60 pąków (szacunek z karty, nie pomiar) = ~12–18 ms czasu wątku na cały bieg; AoE zabijające
6 wrogów w jednym ticku = ~1,5–2 ms w tym ticku (komendy: 3–6 ms).

Wynik `run.sh <klon> arena_l001.js kolejka` (wersja z komendami 16.11 7/7 ×3, klon l001-kolejka; datapack 16.11 klony l001-log/l001-tick:
**10/11 PASS**, `pak` 8/8 ×6, `walka` PNIACZEK/SIEWCA/KORZENIAK uważny 2/2):
- 14 pąków naraz → 12 aktywnych, usychają dwa najstarsze;
- po zdeptaniu jest miejsce (nowy nie wypycha nikogo), następny wypycha najstarszego;
- usychające nie dojrzewają (12 Pniaczków z 14 pąków);
- dwie „instancje” 400 bl. od siebie liczą się osobno (10+10, potem A 12 / B 11);
- scoreboard nie puchnie (po teście tylko `l1r.nr/ile/min`).

Test pąków `… pak` po zmianie: 8/8 (`kotwa-budzi` raz 7/8 — 2 Pniaczki po 10,8 s, test czasowy, nie kolejka; razem 16.11 13/14 biegów).
Pierwszy pomiar wersji z komendami (`mspt`, klon bez graczy poza botem; dokładniejszy spark wyżej): spoczynek 0,3 ms śr.; 6 serii po 14 pąków co 0,8 s (84 spawny w 5 s, ponad każdą
realną walkę) → 1,6 ms śr. w oknie 5 s, max tick 22 ms (max 22,8 ms był też w oknie 1 min przed testem — kosztu samego
spawnu MM i kolejki nie rozdzieliłem).

Pułapki (16.11):
- W YAML ` #` w zwykłym skalarze zaczyna komentarz: `c="...l1r_pak #nr..."` ucięło komendę (`command error` w logu) → nazwy `l1r.nr`.
- **Kilka pąków w jednym ticku** (14 naraz; w grze AoE zabija kilku wrogów naraz): tag dostawały wszystkie, numer jeszcze nie,
  więc pierwszy liczył 14 i uznawał siebie za najstarszego → usychało 13. Naprawa: liczyć i wybierać tylko `scores={l1r_pak=1..}`.
- Murek areny stoi na x≈314 — pąk przywołany w murze ginie od razu (fałszywy FAIL testu).
- **Spam konsoli z `command{}` MM**: każda komenda loguje wynik (`Added … l1r.nr`, `Set … Pąk`, `Test passed`, `Modified entity
  data`, przy przepełnieniu `Set l1r.min` na każdy aktywny pąk). Gamerule `send_command_feedback false` / `log_admin_commands
  false` nie usunęły linii (klon, przywrócone). Stąd datapack (wyżej); zero linii dałaby tylko mechanika Java.
- Bez obiektu `l1r_pak` (datapack nie wczytany): 3 linie `Unknown scoreboard objective` na pąk i brak limitu (13/14 aktywnych).
- **Otwarte: `usychajace-nie-dojrzewaja` 11 zamiast 12 Pniaczków** — 1/4 biegów z komendami i 1/11 z datapackiem (tylko z botem;
  bez bota ta sama sekwencja rcon 18/18). Rozliczenie kolejki w nieudanym biegu poprawne (16 wpisów, 4 wypisy, 10 pąków
  dojrzało), ale wyrosło 9 Pniaczków z 10 (9 wywołań `l1r:maly`): jeden `summon` w `L1R_PAK_DOJRZEWA` nie dał moba. Sonda `say`
  przed summonem: w 5 kolejnych biegach (5/5 PASS) wszystkie pąki miały `dojrzewa=1`, wpadki nie złapano. Przyczyny nie znam.
