package pl.mcmmorpg.wyscigi;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Klawisze w trakcie wyścigu, przejmowane pakietami — zanim zobaczą je Księga (F = zamiana broni), AnimacjeBroni
 * (LPM = seria ciosów), Zaklęcia czy MenuKey (Q = hub). Q = pauza, F = bonus, LPM = cios.
 * Pakiety przychodzą w wątku sieci: tu tylko anulujemy i przekazujemy akcję na wątek główny.
 */
final class Klawisze extends PacketListenerAbstract {
  final Set<UUID> sciganci = ConcurrentHashMap.newKeySet();
  private final WyscigiPlugin plugin;

  Klawisze(WyscigiPlugin plugin) {
    super(PacketListenerPriority.LOWEST);
    this.plugin = plugin;
  }

  @Override public void onPacketReceive(PacketReceiveEvent e) {
    UUID id = e.getUser().getUUID();
    if (id == null || !sciganci.contains(id)) return;
    var typ = e.getPacketType();
    if (typ == PacketType.Play.Client.PLAYER_DIGGING) {
      DiggingAction a = new WrapperPlayClientPlayerDigging(e).getAction();
      if (a == DiggingAction.DROP_ITEM || a == DiggingAction.DROP_ITEM_STACK) { e.setCancelled(true); naGlowny(id, "pauza"); }
      else if (a == DiggingAction.SWAP_ITEM_WITH_OFFHAND) { e.setCancelled(true); naGlowny(id, "bonus"); }
      else e.setCancelled(true);                                     // kopanie z siodła: nic
    } else if (typ == PacketType.Play.Client.ANIMATION) {
      e.setCancelled(true); naGlowny(id, "cios");
    } else if (typ == PacketType.Play.Client.INTERACT_ENTITY || typ == PacketType.Play.Client.USE_ITEM
        || typ == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
      e.setCancelled(true);
    }
  }

  private void naGlowny(UUID id, String akcja) {
    Bukkit.getScheduler().runTask(plugin, () -> {
      Player p = Bukkit.getPlayer(id);
      if (p != null) plugin.klawisz(p, akcja);
    });
  }
}
