# Player Journey & Milestone Roadmap v0.0.1

## Cel dokumentu

Ten dokument laczy fundamenty serwera MCMMORPG w jedna grywalna sciezke gracza od startu do endgame.

Celem jest ustalic, co gracz robi na konkretnych etapach levelowania, jakie systemy poznaje, jakie unlocki dostaje i kiedy gra zaczyna wymagac realnych decyzji buildowych.

Dokument bazuje na modelu:

- Level 1-10: starter style testers.
- Level 10: wybor klasy bazowej.
- Level 25: wybor podklasy.
- Level 50: upgrade wybranej podklasy.
- Level 50+: endgame loop.

To nadal etap projektowy. Nie definiujemy finalnych wartosci XP, drop rate, cen, cooldownow ani plugin configow.

## Journey Principles

Zasady prowadzenia gracza:

- Kazdy milestone musi dawac realna zmiane gameplayu, nie tylko wieksza liczbe.
- Nowy system powinien byc pokazany przez quest, aktywnosc albo NPC, zanim stanie sie wymagany.
- Gracz solo musi miec sensowna sciezke progresji.
- Party content powinien dawac lepsza synergie i rewardy, ale nie blokowac calej gry.
- PvP powinno byc wprowadzane stopniowo, najpierw jako optional contested content.
- Respec i loadout powinny pojawic sie przed ciezkimi decyzjami buildowymi.
- Gear, mastery, staty i skille musza byc tlumaczone w praktyce, nie tylko w menu.
- Kazdy etap powinien przygotowywac do nastepnego progu wyboru.

## Level 1-10: Starter Phase

Cel etapu:

- nauczyc podstaw walki,
- dac testery stylow gry,
- pokazac pierwsze bronie i skille,
- dac pierwsze mastery sygnaly,
- przygotowac gracza do wyboru klasy na Level 10.

Systemy wprowadzane:

- basic attack,
- heavy attack,
- dodge/dash,
- block/guard,
- pierwsze starter skille,
- pierwsze weapon differences,
- pierwsze stat points,
- pierwsze item tags,
- pierwszy prosty crafting/repair,
- podstawowy death penalty,
- safe zone i early PvE zone.

Aktywnosci:

- tutorial combat quest,
- weapon tryout quest,
- starter skill trial,
- first field mobs,
- first mini elite,
- starter crafting task,
- first vendor/bank/loadout explanation.

Milestone rewards:

- pierwsze stat points,
- starter skill slots,
- starter gear,
- pierwszy loadout albo jego tutorial,
- unlock class choice quest chain.

Design notes:

- Gracz nie powinien miec wszystkich starterow naraz na hotbarze.
- Startery maja testowac style, nie tworzyc finalny build.
- System moze sledzic uzywane startery, zeby zasugerowac klase na Level 10.

Success criteria:

- Gracz rozumie, czy woli melee, range, spell, stealth, support, summon, trap albo defense.
- Gracz wie, ze Level 10 to wybor klasy.
- Gracz nie jest jeszcze karany za eksperymentowanie.

## Level 10: Base Class Choice

Cel etapu:

- gracz wybiera jedna z 5 klas bazowych,
- starter phase przechodzi w prawdziwy class kit,
- gracz dostaje pierwszy realny skok identity.

Klasy:

- Wojownik.
- Lotrzyk.
- Lowca.
- Mag.
- Akolita.

Class choice powinien byc zrobiony przez:

- class mentor quest,
- krotki class trial,
- podsumowanie uzywanych starterow,
- mozliwosc przeczytania roli PvE/PvP klasy,
- ostrzezenie, ze hybrydy sa mozliwe, ale najmocniejsze efekty wymagaja inwestycji.

Milestone rewards:

- core class kit,
- class passive,
- pierwszy class quest,
- jasny kierunek stat/mastery,
- dostep do class trainer/mentor,
- respec tutorial dla wczesnych pomylek.

Success criteria:

