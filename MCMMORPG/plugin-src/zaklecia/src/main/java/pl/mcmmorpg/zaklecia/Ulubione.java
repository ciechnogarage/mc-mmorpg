package pl.mcmmorpg.zaklecia;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

/**
 * Ulubione zaklecia jak w Skyrimie. Oznaczane w Ksiedze (Shift+LPM → {@code zaklecia ulubione <gracz> <id>}).
 * W grze (tylko KOLKO myszy, krok ±1 — klawisze 1-9 dzialaja zwyczajnie, tez z Shiftem):
 * <ul>
 *   <li>Shift + kolko w slocie zaklecia prawej dloni → nastepne z kola: ulubione (bez ulubionych — wszystkie) + „pusta”.
 *       „Pusta” odklada zaklecie; slot jest zapamietany, wiec kolkiem na tym samym slocie wraca sie do zaklec.</li>
 *   <li>Shift + Ctrl (sprint) + kolko → to samo dla lewej dloni, niezaleznie od prawej. Tak samo Shift + kolko,
 *       gdy w prawej jest cos innego niz zaklecie (np. miecz), a w lewej zaklecie.</li>
 * </ul>
 * Pasek akcji pokazuje cale kolo z zaznaczonym wyborem. Zapis: PDC gracza (wspolne dla postaci konta).
 */
final class Ulubione implements Listener {
  private final Ksiega ksiega;
  private final Nosnik nosnik;
  private final Rzucanie rzucanie;
  private final NamespacedKey klucz;

  Ulubione(Zaklecia plugin, Ksiega ksiega, Nosnik nosnik, Rzucanie rzucanie) {
    this.ksiega = ksiega; this.nosnik = nosnik; this.rzucanie = rzucanie;
    this.klucz = new NamespacedKey(plugin, "ulubione");
  }

  List<String> lista(Player p) {
    String s = p.getPersistentDataContainer().getOrDefault(klucz, PersistentDataType.STRING, "");
    List<String> w = new ArrayList<>();
    for (String id : s.split(",")) if (ksiega.poId.containsKey(id)) w.add(id);
    return w;
  }

  boolean jest(Player p, String id) { return lista(p).contains(id); }

  /** @return komunikat dla gracza */
  String przelacz(Player p, String id) {
    Ksiega.Zaklecie z = ksiega.poId.get(id);
    if (z == null) return "§cNieznane zaklęcie: " + id;
    List<String> l = lista(p);
    boolean dodane = !l.remove(id);
    if (dodane) l.add(id);
    p.getPersistentDataContainer().set(klucz, PersistentDataType.STRING, String.join(",", l));
    return dodane ? "§e★ §f" + z.nazwa() + " §7w ulubionych." : "§7☆ " + z.nazwa() + " usunięte z ulubionych.";
  }

  /** Kolo wyboru: ulubione (albo wszystkie znane), na koncu „pusta” (null). */
  private List<String> kolo(Player p) {
    List<String> k = lista(p);
    if (k.isEmpty()) k = new ArrayList<>(ksiega.poId.keySet());
    k.add(null);
    return k;
  }

  private static boolean pusty(ItemStack i) { return i == null || i.getType().isAir(); }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void kolko(PlayerItemHeldEvent e) {
    Player p = e.getPlayer();
    if (!p.isSneaking()) return;
    int roznica = (e.getNewSlot() - e.getPreviousSlot() + 9) % 9;
    if (roznica != 1 && roznica != 8) return;               // D: klawisz 1-9 (skok) — zwykla zmiana slotu
    int krok = roznica == 1 ? 1 : -1;                        // kolko w dol = dalej
    PlayerInventory inv = p.getInventory();
    int slot = e.getPreviousSlot();
    ItemStack wRece = inv.getItem(slot);
    boolean ctrl = p.getCurrentInput().isSprint();

    boolean prawaZaklecie = ksiega.w(wRece) != null;
    boolean prawaSlotZaklec = pusty(wRece) && nosnik.slotZaklec(p) == slot;   // A: odlozone kolkiem, ten sam slot
    ItemStack lewa = inv.getItemInOffHand();
    boolean lewaZaklecie = ksiega.w(lewa) != null;

    boolean naLewej;
    if (ctrl) {                                                // B: Shift + Ctrl = lewa dlon zawsze
      if (!pusty(lewa) && !lewaZaklecie) { e.setCancelled(true); nosnik.komunikat(p, "§7Lewa dłoń jest zajęta — zwolnij ją, żeby wziąć zaklęcie."); return; }
      naLewej = true;
    } else if (prawaZaklecie || prawaSlotZaklec) {
      naLewej = false;
    } else if (lewaZaklecie) {
      naLewej = true;                                          // np. miecz w prawej, zaklecie w lewej
    } else {
      return;                                                  // zwykle Shift + kolko bez zaklec — nie ruszamy
    }
    e.setCancelled(true);

    Ksiega.Zaklecie teraz = ksiega.w(naLewej ? lewa : wRece);
    List<String> k = kolo(p);
    int i = k.indexOf(teraz == null ? null : teraz.id());
    if (i < 0) i = k.size() - 1;                                // spoza kola → startujemy od „pustej”
    String nastepne = k.get(Math.floorMod(i + krok, k.size()));

    if (rzucanie.rzuca(p)) rzucanie.stop(p, false);
    ItemStack nowy = nastepne == null ? null : nosnik.zrob(ksiega.poId.get(nastepne));
    if (naLewej) inv.setItemInOffHand(nowy);
    else { inv.setItem(slot, nowy); nosnik.ustawSlot(p, slot); }
    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, nastepne == null ? 0.9f : 1.4f);
    nosnik.komunikat(p, pasek(p, k, nastepne) + "  §8· §7" + (naLewej ? "lewa dłoń" : "prawa dłoń"));
  }

  /** C: cale kolo, wybrana pozycja wyrozniona. */
  private String pasek(Player p, List<String> k, String wybrane) {
    StringBuilder sb = new StringBuilder();
    for (String id : k) {
      boolean ten = (id == null && wybrane == null) || (id != null && id.equals(wybrane));
      String nazwa = id == null ? "pusta" : (jest(p, id) ? "★" : "") + ksiega.poId.get(id).nazwa();
      if (sb.length() > 0) sb.append("  ");
      sb.append(ten ? "§f§l[" + nazwa + "]§r" : "§7" + nazwa);
    }
    return sb.toString();
  }
}
