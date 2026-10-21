package pl.mcmmorpg.huddane;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * %hud_...%:
 *  druzyna_liczba (inni online), druzyna_<n>_{nazwa,nick,poziom,hp,hpmax,zasob,zasobmax,stan,lider};
 *    stan = ok|nisko|daleko|martwy; teksty BEZ tagow MiniMessage (MythicHUD liczylby ich szerokosc).
 *  slugi_liczba, slugi_limit, slugi_<n>_{nazwa,hp,hpmax,s}.
 *  towarzysze = 0|1|2 (ile wierszy panelu: Chowaniec, slugi) -> uklad kolumny HUD-a.
 *  uklad = e<0|1>c<0|1>t<0|1|2>: rzad efektow, cel zadania (zadanie_widac), czy jest rzad efektow i ile wierszy towarzyszy -> przesuniecie paneli kolumny.
 *  cel_{jest,nazwa,hp,hpmax,poziom,ranga}: ostatni cel walki (8 s); ranga = zwykly|mythic|boss (boss = mob MM z bossbarem).
 *  zdobycz_liczba, zdobycz_<n>_{tekst,rodzaj}: ostatnie 4 zdobycze (4,5 s); rodzaj = exp|profesja|przedmiot.
 *  ostrzezenie = zuzycie|pelny|brak (bron w rece >= 85% zuzycia, pelny ekwipunek). loch = tak|nie (MythicDungeons).
 *  zadanie_{jest,tytul,postep,krok,kierunek,odl}: cel zadania (MMOCore albo samouczek), krok przyciety, strzalka n..nw|tu|brak.
 *  obrazenia_kierunek = n..nw|brak (1,2 s). awans_{blisko,brakuje}. umiejetnosc_<n>_{stan,skrot,cd} (sloty MMOCore).
 *  okno = pelne|przygaszone (walka MMOCore, walka Chowancow albo tryb kompakt) -> przygaszenie okna postaci.
 *  pet_typ = pierwszy typ aktywnego Chowanca.
 *  efekty_liczba, efekt_<n>_{id,czas}; czas = m:ss albo pusty dla efektu bez konca.
 * Indeksy n od 1; puste miejsce = pusty tekst, liczby = 0.
 */
final class Placeholdery extends PlaceholderExpansion {
  private final HudDane pl;

  Placeholdery(HudDane pl) { this.pl = pl; }

  @Override public String getIdentifier() { return "hud"; }
  @Override public String getAuthor() { return "mcmmorpg"; }
  @Override public String getVersion() { return pl.getPluginMeta().getVersion(); }
  @Override public boolean persist() { return true; }

