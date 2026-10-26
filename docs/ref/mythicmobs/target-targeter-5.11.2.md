# MythicMobs 5.11.2: `@Target` dla gracza-castera (jak naprawdę wybiera cel)
Źródło: dekompilacja (cfr, `MC/MCMMORPG/scratchpad/cfr.jar`) jara world1 `MythicMobs-5.11.2.jar`,
klasa `io/lumine/mythic/core/utils/MythicUtil.class`, metoda `getTargetedEntity(Player)`; wołana przez
`TargetTargeter.getEntities`, gdy caster jest graczem. Data: 2026-10-26.
Dokumentacja (`docs/Plugins-wiki-local/mythicmobs/wiki/md/skills-targeters-target.md:10,15`) mówi tylko
"targets the entity the player is looking at, if close enough" i nie ma atrybutów.

## Algorytm (streszczenie kodu, nie cytat w całości)
1. `range = 32`; `player.getNearbyEntities(32,32,32)` (z async przez `Promise.supplyingSync`), filtr `LivingEntity`.
   Kolejność listy = kolejność z mapy encji świata (nieustalona dla nas).
2. `BlockIterator(player, 32)` od bloku oczu gracza; przerwa na pierwszym bloku `isOccluding() || isSolid()` (poza BARRIER).
3. Dla KAŻDEGO bloku pętla po liście encji; pierwsza encja, której STOPY (`getLocation()`) leżą w pudle
   `bx-0.75..bx+1.75`, `by-1..by+2.5`, `bz-0.75..bz+1.75`, zostaje celem (gracz w CREATIVE pomijany).

## Skutki dla nas
- Encja stojąca obok gracza (sługa, chowaniec, członek drużyny) mieści się w pudle już 1.-2. bloku linii wzroku
  i "przechwytuje" cel, zanim iterator dojdzie do moba, w którego gracz celuje. Gdy w pudle są dwie encje,
  wygrywa ta wcześniej na liście: stąd losowe ~50% (Rozkaz akolity 25.10: 5-9/10).
- Nie ma filtra sojuszników ani promienia; nie patrzy na hitbox, tylko na stopy (niski cel daleko = pudło trafia, wysoki
  cel z głową w linii wzroku, ale stopami >1 bl. niżej niż blok linii = pudło chybia; wniosek z kodu, nie testowany).
- Zamiennik: `@Celownik{r=20;s=0.3}` z pluginu KlasyBron (`plugin-src/klasy-bron/README.md`, sekcja Celownik).
