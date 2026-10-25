# KANON gry — jedyne źródło ustaleń

Zasady pliku:
- Obowiązuje TYLKO dział „Ustalone” (decyzja właściciela + data). Reszta to propozycje albo pytania.
- Ustalamy od góry: poziom 1 szkielet → poziom 2 fabuła w akapicie + 10 krain → poziom 3 szczegóły aktu.
  Nie piszemy niczego na niższym poziomie, dopóki wyższy nie jest zamknięty.
- Recenzja: jedna runda na poziom, blokuje tylko to, co gracz zobaczy albo co łamie decyzję z „Ustalone”.
  Wynik dla właściciela = 2–3 warianty do wyboru, nie kolejna synteza.
- Stare dokumenty w `docs/lochy/` (akt1-v2, plan100-v2, warianty-gry) = archiwum propozycji, NIE kanon.

## Ustalone (decyzje właściciela)

### Poziom 1 — szkielet
- **Postać 1–100, 10 aktów po 10 poziomów** (17.10, „na razie”).
- **Każdy akt = lochy + 1 wyspa** (17.10; wcześniej 25.09).
- **Loch to nie rajd** — zamknięta instancja z zaprojektowanymi starciami i bossem (17.10).
- **Liczba lochów na akt nie jest święta** — „10, bo ładna liczba”, może być inna, byle treść lochów sensownie
  odpowiadała wyspie, bez cięcia treści (17.10).
- **Wyspa = otwarty świat aktu**: osady, zlecenia, wydarzenia, boss świata, zbieractwo (17.10; wcześniej 25.09).
- **Dwie niezależne ścieżki**: każda sama wystarcza do przejścia aktu, żadna nie blokuje drugiej; postęp mniej więcej
  równy (25.09, 30.09).
- **Ta sama treść, inna forma**: wspólne kraina, klimat, zagrożenie, rodziny wrogów, materiały; inne konkretne
  przygody, wrogowie, bossowie, historie. NIE lustro 1:1 (30.09).
- **Łup równej wartości** na obu ścieżkach, różne konkretne przedmioty (25.09).
- **Wyspy na osobnym serwerze za proxy**, nie obciążać world1; wymaga wspólnej bazy postępu (25.09).
- **Akt = 3–5 h gry jedną ścieżką** (17.10). Rachunek: 10 lochów × ~15 min = ~2,5 h pierwszych przejść
  + powtórki / trudności = 3–5 h → **10 lochów na akt pasuje i zostaje**.
- **Równość ścieżek mierzymy wszystkim naraz** (17.10): ten sam EXP i łup, podobny czas, tyle samo bossów/przygód,
  ta sama historia aktu — ALE z innej perspektywy: lochy i wyspa pokazują różne strony tej samej sprawy.
  Skutek kosztowy: wyspa potrzebuje ~10 przygód z bossem o wadze lochu, nie jednego bossa świata.
- **Rajdy są, ale osobno i później** — poza dwiema ścieżkami, teraz ich nie projektujemy (17.10).
- **Po 100** (17.10): trudniejsze wersje lochów (wybór trudności także WCZEŚNIEJ, przed 100), druga postać / HC,
  profesje i świat (wydarzenia wysp); docelowo endgame w stylu PoE. Szczegóły później.

### Zasady stałe
- 4 grywalne rasy (Człowiek, Elf, Ork, Nieumarły); żadna nie jest „hordą zła” i żadna nie ma przywileju w fabule
  („zabiera innym klasom, bez sensu”, 13.10).
- Akt 1 nie jest związany z wiarami (aluzja wolna).
- HC = Ślub Milczenia; śmierć odbiera status, nie postać (13.10). Szczegóły do ustalenia na poziomie 2–3.

