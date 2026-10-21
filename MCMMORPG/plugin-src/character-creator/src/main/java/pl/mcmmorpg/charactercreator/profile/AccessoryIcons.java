package pl.mcmmorpg.charactercreator.profile;

import fr.phoenixdevt.profiles.event.ProfileSelectedEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredListener;

/**
 * Ikony slotow akcesoriow MMOInventory (rzad 9-17 plecaka) po oddaniu ekwipunku nowej postaci.
 *
 * <p>MMOInventory w trybie vanilla wklada ikony na {@code ProfileSelectedEvent}
 * ({@code VanillaInventoryProfileListener} -> {@code VanillaInventory#replacePlaceholderItems}).
 * Kreator oddaje ekwipunek sprzed tworzenia PO tym zdarzeniu, wiec nadpisywal ikony — nowa
 * postac miala pusty rzad do pierwszego relogu albo smierci (zmierzone klientem QA 27.09).
 *
 * <p>MMOInventory nie ma na to komendy ani API, a {@code replacePlaceholderItems} to metoda
 * INSTANCJI konfiguracji ekwipunku. Instancje bierzemy z jego wlasnego listenera, zarejestrowanego
 * na {@code ProfileSelectedEvent} (pole {@code config}, bajtkod MMOInventory 2.0-20260330).
 * Wszystko po refleksji: brak MMOInventory albo inna wersja = cichy brak efektu i jedna linia w logu.
 */
final class AccessoryIcons {

  private static final String LISTENER_CLASS =
      "net.Indyuce.inventory.inventory.util.VanillaInventoryProfileListener";
  private static boolean warned;

  private AccessoryIcons() {
  }

  static void refresh(Player player, Logger logger) {
    try {
      for (RegisteredListener registered : ProfileSelectedEvent.getHandlerList().getRegisteredListeners()) {
        Object listener = registered.getListener();
        if (!LISTENER_CLASS.equals(listener.getClass().getName())) {
          continue;
        }
        Field field = listener.getClass().getDeclaredField("config");
        field.setAccessible(true);
        Object inventory = field.get(listener);
        Method replace = inventory.getClass().getMethod("replacePlaceholderItems", Player.class);
        replace.invoke(inventory, player);
        return;
      }
    } catch (ReflectiveOperationException | RuntimeException ex) {
      if (!warned) {
        warned = true;
        logger.warning("Ikony akcesoriow MMOInventory: nie udalo sie odswiezyc (" + ex
            + ") — nowa postac zobaczy je po relogu.");
      }
    }
  }
}
