package pl.mcmmorpg.rzemioslo;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Zbieranie z narzedziem: wezly (bloki Nexo) i — przez ten sam silnik — oprawianie zwlok.
 *  Narzedzie NIE musi byc w rece: plugin bierze najlepsze z plecaka i pokazuje je w dloni tylko pakietem. */
final class Zbieranie implements Listener {
    /** Cel sesji: skad (blok albo zwloki), co daje, ile trwa. */
    interface Cel {
        Location miejsce();
        String profesja();
        int tier();
        String rodzaj();
        double czasBazowy();
        Dane.Zrodlo nagroda();
        String nazwa();
        Block blok();                 // null dla zwlok
        boolean aktualny();           // czy cel nadal istnieje
        void poSukcesie(Player p);    // np. wyczerpanie wezla / usuniecie zwlok
        Particle.DustOptions kurz();  // null = czastki pekajacego bloku
    }

    private final class Sesja {
        final Player p;
        final Cel cel;
        final Przedmioty.Znalezione narz;
        final Location start;
        final int ticki;
        final int heldSlot;
        int t = 0;
        BukkitTask zadanie;

        Sesja(Player p, Cel cel, Przedmioty.Znalezione narz, int ticki) {
            this.p = p;
            this.cel = cel;
            this.narz = narz;
            this.start = p.getLocation();
            this.ticki = ticki;
            this.heldSlot = p.getInventory().getHeldItemSlot();
        }
    }

    private final Rzemioslo plugin;
    private final Map<UUID, Sesja> sesje = new HashMap<>();
    /** gracz -> blok -> koniec wyczerpania (ms) */
    private final Map<UUID, Map<Location, Long>> wyczerpane = new HashMap<>();
    private final Map<UUID, Long> ostatniKomunikat = new HashMap<>();

    Zbieranie(Rzemioslo plugin) { this.plugin = plugin; }

