# ModelEngine 4.0.9 — powtarzalny przepis na model (MCMMORPG)

Serwer: Paper 1.21.11. Pluginy istotne: **ModelEngine 4.0.9**, **MythicMobs 5.11.2**,
**MythicCrucible 2.2.0** (UWAGA: Crucible zmienia sposób podpinania modelu — patrz sekcja 6).

Ten dokument opisuje pełny, powtarzalny proces: od pliku `.bbmodel` do działającego
modelu przypiętego do MythicMoba. Pierwszy zrobiony tą metodą model: `grove_guardian`
(Strażnik Gaju, boss Level 1).

---

## 0. Co to jest `.bbmodel` i jak ModelEngine go czyta

`.bbmodel` to plik **JSON** generowany przez [Blockbench](https://blockbench.net) w
formacie modelu **`free`** (`meta.model_format: "free"`, `meta.box_uv: false`).

ModelEngine NIE wymaga ręcznego budowania resource packa. Wrzucasz `.bbmodel` do folderu
blueprintów, robisz `/meg reload`, a plugin sam:
- parsuje `.bbmodel`,
- generuje modele `assets/modelengine/models/<namespace>/...`,
- buduje/aktualizuje resource pack (`plugins/ModelEngine/resource pack.zip`),
- rejestruje blueprint pod ID = nazwa katalogu/pliku.

Kluczowe sekcje pliku `.bbmodel` (potwierdzone na `internals/blueprints/internal_fire.bbmodel`
i `internals/examples/player_model.bbmodel`):

| Sekcja        | Rola |
|---------------|------|
| `meta`        | `format_version`, `model_format: "free"`, `box_uv: false` |
| `resolution`  | rozmiar tekstury (np. 16x16) |
| `elements[]`  | **cube'y** (geometria): `from`/`to` w pikselach (16 px = 1 blok), `origin` (pivot), `faces` z `uv` i `texture` |
| `groups[]`    | **bones (kości)** — drzewo: `name`, `uuid`, `origin` (pivot kości), `rotation`, `children` |
| `outliner[]`  | hierarchia: które `uuid` cube'ów/grup należą do której grupy |
| `textures[]`  | tekstury (base64 w `source`) |
| `animations[]`| OPCJONALNE — animacje keyframe (idle/walk/attack/death). Brak = model statyczny, nadal poprawny |

> ModelEngine używa **`groups` jako bones**. Nazwa grupy = nazwa kości, do której odwołujesz
> się w animacjach i mechanikach (`bindhitbox`, `hitboxconfig` itd.).

---

## 1. Konwencje nazw kości (bones) — czego ModelEngine szuka po nazwie

Z dokumentacji ModelEngine 4 + przykładu `player_model_v2`:

- **`hitbox`** — kość z JEDNYM cube'em w środku. Rozmiar cube'a = rozmiar hitboxa.
  ModelEngine czyta TYLKO X cube'a (Z ignoruje), więc **X i Z muszą być równe** (hitbox kwadratowy).
  **Eye height** = wartość Y pivotu kości `hitbox`. To też wysokość "headshota" i suffocation.
  Limit: 64x64x64 bloki (1024 px).
- **`b_<nazwa>`** — **sub-hitbox** (np. `b_left_arm`). Jak hitbox, ale nie koliduje z blokami.
  Można podpiąć osobnego MythicMoba: `bindhitbox{m=...;p=b_left_arm;t=...}`. X=Z wymagane.
- **`root`** — korzeń hierarchii (opcjonalny kontener).
- **`mount` / `seat` / `p_<n>`** — punkty siadania (mounty), jeśli model ma być wierzchowcem.
- **`limb[type=...]_<n>`** — kości dla User-Limb (modele graczy). Nie używamy w mobach.
- Dowolne inne nazwy (np. `trunk`, `left_branch`, `crown`) = zwykłe kości animowane.

Pivot (`origin` grupy) to środek obrotu kości — ustaw go tam, gdzie kość ma się "mocować"
do rodzica (np. pivot ręki przy barku, pivot głowy u nasady szyi).

---

## 2. Setup w Blockbench (od zera, GUI)

To jest główny workflow artystyczny dla wszystkich finalnych modeli creature/mob w tym projekcie.
Nie pracuj na pałę przez ręczne wpisywanie liczb jako podstawową metodę iteracji jakościowej.
Skrypty, edycje JSON i narzędzia renderujące mogą służyć do bootstrapu, backupu, analizy albo walidacji,
ale finalna geometria, pivots, outliner, UV i bieżący podgląd jakości mają być prowadzone natywnie w Blockbench.

1. Blockbench → **New Model** → wybierz **"Generic Model"** (to format `free`).
   (Dla modeli graczy: format "Modded Entity"/skin — tu nie używamy.)
2. **File → Project**: ustaw resolution tekstury (16x16 wystarcza na blocky styl).
3. Buduj sześcianami (cube). Skala: **16 pikseli = 1 blok**. Boss ~3-4 bloki = 48-64 px wysokości.
4. Grupuj cube'y w **bones (Outliner → New Group)**. Nazwij wg konwencji z sekcji 1.
   - Ustaw **pivot każdej grupy** (narzędzie Pivot / pole "Pivot Point") w miejscu mocowania.
5. Dodaj kość **`hitbox`** z jednym cube'em o kwadratowej podstawie (X=Z), wysokość = wzrost moba.
   Pivot Y kości `hitbox` = eye height.
6. Tekstura: namaluj/zaimportuj, przypisz UV (Auto UV wystarcza dla blocky).
7. (Opcjonalnie) **Animacje**: zakładka Animate → twórz `idle`, `walk`, `attack`, `death`.
   Nazwy animacji = to, czym sterujesz w MythicMobs `state{...;s=attack}`.
8. **Export → Blockbench Model (`.bbmodel`)** — NIE "Export Java Block/Item".

> Refinement geometrii i UV robi się wygodnie w GUI. Plik wygenerowany programowo (jak
> `grove_guardian`) jest poprawny strukturalnie i otwiera się w Blockbench do dalszej obróbki.

---

## 3. Gdzie wrzucić plik

Dwie równoważne lokalizacje (ModelEngine parsuje obie):

**A) Bezpośrednio w ModelEngine (używamy tego):**
```
plugins/ModelEngine/blueprints/<model_id>/<plik>.bbmodel
```
Można też płaski plik: `plugins/ModelEngine/blueprints/<model_id>.bbmodel`.
ID blueprintu = nazwa pliku (bez `.bbmodel`) — tu: **`grove_guardian`**.

