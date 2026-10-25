package pl.mcmmorpg.chowance.tresc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.mcmmorpg.chowance.model.Cecha;
import pl.mcmmorpg.chowance.model.Charakter;
import pl.mcmmorpg.chowance.model.EfektRuchu;
import pl.mcmmorpg.chowance.model.Ewolucja;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Kategoria;
import pl.mcmmorpg.chowance.model.KrzywaExp;
import pl.mcmmorpg.chowance.model.NagrodaBestiariusza;
import pl.mcmmorpg.chowance.model.Przedmioty;
import pl.mcmmorpg.chowance.model.RodzajCechy;
import pl.mcmmorpg.chowance.model.Ruch;
import pl.mcmmorpg.chowance.model.Rzadkosc;
import pl.mcmmorpg.chowance.model.Stat;
import pl.mcmmorpg.chowance.model.StatEtapu;
import pl.mcmmorpg.chowance.model.Startery;
import pl.mcmmorpg.chowance.model.Status;
import pl.mcmmorpg.chowance.model.TabelaTypow;
import pl.mcmmorpg.chowance.model.Trener;
import pl.mcmmorpg.chowance.model.Typ;
import pl.mcmmorpg.chowance.model.Wieza;

/**
 * Wczytuje całą treść z katalogu pluginu i waliduje ją (KONTRAKT). Nie rzuca wyjątków na złe dane:
 * wadliwy wpis jest pomijany i trafia do {@link Problemy} z plikiem i kluczem.
 */
public final class LadowaczTresci {
  private static final Pattern ID = Pattern.compile("[A-Z0-9_]+");
  private static final Map<Rzadkosc, Integer> DOMYSLNY_WSP = Map.of(
      Rzadkosc.POSPOLITY, 190, Rzadkosc.NIEPOSPOLITY, 120, Rzadkosc.RZADKI, 60,
      Rzadkosc.EPICKI, 25, Rzadkosc.LEGENDARNY, 3);

  private static final Set<String> KLUCZE_GATUNKU = Set.of("nazwa", "opis", "typy", "rzadkosc",
      "wspolczynnik_zlapania", "stadium", "linia", "staty_bazowe", "krzywa_exp", "exp_bazowy",
      "wiez_startowa", "cechy", "ruchy", "ewolucje", "hodowla", "lapanie", "legendarny", "wymienialny",
      "wierzchowiec", "mob", "ikona", "ikona_lsniaca", "wystepowanie", "testowy");
  private static final Set<String> KLUCZE_RUCHU = Set.of("nazwa", "opis", "typ", "kategoria", "moc",
      "celnosc", "priorytet", "odnowienie", "kontakt", "efekty", "skill");
  private static final String[] STATY_YAML = {"zdrowie", "atak", "obrona", "atak_spec", "obrona_spec", "szybkosc"};

  private final File dir;
  private final Problemy problemy = new Problemy();

  public LadowaczTresci(File dir) { this.dir = dir; }

  public Problemy problemy() { return problemy; }

  public Rejestr wczytaj() {
    TabelaTypow typy = wczytajTypy();
    Map<String, Charakter> charaktery = wczytajCharaktery();
    Map<String, Cecha> cechy = new LinkedHashMap<>();
    for (File f : pliki("cechy")) wczytajCechy(f, cechy);
    Map<String, Ruch> ruchy = new LinkedHashMap<>();
    for (File f : pliki("ruchy")) wczytajRuchy(f, ruchy);
    Przedmioty przedmioty = wczytajPrzedmioty();
    Map<String, Gatunek> gatunki = new LinkedHashMap<>();
    for (File f : pliki("gatunki")) wczytajGatunki(f, gatunki);
    walidujGatunki(gatunki, ruchy, cechy, przedmioty);
    Startery startery = wczytajStartery(gatunki);
    Map<String, Trener> trenerzy = new LinkedHashMap<>();
    for (File f : pliki("trenerzy")) wczytajTrenerow(f, trenerzy, gatunki, ruchy, cechy, charaktery);
    Map<String, Wieza> wieze = wczytajWieze(trenerzy);
    Map<String, NagrodaBestiariusza> nagrody = wczytajNagrody();
    Map<String, List<String>> zdarzenia = wczytajZdarzenia();

    List<Gatunek> best = new ArrayList<>();
    for (Gatunek g : gatunki.values()) if (!g.testowy()) best.add(g);
    best.sort(Comparator.comparing(Gatunek::linia).thenComparingInt(Gatunek::stadium).thenComparing(Gatunek::id));
    Map<String, String[]> poMobie = new HashMap<>();
    for (Gatunek g : gatunki.values()) {
      mob(poMobie, g.mobDziki(), g.id(), "D");
      mob(poMobie, g.mobDzikiLsniacy(), g.id(), "DL");
      mob(poMobie, g.mobOswojony(), g.id(), "O");
      mob(poMobie, g.mobOswojonyLsniacy(), g.id(), "OL");
    }
    return new Rejestr(typy, Map.copyOf(charaktery), Map.copyOf(gatunki), Map.copyOf(ruchy), Map.copyOf(cechy),
        przedmioty, startery, Map.copyOf(trenerzy), Map.copyOf(wieze), Map.copyOf(nagrody), Map.copyOf(zdarzenia),
        List.copyOf(best), Map.copyOf(poMobie));
  }

