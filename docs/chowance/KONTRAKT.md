# KONTRAKT Chowańców — v0 (2026-09-22 21:00, Agent A)

Właściciel: Agent A (SILNIK). Agent B zgłasza potrzeby w PROBLEMY.md (prefiks `KONTRAKT:`).
Każda zmiana podbija wersję i trafia do sekcji „Historia” na końcu. Zmiany łamiące format są
oznaczane **[ŁAMIE]**; A utrzymuje wtedy przez co najmniej jedną wersję zgodność wstecz
(loader czyta stary klucz i ostrzega w logu).

Plugin: **Chowance** (Paper 1.21.11), katalog danych `plugins/Chowance/`.
Źródła: `/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/chowance/` (obok character-creator).

---------------------------------------------------------------------------------------------
## 1. Pliki i właściciele

| Plik w `plugins/Chowance/`   | Właściciel | Co zawiera |
|------------------------------|-----------|------------|
| `config.yml`                 | A | parametry silnika: wzór łapania, krzywe exp, limity, ranking, turnieje, profesja |
| `typy.yml`                   | A | 10 typów + tabela skuteczności (mnożniki) |
| `charaktery.yml`             | A | charaktery (natury) +/− |
| `gatunki/*.yml`              | **B** | gatunki (sekcja 2), dowolnie wiele plików i gatunków w pliku |
| `ruchy/*.yml`                | **B** | ruchy (sekcja 3) |
| `cechy/*.yml`                | **B** | cechy pasywne (sekcja 4) — `rodzaj` tylko z listy A |
| `przedmioty.yml`             | **B** | role przedmiotów MMOItems w silniku (sekcja 5) |
| `startery.yml`               | **B** | oferta starterów (sekcja 6) |
| `trenerzy/*.yml`             | **B** | Mistrzowie Więzi, trenerzy NPC (sekcja 7) |
| `wieza.yml`                  | **B** | Wieża Chowańców — fale (sekcja 7) |
| `bestiariusz-nagrody.yml`    | **B** | progi i nagrody Bestiariusza (sekcja 8) |
| `zdarzenia.yml`              | **B** | komendy konsoli odpalane przy zdarzeniach (sekcja 11) |
| `gatunki/zz_silnik_test.yml`, `ruchy/zz_silnik_test.yml`, `cechy/zz_silnik_test.yml` | A | gatunki/ruchy TESTOWE silnika (`testowy: true`) do evali — B ich nie rusza |
| `dane/`                      | A (runtime) | zapis graczy — **nie edytować ręcznie** (onDisable nadpisze) |

A dostarcza przy pierwszym starcie pliki startowe dla plików B (puste lub z przykładem w komentarzu).
Od chwili, gdy B je zmieni, należą do B — A ich nie dotyka.

Przeładowanie treści bez restartu: **`/chw admin przeladuj`** (walidacja z dokładnym
`plik: klucz: błąd` w logu i w odpowiedzi komendy; błędny gatunek/ruch jest pomijany,
reszta wchodzi). W odróżnieniu od MythicMobs nieznany klucz JEST zgłaszany jako WARN.

Identyfikatory (gatunki, ruchy, cechy, trenerzy, odznaki): `[A-Z0-9_]+`, bez polskich znaków.
Teksty wyświetlane (`nazwa`, `opis`): po polsku, z polskimi znakami, kolory MiniMessage
(`<gold>`, `<#ff8800>`) — **bez** `<gradient>` w nazwach stworów (bot/skrypty nie znajdą
nazwy, zob. memory mythicmobs-cicha-walidacja).

---------------------------------------------------------------------------------------------
## 2. Gatunek (`gatunki/*.yml`)