**B) Wewnątrz packa MythicMobs** (jeśli wolisz trzymać model przy mobie):
```
plugins/MythicMobs/packs/<Pack>/models/<plik>.bbmodel
```
ModelEngine traktuje `models/` w packach jak katalog blueprints.

W tym repo używamy wariantu A:
```
plugins/ModelEngine/blueprints/grove_guardian/grove_guardian.bbmodel
```

---

## 4. Reload / generacja

In-game (konsola lub OP), NIE restartuj serwera:
```
/meg reload
```
To re-parsuje blueprinty i regeneruje resource pack. Po zmianie geometrii/tekstury — `/meg reload`.

Diagnostyka:
```
/meg list models           # lista załadowanych blueprintów (oczekuj: grove_guardian)
/meg debug colors          # podgląd kości/hitboxów (jeśli dostępne w tej wersji)
```

Gdy gracze już są online, po regeneracji packa może być potrzebne ponowne pobranie
resource packa przez klienta (rejoin / re-send packa) — zależnie od konfiguracji wysyłki RP.

---

## 5. Test "gołego" modelu (bez MythicMoba)

Najszybsza weryfikacja, że blueprint się załadował i renderuje:
```
/meg spawn grove_guardian
```
(Składnia spawnu dummy może się różnić między buildami — jeśli `/meg spawn` nie istnieje,
weryfikuj przez MythicMoba w sekcji 6.) Spójrz, czy model się pojawia, ma poprawny rozmiar
(~3-4 bloki) i hitbox (uderz w niego — powinien reagować na trafienia w obszarze `hitbox`).

---

## 6. Podpięcie do MythicMoba

### WAŻNE — Crucible jest zainstalowany
Gdy obecny jest **MythicCrucible**, klasyczny mechanic `model{...}` bywa nadpisywany przez
Crucible `ModelSet`. Oficjalna rekomendacja: użyj **`meg:SetItemModel`** lub bloku `Model:`
w definicji moba. Najpewniejszy i czytelny wariant to **blok `Model:`** poniżej.

