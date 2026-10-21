# Zmiana w plugins/MythicLib/config.yml (robi ja wdroz.sh)

W sekcji `interaction_rules.pvp_on.offense` ustaw na `true`:
`party_other`, `guild_enemy` (brakowalo ich - przez to PvP nie dzialalo NIGDZIE) oraz
`party_member`, `guild_ally` (inaczej czlonkowie jednej druzyny/gildii nie moga sie bic w ringu - zmierzone 17.10).
`self: false` zostaje. `pvp_on` dziala tylko tam, gdzie swiat ma PvP i WG pozwala na PvP: po wdrozeniu to tylko
ring areny (swiaty glowne: WG pvp deny; lochy MythicDungeons: PvP: false). Reszta serwera uzywa `pvp_off`.
