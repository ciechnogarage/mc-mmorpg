package pl.mcmmorpg.ksiega;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import java.util.ArrayList;
import java.util.List;
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

/** Zmienia tylko pakiety wysylane do klienta; nigdy nie modyfikuje ekwipunku gracza. */
final class UkrywanieEkwipunku implements Listener {
  private static final ItemStack EMPTY = new ItemStack(Material.AIR);

  private final KsiegaPostaci plugin;
  private final List<String> titleSigns;
  private final ConcurrentMap<UUID, BookView> views = new ConcurrentHashMap<>();
  private final PacketAdapter packets;

  UkrywanieEkwipunku(KsiegaPostaci plugin) {
    this.plugin = plugin;
    titleSigns = plugin.getConfig().getStringList("ukryj-ekwipunek.znaki-tytulu").stream()
        .filter(sign -> sign != null && !sign.isEmpty()).toList();
    if (plugin.getConfig().getBoolean("ukryj-ekwipunek.wlaczone", true)) {
      packets = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
          PacketType.Play.Server.OPEN_WINDOW,
          PacketType.Play.Server.WINDOW_ITEMS,
          PacketType.Play.Server.SET_SLOT) {
        @Override public void onPacketSending(PacketEvent event) { mask(event); }
      };
      ProtocolLibrary.getProtocolManager().addPacketListener(packets);
    } else packets = null;
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
    views.put(player.getUniqueId(), new BookView(event.getView()));
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
      for (int slot = first; slot < end; slot++) hidden.set(slot, EMPTY);
      event.getPacket().getItemListModifier().write(0, hidden);
    } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
      int slot = event.getPacket().getIntegers().read(2);
      if (slot >= first && slot < end) event.getPacket().getItemModifier().write(0, EMPTY);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void click(InventoryClickEvent event) {
    if (!isBookView(event.getView())) return;
    int raw = event.getRawSlot();
    int first = event.getView().getTopInventory().getSize();
    ClickType click = event.getClick();
    if (raw >= first && raw < first + 36
        || click.isShiftClick()
        || click == ClickType.NUMBER_KEY
        || click == ClickType.SWAP_OFFHAND
        || click == ClickType.DROP
        || click == ClickType.CONTROL_DROP
        || click == ClickType.DOUBLE_CLICK
        || event.getAction() == InventoryAction.COLLECT_TO_CURSOR
        || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)
      event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void drag(InventoryDragEvent event) {
    if (!isBookView(event.getView())) return;
    int first = event.getView().getTopInventory().getSize();
    if (event.getRawSlots().stream().anyMatch(slot -> slot >= first && slot < first + 36))
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
      if (player.isOnline()) player.updateInventory();
    }, 1L);
  }

  void quit(Player player) { views.remove(player.getUniqueId()); }

  void stop() {
    if (packets != null) ProtocolLibrary.getProtocolManager().removePacketListener(packets);
    views.clear();
  }

  private static final class BookView {
    final InventoryView view;
    final int topSize;
    volatile int windowId = -1;

    BookView(InventoryView view) {
      this.view = view;
      topSize = view.getTopInventory().getSize();
    }
  }
}
