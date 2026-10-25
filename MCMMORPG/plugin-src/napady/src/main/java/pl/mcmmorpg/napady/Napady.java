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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Napady: jednostki walcza na serwerze, gracz moze dolaczyc (po smierci oglada do konca jako widz).
 * Poziom szczegolow (LOD): bez widza napad liczy sie symulacja (bez encji), przy graczu dostaje ciala
 * MythicMobs. Ciezka praca (budowa, spawn i usuwanie cial) idzie przez kolejke z budzetem czasu na tik,
 * sciezki A* maja budzet na tik dla calego serwera - koszt pluginu na tik jest ograniczony z gory.
 * Napady ida w kolejce: najwyzej {@code limit-naraz} jednoczesnie, nowy startuje tylko przy MSPT ponizej progu.
 */
public final class Napady extends JavaPlugin {

  final List<ProfileProperty> skory = new ArrayList<>();
  final Map<Integer, Napad> aktywne = new LinkedHashMap<>();
  final Deque<Integer> kolejka = new ArrayDeque<>();
  /** gracz -> (id napadu, strona) */
  final Map<UUID, Dolaczenie> dolaczeni = new HashMap<>();
  /** cialo MM -> jednostka (zdarzenia bez petli po napadach) */
  final Map<UUID, Jednostka> cialaJednostek = new HashMap<>();
  /** areny wedlug slotu; jedna instancja na slot, bo bilety chunkow sa wspolne dla pluginu */
  private final List<Arena> areny = new ArrayList<>();
  /** kolejka pracy: po kolei, w kazdym tiku do budzet-ms (co najmniej jedna pozycja) */
  private final Deque<Runnable> praca = new ArrayDeque<>();
  private int sciezekWTiku;
  int nastepneId = 1;
  long nastepneZiarno = Long.MIN_VALUE;
  /** pomiar MSPT: czasy tikow serwera [ms] i czas napadow [ns] w oknie pomiaru */
  List<Double> tikiSerwera;
  long napadyNsWPomiarze, napadyMaxNs;
  int pomiarDo;
  CommandSender pomiarDla;

  /** Gracz w napadzie: skad przyszedl i w jakim trybie (przywracane po napadzie). Widz = tylko oglada. */
  static final class Dolaczenie {
    final int napad;
    final Strona strona;
    final Location powrot;
    final GameMode tryb;
    final boolean widz;
    boolean polegl;
    Dolaczenie(int napad, Strona strona, Location powrot, GameMode tryb, boolean widz) {
      this.napad = napad; this.strona = strona; this.powrot = powrot; this.tryb = tryb; this.widz = widz;
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
    // po starcie serwera (MM wczytal moby): areny gotowe i pierwsze ciala za nami, zanim ktos dolaczy
    Bukkit.getScheduler().runTaskLater(this, this::przygotujAreny, 40L);
    getLogger().info("Napady: " + skory.size() + " skor, limit " + getConfig().getInt("limit-naraz")
        + ", lod " + getConfig().getString("lod.tryb", "auto") + (cialaMM() ? "" : " (bez MythicMobs: tylko symulacja)"));
  }

