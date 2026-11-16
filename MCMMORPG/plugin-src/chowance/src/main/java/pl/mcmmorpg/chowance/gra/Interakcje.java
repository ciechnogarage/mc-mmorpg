package pl.mcmmorpg.chowance.gra;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.integracja.Mythic;
import pl.mcmmorpg.chowance.integracja.PrzedmiotMmo;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Przedmioty;

/** PPM na własnego stwora: karma (exp+więź), siodło, dosiadanie; PPM eliksirem: leczenie (SPEC §7). */
public final class Interakcje implements Listener {
  private final Chowance c;
  private final Druzyny druzyny;
  private final Przywolania przywolania;
  private final Map<String, Long> karmiony = new ConcurrentHashMap<>();
  private final Map<UUID, Long> efektGlaskania = new ConcurrentHashMap<>();
  private final Map<UUID, Long> podpowiedzBron = new ConcurrentHashMap<>();
  public java.util.function.BiConsumer<Player, Integer> krok = (p, n) -> {};
  public java.util.function.Predicate<Player> wWalce = p -> false;

  public Interakcje(Chowance c, Druzyny druzyny, Przywolania przywolania) {
    this.c = c;
    this.druzyny = druzyny;
    this.przywolania = przywolania;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void naStwora(PlayerInteractEntityEvent e) {
    if (e.getHand() != EquipmentSlot.HAND || !(e.getRightClicked() instanceof LivingEntity le)) return;
    if (ppm(e.getPlayer(), le)) e.setCancelled(true);
  }

  /** Ostatnio obsłużone PPM gracz→ciało (tick): Bukkit i most ModelEngine mogą zgłosić to samo kliknięcie. */
  private final Map<UUID, Long> ostatniePpm = new ConcurrentHashMap<>();

  private boolean duplikat(Player p, LivingEntity le) {
    long tick = org.bukkit.Bukkit.getCurrentTick();
    Long prev = ostatniePpm.put(p.getUniqueId(), tick * 1_000_003L + le.getEntityId());
    return prev != null && prev == tick * 1_000_003L + le.getEntityId();
  }

  /** PPM na własnego stwora. Zwraca true, gdy kliknięcie dotyczyło chowańca (należy anulować vanillę). */
  public boolean ppm(Player p, LivingEntity le) {
    Player wl = przywolania.wlascicielCiala(le);
    c.debug("ppm " + p.getName() + " -> " + le.getType() + "/" + le.getUniqueId() + " wlasciciel=" + (wl == null ? "-" : wl.getName())
        + " reka=" + PrzedmiotMmo.id(p.getInventory().getItemInMainHand()));
    if (wl == null) return false;
    if (duplikat(p, le)) return true;
    if (!wl.equals(p)) return true;
    ProfilDane d = c.dane(p);
    Osobnik o = przywolania.osobnikAktywny(p);
    if (d == null || o == null) return true;
    Gatunek g = c.gatunek(o);
    ItemStack it = p.getInventory().getItemInMainHand();
    PrzedmiotMmo.Id id = PrzedmiotMmo.z(it);
    Przedmioty prz = c.rej.przedmioty();
    if (id != null && prz.karma().containsKey(id.id()) && prz.karma().get(id.id()).typMmoitems().equals(id.typ())) {
      karm(p, d, o, it, prz.karma().get(id.id()));
      return true;
    }
    if (id != null && prz.siodla().containsKey(id.id())) {
      if (g == null || !g.wierzchowiec()) { c.blad(p, c.nazwa(o) + " nie uniesie jeźdźca."); return true; }
      if (o.siodlo) { c.pasek(p, "<gray>" + c.nazwa(o) + " ma już siodło."); return true; }
      it.setAmount(it.getAmount() - 1);
      o.siodlo = true;
      d.brudne = true;
      p.playSound(le.getLocation(), Sound.ENTITY_HORSE_SADDLE, 1f, 1f);
      c.msg(p, c.nazwaKolor(o) + " <green>nosi siodło — PPM pustą ręką, by dosiąść.");
      return true;
    }
    if (id != null && prz.eliksiry().containsKey(id.id())) {
      uzyjEliksiru(p, d, o, it, prz.eliksiry().get(id.id()));
      return true;
    }
    boolean pusta = pustaReka(it);
    if (c.ust.towarzyszWlaczony && !pusta) {
      long teraz = System.currentTimeMillis();
      if (pl.mcmmorpg.chowance.logika.Odstep.gotowy(podpowiedzBron.getOrDefault(p.getUniqueId(), 0L), teraz, 600_000)) {
        podpowiedzBron.put(p.getUniqueId(), teraz);
        c.pasek(p, "<gray>Pustą ręką (PPM) pogłaszczesz " + c.nazwaKolor(o) + "<gray> — tak rośnie więź.");
      }
      return true;
    }
    if (c.ust.towarzyszWlaczony && pusta && !nosnikZaklecia(p.getInventory().getItemInOffHand())
        && (!o.siodlo || g == null || !g.wierzchowiec() || p.isSneaking())) {
      poglaszcz(p, d, o);
      return true;
    }
    if (pusta && o.siodlo && g != null && g.wierzchowiec()) {
      if (wWalce.test(p)) return true;
      Mythic.sygnal(le, "DOSIADZ", p);
      if (Mythic.aktywny(le) == null) le.addPassenger(p); // ciało zastępcze: vanillowe dosiadanie
    }
    return true;
  }

  /** Zaślepki Księgi Postaci (puste sloty mikstur, rezerwa, obraz lewej broni) nie mają własnej akcji PPM —
   *  nowa postać ma pasek zajęty nimi i bronią, więc liczą się jak pusta ręka (eval_dopieszczenie 03.10). */
  private static final NamespacedKey[] ZASLEPKI = {
      new NamespacedKey("ksiegapostaci", "zuzywalny-slot"), new NamespacedKey("ksiegapostaci", "zapas"),
      new NamespacedKey("ksiega", "obraz"), new NamespacedKey("ksiega", "klon")};

  private static boolean pustaReka(ItemStack i) {
    if (i == null || i.getType().isAir()) return true;
    if (!i.hasItemMeta()) return false;
    var pdc = i.getItemMeta().getPersistentDataContainer();
    for (NamespacedKey k : ZASLEPKI) if (pdc.has(k)) return true;
    return false;
  }

  private static boolean nosnikZaklecia(ItemStack i) {
    return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer()
        .has(new NamespacedKey("zaklecia", "nosnik"));
  }

  private void poglaszcz(Player p, ProfilDane d, Osobnik o) {
    if (wWalce.test(p)) return;
    long teraz = System.currentTimeMillis();
    if (!pl.mcmmorpg.chowance.logika.Odstep.gotowy(efektGlaskania.getOrDefault(p.getUniqueId(), 0L), teraz,
        c.ust.glaskanieEfektOdstepS * 1000L)) return;
    efektGlaskania.put(p.getUniqueId(), teraz);
    d.ostatniePoglaszczanie.values().removeIf(t -> teraz - t >= c.ust.glaskanieOdstepS * 1000L);
    long ostatnie = d.ostatniePoglaszczanie.getOrDefault(o.id, 0L);
    boolean wiez = pl.mcmmorpg.chowance.logika.Odstep.gotowy(ostatnie, teraz, c.ust.glaskanieOdstepS * 1000L);
    if (wiez) {
      d.ostatniePoglaszczanie.put(o.id, teraz);
      druzyny.dodajWiez(o, c.ust.glaskanieWiez);
      d.brudne = true;
    }
    Przywolania.Aktywny a = przywolania.aktywny(p);
    if (a == null || !a.osobnikId.equals(o.id)) return;
    przywolania.sygnalAktywnemu(p, o, "OKRZYK");
    a.cialo.getWorld().spawnParticle(Particle.HEART, a.cialo.getLocation().add(0, 1.1, 0), 3, 0.25, 0.2, 0.25, 0);
    if (wiez) c.pasek(p, "<light_purple>♥ " + c.nazwaKolor(o) + " — więź " + o.wiez + "/" + c.ust.wiezMax);
    krok.accept(p, 2);
  }

  private void karm(Player p, ProfilDane d, Osobnik o, ItemStack it, Przedmioty.Karma k) {
    long teraz = System.currentTimeMillis();
    Long ost = karmiony.get(o.id);
    if (ost != null && teraz - ost < c.ust.karmienieOdstepS * 1000L) {
      c.pasek(p, "<gray>" + c.nazwa(o) + " jest jeszcze najedzony (" + ((c.ust.karmienieOdstepS * 1000L - (teraz - ost)) / 1000 + 1) + " s).");
      return;
    }
    karmiony.values().removeIf(t -> teraz - t >= c.ust.karmienieOdstepS * 1000L);
    karmiony.put(o.id, teraz);
    it.setAmount(it.getAmount() - 1);
    druzyny.dodajWiez(o, k.wiez());
    d.brudne = true;
    var cialo = przywolania.aktywny(p).cialo;
    cialo.getWorld().spawnParticle(Particle.HEART, cialo.getLocation().add(0, 1.1, 0), 4, 0.3, 0.2, 0.3, 0);
    p.playSound(cialo.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.1f);
    c.pasek(p, "<light_purple>♥ " + c.nazwa(o) + " — więź " + o.wiez + "/" + c.ust.wiezMax + (k.exp() > 0 ? " <gray>+" + k.exp() + " exp" : ""));
    if (k.exp() > 0) druzyny.dajExp(p, d, o, k.exp());
    krok.accept(p, 2);
  }

  /** PPM eliksirem w powietrze: aktywny, a gdy go nie ma — pierwszy ranny/zemdlony z drużyny. */
  @EventHandler(priority = EventPriority.HIGH)
  public void eliksirWPowietrze(PlayerInteractEvent e) {
    if (e.getHand() != EquipmentSlot.HAND) return;
    if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    PrzedmiotMmo.Id id = PrzedmiotMmo.z(e.getItem());
    if (id == null) return;
    Przedmioty.Eliksir el = c.rej.przedmioty().eliksiry().get(id.id());
    if (el == null || !el.typMmoitems().equals(id.typ())) return;
    e.setCancelled(true);
    Player p = e.getPlayer();
    if (wWalce.test(p)) { c.blad(p, "W walce turowej użyj przycisku „Przedmiot”."); return; }
    ProfilDane d = c.daneLubBlad(p);
    if (d == null) return;
    Osobnik cel = przywolania.osobnikAktywny(p);
    if (cel == null) {
      for (Osobnik o : d.druzyna) {
        boolean zem = druzyny.zemdlony(p, o);
        if ((el.wskrzesza() && zem) || (!el.wskrzesza() && !zem && druzyny.hp(p, o) < druzyny.hpMax(o))) { cel = o; break; }
      }
    }
    if (cel == null) { c.pasek(p, "<gray>Nikt w drużynie nie potrzebuje tego eliksiru."); return; }
    uzyjEliksiru(p, d, cel, e.getItem(), el);
  }

  /** Zwraca true, gdy eliksir zadziałał (i został zużyty). */
  public boolean uzyjEliksiru(Player p, ProfilDane d, Osobnik o, ItemStack it, Przedmioty.Eliksir el) {
    boolean zem = druzyny.zemdlony(p, o);
    if (zem && !el.wskrzesza()) { c.blad(p, c.nazwa(o) + " jest wyczerpany — potrzebny eliksir wskrzeszenia."); return false; }
    int max = druzyny.hpMax(o);
    double hp = druzyny.hp(p, o);
    if (!zem && hp >= max && !el.leczyStatus()) { c.pasek(p, "<gray>" + c.nazwa(o) + " jest w pełni sił."); return false; }
    if (it != null) it.setAmount(it.getAmount() - 1);
    double nowe = Math.min(max, (zem ? 0 : hp) + max * el.leczenieProcent() / 100.0);
    druzyny.ustawHp(o, Math.max(nowe, zem ? 1 : 0));
    przywolania.odswiezZdrowie(p, o);
    d.brudne = true;
    p.playSound(p.getLocation(), Sound.ENTITY_WITCH_DRINK, 0.8f, 1.3f);
    c.pasek(p, "<green>" + c.nazwa(o) + ": " + (int) Math.round(druzyny.hp(p, o)) + "/" + max + " HP");
    return true;
  }

  public void wyczysc(UUID gracz) { efektGlaskania.remove(gracz); podpowiedzBron.remove(gracz); ostatniePpm.remove(gracz); }
}
