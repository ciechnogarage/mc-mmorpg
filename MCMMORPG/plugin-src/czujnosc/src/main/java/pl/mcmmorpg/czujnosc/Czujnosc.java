package pl.mcmmorpg.czujnosc;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.mcmmorpg.czujnosc.Percepcja.Postawa;
import pl.mcmmorpg.czujnosc.Stan.Poziom;

/** Czujnosc mobow: wzrok (stozek, swiatlo, postawa), sluch (halas ruchu i zdarzen), stany SPOKOJNY/ZANIEPOKOJONY/SZUKA/ALARM.
 *  Koszt: jedno zadanie co tik; gracze w 10 kubelkach (kazdy co 0,5 s) szukaja mobow tylko wokol siebie; para gracz-mob:
 *  dystans -> stozek -> raytrace (tylko gdy w stozku). Halas = zdarzenia z limitem na gracza, bez odpytywania.
 *  Mob wchodzi do systemu tylko z configu (typ MM / frakcja / typ vanilli). Stan widac w PDC `czujnosc:stan`
 *  (0..3 = Poziom.ordinal, brak klucza = spokojny) - czyta go AnimacjeBroni (cios z ukrycia) bez zaleznosci. */
public final class Czujnosc extends JavaPlugin implements Listener {
  static final double DT = 0.5;
  static final double PATRZY = 15;   // od tylu pkt spokojny mob wodzi wzrokiem za bodzcem                        // cykl gracza i moba [s] = 10 tikow

  NamespacedKey kluczStan;
  final Map<UUID, Stan> stany = new HashMap<>();
  final Set<UUID> obce = new HashSet<>();   // moby sprawdzone i bez profilu: bez ponownego pytania MythicMobs co cykl / co EntityTargetEvent
  final Map<UUID, double[]> pozycje = new HashMap<>();
  final Map<UUID, Long> halasOstatni = new HashMap<>();
  final Map<String, Profil> profile = new HashMap<>();
  final Map<String, Profil> poTypieMM = new HashMap<>(), poFrakcji = new HashMap<>();
  final Map<String, Double> ikonaWysokoscTypu = new HashMap<>();   // modele ME sa wyzsze niz hitbox, do ktorego przypieta jest ikona
  final Map<EntityType, Profil> poTypieVanilli = new HashMap<>();
  final Map<String, Double[]> tagi = new HashMap<>();  // tag gracza -> {mnoznik widocznosci, mnoznik glosnosci}
  final Map<String, Double> zbroja = new HashMap<>();   // przedrostek materialu -> ciezar
  Set<Material> swiatloWRece = Set.of();
  double niewidzialnosc, zasiegMax, halasCios, halasPocisk, halasBlok, ikonaWysokosc, ikonaSkala;
  int halasLimitTikow;
  boolean ikony, ikonyPrzedmiot, debug, mythic;
  float[] cmd = new float[4];
  String[] dzwieki = new String[4];
  long czasNs, tiki, pary, promienie;               // pomiar wlasnego kosztu (/czujnosc stan)
  long nsGracze, nsMoby;                            // w tym: zmysly (gracz -> moby obok) i stany/zachowanie mobow

  @Override public void onEnable() {
    kluczStan = new NamespacedKey(this, "stan");
    saveDefaultConfig();
    mythic = getServer().getPluginManager().isPluginEnabled("MythicMobs");
    wczytaj();
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getScheduler().runTaskTimer(this, this::tik, 1, 1);
  }

  @Override public void onDisable() {
    for (Stan s : stany.values()) if (s.ikona != null) s.ikona.remove();
    for (World w : getServer().getWorlds()) for (Entity e : w.getEntities()) e.getPersistentDataContainer().remove(kluczStan);
    stany.clear(); obce.clear();
  }

