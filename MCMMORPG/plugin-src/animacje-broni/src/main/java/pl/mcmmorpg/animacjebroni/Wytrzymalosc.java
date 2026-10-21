package pl.mcmmorpg.animacjebroni;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import net.Indyuce.mmocore.api.event.PlayerResourceUpdateEvent;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.profess.resource.PlayerResource;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stamina liczona w jednostkach L = jeden lekki cios mieczem (wzorca).
 *
 * <p>Cel projektowy (zestaw): ile sekund ciaglej walki wystarcza pelny pasek. Pula = zestaw * ciosy_na_sekunde L,
 * wiec L = MAX_STAMINA / (zestaw * ciosy_na_sekunde). Koszt lekkiego ciosu bronia = L * ciosy_na_sekunde / szybkosc
 * ataku broni: kazda bron pozwala walczyc bez przerwy tyle samo sekund, rozni sie tylko wielkoscia porcji.
 * Regeneracja jest wlasna: kazdy wydatek staminy (takze z innych pluginow) wstrzymuje ja na oddech_ticki,
 * w trakcie gardy nie rosnie, potem oddaje regeneracja L/s (pelny pasek po ~zestaw sekund spokoju).
 * Wbudowana regeneracja MMOCore jest anulowana, zeby nie lecialy dwie naraz.
 */
final class Wytrzymalosc implements Listener {
  private final AnimacjeBroni plugin;
  private final Map<UUID, Integer> ostatniWydatek = new HashMap<>();
  private final Map<UUID, Integer> ostatniaZadyszka = new HashMap<>();
  private boolean wlaczona;
  private int zestaw;
  private List<Integer> zestawy = List.of(7, 10, 14);
  private double ciosyNaSekunde, regeneracja, zmeczenieObrazenia, szturchniecie;
  private double blokMin, blokMax, blokProgHp, tarcza, parowanieZwrot, wspMin, wspMax;
  private int oddechTicki, peknietaGardaTicki;
  /** Wlasna regeneracja dodaje stamine przez MMOCore - ten znacznik przepuszcza ja przez anulowanie. */
  private boolean regenerujemy;

  Wytrzymalosc(AnimacjeBroni plugin) { this.plugin = plugin; }

  void wczytaj(ConfigurationSection s) {
    wlaczona = s != null && s.getBoolean("wlaczona", false);
    if (s == null) return;
    zestawy = s.getIntegerList("zestawy").isEmpty() ? List.of(7, 10, 14) : s.getIntegerList("zestawy");
    zestaw = s.getInt("zestaw", 10);
    ciosyNaSekunde = s.getDouble("ciosy_na_sekunde", 1.6);
    regeneracja = s.getDouble("regeneracja", 1.6);
    oddechTicki = s.getInt("oddech_ticki", 20);
    zmeczenieObrazenia = s.getDouble("zmeczenie_obrazenia", 0.6);
    szturchniecie = s.getDouble("szturchniecie", 1.0);
    blokMin = s.getDouble("blok_min", 0.5);
    blokMax = s.getDouble("blok_max", 3.0);
    blokProgHp = s.getDouble("blok_prog_hp", 0.25);
    tarcza = s.getDouble("tarcza", 0.5);
    parowanieZwrot = s.getDouble("parowanie_zwrot", 1.0);
    peknietaGardaTicki = s.getInt("peknieta_garda_ticki", 40);
    wspMin = s.getDouble("wspolczynnik_min", 0.4);
    wspMax = s.getDouble("wspolczynnik_max", 2.5);
  }

  boolean wlaczona() { return wlaczona; }
  int zestaw() { return zestaw; }
  List<Integer> zestawy() { return zestawy; }
  void ustawZestaw(int z) { zestaw = z; }

  /** Punkty staminy MMOCore na jedna jednostke L dla tego gracza. */
  double jednostka(Player p) {
    double maks = Math.max(0, MMOPlayerData.get(p).getStatMap().getStat("MAX_STAMINA"));
    return maks / Math.max(1e-6, zestaw * ciosyNaSekunde);
  }

