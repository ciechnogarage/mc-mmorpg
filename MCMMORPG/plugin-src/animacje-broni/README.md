# AnimacjeBroni — walka bronią MMOItems (Paper 1.21.11)

## Sterowanie

- **LPM:** lekki, vanillowy cios. Trzy trafienia tworzą łańcuch; kolejny cios od 70% odnowienia ataku, do 1,2 s po poprzednim. Zbyt wczesny cios pozostaje zwykły i nie zrywa łańcucha.
- **PPM przytrzymaj i puść:** mocny cios. Poniżej 6 ticków jest szybkie szturchnięcie (×0,5, bez staminy). Od 6 ticków obrażenia rosną liniowo do pełnego naładowania broni (12 ticków lekkie, 24 ciężkie). Po 60 tickach działa zmęczenie: ×1,0, a stamina nadal jest pobierana. Pudło kosztuje tyle samo co trafienie. Oba warianty mają około 1 s odnowienia.
- **Shift + PPM:** garda i parowanie z przodu. Po parowaniu puść PPM i użyj LPM do riposty. Wciśnięcie Shift już podczas ładowania nie zamienia ataku w blok; najpierw trzeba puścić PPM.
- **Sprint + PPM z halabardą:** vanillowa szarża kinetyczna. Rozpocznij sprint przed wciśnięciem PPM.

PPM przejmuje plugin tylko z pustą lewą ręką i bez umiejętności MMOItems na PPM. Zaklęcie, tarcza lub inny przedmiot w lewej ręce mają pierwszeństwo. Włócznie `SPEAR`, łuki/kusze, różdżki/kostury magiczne, grimuar, totem, filakterium, tarcze i bicze nie mają mocnego ciosu. LPM nie dociera do serwera podczas trzymania PPM na używanym przedmiocie — to cecha klienta vanilla.

## Charakter broni

| Grupa (typy MMOItems) | Lekki łańcuch LPM | Mocny PPM: W / A,D / S / sprint / spadanie | Krótki PPM |
|---|---|---|---|
| Miecz (`SWORD`, `LONG_SWORD`, `KATANA`) | cięcie z prawej → z lewej → pchnięcie z przebiciem | pchnięcie w linii / szeroki łuk / odrzucenie z odskokiem / cios z rozbiegu / cios z góry | uderzenie głowicą |
| Miecz dwuręczny (`GREATSWORD`) | dwa szerokie cięcia → mocne wykończenie | rąbnięcie / szeroki łuk / odrzucenie / rozbieg / z góry | pchnięcie jelcem |
| Topory, kosa, sierp (`AXE`, `GREATAXE`, `KOSA`, `SIERP`) | dwa ukosy → rąbnięcie i krwawienie | z góry / obrót topora 360° lub łuk kosy 180° / odrzucenie (kosa, sierp) / rozbieg / z góry | uderzenie obuchem |
| Obuchowe (`HAMMER`, `GREATSTAFF`) | dwa ciosy → zachwianie | rąbnięcie / — / odrzucenie / taran / z góry | pchnięcie drzewcem |
| Młot dwuręczny (`GREATHAMMER`) | dwa ciosy → zachwianie | uderzenie obszarowe przy pełnym ładunku / — / odrzucenie / taran / z góry | odepchnięcie głowicą |
| Halabarda (`HALBERD`) | dwa pchnięcia → uderzenie drzewcem | wypad w linii / szeroki łuk / odepchnięcie / szarża vanilla / z góry | odepchnięcie drzewcem |
| Sztylet (`DAGGER`) | szybkie pchnięcia → krwawienie; cios w plecy zachowany | doskok i pchnięcie / krótki łuk / odskok / doskok z biegu / z góry | krótkie pchnięcie |

Miecz kłujący `THRUSTING_SWORD` ma pchnięcie również w lekkich ciosach.

