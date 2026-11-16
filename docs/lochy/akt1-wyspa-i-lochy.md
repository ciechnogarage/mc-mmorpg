# Akt 1 — Wyspa i 10 lochów (v0.1, 2026-09-25)

Źródła: stare repo `~/Desktop/Przemek/MC/docs/` (`level-1-dungeon-island-*`, `dungeon-ladder-002-010-*`,
`mob-boss-encounter-001-*`, `loot-reward-table-001-*`, `dungeon-progression-difficulty-theme-ladder-*`),
research Wynncrafta (`docs/research/wynncraft/audyt/99-co-przeniesc-do-nas.md`, pozycje C1–C3),
paczka modeli `~/Desktop/Przemek/MC/modele/OUTPUT — kopia (2)/` (LittleRoom, 540 modeli, gotowe moby MM).
Ten dokument ulepsza stary plan i przenosi go na nasz stos. Szczegóły lochu: `loch-001-kwietna-polana.md`.

## 1. Zasada nadrzędna: dwie niezależne ścieżki

Akt 1 składa się z **jednej wyspy świata otwartego** i **10 lochów** (każdy to osobna instancja MythicDungeons,
wisząca wyspa w pustce). Obie ścieżki:

- **same wystarczają do przejścia aktu** — gracz, który nie lubi lochów, dojdzie do końca aktu wyspą, i odwrotnie;
- **nie blokują się nawzajem** — loch N otwiera poziom postaci, nie zadanie z wyspy; strefa wyspy nie wymaga lochu;
- **mają różny smak**, a klimatem i poziomem odpowiadają sobie parami (strefa wyspy N ↔ loch N);
- **razem dają więcej** — kto korzysta z obu, dostaje szerszy wachlarz: lepsze warianty receptur, sekrety, tytuły.

**Łup: ten sam, ale nie identyczny** (decyzja właściciela 2026-09-25). Obie ścieżki dają te same RODZAJE i TIERY
nagród — sprzęt, materiały profesji, żetony/postęp — o równej wartości. Różnią się konkretnymi przedmiotami i klimatem:
miecz z lochu i miecz z wyspy są tego samego tieru i siły, ale to inne przedmioty z innym wyglądem i afiksami.
Żadna ścieżka nie ma „lepszego” łupu.

| | Wyspa (świat otwarty, osobny serwer) | Lochy (instancje na world1) |
|---|---|---|
| Rytm | eksploracja, swobodne tempo, wracanie | zwarty przebieg 10–20 min, powtarzalny |
| Walka | bestie świata, elity, obozy, jaskinie, wydarzenia na zegarze | zaprojektowane starcia uczące mechaniki, boss z fazami |
| Sprzęt | z elit, obozów, skrzyń jaskiń, zadań — rodzina „wyspiarska” | z mobów, skrzyni końcowej, bossa — rodzina „lochowa” |
| Materiały | te same tiery; więcej zbieractwa (węzły) | te same tiery; więcej łupu z potworów |
| Postęp | żetony zadań / reputacja strefy | żetony lochu z zabezpieczeniem przed pechem |
| Tylko tu | Chowańce endemiczne, transport, sekrety świata | trudności, bossowie z fazami |
| Tryb | solo i spontaniczne grupy | solo lub drużyna, trudności Łatwy / Normalny (później Trudny / Bardzo Trudny) |
| Wspólne | doświadczenie MMOCore i poziom postaci, złoto, doświadczenie profesji | to samo |

Powiązania **opcjonalne** (bonus, nigdy warunek):

- **Receptury mieszane** — przedmiot z samego lochu lub samej wyspy jest pełnoprawny; wariant z materiałem z obu ścieżek
  ma lepszy roll albo dodatkowy efekt użytkowy (nie siłę PvP).
- **Klucze sekretów** — na wyspie można znaleźć klucz do ukrytej komnaty w lochu; w lochu — mapę skarbu na wyspę.
- **Tytuły / osiągnięcia** — „Znawca Gaju” za pełną parę 001 (strefa + loch), „Pogromca Aktu 1” za wszystkie.
- **Chowańce** — gatunki endemiczne żyją na wyspie; Pieczęć Lochowa (projekt Chowańców) wypada tylko z lochów.

