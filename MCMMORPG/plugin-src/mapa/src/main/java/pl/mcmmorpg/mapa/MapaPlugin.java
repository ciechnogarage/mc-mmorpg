package pl.mcmmorpg.mapa;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

public final class MapaPlugin extends JavaPlugin implements Listener {
  private static final NamespacedKey EMPTY_MODEL = new NamespacedKey("mapa", "pusty");
  private final Map<UUID, Double> handZooms = new HashMap<>();
  private final Map<UUID, AtlasState> atlases = new HashMap<>();
  private MapaData data;
  private HandMapRenderer renderer;
  private MapView mapView;
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
    String world;
    int level;
    int left;
    int top;
    int playerSlot = -1;
    int playerDirection = -1;

    AtlasState(AtlasHolder holder, String world, int level) {
      this.holder = holder;
      this.world = world;
      this.level = level;
    }
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    loadAll();
    Bukkit.getPluginManager().registerEvents(this, this);
    Bukkit.getScheduler().runTaskTimer(this, this::refreshPlayerTiles, 20L, 20L);
  }

  @Override
  public void onDisable() {
    if (mapView != null && renderer != null) mapView.removeRenderer(renderer);
    if (renderer != null) renderer.forgetAll();
    atlases.clear();
    handZooms.clear();
  }

  MapaData data() { return data; }
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
    handZooms.clear();
    if (renderer != null) renderer.forgetAll();
    if (mapView != null && renderer != null) mapView.removeRenderer(renderer);
    mapView = Bukkit.getMap(mapId);
    renderer = null;
    if (mapView == null) {
      getLogger().warning("Mapa o ID " + mapId + " nie istnieje; mapa w ręce jest niedostępna.");
      return;
    }
    for (MapRenderer old : List.copyOf(mapView.getRenderers())) mapView.removeRenderer(old);
    renderer = new HandMapRenderer(this);
    mapView.addRenderer(renderer);
    mapView.setTrackingPosition(false);
    mapView.setUnlimitedTracking(false);
    mapView.setLocked(true);
  }

  private boolean ourMap(ItemStack item) {
    return item != null && item.getType() == Material.FILLED_MAP
        && item.getItemMeta() instanceof MapMeta meta && meta.hasMapId() && meta.getMapId() == mapId;
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    if (mapView == null || event.getHand() != EquipmentSlot.HAND || !ourMap(event.getItem())) return;
    event.setCancelled(true);
    Player player = event.getPlayer();
    if (player.isSneaking()) {
      double current = handZoom(player);
      int index = zooms.indexOf(current);
      double next = zooms.get((index + 1) % zooms.size());
      handZooms.put(player.getUniqueId(), next);
      String label = next < 1 ? (int) (1 / next) + " piksele na blok"
          : "1 piksel = " + (next == Math.rint(next) ? Integer.toString((int) next) : next) + " bl.";
      player.sendActionBar(Component.text("Przybliżenie: " + label));
    } else openAtlas(player);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    UUID id = event.getPlayer().getUniqueId();
    handZooms.remove(id);
    atlases.remove(id);
    if (renderer != null) renderer.forget(id);
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof AtlasHolder holder)) return;
    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)) return;
    AtlasState state = atlases.get(player.getUniqueId());
    if (state == null || state.holder != holder) return;
    int slot = event.getRawSlot();
    if (slot < 0 || slot >= 54) return;
    if (slot < 9) {
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
    if (event.getInventory().getHolder() instanceof AtlasHolder holder)
      atlases.computeIfPresent(event.getPlayer().getUniqueId(), (id, state) -> state.holder == holder ? null : state);
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
    if (args.length == 1 && args[0].equalsIgnoreCase("przeladuj")) {
      for (Player player : Bukkit.getOnlinePlayers())
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof AtlasHolder) player.closeInventory();
      atlases.clear();
      loadAll();
      sender.sendMessage("Mapa przeładowana.");
      return true;
    }
    sender.sendMessage("Użycie: /mapa otworz <gracz> | /mapa przeladuj");
    return true;
  }

  private void openAtlas(Player player) {
    String world = data.grid(player.getWorld().getName()) != null
        ? data.grid(player.getWorld().getName()).world() : "world";
    openAtlas(player, world, true);
  }

  private void openAtlas(Player player, String world, boolean initial) {
    MapaData.Grid grid = data.grid(world);
    if (grid == null) {
      player.sendMessage("Mapa świata jest niedostępna.");
      return;
    }
    world = grid.world();
    int level = Math.min(1, grid.levels().size() - 1);
    AtlasHolder holder = new AtlasHolder();
    Component title = Component.text(getConfig().getString("tytul-okna", "")
        + "Mapa świata · " + worldName(world), NamedTextColor.WHITE);
    holder.inventory = Bukkit.createInventory(holder, 54, title);
    AtlasState state = new AtlasState(holder, world, level);
    if (initial) centerOnPlayerOrStart(player, state);
    else centerOnWorld(player, state);
    paintAll(player, state);
    atlases.put(player.getUniqueId(), state);
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
  }

  private ItemStack tabItem(AtlasState state, int slot) {
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
    List<MapaData.Marker> onTile = data.markers(state.world).stream().filter(marker ->
        marker.x() >= x && marker.x() < x + 18 * bpp
            && marker.z() >= z && marker.z() < z + 18 * bpp).toList();
    String name = playerTile ? "Tu jesteś" : onTile.isEmpty()
        ? "Okolica"
        : String.join(", ", onTile.stream().map(MapaData.Marker::name).toList());
    List<Component> lore = new ArrayList<>();
    for (MapaData.Marker marker : onTile)
      for (String line : marker.description()) lore.add(Component.text(line, NamedTextColor.GRAY));
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
      if (player != null && player.getOpenInventory().getTopInventory() == entry.getValue().holder.inventory)
        updatePlayerTile(player, entry.getValue());
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
