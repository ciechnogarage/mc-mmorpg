package pl.mcmmorpg.characterstage;

import java.util.Objects;

public final class CharacterStageSlotResolver {
  private static final String RESOLVER_UNAVAILABLE_DETAIL =
      "character identity resolver unavailable";
  private static final String RESOLVER_MALFORMED_DETAIL =
      "character identity resolver returned malformed result";
  private static final String PROFILE_SLOT_UNAVAILABLE_DETAIL =
      "MMOProfiles slot data unavailable";

  private final CharacterIdentityResolver characterIdentityResolver;

  public CharacterStageSlotResolver(CharacterIdentityResolver characterIdentityResolver) {
    this.characterIdentityResolver = Objects.requireNonNull(characterIdentityResolver);
  }

  public SlotResolution resolve(
      StageConfig.StageSlot slot,
      ProfileBackendAdapter.ProfileSlotData profileSlotData
  ) {
    if (profileSlotData == null) {
      return SlotResolution.blocked(PROFILE_SLOT_UNAVAILABLE_DETAIL);
    }
    if (!profileSlotData.occupied()) {
      return SlotResolution.empty();
    }

    if (profileSlotData.profileId() == null || profileSlotData.profileId().isBlank()) {
      return SlotResolution.blocked("occupied MMOProfiles slot missing technical profile_id");
    }

    CharacterIdentityResolver.Resolution resolution;
    try {
      resolution = characterIdentityResolver.resolve(profileSlotData.profileId());
    } catch (RuntimeException exception) {
      return SlotResolution.blocked(RESOLVER_UNAVAILABLE_DETAIL);
    }
    if (resolution == null || resolution.status() == null) {
      return SlotResolution.blocked(RESOLVER_MALFORMED_DETAIL);
    }

    return switch (resolution.status()) {
      case RESOLVED -> resolveResolvedCharacter(resolution.character());
      case UNRESOLVED, AMBIGUOUS, INCONSISTENT, UNAVAILABLE ->
          SlotResolution.blocked(
              resolution.detail() == null ? RESOLVER_UNAVAILABLE_DETAIL : resolution.detail());
    };
  }

  private SlotResolution resolveResolvedCharacter(CharacterIdentityResolver.ResolvedCharacter character) {
    if (character == null
        || character.characterId() == null
        || character.characterId().isBlank()
        || character.lifecycleStatus() == null
        || character.lifecycleStatus().isBlank()
        || character.creatorStage() == null
        || character.creatorStage().isBlank()
        || character.schemaVersion() <= 0) {
      return SlotResolution.blocked(RESOLVER_MALFORMED_DETAIL);
    }
    return character.readyForPlay()
        ? SlotResolution.ready(character)
        : SlotResolution.incomplete(character);
  }

  public record SlotResolution(
      SlotState state,
      CharacterIdentityResolver.ResolvedCharacter character,
      String detail
  ) {
    static SlotResolution empty() {
      return new SlotResolution(SlotState.EMPTY, null, null);
    }

    static SlotResolution ready(CharacterIdentityResolver.ResolvedCharacter character) {
      return new SlotResolution(SlotState.READY, character, null);
    }

    static SlotResolution incomplete(CharacterIdentityResolver.ResolvedCharacter character) {
      return new SlotResolution(SlotState.INCOMPLETE, character, null);
    }

    static SlotResolution blocked(String detail) {
      return new SlotResolution(SlotState.BLOCKED, null, detail);
    }
  }

  public enum SlotState {
    EMPTY,
    READY,
    INCOMPLETE,
    BLOCKED
  }
}
