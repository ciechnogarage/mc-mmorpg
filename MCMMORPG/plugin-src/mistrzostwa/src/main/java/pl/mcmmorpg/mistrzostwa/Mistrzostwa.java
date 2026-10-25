package pl.mcmmorpg.mistrzostwa;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Mistrzostwa extends JavaPlugin {
    private volatile Ustawienia ustawienia;
    private Stan stan;
    private LuckPerms lp;
    private KontekstProfilu kontekst;
    private Komenda komenda;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ustawienia = new Ustawienia(getConfig());
        lp = LuckPermsProvider.get();
        stan = new Stan(lp, ustawienia);
        kontekst = new KontekstProfilu(this, lp);
        lp.getContextManager().registerCalculator(kontekst);
        Bukkit.getPluginManager().registerEvents(kontekst, this);
        Bukkit.getPluginManager().registerEvents(new BlokadaXp(this), this);
        Komenda k = komenda = new Komenda(this);
        var cmd = getCommand("mistrzostwo");
        cmd.setExecutor(k);
        cmd.setTabCompleter(k);
        Bukkit.getPluginManager().registerEvents(k, this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) new Placeholdery(this).register();
        else getLogger().warning("Brak PlaceholderAPI — menu nie pokaze stanu mistrzostw.");
        if (ekonomia() == null) getLogger().severe("Brak dostawcy ekonomii Vault — platna zmiana mistrzostwa bedzie odrzucana.");
        sprawdzProfesje();
        getLogger().info("Blokada profesji na poziomie " + ustawienia.prog + ", limit mistrzostw " + ustawienia.limit
                + ", ogolne " + ustawienia.ogolne + ", mistrzowskie " + ustawienia.mistrzowskie.size());
    }

    @Override
    public void onDisable() {
        if (lp != null && kontekst != null) lp.getContextManager().unregisterCalculator(kontekst);
    }

    /** Glosno, jesli config nie pasuje do MMOCore — cicha literowka = dziura w blokadzie. */
    private void sprawdzProfesje() {
        if (!ustawienia.sabotaz.isEmpty())
            getLogger().severe("!!! SABOTAZ TESTOWY WLACZONY: " + ustawienia.sabotaz + " — reguly mistrzostw CELOWO zepsute. Na produkcji: sabotaz-testowy: [] !!!");
        for (String id : ustawienia.mistrzowskie) if (Stan.profesja(id) == null) getLogger().severe("Profesja z mistrzowskie nie istnieje w MMOCore: " + id);
        for (String id : ustawienia.ogolne) if (Stan.profesja(id) == null) getLogger().severe("Profesja z ogolne nie istnieje w MMOCore: " + id);
        for (var pr : net.Indyuce.mmocore.MMOCore.plugin.professionManager.getAll()) {
            String id = pr.getId().toLowerCase(java.util.Locale.ROOT);
            boolean znana = ustawienia.mistrzowskie.contains(id) || ustawienia.ogolne.contains(id);
            if (!znana && (!pr.hasMaxLevel() || pr.getMaxLevel() > ustawienia.prog))
                getLogger().warning("Profesja " + id + " nie podlega mistrzostwom, a ma max-level " + pr.getMaxLevel() + " > " + ustawienia.prog);
        }
    }

    void przeladuj() {
        reloadConfig();
        ustawienia = new Ustawienia(getConfig());
        stan.ustawienia(ustawienia);
        sprawdzProfesje();
    }

    Ustawienia ustawienia() { return ustawienia; }
    Stan stan() { return stan; }
    Komenda komenda() { return komenda; }

    Economy ekonomia() {
        RegisteredServiceProvider<Economy> r = Bukkit.getServicesManager().getRegistration(Economy.class);
        return r == null ? null : r.getProvider();
    }

    /** Dziennik wyborow i zmian — slad dla supportu ("kto, kiedy, za ile"). */
    synchronized void historia(String linia) {
        getLogger().info(linia);
        File f = new File(getDataFolder(), "historia.log");
        try (PrintWriter w = new PrintWriter(new FileWriter(f, StandardCharsets.UTF_8, true))) {
            w.println(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + linia);
        } catch (IOException e) {
            getLogger().warning("Nie zapisano historii: " + e.getMessage());
        }
    }
}
