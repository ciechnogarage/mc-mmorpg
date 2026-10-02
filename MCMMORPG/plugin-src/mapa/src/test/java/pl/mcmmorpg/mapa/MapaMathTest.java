package pl.mcmmorpg.mapa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MapaMathTest {
  @Test
  void directionsWrapAroundAndRound() {
    assertEquals(0, MapaMath.modelDirection(0));
    assertEquals(2, MapaMath.modelDirection(90));
    assertEquals(4, MapaMath.modelDirection(180));
    assertEquals(6, MapaMath.modelDirection(270));
    assertEquals(7, MapaMath.modelDirection(-45));
    assertEquals(0, MapaMath.modelDirection(360));
    assertEquals(4, MapaMath.cursorDirection(90));
    assertEquals(8, MapaMath.cursorDirection(180));
    assertEquals(12, MapaMath.cursorDirection(270));
    assertEquals(15, MapaMath.cursorDirection(-22.5f));
  }

  @Test
  void cursorCoordinatesAndRange() {
    assertEquals(0, MapaMath.cursorCoordinate(149, 149, 2));
    assertEquals(127, MapaMath.cursorCoordinate(276, 149, 2));
    assertEquals(-128, MapaMath.cursorCoordinate(21, 149, 2));
    assertTrue(MapaMath.cursorInRange(21, 149, 2));
    assertFalse(MapaMath.cursorInRange(277, 149, 2));
    assertEquals(4, MapaMath.cursorCoordinate(150, 149, 0.5));
    assertTrue(MapaMath.cursorInRange(150, 149, 0.5));
  }

  @Test
  void recenterOnlyAfterFortyMapPixels() {
    for (double zoom : new double[] {0.25, 1, 4}) {
      assertFalse(MapaMath.shouldRecenter(40 * zoom, 0, 0, 0, zoom));
      assertFalse(MapaMath.shouldRecenter(0, -40 * zoom, 0, 0, zoom));
      assertTrue(MapaMath.shouldRecenter(40 * zoom + 0.01, 0, 0, 0, zoom));
      assertTrue(MapaMath.shouldRecenter(0, -40 * zoom - 0.01, 0, 0, zoom));
    }
  }

  @Test
  void playerCursorMovesAroundFixedCenter() {
    assertEquals(0, MapaMath.cursorCoordinate(100.0, 100, 1));
    assertEquals(127, MapaMath.cursorCoordinate(163.5, 100, 1));
    assertEquals(-128, MapaMath.cursorCoordinate(36.0, 100, 1));
    assertEquals(-4, MapaMath.cursorCoordinate(-0.5, 0, 0.25));
    assertEquals(80, MapaMath.cursorCoordinate(10.0, 0, 0.25));
  }

  @Test
  void slotsAndPanning() {
    assertEquals(5, MapaMath.tileX(0, 5));
    assertEquals(13, MapaMath.tileX(44, 5));
    assertEquals(7, MapaMath.tileZ(44, 4));
    assertEquals(4, MapaMath.tileZ(9, 4));
    assertEquals(7, MapaMath.tileZ(36, 4));
    assertEquals(-4, MapaMath.clamp(-20, -4, 17));
    assertEquals(17, MapaMath.clamp(40, -4, 17));
  }

  @Test
  void zoomPreservesViewCenterWithinTilePrecision() {
    int origin = -368;
    int previousBpp = 6;
    int nextBpp = 3;
    double center = MapaMath.centerX(5, origin, previousBpp);
    int nextLeft = MapaMath.leftForCenter(center, origin, nextBpp, 22);
    double after = MapaMath.centerX(nextLeft, origin, nextBpp);
    assertTrue(Math.abs(center - after) <= 9.0 * nextBpp);

    double centerZ = MapaMath.centerZ(4, -656, previousBpp);
    int nextTop = MapaMath.topForCenter(centerZ, -656, nextBpp, 27);
    assertTrue(Math.abs(centerZ - MapaMath.centerZ(nextTop, -656, nextBpp)) <= 9.0 * nextBpp);
  }
}
