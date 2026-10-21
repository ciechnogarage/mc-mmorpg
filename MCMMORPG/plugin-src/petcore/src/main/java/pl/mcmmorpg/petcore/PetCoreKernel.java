package pl.mcmmorpg.petcore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Policy-free PetCore kernel. It owns identity fencing, durable projections,
 * admission, bounded work, and fail-closed lifecycle state only. It has no
 * MythicMobs calls and intentionally contains no summon/spawn gameplay.
 */
final class PetCoreKernel {
  private PetCoreKernel() {}
}

record PetSubjectId(String value) {
  static PetSubjectId of(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("PetSubjectId is required");
    return new PetSubjectId(value);
  }
}

record PlayerSession(UUID playerId, PetSubjectId subjectId, long epoch) {}

final class ProfileSessionRegistry {
  private final Map<UUID, PlayerSession> current = new ConcurrentHashMap<>();
  private final Map<UUID, Long> nextEpoch = new ConcurrentHashMap<>();

  PlayerSession bind(UUID playerId, PetSubjectId subjectId) {
    long epoch = nextEpoch.merge(playerId, 1L, Long::sum);
    PlayerSession session = new PlayerSession(playerId, subjectId, epoch);
    current.put(playerId, session);
    return session;
  }

  boolean isCurrent(PlayerSession session) {
    return session.equals(current.get(session.playerId()));
  }

  Optional<PlayerSession> current(UUID playerId) {
    return Optional.ofNullable(current.get(playerId));
  }

  void unbind(PlayerSession session) {
    current.remove(session.playerId(), session);
  }
}

record PetProfileProjection(PetSubjectId subjectId, Instant createdAt) {}
record ContentRunId(String value) {
  static ContentRunId of(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("ContentRunId is required");
    return new ContentRunId(value);
  }
}
record ContentRunProjection(ContentRunId contentRunId, String instanceId, boolean socialRun,
                            Optional<Instant> firstSocialAt, Instant expiresAt, long revision) {}

sealed interface RunProjectionEvent permits PartyChanged, RunExited, RunReset {
  ContentRunId runId();
  long revision();
  String operationId();
}
record PartyChanged(ContentRunId runId, String instanceId, int partySize, long revision,
                    String operationId) implements RunProjectionEvent {}
record RunExited(ContentRunId runId, long revision, String operationId) implements RunProjectionEvent {}
record RunReset(ContentRunId runId, long revision, String operationId) implements RunProjectionEvent {}
record RunApplyResult(Optional<ContentRunProjection> projection, boolean applied) {}

interface PetCoreStore {
  PetProfileProjection ensureProfile(PetSubjectId subjectId, String operationId);
  Optional<ContentRunProjection> findRun(ContentRunId runId);
  boolean deleteExpiredRun(ContentRunId runId, Instant now);
  RunApplyResult applyRunEvent(RunProjectionEvent event, Instant now);
}

/** In-memory test double. Production uses JdbcPetCoreStore. */
final class InMemoryPetCoreStore implements PetCoreStore {
  private final Map<PetSubjectId, PetProfileProjection> profiles = new ConcurrentHashMap<>();
  private final Map<ContentRunId, ContentRunProjection> runs = new ConcurrentHashMap<>();
  private final Map<String, RunApplyResult> appliedRunOperations = new ConcurrentHashMap<>();
  private final Map<String, PetProfileProjection> appliedProfileOperations = new ConcurrentHashMap<>();
  private final Map<PetSubjectId, ReentrantLock> profileLocks = new ConcurrentHashMap<>();
  private final Map<ContentRunId, ReentrantLock> runLocks = new ConcurrentHashMap<>();

  @Override public PetProfileProjection ensureProfile(PetSubjectId subjectId, String operationId) {
    PetProfileProjection prior = appliedProfileOperations.get(operationId);
    if (prior != null) return prior;
    ReentrantLock lock = profileLocks.computeIfAbsent(subjectId, ignored -> new ReentrantLock());
    lock.lock();
    try {
      PetProfileProjection projection = profiles.computeIfAbsent(subjectId,
          key -> new PetProfileProjection(key, Instant.now()));
      return appliedProfileOperations.computeIfAbsent(operationId, ignored -> projection);
    } finally {
      lock.unlock();
    }
  }

