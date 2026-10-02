package pl.mcmmorpg.mapa;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

// Pakiet ramki moze przyjsc na watku sieciowym; stan sesji zmieniamy dopiero na glownym.
final class PelnaPakiety extends PacketListenerAbstract {
  private final PelnaMapa map;
  private final MapaPlugin plugin;

  PelnaPakiety(PelnaMapa map, MapaPlugin plugin) {
    super(PacketListenerPriority.NORMAL);
    this.map = map;
    this.plugin = plugin;
    PacketEvents.getAPI().getEventManager().registerListener(this);
  }

  void close() { PacketEvents.getAPI().getEventManager().unregisterListener(this); }

  @Override public void onPacketReceive(PacketReceiveEvent event) {
    if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY
        || !(event.getPlayer() instanceof Player player)) return;
    UUID id = player.getUniqueId();
    if (!map.packetActive(id)) return;
    WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
    boolean attack = packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
    event.setCancelled(true);
    map.packetPending(id, attack);
    Bukkit.getScheduler().runTask(plugin, () -> map.packetInput(id, attack));
  }
}
