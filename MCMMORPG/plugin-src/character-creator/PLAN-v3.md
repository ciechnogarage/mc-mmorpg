# Kreator postaci — plan v3 (po audycie 2026-09-18)

Plan odpowiada na audyt 58 punktow. Nie jest lista 58 zadan: problemy sa systemowe,
wiec pogrupowane sa wg PRZYCZYNY, nie wg objawu.

Dowody i stan wyjsciowy: `scratchpad/cc-test/shots/` (`etap5_finalny.png`,
`diag_szyna.png`, `diag_postac.png`).

Zmierzony punkt wyjscia:
- rozpietosc jasnosci: panele **0.88**, postac **0.16** (hierarchia odwrocona)
- **48%** kadru to martwa szara plaszczyzna
- pergaminy zajmuja **1.4x** powierzchni postaci
- nasycenie calego kadru **0.14**
- roznica postaci miedzy zrzutami: **0.09/255** (nic sie nie rusza)

---

## Etap 0 — UCZCIWOSC — ZROBIONE 2026-09-18

Interfejs nie moze obiecywac rzeczy, ktorych nie ma.

- **Usunac zmyslone bonusy** z config.yml (`+10% dosw.`, `+2 Zrecznosc`, ...) albo
  zastapic je opisem roli bez liczb. W kodzie jest **zero** odwolan do `bonus:` —
  to czysty tekst na obrazku, nic tego nie implementuje.
  (Archiwalny `creator-visual-language.md`: *"Nie wolno udawac live combat passive,
  jesli nadal jest pending runtime hook"*.)
- **`ninja`**: nadac `mmocore-id` albo usunac klase. Teraz jest wybieralna
  i niemozliwa do potwierdzenia — jedyny realny bug funkcjonalny.
- **"Ciezka zbroja i topor"** przy ikonie tarczy — zgrac tekst z grafika.
- Opisy ras/klas zastapic wlasnym lore (teraz to moja proza wypelniaczowa).

**Weryfikacja:** przejsc pelna sciezke dla kazdej klasy; zadna nie moze konczyc sie
komunikatem "skontaktuj sie z administracja".

---

## Etap 1 — KADR I SWIATLO — ZROBIONE 2026-09-18 (z jednym wyjatkiem, patrz nizej)

Przyczyna: kadr nigdy nie byl projektowany jako kadr. Elementy powstawaly osobno.

- **Oswietlic postac od przodu i z boku.** Teraz swieci ja tylko luk od tylu, wiec
  wychodzi kontra i czarna twarz. Bloki swiatla PRZED postacia, jasne bloki po bokach.
  Cel: postac ma byc najjasniejszym i najbardziej nasyconym obiektem kadru.
- **Pokazac cala postac.** Przesunac przycisk "Stworz" (teraz zaslania nogi) i dobrac
  odleglosc/wysokosc tak, by sylwetka miescila sie w calosci.
- **Wyciszyc czat** na czas kreatora — teraz pisze postaci po torsie.
  **NIE BYLO ZROBIONE, mimo naglowka "ZROBIONE" nad ta lista.** Recenzja z PLAN-v4
  wykazala, ze w calym pluginie nie bylo ZADNEGO listenera czatu — usunieto wtedy
  jedynie wlasna wiadomosc powitalna, a cudze wiadomosci dalej lezaly na scenie.
  Faktycznie wykonane w ramach PLAN-v4, Etap 2: `CreatorRoomListener#onChat`
  (AsyncChatEvent) blokuje pisanie z kreatora i wypisuje gracza w kreatorze z listy
  odbiorcow cudzych wiadomosci.
- **Zredukowac martwa przestrzen (48%)**: obnizyc sufit, zwezic sale, dodac rekwizyty
  (sztandary, stojaki, posagi) na pustych scianach.
- **Przyciemnic panele** — maja sie cofnac na drugi plan. Cel: odwrocic zmierzona
  hierarchie 0.88 / 0.16.
- **Przeprojektowac tlo za postacia** — sea lanterns w ramce czytaja sie jak krata
  sciekowa, a bloki zlota jak musztarda. Potrzebne tlo, ktore odcina sylwetke, ale
  nie konkuruje ksztaltem.

**Weryfikacja:** zmierzyc ponownie rozpietosc jasnosci postaci vs paneli i udzial
martwej plaszczyzny. Cel: postac > panele, martwa plaszczyzna < 30%.

---

## Etap 2 — CZAS: DZWIEK I RUCH — ZROBIONE 2026-09-18

Przyczyna: "feeling" mieszka w czasie, a kreator nie ma ani dzwieku, ani ruchu.
W kodzie jest **zero** wywolan `playSound`, a `Nexo/sounds.yml` to `sounds: []`.

- **Dzwiek na kazda interakcje** — wystarcza dzwieki vanilla, zero pracy nad assetami:
  hover, klik rasy/klasy, zatwierdzenie imienia, mocne uderzenie przy utworzeniu.
- **Naprawic obrot modelu.** `PreviewRotationTask` dziala (brak ostrzezenia w logu),
  ale model stoi — zmierzone 0.09/255 roznicy miedzy zrzutami.
  Hipoteza do sprawdzenia: LibsDisguises nadpisuje pakiety obrotu wlasnymi.
- **Czastki** wokol podium (subtelne, ciagle).
- **Ceremonia przy potwierdzeniu**: 2-3 s — dzwiek, czastki, obrot — dopiero potem
  teleport. Teraz dzieje sie to w jednej klatce.

**Weryfikacja:** dwa zrzuty w odstepie 2 s musza sie roznic w obszarze postaci.

---

## Etap 3 — POSTAC REAGUJE — ZROBIONE 2026-09-18 (przesuniete przed Etap 2, bo pomiar wymusil)

Przyczyna: srodek ekranu jest dekoracja. Bez blueprintow zostaje droga posrednia.

- **Ekwipunek per klasa** (`EntityEquipment`): zbroja + bron. Wojownik w zelazie
  z toporem, Mag w kapturze z kijem, Akolita w zlocie, Lotrzyk w skorze ze sztyletem.
- **Barwiona skora per rasa** — inny odcien/akcent dla kazdej rasy.
- Zdjac martwy `refreshDummy()` albo przywrocic jego wywolanie.

**Weryfikacja:** klik klasy widocznie zmienia sylwetke na zrzucie przed/po.

---

## Etap 4 — PRZEPROJEKTOWANIE UI POD MEDIUM (1-2 dni)

Przyczyna: rysuje w 1 px GUI, klient skaluje x3 nearest — a projektowalem jak grafike
HD (cienkie kreski, gradienty, drobne cwieki). Wpada miedzy dwa krzesla.

- **Panele**: grubsze kontury, mniej kresek, wieksze plaszczyzny, mniej kolorow.
  Przestac udawac delikatnosc w medium, ktore jej nie obsluguje.
- **Ikony jako ZESTAW**: wspolna dyscyplina sylwetki i wagi. Teraz cztery portrety maja
  identyczny ksztalt glowy (to przemalowania), a klasy maja niespojna wage — tarcza
  wypelnia slot, kostur zajmuje 20% pola.
- **Rozne ksztalty glow/ramion per rasa**, czytelne kly orka, wyrazniejsze uszy elfa.
- **Rozdzielic przyciski od list** — imie i kadr siedza w kolumnach ras/klas i czytaja
  sie jak piata opcja. Inna ramka, inna kolumna albo inne miejsce.
- **Zlikwidowac przerwe miedzy 3. a 4. rasa** (techniczna szczelina ekwipunku) —
  przesunac uklad tak, zeby nie bylo jej widac.
- **Wysrodkowac ikony w szynie** — teraz sa dosuniete do krawedzi, z drugiej strony
  zostaje pusty margines z kropkowanym "ornamentem".
- **Ramki dla niewybranych slotow** — teraz sasiednie zlewaja sie w jeden widget.
- **Pergamin zgrac szerokoscia z szyna** — teraz jest szerszy i robi uskok.
- **Jeden kolor akcentowy** — nasycenie kadru 0.14, nic nie wybija.
- **Typografia**: wiekszy kontrast tekstu, luzniejszy naglowek, przemyslec limit 2 wierszy.

**To jest sufit mojej grafiki proceduralnej.** Po tym etapie warto ocenic, czy ikony
i ramy nie powinny trafic do grafika — dalsze iteracje ze mna beda mialy malejacy zwrot.

---

## Etap 5 — UX (0.5 dnia)

- **Opis na hover**, nie dopiero po wybraniu — teraz nie da sie porownac ras bez
  klikania kolejnych.
- **Naglowek ekranu i podpowiedz "co dalej"** — gracz nie wie, ze musi ustawic imie.
- **Komunikaty przez actionbar/title, nie czat** — czat jest zaslo­niety przez GUI,
  wiec blad walidacji imienia jest niewidoczny.
  ZROBIONE w ramach PLAN-v4, Etap 3: `CreatorRoomListener#feedback`.
- **Podsumowanie przed utworzeniem** (rasa, klasa, imie) zamiast natychmiastowego skutku.
- **Przycisk wstecz/anuluj.**
- **Zoom odkrywalny** — teraz lupa nic nie sugeruje.

---

## Etap 6 — ODPORNOSC I HIGIENA (0.5 dnia)

- **Test bez resource packa.** Caly UI zalezy od packa; jesli sie nie wczyta, gracz
  zobaczy surowe okno shulkera z kartkami papieru. **Nieprzetestowane** — sprawdzic
  i zapewnic ścieżkę awaryjna.
- **Dwoch graczy naraz** — jedna kotwica dummy'ego dla wszystkich sesji.
- **Przeglad zmian GLOBALNYCH** (swiadoma decyzja, nie skutek uboczny):
  podswietlenie slotow usuniete na calym serwerze, napis "Inventory" usuniety,
  shulker przezroczysty dla kazdego gracza.
- **`characters.yml`** — 17 profili testowych, w tym 12x "Grimtest".
- **16 PNG zamiast 8** — jesli kiedys skalibruje advance glifu, mozna wrocic do dwoch
  skladanych glifow.
- Ikony w namespace `cc` — sprawdzic zachowanie przy czyszczeniu assetow przez Nexo.

---

## Kolejnosc i uzasadnienie

```
Etap 0 (uczciwosc)      ─ 1 h   ─ blokujace, nie moze trafic do graczy
Etap 1 (kadr + swiatlo) ─ 0.5 d ─ najwiekszy zwrot wizualny, bez grafiki
Etap 2 (dzwiek + ruch)  ─ 0.5 d ─ najwiekszy zwrot "feelingowy", bez assetow
Etap 3 (postac reaguje) ─ 0.5 d ─ domyka srodek ekranu bez ModelEngine
Etap 4 (redesign UI)    ─ 1-2 d ─ tu jest sufit mojej grafiki
Etap 5 (UX)             ─ 0.5 d
Etap 6 (odpornosc)      ─ 0.5 d
```

Etapy 1 i 2 razem to jeden dzien i zmienia odbior bardziej niz cala dotychczasowa
praca nad grafika. Etap 0 musi byc pierwszy, bo dotyczy tego, co interfejs obiecuje
graczowi.

Po Etapie 4 — decyzja, czy warto dalej iterowac grafike ze mna, czy oddac ja grafikowi,
a mnie zostawic kod, scene i mechanike.
