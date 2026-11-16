package pl.mcmmorpg.huddane;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.Indyuce.mmocore.api.event.PlayerExperienceGainEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Zdarzenia dla HUD-a: ostatni cel walki gracza (kogo bil albo kto go bil) i kanal zdobyczy (EXP, podniesione
 * przedmioty). Tylko pamiec w RAM, nic nie zapisuje.
 */
final class Zdarzenia implements Listener {
  static final long CEL_MS = 8000, ZDOBYCZ_MS = 4500;
  record Cel(UUID id, long czas) {}
  record Wpis(String tekst, String rodzaj, long czas) {}

  final Map<UUID, Cel> cele = new ConcurrentHashMap<>();
  final Map<UUID, Deque<Wpis>> zdobycz = new ConcurrentHashMap<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void obrazenia(EntityDamageByEntityEvent e) {
    Entity zrodlo = e.getDamager();
    if (zrodlo instanceof Projectile pr && pr.getShooter() instanceof Entity s) zrodlo = s;
    long t = System.currentTimeMillis();
    // gracz bije moba: cel = mob; mob bije gracza: cel = mob, chyba ze gracz ma juz swiezy cel
    if (zrodlo instanceof Player p && e.getEntity() instanceof LivingEntity le && !(le instanceof Player) && !jestSluga(le, p))
      cele.put(p.getUniqueId(), new Cel(le.getUniqueId(), t));
    else if (e.getEntity() instanceof Player p && zrodlo instanceof LivingEntity le && !(le instanceof Player)) {
      Cel c = cele.get(p.getUniqueId());
      if (c == null || t - c.czas() > 2000) cele.put(p.getUniqueId(), new Cel(le.getUniqueId(), t));
    }
  }

  private static boolean jestSluga(LivingEntity le, Player p) {
    return MythicBukkit.inst().getMobManager().getActiveMob(le.getUniqueId())
        .map(am -> am.getParentUUID().isPresent() && p.getUniqueId().equals(am.getParentUUID().get())).orElse(false);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void exp(PlayerExperienceGainEvent e) {
    Player p = e.getData().getPlayer();
    if (p == null || e.getExperience() < 1) return;
    String co = e.hasProfession() ? e.getProfession().getName() : "EXP";
    dodaj(p, "+" + Math.round(e.getExperience()) + " " + co, e.hasProfession() ? "profesja" : "exp");
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void podniesienie(EntityPickupItemEvent e) {
    if (!(e.getEntity() instanceof Player p)) return;
    ItemStack it = e.getItem().getItemStack();
    // effectiveName = nazwa wlasna albo tlumaczalna nazwa typu; plain = bez kolorow (MythicHUD liczy szerokosc z surowego tekstu)
    String nazwa = PlainTextComponentSerializer.plainText().serialize(it.effectiveName());
    // vanilla bez nazwy wlasnej daje klucz tlumaczenia (item.minecraft.x) - serwer nie zna jezyka klienta
    if (nazwa.startsWith("item.") || nazwa.startsWith("block.")) {
      String m = it.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
      nazwa = Character.toUpperCase(m.charAt(0)) + m.substring(1);
    }
    dodaj(p, (it.getAmount() > 1 ? it.getAmount() + "× " : "") + nazwa, "przedmiot");
  }

  private void dodaj(Player p, String tekst, String rodzaj) {
    Deque<Wpis> q = zdobycz.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
    synchronized (q) {
      long t = System.currentTimeMillis();
      Wpis ost = q.peekFirst();
      // ten sam rodzaj EXP w krotkim czasie = sumowanie zamiast spamu
      if (ost != null && rodzaj.equals("exp") && ost.rodzaj().equals("exp") && t - ost.czas() < 1500) {
        q.pollFirst();
        long a = Long.parseLong(ost.tekst().substring(1, ost.tekst().indexOf(' ')));
        long b = Long.parseLong(tekst.substring(1, tekst.indexOf(' ')));
        tekst = "+" + (a + b) + " EXP";
      }
      q.addFirst(new Wpis(tekst.length() > 32 ? tekst.substring(0, 31) + "…" : tekst, rodzaj, t));
      while (q.size() > 4) q.pollLast();
    }
  }

  /** Aktualny cel (zywy, swiezy) albo null. */
  LivingEntity cel(Player p) {
    Cel c = cele.get(p.getUniqueId());
    if (c == null || System.currentTimeMillis() - c.czas() > CEL_MS) return null;
    Entity e = Bukkit.getEntity(c.id());
    if (!(e instanceof LivingEntity le) || le.isDead() || !le.isValid() || le.getWorld() != p.getWorld()) return null;
    return le;
  }

  static ActiveMob mm(LivingEntity le) { return MythicBukkit.inst().getMobManager().getActiveMob(le.getUniqueId()).orElse(null); }

  java.util.List<Wpis> swieza(Player p) {
    Deque<Wpis> q = zdobycz.get(p.getUniqueId());
    if (q == null) return java.util.List.of();
    long t = System.currentTimeMillis();
    synchronized (q) { return q.stream().filter(w -> t - w.czas() < ZDOBYCZ_MS).toList(); }
  }

  @EventHandler public void wyjscie(PlayerQuitEvent e) { cele.remove(e.getPlayer().getUniqueId()); zdobycz.remove(e.getPlayer().getUniqueId()); }
}
