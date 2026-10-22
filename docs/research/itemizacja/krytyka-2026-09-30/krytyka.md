# Krytyka systemu przedmiotów Aktu 1 (world1, wdrożenie 29.09)

Recenzja niezależna, 30.09.2026. Tylko odczyt. Podstawą są pliki na serwerze (`plugins/MMOItems/*`, `MythicMobs/*`,
`MythicLib/config.yml`, `MMOCore/classes|attributes`), `tools/przedmioty/tresc.py` w wersji WDROŻONEJ,
`output/przedmioty/sonda.json` (surowe komponenty wysłane do klienta), `evale.json`, research `docs/research/itemizacja/01–03`
i plan klas (artefakt Xy7cJTFt3wgYoouGnquPNZ).

> Uwaga: w trakcie recenzji (30.09, 11:08) inna sesja zmieniała już `tresc.py` i `symulacja.py`: rzadkość wybierana
> modyfikatorem, Epicki/Legendarny, do 3+3 affiksów, efekty legendarne. Oceniam to, co jest na serwerze, a zmiany w toku
> komentuję osobno w §6.

Obliczenia pomocnicze: `/tmp/itm-krytyka/moc.py` (moc zestawu 13 slotów na poz. 1/5/10 z tabel `tresc.py`, tym samym
algorytmem affiksów co wdrożony symulator) oraz `/tmp/itm-krytyka/render.py` (render surowych komponentów z sondy z kolorami).

---

## 0. Oceny

| Obszar | Ocena | Jednym zdaniem |
|---|---|---|
| **Ogólna** | **3/10** | Warsztat jest bardzo dobry (generator, bramka, evale botem, sonda pakietów), ale sama gra ma dziury, które wychodzą w pierwszej godzinie: obrona daje nieśmiertelność, HP z przedmiotów to 2–3× HP klasy, łup prawie nie trafia do gracza, a tooltip jest nieczytelny. |
| Sens struktury | 4/10 | 350 baz to głównie drabina tych samych przedmiotów ×1,04. Prawdziwe decyzje daje tylko implicit zbroi. Broń dwuręczna jest gorsza od jednoręcznej, a sety są praktycznie nieosiągalne. |
| Spójność z projektem | 4/10 | Poziom przedmiotu zależy od zabójcy, a nie od źródła (wbrew planowi). Plan mówił o 5 bazach zamiast 2. Bonusy biorą demo-skille, które plan wycina. Nazwy setów kolidują z nazwami klas i zasobów. Loch 001 nic nie daje. |
| Liczby | 2/10 | Płaska `defense` (MythicLib: `#damage# - #defense#`) zeruje ciosy mobów T1/T2/elit. HP z ekwipunku to 100–340% HP klasy. Pełny set B wymaga ok. 570 zabójstw bossów. |
| Czytelność tooltipu | 3/10 | Pierwsza linia to angielskie „Armor/Sword/Ring” na czerwono. Rzadkość jest na samym dole. Affiksy są odklejone od wartości. Widać trzy różne liczby poziomu, a set różni się od zwykłego przedmiotu tylko kolorem i wielką literą w nazwie. |
| Klimat i nazwy | 5/10 | Słownictwo broni jest świetne (gizarma, berdysz, glewia, pernacz, kropacz, pawęż, mizerykordia). Przy tym trafiają się łańcuchy dopełniaczy, śmieszności („Kaftan spodni wodza”, „Fetysz straży”), angielskie „charm” oraz kolizje z nazwami klas i zasobów. |

---

## 1. SENS: dużo przedmiotów, mało decyzji

### 1.1 Bazy to drabina, nie wybór
- **Broń i ozdoby:** 5 baz na rodzaj różni się tylko mnożnikiem `MNOZNIK_BAZY = 1.00/1.04/1.08/1.12/1.18` i wymaganym
  atrybutem 0/0/2/3/4. Implicit ozdoby zależy od typu i archetypu, nie od bazy (`AKC_IMPL`), więc „Rzemyk” i „Medalion”
  to ten sam przedmiot z innym numerem. Nie ma tu żadnej decyzji.
- **Podwójna oś postępu:** baza (poz. 1/3/5/7/10) i poziom przedmiotu (`level-item: true`, skala z ilvl) robią to samo.
  Baza 1 z ilvl 10 jest tylko o 18% słabsza od bazy 5 z ilvl 10. PoE i D4 mają jedną oś mocy (ilvl / item power),
  a baza różni się jakościowo (implicit, typ atrybutu).
- **Zbroja jest jedynym jasnym punktem.** Bazy mają różne implicity (unik / krytyk / mana / CDR) i różne wagi.
  Tu wybór bazy coś znaczy.
- **Skala:** 13 slotów × 5 baz × 4 archetypy = 350 przedmiotów na 10 poziomów. Plan klas (D5, sekcja „Co zmienia Akt 1”)
  mówi wprost: „dwie bazy na grupę broni i wagę zbroi (poziom 1 i ok. 5)”. Dla porównania Wynncraft daje 9 slotów
  (4 zbroja, 4 akcesoria, broń), D4 12, PoE 10.

### 1.2 Broń dwuręczna jest po prostu gorsza (DPS = obrażenia × szybkość, bazy poz. 1)
| Archetyp | 1H | 2H |
|---|---|---|
| Wojownik | miecz 5,5×1,6 = **8,8**; klinga 5,5×1,8 = **9,9** +3% kryt | halabarda 7,5×1,0 = 7,5; dwuręcznik 8,5×0,9 = 7,65; topór 7,0×0,9 = 6,3 |
| Akolita | buława 5,5×1,2 = 6,6; sierp 5,0×1,6 = **8,0** +3% kryt | młot 8,0×0,8 = **6,4** (mniej niż buława!); kosa 7,0 +1% kradzieży |
| Ninja | sztylet 3,5×2,2 = 7,7 +5% kryt | łuk długi 8,0 (bez tempa, ale 2H) |

