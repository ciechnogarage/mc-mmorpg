# Polecenie Claude dla Agenta A — runda 8 (decyzje właściciela 17.10)

## Stan
Twoje naprawy z rund 1–7 są na world1 od 13.10 (wdrożyła je sesja Kostura: gałąź `zaklecia-kostur`, HEAD `ce6c10c` = jar world1,
sprawdzone klasa w klasę). Pracujesz w NOWYM worktree:
`/home/dev/Desktop/Przemek/MC/.claude/worktrees/zaklecia-zaslanianie/MCMMORPG/plugin-src/zaklecia/` (gałąź `zaklecia-zaslanianie` od `ce6c10c`).
KsiegaPostaci NIE jest budowana (klon ma jar z world1). Nakładka `naprawy/plugins-overlay/` jest pusta (stara zarchiwizowana —
world1 ma już te pliki); dokładaj tylko nowe pliki, kopiując je z AKTUALNEGO world1 `/opt/docker/mc/data/world1/plugins/`.
Klon `klon-zaklecia` stawiany od nowa z dzisiejszego world1 (może czekać na wolny slot). Nie commitujesz, klonu nie zwijasz.
**Uwaga — równoległa praca:** inna sesja zmienia Zaklecia na gałęzi `ui-zaklecia` (Rzucanie.java, RzutyOdrzutowe.java,
Nosnik.java, Komunikat.java, Placeholdery.java, Zaklecia.java). Trzymaj zmiany WĄSKO (nowe metody/klasy, minimalne
wstawki w istniejących), żeby scalenie było proste. Nie refaktoruj.

## Zakres
1. **Zasłanianie strumienia (decyzja właściciela):** cokolwiek stoi na linii między rzucającym a celem — blok ALBO żywa istota
   (sojusznik, wróg, mob, inny gracz) — zatrzymuje strumień: obrażenia/efekt dostaje tylko PIERWSZA istota na linii, ci
   za nią nic. Dotyczy stożka koncentracji (`Rzucanie.stozek` — dziś `hasLineOfSight` widzi tylko bloki) i wizualnej
   długości strumienia (`Strumien` — do pierwszej istoty/bloku). Wyjątek wg propozycji Claude: WŁASNI przywołańcy rzucającego
   nie zasłaniają (flaga configu `zaslanianie.wlasne-przywolania-zaslaniaja: false`), całość za flagą
   `zaslanianie.wlaczone: true`. Zasłaniający sojusznik nie dostaje obrażeń (PvP deny / friendly) — tylko pochłania.
   Sprawdź spójność z pociskami (ognisty pocisk, kula) — czy już zatrzymują się na pierwszej istocie; jeśli nie — opisz, nie zmieniaj.
   Wydajność: test linii tylko dla kandydatów stożka (bez skanu świata), raytrace encji z małym marginesem hitboxu.
2. **Puszczenie PPM w trybie „obie” — dwa warianty za flagą** `ppm-puszczenie-gasi-obie`:
   `false` (obecnie): LPM włączył prawą jako przełącznik, po puszczeniu PPM prawa pali dalej;
   `true`: jeśli prawa została włączona W TRAKCIE trzymania PPM (kombo „obie”), puszczenie PPM gasi obie. Prawa włączona
   samym LPM bez PPM działa jak dotąd (przełącznik). Domyślnie zostaw `false` — wybór po teście Claude.
3. Sondy (Twoje `N_*`): `N_zaslona` — husk za sojusznikiem MagCel na jednej linii: przed zmianą husk dostaje obrażenia
   (FAIL na bazowym), po zmianie 0, a bez zasłaniającego husk dostaje (kontrola dodatnia); to samo z drugim huskiem jako
   zasłoną (pierwszy dostaje, drugi nie); własny wilk na linii nie zasłania; blok nadal zasłania. `N_ppm` — oba warianty
   flagi: stan prawej po puszczeniu PPM (placeholder + mana + encja strumienia).
4. Regresja: A, B, D, F, N_damage, STAT3, PW (osobne przebiegi). `build bazowy` = world1.
Szczegóły `naprawy/szczegoly-r8.md`, krótki raport -o. STOP po raporcie.
