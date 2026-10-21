package pl.mcmmorpg.rzemioslo;

import net.Indyuce.mmoitems.api.crafting.recipe.CraftingRecipe;
import net.Indyuce.mmoitems.api.event.PlayerUseCraftingStationEvent;
import net.Indyuce.mmoitems.api.event.PlayerUseCraftingStationEvent.StationAction;
import net.Indyuce.mmoitems.api.event.item.ConsumableConsumedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/** Skaza z pomylki zielarza (konwalia zebrana jako czosnek niedzwiedzi). Przedmiot jest zwyklym czosnkiem — liczba
 *  skazonych sztuk siedzi w zielniku postaci ("_skaza"). Kara przychodzi pozniej: skup odrzuca partie, a wyrob
 *  ze stacji, do ktorego poszla, szkodzi po zjedzeniu (z opoznieniem). Przebranie lisci na suszarni zdejmuje skaze. */
final class ZSkaza implements Listener {
    static final String TYTUL_SKUPU = "䩨䩩䩪䩫";   // CoreTools Shops/skup.yml: glify tytulu po usunieciu znakow przesuniecia (U+E1xx)
    private final Rzemioslo plugin;

    ZSkaza(Rzemioslo plugin) { this.plugin = plugin; }

    private ZZielnik zn() { return plugin.zielarz().zielnik; }

    private static boolean czosnek(String id) { return id != null && id.startsWith("PROF_WILD_SPICE"); }

    /** Sztuki czosnku w plecaku; skaza nie moze byc wieksza niz to, co gracz jeszcze ma (reszte sprzedal/oddal/zgubil). */
    private int czosnku(Player p) {
        int n = 0;
        for (ItemStack it : p.getInventory().getContents()) if (it != null && czosnek(Przedmioty.id(it))) n += it.getAmount();
        return n;
    }

    private int skaza(Player p) {
        int s = zn().licznik(p, "_skaza");
        if (s <= 0) return 0;
        int ma = czosnku(p);
        if (s > ma) { zn().dolicz(p, "_skaza", ma - s, true); s = ma; }
        return s;
    }

    // ---------------------------------------------------------------- skup
    // CoreTools (InventoryManager.onInventoryClick, priorytet NORMAL, bez ignoreCancelled) sprzedaje mimo setCancelled
    // (CFR 1.4.2, 21.10). Dlatego na czas zdarzenia czosnek znika z plecaka (LOWEST) i wraca w MONITOR tego samego
    // zdarzenia: skup go nie znajduje. Dotyczy kazdego kliku w skupie (takze okna ilosci), dopoki skaza trwa.
    private final java.util.Map<java.util.UUID, java.util.Map<Integer, ItemStack>> schowane = new java.util.HashMap<>();

    private static boolean skup(InventoryClickEvent e) {
        String tytul = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        return tytul.replaceAll("[\\uE000-\\uF8FF]", "").contains(TYTUL_SKUPU);   // glify tla przeplatane znakami przesuniecia
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void naSkup(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !skup(e)) return;
        if (skaza(p) <= 0) return;
        var inv = p.getInventory();
        java.util.Map<Integer, ItemStack> m = new java.util.HashMap<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && czosnek(Przedmioty.id(it))) { m.put(i, it); inv.setItem(i, null); }
        }
        if (m.isEmpty()) return;
        schowane.put(p.getUniqueId(), m);
        e.setCancelled(true);
        if (czosnek(Przedmioty.id(e.getCurrentItem()))) {
            p.sendMessage("§6Skupujący rozciera liść i oddaje cały pęczek: §fto nie sam czosnek, coś tu pachnie gorzko. "
                    + "§7Przebierz liście na suszarni (PPM nimi w suszarnię).");
            zn().odkryj(p, "konwalia_skaza");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void poSkupie(InventoryClickEvent e) {
        var m = schowane.remove(e.getWhoClicked().getUniqueId());
        if (m == null || !(e.getWhoClicked() instanceof Player p)) return;
        var inv = p.getInventory();
        for (var en : m.entrySet()) {
            ItemStack tam = inv.getItem(en.getKey());
            if (tam == null || tam.getType().isAir()) inv.setItem(en.getKey(), en.getValue());
            else for (ItemStack r : inv.addItem(en.getValue()).values()) p.getWorld().dropItem(p.getLocation(), r);   // slot zajety w miedzyczasie
        }
    }

    // ---------------------------------------------------------------- stacje: skaza przechodzi na wyrob
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void naStacje(PlayerUseCraftingStationEvent e) {
        StationAction a = e.getInteraction();
        if (a != StationAction.INTERACT_WITH_RECIPE && a != StationAction.INSTANT_RECIPE) return;
        if (!(e.getRecipe() instanceof CraftingRecipe r)) return;
        Player p = e.getPlayer();
        int s = skaza(p);
        if (s <= 0) return;
        int przed = czosnku(p);
        String wyrob;
        int ile;
        try { wyrob = Przedmioty.id(r.getPreviewItemStack()); ile = Math.max(1, r.getOutputAmount()); }
        catch (Throwable t) { plugin.getLogger().warning("Skaza: nie znam wyniku receptury " + r.getId() + ": " + t); return; }
        if (wyrob == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {   // skladniki zabiera MMOItems po zdarzeniu
            int zuzyte = przed - czosnku(p);
            if (zuzyte <= 0) return;
            int przeszlo = Math.min(zuzyte, zn().licznik(p, "_skaza"));
            if (przeszlo <= 0) return;
            zn().dolicz(p, "_skaza", -przeszlo, true);
            zn().dolicz(p, "_skazone_" + wyrob, ile, true);
        });
    }

    // ---------------------------------------------------------------- zjedzenie/wypicie skazonego wyrobu
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void poZjedzeniu(ConsumableConsumedEvent e) {
        Player p = e.getPlayer();
        String k = "_skazone_" + e.getMMOItem().getId();
        if (Zielarz.pomiar) plugin.getLogger().info("[pomiar] zjedzenie " + p.getName() + " " + e.getMMOItem().getId() + " skazonych=" + zn().licznik(p, k));
        if (zn().licznik(p, k) <= 0) return;
        zn().dolicz(p, k, -1, true);
        long opozn = 600 + ThreadLocalRandom.current().nextInt(601);   // 30-60 s: kara przychodzi pozniej, nie przy stole
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            boolean tr = p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 0));
            boolean md = p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 240, 0));
            if (Zielarz.pomiar) plugin.getLogger().info("[pomiar] kara " + p.getName() + " trucizna=" + tr + " mdlosci=" + md + " efekty=" + p.getActivePotionEffects());
            p.sendMessage("§cŚciska cię w żołądku. W tamtym jedzeniu musiało być coś trującego. §7Może liście nie były tym, czym się zdawały?");
            zn().odkryj(p, "konwalia_skaza");
        }, opozn);
    }

}