  private void mob(Map<String, String[]> m, String mob, String gatunek, String rodzaj) {
    if (mob == null || mob.isBlank()) return;
    String[] prev = m.put(mob, new String[] {gatunek, rodzaj});
    if (prev != null && !prev[0].equals(gatunek)) {
      problemy.blad("gatunki", gatunek + ".mob", "mob " + mob + " jest już ciałem gatunku " + prev[0]);
    }
  }

  // ------------------------------------------------------------------ pliki

  private List<File> pliki(String podkatalog) {
    File d = new File(dir, podkatalog);
    File[] fs = d.listFiles((x, n) -> n.endsWith(".yml"));
    if (fs == null) return List.of();
    Arrays.sort(fs);
    return List.of(fs);
  }

  private YamlConfiguration yaml(File f) {
    YamlConfiguration y = new YamlConfiguration();
    try {
      y.load(f);
    } catch (Exception e) {
      problemy.blad(rel(f), "(plik)", "nie da się sparsować YAML: " + e.getMessage().lines().findFirst().orElse(""));
      return null;
    }
    return y;
  }

  private String rel(File f) {
    String p = f.getPath();
    String base = dir.getPath();
    return p.startsWith(base) ? p.substring(base.length() + 1) : p;
  }

  private void nieznaneKlucze(String plik, String id, ConfigurationSection s, Set<String> znane) {
    for (String k : s.getKeys(false)) {
      if (!znane.contains(k)) problemy.uwaga(plik, id + "." + k, "nieznany klucz — ignorowany (literówka?)");
    }
  }

  private boolean poprawneId(String plik, String id) {
    if (!ID.matcher(id).matches()) {
      problemy.blad(plik, id, "ID musi pasować do [A-Z0-9_]+");
      return false;
    }
    return true;
  }

  // ------------------------------------------------------------------ typy, charaktery

  private TabelaTypow wczytajTypy() {
    TabelaTypow t = new TabelaTypow();
    File f = new File(dir, "typy.yml");
    if (!f.isFile()) {
      problemy.blad("typy.yml", "(plik)", "brak pliku — wszystkie mnożniki = 1.0");
      return t;
    }
    YamlConfiguration y = yaml(f);
    if (y == null) return t;
    double stab = y.getDouble("stab", 1.5);
    if (stab < 1 || stab > 2) problemy.blad("typy.yml", "stab", "1.0..2.0"); else t.ustawStab(stab);
    ConfigurationSection s = y.getConfigurationSection("skutecznosc");
    if (s == null) {
      problemy.blad("typy.yml", "skutecznosc", "brak sekcji");
      return t;
    }
    for (String a : s.getKeys(false)) {
      Typ ta = Typ.parse(a);
      if (ta == null) { problemy.blad("typy.yml", "skutecznosc." + a, "nieznany typ"); continue; }
      ConfigurationSection w = s.getConfigurationSection(a);
      if (w == null) continue;
      for (String d : w.getKeys(false)) {
        Typ td = Typ.parse(d);
        if (td == null) { problemy.blad("typy.yml", "skutecznosc." + a + "." + d, "nieznany typ"); continue; }
        double m = w.getDouble(d, 1.0);
        if (m < 0 || m > 4) problemy.blad("typy.yml", "skutecznosc." + a + "." + d, "mnożnik poza 0..4");
        else t.ustaw(ta, td, m);
      }
    }
    return t;
  }

  private Map<String, Charakter> wczytajCharaktery() {
    Map<String, Charakter> out = new LinkedHashMap<>();
    File f = new File(dir, "charaktery.yml");
    YamlConfiguration y = f.isFile() ? yaml(f) : null;
    if (y == null) {
      problemy.blad("charaktery.yml", "(plik)", "brak pliku — jeden neutralny charakter");
      out.put("SPOKOJNY", new Charakter("SPOKOJNY", "Spokojny", null, null));
      return out;
    }
    for (String id : y.getKeys(false)) {
      if (!poprawneId("charaktery.yml", id)) continue;
      ConfigurationSection s = y.getConfigurationSection(id);
      if (s == null) continue;
      String pl = s.getString("plus", "");
      String mi = s.getString("minus", "");
      Stat plus = pl.isEmpty() ? null : Stat.parse(pl);
      Stat minus = mi.isEmpty() ? null : Stat.parse(mi);
      if ((!pl.isEmpty() && (plus == null || plus == Stat.ZDROWIE)) || (!mi.isEmpty() && (minus == null || minus == Stat.ZDROWIE))) {
        problemy.blad("charaktery.yml", id, "plus/minus musi być statem innym niż ZDROWIE");
        continue;
      }
      out.put(id, new Charakter(id, s.getString("nazwa", id), plus, minus));
    }
    if (out.isEmpty()) out.put("SPOKOJNY", new Charakter("SPOKOJNY", "Spokojny", null, null));
    return out;
  }

