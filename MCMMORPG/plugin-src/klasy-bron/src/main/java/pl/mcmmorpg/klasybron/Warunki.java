package pl.mcmmorpg.klasybron;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.skills.conditions.IEntityComparisonCondition;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.comp.interaction.InteractionType;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Warunki MythicMobs porownujace castera z celem (IEntityComparisonCondition, MM 5.11.2:
 * SkillCondition.evaluateToEntity -> check(caster, cel)). Opis i uzycie: README.md, sekcja "Warunki".
 */
final class Warunki {

  /** Frakcje mobow MM nalezacych do gracza (chowance, slugi akolity, przywolania zaklec). */
  static final Set<String> FRAKCJE_GRACZA = Set.of("chowance", "akolita_slugi", "zaklecia_przywolane");

  /** Typy, ktore nigdy nie sa celem ofensywy: mieszkancy, stojaki, byty-wyswietlacze. */
  static final Set<EntityType> NIE_CEL = Set.of(EntityType.VILLAGER, EntityType.WANDERING_TRADER,
      EntityType.ARMOR_STAND, EntityType.ITEM_DISPLAY, EntityType.TEXT_DISPLAY, EntityType.BLOCK_DISPLAY,
      EntityType.INTERACTION);

  private final Plugin plugin;

  Warunki(Plugin plugin) {
    this.plugin = plugin;
  }

  /** {@code druzyna{}}: cel to caster (lub wlasciciel slugi) albo czlonek jego druzyny MMOCore. */
  IEntityComparisonCondition druzyna() {
    return (caster, cel) -> {
      Player gracz = graczCastera(caster);
      if (gracz == null || !(cel.getBukkitEntity() instanceof Player celGracz)) return false;
      if (gracz.getUniqueId().equals(celGracz.getUniqueId())) return true;
      if (!Bukkit.getPluginManager().isPluginEnabled("MMOCore")) return false;
      return wJednejDruzynie(gracz, celGracz);
    };
  }

  private static boolean wJednejDruzynie(Player a, Player b) {
    if (!PlayerData.has(a) || !PlayerData.has(b)) return false;
    AbstractParty druzyna = PlayerData.get(a).getParty();
    return druzyna != null && druzyna.hasMember(b);
  }

  /** {@code celWrogi{}}: wspolny filtr ofensywy skilli klas (README: "Warunki"). */
  IEntityComparisonCondition celWrogi() {
    return (caster, cel) -> {
      Entity e = cel.getBukkitEntity();
      if (!(e instanceof LivingEntity) || e.isDead() || NIE_CEL.contains(e.getType())) return false;
      if (e.hasMetadata("NPC") || e.getScoreboardTags().contains("npc")) return false;
      Player gracz = graczCastera(caster);
      if (gracz != null && gracz.getUniqueId().equals(e.getUniqueId())) return false;
      if (e instanceof Player celGracz) {
        // Caster-mob bez wlasciciela (zwykly mob MM) - gracz jest dla niego zawsze wrogiem.
        return gracz == null || mozeAtakowac(gracz, celGracz);
      }
      Optional<ActiveMob> mob = MythicBukkit.inst().getMobManager().getActiveMob(e.getUniqueId());
      if (mob.isEmpty()) return true; // vanilla mob (wiesniak/NIE_CEL odpadly wyzej)
      ActiveMob am = mob.get();
      if (am.getType() != null && am.getType().getIsInvincible()) return false;
      String frakcja = am.getFaction();
      if (frakcja == null || !FRAKCJE_GRACZA.contains(frakcja.toLowerCase(java.util.Locale.ROOT))) return true;
      // Mob gracza: cel tylko, gdy wolno atakowac jego wlasciciela (PvP, drużyna, interaction_rules MythicLib).
      Player wlasciciel = wlasciciel(am);
      if (wlasciciel == null) return true; // wlasciciel offline/brak - mob bez opiekuna
      if (gracz == null) return true;
      if (gracz.getUniqueId().equals(wlasciciel.getUniqueId())) return false;
      // D3=A: sluga sojusznika (wlasciciel w druzynie castera) nigdy nie jest celem. Bez tego MythicLib
      // pvp_on.offense.party_member=true (friendly fire druzyn na ringu) przepuszczal AoE na straznika kolegi.
      if (Bukkit.getPluginManager().isPluginEnabled("MMOCore") && wJednejDruzynie(gracz, wlasciciel)) return false;
      return mozeAtakowac(gracz, wlasciciel);
    };
  }

  /**
   * MythicLib EntityManager.canInteract dla celu-gracza wola Bukkit DamageCheckEvent (synchroniczny), wiec
   * poza watkiem glownym przechodzimy na niego (async skill MM). Timeout = cel odrzucony (bezpieczniej niz cios).
   */
  private boolean mozeAtakowac(Player gracz, Player cel) {
    if (Bukkit.isPrimaryThread()) return UtilityMethods.canTarget(gracz, cel, InteractionType.OFFENSE_SKILL);
    try {
      return Bukkit.getScheduler().callSyncMethod(plugin,
          () -> UtilityMethods.canTarget(gracz, cel, InteractionType.OFFENSE_SKILL)).get(1, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return false;
    } catch (Exception ex) {
      plugin.getLogger().log(Level.WARNING, "celWrogi: canTarget poza watkiem glownym nieudane, cel odrzucony", ex);
      return false;
    }
  }

  /**
   * Sojusznik gracza: on sam, czlonek jego druzyny MMOCore albo mob frakcji gracza nalezacy do niego lub do
   * czlonka druzyny. Celownik ({@code @Celownik}) patrzy przez sojusznikow (straznik przed graczem nie zaslania celu).
   */
  static boolean sojusznik(Player gracz, Entity e) {
    if (gracz.getUniqueId().equals(e.getUniqueId())) return true;
    boolean mmocore = Bukkit.getPluginManager().isPluginEnabled("MMOCore");
    if (e instanceof Player p) return mmocore && wJednejDruzynie(gracz, p);
    Optional<ActiveMob> mob = MythicBukkit.inst().getMobManager().getActiveMob(e.getUniqueId());
    if (mob.isEmpty()) return false;
    String frakcja = mob.get().getFaction();
    if (frakcja == null || !FRAKCJE_GRACZA.contains(frakcja.toLowerCase(java.util.Locale.ROOT))) return false;
    Player w = wlasciciel(mob.get());
    if (w == null) return false;
    return w.getUniqueId().equals(gracz.getUniqueId()) || (mmocore && wJednejDruzynie(gracz, w));
  }

  /** Caster-gracz albo wlasciciel castera-moba MM (slugi akolity rzucaja skille jako moby). */
  static Player graczCastera(AbstractEntity caster) {
    if (caster == null) return null;
    Entity e = caster.getBukkitEntity();
    if (e instanceof Player p) return p;
    if (e == null) return null;
    return MythicBukkit.inst().getMobManager().getActiveMob(e.getUniqueId()).map(Warunki::wlasciciel).orElse(null);
  }

  static Player wlasciciel(ActiveMob am) {
    AbstractEntity owner = am.getOwner().orElse(null);
    if (owner == null) return null;
    UUID id = owner.getUniqueId();
    return id == null ? null : Bukkit.getPlayer(id);
  }
}
