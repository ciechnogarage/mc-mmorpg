package pl.mcmmorpg.dwiebronie;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.AttackRange;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseEffects;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import net.Indyuce.mmocore.api.event.PlayerResourceUpdateEvent;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.kyori.adventure.key.Key;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Walka dwiema bronia na wzor Skyrima (wlasciciel 10.10).
 *
 * <p>Dziala, gdy w obu dloniach jest prawdziwa bron MMOItems (Ksiega: wybrany slot 1 = prawa, off-hand =
 * lewa) i postac spelnia wymagania lewej:
 * <ul>
 *   <li>LPM - ciosy naprzemiennie prawa / lewa (wybor wlasciciela),</li>
 *   <li>PPM stukniecie - cios lewa; PPM przytrzymany - mocny cios lewa (Skyrim: x2, stamina, zachwianie),</li>
 *   <li>Shift + PPM przytrzymany - podwojny mocny cios (Skyrim: 3 ataki L/P/L po x1,5),</li>
 *   <li>bez staminy mocny cios zamienia sie w zwykly (jak w Skyrimie),</li>
 *   <li>premia szybkosci ataku za dwie bronie (Skyrim: dwie bronie ~ +33% obrazen na sekunde).</li>
 * </ul>
 * MMOItems liczy obrazenia tylko z glownej reki, wiec cios lewa skaluje obrazenia proporcja ATTACK_DAMAGE
 * lewej do prawej (tylko glowny cel w tym ticku). Przytrzymanie PPM uzywa lewej: off-hand dostaje komponent
 * consumable (klient uzywa off-handu, gdy glowna reka nie ma uzycia) - lewa reka unosi sie do zamachu.
 */
public final class DwieBronie extends JavaPlugin implements Listener {

  /** cel = encja z ataku (moze byc pod-hitboxem ModelEngine), baza = encja, ktora faktycznie dostaje obrazenia. */
  private record Cios(UUID cel, UUID baza, int tick, double skala) {
    boolean trafia(UUID u) { return cel.equals(u) || baza.equals(u); }
  }

  private static final class Stan {
    int ostatniTick = -1000;
    boolean poprzedniaLewa;
    double wymuszonaSkala;      // >0 = najblizszy cios idzie lewa z ta skala (PPM)
    int lewaTick = -1000;
    int brakStaminyTick = -1000;
    int wymaganiaTick = -1000;
    UUID trafiony; int trafionyTick = -1000;
  }

  private final Map<UUID, Stan> stany = new HashMap<>();
  private final Map<UUID, Cios> ciosyLewa = new HashMap<>();
  private final Set<String> typyBroni = new java.util.HashSet<>();
  private NamespacedKey kluczUzbrojenia;
  private NamespacedKey kluczPremii;
  private boolean mmocore;
  private MostModelEngine mostMe;   // null = brak ModelEngine (softdepend)

  /** Pod-hitbox ModelEngine (osobna encja przy modelu) -> bazowa encja moba; inna encja bez zmian. */
  Entity baza(Entity e) {
    if (mostMe == null || e == null) return e;
    try { Entity b = mostMe.baza(e); return b == null ? e : b; }
    catch (Throwable ex) { return e; }
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    getConfig().getStringList("typy-broni").forEach(t -> typyBroni.add(t.toUpperCase(java.util.Locale.ROOT)));
    kluczUzbrojenia = new NamespacedKey(this, "ppm");
    kluczPremii = new NamespacedKey(this, "premia_szybkosci");
    mmocore = getServer().getPluginManager().isPluginEnabled("MMOCore");
    if (getServer().getPluginManager().isPluginEnabled("ModelEngine")) {
      try { mostMe = new MostModelEngine(); }
      catch (Throwable ex) { getLogger().warning("ModelEngine bez zgodnego API - pod-hitboxy bez mapowania: " + ex); }
    }
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getScheduler().runTaskTimer(this, () -> getServer().getOnlinePlayers().forEach(this::odswiez), 10L, 5L);
  }

  @Override
  public void onDisable() {
    getServer().getOnlinePlayers().forEach(p -> { rozbroj(p); zdejmijPremie(p); });
  }

  private int tick() { return getServer().getCurrentTick(); }

  private void dbg(String s) { if (getConfig().getBoolean("debug", false)) getLogger().info("DBDBG " + s); }

