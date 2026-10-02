package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.ThreadSafetyLevel;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import java.util.Locale;
import java.util.logging.Level;
import io.lumine.mythic.lib.player.resource.ResourceUpdateReason;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Ciche zasoby MMOCore dla zaklec koncentracji (MythicMobs/Crucible).
 *
 * Po co: `mmocore admin resource-mana take` zawsze odpisuje do nadawcy ("X now has Y mana points"),
 * a przy konsoli to linia w logu serwera co tik rzucania (2/s na gracza). Zaden zainstalowany plugin
 * (MMOCore, MythicLib, MMOItems) nie daje MythicMobs mechaniki many — stad ten plugin.
 *
 * - mechanika {@code zakZasob{zasob=mana|stamina;a=2}} — zabiera zasob celowi (gracz), cicho;
 * - warunek {@code zakMaZasob{zasob=mana;a=2}} — czy cel (gracz) ma co najmniej {@code a}.
 *
 * Rejestracja przez Mythic*LoadEvent (wzor: github.com/xikage/MythicMobs-API-Examples).
 */
public final class Zaklecia extends JavaPlugin implements Listener {

  private Ksiega ksiega;
  private Nosnik nosnik;
  private Ulubione ulubione;
  private Rzucanie rzucanie;
  private Wybor wybor;
  private Kotwica kotwica;