- Gracz rozumie, czym rozni sie jego klasa od starterow.
- Gracz wie, jakie staty i mastery wspieraja jego wybor.
- Gracz wie, ze podklasa bedzie na Level 25.

## Level 10-25: Core Class Development

Cel etapu:

- rozwinac core class kit,
- pokazac pierwsze pasywki,
- pokazac pierwszy dungeon,
- wprowadzic realne item tags i crafting choices,
- przygotowac do wyboru podklasy.

Systemy wprowadzane:

- class skill upgrades,
- class passive,
- mastery nodes,
- item requirements,
- rarity i affix basics,
- first meaningful crafting/upgrading,
- loadouts,
- partial respec,
- first party content.

Aktywnosci:

- class quests,
- first dungeon,
- first dungeon boss,
- region questline,
- first faction contact,
- first world event PvE,
- optional duel/arena tutorial bez duzego ryzyka.

Milestone rewards:

- additional skill slot albo upgrade slot,
- first passive point,
- first dungeon access,
- first crafting recipe,
- first reputation unlock,
- improved gear base.

Design notes:

- Gracz powinien zobaczyc roznice miedzy gear dla swojego buildu a losowym gear score.
- First dungeon powinien testowac combat loop: block, dodge, interrupt, cleanse/ward albo trap/line of sight zalezne od party.
- PvP na tym etapie powinno byc opcjonalne i niskiego ryzyka.

Success criteria:

- Gracz rozumie core loop swojej klasy.
- Gracz rozumie podstawy item tags i mastery.
- Gracz przed Level 25 zna roznice miedzy trzema podklasami swojej klasy.

## Level 25: Subclass Choice

Cel etapu:

- gracz wybiera jedna z 3 podklas swojej klasy,
- dostaje signature skill,
- dostaje subclass passive,
- build zyskuje konkretny styl i tradeoff.

Subclass choice powinien byc zrobiony przez:

- subclass trial,
- role preview,
- signature skill preview,
- pokazanie mocnych i slabych stron,
- wymaganie mastery/stat/quest, ale bez przesadnego grindu.

Trial design:

- trial musi testowac gameplay, nie tylko level,
- Straznik trial testuje ochrone i block,
- Zabojca trial testuje stealth/backstab,
- Strzelec trial testuje aim/line of sight,
- Piromanta trial testuje area denial,
- Kapelan trial testuje heal/cleanse/protection,
- Rytualista / Warlock trial testuje summon/curse/drain setup.

Milestone rewards:

- signature skill,
- subclass passive,
- subclass questline,
- access to subclass gear targets,
- deeper mastery unlocks.

Success criteria:

- Gracz czuje prawdziwa specjalizacje.
- Gracz rozumie tradeoff podklasy.
- PvP counterplay podklasy jest jasny.

## Level 25-50: Subclass Development

Cel etapu:

- rozwinac podklase,
- wprowadzic trudniejsze dungeony,
- wprowadzic contested zones,
- pokazac reputacje, target farming i bardziej zlozony crafting,
- przygotowac do Level 50 upgrade.

Systemy wprowadzane:

- subclass skill upgrades,
- deeper mastery nodes,
- reputacje,
- target farming,
- rare/epic gear,
- unique item previews,
- PvP contested objectives,
- guild introduction,
- advanced respec/loadout rules.

Aktywnosci:

- mid-game dungeon chain,
- first serious boss,
- first contested zone,
- first PvP objective,
- faction reputation loop,
- crafting/upgrading tutorial,
- guild onboarding quest,
- subclass story quest.

Milestone rewards:

- passive/upgrade points,
- dungeon keys,
- faction vendor unlocks,
- target farm hints,
- PvP currency intro,
- guild content access,
- subclass upgrade quest chain start.

Design notes:

- Contested zones powinny dawac lepsze materialy, ale miec jasne ostrzezenie ryzyka.
- PvP nie moze opierac sie na griefowaniu slabszych graczy.
- Gear progression powinien zaczac wymagac swiadomych wyborow affix/tag.
- Warlock powinien na tym etapie czuc pelny summon/curse/drain loop, ale nadal miec counterplay.

Success criteria:

- Gracz wie, skad brac gear pod build.
- Gracz wie, co farmic pod podklase.
- Gracz rozumie roznice miedzy safe PvE, dungeonem i contested zone.

## Level 50: Subclass Upgrade

Cel etapu:

- gracz rozwija wybrana podklase w advanced specialization,
- nie dostaje drugiej pelnej podklasy,
- build dostaje endgame identity.

Unlock powinien wymagac:

- Level 50,
- mastery zwiazanego z podklasa,
- quest chain albo advanced trial,
- dungeon/boss albo PvP objective zalezne od stylu,
- kosztu ekonomicznego/craftingowego dla sinku.

Przyklady:

- Rytualista / Warlock wybiera Demonolog albo Klatwomistrz.
- Kapelan wybiera Hierophant albo Saint.
- Berserker wybiera Ravager albo Bloodrager.
- Piromanta wybiera Infernalist albo Ashcaller.

Milestone rewards:

- upgrade passive,
- upgrade identity,
- access to endgame target farming,
- advanced crafting/upgrading unlock,
- endgame reputation objectives.

Design notes:

- Level 50 upgrade nie moze usunac kontr PvP.
- Demonolog ma mocniejsze summony, ale owner nadal jest realnym celem.
- Klatwomistrz ma mocniejsze curse/drain, ale cleanse/interrupt nadal dziala.
- Tank upgrade nadal ma nizszy kill pressure albo jasne kontry.

Success criteria:

- Gracz czuje endgame identity.
- Build jest mocniejszy, ale nadal kontrowalny.
- Upgrade jest naturalnym rozwinieciem podklasy, nie nowa pelna klasa.

## Level 50+: Endgame Loop

Cel etapu:

- dac kilka rownoleglych drog progresji,
- utrzymac sens PvE, PvP, guild, crafting i reputacji,
- uniknac jednej obowiazkowej farmy.

Endgame activities:

- boss target farming,
- challenge dungeons,
- PvP ranking,
- guild wars,
- contested world events,
- reputation maxing,
- crafting/upgrading,
- unique item builds,
- seasonal goals,
- cosmetic prestige.

Reward types:

- rare/epic/unique gear,
- crafting materials,
- dungeon/boss tokens,
- PvP currency,
- guild materials,
- reputation unlocks,
- cosmetics,
- build-defining but tradeoff-based unique items.

Design notes:

- PvE gracz ma co robic bez wymuszonego PvP.
- PvP gracz ma PvP progression, ale nie moze ignorowac wszystkich fundamentow gear/build.
- Guild wars sa endgame grupowym, ale nie moga zabetonowac serwera.
- Economy sinks musza usuwac walute i materialy.
- PvP soft capy musza ograniczac one-shot meta.

Success criteria:

- Gracz ma kilka drog dalszego rozwoju.
- Gear advantage istnieje, ale nie kasuje skill expression.
- Endgame wzmacnia buildy, nie tylko liczby.

## System Introduction Timeline

Minimalna kolejnosc wprowadzania systemow:

- Level 1: movement, attack, dodge, starter skill.
- Level 2-3: block/guard, heavy attack, first item stats.
- Level 4-5: crafting/repair basics, first mastery explanation.
- Level 6-9: starter skill experimentation, first elite, first simple event.
- Level 10: class choice, class kit, class passive.
- Level 12-15: class skill upgrades, first item tags.
- Level 15-20: first dungeon and party utility.
- Level 20-24: subclass previews and trial prep.
- Level 25: subclass choice, signature skill, subclass passive.
- Level 30+: reputations, target farming, contested zones.
- Level 40+: advanced dungeons, stronger crafting/upgrading, guild onboarding.
- Level 50: subclass upgrade.
- Level 50+: endgame loops.

