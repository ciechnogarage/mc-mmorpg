package pl.mcmmorpg.chowance.integracja;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.api.skills.targeters.IEntityTargeter;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.gra.Druzyny;
import pl.mcmmorpg.chowance.gra.Dzikie;
import pl.mcmmorpg.chowance.gra.Lapanie;
import pl.mcmmorpg.chowance.gra.Pojedynki;
import pl.mcmmorpg.chowance.gra.Przywolania;
import pl.mcmmorpg.chowance.gra.WalkiTurowe;
import pl.mcmmorpg.chowance.logika.Obrazenia;
import pl.mcmmorpg.chowance.logika.SilnikWalki;
import pl.mcmmorpg.chowance.logika.Wojownik;
import pl.mcmmorpg.chowance.model.Kategoria;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Przedmioty;
import pl.mcmmorpg.chowance.model.Ruch;
import pl.mcmmorpg.chowance.model.Status;
import pl.mcmmorpg.chowance.model.Typ;

/**
 * Mechaniki, warunki i targetery MythicMobs (KONTRAKT §10). Wzór rejestracji z oficjalnego repo przykładów
 * API (github.com/xikage/MythicMobs-API-Examples): listener na Mythic*LoadEvent + event.register(...).
 * MythicMobs 5.11.2 dla nieznanej nazwy tworzy CustomMechanic, który odpala LoadEvent przez
 * Schedulers.sync().runLater (bajtkod CustomMechanic) — czyli już po włączeniu wszystkich pluginów.
 */
public final class MythicRejestracja implements Listener {
  private final Chowance c;
  private final Przywolania przywolania;
  private final Dzikie dzikie;
  private final Druzyny druzyny;
  private final Lapanie lapanie;
  private final WalkiTurowe walki;
  private final Pojedynki pojedynki;
  public int zarejestrowane;

  public MythicRejestracja(Chowance c, Przywolania przywolania, Dzikie dzikie, Druzyny druzyny, Lapanie lapanie,
      WalkiTurowe walki, Pojedynki pojedynki) {
    this.c = c; this.przywolania = przywolania; this.dzikie = dzikie; this.druzyny = druzyny;
    this.lapanie = lapanie; this.walki = walki; this.pojedynki = pojedynki;
  }

  // ------------------------------------------------------------------ mechaniki

  /**
   * Każda nasza mechanika rusza świat (damage, PDC, gracze), więc MUSI iść na wątku głównym.
   * MythicMobs 5.11.2 domyślnie wykonuje mechaniki także asynchronicznie — bez SYNC_ONLY `damage()` rzucało
   * „EntityDamageByEntityEvent may only be triggered synchronously”, a MM połykał wyjątek (PROBLEMY A-012).
   */
  private interface Synchroniczna extends ITargetedEntitySkill {
    @Override
    default io.lumine.mythic.api.skills.ThreadSafetyLevel getThreadSafetyLevel() {
      return io.lumine.mythic.api.skills.ThreadSafetyLevel.SYNC_ONLY;
    }
  }

  private Synchroniczna sync(ITargetedEntitySkill m) {
    return (data, t) -> {
      try {
        return m.castAtEntity(data, t);
      } catch (RuntimeException ex) {
        zglos("mechanika", ex);
        return SkillResult.ERROR;
      }
    };
  }