  @Override public void onDisable() {
    for (Napad n : new ArrayList<>(aktywne.values())) n.zakoncz("wylaczenie pluginu");
    aktywne.clear();
    // usuniecie cial i biletow bez budzetu: planista juz nie ruszy
    while (!praca.isEmpty()) wykonaj(praca.poll());
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

  /** Ciala MM, gdy MythicMobs jest; bez niego napady licza sie tylko symulacja. */
  boolean cialaMM() {
    if (mm == null) mm = CialaMM.dostepne();
    return mm;
  }

  boolean przebrania() { return getConfig().getBoolean("przebrania", true) && CialaMM.przebrania(); }

  // ---------------------------------------------------------------- kolejka pracy i budzety
  /** Do kolejki pracy (watek glowny, wykonanie w nastepnych tikach w ramach budzetu). */
  void praca(Runnable r) { praca.add(r); }

  /** Jedna nowa sciezka A* z budzetu tego tiku; false = budzet wyczerpany. */
  boolean sciezka() {
    if (sciezekWTiku >= getConfig().getInt("lod.sciezki-na-tik", 6)) return false;
    sciezekWTiku++;
    return true;
  }

  private void wykonaj(Runnable r) {
    try { r.run(); } catch (Exception e) { getLogger().warning("Kolejka pracy: " + e); }
  }

  private void tik() {
    sciezekWTiku = 0;
    long t0 = System.nanoTime();
    for (Napad n : new ArrayList<>(aktywne.values())) {
      long t = System.nanoTime();
      n.tik();
      long ns = System.nanoTime() - t;
      n.pomiar(ns);
      if (ns > 5_000_000L) getLogger().warning("Napad " + n.id + " tik " + n.tik + " (" + n.faza + ", " + n.lod + "): " + ns / 1000 + " us");
      if (n.skonczony()) aktywne.remove(n.id);
    }
    long budzet = (long) (getConfig().getDouble("budzet-ms", 2.0) * 1_000_000L);
    long p0 = System.nanoTime();
    int zrobione = 0;
    while (!praca.isEmpty() && (zrobione == 0 || System.nanoTime() - p0 < budzet)) { wykonaj(praca.poll()); zrobione++; }
    long pns = System.nanoTime() - p0;
    if (pns > budzet + 3_000_000L) getLogger().warning("Kolejka pracy: " + zrobione + " pozycji w " + pns / 1000 + " us (budzet " + budzet / 1000 + ")");
    long razem = System.nanoTime() - t0;
    if (tikiSerwera != null) { napadyNsWPomiarze += razem; napadyMaxNs = Math.max(napadyMaxNs, razem); }
  }

  void koniecTiku(com.destroystokyo.paper.event.server.ServerTickEndEvent ev) {
    if (tikiSerwera == null) return;
    tikiSerwera.add(ev.getTickDuration());
    if (ev.getTickNumber() < pomiarDo) return;
    List<Double> t = new ArrayList<>(tikiSerwera);
    Collections.sort(t);
    double sr = t.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    long ponad10 = t.stream().filter(v -> v > 10).count(), ponad20 = t.stream().filter(v -> v > 20).count(),
        ponad50 = t.stream().filter(v -> v > 50).count();
    int ciala = 0;
    for (Napad n : aktywne.values()) for (Jednostka j : n.jednostki) if (j.cialo != null) ciala++;
    String w = String.format(Locale.ROOT,
        "POMIAR %d tikow, napadow %d (ciala %d): MSPT sr %.2f p50 %.2f p95 %.2f max %.2f | tiki >10 ms %d, >20 ms %d, >50 ms %d"
            + " | napady sr %.3f max %.2f ms/tik | encji %d | kolejka pracy %d",
        t.size(), aktywne.size(), ciala, sr, t.get(t.size() / 2), t.get((int) (t.size() * 0.95)), t.get(t.size() - 1),
        ponad10, ponad20, ponad50, napadyNsWPomiarze / 1e6 / t.size(), napadyMaxNs / 1e6, swiat.getEntityCount(), praca.size());
    getLogger().info(w);
    if (pomiarDla != null) pomiarDla.sendMessage(w);
    tikiSerwera = null;
  }

  // ---------------------------------------------------------------- areny
  Arena arena(int slot) {
    while (areny.size() <= slot) areny.add(new Arena(swiat, areny.size() * 200, 0));
    return areny.get(slot);
  }

  /**
   * Areny 0..{@code areny}-1 zbudowane zawczasu (asynchroniczne chunki, budowa kolumnami w kolejce), a w
   * pierwszej - dwa ciala MM ze sciezka i celem: pierwszy spawn/setTarget kosztuje kilkadziesiat ms
   * (ladowanie klas, zimny JIT), lepiej przy starcie niz w tiku z graczem.
   */
  private void przygotujAreny() {
    int n = getConfig().getInt("areny", 4);
    for (int s = 0; s < n; s++) {
      Arena a = arena(s);
      a.zaladuj().whenComplete((v, err) -> Bukkit.getScheduler().runTask(this, () -> {
        if (err != null) { getLogger().warning("Arena x=" + a.ox + ": " + err); return; }
        if (zajeta(a)) return;   // napad juz ja materializuje i sam dobuduje
        a.trzymaj(this);
        boolean budowa = !a.zbudowana();
        if (budowa) for (int x = Uklad.MIN_X; x < Uklad.MIN_X + Uklad.W; x++) { int kx = x; praca(() -> a.zbudujKolumne(kx)); }
        praca(() -> {
          if (!zajeta(a)) a.zwolnij(this);
          getLogger().info("Arena x=" + a.ox + (budowa ? " zbudowana" : " gotowa"));
        });
      }));
    }
  }

  private boolean zajeta(Arena a) {
    for (Napad n : aktywne.values()) if (n.arena == a && n.lod != Napad.Lod.SYM) return true;
    return false;
  }

  /**
   * Pierwszy spawn MM po starcie kosztuje jednorazowo setki ms (pomiar 25.10 na klonie: 474 ms, drugi 66 ms,
   * sciezka i setTarget < 0,3 ms). Placimy to w ServerLoadEvent (koniec startu), a nie w pierwszym napadzie.
   */
  void rozgrzej() {
    if (!cialaMM()) return;
    Arena a = arena(0);
    long t0 = System.nanoTime();
    a.swiat.getChunkAt(a.loc(2, 20));
    a.swiat.getChunkAt(a.loc(6, 22));
    long t1 = System.nanoTime();
    LivingEntity e = CialaMM.spawn("NAP_ATAK_WOJOWNIK", a.loc(2, 20));
    if (!(e instanceof Mob m)) { if (e != null) e.remove(); getLogger().warning("Rozgrzewka: MM nie zna NAP_ATAK_WOJOWNIK"); return; }
    m.setPersistent(false);
    long t2 = System.nanoTime();
    LivingEntity cel = CialaMM.spawn("NAP_OBRONA_WOJOWNIK", a.loc(6, 22));
    long t3 = System.nanoTime();
    m.getPathfinder().moveTo(a.loc(10, 20), 1.0);
    if (cel != null) { cel.setPersistent(false); m.setTarget(cel); }
    long t4 = System.nanoTime();
    m.remove();
    if (cel != null) cel.remove();
    getLogger().info(String.format(Locale.ROOT, "Rozgrzewka cial MM (ms): chunki %.1f spawn %.1f spawn2 %.1f sciezka+cel %.1f usun %.1f",
        (t1 - t0) / 1e6, (t2 - t1) / 1e6, (t3 - t2) / 1e6, (t4 - t3) / 1e6, (System.nanoTime() - t4) / 1e6));
  }

  private int wolnySlot() {
    Set<Integer> zajete = new HashSet<>();
    for (Napad n : aktywne.values()) zajete.add(n.arena.ox / 200);
    int s = 0;
    while (zajete.contains(s)) s++;
    return s;
  }

  private void kolejkuj() {
    int limit = getConfig().getInt("limit-naraz", 4);
    double prog = getConfig().getDouble("mspt-prog", 40.0);
    while (!kolejka.isEmpty() && aktywne.size() < limit && Bukkit.getAverageTickTime() < prog) {
      int id = kolejka.poll();
      long ziarno = nastepneZiarno != Long.MIN_VALUE ? nastepneZiarno++ : new Random().nextLong();
      Napad n = new Napad(this, id, arena(wolnySlot()), ziarno);
      aktywne.put(id, n);
      n.start();
    }
  }

  // ---------------------------------------------------------------- gracze
  /** Koniec napadu: kazdy dolaczony wraca tam, skad przyszedl, w swoim trybie gry. */
  void zwolnijGraczy(int napad, String powod) {
    for (var it = dolaczeni.entrySet().iterator(); it.hasNext(); ) {
      var e = it.next();
      if (e.getValue().napad != napad) continue;
      it.remove();
      Player p = Bukkit.getPlayer(e.getKey());
      if (p == null) continue;
      if (p.isDead()) { doPrzywrocenia.put(p.getUniqueId(), e.getValue()); continue; }
      przywroc(p, e.getValue(), true);
      p.sendMessage("Napad " + napad + " zakonczony: " + powod + ". Wracasz.");
    }
  }

  /** Tryb i miejsce sprzed napadu; teleport asynchroniczny (bez skoku tiku), przy wyjsciu z gry - od razu. */
  void przywroc(Player p, Dolaczenie d, boolean async) {
    if (p.getGameMode() != d.tryb) p.setGameMode(d.tryb);
    if (d.powrot == null || d.powrot.getWorld() == null) return;
    if (async && isEnabled()) p.teleportAsync(d.powrot); else p.teleport(d.powrot);
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

  /**
   * Wejscie do napadu (walka albo widz): ciala musza stac, zanim gracz sie pojawi - przy symulacji
   * napad sie materializuje, a teleport idzie po materializacji.
   */
  private void wejdz(Player p, Napad n, Strona s, boolean widz) {
    Dolaczenie stare = dolaczeni.get(p.getUniqueId());
    Dolaczenie d = stare != null ? new Dolaczenie(n.id, s, stare.powrot, stare.tryb, widz)
        : new Dolaczenie(n.id, s, p.getLocation(), p.getGameMode(), widz);
    dolaczeni.put(p.getUniqueId(), d);
    n.zdarzenie(widz ? "obserwuje" : "dolacza", p.getName() + (widz ? "" : ":" + s.name()));
    UUID u = p.getUniqueId();
    Runnable tp = () -> {
      Player q = Bukkit.getPlayer(u);
      if (q == null || dolaczeni.get(u) != d) return;
      if (widz) q.setGameMode(GameMode.SPECTATOR);
      else if (q.getGameMode() == GameMode.SPECTATOR) q.setGameMode(d.tryb == GameMode.SPECTATOR ? GameMode.SURVIVAL : d.tryb);
      q.teleportAsync(widz ? n.punktWidza() : n.punktDolaczenia(s));
      q.sendMessage(widz ? "Obserwujesz napad " + n.id + " (/napad opusc - wyjscie)." : "Dolaczasz do napadu " + n.id + " po stronie: " + s.nazwa);
    };
    if (n.lod == Napad.Lod.CIALA) tp.run();
    else {
      n.poMaterializacji.add(tp);
      n.zadajCial();
      p.sendMessage("Napad " + n.id + ": przygotowanie pola bitwy...");
    }
  }

  @Override
  public boolean onCommand(CommandSender cs, Command cmd, String label, String[] a) {
    String sub = a.length == 0 ? "stan" : a[0].toLowerCase(Locale.ROOT);
    boolean admin = cs.hasPermission("napady.admin");
    switch (sub) {
      case "start" -> {
        if (!admin) return brak(cs);
        int n = a.length > 1 ? Integer.parseInt(a[1]) : 1;
        // ziarno: powtarzalna symulacja (kolejne napady ziarno+1, +2...)
        if (a.length > 2) nastepneZiarno = Long.parseLong(a[2]);
        for (int i = 0; i < n; i++) kolejka.add(nastepneId++);
        cs.sendMessage("Napady: w kolejce " + kolejka.size() + ", aktywne " + aktywne.size());
      }
      case "stan" -> {
        cs.sendMessage(String.format(Locale.ROOT, "MSPT %.1f | aktywne %d/%d | kolejka %d | lod %s | ciala %s%s | praca %d",
            Bukkit.getAverageTickTime(), aktywne.size(), getConfig().getInt("limit-naraz"), kolejka.size(),
            getConfig().getString("lod.tryb", "auto"), cialaMM() ? "mm" : "brak (symulacja)", przebrania() ? "+lib" : "", praca.size()));
        for (Napad n : aktywne.values()) cs.sendMessage(n.opis());
      }
      case "dolacz", "obserwuj" -> {
        if (!(cs instanceof Player p)) return true;
        boolean widz = sub.equals("obserwuj");
        if (a.length < (widz ? 2 : 3)) { cs.sendMessage(widz ? "/napad obserwuj <id>" : "/napad dolacz <id> atak|obrona"); return true; }
        Napad n = aktywne.get(Integer.parseInt(a[1]));
        if (n == null || n.skonczony()) { cs.sendMessage("Nie ma takiego napadu."); return true; }
        if (!cialaMM()) { cs.sendMessage("Napady bez MythicMobs licza sie tylko symulacja - nie da sie dolaczyc."); return true; }
        Dolaczenie stare = dolaczeni.get(p.getUniqueId());
        if (stare != null && stare.polegl && stare.napad == n.id) { cs.sendMessage("Polegles w tym napadzie - obserwujesz do konca (/napad opusc)."); return true; }
        wejdz(p, n, widz ? Strona.ATAK : a[2].startsWith("o") ? Strona.OBRONA : Strona.ATAK, widz);
      }
      case "opusc" -> {
        if (cs instanceof Player p) {
          Dolaczenie d = dolaczeni.remove(p.getUniqueId());
          if (d != null) { przywroc(p, d, true); p.sendMessage("Opuszczasz napad."); }
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
      case "lod" -> {
        if (!admin || a.length < 2 || !List.of("auto", "ciala", "symulacja").contains(a[1])) {
          cs.sendMessage("/napad lod auto|ciala|symulacja (teraz " + getConfig().getString("lod.tryb", "auto") + ")");
          return true;
        }
        getConfig().set("lod.tryb", a[1]);
        cs.sendMessage("LOD: " + a[1]);
      }
      case "przebrania" -> {
        if (!admin || a.length < 2) return brak(cs);
        getConfig().set("przebrania", Boolean.parseBoolean(a[1]));
        cs.sendMessage("Przebrania LD: " + przebrania());
      }
      case "ai" -> {
        // diagnoza: /napad ai <napad> <jednostka> - model, rozkaz, cialo (target, sciezka, cele AI Paper MobGoals)
        if (!admin || a.length < 3) return brak(cs);
        Napad n = aktywne.get(Integer.parseInt(a[1]));
        int jid = Integer.parseInt(a[2]);
        Jednostka j = n == null ? null : n.jednostki.stream().filter(x -> x.id == jid).findFirst().orElse(null);
        if (j == null) { cs.sendMessage("Brak jednostki."); return true; }
        cs.sendMessage(String.format(Locale.ROOT, "%s %s akcja=%s hp=%.1f model=(%.1f, %.1f) rozkaz=(%.1f, %.1f) cel=%s",
            j.strona, j.rola, j.akcja, j.hp, j.x, j.z, j.px, j.pz,
            j.cel != null ? "#" + j.cel.id : j.celGracz != null ? j.celGracz.getName() : "-"));
        Mob m = j.cialo;
        if (m == null) { cs.sendMessage("bez ciala (symulacja)"); return true; }
        var t = m.getTarget();
        cs.sendMessage("cialo " + m.getType() + " target=" + (t == null ? "-" : t.getType() + "@" + Math.round(t.getLocation().distance(m.getLocation())))
            + " widzi=" + (t != null && m.hasLineOfSight(t)) + " sciezka=" + m.getPathfinder().hasPath() + " aware=" + m.isAware());
        for (var g : Bukkit.getMobGoals().getRunningGoals(m)) cs.sendMessage(" biegnie: " + g.getKey().getNamespacedKey().getKey());
      }
      case "pomiar" -> {
        if (!admin) return brak(cs);
        int sek = a.length > 1 ? Integer.parseInt(a[1]) : 30;
        tikiSerwera = new ArrayList<>();
        napadyNsWPomiarze = 0;
        napadyMaxNs = 0;
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
