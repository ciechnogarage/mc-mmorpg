package pl.mcmmorpg.chowance;

import fr.phoenixdevt.mmoprofiles.bukkit.MMOProfiles;
import fr.phoenixdevt.profiles.PlayerProfile;
import fr.phoenixdevt.profiles.ProfileList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pl.mcmmorpg.chowance.dane.MagazynDanych;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.gra.Bestiariusz;
import pl.mcmmorpg.chowance.gra.Ciala;
import pl.mcmmorpg.chowance.gra.Druzyny;
import pl.mcmmorpg.chowance.gra.Dzikie;
import pl.mcmmorpg.chowance.gra.Handel;
import pl.mcmmorpg.chowance.gra.Hodowla;
import pl.mcmmorpg.chowance.gra.Interakcje;
import pl.mcmmorpg.chowance.gra.Lapanie;
import pl.mcmmorpg.chowance.gra.Okna;
import pl.mcmmorpg.chowance.gra.Pojedynki;
import pl.mcmmorpg.chowance.gra.PierwszeKroki;
import pl.mcmmorpg.chowance.gra.Towarzysz;
import pl.mcmmorpg.chowance.gra.Kamera;
import pl.mcmmorpg.chowance.gra.Scena;
import pl.mcmmorpg.chowance.gra.ProfilModul;
import pl.mcmmorpg.chowance.gra.Przywolania;
import pl.mcmmorpg.chowance.gra.Ranking;
import pl.mcmmorpg.chowance.gra.Startery;
import pl.mcmmorpg.chowance.gra.Turnieje;
import pl.mcmmorpg.chowance.gra.WalkiTurowe;
import pl.mcmmorpg.chowance.gra.Zaproszenia;
import pl.mcmmorpg.chowance.integracja.MythicRejestracja;
import pl.mcmmorpg.chowance.integracja.Placeholdery;
import pl.mcmmorpg.chowance.tresc.LadowaczTresci;
import pl.mcmmorpg.chowance.tresc.Problemy;
import pl.mcmmorpg.chowance.tresc.Rejestr;

/**
 * Chowańce — silnik. Kolejność startu: config → treść (walidacja) → zapis → moduł MMOProfiles →
 * menedżery → listenery → komendy → PAPI → zadania okresowe (żadnego co tick: 20 ticków i rzadziej).
 */
public final class ChowancePlugin extends JavaPlugin implements Listener {
  private Chowance ctx;
  public Druzyny druzyny;
  public Przywolania przywolania;
  public Dzikie dzikie;
  public Lapanie lapanie;
  public Interakcje interakcje;
  public PierwszeKroki pierwszeKroki;
  public Towarzysz towarzysz;
  public Okna okna;
  public Startery startery;
  public WalkiTurowe walki;
  public Pojedynki pojedynki;
  public Kamera kamera;
  public pl.mcmmorpg.chowance.gra.TrenerNpc trenerNpc;
  public Hodowla hodowla;
  public Handel handel;
  public Bestiariusz bestiariusz;
  public Ranking ranking;
  public Turnieje turnieje;
  public Zaproszenia zaproszenia;
  private MythicRejestracja mythic;
  private ProfilModul modul;
  private int tikLicznik;

  public Chowance ctx() { return ctx; }

  public int mythicZarejestrowane() { return mythic == null ? 0 : mythic.zarejestrowane; }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    zasob("typy.yml", "typy.yml");
    zasob("charaktery.yml", "charaktery.yml");
    for (String f : new String[] {"przedmioty", "startery", "wieza", "bestiariusz-nagrody", "zdarzenia"}) zasob("szablony/" + f + ".yml", f + ".yml");
    // Gatunki/ruchy/cechy testowe silnika tylko na serwerach testowych (config: testowe_gatunki: true).
    // Wcześniej trafiały też na produkcję (world1, 2026-09-23).
    if (getConfig().getBoolean("testowe_gatunki", false)) {
      zasob("testowe/gatunki.yml", "gatunki/zz_silnik_test.yml");
      zasob("testowe/ruchy.yml", "ruchy/zz_silnik_test.yml");
      zasob("testowe/cechy.yml", "cechy/zz_silnik_test.yml");
    }
    new File(getDataFolder(), "trenerzy").mkdirs();

