package pl.mcmmorpg.mapa;

import fr.phoenixdevt.profiles.ProfileList;
import fr.phoenixdevt.profiles.ProfileProvider;
import java.util.UUID;
import org.bukkit.Bukkit;

final class ProfilMost {
  private ProfilMost() {}

  static UUID key(UUID player) {
    ProfileProvider provider = Bukkit.getServicesManager().load(ProfileProvider.class);
    if (provider == null) return player;
    ProfileList list = provider.getPlayerData(player);
    if (list == null || list.getCurrent() == null) return player;
    UUID key = list.getCurrent().getUniqueId();
    return key == null ? player : key;
  }
}
