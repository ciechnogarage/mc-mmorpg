# Combat Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje startowy fundament walki dla serwera MCMMORPG. To nie
jest finalny system, tylko wersja v0.0.1, na podstawie ktorej beda pozniej
projektowane typy obrazen, defensywy, itemizacja, klasy bazowe i podklasy.

Walka ma byc dynamiczna, aktywna i czytelna. Inspiracja jest Skyrim, ale system
ma byc lepszy pod multiplayer, PvE, PvP, open world i wojny gildii. Gracz nie
powinien wygrywac samym gear scorem. Ma wygrywac przez timing, pozycjonowanie,
zarzadzanie zasobami, aktywna obrone i znajomosc counterplayu.

## Core Combat Loop

Kazda postac powinna miec podstawowy zestaw akcji:

- Basic Attack - podstawowy atak bronia.
- Heavy Attack - wolniejszy, mocniejszy atak, dobry do przelamywania bloku.
- Block - aktywna obrona, glownie dla melee/tarczy.
- Parry - timingowa obrona z krotkim oknem.
- Counter - kara dla przeciwnika po dobrym bloku, parry, uniku lub przerwaniu.
- Dodge/Dash - krotki unik albo przemieszczenie.
- Class Skills - 3-5 aktywnych umiejetnosci.
- Signature Skill albo Ultimate - mocna umiejetnosc z dluzszym cooldownem.
- Resource - stamina, mana, rage, focus albo faith zaleznie od buildu.

Podstawowy rytm walki:

    atak -> reakcja przeciwnika -> unik/blok/parry -> counter -> skill window

Celem nie jest spam skilli, tylko szukanie okienek do ataku i karanie bledow.

## Aktywne defensywy

### Dodge / Dash

Dash to podstawowe narzedzie unikania.

Zasady:

- Ma krotki zasieg.
- Kosztuje stamine albo ma cooldown.
- Nie moze byc spamowany.
- Daje krotkie okno bezpieczenstwa, ale nie dluga nietykalnosc.
- Sluzy do unikania heavy attackow, projectile, castow i AoE.
- Ciezka zbroja moze skracac dash albo zwiekszac jego koszt.
- Lekka zbroja moze zmniejszac koszt dashu albo przyspieszac jego cooldown.

Rola w PvP:

- Dash jest kontra na wolne ataki i widoczne skille.
- Dash nie moze kasowac kazdej decyzji przeciwnika za darmo.
- Dobry przeciwnik powinien moc baitowac dash i karac gracza po jego uzyciu.

### Block

Block redukuje obrazenia z przodu.

Zasady:

- Najlepiej dziala z tarcza.
- Kosztuje stamine albo guard meter.
- Chroni glownie przed Physical.
- Moze czesciowo blokowac projectile.
- Slabiej dziala na magie bez specjalnych perkow/itemow.
- Ataki od tylu ignoruja albo mocno oslabiaja blok.
- Heavy attack i skille z tagiem Guard Break moga przelamywac blok.

Rola w PvP:

- Dobry przeciw melee burstowi.
- Slaby przeciw flankowaniu, magii, DoT, guard breakom i atakom od tylu.
- Nie moze pozwalac na wieczne turtlowanie.

### Parry

Parry to ryzykowna, timingowa obrona.

Zasady:

- Ma krotkie okno, np. 0.25-0.4 sekundy.
- Dziala na melee i czesc projectile.
- Nie dziala dobrze przeciw duzym AoE, DoT i czesci spellow.
- Udane parry otwiera przeciwnika na counter.
- Nieudane parry zostawia gracza podatnego przez krotki czas.
- Parry powinno wymagac realnego timingu, nie byc latwym spamem.

Rola w PvP:

- Daje skill expression.
- Pozwala dobremu graczowi wygrac przez timing.
- Nie moze byc zbyt latwe, bo zniszczy melee klasy.

### Counter

Counter to nagroda za dobra defensywe.

Zrodla countera:

- udane parry,
- perfekcyjny blok,
- unikniecie ciezkiego ataku,
- przerwanie castu,
- wejscie za plecy przeciwnika po jego bledzie.

Mozliwe efekty countera:

- bonusowe obrazenia,
- krotki stagger,
- guard break,
- reset czesci cooldownu,
- nalozenie debuffa,
- krotkie okno na mocniejszy skill.

