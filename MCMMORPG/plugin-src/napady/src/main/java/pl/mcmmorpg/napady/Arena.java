package pl.mcmmorpg.napady;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Arena w swiecie napadow (slot = przesuniecie ox). Uklad i drogi sa w {@link Uklad}; tu tylko swiat:
 * budowa (raz na zawsze, swiat trzyma ja na dysku), brama w blokach, chunki i bilety.
 * Symulacja nie dotyka swiata wcale: brama w blokach jest ustawiana do stanu napadu dopiero przy
 * materializacji cial, chunki laduja sie asynchronicznie i trzymaja bilet tylko, gdy stoja ciala.
 */
final class Arena {
  static final int Y = Uklad.Y, MUR_H = Uklad.MUR_H;
  static final int BRAMA_X = Uklad.BRAMA_X, BRAMA_Z0 = Uklad.BRAMA_Z0, BRAMA_Z1 = Uklad.BRAMA_Z1;
  static final int MAG_X0 = Uklad.MAG_X0, DRZWI_Z = Uklad.DRZWI_Z;

  final World swiat;
  final int ox, oz;
  /** stan drog: brama stoi albo zburzona (logika napadu, niezalezna od blokow) */
  Uklad uklad = Uklad.BRAMA_STOI;
  private boolean bilety;

  Arena(World swiat, int ox, int oz) { this.swiat = swiat; this.ox = ox; this.oz = oz; }

  Block blok(int x, int y, int z) { return swiat.getBlockAt(ox + x, y, oz + z); }

  Location loc(double x, double z) { return new Location(swiat, ox + x + 0.5, Y, oz + z + 0.5); }

  /** Chunki areny (x, z). */
  List<int[]> chunki() {
    List<int[]> l = new ArrayList<>();
    for (int cx = (ox + Uklad.MIN_X) >> 4; cx <= (ox + Uklad.MIN_X + Uklad.W) >> 4; cx++)
      for (int cz = (oz + Uklad.MIN_Z) >> 4; cz <= (oz + Uklad.MIN_Z + Uklad.H) >> 4; cz++) l.add(new int[]{cx, cz});
    return l;
  }

  /**
   * Laduje (i przy pierwszym razie generuje) chunki areny bez blokowania tiku (Paper World#getChunkAtAsync).
   * Bilety dokladaj potem w watku glownym ({@link #trzymaj}) - na zaladowany chunk bilet niczego nie laduje.
   */
  CompletableFuture<Void> zaladuj() {
    List<CompletableFuture<?>> f = new ArrayList<>();
    for (int[] c : chunki()) f.add(swiat.getChunkAtAsync(c[0], c[1], true));
    return CompletableFuture.allOf(f.toArray(new CompletableFuture[0]));
  }

  void trzymaj(Plugin p) {
    if (bilety) return;
    bilety = true;
    for (int[] c : chunki()) swiat.addPluginChunkTicket(c[0], c[1], p);
  }

  void zwolnij(Plugin p) {
    if (!bilety) return;
    bilety = false;
    for (int[] c : chunki()) swiat.removePluginChunkTicket(c[0], c[1], p);
  }

  boolean chunkiZaladowane() {
    for (int[] c : chunki()) if (!swiat.isChunkLoaded(c[0], c[1])) return false;
    return true;
  }

  /** Znacznik zbudowanej areny: narozniki muru i dach magazynu (wolac po zaladowaniu chunkow). */
  boolean zbudowana() {
    return blok(Uklad.MUR_X0, Y, Uklad.MUR_Z0).getType() == Material.STONE_BRICKS
        && blok(Uklad.MUR_X1, Y, Uklad.MUR_Z1).getType() == Material.STONE_BRICKS
        && blok(Uklad.MAG_X1, Y + MUR_H, Uklad.MAG_Z1).getType() == Material.SPRUCE_SLAB;
  }

  /** Budowa jednej kolumny x (49 x 8 blokow, ponizej 1 ms) - do kolejki pracy z budzetem na tik. */
  void zbudujKolumne(int x) {
    for (int z = Uklad.MIN_Z; z < Uklad.MIN_Z + Uklad.H; z++)
      for (int y = Y; y < Y + Uklad.WYS; y++) {
        Material m = Uklad.blok(x, y, z, true);
        Material cel = m == null ? Material.AIR : m;
        Block b = blok(x, y, z);
        if (b.getType() != cel) b.setType(cel, false);
      }
  }

  /** Brama w blokach zgodna ze stanem drog (9 blokow). */
  void ustawBrame() {
    Material m = uklad.bramaStoi ? Material.OAK_PLANKS : Material.AIR;
    for (int z = BRAMA_Z0; z <= BRAMA_Z1; z++)
      for (int y = Y; y < Y + 3; y++) {
        Block b = blok(BRAMA_X, y, z);
        if (b.getType() != m) b.setType(m, false);
      }
  }
}