  // Przedmioty z wlasnym uzyciem w vanilli (strzal, rzut, blok) - consumable zabralby im PPM, a puszczenie
  // naciagu luku odpalaloby mocny cios lewa (zmierzone 12.10: strzal z luku w lewej zjadal 25% staminy).
  private static final Set<Material> Z_WLASNYM_UZYCIEM = EnumSet.of(Material.BOW, Material.CROSSBOW, Material.TRIDENT,
      Material.SHIELD);

  /** ATTACK_DAMAGE jednorecznej broni do walki wrecz z listy `typy-broni`; 0 dla wszystkiego innego. */
  double obrazenia(ItemStack item) {
    if (item == null || item.getType().isAir() || Z_WLASNYM_UZYCIEM.contains(item.getType())) return 0;
    NBTItem nbt = NBTItem.get(item);
    String typ = nbt.getString("MMOITEMS_ITEM_TYPE");
    if (typ == null || typ.isEmpty() || !typyBroni.contains(typ.toUpperCase(java.util.Locale.ROOT))) return 0;
    return nbt.getStat("ATTACK_DAMAGE");
  }

  // Lewa liczy sie tylko, gdy postac spelnia jej wymagania (poziom, klasa, bieglosc) - inaczej nowa
  // postac bilaby co drugi cios bronia z wysokiego poziomu trzymana w off-handzie.
  private boolean dwieBronie(Player p) {
    return obrazenia(p.getInventory().getItemInMainHand()) > 0 && obrazenia(p.getInventory().getItemInOffHand()) > 0
        && lewaDozwolona(p);
  }

  @SuppressWarnings("deprecation")
  private boolean lewaDozwolona(Player p) {
    PlayerData data = PlayerData.has(p) ? PlayerData.get(p) : null;
    return data != null && data.getRPG().canUse(NBTItem.get(p.getInventory().getItemInOffHand()), false);
  }

  // Prawa bron z wlasna umiejetnoscia pod PPM (MMOItems ability mode RIGHT_CLICK) zachowuje PPM dla siebie -
  // inaczej jeden klik odpalalby i umiejetnosc, i cios lewa.
  static boolean maUmiejetnoscPpm(ItemStack item) {
    if (item == null || item.getType().isAir()) return false;
    String ab = NBTItem.get(item).getString("MMOITEMS_ABILITY");
    return ab != null && ab.contains("RIGHT_CLICK");
  }

  private boolean ppmLewa(Player p) {
    return dwieBronie(p) && !maUmiejetnoscPpm(p.getInventory().getItemInMainHand());
  }

  private double skala(Player p) {
    return obrazenia(p.getInventory().getItemInOffHand()) / obrazenia(p.getInventory().getItemInMainHand());
  }

  // ------------------------------------------------------------------ uzbrojenie lewej i premia

  private boolean oznaczona(ItemStack item) {
    return item != null && !item.getType().isAir() && item.hasItemMeta()
        && item.getPersistentDataContainer().has(kluczUzbrojenia, PersistentDataType.BYTE);
  }

  // AnimacjeBroni resetuje CONSUMABLE przy pierwszym ulozeniu broni w off-handzie - sam znacznik nie wystarcza.
  private boolean uzbrojona(ItemStack item) {
    return oznaczona(item) && item.hasData(DataComponentTypes.CONSUMABLE);
  }

