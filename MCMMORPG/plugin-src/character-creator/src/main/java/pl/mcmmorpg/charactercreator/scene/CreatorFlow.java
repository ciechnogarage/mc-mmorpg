package pl.mcmmorpg.charactercreator.scene;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import pl.mcmmorpg.charactercreator.CreatorConfig;
import pl.mcmmorpg.charactercreator.creator.PreviewDummyManager;

/**
 * Kreator w scenie: RASA -> KLASA (decyzja D2: dwa kroki). Co dzieje sie po wyborze, decyduje
 * wolajacy przez {@link Outcome} — etap 1 tylko melduje wybor, etap 2 podepnie ksiege
 * "Nowa postac" i zatwierdzenie profilu.
 *
 * <p>Podglad postaci to ta sama stajnia nosicieli skorek co w starym kreatorze
 * ({@link PreviewDummyManager}): zmiana rasy albo klasy jest teleportem gotowej encji, wiec
 * klient nie rysuje po drodze skorki domyslnej.
 */
public final class CreatorFlow {

  /** Co zrobic, gdy gracz wybral albo zrezygnowal. */
  public interface Outcome {
    void chosen(SceneView view, CreatorConfig.RaceOption race, CreatorConfig.ClassOption clazz);

    void cancelled(SceneView view);

    /**
     * Czy gracz MUSI dokonczyc tworzenie (nie ma zadnej innej postaci). Wtedy PPM nie
     * przerywa — wyjscie i tak wracaloby do kreatora od zera, gubiac wybrana rase i klase.
     */
    default boolean mustCreate(SceneView view) {
      return false;
    }
  }

  /** Po tylu tickach bez A/D dochodzi sceneria rasy i postac (jak w wyborze postaci). */
  private static final long SCENE_DELAY_TICKS = 6L;

  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
  private static final String PREVIEW_KEY = "cc.preview";

  private final CreatorConfig config;
  private final PreviewDummyManager dummies;
  private final Outcome outcome;
  /** Okna nad scena (potwierdzenie przerwania); null = zachowanie sprzed naprawy. */
  private final SceneMenu menu;

  public CreatorFlow(CreatorConfig config, PreviewDummyManager dummies, Outcome outcome) {
    this(config, dummies, outcome, null);
  }

  public CreatorFlow(CreatorConfig config, PreviewDummyManager dummies, Outcome outcome, SceneMenu menu) {
    this.config = config;
    this.dummies = dummies;
    this.outcome = outcome;
    this.menu = menu;
  }

  /**
   * Podsumowanie wyboru do ksiegi "Nowa postac": klasa rasowa, rasa i archetyp,
   * specjalizacje. Krotsze niz karta — ksiega ma miejsce na imie i kolor.
   */
  public Component summary(CreatorConfig.RaceOption race, CreatorConfig.ClassOption clazz) {
    CreatorConfig.RaceVariant v = clazz.variant(race.id());
    Component out = legacy(clazz.displayFor(race.id())).decoration(TextDecoration.BOLD, true)
        .append(Component.text("   " + plain(race.display()) + " · " + plain(clazz.display()),
            NamedTextColor.GRAY));
    Component role = roleLine(clazz, race);
    if (role != null) {
      out = out.append(Component.newline()).append(role);
    }
    if (v != null && !v.specializations().isEmpty()) {
      out = out.append(Component.newline())
          .append(Component.text("Od 10. poziomu: ", NamedTextColor.GRAY));
      for (int i = 0; i < v.specializations().size(); i++) {
        if (i > 0) {
          out = out.append(Component.text(" albo ", NamedTextColor.GRAY));
        }
        out = out.append(Component.text(plain(v.specializations().get(i).name()),
            NamedTextColor.WHITE));
      }
    }
    return out;
  }

