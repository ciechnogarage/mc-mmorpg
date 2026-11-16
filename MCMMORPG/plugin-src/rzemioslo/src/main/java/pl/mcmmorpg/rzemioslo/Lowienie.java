package pl.mcmmorpg.rzemioslo;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Lowienie v2: woda = lowisko T1; w promieniu R od znacznika Nexo 'lowisko_tN' — tier N. Wedka profesji
 *  tieru >= tier lowiska jest wymagana; jej tier skraca czas brania. Wedka vanilla dziala po staremu (bez surowcow i XP). */
final class Lowienie implements Listener {
    private static final int R = 6;
    private final Rzemioslo plugin;
    private final Map<UUID, Long> podpowiedz = new HashMap<>();

    Lowienie(Rzemioslo plugin) { this.plugin = plugin; }

    @EventHandler
    public void naWyjscie(PlayerQuitEvent e) { podpowiedz.remove(e.getPlayer().getUniqueId()); }

    private record Wedka(int slot, ItemStack item, Dane.Narz narz) {}

    private Wedka wedka(Player p) {
        int held = p.getInventory().getHeldItemSlot();
        ItemStack it = p.getInventory().getItemInMainHand();
        Dane.Narz n = plugin.dane().narzedzia.get(Przedmioty.id(it));
        if (n != null && n.rodzaj().equals("wedka")) return new Wedka(held, it, n);
        it = p.getInventory().getItemInOffHand();
        n = plugin.dane().narzedzia.get(Przedmioty.id(it));
        if (n != null && n.rodzaj().equals("wedka")) return new Wedka(40, it, n);
        return null;
    }

    int tierWody(Location l) {
        int best = 1;
        for (int x = -R; x <= R; x++) for (int y = -R; y <= R; y++) for (int z = -R; z <= R; z++) {
            Block b = l.getWorld().getBlockAt(l.getBlockX() + x, l.getBlockY() + y, l.getBlockZ() + z);
            if (b.getType() != org.bukkit.Material.NOTE_BLOCK) continue;
            Integer t = plugin.dane().lowiskoBloki.get(Zbieranie.nexoId(b));
            if (t != null && t > best) best = t;
        }
        return best;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void naLowienie(PlayerFishEvent e) {
        Player p = e.getPlayer();
        Wedka w = wedka(p);
        if (e.getState() == PlayerFishEvent.State.FISHING) {
            if (w == null) {
                long teraz = System.currentTimeMillis();
                if (teraz - podpowiedz.getOrDefault(p.getUniqueId(), 0L) > 600_000) {
                    podpowiedz.put(p.getUniqueId(), teraz);
                    plugin.pasek(p, plugin.kom("wedka-vanilla"));
                }
                return;
            }
            if (Przedmioty.zepsute(w.item())) {
                plugin.pasek(p, plugin.kom("zepsute", "{nazwa}", plugin.dane().nazwa(w.narz().id())));
                e.setCancelled(true);
                return;
            }
            FishHook h = e.getHook();
            double k = Math.max(0.35, 1 - 0.07 * (w.narz().tier() - 1));
            h.setWaitTime((int) (100 * k), (int) (400 * k));
            return;
        }
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH || w == null || !(e.getCaught() instanceof Item zlowione)) return;
        Dane d = plugin.dane();
        int tier = tierWody(e.getHook().getLocation());
        if (w.narz().tier() < tier) {
            zlowione.remove();
            plugin.pasek(p, plugin.kom("slaba-wedka", "{tier}", d.rzymska(tier), "{mam}", d.rzymska(w.narz().tier())));
            return;
        }
        if (Profesje.poziom(p, "fishing") < d.start(tier)) {
            zlowione.remove();
            plugin.pasek(p, plugin.kom("za-niski-poziom", "{profesja}", d.nazwyProfesji.get("fishing"),
                    "{poziom}", String.valueOf(d.start(tier)), "{nazwa}", "Łowisko " + d.rzymska(tier)));
            return;
        }
        Dane.Lowisko l = d.lowiska.get(tier);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String id = l.glowny();
        for (Map.Entry<String, Double> rz : l.rzadkie()) {
            if (r.nextDouble() < rz.getValue()) { id = rz.getKey(); break; }
        }
        if (id.equals(l.glowny()) && r.nextDouble() < l.szansaPoboczny()) id = l.poboczny();
        int ile = plugin.profesje().podwojny(p, "fishing") ? 2 : 1;
        ItemStack lup = plugin.przedmioty().zrob(id, ile);
        if (lup == null) { zlowione.remove(); return; }
        zlowione.setItemStack(lup);
        e.setExpToDrop(0);
        double xp = plugin.profesje().daj(p, "fishing", tier, d.g(tier), p.getLocation());
        Przedmioty.zuzyj(p, w.slot());
        plugin.zlecenia().zebrano(p, "fishing", id, ile);
        p.playSound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 0.8f, 1.1f);
        plugin.pasek(p, plugin.kom("zebrano", "{lup}", ile + "× " + d.nazwa(id), "{xp}", String.valueOf(Math.round(xp)))
                + (ile > 1 ? plugin.kom("perk-podwojny") : ""));
    }
}