### Królestwa i Rody — etap 0 (decyzje 25.10)
Źródło: Encyklopedia, zakładka „Królestwa i Rody” (https://claude.ai/artifact/XhnUM8PY5q6C6m5xDfXoMi), dziennik decyzji
z 25.10 i sekcje 3–6, 8–9; tabela „Czeka na Twoją decyzję” w zakładce „Spis i decyzje” (numery „nr” = wiersze paczki P14 w `/opt/docker/mc/backups/decyzje-25-10-wykonanie/KOLEJKA.md`: 7, 8, 9, 10, 12, 37, 45, 46).
Właściciel 25.10 (ok. 23:05 UTC) odpowiedział na tę tabelę: skomentował wybrane wiersze numerami, a pozostałe przyjął
słowami „reszta okej”, czyli wariantem z kolumny rekomendacji. Wszystkie decyzje poniżej przyjęto tym zdaniem.
Te decyzje są o systemie gry, nie o fabule, więc nie łamią zasady „od góry”. Nazwa własna i fabuła królestw czekają na poziom 2.

- **Królestwo NPC w stolicy nazywa się roboczo „Korona”**; nazwa własna przyjdzie po wyborze fabuły (poziom 2) i podmienia
  tylko napisy (25.10, „reszta okej”; nr 10 i 46). ⚠ Przeczy decyzji z 21.10: „nazwij to normalnei kurwa KORONA To
  korona nie czaje tej nazwy”. Obowiązuje nowsza (25.10), właściciel ma ją potwierdzić (pytanie w zakładce Królestw).
- **Królestwo daje ok. 1/3 zarobku gracza, który gra 1 h dziennie** (wariant A: rozwinięty gród = ok. 0,5 h grania dziennie,
  na czysto po pensjach). Po teście ewentualnie 1/2 (25.10, „reszta okej”; nr 7). To parametr ekonomii: dzienny dochód
  grodu = R × wartość godziny, udział = R / (R + godziny grania). Wartość godziny trzeba zmierzyć botem na klonie:
  720 $/h z `tools/handel/ekonomia.py:8` to skala waluty, nie zmierzony zarobek. Królestwo ma własny model dochodu, nie jedną
  liczbę (21.10, „raczej nie uciagnie wiec wlasny model, i ładnie sprawdzaj”).
- **Skarbiec: progi ochrony i kara sił według rekomendacji** (25.10, „reszta okej”; nr 8). Ochrona rośnie z poziomem
  Skarbca, a najgorszy najazd zabiera procent skarbca:

  | Poziom Skarbca | Ochrona | Najgorszy najazd |
  |---|---|---|
  | 1 | 50% | 10% |
  | 2 | 60% | 8% |
  | 3 | 70% | 6% |
  | 4 | 80% | 4% |
  | 5 | 85% | 3% |

  Kara sił jest w grze: napastnik silniejszy o 0–1 poziom rodu bierze ×1 łupu, o 2–3 poziomy ×0,5, o 4 i więcej ×0,25.
  Bicie słabszych się nie opłaca. Pozostałe liczby z sekcji 4 zakładki (wynik bitwy 0/0,5/1, tarcze 24 h/8 h, odbudowa 25%,
  −30% produkcji, połowa łupu przepada) to propozycja do symulatora, nie decyzja. Wchodzi w kod dopiero ze Skarbcem.
- **Gildia = związek rodów pod jednym sztandarem (wariant B)** (25.10, „reszta okej”; nr 12). Kilka królestw składa
  przysięgę jednemu przywódcy: posiłki w najazdach, danina do wspólnego skarbca, wspólne wojny, z czasem wspólny cud.
  Gildia NIE ma własnej wyspy-twierdzy. Sojusz trzymamy w tej samej bazie co królestwa, więc plugin Guilds jest zbędny.
  Jego wyłączenie to osobna decyzja przy etapie Rodu; most do MMOCore (sojusznik/wróg dla skilli i PvP) zostaje.
  Własne królestwo postać dostaje po bossie kończącym Akt 1, a wcześniej służy jako rycerz na dworze (gracza z sojuszu
  albo dostojnika NPC w stolicy). ⚠ Przeczy części decyzji z 21.10 (dziennik: gildia = sojusz królestw ze wspólną
  twierdzą; cytat „coś takiego ale lepiej i też na królestwa ale dobrze kminisz”). Sojusz zostaje, wspólna twierdza odpada.
  Obowiązuje nowsza (25.10), właściciel ma ją potwierdzić.
- **Za prawdziwe pieniądze sprzedajemy tylko kosmetykę, bez lekkiego QoL** (25.10, „reszta okej”; nr 45). Zastępuje
  decyzję z 20.10: „pieniądze tylko na kosmetyki i lekki qol bez łamania przepisów”. Zakaz P2W bez zmian: nie sprzedajemy
  przyspieszeń, tarcz, dodatkowych wysepek ani płatnych skrzynek (21.10, „boje się aby gracze mogli pozyskiwać tyle wysepek
  i trochę boje się o aspekt zarabiania na tym bez p2w”). Cytaty Mojang: `docs/research/krolestwa/mojang-usage-guidelines-cytaty-2110.md`.
- **Do startu etapu 0 potrzebne są trzy rzeczy**: ten wpis w KANON-ie, miejsce w menu Q i nazwa „Korona” (25.10,
  „reszta okej”; nr 37). Etap 0 = „Stolica-królestwo” na world1, bez drugiego serwera.
- **Menu Q: kafel „Korona” w Handlu, kafel „Ród” w Postaci, główna strona bez zmian** (wariant A; 25.10, „reszta okej”;
  nr 9). Głównym wejściem jest NPC w stolicy, menu to skrót. Kafel jest ZABLOKOWANY: robimy go dopiero po decyzji
  o układzie menu Q (decyzja otwarta). Wdrożenie łatką z kontrolą md5, nie kopią całego `hub_*.yml`.

## Propozycje agentów (nieobowiązujące, do wyboru na swoim poziomie)
- Poziom 2: warianty fabuły — „Rok Bez Pogrzebu” (D, wygrał porównanie 12.10), „Cztery drogi śmierci” (A, 13–17.10),
  „Powracający / Cienie Poległych” (10.10). Właściciel 17.10: fabułę wybieramy dopiero po zamknięciu poziomu 1.
- Poziom 3 (Akt 1): Zielona Granica, Szczep / Darnik, prawo „co pada — kiełkuje”, 10 kart lochów akt1-v2.
- Kolejność krain 2–10 i prawa świata — odziedziczone z planu 100 v2, nigdy nie wybrane.

## Poziom 1 — ZAMKNIĘTY 17.10

## Otwarte — poziom 2 (następny)
1. Fabuła całej gry w jednym akapicie: pytanie przewodnie + odpowiedź na końcu.
2. 10 krain po jednym zdaniu (czym się różnią, jakie zagrożenie) + kolejność.
3. Jak „jedna historia, dwie perspektywy” działa w każdym akcie (jedno zdanie: co widać z lochów, co z wyspy).
Forma: 2–3 warianty, każdy max 1 strona, w tym samym formacie; właściciel wybiera albo łączy.
Szczegóły (mechanika dusz, demografia, HC w fabule, sceny) są poziomem 3 — nie teraz.

## Otwarte — Królestwa i Rody
- Nazwa własna królestwa NPC: po wyborze fabuły (poziom 2). Do tego czasu „Korona”.
- Kafel „Korona” i „Ród” w menu Q: czeka na decyzję o układzie menu Q.
- Potwierdzenie dwóch sprzeczności z 21.10 (nazwa „Korona”, wspólna twierdza gildii). Obie opisane wyżej przy decyzjach.
