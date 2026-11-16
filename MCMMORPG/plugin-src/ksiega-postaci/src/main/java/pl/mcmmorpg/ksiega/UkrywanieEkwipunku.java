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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
  private final Map<UUID, Deque<String>> history = new HashMap<>();
  private final Map<UUID, String> currentPage = new HashMap<>();
  private final Set<UUID> returning = ConcurrentHashMap.newKeySet();
  private final ConcurrentMap<UUID, BookView> views = new ConcurrentHashMap<>();
  private final PacketAdapter packets;
  private final PanelDolny panel;
  private final BukkitTask refreshTask;

  UkrywanieEkwipunku(KsiegaPostaci plugin) {
    this.plugin = plugin;
    // Identyfikator strony (np. dziennik_przygod, profession-mining) po glifie tytulu:
    // historia podstron (przycisk Wroc) i wylaczenie stacji MMOItems.
    ConfigurationSection ids = plugin.getConfig().getConfigurationSection("podstrony.identyfikatory");
    Map<String, String> configuredIds = new HashMap<>();
    if (ids != null) for (String sign : ids.getKeys(false)) {
      String id = ids.getString(sign, "");
      if (sign != null && !sign.isEmpty() && id != null && !id.isBlank()) configuredIds.put(sign, id.trim());
    }
    pageIds = Map.copyOf(configuredIds);
    if (plugin.getConfig().getBoolean("ukryj-ekwipunek.wlaczone", true)) {
      panel = new PanelDolny(plugin);
      Set<String> signs = new LinkedHashSet<>(plugin.getConfig().getStringList("ukryj-ekwipunek.znaki-tytulu"));
      signs.addAll(panel.pageSigns());
      signs.addAll(pageIds.keySet());
      titleSigns = signs.stream().filter(sign -> sign != null && !sign.isEmpty()).toList();
      packets = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
          PacketType.Play.Server.OPEN_WINDOW,
          PacketType.Play.Server.WINDOW_ITEMS,
          PacketType.Play.Server.SET_SLOT) {
        @Override public void onPacketSending(PacketEvent event) { mask(event); }
      };
      ProtocolLibrary.getProtocolManager().addPacketListener(packets);
      refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 20L, 20L);
    } else {
      titleSigns = List.of();
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
    PanelDolny.Page page = event.getView().getTopInventory().getSize() == 54
        ? panel.match(plain, legacy) : null;
    boolean portalEnabled = page == null && event.getView().getTopInventory().getSize() == 54
        && (plain.contains(Portal.titleSign()) || legacy.contains(Portal.titleSign()));
    String pageId = pageId(plain, legacy);
    // Stacje MMOItems pobieraja skladniki z prawdziwego ekwipunku gracza.
    // Ich dolnych slotow nie wolno zastapic panelem Ksiegi ani blokowac klikniec.
    if (pageId.startsWith("profession-")) {
      views.remove(uuid);
      history.remove(uuid);
      currentPage.remove(uuid);
      returning.remove(uuid);
      return;
    }
    boolean mainPanel = page != null && page.backMenu() == null;
    boolean subpage = page != null && page.backMenu() != null;
    if (mainPanel) history.remove(uuid);
    else if (!returning.remove(uuid) && !pageId.isEmpty()) {
      String previous = currentPage.get(uuid);
      if (previous != null && !previous.equals(pageId) && RETURNABLE.contains(previous))
        history.computeIfAbsent(uuid, unused -> new ArrayDeque<>()).push(previous);
    }
    if (!pageId.isEmpty()) currentPage.put(uuid, pageId);
    Deque<String> stack = history.get(uuid);
    String previous = stack == null ? null : stack.peek();
    String target = !subpage ? null : previous != null ? previous : page.backMenu();
    ItemStack[] items = page != null ? panel.render(player, page)
        : portalEnabled ? plugin.portal.panelItems(player) : new ItemStack[36];
    if (subpage) relabelBack(items, target);
    views.put(uuid, new BookView(event.getView(), page, portalEnabled, target, items));
  }

  private void refresh() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      BookView state = views.get(player.getUniqueId());
      if (state == null || (state.page == null && !state.portalEnabled)
          || state.view.getTopInventory() != player.getOpenInventory().getTopInventory()) continue;
      ItemStack[] next = state.portalEnabled ? plugin.portal.panelItems(player) : panel.render(player, state.page);
      if (state.backTarget != null) relabelBack(next, state.backTarget);
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
    if (!bottom || state == null || (state.page == null && !state.portalEnabled)
        || !Przedmioty.empty(event.getCursor())
        || click != ClickType.LEFT && click != ClickType.RIGHT) return;
    int panelSlot = raw - first;
    boolean portalAction = state.portalEnabled && (panelSlot == 1 || panelSlot == 4);
    boolean backAction = !state.portalEnabled && state.backTarget != null
        && panelSlot >= 3 && panelSlot <= 5 && !Przedmioty.empty(state.panelItems[panelSlot]);
    boolean panelAction = !state.portalEnabled && !backAction
        && panel.hasAction(panelSlot, state.panelItems[panelSlot], state.page);
    if (!portalAction && !backAction && !panelAction) return;
    Player player = (Player) event.getWhoClicked();
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (player.isOnline() && views.get(player.getUniqueId()) == state
          && player.getOpenInventory().getTopInventory() == state.view.getTopInventory()
          && (portalAction || backAction || panelAction)) {
        if (portalAction) plugin.portal.panelClick(player, panelSlot);
        else if (backAction) back(player, state.backTarget);
        else panel.run(player, panelSlot, state.page);
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
      // 3 ticki: DeluxeMenus otwiera nastepna strone z opoznieniem; historia ginie dopiero,
      // gdy po zamknieciu nie otworzyla sie zadna strona Ksiegi.
      if (!player.isOnline() || views.containsKey(player.getUniqueId())) return;
      history.remove(player.getUniqueId());
      currentPage.remove(player.getUniqueId());
      returning.remove(player.getUniqueId());
      player.updateInventory();
      plugin.refreshLater(player);
    }, 3L);
  }

  /** Glif tytulu wystepujacy najwczesniej decyduje o identyfikatorze strony. */
  private String pageId(String plain, String legacy) {
    String id = earliestId(plain);
    return id != null ? id : java.util.Objects.requireNonNullElse(earliestId(legacy), "");
  }

  private String earliestId(String title) {
    int earliest = Integer.MAX_VALUE;
    String found = null;
    for (Map.Entry<String, String> candidate : pageIds.entrySet()) {
      int at = title.indexOf(candidate.getKey());
      if (at >= 0 && at < earliest) {
        earliest = at;
        found = candidate.getValue();
      }
    }
    return found;
  }

  /**
   * Przycisk Wroc (sloty 3-5) rysuje PanelDolny w stylu MENU; tu tylko podpis mowi,
   * dokad naprawde wroci gracz, gdy historia prowadzi gdzie indziej niz menu glowne.
   */
  private static void relabelBack(ItemStack[] items, String target) {
    String label = switch (target) {
      case "dziennik_przygod" -> "Dziennika";
      case "chowance_menazeria" -> "Menażerii";
      case "hub_postac" -> "menu Postaci";
      case "hub_umiejetnosci" -> "menu Umiejętności";
      case "hub_ekonomia" -> "menu Handlu";
      case "hub_spolecznosc" -> "menu Społeczności";
      case "profession_main" -> "menu Profesji";
      case "portal" -> "Portalu";
      default -> null; // hub_glowne i reszta: podpis z konfiguracji (podstrony.wstecz-nazwa)
    };
    if (label == null) return;
    for (int slot = 3; slot <= 5; slot++) {
      ItemStack item = items[slot];
      if (Przedmioty.empty(item)) continue;
      ItemStack copy = item.clone();
      ItemMeta meta = copy.getItemMeta();
      meta.displayName(Component.text("Wróć do " + label, NamedTextColor.YELLOW)
          .decoration(TextDecoration.ITALIC, false));
      copy.setItemMeta(meta);
      items[slot] = copy;
    }
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
    final PanelDolny.Page page;
    final boolean portalEnabled;
    /** Menu otwierane przyciskiem Wroc; null poza podstronami. */
    final String backTarget;
    volatile ItemStack[] panelItems;
    volatile int windowId = -1;

    BookView(InventoryView view, PanelDolny.Page page, boolean portalEnabled,
        String backTarget, ItemStack[] panelItems) {
      this.view = view;
      topSize = view.getTopInventory().getSize();
      this.page = page;
      this.portalEnabled = portalEnabled;
      this.backTarget = backTarget;
      this.panelItems = panelItems;
    }
  }
}
