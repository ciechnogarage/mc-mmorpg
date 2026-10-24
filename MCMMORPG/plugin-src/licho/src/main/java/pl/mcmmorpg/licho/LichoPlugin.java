package pl.mcmmorpg.licho;

import com.github.retrooper.packetevents.PacketEvents;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static pl.mcmmorpg.licho.Runda.MM;
import static pl.mcmmorpg.licho.Runda.PREFIKS;

/** „Licho nie śpi” — chowany w sprzętach zagrody (prop hunt). Gracz wchodzi tylko z menu (komenda konsoli). */
public final class LichoPlugin extends JavaPlugin implements Listener {
  private record Powrot(String swiat, double x, double y, double z, float yaw, float pitch, String profil) {}

  private static final List<String> DOZWOLONE_KOMENDY = List.of("/msg", "/w", "/tell", "/r", "/reply", "/party", "/p", "/friends", "/guild", "/g");

  Ustawienia u;
  Rekordy rekordy;
  Mapa mapa;
  NamespacedKey klucz;
  private Runda runda;
  private final Map<UUID, Powrot> powroty = new HashMap<>();
  private final Map<UUID, BossBar> paski = new HashMap<>();
  private final Map<UUID, String> ostatniTekst = new HashMap<>();
  private final Set<UUID> wlasnyTeleport = new HashSet<>();
  private final Set<UUID> obserwatorzy = new HashSet<>();   // admin/QA oglądający zagrodę (komenda konsoli), nie odsyłani
  private final Map<UUID, String> profilNaStarcie = new HashMap<>();
  private File plikPowrotow;
  private Plugin profile;
  private Klawisze klawisze;