  int profileCount() { return profiles.size(); }
  @Override public Optional<ContentRunProjection> findRun(ContentRunId runId) { return Optional.ofNullable(runs.get(runId)); }
  @Override public boolean deleteExpiredRun(ContentRunId runId, Instant now) {
    ReentrantLock lock = runLocks.computeIfAbsent(runId, ignored -> new ReentrantLock());
    lock.lock();
    try {
      ContentRunProjection current = runs.get(runId);
      if (current == null || current.expiresAt().isAfter(now)) return false;
      runs.remove(runId);
      return true;
    } finally {
      lock.unlock();
    }
  }

  @Override public RunApplyResult applyRunEvent(RunProjectionEvent event, Instant now) {
    RunApplyResult prior = appliedRunOperations.get(event.operationId());
    if (prior != null) return prior;
    ReentrantLock lock = runLocks.computeIfAbsent(event.runId(), ignored -> new ReentrantLock());
    lock.lock();
    try {
      RunApplyResult result = applyLocked(event, now, runs.get(event.runId()));
      return appliedRunOperations.computeIfAbsent(event.operationId(), ignored -> result);
    } finally {
      lock.unlock();
    }
  }

  private RunApplyResult applyLocked(RunProjectionEvent event, Instant now, ContentRunProjection current) {
    if (current != null && event.revision() <= current.revision()) return new RunApplyResult(Optional.of(current), false);
    if (event instanceof PartyChanged party) {
      boolean social = (current != null && current.socialRun()) || party.partySize() >= 2;
      Optional<Instant> firstSocialAt = social
          ? current != null && current.firstSocialAt().isPresent() ? current.firstSocialAt() : Optional.of(now)
          : Optional.empty();
      ContentRunProjection updated = new ContentRunProjection(party.runId(), party.instanceId(), social,
          firstSocialAt, now.plusSeconds(86_400), party.revision());
      runs.put(party.runId(), updated);
      return new RunApplyResult(Optional.of(updated), true);
    }
    if (current == null) return new RunApplyResult(Optional.empty(), false);
    runs.remove(event.runId());
    return new RunApplyResult(Optional.empty(), true);
  }
}

/**
 * MariaDB-backed durable store. Every mutation is profile/run locked and carries
 * an idempotency key; failures are surfaced to the caller so the runtime closes.
 */
final class JdbcPetCoreStore implements PetCoreStore {
  interface ConnectionProvider { Connection open() throws SQLException; }
  private final ConnectionProvider connections;
  private final String tablePrefix;

  JdbcPetCoreStore(String jdbcUrl, String user, String password, String tablePrefix) {
    this(() -> openMariaDb(jdbcUrl, user, password), tablePrefix);
  }

  private static Connection openMariaDb(String jdbcUrl, String user, String password) throws SQLException {
    try {
      Class.forName("org.mariadb.jdbc.Driver", true, JdbcPetCoreStore.class.getClassLoader());
    } catch (ClassNotFoundException exception) {
      throw new SQLException("MariaDB JDBC driver is unavailable to PetCore", exception);
    }
    return DriverManager.getConnection(jdbcUrl, user, password);
  }

  JdbcPetCoreStore(ConnectionProvider connections, String tablePrefix) {
    if (!tablePrefix.matches("[a-z0-9_]+")) throw new IllegalArgumentException("unsafe table prefix");
    this.connections = connections;
    this.tablePrefix = tablePrefix;
    initialize();
  }

