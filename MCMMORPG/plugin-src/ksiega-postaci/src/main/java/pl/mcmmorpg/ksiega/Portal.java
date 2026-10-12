package pl.mcmmorpg.ksiega;

import com.nexomc.nexo.api.NexoItems;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.waypoint.Waypoint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/** Nine MMOCore waypoint IDs; our interface owns discovery and free travel. */
final class Portal implements Listener, CommandExecutor {
  private static final String TITLE = "\uE104䅬\uE101䅭\uE108\uE106\uE105\uE101䅮\uE101䅯\uE108\uE106\uE104\uE101";
  private static final List<Destination> DESTINATIONS = List.of(
      new Destination("start", "Start", "Kraina", "ognisko przy starcie", Material.CAMPFIRE, 22),
      new Destination("szepczacy_gaj", "Szepczący Gaj", "Kraina", "daleki północny zachód", Material.OAK_SAPLING, 1),
      new Destination("wilcze_ostepy", "Wilcze Ostępy", "Kraina", "zachód, na północ od Gąszczu", Material.WOLF_SPAWN_EGG, 19),
      new Destination("swietlisty_gaszcz", "Świetlisty Gąszcz", "Kraina", "zachód od Startu", Material.GLOW_BERRIES, 37),
      new Destination("wietrzne_turnie", "Wietrzne Turnie", "Kraina", "północ od Startu", Material.FEATHER, 5),
      new Destination("perlowa_zatoka", "Perłowa Zatoka", "Kraina", "południowy wschód od Startu", Material.NAUTILUS_SHELL, 41),
      new Destination("rdzawe_wawozy", "Rdzawe Wąwozy", "Kraina", "wschód od Zatoki", Material.RAW_COPPER, 25),
      new Destination("wygasle_palenisko", "Wygasłe Palenisko", "Kraina", "południe od Wąwozów", Material.FLINT_AND_STEEL, 43),
      new Destination("nexus", "Nexus", "Stolica", "brama w Stolicy", Material.ENDER_EYE, -1));
  private final KsiegaPostaci plugin;
  private final Map<UUID, Pending> pending = new HashMap<>();
  private BukkitTask discoveryTask;

  Portal(KsiegaPostaci plugin) {
    this.plugin = plugin;
    discoveryTask = Bukkit.getScheduler().runTaskTimer(plugin, this::discover, 20L, 20L);
  }

  static String titleSign() { return "䅬"; }

  int odkryte(Player player) {
    if (!PlayerData.has(player)) return 0;
    PlayerData data = PlayerData.get(player);
    return (int) DESTINATIONS.stream().filter(dest -> unlocked(data, dest)).count();
  }