Dwuręczna nie ma rekompensaty: ani wyższego DPS, ani dodatkowych affiksów. Do tego broń rasowa jest nierówna: elfie
sierp i klinga biją wszystko w swoich archetypach. W PoE broń 2H ma wyraźnie wyższy DPS bazowy niż 1H (rekompensata za
offhand). **Poprawka:** mnożnik DPS 2H ≈ 1,3–1,4× najlepszej 1H archetypu. Rasowe wyrównać budżetem (DPS + wartość
statystyk dodatkowych) z tolerancją ±5%, tak jak plan wymaga dla ras.

### 1.3 Affiksy: dobra pula, zła mechanika
- **Plus:** podział prefiksy = ofensywa, sufiksy = obrona (PoE). Pule są per archetyp (mana tylko u maga, zgodnie z planem).
  Przymiotnik odmienia się przez rodzaj.
- **Obietnica bez pokrycia:** strona mówi „nie ma dwóch +życie na jednym przedmiocie”, a Krzepki (prefiks) i Niedźwiedzia
  (sufiks) to ta sama statystyka `max-health`. Tak samo Pancerny i Żółwia (`armor`). **Sonda to pokazuje:**
  `PRZ_WOJ_HELM_3` ma Krzepki i Niedźwiedzia, a `PRZ_NIN_RING_4` daje +14,2 życia na jednym pierścieniu, czyli 70% HP
  ninji na poz. 7. PoE blokuje to przez *mod groups*, a nie przez nazwę rodziny. **Poprawka:** jedna statystyka = jedna
  rodzina. Życie tylko jako sufiks, a prefiks Krzepki zamienić na coś innego (np. % życia albo płaską obronę tylko na zbroi).
- **Tiery bez bramki poziomu:** T3 może wypaść na ilvl 1 (8%). W PoE i Last Epoch tier zależy od ilvl. Research (02 §1.5)
  potwierdza, że MMOItems tego nie umie. Obejście: osobne modyfikatory na przedziały ilvl w szablonach baz wyższych
  poziomów, bo baza 1 i 3 nie powinny mieć T3 w puli.
- **Brak decyzji:** gracz nie ma żadnego sposobu, żeby wpłynąć na affiks. Nie ma rerolla, orbów, forging potential
  (Last Epoch), tempering (D4) ani reforge (Hypixel). Jedyne „rzemiosło” to jakość, a ona jest bez ryzyka i bez wyboru
  (§1.5).

### 1.4 Sety: nieosiągalne i bez tożsamości
- **Szansa:** boss ma 20% na część setu A (16 części, 4 archetypy), czyli 5% na część własnego archetypu. Zebranie 4
  różnych części to problem kolekcjonera: 4·H₄/0,05 ≈ **167 zabójstw bossa**. Set B: 8% ÷ 4 = 2% na własną część,
  5 części → 5·H₅/0,02 ≈ **571 zabójstw bossa**. Bossów w Bestiariuszu są 3, Loch 001 ich nie ma. Smart loot jest
  wyłączony (`katalog.lup.smart = null`). W Akcie 1 nikt tego nie skompletuje.
- **Set B jest z poz. 10, a Akt 1 kończy się na poz. 10.** Części mają `level-item: false` i stałe liczby, więc w Akcie 2
  set natychmiast starzeje się.
- **Set jest słabszy od rzadkich:** część setu nie ma ani implicitu, ani affiksów. Bonus 4 cz. setu A (np. +5 uniku, +6%
  kryt, ruch, zdolność co 20 s) to mniej niż 4 × 3,5 affiksu z rzadkich. Last Epoch miał dokładnie ten problem („sety
  uchodziły za słabe”) i rozwiązał go reforgingiem setów na własne bazy (research 01 §4). WoW rozwiązał go Catalystem.
- **Anty-synergia:** „Znamię Bezimiennych” wymusza sztylet (broń setu), a 3 cz. daje +8% obrażeń pocisków.
- **Bonusy to demo-skille MythicLib** (SHOCKWAVE, SHADOW_VEIL, FIREBOLT, MAGICAL_SHIELD, BACKSTAB, VAMPIRISM,
  MINOR_HEALINGS, DEEP_WOUND). Plan: „Skille demo znikają”. `FIREBOLT ON_HIT` u maga odpala się tylko przy trafieniu
  bronią. Trzeba evalem sprawdzić, czy czar się liczy; jeśli nie, bonus 4 cz. maga jest martwy.

### 1.5 Jakość: obowiązkowy podatek, nie decyzja
+1% za kamień, 100% sukcesu, 20 kamieni na przedmiot, 13 slotów, czyli **260 kamieni** na komplet. Komplet wymieniasz co
2 poziomy (bazy 1/3/5/7/10), więc inwestycja przepada. Jakość mnoży wszystkie statystyki, także affiksy i `defense`.
W PoE quality podnosi tylko lokalne statystyki bazy, a w D4 masterworking jest drogi i stosuje się go pod koniec gry.
**Poprawka:** jakość tylko na implicit/bazę. Przenoszenie jakości przy wymianie przedmiotu (np. „rozbiórka zwraca 50%
kamieni”) albo jakość dopiero od Aktu 2.

### 1.6 Brak wyjścia dla śmieci
Skup przyjmuje tylko `PROF_*`, deconstruct nie jest skonfigurowany dla nowych tierów, sklep NPC nie przyjmuje sprzętu,
a w kategorii AH `sprzet` brakuje typów GREATBOW, SIERP, KOSA i FILAKTERIUM (research 03 §7). 80% łupu jest dla innych
archetypów (§3.4) i nie ma dokąd pójść. D4 ma salvage, Last Epoch shattering runą, PoE vendor recipes, a Wynncraft
sprzedaż lub złom u Kowala. **Poprawka (zgodna z „własne treści”):** rozbiórka u NPC warsztatu na `PROF_*`, z których
robi się kamienie jakości. To domyka pętlę profesje ↔ łup.

---

## 2. SPÓJNOŚĆ z projektem

