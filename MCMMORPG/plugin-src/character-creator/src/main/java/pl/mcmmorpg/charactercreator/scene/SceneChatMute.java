package pl.mcmmorpg.charactercreator.scene;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import fr.phoenixdevt.profiles.event.ProfileSelectEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Cisza na czacie w scenie (wybor postaci i kreator). Wlasciciel 30.09: "wywal czat w wyborze
 * i kreatorze" — wiadomosci graczy, powitania i komunikaty pluginow zaslanialy scene.
 *
 * <p>Odrzucamy pakiety czatu wysylane do gracza, ktory stoi w scenie. Pasek akcji (overlay
 * w {@code system_chat}) przepuszczamy — to na nim sa podpowiedzi klawiszy sceny. Po wyjsciu ze
 * sceny czat wraca sam; tego, co przyszlo w trakcie, gracz nie zobaczy (celowo — to byl szum).
 * Lista scen ({@link SceneManager#inScene}) stoi na ConcurrentHashMap, wiec watek sieci moze
 * ja czytac.
 *
 * <p>Wlasciciel 17.10: "wylacz czat przy kreatorze postaci bo syfi". Cisza obejmowala tylko
 * gracza JUZ w scenie, a scena otwiera sie dopiero, gdy MMOProfiles wczyta postacie (kilka
 * tickow do kilku sekund po wejsciu). W tej dziurze przechodzily powitania, "X dolaczyl" i czat
 * innych — i wisialy potem nad scena. Dlatego cisza zaczyna sie juz przy wejsciu (LOWEST, przed
 * powitaniami) i trzyma do otwarcia sceny, wyboru postaci albo {@link #HOLD_MS} jako bezpiecznik
 * (gdy zamiast sceny wyskoczy okno MMOProfiles).
 */
public final class SceneChatMute extends PacketListenerAbstract implements Listener {

  /** Dluzej niz czekanie na MMOProfiles w scenie (20 s) i w strazniku wyboru (15 s). */
  private static final long HOLD_MS = 25_000L;

  private final SceneManager scenes;
  private final boolean holdOnJoin;
  /** Gracz -> do kiedy (ms) cisza z wejscia, zanim otworzy sie scena. Czytane z watku sieci. */
  private final Map<UUID, Long> held = new ConcurrentHashMap<>();
  private PacketListenerCommon registered;

  private SceneChatMute(SceneManager scenes, boolean holdOnJoin) {
    super(PacketListenerPriority.NORMAL);
    this.scenes = scenes;
    this.holdOnJoin = holdOnJoin;
  }

  /**
   * Rejestruje wyciszanie, jesli packetevents dziala; inaczej WARN i nic.
   *
   * @param holdOnJoin scena wyboru wita kazdego wchodzacego — cisza od wejscia, nie od sceny
   */
  public static SceneChatMute register(Plugin plugin, SceneManager scenes, Logger logger,
      boolean holdOnJoin) {
    if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
      logger.warning("Brak packetevents — czat w scenie nie bedzie wyciszony.");
      return null;
    }
    SceneChatMute mute = new SceneChatMute(scenes, holdOnJoin);
    mute.registered = PacketEvents.getAPI().getEventManager().registerListener(mute);
    Bukkit.getPluginManager().registerEvents(mute, plugin);
    scenes.setOpenHook(mute.held::remove);
    return mute;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onJoin(PlayerJoinEvent event) {
    if (holdOnJoin) {
      held.put(event.getPlayer().getUniqueId(), System.currentTimeMillis() + HOLD_MS);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onSelect(ProfileSelectEvent event) {
    held.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    held.remove(event.getPlayer().getUniqueId());
  }

  private boolean heldFromJoin(UUID id) {
    Long until = held.get(id);
    if (until == null) {
      return false;
    }
    if (System.currentTimeMillis() < until) {
      return true;
    }
    held.remove(id, until);
    return false;
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
    if (id == null || !(scenes.inScene(id) || heldFromJoin(id))
        || !pl.mcmmorpg.charactercreator.Ustawienia.b("scena.cisza-na-czacie", true)) {
      return;
    }
    if (system && new WrapperPlayServerSystemChatMessage(event).isOverlay()) {
      return;
    }
    event.setCancelled(true);
  }
}
