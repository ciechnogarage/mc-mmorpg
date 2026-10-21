package pl.mcmmorpg.charactercreator.select;

import fr.phoenixdevt.profiles.event.PendingProfileEvent;
import fr.phoenixdevt.profiles.event.ProfileRemoveEvent;
import fr.phoenixdevt.profiles.event.ProfileSelectedEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pl.mcmmorpg.charactercreator.persist.TrashStore;

/**
 * Dwie rzeczy wokol kosza i usuwania:
 * <ol>
 *   <li>postac z kosza, ktora mimo wszystko zostala WYBRANA (zapasowe okno MMOProfiles, admin
 *       {@code /mmoprofiles open}) — wraca z kosza. Gracz nia gra, wiec nie moze po cichu
 *       zniknac po terminie;</li>
 *   <li>diagnostyka {@code removeProfile}: MMOProfiles zdejmuje profil z listy od razu, ale
 *       future domyka dopiero, gdy KAZDY zarejestrowany modul danych zwaliduje
 *       {@code ProfileRemoveEvent} (bajtkod PendingProfileEvent: lista {@code notLoaded},
 *       callback = samo {@code future.complete}). Future, ktory "nigdy sie nie domyka", to
 *       modul, ktory tego eventu nie waliduje. {@link #pendingModules} mowi ktory.</li>
 * </ol>
 */
public final class TrashGuard implements Listener {

  private final TrashStore trash;
  private final Logger logger;
  private final Map<UUID, PendingProfileEvent> removals = new ConcurrentHashMap<>();

  public TrashGuard(TrashStore trash, Logger logger) {
    this.trash = trash;
    this.logger = logger;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onSelected(ProfileSelectedEvent event) {
    UUID id = event.getProfile().getUniqueId();
    if (trash.restore(id)) {
      logger.info("Postac " + id + " gracza " + event.getPlayer().getName()
          + " zostala wybrana mimo kosza — wraca z kosza.");
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onRemove(ProfileRemoveEvent event) {
    removals.put(event.getProfile().getUniqueId(), event);
  }

  /** Moduly, ktore jeszcze nie zwalidowaly usuniecia profilu (puste = wszystkie). */
  public List<String> pendingModules(UUID profileId) {
    PendingProfileEvent event = removals.get(profileId);
    if (event == null) {
      return List.of("(brak eventu)");
    }
    try {
      Field f = PendingProfileEvent.class.getDeclaredField("notLoaded");
      f.setAccessible(true);
      return ((List<?>) f.get(event)).stream().map(String::valueOf).toList();
    } catch (ReflectiveOperationException | RuntimeException ex) {
      return List.of("(nie da sie odczytac: " + ex + ")");
    }
  }

  public void forget(UUID profileId) {
    removals.remove(profileId);
  }
}