| Założenie projektu | Stan | Werdykt |
|---|---|---|
| „Poziom przedmiotu pochodzi ze źródła (poziom moba lub lochu)” (plan, sekcja Przedmioty) | `Przedmioty.yml`: „Poziom przedmiotu = poziom zabójcy ±2” | **Sprzeczne.** Gracz z poz. 10, który bije zające (12 HP, jeden cios, 5% dropu), dostaje sprzęt ilvl 10–12. To najszybsza farma w grze. |
| „Dwie bazy na grupę i wagę” (D5) | 5 baz na slot | Przerost (§1.1) |
| „Baza wyznacza wygląd (model Nexo)” | wszystkie bazy mają materiały vanilli (np. `NETHERITE_SWORD` jako „Kieł nocy” poz. 10, `DIAMOND_*`) | Wbrew zasadzie „własne treści”. Netherite na poz. 10 mówi graczowi MC „endgame”. |
| „Skille demo znikają” | bonusy setów to demo-skille MythicLib (w toku także „efekty legendarne”) | Sprzeczne |
| Zasoby Szał/Skupienie+Znamiona/Mana/Dusze | mana tylko u maga ✔. Szał i Dusze nie mają żadnego affiksu ani bonusu. `docelowo` jest tylko opisem na stronie. | Połowicznie. Przedmioty w Akcie 1 nie dotykają rdzenia klas. |
| Atrybuty Siła/Zręczność/Intelekt/Wiara | wymagania 0/0/2/3/4 + sufiksy Byka/Lisa/Sowy/Pielgrzyma ✔ | OK. Wiara daje `max_mana 0.5`/pkt, więc sufiks Pielgrzyma podnosi maksimum **Dusz** (0–10): +3 Wiary = +1,5 Duszy. To prawdopodobnie niezamierzone. Sprawdzić z planem („co kilka punktów +1”). |
| Biegłości | wymagania biegłości w każdej bazie ✔, eval A4/A5 ✔ | OK |
| Profesje / `PROF_*` | kamienie jakości z `PROF_REFINED_IRON`, `CUT_STONE`, `LEATHER_PANEL`, `CLOTH_BOLT`, `CUT_GEM`, `ENCHANT_DUST` ✔ | OK, ale `AWAKENING_SEAL`, `EMPTY_SOCKET`, `METAL/FABRIC_REPAIR_KIT` dalej są martwe (research 03 §6). To naturalne wejścia dla rozbiórki i rerolla. |
| Krytyka Lochu 001 („MUSI: nagroda/EXP”) | `Loch_001.yml`: moby i Stary Korzeń dają tylko `PROF_*`, **zero sprzętu** | Sprzeczne. Jedyny dopieszczony loch nie daje nowego systemu. |
| Nazwy klas i zasobów | „Znamię Bezimiennych” (Znamię to zasób ninji!), „Kieł stepu” (baza włóczni) = klasa „Kieł Stepu”, „Srebrna cięciwa” (baza łuku) = specjalizacja „Srebrna Cięciwa”, „pasterz dusz” = motyw archetypu Akolity + baza 5 + set | Kolizje. Opisane w §4 i §5. |

---

## 3. LICZBY

### 3.1 KRYTYCZNE: `defense` odejmuje płasko
`plugins/MythicLib/config.yml`:
```
defense-application:
  natural: '#damage# - #defense#'
```
Domyślna formuła MythicLib jest „inspirowana PoE” i daje malejące zwroty. U nas ktoś zmienił ją na płaskie odejmowanie,
a system przedmiotów rozdaje `defense` szeroko: implicit zbroi straży/rycerskiej (2 + 0,2/ilvl **na każdą część**),
sufiks Twierdzy (2/4/6 + 0,2–0,4/ilvl, w 12 slotach wojownika i akolity) i set A wojownika (+10).

Obrona z kompletu (moc.py, średnia z 400 kompletów):

| | poz. 1 | poz. 5 | poz. 10 |
|---|---|---|---|
| Wojownik, same magiczne | 5 | 14 | 26 |
| Wojownik, same rzadkie | 10 | 22 | 39 |
| Akolita, same rzadkie | 12 | 18 | 26 |
| + set A wojownika (4 cz.) | | +10 | +10 |

Obrażenia mobów (MythicMobs `Damage`): T1 1–4, Loch 001 1–5, T2 7–12, elity 12–20, bossy 22–26.
**Wojownik na poz. 5 w magicznych przedmiotach (obrona ok. 14) nie dostaje nic od T1, T2 i Lochu 001, a od elity 0–6.
Na poz. 10 w rzadkich (39) jest nieśmiertelny także dla bossów.** Ninja i mag obrony nie mają, więc różnica między
archetypami robi się absurdalna. To trzeba potwierdzić jednym evalem (bot-wojownik, 4× Twierdzy, moby T2), bo formuła
jest w konfiguracji, a eval tego nie mierzył. Research o `defense-application` nie wspomina ani razu.
**Poprawka:** przywrócić formułę z malejącymi zwrotami (np. `#damage# * 50 / (50 + #defense#)` albo domyślną PoE) i
policzyć od nowa. Wyrzucić `defense` z implicitów zbroi niespecjalistycznej.

### 3.2 HP: ekwipunek daje 1–3,4× HP klasy
HP klasy według planu (etap 0): W 24 +0,6/poz., N 20 +0,4, M 18 +0,35, A 22 +0,5.

| Archetyp, poz. 10 | HP klasy | HP z EQ: zwykłe | magiczne | rzadkie |
|---|---|---|---|---|
| Wojownik | 29 | 15 (52%) | 40 (137%) | 75 (255%) |
| Ninja | 24 | 13 (55%) | 38 (159%) | 75 (319%) |
| Mag | 21 | 8 (40%) | 31 (148%) | 64 (302%) |
| Akolita | 27 | 29 (108%) | 54 (204%) | 91 (342%) |

Na poz. 5 w magicznych przedmiotach jest to już 107–125%. Pojedynczy przedmiot na poz. 1/5/10 daje średnio ok. 1/13
tych kwot, czyli **4–8% HP postaci** (rzadki pierścień z sondy: 60–70%). Plan klas kładzie HP wojownika 83 dopiero na
poz. 100, a rzadki komplet daje tyle na poz. 10. Progi z krytyki Lochu 001 („gracz przeżywa 5–7 trafień, ciężki atak
25–35% HP”) przestają działać przy pierwszym magicznym komplecie.
**Poprawka:** budżet mocy na slot (np. przedmiot na poz. 10 = maks. 8% HP klasy z implicitu + 1 affiks życia na komplet
co 3 sloty). Życie tylko na zbroi, pasie i naszyjniku (D4 trzyma życie na kilku slotach). Przyrost affiksu życia z ilvl
0,25 → 0,08.

