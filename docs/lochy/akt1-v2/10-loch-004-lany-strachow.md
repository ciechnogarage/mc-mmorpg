# Loch 004 — Łany Strachów (karta szczegółowa v2, Projektant C, etap 5)

Wiąże: 05-synteza-v2 (R1–R5; R3 004: głośna droga pełnoprawna, bycie widzianym kosztuje, pola wykrywania JAWNE),
07-wspólne-liczby (L1–L7: boss 810, elita 25% = 200, EXP 480, moby Gaj v8 ×1,36; WOW „obrót głów” wyłącznie tu),
recenzje 06-Y (5 poprawek) i 06-P (P5: wybór po walce = ryzyko/czas, nie łaska). Odpowiedź w §15.

## 0. Nagłówek
| | |
|---|---|
| Poziom / otwarcie | 4 / od 3 |
| Czas Normalny solo | droga cicha 14–16 min, droga głośna 12–14 min (krótsza, gęstsza walka) |
| Fantazja | „Skradam się przez dojrzałe zboże pod okiem strachów na wróble, które nie powinny się ruszać.” |
| Emocja | groza skradania → zuchwałość (cios z łanu) → osaczenie w finale |
| Motyw | **Widzialność.** Gracz kucający w wysokim łanie jest niewidzialny dla Strachów. Widziany płaci (alarm: wrony, Kosiarz), ale może iść dalej. |
| Skutek dojrzałego pąka (L5: ukrycie) | **Snop**: kępa łanu 2×2 (2 bl.), czyli nowa kryjówka na ściernisku. Ma kształt chwiejnej kępy z wiechciem (nie nieruchomej osłony, jak w 009). Dwuznaczny: snop skoszony przez Kosiarza lub Żniwiarza pęka chmurą ziarna i ściąga 2 wrony na najbliższego gracza. Zdeptanie pąka nic nie daje (R1.2); depczesz, gdy pąk leży na pasie kosy. |

**Protokół R1** (jak 001 §0 i §13): jeden identyczny pąk na zgon; 5 s (Łatwy 8, Trudny 4), ostatnia 1 s świeci; dowolne
trafienie usuwa; przywołańce i Chowaniec depczą przechodząc; kolejka 12 per instancja wypiera NAJSTARSZY (usycha 1 s);
pąk gracza → Cierń (+Płatek dla depczącego), plan B do testu: bez pąka gracza. Pogoda ani nastrój nie zmieniają czasów.

## 1. Kishōtenketsu
| Etap | Sala | Co się dzieje | Rozumienie bez tekstu |
|---|---|---|---|
| Wstęp | S2 Ściernisko | Jeden Strach obraca głowę między 2 kierunkami. Jego stożek wzroku widać jako pas żółtego pyłu na ziemi (jawne pole). Obok prowadzi pas łanu. Przy Strachu żeruje nieświadomy Bocian, w którego gracz zadaje z łanu Pierwszy Cios. Bez kary: ten Strach nie ma wron (gniazdo na płocie jest puste). Za płotem uwiązany Snopowiec „oddycha” plewami (lekcja do zwrotu). | pyłek na ziemi = gdzie patrzy; oczy Strach zapala 1 s przed obrotem (`block.wood.hit` skrzyp) |
| Rozwinięcie | Pierwsze pole (W lub E) | Strachy z wronami: widziany gracz → alarm (3 wrony + najbliższy Kosiarz). Kosiarze idą stałymi pasami i KOSZĄ łan — kryjówka znika na oczach (BlockEditor: łan → ściernisko). Z pąków rosną Snopy — nowe kryjówki. | pas zboża faluje 1,5 s przed kosą; na ściernisku zostaje ostry dźwięk kroków `block.grass.step` głośniej |
| ZWROT | Drugie pole (zawsze to, które gracz wybierze jako drugie) | **Snopowcy** czekają w łanie jak gracz: kryjówka działa w obie strony. Przy przejściu ≤3 bl. snop wstaje. Gracz po raz pierwszy NIE widzi wroga. | **Oddech Snopowca**, niezależny od pogody: co 3 s z kępy unosi się obłoczek plew (`white_ash`) i słychać ciche sapanie (`entity.sniffer.sniffing` p1.4). Pokazany bezpiecznie wcześniej, w S2: uwiązany Snopowiec za płotem kapliczki. |
| Egzamin | S8 Klepisko | Żniwiarz widzi wszystko poza łanem i kosi łan pasami; Strachy na rogach areny są jego oczami — zwab kosę na Strachy, ukryj się, uderz po koszeniu. | patrz §6 |