### Wariant A — blok `Model:` w definicji moba (zalecany)
`plugins/MythicMobs/Mobs/grove_guardian.yml`:
```yaml
GroveGuardian:
  Type: ZOMBIE              # bazowy entity (niewidoczny, zastąpiony modelem)
  Display: "Strażnik Gaju"
  Health: 400
  Damage: 12
  Options:
    MovementSpeed: 0.18
    PreventOtherDrops: true
  Model:
    Id: grove_guardian
    ViewRadius: 64
    Drive: false
    DamageTint: true
  Skills:
    # animacje stanów (jeśli zdefiniowane w .bbmodel):
    - state{mid=grove_guardian;s=attack} @self ~onAttack
    - state{mid=grove_guardian;s=death}  @self ~onDeath
```

### Wariant B — mechanic w Skills (gdy Crucible NIE przeszkadza)
```yaml
  Skills:
    - delay 2
    - model{mid=grove_guardian;n=false} @self ~onSpawn
    - state{mid=grove_guardian;s=attack} @self ~onAttack
```
`n=false` = ukryj nazwę bazowej encji. **Zawsze daj `delay` przed `model{}` na `~onSpawn`**,
inaczej model może nie zdążyć się przypiąć.

### Wariant C — Crucible obejście
```yaml
  Skills:
    - meg:setitemmodel{mid=grove_guardian} @self ~onSpawn
```

Spawn moba do testu:
```
/mm mobs spawn GroveGuardian
```

---

## 7. Walidacja in-game — checklista (NIE restartuj serwera)

1. `/meg reload` → w logu brak błędów parsowania; `/meg list models` pokazuje `grove_guardian`.
2. (jeśli dostępne) `/meg spawn grove_guardian` → model renderuje się, rozmiar OK, znika po despawnie.
3. `/mm mobs spawn GroveGuardian` → mob pojawia się z modelem (nie z gołym ZOMBIE).
4. Trafienia: bij moba — hitbox reaguje w obrębie kości `hitbox`; eye height wygląda sensownie.
5. Animacje (jeśli są): `~onAttack` odpala `state s=attack`; śmierć odpala `s=death`.
6. Walidacja strukturalna pliku przed reloadem (lokalnie):
   ```
   python3 -m json.tool plugins/ModelEngine/blueprints/grove_guardian/grove_guardian.bbmodel >/dev/null
   ```
   Brak błędu = poprawny JSON.

### Walidacja botem (preferencja projektu)
Zamiast "na oko" — zaloguj się botem mineflayer, wykonaj `/mm mobs spawn GroveGuardian`,
sprawdź czy encja istnieje w pobliżu i czy nie sypią się błędy w logu serwera.

---

## 8. Częste pułapki

- **`box_uv: true`** psuje import w formacie `free` — trzymaj `false`.
- **Hitbox nie-kwadratowy** (X≠Z) → ModelEngine wymusi X, hitbox będzie wyglądał źle.
- **Brak kości `hitbox`** → mob może dostać domyślny/zerowy hitbox (nie da się trafić / dusi się).
- **Crucible** nadpisuje `model{}` → użyj bloku `Model:` lub `meg:setitemmodel`.
- **`model{}` na `~onSpawn` bez delay** → model się nie przypina.
- Po `/meg reload` klient może potrzebować ponownego pobrania resource packa.

---

## Źródła
- Model Engine 4 wiki — MythicMobs: https://git.mythiccraft.io/mythiccraft/model-engine-4/-/wikis/MythicMobs
- Model Engine 4 wiki — Importing a Model: https://git.mythiccraft.io/mythiccraft/model-engine-4/-/wikis/Modeling/Importing-a-Model
- Model Engine 4 wiki — Bone Behaviors: https://git.mythiccraft.io/mythiccraft/model-engine-4/-/wikis/Modeling/Bone-Behaviors
- Lokalne przykłady: `plugins/ModelEngine/internals/blueprints/internal_fire.bbmodel`,
  `plugins/ModelEngine/internals/examples/player_model.bbmodel`
