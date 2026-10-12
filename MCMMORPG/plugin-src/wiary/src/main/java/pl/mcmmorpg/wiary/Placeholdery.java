package pl.mcmmorpg.wiary;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * %wiary_...% dla menu 'wiara'. Wartości tak/nie służą do view_requirement w DeluxeMenus.
 * rasa, rasa_nazwa, patron, patron_imie, wiara, laska, laska_czas, modlitwa, zmiana,
 * opis_patrona, dostepny_<id>, wybrany_<id>, opis_<id>, domena_<id>, status_<id>.
 */
final class Placeholdery extends PlaceholderExpansion {
  private final WiaryPlugin plugin;

  Placeholdery(WiaryPlugin plugin) { this.plugin = plugin; }

  @Override public String getIdentifier() { return "wiary"; }
  @Override public String getAuthor() { return "mcmmorpg"; }
  @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
  @Override public boolean persist() { return true; }

  @Override
  public String onRequest(OfflinePlayer op, String param) {
    if (!(op instanceof Player p) || !p.isOnline()) return "";
    Panteon pan = plugin.panteon();
    WiaryPlugin.Stan st = plugin.stan(p);
    String rasa = plugin.rasa(p);
    Magazyn.Dane d = st == null ? null : st.dane;
    long teraz = System.currentTimeMillis();
    Panteon.Bostwo patron = d == null ? null : pan.bostwo(d.patron);

    int podkreslnik = param.indexOf('_');
    if (!param.equals("opis_patrona") && (param.startsWith("dostepny_") || param.startsWith("wybrany_") || param.startsWith("opis_")
        || param.startsWith("domena_") || param.startsWith("status_"))) {
      String rodzaj = param.substring(0, podkreslnik);
      Panteon.Bostwo b = pan.bostwo(param.substring(podkreslnik + 1));
      if (b == null) return "";
      return switch (rodzaj) {
        case "dostepny" -> st != null && pan.dostepne(b, rasa) ? "tak" : "nie";
        case "wybrany" -> patron != null && patron.id().equals(b.id()) ? "tak" : "nie";
        case "opis" -> pan.opisLaski(b, plugin.wiara(p));
        case "status" -> {
          if (patron != null && patron.id().equals(b.id())) yield "&a✔ Twój patron";
          long ms = d == null ? 0 : Panteon.doZmiany(d.patron, d.zmiana, teraz, plugin.odnowienieZmianyMs());
          if (ms > 0) yield "&7Zmiana patrona możliwa za " + Panteon.czas(ms);
          yield patron == null ? "&eKliknij, aby obrać za patrona" : "&eKliknij, aby zmienić patrona &7(zdejmuje łaskę)";
        }
        default -> b.domena();
      };
    }
    return switch (param) {
      case "rasa" -> rasa == null ? "brak" : rasa;
      case "rasa_nazwa" -> rasa == null ? "brak" : pan.rasy().getOrDefault(rasa, rasa);
      case "patron" -> patron == null ? "brak" : patron.id();
      case "patron_imie" -> patron == null ? "&7jeszcze nie wybrano" : patron.kolor() + patron.pelne();
      case "wiara" -> String.valueOf(plugin.wiara(p));
      case "opis_patrona" -> patron == null ? "" : pan.opisLaski(patron, plugin.wiara(p));
      case "laska" -> {
        if (d == null || d.laskaDo <= teraz) yield "&7brak";
        Panteon.Bostwo b = pan.bostwo(d.laskaBostwo);
        yield b == null ? "&7brak" : b.kolor() + b.imie() + "&7: &f" + pan.opisLaski(b, d.laskaWiara);
      }
      case "laska_czas" -> d == null || d.laskaDo <= teraz ? "brak" : Panteon.czas(d.laskaDo - teraz);
      case "modlitwa" -> patron != null && pan.dostepne(patron, rasa) && d.laskaDo <= teraz ? "tak" : "nie";
      case "zmiana" -> {
        if (d == null) yield "teraz";
        long ms = Panteon.doZmiany(d.patron, d.zmiana, teraz, plugin.odnowienieZmianyMs());
        yield ms <= 0 ? "teraz" : "za " + Panteon.czas(ms);
      }
      default -> null;
    };
  }
}
