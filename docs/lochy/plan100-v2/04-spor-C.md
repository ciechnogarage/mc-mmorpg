# Spór — Projektant C (plan 100 v2, etap 2, 2026-10-09)

## 1. Werdykt
**PRZEPUSZCZAM syntezę v1 z jedną BLOKADĄ: Opętanie jako warstwa 3 kręgosłupa (§2).**
Rama (Cienie → Echa i Strażnicy → Kronika tylko w Aktach 7 i 10), łuk i 9 z 10 praw przyjmuję. Szczegóły w §3–5.
Naprawa blokady: Opętanie wypada z kręgosłupa i dostaje status eksperymentu z bramką wejścia (§2c). Na jego miejscu
jako warstwa 3 wchodzi **Wezwanie Cienia** (§2d).

## 2. Opętanie — mocna rzecz czy pułapka?
**a) Research — to już było, i to w najbliższym możliwym kształcie:**
- **Deathloop, „Protect the Loop”**: gracz wchodzi w cudzą kampanię jako antagonistka Julianna. Gdy nie ma człowieka, Juliannę prowadzi AI, a gospodarz ma tryby „tylko AI” i „tylko znajomi” ([wiki](https://deathloop.fandom.com/wiki/Protect_the_Loop), [ustawienia](https://attackofthefanboy.com/guides/deathloop-how-to-turn-off-invasions/)). To 1:1 nasze „Cień AI, a gdy właściciel online — człowiek”.
- **Dark Souls / Elden Ring**: inwazje jako własna postać w świecie gospodarza. Fora pełne skarg na grief („~50 inwazji w 4 h”, prośby o wyłączenie) ([Steam 1](https://steamcommunity.com/app/1245620/discussions/0/4511003376713098906/), [Steam 2](https://steamcommunity.com/app/1245620/discussions/0/5824898961043438216/)).
- **Resident Evil Resistance**: Mastermind steruje zombie i Tyranem przeciw czwórce na żywo ([Wikipedia](https://en.wikipedia.org/wiki/Resident_Evil:_Resistance)); do tego Evil Dead: The Game, Dungeon Keeper.
- **Wniosek**: Opętanie nie jest nowe (Deathloop zrobił dokładnie to). Nowe jest najwyżej „na MC i w MMO”, a mocną rzeczą pozostają Cienie AI.

**b) Dlaczego w naszej formie to pułapka (dowody):**
1. **Technika — sterowanie z zewnątrz w MC nie istnieje.** Opętanie = przeniesienie PRAWDZIWEJ encji gracza do cudzej instancji MD w trakcie przebiegu i podmiana ekwipunku na zestaw Cienia. Podmiana stałych slotów (Dziennik, Sakwa, MMOInventory, Księga zaklęć) grozi duplikacją lub utratą przedmiotów przy rozłączeniu albo crashu. Wymaga też wejścia do instancji kogoś spoza drużyny i flagi PvP. Do tego pamięć z 09.10: **znany, nienaprawiony crash „przywołańcy + PvP” na world1** (test zaklęć gracza). Opętanie od Aktu 7 opiera się na najsłabszym punkcie stosu.
2. **Rekursja kary**: Opętany ginie w cudzej instancji — czy zostawia nowy Cień? czy traci EXP? Każda odpowiedź to exploit: „nie” = darmowe PvP, „tak” = kaskada Cieni.
3. **Exploit nagród**: Odłamek za wygraną Cienia + zwrot EXP za przegraną = handel zwycięstwami między znajomymi (oddaję walkę, ty zwracasz mi EXP i bierzesz Odłamek; potem zamiana).
4. **Kolejki i tempo**: drużyna na Rozdrożu czeka na decyzję właściciela (odbiór, teleport, wczytanie chunków; pamięć md-wejscie-kick-floating). Każde 10 s przestoju w środku lochu psuje rytm beat chartu.
5. **Balans**: nasza walka i zapowiedzi są strojone pod AI (bot nieuważny/uważny). Człowiek z zestawem Cienia kituje, chowa się i gra na czas. Kontry PvE (zamknięty cel) nie działają na człowieka.
6. **Toksyczność**: to jest PvP inwazyjne. Najczęstsza skarga na Elden Ring, a u nas od Aktu 7, gdzie fabuła to konflikt frakcji (grywalnych ras) — przepis na „Ork najechał mnie na Trudnym” jako spór rasowy na czacie.

**c) Naprawa: eksperyment z bramką (nie kręgosłup).** Opętanie można włączyć dopiero, gdy spełnione są wszystkie: naprawiony i zmierzony crash PvP; zestaw Cienia jako osobny, tymczasowy profil MMOProfiles (bez dotykania ekwipunku gracza); wejście tylko ze Stolicy, w stanie bezczynności; tylko „znajomi i gildia” (jak tryb Friends Only w Deathloop), tylko Trudny, zgoda obu stron; limit 90 s, Cień nie wychodzi z Rozdroża; opętany Cień nie zostawia Cienia i nie traci EXP; **zero nagród za wygraną Cienia** (tylko licznik i kosmetyk, raz na tydzień); brak czatu, tylko 3 gesty.
Najlepiej jako **osobny tryb „Arena Cieni”** w Stolicy, a nie w lochach.

**d) Warstwa 3 zamiast Opętania: Wezwanie Cienia (od Aktu 7).** Wzór Nioh: Benevolent Graves, sojusznicze duchy
([Fextralife](https://nioh2.wiki.fextralife.com/Multiplayer)).
- Przy ognisku można przyzwać Cień znajomego jako SOJUSZNIKA (AI z migawki) na jeden loch.
- Gdy ten loch jest przejściem drużyny, właściciel dostaje część EXP z przejścia „swojego” Cienia.
- To PvE, asynchroniczne i bez rekursji kary. Domyka cykl: Cień wroga (Akt 1+) → Echo przewodnik (Akt 4+) → Cień przyjaciel (Akt 7+). Pasuje do Aktu 7: Cienie sojuszników z OBU frakcji walczą razem.

## 3. Wymiany praw
- **Akt 8 „Lawa zastyga” (X) — PRZYJMUJĘ, jest lepsze od mojego.** Moje „podłoga rośnie” było monotonicznym zegarem przestrzeni, blisko przypływu z Aktu 2. U X to zdarzenie lokalne z oknem (półka 20 s, potem kruszeje): decyzja „wskakuję teraz czy czekam”, a do tego czytelne i tanie (BlockEditor/fill).
- **Akt 3 „Słońce pali, cień chroni” (Y) — przyjmuję tożsamość, ale to LICZNIK** (żar narasta), czyli typ, który sami wycięliśmy przy aktach 3 i 5 starego planu. Naprawa, żeby był fizyczny: słońce przesuwa się w trakcie walki (cień przesłon obraca się co ~60 s), a żar nie jest paskiem, tylko trafieniem promienia w zapowiedzianym polu słońca (jasny krąg 1 s → poparzenie). Wtedy cień to RUCHOMA kryjówka.
- **3 vs 6 — TAK, kolizja.** W obu „cień” jest kluczem: w 3 cień chroni gracza, w 6 cień robi wroga nietykalnym. Gracz z Aktu 3 nauczy się „wchodź w cień”, a w Aktu 6 ta sama odruchowa reakcja zadziała przeciw niemu — to nie zwrot, tylko sprzeczny język wizualny dwóch praw. **Naprawa (zmieniam swoje 6):** „W blasku księżyca odbicie jest ciałem” — srebrne tafle (woda, lustra-mgły) odbijają teren i wrogów; w pełni księżyca odbity most jest przechodni, a odbity wróg walczy jako drugi egzemplarz. Kontra: zasłonić księżyc albo zmącić taflę. Cień zostaje wyłącznie w Akcie 3; księżyc i srebro tożsamością Aktu 6.
- **Kontrola „teren w czasie”**: zostają 2 (przypływ, cykl pionowy) i 8 (półki z wyrzutu, zdarzenie lokalne). Różne osie, akceptuję.

## 4. Co z X/Y powinno jeszcze wejść
1. **Wiary jako język interpretacji, nie metafizyka** (X §B8, Y §B4): bogowie nigdy nie pojawiają się osobiście, a Akt 10 nie „potwierdza” Ordhuna ani Vharuna. Tkacz Echa jest kapłanem, a nie bogiem. Chroni to plugin Wiary (łaski patronów) przed retconem.
2. **Atlas Szwów Y jako warstwa wiedzy**: 1 szew na akt (nie 20–30), czyli skrót-widok między krainami odkrywany z Kroniki. To nośnik łuku „skąd przyszedł Szczep”, za niewielki koszt.
3. **Moralnie szare antagonistki X/Y**: X (admirałka Salia: kwarantanna odcina żywność) i Y (Avara: „widziała, jak obcy ładunek zabił załogę”) są lepsze niż moja Ysolde, która tylko sprzedaje sadzonki. Proponuję połączenie: Ysolde sprzedaje sadzonki I trzyma kwarantannę na biedne porty.
4. **„Ostatnie słowo” z X**: zostaje (już w syntezie), 3 gesty bez wolnego tekstu.

## 5. Moje ryzyka E po syntezie — stan
| E | Ryzyko | Stan |
|---|---|---|
| 1 | trzy prawa „teren w czasie” | **zamknięte** (8 zmienione na X; 3 zmienione na Y) |
| 2 | patent Nemesis | **otwarte** — opinia prawna przed budową pluginu |
| 3 | moderacja nicków i skórek | **otwarte**; Echa i posągi zwiększają ekspozycję |
| 4 | pusty serwer = brak Cieni | **otwarte** — rezerwa Cieni deweloperskich w specyfikacji pluginu |
| 5 | Akt 7 konflikt frakcji | **otwarte, pogorszone przez Opętanie** (§2b6); po wycięciu wraca do stanu wyjściowego |
| 6 | Akt 10 kolaż ech | **otwarte** — zasada „1 echo prawa na loch” musi trafić do listy lochów Y |
| 7 | koszt 100 lochów | **zamknięte decyzją** („nie ograniczajmy się”), produkcja etapami |
| 8 | dwa archipelagi (2 i 9) | **otwarte** — kadry WOW i forma wyspy do rejestru L6 planu |
| 9 | ekonomia EXP (zwrot przez Cień) | **otwarte**; Echa i Wezwanie dodają strumienie EXP, potrzebna jedna tabela |
| 10 | słaby Akt 3 | **zamknięte** (Sefir i prawo Y), o ile licznik żaru zamieni się w ruchome słońce (§3) |
| N | crash PvP na world1 | **nowe**, blokuje Opętanie (§2b1) |
