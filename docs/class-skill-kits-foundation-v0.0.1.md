# Class Skill Kits Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje pierwsza wersje skill kitow klas i podklas dla serwera MCMMORPG.

Zakres:

- 4 core class skills dla kazdej klasy bazowej po wyborze na Level 10.
- 1 signature skill dla kazdej podklasy po wyborze na Level 25.
- Kierunek upgrade dla wybranej podklasy na Level 50.
- Role PvE solo, party i PvP.
- Tagi, damage type, resource, scaling direction i counterplay.

Nie definiujemy jeszcze finalnych liczb: damage %, cooldownow, range, duration, kosztow many/staminy ani dokladnych wartosci scalingu. Ten dokument opisuje funkcje gameplayowa i sanity rules.

Model progresji:

- Level 1-10: starter style testers.
- Level 10: wybor klasy bazowej i core class kit.
- Level 25: wybor podklasy i signature skill.
- Level 50: upgrade wybranej podklasy, nie druga pelna podklasa.

## Skill Design Rules

Kazdy skill musi miec:

- gameplay function,
- tags,
- damage type albo brak damage,
- resource,
- scaling direction,
- PvE use,
- PvP counterplay,
- weakness albo tradeoff.

Zasady:

- Najpierw funkcja gameplayowa, potem nazwa fantasy.
- Skill musi wzmacniac loop klasy, nie byc losowym przyciskiem.
- Kazda klasa musi miec PvE solo loop.
- Kazda klasa musi miec party value.
- Kazda klasa musi miec PvP role i PvP counterplay.
- Support nie moze byc martwy solo.
- Summony, stealth, healing, CC, burst i mobility musza miec PvP sanity rules.
- Nie dodajemy nowych damage types.

Dozwolone damage types:

- Physical.
- Fire.
- Cold.
- Lightning.
- Chaos.
- Poison.

## Skill Entry Format

Kazdy skill w przyszlych dokumentach/configach powinien uzywac tego formatu:

- Name.
- Unlock Tier: Class Level 10, Subclass Level 25, Upgrade Level 50 albo later.
- Function.
- Tags.
- Damage Type.
- Resource.
- Scaling Direction.
- PvE Use.
- PvP Use.
- Counterplay.
- Notes.

## Wojownik Level 10 Core Kit

Wojownik po Level 10 rozwija melee pressure, aktywna obrone, stamina tempo i guard/counter gameplay.

### Core Skill: Cleaving Strike

Function:

- podstawowy class damage skill,
- melee pressure,
- lekki cleave na bliskie cele.

Tags:

- Melee.
- Physical.

Damage Type:

- Physical.

Resource:

- Stamina.

Scaling Direction:

- weapon damage,
- Strength,
- Melee Damage,
- Physical Damage.

PvE Use:

- stabilny clear mobow,
- podstawowy skill do walki solo.

PvP Use:

- pressure w melee,
- kara za zle pozycjonowanie przeciwnika.

Counterplay:

- dodge,
- block,
- parry,
- kite,
- nie stac w cleave range.

### Core Skill: Guard Brace

Function:

- aktywna defensywa,
- wzmacnia block/guard na krotko,
- uczy zarzadzania stamina.

Tags:

- Defensive.
- Melee.

Damage Type:

- none.

Resource:

- Stamina.

Scaling Direction:

- Strength,
- Vitality,
- Block Strength,
- Guard,
- Armor.

PvE Use:

- przetrwanie burstu mobow/bossow,
- przygotowanie do countera.

PvP Use:

- obrona przed melee/projectile pressure,
- trzymanie objective.

Counterplay:

- guard break,
- flank,
- DoT,
- magic pressure,
- bait defensywy.

### Core Skill: Breaker Blow

Function:

- guard pressure,
- wolniejszy mocniejszy atak,
- kara za turtlowanie.

Tags:

- Melee.
- Physical.
- Guard Break.

