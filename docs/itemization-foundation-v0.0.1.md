# Itemizacja fundament v0.0.1

## Cel dokumentu

Ten dokument definiuje, jak dzialaja przedmioty na serwerze MCMMORPG: sloty, bronie,
zbroje, rzadkosc, afiksy, wymagania statow, skalowanie, lup, rzemioslo i ograniczenia
PvP. Itemizacja ma byc inspirowana PoE, ale czytelna pod Minecraft i spojna z
fundamentami walka, obrazeniami, obrona oraz systemem umiejetnosci.

Celem jest, zeby przedmioty wzmacnialy style gry przez staty, tagi skilli, typy obrazen
i defensywy, a nie byly tylko prostym wynikiem mocy ekwipunku.

## Sloty ekwipunku

Startowy zestaw slotow:

- Main Hand - bron glowna.
- Off Hand - tarcza, catalyst, off-hand weapon, relikwia.
- Helmet.
- Chestplate.
- Leggings.
- Boots.
- Amulet.
- Ring 1.
- Ring 2.
- Belt.
- Trinket albo Charm.

Sloty Minecraftowe zostaja bazowe, ale system RPG moze dodac akcesoria przez
pluginy albo menu.

Zasady:

- Bron definiuje basic attack, heavy attack i czesc weapon skills.
- Zbroja definiuje glowna defensywe: Armor, Evasion albo Ward.
- Akcesoria sa miejscem na resisty, utility, staty specjalne i style gry-defining affixy.
- Off-hand ma byc waznym wyborem, nie pustym slotem.

## Typy broni

Startowe typy broni:

- One-Handed Sword - szybki melee, balanced, dobry z tarcza/off-handem.
- Two-Handed Sword - wolniejszy, wiekszy cleave i stagger.
- Axe - physical damage, bleed, armor pressure.
- Mace/Hammer - guard break, stagger, armor pressure.
- Dagger - crit, backstab, stealth, poison.
- Bow - projectile physical, crit, dystans.
- Crossbow - wolniejszy burst projectile, armor piercing.
- Staff - caster weapon, spell damage, mana, ward.
- Wand - szybszy caster weapon, cast speed, elemental skalowanie.
- Catalyst/Relic - off-hand dla magii, faith, ward, healing, curses.
- Shield - block, guard, armor, poise.

Zasady:

- Kazdy typ broni powinien miec osobny feel i weapon skill identity.
- Bron nie powinna byc tylko stat-stickiem.
- Bron moze miec wymagania statow.
- Bron moze ograniczac lub odblokowywac konkretne skille.

## Armor Types

Startowe typy zbroi:

- Heavy Armor - Armor, Poise, Guard, slabszy dash, gorszy stealth.
- Medium Armor - balans Armor/Evasion, neutralny movement.
- Light Armor - Evasion, Stealth, lepszy dodge, slabsza ochrona.
- Robes - Ward, Mana, Cast Speed, slaba ochrona fizyczna.

Zasady:

- Typ zbroi wplywa na styl gry.
- Ciezka zbroja nie moze byc najlepsza dla kazdego.
- Szaty nie moga byc tylko slabsza zbroja; musza dawac realny caster benefit.
- Lekka zbroja musi miec sens przez mobility, stealth i evasion.
- Medium armor ma byc kompromisem, nie najlepszym wyborem we wszystkim.

## Rarity System

Startowy system rzadkosc:

- Common - tylko bazowe staty.
- Magic - 1-2 affixy.
- Rare - kilka affixow, glowny typ itemow endgame.
- Epic - mocniejsze albo bardziej wyspecjalizowane rzadki przedmioty.
- Unique - specjalny efekt zmieniajacy gameplay, niekoniecznie najwyzsze staty.

Zasady:

- Rare przedmioty powinny byc trzonem itemizacji.
- Unique przedmioty maja zmieniac style gry, nie byc zawsze najlepsze.
- Common/Magic maja sens glownie w early game, rzemiosla albo jako baza.
- Rarity nie moze automatycznie oznaczac, ze item jest lepszy dla kazdego style gryu.

## Affix System

Itemy moga miec prefixy i suffixy.

Przykladowe prefixy:

- flat damage,
- increased physical damage,
- increased fire/cold/lightning/chaos/poison damage,
- spell damage,
- attack power,
- ability power,
- max health,
- max mana,
- max stamina,
- armor,
- evasion,
- ward.

Przykladowe suffixy:

- resistances,
- crit chance,
- crit damage,
- attack speed,
- cast speed,
- cooldown reduction,
- movement speed,
- tenacity,
- poise,
- stealth,
- detection,
- cleanse strength.

Zasady:

- Affixy powinny wspierac tagi skilli.
- Item nie powinien rollowac calkowicie losowych statow bez sensu dla swojej bazy.
- Ciezka zbroja latwiej rolluje Armor/Poise/Guard.
- Lekka zbroja latwiej rolluje Evasion/Stealth/Movement.
- Robes latwiej rolluja Ward/Mana/Cast Speed.
- Bronie rolluja ofensywne staty pasujace do typu broni.

## Stat Requirements

Itemy moga miec wymagania statow.

Przyklady:

- Heavy weapons wymagaja Strength.
- Daggers, bows i light armor wymagaja Dexterity.
- Staffs, wands i robes wymagaja Intelligence.
- Relics, healing catalysts i faith ekwipunek wymagaja Faith.
- Heavy armor moze wymagac Strength albo Vitality.

Zasady:

- Wymagania statow wspieraja style gry identity.
- Wymagania nie moga calkowicie zabijac hybryd.
- Najmocniejsze przedmioty moga wymagac inwestycji w konkretna sciezke.
- Respec musi uwzgledniac, ze zmiana statow moze zdjac przedmioty.

## Item Scaling With Skills

Itemy powinny wzmacniac skille przez tagi.

Przyklady:

- +% Melee Damage wzmacnia skille z tagiem Melee.
- +% Projectile Damage wzmacnia luki, kusze i projectile spelle, jesli skill ma tag Projectile.
- +% Fire Damage wzmacnia wszystkie skille Fire.
- +% DoT Damage wzmacnia Bleed, Burn, Poison i inne DoT.
- +% Trap Damage wzmacnia skille Trap.
- +% Minion Damage wzmacnia summon style gry.
- +% Healing Power wzmacnia healing skille.
- + Ward wzmacnia defensive caster style gry.

Zasada: item wzmacnia tag albo mechanike, nie tylko nazwe klasy.

## Unique Items

Unique przedmioty moga zmieniac zasady style gryu.

Przyklady efektow:

- Backstab naklada Poison.
- Fire skills maja mniejszy damage, ale zostawiaja Burn ground.
- Ward regeneruje sie wolniej, ale chroni tez przed Physical burstem.
- Block moze odbic czesc projectile, ale kosztuje wiecej staminy.
- Bleed zadaje wiecej obrazen celom z niskim HP.
- Heal naklada maly Ward, ale ma mniejszy direct healing.
- Dash zostawia krotki smoke effect, ale ma dluzszy cooldown.

Zasady:

- Unique item nie powinien byc czystym ulepszaniaem.
- Unique powinien miec tradeoff.
- Unique moze odblokowac style gry, ale nie moze byc wymagany do podstawowej gry.
- Unique musi miec PvP sanity check.

## Rzemioslo And Upgrading

Na start planujemy prosty system rzemiosla i ulepszania, bez pelnego PoE chaosu.

Proponowane mechaniki:

- ulepszanie rzadkosc albo tieru itemu,
- reroll pojedynczego affixu,
- dodanie affixu, jesli item ma wolny slot,
- ulepszania wartosci affixu w ramach tieru,
- socket/enchant jako pozniejsza warstwa,
- salvaging niepotrzebnych itemow na materialy.

Zasady:

- Rzemioslo ma pomagac dopracowac style gry, nie drukowac najlepsze przedmioty bez ryzyka.
- Endgame przedmioty moga wymagac dropu + rzemiosla.
- Rzemioslo powinien miec koszt materialow i golda.
- PvP nie powinno wymagac ekstremalnego rzemiosla, zeby wejsc do gry.

## Lup Progression

Lup powinien miec czytelna progresje.

Etapy:

- Early Game - proste bronie, podstawowe staty, pierwsze resisty.
- Mid Game - rzadki przedmioty, pierwsze synergie tagow, stat requirements.
- Late Game - wyspecjalizowane rare/epic przedmioty, style gry-defining affixy.
- Endgame - unique przedmioty, craftowane rzadki przedmioty, celowe farmienie.

Zasady:

- Lepszy lup powinien otwierac style gry, nie tylko zwiekszac liczby.
- Gracz powinien rozumiec, dlaczego item jest dobry dla jego style gryu.
- Lup table powinien byc powiazany z regionem, dungeonem, bossem albo typem mobow.
- Celowe farmienie jest lepszy niz czysty globalny random.

## PvP Item Rules

PvP wymaga osobnych ograniczen.

Zasady:

- Resisty maja cap.
- Penetracja ma cap.
- Healing power ma PvP modifier.
- Anti-heal ma limit, zeby healer nie byl bezuzyteczny.
- Movement speed ma cap.
- Cooldown reduction ma cap.
- Stealth/detection musza miec counterplay.
- Unique effects musza miec PvP sanity check.
- Ekwipunek advantage ma dawac przewage, ale nie natychmiastowy one-shot.

## Test Cases And Scenarios

Itemizacja powinna przejsc testy projektowe:

- Heavy Armor Tank - duzo Armor/Poise, ale slabszy dash i stealth.
- Robe Caster - mocny Ward/Mana/Cast Speed, ale slaby przeciw melee pressure.
- Dagger Rogue - crit/backstab/stealth, ale podatny na reveal, AoE i armor.
- Bow Hunter - projectile/crit/range, ale wymaga line of sight i pozycji.
- Styl trucizn - mocny w dlugich walkach, ale kontrowany przez odpornosc na trucizne i oczyszczenie.
- Healer Ekwipunek - wzmacnia healing, ale w PvP nie robi niesmiertelnosci.
- Unique Item - zmienia styl gry, ale ma tradeoff.
- Low Ekwipunek vs High Ekwipunek PvP - high ekwipunek ma przewage, ale low ekwipunek ma realna szanse przez skill, counterplay i dobre decyzje.

## Powiazania z fundamentami

Ten dokument musi byc spojny z:

- docs/combat-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.
- docs/professions-foundation-v0.0.1.md.
- docs/model-lupu-z-lochow-foundation-v0.0.1.md.
- docs/ekwipunek-pierwszego-aktu-foundation-v0.0.1.md.
- docs/prefiksy-sufiksy-pierwszego-aktu-foundation-v0.0.1.md.
- docs/loot-reward-table-001-foundation-v0.0.1.md.
- docs/pet-companion-minion-system-foundation-v0.0.1.md.

W szczegolnosci:

- Bron i zbroja musza wspierac combat loop.
- Affixy musza korzystac z ustalonych typow obrazen, defensyw, statusow i tagow skilli.
- Profesje moga tworzyc, ulepszac i modyfikowac przedmioty, ale nie moga omijac lup/content progression.
- Enchanty, runy, sockety, consumable i crafted ekwipunek wymagaja PvP sanity checkow.
- Pierwsze materialy, zetony bossa i wczesne powiazania ekwipunku dla `loch_001` sa opisane w loot-reward-table-001.
- Pet/minion staty i pet ekwipunek wymagaja osobnych capow, zeby global pet nie stal sie obowiazkowym combat style gryem.
- Itemy nie moga dodawac nowych typow obrazen poza ustalonym zestawem.
- PvP capy musza byc spojne z damage/defense i skill counterplayem.

Nie wdrazac jeszcze konfiguracji pluginow ani klas. To nadal etap projektowy.

## Assumptions

- Itemizacja ma byc inspirowana PoE, ale prostsza i czytelna.
- Rare przedmioty sa trzonem ekwipunek progression.
- Unique przedmioty maja zmieniac gameplay, nie byc zawsze najlepsze.
- Itemy wzmacniaja tagi skilli, typy obrazen, defensywy i zasoby.
- Stat requirements wspieraja style gry identity, ale nie zabijaja hybryd.
- PvP wymaga capow i sanity checkow dla najmocniejszych statow.
