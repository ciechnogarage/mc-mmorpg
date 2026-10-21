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
import pl.mcmmorpg.charactercreator.Teksty;
import pl.mcmmorpg.charactercreator.Ustawienia;
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
 * Wybor postaci: scena + lista + zarzadzanie.
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
 * <p><b>Zarzadzanie</b>: zaznaczanie wielu postaci naraz i usuniecie ich NA ZAWSZE (po
 * potwierdzeniu; postac od {@code imie-od-poziomu} wymaga wpisania imienia). Kosza nie ma —
 * wlasciciel 01.10: "jak ktos je usuwa, to je usuwa". Kosz zajmowal miejsca na postacie
 * (profil MMOProfiles dalej istnial), przez co gracz z pelnym koszem nie mogl stworzyc nowej.
 * Pliki modulow usunietej postaci i tak laduja w {@code CharacterCreator/usuniete/<id>/}.
 *
 * <p>MMOProfiles zostaje magazynem postaci — wybor, tworzenie i usuwanie ida jego API
 * ({@code applyProfile}, {@code createProfile}, {@code removeProfile}).
 */
public final class CharacterSelect {

  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
  private static final String PREVIEW_KEY = "cc.select.preview";
  private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
  private static final TextColor MUTED = NamedTextColor.GRAY;
  /**
   * Szerokosc kafelka. Dwie kolumny po 300 wyjezdzaly poza ekran przy GUI 3-4 (ekran ma wtedy
   * ~450-640 jednostek) — 2 x 150 miesci sie od ~320 wzwyz. Klasa jest w dymku, nie na kafelku.
   */
  private static final int TILE_WIDTH = 150;

  private static int tileWidth() {
    return Ustawienia.i("wybor.kafelek-szerokosc", TILE_WIDTH);
  }
  private static final long AUTO_LIST_DELAY_TICKS = 30L;
  /** Po tylu tickach bez A/D dochodzi kadr i podglad postaci. */
  private static final long SCENE_DELAY_TICKS = 6L;
  /** O ile blokow pod ziemie chowamy podglad na miejscach Nowa postac / Usun postacie. */
  private static final double HIDE_DROP = 6.0;

  private enum Extra { NEW, MANAGE }

  private final Plugin plugin;
  private final Logger logger;
  private final SceneManager scenes;
  private final SceneMenu menu;
  private final CreatorConfig config;
  private final CharacterStore store;
  private final PreviewDummyManager dummies;
  private final TrashStore trash;
  private final TrashGuard guard;
  private volatile SelectSettings settings;
  private volatile PlaceNames places;

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

  /** {@code /cc reload}: nowe {@code wybor-postaci} (lista-od, imie-od-poziomu, miejsca). */
  public void reload(SelectSettings next) {
    this.settings = next;
    this.places = new PlaceNames(plugin.getDataFolder().getParentFile(), next.places());
  }

  // --- wejscia -----------------------------------------------------------------------

