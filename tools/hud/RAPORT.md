# Raport — HUD world1

## Zrobione
- Obejrzano `inspiracje/w1`–`w6` i oba obrazy `ref`; karta nawiązuje do Volya, ramka i podział do Lumin, paleta do Księgi Wędrowca.
- Karta 124×54 GUI w lewym górnym rogu: aktualna głowa, poziom, nick/klasa, HP z liczbą, mana, stamina, EXP, absorpcja; niski HP, niski poziom staminy, trucizna i uschnięcie mają osobne stany.
- Chowaniec i panele walki odsunięte pod kartę. Usunięto `default-layout` i `sword-layout` z aktywnej listy.
- Minimapa zakotwiczona po prawej przez istniejące `side: right` NMinimap; własna cienka ramka. Shader MythicHUD/NMinimap pozostał bez zmian. Skrypt wdrożenia przestawi także `isRight` zapisane w SQLite dla obecnych graczy.
- Dolny pasek: wariant A z grupami slotów 0–1 / 2–4 / 5–6 / 7–8 i skrzydłami zbroi/głodu, zachowane zaklęcia. Wariant B w makietach to cztery osobne segmenty. Sloty 7/8 potwierdzone w CoreTools.
- 17 makiet 1920×1080: GUI 3/4, 4 stany, oba warianty, crop karty ×2. Generator `makiety_gen.py`.

## Pliki
- `praca/config.yml`, `praca/layouts/{karta,hotbar,chowance}.yml`, `praca/hud_assets/hud/{karta,hotbar}.yml`, `praca/listeners/karta.yml`.
- `praca/pack/assets/mythichud/textures/assets/karta/`, `praca/pack/assets/minecraft/textures/gui/sprites/hud/hotbar.png`.
- `praca/NMinimap/config.yml`, `praca/NMinimap/frames/karta_square.png`, `praca/MMOCore/config.yml`.
- `wdroz.sh` z kopią i `cofnij.sh` (nie uruchomiony), `README.md`, `makiety/`.

## Decyzje i sprawdzenie w kliencie
- Vanilla HP/głód/zbroja/powietrze/EXP/poziom chowa `elements: ... hidden`; powietrze ma warunkowy pasek MythicHUD. Hotbar i zaznaczenie zostają, jego tło podmienia `hotbar.png`. `hide-exp: true` zapobiega duplikacji EXP.
- Domyślny action bar MMOCore wyłączony (`action-bar.enabled: false`); komentarz pluginu potwierdza, że inne action bary MMOCore zostają.
- Obrazy HUD są 2×/`scale: 0.5`; vanilla hotbar jest 1×, bo 2× przekroczyłby limit 256 px. Żaden plik PNG w paczce nie przekracza 256 px.
- Do testu: skóra po `/cmi skin`, EXP i absorpcja, długie nicki/klasy, GUI 3/4 (odstęp współrzędnych od mapy), zaznaczenie hotbara, spectator, walka Chowańców i action bar innych pluginów.
- Dusze Akolity i płonięcie bez pewnego placeholdera: zasób pokazuje manę, pomarańczowego stanu palenia nie włączono. Sloty 0–6 wymagają potwierdzenia w kliencie.
