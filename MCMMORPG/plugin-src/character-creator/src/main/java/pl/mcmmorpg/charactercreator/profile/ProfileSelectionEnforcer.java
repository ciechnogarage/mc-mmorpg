package pl.mcmmorpg.charactercreator.profile;

import fr.phoenixdevt.mmoprofiles.bukkit.MMOProfiles;
import fr.phoenixdevt.profiles.ProfileList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import pl.mcmmorpg.charactercreator.CreatorConfig;
import pl.mcmmorpg.charactercreator.creator.CreatorSessionManager;

/**
 * Pilnuje, zeby nikt nie grał bez postaci.
 *
 * <p><b>Czego brakowalo.</b> Pierwszy krok cyklu tworzenia postaci — "gracz wchodzi, nie ma
 * postaci, trafia do kreatora" — nie istnial. MMOProfiles otwiera ekran profili po
 * zalogowaniu ({@code no-gui-on-login: false}), ale gracz mogl go zamknac Escape'em i grac
 * dalej: w survivalu, w swiecie {@code world}, bez rasy, bez klasy MMOCore i bez rekordu
 * w characters.yml. W logu widac to wprost — KreatorQA zalogowal sie o 11:50:03, a kreator
 * otworzyl sie dopiero o 11:50:22, po RECZNYM kliknieciu "[+] Create New Character".
 *
 * <p><b>Dlaczego nie wlasna pulapka.</b> Kuszace bylo zamrozic gracza wlasnym listenerem
 * ruchu albo trzymac go w SPECTATOR do czasu wyboru. To byloby trzecie miejsce w tym stacku
 * walczace o tryb gry i pozycje gracza (po Multiverse i MMOProfiles) — dokladnie ten rodzaj
 * nakladajacych sie straznikow, przez ktory tryb gry w sali kreatora trzeba bylo wymuszac
 * co 10 tickow. MMOProfiles ma na to WLASNA, wspierana droge: {@code /mmoprofiles open
 * <gracz>}, ktorego argument {@code force-select} ma fallback ustawiony na
 * {@code !hasChosenProfile()} — czyli dla gracza bez wybranego profilu wymuszenie wlacza sie
 * samo (sprawdzone w bajtkodzie {@code OpenCommandNode}). Wolamy to z KONSOLI, wiec permisja
 * {@code mmoprofiles.command} (default: op) nie wchodzi w gre.
 *
 * <p>Ponawianie jest celowo rzadkie i bezterminowe. Gracz, ktory uparcie zamyka ekran, po
 * prostu dostaje go z powrotem — nie jest kickowany, bo kick za brak postaci to gorsze
 * pierwsze wrazenie niz ekran wyboru, ktory nie chce zniknac.
 */
public final class ProfileSelectionEnforcer implements Listener {

  private final Plugin plugin;
  private final CreatorConfig config;
  private final CreatorSessionManager sessions;
  private final Logger logger;
  /** Gracz w scenie kreatora — ekran MMOProfiles przykrylby kamere. Domyslnie nikt. */
  private java.util.function.Predicate<UUID> inScene = id -> false;

  /** Scena wyboru postaci zamiast okna MMOProfiles; false = nie wyszlo, otworz okno. */
  private java.util.function.Predicate<Player> opener = p -> false;

  public void setOpener(java.util.function.Predicate<Player> opener) {
    this.opener = opener;
  }

  public void setSceneCheck(java.util.function.Predicate<UUID> inScene) {
    this.inScene = inScene;
  }

  public ProfileSelectionEnforcer(Plugin plugin, CreatorConfig config,
      CreatorSessionManager sessions, Logger logger) {
    this.plugin = plugin;
    this.config = config;
    this.sessions = sessions;
    this.logger = logger;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (!config.enforceProfileSelection()) {
      return;
    }
    scheduleCheck(event.getPlayer().getUniqueId(), config.profileCheckDelayTicks());
  }

  /**
   * Co ile tickow pytac, czy MMOProfiles wczytal juz dane gracza, i jak dlugo najwyzej
   * (60 x 5 = 15 s). Dawniej czekalismy na sztywno 100 tickow po wejsciu — gracz stal 5 s
   * w czarnym pudle MMOProfiles, choc dane byly gotowe po ulamku sekundy, a paczka zasobow
   * idzie przed wejsciem (Nexo send_pre_join), wiec nie ma na co czekac.
   */
  private static final long READY_POLL_TICKS = 5L;
  private static final int READY_POLL_LIMIT = 60;

  private void scheduleCheck(UUID playerId, long delayTicks) {
    scheduleCheck(playerId, delayTicks, 0);
  }

