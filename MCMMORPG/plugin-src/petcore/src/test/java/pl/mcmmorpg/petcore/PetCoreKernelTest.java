package pl.mcmmorpg.petcore;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Small, dependency-free contract tests. Run through tools/build-petcore.sh.
 */
public final class PetCoreKernelTest {
  public static void main(String[] args) {
    sessionEpochFencesPreviousProfileOnCharacterSwitch();
    profilesAreProfileScopedAndIdempotentUnderContention();
    socialRunStaysLockedAcrossReconnectUntilExitOrReset();
    instancedResolutionRequiresCurrentDurableRunProjection();
    contentResolutionFailsClosedOnNullAndProjectionOutage();
    staleContentStateAndAuthorityOutageFailClosedWithinFiveTicks();
    admissionAndWorkQueuesRemainBoundedAndObservable();
    System.out.println("PetCoreKernelTest PASS");
  }

  private static void profilesAreProfileScopedAndIdempotentUnderContention() {
    var store = new InMemoryPetCoreStore();
    PetSubjectId subject = PetSubjectId.of("profile-a");
    try (var pool = Executors.newFixedThreadPool(4)) {
      var futures = java.util.stream.IntStream.range(0, 16)
          .mapToObj(index -> pool.submit(() -> store.ensureProfile(subject, "profile-create-" + index)))
          .toList();
      for (var future : futures) {
        check(future.get().subjectId().equals(subject), "profile creation must retain PetSubjectId");
      }
    } catch (Exception exception) {
      throw new AssertionError("profile projection contention failed", exception);
    }
    check(store.profileCount() == 1, "profile lock must prevent duplicate durable projections");
    check(store.ensureProfile(subject, "profile-create-0").createdAt().equals(
            store.ensureProfile(subject, "profile-create-0").createdAt()),
        "same idempotency key must return the same profile projection");
  }

  private static void sessionEpochFencesPreviousProfileOnCharacterSwitch() {
    var sessions = new ProfileSessionRegistry();
    UUID player = UUID.randomUUID();
    PetSubjectId firstProfile = PetSubjectId.of("profile-a");
    PetSubjectId secondProfile = PetSubjectId.of("profile-b");

    PlayerSession first = sessions.bind(player, firstProfile);
    PlayerSession second = sessions.bind(player, secondProfile);

    check(!sessions.isCurrent(first), "character switch must fence the old profile session");
    check(sessions.isCurrent(second), "new profile session must be current");
    check(second.epoch() > first.epoch(), "session epoch must advance on profile switch");

    UUID pet = UUID.randomUUID();
    var leases = new ActiveLeaseRegistry();
    ActiveLease oldLease = new ActiveLease(pet, 1, first.subjectId(), player);
    ActiveLease currentLease = new ActiveLease(pet, 2, second.subjectId(), player);
    leases.activate(oldLease);
    leases.activate(currentLease);
    check(!leases.isCurrent(oldLease), "stale runtime epoch must not mutate a newer executor lease");
    check(leases.isCurrent(currentLease), "full current lease tuple must be required for executor ownership");
  }

