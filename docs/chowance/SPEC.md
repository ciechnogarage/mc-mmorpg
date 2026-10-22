# Chowańce — pełna specyfikacja (zatwierdzona przez właściciela 2026-09-22: wdrożyć CAŁOŚĆ)

Źródło: propozycja z sesji 2026-09-22. Każdy punkt poniżej jest wymaganiem. Nic nie jest „opcjonalne”.
Decyzje, których właściciel nie podjął, zostały ustalone domyślnie (sekcja 0) — jeśli research/dokumentacja
wykaże, że któraś jest zła, agent opisuje to w DECYZJE.md z uzasadnieniem i źródłem, zamiast po cichu zmieniać.

## 0. Decyzje domyślne
- Walka: WSZYSTKIE trzy tryby (towarzysz PvE real-time, pojedynki areny real-time sterowane, walka turowa).
- Łapanie: OBA sposoby (osłabienie + rzut Pieczęcią; oswajanie karmieniem PROF_*), gatunek w konfiguracji decyduje, które działa.
- Technologia: hybryda (C). Ciało/AI = MythicMobs + ModelEngine. Własny plugin „Chowance” = drużyna, łapanie, zapis per profil
  MMOProfiles, Bestiariusz, tury, pojedynki, hodowla, handel, API/placeholdery. MCPets: spike 1–2 dni wg listy kontrolnej
  (zapis per profil vs per konto, wylogowanie/zmiana postaci, wydajność, licencja/cena, zgodność z Paper 1.21.11) — wynik i
  decyzja „używamy jako ciało / nie używamy” w DECYZJE.md.
- Treść startowa (minimum): 3 linie startowe × 3 stadia, 6 linii dzikich × 2 stadia, 2 legendarne, warianty lśniące wszystkich.
- Grafika: WŁASNE modele BlockBench w naszym pixel-arcie (zob. memory pixelart-metoda-i-warsztat, tools/blockbench, tools/pixelart),
  ikony Nexo. LibsDisguises/vanilla tylko jako tymczasowy prototyp, nie w stanie końcowym.
- Nazewnictwo po polsku.

## 1. Pętla
Spotkanie → osłabienie → schwytanie → Menażeria → trening/ewolucja → walki → kolekcja.

## 2. Łapanie
- Pieczęcie Więzi = MMOItems, rzucane jak pocisk (skill MythicLib na trafieniu).
- Szansa = f(HP% celu, status celu [sen/spętanie/oszołomienie], klasa pieczęci [Kamienna/Kryształowa/Pradawna],
  rzadkość gatunku, różnica poziomów, poziom profesji Poskramiacz). Wzór jawny, w configu, przetestowany liczbowo.
- Animacja: pieczęć drży 1–3 razy (stan ModelEngine + dźwięk + cząsteczki); porażka = stwór się wyrywa i wpada w szał.
- Pieczęcie specjalne: nocna, wodna, „kradzieży” (na stwory, które uciekły innym), dungeonowa (tylko drop z lochów).
- Oswajanie: karmienie przedmiotami PROF_* → pasek postępu oswojenia.

## 3. Gatunki, typy, osobniki
- Typy: Ogień, Woda, Ziemia, Powietrze, Natura, Mrok, Światło, Metal, Bestia, Nieumarły + tabela skuteczności (mnożnik obrażeń).
- Linie ewolucyjne 2–3 stadia; ewolucja = poziom + Kamień Przemiany (MMOItems) (+ więź dla części gatunków); zmiana modelu.
- Osobnik: geny (losowy rozrzut statystyk), charakter (+/−), 1 cecha pasywna, 4 aktywne ruchy z puli gatunku.
- Lśniące warianty (~1/1000): inna tekstura, ogłoszenie przy pojawieniu.
- Starter: wybór 1 z 3 na początku (powiązanie z kreatorem postaci — rasa/klasa może wpływać na ofertę).

## 4. Występowanie
- Spawny MythicMobs zależne od biomu Iris, pory dnia, pogody; rzadkie tylko w regionach WorldGuard.
- Legendarne = bossy świata na harmonogramie; łapalne tylko po pokonaniu, jedną specjalną pieczęcią.
- Lochy MythicDungeons z gatunkami endemicznymi („Gniazdo Żmijów”, „Krypta Upiorów”).
- Wydarzenia sezonowe z gatunkami czasowymi.

## 5. Menażeria i drużyna
- Drużyna 6, na zewnątrz 1 aktywny. Magazyn (PC) bez limitu lub z limitem rozszerzanym progresją.
- GUI DeluxeMenus pod Q (kafelek „Chowańce” w hub_glowne): ikona Nexo, statystyki, poziom, ruchy, przywołaj/odwołaj.
- Zapis PER PROFIL MMOProfiles (nowa postać = zero). Odporność na profil-ducha.
- MythicHUD: HP i poziom aktywnego stwora.

## 6. Walki
1. Towarzysz PvE real-time (GoToOwner/OwnerTarget/OwnerAttacker MythicMobs).
2. Pojedynki areny real-time sterowane: komendy 1–4 z hotbara/menu.
3. Walka turowa: obie strony stoją, menu tur w dialogu Papera (jak okno logowania), przyciski ruchów.
- Mistrzowie Więzi (gym leaderzy) w instancjach MythicDungeons, odznaki podnoszą maksymalny poziom stworów.
- Wieża Chowańców — tryb falowy na bazie Wieży (Wieza_p01).
- Ranking PvP + sezonowe turnieje.

## 7. Rozwój i ekonomia
- Profesja MMOCore „Poskramiacz”: exp za łapanie, lepsze pieczęcie, większy limit drużyny.
- Karmienie PROF_* = exp + więź; więź daje bonus i jest warunkiem ewolucji części gatunków.
- Receptury (stacje MMOItems, wejścia wyłącznie mmoitem{...}): pieczęcie, kamienie przemiany, eliksiry, siodła (stwór → wierzchowiec).
- Hodowla: dwa stwory → jajo → wykluwa się po przejściu dystansu → dziedziczenie genów.
- Handel między graczami; stwory wymienialne lub związane. Zero P2W.

## 8. Bestiariusz (Pokédex)
- Widziany/złapany per gatunek, opis, typ, mapa występowania.
- Nagrody za kompletność: 10/25/50%, komplet typu, wszystkie lśniące → tytuły, exp MMOCore, unikalne pieczęcie.

## 9. Grafika
- Każdy gatunek: model BlockBench + animacje (idle, walk, attack, okrzyk, łapanie/drżenie pieczęci), ikona Nexo.

## 12. Ryzyka (obowiązkowo adresowane)
- Wydajność przy 2 GB heapu: 1 aktywny stwór na gracza, żadnych skilli co tick, pomiar spark przed/po.
- Profil-duch MMOProfiles; OP fałszuje hasPermission (isPermissionSet).
- E/Tab nie do przechwycenia → drużyna w menu pod Q.
- Koszt grafiki — jakość ponad ilość, ale minimum z sekcji 0 musi być spełnione.
