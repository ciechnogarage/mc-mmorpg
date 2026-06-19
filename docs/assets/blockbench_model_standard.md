# Standard modeli Blockbench dla MC MMORPG

Status: standard repozytoryjny  
Zakres: Blockbench -> ModelEngine v4 -> MythicMobs -> Nexo/resource pack -> Paper/Purpur 1.21.x  
Klasyfikacja pracy: T2 dla standardu i asset pipeline; T3, gdy zmiana dotyka staging/live plugin config, economy, permissions, resource pack wdrozonego na graczy albo migracji ID.

## 1. Cel standardu

Ten standard ustala, jak tworzymy modele 3D dla serwera MC MMORPG, aby kazdy asset byl czytelny, wydajny, stabilny i gotowy do integracji. Dotyczy grafikow, builderow, developerow oraz agentow AI pracujacych nad bossami, mobami, NPC, petami, mountami, itemami 3D, bronia, armorami, propsami, dekoracjami, furniture i obiektami dungeonowymi.

Standard ma wymuszac:

- spojne nazwy, skale, pivoty, tekstury, animacje i dokumentacje;
- kompatybilnosc z ModelEngine v4, MythicMobs, Nexo/resource pack i Paper/Purpur 1.21.x;
- latwe testowanie na stagingu przed wdrozeniem;
- brak konfliktow ID i nazw w resource packu;
- profesjonalny wyglad bez niszczenia FPS/TPS;
- assety praktyczne gameplayowo, nie tylko ladne w podgladzie Blockbench.

## 2. Typy modeli

| Typ | Kiedy uzywac | Struktura pliku | Wymagane animacje | Limit wydajnosci | Pliki koncowe |
| --- | --- | --- | --- | --- | --- |
| ModelEngine entity model | Mob, boss, NPC, pet, mount lub obiekt animowany jako encja | Jeden `root`, czytelne bones, poprawne hitbox helpers, tekstury lokalne | Mob minimum: `idle`, `walk`, `attack_01`, `hurt`, `death`; szczegoly nizej | Zalezny od liczby instancji; unikac zbednych bones i bardzo gestych bryl | `.bbmodel`, tekstury, eksport ModelEngine, MythicMobs mob/skill YAML |
| Item model | Item 3D w ekwipunku, drop, klucz, relikt, material | Format Java Block/Item tylko swiadomie; pivot dopasowany do display transform | Zwykle brak; opcjonalnie warianty/animacje resource pack tylko po akceptacji | Bardzo niski koszt, bo itemy moga pojawiac sie masowo | `.bbmodel`/JSON model, `.png`, wpis Nexo/item config |
| Furniture/prop | Obiekt stawiany w swiecie przez Nexo lub podobny system | Oddzielny model i tekstura, jednoznaczny footprint, pivot przy podstawie | Opcjonalne `idle`/loop tylko dla obiektow animowanych | Niski/sredni; wiele propsow moze byc w jednym chunku | Model/texture Nexo, resource pack JSON, config furniture |
| Boss model | Dungeon boss, encounter boss, world boss | ModelEngine entity model z telegraph bones i czytelnym root | `idle`, `walk`, `attack_01`, `attack_02`, `cast`, `special_01`, `special_02`, `stagger`, `enrage`, `death`, `spawn` | Wyzej niz mob, ale kontrolowany; boss jest rzadki, nie wolno jednak robic nieczytelnej bryly | `.bbmodel`, tekstury, ModelEngine export, MythicMobs boss/skills, review |
| Dungeon decoration | Dekoracja dungeonowa, runy, oltarze, zapadnie, bramy | Nexo furniture/prop albo ModelEngine tylko gdy potrzebna animacja encji | Brak albo `idle`, `activate`, `deactivate`, `break` | Niski; dungeon moze miec wiele dekoracji naraz | Nexo/resource pack config, opcjonalnie ModelEngine export |
| NPC | Postac dialogowa, vendor, quest giver | ModelEngine entity albo skin/vanilla entity, zalezy od potrzeb | `idle`, `talk`, `gesture`, `walk` jesli chodzi | Sredni; NPC zwykle stale obecne w hubach | `.bbmodel`, tekstury, ModelEngine/NPC plugin config, README |
| Pet/mount | Towarzysz lub wierzchowiec gracza | ModelEngine entity, mount z `mount_seat` i pivotem siedziska | Pet: `idle`, `walk`, `sit`, `follow`, `happy`, `despawn`; mount: `idle`, `walk`, `run`, `jump`, `mount_idle` | Niski/sredni; wiele petow moze byc w lobby | `.bbmodel`, tekstury, ModelEngine export, pet/mount config, test siedzenia |
| Weapon/tool | Bron, narzedzie, staff, relikt trzymany w rece | Java item model z poprawnymi display transforms; ModelEngine tylko dla specjalnych animowanych efektow | Brak albo warianty skillowe; animacje skilli w MythicMobs/MMOItems | Niski; bron widzi wielu graczy naraz | JSON model, tekstura, Nexo/MMOItems config |
| Armor/cosmetic | Zbroja, czapka, skrzydla, kosmetyk | Format zgodny z uzywanym systemem resource pack/cosmetics; pivoty na czesciach ciala | Opcjonalne loop animacje tylko po akceptacji | Niski/sredni; kosmetyki czesto wystepuja masowo | Modele/tekstury resource pack, config Nexo/cosmetic plugin |

