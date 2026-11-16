package pl.mcmmorpg.arena;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.guild.provided.Guild;
import net.Indyuce.mmocore.party.provided.MMOCorePartyModule;
import net.Indyuce.mmocore.party.provided.Party;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.lang.reflect.Method;
import java.time.Duration;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import org.bukkit.event.inventory.InventoryType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Menu Q: wszystkie identyfikatory kliknięć są przypisane do konkretnego widza i celu. */
final class MenuGracza implements CommandExecutor, Listener {
  private enum Kind { DUEL, PARTY, FRIEND, GUILD, CLOSE, ACCEPT, DECLINE }
  private record Click(UUID viewer, UUID target, String viewerProfile, String targetProfile, Kind kind, UUID invitation) {}
  private record Invitation(UUID id, UUID challenger, UUID target, String challengerProfile, String targetProfile, long expires) {}
  private record Cooldown(UUID from, UUID to, Kind kind) {}
  private final class Countdown extends BukkitRunnable {
    final UUID first, second;
    final boolean firstEntered, secondEntered;   // kogo dopiero pojedynek wprowadzil na arene - przy przerwaniu wraca
    int remaining = 3;
    Countdown(Player first, Player second, boolean firstEntered, boolean secondEntered) {
      this.first = first.getUniqueId(); this.second = second.getUniqueId();
      this.firstEntered = firstEntered; this.secondEntered = secondEntered;
    }
    @Override public void run() {
      Player a = Bukkit.getPlayer(first), b = Bukkit.getPlayer(second);
      if (a == null || b == null || !arena.onArena(a) || !arena.onArena(b)
          || !arena.profile(a).equals(startProfiles.get(first)) || !arena.profile(b).equals(startProfiles.get(second))) {
        cancelCountdown(this, "Pojedynek przerwany: gracz opuścił arenę lub zmienił postać.");
        return;
      }
      if (remaining == 0) {
        arena.protectForDuel(a, System.currentTimeMillis());
        arena.protectForDuel(b, System.currentTimeMillis());
        a.showTitle(Title.title(Component.text("WALCZ!", NamedTextColor.GREEN), Component.empty()));
        b.showTitle(Title.title(Component.text("WALCZ!", NamedTextColor.GREEN), Component.empty()));
        active.remove(first); active.remove(second);
        startProfiles.remove(first); startProfiles.remove(second);
        cancel();
        return;
      }
      Title title = Title.title(Component.text(Integer.toString(remaining), NamedTextColor.YELLOW),
          Component.text("Pojedynek zacznie się za chwilę", NamedTextColor.GRAY),
          Title.Times.times(Duration.ZERO, Duration.ofMillis(1050), Duration.ZERO));
      a.showTitle(title); b.showTitle(title);
      remaining--;
    }
  }

  private final ArenaPlugin arena;
  private final Map<Key, Click> clicks = new HashMap<>();
  private final Map<UUID, Invitation> invitations = new HashMap<>();
  private final Map<Cooldown, Long> cooldowns = new HashMap<>();
  private final Map<UUID, Countdown> active = new HashMap<>();
  private final Map<UUID, String> startProfiles = new HashMap<>();

  MenuGracza(ArenaPlugin arena) { this.arena = arena; }