  void wczytaj() {
    reloadConfig();
    var c = getConfig();
    profile.clear(); poTypieMM.clear(); poFrakcji.clear(); poTypieVanilli.clear(); tagi.clear(); zbroja.clear();
    Profil dom = Profil.z("domyslny", c.getConfigurationSection("profile.domyslny"), null);
    profile.put("domyslny", dom);
    ConfigurationSection ps = c.getConfigurationSection("profile");
    if (ps != null) for (String k : ps.getKeys(false)) if (!k.equals("domyslny")) profile.put(k, Profil.z(k, ps.getConfigurationSection(k), dom));
    przypisz(c.getConfigurationSection("moby"), (k, p) -> poTypieMM.put(k, p));
    przypisz(c.getConfigurationSection("frakcje"), (k, p) -> poFrakcji.put(k, p));
    przypisz(c.getConfigurationSection("vanilla"), (k, p) -> {
      try { poTypieVanilli.put(EntityType.valueOf(k.toUpperCase()), p); } catch (IllegalArgumentException e) { getLogger().warning("vanilla: nieznany typ " + k); }
    });
    zasiegMax = profile.values().stream().mapToDouble(Profil::zasiegMax).max().orElse(20);
    niewidzialnosc = c.getDouble("gracz.niewidzialnosc", 0.15);
    ConfigurationSection ts = c.getConfigurationSection("gracz.tagi");
    if (ts != null) for (String t : ts.getKeys(false))
      tagi.put(t, new Double[] { ts.getDouble(t + ".widocznosc", 1), ts.getDouble(t + ".glosnosc", 1) });
    ConfigurationSection zs = c.getConfigurationSection("gracz.zbroja");
    if (zs != null) for (String m : zs.getKeys(false)) zbroja.put(m.toUpperCase(), zs.getDouble(m));
    var sw = new java.util.HashSet<Material>();
    for (String m : c.getStringList("gracz.swiatlo_w_rece")) { Material mat = Material.matchMaterial(m); if (mat != null) sw.add(mat); }
    swiatloWRece = sw;
    halasCios = c.getDouble("halas.cios", 10);
    halasPocisk = c.getDouble("halas.pocisk", 8);
    halasBlok = c.getDouble("halas.blok", 6);
    halasLimitTikow = c.getInt("halas.limit_tikow", 10);
    ikony = c.getBoolean("ikony.wlaczone", true);
    ikonyPrzedmiot = c.getString("ikony.typ", "tekst").equalsIgnoreCase("przedmiot");
    ikonaWysokosc = c.getDouble("ikony.wysokosc", 0.6);
    ikonaSkala = c.getDouble("ikony.skala", 2.5);
    ikonaWysokoscTypu.clear();
    var wt = c.getConfigurationSection("ikony.wysokosc_typ");
    if (wt != null) for (String k : wt.getKeys(false)) ikonaWysokoscTypu.put(k, wt.getDouble(k));
    for (Poziom p : Poziom.values()) {
      cmd[p.ordinal()] = (float) c.getDouble("ikony.cmd." + p.name(), 0);
      dzwieki[p.ordinal()] = c.getString("dzwieki." + p.name(), "");
    }
    debug = c.getBoolean("debug", false);
    for (Stan s : stany.values()) if (s.ikona != null) s.ikona.remove();
    stany.clear(); obce.clear();
    getLogger().info("profile=" + profile.keySet() + " mobyMM=" + poTypieMM.keySet() + " frakcje=" + poFrakcji.keySet()
        + " vanilla=" + poTypieVanilli.keySet() + " zasiegMax=" + zasiegMax);
  }

  private void przypisz(ConfigurationSection s, java.util.function.BiConsumer<String, Profil> cel) {
    if (s == null) return;
    for (String k : s.getKeys(false)) {
      Profil p = profile.get(s.getString(k));
      if (p == null) getLogger().warning("brak profilu '" + s.getString(k) + "' dla " + k);
      else cel.accept(k, p);
    }
  }

  // ---------------------------------------------------------------- rejestr mobow

