package pl.mcmmorpg.chowance.logika;

import pl.mcmmorpg.chowance.model.Cecha;
import pl.mcmmorpg.chowance.model.Charakter;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.RodzajCechy;
import pl.mcmmorpg.chowance.model.Stat;
import pl.mcmmorpg.chowance.model.StatEtapu;
import pl.mcmmorpg.chowance.model.Status;
import pl.mcmmorpg.chowance.tresc.Rejestr;

/** Stan stwora w walce (turowej albo real-time). Osobnik jest źródłem; hp wraca do niego po walce. */
public final class Wojownik {
  public final Osobnik o;
  public final Gatunek g;
  public final Cecha cecha;
  public final int[] staty;
  public final int hpMax;
  public double hp;
  public Status status;
  public int statusCzas;
  public final int[] etapy = new int[StatEtapu.values().length];
  /** Walka turowa: ruch -> ile tur jeszcze się odnawia (mocne ruchy nie co turę — recenzja R-07). */
  public final java.util.Map<String, Integer> odnowienieTur = new java.util.HashMap<>();
  /** Ile razy brał udział w walce (do podziału exp). */
  public boolean walczyl;
  /** NOC/DZIEN/DESZCZ/BURZA/W_WODZIE — ustawia warstwa gry z położenia ciała (cechy z `gdy`). */
  public java.util.Set<String> srodowisko = java.util.Set.of();

  public Wojownik(Rejestr r, Osobnik o, double premiaWiezi, double hpStart) {
    this.o = o;
    this.g = r.gatunek(o.gatunek);
    this.cecha = r.cecha(o.cecha);
    Charakter ch = r.charakter(o.charakter);
    this.staty = Statystyki.wszystkie(g, o, ch, premiaWiezi, cecha);
    int mn = staty[Stat.ZDROWIE.ordinal()];
    this.hpMax = mn;
    this.hp = Math.max(0, Math.min(mn, hpStart));
  }

  public boolean zemdlony() { return hp <= 0; }

  public String nazwa() {
    return o.przydomek != null && !o.przydomek.isBlank() ? o.przydomek : g.nazwa();
  }

  public boolean ma(RodzajCechy r) { return cecha != null && cecha.rodzaj() == r; }

  public void resetEtapow() { java.util.Arrays.fill(etapy, 0); }

  public double hpProcent() { return hpMax <= 0 ? 0 : hp * 100.0 / hpMax; }
}
