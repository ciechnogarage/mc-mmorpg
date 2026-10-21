package pl.mcmmorpg.mapa;

import de.pianoman911.mapengine.api.MapEngineApi;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

final class PelnaMapa implements Listener {
  private final MapaPlugin plugin;
  private final MapEngineApi api;
  private final NamespacedKey kZasieg;
  private final PelnaRysownik renderer = new PelnaRysownik();
  private final Map<UUID, PelnaSesja> sessions = new HashMap<>();
  private volatile Set<UUID> activeIds = Set.of();
  private final Set<UUID> pendingRight = ConcurrentHashMap.newKeySet();
  private final Set<UUID> pendingAttack = ConcurrentHashMap.newKeySet();
  private final PelnaPakiety packets;
  private Map<String, BufferedImage> icons = Map.of();
  private long tick;

  PelnaMapa(MapaPlugin plugin) {
    this.plugin = plugin;
    this.kZasieg = new NamespacedKey(plugin, "pelna_zasieg");
    this.api = Bukkit.getServicesManager().load(MapEngineApi.class);
    if (api == null) throw new IllegalStateException("MapEngine nie udostępnia MapEngineApi");
    reloadIcons();
    packets = Bukkit.getPluginManager().isPluginEnabled("packetevents") ? new PelnaPakiety(this, plugin) : null;
    Bukkit.getPluginManager().registerEvents(this, plugin);
    Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(), 1L, 1L);
  }

  void reloadIcons() {
    Map<String, BufferedImage> loaded = new HashMap<>();
    File folder = new File(plugin.getDataFolder(), "ikony");
    java.util.Set<String> names = new java.util.HashSet<>();
    for (MapaData.Marker marker : plugin.data().markers()) names.add(marker.icon());
    var ownIcons = plugin.getConfig().getConfigurationSection("wlasne.ikony");
    if (ownIcons != null) names.addAll(ownIcons.getKeys(false));
    for (String name : names) {
      if (name.isEmpty() || loaded.containsKey(name)) continue;
      if (!name.matches("[a-zA-Z0-9_-]+")) {
        plugin.getLogger().warning("Niepoprawna nazwa ikony: " + name);
        continue;
      }
      try {
        BufferedImage image = ImageIO.read(new File(folder, name + ".png"));
        if (image != null && image.getWidth() == 16 && image.getHeight() == 16) loaded.put(name, image);
        else plugin.getLogger().warning("Ikona musi miec 16x16 px: " + name);
      } catch (IOException ex) {
        plugin.getLogger().warning("Nie mozna wczytac ikony " + name + ": " + ex.getMessage());
      }
    }
    icons = Map.copyOf(loaded);
    for (PelnaSesja session : sessions.values()) session.dirty = true;
  }

  boolean open(Player player, String world, double x, double z) {
    close(player);
    int mapsWide = MapaMath.clamp(plugin.getConfig().getInt("pelna.szerokosc", 4), 1, 12);
    int mapsHigh = MapaMath.clamp(plugin.getConfig().getInt("pelna.wysokosc", 3), 1, 8);
    double distance = Math.max(1.0, Math.min(5.0, plugin.getConfig().getDouble("pelna.odleglosc", 2.2)));
    Location initial = player.getLocation();
    float yaw = PelnaMath.kierunek(initial.getYaw());
    Location aligned = initial.clone();
    aligned.setYaw(yaw);
    aligned.setPitch(0);
    // ramki stoja w pelnych blokach: ustaw gracza na osi ekranu (parzysta szerokosc -> granica bloku,
    // nieparzysta -> srodek bloku), inaczej obraz jest przesuniety w bok o pol bloku
    boolean osX = Math.floorMod(Math.round(yaw / 90f), 2) == 0;   // patrzy N/S -> szerokosc wzdluz X
    double boczna = osX ? aligned.getX() : aligned.getZ();
    double wyrownana = mapsWide % 2 == 0 ? Math.round(boczna) : Math.floor(boczna) + 0.5;
    if (osX) aligned.setX(wyrownana); else aligned.setZ(wyrownana);
    // Pierwszy teleport jest nasz; sesja powstaje dopiero po jego zakonczeniu.
    if (!player.teleport(aligned)) return false;
    Vector forward = aligned.getDirection();
    Vector right = new Vector(-Math.cos(Math.toRadians(yaw)), 0,
        -Math.sin(Math.toRadians(yaw)));
    Location eyes = player.getEyeLocation();
    double wantedX = eyes.getX() + forward.getX() * distance;
    double wantedZ = eyes.getZ() + forward.getZ() * distance;
    // Ramka w bloku N zajmuje N..N+1, wiec srodek kolumny/rzedu blokow lezy w (min + rozmiar/2).
    // Pion: srodek ekranu na wysokosci oczu. Poziom: srodek na osi gracza (wyrownanej wyzej).
    int bottomY = (int) Math.round(eyes.getY() - mapsHigh / 2.0);
    int topY = bottomY + mapsHigh - 1;
    int forwardX = (int) Math.floor(wantedX), forwardZ = (int) Math.floor(wantedZ);
    int leftX, leftZ, rightX, rightZ;
    if (osX) {
      int min = (int) Math.round(eyes.getX() - mapsWide / 2.0);
      boolean rosnie = right.getX() > 0;
      leftX = rosnie ? min : min + mapsWide - 1; rightX = rosnie ? min + mapsWide - 1 : min;
      leftZ = forwardZ; rightZ = forwardZ;
    } else {
      int min = (int) Math.round(eyes.getZ() - mapsWide / 2.0);
      boolean rosnie = right.getZ() > 0;
      leftZ = rosnie ? min : min + mapsWide - 1; rightZ = rosnie ? min + mapsWide - 1 : min;
      leftX = forwardX; rightX = forwardX;
    }
    // ramki zwrocone DO gracza (mapa w ramce widoczna tylko od przodu): gracz patrzy na poludnie -> ramki na polnoc
    BlockFace facing = switch (Math.floorMod(Math.round(yaw / 90f), 4)) {
      case 0 -> BlockFace.NORTH;
      case 1 -> BlockFace.EAST;
      case 2 -> BlockFace.SOUTH;
      default -> BlockFace.WEST;
    };
    // dokladna odleglosc: plaszczyzna stoi w pelnym bloku, wiec przesuwamy gracza wzdluz wzroku (max pol bloku),
    // inaczej ekran wychodzi raz wiekszy, raz mniejszy (2,5-3,5 bloku)
    double planeFwd = osX ? (forwardZ + 0.5 + forward.getZ() * 0.5) : (forwardX + 0.5 + forward.getX() * 0.5);
    Location dokladna = player.getLocation().clone();
    if (osX) dokladna.setZ(planeFwd - forward.getZ() * distance); else dokladna.setX(planeFwd - forward.getX() * distance);
    if (dokladna.distanceSquared(player.getLocation()) > 1e-6 && !player.teleport(dokladna)) return false;
    IMapDisplay display = api.displayProvider().createBasic(
        new BlockVector(leftX, topY, leftZ), new BlockVector(rightX, bottomY, rightZ), facing);
    try {
      IDrawingSpace drawing = api.pipeline().createDrawingSpace(display);
      drawing.ctx().receivers().add(player);
      drawing.ctx().buffering(true);
      // Dithering jest opcjonalny i domyslnie wylaczony; nie wybieramy FLOYD_STEINBERG.
      if (packets != null) packets.bezMachniecia(display);
      display.spawn(player);
      // plaszczyzna: srodek bloku + pol bloku w strone od gracza (ramka wisi na tylnej scianie bloku)
      double planeX = (leftX + rightX) / 2.0 + 0.5 + forward.getX() * 0.5;
      double planeZ = (leftZ + rightZ) / 2.0 + 0.5 + forward.getZ() * 0.5;
      double planeY = (topY + bottomY) / 2.0 + 0.5;
      sessions.put(player.getUniqueId(), new PelnaSesja(player, display, drawing,
          planeX, planeY, planeZ, yaw, mapsWide * 128, mapsHigh * 128,
          world, x, z, plugin.handZoom(player)));
      activeIds = Set.copyOf(sessions.keySet());
      // PPM musi siegac rogow ekranu (ok. 3,9 bl. przy 4x3 z 3 bl.), inaczej klient "uzywa" przedmiotu z reki
      // (ladowany cios broni, zaklecie). Tryb zaklec ustawia zasieg istot na 0 - dodatek liczony co tick.
      sessions.get(player.getUniqueId()).zasieg =
          Math.sqrt(distance * distance + mapsWide * mapsWide / 4.0 + mapsHigh * mapsHigh / 4.0) + 0.5;
      zasieg(player, sessions.get(player.getUniqueId()).zasieg);
    } catch (RuntimeException ex) {
      if (packets != null) packets.zapomnij(display);
      display.destroy();
      display.despawn(player);
      throw ex;
    }
    return true;
  }

  void close(Player player) {
    PelnaSesja session = sessions.remove(player.getUniqueId());
    if (session == null) return;
    activeIds = Set.copyOf(sessions.keySet());
    pendingRight.remove(session.id);
    pendingAttack.remove(session.id);
    if (plugin.dialogs() != null) plugin.dialogs().forget(player);
    zasieg(player, 0);
    if (!session.busy) destroy(session, player);
  }

  /** Dodatek do zasiegu istot tak, by laczny wynosil co najmniej {@code min}; 0 = zdejmij. */
  private void zasieg(Player player, double min) {
    AttributeInstance ai = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
    if (ai == null) return;
    AttributeModifier stary = ai.getModifier(kZasieg);
    double bez = ai.getValue() - (stary == null ? 0 : stary.getAmount());
    double dodatek = min > 0 ? Math.max(0, min - bez) : 0;
    if (stary != null && Math.abs(stary.getAmount() - dodatek) < 1e-6) return;
    if (stary != null) ai.removeModifier(kZasieg);
    if (dodatek > 0) ai.addTransientModifier(new AttributeModifier(kZasieg, dodatek,
        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
  }

  private void destroy(PelnaSesja session, Player player) {
    if (packets != null) packets.zapomnij(session.display);
    session.display.destroy();
    if (player != null) session.display.despawn(player);
  }

  boolean packetActive(UUID id) { return activeIds.contains(id); }

  void packetPending(UUID id, boolean attack) {
    if (attack) pendingAttack.add(id); else pendingRight.add(id);
  }

  void packetInput(UUID id, boolean attack) {
    try {
      PelnaSesja session = sessions.get(id);
      if (session == null) return;
      if (attack) {
        session.lastAttackTick = tick;
        Player player = Bukkit.getPlayer(id);
        if (player != null) click(player, session, true);
      } else session.lastRightTick = tick;
    } finally {
      if (attack) pendingAttack.remove(id); else pendingRight.remove(id);
    }
  }

  boolean active(Player player) { return sessions.containsKey(player.getUniqueId()); }
  void dirty(Player player) {
    PelnaSesja session = sessions.get(player.getUniqueId());
    if (session != null) session.dirty = true;
  }

  void closeAll() {
    for (UUID id : List.copyOf(sessions.keySet())) {
      Player player = Bukkit.getPlayer(id);
      if (player != null) close(player);
      else {
        PelnaSesja session = sessions.remove(id);
        if (session != null && !session.busy) destroy(session, null);
      }
    }
    activeIds = Set.of();
  }

  void shutdown() {
    closeAll();
    if (packets != null) packets.close();
  }

  private void tick() {
    tick++;
    if (sessions.isEmpty()) return;
    for (PelnaSesja session : List.copyOf(sessions.values())) {
      Player player = Bukkit.getPlayer(session.id);
      if (player == null || !player.isOnline()) continue;
      zasieg(player, session.zasieg);
      Location eye = player.getEyeLocation();
      Location position = player.getLocation();
      Vector ray = eye.getDirection();
      PelnaMath.Punkt cursor = PelnaMath.przeciecie(eye.getX(), eye.getY(), eye.getZ(),
          ray.getX(), ray.getY(), ray.getZ(), session.planeX, session.planeY,
          session.planeZ, session.facingYaw, session.width, session.height);
      session.cursor = cursor;
      if (cursor != null && session.previousCursor != null && tick - session.lastRightTick <= 6) {
        double du = cursor.u() - session.previousCursor.u();
        double dv = cursor.v() - session.previousCursor.v();
        if (du != 0 || dv != 0) {
          PelnaMath.Punkt center = PelnaMath.przesun(session.centerX, session.centerZ, du, dv, session.zoom);
          session.centerX = center.u();
          session.centerZ = center.v();
          session.dirty = true;
        }
      }
      session.previousCursor = cursor;
      if (session.busy || tick - session.lastSendTick < 2) continue;
      boolean visible = MapaData.klucz(player.getWorld().getName()).equals(MapaData.klucz(session.world));
      double playerX = position.getX(), playerZ = position.getZ();
      // kursor sterowany wzrokiem = ciagly obrot glowy; obracanie strzalki gracza wymuszalo pelne przerysowanie
      // 512x384 co 15 st. (pomiar 02.10: 4-5x wiecej GC). Na pelnym ekranie gracz stoi, wiec strzalka zostaje
      // w kierunku z chwili otwarcia, a ruch glowa przesuwa tylko kursor (wycinek 9x9).
      float playerYaw = session.base == null ? position.getYaw() : session.sentPlayerYaw;
      boolean redraw = PelnaMath.pelnePrzerysowanie(session.dirty, session.base != null,
          session.sentPlayerVisible, visible, session.sentPlayerX, session.sentPlayerZ,
          session.sentPlayerYaw, playerX, playerZ, playerYaw, session.zoom);
      if (!redraw && PelnaMath.tenSamPiksel(cursor, session.sentCursor)) continue;
      PelnaRysownik.Dane data = redraw ? new PelnaRysownik.Dane(session.width, session.height,
          session.world, session.centerX, session.centerZ, session.zoom,
          plugin.data().terrain(session.world), plugin.znacznikiDla(player, session.world),
          plugin.data().tabs().subList(0, Math.min(8, plugin.data().tabs().size())),
          icons, visible, playerX, playerZ, playerYaw, cursor, plugin.navigation().target(player)) : null;
      BufferedImage base = session.base;
      PelnaMath.Punkt oldCursor = session.sentCursor;
      session.busy = true;
      session.dirty = false;
      session.lastSendTick = tick;
      Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        BufferedImage newBase = base;
        RuntimeException failure = null;
        try {
          if (redraw) {
            PelnaRysownik.Obraz image = renderer.rysuj(data, null);
            newBase = image.base();
            session.drawing.image(image.image(), 0, 0);
          } else {
            if (oldCursor != null) {
              PelnaRysownik.Wycinek old = renderer.wycinekKursora(base, oldCursor, false);
              session.drawing.image(old.image(), old.x(), old.y());
            }
            if (cursor != null) {
              PelnaRysownik.Wycinek next = renderer.wycinekKursora(base, cursor, true);
              session.drawing.image(next.image(), next.x(), next.y());
            }
          }
          session.drawing.flush();
        } catch (RuntimeException ex) { failure = ex; }
        BufferedImage completedBase = newBase;
        RuntimeException completedFailure = failure;
        Bukkit.getScheduler().runTask(plugin, () -> {
          session.busy = false;
          if (sessions.get(session.id) != session) {
            destroy(session, Bukkit.getPlayer(session.id));
            return;
          }
          if (completedFailure != null) {
            session.dirty = true;
            plugin.getLogger().warning("Blad wysylania pelnej mapy: " + completedFailure.getMessage());
            return;
          }
          session.base = completedBase;
          session.sentCursor = cursor;
          if (redraw) {
            session.sentPlayerVisible = visible;
            session.sentPlayerX = playerX;
            session.sentPlayerZ = playerZ;
            session.sentPlayerYaw = playerYaw;
          }
        });
      });
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMove(PlayerMoveEvent event) {
    PelnaSesja session = sessions.get(event.getPlayer().getUniqueId());
    if (session == null || event instanceof PlayerTeleportEvent || event.getTo() == null) return;
    Location to = event.getTo();
    if (PelnaMath.zamknijPoRuchu(session.startX, session.startZ, to.getX(), to.getZ()))
      close(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTeleport(PlayerTeleportEvent event) {
    PelnaSesja session = sessions.get(event.getPlayer().getUniqueId());
    if (session != null) close(event.getPlayer());
  }

  @EventHandler public void onSneak(PlayerToggleSneakEvent event) {
    if (event.isSneaking()) close(event.getPlayer());
  }

  @EventHandler public void onQuit(PlayerQuitEvent event) { close(event.getPlayer()); }
  @EventHandler public void onWorld(PlayerChangedWorldEvent event) { close(event.getPlayer()); }
  @EventHandler public void onDeath(PlayerDeathEvent event) { close(event.getEntity()); }

  @EventHandler public void onHeld(PlayerItemHeldEvent event) {
    PelnaSesja session = sessions.get(event.getPlayer().getUniqueId());
    if (session == null) return;
    event.setCancelled(true);
    int delta = PelnaMath.kolko(event.getPreviousSlot(), event.getNewSlot());
    if (delta == 0) return;
    List<Double> zooms = plugin.zooms();
    int index = zooms.indexOf(session.zoom);
    int next = MapaMath.clamp(index + Integer.signum(delta), 0, zooms.size() - 1);
    double zoom = zooms.get(next);
    PelnaMath.Punkt cursor = session.cursor;
    if (cursor != null) {
      session.centerX += (cursor.u() - session.width / 2.0) * (session.zoom - zoom);
      session.centerZ += (cursor.v() - session.height / 2.0) * (session.zoom - zoom);
    }
    session.zoom = zoom;
    session.dirty = true;
  }

  @EventHandler public void onInteract(PlayerInteractEvent event) {
    PelnaSesja session = sessions.get(event.getPlayer().getUniqueId());
    if (session == null) return;
    event.setCancelled(true);
    if (event.getHand() != EquipmentSlot.HAND) return;
    switch (event.getAction()) {
      case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> session.lastRightTick = tick;
      case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> click(event.getPlayer(), session, false);
      default -> { }
    }
  }

  @EventHandler public void onInteractEntity(PlayerInteractEntityEvent event) {
    if (sessions.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
  }

  @EventHandler public void onAnimation(PlayerAnimationEvent event) {
    PelnaSesja session = sessions.get(event.getPlayer().getUniqueId());
    if (session != null && event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
      event.setCancelled(true);
      click(event.getPlayer(), session, false);
    }
  }

  @EventHandler public void onDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player && sessions.containsKey(player.getUniqueId()))
      event.setCancelled(true);
  }

  @EventHandler public void onBreak(BlockBreakEvent event) {
    if (sessions.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
  }

  private void click(Player player, PelnaSesja session, boolean packetAttack) {
    if (!packetAttack && (pendingRight.contains(session.id) || pendingAttack.contains(session.id)
        || PelnaMath.ignorujMachniecie(tick, session.lastRightTick, session.lastAttackTick))) return;
    if (session.lastClickTick == tick) return;
    session.lastClickTick = tick;
    PelnaMath.Punkt cursor = session.cursor;
    if (cursor == null) return;
    int button = PelnaMath.przycisk(cursor.u(), cursor.v(), session.width,
        Math.min(8, plugin.data().tabs().size()));
    if (button == -2) { close(player); return; }
    if (button == -3) {
      String world = session.world;
      double x = session.centerX, z = session.centerZ;
      close(player);
      plugin.openAtlas(player, world, x, z);
      return;
    }
    if (button >= 0) {
      MapaData.Tab tab = plugin.data().tabs().get(button);
      session.world = tab.world();
      session.zoom = plugin.zoomSwiata(tab.world());   // kazda mapa ma swoj domyslny zoom
      MapaData.Marker first = plugin.data().markers(tab.world()).stream().findFirst().orElse(null);
      if (MapaData.klucz(player.getWorld().getName()).equals(MapaData.klucz(tab.world()))) {
        session.centerX = player.getLocation().getX();
        session.centerZ = player.getLocation().getZ();
      } else if (first != null) {
        session.centerX = first.x();
        session.centerZ = first.z();
      } else {
        MapaData.Grid grid = plugin.data().grid(tab.world());
        if (grid != null) {
          MapaData.Level level = grid.level(Math.min(1, grid.levels().size() - 1));
          session.centerX = grid.x0() + level.columns() * 9.0 * level.bpp();
          session.centerZ = grid.z0() + level.rows() * 9.0 * level.bpp();
        }
      }
      session.dirty = true;
      return;
    }
    if (cursor.v() < PelnaMath.PASEK || cursor.v() >= session.height - 14) return;
    MapaData.Marker marker = PelnaMath.najblizszy(plugin.znacznikiDla(player, session.world),
        cursor.u(), cursor.v(), session.width, session.height, session.centerX, session.centerZ, session.zoom);
    if (marker != null) {
      if (marker.type().equals("wlasny")) plugin.dialogs().edit(player, marker);
      else plugin.navigation().toggle(player,
          new Prowadzenie.Cel(marker.world(), marker.x(), marker.z(), marker.name()));
      return;
    }
    PelnaMath.Punkt point = PelnaMath.punktSwiata(cursor.u(), cursor.v(), session.width,
        session.height, session.centerX, session.centerZ, session.zoom);
    plugin.dialogs().add(player, session.world, (int) Math.floor(point.u()), (int) Math.floor(point.v()));
  }
}