## 2. Historia środowiskiem
- Pola Bractwa, które wykarmiły Stolicę w Głodnym Roku. Na płotach wiszą tabliczki z imionami — każdy Strach ma imię
  Ogrodnika (płócienna koszula z wyszytą gałęzią-pierścieniem, znakiem Bractwa). Pod kapeluszami pędy Szczepu zamiast słomy.
- Kapliczka przydrożna przy wejściu: świeże kwiaty — ktoś tu wciąż przychodzi (sekret: dezerter).
- **Odkrycie (samodzielne):** w Stodole księga zmian żniwnych: wszystkie imiona Ogrodników mają dopisek „pilnuje pola —
  na zawsze”, ostatni wpis ręką Darnika: „Przysięgli. Szczep dotrzymał za nich.” → Kronika 4: **strachy to Ogrodnicy,
  którzy przysięgli strzec żniw**.
- NPC **Wit, dezerter** (przebrany za Stracha przy kapliczce; dialog Papera, 3 kwestie):
  1. „Nie ruszaj się. Patrzą tylko tam, gdzie pył.” (gdy gracz go zaczepi — wtedy „Strach” się odzywa)
  2. „Kosiarze nie szukają. Idą swoim pasem. Kto wejdzie im w pas — zostaje ścierniskiem.”
  3. (po bossie) „Mój brat stał na wschodnim polu. Teraz może wreszcie położyć się w zbożu.”

## 3. Mapa (1 znak ≈ 4 bl., północ = góra)
```
                         [S8 KLEPISKO — arena 32x32, łan 6 pasów, 4 Strachy w rogach]
                                     |  (brama Stodoły, otwierana 2 połówkami klucza)
                         [S7 STODOŁA 24x16, ognisko2, sekret strych]  <- wieża-Strach olbrzymi 20 bl. = weenie
                          /                                   \
     [S6W KOPA ŻYTNIA 20x20: Przodownik W]           [S6E KOPA PSZENNA 20x20: Przodownik E]
              |                                                   |
     [S5W POLE ŻYTNIE 36x36: kwatery łanu,            [S5E POLE PSZENNE 36x36: kwatery łanu,
      3 Strachy, 2 Kosiarze-pasy]                      3 Strachy, Bociany, wrony]
              \               [S4 MIEDZA GŁÓWNA 8x60 — droga GŁOŚNA]            /
               \______________[S3 ROZSTAJE pagórek 16x16, y+5, WOW]_____________/
                                     |
                         [S2 ŚCIERNISKO NAUKI 20x24]
                                     |
                         [S1 KAPLICZKA 16x16, ognisko1] (wejście od południa)
```
- Dwa pola w dowolnej kolejności; w każdym **Kosiarz Przodownik** (elita) ma połówkę Klucza Stodoły — cel „dopaść
  przywódcę na jego polu” (skrytobójstwo albo pojedynek). Drugie wybrane pole dostaje Snopowców (MD `InstanceVariable`).
- **Droga głośna = Miedza Główna**: szeroka ścieżka po ściernisku między polami, prosto do obu Kop i Stodoły; 4 Strachy
  i 2 Bociany. Iść nią = być widzianym, ale alarm ma limit (§5 „Alarm”), a droga jest o ~2 min krótsza. Łup równy,
  ocena ma osobny warunek S dla każdej drogi (§10).
- Pętla: Kopa W ↔ Stodoła ↔ Kopa E (Stodoła ma wrota na oba pola); skrót: po otwarciu Stodoły drabina ze strychu na
  pagórek Rozstajów (ognisko1 → 30 s). Ruchoma mapa: pasy Kosiarzy (po 4 na pole) zamieniają się w ściernisko na
  stałe w tym przebiegu — powrót wygląda inaczej niż wejście.
- Pionowość: pagórek Rozstajów y+5 (widok na oba pola i Strachy), wieża Stodoły (strych y+8, sekret), Klepisko płaskie.
- Kwatery łanu w polu: 6×8 do 10×10 bl., przerwy-ścieżki 2–3 bl. (ściernisko); żadna kwatera nie jest dłuższa niż 12 bl.
  bez wroga, Stracha albo łupu.