    Ustawienia ust = new Ustawienia(getConfig());
    LadowaczTresci l = new LadowaczTresci(getDataFolder());
    Rejestr rej = l.wczytaj();
    raport(l.problemy());
    MagazynDanych dane;
    try {
      dane = new MagazynDanych(getDataFolder().toPath().resolve("dane"), getLogger());
    } catch (IOException e) {
      getLogger().log(Level.SEVERE, "Nie da się utworzyć katalogu danych — Chowańce WYŁĄCZONE", e);
      Bukkit.getPluginManager().disablePlugin(this);
      return;
    }
    ctx = new Chowance(this, ust, rej, dane);

    okna = new Okna(this);
    okna.debug = () -> ctx != null && ctx.ust.debug;
    Ciala ciala = new Ciala(ctx);
    druzyny = new Druzyny(ctx);
    przywolania = new Przywolania(ctx, ciala, druzyny);
    dzikie = new Dzikie(ctx, ciala);
    bestiariusz = new Bestiariusz(ctx);
    druzyny.ustawBestiariusz(bestiariusz);
    ranking = new Ranking(ctx);
    lapanie = new Lapanie(ctx, dzikie, druzyny, przywolania);
    interakcje = new Interakcje(ctx, druzyny, przywolania);
    walki = new WalkiTurowe(ctx, okna, druzyny, przywolania, dzikie, ciala, ranking);
    walki.polacz(lapanie, interakcje);
    pojedynki = new Pojedynki(ctx, druzyny, przywolania, ranking);
    kamera = new Kamera(ctx);
    Scena scena = new Scena(ctx, ciala);
    pl.mcmmorpg.chowance.gra.HudWalki hud = new pl.mcmmorpg.chowance.gra.HudWalki(ctx);
    walki.kamera = kamera; walki.scena = scena; walki.hud = hud;
    scena.bezNapisow = p -> kamera.aktywna(p) && walki.wWalce(p);
    scena.bezEtykiet = p -> ctx.ust.hudMythic && kamera.aktywna(p);
    scena.walczacy = kamera::aktywni;
    pojedynki.kamera = kamera; pojedynki.scena = scena; pojedynki.hud = hud; pojedynki.ciala = ciala;
    trenerNpc = new pl.mcmmorpg.chowance.gra.TrenerNpc(ctx, okna, walki, pojedynki, druzyny);
    hodowla = new Hodowla(ctx, druzyny, przywolania);
    handel = new Handel(ctx, okna, druzyny, przywolania);
    turnieje = new Turnieje(ctx, walki, ranking);
    startery = new Startery(ctx, okna, druzyny);
    pierwszeKroki = new PierwszeKroki(ctx);
    towarzysz = new Towarzysz(ctx, przywolania);
    przywolania.towarzysz = towarzysz;
    startery.poWyborze = pierwszeKroki::poStarterze;
    interakcje.krok = pierwszeKroki::zalicz;
    przywolania.poOdwolaniu = (gracz, osobnik) -> {
      interakcje.wyczysc(gracz);
      towarzysz.wyczysc(gracz);
    };
    zaproszenia = new Zaproszenia(ctx);
    zaproszenia.okna = okna;

    java.util.function.Predicate<Player> wWalce = p -> walki.wWalce(p) || pojedynki.wPojedynku(p);
    przywolania.wWalce = wWalce;
    lapanie.wWalce = wWalce;
    przywolania.nietykalny = e -> dzikie.zajete.contains(e.getUniqueId());
    interakcje.wWalce = wWalce;

    var pm = Bukkit.getPluginManager();
    for (Listener x : new Listener[] {this, okna, przywolania, dzikie, lapanie, interakcje, walki, pojedynki, handel, kamera, trenerNpc, pierwszeKroki, towarzysz}) pm.registerEvents(x, this);
    if (pm.isPluginEnabled("ModelEngine")) {
      pm.registerEvents(new pl.mcmmorpg.chowance.integracja.MostModelEngine(interakcje, lapanie), this);
    }
    mythic = new MythicRejestracja(ctx, przywolania, dzikie, druzyny, lapanie, walki, pojedynki);
    pm.registerEvents(mythic, this);

