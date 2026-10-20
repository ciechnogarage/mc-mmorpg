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
    static final Particle.DustOptions GRUDKA = new Particle.DustOptions(Color.fromRGB(93, 63, 42), 0.7f);
    private static final NamespacedKey K_ZAMROZ = new NamespacedKey("rzemioslo", "zielarz_zamroz");

    /** Stan kepy dla gracza: ten sam okaz az do odrostu. */
    static final class Stan {
        long ziarno = ThreadLocalRandom.current().nextLong();
        long odrostDo;
        boolean rozpoznana;
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
        boolean trzeszczy, zaczep1, zaczep2, wykopany;
        int ostatniHover = -99;
        final int partia = ThreadLocalRandom.current().nextInt();   // nierozpoznane liscie z roznych zbiorow nie lacza sie w stos
        // wynik
        final Map<String, int[]> zebrane = new LinkedHashMap<>();   // nazwa -> [ilosc, suma jakosci*100]
        final List<String> uwagi = new ArrayList<>();
        double xp;
        int chore;
        boolean zniszczona, okaz;
        BukkitTask zad;

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
    private final NamespacedKey kJakosc, kZamiana, kGatunek, kSlot, kUchwyt, kPartia;
    private final PacketListenerAbstract pakiety;
    final ZZielnik zielnik;
    final ZSuszarnia suszarnia;

    Zielarz(Rzemioslo plugin) {
        this.plugin = plugin;
        this.plikKep = new File(plugin.getDataFolder(), "zielarz-kepy.yml");
        this.kJakosc = new NamespacedKey(plugin, "jakosc");
        this.kZamiana = new NamespacedKey(plugin, "zielarz_zamiana");
        this.kGatunek = new NamespacedKey(plugin, "gatunek");
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
    static String klucz(Block b) { return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ(); }

    private void wczytajKepy() {
        kepy.clear();
        if (!plikKep.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(plikKep);
        var sek = y.getConfigurationSection("kepy");
        if (sek != null) for (String k : sek.getKeys(false)) kepy.put(k.replace('_', ';').replace('~', '-'), sek.getString(k));
    }

    private void zapiszKepy() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, String> e : kepy.entrySet()) y.set("kepy." + e.getKey().replace(';', '_').replace('-', '~'), e.getValue());
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

    private Stan stan(Player p, Block b) {
        return stany.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).computeIfAbsent(klucz(b), k -> new Stan());
    }

    void rozpocznij(Player p, Block b, String gatunek) {
        if (sesje.containsKey(p.getUniqueId()) || plugin.zbieranie().wTrakcie(p) || p.isInsideVehicle()
                || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.getLocation().distance(b.getLocation().add(0.5, 0, 0.5)) > 5) { plugin.pasek(p, "§7Podejdź bliżej."); return; }
        Stan st = stan(p, b);
        long teraz = System.currentTimeMillis();
        if (st.odrostDo > teraz) {
            Bukkit.getScheduler().runTask(plugin, () -> p.sendBlockChange(b.getLocation(), wygladBloku(gatunek, true, b)));
            plugin.pasek(p, "§7Ta kępa jeszcze odrasta. Wróć za §f" + czas((st.odrostDo - teraz) / 1000) + "§7.");
            return;
        }
        if (st.odrostDo != 0) { Stan nowy = new Stan(); stany.get(p.getUniqueId()).put(klucz(b), nowy); st = nowy; }
        String rodzaj = plugin.dane().narzedzieProfesji.getOrDefault(PROF, "sierp");
        Przedmioty.Znalezione sierp = plugin.przedmioty().najlepsze(p, rodzaj, 1);
        if (sierp == null) {
            plugin.pasek(p, plugin.kom("brak-narzedzia", "{narzedzie}", Przedmioty.nazwaRodzaju(rodzaj), "{tier}", "I"));
            return;
        }
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
        Sesja s = new Sesja(p, b, gatunek, st, r);
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
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.skala(1.08f, 7); }, 1L);   // przyblizenie z lekkim odbiciem
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.skala(1f, 4); }, 9L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!s.konczy) patrzPlynnie(p, o.clone().add(0, 0.62, 0));
        }, 1L);
        p.playSound(p.getLocation(), Sound.BLOCK_GRASS_STEP, SoundCategory.PLAYERS, 0.8f, 0.8f);
        plugin.pasek(p, "§7" + nazwaKepy(s) + " §8· §7Shift: odejdź");
        s.r.zaslon(1.5f, gatunek.equals("kozlek") ? 1.6f : 1.2f);
        String rada = wskazowka(s);
        if (rada != null) p.sendMessage("§8Wskazówka: §7" + rada);
        zielnik.zobaczono(p, widocznyGatunek(s));
        s.zad = Bukkit.getScheduler().runTaskTimer(plugin, () -> tik(s), 1L, 1L);
    }

    /** Pierwsze zbiory: jedna rada, dopoki gracz sam nie zrobi tego dobrze (zielnik pamieta, co odkryl). */
    private String wskazowka(Sesja s) {
        return switch (widocznyGatunek(s)) {
            case "dziurawiec" -> zielnik.ma(s.p, "dziurawiec_wyborny") ? null
                    : "LPM sierpem tnie pęd tam, gdzie celujesz. Moc siedzi w kwiatach: tnij tuż pod nimi, chore pędy zostaw.";
            case "kozlek" -> zielnik.ma(s.p, "kozlek_wyborny") ? null
                    : "Korzeń trzymają korzonki: LPM łopatką rozluźnij ziemię tam, gdzie bieleją. Potem trzymaj PPM na rozecie, a gdy mocno drży, popuść.";
            case "szerokolistna" -> zielnik.ma(s.p, "konwalia_zapach") || zielnik.ma(s.p, "czosnek_zapach") ? null
                    : "Te liście mają trującego sobowtóra. Przytrzymaj PPM na liściu, żeby go roztrzeć i powąchać, zanim zerwiesz.";
            default -> null;
        };
    }

    /** Sobowtory wygladaja tak samo z daleka — nazwa dopiero po rozpoznaniu. */
    private String widocznyGatunek(Sesja s) {
        if ((s.gatunek.equals("czosnek") || s.gatunek.equals("konwalia")) && !s.stan.rozpoznana) return "szerokolistna";
        return s.gatunek;
    }

    private String nazwaKepy(Sesja s) {
        return switch (widocznyGatunek(s)) {
            case "dziurawiec" -> "Kępa dziurawca";
            case "kozlek" -> "Kozłek przy wodzie";
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
        if (c == null) return "sierp";
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
        if (!p.isOnline() || p.isDead() || p.getWorld() != s.blok.getWorld() || p.getLocation().distanceSquared(s.stoi) > 1.0) { zakoncz(s, null); return; }
        s.t++;
        if (s.t <= 3 || s.t % 20 == 0 || s.ukryjZnow) { p.sendBlockChange(s.blok.getLocation(), Material.AIR.createBlockData()); s.ukryjZnow = false; }
        if (s.trzyma && s.t - s.ostatniPpm > 6) ppmPuszczony(s);
        if (!s.trzyma) {
            s.hover = s.t < s.zajetyDo ? null : s.r.celowana(4.5);
            s.r.podswietl(s.hover, PODSWIETL);
            if (s.hover != null) s.ostatniHover = s.t;
            // narzedzie zmienia sie dopiero po chwili celowania w nic (bez migania przy przejezdzaniu wzrokiem)
            String co = s.hover == null && s.t - s.ostatniHover < 8 && !s.widok.isEmpty() ? s.widok : narzedzieDla(s, s.hover);
            if (!co.equals(s.widok) || s.t % 20 == 0) pokazWDloni(s, co);
        } else {
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
            if (s.trzeszczy && s.napiecie < 0.6) s.trzeszczy = false;
        }
        if (s.t - s.ostatniaAkcja > 900) zakoncz(s, "§7Wstajesz.");
        else if (wszystkoZebrane(s) && s.t >= s.zajetyDo) zakoncz(s, null);
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
        if (c == null) return;
        s.ostatniaAkcja = s.t;
        switch (c.typ) {
            case "ped" -> tnijPed(s, c);
            case "gleba" -> kop(s, c);
            case "lisc" -> tnijLiscie(s);
            case "kwiat" -> {
                s.r.usunCzesc(c);
                dzwiek(s, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
                plugin.pasek(s.p, "§7Ścinasz kwiatostan. Kwiaty opadają w trawę.");
            }
            case "rozeta" -> plugin.pasek(s.p, "§7Sierp ślizga się po liściach przy ziemi.");
            default -> { }
        }
    }

    /** Klient trzymajacy PPM powtarza uzycie co 4 ticki — kazde powtorzenie przedluza chwyt; cisza = puszczenie. */
    private void ppmWcisniety(Sesja s) {
        s.ostatniPpm = s.t;
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
        if (dl == 0) klik(s, c);
        else if (c.typ.equals("lisc")) plugin.pasek(s.p, "§7Za krótko, żeby cokolwiek poczuć.");
        else if (c.typ.equals("ped") && dl >= 12) { s.r.przechyl(c, new org.joml.Quaternionf(), new Vector3f(), 0f, 6); plugin.pasek(s.p, "§7Puszczasz. Pęd prostuje się."); }
    }

    private void klik(Sesja s, ZRoslina.Czesc c) {
        switch (c.typ) {
            case "lisc" -> zerwijLisc(s, c);
            case "ped" -> zerwijKwiatyDziurawca(s, c);
            case "kwiat" -> plugin.pasek(s.p, s.gatunek.equals("konwalia") ? "§7Drobne białe dzwonki. Lepiej ich nie zrywać."
                    : "§7Gwiazdki kwiatów na wysokiej łodyżce.");
            case "rozeta" -> {
                dzwiek(s, Sound.BLOCK_HANGING_ROOTS_STEP, 0.7f, 0.7f);
                plugin.pasek(s.p, "§7Szarpnięcie. Siedzi w ziemi.");
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
                    if (dl == 12) plugin.pasek(s.p, "§6Pęd napina się w ziemi. Puść, jeśli nie chcesz go wyrwać.");
                }
            }
            case "lisc" -> rozcieraj(s, c, dl);
            default -> { }
        }
    }

    // ================================================================ DZIURAWIEC: ciecie sierpem
    /** Segmenty pedu dziurawca: polozenie i rozmiar kart (w jednostkach seg = h/4). */
    static final double[] SEG_Y = {0, 0.75, 1.45, 2.05}, SEG_R = {0.85, 0.85, 0.9, 2.0};

    private void budujDziurawiec(Sesja s) {
        Random rnd = s.rnd;
        ZRoslina r = s.r;
        int n = 7 + rnd.nextInt(2);   // gesto: krzak, nie rzad tyczek
        for (int i = 0; i < n; i++) {
            double lx = -0.34 + 0.68 * i / (n - 1) + (rnd.nextDouble() * 0.08 - 0.04);
            double lz = (i % 2 == 0 ? -0.1 : 0.12) + rnd.nextDouble() * 0.1;
            double h = (i == 0 || i == n - 1 ? 0.62 : 0.74) + rnd.nextDouble() * 0.12;   // kopula, ~30% nizej
            double v = rnd.nextDouble();
            String faza = v < 0.1 ? "pak" : v < 0.88 ? "kw" : "stary";
            boolean chory = rnd.nextDouble() < 0.18, dbl = faza.equals("kw") && rnd.nextDouble() < 0.12;
            ZRoslina.Czesc c = r.czesc("ped", i, lx, 0, lz);
            c.cechy.put("h", h); c.cechy.put("faza", faza); c.cechy.put("chory", chory); c.cechy.put("dbl", dbl);
            double seg = h / 4;
            c.box = new BoundingBox(lx - 0.12, 0, lz - 0.12, lx + 0.12, (SEG_Y[3] + SEG_R[3] * 0.8) * seg, lz + 0.12);
            String[] karty = {"dziurawiec_dol", chory ? "dziurawiec_srodek_chory" : "dziurawiec_srodek",
                    chory ? "dziurawiec_gora_chory" : "dziurawiec_gora",
                    faza.equals("pak") ? "dziurawiec_pak" : faza.equals("stary") ? "dziurawiec_przekwit" : "dziurawiec_kwiat"};
            double obrot = rnd.nextDouble() * 90;
            // niska, gesta kepa jak blok w swiecie: krotki dol, nakladajace sie segmenty, duzy kwiatostan
            for (int k = 0; k < 4; k++) {
                ZRoslina.Klocek kl = r.karta(c, "zielarz_k_" + karty[k], 0, SEG_Y[k] * seg, 0, SEG_R[k] * seg, ZRoslina.obrotY(obrot + k * 13));
                kl.seg = k;
            }
            if (dbl) r.karta(c, "zielarz_k_dziurawiec_kwiat", 0.06, seg * 1.6, 0.04, seg * 1.3, ZRoslina.obrotY(obrot + 45)).seg = 3;
        }
    }

    /** Ciecie na granicy segmentu (4 karty pedu): kwiaty / gorny ped / srodek / zdrewnialy dol = strefy jakosci. */
    private void tnijPed(Sesja s, ZRoslina.Czesc c) {
        Vector hit = s.r.trafienie(c);
        if (hit == null) return;
        double h = c.d("h"), seg = h / 4;
        double yy = (hit.getY() - c.kotwica.getY()) / seg;
        int k = yy < SEG_Y[1] ? 0 : yy < SEG_Y[2] ? 1 : yy < SEG_Y[3] ? 2 : 3;
        float cy = (float) Math.max(0.04, SEG_Y[k] * seg);
        String faza = c.s("faza");
        double q;
        String m;
        if (faza.equals("pak")) { q = 0.3; m = "Pąki. Ziele jeszcze bez mocy."; }
        else if (faza.equals("stary")) { q = 0.25; m = "Przekwitłe. Ziele bez mocy."; }
        else if (c.b("bezKwiatow") && k >= 2) { q = 0; m = "Bez kwiatów to sama łodyga. Moc zerwałeś wcześniej."; }
        else if (k == 3) { q = 0.4; m = "Same kwiaty, mało ziela."; }
        else if (k == 2) { q = 1.0; m = "Czyste cięcie pod kwiatami."; }
        else if (k == 1) { q = 0.65; m = "Za nisko: w pęczku starsze liście."; }
        else { q = 0.35; m = "Za nisko: zdrewniała łodyga w pęczku."; }
        boolean chory = c.b("chory");
        if (chory) { q = 0; s.chore++; m = "Liście z ciemnymi plamkami w pęczku."; zielnik.odkryj(s.p, "dziurawiec_chory"); }
        boolean okaz = q >= 1.0 && c.b("dbl");
        if (okaz) { s.okaz = true; m = "Czyste cięcie. Podwójny kwiatostan!"; }
        float strona = s.rnd.nextBoolean() ? 1 : -1;
        s.r.przechyl(c, ZRoslina.obrotZ(80 * strona), new Vector3f(0, -cy * 0.9f, -0.06f), cy, 12);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (s.konczy) return;
            c.klocki.removeIf(kl -> { if (kl.y0 >= cy - 1e-3) { kl.d.remove(); return true; } return false; });
            dzwiek(s, Sound.BLOCK_AZALEA_LEAVES_FALL, 0.6f, 1.2f);
        }, 20L);
        c.zabrana = true;
        c.box = null;
        s.zajetyDo = s.t + 6;
        dzwiek(s, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.55f, 1.45f);
        dzwiek(s, Sound.BLOCK_GRASS_BREAK, 0.8f, 1.2f);
        Location pk = s.r.swiat(c.kotwica.getX(), cy, c.kotwica.getZ());
        s.p.spawnParticle(Particle.BLOCK, pk, 12, 0.06, 0.03, 0.06, Material.OAK_LEAVES.createBlockData());
        Przedmioty.zuzyj(s.p, s.slotSierpa);
        if (q > 0) daj(s, "PROF_MEDICINAL_HERB", 1, q);
        else if (chory) uwaga(s, "chore");
        if (okaz) daj(s, "PROF_MEDICINAL_HERB", 1, 1.0);
        plugin.pasek(s.p, (q >= 0.85 ? "§a" : q >= 0.5 ? "§e" : "§c") + m);
    }

    private void zerwijKwiatyDziurawca(Sesja s, ZRoslina.Czesc c) {
        if (c.b("bezKwiatow")) { plugin.pasek(s.p, "§7Kwiatów już tu nie ma."); return; }
        c.klocki.removeIf(kl -> { if (kl.seg == 3) { kl.d.remove(); return true; } return false; });
        c.cechy.put("bezKwiatow", true);
        dzwiek(s, Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.6f, 1.6f);
        if (c.s("faza").equals("kw") && !c.b("chory")) { daj(s, "PROF_MEDICINAL_HERB", 1, 0.4); plugin.pasek(s.p, "§eZrywasz same kwiaty. Mało ziela."); }
        else plugin.pasek(s.p, "§7Zrywasz palcami. Nic, co by się nadało.");
    }

    private void wyrwijPed(Sesja s, ZRoslina.Czesc c) {
        s.chwytZalatwiony = true;
        s.r.przechyl(c, ZRoslina.obrotZ(25), new Vector3f(0, 0.35f, -0.1f), -1f, 6);
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
        if (!c.b("chory")) daj(s, "PROF_MEDICINAL_HERB", 1, 0.25);
        uwaga(s, "wyrwany");
        plugin.pasek(s.p, "§cWyrwany z korzeniem. Ziele w ziemi, a kępa nie odrośnie szybko.");
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
            if (s.boczne[i] && rnd.nextDouble() < 0.6)   // bielejaca koncowka korzonka zdradza, gdzie biegnie
                r.klocek(g, Material.STRIPPED_BIRCH_WOOD, sx[i] * 0.8 + (rnd.nextDouble() - 0.5) * 0.08, 0.015, sz[i] * 0.8 + (rnd.nextDouble() - 0.5) * 0.08,
                        0.04, 0.03, 0.1, ZRoslina.obrotY(i * 90 + rnd.nextDouble() * 40 - 20));
        }
        ZRoslina.Czesc roz = r.czesc("rozeta", 0, 0, 0, 0);
        roz.box = new BoundingBox(-0.2, 0, -0.2, 0.2, 0.12, 0.2);
        for (int k = 0; k < 7; k++) {   // pierzaste liscie rozety: karta lezaca, nasada w srodku
            double a = k * 360.0 / 7 + rnd.nextDouble() * 20, len = 0.5 + rnd.nextDouble() * 0.1, ar = Math.toRadians(a);
            Quaternionf q = ZRoslina.obrotY(a).mul(ZRoslina.obrotX(-48));
            r.kartaSrodek(roz, "zielarz_k_kozlek_lisc", -Math.cos(ar) * len * 0.4, len * 0.3, -Math.sin(ar) * len * 0.4, len, q);
        }
        int lodygi = 1 + rnd.nextInt(2);
        for (int i = 0; i < lodygi; i++) {
            double lx = (rnd.nextDouble() - 0.5) * 0.12, lz = (rnd.nextDouble() - 0.5) * 0.12, seg = 0.42 + rnd.nextDouble() * 0.08;
            double ob = rnd.nextDouble() * 90;
            r.karta(roz, "zielarz_k_kozlek_lodyga", lx, 0, lz, seg, ZRoslina.obrotY(ob));
            r.karta(roz, "zielarz_k_kozlek_lodyga", lx, seg, lz, seg, ZRoslina.obrotY(ob + 40));
            r.karta(roz, "zielarz_k_kozlek_baldach", lx, seg * 2 - 0.12, lz, seg * 1.15, ZRoslina.obrotY(ob + 20));
        }
        // korzen pod ziemia (dwie skrzyzowane karty), wychodzi przy ciagnieciu
        r.karta(roz, "zielarz_k_kozlek_korzen", 0, -0.62, 0, 0.62, ZRoslina.obrotY(20));
        r.karta(roz, "zielarz_k_kozlek_korzen", 0, -0.62, 0, 0.62, ZRoslina.obrotY(110));
    }

    private static ZRoslina.Czesc rozeta(Sesja s) {
        for (ZRoslina.Czesc c : s.r.czesci) if (c.typ.equals("rozeta")) return c;
        return null;
    }

    private int nieruszoneBoczne(Sesja s) {
        int n = 0;
        for (int i = 0; i < 4; i++) if (s.boczne[i] && !s.poluzowane[i]) n++;
        return n;
    }

    private void kop(Sesja s, ZRoslina.Czesc g) {
        int i = g.nr;
        if (s.poluzowane[i]) { plugin.pasek(s.p, "§7Tu ziemia już rozluźniona."); return; }
        if (s.postep > 0.98) return;
        s.poluzowane[i] = true;
        s.zajetyDo = s.t + 8;
        var gleba = ZRoslina.blok("zielarz_k_kozlek_gleba");
        for (ZRoslina.Klocek k : g.klocki) {
            if (k.d instanceof BlockDisplay bd && bd.getBlock().equals(gleba)) bd.setBlock(ZRoslina.blok("zielarz_k_kozlek_gleba_rozkopana"));
            else if (k.d instanceof org.bukkit.entity.ItemDisplay id && "zielarz_k_kozlek_gleba".equals(com.nexomc.nexo.api.NexoItems.idFromItem(id.getItemStack())))
                id.setItemStack(ZRoslina.przedmiot("zielarz_k_kozlek_gleba_rozkopana"));
        }
        Location l = s.r.swiat((g.box.getMinX() + g.box.getMaxX()) / 2, 0.05, (g.box.getMinZ() + g.box.getMaxZ()) / 2);
        s.p.spawnParticle(Particle.DUST, l, 14, 0.12, 0.04, 0.12, GRUDKA);
        dzwiek(s, Sound.ITEM_SHOVEL_FLATTEN, 0.9f, 0.8f);
        dzwiek(s, Sound.BLOCK_ROOTED_DIRT_BREAK, 0.7f, 0.9f);
        plugin.pasek(s.p, s.boczne[i] ? "§7Ziemia puszcza. W ściance wykopu bieleje korzonek." : "§7Sama ziemia, nic tu nie trzyma.");
    }

    private void ciagnij(Sesja s, ZRoslina.Czesc roz) {
        if (s.t - s.ostatniPpm > 6) return;   // klient juz puscil — napiecie schodzi w tik()
        int otw = nieruszoneBoczne(s);
        // nieodkopane korzonki trzymaja: napiecie skacze szybciej, trudniej utrzymac je w pasmie
        double rate = 0.014 + 0.005 * otw;   // strojone symulacja: ciagle trzymanie zawsze urywa, uwazny gracz wyciaga w 5-7 s
        s.napiecie += rate;
        if (!s.zaczep1 && s.postep >= 0.32 || !s.zaczep2 && s.postep >= 0.66) {   // korzonek trzyma: szarpniecie
            if (s.postep >= 0.66) s.zaczep2 = true; else s.zaczep1 = true;
            s.napiecie += 0.05;
            dzwiek(s, Sound.BLOCK_ROOTED_DIRT_HIT, 0.9f, 0.7f);
        }
        boolean wPasmie = s.napiecie >= PASMO_OD && s.napiecie <= PASMO_DO;
        s.postep += s.napiecie < PASMO_OD ? 0.002 : wPasmie ? 0.025 : 0.006;
        int dl = s.t - s.trzymaOd;
        // widoczne napiecie: im mocniej, tym rozeta drzy szerzej i przechyla sie; w pasmie ziemia pracuje
        float drz = (float) (s.napiecie < PASMO_OD ? 0.004 : wPasmie ? 0.01 : 0.03);
        float jx = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * drz), jz = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * drz);
        s.r.przesun(roz, new Vector3f(jx, (float) (s.postep * 0.5 + (s.napiecie > PASMO_DO ? 0.03 : 0)), jz), 2);
        Location base = s.r.swiat(0, 0.04, 0);
        if (wPasmie && dl % 3 == 0) s.p.spawnParticle(Particle.DUST, base, 3, 0.16, 0.02, 0.16, GRUDKA);
        if (wPasmie && dl % 4 == 0) dzwiek(s, Sound.BLOCK_ROOTED_DIRT_STEP, 0.6f, 0.8f);
        if (s.napiecie > PASMO_DO && dl % 2 == 0) dzwiek(s, Sound.BLOCK_HANGING_ROOTS_BREAK, (float) (0.4 + 0.5 * s.napiecie), (float) (0.9 + 1.0 * (s.napiecie - PASMO_DO)));
        if (s.napiecie > 0.86 && !s.trzeszczy) { s.trzeszczy = true; wstrzas(s); plugin.pasek(s.p, "§6Korzeń trzeszczy… popuść."); }
        if (s.napiecie >= ZERWANIE) { zerwijKorzen(s, roz, otw); return; }
        if (s.postep >= 1.0) wyciagnijKorzen(s, roz, otw);
    }

    private void zerwijKorzen(Sesja s, ZRoslina.Czesc roz, int otw) {
        s.chwytZalatwiony = true;
        roz.zabrana = true;
        roz.box = null;
        s.zniszczona = true;
        s.zajetyDo = s.t + 14;
        double q = Math.min(0.45, Math.max(0, s.postep * (1 - 0.15 * otw)));   // urwany = zawsze slaby
        s.r.przechyl(roz, ZRoslina.obrotZ(70), new Vector3f(0, (float) (s.postep * 0.5 - 0.2), -0.1f), -2f, 8);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.usunCzesc(roz); }, 20L);
        dzwiek(s, Sound.ENTITY_ITEM_BREAK, 0.8f, 0.6f);
        dzwiek(s, Sound.BLOCK_ROOTED_DIRT_BREAK, 1f, 0.6f);
        wstrzas(s);
        if (s.postep > 0.3) daj(s, "PROF_KORZEN_KOZLKA", 1, q);
        uwaga(s, "urwany");
        plugin.pasek(s.p, "§cKorzeń urwał się w ziemi." + (s.postep > 0.3 ? " W ręku tylko kawałek." : ""));
    }

    private void wyciagnijKorzen(Sesja s, ZRoslina.Czesc roz, int otw) {
        s.chwytZalatwiony = true;
        roz.zabrana = true;
        roz.box = null;
        s.wykopany = true;
        s.zajetyDo = s.t + 16;
        double q = Math.max(0.2, 1 - 0.15 * otw);
        s.r.przechyl(roz, ZRoslina.obrotZ(28), new Vector3f(0, 0.95f, -0.25f), -2f, 10);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.usunCzesc(roz); }, 26L);
        Location base = s.r.swiat(0, 0.05, 0);
        s.p.spawnParticle(Particle.DUST, base, 30, 0.22, 0.08, 0.22, GRUDKA);
        dzwiek(s, Sound.BLOCK_ROOTED_DIRT_BREAK, 1f, 1.1f);
        dzwiek(s, Sound.BLOCK_MOSS_BREAK, 0.8f, 0.8f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> dzwiek(s, Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.0f), 8L);
        daj(s, "PROF_KORZEN_KOZLKA", 1, q);
        plugin.pasek(s.p, otw == 0 ? "§aKorzeń wyszedł cały, z korzonkami." : "§eKorzeń wyszedł, ale korzonki zostały w ziemi (" + otw + ").");
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
            r.karta(l, "zielarz_k_" + lisc + (stary ? "_stary" : ""), bx, 0, bz, len, ZRoslina.obrotY(-a).mul(ZRoslina.obrotX(tilt)));
            double ex = bx + Math.sin(ar) * Math.sin(tr) * len, ez = bz + Math.cos(ar) * Math.sin(tr) * len, ey = Math.cos(tr) * len;
            double cx = (bx + ex) / 2, cz = (bz + ez) / 2;
            l.box = new BoundingBox(Math.min(bx, ex) - 0.07, len * 0.25, Math.min(bz, ez) - 0.07, Math.max(bx, ex) + 0.07, ey, Math.max(bz, ez) + 0.07);
        }
        if (kwitnie) {
            ZRoslina.Czesc k = r.czesc("kwiat", 0, 0, 0, 0);
            double h = czosnek ? 0.78 : 0.62;
            // kwiaty na kartach odchylonych ku niebu: encja cieniuje kierunkowo i pionowa biel wychodzila szara
            if (czosnek) {
                r.karta(k, "zielarz_k_czosnek_lodyzka", 0, 0, 0, h * 0.85, ZRoslina.obrotY(rnd.nextDouble() * 90));
                r.kartaSrodek(k, "zielarz_k_czosnek_baldach", 0, h * 0.8, 0.02, h * 0.42, ZRoslina.obrotX(-68));
            } else {
                r.karta(k, "zielarz_k_konwalia_kwiat", 0.04, 0, 0.06, h, ZRoslina.obrotX(28));   // twarza do gracza, odchylona
            }
            k.box = new BoundingBox(-0.12, h * 0.6, -0.12, 0.12, h, 0.12);
        }
    }

    private String idLiscia(Sesja s) {
        if (!s.stan.rozpoznana) return "PROF_SZEROKIE_LISCIE";
        return s.gatunek.equals("czosnek") ? "PROF_WILD_SPICE" : "PROF_LISC_KONWALII";
    }

    private double jakoscLiscia(ZRoslina.Czesc l, String gatunek) {
        double q = l.b("stary") ? 0.5 : 1.0;
        if (gatunek.equals("czosnek") && l.b("kwitnie")) q *= 0.8;
        return q;
    }

    private void zerwijLisc(Sesja s, ZRoslina.Czesc l) {
        s.r.przechyl(l, ZRoslina.obrotX(-35), new Vector3f(0, 0.18f, -0.25f), -1f, 5);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy) s.r.usunCzesc(l); }, 7L);
        l.zabrana = true;
        l.box = null;
        s.zajetyDo = s.t + 3;
        dzwiek(s, Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.7f, 1.3f);
        double q = jakoscLiscia(l, s.gatunek);
        daj(s, idLiscia(s), 1, q);
        if (!s.stan.rozpoznana) plugin.pasek(s.p, "§7Zerwany liść. Nie wiesz jeszcze, co to za roślina.");
        else plugin.pasek(s.p, (q >= 0.85 ? "§a" : "§e") + (l.b("stary") ? "Stary liść, żółknie na końcu." : "Młody, jędrny liść."));
    }

    private void tnijLiscie(Sesja s) {
        int n = 0;
        double suma = 0;
        for (ZRoslina.Czesc c : s.r.czesci) {
            if (c.zabrana || !c.typ.equals("lisc")) continue;
            double q = jakoscLiscia(c, s.gatunek) * 0.7;
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
        daj(s, idLiscia(s), n, suma / n);
        plugin.pasek(s.p, "§eŚcinasz wszystko jednym ruchem. Liście pogniecione.");
    }

    private void rozcieraj(Sesja s, ZRoslina.Czesc l, int dl) {
        if (dl % 4 == 0) {
            Location gdzie = s.r.swiat((l.box.getMinX() + l.box.getMaxX()) / 2, (l.box.getMinY() + l.box.getMaxY()) / 2, (l.box.getMinZ() + l.box.getMaxZ()) / 2);
            s.p.spawnParticle(Particle.DUST, gdzie, 3, 0.04, 0.04, 0.04, new Particle.DustOptions(Color.fromRGB(90, 150, 60), 0.7f));
            dzwiek(s, Sound.ITEM_BRUSH_BRUSHING_GENERIC, 0.6f, 1.3f);
        }
        if (dl < 20) return;
        s.chwytZalatwiony = true;
        s.r.usunCzesc(l);
        boolean pierwszy = !s.stan.rozpoznana;
        s.stan.rozpoznana = true;
        boolean czosnek = s.gatunek.equals("czosnek");
        dzwiek(s, Sound.BLOCK_MOSS_CARPET_BREAK, 0.8f, 1.4f);
        plugin.pasek(s.p, czosnek ? "§aRozcierasz liść w palcach. Ostry zapach czosnku." : "§6Rozcierasz liść. Bez zapachu, tylko gorzki sok. To konwalia.");
        if (pierwszy) zielnik.odkryj(s.p, czosnek ? "czosnek_zapach" : "konwalia_zapach");
    }

    // ================================================================ wspolne
    private void daj(Sesja s, String id, int ile, double q) {
        q = Math.max(0, Math.min(1, q));
        int klasa = q >= 0.85 ? 2 : q >= 0.5 ? 1 : 0;
        ItemStack it = plugin.przedmioty().zrob(id, ile);
        if (it == null) { plugin.getLogger().warning("Zielarz: brak przedmiotu " + id); return; }
        ItemMeta m = it.getItemMeta();
        List<Component> lore = m.lore() == null ? new ArrayList<>() : new ArrayList<>(m.lore());
        lore.add(Component.text(klasa == 2 ? "§6Jakość: wyborna" : klasa == 1 ? "§aJakość: dobra" : "§cJakość: słaba"));
        if (id.equals("PROF_SZEROKIE_LISCIE")) lore.add(Component.text("§8Nierozpoznane. Rozetrzyj, zanim zerwiesz."));
        m.lore(lore);
        m.getPersistentDataContainer().set(kJakosc, PersistentDataType.INTEGER, klasa);
        if (id.equals("PROF_SZEROKIE_LISCIE")) {
            m.getPersistentDataContainer().set(kGatunek, PersistentDataType.STRING, s.gatunek);
            m.getPersistentDataContainer().set(kPartia, PersistentDataType.INTEGER, s.partia);
        }
        it.setItemMeta(m);
        for (ItemStack nad : s.p.getInventory().addItem(it).values()) s.p.getWorld().dropItemNaturally(s.p.getLocation(), nad);
        String nazwa = plugin.dane().nazwa(id);
        if (nazwa.equals(id)) nazwa = switch (id) {
            case "PROF_KORZEN_KOZLKA" -> "Korzeń kozłka";
            case "PROF_LISC_KONWALII" -> "Liść konwalii";
            case "PROF_SZEROKIE_LISCIE" -> "Szerokie liście";
            default -> id;
        };
        int[] z = s.zebrane.computeIfAbsent(nazwa, k -> new int[2]);
        z[0] += ile;
        z[1] += (int) Math.round(q * 100) * ile;
        double xp = plugin.dane().g(1) * (0.4 + 0.6 * q) * ile;
        s.xp += plugin.profesje().daj(s.p, PROF, 1, xp, s.r.o.clone().add(0, 0.8, 0));
        zielnik.zbior(s.p, s.gatunek, q);
    }

    private void uwaga(Sesja s, String u) { if (!s.uwagi.contains(u)) s.uwagi.add(u); }

    private void dzwiek(Sesja s, Sound d, float vol, float pitch) {
        s.p.playSound(s.r.o.clone(), d, SoundCategory.BLOCKS, vol, pitch);   // inni gracze nie widza powiekszonej rosliny
    }

    private void wstrzas(Sesja s) {
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(s.p, new WrapperPlayServerHurtAnimation(s.p.getEntityId(), 0f));
        } catch (Throwable t) {
            plugin.getLogger().warning("Zielarz: wstrzas: " + t);
        }
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
        poKoncu.put(s.p.getUniqueId(), System.currentTimeMillis() + 1200);
        int zostalo = 0, wszystkich = 0, zdrowych = 0;
        for (ZRoslina.Czesc c : s.r.czesci) if (c.typ.equals("ped") || c.typ.equals("lisc")) {
            wszystkich++;
            if (!c.zabrana) { zostalo++; if (!c.b("chory")) zdrowych++; }
        }
        Player p = s.p;
        if (p.isOnline()) zamroz(p, false);
        if (p.isOnline()) p.updateInventory();
        // konsekwencje: odrost zalezy od tego, co zostalo
        boolean cos = !s.zebrane.isEmpty() || s.zniszczona || s.chore > 0;
        long odrost = 0;
        String odrostOpis = null;
        boolean pedy = s.gatunek.equals("dziurawiec");
        if (s.zniszczona) { odrost = 30 * 60; odrostOpis = "Kępa zniszczona: odrośnie za 30 min."; }
        else if (s.wykopany) { odrost = 10 * 60; odrostOpis = "Korzeń wykopany czysto: nowa rozeta z korzonków za 10 min."; }
        else if (cos && wszystkich > 0 && zdrowych == 0) { odrost = 10 * 60; odrostOpis = zostalo > 0 ? "Zostały same chore pędy: odrośnie za 10 min." : "Ogołocona do zera: odrośnie za 10 min."; }
        else if (cos && zdrowych == 1) { odrost = 5 * 60; odrostOpis = (pedy ? "Został jeden zdrowy pęd" : "Został jeden liść") + ": odrośnie za 5 min."; }
        else if (cos) { odrost = 2 * 60; odrostOpis = (pedy ? "Zostawiłeś zdrowe pędy (" : "Zostawiłeś liście (") + zdrowych + "): odrośnie za 2 min."; }
        if (odrost > 0) s.stan.odrostDo = System.currentTimeMillis() + odrost * 1000;
        boolean poZbiorze = odrost > 0;
        if (p.isOnline() && p.getWorld() == s.blok.getWorld()) {
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
                    int q = e.getValue()[1] / Math.max(1, e.getValue()[0]);
                    if (!pierwszy) sb.append("§7, §f");
                    pierwszy = false;
                    sb.append(e.getValue()[0]).append("× ").append(e.getKey()).append(q >= 85 ? " §6(wyborne)" : q >= 50 ? " §a(dobre)" : " §c(słabe)");
                }
                sb.append(" §8· §b+").append(Math.round(s.xp)).append(" XP");
                p.sendMessage(sb.toString());
            }
            if (s.chore > 0) p.sendMessage("§7Chore pędy w pęczku (" + s.chore + ") pójdą do wyrzucenia przy suszeniu.");
            if (odrostOpis != null) p.sendMessage("§8" + odrostOpis);
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
            return "celuje w " + typ + " " + nr + " oko=" + String.format("%.2f", p.getEyeLocation().getY() - s.r.o.getY()) + " (h=" + String.format("%.2f", c.d("h")) + ", faza=" + c.s("faza") + ", chory=" + c.b("chory") + ")";
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

    /** Test/admin: opis okazu (czesci i ich cechy). */
    String opis(Player p) {
        Sesja s = sesje.get(p.getUniqueId());
        if (s == null) return "brak sesji";
        StringBuilder b = new StringBuilder(s.gatunek + " jasnosc=" + s.r.jasnosc.getBlockLight() + "/" + s.r.jasnosc.getSkyLight()
                + " blok=" + s.r.o.getBlock().getType() + ":");
        for (ZRoslina.Czesc c : s.r.czesci) b.append(" ").append(c.typ).append(c.nr).append(c.zabrana ? "x" : "").append(c.cechy.isEmpty() ? "" : c.cechy.toString());
        if (s.gatunek.equals("kozlek")) b.append(" boczne=").append(java.util.Arrays.toString(s.boczne)).append(" poluz=").append(java.util.Arrays.toString(s.poluzowane))
                .append(String.format(" napiecie=%.2f postep=%.2f", s.napiecie, s.postep));
        return b.toString();
    }

    void zatrzymajWszystko() {
        for (Sesja s : new ArrayList<>(sesje.values())) zakoncz(s, null);
        try { PacketEvents.getAPI().getEventManager().unregisterListener(pakiety); } catch (Throwable ignored) { }
        zielnik.zapiszWszystko();
        zapiszStany();
        suszarnia.sprzatnij();
    }

    // ================================================================ stan kep na dysku (odrost i uklad rosliny przezywaja restart)
    private File plikStanow() { return new File(plugin.getDataFolder(), "zielarz-stany.yml"); }

    void wczytajStany() {
        File f = plikStanow();
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        long teraz = System.currentTimeMillis();
        for (String u : y.getKeys(false)) {
            var sek = y.getConfigurationSection(u);
            if (sek == null) continue;
            Map<String, Stan> m = stany.computeIfAbsent(UUID.fromString(u), k -> new HashMap<>());
            for (String k : sek.getKeys(false)) {
                String[] a = sek.getString(k, "").split(";");
                if (a.length < 3) continue;
                Stan st = new Stan();
                st.ziarno = Long.parseLong(a[0]); st.odrostDo = Long.parseLong(a[1]); st.rozpoznana = Boolean.parseBoolean(a[2]);
                if (st.odrostDo > teraz || st.rozpoznana) m.put(k.replace('_', ';').replace('~', '-'), st);
            }
        }
    }

    void zapiszStany() {
        YamlConfiguration y = new YamlConfiguration();
        long teraz = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<String, Stan>> e : stany.entrySet())
            for (Map.Entry<String, Stan> k : e.getValue().entrySet()) {
                Stan st = k.getValue();
                if (st.odrostDo <= teraz && !st.rozpoznana) continue;
                y.set(e.getKey() + "." + k.getKey().replace(';', '_').replace('-', '~'), st.ziarno + ";" + st.odrostDo + ";" + st.rozpoznana);
            }
        try { y.save(plikStanow()); } catch (java.io.IOException ex) { plugin.getLogger().warning("zielarz-stany.yml: " + ex); }
    }

    // ================================================================ przerwania i blokady
    @EventHandler(priority = EventPriority.LOWEST)
    public void naShift(org.bukkit.event.player.PlayerToggleSneakEvent e) {
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s != null && e.isSneaking() && !s.konczy) zakoncz(s, "§7Wstajesz znad rośliny.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void naObrazenia(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            Sesja s = sesje.get(p.getUniqueId());
            if (s != null) zakoncz(s, "§cCoś cię zraniło. Zrywasz się na nogi.");
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
        zielnik.zapisz(e.getPlayer().getUniqueId());
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