## 4. Beat chart (Normalny solo; droga cicha / głośna)
| Sala | Rola | Min | Skład (dlaczego) | Start starcia | Tempo | Poza walką |
|---|---|---|---|---|---|---|
| S1 Kapliczka | odpoczynek | 0–1 | — | — | niskie | Wit udaje Stracha; uwiązany Snopowiec „oddycha” |
| S2 Ściernisko | nauka | 1–2,5 | 1 Strach bez wron + Bocian żerujący (Pierwszy Cios z łanu) | gracz zaskakuje | niskie→średnie | pas pyłu = wzrok |
| S3 Rozstaje | WOW, wybór | 2,5–3,5 | — | — | niskie | widok na pola i miedzę; wybór drogi |
| S5 pole 1 (cicha) | rozwinięcie | 3,5–6,5 | 3 Strachy, 2 Kosiarze na pasach, 1 Bocian; alarm wg limitu | gracz zaskakuje | średnie, skok przy alarmie | gniazdo wron, tabliczki z imionami |
| S4 miedza (głośna) | rozwinięcie | 3,5–5,5 | 4 Strachy, 2 Bociany, wrony wg limitu, 1 Kosiarz | widok i wejście | wysokie stale | krótszy dystans |
| S6 kopa 1 | elita | +1,5 | Przodownik + 1 Bocian; Pełen Krąg kosi kopę, więc kryjówki znikają w walce | z łanu (Pierwszy Cios) albo pojedynek | wysokie | połówka klucza |
| S5 pole 2 | ZWROT | +2,5 | 2 Strachy, 1 Kosiarz, **3 Snopowcy** w łanie | wróg zaskakuje (oddech zdradza) | wysokie | jedyny w lochu tekst: tabliczka „Nie śpij w zbożu” |
| S6 kopa 2 | elita (krewniak) | +1,5 | Przodownik 2 + 1 Snopowiec | jak wyżej | wysokie | druga połówka klucza |
| S7 Stodoła | odpoczynek, odkrycie | +0,5 | — | — | niskie | ognisko2, księga zmian (Kronika 4), strych |
| S8 Klepisko | egzamin | +3 | Żniwiarz + 4 Strachy-oczy; F2 Snopowcy | widok + wejście bez powrotu | szczytowe | — |
| S9 Ściernisko | nagroda | +0,5 | — | — | niskie | wybór żniwa (§9), skrzynia |
Zabójstwa w 10 s: pola ≤4, kopa ≤2, boss ≤3 (R1.5). Droga głośna przechodzi przez miedzę zamiast pola 1, ale pole 2
(zwrot) i obie kopy są na każdej drodze.

