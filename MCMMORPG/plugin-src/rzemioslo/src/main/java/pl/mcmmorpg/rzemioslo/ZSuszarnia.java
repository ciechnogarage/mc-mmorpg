package pl.mcmmorpg.rzemioslo;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Suszarnia zielarza: wieszasz swiezy pek, czas robi reszte. Cien zachowuje moc, slonce ja wypala, deszcz = plesn.
 *  Nierozpoznane szerokie liscie trzeba tu przebrac (rozetrzec kazdy) — inaczej nie ida dalej. */
final class ZSuszarnia implements Listener {
    static final int MAX_PEKOW = 6;
    static final int MAX_W_PEKU = 8, MAX_NA_GRACZA = 2;   // nikt nie zablokuje suszarni stosem po 64
    static final long SUSZENIE_MS = 120_000;   // wycinek testowy; docelowo kilka/kilkanascie minut

    /** Co z czego powstaje przy suszeniu. */
    static final Map<String, String> WYNIK = Map.of(
            "PROF_MEDICINAL_HERB", "PROF_DRIED_HERBS",
            "PROF_KORZEN_KOZLKA", "PROF_SUSZONY_KORZEN_KOZLKA");

    static final class Pek {
        String id;
        int ilosc, klasa;
        long start;
        int probki, slonce, deszcz;
        java.util.UUID wlasciciel;   // null = stary wpis, zdejmie kazdy
        boolean zielarza;            // ze zbioru zielarza (ma jakosc); stare ziola z wezlow nie daja XP za suszenie
        ItemDisplay widok;
        org.bukkit.entity.BlockDisplay sznurek;
    }

    private final Rzemioslo plugin;
    private final Map<String, List<Pek>> suszarnie = new HashMap<>();
    private final Map<String, Boolean> podNiebem = new HashMap<>();
    private final File plik;
    private final NamespacedKey kJakosc;

    ZSuszarnia(Rzemioslo plugin) {
        this.plugin = plugin;
        this.plik = new File(plugin.getDataFolder(), "zielarz-suszarnie.yml");
        this.kJakosc = new NamespacedKey(plugin, "jakosc");
        wczytaj();
        Bukkit.getScheduler().runTaskTimer(plugin, this::probkuj, 200L, 200L);
    }

    String ustaw(Block b, boolean usun) {
        String k = Zielarz.klucz(b);
        if (usun) { List<Pek> l = suszarnie.remove(k); if (l != null) for (Pek p : l) { if (p.widok != null) p.widok.remove(); if (p.sznurek != null) p.sznurek.remove(); } zapisz(); return "suszarnia usunieta"; }
        suszarnie.putIfAbsent(k, new ArrayList<>());
        zapisz();
        return "suszarnia @ " + k;
    }

    // ================================================================ interakcja
    @EventHandler(priority = EventPriority.LOW)
    public void naKlik(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        List<Pek> l = suszarnie.get(Zielarz.klucz(e.getClickedBlock()));
        if (l == null) return;
        e.setCancelled(true);
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setUseItemInHand(Event.Result.DENY);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        ItemStack reka = p.getInventory().getItemInMainHand();
        String id = Przedmioty.id(reka);
        if ("PROF_SZEROKIE_LISCIE".equals(id)) { przebierz(p, reka); return; }
        if (id != null && WYNIK.containsKey(id)) { powies(p, b, l, reka, id); return; }
        zdejmij(p, b, l);
    }

    private void powies(Player p, Block b, List<Pek> l, ItemStack reka, String id) {
        if (l.size() >= MAX_PEKOW) { plugin.pasek(p, "§7Na suszarni nie ma już miejsca."); return; }
        long moich = l.stream().filter(x -> Zlecenia.profil(p).equals(x.wlasciciel)).count();
        if (moich >= MAX_NA_GRACZA) { plugin.pasek(p, "§7Masz tu już " + moich + " pęczki. Zdejmij suche, zanim powiesisz następny."); return; }
        Integer kl = reka.getItemMeta().getPersistentDataContainer().get(kJakosc, PersistentDataType.INTEGER);
        Pek k = new Pek();
        l.add(k);   // tymczasowo (wycofywane przy odmowie)
        k.id = id;
        k.ilosc = Math.min(MAX_W_PEKU, reka.getAmount());
        k.klasa = kl == null ? 1 : kl;
        k.zielarza = kl != null;
        if (k.klasa == 0) { l.remove(k); plugin.pasek(p, "§fSłabe ziele nie wyschnie na nic. §7Do suszenia tylko dobre i wyborne."); return; }
        k.start = System.currentTimeMillis();
        k.wlasciciel = Zlecenia.profil(p);   // per postac, nie per konto
        if (reka.getAmount() > k.ilosc) reka.setAmount(reka.getAmount() - k.ilosc);
        else p.getInventory().setItemInMainHand(null);
        pokazPek(b, l, k);
        p.playSound(b.getLocation(), Sound.BLOCK_GRASS_PLACE, SoundCategory.BLOCKS, 0.8f, 1.1f);
        plugin.pasek(p, "§7Wieszasz pęczek (" + k.ilosc + "). Schnie około " + Zielarz.czas(SUSZENIE_MS / 1000) + ".");
        zapisz();
    }

