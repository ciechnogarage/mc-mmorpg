package pl.mcmmorpg.minioncore;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.api.mobs.entities.SpawnReason;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Direct MythicMobs 5.11.2 boundary for the first, deliberately narrow melee slice.
 *
 * <p>Spawn remains fail-closed: the Mythic definition starts with {@code NoAI}, this
 * adapter writes {@code PROVISIONAL} PDC data before returning the entity ID, and
 * {@link #activate(UUID)} changes PDC state before it enables AI. A caller must only
 * invoke {@code activate} after T2 has committed all group indexes.</p>
 */
public final class MythicMinionAdapter implements MinionEntityAdapter {
  public static final String MELEE_TIER_1 = "minion.melee.tier1";
  public static final String RANGED_TIER_1 = "minion.ranged.tier1";
  public static final String SUPPORT_TIER_1 = "minion.support.tier1";
  private static final String MYTHIC_MELEE_TIER_1 = "minion_melee_tier1";
  private static final String MYTHIC_RANGED_TIER_1 = "minion_ranged_tier1";
  private static final String MYTHIC_SUPPORT_TIER_1 = "minion_support_tier1";
  static final String PROVISIONAL = "PROVISIONAL";
  static final String ACTIVE = "ACTIVE";
  private static final double[][] SPAWN_OFFSETS = {
      {1.5d, 0.0d},
      {-1.5d, 0.0d},
      {0.0d, 1.5d},
      {0.0d, -1.5d},
      {2.25d, 1.25d},
      {-2.25d, 1.25d},
      {2.25d, -1.25d},
      {-2.25d, -1.25d},
      {0.0d, 0.0d}
  };
  private static final double MINION_SPACING_SQUARED = 1.21d;

  private final NamespacedKey ownerKey;
  private final NamespacedKey groupKey;
  private final NamespacedKey stateKey;
  private final Map<UUID, ActiveMob> provisional = new ConcurrentHashMap<>();
  private final Map<UUID, ActiveMob> active = new ConcurrentHashMap<>();

  public MythicMinionAdapter(JavaPlugin plugin) {
    Objects.requireNonNull(plugin, "plugin");
    this.ownerKey = new NamespacedKey(plugin, "owner-id");
    this.groupKey = new NamespacedKey(plugin, "group-id");
    this.stateKey = new NamespacedKey(plugin, "lifecycle-state");
  }

  @Override
  public UUID spawn(String typeId, UUID ownerId, UUID groupId) {
    if (!MELEE_TIER_1.equals(typeId) && !RANGED_TIER_1.equals(typeId) && !SUPPORT_TIER_1.equals(typeId)) {
      throw new IllegalArgumentException("Unsupported minion type: " + typeId);
    }
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(groupId, "groupId");

    Player owner = Bukkit.getPlayer(ownerId);
    if (owner == null || !owner.isOnline()) {
      throw new IllegalStateException("Minion owner is not online");
    }
    MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob(mythicTypeId(typeId))
        .orElseThrow(() -> new IllegalStateException("Missing MythicMobs definition: " + typeId));
    Location location = findSpawnLocation(owner);
    ActiveMob activeMob = mob.spawn(
        BukkitAdapter.adapt(location),
        1.0d,
        SpawnReason.SUMMON,
        entity -> markProvisional(entity, ownerId, groupId));
    Entity entity = activeMob.getEntity().getBukkitEntity();
    markProvisional(entity, ownerId, groupId);
    if (!(entity instanceof LivingEntity living)) {
      activeMob.remove();
      throw new IllegalStateException("Mythic melee minion did not spawn as a living entity");
    }
    living.setAI(false);
    living.setCollidable(false);

    UUID entityId = entity.getUniqueId();
    ActiveMob existing = provisional.putIfAbsent(entityId, activeMob);
    if (existing != null || active.containsKey(entityId)) {
      provisional.remove(entityId, activeMob);
      activeMob.remove();
      throw new IllegalStateException("Duplicate minion entity ID: " + entityId);
    }
    return entityId;
  }

  private Location findSpawnLocation(Player owner) {
    Location base = owner.getLocation();
    World world = base.getWorld();
    if (world == null) {
      return base.clone();
    }
    for (double[] offset : SPAWN_OFFSETS) {
      for (int dy = -2; dy <= 4; dy++) {
        Location candidate = centered(base.clone().add(offset[0], dy, offset[1]));
        if (isSafeSpawn(candidate) && isSeparated(candidate)) {
          return candidate;
        }
      }
    }
    for (double[] offset : SPAWN_OFFSETS) {
      Location horizontal = centered(base.clone().add(offset[0], 0.0d, offset[1]));
      int surfaceY = world.getHighestBlockYAt(horizontal);
      Location candidate = horizontal.clone();
      candidate.setY(surfaceY + 1.0d);
      if (isSafeSpawn(candidate) && isSeparated(candidate)) {
        return candidate;
      }
    }
    Location fallback = base.clone().add(0.0d, 1.0d, 0.0d);
    fallback.setYaw(base.getYaw());
    fallback.setPitch(base.getPitch());
    return fallback;
  }

  private static Location centered(Location location) {
    location.setX(location.getBlockX() + 0.5d);
    location.setZ(location.getBlockZ() + 0.5d);
    return location;
  }

  private static boolean isSafeSpawn(Location location) {
    World world = location.getWorld();
    if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
      return false;
    }
    Block feet = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    Block head = world.getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());
    Block ground = world.getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ());
    return feet.isPassable()
        && head.isPassable()
        && ground.getType().isSolid()
        && !isHazard(feet.getType())
        && !isHazard(head.getType())
        && !isHazard(ground.getType());
  }

  private static boolean isHazard(Material material) {
    return material == Material.LAVA
        || material == Material.WATER
        || material == Material.FIRE
        || material == Material.SOUL_FIRE
        || material == Material.CACTUS
        || material == Material.MAGMA_BLOCK
        || material == Material.CAMPFIRE
        || material == Material.SOUL_CAMPFIRE
        || material == Material.POWDER_SNOW
        || material == Material.SWEET_BERRY_BUSH
        || material == Material.WITHER_ROSE;
  }

  private boolean isSeparated(Location candidate) {
    for (ActiveMob activeMob : provisional.values()) {
      if (isTooClose(candidate, activeMob)) {
        return false;
      }
    }
    for (ActiveMob activeMob : active.values()) {
      if (isTooClose(candidate, activeMob)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isTooClose(Location candidate, ActiveMob activeMob) {
    Entity entity = activeMob.getEntity().getBukkitEntity();
    return entity != null
        && entity.isValid()
        && entity.getWorld().equals(candidate.getWorld())
        && entity.getLocation().distanceSquared(candidate) < MINION_SPACING_SQUARED;
  }

  static String mythicTypeId(String canonicalTypeId) {
    return switch (canonicalTypeId) {
      case MELEE_TIER_1 -> MYTHIC_MELEE_TIER_1;
      case RANGED_TIER_1 -> MYTHIC_RANGED_TIER_1;
      case SUPPORT_TIER_1 -> MYTHIC_SUPPORT_TIER_1;
      default -> throw new IllegalArgumentException("Unsupported minion type: " + canonicalTypeId);
    };
  }

  @Override
  public void activate(UUID entityId) {
    ActiveMob activeMob = provisional.remove(entityId);
    if (activeMob == null) {
      throw new IllegalStateException("Cannot activate unknown or already active minion: " + entityId);
    }
    Entity entity = activeMob.getEntity().getBukkitEntity();
    if (!(entity instanceof LivingEntity living) || entity.isDead() || !entity.isValid()) {
      activeMob.remove();
      throw new IllegalStateException("Cannot activate unavailable minion: " + entityId);
    }
    writeState(entity, ACTIVE);
    living.setAI(true);
    living.setCollidable(false);
    if (active.putIfAbsent(entityId, activeMob) != null) {
      living.setAI(false);
      activeMob.remove();
      throw new IllegalStateException("Duplicate active minion entity ID: " + entityId);
    }
  }

  @Override
  public void despawn(UUID entityId) {
    ActiveMob activeMob = provisional.remove(entityId);
    if (activeMob == null) {
      activeMob = active.remove(entityId);
    }
    if (activeMob == null) {
      return;
    }
    activeMob.remove();
  }

  @Override
  public void onEntityRemoved(UUID entityId) {
    provisional.remove(entityId);
    active.remove(entityId);
  }

  @Override
  public void setTarget(UUID entityId, UUID targetId) {
    ActiveMob activeMob = active.get(entityId);
    Entity target = Bukkit.getEntity(targetId);
    if (activeMob == null || !(target instanceof LivingEntity) || target.isDead() || !target.isValid()) {
      throw new IllegalStateException("Cannot target with an inactive minion or invalid target");
    }
    activeMob.setTarget(BukkitAdapter.adapt(target));
    Entity minion = activeMob.getEntity().getBukkitEntity();
    if (minion instanceof Mob mob) {
      mob.setTarget((LivingEntity) target);
    }
  }

  @Override
  public void clearTarget(UUID entityId) {
    ActiveMob activeMob = active.get(entityId);
    if (activeMob == null) {
      return;
    }
    activeMob.resetTarget();
    Entity minion = activeMob.getEntity().getBukkitEntity();
    if (minion instanceof Mob mob) {
      mob.setTarget(null);
    }
  }

  private void markProvisional(Entity entity, UUID ownerId, UUID groupId) {
    if (!(entity instanceof LivingEntity living)) {
      throw new IllegalStateException("Mythic melee minion must be a living entity");
    }
    PersistentDataContainer data = entity.getPersistentDataContainer();
    data.set(ownerKey, PersistentDataType.STRING, ownerId.toString());
    data.set(groupKey, PersistentDataType.STRING, groupId.toString());
    writeState(entity, PROVISIONAL);
    living.setAI(false);
    living.setCollidable(false);
  }

  private void writeState(Entity entity, String state) {
    entity.getPersistentDataContainer().set(stateKey, PersistentDataType.STRING, state);
  }
}
