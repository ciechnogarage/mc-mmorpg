package pl.mcmmorpg.napady;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/** Swiat napadow: bedrock y=-64, ziemia do -61, trawa -60 (stoi sie na -59, jak Arena.Y). */
final class PlaskiGenerator extends ChunkGenerator {
  @Override
  public void generateSurface(WorldInfo w, Random r, int cx, int cz, ChunkData d) {
    for (int x = 0; x < 16; x++)
      for (int z = 0; z < 16; z++) {
        d.setBlock(x, -64, z, Material.BEDROCK);
        for (int y = -63; y < -60; y++) d.setBlock(x, y, z, Material.DIRT);
        d.setBlock(x, -60, z, Material.GRASS_BLOCK);
      }
  }

  @Override public boolean shouldGenerateNoise() { return false; }
  @Override public boolean shouldGenerateSurface() { return false; }
  @Override public boolean shouldGenerateCaves() { return false; }
  @Override public boolean shouldGenerateDecorations() { return false; }
  @Override public boolean shouldGenerateMobs() { return false; }
  @Override public boolean shouldGenerateStructures() { return false; }
}
