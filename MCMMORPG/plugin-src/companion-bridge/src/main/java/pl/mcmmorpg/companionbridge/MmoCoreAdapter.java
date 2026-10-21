package pl.mcmmorpg.companionbridge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Reflection wzgledem net.Indyuce.mmocore.api.player.PlayerData — sygnatury
 * zweryfikowane refleksja wzgledem zainstalowanego
 * MMOCore-1.13.1-...jar + MythicLib-dist-1.7.1-...jar (wymagane na
 * classpath do zaladowania PlayerData.class), nie zgadywane.
 */
public final class MmoCoreAdapter {
  public Map<String, Object> describe(OfflinePlayer player, UUID uuid) {
    try {
      Class<?> playerDataClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
      Object data;
      try {
        data = playerDataClass.getMethod("get", UUID.class).invoke(null, uuid);
      } catch (java.lang.reflect.InvocationTargetException dataNotLoaded) {
        // MythicLib SynchronizedDataManager.get() rzuca NPE("Player data is not
        // loaded") dla offline gracza zamiast zwracac null — zweryfikowane na
        // zywo (srv-items log), nie zgadywane. To jest "loaded=false", nie
        // "plugin niedostepny".
        if (dataNotLoaded.getCause() instanceof NullPointerException) {
          return Map.of("available", true, "loaded", false);
        }
        throw dataNotLoaded;
      }
      if (data == null) {
        return Map.of("available", true, "loaded", false);
      }

      int level = (int) playerDataClass.getMethod("getLevel").invoke(data);
      Object playerClass = playerDataClass.getMethod("getProfess").invoke(data);
      String classId = null;
      if (playerClass != null) {
        classId = (String) playerClass.getClass().getMethod("getId").invoke(playerClass);
      }

      Map<String, Object> out = new LinkedHashMap<>();
      out.put("available", true);
      out.put("loaded", true);
      out.put("classId", classId);
      out.put("level", level);
      return out;
    } catch (ClassNotFoundException notInstalled) {
      return Map.of("available", false);
    } catch (ReflectiveOperationException | RuntimeException ex) {
      Bukkit.getLogger().log(Level.WARNING,
          "CompanionBridge: MmoCoreAdapter reflection failed for " + player.getName()
              + "; zwracam available=false. Sprawdz kompatybilnosc API MMOCore.", ReflectionSupport.unwrap(ex));
      return Map.of("available", false);
    }
  }
}
