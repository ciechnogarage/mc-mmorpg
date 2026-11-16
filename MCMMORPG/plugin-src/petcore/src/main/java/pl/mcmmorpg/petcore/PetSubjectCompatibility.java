package pl.mcmmorpg.petcore;

import java.util.Objects;

interface PetSubjectCompatibility {
  CompatibilityResolution resolve(PetSubjectId legacySubjectId);

  record CompatibilityResolution(Status status, String characterId, String detail) {
    public CompatibilityResolution {
      status = Objects.requireNonNull(status, "status");
      if (status == Status.COMPATIBLE && (characterId == null || characterId.isBlank())) {
        throw new IllegalArgumentException("compatible resolution requires a nonblank character_id");
      }
    }

    static CompatibilityResolution compatible(String characterId) {
      return new CompatibilityResolution(Status.COMPATIBLE, characterId, null);
    }

    static CompatibilityResolution unresolved(String detail) {
      return new CompatibilityResolution(Status.UNRESOLVED, null, detail);
    }

    static CompatibilityResolution ambiguous(String detail) {
      return new CompatibilityResolution(Status.AMBIGUOUS, null, detail);
    }

    static CompatibilityResolution inconsistent(String detail) {
      return new CompatibilityResolution(Status.INCONSISTENT, null, detail);
    }

    static CompatibilityResolution unavailable(String detail) {
      return new CompatibilityResolution(Status.UNAVAILABLE, null, detail);
    }

    static CompatibilityResolution safeResolve(PetSubjectCompatibility compatibility, PetSubjectId legacySubjectId) {
      if (compatibility == null || legacySubjectId == null) {
        return unavailable("domain-02 PetCore read-through compatibility unavailable");
      }
      try {
        CompatibilityResolution resolution = compatibility.resolve(legacySubjectId);
        return resolution == null
            ? unavailable("domain-02 PetCore read-through compatibility unavailable")
            : resolution;
      } catch (RuntimeException failure) {
        return unavailable("domain-02 PetCore read-through compatibility unavailable: "
            + failure.getClass().getSimpleName());
      }
    }

    boolean allowsPhaseOneReadThrough() {
      return status == Status.COMPATIBLE && characterId != null && !characterId.isBlank();
    }

    String uiBlockReason() {
      return switch (status) {
        case COMPATIBLE -> "none";
        case UNRESOLVED -> "domain-character-unresolved";
        case AMBIGUOUS -> "domain-character-ambiguous";
        case INCONSISTENT -> "domain-character-inconsistent";
        case UNAVAILABLE -> "domain-character-unavailable";
      };
    }
  }

  enum Status {
    COMPATIBLE,
    UNRESOLVED,
    AMBIGUOUS,
    INCONSISTENT,
    UNAVAILABLE
  }
}

final class UnavailablePetSubjectCompatibility implements PetSubjectCompatibility {
  private final String detail;

  UnavailablePetSubjectCompatibility(String detail) {
    this.detail = detail;
  }

  @Override
  public CompatibilityResolution resolve(PetSubjectId legacySubjectId) {
    return CompatibilityResolution.unavailable(
        detail == null || detail.isBlank()
            ? "domain-02 PetCore read-through compatibility unavailable"
            : detail);
  }
}
