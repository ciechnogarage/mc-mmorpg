package pl.mcmmorpg.napady;

import org.bukkit.Material;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Staly uklad areny (ten sam w kazdym slocie), liczony raz z kodu, bez czytania blokow swiata.
 * Wspolrzedne wzgledem rogu areny; stoi sie na y = Arena.Y (ziemia y-1 zawsze pelna, swiat plaski).
 * Dwa stany: brama stoi / zburzona. Kazdy ma siatke chodliwosci, skladowe spojne i pola przeplywu (BFS).
 * Symulacja i ciala MM korzystaja z tych samych pol, wiec przejscie miedzy nimi nie zmienia drog.
 */
final class Uklad {
  static final int Y = -59, MIN_X = -4, MIN_Z = -4, W = 57, H = 49, WYS = 8;
  static final int MUR_X0 = 20, MUR_X1 = 44, MUR_Z0 = 8, MUR_Z1 = 32, MUR_H = 4;
  static final int BRAMA_X = 20, BRAMA_Z0 = 19, BRAMA_Z1 = 21;
  static final int MAG_X0 = 34, MAG_X1 = 40, MAG_Z0 = 17, MAG_Z1 = 23, DRZWI_Z = 20;
  static final int NIEOSIAGALNE = Short.MAX_VALUE;

  static final Uklad BRAMA_STOI = new Uklad(true), BRAMA_ZBURZONA = new Uklad(false);

  final boolean bramaStoi;
  final boolean[] chodliwe = new boolean[W * H];
  /** numer spojnej skladowej pola chodliwego (-1 = niechodliwe); zamknieta brama dzieli pole od wnetrza */
  final int[] skladowa = new int[W * H];
  final int[] doBramy, doMagazynu, doOdwrotu;
  /** pola do dowolnej kratki (szyk, posterunek, cel): BFS ~50 us, trzymane do 128 ostatnich */
  private final Map<Integer, int[]> pola = new LinkedHashMap<>(256, 0.75f, true) {
    @Override protected boolean removeEldestEntry(Map.Entry<Integer, int[]> e) { return size() > 128; }
  };

  private Uklad(boolean bramaStoi) {
    this.bramaStoi = bramaStoi;
    for (int i = 0; i < W; i++)
      for (int j = 0; j < H; j++) {
        int x = MIN_X + i, z = MIN_Z + j;
        chodliwe[i * H + j] = blok(x, Y, z, bramaStoi) == null && blok(x, Y + 1, z, bramaStoi) == null;
      }
    doBramy = bfs(new int[][]{{BRAMA_X - 1, BRAMA_Z0}, {BRAMA_X - 1, BRAMA_Z0 + 1}, {BRAMA_X - 1, BRAMA_Z1}});
    doMagazynu = bfs(new int[][]{{MAG_X0 - 1, DRZWI_Z}, {MAG_X0, DRZWI_Z}});
    doOdwrotu = bfs(new int[][]{{3, 18}, {3, 20}, {3, 22}});
    oznaczSkladowe();
  }

  /** Blok ukladu w punkcie (null = powietrze). Jedno zrodlo prawdy dla budowy i siatki. */
  static Material blok(int x, int y, int z, boolean bramaStoi) {
    if (y < Y || y >= Y + WYS) return null;
    int h = y - Y;
    if (h < MUR_H && ((z == MUR_Z0 || z == MUR_Z1) && x >= MUR_X0 && x <= MUR_X1
        || (x == MUR_X0 || x == MUR_X1) && z >= MUR_Z0 && z <= MUR_Z1)) {
      if (x == BRAMA_X && z >= BRAMA_Z0 && z <= BRAMA_Z1 && h < 3) return bramaStoi ? Material.OAK_PLANKS : null;
      return h == MUR_H - 1 && (x + z) % 2 == 0 ? Material.STONE_BRICK_WALL : Material.STONE_BRICKS;
    }
    boolean wMag = x >= MAG_X0 && x <= MAG_X1 && z >= MAG_Z0 && z <= MAG_Z1;
    if (!wMag) return null;
    if (h == MUR_H) return Material.SPRUCE_SLAB;
    if (h > MUR_H) return null;
    boolean sciana = x == MAG_X0 || x == MAG_X1 || z == MAG_Z0 || z == MAG_Z1;
    if (sciana) {
      if (x == MAG_X0 && z == DRZWI_Z && h < 2) return null;     // drzwi
      return h == 0 ? Material.COBBLESTONE : Material.SPRUCE_PLANKS;
    }
    if (h == 0 && x == MAG_X0 + 2 && z > MAG_Z0 && z < MAG_Z1 && (z - MAG_Z0 - 1) % 2 == 0) return Material.BARREL;
    return null;
  }

