package pl.mcmmorpg.characterstage;

import java.util.Objects;
import java.util.Optional;

public final class DomainCharacterIdentityResolver implements CharacterIdentityResolver {
  private static final String REQUIRED_BOUND_STATUS = "BOUND";

  private final ProfileMappingStore profileMappingStore;
  private final CharacterRecordStore characterRecordStore;

  public DomainCharacterIdentityResolver(
      ProfileMappingStore profileMappingStore,
      CharacterRecordStore characterRecordStore
  ) {
    this.profileMappingStore = Objects.requireNonNull(profileMappingStore);
    this.characterRecordStore = Objects.requireNonNull(characterRecordStore);
  }

  @Override
  public Resolution resolve(String profileId) {
    if (profileId == null || profileId.isBlank()) {
      return Resolution.unresolved("missing technical MMOProfiles.profileId");
    }

    ProfileMappingStore.LookupResult lookupResult;
    try {
      lookupResult = profileMappingStore.lookupByProfileId(profileId);
    } catch (RuntimeException exception) {
      return Resolution.unavailable("mapping backend unavailable");
    }
    if (lookupResult == null || lookupResult.status() == null) {
      return Resolution.unavailable("mapping backend unavailable");
    }

    return switch (lookupResult.status()) {
      case UNIQUE -> resolveUnique(profileId, lookupResult.record());
      case NOT_FOUND -> Resolution.unresolved(
          lookupResult.detail() == null ? "no durable mapping for profile_id" : lookupResult.detail());
      case AMBIGUOUS -> Resolution.ambiguous(
          lookupResult.detail() == null ? "multiple durable mappings for profile_id" : lookupResult.detail());
      case UNAVAILABLE -> Resolution.unavailable(
          lookupResult.detail() == null ? "mapping backend unavailable" : lookupResult.detail());
    };
  }

  private Resolution resolveUnique(
      String profileId,
      ProfileMappingStore.ProfileMappingRecord mappingRecord
  ) {
    if (mappingRecord == null) {
      return Resolution.inconsistent("unique lookup returned null mapping record");
    }
    if (!profileId.equals(mappingRecord.profileId())) {
      return Resolution.inconsistent("mapping store returned mismatched profile_id");
    }
    if (mappingRecord.characterId() == null || mappingRecord.characterId().isBlank()) {
      return Resolution.inconsistent("mapping store returned blank character_id");
    }
    if (!REQUIRED_BOUND_STATUS.equals(mappingRecord.bindingStatus())) {
      return Resolution.inconsistent(
          "mapping binding_status must be BOUND, got " + mappingRecord.bindingStatus());
    }

    Optional<CharacterRecordStore.CharacterRecord> record;
    try {
      record = characterRecordStore.findByCharacterId(mappingRecord.characterId());
    } catch (RuntimeException exception) {
      return Resolution.unavailable("character backend unavailable");
    }
    if (record == null) {
      return Resolution.unavailable("character backend unavailable");
    }
    if (record.isEmpty()) {
      return Resolution.inconsistent("mapped character_id does not exist in character store");
    }

    CharacterRecordStore.CharacterRecord character = record.get();
    if (!mappingRecord.characterId().equals(character.characterId())) {
      return Resolution.inconsistent("character store returned mismatched character_id");
    }
    return Resolution.resolved(new ResolvedCharacter(
        character.characterId(),
        character.lifecycleStatus(),
        character.creatorStage(),
        character.schemaVersion()));
  }
}