    modul = new ProfilModul(this, ctx);
    modul.przedZdjeciem = this::sprzatnijSesje;
    modul.poWczytaniu = this::poWczytaniu;
    MMOProfiles.plugin.registerModule(modul);

    Komendy k = new Komendy(this);
    var cmd = getCommand("chw");
    if (cmd != null) { cmd.setExecutor(k); cmd.setTabCompleter(k); }

    if (pm.isPluginEnabled("PlaceholderAPI")) {
      Placeholdery ph = new Placeholdery(ctx, druzyny, przywolania, bestiariusz, ranking, hodowla, lapanie, walki, pojedynki, turnieje);
      ph.hud = walki.hud;
      ph.register();
    } else {
      getLogger().warning("Brak PlaceholderAPI — placeholdery %chowance_*% niedostępne.");
    }

    // Zadania: co 20 ticków (walki, przywołania, turniej), co skan_s (Bestiariusz), co probkowanie_s (jaja),
    // co 60 s (autozapis, jaja z hodowli). Nic co tick.
    Bukkit.getScheduler().runTaskTimer(this, this::tik, 20L, 20L);
    // rozkazy trenera w pojedynkach (pusto, gdy nie ma pojedynków)
    Bukkit.getScheduler().runTaskTimer(this, () -> pojedynki.kamery(), 4L, 4L);
    Bukkit.getScheduler().runTaskTimer(this, () -> kamera.podtrzymaj(), 40L, 40L);
    Bukkit.getScheduler().runTaskLater(this, () -> {
      try { pl.mcmmorpg.chowance.gra.Okna.rozgrzej(); } catch (RuntimeException | LinkageError ex) { getLogger().fine("rozgrzewka okien: " + ex); }
    }, 20L);
    Bukkit.getScheduler().runTaskTimer(this, () -> trenerNpc.pokazPoWalce(), 40L, 40L);
    Bukkit.getScheduler().runTaskTimer(this, () -> dzikie.skan(), 40L, ust.skanS * 20L);
    Bukkit.getScheduler().runTaskTimer(this, () -> hodowla.probkuj(), 100L, ust.probkowanieS * 20L);
    Bukkit.getScheduler().runTaskTimer(this, this::minuta, 1200L, 1200L);

