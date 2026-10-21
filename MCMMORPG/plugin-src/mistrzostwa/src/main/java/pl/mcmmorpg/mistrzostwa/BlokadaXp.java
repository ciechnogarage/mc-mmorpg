package pl.mcmmorpg.mistrzostwa;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.message.actionbar.ActionBarPriority;
import net.Indyuce.mmocore.api.event.PlayerExperienceGainEvent;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.experience.PlayerProfessions;
import net.Indyuce.mmocore.experience.Profession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralna blokada rozwoju profesji na progu.
 *
 * Punkt zaczepienia potwierdzony bajtkodem MMOCore 1.13.1: KAZDE zrodlo XP profesji idzie przez
 * PlayerProfessions.giveExperience(Profession, double, EXPSource, Location, boolean), ktore liczy
 * mnozniki, wola PlayerExperienceGainEvent i przy isCancelled() robi `return` PRZED hologramem,
 * dodaniem XP i petla awansow. Petla awansow potrafi w jednym wywolaniu przeskoczyc wiele
 * poziomow (np. 49 -> 55 przy duzym zastrzyku), dlatego ponizej progu zysk jest PRZYCINANY do
 * dokladnie tylu XP, ile brakuje do progu — samo "anuluj przy >= 50" byloby dziurawe.
 */
final class BlokadaXp implements Listener {
    private final Mistrzostwa plugin;
    private final Map<String, Long> ostatniKomunikat = new ConcurrentHashMap<>();

    BlokadaXp(Mistrzostwa plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void naZyskXp(PlayerExperienceGainEvent e) {
        if (!e.hasProfession()) return;
        Ustawienia u = plugin.ustawienia();
        Profession prof = e.getProfession();
        String id = prof.getId().toLowerCase(Locale.ROOT);
        if (!u.mistrzowskie.contains(id) || u.sab("blokada")) return;

        PlayerData pd = e.getData();
        Player p = pd.getPlayer();
        if (p == null) { e.setCancelled(true); return; }
        // brak profilu = brak dowodu mistrzostwa = blokada (bezpieczny domysl)
        if (plugin.stan().jestMistrzem(p, id)) return;

        PlayerProfessions pp = pd.getCollectionSkills();
        int poziom = pp.getLevel(prof);
        if (poziom >= u.prog) {
            e.setCancelled(true);
            powiadom(p, prof, u);
            return;
        }
        // ponizej progu: nie wiecej niz brakuje do progu (awans na prog z zerowym nadmiarem)
        double brakuje = -pp.getExperience(prof);
        for (int l = poziom; l < u.prog; l++) brakuje += prof.getExpCurve().getExperience(pd, l);
        if (e.getExperience() > brakuje) e.setExperience(Math.max(0d, brakuje));
    }

    private void powiadom(Player p, Profession prof, Ustawienia u) {
        String klucz = p.getUniqueId() + ":" + prof.getId();
        long teraz = System.currentTimeMillis();
        Long ost = ostatniKomunikat.get(klucz);
        if (ost != null && teraz - ost < u.blokadaCoMs) return;
        ostatniKomunikat.put(klucz, teraz);
        String tekst = u.blokada.replace("{profesja}", prof.getName()).replace("{prog}", String.valueOf(u.prog));
        try {
            // przez MythicLib: domyslny pasek MMOCore (co 5 tickow) nie nadpisze nas przez 3 s
            MMOPlayerData.get(p).getActionBar().show(ActionBarPriority.HIGH, 60L, tekst);
        } catch (Throwable t) {
            p.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(tekst));
        }
    }

    @EventHandler
    public void naWyjscie(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        ostatniKomunikat.keySet().removeIf(k -> k.startsWith(id.toString()));
    }
}
