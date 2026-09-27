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
 * Okno z przyciskami nad scena (natywny dialog Papera) — ksiega postaci, potwierdzenie usuniecia.
 *
 * <p>Ten sam mechanizm klikniec co {@link NewCharacterBook}: wlasny id + numer okna, bo callbacki
 * Papera gubily kliki. Ostatni przycisk jest zawsze przyciskiem wyjscia i jego akcja wykonuje sie
 * takze po Escape — gracz nie zostaje bez obslugi.
 */
public final class SceneMenu implements Listener {

  private static final Key CLICK = Key.key(NewCharacterBook.CLICK_NAMESPACE, "menu");
  private static final Pattern NONCE = Pattern.compile("\\bn:(\\d+)");
  private static final Pattern BUTTON = Pattern.compile("\\bb:(\\d+)");

  /** Przycisk: napis, podpowiedz (moze byc null) i co zrobic. */
  public record Button(Component label, Component tooltip, Runnable action) {}

  private record Open(int nonce, List<Button> buttons) {}

  private final Plugin plugin;
  private final Logger logger;
  private final Map<UUID, Open> open = new ConcurrentHashMap<>();
  private final AtomicInteger nonces = new AtomicInteger();

  public SceneMenu(Plugin plugin, Logger logger) {
    this.plugin = plugin;
    this.logger = logger;
  }

  /**
   * @param buttons przyciski; OSTATNI to wyjscie (tez pod Escape)
   */
  public void show(Player player, Component title, Component body, List<Button> buttons, int columns) {
    int nonce = nonces.incrementAndGet();
    open.put(player.getUniqueId(), new Open(nonce, List.copyOf(buttons)));
    List<ActionButton> actions = new ArrayList<>();
    for (int i = 0; i < buttons.size() - 1; i++) {
      actions.add(button(buttons.get(i), nonce, i));
    }
    ActionButton exit = button(buttons.get(buttons.size() - 1), nonce, buttons.size() - 1);
    List<DialogBody> bodies = body == null ? List.of() : List.of(DialogBody.plainMessage(body, 300));
    Dialog dialog = Dialog.create(factory -> factory.empty()
        .base(DialogBase.builder(title)
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .body(bodies)
            .build())
        .type(actions.isEmpty()
            ? DialogType.notice(exit)
            : DialogType.multiAction(actions).columns(Math.max(1, columns)).exitAction(exit).build()));
    player.showDialog(dialog);
  }

  private static ActionButton button(Button b, int nonce, int index) {
    var builder = ActionButton.builder(b.label()).width(200)
        .action(DialogAction.customClick(CLICK,
            BinaryTagHolder.binaryTagHolder("{n:" + nonce + ",b:" + index + "}")));
    if (b.tooltip() != null) {
      builder.tooltip(b.tooltip());
    }
    return builder.build();
  }

  public void forget(Player player) {
    open.remove(player.getUniqueId());
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
      logger.severe("Menu sceny: blad przycisku " + index + " dla " + player.getName() + ": " + ex);
      ex.printStackTrace();
    }
  }
}
