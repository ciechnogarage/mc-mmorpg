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
 *   <li>(v2.4: Shift = LEWA, Ctrl = PRAWA) Ctrl + kolko w slocie zaklecia prawej dloni → nastepne z kola: ulubione (bez ulubionych — wszystkie) + „pusta”.
 *       „Pusta” odklada zaklecie; slot jest zapamietany, wiec kolkiem na tym samym slocie wraca sie do zaklec.</li>
 *   <li>Shift + kolko → to samo dla lewej dloni, niezaleznie od prawej (takze pusta lewa, jesli juz miala zaklecie).</li>
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
  List<String> kolo(Player p) {
    List<String> k = lista(p);
    if (k.isEmpty()) k = new ArrayList<>(ksiega.poId.keySet());
    k.add(null);
    return k;
  }

  private static boolean pusty(ItemStack i) { return i == null || i.getType().isAir(); }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void kolko(PlayerItemHeldEvent e) {
    Player p = e.getPlayer();
    boolean shift = p.isSneaking(), ctrl = p.getCurrentInput().isSprint();
    if (shift == ctrl) return;                                // ani jedno, albo oba naraz — zwykla zmiana slotu
    int roznica = (e.getNewSlot() - e.getPreviousSlot() + 9) % 9;
    if (roznica != 1 && roznica != 8) return;               // D: klawisz 1-9 (skok) — zwykla zmiana slotu
    int krok = roznica == 1 ? 1 : -1;                        // kolko w dol = dalej
    PlayerInventory inv = p.getInventory();
    int slot = e.getPreviousSlot();
    boolean naLewej = shift;                                 // Shift + kolko = LEWA dlon, Ctrl + kolko = PRAWA
    if (naLewej) {
      ItemStack lewa = inv.getItemInOffHand();
      boolean zaklecie = ksiega.w(lewa) != null;
      // pusta lewa: przejmujemy tylko graczom, ktorzy uzywaja zaklec (bez tego skradajacy sie wojownik nie przewinie paska)
      boolean uzywaZaklec = nosnik.lewaUzywana(p) || !lista(p).isEmpty() || ksiega.w(inv.getItem(slot)) != null;
      if (!zaklecie && !(pusty(lewa) && uzywaZaklec)) return;
    } else {
      ItemStack wRece = inv.getItem(slot);
      boolean zaklecie = ksiega.w(wRece) != null;
      if (!zaklecie && !nosnik.pusta(wRece) && !(pusty(wRece) && nosnik.slotZaklec(p) == slot)) return;
    }
    e.setCancelled(true);

    Ksiega.Zaklecie teraz = ksiega.w(naLewej ? inv.getItemInOffHand() : inv.getItem(slot));
    List<String> k = kolo(p);
    int i = k.indexOf(teraz == null ? null : teraz.id());
    if (i < 0) i = k.size() - 1;                                // spoza kola → startujemy od „pustej”
    String nastepne = k.get(Math.floorMod(i + krok, k.size()));

    if (rzucanie.rzuca(p)) rzucanie.stop(p, false);
    ItemStack nowy = nastepne == null ? null : nosnik.zrob(ksiega.poId.get(nastepne));
    if (naLewej) { inv.setItemInOffHand(nowy); nosnik.ustawLewa(p, true); }
    else { inv.setItem(slot, nowy); nosnik.ustawSlot(p, slot); }
    nosnik.odswiezPusta(p);
    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, nastepne == null ? 0.9f : 1.4f);
    nosnik.komunikat(p, pasek(p, k, nastepne) + "  §8· §7" + (naLewej ? "lewa dłoń" : "prawa dłoń"));
  }

  /** C: cale kolo, wybrana pozycja wyrozniona. */
  String pasek(Player p, List<String> k, String wybrane) {
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
