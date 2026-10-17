package pl.mcmmorpg.charactercreator;

import fr.phoenixdevt.mmoprofiles.bukkit.MMOProfiles;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.mcmmorpg.charactercreator.command.CreatorCommand;
import pl.mcmmorpg.charactercreator.creator.ConfirmService;
import pl.mcmmorpg.charactercreator.creator.CreatorMenu;
import pl.mcmmorpg.charactercreator.creator.CreatorRoomListener;
import pl.mcmmorpg.charactercreator.creator.CreatorSessionManager;
import pl.mcmmorpg.charactercreator.creator.NameInputDialog;
import pl.mcmmorpg.charactercreator.creator.PreviewDummyManager;
import pl.mcmmorpg.charactercreator.creator.PreviewRotationTask;
import pl.mcmmorpg.charactercreator.creator.SessionStateStore;
import pl.mcmmorpg.charactercreator.model.ModelEngineBridge;
import pl.mcmmorpg.charactercreator.persist.CharacterStore;
import pl.mcmmorpg.charactercreator.persist.OrphanScanner;
import pl.mcmmorpg.charactercreator.profile.CharacterProfileModule;
import pl.mcmmorpg.charactercreator.profile.ProfileListHeadsListener;
import pl.mcmmorpg.charactercreator.profile.ProfileSelectionEnforcer;
import pl.mcmmorpg.charactercreator.profile.RaceModelListener;
import pl.mcmmorpg.charactercreator.scene.CharacterSelect;
import pl.mcmmorpg.charactercreator.scene.CreatorFlow;
import pl.mcmmorpg.charactercreator.scene.SceneMenu;
import pl.mcmmorpg.charactercreator.scene.NewCharacterBook;
import pl.mcmmorpg.charactercreator.scene.SceneListener;
import pl.mcmmorpg.charactercreator.scene.SceneManager;
import pl.mcmmorpg.charactercreator.scene.SceneSettings;
import pl.mcmmorpg.charactercreator.scene.SceneView;
import pl.mcmmorpg.charactercreator.skin.SkinService;

/**
 * Join routing and the profile-select screen are NOT owned by this plugin — MMOProfiles'
 * own ProfileListViewer is the one select screen, and CharacterProfileModule reacts to its
 * ProfileCreateEvent/ProfileSelectEvent/ProfileUnloadEvent instead of us listening to
 * PlayerJoinEvent ourselves.
 */
public final class CharacterCreatorPlugin extends JavaPlugin {

  /** Straznik sesji — co 10 tickow, patrz CreatorSessionManager#tick. */
  private static final long WATCHDOG_PERIOD_TICKS = 10L;

  /** Walidacja blueprintow czeka, az ModelEngine skonczy je wczytywac. */
  private static final long LATE_VALIDATION_DELAY_TICKS = 40L;

  private CreatorConfig creatorConfig;
  private CharacterStore characterStore;
  private SessionStateStore sessionStateStore;
  private CreatorSessionManager sessionManager;
  private SkinService skinService;
  private PreviewRotationTask rotationTask;
  private BukkitTask watchdogTask;
  private SceneManager sceneManager;
  private pl.mcmmorpg.charactercreator.scene.SceneChatMute chatMute;
  private ProfileSelectionEnforcer profileEnforcer;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    creatorConfig = new CreatorConfig(this);
    creatorConfig.reload();
    // Teksty i pokretla z configu (sekcje teksty: / ustawienia:) — tez przy /cc reload.
    Teksty.load(getConfig());
    Ustawienia.load(getConfig());

    ModelEngineBridge modelEngineBridge = new ModelEngineBridge(getLogger());

    // Skorki czytamy PRZED walidacja configu — to ona mowi, dla ktorych par rasa x klasa
    // ich brakuje.
    skinService = new SkinService(this, getLogger());
    skinService.reload();

