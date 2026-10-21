# Audyt Spójności I Gotowości MMORPG

Data: 2026-07-13  
Repozytorium: `/home/przemek/projects/MC`  
Zakres: repozytorium, stagingowa kopia `MCMMORPG`, istniejące logi, dokumentacja fundamentów i archiwum walidacyjne

## 1. Werdykt

Projekt ma dobrą bazę konfiguracji-as-code, dokumentacji i kontroli zmian, ale nie jest jeszcze spójnym, skalowalnym ani gotowym do testów MVP produktem MMORPG. Największe ryzyko nie leży obecnie w liczbie pluginów, tylko w braku potwierdzonych granic danych, zamkniętej pętli gracza i dowodów runtime.

**Ocena dojrzałości dowodowej: około 40%.** Jest to ocena tego, ile systemu można obecnie potwierdzić, a nie deklarowany procent zaimplementowanego contentu.

Runtime stagingowy został częściowo uruchomiony host-side. Początkowy brak dostępu do `/var/run/docker.sock` był `validation_blockerem` `VAL-RUNTIME-001`, ale został obejściowo zamknięty przez host-side Docker proof. Aktualny blocker produktowy to nieudany economy hook przez Vault oraz niedomknięte przejście Accept w creator E2E.

## Walidacja wykonana 2026-07-13

| Check | Wynik |
| --- | --- |
| `node scripts/repo-hygiene.mjs --audit` | PASS; 55 reguł runtime, 0 artefaktów runtime |
| 4 testy Node repozytorium | PASS; 4/4 |
| `python3 scripts/check-contract-registry.py` | PASS techniczny; 16 kontraktów, 4 blokery |
| `check_modelengine_binding.js` | FAIL; 3 problemy bindingu |
| Docker staging | PASS; MariaDB, `srv-items`, `srv-world` i Velocity healthy |
| CoreTools creator boot | PASS po naprawie składni list YAML |
| Creator/profile E2E | FAIL; login i GUI pass, Accept nie przechodzi do mentora |

Problemy ModelEngine dotyczą `level_1_briarwolf` (brak blueprintu i assetów),
`level_1_glimmer_wisp` (brak wygenerowanych assetów i cache entries) oraz
`level_1_moss_stalker` (brak wygenerowanych assetów i cache entries). Ich
priorytet pozostaje zależny od grafu obowiązkowego vertical slice; sam błąd
statycznego bindingu nie blokuje całej alphy, jeśli model jest opcjonalny.
Runtime ujawnił dodatkowo, że `srv-world` ładuje się, ale `shared.droptable.yml`
zawiera nieobsługiwane typy `mmoitem`, a `srv-items` nie ma działającego
economy provider przez Vault.

## 2. Inwentaryzacja stanu

### Potwierdzone artefakty

| Obszar | Stan | Dowód |
| --- | --- | --- |
| Paper server | obecny | `MCMMORPG/paper.jar`, `server_environment.md` |
| Pluginy | 34 pliki JAR i 33 katalogi konfiguracyjne | `MCMMORPG/plugins`, `docs/plugin_manifest.yaml` |
| Światy | overworld, nether i end obecne | `MCMMORPG/world*` |
| Kod własny | obecny, głównie CharacterStage | `MCMMORPG/plugin-src` |
| Walidacja | rozbudowana, lecz nierówna | `MCMMORPG/_validation` |
| Docker | Velocity, trzy backendy i MariaDB | `MCMMORPG/docker/compose.yml` |
| Resource pack/custom content | częściowo obecny | konfiguracje Nexo/ModelEngine i walidatory |
| Lokalne archiwum | snapshot level-1 | `MCMMORPG/_validation/archive/level_1_template_20260705_184530.tgz` |
| Vendor content | zewnętrzny, nieobecny w checkout | `docs/audits/2026-07-13/vendor-content-report.md` |

### Status systemów

