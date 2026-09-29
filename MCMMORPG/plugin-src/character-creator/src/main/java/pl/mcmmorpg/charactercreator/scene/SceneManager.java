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
  private static final int PANEL_LINE_WIDTH = 230;
  private static final float PANEL_SCALE = 0.5f;

  private final Plugin plugin;
  private final Logger logger;
  private final Map<UUID, SceneView> views = new ConcurrentHashMap<>();
  private SceneSettings settings;
  private BukkitTask task;
  /** Kto moze byc w swiecie sceny bez sceny (stary kreator, admin). Null = straznik wylaczony. */
  private java.util.function.Predicate<Player> strayExempt;
  private java.util.function.Supplier<Location> strayExit;

  /**
   * Straznik zablakanych: gracz w swiecie sceny, ktory nie jest w scenie, nie ma tam czego
   * szukac — odsylamy go na wyjscie i zdejmujemy SPECTATOR. Chroni przed stanem zapisanym
   * przez MMOProfiles w trakcie sceny (wyjscie z gry, crash serwera), ktory wraca przy
   * wyborze postaci, a nie przy samym wejsciu na serwer.
   */
  public void setStrayGuard(java.util.function.Predicate<Player> exempt,
      java.util.function.Supplier<Location> exit) {
    this.strayExempt = exempt;
    this.strayExit = exit;
  }

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
    view.card = spawnPanel(point.card() != null ? point.card() : cam, TextDisplay.TextAlignment.LEFT);
    Location listAt = point.listOrMirror();
    view.list = spawnPanel(listAt != null ? listAt : cam, TextDisplay.TextAlignment.LEFT);
    for (Entity e : new Entity[] {view.camera, view.shield, view.card, view.list}) {
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
    close(playerId, null);
  }

  /**
   * Zamyka scene i stawia gracza w {@code destination} zamiast w miejscu sprzed wejscia —
   * po przelocie gracz jest juz w swiecie gry i powrot do sali oznaczalby dwa ekrany
   * ladowania pod rzad.
   */
  public void close(UUID playerId, Location destination) {
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
    for (Entity e : new Entity[] {view.camera, view.shield, view.card, view.list}) {
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
    stopMusic(view, player);
    player.clearTitle();
    player.sendActionBar(Component.empty());
    player.resetPlayerTime();
    player.resetPlayerWeather();
    if (view.topBar != null) {
      player.hideBossBar(view.topBar);
    }
    runAll(settings.hudOnClose(), player);
    GameMode mode = view.originMode() == null ? GameMode.SURVIVAL : view.originMode();
    if (destination != null) {
      player.teleport(destination);
      player.setGameMode(mode);
    } else {
      player.setGameMode(mode);
      player.teleport(view.origin());
    }
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
    view.freeCamera = false;
    for (Display d : new Display[] {view.camera, view.card, view.list}) {
      d.setTeleportDuration(0);
    }
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
    Location listAt = point.listOrMirror();
    view.list.teleport(listAt != null ? listAt : point.camera());
    view.targetReadyTick = Bukkit.getCurrentTick() + settings.targetDelayTicks();
    applyAmbience(view, player);
  }

  /**
   * Kamera w dowolnym miejscu (scena wyboru stawia ja przed wybrana postacia), klimat
   * z kadru {@code ambiencePointId}. Panele ida za kamera: 3,1 bloku przed nia, 2,1 w bok —
   * tak jak w kadrach z configu (blizej zaslanialy postac).
   */
  void placeCamera(SceneView view, Location at, String ambiencePointId) {
    Player player = view.player();
    SceneSettings.Point point = settings.point(ambiencePointId);
    if (player == null || point == null) {
      return;
    }
    boolean sameWorld = view.camera.getWorld() == at.getWorld();
    view.freeCamera = true;
    boolean ambienceChanged = view.currentPoint != point;
    view.currentPoint = point;
    view.currentPointId = ambiencePointId;
    view.retargeting = true;
    if (player.getGameMode() == GameMode.SPECTATOR) {
      player.setSpectatorTarget(null);
    }
    view.retargeting = false;
    for (Display d : new Display[] {view.camera, view.card, view.list}) {
      d.setTeleportDuration(0);
    }
    player.teleport(at);
    view.camera.teleport(at);
    view.shield.teleport(shieldLocation(at));
    view.card.teleport(panelAt(at, true));
    view.list.teleport(panelAt(at, false));
    view.targetReadyTick = Bukkit.getCurrentTick() + settings.targetDelayTicks() + (sameWorld ? 0 : 5);
    if (ambienceChanged) {
      applyAmbience(view, player);
    }
  }

  /**
   * Przejazd kamery po krokach. Kazdy krok to teleport encji-kamery z interpolacja po stronie
   * klienta (Display#setTeleportDuration), gracz-spectator jedzie z nia. Panele i tarcza
   * klikow jada razem z kamera. Wejscie jest polykane do konca przejazdu.
   */
  void glide(SceneView view, java.util.List<Location> steps, int ticksPerStep) {
    if (steps.isEmpty()) {
      return;
    }
    int t = Math.max(1, Math.min(59, ticksPerStep));
    view.busyUntilTick = Math.max(view.busyUntilTick, Bukkit.getCurrentTick() + (long) steps.size() * t);
    for (int i = 0; i < steps.size(); i++) {
      Location step = steps.get(i);
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (view.closed) {
          return;
        }
        for (Display d : new Display[] {view.camera, view.card, view.list}) {
          d.setTeleportDuration(t);
        }
        view.camera.teleport(step);
        view.card.teleport(panelAt(step, true));
        view.list.teleport(panelAt(step, false));
        view.shield.teleport(shieldLocation(step));
      }, (long) i * t);
    }
  }

  /** Karta (prawo) albo lista (lewo) wzgledem kamery. */
  private static Location panelAt(Location camera, boolean right) {
    Vector fwd = camera.getDirection().setY(0);
    if (fwd.lengthSquared() < 1.0E-6) {
      fwd = new Vector(0, 0, 1);
    }
    fwd.normalize();
    Vector rightVec = new Vector(-fwd.getZ(), 0, fwd.getX());
    Location at = camera.clone().add(fwd.multiply(3.1));
    at.add(rightVec.multiply(right ? 2.1 : -2.1));
    at.add(0, right ? -1.17 : -0.67, 0);
    return at;
  }

  /**
   * Przelot po stworzeniu postaci (D6). Pod sciemnieniem kamera przenosi sie do swiata gry,
   * leci po punktach i konczy nad startem; potem drugie sciemnienie i {@code done} (zamkniecie
   * sceny z miejscem docelowym). Panele, pasek i podpowiedz znikaja — przelot jest cichym
   * ujeciem, bez sterowania.
   */
  public void flyover(SceneView view, SceneSettings.Flyover fly, Runnable done) {
    Player player = view.player();
    if (player == null || fly == null || !fly.usable()) {
      done.run();
      return;
    }
    // Ekran przelotu nie ma sterowania. Poprzedni ekran sprzata po sobie (podglad postaci)
    // dopiero przy zamknieciu sceny — przekazujemy mu to zamkniecie.
    SceneController previous = view.controller;
    view.switchTo(new SceneController() {
      @Override
      public void onOpen(SceneView v) {
      }

      @Override
      public void onClose(SceneView v) {
        if (v.isClosed() && previous != null) {
          previous.onClose(v);
        }
      }
    });
    fade(view, () -> {
      Player p = view.player();
      if (p == null) {
        return;
      }
      setPanel(view.card, null);
      setPanel(view.list, null);
      setTopBar(view, null);
      view.hint(Component.empty());
      view.subjectEntity = null;
      view.flying = true;
      stopMusic(view, p);
      // W nocy z przelotu zostaje czern — pora lotu z configu, po wyladowaniu (pod
      // sciemnieniem) close() oddaje czas swiata.
      if (fly.time() >= 0) {
        p.setPlayerTime(fly.time(), false);
      } else {
        p.resetPlayerTime();
      }
      p.setPlayerWeather(org.bukkit.WeatherType.CLEAR);
      java.util.List<Location> path = new java.util.ArrayList<>();
      for (SceneSettings.FlyPoint fp : fly.path()) {
        Location at = new Location(fly.world(), fp.x(), fp.y(), fp.z());
        path.add(fly.look() == null ? at : SceneSettings.aimed(at, fly.look()));
      }
      // Kamera to encja — zostaje w swiecie sceny. W swiecie gry stawiamy nowa.
      Location first = path.get(0);
      ItemDisplay old = view.camera;
      view.retargeting = true;
      p.setSpectatorTarget(null);
      view.retargeting = false;
      view.camera = first.getWorld().spawn(first, ItemDisplay.class, e -> {
        e.setPersistent(false);
        e.setVisibleByDefault(false);
      });
      p.showEntity(plugin, view.camera);
      if (old != null && old.isValid()) {
        old.remove();
      }
      p.teleport(first);
      // Multiverse przestawia tryb gry przy zmianie swiata — spectator z powrotem od razu.
      p.setGameMode(GameMode.SPECTATOR);
      // Czern trzyma sie, az klient wczyta swiat (ekran "Loading terrain" przy zmianie swiata,
      // potem chunki trasy) — inaczej pierwsze sekundy lotu ida nad pustym niebem.
      int settle = settings.targetDelayTicks() + Math.max(10, fly.holdTicks());
      if (!settings.fadeGlyph().isBlank()) {
        p.showTitle(Title.title(Component.text(settings.fadeGlyph(), NamedTextColor.WHITE),
            Component.empty(), Title.Times.times(Duration.ZERO, ticks(settle), ticks(10))));
      }
      view.targetReadyTick = Bukkit.getCurrentTick() + settle;
      view.busyUntilTick = Long.MAX_VALUE;
      SceneSettings.Sound whoosh = settings.uiSounds().get("flyover");
      if (whoosh != null) {
        p.playSound(p, whoosh.key(), org.bukkit.SoundCategory.MASTER, whoosh.volume(), whoosh.pitch());
      }
      if (!fly.title().isBlank() || !fly.subtitle().isBlank()) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
          Player q = view.player();
          if (q != null && !view.closed) {
            q.showTitle(Title.title(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(fly.title()),
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(fly.subtitle()),
                Title.Times.times(Duration.ofMillis(800), Duration.ofMillis(2500), Duration.ofMillis(1000))));
          }
        }, settle + 10L);
      }
      // Ruch CO TICK po lamanej z lagodnym startem i hamowaniem. Skoki co odcinek (40 blokow
      // naraz, interpolowane tylko u klienta) dawaly w logu "moved too quickly": serwer
      // przestawia spectatora za kamera, a klient zglaszal pozycje z interpolacji.
      int total = 0;
      int[] ends = new int[path.size()];
      for (int i = 1; i < path.size(); i++) {
        total += fly.path().get(i).ticks();
        ends[i] = total;
      }
      final int totalTicks = Math.max(1, total);
      view.camera.setTeleportDuration(2);
      int[] tick = {0};
      Bukkit.getScheduler().runTaskTimer(plugin, task -> {
        if (view.closed || !view.camera.isValid()) {
          task.cancel();
          return;
        }
        tick[0]++;
        double eased = smooth(Math.min(1.0, tick[0] / (double) totalTicks)) * totalTicks;
        int seg = 1;
        while (seg < path.size() - 1 && eased > ends[seg]) {
          seg++;
        }
        double segStart = ends[seg - 1];
        double f = (eased - segStart) / Math.max(1, ends[seg] - segStart);
        Location a0 = path.get(seg - 1);
        Location a1 = path.get(seg);
        Location at = a0.clone().add(a1.toVector().subtract(a0.toVector()).multiply(f));
        if (fly.look() != null) {
          at = SceneSettings.aimed(at, fly.look());
        }
        view.camera.teleport(at);
        if (tick[0] >= totalTicks) {
          task.cancel();
          Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!view.closed) {
              view.busyUntilTick = 0;
              fade(view, done);
            }
          }, 15L);
        }
      }, settle, 1L);
    });
  }

  /** Lagodny start i hamowanie (smoothstep). */
  private static double smooth(double t) {
    return t * t * (3 - 2 * t);
  }

  /** Pora dnia i pogoda kadru — tylko dla tego gracza. */
  private void applyAmbience(SceneView view, Player player) {
    SceneSettings.Ambience a = view.currentPoint.ambience();
    if (a.time() >= 0) {
      player.setPlayerTime(a.time(), false);
    } else {
      player.resetPlayerTime();
    }
    player.setPlayerWeather(a.rain() ? org.bukkit.WeatherType.DOWNFALL : org.bukkit.WeatherType.CLEAR);
    view.nextSound.clear();
    view.nextGestureTick = -1;
    view.gestureOut = false;
    view.holdUntilTick = -1;
    view.headYaw = 0;
    view.targetYaw = 0;
    view.headPitch = a.idle().restPitch();
    view.targetPitch = a.idle().restPitch();
    applyMusic(view, player);
  }

  /** Czastki, odglosy tla i spokojne rozgladanie sie postaci — kazdy element w swoim rytmie. */
  private void animate(SceneView view, Player player, int now) {
    SceneSettings.Ambience a = view.currentPoint.ambience();
    for (SceneSettings.Emitter e : a.emitters()) {
      if (now % e.every() != 0) {
        continue;
      }
      Location at = e.at() != null ? e.at()
          : view.currentPoint.subject().clone().add(0, e.yOffset(), 0);
      player.spawnParticle(e.type(), at, e.count(), e.dx(), e.dy(), e.dz(), e.speed());
    }
    ambientSounds(view, player, a, now);
    Entity subject = view.subjectEntity;
    if (!(subject instanceof org.bukkit.entity.LivingEntity living) || !subject.isValid()
        || subject.getWorld() != player.getWorld()) {
      return;
    }
    lookAround(view, living, a.idle(), now);
  }

  /**
   * Rozgladanie: obraca sie SAMA GLOWA, tulow trzymany przodem do kamery (setRotation ustawia
   * tez tulow — wygladalo jak obrot calej postaci, zmierzone zrzutami). Glowa idzie do gestu
   * z predkoscia gestu, trzyma go i wraca do spoczynku. Zadnych zamachow bronia.
   */
  private void lookAround(SceneView view, org.bukkit.entity.LivingEntity living,
      SceneSettings.Idle idle, int now) {
    float front = frontYaw(view, living);
    if (idle.active()) {
      if (view.nextGestureTick < 0) {
        view.nextGestureTick = now + pause(idle, view);
      }
      if (!view.gestureOut && view.holdUntilTick < 0 && now >= view.nextGestureTick) {
        int pick = RANDOM.nextInt(idle.gestures().size());
        if (idle.gestures().size() > 1 && pick == view.lastGesture) {
          pick = (pick + 1) % idle.gestures().size();
        }
        view.lastGesture = pick;
        SceneSettings.Gesture g = idle.gestures().get(pick);
        view.targetYaw = g.yaw();
        view.targetPitch = idle.restPitch() + g.pitch();
        view.headSpeed = g.speed();
        view.gestureOut = true;
        view.holdUntilTick = -1;
        view.put(GESTURE_HOLD, g.hold());
      } else if (view.gestureOut && view.holdUntilTick < 0 && reached(view)) {
        Integer hold = view.get(GESTURE_HOLD);
        view.holdUntilTick = now + (hold == null ? 30 : hold);
      } else if (view.gestureOut && view.holdUntilTick >= 0 && now >= view.holdUntilTick) {
        // Powrot do spoczynku zawsze spokojny — nawet po nerwowym szarpnieciu.
        view.targetYaw = 0;
        view.targetPitch = idle.restPitch();
        view.headSpeed = Math.min(view.headSpeed, idle.speed());
        view.gestureOut = false;
        view.holdUntilTick = -1;
        view.nextGestureTick = now + pause(idle, view);
      }
    } else {
      view.targetYaw = 0;
      view.targetPitch = idle.restPitch();
    }
    view.headYaw = approach(view.headYaw, view.targetYaw, view.headSpeed);
    view.headPitch = approach(view.headPitch, view.targetPitch, view.headSpeed);
    living.setBodyYaw(front);
    Location loc = living.getLocation();
    float yaw = front + view.headYaw;
    if (loc.getYaw() != yaw || loc.getPitch() != view.headPitch) {
      living.setRotation(yaw, view.headPitch);
      living.setBodyYaw(front);
    }
  }

  private static final String GESTURE_HOLD = "cc.scene.gestureHold";
  private static final java.util.Random RANDOM = new java.util.Random();

  private static int pause(SceneSettings.Idle idle, SceneView view) {
    int span = Math.max(0, idle.pauseMax() - idle.pauseMin());
    return idle.pauseMin() + (span == 0 ? 0 : RANDOM.nextInt(span + 1));
  }

  private static boolean reached(SceneView view) {
    return Math.abs(view.headYaw - view.targetYaw) < 0.01f
        && Math.abs(view.headPitch - view.targetPitch) < 0.01f;
  }

  private static float approach(float current, float target, float step) {
    float d = target - current;
    if (Math.abs(d) <= step) {
      return target;
    }
    return current + Math.signum(d) * step;
  }

  /** Przod postaci: w strone kamery, w poziomie. */
  private float frontYaw(SceneView view, Entity subject) {
    Location from = subject.getLocation();
    Location cam = view.camera != null && view.camera.getWorld() == from.getWorld()
        ? view.camera.getLocation() : view.currentPoint.camera();
    Vector toCamera = cam.toVector().subtract(from.toVector()).setY(0);
    if (toCamera.lengthSquared() < 1.0E-4) {
      return from.getYaw();
    }
    return from.clone().setDirection(toCamera).getYaw();
  }

  /** Odglosy tla kadru, kazdy co {@code every} tickow z rozrzutem, tylko dla tego gracza. */
  private void ambientSounds(SceneView view, Player player, SceneSettings.Ambience a, int now) {
    java.util.List<SceneSettings.Sound> sounds = a.sounds();
    for (int i = 0; i < sounds.size(); i++) {
      SceneSettings.Sound snd = sounds.get(i);
      if (snd.every() <= 0) {
        continue;
      }
      Long next = view.nextSound.get(i);
      if (next == null) {
        view.nextSound.put(i, (long) now + RANDOM.nextInt(snd.every()));
        continue;
      }
      if (now < next) {
        continue;
      }
      int jitter = snd.jitter() > 0 ? RANDOM.nextInt(snd.jitter() + 1) : 0;
      view.nextSound.put(i, (long) now + snd.every() + jitter);
      Location at = view.camera != null ? view.camera.getLocation() : player.getLocation();
      player.playSound(at, snd.key(), snd.category(), snd.volume(), snd.pitch());
    }
  }

  /** Podklad kadru: gasi poprzedni, gra nowy (jeden raz — utwory trwaja kilka minut). */
  private void applyMusic(SceneView view, Player player) {
    SceneSettings.Sound music = view.currentPoint.ambience().music();
    String key = music == null ? null : music.key();
    if (java.util.Objects.equals(key, view.music)) {
      return;
    }
    stopMusic(view, player);
    // Muzyka gry klienta (utwory z menu i swiata) zagluszalaby podklad sceny.
    player.stopSound(org.bukkit.SoundCategory.MUSIC);
    if (music != null) {
      // Zrodlem dzwieku jest gracz — muzyka jedzie z nim (kamera zmienia miejsca w kadrze).
      player.playSound(player, music.key(), music.category(), music.volume(), music.pitch());
      view.music = key;
    }
  }

  private void stopMusic(SceneView view, Player player) {
    if (view.music != null) {
      player.stopSound(view.music);
      view.music = null;
    }
  }

  void playUi(SceneView view, String key) {
    Player player = view.player();
    SceneSettings.Sound snd = settings.uiSounds().get(key);
    if (player == null || snd == null) {
      return;
    }
    player.playSound(player, snd.key(), org.bukkit.SoundCategory.MASTER, snd.volume(), snd.pitch());
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

  /** Panel tekstowy na scenie (karta, lista): widoczny tylko dla gracza, zawsze przodem. */
  private TextDisplay spawnPanel(Location at, TextDisplay.TextAlignment alignment) {
    return at.getWorld().spawn(at, TextDisplay.class, e -> {
      e.setPersistent(false);
      e.setVisibleByDefault(false);
      e.setBillboard(Display.Billboard.CENTER);
      e.setAlignment(alignment);
      e.setLineWidth(PANEL_LINE_WIDTH);
      e.setShadowed(true);
      e.text(Component.empty());
      e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
      e.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
          new Vector3f(PANEL_SCALE, PANEL_SCALE, PANEL_SCALE), new AxisAngle4f()));
    });
  }

  void setPanel(TextDisplay panel, Component text) {
    if (panel == null) {
      return;
    }
    if (text == null) {
      panel.text(Component.empty());
      panel.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
    } else {
      panel.text(text);
      panel.setBackgroundColor(Color.fromARGB(190, 14, 18, 16));
    }
  }

  /**
   * Pasek u gory ekranu: bossbar z samym napisem. Kolor ZOLTY celowo — pack MythicHUD
   * podmienia {@code boss_bar/yellow_background} na przezroczysty, a przy postepie 0 pasek
   * rysuje tylko tlo, wiec zostaje sam tekst.
   */
  void setTopBar(SceneView view, Component text) {
    Player player = view.player();
    if (player == null) {
      return;
    }
    if (text == null) {
      if (view.topBar != null) {
        player.hideBossBar(view.topBar);
      }
      return;
    }
    if (view.topBar == null) {
      view.topBar = net.kyori.adventure.bossbar.BossBar.bossBar(text, 0f,
          net.kyori.adventure.bossbar.BossBar.Color.YELLOW,
          net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS);
    } else {
      view.topBar.name(text);
    }
    player.showBossBar(view.topBar);
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
    if (now % 20 == 0) {
      sendStraysHome();
    }
    for (SceneView view : views.values()) {
      Player player = view.player();
      if (player == null || !player.isOnline()) {
        continue;
      }
      if (now >= view.targetReadyTick) {
        aimCamera(view);
      }
      if (!view.flying) {
        animate(view, player, now);
      }
      if (now % HINT_PERIOD_TICKS == 0) {
        pushHint(view);
      }
      if (now % settings.hudRepeatTicks() == 0) {
        runAll(settings.hudRepeat(), player);
      }
    }
  }

  private void sendStraysHome() {
    if (strayExempt == null || strayExit == null) {
      return;
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.getWorld().getName().equals(settings.worldName())
          || views.containsKey(player.getUniqueId()) || player.isDead()
          || strayExempt.test(player)) {
        continue;
      }
      Location exit = strayExit.get();
      if (exit == null) {
        continue;
      }
      if (player.getGameMode() == GameMode.SPECTATOR) {
        player.setGameMode(GameMode.SURVIVAL);
      }
      player.teleport(exit);
      runAll(settings.hudOnClose(), player);
      logger.warning("Scena: " + player.getName() + " byl w " + settings.worldName()
          + " poza scena (stary zapis profilu?) — odeslany na wyjscie.");
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
