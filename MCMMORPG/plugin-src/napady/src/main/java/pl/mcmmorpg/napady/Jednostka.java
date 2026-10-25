package pl.mcmmorpg.napady;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

/**
 * Postac napadu = model (pozycja w arenie, HP, rozkaz), niezalezny od ciala. Gdy napad jest
 * zmaterializowany, model ma cialo MythicMobs i co tik przepisuje z niego pozycje i HP; bez ciala
 * model liczy {@link Symulacja}. Dowodca czyta i rozkazuje tylko modelowi, wiec obie drogi graja tak samo.
 */
final class Jednostka {
  enum Rola { WOJOWNIK, LUCZNIK }

  final Napad napad;
  final int id;
  final Strona strona;
  final Rola rola;
  final double postX, postZ;      // posterunek obroncy / punkt startu napastnika
  final boolean naMurze;
  /** miejsce w szyku swojej strony i roli (0..n-1) */
  final int slot;
  /** skora LD (indeks w skory.yml): ta sama po kazdej materializacji */
  final int skora;
  /** przedmioty MMOItems roli i liczby walki z nich (wspolne dla jednostek tej roli) */
  final Wyposazenie.Zestaw zestaw;

  // model (wspolrzedne wzgledem rogu areny)
  double x, z, yaw;
  double hp, maxHp;
  boolean zywa = true;
  int gotowyOd;                   // tik, od ktorego moze uderzyc/strzelic (symulacja i brama)
  boolean naciaga;                // lucznik w symulacji ma cel i naciaga luk (pierwsza strzala po luk-naciag)
  String akcja = "stoi";
  boolean ranny;

  // cel od dowodcy (gracz tylko w napadzie z cialami)
  Jednostka cel;
  Player celGracz;

  // rozkaz dowodcy: punkt marszu (NaN = brak) z promieniem dojscia
  double px = Double.NaN, pz = Double.NaN, pr;

  /** cialo MM albo null (symulacja) */
  Mob cialo;
  /** ostatni punkt (posredni), do ktorego wyslano pathfinder, i kiedy; ruchDo = punkt docelowy rozkazu */
  double ruchX = Double.NaN, ruchZ = Double.NaN, ruchDoX = Double.NaN, ruchDoZ = Double.NaN;
  int ruchOd, ruchWstrzymanyDo;

  Jednostka(Napad napad, int id, Strona strona, Rola rola, double x, double z, boolean naMurze, int slot, Wyposazenie.Zestaw zestaw, int skora) {
    this.napad = napad; this.id = id; this.strona = strona; this.rola = rola;
    this.postX = x; this.postZ = z; this.naMurze = naMurze; this.slot = slot; this.skora = skora; this.zestaw = zestaw;
    this.x = x + 0.5; this.z = z + 0.5; this.hp = zestaw.zycie(); this.maxHp = zestaw.zycie();
    this.yaw = strona == Strona.ATAK ? -90 : 90;
  }

  double hpUlamek() { return maxHp <= 0 ? 0 : hp / maxHp; }

  boolean lucznik() { return rola == Rola.LUCZNIK; }

  double odl(double ox, double oz) { return Math.hypot(ox - x, oz - z); }
}
