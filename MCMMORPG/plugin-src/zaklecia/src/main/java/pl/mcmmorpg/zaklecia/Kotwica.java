package pl.mcmmorpg.zaklecia;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/** Klient widzi STRUCTURE_VOID, swiat nadal ma AIR. Wszystkie zmiany swiata i klikniecia sa na watku glownym. */
final class Kotwica implements Listener {
  private record Pozycja(UUID swiat, int x, int y, int z) {
    boolean pasuje(Vector3i b) { return b != null && x == b.getX() && y == b.getY() && z == b.getZ(); }
    Location lokacja(World w) { return new Location(w, x, y, z); }
  }

  private final Zaklecia plugin;
  private final Rzucanie rzucanie;
  /** Odczyt tylko pozycji na watku sieciowym; immutable Pozycja publikowana przez ConcurrentHashMap. */
  private final Map<UUID, Pozycja> widoczne = new ConcurrentHashMap<>();
  private final Map<UUID, Vector> poprzedni = new HashMap<>();
  /** Poprzednia kotwica zyje jeszcze ZOSTAW tickow po przestawieniu: celownik klienta jest za serwerem (wyprzedzenie, ping). */
  private final Map<UUID, Pozycja> odchodzace = new ConcurrentHashMap<>();
  private final Map<UUID, Long> odchodziDo = new HashMap<>();
  private static final int ZOSTAW = 4;
  private final PacketListenerCommon pakiety;
  private long tick;
  private int przestawienia;