  @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof ConsoleCommandSender) && !(sender instanceof RemoteConsoleCommandSender)) {
      sender.sendMessage("§cTylko konsola może otworzyć menu gracza."); return true;
    }
    if (args.length != 2 || !args[0].equalsIgnoreCase("q")) { sender.sendMessage("menugracza q <gracz>"); return true; }
    Player player = Bukkit.getPlayerExact(args[1]);
    if (player == null) { sender.sendMessage("Gracz jest offline: " + args[1]); return true; }
    if (player.getWorld().getName().equalsIgnoreCase("creator_limbo") || player.getGameMode() == GameMode.SPECTATOR) return true;
    Player target = target(player);
    if (target != null) openMenu(player, target);
    else fallback(player);
    return true;
  }

  private Player target(Player player) {
    RayTraceResult hit = player.getWorld().rayTrace(player.getEyeLocation(), player.getEyeLocation().getDirection(),
        arena.getConfig().getDouble("menu-gracza.zasieg", 4.5), FluidCollisionMode.NEVER, true, 0.1,
        entity -> entity instanceof Player other && visible(player, other));
    return hit != null && hit.getHitEntity() instanceof Player other ? other : null;
  }

  private final Map<UUID, UUID> lastHint = new HashMap<>();
  private final Map<UUID, Long> lastHintAt = new HashMap<>();
  /** Co 5 tickow: gracz celujacy w innego gracza widzi na pasku, ze Q otworzy menu tego gracza. */
  void hints() {
    if (!arena.getConfig().getBoolean("menu-gracza.podpowiedz", true)) return;
    long now = System.currentTimeMillis();
    for (Player player : Bukkit.getOnlinePlayers()) {
      UUID id = player.getUniqueId();
      Player target = quiet(player) ? null : target(player);
      if (target == null) { lastHint.remove(id); continue; }
      if (target.getUniqueId().equals(lastHint.get(id)) && now - lastHintAt.getOrDefault(id, 0L) < 1000) continue;
      lastHint.put(id, target.getUniqueId()); lastHintAt.put(id, now);
      if (MMOPlayerData.has(player)) MMOPlayerData.get(player).getActionBar().show(10, 25L, "§7[§eQ§7] Menu gracza: §f" + target.getName());
    }
  }
  // bez podpowiedzi w walce, w ringu, w kreatorze, w trybie widza i przy otwartym oknie
  private boolean quiet(Player player) {
    return player.getGameMode() == GameMode.SPECTATOR || player.getWorld().getName().equalsIgnoreCase("creator_limbo")
        || player.getOpenInventory().getType() != InventoryType.CRAFTING || arena.busy(player)
        || PlayerData.has(player) && PlayerData.get(player).isInCombat();
  }

  private boolean visible(Player viewer, Player target) {
    return viewer != target && target.isOnline() && target.getGameMode() != GameMode.SPECTATOR
        && !target.hasMetadata("vanished") && !target.hasMetadata("NPC") && viewer.canSee(target)
        && !target.getWorld().getName().equalsIgnoreCase("creator_limbo");
  }
  private void fallback(Player player) {
    String command = arena.getConfig().getString("menu-gracza.zwykle", "dm open hub_glowne %gracz%");
    if (command != null && !command.isBlank()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%gracz%", player.getName()).replaceFirst("^/", ""));
  }
  private ActionButton button(Player viewer, Player target, Kind kind, String label, UUID invitation) {
    Key key = Key.key("arena", "menu_gracza/" + UUID.randomUUID() + "/" + kind.name().toLowerCase());
    clicks.put(key, new Click(viewer.getUniqueId(), target.getUniqueId(), arena.profile(viewer), arena.profile(target), kind, invitation));
    return ActionButton.builder(Component.text(label)).action(DialogAction.customClick(key, null)).build();
  }
  private void clearClicks(UUID player) { clicks.entrySet().removeIf(entry -> entry.getValue().viewer().equals(player)); }
  private void show(Player player, String title, String body, List<ActionButton> buttons) {
    player.showDialog(Dialog.create(factory -> factory.empty()
        .base(DialogBase.builder(Component.text(title))
            .body(List.of(DialogBody.plainMessage(Component.text(body))))
            .afterAction(DialogBase.DialogAfterAction.CLOSE).build())
        .type(DialogType.multiAction(buttons).columns(1).build())));
  }
  private void openMenu(Player viewer, Player target) {
    clearClicks(viewer.getUniqueId());
    StringBuilder body = new StringBuilder();
    if (PlayerData.has(target)) {
      PlayerData data = PlayerData.get(target);
      body.append("Klasa: ").append(data.getProfess() == null ? "brak" : data.getProfess().getName())
          .append(" · poziom ").append(data.getLevel());
      if (data.getGuild() != null) body.append("\nGildia: ").append(data.getGuild().getName());
    }
    String patron = patron(target);
    if (patron != null) body.append("\nPatron: ").append(patron);
    if (body.isEmpty()) body.append("Gracz: ").append(target.getName());
    List<ActionButton> buttons = new ArrayList<>();
    buttons.add(button(viewer, target, Kind.DUEL, "Wyzwij na pojedynek", null));
    if (PlayerData.has(viewer) && PlayerData.has(target)) {
      PlayerData from = PlayerData.get(viewer), to = PlayerData.get(target);
      if (MMOCore.plugin.partyModule instanceof MMOCorePartyModule
          && (from.getParty() == null || !from.getParty().hasMember(target)))
        buttons.add(button(viewer, target, Kind.PARTY, "Zaproś do drużyny", null));
      if (!from.hasFriend(target.getUniqueId()))
        buttons.add(button(viewer, target, Kind.FRIEND, "Dodaj do znajomych", null));
      Guild guild = from.getGuild();
      if (guild != null && guild.getOwner().equals(viewer.getUniqueId()) && to.getGuild() == null)
        buttons.add(button(viewer, target, Kind.GUILD, "Zaproś do gildii", null));
    }
    buttons.add(button(viewer, target, Kind.CLOSE, "Zamknij", null));
    show(viewer, "Gracz: " + target.getName(), body.toString(), buttons);
  }
  private String patron(Player player) {
    if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return null;
    try {
      Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
      Method method = papi.getMethod("setPlaceholders", org.bukkit.entity.Player.class, String.class);
      String value = (String) method.invoke(null, player, "%wiary_patron%");
      return value == null || value.isBlank() || value.contains("%wiary_patron%") ? null : value;
    } catch (ReflectiveOperationException | LinkageError error) {
      arena.getLogger().warning("Nie udało się odczytać patrona PAPI: " + error.getMessage());
      return null;
    }
  }

  @EventHandler public void onClick(PlayerCustomClickEvent event) {
    if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
    Player viewer = connection.getPlayer();
    Key id = event.getIdentifier();
    if (!Bukkit.isPrimaryThread()) { Bukkit.getScheduler().runTask(arena, () -> handle(viewer, id)); return; }
    handle(viewer, id);
  }
  private void handle(Player viewer, Key id) {
    Click click = clicks.get(id);
    if (click == null || !viewer.getUniqueId().equals(click.viewer())) return;
    clearClicks(viewer.getUniqueId());
    if (click.kind() == Kind.CLOSE) return;
    Player target = Bukkit.getPlayer(click.target());
    if (target == null || !arena.profile(viewer).equals(click.viewerProfile())
        || !arena.profile(target).equals(click.targetProfile())) { arena.notice(viewer, "Gracz wyszedł lub zmienił postać."); return; }
    if (click.kind() == Kind.ACCEPT || click.kind() == Kind.DECLINE) {
      respond(viewer, click.invitation(), click.kind() == Kind.ACCEPT);
      return;
    }
    if (!visible(viewer, target)) { arena.notice(viewer, "Ten gracz jest niedostępny."); return; }
    switch (click.kind()) {
      case DUEL -> challenge(viewer, target);
      case PARTY -> inviteParty(viewer, target);
      case FRIEND -> inviteFriend(viewer, target);
      case GUILD -> inviteGuild(viewer, target);
      default -> { }
    }
  }
  private boolean cooldown(Player from, Player to, Kind kind) {
    Cooldown key = new Cooldown(from.getUniqueId(), to.getUniqueId(), kind);
    long now = System.currentTimeMillis();
    cooldowns.values().removeIf(until -> until <= now);
    long expires = cooldowns.getOrDefault(key, 0L);
    if (expires > now) { arena.notice(from, "Poczekaj " + ((expires - now + 999) / 1000) + " s przed kolejnym zaproszeniem."); return false; }
    cooldowns.put(key, now + Math.max(1, arena.getConfig().getLong("menu-gracza.cooldown", 15)) * 1000);
    return true;
  }
  private void challenge(Player from, Player to) {
    if (active.containsKey(from.getUniqueId()) || active.containsKey(to.getUniqueId())) { arena.notice(from, "Pojedynek już się rozpoczyna."); return; }
    String reason = arena.duelProblem(from);
    if (reason == null) reason = arena.duelProblem(to);
    if (reason != null) { arena.notice(from, reason); return; }
    if (!cooldown(from, to, Kind.DUEL)) return;
    UUID id = UUID.randomUUID();
    long expires = System.currentTimeMillis() + Math.max(1, arena.getConfig().getLong("menu-gracza.waznosc-pojedynku", 30)) * 1000;
    Invitation invitation = new Invitation(id, from.getUniqueId(), to.getUniqueId(), arena.profile(from), arena.profile(to), expires);
    invitations.put(id, invitation);
    showPendingChallenges(to);
    arena.notice(from, "Wyzwanie wysłane do " + to.getName() + ".");
    Bukkit.getScheduler().runTaskLater(arena, () -> {
      if (invitations.remove(id, invitation)) { Player target = Bukkit.getPlayer(to.getUniqueId()); if (target != null) arena.notice(target, "Wyzwanie na pojedynek wygasło."); }
    }, Math.max(1, arena.getConfig().getLong("menu-gracza.waznosc-pojedynku", 30)) * 20);
  }
  private void showPendingChallenges(Player target) {
    clearClicks(target.getUniqueId());
    List<ActionButton> buttons = new ArrayList<>();
    List<String> challengers = new ArrayList<>();
    for (Invitation invitation : invitations.values()) {
      if (!invitation.target().equals(target.getUniqueId()) || invitation.expires() <= System.currentTimeMillis()) continue;
      Player from = Bukkit.getPlayer(invitation.challenger());
      if (from == null) continue;
      challengers.add(from.getName());
      buttons.add(button(target, from, Kind.ACCEPT, "Przyjmij: " + from.getName(), invitation.id()));
      buttons.add(button(target, from, Kind.DECLINE, "Odrzuć: " + from.getName(), invitation.id()));
    }
    buttons.add(button(target, target, Kind.CLOSE, "Zamknij", null));
    String body = challengers.size() == 1
        ? challengers.get(0) + " wyzywa Cię na pojedynek na Otwartej Arenie."
        : "Wyzwania na pojedynek na Otwartej Arenie: " + String.join(", ", challengers);
    show(target, "Wyzwanie na pojedynek", body, buttons);
  }
  private void respond(Player target, UUID id, boolean accept) {
    Invitation invitation = invitations.remove(id);
    if (invitation == null || invitation.expires() <= System.currentTimeMillis()
        || !invitation.target().equals(target.getUniqueId())) { arena.notice(target, "Wyzwanie wygasło."); return; }
    Player from = Bukkit.getPlayer(invitation.challenger());
    if (from == null || !arena.profile(from).equals(invitation.challengerProfile())
        || !arena.profile(target).equals(invitation.targetProfile())) { arena.notice(target, "Wyzwanie wygasło: gracz wyszedł lub zmienił postać."); return; }
    if (!accept) { arena.notice(from, target.getName() + " odrzucił wyzwanie."); arena.notice(target, "Odrzucono wyzwanie."); return; }
    String a = arena.duelProblem(from), b = arena.duelProblem(target);
    if (a != null || b != null || active.containsKey(from.getUniqueId()) || active.containsKey(target.getUniqueId())) {
      String reason = a != null ? from.getName() + ": " + a : b != null ? target.getName() + ": " + b : "Jeden z graczy rozpoczyna już pojedynek.";
      arena.notice(from, reason); arena.notice(target, reason); return;
    }
    invitations.values().removeIf(other -> other.challenger().equals(from.getUniqueId()) || other.target().equals(from.getUniqueId())
        || other.challenger().equals(target.getUniqueId()) || other.target().equals(target.getUniqueId()));
    boolean firstEntered = !arena.onArena(from), secondEntered = !arena.onArena(target);
    if (!arena.enterForDuel(from) || !arena.enterForDuel(target)) {
      if (firstEntered) arena.leaveFailedDuel(from);
      if (secondEntered) arena.leaveFailedDuel(target);
      arena.notice(from, "Nie udało się wejść na arenę."); arena.notice(target, "Nie udało się wejść na arenę."); return;
    }
    long safeEnd = System.currentTimeMillis() + 4200;
    arena.protectForDuel(from, safeEnd); arena.protectForDuel(target, safeEnd);
    if (!arena.placeForDuel(from, "pierwszy") || !arena.placeForDuel(target, "drugi")) {
      if (firstEntered) arena.leaveFailedDuel(from);
      if (secondEntered) arena.leaveFailedDuel(target);
      arena.notice(from, "Nie udało się przenieść do ringu."); arena.notice(target, "Nie udało się przenieść do ringu."); return;
    }
    Countdown countdown = new Countdown(from, target, firstEntered, secondEntered);
    active.put(from.getUniqueId(), countdown); active.put(target.getUniqueId(), countdown);
    startProfiles.put(from.getUniqueId(), arena.profile(from)); startProfiles.put(target.getUniqueId(), arena.profile(target));
    countdown.runTaskTimer(arena, 0, 20);
  }
  private void inviteParty(Player from, Player to) {
    if (!PlayerData.has(from) || !PlayerData.has(to) || !(MMOCore.plugin.partyModule instanceof MMOCorePartyModule module)) { arena.notice(from, "Drużyny MMOCore są niedostępne."); return; }
    PlayerData owner = PlayerData.get(from), target = PlayerData.get(to);
    if (target.getParty() != null && target.getParty() == owner.getParty()) { arena.notice(from, "Gracz jest już w Twojej drużynie."); return; }
    if (!cooldown(from, to, Kind.PARTY)) return;
    try {
      Party party = owner.getParty() instanceof Party existing ? existing : module.newRegisteredParty(owner);
      party.sendPartyInvite(owner, target);
      arena.notice(from, "Zaproszenie do drużyny wysłane.");
    } catch (RuntimeException error) { arena.getLogger().warning("Zaproszenie do drużyny: " + error.getMessage()); arena.notice(from, "Nie udało się wysłać zaproszenia do drużyny."); }
  }
  private void inviteFriend(Player from, Player to) {
    if (!PlayerData.has(from) || !PlayerData.has(to)) { arena.notice(from, "Znajomi MMOCore są niedostępni."); return; }
    PlayerData owner = PlayerData.get(from);
    if (owner.hasFriend(to.getUniqueId())) { arena.notice(from, "Jesteście już znajomymi."); return; }
    if (!cooldown(from, to, Kind.FRIEND)) return;
    try { owner.sendFriendRequest(PlayerData.get(to)); arena.notice(from, "Prośba o znajomość wysłana."); }
    catch (RuntimeException error) { arena.getLogger().warning("Prośba o znajomość: " + error.getMessage()); arena.notice(from, "Nie udało się wysłać prośby o znajomość."); }
  }
  private void inviteGuild(Player from, Player to) {
    if (!PlayerData.has(from) || !PlayerData.has(to)) { arena.notice(from, "Gildie MMOCore są niedostępne."); return; }
    Guild guild = PlayerData.get(from).getGuild();
    if (guild == null || !guild.getOwner().equals(from.getUniqueId()) || PlayerData.get(to).getGuild() != null) {
      arena.notice(from, "Nie masz prawa zaprosić tego gracza do gildii."); return;
    }
    if (!cooldown(from, to, Kind.GUILD)) return;
    try { guild.sendGuildInvite(PlayerData.get(from), PlayerData.get(to)); arena.notice(from, "Zaproszenie do gildii wysłane."); }
    catch (RuntimeException error) { arena.getLogger().warning("Zaproszenie do gildii: " + error.getMessage()); arena.notice(from, "Nie udało się wysłać zaproszenia do gildii."); }
  }
  private void cancelCountdown(Countdown countdown, String reason) {
    countdown.cancel();
    active.remove(countdown.first); active.remove(countdown.second);
    startProfiles.remove(countdown.first); startProfiles.remove(countdown.second);
    for (UUID id : List.of(countdown.first, countdown.second)) {
      Player player = Bukkit.getPlayer(id);
      if (player == null) continue;
      player.clearTitle(); arena.notice(player, reason);
      boolean entered = id.equals(countdown.first) ? countdown.firstEntered : countdown.secondEntered;
      if (entered && player.isOnline()) arena.leaveFailedDuel(player);
    }
  }
  void cancelFor(Player player) {
    UUID id = player.getUniqueId();
    clicks.entrySet().removeIf(entry -> entry.getValue().viewer().equals(id) || entry.getValue().target().equals(id));
    invitations.values().removeIf(invitation -> invitation.challenger().equals(id) || invitation.target().equals(id));
    lastHint.remove(id); lastHintAt.remove(id);   // cooldowns zostaja: relog nie omija limitu zaproszen
    Countdown countdown = active.get(id);
    if (countdown != null) cancelCountdown(countdown, "Pojedynek przerwany: gracz wyszedł lub zmienił postać.");
  }
  @EventHandler public void onQuit(PlayerQuitEvent event) { cancelFor(event.getPlayer()); }
  @EventHandler public void onDeath(PlayerDeathEvent event) { cancelFor(event.getEntity()); }
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void onTeleport(PlayerTeleportEvent event) {
    Countdown countdown = active.get(event.getPlayer().getUniqueId());
    if (countdown != null && event.getTo() != null && !event.getTo().getWorld().getName().equals(arena.getConfig().getString("world", "arena")))
      cancelCountdown(countdown, "Pojedynek przerwany: gracz opuścił arenę.");
  }
}
