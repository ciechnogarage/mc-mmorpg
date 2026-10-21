package pl.mcmmorpg.rzemioslo;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHurtAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Zielarz w swiecie (wycinek pionowy, MCMMORPG/profesje/ZIELARZ.md): PPM w kepe -> przyklek przy powiekszonej roslinie
 *  widocznej tylko dla gracza; celownik wybiera czesc rosliny (i narzedzie w dloni), LPM = ciecie/kopanie,
 *  PPM klik = zerwanie, PPM przytrzymane = wyrywanie/rozcieranie (puszczenie z pakietu, co do ticka).
 *  Okaz losowany z ziarna kepy (per gracz), konsekwencje realne: odrost zalezy od tego, co zostawiles. */
final class Zielarz implements Listener {
    static final String PROF = "herbalism";
    static final Color PODSWIETL = Color.fromRGB(255, 240, 170);
    /** Wyrwanie pedu: tyle tikow swiadomego trzymania (od 12. ped widocznie sie nagina — mozna puscic). */
    static final int WYRWANIE = 34;
    /** Korzen idzie tylko, gdy napiecie jest w pasmie: za slabo = stoi, za mocno = trzeszczy i grozi urwaniem. */
    static final double PASMO_OD = 0.45, PASMO_DO = 0.8;
    /** Urwanie: zapas nad pasmem na reakcje gracza + 6 tikow, zanim serwer zauwazy puszczenie (symulacja 20.10). */
    static final double ZERWANIE = 1.1;
    static final Particle.DustOptions GRUDKA = new Particle.DustOptions(Color.fromRGB(93, 63, 42), 0.45f);   // drobne grudki, nie kwadraty
    private static final NamespacedKey K_ZAMROZ = new NamespacedKey("rzemioslo", "zielarz_zamroz");

    /** Stan kepy dla gracza: ten sam okaz az do odrostu. */
    static final class Stan {
        long ziarno = ThreadLocalRandom.current().nextLong();
        long odrostDo;
        boolean rozpoznana;
        boolean chciwie;   // ostatnio scieto wiecej niz pol kepy: nastepny okaz bedzie oslabiony
        boolean slaba;     // ten okaz odrasta po chciwym zbiorze (mniej pedow, czesc cienkich)
        /** Rzadka odmiana ~1/30 okazow: okazala kepa, wyborne sztuki z naddatkiem. */
        boolean okazala() { return new Random(ziarno ^ 0x0CA1AL).nextInt(30) == 0; }
        boolean trwaly(long teraz) { return odrostDo > teraz || rozpoznana || chciwie || slaba; }
    }

    final class Sesja {
        final Player p;
        final Block blok;
        final String gatunek;
        final Stan stan;
        final ZRoslina r;
        final Random rnd;
        Location stoi;
        int slotSierpa = -1, ostatniPpm;
        ItemStack sierp;
        int t, ostatniLpm = -99, ostatniaAkcja, zajetyDo;
        ZRoslina.Czesc hover, chwyt;
        boolean trzyma, chwytZalatwiony, konczy, ukryjZnow;
        int trzymaOd;
        String widok = "";
        // kozlek
        double napiecie, postep;
        final boolean[] poluzowane = new boolean[4], boczne = new boolean[4];
        boolean trzeszczy, zaczep1, zaczep2, wykopany, rozpoznana, oko, naderwany;
        boolean czekaNaPuszczenie = true;   // PPM trzymany od klikniecia w kepe nie chwyta rosliny
        volatile long upuszczenieMs;        // Q: klient macha reka po upuszczeniu — to nie jest ciecie
        boolean zranienie, zlaZiemia;
        int zleKopanie, ostatniRuchKom = -99;
        int ostatniHover = -99;
        final int partia = ThreadLocalRandom.current().nextInt();   // nierozpoznane liscie z roznych zbiorow nie lacza sie w stos
        // wynik
        final Map<String, int[]> zebrane = new LinkedHashMap<>();   // nazwa -> [slabe, dobre, wyborne]
        final List<String> uwagi = new ArrayList<>();
        double xp;
        int chore;
        boolean zniszczona, okaz;
        BukkitTask zad;
        // v7
        final boolean[] sonda = new boolean[4];   // pole opukane lopatka
        boolean mokro, przyWodzie;
        double zerwanie = ZERWANIE;
        int naderwania, wprawa, mokrych, skazone, ciecTotal, ciecWszystkich, pokruszone;
        boolean roztarte;   // w tej sesji rozcierano lisc: liscie pogniecione, bez wybornych
        int hoverOd = -1; ZRoslina.Czesc hoverC;
        final long startMs = System.currentTimeMillis();

        Sesja(Player p, Block blok, String gatunek, Stan stan, ZRoslina r) {
            this.p = p; this.blok = blok; this.gatunek = gatunek; this.stan = stan; this.r = r;
            this.rnd = new Random(stan.ziarno);
        }
    }

    private final Rzemioslo plugin;
    private final Map<String, String> kepy = new HashMap<>();
    private final Map<UUID, Sesja> sesje = new java.util.concurrent.ConcurrentHashMap<>();   // czytana tez z watku pakietow
    private final Map<UUID, Long> poKoncu = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Stan>> stany = new HashMap<>();
    private final File plikKep;
    private final NamespacedKey kJakosc, kZamiana, kSlot, kUchwyt, kPartia;
    private final PacketListenerAbstract pakiety;
    final ZZielnik zielnik;
    final ZSuszarnia suszarnia;

    Zielarz(Rzemioslo plugin) {
        this.plugin = plugin;
        this.plikKep = new File(plugin.getDataFolder(), "zielarz-kepy.yml");
        this.kJakosc = new NamespacedKey(plugin, "jakosc");
        this.kZamiana = new NamespacedKey(plugin, "zielarz_zamiana");
        this.kPartia = new NamespacedKey(plugin, "partia");
        this.kSlot = new NamespacedKey(plugin, "zielarz_slot");
        this.kUchwyt = new NamespacedKey(plugin, "zielarz_uchwyt");
        this.zielnik = new ZZielnik(plugin);
        this.suszarnia = new ZSuszarnia(plugin);
        Bukkit.getPluginManager().registerEvents(suszarnia, plugin);
        Bukkit.getScheduler().runTaskLater(plugin, suszarnia::pokazWszystko, 40L);
        wczytajKepy();
        wczytajStany();
        Bukkit.getScheduler().runTaskTimer(plugin, this::zapiszStany, 6000L, 6000L);
        pakiety = new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            /** W trakcie zbioru kliki naleza do zielarza: pakiety machniecia/uzycia/kopania sa anulowane, zanim powstanie
             *  zdarzenie Bukkita (inne pluginy — bron, zaklecia, wymagania MMOItems — ich nie widza). Klient dostaje ack
             *  sekwencji, zeby cofnal przewidziane zmiany blokow. */
            @Override
            public void onPacketReceive(PacketReceiveEvent e) {
                UUID u = e.getUser().getUUID();
                if (u == null) return;
                if (!sesje.containsKey(u)) {
                    // tuz po zbiorze gracz zwykle jeszcze trzyma PPM/LPM — te kliki tez nalezaly do rosliny, nie do broni
                    Long po = poKoncu.get(u);
                    if (po == null || System.currentTimeMillis() > po) return;
                    var t0 = e.getPacketType();
                    if (t0 == PacketType.Play.Client.ANIMATION || t0 == PacketType.Play.Client.USE_ITEM || t0 == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
                        e.setCancelled(true);
                        if (t0 == PacketType.Play.Client.USE_ITEM) e.getUser().sendPacket(new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges(new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem(e).getSequence()));
                        if (t0 == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) e.getUser().sendPacket(new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges(new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement(e).getSequence()));
                    }
                    return;
                }
                var typ = e.getPacketType();
                Runnable akcja = null;
                int seq = -1;
                if (typ == PacketType.Play.Client.ANIMATION) {
                    Sesja sq = sesje.get(u);
                    if (sq != null && System.currentTimeMillis() - sq.upuszczenieMs < 200) { e.setCancelled(true); return; }   // Q macha reka
                    akcja = () -> { Sesja s = sesje.get(u); if (s != null) lpm(s); };
                } else if (typ == PacketType.Play.Client.USE_ITEM) {
                    var w = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem(e);
                    seq = w.getSequence();
                    if (w.getHand() == com.github.retrooper.packetevents.protocol.player.InteractionHand.MAIN_HAND)
                        akcja = () -> { Sesja s = sesje.get(u); if (s != null) ppmWcisniety(s); };
                } else if (typ == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
                    var w = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement(e);
                    seq = w.getSequence();
                    if (w.getHand() == com.github.retrooper.packetevents.protocol.player.InteractionHand.MAIN_HAND)
                        akcja = () -> { Sesja s = sesje.get(u); if (s != null) { s.ukryjZnow = true; ppmWcisniety(s); } };
                } else if (typ == PacketType.Play.Client.PLAYER_DIGGING) {
                    var w = new WrapperPlayClientPlayerDigging(e);
                    if (w.getAction() == DiggingAction.RELEASE_USE_ITEM) return;
                    if (w.getAction() == DiggingAction.DROP_ITEM || w.getAction() == DiggingAction.DROP_ITEM_STACK) {
                        Sesja sq = sesje.get(u);
                        if (sq != null) { sq.upuszczenieMs = System.currentTimeMillis(); Bukkit.getScheduler().runTask(plugin, () -> pokazWDloni(sq, sq.widok)); }   // klient sam czysci reke po Q
                    }
                    seq = w.getSequence();
                    akcja = () -> { Sesja s = sesje.get(u); if (s != null) s.ukryjZnow = true; };
                } else if (typ == PacketType.Play.Client.INTERACT_ENTITY) {
                    var w = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity(e);
                    if (w.getAction() != com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction.ATTACK
                            && w.getHand() == com.github.retrooper.packetevents.protocol.player.InteractionHand.MAIN_HAND)
                        akcja = () -> { Sesja s = sesje.get(u); if (s != null) ppmWcisniety(s); };
                } else {
                    return;
                }
                e.setCancelled(true);
                if (seq >= 0) {
                    int sq = seq;
                    e.getUser().sendPacket(new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges(sq));
                }
                if (akcja != null) Bukkit.getScheduler().runTask(plugin, akcja);
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(pakiety);
    }

    private String opisEncji(int id) {
        for (World w : Bukkit.getWorlds()) for (org.bukkit.entity.Entity en : w.getEntities()) if (en.getEntityId() == id)
            return en.getType() + " " + en.getName() + " @" + en.getLocation().toVector();
        return "?" + id;
    }

    // ================================================================ kepy (rejestr miejsc)
    /** Klucz kepy w YAML: '|' zamiast ';' (stary zapis '_'/'~' psul swiaty z podkresleniem w nazwie, np. lochy). */
    static String doYaml(String k) { return k.replace(';', '|'); }
    static String zYaml(String k) { return k.contains("|") ? k.replace('|', ';') : k.replace('_', ';').replace('~', '-'); }

    static String klucz(Block b) { return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ(); }

    private void wczytajKepy() {
        kepy.clear();
        if (!plikKep.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(plikKep);
        var sek = y.getConfigurationSection("kepy");
        if (sek != null) for (String k : sek.getKeys(false)) kepy.put(zYaml(k), sek.getString(k));
    }

    private void zapiszKepy() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, String> e : kepy.entrySet()) y.set("kepy." + doYaml(e.getKey()), e.getValue());
        try { y.save(plikKep); } catch (IOException ex) { plugin.getLogger().warning("zielarz-kepy.yml: " + ex); }
    }

    static final List<String> GATUNKI = List.of("dziurawiec", "kozlek", "czosnek", "konwalia");
    /** Skala, w ktorej karty zblizenia pokrywaja blok kepy w swiecie (start przyblizenia / koniec oddalenia). */
    static final Map<String, Float> SKALA_SWIATA = Map.of("dziurawiec", 0.62f, "kozlek", 0.72f, "czosnek", 0.9f, "konwalia", 0.9f);
    /** Blok kepy (Nexo) zwykly i po zbiorze — ten sam pixel-art co karty zblizenia. */
    static final Map<String, String[]> BLOK = Map.of("dziurawiec", new String[]{"zielarz_dziurawiec", "zielarz_dziurawiec_scieta"},
            "kozlek", new String[]{"zielarz_kozlek", "zielarz_kozlek_wykopany"},
            "czosnek", new String[]{"zielarz_szerokolistna", "zielarz_szerokolistna_zerwana"},
            "konwalia", new String[]{"zielarz_szerokolistna", "zielarz_szerokolistna_zerwana"});

    static org.bukkit.block.data.BlockData wygladBloku(String gatunek, boolean poZbiorze, Block b) {
        try {
            String[] ids = BLOK.get(gatunek);
            if (ids != null) { var d = com.nexomc.nexo.api.NexoBlocks.blockData(ids[poZbiorze ? 1 : 0]); if (d != null) return d; }
        } catch (Throwable ignored) { }
        return poZbiorze ? Material.SHORT_GRASS.createBlockData() : b.getBlockData();
    }