  // ------------------------------------------------------------------ cechy

  private void wczytajCechy(File f, Map<String, Cecha> out) {
    YamlConfiguration y = yaml(f);
    if (y == null) return;
    String plik = rel(f);
    for (String id : y.getKeys(false)) {
      if (!poprawneId(plik, id)) continue;
      ConfigurationSection s = y.getConfigurationSection(id);
      if (s == null) { problemy.blad(plik, id, "oczekiwano sekcji"); continue; }
      nieznaneKlucze(plik, id, s, Set.of("nazwa", "opis", "rodzaj", "parametry"));
      RodzajCechy r = RodzajCechy.parse(s.getString("rodzaj"));
      if (r == null) { problemy.blad(plik, id + ".rodzaj", "nieznany rodzaj '" + s.getString("rodzaj") + "' (lista: KONTRAKT §4)"); continue; }
      Map<String, String> par = new LinkedHashMap<>();
      ConfigurationSection ps = s.getConfigurationSection("parametry");
      if (ps != null) for (String k : ps.getKeys(false)) {
        Object v = ps.get(k);
        par.put(k, v instanceof List<?> l ? String.join(",", l.stream().map(String::valueOf).toList()) : String.valueOf(v));
      }
      boolean ok = true;
      for (String wym : r.wymaganeParametry()) {
        if (!par.containsKey(wym)) { problemy.blad(plik, id + ".parametry." + wym, "brak wymaganego parametru"); ok = false; }
      }
      Cecha c = new Cecha(id, s.getString("nazwa", id), s.getString("opis", ""), r, Map.copyOf(par));
      if (par.containsKey("typ") && c.typ() == null) { problemy.blad(plik, id + ".parametry.typ", "nieznany typ"); ok = false; }
      if (par.containsKey("status") && c.status() == null) { problemy.blad(plik, id + ".parametry.status", "nieznany status"); ok = false; }
      if (par.containsKey("stat") && c.stat() == null) { problemy.blad(plik, id + ".parametry.stat", "nieznany stat"); ok = false; }
      if (!c.gdy().isEmpty()) {
        if (r != RodzajCechy.MNOZNIK_STATU && r != RodzajCechy.WZMOCNIENIE_TYPU) { problemy.blad(plik, id + ".parametry.gdy", "warunek środowiska tylko dla MNOZNIK_STATU i WZMOCNIENIE_TYPU"); ok = false; }
        for (String g : c.gdy()) if (!Cecha.SRODOWISKA.contains(g)) { problemy.blad(plik, id + ".parametry.gdy", "nieznane '" + g + "' (NOC|DZIEN|DESZCZ|BURZA|W_WODZIE)"); ok = false; }
        if (c.stat() == pl.mcmmorpg.chowance.model.Stat.ZDROWIE) { problemy.blad(plik, id + ".parametry.gdy", "ZDROWIE nie może zależeć od środowiska"); ok = false; }
      }
      for (String k : par.keySet()) if (!r.wymaganeParametry().contains(k) && !k.equals("gdy")) problemy.uwaga(plik, id + ".parametry." + k, "parametr nieużywany przez rodzaj " + r);
      if (!ok) continue;
      if (out.put(id, c) != null) problemy.blad(plik, id, "duplikat ID cechy");
    }
  }

  // ------------------------------------------------------------------ ruchy

  private void wczytajRuchy(File f, Map<String, Ruch> out) {
    YamlConfiguration y = yaml(f);
    if (y == null) return;
    String plik = rel(f);
    for (String id : y.getKeys(false)) {
      if (!poprawneId(plik, id)) continue;
      ConfigurationSection s = y.getConfigurationSection(id);
      if (s == null) { problemy.blad(plik, id, "oczekiwano sekcji"); continue; }
      nieznaneKlucze(plik, id, s, KLUCZE_RUCHU);
      Typ typ = Typ.parse(s.getString("typ"));
      Kategoria kat = Kategoria.parse(s.getString("kategoria"));
      if (typ == null) { problemy.blad(plik, id + ".typ", "nieznany typ"); continue; }
      if (kat == null) { problemy.blad(plik, id + ".kategoria", "FIZYCZNY|SPECJALNY|STATUS"); continue; }
      int moc = s.getInt("moc", 0);
      if (kat != Kategoria.STATUS && moc <= 0) { problemy.blad(plik, id + ".moc", "ruch zadający obrażenia musi mieć moc > 0"); continue; }
      int cel = s.getInt("celnosc", 100);
      if (cel < 0 || cel > 100) { problemy.blad(plik, id + ".celnosc", "0..100"); continue; }
      List<EfektRuchu> ef = new ArrayList<>();
      boolean ok = true;
      List<Map<?, ?>> lista = s.getMapList("efekty");
      for (int i = 0; i < lista.size(); i++) {
        EfektRuchu e = efekt(plik, id + ".efekty[" + i + "]", lista.get(i));
        if (e == null) ok = false; else ef.add(e);
      }
      if (!ok) continue;
      Ruch r = new Ruch(id, s.getString("nazwa", id), s.getString("opis", ""), typ, kat, moc, cel,
          Math.max(-3, Math.min(3, s.getInt("priorytet", 0))), Math.max(0.5, s.getDouble("odnowienie", 3.0)),
          s.getBoolean("kontakt", false), List.copyOf(ef), s.getString("skill", ""));
      if (out.put(id, r) != null) problemy.blad(plik, id, "duplikat ID ruchu");
    }
  }

