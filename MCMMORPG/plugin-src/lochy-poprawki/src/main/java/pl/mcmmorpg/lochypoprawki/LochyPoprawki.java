package pl.mcmmorpg.lochypoprawki;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.playavalon.mythicdungeons.MythicDungeons;
import net.playavalon.mythicdungeons.api.events.dungeon.RemoteTriggerEvent;
import net.playavalon.mythicdungeons.api.parents.instances.AbstractInstance;
import net.playavalon.mythicdungeons.api.parents.instances.InstancePlayable;
import net.playavalon.mythicdungeons.managers.PlayerManager;
import net.playavalon.mythicdungeons.player.MythicPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldInitEvent;
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
 *
 * Zamiatacz (co 30 s, 24.10) - instancje, ktorych MD sam nie zamknie:
 *  - rozlaczenie w trakcie kopiowania swiata: DungeonClassic po latch wola inst.addPlayer() bez sprawdzenia isOnline,
 *    PlayerQuitEvent juz minal, wiec nikt nie planuje kicka - gracz offline bez trackera wisi w instancji na zawsze;
 *  - gracz online, ale w innym swiecie (np. przeniesiony bez PlayerChangedWorldEvent ze swiata instancji);
 *  - "Dungeon took too long to load" (latch 5 s w DungeonClassic): MD wyjmuje instancje z activeInstances, ale swiat
 *    i tak sie dociaga i tyka bez konca (klon 24.10: wczytanie 8,2 s przy obciazonym hoscie). Co 1 s pamietamy aktywne
 *    instancje (slabe odwolania); znikniecie bez dispose = dispose() tej instancji (sciezka MD: funkcje, swiat, pliki).
 * Offline bez trackera = wyrzucenie po KickOfflinePlayersDelay jak w MD; online poza swiatem przez 2 przejscia z rzedu
 * = tylko zwolnienie wpisu MD bez teleportu (gracz juz jest gdzie indziej, np. w scenie kreatora); pusta = dispose().
 * Komenda {@code lochy stan}: aktywne instancje i ich gracze (diagnoza, test).
 */
public final class LochyPoprawki extends JavaPlugin implements Listener {

  private Field graczeField;
  /** Prawdziwe obiekty graczy solo w instancji, zapamietane od LOWEST do MONITOR jednego PlayerQuitEvent. */
  private final Map<UUID, MythicPlayer> wychodzacy = new HashMap<>();
  /** Zamiatacz: "instancja/gracz" -> od kiedy offline bez trackera MD (ms). */
  private final Map<String, Long> offlineOd = new HashMap<>();
  /** Zamiatacz: "instancja/gracz" -> ile przejsc z rzedu online poza swiatem instancji. */
  private final Map<String, Integer> pozaSwiatem = new HashMap<>();
  private static final int ZAMIATANIE_CO_S = 30;
  /** Instancje widziane w activeInstances; slabe klucze, wiec zamknieta i zapomniana instancja nie wisi w pamieci. */
  private final Set<AbstractInstance> znane = Collections.newSetFromMap(new WeakHashMap<>());
  private Field disposingField;
  private int sekundy;

