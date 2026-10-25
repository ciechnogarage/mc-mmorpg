package pl.mcmmorpg.napady;

import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Napady: jednostki walcza na serwerze (ciala MythicMobs z AI vanilla, dowodzi plugin; awaryjnie manekiny),
 * gracz moze dolaczyc, a po smierci oglada do konca jako widz.
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

  /** Gracz w napadzie: skad przyszedl i w jakim trybie (przywracane po napadzie, po smierci jest widzem). */
  static final class Dolaczenie {
    final int napad;
    final Strona strona;
    final Location powrot;
    final GameMode tryb;
    boolean polegl;
    Dolaczenie(int napad, Strona strona, Location powrot, GameMode tryb) {
      this.napad = napad; this.strona = strona; this.powrot = powrot; this.tryb = tryb;
    }
  }

  /** polegli, ktorych napad skonczyl sie, gdy byli na ekranie smierci: przywracani przy odrodzeniu */
  final Map<UUID, Dolaczenie> doPrzywrocenia = new HashMap<>();
  World swiat;
  private Boolean mm;

  @Override public void onEnable() {
    saveDefaultConfig();
    wczytajSkory();
    swiat = swiatNapadow();
    sprawdzAktywacje();
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

  /**
   * Osobny swiat "napady": plaski (4 warstwy), bez spawnu mobow, staly dzien. Osobny, bo w spigot.yml
   * activation range ustawia sie per swiat, a moby MM bez gracza w 24 bl. inaczej prawie stoja.
   */
  private World swiatNapadow() {
    String n = getConfig().getString("swiat", "napady");
    World w = Bukkit.getWorld(n);
    if (w == null) w = new WorldCreator(n).generator(new PlaskiGenerator()).generateStructures(false).createWorld();
    if (w == null) { getLogger().severe("Nie da sie stworzyc swiata " + n + " - napady w swiecie glownym"); return Bukkit.getWorlds().get(0); }
    w.setGameRule(GameRules.SPAWN_MOBS, false);
    w.setGameRule(GameRules.ADVANCE_TIME, false);
    w.setGameRule(GameRules.ADVANCE_WEATHER, false);
    w.setGameRule(GameRules.KEEP_INVENTORY, true);
    w.setTime(6000);
    w.setSpawnLocation(0, -59, 0);
    return w;
  }

  /** Bez activation range 0 jednostki MM daleko od gracza dostaja AI raz na 20 s (wake-up-inactive). */
  private void sprawdzAktywacje() {
    var y = YamlConfiguration.loadConfiguration(new java.io.File("spigot.yml"));
    String k = "world-settings." + swiat.getName() + ".entity-activation-range.monsters";
    int v = y.getInt(k, y.getInt("world-settings.default.entity-activation-range.monsters", 32));
    if (v > 0) getLogger().warning("spigot.yml: " + k + " = " + v + " (powinno byc 0) - jednostki MM bez gracza w "
        + v + " bl. beda prawie stac. Dopisz world-settings." + swiat.getName() + ".entity-activation-range: {monsters: 0, raiders: 0}");
    else getLogger().info("activation range dla " + swiat.getName() + ": monsters " + v + " (wylaczone dla napadow)");
  }

  /** cialo: mm (domyslnie, gdy MythicMobs jest) albo manekin */
  boolean cialaMM() {
    if (mm == null) mm = !"manekin".equals(getConfig().getString("cialo", "mm")) && CialaMM.dostepne();
    return mm;
  }

  boolean przebrania() { return getConfig().getBoolean("przebrania", true) && CialaMM.przebrania(); }

  /** Koniec napadu: kazdy dolaczony wraca tam, skad przyszedl, w swoim trybie gry. */
  void zwolnijGraczy(int napad, String powod) {
    for (var it = dolaczeni.entrySet().iterator(); it.hasNext(); ) {
      var e = it.next();
      if (e.getValue().napad != napad) continue;
      it.remove();
      Player p = Bukkit.getPlayer(e.getKey());
      if (p == null) continue;
      if (p.isDead()) { doPrzywrocenia.put(p.getUniqueId(), e.getValue()); continue; }
      przywroc(p, e.getValue());
      p.sendMessage("Napad " + napad + " zakonczony: " + powod + ". Wracasz.");
    }
  }

  void przywroc(Player p, Dolaczenie d) {
    if (p.getGameMode() != d.tryb) p.setGameMode(d.tryb);
    if (d.powrot != null && d.powrot.getWorld() != null) p.teleport(d.powrot);
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
        napadyNsWPomiarze / 1e6 / t.size(), swiat.getEntityCount());
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
      Napad n = new Napad(this, id, new Arena(swiat, slot * 200, 0));
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
        cs.sendMessage(String.format(Locale.ROOT, "MSPT %.1f | aktywne %d/%d | kolejka %d | ruch %s | cialo %s%s",
            Bukkit.getAverageTickTime(), aktywne.size(), getConfig().getInt("limit-naraz"), kolejka.size(),
            getConfig().getString("ruch"), cialaMM() ? "mm" : "manekin", przebrania() ? "+lib" : ""));
        for (Napad n : aktywne.values()) cs.sendMessage(n.opis());
      }
      case "dolacz" -> {
        if (!(cs instanceof Player p)) return true;
        if (a.length < 3) { cs.sendMessage("/napad dolacz <id> atak|obrona"); return true; }
        Napad n = aktywne.get(Integer.parseInt(a[1]));
        if (n == null) { cs.sendMessage("Nie ma takiego napadu."); return true; }
        Strona s = a[2].startsWith("o") ? Strona.OBRONA : Strona.ATAK;
        Dolaczenie stare = dolaczeni.get(p.getUniqueId());
        if (stare != null && stare.polegl) { cs.sendMessage("Polegles w tym napadzie - obserwujesz do konca (/napad opusc)."); return true; }
        dolaczeni.put(p.getUniqueId(), stare != null ? new Dolaczenie(n.id, s, stare.powrot, stare.tryb)
            : new Dolaczenie(n.id, s, p.getLocation(), p.getGameMode()));
        p.teleport(n.punktDolaczenia(s));
        n.zdarzenie("dolacza", p.getName() + ":" + s.name());
        p.sendMessage("Dolaczasz do napadu " + n.id + " po stronie: " + s.nazwa);
      }
      case "opusc" -> {
        if (cs instanceof Player p) {
          Dolaczenie d = dolaczeni.remove(p.getUniqueId());
          if (d != null) { przywroc(p, d); p.sendMessage("Opuszczasz napad."); }
        }
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
      case "cialo" -> {
        if (!admin || a.length < 2) return brak(cs);
        getConfig().set("cialo", a[1]);
        mm = null;
        cs.sendMessage("Cialo nowych napadow: " + (cialaMM() ? "mm" : "manekin") + (przebrania() ? " + skora LD" : ""));
      }
      case "przebrania" -> {
        if (!admin || a.length < 2) return brak(cs);
        getConfig().set("przebrania", Boolean.parseBoolean(a[1]));
        cs.sendMessage("Przebrania LD: " + przebrania());
      }
      case "ai" -> {
        // diagnoza ciala: /napad ai <napad> <jednostka> - cele AI (Paper MobGoals), target, linia wzroku
        if (!admin || a.length < 3) return brak(cs);
        Napad n = aktywne.get(Integer.parseInt(a[1]));
        int jid = Integer.parseInt(a[2]);
        Jednostka j = n == null ? null : n.jednostki.stream().filter(x -> x.id == jid).findFirst().orElse(null);
        if (j == null) { cs.sendMessage("Brak jednostki."); return true; }
        cs.sendMessage(j.strona + " " + j.rola + " " + j.cialo.getType() + " akcja=" + j.akcja + " hp=" + j.cialo.getHealth()
            + " reka=" + j.cialo.getEquipment().getItemInMainHand().getType());
        if (j.cialo instanceof org.bukkit.entity.Mob m) {
          var t = m.getTarget();
          cs.sendMessage("target=" + (t == null ? "-" : t.getType() + "@" + Math.round(t.getLocation().distance(m.getLocation())))
              + " widzi=" + (t != null && m.hasLineOfSight(t)) + " sciezka=" + m.getPathfinder().hasPath()
              + " aware=" + m.isAware());
          for (var g : Bukkit.getMobGoals().getRunningGoals(m)) cs.sendMessage(" biegnie: " + g.getKey().getNamespacedKey().getKey());
          for (var g : Bukkit.getMobGoals().getAllGoals(m)) cs.sendMessage(" cel: " + g.getKey().getNamespacedKey().getKey() + " " + g.getTypes());
        }
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
    return d == null ? null : aktywne.get(d.napad);
  }
}
