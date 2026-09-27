package pl.mcmmorpg.charactercreator.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class SceneSettingsTest {

  private static SceneSettings withPoints(Map<String, SceneSettings.Point> points) {
    return new SceneSettings("creator_limbo", points, "", 5, 5, 10, 1.4, 5,
        List.of(), List.of(), 40, List.of());
  }

  private static SceneSettings.Point point(double z) {
    return new SceneSettings.Point(new Location(null, 0, 101.62, z), new Location(null, 0, 100, z - 3), null);
  }

  @Test
  void kadr_rasy_bez_wpisu_wraca_do_domyslnego() {
    SceneSettings.Point def = point(0);
    SceneSettings.Point ork = point(50);
    SceneSettings s = withPoints(Map.of(SceneSettings.DEFAULT_POINT, def, "ork", ork));
    assertSame(ork, s.point("ork"));
    assertSame(def, s.point("elf"));
    assertSame(def, s.point(null));
  }

  @Test
  void bez_kadru_domyslnego_scena_jest_nieuzywalna() {
    assertFalse(withPoints(Map.of("ork", point(0))).usable());
    assertTrue(withPoints(Map.of(SceneSettings.DEFAULT_POINT, point(0))).usable());
  }

  @Test
  void kamera_patrzy_na_cel() {
    // Cel na polnoc (-Z) i na wysokosci kamery: yaw 180, pitch 0.
    Location aimed = SceneSettings.aimed(new Location(null, 0, 101, 0), new Location(null, 0, 101, -3));
    assertEquals(180f, Math.abs(aimed.getYaw()), 0.01f);
    assertEquals(0f, aimed.getPitch(), 0.01f);
    // Cel nizej: kamera patrzy w dol (dodatni pitch w Minecrafcie).
    Location down = SceneSettings.aimed(new Location(null, 0, 102, 0), new Location(null, 0, 101, -3));
    assertTrue(down.getPitch() > 0);
  }
}
