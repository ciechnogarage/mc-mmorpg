package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.lib.player.resource.ResourceUpdateReason;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

/** Jednorazowe rzuty. Lewa ręka kończy ładowanie automatycznie; prawa czeka na puszczenie PPM. */
final class RzutyOdrzutowe {
  private static final class Ladunek {
    Ksiega.Zaklecie zaklecie;
    long start, ostatniPpm;
    boolean obie, prawa, gotowe;
    boolean ppm;
  }
  private record Runa(ArmorStand znacznik, Ksiega.Zaklecie zaklecie, long wygasa, boolean obie) {}

  private final Rzucanie wejscie;
  private final Skille skille;
  private final Ksiega ksiega;
  private final EfektyCzasowe efekty;
  private final Map<UUID, Ladunek> prawa = new HashMap<>(), lewa = new HashMap<>();
  private final Map<UUID, Long> czekaLpm = new HashMap<>();
  private final Map<UUID, Deque<Runa>> runy = new HashMap<>();
  private long czas;

  RzutyOdrzutowe(Rzucanie wejscie, Ksiega ksiega, Skille skille, EfektyCzasowe efekty) {
    this.wejscie = wejscie; this.ksiega = ksiega; this.skille = skille; this.efekty = efekty;
  }

  private static String skill(Ksiega.Zaklecie z, String faza, String reka) {
    return "Zak_" + Character.toUpperCase(z.id().charAt(0)) + z.id().substring(1) + "_" + faza + "_" + reka;
  }

  private Ladunek zacznij(Player p, Ksiega.Zaklecie z, boolean prawy, boolean ppm) {
    Ladunek l = new Ladunek();
    l.zaklecie = z; l.start = czas; l.ostatniPpm = czas; l.prawa = prawy; l.ppm = ppm;
    (prawy ? prawa : lewa).put(p.getUniqueId(), l);
    return l;
  }

  void ppm(Player p, Ksiega.Zaklecie z, boolean prawy, int rowno) {
    if (z == null || !z.odrzutowe()) return;
    UUID u = p.getUniqueId();
    Map<UUID, Ladunek> mapa = prawy ? prawa : lewa;
    Ladunek l = mapa.get(u);
    if (l == null || !l.zaklecie.id().equals(z.id()) || !l.ppm) l = zacznij(p, z, prawy, true);
    l.ostatniPpm = czas;
    Long klik = czekaLpm.get(u);
    if (prawy && klik != null && czas - klik <= rowno &&
        z.equals(ksiega.w(p.getInventory().getItemInOffHand()))) {
      lewa.remove(u); l.obie = true; l.start = Math.max(l.start, klik); l.gotowe = false; czekaLpm.remove(u);
    }
  }

  void lpm(Player p, Ksiega.Zaklecie z, int rowno) {
    if (z == null || !z.odrzutowe()) return;
    UUID u = p.getUniqueId();
    Ladunek trzymanaLewa = lewa.get(u);
    if (trzymanaLewa != null && trzymanaLewa.ppm) return;
    Ladunek r = prawa.get(u);
    if (r != null && czas - r.ostatniPpm <= rowno && r.zaklecie.id().equals(z.id())) {
      r.obie = true; r.start = czas; r.gotowe = false; lewa.remove(u); czekaLpm.remove(u);
    } else czekaLpm.put(u, czas);
  }

  String stan(Player p, boolean prawaDlon) {
    Ladunek l = (prawaDlon ? prawa : lewa).get(p.getUniqueId());
    if (l == null && !prawaDlon) {
      l = prawa.get(p.getUniqueId());
      if (l == null || !l.obie) return czekaLpm.containsKey(p.getUniqueId()) ? "laduje" : null;
    }
    return l == null ? null : l.gotowe ? "gotowe" : "laduje";
  }

