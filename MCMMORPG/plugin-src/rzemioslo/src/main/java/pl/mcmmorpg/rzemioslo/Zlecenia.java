package pl.mcmmorpg.rzemioslo;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Zlecenia dnia (3 na profesje z biezacego tieru) i zlecenia startowe zbieractwa — per POSTAC (profil MMOProfiles). */
final class Zlecenia {
    private final Rzemioslo plugin;
    private final File katalog;
    private final Map<UUID, YamlConfiguration> cache = new HashMap<>();

    Zlecenia(Rzemioslo plugin) {
        this.plugin = plugin;
        this.katalog = new File(plugin.getDataFolder(), "dane");
        katalog.mkdirs();
    }

    static UUID profil(Player p) {
        try {
            MMOPlayerData d = MMOPlayerData.getOrNull(p.getUniqueId());
            if (d != null && d.hasProfile()) return d.getProfileId();
        } catch (Throwable ignored) { }
        return p.getUniqueId();
    }

    private YamlConfiguration dane(Player p) {
        UUID u = profil(p);
        return cache.computeIfAbsent(u, k -> YamlConfiguration.loadConfiguration(new File(katalog, k + ".yml")));
    }

    private void zapisz(Player p) {
        UUID u = profil(p);
        YamlConfiguration y = cache.get(u);
        if (y == null) return;
        try {
            y.save(new File(katalog, u + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Nie zapisano zlecen " + u + ": " + e);
        }
    }

    void zapomnij(Player p) { cache.remove(profil(p)); }

    // ------------------------------------------------------------------ zlecenia dnia
    record Pozycja(Dane.Zlec z, int tier, boolean oddane) {}

    List<Pozycja> dzisiejsze(Player p, String prof) {
        YamlConfiguration y = dane(p);
        String dzis = LocalDate.now().toString();
        if (!dzis.equals(y.getString("dzien"))) {
            y.set("dzien", dzis);
            y.set("zlecenia", null);
        }
        ConfigurationSection s = y.getConfigurationSection("zlecenia." + prof);
        List<Dane.Zlec> pula;
        int tier;
        if (s == null) {
            tier = Profesje.tier(p, prof);
            pula = plugin.dane().zlecenia.getOrDefault(prof, Map.of()).getOrDefault(tier, List.of());
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < pula.size(); i++) idx.add(i);
            Collections.shuffle(idx, new Random((profil(p) + dzis + prof).hashCode()));
            List<Integer> wyb = idx.subList(0, Math.min(plugin.dane().zleceniaDziennie, idx.size()));
            y.set("zlecenia." + prof + ".tier", tier);
            y.set("zlecenia." + prof + ".wybrane", new ArrayList<>(wyb));
            y.set("zlecenia." + prof + ".oddane", new ArrayList<Integer>());
            zapisz(p);
            s = y.getConfigurationSection("zlecenia." + prof);
        }
        tier = s.getInt("tier");
        pula = plugin.dane().zlecenia.getOrDefault(prof, Map.of()).getOrDefault(tier, List.of());
        List<Integer> oddane = s.getIntegerList("oddane");
        List<Pozycja> l = new ArrayList<>();
        for (int i : s.getIntegerList("wybrane")) if (i < pula.size()) l.add(new Pozycja(pula.get(i), tier, oddane.contains(i)));
        return l;
    }

    void oddaj(Player p, String prof, int n) {
        List<Pozycja> l = dzisiejsze(p, prof);
        if (n < 1 || n > l.size()) return;
        Pozycja poz = l.get(n - 1);
        if (poz.oddane()) { plugin.pasek(p, plugin.kom("zlecenie-oddane")); return; }
        if (!Przedmioty.zabierz(p, poz.z().id(), poz.z().ilosc())) {
            plugin.pasek(p, plugin.kom("zlecenie-brak", "{brak}", String.valueOf(poz.z().ilosc() - Przedmioty.ile(p, poz.z().id())),
                    "{nazwa}", plugin.dane().nazwa(poz.z().id())));
            return;
        }
        YamlConfiguration y = dane(p);
        List<Integer> wyb = y.getIntegerList("zlecenia." + prof + ".wybrane");
        List<Integer> odd = new ArrayList<>(y.getIntegerList("zlecenia." + prof + ".oddane"));
        odd.add(wyb.get(n - 1));
        y.set("zlecenia." + prof + ".oddane", odd);
        zapisz(p);
        double xp = plugin.profesje().daj(p, prof, poz.tier(), poz.z().xp(), p.getLocation());
        Economy eko = ekonomia();
        if (eko != null) eko.depositPlayer(p, poz.z().pieniadze());
        plugin.pasek(p, plugin.kom("zlecenie-wykonane", "{xp}", String.valueOf(Math.round(xp)), "{kasa}", kwota(poz.z().pieniadze())));
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_YES, 0.7f, 1.1f);
    }

    // ------------------------------------------------------------------ zlecenia startowe
    String stanStartu(Player p, String prof) { return dane(p).getString("start." + prof + ".stan", "brak"); }

    int postepStartu(Player p, String prof) { return dane(p).getInt("start." + prof + ".postep", 0); }

    void start(Player p, String prof) {
        Dane.Start s = plugin.dane().startowe.get(prof);
        if (s == null) return;
        YamlConfiguration y = dane(p);
        String stan = stanStartu(p, prof);
        switch (stan) {
            case "brak" -> {
                y.set("start." + prof + ".stan", "aktywne");
                y.set("start." + prof + ".postep", 0);
                zapisz(p);
                plugin.przedmioty().daj(p, s.narzedzie(), 1);
                plugin.pasek(p, plugin.kom("start-przyjete", "{narzedzie}", plugin.dane().nazwa(s.narzedzie()),
                        "{ilosc}", String.valueOf(s.ilosc()), "{nazwa}", plugin.dane().nazwa(s.zbierz())));
            }
            case "aktywne" -> {
                if (postepStartu(p, prof) < s.ilosc()) {
                    plugin.pasek(p, plugin.kom("start-postep", "{postep}", String.valueOf(postepStartu(p, prof)),
                            "{ilosc}", String.valueOf(s.ilosc()), "{nazwa}", plugin.dane().nazwa(s.zbierz())));
                    return;
                }
                y.set("start." + prof + ".stan", "odebrane");
                zapisz(p);
                double xp = plugin.profesje().daj(p, prof, 1, s.xp(), p.getLocation());
                plugin.pasek(p, plugin.kom("start-wykonane", "{xp}", String.valueOf(Math.round(xp))));
            }
            default -> plugin.pasek(p, plugin.kom("start-juz"));
        }
    }

    /** Wolane przy kazdym zebraniu (wezel, lowisko, zwloki). */
    void zebrano(Player p, String prof, String id, int ile) {
        Dane.Start s = plugin.dane().startowe.get(prof);
        if (s == null || !s.zbierz().equals(id) || !"aktywne".equals(stanStartu(p, prof))) return;
        YamlConfiguration y = dane(p);
        int nowy = Math.min(s.ilosc(), postepStartu(p, prof) + ile);
        y.set("start." + prof + ".postep", nowy);
        zapisz(p);
        if (nowy >= s.ilosc()) plugin.pasek(p, plugin.kom("start-gotowe"));
    }

    // ------------------------------------------------------------------ wspolne
    Economy ekonomia() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return null;
        RegisteredServiceProvider<Economy> r = Bukkit.getServicesManager().getRegistration(Economy.class);
        return r == null ? null : r.getProvider();
    }

    static String kwota(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d).replace('.', ',') + " $";
    }
}
