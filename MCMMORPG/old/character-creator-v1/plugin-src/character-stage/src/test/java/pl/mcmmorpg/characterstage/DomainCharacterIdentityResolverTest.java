package pl.mcmmorpg.characterstage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class DomainCharacterIdentityResolverTest {
  @Test
  void resolvesUniqueBoundProfileToCharacterState() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.unique(
            new ProfileMappingStore.ProfileMappingRecord(
                profileId, "char-1", "account-1", "BOUND", "PLAYABLE", 2)),
        characterId -> Optional.of(
            new CharacterRecordStore.CharacterRecord(characterId, "PLAYABLE", "READY", 2)));

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-1");

    assertEquals(CharacterIdentityResolver.ResolutionStatus.RESOLVED, resolution.status());
    assertEquals("char-1", resolution.character().characterId());
    assertEquals("PLAYABLE", resolution.character().lifecycleStatus());
    assertEquals("READY", resolution.character().creatorStage());
  }

  @Test
  void returnsUnresolvedWhenMappingMissing() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.notFound("missing mapping"),
        characterId -> Optional.empty());

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-1");

    assertEquals(CharacterIdentityResolver.ResolutionStatus.UNRESOLVED, resolution.status());
    assertNull(resolution.character());
  }

  @Test
  void returnsAmbiguousWhenMultipleMappingsClaimSameProfile() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.ambiguous("duplicate mapping"),
        characterId -> Optional.empty());

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-1");

    assertEquals(CharacterIdentityResolver.ResolutionStatus.AMBIGUOUS, resolution.status());
    assertNull(resolution.character());
  }

  @Test
  void returnsInconsistentWhenBindingStatusIsNotBound() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.unique(
            new ProfileMappingStore.ProfileMappingRecord(
                profileId, "char-1", "account-1", "RESERVED", "CREATING", 2)),
        characterId -> Optional.of(
            new CharacterRecordStore.CharacterRecord(characterId, "CREATING", "CLASS_SELECTED", 2)));

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-1");

    assertEquals(CharacterIdentityResolver.ResolutionStatus.INCONSISTENT, resolution.status());
    assertNull(resolution.character());
  }

  @Test
  void returnsUnavailableWhenMappingLookupReturnsNull() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> null,
        characterId -> Optional.empty());

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-1");

    assertEquals(CharacterIdentityResolver.ResolutionStatus.UNAVAILABLE, resolution.status());
    assertNull(resolution.character());
  }

  @Test
  void returnsUnavailableWhenMappingLookupThrows() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> {
          throw new IllegalStateException("mapping backend offline");
        },
        characterId -> Optional.empty());

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-1");

    assertEquals(CharacterIdentityResolver.ResolutionStatus.UNAVAILABLE, resolution.status());
    assertNull(resolution.character());
  }

  @Test
  void returnsUnavailableWhenCharacterLookupReturnsNull() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.unique(
            new ProfileMappingStore.ProfileMappingRecord(
                profileId, "char-1", "account-1", "BOUND", "PLAYABLE", 2)),
        characterId -> null);

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-1");

    assertEquals(CharacterIdentityResolver.ResolutionStatus.UNAVAILABLE, resolution.status());
    assertNull(resolution.character());
  }

  @Test
  void returnsUnavailableWhenCharacterLookupThrows() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.unique(
            new ProfileMappingStore.ProfileMappingRecord(
                profileId, "char-1", "account-1", "BOUND", "PLAYABLE", 2)),
        characterId -> {
          throw new IllegalStateException("character backend offline");
        });

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-1");

    assertEquals(CharacterIdentityResolver.ResolutionStatus.UNAVAILABLE, resolution.status());
    assertNull(resolution.character());
  }

  @Test
  void returnsInconsistentWhenMappedCharacterIdIsBlank() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.unique(
            new ProfileMappingStore.ProfileMappingRecord(
                profileId, "  ", "account-1", "BOUND", "PLAYABLE", 2)),
        characterId -> Optional.of(
            new CharacterRecordStore.CharacterRecord(characterId, "PLAYABLE", "READY", 2)));

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-1");

    assertEquals(CharacterIdentityResolver.ResolutionStatus.INCONSISTENT, resolution.status());
    assertNull(resolution.character());
  }

  @Test
  void returnsInconsistentWhenCharacterStoreReturnsDifferentCharacterId() {
    DomainCharacterIdentityResolver resolver = new DomainCharacterIdentityResolver(
        profileId -> ProfileMappingStore.LookupResult.unique(
            new ProfileMappingStore.ProfileMappingRecord(
                profileId, "char-1", "account-1", "BOUND", "PLAYABLE", 2)),
        characterId -> Optional.of(
            new CharacterRecordStore.CharacterRecord("char-2", "PLAYABLE", "READY", 2)));

    CharacterIdentityResolver.Resolution resolution = resolver.resolve("profile-1");

    assertEquals(CharacterIdentityResolver.ResolutionStatus.INCONSISTENT, resolution.status());
    assertNull(resolution.character());
  }
}
