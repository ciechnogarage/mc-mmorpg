package pl.mcmmorpg.wyscigi;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Tor jako zamknięta linia środkowa (Catmull-Rom) próbkowana co KROK bloku. Postęp gracza = rzut na tę linię. */
final class Tor {
  static final double KROK = 0.5;

  record Rzut(double s, double bok, double odleglosc, int indeks) {}
  record Pole(double s, double bok) {}

  final String id, nazwa;
  final int y;
  final double polszerokosc, pobocze;
  final int okrazenia;
  final List<Pole> pola = new ArrayList<>();
  private final double[] px, pz, tx, tz;
  final double dlugosc;

  Tor(String id, ConfigurationSection c) {
    this.id = id;
    nazwa = c.getString("nazwa", id);
    y = c.getInt("y", 64);
    polszerokosc = c.getDouble("polszerokosc", 5.5);
    pobocze = c.getDouble("pobocze", 3.5);
    okrazenia = c.getInt("okrazenia", 3);
    List<double[]> punkty = new ArrayList<>();
    for (Object o : c.getList("punkty", List.of())) {
      List<?> l = (List<?>) o;
      punkty.add(new double[]{((Number) l.get(0)).doubleValue(), ((Number) l.get(1)).doubleValue()});
    }
    if (punkty.size() < 4) throw new IllegalArgumentException("Tor " + id + ": za mało punktów");
    // Gęste próbkowanie splajnu, potem równe odcinki co KROK.
    List<double[]> gesto = new ArrayList<>();
    int n = punkty.size();
    for (int i = 0; i < n; i++) {
      double[] p0 = punkty.get((i - 1 + n) % n), p1 = punkty.get(i), p2 = punkty.get((i + 1) % n), p3 = punkty.get((i + 2) % n);
      for (int k = 0; k < 64; k++) gesto.add(catmull(p0, p1, p2, p3, k / 64.0));
    }
    gesto.add(gesto.get(0));
    List<double[]> rowno = new ArrayList<>();
    rowno.add(gesto.get(0));
    double zapas = 0;
    for (int i = 1; i < gesto.size(); i++) {
      double[] a = gesto.get(i - 1), b = gesto.get(i);
      double d = Math.hypot(b[0] - a[0], b[1] - a[1]);
      double t = KROK - zapas;
      while (t <= d) {
        rowno.add(new double[]{a[0] + (b[0] - a[0]) * t / d, a[1] + (b[1] - a[1]) * t / d});
        t += KROK;
      }
      zapas = d - (t - KROK);
    }
    // Ostatnia próbka leży tuż przed startem — wyrzucamy, żeby pętla zamykała się równo.
    if (Math.hypot(rowno.get(rowno.size() - 1)[0] - rowno.get(0)[0], rowno.get(rowno.size() - 1)[1] - rowno.get(0)[1]) < KROK * 0.5)
      rowno.remove(rowno.size() - 1);
    int m = rowno.size();
    px = new double[m]; pz = new double[m]; tx = new double[m]; tz = new double[m];
    for (int i = 0; i < m; i++) { px[i] = rowno.get(i)[0]; pz[i] = rowno.get(i)[1]; }
    for (int i = 0; i < m; i++) {
      double dx = px[(i + 1) % m] - px[(i - 1 + m) % m], dz = pz[(i + 1) % m] - pz[(i - 1 + m) % m];
      double d = Math.hypot(dx, dz);
      tx[i] = dx / d; tz[i] = dz / d;
    }
    dlugosc = m * KROK;
    for (Map<?, ?> p : c.getMapList("przyspieszenia"))
      pola.add(new Pole(((Number) p.get("s")).doubleValue(), p.get("bok") == null ? 0 : ((Number) p.get("bok")).doubleValue()));
  }

  private static double[] catmull(double[] p0, double[] p1, double[] p2, double[] p3, double t) {
    double t2 = t * t, t3 = t2 * t;
    double[] r = new double[2];
    for (int k = 0; k < 2; k++)
      r[k] = 0.5 * (2 * p1[k] + (-p0[k] + p2[k]) * t + (2 * p0[k] - 5 * p1[k] + 4 * p2[k] - p3[k]) * t2 + (-p0[k] + 3 * p1[k] - 3 * p2[k] + p3[k]) * t3);
    return r;
  }

  int probki() { return px.length; }

