package pl.mcmmorpg.chowance.gra;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import pl.mcmmorpg.chowance.Chowance;

/**
 * Czysty widok na arenę TYLKO u walczącego gracza: bloki między kamerą a stworami i wokół stworów zamieniamy
 * u klienta na powietrze (fałszywe bloki, świat się nie zmienia), po walce odsyłamy prawdziwe. Do tego ręka:
 * w walce trzymany jest slot 9 (Dziennik), który zasłaniał prawy dolny róg kadru — klient dostaje pusty model.
 *
 * <p>{@link Arena} wybiera możliwie czyste miejsce; tu zdejmujemy to, czego nie dało się ominąć (las, jaskinia,
 * zbocze). Podłoża pod stworami nie ruszamy — stwory mają na czym stać.
 */
public final class Widok {
  private static final int LIMIT = 1800;
  private final Chowance c;
  /** Odsłonięte bloki gracza: klucz bloku → blok (do przywrócenia). */
  private final Map<UUID, Map<Long, Block>> odsloniete = new ConcurrentHashMap<>();
  private final Map<UUID, Integer> podloga = new ConcurrentHashMap<>();
  private final ItemStack pustaReka;

  public Widok(Chowance c) {
    this.c = c;
    ItemStack r = new ItemStack(Material.PAPER);
    try {
      r.setData(io.papermc.paper.datacomponent.DataComponentTypes.ITEM_MODEL, net.kyori.adventure.key.Key.key("minecraft", "air"));
      r.setData(io.papermc.paper.datacomponent.DataComponentTypes.TOOLTIP_DISPLAY,
          io.papermc.paper.datacomponent.item.TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
      // pusta nazwa: przy zmianie slotu nad paskiem migało „Paper”
      r.setData(io.papermc.paper.datacomponent.DataComponentTypes.ITEM_NAME, net.kyori.adventure.text.Component.empty());
    } catch (RuntimeException | LinkageError e) {
      c.log.warning("Widok: nie udało się złożyć pustej ręki (" + e + ")");
    }
    this.pustaReka = r;
  }

  public boolean aktywny(Player p) { return odsloniete.containsKey(p.getUniqueId()); }

  /**
   * Odsłania widok z {@code oko} na ciała. Wołać na starcie walki i po każdej zmianie ciał/kamery (dokłada
   * bloki, niczego nie przywraca w trakcie). {@code poziomPodlogi} — najniższa wysokość stóp stworów.
   */
  public void odslon(Player p, Location oko, List<? extends Entity> ciala, double poziomPodlogi) { odslon(p, oko, ciala, poziomPodlogi, true); }

  /**
   * {@code luz} — także blok wolnego miejsca wokół stworów (walka turowa: stoją w miejscu). W real-time stwory
   * biegają, a pudełka za nimi wycinały w zboczu schodkowe doły (klient QA 2026-09-30) — tam tylko linia wzroku.
   */
  public void odslon(Player p, Location oko, List<? extends Entity> ciala, double poziomPodlogi, boolean luz) {
    if (!c.ust.widokOdslaniaj || p == null || !p.isOnline() || oko == null) return;
    Map<Long, Block> juz = odsloniete.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
    int dno = podloga.merge(p.getUniqueId(), (int) Math.floor(poziomPodlogi), Math::min);
    World w = oko.getWorld();
    Map<Long, Block> nowe = new HashMap<>();
    // podłoże pod każdym stworem zostaje (stwór wyżej niż dno areny nie może zawisnąć w powietrzu)
    for (Entity e : ciala) {
      if (e == null || !e.isValid() || !e.getWorld().equals(w)) continue;
      Location st = e.getLocation();
      for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++) for (int y = dno; y < (int) Math.floor(st.getY() + 0.01); y++)
        nowe.put(klucz(st.getBlockX() + ox, y, st.getBlockZ() + oz), null);
    }
    // kula wokół oczu — siedzisko w liściach/skale nie zasłania kadru
    dodajKule(nowe, oko, 1.3, dno);
    for (Entity e : ciala) {
      if (e == null || !e.isValid() || !e.getWorld().equals(w)) continue;
      Location st = e.getLocation();
      double h = Math.max(1.0, Math.min(3.0, e.getHeight()));
      // promień wzroku w dół i w górę sylwetki, pogrubiony (±0,45) — model bywa większy niż hitbox
      for (double y : new double[] {0.3, h * 0.5, h + 0.3}) dodajOdcinek(nowe, oko, st.clone().add(0, y, 0), 0.45, dno);
      if (!luz) continue;
      // luz wokół stwora: 2 bloki w bok, wysokość sylwetki + 1
      int fy = (int) Math.floor(st.getY());
      for (int ox = -2; ox <= 2; ox++) for (int oz = -2; oz <= 2; oz++) for (int oy = 0; oy <= (int) Math.ceil(h) + 1; oy++) {
        if (Math.abs(ox) + Math.abs(oz) > 3) continue;
        dodaj(nowe, w.getBlockAt(st.getBlockX() + ox, fy + oy, st.getBlockZ() + oz), dno);
      }
    }
    Map<Location, org.bukkit.block.data.BlockData> wyslij = new HashMap<>();
    for (var en : nowe.entrySet()) {
      if (en.getValue() == null) continue; // chronione podłoże
      if (juz.size() >= LIMIT) break;
      if (juz.putIfAbsent(en.getKey(), en.getValue()) != null) continue;
      wyslij.put(en.getValue().getLocation(), POWIETRZE);
    }
    // jeden pakiet na sekcję chunka, bez kopii BlockState (wcześniej getState() na każdy blok)
    if (!wyslij.isEmpty()) p.sendMultiBlockChange(wyslij);
  }

