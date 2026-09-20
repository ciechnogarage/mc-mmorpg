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

  /** gear: co zakladamy dummy'emu, zeby wybor klasy byl widoczny NA POSTACI. */
  public record ClassOption(String id, String display, String mmocoreId, String icon,
      Map<String, Material> gear, String description, String bonus) {
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
  private boolean enforceProfileSelection;
  private int profileCheckDelayTicks;
  private int profileRecheckIntervalTicks;
  private int rotationIntervalTicks;
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
    enforceProfileSelection = cfg.getBoolean("profile-selection.enforce", true);
    profileCheckDelayTicks = Math.max(1, cfg.getInt("profile-selection.first-check-delay-ticks", 100));
    profileRecheckIntervalTicks = Math.max(0, cfg.getInt("profile-selection.recheck-interval-ticks", 200));
    rotationIntervalTicks = cfg.getInt("preview.rotation-interval-ticks", 2);

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
            s.getString("description", ""), s.getString("bonus", "")));
      }
    }
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
  public List<Problem> validate(ModelEngineBridge modelEngineBridge) {
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
            + ") — podglad uzyje przebrania zastepczego."));
      } else if (!modelEngineBridge.hasBlueprint(race.modelId())) {
        problems.add(new Problem(Severity.WARNING, "Rasa '" + race.id() + "' deklaruje model '"
            + race.modelId() + "', ale nie ma takiego blueprintu w "
            + "plugins/ModelEngine/blueprints/. Albo dodaj blueprint, albo ustaw "
            + "model-id: " + NO_MODEL + ", zeby config nie obiecywal czegos, czego nie ma."));
      }
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


  public Map<String, RaceOption> races() {
    return races;
  }

  public Map<String, ClassOption> classes() {
    return classes;
  }
}
