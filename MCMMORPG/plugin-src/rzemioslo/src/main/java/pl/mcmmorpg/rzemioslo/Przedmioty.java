package pl.mcmmorpg.rzemioslo;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

/** MMOItemy po id: rozpoznanie, wydanie, liczenie, zabranie; narzedzia profesji i ich wytrzymalosc. */
final class Przedmioty {
    private final Rzemioslo plugin;

    Przedmioty(Rzemioslo plugin) { this.plugin = plugin; }

    static String id(ItemStack it) {
        if (it == null || it.getType().isAir()) return null;
        try {
            NBTItem n = NBTItem.get(it);
            if (!n.hasType()) return null;
            String id = n.getString("MMOITEMS_ITEM_ID");
            return id == null || id.isEmpty() ? null : id;
        } catch (Throwable t) {
            return null;
        }
    }

    ItemStack zrob(String id, int ilosc) {
        String typ = plugin.dane().typy.getOrDefault(id, "MATERIAL");
        Type t = Type.get(typ);
        if (t == null) return null;
        ItemStack it = MMOItems.plugin.getItem(t, id);
        if (it == null) return null;
        it.setAmount(Math.max(1, ilosc));
        return it;
    }

    /** Wydaje przedmiot; nadmiar (pelny plecak) upuszcza pod nogi. Zwraca false, gdy przedmiot nie istnieje. */
    boolean daj(Player p, String id, int ilosc) {
        ItemStack it = zrob(id, 1);
        if (it == null) {
            plugin.getLogger().warning("Nie moge zbudowac przedmiotu " + id);
            return false;
        }
        int max = Math.max(1, it.getMaxStackSize());
        int reszta = ilosc;
        while (reszta > 0) {
            ItemStack s = it.clone();
            s.setAmount(Math.min(max, reszta));
            reszta -= s.getAmount();
            for (ItemStack nad : p.getInventory().addItem(s).values()) p.getWorld().dropItemNaturally(p.getLocation(), nad);
        }
        return true;
    }

    static int ile(Player p, String id) {
        int n = 0;
        for (ItemStack it : p.getInventory().getStorageContents()) if (id.equals(id(it))) n += it.getAmount();
        return n;
    }

    static boolean zabierz(Player p, String id, int ilosc) {
        if (ile(p, id) < ilosc) return false;
        PlayerInventory inv = p.getInventory();
        ItemStack[] c = inv.getStorageContents();
        int reszta = ilosc;
        for (int i = 0; i < c.length && reszta > 0; i++) {
            if (!id.equals(id(c[i]))) continue;
            int z = Math.min(reszta, c[i].getAmount());
            c[i].setAmount(c[i].getAmount() - z);
            if (c[i].getAmount() <= 0) c[i] = null;
            reszta -= z;
        }
        inv.setStorageContents(c);
        return true;
    }

    // ------------------------------------------------------------------ narzedzia
    record Znalezione(int slot, ItemStack item, Dane.Narz narz) {}

    static boolean zepsute(ItemStack it) {
        ItemMeta m = it.getItemMeta();
        if (!(m instanceof Damageable d) || !d.hasMaxDamage()) return false;
        return d.getDamage() >= d.getMaxDamage() - 1;
    }

    static int pozostalo(ItemStack it) {
        ItemMeta m = it.getItemMeta();
        if (!(m instanceof Damageable d) || !d.hasMaxDamage()) return -1;
        return d.getMaxDamage() - 1 - d.getDamage();
    }

    /** Najlepsze sprawne narzedzie danego rodzaju o tierze >= minTier (plecak, pasek, druga reka). */
    Znalezione najlepsze(Player p, String rodzaj, int minTier) {
        Znalezione best = null;
        Znalezione zepsuteBest = null;
        ItemStack[] c = p.getInventory().getContents();
        for (int i = 0; i < c.length; i++) {
            String id = id(c[i]);
            if (id == null) continue;
            Dane.Narz n = plugin.dane().narzedzia.get(id);
            if (n == null || !n.rodzaj().equals(rodzaj) || n.tier() < minTier) continue;
            if (zepsute(c[i])) {
                if (zepsuteBest == null) zepsuteBest = new Znalezione(i, c[i], n);
                continue;
            }
            if (best == null || n.tier() > best.narz().tier()) best = new Znalezione(i, c[i], n);
        }
        return best != null ? best : zepsuteBest;
    }

    /** Najwyzszy tier narzedzia rodzaju w plecaku (0 = brak) — do komunikatow. */
    int najwyzszyTier(Player p, String rodzaj) {
        int t = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            Dane.Narz n = plugin.dane().narzedzia.get(id(it));
            if (n != null && n.rodzaj().equals(rodzaj)) t = Math.max(t, n.tier());
        }
        return t;
    }

    /** Zuzywa 1 pkt wytrzymalosci narzedzia w slocie (nigdy go nie niszczy — zepsute czeka na naprawe). */
    static void zuzyj(Player p, int slot) {
        ItemStack it = p.getInventory().getItem(slot);
        if (it == null) return;
        ItemMeta m = it.getItemMeta();
        if (!(m instanceof Damageable d) || !d.hasMaxDamage()) return;
        d.setDamage(Math.min(d.getMaxDamage() - 1, d.getDamage() + 1));
        it.setItemMeta(m);
        p.getInventory().setItem(slot, it);
    }

    static String nazwaRodzaju(String rodzaj) {
        return Map.of("kilof", "kilofa", "siekiera", "siekiery", "sierp", "sierpa", "motyka", "motyki",
                "wedka", "wędki", "noz", "noża").getOrDefault(rodzaj, rodzaj);
    }
}
