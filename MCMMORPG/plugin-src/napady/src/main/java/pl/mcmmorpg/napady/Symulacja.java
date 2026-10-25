package pl.mcmmorpg.napady;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Wykonawca bez ciala: ruch i walka jednostki liczone arytmetyka (bez encji, chunkow i A*), te same
 * drogi co ciala ({@link Uklad}: linia prosta albo pole przeplywu). Rozkazy i cele daje ten sam dowodca.
 * Liczby (predkosc, zasiegi, odstepy ciosow, obrazenia po pancerzu, celnosc, odstep w tloku) sa w configu {@code sym.*}; ustawiac z pomiaru cial:
 * testy/kalibracja.py czyta zapisy napadow z cialami i wypisuje te wartosci.
 */
final class Symulacja {
  final double predkosc, szarza, wreczZasieg, lukZasieg, celnosc, strzala, obrazenia, odstep;
  final int wreczCo, lukCo;

  Symulacja(ConfigurationSection c, double obrazenia) {
    predkosc = c == null ? 0.17 : c.getDouble("predkosc", 0.17);
    szarza = c == null ? 1.2 : c.getDouble("szarza", 1.2);
    wreczZasieg = c == null ? 2.0 : c.getDouble("wrecz-zasieg", 2.0);
    wreczCo = c == null ? 20 : c.getInt("wrecz-co", 20);
    lukZasieg = c == null ? 15 : c.getDouble("luk-zasieg", 15);
    lukCo = c == null ? 40 : c.getInt("luk-co", 40);
    celnosc = c == null ? 0.6 : c.getDouble("celnosc", 0.6);
    strzala = c == null ? 2.5 : c.getDouble("strzala", 2.5);
    odstep = c == null ? 0.8 : c.getDouble("odstep", 0.8);
    // cios ciala po pancerzu celu (mob bije config obrazenia, kaftan i helm zdejmuja czesc)
    this.obrazenia = c == null ? obrazenia : c.getDouble("wrecz", obrazenia);
  }

  /** Jeden tik jednostki bez ciala. */
  void krok(Napad n, Jednostka j) {
    Jednostka c = j.cel != null && j.cel.zywa ? j.cel : null;
    if (c != null && !Double.isNaN(j.px)) c = null;   // rozkaz marszu ma pierwszenstwo (np. ranny, odwrot)
    if (c != null) {
      double d = j.odl(c.x, c.z);
      if (j.lucznik()) {
        boolean widzi = j.naMurze || c.naMurze || n.arena.uklad.wolnaLinia(j.x, j.z, c.x, c.z);
        if (d <= lukZasieg && widzi) {
          patrz(j, c.x, c.z);
          if (n.tik >= j.gotowyOd) {
            j.gotowyOd = n.tik + lukCo + n.los.nextInt(Math.max(1, lukCo / 4));
            n.zdarzenie("strzal", j.id + ">" + c.id);
            if (n.los.nextDouble() < celnosc) n.trafienie(j, c, strzala, true);
          }
        } else if (!j.naMurze) idz(n, j, c.x, c.z, predkosc);
        return;
      }
      if (d <= wreczZasieg) {
        patrz(j, c.x, c.z);
        if (n.tik >= j.gotowyOd) {
          j.gotowyOd = n.tik + wreczCo;
          n.trafienie(j, c, obrazenia, false);
        }
      } else idz(n, j, c.x, c.z, predkosc * szarza);
      return;
    }
    if (!Double.isNaN(j.px) && j.odl(j.px, j.pz) > j.pr) idz(n, j, j.px, j.pz, predkosc);
  }

  private static void patrz(Jednostka j, double tx, double tz) {
    j.yaw = Math.toDegrees(Math.atan2(-(tx - j.x), tz - j.z));
  }

  /** Krok w strone punktu: wprost gdy linia wolna, inaczej po polu przeplywu; rozpychanie od swoich, slizg po scianie. */
  private void idz(Napad n, Jednostka j, double tx, double tz, double v) {
    Uklad u = n.arena.uklad;
    double gx = tx, gz = tz;
    if (!u.wolnaLinia(j.x, j.z, tx, tz)) {
      double[] k = u.krok(u.pole(tx, tz), j.x, j.z);
      if (k == null) return;
      gx = k[0]; gz = k[1];
    }
    double dx = gx - j.x, dz = gz - j.z, len = Math.hypot(dx, dz);
    if (len < 1e-3) return;
    double s = Math.min(v, len);
    double mx = dx / len * s, mz = dz / len * s;
    for (Jednostka o : n.jednostki) {
      if (o == j || !o.zywa || o.strona != j.strona || o.naMurze != j.naMurze || "uciekl".equals(o.akcja)) continue;   // uciekl = zszedl z pola
      double ex = j.x - o.x, ez = j.z - o.z, d2 = ex * ex + ez * ez;
      if (d2 < odstep * odstep && d2 > 1e-6) { double d = Math.sqrt(d2); mx += ex / d * (odstep - d) * 0.3; mz += ez / d * (odstep - d) * 0.3; }
    }
    double nx = j.x + mx, nz = j.z + mz;
    if (u.chodliwe((int) Math.floor(nx), (int) Math.floor(nz))) { j.x = nx; j.z = nz; }
    else if (u.chodliwe((int) Math.floor(nx), (int) Math.floor(j.z))) j.x = nx;
    else if (u.chodliwe((int) Math.floor(j.x), (int) Math.floor(nz))) j.z = nz;
    j.yaw = Math.toDegrees(Math.atan2(-dx, dz));
  }
}