    String ustawKepe(Block b, String gatunek) {
        if (gatunek == null) { kepy.remove(klucz(b)); zapiszKepy(); return "usunieto"; }
        if (!GATUNKI.contains(gatunek)) return "nieznany gatunek; " + GATUNKI;
        kepy.put(klucz(b), gatunek);
        zapiszKepy();
        return "kepa " + gatunek + " @ " + klucz(b);
    }

    int liczbaKep() { return kepy.size(); }

    // ================================================================ wejscie
    @EventHandler(priority = EventPriority.LOWEST)
    public void naKlik(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Sesja s = sesje.get(p.getUniqueId());
        if (s != null) {
            Action a = e.getAction();
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.DENY);
            s.ukryjZnow = true;
            if (e.getHand() == EquipmentSlot.OFF_HAND) return;
            // LPM liczymy z machniecia reki (PlayerArmSwingEvent) — klikniecie w blok za roslina nie zawsze przychodzi
            if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) ppmWcisniety(s);
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        String g = kepy.get(klucz(e.getClickedBlock()));
        if (g == null) return;
        e.setCancelled(true);
        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);
        if (e.getHand() != EquipmentSlot.HAND) return;
        rozpocznij(p, e.getClickedBlock(), g);
    }

    /** Prog poziomu zielarstwa dla gatunku (dziurawiec od 1, szerokolistne od 5, kozlek od 10). */
    static final Map<String, Integer> PROG = Map.of("dziurawiec", 1, "czosnek", 5, "konwalia", 5, "kozlek", 10);
    /** Od tego poziomu mistrzostwo "oko": linia ciecia pokazuje strefy jakosci kolorem. */
    static final int MISTRZ_OKO = 50;

    private static String gatunekOkazu(String gatunek, Stan st) {
        if (!gatunek.equals("czosnek") && !gatunek.equals("konwalia")) return gatunek;
        boolean zamiana = new Random(st.ziarno ^ 0x50B0L).nextDouble() < 0.3;
        return zamiana ? (gatunek.equals("czosnek") ? "konwalia" : "czosnek") : gatunek;
    }

    private static String kluczRozpoznania(Block b, Stan st) { return "_rozp_" + klucz(b) + "_" + st.ziarno; }

    private Stan stan(Player p, Block b) {
        // odrost per KONTO: druga postac nie zbiera tej samej kepy jeszcze raz; rozpoznanie i wiedza sa per postac (zielnik)
        return stany.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).computeIfAbsent(klucz(b), k -> new Stan());
    }

    void rozpocznij(Player p, Block b, String gatunek) {
        if (sesje.containsKey(p.getUniqueId()) || plugin.zbieranie().wTrakcie(p) || p.isInsideVehicle()
                || p.getGameMode() == GameMode.SPECTATOR) return;
        double odl = p.getLocation().distance(b.getLocation().add(0.5, 0, 0.5));
        if (odl < 1.1) { plugin.pasek(p, "§fCofnij się o krok. Stoisz w samej roślinie."); return; }
        int prog = PROG.getOrDefault(gatunek, 1), poz = Profesje.poziom(p, PROF);
        if (poz < prog && !p.hasPermission("rzemioslo.zielarz.bezprogow")) { plugin.pasek(p, "§fNie znasz się jeszcze na tej roślinie. §7Zielarstwo " + prog + " (masz " + poz + ")."); return; }
        if (odl > 3.2) { plugin.pasek(p, "§7Podejdź bliżej, żeby pochylić się nad rośliną."); return; }   // stale, bliskie zblizenie
        Stan st = stan(p, b);
        long teraz = System.currentTimeMillis();
        if (st.odrostDo > teraz) {
            String gk = gatunek;
            Bukkit.getScheduler().runTask(plugin, () -> p.sendBlockChange(b.getLocation(), wygladBloku(gk, true, b)));
            plugin.pasek(p, "§7Ta kępa jeszcze odrasta. Wróć za §f" + czas((st.odrostDo - teraz) / 1000) + "§7.");
            return;
        }
        // po odroscie nowy okaz, ale rozpoznanie kepy zostaje (gracz juz wie, co tu rosnie)
        if (st.odrostDo != 0) { Stan nowy = new Stan(); nowy.slaba = st.chciwie; stany.get(p.getUniqueId()).put(klucz(b), nowy); st = nowy; }
        String rodzaj = plugin.dane().narzedzieProfesji.getOrDefault(PROF, "sierp");
        Przedmioty.Znalezione sierp = plugin.przedmioty().najlepsze(p, rodzaj, 1);
        if (sierp == null) {
            plugin.pasek(p, plugin.kom("brak-narzedzia", "{narzedzie}", Przedmioty.nazwaRodzaju(rodzaj), "{tier}", "I"));
            return;
        }
        if (Przedmioty.zepsute(sierp.item())) { plugin.pasek(p, "§cSierp jest stępiony do cna. Napraw go, zanim weźmiesz się za zioła."); return; }
        // miejsce przyklekniecia: ~1,7 bloku przed kepa od strony gracza, na gruncie
        Location o = b.getLocation().add(0.5, 0, 0.5);
        Vector przod = o.toVector().subtract(p.getLocation().toVector()).setY(0);
        if (przod.lengthSquared() < 0.01) przod = p.getLocation().getDirection().setY(0);
        przod.normalize();
        Location miejsce = null;
        for (double d : new double[]{1.7, 1.5, 2.0, 1.3}) {
            Location c = o.clone().subtract(przod.clone().multiply(d));
            Block nogi = c.getBlock(), glowa = nogi.getRelative(0, 1, 0), pod = nogi.getRelative(0, -1, 0);
            if (nogi.isPassable() && glowa.isPassable() && pod.getType().isSolid()) { miejsce = c; break; }
        }
        if (miejsce == null) miejsce = p.getLocation();
        miejsce.setY(Math.floor(o.getY()));
        Vector doRosliny = o.toVector().subtract(miejsce.toVector()).setY(0);
        if (doRosliny.lengthSquared() > 0.01) przod = doRosliny.normalize();

        ZRoslina r = new ZRoslina(plugin, p, o, przod);
        gatunek = gatunekOkazu(gatunek, st);   // sobowtor: kepa to zwykle jej gatunek, ale po odroscie bywa ten drugi
        Sesja s = new Sesja(p, b, gatunek, st, r);
        s.rozpoznana = zielnik.ma(p, kluczRozpoznania(b, st));
        s.oko = Profesje.poziom(p, PROF) >= MISTRZ_OKO;
        s.wprawa = zielnik.licznik(p, "_wprawa_" + grupa(gatunek));
        s.mokro = mokro(b);
        s.przyWodzie = przyWodzie(b) || b.getWorld().hasStorm() && b.getLightFromSky() >= 14;
        if (gatunek.equals("kozlek")) s.zerwanie = s.przyWodzie ? 1.08 : ZERWANIE;
        sesje.put(p.getUniqueId(), s);
        s.sierp = sierp.item().clone();
        s.slotSierpa = sierp.slot();
        p.sendBlockChange(b.getLocation(), Material.AIR.createBlockData());
        // gracz stoi pochylony nad roslina, unieruchomiony (siedzenie odpada: jadacy klient zamienia PPM w machniecie
        // i serwer nie odroznia przytrzymania od ciecia — zmierzone klientem QA 18.10)
        // bez teleportu (kamera nie skacze): gracz zostaje, gdzie stal, i zastyga nad roslina
        s.stoi = p.getLocation().clone();
        zamroz(p, true);
        r.skalaStartu = SKALA_SWIATA.getOrDefault(gatunek, 0.7f);   // karty rodza sie w rozmiarze kepy w swiecie
        try {
            switch (gatunek) {
                case "dziurawiec" -> budujDziurawiec(s);
                case "kozlek" -> budujKozlek(s);
                default -> budujLiscie(s, gatunek.equals("czosnek"));
            }
        } catch (Throwable t) {
            plugin.getLogger().severe("Zielarz: budowa rosliny " + gatunek + ": " + t);
            zakoncz(s, "§cBłąd rośliny.");
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.skala(1.14f, 6); }, 1L);   // przyblizenie z wyraznym odbiciem
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.skala(0.97f, 3); }, 8L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.skala(1f, 3); }, 12L);
        // kamera gracza nie jest obracana na sile (walczyla z mysza); celowanie zostaje w rekach gracza
        p.playSound(p.getLocation(), Sound.BLOCK_GRASS_STEP, SoundCategory.PLAYERS, 0.8f, 0.8f);
        plugin.pasek(p, "§f" + nazwaKepy(s) + " §7· Shift: odejdź");
        s.r.zaslon(1.5f, gatunek.equals("kozlek") ? 1.6f : 1.2f);
        String rada = wskazowka(s);
        if (rada == null && s.mokro && gatunek.equals("dziurawiec") && zielnik.rada3(p, "rosa")) rada = "Mokre ziele|Mokre ziele. Zielarze mówią, że takie pleśnieje w suszarni.";
        if (rada == null && s.stan.okazala()) rada = "Okazała kępa|Okazała kępa. Rzadko się taką spotyka.";
        if (rada != null && zielnik.rada(p, widocznyGatunek(s)) <= 3) {   // najwyzej 3 razy: krotko w podtytule (miesci sie przy GUI 3-4), cala rada na czacie
            String[] rr = rada.split("\\|", 2);
            p.showTitle(net.kyori.adventure.title.Title.title(Component.empty(), Component.text(rr[0], net.kyori.adventure.text.format.NamedTextColor.WHITE),
                    net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(300), java.time.Duration.ofSeconds(4), java.time.Duration.ofMillis(600))));
            p.sendMessage("§2Zielarz: §f" + rr[rr.length - 1]);
        }
        zielnik.zobaczono(p, widocznyGatunek(s));
        s.zad = Bukkit.getScheduler().runTaskTimer(plugin, () -> tik(s), 1L, 1L);
    }

    static String grupa(String gatunek) { return gatunek.equals("czosnek") || gatunek.equals("konwalia") ? "liscie" : gatunek; }

    /** Mokry zbior: deszcz nad odkryta kepa albo rosa rano w czasie gry (23000-1500). */
    static boolean mokro(Block b) {
        long t = b.getWorld().getTime();
        return b.getWorld().hasStorm() && b.getLightFromSky() >= 14 || t >= 23000 || t < 1500;
    }

    /** Woda w promieniu 4 blokow (poziom kepy +-1): ziemia wilgotna. */
    static boolean przyWodzie(Block b) {
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) for (int dy = -1; dy <= 1; dy++)
            if (b.getRelative(dx, dy, dz).getType() == Material.WATER) return true;
        return false;
    }

    /** Pierwsze zbiory: jedna rada, dopoki gracz sam nie zrobi tego dobrze (zielnik pamieta, co odkryl). */
    private String wskazowka(Sesja s) {
        return switch (widocznyGatunek(s)) {
            case "dziurawiec" -> zielnik.ma(s.p, "dziurawiec_wyborny") ? null : "Tnij LPM tuż pod kwiatami|LPM tnie tuż pod kwiatami. Chorych pędów nie bierz.";
            case "kozlek" -> zielnik.ma(s.p, "kozlek_wyborny") ? null : "Opukaj ziemię łopatką (LPM)|Opukaj ziemię łopatką (LPM), odkop głuche miejsca, korzeń ciągnij PPM.";
            case "szerokolistna" -> zielnik.ma(s.p, "konwalia_zapach") || zielnik.ma(s.p, "czosnek_zapach") ? null : "Uwaga: trujący sobowtór!|Trujący sobowtór! Niepewny? Trzymaj PPM na liściu, żeby go powąchać.";
            default -> null;
        };
    }

    /** Sobowtory wygladaja tak samo z daleka — nazwa dopiero po rozpoznaniu. */
    private String widocznyGatunek(Sesja s) {
        if ((s.gatunek.equals("czosnek") || s.gatunek.equals("konwalia")) && !s.rozpoznana) return "szerokolistna";
        return s.gatunek;
    }

    private String nazwaKepy(Sesja s) {
        return switch (widocznyGatunek(s)) {
            case "dziurawiec" -> "Kępa dziurawca";
            case "kozlek" -> s.przyWodzie ? "Kozłek przy wodzie" : "Kozłek w suchej ziemi";
            case "czosnek" -> "Czosnek niedźwiedzi";
            case "konwalia" -> "Konwalia";
            default -> "Roślina o szerokich liściach";
        };
    }

    // ================================================================ narzedzie: tylko wyglad (pakiet), ekwipunek nietkniety
    // Podmiana prawdziwego przedmiotu w rece zderza sie z Ksiega postaci (slot 0 = prawa reka, przywracana przy teleporcie
    // — uchwyt znikal; prawdziwy sierp moglby przepasc). Narzedzie widac tylko u klienta, jak w Zbieraniu v2.

    /** Stare buildy wycinka podmienialy przedmiot w rece — sprzatanie resztek (tylko klony testowe). */
    @EventHandler
    public void naWejscie(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        var pdc = p.getPersistentDataContainer();
        pdc.remove(kZamiana);
        pdc.remove(kSlot);
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(kUchwyt, PersistentDataType.BYTE)) inv.setItem(i, null);
        }
    }

    private ItemStack wyglad(Sesja s, Material m, String nazwa) {
        ItemStack it = m == null ? s.sierp.clone() : new ItemStack(m);
        it.setData(DataComponentTypes.CUSTOM_NAME, Component.empty());   // bez napisu nad paskiem przy kazdej zmianie
        return it;
    }

    private void pokazWDloni(Sesja s, String co) {
        ItemStack it = switch (co) {
            case "lopatka" -> wyglad(s, Material.IRON_SHOVEL, "Łopatka");
            case "rece-ciagnij", "rece-rozcieraj", "rece" -> { ItemStack r = ZRoslina.przedmiot("zielarz_rekawice"); r.setData(DataComponentTypes.CUSTOM_NAME, Component.empty()); yield r; }
            default -> wyglad(s, null, null);
        };
        s.widok = co;
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(s.p,
                    new WrapperPlayServerSetPlayerInventory(s.p.getInventory().getHeldItemSlot(), SpigotConversionUtil.fromBukkitItemStack(it)));
        } catch (Throwable t) {
            plugin.getLogger().warning("Zielarz: wyglad narzedzia: " + t);
        }
    }

    private String narzedzieDla(Sesja s, ZRoslina.Czesc c) {
        if (c == null) return switch (s.gatunek) { case "kozlek" -> "lopatka"; case "czosnek", "konwalia" -> "rece"; default -> "sierp"; };
        return switch (c.typ) {
            case "gleba" -> "lopatka";
            case "rozeta" -> "rece-ciagnij";
            case "lisc" -> "rece-rozcieraj";
            case "kwiat" -> "rece";
            default -> "sierp";
        };
    }

    // ================================================================ tik
    private void tik(Sesja s) {
        Player p = s.p;
        if (s.konczy) return;
        if (!p.isOnline() || p.isDead() || p.getWorld() != s.blok.getWorld() || p.getLocation().distanceSquared(s.stoi) > 1.0) { zakoncz(s, p.isOnline() && !p.isDead() ? "§7Coś cię odepchnęło od rośliny." : null); return; }
        s.t++;
        if (s.t <= 3 || s.t % 20 == 0 || s.ukryjZnow) { p.sendBlockChange(s.blok.getLocation(), Material.AIR.createBlockData()); s.ukryjZnow = false; }
        if (s.czekaNaPuszczenie && s.t > 10 && s.t - s.ostatniPpm > 6) s.czekaNaPuszczenie = false;
        if (s.trzyma && s.t - s.ostatniPpm > 6) ppmPuszczony(s);
        if (!s.trzyma) {
            s.hover = s.t < s.zajetyDo ? null : s.r.celowana(4.5);
            s.r.podswietl(s.hover, PODSWIETL);
            if (s.hover != null) s.ostatniHover = s.t;
            // plamki choroby widac dopiero z bliska: dluzsze przyjrzenie sie pedowi
            if (s.hover != s.hoverC) { s.hoverC = s.hover; s.hoverOd = s.t; }
            else if (s.hover != null && s.hover.typ.equals("ped") && s.hover.b("chory") && !s.hover.b("chorWidac") && s.t - s.hoverOd >= 12) {
                s.hover.cechy.put("chorWidac", true);
                podmienKarte(s.hover, "zielarz_k_dziurawiec_srodek", "zielarz_k_dziurawiec_srodek_chory");
                podmienKarte(s.hover, "zielarz_k_dziurawiec_gora", "zielarz_k_dziurawiec_gora_chory");
            }
            // narzedzie zmienia sie dopiero po chwili celowania w nic (bez migania przy przejezdzaniu wzrokiem)
            String co = s.hover == null && s.t - s.ostatniHover < 8 && !s.widok.isEmpty() ? s.widok : narzedzieDla(s, s.hover);
            if (!co.equals(s.widok) || s.t % 20 == 0) pokazWDloni(s, co);
            pokazLinie(s);
        } else {
            s.r.linia(null, 0, null);
            trzymanie(s);
        }
        if (s.t % 10 == 0) s.r.wiatr(s.t, s.hover, s.chwyt);
        boolean ciagnie = s.trzyma && s.chwyt != null && s.chwyt.typ.equals("rozeta") && s.t - s.ostatniPpm <= 6;
        if (s.gatunek.equals("kozlek") && !ciagnie) {
            if (s.napiecie > 0) {
                s.napiecie = Math.max(0, s.napiecie - 0.022);   // popuszczenie: napiecie schodzi, postep zostaje
                ZRoslina.Czesc roz = rozeta(s);
                if (roz != null && !roz.zabrana && s.t % 2 == 0) s.r.przesun(roz, new Vector3f(0, (float) (s.postep * 0.5), 0), 3);
            }
            if (s.trzeszczy && s.napiecie <= gornaPasma(s)) { s.trzeszczy = false; if (s.wprawa < 3) plugin.pasek(s.p, "§fPopuściłeś. Możesz ciągnąć dalej."); }
        }
        if (s.t - s.ostatniaAkcja > 900) zakoncz(s, "§7Wstajesz.");
        else if (wszystkoZebrane(s) && s.t >= s.zajetyDo) zakoncz(s, null);
    }

    /** Podmiana karty czesci (blok albo przedmiot Nexo, zaleznie od trybu kart). */
    private static void podmienKarte(ZRoslina.Czesc c, String z, String na) {
        var bz = ZRoslina.blok(z);
        for (ZRoslina.Klocek k : c.klocki) {
            if (k.d instanceof BlockDisplay bd && bd.getBlock().equals(bz)) bd.setBlock(ZRoslina.blok(na));
            else if (k.d instanceof org.bukkit.entity.ItemDisplay id && z.equals(com.nexomc.nexo.api.NexoItems.idFromItem(id.getItemStack())))
                id.setItemStack(ZRoslina.przedmiot(na));
        }
    }

    private boolean wszystkoZebrane(Sesja s) {
        for (ZRoslina.Czesc c : s.r.czesci) {
            if (c.zabrana) continue;
            if (c.typ.equals("ped") || c.typ.equals("lisc") || c.typ.equals("rozeta")) return false;
        }
        return true;
    }

    // ================================================================ gesty
    private void lpm(Sesja s) {
        if (s.trzyma || s.t - s.ostatniLpm < 5 || s.t < s.zajetyDo) return;
        s.ostatniLpm = s.t;
        ZRoslina.Czesc c = s.hover;
        if (c == null) { dzwiek(s, Sound.BLOCK_GRASS_HIT, 0.3f, 1.4f); return; }   // machniecie w powietrze slychac, nie ginie bez sladu
        s.ostatniaAkcja = s.t;
        switch (c.typ) {
            case "ped" -> tnijPed(s, c);
            case "gleba" -> { if (!s.sonda[c.nr] && !s.poluzowane[c.nr]) sonduj(s, c); else kop(s, c); }
            case "lisc" -> zerwijLisc(s, c);   // w rekawicach jeden lisc (scinanie calej kepy jednym klikiem bylo pulapka)
            case "kwiat" -> podpowiedz(s, "kwiat", "§fKwiaty zostaw. Zbiera się liście.");
            case "rozeta" -> podpowiedz(s, "rozeta", "§fKozłka się nie ścina. §7Chwyć łodygę przy ziemi (PPM) i ciągnij.");
            default -> { }
        }
    }

    /** Rada-instrukcja po ruchu: tylko 3 pierwsze razy na postac i rodzaj (dzwiek i obraz zostaja zawsze). */
    private void podpowiedz(Sesja s, String rodzaj, String txt) { if (zielnik.rada3(s.p, rodzaj)) plugin.pasek(s.p, txt); }

    /** Pomiar PPM: odstepy miedzy powtorzeniami pakietu (rozklad do raportu; tylko gdy pomiar wlaczony). */
    static final int[] ODSTEPY_PPM = new int[16];
    /** Pomiar testowy (rzemioslo pomiar on): linia w logu po kazdym zbiorze + odstepy PPM. */
    static boolean pomiar;

    static String histogramPpm() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < ODSTEPY_PPM.length; i++) if (ODSTEPY_PPM[i] > 0) b.append(i).append(i == 15 ? "+" : "").append(':').append(ODSTEPY_PPM[i]).append(' ');
        return b.toString().trim();
    }

    /** Test: wszystkie kepy gracza gotowe do zbioru od razu (zamiast czekac minut). */
    int odrostTeraz(Player p) {
        Map<String, Stan> m = stany.get(p.getUniqueId());
        if (m == null) return 0;
        int n = 0;
        long teraz = System.currentTimeMillis();
        for (Stan st : m.values()) if (st.odrostDo > teraz) { st.odrostDo = teraz - 1; n++; }
        return n;
    }

    /** Klient trzymajacy PPM powtarza uzycie co 4 ticki — kazde powtorzenie przedluza chwyt; cisza = puszczenie. */
    private void ppmWcisniety(Sesja s) {
        if (pomiar && s.trzyma) ODSTEPY_PPM[Math.min(15, Math.max(0, s.t - s.ostatniPpm))]++;
        s.ostatniPpm = s.t;
        if (s.czekaNaPuszczenie) return;   // przycisk trzymany jeszcze z klikniecia w kepe
        if (s.trzyma || s.t < s.zajetyDo) return;
        ZRoslina.Czesc c = s.hover;
        if (c == null) return;
        s.trzyma = true;
        s.chwyt = c;
        s.chwytZalatwiony = false;
        s.trzymaOd = s.t;
        s.ostatniaAkcja = s.t;
    }

    private void ppmPuszczony(Sesja s) {
        if (!s.trzyma) return;
        s.trzyma = false;
        int dl = s.ostatniPpm - s.trzymaOd;
        ZRoslina.Czesc c = s.chwyt;
        s.chwyt = null;
        if (c == null || c.zabrana || s.chwytZalatwiony) return;
        if (dl <= 4 || dl <= 11 && (c.typ.equals("kwiat") || c.typ.equals("ped"))) klik(s, c);   // klik dluzszy niz jeden pakiet tez jest klikiem (wczesniej przepadal bez sladu)
        else if (c.typ.equals("lisc")) podpowiedz(s, "lisc-krotko", "§fZa krótko, żeby cokolwiek poczuć.");
        else if (c.typ.equals("ped") && dl >= 12) { s.r.przechyl(c, new org.joml.Quaternionf(), new Vector3f(), 0f, 6); plugin.pasek(s.p, "§fPuszczasz. Pęd prostuje się."); }
    }

    private void klik(Sesja s, ZRoslina.Czesc c) {
        switch (c.typ) {
            case "lisc" -> zerwijLisc(s, c);
            case "ped" -> podpowiedz(s, "ped-klik", "§fZiele bierze się sierpem (LPM). §7Trzymanie PPM wyrywa pęd z korzeniem.");
            case "kwiat" -> plugin.pasek(s.p, s.gatunek.equals("konwalia") ? "§7Drobne białe dzwonki. Lepiej ich nie zrywać."
                    : "§7Gwiazdki kwiatów na wysokiej łodyżce.");
            case "rozeta" -> {
                dzwiek(s, Sound.BLOCK_HANGING_ROOTS_STEP, 0.7f, 0.7f);
                podpowiedz(s, "szarpniecie", "§fSzarpnięcie. Siedzi w ziemi.");
            }
            default -> { }
        }
    }

    private void trzymanie(Sesja s) {
        ZRoslina.Czesc c = s.chwyt;
        if (c == null || c.zabrana || s.chwytZalatwiony) return;
        int dl = s.t - s.trzymaOd;
        // pierwsze 4 ticki: nie wiadomo jeszcze, czy to klik, czy chwyt (ponowny chwyt korzenia w trakcie — od razu)
        if (dl < 5 && !(c.typ.equals("rozeta") && s.postep > 0)) return;
        switch (c.typ) {
            case "rozeta" -> ciagnij(s, c);
            case "ped" -> {
                if (s.t - s.ostatniPpm > 6) return;   // klient juz nie trzyma (puszczenie wykrywamy z opoznieniem)
                if (dl >= WYRWANIE) { wyrwijPed(s, c); return; }
                if (dl >= 12 && dl % 3 == 0) {
                    float u = (dl - 12f) / (WYRWANIE - 12f);   // ped nagina sie i unosi coraz bardziej
                    s.r.przechyl(c, ZRoslina.obrotZ(6 + 16 * u), new Vector3f(0, 0.02f + 0.08f * u, 0), 0f, 3);
                    dzwiek(s, Sound.BLOCK_HANGING_ROOTS_STEP, 0.4f + 0.4f * u, 0.7f + 0.5f * u);
                    if (dl == 12) podpowiedz(s, "ped-napina", "§6Pęd napina się w ziemi. Puść, jeśli nie chcesz go wyrwać.");
                }
            }
            case "lisc" -> {
                if (s.t - s.ostatniPpm > 6) return;
                if (s.rozpoznana) { s.chwytZalatwiony = true; zerwijLisc(s, c); }   // juz wiesz, co to — nie niszczysz liscia
                else rozcieraj(s, c, dl);
            }
            default -> { }
        }
    }

    // ================================================================ DZIURAWIEC: ciecie sierpem
    /** Linia ciecia na celowanym pedzie: biala dla wszystkich (v7: mistrzostwo to ciecie dwoch pedow, nie kolor strefy). */
    private void pokazLinie(Sesja s) {
        ZRoslina.Czesc c = s.hover;
        Vector hit = c != null && c.typ.equals("ped") ? s.r.trafienie(c) : null;
        if (hit == null) { s.r.linia(null, 0, null); return; }
        double seg = c.d("h") / 4, yy = (hit.getY() - c.kotwica.getY()) / seg;
        int k = yy < SEG_Y[1] ? 0 : yy < SEG_Y[2] ? 1 : yy < SEG_Y[3] ? 2 : 3;
        s.r.linia(c, hit.getY(), Material.WHITE_CONCRETE);
    }

    /** Segmenty pedu dziurawca: polozenie i rozmiar kart (w jednostkach seg = h/4). */
    static final double[] SEG_Y = {0, 0.75, 1.45, 2.05}, SEG_R = {0.85, 0.85, 0.9, 2.0};

    private void budujDziurawiec(Sesja s) {
        Random rnd = s.rnd;
        ZRoslina r = s.r;
        int n = 7 + rnd.nextInt(2);   // gesto: krzak, nie rzad tyczek
        boolean slaba = s.stan.slaba;
        if (slaba) n -= 2;   // po chciwym zbiorze: mniej pedow, czesc cienkich
        int faza = fazaKepy(s.blok);
        for (int i = 0; i < n; i++) {
            // kepa w kole (nie rzad tyczek): spirala zlotego kata, wyzsze pedy w srodku
            double kat = Math.toRadians(i * 137.5 + rnd.nextDouble() * 20), rr = 0.06 + 0.28 * Math.sqrt((i + 0.5) / n);
            double lx = Math.cos(kat) * rr, lz = Math.sin(kat) * rr * 0.85;
            double h = 0.62 + 0.22 * (1 - rr / 0.34) + rnd.nextDouble() * 0.06;
            double v = rnd.nextDouble();
            boolean cienki = slaba && rnd.nextDouble() < 0.4;
            if (cienki) h *= 0.8;   // widocznie nizszy
            // faza kepy w czasie gry (dzien cyklu 0 paki, 1-2 pelnia, 3 przekwit); dwa srodkowe pedy w pelni kwitna zawsze
            double pPak = faza == 0 ? 0.6 : faza == 3 ? 0.0 : 0.08, pStary = faza == 3 ? 0.6 : faza == 0 ? 0.0 : 0.06;
            String fz = i < 2 && faza != 0 && faza != 3 ? "kw" : v < pPak ? "pak" : v < 1 - pStary ? "kw" : "stary";
            boolean chory = i >= 2 && rnd.nextDouble() < 0.18, dbl = fz.equals("kw") && rnd.nextDouble() < 0.12;
            s.ciecWszystkich += dbl ? 2 : 1;
            ZRoslina.Czesc c = r.czesc("ped", i, lx, 0, lz);
            c.cechy.put("h", h); c.cechy.put("faza", fz); c.cechy.put("chory", chory); c.cechy.put("dbl", dbl); c.cechy.put("cienki", cienki);
            double seg = h / 4;
            c.box = new BoundingBox(lx - 0.12, 0, lz - 0.12, lx + 0.12, (SEG_Y[3] + SEG_R[3] * 0.8) * seg, lz + 0.12);
            // chory wyglada z daleka jak zdrowy: plamki pojawiaja sie przy dluzszym spojrzeniu (tik)
            String[] karty = {"dziurawiec_dol", "dziurawiec_srodek", "dziurawiec_gora",
                    fz.equals("pak") ? "dziurawiec_pak" : fz.equals("stary") ? "dziurawiec_przekwit" : "dziurawiec_kwiat"};
            double obrot = rnd.nextDouble() * 90;
            // niska, gesta kepa jak blok w swiecie: krotki dol, nakladajace sie segmenty, duzy kwiatostan
            for (int k = 0; k < 4; k++) {
                ZRoslina.Klocek kl = r.karta(c, "zielarz_k_" + karty[k], 0, SEG_Y[k] * seg, 0, SEG_R[k] * seg, ZRoslina.obrotY(obrot));   // segmenty jednego pedu w jednej plaszczyznie
                kl.seg = k;
            }
            if (dbl) r.karta(c, "zielarz_k_dziurawiec_kwiat", 0.06, seg * 1.6, 0.04, seg * 1.3, ZRoslina.obrotY(obrot + 45)).seg = 3;
        }
    }

    /** Faza kwitnienia kepy: 4-dniowy cykl czasu gry, przesuniety o miejsce kepy (sasiednie kepy nie kwitna razem). */
    static int fazaKepy(Block b) {
        long dzien = b.getWorld().getFullTime() / 24000;
        int off = Math.floorMod(b.getX() * 31 + b.getZ() * 17 + b.getY(), 4);
        return (int) Math.floorMod(dzien + off, 4L);
    }

    /** Ciecie na granicy segmentu (4 karty pedu): kwiaty / gorny ped / srodek / zdrewnialy dol = strefy jakosci.
     *  Mistrzostwo (poz. 50): jeden ruch sierpa scina tez najblizszy sasiedni ped na tej samej wysokosci. */
    private void tnijPed(Sesja s, ZRoslina.Czesc c) {
        Vector hit = s.r.trafienie(c);
        if (hit == null) return;
        String m = tnijPedNa(s, c, hit.getY());
        if (s.oko) {
            ZRoslina.Czesc d = null;
            double best = 0.25 * 0.25;
            for (ZRoslina.Czesc x : s.r.czesci) {
                if (x == c || x.zabrana || !x.typ.equals("ped")) continue;
                double dx = x.kotwica.getX() - c.kotwica.getX(), dz = x.kotwica.getZ() - c.kotwica.getZ(), d2 = dx * dx + dz * dz;
                if (d2 < best && hit.getY() - x.kotwica.getY() < x.d("h") * 0.9) { best = d2; d = x; }
            }
            if (d != null) m += " §7Drugi pęd: " + tnijPedNa(s, d, hit.getY());
        }
        plugin.pasek(s.p, m);
    }

    /** Ped sciety na wysokosci swiata yHit (wzgledem kotwicy pedu); zwraca opis z kolorem jakosci. */
    private String tnijPedNa(Sesja s, ZRoslina.Czesc c, double yHit) {
        double h = c.d("h"), seg = h / 4;
        double yy = (yHit - c.kotwica.getY()) / seg;
        int k = yy < SEG_Y[1] ? 0 : yy < SEG_Y[2] ? 1 : yy < SEG_Y[3] ? 2 : 3;
        float cy = (float) Math.max(0.04, SEG_Y[k] * seg);
        String faza = c.s("faza");
        double q;
        String m;
        if (faza.equals("pak")) { q = 0.3; m = "Pąki. Ziele jeszcze bez mocy."; zielnik.odkryj(s.p, "dziurawiec_faza"); }
        else if (faza.equals("stary")) { q = 0.25; m = "Przekwitłe. Ziele bez mocy."; zielnik.odkryj(s.p, "dziurawiec_faza"); }
        else if (c.b("bezKwiatow")) { q = 0; m = "Bez kwiatów to sama łodyga. Moc zerwałeś wcześniej."; }
        else if (k == 3) { q = 0.4; m = "Same kwiaty, mało ziela."; }
        else if (k == 2) { q = 1.0; m = "Czyste cięcie pod kwiatami."; }
        else if (k == 1) { q = 0.65; m = "Za nisko: w pęczku starsze liście."; }
        else { q = 0.35; m = "Za nisko: zdrewniała łodyga w pęczku."; }
        if (c.b("cienki") && q > 0.7) { q = 0.7; m = "Cienki pęd po zbyt łapczywym zbiorze. Mało w nim mocy."; }
        boolean chory = c.b("chory");
        if (chory) { q = 0; s.chore++; m = "Plamy na liściach. Zostawiasz go w trawie."; zielnik.odkryj(s.p, "dziurawiec_chory"); }
        if (q >= 0.85 && !reka(s)) { q = 0.7; m = "Ręka jeszcze niepewna: cięcie poszarpane."; }
        boolean okaz = q >= 0.85 && c.b("dbl");
        if (okaz) { s.okaz = true; m = "Czyste cięcie. Podwójny kwiatostan!"; }
        s.ciecTotal += c.b("dbl") ? 2 : 1;   // podwojny kwiatostan liczy sie do chciwosci podwojnie
        float strona = s.rnd.nextBoolean() ? 1 : -1;
        s.r.przechyl(c, ZRoslina.obrotZ(80 * strona), new Vector3f(0, -cy * 0.9f, -0.06f), cy, 9);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {   // dotyka ziemi, odbija sie lekko i lezy chwile
            if (!s.konczy) s.r.przechyl(c, ZRoslina.obrotZ(88 * strona), new Vector3f(0, -cy * 0.9f + 0.03f, -0.1f), cy, 3);
        }, 10L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (s.konczy) return;
            // znika wszystko, co sie przewrocilo (takze segment przeciety linia ciecia — wczesniej zostawal przechylony)
            c.klocki.removeIf(kl -> { if (kl.y1 > cy + 1e-3) { kl.d.remove(); return true; } return false; });
            dzwiek(s, Sound.BLOCK_AZALEA_LEAVES_FALL, 0.6f, 1.2f);
        }, 20L);
        c.zabrana = true;
        c.box = null;
        s.zajetyDo = s.t + 6;
        dzwiek(s, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.55f, 1.45f);
        dzwiek(s, Sound.BLOCK_GRASS_BREAK, 0.8f, 1.2f);
        Location pk = s.r.swiat(c.kotwica.getX(), cy, c.kotwica.getZ());
        s.p.spawnParticle(Particle.DUST, pk, 6, 0.05, 0.03, 0.05, new Particle.DustOptions(Color.fromRGB(110, 160, 60), 0.35f));
        s.p.spawnParticle(Particle.DUST, pk.clone().add(0, 0.1, 0), 3, 0.06, 0.04, 0.06, new Particle.DustOptions(Color.fromRGB(240, 210, 70), 0.3f));
        zuzyj(s);
        if (q > 0) daj(s, "PROF_MEDICINAL_HERB", okaz ? 2 : 1, q);
        else if (chory) uwaga(s, "chore");
        return (q >= 0.85 ? "§a" : q >= 0.5 ? "§e" : "§c") + m;
    }

    /** Wprawa ("reka sie nauczyla"): szansa, ze wyborny ruch wyjdzie wyborny, rosnie z liczba zbiorow tego gatunku. */
    private static double wprawaP(int n) { return 0.86 - 0.46 * Math.exp(-n / 5.0); }

    private boolean reka(Sesja s) { return ThreadLocalRandom.current().nextDouble() < wprawaP(s.wprawa); }

    private void zerwijKwiatyDziurawca(Sesja s, ZRoslina.Czesc c) {
        if (c.b("bezKwiatow")) { plugin.pasek(s.p, "§fKwiatów już tu nie ma."); return; }
        c.klocki.removeIf(kl -> { if (kl.seg == 3) { kl.d.remove(); return true; } return false; });
        c.cechy.put("bezKwiatow", true);
        dzwiek(s, Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.6f, 1.6f);
        if (c.s("faza").equals("kw") && !c.b("chory")) { daj(s, "PROF_MEDICINAL_HERB", 1, 0.4); plugin.pasek(s.p, "§eZrywasz same kwiaty. Mało ziela."); }
        else plugin.pasek(s.p, "§fZrywasz palcami. Nic, co by się nadało.");
    }

    private void wyrwijPed(Sesja s, ZRoslina.Czesc c) {
        s.chwytZalatwiony = true;
        s.r.przechyl(c, ZRoslina.obrotZ(12), new Vector3f(0, 0.3f, -0.12f), 0f, 6);   // unosi sie przed graczem, nie odlatuje w bok
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.usunCzesc(c); }, 12L);
        c.zabrana = true;
        c.box = null;
        s.zniszczona = true;
        s.zajetyDo = s.t + 8;
        Location base = s.r.swiat(c.kotwica.getX(), 0.05, c.kotwica.getZ());
        s.p.spawnParticle(Particle.DUST, base, 18, 0.12, 0.05, 0.12, GRUDKA);
        dzwiek(s, Sound.BLOCK_ROOTED_DIRT_BREAK, 1f, 0.9f);
        dzwiek(s, Sound.BLOCK_GRASS_BREAK, 0.8f, 0.7f);
        wstrzas(s);
        uwaga(s, "wyrwany");
        plugin.pasek(s.p, "§cWyrwany z korzeniem. Ziele ubrudzone ziemią, do niczego, a kępa nie odrośnie szybko.");
    }

    // ================================================================ KOZLEK: korzen z oporem
    private void budujKozlek(Sesja s) {
        Random rnd = s.rnd;
        ZRoslina r = s.r;
        int boczne = 0;
        for (int i = 0; i < 4; i++) { s.boczne[i] = rnd.nextDouble() < 0.45; if (s.boczne[i]) boczne++; }
        if (boczne == 0) s.boczne[rnd.nextInt(4)] = true;
        // ziemia: 4 placki lezace plasko wokol nasady (0 przod, 1 prawo, 2 tyl, 3 lewo)
        double[][] pola = {{-0.45, -0.5, 0.45, -0.14}, {0.14, -0.14, 0.5, 0.14}, {-0.45, 0.14, 0.45, 0.5}, {-0.5, -0.14, -0.14, 0.14}};
        double[] sx = {0, 0.3, 0, -0.3}, sz = {-0.3, 0, 0.3, 0};
        for (int i = 0; i < 4; i++) {
            double[] b = pola[i];
            ZRoslina.Czesc g = r.czesc("gleba", i, 0, 0, 0);
            g.box = new BoundingBox(b[0], 0, b[1], b[2], 0.07, b[3]);
            r.kartaSrodek(g, "zielarz_k_kozlek_gleba", sx[i], 0.012, sz[i], 0.5, ZRoslina.obrotY(i * 90 + rnd.nextDouble() * 20).mul(ZRoslina.obrotX(-90)));
            // koniuszek korzonka wystaje z ziemi tylko u ~60% (reszte trzeba wyczuc lopatka); po odkopaniu caly korzonek
            if (s.boczne[i] && rnd.nextDouble() < 0.6)
                r.kartaSrodek(g, "zielarz_k_kozlek_korzonek", sx[i] * 0.55, 0.02, sz[i] * 0.55, 0.12, ZRoslina.obrotY(i * 90 + rnd.nextDouble() * 40 - 20).mul(ZRoslina.obrotX(-90)));
        }
        ZRoslina.Czesc roz = r.czesc("rozeta", 0, 0, 0, 0);
        roz.box = new BoundingBox(-0.2, 0, -0.2, 0.2, 0.5, 0.2);   // rozeta + dolna czesc lodyg: chwyta sie tam, gdzie gracz celuje
        for (int k = 0; k < 7; k++) {   // pierzaste liscie rozety: karta lezaca, nasada w srodku
            double a = k * 360.0 / 7 + rnd.nextDouble() * 20, len = 0.5 + rnd.nextDouble() * 0.1, ar = Math.toRadians(a);
            Quaternionf q = ZRoslina.obrotY(a).mul(ZRoslina.obrotX(-48));
            r.kartaSrodek(roz, "zielarz_k_kozlek_lisc", -Math.cos(ar) * len * 0.4, len * 0.3, -Math.sin(ar) * len * 0.4, len, q);
        }
        int lodygi = 1 + rnd.nextInt(2);
        for (int i = 0; i < lodygi; i++) {
            double lx = (rnd.nextDouble() - 0.5) * 0.12, lz = (rnd.nextDouble() - 0.5) * 0.12, seg = 0.33 + rnd.nextDouble() * 0.06;   // nizej: baldach miesci sie w kadrze
            double ob = rnd.nextDouble() * 90;
            r.karta(roz, "zielarz_k_kozlek_lodyga", lx, 0, lz, seg, ZRoslina.obrotY(ob));
            r.karta(roz, "zielarz_k_kozlek_lodyga", lx, seg, lz, seg, ZRoslina.obrotY(ob + 40));
            r.karta(roz, "zielarz_k_kozlek_baldach", lx, seg * 2 - 0.12, lz, seg * 1.15, ZRoslina.obrotY(ob + 20));
        }
        // korzen pod ziemia (dwie skrzyzowane karty), wychodzi przy ciagnieciu
        r.karta(roz, "zielarz_k_kozlek_korzen", 0, -0.62, 0, 0.62, ZRoslina.obrotY(20));
        r.karta(roz, "zielarz_k_kozlek_korzen", 0, -0.62, 0, 0.62, ZRoslina.obrotY(110));
    }

    /** Gorna granica pasma: przy wodzie szersze, w suchej ziemi wezsze; kazdy nieodkopany korzonek i kazde puste
     *  kopniecie (0,08) zweza je (nie skraca czasu reakcji). */
    private double gornaPasma(Sesja s) {
        double baza = PASMO_DO + (s.przyWodzie ? 0.08 : -0.06);
        return Math.max(0.55, baza - 0.05 * nieruszoneBoczne(s) - 0.08 * s.zleKopanie);
    }

    /** Sonda (LPM w nieruszone pole): lopatka opukuje ziemie, nic nie kosztuje. Dzwiek mowi, czy pod spodem jest korzonek. */
    private void sonduj(Sesja s, ZRoslina.Czesc g) {
        int i = g.nr;
        if (s.postep > 0.98) return;
        s.sonda[i] = true;
        s.zajetyDo = s.t + 7;
        Location l = s.r.swiat((g.box.getMinX() + g.box.getMaxX()) / 2, 0.05, (g.box.getMinZ() + g.box.getMaxZ()) / 2);
        s.p.spawnParticle(Particle.DUST, l, 3, 0.06, 0.02, 0.06, GRUDKA);
        if (s.boczne[i]) { dzwiek(s, Sound.BLOCK_ROOTED_DIRT_HIT, 1f, 0.55f); dzwiek(s, Sound.BLOCK_HANGING_ROOTS_HIT, 0.8f, 0.6f); }
        else { dzwiek(s, Sound.BLOCK_SAND_HIT, 1f, 1.5f); dzwiek(s, Sound.BLOCK_GRAVEL_HIT, 0.5f, 1.6f); }
        podpowiedz(s, "sonda", s.boczne[i] ? "§7Głuchy, zbity odgłos. §8LPM jeszcze raz: kop." : "§7Sypki szelest. §8LPM jeszcze raz: kop mimo to.");
        zielnik.odkryj(s.p, "kozlek_sonda");
    }

    private static ZRoslina.Czesc rozeta(Sesja s) {
        for (ZRoslina.Czesc c : s.r.czesci) if (c.typ.equals("rozeta")) return c;
        return null;
    }

    private static int liczBoczne(Sesja s) { int n = 0; for (boolean b : s.boczne) if (b) n++; return n; }

    private int nieruszoneBoczne(Sesja s) {
        int n = 0;
        for (int i = 0; i < 4; i++) if (s.boczne[i] && !s.poluzowane[i]) n++;
        return n;
    }

    private void kop(Sesja s, ZRoslina.Czesc g) {
        int i = g.nr;
        if (s.poluzowane[i]) { plugin.pasek(s.p, "§fTu ziemia już rozluźniona."); return; }
        if (s.postep > 0.98) return;
        s.poluzowane[i] = true;
        zuzyj(s);
        s.zajetyDo = s.t + 8;
        podmienKarte(g, "zielarz_k_kozlek_gleba", "zielarz_k_kozlek_gleba_rozkopana");
        Location l = s.r.swiat((g.box.getMinX() + g.box.getMaxX()) / 2, 0.05, (g.box.getMinZ() + g.box.getMaxZ()) / 2);
        s.p.spawnParticle(Particle.DUST, l, 14, 0.12, 0.04, 0.12, GRUDKA);
        dzwiek(s, Sound.ITEM_SHOVEL_FLATTEN, 0.9f, 0.8f);
        dzwiek(s, Sound.BLOCK_ROOTED_DIRT_BREAK, 0.7f, 0.9f);
        if (s.boczne[i])   // odsloniety korzonek lezy w scianie wykopu
            s.r.kartaSrodek(g, "zielarz_k_kozlek_korzonek", (g.box.getMinX() + g.box.getMaxX()) / 2 * 0.8, 0.025, (g.box.getMinZ() + g.box.getMaxZ()) / 2 * 0.8, 0.26,
                    ZRoslina.obrotY(i * 90).mul(ZRoslina.obrotX(-90)));
        if (!s.boczne[i]) s.zleKopanie++;   // niepotrzebnie ruszona ziemia osypuje sie na korzen: wezsze pasmo, gorszy korzen
        plugin.pasek(s.p, s.boczne[i] ? "§7Ziemia puszcza. Korzonek odsłonięty." : "§6Sama ziemia. Osypuje się na korzeń, ciągnąć będzie trudniej.");
    }

    private void ciagnij(Sesja s, ZRoslina.Czesc roz) {
        if (s.t - s.ostatniPpm > 4) return;   // brak powtorzenia pakietu (co ~4 t): napiecie nie rosnie, zanim serwer uzna puszczenie
        int otw = nieruszoneBoczne(s);
        // tempo napiecia stale (czas reakcji zawsze ~0,5 s); nieodkopane korzonki i puste kopanie zwezaja pasmo
        s.napiecie += 0.014;
        double gorna = gornaPasma(s);
        if (otw > 0 && (!s.zaczep1 && s.postep >= 0.32 || !s.zaczep2 && s.postep >= 0.66)) {   // nieodkopany korzonek trzyma: szarpniecie
            if (s.postep >= 0.66) s.zaczep2 = true; else s.zaczep1 = true;
            s.napiecie += 0.05;
            dzwiek(s, Sound.BLOCK_ROOTED_DIRT_HIT, 0.9f, 0.7f);
        }
        boolean wPasmie = s.napiecie >= PASMO_OD && s.napiecie <= gorna;
        s.postep += s.napiecie < PASMO_OD ? 0.002 : wPasmie ? 0.025 : 0.006;
        int dl = s.t - s.trzymaOd;
        // widoczne napiecie: im mocniej, tym rozeta drzy szerzej i przechyla sie; w pasmie ziemia pracuje
        boolean zaMocno = s.napiecie > gorna;
        float drz = (float) (s.napiecie < PASMO_OD ? 0.012 : wPasmie ? 0.022 : 0.055);   // widoczne golym okiem, rosnie z napieciem
        float jx = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * drz), jz = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * drz);
        s.r.przesun(roz, new Vector3f(jx, (float) (s.postep * 0.5 + s.napiecie * 0.06), jz), 1);
        if (dl % 2 == 0) for (ZRoslina.Czesc g : s.r.czesci) if (g.typ.equals("gleba"))   // ziemia wokol nasady unosi sie kopczykiem
            s.r.przesun(g, new Vector3f(0, (float) (s.napiecie * 0.035), 0), 2);
        Location base = s.r.swiat(0, 0.04, 0);
        if (dl % (wPasmie ? 3 : 5) == 0) s.p.spawnParticle(Particle.DUST, base, wPasmie ? 3 : 1, 0.16, 0.02, 0.16, GRUDKA);
        if (!zaMocno && dl % 4 == 0) dzwiek(s, wPasmie ? Sound.BLOCK_ROOTED_DIRT_STEP : Sound.BLOCK_HANGING_ROOTS_STEP, wPasmie ? 0.6f : 0.35f, 0.8f);
        if (zaMocno && dl % 2 == 0) dzwiek(s, Sound.BLOCK_HANGING_ROOTS_BREAK, (float) (0.4 + 0.5 * s.napiecie), (float) (0.9 + 1.0 * (s.napiecie - gorna)));
        if (s.napiecie > gorna + 0.04 && !s.trzeszczy) {
            s.trzeszczy = true;
            s.naderwany = false;
            wstrzas(s);
            s.p.spawnParticle(Particle.DUST, base, 8, 0.12, 0.05, 0.12, GRUDKA);
            if (s.wprawa < 3) plugin.pasek(s.p, "§6Korzeń trzeszczy… popuść.");   // rada tylko przy 3 pierwszych zbiorach; dzwiek i drzenie zostaja
        }
        // trzeszczenie zignorowane: korzen sie nadrywa (przy wodzie kruchy: wczesniej i drozej)
        if (s.trzeszczy && !s.naderwany && s.napiecie > gorna + (s.przyWodzie ? 0.10 : 0.15)) {
            s.naderwany = true;
            s.naderwania++;
            dzwiek(s, Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
            plugin.pasek(s.p, "§6Coś trzasnęło w ziemi. Korzeń naderwany.");
        }
        if (s.napiecie >= s.zerwanie) { zerwijKorzen(s, roz, otw); return; }
        if (s.postep >= 1.0) wyciagnijKorzen(s, roz, otw);
    }

    private void zerwijKorzen(Sesja s, ZRoslina.Czesc roz, int otw) {
        s.chwytZalatwiony = true;
        zuzyj(s);
        roz.zabrana = true;
        roz.box = null;
        s.zniszczona = true;
        s.zajetyDo = s.t + 14;
        double q = Math.min(0.45, Math.max(0, s.postep * (1 - 0.15 * otw)));   // urwany = zawsze slaby
        // urwany: rozeta odskakuje lekko w gore i opada obok, w dolku zostaje reszta korzenia
        s.r.przesun(roz, new Vector3f(0.18f, (float) (s.postep * 0.5 + 0.12), -0.1f), 3);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.przesun(roz, new Vector3f(0.3f, 0.02f, -0.15f), 6); }, 4L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.usunCzesc(roz); }, 22L);
        dzwiek(s, Sound.ENTITY_ITEM_BREAK, 0.8f, 0.6f);
        dzwiek(s, Sound.BLOCK_ROOTED_DIRT_BREAK, 1f, 0.6f);
        wstrzas(s);
        if (s.postep > 0.3) daj(s, "PROF_KORZEN_KOZLKA", 1, q);
        zielnik.odkryj(s.p, "kozlek_urwany");
        uwaga(s, "urwany");
        plugin.pasek(s.p, "§cKorzeń urwał się w ziemi." + (s.postep > 0.3 ? " W ręku tylko kawałek." : ""));
    }

    private void wyciagnijKorzen(Sesja s, ZRoslina.Czesc roz, int otw) {
        s.chwytZalatwiony = true;
        zuzyj(s);
        roz.zabrana = true;
        roz.box = null;
        s.wykopany = true;
        s.zajetyDo = s.t + 16;
        // wyborny tylko z kompletem korzonkow; wtedy korzen glowny + peczek korzonkow (2 sztuki)
        // puste kopanie obija korzen (-0,1 za kazde), naderwanie -0,2 (przy wodzie -0,3)
        double q = otw == 0 ? 1.0 : Math.max(0.3, 0.75 - 0.15 * (otw - 1));
        q -= 0.1 * s.zleKopanie + (s.przyWodzie ? 0.3 : 0.2) * s.naderwania;
        q = Math.max(0.15, q);
        boolean niepewna = q >= 0.85 && !reka(s);
        if (niepewna) q = 0.7;
        // wielki moment przed oczami: korzen wychodzi pionowo przed kamere i wisi chwile, potem do reki
        s.r.przesun(roz, new Vector3f(0, 0.62f, -0.3f), 8);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (s.konczy) return;
            s.r.przesun(roz, new Vector3f(0, 0.66f, -0.32f), 10);
            s.p.spawnParticle(Particle.DUST, s.r.swiat(0, 0.3, -0.3), 10, 0.08, 0.15, 0.08, GRUDKA);   // ziemia sypie sie z korzonkow
        }, 9L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.usunCzesc(roz); }, 34L);
        Location base = s.r.swiat(0, 0.05, 0);
        s.p.spawnParticle(Particle.DUST, base, 30, 0.22, 0.08, 0.22, GRUDKA);
        dzwiek(s, Sound.BLOCK_ROOTED_DIRT_BREAK, 1f, 1.1f);
        dzwiek(s, Sound.BLOCK_MOSS_BREAK, 0.8f, 0.8f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> dzwiek(s, Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.0f), 8L);
        daj(s, "PROF_KORZEN_KOZLKA", otw == 0 ? 2 : 1, q);
        String m = otw > 0 ? "§eKorzeń wyszedł, ale korzonki zostały w ziemi (" + otw + ")."
                : niepewna ? "§eKorzeń wyszedł cały. Ręka jeszcze niepewna: draśnięty łopatką."
                : q >= 0.85 ? "§aKorzeń wyszedł cały, z korzonkami."
                : "§eKorzeń wyszedł cały, ale " + (s.naderwania > 0 ? "naderwany" : "obity rozkopaną ziemią") + ".";
        plugin.pasek(s.p, m);
        if (s.przyWodzie && s.naderwania > 0 || !s.przyWodzie && s.wprawa >= 2) zielnik.odkryj(s.p, "kozlek_woda");
        if (otw > 0) uwaga(s, "korzonki");
    }

    // ================================================================ CZOSNEK / KONWALIA: zrywanie i rozcieranie
    private void budujLiscie(Sesja s, boolean czosnek) {
        Random rnd = s.rnd;
        ZRoslina r = s.r;
        boolean kwitnie = rnd.nextDouble() < 0.2;   // jak blok w swiecie: zwykle same liscie; rozpoznanie po lisciach
        String lisc = czosnek ? "czosnek_lisc" : "konwalia_lisc";
        // czosnek: pojedyncze liscie na ogonkach rozchodza sie dookola; konwalia: 3-4 pedy po dwa liscie z jednej pochwy
        int pedyK = 3 + rnd.nextInt(2);
        int n = czosnek ? 6 + rnd.nextInt(3) : pedyK * 2;
        double start = rnd.nextDouble() * 360;
        double[] pedX = new double[pedyK], pedZ = new double[pedyK], pedA = new double[pedyK];
        for (int k = 0; k < pedyK; k++) {
            double pa = Math.toRadians(start + k * 360.0 / pedyK + rnd.nextDouble() * 30), pr = 0.12 + rnd.nextDouble() * 0.12;
            pedX[k] = Math.sin(pa) * pr; pedZ[k] = Math.cos(pa) * pr; pedA[k] = Math.toDegrees(pa) + rnd.nextDouble() * 40 - 20;
        }
        for (int i = 0; i < n; i++) {
            double a = czosnek ? start + i * 360.0 / n + rnd.nextDouble() * 20 : pedA[i / 2] + (i % 2 == 0 ? -28 : 28);
            double tilt = czosnek ? 34 + rnd.nextDouble() * 18 : 18 + rnd.nextDouble() * 14;
            double len = czosnek ? 0.62 + rnd.nextDouble() * 0.12 : 0.62 + rnd.nextDouble() * 0.12;
            double ar = Math.toRadians(a), tr = Math.toRadians(tilt), rp = czosnek ? 0.05 : 0;
            double bx = czosnek ? Math.sin(ar) * rp : pedX[i / 2], bz = czosnek ? Math.cos(ar) * rp : pedZ[i / 2];
            boolean stary = rnd.nextDouble() < 0.3;
            ZRoslina.Czesc l = r.czesc("lisc", i, 0, 0, 0);
            l.cechy.put("stary", stary); l.cechy.put("kwitnie", kwitnie);
            // lisc przechyla sie w strone a, ale blaszka zostaje twarza do gracza (bokiem wygladal jak waski miecz)
            double ra = Math.toRadians(a);
            Quaternionf ql = ZRoslina.obrotY(Math.sin(ra) * 18).mul(ZRoslina.obrotZ(-tilt * Math.sin(ra))).mul(ZRoslina.obrotX(tilt * Math.cos(ra)));
            r.karta(l, "zielarz_k_" + lisc + (stary ? "_stary" : ""), bx, 0, bz, len, ql);
            double ex = bx + Math.sin(ar) * Math.sin(tr) * len, ez = bz + Math.cos(ar) * Math.sin(tr) * len, ey = Math.cos(tr) * len;
            double cx = (bx + ex) / 2, cz = (bz + ez) / 2;
            l.box = new BoundingBox(Math.min(bx, ex) - 0.07, len * 0.25, Math.min(bz, ez) - 0.07, Math.max(bx, ex) + 0.07, ey, Math.max(bz, ez) + 0.07);
        }
        if (kwitnie) {
            ZRoslina.Czesc k = r.czesc("kwiat", 0, 0, 0, 0);
            double h = czosnek ? 0.78 : 0.62;
            // kwiaty na kartach odchylonych ku niebu: encja cieniuje kierunkowo i pionowa biel wychodzila szara
            if (czosnek) {
                r.karta(k, "zielarz_k_czosnek_lodyzka", 0, 0, 0, h * 0.85, null);   // przodem do gracza (bokiem ginela)
                r.kartaSrodek(k, "zielarz_k_czosnek_baldach", 0, h * 0.82, 0.02, h * 0.42, ZRoslina.obrotX(-35));   // ku graczowi, nie od spodu
            } else {
                r.karta(k, "zielarz_k_konwalia_kwiat", 0.04, 0, 0.06, h, ZRoslina.obrotX(28));   // twarza do gracza, odchylona
            }
            k.box = new BoundingBox(-0.12, h * 0.6, -0.12, 0.12, h, 0.12);
        }
    }

    /** Nierozpoznany lisc zbierasz jako czosnek (tak go widzisz). Konwalia wzieta za czosnek to skaza: kara przychodzi pozniej. */
    private String idLiscia(Sesja s) {
        if (!s.rozpoznana || s.gatunek.equals("czosnek")) return "PROF_WILD_SPICE";
        return "PROF_LISC_KONWALII";
    }

    private boolean pomylka(Sesja s) { return !s.rozpoznana && s.gatunek.equals("konwalia"); }

    private double jakoscLiscia(Sesja s, ZRoslina.Czesc l) {
        double q = l.b("stary") ? 0.5 : 1.0;
        if (l.b("kwitnie") && (s.gatunek.equals("czosnek") || !s.rozpoznana)) q *= 0.8;
        if (s.roztarte && s.gatunek.equals("czosnek")) q = Math.min(q, 0.7);   // roztarty na probe: kepa pognieciona, premia przepada
        return q;
    }

    private void skaza(Sesja s, int n) {
        if (n <= 0 || !pomylka(s)) return;
        s.skazone += n;
        zielnik.dolicz(s.p, "_skaza", n, true);
    }

    private void zerwijLisc(Sesja s, ZRoslina.Czesc l) {
        zuzyj(s);
        s.r.przechyl(l, ZRoslina.obrotX(-35), new Vector3f(0, 0.18f, -0.25f), -1f, 5);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.usunCzesc(l); }, 7L);
        l.zabrana = true;
        l.box = null;
        s.zajetyDo = s.t + 3;
        dzwiek(s, Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.7f, 1.3f);
        double q = jakoscLiscia(s, l);
        boolean niepewna = q >= 0.85 && !reka(s);
        if (niepewna) q = 0.7;
        skaza(s, daj(s, idLiscia(s), 1, q));
        boolean jakCzosnek = s.gatunek.equals("czosnek") || !s.rozpoznana;   // konwalia nierozpoznana wyglada jak czosnek
        plugin.pasek(s.p, (q >= 0.85 ? "§a" : "§e") + (l.b("stary") ? "Stary liść, żółknie na końcu."
                : niepewna ? "Ręka jeszcze niepewna: liść zgnieciony przy zrywaniu."
                : s.roztarte && s.gatunek.equals("czosnek") ? "Kępa pognieciona przy rozcieraniu: dobre, nie wyborne."
                : l.b("kwitnie") && jakCzosnek ? "Młody liść, ale kępa już kwitnie: mniej ostry." : "Młody, jędrny liść."));
    }

    private void tnijLiscie(Sesja s) {
        int n = 0;
        double suma = 0;
        for (ZRoslina.Czesc c : s.r.czesci) {
            if (c.zabrana || !c.typ.equals("lisc")) continue;
            double q = jakoscLiscia(s, c) * 0.7;
            suma += q;
            n++;
            s.r.przechyl(c, ZRoslina.obrotX(-80), new Vector3f(0, -0.05f, 0), 0.02f, 8);
            ZRoslina.Czesc cc = c;
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.usunCzesc(cc); }, 14L);
            c.zabrana = true;
            c.box = null;
        }
        if (n == 0) return;
        s.zajetyDo = s.t + 10;
        dzwiek(s, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.55f, 1.3f);
        dzwiek(s, Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.8f, 1.0f);
        skaza(s, daj(s, idLiscia(s), n, suma / n));
        plugin.pasek(s.p, "§eŚcinasz wszystko jednym ruchem. Liście pogniecione.");
    }

    private void rozcieraj(Sesja s, ZRoslina.Czesc l, int dl) {
        if (dl % 4 == 0) {
            Location gdzie = s.r.swiat((l.box.getMinX() + l.box.getMaxX()) / 2, l.box.getMinY() + 0.04, (l.box.getMinZ() + l.box.getMaxZ()) / 2);
            s.p.spawnParticle(Particle.DUST, gdzie, 2, 0.03, 0.02, 0.03, new Particle.DustOptions(Color.fromRGB(90, 150, 60), 0.3f));   // drobny sok, nie kwadraty przed kamera
            dzwiek(s, Sound.ITEM_BRUSH_BRUSHING_GENERIC, 0.6f, 1.3f);
        }
        if (dl < 20) return;
        s.chwytZalatwiony = true;
        s.r.usunCzesc(l);
        s.roztarte = true;
        boolean pierwszy = !zielnik.ma(s.p, s.gatunek.equals("czosnek") ? "czosnek_zapach" : "konwalia_zapach");
        s.rozpoznana = true;
        zielnik.zapamietaj(s.p, kluczRozpoznania(s.blok, s.stan));
        boolean czosnek = s.gatunek.equals("czosnek");
        dzwiek(s, Sound.BLOCK_MOSS_CARPET_BREAK, 0.8f, 1.4f);
        plugin.pasek(s.p, czosnek ? "§aRozcierasz liść w palcach. Ostry zapach czosnku." : "§6Rozcierasz liść. Bez zapachu, tylko gorzki sok. To konwalia.");
        if (pierwszy) zielnik.odkryj(s.p, czosnek ? "czosnek_zapach" : "konwalia_zapach");
        if (czosnek) { zielnik.odkryj(s.p, "czosnek_rozcieranie"); podpowiedz(s, "roztarty", "§7Pewność kosztuje: roztarta kępa da liście dobre, nie wyborne."); }
    }

    // ================================================================ wspolne
    /** Jakosc w dwoch stosach: wyborna = osobny przedmiot {@code <ID>_WYBORNE}, dobra = zwykly (bez PDC, stackuje sie),
     *  slaba = zwykly, ale polowa sztuk (reszta sie kruszy). Zwraca liczbe wydanych sztuk. */
    private int daj(Sesja s, String id, int ile, double q) {
        q = Math.max(0, Math.min(1, q));
        int klasa = q >= 0.85 ? 2 : q >= 0.5 ? 1 : 0;
        int wyd = ile;
        if (klasa == 2 && s.stan.okazala() && ThreadLocalRandom.current().nextBoolean()) wyd++;   // rzadka odmiana: naddatek
        if (klasa == 0) wyd = (int) Math.floor(ile * 0.5 + ThreadLocalRandom.current().nextDouble());
        String wid = klasa == 2 ? id + "_WYBORNE" : id;
        ItemStack it = wyd > 0 ? plugin.przedmioty().zrob(wid, wyd) : null;
        if (it == null && wyd > 0 && klasa == 2) {
            plugin.getLogger().warning("Zielarz: brak przedmiotu " + wid + " — wydaje " + id);
            it = plugin.przedmioty().zrob(id, wyd);
        }
        if (it == null && wyd > 0) { plugin.getLogger().warning("Zielarz: brak przedmiotu " + id); return 0; }
        if (wyd == 0) s.pokruszone += ile;
        else {
            // pusty trzymany slot zajmujemy na chwile zaslepka: zbior nie wpada pod podrobione narzedzie w dloni
            var inv = s.p.getInventory();
            int reka = inv.getHeldItemSlot();
            boolean pusta = inv.getItem(reka) == null || inv.getItem(reka).getType().isAir();
            if (pusta) inv.setItem(reka, new ItemStack(Material.BARRIER));
            var nadmiar = inv.addItem(it).values();
            if (pusta) inv.setItem(reka, null);
            for (ItemStack nad : nadmiar) s.p.getWorld().dropItemNaturally(s.p.getLocation(), nad);
            if (!nadmiar.isEmpty()) s.p.sendMessage("§cKosz pełny. Reszta zbioru upadła na ziemię obok ciebie.");
            pokazWDloni(s, s.widok);   // zaslepka w dloni wyslala klientowi pusty slot — narzedzie wraca od razu
            if (s.mokro && id.equals("PROF_MEDICINAL_HERB")) s.mokrych += wyd;
            if (klasa == 0) s.pokruszone += ile - wyd;
        }
        String nazwa = plugin.dane().nazwa(id);
        if (nazwa.equals(id)) nazwa = switch (id) {
            case "PROF_KORZEN_KOZLKA" -> "Korzeń kozłka";
            case "PROF_LISC_KONWALII" -> "Liść konwalii";
            default -> id;
        };
        s.zebrane.computeIfAbsent(nazwa, k -> new int[3])[klasa] += wyd;
        // XP wedlug pracy: korzen kozlka cenny (2,5x), lisc lekki (0,4x); pomylka z konwalia uczy tak samo (gracz nie wie)
        double mn = id.equals("PROF_KORZEN_KOZLKA") ? 2.5 : id.equals("PROF_MEDICINAL_HERB") ? 1.0 : 0.4;
        double xp = plugin.dane().g(1) * (0.4 + 0.6 * q) * ile * mn;
        s.xp += plugin.profesje().daj(s.p, PROF, 1, xp, s.r.o.clone().add(0, 0.8, 0));
        zielnik.zbior(s.p, pomylka(s) ? "czosnek" : s.gatunek, q);
        return wyd;
    }

    /** Zuzycie narzedzia za kazdy gest roboczy (ciecie, kopanie, korzen, lisc); obraz w dloni wraca od razu. */
    private void zuzyj(Sesja s) {
        Przedmioty.zuzyj(s.p, s.slotSierpa);
        pokazWDloni(s, s.widok);
    }

    private void uwaga(Sesja s, String u) { if (!s.uwagi.contains(u)) s.uwagi.add(u); }

    private void dzwiek(Sesja s, Sound d, float vol, float pitch) {
        s.p.playSound(s.r.o.clone(), d, SoundCategory.BLOCKS, vol, pitch);   // inni gracze nie widza powiekszonej rosliny
    }

    private void wstrzas(Sesja s) {
        // drzy roslina i ziemia, nie kamera (animacja zranienia czytala sie jak "dostalem")
        for (ZRoslina.Czesc c : s.r.czesci) {
            if (c.zabrana || c == s.chwyt) continue;
            float a = (float) (ThreadLocalRandom.current().nextBoolean() ? 3.5 : -3.5);
            s.r.przechyl(c, ZRoslina.obrotZ(a), new Vector3f(), 0f, 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy && !c.zabrana) s.r.przechyl(c, new org.joml.Quaternionf(), new Vector3f(), 0f, 3); }, 2L);
        }
        dzwiek(s, Sound.BLOCK_ROOTED_DIRT_BREAK, 0.8f, 0.6f);
    }

    /** Bez chodzenia i skakania na czas zbioru (modyfikator przejsciowy — znika sam po wyjsciu z gry). */
    static void zamroz(Player p, boolean tak) {
        for (org.bukkit.attribute.Attribute at : new org.bukkit.attribute.Attribute[]{org.bukkit.attribute.Attribute.MOVEMENT_SPEED, org.bukkit.attribute.Attribute.JUMP_STRENGTH}) {
            var inst = p.getAttribute(at);
            if (inst == null) continue;
            inst.removeModifier(K_ZAMROZ);
            if (tak) inst.addTransientModifier(new org.bukkit.attribute.AttributeModifier(K_ZAMROZ, -1.0,
                    org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1, org.bukkit.inventory.EquipmentSlotGroup.ANY));
        }
    }

    static String czas(long sek) { return sek >= 60 ? (sek / 60) + " min" : sek + " s"; }

    void zakoncz(Sesja s, String komunikat) {
        if (s.konczy) return;
        s.konczy = true;
        if (s.zad != null) s.zad.cancel();
        sesje.remove(s.p.getUniqueId());
        if (!s.zranienie) poKoncu.put(s.p.getUniqueId(), System.currentTimeMillis() + 1200);   // po ciosie tarcza i mikstury od razu
        int zostalo = 0, wszystkich = 0, zdrowych = 0;
        for (ZRoslina.Czesc c : s.r.czesci) if (c.typ.equals("ped") || c.typ.equals("lisc")) {
            wszystkich++;
            // do odrostu licza sie tylko czesci, ktore same by daly zbior: kwitnace zdrowe pedy, mlode liscie
            if (!c.zabrana) { zostalo++; if (c.typ.equals("ped") ? c.s("faza").equals("kw") && !c.b("chory") && !c.b("bezKwiatow") : !c.b("stary")) zdrowych++; }
        }
        Player p = s.p;
        if (p.isOnline()) zamroz(p, false);
        if (p.isOnline()) p.updateInventory();
        // konsekwencje: odrost zalezy od tego, co zostalo
        boolean ruszona = s.postep > 0.05 || s.zleKopanie > 0 || nieruszoneBoczne(s) < liczBoczne(s)
                || s.r.czesci.stream().anyMatch(c -> c.zabrana && (c.typ.equals("lisc") || c.typ.equals("ped")));
        boolean cos = !s.zebrane.isEmpty() || s.zniszczona || s.chore > 0 || ruszona;   // Shift nie cofa ruszonego korzenia ani roztartego liscia
        long odrost = 0;
        String odrostOpis = null;
        boolean pedy = s.gatunek.equals("dziurawiec");
        if (s.zniszczona) { odrost = 30 * 60; odrostOpis = "Kępa zniszczona: odrośnie za 30 min."; }
        else if (s.zebrane.isEmpty() && ruszona && !s.wykopany) { odrost = 5 * 60; odrostOpis = "Ruszona roślina musi odpocząć: wróć za 5 min."; }
        else if (s.wykopany) { odrost = 10 * 60; odrostOpis = nieruszoneBoczne(s) == 0 ? "Korzeń wykopany czysto: nowa rozeta za 10 min." : "Korzeń wyszedł, korzonki zostały w ziemi: nowa rozeta za 10 min."; }
        else if (cos && wszystkich > 0 && zdrowych == 0) { odrost = 10 * 60; odrostOpis = zostalo > 0 ? (pedy ? "Nie zostawiłeś żadnego kwitnącego pędu" : "Zostały same stare liście") + ": odrośnie za 10 min." : "Ogołocona do zera: odrośnie za 10 min."; }
        else if (cos && zdrowych == 1) { odrost = 5 * 60; odrostOpis = (pedy ? "Został jeden kwitnący pęd" : "Został jeden młody liść") + ": odrośnie za 5 min."; }
        else if (cos) { odrost = 2 * 60; odrostOpis = (pedy ? "Zostawiłeś kwitnące pędy (" : "Zostawiłeś młode liście (") + zdrowych + "): odrośnie za 2 min."; }
        // D1 chciwosc: wiecej niz pol pedow scietych (podwojny kwiatostan liczy sie x2) — odrost dluzszy, nastepny okaz slabszy
        boolean chciwie = pedy && !s.zniszczona && s.ciecWszystkich > 0 && s.ciecTotal * 2 > s.ciecWszystkich;
        if (pedy && !s.zebrane.isEmpty()) s.stan.chciwie = chciwie;
        if (chciwie && odrost > 0) {
            odrost *= 2;
            odrostOpis = "Ścięte ponad pół kępy. Zielarze mówią: kto bierze wszystko, ten za rok nie ma czego brać. Odrośnie za " + czas(odrost) + ", słabsza.";
            zielnik.odkryj(p, "dziurawiec_chciwosc");
        }
        if (odrost > 0) s.stan.odrostDo = System.currentTimeMillis() + odrost * 1000;
        if (!s.zebrane.isEmpty()) zielnik.dolicz(p, "_wprawa_" + grupa(s.gatunek), 1, true);
        if (s.mokrych > 0) {
            zielnik.dolicz(p, "_mokre_ziele", s.mokrych, true);
            zielnik.odkryj(p, "dziurawiec_rosa");
        }
        if (pomiar) {
            int[] k = new int[3];
            for (int[] v : s.zebrane.values()) for (int i = 0; i < 3; i++) k[i] += v[i];
            plugin.getLogger().info(String.format("[pomiar] %s %s %d/%d/%d skazone=%d pokruszone=%d sek=%.1f wprawa=%d woda=%b mokro=%b",
                    p.getName(), s.gatunek, k[0], k[1], k[2], s.skazone, s.pokruszone, (System.currentTimeMillis() - s.startMs) / 1000.0, s.wprawa, s.przyWodzie, s.mokro));
        }
        if (!wylaczanie) zapiszStany();   // crash nie cofa swiezych zbiorow
        boolean poZbiorze = odrost > 0;
        if (wylaczanie) {
            s.r.usun();
            if (p.isOnline() && p.getWorld() == s.blok.getWorld()) p.sendBlockChange(s.blok.getLocation(), s.blok.getBlockData());
        } else if (p.isOnline() && p.getWorld() == s.blok.getWorld()) {
            float k0 = SKALA_SWIATA.getOrDefault(s.gatunek, 0.7f);
            s.r.skala(k0, 8);   // oddalenie: zostaje kepa w swiecie
            // blok wraca ZANIM znikna karty (odwrotnie byla dziura na 2-3 klatki)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline() && p.getWorld() == s.blok.getWorld()) p.sendBlockChange(s.blok.getLocation(), wygladBloku(s.gatunek, poZbiorze, s.blok));
            }, 7L);
            Bukkit.getScheduler().runTaskLater(plugin, s.r::usun, 11L);
        } else s.r.usun();
        if (p.isOnline()) {
            if (odrost > 0) {
                long ms = odrost * 20;
                Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline() && p.getWorld() == s.blok.getWorld()) p.sendBlockChange(s.blok.getLocation(), s.blok.getBlockData()); }, ms);
            }
            if (!s.zebrane.isEmpty()) {
                StringBuilder sb = new StringBuilder("§2Zielarstwo: §f");
                boolean pierwszy = true;
                for (Map.Entry<String, int[]> e : s.zebrane.entrySet()) {
                    int[] k = e.getValue();
                    if (!pierwszy) sb.append("§7; §f");
                    pierwszy = false;
                    sb.append(e.getKey()).append(":");
                    String[] kol = {" §c", " §a", " §6"}, slowo = {"słabe", "dobre", "wyborne"};
                    boolean pp = true;
                    for (int i = 2; i >= 0; i--) if (k[i] > 0) { sb.append(pp ? "" : "§7,").append(kol[i]).append(k[i]).append(" ").append(slowo[i]); pp = false; }
                }
                sb.append(" §8· §b+").append(Math.round(s.xp)).append(" XP");
                p.sendMessage(sb.toString());
            }
            if (s.pokruszone > 0) p.sendMessage("§7Słabe sztuki (" + s.pokruszone + ") pokruszyły się w koszu.");
            if (s.mokrych > 0 && zielnik.rada3(p, "mokre")) p.sendMessage("§7Mokre ziele (" + s.mokrych + "). Przesąd zielarzy mówi, że mokre pleśnieje w suszarni.");
            if (s.chore > 0) p.sendMessage("§7Chorych pędów (" + s.chore + ") nie bierzesz do pęczka: ścięte poszły w trawę.");
            if (odrostOpis != null) p.sendMessage("§7" + odrostOpis);
            zielnik.wypiszZaleglosci(p);
            if (komunikat != null) plugin.pasek(p, komunikat);
        }
    }

    boolean wTrakcie(Player p) { return sesje.containsKey(p.getUniqueId()); }

    /** Test/admin: obraca kamere gracza w trakcie zbioru na czesc rosliny (typ, nr) na wysokosci y nad jej kotwica. */
    String celuj(Player p, String typ, int nr, double y) {
        Sesja s = sesje.get(p.getUniqueId());
        if (s == null) return "brak sesji";
        if (nr < 0) {   // najblizsza graczowi, jeszcze nie zebrana
            ZRoslina.Czesc best = null;
            for (ZRoslina.Czesc c : s.r.czesci) if (c.typ.equals(typ) && !c.zabrana && c.box != null && (best == null || c.box.getCenterZ() < best.box.getCenterZ())) best = c;
            if (best == null) return "brak czesci " + typ;
            nr = best.nr;
        }
        for (ZRoslina.Czesc c : s.r.czesci) {
            if (!c.typ.equals(typ) || c.nr != nr) continue;
            Location l;
            if (c.box != null && typ.equals("gleba")) {
                double cx = (c.box.getMinX() + c.box.getMaxX()) / 2, cz = (c.box.getMinZ() + c.box.getMaxZ()) / 2;
                l = s.r.swiat(cx * 1.25, 0.03, cz * 1.25);   // zewnetrzna czesc pola — nad lisciami rozety
            } else if (c.box != null && (typ.equals("lisc") || typ.equals("kwiat")))
                l = s.r.swiat((c.box.getMinX() + c.box.getMaxX()) / 2, (c.box.getMinY() + c.box.getMaxY()) / 2, (c.box.getMinZ() + c.box.getMaxZ()) / 2);
            else l = s.r.swiat(c.kotwica.getX(), c.kotwica.getY() + y, c.kotwica.getZ());
            patrzPlynnie(p, l);
            // cel= punkt w swiecie: bot (mineflayer nie obsluguje face_player) obraca kamere sam i klika jak gracz
            return String.format(java.util.Locale.ROOT, "cel=%.3f,%.3f,%.3f ", l.getX(), l.getY(), l.getZ()) + "celuje w " + typ + " " + nr + " oko=" + String.format("%.2f", p.getEyeLocation().getY() - s.r.o.getY()) + " (h=" + String.format("%.2f", c.d("h")) + ", faza=" + c.s("faza") + ", chory=" + c.b("chory") + ")";
        }
        return "brak czesci " + typ + " " + nr;
    }

    /** Obrot kamery na punkt liczony od oczu gracza na serwerze (lookAt przy siedzeniu celowal za wysoko). */
    static void patrz(Player p, Location cel) {
        p.lookAt(cel.getX(), cel.getY(), cel.getZ(), LookAnchor.EYES);
    }

    /** Test/nagrania: plynny obrot kamery na punkt w ~8 tickach (jak ruch mysza). */
    void patrzPlynnie(Player p, Location cel) {
        Vector oko = p.getEyeLocation().toVector();
        Vector z = p.getEyeLocation().getDirection().normalize(), na = cel.toVector().subtract(oko).normalize();
        int kroki = 8;
        for (int i = 1; i <= kroki; i++) {
            double t = i / (double) kroki, e = t * t * (3 - 2 * t);
            Vector d = z.clone().multiply(1 - e).add(na.clone().multiply(e)).normalize();
            Bukkit.getScheduler().runTaskLater(plugin, () -> { Vector o = p.getEyeLocation().toVector().add(d.clone().multiply(2)); p.lookAt(o.getX(), o.getY(), o.getZ(), LookAnchor.EYES); }, i);
        }
    }

    String startTest(Player p, Block b) {
        String g = kepy.get(klucz(b));
        if (g == null) return "to nie kepa";
        rozpocznij(p, b, g);
        return sesje.containsKey(p.getUniqueId()) ? "sesja " + g : "nie ruszyla (patrz pasek gracza)";
    }

    /** Test: to, co gracz WIDZI (dla botow-strategii) — bez cech ukrytych (chory z daleka, gatunek sobowtora, korzonki). */
    String opis(Player p) {
        Sesja s = sesje.get(p.getUniqueId());
        if (s == null) return "brak sesji";
        StringBuilder b = new StringBuilder("widac " + widocznyGatunek(s) + (s.przyWodzie ? " woda" : "") + (s.mokro ? " mokro" : "") + (s.stan.slaba ? " slaba" : "") + ":");
        for (ZRoslina.Czesc c : s.r.czesci) {
            if (c.zabrana) continue;
            b.append(" ").append(c.typ).append(c.nr);
            switch (c.typ) {
                case "ped" -> b.append("[").append(c.s("faza")).append(c.b("dbl") ? ",dbl" : "").append(c.b("cienki") ? ",cienki" : "")
                        .append(c.b("chorWidac") ? ",chory" : "").append(c.b("bezKwiatow") ? ",bezKwiatow" : "").append(String.format(",h=%.2f]", c.d("h")));
                case "lisc" -> b.append("[").append(c.b("stary") ? "stary" : "mlody").append(c.b("kwitnie") ? ",kwitnie" : "").append("]");
                case "gleba" -> b.append(s.poluzowane[c.nr] ? "[odkopane]" : s.sonda[c.nr] ? "[opukane]" : "");
                default -> { }
            }
        }
        if (s.gatunek.equals("czosnek") || s.gatunek.equals("konwalia")) b.append(" uklad=").append(s.gatunek.equals("czosnek") ? "pojedyncze" : "pary");
        if (s.gatunek.equals("kozlek")) b.append(String.format(" napiecie=%.2f postep=%.2f", s.napiecie, s.postep));
        if (s.hover != null) {   // podswietlenie i biala linie ciecia widzi gracz (linia = ulamek wysokosci pedu)
            b.append(" celujesz=").append(s.hover.typ).append(s.hover.nr);
            Vector hit = s.hover.typ.equals("ped") ? s.r.trafienie(s.hover) : null;
            if (hit != null) b.append(String.format(java.util.Locale.ROOT, " linia=%.2f", (hit.getY() - s.hover.kotwica.getY()) / s.hover.d("h")));
        }
        return b.toString();
    }

    /** Test/admin: pelny opis okazu (wszystkie cechy, takze ukryte). */
    String opisPelny(Player p) {
        Sesja s = sesje.get(p.getUniqueId());
        if (s == null) return "brak sesji";
        StringBuilder b = new StringBuilder(s.gatunek + " jasnosc=" + s.r.jasnosc.getBlockLight() + "/" + s.r.jasnosc.getSkyLight()
                + " blok=" + s.r.o.getBlock().getType() + ":");
        for (ZRoslina.Czesc c : s.r.czesci) b.append(" ").append(c.typ).append(c.nr).append(c.zabrana ? "x" : "").append(c.cechy.isEmpty() ? "" : c.cechy.toString());
        if (s.gatunek.equals("kozlek")) b.append(" boczne=").append(java.util.Arrays.toString(s.boczne)).append(" poluz=").append(java.util.Arrays.toString(s.poluzowane))
                .append(String.format(" napiecie=%.2f postep=%.2f", s.napiecie, s.postep));
        return b.toString();
    }

    private boolean wylaczanie;
    final ZWzory wzory = new ZWzory();   // warianty wygladu do wyboru (test)

    void zatrzymajWszystko() {
        wylaczanie = true;   // zadania opoznione juz sie nie wykonaja — zakoncz sprzata od razu
        wzory.usunWszystko();
        for (Sesja s : new ArrayList<>(sesje.values())) zakoncz(s, null);
        try { PacketEvents.getAPI().getEventManager().unregisterListener(pakiety); } catch (Throwable ignored) { }
        zielnik.zapiszWszystko();
        zapiszStany();
        suszarnia.sprzatnij();
    }

    // ================================================================ stan kep na dysku (odrost i uklad rosliny przezywaja restart)
    /** Partia nierozpoznanych lisci -> gatunek (zapisywane razem ze stanami kep). */
    private final Map<Integer, String> partie = new HashMap<>();

    String gatunekPartii(int partia) { return partie.get(partia); }

    private File plikStanow() { return new File(plugin.getDataFolder(), "zielarz-stany.yml"); }

    void wczytajStany() {
        File f = plikStanow();
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        long teraz = System.currentTimeMillis();
        var ps = y.getConfigurationSection("_partie");
        if (ps != null) for (String k : ps.getKeys(false)) partie.put(Integer.parseInt(k), ps.getString(k));
        for (String u : y.getKeys(false)) {
            if (u.startsWith("_")) continue;
            var sek = y.getConfigurationSection(u);
            if (sek == null) continue;
            Map<String, Stan> m = stany.computeIfAbsent(UUID.fromString(u), k -> new HashMap<>());
            for (String k : sek.getKeys(false)) {
                String[] a = sek.getString(k, "").split(";");
                if (a.length < 3) continue;
                Stan st = new Stan();
                st.ziarno = Long.parseLong(a[0]); st.odrostDo = Long.parseLong(a[1]); st.rozpoznana = Boolean.parseBoolean(a[2]);
                if (a.length >= 5) { st.chciwie = Boolean.parseBoolean(a[3]); st.slaba = Boolean.parseBoolean(a[4]); }
                if (st.trwaly(teraz)) m.put(zYaml(k), st);
            }
        }
    }

    void zapiszStany() {
        YamlConfiguration y = new YamlConfiguration();
        long teraz = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<String, Stan>> e : stany.entrySet())
            for (Map.Entry<String, Stan> k : e.getValue().entrySet()) {
                Stan st = k.getValue();
                if (!st.trwaly(teraz)) continue;   // wygasle nie ida na dysk (w pamieci zostaja do odwiedzin)
                y.set(e.getKey() + "." + doYaml(k.getKey()), st.ziarno + ";" + st.odrostDo + ";" + st.rozpoznana + ";" + st.chciwie + ";" + st.slaba);
            }
        for (Map.Entry<Integer, String> e : partie.entrySet()) y.set("_partie." + e.getKey(), e.getValue());
        String tekst = y.saveToString();
        File f = plikStanow();
        Runnable zapis = () -> { try { java.nio.file.Files.writeString(f.toPath(), tekst); } catch (java.io.IOException ex) { plugin.getLogger().warning("zielarz-stany.yml: " + ex); } };
        if (wylaczanie) zapis.run(); else Bukkit.getScheduler().runTaskAsynchronously(plugin, zapis);   // dysk poza watkiem serwera
    }

    // ================================================================ przerwania i blokady
    /** Wyglad odrastajacej kepy zna tylko zbierajacy — przy kazdym wyslaniu chunka (relog, powrot) odsylamy go jeszcze raz. */
    @EventHandler
    public void naChunkGracza(io.papermc.paper.event.packet.PlayerChunkLoadEvent e) {
        Map<String, Stan> m = stany.get(e.getPlayer().getUniqueId());
        if (m == null) return;
        long teraz = System.currentTimeMillis();
        String pref = e.getWorld().getName() + ";";
        for (Map.Entry<String, Stan> k : m.entrySet()) {
            if (k.getValue().odrostDo <= teraz || !k.getKey().startsWith(pref)) continue;
            String[] a = k.getKey().split(";");
            int x = Integer.parseInt(a[1]), z = Integer.parseInt(a[3]);
            if (x >> 4 != e.getChunk().getX() || z >> 4 != e.getChunk().getZ()) continue;
            String g = kepy.get(k.getKey());
            if (g == null) continue;
            Block b = e.getWorld().getBlockAt(x, Integer.parseInt(a[2]), z);
            Player p = e.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) p.sendBlockChange(b.getLocation(), wygladBloku(g, true, b)); }, 2L);
        }
    }

    /** Zamrozony gracz probuje isc: przypomnienie, jak wstac (WASD nic nie mowilo). */
    @EventHandler
    public void naRuch(org.bukkit.event.player.PlayerInputEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        var in = e.getInput();
        if (s == null || !(in.isForward() || in.isBackward() || in.isLeft() || in.isRight() || in.isJump())) return;
        if (s.t - s.ostatniRuchKom < 60) return;
        s.ostatniRuchKom = s.t;
        plugin.pasek(s.p, "§fPochylasz się nad rośliną. §7Shift: wstań.");
    }

    /** Dymki "New Recipe" przy zbiorze zaslanialy scene — w trakcie i tuz po sesji receptury odkrywaja sie po cichu. */
    @EventHandler
    public void naRecepture(org.bukkit.event.player.PlayerRecipeDiscoverEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        Long po = poKoncu.get(u);
        if (sesje.containsKey(u) || po != null && System.currentTimeMillis() < po + 3000) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void naShift(org.bukkit.event.player.PlayerToggleSneakEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null && e.isSneaking() && !s.konczy) zakoncz(s, "§7Wstajesz znad rośliny.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void naObrazenia(EntityDamageEvent e) {
        // trucizna, ogien, glod nie zrywaja z kolan; cios albo mocne obrazenie tak
        if (!(e instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) && e.getFinalDamage() < 3) return;
        if (e.getEntity() instanceof Player p) {
            Sesja s = sesje.get(p.getUniqueId());
            if (s != null) { s.zranienie = true; zakoncz(s, "§cCoś cię zraniło. Zrywasz się na nogi."); }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void naMachniecie(io.papermc.paper.event.player.PlayerArmSwingEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s == null || e.getHand() != EquipmentSlot.HAND) return;
        e.setCancelled(true);
        lpm(s);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void naSlot(PlayerItemHeldEvent e) { if (sesje.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void naEkwipunek(InventoryClickEvent e) { if (sesje.containsKey(e.getWhoClicked().getUniqueId())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void naUpuszczenie(PlayerDropItemEvent e) { if (sesje.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void naZamianeRak(PlayerSwapHandItemsEvent e) { if (sesje.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void naUderzenieBloku(BlockDamageEvent e) { if (sesje.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }

    @EventHandler
    public void naWyjscie(PlayerQuitEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null) zakoncz(s, null);
        zielnik.zapisz(Zlecenia.profil(e.getPlayer()));
    }

    @EventHandler
    public void naSmierc(PlayerDeathEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null) zakoncz(s, null);
    }

    @EventHandler
    public void naSwiat(PlayerChangedWorldEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null) zakoncz(s, null);
    }

    World swiatKepy(String k) { return Bukkit.getWorld(k.split(";")[0]); }
}
