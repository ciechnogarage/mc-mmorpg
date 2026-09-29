package pl.mcmmorpg.charactercreator.scene;

import fr.phoenixdevt.mmoprofiles.bukkit.profile.PlayerProfileImpl;
import fr.phoenixdevt.mmoprofiles.bukkit.profile.ProfileListImpl;
import fr.phoenixdevt.profiles.PlayerProfile;
import fr.phoenixdevt.profiles.event.ProfileUnloadEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.mcmmorpg.charactercreator.CreatorConfig;
import pl.mcmmorpg.charactercreator.creator.PreviewDummyManager;
import pl.mcmmorpg.charactercreator.persist.CharacterRecord;
import pl.mcmmorpg.charactercreator.persist.CharacterStore;

/**
 * Etap 3: scena wyboru postaci zamiast okna MMOProfiles.
 *
 * <p>Ostatnio grana postac stoi w scenerii swojej rasy, w swojej skorce. Karta: imie w kolorze
 * ikony, klasa rasowa, poziom, czas gry. Lista po lewej: wszystkie postacie i wolne miejsca.
 * A/D przeglada postacie (pod sciemnieniem), LPM gra, PPM otwiera ksiege postaci (graj / nowa /
 * usun). MMOProfiles zostaje magazynem postaci — wybor i tworzenie ida jego wlasnym API, tym
 * samym, ktorego uzywa jego okno ({@code applyProfile}, {@code createProfile}, {@code removeProfile}).
 */
public final class CharacterSelect {

  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
  private static final String PREVIEW_KEY = "cc.select.preview";

  private final Plugin plugin;
  private final Logger logger;
  private final SceneManager scenes;
  private final SceneMenu menu;
  private final CreatorConfig config;
  private final CharacterStore store;
  private final PreviewDummyManager dummies;

  /** Jedna postac na liscie: profil MMOProfiles + nasz rekord (null = postac sprzed kreatora). */
  private record Entry(PlayerProfileImpl profile, CharacterRecord record) {

    String name() {
      return record != null ? record.name() : profile.getName();
    }

    Instant lastUsed() {
      if (record != null) {
        return record.effectiveLastUsed();
      }
      return Instant.ofEpochMilli(profile.getLastTimePlayed());
    }
  }

  public CharacterSelect(Plugin plugin, Logger logger, SceneManager scenes, SceneMenu menu,
      CreatorConfig config, CharacterStore store, PreviewDummyManager dummies) {
    this.plugin = plugin;
    this.logger = logger;
    this.scenes = scenes;
    this.menu = menu;
    this.config = config;
    this.store = store;
    this.dummies = dummies;
  }

