package pl.mcmmorpg.chowance.gra;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.mcmmorpg.chowance.Chowance;

/**
 * Dymki nad NPC zamiast czatu (zgłoszenie właściciela 16.11: „aby npcy mówiły w dymkach a nie spamowały jak łby na czacie”).
 * Jeden TextDisplay na gracza, widoczny tylko dla niego (visibleByDefault=false + showEntity), nieprzetrwały;
 * nowa kwestia zastępuje poprzednią, dymek znika sam po czasie zależnym od długości tekstu.
 * Koszt: najwyżej jedna encja-wyświetlacz na rozmawiającego gracza, przez kilka sekund, bez pętli.
 */
public final class Dymki {
  private final Chowance c;
  private final Map<UUID, TextDisplay> aktywne = new ConcurrentHashMap<>();
  /** gracz → napis ukryty na czas dymku (etykieta młodego), wraca po zniknięciu dymku */
  private final Map<UUID, Entity> ukryte = new ConcurrentHashMap<>();

  public Dymki(Chowance c) { this.c = c; }

  /** Czas czytania: 2 s + 60 ms na znak, 3–8 s (≈15 znaków/s). */
  public static long czasTicki(String tekstBezTagow) {
    long ms = 2000 + 60L * tekstBezTagow.length();
    return Math.max(60, Math.min(160, ms / 50));
  }

  /** Kwestia NPC: imię w pierwszej linii, tekst pod nim, nad głową mówiącego. */
  public void powiedz(Player p, Entity mowca, String kto, String tekst) {
    String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(Chowance.mm(tekst));
    pokaz(p, mowca.getLocation().add(0, mowca.getHeight() + 0.55, 0), kto + "\n<white>" + tekst, czasTicki(plain), null);
  }

  /**
   * Dymek w podanym miejscu; {@code zastap} (np. stała etykieta młodego) jest na ten czas ukryty dla gracza,
   * żeby napisy nie nachodziły na siebie.
   */
  public void pokaz(Player p, Location gdzie, String miniMessage, long ticki, Entity zastap) {
    UUID u = p.getUniqueId();
    zdejmij(u);
    if (zastap != null && zastap.isValid()) { p.hideEntity(c.plugin, zastap); ukryte.put(u, zastap); }
    TextDisplay t = gdzie.getWorld().spawn(gdzie, TextDisplay.class, x -> {
      x.setPersistent(false);
      x.setVisibleByDefault(false);
      x.setBillboard(Display.Billboard.CENTER);
      x.setShadowed(true);
      x.setLineWidth(200);
      x.setBackgroundColor(org.bukkit.Color.fromARGB(0xB0101010));
      x.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.75f, 0.75f, 0.75f), new AxisAngle4f()));
      x.text(Chowance.mm(miniMessage));
    });
    p.showEntity(c.plugin, t);
    aktywne.put(u, t);
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (aktywne.get(u) == t) zdejmij(u);
    }, ticki);
  }

  /** Usuwa dymek gracza i przywraca ukrytą etykietę. */
  public void zdejmij(UUID gracz) {
    TextDisplay t = aktywne.remove(gracz);
    if (t != null) t.remove();
    Entity e = ukryte.remove(gracz);
    Player p = Bukkit.getPlayer(gracz);
    if (e != null && p != null && e.isValid()) p.showEntity(c.plugin, e);
  }

  public void usunWszystkie() {
    for (UUID u : java.util.List.copyOf(aktywne.keySet())) zdejmij(u);
  }
}
