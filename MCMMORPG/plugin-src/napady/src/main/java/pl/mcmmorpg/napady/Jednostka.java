package pl.mcmmorpg.napady;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Mob;

/**
 * Postac napadu. Cialo to manekin (bez AI, ruch liczy napad) albo mob MythicMobs (AI vanilla,
 * napad tylko dowodzi: cel, punkt marszu, odwrot rannych).
 */
final class Jednostka {
  enum Rola { WOJOWNIK, LUCZNIK }

  final int id;
  final Strona strona;
  final Rola rola;
  final LivingEntity cialo;
  final double postX, postZ;      // posterunek obroncy / punkt startu napastnika
  final boolean naMurze;
  /** miejsce w szyku swojej strony i roli (0..n-1) */
  final int slot;
  LivingEntity cel;
  int gotowyOd;                   // tik, od ktorego moze uderzyc/strzelic
  String akcja = "stoi";
  boolean ranny;
  /** ostatni punkt, do ktorego wyslano pathfinder (tylko cialo MM) */
  double ruchX = Double.NaN, ruchZ = Double.NaN;
  int ruchOd;

  Jednostka(int id, Strona strona, Rola rola, LivingEntity cialo, double postX, double postZ, boolean naMurze, int slot) {
    this.id = id; this.strona = strona; this.rola = rola; this.cialo = cialo;
    this.postX = postX; this.postZ = postZ; this.naMurze = naMurze; this.slot = slot;
  }

  boolean zywa() { return cialo.isValid() && !cialo.isDead(); }

  /** true = cialo MM z wlasnym AI */
  boolean mm() { return !(cialo instanceof Mannequin) && cialo instanceof Mob; }

  double hpUlamek() {
    var a = cialo.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
    return a == null ? 1 : cialo.getHealth() / a.getValue();
  }
}