### 3.3 Obrażenia i czas zabicia
Wojownik poz. 10, rzadkie: broń ok. 11,3 × (1 + 0,45 fiz.) × (1 + 9 pkt Siły × 2%) ≈ **19 na cios**, ok. 1,4 ciosu/s,
czyli ok. 27 DPS bez krytyka.

| Mob | HP | Czas zabicia |
|---|---|---|
| T1 (12–28) | | 1–2 ciosy |
| T2 (45–90) | | 3–5 ciosów |
| Elita (160–280) | | 6–10 s |
| Boss (800–1100) | | 30–40 s |

Samo zabijanie wygląda rozsądnie. Problem leży po stronie obrony i HP (§3.1–3.2), bo bossy nie są groźne.
Mag w rzadkich: +91% obrażeń magicznych (+18% z Intelektu), czyli podwojenie. Na Akt 1 to za dużo, ale nie jest
krytyczne.

### 3.4 Łup na minutę
Tabela T1 ma 140 pozycji równomiernie dla 4 archetypów, a smart loot jest wyłączony. Sensowne dla jednej klasy
(własny archetyp, broń wspólna i własnej rasy) jest ok. 28–30 pozycji, czyli **~20%**.

| Źródło | Szansa na przedmiot | Użyteczny dla klasy | Użyteczny magiczny+ | Użyteczny rzadki |
|---|---|---|---|---|
| T1 (5%) | 1 na 20 zabójstw | 1 na 100 | 1 na 290 | **1 na 1540** |
| T2 (10%) | 1 na 10 | 1 na 50 | 1 na 145 | 1 na 770 |
| Elita (30%) | 1 na 3,3 | 1 na 17 | 1 na 48 | 1 na 256 |
| Boss (100% ×2) | 2 | 0,4 | 0,14 | 1 na 38 |

Przy ok. 6 zabójstwach T1/T2 na minutę daje to **1 użyteczny przedmiot na 8–17 minut i 1 użyteczny rzadki na 2–4
godziny**. Cały Akt 1 to kilka godzin, więc gracz zobaczy ok. 10–15 swoich przedmiotów, z tego ok. 2/3 zwykłych, i
0–1 rzadki. Za to dostanie ok. 50 cudzych śmieci bez zbytu.
D3 Loot 2.0 dawał ok. 85% łupu pod klasę, D4 ma podobnie, a Wynncraft gwarantuje łup ze skrzyń lochu i nagrody z
questów.
**Poprawka:** smart loot 75–85% (`ARCHETYP_PH`, flaga jest gotowa), gwarantowany przedmiot z bossa każdego lochu
(pierwsze zabicie: rzadki pod klasę) i przedmioty z Lochu 001.

### 3.5 Inflacja i ilvl od zabójcy
Ilvl od zabójcy (±2) plus brak poziomu mobów sprawiają, że moc łupu nie zależy od trudności, tylko od poziomu gracza.
Najwydajniejsza farma to najsłabszy mob. PoE (ilvl = poziom obszaru), D4 (item power od poziomu moba) i Wynncraft
(stały poziom przedmiotu w strefie) robią to odwrotnie. **Poprawka:** poziom przedmiotu z moba lub lochu (Level MythicMobs
→ `level` w dropie, albo osobne tabele na pasmo ilvl), z górnym limitem poziomu gracza +2.

### 3.6 Jakość 20% i sety
Jakość mnoży wszystko, więc +20% do HP (§3.2) i do obrony (§3.1) pogłębia oba problemy. Bonusy setów w liczbach
(+6 życia, +10 obrony) są małe względem affiksów, oprócz +10 obrony, która przy płaskiej formule jest najmocniejszą
statystyką w grze.

---

## 4. CZYTELNOŚĆ tooltipu. Analiza surowych komponentów (sonda.json)

`PRZ_WOJ_HELM_3` (Rzadki), render komponentów `custom_name` + `lore`:
```
[yellow]Okrutny |[yellow]Szyszak straży Żółwia     ← nazwa: prefiks + baza z motywem w dopełniaczu + sufiks
[red]Armor                                          ← 1. linia: ANGIELSKI typ, na CZERWONO (kolor błędu)
                                                    ← pusta
[yellow]Wymaga poziomu 5                            ← wymagania na ŻÓŁTO, tym samym kolorem co rzadkość Rzadki
[yellow]Wymaga [red]2 [yellow]Siły
[yellow]Wymaga biegłości: [red]zbroja średnia
                                                    ← pusta
[dark_aqua]' '[gray]■ Obrażenia fizyczne: [white]+3.9%  ← wartości sumaryczne (implicit + affiksy razem), kropka dziesiętna
[gray]■ Obrona: [white]+1.6
[gray]✠ Pancerz: [white]+4.4
[gray]❤ Życie:[red] +6.4                            ← ułamkowe HP; czerwony = ten sam kolor co „błąd” w 1. linii
                                                    ← pusta
[gray]Zbroja: [white]średnia                        ← powtórzenie biegłości z 3 linii wyżej
[dark_gray]Wojownik · baza poz. 5                   ← 1. z trzech liczb „poziomu”
[dark_gray]◆ Okrutny [T1]                           ← affiksy BEZ wartości, ciemnoszare, pod statystykami
[dark_gray]◆ Krzepki [T1]                           ← 4 affiksy, a w nazwie widać 2 (Krzepki i Niedźwiedzia są niewidoczne)
[dark_gray]◆ Żółwia [T1]
[dark_gray]◆ Niedźwiedzia [T1]
                                                    ← pusta
[gray]Rzadkość: [yellow]Rzadki                      ← RZADKOŚĆ NA SAMYM DOLE (linia 19 z 20)
[dark_gray]Poziom przedmiotu: [gray]5               ← 3. liczba poziomu
```
Część setu `PRZ_SET_CALUN_NOCNEGO_LISA_KLATA`:
```
[green]Kurta Nocnego Lisa                           ← jedyna różnica od zwykłego: kolor i Wielkie Litery
[red]Armor
...
[gray]✠ Pancerz: [white]+4.4                         ← jedna statystyka
[gray]Zbroja: [white]lekka
[dark_gray]Ninja · zestaw poz. 5
[green+bold]Całun Nocnego Lisa [dark_gray](4 części) ← set dopiero w 10. linii
[dark_gray]• 2 cz.: [gray]+5 uniku                   ← „uniku” bez %, a statystyka pokazuje „Szansa uniku: +x%”
[dark_gray]• 4 cz.: [gray]+6% szansy na krytyka, szybszy ruch; gdy obrywasz, co 20 s znikasz w cieniu  ← 1 linia ~95 znaków
[gray]Rzadkość: [green]Zestaw                         ← „Rzadkość: Zestaw” to kategoria, a nie rzadkość
                                                     ← brak „Poziomu przedmiotu” (niespójne z bazami)
```