Damage Type:

- Physical.

Resource:

- Stamina.

Scaling Direction:

- Strength,
- Two-Handed albo One-Handed Mastery,
- Physical Damage.

PvE Use:

- przelamywanie defensywnych mobow,
- boss guard/stagger windows.

PvP Use:

- kontra na tankow i block spam.

Counterplay:

- dodge windup,
- parry,
- interrupt/stagger,
- kite.

### Core Skill: Battle Momentum

Function:

- tempo/resource skill,
- odzyskuje albo generuje zasob po udanym block/counter/hit,
- laczy defensywe z ofensywa.

Tags:

- Defensive.
- Melee.
- Utility.

Damage Type:

- none.

Resource:

- Stamina/Rage depending later implementation.

Scaling Direction:

- Strength,
- Vitality,
- Poise,
- mastery.

PvE Use:

- pozwala utrzymac dluzsza walke solo.

PvP Use:

- nagradza dobry timing i aktywna obrone.

Counterplay:

- nie dawac darmowych hitow,
- bait block,
- resource pressure,
- DoT/CC zamiast frontalnego spamowania.

## Wojownik Level 25 Signature Skills

### Straznik Signature: Guardian Bulwark

Function:

- mocne defensywne okno,
- chroni Straznika i czesciowo sojusznikow za nim.

Tags:

- Defensive.
- Aura.

Damage Type:

- none.

Resource:

- Stamina.

Scaling Direction:

- Vitality,
- Shield Mastery,
- Block Strength,
- Guard.

PvE Use:

- boss burst window,
- ochrona party.

PvP Use:

- objective hold,
- ochrona backline.

Counterplay:

- flank,
- guard break,
- DoT,
- rozproszenie przeciwnikow,
- przeczekanie okna.

Tradeoff:

- niska mobilnosc i niski kill pressure podczas defensywnego okna.

Level 50 Upgrade Identity:

- Bastion: mocniejsza ochrona objective i party.
- Protector: wiecej peel/ally protection, mniej personal pressure.

### Berserker Signature: Rage Breaker

Function:

- burst/guard break window,
- wysoka presja w melee,
- po oknie Berserker ma slabsza defensywe.

Tags:

- Melee.
- Physical.
- Guard Break.

Damage Type:

- Physical.

Resource:

- Rage/Stamina.

Scaling Direction:

- Strength,
- Two-Handed Mastery,
- Physical Damage,
- Attack Speed albo Heavy Attack modifiers.

PvE Use:

- burst elite/boss addow,
- cleave wave clear.

PvP Use:

- wymusza defensywe,
- kara za turtlowanie.

Counterplay:

- dodge burst window,
- kite,
- parry heavy attacks,
- CC po oknie,
- punish defensywnego debuffa.

Tradeoff:

- duza sila, ale bardzo karalny po nieudanym engage.

Level 50 Upgrade Identity:

- Ravager: wiecej guard break i cleave.
- Bloodrager: wiecej sustain przez agresje, wieksze ryzyko przy low HP.

### Gladiator Signature: Perfect Riposte

Function:

- timingowy counter po parry/block,
- nagradza skill expression.

Tags:

- Melee.
- Defensive.
- Physical.

Damage Type:

- Physical.

Resource:

- Stamina/Focus.

Scaling Direction:

- Dexterity,
- One-Handed Mastery,
- Crit Damage,
- Counter modifiers.

PvE Use:

- karanie przewidywalnych atakow elite/bossow.

PvP Use:

- mocne 1v1,
- kara za czytelny melee engage.

Counterplay:

- bait parry,
- delayed attacks,
- DoT/AoE,
- ranged pressure,
- nie dawac darmowego riposte.

Tradeoff:

- bez poprawnego timingu skill jest slaby albo niedostepny.

Level 50 Upgrade Identity:

- Duelmaster: lepsze 1v1 i markowanie celu.
- Blademaster: plynniejsze combo i mobility, slabsza defensywa.

