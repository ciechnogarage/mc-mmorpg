package pl.mcmmorpg.charactercreator.scene;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

/**
 * Sekcja {@code scene:} z config.yml — gdzie stoi kamera, postac i karta.
 *
 * <p><b>Punkty sceny</b> ({@code scene.points.<id>}) to kadry: kamera + miejsce postaci + miejsce
 * karty. Kontroler prosi o kadr po nazwie (np. id rasy), a gdy takiego nie ma, dostaje
 * {@value #DEFAULT_POINT}. Dzieki temu cztery scenerie ras (etap 4) to sam config: dopisanie
 * {@code points.ork} przenosi kamere w obozowisko bez zmiany kodu.
 */
public record SceneSettings(String worldName, Map<String, Point> points, String fadeGlyph,
    int fadeInTicks, int fadeStayTicks, int fadeOutTicks, double shieldDistance,
    int targetDelayTicks, List<String> hudOnOpen, List<String> hudRepeat, int hudRepeatTicks,
    List<String> hudOnClose, boolean replaceCreator, boolean replaceSelector) {

  public static final String DEFAULT_POINT = "default";

  /**
   * Jeden kadr: kamera, postac na scenie, karta obok niej (prawa strona) i panel listy
   * (lewa strona). {@code list} null = lustro karty wzgledem postaci.
   */
  public record Point(Location camera, Location subject, Location card, Location list) {

    public Point(Location camera, Location subject, Location card) {
      this(camera, subject, card, null);
    }

    /** Panel listy: z configu albo odbicie karty po drugiej stronie postaci. */
    public Location listOrMirror() {
      if (list != null || card == null) {
        return list;
      }
      Vector dir = subject.toVector().subtract(camera.toVector()).setY(0);
      if (dir.lengthSquared() < 1.0E-6) {
        return null;
      }
      dir.normalize();
      Vector v = card.toVector().subtract(subject.toVector());
      Vector along = dir.clone().multiply(v.getX() * dir.getX() + v.getZ() * dir.getZ());
      Vector across = new Vector(v.getX(), 0, v.getZ()).subtract(along);
      Vector mirrored = along.subtract(across).setY(v.getY());
      return subject.clone().add(mirrored);
    }
  }

  public static SceneSettings read(ConfigurationSection root, String fallbackWorld) {
    ConfigurationSection s = root == null ? null : root.getConfigurationSection("scene");
    String worldName = s == null ? fallbackWorld : s.getString("world", fallbackWorld);
    World world = Bukkit.getWorld(worldName);
    Map<String, Point> points = new LinkedHashMap<>();
    ConfigurationSection pts = s == null ? null : s.getConfigurationSection("points");
    if (pts != null && world != null) {
      for (String id : pts.getKeys(false)) {
        ConfigurationSection p = pts.getConfigurationSection(id);
        if (p == null) {
          continue;
        }
        Location camera = location(world, p.getConfigurationSection("camera"));
        Location subject = location(world, p.getConfigurationSection("subject"));
        Location card = location(world, p.getConfigurationSection("card"));
        Location look = location(world, p.getConfigurationSection("look"));
        Location list = location(world, p.getConfigurationSection("list"));
        if (camera != null && subject != null) {
          Location target = look != null ? look : subject.clone().add(0, 1.0, 0);
          points.put(id, new Point(aimed(camera, target), subject, card, list));
        }
      }
    }
    ConfigurationSection fade = s == null ? null : s.getConfigurationSection("fade");
    ConfigurationSection hud = s == null ? null : s.getConfigurationSection("hud");
    return new SceneSettings(worldName, Map.copyOf(points),
        fade == null ? "" : fade.getString("glyph", ""),
        fade == null ? 6 : fade.getInt("in-ticks", 6),
        fade == null ? 4 : fade.getInt("stay-ticks", 4),
        fade == null ? 8 : fade.getInt("out-ticks", 8),
        s == null ? 1.4 : s.getDouble("shield-distance", 1.4),
        s == null ? 5 : s.getInt("target-delay-ticks", 5),
        hud == null ? List.of() : List.copyOf(hud.getStringList("on-open")),
        hud == null ? List.of() : List.copyOf(hud.getStringList("repeat")),
        hud == null ? 40 : Math.max(1, hud.getInt("repeat-ticks", 40)),
        hud == null ? List.of() : List.copyOf(hud.getStringList("on-close")),
        s != null && s.getBoolean("replace-creator", false),
        s != null && s.getBoolean("replace-selector", false));
  }

  /** Kadr o tej nazwie, a gdy go nie ma — domyslny. {@code null} tylko przy braku obu. */
  public Point point(String id) {
    Point p = id == null ? null : points.get(id);
    return p != null ? p : points.get(DEFAULT_POINT);
  }

  public boolean usable() {
    return points.containsKey(DEFAULT_POINT);
  }

  public List<String> problems() {
    if (Bukkit.getWorld(worldName) == null) {
      return List.of("scene.world '" + worldName + "' nie istnieje — scena wylaczona.");
    }
    if (!usable()) {
      return List.of("scene.points.default brak albo niepelny (camera + subject) — scena wylaczona.");
    }
    if (fadeGlyph.isBlank()) {
      return List.of("scene.fade.glyph pusty — przejscia beda bez sciemnienia.");
    }
    return List.of();
  }

  /**
   * Kamera patrzy na punkt {@code look} (domyslnie piers postaci): kierunek liczymy
   * z pozycji, a nie z yaw/pitch w configu, zeby przestawienie postaci albo kamery nie
   * wymagalo przeliczania katow recznie. {@code look} obok postaci przesuwa ja w kadrze
   * (u Wynna postac stoi troche na prawo od srodka, bo z prawej jest karta).
   */
  static Location aimed(Location camera, Location target) {
    Location out = camera.clone();
    Vector dir = target.toVector().subtract(camera.toVector());
    out.setDirection(dir);
    return out;
  }

  private static Location location(World world, ConfigurationSection c) {
    if (c == null) {
      return null;
    }
    return new Location(world, c.getDouble("x"), c.getDouble("y"), c.getDouble("z"),
        (float) c.getDouble("yaw", 0), (float) c.getDouble("pitch", 0));
  }
}
