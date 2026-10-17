package pl.mcmmorpg.ksiega;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

/** Zmienia tylko pakiety wysylane do klienta; nigdy nie modyfikuje ekwipunku gracza. */
final class UkrywanieEkwipunku implements Listener {
  private static final ItemStack EMPTY = new ItemStack(Material.AIR);
  private static final Set<String> RETURNABLE = Set.of(
      "hub_glowne", "portal", "hub_postac", "hub_umiejetnosci", "hub_ekonomia",
      "hub_spolecznosc", "hub_ustawienia", "hub_pomoc", "stolica_podroze",
      "wiara", "ksiega_zaklec", "profession_main", "dziennik_przygod", "poczta",
      "chowance_menazeria", "chowance_hodowla", "chowance_lochy", "chowance_ranking",
      "chowance_magazyn", "chowance_bestiariusz", "quest-list", "player-stats",
      "attribute-view", "skill-list");

  private final KsiegaPostaci plugin;
  private final List<String> titleSigns;
  private final Map<String, String> pageIds;
  private final Map<String, String> parents;
  private final Map<UUID, Deque<String>> history = new HashMap<>();
  private final Map<UUID, String> currentPage = new HashMap<>();
  private final Set<UUID> returning = ConcurrentHashMap.newKeySet();
  private final ConcurrentMap<UUID, BookView> views = new ConcurrentHashMap<>();
  private final PacketAdapter packets;
  private final PanelDolny panel;
  private final BukkitTask refreshTask;

