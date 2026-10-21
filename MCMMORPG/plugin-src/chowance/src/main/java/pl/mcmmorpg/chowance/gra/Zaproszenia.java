package pl.mcmmorpg.chowance.gra;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import pl.mcmmorpg.chowance.Chowance;

/** Zaproszenia do walki turowej, pojedynku i handlu. Jedno oczekujące na adresata, z terminem. */
public final class Zaproszenia {
  public enum Rodzaj { WALKA, POJEDYNEK, HANDEL }

  public record Zaproszenie(UUID od, UUID doKogo, Rodzaj rodzaj, long wygasa) {}

  private final Chowance c;
  private final Map<UUID, Zaproszenie> oczekujace = new ConcurrentHashMap<>();

  /** Okno zaproszenia z przyciskami (ustawiane przez plugin); bez niego zostaje tylko czat. */
  public Okna okna;

  public Zaproszenia(Chowance c) { this.c = c; }

  public void wyslij(Player od, Player doKogo, Rodzaj r, int sekund) {
    oczekujace.put(doKogo.getUniqueId(), new Zaproszenie(od.getUniqueId(), doKogo.getUniqueId(), r, System.currentTimeMillis() + sekund * 1000L));
    String co = switch (r) { case WALKA -> "walkę turową"; case POJEDYNEK -> "walkę real-time (stwory walczą same)"; case HANDEL -> "wymianę chowańców"; };
    c.msg(doKogo, "<yellow>" + od.getName() + "</yellow> proponuje " + co + ". <green><click:run_command:'/chw akceptuj'>[/chw akceptuj]</click></green> <red><click:run_command:'/chw odrzuc'>[/chw odrzuc]</click></red> <gray>(" + sekund + " s)");
    c.msg(od, "<gray>Wysłano zaproszenie do " + doKogo.getName() + ".");
    if (okna != null) {
      okna.pokaz(doKogo, "<gold>Zaproszenie", java.util.List.of("<yellow>" + od.getName() + "</yellow> <gray>proponuje " + co + "."),
          java.util.List.of(Okna.Przycisk.of("<green>Akceptuj", "", x -> x.performCommand("chw akceptuj"))),
          Okna.Przycisk.of("<red>Odrzuć", "", x -> x.performCommand("chw odrzuc")), 2, true, false);
    }
  }

  /** Zdejmuje i zwraca ważne zaproszenie adresata. */
  public Zaproszenie odbierz(Player doKogo) {
    Zaproszenie z = oczekujace.remove(doKogo.getUniqueId());
    if (z == null || z.wygasa() < System.currentTimeMillis()) return null;
    return z;
  }

  public void usun(UUID gracz) {
    oczekujace.remove(gracz);
    oczekujace.values().removeIf(z -> z.od().equals(gracz));
  }
}
