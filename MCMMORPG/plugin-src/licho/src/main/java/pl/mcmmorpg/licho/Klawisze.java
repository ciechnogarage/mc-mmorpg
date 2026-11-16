package pl.mcmmorpg.licho;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Klawisze w grze, przejmowane pakietami — zanim zobaczą je Księga (F = zamiana broni), AnimacjeBroni (LPM = seria
 * ciosów), Zaklęcia czy MenuKey (Q = hub). LPM w encję = cios, PPM w encję = wcielenie, F = psota/okadzanie, Q = pauza.
 * Pakiety przychodzą w wątku sieci: tu tylko anulujemy i przekazujemy akcję na wątek główny.
 */
final class Klawisze extends PacketListenerAbstract {
  final Set<UUID> gracze = ConcurrentHashMap.newKeySet();
  private final LichoPlugin plugin;

  Klawisze(LichoPlugin plugin) {
    super(PacketListenerPriority.LOWEST);
    this.plugin = plugin;
  }

  @Override public void onPacketReceive(PacketReceiveEvent e) {
    UUID id = e.getUser().getUUID();
    if (id == null || !gracze.contains(id)) return;
    var typ = e.getPacketType();
    if (typ == PacketType.Play.Client.PLAYER_DIGGING) {
      DiggingAction a = new WrapperPlayClientPlayerDigging(e).getAction();
      e.setCancelled(true);
      if (a == DiggingAction.DROP_ITEM || a == DiggingAction.DROP_ITEM_STACK) naGlowny(id, "pauza", 0);
      else if (a == DiggingAction.SWAP_ITEM_WITH_OFFHAND) naGlowny(id, "f", 0);
    } else if (typ == PacketType.Play.Client.INTERACT_ENTITY) {
      WrapperPlayClientInteractEntity w = new WrapperPlayClientInteractEntity(e);
      e.setCancelled(true);
      var akcja = w.getAction();
      if (akcja == WrapperPlayClientInteractEntity.InteractAction.ATTACK) naGlowny(id, "cios", w.getEntityId());
      else if (akcja == WrapperPlayClientInteractEntity.InteractAction.INTERACT && w.getHand() == InteractionHand.MAIN_HAND) naGlowny(id, "ppm", w.getEntityId());
    } else if (typ == PacketType.Play.Client.ANIMATION || typ == PacketType.Play.Client.USE_ITEM
        || typ == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
      e.setCancelled(true);
    }
  }

  private void naGlowny(UUID id, String akcja, int encja) {
    Bukkit.getScheduler().runTask(plugin, () -> {
      Player p = Bukkit.getPlayer(id);
      if (p != null) plugin.klawisz(p, akcja, encja);
    });
  }
}
