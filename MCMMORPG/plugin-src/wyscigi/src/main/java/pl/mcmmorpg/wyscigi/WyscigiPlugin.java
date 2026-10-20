package pl.mcmmorpg.wyscigi;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.manager.MountManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Wyścigi Chowańców. Gracz wchodzi tylko z menu (komenda konsoli), wierzchowiec jest z wypożyczalni. */
public final class WyscigiPlugin extends JavaPlugin implements Listener {
  private record Powrot(String swiat, double x, double y, double z, float yaw, float pitch, String profil) {}

  private static final MiniMessage MM = MiniMessage.miniMessage();
  private static final List<String> DOZWOLONE_KOMENDY = List.of("/msg", "/w", "/tell", "/r", "/reply", "/party", "/p", "/friends", "/guild", "/g");

  Ustawienia u;
  Rekordy rekordy;
  private final Map<UUID, Kierowca> kierowcy = new HashMap<>();
  private final List<Wyscig> wyscigi = new ArrayList<>();
  private final Map<UUID, Powrot> powroty = new HashMap<>();
  private final Map<UUID, BossBar> paski = new HashMap<>();
  private final Set<UUID> wlasnyTeleport = new HashSet<>();
  private final Map<UUID, String> profilNaStarcie = new HashMap<>();
  private NamespacedKey kluczWierzchowca;
  private File plikPowrotow;
  private Plugin profile;

