package pl.mcmmorpg.charactercreator.scene;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Okno-siatka nad scena z WASKIMI przyciskami — lista 16 klas kreatora (4 kolumny = rasy).
 *
 * <p>Osobno od {@link SceneMenu}, bo tamten ma przyciski po 200 px: cztery kolumny nie
 * miescily sie na ekranie juz przy skali GUI 2 (zrzut z klienta QA 30.09 — czwarta rasa
 * ucieta). Szerokosc liczona na najgorszy przypadek: 1280x720 przy GUI 3 = 426 jednostek.
 * Klikniecia tak samo jak w {@link SceneMenu}: wlasny id + numer okna (callbacki Papera gubily
 * kliki), ostatni przycisk to wyjscie i dziala tez pod Escape.
 */
public final class GridDialog implements Listener {

  /** Szerokosc przycisku: 4 x 100 + odstepy miesci sie w 426 jednostkach (GUI 3 przy 720p). */
  public static final int NARROW = 100;

  private static final Key CLICK = Key.key(NewCharacterBook.CLICK_NAMESPACE, "siatka");
  private static final Pattern NONCE = Pattern.compile("\\bn:(\\d+)");
  private static final Pattern BUTTON = Pattern.compile("\\bb:(\\d+)");

  private record Open(int nonce, List<SceneMenu.Button> buttons) {}

  private final Plugin plugin;
  private final Logger logger;
  private final Map<UUID, Open> open = new ConcurrentHashMap<>();
  private final AtomicInteger nonces = new AtomicInteger();

  public GridDialog(Plugin plugin, Logger logger) {
    this.plugin = plugin;
    this.logger = logger;
  }

  /** @param buttons przyciski siatki; OSTATNI to wyjscie (pod siatka, tez pod Escape) */
  public void show(Player player, Component title, Component body, List<SceneMenu.Button> buttons,
      int columns, int width) {
    int nonce = nonces.incrementAndGet();
    open.put(player.getUniqueId(), new Open(nonce, List.copyOf(buttons)));
    List<ActionButton> actions = new ArrayList<>();
    for (int i = 0; i < buttons.size() - 1; i++) {
      actions.add(button(buttons.get(i), nonce, i, width));
    }
    ActionButton exit = button(buttons.get(buttons.size() - 1), nonce, buttons.size() - 1, 150);
    List<DialogBody> bodies = body == null ? List.of() : List.of(DialogBody.plainMessage(body, 400));
    Dialog dialog = Dialog.create(factory -> factory.empty()
        .base(DialogBase.builder(title)
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .body(bodies)
            .build())
        .type(DialogType.multiAction(actions).columns(Math.max(1, columns)).exitAction(exit).build()));
    player.showDialog(dialog);
  }

  private static ActionButton button(SceneMenu.Button b, int nonce, int index, int width) {
    var builder = ActionButton.builder(b.label()).width(width)
        .action(DialogAction.customClick(CLICK,
            BinaryTagHolder.binaryTagHolder("{n:" + nonce + ",b:" + index + "}")));
    if (b.tooltip() != null) {
      builder.tooltip(b.tooltip());
    }
    return builder.build();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    open.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onClick(PlayerCustomClickEvent event) {
    if (!CLICK.equals(event.getIdentifier())
        || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
      return;
    }
    Player player = connection.getPlayer();
    String tag = event.getTag() == null ? "" : event.getTag().string();
    Bukkit.getScheduler().runTask(plugin, () -> handle(player, tag));
  }

  private void handle(Player player, String tag) {
    Open state = open.get(player.getUniqueId());
    Matcher n = NONCE.matcher(tag);
    Matcher b = BUTTON.matcher(tag);
    if (state == null || !player.isOnline() || !n.find() || !b.find()
        || Integer.parseInt(n.group(1)) != state.nonce()) {
      return;
    }
    int index = Integer.parseInt(b.group(1));
    if (index < 0 || index >= state.buttons().size()) {
      return;
    }
    open.remove(player.getUniqueId());
    try {
      state.buttons().get(index).action().run();
    } catch (RuntimeException ex) {
      logger.severe("Siatka sceny: blad przycisku " + index + " dla " + player.getName() + ": " + ex);
      ex.printStackTrace();
    }
  }
}
