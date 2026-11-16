package pl.mcmmorpg.trafienia;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.damage.AttackMetadata;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.damage.DamageType;
import io.lumine.mythic.lib.element.Element;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * MythicLib/MMOItems — ładowane tylko, gdy plugin jest włączony (osobna klasa: brak MythicLib nie wywraca Trafień).
 * Zaklęcia i umiejętności przychodzą jako ENTITY_ATTACK jak cios bronią; odróżnia je dopiero AttackMetadata MythicLib.
 */
final class Integracje {
    private Integracje() {}

    /** Opis ataku z MythicLib: czy to umiejętność (nie cios bronią) i jakie żywioły niesie. */
    record Atak(boolean umiejetnosc, boolean pocisk, Set<String> zywioly) {}

    static Atak atak(EntityDamageEvent e) {
        AttackMetadata am = MythicLib.plugin.getDamage().findAttack(e);
        if (am == null) return null;
        DamageMetadata d = am.getDamage();
        boolean umiej = (d.hasType(DamageType.SKILL) || d.hasType(DamageType.MAGIC) || d.hasType(DamageType.DOT))
                && !d.hasType(DamageType.WEAPON);
        Set<String> z = new HashSet<>();
        for (Element el : d.collectElements()) z.add(el.getId().toLowerCase(Locale.ROOT));
        return new Atak(umiej, d.hasType(DamageType.PROJECTILE), z);
    }

    /**
     * Test: obrażenia zaklęcia dokładnie tą drogą co mmodamage w skillach Zaklęć — atak MythicLib typu SKILL+MAGIC
     * z żywiołem, rzucający = gracz (jego statystyki). Zwraca false, gdy żywioł nieznany.
     */
    static boolean zaklecie(org.bukkit.entity.Player p, org.bukkit.entity.LivingEntity cel, String zywiol, double a) {
        Element el = MythicLib.plugin.getElements().get(zywiol.toUpperCase(Locale.ROOT));
        if (el == null) return false;
        var pm = io.lumine.mythic.lib.api.player.MMOPlayerData.get(p).getStatMap().cache(io.lumine.mythic.lib.api.player.EquipmentSlot.MAIN_HAND);
        AttackMetadata am = new AttackMetadata(new DamageMetadata(a, el, DamageType.SKILL, DamageType.MAGIC), cel, pm);
        MythicLib.plugin.getDamage().damage(am, cel, true);
        return true;
    }

    /** Test: obszarowe obrażenia MythicLib (jak kula ognia) w jedną encję. */
    static void obszarMl(org.bukkit.entity.Player p, org.bukkit.entity.LivingEntity cel, double a) {
        var pm = io.lumine.mythic.lib.api.player.MMOPlayerData.get(p).getStatMap().cache(io.lumine.mythic.lib.api.player.EquipmentSlot.MAIN_HAND);
        MythicLib.plugin.getDamage().damage(new AttackMetadata(new DamageMetadata(a, DamageType.SKILL, DamageType.MAGIC), cel, pm), cel, true);
    }

    /** Typ przedmiotu MMOItems (SWORD, DAGGER, GREATHAMMER...) albo null dla zwykłego przedmiotu. */
    static String typMmo(ItemStack it) {
        if (it == null || it.getType().isAir()) return null;
        String t = NBTItem.get(it).getType();
        return t == null || t.isEmpty() ? null : t.toUpperCase(Locale.ROOT);
    }
}