Counter ma byc mocny, ale wymagac realnego zagrania.

## Pasywne defensywy

### Armor

Redukuje Physical Damage.

Dobre dla:

- Wojownika,
- Inkwizytora,
- ciezkich melee buildow,
- tankow.

Slabosci:

- magia,
- poison,
- chaos,
- armor penetration,
- DoT,
- flankowanie,
- ataki od tylu.

### Evasion

Pomaga unikac atakow fizycznych/projektili albo zmniejsza ich skutecznosc.

Dobre dla:

- Lowcy,
- Lotrzyka,
- lekkich buildow,
- mobilnych postaci.

Slabosci:

- AoE,
- DoT,
- czesc spellow,
- reveal/tracking,
- presja obszarowa.

### Ward

Magiczna oslona przeciw spellom i burstowi.

Dobre dla:

- Maga,
- Akolity,
- casterow,
- buildow w szatach.

Zasady:

- Chroni przed spell burstem.
- Moze dzialac jak regenerujaca sie magiczna bariera.
- Regeneruje sie po czasie bez otrzymywania obrazen.
- Moze byc przelamany przez ciagla presje.
- Nie moze byc drugim paskiem HP bez counterplayu.

Slabosci:

- silence,
- interrupt,
- chaos damage,
- ward break,
- melee pressure,
- szybkie wejscie w casterow.

### Tenacity

Stat przeciw CC.

Zasady:

- Skraca stun, root, slow, silence i inne efekty kontroli.
- Jest szczegolnie wazne w PvP.
- Ma limit, zeby nie usuwac kontroli calkowicie.
- Moze dzialac mocniej w PvP niz PvE.

## Resources

### Stamina

Dla fizycznych klas i aktywnej defensywy.

Zuzywana przez:

- dash,
- block,
- heavy attack,
- parry,
- sprint/combat movement.

Jesli stamina spadnie do zera:

- gracz nie moze skutecznie blokowac,
- dash moze wejsc na wiekszy cooldown,
- gracz moze dostac krotkie exhaustion,
- przeciwnik ma okno do agresji.

### Mana

Dla casterow.

Zuzywana przez:

- spelle,
- ward,
- teleporty,
- magiczne buffy,
- mocniejsze defensive casty.

Mana nie ma byc tylko limitem spamowania. Ma wymuszac decyzje:

- uzyc many na burst,
- zachowac ja na ward,
- uzyc jej na ucieczke,
- zagrac oszczedniej i poczekac na regeneracje.

### Rage / Focus / Faith

Zasoby klasowe do zaprojektowania pozniej.

Przykladowo:

- Rage dla Berserkera: rosnie w walce i przy otrzymywaniu/zadawaniu obrazen.
- Focus dla Lowcy/Lotrzyka: rosnie przez trafienia, uniki i precyzyjne zagrania.
- Faith dla Akolity: rosnie przez leczenie, osady, rytualy albo walke z oznaczonym celem.

## Crowd Control

CC musi byc mocno kontrolowane, bo PvP bedzie wazne.

Typy CC:

- Slow - zmniejsza ruch.
- Root - blokuje ruch, ale pozwala atakowac.
- Stun - pelna kontrola, bardzo krotka.
- Silence - blokuje czary.
- Disarm - blokuje bron fizyczna.
- Knockback - odrzut.
- Pull - przyciagniecie.
- Stagger - bardzo krotka przerwa po ciezkim trafieniu.

Zasady PvP:

- Stun nie moze byc dlugi.
- Chain CC musi miec diminishing returns.
- Po kilku efektach kontroli gracz dostaje krotka odpornosc.
- Root i silence sa zdrowsze niz dlugi stun.
- Mocne CC musi miec cooldown, warunek trafienia albo widoczny tell.
- Perma-control jest zakazany projektowo.

## Stealth / Skradanie

Stealth jest mechanika walki i eksploracji, nie tylko pojedynczym skillem
niewidzialnosci.

Zalozenia:

- Gracz w trybie skradania porusza sie ciszej.
- Kroki sa wyciszone albo duzo slabiej slyszalne.
- Nameplate/nick moze byc ukryty albo widoczny z mniejszego dystansu.
- Gracz porusza sie wolniej.
- Atak z ukrycia daje bonus do kryta.
- Atak w plecy daje dodatkowy bonus do kryta albo obrazen.
- Najwiekszy bonus jest za polaczenie: atak z ukrycia + trafienie w plecy.
- Ciezka zbroja utrudnia skradanie.
- Lekka zbroja i pasywki moga wzmacniac stealth.
- Otrzymanie obrazen, atak albo uzycie glosnego skilla przerywa stealth.

Stealth ma sluzyc do:

- inicjacji,
- flankowania,
- ucieczki,
- zasadzki,
- omijania czesci PvE,
- scoutingu.

Nie ma byc permanentna niewidzialnoscia bez kontr.

## PvP counterplay dla stealth

Kontry na stealth:

- Reveal - skill albo item ujawniajacy ukrytych graczy w obszarze.
- Tracking - wykrywanie sladow albo kierunku ruchu.
- AoE Damage - trafienie obszarowe moze wyrwac gracza ze stealth.
- Bleed/Poison/Burn - aktywny DoT moze blokowac wejscie w stealth.
- Heavy Armor Noise - ciezka zbroja zwieksza szanse wykrycia.
- Light Level - w jasnym miejscu trudniej sie ukryc, w ciemnosci latwiej.
- Combat Timer - ogranicza ciagle znikanie w srodku walki.

## Backstab / Flanking

Backstab premiuje trafienie w plecy przeciwnika.

Zasady:

- Atak od tylu moze miec bonus do crit chance albo crit damage.
- Sztylety i lekkie bronie dostaja najwiekszy bonus.
- Ciezkie bronie moga miec mniejszy bonus, ale wiekszy stagger.
- Tarcza i block dzialaja glownie od przodu.
- Ataki od tylu moga ignorowac czesc bloku.
- Bossy moga miec ograniczona podatnosc na backstab, zeby uniknac cheesowania.

To daje sens pozycjonowaniu, obracaniu sie do przeciwnika i ochronie plecow w
PvP.

## Noise / Halas

Kazda akcja moze generowac halas.

Przyklady:

- sprint - duzy halas,
- ciezka zbroja - wiekszy halas,
- skradanie - maly halas,
- strzal z luku - sredni halas,
- magia ognia/piorunow - duzy halas,
- sztylet - maly halas,
- eksplozje i ciezkie ataki - bardzo duzy halas.

Halas wplywa na:

- wykrywanie przez moby,
- wykrywanie przez graczy,
- skutecznosc stealthu,
- tracking Lowcy,
- zasadzki Lotrzyka.

## Detection / Wykrywanie

Wykrywanie nie powinno byc czysto losowe.

Wplywaja na nie:

- odleglosc,
- linia wzroku,
- poziom swiatla,
- halas,
- typ zbroi,
- ruch gracza,
- staty typu Stealth,
- staty typu Detection,
- aktywne efekty reveal/tracking.

Przyklad: gracz w lekkiej zbroi, kucajacy w ciemnosci za plecami celu, jest
trudny do wykrycia. Gracz w ciezkiej zbroi, sprintujacy po kamieniu, powinien
byc latwy do wykrycia.

## Attack Design

Kazdy mocny atak powinien miec przynajmniej jeden counterplay.

Zasady:

- wolny cast mozna przerwac,
- projectile mozna ominac,
- melee heavy mozna sparowac albo odejsc,
- AoE ma widoczny obszar,
- DoT mozna oczyscic,
- summonera mozna nacisnac bezposrednio,
- healer moze dostac anti-heal albo silence,
- stealth opener mozna skontrowac revealem, trackingiem albo dobrym pozycjonowaniem.

To jest kluczowa zasada pod PvP.

## Dodatkowe mechaniki

### Poise / Stagger Resistance

Poise okresla odpornosc na wytracenie z animacji.

Zasady:

- ciezka zbroja daje wiecej poise,
- lekka zbroja daje mniej poise,
- heavy attack moze powodowac stagger,
- mag moze zostac przerwany, jesli nie ma wardu albo poise,
- bossy i tanki moga miec wieksza odpornosc na stagger.

### Guard Break

Mechanika przeciwko turtlowaniu za tarcza.

Zasady:

- heavy attack niszczy guard szybciej,
- niektore skille maja tag Guard Break,
- po przelamaniu bloku gracz dostaje krotki stagger,
- guard regeneruje sie po czasie,
- guard break jest naturalna kontra na defensywnego tanka.

