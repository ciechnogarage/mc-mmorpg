package pl.mcmmorpg.companionbridge;

import org.bukkit.Bukkit;

/**
 * W tej transzy sprawdzamy wylacznie OBECNOSC pluginu (softdepend), bez
 * wywolywania jego API — adaptery Vault/MMOCore/MMOProfiles (realne
 * odczyty przez reflection) to kolejna transza, patrz plan.
 */
public final class CapabilitiesService {
  public boolean vaultPresent() {
    return Bukkit.getPluginManager().getPlugin("Vault") != null;
  }

  public boolean mmocorePresent() {
    return Bukkit.getPluginManager().getPlugin("MMOCore") != null;
  }

  public boolean mmoprofilesPresent() {
    return Bukkit.getPluginManager().getPlugin("MMOProfiles") != null;
  }

  public boolean sparkPresent() {
    return Bukkit.getPluginManager().getPlugin("spark") != null;
  }
}
