package pl.mcmmorpg.charactercreator.scene;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import pl.mcmmorpg.charactercreator.creator.NameRules;

/**
 * Ksiega "Nowa postac" — natywne okno dialogowe Papera nad scena: pole na imie, kolor ikony,
 * podsumowanie wybranej klasy, przyciski "Stworz postac" i "Wroc".
 *
 * <p><b>Klikniecia przez wlasny identyfikator</b> ({@value #CLICK_NAMESPACE}:ksiega) i
 * {@code PlayerCustomClickEvent}, a nie przez callbacki {@code DialogAction.customClick(callback)}:
 * te gubily u nas ok. 1/3 klikniec (zmierzone przy Chowancach, notatka paper-dialog-przyciski).
 * Kazde otwarcie ma numer; klik w nieaktualne okno tylko pokazuje biezace.
 *
 * <p>Escape wylaczony: jedyne wyjscia to dwa przyciski i kazdy ma obsluge — inaczej gracz
 * zostawalby w scenie bez okna i bez wiedzy, ze dalej jest LPM.
 */
public final class NewCharacterBook implements Listener {

  static final String CLICK_NAMESPACE = "charactercreator";
  private static final Key CLICK = Key.key(CLICK_NAMESPACE, "ksiega");

  /** Kolory ikony postaci (jak u Wynna: 9 barw). id -> nazwa i barwa. */
  public static final Map<String, TextColor> COLORS = colors();
  public static final String DEFAULT_COLOR = "czerwony";

  private static Map<String, TextColor> colors() {
    Map<String, TextColor> m = new java.util.LinkedHashMap<>();
    m.put("szary", TextColor.color(0x9AA0A6));
    m.put("czerwony", TextColor.color(0xE0513F));
    m.put("pomaranczowy", TextColor.color(0xF0913A));
    m.put("zolty", TextColor.color(0xF2D04B));
    m.put("zielony", TextColor.color(0x6CC24A));
    m.put("turkusowy", TextColor.color(0x3FC1B0));
    m.put("niebieski", TextColor.color(0x4A8FE0));
    m.put("fioletowy", TextColor.color(0x9B6BE0));
    m.put("rozowy", TextColor.color(0xE67AB8));
    return java.util.Collections.unmodifiableMap(m);
  }

  private static final Map<String, String> COLOR_NAMES = Map.of(
      "szary", "Szary", "czerwony", "Czerwony", "pomaranczowy", "Pomarańczowy",
      "zolty", "Żółty", "zielony", "Zielony", "turkusowy", "Turkusowy",
      "niebieski", "Niebieski", "fioletowy", "Fioletowy", "rozowy", "Różowy");

  /** Co zrobic z wyborem gracza. Wolane na watku glownym. */
  public interface Handler {
    /**
     * Gracz kliknal "Stworz postac" z poprawnym formalnie imieniem.
     *
     * @return {@code null} = gotowe, okno zamkniete; tekst = powod odmowy (np. imie zajete),
     *         okno otwiera sie ponownie z tym komunikatem
     */
    String create(Player player, String name, String iconColor);

    /** "Wroc" — z powrotem do sceny. */
    void back(Player player);
  }

  /** Stan otwartej ksiegi — do ponownego pokazania po bledzie albo po nieaktualnym kliku. */
  private record Open(int nonce, Component summary, String name, String color, String error,
      Handler handler) {}

  private final Plugin plugin;
  private final Logger logger;
  private final Map<UUID, Open> open = new ConcurrentHashMap<>();
  private final AtomicInteger nonces = new AtomicInteger();

  public NewCharacterBook(Plugin plugin, Logger logger) {
    this.plugin = plugin;
    this.logger = logger;
  }

  /**
   * Otwiera ksiege.
   *
   * @param summary podsumowanie wyboru (klasa, rasa, specjalizacje) — sklada je scena
   */
  public void open(Player player, Component summary, String initialName, String initialColor,
      Handler handler) {
    show(player, summary, initialName, initialColor, null, handler);
  }

  public boolean isOpen(Player player) {
    return open.containsKey(player.getUniqueId());
  }

  /** Scena sie zamyka — ksiega razem z nia. */
  public void forget(Player player) {
    if (open.remove(player.getUniqueId()) != null && player.isOnline()) {
      player.closeDialog();
    }
  }

  private void show(Player player, Component summary, String name, String color, String error,
      Handler handler) {
    int nonce = nonces.incrementAndGet();
    Open state = new Open(nonce, summary, name, color, error, handler);
    open.put(player.getUniqueId(), state);
    player.showDialog(build(state));
  }

