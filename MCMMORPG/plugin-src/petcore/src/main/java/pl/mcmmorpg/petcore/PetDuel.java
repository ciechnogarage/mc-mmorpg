package pl.mcmmorpg.petcore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Real-time, consensual duel state. No player damage, rewards, or stakes. */
record PetDuelRequest(UUID requestId, UUID challengerOwnerId, UUID challengedOwnerId, long expiresAtTick) {}

record PetDuelSession(
    UUID duelId,
    ActiveLease first,
    ActiveLease second,
    long startedAtTick,
    long nextSkillTick,
    long nextAutoSkillTick) {
  PetDuelSession withNextSkill(long tick) {
    return new PetDuelSession(duelId, first, second, startedAtTick, tick, nextAutoSkillTick);
  }

  PetDuelSession withNextAutoSkill(long tick) {
    return new PetDuelSession(duelId, first, second, startedAtTick, nextSkillTick, tick);
  }

  boolean containsOwner(UUID owner) {
    return first.ownerPlayerId().equals(owner) || second.ownerPlayerId().equals(owner);
  }

  boolean containsEntity(UUID entity) {
    return first.entityId().equals(entity) || second.entityId().equals(entity);
  }

  ActiveLease otherEntity(UUID entity) {
    return first.entityId().equals(entity) ? second : first;
  }
}

enum PetDuelResult {
  ACCEPTED,
  DECLINED,
  NOT_FOUND,
  INELIGIBLE,
  ALREADY_IN_DUEL,
  EXPIRED,
  INVALID
}

final class PetDuelService {
  private final GlobalPetRuntime pets;
  private final Map<UUID, PetDuelRequest> requests = new ConcurrentHashMap<>();
  final Map<UUID, PetDuelSession> sessions = new ConcurrentHashMap<>();
  private final long requestTtlTicks;
  private final long skillCooldownTicks;

  PetDuelService(GlobalPetRuntime pets, long requestTtlTicks, long skillCooldownTicks) {
    this.pets = pets;
    this.requestTtlTicks = requestTtlTicks;
    this.skillCooldownTicks = skillCooldownTicks;
  }

  PetDuelResult request(UUID challenger, UUID challenged, long tick) {
    if (challenger == null || challenged == null || challenger.equals(challenged)) return PetDuelResult.INVALID;
    if (sessionFor(challenger).isPresent() || sessionFor(challenged).isPresent()) return PetDuelResult.ALREADY_IN_DUEL;
    ActiveLease challengerLease = leaseFor(challenger).orElse(null);
    ActiveLease challengedLease = leaseFor(challenged).orElse(null);
    if (challengerLease == null || challengedLease == null) return PetDuelResult.INELIGIBLE;
    if (!pets.guardDuelRequest(challengerLease) || !pets.guardDuelRequest(challengedLease)) {
      return PetDuelResult.INELIGIBLE;
    }
    requests.values().removeIf(r -> r.expiresAtTick() <= tick);
    UUID requestId = UUID.randomUUID();
    requests.put(requestId, new PetDuelRequest(requestId, challenger, challenged, tick + requestTtlTicks));
    return PetDuelResult.ACCEPTED;
  }

  Optional<PetDuelRequest> pendingFor(UUID challenged, long tick) {
    requests.values().removeIf(r -> r.expiresAtTick() <= tick);
    return requests.values().stream().filter(r -> r.challengedOwnerId().equals(challenged)).findFirst();
  }

  synchronized PetDuelResult accept(UUID challenged, UUID requestId, long tick) {
    PetDuelRequest request = requests.get(requestId);
    if (request == null || !request.challengedOwnerId().equals(challenged)) return PetDuelResult.NOT_FOUND;
    if (request.expiresAtTick() <= tick) {
      requests.remove(requestId);
      return PetDuelResult.EXPIRED;
    }
    ActiveLease first = leaseFor(request.challengerOwnerId()).orElse(null);
    ActiveLease second = leaseFor(request.challengedOwnerId()).orElse(null);
    if (first == null || second == null) return PetDuelResult.INELIGIBLE;
    if (sessionFor(request.challengerOwnerId()).isPresent()
        || sessionFor(request.challengedOwnerId()).isPresent()) {
      requests.remove(requestId);
      return PetDuelResult.ALREADY_IN_DUEL;
    }
    requests.remove(requestId);
    PetDuelSession duel = new PetDuelSession(
        UUID.randomUUID(),
        first,
        second,
        tick,
        tick,
        tick + (skillCooldownTicks * 3));
    boolean firstTargeted = pets.setDuelTarget(first, second.entityId());
    if (!firstTargeted) {
      forceCleanup(first, second, "duel target acquisition failed");
      return PetDuelResult.INELIGIBLE;
    }
    boolean secondTargeted = pets.setDuelTarget(second, first.entityId());
    if (!secondTargeted) {
      forceCleanup(first, second, "duel target acquisition failed");
      return PetDuelResult.INELIGIBLE;
    }
    sessions.put(duel.duelId(), duel);
    return PetDuelResult.ACCEPTED;
  }