## 5. Bestiariusz (poziom 4 = Gaj v8 ×1,36: Korzeniak 54, Łucznik 35)
| Mob | Rola | Zastępczo / kandydat | HP / obrażenia | Zapowiedź | Atak karzący | Kontra | Nieuważny vs uważny |
|---|---|---|---|---|---|---|---|
| Strach na Wróble | wartownik | ARMOR_STAND z carved_pumpkin + MM / model własny (płótno, pędy) | 16; nie walczy | obrót: `block.wood.hit` + oczy-świetliki, 1 s; stożek 60° × 14 bl. jako żółty pył | alarm | wyjść ze stożka, kucnąć w łanie, zabić od tyłu | n: alarm w każdej strefie; u: 0–1 |
| Wrona Szczepu | kara za alarm | PHANTOM ×0,5, `PreventSunburn` / model własny | 5; nurkowanie 4; żyje 15 s | `entity.phantom.swoop` 0,8 s, cień na ziemi | nurkowanie w zamknięty punkt | krok z cienia, blok; kucający w łanie traci ją (wbija się w ziemię) | n: 12 obr./alarm; u: 0–4 |
| Kosiarz (jedyny humanoid) | patrol-tnący | HUSK z motyką / `rotter` przemalowany | 68 (1,25× Korzeniak); Zamach 13 (120°, r3,5) | `block.grindstone.use` 1,0 s | zamach w zamknięty stożek; kosi łan w pasie | wyjść z pasa, za plecy; 1,5 s kosy w ziemi | jak Korzeniak v8 |
| Bocian Szczepiony | dystans średni | STRAY z AI clear / model własny | 40; Dziobnięcie liniowe 9 (1×5) | klekot `entity.parrot.imitate.creeper` p0.5 + dziób w górę, 0,9 s | linia w zamkniętym kierunku | bok; 1,2 s zakleszczony dziób | jak Łucznik v8 |
| Snopowiec | zasadzka | HUSK z hay_block, AI wyłączone do wstania / model własny | 34; Uderzenie Snopem 8 + Spowolnienie I 1,5 s | **stały oddech** co 3 s (plewy + sapanie); wstaje 0,8 s (`block.grass.break` ×3) | przy ≤3 bl., potem goni 8 s | zauważyć oddech; trafiony śpiący = ×2 | n: 11 obr. z zaskoczenia; u: zabija przed wstaniem |
**Pierwszy Cios**: pierwsze trafienie moba bez celu daje +100% (skill `~onDamaged` z `Cooldown: 9999` na mobie).
Działa dla każdego źródła (R2). Nie dotyczy Strachów.
**Alarm (limit, jawny)**: strefy to pole W, pole E, miedza i Klepisko. Każda strefa ma najwyżej **jeden aktywny alarm**
(3 wrony, Łatwy 2, plus 1 Kosiarz zbaczający na 10 s), potem **40 s odnowienia** (oczy wszystkich Strachów strefy
przygasają na fioletowo: widać, że teraz „nie widzą”). Najwyżej 3 wrony żywe na gracza. Widzący Strach w czasie
odnowienia nie woła nikogo. Przy oczach przygaszonych głośna droga jest szybka i opłacalna.

## 6. Elita i Boss
### Kosiarz Przodownik (elita, S6W/S6E)
Funkcja: pojedynek o klucz z wyborem podejścia. Haczyk: **kosi kopę wokół siebie — każda faza walki odbiera ci kryjówki;
z łanu zaczniesz z przewagą, ale nie skończysz w ukryciu.** Historia: przodownik żniwnej drużyny Bractwa, klucz na szyi.
Wygląd: Kosiarz z wieńcem z pędów na głowie (sylwetka trójkątna: kosa nad głową); słaby punkt: klucz-medalion (jasny).
HP 200 (L2: 25% bossa); cel 40–50 s.
| Zdolność | Zapowiedź | Czas | Cel zamknięty | Kara | Okno |
|---|---|---|---|---|---|
| Zamach (stożek 120°) | `block.grindstone.use` | 1,0 s | tak | 13 | 1,5 s |
| Pełen Krąg (360°, r4, kosi łan wokół) | kosa w górę + `item.trident.riptide_1` | 1,6 s | wokół siebie | 16, łan r4 → ściernisko | 2 s (kosa w ziemi) |
| Rzut Sierpem (anty-kitowanie >8 bl., co 5 s) | `item.trident.throw` cicho, błysk | 0,9 s | tak | 8 | — |
| Wezwanie Bociana (50%) | gwizd `block.note_block.flute` | 1,2 s | — | 1 Bocian | — |

### Żniwiarz Bez Twarzy (boss, S8 Klepisko)
- **Funkcja**: egzamin z widzialności; emocja: osaczenie → triumf. **Haczyk**: „widzi wszystko, czego nie zasłania łan —
  oczami Strachów — i kosi łan, w którym się chowasz; zwab kosę na jego własne Strachy, a oślepnie.”
- **Historia**: pierwszy Strach, ustawiony przez Darnika na środku pól; przysięga strzeżenia żniw zrosła go z kosą.
  Nie ma twarzy — patrzy oczami pozostałych.
- **Wygląd**: Strach 2,5× gracza, płaszcz z płótna na krzyżu z pni, kosa dłuższa niż on. Sylwetka: krzyż + łuk kosy
  (trójkąt). Brak głowy — w miejscu twarzy pęk pędów; **słaby punkt: świecące serce z kłosów w piersi** (najjaśniejsze,
  odsłonięte, gdy kosa tkwi w ziemi). Model własny; zastępczo: STRAY ×2,5 (Options scale) z carved_pumpkin.
