package pl.mcmmorpg.charactercreator.scene;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.mcmmorpg.charactercreator.util.ConsoleCommandRunner;

/**
 * Scena w swiecie w stylu Wynncrafta: gracz patrzy przez nieruchoma kamere na postac,
 * a steruje klawiszami. Technika zmierzona w PoC na prawdziwym kliencie
 * ({@code docs/research/wynncraft/4-jak-zbudowac-u-nas.md}, sekcja 1):
 *
 * <ul>
 *   <li>SPECTATOR + {@code setSpectatorTarget(ItemDisplay)}: kamera zablokowana w 100%,
 *       zero HUD-u (bez paska przedmiotow, serc, reki i celownika), action bar zostaje;</li>
 *   <li>LPM = {@code PlayerStartSpectatingEntityEvent} na encji {@link Interaction} przed
 *       kamera (anulowany), PPM = {@code PlayerInteractEntityEvent} na tej samej encji;</li>
 *   <li>A/D = {@code PlayerInputEvent}. Kolko myszy w spectatorze do serwera NIE dochodzi
 *       (klient zuzywa je na predkosc lotu) — dlatego A/D, decyzja D1.</li>
 * </ul>
 *
 * <p>Scena NIE rusza plecaka gracza. W spectatorze pasek przedmiotow nie jest rysowany,
 * wiec nie ma czego chowac — w przeciwienstwie do starego okna kreatora, ktore czyscilo
 * plecak i ratowalo go przez sessions.yml.
 */
public final class SceneManager {

  /** Co ile tickow ponawiac podpowiedz: action bar gasnie sam po ok. 3 s. */
  private static final int HINT_PERIOD_TICKS = 30;
  private static final double SHIELD_SIZE = 2.6;

  private final Plugin plugin;
  private final Logger logger;
  private final Map<UUID, SceneView> views = new ConcurrentHashMap<>();
  private SceneSettings settings;
  private BukkitTask task;

  public SceneManager(Plugin plugin, Logger logger, SceneSettings settings) {
    this.plugin = plugin;
    this.logger = logger;
    this.settings = settings;
  }

  public void start() {
    keepPointsLoaded();
    task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
  }

  public void reload(SceneSettings next) {
    this.settings = next;
    keepPointsLoaded();
  }

