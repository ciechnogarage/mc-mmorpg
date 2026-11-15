package pl.mcmmorpg.chowance.integracja;

import net.Indyuce.mmocore.api.event.PlayerEnterCastingModeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pl.mcmmorpg.chowance.gra.TrybWalki;

/**
 * F w walce chowańców nie włącza trybu skilli MMOCore (klawisze 1–9 należą do walki). Anulowanie
 * PlayerSwapHandItemsEvent nic nie daje — MMOCore 1.13.1 słucha F z LOWEST i nie patrzy na isCancelled;
 * jedyna blokada to anulowanie PlayerEnterCastingModeEvent. Rejestrowany tylko, gdy MMOCore jest włączony.
 */
public final class BlokadaTrybuSkilli implements Listener {
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void trybSkilli(PlayerEnterCastingModeEvent e) {
    if (TrybWalki.trwa(e.getPlayer())) e.setCancelled(true);
  }
}
