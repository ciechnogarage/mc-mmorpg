# Polecenie Claude dla Agenta A — runda 3

## Ocena rundy 2 (Claude + recenzja B)
- **N2 przyczyna — świetnie:** `KsiegaPostaci.Bron.damage` (HIGHEST) anuluje KAŻDE EntityDamageByEntityEvent od gracza,
  gdy wybrany slot ≠ 0/1 (i przy `virtual(mainhand)`), także obrażenia zaklęć z `mmodamage`.
- **N2 Twoje obejście `damage{dc=MAGIC}` — ODRZUCONE.** B udowodnił A/B (`krytyk/skalowanie-r2.md`, b-2-09 vs b-2-10):
  baza Wiara 40 = 1,60× obrażeń, Twoja wersja 1,00× (MythicMobs `damage` pomija AttackMetadata/StatProvider MythicLib).
  Do tego obejmowało 2 z 15 zaklęć. USUŃ z nakładki `MythicMobs/skills/zaklecia/{plomienie,odmrozenie}.yml`.
- N5 (MythicLib `support.party_other: true`) — zostaje w nakładce, ale to DECYZJA WŁAŚCICIELA (nie wdrażamy bez niej).
- N6 `[refresh]` i N8 spolszczenie — PRZYJĘTE.
- N4 — REMOVE przechwytuje MythicHUD (pusty ADD); to nie nasz błąd. Twoje zamknięcia paska przy śmierci/wyjściu/zmianie
  świata są właściwe — wdrażasz je do kodu.
- „Podwójne leczenie” — B obalił (harness wysyłał 2×LPM zamiast PPM+LPM). Nie naprawiasz.
- Moja wina: w rundzie 2 źródła były tylko do odczytu. Teraz masz zapis do OBU worktree.
- `debug` w nakładce przełączyłem na `false` (pomiary wydajności).

## Gdzie teraz piszesz
- Zaklecia: `/home/dev/Desktop/Przemek/MC/.claude/worktrees/zaklecia-naprawy/MCMMORPG/plugin-src/zaklecia/` (HEAD 24cc940).
- **KsiegaPostaci (nowe):** `/home/dev/Desktop/Przemek/MC/.claude/worktrees/ksiega-naprawy-zaklecia/MCMMORPG/plugin-src/ksiega-postaci/`
  (gałąź `ksiega-naprawy-zaklecia` od 1e1c108 = dokładnie jar na world1). Gałęzi `eq-klon-niewidoczny` NIE ruszasz —
  pracuje na niej inna sesja.
- `build` w kolejce buduje teraz OBA pluginy i wgrywa oba na klon; `build bazowy` przywraca oba jary z world1 ORAZ
  oryginały wszystkich plików nakładki (Twoja uwaga z rundy 2 — poprawione). Kolejka odmawia pracy przy <3 GB wolnego
  na `/` (kod 8) — wtedy STOP i raport.
- NIE commitujesz. Nie wrzucasz do worktree katalogów `.gradle-home`/`build` (kopie 240 MB zapchały dysk).
- Szczegóły do `naprawy/szczegoly-r3.md`, krótki raport -o `naprawy/raport-r3.md`.

## Runda 3 — zakres
1. **N2 poprawka u źródła (KsiegaPostaci `Bron.damage`):** blokuj WYŁĄCZNIE prawdziwy cios wręcz bronią spoza slotu
   0/1 (to zasada broni w ksiega-postaci — jej NIE zmieniasz). Obrażenia umiejętności/zaklęć (MythicLib `mmodamage`,
   skille MMOCore/MythicMobs, pociski) mają przechodzić. Ustal pewny sposób rozpoznania z API MythicLib (np. zarejestrowany
   atak / `AttackMetadata` / typ obrażeń SKILL — sprawdź w jarze i dokumentacji MythicLib, nie zgaduj) albo z przyczyny
   i kontekstu zdarzenia. Sonda FAIL (bazowy) → PASS: KAŻDY typ zaklęcia zadający obrażenia (stożek, pocisk, kula, runa,
   przywołańcy, broń przywołana) z mieczem w slocie 5 i zaklęciem w lewej; kontrola: zwykły cios mieczem ze slotu 5
   nadal 0 (zasada broni), ze slotu 0 działa; skalowanie Wiara 1,60× zachowane (sonda B `STAT2` — uruchom ją).
2. **Zastosuj w worktree Zaklec** swoje zmiany z `naprawy/source-r2/` poza diagnostyką na stałe: N4 (zamknięcie paska
   przy śmierci/wyjściu/zmianie świata), logi N5 tylko pod `debug`. `DiagnostykaObrazen` — tylko pod `debug: true`,
   zero kosztu przy false (sprawdź, że nie rejestruje listenerów).
3. **N3** (zwykły przedmiot wraca do nośnika): B i Claude podejrzewają też nowy plugin **DwieBronie** (na world1 od 10.10,
   jest na klonie; źródło w repo MC — znajdź gałąź `dwie-bronie`), który rusza lewą ręką. Odtwórz `test C` 3× pod rząd,
   zbierz `ZAKDBG slot/pusta` (włącz debug tylko na czas tej sondy przez nakładkę, potem z powrotem false) i ustal, kto
   zmienia slot/przedmiot. Poprawka tylko jeśli przyczyna pewna; inaczej raport z dowodami.
4. **N9** (z review): błędne id usuwa zaklęcie przed walidacją (`Nosnik.java:171–186`); opóźniony LPM paska przypisuje
   pozycję z nowej sesji (`Wybor.java:180–197`); `RzutyOdrzutowe.java:185–195` mana pobrana bez efektu; zmiana świata
   dla runy/rzucania. Każde: sonda FAIL→PASS albo uzasadnienie „nie do odtworzenia”.
5. **N7** strumień w 1. osobie zasłania pół ekranu: przygotuj najprostszy wariant (osobny, węższy/krótszy obiekt tylko
   dla rzucającego, pełny dla innych) za flagą configu, domyślnie WŁĄCZONY. Ocenę zrobi Claude prawdziwym klientem.
Po zmianach: `build`, `test ABCDEF`, wszystkie Twoje `N_*`, plus `STAT2` B. Tabela A/B względem `build bazowy` (te same
testy). Ostatni build = Twoja wersja z `debug: false`. STOP po raporcie.
