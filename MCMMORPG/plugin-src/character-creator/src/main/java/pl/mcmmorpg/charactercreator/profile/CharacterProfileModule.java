package pl.mcmmorpg.charactercreator.profile;

import fr.phoenixdevt.profiles.PlayerProfile;
import fr.phoenixdevt.profiles.ProfileDataModule;
import fr.phoenixdevt.profiles.event.ProfileCreateEvent;
import fr.phoenixdevt.profiles.event.ProfileRemoveEvent;
import fr.phoenixdevt.profiles.event.ProfileSelectEvent;
import fr.phoenixdevt.profiles.event.ProfileUnloadEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;
import pl.mcmmorpg.charactercreator.CreatorConfig;
import pl.mcmmorpg.charactercreator.creator.ConfirmService;
import pl.mcmmorpg.charactercreator.creator.CreatorSession;
import pl.mcmmorpg.charactercreator.creator.CreatorSessionManager;
import pl.mcmmorpg.charactercreator.creator.PendingCreation;
import pl.mcmmorpg.charactercreator.persist.CharacterRecord;
import pl.mcmmorpg.charactercreator.persist.CharacterStore;

/**
 * The MMOProfiles integration. Reacts to MMOProfiles' own profile lifecycle events rather
 * than owning join routing or a select screen (that's MMOProfiles' ProfileListViewer now).
 *
 * <p>Per docs.phoenixdevt.fr/mmoprofiles/api/custom-module.html, a module must validate()
 * FOUR events — ProfileCreateEvent, ProfileSelectEvent, ProfileUnloadEvent,
 * ProfileRemoveEvent — and profile data is only actually saved to disk during
 * ProfileUnloadEvent handling.
 *
 * <p>Root cause of an earlier "profiles never persist" bug, fully diagnosed and fixed: it
 * wasn't a session/player mismatch, and it wasn't us — MMOCore's own
 * {@code force-class-selection: true} setting (plugins/MMOCore/config.yml) makes MMOCore
 * register itself as ANOTHER blocking module on MMOProfiles' profile creation, holding it
 * pending until the player picks a class in MMOCore's own native GUI. Our creator handles
 * class selection independently and never touches that GUI, so MMOCore's half of the
 * validation never completed. Fixed by setting {@code force-class-selection: false}.
 */
public final class CharacterProfileModule implements ProfileDataModule {

  private final JavaPlugin plugin;
  private final CreatorConfig config;
  private final CharacterStore store;
  private final CreatorSessionManager sessions;
  private final ConfirmService confirmService;
  private final NamespacedKey id;
  private final Logger logger;

  /**
   * Postac, ktora jest w tej chwili aktywna dla danego gracza.
   *
   * <p>Trzymamy ja sami, bo model rasy trzeba zalozyc PONOWNIE po kazdym przeniesieniu
   * gracza (patrz {@link ConfirmService#applyRaceModel}), a w tamtym momencie nie mamy pod
   * reka zadnego eventu MMOProfiles — tylko gracza.
   */
  private final Map<UUID, CharacterRecord> activeCharacters = new ConcurrentHashMap<>();

  /**
   * Opoznienie zalozenia modelu. Teleport MMOProfiles po {@code validate()} i nasz wlasny
   * teleport po potwierdzeniu dziej sie w kolejnych tickach — model zakladamy, gdy gracz
   * juz stoi na miejscu.
   */
  private static final long MODEL_APPLY_DELAY_TICKS = 10L;

  /**
   * Opoznienie oddania ekwipunku po zatwierdzeniu profilu. Dluzsze niz
   * {@link #MODEL_APPLY_DELAY_TICKS}, bo ma wypasc PO tym, jak MMOProfiles naloz swoj
   * ekwipunek profilu — inaczej oddane rzeczy zostana nim nadpisane.
   */
  private static final long INVENTORY_RESTORE_DELAY_TICKS = 20L;

