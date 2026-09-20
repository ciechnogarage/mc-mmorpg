package pl.mcmmorpg.charactercreator.creator;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Wpisywanie imienia przez natywny ekran dialogu (Paper/vanilla Dialog API, 1.21.6+).
 *
 * <p>Zastepuje poprzednia wersje opartaa na GUI kowadla, ktora byla zrodlem realnych bledow
 * zglaszanych z gry: MythicLib (`Navigator`) przejmowal okno w ok. sekunde po otwarciu, wiec
 * czekanie na klikniecie w slot wyniku bylo niewiarygodne; obejsciem bylo przechwytywanie
 * tekstu na kazdy PrepareAnvilEvent i zatwierdzanie go przy zamknieciu okna z DOWOLNEGO
 * powodu. Wczesniejsza proba blokowania cudzego InventoryOpenEvent powodowala crash klienta
 * (DecoderException na accept_teleportation), bo klient i serwer przestawaly sie zgadzac co
 * do tego, ktore okno jest otwarte.
 *
 * <p>Dialog nie ma tych problemow z konstrukcji: to osobny ekran klienta z prawdziwym polem
 * tekstowym i przyciskami, nie udawane inventory — nikt go nie podkrada, nie ma slotow ani
 * pakietow okna, a wynik przychodzi raz, jawnie, po kliknieciu "Zatwierdz".
 */
public final class NameInputDialog {

  private final Plugin plugin;

  public NameInputDialog(Plugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Pokazuje dialog wpisywania imienia.
   *
   * @param onConfirm wywolywane na watku glownym po "Zatwierdz", z SUROWYM tekstem z pola.
   *                  Walidacja nalezy do wolajacego ({@link NameRules}) — dialog nie ma jak
   *                  pokazac powodu odmowy, a wolajacy owszem (actionbar).
   * @param onCancel  wywolywane na watku glownym po "Anuluj"
   */
  public void open(Player player, String initial, Consumer<String> onConfirm, Runnable onCancel) {
    ActionButton confirm = ActionButton.builder(Component.text("Zatwierdz"))
        .tooltip(Component.text("Zapisz imie postaci"))
        .action(DialogAction.customClick((view, audience) -> {
          String typed = view.getText("name");
          // Callback moze przyjsc spoza watku glownego — wszystko, co dotyka sesji
          // i ekwipunku, musi wrocic na main thread.
          Bukkit.getScheduler().runTask(plugin, () -> onConfirm.accept(typed));
        }, ClickCallback.Options.builder().build()))
        .build();

    ActionButton cancel = ActionButton.builder(Component.text("Anuluj"))
        .action(DialogAction.customClick((view, audience) ->
            Bukkit.getScheduler().runTask(plugin, onCancel),
            ClickCallback.Options.builder().build()))
        .build();

    Dialog dialog = Dialog.create(factory -> factory.empty()
        .base(DialogBase.builder(Component.text("Imie postaci", NamedTextColor.GOLD))
            // FALSE CELOWO. Przy true Escape zamykal dialog BEZ wywolania onConfirm
            // ani onCancel — a to one otwieraja menu z powrotem. Gracz zostawal w
            // creator_limbo, w SPECTATOR (wymuszanym co 10 tickow), bez GUI, bez HUD
            // i bez sposobu wyjscia poza relogiem. Jedyne wyjscia to teraz przyciski,
            // z ktorych kazdy ma callback.
            .canCloseWithEscape(false)
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .inputs(List.of(DialogInput.text("name", Component.text("Imie"))
                .initial(initial == null ? "" : initial)
                .maxLength(NameRules.MAX_LENGTH)
                .width(200)
                .build()))
            .build())
        .type(DialogType.confirmation(confirm, cancel)));

    player.showDialog(dialog);
  }

}
