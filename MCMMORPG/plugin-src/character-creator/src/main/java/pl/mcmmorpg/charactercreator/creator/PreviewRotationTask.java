package pl.mcmmorpg.charactercreator.creator;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Obraca podglad postaci i utrzymuje delikatny ruch wokol podium.
 *
 * <p><b>Dlaczego serwerowy {@link Entity#setRotation}, a nie pakiety.</b> Poprzednia wersja
 * wysylala {@code WrapperPlayServerEntityRotation} przez packetevents tylko do jednego
 * gracza. Task startowal poprawnie (brak ostrzezenia w logu), ale model **nie obracal sie
 * wcale** — zmierzone na zrzutach z prawdziwego klienta: roznica w obszarze postaci miedzy
 * dwoma ujeciami tej samej sesji wynosila 0.09/255, czyli piksel w piksel to samo.
 * Najbardziej prawdopodobna przyczyna: LibsDisguises zarzadza wlasnymi pakietami dla
 * przebranej encji i nadpisuje nasze. Zamiast walczyc z cudzymi pakietami, obracamy encje
 * naprawde — wtedy i disguise, i zalozony ekwipunek podazaja za nia same.
 *
 * <p>Zadanie tasku to teraz: utrzymac RECZNIE wybrany kat obrotu (serwer potrafi go
 * zresetowac) i siac czastki wokol podium, zeby scena nie byla calkiem martwa.
 */
public final class PreviewRotationTask extends BukkitRunnable {

  private final CreatorSessionManager sessions;
  private int tick;

  public PreviewRotationTask(CreatorSessionManager sessions) {
    this.sessions = sessions;
  }

  @Override
  public void run() {
    tick++;
    for (Map.Entry<UUID, CreatorSession> entry : sessions.activeSessions().entrySet()) {
      Player player = Bukkit.getPlayer(entry.getKey());
      Entity dummy = entry.getValue().previewDummy();
      if (player == null || !player.isOnline() || dummy == null || dummy.isDead()) {
        continue;
      }
      // Model stoi nieruchomo, przodem do gracza. Ani spinu, ani recznego obracania —
      // obie wersje probowalismy i obie wygladaly gorzej niz statyczna poza. Kat i tak
      // trzeba wymuszac co tick, bo serwer resetuje go przy synchronizacji encji.
      float yaw = entry.getValue().modelYaw();
      dummy.setRotation(yaw, 0f);
      if (dummy instanceof LivingEntity living) {
        living.setBodyYaw(yaw);
      }

      // Czastki tylko dla tworzacego gracza — inni nie maja powodu ich widziec.
      if (tick % 5 == 0) {
        Location base = dummy.getLocation();
        player.spawnParticle(Particle.END_ROD, base.getX(), base.getY() + 0.1,
            base.getZ(), 2, 0.6, 0.05, 0.6, 0.0);
      }
    }
  }
}
