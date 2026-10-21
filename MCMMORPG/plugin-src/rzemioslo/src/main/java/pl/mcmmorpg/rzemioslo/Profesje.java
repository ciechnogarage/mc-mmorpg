package pl.mcmmorpg.rzemioslo;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.experience.EXPSource;
import net.Indyuce.mmocore.experience.Profession;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/** Poziomy, tiery, XP (z kara za nizszy tier) i perki. XP idzie przez PlayerProfessions.giveExperience,
 *  wiec przechodzi przez PlayerExperienceGainEvent — blokada mistrzostw (plugin Mistrzostwa) dziala sama. */
final class Profesje {
    private final Rzemioslo plugin;

    Profesje(Rzemioslo plugin) { this.plugin = plugin; }

    static Profession profesja(String id) {
        try {
            return MMOCore.plugin.professionManager.get(id);
        } catch (Throwable t) {
            return null;
        }
    }

    static int poziom(Player p, String prof) {
        Profession pr = profesja(prof);
        if (pr == null || !PlayerData.has(p.getUniqueId())) return 0;
        return PlayerData.get(p).getCollectionSkills().getLevel(pr);
    }

    static int tier(Player p, String prof) { return Dane.tierPoziomu(Math.max(1, poziom(p, prof))); }

    /** XP za akcje/receptury tieru `tierZrodla`; nizszy tier niz biezacy gracza = x kara^roznica. Zwraca przyznane XP. */
    double daj(Player p, String prof, int tierZrodla, double xp, Location gdzie) {
        Profession pr = profesja(prof);
        if (pr == null || !PlayerData.has(p.getUniqueId())) return 0;
        int roznica = tier(p, prof) - tierZrodla;
        if (roznica > 0) xp *= Math.pow(plugin.dane().kara, roznica);
        if (xp <= 0) return 0;
        PlayerData.get(p).getCollectionSkills().giveExperience(pr, xp, EXPSource.SOURCE, gdzie, gdzie != null);
        return xp;
    }

    /** Liczba zdobytych progow perkow (co 10 poziomow). */
    int progi(Player p, String prof) { return poziom(p, prof) / Math.max(1, plugin.dane().perkCo); }

    boolean podwojny(Player p, String prof) {
        return ThreadLocalRandom.current().nextDouble() < progi(p, prof) * plugin.dane().perkPodwojny;
    }

    boolean zwrot(Player p, String prof) {
        return ThreadLocalRandom.current().nextDouble() < progi(p, prof) * plugin.dane().perkZwrot;
    }

    /** Czas akcji zbierackiej [s] wg PROJEKT 3 + perk czasu. */
    double czas(Player p, String prof, int tierZrodla, int tierNarzedzia, double baza) {
        Dane d = plugin.dane();
        int lvl = poziom(p, prof);
        int wTierze = Math.max(0, lvl - d.start(Dane.tierPoziomu(Math.max(1, lvl))));
        double c = baza * (1 - d.czasNaTierNarzedzia * Math.max(0, tierNarzedzia - tierZrodla))
                * (1 - d.czasNaPoziomWTierze * wTierze) * (1 - d.perkCzas * progi(p, prof));
        return Math.max(d.czasMin, c);
    }
}