  private EfektRuchu efekt(String plik, String klucz, Map<?, ?> m) {
    EfektRuchu.Rodzaj r = EfektRuchu.Rodzaj.parse(str(m.get("rodzaj")));
    if (r == null) { problemy.blad(plik, klucz + ".rodzaj", "STATUS|STAT|LECZENIE|WYSYSANIE|ODRZUT"); return null; }
    String celS = str(m.get("cel"));
    boolean naSiebie = celS == null ? (r == EfektRuchu.Rodzaj.LECZENIE) : celS.equalsIgnoreCase("SIEBIE");
    if (celS != null && !celS.equalsIgnoreCase("SIEBIE") && !celS.equalsIgnoreCase("PRZECIWNIK")) {
      problemy.blad(plik, klucz + ".cel", "SIEBIE|PRZECIWNIK"); return null;
    }
    int szansa = num(m.get("szansa"), 100);
    int czas = num(m.get("czas"), 3);
    int procent = num(m.get("procent"), 0);
    switch (r) {
      case STATUS -> {
        Status st = Status.parse(str(m.get("status")));
        if (st == null) { problemy.blad(plik, klucz + ".status", "nieznany status"); return null; }
        return new EfektRuchu(r, st, null, 0, szansa, naSiebie, czas, 0);
      }
      case STAT -> {
        StatEtapu st = StatEtapu.parse(str(m.get("stat")));
        int zm = num(m.get("zmiana"), 0);
        if (st == null || zm == 0 || Math.abs(zm) > 6) { problemy.blad(plik, klucz, "STAT wymaga stat i zmiana -6..6 (≠0)"); return null; }
        return new EfektRuchu(r, null, st, zm, szansa, naSiebie, 0, 0);
      }
      default -> {
        if (procent <= 0 || procent > 100) { problemy.blad(plik, klucz + ".procent", "1..100"); return null; }
        return new EfektRuchu(r, null, null, 0, szansa, r == EfektRuchu.Rodzaj.LECZENIE ? naSiebie : true, 0, procent);
      }
    }
  }

  private static String str(Object o) { return o == null ? null : String.valueOf(o); }

  private static int num(Object o, int def) {
    if (o instanceof Number n) return n.intValue();
    try { return o == null ? def : Integer.parseInt(String.valueOf(o).trim()); } catch (NumberFormatException e) { return def; }
  }

  // ------------------------------------------------------------------ gatunki

  private void wczytajGatunki(File f, Map<String, Gatunek> out) {
    YamlConfiguration y = yaml(f);
    if (y == null) return;
    String plik = rel(f);
    for (String id : y.getKeys(false)) {
      if (!poprawneId(plik, id)) continue;
      ConfigurationSection s = y.getConfigurationSection(id);
      if (s == null) { problemy.blad(plik, id, "oczekiwano sekcji"); continue; }
      nieznaneKlucze(plik, id, s, KLUCZE_GATUNKU);
      Gatunek g = gatunek(plik, id, s);
      if (g == null) continue;
      if (out.put(id, g) != null) problemy.blad(plik, id, "duplikat ID gatunku");
    }
  }