## Lotrzyk Level 10 Core Kit

Lotrzyk po Level 10 rozwija stealth/mobility, backstab, poison/trap pressure i disengage.

### Core Skill: Quick Cut

Function:

- szybki melee strike,
- dobry do weave miedzy ruchem i basic attackami.

Tags:

- Melee.
- Physical.

Damage Type:

- Physical.

Resource:

- Stamina/Focus.

Scaling Direction:

- Dexterity,
- Daggers Mastery,
- Melee Damage,
- Crit Chance.

PvE Use:

- szybki clear pojedynczych mobow.

PvP Use:

- pressure po engage,
- finisher na low HP.

Counterplay:

- block,
- armor,
- peel,
- kite,
- punish po wejscu.

### Core Skill: Fade Step

Function:

- mobility/disengage,
- krotki dash z reduced detection albo minor stealth utility.

Tags:

- Movement.
- Defensive.
- Stealth.

Damage Type:

- none.

Resource:

- Stamina/Focus.

Scaling Direction:

- Dexterity,
- Movement Speed,
- Stealth.

PvE Use:

- reset pozycji,
- unikanie mocnych atakow.

PvP Use:

- wejscie/wyjscie z walki,
- bait defensyw.

Counterplay:

- reveal,
- tracking,
- AoE,
- root/slow,
- pressure po cooldownie.

### Core Skill: Venom Edge

Function:

- poison pressure,
- uczy dluzszej walki i cleanse counterplay.

Tags:

- Melee.
- DoT.
- Poison.

Damage Type:

- Poison.

Resource:

- Focus/Stamina.

Scaling Direction:

- Dexterity,
- Alchemy / Poison Mastery,
- DoT Damage,
- Poison Damage.

PvE Use:

- dobre przeciw elite/high HP mobom.

PvP Use:

- pressure-over-time,
- kara za przeciwnikow bez cleanse/poison resist.

Counterplay:

- poison resist,
- cleanse,
- burst Lotrzyka przed pelnym uptime,
- disengage.

### Core Skill: Cheap Trick

Function:

- utility/control,
- krotki interrupt, blind albo minor slow depending implementation.

Tags:

- Control.
- Melee.

Damage Type:

- Physical albo none.

Resource:

- Focus.

Scaling Direction:

- Dexterity,
- Control modifiers,
- Stealth.

PvE Use:

- przerywanie mobow,
- kontrola pulli.

PvP Use:

- ustawienie burstu albo ucieczki.

Counterplay:

- Tenacity,
- range,
- bait,
- cooldown punish.

## Lotrzyk Level 25 Signature Skills

### Zabojca Signature: Execution Mark

Function:

- oznacza cel,
- pierwszy backstab/crit w oknie ma bonus.

Tags:

- Melee.
- Stealth.
- Physical.

Damage Type:

- Physical.

Resource:

- Focus.

Scaling Direction:

- Dexterity,
- Daggers Mastery,
- Crit Damage,
- Stealth.

PvE Use:

- szybkie zabijanie priorytetowych celow.

PvP Use:

- burst na healer/caster/squishy.

Counterplay:

- reveal,
- peel,
- armor/poise,
- przetrwac opener,
- trzymac plecy bezpiecznie.

Tradeoff:

- po nieudanym openerze Zabojca traci glowne okno presji.

Level 50 Upgrade Identity:

- Executioner: mocniejszy finisher na low HP.
- Nightblade: wiecej stealth resetow, slabszy direct burst.

### Sabotazysta Signature: Venom Minefield

Function:

- trap zone,
- Poison/Control area denial.

Tags:

- Trap.
- AoE.
- Poison.
- Control.

Damage Type:

- Poison.

Resource:

- Focus.

Scaling Direction:

- Dexterity,
- Alchemy / Poison Mastery,
- Trap Damage,
- DoT Damage.

