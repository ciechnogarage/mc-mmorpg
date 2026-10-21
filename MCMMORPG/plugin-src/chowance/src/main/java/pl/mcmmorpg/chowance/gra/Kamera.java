package pl.mcmmorpg.chowance.gra;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import pl.mcmmorpg.chowance.Chowance;

/**
 * „Miejsce trenera” na czas walki: gracz siedzi na niewidzialnym siedzisku przy arenie (nie zejdzie, nie odejdzie,
 * może się rozglądać) i ZOSTAJE w swoim trybie gry — klawisze 1–8 docierają do serwera.
 *
 * <p>Wcześniej: tryb widza przypięty do statywu. Zablokowany widok był ładny, ale w trybie widza klient nie wysyła
 * zmiany slotu (1–9 obsługuje lokalne menu widza), więc trener nie mógł wydawać rozkazów (prośba właściciela
 * 2026-09-27: „wymuszać akcje podczas walki”). Pozycja powrotu w PDC na wypadek crasha.
 */
public final class Kamera implements Listener {
  private final Chowance c;
  private final NamespacedKey klucz;
  private final Map<UUID, BlockDisplay> siedziska = new ConcurrentHashMap<>();
  private final Map<UUID, Location> powroty = new ConcurrentHashMap<>();
  /** Gracze, którym widzenie w ciemności nadała kamera (tylko tym je zdejmujemy). */
  private final java.util.Set<UUID> noktowizja = ConcurrentHashMap.newKeySet();
  /** Zejście z siedziska wykonywane przez nas (koniec walki) — nie blokować. */
  private final java.util.Set<UUID> zsiadam = ConcurrentHashMap.newKeySet();
  /** Teleport gracza wykonywany przez nas w trakcie walki — inne teleporty (także z przyczyną PLUGIN) kończą siedzenie. */
  private final java.util.Set<UUID> wlasnyTeleport = ConcurrentHashMap.newKeySet();

  /** Czysty kadr u walczącego: fałszywe powietrze na przeszkodach + pusta dłoń. */
  public final Widok widok;

  public Kamera(Chowance c) {
    this.c = c;
    this.widok = new Widok(c);
    this.klucz = new NamespacedKey(c.plugin, "kamera_powrot");
  }

  public boolean aktywna(Player p) { return siedziska.containsKey(p.getUniqueId()); }

  /** Gracze siedzący teraz w kamerze walki. */
  public java.util.Collection<Player> aktywni() {
    java.util.List<Player> l = new java.util.ArrayList<>(siedziska.size());
    for (UUID u : siedziska.keySet()) { Player p = Bukkit.getPlayer(u); if (p != null) l.add(p); }
    return l;
  }

