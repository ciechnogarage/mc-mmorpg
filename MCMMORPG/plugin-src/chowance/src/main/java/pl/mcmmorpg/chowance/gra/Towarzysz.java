package pl.mcmmorpg.chowance.gra;

import io.lumine.mythic.bukkit.MythicBukkit;
import com.ticxo.modelengine.api.ModelEngineAPI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.Indyuce.mmocore.api.event.PlayerLevelChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.api.event.ChowaniecZlapanyEvent;
import pl.mcmmorpg.chowance.api.event.WalkaZakonczonaEvent;
import pl.mcmmorpg.chowance.integracja.Mythic;
import pl.mcmmorpg.chowance.logika.LimitSygnalow;
import pl.mcmmorpg.chowance.model.Osobnik;

/** Reakcje na zdarzenia i stan spokojnego chowańca; jedna pętla po aktywnych. */
public final class Towarzysz implements Listener {
  private final Chowance c;
  private final Przywolania przywolania;
  private final LimitSygnalow limit = new LimitSygnalow();
  private final Map<UUID, Long> ostatniaReakcja = new HashMap<>();
  private final Map<UUID, Integer> oczekujace = new HashMap<>();
  private final Map<UUID, Long> terminWygranej = new HashMap<>();
  private final Map<String, String> dzwieki = new HashMap<>();
  private static final Pattern DZWIEK = Pattern.compile("sound\\s*\\{[^}]*\\bs=([^;}]*)", Pattern.CASE_INSENSITIVE);

  public Towarzysz(Chowance c, Przywolania przywolania) { this.c = c; this.przywolania = przywolania; przeladuj(); }
  public void wyczysc(UUID gracz) { ostatniaReakcja.remove(gracz); oczekujace.remove(gracz); terminWygranej.remove(gracz); }
  public void przeladuj() {
    dzwieki.clear();
    for (var g : c.rej.gatunki().values()) {
      for (String id : new String[] {g.mobOswojony(false), g.mobOswojony(true)}) {
        if (id != null && !id.isBlank()) dzwieki.computeIfAbsent(id, this::dzwiek);
      }
    }
  }

  private void dodaj(Player p, int priorytet) { dodaj(p, priorytet, 1L); }
  private void dodaj(Player p, int priorytet, long opoznienie) {
    if (!c.ust.towarzyszWlaczony || p == null) return;
    UUID id = p.getUniqueId();
    boolean nowe = !oczekujace.containsKey(id);
    oczekujace.merge(id, priorytet, Math::max);
    if (nowe) Bukkit.getScheduler().runTaskLater(c.plugin, () -> reaguj(p), opoznienie);
  }