  private Gatunek gatunek(String plik, String id, ConfigurationSection s) {
    boolean ok = true;
    List<Typ> typy = new ArrayList<>();
    for (String t : s.getStringList("typy")) {
      Typ tt = Typ.parse(t);
      if (tt == null) { problemy.blad(plik, id + ".typy", "nieznany typ " + t); ok = false; } else if (!typy.contains(tt)) typy.add(tt);
    }
    if (typy.isEmpty() || typy.size() > 2) { problemy.blad(plik, id + ".typy", "1–2 typy"); ok = false; }
    Rzadkosc rz = Rzadkosc.parse(s.getString("rzadkosc", "POSPOLITY"));
    if (rz == null) { problemy.blad(plik, id + ".rzadkosc", "nieznana rzadkość"); ok = false; rz = Rzadkosc.POSPOLITY; }
    int wsp = s.getInt("wspolczynnik_zlapania", DOMYSLNY_WSP.get(rz));
    if (wsp < 1 || wsp > 255) { problemy.blad(plik, id + ".wspolczynnik_zlapania", "1..255"); ok = false; }
    int[] staty = new int[6];
    ConfigurationSection st = s.getConfigurationSection("staty_bazowe");
    if (st == null) { problemy.blad(plik, id + ".staty_bazowe", "brak sekcji"); ok = false; }
    else {
      for (int i = 0; i < 6; i++) {
        staty[i] = st.getInt(STATY_YAML[i], -1);
        if (staty[i] < 1 || staty[i] > 255) { problemy.blad(plik, id + ".staty_bazowe." + STATY_YAML[i], "1..255"); ok = false; }
      }
    }
    KrzywaExp krzywa = KrzywaExp.parse(s.getString("krzywa_exp", "SREDNIA"));
    if (krzywa == null) { problemy.blad(plik, id + ".krzywa_exp", "SZYBKA|SREDNIA|WOLNA"); ok = false; }
    TreeMap<Integer, List<String>> ruchy = new TreeMap<>();
    ConfigurationSection rs = s.getConfigurationSection("ruchy");
    if (rs == null) { problemy.blad(plik, id + ".ruchy", "brak puli ruchów"); ok = false; }
    else {
      for (String k : rs.getKeys(false)) {
        int lvl;
        try { lvl = Integer.parseInt(k); } catch (NumberFormatException e) { problemy.blad(plik, id + ".ruchy." + k, "klucz musi być poziomem"); ok = false; continue; }
        List<String> l = rs.isList(k) ? rs.getStringList(k) : List.of(rs.getString(k, ""));
        ruchy.put(Math.max(1, lvl), List.copyOf(l));
      }
      if (ruchy.isEmpty() || ruchy.firstKey() > 1) { problemy.blad(plik, id + ".ruchy", "musi być co najmniej jeden ruch na poziomie 1"); ok = false; }
    }
    List<Ewolucja> ewo = new ArrayList<>();
    for (Map<?, ?> m : s.getMapList("ewolucje")) {
      String doG = str(m.get("do"));
      if (doG == null) { problemy.blad(plik, id + ".ewolucje", "brak 'do'"); ok = false; continue; }
      ewo.add(new Ewolucja(doG, num(m.get("poziom"), 1), str(m.get("przedmiot")), num(m.get("wiez"), 0)));
    }
    ConfigurationSection hod = s.getConfigurationSection("hodowla");
    List<String> grupy = hod == null ? List.of("NIEPLODNY") : hod.getStringList("grupy");
    int dystans = hod == null ? 1500 : Math.max(50, hod.getInt("dystans", 1500));
    ConfigurationSection lap = s.getConfigurationSection("lapanie");
    boolean pieczec = lap == null || lap.getBoolean("pieczec", true);
    boolean osw = lap != null && lap.getBoolean("oswajanie", false);
    Map<String, Integer> przysmaki = new LinkedHashMap<>();
    if (lap != null && lap.getConfigurationSection("przysmaki") != null) {
      ConfigurationSection ps = lap.getConfigurationSection("przysmaki");
      for (String k : ps.getKeys(false)) {
        int pkt = ps.getInt(k, 0);
        if (pkt < 1 || pkt > 100) { problemy.blad(plik, id + ".lapanie.przysmaki." + k, "1..100"); ok = false; }
        przysmaki.put(k, pkt);
      }
    }
    if (osw && przysmaki.isEmpty()) { problemy.blad(plik, id + ".lapanie", "oswajanie: true wymaga przysmaków"); ok = false; }
    if (!pieczec && !osw) problemy.uwaga(plik, id + ".lapanie", "gatunek nie jest łapalny ani pieczęcią, ani oswajaniem");
    ConfigurationSection mob = s.getConfigurationSection("mob");
    String mo = mob == null ? "" : mob.getString("oswojony", "");
    String mol = mob == null ? "" : mob.getString("oswojony_lsniacy", "");
    String md = mob == null ? "" : mob.getString("dziki", "");
    String mdl = mob == null ? "" : mob.getString("dziki_lsniacy", "");
    String mz = mob == null ? "" : mob.getString("zastepczy", "");
    if (mo.isBlank() && mz.isBlank()) { problemy.blad(plik, id + ".mob", "brak mob.oswojony i mob.zastepczy"); ok = false; }
    List<String> cechy = s.getStringList("cechy");
    if (cechy.isEmpty() || cechy.size() > 3) { problemy.blad(plik, id + ".cechy", "1–3 cechy"); ok = false; }
    int stadium = s.getInt("stadium", 1);
    if (stadium < 1 || stadium > 3) { problemy.blad(plik, id + ".stadium", "1..3"); ok = false; }
    boolean legend = s.getBoolean("legendarny", false);
    if (!ok) return null;
    return new Gatunek(id, s.getString("nazwa", id), List.copyOf(s.getStringList("opis")), List.copyOf(typy), rz, wsp,
        stadium, s.getString("linia", id), staty, krzywa, Math.max(1, s.getInt("exp_bazowy", 60)),
        Math.max(0, Math.min(255, s.getInt("wiez_startowa", 70))), List.copyOf(cechy), ruchy, List.copyOf(ewo),
        List.copyOf(grupy), dystans, pieczec, osw, Map.copyOf(przysmaki), s.getString("lapanie.wymagana_pieczec", ""),
        legend, s.getBoolean("wymienialny", true) && !legend, s.getBoolean("wierzchowiec", false), mo, mol, md, mdl, mz,
        s.getString("ikona", ""), s.getString("ikona_lsniaca", ""), List.copyOf(s.getStringList("wystepowanie")),
        s.getBoolean("testowy", false));
  }

