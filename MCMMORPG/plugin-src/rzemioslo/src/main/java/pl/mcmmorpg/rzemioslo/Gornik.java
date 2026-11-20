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
import org.bukkit.entity.ItemDisplay;
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
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Gornik: przodek (sciana z zyla w lochu/na wyspie) -> opukiwanie (PPM: dzwiek prowadzi do zyly, gluchy strop = luzny)
 *  -> obrywka luznego stropu (LPM w strop) -> urabianie wzdluz warstwy (LPM; miekka skala kilofem, twarda klinem)
 *  -> zawal, gdy luzny strop zostal (trzeszczenie, pyl, potem obryw: rani i moze zabic, zasypuje zyle)
 *  -> przebieranie urobku (LPM rozbija bryle, PPM bierze do kosza; plonna w koszu i przekruszona ruda obnizaja wynik)
 *  -> Bryla urobku (+ wyborna), krysztal/klejnot z configu wezla. Moze dac 0. Wyrobisko odnawia sie per konto,
 *  postep (wprawa) per postac (flaga profilu). Wszystko, co widac (ruda, wyrobisko, pekanie, obryw, bryly), widza
 *  wszyscy w poblizu. */
final class Gornik implements Listener {
    static final String PROF = "mining";
    static final String KLIN = "PROF_KLIN_GORNICZY";
    static final String ZRODLO = "wezel_t1_mining";
    static final long ODROST_MS = 240_000;           // wyrobisko u gornika (per konto)
    static final int BEZCZYNNOSC = 20 * 40;
    static final int OSTRZEZENIE = 50;               // ticki trzeszczenia przed obrywem (2,5 s)
    static final double ZASIEG = 7;
    static boolean pomiar;

    record Przodek(String swiat, int x, int y, int z, boolean trwaly) {
        boolean swiatPasuje(String w) { return swiat.endsWith("*") ? w.startsWith(swiat.substring(0, swiat.length() - 1)) : w.equals(swiat); }
        String klucz() { return swiat + ":" + x + ":" + y + ":" + z; }
    }

    enum Faza { URABIANIE, PRZEBIERANIE }
    enum Bryla { RUDA, KRYSZTAL, KLEJNOT, PLONNA }

    static final class Grudka {
        final Bryla typ; boolean rozbita, zabrana, miał;
        Location loc; ItemDisplay widok;
        Grudka(Bryla typ) { this.typ = typ; }
    }

    static final class Kawal {      // blok sciany w pracy
        final Block b; final boolean zyla, twarda;
        double postep; int odbicia; boolean odkryty, urobiony, stracony;
        Kawal(Block b, boolean zyla, boolean twarda) { this.b = b; this.zyla = zyla; this.twarda = twarda; }
    }

    final class Sesja {
        final Player p; final Przodek pr; final Block start; final BlockFace normal; final Vector wzdluz;
        final List<Kawal> zyla = new ArrayList<>();
        final Map<Block, Kawal> obce = new HashMap<>();   // plonna skala tknieta poza zyla
        final List<Block> strop = new ArrayList<>();
        final Set<Block> luzne = new HashSet<>();
        final Map<Block, Integer> obrywka = new HashMap<>();
        final List<Grudka> kupka = new ArrayList<>();
        final List<BlockDisplay> gruz = new ArrayList<>();
        final Set<UUID> widzowie = new HashSet<>();
        final List<String> uwagi = new ArrayList<>();
        final Map<String, Integer> kosz = new LinkedHashMap<>();
        Faza faza = Faza.URABIANIE;
        Dane.Zrodlo zr;
        int t, ostatniLpm = -100, ostatniPpm = -100, bezczynnosc, naprezenie, plonnaWKoszu, nieprzebrane, przekruszone, zasypane,
            zKlinem, opukane, pozaWarstwa, ostrzezenieT = -1, idSzkody;
        Block grozi;
        boolean konczy, zawal, przygnieciony, pewnyZawal;
        BukkitTask zad;
        Sesja(Player p, Przodek pr, Block start, BlockFace normal) {
            this.p = p; this.pr = pr; this.start = start; this.normal = normal;
            this.wzdluz = new Vector(-normal.getModZ(), 0, normal.getModX());
            this.idSzkody = ThreadLocalRandom.current().nextInt(1_000_000, 2_000_000);
        }
    }

    private final Rzemioslo plugin;
    private final List<Przodek> przodki = new ArrayList<>();
    private final Map<UUID, Sesja> sesje = new HashMap<>();
    private final Map<String, UUID> zajete = new HashMap<>();
    private final Map<UUID, Map<String, Wyrobisko>> wyrobiska = new HashMap<>();
    private final Set<UUID> wSesji = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> poKoncu = new ConcurrentHashMap<>();
    private final Map<UUID, Long> ppmMs = new ConcurrentHashMap<>();
    private final Map<UUID, int[]> nastepna = new HashMap<>();   // test: {luzne, dlugosc, uskok(0/1/2=los), pewny zawal 0/1}
    private final PacketListenerAbstract pakiety;
    private final File plik;

    record Wyrobisko(List<Block> bloki, long koniec) {}

