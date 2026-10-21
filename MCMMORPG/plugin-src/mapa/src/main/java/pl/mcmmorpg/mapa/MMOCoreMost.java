package pl.mcmmorpg.mapa;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
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

  /** Nawigacja: priorytet 20 MythicLib (nad paskiem MMOCore, pod odmowami), 40 ticków - odświeżana co 30 bez migania. */
  static void nawigacja(Player player, String message) {
    MMOPlayerData.get(player).getActionBar().show(20, 40L, message);
  }
}