  @EventHandler
  public void mechanika(MythicMechanicLoadEvent e) {
    String n = e.getMechanicName().toLowerCase(Locale.ROOT);
    MythicLineConfig cfg = e.getConfig();
    ITargetedEntitySkill m = switch (n) {
      case "chwobrazenia" -> new Obr(cfg);
      case "chwruchauto" -> new RuchMech(0);
      case "chwruch" -> new RuchMech(Math.max(1, Math.min(4, cfg.getInteger(new String[] {"slot", "s"}, 1))));
      case "chwstatus" -> (data, t) -> {
        Status s = Status.parse(cfg.getString(new String[] {"status", "s"}, "SEN"));
        int czas = cfg.getInteger(new String[] {"czas", "t"}, 5);
        Entity cel = BukkitAdapter.adapt(t);
        if (s == null || !dzikie.dziki(cel)) return SkillResult.INVALID_TARGET;
        dzikie.nalozStatus(cel, s, czas);
        return SkillResult.SUCCESS;
      };
      case "chwlap" -> (data, t) -> {
        Entity caster = data.getCaster().getEntity().getBukkitEntity();
        Entity cel = BukkitAdapter.adapt(t);
        Przedmioty.Pieczec pc = c.rej.przedmioty().pieczecie().get(cfg.getString(new String[] {"pieczec", "p"}, ""));
        if (!(caster instanceof Player p) || !(cel instanceof LivingEntity le) || pc == null || !dzikie.dziki(le)) return SkillResult.INVALID_TARGET;
        lapanie.proba(p, le, pc, null);
        return SkillResult.SUCCESS;
      };
      case "chwwalkatrener" -> (data, t) -> {
        if (!(BukkitAdapter.adapt(t) instanceof Player p)) return SkillResult.INVALID_TARGET;
        walki.startTrener(p, cfg.getString(new String[] {"t", "trener"}, ""), null, 0);
        return SkillResult.SUCCESS;
      };
      case "chwleczdruzyne" -> (data, t) -> {
        if (!(BukkitAdapter.adapt(t) instanceof Player p) || c.dane(p) == null) return SkillResult.INVALID_TARGET;
        druzyny.leczWszystkich(p, c.dane(p), cfg.getDouble(new String[] {"procent", "p"}, 100));
        return SkillResult.SUCCESS;
      };
      default -> null;
    };
    if (m != null) { e.register(m instanceof Synchroniczna sy ? sy : sync(m)); zarejestrowane++; c.debug("MM mechanika " + n); }
  }

  /** chwObrazenia{moc;typ;kategoria} — obrażenia wg silnika (typy, staty, geny, cechy). */
  private final class Obr implements Synchroniczna {
    final int moc;
    final Typ typ;
    final Kategoria kat;

    Obr(MythicLineConfig cfg) {
      moc = cfg.getInteger(new String[] {"moc", "m"}, 40);
      Typ t = Typ.parse(cfg.getString(new String[] {"typ", "t"}, "BESTIA"));
      typ = t == null ? Typ.BESTIA : t;
      Kategoria k = Kategoria.parse(cfg.getString(new String[] {"kategoria", "k"}, "FIZYCZNY"));
      kat = k == null || k == Kategoria.STATUS ? Kategoria.FIZYCZNY : k;
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
      try {
        return wykonaj(data, target);
      } catch (RuntimeException ex) {
        zglos("chwObrazenia", ex);
        return SkillResult.ERROR;
      }
    }

    private SkillResult wykonaj(SkillMetadata data, AbstractEntity target) {
      Entity caster = data.getCaster().getEntity().getBukkitEntity();
      Entity cel = BukkitAdapter.adapt(target);
      if (!(cel instanceof LivingEntity le) || cel.equals(caster) || poza(caster, cel)) return SkillResult.INVALID_TARGET;
      // W walce turowej i pojedynku obrażenia liczy wyłącznie tamten silnik.
      if (walki.cialoWWalce(caster) || walki.cialoWWalce(cel) || pojedynki.cialoWPojedynku(caster) || pojedynki.cialoWPojedynku(cel)) return SkillResult.CONDITION_FAILED;
      Wojownik at = wojownik(caster);
      Wojownik ob = wojownik(cel);
      Ruch r = new Ruch("MM", "MM", "", typ, kat, moc, 0, 0, 1, false, List.of(), "");
      double dmg;
      if (at != null && ob != null) {
        dmg = SilnikWalki.obrazenia(c.rej, at, ob, r, ThreadLocalRandom.current()).obrazenia();
      } else {
        int lvlA = at != null ? at.o.poziom : poziomMm(caster);
        double a = at != null ? at.staty[kat == Kategoria.FIZYCZNY ? 1 : 3] : 5 + lvlA * 1.5;
        double d = ob != null ? ob.staty[kat == Kategoria.FIZYCZNY ? 2 : 4] : 5 + poziomMm(cel) * 1.5;
        boolean stab = at != null && at.g.typy().contains(typ);
        if (ob == null && cel instanceof Player gp) {
          dmg = wGracza(lvlA, moc, a, stab, gp);
        } else {
          int baza = Obrazenia.bazowe(lvlA, moc, a, d);
          double typM = ob != null ? c.rej.typy().mnoznik(typ, ob.g.typy()) : 1.0;
          dmg = Obrazenia.koncowe(baza, stab ? c.rej.typy().stab() : 1.0, typM, false, 0.85 + ThreadLocalRandom.current().nextDouble() * 0.15, 1.0);
          if (ob == null) dmg *= c.ust.pveMnoznikNiechowance;
        }
      }
      if (dmg <= 0) return SkillResult.SUCCESS;
      le.setNoDamageTicks(0);
      le.damage(dmg, caster);
      return SkillResult.SUCCESS;
    }
  }

