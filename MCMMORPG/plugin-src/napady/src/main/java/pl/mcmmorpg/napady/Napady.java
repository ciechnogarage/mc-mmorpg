package pl.mcmmorpg.napady;

import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Prototyp napadow (etap 1): manekiny walcza na serwerze, gracz moze dolaczyc.
 * Napady ida w kolejce: najwyzej {@code limit-naraz} liczonych jednoczesnie, nowy startuje
 * tylko gdy srednie MSPT ponizej progu.
 */
public final class Napady extends JavaPlugin {

  final List<ProfileProperty> skory = new ArrayList<>();
  final Map<Integer, Napad> aktywne = new LinkedHashMap<>();
  final Deque<Integer> kolejka = new ArrayDeque<>();
  /** gracz -> (id napadu, strona) */
  final Map<UUID, Dolaczenie> dolaczeni = new HashMap<>();
  int nastepneId = 1;
  /** pomiar MSPT: czasy tikow serwera [ms] i laczny czas napadow [ns] w oknie pomiaru */
  List<Double> tikiSerwera;
  long napadyNsWPomiarze;
  int pomiarDo;
  CommandSender pomiarDla;

  record Dolaczenie(int napad, Strona strona) {}

  @Override public void onEnable() {
    saveDefaultConfig();
    wczytajSkory();
    getServer().getPluginManager().registerEvents(new Zdarzenia(this), this);
    getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
      @org.bukkit.event.EventHandler
      public void e(com.destroystokyo.paper.event.server.ServerTickEndEvent ev) { koniecTiku(ev); }
    }, this);
    Bukkit.getScheduler().runTaskTimer(this, this::tik, 1L, 1L);
    Bukkit.getScheduler().runTaskTimer(this, this::kolejkuj, 20L, 20L);
    getLogger().info("Napady: " + skory.size() + " skor, limit " + getConfig().getInt("limit-naraz"));
  }

  @Override public void onDisable() {
    for (Napad n : new ArrayList<>(aktywne.values())) n.zakoncz("wylaczenie pluginu");
    aktywne.clear();
  }

  private void wczytajSkory() {
    var res = getResource("skory.yml");
    if (res == null) return;
    var y = YamlConfiguration.loadConfiguration(new InputStreamReader(res, StandardCharsets.UTF_8));
    var sek = y.getConfigurationSection("skins");
    if (sek == null) return;
    for (String k : sek.getKeys(false)) {
      String v = sek.getString(k + ".value"), s = sek.getString(k + ".signature");
      if (v != null && s != null) skory.add(new ProfileProperty("textures", v, s));
    }
  }

  private void tik() {
    long sumaTiku = 0;
    for (Napad n : new ArrayList<>(aktywne.values())) {
      long t0 = System.nanoTime();
      n.tik();
      long ns = System.nanoTime() - t0;
      n.pomiar(ns);
      sumaTiku += ns;
      if (ns > 5_000_000L) getLogger().warning("Napad " + n.id + " tik " + n.tik + " (" + n.faza + "): " + ns / 1000 + " us");
      if (n.skonczony()) aktywne.remove(n.id);
    }
    if (tikiSerwera != null) napadyNsWPomiarze += sumaTiku;
  }

  void koniecTiku(com.destroystokyo.paper.event.server.ServerTickEndEvent ev) {
    if (tikiSerwera == null) return;
    tikiSerwera.add(ev.getTickDuration());
    if (ev.getTickNumber() < pomiarDo) return;
    List<Double> t = new ArrayList<>(tikiSerwera);
    Collections.sort(t);
    double sr = t.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    String w = String.format(Locale.ROOT,
        "POMIAR %d tikow, napadow %d: MSPT sr %.2f p50 %.2f p95 %.2f max %.2f | napady sr %.3f ms/tik | encji %d",
        t.size(), aktywne.size(), sr, t.get(t.size() / 2), t.get((int) (t.size() * 0.95)), t.get(t.size() - 1),
        napadyNsWPomiarze / 1e6 / t.size(), Bukkit.getWorlds().get(0).getEntityCount());
    getLogger().info(w);
    if (pomiarDla != null) pomiarDla.sendMessage(w);
    tikiSerwera = null;
  }

  private void kolejkuj() {
    int limit = getConfig().getInt("limit-naraz", 1);
    double prog = getConfig().getDouble("mspt-prog", 40.0);
    while (!kolejka.isEmpty() && aktywne.size() < limit && Bukkit.getAverageTickTime() < prog) {
      int id = kolejka.poll();
      int slot = wolnySlot();
      World w = Bukkit.getWorlds().get(0);
      Napad n = new Napad(this, id, new Arena(w, slot * 200, 0));
      aktywne.put(id, n);
      n.start();
    }
  }

  private int wolnySlot() {
    Set<Integer> zajete = new HashSet<>();
    for (Napad n : aktywne.values()) zajete.add(n.arena.ox / 200);
    int s = 0;
    while (zajete.contains(s)) s++;
    return s;
  }

  @Override
  public boolean onCommand(CommandSender cs, Command cmd, String label, String[] a) {
    String sub = a.length == 0 ? "stan" : a[0].toLowerCase(Locale.ROOT);
    boolean admin = cs.hasPermission("napady.admin");
    switch (sub) {
      case "start" -> {
        if (!admin) return brak(cs);
        int n = a.length > 1 ? Integer.parseInt(a[1]) : 1;
        for (int i = 0; i < n; i++) kolejka.add(nastepneId++);
        cs.sendMessage("Napady: w kolejce " + kolejka.size() + ", aktywne " + aktywne.size());
      }
      case "stan" -> {
        cs.sendMessage(String.format(Locale.ROOT, "MSPT %.1f | aktywne %d/%d | kolejka %d | ruch %s",
            Bukkit.getAverageTickTime(), aktywne.size(), getConfig().getInt("limit-naraz"), kolejka.size(),
            getConfig().getString("ruch")));
        for (Napad n : aktywne.values()) cs.sendMessage(n.opis());
      }
      case "dolacz" -> {
        if (!(cs instanceof Player p)) return true;
        if (a.length < 3) { cs.sendMessage("/napad dolacz <id> atak|obrona"); return true; }
        Napad n = aktywne.get(Integer.parseInt(a[1]));
        if (n == null) { cs.sendMessage("Nie ma takiego napadu."); return true; }
        Strona s = a[2].startsWith("o") ? Strona.OBRONA : Strona.ATAK;
        dolaczeni.put(p.getUniqueId(), new Dolaczenie(n.id, s));
        p.teleport(n.punktDolaczenia(s));
        n.zdarzenie("dolacza", p.getName() + ":" + s.name());
        p.sendMessage("Dolaczasz do napadu " + n.id + " po stronie: " + s.nazwa);
      }
      case "opusc" -> {
        if (cs instanceof Player p && dolaczeni.remove(p.getUniqueId()) != null) p.sendMessage("Opuszczasz napad.");
      }
      case "stop" -> {
        if (!admin) return brak(cs);
        kolejka.clear();
        for (Napad n : new ArrayList<>(aktywne.values()))
          if (a.length < 2 || n.id == Integer.parseInt(a[1])) n.zakoncz("stop");
      }
      case "limit" -> {
        if (!admin || a.length < 2) return brak(cs);
        getConfig().set("limit-naraz", Integer.parseInt(a[1]));
        cs.sendMessage("Limit: " + a[1]);
      }
      case "ruch" -> {
        if (!admin || a.length < 2) return brak(cs);
        getConfig().set("ruch", a[1]);
        cs.sendMessage("Ruch: " + a[1]);
      }
      case "pomiar" -> {
        if (!admin) return brak(cs);
        int sek = a.length > 1 ? Integer.parseInt(a[1]) : 30;
        tikiSerwera = new ArrayList<>();
        napadyNsWPomiarze = 0;
        pomiarDo = Bukkit.getCurrentTick() + sek * 20;
        pomiarDla = cs instanceof Player ? cs : null;
        cs.sendMessage("Pomiar " + sek + " s, wynik w logu.");
      }
      default -> cs.sendMessage(cmd.getUsage());
    }
    return true;
  }

  private boolean brak(CommandSender cs) {
    cs.sendMessage("Brak uprawnien albo argumentu.");
    return true;
  }

  Napad napadGracza(Player p) {
    Dolaczenie d = dolaczeni.get(p.getUniqueId());
    return d == null ? null : aktywne.get(d.napad());
  }
}