- **Arena**: klepisko 32×32 ogrodzone płotem; 6 pasów łanu 4×24 (60% powierzchni); centralna wieżyczka-cień 4×4 (w cieniu
  nie widać cię nawet na ściernisku). W rogach 4 Strachy-oczy (HP 40 każdy), które giną od dowolnych obrażeń, także
  z dystansu (R2). **Strzał w oko nie jest bezkarny**: trafiony Strach-oko obraca się ku źródłu; jeśli strzelec nie kuca
  w łanie, to alarm. Następny Pas Kosy celuje w kwaterę, z której padł strzał (hałas). Snajper musi po każdym trafieniu
  zmienić kwaterę.
- **HP 810** (L2; 3 min uważnie przy DPS 4,5 z oknami +50%).

| # | Zdolność | Faza | Zapowiedź | Czas | Cel zamknięty | Kara | Okno |
|---|---|---|---|---|---|---|---|
| 1 | Wzrok Strachów | 1–2 | każdy Strach-oko obraca głowę (stożek pyłu) co 5 s | 1 s | — | widziany → Sierp + wrony | ukryj się / ustaw się, by kosa przeszła przez Stracha |
| 2 | Rzut Sierpem | 1–3 | błysk na kosie, `item.trident.throw` | 1,0 s | tak (miejsce, gdzie widziany) | 12 | 1 s |
| 3 | Pas Kosy | 1–3 | pas zboża 4×24 faluje + `block.grindstone.use` | 1,5 s | linia w stronę ostatnio widzianego miejsca lub hałasu | 16; pas → ściernisko na stałe; **Strach w pasie ginie**; każdy skoszony snop = 2 wrony | 2 s: kosa w ziemi, serce odsłonięte (+50% obr.) |
| 4 | Wrony | 1–2 | krakanie `entity.phantom.ambient` | 0,8 s | — | 3 wrony na widzianego (limit alarmu: 1 aktywny, 40 s odnowienia) | kucanie w łanie |
| 5 | Młocka (F3, słuch) | 3 | cep w górę, `entity.ravager.stunned`, krąg r4 | 1,2 s | w punkt ostatniego HAŁASU (trafienie, sprint) | 13 + odrzut | 1,5 s |
| 6 | Snopy z ziemi (F2) | 2 | ziemia na ściernisku pęka `block.rooted_dirt.break` | 1,5 s | 2 punkty | 2 Snopowcy | — |
| 7 | Krok Strachów (anty-kitowanie) | 1–3 | jeśli gracz w łanie >10 s i >12 bl.: Pas Kosy celuje w JEGO kwaterę | 1,5 s | tak | jak 3 | — |

Fazy: **F1 Obchód 100–60%**: chodzi ringiem, Wzrok + Sierp + Pas co 12 s. **F2 Żniwa 60–25%**: Pas co 8 s, Snopowcy
na ściernisku, łan ubywa (po F2 zostaje ~25%). **F3 Ślepy Żniwiarz <25%** albo gdy wszystkie 4 Strachy padły
(wcześniej = nagroda za wabienie): nie widzi, słyszy — Młocka w miejsce trafienia/sprintu; zagrożenie przenosi się
z „bądź niewidoczny” na „uderz i zmień miejsce” (każde trafienie zdradza pozycję). Anty-kitowanie: płot (brak wyjścia),
zdolność 7, pasy kosy docierają do każdej kwatery.

**Tabela R2 (solo, bez specjalizacji)**
| | Ukrycie | Wabienie kosy na Strachy | Okno serca | Ryzyko |
|---|---|---|---|---|
| Wojownik | kucanie w łanie (każdy) | stanąć w linii z rogiem i zejść w łan | wręcz w 2 s okna | Sierp przy podejściu po ściernisku |
| Ninja | jak wyżej + szybki doskok | jak wyżej | seria w oknie | mało HP na Pas |
| Mag | jak wyżej; Strachy-oczy zabija zaklęciem z łanu (dowolne obrażenia) | może zamiast wabić strącać oczy z dystansu (wolniej: oko HP 40, każde trafienie zdradza kwaterę) | zaklęcie z dystansu w oknie | F3: każde trafienie zdradza → zmiana miejsca |
| Akolita | jak wyżej; sługi nie wywołują alarmu Strachów (wzrok tylko na graczy) | Chowaniec bije Strachy-oczy | sługi w oknie | sługi kosi Pas |

