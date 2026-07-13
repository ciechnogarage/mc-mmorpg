# Seasons Prestige And Cosmetics Foundation v0.0.1

## Cel dokumentu

Ten dokument zbiera w jednym miejscu fundament sezonow, prestige i kosmetyki dla
serwera MCMMORPG. Do tej pory sezonowosc, prestiz i kosmetyka byly wzmiankowane w
wielu fundamentach (roadmap, world content, ekonomia, PvP, gildie, reputacja),
ale bez wspolnego opisu systemu. Trzy fundamenty odlozyly tu wprost swoje dlugi:
PvP odlozyl sezonowy ladder i nagrody sezonowe, gildie odlozyly sezonowe rankingi
i resety gildii, a reputacja odlozyla sezonowe resety i nagrody reputacyjne. Ten
dokument je konsoliduje.

Sezony, prestiz i kosmetyka sa warstwa endgame: maja dawac powtarzalne, swieze
cele oraz tozsamosc, bez podnoszenia mocy bojowej. Roadmap wymienia seasonal
goals i cosmetic prestige jako rownolegle drogi endgame obok PvE, PvP, gildii,
reputacji i craftingu.

To nie jest finalny system. To wersja v0.0.1. Nie definiujemy tu dlugosci sezonu,
kalendarza, finalnych progow i nagrod ani formuly rankingu czy plugin configow.
Definiujemy zasady i counterplay dla abuse.

## Filozofia

Zasady prowadzace caly system:

- Sezony daja powtarzalne, swieze cele i prestiz, ale nie power creep.
- Reset sezonowy dotyczy warstw sezonowych (laddery, rankingi, sezonowe punkty),
  nie rdzennej progresji gracza.
- Prestiz i kosmetyka to nagroda tozsamosciowa, nie przewaga statystyk.
- Gracz solo i mala gildia maja sensowna sezonowa sciezke.
- Brak pay-to-win i pay-to-skip: sezonowej przewagi nie da sie kupic za realne
  pieniadze (spojnie z `economy-crafting-loot`).
- Sezon ma dawac cele na tygodnie, nie wymuszac codziennego grindu pod kara
  utraty rdzenia.

## Czym jest sezon

- Sezon to powtarzalny cykl czasowy z wlasnym zestawem seasonal goals.
- W trakcie sezonu gracze i gildie zdobywaja sezonowe punkty, pozycje w ladderze
  i sezonowe nagrody.
- Po zakonczeniu sezonu warstwy sezonowe (ladder, rankingi, sezonowe waluty)
  resetuja sie, a rdzen postaci zostaje nienaruszony.
- Sezon nagradza zarowno udzial, jak i koncowa pozycje, zeby cel mial sens takze
  dla graczy spoza czolowki.
- Dlugosc sezonu i kalendarz sa poza zakresem tej wersji.

## Co reset dotyka, a czego nie

To rozstrzygniecie kluczowe dla anti-FOMO: rdzen gracza nigdy nie znika.

Reset sezonowy dotyczy:

- PvP ladder (dlug z `pvp-foundation`),
- rankingow gildii (dlug z `guilds-foundation`),
- sezonowych leaderboardow,
- sezonowych walut i punktow.

Reset sezonowy nie dotyka:

- poziomu postaci,
- wyboru klasy i podklasy,
- zdobytego gearu,
- profesji i ich progresji,
- trwalych unlockow (recipes, region access, vendor unlocks),
- zdobytej kosmetyki, tytulow i prestige.

Gracz, ktory wraca po sezonie, nie traci postaci ani dorobku. Resetuje sie tylko
to, co z zalozenia jest sezonowe i konkurencyjne.

## Prestige

- Prestiz to horyzontalny, prestizowy tor: cosmetic prestige z `roadmap` i
  `world-content-loop`.
- Nosnikiem prestige jest Gameplay Premium Currency opisana w
  `economy-crafting-loot` (rzadsza waluta gameplayowa). Zrodla i wartosci sa
  zdefiniowane tam, zeby nie duplikowac liczb.
- Prestiz zdobywa sie za trudne osiagniecia (bossy, guild wars, seasonal goals),
  nie za sam czas spedzony w grze.
- Prestiz nie daje broken combat power. Buduje status i tozsamosc, nie liczby.

## Kosmetyki

- Kosmetyka to warstwa nagrod tozsamosciowych zasilana z wielu zrodel: PvP,
  gildie, reputacja, bossy, sezony (spojnie z walutami w `economy-crafting-loot`).
- Kosmetyka nigdy nie daje przewagi bojowej.
- Kosmetyki nie kupuje sie za realne pieniadze w sposob dajacy przewage; jezeli
  cos dawaloby combat power, nie moze byc kupione za realne pieniadze (zasada z
  `economy-crafting-loot`).
