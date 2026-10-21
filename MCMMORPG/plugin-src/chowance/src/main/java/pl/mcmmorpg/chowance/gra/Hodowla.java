package pl.mcmmorpg.chowance.gra;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.api.event.ChowaniecZlapanyEvent;
import pl.mcmmorpg.chowance.dane.Jajo;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.logika.Dziedziczenie;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;

/**
 * Hodowla (SPEC §7): dwa stwory w zagrodzie profilu → co hodowla.czas_min jajo (max hodowla.jaja_max)
 * → wykluwa się po przejściu dystansu gatunku. Dystans liczony próbkowaniem pozycji co
 * hodowla.probkowanie_s (tylko gracze z jajami), teleporty i przebiegi > 12 bl/s odrzucane.
 */
public final class Hodowla {
  private final Chowance c;
  private final Druzyny druzyny;
  private final Przywolania przywolania;
  private final Map<UUID, Location> ostatnia = new ConcurrentHashMap<>();

  public Hodowla(Chowance c, Druzyny druzyny, Przywolania przywolania) {
    this.c = c; this.druzyny = druzyny; this.przywolania = przywolania;
  }

  public String wloz(Player p, ProfilDane d, Osobnik o) {
    if (!d.druzyna.contains(o) && !d.magazyn.contains(o)) return "Nie masz takiego stwora.";
    if (d.hodowlaA != null && d.hodowlaB != null) return "Zagroda jest pełna (2).";
    var akt = przywolania.aktywny(p);
    if (akt != null && akt.osobnikId.equals(o.id)) przywolania.odwolaj(p, true);
    Gatunek g = c.gatunek(o);
    if (g == null || !g.plodny()) return c.nazwa(o) + " nie może mieć potomstwa.";
    d.druzyna.remove(o);
    d.magazyn.remove(o);
    if (d.hodowlaA == null) d.hodowlaA = o; else d.hodowlaB = o;
    d.hodowlaStart = d.hodowlaA != null && d.hodowlaB != null ? System.currentTimeMillis() : 0;
    d.brudne = true;
    return null;
  }

  public String wyjmij(Player p, ProfilDane d, int ktory) {
    Osobnik o = ktory == 1 ? d.hodowlaA : d.hodowlaB;
    if (o == null) return "Ten boks zagrody jest pusty.";
    if (druzyny.dodaj(p, d, o, false) == Druzyny.Gdzie.BRAK_MIEJSCA) return "Brak miejsca w drużynie i magazynie.";
    if (ktory == 1) { d.hodowlaA = d.hodowlaB; d.hodowlaB = null; } else d.hodowlaB = null;
    d.hodowlaStart = 0;
    d.brudne = true;
    return null;
  }

  public String niezgodnosc(ProfilDane d) {
    if (d.hodowlaA == null || d.hodowlaB == null) return "W zagrodzie muszą być dwa stwory.";
    Gatunek a = c.gatunek(d.hodowlaA), b = c.gatunek(d.hodowlaB);
    if (a == null || b == null) return "Nieznany gatunek w zagrodzie.";
    return Dziedziczenie.niezgodnosc(d.hodowlaA, d.hodowlaB, a, b);
  }

  /** Postęp do następnego jaja 0..100 (0, gdy para niezgodna). */
  public int postep(ProfilDane d) {
    if (niezgodnosc(d) != null || d.hodowlaStart <= 0) return 0;
    long ms = c.ust.hodowlaCzasMin * 60_000L;
    return (int) Math.min(100, (System.currentTimeMillis() - d.hodowlaStart) * 100 / Math.max(1, ms));
  }

  /** Składa zaległe jaja (leniwie: przy komendzie i co minutę dla graczy online). */
  public void sprawdzJaja(Player p, ProfilDane d) {
    if (niezgodnosc(d) != null || d.hodowlaStart <= 0) return;
    long ms = c.ust.hodowlaCzasMin * 60_000L;
    long teraz = System.currentTimeMillis();
    while (teraz - d.hodowlaStart >= ms && d.jaja.size() < c.ust.jajaMax) {
      Gatunek a = c.gatunek(d.hodowlaA), b = c.gatunek(d.hodowlaB);
      Osobnik m = Dziedziczenie.mlode(c.rej, d.hodowlaA, d.hodowlaB, a, b, c.ust.genyDziedziczone,
          c.ust.szansaLsniacego, c.ust.mnoznikLsniacegoObcy, ThreadLocalRandom.current());
      Jajo j = new Jajo();
      j.mlode = m;
      Gatunek gm = c.gatunek(m);
      j.wymagane = gm == null ? 1500 : gm.dystansJaja();
      j.zlozone = teraz;
      d.jaja.add(j);
      d.hodowlaStart += ms;
      d.brudne = true;
      c.msg(p, "<light_purple>W zagrodzie pojawiło się jajo! <gray>Wykluje się po " + j.wymagane + " blokach marszu.");
    }
    if (d.jaja.size() >= c.ust.jajaMax) d.hodowlaStart = teraz; // pełna wylęgarnia wstrzymuje licznik
  }

  public void probkuj() {
    for (Player p : Bukkit.getOnlinePlayers()) {
      ProfilDane d = c.dane(p);
      if (d == null || d.jaja.isEmpty()) { ostatnia.remove(p.getUniqueId()); continue; }
      Location teraz = p.getLocation();
      Location przed = ostatnia.put(p.getUniqueId(), teraz.clone());
      if (przed == null || !przed.getWorld().equals(teraz.getWorld()) || p.isInsideVehicle() || p.isFlying()) continue;
      double dx = teraz.getX() - przed.getX(), dz = teraz.getZ() - przed.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      if (dist > 12.0 * c.ust.probkowanieS) continue; // teleport albo coś nienaturalnego
      if (dist < 0.5) continue;
      List<Jajo> gotowe = new ArrayList<>();
      for (Jajo j : d.jaja) {
        j.przebyte += dist;
        if (j.przebyte >= j.wymagane) gotowe.add(j);
      }
      d.brudne = true;
      for (Jajo j : gotowe) wyklucie(p, d, j);
    }
  }

  private void wyklucie(Player p, ProfilDane d, Jajo j) {
    Osobnik m = j.mlode;
    if (druzyny.dodaj(p, d, m, true) == Druzyny.Gdzie.BRAK_MIEJSCA) {
      c.pasek(p, "<red>Jajo jest gotowe, ale nie masz miejsca w drużynie ani magazynie!");
      return;
    }
    d.jaja.remove(j);
    p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0);
    p.playSound(p.getLocation(), Sound.ENTITY_TURTLE_EGG_HATCH, 1f, 1.2f);
    c.msg(p, "<light_purple>Z jaja wykluwa się " + c.nazwaKolor(m) + "!");
    c.mmocore.dajExp(p, c.ust.expWyklucie);
    Bukkit.getPluginManager().callEvent(new ChowaniecZlapanyEvent(p, m, "JAJO"));
    c.zdarzenie("WYKLUCIE", Map.of("gracz", p.getName(), "gatunek", m.gatunek, "lsniacy", m.lsniacy ? "tak" : "nie"));
    c.dane.zapisz(d);
  }

  public void zapomnij(UUID p) { ostatnia.remove(p); }
}
