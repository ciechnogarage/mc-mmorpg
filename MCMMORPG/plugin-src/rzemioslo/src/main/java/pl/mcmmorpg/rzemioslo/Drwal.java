package pl.mcmmorpg.rzemioslo;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Drwal: scinka stojacego drzewa w swiecie, w skali 1:1 (bez zblizenia i bez zamrozenia — drwal obchodzi pien).
 *  Wrab (seria LPM z jednej strony, nisko) wyznacza kierunek upadku; rzaz scinajacy (PPM trzymany z drugiej strony,
 *  nie nizej niz wrab) cienczy zawiase — puszczony w pasmie trzeszczenia klada drzewo tam, gdzie wrab. Przeciety
 *  za gleboko, bez wrebu albo z plytkim wrebem: drzewo idzie "gdzie chce", peka albo zawisa na sasiednim.
 *  Upadek widza wszyscy w poblizu (BlockDisplay z interpolacja; prawdziwe bloki ukryte u widzow na czas sceny),
 *  przygniata kazdego gracza w pasie upadku. Lezacy pien okrzesuje sie (korona) i tnie na klody (LPM na granicy klod).
 *  Swierk: nacięcie kory wyzej (spała) — po powrocie scinka daje drewno smolne. Odrost i pniak per konto. */
final class Drwal implements Listener {
    static final String PROF = "woodworking";
    static boolean pomiar = false;

    record Gatunek(String id, Material log, Material kora, Material lisc, String przedmiot, String nazwa, int poziom, long odrostSek, boolean zywica) {}

    static final Map<String, Gatunek> GATUNKI = Map.of(
            "dab", new Gatunek("dab", Material.OAK_LOG, Material.STRIPPED_OAK_LOG, Material.OAK_LEAVES, "PROF_OAK_TIMBER", "dąb", 1, 600, false),
            "swierk", new Gatunek("swierk", Material.SPRUCE_LOG, Material.STRIPPED_SPRUCE_LOG, Material.SPRUCE_LEAVES, "PROF_SPRUCE_RESIN", "świerk", 1, 720, true));

    enum Faza { WRAB, RZAZ, OSTRZEZENIE, PADA, WISI, LEZY }

    /** Obszar, w ktorym kazde naturalne drzewo (1 pien, liscie) da sie sciac — loch/wyspa podaje go komenda `las`. */
    record Las(String swiat, int x1, int z1, int x2, int z2, boolean trwaly) {
        /** swiat "loch_001_*" = kazda instancja MythicDungeons tej mapy (loch_001_0, loch_001_1...). */
        boolean swiatPasuje(String w) { return swiat.endsWith("*") ? w.startsWith(swiat.substring(0, swiat.length() - 1)) : w.equals(swiat); }
        boolean zawiera(Block b) { return swiatPasuje(b.getWorld().getName()) && b.getX() >= x1 && b.getX() <= x2 && b.getZ() >= z1 && b.getZ() <= z2; }
    }

    static final class Drzewo {
        Gatunek g;
        String klucz;
        Block baza;
        final List<Block> pien = new ArrayList<>();       // kolumna od bazy w gore (pien.get(0) = baza)
        final List<Block> galezie = new ArrayList<>();
        final List<Block> liscie = new ArrayList<>();
        int wys;                                          // od podstawy bazy do szczytu korony
        BoundingBox korona;                               // liscie + galezie (uklad swiata, drzewo stojace)

        Location os() { return baza.getLocation().add(0.5, 0, 0.5); }
        List<Block> spadajace() {
            List<Block> l = new ArrayList<>(pien.subList(1, pien.size()));
            l.addAll(galezie);
            l.addAll(liscie);
            return l;
        }
    }

    /** Pniak u drwala na czas odrostu (inni widza drzewo). */
    record Pniak(Drzewo d, long koniec) {}

    static final class Sesja {
        final Player p;
        final Drzewo d;
        Faza faza = Faza.WRAB;
        BukkitTask zad;
        int t, ostatniPpm = -100, ostatniLpm = -100, bezczynnosc;
        // wrab
        int ciosy, rozrzut, wysokie;
        double wrabKat = Double.NaN, wrabY;
        boolean sprochnialy;
        BlockDisplay wrabDisp;
        // zywica (swierk)
        int spala;
        // rzaz
        double postep, rzazKat = Double.NaN, rzazY = Double.NaN;
        boolean trzeszczy, rzazNizej;
        // upadek
        Vector kier;
        boolean gdzieChce, peka, zBoku, zlaStrona;
        double katStop, katKonc, kat;
        int padaOd, krok;
        Location piwot;
        Quaternionf obrot = new Quaternionf();
        final List<BlockDisplay> disp = new ArrayList<>();
        final List<Vector3f> dOff = new ArrayList<>();     // naroznik bloku wzgledem piwotu (drzewo stojace)
        final List<Integer> dSeg = new ArrayList<>();      // nr segmentu pnia (1..n-1), korona = n
        final List<Boolean> dKorona = new ArrayList<>();
        final Set<UUID> widzowie = new HashSet<>();
        int klin;
        boolean zawisl;
        // lezacy pien
        int ciecia[], drzazgi, okrzesanie;
        boolean okrzesany;
        double smolne;
        final List<String> uwagi = new ArrayList<>();
        boolean konczy;

        Sesja(Player p, Drzewo d) { this.p = p; this.d = d; }
        int segmenty() { return d.pien.size() - 1; }
    }

    private final Rzemioslo plugin;
    private final List<Las> lasy = new ArrayList<>();
    private final Map<String, String> pojedyncze = new HashMap<>();       // klucz bazy -> gatunek (trwale)
    private final Map<UUID, Sesja> sesje = new HashMap<>();
    private final Map<String, UUID> zajete = new HashMap<>();
    private final Map<UUID, Map<String, Pniak>> pniaki = new HashMap<>();
    private final Map<String, Boolean> sprochniale = new HashMap<>();     // los raz na drzewo (do odrostu/reloadu)
    private final Map<UUID, Map<String, Long>> nacieta = new HashMap<>(); // spała na swierku: klucz -> kiedy
    private final Map<String, BlockDisplay> spaly = new HashMap<>();
    private final Map<UUID, Long> poKoncu = new ConcurrentHashMap<>();
    private final Set<UUID> wSesji = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> ppmMs = new ConcurrentHashMap<>();   // klient macha reka po PPM w klode z siekiera (przewidziane korowanie)
    private final PacketListenerAbstract pakiety;
    private final File plik;

    Drwal(Rzemioslo plugin) {
        this.plugin = plugin;
        this.plik = new File(plugin.getDataFolder(), "drwal.yml");
        wczytaj();
        Bukkit.getScheduler().runTaskTimer(plugin, this::odrastaj, 400L, 400L);
        pakiety = new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            /** W trakcie scinki kliki naleza do drwala (jak u zielarza): machniecie = LPM, uzycie = PPM; kopanie anulowane
             *  z ack sekwencji, zeby klient cofnal przewidziane kruszenie/korowanie pnia. */
            @Override
            public void onPacketReceive(PacketReceiveEvent e) {
                UUID u = e.getUser().getUUID();
                if (u == null) return;
                var typ = e.getPacketType();
                if (!wSesji.contains(u)) {
                    Long po = poKoncu.get(u);
                    if (po == null || System.currentTimeMillis() > po) return;
                    if (typ == PacketType.Play.Client.USE_ITEM) {
                        e.setCancelled(true);
                        e.getUser().sendPacket(new WrapperPlayServerAcknowledgeBlockChanges(new WrapperPlayClientUseItem(e).getSequence()));
                    } else if (typ == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
                        e.setCancelled(true);
                        e.getUser().sendPacket(new WrapperPlayServerAcknowledgeBlockChanges(new WrapperPlayClientPlayerBlockPlacement(e).getSequence()));
                    }
                    return;
                }
                Runnable akcja;
                int seq = -1;
                if (typ == PacketType.Play.Client.USE_ITEM || typ == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                        || typ == PacketType.Play.Client.INTERACT_ENTITY) ppmMs.put(u, System.currentTimeMillis());
                if (typ == PacketType.Play.Client.ANIMATION) {
                    Long pp = ppmMs.get(u);
                    if (pp != null && System.currentTimeMillis() - pp < 80) { e.setCancelled(true); return; }   // machniecie od PPM, nie cios
                    akcja = () -> { Sesja s = sesje.get(u); if (s != null) lpm(s); };
                } else if (typ == PacketType.Play.Client.USE_ITEM) {
                    var w = new WrapperPlayClientUseItem(e);
                    seq = w.getSequence();
                    akcja = w.getHand() == InteractionHand.MAIN_HAND ? () -> { Sesja s = sesje.get(u); if (s != null) s.ostatniPpm = s.t; } : null;
                } else if (typ == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
                    var w = new WrapperPlayClientPlayerBlockPlacement(e);
                    seq = w.getSequence();
                    akcja = w.getHand() == InteractionHand.MAIN_HAND ? () -> { Sesja s = sesje.get(u); if (s != null) s.ostatniPpm = s.t; } : null;
                } else if (typ == PacketType.Play.Client.PLAYER_DIGGING) {
                    var w = new WrapperPlayClientPlayerDigging(e);
                    if (w.getAction() != DiggingAction.START_DIGGING && w.getAction() != DiggingAction.CANCELLED_DIGGING
                            && w.getAction() != DiggingAction.FINISHED_DIGGING) return;   // Q, zmiana rak itp. zostaja graczowi
                    seq = w.getSequence();
                    akcja = null;
                } else if (typ == PacketType.Play.Client.INTERACT_ENTITY) {
                    var w = new WrapperPlayClientInteractEntity(e);
                    if (w.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;   // machniecie i tak przyjdzie jako ANIMATION
                    akcja = w.getHand() == InteractionHand.MAIN_HAND ? () -> { Sesja s = sesje.get(u); if (s != null) s.ostatniPpm = s.t; } : null;
                } else return;
                e.setCancelled(true);
                if (seq >= 0) e.getUser().sendPacket(new WrapperPlayServerAcknowledgeBlockChanges(seq));
                if (akcja != null) Bukkit.getScheduler().runTask(plugin, akcja);
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(pakiety);
    }

    // ================================================================ rejestr drzew
    static String klucz(Block b) { return b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ(); }

    private void wczytaj() {
        if (!plik.exists()) return;
        var y = YamlConfiguration.loadConfiguration(plik);
        for (String s : y.getStringList("lasy")) {
            String[] a = s.split(":");
            if (a.length == 5) lasy.add(new Las(a[0], Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4]), true));
        }
        var sek = y.getConfigurationSection("drzewa");
        if (sek != null) for (String k : sek.getKeys(false)) pojedyncze.put(k.replace(';', ':'), sek.getString(k));
    }