```yaml
ZARZEWIEC:                      # ID gatunku
  nazwa: "Zarzewiec"
  opis:                         # Bestiariusz, 1–6 linii
    - "Mały lis z żarem w ogonie."
  typy: [OGIEN]                 # 1–2 z listy typów (sekcja 9)
  rzadkosc: POSPOLITY           # POSPOLITY|NIEPOSPOLITY|RZADKI|EPICKI|LEGENDARNY
  wspolczynnik_zlapania: 190    # opcjonalnie 1–255; domyślnie z rzadkości (config.yml)
  stadium: 1                    # 1..3
  linia: ZARZEWIEC              # ID gatunku bazowego linii (hodowla daje zawsze bazowy)
  staty_bazowe:                 # 1–255 każda
    zdrowie: 45
    atak: 60
    obrona: 40
    atak_spec: 70
    obrona_spec: 50
    szybkosc: 65
  krzywa_exp: SREDNIA           # SZYBKA|SREDNIA|WOLNA
  exp_bazowy: 62                # exp dla zwycięzcy za pokonanie tego gatunku
  wiez_startowa: 70             # 0–255
  cechy: [ZAR]                  # 1–3 ID cech; osobnik losuje jedną
  ruchy:                        # pula: poziom -> ruchy; osobnik dostaje 4 najnowsze dostępne
    1: [DRAPNIECIE, ZARZENIE]
    7: [ISKRA]
    14: [PLOMIENNY_KIEL]
  ewolucje:                     # 0..n gałęzi; pierwsza spełniona wygrywa
    - do: ZARZEWIAK
      poziom: 16
      przedmiot: KAMIEN_PRZEMIANY_OGNIA   # opcjonalnie; ID z przedmioty.yml/kamienie
      wiez: 0                             # opcjonalnie, min. więź 0–255
  hodowla:
    grupy: [BESTIE]             # grupy jaj; NIEPLODNY = nie rozmnaża się
    dystans: 1500               # bloki marszu do wyklucia
  lapanie:
    pieczec: true               # łapanie osłabienie + Pieczęć
    oswajanie: false            # łapanie karmieniem
    przysmaki:                  # tylko gdy oswajanie: true; ID MMOItems -> punkty 1–100
      PROF_JAGODY_OGNISTE: 15
    wymagana_pieczec: ""        # np. PIECZEC_PRADAWNA_WIEZ (legendarne: tylko ta działa)
  legendarny: false             # true = łapalny tylko po pokonaniu, w oknie `legendarne.okno_s`
  wymienialny: true             # false = związany: nie do handlu (starter/legendarny zawsze związany)
  wierzchowiec: false           # true = po nałożeniu siodła można dosiąść
  mob:                          # MythicMobs ID ciał — patrz sekcja 10
    oswojony: CHW_ZARZEWIEC
    oswojony_lsniacy: CHW_ZARZEWIEC_L
    dziki: CHW_ZARZEWIEC_D
    dziki_lsniacy: CHW_ZARZEWIEC_DL
    zastepczy: FOX              # EntityType vanilla — TYLKO prototyp, gdy moba MM brak (WARN w logu)
  ikona: chw_zarzewiec          # ID przedmiotu Nexo (menu, Bestiariusz)
  ikona_lsniaca: chw_zarzewiec_l
  wystepowanie:                 # Bestiariusz „mapa występowania” — tekst do lore
    - "Iris: lasy liściaste, dzień"
  testowy: false                # true = poza Bestiariuszem, nagrodami, starterami
```

Walidacja (A): każdy ID z `ruchy`, `cechy`, `ewolucje.do`, `linia`, `przedmiot` musi istnieć;
mob z `mob.*` musi istnieć w MythicMobs (sprawdzane po jego starcie — brak = WARN i ciało zastępcze).

---------------------------------------------------------------------------------------------
## 3. Ruch (`ruchy/*.yml`)

```yaml
ISKRA:
  nazwa: "Iskra"
  opis: "Ciska rozżarzonym węgielkiem."
  typ: OGIEN
  kategoria: SPECJALNY          # FIZYCZNY|SPECJALNY|STATUS
  moc: 40                       # 0 dla STATUS
  celnosc: 100                  # 1–100; 0 = nie chybia
  priorytet: 0                  # -3..+3 (tura)
  odnowienie: 3.0               # s — walka real-time (pojedynek sterowany, towarzysz)
  kontakt: false                # dotyka celu (cechy typu KONTRA_STATUS)
  efekty:                       # 0..n
    - {rodzaj: STATUS, status: OPARZENIE, szansa: 10, cel: PRZECIWNIK, czas: 3}
    - {rodzaj: STAT, stat: ATAK, zmiana: 1, szansa: 100, cel: SIEBIE}
    - {rodzaj: LECZENIE, procent: 50, cel: SIEBIE}
    - {rodzaj: WYSYSANIE, procent: 50}   # % zadanych obrażeń wraca
    - {rodzaj: ODRZUT, procent: 25}      # % zadanych obrażeń w siebie
  skill: CHW_R_ISKRA            # MythicMobs skill — TYLKO wizualia; caster = ciało atakującego,
                                # target = ciało celu. Obrażenia liczy silnik, NIE skill.
```
Statusy: `SEN`, `SPETANIE`, `OSZOLOMIENIE` (liczą się do łapania), `OPARZENIE`, `ZATRUCIE`.
`czas` = tury (walka turowa) albo sekundy (real-time). Etapy statów: `ATAK, OBRONA, ATAK_SPEC,
OBRONA_SPEC, SZYBKOSC, CELNOSC, UNIK`, zmiana −6..+6.

