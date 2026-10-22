package poc;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PoC: jakie zdarzenia dostaje serwer od VANILLOWEGO klienta w trzech wariantach "sceny":
 *  a) SPECTATOR + setSpectatorTarget(ItemDisplay)  - kamera na encji
 *  b) SPECTATOR + setSpectatorTarget(ArmorStand) + Interaction przed kamera
 *  c) ADVENTURE + zamrozenie atrybutami (movement_speed 0, jump_strength 0) + Interaction przed graczem
 * Wszystko idzie do logu z prefiksem [POC].
 */
public final class ScenePoc extends JavaPlugin implements Listener {

  @Override public void onEnable() { Bukkit.getPluginManager().registerEvents(this, this); }

  private void log(Player p, String s) { getLogger().info("[POC] " + p.getName() + " mode=" + p.getGameMode() + " " + s); }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Player p = sender instanceof Player pl ? pl : Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
    if (p == null || args.length == 0) return false;
    Location base = new Location(p.getWorld(), 0.5, -60, 0.5, 180f, 0f);
    switch (args[0]) {
      case "a" -> {
        reset(p);
        p.teleport(base);
        ItemDisplay cam = p.getWorld().spawn(base.clone().add(0, 1.62, 0), ItemDisplay.class, d -> d.setRotation(180f, 10f));
        p.setGameMode(GameMode.SPECTATOR);
        Bukkit.getScheduler().runTaskLater(this, () -> log(p, "setSpectatorTarget(ItemDisplay)=" + setTarget(p, cam)), 5L);
        spawnProps(base);
      }
      case "b" -> {
        reset(p);
        p.teleport(base);
        ArmorStand cam = p.getWorld().spawn(base, ArmorStand.class, a -> { a.setInvisible(true); a.setGravity(false); a.setMarker(true); a.setRotation(180f, 10f); });
        p.setGameMode(GameMode.SPECTATOR);
        Bukkit.getScheduler().runTaskLater(this, () -> log(p, "setSpectatorTarget(ArmorStand)=" + setTarget(p, cam)), 5L);
        spawnProps(base);
      }
      case "c" -> {
        reset(p);
        p.setGameMode(GameMode.ADVENTURE);
        p.teleport(base);
        p.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0);
        p.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(0);
        p.getInventory().clear();
        spawnProps(base);
        log(p, "freeze via attributes");
      }
      case "reset" -> reset(p);
      default -> { return false; }
    }
    return true;
  }

  private boolean setTarget(Player p, Entity e) { p.setSpectatorTarget(e); return p.getSpectatorTarget() != null; }

  /** Interaction 2x2 przed kamera (tarcza na klikniecia) + mannequin-zastepca (zombie bez AI). */
  private void spawnProps(Location base) {
    Location front = base.clone().add(0, 0, -2.5);
    base.getWorld().spawn(front, Interaction.class, i -> { i.setInteractionWidth(3f); i.setInteractionHeight(3f); i.setResponsive(true); });
    base.getWorld().spawn(base.clone().add(0, 0, -4), Zombie.class, z -> { z.setAI(false); z.setSilent(true); z.setPersistent(false); });
  }

  private void reset(Player p) {
    if (p.getGameMode() == GameMode.SPECTATOR) p.setSpectatorTarget(null);
    p.getWorld().setTime(6000);
    p.getWorld().getEntities().stream().filter(e -> !(e instanceof Player)).forEach(Entity::remove);
    p.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1);
    p.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(0.42);
    p.setGameMode(GameMode.ADVENTURE);
    p.getInventory().setItem(0, new ItemStack(Material.STICK));
  }

  @EventHandler public void input(PlayerInputEvent e) { log(e.getPlayer(), "PlayerInputEvent " + e.getInput()); }
  @EventHandler public void anim(PlayerAnimationEvent e) { log(e.getPlayer(), "PlayerAnimationEvent " + e.getAnimationType()); }
  @EventHandler public void held(PlayerItemHeldEvent e) { log(e.getPlayer(), "PlayerItemHeldEvent " + e.getPreviousSlot() + "->" + e.getNewSlot()); }
  @EventHandler public void interact(PlayerInteractEvent e) { log(e.getPlayer(), "PlayerInteractEvent " + e.getAction() + " hand=" + e.getHand()); }
  @EventHandler public void interactEntity(PlayerInteractEntityEvent e) { log(e.getPlayer(), "PlayerInteractEntityEvent " + e.getRightClicked().getType() + " hand=" + e.getHand()); }
  @EventHandler public void interactAt(PlayerInteractAtEntityEvent e) { log(e.getPlayer(), "PlayerInteractAtEntityEvent " + e.getRightClicked().getType()); }
  @EventHandler public void preAttack(PrePlayerAttackEntityEvent e) { log(e.getPlayer(), "PrePlayerAttackEntityEvent " + e.getAttacked().getType()); e.setCancelled(true); }
  @EventHandler public void startSpec(PlayerStartSpectatingEntityEvent e) {
    log(e.getPlayer(), "PlayerStartSpectatingEntityEvent new=" + e.getNewSpectatorTarget().getType() + " cur=" + e.getCurrentSpectatorTarget().getType());
    // Klik w cos innego niz nasza kamera = "klik" w scenie; nie przelaczaj kamery.
    if (!(e.getNewSpectatorTarget() instanceof ItemDisplay) && !(e.getNewSpectatorTarget() instanceof ArmorStand)) e.setCancelled(true);
  }
  @EventHandler public void stopSpec(PlayerStopSpectatingEntityEvent e) { log(e.getPlayer(), "PlayerStopSpectatingEntityEvent (cancel)"); e.setCancelled(true); }
  @EventHandler public void drop(PlayerDropItemEvent e) { log(e.getPlayer(), "PlayerDropItemEvent"); }
  @EventHandler public void swap(PlayerSwapHandItemsEvent e) { log(e.getPlayer(), "PlayerSwapHandItemsEvent"); e.setCancelled(true); }
  @EventHandler public void move(PlayerMoveEvent e) {
    if (e.hasChangedPosition()) log(e.getPlayer(), "PlayerMoveEvent pos " + e.getTo().toVector());
  }
}
