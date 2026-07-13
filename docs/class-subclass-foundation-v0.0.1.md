# Class & Subclass Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje pierwsza wersje klas bazowych i podklas dla serwera MCMMORPG.

Klasy maja wynikac z fundamentow: combat, damage/defense, skill system, itemization, progression/respec, world/content loop oraz economy/crafting/loot. Nie sa tylko nazwami fantasy. Kazda klasa musi miec realny gameplay, sens w PvE solo, wartosc w party, role w PvP i jasny counterplay.

System klas ma wspierac:

- 5 klas bazowych.
- 3 podklasy dla kazdej klasy bazowej.
- Hybrydy z kosztem inwestycji.
- PvE solo bez martwych supportow.
- PvP bez klas bez counterplayu.
- Rozwoj przez staty, mastery, gear i subclass trial.
- Brak nowych typow obrazen poza ustalonymi fundamentami.

To nadal etap projektowy. Nie wdrazamy konfiguracji pluginow, finalnych liczb, cooldownow ani pelnych drzewek skilli.
## Class Rules

Zasady wspolne:

- Klasa bazowa wybierana jest na Level 10.
- Klasa bazowa daje startowy kierunek, nie calkowite wiezienie buildu.
- Podklasa odblokowuje sie na Level 25.
- Podklasa wymaga Level 25, statow, mastery i subclass trial.
- Level 50 odblokowuje upgrade wybranej podklasy, nie druga pelna podklase.
- Kazda klasa musi miec 3-5 core skills i pozniejszy signature skill.
- Kazda podklasa musi miec tradeoff.
- Kazda mocna mechanika musi miec PvP counterplay.
- Support musi umiec grac solo.
- Summony, stealth, healing, CC i burst musza miec osobne PvP sanity rules.
- Hybrydy dozwolone, ale najmocniejsze efekty wymagaja statow, mastery, gearu albo podklasy.

Dozwolone damage types:

- Physical.
- Fire.
- Cold.
- Lightning.
- Chaos.
- Poison.

Nie dodajemy osobnych typow typu Holy, Arcane, Psychic albo Illusion. Klimat klasy musi mapowac sie na istniejace typy obrazen.

## Public Design Interfaces

Kazda klasa bazowa powinna miec:

- Name.
- Role.
- Preferred Weapons.
- Preferred Armor.
- Primary Stats.
- Primary Masteries.
- Resource Model.
- Damage Types.
- Defensive Model.
- PvE Solo Loop.
- Party Value.
- PvP Loop.
- Weaknesses.
- Counterplay.
- Hybrid Notes.

Kazda podklasa powinna miec:

- Name.
- Parent Class.
- Gameplay Identity.
- Unlock Requirements.
- Signature Skill Concept.
- Passive Identity.
- Strong Points.
- Weak Points.
- PvE Role.
- PvP Role.
- Counterplay.
- Gear/Stat Synergy.

Kazdy skill klasowy powinien pozniej korzystac z formatu:

- Name.
- Tags.
- Damage Type.
- Resource Cost.
- Cooldown.
- Cast/Windup.
- Scaling.
- Unlock Requirements.
- PvP Modifier.
- Counterplay.

## Class 1: Wojownik

Wojownik to fizyczna klasa frontline. Bazuje na melee pressure, armor, poise, block, guard, counter i kontroli przestrzeni.

Rola:

- frontline melee,
- tank/bruiser,
- guard pressure,
- anti-ranged engage,
- objective holder.

Preferred weapons:

- one-handed sword,
- two-handed sword,
- axe,
- mace/hammer,
- shield.

Preferred armor:

- heavy armor,
- medium armor.

Primary stats:

- Strength,
- Vitality.

Secondary stats:

- Dexterity dla duel/mobility wariantow,
- Faith tylko dla hybryd defensywnych.

Primary masteries:

- One-Handed,
- Two-Handed,
- Shield.

Damage types:

- Physical.

Defensive model:

- Armor,
- Poise,
- Guard,
- Block Strength,
- Tenacity.

PvE solo loop:

- wejscie w melee,
- block/parry gdy przeciwnik robi mocny atak,
- counter po udanej defensywie,
- heavy attack do guard break,
- sustain przez armor, vitality i kontrolowane zuzycie staminy.

Party value:

- trzymanie aggro/pressure,
- ochrona sojusznikow,
- przerywanie castow,
- kontrola frontu,
- guard break na bossach i tankach.

PvP loop:

- wymusic dodge/block przeciwnika,
- ukarac blad heavy attackiem albo counterem,
- trzymac objective,
- naciskac casterow i low-poise targety.

Weaknesses:

- ograniczony range,
- podatny na kiting,
- slabszy przeciw DoT i magic pressure bez odpowiedniego gearu,
- moze przegrac resource war, jesli spali stamine.

Counterplay:

- kite,
- line of sight,
- DoT,
- guard break,
- flanking,
- magic penetration,
- stamina pressure,
- slows/rooty z diminishing returns.

### Podklasa: Straznik

Identity:

- tank tarczowy,
- ochrona sojusznikow,
- objective holder,
- defensywny frontline.

Unlock requirements:

- Level 25.
- Strength.
- Vitality.
- Shield Mastery.
- Subclass trial oparty o blokowanie, ochrone NPC/party i przetrwanie presji.

Signature skill concept:

- Guardian Bulwark: krotkie defensywne okno, ktore wzmacnia block/guard i czesciowo chroni sojusznikow za Straznikiem.

Strong points:

- wysoka przezywalnosc,
- mocny block,
- ochrona party,
- dobry w objective PvP.

Weak points:

- nizszy kill pressure,
- podatny na guard break,
- podatny na DoT i flankowanie,
- slaba mobilnosc.

PvP counterplay:

- guard break,
- ataki od tylu,
- poison/burn/bleed,
- ignore front shield przez pozycjonowanie,
- odciaganie od objective.

### Podklasa: Berserker

Identity:

- agresywny two-handed bruiser,
- rage,
- burst physical,
- wysokie ryzyko i wysoka presja.

Unlock requirements:

- Level 25.
- Strength.
- Two-Handed Mastery.
- Subclass trial oparty o szybkie eliminowanie fal przeciwnikow bez turtlowania.

Signature skill concept:

- Rage Breaker: krotkie okno zwiekszonego physical pressure i guard damage, ale po oknie Berserker ma slabsza defensywe.

Strong points:

- wysoki melee burst,
- dobry guard break,
- mocny przeciw tankom,
- dobry w PvE cleave.

Weak points:

- gorsza defensywa po nieudanym engage,
- podatny na kite,
- wymaga staminy/rage,
- slabszy przeciw mobile ranged.

PvP counterplay:

- dodge burst window,
- slow/root z diminishing returns,
- kite,
- parry heavy attack,
- punish po rage window.

### Podklasa: Gladiator

Identity:

- duelista,
- parry/counter specialist,
- melee mobility,
- skill expression.

Unlock requirements:

- Level 25.
- Dexterity albo Strength.
- One-Handed Mastery.
- Subclass trial oparty o parry, counter i walki 1v1.

Signature skill concept:

- Perfect Riposte: po udanym parry/counter daje krotkie okno na mocny single-target strike.

Strong points:

- mocny w 1v1,
- nagradza timing,
- dobry przeciw przewidywalnemu melee,
- mobilniejszy niz Straznik.

Weak points:

- mniej tanky,
- slabszy w duzym AoE,
- wymaga dobrego timingu,
- karany za nieudane parry.

PvP counterplay:

- bait parry,
- DoT/AoE,
- ranged pressure,
- delayed attacks,
- nie dawac darmowych counter windows.

## Class 2: Lotrzyk

Lotrzyk to mobilna klasa burst/control. Bazuje na daggerach, stealth, evasion, backstab, poison, traps i flankowaniu.

Rola:

- assassin,
- scout,
- flanker,
- trap/control,
- single-target pressure.

Preferred weapons:

- daggers,
- one-handed sword,
- off-hand utility,
- light crossbow as hybrid option.

Preferred armor:

- light armor.

Primary stats:

- Dexterity.

Secondary stats:

- Intelligence dla poison/trap hybryd,
- Vitality minimalnie dla przezywalnosci.

Primary masteries:

- Daggers,
- Stealth,
- Alchemy / Poison.

Damage types:

- Physical,
- Poison.

Defensive model:

- Evasion,
- mobility,
- stealth,
- avoidance,
- low direct durability.

PvE solo loop:

- podejscie ze stealth,
- backstab/opener,
- poison albo bleed pressure,
- dash disengage,
- reset pozycji,
- kontrola pulli trapami.

Party value:

- scout,
- single-target burst,
- interrupt/disable,
- trap setup,
- anti-caster pressure.

PvP loop:

- znalezc flank,
- wejsc z openerem,
- wymusic defensywe,
- zabic albo wycofac sie przed kontratakiem,
- unikac reveal/AoE.

Weaknesses:

- niski armor,
- slaby w dlugiej frontalnej walce,
- podatny na reveal, detection i AoE,
- zly engage zostawia go bezpiecznie karalnym.

Counterplay:

- reveal,
- detection,
- AoE,
- armor/poise,
- block frontu,
- poison cleanse,
- combat timer,
- trzymanie plecow przy scianie/sojuszniku.

### Podklasa: Zabojca

Identity:

- burst assassin,
- backstab,
- target kill,
- wysoka nagroda za dobre wejscie.

Unlock requirements:

- Level 25.
- Dexterity.
- Daggers Mastery.
- Stealth Mastery.
- Subclass trial oparty o stealth route i eliminacje celu bez alarmu.

Signature skill concept:

- Execution Mark: oznacza cel na krotko; pierwszy backstab albo crit w oknie zadaje bonusowe Physical damage.

Strong points:

- bardzo mocny opener,
- dobry przeciw squishy caster/healer,
- wysoka mobilnosc,
- duzy skill expression.

Weak points:

- slaby po nieudanym openerze,
- podatny na reveal,
- wymaga pozycji,
- slabszy przeciw heavy armor.

PvP counterplay:

- reveal/tracking,
- AoE check,
- trzymanie dystansu,
- armor/poise,
- peel od party,
- przetrwanie pierwszego okna i punish.

### Podklasa: Sabotazysta

Identity:

- traps,
- poison,
- control,
- area denial.

Unlock requirements:

- Level 25.
- Dexterity.
- Alchemy / Poison Mastery.
- Stealth albo Daggers Mastery.
- Subclass trial oparty o zastawianie pulapek i kontrolowanie pola.

Signature skill concept:

- Venom Minefield: rozklada ograniczona liczbe pulapek Poison/Control, ktore karza zle wejscie w teren.

Strong points:

- kontrola przestrzeni,
- dobry w defensywnym PvP,
- mocny w dlugich walkach,
- dobry przeciw melee chase.

Weak points:

- wymaga setupu,
- slabszy w otwartej szybkiej walce,
- counter przez detection/trap reveal,
- poison cleanse obniza presje.

PvP counterplay:

- reveal traps,
- cleanse poison,
- ominiecie terenu,
- ranged pressure,
- szybkie wymuszenie walki zanim postawi setup.

### Podklasa: Cien

Identity:

- stealth mobility,
- scouting,
- disengage,
- anti-targeting.

Unlock requirements:

- Level 25.
- Dexterity.
- Stealth Mastery.
- Subclass trial oparty o infiltracje, ucieczke i scouting.

Signature skill concept:

- Shadow Step: krotki reposition/disengage z ograniczeniami PvP i combat timerem.

Strong points:

- najlepszy scouting,
- mocny disengage,
- dobry do objective play,
- trudny do zlapapania bez detection.

Weak points:

- nizszy burst niz Zabojca,
- zalezy od stealth windows,
- reveal mocno go karze,
- slabszy w dlugim toe-to-toe.

PvP counterplay:

- detection gear,
- tracking,
- AoE reveal,
- combat timer,
- kontrola objective zamiast gonienia.

## Class 3: Lowca