| System | Ocena | Gotowość | Najważniejsza luka |
| --- | --- | ---: | --- |
| Paper/Docker/Velocity | częściowe | 65% | brak świeżego bootu i testu proxy |
| Plugin inventory/compatibility | częściowe | 70% | SNAPSHOT-y i nieuzupełnione licencje |
| CharacterStage/profile | prototyp z kodem | 45% | reflection adapter i brak E2E persystencji |
| MMOCore/MMOProfiles | konfiguracja + adapter | 40% | MySQL wyłączony |
| Combat/MythicLib | projekt + konfiguracja | 30% | brak runtime formula audit |
| MMOItems/MMOInventory | konfiguracja | 40% | niepotwierdzony storage i reward provenance |
| MythicMobs/ModelEngine | content + walidatory | 50% | brak świeżego live spawn i performance evidence |
| MythicDungeons/world | level-1 content | 45% | reset instancji i reward handoff niepotwierdzone |
| Economy/CMI/Vault | częściowo podłączone | 35% | brak pełnego faucet/sink ledgeru |
| Guilds/social | plugin + foundation | 30% | brak zamkniętej funkcji społecznej |
| Quests | design only | 10% | brak authority i adaptera |
| Professions | design only | 10% | brak authority i implementacji |
| PvP/seasons | design only | 10% | brak authority i reset boundary |
| HUD/GUI | pluginy obecne | 35% | brak hierarchii informacji i testu UX |
| Testability | narzędzia częściowe | 50% | brak jednego resetowalnego test world |

## 3. Źródła prawdy i integracje

Rejestr ma 16 kontraktów, ale pozostaje `draft`. Cztery domeny są jawnie zablokowane: questy, profesje, PvP seasons oraz seasons/prestige/cosmetics.

| Domena | Źródło prawdy | Stan |
| --- | --- | --- |
| Profil gracza | MMOProfiles | adapter obserwowany, DB niezweryfikowana |
| Character stage | CharacterStage | kod obecny, runtime flow niezweryfikowany |
| Klasy/progresja | MMOCore | kontrakt oczekuje audytu |
| Combat formulas | MythicLib | wymaga audytu formuł runtime |
| Item definitions | MMOItems | adapter obecny, kontrakt niezamrożony |
| Inventory/storage | MMOInventory | DB niezweryfikowana |
| Waluta | CMI + Vault | kontrakt globalny niezamknięty |
| Loot/rewards | MythicMobs + MythicDungeons | podwójna authority |
| Świat/dungeony | MythicDungeons | reset runtime niezweryfikowany |
| Moby/bossowie | MythicMobs | binding ModelEngine wymaga live proof |
| Pets | MCPets | tylko fallback YAML dla staging MVP |
| Gildie | Guilds | kontrakt oczekuje walidacji |
| Questy | nierozstrzygnięte | P0 dla pełnej pętli |
| Profesje | nierozstrzygnięte | P1, jeśli nie są w MVP |
| PvP seasons | nierozstrzygnięte | P1/P2 zależnie od MVP |
| Seasons/prestige/cosmetics | nierozstrzygnięte | P2 dla pierwszego vertical slice |

Najważniejsze problemy:

1. **Loot ma dwóch potencjalnych właścicieli.** Potrzebny jest jeden reward contract z provenance, antyduplikacją i idempotencją.
2. **Profil, inventory, balance i permissions są współdzielone.** MMOProfiles deklaruje synchronizację części danych, ale MMOProfiles, MMOInventory i MMOCore mają MySQL wyłączony.
3. **Vault/CMI jest mostem, nie kontraktem ekonomii.** Nie określa faucetów, sinków, limitów ani resetów.
4. **PlaceholderAPI i HUD nie mogą być warstwą logiki.** Mogą prezentować stan, ale nie powinny być właścicielem blokad i obliczeń.
5. **ModelEngine wymaga trzech dowodów:** aktywnego odwołania MythicMobs, blueprintu i wpisu `model_id:*` w cache.
6. **CharacterStage ma reflection adapter.** To kontrolowany fallback, ale wymaga testu login/profile/restart.

## 4. Główna pętla gracza

Repozytorium opisuje elementy potrzebne do pętli:

`character/profile -> class -> combat -> mob/dungeon -> loot -> item/progression`

Nie ma jednak jednego potwierdzonego scenariusza łączącego te elementy i dowodzącego zapisu po restarcie. Quest authority, reward contract, professions i część UX pozostają niepodłączone.

### Vertical slice MVP

1. Login przez Velocity i wybór postaci.
2. Jeden hub z jasnym celem pierwszej sesji.
3. Jedna grywalna klasa i jedna alternatywna ścieżka builda.
4. Jeden region z normalnymi mobami i jednym elite archetype.
5. Jedna questline ucząca ruchu, walki, nagrody i ulepszenia.
6. Jeden dungeon z bossem, telegraphami i przynajmniej jedną fazą.
7. Jedna waluta, podstawowy sink i idempotentny reward contract.
8. Jeden item upgrade zmieniający gameplay.
9. HUD pokazujący zdrowie, zasób klasy, cel i powód blokady.
10. Restart, reconnect i reset testowej postaci bez utraty ani duplikacji danych.

### Punkty ryzyka rezygnacji

