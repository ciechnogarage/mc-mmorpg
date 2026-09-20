package pl.mcmmorpg.charactercreator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CreatorConfigTest {

  @Test
  void parsesSixDigitHex() {
    Color color = CreatorConfig.parseColor("#C8B48C");
    assertEquals(0xC8, color.getRed());
    assertEquals(0xB4, color.getGreen());
    assertEquals(0x8C, color.getBlue());
  }

  @Test
  void hexIsCaseInsensitive() {
    assertEquals(CreatorConfig.parseColor("#c8b48c"), CreatorConfig.parseColor("#C8B48C"));
  }

  /** Bledny wpis daje null (rasa bez helmu), a nie wyjatek wysadzajacy caly reload configu. */
  @ParameterizedTest
  @ValueSource(strings = {"", "C8B48C", "#C8B48", "#GGGGGG", "#C8B48CC", "biały"})
  void malformedColorsYieldNull(String hex) {
    assertNull(CreatorConfig.parseColor(hex));
  }

  @Test
  void nullColorYieldsNull() {
    assertNull(CreatorConfig.parseColor(null));
  }
}
