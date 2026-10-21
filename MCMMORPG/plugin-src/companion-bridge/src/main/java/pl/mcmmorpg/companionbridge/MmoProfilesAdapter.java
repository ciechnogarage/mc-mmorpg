package pl.mcmmorpg.companionbridge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

/**
 * Reflection wzgledem fr.phoenixdevt.mmoprofiles.bukkit.MMOProfiles —
 * sygnatury zweryfikowane refleksja wzgledem zainstalowanego
 * MMOProfiles-1.2-...jar + MythicLib (wymagane na classpath), nie zgadywane.
 * getPlayerData(UUID) zwraca ProfileList.getCurrent() -> PlayerProfile
 * (getUniqueId/getName) — a nie zgadywanie indeksu w liscie, jak we
 * wczesniejszej, niezweryfikowanej wersji tego adaptera w character-stage.
 */
public final class MmoProfilesAdapter {
  public Map<String, Object> describe(OfflinePlayer player, UUID uuid) {
    try {
      Plugin plugin = Bukkit.getPluginManager().getPlugin("MMOProfiles");
      if (plugin == null) {
        return Map.of("available", false);
      }

      Object profileList;
      try {
        profileList = plugin.getClass().getMethod("getPlayerData", UUID.class).invoke(plugin, uuid);
      } catch (java.lang.reflect.InvocationTargetException dataNotLoaded) {
        // Ta sama przyczyna co w MmoCoreAdapter: MythicLib SynchronizedDataManager
        // rzuca NPE dla offline gracza zamiast zwracac null — zweryfikowane na
        // zywo (srv-items log).
        if (dataNotLoaded.getCause() instanceof NullPointerException) {
          return Map.of("available", true, "profileLoaded", false);
        }
        throw dataNotLoaded;
      }
      if (profileList == null) {
        return Map.of("available", true, "profileLoaded", false);
      }

      Object current = profileList.getClass().getMethod("getCurrent").invoke(profileList);
      if (current == null) {
        return Map.of("available", true, "profileLoaded", false);
      }

      String profileId = String.valueOf(current.getClass().getMethod("getUniqueId").invoke(current));
      String profileName = (String) current.getClass().getMethod("getName").invoke(current);

      Map<String, Object> out = new LinkedHashMap<>();
      out.put("available", true);
      out.put("profileLoaded", true);
      out.put("profileId", profileId);
      out.put("profileName", profileName);
      return out;
    } catch (ReflectiveOperationException | RuntimeException ex) {
      Bukkit.getLogger().log(Level.WARNING,
          "CompanionBridge: MmoProfilesAdapter reflection failed for " + player.getName()
              + "; zwracam available=false. Sprawdz kompatybilnosc API MMOProfiles.", ReflectionSupport.unwrap(ex));
      return Map.of("available", false);
    }
  }
}
