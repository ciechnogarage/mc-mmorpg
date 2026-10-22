# Królestwa i Rody — projekt v1 (19.10, szkic do recenzji)

Kontekst: serwer MMORPG Paper 1.21.x, world1 (lochy MythicDungeons, MMOCore/MMOItems/MythicMobs/ModelEngine, Nexo, DeluxeMenus pod Q, Chowańce, Rzemiosło+Mistrzostwa, Wiary, Arena, HUD). KANON (docs/KANON.md): postać 1–100, akty = lochy + wyspa (wyspy na osobnym serwerze), dwie ścieżki równe, łup równej wartości, po 100: druga postać/HC (Ślub Milczenia), endgame. Populacja dziś: 4 konta. Serwer offline-mode, logowanie wyłączone.

Prośba właściciela: każdy gracz (za walutę z gry albo pieniądze) zakłada własne „królestwo”: teren, rozbudowa, NPC, wysyłanie ich na misje, małżeństwo i dzieci jak Mount & Blade; gildia = to samo, tylko grubiej. „Odpierdolić w chuj grubo”.

Research (19.10): Bannerlord (tiery klanu, lenna, towarzysze z rolami, karawany, małżeństwo, dzieci 18 lat — krytykowane), Skyrim Hearthfire (małżonek: dochód + posiłek-buff), FFXIV (wyprawy retainerów/sterowców o stałym czasie), Albion (robotnicy wzmacniają działania gracza, zadowolenie z mebli/trofeów), WoW Garnizony (PORAŻKA: zamknęły graczy w instancji, unieważniły resztę gry), Lost Ark/BDO (pętle timerów i karmienia = obowiązek), ArcheAge/Towny (gorączka ziemi, miasta-widma), Hypixel (produkcja offline liczona rachunkiem), Wynn housing (wyspa-instancja za walutę z gry). Usage Guidelines Mojang: za prawdziwe pieniądze tylko to, co nie daje przewagi (kosmetyka); opłata za „część serwera” ryzykowna.

## 0. Zasady nadrzędne (z researchu)
1. Królestwo WZMACNIA wypady w świat (lochy, wyspy), nie zastępuje ich. Nigdy lepszy sprzęt niż z bossa. Nagrody: składniki, wygoda, kosmetyka, NPC z cechami, fabuła.
2. Działa SOLO od dnia 1 (populacja). Gildia to warstwa na wierzchu.
3. Zero kar za nieobecność. Nieaktywne królestwo „usypia”, niczego nie traci. Produkcję offline liczymy rachunkiem przy wejściu.
4. Timery raz na sesję (1 h / 8 h / 24 h), nigdy „pilnuj co godzinę”.
5. Mało wskaźników (2), podnoszonych przedmiotami z gry = ujście dla łupu i rozbiórki.
6. Zagrożenia tylko od mobów (PvE), zawsze gdy gracz jest online.
7. Za prawdziwe pieniądze wyłącznie kosmetyka. Założenie królestwa i wszystko funkcyjne = waluta z gry.

## 1. Ród i tytuł (drabina)
- Gracz zakłada **Ród** (nazwa, herb z edytora sztandaru, dewiza). Ród jest per KONTO (wszystkie postacie konta należą do rodu); królestwo jedno na ród.
- **Renoma** rodu z: pierwszych przejść lochów, bossów, przygód wysp, wydarzeń królestwa. Tiery: Dwór → Włość → Baronia → Hrabstwo → Księstwo → Królestwo (6 tierów, progi typu 50/150/350/900/2350/6150 jak Bannerlord, przeliczone na godziny gry).
- Tier = **jakościowe odblokowanie**: nowy sektor ziemi, slot NPC, typ budynku, typ wyprawy, tytuł przy nicku/w HUD. Nigdy +% do walki.
- Koszt założenia: waluta z gry + list nadania (przedmiot z zadania w stolicy — mała historia „dostajesz ziemię od…”).