  private void initialize() {
    try (Connection connection = connections.open(); Statement statement = connection.createStatement()) {
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablePrefix + "profiles ("
          + "pet_subject_id VARCHAR(191) PRIMARY KEY, created_at TIMESTAMP(6) NOT NULL)");
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablePrefix + "content_runs ("
          + "content_run_id VARCHAR(191) PRIMARY KEY, instance_id VARCHAR(191) NOT NULL, social_run BOOLEAN NOT NULL, "
          + "first_social_at TIMESTAMP(6) NULL, expires_at TIMESTAMP(6) NOT NULL, revision BIGINT NOT NULL)");
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablePrefix + "idempotency ("
          + "operation_id VARCHAR(191) PRIMARY KEY, operation_kind VARCHAR(32) NOT NULL, created_at TIMESTAMP(6) NOT NULL)");
    } catch (SQLException exception) {
      throw new DurableAuthorityException("PetCore MariaDB schema initialization failed", exception);
    }
  }

  @Override public PetProfileProjection ensureProfile(PetSubjectId subjectId, String operationId) {
    try (Connection connection = connections.open()) {
      connection.setAutoCommit(false);
      try {
        if (!claimOperation(connection, operationId, "PROFILE")) {
          PetProfileProjection existing = selectProfileForUpdate(connection, subjectId);
          connection.commit();
          return existing;
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + tablePrefix
            + "profiles (pet_subject_id, created_at) VALUES (?, CURRENT_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE pet_subject_id=VALUES(pet_subject_id)")) {
          insert.setString(1, subjectId.value());
          insert.executeUpdate();
        }
        PetProfileProjection projection = selectProfileForUpdate(connection, subjectId);
        connection.commit();
        return projection;
      } catch (SQLException exception) { rollback(connection); throw exception; }
    } catch (SQLException exception) {
      throw new DurableAuthorityException("PetCore profile transaction failed", exception);
    }
  }

  @Override public Optional<ContentRunProjection> findRun(ContentRunId runId) {
    try (Connection connection = connections.open()) { return selectRun(connection, runId, false); }
    catch (SQLException exception) { throw new DurableAuthorityException("PetCore run read failed", exception); }
  }

  @Override public boolean deleteExpiredRun(ContentRunId runId, Instant now) {
    try (Connection connection = connections.open(); PreparedStatement delete = connection.prepareStatement("DELETE FROM "
        + tablePrefix + "content_runs WHERE content_run_id=? AND expires_at<=?")) {
      delete.setString(1, runId.value());
      delete.setTimestamp(2, java.sql.Timestamp.from(now));
      return delete.executeUpdate() == 1;
    } catch (SQLException exception) {
      throw new DurableAuthorityException("PetCore expired run cleanup failed", exception);
    }
  }

  @Override public RunApplyResult applyRunEvent(RunProjectionEvent event, Instant now) {
    try (Connection connection = connections.open()) {
      connection.setAutoCommit(false);
      try {
        if (!claimOperation(connection, event.operationId(), "RUN")) {
          RunApplyResult repeated = new RunApplyResult(selectRun(connection, event.runId(), true), false);
          connection.commit();
          return repeated;
        }
        Optional<ContentRunProjection> current = selectRun(connection, event.runId(), true);
        if (current.isPresent() && event.revision() <= current.get().revision()) {
          connection.commit();
          return new RunApplyResult(current, false);
        }
        if (event instanceof PartyChanged party) {
          boolean social = current.map(ContentRunProjection::socialRun).orElse(false) || party.partySize() >= 2;
          Instant firstSocial = current.flatMap(ContentRunProjection::firstSocialAt).orElse(social ? now : null);
          try (PreparedStatement upsert = connection.prepareStatement("INSERT INTO " + tablePrefix + "content_runs "
              + "(content_run_id, instance_id, social_run, first_social_at, expires_at, revision) VALUES (?, ?, ?, ?, ?, ?) "
              + "ON DUPLICATE KEY UPDATE instance_id=VALUES(instance_id), social_run=VALUES(social_run), first_social_at=VALUES(first_social_at), expires_at=VALUES(expires_at), revision=VALUES(revision)")) {
            upsert.setString(1, party.runId().value()); upsert.setString(2, party.instanceId()); upsert.setBoolean(3, social);
            if (firstSocial == null) upsert.setNull(4, java.sql.Types.TIMESTAMP); else upsert.setTimestamp(4, java.sql.Timestamp.from(firstSocial));
            upsert.setTimestamp(5, java.sql.Timestamp.from(now.plusSeconds(86_400))); upsert.setLong(6, party.revision()); upsert.executeUpdate();
          }
          Optional<ContentRunProjection> updated = selectRun(connection, party.runId(), true);
          connection.commit(); return new RunApplyResult(updated, true);
        }
        if (current.isEmpty()) { connection.commit(); return new RunApplyResult(Optional.empty(), false); }
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + tablePrefix + "content_runs WHERE content_run_id=?")) {
          delete.setString(1, event.runId().value()); delete.executeUpdate();
        }
        connection.commit(); return new RunApplyResult(Optional.empty(), true);
      } catch (SQLException exception) { rollback(connection); throw exception; }
    } catch (SQLException exception) {
      throw new DurableAuthorityException("PetCore run projection transaction failed", exception);
    }
  }

  private boolean claimOperation(Connection connection, String operationId, String kind) throws SQLException {
    try (PreparedStatement insert = connection.prepareStatement("INSERT IGNORE INTO " + tablePrefix
        + "idempotency (operation_id, operation_kind, created_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))")) {
      insert.setString(1, operationId); insert.setString(2, kind); return insert.executeUpdate() == 1;
    }
  }

  private PetProfileProjection selectProfileForUpdate(Connection connection, PetSubjectId id) throws SQLException {
    try (PreparedStatement select = connection.prepareStatement("SELECT pet_subject_id, created_at FROM " + tablePrefix
        + "profiles WHERE pet_subject_id=? FOR UPDATE")) {
      select.setString(1, id.value()); try (ResultSet rows = select.executeQuery()) {
        if (!rows.next()) throw new SQLException("missing profile after idempotent insert");
        return new PetProfileProjection(PetSubjectId.of(rows.getString(1)), rows.getTimestamp(2).toInstant());
      }
    }
  }

  private Optional<ContentRunProjection> selectRun(Connection connection, ContentRunId runId, boolean forUpdate) throws SQLException {
    String suffix = forUpdate ? " FOR UPDATE" : "";
    try (PreparedStatement select = connection.prepareStatement("SELECT instance_id, social_run, first_social_at, expires_at, revision FROM "
        + tablePrefix + "content_runs WHERE content_run_id=?" + suffix)) {
      select.setString(1, runId.value()); try (ResultSet rows = select.executeQuery()) {
        if (!rows.next()) return Optional.empty();
        java.sql.Timestamp first = rows.getTimestamp(3);
        return Optional.of(new ContentRunProjection(runId, rows.getString(1), rows.getBoolean(2),
            Optional.ofNullable(first).map(java.sql.Timestamp::toInstant), rows.getTimestamp(4).toInstant(), rows.getLong(5)));
      }
    }
  }

  private static void rollback(Connection connection) { try { connection.rollback(); } catch (SQLException ignored) {} }
}

