package pl.mcmmorpg.ksiega;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class KsiegaPostaci extends JavaPlugin implements Listener {
  Dane dane;
  Bizuteria bizuteria;
  Sakwa sakwa;
  Ksiazka ksiazka;
  Bron bron;
  UkrywanieEkwipunku ukrywanie;
  Portal portal;
  PortalPlaceholdery placeholdery;

  @Override public void onEnable() {
    saveDefaultConfig();
    dane = new Dane(this);
    bizuteria = new Bizuteria(this);
    sakwa = new Sakwa(this);
    ksiazka = new Ksiazka(this);
    bron = new Bron(this);
    ukrywanie = new UkrywanieEkwipunku(this);
    portal = new Portal(this);
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      placeholdery = new PortalPlaceholdery(this);
      placeholdery.register();
    }
    getCommand("ksiegaportal").setExecutor(portal);
    MMOItems.plugin.getInventory().register(bizuteria.supplier());
    for (Listener listener : new Listener[] {this, bizuteria, sakwa, ksiazka, bron, ukrywanie,
        portal, new PotwierdzenieZadania(this)}) {
      Bukkit.getPluginManager().registerEvents(listener, this);
    }
    if (Bukkit.getPluginManager().isPluginEnabled("MMOProfiles"))
      Bukkit.getPluginManager().registerEvents(new ProfileEvents(this), this);
    for (Player player : Bukkit.getOnlinePlayers()) refreshLater(player);
  }

  @Override public void onDisable() {
    if (ukrywanie != null) ukrywanie.stop();
    if (portal != null) portal.stop();
    if (placeholdery != null) placeholdery.unregister();
    for (Player player : Bukkit.getOnlinePlayers()) {
      bizuteria.clearView(player);
      bron.clearClone(player);
    }
    dane.saveAll();
    if (bizuteria != null) MMOItems.plugin.getInventory().unregisterIf(s -> s == bizuteria.supplier());
  }

  void message(Player player, String key) {
    player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
        .deserialize(getConfig().getString("komunikaty." + key, "&cBlad.")));
  }

  void refreshLater(Player player) {
    Bukkit.getScheduler().runTaskLater(this, () -> {
      if (!player.isOnline() || player.getWorld().getName().equals("creator_limbo")) return;
      Dane.Record record = dane.current(player);
      if (record == null) return;
      bron.canonical(player);
      bizuteria.show(player);
      sakwa.show(player);
      bron.refresh(player);
      ksiazka.reset(player);
    }, 2L);
  }

  @EventHandler public void join(PlayerJoinEvent e) {
    bron.canonical(e.getPlayer());
    refreshLater(e.getPlayer());
  }
  @EventHandler public void respawn(PlayerRespawnEvent e) { refreshLater(e.getPlayer()); }
  @EventHandler public void world(PlayerChangedWorldEvent e) {
    bron.canonical(e.getPlayer());
    refreshLater(e.getPlayer());
  }
  @EventHandler public void quit(PlayerQuitEvent e) {
    if (ukrywanie != null) ukrywanie.quit(e.getPlayer());
    dane.unload(e.getPlayer());
  }

}
