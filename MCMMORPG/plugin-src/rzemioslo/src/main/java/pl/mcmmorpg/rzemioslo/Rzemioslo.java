package pl.mcmmorpg.rzemioslo;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.message.actionbar.ActionBarPriority;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Profesje v2 (MCMMORPG/profesje/PROJEKT.md). Dane: Rzemioslo/config.yml generowany przez generuj.py. */
public final class Rzemioslo extends JavaPlugin implements Listener {
    private static final Map<String, String> DOMYSLNE = new HashMap<>();
    static {
        DOMYSLNE.put("wyczerpane", "&7Wyczerpane — odnowi się za &f{s} s&7.");
        DOMYSLNE.put("ppm-wezel", "&7Kliknij &fPPM&7, aby zbierać (narzędzie wystarczy mieć w plecaku).");
        DOMYSLNE.put("za-niski-poziom", "&c{nazwa}: wymaga {profesja} &f{poziom}&c.");
        DOMYSLNE.put("brak-narzedzia", "&cPotrzebujesz {narzedzie} tieru {tier} — zlecenie startowe w menu profesji albo kowal.");
        DOMYSLNE.put("slabe-narzedzie", "&cTo źródło wymaga {narzedzie} tieru {tier} (masz {mam}).");
        DOMYSLNE.put("zepsute", "&c{nazwa} jest zepsuty — napraw go zestawem naprawczym.");
        DOMYSLNE.put("przerwane", "&7Przerwano.");
        DOMYSLNE.put("przerwane-ruch", "&7Przerwano — odszedłeś za daleko.");
        DOMYSLNE.put("przerwane-obrazenia", "&cPrzerwano — otrzymujesz obrażenia!");
        DOMYSLNE.put("postep", "&f{nazwa} {pasek}");
        DOMYSLNE.put("zebrano", "&a+ {lup} &8· &b{xp} XP");
        DOMYSLNE.put("perk-podwojny", " &6(premia: podwójny urobek)");
        DOMYSLNE.put("narzedzie-slabnie", " &c· narzędzie: zostało {zostalo} użyć");
        DOMYSLNE.put("wedka-vanilla", "&7Zwykła wędka łowi ryby. Surowce profesji łowi &fWędka rybaka&7.");
        DOMYSLNE.put("slaba-wedka", "&cTa woda to łowisko tieru {tier} — potrzebujesz wędki tieru {tier} (masz {mam}).");
        DOMYSLNE.put("zwloki", "&7Zwłoki można oprawić nożem (&fPPM&7) przez {s} s.");
        DOMYSLNE.put("cudze-zwloki", "&cTo nie twoja zdobycz.");
        DOMYSLNE.put("cooldown", "&cOdnowienie ({grupa}): jeszcze {s} s.");
        DOMYSLNE.put("buff", "&a{nazwa} &7— działa: &f{czas}");
        DOMYSLNE.put("uczta", "&aUczta! Najedzonych: &f{n}");
        DOMYSLNE.put("opatrunek-przerwany", "&cOpatrunek przerwany — obrażenia.");
        DOMYSLNE.put("oboz-miejsce", "&cObozowisko rozbijesz na wolnym miejscu na ziemi.");
        DOMYSLNE.put("oboz", "&aObóz rozbity na {min} min — drużyna odpoczywa szybciej poza walką.");
        DOMYSLNE.put("perk-zwrot", "&6Premia mistrza: odzyskano {nazwa}.");
        DOMYSLNE.put("xp-rzemiosla", "&b+{xp} XP &7({profesja})");
        DOMYSLNE.put("zlecenie-oddane", "&7To zlecenie już oddałeś.");
        DOMYSLNE.put("zlecenie-brak", "&cBrakuje {brak}× {nazwa}.");
        DOMYSLNE.put("zlecenie-wykonane", "&aZlecenie wykonane: &b{xp} XP &7i &e{kasa}");
        DOMYSLNE.put("start-przyjete", "&aDostajesz {narzedzie}. Zbierz {ilosc}× {nazwa} (PPM w źródło).");
        DOMYSLNE.put("start-postep", "&7Pierwsze kroki: {postep}/{ilosc}× {nazwa}.");
        DOMYSLNE.put("start-gotowe", "&aPierwsze kroki ukończone — odbierz nagrodę w menu profesji.");
        DOMYSLNE.put("start-wykonane", "&aPierwsze kroki: &b+{xp} XP&a. Dalej: zlecenia dnia i wyższe tiery.");
        DOMYSLNE.put("start-juz", "&7Pierwsze kroki tej profesji masz za sobą.");
        DOMYSLNE.put("przeprawa-brak", "&cPotrzebujesz: {nazwa} (Budownictwo).");
        DOMYSLNE.put("przeprawa-uzyta", "&aUżyto: {nazwa}.");
    }