## 2. Pary strefa ↔ loch i dobór modeli

Kolejność i klimaty lochów — ze starego `dungeon-ladder-002-010`. Kolumny z modelami to **kandydaci** z paczki LittleRoom,
dobrani po nazwie i konfiguracji MM; każdy zatwierdzamy dopiero po obejrzeniu w kliencie QA.

| # | Loch | Strefa wyspy (para) | Lekcja lochu | Modele — loch | Modele — wyspa |
|---|---|---|---|---|---|
| 001 | Kwietna Polana | Kwietne Zbocza (strefa startowa) | podstawy: unik, blok, parowanie, przerwanie, osłona | direwolf, parry_knight, boreal_druid, lr_owlbear, dreadroot, creepy | goose, giant_snail, chameleon, tiki_totem (wydarzenie) |
| 002 | Bagna Zgniłych Korzeni | Mokradła | statusy, spowolnienia, zasięg widzenia | bufo_bufo (ostrzeżenia językiem), dreadroot_mudmass, zahar (trucizna), voras (pająk, kokony) | giant_snail, merloc, land_squid |
| 003 | Zatopione Ruiny Archiwum | Wybrzeże i wraki | przerywanie, osłony | the_archivist + candlewarrior (boss), medusa_2, merloc, sea_mine | kraken (wydarzenie morskie), shipwreck, giant_turtle, raft |
| 004 | Kryształowe Jaskinie | Góry i jaskinie | pociski, pozycjonowanie | golem_king + construct, talus_knight, ancient_golem, gravelcreep | amethyst golem, kobold (obozy) |
| 005 | Opuszczona Kopalnia Nadzorcy | Wyrobiska | pułapki, patrole | eldric (kowadło, złom — boss), cog_sentinel, junkbot, kobold_assassin_2 (pułapki) | robo_lumberjack, mimic_* |
| 006 | Złamana Forteca | Ruiny strażnic | tarcze, parowanie, tempo | kriger, oog, ork_nil, wolfebersahd, lr_catapult, cannon | cavalier_fox, knight_fox (NPC/obozy) |
| 007 | Mroźna Przełęcz | Śnieżne szczyty | zagrożenia terenu | lr_yeti, yukio (lodowy smok), wendigo, snowcreep | boreal_druid (oryginalny, lód), ice_dragon (wydarzenie), mammoth |
| 008 | Popielne Sanktuarium | Wulkaniczne zbocze | okna ataku, burst | ignevar (meteory), magma_wyrm, hellhound, lavacreep, firebat | phoenix (wydarzenie), giant_lava_snail, cerberus |
| 009 | Pustkowia Kości | Pustkowia | przywołania, klątwy | lr_anubis + mummy + obelisk, desertmaw, ethereal_reaver | direwolf_desert, zombie, undeadcrew |
| 010 | Brama Pierwszego Aktu | Serce wyspy | test całego aktu | hollow_keeper (boss), gargoyle, xarnoth, astral_archon, magnus | lr_gryffin (transport/wydarzenie) |

Rekwizyty wspólne: `bonfire`/`bonfire_lit` (obóz, punkt kontrolny), `loot_chest`/`lootchest_glow` (nagroda),
`hidden_treasure` (sekrety), `lr_trainingdummy_2` (trening), `dungeonkey` (klucz sekretu), `magic_lantern`,
`eye_puzzle` (zagadki), `lr_fence`/`lr_lamppost` (obozy), `zipline`, `player_glider` (ruch po wyspie).

**Nie używamy** (cudza marka, mem albo współczesność — gryzie się z fantasy i grozi problemem prawnym):
pomni, skibiditoilet, luffy, steamboat_willy, imposter_*, gman, headcrab, poring/poporing/angeling/deviling (Ragnarok),
chocobo (Final Fantasy), mudkipaxolotl (Pokémon), wither_storm (Story Mode), pc/arcade, gokart, bumper_car, tank, nuke,
santa, easter, balloons, bbq, lawnmower, sofa i meble współczesne.

## 3. Jak przenosimy modele LittleRoom

