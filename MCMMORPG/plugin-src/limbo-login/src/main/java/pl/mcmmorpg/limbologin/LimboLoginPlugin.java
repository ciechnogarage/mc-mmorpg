package pl.mcmmorpg.limbologin;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Ekran logowania na serwerze {@code limbo} — zamiast wpisywania hasla na czacie.
 *
 * <p><b>Po co.</b> Logowaniem zarzadza LibreLogin stojacy na PROXY, a jego interfejsem byl
 * dotad czat: gracz czytal "Please login using: /login &lt;password&gt;" i wklepywal komende.
 * To jest jednoczesnie niewygodne i niebezpieczne — jedno zapomniane ukosnik i haslo ladzie
 * na czacie publicznym. Ten plugin pokazuje zamiast tego natywne okno klienta z polem
 * tekstowym i przyciskami.
 *
 * <p><b>Dlaczego przyciski wykonuja KOMENDE, a nie wolaja API.</b> To nie jest wybor stylu,
 * tylko jedyna droga, ktora dziala. LibreLogin zyje na proxy i widzi wylacznie komendy
 * wyslane przez KLIENTA: Velocity przechwytuje je, zanim dojda do backendu. Komenda
 * wykonana po stronie serwera ({@code Player#performCommand}, {@code Player#chat}) nie
 * opuszcza tego serwera i do proxy nigdy nie dotrze. Dlatego przyciski uzywaja
 * {@link DialogAction#commandTemplate(String)} — to KLIENT wysyla {@code /login ...},
 * dokladnie tak, jakby gracz go wpisal, tyle ze nic nie ladzie na czacie.
 *
 * <p><b>Czego to okno nie robi.</b> Nie zaslania hasla gwiazdkami — Paper ma tylko cztery
 * typy pol (tekst, liczba, przelacznik, wybor z listy) i zadnego typu "haslo". Znaki sa
 * wiec widoczne w polu, na wlasnym ekranie gracza. Zysk jest w czym innym: haslo nie
 * przechodzi przez czat, wiec nie da sie go wyslac publicznie przez pomylke i nie zostaje
 * w historii czatu.
 *
 * <p><b>Dlaczego dwa przyciski zamiast jednego.</b> Ten plugin stoi na backendzie i nie ma
 * jak zapytac LibreLogin (na proxy), czy dany nick jest juz zarejestrowany. Zamiast zgadywac
 * albo czytac cudza baze SQLite spod proxy, pytamy gracza wprost. Jesli kiedys ta baza
 * zostanie podmontowana do limbo tylko-do-odczytu, mozna to zwinac do jednego przycisku.
 */
public final class LimboLoginPlugin extends JavaPlugin implements Listener {

  /**
   * Opoznienie pokazania okna po wejsciu.
   *
   * <p>Gracz wchodzi na limbo w trakcie przelaczania serwerow przez proxy; okno wyslane w
   * tym samym ticku potrafi zginac razem z ekranem ladowania. Sekunda wystarcza, a gracz
   * i tak nie ma co w tym czasie robic — limbo jest puste.
   */
  private static final long SHOW_DELAY_TICKS = 20L;

  /** Klucz pola z haslem. Ta sama nazwa wchodzi do szablonu komendy jako {@code $(haslo)}. */
  private static final String FIELD = "haslo";

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
    getLogger().info("Ekran logowania aktywny — haslo podaje sie w oknie, nie na czacie.");
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    Bukkit.getScheduler().runTaskLater(this, () -> {
      if (player.isOnline()) {
        player.showDialog(dialog());
      }
    }, SHOW_DELAY_TICKS);
  }

  /**
   * Okno logowania.
   *
   * <p>{@code canCloseWithEscape(false)} + {@code DialogAfterAction.NONE} sa tu para i obie
   * strony tej pary sa potrzebne: Escape nie ma zamykac ekranu logowania, a po kliknieciu
   * przycisku okno ma ZOSTAC otwarte, bo dopiero wtedy okaze sie, czy haslo bylo dobre.
   * Przy udanym logowaniu LibreLogin przerzuca gracza na lobby i okno znika razem ze zmiana
   * serwera; przy blednym gracz zostaje tutaj i moze sprobowac od razu, bez relogu.
   */
  private Dialog dialog() {
    ActionButton login = ActionButton.builder(Component.text("Zaloguj"))
        .tooltip(Component.text("Wejdz na swoje konto"))
        .width(120)
        .action(DialogAction.commandTemplate("login $(" + FIELD + ")"))
        .build();

    // Rejestracja podaje haslo DWA RAZY, bo tego wymaga skladnia LibreLogin
    // (/register <haslo> <haslo>). Powtorzenie bierzemy z tego samego pola — potwierdzanie
    // hasla drugim polem nic tu nie wnosi, skoro gracz widzi to, co wpisal.
    ActionButton register = ActionButton.builder(Component.text("Utworz konto"))
        .tooltip(Component.text("Pierwszy raz na serwerze — minimum 6 znakow"))
        .width(120)
        .action(DialogAction.commandTemplate("register $(" + FIELD + ") $(" + FIELD + ")"))
        .build();

    return Dialog.create(factory -> factory.empty()
        .base(DialogBase.builder(Component.text("Logowanie", NamedTextColor.GOLD))
            .canCloseWithEscape(false)
            // pause(false) NIE jest ozdobnikiem: Paper odrzuca dialog, ktory pauzuje gre
            // i jednoczesnie zostaje otwarty po kliknieciu ("Dialogs that pause the game
            // must use after_action values that unpause it"). Nam zalezy wlasnie na tym,
            // zeby zostal otwarty — wiec rezygnujemy z pauzy. Na serwerze i tak nic nie
            // pauzuje, to ustawienie ma znaczenie tylko w grze jednoosobowej.
            .pause(false)
            .afterAction(DialogBase.DialogAfterAction.NONE)
            .body(List.of(DialogBody.plainMessage(Component.text(
                "Podaj haslo i wybierz przycisk. Jesli jestes tu pierwszy raz, "
                    + "uzyj \"Utworz konto\" — haslo musi miec co najmniej 6 znakow.",
                NamedTextColor.GRAY), 280)))
            .inputs(List.of(DialogInput.text(FIELD, Component.text("Haslo"))
                .maxLength(64)
                .width(200)
                .build()))
            .build())
        .type(DialogType.multiAction(List.of(login, register))
            .columns(2)
            .build()));
  }
}