    private void zdejmij(Player p, Block b, List<Pek> l) {
        long teraz = System.currentTimeMillis();
        int zdjete = 0, cudze = 0;
        List<String> opis = new ArrayList<>();
        for (Pek k : new ArrayList<>(l)) {
            if (k.wlasciciel != null && !k.wlasciciel.equals(Zlecenia.profil(p))) { cudze++; continue; }
            if (teraz - k.start < SUSZENIE_MS) continue;
            l.remove(k);
            if (k.widok != null) k.widok.remove();
            if (k.sznurek != null) k.sznurek.remove();
            zdjete++;
            int klasa = k.klasa;
            String uw = null;
            // plesn stopniowo: kazda mokra probka psuje ~12% peczka (wczesniej jedna kropla = 60% na caly stos)
            int plesn = (int) Math.round(k.ilosc * Math.min(0.8, 0.12 * k.deszcz));
            if (plesn > 0) { daj(p, "PROF_SPLESNIALE_ZIELE", plesn, 0); opis.add("§c" + plesn + "× spleśniałe (zmokło)"); k.ilosc -= plesn; if (k.ilosc <= 0) continue; }
            if (k.probki > 0 && k.slonce * 2 > k.probki) { klasa = Math.max(0, klasa - 1); uw = "wypalone słońcem"; }
            // jakosc = wydajnosc: wyborne 2:1, dobre 3:1, slabe (np. wypalone) 4:1 — suszenie oplaca sie pod receptury, nie pod skup
            // zaokraglenie losowe: srednio dokladnie przelicznik, bez zysku z rozwieszania pojedynczo
            double ile = k.ilosc * (klasa == 2 ? 0.5 : klasa == 1 ? 1.0 / 3 : 0.25);
            int wyn = (int) ile + (ThreadLocalRandom.current().nextDouble() < ile - (int) ile ? 1 : 0);
            String z = " §8(z " + k.ilosc + ")";
            if (wyn == 0) { opis.add("§c" + k.ilosc + "× " + nazwa(k.id) + " " + (klasa == 0 ? "słabe" : "dobre") + ", pokruszyło się" + (uw != null ? " §7(" + uw + ")" : "")); continue; }
            daj(p, WYNIK.get(k.id), wyn, klasa);
            if (k.zielarza) plugin.profesje().daj(p, Zielarz.PROF, 1, plugin.dane().g(1) * (klasa == 2 ? 0.5 : 0.3) * wyn, b.getLocation().add(0.5, 0, 0.5));   // obrobka to tez zielarstwo
            opis.add((klasa == 2 ? "§6" : klasa == 1 ? "§a" : "§c") + wyn + "× " + nazwa(WYNIK.get(k.id)) + z + (uw != null ? " §7(" + uw + ")" : ""));
        }
        if (zdjete == 0) {
            if (cudze > 0 && cudze == l.size()) { plugin.pasek(p, "§7Te pęczki powiesił ktoś inny."); return; }
            if (l.isEmpty()) { plugin.pasek(p, "§7Pusta suszarnia. Powieś świeże ziele albo korzeń (PPM z nim w ręce)."); return; }
            long najb = Long.MAX_VALUE;
            for (Pek k : l) if (k.wlasciciel == null || k.wlasciciel.equals(Zlecenia.profil(p))) najb = Math.min(najb, SUSZENIE_MS - (teraz - k.start));
            plugin.pasek(p, "§7Jeszcze wilgotne. Najbliższy pęczek za §f" + Zielarz.czas(Math.max(1, najb / 1000)) + "§7.");
            return;
        }
        for (int i = 0; i < l.size(); i++) przestaw(b, l, l.get(i), i);
        p.playSound(b.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, SoundCategory.BLOCKS, 0.8f, 1f);
        p.sendMessage("§2Suszarnia: " + String.join("§7, ", opis));
        zapisz();
    }