### Interrupt

Przerywanie castow.

Zasady:

- silne czary maja cast time,
- trafienie konkretnym skillem moze przerwac cast,
- ward albo pasywki moga chronic przed interruptem,
- instant spelle sa slabsze,
- castowane spelle sa mocniejsze, ale ryzykowne.

### Status Effects

Statusy sa powiazane z typami obrazen.

Przyklady:

- Bleed - physical DoT, moze byc mocniejszy gdy cel sie rusza.
- Burn - fire DoT.
- Poison - stacking DoT, dobry w dluzszych walkach.
- Shock - cel otrzymuje wieksze obrazenia przez krotki czas.
- Chill - spowolnienie ruchu i attack speed.

Kazdy status powinien miec odpornosc, cleanse albo inny counterplay.

### Execute

Mechanika dobijania.

Zasady:

- niektore skille sa mocniejsze, gdy cel ma malo HP,
- dobre dla Zabojcy, Berserkera albo Inkwizytora,
- execute nie moze zabijac z polowy HP,
- execute ma byc finisherem, nie calym gameplayem.

### Line Of Sight

Widocznosc ma znaczenie.

Zasady:

- nie mozna rzucac czesci czarow przez sciany,
- luk wymaga czystej linii strzalu,
- gracz moze schowac sie za przeszkoda przed projectile,
- AoE moze dzialac inaczej zaleznie od przeszkod,
- line of sight jest wazne dla PvP na mapie.

### Armor Weight

Waga zbroi wplywa na styl gry.

Typy:

- ciezka zbroja: wiecej Armor/Poise, slabszy dash, gorszy stealth,
- srednia zbroja: balans,
- lekka zbroja: lepszy dodge, stealth i evasion,
- szaty: lepszy Ward, mana i cast speed, slaba ochrona fizyczna.

Armor weight pozwala robic buildy bez sztywnego zamykania klas.

## PvP Rules

Balans bazowy robimy pod 1v1/3v3, ale system musi dzialac tez w open world i
wojnach gildii.

Zasady:

- leczenie w PvP ma obnizona skutecznosc,
- CC ma diminishing returns,
- AoE damage moze miec soft cap na wielu graczy,
- summony maja ograniczona liczbe i mniejsza skutecznosc przeciw graczom,
- tanki maja wysoka przezywalnosc, ale nizszy kill pressure,
- burst klasy musza miec moment wejscia i moment slabosci po nieudanym wejsciu,
- combat timer ogranicza ucieczke/logowanie w walce,
- gear scaling w PvP powinien ograniczac one-shotowanie slabszych graczy.

## Braki do pilnowania przy dalszym projektowaniu

Te rzeczy musza wrocic przy projektowaniu klas, itemow i PvP:

- Poise/Stagger Resistance - zeby kontrolowac przerywanie animacji.
- Guard Break - zeby blok/tarcza nie byly zbyt mocne.
- Interrupt - zeby casterzy mieli counterplay.
- Cleanse - zeby usuwac poison, bleed, curse, burn.
- Anti-heal - konieczne pod PvP z healerami.
- Reveal - kontra na stealth.
- Tracking - kontra i gameplay Lowcy.
- Combat Timer - zeby nie uciekac/logowac sie bez konsekwencji.
- Diminishing Returns - konieczne dla CC.
- Line of Sight - zeby mapa miala znaczenie.
- Friendly Fire - domyslnie raczej wylaczone, chyba ze specjalny tryb gildii.
- Gear Scaling PvP - zeby roznica gearu nie robila natychmiastowego one-shota.
- Server Latency - parry/dodge musza miec wybaczalne okna, bo to Minecraft server, nie singleplayer.

## Assumptions

- To jest wersja v0.0.1, czyli fundament do dalszego dopracowania.
- Walka ma byc dynamiczna, aktywna i czytelna.
- Inspiracja Skyrimem dotyczy bloku, parry, many, broni, magii, stealthu i ciezaru ekwipunku.
- PvP jest kluczowe, wiec kazda mocna mechanika musi miec counterplay.
- Stealth jest pelna mechanika, nie tylko pojedynczym skillem.
- Nie projektujemy jeszcze klas, dopoki typy obrazen, defensywy i itemizacja nie sa ustalone.