  /** Referencje między plikami. Gatunek z martwą referencją wypada (żeby nie wywalić walki w runtime). */
  private void walidujGatunki(Map<String, Gatunek> g, Map<String, Ruch> ruchy, Map<String, Cecha> cechy, Przedmioty p) {
    boolean zmiana = true;
    while (zmiana) {
      zmiana = false;
      for (Gatunek x : List.copyOf(g.values())) {
        String k = "gatunki: " + x.id();
        List<String> bledy = new ArrayList<>();
        for (List<String> l : x.ruchy().values()) for (String r : l) if (!ruchy.containsKey(r)) bledy.add("ruch " + r + " nie istnieje");
        for (String c : x.cechy()) if (!cechy.containsKey(c)) bledy.add("cecha " + c + " nie istnieje");
        for (Ewolucja e : x.ewolucje()) {
          if (!g.containsKey(e.doGatunku())) bledy.add("ewolucja do " + e.doGatunku() + " — gatunek nie istnieje");
          if (e.wymagaPrzedmiotu() && !p.znaKamien(e.przedmiot())) problemy.uwaga("gatunki", x.id() + ".ewolucje",
              "przedmiot " + e.przedmiot() + " nie jest na liście przedmioty.yml/kamienie");
        }
        if (!g.containsKey(x.linia())) bledy.add("linia " + x.linia() + " — gatunek nie istnieje");
        if (!x.wymaganaPieczec().isBlank() && !p.pieczecie().containsKey(x.wymaganaPieczec()))
          bledy.add("wymagana_pieczec " + x.wymaganaPieczec() + " nie ma w przedmioty.yml/pieczecie");
        if (!bledy.isEmpty()) {
          for (String b : bledy) problemy.blad(k, "", b + " — gatunek pominięty");
          g.remove(x.id());
          zmiana = true;
        }
      }
    }
  }

  // ------------------------------------------------------------------ przedmioty

  private Przedmioty wczytajPrzedmioty() {
    File f = new File(dir, "przedmioty.yml");
    if (!f.isFile()) { problemy.uwaga("przedmioty.yml", "(plik)", "brak pliku — brak pieczęci/karmy"); return Przedmioty.puste(); }
    YamlConfiguration y = yaml(f);
    if (y == null) return Przedmioty.puste();
    Map<String, Przedmioty.Pieczec> piecz = new LinkedHashMap<>();
    ConfigurationSection ps = y.getConfigurationSection("pieczecie");
    if (ps != null) for (String id : ps.getKeys(false)) {
      ConfigurationSection s = ps.getConfigurationSection(id);
      if (s == null) continue;
      Przedmioty.RodzajPieczeci r = Przedmioty.RodzajPieczeci.parse(s.getString("rodzaj", "ZWYKLA"));
      if (r == null) { problemy.blad("przedmioty.yml", "pieczecie." + id + ".rodzaj", "nieznany rodzaj"); continue; }
      piecz.put(id, new Przedmioty.Pieczec(id, s.getString("typ_mmoitems", "MISCELLANEOUS").toUpperCase(Locale.ROOT), r,
          s.getDouble("mnoznik", 1.0), s.getDouble("mnoznik_warunku", 1.0), s.getString("model_mob", "")));
    }
    Set<String> kamienie = new LinkedHashSet<>(y.getStringList("kamienie"));
    Map<String, Przedmioty.Karma> karma = new LinkedHashMap<>();
    ConfigurationSection ks = y.getConfigurationSection("karma");
    if (ks != null) for (String id : ks.getKeys(false)) {
      ConfigurationSection s = ks.getConfigurationSection(id);
      if (s == null) continue;
      karma.put(id, new Przedmioty.Karma(id, s.getString("typ_mmoitems", "MATERIAL").toUpperCase(Locale.ROOT),
          s.getInt("exp", 0), s.getInt("wiez", 0)));
    }
    Map<String, Przedmioty.Eliksir> el = new LinkedHashMap<>();
    ConfigurationSection es = y.getConfigurationSection("eliksiry");
    if (es != null) for (String id : es.getKeys(false)) {
      ConfigurationSection s = es.getConfigurationSection(id);
      if (s == null) continue;
      el.put(id, new Przedmioty.Eliksir(id, s.getString("typ_mmoitems", "MISCELLANEOUS").toUpperCase(Locale.ROOT),
          s.getDouble("leczenie_procent", 0), s.getBoolean("wskrzesza", false), s.getBoolean("leczy_status", false)));
    }
    Map<String, String> siodla = new LinkedHashMap<>();
    ConfigurationSection ss = y.getConfigurationSection("siodla");
    if (ss != null) for (String id : ss.getKeys(false)) {
      siodla.put(id, ss.getString(id + ".typ_mmoitems", "MISCELLANEOUS").toUpperCase(Locale.ROOT));
    }
    return new Przedmioty(Map.copyOf(piecz), Set.copyOf(kamienie), Map.copyOf(karma), Map.copyOf(el),
        Map.copyOf(siodla), y.getString("jajo_ikona", "JAJO_CHOWANCA"));
  }