final class DurableAuthorityException extends RuntimeException {
  DurableAuthorityException(String message, Throwable cause) { super(message, cause); }
}

final class ContentRunProjectionService {
  private final PetCoreStore store;
  private final Supplier<Instant> clock;
  ContentRunProjectionService(PetCoreStore store, Supplier<Instant> clock) { this.store = store; this.clock = clock; }
  Optional<ContentRunProjection> find(ContentRunId runId) {
    try {
      if (store == null || runId == null) return Optional.empty();
      Optional<ContentRunProjection> projection = store.findRun(runId);
      return projection == null ? Optional.empty() : projection;
    } catch (RuntimeException unavailable) {
      return Optional.empty();
    }
  }
  Optional<ContentRunProjection> findCurrent(ContentRunId runId) {
    try {
      if (clock == null) return Optional.empty();
      Instant now = clock.get();
      Optional<ContentRunProjection> projection = find(runId);
      if (projection.isPresent() && (projection.get().expiresAt() == null
          || !projection.get().expiresAt().isAfter(now))) {
        try {
          if (store != null) store.deleteExpiredRun(runId, now);
        } catch (RuntimeException ignored) {
          // The projection is unavailable either way; callers must remain blocked.
        }
        return Optional.empty();
      }
      return projection;
    } catch (RuntimeException unavailable) {
      return Optional.empty();
    }
  }
  void partyChanged(ContentRunId run, String instance, int partySize, long revision, String operationId) {
    store.applyRunEvent(new PartyChanged(run, instance, partySize, revision, operationId), clock.get());
  }
  boolean runExited(ContentRunId run, long revision, String operationId) {
    return store.applyRunEvent(new RunExited(run, revision, operationId), clock.get()).applied();
  }
  boolean runReset(ContentRunId run, long revision, String operationId) {
    return store.applyRunEvent(new RunReset(run, revision, operationId), clock.get()).applied();
  }
}

enum ContentMode {
  SAFE, OPEN_PVE, INSTANCE_SOLO, INSTANCE_PARTY, DUEL, OPEN_PVP, ARENA, RANKED;
  boolean isInstanced() { return this == INSTANCE_SOLO || this == INSTANCE_PARTY; }
}
record ContentResolution(boolean known, ContentMode mode, Optional<ContentRunId> contentRunId, int partySize, long revision) {
  static ContentResolution unknown() { return new ContentResolution(false, ContentMode.SAFE, Optional.empty(), 0, -1); }
  static ContentResolution resolved(ContentMode mode, Optional<ContentRunId> run, int partySize, long revision) {
    return new ContentResolution(true, mode, run, partySize, revision);
  }
}
interface ContentEligibilityProvider { ContentResolution resolve(PetSubjectId subject, PlayerSession session); }

