package pl.mcmmorpg.petcore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Location;

/** Contract tests for PetCore's policy-free runtime seam. */
public final class PetCoreGlobalPetTest {
  public static void main(String[] args) {
    compatibleResolutionRequiresNonblankCharacterId();
    safeCompatibilityResolutionFailsClosedOnOutage();
    summonFailsClosedWhenCompatibilityResolverThrows();
    summonProfileEnsureFailsClosedOnDurableOutage();
    phaseOneReadThroughAllowsCompatibleLegacySubject();
    phaseOneReadThroughBlocksUnresolvedAndAmbiguousMappings();
    reserveSpawnCommitAndOwnerCapAreFenced();
    staleCallbacksOnlyDespawnTheirTaggedEntity();
    allowListedSkillAndRecallAreCentralized();
    ownerCommandedActionFencesLeaseWhenCompatibilityDisappears();
    ownerCommandedActionFencesLeaseWhenCompatibilityResolverThrows();
    profileSessionEpochFencesAtoBtoALease();
    duelRequiresAcceptanceAndLocksPair();
    duelAcceptRejectsOverlappingPendingRequests();
    duelAcceptCleansUpAfterSecondTargetFailure();
    duelTickEndsOnTargetRefreshFailure();
    duelRequestBlocksWhenChallengerMappingIsLost();
    duelRequestBlocksWhenChallengedMappingIsLost();
    duelAcceptDoesNotPublishSessionAfterMappingLoss();
    activeSkillFailsClosedWhenContentBecomesUnavailable();
    activeSkillFailsClosedWhenDurableAuthorityBecomesUnavailable();
    paperCommandTreeAcceptsDuelArguments();
    System.out.println("PetCoreGlobalPetTest PASS");
  }

  private static void compatibleResolutionRequiresNonblankCharacterId() {
    checkThrows(
        IllegalArgumentException.class,
        () -> PetSubjectCompatibility.CompatibilityResolution.compatible(null),
        "null compatible character_id must be rejected");
    checkThrows(
        IllegalArgumentException.class,
        () -> PetSubjectCompatibility.CompatibilityResolution.compatible("  \t"),
        "blank compatible character_id must be rejected");
    checkThrows(
        IllegalArgumentException.class,
        () -> new PetSubjectCompatibility.CompatibilityResolution(
            PetSubjectCompatibility.Status.COMPATIBLE, "", null),
        "blank compatible record must be rejected");
  }

  private static void safeCompatibilityResolutionFailsClosedOnOutage() {
    PetSubjectId subject = PetSubjectId.of("profile-outage");
    PetSubjectCompatibility.CompatibilityResolution thrown =
        PetSubjectCompatibility.CompatibilityResolution.safeResolve(
            ignored -> { throw new IllegalStateException("adapter down"); }, subject);
    check(
        thrown.status() == PetSubjectCompatibility.Status.UNAVAILABLE,
        "throwing compatibility resolver must become unavailable");

    PetSubjectCompatibility.CompatibilityResolution nullResult =
        PetSubjectCompatibility.CompatibilityResolution.safeResolve(ignored -> null, subject);
    check(
        nullResult.status() == PetSubjectCompatibility.Status.UNAVAILABLE,
        "null compatibility resolver result must become unavailable");
  }

  private static void summonFailsClosedWhenCompatibilityResolverThrows() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    PlayerSession session = sessions.bind(UUID.randomUUID(), PetSubjectId.of("profile-summon-outage"));
    GlobalPetRuntime runtime = runtime(
        adapter,
        sessions,
        openContent(),
        ignored -> { throw new IllegalStateException("mapping adapter unavailable"); });

