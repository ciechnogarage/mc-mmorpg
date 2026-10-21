package pl.mcmmorpg.lobby;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class LobbyPlugin extends JavaPlugin {

  /** Podbicie = wyspa zbuduje sie od nowa przy nastepnym starcie. */
  private static final int BUILD_VERSION = 1;
  private static final String HOLO_TAG = "lobby_holo";
  private static final MiniMessage MM = MiniMessage.miniMessage();

  private World world;
  private Location spawn;
  private final Map<PortalSlot, Portal> portals = new EnumMap<>(PortalSlot.class);
  private final Map<PortalSlot, TextDisplay> portalHolos = new EnumMap<>(PortalSlot.class);
  private final Map<UUID, Queued> queue = new HashMap<>();
  private int neededPings;
  private String autoQueueServer;
  private volatile int pingRounds;
  private Component networkName;

  static final class Queued {
    Portal portal;
    final BossBar bar;
    final long since = System.currentTimeMillis();
    long lastAttempt;
    boolean announced;

    Queued(Portal portal, BossBar bar) {
      this.portal = portal;
      this.bar = bar;
    }
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    world = Bukkit.getWorlds().getFirst();
    spawn = new Location(world, 0.5, IslandBuilder.TOP + 1, 6.5, 180f, -8f);

    setupWorld();
    NamespacedKey marker = new NamespacedKey(this, "wersja");
    Integer built = world.getPersistentDataContainer().get(marker, PersistentDataType.INTEGER);
    if (built == null || built != BUILD_VERSION) {
      rebuild();
      world.getPersistentDataContainer().set(marker, PersistentDataType.INTEGER, BUILD_VERSION);
    }
    loadConfigAndPortals();

    getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    getServer().getPluginManager().registerEvents(new LobbyListener(this), this);

    Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::pingAll, 20L, 40L);
    Bukkit.getScheduler().runTaskTimer(this, this::tickQueue, 40L, 20L);
    Bukkit.getScheduler().runTaskTimer(this, this::particles, 40L, 5L);
  }

  @Override
  public void onDisable() {
    for (Map.Entry<UUID, Queued> e : queue.entrySet()) {
      Player p = Bukkit.getPlayer(e.getKey());
      if (p != null) p.hideBossBar(e.getValue().bar);
    }
    queue.clear();
    removeHolos();
  }

  // ------------------------------------------------------------------ swiat

  private void setupWorld() {
    world.setDifficulty(Difficulty.PEACEFUL);
    world.setSpawnLocation(spawn);
    world.setTime(getConfig().getLong("czas-dnia", 12600));
    world.setStorm(false);
    world.setThundering(false);
    for (String rule : List.of(
        "advance_time false", "advance_weather false", "spawn_mobs false", "fall_damage false",
        "spawn_phantoms false", "spawn_wandering_traders false", "spawn_patrols false",
        "show_advancement_messages false", "show_death_messages false", "immediate_respawn true",
        "random_tick_speed 0")) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "minecraft:execute in " + world.getKey() + " run gamerule " + rule);
    }
    // wyspa (z malymi wysepkami) trzymana w pamieci na stale: hologramy nie sa zapisywane
    // na dysk, wiec rozladowany chunk zgubilby je, a woda wodospadu stanelaby w pol drogi
    for (int cx = -5; cx <= 4; cx++) {
      for (int cz = -5; cz <= 4; cz++) {
        world.getChunkAt(cx, cz).addPluginChunkTicket(this);
      }
    }
    // kraniec nie jest lobby do niczego potrzebny (nether wylacza docker-compose)
    for (World w : List.copyOf(Bukkit.getWorlds())) {
      if (w != world && w.getEnvironment() == World.Environment.THE_END) Bukkit.unloadWorld(w, false);
    }
  }

  void rebuild() {
    long t0 = System.currentTimeMillis();
    removeHolos();
    new IslandBuilder(world).build();
    getLogger().info("Wyspa zbudowana w " + (System.currentTimeMillis() - t0) + " ms");
  }

  void loadConfigAndPortals() {
    reloadConfig();
    neededPings = Math.max(1, getConfig().getInt("gotowy-po-pingach", 3));
    autoQueueServer = getConfig().getString("auto-kolejka", "");
    networkName = MM.deserialize(getConfig().getString("nazwa-sieci", "MMORPG"));
    world.setTime(getConfig().getLong("czas-dnia", 12600));
    portals.clear();
    for (PortalSlot slot : PortalSlot.values()) {
      Portal portal = new Portal(slot, getConfig().getConfigurationSection("portale." + slot.key));
      portals.put(slot, portal);
      IslandBuilder.applyFill(world, slot, portal.active());
    }
    // kolejka wskazuje na stare obiekty portali - przepinamy ja na nowe po slocie
    for (Map.Entry<UUID, Queued> e : List.copyOf(queue.entrySet())) {
      Queued q = e.getValue();
      q.portal = portals.get(q.portal.slot);
      if (!q.portal.active()) {
        queue.remove(e.getKey());
        Player p = Bukkit.getPlayer(e.getKey());
        if (p != null) p.hideBossBar(q.bar);
      }
    }
    spawnHolos();
  }

  // ------------------------------------------------------------------ hologramy

  private void spawnHolos() {
    removeHolos();
    holo(new Location(world, 0.5, IslandBuilder.TOP + 6.2, 0.5),
        MM.serialize(networkName) + "<newline><gray>Wejdź w portal albo użyj kompasu", 1.6f);
    for (Portal portal : portals.values()) {
      PortalSlot s = portal.slot;
      Location at = new Location(world, s.x(0, 0) + 0.5, IslandBuilder.TOP + 8.6, s.z(0, 0) + 0.5);
      portalHolos.put(s, holo(at, portalText(portal), 1.3f));
    }
  }

  private TextDisplay holo(Location at, String text, float scale) {
    return world.spawn(at, TextDisplay.class, td -> {
      td.setPersistent(false);
      td.addScoreboardTag(HOLO_TAG);
      td.text(MM.deserialize(text));
      td.setBillboard(Display.Billboard.CENTER);
      td.setShadowed(true);
      td.setBackgroundColor(Color.fromARGB(80, 0, 0, 0));
      td.setAlignment(TextDisplay.TextAlignment.CENTER);
      td.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
          new Vector3f(scale, scale, scale), new AxisAngle4f()));
    });
  }

  private String portalText(Portal p) {
    String desc = p.active() && !p.description.isEmpty() ? "<newline><gray>" + p.description : "";
    return p.nameRaw + "<newline>" + p.statusLine(neededPings) + desc;
  }

  private void removeHolos() {
    portalHolos.clear();
    if (world == null) return;
    for (Entity e : world.getEntities()) {
      if (e.getScoreboardTags().contains(HOLO_TAG)) e.remove();
    }
  }

  // ------------------------------------------------------------------ status serwerow

  private void pingAll() {
    for (Portal p : portals.values()) {
      if (!p.active()) continue;
      int players = ServerPinger.ping(p.host, 25565, 1500);
      if (players >= 0) {
        p.players = players;
        p.online = true;
        p.okStreak++;
      } else {
        p.online = false;
        p.okStreak = 0;
      }
    }
    pingRounds++;
    Bukkit.getScheduler().runTask(this, () -> {
      for (Portal p : portals.values()) {
        TextDisplay td = portalHolos.get(p.slot);
        if (td != null && td.isValid()) td.text(MM.deserialize(portalText(p)));
      }
    });
  }

  boolean statusKnown() {
    return pingRounds >= neededPings;
  }

  // ------------------------------------------------------------------ laczenie i kolejka

  /** Wejscie w portal / klik w menu. Serwer gotowy = przenosimy, inaczej kolejka. */
  void travel(Player player, Portal portal) {
    if (!portal.active()) {
      player.sendActionBar(MM.deserialize("<gray>Ten portal jeszcze śpi. <white>Wkrótce."));
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
      return;
    }
    if (portal.ready(neededPings)) {
      Queued q = queue.remove(player.getUniqueId());
      if (q != null) player.hideBossBar(q.bar);
      player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.25f, 1.6f);
      player.showTitle(Title.title(portal.name, MM.deserialize("<gray>Przenoszę…"),
          Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1500), Duration.ofMillis(400))));
      connect(player, portal.server);
    } else {
      enqueue(player, portal, false);
    }
  }

  void enqueue(Player player, Portal portal, boolean automatic) {
    Queued old = queue.get(player.getUniqueId());
    if (old != null && old.portal == portal) return;
    if (old != null) player.hideBossBar(old.bar);
    BossBar bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    Queued q = new Queued(portal, bar);
    queue.put(player.getUniqueId(), q);
    updateBar(q);
    player.showBossBar(bar);
    player.sendMessage(MM.deserialize(automatic
        ? "<yellow>⌛</yellow> " + portal.nameRaw + " <gray>właśnie się uruchamia. Poczekaj tu - przeniosę cię sam, gdy wstanie."
            + "<newline><dark_gray>Kompas → „Opuść kolejkę”, jeśli wolisz zostać."
        : "<yellow>⌛</yellow> " + portal.nameRaw + " <gray>jeszcze nie działa. Jesteś w kolejce - przeniosę cię, gdy wstanie."));
    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.2f);
  }

  void leaveQueue(Player player) {
    Queued q = queue.remove(player.getUniqueId());
    if (q == null) return;
    player.hideBossBar(q.bar);
    player.sendActionBar(MM.deserialize("<gray>Opuściłeś kolejkę."));
  }

  Queued queued(Player player) {
    return queue.get(player.getUniqueId());
  }

  void forget(Player player) {
    queue.remove(player.getUniqueId());
  }

  private void tickQueue() {
    long now = System.currentTimeMillis();
    for (Map.Entry<UUID, Queued> e : List.copyOf(queue.entrySet())) {
      Player p = Bukkit.getPlayer(e.getKey());
      Queued q = e.getValue();
      if (p == null) {
        queue.remove(e.getKey());
        continue;
      }
      if (q.portal.ready(neededPings) && now - q.lastAttempt > 8000) {
        if (!q.announced) {
          q.announced = true;
          p.showTitle(Title.title(MM.deserialize("<green>Serwer wstał!"), q.portal.name,
              Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1800), Duration.ofMillis(400))));
          p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        }
        q.lastAttempt = now;
        connect(p, q.portal.server);
      }
      updateBar(q);
    }
  }

  private void updateBar(Queued q) {
    long secs = (System.currentTimeMillis() - q.since) / 1000;
    String time = secs >= 60 ? (secs / 60) + " min " + (secs % 60) + " s" : secs + " s";
    if (q.portal.ready(neededPings)) {
      q.bar.name(MM.deserialize("<green>Łączę z </green>" + q.portal.nameRaw + "<green>…"));
      q.bar.color(BossBar.Color.GREEN);
      q.bar.progress(1f);
    } else {
      String state = q.portal.online ? "<yellow>wczytuje się" : "<red>jeszcze nie odpowiada";
      q.bar.name(MM.deserialize("<gray>Czekasz na </gray>" + q.portal.nameRaw + " <dark_gray>·</dark_gray> " + state
          + " <dark_gray>·</dark_gray> <white>" + time));
      q.bar.color(q.portal.online ? BossBar.Color.YELLOW : BossBar.Color.RED);
      // "oddech" paska, zeby bylo widac, ze kolejka zyje
      q.bar.progress((float) (0.5 + 0.5 * Math.sin(secs * 0.8)));
    }
  }

  private void connect(Player player, String server) {
    try {
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(buf);
      out.writeUTF("Connect");
      out.writeUTF(server);
      player.sendPluginMessage(this, "BungeeCord", buf.toByteArray());
    } catch (IOException e) {
      getLogger().warning("Nie udalo sie wyslac Connect: " + e.getMessage());
    }
  }

  // ------------------------------------------------------------------ bajery

  private void particles() {
    ThreadLocalRandom r = ThreadLocalRandom.current();
    for (Portal p : portals.values()) {
      if (!p.active()) continue;
      PortalSlot s = p.slot;
      double a = r.nextDouble(-2.5, 2.5);
      double y = IslandBuilder.TOP + r.nextDouble(0.5, 6.5);
      Location at = new Location(world, s.x(0, 0) + 0.5 - s.dirZ * a, y, s.z(0, 0) + 0.5 + s.dirX * a);
      Particle kind = p.ready(neededPings) ? Particle.END_ROD : Particle.SMOKE;
      world.spawnParticle(kind, at, 1, 0.05, 0.05, 0.05, 0.01);
    }
    world.spawnParticle(Particle.END_ROD, 0.5, IslandBuilder.TOP + 5.2, 0.5, 1, 0.3, 0.3, 0.3, 0.02);
  }

  // ------------------------------------------------------------------ dostep dla listenera

  World world() {
    return world;
  }

  Location spawn() {
    return spawn.clone();
  }

  Component networkName() {
    return networkName;
  }

  Map<PortalSlot, Portal> portals() {
    return portals;
  }

  int neededPings() {
    return neededPings;
  }

  Portal autoQueuePortal() {
    if (autoQueueServer == null || autoQueueServer.isEmpty()) return null;
    for (Portal p : portals.values()) if (p.server.equalsIgnoreCase(autoQueueServer)) return p;
    return null;
  }

  Portal nearestPortal(Location loc) {
    Portal best = null;
    double bestD = Double.MAX_VALUE;
    for (Portal p : portals.values()) {
      double d = Math.hypot(loc.getX() - p.slot.x(0, 0), loc.getZ() - p.slot.z(0, 0));
      if (d < bestD) {
        bestD = d;
        best = p;
      }
    }
    return bestD < 6 ? best : null;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String sub = args.length == 0 ? "" : args[0].toLowerCase();
    switch (sub) {
      case "przebuduj" -> {
        rebuild();
        loadConfigAndPortals();
        for (Player p : world.getPlayers()) p.teleport(spawn());
        sender.sendMessage("Wyspa zbudowana od nowa.");
      }
      case "przeladuj" -> {
        loadConfigAndPortals();
        sender.sendMessage("Config przeladowany, portale i hologramy odswiezone.");
      }
      default -> {
        return false;
      }
    }
    return true;
  }
}
