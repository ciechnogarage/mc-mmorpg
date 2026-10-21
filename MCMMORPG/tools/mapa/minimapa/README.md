# Minimapa w rogu (etap 2) — NMinimap 1.0.9 + scalony shader z MythicHUD

Stan 2026-10-01: przetestowane na klonie mapa-proba, NIEWDROZONE (wdroz3.sh czeka na zgode wlasciciela).

- VanillaMinimaps-Next 1.1.1 ODPADA: na Paper 1.21.11 `NoSuchMethodError CommandSourceStack.hasPermission(int)`.
- NMinimap 1.0.9 (Modrinth lBBekvEQ, GPL-3.0) + AnvilORM 1.0.2 (github NezuShin) + packetevents (mamy). Dziala na 1.21.11.
- Konflikt: NMinimap i MythicHUD nadpisuja `rendertype_text`. Scalenie = shader NMinimap + import mythichud_utils
  + `applyCustomHud()` przed blokiem `nminimap:vertex_body` (pliki/rendertype_text.*). Kompiluje sie i minimapa dziala.
- Nexo wstrzykuje do rendertype_text `nexo_gif_utils` (deklaruje Sampler0 i GameTime) -> `Sampler0 redeclared`
  i klient wyrzuca WSZYSTKIE packi. Rozwiazanie: Nexo `Glyphs.generate_gif_shaders: false` (gifow nie uzywamy).
- Pack NMinimap bez wlasnego shadera NIE ukrywa mapy: lezy w swiecie jako duza mapa w niewidzialnej ramce.
- NMinimap mial alias komendy `mm` (= MythicMobs!) — usuniety z plugin.yml w pliki/NMinimap-1.0.9-bez-aliasu-mm.jar.
- Na klonie nie da sie zobaczyc HUD-u MythicHUD (bez Nexo nic nie rysuje) — HUD sprawdzic na world1 po wdrozeniu.