## 3. Format Blockbench

- Moby, bossowie, pety, mounty i animowane NPC tworzymy w formacie kompatybilnym z ModelEngine v4.
- Itemy, bron i narzedzia tworzymy jako Java Block/Item tylko wtedy, gdy maja isc przez resource pack/Nexo jako item model.
- Nie mieszamy przypadkowo formatow Java Block/Item, Bedrock Entity i OptiFine Entity. Zmiana formatu musi byc zapisana w README modelu.
- Kazdy model musi miec poprawnie nazwane bones/groups. Nazwy robocze typu `cube1` lub `bone2` sa blokujace.
- Pivoty musza byc ustawione pod animacje: barki przy barkach, lokcie przy lokciach, glowa w szyi, bron w uchwycie, `root` przy podstawie modelu.
- Skala musi miec sens wzgledem gracza Minecraft. Autor zapisuje wysokosc w blokach i dolacza screen/test porownawczy z graczem.
- Tekstury musza byc podpiete lokalnie, bez bezwzglednych sciezek z komputera autora.
- UV ma byc uporzadkowane, bez losowych nakladek, chyba ze nakladka jest swiadomym mirrorem symetrycznej czesci.
- Model nie moze byc finalny, jesli elementy maja losowe nazwy typu `cube1`, `cube2`, `bone3`, `test`.

## 4. Konwencja nazw

Wszystkie nowe ID i pliki zapisujemy jako `lowercase_snake_case`, bez spacji i bez polskich znakow.

Prefix projektu:

- `mcmmo_` dla assetow globalnych.

Prefix regionu/dungeonu:

- `dr001_` dla `dungeon_ruins_001`;
- `forest001_`, `crypt001_` itd. dla przyszlych regionow lub dungeonow.

Przyklady `model_id`:

- `mcmmo_elite_crypt_warden`
- `dr001_gatewarden_varos`
- `mcmmo_shadow_wolf_pet`

Pliki:

- `mcmmo_elite_crypt_warden.bbmodel`
- `mcmmo_elite_crypt_warden.png`
- `mcmmo_elite_crypt_warden.yml`
- `mcmmo_elite_crypt_warden_modelengine.yml`
- `mcmmo_elite_crypt_warden_mythicmobs.yml`

Standardowe bone names:

- `root`
- `body`
- `head`
- `left_arm`
- `right_arm`
- `left_leg`
- `right_leg`
- `weapon`
- `fx_core`
- `fx_left`
- `fx_right`
- `mount_seat`
- `hitbox_core`

Standardowe animacje:

- `idle`
- `walk`
- `run`
- `attack_01`
- `attack_02`
- `cast`
- `hurt`
- `death`
- `spawn`
- `despawn`
- `special_01`
- `special_02`

Nazwy zakazane:

- `cube1`
- `cube2`
- `bone`
- `bone2`
- `test`
- `nowy_model`
- `final_final`
- `boss_poprawiony`
- `bez_nazwy`