/** Provider-facing seam: callers send only authoritative lifecycle events. */
final class ContentEligibilityService {
  private final ContentEligibilityProvider provider;
  private final ContentRunProjectionService runs;
  ContentEligibilityService(ContentEligibilityProvider provider, ContentRunProjectionService runs) { this.provider = provider; this.runs = runs; }
  ContentResolution resolve(PetSubjectId subject, PlayerSession session) {
    if (provider == null || runs == null || subject == null || session == null) return ContentResolution.unknown();
    try {
      ContentResolution resolution = provider.resolve(subject, session);
      if (resolution == null) return ContentResolution.unknown();
      if (!resolution.known()) return resolution;
      if (resolution.mode() == null || resolution.contentRunId() == null) return ContentResolution.unknown();
      boolean instanced = resolution.mode().isInstanced() || resolution.contentRunId().isPresent();
      if (!instanced) return resolution;
      Optional<ContentRunProjection> projection = resolution.contentRunId().flatMap(runs::findCurrent);
      if (projection == null || projection.isEmpty() || projection.get().revision() != resolution.revision()) {
        return ContentResolution.unknown();
      }
      return resolution;
    } catch (RuntimeException unavailable) {
      return ContentResolution.unknown();
    }
  }
  boolean adventurerAllowed(PetSubjectId subject, PlayerSession session) {
    try {
      ContentResolution resolution = resolve(subject, session);
      if (!resolution.known() || resolution.mode() == null || resolution.contentRunId() == null) return false;
      if (!resolution.mode().isInstanced()) return resolution.partySize() < 2;
      return resolution.contentRunId().flatMap(runs::findCurrent).map(state -> !state.socialRun()).orElse(false);
    } catch (RuntimeException unavailable) {
      return false;
    }
  }
  void partyChanged(ContentRunId run, String instance, int size, long revision, String id) { runs.partyChanged(run, instance, size, revision, id); }
  boolean runExited(ContentRunId run, long revision, String id) { return runs.runExited(run, revision, id); }
  boolean runReset(ContentRunId run, long revision, String id) { return runs.runReset(run, revision, id); }
}

enum CompanionPolicy { GLOBAL_PET, BEAST_COMPANION, SUMMONED_MINION, ADVENTURER_COMPANION }
enum AdmissionRejection { OWNER_CAP, POLICY_CAP, WORLD_CAP, DUNGEON_CAP, GLOBAL_HEADROOM, LIFECYCLE_RESERVATION_CAP, CONTENT_RESTRICTION }
record CapacityLimits(int ownerPolicy, int policy, int world, int instance, int globalActive, int lifecycleReservations) {
  CapacityLimits { if (ownerPolicy < 1 || policy < 1 || world < 1 || instance < 1 || globalActive < 1 || lifecycleReservations < 1) throw new IllegalArgumentException("capacity must be positive"); }
}
record AdmissionContext(PetSubjectId owner, CompanionPolicy policy, String world, Optional<String> instanceId) {}
record AdmissionDecision(boolean accepted, Optional<UUID> reservationId, Optional<AdmissionRejection> rejection) {
  static AdmissionDecision accepted(UUID id) { return new AdmissionDecision(true, Optional.of(id), Optional.empty()); }
  static AdmissionDecision rejected(AdmissionRejection reason) { return new AdmissionDecision(false, Optional.empty(), Optional.of(reason)); }
}
record Reservation(UUID id, AdmissionContext context, String requestId) {}

