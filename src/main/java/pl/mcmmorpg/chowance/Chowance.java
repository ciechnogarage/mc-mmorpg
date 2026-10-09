package pl.mcmmorpg.chowance;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.mcmmorpg.chowance.dane.MagazynDanych;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.integracja.MmoCoreHook;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.tresc.Rejestr;

/** Kontekst pluginu: ustawienia, treść, zapis, klucze PDC, komunikaty, zdarzenia konsoli. */
public final class Chowance {
  public static final MiniMessage MM = MiniMessage.miniMessage();
  public static final String PREFIX = "<#c9a0ff>[Chowańce]</#c9a0ff> ";

  public final JavaPlugin plugin;
  public final Logger log;
  public volatile Ustawienia ust;
  public volatile Rejestr rej;
  public final MagazynDanych dane;
  public final MmoCoreHook mmocore;
  /** Czytelne nazwy przedmiotów MMOItems do komunikatów (nigdy surowe ID). */
  public final pl.mcmmorpg.chowance.integracja.NazwyMmo nazwyMmo = new pl.mcmmorpg.chowance.integracja.NazwyMmo();

  public final NamespacedKey kInstancja, kWlasciciel, kDziki, kPieczec, kRzucajacy, kCialoWalki;

  public Chowance(JavaPlugin plugin, Ustawienia ust, Rejestr rej, MagazynDanych dane) {
    this.plugin = plugin;
    this.log = plugin.getLogger();
    this.ust = ust;
    this.rej = rej;
    this.dane = dane;
    this.mmocore = new MmoCoreHook(ust.profesjaId, log);
    kInstancja = new NamespacedKey(plugin, "instancja");
    kWlasciciel = new NamespacedKey(plugin, "wlasciciel");
    kDziki = new NamespacedKey(plugin, "dziki");
    kPieczec = new NamespacedKey(plugin, "pieczec");
    kRzucajacy = new NamespacedKey(plugin, "rzucajacy");
    kCialoWalki = new NamespacedKey(plugin, "cialo_walki");
    if (plugin.getDataFolder().getParentFile() != null) nazwyMmo.wczytaj(plugin.getDataFolder().getParentFile());
  }

  public static Component mm(String s) { return MM.deserialize(s); }

  public void msg(Player p, String miniMessage) { p.sendMessage(mm(PREFIX + miniMessage)); }

  public void blad(Player p, String tekst) { p.sendMessage(mm(PREFIX + "<red>" + MiniMessage.miniMessage().escapeTags(tekst))); }

  public void pasek(Player p, String miniMessage) {
    Component k = mm(miniMessage);
    if (!mmocore.pasek(p, net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(k))) p.sendActionBar(k);
  }

  public ProfilDane dane(Player p) { return dane.dane(p.getUniqueId()); }

  /** Dane profilu albo komunikat „wybierz postać”. */
  public ProfilDane daneLubBlad(Player p) {
    ProfilDane d = dane(p);
    if (d == null) blad(p, "Chowańce wczytują się po wybraniu postaci. Wybierz postać i spróbuj ponownie.");
    return d;
  }

  public Gatunek gatunek(Osobnik o) { return rej.gatunek(o.gatunek); }

  public String nazwa(Osobnik o) {
    if (o.przydomek != null && !o.przydomek.isBlank()) return o.przydomek;
    Gatunek g = gatunek(o);
    return g == null ? o.gatunek : g.nazwa();
  }

  public String nazwaKolor(Osobnik o) {
    String n = MiniMessage.miniMessage().escapeTags(nazwa(o));
    return o.lsniacy ? "<gold>✦ " + n + "</gold>" : "<white>" + n + "</white>";
  }

  public int limitDruzyny(Player p) {
    return Math.min(6, Math.max(1, Ustawienia.prog(ust.limitDruzyny, mmocore.poziom(p))));
  }

  public int limitMagazynu(Player p) {
    return Math.max(0, Ustawienia.prog(ust.limitMagazynu, mmocore.poziom(p)));
  }

  public int limitPoziomu(ProfilDane d) { return ust.limitPoziomu(d.odznaki.size()); }

  /** Komendy konsoli z zdarzenia.yml (B), placeholdery %klucz%. Zawsze na wątku głównym. */
  public void zdarzenie(String nazwa, Map<String, String> ph) {
    List<String> komendy = rej.zdarzenia().get(nazwa);
    if (komendy == null || komendy.isEmpty()) return;
    for (String k : komendy) {
      String c = k;
      for (Map.Entry<String, String> e : ph.entrySet()) c = c.replace("%" + e.getKey() + "%", e.getValue());
      String cmd = c.startsWith("/") ? c.substring(1) : c;
      try {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
      } catch (RuntimeException ex) {
        log.warning("zdarzenia.yml " + nazwa + ": komenda '" + cmd + "' rzuciła " + ex);
      }
    }
  }

  public void komendyKonsoli(List<String> komendy, Map<String, String> ph) {
    for (String k : komendy) {
      String c = k;
      for (Map.Entry<String, String> e : ph.entrySet()) c = c.replace("%" + e.getKey() + "%", e.getValue());
      try {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c.startsWith("/") ? c.substring(1) : c);
      } catch (RuntimeException ex) {
        log.warning("komenda '" + c + "' rzuciła " + ex);
      }
    }
  }

  public void debug(String s) { if (ust.debug) log.info("[debug] " + s); }
}
