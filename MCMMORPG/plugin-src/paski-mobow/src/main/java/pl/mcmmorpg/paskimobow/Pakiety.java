package pl.mcmmorpg.paskimobow;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pakiety paska: text_display istnieje tylko u klienta i jedzie na mobie jako pasazer (SET_PASSENGERS).
 * Indeksy metadanych 1.21.11: 2 nazwa, 3 nazwa widoczna; Display 11 przesuniecie, 12 skala, 15 billboard,
 * 16 jasnosc, 17 zasieg; TextDisplay 23 tekst, 24 szerokosc linii, 25 tlo, 27 flagi (cien/przezroczystosc).
 */
final class Pakiety {
  private static final int JASNOSC_PELNA = (15 << 4) | (15 << 20);

  private final Map<Integer, Pasek> poMobie;   // id moba -> pasek (czytane z watku netty)
  private PacketListenerCommon nasluch;
  volatile boolean ukryjNazwe = true;

  Pakiety(Map<Integer, Pasek> poMobie) { this.poMobie = poMobie; }

  void wlacz() {
    nasluch = PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.HIGH) {
      @Override public void onPacketSend(PacketSendEvent e) {
        if (e.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
          if (!ukryjNazwe) return;
          WrapperPlayServerEntityMetadata w = new WrapperPlayServerEntityMetadata(e);
          if (!poMobie.containsKey(w.getEntityId())) return;
          boolean zmiana = false;
          for (EntityData<?> d : w.getEntityMetadata()) {
            if (d.getIndex() == 2) { ustaw(d, Optional.empty()); zmiana = true; }
            else if (d.getIndex() == 3) { ustaw(d, Boolean.FALSE); zmiana = true; }
          }
          if (zmiana) e.markForReEncode(true);
        } else if (e.getPacketType() == PacketType.Play.Server.SET_PASSENGERS) {
          WrapperPlayServerSetPassengers w = new WrapperPlayServerSetPassengers(e);
          Pasek p = poMobie.get(w.getEntityId());
          if (p == null) return;
          int[] pas = w.getPassengers();
          for (int id : pas) if (id == p.idWyswietlacza) return;
          int[] nowe = Arrays.copyOf(pas, pas.length + 1);
          nowe[pas.length] = p.idWyswietlacza;
          w.setPassengers(nowe);
          e.markForReEncode(true);
        }
      }
    });
  }

  void wylacz() {
    if (nasluch != null) PacketEvents.getAPI().getEventManager().unregisterListener(nasluch);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void ustaw(EntityData d, Object wartosc) { d.setValue(wartosc); }

  void pokaz(Player gracz, Pasek p, Entity mob, Component tekst, Ustawienia u) {
    Location l = mob.getLocation();
    wyslij(gracz, new WrapperPlayServerSpawnEntity(p.idWyswietlacza, Optional.of(UUID.randomUUID()), EntityTypes.TEXT_DISPLAY,
        new Vector3d(l.getX(), l.getY() + mob.getHeight(), l.getZ()), 0f, 0f, 0f, 0, Optional.empty()));
    float s = (float) u.skala();
    List<EntityData<?>> m = new ArrayList<>();
    m.add(new EntityData<>(11, EntityDataTypes.VECTOR3F, new Vector3f(0f, (float) u.wysokosc(), 0f)));
    m.add(new EntityData<>(12, EntityDataTypes.VECTOR3F, new Vector3f(s, s, s)));
    m.add(new EntityData<>(15, EntityDataTypes.BYTE, (byte) 3));          // billboard CENTER
    m.add(new EntityData<>(16, EntityDataTypes.INT, JASNOSC_PELNA));      // nie ciemnieje w nocy i w jaskini
    m.add(new EntityData<>(17, EntityDataTypes.FLOAT, (float) (u.zasieg() / 64.0)));
    m.add(new EntityData<>(23, EntityDataTypes.ADV_COMPONENT, tekst));
    m.add(new EntityData<>(24, EntityDataTypes.INT, 400));
    m.add(new EntityData<>(25, EntityDataTypes.INT, 0));                  // bez tla
    m.add(new EntityData<>(27, EntityDataTypes.BYTE, (byte) 0));          // bez cienia (cien psuje glify)
    wyslij(gracz, new WrapperPlayServerEntityMetadata(p.idWyswietlacza, m));
    int[] pasazerowie = mob.getPassengers().stream().mapToInt(Entity::getEntityId).toArray();
    int[] z = Arrays.copyOf(pasazerowie, pasazerowie.length + 1);
    z[pasazerowie.length] = p.idWyswietlacza;
    wyslij(gracz, new WrapperPlayServerSetPassengers(mob.getEntityId(), z));
  }

  void tekst(Player gracz, Pasek p, Component tekst) {
    wyslij(gracz, new WrapperPlayServerEntityMetadata(p.idWyswietlacza,
        List.of(new EntityData<>(23, EntityDataTypes.ADV_COMPONENT, tekst))));
  }

  void ukryj(Player gracz, Pasek p) {
    wyslij(gracz, new WrapperPlayServerDestroyEntities(p.idWyswietlacza));
  }

  private static void wyslij(Player gracz, com.github.retrooper.packetevents.wrapper.PacketWrapper<?> w) {
    PacketEvents.getAPI().getPlayerManager().sendPacket(gracz, w);
  }
}