final class PetAdmissionService {
  private final CapacityLimits limits; private final PetCoreTelemetry telemetry;
  private final long reservationTtlTicks;
  private final Map<UUID, TimedReservation> reservations = new HashMap<>();
  private final Map<UUID, AdmissionContext> active = new HashMap<>();
  private final Map<String, AdmissionDecision> requests = new HashMap<>();
  PetAdmissionService(CapacityLimits limits, long reservationTtlTicks, PetCoreTelemetry telemetry) {
    if (reservationTtlTicks < 1) throw new IllegalArgumentException("reservation TTL must be positive");
    this.limits = limits; this.reservationTtlTicks = reservationTtlTicks; this.telemetry = telemetry;
  }
  synchronized AdmissionDecision reserve(AdmissionContext request, String requestId, long nowTick) {
    cleanupExpired(nowTick);
    AdmissionDecision repeated = requests.get(requestId); if (repeated != null) return repeated;
    AdmissionRejection reason = rejection(request);
    AdmissionDecision decision = reason == null ? AdmissionDecision.accepted(UUID.randomUUID()) : AdmissionDecision.rejected(reason);
    requests.put(requestId, decision);
    if (decision.accepted()) reservations.put(decision.reservationId().orElseThrow(), new TimedReservation(request, nowTick + reservationTtlTicks)); else telemetry.recordAdmissionRejection(reason);
    return decision;
  }
  synchronized boolean commit(UUID id, long nowTick) { cleanupExpired(nowTick); TimedReservation reservation = reservations.remove(id); if (reservation == null) return false; active.put(id, reservation.context()); return true; }
  synchronized boolean release(UUID id) { return reservations.remove(id) != null || active.remove(id) != null; }
  synchronized int activeAndReservedCount() { return active.size() + reservations.size(); }
  synchronized int cleanupExpired(long nowTick) {
    int before = reservations.size();
    reservations.entrySet().removeIf(entry -> entry.getValue().expiresAtTick() <= nowTick);
    int expired = before - reservations.size();
    if (expired > 0) telemetry.recordReservationExpirations(expired);
    return expired;
  }
  private AdmissionRejection rejection(AdmissionContext request) {
    if (countOwnerPolicy(request) >= limits.ownerPolicy()) return AdmissionRejection.OWNER_CAP;
    if (countPolicy(request.policy()) >= limits.policy()) return AdmissionRejection.POLICY_CAP;
    if (countWorld(request.world()) >= limits.world()) return AdmissionRejection.WORLD_CAP;
    if (request.instanceId().isPresent() && countInstance(request.instanceId().get()) >= limits.instance()) return AdmissionRejection.DUNGEON_CAP;
    if (active.size() + reservations.size() >= limits.globalActive()) return AdmissionRejection.GLOBAL_HEADROOM;
    if (reservations.size() >= limits.lifecycleReservations()) return AdmissionRejection.LIFECYCLE_RESERVATION_CAP;
    return null;
  }
  private int countOwnerPolicy(AdmissionContext c) { return (int) all().filter(x -> x.owner().equals(c.owner()) && x.policy() == c.policy()).count(); }
  private int countPolicy(CompanionPolicy p) { return (int) all().filter(x -> x.policy() == p).count(); }
  private int countWorld(String world) { return (int) all().filter(x -> x.world().equals(world)).count(); }
  private int countInstance(String instance) { return (int) all().filter(x -> x.instanceId().filter(instance::equals).isPresent()).count(); }
  private java.util.stream.Stream<AdmissionContext> all() { return java.util.stream.Stream.concat(reservations.values().stream().map(TimedReservation::context), active.values().stream()); }
}
record TimedReservation(AdmissionContext context, long expiresAtTick) {}

enum WorkPriority { LIFECYCLE, ADMISSION, COMBAT_VALIDITY, FOLLOW_REPATH, TARGET_SCAN, IDLE }
record ScheduledWork(String id, long enqueuedAtTick) {}
final class BoundedWorkQueue {
  private final WorkPriority priority; private final int maxSize; private final long maxAgeTicks; private final PetCoreTelemetry telemetry; private final ArrayDeque<ScheduledWork> queue = new ArrayDeque<>();
  BoundedWorkQueue(WorkPriority priority, int maxSize, long maxAgeTicks, PetCoreTelemetry telemetry) { this.priority = priority; this.maxSize = maxSize; this.maxAgeTicks = maxAgeTicks; this.telemetry = telemetry; }
  synchronized boolean offer(ScheduledWork work) { if (queue.size() >= maxSize) { telemetry.recordQueueDrop(priority); return false; } queue.addLast(work); telemetry.recordQueueDepth(priority, queue.size()); return true; }
  synchronized Optional<ScheduledWork> poll(long nowTick) { while (!queue.isEmpty() && nowTick - queue.peekFirst().enqueuedAtTick() > maxAgeTicks) { queue.removeFirst(); telemetry.recordQueueDrop(priority); } telemetry.recordQueueDepth(priority, queue.size()); return Optional.ofNullable(queue.pollFirst()); }
}

