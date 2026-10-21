import java.util.List;
import java.util.Optional;
import pl.mcmmorpg.characterstage.CharacterIdentityResolver;
import pl.mcmmorpg.characterstage.CharacterRecordStore;
import pl.mcmmorpg.characterstage.CharacterStageSlotResolver;
import pl.mcmmorpg.characterstage.DomainCharacterIdentityResolver;
import pl.mcmmorpg.characterstage.ProfileBackendAdapter;
import pl.mcmmorpg.characterstage.ProfileMappingStore;
import pl.mcmmorpg.characterstage.StageConfig;

public final class RunCharacterStageResolverChecks {
  private RunCharacterStageResolverChecks() {}

  public static void main(String[] args) {
    verifyOneToOneResolution();
    verifyUnresolvedResolution();
    verifyAmbiguousResolution();
    verifyUnavailableAdapterResults();
    verifyInconsistentMappings();
    verifyMalformedResolverResults();
    verifySlotStateTransitions();
    System.out.println("resolver checks: OK");
  }

  private static void verifyOneToOneResolution() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> new ProfileMappingStore.LookupResult(
            ProfileMappingStore.LookupStatus.UNIQUE,
            new ProfileMappingStore.ProfileMappingRecord(
                profileId, "char-1", "account-1", "BOUND", "PLAYABLE", 2),
            null),
        characterId -> Optional.of(
            new CharacterRecordStore.CharacterRecord(characterId, "PLAYABLE", "READY", 2)));

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-1");
    require(
        resolution.status() == CharacterIdentityResolver.ResolutionStatus.RESOLVED,
        "expected resolved status");
    require("char-1".equals(resolution.character().characterId()), "expected character_id char-1");
  }

  private static void verifyUnresolvedResolution() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> new ProfileMappingStore.LookupResult(
            ProfileMappingStore.LookupStatus.NOT_FOUND,
            null,
            "missing mapping"),
        characterId -> Optional.empty());

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-2");
    require(
        resolution.status() == CharacterIdentityResolver.ResolutionStatus.UNRESOLVED,
        "expected unresolved status");
  }

  private static void verifyAmbiguousResolution() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> new ProfileMappingStore.LookupResult(
            ProfileMappingStore.LookupStatus.AMBIGUOUS,
            null,
            "duplicate mapping"),
        characterId -> Optional.empty());

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-3");
    require(
        resolution.status() == CharacterIdentityResolver.ResolutionStatus.AMBIGUOUS,
        "expected ambiguous status");
  }

  private static void verifyUnavailableAdapterResults() {
    DomainCharacterIdentityResolver nullMappingResolver = new DomainCharacterIdentityResolver(
        profileId -> null,
        characterId -> Optional.empty());
    requireUnavailable(nullMappingResolver.resolve("profile-null-mapping"));

    DomainCharacterIdentityResolver throwingMappingResolver = new DomainCharacterIdentityResolver(
        profileId -> {
          throw new IllegalStateException("mapping backend offline");
        },
        characterId -> Optional.empty());
    requireUnavailable(throwingMappingResolver.resolve("profile-throwing-mapping"));

    DomainCharacterIdentityResolver nullCharacterResolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.unique(
            new ProfileMappingStore.ProfileMappingRecord(
                profileId, "char-1", "account-1", "BOUND", "PLAYABLE", 2)),
        characterId -> null);
    requireUnavailable(nullCharacterResolver.resolve("profile-null-character"));

    DomainCharacterIdentityResolver throwingCharacterResolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.unique(
            new ProfileMappingStore.ProfileMappingRecord(
                profileId, "char-1", "account-1", "BOUND", "PLAYABLE", 2)),
        characterId -> {
          throw new IllegalStateException("character backend offline");
        });
    requireUnavailable(throwingCharacterResolver.resolve("profile-throwing-character"));
  }

  private static void requireUnavailable(CharacterIdentityResolver.Resolution resolution) {
    require(
        resolution.status() == CharacterIdentityResolver.ResolutionStatus.UNAVAILABLE,
        "expected unavailable status");
    require(resolution.character() == null, "unavailable resolution must not expose a character");
  }

  private static void verifyInconsistentMappings() {
    DomainCharacterIdentityResolver blankCharacterIdResolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.unique(
            new ProfileMappingStore.ProfileMappingRecord(
                profileId, "  ", "account-1", "BOUND", "PLAYABLE", 2)),
        characterId -> Optional.of(
            new CharacterRecordStore.CharacterRecord(characterId, "PLAYABLE", "READY", 2)));
    requireInconsistent(blankCharacterIdResolver.resolve("profile-blank-character"));

    DomainCharacterIdentityResolver mismatchedCharacterResolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.unique(
            new ProfileMappingStore.ProfileMappingRecord(
                profileId, "char-1", "account-1", "BOUND", "PLAYABLE", 2)),
        characterId -> Optional.of(
            new CharacterRecordStore.CharacterRecord("char-2", "PLAYABLE", "READY", 2)));
    requireInconsistent(mismatchedCharacterResolver.resolve("profile-mismatched-character"));
  }

  private static void requireInconsistent(CharacterIdentityResolver.Resolution resolution) {
    require(
        resolution.status() == CharacterIdentityResolver.ResolutionStatus.INCONSISTENT,
        "expected inconsistent status");
    require(resolution.character() == null, "inconsistent resolution must not expose a character");
  }

  private static void verifyMalformedResolverResults() {
    StageConfig.StageSlot slot = new StageConfig.StageSlot(
        "slot_1",
        "Bohater I",
        1,
        false,
        new StageConfig.BlockAnchor("world", 0, 64, 0),
        new StageConfig.ScenePoint("world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
        List.of(),
        List.of());

    CharacterStageSlotResolver nullResolver = new CharacterStageSlotResolver(profileId -> null);
    CharacterStageSlotResolver.SlotResolution nullResolution = nullResolver.resolve(
        slot,
        new ProfileBackendAdapter.ProfileSlotData(1, "profile-null-resolution", "Hero", true));
    require(
        nullResolution.state() == CharacterStageSlotResolver.SlotState.BLOCKED,
        "expected null resolver result to block slot");

    CharacterStageSlotResolver.SlotResolution nullProfileSlot = nullResolver.resolve(slot, null);
    require(
        nullProfileSlot.state() == CharacterStageSlotResolver.SlotState.BLOCKED,
        "expected null profile slot data to block slot");

    CharacterStageSlotResolver throwingResolver = new CharacterStageSlotResolver(profileId -> {
      throw new IllegalStateException("resolver backend offline");
    });
    CharacterStageSlotResolver.SlotResolution throwingResolution = throwingResolver.resolve(
        slot,
        new ProfileBackendAdapter.ProfileSlotData(1, "profile-throwing-resolution", "Hero", true));
    require(
        throwingResolution.state() == CharacterStageSlotResolver.SlotState.BLOCKED,
        "expected throwing resolver to block slot");
  }

  private static void verifySlotStateTransitions() {
    CharacterStageSlotResolver slotResolver = new CharacterStageSlotResolver(
        profileId -> switch (profileId) {
          case "ready-profile" -> new CharacterIdentityResolver.Resolution(
              CharacterIdentityResolver.ResolutionStatus.RESOLVED,
              new CharacterIdentityResolver.ResolvedCharacter("char-ready", "PLAYABLE", "READY", 2),
              null);
          case "incomplete-profile" -> new CharacterIdentityResolver.Resolution(
              CharacterIdentityResolver.ResolutionStatus.RESOLVED,
              new CharacterIdentityResolver.ResolvedCharacter(
                  "char-creating",
                  "CREATING",
                  "CLASS_SELECTED",
                  2),
              null);
          default -> new CharacterIdentityResolver.Resolution(
              CharacterIdentityResolver.ResolutionStatus.AMBIGUOUS,
              null,
              "duplicate mapping");
        });

    StageConfig.StageSlot slot = new StageConfig.StageSlot(
        "slot_1",
        "Bohater I",
        1,
        false,
        new StageConfig.BlockAnchor("world", 0, 64, 0),
        new StageConfig.ScenePoint("world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
        List.of(),
        List.of());

    require(
        slotResolver.resolve(slot, new ProfileBackendAdapter.ProfileSlotData(1, null, "Nowy bohater", false)).state()
            == CharacterStageSlotResolver.SlotState.EMPTY,
        "expected empty slot");
    require(
        slotResolver.resolve(slot, new ProfileBackendAdapter.ProfileSlotData(1, "ready-profile", "Ready", true)).state()
            == CharacterStageSlotResolver.SlotState.READY,
        "expected ready slot");
    require(
        slotResolver.resolve(
                slot,
                new ProfileBackendAdapter.ProfileSlotData(1, "incomplete-profile", "Incomplete", true))
            .state() == CharacterStageSlotResolver.SlotState.INCOMPLETE,
        "expected incomplete slot");
    require(
        slotResolver.resolve(
                slot,
                new ProfileBackendAdapter.ProfileSlotData(1, "ambiguous-profile", "Blocked", true))
            .state() == CharacterStageSlotResolver.SlotState.BLOCKED,
        "expected blocked slot");
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
  }
}
