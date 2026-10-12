package pl.mcmmorpg.zaklecia;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * %zaklecia_ulubione_<id>% → "★" albo "☆"; %zaklecia_prawa% / %zaklecia_lewa% → nazwa zaklecia w dloni albo "pusta".
 * Uzywane w Ksiedze zaklec (DeluxeMenus).
 */
final class Placeholdery extends PlaceholderExpansion {
  private final Ksiega ksiega;
  private final Ulubione ulubione;

  Placeholdery(Ksiega ksiega, Ulubione ulubione) { this.ksiega = ksiega; this.ulubione = ulubione; }

  @Override public String getIdentifier() { return "zaklecia"; }
  @Override public String getAuthor() { return "mcmmorpg"; }
  @Override public String getVersion() { return "1"; }
  @Override public boolean persist() { return true; }

  @Override
  public String onRequest(OfflinePlayer op, String param) {
    if (!(op instanceof Player p)) return "";
    if (param.startsWith("ulubione_")) return ulubione.jest(p, param.substring(9)) ? "★" : "☆";
    if (param.equals("prawa") || param.equals("lewa")) {
      Ksiega.Zaklecie z = ksiega.w(param.equals("prawa") ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand());
      return z == null ? "pusta" : z.nazwa();
    }
    return null;
  }
}
