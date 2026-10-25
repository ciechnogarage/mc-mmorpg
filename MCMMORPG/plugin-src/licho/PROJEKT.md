# „Licho nie śpi” — prop hunt w klimacie serwera

Minigra z kafla Q → Drużyna (jak Wyścigi Chowańców). Osobny świat `licho` (VoidGen), mapa „Zagroda Zielarki”
budowana komendą `licho zbuduj` z pliku `mapa.txt` (generator `mapa/generuj.py`). Bez łupu, bez EXP, bez P2W.

## Fantazja
Licho to złośliwy duszek, który chowa się w sprzętach („licho nie śpi”). Zielarka wezwała Zamawiaczy —
wiejskich egzorcystów, którzy pękiem ziół wyganiają licho z beczek i snopków. Licho wygrywa, gdy przetrwa
do świtu (koniec rundy); Zamawiacze, gdy „zamówią” (złapią) wszystkie.

## Role
- **Licho** (chowający). Niewidzialne dla innych (ukryty gracz), widać tylko sprzęt, w który się wcieliło.
  - **Wcielenie**: PPM na sprzęt z mapy (do 5 bl.) = przybiera jego kształt i ustawienie (np. beczka na boku).
    Tylko sprzęty z listy (`przebrania` w config.yml). W rozproszeniu bez ograniczeń, w łowach co 20 s.
  - **Zastygnięcie**: stoisz 1,5 s bez ruchu → sprzęt wskakuje w siatkę bloków i dla innych staje się
    prawdziwym blokiem (pozorny blok `sendBlockChange`: zderzenia, cień, nic go nie odróżnia, także F3+B).
    Ruch = odmarzasz (sprzęt znów się przesuwa, a to widać).
  - **3 iskry** (życie w grze, nie HP postaci). Trafienie = −1 iskra, sprzęt podskakuje i piszczy.
  - **Psota** (F, co 15 s): Licho chichocze, sprzęt się trzęsie → +5 pkt. Ryzyko za punkty.
  - **Szept** (sam, co 40 s): każde Licho cicho szepcze (słychać do ~12 bl.) — przeciw kampieniu do końca.
  - Złapane Licho staje się Zamawiaczem (dalej gra, pomaga łowić).
- **Zamawiacz** (szukający). Na start 1 na każde 4 graczy (min. 1).
  - **Rozproszenie** (30 s): zamknięty w Komorze, oślepiony. Licha się chowają.
  - **Trafienie**: LPM w sprzęt. Trafione Licho traci iskrę. **Trafienie w prawdziwy sprzęt = −1 tchu**
    (6 tchu, +1 co 10 s). Przy 0 tchu **zadyszka** 4 s (spowolnienie, nie bije). Tak klikanie we wszystko
    się nie opłaca — trzeba czytać scenę (czego tu nie było, co stoi krzywo, co szepcze).
  - Ciosy co 0,5 s (spam nie pomaga).
  - **Okadzanie** (F, co 25 s): dym ziołowy w promieniu 5 bl. — każde Licho w dymie po chwili kicha
    (cząsteczki + dźwięk w jego miejscu). Nie zdradza dokładnie, tylko „gdzieś tu”.

## Przebieg rundy
1. Poczekalnia: start, gdy jest ≥2 graczy; 25 s (każdy nowy +5 s, maks. 45 s). Ogłoszenie na czacie jak w Wyścigach.
2. Rozproszenie 30 s → Łowy 180 s → podsumowanie 8 s → powrót tam, skąd gracz przyszedł.
3. Limit 12 graczy na rundę; jedna runda naraz (mapa jedna), kolejni czekają na następną.

## Punkty i ranking (bez nagród przedmiotowych)
Licho: +1 co 10 s w ukryciu, +5 za psotę, +15 za przetrwanie. Zamawiacz: +3 za trafienie, +10 za złapanie.
Ranking „najwięcej punktów w rundzie” i „najczęściej przetrwane” (rekordy.yml). EXP postaci celowo NIE
(dwa konta grające razem nabiłyby EXP).

## Tytuły (decyzja właściciela 24.10: wariant B, „to dawaj”)
Kosmetyka bez mocy. Liczą się tylko rundy dokończone (nie przerwane) przy ≥ `tytuly.min-graczy` (4) graczach na koniec.
- Licho (przetrwane rundy): 1 Kryjak · 5 Domowik · 20 Licho Nieuchwytne · 50 Bies Zagrody.
- Zamawiacz (złapane Licha): 3 Tropiciel · 10 Znachor · 40 Szeptun · 100 Mistrz Zamawiania.
Progi z celu: pierwszy tytuł w pierwszy wieczór, ostatni ~15 h (założenie: Licho przetrwa ~1/3 rund, ~2 złapania na rundę
przy 4–5 graczach — NIE zmierzone, do sprawdzenia w rekordy.yml po tygodniu). Wyświetlany jest najwyższy stopień
(remis: ścieżka Licha). Widać go w podsumowaniu rundy, rekordach i w menu (przycisk Rekordy, `%licho_tytul%`,
`%licho_postep_licho%`, `%licho_postep_zamawiacz%`, `%licho_min_graczy%`). Przy nicku w czacie NIE: CMI ma `ModifyChatFormat: false`,
format czatu to zmiana wspólna dla serwera (osobna decyzja).

## Sterowanie (pakiety, przed Księgą/Zaklęciami/MenuKey)
LPM = trafienie (Zamawiacz) · PPM = wcielenie (Licho) · F = psota / okadzanie · Q = menu (Opuść grę).
Ekwipunek nietknięty (MMOProfiles zapisuje EQ postaci — nie ruszamy go).

## Nadużycia i odporność
- Ucieczka z gry, relog, zmiana postaci = odpada (jak złapane); powrót tam, skąd przyszedł.
- Licho poza mapą / w ścianie / na dachu: co sekundę sprawdzenie obszaru gry, poza nim = cofnięcie.
- F3+B: na każdym prawdziwym sprzęcie stoi taka sama niewidzialna „zaczepa” (Interaction 1,02 bl.),
  więc hitboxy nie zdradzają Licha. Zaczepy istnieją tylko w trakcie rundy.
- Brak nagród = brak farmienia altami.
- Obrażenia, głód, komendy (poza czatem prywatnym/drużyną) wyłączone w świecie gry.

## Koszt serwera
- Mapa ~3×3 chunki, forceload tylko na czas rundy (jak tor Wyścigów).
- Encje: 2 na Licho (BlockDisplay + Interaction) + ~zaczepy (jedna na sprzęt, bez AI, bez ticka).
  Teleport wyświetlacza tylko przy ruchu. Pętla rundy: 12 graczy × kilka porównań/tick.
- Jedna runda naraz → koszt nie rośnie z liczbą graczy online (100+ = kolejka).
