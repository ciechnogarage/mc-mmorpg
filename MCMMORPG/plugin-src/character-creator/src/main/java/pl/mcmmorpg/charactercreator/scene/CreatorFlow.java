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

  /**
   * Podsumowanie wyboru do ksiegi "Nowa postac": klasa rasowa, rasa i archetyp,
   * specjalizacje. Krotsze niz karta — ksiega ma miejsce na imie i kolor.
   */
  public Component summary(CreatorConfig.RaceOption race, CreatorConfig.ClassOption clazz) {
    CreatorConfig.RaceVariant v = clazz.variant(race.id());
    Component out = legacy(clazz.displayFor(race.id())).decoration(TextDecoration.BOLD, true)
        .append(Component.text("   " + plain(race.display()) + " · " + plain(clazz.display()),
            NamedTextColor.GRAY));
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
    return out.append(Component.newline())
        .append(Component.text("Tryby specjalne (Hardcore): wkrótce", NamedTextColor.DARK_GRAY));
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

    /** Pelne odswiezenie: kadr rasy, postac, oba panele, pasek i podpowiedz. */
    private void showRace(SceneView view) {
      view.point(race().id());
      view.topBar(raceStrip());
      view.hint(hint("A/D", "Rasa", "W/S", "Klasa", "LPM", "Dalej", "PPM", "Wyjdź"));
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
          .append(Component.text(nz(r.description()), NamedTextColor.GRAY))
          .append(Component.newline()).append(Component.newline())
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

    /** Prawy panel: karta klasy rasowej z obiema specjalizacjami. */
    private Component classCard(CreatorConfig.RaceOption r, CreatorConfig.ClassOption c) {
      CreatorConfig.RaceVariant v = c.variant(r.id());
      Component card = legacy(c.displayFor(r.id())).decoration(TextDecoration.BOLD, true)
          .append(Component.newline())
          .append(Component.text(plain(r.display()) + " · " + plain(c.display()), NamedTextColor.GRAY))
          .append(Component.newline()).append(Component.newline());
      if (v == null || v.lore().isBlank()) {
        // Config bez pol sceny (stary generator) — opis wielowierszowy jak w starym kreatorze.
        return card.append(lines(c.descriptionFor(r.id()), NamedTextColor.WHITE))
            .append(Component.newline()).append(Component.newline())
            .append(Component.text(nz(c.bonusFor(r.id())), NamedTextColor.GOLD));
      }
      card = card.append(Component.text(v.lore(), NamedTextColor.WHITE))
          .append(Component.newline()).append(Component.newline())
          .append(Component.text("Zbroja: ", NamedTextColor.GRAY))
          .append(Component.text(v.armor(), NamedTextColor.WHITE))
          .append(Component.newline())
          .append(Component.text("Broń: ", NamedTextColor.GRAY))
          .append(Component.text(v.weapons(), NamedTextColor.WHITE));
      if (!v.specializations().isEmpty()) {
        card = card.append(Component.newline()).append(Component.newline())
            .append(Component.text("Specjalizacje od 10. poziomu", NamedTextColor.GOLD));
        for (CreatorConfig.Specialization spec : v.specializations()) {
          // Nazwa zawsze biala, kolor specjalizacji tylko na znaczniku: czesc kolorow
          // z generatora (&5, &8) byla na ciemnej karcie nieczytelna.
          Component marker = legacy(spec.name().replaceAll("^((?:&[0-9a-fA-F])*).*$", "$1") + "◆ ");
          card = card.append(Component.newline())
              .append(marker)
              .append(Component.text(plain(spec.name()), NamedTextColor.WHITE)
                  .decoration(TextDecoration.BOLD, true))
              .append(Component.newline())
              .append(Component.text(spec.description(), NamedTextColor.GRAY));
          if (!spec.perks().isBlank()) {
            card = card.append(Component.newline())
                .append(Component.text("+ " + spec.perks(), NamedTextColor.GREEN));
          }
        }
      }
      return card;
    }

    @Override
    public void onPrevious(SceneView view) {
      view.uiSound("race");
      view.fade(() -> {
        race--;
        showRace(view);
      });
    }

    @Override
    public void onNext(SceneView view) {
      view.uiSound("race");
      view.fade(() -> {
        race++;
        showRace(view);
      });
    }

    @Override
    public void onUp(SceneView view) {
      clazz--;
      view.uiSound("class");
      showClass(view);
    }

    @Override
    public void onDown(SceneView view) {
      clazz++;
      view.uiSound("class");
      showClass(view);
    }

    @Override
    public void onSelect(SceneView view) {
      view.uiSound("select");
      outcome.chosen(view, race(), clazz());
    }

    @Override
    public void onBack(SceneView view) {
      view.uiSound("back");
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
