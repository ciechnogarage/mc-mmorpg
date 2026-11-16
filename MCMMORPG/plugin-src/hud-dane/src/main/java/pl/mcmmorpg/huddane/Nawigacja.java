package pl.mcmmorpg.huddane;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Cel zadania w HUD-zie (jedno zrodlo: zadanie MMOCore albo samouczek Chowancow), strzalka i odleglosc do celu kroku
 * (miejsca z configu, dopasowane po fragmencie tekstu kroku) oraz kierunek ostatnich obrazen.
 */
final class Nawigacja implements Listener {
  record Miejsce(String fragment, String swiat, double x, double y, double z) {}
  static final String[] KIERUNKI = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
  static final long OBRAZENIA_MS = 1200;

  final List<Miejsce> miejsca = new ArrayList<>();
  final int maxKrok, maxPx;
  final Map<UUID, double[]> obrazenia = new ConcurrentHashMap<>(); // {x, z, czas}

  Nawigacja(ConfigurationSection cfg) {
    maxKrok = cfg == null ? 38 : cfg.getInt("max-krok", 38);
    // Szerokosc linii celu w PIKSELACH GUI czcionki gry (nie w znakach: "m" ma 6 px, "i" 2). Tekst zaczyna sie na x 18,
    // widoczne tlo ramki konczy sie na ~147 GUI -> 128 px (16.11: limit 22 znakow wystawal poza ramke).
    maxPx = cfg == null ? 128 : cfg.getInt("max-px", 128);
    ConfigurationSection m = cfg == null ? null : cfg.getConfigurationSection("miejsca");
    if (m != null) for (String k : m.getKeys(false)) {
      ConfigurationSection s = m.getConfigurationSection(k);
      miejsca.add(new Miejsce(s.getString("fragment", k), s.getString("swiat"), s.getDouble("x"), s.getDouble("y"), s.getDouble("z")));
    }
  }

  /** {tytul, postep, krok} (pelne, bez kolorow) albo null, gdy nie ma czego pokazac. */
  String[] zadanie(Player p) {
    if ("tak".equals(PlaceholderAPI.setPlaceholders(p, "%chowance_ma_zadanie_mmocore%")))
      return new String[]{czysty(PlaceholderAPI.setPlaceholders(p, "%mmocore_quest%")),
          PlaceholderAPI.setPlaceholders(p, "%mmocore_quest_progress%") + "%", czysty(PlaceholderAPI.setPlaceholders(p, "%mmocore_quest_objective%"))};
    if ("true".equals(PlaceholderAPI.setPlaceholders(p, "%chowance_kroki_aktywne%")))
      return new String[]{"Pierwsze kroki", PlaceholderAPI.setPlaceholders(p, "%chowance_kroki_postep%"),
          czysty(PlaceholderAPI.setPlaceholders(p, "%chowance_kroki_opis%"))};
    return null;
  }

  static String czysty(String s) {
    return s == null ? "" : s.replaceAll("(?i)[&§][0-9a-fk-orx]", "").replaceAll("<[^>]+>", "").strip();
  }

  /** Krok w jednej linii (stary placeholder zadanie_krok). */
  String krok(String s) { return przytnij(s, maxKrok); }

  static String przytnij(String s, int max) {
    s = czysty(s);
    return s.length() > max ? s.substring(0, max - 1).stripTrailing() + "…" : s;
  }

  /** Szerokosc znakow czcionki minecraft:default 1.21.11 (szerokosci.txt z client.jar); brak w tabeli = 6. */
  static final Map<Integer, Integer> SZEROKOSC = new HashMap<>();
  static {
    try (BufferedReader r = new BufferedReader(new InputStreamReader(
        Nawigacja.class.getResourceAsStream("/szerokosci.txt"), StandardCharsets.UTF_8))) {
      for (String l; (l = r.readLine()) != null; ) {
        if (l.isBlank() || l.startsWith("#")) continue;
        String[] c = l.trim().split(" ");
        SZEROKOSC.put(Integer.parseInt(c[0]), Integer.parseInt(c[1]));
      }
    } catch (Exception e) { throw new IllegalStateException("szerokosci.txt", e); }
  }

