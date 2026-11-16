package pl.mcmmorpg.lobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.noise.SimplexNoiseGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Buduje wyspe lobby w pustym swiecie. Wszystko jest wyliczane z ziarna, wiec
 * {@code /lobby przebuduj} zawsze stawia te sama wyspe - nie ma recznie zbudowanej
 * mapy, ktora mozna zgubic.
 *
 * Uklad (srodek 0,0, powierzchnia y=64, spawn na poludnie od fontanny, patrzy na polnoc):
 * plac z fontanna (r 7), trzy sciezki do portali (polnoc/wschod/zachod, r 20),
 * czwarta na poludnie do stawu, z ktorego strumien spada z krawedzi wodospadem.
 */
final class IslandBuilder {

  static final int TOP = 64;
  static final int RADIUS = 30;
  static final int PLAZA = 7;
  static final int PORTAL_DIST = 20;
  /** Wyrzutnia na poczatku glownej sciezki - rzuca pod portal glowny. */
  static final int PAD_Z = -9;

  private static final long SEED = 0x10bb7L;

  private final World world;
  private final SimplexNoiseGenerator noise = new SimplexNoiseGenerator(SEED);
  private final Random rnd = new Random(SEED);
  /** Wierzch kazdej kolumny glownej wyspy (klucz x,z) - do drzew, kwiatow i wodospadu. */
  private final Map<Long, Integer> tops = new HashMap<>();

  IslandBuilder(World world) {
    this.world = world;
  }