- gracz nie wie, co zrobić po wyborze postaci;
- combat nie pokazuje związku między statystyką, skillem i nagrodą;
- nagroda zwiększa liczby, ale nie otwiera nowej decyzji;
- dungeon nie ma jasnego celu, telegraphów lub recovery po błędzie;
- restart może utracić profil albo nagrodę;
- systemy questów, profesji i gildii istnieją w dokumentacji, lecz nie mają widocznej funkcji.

## 5. Progresja i itemizacja

Projekt przewiduje co najmniej level postaci, klasę, statystyki, skille, itemy, rarity/tier, profesje, reputacje, pety, gildie, achievementy, regiony, dungeony i sezony. To wystarczająco dużo osi, aby łatwo powstał power creep.

Na etapie MVP należy aktywować tylko:

- level/klasę;
- statystyki i skille;
- item level/rarity;
- dostęp do jednego regionu i jednego dungeonu.

Profesje, pety, reputacje, achievementy, sezony i złożone sety powinny pozostać poza pierwszą pętlą, dopóki nie mają własnych kontraktów i sinków.

Wymagane reguły:

- każdy item ma stabilny ID i jednego właściciela definicji;
- Nexo odpowiada za reprezentację assetu, MMOItems za definicję RPG;
- reward jest idempotentny i śledzalny do źródła;
- stary item ma regułę salvage, migracji albo deprecjacji;
- nowy stat i skill mają opis wpływu na combat budget.

## 6. Priorytety

### P0: blokuje uczciwe MVP

- zatwierdzić właściciela profilu, inventory, balance i permissions;
- uruchomić i zweryfikować migrację jednego pluginu do MariaDB;
- zamknąć reward handoff MythicMobs/MythicDungeons/MMOItems;
- zamknąć login -> combat -> loot -> progression -> restart;
- wykonać świeży, izolowany boot backendu;
- naprawić economy provider przez Vault i potwierdzić balance path.
- naprawić dispatch/condition seam creator E2E, zachowując hard-blocked Accept.

### P1: blokuje skalowalny rozwój

- zamknąć kontrakt questów albo usunąć questy z MVP;
- zdefiniować combat math i limity statystyk;
- ustanowić test world, reset postaci i komendy diagnostyczne;
- zweryfikować ModelEngine binding i koszt encounterów;
- przygotować economy ledger z faucetami, sinkami i antyduplikacją;
- zmierzyć wydajność regionu, dungeonu i grupowego encounteru.

### P2: po vertical slice

- profesje, reputacje, pety i sezonowa progresja;
- pełne PvP i ranking;
- rozbudowane gildie, terytoria i live events;
- monetyzacja, roadmapa i proces community;
- formalny vendor-lock-in, licencje i plan migracji.

## 7. Kryteria alpha

MVP można rozpocząć do zamkniętych alpha testów dopiero, gdy:

- nowy gracz przechodzi pełną pętlę bez wiedzy autora;
- każda nagroda ma jednego właściciela i nie duplikuje się po reconnect;
- profil, inventory i currency przeżywają restart;
- dungeon resetuje się bez odziedziczonego stanu;
- jedna klasa ma dwa sensowne buildy i znaną rolę;
- combat ma opis kolejności obliczeń i testy wartości skrajnych;
- logi nie zawierają nierozwiązanych błędów krytycznych dla pętli;
- test world można zresetować, a postać odtworzyć komendami;
- każdy dotknięty backend ma osobne evidence.

## 8. Następne działania

1. Naprawić economy provider przez Vault i potwierdzić balance path na `items`.
2. Naprawić dispatch/condition seam creator E2E i powtórzyć test profilu/klasy.
3. Wybrać właściciela profile/inventory/balance i wykonać kontrolowany test MariaDB.
4. Zdefiniować reward contract i przeprowadzić dungeon-boss-loot.
5. Dopiero potem rozszerzać klasy, itemy, profesje, PvP i content sezonowy.

## Źródła

- `docs/current-server-plan.md`
- `docs/plugin_manifest.yaml`
- `docs/config_authority_registry.yaml`
- `docs/server_environment.md`
- `docs/database-staging-baseline.md`
- `docs/audits/2026-07-13/production-readiness-audit.md`
- `docs/audits/2026-07-13/evidence-register.yaml`
- `docs/audits/2026-07-13/runtime-startup-report.md`
- `docs/audits/2026-07-13/database-validation-report.md`
- `docs/audits/2026-07-13/vendor-content-report.md`
- `MCMMORPG/START_HERE_DOCKER.md`