final class PetCoreTelemetry {
  private final Map<AdmissionRejection, Long> admissionRejects = new EnumMap<>(AdmissionRejection.class);
  private final Map<WorkPriority, Long> queueDrops = new EnumMap<>(WorkPriority.class);
  private final Map<WorkPriority, Integer> queueDepths = new EnumMap<>(WorkPriority.class);
  private long reservationExpirations;
  synchronized void recordAdmissionRejection(AdmissionRejection reason) { admissionRejects.merge(reason, 1L, Long::sum); }
  synchronized long admissionRejects(AdmissionRejection reason) { return admissionRejects.getOrDefault(reason, 0L); }
  synchronized void recordQueueDrop(WorkPriority priority) { queueDrops.merge(priority, 1L, Long::sum); }
  synchronized long queueDrops(WorkPriority priority) { return queueDrops.getOrDefault(priority, 0L); }
  synchronized void recordQueueDepth(WorkPriority priority, int depth) { queueDepths.put(priority, depth); }
  synchronized int queueDepth(WorkPriority priority) { return queueDepths.getOrDefault(priority, 0); }
  synchronized void recordReservationExpirations(int count) { reservationExpirations += count; }
  synchronized long reservationExpirations() { return reservationExpirations; }
}

record ActiveLease(UUID petInstanceId, long runtimeEpoch, PetSubjectId subjectId, UUID ownerPlayerId, UUID entityId) {
  ActiveLease(UUID petInstanceId, long runtimeEpoch, PetSubjectId subjectId, UUID ownerPlayerId) {
    this(petInstanceId, runtimeEpoch, subjectId, ownerPlayerId, null);
  }
}
final class ActiveLeaseRegistry {
  private final Map<UUID, ActiveLease> current = new ConcurrentHashMap<>();
  void activate(ActiveLease lease) { current.compute(lease.petInstanceId(), (ignored, existing) ->
      existing == null || lease.runtimeEpoch() > existing.runtimeEpoch() ? lease : existing); }
  boolean isCurrent(ActiveLease lease) { return lease.equals(current.get(lease.petInstanceId())); }
  boolean release(ActiveLease lease) { return current.remove(lease.petInstanceId(), lease); }
  Optional<ActiveLease> currentForEntity(UUID entityId) {
    if (entityId == null) return Optional.empty();
    return current.values().stream().filter(lease -> entityId.equals(lease.entityId())).findFirst();
  }
  Collection<ActiveLease> snapshot() { return java.util.List.copyOf(current.values()); }
}
enum AuthorityState { HEALTHY, UNAVAILABLE }
interface RecallSink { void recall(ActiveLease lease, String status); }
final class RecordingRecallSink implements RecallSink {
  final java.util.List<ActiveLease> recalled = new java.util.ArrayList<>(); String lastStatus = "";
  @Override public void recall(ActiveLease lease, String status) { recalled.add(lease); lastStatus = status; }
}
final class FailClosedGate {
  static final String RESTING_STATUS = "companion resting while state is verified";
  private final RecallSink recalls; private final Map<ActiveLease, Long> fenced = new ConcurrentHashMap<>();
  FailClosedGate(RecallSink recalls) { this.recalls = recalls; }
  boolean allowNewAction(AuthorityState authority, ContentResolution content) { return authority == AuthorityState.HEALTHY && content.known(); }
  boolean allowAutonomousAbility(ActiveLease lease) { return !fenced.containsKey(lease); }
  void fence(ActiveLease lease, long currentTick, String reason) { fenced.merge(lease, currentTick + 5, Math::min); }
  void onTick(long currentTick) { fenced.entrySet().removeIf(entry -> { if (entry.getValue() > currentTick) return false; recalls.recall(entry.getKey(), RESTING_STATUS); return true; }); }
}

/** Scheduler seam used by the Paper shell. All failure transitions share the five-tick gate. */
final class PetCoreRuntime {
  private final FailClosedGate gate;
  PetCoreRuntime(FailClosedGate gate) { this.gate = gate; }
  boolean admitAction(AuthorityState authority, ContentResolution content) { return gate.allowNewAction(authority, content); }
  void authorityFailure(ActiveLease lease, long tick, String reason) { gate.fence(lease, tick, reason); }
  void tick(long tick) { gate.onTick(tick); }
}
