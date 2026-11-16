# Agent A — runda 9

Stan końcowy: bez commitów. Worktree Zaklęć `20f2ed8` i Księgi `9542518` mają wyłącznie opisane zmiany robocze. Klon `klon-zaklecia` pozostał uruchomiony; ostatni build `a-r9-final2-build` zakończył się kodem 0 i `KLON START OK` (Zaklęcia MD5 `31d0832b9f6c`, Księga MD5 `11ff4ed2905b`). Nakładka zawiera tylko `Zaklecia/config.yml` i `CoreTools/Scripts/stale-sloty.yml`; `debug: false`, `wlasne-przywolania-zaslaniaja: true`. Żadnego pliku produkcyjnego world1 nie zmieniałem.

## 1. Zasłanianie

`Zaslanianie.pierwszyCel` sprawdza środek, górę, dół i dwa boki hitboxu. Punkt musi mieścić się w kierunku celowania (kąt względem osi nie większy niż kąt środka + 2°); dzięki temu widoczna głowa trafia po skierowaniu tam celownika, a samo odsłonięcie głowy nie obchodzi wilka przy celowaniu w środek. Każdy promień bada pierwszy blok lub żywą istotę, także własnego przywołańca. Konfiguracja w źródle i nakładce: `wlasne-przywolania-zaslaniaja: true`. `Strumien` nadal skraca model do pierwszej przeszkody na osi wzroku. W próbie z odsłoniętą głową obrażenia docierają do huska, choć model jest krótki, bo wilk wciąż stoi na głównej osi strumienia — zgodnie z osobną regułą modelu.

Bazowy `a-r9-base-z`: **FAIL** `N_zaslona-wlasny-wilk-srodek` (husk 20→8,19 HP, model 4,14 bloku). Nowy `a-r9-final2-reg-N_zaslona`: **7 PASS**. Wilk przed środkiem: husk bez obrażeń, model skrócony; celowanie w odsłoniętą głowę: obrażenia; drugi husk i blok: pełna zasłona. Pomiar w debug buildzie `a-r9-debug-z`: licznik po 20 kandydatach podał 20 promieni i średnio **65 µs/kandydata**. Kod dopuszcza maksymalnie 5 promieni na kandydata co 10 tików; w tej próbce narzut był pomijalny. Pomiar nie jest profilem górnego przypadku wielu kandydatów naraz.

## 2. Sakwa, slot 18

Do nakładki skopiowałem aktualny world1 `CoreTools/Scripts/stale-sloty.yml` i przywróciłem wywołanie `stale_sloty_wstaw_sakwa` między Dziennikiem a mapą. Slot 18 w `MMOInventory/inventory/default_mmoinventory.yml` jest wymieniony przy `inventory_button`, lecz przycisk ma `enabled: false`. Akcesoria są w rzędzie 9–17. Akcje Księgi dla numerów 18–26 dotyczą osobnego panelu Q, nie fizycznego slotu gracza. Migracja Księgi usuwa stary przycisk Sakwy tylko przy jednorazowej migracji istniejącej postaci; u nowego konta slot 18 jest wolny. Nie wykryto kolizji z innym przedmiotem.

Bazowy `a-r9-base-s`: **FAIL** dla nowego konta, slot 18 pusty. Nowy `a-r9-s2` oraz końcowy `a-r9-final2-reg-N_sakwa`: **4 PASS**. Na świeżym bocie widać Sakwę w slocie 18, klik otwiera schowek, a mapa w 7 i Dziennik w 8 oraz Sakwa po kliknięciu pozostają na miejscu. Odczyt `data get Inventory[{Slot:18b}]` jest ucinany przed identyfikatorem MMOItems, dlatego sonda sprawdza typ `minecraft:leather` w slocie 18 i faktyczne otwarcie schowka.

## 3. Broń w lewej ręce

`Nosnik.klik` przepuszcza wejście zwykłego przedmiotu do zajętej zaklęciem lewej ręki tylko do walidacji Księgi. `Bron` traktuje nośnik jako pustą lewą pozycję przy sprawdzaniu pary, a po zaakceptowaniu broni usuwa nośnik i znacznik `lewa_zaklec`, pokazując raz „Odłożono zaklęcie z lewej ręki” przez pasek MythicLib. Obsłużone są przeciągnięcie w E, F, shift-klik z plecaka, zamiana z lewą bronią zapasową i przeniesienie zapasu do lewej przy wymianie zestawu. Odrzucona operacja nie odkłada zaklęcia. Reguły broni dwuręcznej i tarczy pozostają w walidacji Księgi.

Bazowy `a-r9-base-l`: **3 FAIL** (przeciągnięcie, F, shift). Końcowy `a-r9-check-N_lewa`: **7 PASS**, w tym szybkie F tam i z powrotem i zamiana z zapasu. Sonda zapisuje wszystkie 41 pozycji ekwipunku klienta zsynchronizowanych z serwerem przed/po, liczy realne miecze oddzielnie od wizualnych klonów Księgi i odrzuca nieoczekiwane zmiany innych slotów. Dodatkowe `execute if items entity … weapon.offhand minecraft:stone_sword` potwierdza miecz po stronie serwera. Komunikat jest jeden. Próba z zaklęciem w prawej i bronią w lewej też przeszła. Nie stwierdzono duplikacji ani utraty przedmiotów.

## Regresje i porównania

| Sonda | Wynik końcowy | Uwagi |
|---|---:|---|
| A | 9 PASS | `a-r9-final2-reg-A` |
| C | 21 PASS, 1 FAIL | `a-r9-C3`: jedyny FAIL `C01-miecz-wanilia`; ten sam FAIL na bazowym `a-r9-compare-c-baseline`. Kontrola Sakwy zmieniła się z FAIL na PASS. Próba bloku po odczycie ponad jednego okresu tika zaklęcia przeszła. |
| D | 5 PASS | `a-r9-final2-reg-D` |
| F | 4 PASS, 1 SKIP | `a-r9-final2-reg-F`; SKIP F03: log nie jest montowany w bocie, runner sprawdził `BLEDY-LOGU`. |
| DB3 | 13 PASS | `a-r9-check-DB3`; sprawdzanie offhand serwerowym `execute if items`, ponieważ zapytanie NBT `Inventory[{Slot:-106b}]` zwraca pusty wynik przy widocznym i wyposażonym mieczu. |
| N_zaslona | 7 PASS | `a-r9-final2-reg-N_zaslona` |
| N_ppm1 | 3 PASS | `a-r9-final2-reg-N_ppm1`; flaga PPM pozostaje `true` |
| N_damage | 3 PASS | `a-r9-final2-reg-N_damage` |
| STAT3 | 23 PASS | `a-r9-final2-reg-STAT3` |
| PW | 3 PASS | `a-r9-final2-reg-PW`; na starcie sondy czyszczone są efekty MagCel. Wcześniejszy FAIL po N_zaslona wynikał z odporności 255 utrzymanej przez szybki reconnect; bazowy PW także 3 PASS. |

`git diff --check` w obu worktree jest czysty. Pliki zmienione: Zaklęcia `Nosnik.java`, `Zaslanianie.java`, `config.yml`; Księga `Bron.java`; nakładka `Zaklecia/config.yml`, `CoreTools/Scripts/stale-sloty.yml`. Sondy zmienione/dodane w `gracz/bot/scenariusze/`: `N_zaslona.js`, `N_sakwa.js`, `N_lewa.js`, `C.js`, `DB3.js`, `PW.js`. Po raporcie zatrzymuję pracę zgodnie z poleceniem, bez commitów i bez zatrzymywania klona.
