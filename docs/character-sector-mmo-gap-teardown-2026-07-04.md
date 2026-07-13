# Character Sector MMO Gap Teardown

Data: 2026-07-04

Zakres: `klasy`, `rasy`, `profesje`, `itemizacja`, `skille`, `mastery`, `inventory`, `loadout`, `onboarding`, `first equipment loop`, `profiles`, `storage`.

Benchmark glowny: `World of Warcraft`, `Final Fantasy XIV`, `Guild Wars 2`, `The Elder Scrolls Online`.

Benchmark pomocniczy: `Black Desert Online`, `Old School RuneScape`, `Lost Ark` tam, gdzie problem dotyczy ekonomii, life skilli, sinkow, chase'u albo PvP consequences.

## Werdykt

Obecny character sector nie jest slaby dlatego, ze nie ma duzo contentu. Jest slaby dlatego, ze zbyt wiele jego najmocniejszych obietnic jest nadal `contract`, `pending runtime hook`, `selection contract`, `intro only` albo `city stand-in`.

Najwieksze MMORPG wygrywaja nie liczba klas, tylko tym, ze:

- klasa daje natychmiast odczuwalny model gry, a nie tylko pitch;
- rasa zmienia miejsce postaci w swiecie, a nie tylko ikonke i tone of voice;
- profesje sa osobnym silnikiem ekonomii, a nie menu z obietnica przyszlego XP;
- itemy daja chase, source identity i build obsession;
- onboarding buduje wage decyzji, zamiast szybko ja symulowac.

Wasz obecny sektor postaci ma dobry fundament dokumentacyjny i kilka trafnych kontraktow systemowych, ale przegrywa z benchmarkiem AAA tam, gdzie gracz powinien juz czuc tozsamosc, konsekwencje i dlugoterminowy powod, zeby zostac.

## S-Tier Braki

To sa braki, ktore sprawiaja, ze ten sektor nadal nie brzmi jak dojrzaly MMORPG, tylko jak dobrze opisany foundation slice.

### 1. Rasy sa bardziej obietnica tonu niz systemem MMO

Co obiecujemy:

- `foundation_race_selector.yml` sprzedaje rase jako pierwszy slad, po ktorym swiat ma rozpoznac postac.
- `character-sector-contract.md` definiuje juz `Race Passive Matrix`.

Co faktycznie istnieje:

- menu ras samo mówi `Pasyw zakontraktowany` i `Runtime hook: pending`;
- contract wprost stwierdza, ze rasa jest obecnie `identity and routing metadata, not starter power`;
- nie ma zywego sprzezenia rasy z frakcjami, regionami, NPC relation, ekonomia, profesjami, reputacjami ani contentem.

Jak robia to duze MMO:

- `WoW` daje rasom miejsce w swiecie, frakcji i fantasy polityczno-kulturowe, nie tylko flavor;
- `GW2` buduje rase jako cywilizacje i perspektywe na swiat Tyria, a nie jako kosmetyczny wybór;
- `ESO` i `FFXIV` nie zawsze daja ogromny power delta, ale rasa/pochodzenie nadal osadza postac w swiecie i społeczności.

Brak:

- brak aktywnego race gameplayu;
- brak race utility loop;
- brak race quest path;
- brak race-specific world response;
- brak race-to-profession synergies;
- brak race prestige albo social consequences.

Dlaczego to boli gracza:

Gracz nie wybiera pochodzenia. Gracz wybiera naglowek. To zabija wage pierwszej decyzji i oslabia cale fantasy postaci juz na starcie.

Cios naprawczy:

Dowiezc `small but live` rasowy system: jedna aktywna pasywka, jedna swiatowa preferencja, jedna profesyjna albo ekonomiczna interakcja i jeden NPC/world hook dla kazdej rasy, zamiast dopisywac kolejne lore.

### 2. Klasy maja pitch MMO, ale za malo live mechaniki MMO

Co obiecujemy:

- docs buduja klasy od combat rules, counterplayu, mastery i subclass fantasy;
- klasy w menu maja mocne i wyrazne archetypy.

Co faktycznie istnieje:

- klasy bazowe sa zrobione glownie jako zestawy `hardset` na ograniczonym katalogu MMOCore;
- wiele klas dzieli te same skille bazowe: `WEAKEN`, `COMBO_ATTACK`, `DEEP_WOUND`, `MINOR_HEALINGS`, `AMBERS`;
- nawet tam, gdzie klasa ma dobry flavor, live expression czesto konczy sie na innym opisie tego samego klocka;
- `rogue.yml` i `marksman.yml` szczegolnie mocno pokazuja, jak latwo klasa wpada w re-labelling wspoldzielonych zdolnosci.

Jak robia to duze MMO:

- `WoW` sprzedaje klase przez natychmiast rozpoznawalny resource model, spec loop i role combatową;
- `FFXIV` ma jobs z bardzo czytelną rolą i rotacją;
- `GW2` buduje profession fantasy przez unikalny mechanic layer, nie tylko nazwy skilli;
- `ESO` rozdziela klasy przez trzy własne skill lines na klasę.

Brak:

- za malo klasowego gameplayu, za duzo klasowego copywritingu;
- za malo signature loopów per archetyp;
- za malo osobnych resource tensions;
- za malo realnych weakness windows;
- za malo rozdzielenia między `front`, `burst`, `range`, `caster`, `sustain`.

Dlaczego to boli gracza:

Gracz ma wrazenie, ze wybiera narracje builda, ale nie czuje jeszcze, ze wybiera inny silnik walki.

Cios naprawczy:

Kazda klasa bazowa musi dostac przynajmniej jeden naprawde wlasny live loop, którego nie da się opisać jako reskin wspoldzielonego skilla.

### 3. Subclass fantasy istnieje glownie w docs, nie w doswiadczeniu

Co obiecujemy:

- 3 podklasy na klase;
- Level 25 jako prawdziwa specjalizacja;
- triale, mastery gates i signature skills.

Co faktycznie istnieje:

- subclass IDs sa wpisane w klasach;
- docs rozpisuja je szeroko;
- `character-sector-contract.md` wprost przyznaje, ze `Mastery Gates` i subclass unlock logic sa w duzej mierze `pending runtime hook`.

Jak robia to duze MMO:

- `WoW` specjalizacje zmieniaja praktyczny gameplay od razu;
- `GW2` elite specs przepisuja profession expression;
- `ESO` warstwuje build przez skill lines i transformacje stylu;
- `FFXIV` job identity nie czeka na czysto narracyjny moment odblokowania.

Brak:

- brak subclass trials jako realnych sprawdzianow;
- brak live mastery counting;
- brak signature unlock path odczuwalnego przez gracza;
- brak early foreshadowingu subclass fantasy w pierwszych godzinach gry.

Dlaczego to boli gracza:

Podklasa jest obietnica przyszlego MMO, nie obecna aspiracja builda. To odcina najwazniejszy długoterminowy haczyk postaci.

Cios naprawczy:

Wprowadzic subclass pre-echo juz w foundation: mentor, preview passive, trial objective i jeden tymczasowy signature moment przed Level 25.

### 4. Profesje nie sa jeszcze osobnym filarem ekonomii

Co obiecujemy:

- `professions-foundation-v0.0.1.md` definiuje profesje jako osobny filar progresji, ekonomii, craftu, lupu i PvP;
- menu `foundation_profession_roster.yml` sprzedaje gathering, crafting, profession skills i item sink.

Co faktycznie istnieje:

- samo menu pokazuje `Runtime XP: pending`, `Runtime recipe XP: pending`, `Runtime unlock: pending`;
- profession flow to na razie intro + source contract;
- nie ma jeszcze zywego loopu `gather -> craft -> refine -> salvage -> market -> progression pressure`;
- nie ma realnego rynku specjalizacji, który zmusza do wymiany między graczami.