  /** Sadza gracza w {@code oko} (poziom oczu), twarzą do {@code cel}. False, gdy wyłączone w configu. */
  public boolean wlacz(Player p, Location oko, Location cel) {
    if (!c.ust.kamera || p.isDead() || !p.isOnline()) return false;
    if (aktywna(p)) { przesun(p, oko, cel); return true; }
    Location powrot = p.getLocation().clone();
    powroty.put(p.getUniqueId(), powrot);
    p.getPersistentDataContainer().set(klucz, PersistentDataType.STRING, zapis(powrot));
    if (p.isInsideVehicle()) p.leaveVehicle();
    Location l = ustawienie(wolneOko(oko, cel, p), cel, p);
    BlockDisplay s = l.getWorld().spawn(l, BlockDisplay.class, d -> { d.setPersistent(false); d.setTeleportDuration(3); });
    siedziska.put(p.getUniqueId(), s);
    tp(p, l);
    s.addPassenger(p);
    // Arena bywa w jaskini albo w nocy (world1: kieszeń skalna przy spawnie) — na czas walki widać stwory.
    if (!p.hasPotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION)) {
      p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, org.bukkit.potion.PotionEffect.INFINITE_DURATION, 0, false, false, false));
      noktowizja.add(p.getUniqueId());
    }
    widok.schowajReke(p);
    return true;
  }

  /**
   * Zdejmuje u gracza bloki między jego okiem a ciałami (i wokół ciał). Wołać po postawieniu/zmianie ciał
   * i po przesadzeniu kamery — odsłonięte bloki się sumują, wracają dopiero przy {@link #wylacz}.
   */
  public void odslon(Player p, java.util.List<? extends org.bukkit.entity.Entity> ciala) { odslon(p, ciala, true); }

  public void odslon(Player p, java.util.List<? extends org.bukkit.entity.Entity> ciala, boolean luz) {
    if (!aktywna(p)) return;
    double dno = Double.MAX_VALUE;
    for (var e : ciala) if (e != null && e.isValid()) dno = Math.min(dno, e.getLocation().getY());
    if (dno == Double.MAX_VALUE) return;
    widok.odslon(p, p.getEyeLocation(), ciala, dno, luz);
  }

  /** Co 2 s: fałszywe bloki i pusta dłoń od nowa (aktualizacje chunków/ekwipunku je nadpisują). */
  public void podtrzymaj() {
    for (UUID u : siedziska.keySet()) {
      Player p = Bukkit.getPlayer(u);
      if (p != null) widok.podtrzymaj(p);
    }
  }

  /**
   * Wolne miejsce na oczy i ciało siedzącego gracza: najpierw wymarzone ujęcie, potem punkty na linii w stronę
   * areny i niżej/wyżej, w końcu tam, gdzie gracz stoi. world1 2026-09-27: spawn to kamienna kieszeń — siedzisko
   * lądowało w skale i właściciel udusił się w ścianie (na płaskim klonie nie było tego widać).
   */
  public static Location wolneOko(Location oko, Location cel, Player p) {
    Vector ku = cel.toVector().subtract(oko.toVector());
    for (double f = 0; f <= 0.75; f += 0.125) {
      for (double dy : new double[] {0, -0.8, 0.8, -1.6}) {
        Location l = oko.clone().add(ku.clone().multiply(f)).add(0, dy, 0);
        if (wolne(l)) return l;
      }
    }
    return p.getEyeLocation();
  }

  /** Oczy, ciało i przestrzeń nad głową bez bloków stałych. */
  public static boolean wolne(Location oko) {
    return oko.getBlock().isPassable() && oko.clone().add(0, -1, 0).getBlock().isPassable() && oko.clone().add(0, 0.5, 0).getBlock().isPassable();
  }

  /** Siedzisko pod oczami gracza: oko minus wysokość oczu. */
  private static Location ustawienie(Location oko, Location cel, Player p) {
    Location l = oko.clone();
    Vector d = cel.toVector().subtract(oko.toVector());
    if (d.lengthSquared() > 1e-4) l.setDirection(d);
    return l.subtract(0, p.getEyeHeight(), 0);
  }

  /**
   * Przesadza gracza (np. gdy stwory odbiegły daleko od trybun): zsadzenie, przeniesienie, posadzenie.
   * Teleport z pasażerem/na pojeździe zsadza w Paperze 1.21.11 (flagi RETAIN_* są przestarzałe i nie działają).
   */
  public void przesun(Player p, Location oko, Location cel) {
    BlockDisplay s = siedziska.get(p.getUniqueId());
    if (s == null || !s.isValid()) return;
    Location l = ustawienie(wolneOko(oko, cel, p), cel, p);
    l.setYaw(p.getLocation().getYaw());
    l.setPitch(p.getLocation().getPitch());
    zsiadam.add(p.getUniqueId());
    try { s.removePassenger(p); } finally { zsiadam.remove(p.getUniqueId()); }
    s.teleport(l);
    tp(p, l);
    s.addPassenger(p);
  }

  private void tp(Player p, Location l) {
    wlasnyTeleport.add(p.getUniqueId());
    try { p.teleport(l); } finally { wlasnyTeleport.remove(p.getUniqueId()); }
  }

  /**
   * Obraca widok gracza w stronę celu (np. na starcie tury) — sam obrót (pakiet look_at), bez teleportu:
   * teleport zsadzał gracza z siedziska (eval 2026-09-27: po pierwszej turze gracz już nie siedział).
   */
  public void spojrz(Player p, Location cel) {
    if (!aktywna(p)) return;
    p.lookAt(cel.getX(), cel.getY(), cel.getZ(), io.papermc.paper.entity.LookAnchor.EYES);
  }

  public void wylacz(Player p) {
    BlockDisplay s = siedziska.remove(p.getUniqueId());
    Location powrot = powroty.remove(p.getUniqueId());
    if (s == null) return;
    zsiadam.add(p.getUniqueId());
    try {
      s.removePassenger(p);
    } finally {
      zsiadam.remove(p.getUniqueId());
    }
    s.remove();
    widok.przywroc(p);
    if (noktowizja.remove(p.getUniqueId())) p.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
    p.getPersistentDataContainer().remove(klucz);
    if (powrot != null && powrot.getWorld() != null && p.isOnline()) p.teleport(powrot);
  }

  public void wylaczWszystkich() {
    for (UUID u : List.copyOf(siedziska.keySet())) {
      Player p = Bukkit.getPlayer(u);
      if (p != null) wylacz(p);
      else { BlockDisplay s = siedziska.remove(u); powroty.remove(u); widok.zapomnij(u); if (s != null) s.remove(); }
    }
  }

  private static String zapis(Location l) {
    return l.getWorld().getName() + ";" + l.getX() + ";" + l.getY() + ";" + l.getZ() + ";" + l.getYaw() + ";" + l.getPitch();
  }

  /** Po crashu gracz stałby w miejscu siedziska — wraca tam, skąd zaczął walkę. */
  @EventHandler(priority = EventPriority.LOWEST)
  public void wejscie(PlayerJoinEvent e) {
    Player p = e.getPlayer();
    String z = p.getPersistentDataContainer().get(klucz, PersistentDataType.STRING);
    if (z == null) return;
    p.getPersistentDataContainer().remove(klucz);
    var nv = p.getPotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
    if (nv != null && nv.isInfinite()) p.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION); // z przerwanej walki
    try {
      String[] a = z.split(";");
      World w = Bukkit.getWorld(a[0]);
      if (w != null) p.teleport(new Location(w, Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]), Float.parseFloat(a[4]), Float.parseFloat(a[5])));
      c.log.info("Kamera: " + p.getName() + " wraca na miejsce sprzed przerwanej walki");
    } catch (RuntimeException ex) {
      c.log.warning("Kamera: zły zapis powrotu " + p.getName() + " (" + z + ")");
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void wyjscie(PlayerQuitEvent e) { wylacz(e.getPlayer()); }

  /** Chunk wysłany od nowa kasuje u klienta fałszywe powietrze — ponawiamy je tick później, tylko w tym chunku. */
  @EventHandler
  public void chunk(io.papermc.paper.event.packet.PlayerChunkLoadEvent e) {
    Player p = e.getPlayer();
    if (!aktywna(p)) return;
    int cx = e.getChunk().getX(), cz = e.getChunk().getZ();
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> widok.poChunku(p, cx, cz), 1L);
  }

  /** Shift nie zsadza z miejsca trenera. */
  @EventHandler(ignoreCancelled = true)
  public void zsiadanie(EntityDismountEvent e) {
    if (e.getEntity() instanceof Player p && aktywna(p) && !zsiadam.contains(p.getUniqueId())) e.setCancelled(true);
  }

  /** Teleport z zewnątrz (komenda, inny plugin) w trakcie walki kończy siedzenie bez przywracania pozycji. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void teleport(PlayerTeleportEvent e) {
    Player p = e.getPlayer();
    if (!aktywna(p) || wlasnyTeleport.contains(p.getUniqueId()) || e.getCause() == PlayerTeleportEvent.TeleportCause.DISMOUNT) return;
    BlockDisplay s = siedziska.remove(p.getUniqueId());
    powroty.remove(p.getUniqueId());
    if (s != null) s.remove();
    widok.przywroc(p);
    p.getPersistentDataContainer().remove(klucz);
  }

  /** Trener na siedzisku nie może się bronić ani odsunąć — bez obrażeń do końca walki (poza pustką). */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void ochrona(org.bukkit.event.entity.EntityDamageEvent e) {
    if (e.getEntity() instanceof Player p && aktywna(p) && e.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID) e.setCancelled(true);
  }

  /** F (castowanie skilli MMOCore) zablokowane w walce — klawisze 1–8 należą do walki chowańców. */
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void castowanie(PlayerSwapHandItemsEvent e) {
    if (aktywna(e.getPlayer())) e.setCancelled(true);
  }
}