    private volatile Dane dane;
    private Przedmioty przedmioty;
    private Profesje profesje;
    private Zbieranie zbieranie;
    private Oprawianie oprawianie;
    private Buffy buffy;
    private Zlecenia zlecenia;
    private Placeholdery papi;

    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            getLogger().severe("Brak Rzemioslo/config.yml — wygeneruj go: MCMMORPG/profesje/generuj.py. Plugin wylaczony.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        przedmioty = new Przedmioty(this);
        profesje = new Profesje(this);
        przeladuj();
        zbieranie = new Zbieranie(this);
        oprawianie = new Oprawianie(this);
        buffy = new Buffy(this);
        zlecenia = new Zlecenia(this);
        var pm = getServer().getPluginManager();
        pm.registerEvents(zbieranie, this);
        pm.registerEvents(new Lowienie(this), this);
        pm.registerEvents(oprawianie, this);
        pm.registerEvents(buffy, this);
        pm.registerEvents(new Stacje(this), this);
        pm.registerEvents(this, this);
        Komenda k = new Komenda(this);
        getCommand("rzemioslo").setExecutor(k);
        getCommand("rzemioslo").setTabCompleter(k);
        if (pm.isPluginEnabled("PlaceholderAPI")) { papi = new Placeholdery(this); papi.register(); }
        else getLogger().warning("Brak PlaceholderAPI — menu zlecen nie pokaze danych.");
        if (zlecenia.ekonomia() == null) getLogger().severe("Brak ekonomii Vault — zlecenia nie wyplaca pieniedzy.");
        // bloki Nexo sa gotowe dopiero po NexoItemsLoadedEvent (Nexo laduje przedmioty po starcie)
    }

    @Override
    public void onDisable() {
        if (zbieranie != null) zbieranie.zatrzymajWszystko();
        if (oprawianie != null) oprawianie.sprzatnij();
        if (buffy != null) buffy.sprzatnij();
        if (papi != null) papi.unregister();
        HandlerList.unregisterAll((org.bukkit.plugin.Plugin) this);
    }

    void przeladuj() {
        reloadConfig();
        dane = new Dane(getConfig());
        getLogger().info("Profesje v2: " + dane.wezly.size() + " wezlow, " + dane.lowiska.size() + " lowisk, " + dane.zwierzyna.size()
                + " zwierzyna, " + dane.narzedzia.size() + " narzedzi, " + dane.buffy.size() + " buffow, " + dane.receptury.size() + " receptur.");
        for (String pr : dane.problemy) getLogger().severe(pr);
    }

    /** Glosno, gdy config nie pasuje do serwera — cicha literowka = martwa mechanika. */
    private void sprawdz() {
        int brakProf = 0;
        for (String p : dane.nazwyProfesji.keySet()) if (Profesje.profesja(p) == null) { getLogger().severe("Brak profesji MMOCore: " + p); brakProf++; }
        int brakPrzed = 0;
        for (String id : dane.narzedzia.keySet()) if (przedmioty.zrob(id, 1) == null) { if (brakPrzed++ < 5) getLogger().severe("Brak przedmiotu MMOItems: " + id); }
        for (String id : dane.surowce) if (przedmioty.zrob(id, 1) == null) { if (brakPrzed++ < 5) getLogger().severe("Brak przedmiotu MMOItems: " + id); }
        int brakWezla = 0;
        try {
            for (String w : dane.wezly.keySet()) if (!com.nexomc.nexo.api.NexoBlocks.isCustomBlock(w)) { if (brakWezla++ < 5) getLogger().severe("Wezel bez bloku Nexo: " + w); }
            for (String w : dane.lowiskoBloki.keySet()) if (!com.nexomc.nexo.api.NexoBlocks.isCustomBlock(w)) { if (brakWezla++ < 5) getLogger().severe("Lowisko bez bloku Nexo: " + w); }
        } catch (Throwable t) {
            getLogger().warning("Nie sprawdzono blokow Nexo: " + t);
        }
        getLogger().info("Kontrola: brak profesji " + brakProf + ", brak przedmiotow " + brakPrzed + ", brak blokow " + brakWezla);
    }

    @EventHandler
    public void naWyjscie(PlayerQuitEvent e) { zlecenia.zapomnij(e.getPlayer()); }

    @EventHandler
    public void naNexo(com.nexomc.nexo.api.events.NexoItemsLoadedEvent e) {
        Bukkit.getScheduler().runTask(this, this::sprawdz);
    }

    String kom(String klucz, String... zam) {
        String t = getConfig().getString("komunikaty." + klucz, DOMYSLNE.getOrDefault(klucz, klucz));
        for (int i = 0; i + 1 < zam.length; i += 2) t = t.replace(zam[i], zam[i + 1]);
        return ChatColor.translateAlternateColorCodes('&', t);
    }

    void pasek(Player p, String tekst) {
        try {
            MMOPlayerData.get(p).getActionBar().show(ActionBarPriority.HIGH, 50L, tekst);
        } catch (Throwable t) {
            p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(tekst));
        }
    }

    Dane dane() { return dane; }
    Przedmioty przedmioty() { return przedmioty; }
    Profesje profesje() { return profesje; }
    Zbieranie zbieranie() { return zbieranie; }
    Buffy buffy() { return buffy; }
    Zlecenia zlecenia() { return zlecenia; }
}
