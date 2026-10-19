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
import org.bukkit.event.player.PlayerInputEvent;
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
 * ciosu, zasieg, zestaw wloczni (pchniecie w linii + szarza), garda na Shift+PPM (blocks_attacks),
 * mocny cios po puszczeniu PPM oraz efekt i dzwiek
 * trafienia wg rodzaju obrazen (ciete / klute / obuchowe).
 *
 * Komponenty licza sie tylko w dloni, wiec bron jest poprawiana, gdy trafia do reki (wejscie, zmiana slotu, zamiana
 * rak, klik w ekwipunku, podniesienie) i przegladem obu rak co 10 tickow. ItemBuildEvent MMOItems 6.10 jest martwy.
 * PPM (mocny cios, garda, szarza) tylko przy PUSTEJ lewej rece i bez umiejetnosci MMOItems na PPM - inaczej PPM nalezy do
 * zaklecia/tarczy w lewej rece albo do umiejetnosci.
 */
public final class AnimacjeBroni extends JavaPlugin implements Listener {

  enum Rodzaj { CIETE, KLUTE, OBUCHOWE, BRAK }

  record Garda(float redukcja, float kat, float opoznienie, float spowolnienie) {}

  record Parowanie(int okno, int ripostaTicki, double ripostaMnoznik) {}
  record Uderzenie(double promien, double mnoznik) {}
  record Ukrycie(double plecy, double ukrycie) {}
  record Krwawienie(double procent, int czas, int stosy) {}
  record Riposta(int doTicka, double mnoznik) {}
  record Ciezkie(String przod, String bok, String tyl, String bieg, String spadanie, String brak, double koszt, int pelne) {}
  record AtakPchniecie(Set<String> gdy, SwingAnimation.Animation zamach, Rodzaj rodzaj) {}
  record OstatniAtak(UUID cel, int tick, Rodzaj rodzaj, int ogniwo) {}
  record RytmStan(int trafienia, int ostatniTick, String typ) {}
  record StosKrwawienia(double obrazenia, UUID gracz) {}
  static final class Krwawiacy {
    final List<StosKrwawienia> stosy = new java.util.ArrayList<>();
    int doTicka;
    int nastepnyTick;
  }

  record Ustawienie(SwingAnimation.Animation zamach, int czas, String skala, Rodzaj rodzaj, Float ladowanie,
                    Float zasieg, Material wzor, Garda garda, boolean ciezka, boolean drzewce,
                    ItemUseAnimation poza, String reakcja, Parowanie parowanie, Uderzenie uderzenie,
                    Ukrycie ukrycie, Krwawienie krwawienie, double pancerz, Ciezkie ciezkie, boolean rytm,
                    AtakPchniecie atakPchniecie, ItemUseAnimation pozaMocnego, List<Double> kroki,
                    double progRytmu, int oknoRytmu, LukLekki lukLekki) {}