Jak robia to duze MMO:

- `FFXIV` ma combat jobs oraz pełne ścieżki `Disciples of the Hand` i `Disciples of the Land`;
- `WoW` od lat używa professions jako realnego segmentu gearu, consumables i ekonomii;
- `ESO` daje szerokie crafting lines;
- `OSRS` i `BDO` dokrecaja life-skill obsession, timing, przetwarzanie i ekonomiczny sens długiego grindu.

Brak:

- brak profession XP runtime;
- brak profession mastery fantasy;
- brak workshop identity;
- brak crafter prestige;
- brak regional supply pressure;
- brak profession-first chase items;
- brak realnych sinkow i marz.

Dlaczego to boli gracza:

Profesje nie sa jeszcze drugim zyciem postaci. Sa tablica informacyjna o tym, czym moglyby byc.

Cios naprawczy:

Wybrac jeden kompletny profession loop end-to-end i dowiezc go w runtime, zamiast utrzymywac szeroki katalog profesji jako katalog obietnic.

### 5. Validator dowodzi kontraktu, nie jakosci przezycia

Co obiecujemy:

- character sector wydaje sie „usztywniony”, bo `validate_character_sector.js` przechodzi.

Co faktycznie istnieje:

- validator sprawdza glownie statyczne kontrakty: pliki, referencje, brak duplikatow, canonical IDs, starter safety, quarantine legacy;
- sam contract mowi, ze runtime access i full player-path proof wymagaja osobnego harnessu;
- wiele systemow nadal legalnie przechodzi walidacje mimo stanu `pending runtime hook`.

Jak robia to duze MMO:

- top MMORPG wygrywają nie tym, że YAML się zgadza, tylko tym, że loop jest odporny, czytelny i natychmiast sprawdzalny przez gracza.

Brak:

- brak proofu `fresh player path` dla calej tozsamosci postaci;
- brak runtime verification dla race passives;
- brak runtime verification dla profession XP;
- brak runtime verification dla skill roster equip;
- brak runtime verification dla mastery/subclass gates.

Dlaczego to boli gracza:

Zespol moze mylic „kontrakt jest uporzadkowany” z „system jest dobry”. To najgrozniejszy rodzaj falszywego done.

Cios naprawczy:

Zbudowac osobny harness `player-path proof` dla sektora postaci i traktowac validator statyczny jako bramkę higieny, nie jakości produktu.

## A-Tier Braki

To sa braki, ktore nie zabijaja fundamentu, ale mocno obnizaja klase produktu wzgledem benchmarku.

### 6. Onboarding trywializuje wage wyboru klasy

Co obiecujemy:

- docs mowią o `Level 1-10` jako fazie testowania stylu;
- onboarding ma uczyc przez gre, bez ściany tekstu;
- klasa ma wynikać z praktyki i świadomej decyzji.

Co faktycznie istnieje:

- live flow to `rasa -> menu style test -> rekomendacja -> confirm`;
- `foundation_style_test.yml` jest krotka diagnozą kliknięcia, nie gameplayowym sprawdzianem;
- contract sam przyznaje, że to nie jest jeszcze otwarty bracket z realnym mastery analytics;
- recommendation precedence to po prostu ostatnio wybrana próba.

Jak robia to duze MMO:

- `FFXIV` i `WoW` bardzo szybko ustalają role fantasy przez praktyczne granie;
- `GW2` i `ESO` wcześnie uczą, czym postać gra, a nie tylko co mówi o sobie UI.

Brak:

- brak realnej próby klasy przed decyzją;
- brak micro-combat encounter per style;
- brak feedbacku z wyników walki;
- brak ciężaru wyboru wynikającego z przeżytego gameplayu.

Dlaczego to boli gracza:

Wybór klasy wygląda na ważny w copy, ale nie jest jeszcze ciężki w rękach gracza.

