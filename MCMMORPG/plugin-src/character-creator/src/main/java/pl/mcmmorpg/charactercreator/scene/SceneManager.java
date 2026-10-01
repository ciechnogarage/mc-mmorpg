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

  private static float panelScale() {
    return (float) pl.mcmmorpg.charactercreator.Ustawienia.d("scena.panel-skala", PANEL_SCALE);
  }

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
    emptyHand(view, player);
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
    // Pierwsze wejscie: czern do chwili, gdy klient zbuduje kadr (naprawa 30.09 — klient QA
    // przez ~2 s po wejsciu na serwer pokazywal puste niebo z samymi panelami).
    fade(view, () -> { }, true);
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
    close(playerId, destination, false);
  }

  /**
   * Zamkniecie przy wyjsciu gracza z serwera: bez {@code hud.on-close}. MythicHUD zdejmuje
   * posiadacza HUD-u przy wyjsciu PRZED nami, wiec kazda komenda leciala w pustke i zostawiala
   * w logu "Invalid hud holder specified" + skladnie (3 razy na kazde wyjscie w scenie). Przy
   * nastepnym wejsciu MythicHUD i tak sklada domyslny uklad od nowa.
   */
  public void closeOnQuit(UUID playerId) {
    close(playerId, null, true);
  }

  private void close(UUID playerId, Location destination, boolean quitting) {
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
    if (!quitting) {
      runAll(settings.hudOnClose(), player);
    }
    GameMode mode = view.originMode() == null ? GameMode.SURVIVAL : view.originMode();
    player.setFallDistance(0f);
    if (destination != null) {
      player.teleport(destination);
      player.setGameMode(mode);
    } else {
      player.setGameMode(mode);
      player.teleport(view.origin());
    }
    // Slotu paska NIE przywracamy: miedzy zamknieciem sceny a wczytaniem postaci gracz nie ma atrybutow
    // MMOCore, a reszta klikniecia "Graj" przychodzi wlasnie wtedy — z bronia w rece znowu byl komunikat
    // o bieglosci (zmierzone). Wybrana postac i tak ustawia swoj slot z profilu MMOProfiles.
    reveal(player);
  }

  /**
   * Pusta reka na czas sceny: gracz bez wybranej postaci trzyma przedmiot ostatniej postaci, a MMOItems
   * sprawdza wymagania broni przy KAZDYM kliknieciu — takze LPM "Graj" w scenie. Bez wczytanej postaci
   * atrybuty MMOCore sa zerowe, wiec gracz dostawal "Nie mozesz tego uzyc — brakuje: Bieglosc: ..."
   * (zmierzone na klonie 30.09: rozdzka w rece = komunikat, pusty slot paska = brak). Przestawiamy tylko
   * wskaznik paska na pusty slot (ekwipunek nietkniety); wybrana potem postac i tak ustawia swoj slot.
   * Pelny pasek: bron z komponentami wloczni (AnimacjeBroni: piercing_weapon, attack_range od 2 bl.,
   * minimum_attack_charge) idzie do wolnego miejsca w plecaku — zawartosc ekwipunku ta sama, zmienia sie
   * tylko pozycja. Klient z taka bronia robi pchniecie zamiast uderzenia w encje, wiec LPM w tarcze sceny
   * nie dochodzil wcale (zgloszenie 01.10: wojownik, pelny pasek, topor w slocie 0). Inne przedmioty
   * zostaja — stale sloty (Dziennik) odtwarza CoreTools, przeniesienie zrobiloby duplikat.
   */
  private static void emptyHand(SceneView view, Player player) {
    org.bukkit.inventory.PlayerInventory inv = player.getInventory();
    org.bukkit.inventory.ItemStack held = inv.getItemInMainHand();
    if (held == null || held.getType().isAir()) {
      return;
    }
    for (int slot = 0; slot < 9; slot++) {
      org.bukkit.inventory.ItemStack it = inv.getItem(slot);
      if (it == null || it.getType().isAir()) {
        inv.setHeldItemSlot(slot);
        return;
      }
    }
    if (!blocksSceneClick(held)) {
      return;
    }
    for (int slot = 9; slot < 36; slot++) {
      org.bukkit.inventory.ItemStack it = inv.getItem(slot);
      if (it == null || it.getType().isAir()) {
        inv.setItem(slot, held);
        inv.setItem(inv.getHeldItemSlot(), null);
        return;
      }
    }
  }

  /** Przedmiot, z ktorym LPM klienta nie jest zwyklym uderzeniem w encje. */
  private static boolean blocksSceneClick(org.bukkit.inventory.ItemStack item) {
    return item.hasData(io.papermc.paper.datacomponent.DataComponentTypes.PIERCING_WEAPON)
        || item.hasData(io.papermc.paper.datacomponent.DataComponentTypes.KINETIC_WEAPON)
        || item.hasData(io.papermc.paper.datacomponent.DataComponentTypes.ATTACK_RANGE)
        || item.hasData(io.papermc.paper.datacomponent.DataComponentTypes.MINIMUM_ATTACK_CHARGE);
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
    view.aimedTick = -1;
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
      view.aimedTick = -1;
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
      e.setLineWidth(pl.mcmmorpg.charactercreator.Ustawienia.i("scena.panel-szerokosc", PANEL_LINE_WIDTH));
      e.setShadowed(true);
      e.text(Component.empty());
      e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
      e.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
          new Vector3f(panelScale(), panelScale(), panelScale()), new AxisAngle4f()));
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

  /** Czern trzyma najwyzej tyle — potem rozjasniamy, nawet gdy klient jeszcze doczytuje. */
  private static final int FADE_MAX_HOLD_TICKS = 40;
  /** Ile tickow po wyslaniu chunkow kadru czekamy, zeby klient zdazyl zbudowac teren. */
  private static final int FADE_SETTLE_TICKS = 3;
  /**
   * To samo po DALEKIM skoku kamery (inny kadr rasy, 300 blokow). Zmierzone na kliencie QA
   * (15 FPS): 3 ticki po wyslaniu chunkow to za malo — przez ~0,2 s bylo widac puste niebo.
   */
  private static final int FADE_SETTLE_FAR_TICKS = 10;
  private static final double FADE_FAR_JUMP = 64.0;
  /** Znacznik: sciemnienie w toku, czern jeszcze nasza (atBlack jej nie przejal). */
  private static final long FADE_OURS = Long.MAX_VALUE - 1;

  /**
   * Sciemnienie przez tytul z pelnoekranowym czarnym glifem Nexo. {@code &f} (tu: kolor
   * bialy) PRZED glifem jest obowiazkowy — bez niego klient barwi teksture tytulu i czern
   * wychodzi szara (patrz notatka o tincie tytulu).
   *
   * <p><b>Czern trzyma sie do gotowosci kadru</b> (naprawa 30.09). Kadry ras leza 300 blokow
   * od siebie; po teleporcie klient musi od nowa dostac i zbudowac chunki. Na kliencie QA
   * (15 FPS) przy stalym czasie czerni gracz przez ~0,5 s widzial puste niebo bez terenu,
   * postaci i paneli. Teraz: {@code fade.in-ticks} zaciemnienia, zmiana pod czernia, potem
   * czern co najmniej {@code fade.stay-ticks} i do chwili, gdy kamera jest podpieta, a chunki
   * 5x5 wokol kamery i postaci wyslane (+{@value #FADE_SETTLE_TICKS} ticki, po dalekim skoku
   * {@value #FADE_SETTLE_FAR_TICKS}), najwyzej {@value #FADE_MAX_HOLD_TICKS}; wtedy
   * {@code fade.out-ticks} rozjasnienia. UWAGA: "wyslane" to nie "narysowane" — klient QA
   * (programowy OpenGL, 15 FPS) potrafil budowac teren jeszcze ~1 s dluzej. Zmiana w tym samym
   * kadrze (np. klasa z listy) jest gotowa od razu, wiec trwa tyle co minimum.
   * Gdy {@code atBlack} przejmie ekran (przelot ustawia wlasny tytul), nie ruszamy go.
   */
  void fade(SceneView view, Runnable atBlack) {
    fade(view, atBlack, false);
  }

  /** @param forceFar czekaj jak po dalekim skoku (np. pierwsze wejscie w scene) */
  private void fade(SceneView view, Runnable atBlack, boolean forceFar) {
    Player player = view.player();
    if (player == null) {
      return;
    }
    if (settings.fadeGlyph().isBlank()) {
      atBlack.run();
      return;
    }
    int in = settings.fadeInTicks();
    int minHold = settings.fadeStayTicks();
    int out = settings.fadeOutTicks();
    Component glyph = Component.text(settings.fadeGlyph(), NamedTextColor.WHITE);
    player.showTitle(Title.title(glyph, Component.empty(),
        Title.Times.times(ticks(in), ticks(FADE_MAX_HOLD_TICKS + 5), ticks(out))));
    view.busyUntilTick = FADE_OURS;
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (view.closed) {
        return;
      }
      Location before = view.camera != null && view.camera.isValid() ? view.camera.getLocation() : null;
      atBlack.run();
      Location after = view.camera != null && view.camera.isValid() ? view.camera.getLocation() : null;
      boolean far = forceFar || before == null || after == null || before.getWorld() != after.getWorld()
          || before.distanceSquared(after) > FADE_FAR_JUMP * FADE_FAR_JUMP;
      int settle = far ? FADE_SETTLE_FAR_TICKS : FADE_SETTLE_TICKS;
      if (view.busyUntilTick != FADE_OURS) {
        return; // atBlack przejal czern (przelot) — jego sprawa, kiedy ja zdjac
      }
      int[] held = {0};
      int[] settled = {0};
      Bukkit.getScheduler().runTaskTimer(plugin, task -> {
        Player p = view.player();
        if (view.closed || p == null || view.busyUntilTick != FADE_OURS) {
          task.cancel();
          return;
        }
        held[0]++;
        // Tytul wyslany, gdy klient jest jeszcze na ekranie "Loading terrain", przepada
        // (pierwsze wejscie w scene) — ponawiamy czern, dopoki ja trzymamy.
        if (held[0] % 10 == 0) {
          p.showTitle(Title.title(glyph, Component.empty(),
              Title.Times.times(Duration.ZERO, ticks(FADE_MAX_HOLD_TICKS), ticks(out))));
        }
        boolean ready = held[0] >= minHold && Bukkit.getCurrentTick() > view.targetReadyTick
            && view.aimedTick >= 0 && chunksSent(view, p);
        settled[0] = ready ? settled[0] + 1 : 0;
        if (settled[0] >= settle || held[0] >= FADE_MAX_HOLD_TICKS) {
          task.cancel();
          p.showTitle(Title.title(glyph, Component.empty(),
              Title.Times.times(Duration.ZERO, ticks(1), ticks(out))));
          view.busyUntilTick = Bukkit.getCurrentTick() + 1 + out / 2;
          if (held[0] >= FADE_MAX_HOLD_TICKS && settled[0] < settle) {
            logger.fine("Scena: czern zdjeta po limicie " + FADE_MAX_HOLD_TICKS + " tickow dla " + p.getName());
          }
        }
      }, 1L, 1L);
    }, in + 1L);
  }

  /**
   * Czy klient dostal juz chunki kamery i postaci biezacego kadru — z pierscieniem 5x5:
   * klient buduje sekcje chunka dopiero, gdy ma wszystkich jego sasiadow, wiec sam srodek
   * "wyslany" nie znaczy jeszcze "widac teren" (zmierzone: puste niebo mimo wyslanego srodka).
   */
  private static boolean chunksSent(SceneView view, Player player) {
    java.util.List<Location> spots = new java.util.ArrayList<>();
    if (view.camera != null && view.camera.isValid()) {
      spots.add(view.camera.getLocation());
    }
    if (view.currentPoint != null && view.currentPoint.subject() != null) {
      spots.add(view.currentPoint.subject());
    }
    for (Location at : spots) {
      if (at.getWorld() != player.getWorld()) {
        return false;
      }
      int cx = at.getBlockX() >> 4;
      int cz = at.getBlockZ() >> 4;
      for (int dx = -2; dx <= 2; dx++) {
        for (int dz = -2; dz <= 2; dz++) {
          if (!player.isChunkSent(org.bukkit.Chunk.getChunkKey(cx + dx, cz + dz))) {
            return false;
          }
        }
      }
    }
    return true;
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

  /**
   * Podpina kamere — ale dopiero, gdy klient ma juz chunk, w ktorym ona stoi. Serwer wysyla
   * encje klientowi dopiero po jej chunku, a klient ignoruje pakiet kamery dla encji, ktorej
   * nie zna. Staly czas ({@code target-delay-ticks}) wystarczal na szybkim wejsciu, ale przy
   * wolnym logowaniu albo skoku 300 blokow do kadru innej rasy pakiet wyprzedzal encje:
   * gracz zostawal z WOLNA kamera, a LPM nic nie robil (zgloszenie 01.10). Dla pewnosci
   * {@link #refreshCamera} ponawia jeszcze sam pakiet.
   */
  private void aimCamera(SceneView view) {
    Player player = view.player();
    if (player == null || view.camera == null || !view.camera.isValid()) {
      return;
    }
    if (player.getGameMode() != GameMode.SPECTATOR) {
      player.setGameMode(GameMode.SPECTATOR);
    }
    if (player.getSpectatorTarget() != view.camera) {
      if (!cameraChunkSent(view, player)) {
        return;
      }
      view.retargeting = true;
      try {
        player.setSpectatorTarget(view.camera);
      } finally {
        view.retargeting = false;
      }
      view.aimedTick = Bukkit.getCurrentTick();
    }
  }

  private static boolean cameraChunkSent(SceneView view, Player player) {
    Location at = view.camera.getLocation();
    return at.getWorld() == player.getWorld()
        && player.isChunkSent(org.bukkit.Chunk.getChunkKey(at.getBlockX() >> 4, at.getBlockZ() >> 4));
  }

  /** Po podpieciu/teleporcie klient raz odpowiada pozycja z obrotem — to nie zgubiona kamera. */
  private static final int CAMERA_LOST_GRACE_TICKS = 10;
  private static final int CAMERA_LOST_MIN_GAP_TICKS = 20;

  /**
   * Klient obraca gracza w scenie = patrzy z wlasnych oczu, nie przez kamere (patrz
   * SceneListener#onMove). Podpinamy kamere od nowa: zdjecie + nalozenie celu wysyla pakiet
   * i teleport do kamery, a pakiet packetevents dokladamy na wszelki wypadek.
   */
  void cameraLost(SceneView view) {
    Player player = view.player();
    int now = Bukkit.getCurrentTick();
    if (player == null || view.flying || view.aimedTick < 0 || view.camera == null || !view.camera.isValid()
        || now - view.aimedTick < CAMERA_LOST_GRACE_TICKS || now - view.lostFixTick < CAMERA_LOST_MIN_GAP_TICKS
        || player.getGameMode() != GameMode.SPECTATOR) {
      return;
    }
    view.lostFixTick = now;
    view.lostCount++;
    if (view.lostCount <= 3) {
      logger.info("Scena: " + player.getName() + " ma wolna kamere (obrot gracza w scenie) — podpinam ponownie"
          + " (" + view.lostCount + ")");
    }
    view.retargeting = true;
    try {
      player.setSpectatorTarget(null);
      player.setSpectatorTarget(view.camera);
    } finally {
      view.retargeting = false;
    }
    view.aimedTick = now;
    if (CameraPacket.available()) {
      CameraPacket.send(player, view.camera);
    }
  }

  /** Ponowienia po podpieciu: 5 i 20 tickow (encja zdazy dojsc), potem co 2 s na wszelki wypadek. */
  private static final int[] CAMERA_RESEND_AT = {5, 20};
  private static final int CAMERA_RESEND_EVERY = 40;

  private void refreshCamera(SceneView view, Player player, int now) {
    if (view.aimedTick < 0 || view.camera == null || !view.camera.isValid()
        || player.getSpectatorTarget() != view.camera) {
      return;
    }
    long age = now - view.aimedTick;
    boolean early = age == CAMERA_RESEND_AT[0] || age == CAMERA_RESEND_AT[1];
    if (CameraPacket.available()) {
      if (early || (age > 0 && age % CAMERA_RESEND_EVERY == 0)) {
        CameraPacket.send(player, view.camera);
      }
    } else if (early) {
      // Bez packetevents: zdjecie i nalozenie celu w tym samym ticku — serwer wysle pakiet znowu.
      view.retargeting = true;
      try {
        player.setSpectatorTarget(null);
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
      // Tryb widza co tick, nie dopiero z kamera: przy przelocie Multiverse przestawia tryb
      // gry po zmianie swiata kilka tickow PO naszym setGameMode — zmierzone na klonie 30.09:
      // gracz przez ~1,5 s byl w survivalu nad zatoka i spadal z y=96 na y=65.
      if (player.getGameMode() != GameMode.SPECTATOR) {
        player.setGameMode(GameMode.SPECTATOR);
      }
      if (now >= view.targetReadyTick) {
        aimCamera(view);
        refreshCamera(view, player, now);
      }
      if (!view.flying) {
        animate(view, player, now);
      }
      if (now % Math.max(1, pl.mcmmorpg.charactercreator.Ustawienia.i("scena.podpowiedz-co-ticki", HINT_PERIOD_TICKS)) == 0) {
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
