package pl.mcmmorpg.chowance.gra;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Przesunięcie ciała w animacji BEZ {@code EntityTeleportEvent}. Animacje walki (doskok, odrzut, drżenie) ruszały
 * ciało co tick przez {@code teleport()}, a każdy teleport moba MythicMobs odpala jego
 * {@code MobListeners.onTeleportEvent} (wyzwalacze umiejętności + zadanie w puli wątków). Spark world1 2026-09-30:
 * 330 z 350 ms animacji to był ten nasłuch — koszt rośnie liniowo z liczbą walk naraz.
 *
 * <p>Tu: {@code Entity#snapTo} serwera (Paper 1.21.x, mapowania Mojanga w runtime) — zmienia pozycję, sekcję chunka
 * i obrót; klientom wysyła ją zwykły tracker encji w następnym ticku. Brak metody = zwykły teleport (jak dawniej).
 */
public final class Przesun {
  private static volatile Method uchwyt, snap;
  private static volatile boolean brak;

  private Przesun() {}

  /** Tylko ten sam świat i krótki ruch (animacja); resztę robi zwykły teleport. */
  public static void animuj(Entity e, Location l, Logger log) {
    if (brak || e.getWorld() != l.getWorld() || e.getLocation().distanceSquared(l) > 64) { e.teleport(l); return; }
    try {
      if (snap == null) {
        Method h = e.getClass().getMethod("getHandle");
        Object nms = h.invoke(e);
        Method m = znajdz(nms.getClass(), "snapTo");
        if (m == null) m = znajdz(nms.getClass(), "moveTo");
        if (m == null) throw new NoSuchMethodException("snapTo/moveTo(double,double,double,float,float)");
        uchwyt = h; snap = m;
      }
      snap.invoke(uchwyt.invoke(e), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    } catch (ReflectiveOperationException | RuntimeException ex) {
      brak = true;
      if (log != null) log.warning("Przesun: bezpośredni ruch niedostępny (" + ex + ") — animacje przez teleport");
      e.teleport(l);
    }
  }

  private static Method znajdz(Class<?> k, String nazwa) {
    for (Class<?> c = k; c != null; c = c.getSuperclass()) {
      try {
        Method m = c.getDeclaredMethod(nazwa, double.class, double.class, double.class, float.class, float.class);
        m.setAccessible(true);
        return m;
      } catch (NoSuchMethodException ignored) { }
    }
    return null;
  }
}
