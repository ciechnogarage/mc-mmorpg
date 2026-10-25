package pl.mcmmorpg.napady;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Arena prototypu na plaskim klonie (ziemia y=-60, stoi sie na y=-59). Wspolrzedne wzgledem (ox, oz).
 * Mur kamienny x 20..44, z 8..32, wysokosc 4; brama z desek w zachodnim murze (x=20, z 19..21);
 * magazyn x 34..40, z 17..23 z wejsciem od zachodu (x=34, z=20).
 * Siatka 2D przejsciowosci + pola przeplywu (BFS) do bramy, magazynu i punktu odwrotu.
 */
final class Arena {
  static final int Y = -59, MIN_X = -4, MIN_Z = -4, W = 57, H = 49;
  static final int MUR_X0 = 20, MUR_X1 = 44, MUR_Z0 = 8, MUR_Z1 = 32, MUR_H = 4;
  static final int BRAMA_X = 20, BRAMA_Z0 = 19, BRAMA_Z1 = 21;
  static final int MAG_X0 = 34, MAG_X1 = 40, MAG_Z0 = 17, MAG_Z1 = 23, DRZWI_Z = 20;
  static final int NIEOSIAGALNE = Short.MAX_VALUE;

  final World swiat;
  final int ox, oz;
  final boolean[] chodliwe = new boolean[W * H];
  int[] doBramy, doMagazynu, doOdwrotu;

  Arena(World swiat, int ox, int oz) { this.swiat = swiat; this.ox = ox; this.oz = oz; }

  void zbuduj() {
    for (int x = MIN_X; x < MIN_X + W; x++)
      for (int z = MIN_Z; z < MIN_Z + H; z++)
        for (int y = Y; y < Y + 8; y++) blok(x, y, z).setType(Material.AIR, false);
    for (int y = Y; y < Y + MUR_H; y++) {
      for (int x = MUR_X0; x <= MUR_X1; x++) { mur(x, y, MUR_Z0); mur(x, y, MUR_Z1); }
      for (int z = MUR_Z0; z <= MUR_Z1; z++) { mur(MUR_X0, y, z); mur(MUR_X1, y, z); }
      for (int x = MAG_X0; x <= MAG_X1; x++) { deska(x, y, MAG_Z0, y); deska(x, y, MAG_Z1, y); }
      for (int z = MAG_Z0; z <= MAG_Z1; z++) { deska(MAG_X0, y, z, y); deska(MAG_X1, y, z, y); }
    }
    for (int x = MAG_X0; x <= MAG_X1; x++)
      for (int z = MAG_Z0; z <= MAG_Z1; z++) blok(x, Y + MUR_H, z).setType(Material.SPRUCE_SLAB, false);
    blok(MAG_X0, Y, DRZWI_Z).setType(Material.AIR, false);
    blok(MAG_X0, Y + 1, DRZWI_Z).setType(Material.AIR, false);
    for (int z = MAG_Z0 + 1; z < MAG_Z1; z += 2) blok(MAG_X0 + 2, Y, z).setType(Material.BARREL, false);
    postawBrame();
  }

  private void mur(int x, int y, int z) {
    blok(x, y, z).setType(y == Y + MUR_H - 1 && (x + z) % 2 == 0 ? Material.STONE_BRICK_WALL : Material.STONE_BRICKS, false);
  }

  private void deska(int x, int y, int z, int warstwa) {
    blok(x, y, z).setType(warstwa == Y ? Material.COBBLESTONE : Material.SPRUCE_PLANKS, false);
  }

  void postawBrame() {
    for (int z = BRAMA_Z0; z <= BRAMA_Z1; z++)
      for (int y = Y; y < Y + 3; y++) blok(BRAMA_X, y, z).setType(Material.OAK_PLANKS, false);
  }

  void zburzBrame() {
    for (int z = BRAMA_Z0; z <= BRAMA_Z1; z++)
      for (int y = Y; y < Y + 3; y++) blok(BRAMA_X, y, z).setType(Material.AIR, false);
  }

  Block blok(int x, int y, int z) { return swiat.getBlockAt(ox + x, y, oz + z); }

  Location loc(double x, double z) { return new Location(swiat, ox + x + 0.5, Y, oz + z + 0.5); }

  void bilety(Plugin p, boolean dodaj) {
    for (int cx = (ox + MIN_X) >> 4; cx <= (ox + MIN_X + W) >> 4; cx++)
      for (int cz = (oz + MIN_Z) >> 4; cz <= (oz + MIN_Z + H) >> 4; cz++)
        if (dodaj) swiat.addPluginChunkTicket(cx, cz, p); else swiat.removePluginChunkTicket(cx, cz, p);
  }

  /** Przelicza siatke i pola; woluj po starcie i po zburzeniu bramy. */
  void przelicz() {
    for (int i = 0; i < W; i++)
      for (int j = 0; j < H; j++) {
        int x = MIN_X + i, z = MIN_Z + j;
        chodliwe[i * H + j] = blok(x, Y - 1, z).getType().isSolid()
            && blok(x, Y, z).isPassable() && blok(x, Y + 1, z).isPassable();
      }
    doBramy = bfs(new int[][]{{BRAMA_X - 1, BRAMA_Z0}, {BRAMA_X - 1, BRAMA_Z0 + 1}, {BRAMA_X - 1, BRAMA_Z1}});
    doMagazynu = bfs(new int[][]{{MAG_X0 - 1, DRZWI_Z}, {MAG_X0, DRZWI_Z}});
    doOdwrotu = bfs(new int[][]{{3, 18}, {3, 20}, {3, 22}});
  }

  boolean chodliwe(int x, int z) {
    int i = x - MIN_X, j = z - MIN_Z;
    return i >= 0 && j >= 0 && i < W && j < H && chodliwe[i * H + j];
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