  private Dialog build(Open s) {
    List<DialogBody> body = new ArrayList<>();
    body.add(DialogBody.plainMessage(s.summary(), 300));
    if (s.error() != null) {
      body.add(DialogBody.plainMessage(Component.text(s.error(), NamedTextColor.RED), 300));
    }

    List<SingleOptionDialogInput.OptionEntry> colorEntries = new ArrayList<>();
    String selected = COLORS.containsKey(s.color()) ? s.color() : DEFAULT_COLOR;
    for (var e : COLORS.entrySet()) {
      colorEntries.add(SingleOptionDialogInput.OptionEntry.create(e.getKey(),
          Component.text("■ ", e.getValue()).append(Component.text(COLOR_NAMES.get(e.getKey()))),
          e.getKey().equals(selected)));
    }

    ActionButton create = ActionButton.builder(
            Component.text("✔ Stwórz postać", NamedTextColor.GREEN))
        .tooltip(Component.text("Tworzy postać i wpuszcza cię do gry"))
        .width(150)
        .action(DialogAction.customClick(CLICK, payload("stworz", s.nonce())))
        .build();
    ActionButton back = ActionButton.builder(Component.text("Wróć"))
        .tooltip(Component.text("Z powrotem do wyboru rasy i klasy"))
        .width(150)
        .action(DialogAction.customClick(CLICK, payload("wroc", s.nonce())))
        .build();

    return Dialog.create(factory -> factory.empty()
        .base(DialogBase.builder(Component.text("Nowa postać", NamedTextColor.GOLD))
            .canCloseWithEscape(false)
            // pause(false): gra za oknem dalej zyje (scena), a przy braku Escape Paper
            // wymaga pauzy wylaczonej albo after_action, ktore ja zdejmuje.
            .pause(false)
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .body(body)
            .inputs(List.of(
                DialogInput.text("imie", Component.text("Imię postaci"))
                    .initial(s.name() == null ? "" : s.name())
                    .maxLength(NameRules.MAX_LENGTH)
                    .width(220)
                    .build(),
                DialogInput.singleOption("kolor", Component.text("Kolor ikony"), colorEntries)
                    .width(220)
                    .build()))
            .build())
        .type(DialogType.confirmation(create, back)));
  }

  private static final java.util.regex.Pattern NONCE = java.util.regex.Pattern.compile("\\bn:(\\d+)");
  private static final java.util.regex.Pattern ACTION =
      java.util.regex.Pattern.compile("\\ba:[\"']?(\\w+)");

  private static BinaryTagHolder payload(String action, int nonce) {
    return BinaryTagHolder.binaryTagHolder("{a:\"" + action + "\",n:" + nonce + "}");
  }

  @EventHandler
  public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
    open.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onClick(PlayerCustomClickEvent event) {
    if (!CLICK.equals(event.getIdentifier())) {
      return;
    }
    if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
      return;
    }
    Player player = connection.getPlayer();
    DialogResponseView view = event.getDialogResponseView();
    String tag = event.getTag() == null ? "" : event.getTag().string();
    String typed = view == null ? null : view.getText("imie");
    String color = view == null ? null : view.getText("kolor");
    // Zdarzenie moze przyjsc spoza watku glownego — wszystko dalej dotyka gracza i sceny.
    Bukkit.getScheduler().runTask(plugin, () -> handle(player, tag, typed, color));
  }

  private void handle(Player player, String tag, String typed, String color) {
    Open state = open.get(player.getUniqueId());
    if (state == null || !player.isOnline()) {
      return;
    }
    // Ladunek wraca jako SNBT; kolejnosc kluczy i cudzyslowy nie sa gwarantowane.
    java.util.regex.Matcher n = NONCE.matcher(tag);
    java.util.regex.Matcher a = ACTION.matcher(tag);
    String action = a.find() ? a.group(1) : "";
    if (!n.find() || Integer.parseInt(n.group(1)) != state.nonce()) {
      // Klik w stare okno (np. drugi klik zanim doszlo nowe) — pokaz biezace, nic nie rob.
      player.showDialog(build(state));
      return;
    }
    String chosenColor = COLORS.containsKey(color) ? color : state.color();
    if ("wroc".equals(action)) {
      open.remove(player.getUniqueId());
      state.handler().back(player);
      return;
    }
    NameRules.Result result = NameRules.validate(typed);
    if (!result.ok()) {
      show(player, state.summary(), typed, chosenColor, result.rejection().message(), state.handler());
      return;
    }
    open.remove(player.getUniqueId());
    String refusal;
    try {
      refusal = state.handler().create(player, result.name(), chosenColor);
    } catch (RuntimeException ex) {
      logger.severe("Ksiega: blad tworzenia postaci dla " + player.getName() + ": " + ex);
      ex.printStackTrace();
      refusal = "Coś poszło nie tak — spróbuj jeszcze raz albo zgłoś to administracji.";
    }
    if (refusal != null && player.isOnline()) {
      show(player, state.summary(), result.name(), chosenColor, refusal, state.handler());
    }
  }
}
