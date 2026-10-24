package pl.mcmmorpg.lochypoprawki;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.playavalon.mythicdungeons.MythicDungeons;
import net.playavalon.mythicdungeons.api.events.dungeon.RemoteTriggerEvent;
import net.playavalon.mythicdungeons.api.parents.instances.AbstractInstance;
import net.playavalon.mythicdungeons.api.parents.instances.InstancePlayable;
import net.playavalon.mythicdungeons.managers.PlayerManager;
import net.playavalon.mythicdungeons.player.MythicPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Poprawka MythicDungeons 2.0.1-SNAPSHOT: gracz BEZ party rozlaczony w instancji trzymal ja na zawsze.
 *
 * Bledna kolejnosc w MD przy PlayerQuitEvent (oba NORMAL, oba rejestrowane w onEnable MD):
 *  1. AvalonListener.onPlayerQuit usuwa MythicPlayer gracza solo z PlayerManager;
 *  2. dyspozytor instancji (MythicDungeons.registerInstanceListener) wola InstanceListener.onPlayerDisconnectDungeon,
 *     a ten getMythicPlayer(), ktore tworzy NOWY obiekt; nowego nie ma w livingPlayers, wiec MD robi
 *     removePlayer(nowy) (nic, nowy nie ma instancji) + dispose() (odmawia, bo stary obiekt dalej jest w instancji).
 *     Kick po KickOfflinePlayersDelay nigdy nie zostaje zaplanowany, a powrot gracza dostaje znow nowy obiekt.
 *
 * Miedzy 1 i 2 nie da sie wejsc, wiec na MONITOR oddajemy MD stary obiekt i jeszcze raz wolamy jego
 * onPlayerDisconnectDungeon dla tej instancji. Dalej dziala zwykla sciezka MD jak dla gracza w party:
 * kick po KickOfflinePlayersDelay, potem zamkniecie pustej instancji; powrot przed czasem wznawia loch.
 *
 * Komenda konsoli {@code lochy sygnal <gracz> <nazwa> [ticki]}: sygnal MD (jak mechanika MM dungeonsignal) do
 * instancji, w ktorej jest gracz. Odbiera go TriggerRemote z triggerName = nazwa w functions.yml mapy. Dla walk,
 * ktorych MD nie widzi (Chowance), np. koniec lochu po wygranej. Gracz poza instancja albo mapa bez odbiornika = nic.
 */
public final class LochyPoprawki extends JavaPlugin implements Listener {

  private Field graczeField;
  /** Prawdziwe obiekty graczy solo w instancji, zapamietane od LOWEST do MONITOR jednego PlayerQuitEvent. */
  private final Map<UUID, MythicPlayer> wychodzacy = new HashMap<>();

  @Override
  public void onEnable() {
    try {
      graczeField = PlayerManager.class.getDeclaredField("players");
      graczeField.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      getLogger().severe("Brak pola PlayerManager.players (inna wersja MythicDungeons?) - poprawka WYLACZONA: " + e);
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    getServer().getPluginManager().registerEvents(this, this);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length < 3 || !args[0].equalsIgnoreCase("sygnal")) {
      sender.sendMessage("Uzycie: /lochy sygnal <gracz> <nazwa> [opoznienie_ticki]");
      return true;
    }
    long ticki = 0;
    if (args.length > 3) {
      try {
        ticki = Math.max(0, Long.parseLong(args[3]));
      } catch (NumberFormatException e) {
        sender.sendMessage("Opoznienie musi byc liczba tickow: " + args[3]);
        return true;
      }
    }
    String nick = args[1];
    String nazwa = args[2];
    Bukkit.getScheduler().runTaskLater(this, () -> wyslijSygnal(nick, nazwa), ticki);
    return true;
  }

  private void wyslijSygnal(String nick, String nazwa) {
    Player p = Bukkit.getPlayerExact(nick);
    if (p == null) return;
    MythicPlayer mp = MythicDungeons.inst().getPlayerManager().get(p.getUniqueId());
    AbstractInstance inst = mp == null ? null : mp.getInstance();
    InstancePlayable play = inst == null ? null : inst.asPlayInstance();
    if (play == null || !MythicDungeons.inst().getActiveInstances().containsValue(inst)) return;
    Bukkit.getPluginManager().callEvent(new RemoteTriggerEvent(nazwa, null, play, 0, null, mp));
    getLogger().info("sygnal " + nazwa + " od " + nick + " w lochu " + inst.getInstanceWorld().getName());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void zapamietaj(PlayerQuitEvent event) {
    MythicPlayer mp = MythicDungeons.inst().getPlayerManager().get(event.getPlayer().getUniqueId());
    if (mp != null && mp.getInstance() != null && !mp.hasParty()) {
      wychodzacy.put(event.getPlayer().getUniqueId(), mp);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void napraw(PlayerQuitEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    MythicPlayer mp = wychodzacy.remove(uuid);
    if (mp == null) return;
    AbstractInstance inst = mp.getInstance();
    if (inst == null || !MythicDungeons.inst().getActiveInstances().containsValue(inst)) return;
    InstancePlayable play = inst.asPlayInstance();
    PlayerManager pm = MythicDungeons.inst().getPlayerManager();
    if (pm.get(uuid) == mp && (play == null || play.getOfflineTrackers().containsKey(uuid))) return;
    try {
      @SuppressWarnings("unchecked")
      Map<UUID, MythicPlayer> gracze = (Map<UUID, MythicPlayer>) graczeField.get(pm);
      gracze.put(uuid, mp);
    } catch (ReflectiveOperationException e) {
      getLogger().warning("Nie udalo sie przywrocic gracza " + event.getPlayer().getName() + " do MythicDungeons: " + e);
      return;
    }
    if (inst.getListener() != null) inst.getListener().onPlayerDisconnectDungeon(event);
    String swiat = inst.getInstanceWorld().getName();
    if (play == null || !inst.getConfig().getBoolean("General.KickOfflinePlayers", true)) return;
    if (play.getOfflineTrackers().containsKey(uuid)) {
      getLogger().info(event.getPlayer().getName() + " rozlaczony w lochu " + swiat + " - instancja zamknie sie po "
          + inst.getConfig().getInt("General.KickOfflinePlayersDelay", 300) + " s bez powrotu");
    } else if (MythicDungeons.inst().getActiveInstances().containsValue(inst)) {
      getLogger().warning(event.getPlayer().getName() + " rozlaczony w lochu " + swiat
          + ", ale MythicDungeons nie zaplanowal wyrzucenia - instancja moze wisiec");
    }
  }
}