  private static final org.bukkit.block.data.BlockData POWIETRZE = Material.AIR.createBlockData();

  /**
   * Co ~2 s tylko pusta dłoń (jeden mały pakiet): synchronizacja ekwipunku przywraca Dziennik w dłoni.
   * Bloków NIE wysyłamy okresowo — 1800 bloków co 2 s zmuszało klienta do przebudowy sekcji chunków (przycięcia
   * klatki co 2 s). Fałszywe powietrze znika u klienta tylko przy ponownym wysłaniu chunka — patrz {@link #poChunku}.
   */
  public void podtrzymaj(Player p) {
    if (odsloniete.containsKey(p.getUniqueId()) && p.isOnline()) schowajReke(p);
  }

  /** Klient dostał chunk od nowa — ponów fałszywe powietrze tylko w nim. */
  public void poChunku(Player p, int cx, int cz) {
    Map<Long, Block> juz = odsloniete.get(p.getUniqueId());
    if (juz == null || juz.isEmpty() || !p.isOnline()) return;
    Map<Location, org.bukkit.block.data.BlockData> wyslij = new HashMap<>();
    for (Block b : juz.values()) if (b.getX() >> 4 == cx && b.getZ() >> 4 == cz) wyslij.put(b.getLocation(), POWIETRZE);
    if (!wyslij.isEmpty()) p.sendMultiBlockChange(wyslij);
  }

  /** Klient widzi w dłoni pusty model (bez ręki i Dziennika); prawdziwy ekwipunek bez zmian. */
  public void schowajReke(Player p) {
    if (!c.ust.widokBezReki || p == null || !p.isOnline()) return;
    odsloniete.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
    p.sendEquipmentChange(p, EquipmentSlot.HAND, pustaReka);
  }

  /** Koniec walki: prawdziwe bloki i prawdziwa dłoń. */
  public void przywroc(Player p) {
    Map<Long, Block> juz = odsloniete.remove(p.getUniqueId());
    podloga.remove(p.getUniqueId());
    if (juz == null || !p.isOnline()) return;
    Map<Location, org.bukkit.block.data.BlockData> wyslij = new HashMap<>();
    for (Block b : juz.values()) wyslij.put(b.getLocation(), b.getBlockData());
    if (!wyslij.isEmpty()) p.sendMultiBlockChange(wyslij);
    p.updateInventory();
  }

  public void zapomnij(UUID u) { odsloniete.remove(u); podloga.remove(u); }

  private static void dodajKule(Map<Long, Block> m, Location o, double r, int dno) {
    int R = (int) Math.ceil(r);
    for (int x = -R; x <= R; x++) for (int y = -R; y <= R; y++) for (int z = -R; z <= R; z++) {
      Location l = o.clone().add(x, y, z);
      if (l.distanceSquared(o) <= r * r) dodaj(m, l, dno);
    }
  }

  private static void dodajOdcinek(Map<Long, Block> m, Location z, Location d, double gr, int dno) {
    Vector v = d.toVector().subtract(z.toVector());
    double dl = v.length();
    if (dl < 1e-3) return;
    v.multiply(1 / dl);
    for (double t = 0; t <= dl; t += 0.25) {
      Location x = z.clone().add(v.clone().multiply(t));
      for (double[] o : new double[][] {{0, 0, 0}, {gr, 0, 0}, {-gr, 0, 0}, {0, 0, gr}, {0, 0, -gr}, {0, gr, 0}, {0, -gr, 0}})
        dodaj(m, x.clone().add(o[0], o[1], o[2]), dno);
    }
  }

  private static long klucz(int x, int y, int z) {
    return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
  }

  /** Tylko bloki nad podłogą areny; ciecze zostają (odsłonięta woda wyglądałaby jak dziura w jeziorze). */
  private static void dodaj(Map<Long, Block> m, Location l, int dno) {
    if (l.getBlockY() < dno || !l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) return; // bez synchronicznego wczytania
    dodaj(m, l.getBlock(), dno);
  }

  private static void dodaj(Map<Long, Block> m, Block b, int dno) {
    if (b.getY() < dno) return;
    Material t = b.getType();
    if (t.isAir() || b.isLiquid() || t == Material.LIGHT || t == Material.BARRIER) return;
    long k = klucz(b.getX(), b.getY(), b.getZ());
    if (!m.containsKey(k)) m.put(k, b); // null = chronione podłoże
  }
}
