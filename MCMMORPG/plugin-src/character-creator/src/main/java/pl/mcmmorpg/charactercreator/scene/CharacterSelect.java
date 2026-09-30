package pl.mcmmorpg.charactercreator.scene;

import fr.phoenixdevt.mmoprofiles.bukkit.profile.PlayerProfileImpl;
import fr.phoenixdevt.mmoprofiles.bukkit.profile.ProfileListImpl;
import fr.phoenixdevt.profiles.PlayerProfile;
import fr.phoenixdevt.profiles.event.ProfileUnloadEvent;
import fr.phoenixdevt.profiles.util.WrappedLocation;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.mcmmorpg.charactercreator.CreatorConfig;
import pl.mcmmorpg.charactercreator.creator.PreviewDummyManager;
import pl.mcmmorpg.charactercreator.persist.CharacterRecord;
import pl.mcmmorpg.charactercreator.persist.CharacterStore;
import pl.mcmmorpg.charactercreator.persist.TrashStore;
import pl.mcmmorpg.charactercreator.select.PlaceNames;
import pl.mcmmorpg.charactercreator.select.RealGear;
import pl.mcmmorpg.charactercreator.select.SelectOrder;
import pl.mcmmorpg.charactercreator.select.SelectSettings;
import pl.mcmmorpg.charactercreator.select.TrashGuard;

/**
 * Wybor postaci: scena + lista + zarzadzanie + kosz.
 *
 * <p><b>Scena.</b> Stoi ostatnio grana postac w scenerii swojej rasy, w swoim PRAWDZIWYM
 * ekwipunku ({@link RealGear}). LPM albo spacja = graj. A/D (albo W/S) = poprzednia / nastepna
 * postac; sciemnienie tylko przy zmianie scenerii (inna rasa) — ta sama rasa przelacza sie od razu.
 *
 * <p><b>Lista</b> (PPM, a przy {@code wybor-postaci.lista-od}+ postaciach sama po wejsciu): okno
 * z kafelkiem na kazda postac — jeden klik = graj. Kolejnosc STALA (kolejnosc zakladania),
 * ostatnio grana oznaczona gwiazdka. Ta sama lista jest pod "Zmien postac" w menu Q — wtedy
 * klik zmienia postac od razu, bez sceny ({@link #openList}).
 *
 * <p><b>Zarzadzanie</b>: zaznaczanie wielu postaci naraz i przeniesienie ich do KOSZA. Postac
 * od {@code imie-od-poziomu} wymaga wpisania imienia. Kosz ukrywa postac na
 * {@code kosz-dni}; z kosza mozna przywrocic albo usunac na zawsze. Postac w koszu nadal zajmuje
 * miejsce na postac (MMOProfiles liczy profile, a profil wciaz istnieje) — okna mowia to wprost.
 *
 * <p>MMOProfiles zostaje magazynem postaci — wybor, tworzenie i usuwanie ida jego API
 * ({@code applyProfile}, {@code createProfile}, {@code removeProfile}).
 */
public final class CharacterSelect {

  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
  private static final String PREVIEW_KEY = "cc.select.preview";
  private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
  private static final TextColor MUTED = NamedTextColor.GRAY;
  private static final int TILE_WIDTH = 300;
  private static final long AUTO_LIST_DELAY_TICKS = 30L;

  private final Plugin plugin;
  private final Logger logger;
  private final SceneManager scenes;
  private final SceneMenu menu;
  private final CreatorConfig config;
  private final CharacterStore store;
  private final PreviewDummyManager dummies;
  private final TrashStore trash;
  private final TrashGuard guard;
  private final SelectSettings settings;
  private final PlaceNames places;

  /** Co otworzyc od razu po wejsciu w scene (klik w liscie z gry: "Zarzadzaj"). */
  private enum Start { SCENE, LIST, MANAGE }

  /** Jedna postac: profil MMOProfiles + nasz rekord (null = postac sprzed kreatora) + to, co na karte. */
  private record Entry(PlayerProfileImpl profile, CharacterRecord record, int level, String place) {

    UUID id() {
      return profile.getUniqueId();
    }

    String name() {
      return record != null ? record.name() : profile.getName();
    }

    Instant lastUsed() {
      if (record != null) {
        return record.effectiveLastUsed();
      }
      return Instant.ofEpochMilli(profile.getLastTimePlayed());
    }

    Instant lastPlayed() {
      long t = profile.getLastTimePlayed();
      Instant fromProfile = t > 0 ? Instant.ofEpochMilli(t) : null;
      Instant fromRecord = record == null ? null : record.effectiveLastUsed();
      if (fromProfile == null) {
        return fromRecord;
      }
      return fromRecord != null && fromRecord.isAfter(fromProfile) ? fromRecord : fromProfile;
    }
  }

  public CharacterSelect(Plugin plugin, Logger logger, SceneManager scenes, SceneMenu menu,
      CreatorConfig config, CharacterStore store, PreviewDummyManager dummies, TrashStore trash,
      TrashGuard guard, SelectSettings settings) {
    this.plugin = plugin;
    this.logger = logger;
    this.scenes = scenes;
    this.menu = menu;
    this.config = config;
    this.store = store;
    this.dummies = dummies;
    this.trash = trash;
    this.guard = guard;
    this.settings = settings;
    this.places = new PlaceNames(plugin.getDataFolder().getParentFile(), settings.places());
  }

  // --- wejscia -----------------------------------------------------------------------

