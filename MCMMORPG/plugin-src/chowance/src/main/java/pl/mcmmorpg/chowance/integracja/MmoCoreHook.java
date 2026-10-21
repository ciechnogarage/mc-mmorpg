package pl.mcmmorpg.chowance.integracja;

import java.util.logging.Logger;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.experience.EXPSource;
import net.Indyuce.mmocore.experience.Profession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Profesja „Poskramiacz” przez API MMOCore (PlayerProfessions#giveExperience, EXPSource.SOURCE).
 * Brak MMOCore albo brak pliku profesji = poziom 0 i cichy brak exp + JEDNO ostrzeżenie w logu.
 */
public final class MmoCoreHook {
  private final String profesjaId;
  private final Logger log;
  private boolean ostrzezono;

  public MmoCoreHook(String profesjaId, Logger log) {
    this.profesjaId = profesjaId;
    this.log = log;
  }

  private Profession profesja() {
    if (!Bukkit.getPluginManager().isPluginEnabled("MMOCore")) return null;
    try {
      if (!MMOCore.plugin.professionManager.has(profesjaId)) {
        if (!ostrzezono) {
          log.warning("MMOCore nie zna profesji '" + profesjaId + "' (plik MMOCore/professions/" + profesjaId
              + ".yml robi Agent B) — exp Poskramiacza nie będzie naliczany, poziom = 0.");
          ostrzezono = true;
        }
        return null;
      }
      return MMOCore.plugin.professionManager.get(profesjaId);
    } catch (RuntimeException | LinkageError e) {
      return null;
    }
  }

  public boolean dostepna() { return profesja() != null; }

  public int poziom(Player p) {
    Profession pr = profesja();
    if (pr == null) return 0;
    try {
      return PlayerData.get(p).getCollectionSkills().getLevel(pr);
    } catch (RuntimeException e) {
      return 0;
    }
  }

  public void dajExp(Player p, double ile) {
    Profession pr = profesja();
    if (pr == null || ile <= 0) return;
    try {
      PlayerData.get(p).getCollectionSkills().giveExperience(pr, ile, EXPSource.SOURCE);
    } catch (RuntimeException e) {
      log.warning("Nie udało się dać exp profesji " + profesjaId + " graczowi " + p.getName() + ": " + e);
    }
  }

  /**
   * Komunikat przez wspólny MythicLib ActionBarHandler. Priorytet 30 = informacja, 40 = odmowa;
   * oba wyprzedzają nawigację (20) i zwykły stan MMOCore (10). false = dane gracza niedostępne.
   */
  public boolean pasek(Player p, String legacy, int priorytet) {
    if (!Bukkit.getPluginManager().isPluginEnabled("MMOCore")) return false;
    try {
      PlayerData d = PlayerData.get(p);
      if (d == null) return false;
      io.lumine.mythic.lib.api.player.MMOPlayerData.get(p).getActionBar().show(priorytet, 40L, legacy);
      return true;
    } catch (RuntimeException | LinkageError e) {
      return false;
    }
  }

  /** Poziom postaci (klasy) MMOCore; bez MMOCore — poziom doświadczenia vanilli (min. 1). */
  public int poziomPostaci(Player p) {
    if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
      try {
        PlayerData d = PlayerData.get(p);
        if (d != null) return Math.max(1, d.getLevel());
      } catch (RuntimeException | LinkageError e) {
        // niżej
      }
    }
    return Math.max(1, p.getLevel());
  }

  /** Klasa MMOCore (małymi literami) — do oferty starterów. */
  public String klasa(Player p) {
    if (!Bukkit.getPluginManager().isPluginEnabled("MMOCore")) return null;
    try {
      var c = PlayerData.get(p).getProfess();
      return c == null ? null : c.getId().toLowerCase(java.util.Locale.ROOT);
    } catch (RuntimeException e) {
      return null;
    }
  }
}
