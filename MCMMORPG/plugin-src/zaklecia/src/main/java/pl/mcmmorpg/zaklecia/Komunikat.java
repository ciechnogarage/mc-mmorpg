package pl.mcmmorpg.zaklecia;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Krotkie odmowy: jedna emisja danego powodu na gracza przez dwie sekundy. */
final class Komunikat {
  private final Skille skille;
  private final String brakMany;
  private final Map<UUID, Map<String, Long>> ostatnie = new HashMap<>();

  Komunikat(Skille skille, String brakMany) {
    this.skille = skille; this.brakMany = brakMany;
  }

  void wyjscie(Player p) { ostatnie.remove(p.getUniqueId()); }

  void mana(Player p, double brak) {
    String liczba = String.format(Locale.forLanguageTag("pl-PL"), "%.1f", Math.ceil(Math.max(0, brak) * 10) / 10);
    if (liczba.endsWith(",0")) liczba = liczba.substring(0, liczba.length() - 2);
    if (pokaz(p, "NO_MANA", "§x§F§2§F§2§E§DBrakuje " + liczba + " §x§7§8§B§9§E§Fmany"))
      skille.cast(p, brakMany);
  }

  void odmowa(Player p, String kod, String tekst) {
    if (pokaz(p, kod, "§x§E§8§7§9§7§3" + tekst))
      p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.6f);
  }

  private boolean pokaz(Player p, String kod, String tekst) {
    long teraz = System.currentTimeMillis();
    Map<String, Long> gracz = ostatnie.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
    if (teraz - gracz.getOrDefault(kod, 0L) < 2000) return false;
    gracz.put(kod, teraz);
    // priorytet 40 MythicLib: odmowy przykrywaja pasek MMOCore (10), nawigacje Mapy (20) i informacje (30)
    try { MMOPlayerData.get(p).getActionBar().show(40, 40L, tekst); }
    catch (Throwable t) { p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(tekst)); }
    return true;
  }
}
