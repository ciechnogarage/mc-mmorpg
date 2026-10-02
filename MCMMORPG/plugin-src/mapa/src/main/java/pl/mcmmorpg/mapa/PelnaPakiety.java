package pl.mcmmorpg.mapa;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import de.pianoman911.mapengine.api.clientside.IFrame;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

// Pakiet ramki moze przyjsc na watku sieciowym; stan sesji zmieniamy dopiero na glownym.
final class PelnaPakiety extends PacketListenerAbstract {
  /** Indeks flagi "response" encji Interaction (1.21.11). */
  private static final int INTERAKCJA_ODPOWIEDZ = 10;

  private final PelnaMapa map;
  private final MapaPlugin plugin;
  private final Set<Integer> interakcje = ConcurrentHashMap.newKeySet();

  PelnaPakiety(PelnaMapa map, MapaPlugin plugin) {
    super(PacketListenerPriority.NORMAL);
    this.map = map;
    this.plugin = plugin;
    PacketEvents.getAPI().getEventManager().registerListener(this);
  }

  void close() { PacketEvents.getAPI().getEventManager().unregisterListener(this); }

  /**
   * MapEngine wysyla encje Interaction przed ramkami z response=true. Klient traktuje wtedy kazdy PPM jako
   * udana akcje i macha reka (co 4 ticki przy trzymanym PPM), a AnimacjeBroni bierze machniecie za cios.
   * Z response=false klik dalej dochodzi (INTERACT_ENTITY), ale reka stoi. Rejestrowac PRZED spawn().
   */
  void bezMachniecia(IMapDisplay display) {
    for (IFrame frame : display.frames()) interakcje.add(frame.interactionEntityId());
  }

  void zapomnij(IMapDisplay display) {
    for (IFrame frame : display.frames()) interakcje.remove(frame.interactionEntityId());
  }

  @Override public void onPacketSend(PacketSendEvent event) {
    if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA || interakcje.isEmpty()) return;
    WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
    if (!interakcje.contains(packet.getEntityId())) return;
    boolean zmiana = false;
    for (EntityData<?> data : packet.getEntityMetadata()) {
      if (data.getIndex() == INTERAKCJA_ODPOWIEDZ && Boolean.TRUE.equals(data.getValue())) {
        @SuppressWarnings("unchecked") EntityData<Boolean> flaga = (EntityData<Boolean>) data;
        flaga.setValue(false);
        zmiana = true;
      }
    }
    if (zmiana) event.markForReEncode(true);
  }

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
