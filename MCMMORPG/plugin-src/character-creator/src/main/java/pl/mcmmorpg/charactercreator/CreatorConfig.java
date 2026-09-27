package pl.mcmmorpg.charactercreator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import pl.mcmmorpg.charactercreator.model.ModelEngineBridge;
import pl.mcmmorpg.charactercreator.skin.SkinService;

/**
 * Typed view over config.yml. Loaded once on enable; call {@link #reload} to pick up
 * edits without a restart (patrz {@code /cc reload}).
 */
public final class CreatorConfig {

  /** Rasa z {@code model-id: ''} albo {@code 'none'} jest ŚWIADOMIE bez modelu 3D. */
  public static final String NO_MODEL = "none";

  /**
   * Ile pozycji rasy/klasy menu w ogole potrafi narysowac — patrz
   * {@code CreatorMenu.SLOTS_RACE}. Tu tylko po to, zeby walidacja configu powiedziala
   * o nadmiarze zamiast go po cichu uciac.
   */
  static final int MENU_SLOTS_PER_COLUMN = 4;

  public record RaceOption(String id, String display, String permission, String modelId,
      String icon, Color helmetColor, String description, String bonus) {

    /** Czy config w ogole DEKLARUJE model dla tej rasy (osobno od tego, czy blueprint jest). */
    public boolean declaresModel() {
      return modelId != null && !modelId.isBlank() && !NO_MODEL.equalsIgnoreCase(modelId);
    }
  }

  /**
   * gear: co zakladamy dummy'emu, zeby wybor klasy byl widoczny NA POSTACI.
   * perRace: klasa rasowa pod tym archetypem (Czlowiek + wojownik = Rycerz) — sekcja
   * {@code classes.<id>.races.<rasa>}; bez wpisu menu pokazuje sam archetyp.
   */
  public record ClassOption(String id, String display, String mmocoreId, String icon,
      Map<String, Material> gear, String description, String bonus,
      Map<String, RaceVariant> perRace) {

    public String displayFor(String raceId) {
      RaceVariant v = raceId == null ? null : perRace.get(raceId);
      return v != null && !v.display().isBlank() ? v.display() : display;
    }

    public String descriptionFor(String raceId) {
      RaceVariant v = raceId == null ? null : perRace.get(raceId);
      return v != null && !v.description().isBlank() ? v.description() : description;
    }

    /** Klasa rasowa albo null, gdy config jej nie ma (wtedy karta pokazuje sam archetyp). */
    public RaceVariant variant(String raceId) {
      return raceId == null ? null : perRace.get(raceId);
    }

    public String bonusFor(String raceId) {
      RaceVariant v = raceId == null ? null : perRace.get(raceId);
      return v != null && !v.bonus().isBlank() ? v.bonus() : bonus;
    }
  }

  /**
   * Klasa rasowa. {@code description} (kilka linii rozdzielonych '\n') i {@code bonus} czyta
   * stary kreator; {@code lore}, {@code armor}, {@code weapons} i {@code specializations} —
   * karta klasy w scenie. Wszystko pisze tools/klasy-rasowe/generuj_klasy.py (funkcja kreator).
   */
  public record RaceVariant(String display, String description, String bonus, String lore,
      String armor, String weapons, List<Specialization> specializations) {

    public RaceVariant(String display, String description, String bonus) {
      this(display, description, bonus, "", "", "", List.of());
    }
  }

  /** Specjalizacja z 10. poziomu: nazwa (z kolorem &), opis i co daje. */
  public record Specialization(String name, String description, String perks) {
  }

  /** Waga problemu z configu. FATAL wylacza plugin — patrz CharacterCreatorPlugin#onEnable. */
  public enum Severity { FATAL, WARNING, INFO }

  public record Problem(Severity severity, String message) {
  }

  private final CharacterCreatorPlugin plugin;

  private String roomWorldName;
  private Location roomSpawn;
  private Location dummyAnchor;
  private Location exitLocation;
  private int exitInvulnerabilityTicks;
  private boolean sweepRoomEntities;
  private int roomViewDistance;
  private boolean keepRoomLoaded;
  private boolean enforceProfileSelection;
  private int profileCheckDelayTicks;
  private int profileRecheckIntervalTicks;
  private int rotationIntervalTicks;
  private boolean applySkinToPlayer;
  private boolean applySkinOnJoin;
  private boolean skinProfileHeads;
  private final Map<String, RaceOption> races = new LinkedHashMap<>();
  private final Map<String, ClassOption> classes = new LinkedHashMap<>();

