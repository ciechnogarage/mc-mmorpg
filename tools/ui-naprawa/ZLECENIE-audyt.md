Jesteś bezlitosnym, kompetentnym krytykiem UI/UX gier (HUD, menu, okna, komunikaty, onboarding, czytelność, dostępność, feedback walki). Oceniasz serwer Minecraft MMORPG "world1" (Paper 1.21.x, klient vanilla + resource pack; inspiracja Wynncraft). Wszystko po polsku.

ETAP 1 - RESEARCH W SIECI (obowiązkowy, PRZED oceną): znajdź 8-15 cenionych, topowych źródeł o projektowaniu HUD/UI/UX w grach: GDC talks (np. o HUD-ach i diegetic UI), Game UI Database, Celia Hodent "The Gamer's Brain"/UX w grach, Game Accessibility Guidelines, Xbox Accessibility Guidelines, analizy UI WoW/FFXIV/Guild Wars 2/Lost Ark/Diablo IV/Path of Exile 2, Wynncraft (jego HUD i menu), artykuły typu Fragnet/UX Collective o HUD w MMO, Nielsen heurystyki zastosowane do gier. Z każdego wyciągnij konkretne zasady/progi (nie ogólniki). Tylko źródła, które realnie przeczytałeś.

ETAP 2 - CO MAMY (czytaj, niczego nie zmieniaj):
- Zrzuty z prawdziwego klienta: /home/dev/.claude/jobs/37dc945b/tmp/material/zrzuty/ (obejrzyj KAŻDY obraz; gra_* i B_gui4_noc = HUD; menu_all/portal = menu Q i Portal; r2/r3 = odkrycie/odliczanie podróży; 01/03/10 = Chowańce; t*.png = komunikaty). Zrzuty mogą być lekko starsze niż konfigi - konfig jest źródłem prawdy.
- HUD: /opt/docker/mc/data/world1/plugins/MythicHUD/{config.yml,layouts,popups,listeners,hud_assets} oraz źródło /home/dev/Desktop/Przemek/MC/tools/hud/ (README, RAPORT*, praca/).
- Minimapa: /opt/docker/mc/data/world1/plugins/NMinimap/ (config). Paski HP mobów: plugins/PaskiMobow/. Zaklęcia (pasek, Shift+Q): plugins/Zaklecia/ + tools/zaklecia/README.md. Klawisz Q: plugins/MenuKey/.
- Menu: /opt/docker/mc/data/world1/plugins/DeluxeMenus/gui_menus/*.yml (hub MENU = 12 kafli + dolny pasek), okno E: plugins/KsiegaPostaci/.
- Komunikaty/czat/action bar: /home/dev/Desktop/Przemek/MC/tools/komunikaty/AUDYT-2026-10-02.md i AUDYT2-szeroki-2026-10-03.md (nie powtarzaj ich - sprawdź, co z nich nadal nie jest naprawione w konfigach), plugins/MMOCore/messages.yml, plugins/MMOCore/language/ jeśli są.
Ograniczenia, które są faktami (nie krytykuj ich jako wyboru, ale możesz krytykować sposób obejścia): serwer nie przechwytuje klawiszy E ani Tab; tła okien to glify fontu w tytule; HUD rysowany fontem przez MythicHUD; klient jest vanilla (bez modów).

ETAP 3 - OCENA: porównaj każdy element z zasadami z etapu 1. Wytknij WSZYSTKO, co znajdziesz: czytelność (rozmiar, kontrast, GUI scale 2/3/4), hierarchia, zagracenie ekranu, spójność stylu, redundancja informacji, feedback (obrażenia, cooldowny, zasoby, stany), komunikaty (spam, język mieszany PL/EN, terminologia), nawigacja w menu (ilość kliknięć, powrót, skróty), onboarding nowego gracza, dostępność (daltonizm, tekst, migotanie), wydajność/flicker HUD, zachowanie w walce vs poza walką. Bez lania wody, bez pochwał dla zasady, bez łagodzenia. Każdy zarzut musi mieć dowód: plik:linia albo nazwa zrzutu + co dokładnie widać. Nie wymyślaj stanu runtime - jeśli coś wnioskujesz z konfigu bez potwierdzenia na zrzucie, oznacz [niepotwierdzone].

WYNIK: zapisz do /home/dev/.claude/jobs/37dc945b/tmp/out/krytyka.md, MAKS 120 linii, format:
1. Źródła (lista: tytuł - URL - 1 zasada, którą z niego biorę), max 15 linii.
2. Werdykt ogólny: ocena /10 dla HUD, menu, komunikatów, onboardingu + 3 zdania.
3. Tabela zarzutów posortowana od najcięższych: | # | waga (KRYT/WYS/ŚR/NIS) | element | zarzut | dowód | naruszona zasada (źródło) | poprawka w 1 zdaniu |
4. Top 5 rzeczy do zrobienia najpierw.
Nie wklejaj kodu ani diffów. Nie modyfikuj żadnych plików poza krytyka.md. Nie uruchamiaj komend serwera, restartów ani reloadów.
