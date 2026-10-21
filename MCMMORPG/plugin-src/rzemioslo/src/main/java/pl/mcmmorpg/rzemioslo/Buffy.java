package pl.mcmmorpg.rzemioslo;

import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.modifier.TemporaryStatModifier;
import io.lumine.mythic.lib.player.modifier.ModifierSource;
import io.lumine.mythic.lib.player.modifier.ModifierType;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmoitems.api.event.item.ConsumableConsumedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Buffy z alchemii, gotowania i krawiectwa (statystyki MythicLib, nie efekty vanilli) + obozowisko.
 *  Grupy: mikstura (natychmiast, wspolny cooldown), eliksir/olej/posilek (1 aktywny w grupie), opatrunek. */
final class Buffy implements Listener {
    private static final Set<PotionEffectType> ZLE = Set.of(PotionEffectType.POISON, PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.NAUSEA);

    private record Aktywny(String id, String nazwa, long koniec, List<TemporaryStatModifier> mody) {}

    private final Rzemioslo plugin;
    /** gracz -> grupa -> aktywny buff */
    private final Map<UUID, Map<String, Aktywny>> aktywne = new HashMap<>();
    /** gracz -> grupa -> koniec cooldownu */
    private final Map<UUID, Map<String, Long>> cooldowny = new HashMap<>();
    private final Map<UUID, Long> odpornosc = new HashMap<>();
    private final Map<UUID, BukkitTask> opatrunki = new HashMap<>();
    private final Map<UUID, Obozowisko> obozy = new HashMap<>();

    Buffy(Rzemioslo plugin) { this.plugin = plugin; }

