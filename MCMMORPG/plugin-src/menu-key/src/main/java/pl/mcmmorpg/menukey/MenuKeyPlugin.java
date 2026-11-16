package pl.mcmmorpg.menukey;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Klawisz Q otwiera menu-hub — ZAWSZE, takze przy pustej rece.
 *
 * <p><b>Po co ten plugin w ogole istnieje.</b> Cale menu jest konfiguracja (DeluxeMenus),
 * a klawisz przechwytywal CoreTools jednym wpisem w EventsManager. To rozwiazanie ma jedna
 * dziure, ktorej nie da sie zalatac konfiguracja: {@code PlayerDropItemEvent} jest zdarzeniem
 * o UPUSZCZONYM PRZEDMIOCIE, wiec gdy gracz nie trzyma niczego, nie ma co upuscic i zdarzenie
 * NIE LECI — menu sie nie otwiera. Sprawdzone botem: pakiet dochodzi do serwera, a mimo to
 * zadne zdarzenie Bukkita sie nie odpala. Bukkit po prostu nie ma zdarzenia "gracz wcisnal Q".
 *
 * <p>Dlatego siegamy poziom nizej, po sam pakiet. To JEDYNY powod istnienia tego jara: ma
 * ~jedna klase i nie rysuje zadnego menu ani nie zna zadnej jego pozycji. Co ma sie otworzyc,
 * mowi {@code config.yml}.
 *
 * <p><b>Co dokladnie przechwytujemy.</b> {@code BLOCK_DIG}
 * ({@code ServerboundPlayerActionPacket}) niesie takze kopanie blokow, wiec reagujemy
 * WYLACZNIE na {@code DROP_ITEM} i {@code DROP_ALL_ITEMS}. Pomylka tutaj wylaczylaby graczom
 * niszczenie blokow, dlatego warunek jest jawny, a nie "wszystko poza...".
 *
 * <p><b>Dlaczego anulujemy pakiet.</b> Bez tego Q robiloby obie rzeczy naraz: otwieraloby
 * menu i wyrzucaloby przedmiot. Anulowanie na poziomie pakietu ma przy okazji te zalete, ze
 * {@code PlayerDropItemEvent} w ogole nie powstaje — wiec stare wiazanie w CoreTools nie ma
 * juz czego lapac i zostalo usuniete. Klawisz ma JEDNEGO wlasciciela.
 */
public final class MenuKeyPlugin extends JavaPlugin {

  /** Nick w komendzie konsolowej — patrz {@link #dispatch}. */
  private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

  private String command;
  private boolean asConsole;
  private String permission;
  private Set<String> blockedWorlds;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    loadSettings();

    ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
    protocol.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL,
        PacketType.Play.Client.BLOCK_DIG) {
      @Override
      public void onPacketReceiving(PacketEvent event) {
        EnumWrappers.PlayerDigType type =
            event.getPacket().getPlayerDigTypes().readSafely(0);
        if (type != EnumWrappers.PlayerDigType.DROP_ITEM
            && type != EnumWrappers.PlayerDigType.DROP_ALL_ITEMS) {
          return;
        }
        Player player = event.getPlayer();
        if (!mayOpen(player)) {
          return;
        }
        event.setCancelled(true);
        // Jestesmy na watku sieciowym. Wszystko, co dotyka gry, musi wrocic na glowny.
        Bukkit.getScheduler().runTask(MenuKeyPlugin.this, () -> dispatch(player));
      }
    });

    getLogger().info("Klawisz Q -> " + (asConsole ? "konsola: " : "gracz: ") + command);
  }

  private void loadSettings() {
    reloadConfig();
    command = getConfig().getString("command", "dm open hub_glowne %player%");
    asConsole = !"player".equalsIgnoreCase(getConfig().getString("run-as", "console"));
    permission = getConfig().getString("permission", "");
    Set<String> worlds = new LinkedHashSet<>();
    for (String world : getConfig().getStringList("blocked-worlds")) {
      worlds.add(world.toLowerCase(Locale.ROOT));
    }
    blockedWorlds = Set.copyOf(worlds);
  }

  /**
   * Czy temu graczowi, tu i teraz, wolno otworzyc menu.
   *
   * <p>Warunek o otwartym oknie jest wazniejszy, niz wyglada: gdy gracz ma otwarte CUDZE
   * GUI, klawisz Q w tym oknie to zupelnie inny pakiet ({@code WINDOW_CLICK}), wiec tu
   * normalnie nie trafiamy — ale sprawdzamy to jawnie, zeby zadna przyszla wersja klienta
   * nie otworzyla nam menu nad cudzym oknem. Otwieranie okna "pod" juz otwartym to ta sama
   * rodzina bledow, ktora w kreatorze postaci konczyla sie rozjazdem stanu i crashem klienta.
   */
  private boolean mayOpen(Player player) {
    if (!permission.isBlank() && !player.hasPermission(permission)) {
      return false;
    }
    if (blockedWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
      return false;
    }
    return player.getOpenInventory().getType() == InventoryType.CRAFTING;
  }

  private void dispatch(Player player) {
    if (!player.isOnline()) {
      return;
    }
    if (!asConsole) {
      player.performCommand(strip(command));
      return;
    }
    String name = player.getName();
    // Serwer stoi na online-mode=false, wiec nick podaje klient, a komenda leci jako
    // konsola. Ten sam powod i ta sama reguła co w kreatorze postaci.
    if (!SAFE_NAME.matcher(name).matches()) {
      getLogger().severe("ODMOWA komendy konsolowej dla gracza '" + name
          + "': nazwa spoza [A-Za-z0-9_]{1,16}.");
      return;
    }
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
        strip(command).replace("%player%", name));
  }

  private static String strip(String raw) {
    String text = raw.trim();
    return text.startsWith("/") ? text.substring(1) : text;
  }
}