---------------------------------------------------------------------------------------------
## 4. Cecha pasywna (`cechy/*.yml`)

```yaml
ZAR:
  nazwa: "Żar"
  opis: "Gdy zdrowie spada poniżej 1/3, ruchy ognia są silniejsze o 50%."
  rodzaj: WZMOCNIENIE_TYPU_PRZY_NISKIM_HP
  parametry: {typ: OGIEN, mnoznik: 1.5, prog_hp: 33}
```
Rodzaje (implementuje A; inne = błąd walidacji):
`WZMOCNIENIE_TYPU {typ, mnoznik}`, `WZMOCNIENIE_TYPU_PRZY_NISKIM_HP {typ, mnoznik, prog_hp}`,
`ODPORNOSC_NA_TYP {typ, mnoznik}`, `ODPORNOSC_NA_STATUS {status}`, `REGENERACJA {procent}`
(co turę / co 10 s real-time), `MNOZNIK_STATU {stat, mnoznik}`, `KONTRA_STATUS {status, szansa}`
(atakujący ruchem z `kontakt: true` dostaje status), `UNIK {procent}`, `PREMIA_WIEZI {mnoznik}`,
`PREMIA_EXP {mnoznik}`.

---------------------------------------------------------------------------------------------
## 5. Przedmioty (`przedmioty.yml`) — ID MMOItems

**Wszystkie przedmioty Chowańców poza karmą to MMOItems typu `MISCELLANEOUS`** (typ `CONSUMABLE`
MMOItems zużywa sam przy PPM — konflikt z silnikiem). Materiał bazowy **nie** może być rzucalny ani
używalny w vanilli (nie SNOWBALL/EGG/ENDER_PEARL/jedzenie/bloki/spawn eggi) — np. `PAPER` +
`item_model` Nexo. Silnik rozpoznaje przedmiot po NBT MMOItems (`MMOITEMS_ITEM_TYPE` + `MMOITEMS_ITEM_ID`).

```yaml
pieczecie:                      # PPM = rzut (silnik wystrzeliwuje pocisk i zużywa 1 szt.)
  PIECZEC_KAMIENNA:    {rodzaj: ZWYKLA,     mnoznik: 1.0, model_mob: ""}
  PIECZEC_KRYSZTALOWA: {rodzaj: ZWYKLA,     mnoznik: 1.5}
  PIECZEC_PRADAWNA:    {rodzaj: ZWYKLA,     mnoznik: 2.0}
  PIECZEC_NOCNA:       {rodzaj: NOCNA,      mnoznik: 1.0, mnoznik_warunku: 3.0}  # noc w świecie
  PIECZEC_WODNA:       {rodzaj: WODNA,      mnoznik: 1.0, mnoznik_warunku: 3.0}  # cel w wodzie/deszczu
  PIECZEC_KRADZIEZY:   {rodzaj: KRADZIEZY,  mnoznik: 1.0, mnoznik_warunku: 2.0}  # patrz 12.3
  PIECZEC_LOCHOWA:     {rodzaj: LOCHOWA,    mnoznik: 1.0, mnoznik_warunku: 3.5}  # tylko w instancji MythicDungeons
  PIECZEC_PRADAWNA_WIEZ: {rodzaj: LEGENDARNA, mnoznik: 1.0}                     # jedyna na legendarne
  # model_mob: opcjonalny MM mob animacji drżenia (sygnały DRZENIE/SUKCES/PORAZKA), inaczej ItemDisplay
kamienie: [KAMIEN_PRZEMIANY_OGNIA, KAMIEN_PRZEMIANY_WODY]   # lista do walidacji ewolucji
karma:                          # PPM na WŁASNEGO przywołanego stwora; typ MMOItems podać, gdy nie MATERIAL
  PROF_JAGODY_OGNISTE: {typ_mmoitems: MATERIAL, exp: 20, wiez: 3}
eliksiry:                       # PPM: leczy aktywnego (albo przycisk „Przedmiot” w walce turowej)
  ELIKSIR_MALY:  {leczenie_procent: 30}
  ELIKSIR_WSKRZESZENIA: {wskrzesza: true, leczenie_procent: 50}
  ELIKSIR_PRZEBUDZENIA: {leczy_status: true}
siodla:                         # PPM na własnego stwora z `wierzchowiec: true`
  SIODLO_PROSTE: {}
jajo_ikona: JAJO_CHOWANCA       # tylko ikona w menu — jajo żyje w danych profilu (DECYZJE)
```

