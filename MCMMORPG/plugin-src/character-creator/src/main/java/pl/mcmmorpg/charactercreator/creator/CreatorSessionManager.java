package pl.mcmmorpg.charactercreator.creator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.mcmmorpg.charactercreator.CreatorConfig;

/**
 * Wlasciciel zywych sesji kreatora.
 *
 * <p><b>Klucz to UUID, nie obiekt {@code Player}.</b> Stara mapa trzymala silne referencje do
 * {@code CraftPlayer}: po reconnekcie gracz mial NOWY obiekt, wiec straznik iterowal po
 * martwych instancjach, dummy nigdy nie znikal, a sesja nie dawala sie zakonczyc.
 *
 * <p><b>Stan wejscia jest utrwalany na dysku</b> ({@link SessionStateStore}) przed jakakolwiek
 * modyfikacja ekwipunku, i kasowany dopiero po udanym oddaniu rzeczy.
 */
public final class CreatorSessionManager {

  /**
   * Dlaczego sesja sie konczy. Rozroznienie jest istotne w dwoch miejscach naraz:
   * czy teleportowac gracza i czy skasowac jego stan wejscia z dysku.
   */
  public enum EndReason {
    /**
     * Postac utworzona. Teleportem zarzadza handler commitu; stan wejscia juz niepotrzebny.
     *
     * <p><b>Ekwipunku NIE oddajemy tutaj</b> — robi to {@code CharacterProfileModule#onConfirm}
     * dopiero PO zatwierdzeniu profilu. Powod zmierzony evalem przeciwko zywemu serwerowi
     * (scenariusz 40-ekwipunek): oddanie rzeczy w tym miejscu bylo bezuzyteczne, bo zaraz
     * potem {@code event.validate()} pozwala MMOProfiles naloz na gracza ekwipunek SWIEZEGO
     * profilu ({@code synced-data.inventory: true}), czyli pusty — i kasuje to, co wlasnie
     * oddalismy. Bot wchodzil ze 100 przedmiotami (stone x70, oak_planks x30), a po
     * utworzeniu postaci mial zero.
     */
    CONFIRMED(false, true, false),
    /** Przerwane w locie (blad configu, odmowa commitu). Odsylamy gracza i sprzatamy. */
    ABORTED(true, true, true),
    /**
     * Gracz sam kliknal "Wyjdz". Skutki sa te same co przy {@link #ABORTED} — odeslanie,
     * oddanie rzeczy, skasowanie stanu z dysku — ale powod ma wlasna nazwe, bo w logu to
     * dwie zupelnie rozne historie: awaria kontra normalna rezygnacja.
     *
     * <p>Zawieszonego {@code ProfileCreateEvent} NIE zatwierdzamy — patrz
     * {@code CharacterProfileModule#onCancel}.
     */
    CANCELLED(true, true, true),
    /**
     * Gracza wyrwalo z sali (dungeon queue, respawn, cudzy teleport). Konczymy sesje
     * i oddajemy rzeczy, ale NIE teleportujemy: gracz jest juz tam, gdzie go przeniesiono,
     * a ciagniecie go z powrotem na pozycje wejsciowa byloby walka z tym, co go przeniosło.
     */
    DISPLACED(false, true, true),
    /**
     * Gracz wyszedl z serwera albo serwer sie zatrzymuje. NIE teleportujemy (teleport
     * w trakcie rozlaczania i tak nie jest wiarygodny) i — co wazniejsze — NIE kasujemy
     * wpisu w sessions.yml: jesli oddanie rzeczy tutaj z jakiegokolwiek powodu nie
     * zostanie utrwalone, przywroci je {@link #restoreAfterJoin} przy nastepnym wejsciu.
     * Powtorne przywrocenie jest idempotentne, utrata plecaka nie.
     */
    DISCONNECT(false, false, true);

    private final boolean teleport;
    private final boolean clearPersistedState;
    private final boolean restoreInventory;

    EndReason(boolean teleport, boolean clearPersistedState, boolean restoreInventory) {
      this.teleport = teleport;
      this.clearPersistedState = clearPersistedState;
      this.restoreInventory = restoreInventory;
    }
  }

  /** Straznik nie probuje otwierac menu czesciej niz raz na sekunde. */
  private static final long MENU_REOPEN_COOLDOWN_MILLIS = 1000L;

  /** Odstep, po ktorym przywracamy stan po restarcie — patrz {@link #restoreAfterJoin}. */
  private static final long JOIN_RESTORE_DELAY_TICKS = 40L;

