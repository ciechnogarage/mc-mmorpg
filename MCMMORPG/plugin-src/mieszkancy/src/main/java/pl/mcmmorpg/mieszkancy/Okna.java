package pl.mcmmorpg.mieszkancy;

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
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Rozmowa z NPC w natywnym oknie Papera (Dialog API 1.21.11).
 *
 * <p>Wzór z pluginu Chowance ({@code gra/Okna.java}): przyciski NIE używają callbacków Papera
 * ({@code DialogAction.customClick(callback, …)} gubił ~1/3 kliknięć, pomiar chw-test A-007), tylko własnego
 * klucza {@code mieszkancy:klik} z ładunkiem {@code {o:<okno>,b:<przycisk>}} obsłużonego w
 * {@link PlayerCustomClickEvent}. Klik w nieaktualne okno pokazuje okno bieżące.
 */
public final class Okna implements Listener {
  public static final Key KLIK = Key.key("mieszkancy", "klik");
  private static final Pattern KLUCZ_O = Pattern.compile("\\bo:(-?\\d+)");
  private static final Pattern KLUCZ_B = Pattern.compile("\\bb:(-?\\d+)");

  private final Mieszkancy m;
  private final Map<UUID, Integer> wersja = new ConcurrentHashMap<>();
  private final Map<UUID, Biezace> biezace = new ConcurrentHashMap<>();

  private record Biezace(int wersja, List<Consumer<Player>> akcje, Runnable ponow) {}

  public record Przycisk(String etykieta, String podpowiedz, Consumer<Player> akcja) {}

  public Okna(Mieszkancy m) { this.m = m; }

  public void pokaz(Player p, String tytul, List<String> tresc, List<Przycisk> przyciski, Przycisk wyjscie) {
    UUID id = p.getUniqueId();
    int w = wersja.merge(id, 1, Integer::sum);
    List<Consumer<Player>> akcje = new ArrayList<>();
    List<ActionButton> btn = new ArrayList<>();
    for (Przycisk pr : przyciski) btn.add(przycisk(w, akcje, pr));
    List<DialogBody> body = new ArrayList<>();
    for (String linia : tresc) body.add(DialogBody.plainMessage(Mieszkancy.mm(linia), 320));
    DialogBase base = DialogBase.builder(Mieszkancy.mm(tytul))
        .canCloseWithEscape(true)
        .pause(false)
        .afterAction(DialogBase.DialogAfterAction.CLOSE)
        .body(body)
        .build();
    DialogType typ;
    if (btn.isEmpty()) {
      // multiAction z pustą listą = „actions cannot be empty” (Chowance A-006).
      typ = DialogType.notice(przycisk(w, akcje, wyjscie != null ? wyjscie : new Przycisk("<gray>Do widzenia", "", x -> {})));
    } else if (wyjscie == null) {
      typ = DialogType.multiAction(btn).columns(1).build();
    } else {
      typ = DialogType.multiAction(btn).columns(1).exitAction(przycisk(w, akcje, wyjscie)).build();
    }
    Dialog d = Dialog.create(b -> b.empty().base(base).type(typ));
    Runnable ponow = () -> {
      Player on = Bukkit.getPlayer(id);
      if (on != null && on.isOnline()) pokaz(on, tytul, tresc, przyciski, wyjscie);
    };
    biezace.put(id, new Biezace(w, akcje, ponow));
    p.showDialog(d);
  }

  private ActionButton przycisk(int w, List<Consumer<Player>> akcje, Przycisk pr) {
    int b = akcje.size();
    akcje.add(pr.akcja());
    DialogAction akcja = DialogAction.customClick(KLIK, BinaryTagHolder.binaryTagHolder("{o:" + w + ",b:" + b + "}"));
    Component tip = pr.podpowiedz() == null || pr.podpowiedz().isBlank() ? null : Mieszkancy.mm(pr.podpowiedz());
    return ActionButton.create(Mieszkancy.mm(pr.etykieta()), tip, 260, akcja);
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
    Runnable r = () -> obsluz(p, o, b);
    if (Bukkit.isPrimaryThread()) r.run(); else Bukkit.getScheduler().runTask(m, r);
  }

  private void obsluz(Player p, int o, int b) {
    Biezace cur = biezace.get(p.getUniqueId());
    if (m.debug) m.getLogger().info("[debug] klik " + p.getName() + " okno=" + o + " przycisk=" + b + " aktualne=" + (cur == null ? null : cur.wersja()));
    if (cur == null || cur.wersja() != o || b < 0 || b >= cur.akcje().size()) {
      if (cur != null && cur.wersja() != o) cur.ponow().run();
      return;
    }
    biezace.remove(p.getUniqueId());
    wersja.merge(p.getUniqueId(), 1, Integer::sum);
    cur.akcje().get(b).accept(p);
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) {
    wersja.remove(e.getPlayer().getUniqueId());
    biezace.remove(e.getPlayer().getUniqueId());
  }

  /** Pierwsze Dialog.create po starcie ładuje rejestry (~300 ms w ticku, spark world1 30.09) — robimy to bez gracza. */
  public static void rozgrzej() {
    DialogBase base = DialogBase.builder(Component.text("x")).canCloseWithEscape(true).pause(false)
        .afterAction(DialogBase.DialogAfterAction.CLOSE).build();
    ActionButton ok = ActionButton.builder(Component.text("ok"))
        .action(DialogAction.customClick(KLIK, BinaryTagHolder.binaryTagHolder("{o:0,b:0}"))).build();
    Dialog.create(b -> b.empty().base(base).type(DialogType.notice(ok)));
  }
}