Kierunek jest odczytywany **przy puszczeniu** PPM (klawisz trzymany w trakcie ładowania); sprint liczy się z chwili wciśnięcia PPM, bo ładowanie go wyłącza. Bez kierunku: znad głowy. Prawdziwy lot (fallDistance > 0,5) daje cios z powietrza. **Stan przedmiotu zależy tylko od trybu PPM** (brak/mocny/garda/szarża): każda zmiana komponentów broni w dłoni to w kliencie schowanie i wyjęcie broni (zmierzone 19.10), więc WASD i sprint (poza bronią z szarżą) jej nie zmieniają. Obszarowość tylko z reguł pluginu: łuk kosy przy każdym ciosie, łuk/linia wykończenia serii, mocny boczny; rozprysk MMOItems (`on-attack`) jest zdjęty z typów broni białej, a vanillowe cięcie zamaszyste anulowane. Dodatkowe trafienia pluginu przywracają graczowi naładowanie, a celowi nietykalność i lastDamage (inaczej następny LPM przepada). Mocny atak wybiera cel przed graczem w zasięgu broni, zadaje obrażenia przez MythicLib i respektuje `canTarget`; pchnięcie może trafić kolejne cele w linii, a łuk cele obok. Przed trafieniem ustawia zamach `whack`/`stab`. Smuga każdego lekkiego i mocnego ciosu biegnie przez cztery ticki: z prawej, z lewej lub prosto. Efekt trafienia i dźwięk zależą od rodzaju broni.

## Strona trafienia i blok (sekcje `strona`, `tarcza`, `stamina.blok_obuch`)

- **Strona trafienia:** kąt liczony od kierunku wzroku celu do napastnika (dla pocisku: do kierunku przeciwnego do jego lotu). Do `strona.przod` (60°) to przód, ×1,0; do `strona.tyl` (120°) bok, ×`mnoznik_bok` (1,1); dalej tył, ×`mnoznik_tyl` (1,25). Dotyczy graczy i mobów. Cios w plecy z `z_ukrycia.plecy` (sztylet) ZASTĘPUJE ogólny mnożnik tyłu, nie mnoży się z nim.
- **Tarcza (PPM, jak w vanilli):** dwie warstwy `blocks_attacks.damage_reductions`, które vanilla SUMUJE: przód (kąt 60, factor przód−bok) i bok (kąt 120, factor bok). Przód = `MMOITEMS_BLOCK_POWER`/100, przycięte do 0,70…0,85 (`tarcza.przod`, `tarcza.przod_max`); bok = `tarcza.bok` (0,30); tył 0. `block_delay` = `tarcza.opoznienie` s.
- **Blok pocisku z kierunku lotu:** vanilla liczy kąt bloku od pozycji strzały na hitboxie (`LivingEntity#applyItemBlocking`), więc strzała z boku trafiająca w przód hitboxu dostawała blok przedni. `blokZKata` sumuje warstwy `blocks_attacks` po kącie z kierunku lotu (ten sam co strona trafienia) i nadpisuje BLOCKING, gdy wynik różni się od vanilli (bez strzał z przebiciem i warstw z `type`); debug: `STRONA pocisk blok vanilla=… -> z kierunku lotu=…`.
- **Garda bronią (Shift + PPM):** jedna warstwa, kąt 60 (sam przód).
- **Osłabienie bloku:** stamina poniżej `strona.prog_staminy` (25%) mnoży blok przez ułamek staminy/próg; bełt z kuszy (`AbstractArrow#getWeapon()` = CROSSBOW) zabiera `strona.belt_przebija` (50%) bloku. Blok nigdy nie przekracza `limit_bloku` (90%) obrażeń, w PvP `limit_bloku_pvp` (75%).
- **Koszt staminy bloku** (`Wytrzymalosc#blok`, MONITOR): naliczany, gdy kąt trafienia mieści się w największym kącie warstw aktywnego przedmiotu; cios bronią obuchową gracza kosztuje ×`blok_obuch` (2,0). Brak staminy = blok pęka.
- **Kolejność:** jeden handler `stronaIReakcja` (EDBE HIGHEST) robi najpierw reakcję (zmęczenie, riposta — `setDamage`), na końcu stronę i osłabienie bloku. `setDamage(base)` przelicza modyfikator BLOCKING od nowa, więc osłabienie bloku musi być ostatnie.
- **Losowy blok MythicLib wyłączony:** w `plugins/MythicLib/stats.yml` `min-max-values` wpis `BLOCK_RATING: '0 = 0'` (blokiem rządzi tylko tarcza i ten plugin).
- `debug: true` dodaje linie `STRONA kat x cel blok=a->b wsp belt pvp dmg final` i `BLOK_STAMINA cios L koszt tarcza obuch kat peka`.