  public CreatorConfig(CharacterCreatorPlugin plugin) {
    this.plugin = plugin;
  }

  public void reload() {
    plugin.reloadConfig();
    FileConfiguration cfg = plugin.getConfig();

    roomWorldName = cfg.getString("creator-room.world", "creator_limbo");
    roomSpawn = readLocation(cfg, "creator-room.spawn", 100);
    dummyAnchor = readLocation(cfg, "creator-room.dummy-anchor", 100);
    exitLocation = readExitLocation(cfg);
    exitInvulnerabilityTicks = Math.max(0, cfg.getInt("creator-room.exit-invulnerability-ticks", 100));
    sweepRoomEntities = cfg.getBoolean("creator-room.sweep-stray-entities", true);
    roomViewDistance = Math.max(0, cfg.getInt("creator-room.view-distance", 4));
    keepRoomLoaded = cfg.getBoolean("creator-room.keep-loaded", true);
    enforceProfileSelection = cfg.getBoolean("profile-selection.enforce", true);
    profileCheckDelayTicks = Math.max(1, cfg.getInt("profile-selection.first-check-delay-ticks", 100));
    profileRecheckIntervalTicks = Math.max(0, cfg.getInt("profile-selection.recheck-interval-ticks", 200));
    rotationIntervalTicks = cfg.getInt("preview.rotation-interval-ticks", 2);
    applySkinToPlayer = cfg.getBoolean("skins.apply-to-player", true);
    applySkinOnJoin = cfg.getBoolean("skins.apply-on-join", true);
    skinProfileHeads = cfg.getBoolean("skins.profile-heads", true);

    races.clear();
    ConfigurationSection racesSection = cfg.getConfigurationSection("races");
    if (racesSection != null) {
      for (String id : racesSection.getKeys(false)) {
        ConfigurationSection s = racesSection.getConfigurationSection(id);
        if (s == null) continue;
        races.put(id, new RaceOption(id, s.getString("display", id), s.getString("permission", "race." + id),
            s.getString("model-id", NO_MODEL), s.getString("icon", ""),
            parseColor(s.getString("helmet-color", "")),
            s.getString("description", ""), s.getString("bonus", "")));
      }
    }

    classes.clear();
    ConfigurationSection classesSection = cfg.getConfigurationSection("classes");
    if (classesSection != null) {
      for (String id : classesSection.getKeys(false)) {
        ConfigurationSection s = classesSection.getConfigurationSection(id);
        if (s == null) continue;
        classes.put(id, new ClassOption(id, s.getString("display", id), s.getString("mmocore-id", ""),
            s.getString("icon", ""), parseGear(s.getConfigurationSection("gear")),
            s.getString("description", ""), s.getString("bonus", ""),
            parseRaceVariants(s.getConfigurationSection("races"))));
      }
    }
  }

  private static String str(Object o) {
    return o == null ? "" : o.toString();
  }

  private static Map<String, RaceVariant> parseRaceVariants(ConfigurationSection section) {
    Map<String, RaceVariant> out = new LinkedHashMap<>();
    if (section == null) {
      return out;
    }
    for (String raceId : section.getKeys(false)) {
      ConfigurationSection r = section.getConfigurationSection(raceId);
      if (r == null) continue;
      List<Specialization> specs = new java.util.ArrayList<>();
      for (Map<?, ?> m : r.getMapList("specializations")) {
        specs.add(new Specialization(str(m.get("name")), str(m.get("description")),
            str(m.get("perks"))));
      }
      out.put(raceId, new RaceVariant(r.getString("display", ""),
          r.getString("description", ""), r.getString("bonus", ""), r.getString("lore", ""),
          r.getString("armor", ""), r.getString("weapons", ""), List.copyOf(specs)));
    }
    return out;
  }

