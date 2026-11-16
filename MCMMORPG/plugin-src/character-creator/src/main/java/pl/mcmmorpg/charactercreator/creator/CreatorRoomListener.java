package pl.mcmmorpg.charactercreator.creator;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import pl.mcmmorpg.charactercreator.CreatorConfig;
import pl.mcmmorpg.charactercreator.persist.CharacterStore;

/**
 * Handles the creator room's interaction: a real inventory GUI (CreatorMenu), clicked with
 * the mouse cursor like any vanilla chest menu — DeluxeMenus/CMI-style, the same pattern
 * Wynncraft uses for its class-select screen, since a vanilla client has no other way to
 * point-and-click a 2D button.
 *
 * <p><b>Izolacja, a nie zaslona.</b> Wczesniej caly listener mial TRZY handlery (quit, click,
 * close), a ochrona kreatora opierala sie wylacznie na tym, ze otwarte inventory blokuje ruch
 * klienta. To nie jest izolacja — kazdy {@code /tp}, {@code /back}, respawn MMOCore czy
 * kolejka MythicDungeons wyrywal gracza z sali, a sesja zyla dalej i zaczynala czyscic mu
 * plecak w normalnym swiecie. Teraz zamkniete sa wszystkie cztery drogi: teleport, komenda,
 * czat i rozjazd swiata (ten ostatni w straznika, patrz CreatorSessionManager#tick).
 */
public final class CreatorRoomListener implements Listener {

  /** Everything MMOProfiles-specific about finishing creation lives behind this seam. */
  public interface ConfirmHandler {
    void onConfirm(Player player, CreatorSession session);
  }

  /**
   * Druga strona tego samego szwu: rezygnacja z tworzenia postaci.
   *
   * <p>Osobny interfejs, a nie druga metoda w {@link ConfirmHandler}, bo to sa dwa rozne
   * kontrakty wobec MMOProfiles — jeden konczy zawieszony {@code ProfileCreateEvent}
   * zatwierdzeniem, drugi porzuca go bez zatwierdzenia.
   */
  public interface CancelHandler {
    void onCancel(Player player, CreatorSession session);
  }

  private final Plugin plugin;
  private final CreatorConfig config;
  private final CreatorSessionManager sessions;
  private final PreviewDummyManager dummies;
  private final CreatorMenu menu;
  private final NameInputDialog nameInputDialog;
  private final CharacterStore store;
  private final ConfirmHandler confirmHandler;
  private final CancelHandler cancelHandler;

  public CreatorRoomListener(Plugin plugin, CreatorConfig config, CreatorSessionManager sessions,
      PreviewDummyManager dummies, CreatorMenu menu, NameInputDialog nameInputDialog,
      CharacterStore store, ConfirmHandler confirmHandler, CancelHandler cancelHandler) {
    this.plugin = plugin;
    this.config = config;
    this.sessions = sessions;
    this.dummies = dummies;
    this.menu = menu;
    this.nameInputDialog = nameInputDialog;
    this.store = store;
    this.confirmHandler = confirmHandler;
    this.cancelHandler = cancelHandler;
  }

  /**
   * LOWEST celowo. MMOProfiles ma {@code synced-data.inventory: true} i {@code game-mode:
   * true}, a kolejnosc handlerow {@link PlayerQuitEvent} miedzy pluginami jest
   * niedeterministyczna. Bez wymuszonego pierwszenstwa mozliwe bylo zapisanie do profilu
   * trybu SPECTATOR i PUSTEGO ekwipunku — na stale. Konczymy sesje i oddajemy rzeczy, zanim
   * ktokolwiek zdazy je odczytac.
   */
  @EventHandler(priority = EventPriority.LOWEST)
  public void onQuit(PlayerQuitEvent event) {
    if (sessions.isActive(event.getPlayer())) {
      sessions.end(event.getPlayer(), CreatorSessionManager.EndReason.DISCONNECT);
    }
  }

