# Notatka stanu 25.10 (przerwane limitem)
- Zrobione: wariant A (ciala MM + dowodca), sprzet przez Equipment MM, filtr osiagalne, `napad ai`, README, test testy/dolacz.js (przepisany, NIE uruchomiony), pomiar A/B (testy/ab-20261025.log).
- A/B: MSPT przy 4 napadach MM 1.70-1.84 sr, manekin 1.31-1.83; plugin MM 0.14 ms/tik vs manekin 0.37-0.57 ms/tik. Skok 106 ms raz (MM n=2 runda 1), niewyjasniony (podejrzenie: spawn+przebrania).
- Dalej: run.sh napady testy/dolacz.js; kadry klienta QA obu cial; strona Artifact z A/B; encyklopedia (etap 2) + skrzynka (pulapki z README); klon.sh usun napady.
- Klon `napady` STOI (do sprzatniecia po tescie). Regula: paczka /opt/docker/mc/backups/najlepsza-droga-20261025 gotowa, wdraza wlasciciel.
