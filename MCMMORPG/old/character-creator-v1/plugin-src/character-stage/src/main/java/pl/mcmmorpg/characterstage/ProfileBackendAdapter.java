package pl.mcmmorpg.characterstage;

import java.util.Optional;
import org.bukkit.entity.Player;

public interface ProfileBackendAdapter {
  Optional<ProfileSlotData> getSlot(Player player, int profileIndex);

  record ProfileSlotData(
      int profileIndex,
      String profileId,
      String profileName,
      boolean occupied
  ) {
    public static ProfileSlotData empty(int profileIndex, String profileName) {
      return new ProfileSlotData(profileIndex, null, profileName, false);
    }

    public static ProfileSlotData blocked(int profileIndex, String profileName) {
      return new ProfileSlotData(profileIndex, "", profileName, true);
    }
  }
}
