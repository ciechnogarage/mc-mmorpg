package pl.mcmmorpg.napady;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Wykonawca bez ciala: ruch i walka jednostki liczone arytmetyka (bez encji, chunkow i A*), te same
 * drogi co ciala ({@link Uklad}: linia prosta albo pole przeplywu). Rozkazy i cele daje ten sam dowodca.
 * Liczby ruchu (predkosc, zasiegi, odstepy ciosow, celnosc, odstep w tloku) sa w configu {@code sym.*}; ustawiac z pomiaru cial:
 * testy/kalibracja.py czyta zapisy napadow z cialami i wypisuje te wartosci. Obrazenia NIE sa w configu: cios = atak broni
 * jednostki, po obronie i pancerzu celu z przedmiotow MMOItems ({@link Napad#trafienie}, {@link Wyposazenie}) - jak cialo.
 */
final class Symulacja {
  final double predkosc, szarza, wreczZasieg, lukZasieg, celnosc, odstep;
  /** strzal z muru w dol i z dolu na mur trafia inaczej (cel w tlumie pod brama vs lucznik za blankami), a strzala
   *  spadajaca z muru przyspiesza (vanilla mnozy obrazenia przez predkosc) - mierzone na cialach, kalibracja.py */
  final double celnoscZMuru, celnoscNaMur, strzalaZMuru;
  final int wreczCo, lukCo;
  /** tiki od zlapania celu do pierwszej strzaly: szkielet vanilla zaczyna naciagac po wybraniu celu i puszcza
   *  po 20 tikach (RangedBowAttackGoal), cel stracony = naciaganie od nowa - mierzone na cialach, kalibracja.py */
  final int lukNaciag;

  Symulacja(ConfigurationSection c) {
    predkosc = c == null ? 0.17 : c.getDouble("predkosc", 0.17);
    szarza = c == null ? 1.2 : c.getDouble("szarza", 1.2);
    wreczZasieg = c == null ? 2.0 : c.getDouble("wrecz-zasieg", 2.0);
    wreczCo = c == null ? 20 : c.getInt("wrecz-co", 20);
    lukZasieg = c == null ? 15 : c.getDouble("luk-zasieg", 15);
    lukCo = c == null ? 40 : c.getInt("luk-co", 40);
    lukNaciag = c == null ? 0 : c.getInt("luk-naciag", 0);
    celnosc = c == null ? 0.6 : c.getDouble("celnosc", 0.6);
    odstep = c == null ? 0.8 : c.getDouble("odstep", 0.8);
    celnoscZMuru = c == null ? celnosc : c.getDouble("celnosc-z-muru", celnosc);
    celnoscNaMur = c == null ? celnosc : c.getDouble("celnosc-na-mur", celnosc);
    strzalaZMuru = c == null ? 1 : c.getDouble("strzala-z-muru", 1);
  }

  /** Jeden tik jednostki bez ciala. */
  void krok(Napad n, Jednostka j) {
    Jednostka c = j.cel != null && j.cel.zywa ? j.cel : null;
    if (c != null && !Double.isNaN(j.px)) c = null;   // rozkaz marszu ma pierwszenstwo (np. ranny, odwrot)
    if (j.lucznik()) {
      if (c == null) j.naciaga = false;
      else if (!j.naciaga) { j.naciaga = true; j.gotowyOd = Math.max(j.gotowyOd, n.tik + lukNaciag); }
    }
    if (c != null) {
      double d = j.odl(c.x, c.z);
      if (j.lucznik()) {
        boolean widzi = j.naMurze || c.naMurze || n.arena.uklad.wolnaLinia(j.x, j.z, c.x, c.z);
        if (d <= lukZasieg && widzi) {
          patrz(j, c.x, c.z);
          if (n.tik >= j.gotowyOd) {
            j.gotowyOd = n.tik + lukCo + n.los.nextInt(Math.max(1, lukCo / 4));
            n.zdarzenie("strzal", j.id + ">" + c.id);
            boolean zMuru = j.naMurze && !c.naMurze, naMur = c.naMurze && !j.naMurze;
            if (n.losTraf.nextDouble() < (zMuru ? celnoscZMuru : naMur ? celnoscNaMur : celnosc))
              n.trafienie(j, c, j.zestaw.atak() * (zMuru ? strzalaZMuru : 1), true);
          }
        } else if (!j.naMurze) idz(n, j, c.x, c.z, predkosc);
        return;
      }
      if (d <= wreczZasieg) {
        patrz(j, c.x, c.z);
        if (n.tik >= j.gotowyOd) {
          j.gotowyOd = n.tik + wreczCo;
          n.trafienie(j, c, j.zestaw.atak(), false);
        }
      } else idz(n, j, c.x, c.z, predkosc * szarza);
      return;
    }
    // do samego punktu, nie do brzegu promienia rozkazu: pathfinder ciala idzie do srodka kratki (moveTo), dowodca tylko
    // sprawdza promien; zatrzymanie na brzegu zostawialo drugi rzad grabiacych poza magazynem (kalibracja 25.10)
    if (!Double.isNaN(j.px) && j.odl(j.px, j.pz) > Math.min(j.pr, 0.25)) idz(n, j, j.px, j.pz, predkosc);
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
