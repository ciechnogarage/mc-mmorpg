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
    List<String> hudOnClose, boolean replaceCreator, boolean replaceSelector,
    Map<String, Sound> uiSounds, Flyover flyover, Lineup lineup) {

  public static final String DEFAULT_POINT = "default";

  /**
   * Jeden kadr: kamera, postac na scenie, karta obok niej (prawa strona) i panel listy
   * (lewa strona). {@code list} null = lustro karty wzgledem postaci.
   */
  public record Point(Location camera, Location subject, Location card, Location list,
      Ambience ambience) {

    public Point(Location camera, Location subject, Location card) {
      this(camera, subject, card, null, Ambience.NONE);
    }

    public Point(Location camera, Location subject, Location card, Location list) {
      this(camera, subject, card, list, Ambience.NONE);
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
          points.put(id, new Point(aimed(camera, target), subject, card, list,
              Ambience.read(world, p)));
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
        s != null && s.getBoolean("replace-selector", false),
        uiSounds(s == null ? null : s.getConfigurationSection("ui-sounds")),
        Flyover.read(s == null ? null : s.getConfigurationSection("flyover")),
        Lineup.read(world, s == null ? null : s.getConfigurationSection("select")));
  }

  private static Map<String, Sound> uiSounds(ConfigurationSection c) {
    Map<String, Sound> out = new LinkedHashMap<>();
    if (c != null) {
      for (String key : c.getKeys(false)) {
        ConfigurationSection one = c.getConfigurationSection(key);
        Sound snd = one == null ? null : Sound.read(one.getValues(false));
        if (snd != null) {
          out.put(key, snd);
        }
      }
    }
    return Map.copyOf(out);
  }

  /**
   * Przelot kamery po stworzeniu postaci (decyzja D6): kamera leci po punktach {@code path}
   * w swiecie gry i konczy nad miejscem startu, potem sciemnienie i gracz stoi na starcie.
   * Kazdy odcinek to jeden teleport encji-kamery z interpolacja klienta ({@code ticks} 1-59).
   */
  public record Flyover(String worldName, List<FlyPoint> path, Location look, String title,
      String subtitle, long time, int holdTicks) {

    public boolean usable() {
      return path.size() >= 2 && Bukkit.getWorld(worldName) != null;
    }

    public World world() {
      return Bukkit.getWorld(worldName);
    }

    static Flyover read(ConfigurationSection c) {
      if (c == null || !c.getBoolean("enabled", true)) {
        return null;
      }
      String worldName = c.getString("world", "world");
      World world = Bukkit.getWorld(worldName);
      List<FlyPoint> path = new java.util.ArrayList<>();
      for (Map<?, ?> m : c.getMapList("path")) {
        path.add(new FlyPoint(Emitter.num(m.get("x"), 0), Emitter.num(m.get("y"), 0),
            Emitter.num(m.get("z"), 0),
            Math.max(1, Math.min(59, (int) Emitter.num(m.get("ticks"), 40)))));
      }
      Location look = world == null ? null : location(world, c.getConfigurationSection("look"));
      return new Flyover(worldName, List.copyOf(path), look, c.getString("title", ""),
          c.getString("subtitle", ""), c.getLong("time", -1), Math.max(0, c.getInt("hold-ticks", 40)));
    }
  }

  /** Punkt przelotu i czas dolotu do niego (ticki). */
  public record FlyPoint(double x, double y, double z, int ticks) {}

  /**
   * Scena wyboru postaci: wszystkie postacie gracza stoja w luku wokol {@code center}
   * (ognisko), przodem na zewnatrz; kamera stoi przed wybrana i obiega krag przy A/D.
   * Klimat (pora, czastki, dzwiek) bierze z kadru {@code point}.
   */
  public record Lineup(String point, Location center, double radius, double cameraDistance,
      double cameraHeight, double lookHeight, double spacingDegrees, double startDegrees,
      int stepTicks) {

    static Lineup read(World world, ConfigurationSection c) {
      if (c == null || world == null) {
        return null;
      }
      Location center = location(world, c.getConfigurationSection("center"));
      if (center == null) {
        return null;
      }
      return new Lineup(c.getString("point", "wybor"), center, c.getDouble("radius", 3.2),
          c.getDouble("camera-distance", 3.6), c.getDouble("camera-height", 1.55),
          c.getDouble("look-height", 1.05), c.getDouble("spacing-degrees", 36),
          c.getDouble("start-degrees", 0), Math.max(1, Math.min(59, c.getInt("step-ticks", 4))));
    }

    /** Miejsce postaci nr {@code i} z {@code n}: luk srodkiem na {@code startDegrees}. */
    public Location spot(int i, int n) {
      double spacing = Math.min(spacingDegrees, 360.0 / Math.max(1, n));
      double deg = startDegrees + (i - (n - 1) / 2.0) * spacing;
      double rad = Math.toRadians(deg);
      Location at = center.clone().add(-Math.sin(rad) * radius, 0, Math.cos(rad) * radius);
      at.setYaw((float) deg);
      at.setPitch(0);
      return at;
    }

    /** Kat (stopnie) miejsca nr {@code i} — kamera obiega krag po tych katach. */
    public double angle(int i, int n) {
      double spacing = Math.min(spacingDegrees, 360.0 / Math.max(1, n));
      return startDegrees + (i - (n - 1) / 2.0) * spacing;
    }

    /** Kamera przed postacia stojaca pod katem {@code deg}, patrzy na nia. */
    public Location camera(double deg) {
      double rad = Math.toRadians(deg);
      double r = radius + cameraDistance;
      Location cam = center.clone().add(-Math.sin(rad) * r, cameraHeight, Math.cos(rad) * r);
      Location target = center.clone().add(-Math.sin(rad) * radius, lookHeight, Math.cos(rad) * radius);
      return aimed(cam, target);
    }
  }

  /**
   * Klimat kadru — widoczny tylko dla patrzacego gracza: pora dnia, pogoda, czastki, dzwiek
   * i spokojny ruch postaci. Celowo proste (wlasciciel 28.09: "nie wymyslne animacje, tlo pod
   * klimat i lekkie animacje pasujace do cech"; 29.09: "niech nie pajacuja, niech stoja i sie
   * rozgladaja w charakterystyczny dla rasy sposob" — zamach bronia wylecial).
   *
   * @param time pora dnia gracza (0-24000), -1 = czas swiata
   * @param rain deszcz tylko dla gracza
   * @param idle sposob rozgladania sie postaci
   * @param music podklad kadru (moze byc null)
   * @param sounds pojedyncze odglosy tla w swoim rytmie
   */
  public record Ambience(long time, boolean rain, List<Emitter> emitters, Idle idle,
      Sound music, List<Sound> sounds) {

    public static final Ambience NONE = new Ambience(-1, false, List.of(), Idle.NONE, null, List.of());

    static Ambience read(World world, ConfigurationSection p) {
      List<Emitter> emitters = new java.util.ArrayList<>();
      for (Map<?, ?> m : p.getMapList("particles")) {
        Emitter e = Emitter.read(world, m);
        if (e != null) {
          emitters.add(e);
        }
      }
      List<Sound> sounds = new java.util.ArrayList<>();
      for (Map<?, ?> m : p.getMapList("sounds")) {
        Sound snd = Sound.read(m);
        if (snd != null) {
          sounds.add(snd);
        }
      }
      ConfigurationSection music = p.getConfigurationSection("music");
      return new Ambience(p.getLong("time", -1), p.getBoolean("rain", false), List.copyOf(emitters),
          Idle.read(p.getConfigurationSection("idle")),
          music == null ? null : Sound.read(music.getValues(false)), List.copyOf(sounds));
    }
  }

  /**
   * Rozgladanie sie postaci: co jakis czas ({@code pause} min-max tickow) glowa plynnie
   * odchodzi do losowego gestu, trzyma go i wraca do spoczynku. Rusza sie TYLKO glowa,
   * tulow stoi przodem do kamery. Charakter rasy robia katy, predkosc i spoczynek:
   * spokojny czlowiek, czujny elf, nerwowy ork, powolny nieumarly z opuszczona glowa.
   *
   * @param restPitch pochylenie glowy w spoczynku (+ w dol)
   * @param speed domyslna predkosc obrotu glowy w stopniach na tick
   */
  public record Idle(float restPitch, float speed, int pauseMin, int pauseMax,
      List<Gesture> gestures) {

    public static final Idle NONE = new Idle(0, 3, 0, 0, List.of());

    public boolean active() {
      return !gestures.isEmpty() && pauseMax > 0;
    }

    static Idle read(ConfigurationSection c) {
      if (c == null) {
        return NONE;
      }
      float speed = (float) c.getDouble("speed", 3);
      List<Gesture> gestures = new java.util.ArrayList<>();
      for (Map<?, ?> m : c.getMapList("gestures")) {
        gestures.add(new Gesture(
            (float) Emitter.num(m.get("yaw"), 0), (float) Emitter.num(m.get("pitch"), 0),
            Math.max(1, (int) Emitter.num(m.get("hold"), 30)),
            (float) Emitter.num(m.get("speed"), speed)));
      }
      // Stary zapis (look-every / look-yaw) — dwa gesty na boki.
      if (gestures.isEmpty() && c.getInt("look-every", 0) > 0) {
        float yaw = (float) c.getDouble("look-yaw", 25);
        gestures.add(new Gesture(yaw, 0, 30, speed));
        gestures.add(new Gesture(-yaw, 0, 30, speed));
        int every = c.getInt("look-every");
        return new Idle(0, speed, every, every, List.copyOf(gestures));
      }
      List<Integer> pause = c.getIntegerList("pause");
      int min = pause.isEmpty() ? 80 : pause.get(0);
      int max = pause.size() < 2 ? min : Math.max(min, pause.get(1));
      return new Idle((float) c.getDouble("rest-pitch", 0), speed, min, max, List.copyOf(gestures));
    }
  }

  /** Jeden gest glowy: odchylenie od patrzenia w kamere, czas trzymania, predkosc. */
  public record Gesture(float yaw, float pitch, int hold, float speed) {}

  /**
   * Dzwiek kadru. {@code every} 0 = podklad (gra raz przy wejsciu w kadr, gasnie przy wyjsciu),
   * inaczej odglos co {@code every} tickow z rozrzutem {@code jitter}.
   */
  public record Sound(String key, float volume, float pitch, int every, int jitter,
      org.bukkit.SoundCategory category) {

    static Sound read(Map<?, ?> m) {
      Object key = m.get("sound");
      if (key == null || String.valueOf(key).isBlank()) {
        return null;
      }
      org.bukkit.SoundCategory category = org.bukkit.SoundCategory.AMBIENT;
      if (m.get("category") != null) {
        try {
          category = org.bukkit.SoundCategory.valueOf(String.valueOf(m.get("category")).toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
          Bukkit.getLogger().warning("scene: nieznana kategoria dzwieku " + m.get("category"));
        }
      }
      return new Sound(String.valueOf(key), (float) Emitter.num(m.get("volume"), 1.0),
          (float) Emitter.num(m.get("pitch"), 1.0), (int) Emitter.num(m.get("every"), 0),
          (int) Emitter.num(m.get("jitter"), 0), category);
    }
  }

  /**
   * Zrodlo czastek: w punkcie {@code at} albo, bez niego, wokol postaci ({@code around}).
   * Tylko czastki bez dodatkowych danych (bez koloru/bloku) — reszta jest pomijana z ostrzezeniem.
   */
  public record Emitter(org.bukkit.Particle type, Location at, double dx, double dy, double dz,
      double yOffset, int count, double speed, int every) {

    static Emitter read(World world, Map<?, ?> m) {
      try {
        org.bukkit.Particle type = org.bukkit.Particle.valueOf(String.valueOf(m.get("type")).toUpperCase(java.util.Locale.ROOT));
        if (type.getDataType() != Void.class) {
          Bukkit.getLogger().warning("scene: czastka " + type + " wymaga danych — pomijam.");
          return null;
        }
        Location at = null;
        if (m.get("at") instanceof Map<?, ?> a) {
          at = new Location(world, num(a.get("x")), num(a.get("y")), num(a.get("z")));
        }
        return new Emitter(type, at, num(m.get("dx"), 0.6), num(m.get("dy"), 0.4), num(m.get("dz"), 0.6),
            num(m.get("y"), 1.0), (int) num(m.get("count"), 2), num(m.get("speed"), 0.0),
            Math.max(1, (int) num(m.get("every"), 10)));
      } catch (IllegalArgumentException ex) {
        Bukkit.getLogger().warning("scene: nieznana czastka " + m.get("type") + " — pomijam.");
        return null;
      }
    }

    private static double num(Object o) {
      return num(o, 0);
    }

    static double num(Object o, double def) {
      return o instanceof Number n ? n.doubleValue() : def;
    }
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