    private void zapisz() {
        var y = new YamlConfiguration();
        List<String> l = new ArrayList<>();
        for (Las x : lasy) if (x.trwaly) l.add(x.swiat + ":" + x.x1 + ":" + x.z1 + ":" + x.x2 + ":" + x.z2);
        y.set("lasy", l);
        for (var e : pojedyncze.entrySet()) y.set("drzewa." + e.getKey().replace(':', ';'), e.getValue());
        try { y.save(plik); } catch (Exception ex) { plugin.getLogger().warning("Drwal: zapis drwal.yml: " + ex.getMessage()); }
    }

    String las(String w, int x1, int z1, int x2, int z2, boolean trwaly) {
        Las l = new Las(w, Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2), trwaly);
        if (lasy.contains(l) || lasy.contains(new Las(l.swiat, l.x1, l.z1, l.x2, l.z2, !trwaly))) return "las juz jest";
        lasy.add(l);
        if (trwaly) zapisz();
        return "las " + l.swiat + " " + l.x1 + "," + l.z1 + " .. " + l.x2 + "," + l.z2 + (trwaly ? " (trwaly)" : " (do zamkniecia swiata)");
    }

    String drzewo(Block b, String gatunek) {
        Drzewo d = rozpoznaj(b, true);
        if ("sprochnialy".equals(gatunek)) {   // test: wymusza pusty pien (los 12% raz na drzewo)
            if (d == null) return "to nie drzewo";
            sprochniale.put(d.klucz, true); return "sprochnialy " + d.klucz;
        }
        if (gatunek == null) {
            if (d == null) return "to nie drzewo";
            pojedyncze.remove(d.klucz); zapisz(); return "usunieto " + d.klucz;
        }
        if (d == null) return "nie widze tu drzewa (pien 1x1 debu/swierka 3-12 klod na ziemi, min. 8 lisci)";
        pojedyncze.put(d.klucz, d.g.id()); zapisz();
        return "drzewo " + d.g.id() + " @ " + d.klucz + " pien " + d.pien.size() + " liscie " + d.liscie.size();
    }

    List<String> skan(World w, int x1, int z1, int x2, int z2) {
        List<String> out = new ArrayList<>();
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
                Block top = w.getHighestBlockAt(x, z);
                for (int y = top.getY(); y > top.getY() - 24 && y > w.getMinHeight(); y--) {
                    Block b = w.getBlockAt(x, y, z);
                    Gatunek g = gatunekBloku(b.getType());
                    if (g == null || gatunekBloku(b.getRelative(BlockFace.DOWN).getType()) != null) continue;
                    Drzewo d = rozpoznaj(b, true);
                    if (d != null && d.baza.equals(b)) out.add(g.id() + " " + x + " " + y + " " + z + " pien=" + d.pien.size() + " liscie=" + d.liscie.size());
                }
            }
        return out;
    }

    private static Gatunek gatunekBloku(Material m) {
        for (Gatunek g : GATUNKI.values()) if (g.log == m) return g;
        return null;
    }

    private boolean scinalne(Block baza) {
        if (pojedyncze.containsKey(klucz(baza))) return true;
        for (Las l : lasy) if (l.zawiera(baza)) return true;
        return false;
    }

    /** Drzewo z dowolnego bloku pnia: baza = najnizsza kloda kolumny na gruncie. null = nie drzewo (dom z bali, plot itp.). */
    Drzewo rozpoznaj(Block b, boolean bezObszaru) {
        Gatunek g = gatunekBloku(b.getType());
        if (g == null) return null;
        Block baza = b;
        for (int i = 0; i < 14 && baza.getRelative(BlockFace.DOWN).getType() == g.log; i++) baza = baza.getRelative(BlockFace.DOWN);
        Block pod = baza.getRelative(BlockFace.DOWN);
        if (!pod.getType().isSolid() || Tag.LOGS.isTagged(pod.getType())) return null;
        for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST})
            if (Tag.LOGS.isTagged(baza.getRelative(f).getType())) return null;   // pien 2x2 albo sciana z bali
        if (!bezObszaru && !scinalne(baza)) return null;
        Drzewo d = new Drzewo();
        d.g = g; d.baza = baza; d.klucz = klucz(baza);
        Block c = baza;
        while (c.getType() == g.log && d.pien.size() < 13) { d.pien.add(c); c = c.getRelative(BlockFace.UP); }
        if (d.pien.size() < 3 || d.pien.size() > 12) return null;
        // galezie: klody polaczone z pniem (takze po skosie) w promieniu 3 od osi, nad baza
        Set<Block> wid = new HashSet<>(d.pien);
        ArrayDeque<Block> q = new ArrayDeque<>(d.pien.subList(1, d.pien.size()));
        while (!q.isEmpty() && d.galezie.size() < 24) {
            Block x = q.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                Block n = x.getRelative(dx, dy, dz);
                if (wid.contains(n) || n.getType() != g.log) continue;
                if (Math.abs(n.getX() - baza.getX()) > 3 || Math.abs(n.getZ() - baza.getZ()) > 3 || n.getY() <= baza.getY()) continue;
                wid.add(n); d.galezie.add(n); q.add(n);
            }
        }
        // liscie: BFS po lisciach od klod tego drzewa; lisc nalezy do drzewa, gdy jego vanilla "distance" (kroki do najblizszej
        // klody) nie jest mniejsza niz odleglosc od naszych klod — liscie sasiada (blizej jego pnia) zostaja na miejscu
        List<Block> klody = new ArrayList<>(d.pien); klody.addAll(d.galezie);
        Set<Block> ls = new HashSet<>();
        Map<Block, Integer> odl = new HashMap<>();
        ArrayDeque<Block> fq = new ArrayDeque<>();
        for (Block k : klody) { odl.put(k, 0); fq.add(k); }
        BlockFace[] sz = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        while (!fq.isEmpty()) {
            Block x = fq.poll();
            int dx0 = odl.get(x);
            if (dx0 >= 5) continue;
            for (BlockFace f : sz) {
                Block n = x.getRelative(f);
                if (odl.containsKey(n) || n.getY() <= baza.getY() || !Tag.LEAVES.isTagged(n.getType())) continue;
                if (Math.abs(n.getX() - baza.getX()) > 4 || Math.abs(n.getZ() - baza.getZ()) > 4) continue;
                int dn = dx0 + 1;
                if (n.getBlockData() instanceof org.bukkit.block.data.type.Leaves lv && !lv.isPersistent() && lv.getDistance() < dn) continue;
                odl.put(n, dn); ls.add(n); fq.add(n);
            }
        }
        if (ls.size() < 8) return null;
        List<Block> lsl = new ArrayList<>(ls);
        final int bx = baza.getX(), bz = baza.getZ();
        lsl.sort(Comparator.comparingDouble(n -> Math.pow(n.getX() - bx, 2) + Math.pow(n.getZ() - bz, 2)));
        d.liscie.addAll(lsl.subList(0, Math.min(110, lsl.size())));
        int top = d.pien.get(d.pien.size() - 1).getY();
        BoundingBox kor = null;
        for (Block n : lsl) { top = Math.max(top, n.getY()); kor = kor == null ? n.getBoundingBox().clone() : kor.union(n.getBoundingBox()); }
        for (Block n : d.galezie) { top = Math.max(top, n.getY()); kor = kor.union(n.getBoundingBox()); }
        d.korona = kor;
        d.wys = top - baza.getY() + 1;
        return d;
    }

    // ================================================================ wejscie
    @EventHandler(priority = EventPriority.LOWEST)
    public void naKlik(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (wSesji.contains(p.getUniqueId())) {   // pakiet anulowany, ale zdarzenie z innych drog (np. PPM w powietrze) tez blokujemy
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.DENY);
            return;
        }
        Action a = e.getAction();
        if ((a != Action.LEFT_CLICK_BLOCK && a != Action.RIGHT_CLICK_BLOCK) || e.getClickedBlock() == null) return;
        if (gatunekBloku(e.getClickedBlock().getType()) == null) return;
        Drzewo d = rozpoznaj(e.getClickedBlock(), false);
        if (d == null) return;
        if (e.getHand() != EquipmentSlot.HAND) { e.setCancelled(true); return; }
        if (a == Action.RIGHT_CLICK_BLOCK) {   // korowanie siekiera itp. — nigdy na drzewie do scinki
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.DENY);
        }
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        Sesja s = rozpocznij(p, d);
        if (s != null && a == Action.RIGHT_CLICK_BLOCK) ppmMs.put(p.getUniqueId(), System.currentTimeMillis());   // jego machniecie to nie cios
        if (s != null && a == Action.LEFT_CLICK_BLOCK) lpm(s);
    }

    @EventHandler(ignoreCancelled = true)
    public void naUderzenie(BlockDamageEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        if (gatunekBloku(e.getBlock().getType()) != null && rozpoznaj(e.getBlock(), false) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void naZniszczenie(BlockBreakEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        if (gatunekBloku(e.getBlock().getType()) != null && rozpoznaj(e.getBlock(), false) != null) e.setCancelled(true);
    }

    Sesja rozpocznij(Player p, Drzewo d) {
        if (sesje.containsKey(p.getUniqueId())) return null;
        Long po = poKoncu.get(p.getUniqueId());
        if (po != null && System.currentTimeMillis() < po) return null;
        Pniak pn = pniaki.getOrDefault(p.getUniqueId(), Map.of()).get(d.klucz);
        if (pn != null && pn.koniec > System.currentTimeMillis()) {
            plugin.pasek(p, "§7Tu już ścinałeś. Odrośnie za " + Zielarz.czas((pn.koniec - System.currentTimeMillis()) / 1000 + 1) + ".");
            pokazPniak(p, pn.d);
            return null;
        }
        UUID kto = zajete.get(d.klucz);
        if (kto != null && !kto.equals(p.getUniqueId())) { plugin.pasek(p, "§7Ktoś już ścina to drzewo."); return null; }
        ItemStack reka = p.getInventory().getItemInMainHand();
        Dane.Narz n = plugin.dane().narzedzia.get(Przedmioty.id(reka));
        String rodzaj = plugin.dane().narzedzieProfesji.getOrDefault(PROF, "siekiera");
        if (n == null || !n.rodzaj().equals(rodzaj)) {
            plugin.pasek(p, plugin.przedmioty().najwyzszyTier(p, rodzaj) > 0
                    ? "§7Weź siekierę do ręki." : "§7Gołymi rękami drzewa nie zetniesz. Potrzebna siekiera.");
            return null;
        }
        if (Przedmioty.zepsute(reka)) { plugin.pasek(p, "§cSiekiera stępiona. Najpierw ją napraw."); return null; }
        int lvl = Profesje.poziom(p, PROF);
        if (lvl < d.g.poziom()) { plugin.pasek(p, "§7Za mało wprawy w drwalstwie (poziom " + d.g.poziom() + ")."); return null; }
        Location os = d.os();
        double dyst = Math.hypot(p.getX() - os.getX(), p.getZ() - os.getZ());
        if (dyst > 3.4) return null;
        Sesja s = new Sesja(p, d);
        s.sprochnialy = sprochniale.computeIfAbsent(d.klucz, k -> d.g.id().equals("dab") && ThreadLocalRandom.current().nextDouble() < 0.12);
        Long nac = nacieta.getOrDefault(p.getUniqueId(), Map.of()).get(d.klucz);
        if (nac != null) s.smolne = Math.min(1.0, (System.currentTimeMillis() - nac) / 300_000.0);   // po 5 min zywica wsiaknela w drewno
        sesje.put(p.getUniqueId(), s);
        wSesji.add(p.getUniqueId());
        zajete.put(d.klucz, p.getUniqueId());
        s.zad = Bukkit.getScheduler().runTaskTimer(plugin, () -> tik(s), 1L, 1L);
        if (pomiar) plugin.getLogger().info("[drwal] start " + p.getName() + " " + d.g.id() + " " + d.klucz + " pien=" + d.pien.size() + " liscie=" + d.liscie.size() + " sprochnialy=" + s.sprochnialy);
        return s;
    }

    // ================================================================ geometria
    private static double kat(Location os, Location gdzie) {
        return Math.toDegrees(Math.atan2(gdzie.getZ() - os.getZ(), gdzie.getX() - os.getX()));
    }

    static double roznica(double a, double b) {
        double d = ((a - b) % 360 + 540) % 360 - 180;
        return Math.abs(d);
    }

    private static Vector wektor(double katSt) {
        double r = Math.toRadians(katSt);
        return new Vector(Math.cos(r), 0, Math.sin(r));
    }

    /** Wysokosc trafienia w pien (wzgledem podstawy bazy) albo NaN, gdy celownik nie trafia w pien tego drzewa. */
    private double trafieniePnia(Sesja s, double maxY) {
        RayTraceResult r = s.p.rayTraceBlocks(4.5, FluidCollisionMode.NEVER);
        if (r == null || r.getHitBlock() == null) return Double.NaN;
        Block hb = r.getHitBlock();
        if (!s.d.pien.contains(hb)) return Double.NaN;
        double y = r.getHitPosition().getY() - s.d.baza.getY();
        return y < 0 || y > maxY ? Double.NaN : y;
    }

    // ================================================================ gesty
    void lpm(Sesja s) {
        if (s.konczy || s.t - s.ostatniLpm < 7) return;   // rytm siekiery: szybsze klikanie nic nie daje
        s.ostatniLpm = s.t;
        s.bezczynnosc = 0;
        s.p.swingMainHand();   // pakiet machniecia jest anulowany — inni gracze widza zamach z serwera
        switch (s.faza) {
            case WRAB, RZAZ -> ciosWPien(s);
            case WISI -> klin(s);
            case LEZY -> ciosWLezacy(s);
            default -> { }
        }
    }

    private void ciosWPien(Sesja s) {
        double y = trafieniePnia(s, 2.4);
        if (Double.isNaN(y)) return;
        Location os = s.d.os();
        double k = kat(os, s.p.getLocation());
        Location gdzie = os.clone().add(wektor(k).multiply(0.5)).add(0, y, 0);
        zuzyj(s);
        if (s.d.g.zywica() && y >= 1.15 && s.ciosy == 0) {   // swierk: spała wyzej na pniu, nie wrab
            s.spala++;
            if (pomiar) plugin.getLogger().info("[drwal] spala " + s.p.getName() + " y=" + String.format("%.2f", y) + " pitch=" + String.format("%.1f", s.p.getLocation().getPitch()) + " n=" + s.spala);
            dzwiek(gdzie, Sound.ITEM_AXE_STRIP, 0.9f, 1.1f);
            gdzie.getWorld().spawnParticle(Particle.BLOCK, gdzie, 10, 0.15, 0.15, 0.15, s.d.g.kora().createBlockData());
            if (s.spala >= 3) {
                nacieta.computeIfAbsent(s.p.getUniqueId(), u -> new HashMap<>()).put(s.d.klucz, System.currentTimeMillis());
                BlockDisplay st = spaly.remove(s.p.getUniqueId() + s.d.klucz);
                if (st != null) st.remove();
                spaly.put(s.p.getUniqueId() + s.d.klucz, plytka(os, k, y - 0.25, 0.45, 0.6, s.d.g.kora()));
                gdzie.getWorld().spawnParticle(Particle.FALLING_HONEY, gdzie, 6, 0.1, 0.2, 0.1, 0);
                zakoncz(s, "§6Kora zdjęta pasem. Pień zaczyna sączyć żywicę.", false);
            }
            return;
        }
        if (y > 1.25) {   // za wysoko na wrab — strata wiórów, nic nie daje
            s.wysokie++;
            dzwiek(gdzie, Sound.BLOCK_WOOD_HIT, 0.8f, 1.0f);
            if (s.wysokie == 3 && wprawa(s) < 3) plugin.pasek(s.p, "§7Wrąb tnie się nisko, tuż nad ziemią.");
            return;
        }
        if (s.faza == Faza.RZAZ) {   // po rozpoczeciu rzazu ciosy siekiera psuja zawiase
            s.rozrzut++;
            dzwiek(gdzie, Sound.BLOCK_WOOD_HIT, 0.8f, 0.8f);
            return;
        }
        if (s.ciosy == 0) s.wrabKat = k;
        else if (roznica(k, s.wrabKat) > 25) s.rozrzut++;
        s.ciosy++;
        s.wrabY += y;
        // brzmienie pnia: zdrowy dzwieczy, sprochnialy gluchnie (gra nie mowi tego wprost)
        if (s.sprochnialy) {
            dzwiek(gdzie, Sound.BLOCK_WOOD_HIT, 1f, 0.5f);
            dzwiek(gdzie, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.5f, 0.55f);
            gdzie.getWorld().spawnParticle(Particle.BLOCK, gdzie, 10, 0.15, 0.1, 0.15, Material.BROWN_MUSHROOM_BLOCK.createBlockData());
        } else {
            dzwiek(gdzie, Sound.BLOCK_WOOD_HIT, 1f, 0.9f);
            dzwiek(gdzie, Sound.ITEM_AXE_STRIP, 0.7f, 0.75f);
            gdzie.getWorld().spawnParticle(Particle.BLOCK, gdzie, 12, 0.15, 0.1, 0.15, s.d.g.log().createBlockData());
        }
        // widoczny wrab: jasne swieze drewno w twarzy pnia od strony ciosow (rosnie z kazdym ciosem)
        if (s.wrabDisp != null) s.wrabDisp.remove();
        double h = Math.min(0.55, 0.09 * s.ciosy), sz = Math.min(0.9, 0.45 + 0.05 * s.ciosy);
        s.wrabDisp = plytka(os, s.wrabKat, s.wrabY / s.ciosy - h / 2, h, sz, s.d.g.kora());
        if (pomiar) plugin.getLogger().info("[drwal] wrab " + s.p.getName() + " cios=" + s.ciosy + " kat=" + Math.round(k) + " y=" + String.format("%.2f", y) + " rozrzut=" + s.rozrzut);
    }

    /** Cienka plytka jasnego drewna na powierzchni pnia od strony kata (wrab, spała). Widoczna dla wszystkich. */
    private BlockDisplay plytka(Location os, double katSt, double y, double wys, double szer, Material m) {
        Location l = os.clone().add(0, y, 0);
        float yaw = (float) (katSt - 90);   // os z encji patrzy w strone kata
        l.setYaw(yaw); l.setPitch(0);
        return l.getWorld().spawn(l, BlockDisplay.class, e -> {
            e.setPersistent(false);
            e.setBlock(m.createBlockData());
            e.setTransformation(new Transformation(new Vector3f((float) -szer / 2, 0, 0.5f), new Quaternionf(),
                    new Vector3f((float) szer, (float) wys, 0.04f), new Quaternionf()));
        });
    }

    private void klin(Sesja s) {
        // zawieszone: klin w rzaz od strony przeciwnej do upadku (przy pniaku), kilka ciosow obuchem
        RayTraceResult r = s.p.rayTraceBlocks(4.5, FluidCollisionMode.NEVER);
        if (r == null || !s.d.baza.equals(r.getHitBlock())) return;
        double k = kat(s.d.os(), s.p.getLocation());
        double kierKat = Math.toDegrees(Math.atan2(s.kier.getZ(), s.kier.getX()));
        Location gdzie = r.getHitPosition().toLocation(s.p.getWorld());
        if (roznica(k, kierKat) < 110) {
            dzwiek(gdzie, Sound.BLOCK_WOOD_HIT, 0.7f, 1.2f);
            if (wprawa(s) < 3 && s.klin == 0) plugin.pasek(s.p, "§7Z tej strony klin nic nie ruszy.");
            return;
        }
        s.klin++;
        zuzyj(s);
        dzwiek(gdzie, Sound.BLOCK_ANVIL_LAND, 0.35f, 1.6f);
        dzwiek(gdzie, Sound.BLOCK_WOOD_BREAK, 0.8f, 0.6f);
        ustawKat(s, s.kat + 2.5, 3);
        if (s.klin >= 4) {
            uwaga(s, "zawisl");
            s.katStop = s.katKonc;
            s.faza = Faza.PADA;
            s.padaOd = s.t;
            s.krok = 0;
            krzyk(s, "§c§lDrzewo spada!");
        }
    }

    private void ciosWLezacy(Sesja s) {
        // promien w ukladzie stojacego drzewa: obrot odwrotny wokol piwotu
        Quaternionf inv = new Quaternionf(s.obrot).conjugate();
        Location oko = s.p.getEyeLocation();
        Vector3f o = new Vector3f((float) (oko.getX() - s.piwot.getX()), (float) (oko.getY() - s.piwot.getY()), (float) (oko.getZ() - s.piwot.getZ()));
        Vector kd = oko.getDirection();
        Vector3f dir = new Vector3f((float) kd.getX(), (float) kd.getY(), (float) kd.getZ());
        inv.transform(o); inv.transform(dir);
        Vector st = new Vector(o.x, o.y, o.z), ki = new Vector(dir.x, dir.y, dir.z);
        Location os = s.d.os();
        double px = os.getX() - s.piwot.getX(), pz = os.getZ() - s.piwot.getZ(), py = s.d.baza.getY() + 1 - s.piwot.getY();
        BoundingBox pien = new BoundingBox(px - 0.5, py, pz - 0.5, px + 0.5, py + s.segmenty() + SZCZELINA * ciete(s), pz + 0.5);
        RayTraceResult rp = pien.rayTrace(st, ki, 5.0);
        RayTraceResult rk = null;
        if (!s.okrzesany && s.d.korona != null) {
            BoundingBox kor = s.d.korona.clone().shift(-s.piwot.getX(), -s.piwot.getY(), -s.piwot.getZ());
            rk = kor.rayTrace(st, ki, 5.0);
            if (kor.contains(st)) rk = new RayTraceResult(st.clone().add(ki.clone().multiply(0.5)));   // stoi w galeziach: cios idzie w korone
        }
        double dp = rp == null ? 99 : rp.getHitPosition().distance(st), dk = rk == null ? 99 : rk.getHitPosition().distance(st);
        if (pomiar) plugin.getLogger().info("[drwal] cios-lezacy " + s.p.getName() + " dp=" + String.format("%.2f", dp) + " dk=" + String.format("%.2f", dk)
                + (rp == null ? "" : " wzd=" + String.format("%.2f", rp.getHitPosition().getY() - py)));
        if (dp >= 99 && dk >= 99) return;
        zuzyj(s);
        if (dk < dp) {   // okrzesywanie korony
            s.okrzesanie++;
            Location gdzie = swiatowy(s, rk.getHitPosition());
            dzwiek(gdzie, Sound.BLOCK_AZALEA_LEAVES_BREAK, 1f, 0.8f);
            gdzie.getWorld().spawnParticle(Particle.BLOCK, gdzie, 18, 0.5, 0.4, 0.5, s.d.g.lisc().createBlockData());
            if (s.okrzesanie >= 3) {
                s.okrzesany = true;
                for (int i = 0; i < s.disp.size(); i++) if (s.dKorona.get(i)) { s.disp.get(i).remove(); }
                dzwiek(gdzie, Sound.BLOCK_GRASS_BREAK, 1f, 0.6f);
                gdzie.getWorld().spawnParticle(Particle.BLOCK, gdzie, 40, 1.2, 0.6, 1.2, s.d.g.lisc().createBlockData());
            }
            return;
        }
        double wzd = rp.getHitPosition().getY() - py;   // odleglosc od pniaka wzdluz pnia (tak, jak widac klody)
        int gr = -1;
        double najb = 9;
        for (int g = 1; g < s.segmenty(); g++) { double od = Math.abs(wzd - granicaWidoczna(s, g)); if (od < najb) { najb = od; gr = g; } }
        Location gdzie = swiatowy(s, rp.getHitPosition());
        if (gr >= 1 && najb < TRAF && s.ciecia[gr] < 2) {
            s.ciecia[gr]++;
            dzwiek(gdzie, Sound.BLOCK_WOOD_HIT, 1f, 0.85f);
            gdzie.getWorld().spawnParticle(Particle.BLOCK, gdzie, 10, 0.1, 0.2, 0.1, s.d.g.kora().createBlockData());
            if (s.ciecia[gr] == 2) {
                dzwiek(gdzie, Sound.BLOCK_WOOD_BREAK, 1f, 0.7f);
                odsun(s);
                if (pomiar) plugin.getLogger().info("[drwal] kloda " + s.p.getName() + " granica=" + gr);
                if (wszystkieCiecia(s)) { zakoncz(s, null, true); }
            }
        } else if (gr >= 1 && najb >= TRAF && wzd > 0.3) {
            s.drzazgi++;
            dzwiek(gdzie, Sound.BLOCK_WOOD_HIT, 0.8f, 1.3f);
            gdzie.getWorld().spawnParticle(Particle.BLOCK, gdzie, 6, 0.15, 0.15, 0.15, s.d.g.log().createBlockData());
            if (s.drzazgi == 3 && wprawa(s) < 3) plugin.pasek(s.p, "§7Same drzazgi. Kłoda ma mieć równą długość.");
        }
    }

    private boolean wszystkieCiecia(Sesja s) {
        for (int i = 1; i < s.segmenty(); i++) if (s.ciecia[i] < 2) return false;
        return true;
    }

    private int ciete(Sesja s) {
        int n = 0;
        for (int i = 1; i < s.segmenty(); i++) if (s.ciecia[i] >= 2) n++;
        return n;
    }

    private Location swiatowy(Sesja s, Vector lokalnyStojacy) {
        Vector3f v = new Vector3f((float) lokalnyStojacy.getX(), (float) lokalnyStojacy.getY(), (float) lokalnyStojacy.getZ());
        s.obrot.transform(v);
        return s.piwot.clone().add(v.x, v.y, v.z);
    }

    // ================================================================ tik
    private void tik(Sesja s) {
        if (s.konczy) return;
        s.t++;
        Player p = s.p;
        if (!p.isOnline() || p.isDead() || !p.getWorld().equals(s.d.baza.getWorld())) { zakoncz(s, null, s.faza.ordinal() >= Faza.PADA.ordinal()); return; }
        Location os = s.d.os();
        double dyst = Math.hypot(p.getX() - os.getX(), p.getZ() - os.getZ());
        boolean trzyma = s.t - s.ostatniPpm <= 5;
        if (!trzyma) s.bezczynnosc++; else s.bezczynnosc = 0;
        switch (s.faza) {
            case WRAB, RZAZ -> {
                if (dyst > 4.5) { zakoncz(s, s.ciosy > 0 ? "§7Odchodzisz od nadciętego drzewa." : null, false); return; }
                if (s.bezczynnosc > 900) { zakoncz(s, null, false); return; }
                if (trzyma) rzaz(s);
                else if (s.faza == Faza.RZAZ && s.t - s.ostatniPpm == 6) puszczony(s);
            }
            case OSTRZEZENIE -> {
                int dt = s.t - s.padaOd;
                if (dt % 6 == 0) dzwiek(s.d.baza.getLocation().add(0.5, 1, 0.5), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 0.5f);
                if (dt == 14) dzwiek(s.d.baza.getLocation().add(0.5, 1, 0.5), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.6f, 0.6f);
                if (dt >= 24) { s.faza = Faza.PADA; s.padaOd = s.t; s.krok = 0; }
            }
            case PADA -> pada(s);
            case WISI -> {
                if (s.t % 30 == 0) dzwiek(s.d.baza.getLocation().add(0.5, 2, 0.5), Sound.BLOCK_WOODEN_DOOR_CLOSE, 0.6f, 0.4f);
                if (dyst > 8 || s.bezczynnosc > 1200) { zakoncz(s, "§7Zostawiasz zawieszone drzewo. Zabierasz tylko, co się dało.", true); }
            }
            case LEZY -> {
                if (s.bezczynnosc > 900 || p.getLocation().distance(s.piwot) > 14) zakoncz(s, null, true);
            }
        }
    }

    private void rzaz(Sesja s) {
        double y = trafieniePnia(s, 1.7);
        if (Double.isNaN(y)) return;
        Location os = s.d.os();
        double k = kat(os, s.p.getLocation());
        if (s.faza == Faza.WRAB) {
            s.faza = Faza.RZAZ;
            s.rzazKat = k;
            s.rzazY = y;
            double wy = s.ciosy > 0 ? s.wrabY / s.ciosy : 0;
            s.rzazNizej = s.ciosy > 0 && y < wy - 0.05;
            if (pomiar) plugin.getLogger().info("[drwal] rzaz " + s.p.getName() + " kat=" + Math.round(k) + " y=" + String.format("%.2f", y) + " wrabY=" + String.format("%.2f", wy) + " roznica=" + (Double.isNaN(s.wrabKat) ? "-" : Math.round(roznica(k, s.wrabKat))));
        }
        s.postep += 0.011;
        Location gdzie = os.clone().add(wektor(s.rzazKat).multiply(0.5)).add(0, s.rzazY, 0);
        if (s.t % 4 == 0) {
            dzwiek(gdzie, Sound.BLOCK_WOOD_HIT, 0.6f, 1.25f);
            gdzie.getWorld().spawnParticle(Particle.BLOCK, gdzie, 4, 0.1, 0.05, 0.1, s.d.g.kora().createBlockData());
            if (s.t % 16 == 0) zuzyj(s);
        }
        // zawiasa: od 0,72 drzewo trzeszczy — znak, ze trzeba puscic; dalej tnie sie juz zawiase
        if (s.postep >= 0.72 && !s.trzeszczy) {
            s.trzeszczy = true;
            dzwiek(s.d.baza.getLocation().add(0.5, 1.5, 0.5), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 0.5f);
            if (wprawa(s) < 3) plugin.pasek(s.p, "§6Pień trzeszczy…");
        }
        if (s.trzeszczy && s.t % 5 == 0) dzwiek(s.d.baza.getLocation().add(0.5, 2, 0.5), Sound.BLOCK_WOODEN_DOOR_OPEN, 0.8f, 0.45f + (float) (s.postep - 0.72));
        boolean gleboki = s.ciosy >= 9;
        if (gleboki && s.postep >= 0.55) {   // za gleboki wrab: pien nie trzyma, idzie przed czasem
            uwaga(s, "wrab_gleboki");
            upadek(s, ThreadLocalRandom.current().nextBoolean());
            return;
        }
        if (s.ciosy == 0 && s.postep >= 0.70) { uwaga(s, "bez_wrebu"); upadek(s, true); return; }
        if (s.postep >= 0.90) { uwaga(s, "zawiasa_przecieta"); upadek(s, true); }
    }

    private void puszczony(Sesja s) {
        if (s.postep >= 0.72) upadek(s, false);   // pasmo 0,72-0,90: czysta zawiasa
        else if (pomiar) plugin.getLogger().info("[drwal] puszczony " + s.p.getName() + " postep=" + String.format("%.2f", s.postep) + " — stoi");
    }

    // ================================================================ upadek
    private void upadek(Sesja s, boolean gdzieChce) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double bazowy = Double.isNaN(s.wrabKat) ? r.nextDouble(360) : s.wrabKat;
        double kierKat = bazowy;
        if (!gdzieChce && !Double.isNaN(s.rzazKat) && !Double.isNaN(s.wrabKat) && roznica(s.rzazKat, s.wrabKat) < 60) {
            s.zlaStrona = true;   // rzaz od strony wrebu: zawiasy nie ma — drzewo nie ma prowadzenia (stoi sie wtedy w pasie upadku)
            gdzieChce = true;
        }
        if (gdzieChce) { s.gdzieChce = true; kierKat = bazowy + r.nextDouble(-100, 100); }
        else if (!Double.isNaN(s.rzazKat) && !Double.isNaN(s.wrabKat)) {
            double odPrzeciw = roznica(s.rzazKat, s.wrabKat + 180);
            if (odPrzeciw > 45) {   // rzaz z boku: zawiasa skosna, drzewo skreca
                s.zBoku = true;
                double znak = ((s.rzazKat - (s.wrabKat + 180)) % 360 + 540) % 360 - 180 > 0 ? -1 : 1;
                kierKat += znak * (odPrzeciw - 45) * 0.5;
            }
        }
        kierKat += r.nextDouble(-6, 6);
        s.kier = wektor(kierKat);
        double pekSzansa = s.sprochnialy ? 0.0 : s.ciosy == 0 ? 0.3 : s.ciosy < 4 ? 0.7 : s.rzazNizej ? 0.5 : 0.0;
        s.peka = r.nextDouble() < pekSzansa;
        // przeszkoda w pasie upadku: inne drzewo, skala — drzewo zawisa albo kladzie sie skosem
        s.katKonc = 90 + Math.toDegrees(Math.atan2(1.0, Math.max(1, s.segmenty())));
        s.katStop = s.katKonc;
        double przeszk = przeszkoda(s);
        if (przeszk < s.katKonc) s.katStop = przeszk;
        s.zawisl = s.katStop < 60;
        // piwot = krawedz pniaka po stronie upadku
        Location os = s.d.os();
        s.piwot = os.clone().add(s.kier.clone().multiply(0.5)).add(0, 1, 0);
        s.piwot.setYaw(0); s.piwot.setPitch(0);
        budujDisplaye(s);
        s.faza = Faza.OSTRZEZENIE;
        s.padaOd = s.t;
        ustawKat(s, 3, 24);
        krzyk(s, "§c§lDrzewo idzie!");
        dzwiek(os.clone().add(0, 1, 0), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.9f, 0.5f);
        if (pomiar) plugin.getLogger().info("[drwal] upadek " + s.p.getName() + " kier=" + Math.round(kierKat) + " wrabKat=" + (Double.isNaN(s.wrabKat) ? "-" : Math.round(s.wrabKat))
                + " gdzieChce=" + gdzieChce + " peka=" + s.peka + " zBoku=" + s.zBoku + " zlaStrona=" + s.zlaStrona + " stop=" + Math.round(s.katStop) + " postep=" + String.format("%.2f", s.postep));
    }

    /** Kat (od pionu), przy ktorym pien albo korona zahaczy o cos, co nie jest tym drzewem; katKonc = nic po drodze. */
    private double przeszkoda(Sesja s) {
        Set<Block> swoje = new HashSet<>(s.d.pien); swoje.addAll(s.d.galezie); swoje.addAll(s.d.liscie);
        Location os = s.d.os();
        double best = s.katKonc;
        Vector bok = new Vector(-s.kier.getZ(), 0, s.kier.getX());
        BoundingBox kor = s.d.korona == null ? new BoundingBox() : s.d.korona.clone().expand(0.5);
        for (double dl = 1.5; dl <= s.d.wys; dl += 0.5) {
            for (double b = -1; b <= 1; b += 1) {
                Location kol = os.clone().add(s.kier.clone().multiply(dl)).add(bok.clone().multiply(b));
                for (int h = 1; h <= s.d.wys; h++) {
                    Block x = kol.getWorld().getBlockAt(kol.getBlockX(), s.d.baza.getY() + h, kol.getBlockZ());
                    if (swoje.contains(x)) continue;
                    // liscie we wlasnej koronie ponad limitem displayow (gesty swierk) to nie sasiad — nie zawiesza na sobie
                    if (Tag.LEAVES.isTagged(x.getType()) && kor.getVolume() > 0 && kor.contains(x.getX() + 0.5, x.getY() + 0.5, x.getZ() + 0.5)) continue;
                    boolean twarde = Tag.LEAVES.isTagged(x.getType()) || Tag.LOGS.isTagged(x.getType()) || x.getType().isSolid() && !x.isPassable();
                    if (!twarde) continue;
                    // punkt (dl, h) wzgledem piwotu na wysokosci pniaka: drzewo dosiega go, gdy kat od pionu = atan(dl/h)
                    double hh = h - 0.5, odl = Math.hypot(dl, hh);
                    if (odl > s.d.wys) continue;
                    double k = Math.toDegrees(Math.atan2(dl, hh));
                    if (Math.abs(b) > 0.5 && odl < s.d.wys - 2) continue;   // z boku liczy sie tylko korona
                    best = Math.min(best, k);
                }
            }
        }
        return best;
    }

    private void budujDisplaye(Sesja s) {
        World w = s.piwot.getWorld();
        List<Block> bl = s.d.spadajace();
        int n = s.segmenty();
        for (Block b : bl) {
            Vector3f off = new Vector3f((float) (b.getX() - s.piwot.getX()), (float) (b.getY() - s.piwot.getY()), (float) (b.getZ() - s.piwot.getZ()));
            BlockData bd = b.getBlockData();
            if (s.sprochnialy && b.getType() == s.d.g.log()) bd = Material.STRIPPED_DARK_OAK_LOG.createBlockData();
            final BlockData bdf = bd;
            BlockDisplay d = w.spawn(s.piwot, BlockDisplay.class, e -> {
                e.setPersistent(false);
                e.setBlock(bdf);
                e.setTransformation(new Transformation(new Vector3f(off), new Quaternionf(), new Vector3f(1, 1, 1), new Quaternionf()));
                e.setBrightness(new Display.Brightness(Math.max(b.getLightFromBlocks(), 0), Math.max(b.getLightFromSky(), 12)));
            });
            int idx = s.d.pien.indexOf(b);
            s.disp.add(d);
            s.dOff.add(off);
            s.dSeg.add(idx >= 1 ? idx : n);
            s.dKorona.add(idx < 1);
        }
        s.ciecia = new int[Math.max(2, n)];
        // prawdziwe bloki znikaja u widzow na czas sceny (upadek widzi kazdy w poblizu)
        BlockData air = Material.AIR.createBlockData();
        for (Player v : w.getPlayers()) {
            if (v.getLocation().distanceSquared(s.piwot) > 48 * 48) continue;
            s.widzowie.add(v.getUniqueId());
            for (Block b : bl) v.sendBlockChange(b.getLocation(), air);
        }
    }

    private void ustawKat(Sesja s, double katSt, int ticki) {
        s.kat = katSt;
        Vector3f os = new Vector3f(0, 1, 0).cross(new Vector3f((float) s.kier.getX(), 0, (float) s.kier.getZ())).normalize();
        s.obrot = new Quaternionf().rotateAxis((float) Math.toRadians(katSt), os.x, os.y, os.z);
        aktualizuj(s, ticki);
    }

    private void aktualizuj(Sesja s, int ticki) {
        float gap = (float) SZCZELINA;
        for (int i = 0; i < s.disp.size(); i++) {
            BlockDisplay d = s.disp.get(i);
            if (!d.isValid()) continue;
            Vector3f off = new Vector3f(s.dOff.get(i));
            if (s.faza == Faza.LEZY && s.ciecia != null) {   // rozciete granice rozsuwaja klody wzdluz pnia
                int seg = s.dSeg.get(i), przed = 0;
                for (int g = 1; g < Math.min(seg, s.segmenty()); g++) if (s.ciecia[g] >= 2) przed++;
                if (seg >= s.segmenty()) przed = ciete(s);
                off.add(0, gap * przed, 0);
            }
            Vector3f t = new Quaternionf(s.obrot).transform(off);
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(ticki);
            d.setTransformation(new Transformation(t, new Quaternionf(s.obrot), new Vector3f(1, 1, 1), new Quaternionf()));
        }
    }

    private void odsun(Sesja s) { aktualizuj(s, 3); }

    static final double SZCZELINA = 0.18;  // rozsuniecie klod po przecieciu (widac, gdzie juz cieto)
    static final double TRAF = 0.3;        // tolerancja trafienia w granice klody (wzdluz pnia)

    /** Polozenie granicy g wzdluz pnia tak, jak ja widac (po rozsunieciu przecietych nizej). */
    private double granicaWidoczna(Sesja s, int g) {
        int przed = 0;
        for (int i = 1; i < g; i++) if (s.ciecia[i] >= 2) przed++;
        return g + SZCZELINA * przed;
    }

    private static final double[] KROKI = {6, 11, 18, 27, 38, 51, 66, 80, 92, 999};

    private void pada(Sesja s) {
        if ((s.t - s.padaOd) % 2 != 0) return;
        double cel = KROKI[Math.min(s.krok, KROKI.length - 1)];
        if (cel < s.kat) { s.krok++; cel = KROKI[Math.min(s.krok, KROKI.length - 1)]; }
        s.krok++;
        if (cel >= s.katStop) {
            ustawKat(s, s.katStop, 2);
            uderzenie(s);
            return;
        }
        ustawKat(s, cel, 2);
        if (s.krok == 4) dzwiek(s.piwot, Sound.BLOCK_AZALEA_LEAVES_BREAK, 1f, 0.6f);
    }

    private void uderzenie(Sesja s) {
        Location upad = s.piwot.clone().add(s.kier.clone().multiply(s.d.wys * 0.6));
        World w = upad.getWorld();
        if (s.katStop < 60 && !s.uwagi.contains("zawisl")) {   // zawisa na sasiednim
            s.faza = Faza.WISI;
            s.bezczynnosc = 0;
            w.playSound(upad, Sound.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.BLOCKS, 1.2f, 0.5f);
            w.playSound(upad, Sound.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1f, 0.5f);
            w.spawnParticle(Particle.BLOCK, upad.clone().add(0, s.d.wys * 0.6, 0), 30, 1.0, 0.6, 1.0, s.d.g.lisc().createBlockData());
            plugin.pasek(s.p, "§6Drzewo zawisło na sąsiednim.");
            if (pomiar) plugin.getLogger().info("[drwal] zawisl " + s.p.getName() + " kat=" + Math.round(s.katStop));
            return;
        }
        s.faza = Faza.LEZY;
        s.bezczynnosc = 0;
        w.playSound(upad, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.5f, 0.45f);
        w.playSound(upad, Sound.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.5f, 0.5f);
        w.playSound(upad, Sound.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.BLOCKS, 1.5f, 0.7f);
        w.spawnParticle(Particle.BLOCK, upad, 60, 1.6, 0.4, 1.6, s.d.g.lisc().createBlockData());
        w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, s.piwot.clone().add(s.kier.clone().multiply(2)), 6, 1.5, 0.1, 1.5, 0.01);
        if (s.peka || s.sprochnialy) {
            w.playSound(upad, Sound.BLOCK_BAMBOO_WOOD_BREAK, SoundCategory.BLOCKS, 1.5f, 0.5f);
            w.spawnParticle(Particle.BLOCK, upad, 40, 1.2, 0.3, 1.2, (s.sprochnialy ? Material.BROWN_MUSHROOM_BLOCK : s.d.g.kora()).createBlockData());
        }
        przygniec(s);
        aktualizuj(s, 3);
        if (s.sprochnialy) {   // pusty srodek: pien rozsypuje sie przy upadku — nic do ciecia
            uwaga(s, "sprochnialy");
            Bukkit.getScheduler().runTaskLater(plugin, () -> zakoncz(s, null, true), 30L);
        } else if (s.segmenty() <= 1) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> zakoncz(s, null, true), 20L);
        }
        if (pomiar) plugin.getLogger().info("[drwal] lezy " + s.p.getName() + " kat=" + Math.round(s.katStop));
    }

    /** Pien i korona przygniataja kazdego gracza w pasie upadku (takze drwala, ktory scinal od strony wrebu). */
    private void przygniec(Sesja s) {
        Vector bok = new Vector(-s.kier.getZ(), 0, s.kier.getX());
        int nP = s.segmenty();
        double kr = s.d.korona == null ? 2 : Math.max(s.d.korona.getWidthX(), s.d.korona.getWidthZ()) / 2 + 0.3;
        for (Player v : s.piwot.getWorld().getPlayers()) {
            if (v.getGameMode() == GameMode.CREATIVE || v.getGameMode() == GameMode.SPECTATOR) continue;
            Vector d = v.getLocation().toVector().subtract(s.piwot.toVector());
            double wzd = d.dot(s.kier), pop = Math.abs(d.dot(bok)), dy = d.getY();
            if (dy < -3 || dy > 3 || wzd < 0) continue;
            double max = v.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null ? 20 : v.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            double dmg = 0;
            if (wzd <= nP + 0.3 && pop < 0.9) dmg = Math.max(14, max * 0.75);            // pien
            else if (wzd >= nP - 1.5 && wzd <= s.d.wys + 1 && pop < kr) dmg = Math.max(5, max * 0.3);   // korona
            if (dmg <= 0) continue;
            v.damage(dmg, DamageSource.builder(DamageType.FALLING_BLOCK).withDamageLocation(s.piwot).build());
            v.playSound(v.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.8f);
            if (v.equals(s.p)) uwaga(s, "przygnieciony");
            plugin.getLogger().info("[drwal] przygniecenie " + v.getName() + " dmg=" + Math.round(dmg) + " wzd=" + String.format("%.1f", wzd) + " pop=" + String.format("%.1f", pop) + " hp=" + Math.round(v.getHealth()) + "/" + Math.round(max));
        }
    }

    private void krzyk(Sesja s, String t) {
        for (Player v : s.d.baza.getWorld().getPlayers())
            if (v.getLocation().distanceSquared(s.d.baza.getLocation()) < 30 * 30) plugin.pasek(v, t);
    }

    // ================================================================ wynik
    void zakoncz(Sesja s, String komunikat, boolean scieto) {
        if (s.konczy) return;
        s.konczy = true;
        if (pomiar) { StackTraceElement[] st = new Throwable().getStackTrace();
            plugin.getLogger().info("[drwal] koniec " + s.p.getName() + " faza=" + s.faza + " ciosy=" + s.ciosy + " spala=" + s.spala + " z=" + (st.length > 1 ? st[1].getMethodName() + ":" + st[1].getLineNumber() : "?")); }
        if (s.zad != null) s.zad.cancel();
        sesje.remove(s.p.getUniqueId());
        wSesji.remove(s.p.getUniqueId());
        ppmMs.remove(s.p.getUniqueId());
        zajete.remove(s.d.klucz);
        poKoncu.put(s.p.getUniqueId(), System.currentTimeMillis() + 600);
        if (s.wrabDisp != null) s.wrabDisp.remove();
        String wynik = null;
        if (scieto) wynik = nagroda(s);
        // sprzatanie sceny: displaye znikaja, widzowie odzyskuja drzewo, drwal widzi pniak do odrostu
        for (BlockDisplay d : s.disp) if (d.isValid()) d.remove();
        if (scieto) {
            long kon = System.currentTimeMillis() + s.d.g.odrostSek() * 1000;
            pniaki.computeIfAbsent(s.p.getUniqueId(), u -> new HashMap<>()).put(s.d.klucz, new Pniak(s.d, kon));
            sprochniale.remove(s.d.klucz);
            var nac = nacieta.get(s.p.getUniqueId());
            if (nac != null) nac.remove(s.d.klucz);
            BlockDisplay sp = spaly.remove(s.p.getUniqueId() + s.d.klucz);
            if (sp != null) sp.remove();
            String w = wprawaS(s);
            plugin.zlecenia().ustawFlage(s.p, "drwal_wprawa", String.valueOf(Math.min(99, wprawa(s) + 1)));
            if (w == null) { /* nic */ }
        }
        for (UUID u : s.widzowie) {
            Player v = Bukkit.getPlayer(u);
            if (v == null || !v.getWorld().equals(s.d.baza.getWorld())) continue;
            if (v.equals(s.p) && scieto) pokazPniak(v, s.d);
            else for (Block b : s.d.spadajace()) v.sendBlockChange(b.getLocation(), b.getBlockData());
        }
        if (scieto && !s.widzowie.contains(s.p.getUniqueId())) pokazPniak(s.p, s.d);
        if (komunikat != null) plugin.pasek(s.p, komunikat);
        if (wynik != null) s.p.sendMessage(wynik);
    }

    private String wprawaS(Sesja s) { return plugin.zlecenia().flaga(s.p, "drwal_wprawa"); }

    private int wprawa(Sesja s) {
        try { return Integer.parseInt(wprawaS(s)); } catch (NumberFormatException e) { return 0; }
    }

    /** Jakosc i liczba klod wg calej scinki; zwraca linie czatu. */
    private String nagroda(Sesja s) {
        double q = 1.0;
        if (s.ciosy == 0) q -= 0.4;
        else if (s.ciosy < 4) q -= 0.35;
        else if (s.ciosy == 8) q -= 0.1;
        else if (s.ciosy >= 9) q -= 0.2;
        q -= Math.min(0.25, 0.05 * s.rozrzut);
        if (s.zBoku) q -= 0.15;
        if (s.rzazNizej) q -= 0.15;
        if (s.gdzieChce) q = Math.min(q, 0.55);
        if (s.zawisl) q -= s.uwagi.contains("zawisl") ? 0.1 : 0.0;
        if (s.zawisl && s.faza == Faza.WISI) q = Math.min(q, 0.25);
        q -= Math.min(0.3, 0.04 * s.drzazgi);
        if (s.peka) q = Math.min(q, 0.3);
        q += 0.15 * s.smolne;
        q = Math.max(0, Math.min(1, q));
        int kawalki;
        if (s.sprochnialy) kawalki = 0;
        else if (s.faza == Faza.WISI) kawalki = Math.max(1, s.segmenty() / 2);
        else if (wszystkieCiecia(s)) kawalki = s.segmenty() - (s.okrzesany ? 0 : 1);
        else kawalki = ciete(s);
        if (s.peka) kawalki = kawalki / 2;
        int ekstra = s.smolne >= 1.0 && kawalki > 0 ? 1 : 0;
        int klasa = q >= 0.85 ? 2 : q >= 0.5 ? 1 : 0;
        int wyd = kawalki + ekstra;
        if (klasa == 0) wyd = (int) Math.floor(wyd * 0.5 + ThreadLocalRandom.current().nextDouble());
        String id = s.d.g.przedmiot();
        String wid = klasa == 2 ? id + "_WYBORNE" : id;
        if (wyd > 0) {
            ItemStack it = plugin.przedmioty().zrob(wid, wyd);
            if (it == null && klasa == 2) { plugin.getLogger().warning("Drwal: brak przedmiotu " + wid + " — wydaje " + id); it = plugin.przedmioty().zrob(id, wyd); }
            if (it == null) { plugin.getLogger().warning("Drwal: brak przedmiotu " + id); wyd = 0; }
            else {
                for (ItemStack nad : s.p.getInventory().addItem(it).values()) s.p.getWorld().dropItemNaturally(s.p.getLocation(), nad);
                plugin.zlecenia().zebrano(s.p, PROF, id, wyd);
            }
        }
        double praca = Math.max(1, s.segmenty());
        double xp = plugin.dane().g(1) * (0.4 + 0.6 * q) * praca * (s.sprochnialy ? 0.3 : 1.0);
        double dx = plugin.profesje().daj(s.p, PROF, 1, xp, s.piwot == null ? null : s.piwot.clone());
        String kl = klasa == 2 ? "§6wyborne" : klasa == 1 ? "§adobre" : "§7słabe";
        String nazwa = plugin.dane().nazwa(id);
        StringBuilder sb = new StringBuilder("§2[Drwal] §f");
        sb.append(s.d.g.nazwa().substring(0, 1).toUpperCase()).append(s.d.g.nazwa().substring(1)).append(" ścięty. ");
        if (wyd > 0) sb.append("§f").append(wyd).append("× ").append(nazwa).append(" §7(").append(kl).append("§7)");
        else sb.append("§7Nic z niego nie będzie");
        List<String> pow = new ArrayList<>();
        if (s.sprochnialy) pow.add("pień był spróchniały w środku i rozsypał się");
        if (s.peka) pow.add("pień pękł wzdłuż");
        if (s.gdzieChce) pow.add("drzewo poszło, gdzie chciało");
        if (s.faza == Faza.WISI) pow.add("zostało zawieszone");
        else if (s.zawisl) pow.add("trzeba było je ściągać klinem");
        if (!s.okrzesany && s.faza == Faza.LEZY && wszystkieCiecia(s) && !s.sprochnialy) pow.add("nieokrzesany wierzchołek poszedł na opał");
        if (s.drzazgi >= 3) pow.add("sporo poszło w drzazgi");
        if (s.smolne >= 1.0) pow.add("drewno przesiąkło żywicą");
        if (!pow.isEmpty()) sb.append("§7 — ").append(String.join(", ", pow)).append(".");
        sb.append(" §8(+").append(Math.round(dx)).append(" XP)");
        plugin.getLogger().info("[drwal] wynik " + s.p.getName() + " " + s.d.g.id() + " q=" + String.format("%.2f", q) + " klasa=" + klasa + " wyd=" + wyd + " xp=" + Math.round(dx)
                + " ciosy=" + s.ciosy + " rozrzut=" + s.rozrzut + " gdzieChce=" + s.gdzieChce + " peka=" + s.peka + " zawisl=" + s.zawisl + " faza=" + s.faza
                + " ciete=" + ciete(s) + "/" + Math.max(0, s.segmenty() - 1) + " okrzesany=" + s.okrzesany + " drzazgi=" + s.drzazgi + " smolne=" + String.format("%.2f", s.smolne) + " uwagi=" + s.uwagi);
        return sb.toString();
    }

    private void uwaga(Sesja s, String u) { if (!s.uwagi.contains(u)) s.uwagi.add(u); }

    private void zuzyj(Sesja s) {
        int slot = s.p.getInventory().getHeldItemSlot();
        Przedmioty.zuzyj(s.p, slot);
    }

    private static void dzwiek(Location l, Sound d, float vol, float pitch) {
        l.getWorld().playSound(l, d, SoundCategory.BLOCKS, vol, pitch);   // slychac w poblizu: scinka nie jest cicha
    }

    // ================================================================ pniak i odrost (per konto)
    void pokazPniak(Player p, Drzewo d) {
        BlockData air = Material.AIR.createBlockData();
        for (Block b : d.spadajace()) p.sendBlockChange(b.getLocation(), air);
        p.sendBlockChange(d.baza.getLocation(), d.g.kora().createBlockData());
    }

    private void odrastaj() {
        long teraz = System.currentTimeMillis();
        for (var e : pniaki.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            e.getValue().values().removeIf(pn -> {
                if (pn.koniec > teraz) return false;
                if (p != null && p.getWorld().equals(pn.d.baza.getWorld()) && pn.d.baza.getWorld().isChunkLoaded(pn.d.baza.getX() >> 4, pn.d.baza.getZ() >> 4)) {
                    for (Block b : pn.d.spadajace()) p.sendBlockChange(b.getLocation(), b.getBlockData());
                    p.sendBlockChange(pn.d.baza.getLocation(), pn.d.baza.getBlockData());
                }
                return true;
            });
        }
        pniaki.values().removeIf(Map::isEmpty);
        poKoncu.values().removeIf(t -> t < teraz);
    }

    @EventHandler
    public void naChunk(io.papermc.paper.event.packet.PlayerChunkLoadEvent e) {
        var m = pniaki.get(e.getPlayer().getUniqueId());
        if (m == null || m.isEmpty()) return;
        long teraz = System.currentTimeMillis();
        for (Pniak pn : m.values()) {
            if (pn.koniec <= teraz || !pn.d.baza.getWorld().equals(e.getWorld())) continue;
            if (pn.d.baza.getX() >> 4 != e.getChunk().getX() || pn.d.baza.getZ() >> 4 != e.getChunk().getZ()) continue;
            Player p = e.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) pokazPniak(p, pn.d); }, 2L);
        }
    }

    @EventHandler
    public void naWyjscie(PlayerQuitEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null) zakoncz(s, null, s.faza.ordinal() >= Faza.PADA.ordinal());
    }

    @EventHandler
    public void naSmierc(PlayerDeathEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null) zakoncz(s, null, s.faza.ordinal() >= Faza.PADA.ordinal());
    }

    @EventHandler
    public void naSwiat(PlayerChangedWorldEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null) zakoncz(s, null, s.faza.ordinal() >= Faza.PADA.ordinal());
    }

    @EventHandler
    public void naZamkniecie(WorldUnloadEvent e) {
        String w = e.getWorld().getName();
        for (Sesja s : new ArrayList<>(sesje.values())) if (s.d.baza.getWorld().getName().equals(w)) zakoncz(s, null, false);
        lasy.removeIf(l -> l.swiat.equals(w) && !l.trwaly);   // wzorzec "loch_001_*" zostaje dla nastepnych instancji
        for (var m : pniaki.values()) m.values().removeIf(pn -> pn.d.baza.getWorld().getName().equals(w));
        sprochniale.keySet().removeIf(k -> k.startsWith(w + ":"));
        for (var m : nacieta.values()) m.keySet().removeIf(k -> k.startsWith(w + ":"));
        spaly.entrySet().removeIf(en -> { if (en.getKey().contains(w + ":")) { en.getValue().remove(); return true; } return false; });
    }

    String stan(Player p) {
        Sesja s = sesje.get(p.getUniqueId());
        if (s == null) return "brak sesji; pniaki=" + pniaki.getOrDefault(p.getUniqueId(), Map.of()).size() + " wprawa=" + plugin.zlecenia().flaga(p, "drwal_wprawa");
        return "faza=" + s.faza + " ciosy=" + s.ciosy + " wrabKat=" + (Double.isNaN(s.wrabKat) ? "-" : Math.round(s.wrabKat)) + " postep=" + String.format("%.2f", s.postep)
                + " kat=" + Math.round(s.kat) + " stop=" + Math.round(s.katStop) + " klin=" + s.klin + " ciete=" + (s.ciecia == null ? 0 : ciete(s)) + "/" + Math.max(0, s.segmenty() - 1)
                + " okrzesany=" + s.okrzesany + " segmenty=" + s.segmenty() + " piwot=" + (s.piwot == null ? "-" : String.format("%.2f,%.2f,%.2f", s.piwot.getX(), s.piwot.getY(), s.piwot.getZ()))
                + " kier=" + (s.kier == null ? "-" : String.format("%.2f,%.2f", s.kier.getX(), s.kier.getZ()));
    }

    /** Test: punkt swiata, w ktory celowac, zeby trafic granice nr g lezacego pnia (albo korone dla g=0). */
    String celLezacy(Player p, int g) {
        Sesja s = sesje.get(p.getUniqueId());
        if (s == null || s.faza != Faza.LEZY) return "brak lezacego pnia";
        Location os = s.d.os();
        Vector lok;
        if (g <= 0) {
            if (s.d.korona == null) return "brak korony";
            Vector c = s.d.korona.getCenter();
            lok = new Vector(c.getX() - s.piwot.getX(), c.getY() - s.piwot.getY(), c.getZ() - s.piwot.getZ());
        } else {
            double wz = g >= 100 ? granicaWidoczna(s, g - 100) + 0.5 : granicaWidoczna(s, g);   // 100+n = srodek klody n (drzazgi)
            lok = new Vector(os.getX() - s.piwot.getX(), s.d.baza.getY() + 1 + wz - s.piwot.getY(), os.getZ() - s.piwot.getZ());
            // punkt na powierzchni pnia od strony oka gracza (tam celuje gracz), nie w osi
            Quaternionf inv = new Quaternionf(s.obrot).conjugate();
            Location oko = p.getEyeLocation();
            Vector3f e = new Vector3f((float) (oko.getX() - s.piwot.getX()), (float) (oko.getY() - s.piwot.getY()), (float) (oko.getZ() - s.piwot.getZ()));
            inv.transform(e);
            double hx = e.x - lok.getX(), hz = e.z - lok.getZ(), hl = Math.hypot(hx, hz);
            if (hl > 0.01) lok.add(new Vector(hx / hl * 0.45, 0, hz / hl * 0.45));
        }
        Location w = swiatowy(s, lok);
        return String.format(java.util.Locale.ROOT, "cel=%.3f,%.3f,%.3f bok=%.2f,%.2f", w.getX(), w.getY(), w.getZ(), -s.kier.getZ(), s.kier.getX());
    }

    void zatrzymajWszystko() {
        for (Sesja s : new ArrayList<>(sesje.values())) zakoncz(s, null, false);
        for (BlockDisplay d : spaly.values()) d.remove();
        spaly.clear();
        try { PacketEvents.getAPI().getEventManager().unregisterListener(pakiety); } catch (Throwable ignored) { }
    }
}