### 4.1 Dlaczego właściciel nie odróżnia setu
1. **Słownictwo setów jest takie samo jak zwykłych baz.** Liczyłem nazwy z `katalog.json`:
   - „pasterz(a) dusz” występuje w 13 zwykłych przedmiotach (baza 5 akolity i ozdoby) oraz w secie „Całun Pasterza
     Dusz” („Buty pasterza dusz” obok „Sandały Pasterza Dusz”);
   - „bezimiennych” jest w 8 zwykłych i w secie („Pierścień pieczętny bezimiennych” obok „Pierścień Bezimiennych”);
   - „straży” w 13, „wodza” w 9, „nocnego …” w 12.

   Różni je tylko wielka litera.
2. **Nazwa setu = nazwa części:** napierśnik akolity nazywa się „Całun Pasterza Dusz”, tak samo jak set. „Całun” to
   też drugi set (Nocnego Lisa).
3. **Informacja o secie jest na dole** (linia 10+). Minecraft ucina długie tooltipy i gracz czyta od góry. W hotbarze
   i w czacie widać tylko nazwę.
4. **Zielony `&a` jest blisko niebieskiego `&9`** (Magiczny) przy słabym ekranie, a nazwa nie ma żadnego znacznika.
5. **Brak ramki:** tier ma `tooltip-style` (1.21.2+), a nie jest używany.

### 4.2 Pozostałe wady (względem Wynncrafta, D4, PoE, Hypixela)
- Research (01 §6, „Zasady dla naszego tooltipu”) sam zalecał kolejność: **typ i rzadkość → staty bazy → kreska →
  affiksy z tierem → set → wymagania ✔/✘**. Wdrożenie tego nie zrobiło.
- D4: druga linia to „Legendary Helm” (rzadkość + slot), potem item power, implicity, affiksy, moc, na końcu wymagania
  (maxroll D4 Equipment). PoE: nagłówek w kolorze rzadkości, typ bazy, właściwości, wymagania, implicit, kreska,
  affiksy. Wynncraft: rzadkość na dole, ale wymagania mają ✔/✖, a identyfikacje zielony/czerwony i %.
- Tier affiksu `[T1]` = najsłabszy (konwencja Last Epoch), a w PoE `T1` = najlepszy. Gracz przychodzący z PoE przeczyta
  to odwrotnie. Piktogram ◆◇◇ / ◆◆◇ / ◆◆◆ jest jednoznaczny.
- Kropka dziesiętna (`MythicLib number-format.decimal-separator: .`), a opisy setów mają przecinek („+0,2”). Są ułamki
  HP (+6.4) i surowe liczby silnika: „Szybkość ruchu: +0.002”, „Regeneracja życia: +0.05”. Gracz nie wie, co to znaczy.
  Hypixel i Wynn pokazują ruch w %.
- „Obrona” i „Pancerz” wyglądają jak synonimy, a działają zupełnie inaczej (płaskie odejmowanie vs vanilla).
- „Wymaga biegłości: sztylety i pięściaki” obok „Broń: sztylety”.
- Wymagania nie mówią, czy je spełniasz (MMOItems nie koloruje dynamicznie, Wynn ma ✔/✖).
- Jakość to dopisek w nazwie „[Jakość +3%]”, więc nazwa robi się jeszcze dłuższa. Hypixel stosuje ✪ w nazwie, PoE
  linię „Jakość: +20%”.
- Długość nazwy: „Krzepki Pierścień z oczkiem nocnego łowcy Niedźwiedzia” (54 znaki, 3 dopełniacze).

---

## 5. Makiety tooltipów (linia po linii, kody §)

Ograniczenia MMOItems:
- `lore-format.yml` jest globalny, ale można przenosić linie `#tier#`, `#lore#` i `#set#` na górę oraz dodać własny format
  per przedmiot (`lore-format: <id>` w `language/lore-formats/`), np. osobny dla części setów;
- `#item-type#` usunąć albo przetłumaczyć w `item-types.yml`;
- linia tieru ma format ze `stats.yml` (`tier: '{value}'`), a nazwa tieru jest dowolnym tekstem z kolorem;
- `lore-tag` setu jest statyczny (bez 2/4);
- prefiks i sufiks idą tylko do nazwy;
- `tooltip-style` tieru to ramka z packa Nexo (`assets/<ns>/textures/gui/sprites/tooltip/<id>_background|_frame.png`,
  Minecraft Wiki „tooltip_style”).

**Zasada rozpoznawalności:** set rozpoznajesz po ramce, zanim cokolwiek przeczytasz, po znaku ❖ w nazwie (widocznym
także w hotbarze i czacie) i po linii 2 „Część zestawu …”. Znak ❖ i zieleń są zarezerwowane tylko dla setów, a nazwy
zwykłych baz nie mają motywu (motyw archetypu idzie do linii 2).

