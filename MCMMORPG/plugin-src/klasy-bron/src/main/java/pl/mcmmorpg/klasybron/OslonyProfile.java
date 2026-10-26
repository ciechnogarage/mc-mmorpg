package pl.mcmmorpg.klasybron;

import fr.phoenixdevt.profiles.event.ProfileSelectedEvent;
import fr.phoenixdevt.profiles.event.ProfileUnloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Zmiana postaci w MMOProfiles bez wyjscia z serwera (rejestrowane tylko, gdy MMOProfiles jest wlaczony).
 * ProfileUnloadEvent: przed zapisem danych starej postaci; ProfileSelectedEvent: po wczytaniu nowej (+1 tick).
 */
final class OslonyProfile implements Listener {

  private final Oslony oslony;

  OslonyProfile(Oslony oslony) {
    this.oslony = oslony;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void wyladowanie(ProfileUnloadEvent e) {
    if (e.getPlayer().isOnline()) Oslony.zdejmij(e.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void wybor(ProfileSelectedEvent e) {
    oslony.zdejmijZaTick(e.getPlayer());
  }
}
