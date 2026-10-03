package pl.mcmmorpg.mapa;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.MapId;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

public final class MapaPlugin extends JavaPlugin implements Listener {
  private static final NamespacedKey EMPTY_MODEL = new NamespacedKey("mapa", "pusty");
  private final Map<UUID, Double> handZooms = new HashMap<>();
  private final Map<UUID, AtlasState> atlases = new HashMap<>();
  private volatile MapaData data;
  private HandMapRenderer renderer;
  private PelnaMapa fullMap;
  private MinimapaZnaczniki minimapMarkers;
  private WlasneZnaczniki own;
  private Prowadzenie navigation;
  private ZnacznikDialog dialogs;
  private PanelMost panel;
  private final Map<UUID, Boolean> minimapStates = new HashMap<>();
  private MapView mapView;
  private MapyGraczy playerMaps = new MapyGraczy();
  private final Map<UUID, Integer> replacedMapIds = new HashMap<>();
  // numery map zajete przez innych: wspolna 30000 i minimapa NMinimap (map-id w jej configu). Osobista mapa
  // gracza z takim numerem = klient dostaje pod nim na zmiane nasz obraz i minimape -> "mapa znika" (02.10).
  private Set<Integer> reserved = Set.of();
  private final MapRenderer emptyRenderer = new MapRenderer() {
    @Override public void render(MapView view, org.bukkit.map.MapCanvas canvas, Player player) { }
  };
  private int mapId;
  private List<Double> zooms = List.of(0.25, 0.5, 1.0, 2.0, 4.0, 8.0);
  private double defaultZoom = 1;
  private long refreshMillis = 250;

  private static final class AtlasHolder implements InventoryHolder {
    private Inventory inventory;
    @Override public Inventory getInventory() { return inventory; }
  }

  private static final class AtlasState {
    final AtlasHolder holder;
    final UUID playerId;
    String world;
    int level;
    int left;
    int top;
    int playerSlot = -1;
    int playerDirection = -1;
    PanelMath.Filter filter = PanelMath.Filter.ALL;
    int page;
    PanelMath.Page<MapaData.Marker> visible = PanelMath.page(List.of(), 0);
    Component title;
    int panelX = Integer.MIN_VALUE;
    int panelZ = Integer.MIN_VALUE;

