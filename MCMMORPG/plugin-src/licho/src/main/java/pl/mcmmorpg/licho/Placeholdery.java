package pl.mcmmorpg.licho;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/** %licho_tytul%, %licho_postep_licho%, %licho_postep_zamawiacz%, %licho_min_graczy% dla menu 'licho' (przycisk Rekordy). */
final class Placeholdery extends PlaceholderExpansion {
  private final LichoPlugin plugin;

  Placeholdery(LichoPlugin plugin) { this.plugin = plugin; }

  @Override public String getIdentifier() { return "licho"; }
  @Override public String getAuthor() { return "mcmmorpg"; }
  @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
  @Override public boolean persist() { return true; }

  @Override
  public String onRequest(OfflinePlayer op, String param) {
    if (op == null) return "";
    return switch (param) {
      case "tytul" -> { String t = plugin.rekordy.tytul(op.getUniqueId()); yield t == null ? "&7jeszcze żaden" : "&6«" + t + "»"; }
      case "postep_licho" -> or(plugin.rekordy.postep(op.getUniqueId(), true));
      case "postep_zamawiacz" -> or(plugin.rekordy.postep(op.getUniqueId(), false));
      case "min_graczy" -> String.valueOf(plugin.u.tytulyMinGraczy);
      default -> null;
    };
  }

  private static String or(String s) { return s.isEmpty() ? "&awszystkie zdobyte" : "&f" + s; }
}