    // ================================================================ wezly: wejscie
    static String nexoId(Block b) {
        try {
            CustomBlockMechanic m = NexoBlocks.customBlockMechanic(b);
            return m == null ? null : m.getItemID();
        } catch (Throwable t) {
            return null;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void naKlik(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        String nid = nexoId(b);
        if (nid == null) return;
        Dane.Zrodlo w = plugin.dane().wezly.get(nid);
        if (w == null) return;
        // blok wezla nigdy nie reaguje jak blok (nuta, postawienie na nim)
        e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        Player p = e.getPlayer();
        // PPM z jedzeniem/mikstura w rece = spozycie, nie zbieranie (gracz przy zlozu musi moc wypic miksture)
        if (e.getItem() != null && (e.getItem().getType().isEdible() || plugin.dane().buffy.containsKey(Przedmioty.id(e.getItem()))
                || e.getItem().getType() == org.bukkit.Material.POTION)) return;
        Long konc = wyczerpane.getOrDefault(p.getUniqueId(), Map.of()).get(b.getLocation());
        boolean wyczerp = konc != null && konc > System.currentTimeMillis();
        // inna akcja PPM (zaklecie, garda, postawienie bloku) nie odpala na wezle — tez na wyczerpanym
        e.setCancelled(true);
        e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (sesje.containsKey(p.getUniqueId())) return;
        if (wyczerp) {
            pasek(p, plugin.kom("wyczerpane", "{s}", String.valueOf((konc - System.currentTimeMillis()) / 1000 + 1)));
            return;
        }
        rozpocznij(p, new CelWezla(b, w));
    }

    private final class CelWezla implements Cel {
        final Block b;
        final Dane.Zrodlo w;

        CelWezla(Block b, Dane.Zrodlo w) { this.b = b; this.w = w; }

        public Location miejsce() { return b.getLocation().add(0.5, 0.5, 0.5); }
        public String profesja() { return w.profesja(); }
        public int tier() { return w.tier(); }
        public String rodzaj() { return plugin.dane().narzedzieProfesji.get(w.profesja()); }
        public double czasBazowy() { return plugin.dane().czasBazowy; }
        public Dane.Zrodlo nagroda() { return w; }
        public String nazwa() { return w.nazwa(); }
        public Block blok() { return b; }
        public boolean aktualny() { return w == plugin.dane().wezly.get(nexoId(b)); }
        public void poSukcesie(Player p) { wyczerp(p, b, w); }
        public Particle.DustOptions kurz() { return null; }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void naUderzenie(BlockDamageEvent e) {
        if (plugin.dane().wezly.containsKey(nexoId(e.getBlock())) && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            Player p = e.getPlayer();
            long teraz = System.currentTimeMillis();
            if (teraz - ostatniKomunikat.getOrDefault(p.getUniqueId(), 0L) > 3000) {
                ostatniKomunikat.put(p.getUniqueId(), teraz);
                pasek(p, plugin.kom("ppm-wezel"));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void naZniszczenie(BlockBreakEvent e) {
        if (plugin.dane().wezly.containsKey(nexoId(e.getBlock())) && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
        }
    }

    // ================================================================ sesja
    /** Zwraca true, gdy sesja ruszyla. */
    boolean rozpocznij(Player p, Cel cel) {
        Dane d = plugin.dane();
        String prof = cel.profesja();
        int start = d.start(cel.tier());
        int lvl = Profesje.poziom(p, prof);
        if (lvl < start) {
            pasek(p, plugin.kom("za-niski-poziom", "{profesja}", d.nazwyProfesji.getOrDefault(prof, prof),
                    "{poziom}", String.valueOf(start), "{nazwa}", cel.nazwa()));
            return false;
        }
        String rodzaj = cel.rodzaj();
        Przedmioty.Znalezione n = plugin.przedmioty().najlepsze(p, rodzaj, cel.tier());
        if (n == null) {
            int mam = plugin.przedmioty().najwyzszyTier(p, rodzaj);
            pasek(p, mam == 0
                    ? plugin.kom("brak-narzedzia", "{narzedzie}", Przedmioty.nazwaRodzaju(rodzaj), "{tier}", d.rzymska(cel.tier()))
                    : plugin.kom("slabe-narzedzie", "{narzedzie}", Przedmioty.nazwaRodzaju(rodzaj), "{tier}", d.rzymska(cel.tier()),
                    "{mam}", d.rzymska(mam)));
            return false;
        }
        if (Przedmioty.zepsute(n.item())) {
            pasek(p, plugin.kom("zepsute", "{nazwa}", d.nazwa(n.narz().id())));
            return false;
        }
        double sek = plugin.profesje().czas(p, prof, cel.tier(), n.narz().tier(), cel.czasBazowy());
        Sesja s = new Sesja(p, cel, n, (int) Math.round(sek * 20));
        sesje.put(p.getUniqueId(), s);
        pokazNarzedzie(p, n.item());
        s.zadanie = Bukkit.getScheduler().runTaskTimer(plugin, () -> tik(s), 1L, 1L);
        return true;
    }

    private void tik(Sesja s) {
        Player p = s.p;
        if (!p.isOnline() || !s.cel.aktualny()) { przerwij(s, null); return; }
        if (p.getLocation().getWorld() != s.start.getWorld() || p.getLocation().distance(s.start) > plugin.dane().zasiegRuchu) {
            przerwij(s, plugin.kom("przerwane-ruch"));
            return;
        }
        s.t++;
        // inne pluginy odswiezaja trzymany slot kilka razy na sekunde (zmierzone) — ponawiamy narzedzie co tick
        wyslijNarzedzie(p, s.narz.item());
        float postep = Math.min(1f, s.t / (float) s.ticki);
        if (s.t % plugin.dane().machniecieCo == 1) {
            p.swingMainHand();
            Location l = s.cel.miejsce();
            if (s.cel.blok() != null) {
                p.getWorld().spawnParticle(Particle.BLOCK, l, 8, 0.3, 0.3, 0.3, s.cel.blok().getBlockData());
            } else {
                p.getWorld().spawnParticle(Particle.DUST, l, 6, 0.3, 0.15, 0.3, new Particle.DustOptions(org.bukkit.Color.fromRGB(110, 70, 50), 1.2f));
            }
            p.getWorld().playSound(l, s.cel.nagroda().dzwiek(), SoundCategory.BLOCKS, 0.8f, 0.9f + ThreadLocalRandom.current().nextFloat() * 0.2f);
        }
        if (s.cel.blok() != null) {
            for (Player o : p.getWorld().getPlayers()) {
                if (o.getLocation().distanceSquared(s.cel.miejsce()) < 1024) o.sendBlockDamage(s.cel.blok().getLocation(), postep * 0.95f, p.getEntityId());
            }
        }
        pasek(p, plugin.kom("postep", "{nazwa}", s.cel.nazwa(), "{pasek}", pasekPostepu(postep)));
        if (s.t >= s.ticki) zakoncz(s);
    }

    private void zakoncz(Sesja s) {
        Player p = s.p;
        sprzatnij(s);
        Dane d = plugin.dane();
        Dane.Zrodlo w = s.cel.nagroda();
        List<String> lup = losuj(w);
        boolean podw = plugin.profesje().podwojny(p, s.cel.profesja());
        if (podw) lup.add(lup.get(0));
        Map<String, Integer> zliczone = new HashMap<>();
        for (String id : lup) zliczone.merge(id, 1, Integer::sum);
        StringBuilder opis = new StringBuilder();
        for (Map.Entry<String, Integer> e : zliczone.entrySet()) {
            plugin.przedmioty().daj(p, e.getKey(), e.getValue());
            if (opis.length() > 0) opis.append(", ");
            opis.append(e.getValue()).append("× ").append(d.nazwa(e.getKey()));
            plugin.zlecenia().zebrano(p, s.cel.profesja(), e.getKey(), e.getValue());
        }
        double xp = plugin.profesje().daj(p, s.cel.profesja(), s.cel.tier(), d.g(s.cel.tier()), s.cel.miejsce());
        Przedmioty.zuzyj(p, s.narz.slot());
        ItemStack po = p.getInventory().getItem(s.narz.slot());
        int zost = po == null ? -1 : Przedmioty.pozostalo(po);
        p.getWorld().playSound(s.cel.miejsce(), w.dzwiekKoniec(), SoundCategory.BLOCKS, 1f, 1f);
        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.2f);
        String wiad = plugin.kom("zebrano", "{lup}", opis.toString(), "{xp}", String.valueOf(Math.round(xp)))
                + (podw ? plugin.kom("perk-podwojny") : "")
                + (zost >= 0 && zost <= 10 ? plugin.kom("narzedzie-slabnie", "{zostalo}", String.valueOf(zost)) : "");
        pasek(p, wiad);
        s.cel.poSukcesie(p);
    }

    List<String> losuj(Dane.Zrodlo w) {
        List<String> l = new ArrayList<>();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        l.add(w.glowny());
        if (w.poboczny() != null && r.nextDouble() < w.szansaPoboczny()) l.add(w.poboczny());
        if (w.rzadki() != null && r.nextDouble() < w.szansaRzadki()) l.add(w.rzadki());
        return l;
    }

    private void przerwij(Sesja s, String komunikat) {
        sprzatnij(s);
        if (komunikat != null && s.p.isOnline()) pasek(s.p, komunikat);
    }

    private void sprzatnij(Sesja s) {
        if (s.zadanie != null) s.zadanie.cancel();
        sesje.remove(s.p.getUniqueId());
        if (s.cel.blok() != null) {
            for (Player o : s.p.getWorld().getPlayers()) {
                if (o.getLocation().distanceSquared(s.cel.miejsce()) < 1024) o.sendBlockDamage(s.cel.blok().getLocation(), 0f, s.p.getEntityId());
            }
        }
        if (s.p.isOnline()) ukryjNarzedzie(s.p);
    }

    boolean wTrakcie(Player p) { return sesje.containsKey(p.getUniqueId()); }

    void zatrzymajWszystko() {
        for (Sesja s : new ArrayList<>(sesje.values())) przerwij(s, null);
        for (Map.Entry<UUID, Map<Location, Long>> e : wyczerpane.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) for (Location l : e.getValue().keySet()) p.sendBlockChange(l, l.getBlock().getBlockData());
        }
        wyczerpane.clear();
    }

    // ================================================================ narzedzie w dloni (tylko pakiety)
    private void pokazNarzedzie(Player p, ItemStack narz) {
        wyslijNarzedzie(p, narz);
        for (Player o : p.getWorld().getPlayers()) {
            if (o != p && o.canSee(p) && o.getLocation().distanceSquared(p.getLocation()) < 4096) o.sendEquipmentChange(p, EquipmentSlot.HAND, narz);
        }
    }

    private void wyslijNarzedzie(Player p, ItemStack narz) {
        try {
            // 1.21.2+: wlasny ekwipunek klient aktualizuje pakietem set_player_inventory (indeks slotu gracza 0-40)
            var stack = SpigotConversionUtil.fromBukkitItemStack(narz);
            PacketEvents.getAPI().getPlayerManager().sendPacket(p,
                    new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory(p.getInventory().getHeldItemSlot(), stack));
        } catch (Throwable t) {
            plugin.getLogger().warning("Nie udalo sie pokazac narzedzia w dloni: " + t);
        }
    }

    private void ukryjNarzedzie(Player p) {
        p.updateInventory();
        ItemStack w = p.getInventory().getItemInMainHand();
        for (Player o : p.getWorld().getPlayers()) {
            if (o != p && o.canSee(p) && o.getLocation().distanceSquared(p.getLocation()) < 4096) o.sendEquipmentChange(p, EquipmentSlot.HAND, w);
        }
    }

    // ================================================================ przerwania
    @EventHandler(priority = EventPriority.MONITOR)
    public void naZmianeSlotu(PlayerItemHeldEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null) przerwij(s, plugin.kom("przerwane"));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void naObrazenia(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            Sesja s = sesje.get(p.getUniqueId());
            if (s != null) przerwij(s, plugin.kom("przerwane-obrazenia"));
        }
    }

    @EventHandler
    public void naWyjscie(PlayerQuitEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null) przerwij(s, null);
        wyczerpane.remove(e.getPlayer().getUniqueId());
        ostatniKomunikat.remove(e.getPlayer().getUniqueId());
    }

    // ================================================================ wyczerpanie per gracz
    private void wyczerp(Player p, Block b, Dane.Zrodlo w) {
        long koniec = System.currentTimeMillis() + plugin.dane().wyczerpanieS * 1000L;
        Location l = b.getLocation();
        wyczerpane.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(l, koniec);
        p.sendBlockChange(l, w.wyczerpany());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Map<Location, Long> m = wyczerpane.get(p.getUniqueId());
            if (m == null) return;
            Long k = m.get(l);
            if (k != null && k <= System.currentTimeMillis() + 50) {
                m.remove(l);
                if (p.isOnline() && l.getWorld() == p.getWorld()) {
                    p.sendBlockChange(l, l.getBlock().getBlockData());
                    p.spawnParticle(Particle.HAPPY_VILLAGER, l.clone().add(0.5, 0.8, 0.5), 4, 0.3, 0.2, 0.3);
                }
            }
        }, plugin.dane().wyczerpanieS * 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void naChunk(PlayerChunkLoadEvent e) {
        Map<Location, Long> m = wyczerpane.get(e.getPlayer().getUniqueId());
        if (m == null || m.isEmpty()) return;
        long teraz = System.currentTimeMillis();
        List<Location> tu = new ArrayList<>();
        for (Map.Entry<Location, Long> x : m.entrySet()) {
            Location l = x.getKey();
            if (x.getValue() > teraz && l.getWorld() == e.getChunk().getWorld() && (l.getBlockX() >> 4) == e.getChunk().getX()
                    && (l.getBlockZ() >> 4) == e.getChunk().getZ()) tu.add(l);
        }
        if (tu.isEmpty()) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Location l : tu) {
                Dane.Zrodlo w = plugin.dane().wezly.get(nexoId(l.getBlock()));
                if (w != null) e.getPlayer().sendBlockChange(l, w.wyczerpany());
            }
        }, 2L);
    }

    // ================================================================ komunikaty
    void pasek(Player p, String tekst) { plugin.pasek(p, tekst); }

    static String pasekPostepu(float f) {
        int n = Math.round(f * 20);
        return "§a" + "|".repeat(n) + "§8" + "|".repeat(20 - n);
    }
}