Lowca to ranged physical/projectile klasa. Bazuje na lukach, kuszach, tracking, traps, pozycji i kontroli dystansu.

Rola:

- ranged pressure,
- scout,
- anti-stealth,
- kiting,
- objective harassment.

Preferred weapons:

- bow,
- crossbow,
- dagger/short sword as backup,
- traps.

Preferred armor:

- light armor,
- medium armor.

Primary stats:

- Dexterity.

Secondary stats:

- Vitality dla survival,
- Intelligence dla trap/utility hybryd.

Primary masteries:

- Archery,
- Stealth,
- Alchemy / Poison for trap variants.

Damage types:

- Physical,
- Poison for trap/venom variants.

Defensive model:

- Evasion,
- range,
- movement,
- traps,
- detection.

PvE solo loop:

- pull z dystansu,
- kite,
- trap slow/root,
- projectile pressure,
- reposition,
- finish z bezpiecznej pozycji.

Party value:

- ranged DPS,
- tracking,
- anti-stealth,
- trap utility,
- target calling.

PvP loop:

- trzymac line of sight i range,
- karac otwarte przestrzenie,
- kontrolowac flank trapami,
- uciekac przed hard engage.

Weaknesses:

- line of sight,
- block/projectile defense,
- gap closers,
- slaby gdy zamkniety w melee,
- wymaga pozycji.

Counterplay:

- line of sight,
- shield/block,
- dash engage,
- stealth flank,
- projectile evasion,
- terrain.

### Podklasa: Strzelec

Identity:

- long range single-target,
- precision shots,
- high projectile pressure.

Unlock requirements:

- Level 25.
- Dexterity.
- Archery Mastery.
- Subclass trial oparty o trafienia w ruchome cele i pozycjonowanie.

Signature skill concept:

- Piercing Shot: mocny projectile z windupem, ktory wymaga line of sight i moze byc zablokowany/unikniety.

Strong points:

- duzy range,
- dobry single-target DPS,
- karze slabe pozycjonowanie,
- mocny w open areas.

Weak points:

- slabszy w ciasnych mapach,
- podatny na gap close,
- projectile counterplay,
- wymaga aim/position.

PvP counterplay:

- line of sight,
- shield,
- dash,
- stealth flank,
- pressure podczas windupu.

### Podklasa: Tropiciel

Identity:

- tracking,
- survival,
- traps,
- anti-stealth.

Unlock requirements:

- Level 25.
- Dexterity.
- Archery Mastery.
- Stealth albo Alchemy / Poison Mastery.
- Subclass trial oparty o tropienie celu i wykrywanie ukrytego przeciwnika.

Signature skill concept:

- Hunter's Mark: oznacza cel, wzmacnia tracking i czesciowo oslabia stealth/disengage celu.

Strong points:

- anti-stealth,
- kontrola terenu,
- dobry survival,
- wartosc w party/guild wars jako scout.

Weak points:

- nizszy burst niz Strzelec,
- wymaga przygotowania,
- slabszy w bezposrednim all-in,
- zalezy od dobrego wykorzystania informacji.

PvP counterplay:

- cleanse mark,
- line of sight,
- presja na Tropiciela,
- fake movement,
- walka poza jego trap zone.

### Podklasa: Wladca Bestii

Identity:

- companion pressure,
- PvE utility,
- kontrolowany summoner lowcy,
- physical support przez bestie.

Unlock requirements:

- Level 25.
- Dexterity.
- Archery Mastery.
- Beast/companion trial.

Signature skill concept:

- Pack Command: krotkie okno, w ktorym companion wykonuje specjalna akcje, np. pin, harass albo interrupt.

Strong points:

- dobry PvE solo,
- dodatkowa presja,
- utility companion,
- dobry do scoutingu i kontroli celu.

Weak points:

- companion moze zostac zabity,
- slabszy direct burst,
- wymaga zarzadzania,
- w PvP summon damage musi byc ograniczony.

PvP counterplay:

- zabic/odciac companion,
- pressure owner,
- AoE cleave,
- CC na companion,
- line of sight.