  boolean aktywna(Player p, boolean prawaDlon) {
    Ladunek l = (prawaDlon ? prawa : lewa).get(p.getUniqueId());
    return l != null || (!prawaDlon && prawa.containsKey(p.getUniqueId()) && prawa.get(p.getUniqueId()).obie);
  }

  boolean rzuca(Player p) {
    UUID u = p.getUniqueId();
    return prawa.containsKey(u) || lewa.containsKey(u) || czekaLpm.containsKey(u);
  }

  void przerwij(Player p) {
    UUID u = p.getUniqueId();
    prawa.remove(u); lewa.remove(u); czekaLpm.remove(u);
  }

  void wyjscie(Player p) { przerwij(p); usunRuny(p.getUniqueId()); }

  void wylacz() {
    prawa.clear(); lewa.clear(); czekaLpm.clear();
    for (UUID u : java.util.List.copyOf(runy.keySet())) usunRuny(u);
  }

  private void usunRuny(UUID u) {
    Deque<Runa> lista = runy.remove(u);
    if (lista != null) for (Runa r : lista) r.znacznik.remove();
  }

  void tick(long teraz, int tolerancja, int rowno) {
    czas = teraz;
    for (Iterator<Map.Entry<UUID, Long>> it = czekaLpm.entrySet().iterator(); it.hasNext();) {
      var e = it.next();
      if (czas - e.getValue() <= rowno) continue;
      Player p = Bukkit.getPlayer(e.getKey());
      Ksiega.Zaklecie z = p == null ? null : ksiega.w(p.getInventory().getItemInOffHand());
      if (p != null && p.isOnline() && !p.isDead() && wejscie.trybLpm(p) && z != null && z.odrzutowe())
        zacznij(p, z, false, false).start = e.getValue();
      it.remove();
    }
    ladunki(prawa, tolerancja);
    ladunki(lewa, tolerancja);
    runy();
  }

  private void ladunki(Map<UUID, Ladunek> mapa, int tolerancja) {
    for (Iterator<Map.Entry<UUID, Ladunek>> it = mapa.entrySet().iterator(); it.hasNext();) {
      var e = it.next(); Ladunek l = e.getValue();
      Player p = Bukkit.getPlayer(e.getKey());
      if (p == null || !p.isOnline() || p.isDead() ||
          (!wejscie.trybLpm(p) && !(wejscie.prawaBron(p) && !l.prawa && l.ppm)) ||
          !l.zaklecie.equals(ksiega.w(l.prawa ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand())) ||
          (l.obie && !l.zaklecie.equals(ksiega.w(p.getInventory().getItemInOffHand())))) { it.remove(); continue; }
      String reka = l.obie ? "Obie" : l.prawa ? "P" : "L";
      boolean trzyma = !l.ppm || czas - l.ostatniPpm <= tolerancja;
      if (!trzyma) {
        if (l.gotowe) wystrzel(p, l, reka);
        it.remove(); continue;
      }
      // Gotowość prawa/obie wymaga pakietu PPM po czasie ładowania. Samo okno tolerancji
      // mogłoby uznać puszczenie tuż przed progiem za pełne ładowanie.
      if (!l.gotowe && (l.ppm ? l.ostatniPpm : czas) - l.start >= l.zaklecie.czasLadowania()) {
        l.gotowe = true;
        skille.cast(p, skill(l.zaklecie, "Gotowe", reka));
        if (!l.ppm) { wystrzel(p, l, reka); it.remove(); continue; }
      }
      if (!l.gotowe && (czas - l.start) % 3 == 0) skille.cast(p, skill(l.zaklecie, "Laduj", reka));
    }
  }