  static int szer(String s) {
    int w = 0;
    for (int i = 0; i < s.length(); ) { int c = s.codePointAt(i); w += SZEROKOSC.getOrDefault(c, 6); i += Character.charCount(c); }
    return w;
  }

  /** Tekst bez kolorow przyciety do max px; za dlugi konczy sie "…". */
  static String przytnijPx(String s, int max) {
    s = czysty(s);
    if (szer(s) <= max) return s;
    int k = s.length();
    while (k > 0 && szer(s.substring(0, k).stripTrailing() + "…") > max) k--;
    return k == 0 ? "" : s.substring(0, k).stripTrailing() + "…";
  }

  /** Linia 1 albo 2 kroku zawinietego po slowach do max-px; reszta po 2. linii konczy sie "…". */
  String linia(String s, int n) {
    s = czysty(s);
    if (szer(s) <= maxPx) return n == 1 ? s : "";
    int ciecie = -1;
    for (int i = s.indexOf(' '); i > 0; i = s.indexOf(' ', i + 1)) {
      if (szer(s.substring(0, i)) <= maxPx) ciecie = i; else break;
    }
    if (ciecie <= 0) { // pierwsze slowo za szerokie: ciecie twarde
      ciecie = s.length();
      while (ciecie > 1 && szer(s.substring(0, ciecie)) > maxPx) ciecie--;
    }
    return n == 1 ? s.substring(0, ciecie).stripTrailing() : przytnijPx(s.substring(ciecie).strip(), maxPx);
  }

  /** Tytul przyciety tak, by cala linia "tytul postep odl" (HUD: pojedyncze spacje) miescila sie w max-px. */
  String tytul(String tytul, String postep, String odl) {
    int reszta = szer(" " + postep) + (odl.isEmpty() ? 0 : szer(" " + odl));
    return przytnijPx(tytul, Math.max(0, maxPx - reszta));
  }

  /** {kierunek, odleglosc w m} do miejsca pasujacego do kroku, albo null. */
  String[] kierunek(Player p, String krok) {
    if (krok == null) return null;
    for (Miejsce m : miejsca) {
      if (!krok.contains(m.fragment())) continue;
      World w = Bukkit.getWorld(m.swiat());
      Location l = p.getLocation();
      if (w == null || l.getWorld() != w) return null;
      double dx = m.x() - l.getX(), dz = m.z() - l.getZ(), d = Math.sqrt(dx * dx + dz * dz);
      if (d < 4) return new String[]{"tu", "0"};
      return new String[]{sektor(dx, dz, l.getYaw()), String.valueOf((int) Math.round(d))};
    }
    return null;
  }

  /** 8 sektorow wzgledem patrzenia: n = przed toba. Yaw MC: 0 = +Z, 90 = -X. */
  static String sektor(double dx, double dz, float yaw) {
    double cel = Math.toDegrees(Math.atan2(-dx, dz));
    double wzgl = ((cel - yaw) % 360 + 360 + 22.5) % 360;
    return KIERUNKI[(int) (wzgl / 45) % 8];
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void oberwal(EntityDamageByEntityEvent e) {
    if (!(e.getEntity() instanceof Player p)) return;
    Entity z = e.getDamager();
    if (z instanceof Projectile pr && pr.getShooter() instanceof Entity s) z = s;
    if (z == p) return;
    obrazenia.put(p.getUniqueId(), new double[]{z.getLocation().getX(), z.getLocation().getZ(), System.currentTimeMillis()});
  }

  @EventHandler public void wyjscie(PlayerQuitEvent e) { obrazenia.remove(e.getPlayer().getUniqueId()); }

  String kierunekObrazen(Player p) {
    double[] o = obrazenia.get(p.getUniqueId());
    if (o == null || System.currentTimeMillis() - o[2] > OBRAZENIA_MS) return "brak";
    Location l = p.getLocation();
    return sektor(o[0] - l.getX(), o[1] - l.getZ(), l.getYaw());
  }
}
