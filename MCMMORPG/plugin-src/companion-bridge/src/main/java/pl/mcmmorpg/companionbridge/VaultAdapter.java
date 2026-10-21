package pl.mcmmorpg.companionbridge;

import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Reflection zamiast compile-time zaleznosci na Vault-API: ten workspace
 * nie ma Gradle/resolverka zaleznosci (patrz BuildCompanionBridge.java —
 * classpath to tylko MCMMORPG/libraries), wiec kazda trzecia-strona zostaje
 * softdepend + reflection, tak jak MmoProfilesReflectionAdapter w
 * character-stage. Sygnatury ponizej zweryfikowane refleksja wzgledem
 * zainstalowanego Vault-1.7.4.jar (nie zgadywane) — patrz plan companion-bridge.
 */
public final class VaultAdapter {
  public Map<String, Object> describe(OfflinePlayer player) {
    try {
      Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
      RegisteredServiceProvider<?> registration = Bukkit.getServicesManager()
          .getRegistration((Class) economyClass);
      if (registration == null) {
        return Map.of("available", false);
      }
      Object economy = registration.getProvider();
      String providerName = (String) economyClass.getMethod("getName").invoke(economy);
      double balance = (double) economyClass
          .getMethod("getBalance", org.bukkit.OfflinePlayer.class)
          .invoke(economy, player);

      Map<String, Object> out = new java.util.LinkedHashMap<>();
      out.put("available", true);
      out.put("economyProvider", providerName);
      out.put("balance", balance);
      return out;
    } catch (ClassNotFoundException notInstalled) {
      return Map.of("available", false);
    } catch (ReflectiveOperationException | RuntimeException ex) {
      Bukkit.getLogger().log(Level.WARNING,
          "CompanionBridge: VaultAdapter reflection failed for " + player.getName()
              + "; zwracam available=false. Sprawdz kompatybilnosc API Vault.", ReflectionSupport.unwrap(ex));
      return Map.of("available", false);
    }
  }
}