  /**
   * Sprawdza config Z GORY, zamiast odmawiac w runtime.
   *
   * <p>Stary kod dowiadywal sie o brakujacym {@code mmocore-id} dopiero w momencie, w ktorym
   * gracz klikal "STWORZ POSTAC" — po przejsciu calego kreatora, po ceremonii, bez sciezki
   * powrotu. Swiat byl rozwiazywany raz przy starcie i nigdy nie sprawdzany na {@code null},
   * wiec {@code /mv unload creator_limbo} zamienialo kazdy teleport w NPE. Tu obie te rzeczy
   * sa wykrywane, zanim ktokolwiek wejdzie do sali.
   */
  public List<Problem> validate(ModelEngineBridge modelEngineBridge, SkinService skinService) {
    List<Problem> problems = new ArrayList<>();

    World world = plugin.getServer().getWorld(roomWorldName);
    if (world == null) {
      problems.add(new Problem(Severity.FATAL, "Swiat kreatora '" + roomWorldName
          + "' nie istnieje albo nie jest zaladowany. Sprawdz Multiverse (/mv list) "
          + "i creator-room.world w config.yml."));
    }
    if (roomSpawn == null || dummyAnchor == null) {
      problems.add(new Problem(Severity.FATAL,
          "creator-room.spawn / dummy-anchor nie daja sie rozwiazac "
          + "w swiecie '" + roomWorldName + "'."));
    }

    // Wyjscie NIE jest bledem krytycznym: brak sekcji to swiadomy powrot do zachowania
    // sprzed poprawki (spawn swiata glownego). Ale nazwanie nieistniejacego swiata juz
    // jest usterka i ma byc slychac — inaczej gracz dowiaduje sie o niej w momencie,
    // w ktorym konczy tworzenie postaci.
    String exitWorldName = plugin.getConfig().getString("creator-room.exit.world");
    if (exitWorldName != null && !exitWorldName.isBlank() && exitLocation == null) {
      problems.add(new Problem(Severity.WARNING, "creator-room.exit.world wskazuje na swiat '"
          + exitWorldName + "', ktorego nie ma. Gracze koncza kreator na spawnie swiata "
          + "glownego — tam, gdzie konto testowe ginelo od mobow."));
    }

    if (races.isEmpty()) {
      problems.add(new Problem(Severity.FATAL, "Sekcja 'races' w config.yml jest pusta."));
    }
    if (classes.isEmpty()) {
      problems.add(new Problem(Severity.FATAL, "Sekcja 'classes' w config.yml jest pusta."));
    }

    // Menu ma na sztywno po CZTERY sloty na rase i na klase (CreatorMenu.SLOTS_RACE /
    // SLOTS_CLASS). Piata pozycja w configu po prostu sie nie narysuje — bez slowa,
    // bo petla renderujaca konczy sie na dlugosci tablicy. Lepiej powiedziec to przy
    // starcie niz pozwolic komus szukac, czemu jego nowa rasa "nie dziala".
    if (races.size() > MENU_SLOTS_PER_COLUMN) {
      problems.add(new Problem(Severity.WARNING, "W config.yml jest " + races.size()
          + " ras, a menu ma miejsce na " + MENU_SLOTS_PER_COLUMN
          + ". Nadmiarowe nie beda widoczne w kreatorze."));
    }
    if (classes.size() > MENU_SLOTS_PER_COLUMN) {
      problems.add(new Problem(Severity.WARNING, "W config.yml jest " + classes.size()
          + " klas, a menu ma miejsce na " + MENU_SLOTS_PER_COLUMN
          + ". Nadmiarowe nie beda widoczne w kreatorze."));
    }

    for (ClassOption option : classes.values()) {
      if (option.mmocoreId() == null || option.mmocoreId().isBlank()) {
        problems.add(new Problem(Severity.FATAL, "Klasa '" + option.id()
            + "' nie ma 'mmocore-id' — gracz, ktory ja wybierze, zostalby bez klasy."));
      }
    }

    for (RaceOption race : races.values()) {
      if (!race.declaresModel()) {
        // To jest DEKLARACJA, a nie usterka: rasa jawnie oznaczona jako jeszcze bez modelu.
        problems.add(new Problem(Severity.INFO, "Rasa '" + race.id()
            + "' jest oznaczona jako bez modelu 3D (model-id: " + NO_MODEL
            + ") — pokazuje ja skorka z skins.yml."));
      } else if (!modelEngineBridge.hasBlueprint(race.modelId())) {
        problems.add(new Problem(Severity.WARNING, "Rasa '" + race.id() + "' deklaruje model '"
            + race.modelId() + "', ale nie ma takiego blueprintu w "
            + "plugins/ModelEngine/blueprints/. Albo dodaj blueprint, albo ustaw "
            + "model-id: " + NO_MODEL + ", zeby config nie obiecywal czegos, czego nie ma."));
      }
    }

    problems.addAll(validateSkins(skinService));
    return problems;
  }

