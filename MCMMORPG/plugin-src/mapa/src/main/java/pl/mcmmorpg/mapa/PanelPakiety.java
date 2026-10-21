package pl.mcmmorpg.mapa;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

// Listener pakietow czyta tylko niezmienny obraz panelu przygotowany na watku serwera.
final class PanelPakiety extends PacketListenerAbstract implements PanelMost {
  private static final class Panel {
    final Component title;
    final List<ItemStack> items;
    volatile int windowId;

    Panel(Component title, List<ItemStack> items, int windowId) {
      this.title = title;
      this.items = items;
      this.windowId = windowId;
    }
  }

  private volatile Map<UUID, Panel> panels = Map.of();

  PanelPakiety() {
    super(PacketListenerPriority.NORMAL);
    PacketEvents.getAPI().getEventManager().registerListener(this);
  }

  @Override public void show(Player player, Component title, List<org.bukkit.inventory.ItemStack> items) {
    put(player.getUniqueId(), new Panel(title, convert(items), -1));
  }

  @Override public void refresh(Player player, Component title, List<org.bukkit.inventory.ItemStack> items) {
    Panel old = panels.get(player.getUniqueId());
    if (old != null) put(player.getUniqueId(), new Panel(title, convert(items), old.windowId));
  }

  @Override public void hide(Player player) {
    Map<UUID, Panel> copy = new HashMap<>(panels);
    copy.remove(player.getUniqueId());
    panels = Map.copyOf(copy);
  }

  @Override public void close() {
    panels = Map.of();
    PacketEvents.getAPI().getEventManager().unregisterListener(this);
  }

  private void put(UUID id, Panel panel) {
    Map<UUID, Panel> copy = new HashMap<>(panels);
    copy.put(id, panel);
    panels = Map.copyOf(copy);
  }

  private List<ItemStack> convert(List<org.bukkit.inventory.ItemStack> items) {
    List<ItemStack> result = new ArrayList<>(36);
    for (org.bukkit.inventory.ItemStack item : items)
      result.add(SpigotConversionUtil.fromBukkitItemStack(item));
    return List.copyOf(result);
  }

  @Override public void onPacketSend(PacketSendEvent event) {
    if (!(event.getPlayer() instanceof Player player)) return;
    Panel panel = panels.get(player.getUniqueId());
    if (panel == null) return;
    if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
      WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow(event);
      panel.windowId = panel.title.equals(packet.getTitle()) ? packet.getContainerId() : -2;
    } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
      WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
      int id = packet.getWindowId();
      if (id == 0 || packet.getItems().size() < 90) return;
      if (panel.windowId == -1) panel.windowId = id;
      if (panel.windowId != id) return;
      List<ItemStack> items = new ArrayList<>(packet.getItems());
      for (int slot = 0; slot < 36; slot++) items.set(54 + slot, panel.items.get(slot));
      packet.setItems(items);
      event.markForReEncode(true);
    } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
      WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
      int slot = packet.getSlot();
      if (packet.getWindowId() == panel.windowId && panel.windowId > 0
          && slot >= 54 && slot < 90) {
        packet.setItem(panel.items.get(slot - 54));
        event.markForReEncode(true);
      }
    }
  }
}