  private static void socialRunStaysLockedAcrossReconnectUntilExitOrReset() {
    var store = new InMemoryPetCoreStore();
    var runs = new ContentRunProjectionService(store, () -> Instant.parse("2026-07-20T00:00:00Z"));
    ContentRunId run = ContentRunId.of("dungeon-41");

    runs.partyChanged(run, "instance-41", 2, 1, "party-event-1");
    runs.partyChanged(run, "instance-41", 1, 2, "party-event-2");

    check(runs.find(run).orElseThrow().socialRun(), "social run must remain true after party leave");
    var afterReconnect = new ContentRunProjectionService(store, () -> Instant.parse("2026-07-20T00:01:00Z"));
    check(afterReconnect.find(run).orElseThrow().socialRun(), "projection must survive service restart/reconnect");
    check(afterReconnect.runExited(run, 3, "exit-1"), "exit must remove the run projection");
    check(afterReconnect.find(run).isEmpty(), "run exit must release social state");

    ContentRunId laterRun = ContentRunId.of("dungeon-42");
    afterReconnect.partyChanged(laterRun, "instance-42", 2, 1, "new-run-1");
    check(afterReconnect.runReset(laterRun, 2, "reset-1"), "reset must remove the current projection");
    check(afterReconnect.find(laterRun).isEmpty(), "run reset must allow a later new run id");

    runs.partyChanged(run, "instance-41", 1, 4, "party-event-4");
    var staleProvider = new ContentEligibilityService(
        (subject, session) -> ContentResolution.resolved(ContentMode.INSTANCE_SOLO, Optional.of(run), 1, 3), runs);
    PlayerSession session = new PlayerSession(UUID.randomUUID(), PetSubjectId.of("profile-a"), 1);
    check(!staleProvider.resolve(session.subjectId(), session).known(),
        "provider state older than the durable run revision must fail closed");
    var unavailableProvider = new ContentEligibilityService(
        (subject, playerSession) -> { throw new IllegalStateException("provider unavailable"); }, runs);
    check(!unavailableProvider.resolve(session.subjectId(), session).known(),
        "provider outage must become an explicit fail-closed resolution");
  }

  private static void staleContentStateAndAuthorityOutageFailClosedWithinFiveTicks() {
    var recalls = new RecordingRecallSink();
    var gate = new FailClosedGate(recalls);
    var lease = new ActiveLease(UUID.randomUUID(), 4L, PetSubjectId.of("profile-a"), UUID.randomUUID());

    check(gate.allowNewAction(AuthorityState.HEALTHY,
            ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1)),
        "known healthy state should allow a new action");
    check(!gate.allowNewAction(AuthorityState.HEALTHY, ContentResolution.unknown()),
        "unknown provider state must reject new actions");
    gate.fence(lease, 100, "content state is stale");
    check(!gate.allowAutonomousAbility(lease), "fenced executor must stop autonomous abilities immediately");
    gate.onTick(104);
    check(recalls.recalled.isEmpty(), "recall must not occur before the five-tick deadline");
    gate.onTick(105);
    check(recalls.recalled.size() == 1, "fenced executor must recall no later than five ticks");
    check(recalls.lastStatus.contains("companion resting while state is verified"), "status must be recoverable");

