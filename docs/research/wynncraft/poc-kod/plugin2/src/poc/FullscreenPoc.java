package poc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * PoC okna "pelnoekranowego" jak u Wynncrafta: skrzynia 6x9, a w dolnej czesci okna
 * (raw sloty 54..89 = ekwipunek gracza) FALSZYWE przedmioty wstrzykniete w pakiety.
 * Prawdziwy ekwipunek gracza NIE jest ruszany po stronie serwera.
 *
 * Pakiety: Set Container Content (WINDOW_ITEMS) i Set Container Slot (SET_SLOT) dla
 * naszego window id -> podmiana slotow >= 54. Klik w dolna czesc -> anulowany w
 * InventoryClickEvent (serwer mysli, ze klik byl w prawdziwy ekwipunek) + resync.
 * Po zamknieciu: player.updateInventory() odsyla prawdziwy stan.
 */
public final class FullscreenPoc extends JavaPlugin implements Listener {

  static final class Holder implements InventoryHolder {
    Inventory inv;
    @Override public Inventory getInventory() { return inv; }
  }

  /** uuid gracza -> window id naszego okna (z pakietu OPEN_WINDOW). */
  private final Map<UUID, Integer> openWindow = new HashMap<>();
  private final Set<UUID> pending = new HashSet<>();

  @Override public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
    PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
      @Override public void onPacketSend(PacketSendEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        if (e.getPacketType() == PacketType.Play.Server.OPEN_WINDOW && pending.remove(id)) {
          openWindow.put(id, new WrapperPlayServerOpenWindow(e).getContainerId());
        } else if (e.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
          var w = new WrapperPlayServerWindowItems(e);
          Integer win = openWindow.get(id);
          if (win == null || w.getWindowId() != win) return;
          var items = new ArrayList<>(w.getItems());
          for (int raw = 54; raw < items.size(); raw++) items.set(raw, fake(raw));
          w.setItems(items);
          e.markForReEncode(true);
        } else if (e.getPacketType() == PacketType.Play.Server.SET_SLOT) {
          var w = new WrapperPlayServerSetSlot(e);
          Integer win = openWindow.get(id);
          if (win == null || w.getWindowId() != win || w.getSlot() < 54) return;
          w.setItem(fake(w.getSlot()));
          e.markForReEncode(true);
        }
      }
      @Override public void onPacketReceive(PacketReceiveEvent e) {
        if (e.getPacketType() != PacketType.Play.Client.CLICK_WINDOW || !(e.getPlayer() instanceof Player p)) return;
        var w = new WrapperPlayClientClickWindow(e);
        getLogger().info("[POC] click window=" + w.getWindowId() + " slot=" + w.getSlot() + " button=" + w.getButton() + " type=" + w.getWindowClickType());
      }
    });
  }

  /** Falszywy przycisk: szklo z nazwa slotu (w prawdziwym wdrozeniu: item_model Nexo). */
  private com.github.retrooper.packetevents.protocol.item.ItemStack fake(int raw) {
    ItemStack it = new ItemStack(raw >= 81 ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
    var meta = it.getItemMeta();
    meta.setDisplayName("Przycisk raw " + raw);
    it.setItemMeta(meta);
    return SpigotConversionUtil.fromBukkitItemStack(it);
  }

  @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
    Player p = s instanceof Player pl ? pl : Bukkit.getOnlinePlayers().iterator().next();
    // prawdziwe przedmioty, zeby bylo widac, ze znikaja i wracaja
    p.getInventory().setItem(9, new ItemStack(Material.DIAMOND, 3));
    p.getInventory().setItem(0, new ItemStack(Material.STONE, 64));
    Holder h = new Holder();
    h.inv = Bukkit.createInventory(h, 54, "Fullscreen PoC");
    h.inv.setItem(22, new ItemStack(Material.NETHER_STAR));
    pending.add(p.getUniqueId());
    p.openInventory(h.inv);
    return true;
  }

  @EventHandler public void click(InventoryClickEvent e) {
    if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
    e.setCancelled(true);
    Player p = (Player) e.getWhoClicked();
    getLogger().info("[POC] InventoryClickEvent raw=" + e.getRawSlot() + " click=" + e.getClick() + " bukkitItem=" + e.getCurrentItem());
    // anulowany klik w dolnej czesci: serwer odsyla prawdziwy slot -> nasz SET_SLOT go podmieni
    Bukkit.getScheduler().runTask(this, p::updateInventory);
  }

  @EventHandler public void close(InventoryCloseEvent e) {
    if (!(e.getInventory().getHolder() instanceof Holder)) return;
    openWindow.remove(e.getPlayer().getUniqueId());
    Bukkit.getScheduler().runTask(this, ((Player) e.getPlayer())::updateInventory);
  }
}
