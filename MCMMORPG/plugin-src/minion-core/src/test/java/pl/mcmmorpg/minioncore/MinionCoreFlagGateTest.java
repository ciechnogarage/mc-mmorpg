package pl.mcmmorpg.minioncore;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Contract tests for the pre-enable WorldGuard registration gate. */
public final class MinionCoreFlagGateTest {
  public static void main(String[] args) {
    registersDefaultDenyStateFlag();
    retainsExistingStateFlagAfterCollision();
    rejectsIncompatibleFlagCollision();
    propagatesLateRegistrationFailure();
    System.out.println("FLAG_GATE_TEST_PASS");
  }

  private static void registersDefaultDenyStateFlag() {
    MemoryFlagRegistry registry = new MemoryFlagRegistry();
    StateFlag registered = MinionCorePlugin.registerMinionPvpFlag(registry);
    check(registered == registry.get(MinionCorePlugin.MINION_PVP_FLAG_NAME), "fresh flag reference was not retained");
    check(registered.getDefault() == null, "minion-pvp must not have an implicit ALLOW default");
    check(!StateFlag.test(registered.getDefault()), "unconfigured minion-pvp must fail closed");
  }

  private static void retainsExistingStateFlagAfterCollision() {
    StateFlag existing = new StateFlag(MinionCorePlugin.MINION_PVP_FLAG_NAME, false);
    MemoryFlagRegistry registry = new MemoryFlagRegistry(existing);
    check(
        MinionCorePlugin.registerMinionPvpFlag(registry) == existing,
        "existing StateFlag collision must retain the registered instance");
  }

  private static void rejectsIncompatibleFlagCollision() {
    MemoryFlagRegistry registry = new MemoryFlagRegistry(new StringFlag(MinionCorePlugin.MINION_PVP_FLAG_NAME));
    IllegalStateException failure = expect(IllegalStateException.class, () -> MinionCorePlugin.registerMinionPvpFlag(registry));
    check(failure.getCause() instanceof FlagConflictException, "type conflict must preserve WorldGuard conflict cause");
  }

  private static void propagatesLateRegistrationFailure() {
    FlagRegistry locked = new MemoryFlagRegistry() {
      @Override
      public void register(Flag<?> flag) {
        throw new IllegalStateException("Flag registry is locked");
      }
    };
    IllegalStateException failure = expect(IllegalStateException.class, () -> MinionCorePlugin.registerMinionPvpFlag(locked));
    check("Flag registry is locked".equals(failure.getMessage()), "late registration must not fall back");
  }

  private static <T extends Throwable> T expect(Class<T> type, Runnable action) {
    try {
      action.run();
    } catch (Throwable failure) {
      if (type.isInstance(failure)) {
        return type.cast(failure);
      }
      throw new AssertionError("Expected " + type.getName() + " but got " + failure, failure);
    }
    throw new AssertionError("Expected " + type.getName() + " to be thrown");
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  private static class MemoryFlagRegistry implements FlagRegistry {
    private final Map<String, Flag<?>> flags = new LinkedHashMap<>();

    MemoryFlagRegistry(Flag<?>... initial) {
      for (Flag<?> flag : initial) {
        flags.put(flag.getName(), flag);
      }
    }

    @Override
    public void register(Flag<?> flag) {
      if (flags.containsKey(flag.getName())) {
        throw new FlagConflictException("Duplicate flag: " + flag.getName());
      }
      flags.put(flag.getName(), flag);
    }

    @Override
    public void registerAll(Collection<Flag<?>> entries) {
      for (Flag<?> entry : entries) {
        register(entry);
      }
    }

    @Override
    public Flag<?> get(String name) {
      return flags.get(name);
    }

    @Override
    public List<Flag<?>> getAll() {
      return new ArrayList<>(flags.values());
    }

    @Override
    public Map<Flag<?>, Object> unmarshal(Map<String, Object> values, boolean createUnknown) {
      return Map.of();
    }

    @Override
    public int size() {
      return flags.size();
    }

    @Override
    public Iterator<Flag<?>> iterator() {
      return flags.values().iterator();
    }
  }
}