---------------------------------------------------------------------------------------------
## 6. Startery (`startery.yml`)

```yaml
poziom: 5
domyslne: [ZARZEWIEC, KROPLIK, PEDRAK]        # dokładnie 3
rasy:                                          # opcjonalnie, klucz = rasa CharacterCreator
  elf: [ZARZEWIEC, KROPLIK, LISTEK]            # (sprawdzane isPermissionSet("race.<rasa>"))
klasy: {}                                      # opcjonalnie, klucz = klasa MMOCore (małe litery)
```
Rasa ma pierwszeństwo przed klasą. `/chw starter` (albo auto-okno po wyborze postaci,
`config.yml: starter.auto-okno`) otwiera dialog Papera z trzema kafelkami. Starter jest związany.

---------------------------------------------------------------------------------------------
## 7. Trenerzy, Mistrzowie Więzi, Wieża (`trenerzy/*.yml`, `wieza.yml`)

```yaml
MISTRZ_PLOMIENIA:
  nazwa: "Mistrzyni Iskra"
  rodzaj: MISTRZ                # MISTRZ|TRENER
  odznaka: PLOMIENIA            # MISTRZ: przyznawana za wygraną (podnosi limit poziomu, config.yml)
  wymagane_odznaki: 0           # min. liczba odznak do wyzwania
  druzyna:
    - {gatunek: ZARZEWIAK, poziom: 25, ruchy: [ISKRA, PLOMIENNY_KIEL], cecha: ZAR, geny: 20, charakter: SMIALY}
  nagroda_komendy: ["mi give MISCELLANEOUS PIECZEC_KRYSZTALOWA %gracz% 3"]
```
`wieza.yml`:
```yaml
WIEZA_P01:
  nazwa: "Wieża Chowańców — piętro 1"
  fale: [TRENER_A, TRENER_B, MISTRZ_PLOMIENIA]
  leczenie_miedzy_falami: 25    # % HP drużyny
  nagrody: {5: ["..."], 10: ["..."]}   # za osiągnięcie fali N (raz na profil)
```
Start walki z NPC (MythicMobs/MythicDungeons, konsola): `chw admin walka-npc <gracz> <TRENER>`
albo mechanika `chwWalkaTrener{t=MISTRZ_PLOMIENIA} @trigger`. Wieża: `chw admin wieza <gracz> <WIEZA>`.

---------------------------------------------------------------------------------------------
## 8. Bestiariusz — nagrody (`bestiariusz-nagrody.yml`)

```yaml
PROC_10:  {warunek: {rodzaj: PROCENT_ZLAPANYCH, wartosc: 10}, opis: "10% Bestiariusza", komendy: ["..."]}
PROC_25:  {warunek: {rodzaj: PROCENT_ZLAPANYCH, wartosc: 25}, komendy: []}
PROC_50:  {warunek: {rodzaj: PROCENT_ZLAPANYCH, wartosc: 50}, komendy: []}
TYP_OGIEN: {warunek: {rodzaj: KOMPLET_TYPU, typ: OGIEN}, komendy: []}
LSNIACE:  {warunek: {rodzaj: WSZYSTKIE_LSNIACE}, komendy: []}
```
Liczone po gatunkach bez `testowy: true`. Każda nagroda wypłacana raz na profil (zapis w danych).
W komendach: `%gracz%` (nick), `%profil%` (UUID profilu).

---------------------------------------------------------------------------------------------
## 9. Typy (A, `typy.yml`)
`OGIEN, WODA, ZIEMIA, POWIETRZE, NATURA, MROK, SWIATLO, METAL, BESTIA, NIEUMARLY`
Wyświetlane: Ogień, Woda, Ziemia, Powietrze, Natura, Mrok, Światło, Metal, Bestia, Nieumarły.
Mnożniki: 2.0 (skuteczny), 0.5 (słaby), 0.0 (bez efektu), domyślnie 1.0. Tabela w `typy.yml`.

