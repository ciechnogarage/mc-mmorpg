package pl.mcmmorpg.rzemioslo;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Mysliwy: rewir -> tropy -> podchod pod wiatr -> cios -> zwloki (Oprawianie) -> scena ciec w kolejnosci zaleznej
 *  od zwierzecia -> skora + dziczyzna (jakosc, moze byc 0; spalona skora) -> wedzenie dziczyzny nad ogniskiem.
 *  Wszystko widac dla wszystkich (czasteczki, prawdziwy mob, ItemDisplay); podpowiedzi tylko wykonawcy, dopoki wprawa < 5. */
final class Mysliwy implements Listener {
    static final String PROF = "hunting";
    static final String MIESO = "PROF_DZICZYZNA", SUSZONE = "PROF_SUSZONA_DZICZYZNA";
    static final int CZAS_LOWOW = 20 * 150;          // tropienie + podchod
    static final double WECH = 13, SLUCH = 10, WZROK = 6.5, ODLEGLOSC_ZWIERZA = 17;
    static final int OSWOJENIE = 30;               // ticki po pojawieniu sie zwierza bez sprawdzania (zdazysz kucnac)
    static final int PROWADZENIE = 14;               // ticki prowadzenia noza przy cieciu (>= 700 ms)
    // celowanie nozem: promien oka przecina poziom punktu ciecia; trafienie <= CEL od znaku, poslizg > POSLIZG
    // (kat byl zly: z boku tuszy punkty leza jeden za drugim na linii wzroku i kat ich nie rozroznia)
    static final double CEL = 0.2, POSLIZG = 0.34;
    static final long WEDZENIE_MS = 90_000;          // wycinek testowy; docelowo kilka minut
    static final int MAX_NA_OGNISKU = 4;
    static final double ZASIEG_SCENY = 3.5;          // od zwlok (scena trwa ~10 s, krok w bok nie przerywa)
    static boolean pomiar;

    // ================================================================ rewiry
    record Rewir(String swiat, int x, int y, int z, String mob, boolean trwaly) {
        boolean swiatPasuje(String w) { return swiat.endsWith("*") ? w.startsWith(swiat.substring(0, swiat.length() - 1)) : w.equals(swiat); }
        String klucz() { return swiat + ":" + x + ":" + y + ":" + z; }
    }

    enum Faza { TROP, PODCHOD, UCIECZKA }

    final class Low {
        final Player p; final Rewir r; final String mob; final int tier;
        final List<Location> tropy = new ArrayList<>();
        Location cel;
        int nr, t, wiatrDeg, sploszonyT, pojawilo;
        Location ostatnia;   // pozycja gracza 2 ticki temu (predkosc = halas krokow)
        Faza faza = Faza.TROP;
        UUID zwierze;
        BukkitTask zad;
        boolean konczy, czysty;
        String powod;
        Low(Player p, Rewir r, String mob, int tier) { this.p = p; this.r = r; this.mob = mob; this.tier = tier; }
    }

    // ================================================================ ciecia
    record Ciecie(String nazwa, double f, double s, double h, char produkt, String karaKolejnosc, String karaPoslizg, double kara) {}

    static final List<Ciecie> DUZA = List.of(   // gruba zwierzyna: kolejnosc wg Montana State Univ. "Field Care of Harvested Big Game"
            new Ciecie("Okolica odbytu", -0.5, 0, 0.12, 'M', "jelita rozciete — treść na mięsie", "nóż ześlizgnął się na jelito", 0.35),
            new Ciecie("Brzuch do mostka", -0.17, 0, 0.18, 'M', "brzuch otwarty przed odbytem — treść na mięsie", "za głębokie cięcie brzucha", 0.3),
            new Ciecie("Przełyk i tchawica", 0.5, 0, 0.15, 'M', "trzewia ciągnięte bez odcięcia przełyku", "przełyk rozdarty", 0.2),
            new Ciecie("Przepona", 0.18, -0.14, 0.16, 'M', "przepona przed przełykiem — żołądek pękł", "żołądek przebity", 0.35),
            new Ciecie("Zdjęcie skóry", 0.0, 0.42, 0.1, 'S', "skóra zdjęta przed patroszeniem — zabrudzona", "skóra rozdarta", 0.3));
    static final List<Ciecie> MALA = List.of(   // zajac: regula gry (nie sprawdzona w zrodle)
            new Ciecie("Nacięcie przy łapach", -0.35, 0, 0.1, 'S', "skóra rozdarta — bez nacięcia przy łapach", "łapa przecięta krzywo", 0.3),
            new Ciecie("Ściągnięcie skóry", 0.0, 0.35, 0.1, 'S', "skóra ściągana bez nacięcia", "skóra rozdarta", 0.3),
            new Ciecie("Patroszenie", 0.3, -0.1, 0.12, 'M', "patroszenie w skórze — mięso brudne", "jelito przecięte", 0.35));
    static final List<Ciecie> PAJAK = List.of(  // pajak: regula gry (nie sprawdzona w zrodle)
            new Ciecie("Odcięcie odnóży", 0.0, 0.4, 0.1, 'S', "odnóża łamane — pancerz popękany", "pancerz porysowany", 0.25),
            new Ciecie("Rozcięcie pancerza", -0.3, 0, 0.15, 'S', "pancerz cięty z odnóżami — popękany", "pancerz pęknięty", 0.3),
            new Ciecie("Wyjęcie gruczołu jadu", 0.35, -0.1, 0.12, 'J', "gruczoł zgnieciony pancerzem", "gruczoł przebity", 0.5));

    static List<Ciecie> plan(EntityType t) {
        return switch (t) {
            case RABBIT -> MALA;
            case SPIDER, CAVE_SPIDER -> PAJAK;
            default -> DUZA;
        };
    }
    static boolean maMieso(EntityType t) { return t == EntityType.RABBIT || t == EntityType.PIG || t == EntityType.SHEEP || t == EntityType.POLAR_BEAR; }

    final class Scena {
        final Player p; final Oprawianie.Zwloki zw; final Przedmioty.Znalezione noz; final Location start;
        int aktywne = -1, prowadz, t;
        boolean poslizg, konczy;
        BukkitTask zad;
        Scena(Player p, Oprawianie.Zwloki zw, Przedmioty.Znalezione noz) { this.p = p; this.zw = zw; this.noz = noz; this.start = p.getLocation(); }
    }

    // ================================================================ wedzenie
    static final class Kawalek {
        String swiat; int x, y, z; UUID wlasciciel; boolean wyborne; long start; int probki, zgasle;
        ItemDisplay widok;
    }

    private final Rzemioslo plugin;
    private final List<Rewir> rewiry = new ArrayList<>();
    private final Map<UUID, Low> lowy = new HashMap<>();
    private final Map<UUID, Scena> sceny = new HashMap<>();
    private final Map<UUID, UUID> lowcaZwierza = new HashMap<>();   // zwierze z rewiru -> mysliwy (zabojca dla Oprawiania)
    private final Map<String, Long> odpoczynek = new HashMap<>();   // rewir -> kiedy znow wolny
    private final List<Kawalek> wedzarnie = new ArrayList<>();
    private final File plik;