Cios naprawczy:

Zamienic menu-driven style test w 5 krótkich praktycznych prób bojowych z czytelnym feedbackiem, nawet jeśli całość nadal kończy się tym samym confirm UI.

### 7. Skill roster i mastery są nadal bardziej kontraktem niż build expression

Co obiecujemy:

- docs rozpisuja `starter -> class -> subclass -> upgrades -> mastery nodes`;
- `foundation_skill_roster.yml` sprzedaje kategorie skilli i zasady loadoutu.

Co faktycznie istnieje:

- samo menu mówi `selection contract, not runtime skill equip` oraz `Runtime hook: pending`;
- generic skille są na razie bardziej deklaracją slotu niż odczuwalnym systemem buildowym;
- mastery jest dobrze opisane w docs, ale słabo osadzone w żywej progresji.

Jak robia to duze MMO:

- `GW2` i `ESO` pozwalają budować postać przez realny wybór aktywnych i pasywnych linii;
- `WoW` i `FFXIV` jasno pokazują, czym różni się core kit od rosnącej specjalizacji.

Brak:

- brak realnego equip/unlock flow dla generic skills;
- brak profession skill runtime;
- brak mastery feedback loop;
- brak upgrade branch visibility;
- brak build pressure od „musisz z czegoś zrezygnować”.

Dlaczego to boli gracza:

Gracz czyta o buildcraftingu więcej, niż go doświadcza.

Cios naprawczy:

Dowiezc pierwszy realny `skill/loadout/mastery` loop z ograniczeniami i widocznym kosztem wyboru, nawet jeśli obejmie tylko 2 klasy i 2 generic sloty.

### 8. Itemizacja ma dobra rame, ale za malo obsesji

Co obiecujemy:

- bardzo sensowna architektura slotów, source contracts i early-act item rules;
- `default_mmoinventory.yml` daje czytelny i projektowy layout EQ;
- docs dobrze odrzucają „one dungeon = one class”.

Co faktycznie istnieje:

- aktywny pool foundation jest schludny i kontrolowany;
- ale chase na itemach jest jeszcze za slabo klasowo-profesyjne i za malo source-defining;
- unikalne artefakty i off-handy istnieja bardziej jako slot policy niz jako silne marzenie buildowe.

Jak robia to duze MMO:

- `WoW` daje szybki odczyt „ten item jest dla mojego gameplayu”;
- `PoE`-owy duch, na który się powołujecie, działa przez obsesję na synergiach i wyjątkach;
- `Lost Ark` i `BDO` trzymają gracza w item chase przez długaśną pętlę ulepszeń, źródeł i kosztów.

Brak:

- za mało build-defining drops;
- za mało source bragging rights;
- za mało profession-crafted aspiration items;
- za mało early iconic weapon fantasies;
- za mało „chcę ten konkretny przedmiot”, za dużo „mam poprawną bazę”.

Dlaczego to boli gracza:

System jest rozsądny, ale jeszcze nie jest uzależniający. Dobre MMO nie tylko pozwala założyć item. Ono każe go chcieć.

Cios naprawczy:

Zaprojektować małą, ale brutalnie czytelną serię `chase items` dla pierwszego aktu: po jednym marzeniu na klasę, jedną profesyjną aspirację i jeden unikat, który realnie zmienia styl gry.

### 9. First equipment loop jest bezpieczny, ale jeszcze nie premium

Co obiecujemy:

- slusznie odcięliście auto-grant z klasy;
- quartermaster i city-acquired first equipment są dobrą odpowiedzią systemową.

Co faktycznie istnieje:

- loop jest poprawny kontraktowo;
- ale nadal jest bardziej procesowy niż emocjonalny;
- pierwsze wyposażenie nie buduje jeszcze sceny „oto twoja droga”, tylko scenę „oto bezpieczny onboarding systemu”.

Jak robia to duze MMO:

- duże MMO wcześnie robią z pierwszego sprzętu rytuał, nie tylko krok konfiguracyjny.

Brak:

- brak klasowego ceremoniału odbioru sprzętu;
- brak pierwszego symbolicznego „anchor item”;
- brak połączenia rasa/klasa/profesja z pierwszym miastem i mentorem.

Dlaczego to boli gracza:

Pierwszy sprzęt działa, ale nie zostawia śladu. To błąd w grze, która chce żyć fantasy postaci.

Cios naprawczy:

Nadać first-equipment loopowi formę rytuału: mentor, quartermaster, jedna linia wyboru, jeden przedmiot-sygnet i jeden klarowny kierunek następnego upgrade'u.

## B-Tier Braki

To są warstwy premium, o których łatwo zapomnieć, a które w dużych MMORPG budują retencję, tożsamość i feeling „żywego świata”.

### 10. Brakuje spójnego social glue wokół postaci

Brak:

- reputacji powiązanych z klasą, rasą albo profesją;
- frakcji i dzielnic zawodowych z realnym znaczeniem;
- civic identity miasta dla konkretnych archetypów;
- account-vs-character progression fantasy;
- guild overlap z craftingiem i gear service;
- kosmetycznego chase powiązanego z profesją albo rasą.

Dlaczego to boli gracza:

Postać istnieje głównie w walce i menu. W dużym MMO postać istnieje też w mieście, ekonomii, społeczności i prestiżu.

Cios naprawczy:

Połączyć character sector z jednym `reputation + district + service` loopem, zamiast trzymać go jako czysto mechaniczny slice.

### 11. Storage i loadout maja dobrą politykę bezpieczeństwa, ale za mało wygody premium

Brak:

- brak pełnego runtime safe-zone/combat-lock UX;
- brak wyraźnego account convenience fantasy;
- brak materiałowego przepływu, który skraca friction w profesjach;
- brak „build preparation area” odczuwalnego jako miejsce, nie tylko reguła.

Dlaczego to boli gracza:

System nie oszukuje, ale też jeszcze nie rozpieszcza. Top MMO wygrywają też jakością codziennej wygody.

Cios naprawczy:

Dowiezc jeden kompletny hub preparation node: storage, salvage, forge, loadout i profession handoff w jednym czytelnym przepływie.

### 12. Za malo prestizu, kolekcji i dlugiego ogona

Brak:

- rasa/class cosmetics;
- title chase;
- trofea profesyjne;
- kolekcje dla off-handów, reliktów i artefaktów;
- companion/minion relevance dla identity postaci;
- visual prestige dla subclass path.

Dlaczego to boli gracza:

Bez tego postać jest funkcjonalna, ale nie legendarna. A wielkie MMO zyja z tego, ze gracz chce byc rozpoznawalny, nie tylko skuteczny.

Cios naprawczy:

Wprowadzic w foundation minimum jedną oś prestiżową per sektor: kosmetyka klasy, trofeum profesji i wizualny znacznik rasy albo subclass path.

## Sekcja Specjalna: O czym najlatwiej zapomniec

Jesli ten sektor ma dojrzec do poziomu „prawdziwe MMO”, backlog nie moze konczyc sie na klasach, rasach i itemach. Najwieksze przeoczenia, ktore juz teraz trzeba miec na radarze:

- `material source readability`
  Gracz musi wiedziec nie tylko co istnieje, ale skąd to realnie bierze.
- `profession prestige`
  Crafter musi byc kimś, a nie tylko posiadaczem recept.
- `NPC relationship layer`
  Mentor, kwatermistrz, mistrz profesji i frakcja powinni byc twarzami systemu, nie tylko menu.
- `cross-system identity`
  Rasa, klasa i profesja powinny czasem ze sobą rezonować, a nie żyć osobno.
- `PvP consequence sanity`
  Jeśli sektor ma brać PvP serio, to profesje, itemy i sustain nie mogą później rozjechać combat foundation.

