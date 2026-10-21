package pl.mcmmorpg.minioncore;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

/** Resolves vanilla melee/projectile hits to an active registry-backed minion root. */
public final class MinionSourceResolver {
  private final MinionGroupRegistry registry;
  private final NamespacedKey ownerKey;
  private final NamespacedKey groupKey;
  private final NamespacedKey stateKey;

  public MinionSourceResolver(MinionGroupRegistry registry, JavaPlugin plugin) {
    this(registry,
        new NamespacedKey(plugin, "owner-id"),
        new NamespacedKey(plugin, "group-id"),
        new NamespacedKey(plugin, "lifecycle-state"));
  }

  MinionSourceResolver(MinionGroupRegistry registry, NamespacedKey ownerKey,
      NamespacedKey groupKey, NamespacedKey stateKey) {
    this.registry = registry;
    this.ownerKey = ownerKey;
    this.groupKey = groupKey;
    this.stateKey = stateKey;
  }

  public Optional<MinionSource> resolve(Entity damager) {
    Entity root = rootEntity(damager);
    if (root == null) {
      return Optional.empty();
    }
    String ownerValue = root.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
    String groupValue = root.getPersistentDataContainer().get(groupKey, PersistentDataType.STRING);
    String stateValue = root.getPersistentDataContainer().get(stateKey, PersistentDataType.STRING);
    try {
      UUID ownerId = UUID.fromString(ownerValue);
      UUID groupId = UUID.fromString(groupValue);
      GroupLifecycleState binding = registry.bindingState(root.getUniqueId(), ownerId, groupId).orElse(null);
      if (binding == null) {
        return Optional.empty();
      }
      boolean provisional = MythicMinionAdapter.PROVISIONAL.equals(stateValue)
          && binding == GroupLifecycleState.SPAWNING;
      if (!provisional && !(MythicMinionAdapter.ACTIVE.equals(stateValue) && binding == GroupLifecycleState.ACTIVE)) {
        return Optional.empty();
      }
      return Optional.of(new MinionSource(ownerId, groupId, root, provisional));
    } catch (IllegalArgumentException | NullPointerException ignored) {
      return registryBackedSource(root);
    }
  }

  private Optional<MinionSource> registryBackedSource(Entity root) {
    return registry.groupIdForEntity(root.getUniqueId())
        .flatMap(registry::group)
        .map(group -> new MinionSource(group.ownerId(), group.groupId(), root, false));
  }

  private static Entity rootEntity(Entity damager) {
    if (damager instanceof Projectile projectile) {
      ProjectileSource shooter = projectile.getShooter();
      return shooter instanceof Entity entity ? entity : null;
    }
    return damager;
  }
}