    Mysliwy(Rzemioslo plugin) {
        this.plugin = plugin;
        this.plik = new File(plugin.getDataFolder(), "mysliwy.yml");
        wczytaj();
        Bukkit.getScheduler().runTaskTimer(plugin, this::probkujWedzarnie, 100L, 100L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::znakiRewirow, 40L, 40L);
    }

    private void wczytaj() {
        if (!plik.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(plik);
        for (String s : y.getStringList("rewiry")) {
            String[] a = s.split(":");
            if (a.length == 5) rewiry.add(new Rewir(a[0], Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]), a[4], true));
        }
        for (String s : y.getStringList("wedzarnie")) {
            String[] a = s.split(":");
            if (a.length != 9) continue;
            Kawalek k = new Kawalek();
            k.swiat = a[0]; k.x = Integer.parseInt(a[1]); k.y = Integer.parseInt(a[2]); k.z = Integer.parseInt(a[3]);
            k.wlasciciel = UUID.fromString(a[4]); k.wyborne = a[5].equals("1"); k.start = Long.parseLong(a[6]);
            k.probki = Integer.parseInt(a[7]); k.zgasle = Integer.parseInt(a[8]);
            wedzarnie.add(k);
        }
        Bukkit.getScheduler().runTask(plugin, () -> { for (Kawalek k : wedzarnie) pokazKawalek(k); });
    }

    private void zapisz() {
        YamlConfiguration y = new YamlConfiguration();
        List<String> l = new ArrayList<>();
        for (Rewir r : rewiry) if (r.trwaly) l.add(r.klucz() + ":" + r.mob);
        y.set("rewiry", l);
        List<String> w = new ArrayList<>();
        for (Kawalek k : wedzarnie) w.add(k.swiat + ":" + k.x + ":" + k.y + ":" + k.z + ":" + k.wlasciciel + ":" + (k.wyborne ? 1 : 0) + ":" + k.start + ":" + k.probki + ":" + k.zgasle);
        y.set("wedzarnie", w);
        try { y.save(plik); } catch (Exception ex) { plugin.getLogger().warning("Mysliwy: zapis mysliwy.yml: " + ex.getMessage()); }
    }

    String rewir(String swiat, int x, int y, int z, String mob, boolean trwaly) {
        if (!plugin.dane().zwierzyna.containsKey(mob)) return "mob " + mob + " nie jest na liscie zwierzyna";
        if (MythicBukkit.inst().getMobManager().getMythicMob(mob).isEmpty()) return "brak moba MythicMobs " + mob;
        rewiry.removeIf(r -> r.klucz().equals(swiat + ":" + x + ":" + y + ":" + z));
        rewiry.add(new Rewir(swiat, x, y, z, mob, trwaly));
        if (trwaly) zapisz();
        return "rewir " + swiat + " " + x + " " + y + " " + z + " -> " + mob + (trwaly ? " (trwaly)" : "");
    }

    String rewirUsun(String swiat, int x, int y, int z) {
        boolean b = rewiry.removeIf(r -> r.klucz().equals(swiat + ":" + x + ":" + y + ":" + z));
        if (b) zapisz();
        return b ? "rewir usuniety" : "brak rewiru";
    }

    private Rewir rewirObok(Location l) {
        for (Rewir r : rewiry) {
            if (!r.swiatPasuje(l.getWorld().getName())) continue;
            double dx = l.getX() - (r.x + 0.5), dz = l.getZ() - (r.z + 0.5), dy = l.getY() - r.y;
            if (dx * dx + dz * dz <= 9 && Math.abs(dy) <= 3) return r;
        }
        return null;
    }

    /** Znak rewiru: stare tropy przy wejsciu (widac dla wszystkich, tylko czasteczki). */
    private void znakiRewirow() {
        for (Rewir r : rewiry) for (World w : Bukkit.getWorlds()) {
            if (!r.swiatPasuje(w.getName()) || !w.isChunkLoaded(r.x >> 4, r.z >> 4)) continue;
            Location l = new Location(w, r.x + 0.5, r.y + 0.05, r.z + 0.5);
            if (l.getNearbyPlayers(24).isEmpty()) continue;
            odcisk(l, 0, 0.7f);
        }
    }

    // ================================================================ tropienie
    @EventHandler(ignoreCancelled = true)
    public void naKucniecie(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        Player p = e.getPlayer();
        if (lowy.containsKey(p.getUniqueId()) || sceny.containsKey(p.getUniqueId())) return;
        Rewir r = rewirObok(p.getLocation());
        if (r == null) return;
        Dane.Zrodlo z = plugin.dane().zwierzyna.get(r.mob);
        if (z == null) return;
        String klucz = p.getWorld().getName() + ":" + r.klucz();
        for (Low o : lowy.values()) if (o.r == r && o.p.getWorld() == p.getWorld()) { plugin.pasek(p, "§7Ktoś już idzie tym tropem."); return; }
        Long wolny = odpoczynek.get(klucz);
        if (wolny != null && wolny > System.currentTimeMillis()) {
            plugin.pasek(p, "§7Tropy tu stare — zwierzyna wróci za " + ((wolny - System.currentTimeMillis()) / 1000 + 1) + " s.");
            return;
        }
        if (!sprawdzGotowosc(p, z.tier(), "Rewir: " + nazwaMoba(r.mob))) return;
        Low s = new Low(p, r, r.mob, z.tier());
        Location baza = new Location(p.getWorld(), r.x + 0.5, r.y, r.z + 0.5);
        if (!wytyczTrop(s, baza)) { plugin.pasek(p, "§7Teren tu nie trzyma tropów."); return; }
        s.wiatrDeg = ThreadLocalRandom.current().nextInt(8) * 45;
        lowy.put(p.getUniqueId(), s);
        odpoczynek.put(klucz, System.currentTimeMillis() + 20_000);
        s.zad = Bukkit.getScheduler().runTaskTimer(plugin, () -> tikLowu(s), 1L, 1L);
        plugin.pasek(p, "§2Trop " + opisTropu(s) + " §7— idź po śladach (" + s.tropy.size() + ").");
        if (pomiar) plugin.getLogger().info("[mysliwy] start " + p.getName() + " mob=" + s.mob + " tropow=" + s.tropy.size() + " wiatr=" + s.wiatrDeg);
    }

    /** Poziom + noz T >= tier; komunikaty jak w silniku wezlow. */
    private boolean sprawdzGotowosc(Player p, int tier, String nazwa) {
        Dane d = plugin.dane();
        int start = d.start(tier);
        if (Profesje.poziom(p, PROF) < start) {
            plugin.pasek(p, plugin.kom("za-niski-poziom", "{profesja}", d.nazwyProfesji.getOrDefault(PROF, PROF), "{poziom}", String.valueOf(start), "{nazwa}", nazwa));
            return false;
        }
        Przedmioty.Znalezione n = plugin.przedmioty().najlepsze(p, "noz", tier);
        if (n == null) {
            int mam = plugin.przedmioty().najwyzszyTier(p, "noz");
            plugin.pasek(p, mam == 0 ? plugin.kom("brak-narzedzia", "{narzedzie}", Przedmioty.nazwaRodzaju("noz"), "{tier}", d.rzymska(tier))
                    : plugin.kom("slabe-narzedzie", "{narzedzie}", Przedmioty.nazwaRodzaju("noz"), "{tier}", d.rzymska(tier), "{mam}", d.rzymska(mam)));
            return false;
        }
        if (Przedmioty.zepsute(n.item())) { plugin.pasek(p, plugin.kom("zepsute", "{nazwa}", d.nazwa(n.narz().id()))); return false; }
        return true;
    }

    private boolean wytyczTrop(Low s, Location baza) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double kat = r.nextDouble(Math.PI * 2);
        Location ost = baza;
        for (int i = 0; i < 4; i++) {
            kat += Math.toRadians(r.nextDouble(-28, 28));
            Location n = ziemia(ost.clone().add(Math.cos(kat) * 5.5, 0, Math.sin(kat) * 5.5));
            if (n == null) break;
            s.tropy.add(n);
            ost = n;
        }
        if (s.tropy.size() < 2) return false;
        Location c = ziemia(ost.clone().add(Math.cos(kat) * ODLEGLOSC_ZWIERZA, 0, Math.sin(kat) * ODLEGLOSC_ZWIERZA));
        s.cel = c != null ? c : ziemia(ost.clone().add(Math.cos(kat) * 9, 0, Math.sin(kat) * 9));
        return s.cel != null;
    }

    /** Stopy na podlodze w pionie +-4 (dziala w lochach z sufitem). */
    private static Location ziemia(Location l) {
        World w = l.getWorld();
        int bx = l.getBlockX(), bz = l.getBlockZ(), by = l.getBlockY();
        if (!w.isChunkLoaded(bx >> 4, bz >> 4)) return null;
        for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4}) {
            int y = by + dy;
            Block pod = w.getBlockAt(bx, y - 1, bz), b = w.getBlockAt(bx, y, bz), nad = w.getBlockAt(bx, y + 1, bz);
            if (pod.getType().isSolid() && b.isPassable() && !b.isLiquid() && nad.isPassable() && !nad.isLiquid())
                return new Location(w, bx + 0.5, y, bz + 0.5);
        }
        return null;
    }

    private void tikLowu(Low s) {
        if (s.konczy) return;
        Player p = s.p;
        s.t++;
        if (!p.isOnline() || p.isDead()) { koniecLowu(s, null); return; }
        if (s.t > CZAS_LOWOW) { koniecLowu(s, "§7Trop wystygł."); return; }
        Location pl = p.getLocation();
        if (s.faza == Faza.TROP) {
            Location tr = s.tropy.get(s.nr);
            if (pl.getWorld() != tr.getWorld() || pl.distanceSquared(tr) > 40 * 40) { koniecLowu(s, "§7Zgubiłeś trop."); return; }
            if (s.t % 8 == 0) {
                odcisk(tr, katTropu(s, s.nr), 1.0f);
                if (s.nr + 1 < s.tropy.size() && s.t % 16 == 0) odcisk(s.tropy.get(s.nr + 1), katTropu(s, s.nr + 1), 0.55f);
            }
            double dx = pl.getX() - tr.getX(), dz = pl.getZ() - tr.getZ();
            if (dx * dx + dz * dz <= 2.2 * 2.2 && Math.abs(pl.getY() - tr.getY()) < 2.5) {
                s.nr++;
                p.playSound(tr, Sound.BLOCK_GRASS_STEP, SoundCategory.PLAYERS, 0.6f, 0.8f);
                if (s.nr >= s.tropy.size()) { pokazZwierze(s); return; }
                plugin.pasek(p, "§2Trop " + s.nr + "/" + s.tropy.size() + " §7— " + opisTropu(s) + (wprawa(p) < 5 ? " §8(stań przy następnym śladzie)" : ""));
            }
            if (s.t % 10 == 0) wiatrCzasteczki(s);
            return;
        }
        Entity zw = s.zwierze == null ? null : Bukkit.getEntity(s.zwierze);
        if (zw == null || !zw.isValid()) { koniecLowu(s, s.faza == Faza.UCIECZKA ? s.powod : null); return; }
        if (s.faza == Faza.UCIECZKA) {
            if (s.t - s.sploszonyT > 30) {
                zw.getWorld().spawnParticle(Particle.POOF, zw.getLocation().add(0, 0.4, 0), 8, 0.3, 0.2, 0.3, 0.02);
                zw.remove();
                koniecLowu(s, s.powod);
            }
            return;
        }
        // PODCHOD
        if (s.t % 240 == 0 && ThreadLocalRandom.current().nextDouble() < 0.4) {
            s.wiatrDeg = (s.wiatrDeg + (ThreadLocalRandom.current().nextBoolean() ? 45 : -45) + 360) % 360;
            plugin.pasek(p, "§eWiatr skręca — " + nazwaWiatru(s.wiatrDeg) + ".");
        }
        if (s.t % 10 == 0) wiatrCzasteczki(s);
        if (s.t % 2 != 0 || s.t - s.pojawilo < OSWOJENIE) return;
        Location zl = zw.getLocation();
        if (zl.getWorld() != pl.getWorld()) { koniecLowu(s, "§7Zgubiłeś zwierzynę."); return; }
        double d = zl.distance(pl);
        if (d > 45) { koniecLowu(s, "§7Zwierzyna została daleko w tyle."); return; }
        Vector doZw = zl.toVector().subtract(pl.toVector()).setY(0);
        double dot = doZw.lengthSquared() < 1e-6 ? 0 : doZw.normalize().dot(wiatrNa(s.wiatrDeg));
        if (d < WECH && dot > 0.6) { sploszenie(s, zw, "§cZwierzę cię zwietrzyło — wiatr niósł twój zapach."); return; }
        // halas: sprint albo bieg (> 0,25 bl./tick; chod ~0,22, skradanie ~0,07) — liczony z ruchu, bo flaga sprintu klienta bywa spozniona
        double v = s.ostatnia == null || s.ostatnia.getWorld() != pl.getWorld() ? 0 : Math.hypot(pl.getX() - s.ostatnia.getX(), pl.getZ() - s.ostatnia.getZ()) / 2;
        s.ostatnia = pl.clone();
        if (d < SLUCH && (p.isSprinting() || (v > 0.25 && v < 2))) { sploszenie(s, zw, "§cUsłyszało twoje kroki."); return; }
        if (d < WZROK && !p.isSneaking()) { sploszenie(s, zw, "§cZobaczyło cię — trzeba było kucać."); return; }
        if (s.t % 20 == 0) {
            String hint = wprawa(p) < 5 ? " §8· wiatr od zwierza na ciebie, kucaj" : "";
            plugin.pasek(p, "§2" + nazwaMoba(s.mob) + " §7" + String.format(Locale.ROOT, "%.0f", d) + " m · " + nazwaWiatru(s.wiatrDeg) + hint);
        }
    }

    private void pokazZwierze(Low s) {
        var mm = MythicBukkit.inst().getMobManager().getMythicMob(s.mob).orElse(null);
        if (mm == null) { koniecLowu(s, "§7Trop się urywa."); plugin.getLogger().warning("Mysliwy: brak moba " + s.mob); return; }
        Location c = s.cel.clone();
        c.setYaw(ThreadLocalRandom.current().nextFloat() * 360f);
        var am = mm.spawn(BukkitAdapter.adapt(c), 1);
        Entity en = am == null ? null : am.getEntity().getBukkitEntity();
        if (en == null) { koniecLowu(s, "§7Trop się urywa."); return; }
        en.setPersistent(false);
        if (en instanceof Mob m) m.setAware(false);   // pasie sie, nie ucieka i nie atakuje, dopoki nie sploszy
        s.zwierze = en.getUniqueId();
        lowcaZwierza.put(en.getUniqueId(), s.p.getUniqueId());
        s.faza = Faza.PODCHOD;
        s.pojawilo = s.t;
        plugin.pasek(s.p, "§2" + nazwaMoba(s.mob) + " przed tobą §7— " + nazwaWiatru(s.wiatrDeg) + ". Podejdź i uderz z ukrycia.");
        if (pomiar) plugin.getLogger().info("[mysliwy] zwierze " + s.p.getName() + " " + s.mob + " @" + fmt(c));
    }

    private void sploszenie(Low s, Entity zw, String powod) {
        if (s.faza == Faza.UCIECZKA) return;
        s.faza = Faza.UCIECZKA;
        s.sploszonyT = s.t;
        s.powod = powod;
        if (zw instanceof Mob m) m.setAware(true);
        Vector od = zw.getLocation().toVector().subtract(s.p.getLocation().toVector()).setY(0);
        if (od.lengthSquared() < 1e-4) od = new Vector(1, 0, 0);
        zw.setVelocity(od.normalize().multiply(0.9).setY(0.42));
        zw.getWorld().playSound(zw.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.NEUTRAL, 1f, 1.2f);
        plugin.pasek(s.p, powod);
        if (pomiar) plugin.getLogger().info("[mysliwy] sploszenie " + s.p.getName() + " " + powod.replaceAll("§.", ""));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void naCios(EntityDamageByEntityEvent e) {
        Low s = lowZwierzecia(e.getEntity().getUniqueId());
        if (s == null) return;
        Player p = e.getDamager() instanceof Player x ? x : e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player x2 ? x2 : null;
        if (s.faza == Faza.PODCHOD && p == s.p && p.isSneaking() && e.getEntity() instanceof LivingEntity le) {
            // czysty cios/strzal z podchodu (zwierze niczego nie czuje, mysliwy kuca) = jedno trafienie; reszta: ranne ucieka
            e.setDamage(Math.max(e.getDamage(), le.getHealth() + 1));
            s.czysty = true;
            plugin.pasek(p, "§2Czysty cios z podchodu.");
            if (pomiar) plugin.getLogger().info("[mysliwy] cios-z-podchodu " + p.getName() + " dmg=" + e.getDamage());
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            Entity zw = Bukkit.getEntity(e.getEntity().getUniqueId());
            if (zw == null || !zw.isValid() || zw.isDead()) return;
            // MythicMobs/MythicLib potrafia przeliczyc obrazenia po zdarzeniu (pancerz, modyfikatory) — czysty cios konczy i tak
            if (s.czysty && zw instanceof LivingEntity le) { le.setHealth(0); return; }
            if (s.faza != Faza.UCIECZKA) sploszenie(s, zw, "§cRanne zwierzę uciekło.");
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void naSmiercZwierzecia(EntityDeathEvent e) {
        Low s = lowZwierzecia(e.getEntity().getUniqueId());
        if (s == null) return;
        UUID u = s.zwierze;
        s.zwierze = null;
        Bukkit.getScheduler().runTaskLater(plugin, () -> lowcaZwierza.remove(u), 40L);
        koniecLowu(s, "§2Upolowane. §7Oprawiaj zwłoki nożem (PPM).");
    }

    /** Mysliwy, ktory wytropil to zwierze (MythicMobDeathEvent nie zawsze podaje zabojce). */
    Player lowca(UUID zwierze) {
        UUID u = lowcaZwierza.remove(zwierze);
        return u == null ? null : Bukkit.getPlayer(u);
    }

    private Low lowZwierzecia(UUID u) {
        for (Low s : lowy.values()) if (u.equals(s.zwierze)) return s;
        return null;
    }

    private void koniecLowu(Low s, String kom) {
        if (s.konczy) return;
        s.konczy = true;
        if (s.zad != null) s.zad.cancel();
        lowy.remove(s.p.getUniqueId());
        if (s.zwierze != null) {
            Entity zw = Bukkit.getEntity(s.zwierze);
            if (zw != null && zw.isValid() && !zw.isDead()) { zw.remove(); lowcaZwierza.remove(s.zwierze); }
            else Bukkit.getScheduler().runTaskLater(plugin, () -> lowcaZwierza.remove(s.zwierze), 40L);
        }
        if (kom != null && s.p.isOnline()) plugin.pasek(s.p, kom);
        if (pomiar) plugin.getLogger().info("[mysliwy] koniec-lowu " + s.p.getName() + " faza=" + s.faza + " trop=" + s.nr + "/" + s.tropy.size()
                + " t=" + s.t + " " + (kom == null ? "" : kom.replaceAll("§.", "")));
    }

    private String opisTropu(Low s) {
        EntityType t = typMoba(s.mob);
        String slad = t == EntityType.RABBIT ? "odciski skoków parami" : t == EntityType.PIG ? "racice, zryta ziemia"
                : t == EntityType.SHEEP ? "racice i kłaczki runa" : t == EntityType.WOLF ? "łapy z pazurami" : t == EntityType.POLAR_BEAR ? "szerokie łapy"
                : t == EntityType.SPIDER || t == EntityType.CAVE_SPIDER ? "nitki pajęczyny" : "ślady";
        return slad;
    }

    private EntityType typMoba(String mob) {
        try {
            var mm = MythicBukkit.inst().getMobManager().getMythicMob(mob).orElse(null);
            if (mm != null) return EntityType.valueOf(String.valueOf(mm.getEntityType()).toUpperCase(Locale.ROOT));
        } catch (Throwable ignored) { }
        return EntityType.PIG;
    }

    private String nazwaMoba(String mob) {
        try {
            var mm = MythicBukkit.inst().getMobManager().getMythicMob(mob).orElse(null);
            if (mm != null && mm.getDisplayName() != null) return mm.getDisplayName().get().replaceAll("&.", "");
        } catch (Throwable ignored) { }
        return "Zwierzyna";
    }

    private float katTropu(Low s, int i) {
        Location a = i == 0 ? new Location(s.p.getWorld(), s.r.x + 0.5, s.r.y, s.r.z + 0.5) : s.tropy.get(i - 1), b = s.tropy.get(i);
        return (float) Math.atan2(b.getZ() - a.getZ(), b.getX() - a.getX());
    }

    /** Odcisk: dwie pary plamek w kierunku marszu (widac dla wszystkich). */
    private static void odcisk(Location l, float kat, float sila) {
        World w = l.getWorld();
        Particle.DustOptions brz = new Particle.DustOptions(Color.fromRGB(78, 56, 34), 0.9f * sila + 0.2f);
        double cx = Math.cos(kat), cz = Math.sin(kat);
        for (int k = -1; k <= 1; k += 2) {
            double ox = -cz * 0.18 * k, oz = cx * 0.18 * k;
            w.spawnParticle(Particle.DUST, l.getX() + ox + cx * 0.2 * k, l.getY() + 0.06, l.getZ() + oz + cz * 0.2 * k, 2, 0.03, 0, 0.03, 0, brz);
        }
    }

    // ================================================================ wiatr
    /** Kierunek, w ktory wieje wiatr (0 = na wschod +X, 90 = na poludnie +Z). */
    private static Vector wiatrNa(int deg) { double r = Math.toRadians(deg); return new Vector(Math.cos(r), 0, Math.sin(r)); }

    static String nazwaWiatru(int deg) {
        String[] z = {"zachodu", "północnego zachodu", "północy", "północnego wschodu", "wschodu", "południowego wschodu", "południa", "południowego zachodu"};
        return "wiatr z " + z[((deg % 360) + 360) % 360 / 45];   // wieje na wschod (0) = z zachodu
    }

    private void wiatrCzasteczki(Low s) {
        Location l = s.p.getLocation().add(0, 1.6, 0);
        Vector v = wiatrNa(s.wiatrDeg);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 2; i++)
            l.getWorld().spawnParticle(Particle.WHITE_ASH, l.getX() + r.nextDouble(-1, 1) - v.getX() * 1.5, l.getY() + r.nextDouble(-0.3, 0.5),
                    l.getZ() + r.nextDouble(-1, 1) - v.getZ() * 1.5, 0, v.getX(), 0, v.getZ(), 0.4);
    }

    // ================================================================ oprawianie: scena ciec
    boolean oprawia(Oprawianie.Zwloki zw) {
        for (Scena s : sceny.values()) if (s.zw == zw) return true;
        return false;
    }

    /** PPM w zwloki: pierwszy = start sceny, kolejne = wybor ciecia tam, gdzie patrzysz. */
    void klikZwloki(Player p, Oprawianie.Zwloki zw) {
        Scena s = sceny.get(p.getUniqueId());
        if (s != null && s.zw != zw) return;
        if (s == null) {
            for (Scena o : sceny.values()) if (o.zw == zw) { plugin.pasek(p, "§7Ktoś już oprawia te zwłoki."); return; }
            if (!sprawdzGotowosc(p, zw.z.tier(), "Zwłoki: " + plugin.dane().nazwa(zw.z.glowny()))) return;
            s = new Scena(p, zw, plugin.przedmioty().najlepsze(p, "noz", zw.z.tier()));
            sceny.put(p.getUniqueId(), s);
            Scena fs = s;
            s.zad = Bukkit.getScheduler().runTaskTimer(plugin, () -> tikSceny(fs), 1L, 1L);
            List<Ciecie> pl = plan(zw.typ);
            plugin.pasek(p, "§2Oprawianie §7— " + pl.size() + " cięć. Celuj w znak i PPM." + (wprawa(p) < 5 ? " §8Najpierw: " + pl.get(nastepne(zw)).nazwa() : ""));
            if (zw.spalona) p.sendMessage("§2[Myśliwy] §7Sierść spalona ogniem — skóry z tego nie będzie.");
        }
        if (s.aktywne >= 0) return;   // trwa ciecie
        int i = wycelowane(p, zw);
        if (pomiar) {
            Location o = p.getEyeLocation(); StringBuilder b = new StringBuilder();
            for (Ciecie c : plan(zw.typ)) b.append(String.format(Locale.ROOT, "%.2f ", chybienie(o, o.getDirection(), punkt(zw, c))));
            plugin.getLogger().info(String.format(Locale.ROOT, "[mysliwy] cel %s oko=%.2f,%.2f,%.2f yaw=%.1f pitch=%.1f wybrane=%d chyb=%s", p.getName(), o.getX(), o.getY(), o.getZ(), o.getYaw(), o.getPitch(), i, b));
        }
        if (i < 0) { plugin.pasek(p, "§7Celuj w czerwony znak na zwłokach."); return; }
        List<Ciecie> pl = plan(zw.typ);
        int nast = nastepne(zw);
        if (i != nast) {   // zla kolejnosc: ciecie sie robi, ale psuje produkt
            Ciecie c = pl.get(i);
            kara(zw, c.produkt(), c.kara(), c.karaKolejnosc());
            if (pomiar) plugin.getLogger().info("[mysliwy] zla-kolejnosc " + p.getName() + " ciecie=" + i + " oczekiwane=" + nast);
        }
        s.aktywne = i;
        s.prowadz = 0;
        s.poslizg = false;
        p.getWorld().playSound(punkt(zw, pl.get(i)), Sound.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 0.7f, 0.8f);
    }

    private void tikSceny(Scena s) {
        if (s.konczy) return;
        s.t++;
        Player p = s.p;
        Oprawianie.Zwloki zw = s.zw;
        if (!p.isOnline() || p.isDead() || zw.zuzyte) { koniecSceny(s, null, false); return; }
        if (p.getWorld() != zw.l.getWorld() || p.getLocation().distanceSquared(zw.l) > ZASIEG_SCENY * ZASIEG_SCENY) {
            if (s.aktywne >= 0) kara(zw, plan(zw.typ).get(s.aktywne).produkt(), 0.2, "cięcie przerwane w pół");
            koniecSceny(s, plugin.kom("przerwane-ruch"), false);
            return;
        }
        List<Ciecie> pl = plan(zw.typ);
        if (s.t % 6 == 0) {
            int nast = nastepne(zw);
            for (int i = 0; i < pl.size(); i++) {
                if (zw.ciete[i]) continue;
                Location pt = punkt(zw, pl.get(i));
                pt.getWorld().spawnParticle(Particle.DUST, pt, 2, 0.02, 0.02, 0.02, 0, new Particle.DustOptions(Color.fromRGB(150, 20, 20), 0.6f));
                if (i == nast && wprawa(p) < 5) p.spawnParticle(Particle.DUST, pt.clone().add(0, 0.08, 0), 1, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 0.5f));
            }
        }
        if (s.aktywne < 0) {
            if (s.t > 20 * 40) koniecSceny(s, "§7Odłożyłeś nóż.", false);
            return;
        }
        Ciecie c = pl.get(s.aktywne);
        Location pt = punkt(zw, c);
        double ch = chybienie(p.getEyeLocation(), p.getEyeLocation().getDirection(), pt);
        if (ch > POSLIZG && !s.poslizg) {
            s.poslizg = true;
            kara(zw, c.produkt(), c.kara() * 0.7, c.karaPoslizg());
            p.playSound(pt, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.3f, 1.6f);
        }
        pt.getWorld().spawnParticle(Particle.BLOCK, pt, 2, 0.05, 0.02, 0.05, 0, Material.REDSTONE_BLOCK.createBlockData());
        if (++s.prowadz >= PROWADZENIE) {
            zw.ciete[s.aktywne] = true;
            s.aktywne = -1;
            int nast = nastepne(zw);
            if (nast < 0) { nagroda(s); return; }
            plugin.pasek(p, "§2" + c.nazwa() + (s.poslizg ? " §c— " + c.karaPoslizg() : " §a✔") + (wprawa(p) < 5 ? " §8Dalej: " + pl.get(nast).nazwa() : ""));
        } else if (s.t % 4 == 0) {
            plugin.pasek(p, "§2" + c.nazwa() + " §7" + "▮".repeat(s.prowadz * 10 / PROWADZENIE) + "§8" + "▮".repeat(10 - s.prowadz * 10 / PROWADZENIE) + " §8trzymaj nóż na znaku");
        }
    }

    private static int nastepne(Oprawianie.Zwloki zw) {
        for (int i = 0; i < zw.ciete.length; i++) if (!zw.ciete[i]) return i;
        return -1;
    }

    private int wycelowane(Player p, Oprawianie.Zwloki zw) {
        List<Ciecie> pl = plan(zw.typ);
        Location oko = p.getEyeLocation();
        Vector dir = oko.getDirection();
        int best = -1; double bk = CEL;
        for (int i = 0; i < pl.size(); i++) {
            if (zw.ciete[i]) continue;
            double k = chybienie(oko, dir, punkt(zw, pl.get(i)));
            if (k < bk) { bk = k; best = i; }
        }
        return best;
    }

    /** Odleglosc (w poziomie punktu) miedzy miejscem, gdzie patrzy gracz, a znakiem ciecia. */
    static double chybienie(Location oko, Vector dir, Location pt) {
        if (dir.getY() > -0.05) return 99;   // nie patrzy w dol na tusze
        double t = (pt.getY() - oko.getY()) / dir.getY();
        if (t < 0 || t > 6) return 99;
        double x = oko.getX() + dir.getX() * t, z = oko.getZ() + dir.getZ() * t;
        return Math.hypot(x - pt.getX(), z - pt.getZ());
    }

    /** Punkt ciecia w swiecie: f = wzdluz tuszy (przod = glowa), s = w bok, h = wysokosc. */
    static Location punkt(Oprawianie.Zwloki zw, Ciecie c) {
        double r = Math.toRadians(zw.yaw);
        double fx = -Math.sin(r), fz = Math.cos(r);   // kierunek patrzenia moba w chwili smierci
        return zw.l.clone().add(fx * c.f() - fz * c.s(), c.h(), fz * c.f() + fx * c.s());
    }

    private static void kara(Oprawianie.Zwloki zw, char produkt, double ile, String uwaga) {
        if (produkt == 'S') zw.kS += ile; else if (produkt == 'M') zw.kM += ile; else zw.kJ += ile;
        if (!zw.uwagi.contains(uwaga)) zw.uwagi.add(uwaga);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void naObrazenia(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Scena s = sceny.get(p.getUniqueId());
        if (s == null) return;
        if (s.aktywne >= 0) kara(s.zw, plan(s.zw.typ).get(s.aktywne).produkt(), 0.25, "drgnęła ręka od ciosu");
        koniecSceny(s, plugin.kom("przerwane-obrazenia"), false);
    }

    private void koniecSceny(Scena s, String kom, boolean ok) {
        if (s.konczy) return;
        s.konczy = true;
        if (s.zad != null) s.zad.cancel();
        sceny.remove(s.p.getUniqueId());
        if (s.aktywne >= 0 && !ok) s.zw.ciete[s.aktywne] = false;   // przerwane ciecie trzeba powtorzyc
        if (kom != null && s.p.isOnline()) plugin.pasek(s.p, kom);
        if (pomiar) plugin.getLogger().info("[mysliwy] koniec-sceny " + s.p.getName() + " ok=" + ok + " ciete=" + java.util.Arrays.toString(s.zw.ciete)
                + " kS=" + fmt2(s.zw.kS) + " kM=" + fmt2(s.zw.kM) + " " + (kom == null ? "" : kom.replaceAll("§.", "")));
    }

    private static int klasa(double q) { return q >= 0.85 ? 2 : q >= 0.5 ? 1 : q >= 0.25 ? 0 : -1; }

    private void nagroda(Scena s) {
        Player p = s.p;
        Oprawianie.Zwloki zw = s.zw;
        Dane.Zrodlo z = zw.z;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        boolean podw = plugin.profesje().podwojny(p, PROF);
        double qS = zw.spalona ? 0 : Math.max(0, 1 - zw.kS), qM = Math.max(0, 1 - zw.kM), qJ = Math.max(0, 1 - zw.kJ);
        List<String> wyn = new ArrayList<>();
        List<Double> qs = new ArrayList<>();
        // skora / runo / pancerz = surowiec glowny zwierzyny
        int kS = klasa(qS);
        qs.add(qS);
        if (kS >= 0) wyn.add(wydaj(p, z.glowny(), kS, podw ? 2 : 1));
        else wyn.add("§c" + (zw.spalona ? "skóra spalona" : "skóra do niczego") + "§7");
        if (maMieso(zw.typ)) {
            int kM = klasa(qM);
            qs.add(qM);
            int ile = (zw.typ == EntityType.RABBIT ? 1 : 2) * (podw ? 2 : 1);
            if (kM >= 0) wyn.add(wydaj(p, MIESO, kM, ile)); else wyn.add("§cmięso zepsute§7");
        }
        if (plan(zw.typ) == PAJAK && "PROF_VENOM_SAC".equals(z.rzadki())) {   // gruczol jadu = rzadki pajaka, szansa x4 przy czystym wyjeciu
            if (qJ >= 0.5 && r.nextDouble() < Math.min(1, z.szansaRzadki() * 4)) wyn.add(wydaj(p, z.rzadki(), qJ >= 0.85 ? 2 : 1, 1));
        } else if (z.rzadki() != null && r.nextDouble() < z.szansaRzadki()) wyn.add(wydaj(p, z.rzadki(), 1, 1));
        if (z.poboczny() != null && r.nextDouble() < z.szansaPoboczny()) wyn.add(wydaj(p, z.poboczny(), 1, 1));
        double sr = qs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double xp = plugin.dane().g(zw.z.tier()) * (0.4 + 0.6 * sr);
        double dx = plugin.profesje().daj(p, PROF, zw.z.tier(), xp, zw.l);
        if (s.noz != null) Przedmioty.zuzyj(p, s.noz.slot());
        plugin.zlecenia().ustawFlage(p, "mysliwy_wprawa", String.valueOf(Math.min(99, wprawa(p) + 1)));
        String linia = "§2[Myśliwy] §f" + String.join("§7, §f", wyn) + (zw.uwagi.isEmpty() ? "" : " §8— " + String.join(", ", zw.uwagi)) + " §7+" + Math.round(dx) + " XP";
        p.sendMessage(linia);
        if (pomiar) plugin.getLogger().info("[mysliwy] wynik " + p.getName() + " typ=" + zw.typ + " qS=" + fmt2(qS) + " qM=" + fmt2(qM) + " spalona=" + zw.spalona
                + " uwagi=" + zw.uwagi + " -> " + linia.replaceAll("§.", ""));
        koniecSceny(s, "§2Oprawione.", true);
        plugin.oprawianie().usun(zw);
    }

    /** Wydaje przedmiot klasy (2 = _WYBORNE z fallbackiem); zwraca opis do czatu. */
    private String wydaj(Player p, String id, int klasa, int ile) {
        String wid = klasa == 2 ? id + "_WYBORNE" : id;
        ItemStack it = plugin.przedmioty().zrob(wid, ile);
        if (it == null && klasa == 2) { plugin.getLogger().warning("Mysliwy: brak przedmiotu " + wid + " — wydaje " + id); it = plugin.przedmioty().zrob(id, ile); }
        if (it == null) { plugin.getLogger().warning("Mysliwy: brak przedmiotu " + id); return "§c(brak " + id + ")"; }
        for (ItemStack nad : p.getInventory().addItem(it).values()) p.getWorld().dropItemNaturally(p.getLocation(), nad);
        plugin.zlecenia().zebrano(p, PROF, id, ile);
        String nazwa = plugin.dane().nazwa(id);
        if (nazwa.equals(id) && it.hasItemMeta() && it.getItemMeta().hasDisplayName())
            nazwa = it.getItemMeta().getDisplayName().replaceAll("§.", "").replace(" (wyborna)", "").replace(" (wyborne)", "").replace(" (wyborny)", "");
        return ile + "× " + nazwa + " §7(" + (klasa == 2 ? "§6wyborne" : klasa == 1 ? "§adobre" : "§7słabe") + "§7)";
    }

    // ================================================================ wedzenie dziczyzny
    @EventHandler(priority = EventPriority.HIGH)
    public void naOgnisko(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (b.getType() != Material.CAMPFIRE && b.getType() != Material.SOUL_CAMPFIRE) return;
        Player p = e.getPlayer();
        ItemStack reka = p.getInventory().getItemInMainHand();
        String id = Przedmioty.id(reka);
        List<Kawalek> tu = new ArrayList<>();
        for (Kawalek k : wedzarnie) if (k.x == b.getX() && k.y == b.getY() && k.z == b.getZ() && k.swiat.equals(b.getWorld().getName())) tu.add(k);
        Kawalek gotowy = null;   // odbior gotowego ma pierwszenstwo (takze z dziczyzna w rece)
        for (Kawalek k : tu) if (k.wlasciciel.equals(p.getUniqueId()) && k.start + WEDZENIE_MS <= System.currentTimeMillis()) { gotowy = k; break; }
        if (gotowy == null && (MIESO.equals(id) || (MIESO + "_WYBORNE").equals(id))) {
            e.setCancelled(true);   // vanilla nie piecze dziczyzny na ognisku
            if (!(b.getBlockData() instanceof Lightable lt) || !lt.isLit()) { plugin.pasek(p, "§7Ognisko zgaszone — mięso się nie uwędzi."); return; }
            if (tu.size() >= MAX_NA_OGNISKU) { plugin.pasek(p, "§7Nad tym ogniskiem nie ma już miejsca."); return; }
            Kawalek k = new Kawalek();
            k.swiat = b.getWorld().getName(); k.x = b.getX(); k.y = b.getY(); k.z = b.getZ();
            k.wlasciciel = p.getUniqueId(); k.wyborne = id.endsWith("_WYBORNE"); k.start = System.currentTimeMillis();
            wedzarnie.add(k);
            reka.setAmount(reka.getAmount() - 1);
            p.getInventory().setItemInMainHand(reka.getAmount() <= 0 ? null : reka);
            pokazKawalek(k);
            zapisz();
            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS, 1f, 1f);
            plugin.pasek(p, "§2Dziczyzna zawieszona w dymie §7— " + WEDZENIE_MS / 1000 + " s. Pilnuj ognia." + (wprawa(p) < 5 ? " §8Zgaśnie = mięso nie doschnie." : ""));
            return;
        }
        Kawalek moj = gotowy;
        if (moj == null) for (Kawalek k : tu) if (k.wlasciciel.equals(p.getUniqueId())) { moj = k; break; }
        if (moj == null) { if (!tu.isEmpty()) { e.setCancelled(true); plugin.pasek(p, "§7To cudza dziczyzna."); } return; }
        e.setCancelled(true);
        long zostalo = moj.start + WEDZENIE_MS - System.currentTimeMillis();
        if (zostalo > 0) { plugin.pasek(p, "§7Wędzi się jeszcze " + (zostalo / 1000 + 1) + " s."); return; }
        wedzarnie.remove(moj);
        if (moj.widok != null && moj.widok.isValid()) moj.widok.remove();
        zapisz();
        boolean zepsute = moj.probki == 0 || (double) moj.zgasle / moj.probki > 0.3;
        if (zepsute) {
            p.sendMessage("§2[Myśliwy] §cMięso nie doschło — ogień gasł zbyt często. §8(" + moj.zgasle + "/" + moj.probki + " próbek bez ognia)");
            if (pomiar) plugin.getLogger().info("[mysliwy] wedzenie " + p.getName() + " zepsute " + moj.zgasle + "/" + moj.probki);
            return;
        }
        String wynik = wydaj(p, SUSZONE, moj.wyborne ? 2 : 1, 1);
        double dx = plugin.profesje().daj(p, PROF, 1, plugin.dane().g(1) * 0.3, b.getLocation());
        p.sendMessage("§2[Myśliwy] §f" + wynik + " §7+" + Math.round(dx) + " XP");
        if (pomiar) plugin.getLogger().info("[mysliwy] wedzenie " + p.getName() + " ok wyborne=" + moj.wyborne + " " + moj.zgasle + "/" + moj.probki);
    }

    private void pokazKawalek(Kawalek k) {
        World w = Bukkit.getWorld(k.swiat);
        if (w == null || !w.isChunkLoaded(k.x >> 4, k.z >> 4)) return;
        int nr = 0;
        for (Kawalek o : wedzarnie) { if (o == k) break; if (o.x == k.x && o.y == k.y && o.z == k.z && o.swiat.equals(k.swiat)) nr++; }
        ItemStack wyg = plugin.przedmioty().zrob(MIESO, 1);
        Location l = new Location(w, k.x + 0.2 + 0.2 * (nr % 4), k.y + 1.35, k.z + 0.5);
        k.widok = w.spawn(l, ItemDisplay.class, d -> {
            if (wyg != null) d.setItemStack(wyg);
            d.setPersistent(false);
            d.setViewRange(0.5f);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.35f, 0.35f, 0.35f), new AxisAngle4f()));
        });
    }

    private void probkujWedzarnie() {
        if (wedzarnie.isEmpty()) return;
        long teraz = System.currentTimeMillis();
        boolean zmiana = false;
        for (Kawalek k : wedzarnie) {
            if (teraz - k.start > WEDZENIE_MS) continue;
            World w = Bukkit.getWorld(k.swiat);
            if (w == null || !w.isChunkLoaded(k.x >> 4, k.z >> 4)) continue;
            Block b = w.getBlockAt(k.x, k.y, k.z);
            boolean pali = b.getBlockData() instanceof Lightable lt && lt.isLit();
            k.probki++;
            if (!pali) k.zgasle++;
            zmiana = true;
            if (pali) w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, k.x + 0.5, k.y + 1.1, k.z + 0.5, 1, 0.1, 0.1, 0.1, 0.01);
            if (k.widok == null || !k.widok.isValid()) pokazKawalek(k);
        }
        if (zmiana && teraz / 5000 % 6 == 0) zapisz();
    }

    // ================================================================ koniec z zewnatrz, testy
    private int wprawa(Player p) {
        try { return Integer.parseInt(plugin.zlecenia().flaga(p, "mysliwy_wprawa")); } catch (NumberFormatException e) { return 0; }
    }

    private void koniecGracza(Player p) {
        Low l = lowy.get(p.getUniqueId());
        if (l != null) koniecLowu(l, null);
        Scena s = sceny.get(p.getUniqueId());
        if (s != null) koniecSceny(s, null, false);
    }

    @EventHandler public void naWyjscie(PlayerQuitEvent e) { koniecGracza(e.getPlayer()); }
    @EventHandler public void naSmierc(PlayerDeathEvent e) { koniecGracza(e.getEntity()); }
    @EventHandler public void naSwiat(PlayerChangedWorldEvent e) { koniecGracza(e.getPlayer()); }

    void zatrzymajWszystko() {
        for (Low l : new ArrayList<>(lowy.values())) koniecLowu(l, null);
        for (Scena s : new ArrayList<>(sceny.values())) koniecSceny(s, null, false);
        for (Kawalek k : wedzarnie) if (k.widok != null && k.widok.isValid()) k.widok.remove();
        zapisz();
        rewiry.removeIf(r -> !r.trwaly);
    }

    /** Usuwa nietrwale rewiry instancji lochu, ktora znika (wzorzec "loch_*" zostaje). */
    void swiatZnika(String w) { rewiry.removeIf(r -> !r.trwaly && r.swiat.equals(w)); }

    String stan(Player p) {
        StringBuilder sb = new StringBuilder("wprawa=" + plugin.zlecenia().flaga(p, "mysliwy_wprawa"));
        Low l = lowy.get(p.getUniqueId());
        if (l != null) {
            sb.append(" low faza=").append(l.faza).append(" mob=").append(l.mob).append(" trop=").append(l.nr).append("/").append(l.tropy.size())
                    .append(" wiatr=").append(l.wiatrDeg).append(" t=").append(l.t);
            if (l.faza == Faza.TROP) sb.append(" tropXYZ=").append(fmt(l.tropy.get(l.nr)));
            Entity zw = l.zwierze == null ? null : Bukkit.getEntity(l.zwierze);
            if (zw != null) sb.append(" zwierzeXYZ=").append(fmt(zw.getLocation())).append(" zwierzeUUID=").append(zw.getUniqueId());
        }
        Scena s = sceny.get(p.getUniqueId());
        Oprawianie.Zwloki zw = s != null ? s.zw : plugin.oprawianie().najblizsze(p.getLocation(), 6);
        if (zw != null) {
            List<Ciecie> pl = plan(zw.typ);
            sb.append(" zwloki typ=").append(zw.typ).append(" spalona=").append(zw.spalona).append(" scena=").append(s != null)
                    .append(" aktywne=").append(s == null ? -1 : s.aktywne).append(" nast=").append(nastepne(zw))
                    .append(" kS=").append(fmt2(zw.kS)).append(" kM=").append(fmt2(zw.kM)).append(" ciecia=");
            for (int i = 0; i < pl.size(); i++) sb.append(i).append(zw.ciete[i] ? "+" : "-").append("@").append(fmt(punkt(zw, pl.get(i)))).append(";");
            sb.append(" zwlokiXYZ=").append(fmt(zw.l));
        }
        int w = 0;
        for (Kawalek k : wedzarnie) if (k.wlasciciel.equals(p.getUniqueId())) { w++; sb.append(" wedzi@").append(k.x).append(",").append(k.y).append(",").append(k.z)
                .append(" zostalo=").append(Math.max(0, k.start + WEDZENIE_MS - System.currentTimeMillis()) / 1000).append("s probki=").append(k.zgasle).append("/").append(k.probki); }
        if (w == 0) sb.append(" wedzarnia=0");
        return sb.toString();
    }

    String wymusWiatr(Player p, int deg) {
        Low l = lowy.get(p.getUniqueId());
        if (l == null) return "brak lowu";
        l.wiatrDeg = ((deg % 360) + 360) % 360;
        return "wiatr=" + l.wiatrDeg + " (" + nazwaWiatru(l.wiatrDeg) + ")";
    }

    /** Test: skroc wedzenie gracza (zostawia probki). */
    String wedzenieGotowe(Player p) {
        int n = 0;
        for (Kawalek k : wedzarnie) if (k.wlasciciel.equals(p.getUniqueId())) { k.start = System.currentTimeMillis() - WEDZENIE_MS - 1; n++; }
        return "gotowe " + n;
    }

    int ileRewirow() { return rewiry.size(); }

    private static String fmt(Location l) { return String.format(Locale.ROOT, "%.2f,%.2f,%.2f", l.getX(), l.getY(), l.getZ()); }
    private static String fmt2(double d) { return String.format(Locale.ROOT, "%.2f", d); }
}
