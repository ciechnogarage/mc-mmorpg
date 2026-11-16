package pl.mcmmorpg.chowance.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Jeden konkretny stwór gracza. Klasa-worek pod Gson (pola publiczne, konstruktor bezargumentowy),
 * cała logika siedzi w pl.mcmmorpg.chowance.logika — tu tylko stan.
 *
 * <p>{@code hp} to bieżące zdrowie w punktach. {@code hpCzas} to chwila ostatniego ustawienia hp,
 * od której liczy się regeneracja poza walką (leniwie, bez zadań co tick — patrz Statystyki#regeneruj).
 */
public final class Osobnik {
  public String id = UUID.randomUUID().toString();
  public String gatunek;
  public String przydomek;
  public int poziom = 1;
  /** Łączny exp (nie „od początku poziomu”). */
  public long exp;
  public int[] geny = new int[Stat.values().length];
  public String charakter;
  public String cecha;
  public List<String> ruchy = new ArrayList<>();
  public int wiez;
  public boolean lsniacy;
  public String pochodzenie;
  public String pieczec;
  public long zlapany;
  public String pierwszyWlasciciel;
  public double hp = -1;
  public long hpCzas;
  public boolean zwiazany;
  public boolean siodlo;

  public Osobnik kopia() {
    Osobnik o = new Osobnik();
    o.id = id; o.gatunek = gatunek; o.przydomek = przydomek; o.poziom = poziom; o.exp = exp;
    o.geny = geny.clone(); o.charakter = charakter; o.cecha = cecha; o.ruchy = new ArrayList<>(ruchy);
    o.wiez = wiez; o.lsniacy = lsniacy; o.pochodzenie = pochodzenie; o.pieczec = pieczec;
    o.zlapany = zlapany; o.pierwszyWlasciciel = pierwszyWlasciciel; o.hp = hp; o.hpCzas = hpCzas;
    o.zwiazany = zwiazany; o.siodlo = siodlo;
    return o;
  }

  public int sumaGenow() {
    int s = 0;
    for (int g : geny) s += g;
    return s;
  }
}
