package pl.mcmmorpg.mapa;

import net.Indyuce.mmocore.api.event.PlayerEnterCastingModeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Tryb skilli MMOCore (F) zamyka pelna mape. Rejestrowany tylko, gdy MMOCore jest wlaczony (softdepend). */
final class TrybSkilliMost implements Listener {
  private final PelnaMapa fullMap;

  TrybSkilliMost(PelnaMapa fullMap) { this.fullMap = fullMap; }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void trybSkilli(PlayerEnterCastingModeEvent event) {
    if (fullMap.active(event.getPlayer())) fullMap.close(event.getPlayer());
  }
}