## 7. WOW
**3. minuta, S3 Rozstaje — wyjście z łanu na pagórek (y+5), widok na północ na oba pola.** Niebo ołowiane, bez deszczu
(`weather thunder` tylko na czas sceny 6 s, potem czysto z chmurami). Na polach stoi 14 Strachów; jednym skrzypnięciem
(`block.wood.hit` ×14 zsynchronizowane) wszystkie odwracają głowy w stronę gracza, ich oczy zapalają się; błyskawica
(`effect:lightning` bez obrażeń) nad wieżą Stodoły wycina sylwetkę olbrzymiego Stracha na szczycie; ~20 wron zrywa się
z pól (summon PHANTOM ×0,5 z `Despawn` po 5 s + cząsteczki `smoke`). Potem głowy wracają, gra się zaczyna.
Zbudowane: MD TriggerDistance na pagórku → MM skill (signal do tagu `strach` → obrót przez `rotate`/stan modelu).

## 8. Paleta
Nexo „łan” (2 bl., przechodni, płowo-złoty), hay_block, stripped_spruce_log (krzyże Strachów), dark_oak_fence, coarse_dirt
(ściernisko), carved_pumpkin (głowy), white_wool (płótno), cobblestone (kapliczka). Pora: późne popołudnie pod burzową
chmurą; światło rozproszone, szare; kontrast: blade zboże na ołowianym niebie. Dźwięk: wiatr w zbożu
(`block.grass.step` w pętli emiterów), krakanie z dala, skrzyp drewna; cisza przed obrotem głów.

## 9. Sekrety i wybór
| Sekret | Wskazówka | Warunek | Daje |
|---|---|---|---|
| Wit, dezerter | jeden Strach przy kapliczce NIE ma pyłu wzroku i ma świeże kwiaty u stóp | zaczepić (wejście w zasięg 2 bl.) | kwestie, mapa pól z pasami Kosiarzy (ułatwia drogę cichą) |
| Gniazdo wron (pole 1) | wrony krążą nad jednym krzyżem | dojść bez alarmu w tej strefie | Ząb Kosy (kosmetyk), EXP sekretu |
| Strych Stodoły | z Rozstajów widać światło w oknie strychu | wspiąć się po belkach | księga + wzór Kapelusza Strażnika (wygląd) |
| Ostatni Snop | jedna kępa na Klepisku nie kładzie się po walce | nie zwabić kosy w nią do końca walki | Ziarno Przysięgi (materiał tylko-tu, gotowanie) |
**Wybór po bossie (P5: ryzyko/czas, nie moralność)**:
- **„Zbierz żniwo”**: +2 min i walka z 3 Snopowcami na wyjściu (EXP tych mobów), za to kosmetyk „Snop na plecach”.
- **„Pozwól im się położyć”**: Strachy padają, otwiera się natychmiastowe wyjście (skrót), Wit mówi kwestię 3, a gracz
  dostaje kosmetyk „Wstążka Wita”.
Łup bojowy i materiały profesji są w obu wariantach identyczne (Y5).

## 10. Powtarzalność i ocena
Losowane przy wejściu:
- **Pasy Kosiarzy**: 2 z 4 układów na pole (inne kwatery znikają → inna droga cicha).
- **Kierunki Strachów**: każdy Strach dostaje 1 z 2 par kierunków obrotu.
- **Pogoda**: *Wiatr* (cały łan faluje, wrony nurkują o 0,2 s wolniej) / *Cisza* (kroki na ściernisku słychać dalej, Kosiarz zbacza do 6 bl.). Oddech Snopowca działa w obu.
- **Żniwiarz F2**: Snopowcy albo dwa Pasy naraz (węższe, 3×24).
**Ocena** (po 1 pkt): 0 śmierci · ≥2 Strachy-oczy ścięte kosą Żniwiarza · Ostatni Snop ocalały · sekrety ≥2/4 ·
**warunek drogi**: cicha = ≤1 alarm w całym przejściu; głośna = czas ≤13 min. Warunek wybiera się sam według trasy
  przez pole 1 albo miedzę.
S = 5, A = 4, B = 2–3, C = 0–1. Ocena daje tylko tytuł i wygląd (S: „Płaszcz z Płótna — wersja nocna”).

