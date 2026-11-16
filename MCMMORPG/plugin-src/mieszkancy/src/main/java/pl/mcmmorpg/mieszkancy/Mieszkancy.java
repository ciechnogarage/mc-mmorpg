package pl.mcmmorpg.mieszkancy;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Mieszkańcy Stolicy: NPC (manekin + hitbox + napis + rozmowa), przechodnie, zwierzęta, napisy, towary,
 * dźwięki i cząsteczki. Wszystkie encje są NIEPRZETRWAŁE (setPersistent(false)) i odtwarzane z YAML, gdy ich chunk
 * jest wczytany — nic nie trafia do plików świata, a {@code /mieszkancy przeladuj} stawia wszystko od nowa.
 */
public final class Mieszkancy extends JavaPlugin implements Listener {
  private static final MiniMessage MM = MiniMessage.miniMessage();

  public NamespacedKey kluczNpc;
  /** Każda nasza encja (także dymki, napisy, zwierzęta) — sprzątanie po przeładowaniu/awarii. */
  public NamespacedKey kluczSmiec;
  public boolean debug;
  public String swiatNazwa;
  public Location srodek;
  public double promienAktywny;
  /** Ktoś jest w Stolicy (liczone co sekundę) — bez tego żadna pętla nic nie robi. */
  public volatile boolean aktywne;

  public Okna okna;
  Hud hud;
  public Dymki dymki;
  public Npcs npcs;
  public Zycie zycie;
  private final Map<String, ResolvableProfile> skorki = new HashMap<>();

  public static Component mm(String s) { return MM.deserialize(s == null ? "" : s); }

  @Override
  public void onEnable() {
    kluczNpc = new NamespacedKey(this, "npc");
    kluczSmiec = new NamespacedKey(this, "encja");
    saveDefaultConfig();
    if (!new File(getDataFolder(), "npc.yml").exists()) saveResource("npc.yml", false);
    hud = new Hud(this);
    okna = new Okna(this);
    dymki = new Dymki(this);
    npcs = new Npcs(this);
    zycie = new Zycie(this);
    Bukkit.getPluginManager().registerEvents(this, this);
    Bukkit.getPluginManager().registerEvents(okna, this);
    Bukkit.getPluginManager().registerEvents(npcs, this);
    Bukkit.getPluginManager().registerEvents(zycie, this);
    wczytaj();
    Okna.rozgrzej();
    // Co sekundę: czy ktoś jest w Stolicy + dostawienie brakujących encji w wczytanych chunkach.
    Bukkit.getScheduler().runTaskTimer(this, this::sekunda, 40L, 20L);
    Bukkit.getScheduler().runTaskTimer(this, () -> { if (aktywne) { npcs.tick(); zycie.tick(); } }, 41L, 1L);
  }

  @Override
  public void onDisable() {
    for (Player p : Bukkit.getOnlinePlayers()) { if (hud.schowany(p)) { p.closeDialog(); hud.przywroc(p); } }
    dymki.usunWszystkie();
    npcs.usunWszystkie();
    zycie.usunWszystkie();
  }