  @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)
        || args.length != 1) return true;
    Player player = Bukkit.getPlayerExact(args[0]);
    if (player != null) open(player);
    return true;
  }

  void open(Player player) {
    if (!PlayerData.has(player)) {
      player.sendMessage(line("Dane postaci jeszcze się wczytują.", NamedTextColor.RED));
      return;
    }
    Holder holder = new Holder();
    PlayerData data = PlayerData.get(player);
    int count = (int) DESTINATIONS.stream().filter(d -> unlocked(data, d)).count();
    Inventory inventory = Bukkit.createInventory(holder, 54,
        Component.text(TITLE.replace("䅯", String.valueOf((char) ('䅰' + count))), NamedTextColor.WHITE));
    holder.inventory = inventory;
    for (Destination dest : DESTINATIONS) {
      if (dest.slot() >= 0) inventory.setItem(dest.slot(), icon(data, dest));
    }
    player.openInventory(inventory);
    Destination nearest = nearestUndiscovered(player, data);
    if (nearest != null) {
      Location at = find(nearest.id()).getLocation();
      player.sendMessage(line("Najbliższy nieodkryty punkt: " + nearest.name() + " — "
          + direction(player.getLocation(), at) + " (x " + at.getBlockX()
          + ", z " + at.getBlockZ() + ").", NamedTextColor.YELLOW));
    }
  }

  ItemStack[] panelItems(Player player) {
    ItemStack[] items = new ItemStack[36];
    ItemStack back = blankButton();
    ItemMeta backMeta = back.getItemMeta();
    backMeta.displayName(line("Powrót do MENU", NamedTextColor.GOLD));
    backMeta.lore(List.of(line("Kliknij, aby wrócić.", NamedTextColor.GRAY)));
    back.setItemMeta(backMeta);
    items[1] = back;
    if (!PlayerData.has(player)) return items;
    PlayerData data = PlayerData.get(player);
    long count = DESTINATIONS.stream().filter(d -> unlocked(data, d)).count();
    ItemStack counter = blankButton();
    ItemMeta countMeta = counter.getItemMeta();
    countMeta.displayName(line("Odkryte " + count + "/9", NamedTextColor.GOLD));
    Destination nearest = nearestUndiscovered(player, data);
    countMeta.lore(nearest == null ? List.of(
        line("Wszystkie punkty w tej krainie są odkryte.", NamedTextColor.GRAY))
        : List.of(
            line("Najbliższy nowy punkt: " + nearest.name(), NamedTextColor.YELLOW),
            line(direction(player.getLocation(), find(nearest.id()).getLocation())
                + " · x " + find(nearest.id()).getLocation().getBlockX()
                + ", z " + find(nearest.id()).getLocation().getBlockZ(), NamedTextColor.GRAY),
            line("Podejdź na 8 bloków, aby go odkryć.", NamedTextColor.GRAY)));
    counter.setItemMeta(countMeta);
    items[7] = counter;
    items[4] = icon(data, DESTINATIONS.get(8));
    return items;
  }

  ItemStack[] quickItems(Player player) {
    ItemStack[] items = new ItemStack[9];
    if (!PlayerData.has(player)) return items;
    PlayerData data = PlayerData.get(player);
    for (int i = 0; i < DESTINATIONS.size(); i++) {
      Destination dest = DESTINATIONS.get(i);
      items[i] = icon(data, dest);
    }
    return items;
  }

  boolean quickAction(int slot, ItemStack visible) {
    return slot >= 18 && slot <= 26 && visible != null && !visible.getType().isAir();
  }

  void quickClick(Player player, int slot) {
    int index = slot - 18;
    if (index < 0 || index >= DESTINATIONS.size()) return;
    travel(player, DESTINATIONS.get(index));
  }

  void panelClick(Player player, int slot) {
    if (slot == 1) {
      Bukkit.getScheduler().runTask(plugin, () ->
          Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open hub_glowne " + player.getName()));
    } else if (slot == 4) {
      travel(player, DESTINATIONS.get(8));
    }
  }

  private ItemStack icon(PlayerData data, Destination dest) {
    Waypoint waypoint = find(dest.id());
    boolean unlocked = waypoint != null && data.hasWaypoint(waypoint);
    ItemStack item = waypointItem(dest, unlocked);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(line(dest.name(), unlocked ? NamedTextColor.GOLD : NamedTextColor.GRAY));
    Location at = waypoint == null ? null : waypoint.getLocation();
    meta.lore(unlocked ? List.of(
        line("Kraina: " + dest.realm(), NamedTextColor.GRAY),
        line("Odkryty punkt podróży", NamedTextColor.GREEN),
        line("Kliknij, aby podróżować (3 s, nie ruszaj się)", NamedTextColor.YELLOW))
        : at == null ? List.of(
            line("Nieodkryty punkt podróży", NamedTextColor.GRAY),
            line("Odkryjesz go podczas wyprawy.", NamedTextColor.YELLOW))
        : List.of(
            line("Nieodkryty punkt podróży", NamedTextColor.GRAY),
            line("Kraina: " + dest.realm(), NamedTextColor.GRAY),
            line("Warunek: podejdź na 8 bloków do punktu.", NamedTextColor.YELLOW),
            line("Położenie: x " + at.getBlockX() + ", z " + at.getBlockZ(), NamedTextColor.GRAY),
            line("Wskazówka: " + dest.hint(), NamedTextColor.GRAY)));
    meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
    item.setItemMeta(meta);
    return item;
  }

  private static ItemStack waypointItem(Destination dest, boolean unlocked) {
    if (Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
      try {
        var builder = NexoItems.itemFromId("portal_wp_" + dest.id()
            + (unlocked ? "" : "_locked"));
        if (builder != null) return builder.build();
      } catch (RuntimeException | LinkageError ignored) {
        // Vanilla material remains usable while a test pack is being rebuilt.
      }
    }
    return new ItemStack(unlocked ? dest.icon() : Material.GRAY_DYE);
  }

  private static ItemStack blankButton() {
    if (Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
      try {
        var builder = NexoItems.itemFromId("mmo_icon_blank");
        if (builder != null) return builder.build();
      } catch (RuntimeException | LinkageError ignored) {
        // The fallback below keeps the click target available without Nexo.
      }
    }
    ItemStack item = new ItemStack(Material.GOLD_NUGGET);
    ItemMeta meta = item.getItemMeta();
    meta.setItemModel(NamespacedKey.fromString("nexo:mmo_icon_blank"));
    item.setItemMeta(meta);
    return item;
  }

  private void discover() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!PlayerData.has(player)) continue;
      PlayerData data = PlayerData.get(player);
      Location here = player.getLocation();
      for (Destination dest : DESTINATIONS) {
        if (dest.id().equals("start") || dest.id().equals("nexus")) continue;
        Waypoint waypoint = find(dest.id());
        if (waypoint == null || data.hasWaypoint(waypoint)) continue;
        Location at = waypoint.getLocation();
        if (at == null || at.getWorld() != here.getWorld()) continue;
        double dx = here.getX() - at.getX(), dz = here.getZ() - at.getZ();
        if (dx * dx + dz * dz > 64) continue;
        double dy = here.getY()
            - (at.getWorld().getHighestBlockYAt(at, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1);
        if (dx * dx + dy * dy + dz * dz > 64) continue;
        // MMOCore owns persistence. Its unlock message is blank in messages.yml.
        data.unlockWaypoint(waypoint);
        player.showTitle(net.kyori.adventure.title.Title.title(
            line("Odkryto punkt podróży", NamedTextColor.GOLD),
            line(dest.name(), NamedTextColor.YELLOW),
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ZERO, java.time.Duration.ofSeconds(3),
                java.time.Duration.ofMillis(400))));
        player.playSound(here, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
      }
    }
  }

  private static Component line(String value, NamedTextColor color) {
    return Component.text(value, color).decoration(TextDecoration.ITALIC, false);
  }

  private static Waypoint find(String id) {
    return MMOCore.plugin.waypointManager.getAll().stream()
        .filter(waypoint -> waypoint.getId().equals(id)).findFirst().orElse(null);
  }

  private static boolean unlocked(PlayerData data, Destination dest) {
    Waypoint waypoint = find(dest.id());
    return waypoint != null && data.hasWaypoint(waypoint);
  }

  private static Destination nearestUndiscovered(Player player, PlayerData data) {
    Location here = player.getLocation();
    Destination nearest = null;
    double distance = Double.POSITIVE_INFINITY;
    for (Destination dest : DESTINATIONS) {
      if (dest.id().equals("start") || dest.id().equals("nexus")) continue;
      if (unlocked(data, dest)) continue;
      Waypoint waypoint = find(dest.id());
      Location at = waypoint == null ? null : waypoint.getLocation();
      if (at == null || at.getWorld() != here.getWorld()) continue;
      double dx = at.getX() - here.getX(), dz = at.getZ() - here.getZ();
      double candidate = dx * dx + dz * dz;
      if (candidate < distance) { distance = candidate; nearest = dest; }
    }
    return nearest;
  }

  private static String direction(Location from, Location to) {
    double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
    String northSouth = Math.abs(dz) < 16 ? "" : dz < 0 ? "północ" : "południe";
    String eastWest = Math.abs(dx) < 16 ? "" : dx < 0 ? "zachód" : "wschód";
    return northSouth.isEmpty() ? eastWest.isEmpty() ? "w pobliżu" : eastWest
        : eastWest.isEmpty() ? northSouth : northSouth + " i " + eastWest;
  }

  private void travel(Player player, Destination dest) {
    if (!PlayerData.has(player)) {
      player.sendMessage(line("Dane postaci jeszcze się wczytują.", NamedTextColor.RED));
      return;
    }
    PlayerData data = PlayerData.get(player);
    Waypoint waypoint = find(dest.id());
    if (waypoint == null) {
      showBar(player, 40, 40, "§cPunkt podróży nie jest wczytany.");
    } else if (!data.hasWaypoint(waypoint)) {
      Location at = waypoint.getLocation();
      showBar(player, 40, 40, at == null ? "§cPunkt podróży jest niedostępny."
          : "§eOdkryj punkt: x " + at.getBlockX() + ", z " + at.getBlockZ() + ".");
    } else if (data.isInCombat()) {
      showBar(player, 40, 40, "§cNie możesz podróżować podczas walki.");
    } else if (pending.containsKey(player.getUniqueId())) {
      showBar(player, 30, 30, "§ePodróż już trwa.");
    } else {
      Location target = waypoint.getLocation();
      if (target == null || target.getWorld() == null) {
        showBar(player, 40, 40, "§cCel podróży jest niedostępny.");
        return;
      }
      player.closeInventory();
      player.sendMessage(line("Podróż za 3 sekundy. Ruch przerwie podróż.", NamedTextColor.YELLOW));
      Pending state = new Pending(player.getLocation().clone(), target.clone());
      pending.put(player.getUniqueId(), state);
      state.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(player, state), 0L, 20L);
    }
  }

  private void tick(Player player, Pending state) {
    if (!player.isOnline() || pending.get(player.getUniqueId()) != state) {
      state.task.cancel();
      return;
    }
    if (state.seconds == 0) {
      pending.remove(player.getUniqueId());
      state.task.cancel();
      Location target = state.target;
      // Resolve the destination surface from the live world; config Y is a fallback.
      int y = target.getWorld().getHighestBlockYAt(target, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
      Location landing = target.clone();
      landing.setY(y);
      player.teleport(landing);
      showTravelTitle(player, "Podróż zakończona", "Dotarłeś do punktu podróży.", NamedTextColor.GREEN);
      player.playSound(landing, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
    } else {
      showBar(player, 20, 22, "§ePodróż: " + state.seconds
          + " s  §7•  Ruch przerwie podróż");
      state.seconds--;
    }
  }

  @EventHandler public void move(PlayerMoveEvent event) {
    Pending state = pending.get(event.getPlayer().getUniqueId());
    if (state == null || event.getTo() == null) return;
    Location from = state.origin, to = event.getTo();
    if (from.getWorld() != to.getWorld()
        || Math.abs(from.getX() - to.getX()) > 0.01
        || Math.abs(from.getY() - to.getY()) > 0.01
        || Math.abs(from.getZ() - to.getZ()) > 0.01) {
      pending.remove(event.getPlayer().getUniqueId());
      state.task.cancel();
      showBar(event.getPlayer(), 40, 45, "§cPodróż przerwana — poruszyłeś się.");
    }
  }

  @EventHandler public void click(InventoryClickEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)) return;
    Destination dest = DESTINATIONS.stream()
        .filter(d -> d.slot() == event.getRawSlot()).findFirst().orElse(null);
    if (dest != null) Bukkit.getScheduler().runTask(plugin, () -> {
      if (player.isOnline()) travel(player, dest);
    });
  }

  private static void showTravelTitle(Player player, String heading, String detail, NamedTextColor color) {
    player.showTitle(net.kyori.adventure.title.Title.title(
        line(heading, color), line(detail, NamedTextColor.WHITE),
        net.kyori.adventure.title.Title.Times.times(
            java.time.Duration.ZERO, java.time.Duration.ofMillis(1100),
            java.time.Duration.ofMillis(150))));
  }

  private static void showBar(Player player, int priority, long ticks, String message) {
    MMOPlayerData.get(player).getActionBar().show(priority, ticks, message);
  }

  @EventHandler public void drag(InventoryDragEvent event) {
    if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
  }

  @EventHandler public void quit(PlayerQuitEvent event) {
    Pending state = pending.remove(event.getPlayer().getUniqueId());
    if (state != null) state.task.cancel();
  }

  void stop() {
    if (discoveryTask != null) discoveryTask.cancel();
    for (Pending state : pending.values()) state.task.cancel();
    pending.clear();
  }

  private record Destination(String id, String name, String realm, String hint, Material icon, int slot) {}
  private static final class Pending {
    final Location origin, target;
    BukkitTask task;
    int seconds = 3;
    Pending(Location origin, Location target) { this.origin = origin; this.target = target; }
  }
  private static final class Holder implements InventoryHolder {
    private Inventory inventory;
    @Override public @NotNull Inventory getInventory() { return inventory; }
  }
}