## 11. Nagroda (L3)
- EXP za pierwsze przejście na Normalnym: **480** (0,6 × 200 × 4): ~190 moby i elity, ~190 boss, ~100 sekrety;
  powtórka 200; Łatwy ×0,7, Trudny ×1,3; każdy członek drużyny osobno.
- Rodzina „Żniwna” (`L004_*`): Kosa Przodownika (dwuręczna), Sierp Żniwiarza (lekka), Płaszcz z Płótna Stracha,
  Kapelusz Strażnika Pól, Rękawice Snopowe.
- Materiały: `PROF_FLAX_FIBER`, zboże (istniejące PROF_* zbożowe, sprawdzić w recepturach), Ziarno Przysięgi (sekret),
  **Żeton Pól** (gwarantowany).
- Rzadko: Pieczęć Lochowa, Głowa Stracha (kosmetyk-hełm).
- Odblokowuje: Kronikę 4, cofnięcie plamy „Łany” na Mapie Granicy, Wita w Stolicy.

## 12. Trudności
| | Łatwy | Normalny | Trudny |
|---|---|---|---|
| Alarm | 2 wrony, Kosiarz zbacza 6 s, odnowienie 60 s | 3 wrony, 10 s, 40 s | 4 wrony + 2 Kosiarze, 30 s |
| Stożek Strachów | 45° × 10 bl. | 60° × 14 | 75° × 16, obrót co 3 s |
| Pąk | 8 s | 5 s | 4 s; skoszony Snop = 3 wrony |
| Żniwiarz | Pas co 14 s, F3 bez Młocki | jak §6 | 2 Pasy naraz, Snopowcy także w łanie |

## 13. Wykonanie
Wykrycie „widziany” = trzy kroki co 0,5 s. Plan A to vanilla i darmowe MM, bez warunku `blocktype` na blok Nexo:
1. **W łanie**: łan to Nexo stringblock, czyli vanillowy `minecraft:tripwire` ze stanami. Strach w `~onTimer:10` wykonuje
   `command{c="execute in <caster.l.w> as @a[distance=..16] at @s if block ~ ~ ~ minecraft:tripwire run tag @s add l004_lan"}`,
   a wcześniej zdejmuje tag u wszystkich w tej samej linii (`tag @a[distance=..16] remove l004_lan`).
2. **Stożek**: `execute as <strach> at @s positioned ^ ^ ^7 as @a[distance=..7,tag=!l004_ukryty] run tag @s add l004_widz`.
   To okrąg r7 przesunięty 7 bl. przed Stracha, przybliżenie stożka 60°×14 zgodne z pyłem na ziemi (pył rysowany tym
   samym okręgiem, więc wskazówka jest prawdziwa co do bloku).
3. **Ukryty** = `l004_lan` + kucanie: metaskill z TargetConditions `crouching` + `hastag{tag=l004_lan}` → tag
   `l004_ukryty` na 0,5 s. Alarm = metaskill z TargetConditions `hastag{tag=l004_widz}`.

Każdy warunek sprawdzić mobem-jednorazówką; pamięć mówi o cichej walidacji MM i o tym, że inline `?` to rzucający.
**Plan B**: łan = vanillowy `wheat` na grządkach w bruzdach o 1 bl. niżej (`if block ~ ~1 ~ minecraft:wheat`); reszta
bez zmian. Koszt: 0 dodatkowych encji, ~1 komenda na Stracha co 0,5 s (14 Strachów = 28 komend/s, pomijalne).
| # | Element | Ryzyko | Plan A | Plan B |
|---|---|---|---|---|
| 1 | Wykrycie | kolejność tagów w jednym ticku | 3 kroki w jednym metaskillu w kolejności 1→3→2 (pamięć: linie wykonują się po kolei na żywym stanie) | stały stożek bez obrotu w alarmowym ticku |
| 2 | „Reset aggro” (zarzut X) | vanilla AI goni | Kosiarze bez vanillowego celu (`AIGoalSelectors: clear` + patrol `goto`); alarm = 10 s `goto` do punktu alarmu i atak skillem w r3,5; wrony tylko w zamknięty punkt cienia. Nikt nie traci celu, bo nikt go nie ma. | Kosiarze nieruchomi na końcach pasów, kosa przechodzi pasem jako hazard |
| 3 | Koszenie łanu | liczba bloków | dwie komendy: `fill x1 y z1 x2 y+1 z2 air replace minecraft:tripwire` (łan znika, przejście wolne) i `fill x1 y-1 z1 x2 y-1 z2 coarse_dirt replace minecraft:farmland` (ściernisko tylko w podłożu); pas 4×24 = 192 + 96 bloków, współrzędne z generatora; test: po skoszeniu gracz i Kosiarz przechodzą przez pas (0 pełnych bloków na y, y+1) | 2 warianty kwater jako `.nbt`, `place template` |
**Test na klonie (przed budową)**:
- gracz kuca w łanie, na ściernisku i na granicy kwatery · Mag strzela w Strach-oko z łanu i ze ścierniska · głośna droga przechodzona Magiem bez skradania i Wojownikiem bez osłon (cel: ≤13 min, ≤40% HP straty na alarm)
Encje naraz: pole ≈ 3 Strachy + 2 Kosiarze + 1 Bocian + ≤3 wrony + ≤4 pąki = 13; boss ≈ 1 + 4 + 3 + 2 + 4 = 14.