  private void wystrzel(Player p, Ladunek l, String reka) {
    Ksiega.Zaklecie z = l.zaklecie;
    Location miejsce = null;
    if (z.typ().equals("runa")) {
      RayTraceResult r = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(),
          z.zasieg(), FluidCollisionMode.NEVER, true);
      if (r == null || r.getHitBlock() == null || r.getHitBlockFace() != BlockFace.UP) return;
      miejsce = r.getHitPosition().toLocation(p.getWorld()).add(0, 0.02, 0);
      if (!r.getHitBlock().getRelative(BlockFace.UP).isPassable() ||
          !r.getHitBlock().getRelative(BlockFace.UP, 2).isPassable()) return;
    }
    if (!efekty.przygotuj(p, z, l.obie)) { p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.6f); return; }   // brak celu/miejsca: bez kosztu
    double koszt = z.mana() * (l.obie ? z.manaOburacz() : 1);
    org.bukkit.Bukkit.getLogger().info("[Zaklecia] [wystrzal] t=" + czas + " " + p.getName() + " " + z.id() + " " + reka + " koszt=" + koszt);
    PlayerData dane = PlayerData.get(p);
    if (dane.getMana() < koszt) { skille.cast(p, ksiega.skBrakMany); return; }
    dane.giveMana(-koszt, ResourceUpdateReason.SKILL);
    if (z.typ().equals("bron") || z.typ().equals("przywolanie") || z.typ().equals("na_cel")
        || z.typ().equals("ladowany") && !z.cel().equals("stozek"))
      efekty.wystrzel(p, z, l.prawa, l.obie, reka);
    else if (miejsce == null) skille.cast(p, skill(z, "Wystrzal", reka));
    else {
      skille.punkt(p, skill(z, "Wystrzal", reka), miejsce);
      postaw(p, z, miejsce, l.obie);
    }
  }

  private void postaw(Player p, Ksiega.Zaklecie z, Location miejsce, boolean obie) {
    ArmorStand znacznik = miejsce.getWorld().spawn(miejsce, ArmorStand.class, a -> {
      a.setInvisible(true); a.setMarker(true); a.setGravity(false); a.setInvulnerable(true); a.setPersistent(false);
    });
    Deque<Runa> lista = runy.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
    while (lista.stream().filter(r -> r.zaklecie.id().equals(z.id())).count() >= z.limitAktywnych()) {
      Runa stara = lista.stream().filter(r -> r.zaklecie.id().equals(z.id())).findFirst().orElseThrow();
      stara.znacznik.remove(); lista.remove(stara);
    }
    lista.addLast(new Runa(znacznik, z, czas + z.czasZycia(), obie));
  }

  private void runy() {
    if (czas % 4 != 0) return;
    for (Iterator<Map.Entry<UUID, Deque<Runa>>> i = runy.entrySet().iterator(); i.hasNext();) {
      var e = i.next(); Player p = Bukkit.getPlayer(e.getKey());
      for (Iterator<Runa> j = e.getValue().iterator(); j.hasNext();) {
        Runa r = j.next();
        if (p == null || !p.isOnline() || czas >= r.wygasa || !r.znacznik.isValid()) {
          r.znacznik.remove(); j.remove(); continue;
        }
        Location at = r.znacznik.getLocation();
        skille.punkt(p, "Zak_" + Character.toUpperCase(r.zaklecie.id().charAt(0)) +
            r.zaklecie.id().substring(1) + "_RunaTik", at);
        boolean cel = at.getWorld().getNearbyEntities(at, r.zaklecie.promien(), 1.5, r.zaklecie.promien()).stream()
            .anyMatch(x -> x instanceof org.bukkit.entity.Enemy && x instanceof LivingEntity le && !le.isDead());   // tylko wrogie istoty: nie gracze, NPC, Chowance
        if (cel) {
          skille.punkt(p, "Zak_" + Character.toUpperCase(r.zaklecie.id().charAt(0)) +
              r.zaklecie.id().substring(1) + (r.obie ? "_Wybuch_Obie" : "_Wybuch"), at);
          r.znacznik.remove(); j.remove();
        }
      }
      if (e.getValue().isEmpty()) i.remove();
    }
  }
}
