package pl.mcmmorpg.paskimobow;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PaskiMobow extends JavaPlugin implements Listener {
  private final Map<UUID, Pasek> paski = new HashMap<>();
  private final Map<UUID, Integer> ostatnieTrafienia = new HashMap<>();
  private final Map<Integer, Pasek> poMobie = new ConcurrentHashMap<>();
  private final Pakiety pakiety = new Pakiety(poMobie);
  private Ustawienia u;
  private boolean mythic, modelEngine;
  private Map<UUID, ?> krwawiacy;   // AnimacjeBroni trzyma krwawienie w prywatnej mapie - czytamy tylko klucze

  @Override public void onEnable() {
    saveDefaultConfig();
    u = Ustawienia.wczytaj(getConfig(), getLogger());
    pakiety.ukryjNazwe = u.ukryjNazwe();
    mythic = getServer().getPluginManager().isPluginEnabled("MythicMobs");
    modelEngine = getServer().getPluginManager().isPluginEnabled("ModelEngine");
    podepnijKrwawienie();
    pakiety.wlacz();
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getScheduler().runTaskTimer(this, this::odswiez, 2L, 2L);
    getServer().getScheduler().runTaskTimer(this, this::przeglad, 20L, 20L);
    getLogger().info("PaskiMobow: MythicMobs=" + mythic + ", ModelEngine=" + modelEngine
        + ", krwawienie AnimacjeBroni=" + (krwawiacy != null) + ", wpisow mobow=" + u.moby().size());
  }

  @Override public void onDisable() {
    pakiety.wylacz();
    for (Pasek p : new ArrayList<>(paski.values())) usun(p, true);
  }

  @SuppressWarnings("unchecked")
  private void podepnijKrwawienie() {
    krwawiacy = null;
    Plugin ab = getServer().getPluginManager().getPlugin("AnimacjeBroni");
    if (ab == null || !ab.isEnabled()) return;
    try {
      Field f = ab.getClass().getDeclaredField("krwawiacy");
      f.setAccessible(true);
      krwawiacy = (Map<UUID, ?>) f.get(ab);
    } catch (ReflectiveOperationException | ClassCastException ex) {
      getLogger().warning("AnimacjeBroni bez pola krwawiacy - ikona krwawienia wylaczona (" + ex + ")");
    }
  }

  // ---------------------------------------------------------------- kto ma pasek

  /** Wypelnia nazwe/rzadkosc/poziom; false = ten mob nie dostaje paska. */
  private boolean opisz(Pasek p, LivingEntity e) {
    if (e instanceof Player || e instanceof ArmorStand || !e.isValid() || e.isDead()) return false;
    if (u.pominSwiaty().contains(e.getWorld().getName())) return false;   // np. sala kreatora postaci
    if (modelEngine && !u.modelEngineWlaczone() && ModeleME.jestModelowany(e.getUniqueId())) return false;
    ActiveMob am = mythic ? MythicBukkit.inst().getMobManager().getActiveMob(e.getUniqueId()).orElse(null) : null;
    if (am != null) {
      String id = am.getType().getInternalName();
      if (Ustawienia.pasuje(u.pomin(), id)) return false;
      boolean frakcja = am.hasFaction() && u.frakcje().contains(am.getFaction());
      if (!frakcja && !Ustawienia.pasuje(u.wzorce(), id)) return false;
      Ustawienia.Wpis w = null;
      for (Ustawienia.Wpis x : u.moby()) if (x.wzorzec().matcher(id).matches()) { w = x; break; }
      String rz = w != null && w.rzadkosc() != null ? w.rzadkosc() : automat(am);
      p.rzadkosc = u.rzadkosc(rz);
      p.poziom = (w != null && w.poziom() != null ? w.poziom() : 1)
          + (u.poziomZMm() ? Math.max(0, (int) am.getLevel() - 1) : 0);
      p.nazwa = e.customName() != null ? PlainTextComponentSerializer.plainText().serialize(e.customName())
          : bezKodow(am.getDisplayName());
      p.zMythicMobs = true;
      return true;
    }
    if (!u.vanillaPotwory() || !(e instanceof Enemy)) return false;
    // nieśmiertelny albo bez AI = dekoracja (manekin podglądu kreatora to przebrany husk), nie przeciwnik
    if (e.isInvulnerable() || (e instanceof Mob m && !m.hasAI())) return false;
    p.rzadkosc = u.rzadkosc(null);
    p.poziom = u.vanillaPoziom();
    p.nazwa = e.customName() != null ? PlainTextComponentSerializer.plainText().serialize(e.customName()) : null;
    p.zMythicMobs = false;
    return true;
  }

  private String automat(ActiveMob am) {
    if (am.getType().usesBossBar()) return u.rzadkosci().containsKey("legendarny") ? "legendarny" : null;
    String display = am.getType().getConfig().getString("Display", "");
    if (display.contains("&l") || display.contains("<bold>") || display.contains("<b>"))
      return u.rzadkosci().containsKey("rzadki") ? "rzadki" : null;
    return null;
  }

  private static String bezKodow(String s) {
    return s == null ? "" : s.replaceAll("(?i)[&§][0-9a-fk-orx]|<[^>]+>", "");
  }

  // ---------------------------------------------------------------- widzowie

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void sledzi(PlayerTrackEntityEvent e) {
    if (!(e.getEntity() instanceof LivingEntity mob) || mob instanceof Player) return;
    Player gracz = e.getPlayer();
    // pakiety spawnu moba ida po zdarzeniu - pasazera dosylamy tick pozniej
    getServer().getScheduler().runTask(this, () -> {
      if (gracz.isOnline() && mob.isValid() && mob.getTrackedBy().contains(gracz)) dodajWidza(gracz, mob);
    });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void przestajeSledzic(PlayerUntrackEntityEvent e) {
    Pasek p = paski.get(e.getEntity().getUniqueId());
    if (p != null && p.widzowie.remove(e.getPlayer().getUniqueId())) pakiety.ukryj(e.getPlayer(), p);
  }

  @EventHandler public void wyjscie(PlayerQuitEvent e) {
    for (Pasek p : paski.values()) {
      p.widzowie.remove(e.getPlayer().getUniqueId());
      p.skale.remove(e.getPlayer().getUniqueId());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void smierc(EntityDeathEvent e) {
    ostatnieTrafienia.remove(e.getEntity().getUniqueId());
    Pasek p = paski.get(e.getEntity().getUniqueId());
    if (p != null) usun(p, false);
  }

  @EventHandler public void znika(EntityRemoveFromWorldEvent e) {
    ostatnieTrafienia.remove(e.getEntity().getUniqueId());
    Pasek p = paski.get(e.getEntity().getUniqueId());
    if (p != null) usun(p, false);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void obrazenia(EntityDamageEvent e) {
    if (!(e.getEntity() instanceof LivingEntity mob) || mob instanceof Player) return;
    int teraz = getServer().getCurrentTick();
    ostatnieTrafienia.put(mob.getUniqueId(), teraz);
    Pasek p = paski.get(e.getEntity().getUniqueId());
    if (p == null) return;
    p.duch = Math.max(p.duch, mob.getHealth());   // HP sprzed ciosu
    p.ostatnieTrafienie = teraz;
  }

  private void dodajWidza(Player gracz, LivingEntity mob) {
    Pasek p = paski.get(mob.getUniqueId());
    if (p == null) {
      p = new Pasek(mob.getUniqueId(), mob.getEntityId(), Bukkit.getUnsafe().nextEntityId());
      if (!opisz(p, mob)) return;
      ustawGeometrie(p, mob);
      p.duch = mob.getHealth();
      p.ostatnieTrafienie = ostatnieTrafienia.getOrDefault(p.mob, -1000);
      paski.put(p.mob, p);
      poMobie.put(p.idMoba, p);
      if (u.ukryjNazwe()) odswiezNazwe(mob);
    }
    if (!p.widzowie.add(gracz.getUniqueId())) return;
    pakiety.pokaz(gracz, p, mob, tekst(p, mob), u);
  }

  private void usun(Pasek p, boolean przywrocNazwe) {
    paski.remove(p.mob);
    poMobie.remove(p.idMoba);
    for (UUID w : p.widzowie) {
      Player g = getServer().getPlayer(w);
      if (g != null) pakiety.ukryj(g, p);
    }
    p.widzowie.clear();
    p.skale.clear();
    if (przywrocNazwe && getServer().getEntity(p.mob) instanceof LivingEntity mob && mob.isValid()) odswiezNazwe(mob);
  }

  private void ustawGeometrie(Pasek p, LivingEntity mob) {
    Double h = modelEngine && u.modelEngineWlaczone() ? ModeleME.wysokosc(mob.getUniqueId()) : null;
    p.wysokoscModelu = h == null ? mob.getHeight() : h;
    p.dodatkowaWysokosc = h == null ? 0 : h - mob.getHeight();
    p.podazanie = h != null && u.modelEngineTryb().equals("podazanie");
    org.bukkit.Location l = mob.getLocation();
    p.ostatnieX = l.getX(); p.ostatnieY = l.getY(); p.ostatnieZ = l.getZ();
    p.ostatniSwiat = l.getWorld().getName();
  }

  private void sprawdzGeometrie(Pasek p, LivingEntity mob) {
    Double h = modelEngine && u.modelEngineWlaczone() ? ModeleME.wysokosc(mob.getUniqueId()) : null;
    double nowa = h == null ? mob.getHeight() : h;
    boolean podazanie = h != null && u.modelEngineTryb().equals("podazanie");
    if (Math.abs(nowa - p.wysokoscModelu) < 0.001 && podazanie == p.podazanie) return;
    for (UUID w : p.widzowie) {
      Player g = getServer().getPlayer(w);
      if (g != null) pakiety.ukryj(g, p);
    }
    ustawGeometrie(p, mob);
    Component t = tekst(p, mob);
    for (UUID w : p.widzowie) {
      Player g = getServer().getPlayer(w);
      if (g != null) pakiety.pokaz(g, p, mob, t, u);
    }
  }

  /** Dwie zmiany w jednym ticku = Paper wysyla nazwe jeszcze raz (przez filtr pakietow albo juz bez niego). */
  private static void odswiezNazwe(LivingEntity mob) {
    Component n = mob.customName();
    boolean v = mob.isCustomNameVisible();
    if (n != null) { mob.customName(null); mob.customName(n); }
    mob.setCustomNameVisible(!v);
    mob.setCustomNameVisible(v);
  }

  // ---------------------------------------------------------------- odswiezanie

  /** Co 2 ticki: duch obrazen, HP, statusy - pakiet tekstu tylko przy zmianie. */
  private void odswiez() {
    int teraz = getServer().getCurrentTick();
    for (Pasek p : new ArrayList<>(paski.values())) {
      if (p.widzowie.isEmpty()) continue;
      if (!(getServer().getEntity(p.mob) instanceof LivingEntity mob) || !mob.isValid()) { usun(p, false); continue; }
      for (UUID w : p.widzowie) {
        Player g = getServer().getPlayer(w);
        if (g != null && g.getWorld().equals(mob.getWorld())) pakiety.aktualizujSkale(g, p, mob, u);
      }
      if (p.podazanie) {
        org.bukkit.Location l = mob.getLocation();
        double dx = l.getX() - p.ostatnieX, dy = l.getY() - p.ostatnieY, dz = l.getZ() - p.ostatnieZ;
        if (!l.getWorld().getName().equals(p.ostatniSwiat) || dx * dx + dy * dy + dz * dz > 0.0001) {
          p.ostatnieX = l.getX(); p.ostatnieY = l.getY(); p.ostatnieZ = l.getZ();
          p.ostatniSwiat = l.getWorld().getName();
          for (UUID w : p.widzowie) {
            Player g = getServer().getPlayer(w);
            if (g != null && g.getWorld().equals(l.getWorld())) pakiety.przesun(g, p, l);
          }
        }
      }
      double hp = mob.getHealth();
      if (p.duch < hp) p.duch = hp;
      else if (p.duch > hp && teraz - p.ostatnieTrafienie > 12)
        p.duch = Math.max(hp, p.duch - Math.max(maxHp(mob) * 0.03, (p.duch - hp) * 0.25));
      String klucz = klucz(p, mob);
      if (klucz.equals(p.ostatniKlucz)) continue;
      p.ostatniKlucz = klucz;
      Component t = tekst(p, mob);
      for (UUID w : p.widzowie) {
        Player g = getServer().getPlayer(w);
        if (g != null) pakiety.tekst(g, p, t);
      }
    }
  }

  /** Co sekunde: moby, ktore MM zarejestrowal po wejsciu w zasieg, i paski "vanilla", ktore okazaly sie mobami MM. */
  private void przeglad() {
    int teraz = getServer().getCurrentTick();
    ostatnieTrafienia.entrySet().removeIf(x -> teraz - x.getValue() >= u.walkaSekundy() * 20);
    double r = u.zasieg() + 8;
    for (Player g : getServer().getOnlinePlayers()) {
      for (Entity e : g.getNearbyEntities(r, r, r)) {
        if (!(e instanceof LivingEntity mob) || e instanceof Player) continue;
        Pasek p = paski.get(mob.getUniqueId());
        if (p != null && modelEngine && !u.modelEngineWlaczone() && ModeleME.jestModelowany(mob.getUniqueId())) {
          usun(p, true);
          continue;
        }
        if (p != null) sprawdzGeometrie(p, mob);
        if (p != null && (p.zMythicMobs || !mythic || !MythicBukkit.inst().getMobManager().isActiveMob(mob.getUniqueId()))) {
          if (!p.widzowie.contains(g.getUniqueId()) && mob.getTrackedBy().contains(g)) dodajWidza(g, mob);
          continue;
        }
        if (p != null) {   // byl vanillowy, a to mob MM: opisz od nowa
          if (!opisz(p, mob)) { usun(p, true); continue; }
          p.ostatniKlucz = "";
          continue;
        }
        if (mob.getTrackedBy().contains(g)) dodajWidza(g, mob);
      }
    }
  }

  private String klucz(Pasek p, LivingEntity mob) {
    double max = maxHp(mob);
    int k = max <= 0 ? 0 : (int) Math.ceil(Math.min(1, mob.getHealth() / max) * Wyglad.BAR_W);
    int j = max <= 0 ? 0 : (int) Math.ceil(Math.min(1, p.duch / max) * Wyglad.BAR_W);
    return p.nazwa + '|' + p.poziom + '|' + p.rzadkosc.id() + '|' + (int) Math.ceil(mob.getHealth()) + '|'
        + Math.round(max) + '|' + k + '|' + j + '|' + Wyglad.stopienHp(mob.getHealth(), max, u.kolorHp())
        + '|' + statusy(mob) + '|' + widac(p, mob) + '|' + trybTekstu(p, mob);
  }

  private Component tekst(Pasek p, LivingEntity mob) {
    String tryb = trybTekstu(p, mob);
    if (tryb.equals("nic")) return Component.empty();
    Component nazwa = p.nazwa != null ? Component.text(p.nazwa) : Component.translatable(mob.getType().translationKey());
    return Wyglad.tekst(nazwa, p.rzadkosc, p.poziom, mob.getHealth(), Math.max(p.duch, mob.getHealth()),
        maxHp(mob), statusy(mob), tryb.equals("wszystko"), u.liczbyHp(), u.kolorHp());
  }

  private static boolean widac(Pasek p, LivingEntity mob) {
    return !mob.hasPotionEffect(PotionEffectType.INVISIBILITY) && !mob.isInvisible();
  }

  private String trybTekstu(Pasek p, LivingEntity mob) {
    if (!widac(p, mob)) return "nic";
    int czas = getServer().getCurrentTick() - p.ostatnieTrafienie;
    boolean walka = p.ostatnieTrafienie >= 0 && czas >= 0 && czas < u.walkaSekundy() * 20;
    if (mob instanceof Mob m && m.getTarget() instanceof Player) walka = true;
    return walka ? "wszystko" : u.pozaWalka();
  }

  private static double maxHp(LivingEntity mob) {
    AttributeInstance a = mob.getAttribute(Attribute.MAX_HEALTH);
    return a == null ? mob.getHealth() : a.getValue();
  }

  private List<Wyglad.Status> statusy(LivingEntity m) {
    List<Wyglad.Status> s = new ArrayList<>(4);
    if (m.getFireTicks() > 0 && !m.isDead()) s.add(Wyglad.Status.OGIEN);
    if (m.hasPotionEffect(PotionEffectType.POISON)) s.add(Wyglad.Status.TRUCIZNA);
    if (krwawiacy != null && krwawiacy.containsKey(m.getUniqueId())) s.add(Wyglad.Status.KRWAWIENIE);
    if (m.hasPotionEffect(PotionEffectType.WITHER)) s.add(Wyglad.Status.OBUMARCIE);
    if (m.hasPotionEffect(PotionEffectType.SLOWNESS)) s.add(Wyglad.Status.SPOWOLNIENIE);
    if (m.hasPotionEffect(PotionEffectType.WEAKNESS)) s.add(Wyglad.Status.OSLABIENIE);
    if (m.hasPotionEffect(PotionEffectType.BLINDNESS) || m.hasPotionEffect(PotionEffectType.DARKNESS))
      s.add(Wyglad.Status.OSLEPIENIE);
    if (m.getFreezeTicks() > 0) s.add(Wyglad.Status.ZAMROZENIE);
    if (m.hasPotionEffect(PotionEffectType.STRENGTH)) s.add(Wyglad.Status.SILA);
    if (m.hasPotionEffect(PotionEffectType.RESISTANCE)) s.add(Wyglad.Status.ODPORNOSC);
    if (m.hasPotionEffect(PotionEffectType.REGENERATION)) s.add(Wyglad.Status.REGENERACJA);
    if (m.hasPotionEffect(PotionEffectType.SPEED)) s.add(Wyglad.Status.SZYBKOSC);
    if (m.hasPotionEffect(PotionEffectType.ABSORPTION) || m.getAbsorptionAmount() > 0) s.add(Wyglad.Status.ABSORPCJA);
    return s;
  }

  // ---------------------------------------------------------------- komenda

  @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
    if (a.length > 0 && a[0].equalsIgnoreCase("przeladuj")) {
      reloadConfig();
      u = Ustawienia.wczytaj(getConfig(), getLogger());
      pakiety.ukryjNazwe = u.ukryjNazwe();
      podepnijKrwawienie();
      for (Pasek p : new ArrayList<>(paski.values())) usun(p, true);
      przeglad();
      s.sendMessage("PaskiMobow: przeladowano, wpisow mobow " + u.moby().size() + ", paskow " + paski.size());
      return true;
    }
    if (a.length > 0 && a[0].equalsIgnoreCase("info")) {
      if (!(s instanceof Player g) || !(g.getTargetEntity(32) instanceof LivingEntity mob)) {
        s.sendMessage("PaskiMobow: patrz na moba (do 32 bl.). Paskow: " + paski.size());
        return true;
      }
      Pasek p = paski.get(mob.getUniqueId());
      String mm = mythic ? MythicBukkit.inst().getMobManager().getActiveMob(mob.getUniqueId())
          .map(am -> am.getType().getInternalName() + " frakcja=" + am.getFaction() + " poziomMM=" + am.getLevel())
          .orElse("nie-MM") : "brak MM";
      s.sendMessage(p == null ? "Bez paska: " + mm
          : "Pasek: " + p.nazwa + " | " + p.rzadkosc.nazwa() + " | poz. " + p.poziom + " | widzow " + p.widzowie.size()
            + " | " + mm + " | statusy " + statusy(mob));
      Double h = modelEngine ? ModeleME.wysokosc(mob.getUniqueId()) : null;
      s.sendMessage("ModelEngine: " + (h == null ? "brak aktywnego modelu" : "wysokosc=" + String.format(java.util.Locale.ROOT, "%.2f", h))
          + " | tryb=" + (p != null ? (p.podazanie ? "podazanie" : "pasazer")
              : h != null && u.modelEngineWlaczone() ? u.modelEngineTryb() : "pasazer"));
      return true;
    }
    s.sendMessage("/paskimobow przeladuj | info");
    return true;
  }
}