  @Override public void onEnable() {
    saveDefaultConfig();
    u = new Ustawienia(getConfig());
    rekordy = new Rekordy(new File(getDataFolder(), "rekordy.yml"), getLogger());
    mapa = new Mapa(getResource("mapa.txt"), getLogger());
    klucz = new NamespacedKey(this, "licho");
    plikPowrotow = new File(getDataFolder(), "powroty.yml");
    wczytajPowroty();
    profile = Bukkit.getPluginManager().getPlugin("MMOProfiles");
    klawisze = new Klawisze(this);
    PacketEvents.getAPI().getEventManager().registerListener(klawisze);
    Bukkit.getPluginManager().registerEvents(this, this);
    getLogger().info(u.nazwaMapy + ": " + mapa.bloki.size() + " bloków, przebrań " + u.przebrania.size() + ".");
    Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 1L);
    Bukkit.getScheduler().runTaskLater(this, () -> {
      World w = swiat();
      if (w == null) { getLogger().warning("Brak świata '" + u.swiat + "' — Licho nieaktywne do jego utworzenia."); return; }
      zaladuj(w, false);   // forceload zapisuje się w świecie — zdejmujemy po poprzednim uruchomieniu
      for (Entity e : w.getEntities()) if (e.getPersistentDataContainer().has(klucz)) e.remove();
      for (Player p : w.getPlayers()) odeslijObcego(p);
    }, 40L);
  }

  @Override public void onDisable() {
    if (runda != null) {
      for (Uczestnik x : new ArrayList<>(runda.gracze.values())) sprzatnij(x, true, "Restart serwera — wracasz.");
      runda.usunEncje();
    }
    if (klawisze != null) PacketEvents.getAPI().getEventManager().unregisterListener(klawisze);
    zapiszPowroty();
  }

  World swiat() { return Bukkit.getWorld(u.swiat); }
  private Uczestnik uczestnik(UUID id) { return runda == null ? null : runda.gracze.get(id); }

  /** Zagroda (16 chunków) jest załadowana tylko na czas rundy — pusta nie kosztuje nic. */
  private void zaladuj(World w, boolean tak) {
    int[] c = mapa.chunki();
    for (int cx = c[0]; cx <= c[2]; cx++) for (int cz = c[1]; cz <= c[3]; cz++) w.setChunkForceLoaded(cx, cz, tak);
  }

  void teleport(Player p, Location l) {
    wlasnyTeleport.add(p.getUniqueId());
    try { p.teleport(l); } finally { wlasnyTeleport.remove(p.getUniqueId()); }
  }

  // ------------------------------------------------------------------ pętla
  private void tick() {
    if (runda == null) return;
    try { runda.tick(); } catch (RuntimeException e) { getLogger().severe("Błąd rundy: " + e); e.printStackTrace(); }
    if (runda == null || Bukkit.getCurrentTick() % 20 != 0) return;
    for (Uczestnik x : new ArrayList<>(runda.gracze.values())) {
      Player p = x.gracz();
      if (p == null) continue;
      String start = profilNaStarcie.getOrDefault(x.id, "");
      String obecny = profil(p);
      if (!start.isEmpty() && !obecny.isEmpty() && !start.equals(obecny)) sprzatnij(x, false, null);
    }
    World w = swiat();
    if (w != null) for (Player p : w.getPlayers()) if (uczestnik(p.getUniqueId()) == null) odeslijObcego(p);
  }

  // ------------------------------------------------------------------ wejście
  String dolacz(Player p) {
    if (uczestnik(p.getUniqueId()) != null) return "Już grasz.";
    World w = swiat();
    if (w == null) return "Zagroda jest zamknięta.";
    if (!u.swiatyWejscia.contains(p.getWorld().getName())) return "Do zagrody wyruszysz ze Stolicy albo ze świata.";
    if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) return "Teraz nie możesz.";
    if (p.isInsideVehicle()) return "Najpierw zsiądź.";
    if (runda != null && !runda.przyjmuje()) return runda.faza == Runda.Faza.KONIEC ? "Runda właśnie się kończy — spróbuj za chwilę." : "Zagroda pełna (" + u.max + " graczy).";
    boolean nowa = runda == null;
    if (nowa) { runda = new Runda(this, w); zaladuj(w, true); }

    Location l = p.getLocation();
    powroty.put(p.getUniqueId(), new Powrot(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), profil(p)));
    zapiszPowroty();
    profilNaStarcie.put(p.getUniqueId(), profil(p));
    Uczestnik x = new Uczestnik(p);
    // Przygoda: klient nie „kopie” pozornych bloków; latanie dozwolone, bo stanie na pozornym bloku serwer liczy jako lot.
    p.setGameMode(GameMode.ADVENTURE);
    p.setAllowFlight(true);
    p.setFlying(false);
    klawisze.gracze.add(p.getUniqueId());
    BossBar pasek = paski.computeIfAbsent(p.getUniqueId(), k -> BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10));
    p.showBossBar(pasek);
    runda.dolacz(x, p);
    if (nowa) ogloszenie(p);
    getLogger().info(p.getName() + " -> runda (" + runda.faza + ", graczy " + runda.gracze.size() + ")");
    return null;
  }

  private void ogloszenie(Player p) {
    for (Player o : Bukkit.getOnlinePlayers()) {
      if (o.equals(p) || uczestnik(o.getUniqueId()) != null) continue;
      o.sendMessage(MM.deserialize(PREFIKS + "<yellow>" + p.getName() + "</yellow> <gray>czeka w " + u.nazwaMapy + " — Licho nie śpi! Potrzeba "
          + u.min + " graczy. Dołącz: <white>Q → Drużyna → Licho</white>.</gray>"));
    }
  }

  // ------------------------------------------------------------------ wyjście
  void wyjdz(Player p, String komunikat) {
    Uczestnik x = uczestnik(p.getUniqueId());
    if (x != null) sprzatnij(x, true, komunikat);
  }

  /** Gracz kończy grę (menu, relog, zmiana postaci, teleport). Tryb gry i latanie wracają, zanim MMOProfiles zapisze postać. */
  void sprzatnij(Uczestnik x, boolean powrot, String komunikat) {
    Runda r = runda;
    if (r != null) r.odejdz(x);
    klawisze.gracze.remove(x.id);
    profilNaStarcie.remove(x.id);
    ostatniTekst.remove(x.id);
    Player p = x.gracz();
    BossBar pasek = paski.remove(x.id);
    if (p != null) {
      if (pasek != null) p.hideBossBar(pasek);
      p.setFlying(false);
      p.setAllowFlight(x.latanieDozwolonePrzed);
      p.setGameMode(x.trybPrzed == null ? GameMode.SURVIVAL : x.trybPrzed);
      p.setFallDistance(0);
      if (powrot) odeslij(p);
      if (komunikat != null) p.sendMessage(MM.deserialize(PREFIKS + "<gray>" + komunikat + "</gray>"));
    }
    if (r != null && r == runda && r.gracze.isEmpty()) zamknij(r);
  }

  /** Koniec podsumowania: wszyscy wracają tam, skąd przyszli. */
  void zakonczRunde(Runda r) {
    for (Uczestnik x : new ArrayList<>(r.gracze.values())) {
      Player p = x.gracz();
      if (p != null) rekordyKrotko(p);
      sprzatnij(x, true, null);
    }
    zamknij(r);
  }

  private void zamknij(Runda r) {
    r.usunEncje();
    if (runda == r) runda = null;
    World w = swiat();
    if (w != null) zaladuj(w, false);
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
    if (cel != null) teleport(p, cel);
  }

  private Location awaryjne() {
    for (String nazwa : u.swiatyAwaryjne) { World w = Bukkit.getWorld(nazwa); if (w != null) return w.getSpawnLocation(); }
    return Bukkit.getWorlds().get(0).getSpawnLocation();
  }

  /** Gracz w świecie Licha bez rundy (relog, zmiana postaci, /tp) — wraca tam, skąd przyszedł. */
  private void odeslijObcego(Player p) {
    if (uczestnik(p.getUniqueId()) != null || obserwatorzy.contains(p.getUniqueId())) return;
    if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
    if (p.getGameMode() == GameMode.ADVENTURE) { p.setGameMode(GameMode.SURVIVAL); p.setAllowFlight(false); }   // po crashu w trakcie rundy
    odeslij(p);
  }

  // ------------------------------------------------------------------ zdarzenia
  @EventHandler(priority = EventPriority.LOWEST)
  public void wyjscieZGry(PlayerQuitEvent e) {
    Uczestnik x = uczestnik(e.getPlayer().getUniqueId());
    if (x != null) sprzatnij(x, false, null);   // punkt powrotu zostaje: po ponownym wejściu odeślemy go z zagrody
    obserwatorzy.remove(e.getPlayer().getUniqueId());
  }

  @EventHandler public void wejscie(PlayerJoinEvent e) {
    Player p = e.getPlayer();
    if (swiat() != null && p.getWorld().equals(swiat())) Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline() && p.getWorld().equals(swiat())) odeslijObcego(p); }, 10L);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void teleport(PlayerTeleportEvent e) {
    Player p = e.getPlayer();
    Uczestnik x = uczestnik(p.getUniqueId());
    if (x == null || wlasnyTeleport.contains(p.getUniqueId())) return;
    if (e.getTo().getWorld() != null && !e.getTo().getWorld().equals(swiat())) sprzatnij(x, false, null);
  }

  @EventHandler public void zmianaSwiata(PlayerChangedWorldEvent e) {
    Player p = e.getPlayer();
    if (!p.getWorld().equals(swiat())) return;
    if (runda != null) runda.nowyWidz(p);
    if (uczestnik(p.getUniqueId()) != null) {
      // Multiverse nakłada tryb świata (survival) przy wejściu — Przygoda dopiero po nim (24.10: bot w rundzie miał survival)
      Bukkit.getScheduler().runTaskLater(this, () -> {
        if (p.isOnline() && uczestnik(p.getUniqueId()) != null && p.getWorld().equals(swiat())) { p.setGameMode(GameMode.ADVENTURE); p.setAllowFlight(true); }
      }, 2L);
      return;
    }
    Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline() && p.getWorld().equals(swiat())) odeslijObcego(p); }, 5L);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void obrazenia(EntityDamageEvent e) {
    if (e.getEntity().getWorld().equals(swiat()) && (e.getEntity() instanceof Player || e.getEntity().getPersistentDataContainer().has(klucz))) e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void komenda(PlayerCommandPreprocessEvent e) {
    if (uczestnik(e.getPlayer().getUniqueId()) == null) return;
    String k = e.getMessage().split(" ")[0].toLowerCase(Locale.ROOT);
    if (DOZWOLONE_KOMENDY.contains(k)) return;
    e.setCancelled(true);
    e.getPlayer().sendMessage(MM.deserialize(PREFIKS + "<gray>W trakcie gry komendy są wyłączone. Wyjście: <white>Q</white> → Opuść zagrodę.</gray>"));
  }

  /** Sprzęty zagrody to prawdziwe beczki i kotły — nikt w tym świecie ich nie otwiera ani nie przestawia. */
  @EventHandler(priority = EventPriority.LOWEST)
  public void klik(PlayerInteractEvent e) {
    if (uczestnik(e.getPlayer().getUniqueId()) != null || (e.getPlayer().getWorld().equals(swiat()) && e.getPlayer().getGameMode() != GameMode.CREATIVE)) e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void klikEncji(PlayerInteractEntityEvent e) { if (uczestnik(e.getPlayer().getUniqueId()) != null) e.setCancelled(true); }

  @EventHandler(priority = EventPriority.LOWEST)
  public void wyrzuc(PlayerDropItemEvent e) { if (uczestnik(e.getPlayer().getUniqueId()) != null) e.setCancelled(true); }

  @EventHandler public void glod(FoodLevelChangeEvent e) { if (uczestnik(e.getEntity().getUniqueId()) != null) e.setCancelled(true); }

  @EventHandler(ignoreCancelled = true)
  public void niszczenie(BlockBreakEvent e) { if (e.getBlock().getWorld().equals(swiat()) && e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true); }

  @EventHandler(ignoreCancelled = true)
  public void stawianie(BlockPlaceEvent e) { if (e.getBlock().getWorld().equals(swiat()) && e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true); }

  /** Encje rundy nie zapisują się (setPersistent false); po crashu usuwamy, co by jednak wróciło z chunkiem. */
  @EventHandler public void zaladowane(EntitiesLoadEvent e) {
    if (!e.getWorld().equals(swiat()) || runda != null) return;
    for (Entity en : e.getEntities()) if (en.getPersistentDataContainer().has(klucz)) en.remove();
  }

  // ------------------------------------------------------------------ klawisze w grze (z pakietów)
  void klawisz(Player p, String akcja, int encja) {
    Uczestnik x = uczestnik(p.getUniqueId());
    if (x == null) return;
    switch (akcja) {
      case "pauza" -> {
        p.updateInventory();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open licho_pauza " + p.getName());
      }
      case "f" -> { p.updateInventory(); runda.klawiszF(x, p); }
      case "cios" -> runda.cios(x, p, encja);
      case "ppm" -> runda.ppm(x, p, encja);
      default -> {}
    }
  }

  // ------------------------------------------------------------------ HUD / rekordy
  void pasekBoss(Player p, String tekst, float postep) {
    BossBar b = paski.get(p.getUniqueId());
    if (b == null) return;
    // MythicHUD przestawia swoje paski przy każdej zmianie cudzego — wysyłamy tylko faktyczne zmiany.
    float pr = Math.round(Math.max(0f, Math.min(1f, postep)) * 50f) / 50f;
    if (!tekst.equals(ostatniTekst.put(p.getUniqueId(), tekst))) b.name(MM.deserialize(tekst));
    if (Math.abs(b.progress() - pr) > 0.001f) b.progress(pr);
  }

  void pokazRekordy(Player p) {
    if (p == null || !p.isOnline()) return;
    p.sendMessage(MM.deserialize(PREFIKS + "<yellow>" + u.nazwaMapy + "</yellow> <gray>— najlepsze rundy:</gray>"));
    List<Rekordy.Pozycja> top = rekordy.top(5, Comparator.comparingInt(Rekordy.Pozycja::najlepszy).reversed());
    if (top.isEmpty()) p.sendMessage(MM.deserialize("<gray>  Jeszcze nikt nie zagrał. Bądź pierwszy!</gray>"));
    for (int i = 0; i < top.size(); i++) {
      Rekordy.Pozycja r = top.get(i);
      p.sendMessage(MM.deserialize("<gray>  " + (i + 1) + ".</gray> <white>" + MM.escapeTags(r.nick()) + "</white> <yellow>" + r.najlepszy() + " pkt</yellow> <gray>· rund "
          + r.rundy() + ", przetrwanych jako Licho " + r.przetrwane() + "</gray>"));
    }
    List<Rekordy.Pozycja> sprytne = rekordy.top(1, Comparator.comparingInt(Rekordy.Pozycja::przetrwane).reversed());
    if (!sprytne.isEmpty() && sprytne.get(0).przetrwane() > 0)
      p.sendMessage(MM.deserialize("<gray>  Najsprytniejsze Licho:</gray> <white>" + MM.escapeTags(sprytne.get(0).nick()) + "</white> <green>" + sprytne.get(0).przetrwane() + "× przetrwało</green>"));
    rekordyKrotko(p);
  }

  private void rekordyKrotko(Player p) {
    Rekordy.Pozycja moj = rekordy.moj(p.getUniqueId());
    if (moj != null) p.sendMessage(MM.deserialize("<gray>  Twój rekord:</gray> <green>" + moj.najlepszy() + " pkt</green> <gray>· rund " + moj.rundy() + "</gray>"));
  }

  // ------------------------------------------------------------------ komenda (konsola / menu)
  @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
    if (a.length == 0) { s.sendMessage("/licho dolacz <gracz> | wyjdz <gracz> | rekordy [gracz] | zbuduj | stan | start [s] | obserwuj <gracz> | przeladuj"); return true; }
    Player p = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
    switch (a[0].toLowerCase(Locale.ROOT)) {
      case "dolacz" -> {
        if (p == null) { s.sendMessage("Brak gracza"); return true; }
        String problem = dolacz(p);
        if (problem != null) {
          p.sendMessage(MM.deserialize(PREFIKS + "<red>" + problem + "</red>"));
          s.sendMessage("Odmowa: " + problem);
        } else s.sendMessage("OK");
      }
      case "wyjdz" -> {
        if (p == null) { s.sendMessage("Brak gracza"); return true; }
        wyjdz(p, "Opuszczasz zagrodę.");
        s.sendMessage("OK");
      }
      case "rekordy" -> {
        if (p != null) pokazRekordy(p);
        else for (Rekordy.Pozycja r : rekordy.top(10, Comparator.comparingInt(Rekordy.Pozycja::najlepszy).reversed()))
          s.sendMessage(r.nick() + " " + r.najlepszy() + " pkt, rund " + r.rundy() + ", przetrwane " + r.przetrwane());
      }
      case "zbuduj" -> {
        World w = swiat();
        if (w == null) { s.sendMessage("Brak świata " + u.swiat); return true; }
        if (runda != null) { s.sendMessage("Trwa runda."); return true; }
        zaladuj(w, true);
        int[] ch = mapa.chunki();
        for (int cx = ch[0]; cx <= ch[2]; cx++) for (int cz = ch[1]; cz <= ch[3]; cz++) w.getChunkAt(cx, cz).load(true);
        int n = mapa.zbuduj(w);
        w.setSpawnLocation(u.poczekalnia(w));
        zaladuj(w, false);
        s.sendMessage("Zbudowano " + n + " bloków (" + u.nazwaMapy + "), sprzętów-kryjówek " + mapa.sprzety(w, u.przebrania.keySet()).size()
            + ", chunki " + (ch[2] - ch[0] + 1) * (ch[3] - ch[1] + 1));
      }
      case "stan" -> {
        s.sendMessage(runda == null ? "Brak rundy" : "STAN " + runda.opis());
        World w = swiat();
        if (w != null) s.sendMessage("forceload=" + w.getForceLoadedChunks().size() + " encji z kluczem=" + w.getEntities().stream().filter(e -> e.getPersistentDataContainer().has(klucz)).count());
      }
      case "start" -> {
        int sek = a.length > 1 ? Integer.parseInt(a[1]) : 3;
        s.sendMessage(runda != null && runda.szybkiStart(sek * 20) ? "OK start za " + sek + " s" : "Brak poczekalni z " + u.min + " graczami");
      }
      case "obserwuj" -> {
        World w = swiat();
        if (p == null || w == null) { s.sendMessage("Brak gracza/swiata"); return true; }
        if (obserwatorzy.remove(p.getUniqueId())) { p.setFlying(false); p.setAllowFlight(false); odeslij(p); s.sendMessage("Koniec obserwacji"); return true; }
        obserwatorzy.add(p.getUniqueId());
        Location gdzie = p.getLocation();
        powroty.putIfAbsent(p.getUniqueId(), new Powrot(gdzie.getWorld().getName(), gdzie.getX(), gdzie.getY(), gdzie.getZ(), gdzie.getYaw(), gdzie.getPitch(), profil(p)));
        Location cel = a.length > 4 ? new Location(w, Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4]),
            a.length > 5 ? Float.parseFloat(a[5]) : 0f, a.length > 6 ? Float.parseFloat(a[6]) : 0f) : u.brama(w).add(0, 6, 4);
        teleport(p, cel);
        if (runda != null) runda.nowyWidz(p);
        Bukkit.getScheduler().runTaskLater(this, () -> { p.setAllowFlight(true); p.setFlying(true); }, 5L);
        s.sendMessage("Obserwacja: " + p.getName());
      }
      case "przeladuj" -> {
        if (runda != null) { s.sendMessage("Najpierw niech runda się skończy."); return true; }
        reloadConfig();
        u = new Ustawienia(getConfig());
        s.sendMessage("Przeładowano: przebrań " + u.przebrania.size());
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