  /**
   * Skorki sa CONTENTEM, nie kodem: brak jednej pary rasa x klasa nie moze wylaczac pluginu,
   * ale ma byc slychac. Bez tego postac po prostu zostawalaby w skorce konta gracza i nikt
   * by nie wiedzial, dlaczego akurat ta jedna.
   */
  private List<Problem> validateSkins(SkinService skinService) {
    List<Problem> problems = new ArrayList<>();
    if (skinService == null) {
      return problems;
    }
    if (!applySkinToPlayer) {
      problems.add(new Problem(Severity.INFO, "skins.apply-to-player: false — kreator pokazuje "
          + "skorki rasy i klasy w podgladzie, ale gracz zostaje w skorce swojego konta."));
    }
    if (applySkinOnJoin && !applySkinToPlayer) {
      problems.add(new Problem(Severity.INFO, "skins.apply-on-join: true nic nie robi przy "
          + "skins.apply-to-player: false — nakladanie skorki przy wejsciu jest wylaczone "
          + "razem z nakladaniem jej w ogole."));
    }
    if (skinService.isEmpty()) {
      problems.add(new Problem(Severity.WARNING, "skins.yml nie ma ani jednej skorki — "
          + "postacie beda wygladac tak, jak konta graczy. Wygeneruj je: "
          + "tools/gen_race_skins.py + tools/upload_skins.sh."));
      return problems;
    }
    List<String> missing = new ArrayList<>();
    for (String raceId : races.keySet()) {
      for (String classId : classes.keySet()) {
        String id = SkinService.idFor(raceId, classId);
        if (!skinService.ids().contains(id) && !skinService.ids().contains(raceId)) {
          missing.add(id);
        }
      }
    }
    if (!missing.isEmpty()) {
      problems.add(new Problem(Severity.WARNING, "Brak skorek dla par: "
          + String.join(", ", missing) + ". Te postacie zostana w skorkach kont graczy."));
    }
    return problems;
  }

  /** "#RRGGBB" -> Color; pusty/bledny wpis daje null (brak helmu rasy). */
  static Color parseColor(String hex) {
    if (hex == null || !hex.matches("#[0-9a-fA-F]{6}")) {
      return null;
    }
    return Color.fromRGB(Integer.parseInt(hex.substring(1), 16));
  }

  /** Mapa slot->material; nieznane nazwy materialow sa pomijane, a nie wysadzaja configu. */
  private static Map<String, Material> parseGear(ConfigurationSection section) {
    if (section == null) {
      return Map.of();
    }
    Map<String, Material> out = new LinkedHashMap<>();
    for (String slot : section.getKeys(false)) {
      Material material = Material.matchMaterial(section.getString(slot, ""));
      if (material != null) {
        out.put(slot, material);
      }
    }
    return out;
  }

