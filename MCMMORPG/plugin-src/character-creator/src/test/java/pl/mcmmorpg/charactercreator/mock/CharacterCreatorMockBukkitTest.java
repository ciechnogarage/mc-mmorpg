package pl.mcmmorpg.charactercreator.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.mcmmorpg.charactercreator.CharacterCreatorPlugin;
import pl.mcmmorpg.charactercreator.CreatorConfig;
import pl.mcmmorpg.charactercreator.command.CreatorCommand;
import pl.mcmmorpg.charactercreator.model.ModelEngineBridge;

/**
 * Testy na atrapie serwera (MockBukkit) — bez klona swiata. Patrz README.mockbukkit.md.
 *
 * Plugin NIE wstaje tu w calosci: wymaga prawdziwego MMOProfiles/ModelEngine (twarde
 * 'depend'), ktorych MockBukkit nie zasymuluje. Testujemy wiec to, co zalezy tylko od
 * Bukkita: ladowanie pluginu i configu, walidacje swiata kreatora, komende /cc.
 */
class CharacterCreatorMockBukkitTest {

  private ServerMock server;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private static String text(net.kyori.adventure.text.Component c) {
    return PlainTextComponentSerializer.plainText().serialize(c);
  }

  /** (a) Bez swiata 'creator_limbo' plugin sam sie wylacza z FATAL, zamiast rzucac NPE. */
  @Test
  void pluginLoadsAndDisablesItselfWithoutCreatorWorld() {
    CharacterCreatorPlugin plugin = MockBukkit.load(CharacterCreatorPlugin.class);
    assertNotNull(plugin);
    assertFalse(plugin.isEnabled(), "bez swiata kreatora plugin powinien sie wylaczyc");
  }

  /** (b) Domyslny config.yml z jara jest poprawny: ze swiatem nie ma zadnego FATAL. */
  @Test
  void bundledConfigHasNoFatalProblemsWhenWorldExists() {
    CharacterCreatorPlugin plugin = MockBukkit.load(CharacterCreatorPlugin.class);
    server.addSimpleWorld("creator_limbo");
    CreatorConfig config = new CreatorConfig(plugin);
    config.reload();
    assertFalse(config.races().isEmpty(), "config.yml powinien definiowac rasy");
    assertFalse(config.classes().isEmpty(), "config.yml powinien definiowac klasy");

    List<CreatorConfig.Problem> fatal =
        config.validate(new ModelEngineBridge(plugin.getLogger())).stream()
            .filter(p -> p.severity() == CreatorConfig.Severity.FATAL).toList();
    assertTrue(fatal.isEmpty(), "FATAL w domyslnym configu: " + fatal);
  }

  /** (c) /cc: gracz bez uprawnienia dostaje odmowe, admin dostaje pomoc. */
  @Test
  void adminCommandChecksPermission() {
    CharacterCreatorPlugin plugin = MockBukkit.load(CharacterCreatorPlugin.class);
    CreatorConfig config = new CreatorConfig(plugin);
    CreatorCommand cmd = new CreatorCommand(config, null, null, null, null, () -> { });
    Command dummy = plugin.getServer().getPluginCommand("charactercreator");
    // Komenda jest w plugin.yml, ale plugin sie wylaczyl przed jej podpieciem — executor
    // wywolujemy wiec bezposrednio, a metadane (alias, uprawnienie) sprawdzamy z yml.
    assertEquals("charactercreator.admin", CreatorCommand.PERMISSION);

    PlayerMock player = server.addPlayer("Zwykly");
    assertFalse(player.hasPermission(CreatorCommand.PERMISSION));
    assertTrue(cmd.onCommand(player, dummy, "cc", new String[] {"reload"}));
    assertTrue(text(player.nextComponentMessage()).contains("Brak uprawnień"));

    PlayerMock admin = server.addPlayer("Admin");
    // default: op z plugin.yml — test pilnuje tez, ze uprawnienie jest zadeklarowane.
    admin.setOp(true);
    assertTrue(admin.hasPermission(CreatorCommand.PERMISSION));
    assertTrue(cmd.onCommand(admin, dummy, "cc", new String[] {}));
    assertTrue(text(admin.nextComponentMessage()).contains("/cc reload"));
  }
}
