package pl.mcmmorpg.ksiega;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.scheduler.BukkitTask;

/** Zmienia tylko pakiety wysylane do klienta; nigdy nie modyfikuje ekwipunku gracza. */
final class UkrywanieEkwipunku implements Listener {
  private static final ItemStack EMPTY = new ItemStack(Material.AIR);

  private final KsiegaPostaci plugin;
  private final List<String> titleSigns;
  private final ConcurrentMap<UUID, BookView> views = new ConcurrentHashMap<>();
  private final PacketAdapter packets;
  private final PanelDolny panel;
  private final BukkitTask refreshTask;

  UkrywanieEkwipunku(KsiegaPostaci plugin) {
    this.plugin = plugin;
    if (plugin.getConfig().getBoolean("ukryj-ekwipunek.wlaczone", true)) {
      panel = new PanelDolny(plugin);
      Set<String> signs = new LinkedHashSet<>(plugin.getConfig().getStringList("ukryj-ekwipunek.znaki-tytulu"));
      signs.addAll(panel.pageSigns());
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
    if (titleSigns.stream().noneMatch(sign -> plain.contains(sign) || legacy.contains(sign))) {
      views.remove(player.getUniqueId());
      return;
    }
    PanelDolny.Page page = event.getView().getTopInventory().getSize() == 54
        ? panel.match(plain, legacy) : null;
    boolean portalEnabled = page == null && event.getView().getTopInventory().getSize() == 54
        && (plain.contains(Portal.titleSign()) || legacy.contains(Portal.titleSign()));
    ItemStack[] items = page != null ? panel.render(player, page)
        : portalEnabled ? plugin.portal.panelItems(player) : new ItemStack[36];
    views.put(player.getUniqueId(), new BookView(event.getView(), page, portalEnabled, items));
  }

  private void refresh() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      BookView state = views.get(player.getUniqueId());
      if (state == null || (state.page == null && !state.portalEnabled)
          || state.view.getTopInventory() != player.getOpenInventory().getTopInventory()) continue;
      ItemStack[] next = state.portalEnabled ? plugin.portal.panelItems(player) : panel.render(player, state.page);
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
    if (state.portalEnabled ? panelSlot != 1 && panelSlot != 4
        : !panel.hasAction(panelSlot, state.panelItems[panelSlot], state.page)) return;
    Player player = (Player) event.getWhoClicked();
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (player.isOnline() && views.get(player.getUniqueId()) == state
          && player.getOpenInventory().getTopInventory() == state.view.getTopInventory()
          && (state.portalEnabled || panel.hasAction(panelSlot, state.panelItems[panelSlot], state.page))) {
        if (state.portalEnabled) plugin.portal.panelClick(player, panelSlot);
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
      if (player.isOnline() && !views.containsKey(player.getUniqueId())) {
        player.updateInventory();
        plugin.refreshLater(player);
      }
    }, 1L);
  }

  void quit(Player player) { views.remove(player.getUniqueId()); }

  void stop() {
    if (packets != null) ProtocolLibrary.getProtocolManager().removePacketListener(packets);
    if (refreshTask != null) refreshTask.cancel();
    views.clear();
  }

  private static final class BookView {
    final InventoryView view;
    final int topSize;
    final PanelDolny.Page page;
    final boolean portalEnabled;
    volatile ItemStack[] panelItems;
    volatile int windowId = -1;

    BookView(InventoryView view, PanelDolny.Page page, boolean portalEnabled, ItemStack[] panelItems) {
      this.view = view;
      topSize = view.getTopInventory().getSize();
      this.page = page;
      this.portalEnabled = portalEnabled;
      this.panelItems = panelItems;
    }
  }
}
