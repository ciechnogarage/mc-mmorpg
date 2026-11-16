package pl.mcmmorpg.klasybron;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Kto jest sojusznikiem gracza - filtr {@link Celownik}. Wycinek z galezi klasy-p1p2 (tam dochodza warunki
 * druzyna{}/celWrogi{}); tu tylko to, czego potrzebuje celownik Rozkazu akolity.
 */
final class Warunki {

  /** Frakcje mobow MM nalezacych do gracza (chowance, slugi akolity, przywolania zaklec). */
  static final Set<String> FRAKCJE_GRACZA = Set.of("chowance", "akolita_slugi", "zaklecia_przywolane");

  private Warunki() {}

  /**
   * Sojusznik gracza: on sam, czlonek jego druzyny MMOCore albo mob frakcji gracza nalezacy do niego lub do
   * czlonka druzyny. Celownik patrzy przez sojusznikow (straznik przed graczem nie zaslania celu).
   */
  static boolean sojusznik(Player gracz, Entity e) {
    if (gracz.getUniqueId().equals(e.getUniqueId())) return true;
    boolean mmocore = Bukkit.getPluginManager().isPluginEnabled("MMOCore");
    if (e instanceof Player p) return mmocore && wJednejDruzynie(gracz, p);
    Optional<ActiveMob> mob = MythicBukkit.inst().getMobManager().getActiveMob(e.getUniqueId());
    if (mob.isEmpty()) return false;
    String frakcja = mob.get().getFaction();
    if (frakcja == null || !FRAKCJE_GRACZA.contains(frakcja.toLowerCase(Locale.ROOT))) return false;
    Player w = wlasciciel(mob.get());
    if (w == null) return false;
    return w.getUniqueId().equals(gracz.getUniqueId()) || (mmocore && wJednejDruzynie(gracz, w));
  }

  private static boolean wJednejDruzynie(Player a, Player b) {
    if (!PlayerData.has(a) || !PlayerData.has(b)) return false;
    AbstractParty druzyna = PlayerData.get(a).getParty();
    return druzyna != null && druzyna.hasMember(b);
  }

  static Player wlasciciel(ActiveMob am) {
    AbstractEntity owner = am.getOwner().orElse(null);
    if (owner == null) return null;
    UUID id = owner.getUniqueId();
    return id == null ? null : Bukkit.getPlayer(id);
  }
}
