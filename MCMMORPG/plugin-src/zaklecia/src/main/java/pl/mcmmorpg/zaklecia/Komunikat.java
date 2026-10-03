package pl.mcmmorpg.zaklecia;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.Indyuce.mmocore.api.player.PlayerData;
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

  void mana(Player p) {
    if (pokaz(p, "NO_MANA", "§x§F§2§F§2§E§DBrakuje §x§7§8§B§9§E§Fmany"))
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
    PlayerData dane = PlayerData.get(p);
    if (dane != null) dane.displayActionBar(tekst);
    else p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(tekst));
    return true;
  }
}