  /** Odnowienia ruchów w walce towarzysza: ciało -> ruch -> chwila gotowości. Czyszczone przy usunięciu ciała. */
  private final java.util.Map<java.util.UUID, java.util.Map<String, Long>> odnowienia = new java.util.concurrent.ConcurrentHashMap<>();

  public void zapomnij(java.util.UUID cialo) { odnowienia.remove(cialo); }

  @EventHandler
  public void usuniete(com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent e) { odnowienia.remove(e.getEntity().getUniqueId()); }

  /**
   * chwRuchAuto{} / chwRuch{slot=N} (PROBLEMY B-002): ciało używa ruchów SWOJEGO osobnika w walce
   * real-time PvE — odnowienia z ruchy/*.yml, obrażenia i efekty wg silnika, skill ruchu jako wizualia.
   * Auto: najwyższa oczekiwana wartość spośród gotowych (moc × celność × typ × STAB).
   * Pomijane w walce turowej i pojedynku (tam ruchy wybiera gracz).
   */
  private final class RuchMech implements Synchroniczna {
    final int slot;

    RuchMech(int slot) { this.slot = slot; }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
      try {
        return wykonaj(data, target);
      } catch (RuntimeException ex) {
        zglos("chwRuch", ex);
        return SkillResult.ERROR;
      }
    }

