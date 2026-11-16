package pl.mcmmorpg.zaklecia;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

/** Lista ulubionych z Ksiegi; wybor zaklec odbywa sie w pasku Shift+Q. */
final class Ulubione {
  private final Ksiega ksiega;
  private final NamespacedKey klucz;

  Ulubione(Zaklecia plugin, Ksiega ksiega) {
    this.ksiega = ksiega;
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

  /** Kolo wyboru: wszystkie zaklecia Ksiegi — najpierw ulubione (w kolejnosci dodania), potem reszta; na koncu „pusta” (null). */
  List<String> kolo(Player p) {
    List<String> k = lista(p);
    for (String id : ksiega.poId.keySet()) if (!k.contains(id)) k.add(id);
    k.add(null);
    return k;
  }

}
