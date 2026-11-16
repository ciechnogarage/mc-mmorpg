# Testy Otwartej Areny na klonie

Scenariusze uruchamia operator: `/opt/docker/mc/narzedzia/bot/run.sh <nazwa-klonu> testy/a-ucieczka.js` itd. Każdy `w.sprawdz` drukuje PASS/FAIL. Pliki z literą A–F, I–K są botowe; G i H wymagają klienta. Bot łączy się przez Velocity. Przed testem ustaw świat, postaw budowlę i platformę, zainstaluj plugin oraz kopie konfiguracji. Testy wymagają kont botów gotowych do gry (bez kreatora).

| Grupa | Automat | Dodatkowa kontrola klientem / operatorem |
| --- | --- | --- |
| A | `a-ucieczka.js`: tag, `arena wyjdz`, vanilla teleport, wyjście po 10 s | Portal, szybka podróż i `md play` z menu; odmowa w trakcie walki |
| B | `b-farma.js`: EXP klasy po PvP i manekinie | EXP profesji, broni i Chowańców, łupy, osiągnięcia i zadania po zabiciu alta |
| C | `c-smierc.js`: ekwipunek, EXP, efekt Pecha i respawn | Śmierć poza areną i szybki powrót; kara musi pozostać poza areną |
| D | `d-granica.js`: obie strony granicy, ring | Pociski i zaklęcia obszarowe przez granicę; Stolica bez zmian |
| E | `e-wejscie.js`: kreator i spadanie | Loch MythicDungeons, śmierć, walka poza areną, przełączenie profilu przy powrocie |
| F | `f-manekin.js`: obecność, pasek, brak przesunięcia i śmierci | Szarża, odrzut, smycz, lawa, DOT, komenda gracza, dwa różne profile, 100+ liczb |
| G | — | `G-CHOWANCE-MANUAL.md` |
| H | — | `H-ZAKLECIA-MANUAL.md` |
| I | `i-przedmioty.js`: wyrzucenie przedmiotu | Stawianie/kopanie, podnoszenie, mikstury, perły, łódki, mur, elytra, pustka |
| J | `j-relog.js`: relog po walce, platforma, zakaz ringu 60 s | Crash klienta i zmiana postaci MMOProfiles |
| K | `k-wielu.js`: dwa liczniki, brak rozłączeń | Restart klonu podczas walki, zapis powrotów, sprzątanie TextDisplay |

Po każdym FAIL sprawdź log klonu i nie wdrażaj na produkcję. Potwierdź zrzutem klienta: Q → Społeczność, kafel na slotach 41,42,50,51, na arenie jego wariant „Wyjdź z areny”.

## Kontrole, których bot nie rozstrzyga w pełni

- **A:** W trakcie taga kliknij Portal z Q, szybki cel z mapy i Loch 001 z Podróży; PASS = brak uruchomienia podróży/instancji i pozostanie w arenie, FAIL = zmiana świata, instancja lub utrata punktu powrotu. Menu i opóźnione teleporty potrzebują klików klienta.
- **B:** Zapisz EXP klasy, każdej profesji, broni i Chowańca oraz liczniki zadań/osiągnięć; powtarzaj zabicie alta i ataki manekina; PASS = wszystkie stany bez wzrostu i pusty łup, FAIL = dowolna nagroda. Bot nie odczytuje wszystkich niestandardowych baz postaci.
- **C:** Zgiń w świecie `world`, potwierdź stratę EXP/debuff, wejdź na arenę po odrodzeniu i wyjdź; PASS = kara nadal działa poza areną, FAIL = arena ją usuwa. Własne statystyki CoreTools wymagają klienta i odczytu profilu.
- **D:** Strzel pociskiem z obu stron granicy i użyj AoE na jej krawędzi; PASS = obrażenia tylko gdy obaj stoją w ringu, FAIL = trafienie widza albo brak trafienia w ringu. Klient wymaga właściwego aim i skilli.
- **E:** Spróbuj wejść z lochu `loch_001` i w trakcie śmierci, potem zmień profil MMOProfiles na arenie i kliknij wyjście; PASS = odmowa albo powrót do Stolicy, nigdy do instancji/punktu obcego profilu, FAIL = powrót do lochu. Klon z kreatorem i klientem jest konieczny.
- **F:** Uderz manekina szarżą, zaklęciem, DOT, lawą, smyczą i atakiem przywołańca; PASS = po 5 s stoi na stacji, żyje, nie płonie stale i nie daje nagród; FAIL = ruch, zgon, drop, brak licznika albo narastające TextDisplay. Bot nie odtwarza szarży i gestów obu rąk.
- **G:** Stosuj kroki `G-CHOWANCE-MANUAL.md`; PASS/FAIL są tam rozpisane.
- **H:** Stosuj kroki `H-ZAKLECIA-MANUAL.md`; PASS/FAIL są tam rozpisane.
- **I:** W kliencie: postaw/kop blok, podnieś przedmiot, rzuć miksturę/perłę, postaw łódź, wejdź na mur i poleć elytrą poza promień; PASS = zakaz interakcji i ratunek na platformie, FAIL = przedmiot/blok zostaje albo ucieczka. Bot nie wysyła niezawodnego ruchu `player_input`.
- **J:** Rozłącz klienta w walce i przełącz profil przez GUI; PASS = platforma, 60 s zakazu ringu, powrót do Stolicy dla innego profilu; FAIL = powrót do miejsca walki lub punktu obcej postaci. Botowy relog nie obsługuje wyboru MMOProfiles.
- **K:** Po wejściu dwóch graczy i zapisaniu różnych powrotów zrestartuj tylko klon, nie produkcję; PASS = oba powroty poprawne, combat tag wyczyszczony, żadnych starych TextDisplay; FAIL = stan zamieniony, utracony lub liczby zostają. Botowy `run.sh` nie zarządza restartem klonu.
