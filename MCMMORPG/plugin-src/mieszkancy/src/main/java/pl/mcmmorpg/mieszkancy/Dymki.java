package pl.mcmmorpg.mieszkancy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Dymki nad głową NPC zamiast czatu (decyzja właściciela 16.11 przy Chowańcach: „aby npcy mówiły w dymkach a nie
 * spamowały jak łby na czacie”). Jeden TextDisplay na gracza, widoczny tylko dla niego, nieprzetrwały,
 * znika sam po czasie czytania (2 s + 60 ms/znak, 3–8 s).
 */
public final class Dymki {
  private final Mieszkancy m;
  private final Map<UUID, TextDisplay> aktywne = new ConcurrentHashMap<>();
  /** gracz → napis nad NPC ukryty na czas dymku (żeby nie nachodziły na siebie) */
  private final Map<UUID, Entity> ukryte = new ConcurrentHashMap<>();

  public Dymki(Mieszkancy m) { this.m = m; }

  public static long czasTicki(String tekst) {
    long ms = 2000 + 60L * tekst.length();
    return Math.max(60, Math.min(160, ms / 50));
  }

  /** Kwestia nad głową mówiącego; {@code zastap} (stały napis nad NPC) jest na ten czas ukryty dla gracza. */
  public void powiedz(Player p, Entity mowca, String kto, String tekst, Entity zastap) {
    String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(Mieszkancy.mm(tekst));
    Location gdzie = mowca.getLocation().add(0, mowca.getHeight() + 0.45, 0);
    UUID u = p.getUniqueId();
    zdejmij(u);
    if (zastap != null && zastap.isValid()) { p.hideEntity(m, zastap); ukryte.put(u, zastap); }
    TextDisplay t = gdzie.getWorld().spawn(gdzie, TextDisplay.class, x -> {
      x.setPersistent(false);
      x.setVisibleByDefault(false);
      x.setBillboard(Display.Billboard.CENTER);
      x.setShadowed(true);
      x.setLineWidth(180);
      x.setBackgroundColor(Color.fromARGB(0xC0140F0A));
      x.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.8f, 0.8f, 0.8f), new AxisAngle4f()));
      x.text(Mieszkancy.mm(kto + "\n<#f3ead2>" + tekst));
      x.getPersistentDataContainer().set(m.kluczSmiec, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
    });
    p.showEntity(m, t);
    aktywne.put(u, t);
    Bukkit.getScheduler().runTaskLater(m, () -> { if (aktywne.get(u) == t) zdejmij(u); }, czasTicki(plain));
  }

  public void zdejmij(UUID gracz) {
    TextDisplay t = aktywne.remove(gracz);
    if (t != null) t.remove();
    Entity e = ukryte.remove(gracz);
    Player p = Bukkit.getPlayer(gracz);
    if (e != null && p != null && e.isValid()) p.showEntity(m, e);
  }

  public void usunWszystkie() {
    for (UUID u : List.copyOf(aktywne.keySet())) zdejmij(u);
  }
}