  /** "Rola: X   Trudność: ★★☆" albo null, gdy generator tego nie zapisal. */
  private Component roleLine(CreatorConfig.ClassOption clazz, CreatorConfig.RaceOption race) {
    CreatorConfig.CardExtras x = config.cardExtras(clazz.id(), race.id());
    if (x == null || (x.role().isBlank() && x.difficulty() == 0)) {
      return null;
    }
    Component out = Component.empty();
    if (!x.role().isBlank()) {
      out = out.append(Component.text("Rola: ", NamedTextColor.GRAY))
          .append(Component.text(x.role(), NamedTextColor.WHITE));
    }
    if (x.difficulty() > 0) {
      if (!x.role().isBlank()) {
        out = out.append(Component.text("   "));
      }
      out = out.append(Component.text("Trudność: ", NamedTextColor.GRAY))
          .append(Component.text("★".repeat(x.difficulty()), NamedTextColor.GOLD))
          .append(Component.text("★".repeat(3 - x.difficulty()), NamedTextColor.DARK_GRAY));
    }
    return out;
  }

  /** Pierwszy ekran kreatora. */
  public SceneController start() {
    return new PickStep(0, 0);
  }

  // --- wspolne ----------------------------------------------------------------------

  private List<CreatorConfig.RaceOption> races() {
    return new ArrayList<>(config.races().values());
  }

  private List<CreatorConfig.ClassOption> classes() {
    return new ArrayList<>(config.classes().values());
  }

  /** Stawia (raz na scene) stajnie nosicieli i wystawia wybrana pare na kadr. */
  private void showPreview(SceneView view, CreatorConfig.RaceOption race,
      CreatorConfig.ClassOption clazz) {
    Player player = view.player();
    if (player == null) {
      return;
    }
    Location at = view.subject();
    if (view.get(PREVIEW_KEY) == null) {
      view.subjectEntity(dummies.start(at, player, race, clazz));
      for (Entity carrier : dummies.carriers(player.getUniqueId())) {
        view.own(carrier);
      }
      view.put(PREVIEW_KEY, Boolean.TRUE);
    } else {
      dummies.moveAnchor(player, at);
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

  private static Component legacy(String s) {
    return LEGACY.deserialize(s == null ? "" : s).decoration(TextDecoration.ITALIC, false);
  }

  private static Component lines(String multiline, NamedTextColor color) {
    Component out = Component.empty();
    String[] parts = (multiline == null ? "" : multiline).split("\n", -1);
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) {
        out = out.append(Component.newline());
      }
      out = out.append(Component.text(parts[i], color));
    }
    return out;
  }

  /** Podpowiedz: [klawisz] akcja, rozdzielone kropka — na razie tekst, glify w etapie 5. */
  static Component hint(String... pairs) {
    Component out = Component.empty();
    for (int i = 0; i + 1 < pairs.length; i += 2) {
      if (i > 0) {
        out = out.append(Component.text("   ·   ", NamedTextColor.DARK_GRAY));
      }
      out = out.append(Component.text("[" + pairs[i] + "] ", NamedTextColor.GOLD))
          .append(Component.text(pairs[i + 1], NamedTextColor.WHITE));
    }
    return out;
  }

  /** Tekst bez kodow kolorow (&x) — tam, gdzie kolor nadaje karta, a nie config. */
  private static String plain(String legacyText) {
    return nz(legacyText).replaceAll("&[0-9a-fk-orA-FK-OR]", "");
  }

  private static String nz(String s) {
    return s == null ? "" : s;
  }

  // --- jeden ekran: rasa (A/D) x klasa (W/S) -------------------------------------------

  /**
   * Wybor rasy i klasy na jednym ekranie (16 klas = 4 rasy x 4 archetypy).
   *
   * <p>Uklad wzorowany na Wynnie: u gory pasek ras, po lewej lista klas wybranej rasy,
   * po prawej karta klasy ze specjalizacjami. A/D zmienia rase pod sciemnieniem (kazda rasa
   * dostanie w etapie 4 wlasna scenerie) i ZACHOWUJE archetyp — gracz porownuje np.
   * wojownika miedzy rasami. W/S zmienia klase od razu: to tylko przebranie w tym samym miejscu.
   *
   * <p>Dlaczego W/S, a nie Q/E: w kamerze spectatora serwer nie dostaje ani Q (klient
   * spectatora nie wyrzuca przedmiotow), ani E (ekwipunek jest calkowicie po stronie klienta,
   * patrz notatka o klawiszu E). W/S przychodzi tym samym pakietem co A/D.
   */
  private final class PickStep implements SceneController {
    private int race;
    private int clazz;

