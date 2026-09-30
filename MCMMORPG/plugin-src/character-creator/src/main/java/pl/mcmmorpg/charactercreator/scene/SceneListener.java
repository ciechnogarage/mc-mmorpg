package pl.mcmmorpg.charactercreator.scene;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Wejscie gracza w scenie i blokady. Kazda pulapka z PoC ma tu swoj handler:
 * <ol>
 *   <li>Shift wyrzuca z kamery — anulowany {@code PlayerStopSpectatingEntityEvent};</li>
 *   <li>menu spectatora pod 1-9 teleportuje do innych graczy — anulowany teleport
 *       {@code SPECTATE};</li>
 *   <li>PPM przychodzi dwa razy (obie rece) — tylko {@code HAND} i odstep 4 tickow;</li>
 *   <li>LPM w encje to w spectatorze "zacznij ja ogladac" — anulujemy i traktujemy jak klik.</li>
 * </ol>
 */
public final class SceneListener implements Listener {

  private static final int BACK_DEBOUNCE_TICKS = 4;

  private final SceneManager scenes;
  private final Logger logger;

  public SceneListener(SceneManager scenes, Logger logger) {
    this.scenes = scenes;
    this.logger = logger;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onStartSpectating(PlayerStartSpectatingEntityEvent event) {
    SceneView view = scenes.view(event.getPlayer());
    if (view == null || view.retargeting) {
      return;
    }
    event.setCancelled(true);
    // Klik w KAZDA encje sceny to LPM — w scenie nie ma nic innego do klikania. Zlapane na
    // klonie 30.09: w scenie wyboru promien klienta trafial w postac (nosiciel HUSK) zamiast
    // w tarcze przed kamera i LPM ginal bez sladu.
    dispatch(view, c -> c.onSelect(view), "LPM");
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onStopSpectating(PlayerStopSpectatingEntityEvent event) {
    SceneView view = scenes.view(event.getPlayer());
    if (view != null && !view.retargeting) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInteract(PlayerInteractEntityEvent event) {
    SceneView view = scenes.view(event.getPlayer());
    if (view == null) {
      return;
    }
    event.setCancelled(true);
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }
    int now = Bukkit.getCurrentTick();
    if (now - view.lastBackTick < BACK_DEBOUNCE_TICKS) {
      return;
    }
    view.lastBackTick = now;
    dispatch(view, c -> c.onBack(view), "PPM");
  }

  @EventHandler
  public void onInput(PlayerInputEvent event) {
    SceneView view = scenes.view(event.getPlayer());
    if (view == null) {
      return;
    }
    Input input = event.getInput();
    // Zbocze narastajace: reagujemy na wcisniecie, nie na trzymanie klawisza.
    boolean pressedLeft = input.isLeft() && !view.lastLeft;
    boolean pressedRight = input.isRight() && !view.lastRight;
    boolean pressedUp = input.isForward() && !view.lastForward;
    boolean pressedDown = input.isBackward() && !view.lastBackward;
    view.lastLeft = input.isLeft();
    view.lastRight = input.isRight();
    view.lastForward = input.isForward();
    view.lastBackward = input.isBackward();
    if (pressedLeft) {
      dispatch(view, c -> c.onPrevious(view), "A");
    } else if (pressedRight) {
      dispatch(view, c -> c.onNext(view), "D");
    }
    if (pressedUp) {
      dispatch(view, c -> c.onUp(view), "W");
    } else if (pressedDown) {
      dispatch(view, c -> c.onDown(view), "S");
    }
    boolean pressedJump = input.isJump() && !view.lastJump;
    view.lastJump = input.isJump();
    if (pressedJump) {
      dispatch(view, c -> c.onJump(view), "Spacja");
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onTeleport(PlayerTeleportEvent event) {
    if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE
        && scenes.view(event.getPlayer()) != null) {
      event.setCancelled(true);
    }
  }

  /**
   * Komendy w scenie tylko dla administracji. Gracz ma wszystko pod klawiszami; komenda
   * wpisana w scenie (np. /spawn) wyrwalaby go z kamery w stanie SPECTATOR.
   */
  @EventHandler(priority = EventPriority.LOWEST)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    Player player = event.getPlayer();
    if (scenes.view(player) != null && !player.hasPermission("charactercreator.admin")) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    scenes.onJoin(event.getPlayer());
  }

  /** Smierc w scenie (np. z pustki pod sala) — scena nie ma juz czego pokazywac. */
  @EventHandler
  public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
    scenes.close(event.getPlayer().getUniqueId());
  }

  /**
   * LOWEST: scena ma sie zamknac ZANIM MMOProfiles zapisze postac. Zlapane na klonie:
   * wyjscie z gry w scenie zapisalo profilowi pozycje w sali sceny i tryb SPECTATOR —
   * po powrocie gracz ladowal jako duch w pustej scenerii.
   */
  @EventHandler(priority = EventPriority.LOWEST)
  public void onQuit(PlayerQuitEvent event) {
    scenes.close(event.getPlayer().getUniqueId());
  }

  /** Wejscie w trakcie sciemnienia jest polykane — dwa szybkie A nie przeskocza dwoch pozycji. */
  private void dispatch(SceneView view, Consumer<SceneController> action, String key) {
    if (view.closed || scenes.busy(view)) {
      return;
    }
    try {
      action.accept(view.controller());
    } catch (RuntimeException ex) {
      logger.severe("Scena: blad obslugi " + key + " w " + view.controller().getClass().getSimpleName()
          + ": " + ex);
      ex.printStackTrace();
    }
  }
}