## 2. Ziemia (instancja)
- Każdy ród dostaje **dolinę** — instancję poza światem (jak wyspy Wynna): wejście z Bramy Królestw w stolicy i z menu Q.
- Dolina jest podzielona na **sektory**. Na start mały dziedziniec, reszta to „dzikie ziemie” zajęte przez obóz mobów, ruinę, gniazdo. Odblokowanie sektora = krótka przygoda (oczyść obóz, mini-boss MM, naprawa mostu) — ziemia zdobywana walką, nie kupowana.
- Kilka szablonów dolin (sadowa, górska, bagienna, przybrzeżna — z krain gry), pieczone Irisem offline; wybór przy założeniu.
- Sektory mają węzły surowców (zioła, rudy, drewno) dla Rzemiosła — szybsza droga do materiałów, które istnieją też w świecie (nie wyłączne).

## 3. Budowa
- Sektory mają **gniazda** o stałym obrysie; budynki funkcyjne z .nbt: Dwór/Zamek (serce), Spichlerz, Koszary, Warsztat, Zielnik, Stajnia/Zagroda Chowańców, Kaplica, Karczma (rekrutacja), Wieża zwiadu, Port/Przystań (wyprawy morskie), Sala Trofeów.
- Poziomy budynków (3–5), koszt: waluta + materiały z naszych treści (pochłaniacz ekonomii).
- **Skórki** budynków (styl ras: ludzki, elficki, orczy, nieumarły + style krain) — kosmetyka, można sprzedawać za pieniądze.
- Funkcje na **kotwicach** (markery w .nbt), magazyny wirtualne → zmiana skórki niczego nie niszczy.
- **Wolna zabudowa** poza gniazdami (survival, paleta bloków + meble Nexo) — dekoracja.
- **Własne projekty** budynków: tylko przez sanitizer .nbt (biała lista bloków, zero danych bloków poza banerami, zero encji) + akceptacja admina.
- Stacje produkcyjne (MMOItems) ZOSTAJĄ w stolicy; Warsztat w królestwie daje kolejkę zleceń NPC-rzemieślnika (pracuje, gdy cię nie ma), nie zastępuje stacji.

## 4. Ludzie (NPC)
- **Poddani** to unikalne NPC z imieniem, rasą (4 rasy), skórką (mineskin), 1–2 cechami i poziomem.
- Rekrutacja POZA królestwem: uratowani w lochach (więzień w celi), spotkani na wyspach, z karczmy w stolicy, z wydarzeń. → każdy NPC to pamiątka z przygody, ciągnie gracza w świat.
- Role (slot = jedna rola): Zarządca (dochód/ład), Kapitan straży (obrona przed najazdami), Zwiadowca (wyprawy, odkrycia), Rzemieślnik (kolejka zleceń), Zielarz (leczy rannych NPC, mikstury), Kwatermistrz (zapasy), Kapelan (Wiary: kosmetyka/fabuła, bez mechaniki patrona), Towarzysz (idzie z graczem w świat — jak Chowaniec, slot wspólny z Chowańcem albo zamiast).
- Cechy zmieniają zachowanie (np. „Chciwy” — więcej łupu z wypraw, mniej ładu; „Weteran” — mniej ran).
- **Zadowolenie** poddanych (jak Albion): rośnie od kwater, mebli, trofeów, posiłków ze spichlerza; niezadowolony pracuje słabiej (nie odchodzi bez ostrzeżenia, nie kara za nieobecność).

## 5. Wyprawy
- Tablica wypraw w Wieży zwiadu / Przystani: wysyłasz 1–3 NPC na 1 h / 8 h / 24 h do krain, które TWOJA postać już odwiedziła/przeszła.
- Wynik: raport z krótką historią (generowany z szablonów krainy + cech NPC), materiały, waluta, renoma, czasem **trop** — „zwiadowca znalazł ukrytą komnatę w Lochu 003 / skrytkę na wyspie” → przygoda dla gracza w świecie (anti-Garnizon: wyprawy prowadzą gracza do treści, nie zamiast niej).
- Ryzyko PvE: ranny NPC (wyłączony na X h, leczy Zielarz), nigdy śmierć bez ostrzeżenia; karawany handlowe (Bannerlord) z szansą napadu — napad = wydarzenie, w którym gracz może odbić karawanę.
- Odkrywanie: wyprawy odsłaniają kolejne „sektory mapy wypraw” (jak sterowce FFXIV) — długi postęp.
- Bez tabeli procentów do liczenia w Excelu: szansa pokazana słownie (pewna / ryzykowna / szaleństwo).

