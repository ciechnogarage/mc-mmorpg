package pl.mcmmorpg.petcore;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

final class PetCorePlaceholderExpansion extends PlaceholderExpansion {
  private final PetCorePlugin plugin;

  PetCorePlaceholderExpansion(PetCorePlugin plugin) {
    this.plugin = plugin;
  }

  @Override public String getIdentifier() {
    return "petcore";
  }

  @Override public String getAuthor() {
    return "MCMMORPG";
  }

  @Override public String getVersion() {
    return plugin.getDescription().getVersion();
  }

  @Override public boolean persist() {
    return true;
  }

  @Override public String onPlaceholderRequest(Player player, String params) {
    if (player == null) return "";
    return plugin.uiStatusValue(player, params == null || params.isBlank() ? "summary" : params);
  }
}