  void wczytaj() {
    reloadConfig();
    var c = getConfig();
    debug = c.getBoolean("debug", false);
    swiatNazwa = c.getString("swiat", "stolica_odnowa");
    List<Double> s = c.getDoubleList("srodek");
    World w = Bukkit.getWorld(swiatNazwa);
    srodek = w == null ? null : new Location(w, s.size() > 0 ? s.get(0) : 0, s.size() > 1 ? s.get(1) : 101, s.size() > 2 ? s.get(2) : 0);
    promienAktywny = c.getDouble("promien-aktywny", 120);
    if (w == null) getLogger().warning("Świat '" + swiatNazwa + "' nie istnieje — mieszkańcy nie wstaną, dopóki go nie będzie.");
    wczytajSkorki(c.getString("skorki-plik", "../CharacterCreator/skins.yml"));
    npcs.wczytaj(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "npc.yml")));
    zycie.wczytaj(c);
    if (w != null) sprzatnijSwiat(w);
  }

  private void wczytajSkorki(String sciezka) {
    skorki.clear();
    File f = new File(getDataFolder(), sciezka);
    if (!f.isFile()) { getLogger().warning("Brak pliku skórek " + f + " — NPC dostaną domyślną skórkę."); return; }
    ConfigurationSection sec = YamlConfiguration.loadConfiguration(f).getConfigurationSection("skins");
    if (sec == null) return;
    for (String k : sec.getKeys(false)) {
      String v = sec.getString(k + ".value"), sig = sec.getString(k + ".signature");
      if (v == null || sig == null) continue;
      skorki.put(k, ResolvableProfile.resolvableProfile()
          .name("npc_" + Math.abs(k.hashCode() % 100000))
          .uuid(UUID.nameUUIDFromBytes(("mieszkancy:" + k).getBytes()))
          .addProperty(new ProfileProperty("textures", v, sig)).build());
    }
    getLogger().info("Skórki: " + skorki.size() + " z " + f.getName());
  }

  public ResolvableProfile skorka(String id) {
    if (id == null) return null;
    ResolvableProfile p = skorki.get(id);
    if (p == null) getLogger().warning("Nieznana skórka '" + id + "' (klucze z CharacterCreator/skins.yml).");
    return p;
  }

  public World swiat() { return Bukkit.getWorld(swiatNazwa); }

  private void sekunda() {
    World w = swiat();
    boolean a = false;
    if (w != null && srodek != null) {
      double r2 = promienAktywny * promienAktywny;
      for (Player p : w.getPlayers()) if (p.getLocation().distanceSquared(srodek) < r2) { a = true; break; }
    }
    aktywne = a;
    for (Player p : Bukkit.getOnlinePlayers()) okna.pilnuj(p);
    if (w == null) return;
    npcs.dostaw(w);
    zycie.dostaw(w);
  }

  /** Wyszedł w trakcie rozmowy (albo serwer padł): HUD wraca po wejściu. 40 ticków = MythicHUD zdąży zbudować HudHolder
   *  z PDC, a HudDane minie swoją zwłokę 1,5 s. */
  @EventHandler
  public void wejscie(PlayerJoinEvent e) {
    Player p = e.getPlayer();
    if (!hud.schowany(p)) return;
    Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline()) hud.przywroc(p); }, 40L);
  }

  /** Stare villagery Stolicy v2 (MythicMobs NoAI) i nasze resztki — usuwane przy wczytaniu chunka. */
  @EventHandler
  public void wczytanieEncji(EntitiesLoadEvent e) {
    if (!e.getWorld().getName().equals(swiatNazwa)) return;
    for (Entity x : e.getEntities()) sprzatnij(x);
  }

  private void sprzatnijSwiat(World w) {
    int n = 0;
    for (Entity x : w.getEntities()) if (sprzatnij(x)) n++;
    if (n > 0) getLogger().info("Sprzątnięto " + n + " encji (stare villagery NPC / resztki).");
  }

  private boolean sprzatnij(Entity x) {
    if (x.getPersistentDataContainer().has(kluczSmiec, PersistentDataType.BYTE) && x.isPersistent()) { x.remove(); return true; }
    if (getConfig().getBoolean("usun-stare-villagery", true) && x instanceof Villager v && !v.hasAI()) {
      // Stolica v2: 30 villagerów MM z NoAI (28 spawnerów + 2 nadmiarowe) z zapisanymi ofertami handlu.
      getLogger().info("Usuwam stary villager NPC " + v.getName() + " @ " + v.getLocation().getBlockX() + "," + v.getLocation().getBlockY() + "," + v.getLocation().getBlockZ());
      v.remove();
      return true;
    }
    return false;
  }

  // ---------- wspólne narzędzia ----------

  /** [x,y,z] lub [x,y,z,yaw] w świecie Stolicy. */
  public Location miejsce(List<?> l) {
    World w = swiat();
    if (w == null || l == null || l.size() < 3) return null;
    Location loc = new Location(w, num(l.get(0)), num(l.get(1)), num(l.get(2)));
    if (l.size() > 3) loc.setYaw((float) num(l.get(3)));
    return loc;
  }

  static double num(Object o) { return o instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(o)); }

  /** Wysokość, na której się stoi w kolumnie (x,z): pierwszy blok z kolizją od dołu z dwoma wolnymi nad nim. */
  public static double grunt(World w, double x, double z, int odY, int doY) {
    int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
    for (int y = odY; y <= doY; y++) {
      Block b = w.getBlockAt(bx, y, bz);
      if (!b.isCollidable()) continue;
      if (w.getBlockAt(bx, y + 1, bz).isCollidable() || w.getBlockAt(bx, y + 2, bz).isCollidable()) continue;
      double top = b.getBoundingBox().getMaxY();
      return top > y ? top : y + 1;
    }
    return Double.NaN;
  }

  /** Stopy NPC na podłożu blisko podanego Y (±3): naprawa „lewitowania” villagerów Y o blok za wysoko. */
  public static Location naZiemi(Location l) {
    double g = grunt(l.getWorld(), l.getX(), l.getZ(), l.getBlockY() - 3, l.getBlockY() + 1);
    Location r = l.clone();
    if (!Double.isNaN(g)) r.setY(g);
    return r;
  }

  /** MATERIAL albo MATERIAL#RRGGBB (barwiona skóra). */
  public ItemStack przedmiot(String s) {
    if (s == null || s.isBlank()) return null;
    String[] cz = s.split("#", 2);
    Material mat = Material.matchMaterial(cz[0].trim());
    if (mat == null) { getLogger().warning("Nieznany przedmiot '" + s + "'"); return null; }
    ItemStack it = new ItemStack(mat);
    if (cz.length > 1 && it.getItemMeta() instanceof LeatherArmorMeta lm) {
      lm.setColor(Color.fromRGB(Integer.parseInt(cz[1].trim(), 16)));
      it.setItemMeta(lm);
    }
    return it;
  }

  /** Dźwięk sprawdzony w rejestrze (literówka = ostrzeżenie przy wczytaniu, nie cisza w grze). */
  public String dzwiek(String klucz, String gdzie) {
    if (klucz == null || klucz.isBlank()) return null;
    String k = klucz.toLowerCase(Locale.ROOT);
    NamespacedKey nk = NamespacedKey.fromString(k);
    if (nk == null || Registry.SOUNDS.get(nk) == null) { getLogger().warning("Nieznany dźwięk '" + klucz + "' (" + gdzie + ")"); return null; }
    return k;
  }

  public Particle czastka(String nazwa, String gdzie) {
    if (nazwa == null || nazwa.isBlank()) return null;
    try { return Particle.valueOf(nazwa.toUpperCase(Locale.ROOT)); }
    catch (IllegalArgumentException e) { getLogger().warning("Nieznana cząsteczka '" + nazwa + "' (" + gdzie + ")"); return null; }
  }

  /** Dane cząsteczki wymagane przez typ (DUST → kolor, BLOCK → blok, ITEM → przedmiot). */
  public Object daneCzastki(Particle p, ConfigurationSection c) {
    if (p == null) return null;
    Class<?> t = p.getDataType();
    if (t == Void.class) return null;
    if (t == Particle.DustOptions.class) {
      return new Particle.DustOptions(Color.fromRGB(Integer.parseInt(c.getString("kolor", "FFD27F").replace("#", ""), 16)), (float) c.getDouble("rozmiar", 1.0));
    }
    if (t == org.bukkit.block.data.BlockData.class) {
      Material m = Material.matchMaterial(c.getString("blok", "STONE"));
      return (m == null ? Material.STONE : m).createBlockData();
    }
    if (t == ItemStack.class) return przedmiot(c.getString("przedmiot", "STONE"));
    if (t == Color.class) return Color.fromRGB(Integer.parseInt(c.getString("kolor", "FFFFFF").replace("#", ""), 16));
    getLogger().warning("Cząsteczka " + p + " wymaga danych " + t.getSimpleName() + " — pominięta.");
    return null;
  }

  public void konsola(String komenda, Player p) {
    String k = komenda.replace("%gracz%", p.getName());
    if (debug) getLogger().info("[debug] konsola: " + k);
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), k);
  }

  // ---------- komendy ----------

  @Override
  public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
    String sub = a.length == 0 ? "stan" : a[0].toLowerCase(Locale.ROOT);
    switch (sub) {
      case "przeladuj" -> {
        npcs.usunWszystkie(); zycie.usunWszystkie(); dymki.usunWszystkie();
        wczytaj();
        s.sendMessage(mm("<green>Mieszkańcy przeładowani: " + npcs.ile() + " NPC, " + zycie.opis()));
      }
      case "stan" -> s.sendMessage(mm("<gold>Mieszkańcy: <white>" + npcs.ile() + " NPC (" + npcs.zywych() + " stoi), " + zycie.opis()
          + "<gray> · aktywne=" + aktywne));
      case "prowadz" -> {
        // /mieszkancy prowadz <npc> <gracz> — dla menu Q: usługa jest u NPC, Q tylko wskazuje drogę.
        if (a.length < 3) { s.sendMessage("/mieszkancy prowadz <npc> <gracz>"); return true; }
        Player p = Bukkit.getPlayerExact(a[2]);
        if (p == null) { s.sendMessage("Gracz offline: " + a[2]); return true; }
        if (!npcs.prowadz(p, a[1])) s.sendMessage("Nieznany NPC: " + a[1]);
      }
      case "rozmowa" -> {
        if (a.length < 3) { s.sendMessage("/mieszkancy rozmowa <npc> <gracz>"); return true; }
        Player p = Bukkit.getPlayerExact(a[2]);
        if (p == null) { s.sendMessage("Gracz offline: " + a[2]); return true; }
        if (!npcs.rozmowa(p, a[1])) s.sendMessage("Nieznany NPC: " + a[1]);
      }
      case "lista" -> {
        List<String> l = new ArrayList<>(npcs.ids());
        s.sendMessage("NPC: " + String.join(", ", l));
      }
      default -> s.sendMessage("/mieszkancy stan|przeladuj|lista|prowadz <npc> <gracz>|rozmowa <npc> <gracz>");
    }
    return true;
  }
}
