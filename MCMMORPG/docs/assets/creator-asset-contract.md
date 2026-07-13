# Creator Asset Contract

Status: premium creator v1, 2026-07-05.

## Purpose

Ta tabela jest kanonicznym mapowaniem pomiędzy menu kreatora, MMOItems UI tokens i warstwą resource-pack/CMD.

Current runtime truth:

- creator menus currently render through supported `vanilla` and `player_head` buttons in `MenuCreator`,
- MMOItems token IDs remain a design/asset contract layer,
- final pack models and textures for those IDs are not yet sufficient proof of live creator rendering by themselves.

## Race Tokens

| Token | MMOItems ID | CMD | Screen use | Fallback |
| --- | --- | --- | --- | --- |
| Człowiek | `CREATOR_RACE_CZLOWIEK` | `9101` | race gallery, spotlight | `PAPER` |
| Ork | `CREATOR_RACE_ORK` | `9102` | race gallery, spotlight | `PAPER` |
| Elf | `CREATOR_RACE_ELF` | `9103` | race gallery, spotlight | `PAPER` |
| Krasnolud | `CREATOR_RACE_KRASNOLUD` | `9104` | race gallery, spotlight | `PAPER` |
| Nieumarły | `CREATOR_RACE_NIEUMARLY` | `9105` | race gallery, spotlight | `PAPER` |

## Class Tokens

| Token | MMOItems ID | CMD | Screen use | Fallback |
| --- | --- | --- | --- | --- |
| Wojownik | `CREATOR_CLASS_WARRIOR` | `9201` | class gallery, spotlight, oath | `IRON_SWORD` |
| Łotrzyk | `CREATOR_CLASS_ROGUE` | `9202` | class gallery, spotlight, oath | `IRON_SWORD` |
| Łowca | `CREATOR_CLASS_MARKSMAN` | `9203` | class gallery, spotlight, oath | `BOW` |
| Mag | `CREATOR_CLASS_MAGE` | `9204` | class gallery, spotlight, oath | `BLAZE_POWDER` |
| Akolita | `CREATOR_CLASS_PALADIN` | `9205` | class gallery, spotlight, oath | `BLAZE_ROD` |

## Subclass Crests

| Family | MMOItems IDs |
| --- | --- |
| Warrior | `CREATOR_SUBCLASS_GUARDIAN`, `CREATOR_SUBCLASS_BERSERKER`, `CREATOR_SUBCLASS_GLADIATOR` |
| Rogue | `CREATOR_SUBCLASS_ASSASSIN`, `CREATOR_SUBCLASS_SABOTEUR`, `CREATOR_SUBCLASS_SHADOW` |
| Marksman | `CREATOR_SUBCLASS_SNIPER`, `CREATOR_SUBCLASS_TRACKER`, `CREATOR_SUBCLASS_BESTIARIST` |
| Mage | `CREATOR_SUBCLASS_PYROMANCER`, `CREATOR_SUBCLASS_CRYOMANCER`, `CREATOR_SUBCLASS_TEMPEST` |
| Paladin | `CREATOR_SUBCLASS_CHAPLAIN`, `CREATOR_SUBCLASS_INQUISITOR`, `CREATOR_SUBCLASS_RITUALIST` |

## State Tokens

| State | MMOItems ID | CMD |
| --- | --- | --- |
| Recommended | `CREATOR_STATE_RECOMMENDED` | `9401` |
| Confirm | `CREATOR_STATE_CONFIRM` | `9402` |
| Runtime bridge | `CREATOR_STATE_BRIDGE` | `9403` |

## Resource-Pack Requirement

- CMD IDs są zarezerwowane dla kreatora i nie mogą być współdzielone z przypadkowym lootem.
- Jeśli pack nie dostarczy własnej tekstury, fallback materiał musi pozostać czytelny w GUI.
- Built pack i source pack muszą zachować to samo mapowanie ID.