  /**
   * Chunki kadrow w pamieci na stale (3x3 wokol kamery i postaci) — jak sala starego
   * kreatora. Bez tego pierwsze wejscie w kadr czeka na wczytanie z dysku, a kamera
   * (encja) nie ma gdzie powstac.
   */
  private void keepPointsLoaded() {
    for (SceneSettings.Point point : settings.points().values()) {
      for (Location at : new Location[] {point.camera(), point.subject()}) {
        int cx = at.getBlockX() >> 4;
        int cz = at.getBlockZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
          for (int dz = -1; dz <= 1; dz++) {
            at.getWorld().setChunkForceLoaded(cx + dx, cz + dz, true);
          }
        }
      }
    }
  }

  public SceneSettings settings() {
    return settings;
  }

  public SceneView view(Player player) {
    return player == null ? null : views.get(player.getUniqueId());
  }

  public boolean inScene(UUID playerId) {
    return views.containsKey(playerId);
  }

  public Iterable<SceneView> views() {
    return new ArrayList<>(views.values());
  }

  /**
   * Otwiera scene z podanym ekranem. Gdy gracz juz w niej jest — tylko podmienia ekran.
   *
   * @return {@code false}, gdy scena nie jest skonfigurowana (powod leci do logu)
   */
  public boolean open(Player player, SceneController controller) {
    SceneView existing = views.get(player.getUniqueId());
    if (existing != null) {
      switchController(existing, controller);
      return true;
    }
    if (!settings.usable()) {
      settings.problems().forEach(logger::warning);
      return false;
    }
    // Martwy gracz ma na ekranie "You Died" — scena pod nim jest niewidoczna, a respawn
    // wyrzucilby go z kamery. Zlapane na kliencie QA: scena otwarta na martwym graczu.
    if (player.isDead()) {
      logger.warning("Scena: odmowa dla " + player.getName() + " — gracz nie zyje.");
      return false;
    }
    SceneView view = new SceneView(this, player);
    view.controller = controller;
    views.put(player.getUniqueId(), view);

    SceneSettings.Point point = settings.point(SceneSettings.DEFAULT_POINT);
    view.currentPoint = point;
    view.currentPointId = SceneSettings.DEFAULT_POINT;
    Location cam = point.camera();

    player.teleport(cam);
    player.setGameMode(GameMode.SPECTATOR);

    view.camera = cam.getWorld().spawn(cam, ItemDisplay.class, e -> {
      e.setPersistent(false);
      e.setVisibleByDefault(false);
      // Interpolacja teleportu po stronie klienta — dla najazdow kamery w kolejnych etapach.
      e.setTeleportDuration(0);
    });
    view.shield = cam.getWorld().spawn(shieldLocation(cam), Interaction.class, e -> {
      e.setPersistent(false);
      e.setVisibleByDefault(false);
      e.setInteractionWidth((float) SHIELD_SIZE);
      e.setInteractionHeight((float) SHIELD_SIZE);
      e.setResponsive(false);
    });
    Location cardAt = point.card() != null ? point.card() : cam;
    view.card = cam.getWorld().spawn(cardAt, TextDisplay.class, e -> {
      e.setPersistent(false);
      e.setVisibleByDefault(false);
      e.setBillboard(Display.Billboard.CENTER);
      e.setAlignment(TextDisplay.TextAlignment.LEFT);
      e.setLineWidth(210);
      e.setShadowed(true);
      e.text(Component.empty());
      e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
      e.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
          new Vector3f(0.55f, 0.55f, 0.55f), new AxisAngle4f()));
    });
    for (Entity e : new Entity[] {view.camera, view.shield, view.card}) {
      player.showEntity(plugin, e);
    }
    isolate(player);
    runAll(settings.hudOnOpen(), player);
    runAll(settings.hudRepeat(), player);

    // setSpectatorTarget dziala dopiero, gdy klient sledzi encje — w PoC po 5 tickach od
    // zmiany trybu i teleportu. Robi to tick(); onOpen idzie zaraz za nim.
    view.targetReadyTick = Bukkit.getCurrentTick() + settings.targetDelayTicks();
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!view.closed) {
        aimCamera(view);
        view.controller.onOpen(view);
      }
    }, settings.targetDelayTicks() + 1L);
    logger.info("Scena otwarta: " + player.getName() + " (" + controller.getClass().getSimpleName() + ")");
    return true;
  }

  void switchController(SceneView view, SceneController next) {
    SceneController prev = view.controller;
    if (prev != null && prev != next) {
      prev.onClose(view);
    }
    view.controller = next;
    next.onOpen(view);
  }

  /** Zamyka scene gracza, oddaje tryb gry i miejsce sprzed wejscia. */
  public void close(UUID playerId) {
    SceneView view = views.remove(playerId);
    if (view == null) {
      return;
    }
    view.closed = true;
    try {
      view.controller.onClose(view);
    } catch (RuntimeException ex) {
      logger.warning("onClose sceny rzucil wyjatek: " + ex);
    }
    for (Entity e : new Entity[] {view.camera, view.shield, view.card}) {
      if (e != null && e.isValid()) {
        e.remove();
      }
    }
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) {
      return;
    }
    if (player.getGameMode() == GameMode.SPECTATOR) {
      view.retargeting = true;
      player.setSpectatorTarget(null);
      view.retargeting = false;
    }
    player.clearTitle();
    player.sendActionBar(Component.empty());
    runAll(settings.hudOnClose(), player);
    player.setGameMode(view.originMode() == null ? GameMode.SURVIVAL : view.originMode());
    player.teleport(view.origin());
    reveal(player);
  }

  public void closeAll() {
    for (UUID id : new ArrayList<>(views.keySet())) {
      close(id);
    }
    if (task != null) {
      task.cancel();
    }
  }

  /** Kadr: kamera, tarcza klikow i karta na punkt. Gracz leci za kamera. */
  void moveToPoint(SceneView view, String pointId) {
    SceneSettings.Point point = settings.point(pointId);
    Player player = view.player();
    if (point == null || player == null) {
      return;
    }
    view.currentPoint = point;
    view.currentPointId = settings.points().containsKey(pointId) ? pointId : SceneSettings.DEFAULT_POINT;
    // Zdejmujemy kamere na czas przestawienia: przy dalekim skoku klient moglby zgubic
    // encje-kamere, zanim dogoni ja gracz. tick() nalozy ja z powrotem po targetDelayTicks.
    view.retargeting = true;
    if (player.getGameMode() == GameMode.SPECTATOR) {
      player.setSpectatorTarget(null);
    }
    view.retargeting = false;
    player.teleport(point.camera());
    view.camera.teleport(point.camera());
    view.shield.teleport(shieldLocation(point.camera()));
    view.card.teleport(point.card() != null ? point.card() : point.camera());
    view.targetReadyTick = Bukkit.getCurrentTick() + settings.targetDelayTicks();
  }

  /** Miejsce postaci w kadrze, obrocone przodem do kamery. */
  Location subjectFacingCamera(SceneView view) {
    SceneSettings.Point point = view.currentPoint;
    Location at = point.subject().clone();
    Vector toCamera = point.camera().toVector().subtract(at.toVector()).setY(0);
    if (toCamera.lengthSquared() > 1.0E-4) {
      at.setDirection(toCamera);
      at.setPitch(0);
    }
    return at;
  }

  void setCard(SceneView view, Component text) {
    if (view.card == null) {
      return;
    }
    if (text == null) {
      view.card.text(Component.empty());
      view.card.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
    } else {
      view.card.text(text);
      view.card.setBackgroundColor(Color.fromARGB(190, 14, 18, 16));
    }
  }

  void pushHint(SceneView view) {
    Player player = view.player();
    if (player != null) {
      player.sendActionBar(view.hint);
    }
  }

  void ownEntity(SceneView view, Entity entity) {
    Player player = view.player();
    entity.setVisibleByDefault(false);
    if (player != null) {
      player.showEntity(plugin, entity);
    }
  }

  /**
   * Sciemnienie przez tytul z pelnoekranowym czarnym glifem Nexo. {@code &f} (tu: kolor
   * bialy) PRZED glifem jest obowiazkowy — bez niego klient barwi teksture tytulu i czern
   * wychodzi szara (patrz notatka o tincie tytulu).
   */
  void fade(SceneView view, Runnable atBlack) {
    Player player = view.player();
    if (player == null) {
      return;
    }
    if (settings.fadeGlyph().isBlank()) {
      atBlack.run();
      return;
    }
    int in = settings.fadeInTicks();
    int stay = settings.fadeStayTicks();
    int out = settings.fadeOutTicks();
    player.showTitle(Title.title(
        Component.text(settings.fadeGlyph(), NamedTextColor.WHITE),
        Component.empty(),
        Title.Times.times(ticks(in), ticks(stay), ticks(out))));
    view.busyUntilTick = Bukkit.getCurrentTick() + in + stay + out / 2;
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!view.closed) {
        atBlack.run();
      }
    }, in + 1L);
  }

  private static Duration ticks(int t) {
    return Duration.ofMillis(t * 50L);
  }

  boolean busy(SceneView view) {
    return Bukkit.getCurrentTick() < view.busyUntilTick;
  }

  /** Tarcza klikow: prostopadloscian przed kamera, srodkiem na linii wzroku. */
  private Location shieldLocation(Location camera) {
    Vector dir = camera.getDirection().normalize().multiply(settings.shieldDistance());
    Location at = camera.clone().add(dir);
    at.subtract(0, SHIELD_SIZE / 2.0, 0);
    at.setYaw(0);
    at.setPitch(0);
    return at;
  }

  private void aimCamera(SceneView view) {
    Player player = view.player();
    if (player == null || view.camera == null || !view.camera.isValid()) {
      return;
    }
    if (player.getGameMode() != GameMode.SPECTATOR) {
      player.setGameMode(GameMode.SPECTATOR);
    }
    if (player.getSpectatorTarget() != view.camera) {
      view.retargeting = true;
      try {
        player.setSpectatorTarget(view.camera);
      } finally {
        view.retargeting = false;
      }
    }
  }

  /** Straznik: kamera, tryb i podpowiedz. Co tick, bo kamera ma wrocic bez migniecia. */
  private void tick() {
    int now = Bukkit.getCurrentTick();
    for (SceneView view : views.values()) {
      Player player = view.player();
      if (player == null || !player.isOnline()) {
        continue;
      }
      if (now >= view.targetReadyTick) {
        aimCamera(view);
      }
      if (now % HINT_PERIOD_TICKS == 0) {
        pushHint(view);
      }
      if (now % settings.hudRepeatTicks() == 0) {
        runAll(settings.hudRepeat(), player);
      }
    }
  }

  /**
   * HUD innych pluginow na czas sceny. Spectator chowa vanillowy HUD, ale nie paski
   * MythicHUD (niewidzialny bossbar + shader) ani action bar MMOCore z zyciem i mana — ten
   * drugi co chwile nadpisywal podpowiedz sterowania (zlapane na kliencie QA). Komendy sa
   * w configu, a nie w kodzie: bez twardej zaleznosci od tych pluginow i z mozliwoscia
   * dopisania kolejnego bez przebudowy jara.
   */
  private void runAll(java.util.List<String> commands, Player player) {
    for (String command : commands) {
      ConsoleCommandRunner.run(plugin, command, player);
    }
  }

  // --- gracze nie widza sie nawzajem ---------------------------------------------------

  /** Spectatorzy widza sobie polprzezroczyste glowy — chowamy w obie strony. */
  private void isolate(Player player) {
    for (Player other : Bukkit.getOnlinePlayers()) {
      if (!other.equals(player)) {
        player.hidePlayer(plugin, other);
        other.hidePlayer(plugin, player);
      }
    }
  }

  private void reveal(Player player) {
    for (Player other : Bukkit.getOnlinePlayers()) {
      if (other.equals(player)) {
        continue;
      }
      other.showPlayer(plugin, player);
      if (!views.containsKey(other.getUniqueId())) {
        player.showPlayer(plugin, other);
      }
    }
  }

  /** Nowy gracz na serwerze: ma nie widziec nikogo w scenie i odwrotnie. */
  void onJoin(Player joined) {
    for (SceneView view : views.values()) {
      Player inScene = view.player();
      if (inScene != null && !inScene.equals(joined)) {
        inScene.hidePlayer(plugin, joined);
        joined.hidePlayer(plugin, inScene);
      }
    }
  }
}
