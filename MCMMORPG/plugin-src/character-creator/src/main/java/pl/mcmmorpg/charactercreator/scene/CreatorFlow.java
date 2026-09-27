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
  }

  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
  private static final String PREVIEW_KEY = "cc.preview";

  private final CreatorConfig config;
  private final PreviewDummyManager dummies;
  private final Outcome outcome;

  public CreatorFlow(CreatorConfig config, PreviewDummyManager dummies, Outcome outcome) {
    this.config = config;
    this.dummies = dummies;
    this.outcome = outcome;
  }

  /** Pierwszy ekran kreatora. */
  public SceneController start() {
    return new RaceStep(0);
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
      dummies.start(at, player, race, clazz);
      for (Entity carrier : dummies.carriers(player.getUniqueId())) {
        view.own(carrier);
      }
      view.put(PREVIEW_KEY, Boolean.TRUE);
    } else {
      dummies.moveAnchor(player, at);
      dummies.select(player, race, clazz);
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

  private static String counter(int index, int size) {
    return (index + 1) + "/" + size;
  }

  // --- krok 1: rasa -----------------------------------------------------------------

  private final class RaceStep implements SceneController {
    private int index;

    RaceStep(int index) {
      this.index = index;
    }

    private CreatorConfig.RaceOption race() {
      List<CreatorConfig.RaceOption> all = races();
      return all.get(Math.floorMod(index, all.size()));
    }

    @Override
    public void onOpen(SceneView view) {
      show(view);
    }

    private void show(SceneView view) {
      CreatorConfig.RaceOption race = race();
      view.point(race.id());
      // Rasa w skorce pierwszego archetypu — zeby rasa byla widoczna na postaci od razu.
      showPreview(view, race, classes().get(0));
      view.card(raceCard(race));
      view.hint(hint("A/D", "Rasa " + counter(Math.floorMod(index, races().size()), races().size()),
          "LPM", "Wybierz", "PPM", "Wyjdź"));
    }

    private Component raceCard(CreatorConfig.RaceOption race) {
      // Nazwa rasy na bialo: kolory ras z configu bywaja ciemne (&8 Nieumarly) i na
      // ciemnym tle karty byly nieczytelne — zlapane na kliencie QA.
      Component card = legacy(race.display()).color(NamedTextColor.WHITE)
          .decoration(TextDecoration.BOLD, true)
          .append(Component.newline())
          .append(Component.text(race.bonus() == null ? "" : race.bonus(), NamedTextColor.GOLD))
          .append(Component.newline())
          .append(lines(race.description(), NamedTextColor.GRAY))
          .append(Component.newline()).append(Component.newline())
          .append(Component.text("Klasy tej rasy:", NamedTextColor.WHITE));
      for (CreatorConfig.ClassOption clazz : classes()) {
        card = card.append(Component.newline())
            .append(Component.text(" • ", NamedTextColor.DARK_GRAY))
            .append(legacy(clazz.displayFor(race.id())))
            .append(Component.text(" (", NamedTextColor.DARK_GRAY))
            .append(legacy(clazz.display()).color(NamedTextColor.GRAY))
            .append(Component.text(")", NamedTextColor.DARK_GRAY));
      }
      return card;
    }

    @Override
    public void onPrevious(SceneView view) {
      view.fade(() -> {
        index--;
        show(view);
      });
    }

    @Override
    public void onNext(SceneView view) {
      view.fade(() -> {
        index++;
        show(view);
      });
    }

    @Override
    public void onSelect(SceneView view) {
      view.switchTo(new ClassStep(Math.floorMod(index, races().size()), 0));
    }

    @Override
    public void onBack(SceneView view) {
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

  // --- krok 2: klasa rasowa ---------------------------------------------------------

  private final class ClassStep implements SceneController {
    private final int raceIndex;
    private int index;

    ClassStep(int raceIndex, int index) {
      this.raceIndex = raceIndex;
      this.index = index;
    }

    private CreatorConfig.RaceOption race() {
      return races().get(raceIndex);
    }

    private CreatorConfig.ClassOption clazz() {
      List<CreatorConfig.ClassOption> all = classes();
      return all.get(Math.floorMod(index, all.size()));
    }

    @Override
    public void onOpen(SceneView view) {
      show(view);
    }

    /** Klasy zmieniaja sie w tej samej scenerii rasy — bez sciemnienia, jak przebranie. */
    private void show(SceneView view) {
      CreatorConfig.RaceOption race = race();
      CreatorConfig.ClassOption clazz = clazz();
      showPreview(view, race, clazz);
      view.card(classCard(race, clazz));
      view.hint(hint("A/D", "Klasa " + counter(Math.floorMod(index, classes().size()), classes().size()),
          "LPM", "Wybierz", "PPM", "Zmień rasę"));
    }

    private Component classCard(CreatorConfig.RaceOption race, CreatorConfig.ClassOption clazz) {
      return legacy(clazz.displayFor(race.id())).decoration(TextDecoration.BOLD, true)
          .append(Component.newline())
          .append(legacy(race.display()).color(NamedTextColor.GRAY))
          .append(Component.text(" · ", NamedTextColor.DARK_GRAY))
          .append(legacy(clazz.display()).color(NamedTextColor.GRAY))
          .append(Component.newline()).append(Component.newline())
          .append(lines(clazz.descriptionFor(race.id()), NamedTextColor.WHITE))
          .append(Component.newline()).append(Component.newline())
          .append(Component.text(clazz.bonusFor(race.id()), NamedTextColor.GOLD));
    }

    @Override
    public void onPrevious(SceneView view) {
      index--;
      show(view);
    }

    @Override
    public void onNext(SceneView view) {
      index++;
      show(view);
    }

    @Override
    public void onSelect(SceneView view) {
      CreatorConfig.RaceOption race = race();
      CreatorConfig.ClassOption clazz = clazz();
      outcome.chosen(view, race, clazz);
    }

    @Override
    public void onBack(SceneView view) {
      view.switchTo(new RaceStep(raceIndex));
    }

    @Override
    public void onClose(SceneView view) {
      // Zamkniecie sceny (wyjscie z serwera, admin) sprzata podglad niezaleznie od kroku.
      if (view.isClosed()) {
        dropPreview(view);
      }
    }
  }
}