  @Override public void onEnable() {
    saveDefaultConfig();
    u = new Ustawienia(getConfig());
    rekordy = new Rekordy(new File(getDataFolder(), "rekordy.yml"), getLogger());
    kluczWierzchowca = new NamespacedKey(this, "wierzchowiec");
    plikPowrotow = new File(getDataFolder(), "powroty.yml");
    wczytajPowroty();
    profile = Bukkit.getPluginManager().getPlugin("MMOProfiles");
    KontrolerWierzchowca.zarejestruj(this);
    Bukkit.getPluginManager().registerEvents(this, this);
    getLogger().info("Tor " + u.tor.nazwa + ": " + String.format(Locale.ROOT, "%.1f", u.tor.dlugosc) + " bloków, " + u.tor.probki() + " próbek.");
    Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 1L);
    Bukkit.getScheduler().runTaskLater(this, () -> {
      World w = swiat();
      if (w == null) { getLogger().warning("Brak świata '" + u.swiat + "' — wyścigi nieaktywne do jego utworzenia."); return; }
      zaladujTor(w);
      for (Entity e : w.getEntities()) if (e.getPersistentDataContainer().has(kluczWierzchowca)) e.remove();
      for (Player p : w.getPlayers()) odeslijObcego(p);
    }, 40L);
  }

  @Override public void onDisable() {
    for (Kierowca k : new ArrayList<>(kierowcy.values())) zakoncz(k, true, "Restart serwera — wracasz.");
    zapiszPowroty();
  }

  World swiat() { return Bukkit.getWorld(u.swiat); }
  Kierowca kierowca(UUID id) { return kierowcy.get(id); }
  private final Map<String, java.util.ArrayDeque<String>> zdarzenia = new HashMap<>();
  /** Zdarzenie jazdy: pierwsze słowo = nick. Ostatnie 40 na gracza do wglądu (`wyscigi zdarzenia`), log tylko z debug: true. */
  void debug(String s) {
    String nick = s.split(" ", 2)[0].toLowerCase(Locale.ROOT);
    java.util.ArrayDeque<String> q = zdarzenia.computeIfAbsent(nick, x -> new java.util.ArrayDeque<>());
    q.addLast(Bukkit.getCurrentTick() + " " + s);
    while (q.size() > 40) q.removeFirst();
    if (getConfig().getBoolean("debug", false)) getLogger().info("[debug] " + s);
  }

  private void zaladujTor(World w) {
    int[] o = u.tor.obszar();
    for (int cx = o[0] >> 4; cx <= o[2] >> 4; cx++) for (int cz = o[1] >> 4; cz <= o[3] >> 4; cz++) w.setChunkForceLoaded(cx, cz, true);
  }

  // ------------------------------------------------------------------ pętla
  private void tick() {
    for (Wyscig w : new ArrayList<>(wyscigi)) {
      try { w.tick(); } catch (RuntimeException e) { getLogger().severe("Błąd wyścigu: " + e); e.printStackTrace(); }
    }
    int teraz = Bukkit.getCurrentTick();
    for (Kierowca k : new ArrayList<>(kierowcy.values())) pilnujDosiadu(k);
    if (teraz % 20 == 0) {
      World w = swiat();
      if (w != null) for (Player p : w.getPlayers()) {
        Kierowca k = kierowcy.get(p.getUniqueId());
        if (k == null) { odeslijObcego(p); continue; }
        String start = profilNaStarcie.getOrDefault(p.getUniqueId(), "");
        String obecny = profil(p);
        if (!start.isEmpty() && !obecny.isEmpty() && !start.equals(obecny)) zakoncz(k, false, null);
      }
    }
  }

  /** ModelEngine potrafi zgubić jeźdźca (teleport, relog klienta) — sadzamy go z powrotem. */
  private void pilnujDosiadu(Kierowca k) {
    Player p = k.gracz();
    if (p == null || k.model == null || k.mob == null || !k.mob.isValid()) return;
    MountManager mm = menedzer(k.model);
    if (mm == null) return;
    if (p.equals(mm.getDriver())) { k.bezKierowcy = 0; return; }
    if (++k.bezKierowcy == 4) {
      wlasnyTeleport.add(p.getUniqueId());
      try { p.teleport(k.mob.getLocation().add(0, 0.3, 0)); } finally { wlasnyTeleport.remove(p.getUniqueId()); }
      mm.mountDriver(p, KontrolerWierzchowca.TYP);
      k.bezKierowcy = 0;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static MountManager menedzer(ActiveModel am) {
    Optional o = am.getMountManager();
    return o.isPresent() ? (MountManager) o.get() : null;
  }

  // ------------------------------------------------------------------ wejście
  String start(Player p, String gatunekId, Wyscig.Tryb tryb) {
    if (kierowcy.containsKey(p.getUniqueId())) return "Już się ścigasz.";
    Ustawienia.Gatunek g = u.gatunki.get(gatunekId);
    if (g == null) return "Nieznany wierzchowiec.";
    World w = swiat();
    if (w == null) return "Tor jest zamknięty.";
    if (!getConfig().getStringList("swiaty-wejscia").contains(p.getWorld().getName())) return "Na wyścig wyruszysz ze Stolicy albo ze świata.";
    if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR) return "Teraz nie możesz.";
    if (p.isInsideVehicle()) return "Najpierw zsiądź.";
    Wyscig wyscig = null;
    if (tryb == Wyscig.Tryb.WYSCIG) for (Wyscig x : wyscigi) if (x.przyjmuje()) { wyscig = x; break; }
    boolean nowy = wyscig == null;
    if (nowy) wyscig = new Wyscig(this, tryb, w);
    int nr = tryb == Wyscig.Tryb.CZASOWKA ? 0 : wyscig.wolneMiejsce();
    if (nr < 0) return "Brak miejsc na starcie.";
    if (nowy) wyscigi.add(wyscig);

    Location l = p.getLocation();
    powroty.put(p.getUniqueId(), new Powrot(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), profil(p)));
    zapiszPowroty();
    profilNaStarcie.put(p.getUniqueId(), profil(p));
    Kierowca k = new Kierowca(p, g, wyscig, nr);
    kierowcy.put(p.getUniqueId(), k);
    wyscig.dolaczyl(k);

    Location miejsce = u.tor.miejsceStartowe(w, nr);
    k.kierunek = miejsce.getYaw();
    Tor.Rzut r = u.tor.rzut(miejsce.getX(), miejsce.getZ(), -1, 0);
    k.indeks = r.indeks();
    k.postep = r.s() > u.tor.dlugosc / 2 ? r.s() - u.tor.dlugosc : r.s();
    k.maxPostep = k.postep;
    wlasnyTeleport.add(p.getUniqueId());
    try { p.teleport(miejsce.clone().add(0, 0.2, 0)); } finally { wlasnyTeleport.remove(p.getUniqueId()); }
    k.zamrozonyDo = Bukkit.getCurrentTick() + 100000;
    dosiadz(k, miejsce, tryb == Wyscig.Tryb.WYSCIG);
    BossBar pasek = paski.computeIfAbsent(p.getUniqueId(), x -> Wyscig.nowyPasek());
    p.showBossBar(pasek);
    if (nowy && tryb == Wyscig.Tryb.WYSCIG) ogloszenie(p, g);
    Wyscig.tytul(p, "<gold>" + u.tor.nazwa + "</gold>", "<gray>Wierzchowiec:</gray> <white>" + g.nazwa() + "</white>", org.bukkit.Sound.ENTITY_HORSE_SADDLE, 1f);
    p.sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <white>W</white><gray> gaz · </gray><white>S</white><gray> hamulec · </gray><white>mysz</white><gray> skręt · </gray>"
        + "<white>Shift w zakręcie</white><gray> poślizg i turbo · </gray><white>spacja</white><gray> podskok · pola </gray><light_purple>▶▶</light_purple><gray> przyspieszają. Widok z boku: </gray><white>F5</white><gray>.</gray>"));
    getLogger().info(p.getName() + " -> " + tryb + " na " + g.id() + " (miejsce " + nr + ")");
    return null;
  }

  private void ogloszenie(Player p, Ustawienia.Gatunek g) {
    for (Player o : Bukkit.getOnlinePlayers()) {
      if (o.equals(p) || kierowcy.containsKey(o.getUniqueId())) continue;
      o.sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <yellow>" + p.getName() + "</yellow> <gray>czeka na wyścig Chowańców (" + u.tor.nazwa + "). Start za "
          + u.poczekalnia + " s — dołącz: <white>Q → Społeczność → Wyścigi</white>.</gray>"));
    }
  }

  private void dosiadz(Kierowca k, Location miejsce, boolean zderzenia) {
    Wolf wilk = miejsce.getWorld().spawn(miejsce, Wolf.class, x -> {
      x.setSilent(true);
      x.setInvulnerable(true);
      x.setPersistent(false);
      x.setRemoveWhenFarAway(false);
      x.setCollidable(zderzenia);
      x.setCanPickupItems(false);
      x.getPersistentDataContainer().set(kluczWierzchowca, PersistentDataType.STRING, k.id.toString());
    });
    Bukkit.getMobGoals().removeAllGoals(wilk);
    k.mob = wilk;
    ModeledEntity me = ModelEngineAPI.createModeledEntity(wilk);
    ActiveModel am = ModelEngineAPI.createActiveModel(k.gatunek.model());
    am.setCanHurt(false);
    me.addModel(am, true);
    me.setBaseEntityVisible(false);
    dosiadzProba(k, am, 0);
  }

  private void dosiadzProba(Kierowca k, ActiveModel am, int proba) {
    Bukkit.getScheduler().runTaskLater(this, () -> {
      Player p = k.gracz();
      if (p == null || !kierowcy.containsKey(k.id) || k.mob == null || !k.mob.isValid()) return;
      MountManager mm = menedzer(am);
      boolean ok = false;
      if (mm != null) {
        mm.setCanDrive(true);
        ok = p.equals(mm.getDriver()) || mm.mountDriver(p, KontrolerWierzchowca.TYP);
      }
      if (ok) {
        k.model = am;
        if (k.wyscig.faza == Wyscig.Faza.JAZDA) k.zamrozonyDo = 0;
      } else if (proba < 10) dosiadzProba(k, am, proba + 1);
      else {
        getLogger().warning("Nie udało się dosiąść " + k.gatunek.model() + " dla " + p.getName());
        zakoncz(k, true, "Wierzchowiec się spłoszył — spróbuj ponownie.");
      }
    }, proba == 0 ? 3L : 5L);
  }

  /** Wierzchowiec wypadł z toru: zsadzamy, stawiamy na linii za ostatnim dobrym miejscem i sadzamy znowu. */
  void powrotNaTor(Kierowca k, String komunikat) {
    Player p = k.gracz();
    if (p == null || k.mob == null || k.model == null) return;
    double s = k.maxPostep - 3;
    Location cel = u.tor.punkt(k.mob.getWorld(), s, 0);
    MountManager mm = menedzer(k.model);
    if (mm != null) mm.dismountDriver();
    k.mob.teleport(cel);
    wlasnyTeleport.add(p.getUniqueId());
    try { p.teleport(cel.clone().add(0, 0.3, 0)); } finally { wlasnyTeleport.remove(p.getUniqueId()); }
    k.kierunek = cel.getYaw();
    k.v = 0; k.poslizg = false; k.turbo = 0; k.ladowanie = 0;
    k.postep = s;
    k.indeks = u.tor.rzut(cel.getX(), cel.getZ(), -1, 0).indeks();
    k.ostX = Double.NaN;
    k.zamrozonyDo = Bukkit.getCurrentTick() + 20;
    if (mm != null) Bukkit.getScheduler().runTaskLater(this, () -> { if (kierowcy.containsKey(k.id)) mm.mountDriver(p, KontrolerWierzchowca.TYP); }, 2L);
    k.wyscig.pasek(p, 30, komunikat);
    debug(p.getName() + " powrot-na-tor s=" + String.format(Locale.ROOT, "%.1f", s));
  }

  // ------------------------------------------------------------------ wyjście
  void wyjdz(Player p, String komunikat) {
    Kierowca k = kierowcy.get(p.getUniqueId());
    if (k != null) zakoncz(k, true, komunikat);
  }

  void zakoncz(Kierowca k, boolean powrot, String komunikat) {
    k.wyscig.kierowcy.remove(k);
    sprzatnij(k, powrot, komunikat);
  }

  /** Usuwa wierzchowca i HUD; nie rusza listy wyścigu (wołane też z jej pętli). */
  void sprzatnij(Kierowca k, boolean powrot, String komunikat) {
    kierowcy.remove(k.id);
    profilNaStarcie.remove(k.id);
    Player p = k.gracz();
    if (k.model != null) {
      MountManager mm = menedzer(k.model);
      if (mm != null) mm.dismountAll();
    }
    if (k.mob != null) {
      ModelEngineAPI.removeModeledEntity(k.mob);
      k.mob.remove();
    }
    ostatniTekst.remove(k.id);
    BossBar pasek = paski.remove(k.id);
    if (p != null && pasek != null) p.hideBossBar(pasek);
    if (p != null && powrot) odeslij(p);
    if (p != null && komunikat != null) p.sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <gray>" + komunikat + "</gray>"));
  }

  void usunWyscig(Wyscig w) { wyscigi.remove(w); }

  private void odeslij(Player p) {
    Powrot pw = powroty.remove(p.getUniqueId());
    zapiszPowroty();
    Location cel = null;
    if (pw != null) {
      World w = Bukkit.getWorld(pw.swiat());
      String obecny = profil(p);
      boolean tenSam = pw.profil().isEmpty() || obecny.isEmpty() || pw.profil().equals(obecny);
      if (w != null && tenSam) cel = new Location(w, pw.x(), pw.y(), pw.z(), pw.yaw(), pw.pitch());
    }
    if (cel == null) cel = awaryjne();
    if (cel == null) return;
    wlasnyTeleport.add(p.getUniqueId());
    try { p.teleport(cel); } finally { wlasnyTeleport.remove(p.getUniqueId()); }
  }

  private Location awaryjne() {
    for (String nazwa : u.swiatyAwaryjne) { World w = Bukkit.getWorld(nazwa); if (w != null) return w.getSpawnLocation(); }
    return Bukkit.getWorlds().get(0).getSpawnLocation();
  }

  /** Gracz w świecie wyścigów bez wyścigu (relog, zmiana postaci, /tp) — wraca tam, skąd przyszedł. */
  private void odeslijObcego(Player p) {
    if (kierowcy.containsKey(p.getUniqueId())) return;
    if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
    odeslij(p);
  }

  // ------------------------------------------------------------------ zdarzenia
  @EventHandler public void wyjscieZGry(PlayerQuitEvent e) {
    Kierowca k = kierowcy.get(e.getPlayer().getUniqueId());
    if (k != null) zakoncz(k, false, null);
  }

  @EventHandler public void wejscie(PlayerJoinEvent e) {
    Player p = e.getPlayer();
    if (swiat() != null && p.getWorld().equals(swiat())) Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline() && p.getWorld().equals(swiat())) odeslijObcego(p); }, 10L);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void teleport(PlayerTeleportEvent e) {
    Player p = e.getPlayer();
    Kierowca k = kierowcy.get(p.getUniqueId());
    if (k == null || wlasnyTeleport.contains(p.getUniqueId())) return;
    if (e.getTo().getWorld() != null && !e.getTo().getWorld().equals(swiat())) zakoncz(k, false, null);
  }

  @EventHandler public void zmianaSwiata(PlayerChangedWorldEvent e) {
    Player p = e.getPlayer();
    if (!p.getWorld().equals(swiat()) || kierowcy.containsKey(p.getUniqueId())) return;
    Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline() && p.getWorld().equals(swiat())) odeslijObcego(p); }, 5L);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void obrazenia(EntityDamageEvent e) {
    if (e.getEntity() instanceof Player p && (kierowcy.containsKey(p.getUniqueId()) || p.getWorld().equals(swiat()))) e.setCancelled(true);
    else if (e.getEntity().getPersistentDataContainer().has(kluczWierzchowca)) e.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void cel(EntityTargetEvent e) { if (e.getEntity().getPersistentDataContainer().has(kluczWierzchowca)) e.setCancelled(true); }

  @EventHandler(priority = EventPriority.LOWEST)
  public void komenda(PlayerCommandPreprocessEvent e) {
    if (!kierowcy.containsKey(e.getPlayer().getUniqueId())) return;
    String k = e.getMessage().split(" ")[0].toLowerCase(Locale.ROOT);
    if (DOZWOLONE_KOMENDY.contains(k)) return;
    e.setCancelled(true);
    e.getPlayer().sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <gray>W trakcie wyścigu komendy są wyłączone. Wyjście: przytrzymaj <white>Shift</white> na postoju.</gray>"));
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void klik(PlayerInteractEvent e) { if (kierowcy.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }

  @EventHandler(priority = EventPriority.LOWEST)
  public void klikEncji(PlayerInteractEntityEvent e) { if (kierowcy.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }

  @EventHandler(priority = EventPriority.LOWEST)
  public void wyrzuc(PlayerDropItemEvent e) { if (kierowcy.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }

  @EventHandler public void glod(FoodLevelChangeEvent e) { if (kierowcy.containsKey(e.getEntity().getUniqueId())) e.setCancelled(true); }

  @EventHandler(ignoreCancelled = true)
  public void niszczenie(BlockBreakEvent e) { if (e.getBlock().getWorld().equals(swiat()) && e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true); }

  @EventHandler(ignoreCancelled = true)
  public void stawianie(BlockPlaceEvent e) { if (e.getBlock().getWorld().equals(swiat()) && e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true); }

  /** Wierzchowce z poprzedniego uruchomienia (crash) wczytują się razem z chunkiem — usuwamy osierocone. */
  @EventHandler public void zaladowane(EntitiesLoadEvent e) {
    if (!e.getWorld().equals(swiat())) return;
    for (Entity en : e.getEntities()) {
      String wl = en.getPersistentDataContainer().get(kluczWierzchowca, PersistentDataType.STRING);
      if (wl == null) continue;
      Kierowca k = kierowcy.get(UUID.fromString(wl));
      if (k == null || k.mob == null || !k.mob.getUniqueId().equals(en.getUniqueId())) en.remove();
    }
  }

  // ------------------------------------------------------------------ HUD / rekordy
  void pasekBoss(Player p, String tekst, float postep) {
    BossBar b = paski.get(p.getUniqueId());
    if (b == null) return;
    // MythicHUD przestawia swoje 7 pasków przy każdej zmianie cudzego — wysyłamy tylko faktyczne zmiany.
    float pr = Math.round(Math.max(0f, Math.min(1f, postep)) * 50f) / 50f;
    if (!tekst.equals(ostatniTekst.put(p.getUniqueId(), tekst))) b.name(MM.deserialize(tekst));
    if (Math.abs(b.progress() - pr) > 0.001f) b.progress(pr);
  }
  private final Map<UUID, String> ostatniTekst = new HashMap<>();

  void pokazRekordy(Player p) {
    if (p == null || !p.isOnline()) return;
    String tor = u.tor.id;
    p.sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <yellow>" + u.tor.nazwa + "</yellow> <gray>— najlepsze przejazdy (" + u.tor.okrazenia + " okr.):</gray>"));
    List<Rekordy.Pozycja> top = rekordy.top(tor, "przejazd", 5);
    if (top.isEmpty()) p.sendMessage(MM.deserialize("<gray>  Jeszcze nikt nie ukończył. Bądź pierwszy!</gray>"));
    for (int i = 0; i < top.size(); i++) {
      Rekordy.Pozycja r = top.get(i);
      Ustawienia.Gatunek g = u.gatunki.get(r.gatunek());
      p.sendMessage(MM.deserialize("<gray>  " + (i + 1) + ".</gray> <white>" + MM.escapeTags(r.nick()) + "</white> <gray>" + (g == null ? "" : g.nazwa()) + "</gray> <yellow>" + Wyscig.czas(r.ms()) + "</yellow>"));
    }
    List<Rekordy.Pozycja> okr = rekordy.top(tor, "okrazenie", 1);
    if (!okr.isEmpty()) p.sendMessage(MM.deserialize("<gray>  Najszybsze okrążenie:</gray> <white>" + MM.escapeTags(okr.get(0).nick()) + "</white> <yellow>" + Wyscig.czas(okr.get(0).ms()) + "</yellow>"));
    long moj = rekordy.osobisty(tor, "przejazd", p.getUniqueId());
    if (moj > 0) p.sendMessage(MM.deserialize("<gray>  Twój rekord:</gray> <green>" + Wyscig.czas(moj) + "</green>"));
  }

  // ------------------------------------------------------------------ komenda (konsola / menu)
  @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
    if (a.length == 0) { s.sendMessage("/wyscigi <czasowka|wyscig> <gracz> <gatunek> | wyjdz <gracz> | rekordy <gracz> | zbuduj | stan"); return true; }
    switch (a[0].toLowerCase(Locale.ROOT)) {
      case "czasowka", "wyscig" -> {
        if (a.length < 3) { s.sendMessage("Użycie: /wyscigi " + a[0] + " <gracz> <gatunek>"); return true; }
        Player p = Bukkit.getPlayerExact(a[1]);
        if (p == null) { s.sendMessage("Brak gracza " + a[1]); return true; }
        String problem = start(p, a[2].toLowerCase(Locale.ROOT), a[0].equalsIgnoreCase("czasowka") ? Wyscig.Tryb.CZASOWKA : Wyscig.Tryb.WYSCIG);
        if (problem != null) {
          p.sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <red>" + problem + "</red>"));
          s.sendMessage("Odmowa: " + problem);
        } else s.sendMessage("OK");
      }
      case "wyjdz" -> {
        Player p = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
        if (p == null) { s.sendMessage("Brak gracza"); return true; }
        wyjdz(p, "Opuszczasz wyścig.");
        s.sendMessage("OK");
      }
      case "rekordy" -> {
        Player p = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
        if (p != null) pokazRekordy(p);
        else for (Rekordy.Pozycja r : rekordy.top(u.tor.id, "przejazd", 10)) s.sendMessage(r.nick() + " " + r.gatunek() + " " + Wyscig.czas(r.ms()));
      }
      case "zbuduj" -> {
        World w = swiat();
        if (w == null) { s.sendMessage("Brak świata " + u.swiat); return true; }
        zaladujTor(w);
        int[] o = u.tor.obszar();
        for (int cx = o[0] >> 4; cx <= o[2] >> 4; cx++) for (int cz = o[1] >> 4; cz <= o[3] >> 4; cz++) { Chunk ch = w.getChunkAt(cx, cz); ch.load(true); }
        int n = u.tor.zbuduj(w);
        Location start = u.tor.punkt(w, -8, 0);
        w.setSpawnLocation(start);
        s.sendMessage("Zbudowano " + n + " kolumn, tor " + String.format(Locale.ROOT, "%.1f", u.tor.dlugosc) + " bloków, obszar " + o[0] + "," + o[1] + " .. " + o[2] + "," + o[3]
            + ", chunki " + ((o[2] >> 4) - (o[0] >> 4) + 1) * ((o[3] >> 4) - (o[1] >> 4) + 1));
      }
      case "stan" -> {
        s.sendMessage("Wyścigi: " + wyscigi.size() + ", kierowcy: " + kierowcy.size());
        for (Wyscig w : wyscigi) s.sendMessage(" - " + w.tryb + " " + w.faza + " " + w.kierowcy.size());
        for (Kierowca k : kierowcy.values())
          s.sendMessage(String.format(Locale.ROOT, " * %s %s v=%.3f kier=%.1f postep=%.1f/%.1f max=%.1f poz=%d meta=%d model=%s turbo=%d poslizg=%s pobocze=%s",
              k.nick, k.gatunek.id(), k.v, k.kierunek, k.postep, u.tor.dlugosc, k.maxPostep, k.pozycja, k.metaTick, k.model != null, k.turbo, k.poslizg, k.pozaTorem));
      }
      case "poz" -> {
        Player p = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
        Kierowca k = p == null ? null : kierowcy.get(p.getUniqueId());
        if (k == null || k.mob == null) { s.sendMessage("BRAK"); return true; }
        Location m = k.mob.getLocation();
        MountManager mm = k.model == null ? null : menedzer(k.model);
        s.sendMessage(String.format(Locale.ROOT, "POZ %.2f %.2f %.2f kier=%.1f v=%.3f postep=%.2f max=%.2f faza=%s meta=%d turbo=%d poslizg=%s lad=%d pobocze=%s kierowca=%s poz=%d",
            m.getX(), m.getY(), m.getZ(), k.kierunek, k.v, k.postep, k.maxPostep, k.wyscig.faza, k.metaTick, k.turbo, k.poslizg, k.ladowanie, k.pozaTorem,
            mm != null && p.equals(mm.getDriver()), k.pozycja));
      }
      case "zdarzenia" -> {
        java.util.ArrayDeque<String> q = a.length > 1 ? zdarzenia.get(a[1].toLowerCase(Locale.ROOT)) : null;
        s.sendMessage(q == null ? "ZDARZENIA" : "ZDARZENIA " + String.join(" | ", q));
      }
      case "tor" -> s.sendMessage("TOR " + String.format(Locale.ROOT, "%.1f", u.tor.dlugosc) + " " + u.tor.eksport(4));
      case "przeladuj" -> {
        if (!kierowcy.isEmpty()) { s.sendMessage("Najpierw niech wszyscy dojadą."); return true; }
        reloadConfig();
        u = new Ustawienia(getConfig());
        s.sendMessage("Przeładowano: tor " + u.tor.nazwa + " " + String.format(Locale.ROOT, "%.1f", u.tor.dlugosc));
      }
      default -> s.sendMessage("Nieznane: " + a[0]);
    }
    return true;
  }

  // ------------------------------------------------------------------ MMOProfiles / zapis
  String profil(Player p) {
    if (profile == null || !profile.isEnabled()) return "";
    try {
      Object dane = profile.getClass().getMethod("getPlayerData", UUID.class).invoke(profile, p.getUniqueId());
      if (dane == null) return "";
      Object obecny = dane.getClass().getMethod("getCurrent").invoke(dane);
      if (obecny == null) return "";
      UUID id = (UUID) obecny.getClass().getMethod("getUniqueId").invoke(obecny);
      return id == null ? "" : id.toString();
    } catch (ReflectiveOperationException | RuntimeException | LinkageError brak) {
      return "";
    }
  }

  private void wczytajPowroty() {
    YamlConfiguration y = YamlConfiguration.loadConfiguration(plikPowrotow);
    for (String id : y.getKeys(false)) {
      powroty.put(UUID.fromString(id), new Powrot(y.getString(id + ".swiat"), y.getDouble(id + ".x"), y.getDouble(id + ".y"), y.getDouble(id + ".z"),
          (float) y.getDouble(id + ".yaw"), (float) y.getDouble(id + ".pitch"), y.getString(id + ".profil", "")));
    }
  }

  private void zapiszPowroty() {
    YamlConfiguration y = new YamlConfiguration();
    for (Map.Entry<UUID, Powrot> e : powroty.entrySet()) {
      String id = e.getKey().toString();
      Powrot p = e.getValue();
      y.set(id + ".swiat", p.swiat()); y.set(id + ".x", p.x()); y.set(id + ".y", p.y()); y.set(id + ".z", p.z());
      y.set(id + ".yaw", p.yaw()); y.set(id + ".pitch", p.pitch()); y.set(id + ".profil", p.profil());
    }
    try { y.save(plikPowrotow); } catch (IOException ex) { getLogger().warning("Nie zapisano powrotów: " + ex.getMessage()); }
  }
}
