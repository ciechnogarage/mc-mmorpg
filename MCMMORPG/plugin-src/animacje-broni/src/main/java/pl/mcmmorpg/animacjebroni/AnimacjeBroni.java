package pl.mcmmorpg.animacjebroni;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.SwingAnimation;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Zamach (komponent swing_animation) i model trzymania (item_model) dla broni MMOItems, wg typu MMOItems.
 * Komponenty liczy sie tylko w dloni, wiec przedmiot jest poprawiany, gdy trafia do reki (wejscie, zmiana slotu,
 * zamiana rak, klik w ekwipunku, podniesienie) oraz przegladem obu rak co 2 s (np. /mi give prosto do slotu).
 * ItemBuildEvent MMOItems 6.10 nie jest nigdzie wywolywany - nie ma haka na budowe przedmiotu.
 */
public final class AnimacjeBroni extends JavaPlugin implements Listener {

  private record Ustawienie(SwingAnimation.Animation zamach, int czas, String skala) {}

  private final Map<String, Ustawienie> typy = new HashMap<>();
  private final Set<Material> modele = new HashSet<>();
  private String przestrzen = "bron";

  @Override
  public void onEnable() {
    saveDefaultConfig();
    wczytaj();
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getScheduler().runTaskTimer(this,
        () -> getServer().getOnlinePlayers().forEach(this::dopasujDlonie), 40L, 40L);
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
        String z = t.getString("zamach");
        if (z != null) {
          try {
            zamach = SwingAnimation.Animation.valueOf(z.toUpperCase(Locale.ROOT));
          } catch (IllegalArgumentException e) {
            getLogger().warning("Typ " + typ + ": nieznany zamach '" + z + "' (whack|stab|none)");
          }
        }
        typy.put(typ.toUpperCase(Locale.ROOT), new Ustawienie(zamach, t.getInt("czas", 6), t.getString("skala")));
      }
    }
    for (String m : getConfig().getStringList("materialy-z-modelami")) {
      Material mat = Material.matchMaterial(m);
      if (mat == null) getLogger().warning("Nieznany material w materialy-z-modelami: " + m);
      else modele.add(mat);
    }
    getLogger().info("Typy broni: " + typy.size() + ", materialy z modelami trzymania: " + modele.size());
  }

  /** Zwraca true, gdy cos zmieniono. Przedmioty spoza MMOItems i typy spoza configu zostaja nietkniete. */
  boolean dopasuj(ItemStack item) {
    if (item == null || item.getType().isAir()) return false;
    String typ = NBTItem.get(item).getType();
    if (typ == null) return false;
    Ustawienie u = typy.get(typ.toUpperCase(Locale.ROOT));
    if (u == null) return false;
    boolean zmiana = false;

    if (u.zamach() != null) {
      SwingAnimation cel = SwingAnimation.swingAnimation().type(u.zamach()).duration(u.czas()).build();
      SwingAnimation jest = item.getData(DataComponentTypes.SWING_ANIMATION);
      if (jest == null || jest.type() != cel.type() || jest.duration() != cel.duration()) {
        item.setData(DataComponentTypes.SWING_ANIMATION, cel);
        zmiana = true;
      }
    }

    if (u.skala() != null && modele.contains(item.getType())) {
      Key cel = Key.key(przestrzen, u.skala() + "/" + item.getType().getKey().getKey());
      Key jest = item.getData(DataComponentTypes.ITEM_MODEL);
      // podmieniamy tylko model domyslny materialu albo nasz wlasny (inna skala) - model z Nexo/MMOItems zostaje
      boolean domyslny = jest == null || jest.equals(item.getType().getKey()) || jest.namespace().equals(przestrzen);
      if (domyslny && !cel.equals(jest)) {
        item.setData(DataComponentTypes.ITEM_MODEL, cel);
        zmiana = true;
      }
    }
    return zmiana;
  }

  private void dopasujDlonie(Player p) {
    PlayerInventory inv = p.getInventory();
    ItemStack glowna = inv.getItemInMainHand();
    if (dopasuj(glowna)) inv.setItemInMainHand(glowna);
    ItemStack druga = inv.getItemInOffHand();
    if (dopasuj(druga)) inv.setItemInOffHand(druga);
  }

  private void pozniej(HumanEntity kto) {
    // zwykly scheduler Bukkita: harmonogram encji (p.getScheduler()) Paper wylacza po smierci gracza
    if (kto instanceof Player p) getServer().getScheduler().runTask(this, () -> { if (p.isOnline()) dopasujDlonie(p); });
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
          + " model=" + i.getData(DataComponentTypes.ITEM_MODEL));
      return true;
    }
    return false;
  }
}
