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

## Konfiguracja i pack

`generuj.py` i tabela `TYPY` są źródłem liczb oraz wariantów. Generator zapisuje `wynik/config.yml` i `wynik/pack/assets/bron/`; skopiuj konfigurację do zasobów pluginu:

```sh
python3 generuj.py && cp wynik/config.yml src/main/resources/
```

`blocks_attacks` jest ustawiane dla gardy podczas skradania. Przy zwykłym PPM plugin ustawia `consumable` na godzinę, bez dźwięku i cząstek konsumpcji, z pozą `BOW` albo `TRIDENT`; puszczenie PPM daje serwerowi dokładny czas przez `PlayerStopUsingItemEvent#getTicksHeldFor`. W trakcie używania przedmiot nie jest podmieniany, nawet po zmianie Shift lub sprintu. Vanillowe spowolnienie podczas ładowania jest zamierzone.

Modele `s100c`/`s130c` pokazują odciągniętą broń przy `using_item`. `use_duration` i `range_dispatch`, według definicji vanillowego łuku 1.21.11, przełączają cztery stopnie po 0, ⅓, ⅔ i pełnym czasie ładowania. `s130g` pokazuje dwuręczną gardę, a `s130u` pozostaje modelem młota nad głową. Model MMOItems/Nexo z własnym `item_model` pozostaje bez zmian. Materiały bez vanillowego modelu narzędzia nie dostają modelu trzymania. Stopnie trzeba obejrzeć na kliencie z packiem, bo ich wygląd jest oceną wizualną.

`/animacjebroni sprawdz <gracz>` pokazuje typ, komponenty, ładowanie, ticki do pełnego ładunku, następne ogniwo LPM, zamach, odnowienie i staminę. `debug: true` w konfiguracji włącza linie `DBG` dla lekkiego, mocnego, szturchnięcia i bloku: typ, ogniwo, wariant, ticki, obrażenia oraz stamina. Bez MMOCore dodatkowe koszty staminy są zerowe; standardowe koszty MMOItems nadal działają.

## QA i wdrożenie

`qa/mechanika.sh M3|M12|M24|BG|L3|MW|MA|MS` mierzy nowy PPM, gardę, łańcuch i kierunki na klonie z klientem BronQA. Scenariusze `F/G/W/B/T/Z/N/SW1/SW2` opisują poprzedni układ sterowania i wymagają aktualizacji przed użyciem. Nie uruchamiać Paper natywnie; środowisko testowe i wdrożenie opisują skrypty `qa/klon.sh` i `wdroz.sh`.

Przy aktualizacji z 1.21.11 trzeba ponownie sprawdzić komponenty animacji przedmiotu oraz działanie modeli `using_item` w kliencie.
