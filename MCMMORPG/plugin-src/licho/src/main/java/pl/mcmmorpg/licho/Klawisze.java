package pl.mcmmorpg.licho;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
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
  /** Licha: ich własny klient dostaje puste sloty zbroi, drugiej ręki i paska — niewidzialność nie chowa zbroi
   *  ani przedmiotu w ręce, więc w F5 gracz widział siebie w zbroi w środku sprzętu (16.11, właściciel na world1).
   *  Zmieniamy tylko pakiety do tego gracza; prawdziwy ekwipunek na serwerze zostaje nietknięty. */
  final Set<UUID> bezRzeczy = ConcurrentHashMap.newKeySet();
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

  // okno 0 (ekwipunek gracza): 5–8 zbroja, 36–44 pasek, 45 druga ręka; SetPlayerInventory: 0–8 pasek, 36–39 zbroja, 40 druga ręka
  private static boolean ukrytyWOknie(int s) { return (s >= 5 && s <= 8) || (s >= 36 && s <= 45); }
  private static boolean ukrytyWEkwipunku(int s) { return (s >= 0 && s <= 8) || (s >= 36 && s <= 40); }

  @Override public void onPacketSend(PacketSendEvent e) {
    UUID id = e.getUser().getUUID();
    if (id == null || !bezRzeczy.contains(id)) return;
    var typ = e.getPacketType();
    if (typ == PacketType.Play.Server.WINDOW_ITEMS) {
      WrapperPlayServerWindowItems w = new WrapperPlayServerWindowItems(e);
      if (w.getWindowId() != 0) return;
      List<ItemStack> rzeczy = new ArrayList<>(w.getItems());
      for (int i = 0; i < rzeczy.size(); i++) if (ukrytyWOknie(i)) rzeczy.set(i, ItemStack.EMPTY);
      w.setItems(rzeczy);
      e.markForReEncode(true);
    } else if (typ == PacketType.Play.Server.SET_SLOT) {
      WrapperPlayServerSetSlot w = new WrapperPlayServerSetSlot(e);
      boolean ukryj = w.getWindowId() == 0 ? ukrytyWOknie(w.getSlot()) : w.getWindowId() == -2 && ukrytyWEkwipunku(w.getSlot());
      if (ukryj) { w.setItem(ItemStack.EMPTY); e.markForReEncode(true); }
    } else if (typ == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
      WrapperPlayServerSetPlayerInventory w = new WrapperPlayServerSetPlayerInventory(e);
      if (ukrytyWEkwipunku(w.getSlot())) { w.setStack(ItemStack.EMPTY); e.markForReEncode(true); }
    }
  }

  private void naGlowny(UUID id, String akcja, int encja) {
    Bukkit.getScheduler().runTask(plugin, () -> {
      Player p = Bukkit.getPlayer(id);
      if (p != null) plugin.klawisz(p, akcja, encja);
    });
  }
}