  /** Stan moba, tworzony leniwie; null = mob nie jest czujny (nie ma go w configu). */
  Stan stan(Entity e) {
    if (!(e instanceof Mob)) return null;
    Stan s = stany.get(e.getUniqueId());
    if (s != null) return s;
    if (obce.contains(e.getUniqueId())) return null;
    Profil p = profil(e);
    if (p == null) {
      if (e.getTicksLived() > 40) obce.add(e.getUniqueId());   // MM rejestruje ActiveMob tuz po spawnie: swiezego nie skreslamy
      return null;
    }
    s = new Stan(p);
    stany.put(e.getUniqueId(), s);
    e.getPersistentDataContainer().set(kluczStan, PersistentDataType.BYTE, (byte) 0);   // 0 = spokojny, ale pod Czujnoscia
    return s;
  }

  private Profil profil(Entity e) {
    if (mythic) {
      Profil p = MM.profil(this, e);
      if (p != null) return p;
    }
    return poTypieVanilli.get(e.getType());
  }

  @EventHandler public void usuniety(EntityRemoveFromWorldEvent e) {
    obce.remove(e.getEntity().getUniqueId());
    Stan s = stany.remove(e.getEntity().getUniqueId());
    if (s == null) return;
    if (s.ikona != null) s.ikona.remove();
    e.getEntity().getPersistentDataContainer().remove(kluczStan);
  }

  @EventHandler public void wyszedl(PlayerQuitEvent e) {
    pozycje.remove(e.getPlayer().getUniqueId());
    halasOstatni.remove(e.getPlayer().getUniqueId());
  }

  // ---------------------------------------------------------------- cykl

  void tik() {
    long t0 = System.nanoTime();
    int teraz = getServer().getCurrentTick(), kubel = Math.floorMod(teraz, 10);
    try {
      for (Player p : getServer().getOnlinePlayers())
        if (Math.floorMod(p.getEntityId(), 10) == kubel) gracz(p);
      long t1 = System.nanoTime(); nsGracze += t1 - t0;
      List<UUID> martwe = null;
      for (Map.Entry<UUID, Stan> en : stany.entrySet()) {
        Entity e = getServer().getEntity(en.getKey());
        if (e == null || !e.isValid()) { (martwe == null ? martwe = new ArrayList<>() : martwe).add(en.getKey()); continue; }
        Stan s = en.getValue();
        if (Math.floorMod(e.getEntityId(), 10) == kubel) mob((Mob) e, s, teraz);
        if (s.patrzy && s.ostatnieMiejsce != null && s.ostatnieMiejsce.getWorld() == e.getWorld()) ((Mob) e).lookAt(s.ostatnieMiejsce);
      }
      if (martwe != null) for (UUID u : martwe) { Stan s = stany.remove(u); if (s.ikona != null) s.ikona.remove(); }
      nsMoby += System.nanoTime() - t1;
    } catch (RuntimeException ex) {
      getLogger().log(Level.WARNING, "tik", ex);
    }
    czasNs += System.nanoTime() - t0; tiki++;
  }