  UkrywanieEkwipunku(KsiegaPostaci plugin) {
    this.plugin = plugin;
    ConfigurationSection ids = plugin.getConfig().getConfigurationSection("podstrony.identyfikatory");
    ConfigurationSection configuredParents = plugin.getConfig().getConfigurationSection("podstrony.strony");
    pageIds = new HashMap<>();
    parents = new HashMap<>();
    if (ids != null) for (String sign : ids.getKeys(false)) pageIds.put(sign, ids.getString(sign, ""));
    if (configuredParents != null) for (String sign : configuredParents.getKeys(false))
      parents.put(sign, configuredParents.getString(sign, "hub_glowne"));
    List<String> configuredSigns = plugin.getConfig().getStringList("ukryj-ekwipunek.znaki-tytulu");
    titleSigns = java.util.stream.Stream.concat(pageIds.keySet().stream(), configuredSigns.stream())
        .filter(sign -> sign != null && !sign.isEmpty()).distinct().toList();
    if (plugin.getConfig().getBoolean("ukryj-ekwipunek.wlaczone", true)) {
      panel = new PanelDolny(plugin);
      packets = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
          PacketType.Play.Server.OPEN_WINDOW,
          PacketType.Play.Server.WINDOW_ITEMS,
          PacketType.Play.Server.SET_SLOT) {
        @Override public void onPacketSending(PacketEvent event) { mask(event); }
      };
      ProtocolLibrary.getProtocolManager().addPacketListener(packets);
      refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 20L, 20L);
    } else {
      packets = null;
      panel = null;
      refreshTask = null;
    }
  }

  boolean isBookView(InventoryView view) {
    if (packets == null) return false;
    BookView state = views.get(view.getPlayer().getUniqueId());
    return state != null && state.view == view;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void open(InventoryOpenEvent event) {
    if (packets == null || !(event.getPlayer() instanceof Player player)) return;
    String plain = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
    String legacy = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
    UUID uuid = player.getUniqueId();
    if (titleSigns.stream().noneMatch(sign -> plain.contains(sign) || legacy.contains(sign))) {
      views.remove(uuid);
      history.remove(uuid);
      currentPage.remove(uuid);
      returning.remove(uuid);
      return;
    }
    boolean panelEnabled = event.getView().getTopInventory().getSize() == 54
        && panel.matches(plain, legacy);
    boolean portalEnabled = event.getView().getTopInventory().getSize() == 54
        && (plain.contains(Portal.titleSign()) || legacy.contains(Portal.titleSign()));
    String sign = pageIds.keySet().stream()
        .filter(key -> plain.contains(key) || legacy.contains(key)).findFirst().orElse("");
    String page = pageIds.getOrDefault(sign, "");
    // Stacje MMOItems pobieraja skladniki z prawdziwego ekwipunku gracza.
    // Ich dolnych slotow nie wolno zastapic panelem Ksiegi ani blokowac klikniec.
    if (page.startsWith("profession-")) {
      views.remove(uuid);
      history.remove(uuid);
      currentPage.remove(uuid);
      returning.remove(uuid);
      return;
    }
    boolean subpageEnabled = event.getView().getTopInventory().getSize() == 54
        && !panelEnabled && !portalEnabled && parents.containsKey(sign);
    if (panelEnabled) history.remove(uuid);
    else if (!returning.remove(uuid) && !page.isEmpty()) {
      String previous = currentPage.get(uuid);
      if (previous != null && !previous.equals(page) && RETURNABLE.contains(previous))
        history.computeIfAbsent(uuid, unused -> new ArrayDeque<>()).push(previous);
    }
    if (!page.isEmpty()) currentPage.put(uuid, page);
    String previous = history.getOrDefault(uuid, new ArrayDeque<>()).peek();
    String target = previous != null ? previous : parents.getOrDefault(sign, "hub_glowne");
    ItemStack[] items = portalEnabled ? plugin.portal.panelItems(player)
        : panelEnabled || subpageEnabled ? panel.render(player) : new ItemStack[36];
    if (subpageEnabled) addBack(items, target);
    views.put(uuid, new BookView(event.getView(), panelEnabled, portalEnabled,
        subpageEnabled, target, items));
  }

  private void refresh() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      BookView state = views.get(player.getUniqueId());
      if (state == null || (!state.panelEnabled && !state.portalEnabled && !state.subpageEnabled)
          || state.view.getTopInventory() != player.getOpenInventory().getTopInventory()) continue;
      ItemStack[] next = state.portalEnabled ? plugin.portal.panelItems(player) : panel.render(player);
      if (state.subpageEnabled) addBack(next, state.backTarget);
      if (Arrays.equals(state.panelItems, next)) continue;
      state.panelItems = next;
      player.updateInventory();
    }
  }

  private void mask(PacketEvent event) {
    BookView state = views.get(event.getPlayer().getUniqueId());
    if (state == null) return;
    int windowId = event.getPacket().getIntegers().read(0);
    if (windowId <= 0) return; // 0 to ekwipunek/HUD gracza, nie okno DeluxeMenus.
    if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
      state.windowId = windowId;
      return;
    }
    if (state.windowId == -1 && event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS)
      state.windowId = windowId;
    if (windowId != state.windowId) return;

    int first = state.topSize;
    int end = first + 36;
    if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
      List<ItemStack> original = event.getPacket().getItemListModifier().read(0);
      if (original == null || original.size() < end) return;
      List<ItemStack> hidden = new ArrayList<>(original);
      ItemStack[] items = state.panelItems;
      for (int slot = first; slot < end; slot++)
        hidden.set(slot, items[slot - first] == null ? EMPTY : items[slot - first]);
      event.getPacket().getItemListModifier().write(0, hidden);
    } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
      int slot = event.getPacket().getIntegers().read(2);
      if (slot >= first && slot < end) {
        ItemStack item = state.panelItems[slot - first];
        event.getPacket().getItemModifier().write(0, item == null ? EMPTY : item);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void click(InventoryClickEvent event) {
    if (!isBookView(event.getView())) return;
    int raw = event.getRawSlot();
    int first = event.getView().getTopInventory().getSize();
    ClickType click = event.getClick();
    BookView state = stateFor(event.getView());
    boolean bottom = raw >= first && raw < first + 36;
    boolean blocked = bottom
        || click.isShiftClick()
        || click == ClickType.NUMBER_KEY
        || click == ClickType.SWAP_OFFHAND
        || click == ClickType.DROP
        || click == ClickType.CONTROL_DROP
        || click == ClickType.DOUBLE_CLICK
        || event.getAction() == InventoryAction.COLLECT_TO_CURSOR
        || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
        || !Przedmioty.empty(event.getCursor());
    if (blocked) event.setCancelled(true);
    if (!bottom || state == null || (!state.panelEnabled && !state.portalEnabled && !state.subpageEnabled)
        || !Przedmioty.empty(event.getCursor())
        || click != ClickType.LEFT && click != ClickType.RIGHT) return;
    int panelSlot = raw - first;
    boolean portalAction = state.portalEnabled && (panelSlot == 1 || panelSlot == 4);
    boolean backAction = state.subpageEnabled && panelSlot >= 3 && panelSlot <= 5;
    boolean panelAction = !state.portalEnabled && !backAction
        && panel.hasAction(panelSlot, state.panelItems[panelSlot]);
    if (!portalAction && !backAction && !panelAction) return;
    Player player = (Player) event.getWhoClicked();
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (player.isOnline() && views.get(player.getUniqueId()) == state
          && player.getOpenInventory().getTopInventory() == state.view.getTopInventory()
          && (portalAction || backAction || panelAction)) {
        if (portalAction) plugin.portal.panelClick(player, panelSlot);
        else if (backAction)
          back(player, state.backTarget);
        else panel.run(player, panelSlot);
      }
    });
  }

  private BookView stateFor(InventoryView view) {
    BookView state = views.get(view.getPlayer().getUniqueId());
    return state != null && state.view == view ? state : null;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void drag(InventoryDragEvent event) {
    if (!isBookView(event.getView())) return;
    event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void drop(PlayerDropItemEvent event) {
    if (isBookView(event.getPlayer().getOpenInventory())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void close(InventoryCloseEvent event) {
    Player player = (Player) event.getPlayer();
    BookView state = views.get(player.getUniqueId());
    if (state == null || state.view != event.getView()
        || !views.remove(player.getUniqueId(), state)) return;
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!player.isOnline()) return;
      if (!views.containsKey(player.getUniqueId())) {
        history.remove(player.getUniqueId());
        currentPage.remove(player.getUniqueId());
        returning.remove(player.getUniqueId());
      }
      player.updateInventory();
      plugin.refreshLater(player);
    }, 3L);
  }

  private static void addBack(ItemStack[] items, String target) {
    ItemStack back = new ItemStack(Material.GOLD_NUGGET);
    ItemMeta meta = back.getItemMeta();
    meta.setItemModel(NamespacedKey.fromString("nexo:mmo_icon_blank"));
    String label = switch (target) {
      case "hub_glowne" -> "menu głównego";
      case "dziennik_przygod" -> "Dziennika";
      case "chowance_menazeria" -> "Menażerii";
      case "hub_postac" -> "menu Postaci";
      case "hub_umiejetnosci" -> "menu Umiejętności";
      case "hub_ekonomia" -> "menu Handlu";
      case "hub_spolecznosc" -> "menu Społeczności";
      case "profession_main" -> "menu Profesji";
      default -> "poprzedniej strony";
    };
    meta.displayName(Component.text("Wróć do " + label, NamedTextColor.GOLD));
    meta.lore(List.of(Component.text("Kliknij, aby wrócić.", NamedTextColor.GRAY)));
    back.setItemMeta(meta);
    for (int slot = 3; slot <= 5; slot++) items[slot] = back;
  }

  private void back(Player player, String target) {
    UUID uuid = player.getUniqueId();
    Deque<String> stack = history.get(uuid);
    if (stack != null && !stack.isEmpty()) stack.pop();
    returning.add(uuid);
    Bukkit.getScheduler().runTaskLater(plugin, () -> returning.remove(uuid), 10L);
    switch (target) {
      case "portal" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
          "ksiegaportal " + player.getName());
      case "quest-list" -> player.performCommand("quests");
      case "player-stats" -> player.performCommand("player");
      case "attribute-view" -> player.performCommand("attributes");
      case "skill-list" -> player.performCommand("skills");
      default -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
          "dm open " + target + " " + player.getName());
    }
  }

  void quit(Player player) {
    UUID uuid = player.getUniqueId();
    views.remove(uuid); history.remove(uuid); currentPage.remove(uuid); returning.remove(uuid);
  }

  void stop() {
    if (packets != null) ProtocolLibrary.getProtocolManager().removePacketListener(packets);
    if (refreshTask != null) refreshTask.cancel();
    views.clear();
    history.clear(); currentPage.clear(); returning.clear();
  }

  private static final class BookView {
    final InventoryView view;
    final int topSize;
    final boolean panelEnabled;
    final boolean portalEnabled;
    final boolean subpageEnabled;
    final String backTarget;
    volatile ItemStack[] panelItems;
    volatile int windowId = -1;

    BookView(InventoryView view, boolean panelEnabled, boolean portalEnabled,
        boolean subpageEnabled, String backTarget, ItemStack[] panelItems) {
      this.view = view;
      topSize = view.getTopInventory().getSize();
      this.panelEnabled = panelEnabled;
      this.portalEnabled = portalEnabled;
      this.subpageEnabled = subpageEnabled;
      this.backTarget = backTarget;
      this.panelItems = panelItems;
    }
  }
}