    // Swiat kreatora byl dotad rozwiazywany RAZ, przy starcie, i nigdy nie sprawdzany na
    // null: dzialalo to wylacznie dlatego, ze w logu Multiverse wstawalo 30 s przed nami.
    // To byl zbieg okolicznosci, nie kontrakt — teraz Multiverse jest w 'depend', a brak
    // swiata wylacza plugin z czytelnym bledem zamiast rzucac NPE przy kazdym teleporcie.
    if (!validateOrDisable(modelEngineBridge)) {
      return;
    }

    characterStore = new CharacterStore(this, getLogger());
    characterStore.backupNow("startup");
    characterStore.load();

    sessionStateStore = new SessionStateStore(this, getLogger());
    sessionStateStore.load();
    if (!sessionStateStore.pendingPlayers().isEmpty()) {
      getLogger().warning(sessionStateStore.pendingPlayers().size() + " graczy nie dokonczylo "
          + "tworzenia postaci przed ostatnim zatrzymaniem serwera — ich ekwipunek wroci "
          + "przy najblizszym wejsciu na serwer.");
    }

    PreviewDummyManager previewDummyManager =
        new PreviewDummyManager(modelEngineBridge, skinService);
    CreatorMenu creatorMenu = new CreatorMenu(creatorConfig);
    sessionManager = new CreatorSessionManager(this, creatorConfig, previewDummyManager,
        creatorMenu, sessionStateStore, getLogger());
    NameInputDialog nameInputDialog = new NameInputDialog(this);
    ConfirmService confirmService =
        new ConfirmService(this, creatorConfig, modelEngineBridge, skinService, getLogger());

    CharacterProfileModule profileModule = new CharacterProfileModule(
        this, creatorConfig, characterStore, sessionManager, confirmService);
    // registerModule DOES wire it up as a Bukkit listener internally — a live test with
    // both this and an explicit registerEvents() call showed ProfileCreateEvent firing
    // twice for the same profile, which would leak a second preview dummy per creation.
    MMOProfiles.plugin.registerModule(profileModule);

    CreatorRoomListener creatorRoomListener = new CreatorRoomListener(
        this, creatorConfig, sessionManager, previewDummyManager, creatorMenu, nameInputDialog,
        characterStore, profileModule::onConfirm, profileModule::onCancel);
    getServer().getPluginManager().registerEvents(creatorRoomListener, this);

    // Obejscie bledu MMOCore przy drugiej zmianie postaci — patrz MmocoreProfileSwitchFix.
    getServer().getPluginManager().registerEvents(
        new pl.mcmmorpg.charactercreator.profile.MmocoreProfileSwitchFix(getLogger()), this);

    // Model rasy nie przezywa zmiany swiata ani respawnu — ten listener go przywraca.
    getServer().getPluginManager().registerEvents(new RaceModelListener(profileModule), this);

    // Glowy na ekranie wyboru postaci MMOProfiles — bez tego kazda postac ma tam glowe
    // Steve'a. Wylaczalne, bo dotyka CUDZEGO okna: gdyby MMOProfiles zmienilo jego uklad,
    // ma byc czym to odciac bez przebudowy jara.
    if (creatorConfig.skinProfileHeads()) {
      ProfileListHeadsListener headsListener =
          ProfileListHeadsListener.create(this, characterStore, skinService);
      if (headsListener != null) {
        getServer().getPluginManager().registerEvents(headsListener, this);
      }
    }

    // Pierwszy krok cyklu: nikt nie gra bez postaci. Patrz ProfileSelectionEnforcer —
    // routing joina byl dotad NICZYIM, mimo ze javadoc tej klasy twierdzil, ze nalezy
    // do MMOProfiles.
    if (creatorConfig.enforceProfileSelection()) {
      profileEnforcer = new ProfileSelectionEnforcer(this, creatorConfig, sessionManager, getLogger());
      getServer().getPluginManager().registerEvents(profileEnforcer, this);
    } else {
      getLogger().warning("profile-selection.enforce: false — gracz, ktory zamknie ekran "
          + "wyboru profilu, moze grac bez postaci. Nikt tego nie pilnuje.");
    }