    PickStep(int race, int clazz) {
      this.race = race;
      this.clazz = clazz;
    }

    private CreatorConfig.RaceOption race() {
      List<CreatorConfig.RaceOption> all = races();
      return all.get(Math.floorMod(race, all.size()));
    }

    private CreatorConfig.ClassOption clazz() {
      List<CreatorConfig.ClassOption> all = classes();
      return all.get(Math.floorMod(clazz, all.size()));
    }

    @Override
    public void onOpen(SceneView view) {
      showRace(view);
    }

    private Component normalHint(SceneView view) {
      return outcome.mustCreate(view)
          ? hint("A/D", "Rasa", "W/S", "Klasa", "LPM", "Dalej")
          : hint("A/D", "Rasa", "W/S", "Klasa", "LPM", "Dalej", "PPM", "Wróć");
    }

    /** Pelne odswiezenie: kadr rasy, postac, oba panele, pasek i podpowiedz. */
    private void showRace(SceneView view) {
      view.point(race().id());
      view.topBar(raceStrip());
      // Przy GUI 4 (1080p) action bar miesci ok. 80 znakow — stad krotkie etykiety.
      view.hint(normalHint(view));
      showClass(view);
    }

    private void showClass(SceneView view) {
      CreatorConfig.RaceOption r = race();
      CreatorConfig.ClassOption c = clazz();
      showPreview(view, r, c);
      view.list(classList(r));
      view.card(classCard(r, c));
    }

    /** « Czlowiek   Ork   Elf   Nieumarly » — wybrana rasa jasna i w nawiasach. */
    private Component raceStrip() {
      List<CreatorConfig.RaceOption> all = races();
      int current = Math.floorMod(race, all.size());
      Component out = Component.text("◀ A    ", NamedTextColor.GOLD);
      for (int i = 0; i < all.size(); i++) {
        if (i > 0) {
          out = out.append(Component.text("    "));
        }
        String name = plain(all.get(i).display());
        out = out.append(i == current
            ? Component.text("[ " + name + " ]", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true)
            : Component.text(name, NamedTextColor.GRAY));
      }
      return out.append(Component.text("    D ▶", NamedTextColor.GOLD));
    }

    /** Lewy panel: rasa i jej cztery klasy, biezaca ze strzalka. */
    private Component classList(CreatorConfig.RaceOption r) {
      Component out = Component.text(plain(r.display()), NamedTextColor.WHITE)
          .decoration(TextDecoration.BOLD, true)
          .append(Component.text("  " + nz(r.bonus()), NamedTextColor.GOLD))
          .append(Component.newline())
          .append(Component.text(nz(r.description()), NamedTextColor.GRAY));
      // Prawdziwe roznice ras (generator klas, funkcja rasowe) — wczesniej karta pokazywala
      // tylko nazwe frakcji i jedno zdanie, a rasa zmienia statystyki.
      for (String trait : config.raceTraits(r.id())) {
        out = out.append(Component.newline()).append(Component.text(trait, NamedTextColor.GREEN));
      }
      out = out.append(Component.newline()).append(Component.newline())
          .append(Component.text("Klasy  ", NamedTextColor.WHITE))
          .append(Component.text("[W/S]", NamedTextColor.GOLD));
      List<CreatorConfig.ClassOption> all = classes();
      int current = Math.floorMod(clazz, all.size());
      for (int i = 0; i < all.size(); i++) {
        CreatorConfig.ClassOption c = all.get(i);
        boolean on = i == current;
        out = out.append(Component.newline())
            .append(Component.text(on ? "▶ " : "   ", NamedTextColor.GOLD))
            .append(on ? legacy(c.displayFor(r.id())).decoration(TextDecoration.BOLD, true)
                : Component.text(plain(c.displayFor(r.id())), NamedTextColor.GRAY))
            .append(Component.text("  " + plain(c.display()).toLowerCase(java.util.Locale.ROOT),
                NamedTextColor.DARK_GRAY));
      }
      return out;
    }

