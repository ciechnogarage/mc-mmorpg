package pl.mcmmorpg.mapa;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

final class ZnacznikDialog implements Listener {
  private record Pending(String world, int x, int z, String id) {}
  private final MapaPlugin plugin;
  private final Map<UUID, Pending> pending = new HashMap<>();

  ZnacznikDialog(MapaPlugin plugin) {
    this.plugin = plugin;
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  void forget(Player player) { pending.remove(player.getUniqueId()); }

  void add(Player player, String world, int x, int z) {
    pending.put(player.getUniqueId(), new Pending(world, x, z, ""));
    show(player, "Nowy znacznik", "Mój znacznik", firstIcon(), "");
  }

  void edit(Player player, MapaData.Marker marker) {
    pending.put(player.getUniqueId(), new Pending(marker.world(), marker.x(), marker.z(), marker.id()));
    show(player, "Znacznik: " + marker.name(), marker.name(), marker.icon(), marker.id());
  }

  private String firstIcon() {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("wlasne.ikony");
    return section == null || section.getKeys(false).isEmpty() ? "pin_czerwona"
        : section.getKeys(false).iterator().next();
  }

  private ActionButton button(String title, String action) {
    return ActionButton.builder(Component.text(title))
        .action(action == null ? null : DialogAction.customClick(Key.key("mapa:" + action), null)).build();
  }

  private void show(Player player, String title, String name, String icon, String id) {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("wlasne.ikony");
    List<SingleOptionDialogInput.OptionEntry> options = new ArrayList<>();
    if (section != null) for (String key : section.getKeys(false)) {
      if (!key.matches("[a-z0-9_-]+")) continue;
      options.add(SingleOptionDialogInput.OptionEntry.create(key,
          Component.text(section.getString(key, key)), key.equals(icon)));
    }
    if (options.isEmpty()) options.add(SingleOptionDialogInput.OptionEntry.create(
        "pin_czerwona", Component.text("Czerwona pinezka"), true));
    if (options.stream().noneMatch(SingleOptionDialogInput.OptionEntry::initial)) {
      SingleOptionDialogInput.OptionEntry first = options.get(0);
      options.set(0, SingleOptionDialogInput.OptionEntry.create(first.id(), first.display(), true));
    }
    List<ActionButton> buttons = new ArrayList<>();
    buttons.add(button("Zapisz", id.isEmpty() ? "dodaj" : "zapisz/" + id));
    if (!id.isEmpty()) {
      buttons.add(button("Prowadź", "prowadz/" + id));
      buttons.add(button("Usuń", "usun/" + id));
    }
    buttons.add(button("Anuluj", null));
    Dialog dialog = Dialog.create(builder -> builder.empty()
        .base(DialogBase.builder(Component.text(title)).pause(false)
            .inputs(List.of(DialogInput.text("nazwa", Component.text("Nazwa"))
                    .initial(name).maxLength(24).build(),
                DialogInput.singleOption("ikona", Component.text("Ikona"), options).build()))
            .build())
        .type(DialogType.multiAction(buttons, null, 2)));
    player.showDialog(dialog);
  }

  @EventHandler
  public void onClick(PlayerCustomClickEvent event) {
    Key key = event.getIdentifier();
    if (!key.namespace().equals("mapa")) return;
    String action = key.value();
    if (!action.equals("dodaj") && !action.startsWith("zapisz/")
        && !action.startsWith("usun/") && !action.startsWith("prowadz/")) return;
    if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
    Player player = connection.getPlayer();
    Pending state = pending.remove(player.getUniqueId());
    if (state == null || !plugin.fullActive(player)) return;
    String id = action.contains("/") ? action.substring(action.indexOf('/') + 1) : "";
    if (!id.equals(state.id())) return;
    UUID owner = plugin.ownerKey(player);
    WlasneZnaczniki.Entry previous = id.isEmpty() ? null : plugin.own().find(owner, id);
    if (!id.isEmpty() && previous == null) return;
    if (action.equals("dodaj") || action.startsWith("zapisz/")) {
      DialogResponseView response = event.getDialogResponseView();
      if (response == null) return;
      String name = response.getText("nazwa"), icon = response.getText("ikona");
      ConfigurationSection icons = plugin.getConfig().getConfigurationSection("wlasne.ikony");
      boolean iconAllowed = icon != null && (icons == null ? icon.equals("pin_czerwona")
          : icons.contains(icon));
      if (name == null || name.isBlank() || name.length() > 24 || !iconAllowed) {
        player.sendActionBar(Component.text("Podaj nazwę i wybierz ikonę."));
        return;
      }
      WlasneZnaczniki.Entry entry = new WlasneZnaczniki.Entry(id.isEmpty() ? newId(owner)
          : id, state.world(), state.x(), state.z(), name.strip(), icon);
      boolean changed = id.isEmpty() ? plugin.own().add(owner, entry,
          Math.max(0, plugin.getConfig().getInt("wlasne.limit", 30)))
          : plugin.own().replace(owner, entry);
      if (!changed) { player.sendActionBar(Component.text("Osiągnięto limit własnych znaczników.")); return; }
      player.sendActionBar(Component.text("Zapisano znacznik: " + name));
    } else if (action.startsWith("usun/")) {
      plugin.own().remove(owner, id);
      player.sendActionBar(Component.text("Usunięto znacznik."));
    } else if (action.startsWith("prowadz/")) {
      plugin.navigation().toggle(player, new Prowadzenie.Cel(previous.world(), previous.x(),
          previous.z(), previous.name()));
    }
    plugin.markDirty(player);
  }

  private String newId(UUID owner) {
    String id;
    do { id = UUID.randomUUID().toString().substring(0, 8); }
    while (plugin.own().find(owner, id) != null);
    return id;
  }
}
