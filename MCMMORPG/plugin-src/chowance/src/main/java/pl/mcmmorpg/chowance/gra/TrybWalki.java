package pl.mcmmorpg.chowance.gra;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import pl.mcmmorpg.chowance.Chowance;

/**
 * Walka chowańców zajmuje klawisze 1–9 (decyzja właściciela 15.11: „nowy zamyka stary”, walka wyłącza
 * tryb skilli MMOCore, pasek zaklęć i pełną mapę i nie pozwala ich otworzyć). Znak dla innych pluginów:
 * metadane gracza {@value #META} na czas walki turowej i pojedynku (Zaklecia, Mapa sprawdzają {@code hasMetadata}).
 * Tryb skilli blokuje {@link pl.mcmmorpg.chowance.integracja.BlokadaTrybuSkilli}.
 */
public final class TrybWalki {
  public static final String META = "chowance_walka";

  private TrybWalki() {}

  static void wejdz(Chowance c, Player p) {
    p.setMetadata(META, new FixedMetadataValue(c.plugin, true));
    c.mmocore.wyjdzZTrybuSkilli(p);
    if (Bukkit.getPluginManager().isPluginEnabled("Zaklecia"))
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "zaklecia pasek zamknij " + p.getName());
    if (Bukkit.getPluginManager().isPluginEnabled("Mapa"))
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mapa zamknij " + p.getName());
  }

  static void wyjdz(Chowance c, Player p) { p.removeMetadata(META, c.plugin); }

  public static boolean trwa(Player p) { return p.hasMetadata(META); }
}
