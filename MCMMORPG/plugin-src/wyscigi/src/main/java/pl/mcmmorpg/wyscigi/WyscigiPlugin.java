package pl.mcmmorpg.wyscigi;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation;
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
public final class WyscigiPlugin extends JavaPlugin implements Listener, org.bukkit.command.TabCompleter {
  private record Powrot(String swiat, double x, double y, double z, float yaw, float pitch, String profil) {}

  private static final MiniMessage MM = MiniMessage.miniMessage();
  private static final List<String> DOZWOLONE_KOMENDY = List.of("/msg", "/w", "/tell", "/r", "/reply", "/party", "/p", "/friends", "/guild", "/g");

  Ustawienia u;
  Rekordy rekordy;
  private ZapisAsync zapisPowrotow;
  private final Map<UUID, Kierowca> kierowcy = new HashMap<>();
  private final List<Wyscig> wyscigi = new ArrayList<>();
  private final Map<UUID, Powrot> powroty = new HashMap<>();
  private final Map<UUID, BossBar> paski = new HashMap<>();
  private final Set<UUID> wlasnyTeleport = new HashSet<>();
  private final Set<UUID> obserwatorzy = new HashSet<>();   // admin/QA oglądający tor (komenda konsoli), nie odsyłani
  private final Map<UUID, String> profilNaStarcie = new HashMap<>();
  private NamespacedKey kluczWierzchowca;
  private File plikPowrotow;
  private Plugin profile;
  Bonusy bonusy;
  Klawisze klawisze;

