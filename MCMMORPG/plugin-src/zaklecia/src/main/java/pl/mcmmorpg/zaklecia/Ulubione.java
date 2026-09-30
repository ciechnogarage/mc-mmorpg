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
 * Ulubione zaklecia jak w Skyrimie. Oznaczane w Ksiedze (Shift+LPM → {@code zaklecia ulubione <gracz> <id>}),
 * wybierane w grze: Shift + kolko myszy, gdy zaklecie jest w dloni, przelacza je po ulubionych (bez ulubionych —
 * po wszystkich znanych). Prawa dlon, a gdy w prawej nie ma zaklecia (np. miecz) — lewa.
 *
 * Zapis: PDC gracza (klucz zaklecia:ulubione, lista id po przecinku) — wspolne dla postaci jednego konta.
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

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void kolko(PlayerItemHeldEvent e) {
    Player p = e.getPlayer();
    if (!p.isSneaking()) return;
    PlayerInventory inv = p.getInventory();
    ItemStack wRece = inv.getItem(e.getPreviousSlot());
    boolean prawa = ksiega.w(wRece) != null;
    if (!prawa && ksiega.w(inv.getItemInOffHand()) == null) return;   // zwykle Shift+kolko bez zaklecia — nie ruszamy
    e.setCancelled(true);

    Ksiega.Zaklecie teraz = ksiega.w(prawa ? wRece : inv.getItemInOffHand());
    List<String> kolejka = lista(p);
    if (kolejka.isEmpty()) kolejka = new ArrayList<>(ksiega.poId.keySet());
    if (kolejka.size() < 2 && kolejka.contains(teraz.id())) {
      nosnik.komunikat(p, "§7Dodaj więcej zaklęć do ulubionych w §fKsiędze zaklęć §7(Shift+LPM).");
      return;
    }
    int krok = ((e.getNewSlot() - e.getPreviousSlot() + 9) % 9) <= 4 ? 1 : -1;   // kolko w dol = dalej
    int i = kolejka.indexOf(teraz.id());
    String nastepne = kolejka.get(Math.floorMod((i < 0 ? (krok > 0 ? -1 : 0) : i) + krok, kolejka.size()));

    if (rzucanie.rzuca(p)) rzucanie.stop(p, false);
    Ksiega.Zaklecie z = ksiega.poId.get(nastepne);
    if (prawa) inv.setItem(e.getPreviousSlot(), nosnik.zrob(z));
    else inv.setItemInOffHand(nosnik.zrob(z));
    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.4f);
    nosnik.komunikat(p, "§8◀ " + (jest(p, z.id()) ? "§e★ " : "") + "§f" + z.nazwa() + " §8▶  §7" + (prawa ? "prawa dłoń" : "lewa dłoń"));
  }
}
