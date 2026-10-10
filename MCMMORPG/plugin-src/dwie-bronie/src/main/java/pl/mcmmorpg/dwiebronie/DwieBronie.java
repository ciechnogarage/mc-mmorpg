package pl.mcmmorpg.dwiebronie;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.AttackRange;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import java.util.HashMap;
import net.Indyuce.mmoitems.api.player.PlayerData;
import java.util.Map;
import java.util.UUID;
import org.bukkit.FluidCollisionMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

/**
 * Walka dwiema bronia (wlasciciel 10.10: "rob 1 z PPM jak w 3").
 *
 * <p>Gdy w obu dloniach jest prawdziwa bron MMOItems (wybrany slot 1 = prawa, off-hand = lewa):
 * LPM bije naprzemiennie prawa i lewa, PPM bije zawsze lewa. MMOItems liczy obrazenia tylko z glownej
 * reki, wiec cios lewa skaluje obrazenia proporcja ATTACK_DAMAGE lewej do prawej (w tym samym ticku,
 * tylko glowny cel - rozprysk MMOItems zostaje bez zmian). Klon lewej (ksiega:klon) nie ma danych
 * MMOItems, wiec przy wybranym slocie 2 plugin nic nie robi.
 */
public final class DwieBronie extends JavaPlugin implements Listener {

  private record Cios(UUID cel, int tick, double skala) {}

  private static final class Stan {
    int ostatniTick = -1000;
    boolean poprzedniaLewa;
    boolean wymuszonaLewa;
    int ppmTick = -1000;
    int lewaTick = -1000;
  }

  private final Map<UUID, Stan> stany = new HashMap<>();
  private final Map<UUID, Cios> ciosyLewa = new HashMap<>();

  @Override
  public void onEnable() {
    saveDefaultConfig();
    getServer().getPluginManager().registerEvents(this, this);
  }

  private int tick() { return getServer().getCurrentTick(); }

  private void dbg(String s) { if (getConfig().getBoolean("debug", false)) getLogger().info("DBDBG " + s); }

  static double obrazenia(ItemStack item) {
    if (item == null || item.getType().isAir()) return 0;
    NBTItem nbt = NBTItem.get(item);
    if (nbt.getString("MMOITEMS_ITEM_TYPE") == null || nbt.getString("MMOITEMS_ITEM_TYPE").isEmpty()) return 0;
    return nbt.getStat("ATTACK_DAMAGE");
  }

  // Lewa liczy sie tylko, gdy postac spelnia jej wymagania (poziom, klasa, bieglosc) - inaczej nowa
  // postac bilaby co drugi cios bronia z wysokiego poziomu trzymana w off-handzie.
  private boolean dwieBronie(Player p) {
    ItemStack lewa = p.getInventory().getItemInOffHand();
    if (obrazenia(p.getInventory().getItemInMainHand()) <= 0 || obrazenia(lewa) <= 0) return false;
    PlayerData data = PlayerData.has(p) ? PlayerData.get(p) : null;
    return data != null && data.getRPG().canUse(NBTItem.get(lewa), false);
  }

  private double skala(Player p) {
    return obrazenia(p.getInventory().getItemInOffHand()) / obrazenia(p.getInventory().getItemInMainHand());
  }

  // ------------------------------------------------------------------ LPM: naprzemiennie

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void przedCiosem(PrePlayerAttackEntityEvent e) {
    Player p = e.getPlayer();
    Stan s = stany.computeIfAbsent(p.getUniqueId(), k -> new Stan());
    if (!dwieBronie(p) || !(e.getAttacked() instanceof LivingEntity)) {
      s.wymuszonaLewa = false;
      return;
    }
    int teraz = tick();
    boolean lewa;
    if (s.wymuszonaLewa) lewa = true;
    else lewa = teraz - s.ostatniTick <= getConfig().getInt("okno-serii-tickow", 40) && !s.poprzedniaLewa;
    s.wymuszonaLewa = false;
    s.ostatniTick = teraz;
    s.poprzedniaLewa = lewa;
    if (!lewa) { dbg(p.getName() + " cios PRAWA tick=" + teraz); return; }
    double k = skala(p);
    ciosyLewa.put(p.getUniqueId(), new Cios(e.getAttacked().getUniqueId(), teraz, k));
    s.lewaTick = teraz;
    p.swingOffHand();
    dbg(p.getName() + " cios LEWA tick=" + teraz + " skala=" + String.format("%.3f", k));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void obrazenia(EntityDamageByEntityEvent e) {
    if (!(e.getDamager() instanceof Player p) || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
    Cios c = ciosyLewa.get(p.getUniqueId());
    if (c == null) return;
    if (c.tick() != tick()) { ciosyLewa.remove(p.getUniqueId()); return; }
    if (!c.cel().equals(e.getEntity().getUniqueId())) return;
    double przed = e.getDamage();
    e.setDamage(przed * c.skala());
    ciosyLewa.remove(p.getUniqueId());
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

  // ------------------------------------------------------------------ PPM: zawsze lewa

  @EventHandler(priority = EventPriority.HIGH)
  public void ppm(PlayerInteractEvent e) {
    if (e.getHand() != EquipmentSlot.HAND) return;
    if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (ciosLewa(e.getPlayer(), null)) e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void ppmIstota(PlayerInteractEntityEvent e) {
    if (e.getHand() != EquipmentSlot.HAND) return;
    ciosLewa(e.getPlayer(), e.getRightClicked());
  }

  private double zasieg(Player p) {
    ItemStack lewa = p.getInventory().getItemInOffHand();
    AttackRange r = lewa.getData(DataComponentTypes.ATTACK_RANGE);
    if (r != null) return r.maxReach();
    AttributeInstance a = p.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
    return a == null ? 3.0 : a.getValue();
  }

  private boolean ciosLewa(Player p, Entity klikniety) {
    if (!dwieBronie(p)) return false;
    Stan s = stany.computeIfAbsent(p.getUniqueId(), k -> new Stan());
    int teraz = tick();
    if (s.ppmTick == teraz) return true;
    if (p.getAttackCooldown() < (float) getConfig().getDouble("min-naladowanie-ppm", 0.9)) return false;
    Entity cel = klikniety instanceof LivingEntity && !(klikniety instanceof ArmorStand) ? klikniety : null;
    if (cel == null) {
      RayTraceResult r = p.getWorld().rayTrace(p.getEyeLocation(), p.getEyeLocation().getDirection(), zasieg(p),
          FluidCollisionMode.NEVER, true, 0.3,
          x -> x instanceof LivingEntity && x != p && !(x instanceof ArmorStand) && !x.isDead());
      cel = r == null ? null : r.getHitEntity();
    }
    if (cel == null) { p.swingOffHand(); s.ppmTick = teraz; return false; }
    s.ppmTick = teraz;
    s.wymuszonaLewa = true;
    p.attack(cel);
    if (s.wymuszonaLewa) {
      // attack() nie wywolal PrePlayerAttackEntityEvent (cel nietrafialny) - nie zostawiaj flagi
      s.wymuszonaLewa = false;
      dbg(p.getName() + " PPM bez ciosu");
    }
    return true;
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) {
    stany.remove(e.getPlayer().getUniqueId());
    ciosyLewa.remove(e.getPlayer().getUniqueId());
  }
}
