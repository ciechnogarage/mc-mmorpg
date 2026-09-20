package pl.mcmmorpg.charactercreator.creator;

/**
 * Small seam so CreatorSession/CreatorRoomListener don't need a direct MMOProfiles import.
 * Wraps ProfileCreateEvent#validate(ProfileDataModule) — see CharacterProfileModule.
 */
public interface PendingCreation {
  void complete();
}
