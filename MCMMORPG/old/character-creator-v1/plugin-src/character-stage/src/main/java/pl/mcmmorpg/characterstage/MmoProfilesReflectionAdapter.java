package pl.mcmmorpg.characterstage;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class MmoProfilesReflectionAdapter implements ProfileBackendAdapter {
  private static final String EMPTY_PROFILE_NAME = "Nowy bohater";
  private static final String BLOCKED_PROFILE_NAME = "Slot niedostepny";

  @Override
  public Optional<ProfileSlotData> getSlot(Player player, int profileIndex) {
    try {
      Plugin plugin = Bukkit.getPluginManager().getPlugin("MMOProfiles");
      if (plugin == null) {
        return Optional.of(blockedSlot(profileIndex));
      }

      Method getPlayerData = plugin.getClass().getMethod("getPlayerData", UUID.class);
      Object profileList = getPlayerData.invoke(plugin, player.getUniqueId());
      if (profileList == null) {
        return Optional.of(blockedSlot(profileIndex));
      }

      Method getProfiles = profileList.getClass().getMethod("getProfiles");
      Object rawProfiles = getProfiles.invoke(profileList);
      if (!(rawProfiles instanceof List<?> profiles)) {
        return Optional.of(blockedSlot(profileIndex));
      }

      int index = Math.max(0, profileIndex - 1);
      if (index >= profiles.size()) {
        return Optional.of(ProfileSlotData.empty(profileIndex, EMPTY_PROFILE_NAME));
      }

      Object profile = profiles.get(index);
      if (profile == null) {
        return Optional.of(ProfileSlotData.empty(profileIndex, EMPTY_PROFILE_NAME));
      }

      String profileName = invokeString(profile, "getName").orElse(EMPTY_PROFILE_NAME);
      String profileId = invokeUuid(profile, "getUniqueId").map(UUID::toString).orElse(null);
      return Optional.of(new ProfileSlotData(profileIndex, profileId, profileName, true));
    } catch (ReflectiveOperationException ex) {
      Bukkit.getLogger().log(
          Level.WARNING,
          "CharacterStage: MMOProfiles reflection failed for player "
              + player.getName()
              + " slot "
              + profileIndex
              + ". Failing closed.",
          ex);
      return Optional.of(blockedSlot(profileIndex));
    }
  }

  private ProfileSlotData blockedSlot(int profileIndex) {
    return ProfileSlotData.blocked(profileIndex, BLOCKED_PROFILE_NAME);
  }

  private Optional<String> invokeString(Object target, String methodName) {
    try {
      Method method = target.getClass().getMethod(methodName);
      Object raw = method.invoke(target);
      return raw == null ? Optional.empty() : Optional.of(String.valueOf(raw));
    } catch (ReflectiveOperationException ex) {
      return Optional.empty();
    }
  }

  private Optional<UUID> invokeUuid(Object target, String methodName) {
    try {
      Method method = target.getClass().getMethod(methodName);
      Object raw = method.invoke(target);
      return raw instanceof UUID uuid ? Optional.of(uuid) : Optional.empty();
    } catch (ReflectiveOperationException ex) {
      return Optional.empty();
    }
  }
}