## Priorytet Ciec

Jesli mam to pociśnąć bez dyplomacji, kolejność najgroźniejszych braków jest taka:

1. `Rasy nie istnieją jeszcze jako system.`
2. `Klasy są lepiej napisane niż zmechanizowane.`
3. `Subclass progression nie żyje jeszcze w rękach gracza.`
4. `Profesje nie są jeszcze równorzędnym filarem gry.`
5. `Onboarding za szybko redukuje wybór do menu.`
6. `Skill/mastery/loadout expression jest słabsze niż jego dokumentacja.`
7. `Itemizacja jest poprawna, ale jeszcze nie natrętnie pożądana.`
8. `Validator za łatwo może udawać dojrzałość systemu.`

## Najważniejszy Wniosek

Najwieksze MMO nie wygrywaja tym, ze maja „wiecej”. Wygrywaja tym, ze bardzo szybko przekuwaja fantasy w rytm gry, a rytm gry w dlugoterminowy powod do powrotu.

U was ten sektor jest juz dobrze uporzadkowany koncepcyjnie, ale za czesto konczy na granicy:

- `tozsamosc jest opisana, ale nie przezyta`;
- `system jest zabezpieczony, ale nie pozadany`;
- `kontrakt jest gotowy, ale loop jeszcze nie zyje`.

To nie jest problem kosmetyczny. To jest centralny problem produktu.

## Repo Evidence

Najmocniejsze lokalne sygnaly, na ktorych stoi ten teardown:

- `MCMMORPG/docs/character-sector-contract.md`
- `MCMMORPG/_validation/validate_character_sector.js`
- `MCMMORPG/plugins/CoreTools/MenuCreator/foundation_race_selector.yml`
- `MCMMORPG/plugins/CoreTools/MenuCreator/foundation_style_test.yml`
- `MCMMORPG/plugins/CoreTools/MenuCreator/foundation_skill_roster.yml`
- `MCMMORPG/plugins/CoreTools/MenuCreator/foundation_profession_roster.yml`
- `MCMMORPG/plugins/CoreTools/Scripts/foundation_character_flow.yml`
- `MCMMORPG/plugins/MMOCore/classes/warrior.yml`
- `MCMMORPG/plugins/MMOCore/classes/rogue.yml`
- `MCMMORPG/plugins/MMOCore/classes/marksman.yml`
- `MCMMORPG/plugins/MMOCore/classes/mage/mage.yml`
- `MCMMORPG/plugins/MMOCore/classes/paladin.yml`
- `MCMMORPG/plugins/MMOInventory/inventory/default_mmoinventory.yml`
- `docs/class-subclass-foundation-v0.0.1.md`
- `docs/starter-skills-class-progression-foundation-v0.0.1.md`
- `docs/professions-foundation-v0.0.1.md`
- `docs/itemization-foundation-v0.0.1.md`
- `docs/skill-trees-passives-upgrades-foundation-v0.0.1.md`
- `docs/progression-respec-foundation-v0.0.1.md`
- `docs/onboarding-tutorial-foundation-v0.0.1.md`
- `docs/loadout-bank-storage-foundation-v0.0.1.md`

## Benchmark Sources

Oficjalne strony uzyte do benchmarku:

- World of Warcraft classes: https://worldofwarcraft.blizzard.com/en-us/game/classes
- World of Warcraft races: https://worldofwarcraft.blizzard.com/en-us/game/races
- FINAL FANTASY XIV Job Guide: https://na.finalfantasyxiv.com/jobguide/battle/
- FINAL FANTASY XIV Lodestone item and discipline database: https://na.finalfantasyxiv.com/lodestone/playguide/db/item/
- Guild Wars 2 professions: https://www.guildwars2.com/en/the-game/professions/
- Guild Wars 2 races: https://www.guildwars2.com/en/the-game/races/
- The Elder Scrolls Online classes: https://www.elderscrollsonline.com/en-us/classes
