package pl.mcmmorpg.wiary;

import fr.phoenixdevt.mmoprofiles.bukkit.MMOProfiles;
import fr.phoenixdevt.profiles.PlayerProfile;
import fr.phoenixdevt.profiles.ProfileList;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.modifier.TemporaryStatModifier;
import io.lumine.mythic.lib.player.modifier.ModifierSource;
import io.lumine.mythic.lib.player.modifier.ModifierType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.attribute.PlayerAttributes;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Wiary: patron postaci (3 Starszych + 3 bóstwa rasy) i błogosławieństwo — czasowe statystyki MythicLib,
 * których siła rośnie z atrybutem MMOCore 'wiara'. Gracz obsługuje wszystko z menu DeluxeMenus 'wiara'
 * (hub pod Q -> Postać -> Wiara); menu woła komendy jako konsola.
 */
public final class WiaryPlugin extends JavaPlugin implements Listener {
  /** Klucz modyfikatorów MythicLib — po nim poznajemy nasze statystyki. */
  public static final String KLUCZ = "wiary_laska";

  /** Aktywna postać gracza: id profilu, jej dane i nałożone modyfikatory. */
  static final class Stan {
    final UUID profil;
    final Magazyn.Dane dane;
    final List<TemporaryStatModifier> mody = new ArrayList<>();
    Stan(UUID profil, Magazyn.Dane dane) { this.profil = profil; this.dane = dane; }
  }

