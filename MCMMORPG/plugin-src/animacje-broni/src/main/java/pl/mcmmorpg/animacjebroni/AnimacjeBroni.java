package pl.mcmmorpg.animacjebroni;

import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.comp.interaction.InteractionType;
import io.lumine.mythic.lib.damage.DamageType;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.AttackRange;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.datacomponent.item.SwingAnimation;
import io.papermc.paper.datacomponent.item.UseEffects;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.event.PlayerResourceUpdateEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Input;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * "Feeling" broni MMOItems wg typu: zamach (swing_animation), model trzymania (item_model), minimalne naladowanie
 * ciosu, zasieg, zestaw wloczni (pchniecie w linii + szarza), garda na PPM (blocks_attacks) oraz efekt i dzwiek
 * trafienia wg rodzaju obrazen (ciete / klute / obuchowe).
 *
 * Komponenty licza sie tylko w dloni, wiec bron jest poprawiana, gdy trafia do reki (wejscie, zmiana slotu, zamiana
 * rak, klik w ekwipunku, podniesienie) i przegladem obu rak co 10 tickow. ItemBuildEvent MMOItems 6.10 jest martwy.
 * PPM (garda, szarza) tylko przy PUSTEJ lewej rece i bez umiejetnosci MMOItems na PPM - inaczej PPM nalezy do
 * zaklecia/tarczy w lewej rece albo do umiejetnosci.
 */
public final class AnimacjeBroni extends JavaPlugin implements Listener {

  enum Rodzaj { CIETE, KLUTE, OBUCHOWE, BRAK }

  record Garda(float redukcja, float kat, float opoznienie, float spowolnienie) {}

  /** Wypad (kontra): garda >= min tickow, puszczenie PPM i LPM w ciagu okno tickow. Odnowienie tylko wypadu, nie bloku. */
  record Wypad(int min, double mnoznik, double zasieg, double szerokosc, double sila, int odnowienie, int okno) {}
  record Parowanie(int okno, int ripostaTicki, double ripostaMnoznik) {}
  record Uderzenie(int min, double promien, double mnoznik, int odnowienie) {}
  record Ukrycie(double plecy, double ukrycie) {}
  record Krwawienie(double procent, int czas, int stosy) {}
  record Riposta(int doTicka, double mnoznik) {}
  record Ciezkie(String przod, String bok, String tyl, String bieg, double koszt) {}
  record RytmStan(int trafienia, int ostatniTick, String typ) {}
  record StosKrwawienia(double obrazenia, UUID gracz) {}
  static final class Krwawiacy {
    final List<StosKrwawienia> stosy = new java.util.ArrayList<>();
    int doTicka;
    int nastepnyTick;
  }

  record Ustawienie(SwingAnimation.Animation zamach, int czas, String skala, Rodzaj rodzaj, Float ladowanie,
                    Float zasieg, Material wzor, Garda garda, boolean ciezka, Wypad wypad,
                    ItemUseAnimation poza, String reakcja, Parowanie parowanie, Uderzenie uderzenie,
                    Ukrycie ukrycie, Krwawienie krwawienie, double pancerz, Ciezkie ciezkie, boolean rytm) {}

  /** Komponenty, ktore plugin ustawia; przy zmianie wracaja do wartosci materialu (resetData). */
  private static final List<DataComponentType> ZARZADZANE = List.of(
      DataComponentTypes.SWING_ANIMATION, DataComponentTypes.MINIMUM_ATTACK_CHARGE, DataComponentTypes.ATTACK_RANGE,
      DataComponentTypes.PIERCING_WEAPON, DataComponentTypes.KINETIC_WEAPON, DataComponentTypes.USE_EFFECTS,
      DataComponentTypes.BLOCKS_ATTACKS, DataComponentTypes.CONSUMABLE);
  private static final Set<DataComponentType> Z_WZORU_LPM = Set.of(
      DataComponentTypes.PIERCING_WEAPON, DataComponentTypes.ATTACK_RANGE, DataComponentTypes.MINIMUM_ATTACK_CHARGE);
  private static final Set<DataComponentType> Z_WZORU_PPM = Set.of(
      DataComponentTypes.KINETIC_WEAPON, DataComponentTypes.USE_EFFECTS);

  private final Map<String, Ustawienie> typy = new HashMap<>();
  private final Map<UUID, Integer> oknoKontry = new HashMap<>();     // do ktorego ticka LPM = wypad
  private final Map<UUID, Integer> odnowienieWypadu = new HashMap<>(); // do ktorego ticka wypad niedostepny
  private final Map<UUID, Integer> tlumPchniecie = new HashMap<>();    // do ktorego ticka gasimy zwykly cios (wypad go zastepuje)
  private final Map<UUID, Riposta> riposty = new HashMap<>();
  private final Map<UUID, Integer> odnowienieUderzenia = new HashMap<>();
  private final Map<UUID, Krwawiacy> krwawiacy = new HashMap<>();
  private final Map<UUID, RytmStan> serie = new HashMap<>();
  private final Map<UUID, UUID> podwojKrwawienie = new HashMap<>();
  private final Map<UUID, Integer> naladowanyZamach = new HashMap<>();
  private final Map<UUID, Integer> ostatniZamach = new HashMap<>();
  /** Cel prawdziwego ciosu gracza (PrePlayerAttackEntityEvent) i tick - odroznia cios od rozprysku MMOItems/MythicLib,
   *  ktory tez przychodzi jako ENTITY_ATTACK od gracza (zmierzone: 1 klik = 3 zdarzenia przy 3 mobach obok siebie). */
  private final Map<UUID, UUID> celCiosu = new HashMap<>();
  private final Map<UUID, Integer> tickCiosu = new HashMap<>();
  private final Set<UUID> sygnalCiezkiego = new HashSet<>();
  private final Set<UUID> wykonczenieZamach = new HashSet<>();
  private final Map<UUID, Runnable> oczekujacyZamach = new HashMap<>();
  private boolean mmocore;
  private double progRytmu, kosztRytmu, mnoznikRytmu, lukRytmu;
  private int czekanieCiezkiego, oknoRytmu, dlugoscRytmu;
  private boolean wlasnyCios;
  private boolean debug;
  private void dbg(String m) { if (debug) getLogger().info("DBG t=" + getServer().getCurrentTick() + " " + m); }
  private final Set<Material> modele = new HashSet<>();
  private String przestrzen = "bron";
  private int wersja;
  private NamespacedKey kluczStanu;
  private NamespacedKey kluczZamachu;

  @Override
  public void onEnable() {
    kluczStanu = new NamespacedKey(this, "stan");
    kluczZamachu = new NamespacedKey(this, "zamach_do_ticka");
    mmocore = getServer().getPluginManager().isPluginEnabled("MMOCore");
    if (!mmocore) getLogger().warning("Brak MMOCore: dodatkowe koszty staminy sa wylaczone.");
    saveDefaultConfig();
    wczytaj();
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getScheduler().runTaskTimer(this,
        () -> getServer().getOnlinePlayers().forEach(this::dopasujDlonie), 10L, 10L);
    getServer().getScheduler().runTaskTimer(this, this::tyknijKrwawienie, 1L, 1L);
    getServer().getScheduler().runTaskTimer(this, this::sygnalizujCiezkie, 2L, 2L);
  }

  private static Float liczbaLubNull(ConfigurationSection t, String k) {
    return t.contains(k) ? (float) t.getDouble(k) : null;
  }

