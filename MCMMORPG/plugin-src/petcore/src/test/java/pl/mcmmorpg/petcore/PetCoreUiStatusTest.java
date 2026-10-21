package pl.mcmmorpg.petcore;

import java.util.Optional;
import java.util.UUID;

public final class PetCoreUiStatusTest {
  public static void main(String[] args) {
    reportsFailClosedReasonsInPriorityOrder();
    reportsReadyActiveAndDuelStates();
    reportsCompatibilityFailures();
    reportsCompatibilityOutageAsUnavailable();
    blocksSummonDuringPetDuel();
    rejectsUnknownFieldsWithoutThrowing();
    reportsPlaceholderRegistrationOutcome();
    System.out.println("PetCoreUiStatusTest PASS");
  }

  static void reportsFailClosedReasonsInPriorityOrder() {
    PetCoreUiStatusService service = new PetCoreUiStatusService();
    PetSubjectId subject = PetSubjectId.of("profile-ui");

    PetCoreUiSnapshot noProfile =
        service.snapshot(
            Optional.empty(),
            Optional.empty(),
            AuthorityState.HEALTHY,
            ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1),
            Optional.empty(),
            false);
    check(noProfile.value("profile").equals("unverified"), "profile placeholder");
    check(noProfile.value("can_summon").equals("no"), "cannot summon without profile");
    check(noProfile.value("block_reason").equals("profile-unverified"), "profile block reason");
    check(noProfile.value("summary").equals("blocked:profile-unverified"), "profile summary");