  public CharacterProfileModule(JavaPlugin plugin, CreatorConfig config, CharacterStore store,
      CreatorSessionManager sessions, ConfirmService confirmService) {
    this.plugin = plugin;
    this.config = config;
    this.store = store;
    this.sessions = sessions;
    this.confirmService = confirmService;
    this.id = new NamespacedKey(plugin, "character_data");
    this.logger = plugin.getLogger();
  }

  @Override
  public JavaPlugin getOwningPlugin() {
    return plugin;
  }

  @Override
  public NamespacedKey getId() {
    return id;
  }

  /**
   * Wejscie do kreatora.
   *
   * <p><b>Cale cialo jest w try/catch.</b> Wczesniej dowolny wyjatek (chocby
   * {@code IllegalStateException} z pustej sekcji ras) leciał przez szyne zdarzen MMOProfiles
   * i profil zostawal w stanie "pending" NA ZAWSZE — gracz nie mial jak dokonczyc tworzenia
   * ani przy tej sesji, ani przy zadnej nastepnej. Teraz awaria konczy sie zwolnieniem
   * profilu, oddaniem rzeczy i czytelnym komunikatem.
   */
  @EventHandler
  public void onCreate(ProfileCreateEvent event) {
    Player player = event.getPlayer();
    PlayerProfile profile = event.getProfile();
    UUID profileId = profile.getUniqueId();

    if (store.get(profileId) != null) {
      // Already has our data (e.g. a module reload replaying the event) — validate
      // immediately instead of sending them through the creator again.
      event.validate(this);
      return;
    }

    // Jedno zatwierdzenie na event, nawet gdyby cos wywolalo complete() dwa razy.
    AtomicBoolean validated = new AtomicBoolean();
    PendingCreation pending = () -> {
      if (validated.compareAndSet(false, true)) {
        event.validate(this);
      }
    };

    try {
      Location room = config.roomSpawn();
      if (room == null || room.getWorld() == null) {
        throw new IllegalStateException("Swiat kreatora '" + config.roomWorldName()
            + "' nie jest zaladowany");
      }
      // Pochodzenie MUSI zostac zlapane przed teleportem. Wczesniej sesja odczytywala je
      // sama, juz po przeniesieniu gracza do sali — wiec kazdy wpis w sessions.yml mial
      // origin == spawn kreatora, a cala sciezka powrotu "tam, skad przyszedl" byla martwa.
      Location origin = player.getLocation().clone();
      player.teleport(room);
      CreatorSession session = sessions.start(player, profileId, pending, origin);
      if (session == null) {
        // start() sam zalogowal powod (gracz jest juz w kreatorze dla innego profilu).
        //
        // Ten event trzeba ODRZUCIC, a nie zostawic. Poprzednia wersja nie robila ani
        // validate(), ani cancel() — "swiadomie", wedlug komentarza, ktory tu stal — wiec
        // MMOProfiles zostawalo z operacja zawieszona do konca sesji gracza. Dokladnie ta
        // klasa bledu, ktora naprawiamy w onRemove. PendingProfileEvent implementuje
        // Cancellable, wiec odmowa ma tu wlasna, jawna droge: drugi profil po prostu
        // nie powstaje, a pierwsza sesja kreatora toczy sie dalej.
        event.setCancelled(true);
        player.sendMessage(Component.text(
            "Jesteś już w kreatorze postaci — dokończ tworzenie tej, którą zacząłeś.",
            NamedTextColor.YELLOW));
      }
    } catch (RuntimeException ex) {
      logger.log(Level.SEVERE, "Nie udalo sie otworzyc kreatora dla " + player.getName()
          + " (profil " + profileId + ")", ex);
      pending.complete();
      sessions.end(player, CreatorSessionManager.EndReason.ABORTED);
      abort(player, "Nie udało się otworzyć kreatora postaci — zgłoś to administracji.");
    }
    // Zadnego komunikatu powitalnego na czacie: czat renderuje sie NA scenie i pisal
    // postaci po torsie (widoczne na zrzutach). Wskazowki siedza w tooltipach ikon,
    // a komunikaty bledow ida na actionbar (CreatorRoomListener#feedback).
  }