  private final Map<UUID, CreatorSession> sessions = new ConcurrentHashMap<>();
  private final Plugin plugin;
  private final CreatorConfig config;
  private final PreviewDummyManager dummies;
  private final CreatorMenu menu;
  private final SessionStateStore stateStore;
  private final Logger logger;

  public CreatorSessionManager(Plugin plugin, CreatorConfig config, PreviewDummyManager dummies,
      CreatorMenu menu, SessionStateStore stateStore, Logger logger) {
    this.plugin = plugin;
    this.config = config;
    this.dummies = dummies;
    this.menu = menu;
    this.stateStore = stateStore;
    this.logger = logger;
  }

  /**
   * Otwiera sesje kreatora. Zwraca {@code null}, gdy gracz JUZ jest w kreatorze.
   *
   * <p>Odmowa drugiej sesji jest krytyczna, a nie kosmetyczna: stary kod nie sprawdzal nic,
   * wiec drugie wejscie zapisywalo do stashu ekwipunek JUZ wyczyszczony przez pierwsze —
   * i {@code end()} oddawalo graczowi pustke. To byla trwala utrata itemow plus wyciek
   * starego dummy'ego.
   */
  /**
   * @param origin gdzie gracz stal, ZANIM cokolwiek go ruszylo. Przekazywany z zewnatrz,
   *               a nie odczytywany tutaj, bo wolajacy teleportuje gracza do sali przed
   *               otwarciem sesji: {@code player.getLocation()} w tym miejscu zwracalo juz
   *               spawn kreatora. Skutek byl widoczny w danych — oba wpisy w sessions.yml
   *               na produkcji mialy {@code origin.world: creator_limbo}, wiec cala galaz
   *               "odeslij gracza tam, skad przyszedl" w {@link #exitLocation} byla martwa.
   */
  public CreatorSession start(Player player, UUID profileId, PendingCreation pendingCreation,
      Location origin) {
    UUID playerId = player.getUniqueId();
    if (sessions.containsKey(playerId)) {
      logger.severe("Odmowa drugiej sesji kreatora dla " + player.getName() + " (profil "
          + profileId + "): gracz jest juz w kreatorze dla profilu "
          + sessions.get(playerId).profileId() + ". Ekwipunek NIE zostal ruszony.");
      return null;
    }
    List<String> raceIds = List.copyOf(config.races().keySet());
    List<String> classIds = List.copyOf(config.classes().keySet());
    if (raceIds.isEmpty() || classIds.isEmpty()) {
      throw new IllegalStateException("No races/classes configured — check config.yml");
    }

    // KOLEJNOSC MA ZNACZENIE: stan wejscia laduje na dysku ZANIM cokolwiek zmienimy.
    EntryState entry = captureEntryState(player, origin);
    stateStore.put(entry);

    CreatorSession session = new CreatorSession(playerId, profileId, pendingCreation, raceIds, classIds);
    session.setEntryState(entry);
    sessions.put(playerId, session);

    var race = config.races().get(session.currentRaceId());
    var clazz = config.classes().get(session.currentClassId());
    // Stajnia nosicieli: po jednym na skorke, wszyscy zywi do konca sesji. Zmiana rasy albo
    // klasy jest wtedy teleportem, a nie tworzeniem nowej encji — patrz PreviewDummyManager.
    var dummy = dummies.start(config.dummyAnchor(), player, race, clazz);
    session.setPreviewDummy(dummy);
    dummies.applyAppearance(player, race, clazz);
    hideFromOthers(player, dummies.carriers(playerId));
    // SPECTATOR, nie ADVENTURE. Powod ustalony zrzutami z prawdziwego klienta: na tym
    // serwerze vanillowy HUD (serduszka, glod, hotbar) renderuje sie MIMO otwartego ekranu
    // kontenera — widac to juz na czystym zrzucie z shulkera, bez naszego UI, wiec nie jest
    // to skutek kreatora. MythicHUD tego nie rysuje (sprawdzone: z pustym layout.default
    // HUD zostaje), a jego komenda `layout ... remove` i tak odmawia zdjecia layoutow
    // domyslnych. Spectator usuwa caly HUD jednym ruchem i — co potwierdzone klikiem w
    // zywym kliencie — NIE blokuje obslugi klikniec w otwartym inventory.
    // Spectator nie ma tez grawitacji, wiec nie trzeba jej wylaczac osobno, a gracz jest
    // niewidoczny dla innych.
    player.setGameMode(GameMode.SPECTATOR);
    // NOCNE WIDZENIE — jedyny sposob, zeby postac bylo widac przez przyciemnienie.
    //
    // Ekran kontenera przyciemnia SWIAT za soba mniej wiecej o polowe, a UI rysuje sie PO
    // przyciemnieniu. Zmierzone wczesniej w tym module: sufit jasnosci sceny to ~0.26, wiec
    // interfejs z definicji dorownywal bohaterowi. Samego przyciemnienia zdjac sie nie da —
    // to fillGradient z wartosciami zaszytymi w kliencie, resource pack tego nie widzi
    // (sprawdzone pomiarem: nadpisanie gui/*_background.png nie zmienilo ani jednego piksela).
    // Ale przyciemnienie MNOZY jasnosc swiata, wiec podniesienie tej jasnosci u zrodla
    // przechodzi przez nie w calosci. Nocne widzenie podnosi ja do maksimum niezaleznie od
    // swiatla blokowego, wiec dziala nawet gdyby sala byla nieoswietlona.
    // ambient + bez czastek i bez ikony: gracz ma widziec scene, a nie efekt mikstury.
    player.addPotionEffect(new PotionEffect(
        PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, true, false, false));
    // A 9-slot top inventory always renders the player's OWN inventory underneath it in the
    // same window (fundamental Bukkit/vanilla behavior, no way around it while a real
    // inventory stays populated) — a live test showed a fresh player's starter-kit items
    // (stone, oak_planks) visibly cluttering the bottom of the "character creator" screen,
    // which reads as unpolished/unfinished rather than a dedicated creator UI. Hiding their
    // real inventory for the duration (restored on exit) keeps only our own GUI visible.
    player.getInventory().setStorageContents(new ItemStack[36]);
    session.setMenuInventory(menu.open(player, session));
    return session;
  }

