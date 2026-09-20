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

`tools-discover.js` nie jest scenariuszem — to rekonesans drukujacy realny uklad okien
(uzyc, gdy zmieni sie GUI: `node tools-discover.js` w kontenerze).

## Uwaga

Evale **tworza postacie na produkcji** (konta `Ev*`). Po serii warto je sprzatnac:
`/cc prune` pokazuje rekordy bez profilu, a profile bez rekordu trzeba usunac z GUI profili.