  /**
   * Otwiera scene wyboru. Gracz BEZ postaci (i z pustym koszem) idzie od razu do kreatora.
   * Gracz W TRAKCIE gry najpierw wychodzi z postaci (jak przy oknie MMOProfiles).
   *
   * @return false, gdy sie nie dalo (brak danych MMOProfiles) — wolajacy moze otworzyc okno MMOProfiles
   */
  public boolean open(Player player) {
    return open(player, Start.SCENE);
  }

  private boolean open(Player player, Start start) {
    ProfileListImpl list = ProfileListImpl.getOrNull(player);
    if (list == null || list.hasProfilePending()) {
      return false;
    }
    if (list.getCurrent() != null) {
      UUID id = player.getUniqueId();
      list.quitCurrentProfile(ProfileUnloadEvent.Reason.QUIT_PROFILE).thenRun(() ->
          Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(id);
            if (online != null && online.isOnline()) {
              openFresh(online, start);
            }
          }));
      return true;
    }
    return openFresh(player, start);
  }

  /**
   * "Zmien postac" z menu Q: lista postaci jako okno NAD GRA — klik zmienia postac od razu,
   * bez sceny. Escape = zostajesz, gdzie jestes. Gracz bez wybranej postaci albo juz w scenie
   * dostaje zwykla scene z otwarta lista.
   */
  public boolean openList(Player player) {
    ProfileListImpl list = ProfileListImpl.getOrNull(player);
    if (list == null || list.hasProfilePending()) {
      return false;
    }
    SceneView view = scenes.view(player);
    if (view != null && view.controller() instanceof SelectStep step) {
      step.grid(view);
      return true;
    }
    if (list.getCurrent() == null || view != null) {
      return open(player, Start.LIST);
    }
    purgeExpired(player, list);
    List<Entry> entries = entries(list);
    UUID currentId = list.getCurrent().getUniqueId();
    List<SceneMenu.Button> buttons = new ArrayList<>();
    for (Entry e : entries) {
      boolean playing = e.id().equals(currentId);
      buttons.add(new SceneMenu.Button(tileLabel(e, false, playing), tileTooltip(e, playing),
          () -> switchInGame(player, e)).width(TILE_WIDTH));
    }
    addNewAndManage(player, list, buttons,
        () -> startCreationFromGame(player), () -> open(player, Start.MANAGE));
    buttons.add(new SceneMenu.Button(Component.text("Zamknij"), null, () -> { }));
    menu.form(player, listTitle(list), listBody(player, list, true), List.of(), buttons, 2);
    return true;
  }

  // --- dane --------------------------------------------------------------------------

  private List<Entry> entries(ProfileListImpl list) {
    List<Entry> out = new ArrayList<>();
    for (PlayerProfile p : profiles(list)) {
      if (p instanceof PlayerProfileImpl impl && !trash.isTrashed(impl.getUniqueId())) {
        out.add(entry(impl));
      }
    }
    // STALA kolejnosc: tak, jak profile leza w MMOProfiles (kolejnosc zakladania). Nie sortujemy
    // po "ostatnio grana" — postac nie moze skakac po liscie miedzy wejsciami.
    return out;
  }

  /**
   * Kopia listy profili. Zlapane na klonie 30.09: przy wejsciu MMOProfiles jeszcze dopisuje
   * profile (inny watek), a przegladanie zywej listy z czytaniem pliku poziomu dla kazdej postaci
   * trwalo na tyle dlugo, ze lecial ConcurrentModificationException i scena sie nie otwierala.
   * Kopiujemy szybko, a kolizje przy samym kopiowaniu ponawiamy.
   */
  private static List<PlayerProfile> profiles(ProfileListImpl list) {
    for (int attempt = 0; ; attempt++) {
      try {
        return new ArrayList<>(list.getProfiles());
      } catch (java.util.ConcurrentModificationException ex) {
        if (attempt >= 4) {
          throw ex;
        }
        Thread.onSpinWait();
      }
    }
  }

  private Entry entry(PlayerProfileImpl impl) {
    return new Entry(impl, store.get(impl.getUniqueId()), level(impl), place(impl));
  }

  private List<TrashStore.Entry> trashed(Player player, ProfileListImpl list) {
    List<TrashStore.Entry> out = new ArrayList<>();
    for (TrashStore.Entry t : trash.byOwner(player.getUniqueId())) {
      if (list.getProfile(t.profileId()) != null) {
        out.add(t);
      }
    }
    return out;
  }

  private static int indexOf(List<Entry> entries, UUID id) {
    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i).id().equals(id)) {
        return i;
      }
    }
    return -1;
  }

  private int level(PlayerProfileImpl p) {
    String level = p.getPlaceholderCache().getPlaceholder("mmocore_level");
    if (level == null || level.isBlank()) {
      level = levelFromFile(p.getUniqueId());
    }
    try {
      return level == null ? 0 : Integer.parseInt(level.trim());
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  private String place(PlayerProfileImpl p) {
    try {
      WrappedLocation at = p.getLocationMap() == null ? null
          : p.getLocationMap().getCurrentWrapped();
      if (at == null) {
        return null;
      }
      return places.describe(at.getWorldName(), at.getX(), at.getY(), at.getZ());
    } catch (RuntimeException ex) {
      return null;
    }
  }

  /**
   * Poziom z pliku MMOCore postaci. Pamiec MMOProfiles wypelnia sie dopiero przy otwarciu jego
   * okna, a scena wyboru to okno zastepuje. Przy MySQL w MMOCore pliku nie ma — wtedy bez poziomu.
   */
  private String levelFromFile(UUID profileId) {
    java.io.File file = new java.io.File(plugin.getDataFolder().getParentFile(),
        "MMOCore/userdata/" + profileId + ".yml");
    if (!file.isFile()) {
      return null;
    }
    int level = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file).getInt("level", 0);
    return level > 0 ? String.valueOf(level) : null;
  }

  // --- scena -------------------------------------------------------------------------

  private boolean openFresh(Player player, Start start) {
    ProfileListImpl list = ProfileListImpl.getOrNull(player);
    if (list == null) {
      return false;
    }
    purgeExpired(player, list);
    List<Entry> entries = entries(list);
    if (entries.isEmpty() && trashed(player, list).isEmpty()) {
      createNew(player, list);
      return true;
    }
    return scenes.open(player, new SelectStep(entries, start));
  }

  private final class SelectStep implements SceneController {
    private final List<Entry> entries;
    private final Start start;
    private int index;
    private UUID lastUsedId;

    SelectStep(List<Entry> entries, Start start) {
      this.entries = new ArrayList<>(entries);
      this.start = start;
      int last = SelectOrder.lastUsedIndex(this.entries, Entry::lastUsed);
      this.index = Math.max(0, last);
      this.lastUsedId = last < 0 ? null : this.entries.get(last).id();
    }

    private Entry current() {
      return entries.isEmpty() ? null : entries.get(Math.floorMod(index, entries.size()));
    }

    private String pointOf(Entry e) {
      CreatorConfig.RaceOption race = e == null || e.record() == null ? null
          : config.races().get(e.record().raceId());
      return race == null ? SceneSettings.DEFAULT_POINT : race.id();
    }

    @Override
    public void onOpen(SceneView view) {
      show(view);
      switch (start) {
        case LIST -> grid(view);
        case MANAGE -> manage(view, null);
        default -> {
          if (settings.listFrom() > 0 && entries.size() >= settings.listFrom()) {
            // Z opoznieniem: scena otwiera sie tuz po zmianie swiata (creator_limbo), a klient
            // pokazuje wtedy jeszcze ekran ladowania terenu. Okno wyslane w tej chwili potrafi
            // zniknac razem z nim. 30 tickow — gracz najpierw widzi swoja postac, potem liste.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
              if (!view.isClosed() && view.controller() == this) {
                grid(view);
              }
            }, AUTO_LIST_DELAY_TICKS);
          }
        }
      }
    }

    private void show(SceneView view) {
      Entry e = current();
      Player player = view.player();
      if (player == null) {
        return;
      }
      view.topBar(Component.text("Wybierz postać", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
      if (e == null) {
        view.point(SceneSettings.DEFAULT_POINT);
        dropPreview(view);
        view.list(null);
        int inTrash = trashedCount(view);
        view.card(Component.text("Nie masz aktywnych postaci", NamedTextColor.WHITE)
            .decoration(TextDecoration.BOLD, true)
            .append(Component.newline()).append(Component.newline())
            .append(Component.text(inTrash > 0
                ? "W koszu: " + SelectOrder.characters(inTrash) + ". Przywrócisz je pod PPM → Zarządzaj."
                : "Stwórz pierwszą postać.", MUTED)));
        view.hint(CreatorFlow.hint("LPM", "Nowa postać", "PPM", "Lista i zarządzanie"));
        return;
      }
      String point = pointOf(e);
      if (!point.equals(view.pointId())) {
        view.point(point);
      }
      CreatorConfig.RaceOption race = e.record() == null ? null : config.races().get(e.record().raceId());
      CreatorConfig.ClassOption clazz = e.record() == null ? null : config.classes().get(e.record().classId());
      Entity shown = showPreview(view, race, clazz);
      RealGear.apply(player, shown, e.profile().getInventory(), e.profile().getHeldItemSlot());
      view.list(sideList());
      view.card(card(e, race, clazz, e.id().equals(lastUsedId), false));
      int pos = Math.floorMod(index, entries.size()) + 1;
      view.hint(entries.size() > 1
          ? CreatorFlow.hint("LPM/Spacja", "Graj", "A/D", "Postać " + pos + "/" + entries.size(),
              "PPM", "Lista i zarządzanie")
          : CreatorFlow.hint("LPM/Spacja", "Graj", "PPM", "Lista i zarządzanie"));
    }

    private Component sideList() {
      Component out = Component.text("Twoje postacie  ", NamedTextColor.WHITE)
          .append(Component.text("[A/D]", NamedTextColor.GOLD));
      int current = Math.floorMod(index, entries.size());
      for (int i = 0; i < entries.size(); i++) {
        Entry e = entries.get(i);
        boolean on = i == current;
        out = out.append(Component.newline())
            .append(Component.text(on ? "▶ " : "   ", NamedTextColor.GOLD))
            .append(Component.text(e.name(), on ? iconColor(e) : NamedTextColor.GRAY)
                .decoration(TextDecoration.BOLD, on))
            .append(Component.text(e.level() > 0 ? "  " + e.level() : "", NamedTextColor.DARK_GRAY))
            .append(e.id().equals(lastUsedId) ? Component.text(" ★", NamedTextColor.GOLD) : Component.empty());
      }
      return out;
    }

    private void move(SceneView view, int delta) {
      if (entries.size() < 2) {
        return;
      }
      String from = pointOf(current());
      String to = pointOf(entries.get(Math.floorMod(index + delta, entries.size())));
      view.uiSound("character");
      if (from.equals(to)) {
        // Ta sama sceneria — bez sciemnienia: nosiciel skorki podmienia sie teleportem od razu.
        index += delta;
        show(view);
        return;
      }
      view.fade(() -> {
        index += delta;
        show(view);
      });
    }

    @Override
    public void onPrevious(SceneView view) {
      move(view, -1);
    }

    @Override
    public void onNext(SceneView view) {
      move(view, 1);
    }

    @Override
    public void onUp(SceneView view) {
      move(view, -1);
    }

    @Override
    public void onDown(SceneView view) {
      move(view, 1);
    }

    @Override
    public void onSelect(SceneView view) {
      Entry e = current();
      view.uiSound("select");
      if (e == null) {
        startCreation(view);
      } else {
        play(view, e);
      }
    }

    @Override
    public void onJump(SceneView view) {
      onSelect(view);
    }

    @Override
    public void onBack(SceneView view) {
      view.uiSound("back");
      grid(view);
    }

    /** Lista-siatka: kafelek na postac, klik = graj. */
    void grid(SceneView view) {
      Player player = view.player();
      ProfileListImpl list = player == null ? null : ProfileListImpl.getOrNull(player);
      if (list == null) {
        return;
      }
      List<SceneMenu.Button> buttons = new ArrayList<>();
      for (Entry e : entries) {
        buttons.add(new SceneMenu.Button(tileLabel(e, e.id().equals(lastUsedId), false),
            tileTooltip(e, false), () -> play(view, e)).width(TILE_WIDTH));
      }
      addNewAndManage(player, list, buttons, () -> startCreation(view), () -> manage(view, null));
      buttons.add(new SceneMenu.Button(Component.text("Wróć do sceny"), null, () -> { }));
      menu.form(player, listTitle(list), listBody(player, list, false), List.of(), buttons, 2);
    }

    /** Zaznaczanie wielu postaci do kosza. */
    void manage(SceneView view, Component note) {
      Player player = view.player();
      ProfileListImpl list = player == null ? null : ProfileListImpl.getOrNull(player);
      if (list == null) {
        return;
      }
      List<Component> body = new ArrayList<>();
      if (note != null) {
        body.add(note);
      }
      body.add(entries.isEmpty()
          ? Component.text("Nie masz aktywnych postaci.", NamedTextColor.WHITE)
          : Component.text("Zaznacz postacie do usunięcia. Trafią do kosza na "
              + SelectOrder.days(settings.trashTtl().toDays()) + " — w tym czasie przywrócisz je "
              + "jednym kliknięciem.", NamedTextColor.WHITE));
      List<DialogInput> inputs = new ArrayList<>();
      for (int i = 0; i < entries.size(); i++) {
        inputs.add(DialogInput.bool("z" + i, checkLabel(entries.get(i))).build());
      }
      List<TrashStore.Entry> inTrash = trashed(player, list);
      List<SceneMenu.Button> buttons = new ArrayList<>();
      if (!entries.isEmpty()) {
        List<Entry> snapshot = List.copyOf(entries);
        buttons.add(SceneMenu.Button.submit(Component.text("Usuń zaznaczone", NamedTextColor.RED),
            Component.text("Pyta o potwierdzenie"), r -> {
              List<Entry> chosen = pickChecked(r, "z", snapshot);
              if (chosen.isEmpty()) {
                manage(view, Component.text("Nic nie zaznaczono.", NamedTextColor.YELLOW));
              } else {
                confirm(view, chosen, false, null);
              }
            }));
      }
      if (!inTrash.isEmpty()) {
        buttons.add(new SceneMenu.Button(Component.text("Kosz (" + inTrash.size() + ")", NamedTextColor.GOLD),
            Component.text("Przywróć albo usuń na zawsze"), () -> trashScreen(view, null)));
      }
      buttons.add(new SceneMenu.Button(Component.text("Wróć"), null, () -> grid(view)));
      menu.form(player, Component.text("Zarządzaj postaciami", NamedTextColor.GOLD), body, inputs, buttons, 2);
    }

    /**
     * Potwierdzenie: lista wybranych, a przy postaciach od {@code imie-od-poziomu} pole na imie.
     *
     * @param forever true = usuniecie na zawsze z kosza; false = przeniesienie do kosza
     */
    private void confirm(SceneView view, List<Entry> chosen, boolean forever, Component error) {
      Player player = view.player();
      if (player == null) {
        return;
      }
      List<Component> body = new ArrayList<>();
      if (error != null) {
        body.add(error);
      }
      Component names = Component.empty();
      for (int i = 0; i < chosen.size(); i++) {
        Entry e = chosen.get(i);
        names = names.append(Component.text(i == 0 ? "" : ", "))
            .append(Component.text(e.name(), iconColor(e)).decoration(TextDecoration.BOLD, true))
            .append(Component.text(e.level() > 0 ? " (poz. " + e.level() + ")" : "", MUTED));
      }
      body.add(names);
      body.add(forever
          ? Component.text("Te postacie znikną NA ZAWSZE razem z ekwipunkiem i postępem. "
              + "Tego nie da się cofnąć.", NamedTextColor.RED)
          : Component.text("Trafią do kosza na " + SelectOrder.days(settings.trashTtl().toDays())
              + ". Do tego czasu nadal zajmują miejsca na postacie.", NamedTextColor.WHITE));
      List<DialogInput> inputs = new ArrayList<>();
      List<Entry> needName = new ArrayList<>();
      for (Entry e : chosen) {
        if (e.level() >= settings.confirmNameFromLevel()) {
          inputs.add(DialogInput.text("p" + needName.size(),
              Component.text("Wpisz imię „" + e.name() + "” (poziom " + e.level() + ")"))
              .maxLength(32).build());
          needName.add(e);
        }
      }
      Runnable back = forever ? () -> trashScreen(view, null) : () -> manage(view, null);
      List<SceneMenu.Button> buttons = List.of(
          SceneMenu.Button.submit(Component.text(forever ? "Usuń na zawsze" : "Przenieś do kosza",
              NamedTextColor.RED), null, r -> {
                for (int i = 0; i < needName.size(); i++) {
                  String typed = r == null ? null : r.getText("p" + i);
                  if (!SelectOrder.nameMatches(typed, needName.get(i).name())) {
                    confirm(view, chosen, forever, Component.text("Imię „" + needName.get(i).name()
                        + "” się nie zgadza — nic nie usunięto.", NamedTextColor.RED));
                    return;
                  }
                }
                if (forever) {
                  purge(view, chosen);
                } else {
                  toTrash(view, chosen);
                }
              }),
          new SceneMenu.Button(Component.text("Anuluj"), null, back));
      menu.form(player, Component.text(forever ? "Usunąć na zawsze?" : "Przenieść do kosza?",
          NamedTextColor.RED), body, inputs, buttons, 2);
    }

    private void toTrash(SceneView view, List<Entry> chosen) {
      Player player = view.player();
      if (player == null) {
        return;
      }
      List<Entry> before = List.copyOf(entries);
      Instant now = Instant.now();
      List<String> names = new ArrayList<>();
      for (Entry e : chosen) {
        trash.trash(new TrashStore.Entry(e.id(), player.getUniqueId(), e.name(), now));
        names.add(e.name());
        logger.info("Wybor: " + player.getName() + " przenosi do kosza postac " + e.name()
            + " (profil " + e.id() + ", poz. " + e.level() + ")");
      }
      refresh(view, before);
      manage(view, Component.text("W koszu: " + String.join(", ", names) + ". Przywrócisz je przez "
          + SelectOrder.days(settings.trashTtl().toDays()) + " w „Kosz”.", NamedTextColor.GREEN));
    }

    /** Kosz: przywroc albo usun na zawsze (zaznaczanie wielu). */
    void trashScreen(SceneView view, Component note) {
      Player player = view.player();
      ProfileListImpl list = player == null ? null : ProfileListImpl.getOrNull(player);
      if (list == null) {
        return;
      }
      List<TrashStore.Entry> inTrash = trashed(player, list);
      if (inTrash.isEmpty()) {
        manage(view, note != null ? note : Component.text("Kosz jest pusty.", MUTED));
        return;
      }
      Instant now = Instant.now();
      List<Component> body = new ArrayList<>();
      if (note != null) {
        body.add(note);
      }
      body.add(Component.text("Postacie w koszu zajmują miejsca na postacie ("
          + list.getProfiles().size() + "/" + list.getProfileSlots() + " zajętych). Po terminie "
          + "znikają na zawsze.", NamedTextColor.WHITE));
      List<DialogInput> inputs = new ArrayList<>();
      List<Entry> rows = new ArrayList<>();
      for (TrashStore.Entry t : inTrash) {
        Entry e = entry(list.getProfile(t.profileId()));
        inputs.add(DialogInput.bool("k" + rows.size(), checkLabel(e)
            .append(Component.text("  · zostało " + SelectOrder.days(t.daysLeft(now, settings.trashTtl())),
                NamedTextColor.GOLD))).build());
        rows.add(e);
      }
      List<SceneMenu.Button> buttons = List.of(
          SceneMenu.Button.submit(Component.text("Przywróć zaznaczone", NamedTextColor.GREEN), null, r -> {
            List<Entry> chosen = pickChecked(r, "k", rows);
            if (chosen.isEmpty()) {
              trashScreen(view, Component.text("Nic nie zaznaczono.", NamedTextColor.YELLOW));
              return;
            }
            List<Entry> before = List.copyOf(entries);
            List<String> names = new ArrayList<>();
            for (Entry e : chosen) {
              trash.restore(e.id());
              names.add(e.name());
              logger.info("Wybor: " + player.getName() + " przywraca z kosza " + e.name() + " (" + e.id() + ")");
            }
            refresh(view, before);
            trashScreen(view, Component.text("Przywrócono: " + String.join(", ", names) + ".",
                NamedTextColor.GREEN));
          }),
          SceneMenu.Button.submit(Component.text("Usuń zaznaczone na zawsze", NamedTextColor.RED), null, r -> {
            List<Entry> chosen = pickChecked(r, "k", rows);
            if (chosen.isEmpty()) {
              trashScreen(view, Component.text("Nic nie zaznaczono.", NamedTextColor.YELLOW));
              return;
            }
            confirm(view, chosen, true, null);
          }),
          new SceneMenu.Button(Component.text("Wróć"), null, () -> manage(view, null)));
      menu.form(player, Component.text("Kosz", NamedTextColor.GOLD), body, inputs, buttons, 2);
    }

    private void purge(SceneView view, List<Entry> chosen) {
      Player player = view.player();
      ProfileListImpl list = player == null ? null : ProfileListImpl.getOrNull(player);
      if (list == null) {
        return;
      }
      List<String> gone = new ArrayList<>();
      List<String> failed = new ArrayList<>();
      for (Entry e : chosen) {
        if (removeForever(player, list, e.profile(), e.name())) {
          gone.add(e.name());
        } else {
          failed.add(e.name());
        }
      }
      Component note = Component.empty();
      if (!gone.isEmpty()) {
        note = note.append(Component.text("Usunięto na zawsze: " + String.join(", ", gone) + ".",
            NamedTextColor.GREEN));
      }
      if (!failed.isEmpty()) {
        note = note.append(Component.text((gone.isEmpty() ? "" : " ") + "Nie udało się usunąć: "
            + String.join(", ", failed) + " — zgłoś to administracji.", NamedTextColor.RED));
      }
      trashScreen(view, note);
    }

    /** Po zmianie listy: odswiez wpisy i zostan na tej samej postaci (albo najblizszej). */
    private void refresh(SceneView view, List<Entry> before) {
      Player player = view.player();
      ProfileListImpl list = player == null ? null : ProfileListImpl.getOrNull(player);
      if (list == null) {
        return;
      }
      int oldIndex = before.isEmpty() ? 0 : Math.floorMod(index, before.size());
      entries.clear();
      entries.addAll(entries(list));
      index = SelectOrder.keepPosition(before, oldIndex, entries, Entry::id);
      if (lastUsedId == null || indexOf(entries, lastUsedId) < 0) {
        int last = SelectOrder.lastUsedIndex(entries, Entry::lastUsed);
        lastUsedId = last < 0 ? null : entries.get(last).id();
      }
      show(view);
    }

    private int trashedCount(SceneView view) {
      Player player = view.player();
      ProfileListImpl list = player == null ? null : ProfileListImpl.getOrNull(player);
      return list == null ? 0 : trashed(player, list).size();
    }

    @Override
    public void onClose(SceneView view) {
      dropPreview(view);
    }
  }

  // --- wspolne: kafelki, karta, akcje --------------------------------------------------

  private static boolean checked(DialogResponseView r, String key) {
    if (r == null) {
      return false;
    }
    Boolean b = r.getBoolean(key);
    return b != null && b;
  }

  private static List<Entry> pickChecked(DialogResponseView r, String prefix, List<Entry> rows) {
    List<Entry> out = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      if (checked(r, prefix + i)) {
        out.add(rows.get(i));
      }
    }
    return out;
  }

  /** "+ Nowa postac" (albo "Brak miejsc") i "Zarzadzaj postaciami" — wspolne dla listy w scenie i w grze. */
  private void addNewAndManage(Player player, ProfileListImpl list, List<SceneMenu.Button> buttons,
      Runnable create, Runnable manage) {
    int used = list.getProfiles().size();
    int slots = list.getProfileSlots();
    if (used < slots) {
      buttons.add(new SceneMenu.Button(Component.text("+ Nowa postać", NamedTextColor.GREEN),
          Component.text("Wolne miejsca: " + (slots - used)), create).width(TILE_WIDTH));
    } else {
      int inTrash = trashed(player, list).size();
      buttons.add(new SceneMenu.Button(Component.text("Brak wolnych miejsc", NamedTextColor.DARK_GRAY),
          Component.text(inTrash > 0
              ? "Postacie w koszu (" + inTrash + ") nadal zajmują miejsca — opróżnij kosz w „Zarządzaj”."
              : "Usuń postać w „Zarządzaj”, żeby zrobić miejsce."), manage).width(TILE_WIDTH));
    }
    buttons.add(new SceneMenu.Button(Component.text("Zarządzaj postaciami", NamedTextColor.GOLD),
        Component.text("Usuń kilka naraz, kosz, przywracanie"), manage).width(TILE_WIDTH));
  }

  private static Component listTitle(ProfileListImpl list) {
    return Component.text("Twoje postacie", NamedTextColor.GOLD)
        .append(Component.text("  " + list.getProfiles().size() + "/" + list.getProfileSlots(), MUTED));
  }

  private List<Component> listBody(Player player, ProfileListImpl list, boolean inGame) {
    int inTrash = trashed(player, list).size();
    Component text = Component.text(inGame
        ? "Kliknij postać, żeby od razu nią grać. " : "Kliknij postać, żeby grać. ", NamedTextColor.WHITE)
        .append(Component.text(inGame ? "▶" : "★", inGame ? NamedTextColor.GREEN : NamedTextColor.GOLD))
        .append(Component.text(inGame ? " = grasz teraz." : " = ostatnio grana.", NamedTextColor.WHITE));
    if (inTrash > 0) {
      text = text.append(Component.text(" W koszu: " + SelectOrder.characters(inTrash)
          + " (nadal zajmują miejsca).", MUTED));
    }
    return List.of(text);
  }

  /** Kafelek listy: "★ Imie  12 · Leśny Łowca". */
  private Component tileLabel(Entry e, boolean lastUsed, boolean playing) {
    Component out = Component.empty();
    if (playing) {
      out = out.append(Component.text("▶ ", NamedTextColor.GREEN));
    } else if (lastUsed) {
      out = out.append(Component.text("★ ", NamedTextColor.GOLD));
    }
    out = out.append(Component.text(e.name(), iconColor(e)).decoration(TextDecoration.BOLD, true));
    if (e.level() > 0) {
      out = out.append(Component.text("  " + e.level(), NamedTextColor.YELLOW));
    }
    String cls = classLine(e);
    if (cls != null) {
      out = out.append(Component.text(" · " + cls, MUTED));
    }
    return out;
  }

  private Component tileTooltip(Entry e, boolean playing) {
    CreatorConfig.RaceOption race = e.record() == null ? null : config.races().get(e.record().raceId());
    CreatorConfig.ClassOption clazz = e.record() == null ? null : config.classes().get(e.record().classId());
    return card(e, race, clazz, false, playing);
  }

  private Component checkLabel(Entry e) {
    Component out = Component.text(e.name(), iconColor(e)).decoration(TextDecoration.BOLD, true);
    if (e.level() > 0) {
      out = out.append(Component.text("  poz. " + e.level(), NamedTextColor.YELLOW));
    }
    String cls = classLine(e);
    return cls == null ? out : out.append(Component.text("  " + cls, MUTED));
  }

  /** Klasa rasowa ("Leśny Łowca") albo, dla postaci sprzed kreatora, klasa MMOCore. */
  private String classLine(Entry e) {
    if (e.record() != null) {
      CreatorConfig.RaceOption race = config.races().get(e.record().raceId());
      CreatorConfig.ClassOption clazz = config.classes().get(e.record().classId());
      if (race != null && clazz != null) {
        return plain(clazz.displayFor(race.id()));
      }
    }
    String cls = e.profile().getPlaceholderCache().getPlaceholder("mmocore_class");
    return cls == null || cls.isBlank() ? null : cls;
  }

  private Component card(Entry e, CreatorConfig.RaceOption race, CreatorConfig.ClassOption clazz,
      boolean lastUsed, boolean playing) {
    Component out = Component.text(e.name(), iconColor(e)).decoration(TextDecoration.BOLD, true);
    if (playing) {
      out = out.append(Component.text("  ▶ grasz teraz", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, false));
    } else if (lastUsed) {
      out = out.append(Component.text("  ★ ostatnio grana", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, false));
    }
    String className = e.profile().getPlaceholderCache().getPlaceholder("mmocore_class");
    if (clazz != null && race != null) {
      out = out.append(Component.newline())
          .append(LEGACY.deserialize(clazz.displayFor(race.id())).decoration(TextDecoration.ITALIC, false))
          .append(Component.text("  " + plain(race.display()) + " · " + plain(clazz.display()), MUTED));
    } else if (className != null && !className.isBlank()) {
      out = out.append(Component.newline()).append(Component.text(className, NamedTextColor.WHITE));
    }
    out = out.append(Component.newline()).append(Component.newline());
    if (className != null && !className.isBlank() && clazz != null && race != null
        && !plain(clazz.displayFor(race.id())).equalsIgnoreCase(className)) {
      // Po 10. poziomie klasa MMOCore to juz specjalizacja — pokazujemy ja osobno.
      out = out.append(Component.text("Specjalizacja: ", MUTED))
          .append(Component.text(className, NamedTextColor.WHITE)).append(Component.newline());
    }
    if (e.level() > 0) {
      out = out.append(Component.text("Poziom: ", MUTED))
          .append(Component.text(String.valueOf(e.level()), NamedTextColor.YELLOW)).append(Component.newline());
    }
    if (e.place() != null) {
      out = out.append(Component.text("Miejsce: ", MUTED))
          .append(Component.text(e.place(), NamedTextColor.WHITE)).append(Component.newline());
    }
    out = out.append(Component.text("Ostatnio: ", MUTED))
        .append(Component.text(playing ? "teraz" : SelectOrder.when(e.lastPlayed(), Instant.now(), ZONE),
            NamedTextColor.WHITE)).append(Component.newline());
    return out.append(Component.text("Czas gry: ", MUTED))
        .append(Component.text(SelectOrder.playTime(e.profile().getTimePlayed()), NamedTextColor.WHITE));
  }

  private void play(SceneView view, Entry entry) {
    Player player = view.player();
    if (player == null) {
      return;
    }
    view.close();
    // Tick pozniej: zamkniecie sceny oddaje tryb gry i pozycje, dopiero potem MMOProfiles
    // stawia gracza na zapisanej pozycji postaci.
    Bukkit.getScheduler().runTask(plugin, () -> {
      ProfileListImpl list = ProfileListImpl.getOrNull(player);
      if (list != null && player.isOnline()) {
        list.applyProfile(entry.profile());
      }
    });
  }

  /** Zmiana postaci z listy w grze: wyjscie z obecnej i od razu wybor nowej, bez sceny. */
  private void switchInGame(Player player, Entry target) {
    ProfileListImpl list = ProfileListImpl.getOrNull(player);
    if (list == null || !player.isOnline() || scenes.view(player) != null) {
      return;
    }
    if (list.getCurrent() == null) {
      list.applyProfile(target.profile());
      return;
    }
    if (list.getCurrent().getUniqueId().equals(target.id())) {
      player.sendActionBar(Component.text("Już grasz postacią " + target.name() + ".", NamedTextColor.YELLOW));
      return;
    }
    UUID id = player.getUniqueId();
    logger.info("Wybor: " + player.getName() + " zmienia postac z listy w grze na " + target.name()
        + " (" + target.id() + ")");
    list.quitCurrentProfile(ProfileUnloadEvent.Reason.QUIT_PROFILE).thenRun(() ->
        Bukkit.getScheduler().runTask(plugin, () -> {
          Player online = Bukkit.getPlayer(id);
          ProfileListImpl l = online == null ? null : ProfileListImpl.getOrNull(online);
          if (l != null && online.isOnline() && l.getCurrent() == null) {
            l.applyProfile(target.profile());
          }
        }));
  }

  private void startCreation(SceneView view) {
    Player player = view.player();
    if (player == null) {
      return;
    }
    view.close();
    Bukkit.getScheduler().runTask(plugin, () -> {
      ProfileListImpl list = ProfileListImpl.getOrNull(player);
      if (list != null && player.isOnline()) {
        createNew(player, list);
      }
    });
  }

  private void startCreationFromGame(Player player) {
    ProfileListImpl list = ProfileListImpl.getOrNull(player);
    if (list == null) {
      return;
    }
    UUID id = player.getUniqueId();
    Runnable create = () -> {
      Player online = Bukkit.getPlayer(id);
      ProfileListImpl l = online == null ? null : ProfileListImpl.getOrNull(online);
      if (l != null && online.isOnline()) {
        createNew(online, l);
      }
    };
    if (list.getCurrent() == null) {
      create.run();
      return;
    }
    list.quitCurrentProfile(ProfileUnloadEvent.Reason.QUIT_PROFILE)
        .thenRun(() -> Bukkit.getScheduler().runTask(plugin, create));
  }

  /**
   * Nowa postac przez MMOProfiles — ProfileCreateEvent otworzy scene kreatora.
   *
   * <p>Po zatwierdzeniu profil trzeba jeszcze WYBRAC. {@code createProfile} tylko dopisuje go do
   * listy; ProfileSelectEvent (a z nim dane modulow: Chowance, MMOCore) odpala dopiero
   * {@code applyProfile}. Okno MMOProfiles robi dokladnie to samo (bajtkod
   * ProfileListViewer: createProfile -> thenAccept(applyProfile)). Bez tego nowa postac grala
   * bez wybranego profilu: Chowance bez danych ("wybierz postac"), brak startera.
   */
  private void createNew(Player player, ProfileListImpl list) {
    int n = list.getProfiles().size() + 1;
    UUID playerId = player.getUniqueId();
    list.createProfile("Postac " + n).thenAccept(profile -> {
      Runnable select = () -> {
        Player online = Bukkit.getPlayer(playerId);
        if (online == null || !online.isOnline() || list.getCurrent() != null) {
          return;
        }
        list.applyProfile(profile);
      };
      if (Bukkit.isPrimaryThread()) {
        select.run();
      } else {
        Bukkit.getScheduler().runTask(plugin, select);
      }
    }).exceptionally(ex -> {
      logger.warning("Nie udalo sie utworzyc profilu dla " + player.getName() + ": " + ex);
      return null;
    });
  }

  // --- usuwanie: JEDNA droga -----------------------------------------------------------

  /**
   * Prawdziwe usuniecie profilu. MMOProfiles zdejmuje profil z listy SYNCHRONICZNIE, zaraz po
   * {@code ProfileRemoveEvent} (bajtkod removeProfile: callEvent -> isCancelled? -> list.remove).
   * Future domyka sie dopiero, gdy wszystkie moduly danych zwaliduja event — i to on "wisial".
   * Dlatego wynik czytamy z listy od razu, a future tylko obserwujemy: jesli po 5 s dalej
   * otwarty, log mowi, ktory modul nie zwalidowal.
   *
   * @return true, gdy profilu nie ma juz na liscie (rekord w characters.yml sprzata
   *     CharacterProfileModule#onRemove, wpis kosza — ta metoda)
   */
  private boolean removeForever(Player player, ProfileListImpl list, PlayerProfileImpl profile, String name) {
    UUID id = profile.getUniqueId();
    logger.info("Wybor: " + player.getName() + " usuwa NA ZAWSZE postac " + name + " (profil " + id + ")");
    CompletableFuture<PlayerProfileImpl> future;
    try {
      future = list.removeProfile(profile);
    } catch (RuntimeException ex) {
      logger.warning("Usuwanie postaci " + name + " rzucilo: " + ex);
      return false;
    }
    if (list.getProfile(id) != null) {
      logger.warning("Postac " + name + " (" + id + ") nadal na liscie MMOProfiles — ProfileRemoveEvent "
          + "anulowany przez inny plugin?");
      return false;
    }
    trash.restore(id);
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!future.isDone()) {
        logger.warning("removeProfile " + id + ": future nie domkniety po 5 s — moduly bez walidacji "
            + "ProfileRemoveEvent: " + guard.pendingModules(id) + ". Profil i tak zniknal z listy.");
      }
      guard.forget(id);
    }, 100L);
    return true;
  }

  /** Postacie po terminie w koszu — usuwane naprawde przy wejsciu wlasciciela do wyboru. */
  private void purgeExpired(Player player, ProfileListImpl list) {
    for (TrashStore.Entry t : trash.expired(player.getUniqueId(), Instant.now(), settings.trashTtl())) {
      PlayerProfileImpl p = list.getProfile(t.profileId());
      if (p == null) {
        trash.restore(t.profileId());
        continue;
      }
      if (list.getCurrent() != null && list.getCurrent().getUniqueId().equals(t.profileId())) {
        continue;
      }
      logger.info("Kosz: termin minal — " + t.name() + " gracza " + player.getName() + " usuwana na zawsze.");
      removeForever(player, list, p, t.name());
    }
  }

  // --- podglad postaci (ta sama stajnia nosicieli skorek co w kreatorze) --------------

  private Entity showPreview(SceneView view, CreatorConfig.RaceOption race, CreatorConfig.ClassOption clazz) {
    Player player = view.player();
    if (player == null) {
      return null;
    }
    Entity shown;
    if (view.get(PREVIEW_KEY) == null) {
      shown = dummies.start(view.subject(), player, race, clazz);
      for (Entity carrier : dummies.carriers(player.getUniqueId())) {
        view.own(carrier);
      }
      view.put(PREVIEW_KEY, Boolean.TRUE);
    } else {
      dummies.moveAnchor(player, view.subject());
      shown = dummies.select(player, race, clazz);
    }
    view.subjectEntity(shown);
    dummies.applyAppearance(player, race, clazz);
    return shown;
  }

  private void dropPreview(SceneView view) {
    if (view.get(PREVIEW_KEY) != null) {
      dummies.despawnAll(view.playerId());
      view.put(PREVIEW_KEY, null);
    }
  }

  private static TextColor iconColor(Entry e) {
    String id = e.record() == null ? "" : e.record().iconColor();
    TextColor c = NewCharacterBook.COLORS.get(id == null || id.isBlank() ? NewCharacterBook.DEFAULT_COLOR : id);
    return c == null ? NamedTextColor.WHITE : c;
  }

  private static String plain(String legacyText) {
    return (legacyText == null ? "" : legacyText).replaceAll("&[0-9a-fk-orA-FK-OR]", "");
  }
}