    // ================================================================ konsumpcja
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void przedZjedzeniem(PlayerItemConsumeEvent e) {
        Dane.Buff b = plugin.dane().buffy.get(Przedmioty.id(e.getItem()));
        if (b == null || b.cooldown() <= 0) return;
        long kon = cooldowny.getOrDefault(e.getPlayer().getUniqueId(), Map.of()).getOrDefault(b.grupa(), 0L);
        if (kon > System.currentTimeMillis()) {
            e.setCancelled(true);
            plugin.pasek(e.getPlayer(), plugin.kom("cooldown", "{grupa}", b.grupa(), "{s}",
                    String.valueOf((kon - System.currentTimeMillis()) / 1000 + 1)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void poZjedzeniu(ConsumableConsumedEvent e) {
        Player p = e.getPlayer();
        String id = e.getMMOItem().getId();
        Dane.Buff b = plugin.dane().buffy.get(id);
        if (b == null) return;
        if (b.druzyna()) {
            int n = 0;
            for (Player x : druzynaWPoblizu(p, 8)) { zastosuj(x, b); n++; }
            plugin.pasek(p, plugin.kom("uczta", "{n}", String.valueOf(n)));
        } else {
            zastosuj(p, b);
        }
    }

    private List<Player> druzynaWPoblizu(Player p, double r) {
        List<Player> l = new ArrayList<>();
        l.add(p);
        try {
            PlayerData pd = PlayerData.get(p);
            if (pd.getParty() != null) for (PlayerData m : pd.getParty().getOnlineMembers()) {
                Player x = m.getPlayer();
                if (x != null && x != p && x.getWorld() == p.getWorld() && x.getLocation().distance(p.getLocation()) <= r) l.add(x);
            }
        } catch (Throwable ignored) { }
        return l;
    }

    void zastosuj(Player p, Dane.Buff b) {
        UUID u = p.getUniqueId();
        if (b.cooldown() > 0) cooldowny.computeIfAbsent(u, k -> new HashMap<>()).put(b.grupa(), System.currentTimeMillis() + b.cooldown() * 1000L);
        List<String> efekty = new ArrayList<>();
        List<TemporaryStatModifier> mody = new ArrayList<>();
        MMOPlayerData md = MMOPlayerData.get(p);
        for (Map.Entry<String, Double> s : b.staty().entrySet()) {
            double v = s.getValue();
            switch (s.getKey()) {
                case "leczenie" -> {
                    double max = p.getAttribute(Attribute.MAX_HEALTH).getValue();
                    p.setHealth(Math.min(max, p.getHealth() + v));
                    p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 1.8, 0), 3, 0.3, 0.2, 0.3);
                }
                case "mana" -> {
                    try { PlayerData.get(p).giveMana(v); } catch (Throwable ignored) { }
                }
                case "oczyszczenie" -> {
                    for (PotionEffectType t : ZLE) p.removePotionEffect(t);
                    odpornosc.put(u, System.currentTimeMillis() + Math.max(1, b.czas()) * 1000L);
                }
                case "leczenie_w_czasie" -> opatrunek(p, v, b.czas());
                default -> {
                    String stat = s.getKey().toUpperCase(Locale.ROOT).replace('-', '_');
                    TemporaryStatModifier m = new TemporaryStatModifier(staly(u, b.grupa(), stat), "rzemioslo_" + b.grupa(), stat, v,
                            ModifierType.FLAT, EquipmentSlot.OTHER, ModifierSource.OTHER);
                    m.register(md, b.czas() * 20L);
                    mody.add(m);
                }
            }
        }
        if (!mody.isEmpty()) {
            Map<String, Aktywny> g = aktywne.computeIfAbsent(u, k -> new HashMap<>());
            Aktywny stary = g.put(b.grupa(), new Aktywny(b.id(), b.nazwa(), System.currentTimeMillis() + b.czas() * 1000L, mody));
            if (stary != null) for (TemporaryStatModifier m : stary.mody()) { try { if (m.isActive()) m.close(); } catch (Throwable ignored) { } }
        }
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.4f, 1.3f);
        if (!b.druzyna()) plugin.pasek(p, plugin.kom("buff", "{nazwa}", b.nazwa(), "{czas}", czasTekst(b.czas())));
    }

    private void opatrunek(Player p, double razem, int sek) {
        BukkitTask stary = opatrunki.remove(p.getUniqueId());
        if (stary != null) stary.cancel();
        final double naSek = razem / Math.max(1, sek);
        final int[] ile = {0};
        BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || ile[0]++ >= sek) { BukkitTask x = opatrunki.remove(p.getUniqueId()); if (x != null) x.cancel(); return; }
            double max = p.getAttribute(Attribute.MAX_HEALTH).getValue();
            p.setHealth(Math.min(max, p.getHealth() + naSek));
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 2, 0.3, 0.4, 0.3);
        }, 20L, 20L);
        opatrunki.put(p.getUniqueId(), t);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void naObrazenia(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        BukkitTask t = opatrunki.remove(p.getUniqueId());
        if (t != null) {
            t.cancel();
            plugin.pasek(p, plugin.kom("opatrunek-przerwany"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void naEfekt(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p) || e.getNewEffect() == null) return;
        Long d = odpornosc.get(p.getUniqueId());
        if (d != null && d > System.currentTimeMillis() && ZLE.contains(e.getNewEffect().getType())) e.setCancelled(true);
    }

    @EventHandler
    public void naWyjscie(PlayerQuitEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        Map<String, Aktywny> g = aktywne.remove(u);
        if (g != null) for (Aktywny a : g.values()) for (TemporaryStatModifier m : a.mody()) { try { if (m.isActive()) m.close(); } catch (Throwable ignored) { } }
        BukkitTask t = opatrunki.remove(u);
        if (t != null) t.cancel();
        odpornosc.remove(u);
        Obozowisko o = obozy.remove(u);
        if (o != null) o.zakoncz();
    }

    /** "Eliksir siły III (4:12)" albo pusty — placeholder %rzemioslo_buff_<grupa>%. */
    String opis(Player p, String grupa) {
        Aktywny a = aktywne.getOrDefault(p.getUniqueId(), Map.of()).get(grupa);
        if (a == null || a.koniec() < System.currentTimeMillis()) return "";
        long s = (a.koniec() - System.currentTimeMillis()) / 1000;
        return a.nazwa() + " (" + s / 60 + ":" + String.format("%02d", s % 60) + ")";
    }

    /** Staly UUID modyfikatora: ponowna rejestracja tej samej grupy/statystyki zastepuje, a nie sumuje. */
    static UUID staly(UUID gracz, String grupa, String stat) {
        return UUID.nameUUIDFromBytes(("rzemioslo:" + gracz + ":" + grupa + ":" + stat).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    static String czasTekst(int s) {
        if (s <= 0) return "natychmiast";
        return s >= 60 ? (s / 60) + " min" : s + " s";
    }

    // ================================================================ obozowisko
    @EventHandler(priority = EventPriority.HIGH)
    public void naObozowisko(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || e.getItem() == null) return;
        Dane.Oboz o = plugin.dane().obozowiska.get(Przedmioty.id(e.getItem()));
        if (o == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (e.getBlockFace() != org.bukkit.block.BlockFace.UP || !e.getClickedBlock().getRelative(0, 1, 0).getType().isAir()) {
            plugin.pasek(p, plugin.kom("oboz-miejsce"));
            return;
        }
        ItemStack it = e.getItem();
        it.setAmount(it.getAmount() - 1);
        rozbij(p, o, e.getClickedBlock().getLocation().add(0, 1, 0));
    }

    /** Wspolne dla PPM i komendy admina (test): jeden oboz na gracza, nowy zastepuje stary. */
    void rozbij(Player p, Dane.Oboz o, Location l) {
        Obozowisko stary = obozy.remove(p.getUniqueId());
        if (stary != null) stary.zakoncz();
        obozy.put(p.getUniqueId(), new Obozowisko(p, o, l));
        plugin.pasek(p, plugin.kom("oboz", "{min}", String.valueOf(o.czas() / 60)));
    }

    private final class Obozowisko {
        final UUID wlasciciel;
        final Dane.Oboz o;
        final Location l;
        final UUID ogien;
        final BukkitTask zad;
        final long koniec;

        Obozowisko(Player p, Dane.Oboz o, Location l) {
            this.wlasciciel = p.getUniqueId();
            this.o = o;
            this.l = l;
            this.koniec = System.currentTimeMillis() + o.czas() * 1000L;
            Campfire cf = (Campfire) Bukkit.createBlockData(Material.CAMPFIRE);
            cf.setLit(true);
            BlockDisplay bd = l.getWorld().spawn(l, BlockDisplay.class, x -> {
                x.setBlock(cf);
                x.setPersistent(false);
                x.setTransformation(new Transformation(new Vector3f(0.1f, 0, 0.1f), new AxisAngle4f(), new Vector3f(0.8f, 0.8f, 0.8f), new AxisAngle4f()));
            });
            this.ogien = bd.getUniqueId();
            this.zad = Bukkit.getScheduler().runTaskTimer(plugin, this::tik, 40L, 40L);
        }

        void tik() {
            if (System.currentTimeMillis() > koniec) { zakoncz(); obozy.remove(wlasciciel); return; }
            Location c = l.clone().add(0.5, 0.5, 0.5);
            c.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.clone().add(0, 0.6, 0), 1, 0.05, 0.1, 0.05, 0.01);
            for (Player x : c.getWorld().getPlayers()) {
                if (x.getLocation().distance(c) > o.promien()) continue;
                boolean walka = false;
                try { walka = PlayerData.get(x).isInCombat(); } catch (Throwable ignored) { }
                if (walka) continue;
                MMOPlayerData md = MMOPlayerData.get(x);
                for (Map.Entry<String, Double> s : o.staty().entrySet()) {
                    String stat = s.getKey().toUpperCase(Locale.ROOT).replace('-', '_');
                    new TemporaryStatModifier(staly(x.getUniqueId(), "oboz", stat), "rzemioslo_oboz", stat, s.getValue(),
                            ModifierType.FLAT, EquipmentSlot.OTHER, ModifierSource.OTHER).register(md, 50L);
                }
            }
        }

        void zakoncz() {
            zad.cancel();
            Entity en = Bukkit.getEntity(ogien);
            if (en != null) en.remove();
            l.getWorld().spawnParticle(Particle.SMOKE, l.clone().add(0.5, 0.4, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
        }
    }

    void sprzatnij() {
        for (Obozowisko o : obozy.values()) o.zakoncz();
        obozy.clear();
        for (Map<String, Aktywny> g : aktywne.values()) for (Aktywny a : g.values()) for (TemporaryStatModifier m : a.mody()) {
            try { if (m.isActive()) m.close(); } catch (Throwable ignored) { }
        }
        aktywne.clear();
        for (BukkitTask t : opatrunki.values()) t.cancel();
        opatrunki.clear();
    }
}