  @Override
  public void onEnable() {
    try {
      graczeField = PlayerManager.class.getDeclaredField("players");
      graczeField.setAccessible(true);
      disposingField = AbstractInstance.class.getDeclaredField("disposing");
      disposingField.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      getLogger().severe("Brak pola PlayerManager.players (inna wersja MythicDungeons?) - poprawka WYLACZONA: " + e);
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    getServer().getPluginManager().registerEvents(this, this);
    Bukkit.getScheduler().runTaskTimer(this, this::coSekunde, 20L, 20L);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1 && args[0].equalsIgnoreCase("stan")) {
      sender.sendMessage(stan());
      return true;
    }
    if (args.length < 3 || !args[0].equalsIgnoreCase("sygnal")) {
      sender.sendMessage("Uzycie: /lochy sygnal <gracz> <nazwa> [opoznienie_ticki] | /lochy stan");
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

  /**
   * Swiat instancji tworzy sie na watku serwera i blokuje go (11 s na klonie 24.10); timeout MD (5 s) wyrzuca
   * instancje z activeInstances w tym czasie, wiec coSekunde jej nie zobaczy. WorldInitEvent idzie na poczatku
   * tworzenia swiata, gdy instancja jest jeszcze aktywna.
   */
  @EventHandler
  public void nowySwiat(WorldInitEvent event) {
    znane.addAll(MythicDungeons.inst().getActiveInstances().values());
  }

  /** Jedna linia na instancje: swiat, gracze (online:swiat / offline +tracker), np. dla testow. */
  private String stan() {
    StringBuilder sb = new StringBuilder("instancji " + MythicDungeons.inst().getActiveInstances().size());
    for (AbstractInstance inst : MythicDungeons.inst().getActiveInstances().values()) {
      World w = inst.getInstanceWorld();
      InstancePlayable play = inst.asPlayInstance();
      sb.append(" | ").append(w == null ? "?" : w.getName()).append(":");
      for (MythicPlayer mp : inst.getPlayers()) {
        UUID uuid = mp.getPlayer().getUniqueId();
        Player p = Bukkit.getPlayer(uuid);
        sb.append(' ').append(mp.getPlayer().getName()).append('=');
        if (p != null) sb.append("online@").append(p.getWorld().getName());
        else sb.append(play != null && play.getOfflineTrackers().containsKey(uuid) ? "offline+tracker" : "offline");
      }
    }
    return sb.toString();
  }

  private void coSekunde() {
    Collection<AbstractInstance> aktywne = MythicDungeons.inst().getActiveInstances().values();
    for (AbstractInstance inst : new ArrayList<>(znane)) {
      if (aktywne.contains(inst)) continue;
      boolean zamykana;
      try {
        zamykana = disposingField.getBoolean(inst);
      } catch (IllegalAccessException e) {
        zamykana = true;
      }
      if (zamykana) {
        znane.remove(inst);
      } else if (inst.getInstanceWorld() != null && inst.getPlayers().isEmpty()) {
        getLogger().info("instancja " + inst.getInstanceWorld().getName() + " wypadla z MD bez zamkniecia (timeout wczytania?) - zamykam");
        znane.remove(inst);
        inst.dispose();
      }
    }
    znane.addAll(aktywne);
    if (++sekundy % ZAMIATANIE_CO_S == 0) zamiataj();
  }

  private void zamiataj() {
    Set<String> widziane = new HashSet<>();
    for (AbstractInstance inst : new ArrayList<>(MythicDungeons.inst().getActiveInstances().values())) {
      InstancePlayable play = inst.asPlayInstance();
      World w = inst.getInstanceWorld();
      if (play == null || w == null || inst.isEditInstance()) continue;
      for (MythicPlayer mp : new ArrayList<>(inst.getPlayers())) {
        UUID uuid = mp.getPlayer().getUniqueId();
        String klucz = inst.getUuid() + "/" + uuid;
        widziane.add(klucz);
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) {
          pozaSwiatem.remove(klucz);
          if (play.getOfflineTrackers().containsKey(uuid) || !inst.getConfig().getBoolean("General.KickOfflinePlayers", true)) {
            offlineOd.remove(klucz);
            continue;
          }
          long od = offlineOd.computeIfAbsent(klucz, k -> System.currentTimeMillis());
          long limit = inst.getConfig().getInt("General.KickOfflinePlayersDelay", 300) * 1000L;
          if (System.currentTimeMillis() - od < limit) continue;
          getLogger().info(mp.getPlayer().getName() + " offline w " + w.getName() + " bez wyrzucenia MD - wyrzucam");
          inst.removePlayer(mp);
          offlineOd.remove(klucz);
        } else {
          offlineOd.remove(klucz);
          if (p.getWorld() == w || mp.isAwaitingDungeon()) {
            pozaSwiatem.remove(klucz);
            continue;
          }
          if (pozaSwiatem.merge(klucz, 1, Integer::sum) < 2) continue;
          getLogger().info(p.getName() + " w " + p.getWorld().getName() + ", a MD trzyma go w " + w.getName() + " - zwalniam wpis");
          inst.removePlayer(mp, true, false);
          play.getLivingPlayers().remove(mp);
          if (mp.getInstance() == inst) {
            mp.setInstance(null);
            mp.setSavedPosition(null);
          }
          pozaSwiatem.remove(klucz);
        }
      }
      if (inst.getPlayers().isEmpty()) {
        getLogger().info("pusta instancja " + w.getName() + " - zamykam");
        inst.dispose();
      }
    }
    offlineOd.keySet().retainAll(widziane);
    pozaSwiatem.keySet().retainAll(widziane);
  }
}
