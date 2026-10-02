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

/** Jednorazowe rzuty: PPM trzyma lewą, LPM ładuje prawą automatycznie. */
final class RzutyOdrzutowe {
  private static final class Ladunek {
    UUID gracz;
    Ksiega.Zaklecie zaklecie;
    long start, startP, startL, ostatniPpm;
    boolean prawa, lewa, ppm, gotowe;
    boolean obie() { return prawa && lewa; }
  }
  private record Runa(ArmorStand znacznik, Ksiega.Zaklecie zaklecie, long wygasa, boolean obie) {}

  private final Rzucanie wejscie;
  private final Skille skille;
  private final Ksiega ksiega;
  private final EfektyCzasowe efekty;
  private final Map<UUID, Ladunek> prawa = new HashMap<>(), lewa = new HashMap<>();
  private final Map<UUID, Deque<Runa>> runy = new HashMap<>();
  private long czas;

  RzutyOdrzutowe(Rzucanie wejscie, Ksiega ksiega, Skille skille, EfektyCzasowe efekty) {
    this.wejscie = wejscie; this.ksiega = ksiega; this.skille = skille; this.efekty = efekty;
  }

  private static String skill(Ksiega.Zaklecie z, String faza, String reka) {
    return "Zak_" + Character.toUpperCase(z.id().charAt(0)) + z.id().substring(1) + "_" + faza + "_" + reka;
  }

  private Ladunek zacznij(Player p, Ksiega.Zaklecie z, boolean prawy, int rowno) {
    UUID u = p.getUniqueId();
    Ladunek drugi = (prawy ? lewa : prawa).get(u);
    long startDrugiej = drugi == null ? -1 : prawy ? drugi.startL : drugi.startP;
    if (drugi != null && drugi.zaklecie.id().equals(z.id()) && czas - startDrugiej <= rowno) {
      if (prawy) { drugi.prawa = true; drugi.startP = czas; prawa.put(u, drugi); }
      else { drugi.lewa = true; drugi.ppm = true; drugi.startL = czas; drugi.ostatniPpm = czas; lewa.put(u, drugi); }
      drugi.start = czas; drugi.gotowe = false;
      return drugi;
    }
    Ladunek l = new Ladunek();
    l.gracz = u;
    l.zaklecie = z; l.start = czas; l.ostatniPpm = czas;
    l.prawa = prawy; l.lewa = !prawy; l.startP = prawy ? czas : -1; l.startL = prawy ? -1 : czas;
    l.ppm = !prawy;
    (prawy ? prawa : lewa).put(u, l);
    return l;
  }

  void ppm(Player p, Ksiega.Zaklecie z, int rowno) {
    if (z == null || !z.odrzutowe()) return;
    UUID u = p.getUniqueId();
    Ladunek l = lewa.get(u);
    if (l == null || !l.zaklecie.id().equals(z.id())) l = zacznij(p, z, false, rowno);
    l.ostatniPpm = czas;
  }

  void lpm(Player p, Ksiega.Zaklecie z, int rowno) {
    if (z == null || !z.odrzutowe()) return;
    Ladunek r = prawa.get(p.getUniqueId());
    if (r == null || !r.zaklecie.id().equals(z.id())) zacznij(p, z, true, rowno);
  }

  String stan(Player p, boolean prawaDlon) {
    Ladunek l = (prawaDlon ? prawa : lewa).get(p.getUniqueId());
    return l == null ? null : l.gotowe ? "gotowe" : "laduje";
  }

  boolean aktywna(Player p, boolean prawaDlon) {
    Ladunek l = (prawaDlon ? prawa : lewa).get(p.getUniqueId());
    return l != null;
  }

  boolean rzuca(Player p) {
    UUID u = p.getUniqueId();
    return prawa.containsKey(u) || lewa.containsKey(u);
  }

  void przerwij(Player p) {
    UUID u = p.getUniqueId();
    prawa.remove(u); lewa.remove(u);
  }

  void wyjscie(Player p) { przerwij(p); usunRuny(p.getUniqueId()); }

  void wylacz() {
    prawa.clear(); lewa.clear();
    for (UUID u : java.util.List.copyOf(runy.keySet())) usunRuny(u);
  }

  private void usunRuny(UUID u) {
    Deque<Runa> lista = runy.remove(u);
    if (lista != null) for (Runa r : lista) r.znacznik.remove();
  }

  void ustawCzas(long teraz) { czas = teraz; }

  void tick(long teraz, int tolerancja) {
    czas = teraz;
    for (Ladunek l : java.util.List.copyOf(new java.util.HashSet<>(prawa.values()))) ladunek(l, tolerancja);
    for (Ladunek l : java.util.List.copyOf(new java.util.HashSet<>(lewa.values())))
      if (!l.prawa) ladunek(l, tolerancja);
    runy();
  }

  private void usun(UUID u, Ladunek l) {
    prawa.remove(u, l); lewa.remove(u, l);
  }

  private void ladunek(Ladunek l, int tolerancja) {
    UUID u = l.gracz;
    Player p = Bukkit.getPlayer(u);
    if (p == null || !p.isOnline() || p.isDead() ||
        (!wejscie.trybLpm(p) && l.prawa) ||
        (l.prawa && !l.zaklecie.equals(ksiega.w(p.getInventory().getItemInMainHand()))) ||
        (l.lewa && !l.zaklecie.equals(ksiega.w(p.getInventory().getItemInOffHand())))) {
      usun(u, l); return;
    }
    String reka = l.obie() ? "Obie" : l.prawa ? "P" : "L";
    boolean trzymaL = l.ppm && czas - l.ostatniPpm <= tolerancja;
    long czasTrzymania = l.ppm ? l.ostatniPpm : czas;
    if (!l.gotowe && czasTrzymania - l.start >= l.zaklecie.czasLadowania()) {
      l.gotowe = true;
      skille.cast(p, skill(l.zaklecie, "Gotowe", reka));
    }
    if ((!l.ppm && l.gotowe) || (l.ppm && !trzymaL)) {
      if (l.gotowe) wystrzel(p, l, reka);
      usun(u, l); return;
    }
    if (!l.gotowe && (czas - l.start) % 3 == 0) skille.cast(p, skill(l.zaklecie, "Laduj", reka));
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
    if (!efekty.przygotuj(p, z, l.obie())) { p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.6f); return; }   // brak celu/miejsca: bez kosztu
    double koszt = z.mana() * (l.obie() ? z.manaOburacz() : 1);
    org.bukkit.Bukkit.getLogger().info("[Zaklecia] [wystrzal] t=" + czas + " " + p.getName() + " " + z.id() + " " + reka + " koszt=" + koszt);
    PlayerData dane = PlayerData.get(p);
    if (dane.getMana() < koszt) { skille.cast(p, ksiega.skBrakMany); return; }
    dane.giveMana(-koszt, ResourceUpdateReason.SKILL);
    if (z.typ().equals("bron") || z.typ().equals("przywolanie") || z.typ().equals("na_cel")
        || z.typ().equals("ladowany") && !z.cel().equals("stozek"))
      efekty.wystrzel(p, z, l.prawa, l.obie(), reka);
    else if (miejsce == null) skille.cast(p, skill(z, "Wystrzal", reka));
    else {
      skille.punkt(p, skill(z, "Wystrzal", reka), miejsce);
      postaw(p, z, miejsce, l.obie());
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
