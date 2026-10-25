package pl.mcmmorpg.napady;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Ciala z MythicMobs (prawdziwe AI: sciezki, cios, luk) i skora gracza z LibsDisguises.
 * Klasa ladowana tylko gdy oba pluginy sa wlaczone (softdepend), inaczej NoClassDefFoundError.
 */
final class CialaMM {
  private CialaMM() {}

  static boolean dostepne() {
    var pm = Bukkit.getPluginManager();
    return pm.isPluginEnabled("MythicMobs");
  }

  static boolean przebrania() { return Bukkit.getPluginManager().isPluginEnabled("LibsDisguises"); }

  /** Typ moba z plugins/MythicMobs/mobs/Napady.yml; null gdy MM go nie zna. */
  static LivingEntity spawn(String typ, Location l) {
    var mm = MythicBukkit.inst().getMobManager().getMythicMob(typ).orElse(null);
    if (mm == null) return null;
    var am = mm.spawn(BukkitAdapter.adapt(l), 1);
    Entity e = am == null ? null : am.getEntity().getBukkitEntity();
    return e instanceof LivingEntity le ? le : null;
  }

  /**
   * Skora gracza na ciele moba (LD liczy animacje marszu/ciosu po stronie klienta jak u gracza).
   * Nazwa nad glowa ukryta, poza lista graczy. Zwraca false, gdy LD odmowil.
   */
  static boolean przebierz(LivingEntity e, String nazwa, ProfileProperty skora, Logger log) {
    try {
      var prof = new UserProfile(UUID.randomUUID(), nazwa,
          List.of(new TextureProperty("textures", skora.getValue(), skora.getSignature())));
      PlayerDisguise d = new PlayerDisguise(prof);
      d.setNameVisible(false);
      d.setDisplayedInTab(false);
      DisguiseAPI.disguiseToAll(e, d);
      return DisguiseAPI.isDisguised(e);
    } catch (Throwable t) {
      log.warning("LibsDisguises: przebranie nie wyszlo: " + t);
      return false;
    }
  }
}
