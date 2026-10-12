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
import pl.mcmmorpg.chowance.model.Typ;
import pl.mcmmorpg.chowance.model.Stat;

/** Wybór startera 1 z 3 (SPEC §3): oferta zależna od rasy (CharacterCreator) albo klasy MMOCore. */
public final class Startery {
  private final Chowance c;
  private final Okna okna;
  private final Druzyny druzyny;
  public java.util.function.BiConsumer<Player, ProfilDane> poWyborze = (p, d) -> {};

  public Startery(Chowance c, Okna okna, Druzyny druzyny) {
    this.c = c;
    this.okna = okna;
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

  public void otworz(Player p) {
    ProfilDane d = c.daneLubBlad(p);
    if (d == null) return;
    if (d.starter) { c.blad(p, "Ta postać ma już swojego pierwszego chowańca."); return; }
    List<Gatunek> of = oferta(p);
    if (of.isEmpty()) { c.blad(p, "Oferta starterów jest pusta (startery.yml) — zgłoś administracji."); return; }
    List<String> tresc = new ArrayList<>();
    tresc.add("<gray>Wybierz towarzysza. Decyzję potwierdzisz w następnym kroku.");
    List<Okna.Przycisk> pr = new ArrayList<>();
    for (Gatunek g : of) {
      StringBuilder typy = new StringBuilder();
      for (Typ t : g.typy()) typy.append(t.kolor()).append(t.nazwa()).append(" ");
      pr.add(new Okna.Przycisk("<white>" + g.nazwa() + "<gray>·" + typy.toString().trim() + "<gray>·" + krotkaRola(g),
          "Ruchy na poz. " + c.rej.startery().poziom() + ": " + ruchy(g), 110,
          x -> potwierdz(x, g)));
    }
    okna.pokaz(p, "<gold>Pierwszy chowaniec", tresc, pr, Okna.Przycisk.of("<gray>Później", "Wrócisz tu z menu Chowańców (klawisz Q)", x -> {}), 3, true, false);
  }

  private String ruchy(Gatunek g) {
    List<String> nazwy = new ArrayList<>();
    for (String id : g.ruchyDo(c.rej.startery().poziom())) {
      var r = c.rej.ruch(id);
      if (r != null) nazwy.add(r.nazwa());
    }
    return nazwy.isEmpty() ? "brak" : String.join(", ", nazwy);
  }

  private static String rola(Gatunek g) {
    int obrona = g.bazowy(Stat.OBRONA) + g.bazowy(Stat.OBRONA_SPEC);
    int atak = g.bazowy(Stat.ATAK) + g.bazowy(Stat.ATAK_SPEC);
    if (obrona >= atak + 12) return "obrona";
    if (g.bazowy(Stat.ATAK_SPEC) >= g.bazowy(Stat.ATAK) + 12) return "atak magiczny";
    if (g.bazowy(Stat.SZYBKOSC) >= 60 && g.bazowy(Stat.ATAK) > g.bazowy(Stat.ATAK_SPEC)) return "szybki atak";
    if (atak >= obrona + 12) return "atak";
    return "wszechstronny";
  }

  private static String krotkaRola(Gatunek g) {
    return switch (rola(g)) {
      case "atak magiczny" -> "magia";
      case "szybki atak" -> "szybki";
      case "wszechstronny" -> "balans";
      default -> rola(g);
    };
  }

  private void potwierdz(Player p, Gatunek g) {
    okna.pokaz(p, "<gold>Potwierdź wybór", List.of(
        "<white>" + g.nazwa() + " <gray>· " + rola(g),
        "<gray>Ruchy: <white>" + ruchy(g),
        "<red>Tej decyzji nie można cofnąć.</red>"),
        List.of(Okna.Przycisk.of("<green>Wybieram " + g.nazwa(), "Potwierdź wybór", x -> wybierz(x, g.id()))),
        Okna.Przycisk.of("<gray>Wróć do porównania", "", this::otworz), 1, true, false);
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
    c.msg(p, "<gold>" + c.nazwaKolor(o) + " <green>zostaje twoim pierwszym chowańcem! <yellow><click:run_command:'/chw przywolaj 1'>[przywołaj]</click></yellow> <gray>· menu: klawisz Q → Chowańce");
    Bukkit.getPluginManager().callEvent(new ChowaniecZlapanyEvent(p, o, "STARTER"));
    c.zdarzenie("STARTER", java.util.Map.of("gracz", p.getName(), "gatunek", g.id()));
  }
}
