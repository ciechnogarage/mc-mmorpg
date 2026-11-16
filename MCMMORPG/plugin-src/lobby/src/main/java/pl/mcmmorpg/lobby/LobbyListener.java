package pl.mcmmorpg.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Zachowanie gracza w lobby: ochrona, powrot z pustki, podwojny skok, portale, menu. */
final class LobbyListener implements Listener {

  private static final MiniMessage MM = MiniMessage.miniMessage();
  /** Punkt, w ktory celuje wyrzutnia na poczatku glownej sciezki (IslandBuilder.PAD_Z). */
  private static final Vector PAD_TARGET = new Vector(0.5, IslandBuilder.TOP + 1, -15.5);

  private final LobbyPlugin plugin;
  private final NamespacedKey itemKey;
  private final Map<UUID, Long> portalCooldown = new HashMap<>();
  private final Map<UUID, Long> padCooldown = new HashMap<>();

  LobbyListener(LobbyPlugin plugin) {
    this.plugin = plugin;
    this.itemKey = new NamespacedKey(plugin, "item");
  }

  // ------------------------------------------------------------------ wejscie

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    Player p = e.getPlayer();
    e.joinMessage(MM.deserialize("<dark_gray>» <gray>" + p.getName()));
    p.teleport(plugin.spawn());
    if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
      p.setGameMode(GameMode.ADVENTURE);
    }
    p.getActivePotionEffects().forEach(fx -> p.removePotionEffect(fx.getType()));
    p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
    p.setFoodLevel(20);
    p.setSaturation(20);
    p.setFireTicks(0);
    p.setExp(0);
    p.setLevel(0);
    giveItems(p);
    enableDoubleJump(p);

    p.showTitle(Title.title(plugin.networkName(), MM.deserialize("<gray>Witaj, <white>" + p.getName()),
        Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2200), Duration.ofMillis(700))));
    p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
    p.sendMessage(MM.deserialize("<newline>  <gray>Wejdź w <light_purple>portal</light_purple> na północy"
        + " albo kliknij <yellow>kompas</yellow>, żeby wybrać świat."
        + "<newline>  <dark_gray>W powietrzu wciśnij spację drugi raz - podwójny skok.<newline>"));

    // Gracz, ktory wpada do lobby w czasie restartu swiata, zwykle na ten swiat czeka.
    Portal auto = plugin.autoQueuePortal();
    if (auto != null) waitForStatusThenQueue(p, auto, 0);
  }

  private void waitForStatusThenQueue(Player p, Portal portal, int tries) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!p.isOnline()) return;
      if (!plugin.statusKnown()) {
        if (tries < 15) waitForStatusThenQueue(p, portal, tries + 1);
        return;
      }
      if (!portal.ready(plugin.neededPings()) && plugin.queued(p) == null) plugin.enqueue(p, portal, true);
    }, 20L);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    e.quitMessage(null);
    plugin.forget(e.getPlayer());
    portalCooldown.remove(e.getPlayer().getUniqueId());
    padCooldown.remove(e.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent e) {
    e.setRespawnLocation(plugin.spawn());
    Bukkit.getScheduler().runTask(plugin, () -> {
      giveItems(e.getPlayer());
      enableDoubleJump(e.getPlayer());
    });
  }

  // ------------------------------------------------------------------ przedmioty i menu

  private void giveItems(Player p) {
    if (p.getGameMode() == GameMode.CREATIVE) return;
    p.getInventory().clear();
    ItemStack compass = new ItemStack(Material.COMPASS);
    ItemMeta meta = compass.getItemMeta();
    meta.displayName(MM.deserialize("<yellow><b>Wybór świata</b></yellow> <gray>(PPM)").decoration(TextDecoration.ITALIC, false));
    meta.lore(List.of(MM.deserialize("<gray>Portale, status serwerów i kolejka").decoration(TextDecoration.ITALIC, false)));
    meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "menu");
    meta.setEnchantmentGlintOverride(true);
    compass.setItemMeta(meta);
    p.getInventory().setItem(4, compass);
    p.getInventory().setHeldItemSlot(4);
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    Player p = e.getPlayer();
    if (e.getAction() == Action.PHYSICAL) {
      Block b = e.getClickedBlock();
      if (b != null && b.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) launch(p);
      else if (b != null && p.getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
      return;
    }
    ItemStack item = e.getItem();
    boolean isMenu = item != null && item.hasItemMeta()
        && "menu".equals(item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING));
    if (isMenu && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
      e.setCancelled(true);
      openMenu(p);
      return;
    }
    if (e.getAction() == Action.RIGHT_CLICK_BLOCK && p.getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
  }

  static final class MenuHolder implements InventoryHolder {
    final Map<Integer, Portal> slots = new HashMap<>();
    int leaveSlot = -1;
    Inventory inv;

    @Override
    public Inventory getInventory() {
      return inv;
    }
  }

  private void openMenu(Player p) {
    MenuHolder holder = new MenuHolder();
    Inventory inv = Bukkit.createInventory(holder, 27, MM.deserialize("<dark_gray>Wybierz świat"));
    holder.inv = inv;
    ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "), List.of());
    for (int i = 0; i < 27; i++) inv.setItem(i, pane);

    LobbyPlugin.Queued q = plugin.queued(p);
    int needed = plugin.neededPings();
    for (Portal portal : plugin.portals().values()) {
      List<Component> lore = new ArrayList<>();
      if (portal.active() && !portal.description.isEmpty()) lore.add(mm("<gray>" + portal.description));
      lore.add(mm(portal.statusLine(needed)));
      lore.add(Component.empty());
      if (!portal.active()) lore.add(mm("<dark_gray>Jeszcze zamknięte"));
      else if (q != null && q.portal == portal) lore.add(mm("<yellow>Jesteś w kolejce"));
      else if (portal.ready(needed)) lore.add(mm("<green>▶ Kliknij, aby przejść"));
      else lore.add(mm("<yellow>▶ Kliknij, aby czekać w kolejce"));
      Material icon = portal.active() ? portal.icon : Material.GRAY_DYE;
      ItemStack it = named(icon, portal.name, lore);
      if (portal.active() && portal.ready(needed)) {
        ItemMeta m = it.getItemMeta();
        m.setEnchantmentGlintOverride(true);
        it.setItemMeta(m);
        it.setAmount(Math.max(1, Math.min(64, portal.players)));
      }
      inv.setItem(portal.slot.menuSlot, it);
      holder.slots.put(portal.slot.menuSlot, portal);
    }
    if (q != null) {
      holder.leaveSlot = 22;
      inv.setItem(22, named(Material.BARRIER, mm("<red><b>Opuść kolejkę"), List.of(mm("<gray>Zostaniesz w lobby"))));
    }
    p.openInventory(inv);
    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
  }

  private static Component mm(String s) {
    return MM.deserialize(s).decoration(TextDecoration.ITALIC, false);
  }

  private static ItemStack named(Material m, Component name, List<Component> lore) {
    ItemStack it = new ItemStack(m);
    ItemMeta meta = it.getItemMeta();
    meta.displayName(name.decoration(TextDecoration.ITALIC, false));
    meta.lore(lore);
    it.setItemMeta(meta);
    return it;
  }

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player p)) return;
    if (e.getInventory().getHolder() instanceof MenuHolder holder) {
      e.setCancelled(true);
      if (e.getClickedInventory() != e.getInventory()) return;
      int slot = e.getSlot();
      if (slot == holder.leaveSlot) {
        p.closeInventory();
        plugin.leaveQueue(p);
        return;
      }
      Portal portal = holder.slots.get(slot);
      if (portal == null) return;
      p.closeInventory();
      plugin.travel(p, portal);
      return;
    }
    if (p.getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
  }

  @EventHandler
  public void onDrag(InventoryDragEvent e) {
    if (e.getInventory().getHolder() instanceof MenuHolder
        || (e.getWhoClicked() instanceof Player p && p.getGameMode() != GameMode.CREATIVE)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onDrop(PlayerDropItemEvent e) {
    if (e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
  }

  @EventHandler
  public void onSwap(PlayerSwapHandItemsEvent e) {
    if (e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
  }

  // ------------------------------------------------------------------ ruch: portale, pustka, skok

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMove(PlayerMoveEvent e) {
    Player p = e.getPlayer();
    Location to = e.getTo();
    if (to.getY() < 30) {
      p.teleport(plugin.spawn());
      p.setFallDistance(0);
      p.playSound(plugin.spawn(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
      p.sendActionBar(MM.deserialize("<aqua>Wyspa cię złapała."));
      enableDoubleJump(p);
      return;
    }
    if (e.hasChangedBlock()) {
      Block feet = to.getBlock();
      Block head = feet.getRelative(0, 1, 0);
      if (feet.getType() == Material.NETHER_PORTAL || head.getType() == Material.NETHER_PORTAL) {
        long now = System.currentTimeMillis();
        Long last = portalCooldown.get(p.getUniqueId());
        if (last == null || now - last > 3000) {
          portalCooldown.put(p.getUniqueId(), now);
          Portal portal = plugin.nearestPortal(to);
          if (portal != null) plugin.travel(p, portal);
        }
      }
    }
    if (!p.getAllowFlight() && canDoubleJump(p) && isOnGround(p)) p.setAllowFlight(true);
  }

  /** Vanillowe przejscie przez portal (do netheru) nie ma w lobby sensu. */
  @EventHandler
  public void onPortal(PlayerPortalEvent e) {
    e.setCancelled(true);
  }

  private boolean canDoubleJump(Player p) {
    return p.getGameMode() == GameMode.ADVENTURE || p.getGameMode() == GameMode.SURVIVAL;
  }

  private static boolean isOnGround(Player p) {
    Location l = p.getLocation();
    return !l.clone().subtract(0, 0.2, 0).getBlock().isPassable();
  }

  private void enableDoubleJump(Player p) {
    if (canDoubleJump(p)) {
      p.setFlying(false);
      p.setAllowFlight(true);
    }
  }

  @EventHandler
  public void onToggleFlight(PlayerToggleFlightEvent e) {
    Player p = e.getPlayer();
    if (!canDoubleJump(p) || !e.isFlying()) return;
    e.setCancelled(true);
    p.setFlying(false);
    p.setAllowFlight(false);
    Vector v = p.getLocation().getDirection().setY(0);
    if (v.lengthSquared() > 0) v.normalize().multiply(0.75);
    p.setVelocity(v.setY(0.85));
    p.playSound(p.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.6f, 1.3f);
    p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 8, 0.25, 0.05, 0.25, 0.02);
  }

  private void launch(Player p) {
    long now = System.currentTimeMillis();
    Long last = padCooldown.get(p.getUniqueId());
    if (last != null && now - last < 1500) return;
    padCooldown.put(p.getUniqueId(), now);
    Vector flat = PAD_TARGET.clone().subtract(p.getLocation().toVector()).setY(0);
    // ~37 tickow lotu przy vy=1.0, opor powietrza 0.91/tick -> zasieg ~10.5 * vh
    double vh = Math.min(1.4, flat.length() / 10.5);
    Vector v = flat.normalize().multiply(vh).setY(1.0);
    p.setVelocity(v);
    p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7f, 1.1f);
    p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation(), 20, 0.3, 0.1, 0.3, 0.08);
  }

  // ------------------------------------------------------------------ ochrona

  @EventHandler
  public void onDamage(EntityDamageEvent e) {
    if (!(e.getEntity() instanceof Player p)) return;
    e.setCancelled(true);
    if (e.getCause() == EntityDamageEvent.DamageCause.VOID) p.teleport(plugin.spawn());
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent e) {
    e.deathMessage(null);
    e.getDrops().clear();
  }

  @EventHandler
  public void onFood(FoodLevelChangeEvent e) {
    e.setCancelled(true);
    e.getEntity().setFoodLevel(20);
  }

  @EventHandler
  public void onBreak(BlockBreakEvent e) {
    if (e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
  }

  @EventHandler
  public void onPlace(BlockPlaceEvent e) {
    if (e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
  }

  @EventHandler
  public void onWeather(WeatherChangeEvent e) {
    if (e.toWeatherState()) e.setCancelled(true);
  }
}
