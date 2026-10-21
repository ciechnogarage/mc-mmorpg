package pl.mcmmorpg.characterstage;

public interface CharacterIdentityResolver {
  Resolution resolve(String profileId);

  record Resolution(ResolutionStatus status, ResolvedCharacter character, String detail) {
    public static Resolution resolved(ResolvedCharacter character) {
      return new Resolution(ResolutionStatus.RESOLVED, character, null);
    }

    public static Resolution unresolved(String detail) {
      return new Resolution(ResolutionStatus.UNRESOLVED, null, detail);
    }

    public static Resolution ambiguous(String detail) {
      return new Resolution(ResolutionStatus.AMBIGUOUS, null, detail);
    }

    public static Resolution inconsistent(String detail) {
      return new Resolution(ResolutionStatus.INCONSISTENT, null, detail);
    }

    public static Resolution unavailable(String detail) {
      return new Resolution(ResolutionStatus.UNAVAILABLE, null, detail);
    }
  }

  enum ResolutionStatus {
    RESOLVED,
    UNRESOLVED,
    AMBIGUOUS,
    INCONSISTENT,
    UNAVAILABLE
  }

  record ResolvedCharacter(
      String characterId,
      String lifecycleStatus,
      String creatorStage,
      int schemaVersion
  ) {
    boolean readyForPlay() {
      return "PLAYABLE".equals(lifecycleStatus) && "READY".equals(creatorStage);
    }
  }
}
