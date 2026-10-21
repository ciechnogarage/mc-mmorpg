package pl.mcmmorpg.chowance.gra;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.api.event.ChowaniecZlapanyEvent;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;

/**
 * Bezpieczna wymiana (SPEC §7): oferta po każdej stronie (stwór albo nic), potwierdzenie OBU stron,
 * każda zmiana oferty kasuje potwierdzenia, walidacja PRZED zamianą (własność, związanie, miejsce,
 * odległość), zamiana atomowa na wątku głównym, zapis obu profili, wpis do dane/handel.log.
 */
public final class Handel implements Listener {
  private final Chowance c;
  private final Okna okna;
  private final Druzyny druzyny;
  private final Przywolania przywolania;
  private final Map<UUID, Sesja> wgGracza = new ConcurrentHashMap<>();

  public Handel(Chowance c, Okna okna, Druzyny druzyny, Przywolania przywolania) {
    this.c = c; this.okna = okna; this.druzyny = druzyny; this.przywolania = przywolania;
  }

  final class Sesja {
    final Player[] g = new Player[2];
    final String[] oferta = new String[2];
    final boolean[] ok = new boolean[2];
    boolean koniec;
    int s(Player p) { return g[0].getUniqueId().equals(p.getUniqueId()) ? 0 : 1; }
  }

  public boolean wHandlu(Player p) { return wgGracza.containsKey(p.getUniqueId()); }

  public static boolean wymienialny(Chowance c, Osobnik o) {
    Gatunek g = c.gatunek(o);
    return !o.zwiazany && g != null && g.wymienialny();
  }

  public void start(Player a, Player b) {
    if (c.dane(a) == null || c.dane(b) == null) { c.blad(a, "Obie strony muszą mieć wybraną postać."); return; }
    if (wHandlu(a) || wHandlu(b)) { c.blad(a, "Któraś ze stron już handluje."); return; }
    if (!blisko(a, b)) { c.blad(a, "Stańcie bliżej siebie (" + (int) c.ust.handelZasieg + " bloków)."); return; }
    Sesja s = new Sesja();
    s.g[0] = a; s.g[1] = b;
    wgGracza.put(a.getUniqueId(), s);
    wgGracza.put(b.getUniqueId(), s);
    pokazObu(s);
  }

  private boolean blisko(Player a, Player b) {
    return a.getWorld().equals(b.getWorld()) && a.getLocation().distance(b.getLocation()) <= c.ust.handelZasieg;
  }

  private String opis(Player wl, String id) {
    if (id == null) return "<gray>(nic)";
    ProfilDane d = c.dane(wl);
    Osobnik o = d == null ? null : d.znajdz(id);
    if (o == null) return "<red>(zniknął)";
    Gatunek g = c.gatunek(o);
    return c.nazwaKolor(o) + " <gray>Poz. " + o.poziom + " · " + (g == null ? o.gatunek : g.nazwa()) + " · geny " + o.sumaGenow() + "/186 · więź " + o.wiez;
  }

  private void pokazObu(Sesja s) {
    for (int i = 0; i < 2; i++) pokaz(s, i);
  }

  private void pokaz(Sesja s, int i) {
    Player p = s.g[i], on = s.g[1 - i];
    List<String> t = new ArrayList<>();
    t.add("<green>Ty dajesz:</green> " + opis(p, s.oferta[i]) + (s.ok[i] ? " <green>✔" : ""));
    t.add("<yellow>" + on.getName() + " daje:</yellow> " + opis(on, s.oferta[1 - i]) + (s.ok[1 - i] ? " <green>✔" : ""));
    t.add("<gray>Wymiana nastąpi, gdy OBIE strony zatwierdzą. Każda zmiana oferty zeruje zatwierdzenia.");
    List<Okna.Przycisk> pr = new ArrayList<>();
    pr.add(new Okna.Przycisk("<aqua>Wybierz ofertę", "Który stwór z drużyny/magazynu", 150, x -> wyborOferty(s, i, 0)));
    pr.add(new Okna.Przycisk("<green>Zatwierdź", "", 150, x -> zatwierdz(s, i)));
    okna.pokaz(p, "<gold>Handel z " + on.getName(), t, pr, new Okna.Przycisk("<red>Anuluj", "", 100, x -> anuluj(s, p.getName() + " anulował handel.")), 2, false, false);
  }

  private void wyborOferty(Sesja s, int i, int strona) {
    if (s.koniec) return;
    Player p = s.g[i];
    ProfilDane d = c.dane(p);
    if (d == null) { anuluj(s, "Dane niedostępne."); return; }
    List<Osobnik> wszystkie = new ArrayList<>(d.druzyna);
    wszystkie.addAll(d.magazyn);
    int naStr = 16;
    int od = strona * naStr;
    List<Okna.Przycisk> pr = new ArrayList<>();
    pr.add(new Okna.Przycisk("<gray>(nic)", "Nie oddajesz nikogo", 150, x -> ustawOferte(s, i, null)));
    for (int k = od; k < Math.min(wszystkie.size(), od + naStr); k++) {
      Osobnik o = wszystkie.get(k);
      boolean moz = wymienialny(c, o);
      pr.add(new Okna.Przycisk((moz ? "" : "<red>[związany]</red> ") + c.nazwaKolor(o) + " <gray>" + o.poziom,
          moz ? "Geny " + o.sumaGenow() + "/186" : "Związany — nie do wymiany", 150,
          x -> { if (moz) ustawOferte(s, i, o.id); else wyborOferty(s, i, strona); }));
    }
    if (od + naStr < wszystkie.size()) pr.add(new Okna.Przycisk("<aqua>Dalej »", "", 100, x -> wyborOferty(s, i, strona + 1)));
    if (strona > 0) pr.add(new Okna.Przycisk("<aqua>« Wstecz", "", 100, x -> wyborOferty(s, i, strona - 1)));
    okna.pokaz(p, "<aqua>Twoja oferta", List.of("<gray>Wybierz stwora do wymiany. [związany] = nie do handlu."), pr,
        new Okna.Przycisk("<gray>Wróć", "", 100, x -> pokaz(s, i)), 2, false, false);
  }