  @Override public String onRequest(OfflinePlayer op, String k) {
    if (!(op instanceof Player p) || !p.isOnline()) return "";
    Migawka m = pl.migawka(p);
    if (k.equals("druzyna_liczba")) return String.valueOf(m.druzyna.size());
    if (k.equals("slugi_liczba")) return String.valueOf(m.slugi.size());
    if (k.equals("slugi_limit")) return String.valueOf(pl.limitSlug);
    if (k.equals("efekty_liczba")) return String.valueOf(m.efekty.size());
    if (k.equals("towarzysze")) {
      boolean pet = "tak".equals(PlaceholderAPI.setPlaceholders(p, "%chowance_aktywny_istnieje%"));
      return String.valueOf((pet ? 1 : 0) + (m.slugi.isEmpty() ? 0 : 1));
    }
    if (k.equals("zadanie_widac")) return zadanieWidac(p) ? "tak" : "nie";
    if (k.startsWith("zadanie_")) {
      String[] z = pl.nawigacja.zadanie(p);
      return switch (k) {
        case "zadanie_jest" -> z == null ? "nie" : "tak";
        case "zadanie_tytul" -> z == null ? "" : z[0];
        case "zadanie_postep" -> z == null ? "" : z[1];
        case "zadanie_krok" -> z == null ? "" : z[2];
        case "zadanie_kierunek", "zadanie_odl" -> {
          String[] kr = z == null ? null : pl.nawigacja.kierunek(p, z[2]);
          yield kr == null ? (k.endsWith("odl") ? "" : "brak") : (k.endsWith("odl") ? kr[1] + " m" : kr[0]);
        }
        default -> null;
      };
    }
    if (k.equals("obrazenia_kierunek")) return pl.nawigacja.kierunekObrazen(p);
    if (k.startsWith("awans_")) {
      double proc = parseD(PlaceholderAPI.setPlaceholders(p, "%mmocore_level_percent%"));
      long brak = Math.max(0, Math.round(parseD(PlaceholderAPI.setPlaceholders(p, "%mmocore_next_level%"))
          - parseD(PlaceholderAPI.setPlaceholders(p, "%mmocore_experience%"))));
      return switch (k) { case "awans_blisko" -> proc >= 90 ? "tak" : "nie"; case "awans_brakuje" -> String.valueOf(brak); default -> null; };
    }
    if (k.startsWith("umiejetnosc_")) {
      String[] u = k.split("_", 3);
      if (u.length < 3) return null;
      String nazwa = bezKolorow(PlaceholderAPI.setPlaceholders(p, "%mmocore_bound_" + u[1] + "%"));
      // pusty slot MMOCore zwraca komunikat ("Brak przypisanej..."), wiec stan z API; pasywne nie maja odnowienia w HUD-zie
      int slot; try { slot = Integer.parseInt(u[1]); } catch (NumberFormatException e) { return null; }
      var bs = net.Indyuce.mmocore.api.player.PlayerData.has(p) ? net.Indyuce.mmocore.api.player.PlayerData.get(p).getBoundSkills().get(slot) : null;
      boolean jest = bs != null && !bs.isPassive();
      double cd = jest ? parseD(PlaceholderAPI.setPlaceholders(p, "%mmocore_cooldown_bound_" + u[1] + "%")) : 0;
      return switch (u[2]) {
        case "stan" -> !jest ? "brak" : cd > 0.05 ? "odnowienie" : "gotowa";
        case "skrot" -> !jest ? "" : nazwa.length() > 7 ? nazwa.substring(0, 6) + "." : nazwa;
        case "cd" -> cd > 0.05 ? (cd >= 10 ? String.valueOf((int) Math.ceil(cd)) : String.format(java.util.Locale.ROOT, "%.1f", cd)) : "";
        default -> null;
      };
    }
    if (k.startsWith("cel_")) {
      var le = pl.zdarzenia.cel(p);
      var am = le == null ? null : Zdarzenia.mm(le);
      return switch (k) {
        case "cel_jest" -> le == null ? "nie" : "tak";
        case "cel_nazwa" -> le == null ? "" : bezKolorow(am != null ? am.getDisplayName() : le.getName());
        case "cel_hp" -> le == null ? "0" : liczba(le.getHealth());
        case "cel_hpmax" -> le == null ? "1" : liczba(le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null ? le.getHealth() : le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        case "cel_poziom" -> am == null ? "" : String.valueOf((int) am.getLevel());
        // boss = mob MythicMobs z wlasnym bossbarem (u gory ekranu): HUD nie dubluje go ramka celu
        case "cel_ranga" -> am == null ? "zwykly" : am.getType().usesBossBar() ? "boss" : "mythic";
        default -> null;
      };
    }
    if (k.startsWith("zdobycz_")) {
      var lista = pl.zdarzenia.swieza(p);
      if (k.equals("zdobycz_liczba")) return String.valueOf(lista.size());
      String[] z = k.split("_", 3);
      if (z.length < 3) return null;
      int i; try { i = Integer.parseInt(z[1]) - 1; } catch (NumberFormatException e) { return null; }
      if (i < 0 || i >= lista.size()) return "";
      return switch (z[2]) { case "tekst" -> lista.get(i).tekst(); case "rodzaj" -> lista.get(i).rodzaj(); default -> null; };
    }
    if (k.equals("ostrzezenie")) {
      var reka = p.getInventory().getItemInMainHand();
      if (reka.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dm && reka.getType().getMaxDurability() > 0
          && dm.getDamage() >= reka.getType().getMaxDurability() * 0.85) return "zuzycie";
      if (p.getInventory().firstEmpty() < 0) return "pelny";
      return "brak";
    }
    if (k.equals("loch")) {
      String d = PlaceholderAPI.setPlaceholders(p, "%md_dungeon_name%");
      return d == null || d.isBlank() || d.startsWith("%") ? "nie" : "tak";
    }
    if (k.equals("uklad")) {
      boolean pet = "tak".equals(PlaceholderAPI.setPlaceholders(p, "%chowance_aktywny_istnieje%"));
      int t = (pet ? 1 : 0) + (m.slugi.isEmpty() ? 0 : 1);
      return "e" + (m.efekty.isEmpty() ? 0 : 1) + "c" + (zadanieWidac(p) ? 1 : 0) + "t" + t;
    }
    if (k.equals("okno")) {
      boolean walka = "true".equals(PlaceholderAPI.setPlaceholders(p, "%mmocore_in_combat%"))
          || "tak".equals(PlaceholderAPI.setPlaceholders(p, "%chowance_w_walce%"))
          || "compact".equals(PlaceholderAPI.setPlaceholders(p, "%luckperms_meta_ui_hud%"));
      return walka ? "przygaszone" : "pelne";
    }
    if (k.equals("pet_typ")) {
      String t = PlaceholderAPI.setPlaceholders(p, "%chowance_aktywny_typ%");
      String pierwszy = t.contains("/") ? t.substring(0, t.indexOf('/')) : t;
      return pierwszy; // bez tagu koloru, patrz nick
    }
    String[] c = k.split("_", 3);
    if (c.length < 3) return null;
    int n;
    try { n = Integer.parseInt(c[1]) - 1; } catch (NumberFormatException e) { return null; }
    switch (c[0]) {
      case "druzyna" -> {
        // Pusty slot: "nazwa" to cel warstwy head, ktora MythicHUD odswieza takze w ukrytym elemencie;
        // "" = wyjatek co odswiezenie (20.10: 3 mln stack trace'ow, 15 GB logu). Wlasny nick jest juz w cache glow.
        if (n < 0 || n >= m.druzyna.size()) return c[2].equals("nazwa") ? p.getName() : pusty(c[2]);
        Migawka.Czlonek x = m.druzyna.get(n);
        String stan = x.martwy() ? "martwy" : x.daleko() ? "daleko" : x.hp() / Math.max(1, x.hpMax()) < pl.niskieHp ? "nisko" : "ok";
        return switch (c[2]) {
          case "nazwa" -> x.nazwa();
          case "nick" -> x.nazwa(); // bez tagow: MythicHUD liczy szerokosc z surowego tekstu (tag = przesuniety HUD)
          case "poziom" -> String.valueOf(x.poziom());
          case "hp" -> liczba(x.hp());
          case "hpmax" -> liczba(x.hpMax());
          case "zasob" -> liczba(x.zasob());
          case "zasobmax" -> liczba(Math.max(1, x.zasobMax()));
          case "stan" -> stan;
          case "lider" -> x.lider() ? "tak" : "nie";
          default -> null;
        };
      }
      case "slugi" -> {
        if (n < 0 || n >= m.slugi.size()) return pusty(c[2]);
        Migawka.Sluga s = m.slugi.get(n);
        return switch (c[2]) {
          case "nazwa" -> s.nazwa();
          case "hp" -> liczba(s.hp());
          case "hpmax" -> liczba(s.hpMax());
          case "s" -> s.sekundy() + "s";
          default -> null;
        };
      }
      case "efekt" -> {
        if (n < 0 || n >= m.efekty.size()) return pusty(c[2]);
        Migawka.Efekt e = m.efekty.get(n);
        return switch (c[2]) {
          case "id" -> e.id();
          case "czas" -> e.sekundy() < 0 ? "" : e.sekundy() / 60 + ":" + String.format("%02d", e.sekundy() % 60);
          default -> null;
        };
      }
      default -> { return null; }
    }
  }

  /** Cel zadania widac poza walka, gdy jest co pokazac i gracz go nie wylaczyl (Ustawienia: ui_goal, tryb minimalny). */
  private boolean zadanieWidac(Player p) {
    if (pl.nawigacja.zadanie(p) == null) return false;
    String hud = PlaceholderAPI.setPlaceholders(p, "%luckperms_meta_ui_hud%"), cel = PlaceholderAPI.setPlaceholders(p, "%luckperms_meta_ui_goal%");
    return !"off".equals(cel) && !"minimal".equals(hud)
        && !"true".equals(PlaceholderAPI.setPlaceholders(p, "%mmocore_in_combat%"))
        && !"tak".equals(PlaceholderAPI.setPlaceholders(p, "%chowance_w_walce%"));
  }

  private static double parseD(String s) {
    try { return Double.parseDouble(s.replace(',', '.').replaceAll("[^0-9.-]", "")); } catch (Exception e) { return 0; }
  }

  private static String bezKolorow(String s) {
    if (s == null) return "";
    return s.replaceAll("(?i)[&§][0-9a-fk-orx]", "").replaceAll("<[^>]+>", "");
  }

  private static String pusty(String pole) {
    return switch (pole) { case "hp", "hpmax", "zasob", "poziom" -> "0"; case "zasobmax" -> "1"; default -> ""; };
  }

  private static String liczba(double v) { return String.valueOf((int) Math.round(v)); }
}