## 6. Gospodarka królestwa
- Dwa wskaźniki: **Zapasy** (jedzenie, materiały — z węzłów, wypraw, zakupu) i **Ład** (straż, zarządca, budynki, rozwiązane sprawy).
- **Skarbiec rodu**: osobna księga waluty królestwa (dochód z poddanych, wypraw, targu), wymiana na walutę postaci z limitem tygodniowym (żeby nie rozwalić ekonomii).
- **Sprawy dworu**: raz na sesję 1–3 decyzje (spór dwóch poddanych, prośba wioski, poseł) — dialog Papera z wyborem, skutki na ładzie/zadowoleniu/fabule. Zależne od stanu królestwa (lekcja Fallouta), nie losowy spam.
- **Najazdy**: gdy jesteś online w królestwie i warunki (niski ład, sąsiedztwo dzikich sektorów, wydarzenie krainy), fala mobów MM atakuje — bronisz z Kapitanem, strażą i Chowańcami. Nagroda: renoma, trofea do Sali. Można zignorować — wtedy najazd „przechodzi” z małą stratą zapasów, nigdy budynków.

## 7. Ród: małżeństwo i dzieci
- **Małżeństwo z NPC**: kandydaci to wybrane NPC świata (szlachcianki/szlachcice z krain, poznani w przygodach — po 1–2 na krainę). Zaloty = krótka linia zadań + rozmowy (dialogi) + dar; kandydat ma własne oczekiwania (np. renoma rodu, dokonanie). Ślub w kaplicy w stolicy (wydarzenie, goście mogą przyjść).
- Małżonek: zostaje w królestwie jako **Pani/Pan dworu** — rola Zarządcy bez slotu, codzienny posiłek (lekki buff wygody, np. regeneracja poza walką, nie DPS), własne sprawy dworu, rozmowy zależne od twoich przygód (komentuje ostatni loch). Może iść z tobą jako Towarzysz.
- **Małżeństwo dwóch graczy**: łączy rody (wspólne królestwo albo sojusz dwóch dolin), wspólny herb. Premia za granie razem (jak ESO).
- **Dzieci**: rodzą się po ślubie (po np. 3–7 dniach realnych). Dorastanie AKTYWNE, nie czekanie: 3 etapy (dziecko → podrostek → dorosły), każdy kończy się zadaniem wychowania, które gracz wykonuje (zabierz na targ, naucz władać bronią na arenie, zabierz do świątyni) → wybory kształtują cechy i ścieżkę (wojownik / uczony / rzemieślnik / kapłan). Całość ~2–3 tygodnie realne.
- Dorosłe dziecko = unikalny NPC z cechami po rodzicach: najlepszy Zarządca/Towarzysz/Zwiadowca, może przejąć królestwo.
- **Dziedzic a KANON**: po 100 „druga postać / HC”. Druga postać konta może zacząć jako dziecko rodu (wygląd/imię/herb, tytuł „z rodu X”, kosmetyki) — dynastia bez permadeath. HC (Ślub Milczenia): śmierć HC odbiera status, dziedzic go przejmuje (fabularnie).
- Bez CK-ów: brak intryg, zabójstw, sukcesji tronu przy śmierci; ród to emocja i tożsamość + kilka unikalnych NPC.