    PetCoreUiSnapshot noAuthority =
        service.snapshot(
            Optional.of(subject),
            Optional.of(PetSubjectCompatibility.CompatibilityResolution.compatible("char-ui")),
            AuthorityState.UNAVAILABLE,
            ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1),
            Optional.empty(),
            false);
    check(noAuthority.value("authority").equals("offline"), "authority placeholder");
    check(noAuthority.value("block_reason").equals("authority-offline"), "authority block reason");
    check(noAuthority.value("can_summon").equals("no"), "cannot summon without authority");

    PetCoreUiSnapshot noContent =
        service.snapshot(
            Optional.of(subject),
            Optional.of(PetSubjectCompatibility.CompatibilityResolution.compatible("char-ui")),
            AuthorityState.HEALTHY,
            ContentResolution.unknown(),
            Optional.empty(),
            false);
    check(noContent.value("content").equals("unknown"), "content placeholder");
    check(noContent.value("block_reason").equals("content-unknown"), "content block reason");
    check(noContent.value("summary").equals("blocked:content-unknown"), "content summary");
  }

  static void reportsReadyActiveAndDuelStates() {
    PetCoreUiStatusService service = new PetCoreUiStatusService();
    PetSubjectId subject = PetSubjectId.of("profile-ui");
    ContentResolution content =
        ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1);

    PetCoreUiSnapshot ready =
        service.snapshot(
            Optional.of(subject),
            Optional.of(PetSubjectCompatibility.CompatibilityResolution.compatible("char-ui")),
            AuthorityState.HEALTHY,
            content,
            Optional.empty(),
            false);
    check(ready.value("profile").equals("profile-ui"), "verified profile value");
    check(ready.value("can_summon").equals("yes"), "can summon when ready and no active pet");
    check(ready.value("can_recall").equals("no"), "cannot recall missing pet");
    check(ready.value("summary").equals("ready"), "ready summary");

    ActiveLease lease =
        new ActiveLease(UUID.randomUUID(), 1, subject, UUID.randomUUID(), UUID.randomUUID());
    PetCoreUiSnapshot active =
        service.snapshot(
            Optional.of(subject),
            Optional.of(PetSubjectCompatibility.CompatibilityResolution.compatible("char-ui")),
            AuthorityState.HEALTHY,
            content,
            Optional.of(lease),
            false);
    check(active.value("active_pet").equals("active"), "active pet value");
    check(active.value("can_summon").equals("no"), "cannot summon second global pet");
    check(active.value("can_recall").equals("yes"), "can recall active pet");
    check(active.value("summary").equals("global-pet-active"), "active summary");

    PetCoreUiSnapshot duel =
        service.snapshot(
            Optional.of(subject),
            Optional.of(PetSubjectCompatibility.CompatibilityResolution.compatible("char-ui")),
            AuthorityState.HEALTHY,
            content,
            Optional.of(lease),
            true);
    check(duel.value("duel").equals("active"), "duel value");
    check(duel.value("summary").equals("pet-duel-active"), "duel summary");
  }

  static void reportsCompatibilityFailures() {
    PetCoreUiStatusService service = new PetCoreUiStatusService();
    PetSubjectId subject = PetSubjectId.of("profile-compat");
    ContentResolution content =
        ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1);

    PetCoreUiSnapshot unresolved =
        service.snapshot(
            Optional.of(subject),
            Optional.of(PetSubjectCompatibility.CompatibilityResolution.unresolved("missing mapping")),
            AuthorityState.HEALTHY,
            content,
            Optional.empty(),
            false);
    check(unresolved.value("block_reason").equals("domain-character-unresolved"), "unresolved compatibility reason");
    check(unresolved.value("can_summon").equals("no"), "unresolved compatibility blocks summon");

    PetCoreUiSnapshot ambiguous =
        service.snapshot(
            Optional.of(subject),
            Optional.of(PetSubjectCompatibility.CompatibilityResolution.ambiguous("duplicate mapping")),
            AuthorityState.HEALTHY,
            content,
            Optional.empty(),
            false);
    check(ambiguous.value("block_reason").equals("domain-character-ambiguous"), "ambiguous compatibility reason");
    check(
        ambiguous.value("summary").equals("blocked:domain-character-ambiguous"),
        "ambiguous compatibility summary");
  }

  static void reportsCompatibilityOutageAsUnavailable() {
    PetSubjectId subject = PetSubjectId.of("profile-ui-outage");
    PetSubjectCompatibility.CompatibilityResolution resolution =
        PetSubjectCompatibility.CompatibilityResolution.safeResolve(
            ignored -> { throw new IllegalStateException("mapping adapter unavailable"); }, subject);
    PetCoreUiSnapshot snapshot = new PetCoreUiStatusService().snapshot(
        Optional.of(subject), Optional.of(resolution), AuthorityState.HEALTHY,
        ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1),
        Optional.empty(), false);
    check(snapshot.value("block_reason").equals("domain-character-unavailable"),
        "compatibility outage must render the unavailable UI block reason");
    check(snapshot.value("can_summon").equals("no"),
        "compatibility outage must block UI summon state");
  }

  static void blocksSummonDuringPetDuel() {
    PetCoreUiStatusService service = new PetCoreUiStatusService();
    PetSubjectId subject = PetSubjectId.of("profile-duel");

    PetCoreUiSnapshot duel =
        service.snapshot(
            Optional.of(subject),
            Optional.of(PetSubjectCompatibility.CompatibilityResolution.compatible("char-duel")),
            AuthorityState.HEALTHY,
            ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1),
            Optional.empty(),
            true);

    check(duel.value("duel").equals("active"), "duel placeholder");
    check(duel.value("can_summon").equals("no"), "cannot summon during pet duel");
    check(duel.value("block_reason").equals("pet-duel-active"), "duel block reason");
  }

  static void rejectsUnknownFieldsWithoutThrowing() {
    PetCoreUiSnapshot snapshot =
        new PetCoreUiStatusService().snapshot(
            Optional.of(PetSubjectId.of("profile-ui")),
            Optional.of(PetSubjectCompatibility.CompatibilityResolution.compatible("char-ui")),
            AuthorityState.HEALTHY,
            ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1),
            Optional.empty(),
            false);
    check(snapshot.value("unknown-field").equals("unknown-field"), "unknown field should not throw");
  }

  static void reportsPlaceholderRegistrationOutcome() {
    check(
        PetCorePlaceholderRegistrationResult.from(true).registered(),
        "placeholder registered result");
    check(
        !PetCorePlaceholderRegistrationResult.from(false).registered(),
        "placeholder rejected result");
    check(
        PetCorePlaceholderRegistrationResult.from(false).logMessage().contains("rejected"),
        "placeholder rejection log message");
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