    AtlasState(AtlasHolder holder, UUID playerId, String world, int level) {
      this.holder = holder;
      this.playerId = playerId;
      this.world = world;
      this.level = level;
    }
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    own = new WlasneZnaczniki(new java.io.File(getDataFolder(), "gracze"), getLogger());
    loadAll();
    if (Bukkit.getPluginManager().isPluginEnabled("MapEngine")) {
      try { fullMap = new PelnaMapa(this); }
      catch (LinkageError | IllegalStateException ex) {
        getLogger().warning("Pelna mapa niedostepna: " + ex.getMessage());
      }
    } else getLogger().warning("MapEngine nie jest aktywny; pelna mapa jest wylaczona.");
    navigation = new Prowadzenie(this);
    if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
      try { panel = new PanelPakiety(); }
      catch (LinkageError | IllegalStateException ex) {
        getLogger().warning("Panel atlasu niedostepny: " + ex.getMessage());
      }
    }
    if (Bukkit.getPluginManager().isPluginEnabled("NMinimap")) {
      minimapMarkers = new MinimapaZnaczniki(this);
      Bukkit.getPluginManager().registerEvents(minimapMarkers, this);
    }
    Bukkit.getPluginManager().registerEvents(this, this);
    dialogs = new ZnacznikDialog(this);
    for (Player player : Bukkit.getOnlinePlayers()) {
      ownerKey(player);
      personalMap(player);
      replaceLegacyMaps(player);
    }
    Bukkit.getScheduler().runTaskTimer(this, this::refreshPlayerTiles, 20L, 20L);
    // klucz postaci MMOProfiles zmienia sie po wyborze postaci bez zdarzenia, ktore lapiemy; minimapa (async) czyta
    // tylko migawke kluczy, wiec odswiezamy ja co sekunde (recenzja 02.10: znaczniki poprzedniej postaci na minimapie)
    Bukkit.getScheduler().runTaskTimer(this, this::refreshOwnerKeys, 40L, 20L);
  }

  private void refreshOwnerKeys() {
    boolean profiles = Bukkit.getPluginManager().isPluginEnabled("MMOProfiles");
    for (Player player : Bukkit.getOnlinePlayers()) own.key(player, profiles);
  }

  @Override
  public void onDisable() {
    if (navigation != null) for (Player player : Bukkit.getOnlinePlayers()) navigation.clear(player);
    if (fullMap != null) fullMap.shutdown();
    detachRenderers();
    if (renderer != null) renderer.forgetAll();
    if (panel != null) {
      panel.close();
      panel = null;
      for (Player player : Bukkit.getOnlinePlayers()) if (atlasActive(player)) {
        player.closeInventory();
        player.updateInventory();
      }
    }
    atlases.clear();
    handZooms.clear();
    if (own != null) own.close();
  }

  MapaData data() { return data; }
  WlasneZnaczniki own() { return own; }
  Prowadzenie navigation() { return navigation; }
  ZnacznikDialog dialogs() { return dialogs; }
  boolean waypointsEnabled() { return Bukkit.getPluginManager().isPluginEnabled("packetevents"); }
  boolean mmoCoreEnabled() { return Bukkit.getPluginManager().isPluginEnabled("MMOCore"); }
  boolean fullActive(Player player) { return fullMap != null && fullMap.active(player); }
  void markDirty(Player player) {
    if (fullMap != null) fullMap.dirty(player);
    refreshPanel(player);
  }
  boolean atlasActive(Player player) {
    AtlasState state = atlases.get(player.getUniqueId());
    return state != null && player.getOpenInventory().getTopInventory() == state.holder.inventory;
  }
  void refreshAtlas(Player player) {
    AtlasState state = atlases.get(player.getUniqueId());
    if (state != null && atlasActive(player)) paintAll(player, state);
  }
  void restoreAtlas(Player player, String world, double x, double z) {
    if (atlasActive(player)) refreshAtlas(player);
    else Bukkit.getScheduler().runTask(this, () -> openAtlas(player, world, x, z));
  }
  UUID ownerKey(Player player) {
    UUID key = own.key(player, Bukkit.getPluginManager().isPluginEnabled("MMOProfiles"));
    own.publishKeys();
    return key;
  }
  List<MapaData.Marker> znacznikiDla(Player player, String world) {
    ownerKey(player);
    List<MapaData.Marker> result = new ArrayList<>(data.markers(world));
    result.addAll(own.forPlayer(player.getUniqueId(), world));
    return List.copyOf(result);
  }
  List<MapaData.Marker> znacznikiDlaAsync(UUID player, String world) {
    List<MapaData.Marker> result = new ArrayList<>(data.markers(world));
    result.addAll(own.asyncForPlayer(player, world));
    return List.copyOf(result);
  }
  long markerRevision() { return own.revision(); }
  // domyslny zoom swiata (zoom-reka-swiat, z fallbackiem na szablon lochu), inaczej globalny
  double zoomSwiata(String world) {
    String key = getConfig().contains("zoom-reka-swiat." + world) ? world : MapaData.klucz(world);
    double z = getConfig().getDouble("zoom-reka-swiat." + key, defaultZoom);
    return zooms.contains(z) ? z : defaultZoom;
  }
  // zoom wybrany przez gracza, inaczej domyslny dla swiata (zoom-reka-swiat), inaczej globalny
  double handZoom(Player player) {
    Double own = handZooms.get(player.getUniqueId());
    if (own != null) return own;
    String world = player.getWorld().getName();
    String key = getConfig().contains("zoom-reka-swiat." + world) ? world : MapaData.klucz(world);
    double perWorld = getConfig().getDouble("zoom-reka-swiat." + key, defaultZoom);
    return zooms.contains(perWorld) ? perWorld : defaultZoom;
  }
  long refreshMillis() { return refreshMillis; }
  List<Double> zooms() { return zooms; }

  private void loadAll() {
    reloadConfig();
    FileConfiguration config = getConfig();
    mapId = config.getInt("mapa-id", 30000);
    zooms = config.getList("zoom-reka", List.of()).stream().filter(Number.class::isInstance)
        .map(value -> ((Number) value).doubleValue())
        .filter(value -> value >= 1 || value == 0.5 || value == 0.25)
        .distinct().toList();
    if (zooms.isEmpty()) zooms = List.of(0.25, 0.5, 1.0, 2.0, 4.0, 8.0);
    defaultZoom = config.getDouble("zoom-reka-domyslny", 1);
    if (!zooms.contains(defaultZoom)) defaultZoom = zooms.get(0);
    refreshMillis = Math.max(0, config.getLong("odswiezanie-ms", 250));
    data = MapaData.load(getDataFolder(), config, getLogger());
    if (minimapMarkers != null) minimapMarkers.reloadRadius();
    handZooms.clear();
    if (renderer != null) renderer.forgetAll();
    detachRenderers();
    replacedMapIds.clear();
    Set<Integer> zajete = new HashSet<>(); zajete.add(mapId);
    File nmm = new File(getDataFolder().getParentFile(), "NMinimap/config.yml");
    if (nmm.isFile()) zajete.add(YamlConfiguration.loadConfiguration(nmm).getInt("map-id", 0));
    reserved = Set.copyOf(zajete);
    File registryFile = new File(getDataFolder(), "mapy-graczy.yml");
    playerMaps = MapyGraczy.fromYaml(YamlConfiguration.loadConfiguration(registryFile));
    mapView = Bukkit.getMap(mapId);
    renderer = new HandMapRenderer(this);
    if (mapView == null) getLogger().warning("Mapa o ID " + mapId + " nie istnieje; stare przedmioty czekaja na podmiane.");
    else configureMap(mapView, emptyRenderer);
    Set<Integer> used = new HashSet<>();
    boolean changed = false;
    for (Map.Entry<UUID, Integer> entry : playerMaps.entries().entrySet()) {
      int id = entry.getValue();
      MapView view = reserved.contains(id) || !used.add(id) ? null : Bukkit.getMap(id);
      if (view == null) {
        replacedMapIds.put(entry.getKey(), id);
        view = nowaMapa(worldForMap(entry.getKey()));
        playerMaps.put(entry.getKey(), view.getId());
        used.add(view.getId());
        changed = true;
      }
      configureMap(view, renderer);
    }
    if (changed) savePlayerMaps();
  }

  // nowa mapa vanilli z numerem spoza zarezerwowanych (pominiete dostaja pusty renderer, zeby nic nie rysowaly)
  private MapView nowaMapa(org.bukkit.World world) {
    MapView view = Bukkit.createMap(world);
    while (reserved.contains(view.getId())) {
      configureMap(view, emptyRenderer);
      view = Bukkit.createMap(world);
    }
    return view;
  }

  private org.bukkit.World worldForMap(UUID player) {
    Player online = Bukkit.getPlayer(player);
    return online != null ? online.getWorld() : Bukkit.getWorlds().get(0);
  }

  private void configureMap(MapView view, MapRenderer active) {
    for (MapRenderer old : List.copyOf(view.getRenderers())) view.removeRenderer(old);
    view.addRenderer(active);
    view.setTrackingPosition(false);
    view.setUnlimitedTracking(false);
    view.setLocked(true);
  }

  private void detachRenderers() {
    if (mapView != null) mapView.removeRenderer(emptyRenderer);
    if (renderer == null) return;
    for (int id : playerMaps.entries().values()) {
      MapView view = Bukkit.getMap(id);
      if (view != null) view.removeRenderer(renderer);
    }
  }

  Integer personalMapId(UUID player) { return playerMaps.id(player); }

  private void personalMap(Player player) {
    if (playerMaps.id(player.getUniqueId()) != null) return;
    MapView view = nowaMapa(player.getWorld());
    playerMaps.put(player.getUniqueId(), view.getId());
    configureMap(view, renderer);
    savePlayerMaps();
  }

  private void savePlayerMaps() {
    Path folder = getDataFolder().toPath();
    Path temp = null;
    try {
      Files.createDirectories(folder);
      temp = Files.createTempFile(folder, "mapy-graczy-", ".tmp");
      Files.writeString(temp, playerMaps.toYaml());
      Files.move(temp, folder.resolve("mapy-graczy.yml"), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      getLogger().warning("Nie mozna zapisac map graczy: " + ex.getMessage());
    } finally {
      if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
    }
  }

  private void replaceLegacyMaps(Player player) {
    personalMap(player);
    UUID playerId = player.getUniqueId();
    int id = playerMaps.id(playerId);
    Integer replaced = replacedMapIds.get(playerId);
    ItemStack[] items = player.getInventory().getContents();
    for (int slot = 0; slot < items.length; slot++) {
      ItemStack item = items[slot];
      int previousId = mapComponentId(item);
      if (previousId != mapId && (replaced == null || previousId != replaced)) continue;
      item.setData(DataComponentTypes.MAP_ID, MapId.mapId(id));
      player.getInventory().setItem(slot, item);
    }
  }

  // Stare mapy (opis "Klik — duza mapa") dostaja nowy opis sterowania. Zmieniamy TYLKO lore —
  // aktualizacja rewizji MMOItems przebudowalaby przedmiot i zgubila map_id.
  private static final String STARY_OPIS = "duża mapa ze znacznikami";
  private void odswiezOpis(Player player) {
    ItemStack[] zawartosc = player.getInventory().getContents();
    for (int i = 0; i < zawartosc.length; i++) {
      ItemStack item = zawartosc[i];
      if (!ourMap(item, player) || item.lore() == null) continue;
      List<Component> lore = item.lore();
      boolean stary = lore.stream().anyMatch(l -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
          .plainText().serialize(l).contains(STARY_OPIS));
      if (!stary) continue;
      List<Component> nowy = new ArrayList<>();
      for (Component l : lore) {
        String tekst = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(l);
        if (tekst.contains(STARY_OPIS)) {
          nowy.add(Component.text("PPM", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
              .append(Component.text(" — przybliż / oddal", NamedTextColor.GRAY)));
          nowy.add(Component.text("Shift+PPM", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
              .append(Component.text(" — mapa na cały ekran", NamedTextColor.GRAY)));
        } else nowy.add(l);
      }
      item.lore(nowy);
      player.getInventory().setItem(i, item);
    }
  }

  @EventHandler
  public void onJoinOpis(PlayerJoinEvent event) {
    Bukkit.getScheduler().runTaskLater(this, () -> {
      if (!event.getPlayer().isOnline()) return;
      replaceLegacyMaps(event.getPlayer());
      odswiezOpis(event.getPlayer());
    }, 40L);
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent event) {
    Bukkit.getScheduler().runTask(this, () -> replaceLegacyMaps(event.getPlayer()));
  }

  @EventHandler
  public void onHeldOpis(PlayerItemHeldEvent event) {
    if (ourMap(event.getPlayer().getInventory().getItem(event.getNewSlot()), event.getPlayer())) {
      Bukkit.getScheduler().runTask(this, () -> replaceLegacyMaps(event.getPlayer()));
      odswiezOpis(event.getPlayer());
    }
  }

  // czy gracz trzyma nasza mape w ktorejs rece (tanie: dwa odczyty ekwipunku)
  boolean trzymaMape(Player player) {
    return ourMap(player.getInventory().getItemInMainHand(), player)
        || ourMap(player.getInventory().getItemInOffHand(), player);
  }

  private int mapComponentId(ItemStack item) {
    // komponent MAP_ID zamiast getItemMeta(): bez kopiowania metadanych (wolane co tick dla kazdego gracza)
    if (item == null || item.getType() != Material.FILLED_MAP) return -1;
    MapId id = item.getData(DataComponentTypes.MAP_ID);
    return id == null ? -1 : id.id();
  }

  private boolean ourMap(ItemStack item, Player player) {
    int id = mapComponentId(item);
    Integer personal = personalMapId(player.getUniqueId());
    return id == mapId || personal != null && id == personal;
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    if (fullMap != null && fullMap.active(event.getPlayer())) return;
    if (event.getHand() != EquipmentSlot.HAND || !ourMap(event.getItem(), event.getPlayer())) return;
    replaceLegacyMaps(event.getPlayer());
    event.setCancelled(true);
    switch (event.getAction()) {
      case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> { }
      default -> { return; }
    }
    Player player = event.getPlayer();
    if (!player.isSneaking()) {
      double current = handZoom(player);
      int index = zooms.indexOf(current);
      double next = zooms.get((index + 1) % zooms.size());
      handZooms.put(player.getUniqueId(), next);
      if (renderer != null) renderer.invalidate(player.getUniqueId());
      String label = next < 1 ? (int) (1 / next) + " piksele na blok"
          : "1 piksel = " + (next == Math.rint(next) ? Integer.toString((int) next) : next) + " bl.";
      MMOCoreMost.actionBar(player, "Przybliżenie: " + label);
    } else if (fullMap != null) {
      openFull(player, player.getWorld().getName(), player.getLocation().getX(),
          player.getLocation().getZ());
    } else openAtlas(player);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) { ownerKey(event.getPlayer()); }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    UUID id = event.getPlayer().getUniqueId();
    handZooms.remove(id);
    atlases.remove(id);
    minimapStates.remove(id);
    if (panel != null) panel.hide(event.getPlayer());
    if (renderer != null) renderer.forget(id);
    own.forget(event.getPlayer());
    own.publishKeys();
    dialogs.forget(event.getPlayer());
    navigation.forget(event.getPlayer());
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player player
        && (ourMap(event.getCurrentItem(), player) || ourMap(event.getCursor(), player)))
      Bukkit.getScheduler().runTask(this, () -> replaceLegacyMaps(player));
    if (!(event.getView().getTopInventory().getHolder() instanceof AtlasHolder holder)) return;
    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)) return;
    AtlasState state = atlases.get(player.getUniqueId());
    if (state == null || state.holder != holder) return;
    int slot = event.getRawSlot();
    if (slot >= 54 && slot < 90 && panel != null) {
      panelClick(player, state, slot, event.isShiftClick(), event.isRightClick());
      return;
    }
    if (slot < 0 || slot >= 54) return;
    if (slot < 9) {
      if (slot == 8) {
        String world = state.world;
        MapaData.Grid grid = data.grid(world);
        if (grid != null) {
          MapaData.Level level = grid.level(state.level);
          double x = MapaMath.centerX(state.left, grid.x0(), level.bpp());
          double z = MapaMath.centerZ(state.top, grid.z0(), level.bpp());
          Bukkit.getScheduler().runTask(this, () -> {
            player.closeInventory();
            openFull(player, world, x, z);
          });
        }
        return;
      }
      MapaData.Tab tab = MapaData.tab(data.tabs(), slot);
      if (tab != null && data.grid(tab.world()) != null)
        Bukkit.getScheduler().runTask(this, () -> openAtlas(player, tab.world(), false));
      return;
    }
    if (slot < 45) {
      MapaData.Grid grid = data.grid(state.world);
      if (grid == null) return;
      MapaData.Level level = grid.level(state.level);
      int tx = MapaMath.tileX(slot, state.left);
      int tz = MapaMath.tileZ(slot, state.top);
      if (tx < 0 || tz < 0 || tx >= level.columns() || tz >= level.rows()) return;
      centerAt(state, grid.x0() + (tx + 0.5) * 18.0 * level.bpp(),
          grid.z0() + (tz + 0.5) * 18.0 * level.bpp());
      paintAll(player, state);
      return;
    }
    switch (slot) {
      case 45 -> {
        Bukkit.getScheduler().runTask(this, () -> {
          player.closeInventory();
          Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open hub_przygoda " + player.getName());
        });
      }
      case 46 -> move(player, state, -3, 0);
      case 47 -> move(player, state, 0, -3);
      case 48 -> move(player, state, 0, 3);
      case 49 -> move(player, state, 3, 0);
      case 50 -> {
        String targetWorld = data.grid(player.getWorld().getName()) != null
            ? data.grid(player.getWorld().getName()).world() : "world";
        if (data.grid(targetWorld) == null) player.sendMessage("Mapa świata jest niedostępna.");
        else if (!targetWorld.equals(state.world))
          Bukkit.getScheduler().runTask(this, () -> openAtlas(player));
        else {
          centerOnPlayerOrStart(player, state);
          paintAll(player, state);
        }
      }
      case 51 -> zoom(player, state, -1);
      case 52 -> zoom(player, state, 1);
      case 53 -> Bukkit.getScheduler().runTask(this, () -> player.closeInventory());
      default -> { }
    }
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (event.getView().getTopInventory().getHolder() instanceof AtlasHolder) event.setCancelled(true);
  }

  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    if (!(event.getInventory().getHolder() instanceof AtlasHolder holder)) return;
    AtlasState state = atlases.get(event.getPlayer().getUniqueId());
    if (state == null || state.holder != holder) return;
    atlases.remove(event.getPlayer().getUniqueId());
    if (panel != null && event.getPlayer() instanceof Player player) {
      panel.hide(player);
      Bukkit.getScheduler().runTask(this, () -> {
        if (player.isOnline()) player.updateInventory();
      });
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("mapa.admin")) {
      sender.sendMessage("Nie masz uprawnień.");
      return true;
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("otworz")) {
      Player target = Bukkit.getPlayerExact(args[1]);
      if (target == null) sender.sendMessage("Nie znaleziono gracza.");
      else openAtlas(target);
      return true;
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("pelna")) {
      Player target = Bukkit.getPlayerExact(args[1]);
      if (target == null) sender.sendMessage("Nie znaleziono gracza.");
      else openFull(target, target.getWorld().getName(), target.getLocation().getX(),
          target.getLocation().getZ());
      return true;
    }
    if (args.length >= 3 && args[0].equalsIgnoreCase("cel")) {
      Player target = Bukkit.getPlayerExact(args[1]);
      if (target == null) { sender.sendMessage("Nie znaleziono gracza."); return true; }
      if (args.length == 3 && args[2].equalsIgnoreCase("wyczysc")) {
        navigation.clear(target);
        sender.sendMessage("Prowadzenie wyłączone.");
        return true;
      }
      if (args.length >= 6) {
        try {
          int x = Integer.parseInt(args[3]), z = Integer.parseInt(args[4]);
          String world = args[2], name = String.join(" ", java.util.Arrays.copyOfRange(args, 5, args.length));
          if (!world.matches("[a-z0-9_.-]+") || name.isBlank()) throw new IllegalArgumentException();
          navigation.set(target, new Prowadzenie.Cel(world, x, z, name));
          sender.sendMessage("Prowadzę do: " + name);
        } catch (IllegalArgumentException ex) { sender.sendMessage("Niepoprawny świat lub współrzędne."); }
        return true;
      }
    }
    if (args.length == 1 && args[0].equalsIgnoreCase("przeladuj")) {
      if (fullMap != null) fullMap.closeAll();
      for (Player player : Bukkit.getOnlinePlayers())
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof AtlasHolder) player.closeInventory();
      atlases.clear();
      loadAll();
      for (Player player : Bukkit.getOnlinePlayers()) replaceLegacyMaps(player);
      if (fullMap != null) fullMap.reloadIcons();
      sender.sendMessage("Mapa przeładowana.");
      return true;
    }
    sender.sendMessage("Użycie: /mapa otworz <gracz> | /mapa pelna <gracz> | /mapa cel <gracz> <swiat> <x> <z> <nazwa> | /mapa cel <gracz> wyczysc | /mapa przeladuj");
    return true;
  }

  private void openFull(Player player, String world, double x, double z) {
    if (fullMap == null) {
      player.sendMessage("Pełna mapa jest niedostępna (brak MapEngine).");
      return;
    }
    player.closeInventory();
    try {
      if (!fullMap.open(player, world, x, z)) player.sendMessage("Nie można otworzyć pełnej mapy.");
    } catch (RuntimeException ex) {
      getLogger().warning("Nie mozna otworzyc pelnej mapy: " + ex.getMessage());
      player.sendMessage("Nie można otworzyć pełnej mapy.");
    }
  }

  private void openAtlas(Player player) {
    String world = data.grid(player.getWorld().getName()) != null
        ? data.grid(player.getWorld().getName()).world() : "world";
    openAtlas(player, world, true);
  }

  private void openAtlas(Player player, String world, boolean initial) {
    openAtlas(player, world, initial, null, null);
  }

  void openAtlas(Player player, String world, double x, double z) {
    openAtlas(player, world, false, x, z);
  }

  private void openAtlas(Player player, String world, boolean initial, Double x, Double z) {
    MapaData.Grid grid = data.grid(world);
    if (grid == null) {
      player.sendMessage("Mapa świata jest niedostępna.");
      return;
    }
    if (fullMap != null && fullMap.active(player)) fullMap.close(player);
    world = grid.world();
    int level = Math.min(1, grid.levels().size() - 1);
    AtlasHolder holder = new AtlasHolder();
    Component title = Component.text(getConfig().getString("tytul-okna", "")
        + "Mapa świata · " + worldName(world), NamedTextColor.WHITE);
    holder.inventory = Bukkit.createInventory(holder, 54, title);
    ownerKey(player);
    AtlasState state = new AtlasState(holder, player.getUniqueId(), world, level);
    state.title = title;
    if (x != null && z != null) centerAt(state, x, z);
    else if (initial) centerOnPlayerOrStart(player, state);
    else centerOnWorld(player, state);
    paintAll(player, state);
    atlases.put(player.getUniqueId(), state);
    if (panel != null) panel.show(player, title, panelItems(player, state));
    player.openInventory(holder.inventory);
  }

  private void centerOnPlayerOrStart(Player player, AtlasState state) {
    if (data.grid(player.getWorld().getName()) != null) {
      state.world = data.grid(player.getWorld().getName()).world();
      state.level = Math.min(state.level, data.grid(state.world).levels().size() - 1);
      centerAt(state, player.getLocation().getX(), player.getLocation().getZ());
      return;
    }
    state.world = "world";
    MapaData.Marker start = data.markers("world").stream()
        .filter(m -> m.world().equals("world") && m.type().equals("start")).findFirst().orElse(null);
    centerAt(state, start == null ? 149 : start.x(), start == null ? 197 : start.z());
  }

  private void centerOnWorld(Player player, AtlasState state) {
    if (sameWorld(player.getWorld().getName(), state.world)) {
      centerAt(state, player.getLocation().getX(), player.getLocation().getZ());
      return;
    }
    MapaData.Marker first = data.markers(state.world).stream().findFirst().orElse(null);
    MapaData.Grid grid = data.grid(state.world);
    MapaData.Level level = grid.level(state.level);
    if (first != null) centerAt(state, first.x(), first.z());
    else centerAt(state, grid.x0() + level.columns() * 9.0 * level.bpp(),
        grid.z0() + level.rows() * 9.0 * level.bpp());
  }

  private boolean sameWorld(String playerWorld, String shownWorld) {
    return playerWorld.equals(shownWorld) || playerWorld.matches(java.util.regex.Pattern.quote(shownWorld) + "_\\d+");
  }

  private String worldName(String world) {
    String key = getConfig().contains("nazwy-swiatow." + world) ? world : MapaData.klucz(world);
    return getConfig().getString("nazwy-swiatow." + key, world);
  }

  private void centerAt(AtlasState state, double x, double z) {
    MapaData.Grid grid = data.grid(state.world);
    if (grid == null) return;
    MapaData.Level level = grid.level(state.level);
    state.left = MapaMath.leftForCenter(x, grid.x0(), level.bpp(), level.columns());
    state.top = MapaMath.topForCenter(z, grid.z0(), level.bpp(), level.rows());
  }

  private void move(Player player, AtlasState state, int dx, int dz) {
    MapaData.Level level = data.grid(state.world).level(state.level);
    state.left = MapaMath.clamp(state.left + dx, -4, Math.max(-4, level.columns() - 5));
    state.top = MapaMath.clamp(state.top + dz, -4, Math.max(-4, level.rows() - 1));
    paintAll(player, state);
  }

  private void zoom(Player player, AtlasState state, int step) {
    MapaData.Grid grid = data.grid(state.world);
    int next = MapaMath.clamp(state.level + step, 0, grid.levels().size() - 1);
    if (next == state.level) return;
    MapaData.Level previous = grid.level(state.level);
    double x = MapaMath.centerX(state.left, grid.x0(), previous.bpp());
    double z = MapaMath.centerZ(state.top, grid.z0(), previous.bpp());
    state.level = next;
    centerAt(state, x, z);
    paintAll(player, state);
  }

  private void paintAll(Player player, AtlasState state) {
    Inventory inventory = state.holder.inventory;
    for (int slot = 0; slot < 9; slot++) inventory.setItem(slot, tabItem(state, slot));
    for (int slot = 9; slot < 45; slot++) inventory.setItem(slot, tileItem(state, slot, false, 0));
    String[] names = {"Wstecz", "←", "↑", "↓", "→", "Wyśrodkuj na mnie", "Przybliż", "Oddal", "Zamknij"};
    String[] models = {"nexo:hub_wstecz", "mapa:ui/lewo", "mapa:ui/gora", "mapa:ui/dol",
        "mapa:ui/prawo", "mapa:ui/srodek", "mapa:ui/plus", "mapa:ui/minus", "nexo:hub_zamknij"};
    for (int i = 0; i < names.length; i++) inventory.setItem(45 + i, item(models[i], names[i], List.of()));
    state.playerSlot = -1;
    state.playerDirection = -1;
    updatePlayerTile(player, state);
    refreshPanel(player);
  }

  private void panelClick(Player player, AtlasState state, int rawSlot, boolean shift, boolean right) {
    int slot = rawSlot - 54;
    if (slot < 27) {
      if (slot == 0 && state.visible.previous()) { state.page--; refreshPanel(player); return; }
      if (slot == 26 && state.visible.next()) { state.page++; refreshPanel(player); return; }
      MapaData.Marker marker = state.visible.marker(slot);
      if (marker == null) return;
      if (shift) {
        navigation.toggle(player, new Prowadzenie.Cel(marker.world(), marker.x(),
            marker.z(), marker.name()));
      } else if (right && marker.type().equals("wlasny")) {
        dialogs.editAtlas(player, marker, state.world, viewX(state), viewZ(state));
      } else {
        centerAt(state, marker.x(), marker.z());
        paintAll(player, state);
      }
      return;
    }
    switch (PanelMath.action(rawSlot)) {
      case ADD -> {
        boolean here = sameWorld(player.getWorld().getName(), state.world);
        int x = (int) Math.floor(here ? player.getLocation().getX() : viewX(state));
        int z = (int) Math.floor(here ? player.getLocation().getZ() : viewZ(state));
        dialogs.addAtlas(player, state.world, x, z, viewX(state), viewZ(state));
      }
      case FILTER -> {
        state.filter = state.filter.next();
        state.page = 0;
        refreshPanel(player);
      }
      case NAVIGATION -> {
        if (navigation.target(player) != null) navigation.clear(player);
      }
      case MINIMAP -> {
        boolean enabled = minimapEnabled(player);
        if (player.performCommand("minimap " + (enabled ? "disable" : "enable"))) {
          minimapStates.put(player.getUniqueId(), !enabled);
          refreshPanel(player);
        }
      }
      case FULL -> {
        String world = state.world;
        double x = viewX(state), z = viewZ(state);
        Bukkit.getScheduler().runTask(this, () -> {
          player.closeInventory();
          openFull(player, world, x, z);
        });
      }
      case CLOSE -> Bukkit.getScheduler().runTask(this, () -> player.closeInventory());
      default -> { }
    }
  }

  private double viewX(AtlasState state) {
    MapaData.Grid grid = data.grid(state.world);
    return MapaMath.centerX(state.left, grid.x0(), grid.level(state.level).bpp());
  }

  private double viewZ(AtlasState state) {
    MapaData.Grid grid = data.grid(state.world);
    return MapaMath.centerZ(state.top, grid.z0(), grid.level(state.level).bpp());
  }

  private void refreshPanel(Player player) {
    if (panel == null) return;
    AtlasState state = atlases.get(player.getUniqueId());
    if (state == null || !atlasActive(player)) return;
    panel.refresh(player, state.title, panelItems(player, state));
    Bukkit.getScheduler().runTask(this, () -> {
      if (player.isOnline() && atlasActive(player)) player.updateInventory();
    });
  }

  private List<ItemStack> panelItems(Player player, AtlasState state) {
    state.panelX = player.getLocation().getBlockX();
    state.panelZ = player.getLocation().getBlockZ();
    List<MapaData.Marker> markers = new ArrayList<>(znacznikiDla(player, state.world));
    markers.removeIf(marker -> !state.filter.accepts(marker.type()));
    boolean local = sameWorld(player.getWorld().getName(), state.world);
    if (local) markers.sort(Comparator.comparingDouble(marker ->
        Math.hypot(marker.x() + 0.5 - player.getLocation().getX(),
            marker.z() + 0.5 - player.getLocation().getZ())));
    else markers.sort(Comparator.comparing(MapaData.Marker::name, String.CASE_INSENSITIVE_ORDER));
    state.visible = PanelMath.page(markers, state.page);
    state.page = state.visible.index();
    List<ItemStack> items = new ArrayList<>(java.util.Collections.nCopies(36, new ItemStack(Material.AIR)));
    int offset = state.visible.previous() ? 1 : 0;
    for (int i = 0; i < state.visible.items().size(); i++) {
      MapaData.Marker marker = state.visible.items().get(i);
      List<Component> lore = new ArrayList<>();
      if (marker.type().equals("wlasny")) lore.add(Component.text("Twój znacznik", NamedTextColor.GRAY));
      else for (String line : marker.description()) lore.add(Component.text(line, NamedTextColor.GRAY));
      if (local) lore.add(Component.text("Odległość: " + Math.round(Math.hypot(
          marker.x() + 0.5 - player.getLocation().getX(),
          marker.z() + 0.5 - player.getLocation().getZ())) + " m", NamedTextColor.GRAY));
      lore.add(Component.text("Klik — pokaż na mapie", NamedTextColor.GRAY));
      lore.add(Component.text("Shift+klik — prowadź", NamedTextColor.GRAY));
      if (marker.type().equals("wlasny")) lore.add(Component.text("PPM — edytuj", NamedTextColor.GRAY));
      ItemStack icon = item("mapa:ikona/" + marker.icon(), marker.name(), lore);
      if (marker.type().equals("wlasny")) icon.setData(DataComponentTypes.ITEM_NAME,
          Component.text(marker.name(), NamedTextColor.GOLD));
      items.set(offset + i, icon);
    }
    if (state.visible.previous()) items.set(0, item("mapa:ui/lewo",
        "Poprzednia strona (" + (state.page + 1) + "/" + state.visible.total() + ")", List.of()));
    if (state.visible.next()) items.set(26, item("mapa:ui/prawo",
        "Następna strona (" + (state.page + 1) + "/" + state.visible.total() + ")", List.of()));
    items.set(27, item("mapa:ui/plus", "Dodaj znacznik tutaj", List.of()));
    items.set(28, item(state.filter.model, "Pokaż: " + state.filter.label,
        List.of(Component.text("Klik — następny filtr", NamedTextColor.GRAY))));
    Prowadzenie.Cel target = navigation.target(player);
    if (target != null) items.set(29, item("mapa:ikona/cel", "Prowadzę do: " + target.name(),
        List.of(Component.text("Klik — wyłącz prowadzenie", NamedTextColor.GRAY))));
    ItemStack compass = item("minecraft:compass", "Minimapa: "
        + (minimapEnabled(player) ? "włączona" : "wyłączona"), List.of());
    compass.setType(Material.COMPASS);
    items.set(30, compass);
    items.set(34, item("mapa:ui/pelny", "Pełny ekran", List.of()));
    items.set(35, item("nexo:hub_zamknij", "Zamknij", List.of()));
    return items;
  }

  private boolean minimapEnabled(Player player) {
    boolean remembered = minimapStates.getOrDefault(player.getUniqueId(), true);
    if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return remembered;
    try {
      Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
      Object value = api.getMethod("setPlaceholders", Player.class, String.class)
          .invoke(null, player, "%nminimap_enabled%");
      if ("true".equalsIgnoreCase(String.valueOf(value))) return true;
      if ("false".equalsIgnoreCase(String.valueOf(value))) return false;
    } catch (ReflectiveOperationException | LinkageError ex) {
      return remembered;
    }
    return remembered;
  }

  private ItemStack tabItem(AtlasState state, int slot) {
    if (slot == 8) return item("mapa:ui/pelny", "Pełny ekran", List.of(
        Component.text("Klik — otwórz pełną mapę", NamedTextColor.GRAY)));
    MapaData.Tab tab = MapaData.tab(data.tabs(), slot);
    if (tab == null) {
      ItemStack empty = item(EMPTY_MODEL, " ", List.of());
      empty.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true));
      return empty;
    }
    boolean active = sameWorld(state.world, tab.world());
    ItemStack item = item(tab.icon(), tab.name(), List.of(Component.text(
        active ? "Oglądasz tę mapę" : "Klik — pokaż tę mapę", NamedTextColor.GRAY)));
    if (active) item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
    return item;
  }

  private ItemStack tileItem(AtlasState state, int slot, boolean playerTile, int direction) {
    MapaData.Grid grid = data.grid(state.world);
    MapaData.Level level = grid.level(state.level);
    int tx = MapaMath.tileX(slot, state.left), tz = MapaMath.tileZ(slot, state.top);
    if (tx < 0 || tz < 0 || tx >= level.columns() || tz >= level.rows())
      return item(EMPTY_MODEL, "Poza mapą", List.of());
    int bpp = level.bpp();
    int x = grid.x0() + tx * 18 * bpp;
    int z = grid.z0() + tz * 18 * bpp;
    List<MapaData.Marker> all = new ArrayList<>(data.markers(state.world));
    all.addAll(own.forPlayer(state.playerId, state.world));
    List<MapaData.Marker> onTile = all.stream().filter(marker ->
        marker.x() >= x && marker.x() < x + 18 * bpp
            && marker.z() >= z && marker.z() < z + 18 * bpp).toList();
    String name = playerTile ? "Tu jesteś" : onTile.isEmpty()
        ? "Okolica"
        : String.join(", ", onTile.stream().map(MapaData.Marker::name).toList());
    List<Component> lore = new ArrayList<>();
    for (MapaData.Marker marker : onTile)
      if (marker.type().equals("wlasny"))
        lore.add(Component.text("Twój znacznik: " + marker.name(), NamedTextColor.GRAY));
      else for (String line : marker.description()) lore.add(Component.text(line, NamedTextColor.GRAY));
    lore.add(Component.text("x " + (x + 9 * bpp) + ", z " + (z + 9 * bpp), NamedTextColor.GRAY));
    lore.add(Component.text("Klik — wyśrodkuj tutaj", NamedTextColor.GRAY));
    ItemStack item = item(new NamespacedKey("mapa", state.world + "/b" + bpp + "/" + tx + "_" + tz), name, lore);
    if (playerTile) item.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
        CustomModelData.customModelData().addString("g" + direction));
    return item;
  }

  private ItemStack item(String model, String name, List<Component> lore) {
    NamespacedKey key = NamespacedKey.fromString(model);
    return item(key == null ? EMPTY_MODEL : key, name, lore);
  }

  private ItemStack item(NamespacedKey model, String name, List<Component> lore) {
    ItemStack item = new ItemStack(Material.PAPER);
    item.setData(DataComponentTypes.ITEM_MODEL, model);
    item.setData(DataComponentTypes.ITEM_NAME, Component.text(name, NamedTextColor.WHITE));
    // lore bez kursywy (klient domyslnie pochyla lore)
    item.lore(lore.stream().map(l -> l.decoration(TextDecoration.ITALIC, false)).toList());
    item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
        .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS, DataComponentTypes.UNBREAKABLE));
    return item;
  }

  private void refreshPlayerTiles() {
    for (Map.Entry<UUID, AtlasState> entry : List.copyOf(atlases.entrySet())) {
      Player player = Bukkit.getPlayer(entry.getKey());
      if (player != null && player.getOpenInventory().getTopInventory() == entry.getValue().holder.inventory) {
        updatePlayerTile(player, entry.getValue());
        AtlasState state = entry.getValue();
        if (panel != null && sameWorld(player.getWorld().getName(), state.world)
            && (state.panelX != player.getLocation().getBlockX()
                || state.panelZ != player.getLocation().getBlockZ())) refreshPanel(player);
      }
    }
  }

  private void updatePlayerTile(Player player, AtlasState state) {
    MapaData.Grid grid = data.grid(state.world);
    int slot = -1;
    int direction = MapaMath.modelDirection(player.getLocation().getYaw());
    if (grid != null && sameWorld(player.getWorld().getName(), state.world)) {
      MapaData.Level level = grid.level(state.level);
      int tx = Math.floorDiv(player.getLocation().getBlockX() - grid.x0(), 18 * level.bpp());
      int tz = Math.floorDiv(player.getLocation().getBlockZ() - grid.z0(), 18 * level.bpp());
      int col = tx - state.left, row = tz - state.top;
      if (tx >= 0 && tz >= 0 && tx < level.columns() && tz < level.rows()
          && col >= 0 && col < 9 && row >= 0 && row < 4) slot = (row + 1) * 9 + col;
    }
    if (slot == state.playerSlot && direction == state.playerDirection) return;
    if (state.playerSlot >= 0) state.holder.inventory.setItem(state.playerSlot,
        tileItem(state, state.playerSlot, false, 0));
    if (slot >= 0) state.holder.inventory.setItem(slot, tileItem(state, slot, true, direction));
    state.playerSlot = slot;
    state.playerDirection = direction;
  }
}