## 8. Gildia = Korona (to samo grubiej)
- Gildia to **Korona** — związek rodów. Członkowie zachowują swoje doliny; składają przysięgę lenną → ich doliny stają się lennami Korony na mapie Korony.
- Korona ma **stolicę gildii** (większa instancja) z wielkimi projektami budowy: każdy ród wnosi materiały i NPC (jak sterowce FC) — mury, katedra, flota.
- **Rada Korony**: role z rodów członków (Kanclerz, Marszałek, Skarbnik, Kapłan) — uprawnienia + jedna mechanika na rolę.
- **Wyprawy Korony**: duże, wieloosobowe wyprawy (NPC z kilku rodów naraz), dłuższe, z odkryciem całej krainy wypraw.
- **Najazdy na Koronę**: oblężenie mobów na stolicę gildii — wydarzenie dla całej gildii w zapowiedzianym terminie.
- Tytuł króla dla głowy Korony, herb Korony nad herbami rodów.
- Od 2 rodów. Przy 4 kontach realnie jedna Korona — działa jako wspólny projekt, nie rywalizacja.
- PvP między Koronami (wojny, terytoria w otwartej strefie, turnieje na Arenie) dopiero po progu populacji (np. 30 aktywnych tygodniowo i 4 Korony).

## 9. Społeczne
- Odwiedziny: każdy może zwiedzić cudzą dolinę (Brama Królestw: lista, ranking piękna/renomy, „dolina tygodnia”).
- Targ: twój NPC-kupiec prowadzi stragan w stolicy i w twojej dolinie (handel bez obecności).
- Kronika rodu: dziennik (ślub, narodziny, oczyszczone sektory, wielkie wyprawy) + drzewo genealogiczne.
- Herb przy nicku, w HUD (karta gracza), na tarczy, na budynkach.

## 10. Pieniądze
- Założenie rodu i królestwa, sektory, budynki, NPC, wyprawy: TYLKO waluta z gry.
- Sklep (prawdziwe pieniądze), tylko kosmetyka: skórki budynków, style herbów i sztandarów, skórki NPC/strojów ślubnych, dekoracje, efekty. Historia zakupów dla kupujących. Osobna pula od nagród sezonowych.
- Ryzyko prawne wariantu „królestwo za pieniądze”: sprzedaż funkcji = przewaga = łamie Usage Guidelines; nie robimy.

## 11. Technika (wstępnie)
- Własny plugin `Krolestwa` (ród, królestwo, NPC, wyprawy, Korona) — zamiast Guilds (Guilds zbyt płytki; KingdomsX = PvP/inwazje, bez MMOCore). Mostek MMOCore guildModule (relacje: Korona = sojusz).
- Doliny: jeden świat z siatką slotów daleko od siebie, wklejanie szablonu FAWE przy założeniu, chunki ładowane tylko przy wizycie (bez forceload). Docelowo osobny serwer za proxy (jak wyspy) — nie obciążać world1.
- NPC: MythicMobs + skórki przez mineskin (LibsDisguises/mannequin jak kreator), dialogi Papera (custom click), spawn przy wejściu do doliny, despawn przy wyjściu.
- Wyprawy/produkcja: znacznik czasu + rachunek przy wejściu (żadnej symulacji offline).
- Dane: baza per konto, klucze UUID; po włączeniu logowania (warunek konieczny: offline-mode = kradzież rodu nickiem).
- Wszystko z menu (Q → Ród), gracz nie wpisuje komend.

## 12. Etapy
- F0: logowanie na proxy; decyzja: doliny na world1 czy osobny serwer.
- F1 „Dwór” (wycinek grywalny): założenie rodu, dolina z 1 szablonem, 3 sektory (1 do oczyszczenia), 4 budynki, 3 NPC (z lochów/karczmy), wyprawy 1/8/24 h z raportem i tropem, Skarbiec, menu Q. Test solo na klonie.
- F2 „Włość”: zadowolenie, sprawy dworu, najazdy, więcej szablonów, skórki, wolna zabudowa.
- F3 „Ród”: małżeństwo NPC, dzieci z aktywnym dorastaniem, kronika, drzewo.
- F4 „Korona”: gildia, lenna, stolica gildii, wielkie projekty, rada, wyprawy Korony.
- F5: odwiedziny/targ/ranking; PvP Koron po progu populacji.
