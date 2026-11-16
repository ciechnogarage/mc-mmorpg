package pl.mcmmorpg.licho;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Gracz w rundzie. Licho = niewidzialny gracz + wyświetlacz sprzętu + zaczepa (hitbox); Zamawiacz = zwykły gracz. */
final class Uczestnik {
  enum Rola { LICHO, ZAMAWIACZ }

  final UUID id;
  final String nick;
  Rola rola = Rola.LICHO;
  boolean bylLicho;              // zaczynał rundę jako Licho (do podsumowania)
  int iskry;
  int tchu;
  int punkty, trafienia, zlapani;
  boolean przetrwal;

  BlockData ksztalt;
  BlockDisplay wyglad;
  Interaction zaczepa;
  Block zastygly;                // blok, w który Licho wskoczyło; null = rusza się
  Location ostatnia;
  int bezRuchu;
  boolean zlyGrunt;              // podpowiedź „tu nie zastygniesz” już pokazana

  int wcielenieDo, psotaDo, okadzanieDo, ciosDo, zadyszkaDo, tchuOd, szeptOd;

  // stan sprzed gry, przywracany przy wyjściu
  GameMode trybPrzed;
  boolean latanieDozwolonePrzed;

  Uczestnik(Player p) {
    id = p.getUniqueId();
    nick = p.getName();
    trybPrzed = p.getGameMode();
    latanieDozwolonePrzed = p.getAllowFlight();
  }

  Player gracz() { return Bukkit.getPlayer(id); }
  boolean licho() { return rola == Rola.LICHO; }
}