### 5.1 Zwykły (ramka domyślna)
```
§fCzepiec                                   ← nazwa bazy BEZ motywu i dopełniacza
§7Zwykły hełm §8· zbroja średnia · Wojownik  ← #tier# + #lore#: rzadkość i slot od razu (D4)
§8§m                    §r                     ← kreska
§7Pancerz §f4
§7Życie §c+1
§8§m                    §r
§8Poziom przedmiotu §75  §8· wymaga poz. §75§8, §72 Siły
```

### 5.2 Rzadki (ramka `nexo:rzadki`, żółta)
```
§eOkrutny Czepiec Żółwia                     ← krótko: prefiks + baza + sufiks (bez motywu)
§eRzadki hełm §8· zbroja średnia · Wojownik
§8§m                    §r
§7Pancerz §f5    §7Obrona §f2                 ← implicit/baza (wartości zaokrąglone, przecinek)
§8§m                    §r
§6◆◆◇ §f+5% §7obrażeń fizycznych   §8Okrutny  ← każdy affiks: pips tieru + WARTOŚĆ + nazwa
§6◆◇◇ §f+2 §7pancerza               §8Żółwia
§6◆◇◇ §c+4 §7życia                  §8Niedźwiedzia
§8§m                    §r
§8Poziom przedmiotu §75  §8· wymaga poz. §75§8, §72 Siły
```
Linie affiksów z wartością można zrobić w MMOItems dziś, jeśli affiks ma **stałą wartość na tier** (bez `spread` i bez
skali z ilvl, bo o mocy decyduje wtedy tier zależny od pasma ilvl). Wtedy statyczna linia `stats.lore` modyfikatora mówi
prawdę. Blok statystyk MMOItems dalej pokazuje sumy, więc go skrócić (implicit), a affiksy dać jako linie opisowe.
Wersja docelowa (jak Wynncraft): mały renderer lore na pakietach, który czyta StatHistory MMOItems i dopisuje ✔/✖ przy
wymaganiach.

### 5.3 Część setu (ramka `nexo:zestaw`, zielona, plus `enchantment_glint_override`)
```
§a❖ Kurta Nocnego Lisa
§aCzęść zestawu §2§lCałun Nocnego Lisa §8· zbroja lekka
§8§m                    §r
§7Pancerz §f5    §7Unik §f+3%                 ← część setu MA implicit (nie jest gorsza od bazy)
§8§m                    §r
§2❖ Całun Nocnego Lisa §8— 4 części
§8   Kaptur · Kurta · Spodnie · Ciżmy          ← lista części (WoW, D2)
§a (2) §7+5% szansy uniku
§a (4) §7+6% szansy na krytyk, +10% szybkości ruchu
§a     §7Gdy obrywasz: znikasz w cieniu §8(co 20 s)
§8§m                    §r
§8Wymaga poz. §75§8, §73 Zręczności
```
Licznika „2/4” statyczny `lore-tag` nie da. Nadrabia go ekran postaci albo renderer z §5.2. WoW i D2 podświetlają
posiadane części, Wynncraft pokazuje bonus zależny od liczby części.

### 5.4 Legendarny (ramka `nexo:legendarny`, pomarańczowa)
```
§6★ Kieł Pierwszego Świtu                    ← nazwa własna, nie „prefiks + baza + sufiks”
§6Legendarny sztylet §8· Ninja
§8§m                    §r
§7Obrażenia §f9 §8· szybkość §f2,2
§8§m                    §r
§6◆◆◆ §f+6% §7szansy na krytyk ...           ← affiksy jak w rzadkim
§8§m                    §r
§6✦ Moc: §eFinisher zużywający 5 Znamion zwraca 30 Skupienia  ← efekt zmieniający RDZEŃ klasy
§8§o„Świt nie pyta, kto spał.”                 ← jedna linia fabuły
§8Poziom przedmiotu §710 §8· wymaga poz. §710
```

---

## 6. RZADKOŚCI 5-stopniowe: rekomendacja

### 6.1 Jak robią inni
| Gra | Stopnie i kolory | Czym różni się wyższy stopień |
|---|---|---|
| Last Epoch | Normal biały, Magic niebieski 1–2 affiksy, Rare żółty 3–4, **Exalted fioletowy** (co najmniej 1 affiks T6–T7, tylko z dropu), Unique pomarańczowy, Set zielony, Legendary czerwony (unikat + affiksy exalted) | **Jakość affiksu, nie liczba** (maks. 4) |
| Diablo 4 | Common biały, Magic niebieski 1–2, Rare żółty 3, Legendary pomarańczowy 4 + aspekt (moc), Unique złoty (stałe affiksy + unikalna moc) | Legendary = moc zmieniająca skill |
| PoE | Normal, Magic 1+1, Rare do 3+3 (żółty), Unique (brąz/pomarańcz) | Unique = mechanika |
| Wynncraft | Normal biały, Unique żółty, Rare różowy, Legendary błękitny, Fabled czerwony, Mythic fioletowy, Set limonkowy | Rzadsze = silniejsze + większe/unikalne identyfikacje. Set osobnym kolorem. |
| Hypixel SkyBlock | Common, Uncommon zielony, Rare niebieski, Epic fioletowy, Legendary złoty, Mythic różowy, Divine | Liczby, reforge skaluje się z rzadkością |
| WoW / Lost Ark | szary/biały/zielony/niebieski/**fioletowy Epic**/**pomarańczowy Legendary** | Epic = lepsze liczby, Legendary = efekt |
| Minecraft vanilla | komponent `rarity`: common biały, uncommon żółty, rare aqua, **epic `&d`** | |

Wspólny mianownik: **Epicki = fiolet, Legendarny = pomarańcz/złoto, Zestaw = zieleń**. Gracze MC znają fiolet `&d` z
vanillowego „epic”.

