package pl.mcmmorpg.chowance.integracja;

import net.playavalon.mythicdungeons.api.MythicDungeonsService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Czy gracz jest w instancji MythicDungeons (pieczęć lochowa). Serwis z ServicesManagera. */
public final class Lochy {
  private Lochy() {}

  public static boolean wLochu(Player p) {
    if (!Bukkit.getPluginManager().isPluginEnabled("MythicDungeons")) return false;
    try {
      MythicDungeonsService s = Bukkit.getServicesManager().load(MythicDungeonsService.class);
      return s != null && s.isPlayerInDungeon(p);
    } catch (RuntimeException | LinkageError e) {
      return false;
    }
  }
}
