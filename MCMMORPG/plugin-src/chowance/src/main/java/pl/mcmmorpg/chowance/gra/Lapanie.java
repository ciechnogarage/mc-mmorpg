package pl.mcmmorpg.chowance.gra;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.api.event.ChowaniecZlapanyEvent;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.integracja.Lochy;
import pl.mcmmorpg.chowance.integracja.Mythic;
import pl.mcmmorpg.chowance.integracja.PrzedmiotMmo;
import pl.mcmmorpg.chowance.integracja.SkalaModelu;
import pl.mcmmorpg.chowance.logika.WzorLapania;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Przedmioty;
import pl.mcmmorpg.chowance.model.Status;

/** Łapanie Pieczęciami (SPEC §2) i oswajanie karmieniem. */
public final class Lapanie implements Listener {
  private final Chowance c;
  private final Dzikie dzikie;
  private final Druzyny druzyny;
  private final Przywolania przywolania;
  private final Map<UUID, Long> ostatniRzut = new ConcurrentHashMap<>();
  private final Map<String, Integer> oswajanie = new ConcurrentHashMap<>();
  private final Map<UUID, Long> ostatnieKarmienie = new ConcurrentHashMap<>();
  public java.util.function.Predicate<Player> wWalce = p -> false;

  public Lapanie(Chowance c, Dzikie dzikie, Druzyny druzyny, Przywolania przywolania) {
    this.c = c;
    this.dzikie = dzikie;
    this.druzyny = druzyny;
    this.przywolania = przywolania;
  }

  public Przedmioty.Pieczec pieczec(ItemStack it) {
    PrzedmiotMmo.Id id = PrzedmiotMmo.z(it);
    if (id == null) return null;
    Przedmioty.Pieczec p = c.rej.przedmioty().pieczecie().get(id.id());
    return p != null && p.typMmoitems().equals(id.typ()) ? p : null;
  }

  // ------------------------------------------------------------------ rzut