## 14. Samokontrola
Brief §4:
- Wskazówki prawdziwe: pył = okrąg wykrycia, oddech Snopowca w każdej pogodzie ✓ · Najwyżej 1 komunikat w walce (alarm to dźwięk i wrony) ✓ · Tekst nie obiecuje rzeczy, których nie ma ✓ · Nagroda równa dla obu dróg i wyborów ✓ · Gracz wie, co robić (z Rozstajów widać dwie kopy z flagami Przodowników) ✓ · Walka karze ignorowanie mechaniki ✓ · Kitowanie zamknięte (płot, Rzut Sierpem co 5 s, zdolność 7, Pas za hałasem) ✓ · Głośna droga pełnoprawna z limitem alarmu ✓ · Losowość zmienia taktykę ✓
| Fun | Mechanika/zwrot | Czytelność | Topologia | WOW | Sens | Powtarzalność | Nagroda | Wykonalność | Różnorodność |
|---|---|---|---|---|---|---|---|---|---|
| 8 | 8 | 7 | 7 | 9 | 8 | 7 | 7 | 7 | 8 |

## 15. Odpowiedź na recenzję (etap 5)
| Poprawka | Źródło | Zmiana |
|---|---|---|
| Wskazówka Snopowca fałszywa przy Wietrze | Y1 | stały „oddech” (plewy + sapanie co 3 s) niezależny od pogody, pokazany bezpiecznie w S1/S2 (§1, §5) |
| Głośna droga oceniana gorzej, brak progów | Y2 | osobny warunek S dla obu dróg, progi S–C, droga głośna krótsza (12–14 min) (§0, §10) |
| Spiętrzenie alarmów | Y3 | 1 aktywny alarm na strefę, 40 s jawnego odnowienia (oczy fioletowe), maks. 3 wrony na gracza; miedza 4 Strachy (§5) |
| Wykrycie i koszenie niezweryfikowane | Y4 | plan A przepisany na vanilla `execute if block tripwire` + stożek `positioned ^ ^ ^7` + TargetConditions; `blocktype` Nexo usunięty; plan B wheat w bruzdach; koszt policzony; lista testów (§13). Samego testu nie przeprowadzam, bo brief zabrania dotykania serwera. |
| Wybór daje ×2 materiałów | Y5, P5 | wybór = ryzyko/czas; łup i materiały równe; różnica: walka/skrót i kosmetyk (§9) |
| R1 niepełne | Y-R1 | Protokół R1 w §0 |
| Snajperzy Maga na oczy bezkarni | Y-test | trafione oko obraca się i alarmuje, następny Pas celuje w kwaterę strzelca (§6) |
| Snop podobny do 009 | Y-powtórki, L5 | 009 zmienia skutek na Wstęgę (L5); tutaj snop ma chwiejną bryłę z wiechciem |
| Wspólne liczby | L2, L3 | boss 810, Przodownik 200, moby ×1,36, EXP 480 |
| Koszenie stawiało pełne bloki w przejściu | Y-runda2 | łan → `air`, `coarse_dirt` tylko w podłożu (y−1); test przechodniości po skoszeniu (§13) |
