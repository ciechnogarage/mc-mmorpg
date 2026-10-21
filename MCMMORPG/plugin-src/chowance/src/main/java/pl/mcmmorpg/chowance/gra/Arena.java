package pl.mcmmorpg.chowance.gra;

import java.util.function.BiFunction;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

/**
 * Wybór miejsca walki na prawdziwym terenie. Wcześniej stwory stawały tam, gdzie stał gracz (przed nim),
 * a kamera obok — w lesie, na zboczu albo w jaskini ciała wchodziły w bloki, a między kamerą i stworami
 * były liście/skała (właściciel 2026-09-30: „jak teren jest zjebany to nic nie widać i wpierdala nas w bloki”).
 *
 * <p>Teraz sprawdzamy kilkaset ustawień w pobliżu (środek × kierunek) i bierzemy najlepsze: oba stwory na
 * gruncie na podobnej wysokości, wolny korytarz między nimi, wolne miejsce dla kamery i czysta linia wzroku
 * z kamery do obu stworów, płaska okolica (real-time: stwory biegają). Resztę przeszkód zdejmuje {@link Widok}
 * po stronie klienta.
 */
public final class Arena {
  private Arena() {}

  /** Wybrane ustawienie: miejsca stworów (0 = strona gracza), kierunek 0→1, środek, ocena (0 = idealnie). */
  public record Plan(Location[] miejsca, Vector kier, Location srodek, double kara) {}

  /**
   * @param start       gdzie zaczęła się walka (stoi gracz)
   * @param pref        preferowany kierunek 0→1 (np. na dzikiego) — lekka premia, nie przymus
   * @param odl         rozstaw stworów
   * @param zasieg      jak daleko od startu może być środek areny
   * @param promien     promień, który ma być płaski (real-time), 0 = tylko miejsca stworów
   * @param oczy        pozycje kamer (oko) dla danego ustawienia miejsc — oceniamy je razem z miejscami
   */
  public static Plan znajdz(Location start, Vector pref, double odl, double zasieg, double promien,
      BiFunction<Location, Location, Location[]> oczy) {
    Vector p = pref == null ? new Vector(0, 0, 0) : pref.clone().setY(0);
    if (p.lengthSquared() > 1e-4) p.normalize();
    Plan najlepszy = null;
    for (double r = 0; r <= zasieg + 0.01; r += 3) {
      int kroki = r == 0 ? 1 : r <= 9 ? 8 : 12;
      for (int i = 0; i < kroki; i++) {
        double a = Math.PI * 2 * i / kroki;
        Location srodek = start.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
        for (int j = 0; j < 8; j++) {
          double b = Math.PI * 2 * j / 8;
          Vector k = new Vector(Math.cos(b), 0, Math.sin(b));
          Plan x = ocen(start, srodek, k, odl, promien, oczy);
          if (x == null) continue;
          double kara = x.kara + odleglosc(r) + (p.lengthSquared() > 0 ? (1 - k.dot(p)) * 0.8 : 0);
          if (najlepszy == null || kara < najlepszy.kara) najlepszy = new Plan(x.miejsca, x.kier, x.srodek, kara);
        }
      }
      // Dobre ustawienie blisko startu wystarczy — nie ciągniemy walki dalej, niż trzeba.
      if (najlepszy != null && najlepszy.kara < 1.5 + odleglosc(r)) break;
    }
    return najlepszy;
  }

  /** Kara za odejście od startu: blisko rośnie szybko, dalej wolniej (daleki płaski teren ma wygrać ze stokiem). */
  private static double odleglosc(double r) { return r <= 9 ? r * 0.35 : 3.15 + (r - 9) * 0.15; }

