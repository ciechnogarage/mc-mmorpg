# Evale kreatora postaci

Bot protokolowy (mineflayer) wchodzi na **zywy world1 jak gracz** i sprawdza cala sciezke
tworzenia postaci. Wynik to linie `PASS`/`FAIL`; kod wyjscia = liczba niepowodzen.

```bash
./run.sh              # wszystkie scenariusze
./run.sh 40           # tylko te, ktorych nazwa zawiera "40"
```

## Co to sprawdza, a czego nie

**Sprawdza** (protokolem, wiec tanio i powtarzalnie): tytul i zawartosc kazdego okna,
ekwipunek slot po slocie, pozycje, tryb gry, swiat, wiadomosci od serwera. Czyli:
czy ekran wyboru postaci jest wymuszany, czy kreator sie otwiera i co w nim jest, gdzie
gracz laduje po zatwierdzeniu, czy zyje, czy dostaje z powrotem rzeczy.

**Nie sprawdza**: tekstur, przezroczystosci panelu, kadru, czytelnosci ikon — bot nie
renderuje. Na to jest klient z Xvfb (`mc-client-test`, patrz katalog `output/`).

Wyjatek: skorke postaci bot sprawdza (scenariusz 70), ale nie po WYGLADZIE, tylko po
wartosci wlasnosci `textures` w pakiecie listy graczy — czyli po tym, co serwer naprawde
wysyla klientowi. Czy ta tekstura dobrze wyglada, rozstrzyga klient z Xvfb.

## Jak bot w ogole wchodzi

Dwie rzeczy sa konieczne i obie sa w `lib/`:

- **`velocity.js`** — world1 ma `velocity.enabled: true` i odrzuca polaczenia bez podpisanych
  danych proxy. Bot podpisuje je sam (sekret z `world1/config/paper-global.yml`) i laczy sie
  wprost z `world1:25565`, z pominieciem proxy. *Pulapka:* mineflayer sam odpowiada na
  `login_plugin_request` pusta odpowiedzia i robi to pierwszy — trzeba mu odebrac ten event.
- **`resourcepack.js`** — serwer wysyla `add_resource_pack` i czeka na status klienta. Bez
  odpowiedzi polaczenie zostaje w fazie `configuration` na zawsze.

`EVAL_DECLINE_PACK=1` kaze botowi odrzucic pack — tak sprawdza sie zachowanie gracza bez
resource packa.

## Scenariusze

| plik | co pilnuje |
|---|---|
| `10-wymuszenie-wyboru` | nikt nie gra bez postaci; zamkniety ekran wyboru wraca |
| `20-pelny-cykl` | kreator -> wybor -> zatwierdzenie -> bezpieczne wyjscie, gracz zyje |
| `30-straznik-vs-kreator` | regresja z gry: ekran wyboru nie podmienia kreatora |
| `40-ekwipunek` | plecak schowany w kreatorze i oddany w calosci, razem z kitem |
| `50-dwoch-graczy` | dwie sesje naraz, zadnej odmowy |
| `60-rozlaczenie` | wyjscie w trakcie kreatora nie gubi rzeczy ani nie wiezi w sali |
| `70-skorka` | skorka pary rasa x klasa laduje na graczu JUZ PRZY WEJSCIU (przed ekranem wyboru), przezywa relog i idzie za postacia |
| `80-wyjscie-z-kreatora` | przycisk WYJDZ oddaje rzeczy i wraca na ekran wyboru, a porzucony `ProfileCreateEvent` nie blokuje kolejnego wejscia |
| `90-stajnia-podgladu` | po wejsciu stoi komplet nosicieli, a zmiana rasy/klasy nie tworzy ani nie kasuje encji |
| `95-glowy-na-liscie` | glowa postaci na ekranie wyboru nosi skorke tej postaci i nie znika po przerysowaniu okna |

`tools-skin-packets.js` i `tools-skin-timeline.js` tez nie sa scenariuszami — to sondy do
skarg "nie mam skorki postaci". Drukuja os czasu `player_remove`/`player_info`/`respawn` dla
wlasnego wpisu gracza i tlumacza kazda teksture na id z `skins.yml` albo na obcy hash, wiec
widac, KTO naklada skorke ostatni. Env: `EVAL_USER`, `EVAL_DELAY_MS` (pauza na ekranie wyboru
— bez niej nie zobaczysz nadpisan, ktore przychodza pozniej), `EVAL_WATCH_MS`,
`EVAL_VIA_PROXY=1` razem z `MC_HOST=proxy MC_PORT=25577` (wejscie jak u czlowieka).

`tools-auth-probe.js` sprawdza LOGOWANIE na proxy (LibreLogin), nie kreator: rejestracja,
żądanie hasła po relogu, złe hasło, dobre hasło, próba wejścia na cudzy nick i przejście
na world1 z ekranem wyboru postaci. Uruchamiać z `MC_HOST=proxy MC_PORT=25577` — w
odróżnieniu od scenariuszy, które łączą się WPROST z world1 i logowania w ogóle nie widzą.

`tools-discover.js` nie jest scenariuszem — to rekonesans drukujacy realny uklad okien
(uzyc, gdy zmieni sie GUI: `node tools-discover.js` w kontenerze).

## Uwaga

Evale **tworza postacie na produkcji** (konta `Ev*`). Po serii warto je sprzatnac:
`/cc prune` pokazuje rekordy bez profilu, a profile bez rekordu trzeba usunac z GUI profili.
