package pl.mcmmorpg.minioncore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Structural safety contract for the source MythicMobs definition shipped with T3. */
public final class MinionMeleeDefinitionTest {
  public static void main(String[] args) throws IOException {
    String definition = Files.readString(Path.of(
        "..", "..", "plugins", "MythicMobs", "mobs", "minion-melee-tier1.yml"));

    assertSafeDefinition(definition, "minion_melee_tier1:");
    assertSafeDefinition(Files.readString(Path.of(
        "..", "..", "plugins", "MythicMobs", "mobs", "minion-ranged-tier1.yml")), "minion_ranged_tier1:");
    String support = Files.readString(Path.of(
        "..", "..", "plugins", "MythicMobs", "mobs", "minion-support-tier1.yml"));
    assertSafeDefinition(support, "minion_support_tier1:");
    check(!support.contains("damage{"), "support must not contain player-facing AoE damage");
    System.out.println("MINION_DEFINITION_TEST_PASS");
  }

  private static void assertSafeDefinition(String definition, String typeId) {
    check(definition.contains(typeId), "type ID must match the adapter contract: " + typeId);
    check(definition.contains("NoAI: true"), "spawn definition must be non-combat until activation");
    check(!definition.contains("~onSpawn"), "provisional definition must not run spawn skills");
    check(!definition.contains("~onTimer"), "provisional definition must not create timer work");
    check(!definition.contains("Skills:"), "T3 melee definition must not contain player-facing skills");
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
