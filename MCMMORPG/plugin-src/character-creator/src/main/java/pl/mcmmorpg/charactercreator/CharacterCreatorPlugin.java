package pl.mcmmorpg.charactercreator;

import fr.phoenixdevt.mmoprofiles.bukkit.MMOProfiles;
import java.io.File;
import java.util.List;
import org.bukkit.command.PluginCommand;
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
import pl.mcmmorpg.charactercreator.profile.RaceModelListener;

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
  private PreviewRotationTask rotationTask;
  private BukkitTask watchdogTask;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    creatorConfig = new CreatorConfig(this);
    creatorConfig.reload();

    ModelEngineBridge modelEngineBridge = new ModelEngineBridge(getLogger());

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

    sessionStateStore = new SessionStateStore(getDataFolder(), getLogger());
    sessionStateStore.load();
    if (!sessionStateStore.pendingPlayers().isEmpty()) {
      getLogger().warning(sessionStateStore.pendingPlayers().size() + " graczy nie dokonczylo "
          + "tworzenia postaci przed ostatnim zatrzymaniem serwera — ich ekwipunek wroci "
          + "przy najblizszym wejsciu na serwer.");
    }

    PreviewDummyManager previewDummyManager = new PreviewDummyManager(modelEngineBridge);
    CreatorMenu creatorMenu = new CreatorMenu(creatorConfig);
    sessionManager = new CreatorSessionManager(this, creatorConfig, previewDummyManager,
        creatorMenu, sessionStateStore, getLogger());
    NameInputDialog nameInputDialog = new NameInputDialog(this);
    ConfirmService confirmService =
        new ConfirmService(this, creatorConfig, modelEngineBridge, getLogger());

    CharacterProfileModule profileModule = new CharacterProfileModule(
        this, creatorConfig, characterStore, sessionManager, confirmService);
    // registerModule DOES wire it up as a Bukkit listener internally — a live test with
    // both this and an explicit registerEvents() call showed ProfileCreateEvent firing
    // twice for the same profile, which would leak a second preview dummy per creation.
    MMOProfiles.plugin.registerModule(profileModule);

    CreatorRoomListener creatorRoomListener = new CreatorRoomListener(
        this, creatorConfig, sessionManager, previewDummyManager, creatorMenu, nameInputDialog,
        characterStore, profileModule::onConfirm);
    getServer().getPluginManager().registerEvents(creatorRoomListener, this);

    // Model rasy nie przezywa zmiany swiata ani respawnu — ten listener go przywraca.
    getServer().getPluginManager().registerEvents(new RaceModelListener(profileModule), this);

    registerCommand(modelEngineBridge);

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
        () -> reportProblems(creatorConfig.validate(modelEngineBridge), false /* earlyPass */),
        LATE_VALIDATION_DELAY_TICKS);

    getLogger().info("CharacterCreator " + getPluginMeta().getVersion()
        + " wlaczony — integracja z MMOProfiles, " + creatorConfig.races().size() + " ras, "
        + creatorConfig.classes().size() + " klas.");
  }

  private boolean validateOrDisable(ModelEngineBridge modelEngineBridge) {
    List<CreatorConfig.Problem> problems = creatorConfig.validate(modelEngineBridge);
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

  private void registerCommand(ModelEngineBridge modelEngineBridge) {
    File mmoProfilesFolder = new File(getDataFolder().getParentFile(), "MMOProfiles");
    OrphanScanner orphanScanner = new OrphanScanner(mmoProfilesFolder, getLogger());
    CreatorCommand executor = new CreatorCommand(creatorConfig, characterStore, sessionManager,
        orphanScanner, modelEngineBridge, () -> { });
    PluginCommand command = getCommand("charactercreator");
    if (command == null) {
      getLogger().severe("Komenda 'charactercreator' nie jest zadeklarowana w plugin.yml — "
          + "narzedzia administracyjne sa niedostepne.");
      return;
    }
    command.setExecutor(executor);
    command.setTabCompleter(executor);
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
  }
}