  /**
   * Otwiera scene wyboru. Gracz BEZ postaci idzie od razu do kreatora — nie ma czego wybierac.
   * Gracz W TRAKCIE gry najpierw wychodzi z postaci (jak przy oknie MMOProfiles).
   *
   * @return false, gdy sie nie dalo (brak danych MMOProfiles) — wolajacy moze otworzyc okno MMOProfiles
   */
  public boolean open(Player player) {
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
              openFresh(online);
            }
          }));
      return true;
    }
    return openFresh(player);
  }

  private boolean openFresh(Player player) {
    ProfileListImpl list = ProfileListImpl.getOrNull(player);
    if (list == null) {
      return false;
    }
    List<Entry> entries = entries(list);
    if (entries.isEmpty()) {
      createNew(player, list);
      return true;
    }
    SceneSettings.Lineup lineup = scenes.settings().lineup();
    if (lineup != null && scenes.settings().points().containsKey(lineup.point())) {
      return scenes.open(player, new LineupStep(entries, list.getProfileSlots(), lineup));
    }
    return scenes.open(player, new SelectStep(entries, list.getProfileSlots()));
  }

  private List<Entry> entries(ProfileListImpl list) {
    List<Entry> out = new ArrayList<>();
    for (PlayerProfile p : list.getProfiles()) {
      if (p instanceof PlayerProfileImpl impl) {
        out.add(new Entry(impl, store.get(impl.getUniqueId())));
      }
    }
    // Ostatnio grana pierwsza — to ja gracz zobaczy od razu, jak u Wynna.
    out.sort(Comparator.comparing(Entry::lastUsed).reversed());
    return out;
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

  // --- scena -----------------------------------------------------------------------

  private final class SelectStep implements SceneController {
    private final List<Entry> entries;
    private final int slots;
    private int index;

    SelectStep(List<Entry> entries, int slots) {
      this.entries = new ArrayList<>(entries);
      this.slots = slots;
    }

    private Entry current() {
      return entries.get(Math.floorMod(index, entries.size()));
    }

    @Override
    public void onOpen(SceneView view) {
      show(view);
    }

    private void show(SceneView view) {
      Entry e = current();
      CreatorConfig.RaceOption race = e.record() == null ? null : config.races().get(e.record().raceId());
      CreatorConfig.ClassOption clazz = e.record() == null ? null : config.classes().get(e.record().classId());
      view.point(race == null ? SceneSettings.DEFAULT_POINT : race.id());
      showPreview(view, race, clazz);
      view.topBar(Component.text("Wybierz postać", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
      view.list(list());
      view.card(cardFor(e, race, clazz));
      view.hint(CreatorFlow.hint("A/D", "Postać " + (Math.floorMod(index, entries.size()) + 1) + "/"
          + entries.size(), "LPM", "Graj", "PPM", "Księga postaci"));
    }

    private Component list() {
      Component out = Component.text("Twoje postacie  ", NamedTextColor.WHITE)
          .append(Component.text("[A/D]", NamedTextColor.GOLD));
      int current = Math.floorMod(index, entries.size());
      for (int i = 0; i < entries.size(); i++) {
        Entry e = entries.get(i);
        boolean on = i == current;
        out = out.append(Component.newline())
            .append(Component.text(on ? "▶ " : "   ", NamedTextColor.GOLD))
            .append(Component.text("■ ", iconColor(e)))
            .append(Component.text(e.name(), on ? NamedTextColor.WHITE : NamedTextColor.GRAY)
                .decoration(TextDecoration.BOLD, on));
      }
      int free = Math.max(0, slots - entries.size());
      out = out.append(Component.newline()).append(Component.newline())
          .append(Component.text(free > 0 ? "Wolne miejsca: " + free : "Brak wolnych miejsc",
              NamedTextColor.DARK_GRAY));
      return out;
    }

    @Override
    public void onPrevious(SceneView view) {
      if (entries.size() > 1) {
        view.fade(() -> {
          index--;
          show(view);
        });
      }
    }

    @Override
    public void onNext(SceneView view) {
      if (entries.size() > 1) {
        view.fade(() -> {
          index++;
          show(view);
        });
      }
    }

    @Override
    public void onSelect(SceneView view) {
      play(view, current());
    }

    @Override
    public void onBack(SceneView view) {
      Player player = view.player();
      if (player == null) {
        return;
      }
      List<SceneMenu.Button> buttons = new ArrayList<>();
      for (int i = 0; i < entries.size(); i++) {
        Entry e = entries.get(i);
        buttons.add(new SceneMenu.Button(
            Component.text("■ ", iconColor(e)).append(Component.text(e.name(), NamedTextColor.WHITE)),
            Component.text("Graj tą postacią"), () -> play(view, e)));
      }
      if (entries.size() < slots) {
        buttons.add(new SceneMenu.Button(Component.text("+ Nowa postać", NamedTextColor.GREEN),
            Component.text("Otwiera kreator postaci"), () -> startCreation(view)));
      }
      Entry shown = current();
      buttons.add(new SceneMenu.Button(Component.text("Usuń: " + shown.name(), NamedTextColor.RED),
          Component.text("Pyta o potwierdzenie"), () -> confirmDelete(view, shown)));
      buttons.add(new SceneMenu.Button(Component.text("Wróć"), null, () -> { }));
      menu.show(player, Component.text("Księga postaci", NamedTextColor.GOLD), null, buttons, 2);
    }

    private void confirmDelete(SceneView view, Entry entry) {
      Player player = view.player();
      if (player == null) {
        return;
      }
      menu.show(player, Component.text("Usunąć postać?", NamedTextColor.RED),
          Component.text("Postać ", NamedTextColor.WHITE)
              .append(Component.text(entry.name(), iconColor(entry)).decoration(TextDecoration.BOLD, true))
              .append(Component.text(" zniknie na zawsze razem z ekwipunkiem i postępem. "
                  + "Tego nie da się cofnąć.", NamedTextColor.WHITE)),
          List.of(
              new SceneMenu.Button(Component.text("Usuń na zawsze", NamedTextColor.RED), null,
                  () -> delete(view, entry)),
              new SceneMenu.Button(Component.text("Anuluj"), null, () -> { })), 1);
    }

    private void delete(SceneView view, Entry entry) {
      Player player = view.player();
      ProfileListImpl list = player == null ? null : ProfileListImpl.getOrNull(player);
      if (list == null) {
        return;
      }
      logger.info("Scena: " + player.getName() + " usuwa postac " + entry.name() + " (profil "
          + entry.profile().getUniqueId() + ")");
      list.removeProfile(entry.profile()).exceptionally(ex -> {
        logger.warning("Usuwanie postaci nie powiodlo sie: " + ex);
        return null;
      });
      // NIE czekamy na future z removeProfile: na klonie profil znikal z listy MMOProfiles,
      // a future nie domykal sie wcale (scena stala z usunieta postacia). Patrzymy na liste.
      UUID removed = entry.profile().getUniqueId();
      int[] tries = {0};
      Bukkit.getScheduler().runTaskTimer(plugin, task -> {
        boolean gone = list.getProfiles().stream().noneMatch(p -> p.getUniqueId().equals(removed));
        if (view.isClosed() || gone || ++tries[0] > 20) {
          task.cancel();
          if (view.isClosed()) {
            return;
          }
          if (!gone) {
            logger.warning("Postac " + entry.name() + " nadal na liscie MMOProfiles po 5 s.");
          }
          entries.clear();
          entries.addAll(entries(list));
          if (entries.isEmpty()) {
            startCreation(view);
          } else {
            index = 0;
            view.fade(() -> show(view));
          }
        }
      }, 5L, 5L);
    }

    @Override
    public void onClose(SceneView view) {
      dropPreview(view);
    }
  }

  // --- scena wyboru: wszystkie postacie przy ognisku ---------------------------------

  /**
   * Wszystkie postacie konta stoja naraz w luku wokol ogniska, przodem na zewnatrz; kamera
   * stoi przed wybrana i obiega krag przy A/D (plynnie, kroki interpolowane po stronie
   * klienta). Wolne miejsce na koncu luku to "Nowa postac" — LPM na nim otwiera kreator.
   * Nad kazda postacia imie w kolorze jej ikony. Karta, lista i ksiega (PPM) jak w
   * {@link SelectStep}.
   */
  private final class LineupStep implements SceneController {
    private static final double MAX_STEP_DEGREES = 6.0;

    private final List<Entry> entries;
    private final int slots;
    private final SceneSettings.Lineup lineup;
    private final List<org.bukkit.entity.LivingEntity> figures = new ArrayList<>();
    private final List<Entity> labels = new ArrayList<>();
    private org.bukkit.entity.LivingEntity ghost;
    private int index;

    LineupStep(List<Entry> entries, int slots, SceneSettings.Lineup lineup) {
      this.entries = new ArrayList<>(entries);
      this.slots = slots;
      this.lineup = lineup;
    }

    private boolean hasFreeSlot() {
      return entries.size() < slots;
    }

    private int count() {
      return entries.size() + (hasFreeSlot() ? 1 : 0);
    }

    private int current() {
      return Math.floorMod(index, count());
    }

    private boolean onFreeSlot() {
      return current() >= entries.size();
    }

    @Override
    public void onOpen(SceneView view) {
      Player player = view.player();
      if (player == null) {
        return;
      }
      spawnAll(view, player);
      view.placeCamera(lineup.camera(lineup.angle(current(), count())), lineup.point());
      view.topBar(Component.text("Wybierz postać", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
      refresh(view);
    }

    private void spawnAll(SceneView view, Player player) {
      clear();
      int n = count();
      for (int i = 0; i < entries.size(); i++) {
        Entry e = entries.get(i);
        CreatorConfig.RaceOption race = e.record() == null ? null : config.races().get(e.record().raceId());
        CreatorConfig.ClassOption clazz = e.record() == null ? null : config.classes().get(e.record().classId());
        Location spot = lineup.spot(i, n);
        org.bukkit.entity.LivingEntity figure = dummies.spawnFigure(player, spot, race, clazz);
        view.own(figure);
        figures.add(figure);
        labels.add(label(view, spot.clone().add(0, 2.15, 0),
            Component.text(e.name(), iconColor(e)).decoration(TextDecoration.BOLD, true)));
      }
      if (hasFreeSlot()) {
        Location spot = lineup.spot(entries.size(), n);
        ghost = dummies.spawnGhost(player, spot);
        view.own(ghost);
        labels.add(label(view, spot.clone().add(0, 2.15, 0),
            Component.text("+ Nowa postać", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true)));
      }
    }

    private Entity label(SceneView view, Location at, Component text) {
      org.bukkit.entity.TextDisplay d = at.getWorld().spawn(at, org.bukkit.entity.TextDisplay.class, e -> {
        e.setPersistent(false);
        e.setVisibleByDefault(false);
        e.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        e.setShadowed(true);
        e.setAlignment(org.bukkit.entity.TextDisplay.TextAlignment.CENTER);
        e.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        e.text(text);
      });
      view.own(d);
      return d;
    }

    private void clear() {
      for (Entity e : labels) {
        if (e.isValid()) {
          e.remove();
        }
      }
      labels.clear();
      figures.clear();
      ghost = null;
    }

    /** Karta, lista, podpowiedz i ozywiana postac dla biezacego miejsca. */
    private void refresh(SceneView view) {
      int i = current();
      dummies.setGhostGlow(ghost, onFreeSlot());
      view.list(list());
      if (onFreeSlot()) {
        view.subjectEntity(ghost);
        view.card(Component.text("Nowa postać", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true)
            .append(Component.newline()).append(Component.newline())
            .append(Component.text("Wolne miejsce przy ognisku. Wybierz rasę i klasę, "
                + "nadaj imię i ruszaj w drogę.", NamedTextColor.WHITE))
            .append(Component.newline()).append(Component.newline())
            .append(Component.text("Wolne miejsca: " + (slots - entries.size()), NamedTextColor.GRAY)));
        view.hint(CreatorFlow.hint("A/D", "Postać", "LPM", "Stwórz postać", "PPM", "Księga postaci"));
        return;
      }
      Entry e = entries.get(i);
      view.subjectEntity(figures.get(i));
      CreatorConfig.RaceOption race = e.record() == null ? null : config.races().get(e.record().raceId());
      CreatorConfig.ClassOption clazz = e.record() == null ? null : config.classes().get(e.record().classId());
      view.card(cardFor(e, race, clazz));
      view.hint(CreatorFlow.hint("A/D", "Postać " + (i + 1) + "/" + entries.size(), "LPM", "Graj",
          "PPM", "Księga postaci"));
    }

    private Component list() {
      Component out = Component.text("Twoje postacie  ", NamedTextColor.WHITE)
          .append(Component.text("[A/D]", NamedTextColor.GOLD));
      int current = current();
      for (int i = 0; i < entries.size(); i++) {
        Entry e = entries.get(i);
        boolean on = i == current;
        out = out.append(Component.newline())
            .append(Component.text(on ? "▶ " : "   ", NamedTextColor.GOLD))
            .append(Component.text("■ ", iconColor(e)))
            .append(Component.text(e.name(), on ? NamedTextColor.WHITE : NamedTextColor.GRAY)
                .decoration(TextDecoration.BOLD, on));
      }
      if (hasFreeSlot()) {
        boolean on = onFreeSlot();
        out = out.append(Component.newline())
            .append(Component.text(on ? "▶ " : "   ", NamedTextColor.GOLD))
            .append(Component.text("+ Nowa postać", on ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.BOLD, on));
      }
      return out;
    }

    /** Kamera po luku z kata obecnego miejsca do nowego, najkrotsza droga w kregu. */
    private void move(SceneView view, int delta) {
      if (count() < 2) {
        return;
      }
      int n = count();
      double from = lineup.angle(current(), n);
      index += delta;
      double to = lineup.angle(current(), n);
      int steps = Math.max(1, (int) Math.ceil(Math.abs(to - from) / MAX_STEP_DEGREES));
      List<Location> path = new ArrayList<>();
      for (int k = 1; k <= steps; k++) {
        path.add(lineup.camera(from + (to - from) * k / steps));
      }
      view.uiSound("character");
      view.glideCamera(path, lineup.stepTicks());
      refresh(view);
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
    public void onSelect(SceneView view) {
      view.uiSound("select");
      if (onFreeSlot()) {
        startCreation(view);
      } else {
        play(view, entries.get(current()));
      }
    }

    @Override
    public void onBack(SceneView view) {
      Player player = view.player();
      if (player == null) {
        return;
      }
      view.uiSound("back");
      List<SceneMenu.Button> buttons = new ArrayList<>();
      for (Entry e : entries) {
        buttons.add(new SceneMenu.Button(
            Component.text("■ ", iconColor(e)).append(Component.text(e.name(), NamedTextColor.WHITE)),
            Component.text("Graj tą postacią"), () -> play(view, e)));
      }
      if (hasFreeSlot()) {
        buttons.add(new SceneMenu.Button(Component.text("+ Nowa postać", NamedTextColor.GREEN),
            Component.text("Otwiera kreator postaci"), () -> startCreation(view)));
      }
      if (!onFreeSlot()) {
        Entry shown = entries.get(current());
        buttons.add(new SceneMenu.Button(Component.text("Usuń: " + shown.name(), NamedTextColor.RED),
            Component.text("Pyta o potwierdzenie"), () -> confirmDeleteLineup(view, shown)));
      }
      buttons.add(new SceneMenu.Button(Component.text("Wróć"), null, () -> { }));
      menu.show(player, Component.text("Księga postaci", NamedTextColor.GOLD), null, buttons, 2);
    }

    private void confirmDeleteLineup(SceneView view, Entry entry) {
      Player player = view.player();
      if (player == null) {
        return;
      }
      menu.show(player, Component.text("Usunąć postać?", NamedTextColor.RED),
          Component.text("Postać ", NamedTextColor.WHITE)
              .append(Component.text(entry.name(), iconColor(entry)).decoration(TextDecoration.BOLD, true))
              .append(Component.text(" zniknie na zawsze razem z ekwipunkiem i postępem. "
                  + "Tego nie da się cofnąć.", NamedTextColor.WHITE)),
          List.of(
              new SceneMenu.Button(Component.text("Usuń na zawsze", NamedTextColor.RED), null,
                  () -> deleteLineup(view, entry)),
              new SceneMenu.Button(Component.text("Anuluj"), null, () -> { })), 1);
    }

    private void deleteLineup(SceneView view, Entry entry) {
      removeAndThen(view, entry, fresh -> {
        entries.clear();
        entries.addAll(fresh);
        if (entries.isEmpty()) {
          clear();
          dummies.despawnFigures(view.playerId());
          startCreation(view);
          return;
        }
        index = 0;
        view.fade(() -> {
          Player p = view.player();
          if (p == null) {
            return;
          }
          dummies.despawnFigures(view.playerId());
          spawnAll(view, p);
          view.placeCamera(lineup.camera(lineup.angle(current(), count())), lineup.point());
          refresh(view);
        });
      });
    }

    @Override
    public void onClose(SceneView view) {
      clear();
      dummies.despawnFigures(view.playerId());
    }
  }

private Component cardFor(Entry e, CreatorConfig.RaceOption race, CreatorConfig.ClassOption clazz) {
    Component out = Component.text(e.name(), iconColor(e)).decoration(TextDecoration.BOLD, true);
    String className = e.profile().getPlaceholderCache().getPlaceholder("mmocore_class");
    String level = e.profile().getPlaceholderCache().getPlaceholder("mmocore_level");
    if (level == null || level.isBlank()) {
      level = levelFromFile(e.profile().getUniqueId());
    }
    if (clazz != null && race != null) {
      out = out.append(Component.newline())
          .append(LEGACY.deserialize(clazz.displayFor(race.id())).decoration(TextDecoration.ITALIC, false))
          .append(Component.text("  " + plain(race.display()) + " · " + plain(clazz.display()),
              NamedTextColor.GRAY));
    } else if (className != null && !className.isBlank()) {
      out = out.append(Component.newline()).append(Component.text(className, NamedTextColor.WHITE));
    }
    out = out.append(Component.newline()).append(Component.newline());
    if (className != null && !className.isBlank() && clazz != null && race != null
        && !plain(clazz.displayFor(race.id())).equalsIgnoreCase(className)) {
      // Po 10. poziomie klasa MMOCore to juz specjalizacja — pokazujemy ja osobno.
      out = out.append(Component.text("Specjalizacja: ", NamedTextColor.GRAY))
          .append(Component.text(className, NamedTextColor.WHITE)).append(Component.newline());
    }
    if (level != null && !level.isBlank()) {
      out = out.append(Component.text("Poziom: ", NamedTextColor.GRAY))
          .append(Component.text(level, NamedTextColor.YELLOW)).append(Component.newline());
    }
    long minutes = e.profile().getTimePlayed() / 60_000L;
    out = out.append(Component.text("Czas gry: ", NamedTextColor.GRAY))
        .append(Component.text(minutes >= 60 ? (minutes / 60) + " h " + (minutes % 60) + " min"
            : minutes + " min", NamedTextColor.WHITE));
    return out;
  }

  // --- wspolne dla obu ekranow wyboru ------------------------------------------------

  /**
   * Usuwa postac i po zniknieciu z listy MMOProfiles woła {@code then} z nowa lista.
   * NIE czekamy na future z removeProfile: na klonie profil znikal z listy, a future nie
   * domykal sie wcale — patrzymy na liste.
   */
  private void removeAndThen(SceneView view, Entry entry, java.util.function.Consumer<List<Entry>> then) {
    Player player = view.player();
    ProfileListImpl list = player == null ? null : ProfileListImpl.getOrNull(player);
    if (list == null) {
      return;
    }
    logger.info("Scena: " + player.getName() + " usuwa postac " + entry.name() + " (profil "
        + entry.profile().getUniqueId() + ")");
    list.removeProfile(entry.profile()).exceptionally(ex -> {
      logger.warning("Usuwanie postaci nie powiodlo sie: " + ex);
      return null;
    });
    UUID removed = entry.profile().getUniqueId();
    int[] tries = {0};
    Bukkit.getScheduler().runTaskTimer(plugin, task -> {
      boolean gone = list.getProfiles().stream().noneMatch(p -> p.getUniqueId().equals(removed));
      if (view.isClosed() || gone || ++tries[0] > 20) {
        task.cancel();
        if (view.isClosed()) {
          return;
        }
        if (!gone) {
          logger.warning("Postac " + entry.name() + " nadal na liscie MMOProfiles po 5 s.");
        }
        then.accept(entries(list));
      }
    }, 5L, 5L);
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

  // --- podglad postaci (ta sama stajnia nosicieli skorek co w kreatorze) --------------

  private void showPreview(SceneView view, CreatorConfig.RaceOption race, CreatorConfig.ClassOption clazz) {
    Player player = view.player();
    if (player == null) {
      return;
    }
    if (view.get(PREVIEW_KEY) == null) {
      view.subjectEntity(dummies.start(view.subject(), player, race, clazz));
      for (Entity carrier : dummies.carriers(player.getUniqueId())) {
        view.own(carrier);
      }
      view.put(PREVIEW_KEY, Boolean.TRUE);
    } else {
      dummies.moveAnchor(player, view.subject());
      view.subjectEntity(dummies.select(player, race, clazz));
    }
    dummies.applyAppearance(player, race, clazz);
  }

  private void dropPreview(SceneView view) {
    if (view.get(PREVIEW_KEY) != null) {
      dummies.despawnAll(view.playerId());
      view.put(PREVIEW_KEY, null);
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

  private static TextColor iconColor(Entry e) {
    String id = e.record() == null ? "" : e.record().iconColor();
    TextColor c = NewCharacterBook.COLORS.get(id == null || id.isBlank() ? NewCharacterBook.DEFAULT_COLOR : id);
    return c == null ? NamedTextColor.WHITE : c;
  }

  private static String plain(String legacyText) {
    return (legacyText == null ? "" : legacyText).replaceAll("&[0-9a-fk-orA-FK-OR]", "");
  }
}
