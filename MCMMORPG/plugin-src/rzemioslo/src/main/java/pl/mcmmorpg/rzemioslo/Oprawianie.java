package pl.mcmmorpg.rzemioslo;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Mysliwstwo v3: bestia z listy 'zwierzyna' zostawia zwloki na N s; zabojca (i jego druzyna) oprawia je nozem
 *  scena ciec w Mysliwy (kolejnosc zalezna od zwierzecia, spalona skora). Zwykle zabicie nie daje XP ani surowcow. */
final class Oprawianie implements Listener {
    /** Zwloki upolowanej bestii; postep ciec i kary trzyma Mysliwy (scena moze byc przerwana i wznowiona). */
    final class Zwloki {
        final UUID interakcja, wyswietlacz;
        final Set<UUID> wolno;
        final Dane.Zrodlo z;
        final Location l;
        final EntityType typ;
        final boolean spalona;
        final float yaw;
        final boolean[] ciete;
        double kS, kM, kJ;
        final List<String> uwagi = new ArrayList<>();
        long wygasa;
        boolean zuzyte;

        Zwloki(UUID interakcja, UUID wyswietlacz, Set<UUID> wolno, Dane.Zrodlo z, Location l, EntityType typ, boolean spalona, float yaw) {
            this.interakcja = interakcja;
            this.wyswietlacz = wyswietlacz;
            this.wolno = wolno;
            this.z = z;
            this.l = l;
            this.typ = typ;
            this.spalona = spalona;
            this.yaw = yaw;
            this.ciete = new boolean[Mysliwy.plan(typ).size()];
        }
    }

    private final Rzemioslo plugin;
    private final Map<UUID, Zwloki> zwloki = new HashMap<>();

    Oprawianie(Rzemioslo plugin) { this.plugin = plugin; }

    /** ostatni gracz, ktory zranil encje (uuid encji -> (gracz, czas)) — gdy MythicMobs/Bukkit nie podaja zabojcy */
    private final Map<UUID, Map.Entry<UUID, Long>> ostatniCios = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void naCios(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        Player p = e.getDamager() instanceof Player x ? x
                : e.getDamager() instanceof org.bukkit.entity.Projectile pr && pr.getShooter() instanceof Player x2 ? x2 : null;
        if (p != null) ostatniCios.put(e.getEntity().getUniqueId(), Map.entry(p.getUniqueId(), System.currentTimeMillis()));
    }

