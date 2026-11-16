package pl.mcmmorpg.ksiega;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRecipeBookSettingsChangeEvent;

final class Ksiazka implements Listener {
  private final KsiegaPostaci plugin;
  Ksiazka(KsiegaPostaci plugin) { this.plugin = plugin; }

  @EventHandler public void settings(PlayerRecipeBookSettingsChangeEvent event) {
    if (!event.isOpen()) return;
    Player player = event.getPlayer();
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (!player.isOnline()) return;
      reset(player);
      player.closeInventory();
      String command = plugin.getConfig().getString("karta-komenda", "player");
      player.performCommand(command.startsWith("/") ? command.substring(1) : command);
    });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  void reset(Player player) {
    try {
      // Paper 1.21.11: te same refleksyjne wywołania co w sondzie okna E.
      Object handle = player.getClass().getMethod("getHandle").invoke(player);
      Object book = handle.getClass().getMethod("getRecipeBook").invoke(handle);
      Class<?> type = Class.forName("net.minecraft.world.inventory.RecipeBookType");
      Object crafting = Enum.valueOf((Class) type, "CRAFTING");
      book.getClass().getMethod("setOpen", type, boolean.class).invoke(book, crafting, false);
      Object settings = book.getClass().getMethod("getBookSettings").invoke(book);
      Object copy = settings.getClass().getMethod("copy").invoke(settings);
      Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket");
      Object packet = packetClass.getConstructor(settings.getClass()).newInstance(copy);
      Object connection = handle.getClass().getField("connection").get(handle);
      Class<?> packetInterface = Class.forName("net.minecraft.network.protocol.Packet");
      connection.getClass().getMethod("send", packetInterface).invoke(connection, packet);
    } catch (ReflectiveOperationException ex) {
      plugin.getLogger().warning("Nie udalo sie zamknac ksiazki receptur: " + ex);
    }
  }
}
