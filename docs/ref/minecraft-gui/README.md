# Minecraft 1.21.x GUI textures - fragmenty referencyjne (pobrane 2026-10-21)

## A. Zawartosc oficjalnego klienta 1.21.11 (zrodlo pierwotne: client.jar, version.json id=1.21.11)
Plik: /home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client/client.jar (klasy zaciemnione; sprawdzono tablice stalych klas).
- `assets/minecraft/textures/gui/container/inventory.png` = 256x256 (4-bit). Klasa `gti` (zaciemniona) zawiera lancuch `textures/gui/container/inventory.png` oraz `container/slot_highlight_front|back`.
- `textures/gui/container/creative_inventory/tab_inventory.png` = 256x256, OSOBNY plik (kreatywny nie uzywa inventory.png wg stalych klas: tylko `gti` ma inventory.png).
- Efekty w E: sprity `gui/sprites/container/inventory/effect_background.png` i `effect_background_ambient.png` (32x32, z .mcmeta), uzywane przez klase `gud`. HUD uzywa osobnych `gui/sprites/hud/effect_background*.png` (klasa `giq`).
- Przycisk ksiazki receptur: `gui/sprites/recipe_book/button.png`, `button_highlighted.png` (20x18); klasa `gwv` (wspolna dla ksiazki receptur: crafting table/piec tez).
- Sloty: `gui/sprites/container/slot/{helmet,chestplate,leggings,boots,shield,...}.png`, `container/slot.png`.
- `gui/sprites/container/slot_highlight_front.png.mcmeta` (vanilla): {"gui":{"scaling":{"type":"nine_slice","width":24,"height":24,"border":4}}}; analogicznie back. Osobna para `container/bundle/slot_highlight_*`.

## B. minecraft.wiki/w/Resource_pack (odczyt 2026-10-21)
- "All sprites in the `gui` atlas support a `.mcmeta` file ... containing scaling behavior" ; type: stretch (domyslny) | tile | nine_slice; width/height wymagane dla tile i nine_slice; border/stretch_inner dla nine_slice.
- "For most other textures, the file is stretched to fit the required dimensions." (brak wymogu rozdzielczosci dla inventory.png).
- 21w39a: format packa 8, bo "inventory.png now contains an extra sprite for a thin-layout version of the effect list" (stad w inventory.png pozostal fragment ukladu efektow; pozniej przeniesione do sprites).
- Ref: https://minecraft.wiki/w/Resource_pack

## C. minecraft.wiki 1.20.2 (resource pack)
- "All textures containing multiple sprites in a sheet for GUI have been split into individual sprites" w `textures/gui/sprites` (https://minecraft.wiki/w/Java_Edition_1.20.2).

## D. minecraft.wiki 1.21.2
- "Updated slot highlight so that item texture has better visibility." "The previous textures have been added to Programmer Art" (https://minecraft.wiki/w/Java_Edition_1.21.2). Nie potwierdzono dosl., ze to wtedy wprowadzono sprite slot_highlight_front/back (strona nie podaje nazw plikow w czesci, ktora przeczytano).

## E. Nexo (lokalna wiki: Plugins-wiki-local/nexo/wiki/md/configuration-glyphs.md l.44)
- "If your texture is above 256x256 resolution you need to either downscale it or make a Multi-Bitmap Glyph" - limit dotyczy GLIFOW (tekstur czcionki), nic o nadpisaniach `minecraft:`.
- Multi-bitmap glyph: configuration-glyphs-multi-bitmap-glyph.md l.8.

## F. OptiFine Custom GUIs (https://optifine.readthedocs.io/custom_guis.html)
- "`inventory` refers to the normal survival inventory (with the player in a window)" - mozliwe osobne nadpisanie per kontener (nie dotyczy czystego vanilla/Sodium).

## G. Pakiety komercyjne (BuiltByBit) - Wonderland GUI Pack (Oraxen/IA/Nexo)
- Changelog: autor usunal pszczoly i model Steve'a z UI ekwipunku, bo przycisk craftingu nakladal sie na nie w nowszych wersjach (https://builtbybit.com/resources/wonderland-gui-pack-oraxen-ia-nexo.106693/update/262922/). Jedno zrodlo.
- Wynncraft 2.1: "Reworked the inventory GUI" (https://forums.wynncraft.com/threads/2-1-rekindled-world-changelog.316880/, wg /home/dev/Desktop/Przemek/MC/docs/research/wynncraft/1-research-tekstowy.md l.87).