  private final Map<UUID, Stan> aktywne = new ConcurrentHashMap<>();
  private final Map<String, String> klasaNaRase = new HashMap<>();
  private Panteon panteon;
  private Magazyn magazyn;
  private long czasLaskiMs;
  private long odnowienieZmianyMs;
  private String atrybut;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    magazyn = new Magazyn(getDataFolder());
    przeladuj();
    MMOProfiles.plugin.registerModule(new ProfilModul(this));
    getServer().getPluginManager().registerEvents(this, this);
    Komenda k = new Komenda(this);
    getCommand("wiary").setExecutor(k);
    getCommand("wiary").setTabCompleter(k);
    if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new Placeholdery(this).register();
    } else {
      getLogger().warning("Brak PlaceholderAPI — menu Wiary nie pokaże stanu postaci.");
    }
    // Gracze obecni przy starcie (np. /reload): wczytaj ich bieżącą postać.
    for (Player p : Bukkit.getOnlinePlayers()) {
      UUID pr = biezacyProfil(p);
      if (pr != null) aktywuj(p, pr);
    }
  }

  @Override
  public void onDisable() {
    for (Player p : Bukkit.getOnlinePlayers()) dezaktywuj(p);
  }

  /** Config + mapa klas MMOCore -> rasa. Zwraca liczbę problemów (zalogowanych). */
  public int przeladuj() {
    reloadConfig();
    List<String> problemy = new ArrayList<>();
    panteon = Panteon.wczytaj(getConfig(), problemy);
    czasLaskiMs = getConfig().getLong("laska.czas-minut", 30) * 60_000L;
    odnowienieZmianyMs = getConfig().getLong("zmiana-patrona.odnowienie-godzin", 24) * 3_600_000L;
    atrybut = getConfig().getString("laska.atrybut", "wiara");
    klasaNaRase.clear();
    File klasy = new File(getDataFolder().getParentFile(), "MMOCore/classes");
    for (String rasa : panteon.rasy().keySet()) {
      File[] pliki = new File(klasy, rasa).listFiles((d, n) -> n.endsWith(".yml"));
      if (pliki == null || pliki.length == 0) {
        problemy.add("brak klas rasy " + rasa + " w " + new File(klasy, rasa));
        continue;
      }
      for (File f : pliki) {
        String id = f.getName().substring(0, f.getName().length() - 4).toUpperCase(Locale.ROOT).replace('-', '_');
        klasaNaRase.put(id, rasa);
      }
    }
    for (String rasa : panteon.rasy().keySet()) {
      int n = panteon.doWyboru(rasa).size();
      if (n != 6) problemy.add("rasa " + rasa + " ma " + n + " bóstw do wyboru (oczekiwane 6)");
    }
    for (String p : problemy) getLogger().warning(p);
    getLogger().info("Wiary: " + panteon.bostwa().size() + " bóstw, " + klasaNaRase.size() + " klas z rasą, problemów: " + problemy.size());
    return problemy.size();
  }

  public Panteon panteon() { return panteon; }
  public Magazyn magazyn() { return magazyn; }
  public long odnowienieZmianyMs() { return odnowienieZmianyMs; }
  Stan stan(Player p) { return aktywne.get(p.getUniqueId()); }

  /** Profil-duch MMOProfiles (id profilu == UUID gracza) nie jest postacią — nie dostaje wiary. */
  static boolean duch(UUID gracz, UUID profil) { return gracz.equals(profil); }

  UUID biezacyProfil(Player p) {
    try {
      ProfileList pl = MMOProfiles.plugin.getPlayerData(p.getUniqueId());
      PlayerProfile pr = pl == null ? null : pl.getCurrent();
      if (pr == null || duch(p.getUniqueId(), pr.getUniqueId())) return null;
      return pr.getUniqueId();
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** Rasa aktywnej postaci z jej klasy MMOCore; null = brak postaci albo klasa bez rasy (np. scena wyboru). */
  public String rasa(Player p) {
    try {
      PlayerClass k = PlayerData.get(p).getProfess();
      return k == null ? null : klasaNaRase.get(k.getId().toUpperCase(Locale.ROOT));
    } catch (RuntimeException e) {
      return null;
    }
  }

  public String klasa(Player p) {
    try {
      PlayerClass k = PlayerData.get(p).getProfess();
      return k == null ? "?" : k.getId();
    } catch (RuntimeException e) {
      return "?";
    }
  }

  /** Suma atrybutu Wiara (punkty + przedmioty). */
  public int wiara(Player p) {
    try {
      PlayerAttributes.AttributeInstance a = PlayerData.get(p).getAttributes().getInstance(atrybut);
      return a == null ? 0 : a.getTotal();
    } catch (RuntimeException e) {
      return 0;
    }
  }

  // ---- cykl życia postaci ----

  void aktywuj(Player p, UUID profil) {
    zdejmijMody(p);
    if (duch(p.getUniqueId(), profil)) {
      aktywne.remove(p.getUniqueId());
      return;
    }
    Stan s = new Stan(profil, magazyn.wczytaj(profil));
    aktywne.put(p.getUniqueId(), s);
    // Łaskę nakładamy chwilę później: przy wyborze postaci MMOCore/MythicLib jeszcze przestawiają dane.
    Bukkit.getScheduler().runTaskLater(this, () -> {
      if (p.isOnline() && aktywne.get(p.getUniqueId()) == s) nalozLaske(p, s);
    }, 20L);
  }

  void dezaktywuj(Player p) {
    zdejmijMody(p);
    aktywne.remove(p.getUniqueId());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    dezaktywuj(e.getPlayer());
  }

  private void zdejmijMody(Player p) {
    Stan s = aktywne.get(p.getUniqueId());
    MMOPlayerData mmo = null;
    try { mmo = MMOPlayerData.get(p); } catch (RuntimeException ignored) { }
    if (s != null) {
      for (TemporaryStatModifier m : s.mody) {
        try { m.close(); } catch (RuntimeException ignored) { }
        if (mmo != null) try { m.unregister(mmo); } catch (RuntimeException ignored) { }
      }
      s.mody.clear();
    }
    // Asekuracja: modyfikator z naszym kluczem, który przeżył poza listą (np. po błędzie), też znika.
    if (mmo != null) {
      for (Panteon.Bostwo b : panteon.bostwa().values()) {
        for (Panteon.Efekt e : b.efekty()) {
          try { mmo.getStatMap().getInstance(e.stat()).removeIf(k -> k.equals(KLUCZ)); } catch (RuntimeException ignored) { }
        }
      }
    }
  }

  /** Nakłada zapisaną łaskę z pozostałym czasem (po wyborze postaci albo po modlitwie). */
  void nalozLaske(Player p, Stan s) {
    Magazyn.Dane d = s.dane;
    long zostalo = d.laskaDo - System.currentTimeMillis();
    Panteon.Bostwo b = panteon.bostwo(d.laskaBostwo);
    if (zostalo <= 0 || b == null) return;
    MMOPlayerData mmo = MMOPlayerData.get(p);
    long ticki = Math.max(1, zostalo / 50);
    for (Panteon.Efekt e : b.efekty()) {
      TemporaryStatModifier m = new TemporaryStatModifier(KLUCZ, e.stat(), e.wartosc(d.laskaWiara, panteon.limit()),
          ModifierType.valueOf(e.typ()), EquipmentSlot.OTHER, ModifierSource.OTHER);
      m.register(mmo, ticki);
      s.mody.add(m);
    }
  }

  // ---- akcje gracza (z menu) ----

  /** Wynik akcji: komunikat dla gracza i krótka linia dla konsoli. */
  record Wynik(boolean ok, String graczowi, String konsoli) { }

  Wynik wybierz(Player p, String bostwoId) {
    Stan s = aktywne.get(p.getUniqueId());
    if (s == null) return new Wynik(false, "&cNajpierw wybierz postać.", "brak aktywnej postaci");
    String rasa = rasa(p);
    if (rasa == null) return new Wynik(false, "&cTwoja postać nie ma jeszcze rasy — dokończ jej tworzenie.", "klasa " + klasa(p) + " bez rasy");
    Panteon.Bostwo b = panteon.bostwo(bostwoId);
    if (b == null) return new Wynik(false, "&cNie ma takiego bóstwa.", "nieznane bóstwo " + bostwoId);
    if (!panteon.dostepne(b, rasa)) return new Wynik(false, "&c" + b.imie() + " nie przyjmuje modlitw twojej rasy.", "bóstwo " + b.id() + " niedostępne dla rasy " + rasa);
    Magazyn.Dane d = s.dane;
    if (b.id().equals(d.patron)) return new Wynik(false, "&7" + b.imie() + " już jest twoim patronem.", "bez zmian");
    long czekaj = Panteon.doZmiany(d.patron, d.zmiana, System.currentTimeMillis(), odnowienieZmianyMs);
    if (czekaj > 0) return new Wynik(false, "&cPatrona zmienisz za " + Panteon.czas(czekaj) + ".", "odnowienie " + Panteon.czas(czekaj));
    boolean pierwszy = d.patron == null;
    zdejmijMody(p);
    d.patron = b.id();
    d.zmiana = System.currentTimeMillis();
    d.laskaDo = 0;
    d.laskaBostwo = null;
    d.laskaWiara = 0;
    if (!zapisz(p, s)) return new Wynik(false, "&cNie udało się zapisać wyboru — zgłoś to administracji.", "błąd zapisu");
    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.2f);
    String tekst = (pierwszy ? "&fTwój patron: " : "&fNowy patron: ") + b.kolor() + b.pelne()
        + "&f. Módl się, aby otrzymać łaskę." + (pierwszy ? "" : " &7Kolejna zmiana za " + Panteon.czas(odnowienieZmianyMs) + ".");
    return new Wynik(true, tekst, "patron " + b.id());
  }

  Wynik modlitwa(Player p) {
    Stan s = aktywne.get(p.getUniqueId());
    if (s == null) return new Wynik(false, "&cNajpierw wybierz postać.", "brak aktywnej postaci");
    Magazyn.Dane d = s.dane;
    Panteon.Bostwo b = panteon.bostwo(d.patron);
    if (b == null) return new Wynik(false, "&cNajpierw obierz patrona.", "brak patrona");
    String rasa = rasa(p);
    if (!panteon.dostepne(b, rasa)) return new Wynik(false, "&c" + b.imie() + " nie słucha tej postaci. Obierz innego patrona.", "patron " + b.id() + " niezgodny z rasą " + rasa);
    long teraz = System.currentTimeMillis();
    if (d.laskaDo > teraz) return new Wynik(false, "&7Łaska " + b.imie() + " wciąż trwa (" + Panteon.czas(d.laskaDo - teraz) + ").", "łaska trwa");
    zdejmijMody(p);
    d.laskaWiara = wiara(p);
    d.laskaBostwo = b.id();
    d.laskaDo = teraz + czasLaskiMs;
    if (!zapisz(p, s)) return new Wynik(false, "&cNie udało się zapisać łaski — zgłoś to administracji.", "błąd zapisu");
    nalozLaske(p, s);
    p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 1.0f);
    p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1.2, 0), 24, 0.4, 0.6, 0.4, 0.02);
    return new Wynik(true, "&fŁaska " + b.kolor() + b.imie() + "&f: " + panteon.opisLaski(b, d.laskaWiara)
        + " &7(" + Panteon.czas(czasLaskiMs) + ", Wiara " + d.laskaWiara + ")", "łaska " + b.id() + " wiara " + d.laskaWiara);
  }

  /** Admin/test: czyści wiarę bieżącej postaci. */
  Wynik reset(Player p) {
    Stan s = aktywne.get(p.getUniqueId());
    if (s == null) return new Wynik(false, "", "brak aktywnej postaci");
    zdejmijMody(p);
    s.dane.patron = null;
    s.dane.zmiana = 0;
    s.dane.laskaDo = 0;
    s.dane.laskaBostwo = null;
    s.dane.laskaWiara = 0;
    return zapisz(p, s) ? new Wynik(true, "", "wyczyszczono") : new Wynik(false, "", "błąd zapisu");
  }

  private boolean zapisz(Player p, Stan s) {
    try {
      magazyn.zapisz(s.profil, p.getName(), s.dane);
      return true;
    } catch (IOException e) {
      getLogger().severe("Zapis wiary " + p.getName() + " (" + s.profil + "): " + e);
      return false;
    }
  }

  public void wyslij(Player p, String tekst) {
    if (tekst == null || tekst.isEmpty()) return;
    p.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&6✦ " + tekst));
  }
}