    private SkillResult wykonaj(SkillMetadata data, AbstractEntity target) {
      Entity caster = data.getCaster().getEntity().getBukkitEntity();
      Entity cel = BukkitAdapter.adapt(target);
      c.debug("chwRuch slot=" + slot + " caster=" + caster.getType() + "/" + caster.getUniqueId() + " cel=" + (cel == null ? null : cel.getType() + "/" + cel.getUniqueId()));
      if (!(cel instanceof LivingEntity le) || cel.equals(caster) || !(caster instanceof LivingEntity lc) || poza(caster, cel)) return SkillResult.INVALID_TARGET;
      if (walki.cialoWWalce(caster) || pojedynki.cialoWPojedynku(caster) || walki.cialoWWalce(cel) || pojedynki.cialoWPojedynku(cel)) return SkillResult.CONDITION_FAILED;
      // w trakcie wiązania pieczęcią nikt nie walczy: ani łapany, ani nikt w niego
      if (dzikie.zajete.contains(caster.getUniqueId()) || dzikie.zajete.contains(cel.getUniqueId())) return SkillResult.CONDITION_FAILED;
      Wojownik at = wojownik(caster);
      if (at == null) { c.debug("chwRuch: caster nie jest chowańcem"); return SkillResult.INVALID_TARGET; }
      // Ranny chowaniec (< 25% HP) wycofuje się do trenera zamiast walczyć do omdlenia (recenzja R-12).
      if (przywolania.oswojoneCialo(caster) && lc.getHealth() < maxHp(lc) * 0.25) {
        Player wl = przywolania.wlascicielCiala(caster);
        if (lc instanceof org.bukkit.entity.Mob m) { m.setTarget(null); if (wl != null) m.getPathfinder().moveTo(wl, 1.5); }
        long t = System.currentTimeMillis();
        if (wl != null && t - wycofanie.getOrDefault(wl.getUniqueId(), 0L) > 10_000) {
          wycofanie.put(wl.getUniqueId(), t);
          c.pasek(wl, "<yellow>" + at.nazwa() + " jest ranny i wycofuje się — ulecz go eliksirem albo odwołaj.");
        }
        return SkillResult.CONDITION_FAILED;
      }
      Wojownik ob = wojownik(cel);
      long teraz = System.currentTimeMillis();
      var mapa = odnowienia.computeIfAbsent(caster.getUniqueId(), k -> new java.util.concurrent.ConcurrentHashMap<>());
      Ruch wybrany = null;
      // Ruch kontaktowy tylko z bliska (wcześniej „ugryzienie” trafiało z 16 bl. — log klonu 2026-09-29).
      double dystans = lc.getWorld().equals(le.getWorld()) ? lc.getLocation().distance(le.getLocation()) : 99;
      if (slot > 0) {
        if (slot > at.o.ruchy.size()) return SkillResult.CONDITION_FAILED;
        Ruch r = c.rej.ruch(at.o.ruchy.get(slot - 1));
        if (r != null && mapa.getOrDefault(r.id(), 0L) <= teraz && (!r.kontakt() || dystans <= ZASIEG_KONTAKTU)) wybrany = r;
      } else {
        double best = -1;
        for (String id : at.o.ruchy) {
          Ruch r = c.rej.ruch(id);
          if (r == null || mapa.getOrDefault(r.id(), 0L) > teraz) continue;
          if (r.kontakt() && dystans > ZASIEG_KONTAKTU) continue;
          double v = r.kategoria() == Kategoria.STATUS ? 15
              : r.moc() * (r.zawszeTrafia() ? 1 : r.celnosc() / 100.0) * (ob == null ? 1 : c.rej.typy().mnoznik(r.typ(), ob.g.typy()))
                * (at.g.typy().contains(r.typ()) ? c.rej.typy().stab() : 1.0);
          v *= 0.85 + ThreadLocalRandom.current().nextDouble() * 0.3;
          if (v > best) { best = v; wybrany = r; }
        }
      }
      if (wybrany == null) { c.debug("chwRuch: brak gotowego ruchu, ruchy=" + at.o.ruchy); return SkillResult.CONDITION_FAILED; }
      mapa.put(wybrany.id(), teraz + Math.round(wybrany.odnowienie() * 1000));
      if (!wybrany.skill().isBlank()) Mythic.skill(caster, wybrany.skill(), cel);
      boolean trafil = wybrany.zawszeTrafia() || ThreadLocalRandom.current().nextDouble() * 100 < wybrany.celnosc()
          * (ob != null && ob.ma(pl.mcmmorpg.chowance.model.RodzajCechy.UNIK) ? 1 - ob.cecha.d("procent", 0) / 100.0 : 1.0);
      if (!trafil) { c.debug("chwRuch " + wybrany.id() + ": pudło"); return SkillResult.SUCCESS; }
      double zadane = 0;
      if (wybrany.kategoria() != Kategoria.STATUS) {
        if (ob != null) {
          zadane = SilnikWalki.obrazenia(c.rej, at, ob, wybrany, ThreadLocalRandom.current()).obrazenia();
        } else if (cel instanceof Player gp) {
          double a = at.staty[wybrany.kategoria() == Kategoria.FIZYCZNY ? 1 : 3];
          zadane = wGracza(at.o.poziom, wybrany.moc(), a, at.g.typy().contains(wybrany.typ()), gp);
        } else {
          double a = at.staty[wybrany.kategoria() == Kategoria.FIZYCZNY ? 1 : 3];
          int baza = Obrazenia.bazowe(at.o.poziom, wybrany.moc(), a, 5 + poziomMm(cel) * 1.5);
          zadane = Obrazenia.koncowe(baza, at.g.typy().contains(wybrany.typ()) ? c.rej.typy().stab() : 1.0, 1.0, false,
              0.85 + ThreadLocalRandom.current().nextDouble() * 0.15, 1.0) * c.ust.pveMnoznikNiechowance;
        }
        double hpPrzed = le.getHealth();
        // klatki nietykalności po ciosie gracza połykały cios chowańca w tym samym ticku (klon 2026-09-29)
        if (zadane > 0) { le.setNoDamageTicks(0); le.damage(zadane, caster); }
        // Dziki bity przez chowańca nie ignoruje go: z szansą przerzuca się z gracza na chowańca (wcześniej chowaniec
        // walczył bez ryzyka — pomiar 2026-09-29: 0 obrażeń chowańca w całej walce).
        if (zadane > 0 && przywolania.oswojoneCialo(caster) && dzikie.dziki(cel) && cel instanceof org.bukkit.entity.Mob m
            && !(m.getTarget() != null && m.getTarget().equals(caster)) && ThreadLocalRandom.current().nextDouble() < c.ust.dzikiZmianaCelu) {
          m.setTarget(lc);
        }
        c.debug("chwRuch " + wybrany.id() + " at=" + at.o.gatunek + "/" + at.o.poziom + " ob=" + (ob == null ? "-" : ob.o.gatunek) + " dmg=" + zadane + " hp " + hpPrzed + "->" + le.getHealth());
      }
      for (pl.mcmmorpg.chowance.model.EfektRuchu ef : wybrany.efekty()) {
        if (ThreadLocalRandom.current().nextDouble() * 100 >= ef.szansa()) continue;
        switch (ef.rodzaj()) {
          case STATUS -> { Entity x = ef.naSiebie() ? caster : cel; if (dzikie.dziki(x)) dzikie.nalozStatus(x, ef.status(), Math.max(1, ef.czas())); }
          case LECZENIE -> lc.setHealth(Math.min(maxHp(lc), lc.getHealth() + maxHp(lc) * ef.procent() / 100.0));
          case WYSYSANIE -> lc.setHealth(Math.min(maxHp(lc), lc.getHealth() + zadane * ef.procent() / 100.0));
          case ODRZUT -> lc.damage(Math.max(1, zadane * ef.procent() / 100.0));
          case STAT -> { /* etapy statów istnieją tylko w walce turowej i pojedynku */ }
        }
      }
      return SkillResult.SUCCESS;
    }
  }

  static final double ZASIEG_KONTAKTU = 3.5;
  private final java.util.Map<java.util.UUID, Long> wycofanie = new java.util.concurrent.ConcurrentHashMap<>();

  /**
   * Obrażenia stwora w GRACZA: jak w wirtualnego, przeciętnego stwora na poziomie postaci gracza (staty bazowe 60,
   * geny 15), przeliczone na ułamek jego HP i przyłożone do max HP gracza × dzikie_walka.obrazenia_w_gracza.
   * Wcześniej: mnożnik 0,15 do skali MC = ~0,7 HP co 2 s, zjadane przez regenerację (dziki „nie bił”).
   * Poziom ma znaczenie: dziki poz. 5 ledwo drapie postać poz. 30, a dziki poz. 20 jest groźny dla postaci poz. 5.
   */
  double wGracza(int poziomAtak, int moc, double atak, boolean stab, Player p) {
    if (moc <= 0) return 0;
    int lp = Math.max(1, Math.min(100, c.mmocore.poziomPostaci(p)));
    int obronaW = 135 * lp / 100 + 5, hpW = 135 * lp / 100 + lp + 10;
    int baza = Obrazenia.bazowe(poziomAtak, moc, atak, obronaW);
    double dmg = Obrazenia.koncowe(baza, stab ? c.rej.typy().stab() : 1.0, 1.0, false, 0.85 + ThreadLocalRandom.current().nextDouble() * 0.15, 1.0);
    var attr = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
    double max = attr == null ? 20 : attr.getValue();
    return dmg / hpW * max * c.ust.dzikiWGracza;
  }

  /** MythicMobs połyka wyjątki z mechanik — logujemy sami, raz na typ wyjątku i miejsce (bez zalewania logu). */
  private final java.util.Set<String> zgloszone = java.util.concurrent.ConcurrentHashMap.newKeySet();

  private void zglos(String gdzie, RuntimeException ex) {
    String k = gdzie + ":" + ex.getClass().getName() + ":" + (ex.getStackTrace().length > 0 ? ex.getStackTrace()[0] : "");
    if (zgloszone.add(k)) c.log.log(java.util.logging.Level.WARNING, "Wyjątek w mechanice " + gdzie + " (kolejne takie same pomijam)", ex);
  }

  /** Oswojony chowaniec poza walką turową/pojedynkiem nie atakuje graczy ani cudzych chowańców (Przywolania.swojNieBije). */
  private boolean poza(Entity caster, Entity cel) {
    return przywolania.oswojoneCialo(caster) && (cel instanceof Player || przywolania.oswojoneCialo(cel));
  }

  private int poziomMm(Entity e) {
    var am = Mythic.aktywny(e);
    return am == null ? 1 : (int) Math.max(1, am.getLevel());
  }

  /** Wojownik dla ciała: oswojony (przez właściciela) albo dziki (PDC). */
  private Wojownik wojownik(Entity e) {
    Player wl = przywolania.wlascicielCiala(e);
    if (wl != null) {
      Osobnik o = przywolania.osobnikAktywny(wl);
      if (o == null) return null;
      Wojownik w = druzyny.wojownik(wl, o);
      w.srodowisko = pl.mcmmorpg.chowance.gra.Srodowisko.z(e);
      return w;
    }
    Osobnik o = dzikie.osobnik(e);
    if (o == null || c.gatunek(o) == null) return null;
    double hp = e instanceof LivingEntity le ? le.getHealth() : 1;
    Wojownik w = new Wojownik(c.rej, o, 0, hp);
    w.srodowisko = pl.mcmmorpg.chowance.gra.Srodowisko.z(e);
    return w;
  }

  private Osobnik osobnik(Entity e) {
    Player wl = przywolania.wlascicielCiala(e);
    if (wl != null) return przywolania.osobnikAktywny(wl);
    return dzikie.osobnik(e);
  }

  // ------------------------------------------------------------------ warunki

  @EventHandler
  public void warunek(MythicConditionLoadEvent e) {
    String n = e.getConditionName().toLowerCase(Locale.ROOT);
    MythicLineConfig cfg = e.getConfig();
    Function<Entity, Boolean> f = switch (n) {
      case "chwchowaniec" -> x -> osobnik(x) != null;
      case "chwoswojony" -> x -> przywolania.wlascicielCiala(x) != null;
      case "chwdziki" -> x -> dzikie.dziki(x);
      case "chwlsniacy" -> x -> { Osobnik o = osobnik(x); return o != null && o.lsniacy; };
      case "chwgatunek" -> {
        String g = cfg.getString(new String[] {"g", "gatunek"}, "");
        yield x -> { Osobnik o = osobnik(x); return o != null && o.gatunek.equalsIgnoreCase(g); };
      }
      case "chwtyp" -> {
        Typ t = Typ.parse(cfg.getString(new String[] {"t", "typ"}, ""));
        yield x -> { Osobnik o = osobnik(x); var g = o == null ? null : c.gatunek(o); return g != null && g.typy().contains(t); };
      }
      case "chwmasiodlo" -> x -> { Osobnik o = osobnik(x); return o != null && o.siodlo; };
      case "chwwwalce" -> x -> walki.cialoWWalce(x) || pojedynki.cialoWPojedynku(x)
          || (x instanceof Player p && (walki.wWalce(p) || pojedynki.wPojedynku(p)));
      case "chwstatus" -> {
        Status s = Status.parse(cfg.getString(new String[] {"s", "status"}, ""));
        yield x -> s != null && s == dzikie.status(x);
      }
      case "chwmaaktywnego" -> x -> x instanceof Player p && przywolania.aktywny(p) != null;
      default -> null;
    };
    if (f == null) return;
    e.register((IEntityCondition) t -> {
      try {
        return Boolean.TRUE.equals(f.apply(BukkitAdapter.adapt(t)));
      } catch (RuntimeException ex) {
        return false;
      }
    });
    zarejestrowane++;
  }

  // ------------------------------------------------------------------ targetery

  @EventHandler
  public void targeter(MythicTargeterLoadEvent e) {
    String n = e.getTargeterName().toLowerCase(Locale.ROOT);
    Function<Entity, Entity> f = switch (n) {
      case "chwaktywny" -> x -> x instanceof Player p && przywolania.aktywny(p) != null ? przywolania.aktywny(p).cialo : null;
      case "chwwlasciciel" -> przywolania::wlascicielCiala;
      case "chwprzeciwnik" -> x -> { LivingEntity a = walki.przeciwnikCiala(x); return a != null ? a : pojedynki.przeciwnikCiala(x); };
      default -> null;
    };
    if (f == null) return;
    e.register((IEntityTargeter) data -> {
      Entity caster = data.getCaster().getEntity().getBukkitEntity();
      Entity cel = f.apply(caster);
      return cel == null ? List.<AbstractEntity>of() : List.of(BukkitAdapter.adapt(cel));
    });
    zarejestrowane++;
  }

  static Collection<AbstractEntity> brak() { return List.of(); }

  private static double maxHp(LivingEntity le) {
    var a = le.getAttribute(Attribute.MAX_HEALTH);
    return a == null ? le.getHealth() : a.getValue();
  }
}