    Gornik(Rzemioslo plugin) {
        this.plugin = plugin;
        this.plik = new File(plugin.getDataFolder(), "gornik.yml");
        wczytaj();
        Bukkit.getScheduler().runTaskTimer(plugin, this::odnawiaj, 400L, 400L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::znaki, 40L, 40L);
        pakiety = new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            /** W trakcie pracy kliki naleza do gornika (wzor: Drwal): machniecie = LPM, uzycie = PPM; kopanie anulowane z ack. */
            @Override
            public void onPacketReceive(PacketReceiveEvent e) {
                UUID u = e.getUser().getUUID();
                if (u == null || !wSesji.contains(u)) return;
                var typ = e.getPacketType();
                Runnable akcja;
                int seq = -1;
                if (typ == PacketType.Play.Client.ANIMATION) {
                    Long pp = ppmMs.get(u);
                    // klient po PPM sam macha reka (ANIMATION); przy 10 FPS ~100 ms po pakiecie PPM (klient QA 20.11: 98 ms),
                    // wiec okno 250 ms (rytm LPM to 350 ms) — inaczej opukanie liczylo sie tez jako uderzenie kilofem
                    if (pp != null && System.currentTimeMillis() - pp < 250) { e.setCancelled(true); return; }
                    akcja = () -> { Sesja s = sesje.get(u); if (s != null) lpm(s); };
                } else if (typ == PacketType.Play.Client.USE_ITEM) {
                    var w = new WrapperPlayClientUseItem(e);
                    seq = w.getSequence();
                    ppmMs.put(u, System.currentTimeMillis());
                    akcja = w.getHand() == InteractionHand.MAIN_HAND ? () -> { Sesja s = sesje.get(u); if (s != null) ppm(s); } : null;
                } else if (typ == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
                    var w = new WrapperPlayClientPlayerBlockPlacement(e);
                    seq = w.getSequence();
                    ppmMs.put(u, System.currentTimeMillis());
                    akcja = w.getHand() == InteractionHand.MAIN_HAND ? () -> { Sesja s = sesje.get(u); if (s != null) ppm(s); } : null;
                } else if (typ == PacketType.Play.Client.PLAYER_DIGGING) {
                    var w = new WrapperPlayClientPlayerDigging(e);
                    if (w.getAction() != DiggingAction.START_DIGGING && w.getAction() != DiggingAction.CANCELLED_DIGGING
                            && w.getAction() != DiggingAction.FINISHED_DIGGING) return;
                    seq = w.getSequence();
                    akcja = null;
                } else if (typ == PacketType.Play.Client.INTERACT_ENTITY) {
                    var w = new WrapperPlayClientInteractEntity(e);
                    if (w.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;
                    ppmMs.put(u, System.currentTimeMillis());
                    akcja = w.getHand() == InteractionHand.MAIN_HAND ? () -> { Sesja s = sesje.get(u); if (s != null) ppm(s); } : null;
                } else return;
                if (pomiar) plugin.getLogger().info("[gornik] pakiet " + typ.getName() + " t=" + (System.currentTimeMillis() % 100000));
                e.setCancelled(true);
                if (seq >= 0) e.getUser().sendPacket(new WrapperPlayServerAcknowledgeBlockChanges(seq));
                if (akcja != null) Bukkit.getScheduler().runTask(plugin, akcja);
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(pakiety);
    }

    // ================================================================ rejestr przodkow
    private void wczytaj() {
        if (!plik.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(plik);
        for (String s : y.getStringList("przodki")) {
            String[] a = s.split(":");
            if (a.length == 4) przodki.add(new Przodek(a[0], Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]), true));
        }
    }

    private void zapisz() {
        YamlConfiguration y = new YamlConfiguration();
        List<String> l = new ArrayList<>();
        for (Przodek r : przodki) if (r.trwaly) l.add(r.klucz());
        y.set("przodki", l);
        try { y.save(plik); } catch (Exception ex) { plugin.getLogger().warning("Gornik: zapis gornik.yml: " + ex.getMessage()); }
    }

    String przodek(String swiat, int x, int y, int z, boolean trwaly) {
        if (plugin.dane().wezly.get(ZRODLO) == null) return "brak wezla " + ZRODLO + " w configu (wezly)";
        przodki.removeIf(r -> r.klucz().equals(swiat + ":" + x + ":" + y + ":" + z));
        przodki.add(new Przodek(swiat, x, y, z, trwaly));
        if (trwaly) zapisz();
        return "przodek " + swiat + " " + x + " " + y + " " + z + (trwaly ? " (trwaly)" : "");
    }

    String przodekUsun(String swiat, int x, int y, int z) {
        boolean b = przodki.removeIf(r -> r.klucz().equals(swiat + ":" + x + ":" + y + ":" + z));
        if (b) zapisz();
        return b ? "przodek usuniety" : "brak przodka";
    }

    private Przodek przodekPrzy(Block b) {
        for (Przodek r : przodki) {
            if (!r.swiatPasuje(b.getWorld().getName())) continue;
            if (Math.abs(b.getX() - r.x) <= 3 && Math.abs(b.getY() - r.y) <= 2 && Math.abs(b.getZ() - r.z) <= 3) return r;
        }
        return null;
    }

    private static String kluczP(Przodek r, World w) { return w.getName() + ":" + r.x + ":" + r.y + ":" + r.z; }

    /** Znak przodka: pyl rudy przy scianie (czasteczki, widac dla wszystkich w poblizu). */
    private void znaki() {
        for (Przodek r : przodki) for (World w : Bukkit.getWorlds()) {
            if (!r.swiatPasuje(w.getName()) || !w.isChunkLoaded(r.x >> 4, r.z >> 4)) continue;
            Location l = new Location(w, r.x + 0.5, r.y + 0.5, r.z + 0.5);
            if (l.getNearbyPlayers(20).isEmpty()) continue;
            w.spawnParticle(Particle.DUST, l, 4, 0.55, 0.45, 0.55, 0, new Particle.DustOptions(org.bukkit.Color.fromRGB(176, 104, 58), 0.7f));
        }
    }

    // ================================================================ skala
    static boolean skala(Material m) {
        return Tag.BASE_STONE_OVERWORLD.isTagged(m) || m == Material.COBBLESTONE || m == Material.MOSSY_COBBLESTONE
                || m == Material.SANDSTONE || m == Material.CALCITE || m == Material.DRIPSTONE_BLOCK || twarda(m);
    }

    /** Twarda skala: kilof sie odbija, potrzebny klin (Agricola ks. V: twarda zyla — zelazko/klin pod mlot). */
    static boolean twarda(Material m) {
        String n = m.name();
        return n.contains("DEEPSLATE") || n.contains("BASALT") || n.contains("BLACKSTONE") || m == Material.GRANITE;
    }

    static BlockData ruda(Block b) { return (twarda(b.getType()) ? Material.DEEPSLATE_IRON_ORE : Material.IRON_ORE).createBlockData(); }
    static BlockData wyrobek(Block b) { return (twarda(b.getType()) ? Material.COBBLED_DEEPSLATE : Material.COBBLESTONE).createBlockData(); }

    private static boolean wolne(Block b) { return b.isPassable() && !b.isLiquid(); }

    // ================================================================ wejscie
    @EventHandler(priority = EventPriority.LOWEST)
    public void naKlik(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (wSesji.contains(p.getUniqueId())) {
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.DENY);
            return;
        }
        Action a = e.getAction();
        if ((a != Action.LEFT_CLICK_BLOCK && a != Action.RIGHT_CLICK_BLOCK) || e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (pomiar) plugin.getLogger().info("[gornik] klik " + p.getName() + " " + a + " " + b.getWorld().getName() + " " + b.getX() + "," + b.getY() + "," + b.getZ() + " " + b.getType() + " przodek=" + (przodekPrzy(b) != null));
        if (!skala(b.getType())) return;
        Przodek r = przodekPrzy(b);
        if (r == null) return;
        if (e.getHand() != EquipmentSlot.HAND) { e.setCancelled(true); return; }
        e.setCancelled(true);
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setUseItemInHand(Event.Result.DENY);
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        Sesja s = rozpocznij(p, r, b);
        if (s == null) return;
        if (a == Action.RIGHT_CLICK_BLOCK) { ppmMs.put(p.getUniqueId(), System.currentTimeMillis()); ppm(s); }
        else lpm(s);
    }

    @EventHandler(ignoreCancelled = true)
    public void naUderzenie(BlockDamageEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        if (skala(e.getBlock().getType()) && przodekPrzy(e.getBlock()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void naZniszczenie(BlockBreakEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        if (przodekPrzy(e.getBlock()) != null) e.setCancelled(true);
    }

    /** Narzedzie w rece: "kilof", "klin" albo null. */
    private String narzedzie(Player p) {
        ItemStack reka = p.getInventory().getItemInMainHand();
        String id = Przedmioty.id(reka);
        if (KLIN.equals(id)) return "klin";
        Dane.Narz n = plugin.dane().narzedzia.get(id);
        String rodzaj = plugin.dane().narzedzieProfesji.getOrDefault(PROF, "kilof");
        return n != null && n.rodzaj().equals(rodzaj) ? "kilof" : null;
    }

    Sesja rozpocznij(Player p, Przodek r, Block klik) {
        UUID u = p.getUniqueId();
        if (sesje.containsKey(u)) return null;
        Long po = poKoncu.get(u);
        if (po != null && System.currentTimeMillis() < po) return null;
        World w = klik.getWorld();
        String kl = kluczP(r, w);
        Wyrobisko wy = wyrobiska.getOrDefault(u, Map.of()).get(kl);
        if (wy != null && wy.koniec > System.currentTimeMillis()) {
            plugin.pasek(p, "§7Tę żyłę już wybrałeś. Następną odsłonisz za " + Zielarz.czas((wy.koniec - System.currentTimeMillis()) / 1000 + 1) + ".");
            pokazWyrobisko(p, wy);
            return null;
        }
        UUID kto = zajete.get(kl);
        if (kto != null && !kto.equals(u)) { plugin.pasek(p, "§7Ktoś już pracuje przy tym przodku."); return null; }
        String n = narzedzie(p);
        if (n == null) {
            boolean pusta = p.getInventory().getItemInMainHand().getType().isAir();
            plugin.pasek(p, plugin.przedmioty().najwyzszyTier(p, "kilof") > 0 ? "§7Weź kilof do ręki."
                    : pusta ? "§7Gołymi rękami skały nie ruszysz. Potrzebny kilof." : "§7Tym skały nie ruszysz. Potrzebny kilof (twardą skałę — klin).");
            return null;
        }
        if (n.equals("kilof") && Przedmioty.zepsute(p.getInventory().getItemInMainHand())) { plugin.pasek(p, "§cKilof stępiony. Najpierw go napraw."); return null; }
        Dane.Zrodlo zr = plugin.dane().wezly.get(ZRODLO);
        if (zr == null) { plugin.getLogger().warning("Gornik: brak wezla " + ZRODLO); return null; }
        int lvl = Profesje.poziom(p, PROF);
        int wym = 10 * zr.tier() - 9;
        if (lvl < wym) { plugin.pasek(p, "§7Za mało wprawy w górnictwie (poziom " + wym + ")."); return null; }
        Block P = w.getBlockAt(r.x, r.y, r.z);
        if (p.getLocation().distance(P.getLocation().add(0.5, 0.5, 0.5)) > 4.8) return null;
        BlockFace nf = twarz(P, p);
        if (nf == null) { plugin.pasek(p, "§7Ten przodek jest zawalony — nie ma odsłoniętej ściany."); return null; }
        Sesja s = new Sesja(p, r, P, nf);
        s.zr = zr;
        int[] nast = nastepna.remove(u);
        wytycz(s, nast);
        s.pewnyZawal = nast != null && nast.length > 3 && nast[3] == 1;
        if (s.zyla.isEmpty()) { plugin.pasek(p, "§7W tej ścianie nie ma litej skały."); return null; }
        sesje.put(u, s);
        wSesji.add(u);
        zajete.put(kl, u);
        s.zad = Bukkit.getScheduler().runTaskTimer(plugin, () -> tik(s), 1L, 1L);
        if (wprawa(s) < 5) plugin.pasek(p, "§7Opukaj ścianę (PPM): dźwięczy przy żyle. Opukaj też strop.");
        if (pomiar) plugin.getLogger().info("[gornik] start " + p.getName() + " " + kl + " normal=" + nf + " zyla=" + opisZyly(s) + " luzne=" + s.luzne.size());
        return s;
    }

    /** Strona sciany zwrocona do gracza (blok przed nia wolny). */
    private static BlockFace twarz(Block P, Player p) {
        BlockFace best = null; double bd = 1e9;
        for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block przed = P.getRelative(f);
            if (!wolne(przed)) continue;
            double d = przed.getLocation().add(0.5, 0, 0.5).distanceSquared(p.getLocation());
            if (d < bd) { bd = d; best = f; }
        }
        return best;
    }

    /** Zyla: 3-6 blokow wzdluz warstwy od punktu przodka, z mozliwym uskokiem o blok; luzny strop w obszarze pracy. */
    private void wytycz(Sesja s, int[] nast) {
        ThreadLocalRandom R = ThreadLocalRandom.current();
        int dl = nast != null && nast[1] > 0 ? nast[1] : 3 + R.nextInt(4);
        int kier = R.nextBoolean() ? 1 : -1;
        int uskokNa = nast != null && nast[2] == 0 ? -1 : (nast != null && nast[2] == 1 ? 1 + R.nextInt(Math.max(1, dl - 1)) : (R.nextDouble() < 0.6 ? 1 + R.nextInt(Math.max(1, dl - 1)) : -1));
        int dy = R.nextBoolean() ? 1 : -1;
        Block b = s.start;
        for (int i = 0; i < dl; i++) {
            if (i > 0) {
                b = b.getRelative((int) s.wzdluz.getX() * kier, 0, (int) s.wzdluz.getZ() * kier);
                if (i == uskokNa) {
                    Block alt = b.getRelative(0, dy, 0);
                    if (zywaSciana(alt, s.normal)) b = alt; else { Block alt2 = b.getRelative(0, -dy, 0); if (zywaSciana(alt2, s.normal)) b = alt2; }
                }
            }
            if (!zywaSciana(b, s.normal)) {
                if (i == 0) return;
                if (kier == 1 && s.zyla.size() < 3) { kier = -1; b = s.start; i = 0; continue; }   // druga strona punktu
                break;
            }
            final Block fb = b;
            if (s.zyla.stream().noneMatch(k -> k.b.equals(fb))) s.zyla.add(new Kawal(b, true, twarda(b.getType())));
        }
        // strop nad stanowiskiem: pierwszy staly blok nad wolnym polem przed kazdym blokiem zyly
        Set<Block> st = new HashSet<>();
        for (Kawal k : s.zyla) for (int off = 0; off <= 1; off++) {
            Block przed = k.b.getRelative(s.normal, 1 + off);
            for (int h = 0; h <= 4; h++) {
                Block x = przed.getRelative(0, h, 0);
                if (!wolne(x)) { if (h >= 1 && skala(x.getType())) st.add(x); break; }
            }
        }
        s.strop.addAll(st);
        int luzne = nast != null && nast[0] >= 0 ? nast[0] : (R.nextDouble() < 0.6 ? 1 : 0) + (R.nextDouble() < 0.2 ? 1 : 0);
        List<Block> kand = new ArrayList<>(s.strop);
        java.util.Collections.shuffle(kand);
        for (int i = 0; i < Math.min(luzne, kand.size()); i++) s.luzne.add(kand.get(i));
    }

    private static boolean zywaSciana(Block b, BlockFace n) { return skala(b.getType()) && wolne(b.getRelative(n)); }

    private String opisZyly(Sesja s) {
        StringBuilder sb = new StringBuilder();
        for (Kawal k : s.zyla) sb.append(k.b.getX()).append(',').append(k.b.getY()).append(',').append(k.b.getZ()).append(k.twarda ? "T" : "M").append(' ');
        return sb.toString().trim();
    }

    // ================================================================ tik
    private void tik(Sesja s) {
        if (s.konczy) return;
        s.t++;
        s.bezczynnosc++;
        Player p = s.p;
        if (!p.isOnline() || p.isDead() || !p.getWorld().equals(s.start.getWorld())
                || p.getLocation().distance(s.start.getLocation().add(0.5, 0.5, 0.5)) > ZASIEG) {
            zakoncz(s, "§7Odszedłeś od przodka.", s.faza == Faza.PRZEBIERANIE);
            return;
        }
        if (s.ostrzezenieT >= 0) {
            int dt = s.t - s.ostrzezenieT;
            if (dt % 8 == 0 && dt < OSTRZEZENIE) trzeszczy(s);
            if (dt >= OSTRZEZENIE) obryw(s, s.grozi, false);
        }
        if (s.bezczynnosc > BEZCZYNNOSC) {
            if (s.faza == Faza.URABIANIE && !s.kupka.isEmpty()) przebieranie(s, "§7Przodek stoi. Przebierz urobek.");
            else zakoncz(s, "§7Zostawiłeś robotę.", s.faza == Faza.PRZEBIERANIE);
        }
        if (s.t % 20 == 0) dolaczWidzow(s);
    }

    /** Nowi gracze w poblizu widza rude i wyrobisko tak jak gornik. */
    private void dolaczWidzow(Sesja s) {
        for (Player v : s.start.getWorld().getPlayers()) {
            if (s.widzowie.contains(v.getUniqueId()) || v.getLocation().distanceSquared(s.start.getLocation()) > 48 * 48) continue;
            s.widzowie.add(v.getUniqueId());
            for (Kawal k : s.zyla) if (k.urobiony) v.sendBlockChange(k.b.getLocation(), wyrobek(k.b)); else if (k.odkryty) v.sendBlockChange(k.b.getLocation(), ruda(k.b));
            for (Kawal k : s.obce.values()) if (k.urobiony) v.sendBlockChange(k.b.getLocation(), wyrobek(k.b));
        }
    }

    private void pokazWszystkim(Sesja s, Block b, BlockData bd) {
        for (Player v : s.start.getWorld().getPlayers()) {
            if (v.getLocation().distanceSquared(b.getLocation()) > 48 * 48) continue;
            s.widzowie.add(v.getUniqueId());
            v.sendBlockChange(b.getLocation(), bd);
        }
        // Klik, ktory zaczal sesje, idzie przez PlayerInteractEvent: anulowane zdarzenie odsyla klikajacemu prawdziwy blok
        // PO naszym sendBlockChange (klient QA 20.11: gornik widzial kamien, widz rude) -> powtorz gornikowi tick pozniej.
        Player g = s.p;
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (g.isOnline() && sesje.get(g.getUniqueId()) == s) g.sendBlockChange(b.getLocation(), bd); }, 1L);
    }

    private void peknij(Sesja s, Block b, float postep) {
        for (Player v : s.start.getWorld().getPlayers())
            if (v.getLocation().distanceSquared(b.getLocation()) < 32 * 32) v.sendBlockDamage(b.getLocation(), Math.max(0f, Math.min(1f, postep)), s.idSzkody + b.hashCode() % 1000);
    }

    // ================================================================ gesty
    private Block celBlok(Sesja s) {
        RayTraceResult r = s.p.rayTraceBlocks(4.5, FluidCollisionMode.NEVER);
        return r == null ? null : r.getHitBlock();
    }

    void ppm(Sesja s) {
        if (s.konczy || s.t - s.ostatniPpm < 6) return;
        s.ostatniPpm = s.t;
        s.bezczynnosc = 0;
        if (s.faza == Faza.PRZEBIERANIE) { wezGrudke(s); return; }
        if (narzedzie(s.p) == null) { plugin.pasek(s.p, "§7Opukuje się kilofem albo klinem."); return; }
        Block b = celBlok(s);
        if (b == null) return;
        s.p.swingMainHand();
        Location l = b.getLocation().add(0.5, 0.5, 0.5);
        if (s.strop.contains(b)) {
            s.opukane++;
            if (s.luzne.contains(b)) {   // gluchy: luzna skala (NIOSH: "drummy")
                dzwiek(l, Sound.BLOCK_GRAVEL_HIT, 1f, 0.5f);
                dzwiek(l, Sound.BLOCK_ROOTED_DIRT_HIT, 0.8f, 0.6f);
                l.getWorld().spawnParticle(Particle.FALLING_DUST, l.clone().add(0, -0.55, 0), 6, 0.3, 0, 0.3, 0, Material.GRAVEL.createBlockData());
                if (wprawa(s) < 5) plugin.pasek(s.p, "§6Strop brzmi głucho — luźna skała. Zbij ją (LPM), stojąc z boku.");
            } else {
                dzwiek(l, Sound.BLOCK_STONE_HIT, 1f, 1.7f);
                if (wprawa(s) < 5) plugin.pasek(s.p, "§7Strop dzwoni — lity.");
            }
            if (pomiar) plugin.getLogger().info("[gornik] opuk-strop " + s.p.getName() + " " + b.getX() + "," + b.getY() + "," + b.getZ() + " luzny=" + s.luzne.contains(b));
            return;
        }
        if (!skala(b.getType()) && zKawala(s, b) == null) return;
        s.opukane++;
        int d = odlegloscOdZyly(s, b);
        if (d == 0) {
            Kawal k = zKawala(s, b);
            dzwiek(l, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 0.7f, 1.4f);
            dzwiek(l, Sound.BLOCK_STONE_HIT, 1f, 1.3f);
            if (k != null && !k.odkryty && !k.urobiony) {
                k.odkryty = true;
                pokazWszystkim(s, b, ruda(b));
                l.getWorld().spawnParticle(Particle.BLOCK, l, 8, 0.3, 0.3, 0.3, ruda(b));
                if (wprawa(s) < 5) plugin.pasek(s.p, "§6Dźwięczy jasno — żyła. Urabiaj (LPM)" + (k.twarda ? ", ale skała twarda: potrzebny klin." : "."));
            }
        } else {
            dzwiek(l, Sound.BLOCK_STONE_HIT, 1f, d == 1 ? 1.15f : 0.7f);
            if (d == 1) dzwiek(l, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 0.25f, 1.0f);
            if (wprawa(s) < 5) plugin.pasek(s.p, d == 1 ? "§7Słychać pobrzękiwanie — żyła tuż obok." : "§7Głucha skała.");
        }
        if (pomiar) plugin.getLogger().info("[gornik] opuk " + s.p.getName() + " " + b.getX() + "," + b.getY() + "," + b.getZ() + " d=" + d);
    }

    private Kawal zKawala(Sesja s, Block b) {
        for (Kawal k : s.zyla) if (k.b.equals(b)) return k;
        return null;
    }

    private int odlegloscOdZyly(Sesja s, Block b) {
        int best = 99;
        for (Kawal k : s.zyla) {
            if (k.urobiony || k.stracony) continue;
            int d = Math.abs(k.b.getX() - b.getX()) + Math.abs(k.b.getY() - b.getY()) + Math.abs(k.b.getZ() - b.getZ());
            best = Math.min(best, d);
        }
        return best;
    }

    void lpm(Sesja s) {
        String n = narzedzie(s.p);
        int rytm = "klin".equals(n) ? 9 : 7;
        if (s.konczy || s.t - s.ostatniLpm < rytm) return;
        s.ostatniLpm = s.t;
        s.bezczynnosc = 0;
        s.p.swingMainHand();
        if (s.faza == Faza.PRZEBIERANIE) { rozbij(s); return; }
        if (n == null) { plugin.pasek(s.p, "§7Tym skały nie urobisz."); return; }
        Block b = celBlok(s);
        if (b == null) return;
        Location l = b.getLocation().add(0.5, 0.5, 0.5);
        if (s.strop.contains(b)) { zbijaj(s, b, n); return; }
        Kawal k = zKawala(s, b);
        if (k == null) {
            if (!skala(b.getType()) || przodekPrzy(b) == null || !wolne(b.getRelative(s.normal))) return;
            k = s.obce.computeIfAbsent(b, x -> new Kawal(x, false, twarda(x.getType())));
        }
        if (k.urobiony || k.stracony) return;
        if (!"klin".equals(n)) zuzyj(s);
        double przyrost;
        if (k.twarda) {
            if ("kilof".equals(n)) {   // kilof odbija sie od twardej skaly
                k.odbicia++;
                dzwiek(l, Sound.BLOCK_ANVIL_LAND, 0.35f, 2.0f);
                l.getWorld().spawnParticle(Particle.CRIT, l.clone().add(s.normal.getDirection().multiply(0.55)), 6, 0.1, 0.1, 0.1, 0.05);
                zuzyj(s); zuzyj(s);
                if (k.zyla && k.odbicia >= 3 && !k.stracony) {
                    k.stracony = true;
                    uwaga(s, "skruszona");
                    pokazWszystkim(s, b, wyrobek(b));
                    l.getWorld().spawnParticle(Particle.BLOCK, l, 20, 0.3, 0.3, 0.3, Material.GRAVEL.createBlockData());
                    plugin.pasek(s.p, "§cKilof skruszył rudę w miał — ten kawałek przepadł.");
                    sprawdzKoniec(s);
                } else if (wprawa(s) < 5 || k.odbicia == 1) plugin.pasek(s.p, "§7Kilof się odbija — twarda skała. Tu trzeba klina.");
                return;
            }
            przyrost = 1.0 / 5;
            s.zKlinem++;
        } else przyrost = "klin".equals(n) ? 1.0 / 8 : 1.0 / 4;
        k.postep += przyrost;
        dzwiek(l, k.twarda ? Sound.BLOCK_DEEPSLATE_HIT : Sound.BLOCK_STONE_HIT, 1f, "klin".equals(n) ? 0.8f : 1.0f);
        if ("klin".equals(n)) dzwiek(l, Sound.BLOCK_ANVIL_PLACE, 0.25f, 1.6f);
        l.getWorld().spawnParticle(Particle.BLOCK, l.clone().add(s.normal.getDirection().multiply(0.55)), 6, 0.2, 0.2, 0.2, k.zyla && k.odkryty ? ruda(b) : b.getBlockData());
        peknij(s, b, (float) k.postep);
        if (k.postep < 0.999) return;
        // blok urobiony
        k.urobiony = true;
        peknij(s, b, 0f);
        pokazWszystkim(s, b, wyrobek(b));
        dzwiek(l, k.twarda ? Sound.BLOCK_DEEPSLATE_BREAK : Sound.BLOCK_STONE_BREAK, 1f, 0.9f);
        l.getWorld().spawnParticle(Particle.BLOCK, l, 18, 0.3, 0.3, 0.3, k.zyla ? ruda(b) : b.getBlockData());
        ThreadLocalRandom R = ThreadLocalRandom.current();
        if (k.zyla) {
            for (int i = 0; i < 2; i++) dodajGrudke(s, R.nextDouble() < 0.75 ? Bryla.RUDA : Bryla.PLONNA);
            if (R.nextDouble() < s.zr.szansaPoboczny()) dodajGrudke(s, Bryla.KRYSZTAL);
            if (R.nextDouble() < s.zr.szansaRzadki()) dodajGrudke(s, Bryla.KLEJNOT);
        } else {
            s.pozaWarstwa++;
            dodajGrudke(s, Bryla.PLONNA);
            if (wprawa(s) < 5) plugin.pasek(s.p, "§7Płonna skała — żyła idzie warstwą obok. Opukaj.");
        }
        s.naprezenie++;
        if (pomiar) plugin.getLogger().info("[gornik] urobiony " + s.p.getName() + " " + b.getX() + "," + b.getY() + "," + b.getZ() + " zyla=" + k.zyla + " naprezenie=" + s.naprezenie);
        // zawal: luzny strop, ktorego nie zbito, puszcza po kilku urobionych blokach
        if (s.ostrzezenieT < 0 && !s.luzne.isEmpty() && s.naprezenie >= 2 && (s.pewnyZawal || R.nextDouble() < 0.5)) ostrzez(s, s.luzne.iterator().next());
        sprawdzKoniec(s);
    }

    private void sprawdzKoniec(Sesja s) {
        if (s.faza != Faza.URABIANIE || s.ostrzezenieT >= 0) return;
        boolean zostalo = s.zyla.stream().anyMatch(k -> !k.urobiony && !k.stracony);
        if (zostalo) return;
        if (s.kupka.isEmpty()) zakoncz(s, s.zawal ? "§cZłoże zasypane. Nic z niego nie będzie." : s.uwagi.contains("skruszona") ? "§cRuda skruszona w miał. Nic z niej nie będzie." : "§7Żyła wybrana do płonnej skały. Nic z niej nie będzie.", true);
        else przebieranie(s, "§6Żyła wybrana. Przebierz urobek: LPM rozbija bryłę, PPM bierze do kosza.");
    }

    // ================================================================ strop i zawal
    private void zbijaj(Sesja s, Block b, String n) {
        Location l = b.getLocation().add(0.5, 0.5, 0.5);
        if (!s.luzne.contains(b)) {   // lity strop: tylko odpryski
            dzwiek(l, Sound.BLOCK_STONE_HIT, 0.8f, 1.6f);
            return;
        }
        int u = s.obrywka.merge(b, 1, Integer::sum);
        dzwiek(l, Sound.BLOCK_GRAVEL_HIT, 1f, 0.6f);
        l.getWorld().spawnParticle(Particle.FALLING_DUST, l.clone().add(0, -0.55, 0), 8, 0.3, 0, 0.3, 0, Material.GRAVEL.createBlockData());
        if (u < 2) return;
        if (s.grozi != null && s.grozi.equals(b)) { s.ostrzezenieT = -1; s.grozi = null; }   // zbita w trakcie trzeszczenia
        s.luzne.remove(b);
        uwaga(s, "obrywka");
        obryw(s, b, true);
    }

    private void ostrzez(Sesja s, Block b) {
        s.grozi = b;
        s.ostrzezenieT = s.t;
        trzeszczy(s);
        for (Player v : b.getWorld().getPlayers())
            if (v.getLocation().distanceSquared(b.getLocation()) < 16 * 16) plugin.pasek(v, "§cStrop trzeszczy! Odsuń się!");
        if (pomiar) plugin.getLogger().info("[gornik] ostrzezenie " + s.p.getName() + " " + b.getX() + "," + b.getY() + "," + b.getZ());
    }

    private void trzeszczy(Sesja s) {
        if (s.grozi == null) return;
        Location l = s.grozi.getLocation().add(0.5, 0, 0.5);
        dzwiek(l, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.5f);
        dzwiek(l, Sound.BLOCK_POINTED_DRIPSTONE_LAND, 0.5f, 0.6f);
        l.getWorld().spawnParticle(Particle.FALLING_DUST, l, 10, 0.5, 0, 0.5, 0, Material.GRAVEL.createBlockData());
    }

    /** Spadajacy gruz spod luznego stropu. obrywka = zbita celowo (mniejszy kawal), inaczej zawal (rani i moze zabic, zasypuje). */
    private void obryw(Sesja s, Block b, boolean obrywka) {
        if (b == null) return;
        if (!obrywka) { s.ostrzezenieT = -1; s.grozi = null; s.luzne.remove(b); s.zawal = true; uwaga(s, "zawal"); }
        World w = b.getWorld();
        Location gora = b.getLocation().add(0.5, 0, 0.5);
        int dno = b.getY() - 1;
        while (dno > b.getY() - 6 && wolne(w.getBlockAt(b.getX(), dno - 1, b.getZ()))) dno--;
        final int spadek = b.getY() - dno, dnoF = dno;
        int ile = obrywka ? 1 : 4;
        for (int i = 0; i < ile; i++) {
            float ox = obrywka ? -0.35f : (float) (ThreadLocalRandom.current().nextDouble(-1.1, 0.4));
            float oz = obrywka ? -0.35f : (float) (ThreadLocalRandom.current().nextDouble(-1.1, 0.4));
            float sk = obrywka ? 0.7f : 0.8f;
            BlockData bd = (i % 2 == 0 ? Material.GRAVEL : (twarda(b.getType()) ? Material.COBBLED_DEEPSLATE : Material.COBBLESTONE)).createBlockData();
            BlockDisplay d = w.spawn(gora.clone().add(0, -0.3, 0), BlockDisplay.class, e -> {
                e.setPersistent(false);
                e.setBlock(bd);
                e.setTransformation(new Transformation(new Vector3f(ox, 0, oz), new Quaternionf(), new Vector3f(sk, sk, sk), new Quaternionf()));
            });
            s.gruz.add(d);
            final int sp = spadek;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!d.isValid()) return;
                d.setInterpolationDelay(0);
                d.setInterpolationDuration(5);
                d.setTransformation(new Transformation(new Vector3f(ox, -sp + 0.3f, oz), new Quaternionf().rotateY((float) (ThreadLocalRandom.current().nextDouble() * 3)), new Vector3f(sk, sk * 0.6f, sk), new Quaternionf()));
            }, 2L);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            dzwiek(gora, Sound.BLOCK_GRAVEL_BREAK, 1.5f, 0.6f);
            dzwiek(gora, obrywka ? Sound.BLOCK_STONE_BREAK : Sound.ENTITY_GENERIC_EXPLODE, obrywka ? 1f : 0.6f, 0.5f);
            w.spawnParticle(Particle.BLOCK, gora.clone().add(0, -spadek + 0.5, 0), obrywka ? 15 : 50, obrywka ? 0.4 : 1.2, 0.3, obrywka ? 0.4 : 1.2, Material.GRAVEL.createBlockData());
            w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, gora.clone().add(0, -spadek + 0.8, 0), obrywka ? 2 : 8, 1.0, 0.2, 1.0, 0.01);
            przygniec(s, b, dnoF, obrywka);
            if (!obrywka) zasyp(s, b);
        }, 7L);
    }

    private void przygniec(Sesja s, Block b, int dno, boolean obrywka) {
        double cx = b.getX() + 0.5, cz = b.getZ() + 0.5;
        for (Player v : b.getWorld().getPlayers()) {
            if (v.getGameMode() == GameMode.CREATIVE || v.getGameMode() == GameMode.SPECTATOR) continue;
            double dy = v.getLocation().getY() - dno;
            if (dy < -1 || dy > b.getY() - dno + 0.5) continue;
            double r = Math.hypot(v.getLocation().getX() - cx, v.getLocation().getZ() - cz);
            double max = v.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null ? 20 : v.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            double dmg = 0;
            if (obrywka) { if (r < 0.8) dmg = Math.max(4, max * 0.2); }
            else if (r < 1.0) dmg = Math.max(14, max * 0.75);
            else if (r < 1.9) dmg = Math.max(5, max * 0.3);
            if (dmg <= 0) continue;
            v.damage(dmg, DamageSource.builder(DamageType.FALLING_BLOCK).withDamageLocation(b.getLocation()).build());
            if (v.equals(s.p)) { s.przygnieciony = true; uwaga(s, "przygnieciony"); }
            plugin.getLogger().info("[gornik] przygniecenie " + v.getName() + " dmg=" + Math.round(dmg) + " r=" + String.format(Locale.ROOT, "%.1f", r) + " hp=" + Math.round(v.getHealth()) + "/" + Math.round(max) + (obrywka ? " (obrywka)" : " (zawal)"));
        }
    }

    /** Zawal zasypuje nieurobiona zyle w promieniu 3 i polowe kupki urobku w promieniu 2,5. */
    private void zasyp(Sesja s, Block b) {
        if (s.konczy) return;
        for (Kawal k : s.zyla) {
            if (k.urobiony || k.stracony) continue;
            if (Math.abs(k.b.getX() - b.getX()) + Math.abs(k.b.getZ() - b.getZ()) <= 3) { k.stracony = true; s.zasypane++; pokazWszystkim(s, k.b, Material.GRAVEL.createBlockData()); }
        }
        Location c = b.getLocation().add(0.5, 0, 0.5);
        int i = 0;
        for (Grudka g : s.kupka) {
            if (g.zabrana || g.loc == null) continue;
            if (Math.hypot(g.loc.getX() - c.getX(), g.loc.getZ() - c.getZ()) < 2.5 && (i++ % 2 == 0)) {
                g.zabrana = true; g.miał = true;
                if (g.widok != null) g.widok.remove();
                s.zasypane++;
            }
        }
        if (s.p.isOnline() && !s.p.isDead()) plugin.pasek(s.p, "§cZawał! Gruz zasypał część żyły" + (s.zasypane > 0 ? " i urobku." : "."));
        if (pomiar) plugin.getLogger().info("[gornik] zawal " + s.p.getName() + " zasypane=" + s.zasypane);
        Bukkit.getScheduler().runTaskLater(plugin, () -> sprawdzKoniec(s), 10L);
    }

    // ================================================================ urobek i przebieranie (cobbing)
    private Location miejsceKupki(Sesja s, int nr) {
        List<Kawal> z = s.zyla;
        Block srodek = z.get(z.size() / 2).b;
        Block przed = srodek.getRelative(s.normal);
        World w = przed.getWorld();
        int y = przed.getY();
        while (y > przed.getY() - 4 && wolne(w.getBlockAt(przed.getX(), y - 1, przed.getZ()))) y--;
        Vector n = s.normal.getDirection();
        double wz = ((nr % 6) - 2.5) * 0.42, od = 0.35 + (nr / 6) * 0.45;
        return new Location(w, srodek.getX() + 0.5 + n.getX() * (0.5 + od) + s.wzdluz.getX() * wz, y + 0.12,
                srodek.getZ() + 0.5 + n.getZ() * (0.5 + od) + s.wzdluz.getZ() * wz);
    }

    private void dodajGrudke(Sesja s, Bryla t) {
        if (s.kupka.size() >= 18) return;
        Grudka g = new Grudka(t);
        g.loc = miejsceKupki(s, s.kupka.size());
        g.widok = widokGrudki(g.loc, new ItemStack(twarda(s.start.getType()) ? Material.COBBLED_DEEPSLATE : Material.COBBLESTONE), 0.42f);
        s.kupka.add(g);
    }

    private ItemDisplay widokGrudki(Location l, ItemStack it, float sk) {
        return l.getWorld().spawn(l, ItemDisplay.class, e -> {
            e.setPersistent(false);
            e.setItemStack(it);
            e.setTransformation(new Transformation(new Vector3f(0, sk / 2, 0), new Quaternionf().rotateY((float) ThreadLocalRandom.current().nextDouble(6.28)), new Vector3f(sk, sk, sk), new Quaternionf()));
        });
    }

    private void przebieranie(Sesja s, String komunikat) {
        s.faza = Faza.PRZEBIERANIE;
        s.bezczynnosc = 0;
        plugin.pasek(s.p, komunikat);
        if (pomiar) plugin.getLogger().info("[gornik] przebieranie " + s.p.getName() + " grudek=" + s.kupka.size());
    }

    /** Grudka, w ktora patrzy gracz (promien oka, <= 0,35 bloku od srodka grudki). */
    private Grudka celGrudka(Sesja s) {
        Location oko = s.p.getEyeLocation();
        Vector kier = oko.getDirection().normalize();
        Grudka best = null; double bd = 0.35;
        for (Grudka g : s.kupka) {
            if (g.zabrana || g.loc == null) continue;
            Vector c = g.loc.toVector().add(new Vector(0, 0.2, 0)).subtract(oko.toVector());
            double wz = c.dot(kier);
            if (wz < 0 || wz > 4.5) continue;
            double pop = c.clone().subtract(kier.clone().multiply(wz)).length();
            if (pop < bd) { bd = pop; best = g; }
        }
        return best;
    }

    private void rozbij(Sesja s) {
        Grudka g = celGrudka(s);
        if (g == null) return;
        if (narzedzie(s.p) == null) { plugin.pasek(s.p, "§7Bryłę rozbija się kilofem."); return; }
        Location l = g.loc.clone().add(0, 0.2, 0);
        if (!g.rozbita) {
            g.rozbita = true;
            dzwiek(l, Sound.BLOCK_STONE_BREAK, 0.8f, 1.3f);
            l.getWorld().spawnParticle(Particle.BLOCK, l, 8, 0.1, 0.1, 0.1, Material.COBBLESTONE.createBlockData());
            if (g.widok != null) g.widok.remove();
            g.widok = widokGrudki(g.loc, wyglad(s, g.typ), g.typ == Bryla.PLONNA ? 0.32f : 0.4f);
            if (wprawa(s) < 5) plugin.pasek(s.p, g.typ == Bryla.PLONNA ? "§7Płonna skała — zostaw ją." : "§6" + nazwaB(s, g.typ) + " — weź do kosza (PPM).");
            return;
        }
        if (g.typ == Bryla.PLONNA) { dzwiek(l, Sound.BLOCK_GRAVEL_HIT, 0.6f, 1.2f); return; }
        // drugi cios w rozbita rude: przekruszona na mial
        g.zabrana = true; g.miał = true;
        s.przekruszone++;
        if (g.widok != null) g.widok.remove();
        dzwiek(l, Sound.BLOCK_GRAVEL_BREAK, 0.9f, 1.4f);
        l.getWorld().spawnParticle(Particle.BLOCK, l, 14, 0.15, 0.05, 0.15, Material.GRAVEL.createBlockData());
        plugin.pasek(s.p, "§c" + nazwaB(s, g.typ) + " przekruszony na miał.");
        sprawdzPrzebrane(s);
    }

    private void wezGrudke(Sesja s) {
        Grudka g = celGrudka(s);
        if (g == null) return;
        g.zabrana = true;
        if (g.widok != null) g.widok.remove();
        dzwiek(g.loc, Sound.BLOCK_STONE_PLACE, 0.6f, 1.4f);
        if (g.typ == Bryla.PLONNA) {
            s.plonnaWKoszu++;
            if (g.rozbita || wprawa(s) < 5) plugin.pasek(s.p, "§7Płonna skała w koszu — kowal tego nie przetopi.");
        } else {
            if (!g.rozbita) s.nieprzebrane++;
            s.kosz.merge(idB(s, g.typ), 1, Integer::sum);
        }
        sprawdzPrzebrane(s);
    }

    private void sprawdzPrzebrane(Sesja s) {
        boolean zostalo = s.kupka.stream().anyMatch(g -> !g.zabrana && !(g.rozbita && g.typ == Bryla.PLONNA));
        if (!zostalo) Bukkit.getScheduler().runTaskLater(plugin, () -> zakoncz(s, null, true), 8L);
    }

    @EventHandler
    public void naKucniecie(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s == null || s.konczy || s.ostrzezenieT >= 0) return;
        if (s.faza == Faza.URABIANIE && !s.kupka.isEmpty()) przebieranie(s, "§6Kończysz urabianie. Przebierz urobek: LPM rozbija, PPM do kosza.");
        else if (s.faza == Faza.URABIANIE) zakoncz(s, "§7Odchodzisz od przodka.", false);
        else zakoncz(s, null, true);
    }

    private String idB(Sesja s, Bryla t) {
        return switch (t) { case KRYSZTAL -> s.zr.poboczny(); case KLEJNOT -> s.zr.rzadki(); default -> s.zr.glowny(); };
    }

    private String nazwaB(Sesja s, Bryla t) { return plugin.dane().nazwa(idB(s, t)); }

    private ItemStack wyglad(Sesja s, Bryla t) {
        if (t == Bryla.PLONNA) return new ItemStack(Material.GRAVEL);
        ItemStack it = plugin.przedmioty().zrob(idB(s, t), 1);
        return it != null ? it : new ItemStack(t == Bryla.RUDA ? Material.RAW_IRON : Material.AMETHYST_SHARD);
    }

    // ================================================================ wynik
    void zakoncz(Sesja s, String komunikat, boolean nagroda) {
        if (s.konczy) return;
        s.konczy = true;
        if (s.zad != null) s.zad.cancel();
        UUID u = s.p.getUniqueId();
        sesje.remove(u);
        wSesji.remove(u);
        ppmMs.remove(u);
        World w = s.start.getWorld();
        String kl = kluczP(s.pr, w);
        zajete.remove(kl);
        poKoncu.put(u, System.currentTimeMillis() + 600);
        for (Kawal k : s.zyla) peknij(s, k.b, 0f);
        for (Kawal k : s.obce.values()) peknij(s, k.b, 0f);
        String wynik = nagroda && !s.p.isDead() ? nagroda(s) : null;
        for (Grudka g : s.kupka) if (g.widok != null && g.widok.isValid()) g.widok.remove();
        List<BlockDisplay> gruz = new ArrayList<>(s.gruz);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { for (BlockDisplay d : gruz) if (d.isValid()) d.remove(); }, 40L);
        List<Block> rob = new ArrayList<>();
        for (Kawal k : s.zyla) if (k.urobiony || k.stracony) rob.add(k.b);
        for (Kawal k : s.obce.values()) if (k.urobiony) rob.add(k.b);
        boolean pracowal = !rob.isEmpty();
        Wyrobisko wy = new Wyrobisko(rob, System.currentTimeMillis() + ODROST_MS);
        if (pracowal) {
            wyrobiska.computeIfAbsent(u, x -> new HashMap<>()).put(kl, wy);
            plugin.zlecenia().ustawFlage(s.p, "gornik_wprawa", String.valueOf(Math.min(99, wprawa(s) + 1)));
        }
        // widzowie odzyskuja sciane; gornik widzi swoje wyrobisko do odnowy
        for (UUID x : s.widzowie) {
            Player v = Bukkit.getPlayer(x);
            if (v == null || !v.getWorld().equals(w)) continue;
            for (Kawal k : s.zyla) v.sendBlockChange(k.b.getLocation(), k.b.getBlockData());
            for (Kawal k : s.obce.values()) v.sendBlockChange(k.b.getLocation(), k.b.getBlockData());
        }
        if (pracowal && s.p.isOnline()) Bukkit.getScheduler().runTaskLater(plugin, () -> { if (s.p.isOnline()) pokazWyrobisko(s.p, wy); }, 2L);
        if (komunikat != null) plugin.pasek(s.p, komunikat);
        if (wynik != null) s.p.sendMessage(wynik);
        if (pomiar) plugin.getLogger().info("[gornik] koniec " + s.p.getName() + " nagroda=" + nagroda + " faza=" + s.faza);
    }

    private String nagroda(Sesja s) {
        int urobione = (int) s.zyla.stream().filter(k -> k.urobiony).count();
        double q = 1.0;
        q -= Math.min(0.45, 0.15 * s.plonnaWKoszu);
        q -= Math.min(0.3, 0.1 * s.nieprzebrane);
        q -= Math.min(0.3, 0.1 * s.pozaWarstwa);       // robota w plonnej skale obok warstwy: urobek zmieszany
        q -= Math.min(0.3, 0.1 * s.przekruszone);
        if (s.uwagi.contains("skruszona")) q -= 0.15;
        if (s.zawal) q -= 0.2;
        q = Math.max(0, Math.min(1, q));
        int klasa = q >= 0.85 ? 2 : q >= 0.5 ? 1 : 0;
        String gl = s.zr.glowny();
        StringBuilder lista = new StringBuilder();
        int lacznie = 0;
        for (var e : s.kosz.entrySet()) {
            String id = e.getKey();
            int ile = e.getValue();
            if (id.equals(gl) && klasa == 0) ile = (int) Math.floor(ile * 0.5 + ThreadLocalRandom.current().nextDouble());
            if (ile <= 0) continue;
            String wid = id.equals(gl) && klasa == 2 ? id + "_WYBORNE" : id;
            ItemStack it = plugin.przedmioty().zrob(wid, ile);
            if (it == null && !wid.equals(id)) { plugin.getLogger().warning("Gornik: brak przedmiotu " + wid + " — wydaje " + id); it = plugin.przedmioty().zrob(id, ile); }
            if (it == null) { plugin.getLogger().warning("Gornik: brak przedmiotu " + id); continue; }
            for (ItemStack nad : s.p.getInventory().addItem(it).values()) s.p.getWorld().dropItemNaturally(s.p.getLocation(), nad);
            plugin.zlecenia().zebrano(s.p, PROF, id, ile);
            lacznie += ile;
            if (lista.length() > 0) lista.append(", ");
            lista.append(ile).append("× ").append(plugin.dane().nazwa(id));
        }
        double xp = plugin.dane().g(s.zr.tier()) * (0.4 + 0.6 * q) * Math.max(1, urobione) * (s.zawal ? 0.5 : 1.0);
        double dx = plugin.profesje().daj(s.p, PROF, s.zr.tier(), xp, s.start.getLocation());
        String kl = klasa == 2 ? "§6czysty, wyborny" : klasa == 1 ? "§adobry" : "§7zanieczyszczony";
        StringBuilder sb = new StringBuilder("§6[Górnik] §f");
        if (lacznie > 0) sb.append(lista).append(" §7(").append(kl).append("§7)");
        else sb.append("§7Nic z tej żyły nie będzie");
        List<String> pow = new ArrayList<>();
        if (s.zawal) pow.add("strop się obwalił" + (s.zasypane > 0 ? " i zasypał część" : ""));
        if (s.uwagi.contains("skruszona")) pow.add("kilof skruszył rudę w twardej skale");
        if (s.pozaWarstwa >= 2) pow.add("sporo pracy poszło w płonną skałę obok warstwy");
        if (s.plonnaWKoszu > 0) pow.add("w koszu płonna skała");
        if (s.nieprzebrane > 0) pow.add("bryły nieprzebrane");
        if (s.przekruszone > 0) pow.add("ruda przekruszona na miał");
        if (s.uwagi.contains("obrywka") && !s.zawal) pow.add("luźny strop zbity w porę");
        if (!pow.isEmpty()) sb.append("§7 — ").append(String.join(", ", pow)).append(".");
        sb.append(" §8(+").append(Math.round(dx)).append(" XP)");
        plugin.getLogger().info("[gornik] wynik " + s.p.getName() + " q=" + String.format(Locale.ROOT, "%.2f", q) + " klasa=" + klasa + " kosz=" + s.kosz + " wydano=" + lacznie
                + " urobione=" + urobione + "/" + s.zyla.size() + " poza=" + s.pozaWarstwa + " plonnaWKoszu=" + s.plonnaWKoszu + " nieprzebrane=" + s.nieprzebrane
                + " przekruszone=" + s.przekruszone + " zawal=" + s.zawal + " zasypane=" + s.zasypane + " klin=" + s.zKlinem + " opukane=" + s.opukane + " xp=" + Math.round(dx) + " uwagi=" + s.uwagi);
        return sb.toString();
    }

    private void uwaga(Sesja s, String u) { if (!s.uwagi.contains(u)) s.uwagi.add(u); }

    private int wprawa(Sesja s) {
        try { return Integer.parseInt(plugin.zlecenia().flaga(s.p, "gornik_wprawa")); } catch (NumberFormatException e) { return 0; }
    }

    private void zuzyj(Sesja s) { Przedmioty.zuzyj(s.p, s.p.getInventory().getHeldItemSlot()); }

    private static void dzwiek(Location l, Sound d, float vol, float pitch) { l.getWorld().playSound(l, d, SoundCategory.BLOCKS, vol, pitch); }

    // ================================================================ wyrobisko i odnowa (per konto)
    void pokazWyrobisko(Player p, Wyrobisko wy) {
        for (Block b : wy.bloki) p.sendBlockChange(b.getLocation(), wyrobek(b));
    }

    private void odnawiaj() {
        long teraz = System.currentTimeMillis();
        for (var e : wyrobiska.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            e.getValue().values().removeIf(wy -> {
                if (wy.koniec > teraz) return false;
                if (p != null) for (Block b : wy.bloki) if (p.getWorld().equals(b.getWorld())) p.sendBlockChange(b.getLocation(), b.getBlockData());
                return true;
            });
        }
        wyrobiska.values().removeIf(Map::isEmpty);
        poKoncu.values().removeIf(t -> t < teraz);
    }

    @EventHandler
    public void naChunk(io.papermc.paper.event.packet.PlayerChunkLoadEvent e) {
        var m = wyrobiska.get(e.getPlayer().getUniqueId());
        if (m == null || m.isEmpty()) return;
        long teraz = System.currentTimeMillis();
        for (Wyrobisko wy : m.values()) {
            if (wy.koniec <= teraz || wy.bloki.isEmpty()) continue;
            Block b = wy.bloki.get(0);
            if (!b.getWorld().equals(e.getWorld()) || b.getX() >> 4 != e.getChunk().getX() || b.getZ() >> 4 != e.getChunk().getZ()) continue;
            Player p = e.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) pokazWyrobisko(p, wy); }, 2L);
        }
    }

    @EventHandler
    public void naWyjscie(PlayerQuitEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null) zakoncz(s, null, false);
    }

    @EventHandler
    public void naSmierc(PlayerDeathEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null) { plugin.getLogger().info("[gornik] smierc " + e.getPlayer().getName() + " w sesji (zawal=" + s.zawal + ")"); zakoncz(s, null, false); }
    }

    @EventHandler
    public void naSwiat(PlayerChangedWorldEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null) zakoncz(s, null, false);
    }

    @EventHandler
    public void naZamkniecie(WorldUnloadEvent e) {
        String w = e.getWorld().getName();
        for (Sesja s : new ArrayList<>(sesje.values())) if (s.start.getWorld().getName().equals(w)) zakoncz(s, null, false);
        przodki.removeIf(r -> r.swiat.equals(w) && !r.trwaly);
        for (var m : wyrobiska.values()) m.keySet().removeIf(k -> k.startsWith(w + ":"));
    }

    // ================================================================ test / diagnostyka
    String nastepna(Player p, int luzne, int dlugosc, int uskok, int zawal) {
        nastepna.put(p.getUniqueId(), new int[]{luzne, dlugosc, uskok, zawal});
        return "nastepna sesja " + p.getName() + ": luzne=" + luzne + " dlugosc=" + dlugosc + " uskok=" + uskok + " pewnyZawal=" + (zawal == 1);
    }

    String odnow(Player p) {
        var m = wyrobiska.remove(p.getUniqueId());
        if (m != null) for (Wyrobisko wy : m.values()) for (Block b : wy.bloki) if (p.getWorld().equals(b.getWorld())) p.sendBlockChange(b.getLocation(), b.getBlockData());
        return "odnowione wyrobiska " + p.getName() + ": " + (m == null ? 0 : m.size());
    }

    String stan(Player p) {
        Sesja s = sesje.get(p.getUniqueId());
        if (s == null) return "brak sesji; wyrobiska=" + wyrobiska.getOrDefault(p.getUniqueId(), Map.of()).size() + " wprawa=" + plugin.zlecenia().flaga(p, "gornik_wprawa");
        StringBuilder sb = new StringBuilder("faza=" + s.faza + " normal=" + s.normal + " zyla=");
        for (Kawal k : s.zyla) sb.append(k.b.getX()).append(',').append(k.b.getY()).append(',').append(k.b.getZ())
                .append(k.twarda ? "T" : "M").append(k.urobiony ? "u" : k.stracony ? "x" : k.odkryty ? "o" : "-").append(' ');
        sb.append("| luzne=");
        for (Block b : s.luzne) sb.append(b.getX()).append(',').append(b.getY()).append(',').append(b.getZ()).append(' ');
        sb.append("| strop=").append(s.strop.size()).append(" grozi=").append(s.grozi != null).append(" zawal=").append(s.zawal).append(" kupka=");
        for (Grudka g : s.kupka) if (!g.zabrana) sb.append(String.format(Locale.ROOT, "%.2f,%.2f,%.2f", g.loc.getX(), g.loc.getY(), g.loc.getZ()))
                .append(g.typ.name().charAt(0)).append(g.rozbita ? "r" : "-").append(' ');
        sb.append("| kosz=").append(s.kosz).append(" plonnaWKoszu=").append(s.plonnaWKoszu).append(" naprezenie=").append(s.naprezenie).append(" zasypane=").append(s.zasypane).append(" przygnieciony=").append(s.przygnieciony);
        return sb.toString();
    }

    void zatrzymajWszystko() {
        for (Sesja s : new ArrayList<>(sesje.values())) zakoncz(s, null, false);
        try { PacketEvents.getAPI().getEventManager().unregisterListener(pakiety); } catch (Throwable ignored) { }
    }
}
