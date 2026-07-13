# Foundation MVP — Spec twardych wartości

Cel: jedyne źródło prawdy dla **konkretnych wartości** potrzebnych do implementacji
vertical slice (Stolica Wyspy → Portal Nexus → `level_1` → boss → reward → return).
Foundation-docy projektowe są na ~40% konkretności; ten plik domyka je do wartości
gotowych do wpisania w config, bez zgadywania przez agentów.

Zasada źródła:
- **[CONFIG]** = wartość skopiowana z istniejącego, działającego configu. Nie zmieniać bez powodu.
- **[DECYZJA]** = wartość ustalona przez integratora, bo brak jej w źródłach. Można dostroić.

Powiązane: `docs/ai/foundation-mvp-implementation.md`, `docs/ai/city-hub-blockout-v0.0.1.md`,
`docs/ai/foundation-long-run-ledger.md`. Konsumują ten plik zadania faz B–E.

---

## 1. Świat i siatka miasta (B1)

Świat hubu: `world` (Multiverse-Core), `level-name=world`, `online-mode=false`. **[CONFIG]**
Dungeon `level_1` to osobna instancja (START `0,64,10`) — **nie koliduje** z hubem. **[CONFIG]**

Kompaktowe miasto, oś główna wzdłuż +Z (gracz idzie od spawnu na północ do Portal Nexus).
Koordynaty bazowe **[DECYZJA]** (X, Y, Z); Y orientacyjne — B1 wyrównuje platformę do realnego terenu:

| Punkt / dystrykt | Środek (X, Y, Z) | Rola |
| --- | --- | --- |
| Spawn Gate | `0, 103, 0` | punkt spawnu gracza (zgodny z Multiverse spawn) |
| Market/Gear + Profession spine | `0, 103, 24` | usługi gear/repair/salvage/stash + craft |
| **Portal Nexus (centrum)** | `0, 103, 48` | najsilniejszy landmark, selektor lochów |
| Skill/Class District | `-24, 103, 48` | starter skille + mentorzy klas |
| Quest/Info Board | `24, 103, 48` | tablica + wskaźniki miasta |
| Temple District | `0, 103, 72` | świątynia/kapliczki |

Wymóg topologii **[CONFIG z blockout]**: dystrykty widoczne z ulic, gracz przechodzi pętlę pieszo
bez teleportu; spawn musi „widzieć" drogę do Portal Nexus.

Region hubu (WorldGuard): `hub_stolica` obejmujący prostokąt ~`(-40..40, _, -8..88)`, flagi:
`pvp deny`, `mob-spawning deny`, `build deny` dla default. **[DECYZJA]**

---

## 2. Roster 17 NPC (B2) — MythicMobs, AI off, nieruchome

Plik docelowy: `plugins/MythicMobs/mobs/hub_npcs.yml`. Wszystkie: typ bazowy `VILLAGER`
(lub stosowny), `Options: {AlwaysShowName: true, NoAI: true, Despawn: false, Persistent: true}`,
`Health: 1000` (nie do zabicia w hubie — region i tak blokuje dmg). **[DECYZJA]**
ID = nazwa z blockout roster **[CONFIG]**; koordynaty i dialog **[DECYZJA]**.

