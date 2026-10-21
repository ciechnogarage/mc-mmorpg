package pl.mcmmorpg.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/** Portal z configu + jego zywy stan z pingowania backendu. */
final class Portal {
  final PortalSlot slot;
  final String server;
  final String host;
  final String nameRaw;
  final Component name;
  final String description;
  final Material icon;

  // zapisywane z watku pingu, czytane z glownego
  volatile boolean online;
  volatile int players;
  volatile int okStreak;

  Portal(PortalSlot slot, ConfigurationSection sec) {
    this.slot = slot;
    this.server = sec == null ? "" : sec.getString("serwer", "");
    String h = sec == null ? "" : sec.getString("host", "");
    this.host = h.isEmpty() ? server : h;
    this.nameRaw = sec == null ? "<gray>???" : sec.getString("nazwa", "<gray>???");
    this.name = MiniMessage.miniMessage().deserialize(nameRaw);
    this.description = sec == null ? "Wkrótce" : sec.getString("opis", "");
    Material m = sec == null ? null : Material.matchMaterial(sec.getString("ikona", "BARRIER"));
    this.icon = m == null ? Material.BARRIER : m;
  }

  boolean active() {
    return !server.isEmpty();
  }

  boolean ready(int neededPings) {
    return active() && online && okStreak >= neededPings;
  }

  /** Linia statusu w MiniMessage - ta sama nad portalem i w menu. */
  String statusLine(int neededPings) {
    if (!active()) return "<gray>Wkrótce";
    if (ready(neededPings)) {
      return "<green>● Online</green> <dark_gray>·</dark_gray> <white>" + players + "</white> <gray>" + gracze(players);
    }
    if (online) return "<yellow>● Uruchamia się…";
    return "<red>● Offline</red> <dark_gray>·</dark_gray> <gray>restart / przerwa";
  }

  static String gracze(int n) {
    if (n == 1) return "gracz";
    int d = n % 10;
    int dd = n % 100;
    return d >= 2 && d <= 4 && (dd < 12 || dd > 14) ? "gracze" : "graczy";
  }
}