  private void uzbroj(ItemStack item) {
    item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().consumeSeconds(3600f)
        .animation(ItemUseAnimation.valueOf(getConfig().getString("poza-zamachu", "TRIDENT")))
        .hasConsumeParticles(false).sound(Key.key("minecraft", "intentionally_empty")).build());
    item.setData(DataComponentTypes.USE_EFFECTS, UseEffects.useEffects().canSprint(false)
        .speedMultiplier((float) getConfig().getDouble("spowolnienie-zamachu", 0.45)).interactVibrations(true).build());
    item.editPersistentDataContainer(pdc -> pdc.set(kluczUzbrojenia, PersistentDataType.BYTE, (byte) 1));
  }

  private ItemStack rozbrojony(ItemStack item) {
    if (!oznaczona(item)) return null;
    item.resetData(DataComponentTypes.CONSUMABLE);
    item.resetData(DataComponentTypes.USE_EFFECTS);
    item.editPersistentDataContainer(pdc -> pdc.remove(kluczUzbrojenia));
    return item;
  }

  private void rozbroj(Player p) {
    PlayerInventory inv = p.getInventory();
    ItemStack lewa = inv.getItemInOffHand();
    if (rozbrojony(lewa) != null) inv.setItemInOffHand(lewa);
    for (int i = 0; i < 36; i++) {
      ItemStack it = inv.getItem(i);
      if (rozbrojony(it) != null) inv.setItem(i, it);
    }
    ItemStack kursor = p.getItemOnCursor();
    if (rozbrojony(kursor) != null) p.setItemOnCursor(kursor);
  }

  private void zdejmijPremie(Player p) {
    AttributeInstance a = p.getAttribute(Attribute.ATTACK_SPEED);
    if (a != null) a.removeModifier(kluczPremii);
  }

  /** Co 5 tickow: lewa uzbrojona tylko przy dwoch broniach; bron poza off-handem nigdy nie ma consumable. */
  private void odswiez(Player p) {
    PlayerInventory inv = p.getInventory();
    boolean dwie = dwieBronie(p), ppm = dwie && !maUmiejetnoscPpm(inv.getItemInMainHand());
    for (int i = 0; i < 36; i++) {
      ItemStack it = inv.getItem(i);
      if (rozbrojony(it) != null) inv.setItem(i, it);
    }
    ItemStack kursor = p.getItemOnCursor();
    if (rozbrojony(kursor) != null) p.setItemOnCursor(kursor);
    ItemStack lewa = inv.getItemInOffHand();
    if (!p.isHandRaised()) {
      if (ppm && !uzbrojona(lewa)) { uzbroj(lewa); inv.setItemInOffHand(lewa); }
      else if (!ppm && rozbrojony(lewa) != null) inv.setItemInOffHand(lewa);
    }
    AttributeInstance a = p.getAttribute(Attribute.ATTACK_SPEED);
    double premia = getConfig().getDouble("premia-szybkosci-ataku", 0.33);
    if (a == null) return;
    boolean ma = a.getModifier(kluczPremii) != null;
    if (dwie && premia > 0 && !ma)
      a.addTransientModifier(new AttributeModifier(kluczPremii, premia,
          AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
    else if ((!dwie || premia <= 0) && ma) a.removeModifier(kluczPremii);
  }

  // ------------------------------------------------------------------ LPM: naprzemiennie

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void przedCiosem(PrePlayerAttackEntityEvent e) {
    Player p = e.getPlayer();
    Stan s = stany.computeIfAbsent(p.getUniqueId(), k -> new Stan());
    double wymuszona = s.wymuszonaSkala;
    s.wymuszonaSkala = 0;
    if (!dwieBronie(p) || !(e.getAttacked() instanceof LivingEntity)) return;
    int teraz = tick();
    boolean lewa = wymuszona > 0
        || teraz - s.ostatniTick <= getConfig().getInt("okno-serii-tickow", 40) && !s.poprzedniaLewa;
    s.ostatniTick = teraz;
    s.poprzedniaLewa = lewa;
    if (!lewa) { dbg(p.getName() + " cios PRAWA tick=" + teraz); return; }
    double k = wymuszona > 0 ? wymuszona : skala(p);
    // pod-hitbox ME (np. ostrze kosy) przekazuje obrazenia bazie moba - EntityDamageByEntity przychodzi na baze
    ciosyLewa.put(p.getUniqueId(), new Cios(e.getAttacked().getUniqueId(), baza(e.getAttacked()).getUniqueId(), teraz, k));
    // znacznik dla innych pluginow (Trafienia: luk ciosu lewej reki idzie od lewej): wazny tylko w tym ticku
    p.setMetadata("dwiebronie_lewa", new org.bukkit.metadata.FixedMetadataValue(this, teraz));
    s.lewaTick = teraz;
    if (wymuszona <= 0) {
      p.swingOffHand();
      // machniecie prawej jest wyciete, wiec AnimacjeBroni nie policzy tego lekkiego ciosu - placimy tu,
      // tick pozniej: zadyszka przy obrazeniach liczy sie ze stanu sprzed ciosu
      getServer().getScheduler().runTask(this, () -> { if (p.isOnline()) lekkiCios(p); });
    }
    dbg(p.getName() + " cios LEWA tick=" + teraz + " skala=" + String.format("%.3f", k));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void obrazenia(EntityDamageByEntityEvent e) {
    if (!(e.getDamager() instanceof Player p) || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
    Cios c = ciosyLewa.get(p.getUniqueId());
    if (c == null) return;
    if (c.tick() != tick()) { ciosyLewa.remove(p.getUniqueId()); return; }
    if (!c.trafia(e.getEntity().getUniqueId())) return;
    double przed = e.getDamage();
    e.setDamage(przed * c.skala());
    ciosyLewa.remove(p.getUniqueId());
    Stan s = stany.computeIfAbsent(p.getUniqueId(), k -> new Stan());
    s.trafiony = e.getEntity().getUniqueId();
    s.trafionyTick = tick();
    dbg(p.getName() + " obrazenia lewej " + String.format("%.2f -> %.2f", przed, e.getDamage()));
  }

  // Klient wysyla cios PRZED machnieciem: przy ciosie lewa nie rozsylaj innym machniecia prawa reka
  // (gracz u siebie i tak widzi prawa - to animacja lokalna klienta).
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void machniecie(PlayerAnimationEvent e) {
    if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
    Stan s = stany.get(e.getPlayer().getUniqueId());
    boolean wytnij = s != null && tick() - s.lewaTick <= 1;
    dbg(e.getPlayer().getName() + " machniecie tick=" + tick() + (wytnij ? " WYCIETE (cios lewa)" : ""));
    if (wytnij) e.setCancelled(true);
  }

  // ------------------------------------------------------------------ PPM: lewa (stukniecie / mocny / podwojny)

  @EventHandler(priority = EventPriority.MONITOR)
  public void startZamachu(PlayerInteractEvent e) {
    if (e.getHand() != EquipmentSlot.OFF_HAND || !uzbrojona(e.getItem())) return;
    if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    Player p = e.getPlayer();
    int pelne = getConfig().getInt("mocny.pelne-ticki", 12);
    getServer().getScheduler().runTaskLater(this, () -> {
      if (p.isOnline() && p.isHandRaised() && p.getActiveItemHand() == EquipmentSlot.OFF_HAND
          && p.getActiveItemUsedTime() >= pelne - 1) {
        p.playSound(p.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS, 0.6f, 1.4f);
      }
    }, pelne);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void puszczenie(PlayerStopUsingItemEvent e) {
    Player p = e.getPlayer();
    if (!uzbrojona(e.getItem()) || !ppmLewa(p)) return;
    int ticki = e.getTicksHeldFor();
    int prog = getConfig().getInt("mocny.prog-ticki", 6), pelne = getConfig().getInt("mocny.pelne-ticki", 12);
    String wariant = ticki < prog ? "stukniecie" : ticki < pelne ? "przerwany" : p.isSneaking() ? "podwojny" : "mocny";
    double l = skala(p), mnoznik;
    if (wariant.equals("podwojny")) {
      // Skyrim: 3 ataki (2x lewa, 1x prawa) po x1,5 - tu jednym trafieniem, trzy zamachy dla oka
      if (stamina(p, getConfig().getDouble("podwojny.koszt-staminy", 18), getConfig().getDouble("podwojny.koszt-jednostek", 4.0)))
        mnoznik = getConfig().getDouble("podwojny.mnoznik", 1.5) * (2 * l + 1);
      else { wariant = "podwojny-bez-staminy"; mnoznik = l; }
    } else if (wariant.equals("mocny")) {
      if (stamina(p, getConfig().getDouble("mocny.koszt-staminy", 12), getConfig().getDouble("mocny.koszt-jednostek", 2.5)))
        mnoznik = getConfig().getDouble("mocny.mnoznik", 2.0) * l;
      else { wariant = "mocny-bez-staminy"; mnoznik = l; }
    } else mnoznik = l;
    // stukniecie/przerwany i ciosy "bez staminy" to zwykly lekki cios lewa - tez wysilek (po trafieniu, jak w LPM)
    if (wariant.equals("stukniecie") || wariant.equals("przerwany") || wariant.endsWith("bez-staminy"))
      getServer().getScheduler().runTask(this, () -> { if (p.isOnline()) lekkiCios(p); });
    boolean naladowany = p.getAttackCooldown() >= (float) getConfig().getDouble("min-naladowanie-ppm", 0.9);
    String kierunek = wariant.equals("mocny") ? kierunek(p) : "brak";
    double dodatek = kierunek.equals("przod") ? getConfig().getDouble("mocny.wypad.zasieg", 1.5) : 0;
    Entity cel = naladowany ? cel(p, null, dodatek) : null;
    if (!kierunek.equals("brak")) ruchMocnego(p, kierunek, cel);
    dbg(p.getName() + " PPM " + wariant + " kierunek=" + kierunek + " ticki=" + ticki + " mnoznik="
        + String.format("%.3f", mnoznik) + " cel=" + (cel == null ? "-" : cel.getType()));
    p.swingOffHand();
    if (wariant.equals("podwojny")) {
      getServer().getScheduler().runTaskLater(this, () -> { if (p.isOnline()) p.swingMainHand(); }, 4L);
      getServer().getScheduler().runTaskLater(this, () -> { if (p.isOnline()) p.swingOffHand(); }, 8L);
    }
    if (cel == null) return;
    Stan s = stany.computeIfAbsent(p.getUniqueId(), k -> new Stan());
    s.wymuszonaSkala = mnoznik;
    p.attack(cel);
    s.wymuszonaSkala = 0;
    // efekty tylko po prawdziwym trafieniu - nie przy obrazeniach anulowanych przez ochrone (PvP w Stolicy, NPC)
    Entity bazaCelu = baza(cel);
    boolean trafil = s.trafionyTick == tick()
        && (cel.getUniqueId().equals(s.trafiony) || bazaCelu.getUniqueId().equals(s.trafiony));
    dbg(p.getName() + " PPM trafil=" + trafil + (bazaCelu != cel ? " baza=" + bazaCelu.getType() : ""));
    if (trafil && bazaCelu instanceof LivingEntity z && (wariant.equals("mocny") || wariant.equals("podwojny"))) efektMocnego(p, z, wariant);
  }

  /** Kierunek z klawiszy ruchu w chwili puszczenia PPM (jak w Skyrimie: przod > tyl > bok). */
  private String kierunek(Player p) {
    var in = p.getCurrentInput();
    if (in.isForward() && !in.isBackward()) return "przod";
    if (in.isBackward() && !in.isForward()) return "tyl";
    if (in.isLeft() != in.isRight()) return in.isLeft() ? "lewo" : "prawo";
    return "brak";
  }

  /** Skyrim: wypad do przodu (doskok, dalszy zasieg), cios z odskokiem, cios z unikiem w bok. */
  private void ruchMocnego(Player p, String kierunek, Entity cel) {
    Vector przod = p.getLocation().getDirection().setY(0);
    if (przod.lengthSquared() < 1e-4) return;
    przod.normalize();
    Vector ruch = switch (kierunek) {
      case "przod" -> {
        double dystans = cel == null ? 99 : cel.getLocation().distance(p.getLocation());
        // nie wpychaj w cel stojacy tuz przed nami - doskok tylko gdy jest do czego doskoczyc
        yield dystans > getConfig().getDouble("mocny.wypad.min-dystans", 2.2)
            ? przod.clone().multiply(getConfig().getDouble("mocny.wypad.sila", 0.75)) : new Vector();
      }
      case "tyl" -> przod.clone().multiply(-getConfig().getDouble("mocny.odskok.sila", 0.6));
      default -> new Vector(-przod.getZ(), 0, przod.getX()).multiply(
          (kierunek.equals("lewo") ? -1 : 1) * getConfig().getDouble("mocny.unik.sila", 0.6));
    };
    if (ruch.lengthSquared() < 1e-4) return;
    final Vector v = ruch.setY(0);   // bez podbicia: w powietrzu tarcie prawie nie hamuje (3,5+ bloku zamiast ~1)
    // tyl/bok: najpierw cios, potem krok (cios z odskokiem); przod: doskok razem z ciosem
    getServer().getScheduler().runTaskLater(this, () -> { if (p.isOnline()) p.setVelocity(p.getVelocity().add(v)); },
        kierunek.equals("przod") ? 0L : 2L);
  }

  /** Bron w lewej, ale postac nie spelnia jej wymagan: PPM nic nie robi - powiedz dlaczego (raz na 10 s). */
  private void informujOWymaganiach(Player p) {
    if (obrazenia(p.getInventory().getItemInMainHand()) <= 0 || obrazenia(p.getInventory().getItemInOffHand()) <= 0
        || lewaDozwolona(p)) return;
    Stan s = stany.computeIfAbsent(p.getUniqueId(), k -> new Stan());
    if (tick() - s.wymaganiaTick < 200) return;
    s.wymaganiaTick = tick();
    MMOPlayerData.get(p).getActionBar().show(40, 60L,
        "§x§F§2§F§2§E§DLewa broń nie działa §7- §x§E§8§7§9§7§3nie spełniasz jej wymagań");
  }

  // Model staminy AnimacjeBroni (jednostki lekkiego ciosu): metody szukane refleksja przy pierwszym uzyciu,
  // zeby DwieBronie nie zalezaly od AnimacjeBroni przy kompilacji. Brak pluginu/metody = stare koszty w %.
  private java.lang.reflect.Method apiKoszt, apiLekki;
  private Object apiAnimacje;
  private boolean apiSzukane;

  private void szukajApi() {
    if (apiSzukane) return;
    apiSzukane = true;
    var pl = getServer().getPluginManager().getPlugin("AnimacjeBroni");
    if (pl == null || !pl.isEnabled()) return;
    try {
      apiKoszt = pl.getClass().getMethod("kosztJednostek", Player.class, double.class);
      apiLekki = pl.getClass().getMethod("lekkiCiosZInnejReki", Player.class);
      apiAnimacje = pl;
    } catch (NoSuchMethodException ex) {
      getLogger().info("AnimacjeBroni bez modelu jednostek staminy - koszty w % z configu.");
    }
  }

  /** Punkty staminy za akcje warta {@code jednostki} lekkich ciosow; -1 = model jednostek niedostepny. */
  private double kosztModelu(Player p, double jednostki) {
    szukajApi();
    if (apiKoszt == null) return -1;
    try { return (double) apiKoszt.invoke(apiAnimacje, p, jednostki); }
    catch (ReflectiveOperationException ex) { return -1; }
  }

  private void lekkiCios(Player p) {
    szukajApi();
    if (apiLekki == null) return;
    try { apiLekki.invoke(apiAnimacje, p); } catch (ReflectiveOperationException ignored) { }
  }

  private boolean stamina(Player p, double procent, double jednostki) {
    if (!mmocore || procent <= 0) return true;
    double maks = Math.max(0, MMOPlayerData.get(p).getStatMap().getStat("MAX_STAMINA"));
    double model = kosztModelu(p, jednostki);
    double koszt = model >= 0 ? model : maks * procent / 100.0;
    net.Indyuce.mmocore.api.player.PlayerData dane = net.Indyuce.mmocore.api.player.PlayerData.get(p);
    dbg(p.getName() + " stamina maks=" + String.format("%.2f", maks) + " teraz="
        + (dane == null ? "-" : String.format("%.2f", dane.getStamina())) + " koszt=" + String.format("%.2f", koszt));
    if (dane != null && dane.getStamina() + 1e-6 >= koszt) {
      dane.giveStamina(-koszt, PlayerResourceUpdateEvent.UpdateReason.SKILL_COST);
      return true;
    }
    Stan s = stany.computeIfAbsent(p.getUniqueId(), k -> new Stan());
    if (tick() - s.brakStaminyTick >= 40) {
      s.brakStaminyTick = tick();
      p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 0.8f, 0.7f);
      MMOPlayerData.get(p).getActionBar().show(40, 40L, "§x§F§2§F§2§E§DBrakuje §x§E§8§7§9§7§3staminy");
    }
    return false;
  }

  /** Mocny cios: zachwianie (jak AnimacjeBroni), iskry, dzwiek; podwojny - trzy trafienia dla oka i ucha. */
  private void efektMocnego(Player p, LivingEntity cel, String wariant) {
    cel.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 2, false, true, true));
    Vector od = cel.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
    AttributeInstance kb = cel.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
    double odpornosc = kb == null ? 0 : Math.max(0, Math.min(1, kb.getValue()));
    if (od.lengthSquared() > 1e-4 && odpornosc < 1)
      cel.setVelocity(cel.getVelocity().add(od.normalize().multiply(0.35 * (1 - odpornosc)).setY(0.1 * (1 - odpornosc))));
    boolean podwojny = wariant.equals("podwojny");
    for (int i = 0; i < (podwojny ? 3 : 1); i++) {
      getServer().getScheduler().runTaskLater(this, () -> {
        if (cel.isDead()) return;
        Location l = cel.getLocation().add(0, cel.getHeight() * 0.6, 0);
        cel.playHurtAnimation(0);
        cel.getWorld().spawnParticle(Particle.SWEEP_ATTACK, l, 1, 0.3, 0.2, 0.3, 0);
        cel.getWorld().spawnParticle(Particle.CRIT, l, 15, 0.35, 0.35, 0.35, 0.3);
        cel.getWorld().playSound(l, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1f, podwojny ? 1.1f : 0.8f);
      }, i * 4L);
    }
    MMOPlayerData.get(p).getActionBar().show(30, 30L, podwojny ? "§6⚔ Podwójny mocny cios" : "§e⚔ Mocny cios lewą");
  }

  private double zasieg(Player p) {
    AttackRange r = p.getInventory().getItemInOffHand().getData(DataComponentTypes.ATTACK_RANGE);
    if (r != null) return r.maxReach();
    AttributeInstance a = p.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
    return a == null ? 3.0 : a.getValue();
  }

  private Entity cel(Player p, Entity klikniety) { return cel(p, klikniety, 0); }

  private Entity cel(Player p, Entity klikniety, double dodatek) {
    if (klikniety instanceof LivingEntity && !(klikniety instanceof ArmorStand)) return klikniety;
    RayTraceResult r = p.getWorld().rayTrace(p.getEyeLocation(), p.getEyeLocation().getDirection(), zasieg(p) + dodatek,
        FluidCollisionMode.NEVER, true, 0.3,
        x -> x instanceof LivingEntity && x != p && !(x instanceof ArmorStand) && !x.isDead());
    return r == null ? null : r.getHitEntity();
  }

  // Zapasowo: gdy lewa nie zostala jeszcze uzbrojona (pierwsze ticki po zalozeniu), PPM bije od razu.
  @EventHandler(priority = EventPriority.HIGH)
  public void ppmBezUzbrojenia(PlayerInteractEvent e) {
    if (e.getHand() != EquipmentSlot.HAND) return;
    if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    Player p = e.getPlayer();
    if (uzbrojona(p.getInventory().getItemInOffHand())) return;
    if (!ppmLewa(p)) { informujOWymaganiach(p); return; }
    if (p.getAttackCooldown() < (float) getConfig().getDouble("min-naladowanie-ppm", 0.9)) return;
    Entity cel = cel(p, null);
    p.swingOffHand();
    if (cel == null) return;
    Stan s = stany.computeIfAbsent(p.getUniqueId(), k -> new Stan());
    s.wymuszonaSkala = skala(p);
    p.attack(cel);
    s.wymuszonaSkala = 0;
  }

  // Uzbrojona bron poza off-handem nie moze wyjsc ze swiata gracza z consumable: wyrzucenie, skrzynia, zmiana slotu.
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void wyrzucenie(PlayerDropItemEvent e) {
    ItemStack it = e.getItemDrop().getItemStack();
    if (rozbrojony(it) != null) e.getItemDrop().setItemStack(it);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void zamkniecie(InventoryCloseEvent e) {
    Inventory gora = e.getView().getTopInventory();
    if (gora.getType() == InventoryType.CRAFTING || gora.getHolder() instanceof Player) return;
    for (int i = 0; i < gora.getSize(); i++) {
      ItemStack it = gora.getItem(i);
      if (rozbrojony(it) != null) gora.setItem(i, it);
    }
  }

  private void odswiezZaraz(Player p) {
    getServer().getScheduler().runTask(this, () -> { if (p.isOnline()) odswiez(p); });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void slot(PlayerItemHeldEvent e) { odswiezZaraz(e.getPlayer()); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void zamiana(PlayerSwapHandItemsEvent e) { odswiezZaraz(e.getPlayer()); }

  @EventHandler(priority = EventPriority.MONITOR)
  public void klik(InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player p) odswiezZaraz(p); }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) {
    rozbroj(e.getPlayer());
    zdejmijPremie(e.getPlayer());
    stany.remove(e.getPlayer().getUniqueId());
    ciosyLewa.remove(e.getPlayer().getUniqueId());
  }
}