  static Uklad dla(boolean bramaStoi) { return bramaStoi ? BRAMA_STOI : BRAMA_ZBURZONA; }

  boolean chodliwe(int x, int z) {
    int i = x - MIN_X, j = z - MIN_Z;
    return i >= 0 && j >= 0 && i < W && j < H && chodliwe[i * H + j];
  }

  /** Pole przeplywu do kratki (tx, tz); dla kratki niechodliwej - do najblizszej chodliwej obok. */
  synchronized int[] pole(double tx, double tz) {
    int cx = (int) Math.floor(tx), cz = (int) Math.floor(tz);
    if (!chodliwe(cx, cz)) {
      outer:
      for (int r = 1; r <= 2; r++)
        for (int dx = -r; dx <= r; dx++)
          for (int dz = -r; dz <= r; dz++)
            if (chodliwe(cx + dx, cz + dz)) { cx += dx; cz += dz; break outer; }
    }
    int klucz = cx * 1000 + cz;
    int[] p = pola.get(klucz);
    if (p == null) {
      p = chodliwe(cx, cz) ? bfs(new int[][]{{cx, cz}}) : doOdwrotu;
      pola.put(klucz, p);
    }
    return p;
  }

  private void oznaczSkladowe() {
    Arrays.fill(skladowa, -1);
    int nr = 0;
    ArrayDeque<Integer> q = new ArrayDeque<>();
    for (int s0 = 0; s0 < W * H; s0++) {
      if (!chodliwe[s0] || skladowa[s0] >= 0) continue;
      skladowa[s0] = nr;
      q.add(s0);
      while (!q.isEmpty()) {
        int k = q.poll(), i = k / H, j = k % H;
        int[][] sas = {{i + 1, j}, {i - 1, j}, {i, j + 1}, {i, j - 1}};
        for (int[] n : sas) {
          if (n[0] < 0 || n[1] < 0 || n[0] >= W || n[1] >= H) continue;
          int m = n[0] * H + n[1];
          if (chodliwe[m] && skladowa[m] < 0) { skladowa[m] = nr; q.add(m); }
        }
      }
      nr++;
    }
  }

  /** Skladowa pola pod punktem areny; encja przy scianie bywa na granicy kratki, wiec bierze sasiada. */
  int skladowa(double x, double z) {
    int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
    for (int r = 0; r <= 1; r++)
      for (int dx = -r; dx <= r; dx++)
        for (int dz = -r; dz <= r; dz++) {
          int i = bx + dx - MIN_X, j = bz + dz - MIN_Z;
          if (i >= 0 && j >= 0 && i < W && j < H && skladowa[i * H + j] >= 0) return skladowa[i * H + j];
        }
    return -1;
  }

  /** Czy piechota dojdzie z punktu do punktu (ta sama skladowa). */
  boolean osiagalne(double x0, double z0, double x1, double z1) {
    int a = skladowa(x0, z0);
    return a >= 0 && a == skladowa(x1, z1);
  }