  // ------------------------------------------------------------------ startery, trenerzy, wieża, nagrody, zdarzenia

  private Startery wczytajStartery(Map<String, Gatunek> g) {
    File f = new File(dir, "startery.yml");
    YamlConfiguration y = f.isFile() ? yaml(f) : null;
    if (y == null) return Startery.puste();
    List<String> dom = sprawdzStartery("domyslne", y.getStringList("domyslne"), g);
    Map<String, List<String>> rasy = new LinkedHashMap<>();
    ConfigurationSection rs = y.getConfigurationSection("rasy");
    if (rs != null) for (String k : rs.getKeys(false)) rasy.put(k.toLowerCase(Locale.ROOT), sprawdzStartery("rasy." + k, rs.getStringList(k), g));
    Map<String, List<String>> klasy = new LinkedHashMap<>();
    ConfigurationSection ks = y.getConfigurationSection("klasy");
    if (ks != null) for (String k : ks.getKeys(false)) klasy.put(k.toLowerCase(Locale.ROOT), sprawdzStartery("klasy." + k, ks.getStringList(k), g));
    return new Startery(Math.max(1, y.getInt("poziom", 5)), dom, Map.copyOf(rasy), Map.copyOf(klasy));
  }

  private List<String> sprawdzStartery(String klucz, List<String> l, Map<String, Gatunek> g) {
    List<String> out = new ArrayList<>();
    for (String s : l) {
      if (!g.containsKey(s)) problemy.blad("startery.yml", klucz, "gatunek " + s + " nie istnieje");
      else out.add(s);
    }
    if (!l.isEmpty() && l.size() != 3) problemy.uwaga("startery.yml", klucz, "powinny być dokładnie 3 startery (jest " + l.size() + ")");
    return List.copyOf(out);
  }

  private void wczytajTrenerow(File f, Map<String, Trener> out, Map<String, Gatunek> g, Map<String, Ruch> ruchy,
      Map<String, Cecha> cechy, Map<String, Charakter> ch) {
    YamlConfiguration y = yaml(f);
    if (y == null) return;
    String plik = rel(f);
    for (String id : y.getKeys(false)) {
      if (!poprawneId(plik, id)) continue;
      ConfigurationSection s = y.getConfigurationSection(id);
      if (s == null) continue;
      Trener.Rodzaj rodzaj;
      try { rodzaj = Trener.Rodzaj.valueOf(s.getString("rodzaj", "TRENER").toUpperCase(Locale.ROOT)); }
      catch (IllegalArgumentException e) { problemy.blad(plik, id + ".rodzaj", "MISTRZ|TRENER"); continue; }
      List<Trener.Czlonek> dr = new ArrayList<>();
      boolean ok = true;
      List<Map<?, ?>> lista = s.getMapList("druzyna");
      for (int i = 0; i < lista.size(); i++) {
        Map<?, ?> m = lista.get(i);
        String gat = str(m.get("gatunek"));
        String k = id + ".druzyna[" + i + "]";
        if (gat == null || !g.containsKey(gat)) { problemy.blad(plik, k + ".gatunek", "nie istnieje: " + gat); ok = false; continue; }
        List<String> rl = new ArrayList<>();
        if (m.get("ruchy") instanceof List<?> rr) for (Object o : rr) {
          if (!ruchy.containsKey(String.valueOf(o))) { problemy.blad(plik, k + ".ruchy", "ruch " + o + " nie istnieje"); ok = false; }
          else rl.add(String.valueOf(o));
        }
        String cecha = str(m.get("cecha"));
        if (cecha != null && !cechy.containsKey(cecha)) { problemy.blad(plik, k + ".cecha", "nie istnieje"); ok = false; }
        String chr = str(m.get("charakter"));
        if (chr != null && !ch.containsKey(chr)) { problemy.blad(plik, k + ".charakter", "nie istnieje"); ok = false; }
        dr.add(new Trener.Czlonek(gat, Math.max(1, Math.min(100, num(m.get("poziom"), 5))), List.copyOf(rl), cecha,
            Math.max(0, Math.min(31, num(m.get("geny"), 15))), chr));
      }
      if (dr.isEmpty() || dr.size() > 6) { problemy.blad(plik, id + ".druzyna", "1–6 stworów"); ok = false; }
      String odz = s.getString("odznaka", "");
      if (rodzaj == Trener.Rodzaj.MISTRZ && (odz.isBlank() || !ID.matcher(odz).matches())) { problemy.blad(plik, id + ".odznaka", "MISTRZ wymaga odznaki [A-Z0-9_]+"); ok = false; }
      if (!ok) continue;
      out.put(id, new Trener(id, s.getString("nazwa", id), rodzaj, odz, s.getInt("wymagane_odznaki", 0), List.copyOf(dr),
          List.copyOf(s.getStringList("nagroda_komendy")), List.copyOf(s.getStringList("komendy_po_walce"))));
    }
  }

