package pl.mcmmorpg.chowance.gra;

import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.NagrodaBestiariusza;

/** Bestiariusz (SPEC §8): liczniki i jednorazowe nagrody wg bestiariusz-nagrody.yml (B). */
public final class Bestiariusz {
  private final Chowance c;

  public Bestiariusz(Chowance c) { this.c = c; }

  public int wszystkie() { return c.rej.bestiariusz().size(); }

  public int widziane(ProfilDane d) {
    int n = 0;
    for (Gatunek g : c.rej.bestiariusz()) if (d.stanBestiariusza(g.id()) >= 1) n++;
    return n;
  }

  public int zlapane(ProfilDane d) {
    int n = 0;
    for (Gatunek g : c.rej.bestiariusz()) if (d.stanBestiariusza(g.id()) >= 2) n++;
    return n;
  }

  public int procent(ProfilDane d) {
    int w = wszystkie();
    return w == 0 ? 0 : (int) Math.floor(zlapane(d) * 100.0 / w);
  }

  public boolean spelniona(ProfilDane d, NagrodaBestiariusza n) {
    List<Gatunek> all = c.rej.bestiariusz();
    if (all.isEmpty()) return false;
    return switch (n.warunek()) {
      case PROCENT_ZLAPANYCH -> procent(d) >= n.wartosc();
      case KOMPLET_TYPU -> {
        boolean jest = false;
        for (Gatunek g : all) {
          if (!g.typy().contains(n.typ())) continue;
          jest = true;
          if (d.stanBestiariusza(g.id()) < 2) yield false;
        }
        yield jest;
      }
      case WSZYSTKIE_LSNIACE -> {
        for (Gatunek g : all) if (!d.lsniace.contains(g.id())) yield false;
        yield true;
      }
    };
  }

  /** Wypłaca każdą spełnioną, jeszcze nieodebraną nagrodę (raz na profil). */
  public void sprawdzNagrody(Player p, ProfilDane d) {
    for (NagrodaBestiariusza n : c.rej.nagrody().values()) {
      if (d.nagrody.contains(n.id()) || !spelniona(d, n)) continue;
      d.nagrody.add(n.id());
      d.brudne = true;
      c.msg(p, "<gold>Bestiariusz: <yellow>" + n.opis() + "</yellow> — nagroda odebrana!");
      c.komendyKonsoli(n.komendy(), Map.of("gracz", p.getName(), "profil", d.profil));
    }
  }
}