PvE Use:

- kontrola fal mobow,
- setup przed pullami.

PvP Use:

- kontrola objective,
- kara za melee chase.

Counterplay:

- trap detection,
- ominiecie pola,
- cleanse/root break,
- ranged pressure przed setupem.

Tradeoff:

- wymaga przygotowania i slabszy w naglym all-in.

Level 50 Upgrade Identity:

- Plague Engineer: mocniejsze poison/DoT identity.
- Trapmaster: wiecej kontroli terenu i utility.

### Cien Signature: Shadow Step

Function:

- mocny reposition/disengage,
- stealth/mobility identity.

Tags:

- Movement.
- Stealth.
- Defensive.

Damage Type:

- none.

Resource:

- Focus/Stamina.

Scaling Direction:

- Dexterity,
- Stealth,
- Movement Speed.

PvE Use:

- omijanie zagrozen,
- scoutowanie,
- reset pozycji.

PvP Use:

- objective play,
- disengage,
- flank setup.

Counterplay:

- reveal,
- tracking,
- combat timer,
- AoE,
- kontrola celu zamiast gonienia.

Tradeoff:

- nizszy burst niz Zabojca.

Level 50 Upgrade Identity:

- Phantom: lepsze stealth/reveal resistance, ale mniejszy damage.
- Shadowrunner: lepszy objective mobility, slabsza walka frontalna.

## Lowca Level 10 Core Kit

Lowca po Level 10 rozwija projectile pressure, tracking, traps, companion utility i anti-stealth.

### Core Skill: Focused Shot

Function:

- podstawowy ranged damage skill,
- uczy line of sight i aim.

Tags:

- Projectile.
- Physical.

Damage Type:

- Physical.

Resource:

- Stamina/Focus.

Scaling Direction:

- Dexterity,
- Archery Mastery,
- Projectile Damage,
- Crit Chance.

PvE Use:

- bezpieczny pull i single-target DPS.

PvP Use:

- ranged pressure i punish na open terrain.

Counterplay:

- line of sight,
- block,
- dodge,
- gap close,
- terrain.

### Core Skill: Snare Trap

Function:

- trap/control,
- utrzymanie dystansu.

Tags:

- Trap.
- Control.

Damage Type:

- Physical albo none.

Resource:

- Focus.

Scaling Direction:

- Dexterity,
- Trap modifiers,
- Control duration with PvP limits.

PvE Use:

- kontrola pulli,
- kite elite mobow.

PvP Use:

- peel,
- objective control,
- kara za chase.

Counterplay:

- detection,
- ominiecie,
- cleanse/root break,
- ranged pressure.

### Core Skill: Tracking Mark

Function:

- mark/detection,
- anti-stealth,
- target focus.

Tags:

- Utility.
- Control.
- Detection.

Damage Type:

- none.

Resource:

- Focus.

Scaling Direction:

- Dexterity,
- Detection,
- Archery Mastery.

PvE Use:

- oznaczanie priorytetowych celow.

PvP Use:

- anti-stealth,
- focus target,
- ograniczenie disengage.

Counterplay:

- cleanse,
- line of sight,
- bait mark,
- pressure Lowcy.

### Core Skill: Companion Command

Function:

- basic companion/summon command,
- wprowadza droge do Wladcy Bestii.

Tags:

- Minion.
- Utility.

Damage Type:

- Physical.

Resource:

- Focus.

Scaling Direction:

- Dexterity,
- Minion Damage,
- Archery Mastery albo companion mastery later.

PvE Use:

- companion harass/tank light targets,
- solo utility.

PvP Use:

- minor pressure,
- interrupt/harass depending implementation.

Counterplay:

- kill companion,
- AoE,
- pressure owner,
- line of sight.

PvP Notes:

- companion damage vs players musi byc limitowany.

## Lowca Level 25 Signature Skills

### Strzelec Signature: Piercing Shot

Function:

- long range high-commit projectile.

Tags:

- Projectile.
- Physical.

Damage Type:

- Physical.

Resource:

- Focus/Stamina.

Scaling Direction:

- Dexterity,
- Archery Mastery,
- Projectile Damage,
- Crit Damage.

PvE Use:

- burst na priority targets.

PvP Use:

- kara za slabe pozycjonowanie.

Counterplay:

- line of sight,
- block,
- dodge windup,
- pressure podczas przygotowania.

Tradeoff:

- mocny strzal, ale czytelny i karalny.

Level 50 Upgrade Identity:

- Deadeye: mocniejszy single-target precision.
- Marksman: stabilniejszy DPS i range control.

### Tropiciel Signature: Hunter's Mark

Function:

- mocny mark/tracking,
- anti-stealth i focus target.

Tags:

- Utility.
- Control.
- Detection.

Damage Type:

- none.

Resource:

- Focus.

Scaling Direction:

- Dexterity,
- Detection,
- Archery Mastery.

PvE Use:

- oznaczanie elite/boss targetow.

PvP Use:

- reveal/pressure na stealth i mobile cele.

Counterplay:

- cleanse,
- line of sight,
- presja na Tropiciela,
- fake movement.

Tradeoff:

- mniej burstu niz Strzelec.

Level 50 Upgrade Identity:

- Pathfinder: wiecej survival/trap mobility.
- Stalker: mocniejsze tracking i anti-stealth.

### Wladca Bestii Signature: Pack Command

Function:

- wzmacnia companion i wydaje mu specjalna komende.

Tags:

- Minion.
- Utility.
- Physical.

Damage Type:

- Physical.

Resource:

- Focus.

Scaling Direction:

- Dexterity,
- Minion Damage,
- companion modifiers.

PvE Use:

- companion tank/harass,
- solo utility.

PvP Use:

- dodatkowa presja i interrupt/harass.

Counterplay:

- kill companion,
- CC companion,
- pressure owner,
- AoE cleave.

Tradeoff:

- slabszy direct personal burst niz Strzelec.

Level 50 Upgrade Identity:

- Packlord: mocniejszy companion pressure.
- Primal Caller: wiecej utility/survival przez companion.

## Mag Level 10 Core Kit

Mag po Level 10 rozwija elemental spellcasting, ward, mana tempo i area/control gameplay.

### Core Skill: Elemental Bolt

Function:

- podstawowy spell damage,
- Fire/Cold/Lightning wariant zalezne od loadout/skilla.

Tags:

- Spell.
- Projectile.

Damage Type:

- Fire albo Cold albo Lightning.

Resource:

- Mana.

Scaling Direction:

- Intelligence,
- Spell Damage,
- elemental damage.

PvE Use:

- podstawowy ranged clear.

PvP Use:

- ranged magic pressure.

Counterplay:

- interrupt,
- line of sight,
- dodge projectile,
- resist.

### Core Skill: Lesser Ward

Function:

- podstawowa defensywa casterow.

Tags:

- Defensive.
- Spell.

Damage Type:

- none.

Resource:

- Mana.

Scaling Direction:

- Intelligence,
- Ward,
- Mana.

PvE Use:

- przetrwanie burstu mobow.

PvP Use:

- ochrona przed burst window.

Counterplay:

- pressure ward,
- interrupt recast,
- mana drain,
- sustained damage.

### Core Skill: Arcane Step

Function:

- caster reposition,
- nie pelny teleport bez kontr.

Tags:

- Movement.
- Spell.
- Defensive.

Damage Type:

- none.

Resource:

- Mana.

Scaling Direction:

- Intelligence,
- Movement modifiers,
- cooldown reduction with PvP caps.

PvE Use:

- unikanie boss mechanics.

PvP Use:

- dystans od melee engage.

Counterplay:

- bait movement,
- root/slow,
- pressure po cooldownie,
- line of sight.

