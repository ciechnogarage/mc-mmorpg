package pl.mcmmorpg.petcore;

import java.util.Locale;
import java.util.Optional;

/**
 * Stable menu-facing status contract. Menus consume this surface directly and
 * must not infer PetCore runtime state from unrelated plugin data or entity scans.
 */
record PetCoreUiSnapshot(
    boolean profileVerified,
    String profile,
    String authority,
    String content,
    String activePet,
    String duel,
    boolean canSummon,
    boolean canRecall,
    String blockReason,
    String summary
) {
  String value(String field) {
    return switch (field.toLowerCase(Locale.ROOT)) {
      case "profile" -> profile;
      case "profile_verified" -> yesNo(profileVerified);
      case "authority" -> authority;
      case "content" -> content;
      case "active_pet" -> activePet;
      case "duel" -> duel;
      case "can_summon" -> yesNo(canSummon);
      case "can_recall" -> yesNo(canRecall);
      case "block_reason" -> blockReason;
      case "summary" -> summary;
      default -> "unknown-field";
    };
  }

  private static String yesNo(boolean value) {
    return value ? "yes" : "no";
  }
}

final class PetCoreUiStatusService {
  PetCoreUiSnapshot snapshot(
      Optional<PetSubjectId> subject,
      Optional<PetSubjectCompatibility.CompatibilityResolution> compatibility,
      AuthorityState authority,
      ContentResolution content,
      Optional<ActiveLease> activePet,
      boolean ownerInDuel
  ) {
    boolean profileVerified = subject.isPresent();
    boolean subjectCompatible =
        compatibility.map(PetSubjectCompatibility.CompatibilityResolution::allowsPhaseOneReadThrough)
            .orElse(false);
    boolean authorityHealthy = authority == AuthorityState.HEALTHY;
    boolean contentKnown = content != null && content.known();
    boolean active = activePet.isPresent();
    String block =
        blockReason(
            profileVerified,
            subjectCompatible,
            compatibility,
            authorityHealthy,
            contentKnown,
            ownerInDuel);

    return new PetCoreUiSnapshot(
        profileVerified,
        subject.map(PetSubjectId::value).orElse("unverified"),
        authorityHealthy ? "healthy" : "offline",
        contentKnown ? content.mode().name().toLowerCase(Locale.ROOT) : "unknown",
        active ? "active" : "none",
        ownerInDuel ? "active" : "none",
        profileVerified && subjectCompatible && authorityHealthy && contentKnown && !active && !ownerInDuel,
        active,
        block,
        summary(
            profileVerified,
            subjectCompatible,
            authorityHealthy,
            contentKnown,
            active,
            ownerInDuel,
            block));
  }

  private String blockReason(
      boolean profileVerified,
      boolean subjectCompatible,
      Optional<PetSubjectCompatibility.CompatibilityResolution> compatibility,
      boolean authorityHealthy,
      boolean contentKnown,
      boolean ownerInDuel
  ) {
    if (!profileVerified) {
      return "profile-unverified";
    }
    if (!subjectCompatible) {
      return compatibility
          .map(PetSubjectCompatibility.CompatibilityResolution::uiBlockReason)
          .orElse("domain-character-unavailable");
    }
    if (!authorityHealthy) {
      return "authority-offline";
    }
    if (!contentKnown) {
      return "content-unknown";
    }
    if (ownerInDuel) {
      return "pet-duel-active";
    }
    return "none";
  }

  private String summary(
      boolean profileVerified,
      boolean subjectCompatible,
      boolean authorityHealthy,
      boolean contentKnown,
      boolean active,
      boolean ownerInDuel,
      String block
  ) {
    if (!profileVerified || !subjectCompatible || !authorityHealthy || !contentKnown) {
      return "blocked:" + block;
    }
    if (ownerInDuel) {
      return "pet-duel-active";
    }
    if (active) {
      return "global-pet-active";
    }
    return "ready";
  }
}
