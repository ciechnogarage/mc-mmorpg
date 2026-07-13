# Quest, Contract & Dungeon Objective Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje lekkie questy, zlecenia i objective'y contentowe dla
serwera MCMMORPG. Questy nie sa glowna fabularna kampania. Sa gameplayowym
wrapperem, ktory prowadzi gracza do konkretnego contentu: mobow, dungeonow,
dropow, materialow, profesji, bossow, bounty i aktywnosci.

Przyklad docelowej logiki:

- quest: zbierz 10 klow wilka,
- source: quest board albo NPC,
- content source: `loch_001`,
- target: wilki w `loch_001`,
- reward: XP, gold, material, token, reputation albo starter item.

Quest ma wskazywac, gdzie i po co grac, ale nie ma zastepowac samej gry.

Dokument jest projektowy. Nie definiujemy jeszcze finalnego pluginu questowego,
quest ID w konfiguracji, drop rate, reward numbers ani dialogow NPC.

## Core Model

Questy i objective'y maja byc krotkie, czytelne i oparte o aktywnosc.

Zasady:

- quest prowadzi do contentu, nie jest contentem samym w sobie,
- cel musi jasno mowic, co zrobic i gdzie,
- drop quest musi mowic, z jakiego moba albo regionu pochodzi item,
- dungeon objective ma byc czescia runu albo opcjonalnym celem,
- achievementy sa osobnym systemem pasywnym, nie glowna progresja,
- repeatable questy nie moga stac sie jedna najlepsza farma,
- rewardy musza wspierac ekonomie, loot i progresje bez inflacji.

## Typy aktywnosci

### Quest / Zlecenie

Rola:

- lekkie zadanie z NPC, boarda albo huba,
- prowadzi gracza do konkretnego content source,
- daje nagrode i uczy target farmingu.

Przyklady:

- zbierz 10 klow wilka z wilkow w `loch_001`,
- zabij 12 corrupted roots w `Level 1 Dungeon Island`,
- przynies boss token od `GroveGuardian`.

### Dungeon Objective

Rola:

- cel wewnatrz dungeon runu,
- moze byc glowny albo poboczny,
- urozmaica run bez robienia fabularnej kampanii.

Przyklady:

- aktywuj 3 pieczecie,
- obron obelisk przez 60 sekund,
- przerwij rytual channelerow,
- otworz brame przez pokonanie elite gatekeepera,
- pokonaj bossa.

### Bounty

Rola:

- zlecenie na elite moba, mini-bossa, boss family albo konkretnego przeciwnika,
- dobre dla target farmingu i ryzykowniejszego contentu.

Przyklady:

- zabij elite brute w `loch_001`,
- pokonaj rare spawn,
- przynies trophy z mini-bossa.

### Profession Order

Rola:

- zlecenie powiazane z craftingiem, gatheringiem albo salvage,
- uczy profesji bez wymuszania ich jako glownego contentu.

Przyklady:

- dostarcz 5 ziol z dungeon side area,
- wykonaj 3 proste antidota,
- przynies 2 skory z mobow,
- rozbij 1 niepotrzebny item i oddaj odzyskany fragment.

### Daily / Weekly

Rola:

- powtarzalny wrapper dla aktywnosci,
- pozniejszy system retencji i kierowania ruchu na mapie.

Zasady:

- nie moze byc obowiazkowa lista chore'ow,
- nie moze byc najlepszym zrodlem wszystkiego,
- powinien rotowac aktywnosci i rewardy.

### Achievement

Rola:

- pasywne sledzenie milestone'ow,
- tytuly,
- kosmetyki,
- prestige,
- stat tracking.

Nie powinno:

- blokowac progresji,
- mylic sie z questem,
- byc glownym zrodlem XP albo gearu.

## Quest Data Shape

Kazdy quest albo zlecenie powinno miec minimalny opis danych.

Pola:

- `id`: stabilne ID, np. `quest_loch001_wolf_fangs`.
- `display_name`: nazwa dla gracza.
- `source`: NPC, quest board, profession board, dungeon board, Portal Nexus.
- `content_source`: gdzie wykonac cel, np. `loch_001`, `Level 1 Dungeon Island`.
- `objective_type`: kill, collect, gather, activate, defend, boss, craft, return.
- `target`: mob, material, boss, object, station albo NPC.
- `required_amount`: liczba wymagana.
- `reward`: XP, gold, item, material, token, reputation, recipe.
- `repeat_rule`: one-time, repeatable, daily, weekly.
- `unlock_rule`: level, previous quest, dungeon unlock, profession level,
  reputation.
- `party_credit_rule`: jak party dostaje credit.
- `anti_abuse`: eligibility, contribution, no AFK credit, no alt farming.

Minimalna zasada:

- gracz musi wiedziec, gdzie znalezc target i co z nim zrobic.

## Reward Rules

Rewardy powinny wspierac progresje, ale nie zastapic lootu.

Typy rewardow:

- XP,
- gold,
- material,
- token/progress,
- starter gear,
- consumable,
- recipe unlock,
- profession XP,
- reputation,
- cosmetic/prestige w pozniejszym etapie.

Zasady:

- quest reward nie powinien przebijac normalnego loot loopu,
- drop quest powinien pomagac target farmingowi,
- boss quest moze dawac pity/token progress,
- profession order moze dawac profession XP albo material,
- daily/weekly musza miec limity i nie moga byc najlepsza farma,
- PvP questy wymagaja anti-abuse.

## Party Credit

Questy i objective'y musza dzialac solo i w party.

Zasady:

- credit za kill/drop wymaga realnego udzialu,
- gracz AFK nie powinien dostac pelnego creditu,
- party member w rozsadnym zasiegu moze dostac credit,
- drop quest moze miec personal drops, zeby uniknac konfliktu party,
- boss token powinien byc personal albo jasnie okreslony,
- repeatable rewardy musza miec zabezpieczenie przed alt farmingiem.

## Level 1 Examples

Przyklady dla `loch_001` / `Level 1 Dungeon Island`.

### Kly z Lochu 001

Typ:

- Drop Quest.

Cel:

- zbierz 10 klow wilka.

Source:

- Quest Board w Stolicy Wyspy albo NPC przy Portal Nexus.

Content source:

- `loch_001`.

Target:

- wolf-type mobs w `loch_001`.

Reward:

- XP,
- gold,
- low-tier material,
- opcjonalnie progress do levelu 10.

### Oczysc Sciezke

Typ:

- Kill Quest.

Cel:

- zabij 12 weak mobs w `Level 1 Dungeon Island`.

Reward:

- XP,
- common material,
- starter consumable.

### Przerwany Rytual

Typ:

- Dungeon Objective / Combat Lesson.

Cel:

- przerwij 3 channele casterow albo channelerow.

Reward:

- XP,
- small token/progress,
- tutorial credit dla interruptu.

### Korzenie Straznika

Typ:

- Gather / Drop Quest.

Cel:

- zbierz 5 corrupted roots z side area albo mobow.

Reward:

- material do Alchemii, Krawiectwa albo Zaklinania,
- opcjonalny profession XP.

### Pierwszy Salvage

Typ:

- Profession Order / Hub Return Task.

Cel:

- rozbij 1 niepotrzebny item po powrocie do Stolicy Wyspy.

Reward:

- material fragment,
- wyjasnienie item sinku,
- maly XP/progress.

### Pierwsze Ulepszenie

Typ:

- Crafting / Upgrade Task.

Cel:

- uzyj materialu z dungeonu do prostego upgrade.

Reward:

- upgrade tutorial complete,
- small XP/progress.

### Straznik Gaju

Typ:

- Boss Quest.

Cel:

- pokonaj `GroveGuardian` / `Straznik Gaju`.

Reward:

- boss token albo pity progress,
- XP,
- material,
- chance na early magic/rare item.

### Raport do Portalu

Typ:

- Return Quest.

Cel:

- wroc do NPC przy Portal Nexus albo Quest Board po clearze.

Reward:

- domkniecie runu,
- wskazanie repair/salvage/stash/crafting,
- progress do kolejnych aktywnosci.

## Design Rules

Zasady projektowania questow:

- quest ma byc jasny w jednym zdaniu,
- content source ma byc jawny,
- target ma byc jawny,
- reward ma byc proporcjonalny,
- quest nie moze wymagac wiki,
- quest nie powinien blokowac dungeonu, jesli jest poboczny,
- questy early game maja uczyc systemow,
- questy nie maja udawac wielkiej fabuly,
- fabularne teksty moga byc krotkim kontekstem, nie sednem systemu.

## Integration

Ten dokument musi pozostac spojny z:

- docs/stolica-wyspy-hub-foundation-v0.0.1.md.
- docs/level-1-dungeon-island-foundation-v0.0.1.md.
- docs/mob-boss-encounter-001-foundation-v0.0.1.md.
- docs/loot-reward-table-001-foundation-v0.0.1.md.
- docs/discovery-npc-board-loop-001-foundation-v0.0.1.md.
- docs/world-content-loop-foundation-v0.0.1.md.
- docs/player-journey-milestone-roadmap-v0.0.1.md.
- docs/professions-foundation-v0.0.1.md.
- docs/economy-crafting-loot-foundation-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.

W szczegolnosci:

- `Stolica Wyspy` zawiera Quest Board, Profession Board, Bounty Board i Portal
  Nexus.
- `Level 1 Dungeon Island` jest pierwszym content source dla questow early game.
- `mob-boss-encounter-001` okresla moby i bossy, ktore sa targetami kill/drop,
  interrupt, bounty i boss objectives.
- `loot-reward-table-001` okresla, z jakich mobow i objective'ow pochodza
  materialy typu wolf fang, corrupted root, weak essence i grove token.
- `discovery-npc-board-loop-001` okresla, jak Quest Board i Profession Board
  pokazuja zlecenia bez robienia z nich liniowego tutorialu.
- `world-content` okresla aktywnosci, ktore questy moga wskazywac.
- `player-journey` okresla progres 1-10 i wybor klasy na levelu 10.
- `professions` okresla gathering, crafting, salvage i profession orders.
- `economy` okresla rewardy, tokeny, pity i anti-inflation.
- `itemization` okresla starter gear, materialy, salvage i upgrade.

## Out Of Scope

Nie robimy jeszcze:

- finalnego quest pluginu,
- konfiguracji questow,
- dialogow NPC,
- finalnych reward values,
- drop rate,
- finalnego achievement systemu,
- pelnych daily/weekly rotacji,
- PvP bounty abuse modelu poza ogolnymi zasadami.

## Test Cases

Quest, Contract & Dungeon Objective v0.0.1 powinien przejsc ponizsze scenariusze:

- Gracz rozumie, gdzie zdobyc item typu `10 klow wilka`.
- Quest wskazuje konkretny content source, np. `loch_001`.
- Dungeon objective da sie wykonac solo i w party.
- Reward nie przebija normalnego lootu ani nie niszczy ekonomii.
- Repeatable quest nie staje sie jedna najlepsza farma.
- Profession order uczy profesji, ale jej nie wymusza.
- Achievement nie myli sie z questem i nie blokuje progresji.
- Questy 1-10 prowadza przez hub, dungeon, loot, salvage i upgrade bez robienia
  z tego wielkiej fabularnej kampanii.
- Party credit wymaga realnego udzialu, nie samego stania AFK.
- Boss quest daje progress bez tworzenia best-in-slot rewardu.

## Assumptions

- Questy sa lekkie i gameplayowe, nie cinematic story chain.
- `loch_001` / `Level 1 Dungeon Island` jest pierwszym content source dla
  questow.
- Achievementy beda osobnym pozniejszym systemem.
- Questy wspieraja target farming, onboarding i reward loop.
- Dokument jest projektowy i nie wdraza jeszcze plugin configow.
