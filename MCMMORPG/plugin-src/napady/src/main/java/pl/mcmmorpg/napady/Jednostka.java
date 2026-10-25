package pl.mcmmorpg.napady;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;

/** Postac napadu: manekin (bez AI) sterowany przez napad. */
final class Jednostka {
  enum Rola { WOJOWNIK, LUCZNIK }

  final int id;
  final Strona strona;
  final Rola rola;
  final Mannequin cialo;
  final double postX, postZ;      // posterunek obroncy / punkt startu napastnika
  final boolean naMurze;
  LivingEntity cel;
  int gotowyOd;                   // tik, od ktorego moze uderzyc/strzelic
  String akcja = "stoi";

  Jednostka(int id, Strona strona, Rola rola, Mannequin cialo, double postX, double postZ, boolean naMurze) {
    this.id = id; this.strona = strona; this.rola = rola; this.cialo = cialo;
    this.postX = postX; this.postZ = postZ; this.naMurze = naMurze;
  }

  boolean zywa() { return cialo.isValid() && !cialo.isDead(); }
}