    sweepCreatorRoom();
    prepareCreatorRoom();

    CreatorCommand command = registerCommand(modelEngineBridge, profileModule);
    startScene(previewDummyManager, command, profileModule);

    // /cc testpostac — wejscie botow na klonach testowych, za trzema zamkami (TestEntry).
    pl.mcmmorpg.charactercreator.command.TestEntry testEntry =
        new pl.mcmmorpg.charactercreator.command.TestEntry(this, getLogger(), creatorConfig);
    testEntry.warnOnStartup();
    profileModule.setTestEntry(testEntry);
    if (command != null) {
      command.setTestEntry(testEntry);
    }

    watchdogTask = getServer().getScheduler().runTaskTimer(this, sessionManager::tick,
        WATCHDOG_PERIOD_TICKS, WATCHDOG_PERIOD_TICKS);

    // Obrot podgladu nie wymaga packetevents: zamiast wysylac pakiety rotacji (ktore byly
    // nadpisywane przez LibsDisguises i w efekcie model stal nieruchomo), obracamy encje
    // serwerowo przez Entity#setRotation. Patrz PreviewRotationTask.
    rotationTask = new PreviewRotationTask(sessionManager);
    rotationTask.runTaskTimer(this, 1L, Math.max(1, creatorConfig.rotationIntervalTicks()));

    // Blueprinty ModelEngine wczytuja sie w jego wlasnym onEnable — sprawdzenie ich w tym
    // samym ticku co nasz start dawalo falszywe "brak blueprintu" dla KAZDEJ rasy.
    getServer().getScheduler().runTaskLater(this,
        () -> reportProblems(creatorConfig.validate(modelEngineBridge, skinService),
            false /* earlyPass */),
        LATE_VALIDATION_DELAY_TICKS);