  private void reaguj(Player p) {
    UUID id = p.getUniqueId();
    Integer priorytet = oczekujace.get(id);
    Long termin = terminWygranej.get(id);
    if (priorytet != null && priorytet == 4 && termin != null && Bukkit.getCurrentTick() < termin) {
      Bukkit.getScheduler().runTaskLater(c.plugin, () -> reaguj(p), Math.max(1, termin - Bukkit.getCurrentTick()));
      return;
    }
    oczekujace.remove(id);
    terminWygranej.remove(id);
    if (priorytet == null || !p.isOnline() || przywolania.wWalce.test(p)) return;
    long teraz = System.currentTimeMillis();
    if (teraz - ostatniaReakcja.getOrDefault(p.getUniqueId(), 0L) < c.ust.reakcjaOdstepS * 1000L) return;
    Przywolania.Aktywny a = przywolania.aktywny(p);
    Osobnik o = przywolania.osobnik(p, a);
    if (a == null || o == null || !a.cialo.isValid() || !blisko(p.getLocation(), a.cialo.getLocation(), 12)) return;
    if (!limit.wez(teraz, c.ust.limitSygnalowNaS)) return;
    ostatniaReakcja.put(p.getUniqueId(), teraz);
    przywolania.sygnalAktywnemu(p, o, "OKRZYK");
    a.cialo.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, a.cialo.getLocation().add(0, 1, 0), 4, .2, .2, .2, 0);
  }

  @EventHandler public void wygrana(WalkaZakonczonaEvent e) {
    Player p = e.getZwyciezca();
    if (p == null) return;
    terminWygranej.put(p.getUniqueId(), (long) Bukkit.getCurrentTick() + 20L);
    dodaj(p, 4, 20L);
  }
  @EventHandler public void zlapany(ChowaniecZlapanyEvent e) {
    if (!"STARTER".equals(e.getSposob())) dodaj(e.getPlayer(), 3);
  }
  /** Tylko poziom postaci — MMOCore zgłasza tym samym eventem awanse profesji (zbieractwo co chwilę). */
  @EventHandler public void awans(PlayerLevelChangeEvent e) {
    if (e.getReason() == PlayerLevelChangeEvent.Reason.LEVEL_UP && !e.hasProfession() && e.getNewLevel() > e.getOldLevel()) dodaj(e.getPlayer(), 2);
  }
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void niskiHp(EntityDamageEvent e) {
    Player p = przywolania.wlascicielCiala(e.getEntity());
    if (p == null || !c.ust.towarzyszWlaczony || przywolania.wWalce.test(p)) return;
    Przywolania.Aktywny a = przywolania.aktywny(p);
    if (a == null || a.cialo != e.getEntity() || !a.niskiHpUzbrojony) return;
    var max = a.cialo.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
    if (max == null || max.getValue() <= 0) return;
    double po = a.cialo.getHealth() - e.getFinalDamage();
    if (po > 0 && po / max.getValue() < c.ust.niskiHpProg) {
      a.niskiHpUzbrojony = false;
      Osobnik o = przywolania.osobnik(p, a);
      if (o != null) c.pasek(p, "<red>" + c.nazwaKolor(o) + " ma mało zdrowia!");
    }
  }
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void zabicie(EntityDeathEvent e) {
    LivingEntity ofiara = e.getEntity();
    if (ofiara instanceof Player || Ciala.instancja(c, ofiara) != null) return;
    Player p = ofiara.getKiller();
    if (ofiara.getLastDamageCause() instanceof EntityDamageByEntityEvent dmg) {
      Entity sprawca = dmg.getDamager();
      if (sprawca instanceof Projectile pr && pr.getShooter() instanceof Entity x) sprawca = x;
      Player wl = przywolania.wlascicielCiala(sprawca);
      if (wl != null) p = wl;
    }
    if (p != null && ThreadLocalRandom.current().nextDouble() < .25) dodaj(p, 1);
  }

  public void tik(Przywolania.Aktywny a, Player p, long teraz) {
    if (!c.ust.towarzyszWlaczony || !a.cialo.getWorld().equals(p.getWorld())) { zakonczSpoczynek(a); return; }
    if (a.spoczynekModel != null && a.spoczynekPozycja != null
        && (!a.spoczynekPozycja.getWorld().equals(a.cialo.getWorld()) || a.spoczynekPozycja.distanceSquared(a.cialo.getLocation()) > .25)) zakonczSpoczynek(a);
    if (przywolania.wWalce.test(p)) { zakonczSpoczynek(a); a.bezruchOd = teraz; a.podszedl = false; a.odpoczal = false; return; }
    Location tu = p.getLocation();
    if (a.ostatniaPozycja == null || !a.ostatniaPozycja.getWorld().equals(tu.getWorld())
        || a.ostatniaPozycja.distanceSquared(tu) > .09 || p.isInsideVehicle() || teraz - a.ostatniaWalka < 5000) {
      zakonczSpoczynek(a);
      a.ostatniaPozycja = tu.clone(); a.bezruchOd = teraz; a.podszedl = false; a.odpoczal = false;
    }
    if (a.ambientTermin == 0) a.ambientTermin = termin(teraz);
    if (teraz >= a.ambientTermin) {
      a.ambientTermin = termin(teraz);
      if (blisko(tu, a.cialo.getLocation(), c.ust.ambientZasieg)) {
        String dzwiek = dzwieki.computeIfAbsent(Mythic.typ(a.cialo) == null ? "" : Mythic.typ(a.cialo), this::dzwiek);
        if (!dzwiek.isBlank() && limit.wez(teraz, c.ust.limitSygnalowNaS)) a.cialo.getWorld().playSound(a.cialo.getLocation(), dzwiek, SoundCategory.NEUTRAL, .35f,
            1.2f + ThreadLocalRandom.current().nextFloat() * .2f);
      }
    }
    if (teraz - a.bezruchOd >= c.ust.bezruchS * 1000L && !a.podszedl) {
      a.podszedl = true;
      if (!a.mythic && a.cialo instanceof Mob m && !blisko(tu, a.cialo.getLocation(), 1.5)) {
        org.bukkit.util.Vector od = a.cialo.getLocation().toVector().subtract(tu.toVector()).setY(0);
        if (od.lengthSquared() > .01) m.getPathfinder().moveTo(tu.clone().add(od.normalize().multiply(1.5)), 1.0);
      }
    }
    if (!a.odpoczal && teraz - a.bezruchOd >= 2L * c.ust.bezruchS * 1000L) {
      a.odpoczal = true;
      if (a.mythic && Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) odegrajSpoczynek(a);
    }
    double max = a.cialo.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null ? 0
        : a.cialo.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
    if (max > 0) {
      double hp = a.cialo.getHealth() / max;
      if (hp > c.ust.niskiHpReset) a.niskiHpUzbrojony = true;
      else if (hp < c.ust.niskiHpProg && a.niskiHpUzbrojony) {
        a.niskiHpUzbrojony = false;
        Osobnik o = przywolania.osobnik(p, a);
        if (o != null) c.pasek(p, "<red>" + c.nazwaKolor(o) + " ma mało zdrowia!");
      }
    }
  }

  private long termin(long teraz) { return teraz + ThreadLocalRandom.current().nextLong(c.ust.ambientMinS, c.ust.ambientMaxS + 1L) * 1000L; }

  private void odegrajSpoczynek(Przywolania.Aktywny a) {
    try {
      var modeled = ModelEngineAPI.getModeledEntity(a.cialo.getUniqueId());
      if (modeled == null) return;
      for (String id : modeled.getModels().keySet()) {
        String nazwa = c.ust.spoczynekModele.get(id.endsWith("_l") ? id.substring(0, id.length() - 2) : id);
        if (nazwa == null || nazwa.isBlank()) continue;
        modeled.getModel(id).ifPresent(model -> {
          if (model.getAnimationHandler().getAnimation(nazwa) != null) {
            model.getAnimationHandler().playAnimation(nazwa, 0.15, 0.15, 1.0, true);
            a.spoczynekModel = id; a.spoczynekAnimacja = nazwa; a.spoczynekPozycja = a.cialo.getLocation();
          }
        });
        break;
      }
    } catch (RuntimeException ex) { c.log.fine("Spoczynek ModelEngine: " + ex); }
  }

  private void zakonczSpoczynek(Przywolania.Aktywny a) {
    if (a.spoczynekModel == null || !Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) return;
    try {
      var modeled = ModelEngineAPI.getModeledEntity(a.cialo.getUniqueId());
      if (modeled != null) modeled.getModel(a.spoczynekModel).ifPresent(model -> model.getAnimationHandler().stopAnimation(a.spoczynekAnimacja));
    } catch (RuntimeException ex) { c.log.fine("Koniec spoczynku ModelEngine: " + ex); }
    a.spoczynekModel = null; a.spoczynekAnimacja = null; a.spoczynekPozycja = null;
  }
  private static boolean blisko(Location a, Location b, double r) { return a.getWorld().equals(b.getWorld()) && a.distanceSquared(b) <= r * r; }

  /** Config MythicMobs jest odczytywany tylko raz na typ; brak jawnego sound oznacza ciszę. */
  private String dzwiek(String id) {
    if (id.isBlank()) return "";
    try {
      var mob = MythicBukkit.inst().getMobManager().getMythicMob(id).orElse(null);
      if (mob == null) return "";
      for (String linia : mob.getConfig().getStringList("Skills")) {
        String s = String.valueOf(linia);
        if (!s.contains("onSignal:OKRZYK")) continue;
        Matcher m = DZWIEK.matcher(s);
        if (m.find()) return m.group(1).trim();
      }
    } catch (RuntimeException ex) { c.log.fine("Dźwięk ambientu " + id + ": " + ex); }
    return "";
  }
}