PvP summon rules:

- limit jednego glownego companion,
- obnizony damage vs players,
- companion zalezy od pozycji wlasciciela,
- brak permanentnego auto-win pressure.

## Class 4: Mag

Mag to caster oparty o mana, ward, Destruction, cast windows i elementy. Ma duzy damage/control, ale wymaga zarzadzania zasobami i pozycja.

Rola:

- spell damage,
- area control,
- burst/control,
- ward defense,
- ranged magic pressure.

Preferred weapons:

- staff,
- wand,
- catalyst,
- off-catalyst.

Preferred armor:

- robes,
- light armor for mobility hybrids.

Primary stats:

- Intelligence.

Secondary stats:

- Dexterity dla mobility/cast handling,
- Vitality dla przezywalnosci.

Primary masteries:

- Destruction.

Damage types:

- Fire,
- Cold,
- Lightning.

Defensive model:

- Ward,
- mana management,
- positioning,
- slows/control.

PvE solo loop:

- kontrola dystansu,
- ward przed burstem,
- spell rotation,
- movement i line of sight,
- mana decyzje: burst czy defensywa.

Party value:

- AoE,
- burst windows,
- control,
- shield/ward utility depending skills,
- elemental coverage.

PvP loop:

- stworzyc safe cast window,
- wymusic defensywe,
- uzyc ward/mobility do przetrwania engage,
- punishowac stackowanie przeciwnikow AoE/control.

Weaknesses:

- interrupt,
- silence,
- mana pressure,
- line of sight,
- melee pressure,
- niski armor.

Counterplay:

- interrupt cast,
- line of sight,
- gap close,
- silence,
- mana drain/resource pressure,
- resisty,
- rozproszenie przeciw AoE.

### Podklasa: Piromanta

Identity:

- Fire damage,
- burn,
- area denial,
- sustained pressure.

Unlock requirements:

- Level 25.
- Intelligence.
- Destruction Mastery.
- Fire-focused trial.

Signature skill concept:

- Inferno Ground: tworzy krotka strefe Fire/Burn, dobra do area denial, mozliwa do opuszczenia.

Strong points:

- mocny pressure,
- dobry w PvE AoE,
- area denial,
- karze stojace cele.

Weak points:

- Fire resistance,
- mobility przeciwnika,
- wymaga cast windows,
- slabszy burst instant niz Burzomanta.

PvP counterplay:

- wyjsc ze strefy,
- Fire resistance,
- interrupt,
- line of sight,
- cleanse burn.

### Podklasa: Kriomanta

Identity:

- Cold control,
- slows,
- defensive setup,
- tempo control.

Unlock requirements:

- Level 25.
- Intelligence.
- Destruction Mastery.
- Cold-focused trial.

Signature skill concept:

- Frost Prison: krotkie kontrolne okno z silnymi limitami PvP i jasnym counterplayem.

Strong points:

- kontrola ruchu,
- kite,
- setup dla party,
- defensywna gra.

Weak points:

- nizszy raw damage,
- Tenacity ogranicza CC,
- mobile cleanse/buildy moga skrocic presje,
- wymaga setupu.

PvP counterplay:

- Tenacity,
- cleanse,
- mobility cooldown timing,
- interrupt,
- ranged pressure.

### Podklasa: Burzomanta

Identity:

- Lightning burst,
- shock,
- chain effects,
- mobility windows.

Unlock requirements:

- Level 25.
- Intelligence.
- Destruction Mastery.
- Lightning-focused trial.

Signature skill concept:

- Storm Surge: szybkie okno Lightning burst i reposition, ale po oknie Burzomanta jest podatny na pressure.

Strong points:

- burst,
- szybkie punish windows,
- dobry przeciw rozproszonym slabym celom,
- mobilniejszy caster.

Weak points:

- cooldown dependent,
- slabszy sustain,
- Lightning resistance,
- karany po spaleniu burstu.

PvP counterplay:

- przetrwac burst przez ward/block/resist,
- bait cooldown,
- pressure po oknie,
- line of sight,
- interrupt.

## Class 5: Akolita