    getLogger().info("CharacterCreator " + getPluginMeta().getVersion()
        + " wlaczony — integracja z MMOProfiles, " + creatorConfig.races().size() + " ras, "
        + creatorConfig.classes().size() + " klas.");
  }

  /**
   * Sala kreatora ma byc PUSTA — poza podgladem, ktorego wlascicielem jest sesja.
   *
   * <p>Powod jest zmierzony, nie porzadkowy. W {@code creator_limbo} na produkcji stal
   * widoczny {@code armor_stand} (tagi {@code cc_dummy}, {@code cc_p_TEST}) i
   * {@code mannequin} — obie encje DOKLADNIE w punkcie {@code creator-room.dummy-anchor},
   * czyli w tym samym miejscu, co podglad postaci, obie wsummonowane recznie
   * ({@code Paper.SpawnReason: COMMAND}) przy porownywaniu tej wersji z prototypem
   * config-only. Skutek dla gracza byl taki, ze armor stand wystawal zza ramion postaci,
   * a przy kazdej zmianie rasy — kiedy LibsDisguises na chwile zdejmuje i zaklada
   * przebranie od nowa — zza podgladu wygladal mannequin. Wygladalo to na blad modelu.
   *
   * <p>Czyscimy przy starcie, a nie przy kazdej sesji: w tym momencie nie ma jeszcze ani
   * jednej sesji, wiec nie ma jak pomylic cudzego podgladu ze smieciem. Dwoch graczy
   * naraz w sali ma po jednym zywym dummy'm i zamiatanie w trakcie skasowaloby jednemu
   * z nich postac z ekranu.
   */
  private void sweepCreatorRoom() {
    if (!creatorConfig.sweepRoomEntities()) {
      return;
    }
    World world = getServer().getWorld(creatorConfig.roomWorldName());
    if (world == null) {
      return;
    }
    Map<String, Integer> removed = new TreeMap<>();
    for (Entity entity : world.getEntities()) {
      if (entity instanceof Player) {
        continue;
      }
      removed.merge(entity.getType().name(), 1, Integer::sum);
      entity.remove();
    }
    if (!removed.isEmpty()) {
      getLogger().warning("Sprzatnieto " + removed.values().stream().mapToInt(Integer::intValue).sum()
          + " obcych encji z sali kreatora (" + creatorConfig.roomWorldName() + "): " + removed
          + ". Podglad postaci nalezy do sesji i powstaje dopiero przy wejsciu gracza — cokolwiek "
          + "innego stoi w tej sali, zaslania postac albo wystaje zza niej.");
    }
    // Samo sprzatniecie nie wystarcza: sala ma podloge, wiec zwierzeta spawnuja sie w niej
    // NA BIEZACO. Zlapane na zrzucie z prawdziwego klienta — kura chodzaca po scenie za
    // plecami postaci, kilkanascie minut po tym, jak sprzatanie przy starcie usunelo
    // poprzednia. Zamykamy zrodlo, a nie skutek; gamerule jest trwaly (level.dat) i widoczny
    // dla admina przez /gamerule, wiec nie jest to ciche ustawienie znikad.
    if (Boolean.TRUE.equals(world.getGameRuleValue(GameRule.DO_MOB_SPAWNING))) {
      world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
      getLogger().info("Wylaczono naturalny spawn mobow w sali kreatora ("
          + creatorConfig.roomWorldName() + ") — w kadrze podgladu nie ma po co nic chodzic.");
    }
  }

  /**
   * Sprawia, ze wejscie do sali jest NATYCHMIASTOWE, a nie "po chwili".
   *
   * <p>Zmierzone na prawdziwym kliencie: po kliknieciu "Create New Character" gracz widzi
   * najpierw ekran <i>Loading terrain...</i> narysowany na POPRZEDNIM swiecie, potem sale
   * w polowie doladowana (zrzuty w {@code output/kreator-responsywnosc-01/captures/}),
   * a podglad postaci pojawia sie 352 ms po kliknieciu. Sala kreatora to osobny swiat, wiec
   * wejscie do niej jest zmiana wymiaru i tego sie nie obejdzie — ale mozna skrocic to,
   * na co klient czeka:
   *
   * <ul>
   *   <li><b>zasieg widzenia</b>: sala ma kilkanascie blokow, a serwer wysylal jej chunki
   *       z globalnego {@code view-distance=10}, czyli 21x21 = 441 chunkow pustki. Przy 4
   *       jest ich 81 i ekran ladowania konczy sie odpowiednio wczesniej;</li>
   *   <li><b>chunki na stale w pamieci</b>: bez tego serwer musi je najpierw wczytac
   *       z dysku przy KAZDYM wejsciu, bo po wyjsciu ostatniego gracza sala sie wyladowuje.</li>
   * </ul>
   */
  private void prepareCreatorRoom() {
    World world = getServer().getWorld(creatorConfig.roomWorldName());
    if (world == null) {
      return;
    }
    int distance = creatorConfig.roomViewDistance();
    if (distance > 0 && world.getViewDistance() != distance) {
      world.setViewDistance(distance);
      world.setSimulationDistance(distance);
      getLogger().info("Zasieg widzenia sali kreatora (" + creatorConfig.roomWorldName()
          + ") ustawiony na " + distance + " chunkow — ekran 'Loading terrain' konczy sie "
          + "wtedy szybciej.");
    }
    if (!creatorConfig.keepRoomLoaded()) {
      return;
    }
    // Kwadrat 3x3 wokol kotwic. Wiecej nie ma sensu: w kadrze nie widac dalej, a kazdy
    // wymuszony chunk to pamiec trzymana do konca zycia serwera.
    int loaded = 0;
    for (Location anchor : List.of(creatorConfig.roomSpawn(), creatorConfig.dummyAnchor())) {
      if (anchor == null) {
        continue;
      }
      int cx = anchor.getBlockX() >> 4;
      int cz = anchor.getBlockZ() >> 4;
      for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
          if (!world.isChunkForceLoaded(cx + dx, cz + dz)) {
            world.setChunkForceLoaded(cx + dx, cz + dz, true);
            loaded++;
          }
        }
      }
    }
    if (loaded > 0) {
      getLogger().info("Sala kreatora trzymana w pamieci: " + loaded + " chunkow wymuszonych "
          + "na stale, zeby wejscie nie czekalo na ich wczytanie z dysku.");
    }
  }

  private boolean validateOrDisable(ModelEngineBridge modelEngineBridge) {
    List<CreatorConfig.Problem> problems = creatorConfig.validate(modelEngineBridge, skinService);
    boolean fatal = reportProblems(problems, true /* earlyPass */);
    if (fatal) {
      getLogger().severe("CharacterCreator NIE zostanie wlaczony: config.yml ma bledy, ktore "
          + "konczylyby sie utrata danych gracza albo NPE w trakcie gry (patrz wyzej).");
      getServer().getPluginManager().disablePlugin(this);
      return false;
    }
    return true;
  }

  /**
   * @param earlyPass true dla sprawdzenia przy starcie. Na tym etapie ModelEngine moze
   *                  jeszcze nie miec wczytanych blueprintow, wiec WARNING i INFO (ktore
   *                  dotycza wlasnie ich) sa przemilczane i wracaja w przebiegu opoznionym.
   * @return true, jesli wsrod problemow byl chocby jeden FATAL.
   */
  private boolean reportProblems(List<CreatorConfig.Problem> problems, boolean earlyPass) {
    boolean fatal = false;
    for (CreatorConfig.Problem problem : problems) {
      switch (problem.severity()) {
        case FATAL -> {
          fatal = true;
          getLogger().severe(problem.message());
        }
        case WARNING -> {
          if (!earlyPass) {
            getLogger().warning(problem.message());
          }
        }
        case INFO -> {
          if (!earlyPass) {
            getLogger().info(problem.message());
          }
        }
      }
    }
    return fatal;
  }

  /**
   * Scena w stylu Wynncrafta (etap 1: szkielet). Na razie wchodzi sie do niej tylko
   * komenda admina {@code /cc scena} — stary kreator dziala dalej bez zmian, dopoki etap 2
   * nie podepnie zatwierdzania postaci.
   */
  private void startScene(PreviewDummyManager previewDummyManager, CreatorCommand command,
      CharacterProfileModule profileModule) {
    SceneSettings settings = SceneSettings.read(getConfig(), creatorConfig.roomWorldName());
    settings.problems().forEach(getLogger()::warning);
    if (!settings.usable()) {
      return;
    }
    sceneManager = new SceneManager(this, getLogger(), settings);
    getServer().getPluginManager().registerEvents(new SceneListener(sceneManager, getLogger()), this);
    // Cisza na czacie w scenie (wybor i kreator); pasek akcji z podpowiedziami przechodzi.
    chatMute = pl.mcmmorpg.charactercreator.scene.SceneChatMute.register(this, sceneManager, getLogger(),
        settings.replaceSelector() && creatorConfig.enforceProfileSelection());
    sceneManager.start();
    if (profileEnforcer != null) {
      profileEnforcer.setSceneCheck(sceneManager::inScene);
    }
    // Stary kreator stoi w tym samym swiecie — jego sesji straznik nie rusza, admina tez nie
    // (buduje scenerie). isPermissionSet, nie hasPermission: wlasciciel ma OP.
    sceneManager.setStrayGuard(
        p -> sessionManager.isActive(p) || p.isPermissionSet(CreatorCommand.PERMISSION),
        sessionManager::configuredExit);
    NewCharacterBook book = new NewCharacterBook(this, getLogger());
    getServer().getPluginManager().registerEvents(book, this);
    SceneMenu sceneMenu = new SceneMenu(this, getLogger());
    getServer().getPluginManager().registerEvents(sceneMenu, this);
    // Wybor postaci: kosz (trash.yml), straznik kosza i ustawienia 'wybor-postaci'.
    pl.mcmmorpg.charactercreator.persist.TrashStore trashStore =
        new pl.mcmmorpg.charactercreator.persist.TrashStore(new java.io.File(getDataFolder(), "trash.yml"), getLogger());
    trashStore.load();
    pl.mcmmorpg.charactercreator.select.TrashGuard trashGuard =
        new pl.mcmmorpg.charactercreator.select.TrashGuard(trashStore, getLogger());
    getServer().getPluginManager().registerEvents(trashGuard, this);
    // Kreator dostaje okno sceny: potwierdzenie przerwania (PPM).
    java.util.function.Function<CreatorFlow.Outcome, CreatorFlow> flows =
        outcome -> new CreatorFlow(creatorConfig, previewDummyManager, outcome, sceneMenu);
    CharacterSelect select = new CharacterSelect(this, getLogger(), sceneManager, sceneMenu,
        creatorConfig, characterStore, previewDummyManager, trashStore, trashGuard,
        pl.mcmmorpg.charactercreator.select.SelectSettings.read(getConfig()));
    if (command != null) {
      command.setListOpener(settings.replaceSelector() ? select::openList : null);
      command.setTrash(trashStore);
      command.setSelectReload(() -> select.reload(
          pl.mcmmorpg.charactercreator.select.SelectSettings.read(getConfig())));
    }
    java.util.function.Predicate<Player> openSelect = p -> {
      try {
        return select.open(p);
      } catch (RuntimeException ex) {
        getLogger().severe("Scena wyboru postaci: blad dla " + p.getName() + ": " + ex);
        ex.printStackTrace();
        return false;
      }
    };
    if (command != null) {
      command.setSelectOpener(settings.replaceSelector() ? openSelect : null);
    }
    if (settings.replaceSelector()) {
      profileModule.setSelectOpener(openSelect);
      if (profileEnforcer != null) {
        profileEnforcer.setOpener(openSelect);
      }
      getLogger().info("Scena ZASTEPUJE okno wyboru postaci MMOProfiles (scene.replace-selector). "
          + "Ustaw w MMOProfiles 'no-gui-on-login: true', inaczej oba ekrany wyskocza naraz.");
    }
    if (settings.replaceCreator()) {
      profileModule.enableScene(sceneManager, flows, book);
      getLogger().info("Scena ZASTEPUJE sale kreatora przy tworzeniu postaci (scene.replace-creator).");
    }
    if (command != null) {
      command.setFlyoverPreview(p -> {
        if (sceneManager.inScene(p.getUniqueId())) {
          return "Gracz jest w scenie — najpierw /cc scena " + p.getName() + " zamknij.";
        }
        if (!sceneManager.open(p, new pl.mcmmorpg.charactercreator.scene.SceneController() {
          @Override
          public void onOpen(SceneView v) {
            v.flyover(v.settings().flyover(), v::close);
          }
        })) {
          return "Scena sie nie otworzyla (log).";
        }
        return sceneManager.settings().flyover() == null ? "Brak scene.flyover w configu."
            : "Przelot: " + p.getName();
      });
    }
    // /cc scena: podglad dla admina — cala droga z ksiega, ale bez tworzenia postaci.
    CreatorFlow[] preview = new CreatorFlow[1];
    preview[0] = flows.apply(new CreatorFlow.Outcome() {
      @Override
      public void chosen(SceneView view, CreatorConfig.RaceOption race,
          CreatorConfig.ClassOption clazz) {
        Player player = view.player();
        if (player == null) {
          return;
        }
        book.open(player, preview[0].summary(race, clazz), player.getName(),
            NewCharacterBook.DEFAULT_COLOR, new NewCharacterBook.Handler() {
              @Override
              public String create(Player p, String name, String color) {
                view.close();
                p.sendMessage(net.kyori.adventure.text.Component.text("Podgląd sceny: "
                    + name + " (" + race.id() + " + " + clazz.id() + ", " + color
                    + ") — w podglądzie postać nie jest tworzona.",
                    net.kyori.adventure.text.format.NamedTextColor.GREEN));
                return null;
              }

              @Override
              public void back(Player p) {
              }
            });
      }

      @Override
      public void cancelled(SceneView view) {
        view.close();
      }
    });
    CreatorFlow flow = preview[0];
    if (command != null) {
      command.setSceneOpener((player, close) -> {
        if (close) {
          sceneManager.close(player.getUniqueId());
          return "Scena zamknieta: " + player.getName();
        }
        if (sessionManager.isActive(player)) {
          return player.getName() + " jest w starym kreatorze — najpierw go zakoncz.";
        }
        return sceneManager.open(player, flow.start())
            ? "Scena otwarta: " + player.getName()
            : "Scena nieskonfigurowana — patrz log.";
      });
    }
    getLogger().info("Scena kreatora gotowa: " + settings.points().size() + " kadr(ow), /cc scena.");
  }

  private CreatorCommand registerCommand(ModelEngineBridge modelEngineBridge,
      CharacterProfileModule profileModule) {
    File mmoProfilesFolder = new File(getDataFolder().getParentFile(), "MMOProfiles");
    OrphanScanner orphanScanner = new OrphanScanner(mmoProfilesFolder, getLogger());
    // /cc reload przeladowuje TEZ skins.yml — inaczej poprawka skorki wymagalaby restartu
    // serwera, a jest to plik danych, nie kod.
    CreatorCommand executor = new CreatorCommand(creatorConfig, characterStore, sessionManager,
        orphanScanner, modelEngineBridge, skinService, profileModule, () -> {
          skinService.reload();
          Teksty.load(getConfig());
          Ustawienia.load(getConfig());
          // Kadry sceny tez z /cc reload — strojenie kamery bez restartu serwera.
          // Scena wlaczona po starcie (brak sekcji przy starcie) nadal wymaga restartu.
          if (sceneManager != null) {
            SceneSettings next = SceneSettings.read(getConfig(), creatorConfig.roomWorldName());
            next.problems().forEach(getLogger()::warning);
            if (next.usable()) {
              sceneManager.reload(next);
            }
          }
        });
    PluginCommand command = getCommand("charactercreator");
    if (command == null) {
      getLogger().severe("Komenda 'charactercreator' nie jest zadeklarowana w plugin.yml — "
          + "narzedzia administracyjne sa niedostepne.");
      return null;
    }
    command.setExecutor(executor);
    command.setTabCompleter(executor);
    return executor;
  }

  /**
   * <b>Musi sprzatnac sesje.</b> Stara wersja robila tylko {@code rotationTask.cancel()} i
   * zapis rekordow, wiec {@code /stop} z graczem w kreatorze oznaczal trwale skasowany
   * plecak — stash sesji zyl wylacznie w RAM-ie. Teraz kazda sesja jest konczona (rzeczy
   * wracaja do gracza), a to, czego nie da sie oddac offline, zostaje w sessions.yml
   * i wraca przy najblizszym wejsciu na serwer.
   */
  @Override
  public void onDisable() {
    if (chatMute != null) {
      chatMute.unregister();
      chatMute = null;
    }
    // Scena pierwsza: oddaje graczom tryb gry i miejsce, zanim reszta sie wylaczy.
    if (sceneManager != null) {
      sceneManager.closeAll();
    }
    if (watchdogTask != null) {
      watchdogTask.cancel();
    }
    if (rotationTask != null) {
      rotationTask.cancel();
    }
    if (sessionManager != null) {
      sessionManager.endAll();
    }
    if (characterStore != null) {
      characterStore.saveNow();
    }
    // Zapis sesji jest teraz asynchroniczny z debouncem, wiec przy wylaczaniu trzeba go
    // domknac recznie — scheduler juz nie odpali odlozonego zadania.
    if (sessionStateStore != null) {
      sessionStateStore.saveNow();
    }
  }
}
