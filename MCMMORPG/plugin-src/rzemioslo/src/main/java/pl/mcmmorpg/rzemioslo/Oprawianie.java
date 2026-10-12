package pl.mcmmorpg.rzemioslo;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Mysliwstwo v2: bestia z listy 'zwierzyna' zostawia zwloki na N s; zabojca (i jego druzyna) oprawia je nozem
 *  (PPM, ten sam silnik co wezly). Zwykle zabicie nie daje XP mysliwstwa ani surowcow. */
final class Oprawianie implements Listener {
    private final class Zwloki implements Zbieranie.Cel {
        final UUID interakcja, wyswietlacz;
        final Set<UUID> wolno;
        final Dane.Zrodlo z;
        final Location l;
        boolean zuzyte;

        Zwloki(UUID interakcja, UUID wyswietlacz, Set<UUID> wolno, Dane.Zrodlo z, Location l) {
            this.interakcja = interakcja;
            this.wyswietlacz = wyswietlacz;
            this.wolno = wolno;
            this.z = z;
            this.l = l;
        }

        public Location miejsce() { return l.clone().add(0, 0.3, 0); }
        public String profesja() { return "hunting"; }
        public int tier() { return z.tier(); }
        public String rodzaj() { return "noz"; }
        public double czasBazowy() { return plugin.dane().czasOprawiania; }
        public Dane.Zrodlo nagroda() { return z; }
        public String nazwa() { return "Zwłoki: " + plugin.dane().nazwa(z.glowny()); }
        public Block blok() { return null; }
        public boolean aktualny() { return !zuzyte && Bukkit.getEntity(interakcja) != null; }
        public void poSukcesie(Player p) { usun(this); }
        public org.bukkit.Particle.DustOptions kurz() { return null; }
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
        if (plugin.getConfig().getBoolean("debug")) plugin.getLogger().info("smierc " + mob + " zwierzyna=" + (z != null) + " zabojca=" + (zabojca == null ? "-" : zabojca.getName()));
        ostatniCios.remove(e.getEntity().getUniqueId());
        if (z == null || zabojca == null) return;
        Location l = e.getEntity().getLocation();
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
            x.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0),
                    new Vector3f(0.9f, 0.9f, 0.9f), new AxisAngle4f()));
        });
        Zwloki zw = new Zwloki(in.getUniqueId(), d.getUniqueId(), wolno, z, l);
        zwloki.put(in.getUniqueId(), zw);
        plugin.pasek(zabojca, plugin.kom("zwloki", "{s}", String.valueOf(plugin.dane().zwlokiS)));
        Bukkit.getScheduler().runTaskLater(plugin, () -> usun(zw), plugin.dane().zwlokiS * 20L);
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
        plugin.zbieranie().rozpocznij(p, zw);
    }

    private void usun(Zwloki zw) {
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