## 5. Standard skali

| Kategoria | Docelowa wysokosc |
| --- | --- |
| Zwykly mob | okolo 1.8-2.2 bloka |
| Elite mob | 2.2-3.0 bloki |
| Mini boss | 2.8-4.0 bloki |
| Boss dungeonowy | 3.5-6.0 blokow |
| Pet | 0.5-1.2 bloka |
| Mount | dopasowany do siedzenia gracza, z czytelnym `mount_seat` |
| Item/weapon | dopasowany do reki Minecraft i display transforms |

Kazdy model musi miec w README notatke o skali oraz screen/test porownawczy z graczem. Dla bossow autor zapisuje rowniez, czy kamera gracza widzi glowe, bron i telegraph ataku z normalnej odleglosci walki.

## 6. Standard wydajnosci

Progi sa praktyczne, a nie matematycznie absolutne. Jezeli asset przekracza prog, musi miec review techniczne i uzasadnienie.

| Klasa | Przyklad | Zalecany cube/poly budget | Bones/groups | Tekstury | Maks. instancje |
| --- | --- | --- | --- | --- | --- |
| Low mob | trash mob, pet lobby | do ok. 40-70 cubes lub niski polycount | do 12-18 | 16x16-64x64 | 20-40 naraz |
| Medium mob | elite, NPC, mount | do ok. 70-140 cubes | do 18-32 | 32x32-128x128 | 5-20 naraz |
| Boss | dungeon boss | do ok. 140-260 cubes | do 32-55 | 64x64-128x128 | 1-3 naraz |
| Hero boss | final boss, cinematic | do ok. 260-400 cubes tylko po review | do 55-80 | 128x128, rzadko wiecej | 1 naraz |

Zasady:

- Nie robimy ciezkich animacji dla mobow, ktore moga wystepowac masowo.
- Unikamy przesadnej liczby bones, szczegolnie malych niezauwazalnych elementow.
- Efekty czasteczkowe, dzwieki i duze skill VFX trzymamy glownie w MythicMobs albo resource packu, nie jako ciezkie obejscia modelowe.
- Detal musi byc widoczny z dystansu gameplayowego. Jezeli gracz go nie widzi, zwykle nie zasluguje na koszt renderowania.
- Boss moze byc bogatszy, ale musi miec czytelna sylwetke, telegraphy i stabilny death/despawn.

## 7. Standard tekstur

- Dopuszczalne typowe rozmiary: 16x16, 32x32, 64x64, 128x128.
- 256x256 wymaga uzasadnienia w README i review technicznego.
- Nie uzywamy 512x512 ani 1024x1024 bez mocnego powodu, testu FPS i akceptacji technicznej.
- Styl: fantasy/MMORPG, czytelne materialy, czytelny kontrast, brak przypadkowego szumu.
- Paleta powinna pasowac do frakcji, biomu albo dungeonu. Dla DR001 priorytetem jest odrebna paleta ruin, mchu, wody/cysterny, void/crown i kamienia.
- Nazwy plikow: `lowercase_snake_case`, bez spacji, bez polskich znakow.
- Tekstura musi byc dolaczona do folderu modelu lub do uzgodnionej struktury resource packa. Nie zostawiamy zaleznosci od lokalnej sciezki autora.

## 8. Animacje wymagane

ModelEngine mob/boss minimum:

- `idle`
- `walk`
- `attack_01`
- `hurt`
- `death`

Boss:

- `idle`
- `walk`
- `attack_01`
- `attack_02`
- `cast`
- `special_01`
- `special_02`
- `stagger`
- `enrage`
- `death`
- `spawn`

Pet:

- `idle`
- `walk`
- `sit`
- `follow`
- `happy`
- `despawn`

Mount:

- `idle`
- `walk`
- `run`
- `jump`
- `mount_idle`

Zasady animacji:

- Animacje musza miec plynne przejscia i przewidywalne pozycje start/end.
- `root` nie moze przesuwac sie przypadkowo. Ruch lokomocji ma byc kontrolowany przez encje lub swiadomie opisany.
- Ataki musza miec czytelny moment impact. Dla kazdego ataku zapisujemy sugerowany `damage frame`.
- Boss musi miec animacje zapowiadajace atak, szczegolnie przed AoE, stunem, cleave, slamem i skillami fazowymi.
- Animacja `death` nie moze blokowac cleanupu moba. Jezeli jest dluga, MythicMobs skill musi miec jawny plan despawn/cleanup.
- Nazwy animacji moga byc specyficzne dla dungeonu, np. DR001 ma `attack_slam`, `shield`, `flood`, `mirror`, `shockwave`, ale README musi mapowac je do standardowych funkcji gameplayowych.

## 9. Hitbox i gameplay

- Model wizualny nie rowna sie hitbox.
- Hitbox ustawiamy po stronie MythicMobs/ModelEngine/Minecraft entity zgodnie z tym, jak gracz ma realnie walczyc.
- Duzy boss musi wygladac imponujaco, ale nie moze oszukiwac gracza. Jezeli miecz, skrzydlo albo ogon wystaje poza hitbox, damage timing i zasieg skilla musza to jasno komunikowac.
- Bron, pazury i efekty moga wystawac poza hitbox, ale skill damage musi byc zsynchronizowany z animacja.
- Kazdy animowany atak wymaga sugerowanego momentu `damage frame`, np. `attack_01: 18/32 tick`.
- Dla mountow testujemy siedzisko, wysokosc kamery, clipping gracza i przejazd przez typowe drzwi/korytarze.

## 10. Integracja z MythicMobs

Model przygotowany pod MythicMobs musi miec:

- jednoznaczny `model_id`;
- animacje nazwane tak, aby skill mogl je wywolac bez zgadywania;
- osobne animacje dla boss skillow i faz;
- dokumentacje assetu z tabela, ktory skill odpala ktora animacje;
- dzwieki i particles opisane jako hooki MythicMobs, chyba ze projektowy pipeline dla danego assetu mowi inaczej.

Tabela do README modelu:

| Animation | MythicMobs skill | Damage timing | Notes |
| --- | --- | --- | --- |
| `attack_01` | `<skill_id>` | `<tick/frame>` | `<tell, range, cleanup>` |
| `cast` | `<skill_id>` | `<tick/frame>` | `<particles/sound hook>` |

Komendy reload/spawn sa zalezne od wersji pluginow i konfiguracji projektu. Agent nie moze podac finalnych komend jako pewnych bez sprawdzenia lokalnych docs/config. W tym standardzie przyklady komend sa placeholderami testowymi.

## 11. Integracja z Nexo / resource pack

- Itemy i modele musza miec stabilne ID. Nie wolno zmieniac ID po wdrozeniu bez migracji.
- Foldery resource packa musza byc lowercase i bez spacji.
- Pliki musza miec unikalne nazwy, najlepiej z prefixem projektu lub dungeonu.
- Kazdy model/item musi miec wpis w manifest/inventory, jezeli projekt dla danego obszaru taki system posiada.
- Po imporcie trzeba sprawdzic, czy Nexo generuje resource pack bez bledow.
- Dla itemow trzymamy spojnosc miedzy Nexo ID, MMOItems ID, nazwa pliku JSON i tekstura.
- Dla furniture sprawdzamy footprint, rotacje, kolizje, interakcje i zachowanie po reconnect.

## 12. Struktura folderow

Docelowa neutralna struktura dla nowych globalnych assetow:

```text
assets/blockbench/source/
assets/blockbench/export/
assets/textures/
assets/modelengine/
assets/nexo/
assets/mythicmobs/
docs/assets/
docs/assets/reviews/
```

Dla kazdego modelu:

```text
assets/blockbench/source/<model_id>/<model_id>.bbmodel
assets/blockbench/source/<model_id>/textures/<model_id>.png
assets/blockbench/source/<model_id>/README.md
assets/blockbench/export/<model_id>/
docs/assets/reviews/<model_id>_review.md
```

Aktualne mapowanie repo:

- Globalny standard dokumentujemy w `docs/assets/`.
- Istniejace importy zewnetrzne sa w `modele/OUTPUT — kopia (2)/` i traktujemy je jako zrodlo/import/reference, nie jako kanoniczny standard nazw.
- DR001 uzywa `content/dungeons/dungeon_ruins_001/`:
  - source/intake ModelEngine: `content/dungeons/dungeon_ruins_001/intake/modelengine/`
  - final/projektowe blueprinty ModelEngine: `content/dungeons/dungeon_ruins_001/modelengine/blueprints/`
  - Nexo config: `content/dungeons/dungeon_ruins_001/nexo/`
  - Nexo/resource pack assets: `content/dungeons/dungeon_ruins_001/resourcepack/`
  - MythicMobs config: `content/dungeons/dungeon_ruins_001/mythicmobs/`
  - QA/review: `content/dungeons/dungeon_ruins_001/qa/`

Jezeli asset nalezy do konkretnego dungeonu, uzywamy struktury tego dungeonu i zapisujemy mapowanie w README modelu. Jezeli asset jest globalny, uzywamy docelowej struktury `assets/...` po utworzeniu jej w osobnym zadaniu.

## 13. README dla kazdego modelu

Kazdy model musi miec `README.md` wedlug szablonu:

```markdown
# Model: <model_id>

Type:
Mob / Boss / Item / Furniture / Pet / Mount / NPC

Purpose:
Do czego sluzy w grze.

Biome/Dungeon/Faction:
Gdzie wystepuje.

Scale:
Wysokosc w blokach.

Files:
- source:
- texture:
- export:
- ModelEngine:
- Nexo:
- MythicMobs:

Animations:
| Name | Duration | Loop | Used by skill | Notes |
| --- | --- | --- | --- | --- |

Gameplay notes:
- hitbox:
- attack range:
- damage frame:
- visual tells:
- sound hooks:
- particle hooks:

Performance:
- texture size:
- approximate cube/bone count:
- intended max simultaneous instances:

Review:
- art approved: no
- technical approved: no
- gameplay approved: no
- imported in staging: no
```

## 14. Checklist jakosci

Blockbench:

- [ ] poprawny format modelu
- [ ] poprawna skala
- [ ] poprawne pivoty
- [ ] poprawne UV
- [ ] brak nazw `cube1`/`bone2`/`test`
- [ ] tekstury podpiete lokalnie
- [ ] animacje nazwane zgodnie ze standardem
- [ ] `idle`/`walk`/`attack`/`hurt`/`death` istnieja, jesli model jest mobem
- [ ] `root` nie przesuwa sie przypadkowo
- [ ] model wyglada dobrze z dystansu

Performance:

- [ ] rozsadny polycount/cube count
- [ ] rozsadna liczba bones
- [ ] tekstura nie jest za duza
- [ ] model nie bedzie masowo lagowal
- [ ] brak ciezkich, zbednych detali

ModelEngine:

- [ ] import przechodzi bez bledow
- [ ] model pojawia sie w grze
- [ ] animacje dzialaja
- [ ] hitbox jest zgodny z gameplayem
- [ ] model nie znika/nie migocze
- [ ] death/despawn dziala

MythicMobs:

- [ ] mob odpala model
- [ ] skille wywoluja wlasciwe animacje
- [ ] damage frame pasuje do animacji
- [ ] particles/sounds sa zsynchronizowane
- [ ] mob dziala po reload/restart

Nexo/resource pack:

- [ ] resource pack generuje sie bez bledow
- [ ] model/item ma poprawne ID
- [ ] tekstury sa widoczne
- [ ] brak konfliktow nazw
- [ ] dziala po reconnect

Staging:

- [ ] testowane na stagingu, nie na live
- [ ] `latest.log` bez bledow modelu
- [ ] screenshot wykonany
- [ ] krotki raport testu zapisany

## 15. Procedura tworzenia modelu krok po kroku

