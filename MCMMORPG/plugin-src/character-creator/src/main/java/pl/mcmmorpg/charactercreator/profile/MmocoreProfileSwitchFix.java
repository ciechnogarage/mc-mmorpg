package pl.mcmmorpg.charactercreator.profile;

import io.lumine.mythic.lib.api.event.session.SessionUpdateEvent;
import io.lumine.mythic.lib.profile.ProfileSessionState;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Obejscie bledu MMOCore 1.13.1-SNAPSHOT (20260330) przy zmianie postaci MMOProfiles.
 *
 * <p><b>Objaw</b> (zmierzone na klonie 27.09, w czystym przebiegu po restarcie): pierwsza zmiana
 * postaci dziala, DRUGA zawiesza gracza — klasa "Human", w logu {@code BoundSkillInfo has
 * already been closed} przy wczytywaniu, potem NPE {@code bufferedForcedClassSelectionCallback
 * is null} i co 5 s {@code Ghost session detected ... state=CLOSING}. Kolejne wybory postaci
 * nie przechodza az do restartu serwera (relog nie pomaga).
 *
 * <p><b>Przyczyna</b> (bajtkod): MythicLib podmienia {@code PlayerData} na swiezy dopiero przy
 * stanie sesji DEAD, czyli przy wyjsciu z serwera. Przy zmianie postaci ten sam obiekt jest
 * uzywany ponownie, a w nim zostaje:
 * <ul>
 *   <li>{@code boundSkills} z ZAMKNIETYMI umiejetnosciami poprzedniej postaci — {@code setClass}
 *       przy wczytywaniu zamyka je jeszcze raz i rzuca, przerywajac wczytywanie danych;</li>
 *   <li>{@code bufferedForcedClassSelection == true} z poprzedniej sesji — {@code
 *       tryForceClassSelection} przy "session ready" uznaje wtedy, ze callback juz czeka, i wola
 *       {@code null}.</li>
 * </ul>
 *
 * <p><b>Obejscie</b>: przy otwieraniu sesji (OPENING, przed wczytaniem danych przez MMOCore)
 * domykamy to, co jeszcze otwarte, czyscimy mape i zerujemy flage — stan taki, jaki ma swiezy
 * obiekt. Tylko refleksja i tylko te trzy pola; gdy MMOCore zmieni nazwy, obejscie wylacza sie
 * z jednym ostrzezeniem zamiast rzucac przy kazdej zmianie postaci. Do usuniecia po aktualizacji
 * MMOCore, ktora to naprawi (sprawdzic: dwie zmiany postaci pod rzad bez bledow w logu).
 */
public final class MmocoreProfileSwitchFix implements Listener {

  private final Logger logger;
  private Method get;
  private Field boundSkills;
  private Field forced;
  private Field forcedCallback;
  private Field skillOpen;
  private Method skillClose;
  private boolean disabled;

  public MmocoreProfileSwitchFix(Logger logger) {
    this.logger = logger;
    try {
      Class<?> playerData = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
      get = playerData.getMethod("get", java.util.UUID.class);
      boundSkills = field(playerData, "boundSkills");
      forced = field(playerData, "bufferedForcedClassSelection");
      forcedCallback = field(playerData, "bufferedForcedClassSelectionCallback");
      Class<?> bound = Class.forName("net.Indyuce.mmocore.skill.binding.BoundSkillInfo");
      skillOpen = field(bound, "open");
      skillClose = bound.getMethod("close");
      // Typy sprawdzamy od razu (naprawa 30.09): zmiana typu pola w nowym MMOCore wylaczala
      // obejscie dopiero przy PIERWSZEJ zmianie postaci, po czesciowym czyszczeniu.
      String bad = typeProblem();
      if (bad != null) {
        disable(bad);
        return;
      }
      org.bukkit.plugin.Plugin mmocore = org.bukkit.Bukkit.getPluginManager().getPlugin("MMOCore");
      logger.info("Obejscie zmiany postaci MMOCore aktywne (MMOCore "
          + (mmocore == null ? "?" : mmocore.getPluginMeta().getVersion()) + "; sprawdzone na "
          + TESTED_ON + "). Do usuniecia, gdy MMOCore naprawi blad — patrz javadoc.");
    } catch (ReflectiveOperationException | RuntimeException ex) {
      disable("nie znaleziono pol MMOCore (" + ex + ")");
    }
  }

  /**
   * Build MMOCore, na ktorym blad zmierzono i obejscie sprawdzono. Stan 30.09: nowszy build
   * 1.13.1-SNAPSHOT (12.09.2026, nexus phoenixdevt) ma w historii zmian tylko poprawki
   * atrybutow i blokow MMOItems — nic o sesjach/umiejetnosciach, wiec obejscie zostaje.
   */
  static final String TESTED_ON = "1.13.1-SNAPSHOT 20260330.110342";

  /** Null = typy pol takie, jakich obejscie oczekuje; inaczej opis problemu. */
  private String typeProblem() {
    if (!Map.class.isAssignableFrom(boundSkills.getType())) {
      return "PlayerData.boundSkills nie jest Map (" + boundSkills.getType().getName() + ")";
    }
    if (!AtomicBoolean.class.isAssignableFrom(forced.getType())) {
      return "PlayerData.bufferedForcedClassSelection nie jest AtomicBoolean (" + forced.getType().getName() + ")";
    }
    if (forcedCallback.getType().isPrimitive()) {
      return "PlayerData.bufferedForcedClassSelectionCallback jest prymitywem";
    }
    if (skillOpen.getType() != boolean.class && skillOpen.getType() != Boolean.class) {
      return "BoundSkillInfo.open nie jest boolean (" + skillOpen.getType().getName() + ")";
    }
    return null;
  }

  private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
    Field f = owner.getDeclaredField(name);
    f.setAccessible(true);
    return f;
  }

  private void disable(String why) {
    disabled = true;
    logger.warning("Obejscie zmiany postaci MMOCore WYLACZONE: " + why + ". Druga zmiana postaci "
        + "w jednej sesji moze zawiesic gracza (Ghost session) — sprawdz wersje MMOCore.");
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onSession(SessionUpdateEvent event) {
    if (disabled || event.getNewState() != ProfileSessionState.OPENING) {
      return;
    }
    Object data;
    try {
      data = get.invoke(null, event.getPlayerData().getUniqueId());
    } catch (ReflectiveOperationException | RuntimeException ex) {
      return; // brak danych MMOCore dla gracza — nie ma czego czyscic
    }
    if (data == null) {
      return;
    }
    try {
      int cleared = 0;
      Map<?, ?> skills = (Map<?, ?>) boundSkills.get(data);
      for (Object info : skills.values()) {
        if (Boolean.TRUE.equals(skillOpen.get(info))) {
          skillClose.invoke(info);
        }
        cleared++;
      }
      skills.clear();
      boolean stale = ((AtomicBoolean) forced.get(data)).getAndSet(false);
      forcedCallback.set(data, null);
      if (cleared > 0 || stale) {
        logger.fine("Obejscie MMOCore: " + event.getPlayerData().getUniqueId() + " — wyczyszczono "
            + cleared + " umiejetnosci poprzedniej postaci, flaga wyboru klasy: " + stale);
      }
    } catch (ReflectiveOperationException | RuntimeException ex) {
      disable("blad przy czyszczeniu (" + ex + ")");
    }
  }
}
