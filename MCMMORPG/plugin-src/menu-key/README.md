# MenuKey

Jeden klawisz — **Q** — otwiera menu-hub serwera. Zawsze, takze gdy gracz ma puste rece.

## Po co osobny plugin

Menu to konfiguracja (DeluxeMenus), a klawisz przechwytywal CoreTools jednym wpisem
w `EventsManager/`. Tamto rozwiazanie ma dziure, ktorej **nie da sie zalatac konfiguracja**:
`PlayerDropItemEvent` jest zdarzeniem o UPUSZCZONYM przedmiocie, wiec przy pustej rece nie
ma co upuscic i zdarzenie nie leci. Sprawdzone botem: pakiet dochodzi do serwera, a zadne
zdarzenie Bukkita sie nie odpala — Bukkit nie ma zdarzenia "gracz wcisnal Q".

Ten plugin slucha wiec samego pakietu (ProtocolLib, `BLOCK_DIG`), reaguje **wylacznie** na
`DROP_ITEM` i `DROP_ALL_ITEMS` (kopanie blokow zostaje nietkniete), anuluje go — zeby Q nie
robilo dwoch rzeczy naraz — i wola komende z `config.yml`.

**Machniecie po Q (16.11, zgloszenie "po kliknieciu q robi atak").** Klient po wyrzuceniu
przedmiotu sam macha reka (pakiet `ARM_ANIMATION` zaraz po `DROP_ITEM`), a AnimacjeBroni bralo
to za lekki cios (smuga, koszt staminy, obrazenia w celu przed graczem). Plugin zapamietuje
chwile przechwyconego Q i odrzuca JEDNO machniecie, ktore przyjdzie do 150 ms pozniej
(`SWING_WINDOW_NANOS`). Klon zwod-bledy, klient QA: Q bez okna przed celem 0/3 ciosow, menu
otwiera sie; LPM przed i po trafia (7,6). Wpis czyszczony przy wyjsciu gracza.

To wszystko. Plugin nie rysuje zadnego menu i nie zna ani jednej jego pozycji.

## Build i wdrozenie

```
./build.sh                  # gradle w kontenerze (na hoscie nie ma JDK)
cp build/libs/menu-key-*.jar /opt/docker/mc/data/world1/plugins/
# restart serwera — nowy plugin nie wchodzi przez reload
```

## Konfiguracja

`config.yml`: `command` (domyslnie `dm open hub_glowne %player%`), `run-as`
(`console`/`player`), `permission`, `blocked-worlds`.

## Jak to sprawdzic

`/home/dev/Desktop/Przemek/MC/tools/menu-hub/run.sh` — eval botowy. Sprawdza m.in. to, po co
ten plugin powstal: **Q z pusta reka otwiera menu**.