    private void przebierz(Player p, ItemStack reka) {
        Integer partia = reka.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "partia"), PersistentDataType.INTEGER);
        String gat = partia == null ? null : plugin.zielarz().gatunekPartii(partia);   // gatunek zna tylko serwer (dane przedmiotu widzi klient)
        if (gat == null) { plugin.pasek(p, "§7Nie wiesz, skąd te liście. Nie da się ich przebrać."); return; }
        Integer kl = reka.getItemMeta().getPersistentDataContainer().get(kJakosc, PersistentDataType.INTEGER);
        int n = reka.getAmount();
        p.getInventory().setItemInMainHand(null);
        boolean czosnek = "czosnek".equals(gat);
        // rozcieranie kazdego liscia po fakcie gniecie je: jakosc o klase nizej (rozpoznanie przy zbiorze jest lepsze)
        daj(p, czosnek ? "PROF_WILD_SPICE" : "PROF_LISC_KONWALII", n, Math.max(0, (kl == null ? 1 : kl) - 1));
        p.playSound(p.getLocation(), Sound.ITEM_BRUSH_BRUSHING_GENERIC, SoundCategory.PLAYERS, 0.7f, 1.2f);
        p.sendMessage(czosnek ? "§2Rozcierasz liście po kolei: pachną czosnkiem. §7(" + n + "× czosnek niedźwiedzi, pogniecione)"
                : "§6Rozcierasz liście po kolei: bez zapachu, gorzkie. To konwalia. §7(" + n + "× liść konwalii, trujący)");
        plugin.zielarz().zielnik.odkryj(p, czosnek ? "czosnek_zapach" : "konwalia_zapach");
    }

    private String nazwa(String id) {
        String n = plugin.dane().nazwa(id);
        if (!n.equals(id)) return n;
        return switch (id) {
            case "PROF_SUSZONY_KORZEN_KOZLKA" -> "Suszony korzeń kozłka";
            case "PROF_KORZEN_KOZLKA" -> "Korzeń kozłka";
            default -> id;
        };
    }

    private void daj(Player p, String id, int ile, int klasa) {
        ItemStack it = plugin.przedmioty().zrob(id, ile);
        if (it == null) { plugin.getLogger().warning("Suszarnia: brak przedmiotu " + id); return; }
        ItemMeta m = it.getItemMeta();
        if (!id.equals("PROF_SPLESNIALE_ZIELE")) {
            List<Component> lore = m.lore() == null ? new ArrayList<>() : new ArrayList<>(m.lore());
            lore.add(Component.text(klasa == 2 ? "§6Jakość: wyborna" : klasa == 1 ? "§aJakość: dobra" : "§cJakość: słaba"));
            m.lore(lore);
            m.getPersistentDataContainer().set(kJakosc, PersistentDataType.INTEGER, klasa);
        }
        it.setItemMeta(m);
        for (ItemStack nad : p.getInventory().addItem(it).values()) p.getWorld().dropItemNaturally(p.getLocation(), nad);
    }

    // ================================================================ czas i warunki
    private void probkuj() {
        for (Map.Entry<String, List<Pek>> e : suszarnie.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            String[] a = e.getKey().split(";");
            World w = Bukkit.getWorld(a[0]);
            if (w == null) continue;
            Block b = w.getBlockAt(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
            // czas schniecia plynie zawsze; bez zaladowanego chunka pogode liczymy z zapamietanego miejsca (pod dachem / pod niebem)
            boolean zaladowany = w.isChunkLoaded(b.getX() >> 4, b.getZ() >> 4);
            if (zaladowany) podNiebem.put(e.getKey(), b.getRelative(0, 1, 0).getLightFromSky() >= 15);
            boolean niebo = podNiebem.getOrDefault(e.getKey(), false);
            long czas = w.getTime();
            boolean dzien = czas < 12300 || czas > 23850;
            long teraz = System.currentTimeMillis();
            for (Pek k : e.getValue()) {
                if (teraz - k.start >= SUSZENIE_MS) continue;
                k.probki++;
                if (niebo && dzien && !w.hasStorm()) k.slonce++;
                if (niebo && w.hasStorm()) {
                    k.deszcz++;
                    if (zaladowany && k.widok != null) w.spawnParticle(Particle.DRIPPING_WATER, k.widok.getLocation(), 2, 0.05, 0.05, 0.05);
                }
            }
        }
    }

    // ================================================================ wyglad: peczki wisza pod belka
    private void pokazPek(Block b, List<Pek> l, Pek k) { przestaw(b, l, k, l.indexOf(k)); }

    private void przestaw(Block b, List<Pek> l, Pek k, int i) {
        // pek wisi pionowo glowa w dol na krotkim sznurku pod belka, rzad wzdluz belki
        Location loc = b.getLocation().add(0.17 + (i % 3) * 0.33, -0.38, i < 3 ? 0.35 : 0.65);
        Location wez = b.getLocation().add(0.17 + (i % 3) * 0.33, 0, i < 3 ? 0.35 : 0.65);
        if (k.widok == null || !k.widok.isValid()) {
            ItemStack wyg = plugin.przedmioty().zrob(k.id, 1);
            float obrot = (float) Math.toRadians(ThreadLocalRandom.current().nextDouble(-12, 12));
            k.widok = b.getWorld().spawn(loc, ItemDisplay.class, d -> {
                d.setPersistent(false);
                d.setItemStack(wyg == null ? new ItemStack(Material.DEAD_BUSH) : wyg);
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                d.setBrightness(new org.bukkit.entity.Display.Brightness(11, 15));   // w ciemnej szopie pek byl czarna plama
                d.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotateY(obrot).rotateZ((float) Math.PI), new Vector3f(0.3f), new Quaternionf()));
            });
        } else k.widok.teleport(loc);
        if (k.sznurek == null || !k.sznurek.isValid())
            k.sznurek = b.getWorld().spawn(wez, org.bukkit.entity.BlockDisplay.class, d -> {
                d.setPersistent(false);
                d.setBlock(Material.STRIPPED_BIRCH_WOOD.createBlockData());   // jasny sznurek (brazowa welna wychodzila czarna)
                d.setBrightness(new org.bukkit.entity.Display.Brightness(11, 15));
                d.setTransformation(new Transformation(new Vector3f(-0.012f, -0.24f, -0.012f), new Quaternionf(), new Vector3f(0.024f, 0.24f, 0.024f), new Quaternionf()));
            });
        else k.sznurek.teleport(wez);
    }

    /** Peczki to encje nietrwale: po rozladowaniu chunka znikaja, wiec przy ponownym wczytaniu wieszamy je od nowa. */
    @EventHandler
    public void naChunk(org.bukkit.event.world.ChunkLoadEvent e) {
        String pref = e.getWorld().getName() + ";";
        for (Map.Entry<String, List<Pek>> s : suszarnie.entrySet()) {
            if (!s.getKey().startsWith(pref) || s.getValue().isEmpty()) continue;
            String[] a = s.getKey().split(";");
            int x = Integer.parseInt(a[1]), z = Integer.parseInt(a[3]);
            if (x >> 4 != e.getChunk().getX() || z >> 4 != e.getChunk().getZ()) continue;
            Block b = e.getWorld().getBlockAt(x, Integer.parseInt(a[2]), z);
            Bukkit.getScheduler().runTask(plugin, () -> { for (int i = 0; i < s.getValue().size(); i++) przestaw(b, s.getValue(), s.getValue().get(i), i); });
        }
    }

    void pokazWszystko() {
        for (Map.Entry<String, List<Pek>> e : suszarnie.entrySet()) {
            String[] a = e.getKey().split(";");
            World w = Bukkit.getWorld(a[0]);
            if (w == null) continue;
            Block b = w.getBlockAt(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
            for (int i = 0; i < e.getValue().size(); i++) przestaw(b, e.getValue(), e.getValue().get(i), i);
        }
    }

    void sprzatnij() {
        zapisz();
        for (List<Pek> l : suszarnie.values()) for (Pek k : l) { if (k.widok != null) k.widok.remove(); if (k.sznurek != null) k.sznurek.remove(); }
    }

    // ================================================================ zapis
    private void wczytaj() {
        if (!plik.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(plik);
        ConfigurationSection s = y.getConfigurationSection("suszarnie");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            List<Pek> l = new ArrayList<>();
            ConfigurationSection ps = s.getConfigurationSection(k);
            if (ps != null) for (String i : ps.getKeys(false)) {
                ConfigurationSection x = ps.getConfigurationSection(i);
                Pek p = new Pek();
                p.id = x.getString("id"); p.ilosc = x.getInt("ilosc"); p.klasa = x.getInt("klasa"); p.start = x.getLong("start");
                p.probki = x.getInt("probki"); p.slonce = x.getInt("slonce"); p.deszcz = x.getInt("deszcz"); p.zielarza = x.getBoolean("zielarza", true);
                String w = x.getString("wlasciciel"); if (w != null) p.wlasciciel = java.util.UUID.fromString(w);
                l.add(p);
            }
            suszarnie.put(Zielarz.zYaml(k), l);
        }
    }

    private void zapisz() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, List<Pek>> e : suszarnie.entrySet()) {
            String k = "suszarnie." + Zielarz.doYaml(e.getKey());
            y.createSection(k);
            int i = 0;
            for (Pek p : e.getValue()) {
                String b = k + "." + (i++);
                y.set(b + ".id", p.id); y.set(b + ".ilosc", p.ilosc); y.set(b + ".klasa", p.klasa); y.set(b + ".start", p.start);
                y.set(b + ".probki", p.probki); y.set(b + ".slonce", p.slonce); y.set(b + ".deszcz", p.deszcz); y.set(b + ".zielarza", p.zielarza);
                if (p.wlasciciel != null) y.set(b + ".wlasciciel", p.wlasciciel.toString());
            }
        }
        try { y.save(plik); } catch (IOException ex) { plugin.getLogger().warning("zielarz-suszarnie.yml: " + ex); }
    }
}