| MythicMob ID | Dystrykt | Koord (X,Y,Z) | Display | Dialog (1-2 linie) |
| --- | --- | --- | --- | --- |
| `CityGuide` | Spawn Gate | `0,103,3` | `<yellow>Przewodnik</yellow>` | „Witaj w Stolicy Wyspy. Portal Nexus jest prosto na północ." |
| `GearVendor` | Market | `-5,103,24` | `<aqua>Handlarz Ekwipunku</aqua>` | „Podstawowy sprzęt na start? Mam to." |
| `RepairKeeper` | Market | `-2,103,24` | `<aqua>Kowal Napraw</aqua>` | „Przynieś zniszczony sprzęt, naprawię." |
| `SalvageKeeper` | Market | `2,103,24` | `<aqua>Złomiarz</aqua>` | „Rozłożę zbędny łup na materiały." |
| `StashKeeper` | Market | `5,103,24` | `<aqua>Skarbnik</aqua>` | „Twój bank i schowek." |
| `StarterSkillTrainer` | Skill/Class | `-24,103,44` | `<light_purple>Trener Umiejętności</light_purple>` | „Wybierz styl startowy, nim ruszysz w loch." |
| `WarriorMentor` | Skill/Class | `-28,103,48` | `<gray>Mentor Wojownika</gray>` | „Klasa otworzy się na 10 poziomie." |
| `RogueMentor` | Skill/Class | `-26,103,50` | `<gray>Mentor Łotrzyka</gray>` | „Klasa otworzy się na 10 poziomie." |
| `RangerMentor` | Skill/Class | `-22,103,50` | `<gray>Mentor Łowcy</gray>` | „Klasa otworzy się na 10 poziomie." |
| `MageMentor` | Skill/Class | `-20,103,48` | `<gray>Mentor Maga</gray>` | „Klasa otworzy się na 10 poziomie." |
| `AcolyteMentor` | Skill/Class | `-24,103,52` | `<gray>Mentor Akolity</gray>` | „Klasa otworzy się na 10 poziomie." |
| `PortalKeeper` | Portal Nexus | `0,103,46` | `<gold>Strażnik Portalu</gold>` | „Wybierz loch. Na start: Kwietna Polana (level_1)." |
| `BoardClerk` | Quest/Info | `24,103,46` | `<yellow>Urzędnik Tablicy</yellow>` | „Zlecenia i wskazówki. Po loch — idź do Portalu." |
| `ShrineKeeper` | Temple | `0,103,70` | `<white>Strażnik Kapliczki</white>` | „Bóstwa czekają. Błogosławieństwa wkrótce." |
| `ForgeKeeper` | Profession | `-5,103,18` | `<aqua>Mistrz Kuźni</aqua>` | „Kucie i rzemiosło — podgląd na razie." |
| `Alchemist` | Profession | `-2,103,18` | `<aqua>Alchemik</aqua>` | „Mikstury i zioła." |
| `Runesmith` | Profession | `2,103,18` | `<aqua>Runarz</aqua>` | „Runy i zaklęcia." |
| `Artisan` | Profession | `5,103,18` | `<aqua>Rzemieślnik</aqua>` | „Klejnoty i ozdoby." |

Reguła **[CONFIG z blockout]**: NPC tylko MythicMobs (nie Citizens), resetowalne, nie wpiekane w template.

---

## 3. Portal Nexus jako obiekt + board (B2/B3)

- Fizyczny portal: ramka/struktura w `0,103,48`, interakcja (prawy klik bloku/NPC `PortalKeeper`)
  otwiera **istniejące** menu DeluxeMenus `foundation_nexus`. **[CONFIG: menu już działa]**
- Akcja menu Portal Nexus → `[player] md play level_1`. **[CONFIG: foundation_nexus.yml:88]**
- Board (Quest/Info): tablica/hologram w `24,103,48` z wskaźnikami; **nie** startuje lochu, tylko
  kieruje do Portal Nexus. **[CONFIG z blockout]**

---

## 4. Boss, moby, difficulty (C2) — z `mobs/level1_grove.yml` + `maps/level_1/config.yml`

Wszystko **[CONFIG]**:

| Mob | Type | Health | Damage | Armor | Uwagi |
| --- | --- | --- | --- | --- | --- |
| `level_1_grove_guardian` | ZOMBIE | 400 | 12 | 6 | Display `Strażnik Gaju`; LevelModifiers Health+180/Dmg+3.5/Armor+2 |
| `GroveWolf` | WOLF | 30 | 5 | — | trash, MovementSpeed 0.32 |
| `CorruptedSprout` | HUSK | 20 | 4 | — | trash, ThornShot ranged |

Difficulty (mnożniki MD, `config.yml` Levels) **[CONFIG]**:

| Poziom | MobHealth | MobAmounts | MobDamage | BonusMythicLevels | BonusLoot |
| --- | --- | --- | --- | --- | --- |
| EASY | 1.0 | 1.0 | 1.0 | 0 | 0 |
| NORMAL | 1.4 | 1.3 | 1.25 | 1 | 1-2 |
| HARD | 1.9 | 1.6 | 1.6 | 2 | 2-4 |

Boss enrage (próg HP, `~onTimer:10 ?stance{ready} ?health`) **[CONFIG]**:
EASY `<119`, NORMAL `<174`, HARD `<228`. Skille: RootSmash (AoE), SummonAdds, ThornNova (tylko HARD).
Śmierć: `state{...;s=death} @self ~onDeath` → odpala `FunctionFinishDungeon` przez
`TriggerMythicMobDeath: level_1_grove_guardian` w `maps/level_1/functions.yml`. **[CONFIG]**