---------------------------------------------------------------------------------------------
## 10. MythicMobs — ciała i to, co silnik rejestruje

### 10.1 Ciała (B)
- `mob.oswojony` / `oswojony_lsniacy`: ciało przywołanego stwora, AI peta (GoToOwner, OwnerTarget,
  OwnerAttacker). Silnik po spawnie: `setOwner(gracz)`, poziom MM = poziom stwora, nieprzetrwały
  (`persistent=false`), znacznik PDC `chowance:instancja`. Silnik **nadpisuje** max HP z własnych
  statystyk; atak skillami B **przez mechanikę `chwObrazenia`** (nie gołe `damage{}`), żeby liczyły
  się typy, staty i geny.
- `mob.dziki` / `dziki_lsniacy`: dzikie ciało; spawny robi B (MM RandomSpawns/spawnery). Silnik rozpoznaje
  gatunek po typie moba, poziom = poziom MM. Lśniący: przy spawnie dzikiego silnik losuje
  `config.yml: lsniace.szansa` (1/1000) i podmienia na `dziki_lsniacy` + ogłoszenie.

### 10.2 Mechaniki (A)
| Mechanika | Opis |
|---|---|
| `chwObrazenia{moc=40;typ=OGIEN;kategoria=FIZYCZNY}` | obrażenia wg silnika (staty castera-chowańca, typ, STAB, cechy). Caster nie-chowaniec: poziom MM, staty neutralne |
| `chwStatus{status=SEN;czas=5}` | nakłada status (s) na cel-chowańca |
| `chwLap{pieczec=PIECZEC_KAMIENNA}` | próba schwytania celu przez castera-gracza tą pieczęcią (bez zużycia przedmiotu) |
| `chwWalkaTrener{t=ID}` | cel-gracz zaczyna walkę turową z trenerem |
| `chwLeczDruzyne{procent=100}` | leczy drużynę celu-gracza (np. uzdrowiciel w mieście) |

### 10.3 Warunki (A)
`chwChowaniec` (cel jest chowańcem: dzikim lub oswojonym), `chwOswojony`, `chwDziki`, `chwLsniacy`,
`chwGatunek{g=ZARZEWIEC}`, `chwTyp{t=OGIEN}`, `chwMaSiodlo`, `chwWWalce` (w walce turowej/pojedynku —
AI ma stać), `chwStatus{s=SEN}`, `chwMaAktywnego` (cel-gracz ma przywołanego stwora).

### 10.4 Targetery (A)
`@ChwAktywny` (aktywny stwór castera-gracza), `@ChwWlasciciel` (właściciel castera-stwora; to samo
co `@Owner`), `@ChwPrzeciwnik` (ciało przeciwnika w bieżącej walce).

### 10.5 Sygnały wysyłane przez silnik (`~onSignal:<NAZWA>` w mobie B)
| Do | Sygnał | Kiedy | trigger |
|---|---|---|---|
| oswojony | `PRZYWOLANIE` | 1 tick po przywołaniu | właściciel |
| oswojony | `ODWOLANIE` | 10 ticków przed usunięciem | właściciel |
| oswojony | `EWOLUCJA` | po ewolucji (nowe ciało) | właściciel |
| oswojony | `DOSIADZ` | gracz dosiada (B: `mountmodel{...} @trigger`) | gracz |
| oswojony | `OKRZYK` | wygrana walka / awans poziomu | właściciel |
| dziki | `SZAL` | nieudane łapanie (silnik dodatkowo: Siła I + Szybkość II na 10 s) | gracz |
| dziki | `POKONANY` | legendarny pokonany, otwiera okno łapania | gracz |
| dziki | `LSNIACY` | po podmianie na lśniącego | — |
| dziki | `OSWOJONY` | oswajanie 100%, 10 ticków przed zamianą | gracz |
| pieczęć (`model_mob`) | `DRZENIE` ×1–3, `SUKCES`, `PORAZKA` | animacja łapania | gracz |

---------------------------------------------------------------------------------------------
## 11. Zdarzenia (`zdarzenia.yml`, B) — komendy konsoli

