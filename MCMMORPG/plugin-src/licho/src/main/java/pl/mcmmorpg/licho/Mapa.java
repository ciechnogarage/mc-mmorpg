package pl.mcmmorpg.licho;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Zagroda Zielarki z mapa.txt (generator: mapa/generuj.py). Pozycje sprzętów = miejsca zaczep w rundzie. */
final class Mapa {
  record Wpis(int x, int y, int z, String dane) {}

  final List<Wpis> bloki = new ArrayList<>();
  final int minX, minY, minZ, maxX, maxY, maxZ;

  Mapa(InputStream in, Logger log) {
    int[] r = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
    if (in != null) try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String l;
      while ((l = br.readLine()) != null) {
        if (l.isBlank() || l.startsWith("#")) continue;
        String[] c = l.split(" ", 4);
        Wpis w = new Wpis(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]), c[3]);
        bloki.add(w);
        r[0] = Math.min(r[0], w.x); r[1] = Math.min(r[1], w.y); r[2] = Math.min(r[2], w.z);
        r[3] = Math.max(r[3], w.x); r[4] = Math.max(r[4], w.y); r[5] = Math.max(r[5], w.z);
      }
    } catch (Exception e) { log.severe("Nie wczytano mapa.txt: " + e); }
    if (bloki.isEmpty()) r = new int[]{0, 0, 0, 0, 0, 0};
    minX = r[0]; minY = r[1]; minZ = r[2]; maxX = r[3]; maxY = r[4]; maxZ = r[5];
  }

  /** Czyści prostopadłościan mapy i stawia bloki bez fizyki. Jednorazowo z konsoli (~120 tys. zmian, ~1 s). */
  int zbuduj(World w) {
    BlockData powietrze = Material.AIR.createBlockData();
    for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY + 4; y++) for (int z = minZ; z <= maxZ; z++) {
      Block b = w.getBlockAt(x, y, z);
      if (!b.getType().isAir()) b.setBlockData(powietrze, false);
    }
    int n = 0;
    for (Wpis e : bloki) {
      w.getBlockAt(e.x, e.y, e.z).setBlockData(Bukkit.createBlockData(e.dane), false);
      n++;
    }
    return n;
  }

  /** Sprzęty z listy przebrań, które naprawdę stoją w świecie (ktoś mógł zmienić mapę ręcznie). */
  List<Block> sprzety(World w, java.util.Set<Material> rodzaje) {
    List<Block> l = new ArrayList<>();
    for (Wpis e : bloki) {
      Block b = w.getBlockAt(e.x, e.y, e.z);
      if (rodzaje.contains(b.getType())) l.add(b);
    }
    return l;
  }

  /** Zakres chunków mapy: x1, z1, x2, z2. */
  int[] chunki() { return new int[]{minX >> 4, minZ >> 4, maxX >> 4, maxZ >> 4}; }
}
