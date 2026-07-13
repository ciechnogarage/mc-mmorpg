package pl.mcmmorpg.characterstage;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class MmoProfilesReflectionAdapter implements ProfileBackendAdapter {
  @Override
  public Optional<ProfileSlotData> getSlot(Player player, int profileIndex) {
    try {
      Plugin plugin = Bukkit.getPluginManager().getPlugin("MMOProfiles");
      if (plugin == null) {
        return Optional.empty();
      }

      Method getPlayerData = plugin.getClass().getMethod("getPlayerData", UUID.class);
      Object profileList = getPlayerData.invoke(plugin, player.getUniqueId());
      if (profileList == null) {
        return Optional.empty();
      }

      Method getProfiles = profileList.getClass().getMethod("getProfiles");
      Object rawProfiles = getProfiles.invoke(profileList);
      if (!(rawProfiles instanceof List<?> profiles)) {
        return Optional.empty();
      }

      int index = Math.max(0, profileIndex - 1);
      if (index >= profiles.size()) {
        return Optional.of(new ProfileSlotData(profileIndex, "Nowy bohater", false));
      }

      Object profile = profiles.get(index);
      if (profile == null) {
        return Optional.of(new ProfileSlotData(profileIndex, "Nowy bohater", false));
      }

      Method getName = profile.getClass().getMethod("getName");
      String profileName = String.valueOf(getName.invoke(profile));
      return Optional.of(new ProfileSlotData(profileIndex, profileName, true));
    } catch (ReflectiveOperationException ex) {
      return Optional.empty();
    }
  }
}
