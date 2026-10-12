package pl.mcmmorpg.ksiega;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** Stan portalu dla dymków MENU. */
final class PortalPlaceholdery extends PlaceholderExpansion {
  private final KsiegaPostaci plugin;

  PortalPlaceholdery(KsiegaPostaci plugin) { this.plugin = plugin; }

  @Override public String getIdentifier() { return "ksiegapostaci"; }
  @Override public String getAuthor() { return "mcmmorpg"; }
  @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
  @Override public boolean persist() { return true; }

  @Override public String onRequest(OfflinePlayer offline, String key) {
    if (!key.equals("odkryte_punkty")) return null;
    return offline instanceof Player player ? String.valueOf(plugin.portal.odkryte(player)) : "0";
  }
}