  @Override public void onEnable() {
    saveDefaultConfig();
    u = new Ustawienia(getConfig(), getLogger());
    rekordy = new Rekordy(new File(getDataFolder(), "rekordy.yml"), getLogger());
    zapisPowrotow = new ZapisAsync("Wyscigi-zapis-powrotow", getLogger());
    kluczWierzchowca = new NamespacedKey(this, "wierzchowiec");
    plikPowrotow = new File(getDataFolder(), "powroty.yml");
    wczytajPowroty();
    profile = Bukkit.getPluginManager().getPlugin("MMOProfiles");
    KontrolerWierzchowca.zarejestruj(this);
    bonusy = new Bonusy(this, kluczWierzchowca);
    klawisze = new Klawisze(this);
    PacketEvents.getAPI().getEventManager().registerListener(klawisze);
    Bukkit.getPluginManager().registerEvents(this, this);
    getLogger().info("Tor " + u.tor.nazwa + ": " + String.format(Locale.ROOT, "%.1f", u.tor.dlugosc) + " bloków, " + u.tor.probki() + " próbek.");
    Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 1L);
    Bukkit.getScheduler().runTaskLater(this, () -> {
      World w = swiat();
      if (w == null) { getLogger().warning("Brak świata '" + u.swiat + "' — wyścigi nieaktywne do jego utworzenia."); return; }
      zaladujTor(w, false);   // forceload zapisuje się w świecie — zdejmujemy po poprzednim uruchomieniu
      for (Entity e : w.getEntities()) if (e.getPersistentDataContainer().has(kluczWierzchowca)) e.remove();
      for (Player p : w.getPlayers()) odeslijObcego(p);
    }, 40L);
  }

  @Override public void onDisable() {
    for (Kierowca k : new ArrayList<>(kierowcy.values())) zakoncz(k, true, "Restart serwera — wracasz.");
    if (bonusy != null) { bonusy.zdejmijSkrzynki(); bonusy.zdejmijStrzalki(); }
    if (klawisze != null) PacketEvents.getAPI().getEventManager().unregisterListener(klawisze);
    zapiszPowroty(true);
    if (rekordy != null) rekordy.zamknij();
  }

  World swiat() { return Bukkit.getWorld(u.swiat); }
  Kierowca kierowca(UUID id) { return kierowcy.get(id); }
  private final Map<String, java.util.ArrayDeque<String>> zdarzenia = new HashMap<>();
  /** Zdarzenie jazdy: pierwsze słowo = nick. Ostatnie 40 na gracza do wglądu (`wyscigi zdarzenia`), log tylko z debug: true. */
  /** Obrót kamery gracza (względny yaw, pochylenie bez zmian) — sterowanie A/D z kamerą za wierzchowcem. */
  void obroc(Player p, float deltaYaw) {
    try { PacketEvents.getAPI().getPlayerManager().sendPacket(p, new WrapperPlayServerPlayerRotation(deltaYaw, true, 0f, true)); }
    catch (RuntimeException e) { debug(p.getName() + " obrot-blad " + e.getMessage()); }
  }
  void ustawKamere(Player p, float yaw) {
    try { PacketEvents.getAPI().getPlayerManager().sendPacket(p, new WrapperPlayServerPlayerRotation(yaw, false, 12f, false)); }
    catch (RuntimeException ignored) {}
  }

  void debug(String s) {
    String nick = s.split(" ", 2)[0].toLowerCase(Locale.ROOT);
    java.util.ArrayDeque<String> q = zdarzenia.computeIfAbsent(nick, x -> new java.util.ArrayDeque<>());
    q.addLast(Bukkit.getCurrentTick() + " " + s);
    while (q.size() > 40) q.removeFirst();
    if (getConfig().getBoolean("debug", false)) getLogger().info("[debug] " + s);
  }

  /** Tor (~117 chunków) jest załadowany tylko na czas wyścigów — na produkcji pusty tor nie kosztuje nic. */
  private void zaladujTor(World w, boolean zaladuj) {
    int[] o = u.tor.obszar();
    for (int cx = o[0] >> 4; cx <= o[2] >> 4; cx++) for (int cz = o[1] >> 4; cz <= o[3] >> 4; cz++) w.setChunkForceLoaded(cx, cz, zaladuj);
  }

  // ------------------------------------------------------------------ pętla
  private void tick() {
    for (Wyscig w : new ArrayList<>(wyscigi)) {
      try { w.tick(); } catch (RuntimeException e) { getLogger().severe("Błąd wyścigu: " + e); e.printStackTrace(); }
    }
    boolean zInnymi = wyscigi.stream().anyMatch(w -> w.tryb == Wyscig.Tryb.WYSCIG && w.faza != Wyscig.Faza.KONIEC);
    World sw = swiat();
    if (zInnymi && !bonusy.saSkrzynki() && sw != null) bonusy.postawSkrzynki(sw, u.tor);
    if (!zInnymi && bonusy.saSkrzynki()) bonusy.zdejmijSkrzynki();
    try { bonusy.tick(wyscigi); } catch (RuntimeException e) { getLogger().severe("Błąd bonusów: " + e); e.printStackTrace(); }
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
  String start(Player p, String gatunekId, Wyscig.Tryb tryb) { return start(p, gatunekId, tryb, false); }

  /** ponowny = "Od nowa" z pauzy: gracz jest już na torze, punkt powrotu zostaje stary. */
  String start(Player p, String gatunekId, Wyscig.Tryb tryb, boolean ponowny) {
    if (kierowcy.containsKey(p.getUniqueId())) return "Już się ścigasz.";
    Ustawienia.Gatunek g = u.gatunki.get(gatunekId);
    if (g == null) return "Nieznany wierzchowiec.";
    World w = swiat();
    if (w == null) return "Tor jest zamknięty.";
    if (!ponowny && !getConfig().getStringList("swiaty-wejscia").contains(p.getWorld().getName())) return "Na wyścig wyruszysz ze Stolicy albo ze świata.";
    if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR) return "Teraz nie możesz.";
    if (p.isInsideVehicle()) return "Najpierw zsiądź.";
    Wyscig wyscig = null;
    if (tryb == Wyscig.Tryb.WYSCIG) for (Wyscig x : wyscigi) if (x.przyjmuje()) { wyscig = x; break; }
    boolean nowy = wyscig == null;
    if (nowy) wyscig = new Wyscig(this, tryb, w);
    int nr = tryb == Wyscig.Tryb.CZASOWKA ? 0 : wyscig.wolneMiejsce();
    if (nr < 0) return "Brak miejsc na starcie.";
    if (nowy) { wyscigi.add(wyscig); zaladujTor(w, true); bonusy.postawStrzalki(w, u.tor); }

    if (!ponowny) {
      Location l = p.getLocation();
      powroty.put(p.getUniqueId(), new Powrot(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), profil(p)));
      zapiszPowroty();
    }
    profilNaStarcie.put(p.getUniqueId(), profil(p));
    Kierowca k = new Kierowca(p, g, wyscig, nr);
    kierowcy.put(p.getUniqueId(), k);
    klawisze.sciganci.add(p.getUniqueId());
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
    p.sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <white>W</white><gray> gaz · </gray><white>S</white><gray> hamulec · </gray><white>A/D</white><gray> skręt · </gray>"
        + "<white>Shift w zakręcie</white><gray> poślizg i turbo · </gray><white>spacja</white><gray> podskok · złote </gray><gold>»</gold><gray> przyspieszają. Widok z boku: </gray><white>F5</white><gray>.</gray>"));
    getLogger().info(p.getName() + " -> " + tryb + " na " + g.id() + " (miejsce " + nr + ")");
    return null;
  }

  private void ogloszenie(Player p, Ustawienia.Gatunek g) {
    for (Player o : Bukkit.getOnlinePlayers()) {
      if (o.equals(p) || kierowcy.containsKey(o.getUniqueId())) continue;
      o.sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <yellow>" + p.getName() + "</yellow> <gray>czeka na wyścig Chowańców (" + u.tor.nazwa + "). Start za "
          + u.poczekalnia + " s — dołącz: <white>Q → Drużyna → Wyścigi</white>.</gray>"));
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
    am.setScale(u.skalaModelu);          // Chowaniec-towarzysz jest mały pod jeźdźcem; na torze wygląda jak wierzchowiec
    am.setHitboxScale(1.0);
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
        ustawKamere(p, k.kierunek);
        animujPoslizg(k, am);
        if (k.wyscig.faza == Wyscig.Faza.JAZDA) k.zamrozonyDo = 0;
      } else if (proba < 10) dosiadzProba(k, am, proba + 1);
      else {
        getLogger().warning("Nie udało się dosiąść " + k.gatunek.model() + " dla " + p.getName());
        zakoncz(k, true, "Wierzchowiec się spłoszył — spróbuj ponownie.");
      }
    }, proba == 0 ? 3L : 5L);
  }

  /**
   * Poślizg = wierzchowiec siada na zadzie: kość body (rodzic siodła) przechyla się do tyłu i lekko na bok,
   * po trafieniu kołysze się. Animacja chodu pozostałych kości (nogi, ogon) działa dalej.
   */
  private void animujPoslizg(Kierowca k, ActiveModel am) {
    am.getBone("body").ifPresent(b -> b.setManualAnimator(kosc -> {
      float t = k.przechylTest >= 0 ? k.przechylTest : k.przechyl;
      float kolys = Bukkit.getCurrentTick() < k.zachwianieDo ? (float) Math.sin(Bukkit.getCurrentTick() * 1.3) * 0.35f : 0f;
      // ModelEngine co tick ustawia pozę z modelu, potem woła ten animator — dodajemy przechył do niej
      // (getLeftEuler/getPosition zwracają kopie, zmieniać trzeba przez mutate*).
      // Zmierzone na modelu (klon 20.10): oś X = przód/tył (+X opuszcza łeb), oś Z = przechył na bok, pozycja w blokach.
      float pitch = (float) Math.toRadians(-u.przechylPoslizgu) * t, roll = (float) Math.toRadians(6) * t * k.stronaPoslizgu + kolys;
      if (k.pozaTest != null) {
        float[] q = k.pozaTest;
        kosc.getLocalTransform().mutateLeftEuler(e -> e.add(q[0], q[1], q[2]));
        kosc.getLocalTransform().mutatePosition(v -> v.add(0f, q[3], 0f));
        return;
      }
      kosc.getLocalTransform().mutateLeftEuler(e -> e.add(pitch, 0f, roll));
      kosc.getLocalTransform().mutatePosition(v -> v.add(0f, -0.08f * t, 0f));
    }));
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
    klawisze.sciganci.remove(k.id);
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

  void usunWyscig(Wyscig w) {
    wyscigi.remove(w);
    if (wyscigi.isEmpty() && swiat() != null) { zaladujTor(swiat(), false); bonusy.zdejmijStrzalki(); bonusy.zdejmijSkrzynki(); }
  }

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
    if (kierowcy.containsKey(p.getUniqueId()) || obserwatorzy.contains(p.getUniqueId())) return;
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
      if (wl.equals("dekoracja")) { if (wyscigi.isEmpty()) en.remove(); continue; }
      Kierowca k = kierowcy.get(UUID.fromString(wl));
      if (k == null || k.mob == null || !k.mob.getUniqueId().equals(en.getUniqueId())) en.remove();
    }
  }

  // ------------------------------------------------------------------ klawisze w wyścigu (z pakietów)
  void klawisz(Player p, String akcja) {
    Kierowca k = kierowcy.get(p.getUniqueId());
    if (k == null) return;
    switch (akcja) {
      case "pauza" -> {
        p.updateInventory();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open wyscigi_pauza " + p.getName());
      }
      case "bonus" -> {
        p.updateInventory();
        if (k.bonus == null) k.wyscig.pasek(p, 20, k.wyscig.tryb == Wyscig.Tryb.WYSCIG ? "§7Brak bonusu — przejedź przez skrzynkę." : "§7W czasówce nie ma bonusów.");
        else if (k.wyscig.faza == Wyscig.Faza.JAZDA) bonusy.uzyj(k);
      }
      case "cios" -> { if (k.wyscig.tryb == Wyscig.Tryb.WYSCIG) bonusy.cios(k); }
      default -> {}
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
  private static final String UZYCIE = "/wyscigi czasowka|wyscig <gracz> <gatunek> | odnowa <gracz> | wyjdz <gracz> | rekordy [gracz] | obserwuj <gracz> [x y z]"
      + " | zbuduj | stan | poz <gracz> | zdarzenia <nick> | tor | przeladuj | poza <gracz> <4 liczby> | przechyl <gracz> <stopnie> [strona]";
  private static final List<String> PODKOMENDY = List.of("czasowka", "wyscig", "odnowa", "wyjdz", "rekordy", "obserwuj", "zbuduj", "stan", "poz", "zdarzenia",
      "tor", "przeladuj", "poza", "przechyl");

  @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
    List<String> opcje = new ArrayList<>();
    String pierwsza = a.length > 0 ? a[0].toLowerCase(Locale.ROOT) : "";
    if (a.length == 1) opcje.addAll(PODKOMENDY);
    else if (a.length == 2 && List.of("czasowka", "wyscig", "odnowa", "wyjdz", "rekordy", "obserwuj", "poz", "poza", "przechyl", "zdarzenia").contains(pierwsza))
      Bukkit.getOnlinePlayers().forEach(p -> opcje.add(p.getName()));
    else if (a.length == 3 && (pierwsza.equals("czasowka") || pierwsza.equals("wyscig"))) opcje.addAll(u.gatunki.keySet());
    String pref = a[a.length - 1].toLowerCase(Locale.ROOT);
    return opcje.stream().filter(x -> x.toLowerCase(Locale.ROOT).startsWith(pref)).sorted().toList();
  }

  @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
    try {
      return wykonaj(s, a);
    } catch (NumberFormatException e) {
      s.sendMessage("Błędna liczba w poleceniu. Użycie: " + UZYCIE);
      return true;
    }
  }

  private boolean wykonaj(CommandSender s, String[] a) {
    if (a.length == 0) { s.sendMessage(UZYCIE); return true; }
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
      case "poza" -> {
        Player p = a.length > 5 ? Bukkit.getPlayerExact(a[1]) : null;
        Kierowca k = p == null ? null : kierowcy.get(p.getUniqueId());
        if (k == null) { s.sendMessage("Brak kierowcy (użycie: /wyscigi poza <gracz> <4 liczby>)"); return true; }
        k.pozaTest = new float[]{Float.parseFloat(a[2]), Float.parseFloat(a[3]), Float.parseFloat(a[4]), Float.parseFloat(a[5])};
        s.sendMessage("OK poza");
      }
      case "przechyl" -> {
        Player p = a.length > 2 ? Bukkit.getPlayerExact(a[1]) : null;
        Kierowca k = p == null ? null : kierowcy.get(p.getUniqueId());
        if (k == null) { s.sendMessage("Brak kierowcy (użycie: /wyscigi przechyl <gracz> <stopnie> [strona])"); return true; }
        k.przechylTest = Float.parseFloat(a[2]);
        if (a.length > 3) k.stronaPoslizgu = Integer.parseInt(a[3]);
        s.sendMessage("OK przechyl=" + k.przechylTest);
      }
      case "obserwuj" -> {
        Player p = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
        if (p == null || swiat() == null) { s.sendMessage("Brak gracza/swiata"); return true; }
        if (obserwatorzy.remove(p.getUniqueId())) { p.setFlying(false); p.setAllowFlight(false); odeslij(p); s.sendMessage("Koniec obserwacji"); return true; }
        obserwatorzy.add(p.getUniqueId());
        Location gdzie = p.getLocation();
        powroty.putIfAbsent(p.getUniqueId(), new Powrot(gdzie.getWorld().getName(), gdzie.getX(), gdzie.getY(), gdzie.getZ(), gdzie.getYaw(), gdzie.getPitch(), profil(p)));
        Location cel = a.length > 4 ? new Location(swiat(), Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4])) : u.tor.punkt(swiat(), -8, 0).add(0, 6, 0);
        wlasnyTeleport.add(p.getUniqueId());
        try { p.teleport(cel); } finally { wlasnyTeleport.remove(p.getUniqueId()); }
        Bukkit.getScheduler().runTaskLater(this, () -> { p.setAllowFlight(true); p.setFlying(true); }, 5L);
        s.sendMessage("Obserwacja: " + p.getName());
      }
      case "odnowa" -> {
        Player p = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
        Kierowca k = p == null ? null : kierowcy.get(p.getUniqueId());
        if (k == null) { s.sendMessage("Brak kierowcy"); return true; }
        if (k.wyscig.tryb != Wyscig.Tryb.CZASOWKA) {
          p.sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <gray>Wyścig z innymi trwa — od nowa zaczniesz tylko czasówkę.</gray>"));
          return true;
        }
        String g = k.gatunek.id();
        zakoncz(k, false, null);
        String problem = start(p, g, Wyscig.Tryb.CZASOWKA, true);
        s.sendMessage(problem == null ? "OK" : "Odmowa: " + problem);
        if (problem != null) odeslij(p);
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
        zaladujTor(w, true);
        int[] o = u.tor.obszar();
        for (int cx = o[0] >> 4; cx <= o[2] >> 4; cx++) for (int cz = o[1] >> 4; cz <= o[3] >> 4; cz++) { Chunk ch = w.getChunkAt(cx, cz); ch.load(true); }
        int n = u.tor.zbuduj(w);
        Location start = u.tor.punkt(w, -8, 0);
        w.setSpawnLocation(start);
        if (wyscigi.isEmpty()) zaladujTor(w, false);
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
        try {
          u = new Ustawienia(getConfig(), getLogger());
          s.sendMessage("Przeładowano: tor " + u.tor.nazwa + " " + String.format(Locale.ROOT, "%.1f", u.tor.dlugosc));
        } catch (IllegalArgumentException e) {
          s.sendMessage("Nie przeładowano (zostają poprzednie ustawienia): " + e.getMessage());
        }
      }
      default -> s.sendMessage("Nieznane: " + a[0] + ". Użycie: " + UZYCIE);
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

  /** koncowy = onDisable: dokończ zapisy w tle i zapisz synchronicznie; inaczej zrzut tu, plik w tle. */
  private void zapiszPowroty() { zapiszPowroty(false); }

  private void zapiszPowroty(boolean koncowy) {
    YamlConfiguration y = new YamlConfiguration();
    for (Map.Entry<UUID, Powrot> e : powroty.entrySet()) {
      String id = e.getKey().toString();
      Powrot p = e.getValue();
      y.set(id + ".swiat", p.swiat()); y.set(id + ".x", p.x()); y.set(id + ".y", p.y()); y.set(id + ".z", p.z());
      y.set(id + ".yaw", p.yaw()); y.set(id + ".pitch", p.pitch()); y.set(id + ".profil", p.profil());
    }
    if (koncowy) {
      if (zapisPowrotow != null) zapisPowrotow.zamknij();
      ZapisAsync.zapiszSync(plikPowrotow, y.saveToString(), "powrotów", getLogger());
    } else if (zapisPowrotow != null) zapisPowrotow.zapisz(plikPowrotow, y.saveToString(), "powrotów");
    else ZapisAsync.zapiszSync(plikPowrotow, y.saveToString(), "powrotów", getLogger());
  }
}
