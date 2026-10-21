package pl.mcmmorpg.companionbridge;

import java.lang.reflect.InvocationTargetException;

/**
 * Method.invoke() opakowuje kazdy wyjatek celu w InvocationTargetException —
 * logowanie samego wyjatku bez unwrap ukrywa prawdziwa przyczyne (patrz
 * plan companion-bridge: przypadek syntetycznego UUID bez danych MMOCore
 * logowal tylko "InvocationTargetException" bez stack trace przyczyny).
 */
final class ReflectionSupport {
  private ReflectionSupport() {
  }

  static Throwable unwrap(Throwable ex) {
    return (ex instanceof InvocationTargetException && ex.getCause() != null) ? ex.getCause() : ex;
  }
}