  private void wczytaj() {
    reloadConfig();
    typy.clear();
    modele.clear();
    przestrzen = getConfig().getString("przestrzen-modeli", "bron");
    debug = getConfig().getBoolean("debug", false);
    czekanieCiezkiego = Math.max(0, getConfig().getInt("ciezkie.czekanie", 10));
    progRytmu = getConfig().getDouble("rytm.prog_rytmu", 0.9);
    oknoRytmu = getConfig().getInt("rytm.okno_rytmu", 8);
    kosztRytmu = getConfig().getDouble("rytm.koszt_rytmu", 2);
    dlugoscRytmu = Math.max(2, getConfig().getInt("rytm.dlugosc", 3));
    mnoznikRytmu = getConfig().getDouble("rytm.mnoznik", 1.4);
    lukRytmu = getConfig().getDouble("rytm.luk_mnoznik", 0.5);
    ConfigurationSection sek = getConfig().getConfigurationSection("typy");
    if (sek != null) {
      for (String typ : sek.getKeys(false)) {
        ConfigurationSection t = sek.getConfigurationSection(typ);
        if (t == null) continue;
        SwingAnimation.Animation zamach = null;
        try {
          if (t.getString("zamach") != null) zamach = SwingAnimation.Animation.valueOf(t.getString("zamach").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
          getLogger().warning("Typ " + typ + ": nieznany zamach '" + t.getString("zamach") + "' (whack|stab|none)");
        }
        Rodzaj rodzaj = Rodzaj.BRAK;
        try {
          rodzaj = Rodzaj.valueOf(t.getString("rodzaj", "brak").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
          getLogger().warning("Typ " + typ + ": nieznany rodzaj '" + t.getString("rodzaj") + "' (ciete|klute|obuchowe|brak)");
        }
        Material wzor = t.getString("wzor") == null ? null : Material.matchMaterial(t.getString("wzor"));
        if (t.getString("wzor") != null && wzor == null) getLogger().warning("Typ " + typ + ": nieznany wzor " + t.getString("wzor"));
        Garda garda = null;
        ConfigurationSection g = t.getConfigurationSection("garda");
        if (g != null) {
          garda = new Garda((float) g.getDouble("redukcja", 0.4), (float) g.getDouble("kat", 90),
              (float) g.getDouble("opoznienie", 0.15), (float) g.getDouble("spowolnienie", 0.5));
        }
        Wypad wypad = null;
        ConfigurationSection w = t.getConfigurationSection("wypad");
        if (w != null) {
          wypad = new Wypad(w.getInt("min", 10), w.getDouble("mnoznik", 1.5), w.getDouble("zasieg", 4.5),
              w.getDouble("szerokosc", 0.9), w.getDouble("sila", 0.8), w.getInt("odnowienie", 60), w.getInt("okno", 10));
        }
        ConfigurationSection par = t.getConfigurationSection("parowanie");
        Parowanie parowanie = par == null ? null : new Parowanie(par.getInt("okno", 6),
            par.getInt("riposta_ticki", 30), par.getDouble("riposta_mnoznik", 1.5));
        ConfigurationSection ud = t.getConfigurationSection("uderzenie");
        Uderzenie uderzenie = ud == null ? null : new Uderzenie(ud.getInt("min", 16),
            ud.getDouble("promien", 3.5), ud.getDouble("mnoznik", 1.6), ud.getInt("odnowienie", 120));
        ConfigurationSection uk = t.getConfigurationSection("z_ukrycia");
        Ukrycie ukrycie = uk == null ? null : new Ukrycie(uk.getDouble("plecy", 2), uk.getDouble("ukrycie", 3));
        ConfigurationSection kr = t.getConfigurationSection("krwawienie");
        Krwawienie krwawienie = kr == null ? null : new Krwawienie(kr.getDouble("procent", 0.25),
            kr.getInt("czas", 60), kr.getInt("stosy", 1));
        ConfigurationSection ci = t.getConfigurationSection("ciezkie");
        Ciezkie ciezkie = ci == null ? null : new Ciezkie(ci.getString("przod"), ci.getString("bok"),
            ci.getString("tyl"), ci.getString("bieg"), ci.getDouble("koszt", 12));
        ItemUseAnimation poza = null;
        if (t.getString("poza") != null) {
          try {
            poza = (ItemUseAnimation) ItemUseAnimation.class.getField(t.getString("poza").toUpperCase(Locale.ROOT)).get(null);
          } catch (ReflectiveOperationException ex) {
            getLogger().warning("Typ " + typ + ": nieznana poza '" + t.getString("poza") + "' (bow|crossbow|block|spear|trident|...)");
          }
        }
        typy.put(typ.toUpperCase(Locale.ROOT), new Ustawienie(zamach, t.getInt("czas", 6), t.getString("skala"), rodzaj,
            liczbaLubNull(t, "ladowanie"), liczbaLubNull(t, "zasieg"), wzor, garda, t.getBoolean("ciezka", false), wypad,
            poza, t.getString("reakcja", "brak"), parowanie, uderzenie, ukrycie, krwawienie,
            t.getDouble("pancerz.procent", 0.30), ciezkie, t.getBoolean("rytm", false)));
      }
    }
    for (String m : getConfig().getStringList("materialy-z-modelami")) {
      Material mat = Material.matchMaterial(m);
      if (mat == null) getLogger().warning("Nieznany material w materialy-z-modelami: " + m);
      else modele.add(mat);
    }
    wersja = getConfig().saveToString().hashCode();
    riposty.clear();
    krwawiacy.clear();
    serie.clear();
    podwojKrwawienie.clear();
    naladowanyZamach.clear();
    ostatniZamach.clear();
    celCiosu.clear();
    tickCiosu.clear();
    sygnalCiezkiego.clear();
    wykonczenieZamach.clear();
    oczekujacyZamach.clear();
    getLogger().info("Typy broni: " + typy.size() + ", materialy z modelami trzymania: " + modele.size());
  }

  private Ustawienie ustawienie(ItemStack item) {
    if (item == null || item.getType().isAir()) return null;
    String typ = NBTItem.get(item).getType();
    return typ == null ? null : typy.get(typ.toUpperCase(Locale.ROOT));
  }

  private static boolean maUmiejetnoscPpm(ItemStack item) {
    String ab = NBTItem.get(item).getString("MMOITEMS_ABILITY");
    return ab != null && ab.contains("RIGHT_CLICK");
  }

  /** Zwraca true, gdy cos zmieniono. Przedmioty spoza MMOItems i typy spoza configu zostaja nietkniete. */
  boolean dopasuj(ItemStack item, boolean lewaPusta, boolean sprint) {
    Ustawienie u = ustawienie(item);
    if (u == null) return false;
    boolean ppm = lewaPusta && !maUmiejetnoscPpm(item);
    boolean dwaTryby = u.wzor() != null && u.garda() != null;
    boolean trybSprintu = dwaTryby && sprint;
    boolean szarza = ppm && trybSprintu;
    Integer zamachDo = item.getPersistentDataContainer().get(kluczZamachu, PersistentDataType.INTEGER);
    boolean odwrotnyZamach = zamachDo != null && zamachDo > getServer().getCurrentTick();
    String stan = wersja + ":" + ppm + (dwaTryby ? ":" + sprint : "") + ":" + odwrotnyZamach;
    if (stan.equals(item.getPersistentDataContainer().get(kluczStanu, PersistentDataType.STRING))) return false;

    for (DataComponentType t : ZARZADZANE) item.resetData(t);

    if (u.wzor() != null) {
      ItemStack wzor = new ItemStack(u.wzor());
      item.copyDataFrom(wzor, t -> Z_WZORU_LPM.contains(t)
          || (ppm && (!dwaTryby || sprint) && Z_WZORU_PPM.contains(t)));
    }
    if (u.zamach() != null) {
      SwingAnimation.Animation zamach = odwrotnyZamach
          ? (u.zamach() == SwingAnimation.Animation.STAB ? SwingAnimation.Animation.WHACK : SwingAnimation.Animation.STAB)
          : u.zamach();
      item.setData(DataComponentTypes.SWING_ANIMATION,
          SwingAnimation.swingAnimation().type(zamach).duration(u.czas()).build());
    }
    if (u.ladowanie() != null) item.setData(DataComponentTypes.MINIMUM_ATTACK_CHARGE, u.ladowanie());
    if (u.zasieg() != null) {
      AttackRange wz = u.wzor() != null ? item.getData(DataComponentTypes.ATTACK_RANGE) : null;
      item.setData(DataComponentTypes.ATTACK_RANGE, AttackRange.attackRange()
          .minReach(wz != null ? wz.minReach() : 0f).maxReach(u.zasieg())
          .minCreativeReach(0f).maxCreativeReach(Math.max(5f, u.zasieg())).hitboxMargin(0.3f).mobFactor(1f).build());
    }
    if (ppm && u.garda() != null && !szarza) {
      Garda g = u.garda();
      item.setData(DataComponentTypes.BLOCKS_ATTACKS, BlocksAttacks.blocksAttacks()
          .blockDelaySeconds(g.opoznienie())
          .addDamageReduction(DamageReduction.damageReduction()
              .horizontalBlockingAngle(g.kat()).base(0f).factor(g.redukcja()).build())
          .blockSound(Key.key("minecraft", "item.shield.block"))
          .disableSound(Key.key("minecraft", "item.shield.break"))
          .build());
      item.setData(DataComponentTypes.USE_EFFECTS, UseEffects.useEffects()
          .canSprint(false).speedMultiplier(g.spowolnienie()).interactVibrations(true).build());
    }

    if (ppm && u.uderzenie() != null) {
      item.setData(DataComponentTypes.USE_EFFECTS, UseEffects.useEffects()
          .canSprint(false).speedMultiplier(0.4f).interactVibrations(true).build());
    }

    if (ppm && !szarza && u.poza() != null) {
      // tylko wyglad trzymania PPM: "jedzenie" trwa godzine, bez dzwieku i czasteczek (jak nosniki zaklec)
      item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().consumeSeconds(3600f).animation(u.poza())
          .hasConsumeParticles(false).sound(Key.key("minecraft", "intentionally_empty")).build());
    }

    if (u.skala() != null && modele.contains(item.getType())) {
      String skala = trybSprintu && u.skala().endsWith("g")
          ? u.skala().substring(0, u.skala().length() - 1) : u.skala();
      Key cel = Key.key(przestrzen, skala + "/" + item.getType().getKey().getKey());
      Key jest = item.getData(DataComponentTypes.ITEM_MODEL);
      // podmieniamy tylko model domyslny materialu albo nasz wlasny - model z Nexo/MMOItems zostaje
      boolean domyslny = jest == null || jest.equals(item.getType().getKey()) || jest.namespace().equals(przestrzen);
      if (domyslny) item.setData(DataComponentTypes.ITEM_MODEL, cel);
    }
    item.editPersistentDataContainer(pdc -> pdc.set(kluczStanu, PersistentDataType.STRING, stan));
    if (zamachDo != null && !odwrotnyZamach) item.editPersistentDataContainer(pdc -> pdc.remove(kluczZamachu));
    return true;
  }

  private void dopasujDlonie(Player p) {
    if (!p.isOnline()) return;
    PlayerInventory inv = p.getInventory();
    ItemStack druga = inv.getItemInOffHand();
    boolean lewaPusta = druga.getType().isAir();
    ItemStack glowna = inv.getItemInMainHand();
    if (dopasuj(glowna, lewaPusta, p.isSprinting())) inv.setItemInMainHand(glowna);
    if (dopasuj(druga, false, false)) inv.setItemInOffHand(druga);
  }

  private void pozniej(HumanEntity kto) {
    // zwykly scheduler Bukkita: harmonogram encji (p.getScheduler()) Paper wylacza po smierci gracza
    if (kto instanceof Player p) getServer().getScheduler().runTask(this, () -> dopasujDlonie(p));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void wejscie(PlayerJoinEvent e) { pozniej(e.getPlayer()); }

  @EventHandler(priority = EventPriority.MONITOR)
  public void wyjscie(PlayerQuitEvent e) {
    UUID id = e.getPlayer().getUniqueId();
    serie.remove(id);
    podwojKrwawienie.remove(id);
    naladowanyZamach.remove(id);
    ostatniZamach.remove(id);
    sygnalCiezkiego.remove(id);
    celCiosu.remove(id);
    tickCiosu.remove(id);
    wykonczenieZamach.remove(id);
    oczekujacyZamach.remove(id);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void slot(PlayerItemHeldEvent e) {
    wyzerujCiezkie(e.getPlayer());
    pozniej(e.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void zamiana(PlayerSwapHandItemsEvent e) {
    wyzerujCiezkie(e.getPlayer());
    pozniej(e.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void sprint(PlayerToggleSprintEvent e) { pozniej(e.getPlayer()); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void klik(InventoryClickEvent e) { pozniej(e.getWhoClicked()); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void przeciagniecie(InventoryDragEvent e) { pozniej(e.getWhoClicked()); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void podniesienie(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player p) pozniej(p); }

  /**
   * Wypad (kontra): po gardzie trzymanej >= min tickow puszczenie PPM otwiera okno; LPM w tym oknie = krok do przodu
   * i pchniecie przez linie patrzenia (wszystkie cele do zasieg blokow) za mnoznik x obrazenia broni, przez MythicLib
   * (statystyki, PvP, party). Klient nie wysyla LPM, dopoki trzymany jest PPM - stad "pusc prawy, kliknij lewy".
   * Blok nie ma odnowienia; odnowienie ma tylko wypad (w tym czasie LPM to zwykle pchniecie).
   */
  @EventHandler(priority = EventPriority.MONITOR)
  public void puszczeniePpm(PlayerStopUsingItemEvent e) {
    Player p = e.getPlayer();
    ItemStack item = p.getInventory().getItemInMainHand();
    Ustawienie u = ustawienie(item);
    if (u != null && u.uderzenie() != null && e.getItem().hasData(DataComponentTypes.CONSUMABLE)
        && item.isSimilar(e.getItem())) {
      int teraz = getServer().getCurrentTick();
      if (e.getTicksHeldFor() >= u.uderzenie().min()
          && odnowienieUderzenia.getOrDefault(p.getUniqueId(), 0) <= teraz) {
        odnowienieUderzenia.put(p.getUniqueId(), teraz + u.uderzenie().odnowienie());
        uderzenie(p, u.uderzenie());
      }
      return;
    }
    if (u == null || u.wypad() == null || e.getTicksHeldFor() < u.wypad().min()
        || !e.getItem().hasData(DataComponentTypes.BLOCKS_ATTACKS) || !item.isSimilar(e.getItem())) return;
    int teraz = getServer().getCurrentTick();
    if (odnowienieWypadu.getOrDefault(p.getUniqueId(), 0) > teraz) return;
    oknoKontry.put(p.getUniqueId(), teraz + u.wypad().okno());
    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.PLAYERS, 0.5f, 1.8f);   // sygnal: teraz LPM
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void lpm(PlayerAnimationEvent e) {
    if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
    Player p = e.getPlayer();
    ItemStack bron = p.getInventory().getItemInMainHand();
    Ustawienie smuga = ustawienie(bron);
    if (smuga != null && smuga.zamach() != null && smuga.rodzaj() != Rodzaj.BRAK
        && (p.getAttackCooldown() >= 0.9f || naladowanyZamach.getOrDefault(p.getUniqueId(), -1) >= getServer().getCurrentTick() - 1))
      smuga(p, smuga);
    Runnable nastepny = oczekujacyZamach.remove(p.getUniqueId());
    if (nastepny != null) getServer().getScheduler().runTask(this, nastepny);
    if (wykonczenieZamach.remove(p.getUniqueId()))
      getServer().getScheduler().runTask(this, () -> przywrocZamach(p));
    int teraz = getServer().getCurrentTick();
    if (smuga != null) wyzerujCiezkie(p);
    Integer okno = oknoKontry.remove(p.getUniqueId());
    if (okno == null || okno < teraz) return;
    Ustawienie u = ustawienie(p.getInventory().getItemInMainHand());
    if (u == null || u.wypad() == null) return;
    odnowienieWypadu.put(p.getUniqueId(), teraz + u.wypad().odnowienie());
    tlumPchniecie.put(p.getUniqueId(), teraz + 3);
    wypad(p, u.wypad());
  }

  /** Zwykle pchniecie LPM, ktore zastapil wypad, nie zadaje obrazen (bez tego cel dostalby dwa razy). */
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void zgasPchniecie(EntityDamageByEntityEvent e) {
    if (wlasnyCios || !(e.getDamager() instanceof Player p)) return;
    Integer do_ = tlumPchniecie.get(p.getUniqueId());
    if (do_ != null && do_ >= getServer().getCurrentTick()) e.setCancelled(true);
  }

  private void wypad(Player p, Wypad w) {
    Vector kier = p.getLocation().getDirection().setY(0);
    if (kier.lengthSquared() > 1e-4) p.setVelocity(p.getVelocity().add(kier.normalize().multiply(w.sila()).setY(0.1)));
    World swiat = p.getWorld();
    swiat.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.8f, 0.8f);
    getServer().getScheduler().runTaskLater(this, () -> {
      if (!p.isOnline() || p.isDead()) return;
      Location oko = p.getEyeLocation();
      Vector d = oko.getDirection().normalize();
      for (double t = 0.6; t <= w.zasieg(); t += 0.35) {
        swiat.spawnParticle(Particle.CRIT, oko.clone().add(d.clone().multiply(t)), 1, 0.02, 0.02, 0.02, 0);
      }
      double obrazenia = MMOPlayerData.get(p).getStatMap().getStat("ATTACK_DAMAGE") * w.mnoznik();
      var meta = MMOPlayerData.get(p).getStatMap().cache(io.lumine.mythic.lib.api.player.EquipmentSlot.MAIN_HAND);
      int trafione = 0;
      for (var enc : swiat.getNearbyEntities(oko, w.zasieg() + 1, w.zasieg() + 1, w.zasieg() + 1)) {
        if (!(enc instanceof LivingEntity cel) || cel.equals(p) || cel.isDead()) continue;
        Vector doCelu = cel.getLocation().add(0, cel.getHeight() / 2, 0).toVector().subtract(oko.toVector());
        double wzdluz = doCelu.dot(d);
        if (wzdluz < 0.3 || wzdluz > w.zasieg()) continue;
        double obok = doCelu.clone().subtract(d.clone().multiply(wzdluz)).length();
        if (obok > w.szerokosc() + cel.getWidth() / 2) continue;
        if (!UtilityMethods.canTarget(p, cel, InteractionType.OFFENSE_ACTION)) continue;
        cel.setNoDamageTicks(0);
        wlasnyCios = true;
        try {
          meta.attack(cel, obrazenia, DamageType.WEAPON, DamageType.PHYSICAL);
        } finally {
          wlasnyCios = false;
        }
        Location c = cel.getLocation().add(0, cel.getHeight() * 0.6, 0);
        swiat.spawnParticle(Particle.CRIT, c, 8, 0.15, 0.15, 0.15, 0.2);
        trafione++;
      }
      swiat.playSound(oko, trafione > 0 ? Sound.ITEM_TRIDENT_HIT : Sound.ENTITY_PLAYER_ATTACK_NODAMAGE,
          SoundCategory.PLAYERS, 0.9f, trafione > 0 ? 0.9f : 1f);
    }, 2L);
  }

  private void uderzenie(Player p, Uderzenie u) {
    World swiat = p.getWorld();
    Location srodek = p.getLocation();
    Block pod = srodek.clone().subtract(0, 0.2, 0).getBlock();
    for (int i = 0; i < 32; i++) {
      double kat = i * Math.PI * 2 / 32;
      Location punkt = srodek.clone().add(Math.cos(kat) * u.promien(), 0.12, Math.sin(kat) * u.promien());
      if (!pod.getType().isAir()) swiat.spawnParticle(Particle.BLOCK, punkt, 3, 0.12, 0.05, 0.12, 0.1, pod.getBlockData());
    }
    swiat.spawnParticle(Particle.EXPLOSION, srodek.clone().add(0, 0.2, 0), 3, 0.6, 0.1, 0.6, 0);
    swiat.playSound(srodek, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, SoundCategory.PLAYERS, 1f, 0.8f);
    double obrazenia = MMOPlayerData.get(p).getStatMap().getStat("ATTACK_DAMAGE") * u.mnoznik();
    var meta = MMOPlayerData.get(p).getStatMap().cache(io.lumine.mythic.lib.api.player.EquipmentSlot.MAIN_HAND);
    for (var enc : swiat.getNearbyEntities(srodek, u.promien(), u.promien(), u.promien())) {
      if (!(enc instanceof LivingEntity cel) || cel.equals(p) || cel.isDead()
          || cel.getLocation().distanceSquared(srodek) > u.promien() * u.promien()
          || !UtilityMethods.canTarget(p, cel, InteractionType.OFFENSE_ACTION)) continue;
      cel.setNoDamageTicks(0);
      wlasnyCios = true;
      try {
        meta.attack(cel, obrazenia, DamageType.WEAPON, DamageType.PHYSICAL);
      } finally {
        wlasnyCios = false;
      }
      zachwiej(cel, p.getLocation());
    }
  }

  private void zachwiej(LivingEntity cel, Location skad) {
    cel.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 2, false, true, true));
    cel.playHurtAnimation(0);
    Vector od = cel.getLocation().toVector().subtract(skad.toVector()).setY(0);
    if (od.lengthSquared() > 1e-4) cel.setVelocity(cel.getVelocity().add(od.normalize().multiply(0.35).setY(0.1)));
  }


  /** Parowanie zachowuje vanillowa garde; tylko cios z przodu w pierwszych tickach odbija calosc. */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void parowanie(EntityDamageByEntityEvent e) {
    if (e.getEntity() instanceof Player pp) dbg("PAROWANIE? blokuje=" + pp.isBlocking() + " uzyte=" + pp.getActiveItemUsedTime()
        + " przyczyna=" + e.getCause() + " napastnik=" + e.getDamager().getType());
    if (!(e.getEntity() instanceof Player p) || !(e.getDamager() instanceof LivingEntity napastnik)
        || !p.isBlocking() || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
    ItemStack bron = p.getInventory().getItemInMainHand();
    Ustawienie u = ustawienie(bron);
    if (u == null || u.parowanie() == null || u.garda() == null || !"krytyk".equals(u.reakcja())
        || !bron.hasData(DataComponentTypes.BLOCKS_ATTACKS) || !p.getInventory().getItemInOffHand().getType().isAir()) return;
    Vector doNapastnika = napastnik.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
    Vector patrzy = p.getLocation().getDirection().setY(0);
    if (doNapastnika.lengthSquared() < 1e-4 || patrzy.lengthSquared() < 1e-4
        || Math.toDegrees(doNapastnika.angle(patrzy)) > u.garda().kat()
        // okno liczone od chwili, gdy garda zaczyna dzialac (opoznienie bloku), nie od wcisniecia PPM -
        // inaczej miecz dwureczny (opoznienie 0,3 s = 6 tickow) mialby okno ~1 tick
        || p.getActiveItemUsedTime() > Math.round(u.garda().opoznienie() * 20) + u.parowanie().okno()) return;
    e.setDamage(0);
    riposty.put(p.getUniqueId(), new Riposta(getServer().getCurrentTick() + u.parowanie().ripostaTicki(),
        u.parowanie().ripostaMnoznik()));
    zachwiej(napastnik, p.getLocation());
    Location styk = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(0.8));
    p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, styk, 28, 0.2, 0.2, 0.2, 0.3);
    p.getWorld().playSound(styk, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.8f, 1.5f);
  }

  private boolean pobierzStamine(Player p, double procent) {
    if (!mmocore || procent <= 0) return true;
    double koszt = Math.max(0, MMOPlayerData.get(p).getStatMap().getStat("MAX_STAMINA")) * procent / 100.0;
    PlayerData dane = PlayerData.get(p);
    if (dane == null || dane.getStamina() + 1e-6 < koszt) return false;
    dane.giveStamina(-koszt, PlayerResourceUpdateEvent.UpdateReason.SKILL_COST);
    return true;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void zapiszCelCiosu(PrePlayerAttackEntityEvent e) {
    if (!e.willAttack()) return;
    celCiosu.put(e.getPlayer().getUniqueId(), e.getAttacked().getUniqueId());
    tickCiosu.put(e.getPlayer().getUniqueId(), getServer().getCurrentTick());
  }

  /** Czy to zdarzenie to cios gracza w cel, ktory zaatakowal (a nie rozprysk na sasiada w tym samym ticku). */
  private boolean prawdziwyCios(Player p, LivingEntity cel) {
    Integer tick = tickCiosu.get(p.getUniqueId());
    return tick != null && tick == getServer().getCurrentTick() && cel.getUniqueId().equals(celCiosu.get(p.getUniqueId()));
  }

  private int pelneNaladowanie(Player p) {
    double szybkosc = p.getAttribute(Attribute.ATTACK_SPEED) == null ? 4
        : p.getAttribute(Attribute.ATTACK_SPEED).getValue();
    return (int) Math.ceil(20.0 / Math.max(0.1, szybkosc));
  }

  private int tickiDoCiezkiego(Player p) {
    int teraz = getServer().getCurrentTick();
    int ostatni = ostatniZamach.computeIfAbsent(p.getUniqueId(), id -> teraz);
    return Math.max(0, ostatni + pelneNaladowanie(p) + czekanieCiezkiego - teraz);
  }

  private void wyzerujCiezkie(Player p) {
    ostatniZamach.put(p.getUniqueId(), getServer().getCurrentTick());
    sygnalCiezkiego.remove(p.getUniqueId());
  }

  private void sygnalizujCiezkie() {
    for (Player p : getServer().getOnlinePlayers()) {
      UUID id = p.getUniqueId();
      Ustawienie u = ustawienie(p.getInventory().getItemInMainHand());
      if (u == null || u.ciezkie() == null) {
        ostatniZamach.remove(id);
        sygnalCiezkiego.remove(id);
        continue;
      }
      if (tickiDoCiezkiego(p) > 0 || !sygnalCiezkiego.add(id)) continue;
      p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 0.25f, 1.6f);
      Vector przod = p.getEyeLocation().getDirection().setY(0);
      if (przod.lengthSquared() < 1e-4) przod = new Vector(1, 0, 0);
      przod.normalize();
      Vector prawo = new Vector(-przod.getZ(), 0, przod.getX());
      Location dlon = p.getEyeLocation().add(przod.multiply(0.45)).add(prawo.multiply(0.28)).subtract(0, 0.4, 0);
      p.getWorld().spawnParticle(Particle.ENCHANTED_HIT, dlon, 3, 0.08, 0.08, 0.08, 0.01);
    }
  }

  private String kierunekCiezkiego(Player p, Ciezkie c) {
    Input wejscie = p.getCurrentInput();
    if (p.isSprinting() && c.bieg() != null) return c.bieg();
    if (wejscie.isForward() && c.przod() != null) return c.przod();
    if ((wejscie.isLeft() || wejscie.isRight()) && c.bok() != null) return c.bok();
    if (wejscie.isBackward() && c.tyl() != null) return c.tyl();
    return c.przod();
  }

  private void odrzuc(LivingEntity cel, Player p, double sila) {
    Vector d = cel.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
    if (d.lengthSquared() > 1e-4) {
      d.normalize().multiply(sila).setY(Math.max(cel.getVelocity().getY(), 0.18));
      cel.setVelocity(d);
    }
  }

  private void dodatkowyCios(Player p, LivingEntity cel, double obrazenia) {
    if (!UtilityMethods.canTarget(p, cel, InteractionType.OFFENSE_ACTION)) return;
    var meta = MMOPlayerData.get(p).getStatMap().cache(io.lumine.mythic.lib.api.player.EquipmentSlot.MAIN_HAND);
    cel.setNoDamageTicks(0);
    wlasnyCios = true;
    try { meta.attack(cel, obrazenia, DamageType.WEAPON, DamageType.PHYSICAL); }
    finally { wlasnyCios = false; }
  }

  private double zasieg(Ustawienie u) { return u.zasieg() == null ? 3.0 : u.zasieg(); }

  private void luk(Player p, LivingEntity pierwotny, Ustawienie u, double obrazenia, double kat) {
    Location oko = p.getEyeLocation();
    Vector patrzy = oko.getDirection().setY(0);
    if (patrzy.lengthSquared() < 1e-4) return;
    patrzy.normalize();
    double zasieg = zasieg(u);
    for (var enc : p.getWorld().getNearbyEntities(p.getLocation(), zasieg + 1, 2.5, zasieg + 1)) {
      if (!(enc instanceof LivingEntity cel) || cel.equals(p) || cel.equals(pierwotny) || cel.isDead()) continue;
      Vector d = cel.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
      if (d.lengthSquared() > (zasieg + cel.getWidth() / 2) * (zasieg + cel.getWidth() / 2)
          || d.lengthSquared() < 1e-4 || Math.toDegrees(patrzy.angle(d)) > kat / 2) continue;
      dodatkowyCios(p, cel, obrazenia);
    }
    Vector bok = new Vector(-patrzy.getZ(), 0, patrzy.getX());
    for (int i = 0; i < 12; i++) {
      double a = Math.toRadians(-kat / 2 + kat * i / 11);
      Vector d = patrzy.clone().multiply(Math.cos(a)).add(bok.clone().multiply(Math.sin(a)));
      p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, oko.clone().add(d.multiply(Math.min(zasieg, 2.2))).subtract(0, 0.5, 0),
          1, 0, 0, 0, 0);
    }
  }

  private void linia(Player p, LivingEntity pierwotny, Ustawienie u, double obrazenia) {
    Location oko = p.getEyeLocation();
    Vector patrzy = oko.getDirection().normalize();
    double poczatek = pierwotny.getLocation().add(0, pierwotny.getHeight() / 2, 0).toVector()
        .subtract(oko.toVector()).dot(patrzy);
    double koniec = Math.max(zasieg(u), poczatek) + 2.5;
    List<LivingEntity> cele = new ArrayList<>();
    for (var enc : p.getWorld().getNearbyEntities(oko, koniec, koniec, koniec)) {
      if (!(enc instanceof LivingEntity cel) || cel.equals(p) || cel.equals(pierwotny) || cel.isDead()) continue;
      Vector d = cel.getLocation().add(0, cel.getHeight() / 2, 0).toVector().subtract(oko.toVector());
      double t = d.dot(patrzy);
      if (t <= poczatek + 0.2 || t > koniec
          || d.subtract(patrzy.clone().multiply(t)).length() > 0.75 + cel.getWidth() / 2) continue;
      cele.add(cel);
    }
    cele.sort(Comparator.comparingDouble(c -> c.getLocation().toVector().subtract(oko.toVector()).dot(patrzy)));
    for (int i = 0; i < Math.min(2, cele.size()); i++) dodatkowyCios(p, cele.get(i), obrazenia);
  }

  private void ciezkiEfekt(Player p, LivingEntity cel, Ustawienie u, String wariant, double baza) {
    String sciezka = "ciezkie_warianty." + wariant + ".";
    switch (wariant) {
      case "wypad" -> {
        Vector d = p.getLocation().getDirection().setY(0);
        if (d.lengthSquared() > 1e-4) p.setVelocity(p.getVelocity().add(d.normalize().multiply(getConfig().getDouble(sciezka + "sila", 0.45))));
        if (u.rodzaj() == Rodzaj.KLUTE) linia(p, cel, u, baza * getConfig().getDouble(sciezka + "linia_mnoznik", 0.6));
      }
      case "znad_glowy" -> {
        zachwiej(cel, p.getLocation());
        Block pod = cel.getLocation().subtract(0, 0.2, 0).getBlock();
        if (!pod.getType().isAir()) p.getWorld().spawnParticle(Particle.BLOCK,
            pod.getLocation().add(0.5, 1, 0.5), 4, 0.2, 0, 0.2, 0, pod.getBlockData());
      }
      case "zamach_boczny" -> luk(p, cel, u, baza * getConfig().getDouble(sciezka + "luk_mnoznik", 0.7),
          getConfig().getDouble(sciezka + ("KOSA".equals(NBTItem.get(p.getInventory().getItemInMainHand()).getType()) ? "kat_kosa" : "kat"), 120));
      case "odepchniecie" -> { zachwiej(cel, p.getLocation()); odrzuc(cel, p, getConfig().getDouble(sciezka + "sila", 1.1)); }
      case "z_rozbiegu" -> { odrzuc(cel, p, getConfig().getDouble(sciezka + "sila", 1.1)); p.setSprinting(false); }
      default -> { }
    }
  }

  private void odwrocZamach(Player p, ItemStack bron, int naTickow) {
    ItemStack wzor = bron.clone();
    // Damage poprzedza ARM_SWING: czekamy na zamach drugiego ciosu, potem ustawiamy animacje trzeciego.
    Runnable ustaw = () -> {
      if (!p.isOnline()) return;
      ItemStack trzymany = p.getInventory().getItemInMainHand();
      if (!trzymany.isSimilar(wzor)) return;
      trzymany.editPersistentDataContainer(pdc -> pdc.set(kluczZamachu, PersistentDataType.INTEGER,
          getServer().getCurrentTick() + naTickow));
      trzymany.editPersistentDataContainer(pdc -> pdc.remove(kluczStanu));
      p.getInventory().setItemInMainHand(trzymany);
      dopasujDlonie(p);
      getServer().getScheduler().runTaskLater(this, () -> dopasujDlonie(p), naTickow + 1L);
    };
    oczekujacyZamach.put(p.getUniqueId(), ustaw);
    getServer().getScheduler().runTaskLater(this, () -> {
      Runnable awaryjnie = oczekujacyZamach.remove(p.getUniqueId());
      if (awaryjnie != null) awaryjnie.run();
    }, 4L);
  }

  private void przywrocZamach(Player p) {
    oczekujacyZamach.remove(p.getUniqueId());
    if (!p.isOnline()) return;
    ItemStack bron = p.getInventory().getItemInMainHand();
    if (!bron.getPersistentDataContainer().has(kluczZamachu, PersistentDataType.INTEGER)) return;
    bron.editPersistentDataContainer(pdc -> {
      pdc.remove(kluczZamachu);
      pdc.remove(kluczStanu);
    });
    p.getInventory().setItemInMainHand(bron);
    dopasujDlonie(p);
  }

  private void rytm(Player p, LivingEntity cel, ItemStack bron, Ustawienie u, EntityDamageByEntityEvent e,
                    int teraz, float naladowanie) {
    String typ = NBTItem.get(bron).getType();
    RytmStan poprzedni = serie.get(p.getUniqueId());
    int pelne = pelneNaladowanie(p);
    boolean wOknie = poprzedni != null && typ.equals(poprzedni.typ())
        && teraz >= poprzedni.ostatniTick() + pelne
        && teraz <= poprzedni.ostatniTick() + pelne + oknoRytmu;
    int numer = naladowanie >= progRytmu && wOknie ? poprzedni.trafienia() + 1 : 1;
    if (numer > 1 && !pobierzStamine(p, kosztRytmu)) numer = 1;
    if (numer == 1 && poprzedni != null && poprzedni.trafienia() == dlugoscRytmu - 1) przywrocZamach(p);
    dbg("RYTM numer=" + numer + " pelne=" + pelne + " okno=" + wOknie + " naladowanie=" + naladowanie);
    float ton = numer >= dlugoscRytmu ? 1.6f : numer == 1 ? 0.8f : numer == 2 ? 1.0f : 1.3f;
    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS,
        numer >= dlugoscRytmu ? 0.9f : 0.3f, ton);
    if (numer >= dlugoscRytmu) {
      double baza = e.getDamage();
      e.setDamage(baza * mnoznikRytmu);
      switch (u.rodzaj()) {
        case CIETE -> {
          if (u.krwawienie() != null) podwojKrwawienie.put(p.getUniqueId(), cel.getUniqueId());
          else luk(p, cel, u, baza * lukRytmu, 120);
        }
        case KLUTE -> linia(p, cel, u, baza * getConfig().getDouble("ciezkie_warianty.wypad.linia_mnoznik", 0.6));
        case OBUCHOWE -> { zachwiej(cel, p.getLocation()); odrzuc(cel, p, 0.6); }
        default -> { }
      }
      serie.remove(p.getUniqueId());
      oczekujacyZamach.remove(p.getUniqueId());
      wykonczenieZamach.add(p.getUniqueId());
      getServer().getScheduler().runTaskLater(this, () -> {
        if (wykonczenieZamach.remove(p.getUniqueId())) przywrocZamach(p);
      }, 4L);
    } else {
      serie.put(p.getUniqueId(), new RytmStan(numer, teraz, typ));
      if (numer == dlugoscRytmu - 1 && u.zamach() != null) odwrocZamach(p, bron, oknoRytmu + pelne);
    }
  }

  private void smuga(Player p, Ustawienie u) {
    Location oko = p.getEyeLocation();
    Vector przod = oko.getDirection().normalize();
    Vector bok = new Vector(-przod.getZ(), 0, przod.getX());
    boolean klute = u.rodzaj() == Rodzaj.KLUTE;
    int punkty = klute ? 8 : 12;
    for (int i = 0; i < punkty; i++) {
      double t = (double) i / (punkty - 1);
      Location punkt = klute ? oko.clone().add(przod.clone().multiply(0.5 + t * 2.0))
          : oko.clone().add(przod.clone().multiply(1.4)).add(bok.clone().multiply(0.8 - 1.6 * t)).add(0, 0.65 - 1.3 * t, 0);
      Runnable pokaz = () -> {
        if (!p.isOnline()) return;
        if (klute) p.getWorld().spawnParticle(Particle.CRIT, punkt, 1, 0, 0, 0, 0);
        else p.getWorld().spawnParticle(Particle.DUST, punkt, 1, 0, 0, 0, 0,
            new Particle.DustOptions(u.rodzaj() == Rodzaj.OBUCHOWE ? Color.fromRGB(116, 83, 52) : Color.fromRGB(210, 216, 220), 0.8f));
      };
      if (i < punkty / 2) pokaz.run();
      else getServer().getScheduler().runTaskLater(this, pokaz, 1L);
    }
    if (u.rodzaj() == Rodzaj.CIETE) p.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
        oko.clone().add(przod.multiply(1.8)), 1, 0, 0, 0, 0);
    if (u.rodzaj() == Rodzaj.OBUCHOWE) {
      Block pod = p.getLocation().subtract(0, 0.2, 0).getBlock();
      if (!pod.getType().isAir()) p.getWorld().spawnParticle(Particle.BLOCK, pod.getLocation().add(0.5, 1, 0.5),
          3, 0.2, 0, 0.2, 0, pod.getBlockData());
    }
  }

  /** Ciezki cios, rytm i reakcje trafienia bronia w glownej rece. HIGHEST: MythicLib liczy obrazenia broni wczesniej
   *  i nadpisalby zmiane z nizszego priorytetu (zmierzone: na HIGH mnoznik ciosu w plecy znikal). */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void reakcja(EntityDamageByEntityEvent e) {
    if (wlasnyCios || !(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity cel)
        || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || !prawdziwyCios(p, cel)) return;
    ItemStack bron = p.getInventory().getItemInMainHand();
    Ustawienie u = ustawienie(bron);
    if (u == null) return;
    int teraz = getServer().getCurrentTick();
    boolean ciezkiGotowy = u.ciezkie() != null && tickiDoCiezkiego(p) == 0;
    wyzerujCiezkie(p); // trafienie poprzedza ARM_SWING; decyzja uzywa poprzedniego zamachu
    podwojKrwawienie.remove(p.getUniqueId());
    // riposta zuzywa sie przy trafieniu (klient wysyla cios PRZED machnieciem, wiec nie przez PlayerAnimationEvent)
    Riposta riposta = u.parowanie() != null ? riposty.remove(p.getUniqueId()) : null;
    if (riposta != null && riposta.doTicka() >= teraz) {
      e.setDamage(e.getDamage() * riposta.mnoznik());
      cel.getWorld().spawnParticle(Particle.CRIT, cel.getLocation().add(0, cel.getHeight() * 0.6, 0), 12, 0.2, 0.2, 0.2, 0.1);
      cel.getWorld().playSound(cel.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.6f, 1.2f);
    }
    // naladowanie z chwili ciosu: Paper resetuje je (PlayerAttackEntityCooldownResetEvent) dopiero PO tym zdarzeniu - zmierzone
    float naladowanie = p.getAttackCooldown();
    dbg("REAKCJA " + u.reakcja() + " cel=" + cel.getType() + " naladowanie=" + naladowanie + " dmg=" + e.getDamage());
    if (naladowanie >= 0.9f) naladowanyZamach.put(p.getUniqueId(), teraz);
    String wariant = ciezkiGotowy ? kierunekCiezkiego(p, u.ciezkie()) : null;
    if (wariant != null && getConfig().isConfigurationSection("ciezkie_warianty." + wariant)) {
      if (pobierzStamine(p, u.ciezkie().koszt())) {
        double baza = e.getDamage();
        e.setDamage(baza * getConfig().getDouble("ciezkie_warianty." + wariant + ".mnoznik", 1));
        serie.remove(p.getUniqueId());
        przywrocZamach(p);
        ciezkiEfekt(p, cel, u, wariant, baza);
        dbg("CIEZKI " + wariant + " dmg=" + e.getDamage());
      } else {
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 0.2f, 0.7f);
        if (u.rytm()) rytm(p, cel, bron, u, e, teraz, naladowanie);
      }
    } else if (u.rytm()) rytm(p, cel, bron, u, e, teraz, naladowanie);
    if (naladowanie < 0.9f) return;
    switch (u.reakcja()) {
      case "pancerz" -> {
        double procent = Math.max(0, Math.min(1, u.pancerz()));
        double przed = e.getFinalDamage();
        if (e.isApplicable(EntityDamageEvent.DamageModifier.ARMOR)) {
          double oslabienie = e.getDamage(EntityDamageEvent.DamageModifier.ARMOR);
          e.setDamage(EntityDamageEvent.DamageModifier.ARMOR, oslabienie * (1 - procent));
        }
        if (e.getFinalDamage() <= przed + 1e-6) {
          double roznica = Math.max(0, e.getDamage() - przed);
          e.setDamage(e.getDamage() + roznica * procent);
        }
      }
      case "z_ukrycia" -> {
        if (u.ukrycie() == null) return;
        Vector patrzy = cel.getLocation().getDirection().setY(0);
        Vector doGracza = p.getLocation().toVector().subtract(cel.getLocation().toVector()).setY(0);
        if (patrzy.lengthSquared() < 1e-4 || doGracza.lengthSquared() < 1e-4
            || Math.toDegrees(patrzy.angle(doGracza)) <= 120) return;
        boolean ukryty = p.isSneaking() && (!(cel instanceof Mob mob) || !p.equals(mob.getTarget()));
        e.setDamage(e.getDamage() * (ukryty ? u.ukrycie().ukrycie() : u.ukrycie().plecy()));
        Location punkt = cel.getLocation().add(0, cel.getHeight() * 0.6, 0);
        cel.getWorld().spawnParticle(Particle.ENCHANTED_HIT, punkt, 8, 0.15, 0.15, 0.15, 0.05);
        cel.getWorld().playSound(punkt, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.3f, 1.4f);
      }
      default -> { }
    }
    dbg("REAKCJA po: dmg=" + e.getDamage() + " final=" + e.getFinalDamage());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void uruchomKrwawienie(EntityDamageByEntityEvent e) {
    if (wlasnyCios || !(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity cel)
        || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || e.getFinalDamage() <= 0 || !prawdziwyCios(p, cel)) return;
    Ustawienie u = ustawienie(p.getInventory().getItemInMainHand());
    if (u == null || !"krwawienie".equals(u.reakcja()) || u.krwawienie() == null || p.getAttackCooldown() < 0.9f) return;
    Krwawienie k = u.krwawienie();
    int teraz = getServer().getCurrentTick();
    Krwawiacy stan = krwawiacy.computeIfAbsent(cel.getUniqueId(), id -> new Krwawiacy());
    boolean podwoj = cel.getUniqueId().equals(podwojKrwawienie.remove(p.getUniqueId()));
    int nowe = podwoj ? 2 : 1;
    int limit = Math.max(k.stosy(), nowe);
    while (stan.stosy.size() > Math.max(0, limit - nowe)) stan.stosy.remove(0);
    for (int i = 0; i < nowe; i++) stan.stosy.add(new StosKrwawienia(
        e.getFinalDamage() * k.procent() / Math.max(1, k.czas() / 20), p.getUniqueId()));
    stan.doTicka = teraz + k.czas();
    if (stan.nastepnyTick <= teraz) stan.nastepnyTick = teraz + 20;
  }

  private void tyknijKrwawienie() {
    int teraz = getServer().getCurrentTick();
    var it = krwawiacy.entrySet().iterator();
    while (it.hasNext()) {
      var wpis = it.next();
      var enc = getServer().getEntity(wpis.getKey());
      Krwawiacy stan = wpis.getValue();
      if (!(enc instanceof LivingEntity cel) || cel.isDead() || teraz > stan.doTicka) { it.remove(); continue; }
      if (teraz < stan.nastepnyTick) continue;
      stan.nastepnyTick = teraz + 20;
      for (StosKrwawienia stos : stan.stosy) {
        Player p = getServer().getPlayer(stos.gracz());
        if (p == null || !p.isOnline() || !UtilityMethods.canTarget(p, cel, InteractionType.OFFENSE_ACTION)) continue;
        var meta = MMOPlayerData.get(p).getStatMap().cache(io.lumine.mythic.lib.api.player.EquipmentSlot.MAIN_HAND);
        cel.setNoDamageTicks(0);
        wlasnyCios = true;
        try { meta.attack(cel, stos.obrazenia(), DamageType.DOT); }
        finally { wlasnyCios = false; }
      }
      cel.getWorld().spawnParticle(Particle.DUST, cel.getLocation().add(0, cel.getHeight() * 0.6, 0),
          8, 0.2, 0.2, 0.2, new Particle.DustOptions(org.bukkit.Color.fromRGB(95, 12, 18), 1.1f));
    }
  }

  /** Widoczny blok: iskry w miejscu styku, gdy gracz zablokowal cios bronia z garda (dzwiek daje sam komponent). */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void zablokowano(EntityDamageByEntityEvent e) {
    if (!(e.getEntity() instanceof Player p) || !p.isBlocking()) return;
    Vector doNapastnika = e.getDamager().getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
    Vector patrzy = p.getLocation().getDirection().setY(0);
    if (doNapastnika.lengthSquared() < 1e-4 || patrzy.lengthSquared() < 1e-4) return;
    Ustawienie u = ustawienie(p.getInventory().getItemInMainHand());
    if (u == null || u.garda() == null) return;
    if (Math.toDegrees(doNapastnika.angle(patrzy)) > u.garda().kat()) return;   // cios z boku/tylu nie jest blokowany
    Location oko = p.getEyeLocation();
    Location styk = oko.clone().add(oko.getDirection().multiply(0.8)).subtract(0, 0.35, 0);
    p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, styk, 12, 0.15, 0.15, 0.15, 0.25);
    p.getWorld().spawnParticle(Particle.CRIT, styk, 6, 0.1, 0.1, 0.1, 0.3);
    p.getWorld().playSound(styk, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.25f, 1.8f);
  }

  /** Efekt i dzwiek trafienia wg rodzaju obrazen broni w glownej rece. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void trafienie(EntityDamageByEntityEvent e) {
    if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity cel)) return;
    if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || e.getFinalDamage() <= 0) return;
    Ustawienie u = ustawienie(p.getInventory().getItemInMainHand());
    if (u == null || u.rodzaj() == Rodzaj.BRAK) return;
    World w = cel.getWorld();
    Location srodek = cel.getLocation().add(0, cel.getHeight() * 0.6, 0);
    float ton = u.ciezka() ? 0.75f : 1.1f;
    switch (u.rodzaj()) {
      case CIETE -> {
        w.spawnParticle(Particle.SWEEP_ATTACK, srodek, 1, 0, 0, 0, 0);
        w.playSound(srodek, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.6f, ton);
      }
      case KLUTE -> {
        Vector kier = srodek.toVector().subtract(p.getEyeLocation().toVector()).normalize();
        for (int i = 0; i < 6; i++) {
          w.spawnParticle(Particle.CRIT, srodek.clone().add(kier.clone().multiply(0.25 * i)), 2, 0.03, 0.03, 0.03, 0.02);
        }
        w.playSound(srodek, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.5f, u.ciezka() ? 1.0f : 1.4f);
      }
      case OBUCHOWE -> {
        Block pod = cel.getLocation().subtract(0, 0.2, 0).getBlock();
        if (!pod.getType().isAir()) {
          w.spawnParticle(Particle.BLOCK, cel.getLocation().add(0, 0.1, 0), u.ciezka() ? 30 : 12,
              0.45, 0.05, 0.45, 0.1, pod.getBlockData());
        }
        w.spawnParticle(Particle.CRIT, srodek, u.ciezka() ? 10 : 5, 0.2, 0.2, 0.2, 0.1);
        w.playSound(srodek, u.ciezka() ? Sound.ITEM_MACE_SMASH_GROUND : Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
            SoundCategory.PLAYERS, u.ciezka() ? 0.45f : 0.7f, ton);
      }
      default -> { }
    }
  }

  @Override
  public boolean onCommand(CommandSender s, Command c, String label, String[] args) {
    if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
      wczytaj();
      s.sendMessage("AnimacjeBroni: przeladowano (" + typy.size() + " typow).");
      return true;
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("sprawdz")) {
      Player p = getServer().getPlayerExact(args[1]);
      if (p == null) { s.sendMessage("Brak gracza " + args[1]); return true; }
      ItemStack i = p.getInventory().getItemInMainHand();
      String typ = i.getType().isAir() ? null : NBTItem.get(i).getType();
      Ustawienie u = ustawienie(i);
      int ciezkiZa = u == null || u.ciezkie() == null ? -1 : tickiDoCiezkiego(p);
      s.sendMessage("typ=" + typ + " swing=" + i.getData(DataComponentTypes.SWING_ANIMATION)
          + " model=" + i.getData(DataComponentTypes.ITEM_MODEL)
          + " ladowanie=" + i.getData(DataComponentTypes.MINIMUM_ATTACK_CHARGE)
          + " zasieg=" + i.getData(DataComponentTypes.ATTACK_RANGE)
          + " pierce=" + i.hasData(DataComponentTypes.PIERCING_WEAPON)
          + " szarza=" + i.hasData(DataComponentTypes.KINETIC_WEAPON)
          + " garda=" + i.hasData(DataComponentTypes.BLOCKS_ATTACKS)
          + " reakcja=" + (u == null ? "brak" : u.reakcja())
          + " ciezkiGotowy=" + (ciezkiZa == 0)
          + " ciezkiZa=" + ciezkiZa
          + " wypadZa=" + Math.max(0, odnowienieWypadu.getOrDefault(p.getUniqueId(), 0) - getServer().getCurrentTick())
          + " uderzenieZa=" + Math.max(0, odnowienieUderzenia.getOrDefault(p.getUniqueId(), 0) - getServer().getCurrentTick())
          + " ripostaZa=" + Math.max(0, riposty.getOrDefault(p.getUniqueId(), new Riposta(0, 1)).doTicka() - getServer().getCurrentTick())
          + " sprint=" + p.isSprinting()
          + " stamina=" + (mmocore ? PlayerData.get(p).getStamina() + "/"
              + MMOPlayerData.get(p).getStatMap().getStat("MAX_STAMINA") : "bez MMOCore")
          + " lewa=" + p.getInventory().getItemInOffHand().getType());
      return true;
    }
    return false;
  }
}
