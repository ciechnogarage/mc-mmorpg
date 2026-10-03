package pl.mcmmorpg.ksiega;

import com.nexomc.nexo.api.NexoItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Dane panelu sa odczytywane i renderowane tylko na glownym watku. */
final class PanelDolny {
  private final KsiegaPostaci plugin;
  private final String titleSign;
  private final Map<Integer, Entry> entries;
  private final boolean papi;
  private final boolean nexo;
  private final Set<String> warnedNexoIds = ConcurrentHashMap.newKeySet();

  PanelDolny(KsiegaPostaci plugin) {
    this.plugin = plugin;
    titleSign = plugin.getConfig().getString("panel-dolny.znak-tytulu", "䅨");
    papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    nexo = Bukkit.getPluginManager().isPluginEnabled("Nexo");
    if (!papi) plugin.getLogger().warning("Brak PlaceholderAPI: panel Q pokaze kreski i ukryje liczniki.");
    if (!nexo) plugin.getLogger().warning("Brak Nexo: panel Q uzyje materialow z konfiguracji.");
    Map<Integer, Entry> configured = new HashMap<>();
    for (Map<?, ?> values : plugin.getConfig().getMapList("panel-dolny.przedmioty")) {
      Object rawSlot = values.get("slot");
      int slot;
      try { slot = Integer.parseInt(String.valueOf(rawSlot)); }
      catch (NumberFormatException ex) { plugin.getLogger().warning("Panel Q: niepoprawny slot " + rawSlot); continue; }
      if (slot < 0 || slot >= 36) {
        plugin.getLogger().warning("Panel Q: slot poza zakresem 0-35: " + slot);
        continue;
      }
      Entry entry = new Entry(slot, string(values, "material", "PAPER"),
          string(values, "nexo", ""), string(values, "item-model", ""),
          string(values, "nazwa", ""), strings(values.get("lore")),
          string(values, "ilosc", "1"), Boolean.parseBoolean(string(values, "ukryj-gdy-zero", "false")),
          strings(values.get("komendy")), string(values, "warunek-brak-zadania", ""),
          strings(values.get("lore-gdy-brak")));
      if (configured.putIfAbsent(slot, entry) != null)
        plugin.getLogger().warning("Panel Q: powtorzony slot " + slot);
    }
    entries = Collections.unmodifiableMap(configured);
  }

  boolean matches(String plain, String legacy) {
    return titleSign != null && !titleSign.isEmpty()
        && (plain.contains(titleSign) || legacy.contains(titleSign));
  }

  boolean hasAction(int slot, ItemStack visible) {
    if (slot >= 18 && slot <= 26) return plugin.portal.quickAction(slot, visible);
    Entry entry = entries.get(slot);
    return entry != null && visible != null && !visible.getType().isAir()
        && !entry.commands.isEmpty();
  }

  void run(Player player, int slot) {
    if (slot >= 18 && slot <= 26) {
      plugin.portal.quickClick(player, slot);
      return;
    }
    Entry entry = entries.get(slot);
    if (entry == null) return;
    for (String raw : entry.commands) {
      String line = resolve(player, raw).trim();
      if (line.equalsIgnoreCase("[close]")) {
        player.closeInventory();
      } else if (line.startsWith("[console] ")) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command(line.substring(10)));
      } else if (line.startsWith("[player] ")) {
        player.performCommand(command(line.substring(9)));
      } else if (line.startsWith("[menu] ")) {
        String menu = line.substring(7).trim();
        if (menu.matches("[a-zA-Z0-9_-]+"))
          Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open " + menu + " " + player.getName());
      }
    }
  }

  ItemStack[] render(Player player) {
    ItemStack[] result = new ItemStack[36];
    for (Entry entry : entries.values()) {
      int amount = quantity(resolve(player, entry.amount));
      if (amount <= 0 && entry.hideWhenZero) continue;
      ItemStack item = base(entry);
      if (item == null || item.getType().isAir()) continue;
      item.setAmount(Math.max(1, amount));
      ItemMeta meta = item.getItemMeta();
      meta.displayName(text(resolve(player, entry.name)));
      List<String> lore = entry.lore;
      if (!entry.noQuestCondition.isEmpty()) {
        String condition = resolve(player, entry.noQuestCondition);
        if (!papi || condition.isBlank() || condition.equals(entry.noQuestCondition))
          lore = entry.noQuestLore;
      }
      List<Component> renderedLore = new ArrayList<>(lore.size());
      for (String line : lore) renderedLore.add(text(resolve(player, line)));
      meta.lore(renderedLore);
      meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
      item.setItemMeta(meta);
      result[entry.slot] = item;
    }
    ItemStack[] quick = plugin.portal.quickItems(player);
    System.arraycopy(quick, 0, result, 18, quick.length);
    return result;
  }

  private ItemStack base(Entry entry) {
    Material material = Material.matchMaterial(entry.material);
    if (material == null || material.isAir()) material = Material.PAPER;
    if (nexo && !entry.nexoId.isEmpty()) {
      try {
        var builder = NexoItems.itemFromId(entry.nexoId);
        if (builder != null) return builder.build();
        if (warnedNexoIds.add(entry.nexoId))
          plugin.getLogger().warning("Panel Q: brak przedmiotu Nexo " + entry.nexoId);
      } catch (RuntimeException | LinkageError ex) {
        if (warnedNexoIds.add(entry.nexoId))
          plugin.getLogger().warning("Panel Q: blad Nexo " + entry.nexoId + ": " + ex.getMessage());
      }
    }
    ItemStack fallback = new ItemStack(material);
    if (!entry.itemModel.isEmpty()) {
      NamespacedKey model = NamespacedKey.fromString(entry.itemModel);
      if (model != null) {
        ItemMeta meta = fallback.getItemMeta();
        meta.setItemModel(model);
        fallback.setItemMeta(meta);
      }
    }
    return fallback;
  }

  private String resolve(Player player, String source) {
    String value = source.replace("%player_name%", player.getName());
    return papi ? PlaceholderAPI.setPlaceholders(player, value)
        : value.replaceAll("%[A-Za-z0-9_]+%", "—");
  }

  private static int quantity(String value) {
    try { return Math.min(99, Math.max(0, Integer.parseInt(value.trim()))); }
    catch (NumberFormatException ex) { return 0; }
  }

  /** Bez kursywy: klient domyslnie pochyla wlasne nazwy i opisy przedmiotow. */
  private static Component text(String value) {
    return LegacyComponentSerializer.legacySection().deserialize(colored(value))
        .decoration(TextDecoration.ITALIC, false);
  }

  private static String colored(String value) {
    return ChatColor.translateAlternateColorCodes('&', value);
  }

  private static String command(String value) {
    String trimmed = value.trim();
    return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
  }

  private static String string(Map<?, ?> values, String key, Object fallback) {
    Object value = values.containsKey(key) ? values.get(key) : fallback;
    return value == null ? "" : value.toString();
  }

  private static List<String> strings(Object value) {
    if (!(value instanceof List<?> list)) return List.of();
    return list.stream().map(String::valueOf).toList();
  }

  private record Entry(int slot, String material, String nexoId, String itemModel,
      String name, List<String> lore, String amount, boolean hideWhenZero,
      List<String> commands, String noQuestCondition, List<String> noQuestLore) {}
}