1. Utworz `model_id` zgodny z prefixem projektu, regionu albo dungeonu.
2. Utworz folder modelu w strukturze globalnej albo dungeonowej.
3. Stworz szkic sylwetki i funkcji gameplayowej.
4. Zablokuj skale wzgledem gracza Minecraft.
5. Zbuduj glowne bryly, najpierw czytelna sylwetka, potem detal.
6. Ustaw pivoty wszystkich ruchomych elementow.
7. Nazwij bones/groups zgodnie ze standardem.
8. Zrob UV i sprawdz brak przypadkowych nakladek.
9. Zrob teksture w zatwierdzonym stylu i palecie.
10. Dodaj minimalne animacje dla typu assetu.
11. Eksportuj do uzgodnionego folderu export.
12. Importuj do ModelEngine, jesli asset tego wymaga.
13. Podepnij model do MythicMobs, jesli jest mobem/bossem/NPC/encja.
14. Podepnij item/model do Nexo/resource pack, jesli dotyczy.
15. Uruchom staging smoke test po uzyskaniu zgody na staging.
16. Zapisz review i evidence.
17. Dopiero po akceptacji oznacz asset jako approved.

## 16. Procedura testu staging

Bezpieczny standard:

- Nigdy nie testujemy nowych modeli bezposrednio na live.
- Testujemy na staging serverze. Aktualny standard repo wskazuje `Server — kopia/1.21.11` jako staging, chyba ze task jawnie poda inny target.
- Robimy backup przed testem, jezeli dotykamy pluginow, resource packa lub plikow staging servera.
- Sprawdzamy `latest.log`.
- Wykonujemy spawn test lub item/furniture placement test.
- Sprawdzamy animacje, tekstury, hitbox, skale i reconnect.
- Sprawdzamy resource pack generation/load.
- Sprawdzamy TPS/MSPT, jesli model jest ciezki, bossowy albo moze wystepowac masowo.
- Zatrzymujemy test i zapisujemy wynik w review/QA.

Przykladowe placeholdery komend, nie wykonywac automatycznie:

```text
/meg reload
/mm reload
/mm mobs spawn <mob_id>
/nexo reload
```

Uwaga: komendy zaleza od wersji pluginow i konfiguracji projektu. Agent musi sprawdzic lokalne docs/config przed podaniem finalnych komend albo przed prosba o ich uruchomienie.

## 17. Zakazane praktyki

- Robienie modeli bez nazw i dokumentacji.
- Uzywanie spacji lub polskich znakow w nazwach plikow.
- Wrzucanie wszystkiego do jednego folderu.
- Zmiana ID modelu po wdrozeniu bez migracji.
- Uzywanie ogromnych tekstur bez potrzeby.
- Animacje bez nazw lub z losowymi nazwami.
- Eksport bez testu importu.
- Testowanie od razu na live.
- Tworzenie bossow bez damage tells.
- Robienie modelu, ktorego hitbox oszukuje gracza.
- Mieszanie formatow Blockbench bez swiadomej decyzji.
- Ignorowanie `latest.log`.
- Oznaczanie technicznego placeholdera jako final approved.
- Usuwanie albo nadpisywanie istniejacych assetow bez decyzji i rollbacku.

## 18. Szablon raportu review modelu

```markdown
# Asset Review: <model_id>

Date:
Reviewer:
Asset type:
Status:
DRAFT / TECHNICAL_PASS / ART_PASS / GAMEPLAY_PASS / STAGING_PASS / APPROVED / REJECTED

Files checked:
-

Visual review:
- silhouette:
- readability:
- texture:
- scale:
- style match:

Technical review:
- format:
- bones:
- pivots:
- animations:
- export:
- import:

Gameplay review:
- hitbox:
- attack readability:
- damage timing:
- player fairness:
- boss tells:

Performance review:
- texture size:
- cube/bone count:
- expected simultaneous instances:
- TPS/MSPT notes:

Staging evidence:
- server:
- date:
- commands:
- latest.log:
- screenshots:
- issues:

Decision:
Approved / Needs fixes / Rejected

Required fixes:
1.
2.
3.
```

## 19. Przyklad gotowego modelu