  private static Plan ocen(Location start, Location srodek, Vector k, double odl, double promien,
      BiFunction<Location, Location, Location[]> oczy) {
    Location a = grunt(srodek.clone().add(k.clone().multiply(-odl / 2)), 3, 4);
    Location b = grunt(srodek.clone().add(k.clone().multiply(odl / 2)), 3, 4);
    if (a == null || b == null) return null;
    double dy = Math.abs(a.getY() - b.getY());
    if (dy > 1.5) return null;
    if (Math.abs(a.getY() - start.getY()) > 5) return null;
    double kara = dy * 2;
    if (a.getBlock().isLiquid() || b.getBlock().isLiquid()) kara += 4;
    // korytarz między stworami (doskok, pocisk)
    kara += 3 * zablokowane(a.clone().add(0, 0.5, 0), b.clone().add(0, 0.5, 0));
    kara += 2 * zablokowane(a.clone().add(0, 1.5, 0), b.clone().add(0, 1.5, 0));
    // luz wokół stworów (duże modele) i niebo/strop nad nimi
    for (Location x : new Location[] {a, b}) {
      for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++) for (int oy = 0; oy <= 1; oy++)
        if (!x.clone().add(ox, oy, oz).getBlock().isPassable()) kara += 0.6;
      for (int oy = 2; oy <= 3; oy++) if (!x.clone().add(0, oy, 0).getBlock().isPassable()) kara += 0.5;
    }
    Location m = a.clone().add(b).multiply(0.5);
    if (promien > 0) {
      for (int i = 0; i < 12; i++) {
        double t = Math.PI * 2 * i / 12;
        for (double r : new double[] {promien * 0.5, promien}) {
          Location g = grunt(m.clone().add(Math.cos(t) * r, 0, Math.sin(t) * r), 2, 3);
          if (g == null || Math.abs(g.getY() - m.getY()) > 1.2) kara += 0.5;
        }
      }
    }
    a.setDirection(k);
    b.setDirection(k.clone().multiply(-1));
    if (oczy != null) {
      for (Location oko : oczy.apply(a, b)) {
        if (oko == null) continue;
        if (!Kamera.wolne(oko)) kara += 5;
        kara += 1.5 * zablokowane(oko, a.clone().add(0, 0.6, 0));
        kara += 1.5 * zablokowane(oko, b.clone().add(0, 0.6, 0));
      }
    }
    return new Plan(new Location[] {a, b}, k.clone(), m, kara);
  }

  /** Stopy stwora na gruncie: 2 wolne bloki i podłoże; szukamy od {@code gora} wyżej do {@code dol} niżej. */
  public static Location grunt(Location l, int gora, int dol) {
    Location base = l.clone();
    base.setY(Math.floor(base.getY()));
    // getBlock na niezaładowanym chunku wczytuje go SYNCHRONICZNIE w ticku — taki kandydat po prostu odpada
    if (!base.getWorld().isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) return null;
    for (int dy = gora; dy >= -dol; dy--) {
      Location x = base.clone().add(0, dy, 0);
      Block b = x.getBlock();
      if (b.isPassable() && !b.isLiquid() && b.getRelative(0, 1, 0).isPassable() && !b.getRelative(0, -1, 0).isPassable()) {
        return new Location(x.getWorld(), Math.floor(x.getX()) + 0.5, x.getY(), Math.floor(x.getZ()) + 0.5);
      }
    }
    return null;
  }

  /** Liczba różnych bloków stałych na odcinku (bez końców). */
  public static int zablokowane(Location z, Location d) {
    Vector v = d.toVector().subtract(z.toVector());
    double dl = v.length();
    if (dl < 1e-3) return 0;
    v.multiply(1 / dl);
    int n = 0;
    long ostatni = Long.MIN_VALUE;
    for (double t = 0.3; t < dl - 0.3; t += 0.25) {
      Location x = z.clone().add(v.clone().multiply(t));
      long klucz = ((long) x.getBlockX() & 0x3FFFFFF) << 38 | ((long) x.getBlockZ() & 0x3FFFFFF) << 12 | (x.getBlockY() & 0xFFF);
      if (klucz == ostatni) continue;
      ostatni = klucz;
      if (!x.getWorld().isChunkLoaded(x.getBlockX() >> 4, x.getBlockZ() >> 4)) { n++; continue; }
      if (!x.getBlock().isPassable()) n++;
    }
    return n;
  }
}
