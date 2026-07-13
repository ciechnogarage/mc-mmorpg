package pl.mcmmorpg.characterstage;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class JoinStageListener implements Listener {
  private final CharacterStageService service;

  JoinStageListener(CharacterStageService service) {
    this.service = service;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (service.config().interceptJoin()) {
      service.stage(event.getPlayer());
    }
  }

  @EventHandler
  public void onMove(PlayerMoveEvent event) {
    if (event.getTo() == null) {
      return;
    }
    service.blockStageEscape(event.getPlayer(), event.getTo());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    if (service.isStaged(event.getPlayer())) {
      service.clearSession(event.getPlayer());
    }
  }
}