  /**
   * Miejsce wyjscia z kreatora. W przeciwienstwie do lokalizacji w sali, ta ma WLASNY
   * swiat — sala jest w limbo, a gracz wychodzi do swiata gry.
   *
   * @return {@code null}, gdy sekcji nie ma albo swiat nie jest zaladowany; wolajacy ma
   *         wtedy uzyc spawnu swiata glownego.
   */
  private Location readExitLocation(FileConfiguration cfg) {
    ConfigurationSection s = cfg.getConfigurationSection("creator-room.exit");
    if (s == null) {
      return null;
    }
    World world = plugin.getServer().getWorld(s.getString("world", ""));
    if (world == null) {
      return null;
    }
    return new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
        (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
  }

  /** Zwraca {@code null}, gdy swiata nie ma — {@link #validate} zglosi to jako FATAL. */
  private Location readLocation(FileConfiguration cfg, String path, double defaultY) {
    World world = plugin.getServer().getWorld(cfg.getString("creator-room.world", "creator_limbo"));
    if (world == null) {
      return null;
    }
    ConfigurationSection s = cfg.getConfigurationSection(path);
    double x = s != null ? s.getDouble("x", 0) : 0;
    double y = s != null ? s.getDouble("y", defaultY) : defaultY;
    double z = s != null ? s.getDouble("z", 0) : 0;
    float yaw = s != null ? (float) s.getDouble("yaw", 0) : 0f;
    float pitch = s != null ? (float) s.getDouble("pitch", 0) : 0f;
    return new Location(world, x, y, z, yaw, pitch);
  }

  public String roomWorldName() {
    return roomWorldName;
  }

  public Location roomSpawn() {
    return roomSpawn;
  }

  public Location dummyAnchor() {
    return dummyAnchor;
  }

  /**
   * Dokad odeslac gracza po kreatorze. {@code null} = spawn swiata glownego, czyli
   * zachowanie sprzed poprawki.
   */
  public Location exitLocation() {
    return exitLocation;
  }

  public int exitInvulnerabilityTicks() {
    return exitInvulnerabilityTicks;
  }

  /** false = sala kreatora nie jest czyszczona przy starcie serwera. */
  public boolean sweepRoomEntities() {
    return sweepRoomEntities;
  }

  /** 0 = nie ruszaj zasiegu widzenia sali (zostaje globalny z server.properties). */
  public int roomViewDistance() {
    return roomViewDistance;
  }

  /** false = chunki sali nie sa trzymane w pamieci miedzy wejsciami graczy. */
  public boolean keepRoomLoaded() {
    return keepRoomLoaded;
  }

  public boolean enforceProfileSelection() {
    return enforceProfileSelection;
  }

  public int profileCheckDelayTicks() {
    return profileCheckDelayTicks;
  }

  public int profileRecheckIntervalTicks() {
    return profileRecheckIntervalTicks;
  }

  public int rotationIntervalTicks() {
    return rotationIntervalTicks;
  }

  /**
   * Czy skorka postaci ma trafiac na PRAWDZIWEGO gracza (a nie tylko na podglad w kreatorze).
   * Wylacznik istnieje, bo to jedyna rzecz w tym pluginie, ktora zmienia profil gracza —
   * przy sporze z innym pluginem od skorek ma byc czym to rozdzielic bez przebudowy jara.
   */
  public boolean applySkinToPlayer() {
    return applySkinToPlayer;
  }

  /**
   * Czy nakladac skorke ostatnio granej postaci juz przy WEJSCIU na serwer.
   *
   * <p>Bez tego gracz od wejscia do wyboru postaci chodzi w skorce swojego konta —
   * kilkanascie sekund, w ktorych wyglada jak nie swoja postac. Po wyborze skorka i tak
   * jest poprawiana na te, ktora nalezy do wybranej postaci, wiec najgorsze, co moze tu
   * wyjsc, to skorka POPRZEDNIEJ postaci przez chwile zamiast skorki konta.
   *
   * <p>Osobny wylacznik, bo to jedyne miejsce, gdzie ruszamy profil gracza BEZ zadnego
   * zdarzenia MMOProfiles — gdyby weszlo w droge innemu pluginowi, ma byc czym to odciac.
   */
  public boolean applySkinOnJoin() {
    return applySkinOnJoin;
  }

  /**
   * Czy dopisywac skorki postaci do glow na ekranie wyboru postaci MMOProfiles.
   *
   * <p>Osobny wylacznik od {@link #applySkinToPlayer()}, bo to jedyne miejsce, w ktorym
   * wchodzimy w CUDZE okno — przy zmianie ukladu po stronie MMOProfiles ma byc czym to
   * odciac bez przebudowy jara.
   */
  public boolean skinProfileHeads() {
    return skinProfileHeads;
  }


  public Map<String, RaceOption> races() {
    return races;
  }

  public Map<String, ClassOption> classes() {
    return classes;
  }
}