> **Krytyczne dla A2/E:** finish odpala TYLKO realna śmierć w walce. `/kill` / RCON kill **nie**
> matchuje `TriggerMythicMobDeath`. Walidacja musi zabić bossa realnymi obrażeniami.

---

## 5. Droptable bossa (C2) — z `droptables/level1_grove.yml`

Wszystko **[CONFIG]**. Baza (każda trudność): `STEEL_SWORD` 0.35, `STEEL_INGOT` 2-4 ×1.0,
`RECALL_POTION` 0.5, exp 120, money 50-90. Bonus NORMAL: +STEEL_INGOT 1-2, +exp 60, +money 30-50,
+STEEL_SWORD 0.10. Bonus HARD: +STEEL_INGOT 3-5, +exp 150, +money 90-140, +STEEL_SWORD 0.25,
+RECALL_POTION ×1.0. Trash: STEEL_INGOT 0.15, exp 8, money 1-4 ×0.6.

---

## 6. Starter gear, klasa, money (C1) — z `MMOItems/`, `MMOCore/`, `foundation_nexus.yml`

Wszystko **[CONFIG]**:
- `STEEL_SWORD`: bazowy `IRON_SWORD`, attack-damage 6.5, **required-level 1.0**.
  > **Uwaga dla C1/A2:** nowy gracz lvl 1 < 6 — sprawdzić czy zadaje pełne obrażenia. Jeśli nie:
  > obniżyć required-level startowej broni LUB dać botowi/graczowi item bez gate'u poziomu (decyzja C1).
- `RECALL_POTION`: bazowy `POTION`, cooldown 60s, command `spawn`.
- Klasa default: `Human` (`human.yml`), `default=true`, 4 sloty skilli, exp curve `{level}*200`.
- Starter claim state: `foundation.starter.claimed`; to jest jedyny node stanu używany przez UI i proof backendowy. **[DECYZJA]**
- Grant itemów startowych z menu: `[console] mi give SWORD STEEL_SWORD %player_name% 1` oraz `[console] mi give CONSUMABLE RECALL_POTION %player_name% 1`. **[CONFIG: foundation_nexus.yml]**
- Money startowe z menu: `[console] cmi money give %player_name% 25 -s`. **[CONFIG: foundation_nexus.yml]**
- Claim zapisuje backend przez LuckPerms: `[console] lp user %player_name% permission set foundation.starter.claimed true`. **[CONFIG: foundation_nexus.yml]**

---

## 7. Dungeon `level_1` — parametry instancji (C3) — z `maps/level_1/config.yml`

Wszystko **[CONFIG]**: DisplayName `Kwietna Polana`, StartLocation `0,64,10`, Gamemode SURVIVAL,
PlayerLives 2, KeepInventoryOnEnter true, KeepExpOnEnter true, MaxInstances 5, MaxParty 4,
EnableDifficultyMenu true (EASY/NORMAL/HARD), PvP false, CleanupDelay 0.

---

## 8. Permission/economy gates (D1) — minimalne **[DECYZJA]**

- `mythicdungeons.play.level_1` dla grupy `default`.
- `dungeons.play` i `dungeons.play.send` dla grupy `default`.
- `mmocore.profile` dla grupy `default`, bo alias `profile` jest częścią komendy MMOCore `player`.
- dostęp do menu `foundation_nexus` (DeluxeMenus `open` perm jeśli wymagany) dla `default`.
- `foundation.starter.claimed` ma `default: false` i jest nadawany tylko przez starter flow.
- aktywne źródło prawdy dla gate'ów M1/M2: `MCMMORPG/permissions.yml`; `plugins/CoreTools/core-perms.yml` jest poza ścieżką, bo `core_perms.enabled: false`.
- vendor/reward przez CMI/Vault economy (już provider). Admin-komendy **zamknięte** dla `default`.

---

## Mapa zależności wartości → zadania

- §1 → B1 (build świata). §2,§3 → B2/B3 (NPC, portal, board). §4,§5 → C2 (boss/loot).
  §6 → C1 (starter RPG). §7 → C3 (dungeon flow). §8 → D1 (gates). §4 (próg death) → A2/E (walidacja).