- Stos zgodny: ModelEngine 4.0.9, MythicMobs 5.11.2; blueprinty w formatach BlockBench 3.6–5.0.
- **Bierzemy geometrię, animacje i dźwięki; konfigurację MM piszemy od nowa.** Oryginały mają angielskie nazwy, własne
  paski bossa, HP 100–500 bez związku z naszym balansem i `PreventOtherDrops` bez łupu. Nasze moby dziedziczą po nich
  tylko modele/stany animacji; statystyki, skille walki, łup i nazwy są nasze.
- Tekstury przemalowujemy, gdy gryzą się z kartą stylu (`docs/styl/karta-stylu-grafik.md`) — np. boreal_druid z lodu
  na liście i korę dla Lochu 001. Decyzja zawsze po zrzucie w kliencie QA obok naszych modeli (Chowańce).
- Dźwięki LittleRoom (`littleroom_*:` w resource packu) trafiają do packa razem z modelem.
- Wdrażamy **tylko modele z aktualnie budowanego lochu**, nie całą paczkę (rozmiar packa, czas ładowania klienta).
- Licencja: potwierdzona przez właściciela (2026-09-25).

## 4. Poziomy i odblokowanie (propozycja do zatwierdzenia)

Klasa wybierana od razu przy tworzeniu postaci (decyzja właściciela). Podklasa — prawdopodobnie na poziomie 10,
do ustalenia. Lochy Aktu 1 nie mogą wymagać konkretnej klasy.

| Para | Poziom zalecany | Odblokowanie lochu |
|---|---|---|
| 001 | 1–5 | od razu |
| 002–003 | 4–9 | poziom postaci |
| 004–006 | 8–14 | poziom postaci |
| 007–009 | 13–19 | poziom postaci |
| 010 | 18–20 | poziom postaci (bez wymogu ukończenia 001–009) |

Strefy wyspy mają te same przedziały, ale bez twardych blokad — tylko coraz groźniejsze bestie.

## 5. Kolejność pracy

Zasada właściciela: **jeden loch naraz, dopieszczony do końca**. Najpierw Loch 001, potem strefa Kwietne Zbocza
(wymaga projektu całej wyspy), potem 002. Nie klonujemy szablonu na kolejne lochy, zanim 001 nie przejdzie testów gracza.

## 6. Wyspa jako osobny serwer

Decyzja właściciela: wyspa Aktu 1 to **nowa wyspa na nowym serwerze** (backend za proxy), żeby odciążyć world1,
ale bez zabicia maszyny. Stan hosta 2026-09-25: 4 rdzenie CPU, 32 GB RAM (19 GB wolne), dysk 42 GB wolne;
world1/2/3 mają limit 3,3 GB każdy, lobby 1,8 GB, kuźnia 5 GB. Wąskim gardłem jest CPU (4 rdzenie na 5 serwerów).

Warunki, zanim serwer wyspy powstanie:
- **Budżet:** nowy serwer nie może dokładać obciążenia bez zwolnienia innego. Propozycja: przejmuje miejsce world2 lub world3
  (dziś poza zakresem, ~1,7–1,9 GB i ~3% CPU w spoczynku) albo kuźnia chodzi tylko na czas pieczenia.
- **Świat pieczony offline:** teren generowany Irisem w kuźni i wgrywany jako gotowe regiony, pregen i twarda granica świata —
  zero generacji chunków przy graczach; Iris w runtime tylko jeśli pomiar spark to udźwignie.
- **Wspólny postęp:** poziom, klasa, profesje, ekwipunek i profile muszą być te same na world1 (lochy) i na wyspie.
  Dziś MMOCore trzyma dane lokalnie (`mysql.enabled: false`) — potrzebna wspólna baza MySQL/MariaDB dla MMOCore,
  MMOItems/MMOInventory i MMOProfiles oraz synchronizacja ekwipunku przy przejściu przez proxy. To osobny projekt
  i warunek konieczny równoległego postępu.
- **Pomiar przed i po:** spark na world1 i nowym serwerze pod obciążeniem botami.

## 7. Otwarte pytania

1. ~~Wyspa~~ → nowa wyspa, nowy serwer (§6). Otwarte: który slot zwalniamy (world2 / world3)?
2. Zakres poziomów aktu (propozycja 1–20). ~~Klasa~~ → od razu; podklasa ~10, do ustalenia.
3. ~~Licencja LittleRoom~~ → potwierdzona przez właściciela.