  private void scheduleCheck(UUID playerId, long delayTicks, int attempt) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> check(playerId, attempt), delayTicks);
  }

  private void check(UUID playerId, int attempt) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null || !player.isOnline() || !config.enforceProfileSelection()) {
      return;
    }
    if (!dataReady(playerId)) {
      if (attempt < READY_POLL_LIMIT) {
        scheduleCheck(playerId, READY_POLL_TICKS, attempt + 1);
      } else {
        logger.warning("MMOProfiles nie wczytal danych " + player.getName() + " w 15 s — "
            + "sprawdze ponownie za " + config.profileRecheckIntervalTicks() + " tickow.");
        long interval = config.profileRecheckIntervalTicks();
        if (interval > 0) {
          scheduleCheck(playerId, interval);
        }
      }
      return;
    }
    if (hasChosenProfile(playerId)) {
      return;
    }
    // Gracz, ktory wyszedl na ekranie smierci, wraca martwy. Scena martwego nie przyjmie,
    // a zapasowe okno MMOProfiles z wymuszonym wyborem wracalo potem NAD scena i nie dalo
    // sie go zamknac (zlapane na klonie 30.09). Czekamy na odrodzenie.
    if (player.isDead()) {
      scheduleCheck(playerId, READY_POLL_TICKS * 4);
      return;
    }
    // ZLAPANE NA PRODUKCJI, 20.09 16:20-16:21. Gracz W TRAKCIE tworzenia postaci z definicji
    // nie ma jeszcze wybranego profilu: ProfileCreateEvent jest wstrzymany do naszego
    // validate(), wiec getCurrent() zwraca null przez caly czas trwania kreatora. Bez tego
    // warunku straznik otwieral graczowi Miau ekran wyboru profilu CO 10 SEKUND na wierzchu
    // kreatora (16:20:42, 16:20:52, ...), az doprowadzil do drugiego ProfileCreateEvent
    // ("Odmowa drugiej sesji kreatora dla Miau", 16:21:14) i do bledu wewnetrznego
    // MMOProfiles przy kliknieciu w profil ("Player has not started playing",
    // ProfileListImpl.quitCurrentProfile — ani razu w ~110 wczesniejszych logach).
    //
    // To byl dokladnie ten blad, ktory ten plik w swoim wlasnym javadocu wytyka gdzie
    // indziej: kolejny straznik walczacy o to samo, zamiast ustapic wlascicielowi stanu.
    if (sessions.isActive(playerId)) {
      return;
    }
    // Ten sam przypadek co wyzej, tylko dla sceny: gracz w scenie jest w trakcie wyboru,
    // a okno MMOProfiles zaslonioby mu kamere i przejelo klikniecia.
    if (inScene.test(playerId)) {
      return;
    }
    // Gracz ma juz cos otwartego — najpewniej wlasnie ten ekran wyboru. Ponawianie
    // otwarcia na wierzchu otwartego GUI jest tym samym rodzajem walki.
    // CRAFTING to widok "nic nie otwarte" (wlasny ekwipunek gracza).
    if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
      return;
    }
    if (opener.test(player)) {
      logger.info("Gracz " + player.getName() + " nie ma wybranej postaci — otwieram scene wyboru.");
      long interval = config.profileRecheckIntervalTicks();
      if (interval > 0) {
        scheduleCheck(playerId, interval);
      }
      return;
    }
    logger.info("Gracz " + player.getName() + " nie ma wybranej postaci — otwieram ekran "
        + "wyboru profilu.");
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mmoprofiles open " + player.getName());

    long interval = config.profileRecheckIntervalTicks();
    if (interval > 0) {
      scheduleCheck(playerId, interval);
    }
  }

  /** Dane MMOProfiles gracza wczytane i bez profilu w trakcie ladowania. */
  private boolean dataReady(UUID playerId) {
    try {
      ProfileList profiles = MMOProfiles.plugin.getPlayerData(playerId);
      if (profiles == null) {
        return false;
      }
      return !(profiles instanceof fr.phoenixdevt.mmoprofiles.bukkit.profile.ProfileListImpl impl)
          || !impl.hasProfilePending();
    } catch (RuntimeException ex) {
      // Brak danych w pamieci (getPlayerData rzuca dla niewczytanego gracza) = jeszcze nie gotowe.
      return false;
    }
  }

  /**
   * @return true takze wtedy, gdy nie da sie tego ustalic — brak odpowiedzi od MMOProfiles
   *         nie moze skutkowac otwieraniem ekranu wyboru w kolko komus, kto postac ma.
   */
  private boolean hasChosenProfile(UUID playerId) {
    try {
      ProfileList profiles = MMOProfiles.plugin.getPlayerData(playerId);
      return profiles == null || profiles.getCurrent() != null;
    } catch (RuntimeException ex) {
      logger.log(Level.WARNING, "Nie udalo sie sprawdzic profilu gracza " + playerId
          + " — pomijam wymuszenie wyboru postaci dla tego gracza.", ex);
      return true;
    }
  }
}