  @Override
  public void onDisable() {
    if (kotwica != null) kotwica.wylacz();
    if (rzucanie != null) rzucanie.wylacz();
    if (nosnik != null) for (Player p : getServer().getOnlinePlayers()) nosnik.usunPuste(p);
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    ksiega = new Ksiega(getConfig());
    nosnik = new Nosnik(this, ksiega);
    getServer().getPluginManager().registerEvents(this, this);
    rzucanie = new Rzucanie(this, ksiega, nosnik);
    ulubione = new Ulubione(this, ksiega, nosnik, rzucanie);
    getServer().getPluginManager().registerEvents(rzucanie, this);
    getServer().getPluginManager().registerEvents(nosnik, this);
    getServer().getPluginManager().registerEvents(ulubione, this);
    wybor = new Wybor(this, ksiega, nosnik, ulubione, rzucanie);
    rzucanie.ustawWybor(wybor);
    getServer().getPluginManager().registerEvents(wybor, this);
    kotwica = new Kotwica(this, rzucanie);
    getServer().getPluginManager().registerEvents(kotwica, this);
    if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) new Placeholdery(ksiega, ulubione, rzucanie).register();
    getLogger().info("zaklecia: " + ksiega.poId.keySet());
  }

  /**
   * {@code zaklecia przypisz <gracz> <id|nic> <prawa|lewa>}, {@code zaklecia ulubione <gracz> <id>} — wolane
   * z Ksiegi zaklec (DeluxeMenus, jako konsola).
   */
  @Override
  public boolean onCommand(org.bukkit.command.CommandSender s, org.bukkit.command.Command c, String l, String[] a) {
    if (a.length == 2 && a[0].equalsIgnoreCase("q")) {
      // E: klawisz Q (MenuKey wola 'zaklecia q <gracz>'): z Shiftem okno ulubionych, bez — hub jak dotad
      Player p = getServer().getPlayerExact(a[1]);
      if (p == null) { s.sendMessage("Brak gracza " + a[1]); return true; }
      rzucanie.oznaczQ(p);
      // MenuKey przechwytuje pakiet Q, a klient juz "wyrzucil" przedmiot z reki (przewidywanie) — odswiez slot
      getServer().getScheduler().runTask(this, p::updateInventory);
      // Shift+Q = szybki wybor (pasek, wlacz/wylacz); Q przy otwartym pasku zamyka go; samo Q = hub
      if (p.isSneaking() || wybor.otwarty(p)) { wybor.przelacz(p); return true; }
      String k = getConfig().getString("klawisz-q.zwykle", "dm open hub_glowne %gracz%");
      getServer().dispatchCommand(getServer().getConsoleSender(), k.replace("%gracz%", p.getName()));
      return true;
    }
    if (a.length == 3 && a[0].equalsIgnoreCase("ulubione")) {
      Player p = getServer().getPlayerExact(a[1]);
      if (p == null) { s.sendMessage("Brak gracza " + a[1]); return true; }
      nosnik.komunikat(p, ulubione.przelacz(p, a[2].toLowerCase(Locale.ROOT)));
      return true;
    }
    if (a.length != 4 || !a[0].equalsIgnoreCase("przypisz")) {
      s.sendMessage("/zaklecia przypisz <gracz> <" + String.join("|", ksiega.poId.keySet()) + "|nic> <prawa|lewa>");
      return true;
    }
    Player p = getServer().getPlayerExact(a[1]);
    if (p == null) { s.sendMessage("Brak gracza " + a[1]); return true; }
    String id = a[2].equalsIgnoreCase("nic") ? null : a[2].toLowerCase(Locale.ROOT);
    String reka = a[3].toLowerCase(Locale.ROOT);
    if (!reka.equals("prawa") && !reka.equals("lewa")) { s.sendMessage("Reka: prawa albo lewa"); return true; }
    boolean prawa = reka.equals("prawa");
    rzucanie.stop(p, false);
    rzucanie.zdejmijBronie(p);
    nosnik.komunikat(p, nosnik.przypisz(p, id, prawa));
    return true;
  }

  /** Ktora dlon (prawa = glowna, lewa = druga) trzyma zaklecie z (albo dowolne). */
  private boolean wDloni(Player p, String reka, String z) {
    ItemStack i = reka.equals("lewa") ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
    Ksiega.Zaklecie zak = ksiega.w(i);
    // w trakcie rzucania liczy sie tylko AKTYWNA dlon (LPM/PPM osobno) — dzwieki i gesty tylko rzucajacej reki
    return zak != null && (z.equals("dowolne") || zak.id().equals(z)) && rzucanie.aktywna(p, !reka.equals("lewa"));
  }

  private static String zasob(MythicLineConfig cfg) {
    String z = cfg.getString(new String[] {"zasob", "z"}, "mana").toLowerCase(Locale.ROOT);
    if (!z.equals("mana") && !z.equals("stamina")) throw new IllegalArgumentException("zasob=" + z);
    return z;
  }

  private static double stan(PlayerData d, String zasob) {
    return zasob.equals("mana") ? d.getMana() : d.getStamina();
  }

  @EventHandler
  public void mechaniki(MythicMechanicLoadEvent e) {
    if (!e.getMechanicName().equalsIgnoreCase("zakZasob")) return;
    MythicLineConfig cfg = e.getConfig();
    String zasob = zasob(cfg);
    double ile = cfg.getDouble(new String[] {"amount", "a"}, 1);
    e.register(new ITargetedEntitySkill() {
      @Override
      public ThreadSafetyLevel getThreadSafetyLevel() {
        return ThreadSafetyLevel.SYNC_ONLY; // PlayerResourceUpdateEvent jest zdarzeniem Bukkit
      }

      @Override
      public SkillResult castAtEntity(SkillMetadata data, AbstractEntity cel) {
        try {
          if (!(cel.getBukkitEntity() instanceof Player p)) return SkillResult.INVALID_TARGET;
          PlayerData d = PlayerData.get(p);
          if (zasob.equals("mana")) d.giveMana(-ile, ResourceUpdateReason.SKILL);
          else d.giveStamina(-ile, ResourceUpdateReason.SKILL);
          return SkillResult.SUCCESS;
        } catch (Throwable t) { // MM polyka wyjatki mechanik bez sladu — logujemy sami
          getLogger().log(Level.WARNING, "zakZasob: " + t, t);
          return SkillResult.ERROR;
        }
      }
    });
  }

  @EventHandler
  public void mechanikaUspokojenia(MythicMechanicLoadEvent e) {
    if (!e.getMechanicName().equalsIgnoreCase("zakUspokoj")) return;
    int ticki = Math.max(1, e.getConfig().getInteger("t", 300));
    e.register(new ITargetedEntitySkill() {
      @Override public ThreadSafetyLevel getThreadSafetyLevel() { return ThreadSafetyLevel.SYNC_ONLY; }
      @Override public SkillResult castAtEntity(SkillMetadata data, AbstractEntity cel) {
        if (!(cel.getBukkitEntity() instanceof org.bukkit.entity.Mob m)) return SkillResult.INVALID_TARGET;
        rzucanie.uspokoj(m, ticki);
        return SkillResult.SUCCESS;
      }
    });
  }

  @EventHandler
  public void warunkiDloni(MythicConditionLoadEvent e) {
    String nazwa = e.getConditionName().toLowerCase(Locale.ROOT);
    if (!nazwa.equals("zakdlon") && !nazwa.equals("zakrece")) return;
    MythicLineConfig cfg = e.getConfig();
    String z = cfg.getString(new String[] {"zaklecie", "z"}, "dowolne").toLowerCase(Locale.ROOT);
    if (nazwa.equals("zakdlon")) {
      // zakDlon{reka=prawa|lewa;z=plomienie|odmrozenie|dowolne}
      String reka = cfg.getString(new String[] {"reka", "r"}, "prawa").toLowerCase(Locale.ROOT);
      e.register((IEntityCondition) cel -> cel.getBukkitEntity() instanceof Player p && wDloni(p, reka, z));
    } else {
      // zakRece{z=..;min=1;max=2} — w ilu dloniach jest zaklecie z
      int min = cfg.getInteger("min", 1), max = cfg.getInteger("max", 2);
      e.register((IEntityCondition) cel -> {
        if (!(cel.getBukkitEntity() instanceof Player p)) return false;
        int n = (wDloni(p, "prawa", z) ? 1 : 0) + (wDloni(p, "lewa", z) ? 1 : 0);
        return n >= min && n <= max;
      });
    }
  }

  @EventHandler
  public void warunki(MythicConditionLoadEvent e) {
    if (!e.getConditionName().equalsIgnoreCase("zakMaZasob")) return;
    MythicLineConfig cfg = e.getConfig();
    String zasob = zasob(cfg);
    double ile = cfg.getDouble(new String[] {"amount", "a"}, 1);
    e.register((IEntityCondition) cel -> {
      if (!(cel.getBukkitEntity() instanceof Player p)) return false;
      return stan(PlayerData.get(p), zasob) >= ile;
    });
  }
}
