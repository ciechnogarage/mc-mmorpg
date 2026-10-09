package pl.mcmmorpg.chowance.gra;

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
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import pl.mcmmorpg.chowance.Chowance;

/**
 * Natywne okna Papera (Dialog API, Paper 1.21.7+, https://docs.papermc.io/paper/dev/dialogs/).
 *
 * <p>Przyciski NIE używają rejestru callbacków Papera. Pomiar na chw-test (PROBLEMY A-007): surowe
 * kliknięcie dochodziło (PlayerCustomClickEvent), a callback `paper:dialog_click_callback` się nie
 * wykonywał — gracz zostawał z oknem, które nic nie robi. Każdy przycisk ma więc własny identyfikator
 * {@code chowance:klik} z ładunkiem {@code {o:<numer okna>,b:<numer przycisku>}}; obsługa jest tutaj,
 * w {@link PlayerCustomClickEvent}. Kliknięcie w nieaktualne okno (podwójny klik, lag) nie wykonuje
 * akcji, tylko ponownie pokazuje okno bieżące — gracz nigdy nie zostaje z martwym oknem.
 */
public final class Okna implements Listener {
  public static final Key KLIK = Key.key("chowance", "klik");
  /** SNBT porządkuje klucze po swojemu (przyszło {b:0,o:1}) — każdy klucz osobno, bez założeń o kolejności. */
  private static final Pattern KLUCZ_O = Pattern.compile("\\bo:(-?\\d+)");
  private static final Pattern KLUCZ_B = Pattern.compile("\\bb:(-?\\d+)");

  private final JavaPlugin plugin;
  private final Map<UUID, Integer> wersja = new ConcurrentHashMap<>();
  /** Bieżące okno gracza: akcje przycisków (indeks = b) i sposób ponownego pokazania. */
  private final Map<UUID, Biezace> biezace = new ConcurrentHashMap<>();

  /** debug: config.yml `debug: true` — każde kliknięcie z numerem okna trafia do logu. */
  public java.util.function.BooleanSupplier debug = () -> false;

  private record Biezace(int wersja, List<Consumer<Player>> akcje, Runnable ponow) {}

  public Okna(JavaPlugin plugin) { this.plugin = plugin; }

  public record Przycisk(String etykieta, String podpowiedz, int szerokosc, Consumer<Player> akcja) {
    public static Przycisk of(String etykieta, String podpowiedz, Consumer<Player> akcja) {
      return new Przycisk(etykieta, podpowiedz, 150, akcja);
    }
  }

  /** Pokazuje okno; {@code czekaj}=true zostawia klienta w „oczekiwaniu” po kliknięciu (walka turowa). */
  public void pokaz(Player p, String tytul, List<String> tresc, List<Przycisk> przyciski, Przycisk wyjscie,
      int kolumny, boolean escape, boolean czekaj) {
    pokaz(p, tytul, tresc, przyciski, wyjscie, kolumny, escape, czekaj, null);
  }

  /** Pole tekstowe okna (np. imię stwora); wartość po kliknięciu: {@link #tekst(Player, String)}. */
  public record Pole(String klucz, String etykieta, String poczatkowe, int maxDlugosc) {}

  /** Wartości pól z ostatniego kliknięcia gracza (ustawiane tuż przed wykonaniem akcji przycisku). */
  private final Map<UUID, Map<String, String>> pola = new ConcurrentHashMap<>();

  public String tekst(Player p, String klucz) {
    Map<String, String> m = pola.get(p.getUniqueId());
    return m == null ? null : m.get(klucz);
  }

  public void pokaz(Player p, String tytul, List<String> tresc, List<Przycisk> przyciski, Przycisk wyjscie,
      int kolumny, boolean escape, boolean czekaj, Pole pole) {
    UUID id = p.getUniqueId();
    int w = wersja.merge(id, 1, Integer::sum);
    List<Consumer<Player>> akcje = new ArrayList<>();
    List<ActionButton> btn = new ArrayList<>();
    for (Przycisk pr : przyciski) btn.add(przycisk(w, akcje, pr));
    List<DialogBody> body = new ArrayList<>();
    for (String linia : tresc) body.add(DialogBody.plainMessage(Chowance.mm(linia), 300));
    DialogBase base = DialogBase.builder(Chowance.mm(tytul))
        .canCloseWithEscape(escape)
        .pause(false)
        .afterAction(czekaj ? DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE : DialogBase.DialogAfterAction.CLOSE)
        .body(body)
        .inputs(pole == null ? List.of() : List.of(io.papermc.paper.registry.data.dialog.input.DialogInput
            .text(pole.klucz(), Chowance.mm(pole.etykieta())).initial(pole.poczatkowe() == null ? "" : pole.poczatkowe())
            .maxLength(Math.max(1, pole.maxDlugosc())).width(200).build()))
        .build();
    // multiAction z pustą listą rzuca IllegalArgumentException("actions cannot be empty") — PROBLEMY A-006.
    DialogType typ;
    if (btn.isEmpty()) {
      Przycisk jedyny = wyjscie != null ? wyjscie : Przycisk.of("<gray>OK", "", x -> {});
      typ = DialogType.notice(przycisk(w, akcje, jedyny));
    } else if (wyjscie == null) {
      typ = DialogType.multiAction(btn).columns(Math.max(1, kolumny)).build();
    } else {
      typ = DialogType.multiAction(btn).columns(Math.max(1, kolumny)).exitAction(przycisk(w, akcje, wyjscie)).build();
    }
    Dialog d = Dialog.create(b -> b.empty().base(base).type(typ));
    // Ponowne pokazanie = ten sam dialog z tymi samymi akcjami pod NOWYM numerem okna.
    Runnable ponow = () -> {
      Player on = Bukkit.getPlayer(id);
      if (on != null && on.isOnline()) pokaz(on, tytul, tresc, przyciski, wyjscie, kolumny, escape, czekaj, pole);
    };
    biezace.put(id, new Biezace(w, akcje, ponow));
    p.showDialog(d);
  }

