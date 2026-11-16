package pl.mcmmorpg.characterstage;

import java.util.Optional;

public final class UnavailableDomainCharacterStores {
  private UnavailableDomainCharacterStores() {}

  public static ProfileMappingStore unavailableProfileMappings(String detail) {
    return profileId -> ProfileMappingStore.LookupResult.unavailable(detail);
  }

  public static CharacterRecordStore unavailableCharacterRecords() {
    return characterId -> Optional.empty();
  }
}