### 6.2 Co u nas powinno odróżniać Epicki i Legendarny
| Stopień | Kolor / ramka | Affiksy | Tożsamość | Udział w dropie (z przedmiotów) |
|---|---|---|---|---|
| Zwykły | `&f` / domyślna | 0 | tylko baza | 60% |
| Magiczny | `&9` / niebieska | 1–2 | | 30% |
| Rzadki | `&e` / żółta | 3–4 (maks. 2+2) | | 8,5% |
| **Epicki** | `&d` / fioletowa | **4 (jak rzadki)**, w tym **≥1 affiks „wzniesiony” ✦** (tier T4 dostępny wyłącznie tutaj, poza zasięgiem rzadkich) | model Last Epoch Exalted: lepsza *jakość*, nie więcej linii | 1,3% |
| **Legendarny** | `&6` / pomarańczowa, `★` w nazwie | 2–4 stałe affiksy (zakresy) | **nazwany przedmiot z mocą zmieniającą rdzeń klasy** (Szał/Znamiona/Mana/Dusze), 1–2 na archetyp w Akcie 1 | 0,2% + **gwarantowany pierwszy z bossa Lochu 001/010** (pity) |
| Zestaw | `&a` / zielona, `❖` | implicit + stałe | bonusy progów | tylko z bossów, smart + pity |

„Unikatowy” proponuję scalić z Legendarnym (D4 i LE mają dwa stopnie, ale przy 16 kolorach MC kolor złoty `&6` i
pomarańcz się zlewają) albo przenieść na hex z brązem PoE, jeśli MythicLib go przyjmie w nazwie tieru.

### 6.3 Ocena zmian w toku (tresc.py z 30.09 11:08)
- **Epicki 4–5 i Legendarny 5–6 affiksów (3+3)** to Hypixel/WoW („więcej tego samego”). Przy obecnym budżecie HP (§3.2)
  pogłębia to inflację. Przy 13 slotach 6 affiksów na poz. ≤10 daje HP z EQ rzędu 4× HP klasy. Epicki trzymać na 4
  affiksach i dawać mu lepszy tier (pasmo `'e'` w nowym kodzie idzie w dobrą stronę), a nie więcej linii.
- **Efekty legendarne = te same demo-skille co bonusy setów** (SHOCKWAVE, SHADOW_VEIL, FIREBOLT, MAGICAL_SHIELD,
  BACKSTAB, VAMPIRISM, MINOR_HEALINGS, DEEP_WOUND). Legendarny przedmiot duplikuje 4. próg setu, więc obie kategorie
  tracą tożsamość, a plan zapowiada wycięcie demo-skilli. Moce legendarne muszą dotykać zasobów klas (jak `docelowo` w
  setach) albo poczekać na etap 3.
- `UNIKATOWY` zniknął z listy RARITY. Sprawdzić, czy nic go nie referuje (sprawdz.py).
- Rzadki podniesiony do 9%, ale smart loot dalej jest wyłączony, więc problem z §3.4 zostaje.

---

## 7. Lista problemów

### MUSI (przed kolejnymi graczami)
1. **Płaska obrona łamie walkę.** `MythicLib natural: '#damage# - #defense#'` + defense w implicitach, w sufiksie Twierdzy
   i w secie. Poprawka: formuła z malejącymi zwrotami, `defense` tylko u specjalizacji obronnych i na ciężkiej zbroi,
   potwierdzone evalem (bot vs T2). *Źródło: domyślna formuła MythicLib / PoE Armour.*
2. **Budżet HP.** Rzadki komplet daje 255–342% HP klasy na poz. 10. Poprawka: budżet mocy na slot w generatorze i
   bramka „HP z EQ ≤ 60% HP klasy na poz. 10” (w sprawdz.py, tak jak bramka klas). Jedna statystyka = jedna rodzina
   affiksów (koniec Krzepki + Niedźwiedzia). *Źródło: PoE mod groups, D4 ograniczenia slotów.*
3. **Poziom przedmiotu od źródła, nie od zabójcy.** Farmienie zajęcy daje ilvl 10. *Źródło: plan klas; PoE area
   level; D4 item power.*
4. **Łup, który trafia do gracza:** smart loot 75–85%, sprzęt z Lochu 001 (gwarantowany rzadki pod klasę za pierwsze
   zabicie Starego Korzenia), pity na sety. Dziś jest 1 użyteczny rzadki na 2–4 h, a set B wymaga ok. 570 bossów.
   *Źródło: D3 Loot 2.0, WoW Catalyst, Wynn nagrody lochów.*
5. **Set musi być widoczny od pierwszej sekundy:**
   - ramka `tooltip-style` per tier (zestaw zielona);
   - znak ❖ w nazwie tylko dla setów;
   - linia 2 „Część zestawu X”;
   - blok setu wyżej;
   - zwykłe bazy bez motywów („pasterza dusz”, „bezimiennych”, „straży”, „wodza”, „nocnego łowcy”);
   - część ≠ nazwa setu;
   - jeden „Całun”.

   *Źródło: D2/Wynn/LE (zielony set), WoW (lista części), MC 1.21.2 tooltip_style.*
6. **Pierwsza linia tooltipu:** angielskie „Armor/Sword/Ring/Dagger/Consumable” na czerwono zastąpić linią
   „Rzadkość + slot” (D4). Rzadkość z dołu przenieść na górę. Wymagania na koniec, na szaro (nie na żółto jak Rzadki).
7. **Rozbiórka/zbyt dla przedmiotów** (80% łupu to cudze bazy): NPC warsztatu rozbiera na `PROF_*`. Uzupełnić kategorię
   AH `sprzet` o GREATBOW, SIERP, KOSA, FILAKTERIUM. *Źródło: D4 salvage, LE shattering, Wynn Kowal.*

### POWINNO
1. Zredukować bazy do 2–3 na slot (decyzja D5) i zróżnicować je jakościowo (implicit, typ atrybutu), a nie mnożnikiem ×1,04.
2. Broń 2H: DPS ×1,3–1,4 względem 1H. Wyrównać bronie rasowe budżetem (sierp i klinga dziś wygrywają).
3. Bonusy setów i moce legendarne przepiąć na zasoby klas (opisy `docelowo` są dobre). Nie używać demo-skilli
   MythicLib. Zweryfikować `ON_HIT` u maga.