    check(!gate.allowNewAction(AuthorityState.UNAVAILABLE, ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1)),
        "database/provider authority outage must reject actions without fallback");
  }

  private static void instancedResolutionRequiresCurrentDurableRunProjection() {
    var store = new InMemoryPetCoreStore();
    var atStart = Instant.parse("2026-07-20T00:00:00Z");
    var runs = new ContentRunProjectionService(store, () -> atStart);
    PetSubjectId subject = PetSubjectId.of("profile-a");
    PlayerSession session = new PlayerSession(UUID.randomUUID(), subject, 1);
    ContentRunId missingRun = ContentRunId.of("missing-run");

    var missing = new ContentEligibilityService((profile, playerSession) ->
        ContentResolution.resolved(ContentMode.INSTANCE_SOLO, Optional.of(missingRun), 1, 1), runs);
    check(!missing.resolve(subject, session).known(), "instanced response without a durable run projection must be unknown");
    check(!missing.adventurerAllowed(subject, session), "missing instanced projection must block Adventurer eligibility");

    ContentRunId expiredRun = ContentRunId.of("expired-run");
    store.applyRunEvent(new PartyChanged(expiredRun, "instance-expired", 1, 1, "expired-run-event"), atStart);
    var afterTtl = new ContentRunProjectionService(store, () -> atStart.plusSeconds(86_401));
    var expired = new ContentEligibilityService((profile, playerSession) ->
        ContentResolution.resolved(ContentMode.INSTANCE_SOLO, Optional.of(expiredRun), 1, 1), afterTtl);
    check(!expired.resolve(subject, session).known(), "expired instanced projection must fail closed");
    check(afterTtl.find(expiredRun).isEmpty(), "expired run projection must be cleaned up before a later run can reuse capacity");

    ContentRunId staleRun = ContentRunId.of("stale-run");
    runs.partyChanged(staleRun, "instance-stale", 1, 1, "stale-run-event");
    var stale = new ContentEligibilityService((profile, playerSession) ->
        ContentResolution.resolved(ContentMode.INSTANCE_SOLO, Optional.of(staleRun), 1, 2), runs);
    check(!stale.resolve(subject, session).known(), "run revision mismatch must not permit an instanced action");

    var openWorld = new ContentEligibilityService((profile, playerSession) ->
        ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1), runs);
    check(openWorld.adventurerAllowed(subject, session), "only non-instanced modes may use direct party-size eligibility");
  }

  private static void contentResolutionFailsClosedOnNullAndProjectionOutage() {
    PetSubjectId subject = PetSubjectId.of("profile-outage");
    PlayerSession session = new PlayerSession(UUID.randomUUID(), subject, 1);
    ContentRunId run = ContentRunId.of("projection-outage");
    ContentRunProjectionService throwingRuns = new ContentRunProjectionService(
        new PetCoreStore() {
          @Override public PetProfileProjection ensureProfile(PetSubjectId id, String operationId) { return null; }
          @Override public Optional<ContentRunProjection> findRun(ContentRunId id) {
            throw new DurableAuthorityException("projection unavailable", new IllegalStateException("store down"));
          }
          @Override public boolean deleteExpiredRun(ContentRunId id, Instant now) {
            throw new DurableAuthorityException("projection unavailable", new IllegalStateException("store down"));
          }
          @Override public RunApplyResult applyRunEvent(RunProjectionEvent event, Instant now) { return null; }
        },
        Instant::now);
    ContentEligibilityService nullProvider = new ContentEligibilityService(
        (ignored, ignoredSession) -> null, throwingRuns);
    check(!nullProvider.resolve(subject, session).known(),
        "null content provider result must fail closed without throwing");

    ContentEligibilityService throwingProjection = new ContentEligibilityService(
        (ignored, ignoredSession) -> ContentResolution.resolved(
            ContentMode.INSTANCE_SOLO, Optional.of(run), 1, 1),
        throwingRuns);
    check(!throwingProjection.resolve(subject, session).known(),
        "durable projection outage must fail closed without throwing");
    check(!throwingProjection.adventurerAllowed(subject, session),
        "durable projection outage must block dependent eligibility");
  }

  private static void admissionAndWorkQueuesRemainBoundedAndObservable() {
    var telemetry = new PetCoreTelemetry();
    var admission = new PetAdmissionService(new CapacityLimits(1, 2, 2, 2, 2, 1), 5, telemetry);
    PetSubjectId owner = PetSubjectId.of("profile-a");
    AdmissionContext context = new AdmissionContext(owner, CompanionPolicy.GLOBAL_PET, "world", Optional.empty());

    check(admission.reserve(context, "request-1", 100).accepted(), "first reservation should be admitted");
    check(!admission.reserve(context, "request-2", 101).accepted(), "owner cap must synchronously reject, not queue");
    check(telemetry.admissionRejects(AdmissionRejection.OWNER_CAP) == 1, "owner cap rejection must be observable");
    check(admission.activeAndReservedCount() == 1, "reservation must count against bounded global capacity");
    AdmissionDecision reAdmitted = admission.reserve(context, "request-3", 105);
    check(reAdmitted.accepted(), "expired reservation must release capacity before re-admission");
    check(telemetry.reservationExpirations() == 1, "reservation timeout must emit telemetry");
    check(admission.activeAndReservedCount() == 1, "expired reservation must not leak capacity");
    check(admission.release(reAdmitted.reservationId().orElseThrow()), "failed reservation path must release capacity");
    check(admission.reserve(context, "request-4", 106).accepted(), "released reservation must permit a later request");

    var queue = new BoundedWorkQueue(WorkPriority.TARGET_SCAN, 1, 3, telemetry);
    check(queue.offer(new ScheduledWork("scan-1", 0)), "first optional work should fit");
    check(!queue.offer(new ScheduledWork("scan-2", 0)), "full bounded queue must reject optional work");
    check(queue.poll(4).isEmpty(), "expired optional work must be dropped, never executed stale");
    check(telemetry.queueDrops(WorkPriority.TARGET_SCAN) == 2, "queue rejection and expiry must be counted");
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
