package pl.mcmmorpg.charactercreator.scene;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Sam pakiet "patrz przez te encje" — bez zmiany stanu serwera. Klient, ktory juz patrzy przez
 * kamere, nic nie zauwazy; klient, ktory zgubil kamere, wraca do niej.
 *
 * <p>Po co: klient IGNORUJE pakiet kamery, jesli nie zna jeszcze encji, a serwer wysyla encje
 * dopiero po wyslaniu jej chunka. Przy wolnym wejsciu (proxy, obciazony serwer) albo po skoku
 * do innego kadru pakiet przychodzil przed encja — gracz mial wolna kamere, A/D dzialalo, LPM
 * nie (celownik nie trafial w tarcze klikow), a serwer byl pewien, ze kamera jest podpieta
 * (zgloszenie 01.10). Packetevents to softdepend — bez niego {@link SceneManager} ponawia
 * podpiecie przez zdjecie i nalozenie celu.
 */
final class CameraPacket {

  private CameraPacket() {
  }

  static boolean available() {
    return Bukkit.getPluginManager().isPluginEnabled("packetevents");
  }

  static void send(Player player, Entity camera) {
    PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerCamera(camera.getEntityId()));
  }
}
