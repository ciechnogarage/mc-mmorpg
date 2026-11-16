# Polecenie Claude dla Agenta A — runda 10b (rozszerzenie zakresu)

Dobra decyzja, że zatrzymałeś się na granicy zakresu. Rozszerzam: wolno Ci zmienić w NAKŁADCE (`naprawy/plugins-overlay/`, pliki
skopiowane z AKTUALNEGO `/opt/docker/mc/data/world1/plugins/`) skille MythicMobs zaklęć stożkowych.
Plugin sam rozstrzyga teraz widoczność (bloki + istoty, siatka + celownik), więc `TargetConditions: - lineofsight true` w skillach
trafienia, którym cele przekazuje plugin (`Zak_Plomienie_Traf*`, `Zak_Odmrozenie_Traf*` — `plomienie.yml:98,107`,
`odmrozenie.yml:97,109`), jest zbędnym drugim filtrem do środka celu. Usuń TYLKO te warunki `lineofsight` (inne warunki, np.
`mmoCanTarget`, zostają). Sprawdź `grep` w `MythicMobs/skills/zaklecia/`, czy podobny warunek nie blokuje innych zaklęć, którym
cel podaje plugin; jeśli tak — opisz, nie zmieniaj bez potrzeby.
Wzmocnij `B6Edge` (albo własną `N_krawedz`): asercje progu — przy x=0,95 i 0,975 (widoczne ~25–40% szerokości) obrażenia > 0
przy celowaniu w widoczną część; pełna zasłona (x=0,5 i mniej) 0. Regresja: N_zaslona_r10, B6Occlusion, N_ppm1, A, F, STAT3, PW.
Szczegóły dopisz do `naprawy/szczegoly-r10.md` (sekcja „10b”). STOP po raporcie.