  /** Kosa: kazdy lekki cios tnie lukiem (kat w stopniach, zasieg broni) - boczne cele dostaja mnoznik obrazen. */
  record LukLekki(double kat, double mnoznik) {}


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
  /** Waga typu broni = mnoznik kosztu staminy (typy.<typ>.waga, domyslnie 1). */
  private final Map<String, Double> wagi = new HashMap<>();
  private final Map<UUID, Riposta> riposty = new HashMap<>();
  private final Map<UUID, Integer> odnowienieMocnego = new HashMap<>();
  private final Set<UUID> sygnalMocnego = new HashSet<>();
  private final Map<UUID, Integer> ostatniBrakStaminy = new HashMap<>();
  private final Set<UUID> programowyZamach = new HashSet<>();
  private final Map<UUID, Integer> komboStart = new HashMap<>(); // ile lekkich ciosow serii bylo, gdy zaczelo sie ladowanie PPM
  private final Map<UUID, Input> nowyInput = new HashMap<>(); // wejscie z PlayerInputEvent (getCurrentInput jeszcze stary)
  /** Sprint w chwili wcisniecia PPM (w trakcie ladowania sprint jest wylaczony, a z_rozbiegu ma byc osiagalny). */
  private final Set<UUID> sprintPrzyLadowaniu = new HashSet<>();
  /** Tick ostatniego machniecia - naladowanie lekkiego ciosu do kosztu staminy liczone niezaleznie od licznika serwera. */
  private final Map<UUID, Integer> ostatnieMachniecie = new HashMap<>();
  /** Gracze kopiacy blok (LPM trzymany na bloku): klient macha w petli, to nie sa ciosy (19.10: 62 "ciosy" i pusty pasek w 2 s). */
  private final Set<UUID> kopie = new HashSet<>();
  private final Map<UUID, Integer> tickProgramowegoZamachu = new HashMap<>(); // echo zamachu z API przychodzi pozniej
  private final Map<UUID, Krwawiacy> krwawiacy = new HashMap<>();
  private final Map<UUID, RytmStan> serie = new HashMap<>();
  private final Map<UUID, Integer> ostatnieOgniwo = new HashMap<>();
  private final Map<UUID, UUID> podwojKrwawienie = new HashMap<>();
  /** Cel prawdziwego ciosu gracza (PrePlayerAttackEntityEvent) i tick - odroznia cios od rozprysku MMOItems/MythicLib,
   *  ktory tez przychodzi jako ENTITY_ATTACK od gracza (zmierzone: 1 klik = 3 zdarzenia przy 3 mobach obok siebie). */
  private final Map<UUID, UUID> celCiosu = new HashMap<>();
  private final Map<UUID, Integer> tickCiosu = new HashMap<>();
  private final Map<UUID, OstatniAtak> ostatniAtak = new HashMap<>();
  private Rodzaj rodzajDodatkowegoCiosu;
  private boolean pancerzDodatkowegoCiosu;
  private final Set<UUID> wykonczenieZamach = new HashSet<>();
  private final Map<UUID, Runnable> oczekujacyZamach = new HashMap<>();
  private boolean mmocore;
  /** Model staminy w jednostkach lekkiego ciosu (sekcja stamina w configu); null bez MMOCore. */
  private Wytrzymalosc wytrzymalosc;
  private double progRytmu, kosztRytmu, mnoznikRytmu, lukRytmu;
  private int progMocnego, odnowienieMocnegoTicki, odnowienieSzturchniecia, zmeczenieMocnego, oknoRytmu, dlugoscRytmu;
  private double mnoznikSzturchniecia, bazowyMnoznikMocnego;
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
    if (mmocore) wytrzymalosc = new Wytrzymalosc(this);
    saveDefaultConfig();
    wczytaj();
    getServer().getPluginManager().registerEvents(this, this);
    if (wytrzymalosc != null) {
      getServer().getPluginManager().registerEvents(wytrzymalosc, this);
      getServer().getScheduler().runTaskTimer(this, wytrzymalosc::tyknij, 2L, 2L);
    }
    getServer().getScheduler().runTaskTimer(this,
        () -> getServer().getOnlinePlayers().forEach(p -> dopasujDlonie(p, false)), 10L, 10L);
    getServer().getScheduler().runTaskTimer(this, this::tyknijKrwawienie, 1L, 1L);
    getServer().getScheduler().runTaskTimer(this, this::sygnalizujMocne, 2L, 2L);
  }

  private static Float liczbaLubNull(ConfigurationSection t, String k) {
    return t.contains(k) ? (float) t.getDouble(k) : null;
  }

  private void wczytaj() {
    reloadConfig();
    typy.clear();
    wagi.clear();
    modele.clear();
    przestrzen = getConfig().getString("przestrzen-modeli", "bron");
    debug = getConfig().getBoolean("debug", false);
    progMocnego = Math.max(1, getConfig().getInt("mocny.prog", 6));
    odnowienieMocnegoTicki = getConfig().getInt("mocny.odnowienie", 20);
    odnowienieSzturchniecia = getConfig().getInt("mocny.szturchniecie_odnowienie", 20);
    zmeczenieMocnego = getConfig().getInt("mocny.zmeczenie", 60);
    mnoznikSzturchniecia = getConfig().getDouble("mocny.szturchniecie_mnoznik", 0.5);
    bazowyMnoznikMocnego = getConfig().getDouble("mocny.bazowy_mnoznik", 0.8);
    progRytmu = getConfig().getDouble("rytm.prog_rytmu", 0.9);
    oknoRytmu = getConfig().getInt("rytm.okno_rytmu", 8);
    kosztRytmu = getConfig().getDouble("rytm.koszt_rytmu", 2);
    dlugoscRytmu = Math.max(2, getConfig().getInt("rytm.dlugosc", 3));
    mnoznikRytmu = getConfig().getDouble("rytm.mnoznik", 1.4);
    lukRytmu = getConfig().getDouble("rytm.luk_mnoznik", 0.5);
    if (wytrzymalosc != null) wytrzymalosc.wczytaj(getConfig().getConfigurationSection("stamina"));
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
        ConfigurationSection par = t.getConfigurationSection("parowanie");
        Parowanie parowanie = par == null ? null : new Parowanie(par.getInt("okno", 6),
            par.getInt("riposta_ticki", 30), par.getDouble("riposta_mnoznik", 1.5));
        ConfigurationSection ud = t.getConfigurationSection("uderzenie");
        Uderzenie uderzenie = ud == null ? null : new Uderzenie(ud.getDouble("promien", 3.5), ud.getDouble("mnoznik", 1.6));
        ConfigurationSection uk = t.getConfigurationSection("z_ukrycia");
        Ukrycie ukrycie = uk == null ? null : new Ukrycie(uk.getDouble("plecy", 2), uk.getDouble("ukrycie", 3));
        ConfigurationSection kr = t.getConfigurationSection("krwawienie");
        Krwawienie krwawienie = kr == null ? null : new Krwawienie(kr.getDouble("procent", 0.25),
            kr.getInt("czas", 60), kr.getInt("stosy", 1));
        ConfigurationSection ci = t.getConfigurationSection("ciezkie");
        Ciezkie ciezkie = ci == null ? null : new Ciezkie(ci.getString("przod"), ci.getString("bok"),
            ci.getString("tyl"), ci.getString("bieg"), ci.getString("spadanie"),
            ci.getString("brak", ci.getString("przod")), ci.getDouble("koszt", 12), ci.getInt("pelne", 12));
        AtakPchniecie atakPchniecie = null;
        ConfigurationSection ap = t.getConfigurationSection("atak_pchniecie");
        if (ap != null) {
          try {
            atakPchniecie = new AtakPchniecie(Set.copyOf(ap.getStringList("gdy")),
                SwingAnimation.Animation.valueOf(ap.getString("zamach", "stab").toUpperCase(Locale.ROOT)),
                Rodzaj.valueOf(ap.getString("rodzaj", "klute").toUpperCase(Locale.ROOT)));
          } catch (IllegalArgumentException ex) {
            getLogger().warning("Typ " + typ + ": niepoprawny atak_pchniecie");
          }
        }
        ItemUseAnimation poza = null;
        if (t.getString("poza") != null) {
          try {
            poza = (ItemUseAnimation) ItemUseAnimation.class.getField(t.getString("poza").toUpperCase(Locale.ROOT)).get(null);
          } catch (ReflectiveOperationException ex) {
            getLogger().warning("Typ " + typ + ": nieznana poza '" + t.getString("poza") + "' (bow|crossbow|block|spear|trident|...)");
          }
        }
        ItemUseAnimation pozaMocnego = null;
        if (t.getString("poza_mocnego") != null) {
          try {
            pozaMocnego = (ItemUseAnimation) ItemUseAnimation.class.getField(t.getString("poza_mocnego").toUpperCase(Locale.ROOT)).get(null);
          } catch (ReflectiveOperationException ex) {
            getLogger().warning("Typ " + typ + ": nieznana poza mocnego '" + t.getString("poza_mocnego") + "'");
          }
        }
        wagi.put(typ.toUpperCase(Locale.ROOT), t.getDouble("waga", 1.0));
        typy.put(typ.toUpperCase(Locale.ROOT), new Ustawienie(zamach, t.getInt("czas", 6), t.getString("skala"), rodzaj,
            liczbaLubNull(t, "ladowanie"), liczbaLubNull(t, "zasieg"), wzor, garda,
            t.getBoolean("ciezka", false), t.getBoolean("drzewce", false),
            poza, t.getString("reakcja", "brak"), parowanie, uderzenie, ukrycie, krwawienie,
            t.getDouble("pancerz.procent", 0.30), ciezkie, t.getBoolean("rytm", false), atakPchniecie,
            pozaMocnego, t.getDoubleList("kroki"), t.getDouble("prog_rytmu", progRytmu),
            t.getInt("okno_rytmu", oknoRytmu),
            t.isConfigurationSection("luk_lekki") ? new LukLekki(t.getDouble("luk_lekki.kat", 180),
                t.getDouble("luk_lekki.mnoznik", 0.5)) : null));
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
    ostatnieOgniwo.clear();
    podwojKrwawienie.clear();
    celCiosu.clear();
    tickCiosu.clear();
    ostatniAtak.clear();
    wykonczenieZamach.clear();
    oczekujacyZamach.clear();
    odnowienieMocnego.clear();
    sygnalMocnego.clear();
    ostatniBrakStaminy.clear();
    programowyZamach.clear();
    sprintPrzyLadowaniu.clear();
    ostatnieMachniecie.clear();
    kopie.clear();
    getLogger().info("Typy broni: " + typy.size() + ", materialy z modelami trzymania: " + modele.size());
  }

  private Ustawienie ustawienie(ItemStack item) {
    if (item == null || item.getType().isAir()) return null;
    String typ = NBTItem.get(item).getType();
    return typ == null ? null : typy.get(typ.toUpperCase(Locale.ROOT));
  }

  private static final java.util.regex.Pattern SAM_PPM = java.util.regex.Pattern.compile("(?<!SHIFT_)RIGHT_CLICK");

  /** Umiejetnosc MMOItems na zwyklym PPM zabiera PPM (mocny, szarza); SHIFT_RIGHT_CLICK zabiera tylko garde. */
  private static boolean maUmiejetnoscPpm(ItemStack item) {
    String ab = NBTItem.get(item).getString("MMOITEMS_ABILITY");
    return ab != null && SAM_PPM.matcher(ab).find();
  }

  private static boolean maUmiejetnoscShiftPpm(ItemStack item) {
    String ab = NBTItem.get(item).getString("MMOITEMS_ABILITY");
    return ab != null && ab.contains("SHIFT_RIGHT_CLICK");
  }

  private boolean pchniecie(Ustawienie u, String powod) {
    return u.atakPchniecie() != null && u.atakPchniecie().gdy().contains(powod);
  }

  private boolean zapowiedzPchniecia(Player p, ItemStack item, Ustawienie u) {
    if (p == null || u.atakPchniecie() == null) return false;
    Integer doTicka = item.getPersistentDataContainer().get(kluczZamachu, PersistentDataType.INTEGER);
    return doTicka != null && doTicka > getServer().getCurrentTick() && pchniecie(u, "wykonczenie");
  }

  private SwingAnimation.Animation zamachTeraz(Player p, ItemStack item, Ustawienie u) {
    Integer doTicka = item.getPersistentDataContainer().get(kluczZamachu, PersistentDataType.INTEGER);
    if (doTicka != null && doTicka > getServer().getCurrentTick() && u.drzewce())
      return SwingAnimation.Animation.WHACK;
    return zapowiedzPchniecia(p, item, u) ? u.atakPchniecie().zamach() : u.zamach();
  }

  /**
   * Zwraca true, gdy cos zmieniono. Przedmioty spoza MMOItems i typy spoza configu zostaja nietkniete.
   * Kazda zmiana komponentow broni w dloni = klient chowa i wyjmuje bron (~6 t, zmierzone klientem QA 19.10), wiec stan
   * przedmiotu zalezy tylko od trybu PPM (brak/mocny/garda/szarza), nie od WASD ani sprintu broni bez szarzy.
   * zamachTez=false (przeglad, ruch): sam zamach (odwrocony w serii) zostaje do najblizszego machniecia.
   */
  boolean dopasuj(ItemStack item, boolean lewaPusta, boolean sprint, Player gracz, boolean zamachTez) {
    Ustawienie u = ustawienie(item);
    if (u == null) return false;
    // Podmiana komponentu w trakcie uzywania zrywa animacje po stronie klienta.
    if (gracz != null && gracz.isHandRaised() && gracz.getActiveItem().getType() == item.getType()) return false;
    boolean ppm = lewaPusta && !maUmiejetnoscPpm(item);
    Input zEventu = gracz == null ? null : nowyInput.get(gracz.getUniqueId());
    boolean skradanie = gracz != null && (zEventu != null ? zEventu.isSneak()
        : gracz.isSneaking() || gracz.getCurrentInput().isSneak());
    boolean szarza = ppm && u.wzor() != null && (u.garda() == null || sprint && !skradanie);
    boolean garda = ppm && skradanie && u.garda() != null && !szarza && !maUmiejetnoscShiftPpm(item);
    boolean mocny = ppm && !skradanie && u.ciezkie() != null && !szarza;
    Integer zamachDo = item.getPersistentDataContainer().get(kluczZamachu, PersistentDataType.INTEGER);
    boolean odwrotnyZamach = zamachDo != null && zamachDo > getServer().getCurrentTick();
    SwingAnimation.Animation zamach = zamachTeraz(gracz, item, u);
    String tryb = szarza ? "szarza" : garda ? "garda" : mocny ? "mocny" : "brak";
    String baza = wersja + ":" + tryb;
    String byl = item.getPersistentDataContainer().get(kluczStanu, PersistentDataType.STRING);
    if (byl != null && byl.startsWith(baza + ":")) {
      if (!zamachTez) return false;
    }
    String stan = baza + ":" + zamach;
    if (stan.equals(byl)) return false;

    for (DataComponentType t : ZARZADZANE) item.resetData(t);

    if (u.wzor() != null) {
      ItemStack wzor = new ItemStack(u.wzor());
      item.copyDataFrom(wzor, t -> Z_WZORU_LPM.contains(t)
          || (szarza && Z_WZORU_PPM.contains(t)));
    }
    if (zamach != null) {
      item.setData(DataComponentTypes.SWING_ANIMATION,
          SwingAnimation.swingAnimation().type(zamach).duration(u.czas()).build());
    }
    if (u.ladowanie() != null) item.setData(DataComponentTypes.MINIMUM_ATTACK_CHARGE, u.ladowanie());
    if (u.zasieg() != null) {
      AttackRange wz = u.wzor() != null ? item.getData(DataComponentTypes.ATTACK_RANGE) : null;
      item.setData(DataComponentTypes.ATTACK_RANGE, AttackRange.attackRange()
          .minReach(wz != null ? wz.minReach() : 0f).maxReach(u.zasieg())
          .minCreativeReach(0f).maxCreativeReach(Math.max(5f, u.zasieg())).hitboxMargin(0f).mobFactor(1f).build());
    }
    if (garda) {
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

    if (mocny) {
      item.setData(DataComponentTypes.USE_EFFECTS, UseEffects.useEffects()
          .canSprint(false).speedMultiplier(0.45f).interactVibrations(true).build());
    }

    ItemUseAnimation poza = garda ? u.poza() : mocny ? u.pozaMocnego() : null;
    if ((mocny || garda) && poza != null) {
      // Godzina uzywania: gracz puszcza PPM zanim konsumpcja sie skonczy.
      item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().consumeSeconds(3600f).animation(poza)
          .hasConsumeParticles(false).sound(Key.key("minecraft", "intentionally_empty")).build());
    }

    if (u.skala() != null && modele.contains(item.getType())) {
      String skala = u.skala() + (garda && u.garda() != null && u.poza() != null ? "g"
          : mocny ? (u.uderzenie() != null ? "u" : "c") : "");
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

  private void dopasujDlonie(Player p) { dopasujDlonie(p, true); }

  private void dopasujDlonie(Player p, boolean zamachTez) {
    if (!p.isOnline()) return;
    PlayerInventory inv = p.getInventory();
    ItemStack druga = inv.getItemInOffHand();
    boolean lewaPusta = druga.getType().isAir();
    ItemStack glowna = inv.getItemInMainHand();
    if (dopasuj(glowna, lewaPusta, p.isSprinting(), p, zamachTez)) inv.setItemInMainHand(glowna);
    if (dopasuj(druga, false, false, null, zamachTez)) inv.setItemInOffHand(druga);
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
    komboStart.remove(id);
    ostatnieOgniwo.remove(id);
    podwojKrwawienie.remove(id);
    celCiosu.remove(id);
    tickCiosu.remove(id);
    ostatniAtak.remove(id);
    wykonczenieZamach.remove(id);
    oczekujacyZamach.remove(id);
    odnowienieMocnego.remove(id);
    sygnalMocnego.remove(id);
    ostatniBrakStaminy.remove(id);
    programowyZamach.remove(id);
    tickProgramowegoZamachu.remove(id);
    sprintPrzyLadowaniu.remove(id);
    ostatnieMachniecie.remove(id);
    kopie.remove(id);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void kopanie(org.bukkit.event.block.BlockDamageEvent e) { kopie.add(e.getPlayer().getUniqueId()); }

  @EventHandler(priority = EventPriority.MONITOR)
  public void koniecKopania(org.bukkit.event.block.BlockDamageAbortEvent e) { kopie.remove(e.getPlayer().getUniqueId()); }

  @EventHandler(priority = EventPriority.MONITOR)
  public void wykopane(org.bukkit.event.block.BlockBreakEvent e) { kopie.remove(e.getPlayer().getUniqueId()); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void slot(PlayerItemHeldEvent e) {
    serie.remove(e.getPlayer().getUniqueId());
    ostatnieOgniwo.remove(e.getPlayer().getUniqueId());
    pozniej(e.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void zamiana(PlayerSwapHandItemsEvent e) {
    serie.remove(e.getPlayer().getUniqueId());
    ostatnieOgniwo.remove(e.getPlayer().getUniqueId());
    pozniej(e.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void sprint(PlayerToggleSprintEvent e) {
    Player p = e.getPlayer();
    getServer().getScheduler().runTask(this, () -> dopasujDlonie(p, false));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void wejscieRuchu(PlayerInputEvent e) {
    nowyInput.put(e.getPlayer().getUniqueId(), e.getInput());
    try { dopasujDlonie(e.getPlayer(), false); } finally { nowyInput.remove(e.getPlayer().getUniqueId()); }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void klik(InventoryClickEvent e) { pozniej(e.getWhoClicked()); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void przeciagniecie(InventoryDragEvent e) { pozniej(e.getWhoClicked()); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void podniesienie(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player p) pozniej(p); }

  /** Puszczenie PPM konczy ladowanie. Komponenty i zamach przestawiamy tick pozniej, po skonczeniu uzywania. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void startLadowania(org.bukkit.event.player.PlayerInteractEvent e) {
    if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND || !e.getAction().isRightClick()) return;
    Player p = e.getPlayer();
    ItemStack bron = p.getInventory().getItemInMainHand();
    Ustawienie u = ustawienie(bron);
    if (u != null && u.ciezkie() != null && bron.hasData(DataComponentTypes.CONSUMABLE)
        && !bron.hasData(DataComponentTypes.BLOCKS_ATTACKS) && !p.isHandRaised()) {
      if (p.isSprinting()) sprintPrzyLadowaniu.add(p.getUniqueId()); else sprintPrzyLadowaniu.remove(p.getUniqueId());
    }
    RytmStan stan = serie.get(p.getUniqueId());
    if (u == null || u.ciezkie() == null || stan == null || !bron.hasData(DataComponentTypes.CONSUMABLE)
        || !java.util.Objects.equals(stan.typ(), NBTItem.get(bron).getType())
        || getServer().getCurrentTick() > stan.ostatniTick() + u.oknoRytmu()) { komboStart.remove(p.getUniqueId()); return; }
    komboStart.put(p.getUniqueId(), Math.min(2, stan.trafienia()));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void sondaPpm(org.bukkit.event.player.PlayerInteractEvent e) {
    if (!debug || e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND || !e.getAction().isRightClick()) return;
    Player p = e.getPlayer();
    ItemStack i = p.getInventory().getItemInMainHand();
    dbg("SONDA_PPM akcja=" + e.getAction() + " anulowane=" + e.isCancelled() + " uzyjPrzedmiot=" + e.useItemInHand()
        + " cooldown=" + p.getCooldown(i) + " konsumpcja=" + i.hasData(DataComponentTypes.CONSUMABLE)
        + " uniesiona=" + p.isHandRaised());
    getServer().getScheduler().runTask(this, () -> dbg("SONDA_PPM+1 uniesiona=" + p.isHandRaised()
        + " aktywny=" + p.getActiveItem().getType()));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void puszczeniePpm(PlayerStopUsingItemEvent e) {
    Player p = e.getPlayer();
    ItemStack item = p.getInventory().getItemInMainHand();
    Ustawienie u = ustawienie(item);
    if (u == null || u.ciezkie() == null || !e.getItem().hasData(DataComponentTypes.CONSUMABLE)
        || e.getItem().hasData(DataComponentTypes.BLOCKS_ATTACKS)
        || !p.getInventory().getItemInOffHand().getType().isAir() || maUmiejetnoscPpm(item)
        || item.getType() != e.getItem().getType()
        || !java.util.Objects.equals(NBTItem.get(item).getString("MMOITEMS_ITEM_ID"),
            NBTItem.get(e.getItem()).getString("MMOITEMS_ITEM_ID"))) return;
    UUID id = p.getUniqueId();
    sygnalMocnego.remove(id);
    int teraz = getServer().getCurrentTick();
    int ticki = e.getTicksHeldFor();
    if (odnowienieMocnego.getOrDefault(id, 0) > teraz) {
      dbg("MOCNY typ=" + NBTItem.get(item).getType() + " wariant=odnowienie ticki=" + ticki + " obrazenia=0 stamina=0");
      return;
    }
    boolean szturchniecie = ticki < progMocnego;
    // Kierunek przy puszczeniu (klawisz trzymany w trakcie ladowania), sprint z chwili wcisniecia (ladowanie go wylacza),
    // w prawdziwym locie cios z gory.
    boolean sprintStart = sprintPrzyLadowaniu.remove(id);
    Ciezkie c = u.ciezkie();
    String wariant = szturchniecie ? "szturchniecie"
        : kierunekCiezkiego(p, c, p.getCurrentInput(), sprintStart || p.isSprinting());
    boolean model = wytrzymalosc != null && wytrzymalosc.wlaczona();
    // Model jednostek: mocny = koszt lekkiego tej broni x mnoznik wariantu (ciezkie_warianty.<w>.stamina);
    // szturchniecie tez jest wysilkiem, ale nie odmawia go zmeczonemu (pobiera, ile jest).
    if (model && szturchniecie) wytrzymalosc.wydajIleSie(p, wytrzymalosc.kosztSzturchniecia(p));
    boolean staminaOk = szturchniecie || (model
        ? wytrzymalosc.wydaj(p, wytrzymalosc.kosztMocnego(p,
            getConfig().getDouble("ciezkie_warianty." + wariant + ".stamina", 2.5)))
        : pobierzStamine(p, u.ciezkie().koszt()));
    if (!staminaOk) {
      if (teraz - ostatniBrakStaminy.getOrDefault(id, -1000) >= 40) {
        ostatniBrakStaminy.put(id, teraz);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 0.8f, 0.7f);
        komunikat(p, "&x&F&2&F&2&E&DBrakuje &x&E&8&7&9&7&3staminy", 40);
      }
      dbg("MOCNY typ=" + NBTItem.get(item).getType() + " wariant=brak_staminy ticki=" + ticki + " obrazenia=0 stamina=0");
      return;
    }
    odnowienieMocnego.put(id, teraz + (szturchniecie ? odnowienieSzturchniecia : odnowienieMocnegoTicki));
    int kombo = szturchniecie ? 0 : komboStart.getOrDefault(id, 0);
    komboStart.remove(id);
    serie.remove(id);
    ostatnieOgniwo.remove(id);
    String typ = NBTItem.get(item).getType();
    String iid = NBTItem.get(item).getString("MMOITEMS_ITEM_ID");
    int strona = p.getCurrentInput().isLeft() ? -1 : 1;
    getServer().getScheduler().runTask(this, () -> {
      if (!p.isOnline() || p.isDead()) return;
      ItemStack bron = p.getInventory().getItemInMainHand();
      if (!java.util.Objects.equals(typ, NBTItem.get(bron).getType())
          || !java.util.Objects.equals(iid, NBTItem.get(bron).getString("MMOITEMS_ITEM_ID"))) return;
      wykonajMocny(p, bron, u, wariant, ticki, szturchniecie, strona, kombo);
    });
  }

  private LivingEntity celMocnego(Player p, Ustawienie u, String wariant) {
    Location oko = p.getEyeLocation();
    Vector przod = oko.getDirection().normalize();
    double zasieg = zasieg(u);
    AttackRange zakres = p.getInventory().getItemInMainHand().getData(DataComponentTypes.ATTACK_RANGE);
    double min = zakres == null ? 0.3 : zakres.minReach();
    double szerokosc = getConfig().getDouble("mocny." + ("zamach_boczny".equals(wariant)
        ? "szerokosc_luku" : "szerokosc_celu"), 0.65);
    LivingEntity najlepszy = null;
    double odleglosc = Double.MAX_VALUE;
    for (var enc : p.getWorld().getNearbyEntities(oko, zasieg + 1, zasieg + 1, zasieg + 1)) {
      if (!(enc instanceof LivingEntity cel) || cel.equals(p) || cel.isDead() || !p.hasLineOfSight(cel)
          || !UtilityMethods.canTarget(p, cel, InteractionType.OFFENSE_ACTION)) continue;
      Vector doCelu = cel.getLocation().add(0, cel.getHeight() * 0.55, 0).toVector().subtract(oko.toVector());
      double wzdluz = doCelu.dot(przod);
      if (wzdluz < min || wzdluz > zasieg + cel.getWidth() / 2) continue;
      double obok = doCelu.subtract(przod.clone().multiply(wzdluz)).length();
      if (obok > szerokosc + cel.getWidth() / 2 || wzdluz >= odleglosc) continue;
      najlepszy = cel;
      odleglosc = wzdluz;
    }
    return najlepszy;
  }

  private void ruchMocnego(Player p, String wariant, boolean szturchniecie, int strona) {
    Vector przod = p.getLocation().getDirection().setY(0);
    if (przod.lengthSquared() < 1e-4) return;
    przod.normalize();
    Vector ruch = new Vector(0, 0, 0);
    if (szturchniecie) ruch = przod.clone().multiply(getConfig().getDouble("mocny.krok_szturchniecia", 0.18));
    else if ("odepchniecie".equals(wariant)) ruch = przod.clone().multiply(-getConfig().getDouble("mocny.odskok", 0.35));
    else if ("zamach_boczny".equals(wariant))
      ruch = new Vector(-przod.getZ(), 0, przod.getX()).multiply(strona * getConfig().getDouble("mocny.krok_boczny", 0.32));
    else if ("wypad".equals(wariant) || "z_rozbiegu".equals(wariant))
      ruch = przod.multiply(getConfig().getDouble("mocny." + ("z_rozbiegu".equals(wariant)
          ? "krok_biegu" : "krok_wypadu"), 0.4));
    ruch = bezWchodzeniaWCel(p, ruch);
    if (ruch.lengthSquared() > 0) p.setVelocity(p.getVelocity().add(ruch.setY(0.08)));
  }

  /** Wykonczenie serii LPM mocnym ciosem: zachwianie, rozblysk, dzwiek i napis na pasku. */
  private void efektKomba(Player p, LivingEntity cel, int kombo) {
    zachwiej(cel, p.getLocation());
    Location l = cel.getLocation().add(0, cel.getHeight() * 0.6, 0);
    p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, l, kombo == 2 ? 3 : 1, 0.3, 0.2, 0.3, 0);
    p.getWorld().spawnParticle(Particle.CRIT, l, kombo == 2 ? 30 : 15, 0.35, 0.35, 0.35, 0.3);
    p.getWorld().playSound(l, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, kombo == 2 ? 0.7f : 0.9f);
    if (kombo == 2) p.getWorld().playSound(l, Sound.ITEM_MACE_SMASH_AIR, SoundCategory.PLAYERS, 0.7f, 1.2f);
    if (getServer().getCurrentTick() - ostatniBrakStaminy.getOrDefault(p.getUniqueId(), -1000) >= 40)
      komunikat(p, kombo == 2 ? "&6⚔ Kombo: L + L + P" : "&e⚔ Kombo: L + P", 30);
  }

  /** Krok ciosu nie wpycha gracza w istote przed nim: przy celu blizej niz mocny.dystans_kroku zdejmuje skladowa do przodu. */
  private Vector bezWchodzeniaWCel(Player p, Vector ruch) {
    Vector przod = p.getLocation().getDirection().setY(0);
    if (przod.lengthSquared() < 1e-4) return ruch;
    przod.normalize();
    double naprzod = ruch.dot(przod);
    if (naprzod <= 0) return ruch;
    double limit = getConfig().getDouble("mocny.dystans_kroku", 1.5);
    for (var enc : p.getNearbyEntities(limit + 1, 2, limit + 1)) {
      if (!(enc instanceof LivingEntity cel) || cel.isDead()) continue;
      Vector doCelu = cel.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
      double wzdluz = doCelu.dot(przod);
      double obok = doCelu.clone().subtract(przod.clone().multiply(wzdluz)).length();
      if (wzdluz > 0 && wzdluz - cel.getWidth() / 2 < limit && obok < 0.6 + cel.getWidth() / 2)
        return ruch.clone().subtract(przod.multiply(naprzod));
    }
    return ruch;
  }

  private void wykonajMocny(Player p, ItemStack bron, Ustawienie u, String wariant, int ticki,
                            boolean szturchniecie, int strona, int kombo) {
    boolean zmeczony = !szturchniecie && ticki >= zmeczenieMocnego;
    double postep = szturchniecie ? 0 : Math.max(0, Math.min(1,
        (double) (ticki - progMocnego) / Math.max(1, u.ciezkie().pelne() - progMocnego)));
    double maks = getConfig().getDouble("ciezkie_warianty." + wariant + ".mnoznik", 1.3);
    double mnoznik = szturchniecie ? mnoznikSzturchniecia : zmeczony ? 1.0
        : bazowyMnoznikMocnego + (maks - bazowyMnoznikMocnego) * postep;
    // Kombo: mocny cios zaczety w trakcie serii LPM jest jej wykonczeniem (L+P, L+L+P).
    if (kombo > 0) mnoznik *= getConfig().getDouble("kombo.mnoznik_" + kombo, kombo == 1 ? 1.2 : 1.4);
    Rodzaj rodzaj = szturchniecie && u.rodzaj() != Rodzaj.KLUTE ? Rodzaj.OBUCHOWE
        : u.rodzaj();
    // Zamach broni bez podmiany przedmiotu (podmiana = klient chowa i wyjmuje bron w srodku ciosu).
    SwingAnimation zamachBroni = bron.getData(DataComponentTypes.SWING_ANIMATION);
    SwingAnimation.Animation zamach = zamachBroni != null ? zamachBroni.type() : u.zamach();
    programowyZamach.add(p.getUniqueId());
    tickProgramowegoZamachu.put(p.getUniqueId(), getServer().getCurrentTick());
    try { p.swingMainHand(); }
    finally { programowyZamach.remove(p.getUniqueId()); }
    ruchMocnego(p, wariant, szturchniecie, strona);
    smuga(p, rodzaj, szturchniecie ? (rodzaj == Rodzaj.KLUTE ? 0 : 1) : "zamach_boczny".equals(wariant)
        ? strona : rodzaj == Rodzaj.KLUTE ? 0 : 2);
    LivingEntity cel = celMocnego(p, u, wariant);
    double baza = MMOPlayerData.get(p).getStatMap().getStat("ATTACK_DAMAGE");
    double obrazenia = baza * mnoznik;
    if (cel != null) {
      if (u.ukrycie() != null) {
        Vector patrzy = cel.getLocation().getDirection().setY(0);
        Vector doGracza = p.getLocation().toVector().subtract(cel.getLocation().toVector()).setY(0);
        if (patrzy.lengthSquared() > 1e-4 && doGracza.lengthSquared() > 1e-4
            && Math.toDegrees(patrzy.angle(doGracza)) > 120)
          obrazenia *= p.isSneaking() && (!(cel instanceof Mob mob) || !p.equals(mob.getTarget()))
              ? u.ukrycie().ukrycie() : u.ukrycie().plecy();
      }
      Rodzaj poprzedni = rodzajDodatkowegoCiosu;
      boolean poprzedniPancerz = pancerzDodatkowegoCiosu;
      rodzajDodatkowegoCiosu = rodzaj;
      pancerzDodatkowegoCiosu = u.pancerz() > 0 && "pancerz".equals(u.reakcja())
          || rodzaj == Rodzaj.KLUTE && u.atakPchniecie() != null;
      try { dodatkowyCios(p, cel, obrazenia); }
      finally { rodzajDodatkowegoCiosu = poprzedni; pancerzDodatkowegoCiosu = poprzedniPancerz; }
      efektMocnegoTrafienia(p, cel, rodzaj, u.ciezka());
      if (kombo > 0) efektKomba(p, cel, kombo);
      if (u.krwawienie() != null && !szturchniecie) nalozKrwawienie(p, cel, u.krwawienie(), obrazenia, 1);
      if (szturchniecie) {
        zachwiej(cel, p.getLocation());
        odrzuc(cel, p, getConfig().getDouble("ciezkie_warianty.odepchniecie.sila", 1.1)
            * getConfig().getDouble("mocny.odrzut_szturchniecia", 0.45));
      } else {
        ciezkiEfekt(p, cel, u, wariant, obrazenia, rodzaj == Rodzaj.KLUTE);
        if ("z_powietrza".equals(wariant)) zachwiej(cel, p.getLocation());
      }
    } else if (!szturchniecie && "zamach_boczny".equals(wariant)) {
      // obrot w tlumie trafia luk takze bez celu na wprost
      ciezkiEfekt(p, null, u, wariant, obrazenia, false);
    }
    // Mlot zachowuje obszarowe uderzenie; pelne ladowanie i wariant przedni.
    if (!szturchniecie && u.uderzenie() != null && postep >= 1 && "znad_glowy".equals(wariant))
      uderzenie(p, u.uderzenie(), baza * u.uderzenie().mnoznik(), cel);
    if (szturchniecie) p.getWorld().playSound(p.getLocation(), rodzaj == Rodzaj.KLUTE
        ? Sound.ITEM_TRIDENT_HIT : u.drzewce() ? Sound.BLOCK_BAMBOO_WOOD_HIT : u.ciezka()
            ? Sound.ITEM_MACE_SMASH_GROUND : Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
        SoundCategory.PLAYERS, 0.6f, 1.4f);
    else p.getWorld().playSound(p.getLocation(), rodzaj == Rodzaj.KLUTE ? Sound.ITEM_TRIDENT_THROW
        : u.ciezka() ? Sound.ITEM_MACE_SMASH_GROUND : Sound.ENTITY_PLAYER_ATTACK_SWEEP,
        SoundCategory.PLAYERS, 0.8f, u.ciezka() ? 0.75f : 1.1f);
    dbg((szturchniecie ? "SZTURCHNIECIE" : "MOCNY") + " typ=" + NBTItem.get(bron).getType()
        + " ogniwo=0 wariant=" + wariant + " kombo=" + kombo + " ticki=" + ticki + " obrazenia=" + obrazenia
        + " stamina=" + (szturchniecie ? 0 : u.ciezkie().koszt()) + " cel=" + (cel == null ? "pudlo" : cel.getType())
        + " zamach=" + zamach + " zmeczenie=" + zmeczony);
  }

  private void efektMocnegoTrafienia(Player p, LivingEntity cel, Rodzaj rodzaj, boolean ciezka) {
    Location punkt = cel.getLocation().add(0, cel.getHeight() * 0.6, 0);
    switch (rodzaj) {
      case CIETE -> {
        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, punkt, 2, 0.2, 0.2, 0.2, 0);
        p.getWorld().playSound(punkt, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.7f, ciezka ? 0.8f : 1.2f);
      }
      case KLUTE -> {
        p.getWorld().spawnParticle(Particle.CRIT, punkt, 12, 0.15, 0.15, 0.15, 0.1);
        p.getWorld().playSound(punkt, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.6f, ciezka ? 0.9f : 1.3f);
      }
      case OBUCHOWE -> {
        p.getWorld().spawnParticle(Particle.CRIT, punkt, 7, 0.25, 0.15, 0.25, 0.1);
        p.getWorld().playSound(punkt, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
            SoundCategory.PLAYERS, 0.7f, ciezka ? 0.7f : 1.0f);
      }
      default -> { }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void lpm(PlayerAnimationEvent e) {
    if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
    // anulowane machniecie nie jest ciosem (klik w pelna mape, tryb zaklec) - bez smugi i swistu
    if (e.isCancelled()) return;
    Player p = e.getPlayer();
    if (programowyZamach.contains(p.getUniqueId())
        || getServer().getCurrentTick() - tickProgramowegoZamachu.getOrDefault(p.getUniqueId(), -100) <= 1) return;
    ItemStack bron = p.getInventory().getItemInMainHand();
    Ustawienie smuga = ustawienie(bron);
    OstatniAtak ostatni = ostatniAtak.get(p.getUniqueId());
    boolean trafilTeraz = ostatni != null && ostatni.tick() >= getServer().getCurrentTick() - 1;
    if (smuga != null && !trafilTeraz) {
      // machniecie przy kopaniu bloku to nie cios
      if (kopie.contains(p.getUniqueId())) return;
      // martwe klikniecie: przed progiem naladowania broni (minimum_attack_charge) cios nie wchodzi - bez smugi, swistu
      // i kosztu; rytm broni wyznacza odstep od ostatniego liczonego machniecia
      Integer poprzednie = ostatnieMachniecie.get(p.getUniqueId());
      if (smuga.ladowanie() != null && poprzednie != null
          && (double) (getServer().getCurrentTick() - poprzednie) / pelneNaladowanie(p) < smuga.ladowanie()) return;
    }
    Rodzaj rodzajSmugi = ostatni != null && ostatni.tick() >= getServer().getCurrentTick() - 1
        ? ostatni.rodzaj() : smuga == null ? Rodzaj.BRAK
        : zapowiedzPchniecia(p, bron, smuga) ? smuga.atakPchniecie().rodzaj() : smuga.rodzaj();
    int ogniwo = ostatni != null && ostatni.tick() >= getServer().getCurrentTick() - 1
        ? ostatni.ogniwo() : zapowiedzOgniwo(p, bron);
    // Kierunek lekkiego ciosu (19.10): A/D = ciecie z tej strony i krok w bok, S = ciecie z cofnieciem; W/brak = seria.
    // Bez podmiany przedmiotu (zamach siedzi w komponencie broni) - kierunek widac w smudze i ruchu, obrazenia te same.
    Input wejscie = p.getCurrentInput();
    int bokKlawisza = wejscie.isLeft() == wejscie.isRight() ? 0 : wejscie.isLeft() ? -1 : 1;
    if (smuga != null && smuga.zamach() != null && smuga.rodzaj() != Rodzaj.BRAK)
      smuga(p, rodzajSmugi, rodzajSmugi != Rodzaj.KLUTE && bokKlawisza != 0 ? -bokKlawisza
          : ogniwo == 2 ? -1 : ogniwo == 3 && rodzajSmugi == Rodzaj.KLUTE ? 0 : ogniwo == 3 ? 2 : 1);
    if (smuga != null && smuga.ciezkie() != null) {
      Sound swist = smuga.drzewce() || smuga.rodzaj() == Rodzaj.KLUTE ? Sound.ITEM_TRIDENT_THROW
          : smuga.rodzaj() == Rodzaj.OBUCHOWE ? Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK
          : smuga.ciezka() ? Sound.ENTITY_PLAYER_ATTACK_STRONG : Sound.ENTITY_PLAYER_ATTACK_SWEEP;
      p.getWorld().playSound(p.getLocation(), swist, SoundCategory.PLAYERS, 0.35f,
          ogniwo == 3 ? 0.85f : ogniwo == 2 ? 1.15f : 1.0f);
    }
    boolean trafil = ostatni != null && ostatni.tick() >= getServer().getCurrentTick() - 1;
    if (trafil && smuga != null && ogniwo >= 1 && ogniwo <= smuga.kroki().size()) {
      Vector kierunek = p.getLocation().getDirection().setY(0);
      if (kierunek.lengthSquared() > 1e-4) {
        double krok = smuga.kroki().get(ogniwo - 1);
        kierunek.normalize();
        Vector prawo = new Vector(-kierunek.getZ(), 0, kierunek.getX());
        if (bokKlawisza != 0) kierunek = prawo.multiply(bokKlawisza * Math.abs(krok));
        else if (wejscie.isBackward()) kierunek.multiply(-Math.abs(krok));
        else {
          kierunek.multiply(krok);
          if (ogniwo == 2 && !smuga.drzewce())
            kierunek.add(prawo.multiply(krok * getConfig().getDouble("rytm.krok_boczny_mnoznik", 0.35)));
        }
        p.setVelocity(p.getVelocity().add(bezWchodzeniaWCel(p, kierunek)));
      }
    }
    if (smuga != null && ostatni == null) dbg("LEKKI typ=" + NBTItem.get(bron).getType() + " ogniwo=" + ogniwo
        + " wariant=" + (ogniwo == 3 ? "wykonczenie" : ogniwo == 2 ? "z_lewej" : "z_prawej")
        + " ticki=0 obrazenia=0 stamina=0"
        + " zamach=" + (rodzajSmugi == Rodzaj.KLUTE ? "STAB" : "WHACK"));
    // Kazde machniecie bronia to wysilek (takze pudlo), ale proporcjonalny do zamachu: spam niedoladowanych klikniec
    // (8 CPS) wyczerpywal pelny pasek w 2-3 s. Naladowanie z odstepu od poprzedniego machniecia (min. 0,25 kosztu).
    if (smuga != null) {
      int teraz = getServer().getCurrentTick();
      Integer poprzednie = ostatnieMachniecie.put(p.getUniqueId(), teraz);
      double zamachu = poprzednie == null ? 1 : Math.min(1, (double) (teraz - poprzednie) / pelneNaladowanie(p));
      if (wytrzymalosc != null && wytrzymalosc.wlaczona())
        wytrzymalosc.wydajIleSie(p, wytrzymalosc.kosztLekkiego(p) * Math.max(0.25, zamachu));
    }
    ostatniAtak.remove(p.getUniqueId());
    Runnable nastepny = oczekujacyZamach.remove(p.getUniqueId());
    if (nastepny != null) getServer().getScheduler().runTask(this, nastepny);
    if (wykonczenieZamach.remove(p.getUniqueId()))
      getServer().getScheduler().runTask(this, () -> przywrocZamach(p));
    if (smuga != null) dopasujDlonie(p);
  }

  private void uderzenie(Player p, Uderzenie u, double obrazenia, LivingEntity pierwotny) {
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
    var meta = MMOPlayerData.get(p).getStatMap().cache(io.lumine.mythic.lib.api.player.EquipmentSlot.MAIN_HAND);
    for (var enc : swiat.getNearbyEntities(srodek, u.promien(), u.promien(), u.promien())) {
      if (!(enc instanceof LivingEntity cel) || cel.equals(p) || cel.equals(pierwotny) || cel.isDead()
          || cel.getLocation().distanceSquared(srodek) > u.promien() * u.promien()
          || !UtilityMethods.canTarget(p, cel, InteractionType.OFFENSE_ACTION)) continue;
      cel.setNoDamageTicks(0);
      wlasnyCios = true;
      int naladowanie = licznikNaladowania(p);
      int nietykalnosc = cel.getNoDamageTicks();
      double ostatnie = cel.getLastDamage();
      try {
        meta.attack(cel, obrazenia, DamageType.WEAPON, DamageType.PHYSICAL);
      } finally {
        wlasnyCios = false;
        przywrocNaladowanie(p, naladowanie);
        if (!cel.isDead()) { cel.setNoDamageTicks(nietykalnosc); cel.setLastDamage(ostatnie); }
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
    if (wytrzymalosc != null && wytrzymalosc.wlaczona()) wytrzymalosc.zwrocZaParowanie(p);   // nagroda za timing
    dbg("BLOK typ=" + NBTItem.get(bron).getType() + " ogniwo=0 wariant=parowanie ticki="
        + p.getActiveItemUsedTime() + " obrazenia=0 stamina=0");
    riposty.put(p.getUniqueId(), new Riposta(getServer().getCurrentTick() + u.parowanie().ripostaTicki(),
        u.parowanie().ripostaMnoznik()));
    zachwiej(napastnik, p.getLocation());
    Location styk = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(0.8));
    p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, styk, 28, 0.2, 0.2, 0.2, 0.3);
    p.getWorld().playSound(styk, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.8f, 1.5f);
  }

  /** Priorytety paska MythicLib: 40 = odmowa (brak staminy), 30 = informacja (kombo); pasek MMOCore ma 10. */
  private void komunikat(Player p, String tekst, int priorytet) {
    String t = org.bukkit.ChatColor.translateAlternateColorCodes('&', tekst);
    if (mmocore) MMOPlayerData.get(p).getActionBar().show(priorytet, 40L, t);   // zwykly pasek akcji MMOCore zamazuje po 0,25 s
    else p.sendActionBar(net.kyori.adventure.text.Component.text(t));
  }

  void komunikatWytrzymalosci(Player p, String tekst) { komunikat(p, tekst, 40); }

  void dbgWytrzymalosci(String m) { dbg(m); }

  /** Waga broni w prawej rece (1 = bez broni MMOItems albo typ bez wagi). */
  double wagaBroni(Player p) {
    ItemStack i = p.getInventory().getItemInMainHand();
    if (i.getType().isAir()) return 1.0;
    String typ = NBTItem.get(i).getType();
    return typ == null ? 1.0 : wagi.getOrDefault(typ.toUpperCase(Locale.ROOT), 1.0);
  }

  /**
   * API dla innych pluginow walki (DwieBronie, przez refleksje): punkty staminy za akcje warta {@code jednostki}
   * lekkich ciosow trzymana bronia; -1, gdy model jednostek jest wylaczony (wtedy plugin zostaje przy swoich %).
   */
  public double kosztJednostek(Player p, double jednostki) {
    return wytrzymalosc == null || !wytrzymalosc.wlaczona() ? -1 : wytrzymalosc.kosztLekkiego(p) * jednostki;
  }

  /** API: lekki cios innym kanalem (np. lewa reka w DwieBronie) - pobiera ile jest; true = gracz byl w zadyszce. */
  public boolean lekkiCiosZInnejReki(Player p) {
    if (wytrzymalosc == null || !wytrzymalosc.wlaczona()) return false;
    boolean zmeczony = wytrzymalosc.zmeczony(p);
    wytrzymalosc.wydajIleSie(p, wytrzymalosc.kosztLekkiego(p));
    if (zmeczony) wytrzymalosc.sygnalizujZadyszke(p);
    return zmeczony;
  }

  public double zmeczenieObrazenia() { return wytrzymalosc == null ? 1 : wytrzymalosc.zmeczenieObrazenia(); }

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

  private void sygnalizujMocne() {
    for (Player p : getServer().getOnlinePlayers()) {
      UUID id = p.getUniqueId();
      Ustawienie u = ustawienie(p.getInventory().getItemInMainHand());
      if (u == null || u.ciezkie() == null || !p.isHandRaised()
          || !p.getActiveItem().hasData(DataComponentTypes.CONSUMABLE)
          || p.getActiveItem().hasData(DataComponentTypes.BLOCKS_ATTACKS)) {
        sygnalMocnego.remove(id);
        continue;
      }
      if (p.getActiveItemUsedTime() < u.ciezkie().pelne() || !sygnalMocnego.add(id)) continue;
      p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 0.25f, 1.6f);
      Vector przod = p.getEyeLocation().getDirection().setY(0);
      if (przod.lengthSquared() < 1e-4) przod = new Vector(1, 0, 0);
      przod.normalize();
      Vector prawo = new Vector(-przod.getZ(), 0, przod.getX());
      Location dlon = p.getEyeLocation().add(przod.multiply(0.45)).add(prawo.multiply(0.28)).subtract(0, 0.4, 0);
      p.getWorld().spawnParticle(Particle.ENCHANTED_HIT, dlon, 3, 0.08, 0.08, 0.08, 0.01);
    }
  }

  /** Prawdziwy lot (skok, spadanie): predkosc pionowa gracza na serwerze bywa < -0,1 tez przy ruchu po ziemi
   *  w trakcie ladowania (zrzuty 18.10: W i D dawaly cios z powietrza na plaskim). */
  private static boolean wLocie(Player p) {
    return !p.isOnGround() && p.getFallDistance() > 0.5f;
  }

  private String kierunekCiezkiego(Player p, Ciezkie c, Input wejscie, boolean sprint) {
    if (c.spadanie() != null && wLocie(p)) return c.spadanie();
    if (sprint && c.bieg() != null) return c.bieg();
    if (wejscie.isBackward() && c.tyl() != null) return c.tyl();
    if ((wejscie.isLeft() || wejscie.isRight()) && c.bok() != null) return c.bok();
    if (wejscie.isForward() && c.przod() != null) return c.przod();
    return c.brak() != null ? c.brak() : c.przod();
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
    // meta.attack = playerAttack gracza -> vanilla zeruje mu naladowanie (jak w krwawieniu), a cel dostaje nietykalnosc
    // i lastDamage = nasz cios: LPM tuz po trafionym mocnym (4 t, klient QA/bot 19.10) nie zadawal nic. Oba wracaja.
    int naladowanie = licznikNaladowania(p);
    int nietykalnosc = cel.getNoDamageTicks();
    double ostatnie = cel.getLastDamage();
    try { meta.attack(cel, obrazenia, DamageType.WEAPON, DamageType.PHYSICAL); }
    finally {
      wlasnyCios = false;
      przywrocNaladowanie(p, naladowanie);
      if (!cel.isDead()) { cel.setNoDamageTicks(nietykalnosc); cel.setLastDamage(ostatnie); }
    }
  }

  private double zasieg(Ustawienie u) { return u.zasieg() == null ? 3.0 : u.zasieg(); }

  private void luk(Player p, LivingEntity pierwotny, Ustawienie u, double obrazenia, double kat) {
    Location oko = p.getEyeLocation();
    Vector patrzy = oko.getDirection().setY(0);
    if (patrzy.lengthSquared() < 1e-4) return;
    patrzy.normalize();
    double zasieg = zasieg(u);
    for (var enc : p.getWorld().getNearbyEntities(p.getLocation(), zasieg + 1, 2.5, zasieg + 1)) {
      if (!(enc instanceof LivingEntity cel) || cel.equals(p) || cel.equals(pierwotny)
          || cel.isDead() || !p.hasLineOfSight(cel)) continue;
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

  private void linia(Player p, LivingEntity pierwotny, Ustawienie u, double obrazenia, boolean przebiciePancerza) {
    Location oko = p.getEyeLocation();
    Vector patrzy = oko.getDirection().normalize();
    double poczatek = pierwotny.getLocation().add(0, pierwotny.getHeight() / 2, 0).toVector()
        .subtract(oko.toVector()).dot(patrzy);
    double koniec = zasieg(u);
    List<LivingEntity> cele = new ArrayList<>();
    for (var enc : p.getWorld().getNearbyEntities(oko, koniec, koniec, koniec)) {
      if (!(enc instanceof LivingEntity cel) || cel.equals(p) || cel.equals(pierwotny)
          || cel.isDead() || !p.hasLineOfSight(cel)) continue;
      Vector d = cel.getLocation().add(0, cel.getHeight() / 2, 0).toVector().subtract(oko.toVector());
      double t = d.dot(patrzy);
      if (t <= poczatek + 0.2 || t > koniec
          || d.subtract(patrzy.clone().multiply(t)).length() > 0.75 + cel.getWidth() / 2) continue;
      cele.add(cel);
    }
    cele.sort(Comparator.comparingDouble(c -> c.getLocation().toVector().subtract(oko.toVector()).dot(patrzy)));
    Rodzaj poprzedniRodzaj = rodzajDodatkowegoCiosu;
    boolean poprzedniPancerz = pancerzDodatkowegoCiosu;
    rodzajDodatkowegoCiosu = Rodzaj.KLUTE;
    pancerzDodatkowegoCiosu = przebiciePancerza;
    try {
      for (int i = 0; i < Math.min(2, cele.size()); i++) dodatkowyCios(p, cele.get(i), obrazenia);
    } finally {
      rodzajDodatkowegoCiosu = poprzedniRodzaj;
      pancerzDodatkowegoCiosu = poprzedniPancerz;
    }
  }

  private void ciezkiEfekt(Player p, LivingEntity cel, Ustawienie u, String wariant, double baza, boolean pchniecie) {
    String sciezka = "ciezkie_warianty." + wariant + ".";
    switch (wariant) {
      case "wypad" -> {
        if (u.rodzaj() == Rodzaj.KLUTE || pchniecie)
          linia(p, cel, u, baza * getConfig().getDouble(sciezka + "linia_mnoznik", 0.6), pchniecie);
      }
      case "znad_glowy" -> {
        zachwiej(cel, p.getLocation());
        Block pod = cel.getLocation().subtract(0, 0.2, 0).getBlock();
        if (!pod.getType().isAir()) p.getWorld().spawnParticle(Particle.BLOCK,
            pod.getLocation().add(0.5, 1, 0.5), 4, 0.2, 0, 0.2, 0, pod.getBlockData());
      }
      case "zamach_boczny" -> {
        String typ = NBTItem.get(p.getInventory().getItemInMainHand()).getType();
        String pole = "KOSA".equals(typ) ? "kat_kosa"
            : "AXE".equals(typ) || "GREATAXE".equals(typ) ? "kat_topor" : "kat";
        luk(p, cel, u, baza * getConfig().getDouble(sciezka + "luk_mnoznik", 0.7),
            getConfig().getDouble(sciezka + pole, 120));
      }
      case "odepchniecie" -> { zachwiej(cel, p.getLocation()); odrzuc(cel, p, getConfig().getDouble(sciezka + "sila", 1.1)); }
      case "z_rozbiegu" -> { odrzuc(cel, p, getConfig().getDouble(sciezka + "sila", 1.1)); p.setSprinting(false); }
      default -> { }
    }
  }

  private void odwrocZamach(Player p, ItemStack bron, int naTickow) {
    // ta sama bron = ten sam typ i id MMOItems; NIE isSimilar - MMOItems zmienia dane przedmiotu przy trafieniu
    // (np. wytrzymalosc), wiec isSimilar po ciosie zawsze falszywe (zmierzone: wykonczenie nigdy nie dostawalo pchniecia)
    String typWzoru = NBTItem.get(bron).getType(), idWzoru = NBTItem.get(bron).getString("MMOITEMS_ITEM_ID");
    // Damage poprzedza ARM_SWING: czekamy na zamach drugiego ciosu, potem ustawiamy animacje trzeciego.
    Runnable ustaw = () -> {
      if (!p.isOnline()) return;
      ItemStack trzymany = p.getInventory().getItemInMainHand();
      if (trzymany.getType().isAir() || !java.util.Objects.equals(typWzoru, NBTItem.get(trzymany).getType())
          || !java.util.Objects.equals(idWzoru, NBTItem.get(trzymany).getString("MMOITEMS_ITEM_ID"))) return;
      trzymany.editPersistentDataContainer(pdc -> pdc.set(kluczZamachu, PersistentDataType.INTEGER,
          getServer().getCurrentTick() + naTickow));
      dopasuj(trzymany, p.getInventory().getItemInOffHand().getType().isAir(), p.isSprinting(), p, true);
      p.getInventory().setItemInMainHand(trzymany);
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
    bron.editPersistentDataContainer(pdc -> pdc.remove(kluczZamachu));
    dopasuj(bron, p.getInventory().getItemInOffHand().getType().isAir(), p.isSprinting(), p, true);
    p.getInventory().setItemInMainHand(bron);
  }

  private int zapowiedzOgniwo(Player p, ItemStack bron) {
    Ustawienie u = ustawienie(bron);
    if (u == null || !u.rytm()) return 0;
    RytmStan stan = serie.get(p.getUniqueId());
    if (stan == null || !java.util.Objects.equals(stan.typ(), NBTItem.get(bron).getType())) return 1;
    int teraz = getServer().getCurrentTick();
    if (teraz > stan.ostatniTick() + u.oknoRytmu() || p.getAttackCooldown() < u.progRytmu()) return 1;
    return Math.min(dlugoscRytmu, stan.trafienia() + 1);
  }

  private boolean rytm(Player p, LivingEntity cel, ItemStack bron, Ustawienie u, EntityDamageByEntityEvent e,
                    int teraz, float naladowanie) {
    String typ = NBTItem.get(bron).getType();
    RytmStan poprzedni = serie.get(p.getUniqueId());
    int pelne = pelneNaladowanie(p);
    boolean wOknie = poprzedni != null && typ.equals(poprzedni.typ())
        && teraz <= poprzedni.ostatniTick() + u.oknoRytmu();
    if (naladowanie < u.progRytmu()) {
      ostatnieOgniwo.put(p.getUniqueId(), 0);
      dbg("LEKKI typ=" + typ + " ogniwo=0 wariant=za_wczesnie ticki=0 obrazenia=" + e.getDamage() + " stamina=0");
      return false;
    }
    int numer = naladowanie >= u.progRytmu() && wOknie ? poprzedni.trafienia() + 1 : 1;
    // W modelu jednostek cios w rytmie jest zwyklym lekkim ciosem (placony przy machnieciu), nie osobnym kosztem.
    if (numer > 1 && (wytrzymalosc == null || !wytrzymalosc.wlaczona()) && !pobierzStamine(p, kosztRytmu)) numer = 1;
    if (numer == 1 && poprzedni != null && poprzedni.trafienia() == dlugoscRytmu - 1) przywrocZamach(p);
    ostatnieOgniwo.put(p.getUniqueId(), numer);
    dbg("RYTM numer=" + numer + " pelne=" + pelne + " okno=" + wOknie + " naladowanie=" + naladowanie);
    float ton = numer >= dlugoscRytmu ? 1.6f : numer == 1 ? 0.8f : numer == 2 ? 1.0f : 1.3f;
    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS,
        numer >= dlugoscRytmu ? 0.9f : 0.3f, ton);
    if (numer >= dlugoscRytmu) {
      double baza = e.getDamage();
      e.setDamage(baza * mnoznikRytmu);
      boolean pchniecie = pchniecie(u, "wykonczenie");
      if (pchniecie) linia(p, cel, u, baza * getConfig().getDouble("ciezkie_warianty.wypad.linia_mnoznik", 0.6), true);
      switch (pchniecie ? u.atakPchniecie().rodzaj() : u.rodzaj()) {
        case CIETE -> {
          if (u.krwawienie() != null) podwojKrwawienie.put(p.getUniqueId(), cel.getUniqueId());
          else luk(p, cel, u, baza * lukRytmu, 120);
        }
        case KLUTE -> {
          if (u.drzewce()) { zachwiej(cel, p.getLocation()); odrzuc(cel, p, 0.65); }
          else if (!pchniecie) linia(p, cel, u, baza * getConfig().getDouble("ciezkie_warianty.wypad.linia_mnoznik", 0.6), false);
        }
        case OBUCHOWE -> { zachwiej(cel, p.getLocation()); odrzuc(cel, p, 0.6); }
        default -> { }
      }
      if ("DAGGER".equals(typ) && u.krwawienie() != null)
        nalozKrwawienie(p, cel, u.krwawienie(), e.getDamage(), 1);
      serie.remove(p.getUniqueId());
      oczekujacyZamach.remove(p.getUniqueId());
      if (pchniecie || u.drzewce()) {
        wykonczenieZamach.add(p.getUniqueId());
        getServer().getScheduler().runTaskLater(this, () -> {
          if (wykonczenieZamach.remove(p.getUniqueId())) przywrocZamach(p);
        }, 4L);
      }
      return pchniecie;
    } else {
      serie.put(p.getUniqueId(), new RytmStan(numer, teraz, typ));
      if (numer == dlugoscRytmu - 1 && (pchniecie(u, "wykonczenie") || u.drzewce()))
        odwrocZamach(p, bron, u.oknoRytmu());
      return false;
    }
  }

  private void smuga(Player p, Rodzaj rodzaj, int kierunek) {
    Location oko = p.getEyeLocation();
    Vector przod = oko.getDirection().normalize();
    Vector bok = new Vector(-przod.getZ(), 0, przod.getX());
    boolean prosto = kierunek == 0 || rodzaj == Rodzaj.KLUTE;
    for (int faza = 0; faza < 4; faza++) {
      final int f = faza;
      getServer().getScheduler().runTaskLater(this, () -> {
        if (!p.isOnline()) return;
        for (int i = 0; i < 8; i++) {
          double t = (f * 8.0 + i) / 31.0;
          Location punkt = prosto ? oko.clone().add(przod.clone().multiply(0.45 + t * 2.75))
                  .add(bok.clone().multiply(kierunek * 0.55 * (1 - t))).subtract(0, 0.48, 0)
              : oko.clone().add(przod.clone().multiply(1.45 + 0.45 * Math.sin(Math.PI * t)))
                  .add(bok.clone().multiply(kierunek == 2 ? 0 : kierunek * (1.25 - 2.5 * t)))
                  .add(0, kierunek == 2 ? 0.8 - 1.6 * t : 0.35 - 0.9 * t, 0);
          if (prosto) p.getWorld().spawnParticle(Particle.CRIT, punkt, 2, 0.015, 0.015, 0.015, 0);
          else p.getWorld().spawnParticle(Particle.DUST, punkt, 2, 0.01, 0.01, 0.01, 0,
              new Particle.DustOptions(rodzaj == Rodzaj.OBUCHOWE ? Color.fromRGB(116, 83, 52)
                  : Color.fromRGB(210, 216, 220), 1.05f));
        }
      }, faza);
    }
    if (rodzaj == Rodzaj.CIETE) p.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
        oko.clone().add(przod.multiply(1.8)).subtract(0, 0.45, 0), 1, 0, 0, 0, 0);
    if (rodzaj == Rodzaj.OBUCHOWE) {
      Block pod = p.getLocation().subtract(0, 0.2, 0).getBlock();
      if (!pod.getType().isAir()) p.getWorld().spawnParticle(Particle.BLOCK, pod.getLocation().add(0.5, 1, 0.5),
          3, 0.2, 0, 0.2, 0, pod.getBlockData());
    }
  }

  private void przebijPancerz(EntityDamageByEntityEvent e, double procent) {
    procent = Math.max(0, Math.min(1, procent));
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

  /** Vanillowe ciecie zamaszyste (miecze z vanilli: 1 + sweeping na moby obok celu) to trzecie, niewidoczne zrodlo obszarowosci
   *  obok lukow pluginu - "czasem trafia sasiada". Bronie z configu trafiaja obok tylko wg wlasnych regul (19.10). */
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void bezVanillowegoZamachu(EntityDamageByEntityEvent e) {
    if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK && e.getDamager() instanceof Player p
        && ustawienie(p.getInventory().getItemInMainHand()) != null) e.setCancelled(true);
  }

  /** Lekki rytm i reakcje trafienia bronia w glownej rece. HIGHEST: MythicLib liczy obrazenia broni wczesniej
   *  i nadpisalby zmiane z nizszego priorytetu (zmierzone: na HIGH mnoznik ciosu w plecy znikal). */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void reakcja(EntityDamageByEntityEvent e) {
    if (wlasnyCios && pancerzDodatkowegoCiosu) {
      przebijPancerz(e, 0.30);
      return;
    }
    if (wlasnyCios || !(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity cel)
        || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || !prawdziwyCios(p, cel)) return;
    ItemStack bron = p.getInventory().getItemInMainHand();
    Ustawienie u = ustawienie(bron);
    if (u == null) return;
    int teraz = getServer().getCurrentTick();
    // Zadyszka: zabraklo staminy na ten cios (pobierana dopiero przy machnieciu, ktore przychodzi po obrazeniach).
    if (wytrzymalosc != null && wytrzymalosc.wlaczona() && wytrzymalosc.zmeczony(p)) {
      e.setDamage(e.getDamage() * wytrzymalosc.zmeczenieObrazenia());
      wytrzymalosc.sygnalizujZadyszke(p);
    }
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
    if (!u.rytm()) ostatnieOgniwo.put(p.getUniqueId(), 1);
    boolean pchniecieCios = u.rytm() && rytm(p, cel, bron, u, e, teraz, naladowanie);
    int ogniwo = ostatnieOgniwo.getOrDefault(p.getUniqueId(), 1);
    Rodzaj rodzajCiosu = pchniecieCios ? u.atakPchniecie().rodzaj()
        : ogniwo == 3 && u.drzewce() ? Rodzaj.OBUCHOWE : u.rodzaj();
    ostatniAtak.put(p.getUniqueId(), new OstatniAtak(cel.getUniqueId(), teraz, rodzajCiosu, ogniwo));
    dbg("LEKKI typ=" + NBTItem.get(bron).getType() + " ogniwo=" + ogniwo + " wariant="
        + (ogniwo == 3 ? "wykonczenie" : ogniwo == 2 ? "z_lewej" : "z_prawej")
        + " ticki=0 obrazenia=" + e.getDamage() + " stamina=" + (ogniwo > 1 ? kosztRytmu : 0)
        + " zamach=" + (rodzajCiosu == Rodzaj.KLUTE ? "STAB" : "WHACK"));
    // Damage poprzedza ARM_SWING: przedmiot musi zachowac zamach trafiajacego ciosu.
    // Dopasowanie nastepnego zamachu wykona lpm() po animacji.
    if (naladowanie < u.progRytmu()) return;
    // Kosa tnie lukiem zawsze tak samo: wszyscy w luku i zasiegu broni (zamiast rozprysku MMOItems z ukrytym cooldownem 3 s).
    if (u.lukLekki() != null) luk(p, cel, u, e.getDamage() * u.lukLekki().mnoznik(), u.lukLekki().kat());
    if (pchniecieCios) przebijPancerz(e, 0.30);
    switch (u.reakcja()) {
      case "pancerz" -> przebijPancerz(e, u.pancerz());
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
    if (u == null || !"krwawienie".equals(u.reakcja()) || u.krwawienie() == null
        || p.getAttackCooldown() < u.progRytmu()) return;
    Krwawienie k = u.krwawienie();
    boolean podwoj = cel.getUniqueId().equals(podwojKrwawienie.remove(p.getUniqueId()));
    nalozKrwawienie(p, cel, k, e.getFinalDamage(), podwoj ? 2 : 1);
  }

  private void nalozKrwawienie(Player p, LivingEntity cel, Krwawienie k, double obrazenia, int nowe) {
    int teraz = getServer().getCurrentTick();
    Krwawiacy stan = krwawiacy.computeIfAbsent(cel.getUniqueId(), id -> new Krwawiacy());
    int limit = Math.max(k.stosy(), nowe);
    while (stan.stosy.size() > Math.max(0, limit - nowe)) stan.stosy.remove(0);
    for (int i = 0; i < nowe; i++) stan.stosy.add(new StosKrwawienia(
        obrazenia * k.procent() / Math.max(1, k.czas() / 20), p.getUniqueId()));
    stan.doTicka = teraz + k.czas();
    if (stan.nastepnyTick <= teraz) stan.nastepnyTick = teraz + 20;
  }

  /** Pole serwera z licznikiem naladowania ciosu (Paper bez settera); null = nie znaleziono, wtedy bez przywracania. */
  private java.lang.reflect.Field poleNaladowania;
  private boolean szukanoPolaNaladowania;

  private int licznikNaladowania(Player p) {
    try {
      Object uchwyt = p.getClass().getMethod("getHandle").invoke(p);
      if (!szukanoPolaNaladowania) {
        szukanoPolaNaladowania = true;
        for (Class<?> k = uchwyt.getClass(); k != null && poleNaladowania == null; k = k.getSuperclass()) {
          try {
            poleNaladowania = k.getDeclaredField("attackStrengthTicker");
            poleNaladowania.setAccessible(true);
          } catch (NoSuchFieldException ignored) { }
        }
        if (poleNaladowania == null) getLogger().warning("Brak pola attackStrengthTicker: krwawienie zeruje naladowanie ciosu");
      }
      return poleNaladowania == null ? -1 : poleNaladowania.getInt(uchwyt);
    } catch (ReflectiveOperationException | RuntimeException e) {
      return -1;
    }
  }

  private void przywrocNaladowanie(Player p, int licznik) {
    if (licznik < 0 || poleNaladowania == null) return;
    try { poleNaladowania.setInt(p.getClass().getMethod("getHandle").invoke(p), licznik); }
    catch (ReflectiveOperationException | RuntimeException ignored) { }
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
        // MythicLib zadaje DOT jako playerAttack gracza, a vanilla zeruje mu wtedy naladowanie ciosu (zmierzone: 0,975 -> 0,025).
        // Takt co 20 t kasowal tak ciosy w trakcie krwawienia: kosa (minimum_attack_charge 0,8) 7/10 ciosow bez trafienia.
        // Nietykalnosc i lastDamage celu tez wracaja: inaczej cios tuz po takcie tracil wartosc taktu (14,12 -> 12,96).
        int naladowanie = licznikNaladowania(p);
        int nietykalnosc = cel.getNoDamageTicks();
        double ostatnie = cel.getLastDamage();
        try { meta.attack(cel, stos.obrazenia(), DamageType.DOT); }
        finally {
          wlasnyCios = false;
          przywrocNaladowanie(p, naladowanie);
          if (!cel.isDead()) {
            cel.setNoDamageTicks(nietykalnosc);
            cel.setLastDamage(ostatnie);
          }
        }
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
    dbg("BLOK typ=" + NBTItem.get(p.getInventory().getItemInMainHand()).getType()
        + " ogniwo=0 wariant=garda ticki=" + p.getActiveItemUsedTime()
        + " obrazenia=" + e.getFinalDamage() + " stamina=0");
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
    OstatniAtak ostatni = ostatniAtak.get(p.getUniqueId());
    Rodzaj rodzaj = wlasnyCios && rodzajDodatkowegoCiosu != null ? rodzajDodatkowegoCiosu
        : ostatni != null && ostatni.tick() == getServer().getCurrentTick()
            && ostatni.cel().equals(cel.getUniqueId()) && prawdziwyCios(p, cel)
            ? ostatni.rodzaj() : u.rodzaj();
    World w = cel.getWorld();
    Location srodek = cel.getLocation().add(0, cel.getHeight() * 0.6, 0);
    float ton = u.ciezka() ? 0.75f : 1.1f;
    switch (rodzaj) {
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
    // /animacjebroni stamina [<zestaw> | gracz <nick>] - zestawy do porownania (sekundy ciaglej walki)
    if (args.length >= 1 && args[0].equalsIgnoreCase("stamina")) {
      if (wytrzymalosc == null) { s.sendMessage("Brak MMOCore - model staminy niedostepny."); return true; }
      if (args.length == 3 && args[1].equalsIgnoreCase("gracz")) {
        Player p = getServer().getPlayerExact(args[2]);
        s.sendMessage(p == null ? "Brak gracza " + args[2] : wytrzymalosc.opis(p));
        return true;
      }
      if (args.length == 2) {
        int z;
        try { z = Integer.parseInt(args[1]); } catch (NumberFormatException ex) { z = -1; }
        if (!wytrzymalosc.zestawy().contains(z)) {
          s.sendMessage("Zestawy: " + wytrzymalosc.zestawy() + " (sekundy ciaglej walki)");
          return true;
        }
        wytrzymalosc.ustawZestaw(z);
        getConfig().set("stamina.zestaw", z);
        saveConfig();
        s.sendMessage("Stamina: zestaw " + z + " s ciaglej walki (zapisany w configu).");
        return true;
      }
      s.sendMessage("Stamina: " + (wytrzymalosc.wlaczona() ? "zestaw " + wytrzymalosc.zestaw() + " s" : "model wylaczony")
          + ", dostepne " + wytrzymalosc.zestawy() + "; /animacjebroni stamina <zestaw> | gracz <nick>");
      return true;
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("sprawdz")) {
      Player p = getServer().getPlayerExact(args[1]);
      if (p == null) { s.sendMessage("Brak gracza " + args[1]); return true; }
      ItemStack i = p.getInventory().getItemInMainHand();
      String typ = i.getType().isAir() ? null : NBTItem.get(i).getType();
      Ustawienie u = ustawienie(i);
      boolean laduje = u != null && u.ciezkie() != null && p.isHandRaised()
          && p.getActiveItem().hasData(DataComponentTypes.CONSUMABLE)
          && !p.getActiveItem().hasData(DataComponentTypes.BLOCKS_ATTACKS);
      int tickiLadowania = laduje ? p.getActiveItemUsedTime() : 0;
      SwingAnimation swing = i.getData(DataComponentTypes.SWING_ANIMATION);
      s.sendMessage("typ=" + typ + " swing=" + i.getData(DataComponentTypes.SWING_ANIMATION)
          + " model=" + i.getData(DataComponentTypes.ITEM_MODEL)
          + " ladowanie=" + i.getData(DataComponentTypes.MINIMUM_ATTACK_CHARGE)
          + " zasieg=" + i.getData(DataComponentTypes.ATTACK_RANGE)
          + " pierce=" + i.hasData(DataComponentTypes.PIERCING_WEAPON)
          + " szarza=" + i.hasData(DataComponentTypes.KINETIC_WEAPON)
          + " garda=" + i.hasData(DataComponentTypes.BLOCKS_ATTACKS)
          + " reakcja=" + (u == null ? "brak" : u.reakcja())
          + " laduje=" + laduje
          + " tickiLadowania=" + tickiLadowania
          + " pelneZa=" + (u == null || u.ciezkie() == null ? -1 : Math.max(0, u.ciezkie().pelne() - tickiLadowania))
          + " ogniwo=" + (u == null ? 0 : zapowiedzOgniwo(p, i))
          + " zamachTeraz=" + (swing == null ? "BRAK" : swing.type())
          + " nastepnyAtak=" + (u != null && zapowiedzPchniecia(p, i, u) ? "pchniecie" : "ciecie")
          + " mocnyZa=" + Math.max(0, odnowienieMocnego.getOrDefault(p.getUniqueId(), 0) - getServer().getCurrentTick())
          + " ripostaZa=" + Math.max(0, riposty.getOrDefault(p.getUniqueId(), new Riposta(0, 1)).doTicka() - getServer().getCurrentTick())
          + " sprint=" + p.isSprinting()
          + " stamina=" + (mmocore ? PlayerData.get(p).getStamina() + "/"
              + MMOPlayerData.get(p).getStatMap().getStat("MAX_STAMINA") : "bez MMOCore")
          + (wytrzymalosc != null ? " [" + wytrzymalosc.opis(p) + "]" : "")
          + " lewa=" + p.getInventory().getItemInOffHand().getType());
      return true;
    }
    return false;
  }
}