  /** Linia środkowa co `co` próbek, "x,z;x,z;..." — dla autopilota testów. */
  String eksport(int co) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < px.length; i += co) b.append(String.format(java.util.Locale.ROOT, "%.1f,%.1f;", px[i], pz[i]));
    return b.toString();
  }
  private int indeks(double s) { int m = px.length; return (int) Math.floorMod(Math.round(s / KROK), (long) m); }

  /** Rzut punktu na linię; okno wokół poprzedniego indeksu chroni przed przeskokiem na sąsiedni fragment toru. */
  Rzut rzut(double x, double z, int poprzedni, int okno) {
    int m = px.length, best = -1;
    double bd = Double.MAX_VALUE;
    if (poprzedni >= 0) {
      for (int k = -okno; k <= okno; k++) {
        int i = Math.floorMod(poprzedni + k, m);
        double d = (x - px[i]) * (x - px[i]) + (z - pz[i]) * (z - pz[i]);
        if (d < bd) { bd = d; best = i; }
      }
    } else {
      for (int i = 0; i < m; i++) {
        double d = (x - px[i]) * (x - px[i]) + (z - pz[i]) * (z - pz[i]);
        if (d < bd) { bd = d; best = i; }
      }
    }
    double dx = x - px[best], dz = z - pz[best];
    double wzdluz = dx * tx[best] + dz * tz[best];
    // Prawa strona względem kierunku jazdy (MC: x w prawo od z+ to -x), liczona iloczynem wektorowym.
    double bok = dx * -tz[best] + dz * tx[best];
    return new Rzut(best * KROK + wzdluz, bok, Math.sqrt(bd), best);
  }

  /** Punkt na torze w odległości s od startu, przesunięty w bok, z kierunkiem jazdy. */
  Location punkt(World w, double s, double bok) {
    int i = indeks(s);
    double x = px[i] + -tz[i] * bok, z = pz[i] + tx[i] * bok;
    Location l = new Location(w, x, y, z);
    l.setYaw(yaw(i));
    return l;
  }

  float yaw(int i) { return (float) Math.toDegrees(Math.atan2(-tx[i], tz[i])); }
  float yawPrzy(double s) { return yaw(indeks(s)); }

  /** Miejsca startowe: pary rzędów za linią startu, przesunięte jak w kartingu. */
  Location miejsceStartowe(World w, int nr) {
    int rzad = nr / 2;
    double bok = (nr % 2 == 0) ? -2.3 : 2.3;
    double s = -3.5 - rzad * 4.5 - (nr % 2) * 1.5;
    return punkt(w, s, bok);
  }

  // ---------------------------------------------------------------- budowa
  private static final Material[] KWIATY = {Material.SHORT_GRASS, Material.SHORT_GRASS, Material.SHORT_GRASS, Material.POPPY, Material.DANDELION, Material.CORNFLOWER, Material.OXEYE_DAISY};

  int[] obszar() {
    double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
    for (int i = 0; i < px.length; i++) { minX = Math.min(minX, px[i]); maxX = Math.max(maxX, px[i]); minZ = Math.min(minZ, pz[i]); maxZ = Math.max(maxZ, pz[i]); }
    int m = (int) Math.ceil(polszerokosc + pobocze + 10);
    return new int[]{(int) Math.floor(minX) - m, (int) Math.floor(minZ) - m, (int) Math.ceil(maxX) + m, (int) Math.ceil(maxZ) + m};
  }

  /** Buduje blockout toru. Zwraca liczbę postawionych bloków. Wywoływać na załadowanych chunkach (forceload). */
  int zbuduj(World w) {
    int[] o = obszar();
    Random r = new Random(1234);
    int licznik = 0;
    double sciana = polszerokosc + pobocze;
    for (int x = o[0]; x <= o[2]; x++) for (int z = o[1]; z <= o[3]; z++) {
      Rzut rz = rzut(x + 0.5, z + 0.5, -1, 0);
      double d = rz.odleglosc();
      for (int yy = y - 4; yy <= y + 6; yy++) w.getBlockAt(x, yy, z).setType(Material.AIR, false);
      w.getBlockAt(x, y - 2, z).setType(Material.DIRT, false);
      Block gora = w.getBlockAt(x, y - 1, z);
      if (d <= polszerokosc) {
        double s = ((rz.s() % dlugosc) + dlugosc) % dlugosc;
        if (s < 1.25 || s > dlugosc - 0.25) gora.setType(((x + z) & 1) == 0 ? Material.WHITE_CONCRETE : Material.BLACK_CONCRETE, false);
        else if (d > polszerokosc - 1.0) gora.setType(((int) (s / 2)) % 2 == 0 ? Material.RED_CONCRETE : Material.WHITE_CONCRETE, false);
        else gora.setType(r.nextInt(9) == 0 ? Material.COARSE_DIRT : Material.PACKED_MUD, false);
      } else if (d <= sciana) {
        gora.setType(Material.GRASS_BLOCK, false);
        if (r.nextInt(6) == 0) w.getBlockAt(x, y, z).setType(KWIATY[r.nextInt(KWIATY.length)], false);
      } else if (d <= sciana + 1.0) {
        gora.setType(Material.GRASS_BLOCK, false);
        w.getBlockAt(x, y, z).setType(Material.OAK_FENCE, false);
      } else {
        gora.setType(Material.GRASS_BLOCK, false);
        if (r.nextInt(9) == 0) w.getBlockAt(x, y, z).setType(KWIATY[r.nextInt(KWIATY.length)], false);
      }
      licznik++;
    }
    // Płoty łączą się dopiero po aktualizacji sąsiadów — druga przejście z fizyką.
    for (int x = o[0]; x <= o[2]; x++) for (int z = o[1]; z <= o[3]; z++) {
      Block b = w.getBlockAt(x, y, z);
      if (b.getType() == Material.OAK_FENCE) { b.setType(Material.AIR, false); b.setType(Material.OAK_FENCE, true); }
    }
    for (Pole p : pola) zbudujPole(w, p);
    zbudujBrame(w);
    sadzDrzewa(w, o, r, sciana);
    return licznik;
  }

  private void zbudujPole(World w, Pole p) {
    int i = indeks(p.s());
    BlockFace kierunek = kierunekBloku(i);
    for (double a = -1.5; a <= 1.5; a += 0.5) for (double b = -1.5; b <= 1.5; b += 0.5) {
      double s = p.s() + a, bok = p.bok() + b;
      Location l = punkt(w, s, bok);
      Block blok = w.getBlockAt(l.getBlockX(), y - 1, l.getBlockZ());
      blok.setType(Material.MAGENTA_GLAZED_TERRACOTTA, false);
      if (blok.getBlockData() instanceof Directional dir) { dir.setFacing(kierunek.getOppositeFace()); blok.setBlockData(dir, false); }
    }
  }

  private BlockFace kierunekBloku(int i) {
    if (Math.abs(tx[i]) > Math.abs(tz[i])) return tx[i] > 0 ? BlockFace.EAST : BlockFace.WEST;
    return tz[i] > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
  }

  private void zbudujBrame(World w) {
    double bok = polszerokosc + 1.0;
    Location lewa = punkt(w, 0.5, -bok), prawa = punkt(w, 0.5, bok);
    for (Location l : new Location[]{lewa, prawa})
      for (int h = 0; h < 6; h++) w.getBlockAt(l.getBlockX(), y + h, l.getBlockZ()).setType(Material.STRIPPED_SPRUCE_LOG, false);
    for (double b = -bok; b <= bok; b += 0.5) {
      Location l = punkt(w, 0.5, b);
      w.getBlockAt(l.getBlockX(), y + 6, l.getBlockZ()).setType(Material.SPRUCE_PLANKS, false);
      boolean szachownica = ((int) Math.floor(b + 100)) % 2 == 0;
      w.getBlockAt(l.getBlockX(), y + 5, l.getBlockZ()).setType(Math.abs(b) < bok - 0.5 ? (szachownica ? Material.WHITE_WOOL : Material.BLACK_WOOL) : Material.STRIPPED_SPRUCE_LOG, false);
    }
    for (double b : new double[]{-bok + 2, 0, bok - 2}) {
      Location l = punkt(w, 0.5, b);
      Block lampa = w.getBlockAt(l.getBlockX(), y + 4, l.getBlockZ());
      lampa.setType(Material.LANTERN, false);
      if (lampa.getBlockData() instanceof Lantern lan) { lan.setHanging(true); lampa.setBlockData(lan, false); }
    }
  }

  /** Sad wokół toru: proste drzewa owocowe poza poboczem (blockout — docelowy wygląd do zaprojektowania). */
  private void sadzDrzewa(World w, int[] o, Random r, double sciana) {
    for (int proba = 0; proba < 900; proba++) {
      int x = o[0] + 3 + r.nextInt(Math.max(1, o[2] - o[0] - 6)), z = o[1] + 3 + r.nextInt(Math.max(1, o[3] - o[1] - 6));
      if (rzut(x + 0.5, z + 0.5, -1, 0).odleglosc() < sciana + 4.5) continue;
      if (w.getBlockAt(x, y, z).getType() == Material.OAK_LOG) continue;
      if (r.nextInt(3) != 0) continue;
      boolean zajete = false;
      for (int a = -3; a <= 3 && !zajete; a++) for (int b = -3; b <= 3; b++)
        if (w.getBlockAt(x + a, y, z + b).getType() == Material.OAK_LOG) { zajete = true; break; }
      if (zajete) continue;
      int h = 3 + r.nextInt(2);
      for (int k = 0; k < h; k++) w.getBlockAt(x, y + k, z).setType(Material.OAK_LOG, false);
      Material lisc = r.nextInt(3) == 0 ? Material.FLOWERING_AZALEA_LEAVES : Material.AZALEA_LEAVES;
      for (int a = -2; a <= 2; a++) for (int b = -2; b <= 2; b++) for (int c = 0; c <= 2; c++) {
        if (Math.abs(a) + Math.abs(b) + c > 4 || (Math.abs(a) == 2 && Math.abs(b) == 2)) continue;
        Block l = w.getBlockAt(x + a, y + h - 1 + c, z + b);
        if (l.getType().isAir()) {
          l.setType(lisc, false);
          if (l.getBlockData() instanceof org.bukkit.block.data.type.Leaves leaves) { leaves.setPersistent(true); l.setBlockData(leaves, false); }
        }
      }
    }
  }
}
