package pl.mcmmorpg.charactercreator.scene;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

/**
 * Cisza na czacie w scenie (wybor postaci i kreator). Wlasciciel 30.09: "wywal czat w wyborze
 * i kreatorze" — wiadomosci graczy, powitania i komunikaty pluginow zaslanialy scene.
 *
 * <p>Odrzucamy pakiety czatu wysylane do gracza, ktory stoi w scenie. Pasek akcji (overlay
 * w {@code system_chat}) przepuszczamy — to na nim sa podpowiedzi klawiszy sceny. Po wyjsciu ze
 * sceny czat wraca sam; tego, co przyszlo w trakcie, gracz nie zobaczy (celowo — to byl szum).
 * Lista scen ({@link SceneManager#inScene}) stoi na ConcurrentHashMap, wiec watek sieci moze
 * ja czytac.
 */
public final class SceneChatMute extends PacketListenerAbstract {

  private final SceneManager scenes;
  private PacketListenerCommon registered;

  private SceneChatMute(SceneManager scenes) {
    super(PacketListenerPriority.NORMAL);
    this.scenes = scenes;
  }

  /** Rejestruje wyciszanie, jesli packetevents dziala; inaczej WARN i nic. */
  public static SceneChatMute register(SceneManager scenes, Logger logger) {
    if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
      logger.warning("Brak packetevents — czat w scenie nie bedzie wyciszony.");
      return null;
    }
    SceneChatMute mute = new SceneChatMute(scenes);
    mute.registered = PacketEvents.getAPI().getEventManager().registerListener(mute);
    return mute;
  }

  public void unregister() {
    if (registered != null) {
      PacketEvents.getAPI().getEventManager().unregisterListener(registered);
      registered = null;
    }
  }

  @Override
  public void onPacketSend(PacketSendEvent event) {
    PacketTypeCommon type = event.getPacketType();
    boolean system = type == PacketType.Play.Server.SYSTEM_CHAT_MESSAGE;
    if (!system && type != PacketType.Play.Server.CHAT_MESSAGE
        && type != PacketType.Play.Server.DISGUISED_CHAT) {
      return;
    }
    UUID id = event.getUser() == null ? null : event.getUser().getUUID();
    if (id == null || !scenes.inScene(id)) {
      return;
    }
    if (system && new WrapperPlayServerSystemChatMessage(event).isOverlay()) {
      return;
    }
    event.setCancelled(true);
  }
}
