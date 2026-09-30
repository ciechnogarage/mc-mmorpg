package pl.mcmmorpg.animacjebroni;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.AttackRange;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
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

  record Ustawienie(SwingAnimation.Animation zamach, int czas, String skala, Rodzaj rodzaj, Float ladowanie,
                    Float zasieg, Material wzor, Garda garda, boolean ciezka) {}

  /** Komponenty, ktore plugin ustawia; przy zmianie wracaja do wartosci materialu (resetData). */
  private static final List<DataComponentType> ZARZADZANE = List.of(
      DataComponentTypes.SWING_ANIMATION, DataComponentTypes.MINIMUM_ATTACK_CHARGE, DataComponentTypes.ATTACK_RANGE,
      DataComponentTypes.PIERCING_WEAPON, DataComponentTypes.KINETIC_WEAPON, DataComponentTypes.USE_EFFECTS,
      DataComponentTypes.BLOCKS_ATTACKS);
  private static final Set<DataComponentType> Z_WZORU_LPM = Set.of(
      DataComponentTypes.PIERCING_WEAPON, DataComponentTypes.ATTACK_RANGE, DataComponentTypes.MINIMUM_ATTACK_CHARGE);
  private static final Set<DataComponentType> Z_WZORU_PPM = Set.of(
      DataComponentTypes.KINETIC_WEAPON, DataComponentTypes.USE_EFFECTS);

  private final Map<String, Ustawienie> typy = new HashMap<>();
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
  }

  private static Float liczbaLubNull(ConfigurationSection t, String k) {
    return t.contains(k) ? (float) t.getDouble(k) : null;
  }

  private void wczytaj() {
    reloadConfig();
    typy.clear();
    modele.clear();
    przestrzen = getConfig().getString("przestrzen-modeli", "bron");
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
        typy.put(typ.toUpperCase(Locale.ROOT), new Ustawienie(zamach, t.getInt("czas", 6), t.getString("skala"), rodzaj,
            liczbaLubNull(t, "ladowanie"), liczbaLubNull(t, "zasieg"), wzor, garda, t.getBoolean("ciezka", false)));
      }
    }
    for (String m : getConfig().getStringList("materialy-z-modelami")) {
      Material mat = Material.matchMaterial(m);
      if (mat == null) getLogger().warning("Nieznany material w materialy-z-modelami: " + m);
      else modele.add(mat);
    }
    wersja = getConfig().saveToString().hashCode();
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
  boolean dopasuj(ItemStack item, boolean lewaPusta) {
    Ustawienie u = ustawienie(item);
    if (u == null) return false;
    boolean ppm = lewaPusta && !maUmiejetnoscPpm(item);
    String stan = wersja + ":" + ppm;
    if (stan.equals(item.getPersistentDataContainer().get(kluczStanu, PersistentDataType.STRING))) return false;

    for (DataComponentType t : ZARZADZANE) item.resetData(t);

    if (u.wzor() != null) {
      ItemStack wzor = new ItemStack(u.wzor());
      item.copyDataFrom(wzor, t -> Z_WZORU_LPM.contains(t) || (ppm && Z_WZORU_PPM.contains(t)));
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
    if (ppm && u.garda() != null) {
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

    if (u.skala() != null && modele.contains(item.getType())) {
      Key cel = Key.key(przestrzen, u.skala() + "/" + item.getType().getKey().getKey());
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
    if (dopasuj(glowna, lewaPusta)) inv.setItemInMainHand(glowna);
    if (dopasuj(druga, false)) inv.setItemInOffHand(druga);
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
  public void klik(InventoryClickEvent e) { pozniej(e.getWhoClicked()); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void przeciagniecie(InventoryDragEvent e) { pozniej(e.getWhoClicked()); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void podniesienie(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player p) pozniej(p); }

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
          + " lewa=" + p.getInventory().getItemInOffHand().getType());
      return true;
    }
    return false;
  }
}