    /**
     * Prawy panel: karta klasy rasowej. Hierarchia (naprawa 30.09): nazwa, rola i trudnosc,
     * jedno zdanie opisu, umiejetnosc na start, sprzet jednym wierszem, specjalizacje po
     * jednym wierszu (nazwa — co daje). Opisy specjalizacji sa w ksiedze i w oknie /class;
     * na karcie robily sciane tekstu.
     */
    private Component classCard(CreatorConfig.RaceOption r, CreatorConfig.ClassOption c) {
      CreatorConfig.RaceVariant v = c.variant(r.id());
      Component card = legacy(c.displayFor(r.id())).decoration(TextDecoration.BOLD, true)
          .append(Component.newline())
          .append(Component.text(plain(r.display()) + " · " + plain(c.display()), NamedTextColor.GRAY));
      Component role = roleLine(c, r);
      if (role != null) {
        card = card.append(Component.newline()).append(role);
      }
      card = card.append(Component.newline()).append(Component.newline());
      if (v == null || v.lore().isBlank()) {
        // Config bez pol sceny (stary generator) — opis wielowierszowy jak w starym kreatorze.
        return card.append(lines(c.descriptionFor(r.id()), NamedTextColor.WHITE))
            .append(Component.newline()).append(Component.newline())
            .append(Component.text(nz(c.bonusFor(r.id())), NamedTextColor.GOLD));
      }
      card = card.append(Component.text(v.lore(), NamedTextColor.WHITE));
      CreatorConfig.CardExtras x = config.cardExtras(c.id(), r.id());
      if (x != null && !x.skillName().isBlank()) {
        card = card.append(Component.newline()).append(Component.newline())
            .append(Component.text(x.skillLevel() <= 1 ? "Na start: " : "Od " + x.skillLevel() + ". poz.: ",
                NamedTextColor.GRAY))
            .append(Component.text(x.skillName(), NamedTextColor.AQUA));
        if (!x.skillText().isBlank()) {
          card = card.append(Component.text(" — " + x.skillText(), NamedTextColor.GRAY));
        }
      }
      card = card.append(Component.newline()).append(Component.newline())
          .append(Component.text("Zbroja: ", NamedTextColor.GRAY))
          .append(Component.text(v.armor(), NamedTextColor.WHITE))
          .append(Component.newline())
          .append(Component.text("Broń: ", NamedTextColor.GRAY))
          .append(Component.text(v.weapons(), NamedTextColor.WHITE));
      if (!v.specializations().isEmpty()) {
        card = card.append(Component.newline()).append(Component.newline())
            .append(Component.text("Od 10. poziomu jedna z dwóch:", NamedTextColor.GOLD));
        for (CreatorConfig.Specialization spec : v.specializations()) {
          // Nazwa zawsze biala, kolor specjalizacji tylko na znaczniku: czesc kolorow
          // z generatora (&5, &8) byla na ciemnej karcie nieczytelna.
          Component marker = legacy(spec.name().replaceAll("^((?:&[0-9a-fA-F])*).*$", "$1") + "◆ ");
          card = card.append(Component.newline())
              .append(marker)
              .append(Component.text(plain(spec.name()), NamedTextColor.WHITE)
                  .decoration(TextDecoration.BOLD, true));
          if (!spec.perks().isBlank()) {
            card = card.append(Component.text(" — " + spec.perks(), NamedTextColor.GREEN));
          }
        }
      }
      return card;
    }

