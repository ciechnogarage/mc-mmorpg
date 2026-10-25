package pl.mcmmorpg.huddane;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import net.Indyuce.mmocore.party.provided.Party;

/** Stan widziany przez jednego gracza w jednej chwili; liczony leniwie i trzymany krotko. */
final class Migawka {
  record Czlonek(String nazwa, int poziom, double hp, double hpMax, double zasob, double zasobMax,
                 boolean lider, boolean daleko, boolean martwy) {}
  record Sluga(String nazwa, double hp, double hpMax, int sekundy) {}
  record Efekt(String id, int sekundy) {}

  /** Pierwsze zobaczenie przywolanca: jego zegar, bo MythicMobs nie podaje czasu zycia. */
  static final Map<UUID, Long> pierwszyRaz = new ConcurrentHashMap<>();

  final long czas;
  final List<Czlonek> druzyna = new ArrayList<>();
  final List<Sluga> slugi = new ArrayList<>();
  final List<Efekt> efekty = new ArrayList<>();

  private Migawka(long czas) { this.czas = czas; }

  static Migawka zbierz(HudDane pl, Player p, long teraz) {
    Migawka m = new Migawka(teraz);
    m.zbierzDruzyne(pl, p);
    m.zbierzSlugi(pl, p, teraz);
    m.zbierzEfekty(pl, p);
    return m;
  }

  private void zbierzDruzyne(HudDane pl, Player p) {
    if (!PlayerData.has(p)) return;
    AbstractParty party = PlayerData.get(p).getParty();
    if (party == null) return;
    UUID lider = party instanceof Party pr && pr.getOwner() != null ? pr.getOwner().getUniqueId() : null;
    for (PlayerData d : party.getOnlineMembers()) {
      Player o = d.getPlayer();
      if (o == null || o.getUniqueId().equals(p.getUniqueId())) continue;
      boolean daleko = o.getWorld() != p.getWorld() || o.getLocation().distanceSquared(p.getLocation()) > (double) pl.zasieg * pl.zasieg;
      double max = o.getAttribute(Attribute.MAX_HEALTH) != null ? o.getAttribute(Attribute.MAX_HEALTH).getValue() : 20;
      druzyna.add(new Czlonek(o.getName(), d.getLevel(), o.getHealth(), max, d.getMana(), d.getStats().getStat("MAX_MANA"),
          o.getUniqueId().equals(lider), daleko, o.isDead()));
    }
    // Lider na gorze, potem wg imienia: kolejnosc nie skacze przy zmianach HP.
    druzyna.sort(Comparator.comparing((Czlonek c) -> !c.lider()).thenComparing(Czlonek::nazwa, String.CASE_INSENSITIVE_ORDER));
  }

  /** Indeks rodzic -> jego aktywne moby, budowany raz na tick serwera (jedno przejscie po wszystkich mobach MM zamiast jednego na gracza). */
  private record Indeks(int tick, Map<UUID, List<ActiveMob>> poRodzicu) {}
  private static volatile Indeks indeks = new Indeks(-1, Map.of());

  private static List<ActiveMob> przywolancePo(UUID rodzic) {
    int tick = org.bukkit.Bukkit.getCurrentTick();
    Indeks i = indeks;
    if (i.tick() != tick) {
      Map<UUID, List<ActiveMob>> m = new java.util.HashMap<>();
      for (ActiveMob a : MythicBukkit.inst().getMobManager().getActiveMobs(a -> a.getParentUUID().isPresent()))
        m.computeIfAbsent(a.getParentUUID().get(), k -> new ArrayList<>()).add(a);
      i = new Indeks(tick, m);
      indeks = i;
    }
    return i.poRodzicu().getOrDefault(rodzic, List.of());
  }

  private void zbierzSlugi(HudDane pl, Player p, long teraz) {
    UUID id = p.getUniqueId();
    for (ActiveMob am : przywolancePo(id)) {
      Entity e = am.getEntity() == null ? null : am.getEntity().getBukkitEntity();
      if (!(e instanceof LivingEntity le) || le.isDead()) continue;
      Integer ticki = pl.czasZycia.get(am.getMobType());
      if (ticki == null) continue; // tylko znane przywolance; znaczniki i efekty umiejetnosci tez maja rodzica
      long start = pierwszyRaz.computeIfAbsent(am.getUniqueId(), k -> teraz);
      int zostalo = (int) Math.max(0, Math.ceil((ticki * 50L - (teraz - start)) / 1000.0));
      double max = le.getAttribute(Attribute.MAX_HEALTH) != null ? le.getAttribute(Attribute.MAX_HEALTH).getValue() : le.getHealth();
      slugi.add(new Sluga(am.getDisplayName(), le.getHealth(), max, zostalo));
    }
    slugi.sort(Comparator.comparingInt(Sluga::sekundy));
    if (pierwszyRaz.size() > 512) pierwszyRaz.keySet().removeIf(u -> MythicBukkit.inst().getMobManager().getActiveMob(u).isEmpty());
  }

  private void zbierzEfekty(HudDane pl, Player p) {
    int i = 0;
    for (Map.Entry<String, String> e : pl.efekty.entrySet()) {
      for (PotionEffect pe : p.getActivePotionEffects()) {
        if (!pe.getType().getKey().getKey().equals(e.getKey())) continue;
        efekty.add(new Efekt(e.getValue(), pe.isInfinite() ? -1 : pe.getDuration() / 20));
      }
      if (++i > 64) break;
    }
  }
}