4. Set B: `level-item` albo poziom 8, a nie 10 (dziś żyje jeden poziom). Część setu ma implicit równy bazie.
5. Jakość: tylko na implicit, częściowy zwrot przy rozbiórce albo dopiero od Aktu 2 (dziś 260 kamieni na komplet, który
   żyje 2 poziomy).
6. Tiery affiksów: piktogram ◆◇◇ zamiast `[T1]`. Bramkować T3 pasmem ilvl (osobne modyfikatory dla baz 1–3).
7. Liczby: przecinek dziesiętny (MythicLib `number-format`), HP i pancerz całkowite, ruch i regeneracja w % oraz „/s”.
   „Obrona” opisana (np. „Obrona (odejmuje obrażenia)”).
8. Wartości affiksów stałe na tier. Wtedy linia affiksu może pokazać wartość (§5.2).
9. Wygląd: stuby Nexo dla baz. Nie netherite i diament na poz. 10 (zasada „własne treści”, plan: „baza wyznacza model”).
10. Wiara → `max_mana` podnosi maksimum Dusz. Potwierdzić, że to zamierzone.

### MOŻNA
1. Renderer lore na pakietach (StatHistory MMOItems): wartość per affiks, ✔/✖ przy wymaganiach, licznik części setu
   „2/4” (Wynn, WoW).
2. Ograniczone rzemiosło affiksów z martwych `PROF_*` (EMPTY_SOCKET, AWAKENING_SEAL) na wzór Forging Potential z Last
   Epoch.
3. Jakość jako ✪ w nazwie (Hypixel) zamiast „[Jakość +3%]”.
4. Glint w kolorze rzadkości na ziemi (Wynn) jest już częściowo przez `item-glow`. Dopracować też ikonę niezidentyfikowanego.
5. Rzadkim nadawać losowe nazwy dwuczłonowe (PoE/D2 „Doom Cry”), żeby odróżnić je od magicznych nie tylko kolorem.

---

## 8. Nazwy i klimat: konkretne wpadki

| Nazwa | Problem | Propozycja |
|---|---|---|
| „Kaftan spodni wodza” | czyta się „kaftan (od) spodni wodza” | „Kaftan spodni” → „Przeszywanica” / „Gambeson” |
| „Fetysz straży” | „fetysz” ma w polszczyźnie konotację seksualną | „Amulet”, „Talizman”, „Znak” (już jest), „Wotum” |
| „Ozdoba: charm” | angielskie słowo w lore (`AKC_PL['CHARM']`) | „talizman” |
| „Kolczyki” jako baza 5 slotu na jeden kolczyk | liczba mnoga | „Kolczyk z perłą” |
| „Barki straży”, „Epolety”, „Pauldrony” | barki = też łodzie, epolety to XIX w., pauldrony to anglicyzm | „Naramienniki” + przymiotnik („okute”, „łuskowe”, „płytowe”) |
| „Mitenki straży” | mitenki = rękawiczki bez palców, komicznie | „Rękawice kolcze” |
| „Relikwia nocnego łowcy/wodza” | relikwia u ninji i wojownika? | relikwie tylko u akolity |
| „…Szyszak straży Żółwia”, „Pierścień z oczkiem nocnego łowcy Niedźwiedzia” | 2–3 dopełniacze, sufiks czyta się jako część motywu („straż Żółwia”) | bazy bez motywu, sufiks z „…” lub przymiotnik („Żółwi Czepiec”) |
| „Znamię Bezimiennych” | Znamię = zasób ninji | „Pieczęć Bezimiennych” |
| „Kieł stepu” (baza) / „Srebrna cięciwa” (baza) | identyczne z klasą „Kieł Stepu” i specjalizacją „Srebrna Cięciwa” | zmienić bazy |
| „Całun Pasterza Dusz” (set i napierśnik), „Całun Nocnego Lisa” | dwa „Całuny”, część = nazwa setu | „Szaty Pasterza” / „Płaszcz Nocnego Lisa” |
| „Kopia korony” w grupie drzewcowej | kopia to lanca konna | „Partyzana”, „Korseka” |
| „Rzadkość: Zestaw” | zestaw nie jest rzadkością | „Część zestawu” w linii 2 |

Na plus: broń drzewcowa, obuchowa i łańcuchowa ma staropolskie, trafne słownictwo. Motywy archetypów (najemnik →
czempion, uczeń → arcymag, nowicjusz → pasterz) dają czytelną drabinę. Odmiana przymiotnika przez rodzaj działa.

---

## Źródła
- Last Epoch Support, Rarity / What do item colors mean: https://support.lastepoch.com/hc/en-us/articles/46362011564187-Rarity ,
  https://support.lastepoch.com/hc/en-us/articles/46363247127963-What-do-item-colors-mean ; T6/T7: https://forum.lastepoch.com/t/introducing-tier-6-and-7-item-affixes/22279
- Diablo 4 (Maxroll Equipment): https://maxroll.gg/d4/resources/equipment ; Icy Veins itemization: https://www.icy-veins.com/d4/guides/gear-systems-and-itemization-overview/
- Wynncraft Item Rarity: https://wynncraft.wiki.gg/wiki/Item_Rarity
- Hypixel SkyBlock Rarity: https://wiki.hypixel.net/Rarity , https://hypixelskyblock.minecraft.wiki/w/Rarity
- Minecraft tooltip_style: https://minecraft.wiki/w/Data_component_format/tooltip_style , snapshot 24w36a: https://www.minecraft.net/en-us/article/minecraft-snapshot-24w36a
- PoE, WoW Catalyst, D3 Loot 2.0, LE sets: zebrane w `docs/research/itemizacja/01-gry-analiza.md` (z linkami).
- Pliki: `plugins/MythicLib/config.yml` l.32–34; `plugins/MythicMobs/mobs/{Bestiariusz,Loch_001}.yml`;
  `plugins/MythicMobs/droptables/{Przedmioty,Bestiariusz}.yml`; `plugins/MMOItems/{item-tiers,item-sets}.yml`,
  `modifiers/przedmioty.yml`, `item/*.yml`; `plugins/MMOCore/classes/czlowiek/*.yml`, `attributes/default_attributes.yml`.
