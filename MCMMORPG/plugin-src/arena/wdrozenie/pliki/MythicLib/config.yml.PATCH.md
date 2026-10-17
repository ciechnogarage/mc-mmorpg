# Zmiana w plugins/MythicLib/config.yml

W istniejącej sekcji `interaction_rules.pvp_on.offense` dodaj dokładnie dwa klucze:

```yaml
      party_other: true
      guild_enemy: true
```

Zachowaj pozostałe wartości (`party_member: false`, `guild_ally: false`,
`guild_neutral: true`, `self: false`). Wcięcie nowych kluczy ma być takie
samo jak `party_member`. Ta zmiana odblokowuje PvP w ringu; globalną zaporę
stanowi `pvp-tylko-w-ringu: true` w pluginie Arena i flagi WorldGuard.
