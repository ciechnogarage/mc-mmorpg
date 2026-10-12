package pl.mcmmorpg.wiary;

import fr.phoenixdevt.profiles.ProfileDataModule;
import fr.phoenixdevt.profiles.event.ProfileCreateEvent;
import fr.phoenixdevt.profiles.event.ProfileRemoveEvent;
import fr.phoenixdevt.profiles.event.ProfileSelectEvent;
import fr.phoenixdevt.profiles.event.ProfileUnloadEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Moduł danych MMOProfiles (https://docs.phoenixdevt.fr/mmoprofiles/api/custom-module.html), ten sam wzór co
 * w Chowańcach: każde z czterech zdarzeń MUSI dostać validate(this), inaczej MMOProfiles czeka na nas w nieskończoność.
 * Pliki są małe, więc czytamy je na wątku głównym.
 */
public final class ProfilModul implements ProfileDataModule {
  private final WiaryPlugin plugin;
  private final NamespacedKey id;

  public ProfilModul(WiaryPlugin plugin) {
    this.plugin = plugin;
    this.id = new NamespacedKey(plugin, "wiary");
  }

  @Override public JavaPlugin getOwningPlugin() { return plugin; }
  @Override public NamespacedKey getId() { return id; }

  @EventHandler
  public void onCreate(ProfileCreateEvent e) {
    e.validate(this);
  }

  @EventHandler
  public void onSelect(ProfileSelectEvent e) {
    try {
      plugin.aktywuj(e.getPlayer(), e.getProfile().getUniqueId());
    } catch (RuntimeException ex) {
      plugin.getLogger().severe("Błąd wczytania wiary " + e.getPlayer().getName() + ": " + ex);
    } finally {
      e.validate(this);
    }
  }

  @EventHandler
  public void onUnload(ProfileUnloadEvent e) {
    try {
      plugin.dezaktywuj(e.getPlayer());
    } catch (RuntimeException ex) {
      plugin.getLogger().severe("Błąd zdjęcia wiary " + e.getPlayer().getName() + ": " + ex);
    } finally {
      e.validate(this);
    }
  }

  @EventHandler
  public void onRemove(ProfileRemoveEvent e) {
    try {
      plugin.magazyn().archiwizuj(e.getProfile().getUniqueId());
    } finally {
      e.validate(this);
    }
  }
}