### Core Skill: Mana Focus

Function:

- resource/tempo skill,
- odzyskuje albo oszczedza mane kosztem okna podatnosci.

Tags:

- Spell.
- Utility.

Damage Type:

- none.

Resource:

- Mana.

Scaling Direction:

- Intelligence,
- Mana Regen,
- Cast Speed.

PvE Use:

- dluzsze walki bossowe.

PvP Use:

- ryzykowne odzyskanie tempa.

Counterplay:

- interrupt,
- pressure podczas channel/cast,
- force movement.

## Mag Level 25 Signature Skills

### Piromanta Signature: Inferno Ground

Function:

- Fire area denial.

Tags:

- Spell.
- AoE.
- Fire.
- DoT.

Damage Type:

- Fire.

Resource:

- Mana.

Scaling Direction:

- Intelligence,
- Fire Damage,
- AoE Damage,
- DoT Damage.

PvE Use:

- wave clear,
- kontrola pozycji mobow.

PvP Use:

- objective denial,
- punish stacked players.

Counterplay:

- wyjsc ze strefy,
- Fire resistance,
- cleanse burn,
- interrupt cast.

Tradeoff:

- slabszy przeciw mobilnym celom.

Level 50 Upgrade Identity:

- Infernalist: mocniejszy burn i pressure.
- Ashcaller: wiecej area denial i debuffow.

### Kriomanta Signature: Frost Prison

Function:

- Cold control,
- krotkie setup window.

Tags:

- Spell.
- Control.
- Cold.

Damage Type:

- Cold.

Resource:

- Mana.

Scaling Direction:

- Intelligence,
- Cold Damage,
- Control modifiers with PvP limits.

PvE Use:

- kontrola elite/addow.

PvP Use:

- setup dla teamu,
- defensywne zatrzymanie engage.

Counterplay:

- Tenacity,
- cleanse,
- interrupt,
- mobility timing.

Tradeoff:

- nizszy raw damage niz Piromanta/Burzomanta.

Level 50 Upgrade Identity:

- Frostbinder: mocniejsza kontrola celu.
- Cryoguard: defensywny ward/control hybrid.

### Burzomanta Signature: Storm Surge

Function:

- Lightning burst + reposition window.

Tags:

- Spell.
- Lightning.
- Movement.

Damage Type:

- Lightning.

Resource:

- Mana.

Scaling Direction:

- Intelligence,
- Lightning Damage,
- Cast Speed,
- Crit Chance.

PvE Use:

- szybkie punish windows,
- mobilny caster clear.

PvP Use:

- burst na zle ustawione cele,
- reposition.

Counterplay:

- przetrwac burst,
- Lightning resistance,
- bait cooldown,
- pressure po oknie.

Tradeoff:

- cooldown dependent i slabszy sustain.

Level 50 Upgrade Identity:

- Stormcaller: mocniejszy shock/chain pressure.
- Tempest: wiecej mobility i burst windows.

## Akolita Level 10 Core Kit

Akolita po Level 10 rozwija heal/ward, cleanse, judgment/anti-heal i curse/summon/ritual gameplay.

### Core Skill: Minor Mending

Function:

- podstawowy heal/sustain,
- support i solo stabilnosc.

Tags:

- Heal.
- Spell.

Damage Type:

- none.

Resource:

- Faith/Mana.

Scaling Direction:

- Faith,
- Healing Power,
- Restoration Mastery.

PvE Use:

- sustain solo i party.

PvP Use:

- ograniczony heal pod PvP modifierem.

Counterplay:

- anti-heal,
- interrupt,
- silence,
- pressure.

### Core Skill: Purifying Ward

Function:

- defensive support,
- minor ward albo cleanse-lite.

Tags:

- Defensive.
- Heal.
- Spell.

Damage Type:

- none.

Resource:

- Faith/Mana.

Scaling Direction:

- Faith,
- Ward,
- Cleanse Strength,
- Restoration Mastery.