  void build() {
    clear();
    island(0, TOP, 0, RADIUS, 26, true);
    plaza();
    for (PortalSlot slot : PortalSlot.values()) {
      path(slot.dirX, slot.dirZ, PLAZA + 1, PORTAL_DIST - 4);
      portalFrame(slot);
    }
    world.setType(0, TOP + 1, PAD_Z, Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    world.setType(0, TOP, PAD_Z, Material.GOLD_BLOCK);
    int pondZ = southPondAndWaterfall();
    path(0, 1, PLAZA + 1, pondZ - 3);
    lampPosts(pondZ);
    trees();
    flowers();
    miniIslands();
  }

  // ------------------------------------------------------------------ teren

  private void clear() {
    BlockData air = Material.AIR.createBlockData();
    for (int x = -72; x <= 72; x++) {
      for (int z = -72; z <= 72; z++) {
        for (int y = 20; y <= 100; y++) {
          if (!world.getType(x, y, z).isAir()) world.setBlockData(x, y, z, air);
        }
      }
    }
  }

  /**
   * Latajaca wyspa: plaski wierzch z lagodnymi pagorkami, spod zwezajacy sie w sople.
   * {@code main} = glowna wyspa (rezerwuje plac i sciezki, zapisuje wierzch kolumn).
   */
  private void island(int cx, int top, int cz, int radius, int maxDepth, boolean main) {
    int reach = radius + 8;
    double salt = cx * 0.37 + cz * 0.71;
    for (int dx = -reach; dx <= reach; dx++) {
      for (int dz = -reach; dz <= reach; dz++) {
        int x = cx + dx;
        int z = cz + dz;
        double r = Math.hypot(dx, dz);
        double edge = radius
            + noise.noise(x * 0.06, z * 0.06, salt) * radius * 0.17
            + noise.noise(x * 0.2, z * 0.2, salt + 5) * 1.5;
        if (r >= edge) continue;
        double t = 1 - r / edge; // 0 na krawedzi, 1 w srodku

        int mound = 0;
        if (main) {
          double free = freeWeight(x, z);
          double rim = clamp((edge - r) / 4.0);
          mound = (int) Math.round(Math.max(0, noise.noise(x * 0.06, z * 0.06, 3) - 0.05) * 6 * free * rim);
        } else {
          mound = (int) Math.round(Math.max(0, noise.noise(x * 0.15, z * 0.15, salt + 2)) * 2 * t);
        }
        int colTop = top + mound;
        int depth = (int) Math.round(3 + Math.pow(t, 0.7) * maxDepth
            + noise.noise(x * 0.15, z * 0.15, salt + 7) * 3 * t);
        int bottom = top - depth;
        int dirt = t < 0.1 ? 1 : 3;

        for (int y = bottom; y <= colTop; y++) {
          int below = colTop - y;
          Material m;
          if (below == 0) m = Material.GRASS_BLOCK;
          else if (below <= dirt) m = Material.DIRT;
          else if (y - bottom < 2) m = rnd.nextInt(3) == 0 ? Material.DEEPSLATE : Material.COBBLED_DEEPSLATE;
          else m = stone();
          world.setType(x, y, z, m);
        }
        if (rnd.nextDouble() < 0.12) world.setType(x, bottom - 1, z, Material.HANGING_ROOTS);
        if (main) tops.put(key(x, z), colTop);
      }
    }
  }

  private Material stone() {
    int roll = rnd.nextInt(100);
    if (roll < 58) return Material.STONE;
    if (roll < 74) return Material.ANDESITE;
    if (roll < 84) return Material.TUFF;
    if (roll < 92) return Material.COBBLESTONE;
    if (roll < 96) return Material.COAL_ORE;
    if (roll < 98) return Material.COPPER_ORE;
    return Material.IRON_ORE;
  }

  /**
   * 1 = teren wolny (moga byc pagorki i drzewa), 0 = plac/sciezka/portal.
   * Miedzy nimi 3 bloki lagodnego przejscia, zeby pagorek nie konczyl sie scianka przy sciezce.
   */
  static double freeWeight(int x, int z) {
    double w = clamp((Math.hypot(x, z) - (PLAZA + 2)) / 3.0);
    for (PortalSlot slot : PortalSlot.values()) {
      w = Math.min(w, corridor(x, z, slot.dirX, slot.dirZ, 3, PORTAL_DIST + 5, 5));
    }
    w = Math.min(w, corridor(x, z, 0, 1, 3, RADIUS + 10, 3));
    return w;
  }

  /** Waga wolnosci wzgledem korytarza od srodka w strone (dirX,dirZ) o polszerokosci halfWidth. */
  private static double corridor(int x, int z, int dirX, int dirZ, int halfWidth, int length, int endHalfWidth) {
    double along = x * dirX + z * dirZ;
    double perp = Math.abs(x * dirZ - z * dirX);
    if (along < 0 || along > length + 3) return 1;
    double hw = along > PORTAL_DIST - 6 ? endHalfWidth : halfWidth;
    double outside = Math.max(perp - hw, along - length);
    return clamp(outside / 3.0);
  }

  // ------------------------------------------------------------------ plac i fontanna

  private void plaza() {
    for (int x = -PLAZA - 1; x <= PLAZA + 1; x++) {
      for (int z = -PLAZA - 1; z <= PLAZA + 1; z++) {
        double r = Math.hypot(x, z);
        if (r > PLAZA + 0.5) continue;
        Material m;
        if (r > PLAZA - 0.5) m = rnd.nextInt(4) == 0 ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS;
        else if (r <= 2.3) m = Material.WATER;
        else if (r <= 3.3) m = Material.STONE_BRICKS;
        else m = ((int) Math.round(r)) % 2 == 0 ? Material.POLISHED_ANDESITE : Material.SMOOTH_STONE;
        world.setType(x, TOP, z, m);
        if (m == Material.WATER) world.setType(x, TOP - 1, z, Material.PRISMARINE_BRICKS);
        if (r > 2.3 && r <= 3.3) world.setBlockData(x, TOP + 1, z, Bukkit.createBlockData("minecraft:stone_brick_slab[type=bottom]"));
      }
    }
    // kolumna w fontannie ze swiatlem na szczycie
    for (int y = TOP - 1; y <= TOP + 2; y++) world.setType(0, y, 0, Material.CHISELED_STONE_BRICKS);
    world.setType(0, TOP + 3, 0, Material.SEA_LANTERN);
    world.setBlockData(0, TOP + 4, 0, Bukkit.createBlockData("minecraft:end_rod[facing=up]"));
    // swiatlo pod woda, zeby fontanna swiecila o zachodzie
    for (int[] p : new int[][] {{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) world.setType(p[0], TOP - 1, p[1], Material.SEA_LANTERN);
  }

  // ------------------------------------------------------------------ sciezki i latarnie

  private void path(int dirX, int dirZ, int from, int to) {
    for (int d = from; d <= to; d++) {
      for (int off = -2; off <= 2; off++) {
        if (Math.abs(off) == 2 && rnd.nextDouble() > 0.3) continue;
        int x = dirX * d + dirZ * off;
        int z = dirZ * d + dirX * off;
        Integer top = tops.get(key(x, z));
        if (top == null) continue;
        world.setType(x, top, z, rnd.nextInt(9) == 0 ? Material.COARSE_DIRT : Material.DIRT_PATH);
        world.setType(x, top + 1, z, Material.AIR);
      }
    }
  }

  private void lampPosts(int pondZ) {
    List<int[]> posts = new ArrayList<>();
    for (PortalSlot slot : PortalSlot.values()) {
      for (int d : new int[] {10, 14}) {
        for (int side : new int[] {-3, 3}) posts.add(new int[] {slot.dirX * d + slot.dirZ * side, slot.dirZ * d + slot.dirX * side});
      }
    }
    for (int d = 10; d < pondZ - 3; d += 5) {
      for (int side : new int[] {-3, 3}) posts.add(new int[] {side, d});
    }
    for (int[] p : posts) {
      Integer top = tops.get(key(p[0], p[1]));
      if (top == null) continue;
      world.setType(p[0], top + 1, p[1], Material.SPRUCE_FENCE);
      world.setType(p[0], top + 2, p[1], Material.SPRUCE_FENCE);
      world.setBlockData(p[0], top + 3, p[1], Bukkit.createBlockData("minecraft:lantern[hanging=false]"));
    }
  }

  // ------------------------------------------------------------------ portale

  private void portalFrame(PortalSlot slot) {
    // podest przed i za lukiem
    for (int a = -4; a <= 4; a++) {
      for (int n = -3; n <= 4; n++) {
        int x = slot.x(a, n);
        int z = slot.z(a, n);
        int roll = rnd.nextInt(10);
        world.setType(x, TOP, z, roll < 6 ? Material.STONE_BRICKS : roll < 8 ? Material.MOSSY_STONE_BRICKS : Material.CRACKED_STONE_BRICKS);
        for (int y = TOP + 1; y <= TOP + 9; y++) world.setType(x, y, z, Material.AIR);
        for (int y = TOP - 3; y < TOP; y++) if (world.getType(x, y, z).isAir()) world.setType(x, y, z, Material.STONE);
      }
    }
    boolean main = slot == PortalSlot.POLNOC;
    for (int a = -2; a <= 2; a++) {
      for (int h = 0; h <= 5; h++) {
        boolean border = Math.abs(a) == 2 || h == 0 || h == 5;
        if (!border) continue;
        boolean corner = Math.abs(a) == 2 && (h == 0 || h == 5);
        Material m;
        if (main) m = corner ? Material.GILDED_BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS;
        else m = corner ? Material.CHISELED_STONE_BRICKS : (rnd.nextInt(3) == 0 ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS);
        world.setType(slot.x(a, 0), TOP + h, slot.z(a, 0), m);
      }
    }
    // korona
    Material crown = main ? Material.CHISELED_POLISHED_BLACKSTONE : Material.CHISELED_STONE_BRICKS;
    String slab = main ? "polished_blackstone_brick_slab" : "stone_brick_slab";
    world.setType(slot.x(0, 0), TOP + 6, slot.z(0, 0), crown);
    for (int a : new int[] {-1, 1}) {
      world.setBlockData(slot.x(a, 0), TOP + 6, slot.z(a, 0), Bukkit.createBlockData("minecraft:" + slab + "[type=bottom]"));
    }
    world.setBlockData(slot.x(0, 0), TOP + 7, slot.z(0, 0),
        Bukkit.createBlockData(main ? "minecraft:soul_lantern[hanging=false]" : "minecraft:lantern[hanging=false]"));
    // latarnie po bokach wejscia
    for (int a : new int[] {-4, 4}) {
      world.setType(slot.x(a, 2), TOP + 1, slot.z(a, 2), main ? Material.POLISHED_BLACKSTONE_WALL : Material.STONE_BRICK_WALL);
      world.setBlockData(slot.x(a, 2), TOP + 2, slot.z(a, 2),
          Bukkit.createBlockData(main ? "minecraft:soul_lantern[hanging=false]" : "minecraft:lantern[hanging=false]"));
    }
  }

  /** Tafla portalu (albo pustka dla luku "Wkrotce"). Wolane przy kazdym starcie - zalezy od configu. */
  static void applyFill(World world, PortalSlot slot, boolean active) {
    BlockData fill = active
        ? Bukkit.createBlockData("minecraft:nether_portal[axis=" + (slot.dirX == 0 ? "x" : "z") + "]")
        : Material.AIR.createBlockData();
    for (int a = -1; a <= 1; a++) {
      for (int h = 1; h <= 4; h++) {
        world.getBlockAt(slot.x(a, 0), TOP + h, slot.z(a, 0)).setBlockData(fill, false);
      }
    }
  }

  // ------------------------------------------------------------------ staw i wodospad

  /** Staw na koncu poludniowej sciezki i strumien spadajacy z krawedzi. Zwraca z stawu. */
  private int southPondAndWaterfall() {
    int edgeZ = 0;
    while (tops.containsKey(key(0, edgeZ + 1)) && tops.containsKey(key(-1, edgeZ + 1)) && tops.containsKey(key(1, edgeZ + 1))) edgeZ++;
    int pondZ = edgeZ - 6;
    for (int x = -4; x <= 4; x++) {
      for (int z = pondZ - 4; z <= pondZ + 4; z++) {
        double r = Math.hypot(x, z - pondZ);
        Integer top = tops.get(key(x, z));
        if (top == null) continue;
        if (r <= 2.3) {
          world.setType(x, TOP, z, Material.WATER);
          world.setType(x, TOP - 1, z, Material.GRAVEL);
          for (int y = TOP + 1; y <= top + 1; y++) world.setType(x, y, z, Material.AIR);
          tops.put(key(x, z), TOP);
        } else if (r <= 3.4 && rnd.nextInt(2) == 0) {
          world.setType(x, top, z, Material.MOSSY_COBBLESTONE);
        }
      }
    }
    for (int z = pondZ + 2; z <= edgeZ; z++) {
      world.setType(0, TOP, z, Material.WATER);
      world.setType(0, TOP - 1, z, Material.GRAVEL);
      world.setType(0, TOP + 1, z, Material.AIR);
      for (int side : new int[] {-1, 1}) {
        world.setType(side, TOP, z, Material.MOSSY_COBBLESTONE);
        world.setType(side, TOP + 1, z, Material.AIR);
      }
      tops.put(key(0, z), TOP);
    }
    // zrodlo tuz za krawedzia: z powietrzem pod spodem plynie tylko w dol
    world.getBlockAt(0, TOP, edgeZ + 1).setType(Material.WATER, true);
    return pondZ;
  }

  // ------------------------------------------------------------------ roslinnosc

  private void trees() {
    List<int[]> placed = new ArrayList<>();
    for (int attempt = 0; attempt < 600 && placed.size() < 13; attempt++) {
      double ang = rnd.nextDouble() * Math.PI * 2;
      double r = 11 + rnd.nextDouble() * (RADIUS - 15);
      int x = (int) Math.round(Math.cos(ang) * r);
      int z = (int) Math.round(Math.sin(ang) * r);
      Integer top = tops.get(key(x, z));
      if (top == null || freeWeight(x, z) < 0.95 || world.getType(x, top, z) != Material.GRASS_BLOCK) continue;
      boolean crowded = false;
      for (int[] p : placed) if (Math.hypot(p[0] - x, p[1] - z) < 7) crowded = true;
      if (crowded) continue;
      int roll = rnd.nextInt(100);
      TreeType type = roll < 55 ? TreeType.CHERRY : roll < 80 ? TreeType.BIRCH : TreeType.TREE;
      if (world.generateTree(new Location(world, x, top + 1, z), rnd, type)) placed.add(new int[] {x, z});
    }
  }

  private static final Material[] FLOWERS = {
      Material.POPPY, Material.DANDELION, Material.CORNFLOWER, Material.ALLIUM,
      Material.AZURE_BLUET, Material.OXEYE_DAISY, Material.LILY_OF_THE_VALLEY,
  };

  private void flowers() {
    for (Map.Entry<Long, Integer> e : tops.entrySet()) {
      int x = (int) (e.getKey() >> 32);
      int z = (int) (long) e.getKey();
      int top = e.getValue();
      if (world.getType(x, top, z) != Material.GRASS_BLOCK || !world.getType(x, top + 1, z).isAir()) continue;
      if (freeWeight(x, z) < 0.4) continue;
      double roll = rnd.nextDouble();
      Material m = null;
      if (roll < 0.03) m = Material.FIREFLY_BUSH;
      else if (roll < 0.20) m = Material.SHORT_GRASS;
      else if (roll < 0.26) m = Material.PINK_PETALS;
      else if (roll < 0.30) m = Material.WILDFLOWERS;
      else if (roll < 0.36) m = FLOWERS[rnd.nextInt(FLOWERS.length)];
      if (m != null) world.setType(x, top + 1, z, m);
    }
  }

  private void miniIslands() {
    int[][] spots = {
        // x, top, z, promien
        {44, 70, -30, 6}, {-48, 60, -18, 5}, {-34, 74, 38, 7}, {40, 57, 34, 4}, {8, 78, -52, 4},
    };
    for (int[] s : spots) {
      island(s[0], s[1], s[2], s[3], s[3] * 2, false);
      if (s[3] >= 6) {
        world.generateTree(new Location(world, s[0], s[1] + 1, s[2]), rnd, TreeType.CHERRY);
      } else {
        world.setType(s[0], s[1] + 1, s[2], Material.FIREFLY_BUSH);
        world.setType(s[0] + 1, s[1] + 1, s[2], Material.PINK_PETALS);
      }
    }
  }

  // ------------------------------------------------------------------ pomocnicze

  private static long key(int x, int z) {
    return ((long) x << 32) | (z & 0xffffffffL);
  }

  private static double clamp(double v) {
    return Math.max(0, Math.min(1, v));
  }
}
