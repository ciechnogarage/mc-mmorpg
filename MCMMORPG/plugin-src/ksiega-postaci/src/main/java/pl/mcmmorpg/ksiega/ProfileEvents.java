package pl.mcmmorpg.ksiega;

import fr.phoenixdevt.profiles.event.ProfileSelectedEvent;
import fr.phoenixdevt.profiles.event.ProfileUnloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

final class ProfileEvents implements Listener {
  private final KsiegaPostaci plugin;
  ProfileEvents(KsiegaPostaci plugin) { this.plugin = plugin; }

  @EventHandler(priority = EventPriority.LOWEST)
  public void selected(ProfileSelectedEvent event) {
    plugin.bron.canonical(event.getPlayer());
    plugin.dane.unload(event.getPlayer());
    plugin.refreshLater(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void unload(ProfileUnloadEvent event) {
    plugin.bron.canonical(event.getPlayer());
    plugin.dane.unload(event.getPlayer());
  }
}