  /** Gracz, ktory wypadl w trakcie kreatora, dostaje swoje rzeczy z powrotem. */
  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    sessions.restoreAfterJoin(event.getPlayer());
  }

  /**
   * Zadnego wyjscia z sali w trakcie sesji. Blokujemy tylko cel POZA swiatem kreatora —
   * nasze wlasne teleporty (powrot kamery po Escape) zostaja w {@code creator_limbo},
   * a teleport wyjsciowy leci juz po zakonczeniu sesji.
   */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onTeleport(PlayerTeleportEvent event) {
    if (!sessions.isActive(event.getPlayer())) {
      return;
    }
    Location to = event.getTo();
    if (to.getWorld() != null && to.getWorld().getName().equals(config.roomWorldName())) {
      return;
    }
    event.setCancelled(true);
    feedback(event.getPlayer(), "Najpierw dokończ tworzenie postaci.", true);
  }

  /**
   * W kreatorze nie dziala zadna komenda POZA nasza wlasna.
   *
   * <p>Whitelist byla dotad pusta, co obejmowalo rowniez {@code /cc} — czyli jedyne
   * narzedzie, ktorym admin moglby sie z sali wydostac albo obejrzec stan sesji. Admin,
   * ktory tam wpadl, nie mial wyjscia innego niz relog. Komenda i tak sprawdza wlasna
   * permisje, wiec zwyklemu graczowi nic to nie daje.
   */
  private static final java.util.Set<String> ALLOWED_COMMANDS =
      java.util.Set.of("/cc", "/charactercreator");

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    if (!sessions.isActive(event.getPlayer())) {
      return;
    }
    String root = event.getMessage().split(" ", 2)[0].toLowerCase(java.util.Locale.ROOT);
    if (ALLOWED_COMMANDS.contains(root)) {
      return;
    }
    event.setCancelled(true);
    feedback(event.getPlayer(), "Komendy są wyłączone w kreatorze postaci.", true);
  }

  /**
   * Czat renderuje sie NA scenie i pisal postaci po torsie — widoczne na zrzutach z klienta.
   * PLAN-v3 oznaczal to jako zrobione, ale w kodzie nie bylo ZADNEGO listenera czatu; usunieto
   * wtedy jedynie wlasna wiadomosc powitalna. Tu zamykamy oba kierunki: gracz w kreatorze nie
   * pisze i nie widzi cudzych wiadomosci.
   */
  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(AsyncChatEvent event) {
    if (sessions.isActive(event.getPlayer())) {
      event.setCancelled(true);
      // AsyncChatEvent leci POZA watkiem glownym, wiec bez dzwieku: sam actionbar jest
      // bezpieczny do wyslania asynchronicznie, playSound juz nie.
      event.getPlayer().sendActionBar(
          Component.text("Czat jest wyciszony w kreatorze postaci.", NamedTextColor.RED));
      return;
    }
    try {
      event.viewers().removeIf(audience ->
          audience instanceof Player viewer && sessions.isActive(viewer));
    } catch (UnsupportedOperationException ex) {
      // Inny plugin podmienil zbior odbiorcow na niemodyfikowalny. Nie mamy jak go
      // przyciac — gorzej wyglada, ale nic sie nie psuje.
    }
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    CreatorSession session = sessions.get(player);
    if (session == null || !(event.getInventory().getHolder() instanceof CreatorMenu.Holder)) {
      return;
    }
    // Always cancel: this GUI is display-only buttons, never a real item transfer, and
    // this also blocks shift-click/hotbar-swap from pulling a filler item into the
    // player's real inventory.
    event.setCancelled(true);

    // Po kliknieciu "STWORZ POSTAC" ekran jest juz tylko ceremonia — zadne klikniecie nie
    // moze go cofnac ani uruchomic drugi raz.
    if (session.confirming()) {
      return;
    }

    String action = CreatorMenu.actionOf(event.getRawSlot());
    if (action == null) {
      return;
    }

    if (action.startsWith("race_")) {
      session.selectRace(Integer.parseInt(action.substring(5)));
      refreshAppearance(session, player);
      click(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f);
    } else if (action.startsWith("class_")) {
      session.selectClass(Integer.parseInt(action.substring(6)));
      refreshAppearance(session, player);
      click(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f);
    } else {
      switch (action) {
        case "set_name" -> {
          if (event.isRightClick()) {
            applyName(player, session, randomName(session));
            menu.repaint(player, session);
            return;
          }
          // Dialog zastepuje ekran kreatora; po zamknieciu otwieramy menu z powrotem.
          session.markDialogOpen();
          nameInputDialog.open(player,
              session.hasName() ? session.pendingName() : player.getName(),
              raw -> {
                session.clearDialog();
                NameRules.Result result = NameRules.validate(raw);
                if (result.ok()) {
                  applyName(player, session, result.name());
                } else {
                  feedback(player, result.rejection().message(), true);
                }
                reopenMenuNextTick(player, session);
              },
              () -> {
                session.clearDialog();
                reopenMenuNextTick(player, session);
              });
          return;
        }
        case "confirm" -> {
          tryConfirm(player, session);
          return;
        }
        case "exit" -> {
          // Bez ceremonii i bez pytania "na pewno?". Przycisk istnieje po to, zeby wyjscie
          // bylo jednym klikiem — potwierdzenie zamienialoby go w to samo kombinowanie,
          // ktore mial usunac. Ryzykiem jest porzucony wybor rasy i klasy, a nie dane.
          click(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f);
          cancelHandler.onCancel(player, session);
          return;
        }
        default -> {
          return;
        }
      }
    }
    menu.repaint(player, session);
  }

  /** Ustawia imie po sprawdzeniu unikalnosci — inaczej mielismy 12 postaci "Grimtest". */
  private void applyName(Player player, CreatorSession session, String name) {
    if (store.isNameTaken(name, session.profileId())) {
      feedback(player, NameRules.Rejection.DUPLICATE.message(), true);
      return;
    }
    session.setPendingName(name);
    click(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.6f);
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player)) {
      return;
    }
    if (!(event.getInventory().getHolder() instanceof CreatorMenu.Holder)) {
      return;
    }
    CreatorSession session = sessions.get(player);
    if (session == null || session.confirming()) {
      return;
    }
    // Only reopen when the PLAYER closed it themselves (Esc/E, e.g. by accident) — the
    // OPEN_NEW reason covers our own nameInputDialog.open() swapping the screen out from
    // under this one, and PLUGIN/TELEPORT cover sessions.end() closing it deliberately on
    // confirm/quit. Reopening on those would fight our own code instead of just the
    // player's own accidental close.
    if (event.getReason() == InventoryCloseEvent.Reason.PLAYER) {
      reopenMenuNextTick(player, session);
    }
  }

  /**
   * Paper/Spigot don't reliably support opening a new inventory from directly inside an
   * InventoryCloseEvent (or an event fired from within one, like the name dialog callback
   * here) — deferring one tick is the documented-safe way to swap screens.
   */
  private void reopenMenuNextTick(Player player, CreatorSession session) {
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (!sessions.isActive(player) || session.confirming()) {
        return;
      }
      // Przytrzymanie Escape zamyka okno na jeden tick, zanim je odtworzymy — gracz jest
      // w SPECTATOR, wiec zdazy rozejrzec sie po sali i zobaczyc jej kulisy. Zamknac sie
      // tego nie da (vanilla zawsze pozwala zamknac ekran), ale mozna cofnac skutek:
      // wracamy kamera na dokladna pozycje i rotacje startowa razem z otwarciem menu.
      player.teleport(config.roomSpawn());
      session.setMenuInventory(menu.open(player, session));
    });
  }

  /**
   * Kreator byl kiedys CALKOWICIE NIEMY — w calym pluginie nie bylo ani jednego
   * playSound. Interfejs bez sprzezenia dzwiekowego czyta sie jak martwy, niezaleznie
   * od tego, jak wyglada. Uzywamy wylacznie dzwiekow vanilla, wiec bez zadnych assetow.
   */
  private void click(Player player, Sound sound, float pitch) {
    player.playSound(player.getLocation(), sound, 0.7f, pitch);
  }

  /**
   * Komunikaty ida na ACTIONBAR, nie na czat.
   *
   * <p>Powod jest banalny i byl przeoczony przez trzy wersje planu: ekran kreatora zaslania
   * czat w calosci, wiec {@code sendMessage} z powodem odrzucenia imienia byl dla gracza
   * NIEWIDOCZNY. Actionbar rysuje sie nad hotbarem, ponad otwartym GUI.
   */
  private void feedback(Player player, String message, boolean error) {
    player.sendActionBar(Component.text(message,
        error ? NamedTextColor.RED : NamedTextColor.GREEN));
    if (error) {
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
    }
  }

  /**
   * Przebiera dummy'ego po zmianie rasy lub klasy — wybor ma byc widoczny na postaci.
   *
   * <p>Wyboru NIE przebieramy juz na jednej encji: kazda skorka ma wlasnego nosiciela,
   * a klikniecie tylko wystawia wlasciwego na kotwice. Przebieranie w locie oznaczalo
   * skasowanie i odtworzenie encji u klienta, czyli migniecie skorka domyslna — patrz
   * PreviewDummyManager.
   */
  private void refreshAppearance(CreatorSession session, Player viewer) {
    CreatorConfig.RaceOption race = config.races().get(session.currentRaceId());
    CreatorConfig.ClassOption clazz = config.classes().get(session.currentClassId());
    var shown = dummies.select(viewer, race, clazz);
    if (shown == null) {
      return;
    }
    // Podgladem jest od teraz TEN nosiciel — rotacja i ceremonia musza wiedziec ktory.
    session.setPreviewDummy(shown);
    dummies.applyAppearance(viewer, race, clazz);
  }

  /** Krotka ceremonia zamiast natychmiastowego znikniecia — moment ma byc zauwazalny. */
  private void celebrate(Player player, CreatorSession session, Runnable then) {
    player.closeInventory();
    var dummy = session.previewDummy();
    Location at = dummy != null && !dummy.isDead()
        ? dummy.getLocation().add(0, 1.0, 0) : player.getLocation();
    player.playSound(at, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
    player.spawnParticle(Particle.END_ROD, at, 60, 0.5, 0.8, 0.5, 0.08);
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!player.isOnline()) {
        return;
      }
      player.playSound(at, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
      player.spawnParticle(Particle.TOTEM_OF_UNDYING, at, 80, 0.5, 0.9, 0.5, 0.35);
    }, 20L);
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      // Ceremonia trwa 2.25 s. Bez tej straznicy rozlaczenie w jej trakcie konczylo sie
      // zapisem rekordu, komendami konsolowymi i teleportem na obiekcie gracza, ktorego
      // juz nie ma.
      if (!player.isOnline()) {
        return;
      }
      player.playSound(at, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
      then.run();
    }, 45L);
  }

  private static final String[] NAME_PREFIX = {"Aer", "Bran", "Cor", "Dra", "El", "Fen",
      "Gor", "Hal", "Ith", "Kar", "Lor", "Mor", "Nar", "Oth", "Ral", "Syl"};
  private static final String[] NAME_SUFFIX = {"ion", "wyn", "dor", "ath", "iel", "mir",
      "ruk", "gard", "essa", "thas", "vyn", "orn"};

  /**
   * Losowe imie, ktore jest WOLNE.
   *
   * <p>Pula ma 16 x 12 = 192 kombinacje i to jest caly problem: postaci na serwerze
   * przybywa, a poprzednia wersja losowala jedna kombinacje i oddawala ja bez pytania.
   * Przy 55 zajetych imionach co trzecie klikniecie "PPM — losuj" trafialo w zajete i nie
   * robilo NIC poza czerwonym komunikatem — gracz klikal do skutku, nie wiedzac dlaczego.
   * (Tak samo wygladalo to w evalach: scenariusz konczyl sie "brak komunikatu o utworzeniu",
   * bo przycisk STWORZ zostawal w stanie "Najpierw ustaw imie".)
   *
   * <p>Losujemy wiec do skutku, a gdy pula sie wyczerpie — doklejamy liczbe, zamiast
   * oddawac cokolwiek. {@link #applyName} i tak sprawdza zajetosc jeszcze raz: miedzy
   * losowaniem a klinieciem "STWORZ" imie moze zajac ktos inny.
   */
  private String randomName(CreatorSession session) {
    var rnd = java.util.concurrent.ThreadLocalRandom.current();
    for (int attempt = 0; attempt < 64; attempt++) {
      String candidate = NAME_PREFIX[rnd.nextInt(NAME_PREFIX.length)]
          + NAME_SUFFIX[rnd.nextInt(NAME_SUFFIX.length)];
      if (!store.isNameTaken(candidate, session.profileId())) {
        return candidate;
      }
    }
    String base = NAME_PREFIX[rnd.nextInt(NAME_PREFIX.length)]
        + NAME_SUFFIX[rnd.nextInt(NAME_SUFFIX.length)];
    for (int number = 2; number < 1000; number++) {
      String candidate = base + number;
      if (!store.isNameTaken(candidate, session.profileId())) {
        return candidate;
      }
    }
    // Nie do osiagniecia przy jakiejkolwiek realnej liczbie postaci — ale niech oddaje
    // cos, co przejdzie walidacje, zamiast null.
    return base + java.util.UUID.randomUUID().toString().substring(0, 4);
  }

  private void tryConfirm(Player player, CreatorSession session) {
    if (session.confirming()) {
      return;
    }
    if (!session.hasName()) {
      feedback(player, "Najpierw ustaw imię postaci.", true);
      menu.repaint(player, session);
      return;
    }
    if (store.isNameTaken(session.pendingName(), session.profileId())) {
      // Ktos mogl zajac to imie miedzy jego wpisaniem a kliknieciem "STWORZ".
      session.setPendingName(null);
      feedback(player, NameRules.Rejection.DUPLICATE.message(), true);
      menu.repaint(player, session);
      return;
    }
    // Od tego momentu ekran jest tylko ceremonia: drugie klikniecie nic nie robi, a straznik
    // nie odtwarza menu z klikalnym przyciskiem.
    session.markConfirming();
    // Sesja konczy sie DOPIERO po ceremonii — inaczej dummy znika, zanim cokolwiek widac.
    celebrate(player, session, () -> {
      // false: teleportem wyjsciowym zarzadza handler commitu, bo zalezy on od tego,
      // czy commit sie udal.
      sessions.end(player, CreatorSessionManager.EndReason.CONFIRMED);
      confirmHandler.onConfirm(player, session);
    });
  }
}