  PetDuelResult decline(UUID challenged, UUID requestId) {
    PetDuelRequest request = requests.get(requestId);
    if (request == null || !request.challengedOwnerId().equals(challenged)) return PetDuelResult.NOT_FOUND;
    requests.remove(requestId);
    return PetDuelResult.DECLINED;
  }

  boolean canDamage(UUID sourceEntity, UUID targetEntity) {
    return sessions.values().stream()
        .anyMatch(s -> s.containsEntity(sourceEntity) && s.otherEntity(sourceEntity).entityId().equals(targetEntity));
  }

  boolean isDuelPet(UUID entity) {
    return sessions.values().stream().anyMatch(s -> s.containsEntity(entity));
  }

  boolean isOwnerInDuel(UUID owner) {
    return sessionFor(owner).isPresent();
  }

  boolean commandSkill(UUID owner, String skillId, long tick) {
    for (PetDuelSession duel : sessions.values()) {
      if (!duel.containsOwner(owner)) continue;
      if (tick < duel.nextSkillTick()) return false;
      ActiveLease own = duel.first().ownerPlayerId().equals(owner) ? duel.first() : duel.second();
      if (!pets.invokeDuelSkill(own, skillId)) return false;
      sessions.put(duel.duelId(), duel.withNextSkill(tick + skillCooldownTicks));
      return true;
    }
    return false;
  }

  void tick(long tick) {
    requests.values().removeIf(r -> r.expiresAtTick() <= tick);
    for (PetDuelSession duel : sessions.values()) {
      if (!pets.activeLeases().contains(duel.first()) || !pets.activeLeases().contains(duel.second())) {
        end(duel, "pet unavailable");
        continue;
      }
      boolean firstTargeted = pets.setDuelTarget(duel.first(), duel.second().entityId());
      boolean secondTargeted = pets.setDuelTarget(duel.second(), duel.first().entityId());
      if (!firstTargeted || !secondTargeted) {
        end(duel, "duel target refresh failed");
        continue;
      }
      if (tick >= duel.nextAutoSkillTick()) {
        pets.invokeDuelSkill(duel.first(), GlobalPetDefinitions.ALLOW_LISTED_SKILL);
        pets.invokeDuelSkill(duel.second(), GlobalPetDefinitions.ALLOW_LISTED_SKILL);
        sessions.put(duel.duelId(), duel.withNextSkill(tick + skillCooldownTicks)
            .withNextAutoSkill(tick + (skillCooldownTicks * 3)));
      }
    }
  }

  void endForOwner(UUID owner, String reason) {
    sessions.values().stream().filter(s -> s.containsOwner(owner)).forEach(s -> end(s, reason));
  }

  void endForPetEntity(UUID entity, String reason) {
    sessions.values().stream().filter(s -> s.containsEntity(entity)).forEach(s -> end(s, reason));
  }

  void end(PetDuelSession duel, String reason) {
    sessions.remove(duel.duelId());
    forceCleanup(duel.first(), duel.second(), reason);
  }

  private void forceCleanup(ActiveLease first, ActiveLease second, String reason) {
    boolean firstCleared = pets.clearDuelTarget(first);
    boolean secondCleared = pets.clearDuelTarget(second);
    if (!firstCleared || !secondCleared) {
      pets.recall(first, reason + ": force-safe cleanup");
      pets.recall(second, reason + ": force-safe cleanup");
    }
  }

  Optional<PetDuelSession> sessionFor(UUID owner) {
    return sessions.values().stream().filter(s -> s.containsOwner(owner)).findFirst();
  }

  Collection<PetDuelSession> sessions() {
    return List.copyOf(sessions.values());
  }

  private Optional<ActiveLease> leaseFor(UUID owner) {
    return pets.activeLeases().stream().filter(l -> l.ownerPlayerId().equals(owner)).findFirst();
  }
}
