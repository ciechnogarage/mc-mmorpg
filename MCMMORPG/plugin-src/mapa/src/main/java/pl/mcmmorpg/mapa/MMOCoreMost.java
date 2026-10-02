package pl.mcmmorpg.mapa;

import net.Indyuce.mmocore.api.player.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

final class MMOCoreMost {
  private MMOCoreMost() {}

  static void actionBar(Player player, String message) {
    PlayerData data = PlayerData.get(player);
    if (data != null) data.displayActionBar(message);
    else player.sendActionBar(Component.text(message));
  }
}
