package pl.mcmmorpg.mapa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class MapaDataTest {
  @Test
  void instanceWorldUsesTemplateKey() {
    assertEquals("loch_001", MapaData.klucz("loch_001_0"));
    assertEquals("world", MapaData.klucz("world"));
    assertEquals("stolica_odnowa", MapaData.klucz("stolica_odnowa"));
  }

  @Test
  void texturePixelsUseFloorDivisionAndModulus() {
    byte[] pixels = {7, 7, 7, 7};
    byte[] blocks = {1, (byte) 0x80, 1, (byte) 0x80, 1, (byte) 0x80, 1, (byte) 0x80};
    byte[] textures = new byte[96];
    for (int i = 0; i < 16; i++) textures[80 + i] = (byte) (i + 1);
    MapaData.Terrain terrain = new MapaData.Terrain(-1, -1, 2, 2, pixels, blocks, textures);

    assertEquals(16, Byte.toUnsignedInt(terrain.pixel(0, 0, 0.25, 63, 63)));
    assertEquals(11, Byte.toUnsignedInt(terrain.pixel(0, 0, 0.5, 63, 63)));
    assertEquals(1, Byte.toUnsignedInt(terrain.pixel(0, 0, 0.25, 64, 64)));
    assertEquals(7, Byte.toUnsignedInt(new MapaData.Terrain(-1, -1, 2, 2,
        pixels, null, textures).pixel(0, 0, 0.25, 63, 63)));
    blocks[0] = 0;
    blocks[1] = 0;
    assertEquals(7, Byte.toUnsignedInt(terrain.pixel(0, 0, 0.25, 63, 63)));
  }

  @Test
  void tabOutsideConfiguredListIsEmpty() {
    List<MapaData.Tab> tabs = List.of(new MapaData.Tab("world", "Kraina", "minecraft:grass_block"));
    assertEquals("world", MapaData.tab(tabs, 0).world());
    assertNull(MapaData.tab(tabs, 1));
    assertNull(MapaData.tab(tabs, 9));
  }
}