  private void ustawOferte(Sesja s, int i, String id) {
    if (s.koniec) return;
    s.oferta[i] = id;
    s.ok[0] = s.ok[1] = false;
    pokazObu(s);
  }

  private void zatwierdz(Sesja s, int i) {
    if (s.koniec) return;
    if (s.oferta[0] == null && s.oferta[1] == null) { c.blad(s.g[i], "Nikt nic nie oferuje."); pokaz(s, i); return; }
    s.ok[i] = true;
    if (s.ok[0] && s.ok[1]) wykonaj(s);
    else pokazObu(s);
  }

  private void wykonaj(Sesja s) {
    Player a = s.g[0], b = s.g[1];
    ProfilDane da = c.dane(a), db = c.dane(b);
    String blad = null;
    if (da == null || db == null || !a.isOnline() || !b.isOnline()) blad = "Któraś strona zniknęła.";
    else if (!blisko(a, b)) blad = "Oddaliliście się od siebie.";
    Osobnik oa = null, ob = null;
    if (blad == null) {
      oa = s.oferta[0] == null ? null : da.znajdz(s.oferta[0]);
      ob = s.oferta[1] == null ? null : db.znajdz(s.oferta[1]);
      if ((s.oferta[0] != null && oa == null) || (s.oferta[1] != null && ob == null)) blad = "Oferowany stwór już nie należy do oferującego.";
      else if ((oa != null && !wymienialny(c, oa)) || (ob != null && !wymienialny(c, ob))) blad = "Związanych stworów nie można wymieniać.";
      else if (ob != null && oa == null && da.wszystkich() >= c.limitDruzyny(a) + c.limitMagazynu(a)) blad = a.getName() + " nie ma miejsca.";
      else if (oa != null && ob == null && db.wszystkich() >= c.limitDruzyny(b) + c.limitMagazynu(b)) blad = b.getName() + " nie ma miejsca.";
    }
    if (blad != null) { anuluj(s, blad); return; }
    // odwołanie przywołanych, zdjęcie z list — potem dodanie; wszystko w jednym ticku
    if (oa != null) { odwolajJesli(a, oa); da.druzyna.remove(oa); da.magazyn.remove(oa); }
    if (ob != null) { odwolajJesli(b, ob); db.druzyna.remove(ob); db.magazyn.remove(ob); }
    if (oa != null) { oa.wiez = Math.min(oa.wiez, 70); druzyny.dodaj(b, db, oa, true); }
    if (ob != null) { ob.wiez = Math.min(ob.wiez, 70); druzyny.dodaj(a, da, ob, true); }
    s.koniec = true;
    wgGracza.remove(a.getUniqueId());
    wgGracza.remove(b.getUniqueId());
    c.dane.zapisz(da);
    c.dane.zapisz(db);
    for (Player p : s.g) { okna.zamknij(p); p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1.1f); }
    c.msg(a, "<green>Wymiana zakończona. Otrzymujesz: " + (ob == null ? "nic" : c.nazwaKolor(ob)));
    c.msg(b, "<green>Wymiana zakończona. Otrzymujesz: " + (oa == null ? "nic" : c.nazwaKolor(oa)));
    if (ob != null) org.bukkit.Bukkit.getPluginManager().callEvent(new ChowaniecZlapanyEvent(a, ob, "HANDEL"));
    if (oa != null) org.bukkit.Bukkit.getPluginManager().callEvent(new ChowaniecZlapanyEvent(b, oa, "HANDEL"));
    String wpis = Instant.now() + " " + a.getName() + "(" + da.profil + ") -> " + (oa == null ? "-" : oa.gatunek + "/" + oa.id)
        + " | " + b.getName() + "(" + db.profil + ") -> " + (ob == null ? "-" : ob.gatunek + "/" + ob.id) + "\n";
    c.dane.wykonajIO(() -> {
      try {
        Files.writeString(c.dane.katalog().resolve("handel.log"), wpis, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (Exception e) {
        c.log.warning("Nie zapisano handel.log: " + e);
      }
    });
  }

  private void odwolajJesli(Player p, Osobnik o) {
    var a = przywolania.aktywny(p);
    if (a != null && a.osobnikId.equals(o.id)) przywolania.odwolaj(p, true);
  }

  private void anuluj(Sesja s, String powod) {
    if (s.koniec) return;
    s.koniec = true;
    for (Player p : s.g) {
      if (p == null) continue;
      wgGracza.remove(p.getUniqueId());
      if (p.isOnline()) { okna.zamknij(p); c.msg(p, "<red>Handel przerwany: <gray>" + powod); }
    }
  }

  public void przerwij(Player p) {
    Sesja s = wgGracza.get(p.getUniqueId());
    if (s != null) anuluj(s, p.getName() + " opuścił handel.");
  }

  public void przerwijWszystkie() {
    for (Sesja s : List.copyOf(new java.util.HashSet<>(wgGracza.values()))) anuluj(s, "restart pluginu");
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) { przerwij(e.getPlayer()); }
}