PvE Use:

- ochrona przed DoT/statusami.

PvP Use:

- odpowiedz na poison/burn/curse pressure.

Counterplay:

- bait cleanse,
- reapply debuffs,
- interrupt,
- resource pressure.

### Core Skill: Judgment Spark

Function:

- offensive support/disrupt,
- Fire/Faith pressure,
- lekki anti-heal albo cast punish depending implementation.

Tags:

- Spell.
- Fire.
- Control.

Damage Type:

- Fire.

Resource:

- Faith/Mana.

Scaling Direction:

- Faith,
- Fire Damage,
- Spell Damage,
- Anti-Heal modifiers with caps.

PvE Use:

- solo damage dla Akolity.

PvP Use:

- pressure na healer/caster.

Counterplay:

- cleanse,
- Fire resistance,
- interrupt,
- line of sight.

### Core Skill: Lesser Rite

Function:

- ritual/debuff/summon teaser for Akolita dark branch,
- daje droge do Rytualista / Warlock.

Tags:

- Curse.
- Minion.
- Chaos.
- Spell.

Damage Type:

- Chaos albo none depending variant.

Resource:

- Faith/Mana/Soul later.

Scaling Direction:

- Faith,
- Intelligence,
- Chaos Mastery,
- Minion Damage,
- Curse modifiers.

PvE Use:

- minor summon/debuff support,
- solo pressure.

PvP Use:

- wymusza cleanse albo zabicie summon/pressure ownera.

Counterplay:

- cleanse curse,
- kill summon,
- interrupt ritual,
- pressure owner.

PvP Notes:

- Level 10 version musi byc slabsza niz pelny Warlock kit.

## Akolita Level 25 Signature Skills

### Kapelan Signature: Sanctuary Ward

Function:

- mocne protection/heal window.

Tags:

- Heal.
- Defensive.
- Aura.

Damage Type:

- none.

Resource:

- Faith/Mana.

Scaling Direction:

- Faith,
- Healing Power,
- Ward,
- Restoration Mastery.

PvE Use:

- boss burst windows,
- party sustain.

PvP Use:

- ochrona teamu, z PvP modifierem.

Counterplay:

- anti-heal,
- silence,
- interrupt,
- force movement,
- bait cooldown.

Tradeoff:

- nizszy damage i podatnosc na focus pressure.

Level 50 Upgrade Identity:

- Hierophant: mocniejsze ward/protection.
- Saint: mocniejszy cleanse/heal identity, mocne PvP limity.

### Inkwizytor Signature: Judgment Brand

Function:

- mark/anti-heal/cast punish,
- Fire + Physical disruptor identity.

Tags:

- Spell.
- Fire.
- Curse.
- Control.

Damage Type:

- Fire albo Physical depending follow-up.

Resource:

- Faith/Mana/Stamina hybrid.

Scaling Direction:

- Faith,
- Strength albo Intelligence,
- Fire Damage,
- Anti-Heal modifiers with caps.

PvE Use:

- solo damage i caster/healer mob disruption.

PvP Use:

- presja na healerow/casterow,
- anti-heal window.

Counterplay:

- cleanse,
- bait interrupt,
- kite,
- Fire resistance,
- pressure po wejsciu.

Tradeoff:

- nie jest najlepszym healerem ani pelnym tankiem.

Level 50 Upgrade Identity:

- Justicar: wiecej melee/Physical pressure.
- Witch Hunter: mocniejszy anti-caster/anti-heal disrupt.

### Rytualista / Warlock Signature: Pact of the Hollow

Function:

- wzmacnia summon/curse gameplay,
- krotkie pact window z wyraznym kosztem.

Tags:

- Minion.
- Curse.
- Chaos.
- Spell.

Damage Type:

- Chaos,
- Physical przez summony.

Resource:

- Faith/Mana/Soul later.

Scaling Direction:

- Faith,
- Intelligence,
- Chaos Mastery,
- Minion Damage,
- Curse modifiers.

PvE Use:

- mocny solo setup: summon tank/pressure, curse, drain.

PvP Use:

- attrition,
- wymuszenie decyzji: cleanse, kill summon, pressure owner.

Counterplay:

- cleanse curse,
- kill summon,
- interrupt ritual,
- burst ownera,
- AoE cleave,
- resource pressure.

Tradeoff:

- wymaga setupu i po oknie pactu moze oslabiac defensywe albo zasoby.

PvP Summon Rules:

- limit aktywnych summonow,
- summon damage vs players ograniczony,
- summony zabijalne,
- owner musi byc realnym celem,
- recast summon ma koszt/cooldown.

Level 50 Upgrade Identity:

- Demonolog: mocniejsze summony i pact windows, wieksza zaleznosc od owner positioning.
- Klatwomistrz: mocniejsze curse/debuff/drain, slabszy direct summon pressure.

## Cross-Class PvP Sanity

Burst:

- wymaga setupu, cooldownu albo ryzyka,
- musi miec punish window.

Healing:

- ma PvP modifier,
- kontrowany anti-heal, silence, interrupt i pressure.

Stealth:

- kontrowany reveal, detection, AoE, tracking i combat timer.

Summons:

- limitowane, zabijalne i zalezne od ownera.

CC:

- ma diminishing returns i Tenacity counterplay.

Mobility:

- ma cooldown i nie moze resetowac kazdej walki bez ryzyka.

Tanks:

- maja nizszy kill pressure i kontry: guard break, flank, DoT, magic pressure.

## Test Cases And Scenarios

Class Skill Kits v0.0.1 powinno przejsc ponizsze scenariusze:

- Gracz po Level 10 czuje wyrazny skok identity wzgledem starterow.
- Kazda klasa ma 4 core skills i pelny PvE solo loop.
- Kazda klasa ma minimum jedna wartosc w party.
- Kazda klasa ma PvP role i counterplay.
- Kazda podklasa ma jeden signature skill i tradeoff.
- Level 50 rozwija wybrana podklase, nie daje drugiej pelnej podklasy.
- Rytualista / Warlock ma realny summon/curse/drain loop.
- Wladca Bestii i Warlock oba uzywaja summonow, ale maja inna role: companion physical utility vs Chaos curse/minion ritual.
- Kapelan moze grac solo wolniej, ale stabilnie.
- Inkwizytor dziala jako Fire/Physical disruptor, nie anti-undead-only nisza.
- Zaden signature skill nie jest bez counterplayu w PvP.
- Zaden skill kit nie wymaga konkretnego unique itemu.

## Powiazania z fundamentami

Ten dokument musi pozostac spojny z:

- docs/combat-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.
- docs/progression-respec-foundation-v0.0.1.md.
- docs/class-subclass-foundation-v0.0.1.md.
- docs/starter-skills-class-progression-foundation-v0.0.1.md.
- docs/mob-boss-encounter-001-foundation-v0.0.1.md.
- docs/pet-companion-minion-system-foundation-v0.0.1.md.

W szczegolnosci:

- Skill tags musza korzystac z ustalonego skill systemu.
- Damage types zostaja bez zmian.
- Level unlocki musza respektowac model 1-10 / 10 / 25 / 50.
- Summony musza respektowac PvP sanity rules.
- Healing, stealth, burst, CC i mobility musza miec counterplay.
- Itemy maja wzmacniac tagi i buildy, nie nazwy klas.

## Assumptions

- Ten dokument jest projektowy, bez plugin configow.
- Nie wpisujemy finalnych liczb balansu.
- Nazwy skilli sa robocze i moga zostac zmienione po ustaleniu lore/UI.
- Starter skills pozostaja osobnym early-game systemem.
- Pelne drzewka skilli i konkretne configi beda osobnym etapem.
