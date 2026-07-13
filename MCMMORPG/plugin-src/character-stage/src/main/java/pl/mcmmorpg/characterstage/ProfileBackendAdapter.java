package pl.mcmmorpg.characterstage;

import java.util.Optional;
import org.bukkit.entity.Player;

public interface ProfileBackendAdapter {
  Optional<ProfileSlotData> getSlot(Player player, int profileIndex);

  record ProfileSlotData(int profileIndex, String profileName, boolean occupied) {}
}
