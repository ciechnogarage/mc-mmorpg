# Komunikaty etap 1 — WDROZONE na world1 03.10 00:31 UTC (jary *-202610030029)

Galezie repo MC (bez push): kom-zaklecia 8db80a3 (od zaklecia-skyrim 0021c3a), kom-mapa 76f12a2 (od mapa e3131cb),
kom-ab 114bb46 (od animacje-broni 9381368). Jary w tym katalogu.

- Zaklecia: zatrzask PPM po braku many do puszczenia; "Brakuje many" na pasku (priorytet MythicLib 40, dedup 2 s);
  ladowane: ostrzezenie przy starcie; powody odmowy (Wskaż cel / Brak miejsca na runę / Wskaż sojusznika / przywolaniec);
  leczenie innych bez celu nie pobiera many; Skille nie cache'uje braku na zawsze.
  RAZEM z jarem wdrozyc etap1/_wspolne.yml -> plugins/MythicMobs/skills/zaklecia/_wspolne.yml (i do tools/zaklecia/v2/src).
- Mapa: teksty przez pasek MMOCore; prowadzenie priorytet 20, odswiezane co 30 t lub przy zmianie (bez migania).
  NIE testowana na kliencie (klon bez MapEngine/NMinimap) — tylko build.
- AnimacjeBroni: "Brakuje staminy" priorytet 40 + dedup 2 s, kombo 30.

Priorytety paska (MythicLib): MMOCore HP/mana 10 < nawigacja 20 < informacja 30 < odmowa 40 < Nosnik 50.
Test na klonie + klient QA (zrzuty/): 0 many PPM = 1 komunikat bez migania; ponowna proba po puszczeniu = nowy;
pelna mana = normalny strumien; mana konczy sie w trakcie = komunikat i stop do puszczenia PPM.
Przed wdrozeniem: sprawdzic, czy inne sesje nie maja nowszych zmian na zaklecia-skyrim / mapa(-ppm) / animacje-broni
(wtedy zrebase'owac kom-*), restart world1 przy 0 graczy.

## Wdrozenie 03.10 00:31 UTC
Przed wdrozeniem kom-* zrebase'owane na najnowsze czubki: kom-zaklecia 4cbe072 (na zaklecia-skyrim 4c138d5),
kom-mapa 39cece2 (na mapa-ppm 1dbb7cf), kom-ab 4a83762 (na ab-mapa cab519d). 0 graczy, log czysty.
Cofniecie: backups/komunikaty-etap1-20261003-003018/cofnij.sh.
UWAGA: kolejne wdrozenie z zaklecia-skyrim / mapa-ppm / ab-mapa BEZ zmergowania kom-* cofnie te zmiany.
03.10: zaklecia-skyrim przewinieta do kom-zaklecia 4cbe072 (sesja Zaklec) - Zaklecia OK; mapa-ppm/ab-mapa jeszcze bez merge kom-*.
