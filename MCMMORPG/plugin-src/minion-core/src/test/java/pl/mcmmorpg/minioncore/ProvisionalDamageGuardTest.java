package pl.mcmmorpg.minioncore;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

/** Regression: an indexed SPAWNING PDC source must be cancelled before it can deal damage. */
public final class ProvisionalDamageGuardTest {
  private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

  public static void main(String[] args) {
    cancelsDirectDamageFromAnIndexedProvisionalSource();
    System.out.println("PROVISIONAL_DAMAGE_GUARD_TEST_PASS");
  }

  private static void cancelsDirectDamageFromAnIndexedProvisionalSource() {
    UUID entityId = UUID.randomUUID();
    NamespacedKey ownerKey = new NamespacedKey("minioncore", "owner-id");
    NamespacedKey groupKey = new NamespacedKey("minioncore", "group-id");
    NamespacedKey stateKey = new NamespacedKey("minioncore", "lifecycle-state");
    FakeAdapter adapter = new FakeAdapter(entityId);
    MinionGroupRegistry registry = new MinionGroupRegistry(adapter, MainThreadGuard.noop(),
        Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC), 3, 3);
    MinionSourceResolver resolver = new MinionSourceResolver(registry, ownerKey, groupKey, stateKey);
    AtomicReference<Throwable> assertionFailure = new AtomicReference<>();
    adapter.afterIndexed = groupId -> {
      try {
        Entity source = markedEntity(entityId, ownerKey, OWNER.toString(), groupKey, groupId.toString(), stateKey,
            MythicMinionAdapter.PROVISIONAL);
        MinionSource resolved = resolver.resolve(source).orElseThrow();
        check(resolved.provisional(), "indexed SPAWNING PDC source must resolve as provisional");
        check(DamageGuard.mustCancelProvisional(resolved),
            "DamageGuard must cancel indexed PROVISIONAL minion damage");
        Projectile projectile = (Projectile) Proxy.newProxyInstance(ProvisionalDamageGuardTest.class.getClassLoader(),
            new Class<?>[] {Projectile.class}, (proxy, method, arguments) -> method.getName().equals("getShooter")
                ? source : defaultValue(method.getReturnType()));
        MinionSource projectileRoot = resolver.resolve(projectile).orElseThrow();
        check(projectileRoot.provisional() && DamageGuard.mustCancelProvisional(projectileRoot),
            "projectile root must retain the provisional damage block");
      } catch (Throwable failure) {
        assertionFailure.set(failure);
      }
    };

    SummonResult result = registry.summon(OWNER, MythicMinionAdapter.MELEE_TIER_1, 1, Duration.ofSeconds(60));
    check(assertionFailure.get() == null, "provisional assertion failed: " + assertionFailure.get());
    check(result.success(), "test summon must activate after the provisional assertion: " + result.failure());
  }

  private static Entity markedEntity(UUID id, NamespacedKey ownerKey, String owner, NamespacedKey groupKey,
      String group, NamespacedKey stateKey, String state) {
    Map<NamespacedKey, String> values = new HashMap<>();
    values.put(ownerKey, owner);
    values.put(groupKey, group);
    values.put(stateKey, state);
    PersistentDataContainer pdc = (PersistentDataContainer) Proxy.newProxyInstance(
        ProvisionalDamageGuardTest.class.getClassLoader(), new Class<?>[] {PersistentDataContainer.class},
        (proxy, method, arguments) -> method.getName().equals("get")
            ? values.get(arguments[0]) : defaultValue(method.getReturnType()));
    return (Entity) Proxy.newProxyInstance(ProvisionalDamageGuardTest.class.getClassLoader(),
        new Class<?>[] {Entity.class, ProjectileSource.class},
        (proxy, method, arguments) -> switch (method.getName()) {
          case "getUniqueId" -> id;
          case "getPersistentDataContainer" -> pdc;
          default -> defaultValue(method.getReturnType());
        });
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) return null;
    if (type == boolean.class) return false;
    if (type == double.class) return 0.0d;
    if (type == float.class) return 0.0f;
    if (type == long.class) return 0L;
    if (type == int.class) return 0;
    if (type == short.class) return (short) 0;
    if (type == byte.class) return (byte) 0;
    if (type == char.class) return (char) 0;
    return null;
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static final class FakeAdapter implements MinionEntityAdapter {
    private final UUID entityId;
    private java.util.function.Consumer<UUID> afterIndexed;
    private UUID groupId;

    private FakeAdapter(UUID entityId) { this.entityId = entityId; }
    @Override public UUID spawn(String typeId, UUID ownerId, UUID groupId) { this.groupId = groupId; return entityId; }
    @Override public void onIndexed(UUID indexed) { afterIndexed.accept(groupId); }
    @Override public void despawn(UUID entityId) { }
    @Override public void setTarget(UUID entityId, UUID targetId) { }
    @Override public void clearTarget(UUID entityId) { }
  }
}