  private ActionButton przycisk(int w, List<Consumer<Player>> akcje, Przycisk pr) {
    int b = akcje.size();
    akcje.add(pr.akcja());
    DialogAction akcja = DialogAction.customClick(KLIK, BinaryTagHolder.binaryTagHolder("{o:" + w + ",b:" + b + "}"));
    Component tip = pr.podpowiedz() == null || pr.podpowiedz().isBlank() ? null : Chowance.mm(pr.podpowiedz());
    return ActionButton.create(Chowance.mm(pr.etykieta()), tip, Math.max(40, Math.min(1024, pr.szerokosc())), akcja);
  }

  @EventHandler
  public void klik(PlayerCustomClickEvent e) {
    if (!KLIK.equals(e.getIdentifier())) return;
    if (!(e.getCommonConnection() instanceof PlayerGameConnection gc)) return;
    Player p = gc.getPlayer();
    BinaryTagHolder tag = e.getTag();
    if (tag == null) return;
    Matcher mo = KLUCZ_O.matcher(tag.string());
    Matcher mb = KLUCZ_B.matcher(tag.string());
    if (!mo.find() || !mb.find()) return;
    int o = Integer.parseInt(mo.group(1));
    int b = Integer.parseInt(mb.group(1));
    Map<String, String> wartosci = new java.util.HashMap<>();
    try {
      var v = e.getDialogResponseView();
      if (v != null) for (String k : List.of("imie")) { String t = v.getText(k); if (t != null) wartosci.put(k, t); }
    } catch (RuntimeException ignored) { /* okno bez pól */ }
    Runnable r = () -> { pola.put(p.getUniqueId(), wartosci); obsluz(p, o, b); };
    if (Bukkit.isPrimaryThread()) r.run(); else Bukkit.getScheduler().runTask(plugin, r);
  }

  private void obsluz(Player p, int o, int b) {
    Biezace cur = biezace.get(p.getUniqueId());
    if (debug.getAsBoolean()) plugin.getLogger().info("[debug] okno klik " + p.getName() + " okno=" + o + " przycisk=" + b + " aktualne=" + (cur == null ? null : cur.wersja()));
    if (cur == null || cur.wersja() != o || b < 0 || b >= cur.akcje().size()) {
      // Nieaktualne okno: nie wykonujemy akcji, pokazujemy bieżące (jeśli jest) — gracz nie zostaje z martwym oknem.
      if (cur != null && cur.wersja() != o) cur.ponow().run();
      return;
    }
    biezace.remove(p.getUniqueId()); // jedno kliknięcie na okno
    wersja.merge(p.getUniqueId(), 1, Integer::sum);
    cur.akcje().get(b).accept(p);
  }

  public void zamknij(Player p) {
    wersja.merge(p.getUniqueId(), 1, Integer::sum);
    biezace.remove(p.getUniqueId());
    p.closeDialog();
  }

  public void zapomnij(UUID p) {
    wersja.remove(p);
    pola.remove(p);
    biezace.remove(p);
  }

  /**
   * Rozgrzewka po starcie: pierwsze Dialog.create po restarcie ładuje klasy i rejestry dialogów Papera
   * (spark world1 2026-09-30: ~300 ms w ticku, w którym pierwszy gracz kliknął NPC). Budujemy jeden dialog
   * bez pokazywania, gdy nikogo jeszcze nie ma.
   */
  public static void rozgrzej() {
    DialogBase base = DialogBase.builder(Component.text("x")).canCloseWithEscape(true).pause(false)
        .afterAction(DialogBase.DialogAfterAction.CLOSE).build();
    ActionButton ok = ActionButton.builder(Component.text("ok"))
        .action(DialogAction.customClick(Key.key("chowance", "klik"), BinaryTagHolder.binaryTagHolder("{o:0,b:0}"))).build();
    Dialog.create(b -> b.empty().base(base).type(DialogType.notice(ok)));
  }
}
