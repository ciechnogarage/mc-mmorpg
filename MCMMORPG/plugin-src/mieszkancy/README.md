# Mieszkancy — życie Stolicy (Paper 1.21.11)

Zastępuje 28 villagerów MythicMobs ze Stolicy v2 (NoAI, okno handlu villagera, lewitujące o blok) żywymi NPC
z rozmową i usługami oraz dodaje miastu ruch, światło i dźwięk. Świat: `stolica_odnowa`.

## Co robi
- **NPC (`npc.yml`, 26)** — `Mannequin` (skórka z `CharacterCreator/skins.yml`, strój, broń) + `Interaction` (PPM)
  + napis `TextDisplay` (imię, rola, „PPM — porozmawiaj”). Stopy dosuwane do podłoża (±3 bloki).
  Głowa śledzi gracza do ~8 m, ciało dogania dopiero powyżej 70°; w spoczynku patrzy na swój warsztat
  i co jakiś czas wykonuje czynność (machnięcie, dźwięk, cząsteczki). Przy podejściu rzuca zaczepkę w dymku.
- **Rozmowa** — okno Paper Dialog (`Okna.java`): kwestia + opcje. Opcja może wykonać komendę konsoli
  (`%gracz%`), odpowiedzieć tekstem albo poprowadzić do innego NPC. Kliknięcia przez
  `customClick(Key, BinaryTagHolder)` (nie callback — gubi kliknięcia, `mc-gui-pack-dialogi`).
- **Usługi tylko u NPC** (decyzja właściciela 16.11: „tak tylko u NPC”): naprawa, rozbiórka, aukcje, sakwa,
  modlitwa. Menu Q tylko wskazuje drogę: `[console] mieszkancy prowadz <id> %player_name%` → ślad cząsteczek
  i pasek akcji „→ Imię · Rola · N m” do NPC (poza Stolicą: tekst z `gdzie`).
- **Życie miasta (`config.yml`)** — `napisy` (TextDisplay zamiast tabliczek; `usun-tabliczki` zdejmuje stare),
  `efekty` (cząsteczki/dźwięki, osobno dzień/noc, m.in. dzwon co 2 min słyszalny w całym mieście),
  `towary` (ItemDisplay na ladach), `zwierzeta`, `przechodnie` (chodzą po trasach, mówią kwestie).
- **Sprzątanie** — przy wczytaniu chunka usuwa stare villagery NoAI (`usun-stare-villagery`) i własne resztki
  (klucz PDC). Wszystkie encje są nietrwałe (`setPersistent(false)`) i dostawiane co sekundę w wczytanych chunkach.

## Koszt
- Wszystko tyka tylko, gdy ktoś jest w promieniu `promien-aktywny` (120) od `srodek`; inaczej `aktywne=false`.
- `tick()` = NPC × gracze w świecie (porównanie odległości). Encje: 26×3 (NPC) + 8 przechodniów + 9 zwierząt + 18 napisów + 12 towarów.
- Pomiar 16.11 na klonie (`paper:mspt`, 1 gracz, 60 s): życie uśpione 1,0–1,2 ms, gracz w Stolicy 1,8–2,5 ms
  (różnica zawiera też tykanie samych chunków miasta). Tłumu (20/100 graczy) nie mierzono.

## Komendy (`mieszkancy.admin`, OP/konsola)
| Komenda | Co |
|---|---|
| `/mieszkancy stan` | liczniki, ile NPC stoi, ilu przechodniów idzie, `aktywne` |
| `/mieszkancy przeladuj` | wczytuje `config.yml` i `npc.yml`, stawia wszystko od nowa |
| `/mieszkancy lista` | id NPC |
| `/mieszkancy prowadz <id> <gracz>` | ślad do NPC (dla menu Q) |
| `/mieszkancy rozmowa <id> <gracz>` | otwiera rozmowę z NPC |

## Pliki i pułapki
- `npc.yml` — opis pól w nagłówku pliku. `config.yml` — komentarze przy sekcjach.
- Nieznany przedmiot / dźwięk / cząsteczka / skórka = WARN w logu przy wczytaniu (nie cisza).
- Wymaga, by spawnery MythicMobs `stolica2_*` były zdjęte (inaczej villagery wracają i są usuwane w kółko).
- Stragany rynku w `tools/stolica_v2/build.py` stały o blok za wysoko (podłoga y100); lada przeniesiona na y101,
  towary na y102.x. Światła nocne (`minecraft:light`) — `night_lights()` w `build.py`.

## Build
`./build.sh` (gradle w dockerze) → `build/libs/mieszkancy-<wersja>.jar`.