  Kotwica(Zaklecia plugin, Rzucanie rzucanie) {
    this.plugin = plugin;
    this.rzucanie = rzucanie;
    // Paper po PPM w blok (i po LPM w ochronie spawnu) odsyla graczowi realny stan = AIR, co kasuje kotwice u klienta.
    // Odsiewamy takie pakiety dla pozycji kotwicy (nasze wlasne z STRUCTURE_VOID przechodza). Watek sieciowy: tylko odczyt mapy.
    pakiety = PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
      @Override public void onPacketSend(PacketSendEvent e) {
        if (e.getPacketType() != PacketType.Play.Server.BLOCK_CHANGE || e.getUser().getUUID() == null) return;
        Pozycja k = widoczne.get(e.getUser().getUUID());
        Pozycja o = odchodzace.get(e.getUser().getUUID());
        if (k == null && o == null) return;
        WrapperPlayServerBlockChange w = new WrapperPlayServerBlockChange(e);
        Vector3i b = w.getBlockPosition();
        if (((k != null && k.pasuje(b)) || (o != null && o.pasuje(b))) && w.getBlockState().getType() != StateTypes.STRUCTURE_VOID)
          e.setCancelled(true);
      }
    });
    Bukkit.getScheduler().runTaskTimer(plugin, this::odswiez, 1, 1);
  }

  private void odswiez() {
    tick++;
    if (!plugin.getConfig().getBoolean("kotwica.wlaczona", true)) {
      for (Player p : Bukkit.getOnlinePlayers()) usun(p);
      return;
    }
    double min = Math.max(0, plugin.getConfig().getDouble("kotwica.min", 2.5));
    double max = Math.max(min, plugin.getConfig().getDouble("kotwica.max", 6));
    double wyprzedzenie = plugin.getConfig().getDouble("kotwica.wyprzedzenie", 1.0);
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (p.isDead() || !rzucanie.trybLpm(p)) { usun(p); continue; }
      Location oko = p.getEyeLocation();
      Vector aktualny = oko.getDirection().normalize();
      Vector stary = poprzedni.put(p.getUniqueId(), aktualny.clone());
      Vector kierunek = aktualny.clone();
      if (stary != null && Double.isFinite(wyprzedzenie))
        kierunek.add(aktualny.clone().subtract(stary).multiply(wyprzedzenie));
      if (kierunek.lengthSquared() < 1e-8) kierunek = aktualny;
      kierunek.normalize();
      Pozycja stara = widoczne.get(p.getUniqueId());
      Pozycja nowa = znajdz(oko, kierunek, min, max, stara);
      if (tick % 20 == 0 && plugin.getConfig().getBoolean("debug-pakiety", false))
        plugin.getLogger().info("[kotwica-stan] " + p.getName() + " pitch=" + Math.round(oko.getPitch()) + " -> " + (nowa == null ? "BRAK" : nowa.x + "," + nowa.y + "," + nowa.z));
      // Najpierw mapa (filtr pakietow przepuszcza AIR tylko dla pozycji, ktora juz nie jest kotwica), potem pakiety.
      if (nowa == null) widoczne.remove(p.getUniqueId()); else widoczne.put(p.getUniqueId(), nowa);
      Long doKiedy = odchodziDo.get(p.getUniqueId());
      if (doKiedy != null && (tick >= doKiedy || odchodzace.get(p.getUniqueId()).equals(nowa))) {
        Pozycja o = odchodzace.remove(p.getUniqueId()); odchodziDo.remove(p.getUniqueId());
        if (o != null && !o.equals(nowa)) przywroc(p, o);
      }
      if (stara != null && !stara.equals(nowa)) {
        if (nowa == null) przywroc(p, stara);                     // koniec kotwicy (teren blisko, wyjscie z trybu)
        else {                                                  // przestawienie: stara zostaje jeszcze chwile
          Pozycja o = odchodzace.put(p.getUniqueId(), stara);
          if (o != null && !o.equals(stara) && !o.equals(nowa)) przywroc(p, o);
          odchodziDo.put(p.getUniqueId(), tick + ZOSTAW);
        }
      }
      if (nowa != null) {
        if (!nowa.equals(stara)) { wyslij(p, nowa); przestawienia++;
          if (plugin.getConfig().getBoolean("debug-pakiety", false)) plugin.getLogger().info("[os] t=" + tick + " KOTWICA " + nowa.x + "," + nowa.y + "," + nowa.z + " yaw=" + Math.round(oko.getYaw())); }
        else if (tick % (rzucanie.rzuca(p) ? 5 : 10) == 0) wyslij(p, nowa);   // zapas, gdyby cos nadpisalo kotwice
      }
    }
    if (tick % 20 == 0) {
      if (plugin.getConfig().getBoolean("debug-wejscie", false) && przestawienia > 0)
        plugin.getLogger().info("[kotwica] przestawienia/20t=" + przestawienia);
      przestawienia = 0;
    }
  }

  private Pozycja znajdz(Location oko, Vector kierunek, double min, double max, Pozycja stara) {
    World w = oko.getWorld();
    if (w == null || max <= min) return null;
    RayTraceResult teren = w.rayTraceBlocks(oko, kierunek, max, FluidCollisionMode.ALWAYS, false);
    double przeszkoda = teren == null || teren.getHitPosition() == null
        ? Double.POSITIVE_INFINITY : teren.getHitPosition().distance(oko.toVector());
    if (przeszkoda < min) return null;
    double granica = Math.min(max, przeszkoda);
    if (stara != null && stara.swiat.equals(w.getUID()) && wolne(w, stara)
        && przeciecie(oko.toVector(), kierunek, stara, min, granica, POL - 0.01))
      return stara;
    Pozycja najlepsza = null, zapasowa = null;
    Vector poczatek = oko.toVector();
    Pozycja poprzednia = null;
    for (double t = min; t <= granica && najlepsza == null; t += 0.125) {
      Vector punkt = poczatek.clone().add(kierunek.clone().multiply(t));
      Pozycja b = new Pozycja(w.getUID(), punkt.getBlockX(), punkt.getBlockY(), punkt.getBlockZ());
      if (b.equals(poprzednia)) continue;
      poprzednia = b;
      if (!wolne(w, b) || !przeciecie(poczatek, kierunek, b, min, granica, POL - 0.03)) continue;
      if (odSrodka(poczatek, kierunek, b) <= SRODEK) najlepsza = b;      // najblizsza dobrze wycentrowana
      else if (zapasowa == null) zapasowa = b;                           // najblizsza trafiona z zapasem 0,03
    }
    if (najlepsza == null) najlepsza = zapasowa;
    return najlepsza;
  }

  private static boolean wolne(World w, Pozycja p) {
    Block b = w.getBlockAt(p.x, p.y, p.z);
    return b.getType().isAir() && b.getType() != Material.WATER && b.getType() != Material.LAVA;
  }

  /**
   * Klient trafia tylko w szescian 6/16 w srodku bloku (polowa boku POL). Nowa kotwica: promien musi przecinac szescian
   * pomniejszony o 0,03 (zapas na roznice pozycji oka), najlepiej blisko srodka (SRODEK); obecna zostaje, poki
   * promien przecina szescian pomniejszony o 0,01.
   */
  private static final double POL = 3.0 / 16.0, SRODEK = 0.12;

  private static double odSrodka(Vector oko, Vector d, Pozycja p) {
    Vector c = new Vector(p.x + 0.5, p.y + 0.5, p.z + 0.5).subtract(oko);
    return c.clone().subtract(d.clone().multiply(c.dot(d))).length();
  }

  private static boolean przeciecie(Vector oko, Vector d, Pozycja p, double min, double max, double pol) {
    double od = min, doT = max;
    double[] o = {oko.getX(), oko.getY(), oko.getZ()}, k = {d.getX(), d.getY(), d.getZ()};
    int[] b = {p.x, p.y, p.z};
    for (int i = 0; i < 3; i++) {
      double lo = b[i] + 0.5 - pol, hi = b[i] + 0.5 + pol;
      if (Math.abs(k[i]) < 1e-9) { if (o[i] < lo || o[i] > hi) return false; continue; }
      double t1 = (lo - o[i]) / k[i], t2 = (hi - o[i]) / k[i];
      od = Math.max(od, Math.min(t1, t2)); doT = Math.min(doT, Math.max(t1, t2));
      if (od > doT) return false;
    }
    return true;
  }

  private void wyslij(Player p, Pozycja pozycja) {
    if (p.getWorld().getUID().equals(pozycja.swiat)) p.sendBlockChange(pozycja.lokacja(p.getWorld()), Material.STRUCTURE_VOID.createBlockData());
  }

  private void przywroc(Player p, Pozycja pozycja) {
    if (p.getWorld().getUID().equals(pozycja.swiat)) {
      Location l = pozycja.lokacja(p.getWorld());
      p.sendBlockChange(l, l.getBlock().getBlockData());
    }
  }

  private void usun(Player p) {
    Pozycja stara = widoczne.remove(p.getUniqueId());
    Pozycja o = odchodzace.remove(p.getUniqueId()); odchodziDo.remove(p.getUniqueId());
    poprzedni.remove(p.getUniqueId());
    if (stara != null) przywroc(p, stara);
    if (o != null) przywroc(p, o);
  }

  /**
   * Paper po kliknieciu bloku odsyla graczowi jego realny stan (PPM zawsze — cel i sasiad; LPM w ochronie spawnu),
   * co kasuje kotwice u klienta. Odsylamy ja w nastepnym ticku, zanim LPM przekroczy tolerancje.
   */
  @EventHandler(priority = EventPriority.MONITOR)
  public void klik(org.bukkit.event.player.PlayerInteractEvent e) {
    if (plugin.getConfig().getBoolean("debug-pakiety", false))
      plugin.getLogger().info("[os] t=" + tick + " " + e.getAction() + " " + e.getHand() + " " + (e.getClickedBlock() == null ? "-" : e.getClickedBlock().getX() + "," + e.getClickedBlock().getY() + "," + e.getClickedBlock().getZ() + " " + e.getBlockFace()));
    if (e.getClickedBlock() == null) return;
    Player p = e.getPlayer();
    Pozycja k = widoczne.get(p.getUniqueId());
    if (k == null) return;
    Block b = e.getClickedBlock(), obok = b.getRelative(e.getBlockFace());
    boolean dotyka = (b.getX() == k.x && b.getY() == k.y && b.getZ() == k.z)
        || (obok.getX() == k.x && obok.getY() == k.y && obok.getZ() == k.z);
    if (!dotyka) return;
    Bukkit.getScheduler().runTask(plugin, () -> {
      Pozycja teraz = widoczne.get(p.getUniqueId());
      if (teraz != null && p.isOnline()) wyslij(p, teraz);
    });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void teleport(PlayerTeleportEvent e) { usun(e.getPlayer()); }

  @EventHandler public void swiat(PlayerChangedWorldEvent e) { usun(e.getPlayer()); }
  @EventHandler public void wyjscie(PlayerQuitEvent e) { usun(e.getPlayer()); }
  @EventHandler public void smierc(PlayerDeathEvent e) { usun(e.getEntity()); }

  void wylacz() {
    PacketEvents.getAPI().getEventManager().unregisterListener(pakiety);
    for (Player p : Bukkit.getOnlinePlayers()) usun(p);
    widoczne.clear();
    odchodzace.clear(); odchodziDo.clear();
    poprzedni.clear();
  }
}