  /**
   * Chowa nosicieli przed WSZYSTKIMI poza tworzacym graczem.
   *
   * <p>Przebranie LibsDisguises jest nakladane tylko dla niego ({@code disguiseToPlayers}),
   * wiec kazdy inny gracz w sali widzi goly nosiciel — zombie. Przy dwoch graczach naraz
   * (scenariusz 50-dwoch-graczy) oznaczalo to szereg zombie stojacy w cudzym kadrze.
   */
  private void hideFromOthers(Player owner, List<org.bukkit.entity.Entity> entities) {
    for (Player other : Bukkit.getOnlinePlayers()) {
      if (other.equals(owner)) {
        continue;
      }
      for (org.bukkit.entity.Entity entity : entities) {
        other.hideEntity(plugin, entity);
      }
    }
  }

  private EntryState captureEntryState(Player player, Location origin) {
    PlayerInventory inventory = player.getInventory();
    // Kopie GLEBOKIE: tablica z getStorageContents() trzyma opakowania wskazujace na zywe
    // stosy serwera, a zaraz potem czyscimy ekwipunek. Snapshot ma byc wartoscia, nie widokiem.
    return new EntryState(player.getUniqueId(),
        deepCopy(inventory.getStorageContents()),
        deepCopy(inventory.getArmorContents()),
        inventory.getItemInOffHand() == null ? null : inventory.getItemInOffHand().clone(),
        player.getGameMode(),
        origin == null ? player.getLocation().clone() : origin.clone(),
        System.currentTimeMillis());
  }

  private static ItemStack[] deepCopy(ItemStack[] source) {
    ItemStack[] out = new ItemStack[source.length];
    for (int i = 0; i < source.length; i++) {
      out[i] = source[i] == null ? null : source[i].clone();
    }
    return out;
  }

  public CreatorSession get(Player player) {
    return player == null ? null : sessions.get(player.getUniqueId());
  }

  public CreatorSession get(UUID playerId) {
    return sessions.get(playerId);
  }

  public boolean isActive(Player player) {
    return player != null && sessions.containsKey(player.getUniqueId());
  }

  public boolean isActive(UUID playerId) {
    return sessions.containsKey(playerId);
  }

  public int activeCount() {
    return sessions.size();
  }