    private Player zabojca(MythicMobDeathEvent e) {
        Player lowca = plugin.mysliwy().lowca(e.getEntity().getUniqueId());
        if (lowca != null) return lowca;
        if (e.getKiller() instanceof Player p) return p;
        if (e.getEntity() instanceof org.bukkit.entity.LivingEntity le && le.getKiller() != null) return le.getKiller();
        Map.Entry<UUID, Long> c = ostatniCios.remove(e.getEntity().getUniqueId());
        if (c != null && System.currentTimeMillis() - c.getValue() < 10_000) return Bukkit.getPlayer(c.getKey());
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void naSmierc(MythicMobDeathEvent e) {
        String mob = e.getMobType().getInternalName();
        Dane.Zrodlo z = plugin.dane().zwierzyna.get(mob);
        Player zabojca = z == null ? null : zabojca(e);
        if (plugin.getConfig().getBoolean("debug") || Mysliwy.pomiar) plugin.getLogger().info("smierc " + mob + " zwierzyna=" + (z != null) + " zabojca=" + (zabojca == null ? "-" : zabojca.getName()));
        ostatniCios.remove(e.getEntity().getUniqueId());
        if (z == null || zabojca == null) return;
        Location l = e.getEntity().getLocation();
        EntityType typ = e.getEntity().getType();
        // ogien pali skore: plonie w chwili smierci albo zginal od ognia/lawy
        var ost = e.getEntity().getLastDamageCause();
        var przyczyna = ost == null ? null : ost.getCause();
        boolean spalona = e.getEntity().getFireTicks() > 0 || przyczyna == EntityDamageEvent.DamageCause.FIRE
                || przyczyna == EntityDamageEvent.DamageCause.FIRE_TICK || przyczyna == EntityDamageEvent.DamageCause.LAVA
                || przyczyna == EntityDamageEvent.DamageCause.HOT_FLOOR || przyczyna == EntityDamageEvent.DamageCause.CAMPFIRE;
        float yaw = l.getYaw();
        Set<UUID> wolno = new HashSet<>();
        wolno.add(zabojca.getUniqueId());
        try {
            PlayerData pd = PlayerData.get(zabojca);
            if (pd.getParty() != null) for (PlayerData m : pd.getParty().getOnlineMembers()) wolno.add(m.getUniqueId());
        } catch (Throwable ignored) { }
        Interaction in = l.getWorld().spawn(l, Interaction.class, i -> {
            i.setInteractionWidth(1.0f);
            i.setInteractionHeight(0.6f);
            i.setResponsive(true);
            i.setPersistent(false);
        });
        ItemStack wyglad = plugin.przedmioty().zrob(z.glowny(), 1);
        ItemDisplay d = l.getWorld().spawn(l.clone().add(0, 0.05, 0), ItemDisplay.class, x -> {
            if (wyglad != null) x.setItemStack(wyglad);
            x.setPersistent(false);
            x.setRotation(yaw, 0);
            x.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0),
                    new Vector3f(0.9f, 0.9f, 0.9f), new AxisAngle4f()));
        });
        Zwloki zw = new Zwloki(in.getUniqueId(), d.getUniqueId(), wolno, z, l, typ, spalona, yaw);
        zwloki.put(in.getUniqueId(), zw);
        if (spalona) l.getWorld().spawnParticle(Particle.SMOKE, l.clone().add(0, 0.3, 0), 12, 0.3, 0.1, 0.3, 0.01);
        plugin.pasek(zabojca, plugin.kom("zwloki", "{s}", String.valueOf(plugin.dane().zwlokiS)));
        zw.wygasa = System.currentTimeMillis() + plugin.dane().zwlokiS * 1000L;
        if (plugin.getConfig().getBoolean("debug") || Mysliwy.pomiar) plugin.getLogger().info("zwloki " + mob + " typ=" + typ + " spalona=" + spalona + " przyczyna=" + przyczyna);
        wygasanie(zw);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void naKlik(PlayerInteractEntityEvent e) {
        Zwloki zw = zwloki.get(e.getRightClicked().getUniqueId());
        if (zw == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!zw.wolno.contains(p.getUniqueId())) {
            plugin.pasek(p, plugin.kom("cudze-zwloki"));
            return;
        }
        if (plugin.zbieranie().wTrakcie(p)) return;
        plugin.mysliwy().klikZwloki(p, zw);
    }

    /** Zwloki znikaja po zwloki-s; trwajaca scena ciec przedluza je o 10 s. */
    private void wygasanie(Zwloki zw) {
        long za = Math.max(1, (zw.wygasa - System.currentTimeMillis()) / 50);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (zw.zuzyte) return;
            if (plugin.mysliwy().oprawia(zw)) { zw.wygasa = System.currentTimeMillis() + 10_000; wygasanie(zw); return; }
            if (System.currentTimeMillis() < zw.wygasa) { wygasanie(zw); return; }
            usun(zw);
        }, za);
    }

    Zwloki najblizsze(Location l, double r) {
        Zwloki best = null; double bd = r * r;
        for (Zwloki zw : zwloki.values()) {
            if (zw.l.getWorld() != l.getWorld()) continue;
            double d = zw.l.distanceSquared(l);
            if (d < bd) { bd = d; best = zw; }
        }
        return best;
    }

    void usun(Zwloki zw) {
        if (zw.zuzyte) return;
        zw.zuzyte = true;
        zwloki.remove(zw.interakcja);
        for (UUID u : new UUID[]{zw.interakcja, zw.wyswietlacz}) {
            Entity en = Bukkit.getEntity(u);
            if (en != null) en.remove();
        }
        zw.l.getWorld().spawnParticle(Particle.POOF, zw.l.clone().add(0, 0.3, 0), 6, 0.3, 0.1, 0.3, 0.01);
    }

    void sprzatnij() {
        for (Zwloki zw : zwloki.values().toArray(new Zwloki[0])) usun(zw);
    }
}