```text
model_id:
mcmmo_elite_crypt_warden

type:
Dungeon Boss

files:
assets/blockbench/source/mcmmo_elite_crypt_warden/mcmmo_elite_crypt_warden.bbmodel
assets/blockbench/source/mcmmo_elite_crypt_warden/textures/mcmmo_elite_crypt_warden.png
assets/modelengine/mcmmo_elite_crypt_warden/
assets/mythicmobs/mobs/mcmmo_elite_crypt_warden.yml
assets/mythicmobs/skills/mcmmo_elite_crypt_warden_skills.yml
docs/assets/reviews/mcmmo_elite_crypt_warden_review.md

animations:
idle
walk
attack_01
attack_02
cast
special_01
enrage
hurt
death
spawn

gameplay:
- duzy boss krypt
- czytelne zamachy
- particles z MythicMobs
- dzwieki z MythicMobs
- damage frame przy koncowce animacji attack_01/attack_02
```

Przyklad zgodny z aktualnym DR001 namespace:

```text
model_id:
dr001_gatewarden_varos

type:
Dungeon Boss

files:
content/dungeons/dungeon_ruins_001/intake/modelengine/dr001_gatewarden_varos.bbmodel
content/dungeons/dungeon_ruins_001/modelengine/blueprints/dr001_gatewarden_varos.bbmodel
content/dungeons/dungeon_ruins_001/mythicmobs/bosses.yml
content/dungeons/dungeon_ruins_001/mythicmobs/skills.yml
content/dungeons/dungeon_ruins_001/qa/dr001_gatewarden_varos_review.md

animations:
idle
walk
attack_slam
shield
roar
death

gameplay:
- gatewarden pierwszego bossa DR001
- slam ma czytelny wind-up
- shield phase odpala addy/rubble
- damage frame zapisany przy `attack_slam`
- particles i dzwieki mapowane w MythicMobs
```

## 20. Definition of Done dla modelu

Model jest gotowy dopiero gdy:

- [ ] source `.bbmodel` istnieje
- [ ] tekstura istnieje
- [ ] README modelu istnieje
- [ ] review istnieje
- [ ] eksport/import dziala
- [ ] ModelEngine laduje model
- [ ] MythicMobs potrafi spawnowac moba z modelem, jesli dotyczy
- [ ] Nexo/resource pack nie ma bledow, jesli dotyczy
- [ ] animacje dzialaja
- [ ] hitbox i skala sa zaakceptowane
- [ ] `latest.log` nie pokazuje bledow assetu
- [ ] screenshot/test evidence zapisane
- [ ] art review zaakceptowany
- [ ] technical review zaakceptowany
- [ ] gameplay review zaakceptowany

## Role i gate dla prac T2/T3

Minimalne role dla wiekszego assetu:

- Product/Design: potwierdza funkcje modelu, styl, czytelnosc i zgodnosc z dungeonem/frakcja.
- Art/Asset: odpowiada za sylwetke, teksture, UV, skale i estetyke.
- Plugin Admin: sprawdza integracje ModelEngine/MythicMobs/Nexo i reload/restart path.
- Combat/Gameplay: sprawdza hitbox, damage frame, attack tells i fairness.
- QA/Ops: prowadzi staging smoke test, log review, rollback note i evidence.

Asset T2/T3 nie jest gotowy, jesli brakuje review roli, staging evidence albo decyzji, ze runtime test jest `waiting-for-live-approval`.

## Zrodla repo i zgodnosc

Ten standard dopasowuje sie do obecnej struktury repo:

- `AGENTS.md` i `docs/ai/standard-map.md` wymagaja klasyfikacji T2/T3, ochrony live i walidacji przez zrodla lokalne.
- `content/dungeons/dungeon_ruins_001/README_ASSETS.md` wymaga finalnych `dr001_*` assetow i traktuje legacy `dungeon_ruins_001_*` jako reference only.
- `content/dungeons/dungeon_ruins_001/modelengine/models.md` definiuje aktywne DR001 ModelEngine ID i wymagane animacje.
- `content/dungeons/dungeon_ruins_001/modelengine/animation_requirements.md` mapuje eventy encounterow na animacje i MythicMobs triggers.
- `content/dungeons/dungeon_ruins_001/resourcepack/IMPORT_TO_STAGING.md` blokuje final apply bez akceptacji final intake i zgody staging.
- `content/dungeons/dungeon_ruins_001/qa/FINAL_ASSET_REVIEW.md` rozroznia technical candidates od final approved art.