- Kosmetyka moze byc bind albo ograniczona w handlu, zgodnie z regulami waluty,
  z ktorej pochodzi.
- Zdobyta kosmetyka jest trwala i nie znika z resetem sezonu.

## Rankingi i laddery

Tu domykamy dlugi z `pvp-foundation` (sezonowy ladder, formula rankingu) i
`guilds-foundation` (rankingi i resety gildii).

- Istnieja sezonowe laddery dla PvP i rankingi dla gildii.
- Pozycja opiera sie na aktywnosci i umiejetnosci, nie na samym czasie grindu.
- Laddery i rankingi resetuja sie co sezon.
- Anti-snowball: dominacja jednego gracza albo jednej gildii nie moze betonowac
  ladderu na staly (spojnie z anti-snowball z `pvp-foundation` i
  `guilds-foundation`).
- Gracz solo i mala gildia maja sensowna pozycje i cele, nie sa z gory odcieci.
- Konkretna formula rankingu i matchmaking sa poza zakresem tej wersji.

## Sezonowe nagrody

- Nagrody przyznaje sie za udzial i za koncowa pozycje.
- Glowne typy nagrod: kosmetyka, prestiz, tytuly i sezonowe waluty.
- Sezonowe nagrody nie sa obowiazkowym best-in-slot. Mocne nagrody maja limity
  zdobywania (spojnie z `economy-crafting-loot`).
- Nagroda za udzial chroni graczy spoza czolowki przed poczuciem, ze sezon nie
  ma dla nich sensu.

## Fairness, anti-FOMO i solo

- Reset nie kasuje rdzennej progresji, wiec przerwa w grze nie niszczy postaci.
- Brak pay-to-skip i pay-to-win: sezonowych pozycji ani prestige nie da sie
  kupic za realne pieniadze.
- Solo gracz i mala gildia maja wlasne sezonowe cele i nagrody za udzial.
- Anti-snowball z `pvp-foundation` i `guilds-foundation` przenosi sie na laddery
  i rankingi sezonowe.
- Sezon stawia cele na tygodnie, nie wymusza codziennego grindu pod groza utraty
  rdzenia.

## Test Cases

- Reset sezonu nie kasuje poziomu, klasy, gearu ani profesji gracza.
- PvP ladder i rankingi gildii resetuja sie po zakonczeniu sezonu.
- Prestiz i kosmetyka nie daja graczowi przewagi bojowej.
- Gracz solo robi sensowny sezonowy progres i dostaje nagrode za udzial.
- Sezonowej przewagi ani prestige nie da sie kupic za realne pieniadze.
- Dominujaca gildia albo gracz trafia na anti-snowball limity w ladderze.
- Zdobyta kosmetyka pozostaje po resecie sezonu.

## Assumptions

- To jest wersja v0.0.1, fundament do dalszego dopracowania.
- Sezony, prestiz i kosmetyka sa warstwa horyzontalna i endgame obok PvE, PvP,
  gildii, reputacji i craftingu.
- Nosnikiem prestige jest Gameplay Premium Currency zdefiniowana w
  `economy-crafting-loot`.
- Nie definiujemy jeszcze dlugosci sezonu, progow, capow, formul ani plugin
  configow.
- Ten dokument konsoliduje decyzje juz obecne w innych fundamentach i nie tworzy
  sprzecznych regul.

## Integration

Ten dokument musi pozostac spojny z:

- docs/player-journey-milestone-roadmap-v0.0.1.md (Endgame Loop, seasonal goals,
  cosmetic prestige).
- docs/world-content-loop-foundation-v0.0.1.md (Endgame paths, Activity Reward
  Rules, prestiz i cosmetics jako reward).
- docs/economy-crafting-loot-foundation-v0.0.1.md (Gameplay Premium Currency jako
  nosnik prestige, cosmetics, limity i zasada braku pay-to-win).
- docs/pvp-foundation-v0.0.1.md (sezonowy PvP ladder i nagrody sezonowe).
- docs/guilds-foundation-v0.0.1.md (sezonowe rankingi i resety gildii).
- docs/reputation-faction-foundation-v0.0.1.md (sezonowe resety i sezonowe
  nagrody reputacyjne).

## Out Of Scope

- Dlugosc sezonu, kalendarz i harmonogram resetow.
- Finalne progi, capy, tabele nagrod i sezonowe waluty (wartosci).
- Formula rankingu PvP, rankingu gildii i matchmaking.
- Plugin configi i mapowanie na konkretne pluginy.
- Konkretne listy kosmetykow, tytulow i nagrod sezonowych.
- Monetyzacja realnymi pieniedzmi i sklep premium.