  /**
   * Konczy sesje i ODDAJE graczowi wszystko, co kreator wzial: ekwipunek, zbroje, offhand
   * i tryb gry sprzed wejscia.
   */
  public void end(Player player, EndReason reason) {
    if (player == null) {
      return;
    }
    CreatorSession session = sessions.remove(player.getUniqueId());
    if (session == null) {
      return;
    }
    dummies.despawnAll(player.getUniqueId());
    // Nocne widzenie nalezy do kreatora i konczy sie razem z nim — inaczej gracz wychodzi
    // w swiat z widzeniem w ciemnosci i nie wie, skad je ma.
    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
    if (player.getOpenInventory().getTopInventory().getHolder() instanceof CreatorMenu.Holder) {
      player.closeInventory();
    }
    EntryState entry = session.entryState();
    if (entry == null) {
      // Nie powinno sie zdarzyc — start() zawsze go ustawia — ale gdyby, to lepiej sprobowac
      // odzyskac zapis z dysku niz zostawic gracza z pustym plecakiem.
      entry = stateStore.get(player.getUniqueId());
    }
    if (entry != null) {
      if (reason.restoreInventory) {
        restoreInventory(player, entry);
        returnOverflow(player, session);
      }
      // Admin wchodzacy z CREATIVE musi wyjsc w CREATIVE. Stara wersja robila
      // "jesli SPECTATOR to SURVIVAL" i wypychala kazdego do survivalu.
      player.setGameMode(entry.gameMode() == null ? GameMode.SURVIVAL : entry.gameMode());
      if (reason.teleport) {
        player.teleport(exitLocation(entry));
      }
    } else if (player.getGameMode() == GameMode.SPECTATOR) {
      player.setGameMode(GameMode.SURVIVAL);
    }
    if (reason.clearPersistedState) {
      stateStore.clear(player.getUniqueId());
    }
  }

  /**
   * Oddaje przedmioty, ktore sweeper zabral, a ktore nie zmiescily sie w snapshocie
   * (plecak byl juz pelny przy wejsciu). Co sie nie miesci TERAZ, ladzie pod nogami —
   * nigdy nie jest kasowane.
   */
  private static void returnOverflow(Player player, CreatorSession session) {
    List<ItemStack> overflow = session.takeOverflow();
    if (overflow.isEmpty()) {
      return;
    }
    Map<Integer, ItemStack> rejected =
        player.getInventory().addItem(overflow.toArray(new ItemStack[0]));
    for (ItemStack leftover : rejected.values()) {
      player.getWorld().dropItemNaturally(player.getLocation(), leftover);
    }
  }

  private static void restoreInventory(Player player, EntryState entry) {
    PlayerInventory inventory = player.getInventory();
    if (entry.inventory() != null) {
      inventory.setStorageContents(entry.inventory());
    }
    if (entry.armor() != null) {
      inventory.setArmorContents(entry.armor());
    }
    inventory.setItemInOffHand(entry.offHand());
  }

  /**
   * Miejsce wyjscia: tam, skad gracz przyszedl — chyba ze to byl sam kreator.
   *
   * <p>Gdy pochodzenia nie znamy (nowy gracz, ktory zaczal w sali), schodzimy na
   * {@code creator-room.exit} z configu, a dopiero w ostatniej kolejnosci na spawn swiata
   * glownego. Ta ostatnia galaz byla wczesniej JEDYNA — i to na niej konto testowe ginelo
   * od mobow w dwoch niezaleznych sesjach QA.
   */
  Location exitLocation(EntryState entry) {
    Location origin = entry == null ? null : entry.origin();
    if (origin != null && origin.getWorld() != null
        && !origin.getWorld().getName().equals(config.roomWorldName())) {
      return origin;
    }
    return configuredExit();
  }

  /** Skonfigurowane wyjscie albo spawn swiata glownego, gdy configu nie ma. */
  public Location configuredExit() {
    Location exit = config.exitLocation();
    return exit != null ? exit : Bukkit.getWorlds().get(0).getSpawnLocation();
  }

