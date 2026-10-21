package pl.mcmmorpg.huddane;

import java.util.ArrayList;
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
  final int maxKrok;
  final Map<UUID, double[]> obrazenia = new ConcurrentHashMap<>(); // {x, z, czas}

  Nawigacja(ConfigurationSection cfg) {
    maxKrok = cfg == null ? 38 : cfg.getInt("max-krok", 38);
    ConfigurationSection m = cfg == null ? null : cfg.getConfigurationSection("miejsca");
    if (m != null) for (String k : m.getKeys(false)) {
      ConfigurationSection s = m.getConfigurationSection(k);
      miejsca.add(new Miejsce(s.getString("fragment", k), s.getString("swiat"), s.getDouble("x"), s.getDouble("y"), s.getDouble("z")));
    }
  }

  /** {tytul, postep, krok} albo null, gdy nie ma czego pokazac. */
  String[] zadanie(Player p) {
    if ("tak".equals(PlaceholderAPI.setPlaceholders(p, "%chowance_ma_zadanie_mmocore%")))
      return new String[]{PlaceholderAPI.setPlaceholders(p, "%mmocore_quest%"),
          PlaceholderAPI.setPlaceholders(p, "%mmocore_quest_progress%") + "%", przytnij(PlaceholderAPI.setPlaceholders(p, "%mmocore_quest_objective%"))};
    if ("true".equals(PlaceholderAPI.setPlaceholders(p, "%chowance_kroki_aktywne%")))
      return new String[]{"Pierwsze kroki", PlaceholderAPI.setPlaceholders(p, "%chowance_kroki_postep%"),
          przytnij(PlaceholderAPI.setPlaceholders(p, "%chowance_kroki_opis%"))};
    return null;
  }

  private String przytnij(String s) {
    s = s == null ? "" : s.replaceAll("(?i)[&§][0-9a-fk-orx]", "").replaceAll("<[^>]+>", "");
    return s.length() > maxKrok ? s.substring(0, maxKrok - 1).stripTrailing() + "…" : s;
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

  String kierunekObrazen(Player p) {
    double[] o = obrazenia.get(p.getUniqueId());
    if (o == null || System.currentTimeMillis() - o[2] > OBRAZENIA_MS) return "brak";
    Location l = p.getLocation();
    return sektor(o[0] - l.getX(), o[1] - l.getZ(), l.getYaw());
  }
}