    // /reload albo późny start: gracze już online z wybranym profilem
    for (Player p : Bukkit.getOnlinePlayers()) wczytajDlaOnline(p);
    getLogger().info("Chowańce włączone: gatunków " + rej.gatunki().size() + " (w Bestiariuszu " + rej.bestiariusz().size() + "), ruchów "
        + rej.ruchy().size() + ", cech " + rej.cechy().size() + ", trenerów " + rej.trenerzy().size() + ", MMOCore profesja "
        + (ctx.mmocore.dostepna() ? "OK" : "BRAK"));
  }

  private void zasob(String zasob, String cel) {
    File f = new File(getDataFolder(), cel);
    if (f.exists()) return;
    f.getParentFile().mkdirs();
    try (InputStream in = getResource(zasob)) {
      if (in != null) Files.copy(in, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      getLogger().warning("Nie skopiowano " + zasob + ": " + e);
    }
  }

  private void raport(Problemy pr) {
    for (Problemy.Wpis w : pr.wpisy()) {
      if (w.blad()) getLogger().severe("[treść] " + w); else getLogger().warning("[treść] " + w);
    }
  }

  /** /chw admin przeladuj: nowa migawka treści + config; walki trwają na starej migawce do końca tury. */
  public Problemy przeladuj() {
    reloadConfig();
    LadowaczTresci l = new LadowaczTresci(getDataFolder());
    Rejestr r = l.wczytaj();
    raport(l.problemy());
    ctx.rej = r;
    ctx.ust = new Ustawienia(getConfig());
    if (towarzysz != null) towarzysz.przeladuj();
    ctx.nazwyMmo.wczytaj(getDataFolder().getParentFile());
    return l.problemy();
  }

  private void wczytajDlaOnline(Player p) {
    try {
      ProfileList pl = MMOProfiles.plugin.getPlayerData(p.getUniqueId());
      PlayerProfile pr = pl == null ? null : pl.getCurrent();
      if (pr == null || ProfilModul.duch(p.getUniqueId(), pr.getUniqueId())) return;
      ctx.dane.wczytaj(pr.getUniqueId(), p.getUniqueId(), p.getName()).thenAccept(d -> Bukkit.getScheduler().runTask(this, () -> {
        // Gracz mógł w międzyczasie zmienić postać — podpinamy dane tylko do profilu, który nadal jest bieżący.
        ProfileList teraz = p.isOnline() ? MMOProfiles.plugin.getPlayerData(p.getUniqueId()) : null;
        PlayerProfile biezacy = teraz == null ? null : teraz.getCurrent();
        if (biezacy != null && biezacy.getUniqueId().equals(pr.getUniqueId())) {
          ctx.dane.aktywuj(p.getUniqueId(), d);
          poWczytaniu(p);
        }
      }));
    } catch (RuntimeException e) {
      getLogger().warning("Nie wczytano Chowańców gracza " + p.getName() + " po starcie: " + e);
    }
  }

  private void poWczytaniu(Player p) {
    ProfilDane d = ctx.dane(p);
    if (d == null) return;
    hodowla.sprawdzJaja(p, d);
    bestiariusz.sprawdzNagrody(p, d);
    pierwszeKroki.poWczytaniu(p, d);
    if (!d.starter && ctx.ust.starterAutoOkno && !ctx.rej.startery().domyslne().isEmpty()) {
      Bukkit.getScheduler().runTaskLater(this, () -> {
        ProfilDane teraz = ctx.dane(p);
        if (p.isOnline() && teraz == d && !d.starter) startery.otworz(p);
      }, ctx.ust.starterOpoznienie);
    }
  }

  /** Przed zdjęciem profilu (zmiana postaci / wyjście): walki, handel, ciało — zanim zapis. */
  private void sprzatnijSesje(Player p) {
    walki.przerwij(p);
    pojedynki.przerwij(p);
    handel.przerwij(p);
    przywolania.odwolaj(p, true);
    interakcje.wyczysc(p.getUniqueId());
    towarzysz.wyczysc(p.getUniqueId());
    zaproszenia.usun(p.getUniqueId());
    okna.zapomnij(p.getUniqueId());
  }

  private void tik() {
    tikLicznik++;
    przywolania.tik();
    walki.tik();
    pojedynki.tik();
    turnieje.tik();
  }

  private void minuta() {
    for (Player p : Bukkit.getOnlinePlayers()) {
      ProfilDane d = ctx.dane(p);
      if (d != null) hodowla.sprawdzJaja(p, d);
    }
    ctx.dane.zapiszBrudne();
  }

  /** debug: każde surowe kliknięcie dialogu (czy pakiet w ogóle dochodzi — PROBLEMY A-007). */
  @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
  public void surowyKlik(io.papermc.paper.event.player.PlayerCustomClickEvent e) {
    if (ctx != null && ctx.ust.debug) {
      getLogger().info("[debug] surowy klik " + e.getIdentifier() + " tag=" + (e.getTag() == null ? null : e.getTag().string()));
    }
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) {
    // Zapas na wypadek, gdyby MMOProfiles nie odpaliło ProfileUnloadEvent (np. gracz bez wybranego profilu).
    Player p = e.getPlayer();
    sprzatnijSesje(p);
    hodowla.zapomnij(p.getUniqueId());
    Bukkit.getScheduler().runTaskLater(this, () -> {
      if (Bukkit.getPlayer(p.getUniqueId()) == null && ctx.dane(p) != null) ctx.dane.dezaktywuj(p.getUniqueId());
    }, 40L);
  }

  @Override
  public void onDisable() {
    if (ctx == null) return;
    try {
      if (walki != null) walki.przerwijWszystkie();
      if (pojedynki != null) pojedynki.przerwijWszystkie();
      if (handel != null) handel.przerwijWszystkie();
      if (kamera != null) kamera.wylaczWszystkich();
      if (przywolania != null) przywolania.odwolajWszystkich();
    } catch (RuntimeException e) {
      getLogger().log(Level.SEVERE, "Błąd sprzątania przy wyłączaniu", e);
    }
    ctx.dane.zamknij();
  }
}