  /**
   * Konczy WSZYSTKIE sesje — wolane z onDisable.
   *
   * <p>Bez tego {@code /stop} z graczem w kreatorze oznaczal trwale skasowany plecak: stash
   * zyl wylacznie w RAM-ie, wiec wylaczenie serwera zabieralo go ze soba. To byl najdrozszy
   * pojedynczy blad w calym module.
   */
  public void endAll() {
    for (UUID playerId : new ArrayList<>(sessions.keySet())) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        end(player, EndReason.DISCONNECT);
      } else {
        // Gracza juz nie ma na serwerze — sprzataj to, co da sie sprzatnac bez niego.
        sessions.remove(playerId);
        dummies.despawnAll(playerId);
        // Wpis w sessions.yml ZOSTAJE celowo: to on odda graczowi rzeczy przy najblizszym
        // wejsciu na serwer.
      }
    }
  }

  /**
   * Przywraca stan po restarcie/rozlaczeniu w trakcie kreatora.
   *
   * <p>Opoznione o {@value #JOIN_RESTORE_DELAY_TICKS} tickow, bo MMOProfiles ma
   * {@code synced-data.inventory: true} i sam wgrywa ekwipunek profilu po zalogowaniu —
   * przywracanie przed nim zostaloby po prostu nadpisane.
   */
  public void restoreAfterJoin(Player player) {
    UUID playerId = player.getUniqueId();
    EntryState entry = stateStore.get(playerId);
    if (entry == null) {
      return;
    }
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      Player online = Bukkit.getPlayer(playerId);
      if (online == null || !online.isOnline()) {
        return;
      }
      CreatorSession session = sessions.get(playerId);
      if (session != null) {
        // Gracz zdazyl trafic do kreatora ponownie (MMOProfiles wystawil nowy
        // ProfileCreateEvent). Sesja zlapala ekwipunek JUZ po awarii, wiec prawdziwy stan
        // wejscia to ten z dysku — podmieniamy i nie ruszamy gracza.
        session.setEntryState(entry);
        logger.info("Przywrocono stan wejscia sprzed restartu do trwajacej sesji "
            + online.getName());
        return;
      }
      restoreInventory(online, entry);
      online.setGameMode(entry.gameMode() == null ? GameMode.SURVIVAL : entry.gameMode());
      if (online.getWorld().getName().equals(config.roomWorldName())) {
        online.teleport(exitLocation(entry));
      }
      stateStore.clear(playerId);
      online.sendMessage(Component.text(
          "Tworzenie postaci zostalo przerwane — twoje rzeczy wrocily. Sprobuj ponownie.",
          NamedTextColor.YELLOW));
      logger.info("Przywrocono ekwipunek sprzed kreatora graczowi " + online.getName());
    }, JOIN_RESTORE_DELAY_TICKS);
  }

  /**
   * Straznik sesji, chodzi co 10 tickow. Trzyma trzy rzeczy naraz, bo wszystkie trzy maja
   * ten sam warunek brzegowy — gracz MUSI byc w sali kreatora:
   * <ol>
   *   <li>rozjazd swiata konczy sesje (dawniej: eskalowal ja w normalnym swiecie, czyszczac
   *       graczowi plecak co 10 tickow),</li>
   *   <li>tryb SPECTATOR jest utrzymywany (cos w lancuchu MMOProfiles/MMOCore przywraca
   *       ADVENTURE juz PO naszym wywolaniu w {@link #start}),</li>
   *   <li>zgubione menu jest odtwarzane — ale tylko wtedy, gdy gracz NIE ma otwartego
   *       cudzego okna, i nie czesciej niz raz na sekunde.</li>
   * </ol>
   */
  public void tick() {
    for (Map.Entry<UUID, CreatorSession> entry : sessions.entrySet()) {
      Player player = Bukkit.getPlayer(entry.getKey());
      CreatorSession session = entry.getValue();
      if (player == null || !player.isOnline()) {
        continue;
      }
      if (!player.getWorld().getName().equals(config.roomWorldName())) {
        // Cokolwiek go stad wyrwalo (dungeon queue, /back, respawn), sesja nie ma juz sensu.
        // Konczymy JA, a nie ciagniemy gracza z powrotem: eskalacja w normalnym swiecie
        // oznaczala wymuszony SPECTATOR, otwierane menu i czyszczony ekwipunek co 10 tickow.
        logger.warning("Gracz " + player.getName() + " opuscil " + config.roomWorldName()
            + " w trakcie kreatora — sesja zakonczona, rzeczy oddane.");
        end(player, EndReason.DISPLACED);
        continue;
      }
      if (player.getGameMode() != GameMode.SPECTATOR) {
        player.setGameMode(GameMode.SPECTATOR);
      }
      reopenLostMenu(player, session);
      sweepInventory(player, session);
    }
  }

  /**
   * Gracz w kreatorze jest w SPECTATOR i bez HUD, wiec sesja bez otwartego GUI to gracz
   * uwieziony w pustej sali. Straznik to naprawia — ale NIE walczy z cudzym oknem:
   * bez tego rozroznienia DeluxeMenus/MythicLib/CMI dostawaly nasze menu na twarz co pol
   * sekundy, w nieskonczonosc.
   */
  private void reopenLostMenu(Player player, CreatorSession session) {
    if (session.awaitingDialog() || session.confirming()) {
      return;
    }
    if (player.getOpenInventory().getTopInventory().getHolder() instanceof CreatorMenu.Holder) {
      return;
    }
    // CRAFTING to widok "nic nie otwarte" (wlasny ekwipunek gracza). Cokolwiek innego
    // oznacza cudze GUI — zostawiamy je w spokoju.
    if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
      return;
    }
    if (!session.menuReopenAllowed(System.currentTimeMillis(), MENU_REOPEN_COOLDOWN_MILLIS)) {
      return;
    }
    session.setMenuInventory(menu.open(player, session));
  }

  /**
   * Some other plugin (kit/starter-item granting — no message logged, so not identified
   * further) re-populates a fresh player's real inventory on a short delay AFTER
   * ProfileCreateEvent fires, meaning the one-time clear in start() sometimes loses the
   * race: a live test showed stone/oak_planks reappearing under the creator GUI a few
   * seconds in despite that clear.
   *
   * <p>Zbroja i offhand (36..40) zostaja nietkniete: stary kod robil inv.clear(), co kasowalo
   * tez zbroje, mimo ze stash jej nie obejmowal.
   */
  private void sweepInventory(Player player, CreatorSession session) {
    var inv = player.getInventory();
    boolean dirty = false;
    for (int i = 0; i < 36; i++) {
      if (CreatorMenu.PLAYER_SLOTS_IN_USE.contains(i)) {
        continue;
      }
      ItemStack found = inv.getItem(i);
      if (found != null) {
        stash(session, found.clone());
        inv.setItem(i, null);
        dirty = true;
      }
    }
    if (dirty) {
      // Plecak zmienil sie po wejsciu do kreatora, wiec zapis na dysku jest juz nieaktualny.
      // Bez tego crash w trakcie sesji oddalby graczowi stan sprzed dosypanych przedmiotow.
      EntryState entry = session.entryState();
      if (entry != null) {
        stateStore.put(entry);
      }
      menu.render(session.menuInventory(), session, player);
    }
  }

  /**
   * Odklada przedmiot zabrany graczowi w trakcie kreatora.
   *
   * <p>Wczesniej sweeper po prostu KASOWAL wszystko, co pojawilo sie w plecaku po wejsciu
   * do sali. Snapshot ekwipunku powstaje raz, przy wejsciu, wiec kit startowy, {@code /give}
   * od admina czy nagroda z eventu nie trafialy do stashu — znikaly bez sladu. Komentarz nad
   * sweeperem sam opisywal ten wyscig; rozwiazaniem bylo skasowanie, nie zapisanie.
   *
   * <p>Najpierw probujemy wolnego slotu w snapshocie (tablica jest nasza, gleboka kopia —
   * mutowanie jej jest bezpieczne), a gdy plecak byl pelny juz przy wejsciu, przedmiot ladzie
   * na liscie nadmiarowej sesji i wraca przy wyjsciu.
   */
  private static void stash(CreatorSession session, ItemStack item) {
    EntryState entry = session.entryState();
    if (entry != null && entry.inventory() != null) {
      ItemStack[] slots = entry.inventory();
      for (int i = 0; i < slots.length; i++) {
        if (slots[i] == null) {
          slots[i] = item;
          return;
        }
      }
    }
    session.addOverflow(item);
  }

  /**
   * Oddaje ekwipunek sprzed kreatora — dla sciezki, ktora musi to zrobic PO zatwierdzeniu
   * profilu (patrz {@link EndReason#CONFIRMED}). Publiczne, bo wola to modul profilu.
   */
  public void restoreEntryInventory(Player player, CreatorSession session) {
    if (player == null || session == null) {
      return;
    }
    EntryState entry = session.entryState();
    if (entry == null) {
      entry = stateStore.get(player.getUniqueId());
    }
    if (entry != null) {
      restoreInventory(player, entry);
    }
    returnOverflow(player, session);
  }

  /** Zywe sesje — dla tasku obrotu podgladu i komend administracyjnych. */
  public Map<UUID, CreatorSession> activeSessions() {
    return Map.copyOf(sessions);
  }
}