  private int[] bfs(int[][] cele) {
    int[] d = new int[W * H];
    Arrays.fill(d, NIEOSIAGALNE);
    ArrayDeque<Integer> q = new ArrayDeque<>();
    for (int[] c : cele) {
      int i = c[0] - MIN_X, j = c[1] - MIN_Z;
      d[i * H + j] = 0;
      q.add(i * H + j);
    }
    while (!q.isEmpty()) {
      int k = q.poll(), i = k / H, j = k % H;
      for (int di = -1; di <= 1; di++)
        for (int dj = -1; dj <= 1; dj++) {
          if (di == 0 && dj == 0) continue;
          int ni = i + di, nj = j + dj;
          if (ni < 0 || nj < 0 || ni >= W || nj >= H || !chodliwe[ni * H + nj]) continue;
          // skos tylko gdy oba boki wolne (bez scinania naroznikow muru)
          if (di != 0 && dj != 0 && (!chodliwe[ni * H + j] || !chodliwe[i * H + nj])) continue;
          int nd = d[k] + (di != 0 && dj != 0 ? 14 : 10);
          if (nd < d[ni * H + nj]) { d[ni * H + nj] = nd; q.add(ni * H + nj); }
        }
    }
    return d;
  }

  /** Nastepne pole (srodek) w strone celu pola {@code pole}; null gdy cel osiagniety lub nieosiagalny. */
  double[] krok(int[] pole, double x, double z) {
    int cx = (int) Math.floor(x), cz = (int) Math.floor(z);
    int i = cx - MIN_X, j = cz - MIN_Z;
    if (i < 0 || j < 0 || i >= W || j >= H) return null;
    int best = pole[i * H + j], bi = -1, bj = -1;
    if (best == 0) return null;
    for (int di = -1; di <= 1; di++)
      for (int dj = -1; dj <= 1; dj++) {
        int ni = i + di, nj = j + dj;
        if (ni < 0 || nj < 0 || ni >= W || nj >= H) continue;
        if (di != 0 && dj != 0 && (!chodliwe[ni * H + j] || !chodliwe[i * H + nj])) continue;
        if (pole[ni * H + nj] < best) { best = pole[ni * H + nj]; bi = ni; bj = nj; }
      }
    if (bi < 0) return null;
    return new double[]{MIN_X + bi + 0.5, MIN_Z + bj + 0.5};
  }

  /**
   * Punkt posredni dla pathfindera vanilla: do celu wprost, gdy blisko i linia wolna, inaczej
   * {@code kroki} kratek po polu przeplywu. Krotki odcinek = maly A* (staly koszt na jednostke).
   */
  double[] posredni(double x, double z, double tx, double tz, int kroki) {
    double d = Math.hypot(tx - x, tz - z);
    if (d <= kroki || (d <= 2 * kroki && wolnaLinia(x, z, tx, tz))) return new double[]{tx, tz};
    int[] p = pole(tx, tz);
    double px = x, pz = z;
    for (int k = 0; k < kroki; k++) {
      double[] n = krok(p, px, pz);
      if (n == null) break;
      px = n[0]; pz = n[1];
    }
    return px == x && pz == z ? new double[]{tx, tz} : new double[]{px, pz};
  }

  /** Prosta linia po siatce (Bresenham) - czy da sie isc wprost. */
  boolean wolnaLinia(double x0, double z0, double x1, double z1) {
    int a = (int) Math.floor(x0), b = (int) Math.floor(z0), c = (int) Math.floor(x1), e = (int) Math.floor(z1);
    int dx = Math.abs(c - a), dz = Math.abs(e - b), sx = a < c ? 1 : -1, sz = b < e ? 1 : -1, err = dx - dz;
    while (true) {
      if (!chodliwe(a, b)) return false;
      if (a == c && b == e) return true;
      int e2 = 2 * err;
      if (e2 > -dz) { err -= dz; a += sx; }
      if (e2 < dx) { err += dx; b += sz; }
    }
  }
}
