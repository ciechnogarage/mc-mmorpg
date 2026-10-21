package pl.mcmmorpg.chowance.gra;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.api.event.ChowaniecZlapanyEvent;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.logika.Generator;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;

/**
 * Starter (SPEC §3): oferta zależna od rasy (CharacterCreator) albo klasy MMOCore. Wybiera się go w zagrodzie
 * młodych ({@link Zagroda}) — okno wyboru po wejściu usunięte 18.10.
 */
public final class Startery {
  private final Chowance c;
  private final Druzyny druzyny;
  public java.util.function.BiConsumer<Player, ProfilDane> poWyborze = (p, d) -> {};

  public Startery(Chowance c, Druzyny druzyny) {
    this.c = c;
    this.druzyny = druzyny;
  }

  /** Rasa z uprawnienia race.&lt;rasa&gt; — isPermissionSet, bo OP fałszuje hasPermission. */
  public String rasa(Player p) {
    for (String r : c.rej.startery().rasy().keySet()) {
      String n = "race." + r;
      if (p.isPermissionSet(n) && p.hasPermission(n)) return r;
    }
    return null;
  }

  public List<Gatunek> oferta(Player p) {
    List<Gatunek> out = new ArrayList<>();
    for (String id : c.rej.startery().oferta(rasa(p), c.mmocore.klasa(p))) {
      Gatunek g = c.rej.gatunek(id);
      if (g != null) out.add(g);
    }
    return out;
  }

  public void wybierz(Player p, String gatunek) {
    ProfilDane d = c.dane(p);
    if (d == null || d.starter) return;
    Gatunek g = c.rej.gatunek(gatunek);
    if (g == null || oferta(p).stream().noneMatch(x -> x.id().equals(gatunek))) return;
    Osobnik o = Generator.nowy(c.rej, g, c.rej.startery().poziom(), Generator.losujLsniacy(c.ust.szansaLsniacego, ThreadLocalRandom.current()), ThreadLocalRandom.current());
    o.pochodzenie = "STARTER";
    o.zwiazany = true;
    if (druzyny.dodaj(p, d, o, true) == Druzyny.Gdzie.BRAK_MIEJSCA) { c.blad(p, "Brak miejsca w drużynie."); return; }
    poWyborze.accept(p, d);
    d.starter = true;
    d.brudne = true;
    c.dane.zapisz(d);
    c.msg(p, "<gold>" + c.nazwaKolor(o) + " <green>zostaje twoim pierwszym chowańcem!");
    Bukkit.getPluginManager().callEvent(new ChowaniecZlapanyEvent(p, o, "STARTER"));
    c.zdarzenie("STARTER", java.util.Map.of("gracz", p.getName(), "gatunek", g.id()));
  }
}