Akolita to klasa Faith/ritual utility. Obejmuje healing, protection, cleanse, debuffy, anti-heal, Fire/Physical disrupt oraz ciemna galaz Chaos summonera/warlocka.

Rola:

- support,
- healer,
- protector,
- disruptor,
- debuffer,
- warlock/summoner branch.

Preferred weapons:

- mace/hammer,
- staff,
- wand,
- catalyst,
- shield for defensive variants.

Preferred armor:

- robes,
- medium armor,
- heavy armor for Inkwizytor hybrids.

Primary stats:

- Faith.

Secondary stats:

- Intelligence for ritual/warlock,
- Strength for Inkwizytor,
- Vitality for support survivability.

Primary masteries:

- Restoration,
- Chaos for warlock branch,
- One-Handed/Shield for Inkwizytor variants.

Damage types:

- Fire,
- Physical,
- Chaos,
- Poison only as optional ritual/curse extension if needed.

Defensive model:

- healing,
- cleanse,
- ward/protection,
- debuffs,
- medium durability,
- resource management.

PvE solo loop:

- sustain przez heal/ward,
- kontrola przeciwnika debuffem,
- Fire/Physical albo Chaos pressure zalezne od builda,
- ostrozne zarzadzanie zasobem Faith/mana,
- utility zamiast czystego burstu.

Party value:

- healing,
- cleanse,
- protection,
- anti-heal,
- debuffs,
- summon/curse utility for Warlock,
- disrupt casterow.

PvP loop:

- utrzymac sojusznikow,
- wymusic cleanse/anti-heal decisions,
- karac zle engage,
- przetrwac presje bez stania sie niesmiertelnym.

Weaknesses:

- anti-heal,
- silence,
- interrupt,
- resource pressure,
- focus fire,
- slabszy burst zalezne od podklasy.

Counterplay:

- anti-heal,
- silence,
- interrupt,
- burst windows,
- mana/faith pressure,
- target switch,
- cleanse curses,
- kill summons.

### Podklasa: Kapelan

Identity:

- healer/support/protection,
- party sustain,
- cleanse,
- defensive utility.

Unlock requirements:

- Level 25.
- Faith.
- Restoration Mastery.
- Subclass trial oparty o leczenie, cleanse i ochrone NPC/party pod presja.

Signature skill concept:

- Sanctuary Ward: krotkie protection window dla sojusznikow, z PvP healing/protection modifierem.

Strong points:

- najlepszy healing,
- cleanse,
- ochrona party,
- stabilnosc w dungeonach.

Weak points:

- nizszy damage,
- podatny na anti-heal/silence,
- wymaga party positioning,
- focus pressure zmusza go do defensywy.

PvP counterplay:

- anti-heal,
- silence,
- interrupt,
- swap target,
- force movement,
- burst po wyczerpaniu defensyw.

Solo viability:

- Kapelan musi miec podstawowe Fire/Physical albo Faith damage skille, zeby grac solo wolniej, ale stabilnie.

### Podklasa: Inkwizytor

Identity:

- Fire + Physical disruptor,
- anti-heal,
- silence/interrupt pressure,
- melee/caster hybrid.

Unlock requirements:

- Level 25.
- Faith.
- Strength albo Intelligence depending variant.
- Restoration albo One-Handed Mastery.
- Subclass trial oparty o przerwanie rytualow/castow i wygranie walki przez presje.

Signature skill concept:

- Judgment Brand: oznacza cel, naklada anti-heal albo punishment za cast/heal w krotkim oknie.

Strong points:

- dobry przeciw healerom i casterom,
- Fire/Physical pressure,
- anti-heal,
- interrupt/silence utility,
- sensowny solo damage.

Weak points:

- nie jest najlepszym healerem,
- nie jest pelnym tankiem,
- wymaga wejscia w range,
- podatny na kite i resource pressure.

PvP counterplay:

- kite,
- bait interrupt,
- cleanse/shorten brand,
- focus po wejsciu,
- Fire resistance,
- block/parry melee pressure.

Solo viability:

- Inkwizytor nie jest anti-undead-only. Ma dzialac jako normalny PvE/PvP disruptor przez Fire/Physical pressure.

### Podklasa: Rytualista / Warlock

Identity:

- summoner,
- warlock,
- Chaos curses,
- soul/resource drain,
- debuffs,
- kontrolowany minion pressure.

Unlock requirements:

- Level 25.
- Faith.
- Intelligence albo Chaos Mastery.
- Restoration albo Chaos Mastery depending final progression.
- Subclass trial oparty o utrzymanie summonow, curse uptime i przetrwanie presji podczas rytualu.

Damage types:

- Chaos jako glowny typ.
- Physical przez summony.
- Poison opcjonalnie dla curse/decay wariantow, jesli pasuje do skilli.

Core gameplay:

- curse target,
- summon minion,
- drain resources albo health,
- setup debuff window,
- pressure przez attrition,
- utrzymanie pozycji wlasciciela.

Signature skill concept:

- Pact of the Hollow: przywoluje mocniejszego miniona albo wzmacnia aktywne summony na krotko, ale zwieksza koszt zasobow albo oslabia defensywe wlasciciela po oknie.

Strong points:

- bardzo dobry PvE solo,
- presja przez summony i curse,
- wymusza cleanse/target decisions,
- utility w party przez debuffy,
- dobry w dluzszych walkach.

Weak points:

- wymaga setupu,
- slaba mobilnosc,
- podatny na burst po przerwaniu rytualu,
- summony mozna zabic,
- curse mozna cleanseowac,
- owner pressure jest kluczowa kontra.

PvE solo loop:

- summon tank/pressure,
- curse glowny cel,
- drain/sustain,
- kontrola pulli,
- recast summon po stracie,
- nie stac w melee bez przygotowania.

Party value:

- debuffer,
- summon utility,
- curse support,
- anti-heal/resource pressure,
- dodatkowe targety odciagajace czesc presji w PvE.

PvP role:

- pressure-over-time,
- target disruption,
- wymuszanie cleanse,
- wymuszanie decyzji: zabic summony czy naciskac ownera,
- attrition w dluzszej walce.

PvP summon rules:

- limit aktywnych summonow.
- slabszy summon damage vs players.
- summony musza byc zabijalne.
- summony nie moga same wygrac walki bez aktywnej gry ownera.
- owner musi byc realnym celem do zabicia.
- summony powinny miec czytelne role: tank, harass, debuff, nie wszystko naraz.
- recast summon powinien miec koszt i/lub cooldown.

PvP counterplay:

- cleanse curse,
- kill summon,
- interrupt ritual,
- burst ownera,
- line of sight,
- AoE cleave,
- anti-heal,
- resource pressure.

## Hybrid Rules

Hybrydy sa dozwolone, ale maja koszt.

Zasady:

- Bron moze odblokowywac czesc weapon skills niezaleznie od klasy.
- Proste utility skille moga byc mieszane szerzej.
- Najmocniejsze skille wymagaja klasy albo podklasy.
- Najmocniejsze pasywki wymagaja mastery i stat investment.
- Gear moze wspierac hybryde, ale nie powinien calkowicie omijac wymagan.
- Hybryda ma placic statami, mastery, resource economy albo slabszym dostepem do signature skilli.

Przyklady:

- Wojownik moze uzyc prostego ward/utility spell, ale nie bedzie pelnym Magiem bez Intelligence, mana i Destruction.
- Akolita Inkwizytor moze isc w melee, ale nie bedzie tak tanky jak Straznik.
- Lowca moze uzywac stealth, ale nie bedzie mial burstu Zabojcy bez Daggers/Stealth investment.
- Rytualista / Warlock moze korzystac z summonow, ale nie powinien miec rownoczesnie pelnego healingu Kapelana i burstu Maga.

## PvP Sanity Rules

Kazda klasa musi miec jasne PvP ograniczenia.

Burst:

- musi miec setup albo cooldown,
- musi miec punish window,
- nie moze byc niewidzialny i bez reakcji.

Healing:

- ma PvP modifier,
- kontrowany anti-heal, silence, interrupt, pressure,
- nie moze robic niesmiertelnego turtla.

Stealth:

- kontrowany reveal, detection, AoE, tracking, combat timer,
- opener mocny, ale zly engage karalny.

Summons:

- limit summonow,
- obnizony damage vs players,
- zabijalne,
- owner pressure jako glowna kontra.

Tanks:

- wysoka przezywalnosc,
- nizszy kill pressure,
- kontrowani guard break, flank, DoT, magic pressure.

Casters:

- mocne spelle,
- podatni na interrupt, silence, line of sight, mana pressure i melee engage.

Ranged:

- mocny range,
- kontrowany line of sight, gap close, block i terrain.

## Test Cases And Scenarios

Class & Subclass v0.0.1 powinno przejsc ponizsze scenariusze:

- Wojownik moze clearowac PvE solo przez armor, block, counter i physical pressure.
- Straznik trzyma objective, ale nie zabija szybko i przegrywa z guard break/flank/DoT.
- Berserker ma mocny burst, ale po nieudanym engage jest karalny.
- Gladiator wygrywa timingiem, ale przegrywa z baitem, AoE i zlym parry.
- Lotrzyk zabija przez stealth/backstab, ale reveal, AoE, detection i armor sa realnymi kontrami.
- Sabotazysta kontroluje teren, ale przegrywa, jesli przeciwnik ominie setup albo uzyje cleanse.
- Lowca kiteuje i strzela, ale line of sight, shield i gap closer maja znaczenie.
- Wladca Bestii ma companion utility, ale summon damage w PvP jest ograniczony.
- Mag ma burst/control/ward, ale interrupt, silence, mana pressure i line of sight go karza.
- Piromanta kontroluje teren Fire, ale Fire resistance i movement ograniczaja presje.
- Kriomanta kontroluje ruchem, ale Tenacity i cleanse skracaja CC.
- Burzomanta burstuje, ale ma okno slabosci po spaleniu cooldownow.
- Kapelan leczy party i gra solo wolniej, ale anti-heal/silence/interrupt go zatrzymuja.
- Inkwizytor dziala jako Fire/Physical disruptor, nie jako anti-undead-only nisza.
- Rytualista / Warlock gra summonami i curse, ale cleanse, kill summon, interrupt i owner pressure sa jasna kontra.
- Hybrydy sa mozliwe, ale najmocniejsze setupy wymagaja kosztu w statach, mastery i gearze.
- Zadna klasa nie wymaga jednego konkretnego unique itemu, zeby dzialac.

## Powiazania z fundamentami

Ten dokument musi pozostac spojny z:

- docs/combat-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.
- docs/progression-respec-foundation-v0.0.1.md.
- docs/world-content-loop-foundation-v0.0.1.md.
- docs/economy-crafting-loot-foundation-v0.0.1.md.
- docs/mob-boss-encounter-001-foundation-v0.0.1.md.
- docs/pet-companion-minion-system-foundation-v0.0.1.md.

W szczegolnosci:

- Typy obrazen zostaja bez zmian.
- Defensywy zostaja bez zmian.
- Skille musza uzywac tagow z skill systemu.
- Itemy wzmacniaja tagi i buildy, nie same nazwy klas.
- Podklasy odblokowuja sie przez level, staty, mastery i trial.
- PvP wymaga counterplayu dla kazdej mocnej mechaniki.
- Summony musza miec osobne PvP zasady.

## Assumptions

- Nazwy klas moga pozniej dostac lore flavor, ale teraz priorytetem jest mechanika.
- Rytualista / Warlock zostaje pod Akolita jako ciemna, summonowo-klatkowa galaz.
- Iluzjonista nie jest teraz core podklasa, bo wymaga osobnego, bardzo czytelnego PvP counterplayu.
- Nekromanta jako pelna osobna fantazja moze pozniej wyrosnac z Rytualisty / Warlocka, jesli summony i Chaos beda dzialac w PvP.
- Konkretne liczby, cooldowny, scalingi i pelne listy skilli beda osobnym etapem.