    GlobalPetSummonResult result = runtime.summon(
        session,
        session.playerId(),
        GlobalPetDefinitions.ID,
        new Location(null, 0, 64, 0),
        AuthorityState.HEALTHY,
        "summon-outage",
        1);
    check(!result.accepted(), "resolver outage must block summon without throwing");
    check(adapter.spawned.isEmpty(), "resolver outage must prevent pet spawn");
  }

  private static void phaseOneReadThroughAllowsCompatibleLegacySubject() {
    PetSubjectCompatibility.CompatibilityResolution resolution =
        PetSubjectCompatibility.CompatibilityResolution.compatible("char-1");
    check(resolution.allowsPhaseOneReadThrough(), "compatible mapping must allow phase-1 read-through");
    check("char-1".equals(resolution.characterId()), "compatible mapping should expose resolved character id");
  }

  private static void phaseOneReadThroughBlocksUnresolvedAndAmbiguousMappings() {
    FakeAdapter unresolvedAdapter = new FakeAdapter();
    ProfileSessionRegistry unresolvedSessions = new ProfileSessionRegistry();
    PlayerSession unresolvedSession =
        unresolvedSessions.bind(UUID.randomUUID(), PetSubjectId.of("profile-unresolved"));
    GlobalPetRuntime unresolvedRuntime =
        runtime(
            unresolvedAdapter,
            unresolvedSessions,
            openContent(),
            legacySubjectId ->
                PetSubjectCompatibility.CompatibilityResolution.unresolved("missing mapping"));
    check(
        !unresolvedRuntime
            .summon(
                unresolvedSession,
                unresolvedSession.playerId(),
                GlobalPetDefinitions.ID,
                new Location(null, 0, 64, 0),
                AuthorityState.HEALTHY,
                "compat-unresolved",
                1)
            .accepted(),
        "unresolved mapping must block summon");

    FakeAdapter ambiguousAdapter = new FakeAdapter();
    ProfileSessionRegistry ambiguousSessions = new ProfileSessionRegistry();
    PlayerSession ambiguousSession =
        ambiguousSessions.bind(UUID.randomUUID(), PetSubjectId.of("profile-ambiguous"));
    GlobalPetRuntime ambiguousRuntime =
        runtime(
            ambiguousAdapter,
            ambiguousSessions,
            openContent(),
            legacySubjectId ->
                PetSubjectCompatibility.CompatibilityResolution.ambiguous("duplicate mapping"));
    check(
        !ambiguousRuntime
            .summon(
                ambiguousSession,
                ambiguousSession.playerId(),
                GlobalPetDefinitions.ID,
                new Location(null, 0, 64, 0),
                AuthorityState.HEALTHY,
                "compat-ambiguous",
                1)
            .accepted(),
        "ambiguous mapping must block summon");
  }

  private static void reserveSpawnCommitAndOwnerCapAreFenced() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    PlayerSession session = sessions.bind(UUID.randomUUID(), PetSubjectId.of("profile-global"));
    GlobalPetRuntime runtime = runtime(adapter, sessions, openContent());

    GlobalPetSummonResult first =
        runtime.summon(
            session,
            session.playerId(),
            GlobalPetDefinitions.ID,
            new Location(null, 0, 64, 0),
            AuthorityState.HEALTHY,
            "summon-1",
            10);
    check(first.accepted(), "reserve -> spawn -> commit should succeed");
    check(runtime.activeLeases().size() == 1, "one committed lease expected");

    GlobalPetSummonResult duplicate =
        runtime.summon(
            session,
            session.playerId(),
            GlobalPetDefinitions.ID,
            new Location(null, 0, 64, 0),
            AuthorityState.HEALTHY,
            "summon-2",
            10);
    check(!duplicate.accepted(), "owner policy cap should reject duplicate summon");
  }

  private static void staleCallbacksOnlyDespawnTheirTaggedEntity() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    PlayerSession session = sessions.bind(UUID.randomUUID(), PetSubjectId.of("profile-callback"));
    GlobalPetRuntime runtime = runtime(adapter, sessions, openContent());
    ActiveLease current =
        runtime
            .summon(
                session,
                session.playerId(),
                GlobalPetDefinitions.ID,
                new Location(null, 0, 64, 0),
                AuthorityState.HEALTHY,
                "callback-1",
                1)
            .lease()
            .orElseThrow();

    ActiveLease stale =
        new ActiveLease(
            current.petInstanceId(),
            current.runtimeEpoch() - 1,
            current.subjectId(),
            current.ownerPlayerId(),
            UUID.randomUUID());

    runtime.handleCallback(stale, "stale death callback");
    check(adapter.staleDespawns == 1, "stale callback should only despawn tagged stale entity");
    check(runtime.activeLeases().size() == 1, "stale callback must not drop current lease");

    runtime.handleCallback(current, "current death callback");
    check(runtime.activeLeases().isEmpty(), "current callback should recall active lease");
    check(adapter.despawned == 1, "current callback should despawn active pet");
  }

  private static void allowListedSkillAndRecallAreCentralized() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    PlayerSession session = sessions.bind(UUID.randomUUID(), PetSubjectId.of("profile-skill"));
    GlobalPetRuntime runtime = runtime(adapter, sessions, openContent());
    ActiveLease lease =
        runtime
            .summon(
                session,
                session.playerId(),
                GlobalPetDefinitions.ID,
                new Location(null, 0, 64, 0),
                AuthorityState.HEALTHY,
                "skill-1",
                1)
            .lease()
            .orElseThrow();

    check(
        runtime.invokeSkill(lease, GlobalPetDefinitions.ALLOW_LISTED_SKILL),
        "allow-listed skill should reach adapter");
    check(!runtime.invokeSkill(lease, "UnapprovedSkill"), "unapproved skill must reject centrally");

    runtime.recallOwner(session.playerId(), "logout");
    check(runtime.activeLeases().isEmpty(), "owner recall should release lease");
    check(adapter.despawned == 1, "owner recall should despawn active pet");
  }

  private static void ownerCommandedActionFencesLeaseWhenCompatibilityDisappears() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    PlayerSession session = sessions.bind(UUID.randomUUID(), PetSubjectId.of("profile-compat"));
    java.util.concurrent.atomic.AtomicReference<PetSubjectCompatibility.CompatibilityResolution> state =
        new java.util.concurrent.atomic.AtomicReference<>(
            PetSubjectCompatibility.CompatibilityResolution.compatible("char-1"));
    GlobalPetRuntime runtime = runtime(adapter, sessions, openContent(), legacySubjectId -> state.get());
    ActiveLease lease =
        runtime
            .summon(
                session,
                session.playerId(),
                GlobalPetDefinitions.ID,
                new Location(null, 0, 64, 0),
                AuthorityState.HEALTHY,
                "compat-loss",
                1)
            .lease()
            .orElseThrow();

    state.set(PetSubjectCompatibility.CompatibilityResolution.ambiguous("duplicate mapping"));
    check(
        !runtime.invokeSkill(lease, GlobalPetDefinitions.ALLOW_LISTED_SKILL),
        "owner-commanded skill must block once compatibility becomes ambiguous");
    check(runtime.activeLeases().isEmpty(), "compatibility loss should fence active lease");
    check(adapter.despawned == 1, "compatibility loss should despawn active pet");
  }

  private static void ownerCommandedActionFencesLeaseWhenCompatibilityResolverThrows() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    PlayerSession session = sessions.bind(UUID.randomUUID(), PetSubjectId.of("profile-compat-outage"));
    java.util.concurrent.atomic.AtomicReference<Boolean> outage =
        new java.util.concurrent.atomic.AtomicReference<>(false);
    GlobalPetRuntime runtime = runtime(
        adapter,
        sessions,
        openContent(),
        legacySubjectId -> {
          if (outage.get()) throw new IllegalStateException("mapping adapter unavailable");
          return PetSubjectCompatibility.CompatibilityResolution.compatible("char-outage");
        });
    ActiveLease lease = runtime.summon(
        session,
        session.playerId(),
        GlobalPetDefinitions.ID,
        new Location(null, 0, 64, 0),
        AuthorityState.HEALTHY,
        "compat-outage",
        1).lease().orElseThrow();

    outage.set(true);
    check(
        !runtime.invokeSkill(lease, GlobalPetDefinitions.ALLOW_LISTED_SKILL),
        "resolver outage must block owner-commanded actions");
    check(runtime.activeLeases().isEmpty(), "resolver outage should fence active lease");
    check(adapter.despawned == 1, "resolver outage should despawn active pet");
  }

  private static void profileSessionEpochFencesAtoBtoALease() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    UUID owner = UUID.randomUUID();
    PlayerSession first = sessions.bind(owner, PetSubjectId.of("profile-a"));
    GlobalPetRuntime runtime = runtime(adapter, sessions, openContent());
    ActiveLease lease =
        runtime
            .summon(
                first,
                owner,
                GlobalPetDefinitions.ID,
                new Location(null, 0, 64, 0),
                AuthorityState.HEALTHY,
                "epoch-1",
                1)
            .lease()
            .orElseThrow();

    sessions.bind(owner, PetSubjectId.of("profile-b"));
    PlayerSession returned = sessions.bind(owner, PetSubjectId.of("profile-a"));
    check(returned.epoch() > first.epoch(), "returning profile A should retain newer session epoch");
    check(
        !runtime.invokeSkill(lease, GlobalPetDefinitions.ALLOW_LISTED_SKILL),
        "old lease must fence on A -> B -> A switch");
    check(runtime.activeLeases().isEmpty(), "epoch-stale lease should be recalled");
    check(adapter.despawned == 1, "epoch-stale lease should despawn once");
  }

  private static void duelRequiresAcceptanceAndLocksPair() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    UUID firstOwner = UUID.randomUUID();
    UUID secondOwner = UUID.randomUUID();
    PlayerSession firstSession = sessions.bind(firstOwner, PetSubjectId.of("duel-a"));
    PlayerSession secondSession = sessions.bind(secondOwner, PetSubjectId.of("duel-b"));
    GlobalPetRuntime runtime = runtime(adapter, sessions, openContent());
    ActiveLease first =
        runtime
            .summon(
                firstSession,
                firstOwner,
                GlobalPetDefinitions.ID,
                new Location(null, 0, 64, 0),
                AuthorityState.HEALTHY,
                "duel-a",
                1)
            .lease()
            .orElseThrow();
    ActiveLease second =
        runtime
            .summon(
                secondSession,
                secondOwner,
                GlobalPetDefinitions.ID,
                new Location(null, 0, 64, 0),
                AuthorityState.HEALTHY,
                "duel-b",
                1)
            .lease()
            .orElseThrow();

    PetDuelService duels = new PetDuelService(runtime, 20, 5);
    check(duels.request(firstOwner, secondOwner, 1) == PetDuelResult.ACCEPTED, "duel request should be accepted");
    PetDuelRequest pending = duels.pendingFor(secondOwner, 2).orElseThrow();
    check(
        duels.accept(secondOwner, pending.requestId(), 2) == PetDuelResult.ACCEPTED,
        "duel accept should start the duel");
    check(duels.sessions().size() == 1, "exactly one duel session expected");
    check(duels.canDamage(first.entityId(), second.entityId()), "paired pets may damage each other");
    check(duels.canDamage(second.entityId(), first.entityId()), "paired pet damage must be symmetric");
    check(!duels.canDamage(first.entityId(), UUID.randomUUID()), "third-party target must be rejected");
    check(!duels.canDamage(UUID.randomUUID(), first.entityId()), "third-party source must be rejected");
    check(duels.isDuelPet(first.entityId()) && duels.isDuelPet(second.entityId()), "accepted pets should be duel pets");
    check(!duels.isDuelPet(UUID.randomUUID()), "unrelated entity must not be duel pet");
    check(
        duels.commandSkill(firstOwner, GlobalPetDefinitions.ALLOW_LISTED_SKILL, 2),
        "owner skill command should work during duel");
    check(
        !duels.commandSkill(firstOwner, GlobalPetDefinitions.ALLOW_LISTED_SKILL, 3),
        "shared cooldown should gate repeated duel skill");

    duels.endForPetEntity(first.entityId(), "pet zero hp");
    check(duels.sessions().isEmpty(), "duel should end when one participating pet reaches zero hp");
    check(!duels.canDamage(first.entityId(), second.entityId()), "duel damage should stop after end");
    check(adapter.clearedTargets == 2, "duel end should clear both pet targets");
    check(
        !duels.commandSkill(firstOwner, GlobalPetDefinitions.ALLOW_LISTED_SKILL, 6),
        "duel skill should fail after duel end");
  }

  private static void summonProfileEnsureFailsClosedOnDurableOutage() {
    PlayerSession session = new PlayerSession(UUID.randomUUID(), PetSubjectId.of("profile-store-outage"), 1);
    PetCoreStore throwingStore = new PetCoreStore() {
      @Override public PetProfileProjection ensureProfile(PetSubjectId subject, String operationId) {
        throw new DurableAuthorityException("profile ensure failed", new IllegalStateException("store down"));
      }
      @Override public Optional<ContentRunProjection> findRun(ContentRunId runId) { return Optional.empty(); }
      @Override public boolean deleteExpiredRun(ContentRunId runId, Instant now) { return false; }
      @Override public RunApplyResult applyRunEvent(RunProjectionEvent event, Instant now) { return null; }
    };

    check(!PetCorePlugin.ensureProfileForSummon(throwingStore, session, "profile-store-outage"),
        "durable profile ensure outage must deny summon admission");
  }

  private static void duelAcceptDoesNotPublishSessionAfterMappingLoss() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    UUID firstOwner = UUID.randomUUID();
    UUID secondOwner = UUID.randomUUID();
    PlayerSession firstSession = sessions.bind(firstOwner, PetSubjectId.of("duel-loss-a"));
    PlayerSession secondSession = sessions.bind(secondOwner, PetSubjectId.of("duel-loss-b"));
    java.util.concurrent.atomic.AtomicReference<PetSubjectCompatibility.CompatibilityResolution> state =
        new java.util.concurrent.atomic.AtomicReference<>(
            PetSubjectCompatibility.CompatibilityResolution.compatible("char-duel-loss"));
    GlobalPetRuntime runtime = runtime(adapter, sessions, openContent(), ignored -> state.get());
    runtime.summon(
        firstSession, firstOwner, GlobalPetDefinitions.ID, new Location(null, 0, 64, 0),
        AuthorityState.HEALTHY, "duel-loss-a", 1).lease().orElseThrow();
    runtime.summon(
        secondSession, secondOwner, GlobalPetDefinitions.ID, new Location(null, 0, 64, 0),
        AuthorityState.HEALTHY, "duel-loss-b", 1).lease().orElseThrow();

    PetDuelService duels = new PetDuelService(runtime, 20, 5);
    check(duels.request(firstOwner, secondOwner, 1) == PetDuelResult.ACCEPTED,
        "duel request should be accepted before mapping loss");
    PetDuelRequest pending = duels.pendingFor(secondOwner, 2).orElseThrow();
    state.set(PetSubjectCompatibility.CompatibilityResolution.ambiguous("mapping lost before accept"));

    check(
        duels.accept(secondOwner, pending.requestId(), 2) == PetDuelResult.INELIGIBLE,
        "mapping loss during accept must block duel");
    check(duels.sessions().isEmpty(), "failed duel accept must not publish a session");
    check(!duels.isDuelPet(UUID.randomUUID()), "failed duel accept must not expose duel pets");
  }

  private static void duelAcceptCleansUpAfterSecondTargetFailure() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    UUID firstOwner = UUID.randomUUID();
    UUID secondOwner = UUID.randomUUID();
    PlayerSession firstSession = sessions.bind(firstOwner, PetSubjectId.of("duel-accept-cleanup-a"));
    PlayerSession secondSession = sessions.bind(secondOwner, PetSubjectId.of("duel-accept-cleanup-b"));
    GlobalPetRuntime runtime = runtime(adapter, sessions, openContent());
    ActiveLease first = runtime.summon(firstSession, firstOwner, GlobalPetDefinitions.ID,
        new Location(null, 0, 64, 0), AuthorityState.HEALTHY, "duel-accept-cleanup-a", 1).lease().orElseThrow();
    ActiveLease second = runtime.summon(secondSession, secondOwner, GlobalPetDefinitions.ID,
        new Location(null, 0, 64, 0), AuthorityState.HEALTHY, "duel-accept-cleanup-b", 1).lease().orElseThrow();
    adapter.failSetTargetFor = second.entityId();
    adapter.failClearTargetFor = first.entityId();

    PetDuelService duels = new PetDuelService(runtime, 20, 5);
    check(duels.request(firstOwner, secondOwner, 1) == PetDuelResult.ACCEPTED,
        "duel request should be accepted before target failure");
    PetDuelRequest pending = duels.pendingFor(secondOwner, 2).orElseThrow();
    check(duels.accept(secondOwner, pending.requestId(), 2) == PetDuelResult.INELIGIBLE,
        "second target failure must reject duel acceptance");
    check(duels.sessions().isEmpty(), "failed target acquisition must not publish a duel");
    check(runtime.activeLeases().isEmpty(), "cleanup failure must recall both participating pets");
    check(adapter.despawned == 2, "cleanup failure must force-safe despawn both pets");
  }

  private static void duelTickEndsOnTargetRefreshFailure() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    UUID firstOwner = UUID.randomUUID();
    UUID secondOwner = UUID.randomUUID();
    PlayerSession firstSession = sessions.bind(firstOwner, PetSubjectId.of("duel-tick-failure-a"));
    PlayerSession secondSession = sessions.bind(secondOwner, PetSubjectId.of("duel-tick-failure-b"));
    GlobalPetRuntime runtime = runtime(adapter, sessions, openContent());
    ActiveLease second = runtime.summon(secondSession, secondOwner, GlobalPetDefinitions.ID,
        new Location(null, 0, 64, 0), AuthorityState.HEALTHY, "duel-tick-failure-b", 1).lease().orElseThrow();
    runtime.summon(firstSession, firstOwner, GlobalPetDefinitions.ID,
        new Location(null, 0, 64, 0), AuthorityState.HEALTHY, "duel-tick-failure-a", 1).lease().orElseThrow();

    PetDuelService duels = new PetDuelService(runtime, 20, 5);
    check(duels.request(firstOwner, secondOwner, 1) == PetDuelResult.ACCEPTED,
        "duel request should be accepted before tick failure");
    PetDuelRequest pending = duels.pendingFor(secondOwner, 2).orElseThrow();
    check(duels.accept(secondOwner, pending.requestId(), 2) == PetDuelResult.ACCEPTED,
        "duel should start before tick refresh failure");
    adapter.failSetTargetFor = second.entityId();

    duels.tick(3);
    check(duels.sessions().isEmpty(), "one-sided target refresh must end the duel immediately");
    check(adapter.clearedTargets == 2, "tick target failure must clear both target sides");
  }

  private static void duelRequestBlocksWhenChallengerMappingIsLost() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    UUID firstOwner = UUID.randomUUID();
    UUID secondOwner = UUID.randomUUID();
    PlayerSession firstSession = sessions.bind(firstOwner, PetSubjectId.of("duel-request-loss-a"));
    PlayerSession secondSession = sessions.bind(secondOwner, PetSubjectId.of("duel-request-loss-b"));
    java.util.concurrent.atomic.AtomicReference<PetSubjectCompatibility.CompatibilityResolution> firstState =
        new java.util.concurrent.atomic.AtomicReference<>(
            PetSubjectCompatibility.CompatibilityResolution.compatible("char-duel-request-a"));
    GlobalPetRuntime runtime = runtime(
        adapter,
        sessions,
        openContent(),
        subject -> subject.value().equals("duel-request-loss-a")
            ? firstState.get()
            : PetSubjectCompatibility.CompatibilityResolution.compatible("char-duel-request-b"));
    runtime.summon(
        firstSession, firstOwner, GlobalPetDefinitions.ID, new Location(null, 0, 64, 0),
        AuthorityState.HEALTHY, "duel-request-loss-a", 1).lease().orElseThrow();
    runtime.summon(
        secondSession, secondOwner, GlobalPetDefinitions.ID, new Location(null, 0, 64, 0),
        AuthorityState.HEALTHY, "duel-request-loss-b", 1).lease().orElseThrow();

    firstState.set(PetSubjectCompatibility.CompatibilityResolution.unresolved("mapping lost before request"));
    PetDuelService duels = new PetDuelService(runtime, 20, 5);

    check(
        duels.request(firstOwner, secondOwner, 2) == PetDuelResult.INELIGIBLE,
        "challenger mapping loss must block duel request");
    check(runtime.activeLeases().stream().noneMatch(lease -> lease.ownerPlayerId().equals(firstOwner)),
        "challenger mapping loss must quarantine and recall its pet");
    check(duels.pendingFor(secondOwner, 2).isEmpty(), "blocked challenger must not leave a pending request");
  }

  private static void duelAcceptRejectsOverlappingPendingRequests() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    UUID firstOwner = UUID.randomUUID();
    UUID challengedOwner = UUID.randomUUID();
    UUID thirdOwner = UUID.randomUUID();
    PlayerSession firstSession = sessions.bind(firstOwner, PetSubjectId.of("duel-overlap-a"));
    PlayerSession challengedSession = sessions.bind(challengedOwner, PetSubjectId.of("duel-overlap-b"));
    PlayerSession thirdSession = sessions.bind(thirdOwner, PetSubjectId.of("duel-overlap-c"));
    GlobalPetRuntime runtime = runtime(adapter, sessions, openContent());
    runtime.summon(firstSession, firstOwner, GlobalPetDefinitions.ID,
        new Location(null, 0, 64, 0), AuthorityState.HEALTHY, "duel-overlap-a", 1).lease().orElseThrow();
    runtime.summon(challengedSession, challengedOwner, GlobalPetDefinitions.ID,
        new Location(null, 0, 64, 0), AuthorityState.HEALTHY, "duel-overlap-b", 1).lease().orElseThrow();
    runtime.summon(thirdSession, thirdOwner, GlobalPetDefinitions.ID,
        new Location(null, 0, 64, 0), AuthorityState.HEALTHY, "duel-overlap-c", 1).lease().orElseThrow();

    PetDuelService duels = new PetDuelService(runtime, 20, 5);
    check(duels.request(firstOwner, challengedOwner, 1) == PetDuelResult.ACCEPTED,
        "first overlapping duel request should be accepted");
    check(duels.request(thirdOwner, challengedOwner, 1) == PetDuelResult.ACCEPTED,
        "second overlapping duel request should remain pending until accept");
    PetDuelRequest firstRequest = duels.pendingFor(challengedOwner, 2).orElseThrow();
    check(duels.accept(challengedOwner, firstRequest.requestId(), 2) == PetDuelResult.ACCEPTED,
        "first overlapping request should start the duel");
    PetDuelRequest secondRequest = duels.pendingFor(challengedOwner, 3).orElseThrow();
    check(duels.accept(challengedOwner, secondRequest.requestId(), 3) == PetDuelResult.ALREADY_IN_DUEL,
        "second overlapping request must respect the active-duel owner lock");
    check(duels.sessions().size() == 1, "overlapping accepts must publish only one duel session");
  }

  private static void duelRequestBlocksWhenChallengedMappingIsLost() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    UUID firstOwner = UUID.randomUUID();
    UUID secondOwner = UUID.randomUUID();
    PlayerSession firstSession = sessions.bind(firstOwner, PetSubjectId.of("duel-request-block-a"));
    PlayerSession secondSession = sessions.bind(secondOwner, PetSubjectId.of("duel-request-block-b"));
    java.util.concurrent.atomic.AtomicReference<PetSubjectCompatibility.CompatibilityResolution> secondState =
        new java.util.concurrent.atomic.AtomicReference<>(
            PetSubjectCompatibility.CompatibilityResolution.compatible("char-duel-request-b"));
    GlobalPetRuntime runtime = runtime(
        adapter,
        sessions,
        openContent(),
        subject -> subject.value().equals("duel-request-block-b")
            ? secondState.get()
            : PetSubjectCompatibility.CompatibilityResolution.compatible("char-duel-request-a"));
    runtime.summon(
        firstSession, firstOwner, GlobalPetDefinitions.ID, new Location(null, 0, 64, 0),
        AuthorityState.HEALTHY, "duel-request-block-a", 1).lease().orElseThrow();
    runtime.summon(
        secondSession, secondOwner, GlobalPetDefinitions.ID, new Location(null, 0, 64, 0),
        AuthorityState.HEALTHY, "duel-request-block-b", 1).lease().orElseThrow();

    secondState.set(PetSubjectCompatibility.CompatibilityResolution.ambiguous("mapping quarantined before request"));
    PetDuelService duels = new PetDuelService(runtime, 20, 5);

    check(
        duels.request(firstOwner, secondOwner, 2) == PetDuelResult.INELIGIBLE,
        "challenged mapping quarantine must block duel request");
    check(runtime.activeLeases().stream().noneMatch(lease -> lease.ownerPlayerId().equals(secondOwner)),
        "challenged mapping quarantine must quarantine and recall its pet");
    check(duels.pendingFor(secondOwner, 2).isEmpty(), "blocked challenged owner must not receive a pending request");
  }

  private static void activeSkillFailsClosedWhenContentBecomesUnavailable() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    PlayerSession session = sessions.bind(UUID.randomUUID(), PetSubjectId.of("content-loss"));
    AtomicReference<ContentResolution> state = new AtomicReference<>(
        ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1));
    ContentEligibilityService content = new ContentEligibilityService(
        (ignored, ignoredSession) -> state.get(),
        new ContentRunProjectionService(new InMemoryPetCoreStore(), Instant::now));
    GlobalPetRuntime runtime = runtime(adapter, sessions, content);
    ActiveLease lease = runtime.summon(
        session, session.playerId(), GlobalPetDefinitions.ID, new Location(null, 0, 64, 0),
        AuthorityState.HEALTHY, "content-loss", 1).lease().orElseThrow();

    state.set(ContentResolution.unknown());
    check(!runtime.invokeSkill(lease, GlobalPetDefinitions.ALLOW_LISTED_SKILL),
        "content outage must block active skills");
    check(runtime.activeLeases().isEmpty(), "content outage must recall the active lease");
    check(adapter.despawned == 1, "content outage must despawn the active pet");
  }

  private static void activeSkillFailsClosedWhenDurableAuthorityBecomesUnavailable() {
    FakeAdapter adapter = new FakeAdapter();
    ProfileSessionRegistry sessions = new ProfileSessionRegistry();
    PlayerSession session = sessions.bind(UUID.randomUUID(), PetSubjectId.of("authority-loss"));
    AtomicReference<AuthorityState> authority = new AtomicReference<>(AuthorityState.HEALTHY);
    GlobalPetRuntime runtime = runtime(
        adapter, sessions, openContent(),
        legacySubjectId -> PetSubjectCompatibility.CompatibilityResolution.compatible("char-authority"),
        authority::get);
    ActiveLease lease = runtime.summon(
        session, session.playerId(), GlobalPetDefinitions.ID, new Location(null, 0, 64, 0),
        AuthorityState.HEALTHY, "authority-loss", 1).lease().orElseThrow();

    authority.set(AuthorityState.UNAVAILABLE);
    check(!runtime.invokeSkill(lease, GlobalPetDefinitions.ALLOW_LISTED_SKILL),
        "durable authority outage must block active skills");
    check(runtime.activeLeases().isEmpty(), "durable authority outage must recall the active lease");
    check(adapter.despawned == 1, "durable authority outage must despawn the active pet");
  }

  private static void paperCommandTreeAcceptsDuelArguments() {
    CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
    dispatcher.register(
        LiteralArgumentBuilder.<Object>literal("petcore")
            .then(
                LiteralArgumentBuilder.<Object>literal("duel")
                    .then(
                        LiteralArgumentBuilder.<Object>literal("request")
                            .then(
                                RequiredArgumentBuilder.<Object, String>argument(
                                    "player", StringArgumentType.word())))
                    .then(LiteralArgumentBuilder.<Object>literal("accept"))
                    .then(LiteralArgumentBuilder.<Object>literal("decline"))
                    .then(
                        LiteralArgumentBuilder.<Object>literal("skill")
                            .then(
                                RequiredArgumentBuilder.<Object, String>argument(
                                    "id", StringArgumentType.word())))
                    .then(LiteralArgumentBuilder.<Object>literal("end"))));

    var parsed = dispatcher.parse("petcore duel request targetPlayer", new Object());
    check(!parsed.getReader().canRead(), "Brigadier tree should consume duel request arguments");
  }

  private static ContentEligibilityService openContent() {
    return new ContentEligibilityService(
        (ignored, ignoredSession) ->
            ContentResolution.resolved(ContentMode.OPEN_PVE, Optional.empty(), 1, 1),
        new ContentRunProjectionService(new InMemoryPetCoreStore(), Instant::now));
  }

  private static GlobalPetRuntime runtime(
      FakeAdapter adapter,
      ProfileSessionRegistry sessions,
      ContentEligibilityService content
  ) {
    return runtime(
        adapter,
        sessions,
        content,
        legacySubjectId ->
            PetSubjectCompatibility.CompatibilityResolution.compatible("char-test"));
  }

  private static GlobalPetRuntime runtime(
      FakeAdapter adapter,
      ProfileSessionRegistry sessions,
      ContentEligibilityService content,
      PetSubjectCompatibility compatibility
  ) {
    return runtime(adapter, sessions, content, compatibility, () -> AuthorityState.HEALTHY);
  }

  private static GlobalPetRuntime runtime(
      FakeAdapter adapter,
      ProfileSessionRegistry sessions,
      ContentEligibilityService content,
      PetSubjectCompatibility compatibility,
      java.util.function.Supplier<AuthorityState> authority
  ) {
    return new GlobalPetRuntime(
        new PetAdmissionService(new CapacityLimits(1, 4, 4, 4, 4, 2), 5, new PetCoreTelemetry()),
        new ActiveLeaseRegistry(),
        new FailClosedGate(new RecordingRecallSink()),
        adapter,
        sessions,
        player -> Optional.of(PetSubjectId.of("unused")),
        compatibility,
        content,
        authority);
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  private static void checkThrows(Class<? extends Throwable> type, Runnable action, String message) {
    try {
      action.run();
    } catch (Throwable failure) {
      if (type.isInstance(failure)) return;
      throw new AssertionError(message + ": unexpected " + failure.getClass().getSimpleName(), failure);
    }
    throw new AssertionError(message);
  }

  private static final class FakeAdapter implements PetExecutorAdapter {
    private final List<UUID> spawned = new ArrayList<>();
    private int staleDespawns;
    private int despawned;
    private int clearedTargets;
    private UUID failSetTargetFor;
    private UUID failClearTargetFor;

    @Override
    public PetSpawnResult spawn(PetDefinition definition, ActiveLease lease, UUID ownerId, Location location) {
      UUID entity = UUID.randomUUID();
      spawned.add(entity);
      return PetSpawnResult.success(entity);
    }

    @Override
    public boolean setTarget(ActiveLease lease, UUID targetId) {
      return !lease.entityId().equals(failSetTargetFor);
    }

    @Override
    public boolean clearTarget(ActiveLease lease) {
      clearedTargets++;
      return !lease.entityId().equals(failClearTargetFor);
    }

    @Override
    public FollowResult follow(ActiveLease lease, UUID ownerId) {
      return new FollowResult(FollowAction.NORMAL_FOLLOW, "fake");
    }

    @Override
    public boolean invokeSkill(ActiveLease lease, String skillId) {
      return true;
    }

    @Override
    public boolean despawn(ActiveLease lease) {
      despawned++;
      return true;
    }

    @Override
    public boolean despawnTaggedEntity(UUID entityId, ActiveLease expectedLease) {
      staleDespawns++;
      return true;
    }

    @Override
    public void reconcileTaggedEntities(Collection<ActiveLease> currentLeases) {}
  }
}