  @EventHandler(priority = EventPriority.HIGH)
  public void rzut(PlayerInteractEvent e) {
    if (e.getHand() != EquipmentSlot.HAND) return;
    if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    ItemStack it = e.getItem();
    Przedmioty.Pieczec pc = pieczec(it);
    if (pc == null) return;
    e.setCancelled(true);
    Player p = e.getPlayer();
    if (c.daneLubBlad(p) == null) return;
    if (wWalce.test(p)) { c.blad(p, "W walce turowej użyj przycisku „Pieczęć”."); return; }
    long teraz = System.currentTimeMillis();
    Long ost = ostatniRzut.get(p.getUniqueId());
    if (ost != null && teraz - ost < c.ust.odstepRzutuMs) return;
    ostatniRzut.put(p.getUniqueId(), teraz);
    ItemStack jeden = it.clone();
    jeden.setAmount(1);
    it.setAmount(it.getAmount() - 1);
    Snowball s = p.launchProjectile(Snowball.class, p.getLocation().getDirection().multiply(c.ust.predkoscRzutu));
    s.setItem(jeden);
    s.getPersistentDataContainer().set(c.kPieczec, PersistentDataType.STRING, pc.id());
    s.getPersistentDataContainer().set(c.kRzucajacy, PersistentDataType.STRING, p.getUniqueId().toString());
    p.playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.8f, 0.8f);
  }

  /**
   * Pieczęć użyta Z BLISKA na dzikim (PPM albo LPM) = rzut prosto w niego. Klient klikający PPM w hitbox modelu
   * MEG (encja interaction) nie wysyła „użyj przedmiotu”, więc zwykły rzut nie startował, a LPM bił stwora
   * pieczęcią — dziki się mścił, a przywołany chowaniec rzucał się do walki (zgłoszenie właściciela 2026-09-28).
   * Woła to most ModelEngine, PPM na ciele bez modelu i LPM (obrażenia anulowane). true = obsłużone.
   */
  public boolean pieczecNaDzikim(Player p, LivingEntity le) {
    ItemStack it = p.getInventory().getItemInMainHand();
    Przedmioty.Pieczec pc = pieczec(it);
    if (pc == null || !dzikie.dziki(le)) return false;
    if (c.daneLubBlad(p) == null) return true;
    if (wWalce.test(p)) { c.blad(p, "W walce turowej użyj klawisza [7] „Pieczęć”."); return true; }
    // odstęp rzutu najpierw: LPM w model MEG przychodzi dwa razy (zdarzenie MEG + obrażenia) — dubel po cichu
    long teraz = System.currentTimeMillis();
    Long ost = ostatniRzut.get(p.getUniqueId());
    if (ost != null && teraz - ost < c.ust.odstepRzutuMs) return true;
    if (dzikie.zajete.contains(le.getUniqueId())) { c.pasek(p, "<gray>Pieczęć już wiąże tego stwora…"); return true; }
    ostatniRzut.put(p.getUniqueId(), teraz);
    ItemStack jeden = it.clone();
    jeden.setAmount(1);
    it.setAmount(it.getAmount() - 1);
    uspokoj(le);
    dzikie.zajete.add(le.getUniqueId()); // nietykalny już w locie pieczęci
    Location start = p.getEyeLocation().add(p.getLocation().getDirection().multiply(0.6)).add(0, -0.3, 0);
    Location cel = le.getLocation().add(0, Math.min(1.0, le.getHeight() * 0.6), 0);
    ItemDisplay lot = start.getWorld().spawn(start, ItemDisplay.class, x -> {
      x.setItemStack(jeden.clone());
      x.setPersistent(false);
      x.setTeleportDuration(6);
      x.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.5f, 0.5f, 0.5f), new AxisAngle4f()));
    });
    p.playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.8f, 0.8f);
    p.swingMainHand();
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> { if (lot.isValid()) lot.teleport(cel); }, 1L);
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      lot.remove();
      dzikie.zajete.remove(le.getUniqueId());
      if (!p.isOnline() || !le.isValid()) { oddaj(jeden, cel); return; }
      proba(p, le, pc, jeden);
    }, 7L);
    return true;
  }

  /** LPM pieczęcią w dzikiego: bez obrażeń, zamiast tego rzut (patrz pieczecNaDzikim). */
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void uderzeniePieczecia(EntityDamageByEntityEvent e) {
    if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity le)) return;
    if (pieczec(p.getInventory().getItemInMainHand()) == null || !dzikie.dziki(le)) return;
    e.setCancelled(true);
    pieczecNaDzikim(p, le);
  }

  /** Stwór przestaje walczyć: bez celu, bez szału, nikt z pobliskich mobów (także chowańce gracza) w niego nie celuje. */
  private void uspokoj(LivingEntity le) {
    if (le instanceof Mob m) m.setTarget(null);
    le.removePotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH);
    le.removePotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
    for (Entity e : le.getNearbyEntities(24, 12, 24)) {
      if (e instanceof Mob m && le.equals(m.getTarget())) m.setTarget(null);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void trafienie(ProjectileHitEvent e) {
    Projectile pr = e.getEntity();
    String pid = pr.getPersistentDataContainer().get(c.kPieczec, PersistentDataType.STRING);
    if (pid == null) return;
    e.setCancelled(true);
    String rz = pr.getPersistentDataContainer().get(c.kRzucajacy, PersistentDataType.STRING);
    ItemStack przedmiot = pr instanceof Snowball sb ? sb.getItem() : null;
    Location gdzie = pr.getLocation();
    pr.remove();
    Player p = rz == null ? null : Bukkit.getPlayer(UUID.fromString(rz));
    Przedmioty.Pieczec pc = c.rej.przedmioty().pieczecie().get(pid);
    Entity cel = e.getHitEntity();
    // pomoc w celowaniu: pieczęć spadła tuż przy dzikim (w ziemię u jego stóp) — liczy się jak trafienie
    if (cel == null && gdzie != null) {
      cel = najblizszyDziki(gdzie, 2.0);   // pomiar: rzut „pod nogi” z 7 bl. spada ~1,7 bl. przed stworem
      if (cel == null && c.ust.debug) {
        LivingEntity d4 = najblizszyDziki(gdzie, 5);
        c.debug("pieczęć chybiła: upadek " + gdzie.toVector() + (d4 == null ? " (brak dzikich w 5 bl.)" : " odl. od dzikiego " + String.format("%.2f", d4.getLocation().distance(gdzie))));
      }
    }
    if (p == null || pc == null || !(cel instanceof LivingEntity le) || !dzikie.dziki(le)) {
      oddaj(przedmiot, gdzie);
      if (p != null && p.isOnline()) {
        c.pasek(p, cel instanceof LivingEntity && !(cel instanceof Player)
            ? "<red>Pieczęć działa tylko na dzikie chowańce <gray>— leży na ziemi."
            : "<gray>Pieczęć chybiła — leży na ziemi. <white>Podejdź i kliknij stwora pieczęcią.");
      }
      return;
    }
    proba(p, le, pc, przedmiot);
  }

  private LivingEntity najblizszyDziki(Location l, double r) {
    LivingEntity best = null;
    double bd = r * r;
    for (Entity x : l.getWorld().getNearbyEntities(l, r, r + 1, r)) {
      if (!(x instanceof LivingEntity le) || x instanceof Player || !dzikie.dziki(le) || dzikie.zajete.contains(le.getUniqueId())) continue;
      double d = le.getLocation().distanceSquared(l);
      if (d <= bd) { bd = d; best = le; }
    }
    return best;
  }

  private static void oddaj(ItemStack przedmiot, Location l) {
    if (przedmiot != null && l != null && l.getWorld() != null) l.getWorld().dropItemNaturally(l, przedmiot);
  }

  // ------------------------------------------------------------------ próba

  /** Próba schwytania. {@code przedmiot} (może być null) wraca na ziemię, gdy pieczęć się odbije. */
  public void proba(Player p, LivingEntity le, Przedmioty.Pieczec pc, ItemStack przedmiot) {
    Location l = le.getLocation();
    ProfilDane d = c.dane(p);
    Osobnik o = dzikie.osobnik(le);
    Gatunek g = o == null ? null : c.gatunek(o);
    String odmowa = null;
    if (d == null) odmowa = "Chowańce wczytują się po wybraniu postaci.";
    else if (g == null) odmowa = "Tego stwora nie da się związać.";
    else if (dzikie.zajete.contains(le.getUniqueId())) odmowa = "Ktoś właśnie wiąże tego stwora.";
    else if (!g.lapaniePieczecia()) odmowa = g.nazwa() + " nie daje się związać pieczęcią — spróbuj go oswoić.";
    else if (!g.wymaganaPieczec().isBlank() && !g.wymaganaPieczec().equals(pc.id())) odmowa = g.nazwa() + " przyjmie tylko pieczęć " + g.wymaganaPieczec() + ".";
    else if (g.legendarny() && !dzikie.wOknieLegendarnym(le)) odmowa = g.nazwa() + " jest zbyt potężny — najpierw go pokonaj.";
    else if (pc.rodzaj() == Przedmioty.RodzajPieczeci.LOCHOWA && !Lochy.wLochu(p)) odmowa = "Pieczęć lochowa działa tylko w lochach.";
    else if (d.druzyna.size() >= c.limitDruzyny(p) && d.magazyn.size() >= c.limitMagazynu(p)) odmowa = "Nie masz miejsca ani w drużynie, ani w magazynie.";
    else {
      UUID zbiegl = dzikie.zbieglyOd(le);
      if (zbiegl != null && !zbiegl.equals(p.getUniqueId()) && pc.rodzaj() != Przedmioty.RodzajPieczeci.KRADZIEZY)
        odmowa = "Ten stwór jest spłoszony przez innego gracza — działa na niego tylko Pieczęć Kradzieży.";
    }
    if (odmowa != null) {
      c.debug("łapanie ODMOWA " + p.getName() + " " + (g == null ? "?" : g.id()) + ": " + odmowa);
      c.blad(p, odmowa);
      le.getWorld().playSound(l, Sound.ITEM_SHIELD_BLOCK, 0.7f, 1.4f);
      oddaj(przedmiot, l);
      return;
    }
    d.oznaczWidziany(g.id());
    double mnoz = pc.mnoznik() * (warunekSpelniony(p, le, pc) ? pc.mnoznikWarunku() : 1.0);
    WzorLapania.Wynik wynik;
    if (g.legendarny()) {
      boolean ok = ThreadLocalRandom.current().nextDouble() < c.ust.szansaLegendarnego;
      wynik = new WzorLapania.Wynik(c.ust.szansaLegendarnego, ok ? 3 : ThreadLocalRandom.current().nextInt(3), ok);
    } else {
      var attr = le.getAttribute(Attribute.MAX_HEALTH);
      double max = attr == null ? le.getHealth() : attr.getValue();
      Status st = dzikie.status(le);
      int odn = c.ust.odniesienieAktywny && przywolania.osobnikAktywny(p) != null
          ? przywolania.osobnikAktywny(p).poziom : (d.druzyna.isEmpty() ? o.poziom : druzyny.maxPoziom(d));
      WzorLapania w = new WzorLapania(c.ust.lapanie);
      WzorLapania.Wejscie we = new WzorLapania.Wejscie(le.getHealth(), max, g.wspolczynnikZlapania(), mnoz,
          st == null ? null : st.name(), o.poziom, odn, c.mmocore.poziom(p));
      wynik = w.proba(we, ThreadLocalRandom.current());
    }
    c.debug("łapanie " + p.getName() + " " + g.id() + " pieczęć=" + pc.id() + " mnożnik=" + mnoz + " p=" + wynik.szansa() + " drżenia=" + wynik.drzenia() + " sukces=" + wynik.sukces());
    animacja(p, le, pc, przedmiot, o, g, wynik);
  }

  private boolean warunekSpelniony(Player p, LivingEntity le, Przedmioty.Pieczec pc) {
    return switch (pc.rodzaj()) {
      case NOCNA -> { long t = le.getWorld().getTime(); yield t >= 13000 && t <= 23000; }
      case WODNA -> le.isInWater() || (le.getWorld().hasStorm() && le.getLocation().getBlock().getTemperature() < 0.95);
      case KRADZIEZY -> { UUID z = dzikie.zbieglyOd(le); yield z != null && !z.equals(p.getUniqueId()); }
      case LOCHOWA -> Lochy.wLochu(p);
      default -> false;
    };
  }

  private void animacja(Player p, LivingEntity le, Przedmioty.Pieczec pc, ItemStack przedmiot, Osobnik o, Gatunek g, WzorLapania.Wynik w) {
    UUID id = le.getUniqueId();
    dzikie.zajete.add(id);
    uspokoj(le);
    le.setAI(false);
    le.setVelocity(new Vector());
    Location l = le.getLocation().clone();
    Location srodek = l.clone().add(0, Math.min(1.0, le.getHeight() * 0.5), 0);
    Entity model = null;
    ItemDisplay disp = null;
    if (!pc.modelMob().isBlank() && Mythic.mobIstnieje(pc.modelMob())) {
      var am = Mythic.spawn(pc.modelMob(), l, 1);
      if (am != null && am.getEntity() != null) {
        model = am.getEntity().getBukkitEntity();
        model.setPersistent(false);
        // ciało modelu to stojak na zbroję — bez tego klient widzi go przez chwilę, zanim MEG nałoży model (klient QA)
        if (model instanceof LivingEntity lm) lm.setInvisible(true);
      }
    }
    if (model == null) {
      ItemStack wyg = przedmiot != null ? przedmiot.clone() : new ItemStack(org.bukkit.Material.PAPER);
      disp = l.getWorld().spawn(l.clone().add(0, 0.35, 0), ItemDisplay.class, x -> {
        x.setItemStack(wyg);
        x.setPersistent(false);
        x.setInterpolationDuration(4);
        x.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f()));
      });
    }
    // Wciągnięcie: model MEG kurczy się do pieczęci (setInvisible chowa tylko bazę), smuga cząstek zbiega do środka.
    boolean meg = SkalaModelu.maModel(le);
    double skala0 = meg ? SkalaModelu.skala(le) : 1.0;
    int kroki = 8;
    for (int i = 1; i <= kroki; i++) {
      int nr = i;
      Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
        if (!le.isValid()) return;
        double f = 1.0 - (double) nr / kroki;
        if (meg) SkalaModelu.ustaw(le, Math.max(0.02, skala0 * f));
        double r = 0.3 + 0.9 * f;
        for (int k = 0; k < 6; k++) {
          double a = nr * 0.9 + k * Math.PI / 3;
          srodek.getWorld().spawnParticle(Particle.END_ROD, srodek.clone().add(Math.cos(a) * r, (k % 2 == 0 ? 0.3 : -0.2) * f, Math.sin(a) * r), 0,
              -Math.cos(a) * 0.08, 0, -Math.sin(a) * 0.08, 1);
        }
        if (nr == kroki) le.setInvisible(true);
      }, i);
    }
    l.getWorld().playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.8f);
    l.getWorld().playSound(l, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);
    tytul(p, "", "<gold>Pieczęć wiąże: <white>" + g.nazwa() + " <gray>○○○", 30);
    c.pasek(p, "<gold>Wiązanie: <white>" + g.nazwa() + " <gray>○○○");
    final Entity fModel = model;
    final ItemDisplay fDisp = disp;
    int drz = w.drzenia();
    for (int i = 1; i <= drz; i++) {
      int nr = i;
      Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
        if (fModel != null && fModel.isValid()) Mythic.sygnal(fModel, "DRZENIE", p);
        if (fDisp != null && fDisp.isValid()) {
          float kat = (nr % 2 == 0 ? 1 : -1) * 0.45f;
          fDisp.setInterpolationDelay(0);
          fDisp.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(kat, 0, 0, 1), new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f()));
          Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
            if (fDisp.isValid()) fDisp.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f()));
          }, 6L);
        }
        l.getWorld().playSound(l, Sound.BLOCK_CHAIN_HIT, 0.9f, 0.7f + 0.15f * nr);
        l.getWorld().spawnParticle(Particle.CRIT, l.clone().add(0, 0.5, 0), 6, 0.15, 0.1, 0.15, 0.05);
        String kropki = "<yellow>" + "●".repeat(nr) + "<gray>" + "○".repeat(3 - nr);
        tytul(p, "", "<gold>Pieczęć drży… " + kropki, 25);
        c.pasek(p, "<gold>Wiązanie: <white>" + g.nazwa() + " " + kropki);
      }, 20L * i);
    }
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (fDisp != null) fDisp.remove();
      if (fModel != null && fModel.isValid()) {
        Mythic.sygnal(fModel, w.sukces() ? "SUKCES" : "PORAZKA", p);
        Bukkit.getScheduler().runTaskLater(c.plugin, () -> Mythic.usun(fModel), 20L);
      }
      dzikie.zajete.remove(id);
      if (!le.isValid()) { oddaj(przedmiot, l); return; }
      if (w.sukces() && p.isOnline()) {
        dzikie.zamknijOkno(le);
        schwytaj(p, le, o, "PIECZEC", pc.id());
      } else {
        le.setInvisible(false);
        if (meg) SkalaModelu.ustaw(le, skala0);
        // Pokonany legendarny w oknie łapania zostaje ogłuszony (bez AI) — inaczej po pierwszej
        // nieudanej próbie uciekał z 1 HP i kolejne pieczęcie chybiały (PROBLEMY A-015).
        boolean oknoLeg = g.legendarny() && dzikie.wOknieLegendarnym(le);
        le.setAI(!oknoLeg);
        uspokoj(le);
        l.getWorld().spawnParticle(Particle.EXPLOSION, l.clone().add(0, 0.6, 0), 1);
        l.getWorld().playSound(l, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
        if (p.isOnline()) {
          if (!oknoLeg) {
            // odskok od gracza — wyrywa się, ale nie atakuje (szał tylko gdy lapanie.szal_sekundy > 0)
            Vector od = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (od.lengthSquared() > 1e-4) le.setVelocity(od.normalize().multiply(0.45).setY(0.35));
            dzikie.oznaczZbieglego(le, p);
            if (c.ust.szalSek > 0) dzikie.szal(le, p);
          }
          int proc = (int) Math.round(w.szansa() * 100);
          tytul(p, "<red><b>Wyrwał się!", "<gray>szansa " + proc + "% <dark_gray>· <white>" + krotko(le, g), 50);
          c.msg(p, "<red>" + g.nazwa() + " wyrywa się z pieczęci" + (drz > 0 ? " po " + drz + (drz == 1 ? " drżeniu" : " drżeniach") : "")
              + "! <gray>(szansa " + proc + "%) <white>" + podpowiedz(le, pc, g));
        }
      }
    }, 20L * (drz + 1) + 5);
  }

  /** Krótka wersja podpowiedzi do podtytułu (długi podtytuł ucina się na krawędziach ekranu — klient QA). */
  private String krotko(LivingEntity le, Gatunek g) {
    if (g.legendarny()) return "rzucaj dalej";
    var attr = le.getAttribute(Attribute.MAX_HEALTH);
    double max = attr == null ? le.getHealth() : attr.getValue();
    if (max > 0 && le.getHealth() / max > 0.5) return "osłab go i rzuć znowu";
    return dzikie.status(le) == null ? "uśpij go albo lepsza pieczęć" : "spróbuj ponownie";
  }

  /** Co gracz może zrobić, by następna pieczęć miała większą szansę. */
  private String podpowiedz(LivingEntity le, Przedmioty.Pieczec pc, Gatunek g) {
    if (g.legendarny()) return "Rzucaj dalej, póki słabnie.";
    var attr = le.getAttribute(Attribute.MAX_HEALTH);
    double max = attr == null ? le.getHealth() : attr.getValue();
    if (max > 0 && le.getHealth() / max > 0.5) return "Osłab go — im mniej HP, tym łatwiej.";
    if (dzikie.status(le) == null) return "Uśpij albo spętaj go, lub weź lepszą pieczęć.";
    return "Spróbuj ponownie albo weź lepszą pieczęć.";
  }

  private static void tytul(Player p, String gora, String dol, int ticki) {
    if (p == null || !p.isOnline()) return;
    p.showTitle(net.kyori.adventure.title.Title.title(Chowance.mm(gora), Chowance.mm(dol),
        net.kyori.adventure.title.Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofMillis(ticki * 50L), java.time.Duration.ofMillis(250))));
  }

  /** Wspólna ścieżka sukcesu: pieczęć, oswojenie, chwLap. */
  public void schwytaj(Player p, LivingEntity le, Osobnik o, String sposob, String pieczec) {
    ProfilDane d = c.dane(p);
    Gatunek g = c.gatunek(o);
    if (d == null || g == null) return;
    Osobnik nowy = o.kopia();
    nowy.id = UUID.randomUUID().toString();
    nowy.pochodzenie = sposob.equals("OSWOJENIE") ? "OSWOJONY" : (g.legendarny() ? "LEGENDARNY" : "DZIKI");
    nowy.pieczec = pieczec;
    nowy.zwiazany = !g.wymienialny();
    druzyny.ustawHp(nowy, Math.min(le.getHealth(), druzyny.hpMax(nowy)));
    Druzyny.Gdzie gdzie = druzyny.dodaj(p, d, nowy, true);
    if (gdzie == Druzyny.Gdzie.BRAK_MIEJSCA) {
      c.blad(p, "Brak miejsca — stwór ucieka.");
      le.setInvisible(false);
      le.setAI(true);
      return;
    }
    Location l = le.getLocation();
    Mythic.usun(le);
    l.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, l.clone().add(0, 0.8, 0), 40, 0.4, 0.5, 0.4, 0.2);
    l.getWorld().playSound(l, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.3f);
    String dokad = gdzie == Druzyny.Gdzie.DRUZYNA ? "drużyna" : "magazyn";
    c.msg(p, "<green>Związano: " + c.nazwaKolor(nowy) + " <gray>(poz. " + nowy.poziom + ") → " + dokad);
    tytul(p, "<green><b>Związano!", c.nazwaKolor(nowy) + " <gray>poz. " + nowy.poziom + " → <white>" + dokad, 60);
    double exp = c.ust.expZlapanie * (1 + nowy.poziom / 10.0) * (nowy.lsniacy ? c.ust.expLsniacyMn : 1.0);
    if (sposob.equals("OSWOJENIE")) exp += c.ust.expOswojenie;
    c.mmocore.dajExp(p, exp);
    Bukkit.getPluginManager().callEvent(new ChowaniecZlapanyEvent(p, nowy, nowy.pochodzenie));
    c.zdarzenie("ZLAPANIE", druzyny.ph(p, nowy));
    c.dane.zapisz(d);
  }

  // ------------------------------------------------------------------ oswajanie

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void karmienieDzikiego(PlayerInteractEntityEvent e) {
    if (e.getHand() != EquipmentSlot.HAND) return;
    if (!(e.getRightClicked() instanceof LivingEntity le)) return;
    if (ppmDziki(e.getPlayer(), le)) e.setCancelled(true);
  }

  private final Map<UUID, Long> ostatniePpm = new ConcurrentHashMap<>();

  /** PPM przysmakiem na dzikiego (oswajanie). true = kliknięcie obsłużone. Woła je też most ModelEngine. */
  public boolean ppmDziki(Player p, LivingEntity le) {
    Osobnik o = dzikie.osobnik(le);
    if (o == null) return false;
    if (pieczecNaDzikim(p, le)) return true;
    ItemStack it = p.getInventory().getItemInMainHand();
    String id = PrzedmiotMmo.id(it);
    Gatunek g = c.gatunek(o);
    if (g == null || id == null) return false;
    Integer pkt = g.przysmaki().get(id);
    if (pkt == null) return false;
    long tick = Bukkit.getCurrentTick();
    Long prev = ostatniePpm.put(p.getUniqueId(), tick * 1_000_003L + le.getEntityId());
    if (prev != null && prev == tick * 1_000_003L + le.getEntityId()) return true;
    if (!g.oswajanie()) { c.blad(p, g.nazwa() + " nie daje się oswoić."); return true; }
    ProfilDane d = c.daneLubBlad(p);
    if (d == null || dzikie.zajete.contains(le.getUniqueId())) return true;
    if (p.getLocation().distanceSquared(le.getLocation()) > c.ust.zasiegOswajania * c.ust.zasiegOswajania) return true;
    long teraz = System.currentTimeMillis();
    Long ost = ostatnieKarmienie.get(p.getUniqueId());
    if (ost != null && teraz - ost < c.ust.odstepOswajaniaMs) return true;
    ostatnieKarmienie.put(p.getUniqueId(), teraz);
    it.setAmount(it.getAmount() - 1);
    String klucz = le.getUniqueId() + ":" + p.getUniqueId();
    int postep = Math.min(100, oswajanie.getOrDefault(klucz, 0) + pkt);
    oswajanie.put(klucz, postep);
    d.oznaczWidziany(g.id());
    le.getWorld().spawnParticle(Particle.HEART, le.getLocation().add(0, 1.2, 0), 3, 0.3, 0.2, 0.3, 0);
    le.getWorld().playSound(le.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.2f);
    int kreski = postep / 10;
    c.pasek(p, "<light_purple>Oswajanie " + g.nazwa() + ": <green>" + "■".repeat(kreski) + "<gray>" + "■".repeat(10 - kreski) + " <white>" + postep + "%");
    if (postep >= 100) {
      if (d.druzyna.size() >= c.limitDruzyny(p) && d.magazyn.size() >= c.limitMagazynu(p)) {
        c.blad(p, "Nie masz miejsca — zwolnij miejsce w magazynie.");
        return true;
      }
      oswajanie.remove(klucz);
      dzikie.zajete.add(le.getUniqueId());
      le.setAI(false);
      Mythic.sygnal(le, "OSWOJONY", p);
      Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
        dzikie.zajete.remove(le.getUniqueId());
        if (le.isValid() && p.isOnline()) schwytaj(p, le, o, "OSWOJENIE", null);
      }, 10L);
    }
    return true;
  }

  public int postepOswajania(Player p) {
    int max = 0;
    String suf = ":" + p.getUniqueId();
    for (Map.Entry<String, Integer> e : oswajanie.entrySet()) if (e.getKey().endsWith(suf)) max = Math.max(max, e.getValue());
    return max;
  }
}