## Warlock Example Journey

Przyklad sciezki Rytualisty / Warlocka:

- Level 1-10: gracz testuje Summon Tester i Debuff Tester.
- Level 10: wybiera Akolite, dostaje heal/ward/debuff baseline oraz Lesser Rite.
- Level 10-25: uczy sie laczyc sustain z debuffem i slabym summon/ritual utility.
- Level 25: wybiera Rytualiste / Warlocka, dostaje Pact of the Hollow i pelny summon/curse/drain loop.
- Level 25-50: rozwija Chaos mastery, curse uptime, summon control i drain sustain.
- Level 50: wybiera Demonologa albo Klatwomistrza.
- Level 50+: farmi gear pod Minion/Curse/Chaos tags, ale nadal ma kontry: cleanse, kill summon, interrupt, owner pressure.

## Test Cases And Scenarios

Player Journey & Milestone Roadmap v0.0.1 powinno przejsc ponizsze scenariusze:

- Nowy gracz na Level 10 rozumie, ktora klase chce wybrac i dlaczego.
- Gracz przed Level 25 rozumie roznice miedzy trzema podklasami swojej klasy.
- Subclass trial sprawdza gameplay, nie tylko level.
- Level 50 upgrade rozwija wybrana podklase, nie daje druga pelna podklase.
- Pierwszy dungeon testuje combat, defensywy i party utility.
- Pierwsza contested zone uczy ryzyka PvP bez griefowania nowych graczy.
- Gracz rozumie, skad brac gear pod swoj build.
- Respec/loadout pojawia sie zanim gracz podejmuje najciezsze decyzje buildowe.
- Warlock ma logiczna sciezke: summon/debuff tester -> Akolita -> Rytualista / Warlock -> Demonolog albo Klatwomistrz.
- Endgame ma kilka drog progresji, a nie jedna obowiazkowa farme.

## Powiazania z fundamentami

Ten dokument musi pozostac spojny z:

- docs/combat-foundation-v0.0.1.md.
- docs/pvp-foundation-v0.0.1.md.
- docs/reputation-faction-foundation-v0.0.1.md.
- docs/onboarding-tutorial-foundation-v0.0.1.md.
- docs/guilds-foundation-v0.0.1.md.
- docs/seasons-prestige-cosmetics-foundation-v0.0.1.md.
- docs/loadout-bank-storage-foundation-v0.0.1.md.
- docs/world-events-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.
- docs/progression-respec-foundation-v0.0.1.md.
- docs/world-content-loop-foundation-v0.0.1.md.
- docs/economy-crafting-loot-foundation-v0.0.1.md.
- docs/dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md.
- docs/stolica-wyspy-hub-foundation-v0.0.1.md.
- docs/level-1-dungeon-island-foundation-v0.0.1.md.
- docs/quest-contract-objective-foundation-v0.0.1.md.
- docs/discovery-npc-board-loop-001-foundation-v0.0.1.md.
- docs/class-subclass-foundation-v0.0.1.md.
- docs/starter-skills-class-progression-foundation-v0.0.1.md.
- docs/class-skill-kits-foundation-v0.0.1.md.
- docs/skill-trees-passives-upgrades-foundation-v0.0.1.md.

W szczegolnosci:

- Level model zostaje 1-10 / 10 / 25 / 50 / 50+.
- PvP jest wprowadzane stopniowo i optional na early/mid game.
- Subclass trials musza testowac realny gameplay.
- Gear, mastery, economy i world content musza wspierac buildy.
- Endgame nie moze usuwac counterplayu.

## Assumptions

- Dokument jest projektowy, bez plugin configow.
- Nie definiujemy finalnych XP values, drop rate, cen ani liczbowych rewardow.
- Nazwy questow, regionow i bossow beda osobnym etapem.
- Ten dokument bedzie baza pod konkretne questy, regiony, dungeony i MVP serwera.
