package pl.mcmmorpg.chowance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mcmmorpg.chowance.logika.Wybieg;

class WybiegTest {
  @Test void dopasowujePoczatekNazwySpawnera() {
    assertTrue(Wybieg.spawnerPasuje("chw_wybieg_swistek", List.of("chw_wybieg_")));
    assertTrue(Wybieg.spawnerPasuje("tutorial_grudnik", List.of("chw_wybieg_", "tutorial_")));
    assertFalse(Wybieg.spawnerPasuje("inne_chw_wybieg_swistek", List.of("chw_wybieg_")));
    assertFalse(Wybieg.spawnerPasuje("chw_wybieg_swistek", List.of("")));
    assertFalse(Wybieg.spawnerPasuje(null, List.of("chw_wybieg_")));
    assertFalse(Wybieg.spawnerPasuje("chw_wybieg_swistek", List.of()));
  }
}
