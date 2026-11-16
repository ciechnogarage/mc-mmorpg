# Rasy i klasy — analiza sensu, logiki i spójności (2026-10-03)

Analizę zrobił Codex (tylko odczyt plików), Claude sprawdził każdą tezę w plikach, poprawił oceny i dopisał braki.
Surowy raport Codexa: 13 pozycji; po weryfikacji 1 fałszywa, 2 przeszacowane, 3 dopisane.

## Naprawione od razu
| Co | Poprawka |
|---|---|
| Kreator: „umiejętność startowa” wszystkich 16 klas = stare demo (Deep Wound / Fire Storm / Minor Healings) | `karta_kreatora.py` czyta nazwy i pierwsze zdanie opisu ze WSZYSTKICH `MythicLib/skill/*.yml`; uruchomiony — karta pokazuje sygnaturę rasy (np. Pchnięcie Korony, Kosiarz Ciszy) |
| Kreator, cecha orka: „-20% many (poza wojownikiem)” — generator tnie tylko manę maga | „-20% many (tylko mag)” |
| Kreator, cecha elfa: „szybsza regeneracja many” — nie dotyczy Dusz akolity | „szybsza regeneracja zasobu (poza akolitą)” |
| Kreator, kafelek Wojownika: „Ciężka zbroja i tarcza.” — baza ma L+Ś, tarcza tylko w 3 specjalizacjach | „Pierwsza linia: stal, zbroja i Szał.” |
| `rozpiska-16-klas.md` z nazwami i stanem z 26.09 | dopisek HISTORIA u góry + wskazanie aktualnych źródeł |
| `burza-sygnatury`: „broń rasowa musi być wyposażona” — wdrożenie etapu 3 zdecydowało inaczej | dopisek ODRZUCONE (broń = premia ◆) |

Zmiany w `plugins/CharacterCreator/config.yml` działają po `/cc reload` albo restarcie. Kopia sprzed: `backups/kreator-karta-spojnosc-20261003/`.

## Otwarte — wymagają decyzji (od najważniejszych)
1. **[ważny] Rasy nierówne: ork nie płaci nic w 3 z 4 archetypów.** Wojownik/ninja/akolita orka: +15% HP, +10% obrażeń fizycznych;
   kara -20% many dotyczy tylko maga (Szał/Skupienie/Dusze mają stałą pulę). Elfia „regeneracja” poza magiem to +0,1/s na pasku 100
   (≈ nic) — elf-wojownik/ninja/akolita ma realnie tylko +10% ruchu za -10% HP. Propozycja: kara orka i bonus elfa w zasobie archetypu
   (np. ork: wolniejszy przyrost Szału/Skupienia, mniejsza pula Dusz; elf: +Skupienia/s, wolniejszy spadek Szału) albo statystyka obecna
   w każdym archetypie. (Liczby: poz. 10 z wygenerowanych plików klas.)
2. **[ważny] Podpis „obrażenia pocisków” specjalizacji dystansowych prawie nic nie daje.** Łowca Nagród, Srebrna Cięciwa, Kościany Grot,
   Łucznik Ciszy dostają `projectile-damage`, a WSZYSTKIE skille ninja (Bełt, Strzała z Korony, Ślad Kła, Egzekucja...) zadają
   `SKILL,PHYSICAL` — premia działa tylko na zwykły strzał z łuku. Propozycja: skille pociskowe z typem `PROJECTILE` albo inny podpis.
   (Codex zgłosił tylko Egzekucję — problem jest szerszy.)
3. **[ważny] Ork Szaman: rasowe +10% fizycznych nie wspiera maga** (czary = `MAGIC`), a mag orka jako jedyny płaci -20% many.
   Na papierze najsłabsza para rasa×archetyp.
4. **[ważny] Dusze akolity bez źródła w walce z samotnym bossem.** Pula 10, brak regeneracji, źródło = śmierć moba w r=16 (także
   własnego sługi, więc Wezwanie kosztuje netto 1). Celowe wg `skille-specjalizacji` („Dusze wyłącznie z reguły śmierci”), ale boss bez
   adds wyczerpuje akolitę (Opieka też kosztuje Duszę). Codex: krytyczny — raczej decyzja projektowa niż błąd.
5. **[ważny] Znamiona ninja wspólne dla drużyny.** `target.znamiona` to jedna zmienna na celu: Egzekucja jednego ninja zużywa Znamiona
   drugiego. Do decyzji: osobiste czy drużynowe (wtedy opisać w lore).
6. **[ważny] Wiara nie skaluje leczenia.** Główny atrybut akolity daje `skill_damage` i pulę Dusz; leczenie Opieki stałe 2–6 HP.
   Uboczny skutek: `max_mana` z Wiary powiększa też stały pasek Szału/Skupienia (100) wojownika i ninji.
7. **[średni] Lore Wiar: nieumarły może wybrać Vharuna**, którego `docs/wiary/panteon.md` opisuje jako „wroga wszystkich nieumarłych”
   (Starsi dostępni każdej rasie). Zablokować albo dopisać wyjątek fabularny.
