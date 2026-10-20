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
 *  uklad = e<0|1>t<0|1|2>: czy jest rzad efektow i ile wierszy towarzyszy -> przesuniecie paneli kolumny.
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
    if (k.equals("uklad")) {
      boolean pet = "tak".equals(PlaceholderAPI.setPlaceholders(p, "%chowance_aktywny_istnieje%"));
      int t = (pet ? 1 : 0) + (m.slugi.isEmpty() ? 0 : 1);
      return "e" + (m.efekty.isEmpty() ? 0 : 1) + "t" + t;
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
        if (n < 0 || n >= m.druzyna.size()) return pusty(c[2]);
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

  private static String pusty(String pole) {
    return switch (pole) { case "hp", "hpmax", "zasob", "poziom" -> "0"; case "zasobmax" -> "1"; default -> ""; };
  }

  private static String liczba(double v) { return String.valueOf((int) Math.round(v)); }
}