```yaml
ZLAPANIE: ["..."]        # %gracz% %gatunek% %nazwa% %poziom% %lsniacy%(tak/nie)
LSNIACY_POJAWIL_SIE: []  # %gatunek% %nazwa% %x% %y% %z% %swiat%
EWOLUCJA: []             # %gracz% %z% %do%
WYKLUCIE: []             # %gracz% %gatunek% %lsniacy%
STARTER: []              # %gracz% %gatunek%
ODZNAKA: []              # %gracz% %odznaka%
WYGRANA_POJEDYNKU: []    # %gracz% %przegrany% %elo%
WYGRANA_TURNIEJU: []     # %gracz% %turniej%
FALA_WIEZY: []           # %gracz% %wieza% %fala%
```
Dla kodu innych pluginów silnik publikuje zdarzenia Bukkit w `pl.mcmmorpg.chowance.api.event`:
`ChowaniecZlapanyEvent`, `ChowaniecEwolucjaEvent`, `ChowaniecPrzywolanyEvent`, `WalkaZakonczonaEvent`.

---------------------------------------------------------------------------------------------
## 12. Zachowania silnika, na których B może polegać

1. **Drużyna 6 + magazyn.** Na zewnątrz 1 aktywny. Limit drużyny i magazynu rośnie z poziomem
   profesji `poskramiacz` (progi w `config.yml`). Dane per profil MMOProfiles — nowa postać = zero.
2. **Despawn** aktywnego: wyjście, zmiana profilu, śmierć gracza, zmiana świata, restart/wyłączenie
   pluginu, HP stwora 0. Po teleporcie w tym samym świecie stwór jest przenoszony za graczem.
3. **Łapanie:** PPM pieczęcią = rzut. Trafienie dzikiego → wzór z `config.yml` → 1–3 drżenia →
   sukces/porażka. Po porażce stwór jest „zbiegły” od tego gracza na 60 s: pieczęcie innych graczy
   się odbijają — poza `KRADZIEZY`, która na zbiegłych ma `mnoznik_warunku`.
4. **Oswajanie:** PPM przysmakiem na dzikiego gatunku z `oswajanie: true` → pasek postępu na
   actionbarze → 100% = schwytany.
5. **Karmienie** własnego: PPM karmą → exp + więź. **Siodło:** PPM siodłem → `ma_siodlo`;
   potem PPM gołą ręką na własnego wierzchowca = sygnał `DOSIADZ`.
6. **Walka turowa** (dialog Papera): `/chw walka <gracz>` (PvP), `/chw walka` patrząc na dzikiego
   (PvE, z przyciskiem „Pieczęć”), trenerzy z sekcji 7. Ciała stoją naprzeciw siebie.
7. **Pojedynek areny sterowany:** `/chw pojedynek <gracz>`; w pojedynku klawisze hotbara 1–4 =
   ruchy 1–4 (silnik anuluje zmianę slotu, ekwipunek nietknięty). Rankingowy (ELO).
8. **Hodowla:** dwa stwory tej samej grupy jaj → po `config.yml: hodowla.czas_min` jajo w Wylęgarni
   profilu (max 3) → wykluwa się po przejściu `hodowla` dystansu gatunku.

---------------------------------------------------------------------------------------------
## 13. Komendy

Gracz (`chowance.gracz`, domyślnie wszyscy): `/chw`, aliasy `/chowance`, `/chowaniec`.
| Komenda | Działanie |
|---|---|
| `/chw` | pomoc |
| `/chw przywolaj <1-6>` / `/chw odwolaj` | przywołanie/odwołanie |
| `/chw druzyna` | tekstowy podgląd (menu robi B na placeholderach) |
| `/chw info <1-6>` | karta stwora (dialog) |
| `/chw zamien <a> <b>` | kolejność w drużynie |
| `/chw magazyn [strona <n>]` · `/chw magazyn wloz <slot>` · `/chw magazyn wyjmij <nr>` | magazyn (nr globalny od 1) |
| `/chw nazwij <slot> <imię>` | przydomek (max 16 zn.) |
| `/chw ewoluuj <slot>` | ewolucja (przedmiot w ręce, gdy wymagany) |
| `/chw naucz <slot> <RUCH> <1-4>` | wymiana ruchu na inny z puli dostępnej na poziomie |
| `/chw uwolnij <slot> potwierdz` | wypuszczenie |
| `/chw starter` | wybór startera (raz na profil) |
| `/chw walka [gracz]` · `/chw pojedynek <gracz>` · `/chw akceptuj` · `/chw odrzuc` · `/chw poddaj` | walki |
| `/chw handel <gracz>` | handel (dialog) |
| `/chw hodowla` · `/chw hodowla wloz <slot>` · `/chw hodowla wyjmij <1-2>` | hodowla |
| `/chw bestiariusz [strona <n>]` · `/chw bestiariusz gatunek <ID>` | Bestiariusz |
| `/chw ranking` · `/chw turniej zapisz` · `/chw turniej wypisz` | ranking/turniej |
| `/chw menu` | `dm open chowance_menazeria <gracz>` (menu B) |