    /** Kolejne A/D (i W/S w trakcie) uniewazniaja zaplanowane doladowanie kadru rasy. */
    private int sceneToken;
    /** Rasa zmieniona, kadr jeszcze nie doszedl — W/S nie stawia wtedy postaci na starej scenerii. */
    private boolean racePending;

    /**
     * A/D jak w wyborze postaci (wlasciciel 30.09: "dobrze to dziala"): pasek ras, lista klas
     * i karta zmieniaja sie OD RAZU, bez sciemnienia i bez polykania klawiszy; sceneria rasy
     * i postac dochodza {@link #SCENE_DELAY_TICKS} po ostatnim kliknieciu.
     */
    private void moveRace(SceneView view, int delta) {
      race += delta;
      view.uiSound("race");
      showText(view);
      scheduleScene(view);
    }

    private void showText(SceneView view) {
      view.topBar(raceStrip());
      view.hint(normalHint(view));
      view.list(classList(race()));
      view.card(classCard(race(), clazz()));
    }

    private void scheduleScene(SceneView view) {
      racePending = true;
      int token = ++sceneToken;
      org.bukkit.Bukkit.getScheduler().runTaskLater(
          org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(CreatorFlow.class), () -> {
            if (!view.isClosed() && view.controller() == this && token == sceneToken) {
              racePending = false;
              showRace(view);
            }
          }, SCENE_DELAY_TICKS);
    }

    @Override
    public void onPrevious(SceneView view) {
      moveRace(view, -1);
    }

    @Override
    public void onNext(SceneView view) {
      moveRace(view, 1);
    }

    private void moveClass(SceneView view, int delta) {
      clazz += delta;
      view.uiSound("class");
      if (racePending) {
        showText(view);
        scheduleScene(view);
      } else {
        showClass(view);
      }
    }

    @Override
    public void onUp(SceneView view) {
      moveClass(view, -1);
    }

    @Override
    public void onDown(SceneView view) {
      moveClass(view, 1);
    }

    @Override
    public void onSelect(SceneView view) {
      view.uiSound("select");
      outcome.chosen(view, race(), clazz());
    }

    /**
     * PPM = "wroc". Gracz bez zadnej postaci nie ma dokad wrocic — dostaje komunikat, a wybor
     * zostaje. Pozostali dostaja pytanie: jeden przypadkowy PPM kasowal caly wybor.
     */
    @Override
    public void onBack(SceneView view) {
      view.uiSound("back");
      Player player = view.player();
      if (outcome.mustCreate(view)) {
        // Na chwile ZAMIAST podpowiedzi na dole — duzy tytul nachodzil na karte klasy (recenzja 30.09).
        view.hint(Component.text("Najpierw stwórz postać  ", NamedTextColor.YELLOW)
            .append(Component.text("[LPM] ", NamedTextColor.GOLD))
            .append(Component.text("Dalej", NamedTextColor.WHITE)));
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(CreatorFlow.class), () -> {
              if (!view.isClosed() && view.controller() == this) {
                view.hint(normalHint(view));
              }
            }, 60L);
        return;
      }
      if (menu == null || player == null) {
        cancel(view);
        return;
      }
      menu.show(player, Component.text("Przerwać tworzenie postaci?", NamedTextColor.GOLD),
          Component.text("Wybrana rasa i klasa nie zostaną zapisane.", NamedTextColor.WHITE),
          List.of(
              new SceneMenu.Button(Component.text("Tak, przerwij", NamedTextColor.RED), null,
                  () -> {
                    if (!view.isClosed()) {
                      cancel(view);
                    }
                  }),
              new SceneMenu.Button(Component.text("Nie, tworzę dalej", NamedTextColor.GREEN), null, () -> { })),
          2);
    }

    private void cancel(SceneView view) {
      dropPreview(view);
      outcome.cancelled(view);
    }

    @Override
    public void onClose(SceneView view) {
      if (view.isClosed()) {
        dropPreview(view);
      }
    }
  }
}