  /** Ile L kosztuje lekki cios trzymana bronia: wolniejsza (ciezsza) bron = wieksza porcja. */
  double wspolczynnikBroni(Player p) {
    var a = p.getAttribute(Attribute.ATTACK_SPEED);
    double szybkosc = a == null ? 4 : a.getValue();
    return Math.max(wspMin, Math.min(wspMax, ciosyNaSekunde / Math.max(0.1, szybkosc)));
  }

  /** Lekki cios: porcja wg szybkosci ataku (wyrownuje czas walki) x waga broni (ciezka meczy bardziej). */
  double kosztLekkiego(Player p) { return jednostka(p) * wspolczynnikBroni(p) * plugin.wagaBroni(p); }

  double kosztMocnego(Player p, double mnoznikWariantu) { return kosztLekkiego(p) * mnoznikWariantu; }

  double kosztSzturchniecia(Player p) { return kosztLekkiego(p) * szturchniecie; }

  double stamina(Player p) {
    PlayerData d = PlayerData.get(p);
    return d == null ? 0 : d.getStamina();
  }

  boolean zmeczony(Player p) { return stamina(p) + 1e-6 < kosztLekkiego(p); }

  double zmeczenieObrazenia() { return zmeczenieObrazenia; }

  /** Pobiera dokladnie koszt, jesli wystarcza staminy. */
  boolean wydaj(Player p, double punkty) {
    PlayerData d = PlayerData.get(p);
    if (d == null || punkty <= 0) return true;
    if (d.getStamina() + 1e-6 < punkty) return false;
    d.giveStamina(-punkty, PlayerResourceUpdateEvent.UpdateReason.SKILL_COST);
    return true;
  }

  /** Wysilek, ktorego nie da sie odmowic (machniecie juz sie stalo): pobiera ile jest, do zera. */
  void wydajIleSie(Player p, double punkty) {
    PlayerData d = PlayerData.get(p);
    if (d == null || punkty <= 0) return;
    double ile = Math.min(d.getStamina(), punkty);
    if (ile > 0) d.giveStamina(-ile, PlayerResourceUpdateEvent.UpdateReason.SKILL_COST);
    else ostatniWydatek.put(p.getUniqueId(), plugin.getServer().getCurrentTick());   // pusty pasek: dalej bez oddechu
  }

  void zwroc(Player p, double punkty) {
    PlayerData d = PlayerData.get(p);
    if (d == null || punkty <= 0) return;
    regenerujemy = true;
    try { d.giveStamina(punkty, PlayerResourceUpdateEvent.UpdateReason.OTHER); }
    finally { regenerujemy = false; }
  }

  void zwrocZaParowanie(Player p) { zwroc(p, jednostka(p) * parowanieZwrot); }