Admin (`chowance.admin`): `/chw admin daj <gracz> <GATUNEK> [poziom] [lsniacy]`,
`przeladuj`, `zapisz`, `info <gracz>`, `lecz <gracz>`, `odznaka <gracz> <ID> [zabierz]`,
`walka-npc <gracz> <TRENER>`, `wieza <gracz> <WIEZA>`, `turniej start|stop|stan`,
`sezon nowy`, `exp <gracz> <slot> <ile>`, `jajo <gracz> <GATUNEK>`, `spawn <GATUNEK> [poziom] [lsniacy]`,
`diag` (stan pamięci/cache/liczniki).

---------------------------------------------------------------------------------------------
## 14. Placeholdery PAPI (`%chowance_<klucz>%`)

`<n>` = slot drużyny 1–6, `<k>` = pozycja na bieżącej stronie magazynu/Bestiariusza 1–28.
Pusty slot: tekstowe zwracają `""`, `_istnieje` zwraca `nie`.

Aktywny: `aktywny_istnieje`, `aktywny_nazwa`, `aktywny_gatunek`, `aktywny_poziom`, `aktywny_hp`,
`aktywny_hpmax`, `aktywny_hp_procent`, `aktywny_exp_procent`, `aktywny_typ`, `aktywny_wiez`.

Drużyna: `druzyna_liczba`, `druzyna_limit`, `druzyna_<n>_istnieje|nazwa|gatunek|gatunek_nazwa|poziom|
hp|hpmax|hp_procent|exp|exp_do|exp_procent|typ|cecha|charakter|wiez|lsniacy|ikona|material|
ruch_<1-4>|ruch_<1-4>_typ|aktywny|zemdlony|mozna_ewoluowac|staty` (`material` = `nexo-<ikona>` do
DeluxeMenus; `staty` = jedna linia „HP 45 · Atk 60 · …”).

Magazyn: `magazyn_liczba`, `magazyn_limit`, `magazyn_strona`, `magazyn_stron`,
`magazyn_<k>_istnieje|nazwa|poziom|ikona|material|nr` (nr globalny do `/chw magazyn wyjmij`).

Bestiariusz: `bestiariusz_widziane`, `bestiariusz_zlapane`, `bestiariusz_wszystkie`,
`bestiariusz_procent`, `bestiariusz_strona`, `bestiariusz_<k>_stan` (NIEZNANY|WIDZIANY|ZLAPANY),
`bestiariusz_<k>_nazwa|ikona|material|id`, `gatunek_<ID>_stan`.

Postęp: `odznaki_liczba`, `odznaka_<ID>` (tak/nie), `limit_poziomu`, `ranking_elo`,
`ranking_pozycja`, `ranking_top_<1-10>_nazwa|elo`, `wieza_<WIEZA>_rekord`, `oswajanie_postep`,
`hodowla_<1-2>_nazwa`, `hodowla_postep_procent`, `jajo_<1-3>_istnieje|postep_procent`,
`starter_wybrany`, `w_walce`, `turniej_stan`.

## 15. Uprawnienia
`chowance.gracz` (default true), `chowance.admin` (op). Warunki „ma/nie ma” silnik sprawdza
przez `isPermissionSet` (memory op-falszuje-haspermission).

## 16. Profesja MMOCore
ID `poskramiacz` (plik `MMOCore/professions/poskramiacz.yml` robi B, krzywa `.txt` — memory
mmocore-profesje-klucze). Silnik daje exp przez API MMOCore za: łapanie, wyklucie, ewolucję,
oswojenie (kwoty w `config.yml: profesja.exp`). Poziom profesji → limit drużyny/magazynu i bonus
do łapania.

---------------------------------------------------------------------------------------------
## Historia
- v0 (2026-09-22 21:00) — pierwsza wersja.