  /**
   * Implements CreatorRoomListener.ConfirmHandler — called once the player confirms.
   *
   * <p><b>Kolejnosc commitu jest odwrocona wzgledem starej wersji</b> i to jest najwazniejsza
   * zmiana w calym module. Bylo: zapis rekordu na dysk -> zatwierdzenie profilu -> teleport,
   * wiec KAZDA awaria po pierwszym kroku zostawiala rekord bez profilu. Jest:
   * {@code validate() -> applyToRealPlayer() -> zapis -> teleport}, wiec najwczesniejszy
   * moment, w ktorym cokolwiek trafia na dysk, jest juz PO tym, jak profil naprawde istnieje.
   */
  public void onConfirm(Player player, CreatorSession session) {
    try {
      CharacterRecord record = confirmService.buildRecord(player, session, session.profileId());
      if (record == null) {
        abort(player, "Nie udało się utworzyć postaci — zgłoś to administracji.");
        return;
      }

      if (session.pendingCreation() != null) {
        session.pendingCreation().complete();
      }
      confirmService.applyToRealPlayer(player, record);

      CharacterRecord stored = store.put(record);
      if (stored == null) {
        // Imie zajete w oknie miedzy walidacja a zapisem. Profil juz ISTNIEJE, wiec odmowa
        // byla najgorszym mozliwym wyjsciem — nadajemy imie zastepcze i mowimy o tym wprost.
        stored = storeWithFallbackName(record, player);
      }
      // MMOProfiles does not relocate the player once ProfileCreateEvent#validate()
      // completes — it only unblocks the profile — so we still own leaving creator_limbo.
      sendToWorld(player);
      trackAndApplyRaceModel(player, stored);
      // Ekwipunek oddajemy DOPIERO TERAZ i jeszcze z opoznieniem. MMOProfiles naklada na
      // gracza ekwipunek swiezo zatwierdzonego profilu ('synced-data.inventory: true') w
      // jednym z kolejnych tickow po validate() — oddanie rzeczy wczesniej (tak bylo)
      // konczy sie tym, ze zostaja nadpisane pustka. Zmierzone evalem 40-ekwipunek:
      // bot wchodzil ze 100 przedmiotami, a po utworzeniu postaci mial zero.
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        Player online = Bukkit.getPlayer(player.getUniqueId());
        if (online != null && online.isOnline()) {
          sessions.restoreEntryInventory(online, session);
        }
      }, INVENTORY_RESTORE_DELAY_TICKS);
      player.sendMessage(Component.text("Postać utworzona: " + stored.name(),
          NamedTextColor.GREEN));
    } catch (RuntimeException ex) {
      logger.log(Level.SEVERE, "Awaria przy potwierdzaniu postaci dla " + player.getName(), ex);
      abort(player, "Coś poszło nie tak przy tworzeniu postaci — zgłoś to administracji.");
    }
  }

  /**
   * Odsyla gracza z sali do swiata gry — i daje mu chwile nietykalnosci.
   *
   * <p>Wczesniej byl tu goly {@code Bukkit.getWorlds().get(0).getSpawnLocation()}. Gracz
   * ladowal na surowym spawnie, w survivalu, z pustym hotbarem, przy wlaczonym spawnie
   * mobow. W logach obu serwerow konczylo sie to tak samo: {@code KreatorQA was slain by
   * Zombie} — 36 s po zatwierdzeniu na world1, 9 s na world3. Cel wyjscia jest teraz
   * konfigurowalny ({@code creator-room.exit}), a nietykalnosc zamyka okno, w ktorym gracz
   * nie ma jeszcze kontroli nad postacia.
   */
  private void sendToWorld(Player player) {
    player.teleport(sessions.configuredExit());
    int ticks = config.exitInvulnerabilityTicks();
    if (ticks <= 0 || player.isInvulnerable()) {
      return;
    }
    UUID playerId = player.getUniqueId();
    player.setInvulnerable(true);
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      Player online = Bukkit.getPlayer(playerId);
      if (online != null && online.isOnline()) {
        online.setInvulnerable(false);
      }
    }, ticks);
  }

  private CharacterRecord storeWithFallbackName(CharacterRecord record, Player player) {
    for (int suffix = 2; suffix < 100; suffix++) {
      String candidate = record.name() + suffix;
      CharacterRecord attempt = new CharacterRecord(record.characterId(), candidate,
          record.raceId(), record.classId(), record.skinSource(), record.ownerUuid(),
          record.ownerName(), record.createdAt());
      CharacterRecord stored = store.put(attempt);
      if (stored != null) {
        logger.warning("Imie '" + record.name() + "' zostalo zajete w trakcie tworzenia "
            + "postaci dla " + player.getName() + " — zapisano jako '" + candidate + "'.");
        player.sendMessage(Component.text("Imię było już zajęte — twoja postać nazywa się "
            + candidate + ".", NamedTextColor.YELLOW));
        return stored;
      }
    }
    throw new IllegalStateException("Nie udalo sie znalezc wolnego imienia dla "
        + record.name());
  }

  /**
   * Sciezka odmowy. Musi robic DOKLADNIE TO SAMO co sciezka sukcesu poza zapisem: oddac
   * gracza swiatu i powiedziec mu, co sie stalo.
   *
   * <p>Wczesniej odmowa konczyla sie samym {@code return} przed teleportem, a wiadomosc szla
   * na zaslaniety czat — gracz zostawal w {@code creator_limbo}, w SPECTATOR wymuszanym co
   * 10 tickow, bez GUI i bez wyjscia innego niz relog. W logu widac dwoch graczy, ktorzy
   * w to wpadli.
   */
  private void abort(Player player, String message) {
    if (!player.isOnline()) {
      return;
    }
    if (sessions.isActive(player)) {
      sessions.end(player, CreatorSessionManager.EndReason.ABORTED);
    } else if (player.getWorld().getName().equals(config.roomWorldName())) {
      sendToWorld(player);
    }
    player.sendMessage(Component.text(message, NamedTextColor.RED));
    player.sendActionBar(Component.text(message, NamedTextColor.RED));
  }

  @EventHandler
  public void onSelect(ProfileSelectEvent event) {
    // docs.phoenixdevt.fr/mmoprofiles/api/custom-module.html: "After all data modules
    // validate [ProfileSelectEvent], the profile is applied to the player" — apply our
    // own data (race/class/model) here, before validating.
    CharacterRecord record = store.get(event.getProfile().getUniqueId());
    if (record != null) {
      confirmService.applyToRealPlayer(event.getPlayer(), record);
      backfillOwner(record, event.getPlayer());
    } else {
      // Profil ISTNIEJE, ale nie ma do niego naszego rekordu. Dotad konczylo sie to cicha
      // galezia else — gracz wchodzil do gry bez rasy, bez klasy i bez imienia, a w logu
      // nie bylo ani slowa. To jest slad po profilu zawieszonym przez brak validate()
      // w onRemove (patrz nizej) albo po recznej edycji characters.yml. Naprawa: /cc prune
      // pokazuje takie profile, a gracz ma sie dowiedziec, zeby zglosil to administracji,
      // zamiast zastanawiac sie, czemu nic nie dziala.
      logger.severe("Profil " + event.getProfile().getUniqueId() + " gracza "
          + event.getPlayer().getName() + " nie ma rekordu postaci w characters.yml. "
          + "Gracz wchodzi bez rasy i klasy. Sprawdz: /cc prune");
      event.getPlayer().sendMessage(Component.text(
          "Ta postać nie ma zapisanych danych — zgłoś to administracji.",
          NamedTextColor.RED));
    }
    event.validate(this);
    if (record != null) {
      // PO validate: to dopiero ono pozwala MMOProfiles zastosowac profil, czyli m.in.
      // postawic gracza na zapisanej pozycji (synced-data.location: true). Model zalozony
      // przed tym teleportem nie dozylby konca przenosin.
      trackAndApplyRaceModel(event.getPlayer(), record);
    }
  }

  /**
   * Zapamietuje aktywna postac gracza i zaklada jej model rasy z opoznieniem.
   *
   * <p>Publiczne, bo wola to tez {@link RaceModelListener} po kazdej zmianie swiata
   * i po respawnie — model nie przezywa ani jednego, ani drugiego.
   */
  public void trackAndApplyRaceModel(Player player, CharacterRecord record) {
    activeCharacters.put(player.getUniqueId(), record);
    scheduleRaceModel(player);
  }

  /** Zaklada model aktywnej postaci gracza, jesli taka zna. */
  public void scheduleRaceModel(Player player) {
    UUID playerId = player.getUniqueId();
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      Player online = Bukkit.getPlayer(playerId);
      if (online == null || !online.isOnline()) {
        return;
      }
      // W kreatorze gracz jest w SPECTATOR i oglada dummy'ego — zakladanie mu wtedy modelu
      // poprzedniej postaci nie ma sensu. Wroci przy wyjsciu z sali (zmiana swiata).
      if (sessions.isActive(online)) {
        return;
      }
      CharacterRecord active = activeCharacters.get(playerId);
      if (active != null) {
        confirmService.applyRaceModel(online, active);
      }
    }, MODEL_APPLY_DELAY_TICKS);
  }

  /** Gracz wyszedl albo przelaczyl profil — przestajemy mu zakladac model tej postaci. */
  public void forget(Player player) {
    activeCharacters.remove(player.getUniqueId());
  }

  /** Uzupelnia wlasciciela w rekordach ze schematu 1 — jedyny moment, gdy go znamy. */
  private void backfillOwner(CharacterRecord record, Player player) {
    if (record.hasOwner() && player.getName().equals(record.ownerName())) {
      return;
    }
    store.put(new CharacterRecord(record.characterId(), record.name(), record.raceId(),
        record.classId(), record.skinSource(), player.getUniqueId(), player.getName(),
        record.createdAt()));
  }

  @EventHandler
  public void onUnload(ProfileUnloadEvent event) {
    forget(event.getPlayer());
    // Same docs page: "When a profile is unselected, profile data is unloaded and saved
    // asynchronously, after which #validate is called." Our own data jest utrwalana przez
    // CharacterStore (atomowo, z debounce), wiec nie ma tu czego dosylac.
    event.validate(this);
  }

  /**
   * Usuniecie postaci.
   *
   * <p><b>Brakowalo tu {@code event.validate(this)}</b> — i to byla czynna sciezka utraty
   * danych, nie usterka kosmetyczna. {@code ProfileRemoveEvent} dziedziczy po
   * {@code PendingProfileEvent}, czyli ma ten sam mechanizm blokujacy co
   * {@code ProfileCreateEvent}: dopoki modul nie zwolni eventu, MMOProfiles nie konczy
   * usuwania. Skutek byl taki, ze rekord znikal z characters.yml, profil zostawal — na
   * zawsze, bo kreator wisi na {@code ProfileCreateEvent}, a ten profil juz istnial.
   * Na produkcji wyprodukowalo to 9 takich profili na 19 zywych. Javadoc tej klasy sam
   * wymienia CZTERY eventy do zwalidowania i ten byl jedynym pominietym.
   *
   * <p>{@code validate} jest w {@code finally}: gdyby zapis rzucil wyjatkiem, profil i tak
   * nie ma prawa zostac zawieszony. Kopia zapasowa idzie PRZED skasowaniem rekordu —
   * dotad powstawala tylko przed {@code /cc delete} i {@code /cc prune}, wiec usuniecie
   * postaci z GUI bylo jedyna operacja kasujaca dane bez siatki bezpieczenstwa.
   */
  @EventHandler
  public void onRemove(ProfileRemoveEvent event) {
    try {
      forget(event.getPlayer());
      store.backupNow("remove");
      store.remove(event.getProfile().getUniqueId());
    } catch (RuntimeException ex) {
      logger.log(Level.SEVERE, "Awaria przy usuwaniu postaci (profil "
          + event.getProfile().getUniqueId() + ")", ex);
    } finally {
      event.validate(this);
    }
  }
}
