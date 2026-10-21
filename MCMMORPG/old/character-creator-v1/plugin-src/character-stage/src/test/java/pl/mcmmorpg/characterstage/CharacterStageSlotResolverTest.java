package pl.mcmmorpg.characterstage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CharacterStageSlotResolverTest {
  private static final StageConfig.StageSlot SLOT = new StageConfig.StageSlot(
      "slot_1",
      "Bohater I",
      1,
      false,
      new StageConfig.BlockAnchor("world", 0, 64, 0),
      new StageConfig.ScenePoint("world", 0.0, 64.0, 0.0, 0.0f, 0.0f),
      List.of(),
      List.of());

  @Test
  void marksEmptySlotAsEmpty() {
    CharacterStageSlotResolver resolver = new CharacterStageSlotResolver(
        profileId -> CharacterIdentityResolver.Resolution.unresolved("unused"));

    CharacterStageSlotResolver.SlotResolution resolution = resolver.resolve(
        SLOT,
        ProfileBackendAdapter.ProfileSlotData.empty(1, "Nowy bohater"));

    assertEquals(CharacterStageSlotResolver.SlotState.EMPTY, resolution.state());
  }

  @Test
  void blocksWhenProfileSlotDataIsNull() {
    CharacterStageSlotResolver resolver = new CharacterStageSlotResolver(
        profileId -> CharacterIdentityResolver.Resolution.unresolved("unused"));

    CharacterStageSlotResolver.SlotResolution resolution = resolver.resolve(SLOT, null);

    assertEquals(CharacterStageSlotResolver.SlotState.BLOCKED, resolution.state());
  }

  @Test
  void marksResolvedPlayableReadyCharacterAsReady() {
    CharacterStageSlotResolver resolver = new CharacterStageSlotResolver(
        profileId -> CharacterIdentityResolver.Resolution.resolved(
            new CharacterIdentityResolver.ResolvedCharacter("char-1", "PLAYABLE", "READY", 2)));

    CharacterStageSlotResolver.SlotResolution resolution = resolver.resolve(
        SLOT,
        new ProfileBackendAdapter.ProfileSlotData(1, "profile-1", "Hero", true));

    assertEquals(CharacterStageSlotResolver.SlotState.READY, resolution.state());
  }

  @Test
  void marksResolvedNonReadyCharacterAsIncomplete() {
    CharacterStageSlotResolver resolver = new CharacterStageSlotResolver(
        profileId -> CharacterIdentityResolver.Resolution.resolved(
            new CharacterIdentityResolver.ResolvedCharacter("char-1", "CREATING", "CLASS_SELECTED", 2)));

    CharacterStageSlotResolver.SlotResolution resolution = resolver.resolve(
        SLOT,
        new ProfileBackendAdapter.ProfileSlotData(1, "profile-1", "Hero", true));

    assertEquals(CharacterStageSlotResolver.SlotState.INCOMPLETE, resolution.state());
  }

  @Test
  void blocksAmbiguousMapping() {
    CharacterStageSlotResolver resolver = new CharacterStageSlotResolver(
        profileId -> CharacterIdentityResolver.Resolution.ambiguous("duplicate profile claim"));

    CharacterStageSlotResolver.SlotResolution resolution = resolver.resolve(
        SLOT,
        new ProfileBackendAdapter.ProfileSlotData(1, "profile-1", "Hero", true));

    assertEquals(CharacterStageSlotResolver.SlotState.BLOCKED, resolution.state());
  }

  @Test
  void blocksWhenIdentityResolverReturnsNull() {
    CharacterStageSlotResolver resolver = new CharacterStageSlotResolver(profileId -> null);

    CharacterStageSlotResolver.SlotResolution resolution = resolver.resolve(
        SLOT,
        new ProfileBackendAdapter.ProfileSlotData(1, "profile-1", "Hero", true));

    assertEquals(CharacterStageSlotResolver.SlotState.BLOCKED, resolution.state());
  }

  @Test
  void blocksWhenIdentityResolverThrows() {
    CharacterStageSlotResolver resolver = new CharacterStageSlotResolver(profileId -> {
      throw new IllegalStateException("mapping backend offline");
    });

    CharacterStageSlotResolver.SlotResolution resolution = resolver.resolve(
        SLOT,
        new ProfileBackendAdapter.ProfileSlotData(1, "profile-1", "Hero", true));

    assertEquals(CharacterStageSlotResolver.SlotState.BLOCKED, resolution.state());
  }

  @Test
  void blocksWhenResolvedIdentityHasNoCharacter() {
    CharacterStageSlotResolver resolver = new CharacterStageSlotResolver(
        profileId -> CharacterIdentityResolver.Resolution.resolved(null));

    CharacterStageSlotResolver.SlotResolution resolution = resolver.resolve(
        SLOT,
        new ProfileBackendAdapter.ProfileSlotData(1, "profile-1", "Hero", true));

    assertEquals(CharacterStageSlotResolver.SlotState.BLOCKED, resolution.state());
  }

  @Test
  void blocksWhenResolverStatusIsNull() {
    CharacterStageSlotResolver resolver = new CharacterStageSlotResolver(
        profileId -> new CharacterIdentityResolver.Resolution(null, null, "malformed"));

    CharacterStageSlotResolver.SlotResolution resolution = resolver.resolve(
        SLOT,
        new ProfileBackendAdapter.ProfileSlotData(1, "profile-1", "Hero", true));

    assertEquals(CharacterStageSlotResolver.SlotState.BLOCKED, resolution.state());
  }

  @Test
  void blocksWhenResolvedIdentityIdIsBlank() {
    CharacterStageSlotResolver resolver = new CharacterStageSlotResolver(
        profileId -> CharacterIdentityResolver.Resolution.resolved(
            new CharacterIdentityResolver.ResolvedCharacter("  ", "PLAYABLE", "READY", 2)));

    CharacterStageSlotResolver.SlotResolution resolution = resolver.resolve(
        SLOT,
        new ProfileBackendAdapter.ProfileSlotData(1, "profile-1", "Hero", true));

    assertEquals(CharacterStageSlotResolver.SlotState.BLOCKED, resolution.state());
  }
}