8. **[drobny] Nieumarły akolita — wampiryzm czarów** działa (skille mają typ `SKILL`), ale nie obejmuje obrażeń sług, wokół których
   zbudowana jest klasa. Sugestia Codexa (kradzież życia) byłaby gorsza: działa na broń, nie na skille.
9. **[drobny, niepewne] Elfy „Dzieci Świtu”** przy kulcie księżyca (Aelyra); dzień to Solenna (ludzie).

## Odrzucone tezy Codexa
- „Sygnatury nie wymagają broni rasowej wbrew projektowi” — wymóg odrzucono świadomie przy wdrożeniu etapu 3; nieaktualny był dokument.

## Pytania do właściciela
1. Wyrównać orka i elfa w zasobie archetypu (pkt 1)?
2. Specjalizacje dystansowe: skille pociskowe jako `PROJECTILE` czy inny podpis (pkt 2)?
3. Akolita ma utrzymać walkę z samotnym bossem bez zapasu Dusz (pkt 4)?
4. Znamiona osobiste czy drużynowe (pkt 5)?
5. Skalowanie leczenia/osłon z Wiary (pkt 6)?
6. Vharun dla nieumarłych: blokada czy wyjątek fabularny (pkt 7)?

---
# Część 2: ekwipunek klas (Codex + weryfikacja, 2026-10-03)

**Dlaczego Wojownik nie ma C i tarczy:** decyzja rozpiski 26.09 („Spec frontowa: +1 stopień”, tarcza = biegłość spec obronnych).
Baza Wojownika L+Ś, ciężką zbroję i tarczę dają dopiero Nieugięty / Różany Rycerz / Tyran / Kustosz Krypt na poz. 10 = koniec Aktu 1.

## Potwierdzone dziury (od najważniejszych)
1. **[krytyczny] Ciężka zbroja martwa przez cały Akt 1.** 31 części C ma poziom ≤10, ale żadna klasa bazowa nie ma biegłości C
   (komentarz w `tresc_startowa.py`: „ciężkiej nie ma żadna klasa bazowa”). Do tego zamiennik vanilli (`CoreTools/item-replacer.yml:50-53`)
   zamienia **każdą żelazną zbroję** (zombie, skrzynie) na `ZBROJA_ZELAZNA` = ciężka — najpopularniejszy pancerz gry jest bezużyteczny
   dla wszystkich na poz. 1–9. (Tę pozycję dopisał Claude; Codex jej nie zauważył.)
2. **[ważny] Tarcza u Akolity, nie u Wojownika.** Z 16 klas bazowych tarczę ma tylko Nosiciel Relikwii (człowiek-akolita: L+Ś+tarcza) —
   ma więcej obrony niż każdy bazowy Wojownik. 9 tarcz ≤ poz. 10 dla 1 klasy bazowej.
3. **[ważny] Tarcza + broń dwuręczna.** `two-handed-item-restriction: true` (MMOItems): Nieugięty (halabarda) i Kustosz Krypt (miecz dwuręczny)
   dostają tarczę, która wyklucza ich broń rasową — sygnatura z premią broni i tarcza się wykluczają. Tyran: topór jednoręczny OK.
4. **[ważny] Zasób archetypu podbijany przedmiotami.** `mana-regeneration` na pasach (5), naszyjniku, katalizatorze drugiej ręki i zbrojach,
   `max-mana` na 24 zbrojach i biżuterii — przedmioty dostępne każdemu. Dusze to przemianowana mana: pas z regeneracją daje Dusze z czasu
   (wbrew „Dusze tylko ze śmierci”), max-mana powiększa pulę Dusz i stałe 100 Szału/Skupienia. (Działanie w grze niezmierzone.)
5. **[średni] Broń poza biegłościami:** 3 przedmioty `COMBAT` (miecz 7 obr., łuk, kostur) i 10 `TOOL` z `attack-damage` bez wymagań;
   vanillowe siekiery celowo niezamieniane (narzędzie) = topór dla każdego. Typ `COMBAT` nie ma wpisu w `item-types.yml` (czy się ładuje — niepewne).
6. **[drobny]** Zestaw `SPELLCASTER` („dla magów”) wymaga lekkiej zbroi — 3 z 4 magów jej nie ma. Typ `STAVE` przypisany, 0 przedmiotów.
   Podgląd kreatora ubiera Wojownika w żelazo (`gear`), którego postać nie założy.

Sprawdzone i OK: 48 plików klas = generator, 16 zestawów startowych zgodnych z biegłościami, każda biegłość ma przedmioty poz. 1–9,
każda premia `?bronRasowa` wskazuje broń, którą klasa ma.

## Rekomendacja
Wojownik (wszystkie 4 rasy) od poz. 1: L+Ś+**C** + **tarcza**; start: jednoręczny miecz + tarcza obok broni rasowej, zbroja średnia
(albo żelazna). Spec obronne zachowują przewagę przez `defense`, prowokację i premię ◆ za tarczę; zamiast „+C +tarcza” dostają np.
+obronę/blok. Akolita człowieka zostaje przy tarczy (relikwia), ale bez C. Osobno: zdjąć `mana-regeneration`/`max-mana` z przedmiotów
dla nie-magów albo przenieść je na statystyki zasobu archetypu; `COMBAT`/`TOOL` bojowe — wymóg grupy albo usunąć.