  /** Lekki cios w zadyszce: oddech + komunikat co 2 s. */
  void sygnalizujZadyszke(Player p) {
    int teraz = plugin.getServer().getCurrentTick();
    if (teraz - ostatniaZadyszka.getOrDefault(p.getUniqueId(), -1000) < 40) return;
    ostatniaZadyszka.put(p.getUniqueId(), teraz);
    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 0.8f, 0.7f);
    plugin.komunikatWytrzymalosci(p, "&x&F&2&F&2&E&DZadyszka: &x&E&8&7&9&7&3słabsze ciosy");
  }

  /** Co 2 ticki: regeneracja po oddechu, bez regeneracji w gardzie. */
  void tyknij() {
    if (!wlaczona) return;
    int teraz = plugin.getServer().getCurrentTick();
    for (Player p : plugin.getServer().getOnlinePlayers()) {
      if (p.isDead() || p.isBlocking()) continue;
      if (teraz - ostatniWydatek.getOrDefault(p.getUniqueId(), -1000) < oddechTicki) continue;
      PlayerData d = PlayerData.get(p);
      if (d == null) continue;
      double maks = MMOPlayerData.get(p).getStatMap().getStat("MAX_STAMINA");
      if (d.getStamina() >= maks) continue;
      zwroc(p, Math.min(maks - d.getStamina(), regeneracja * jednostka(p) * 0.1));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void zmianaStaminy(PlayerResourceUpdateEvent e) {
    if (!wlaczona || e.getResource() != PlayerResource.STAMINA || regenerujemy) return;
    if (e.getReason() == PlayerResourceUpdateEvent.UpdateReason.REGENERATION) { e.setCancelled(true); return; }
    if (e.getDifference() < 0) ostatniWydatek.put(e.getPlayer().getUniqueId(), plugin.getServer().getCurrentTick());
  }

  /**
   * Zablokowany cios kosztuje tyle, ile wazyl: od blok_min L (lekki cios) do blok_max L (cios za blok_prog_hp
   * maks. zdrowia lub wiecej); tarcza polowe. Brak staminy = garda peka (cooldown przedmiotu, opuszczona reka).
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void blok(EntityDamageByEntityEvent e) {
    // obrazenia 0 = udane parowanie (AnimacjeBroni#parowanie) - ono zwraca stamine zamiast kosztowac;
    // ~0 to nie cios (zmierzone na klonie: SLIME o nieustalonym pochodzeniu, 2e-8 co tick przy dotyku) - inaczej garda topnialaby od widm
    if (!wlaczona || !(e.getEntity() instanceof Player p) || !p.isBlocking() || e.getDamage() < 0.01) return;
    Vector doNapastnika = e.getDamager().getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
    Vector patrzy = p.getLocation().getDirection().setY(0);
    if (doNapastnika.lengthSquared() < 1e-4 || patrzy.lengthSquared() < 1e-4
        || Math.toDegrees(doNapastnika.angle(patrzy)) > 90) return;
    ItemStack garda = p.getActiveItem();
    boolean zTarcza = garda.getType() == Material.SHIELD;
    var hp = p.getAttribute(Attribute.MAX_HEALTH);
    double prog = Math.max(1, (hp == null ? 20 : hp.getValue()) * blokProgHp);
    double l = blokMin + (blokMax - blokMin) * Math.min(1, e.getDamage() / prog);
    double koszt = jednostka(p) * l * (zTarcza ? tarcza : 1);
    boolean ok = wydaj(p, koszt);
    plugin.dbgWytrzymalosci(String.format(java.util.Locale.ROOT, "BLOK_STAMINA cios=%.2f L=%.2f koszt=%.2f tarcza=%b peka=%b",
        e.getDamage(), l, koszt, zTarcza, !ok));
    if (ok) return;
    wydajIleSie(p, koszt);
    if (!garda.getType().isAir()) p.setCooldown(garda, peknietaGardaTicki);
    p.clearActiveItem();
    p.getWorld().playSound(p.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.9f, 0.9f);
    plugin.komunikatWytrzymalosci(p, "&x&E&8&7&9&7&3Garda pęka &x&F&2&F&2&E&D— brak staminy");
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void wyjscie(PlayerQuitEvent e) {
    ostatniWydatek.remove(e.getPlayer().getUniqueId());
    ostatniaZadyszka.remove(e.getPlayer().getUniqueId());
  }

  String opis(Player p) {
    if (!wlaczona) return "stamina: model wylaczony";
    return String.format(java.util.Locale.ROOT,
        "zestaw=%ds L=%.2f pkt wsp_broni=%.2f waga=%.1f lekki=%.2f stamina=%.2f/%.0f oddech=%dt", zestaw, jednostka(p),
        wspolczynnikBroni(p), plugin.wagaBroni(p), kosztLekkiego(p), stamina(p), MMOPlayerData.get(p).getStatMap().getStat("MAX_STAMINA"),
        Math.max(0, oddechTicki - (plugin.getServer().getCurrentTick() - ostatniWydatek.getOrDefault(p.getUniqueId(), -1000))));
  }
}