  /**
   * Otwiera scene wyboru. Gracz BEZ postaci idzie od razu do kreatora.
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
    if (!list.isSessionReady()) {
      return whenLoaded(player, () -> open(player, start));
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
    if (!list.isSessionReady()) {
      return whenLoaded(player, () -> openList(player));
    }
    SceneView view = scenes.view(player);
    if (view != null && view.controller() instanceof SelectStep step) {
      step.grid(view);
      return true;
    }
    if (list.getCurrent() == null || view != null) {
      return open(player, Start.LIST);
    }
    List<Entry> entries = entries(list);
    UUID currentId = list.getCurrent().getUniqueId();
    List<SceneMenu.Button> buttons = new ArrayList<>();
    for (Entry listed : entries) {
      boolean playing = listed.id().equals(currentId);
      // Zapis MMOProfiles postaci, ktora sie gra, jest nieaktualny do najblizszego zapisu —
      // miejsce bierzemy z tego, gdzie gracz stoi teraz.
      Entry e = playing ? new Entry(listed.profile(), listed.record(), listed.level(),
          places.describe(player.getWorld().getName(), player.getLocation().getX(),
              player.getLocation().getY(), player.getLocation().getZ())) : listed;
      buttons.add(new SceneMenu.Button(tileLabel(e, false, playing), tileTooltip(e, playing),
          () -> switchInGame(player, e)).width(tileWidth()));
    }
    addNewAndManage(list, buttons, () -> startCreationFromGame(player), () -> open(player, Start.MANAGE));
    buttons.add(new SceneMenu.Button(Teksty.t("wybor.przycisk.zamknij", "Zamknij"), null, () -> { }));
    menu.form(player, listTitle(list), listBody(player, list, true), List.of(), buttons, 2);
    return true;
  }

  private final java.util.Map<UUID, Integer> waiting = new java.util.concurrent.ConcurrentHashMap<>();

  /**
   * MMOProfiles wczytuje profile NA INNYM WATKU, dopisujac je do listy po kolei
   * ({@code loadFromJson}), a gotowosc ({@code markSessionReady}) oglasza dopiero na koniec.
   * Zlapane evalem 30.09: pierwsze wejscie po restarcie pokazalo 8 postaci z 11 — scena
   * zbudowala liste w polowie wczytywania (ten sam wyscig dawal wczesniej
   * ConcurrentModificationException). Czekamy wiec na gotowosc: co 5 tickow, najwyzej 20 s.
   *
   * @return true — wejscie przejete (gracz dostanie scene/liste, gdy dane beda gotowe)
   */
  private boolean whenLoaded(Player player, Runnable then) {
    UUID id = player.getUniqueId();
    int attempt = waiting.merge(id, 1, Integer::sum);
    if (attempt > 80) {
      waiting.remove(id);
      logger.warning("MMOProfiles nie wczytal postaci " + player.getName() + " w 20 s — wybor postaci pominiety.");
      return false;
    }
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      Player online = Bukkit.getPlayer(id);
      ProfileListImpl l = online == null ? null : ProfileListImpl.getOrNull(online);
      if (online == null || l == null) {
        waiting.remove(id);
        return;
      }
      if (l.isSessionReady()) {
        waiting.remove(id);
      }
      then.run();
    }, 5L);
    return true;
  }

  // --- dane --------------------------------------------------------------------------

  private List<Entry> entries(ProfileListImpl list) {
    List<Entry> out = new ArrayList<>();
    for (PlayerProfile p : profiles(list)) {
      if (p instanceof PlayerProfileImpl impl) {
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
    List<Entry> entries = entries(list);
    if (entries.isEmpty()) {
      createNew(player, list);
      return true;
    }
    return scenes.open(player, new SelectStep(entries, start));
  }

  private final class SelectStep implements SceneController {
    private final List<Entry> entries;
    private final Start start;
    private int index;
    /** Kolejne A/D uniewazniaja zaplanowane odswiezenie kadru. */
    private int sceneToken;
    private UUID lastUsedId;
    /** Zaznaczone w "Zarzadzaj" — trzymane miedzy odswiezeniami okna. */
    private final java.util.Set<UUID> chosen = new java.util.LinkedHashSet<>();

    SelectStep(List<Entry> entries, Start start) {
      this.entries = new ArrayList<>(entries);
      this.start = start;
      int last = SelectOrder.lastUsedIndex(this.entries, Entry::lastUsed);
      this.index = Math.max(0, last);
      this.lastUsedId = last < 0 ? null : this.entries.get(last).id();
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
            }, Ustawienia.l("wybor.lista-sama-po-ticki", AUTO_LIST_DELAY_TICKS));
          }
        }
      }
    }

    /**
     * Dodatkowe miejsca karuzeli A/D za postaciami: nowa postac i usuwanie. "Nowa postac" jest
     * ZAWSZE, takze przy pelnym limicie — wtedy karta mowi wprost "15/15, usun postac". Wczesniej
     * znikala bez slowa i gracz nie wiedzial, czemu nie moze stworzyc postaci (zgloszenie 01.10).
     */
    private List<Extra> extras(SceneView view) {
      List<Extra> out = new ArrayList<>();
      if (Ustawienia.b("wybor.ad-nowa-postac", true) || entries.isEmpty()) {
        out.add(Extra.NEW);
      }
      if (Ustawienia.b("wybor.ad-usun-postacie", true) && !entries.isEmpty()) {
        out.add(Extra.MANAGE);
      }
      return out;
    }

    private ProfileListImpl list(SceneView view) {
      Player player = view.player();
      return player == null ? null : ProfileListImpl.getOrNull(player);
    }

    private int count(SceneView view) {
      return entries.size() + extras(view).size();
    }

    /** Dodatkowe miejsce pod kursorem albo null, gdy kursor stoi na postaci. */
    private Extra extraAt(SceneView view) {
      int n = count(view);
      int i = Math.floorMod(index, n);
      if (i < entries.size()) {
        return null;
      }
      List<Extra> ex = extras(view);
      return ex.get(Math.min(i - entries.size(), ex.size() - 1));
    }

    /** Postac pod kursorem (null na dodatkowym miejscu albo bez postaci). */
    private Entry shownEntry(SceneView view) {
      if (entries.isEmpty()) {
        return null;
      }
      int i = Math.floorMod(index, count(view));
      return i < entries.size() ? entries.get(i) : null;
    }

    private void show(SceneView view) {
      showText(view);
      showScene(view);
    }

    /**
     * Tanie odswiezenie: lista, karta i podpowiedz od razu przy kazdym A/D. Kadr i podglad
     * dochodza z opoznieniem ({@link #showScene}) — gracz przewija bez czekania i bez czerni.
     */
    private void showText(SceneView view) {
      view.topBar(Teksty.t("wybor.tytul", "&6&lWybierz postać"));
      view.list(sideList(view));
      Extra extra = extraAt(view);
      if (extra == Extra.NEW) {
        ProfileListImpl list = list(view);
        if (list != null && list.getProfiles().size() >= list.getProfileSlots()) {
          view.card(Teksty.t("wybor.nowa.karta-limit", "&c&lLimit postaci: {zajete}/{wszystkie}\n\n&r&fMasz już wszystkie miejsca na postacie. Usuń którąś, żeby stworzyć nową.",
              "zajete", list.getProfiles().size(), "wszystkie", list.getProfileSlots()));
          view.hint(CreatorFlow.hint("LPM", Teksty.plain("podpowiedz.usun-postac", "Usuń postać"),
              "A/D", Teksty.plain("podpowiedz.przegladaj", "Przeglądaj"), "PPM", Teksty.plain("podpowiedz.lista", "Lista i zarządzanie")));
          return;
        }
        view.card(Teksty.t("wybor.nowa.karta", "&a&l+ Nowa postać\n\n&r&fWybierz rasę i klasę, nadaj imię i ruszaj w drogę."));
        view.hint(CreatorFlow.hint("LPM", Teksty.plain("podpowiedz.stworz", "Stwórz postać"),
            "A/D", Teksty.plain("podpowiedz.przegladaj", "Przeglądaj"), "PPM", Teksty.plain("podpowiedz.lista", "Lista i zarządzanie")));
        return;
      }
      if (extra == Extra.MANAGE) {
        view.card(Teksty.t("wybor.usun.karta-na-zawsze", "&c&lUsuń postacie\n\n&r&fZaznacz jedną lub kilka postaci. Usunięcie jest ostateczne — postać znika razem z ekwipunkiem i postępem."));
        view.hint(CreatorFlow.hint("LPM", Teksty.plain("podpowiedz.usun-postac", "Usuń postać"),
            "A/D", Teksty.plain("podpowiedz.przegladaj", "Przeglądaj"), "PPM", Teksty.plain("podpowiedz.lista", "Lista i zarządzanie")));
        return;
      }
      Entry e = shownEntry(view);
      if (e == null) {
        return;
      }
      CreatorConfig.RaceOption race = e.record() == null ? null : config.races().get(e.record().raceId());
      CreatorConfig.ClassOption clazz = e.record() == null ? null : config.classes().get(e.record().classId());
      view.card(card(e, race, clazz, e.id().equals(lastUsedId), false));
      // Licznik tylko po postaciach — "Nowa postac" i "Usun postacie" to nie postacie.
      String nav = (Math.floorMod(index, count(view)) + 1) + "/" + entries.size();
      view.hint(CreatorFlow.hint("LPM/Spacja", Teksty.plain("podpowiedz.graj", "Graj"),
          "A/D", Teksty.plain("podpowiedz.postac", "Postać {nr}", "nr", nav), "PPM", Teksty.plain("podpowiedz.lista", "Lista i zarządzanie")));
    }

    /** Kadr rasy i podglad postaci (z prawdziwym ekwipunkiem) — bez sciemnienia. */
    private void showScene(SceneView view) {
      Player player = view.player();
      if (player == null) {
        return;
      }
      Entry e = shownEntry(view);
      if (e == null) {
        hidePreview(view);
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
    }

    /** Chowa podglad pod ziemie (nosiciele zostaja — powrot do postaci bez migania skorki). */
    private void hidePreview(SceneView view) {
      Player player = view.player();
      if (player != null && view.get(PREVIEW_KEY) != null) {
        dummies.moveAnchor(player, view.subject().clone().subtract(0, Ustawienia.d("wybor.schowaj-podglad-bloki", HIDE_DROP), 0));
      }
    }

    private Component sideList(SceneView view) {
      ProfileListImpl list = list(view);
      Component out = Teksty.t("wybor.panel.naglowek-licznik", "&fTwoje postacie &7{zajete}/{wszystkie}  &6[A/D]",
          "zajete", list == null ? entries.size() : list.getProfiles().size(),
          "wszystkie", list == null ? "?" : list.getProfileSlots());
      int current = Math.floorMod(index, count(view));
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
      List<Extra> ex = extras(view);
      boolean full = list != null && list.getProfiles().size() >= list.getProfileSlots();
      for (int k = 0; k < ex.size(); k++) {
        boolean on = entries.size() + k == current;
        boolean isNew = ex.get(k) == Extra.NEW;
        NamedTextColor color = isNew
            ? (full ? (on ? NamedTextColor.GRAY : NamedTextColor.DARK_GRAY) : (on ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN))
            : (on ? NamedTextColor.RED : NamedTextColor.DARK_RED);
        String label = isNew
            ? (full ? Teksty.plain("wybor.panel.nowa-limit", "+ Nowa postać (limit)") : Teksty.plain("wybor.panel.nowa", "+ Nowa postać"))
            : Teksty.plain("wybor.panel.usun", "✖ Usuń postacie");
        out = out.append(Component.newline())
            .append(Component.text(on ? "▶ " : "   ", NamedTextColor.GOLD))
            .append(Component.text(label, color).decoration(TextDecoration.BOLD, on));
      }
      return out;
    }

    /**
     * A/D: kursor przesuwa sie OD RAZU (lista, karta, dzwiek), bez blokady i bez czerni —
     * mozna klikac do woli. Kadr i podglad dochodza, gdy gracz na chwile przestanie
     * ({@link #SCENE_DELAY_TICKS}); kazde kolejne klikniecie przesuwa ten termin.
     */
    private void move(SceneView view, int delta) {
      int n = count(view);
      if (n < 2) {
        return;
      }
      index = Math.floorMod(index + delta, n);
      view.uiSound("character");
      showText(view);
      int token = ++sceneToken;
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (!view.isClosed() && view.controller() == this && token == sceneToken) {
          showScene(view);
        }
      }, Ustawienia.l("wybor.scena-po-ticki", SCENE_DELAY_TICKS));
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
      view.uiSound("select");
      Extra extra = extraAt(view);
      if (extra == Extra.NEW) {
        startCreation(view);
        return;
      }
      if (extra == Extra.MANAGE) {
        manage(view, null);
        return;
      }
      Entry e = shownEntry(view);
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
            tileTooltip(e, false), () -> play(view, e)).width(tileWidth()));
      }
      addNewAndManage(list, buttons, () -> startCreation(view), () -> manage(view, null));
      buttons.add(new SceneMenu.Button(Teksty.t("wybor.przycisk.wroc-do-sceny", "Wróć do sceny"), null, () -> { }));
      menu.form(player, listTitle(list), listBody(player, list, false), List.of(), buttons, 2);
    }

    /**
     * Zaznaczanie wielu postaci do usuniecia. Siatka przyciskow-przelacznikow jak w liscie
     * ("☐/☑ Imie · poz."), klik przelacza i odswieza okno; akcje w PIERWSZYM rzedzie, zeby byly
     * na widoku bez przewijania nawet przy 15 postaciach. (Pierwsza wersja miala pola wyboru
     * Papera: klient centruje je po dlugosci etykiety — zygzak — a akcje lady pod lista.)
     */
    void manage(SceneView view, Component note) {
      Player player = view.player();
      ProfileListImpl list = player == null ? null : ProfileListImpl.getOrNull(player);
      if (list == null) {
        return;
      }
      chosen.removeIf(id -> indexOf(entries, id) < 0);
      List<Component> body = new ArrayList<>();
      if (note != null) {
        body.add(note);
      }
      body.add(entries.isEmpty()
          ? Teksty.t("wybor.zarzadzaj.brak-postaci", "&fNie masz aktywnych postaci.")
          : Teksty.t("wybor.zarzadzaj.opis-na-zawsze", "&fKliknij postacie, żeby je zaznaczyć. Usunięcie jest ostateczne ({zajete}/{wszystkie} miejsc zajętych).",
              "zajete", list.getProfiles().size(), "wszystkie", list.getProfileSlots()));
      List<SceneMenu.Button> buttons = new ArrayList<>();
      if (!entries.isEmpty()) {
        buttons.add(new SceneMenu.Button(Component.text(Teksty.plain("wybor.zarzadzaj.usun-zaznaczone", "Usuń zaznaczone ({ile})", "ile", chosen.size()),
            chosen.isEmpty() ? NamedTextColor.DARK_GRAY : NamedTextColor.RED),
            Teksty.t("wybor.pyta-o-potwierdzenie", "Pyta o potwierdzenie"), () -> {
              List<Entry> picked = new ArrayList<>();
              for (Entry e : entries) {
                if (chosen.contains(e.id())) {
                  picked.add(e);
                }
              }
              if (picked.isEmpty()) {
                manage(view, Teksty.t("wybor.nic-nie-zaznaczono", "&eNic nie zaznaczono — kliknij postać na liście."));
              } else {
                confirm(view, picked, null);
              }
            }).width(tileWidth()));
      }
      for (Entry e : entries) {
        boolean on = chosen.contains(e.id());
        buttons.add(new SceneMenu.Button(toggleLabel(e, on), null, () -> {
          if (!chosen.remove(e.id())) {
            chosen.add(e.id());
          }
          manage(view, null);
        }).width(tileWidth()));
      }
      buttons.add(new SceneMenu.Button(Teksty.t("wybor.przycisk.wroc", "Wróć"), null, () -> {
        chosen.clear();
        grid(view);
      }));
      menu.form(player, Teksty.t("wybor.zarzadzaj.tytul", "&6Zarządzaj postaciami"), body, List.of(), buttons, 2);
    }

    /** Potwierdzenie: lista wybranych, a przy postaciach od {@code imie-od-poziomu} pole na imie. */
    private void confirm(SceneView view, List<Entry> chosen, Component error) {
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
      body.add(Teksty.t("wybor.potwierdz.na-zawsze", "&cTe postacie znikną NA ZAWSZE razem z ekwipunkiem i postępem. Tego nie da się cofnąć."));
      List<DialogInput> inputs = new ArrayList<>();
      List<Entry> needName = new ArrayList<>();
      for (Entry e : chosen) {
        if (e.level() >= settings.confirmNameFromLevel()) {
          inputs.add(DialogInput.text("p" + needName.size(),
              Teksty.t("wybor.potwierdz.wpisz-imie", "Wpisz imię „{imie}” (poziom {poziom})", "imie", e.name(), "poziom", e.level()))
              .maxLength(32).build());
          needName.add(e);
        }
      }
      List<SceneMenu.Button> buttons = List.of(
          SceneMenu.Button.submit(Teksty.t("wybor.potwierdz.przycisk-na-zawsze", "&cUsuń na zawsze"), null, r -> {
                for (int i = 0; i < needName.size(); i++) {
                  String typed = r == null ? null : r.getText("p" + i);
                  if (!SelectOrder.nameMatches(typed, needName.get(i).name())) {
                    confirm(view, chosen, Teksty.t("wybor.potwierdz.zle-imie",
                        "&cImię „{imie}” się nie zgadza — nic nie usunięto.", "imie", needName.get(i).name()));
                    return;
                  }
                }
                purge(view, chosen);
              }),
          new SceneMenu.Button(Teksty.t("wybor.przycisk.anuluj", "Anuluj"), null, () -> manage(view, null)));
      menu.form(player, Teksty.t("wybor.potwierdz.tytul-na-zawsze", "&cUsunąć na zawsze?"), body, inputs, buttons, 2);
    }

    /**
     * Usuniecie na zawsze. Po nim scena odswieza sie OD RAZU (lista po lewej, karuzela A/D):
     * wczesniej po usunieciu wszystkich postaci panel zostawal stary, a A/D przy jednym miejscu
     * nic nie robilo — "+ Nowa postac" bylo widac dopiero pod PPM (zgloszenie 01.10).
     */
    private void purge(SceneView view, List<Entry> chosen) {
      Player player = view.player();
      ProfileListImpl list = player == null ? null : ProfileListImpl.getOrNull(player);
      if (list == null) {
        return;
      }
      List<Entry> before = List.copyOf(entries);
      List<String> gone = new ArrayList<>();
      List<String> failed = new ArrayList<>();
      for (Entry e : chosen) {
        if (removeForever(player, list, e.profile(), e.name())) {
          gone.add(e.name());
        } else {
          failed.add(e.name());
        }
      }
      this.chosen.clear();
      refresh(view, before);
      Component note = Component.empty();
      if (!gone.isEmpty()) {
        note = note.append(Teksty.t("wybor.kosz.usunieto", "&aUsunięto na zawsze: {imiona}.", "imiona", String.join(", ", gone)));
      }
      if (!failed.isEmpty()) {
        note = note.append(Component.text(gone.isEmpty() ? "" : " ")).append(Teksty.t("wybor.kosz.blad-usuwania",
            "&cNie udało się usunąć: {imiona} — zgłoś to administracji.", "imiona", String.join(", ", failed)));
      }
      if (entries.isEmpty()) {
        // Nic nie zostalo do zarzadzania — lista z "+ Nowa postac" zamiast pustego okna.
        grid(view);
      } else {
        manage(view, note);
      }
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

    @Override
    public void onClose(SceneView view) {
      dropPreview(view);
    }
  }

  // --- wspolne: kafelki, karta, akcje --------------------------------------------------

  /** "+ Nowa postac" (albo "Brak miejsc") i "Zarzadzaj postaciami" — wspolne dla listy w scenie i w grze. */
  private void addNewAndManage(ProfileListImpl list, List<SceneMenu.Button> buttons, Runnable create, Runnable manage) {
    int used = list.getProfiles().size();
    int slots = list.getProfileSlots();
    if (used < slots) {
      buttons.add(new SceneMenu.Button(Teksty.t("wybor.lista.nowa", "&a+ Nowa postać"),
          Teksty.t("wybor.lista.wolne-miejsca", "Wolne miejsca: {ile}", "ile", slots - used), create).width(tileWidth()));
    } else {
      buttons.add(new SceneMenu.Button(Teksty.t("wybor.lista.brak-miejsc-licznik", "&8Limit postaci {zajete}/{wszystkie}",
              "zajete", used, "wszystkie", slots),
          Teksty.t("wybor.lista.brak-miejsc-dymek", "Usuń postać w „Zarządzaj”, żeby zrobić miejsce."), manage).width(tileWidth()));
    }
    if (used > 0) {
      buttons.add(new SceneMenu.Button(Teksty.t("wybor.lista.zarzadzaj", "&6Zarządzaj postaciami"),
          Teksty.t("wybor.lista.zarzadzaj-dymek-usun", "Usuń jedną albo kilka postaci naraz"), manage).width(tileWidth()));
    }
  }

  private static Component listTitle(ProfileListImpl list) {
    return Teksty.t("wybor.lista.tytul", "&6Twoje postacie  &7{zajete}/{wszystkie}",
        "zajete", list.getProfiles().size(), "wszystkie", list.getProfileSlots());
  }

  private List<Component> listBody(Player player, ProfileListImpl list, boolean inGame) {
    if (entries(list).isEmpty()) {
      return List.of(Teksty.t("wybor.lista.brak-postaci", "&fNie masz jeszcze postaci — stwórz pierwszą."));
    }
    return List.of(inGame
        ? Teksty.t("wybor.lista.opis-w-grze", "&fKliknij postać, żeby od razu nią grać. &a▶&f = grasz teraz.")
        : Teksty.t("wybor.lista.opis", "&fKliknij postać, żeby grać. &6★&f = ostatnio grana."));
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
    return out;
  }

  private Component tileTooltip(Entry e, boolean playing) {
    CreatorConfig.RaceOption race = e.record() == null ? null : config.races().get(e.record().raceId());
    CreatorConfig.ClassOption clazz = e.record() == null ? null : config.classes().get(e.record().classId());
    return card(e, race, clazz, false, playing);
  }

  /** Przelacznik: "☐ Imie  12 · klasa" / "☑ ..." (☐☑ z BMP — klient ma dla nich glif). */
  private Component toggleLabel(Entry e, boolean on) {
    return toggleLabel(e, on, true);
  }

  private Component toggleLabel(Entry e, boolean on, boolean withLevel) {
    Component out = Component.text(on ? "☑ " : "☐ ", on ? NamedTextColor.GREEN : NamedTextColor.GRAY)
        .append(Component.text(e.name(), iconColor(e)).decoration(TextDecoration.BOLD, true));
    if (withLevel && e.level() > 0) {
      out = out.append(Component.text("  " + e.level(), NamedTextColor.YELLOW));
    }
    return out;
  }

  private Component card(Entry e, CreatorConfig.RaceOption race, CreatorConfig.ClassOption clazz,
      boolean lastUsed, boolean playing) {
    Component out = Component.text(e.name(), iconColor(e)).decoration(TextDecoration.BOLD, true);
    if (playing) {
      out = out.append(Teksty.t("wybor.karta.grasz-teraz", "&a  ▶ grasz teraz").decoration(TextDecoration.BOLD, false));
    } else if (lastUsed) {
      out = out.append(Teksty.t("wybor.karta.ostatnio-grana", "&6  ★ ostatnio grana").decoration(TextDecoration.BOLD, false));
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
      out = out.append(Teksty.t("wybor.karta.specjalizacja", "&7Specjalizacja: "))
          .append(Component.text(className, NamedTextColor.WHITE)).append(Component.newline());
    }
    if (e.level() > 0) {
      out = out.append(Teksty.t("wybor.karta.poziom", "&7Poziom: "))
          .append(Component.text(String.valueOf(e.level()), NamedTextColor.YELLOW)).append(Component.newline());
    }
    if (e.place() != null) {
      out = out.append(Teksty.t("wybor.karta.miejsce", "&7Miejsce: "))
          .append(Component.text(e.place(), NamedTextColor.WHITE)).append(Component.newline());
    }
    out = out.append(Teksty.t("wybor.karta.ostatnio", "&7Ostatnio: "))
        .append(Component.text(playing ? Teksty.plain("wybor.karta.teraz", "teraz") : SelectOrder.when(e.lastPlayed(), Instant.now(), ZONE),
            NamedTextColor.WHITE)).append(Component.newline());
    return out.append(Teksty.t("wybor.karta.czas-gry", "&7Czas gry: "))
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
      player.sendActionBar(Teksty.t("wybor.juz-grasz", "&eJuż grasz postacią {imie}.", "imie", target.name()));
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
            // Zamiast angielskiego "Profile selected" z MMOProfiles (skrypty wyciszone w pakiecie).
            online.sendMessage(Teksty.t("wybor.grasz-teraz", "&7Grasz teraz postacią {imie}&7.",
                "imie", LEGACY.serialize(Component.text(target.name(), iconColor(target)).decoration(TextDecoration.BOLD, true))));
          }
        }));
  }

  private void startCreation(SceneView view) {
    Player player = view.player();
    if (player == null) {
      return;
    }
    ProfileListImpl current = ProfileListImpl.getOrNull(player);
    if (current != null && current.getProfiles().size() >= current.getProfileSlots()) {
      // MMOProfiles i tak by odmowil, a zamknieta scena zostawilaby gracza bez postaci.
      if (view.controller() instanceof SelectStep step) {
        step.manage(view, Teksty.t("wybor.brak-miejsc-licznik", "&eLimit postaci: {zajete}/{wszystkie} — usuń postać, żeby stworzyć nową.",
            "zajete", current.getProfiles().size(), "wszystkie", current.getProfileSlots()));
      }
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
   *     CharacterProfileModule#onRemove, ewentualny stary wpis kosza — ta metoda)
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
    UUID owner = player.getUniqueId();
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!future.isDone()) {
        logger.warning("removeProfile " + id + ": future nie domkniety po 5 s — moduly bez walidacji "
            + "ProfileRemoveEvent: " + guard.pendingModules(id) + ". Profil i tak zniknal z listy.");
      }
      guard.forget(id);
      // Po 5 s: zapisy modulow po wyjsciu z postaci juz poszly. Nigdy dla id == UUID konta
      // (profil-duch MMOProfiles) ani dla profilu, ktory jakims cudem wrocil na liste.
      ProfileListImpl now = ProfileListImpl.getOrNull(player);
      if (id.equals(owner) || (now != null && now.getProfile(id) != null)) {
        logger.warning("Nie archiwizuje plikow postaci " + id + " — profil nadal istnieje albo to UUID konta.");
        return;
      }
      List<String> moved = pl.mcmmorpg.charactercreator.select.ProfileLeftovers.archive(
          plugin.getDataFolder().getParentFile(), new java.io.File(plugin.getDataFolder(), "usuniete"), id,
          settings.leftovers());
      logger.info("Usunieta postac " + name + " (" + id + "): pliki modulow przeniesione do "
          + "CharacterCreator/usuniete/" + id + "/: " + (moved.isEmpty() ? "(brak)" : String.join(", ", moved)));
    }, 100L);
    return true;
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