## Konfiguracja i pack

`generuj.py` i tabela `TYPY` są źródłem liczb oraz wariantów. Generator zapisuje `wynik/config.yml` i `wynik/pack/assets/bron/`; skopiuj konfigurację do zasobów pluginu:

```sh
python3 generuj.py && cp wynik/config.yml src/main/resources/
```

`blocks_attacks` jest ustawiane dla gardy podczas skradania. Przy zwykłym PPM plugin ustawia `consumable` na godzinę, bez dźwięku i cząstek konsumpcji, z pozą `BOW` albo `TRIDENT`; puszczenie PPM daje serwerowi dokładny czas przez `PlayerStopUsingItemEvent#getTicksHeldFor`. W trakcie używania przedmiot nie jest podmieniany, nawet po zmianie Shift lub sprintu. Vanillowe spowolnienie podczas ładowania jest zamierzone.

Modele `s100c`/`s130c` pokazują odciągniętą broń przy `using_item`. `use_duration` i `range_dispatch`, według definicji vanillowego łuku 1.21.11, przełączają cztery stopnie po 0, ⅓, ⅔ i pełnym czasie ładowania. `s130g` pokazuje dwuręczną gardę, a `s130u` pozostaje modelem młota nad głową. Model MMOItems/Nexo z własnym `item_model` pozostaje bez zmian. Materiały bez vanillowego modelu narzędzia nie dostają modelu trzymania. Stopnie trzeba obejrzeć na kliencie z packiem, bo ich wygląd jest oceną wizualną.

`/animacjebroni sprawdz <gracz>` pokazuje typ, komponenty, ładowanie, ticki do pełnego ładunku, następne ogniwo LPM, zamach, odnowienie i staminę. `debug: true` w konfiguracji włącza linie `DBG` dla lekkiego, mocnego, szturchnięcia i bloku: typ, ogniwo, wariant, ticki, obrażenia oraz stamina. Bez MMOCore dodatkowe koszty staminy są zerowe; standardowe koszty MMOItems nadal działają.

## QA i wdrożenie

`qa/mechanika.sh M3|M12|M24|BG|L3|MW|MA|MS` mierzy nowy PPM, gardę, łańcuch i kierunki na klonie z klientem BronQA. `qa/eval-strona.js a|b|c|d|e|k` (bot, `narzedzia/bot/run.sh <klon> qa/eval-strona.js <tryb>`, klon `--z-kreatorem`, `debug: true`, `gamerule spawn_monsters true`): a = mnożniki boku/tyłu i cios w plecy na husku, b = warstwy tarczy, przód/bok/tył, niska stamina, bełt, c = garda mieczem, e = strzały szkieletów w tarczę z przodu/boku/tyłu, k = jak e, ale szkielet 5 bloków pod kątem 55/65/94/115/125° od wzroku (granice warstw 60 i 120; oczekiwany blok 70/30/30/30/0 %), d = PvP (limit 75% i obuch; świat `world` ma w WorldGuard `__global__ pvp: deny`, więc na czas testu na klonie `rg flag -w world __global__ pvp allow`, potem `deny`). Testy strzał i bełtów (b, e) przestawiają klon na `difficulty hard`: na easy szkielet i pillager mają rozrzut 10 (`14 - 4*trudność`, `AbstractSkeleton`, `CrossbowAttackMob`) i pudłują z kilku bloków. Liczby czytaj z linii `STRONA`/`BLOK_STAMINA` w logu klona. Scenariusze `F/G/W/B/T/Z/N/SW1/SW2` opisują poprzedni układ sterowania i wymagają aktualizacji przed użyciem. Nie uruchamiać Paper natywnie; środowisko testowe i wdrożenie opisują skrypty `qa/klon.sh` i `wdroz.sh`.

Przy aktualizacji z 1.21.11 trzeba ponownie sprawdzić komponenty animacji przedmiotu oraz działanie modeli `using_item` w kliencie.