  private void gracz(Player p) {
    Location lp = p.getLocation();
    double[] stara = pozycje.get(p.getUniqueId());
    pozycje.put(p.getUniqueId(), new double[] { lp.getX(), lp.getY(), lp.getZ() });
    GameMode gm = p.getGameMode();
    if (p.isDead() || gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;
    double skok = stara == null ? 0 : sq(lp.getX() - stara[0]) + sq(lp.getZ() - stara[2]);
    boolean ruch = skok > 0.04 && skok < 16;   // > 4 bl. na 0,5 s = teleport (skill, CMI), nie krok - bez halasu
    Postawa postawa = p.isSneaking() ? Postawa.KUCA : !ruch ? Postawa.STOI : p.isSprinting() ? Postawa.BIEGNIE : Postawa.CHODZI;
    double widoczny = (p.isInvisible() || p.hasPotionEffect(PotionEffectType.INVISIBILITY)) ? niewidzialnosc : 1, glosny = 1;
    for (String t : p.getScoreboardTags()) { Double[] m = tagi.get(t); if (m != null) { widoczny *= m[0]; glosny *= m[1]; } }
    double glosnosc = Percepcja.glosnosc(postawa, ciezar(p), glosny);
    int swiatlo = trzymaSwiatlo(p) ? 15 : p.getEyeLocation().getBlock().getLightLevel();
    Location oko = p.getEyeLocation(), srodek = lp.clone().add(0, p.getHeight() * 0.5, 0);
    for (Mob mob : p.getWorld().getNearbyEntitiesByType(Mob.class, lp, zasiegMax, zasiegMax * 0.5, zasiegMax)) {
      Stan s = stan(mob);
      if (s == null) continue;
      pary++;
      Profil pr = s.profil;
      Location mo = mob.getEyeLocation();
      double dx = srodek.getX() - mo.getX(), dy = srodek.getY() - mo.getY(), dz = srodek.getZ() - mo.getZ();
      double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
      Vector w = mo.getDirection();
      double kat = Percepcja.kat(w.getX(), w.getY(), w.getZ(), dx, dy, dz);
      if (d <= pr.dotyk() && (kat <= pr.wzrokKat() / 2 || postawa != Postawa.KUCA)) {   // wpadl na moba (od tylu tylko kucajac sie uda)
        s.dodaj(p.getUniqueId(), 1000, true, lp);
        continue;
      }
      double g = Percepcja.wzrok(pr, d, kat, postawa, swiatlo, widoczny);
      boolean los = g > 0 && widzi(mo, oko, srodek);
      if (los) s.dodaj(p.getUniqueId(), g * DT, true, lp);
      double h = Percepcja.sluch(pr, d, glosnosc);
      if (h > 0) s.dodaj(p.getUniqueId(), h * DT, false, lp);
      if (debug) s.slad = String.format(Locale.ROOT, "kat=%.0f d=%.1f sw=%d g=%.1f%s h=%.1f", kat, d, swiatlo, g, g > 0 && !los ? "(zaslonieta)" : "", h);
    }
  }

  /** Linia wzroku oko moba -> oko gracza, a gdy zaslonieta: -> srodek ciala (zza murku widac glowe, zza krzaka tulow). */
  private boolean widzi(Location z, Location oko, Location srodek) {
    return promien(z, oko) || promien(z, srodek);
  }

  private boolean promien(Location z, Location cel) {
    promienie++;
    Vector kier = cel.toVector().subtract(z.toVector());
    double dl = kier.length();
    if (dl < 1e-3) return true;
    RayTraceResult r = z.getWorld().rayTraceBlocks(z, kier.multiply(1 / dl), dl, FluidCollisionMode.NEVER, true);
    return r == null || r.getHitBlock() == null;
  }

  private double ciezar(Player p) {
    if (zbroja.isEmpty()) return 0;
    double c = 0;
    EntityEquipment eq = p.getEquipment();
    for (ItemStack it : eq.getArmorContents()) {
      if (it == null || it.getType().isAir()) continue;
      String n = it.getType().name();
      for (Map.Entry<String, Double> z : zbroja.entrySet()) if (n.startsWith(z.getKey())) { c += z.getValue(); break; }
    }
    return c;
  }

  private boolean trzymaSwiatlo(Player p) {
    EntityEquipment eq = p.getEquipment();
    return swiatloWRece.contains(eq.getItemInMainHand().getType()) || swiatloWRece.contains(eq.getItemInOffHand().getType());
  }

  private void mob(Mob mob, Stan s, int teraz) {
    Poziom stary = s.poziom;
    if (stary == Poziom.ALARM) {
      if (s.zyskWzrok > 0) s.ostatnioWidzianyTick = teraz;            // w alarmie liczy sie tylko, czy go WIDZI
      else if (teraz - s.ostatnioWidzianyTick > s.profil.pamiecS() * 20 || !celZywy(mob, s)) s.wartosc = 70;
    } else if (s.zyskWzrok > 0) {
      s.ostatniBodziecTick = teraz;
      s.wartosc = Math.min(100, s.wartosc + s.zysk);
    } else if (s.zysk > 0) {
      s.ostatniBodziecTick = teraz;
      if (s.wartosc < s.profil.sluchLimit()) s.wartosc = Math.min(s.profil.sluchLimit(), s.wartosc + s.zysk);   // sam halas nie da alarmu
    } else if (teraz - s.ostatniBodziecTick > s.profil.zanikZwloka() * 20) {
      s.wartosc = Math.max(0, s.wartosc - s.profil.zanik() * DT * (stary == Poziom.SZUKA ? 0.25 : 1));
    }
    if (debug && (s.zysk > 0 || s.wartosc > 0)) getLogger().info(String.format(Locale.ROOT, "CZUJ slad %s v=%.1f +%.1f %s", nazwa(mob), s.wartosc, s.zysk, s.slad));
    s.zysk = s.zyskWzrok = s.zrodloZysk = 0; s.slad = null;
    Poziom nowy = Poziom.z(s.wartosc, stary);
    if (nowy != stary) zmiana(mob, s, stary, nowy);
    zachowanie(mob, s, teraz);
  }

  private boolean celZywy(Mob mob, Stan s) {
    Player p = s.zrodlo == null ? null : getServer().getPlayer(s.zrodlo);
    return p != null && !p.isDead() && p.getWorld() == mob.getWorld() && p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR;
  }

  private void zachowanie(Mob mob, Stan s, int teraz) {
    Location cel = s.ostatnieMiejsce;
    // Wzrok trzyma sie bodzca co tik (lookAt dziala 2 tiki, potem randomlookaround zabiera glowe): zaniepokojony
    // albo spokojny, ktory juz cos dostrzegl (>= PATRZY) i bodziec jest swiezy. Gracz widzi: odwrocil glowe -> zamarznij/schowaj sie.
    s.patrzy = s.poziom == Poziom.ZANIEPOKOJONY
        || (s.poziom == Poziom.SPOKOJNY && s.wartosc >= PATRZY && teraz - s.ostatniBodziecTick <= s.profil.zanikZwloka() * 20);
    switch (s.poziom) {
      case SZUKA -> {
        if (cel == null || cel.getWorld() != mob.getWorld()) return;
        if (mob.getLocation().distanceSquared(cel) > 4) {
          if (!mob.getPathfinder().hasPath()) mob.getPathfinder().moveTo(cel, s.profil.predkoscSzukania());
        } else mob.lookAt(cel.clone().add(Math.random() * 8 - 4, 0, Math.random() * 8 - 4));   // na miejscu rozglada sie
      }
      case ALARM -> {
        Player p = s.zrodlo == null ? null : getServer().getPlayer(s.zrodlo);
        if (p != null && celZywy(mob, s) && (mob.getTarget() == null || !mob.getTarget().isValid())) mob.setTarget(p);
      }
      default -> {}
    }
  }

  private void zmiana(Mob mob, Stan s, Poziom stary, Poziom nowy) {
    s.poziom = nowy;
    mob.getPersistentDataContainer().set(kluczStan, PersistentDataType.BYTE, (byte) nowy.ordinal());
    Player zr = s.zrodlo == null ? null : getServer().getPlayer(s.zrodlo);
    if (nowy == Poziom.ALARM) {
      s.ostatnioWidzianyTick = getServer().getCurrentTick();
      if (zr != null) mob.setTarget(zr);
      wolaj(mob, s, zr);
    } else if (stary == Poziom.ALARM) {
      mob.setTarget(null);
      if (zr != null && zr.getWorld() == mob.getWorld()) s.ostatnieMiejsce = zr.getLocation();   // idzie tam, gdzie go stracil
    }
    ikona(mob, s);
    String dz = dzwieki[nowy.ordinal()];
    if (nowy.ordinal() > stary.ordinal() && dz != null && !dz.isEmpty()) mob.getWorld().playSound(mob.getLocation(), dz, 1f, 1f);
    if (mythic) MM.sygnal(mob, zr, "CZUJ_" + nowy.name());
    if (debug) getLogger().info("CZUJ " + nazwa(mob) + " " + stary + "->" + nowy + " v=" + Math.round(s.wartosc) + " od=" + (zr == null ? "-" : zr.getName()));
  }

  /** Alarm wola sojusznikow tej samej frakcji (albo profilu) w promieniu: ida sprawdzic (SZUKA), nie dostaja alarmu za darmo. */
  private void wolaj(Mob mob, Stan s, Player zr) {
    double r = s.profil.wolanie();
    if (r <= 0) return;
    String grupa = mythic ? MM.frakcja(mob) : null;
    Location gdzie = zr != null ? zr.getLocation() : mob.getLocation();
    for (Mob inny : mob.getWorld().getNearbyEntitiesByType(Mob.class, mob.getLocation(), r)) {
      if (inny == mob) continue;
      Stan o = stan(inny);
      if (o == null || o.poziom.ordinal() >= Poziom.SZUKA.ordinal()) continue;
      boolean swoj = grupa != null ? grupa.equals(MM.frakcja(inny)) : o.profil == s.profil;
      if (!swoj) continue;
      o.wartosc = Math.max(o.wartosc, 65);
      o.zrodlo = s.zrodlo;
      o.ostatnieMiejsce = gdzie;
      zmiana(inny, o, o.poziom, Poziom.z(o.wartosc, o.poziom));
    }
  }

  // ---------------------------------------------------------------- ikony

  private void ikona(Mob mob, Stan s) {
    if (!ikony) return;
    if (s.poziom == Poziom.SPOKOJNY) {
      if (s.ikona != null) { s.ikona.remove(); s.ikona = null; }
      return;
    }
    if (s.ikona == null || !s.ikona.isValid()) {
      Location l = mob.getLocation().add(0, mob.getHeight(), 0);
      Display d = ikonyPrzedmiot
          ? mob.getWorld().spawn(l, ItemDisplay.class, x -> przygotuj(x, mob))
          : mob.getWorld().spawn(l, TextDisplay.class, x -> { przygotuj(x, mob); x.setDefaultBackground(false); x.setBackgroundColor(org.bukkit.Color.fromARGB(0)); x.setShadowed(true); });
      mob.addPassenger(d);
      s.ikona = d;
    }
    if (s.ikona instanceof TextDisplay td) {
      td.text(switch (s.poziom) {
        case ZANIEPOKOJONY -> Component.text("?", NamedTextColor.YELLOW);
        case SZUKA -> Component.text("?", NamedTextColor.GOLD, TextDecoration.BOLD);
        default -> Component.text("!", NamedTextColor.RED, TextDecoration.BOLD);
      });
    } else if (s.ikona instanceof ItemDisplay id) {
      ItemStack it = new ItemStack(Material.INK_SAC);
      ItemMeta m = it.getItemMeta();
      var cmdc = m.getCustomModelDataComponent();
      cmdc.setFloats(List.of(cmd[s.poziom.ordinal()]));
      m.setCustomModelDataComponent(cmdc);
      it.setItemMeta(m);
      id.setItemStack(it);
    }
  }

  private void przygotuj(Display d, Mob mob) {
    String typ = MM.typ(mob);
    double h = typ == null ? ikonaWysokosc : ikonaWysokoscTypu.getOrDefault(typ, ikonaWysokosc);
    d.setPersistent(false);
    d.setBillboard(Display.Billboard.CENTER);
    d.setBrightness(new Display.Brightness(15, 15));
    d.setViewRange(0.5f);   // x64 bl. = 32 bl.
    d.setTransformation(new Transformation(new Vector3f(0, (float) h, 0), new Quaternionf(), new Vector3f((float) ikonaSkala), new Quaternionf()));
  }

  // ---------------------------------------------------------------- zdarzenia

  /** Czujny mob nie celuje w gracza, dopoki go nie zauwazy (ALARM). */
  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void cel(EntityTargetLivingEntityEvent e) {
    if (!(e.getTarget() instanceof Player)) return;
    Stan s = stan(e.getEntity());
    if (s != null && s.poziom != Poziom.ALARM) e.setCancelled(true);
  }

  /** Cios w moba = alarm od razu (MONITOR: cios z ukrycia AnimacjeBroni liczy sie jeszcze na nieswiadomym). Cios = halas. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void cios(EntityDamageByEntityEvent e) {
    if (debug && stan(e.getEntity()) != null) getLogger().info("CZUJ cios " + e.getDamager().getType() + " -> " + e.getEntity().getName() + " dmg=" + e.getFinalDamage() + " anulowany=" + e.isCancelled());
    if (e.isCancelled()) return;
    Player p = e.getDamager() instanceof Player pl ? pl
        : e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player pl ? pl : null;
    if (p == null) return;
    Stan s = stan(e.getEntity());
    if (s != null && s.poziom != Poziom.ALARM) {
      s.wartosc = 100; s.zrodlo = p.getUniqueId(); s.ostatnieMiejsce = p.getLocation();
      zmiana((Mob) e.getEntity(), s, s.poziom, Poziom.ALARM);
    }
    halas(p, p.getLocation(), halasCios, true);
  }

  /** Pocisk, ktory uderzyl w blok = halas w miejscu uderzenia (odwrocenie uwagi: mob idzie tam, nie do gracza). */
  @EventHandler(priority = EventPriority.MONITOR)
  public void pocisk(ProjectileHitEvent e) {
    if (debug) getLogger().info("CZUJ pocisk " + e.getEntity().getType() + " anulowany=" + e.isCancelled() + " blok=" + e.getHitBlock() + " encja=" + e.getHitEntity() + " strzelec=" + e.getEntity().getShooter());
    if (e.isCancelled()) return;
    if (e.getHitBlock() == null || !(e.getEntity().getShooter() instanceof Player p)) return;
    halas(p, e.getEntity().getLocation(), halasPocisk, false);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void blok(BlockBreakEvent e) {
    halas(e.getPlayer(), e.getBlock().getLocation().add(0.5, 0.5, 0.5), halasBlok, true);
  }

  /** Halas w punkcie: moby w promieniu (bez alarmu) dostaja przyrost jak ze sluchu; zrodlem jest MIEJSCE.
   *  `zdradzaGracza` = mob idzie do gracza (cios, kopanie), inaczej do punktu (rzucony kamien). Limit: raz na halas.limit_tikow. */
  void halas(Player p, Location gdzie, double r, boolean zdradzaGracza) {
    if (r <= 0 || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
    long teraz = getServer().getCurrentTick();
    Long ost = halasOstatni.get(p.getUniqueId());
    if (ost != null && teraz - ost < halasLimitTikow) return;
    halasOstatni.put(p.getUniqueId(), teraz);
    for (Mob mob : gdzie.getWorld().getNearbyEntitiesByType(Mob.class, gdzie, r)) {
      Stan s = stan(mob);
      if (s == null || s.poziom == Poziom.ALARM) continue;
      double d = mob.getLocation().distance(gdzie);
      double ile = s.profil.sluchLimit() * Math.min(1, 4 * (1 - d / r));   // do 3/4 promienia pelna sila (idzie sprawdzic), dalej szybko slabnie
      s.dodaj(p.getUniqueId(), ile, false, zdradzaGracza ? p.getLocation() : gdzie.clone());
      if (debug) getLogger().info("CZUJ halas " + nazwa(mob) + " d=" + Math.round(d) + " +" + Math.round(ile) + (zdradzaGracza ? " (gracz)" : " (punkt)"));
    }
  }

  // ---------------------------------------------------------------- komenda

  @Override public boolean onCommand(CommandSender cs, Command c, String l, String[] a) {
    String sub = a.length == 0 ? "stan" : a[0];
    switch (sub) {
      case "przeladuj" -> { wczytaj(); cs.sendMessage("Czujnosc: przeladowano."); }
      case "debug" -> { debug = !debug; cs.sendMessage("Czujnosc debug=" + debug); }
      case "info" -> {
        if (!(cs instanceof Player p)) { cs.sendMessage("tylko gracz"); return true; }
        Entity e = p.getTargetEntity(32);
        Stan s = e == null ? null : stan(e);
        cs.sendMessage(s == null ? "brak czujnego moba pod celownikiem"
            : nazwa(e) + " profil=" + s.profil.nazwa() + " poziom=" + s.poziom + " v=" + Math.round(s.wartosc));
      }
      default -> {
        double us = tiki == 0 ? 0 : czasNs / 1000.0 / tiki, usG = tiki == 0 ? 0 : nsGracze / 1000.0 / tiki, usM = tiki == 0 ? 0 : nsMoby / 1000.0 / tiki;
        long[] tt = getServer().getTickTimes().clone();   // ostatnie 100 tikow serwera [ns] - MSPT do pomiaru kosztu (spark tps przez RCON przychodzi z opoznieniem)
        java.util.Arrays.sort(tt);
        double sr = java.util.Arrays.stream(tt).average().orElse(0) / 1e6;
        cs.sendMessage(String.format(Locale.ROOT, "Czujnosc: moby=%d obce=%d sr=%.1f us/tik (zmysly %.1f, moby %.1f) pary=%d promienie=%d tiki=%d | serwer mspt sr=%.2f med=%.2f p95=%.2f max=%.2f tps=%.2f",
            stany.size(), obce.size(), us, usG, usM, pary, promienie, tiki, sr, tt[tt.length / 2] / 1e6, tt[tt.length * 95 / 100] / 1e6, tt[tt.length - 1] / 1e6, getServer().getTPS()[0]));
        if (a.length > 1 && a[1].equals("zeruj")) { czasNs = tiki = pary = promienie = nsGracze = nsMoby = 0; }
      }
    }
    return true;
  }

  private String nazwa(Entity e) {
    String mm = mythic ? MM.typ(e) : null;
    return (mm != null ? mm : e.getType().name()) + "#" + e.getEntityId();
  }

  private static double sq(double x) { return x * x; }

  /** Wszystko z MythicMobs w osobnej klasie: bez MM wtyczka dziala na typach vanilli (klasa sie nie laduje). */
  static final class MM {
    static io.lumine.mythic.core.mobs.ActiveMob am(Entity e) {
      return io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().getActiveMob(e.getUniqueId()).orElse(null);
    }
    static Profil profil(Czujnosc c, Entity e) {
      var am = am(e);
      if (am == null) return null;
      Profil p = c.poTypieMM.get(am.getType().getInternalName());
      if (p == null && am.hasFaction()) p = c.poFrakcji.get(am.getFaction());
      return p;
    }
    static String frakcja(Entity e) { var am = am(e); return am != null && am.hasFaction() ? am.getFaction() : null; }
    static String typ(Entity e) { var am = am(e); return am == null ? null : am.getType().getInternalName(); }
    static void sygnal(Entity e, Player zrodlo, String s) {
      var am = am(e);
      if (am == null) return;
      try {
        am.signalMob(zrodlo == null ? null : io.lumine.mythic.bukkit.BukkitAdapter.adapt(zrodlo), s);
      } catch (RuntimeException ex) {
        org.bukkit.Bukkit.getLogger().log(Level.FINE, "sygnal " + s, ex);
      }
    }
  }
}
