package pl.mcmmorpg.animacjebroni;

import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.comp.interaction.InteractionType;
import io.lumine.mythic.lib.damage.DamageType;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.AttackRange;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.datacomponent.item.SwingAnimation;
import io.papermc.paper.datacomponent.item.UseEffects;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
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
  record StosKrwawienia(double obrazenia, UUID gracz) {}
  static final class Krwawiacy {
    final List<StosKrwawienia> stosy = new java.util.ArrayList<>();
    int doTicka;
    int nastepnyTick;
  }

  record Ustawienie(SwingAnimation.Animation zamach, int czas, String skala, Rodzaj rodzaj, Float ladowanie,
                    Float zasieg, Material wzor, Garda garda, boolean ciezka, Wypad wypad,
                    ItemUseAnimation poza, String reakcja, Parowanie parowanie, Uderzenie uderzenie,
                    Ukrycie ukrycie, Krwawienie krwawienie, double pancerz) {}

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
  private boolean wlasnyCios;
  private boolean debug;
  private void dbg(String m) { if (debug) getLogger().info("DBG t=" + getServer().getCurrentTick() + " " + m); }
  private final Set<Material> modele = new HashSet<>();
  private String przestrzen = "bron";
  private int wersja;
  private NamespacedKey kluczStanu;

  @Override
  public void onEnable() {
    kluczStanu = new NamespacedKey(this, "stan");
    saveDefaultConfig();
    wczytaj();
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getScheduler().runTaskTimer(this,
        () -> getServer().getOnlinePlayers().forEach(this::dopasujDlonie), 10L, 10L);
    getServer().getScheduler().runTaskTimer(this, this::tyknijKrwawienie, 1L, 1L);
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
            t.getDouble("pancerz.procent", 0.30)));
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
    String stan = wersja + ":" + ppm + (dwaTryby ? ":" + sprint : "");
    if (stan.equals(item.getPersistentDataContainer().get(kluczStanu, PersistentDataType.STRING))) return false;

    for (DataComponentType t : ZARZADZANE) item.resetData(t);

    if (u.wzor() != null) {
      ItemStack wzor = new ItemStack(u.wzor());
      item.copyDataFrom(wzor, t -> Z_WZORU_LPM.contains(t)
          || (ppm && (!dwaTryby || sprint) && Z_WZORU_PPM.contains(t)));
    }
    if (u.zamach() != null) {
      item.setData(DataComponentTypes.SWING_ANIMATION,
          SwingAnimation.swingAnimation().type(u.zamach()).duration(u.czas()).build());
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

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void slot(PlayerItemHeldEvent e) { pozniej(e.getPlayer()); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void zamiana(PlayerSwapHandItemsEvent e) { pozniej(e.getPlayer()); }

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
    int teraz = getServer().getCurrentTick();
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

  /** Reakcje tylko dla zwyklego, naladowanego ciosu w glownej rece. HIGHEST: MythicLib liczy obrazenia broni wczesniej
   *  i nadpisalby zmiane z nizszego priorytetu (zmierzone: na HIGH mnoznik ciosu w plecy znikal). */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void reakcja(EntityDamageByEntityEvent e) {
    if (wlasnyCios || !(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity cel)
        || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
    Ustawienie u = ustawienie(p.getInventory().getItemInMainHand());
    if (u == null) return;
    int teraz = getServer().getCurrentTick();
    // riposta zuzywa sie przy trafieniu (klient wysyla cios PRZED machnieciem, wiec nie przez PlayerAnimationEvent)
    Riposta riposta = u.parowanie() != null ? riposty.remove(p.getUniqueId()) : null;
    if (riposta != null && riposta.doTicka() >= teraz) {
      e.setDamage(e.getDamage() * riposta.mnoznik());
      cel.getWorld().spawnParticle(Particle.CRIT, cel.getLocation().add(0, cel.getHeight() * 0.6, 0), 12, 0.2, 0.2, 0.2, 0.1);
      cel.getWorld().playSound(cel.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.6f, 1.2f);
    }
    // naladowanie z chwili ciosu: Paper resetuje je (PlayerAttackEntityCooldownResetEvent) dopiero PO tym zdarzeniu - zmierzone
    dbg("REAKCJA " + u.reakcja() + " cel=" + cel.getType() + " naladowanie=" + p.getAttackCooldown() + " dmg=" + e.getDamage());
    if (p.getAttackCooldown() < 0.9f) return;
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
        || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || e.getFinalDamage() <= 0) return;
    Ustawienie u = ustawienie(p.getInventory().getItemInMainHand());
    if (u == null || !"krwawienie".equals(u.reakcja()) || u.krwawienie() == null || p.getAttackCooldown() < 0.9f) return;
    Krwawienie k = u.krwawienie();
    int teraz = getServer().getCurrentTick();
    Krwawiacy stan = krwawiacy.computeIfAbsent(cel.getUniqueId(), id -> new Krwawiacy());
    while (stan.stosy.size() >= Math.max(1, k.stosy())) stan.stosy.remove(0);
    stan.stosy.add(new StosKrwawienia(e.getFinalDamage() * k.procent() / Math.max(1, k.czas() / 20), p.getUniqueId()));
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
      s.sendMessage("typ=" + typ + " swing=" + i.getData(DataComponentTypes.SWING_ANIMATION)
          + " model=" + i.getData(DataComponentTypes.ITEM_MODEL)
          + " ladowanie=" + i.getData(DataComponentTypes.MINIMUM_ATTACK_CHARGE)
          + " zasieg=" + i.getData(DataComponentTypes.ATTACK_RANGE)
          + " pierce=" + i.hasData(DataComponentTypes.PIERCING_WEAPON)
          + " szarza=" + i.hasData(DataComponentTypes.KINETIC_WEAPON)
          + " garda=" + i.hasData(DataComponentTypes.BLOCKS_ATTACKS)
          + " reakcja=" + (ustawienie(i) == null ? "brak" : ustawienie(i).reakcja())
          + " wypadZa=" + Math.max(0, odnowienieWypadu.getOrDefault(p.getUniqueId(), 0) - getServer().getCurrentTick())
          + " uderzenieZa=" + Math.max(0, odnowienieUderzenia.getOrDefault(p.getUniqueId(), 0) - getServer().getCurrentTick())
          + " ripostaZa=" + Math.max(0, riposty.getOrDefault(p.getUniqueId(), new Riposta(0, 1)).doTicka() - getServer().getCurrentTick())
          + " sprint=" + p.isSprinting()
          + " lewa=" + p.getInventory().getItemInOffHand().getType());
      return true;
    }
    return false;
  }
}
