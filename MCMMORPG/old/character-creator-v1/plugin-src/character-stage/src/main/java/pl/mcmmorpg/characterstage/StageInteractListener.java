package pl.mcmmorpg.characterstage;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class StageInteractListener implements Listener {
  private final CharacterStageService service;

  StageInteractListener(CharacterStageService service) {
    this.service = service;
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    if (event.getClickedBlock() == null) {
      return;
    }
    Action action = event.getAction();
    if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
      return;
    }
    if (service.tryHandleAnchor(event.getPlayer(), event.getClickedBlock())) {
      event.setCancelled(true);
    }
  }
}