  private Map<String, Wieza> wczytajWieze(Map<String, Trener> trenerzy) {
    Map<String, Wieza> out = new LinkedHashMap<>();
    File f = new File(dir, "wieza.yml");
    YamlConfiguration y = f.isFile() ? yaml(f) : null;
    if (y == null) return out;
    for (String id : y.getKeys(false)) {
      if (!poprawneId("wieza.yml", id)) continue;
      ConfigurationSection s = y.getConfigurationSection(id);
      if (s == null) continue;
      List<String> fale = s.getStringList("fale");
      List<String> brak = fale.stream().filter(t -> !trenerzy.containsKey(t)).toList();
      if (fale.isEmpty() || !brak.isEmpty()) { problemy.blad("wieza.yml", id + ".fale", "puste lub nieznani trenerzy: " + brak); continue; }
      Map<Integer, List<String>> nag = new TreeMap<>();
      ConfigurationSection ns = s.getConfigurationSection("nagrody");
      if (ns != null) for (String k : ns.getKeys(false)) {
        try { nag.put(Integer.parseInt(k), List.copyOf(ns.getStringList(k))); }
        catch (NumberFormatException e) { problemy.blad("wieza.yml", id + ".nagrody." + k, "klucz = numer fali"); }
      }
      out.put(id, new Wieza(id, s.getString("nazwa", id), List.copyOf(fale),
          Math.max(0, Math.min(100, s.getInt("leczenie_miedzy_falami", 25))), Map.copyOf(nag)));
    }
    return out;
  }

  private Map<String, NagrodaBestiariusza> wczytajNagrody() {
    Map<String, NagrodaBestiariusza> out = new LinkedHashMap<>();
    File f = new File(dir, "bestiariusz-nagrody.yml");
    YamlConfiguration y = f.isFile() ? yaml(f) : null;
    if (y == null) return out;
    for (String id : y.getKeys(false)) {
      ConfigurationSection s = y.getConfigurationSection(id);
      if (s == null || !poprawneId("bestiariusz-nagrody.yml", id)) continue;
      NagrodaBestiariusza.Warunek w;
      try { w = NagrodaBestiariusza.Warunek.valueOf(s.getString("warunek.rodzaj", "").toUpperCase(Locale.ROOT)); }
      catch (IllegalArgumentException e) { problemy.blad("bestiariusz-nagrody.yml", id + ".warunek.rodzaj", "PROCENT_ZLAPANYCH|KOMPLET_TYPU|WSZYSTKIE_LSNIACE"); continue; }
      Typ t = Typ.parse(s.getString("warunek.typ"));
      if (w == NagrodaBestiariusza.Warunek.KOMPLET_TYPU && t == null) { problemy.blad("bestiariusz-nagrody.yml", id + ".warunek.typ", "wymagany typ"); continue; }
      int wart = s.getInt("warunek.wartosc", 0);
      if (w == NagrodaBestiariusza.Warunek.PROCENT_ZLAPANYCH && (wart < 1 || wart > 100)) { problemy.blad("bestiariusz-nagrody.yml", id + ".warunek.wartosc", "1..100"); continue; }
      out.put(id, new NagrodaBestiariusza(id, w, wart, t, s.getString("opis", id), List.copyOf(s.getStringList("komendy"))));
    }
    return out;
  }

  public static final Set<String> ZDARZENIA = Set.of("ZLAPANIE", "LSNIACY_POJAWIL_SIE", "EWOLUCJA", "WYKLUCIE",
      "STARTER", "ODZNAKA", "WYGRANA_POJEDYNKU", "WYGRANA_TURNIEJU", "FALA_WIEZY");

  private Map<String, List<String>> wczytajZdarzenia() {
    Map<String, List<String>> out = new LinkedHashMap<>();
    File f = new File(dir, "zdarzenia.yml");
    YamlConfiguration y = f.isFile() ? yaml(f) : null;
    if (y == null) return out;
    for (String k : y.getKeys(false)) {
      if (!ZDARZENIA.contains(k)) { problemy.uwaga("zdarzenia.yml", k, "nieznane zdarzenie (lista: KONTRAKT §11)"); continue; }
      out.put(k, List.copyOf(y.getStringList(k)));
    }
    return out;
  }
}
