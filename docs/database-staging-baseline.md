# Database Staging Baseline

Status: infrastructure prepared, not yet live-validated.

This baseline adds one private MariaDB service for the Docker staging stack.
It prepares persistent storage without enabling SQL for CMI, HuskSync,
MMOProfiles, MMOInventory, MCPets, or any other plugin. Each plugin migration
needs its own syntax check, backup, rollback, and staging evidence.

## Security Boundary

- MariaDB is attached only to the `mcmmorpg_net` Docker network. It has no
  host port mapping, so use `mariadb:3306` only from containers on that network.
- `MCMMORPG/docker/.env` contains local secrets and is not tracked by Git.
- `MCMMORPG/docker/.env.example` is a committed template only. Replace every
  `change-me` value in the local `.env` before starting the database.
- Plugins must use the application user from `MARIADB_USER`; do not configure
  a plugin with the MariaDB root account.

## First Local Start

1. Edit the ignored `MCMMORPG/docker/.env` and add the four `MARIADB_*`
   variables from `.env.example` with unique local values.
2. From `MCMMORPG/docker`, start only the database:

   ```bash
   docker compose up -d mariadb
   docker compose ps mariadb
   docker compose logs mariadb
   ```

3. Start an affected backend only after `mariadb` reports `healthy`. Docker
   Compose enforces this dependency for `world`, `items`, and `hub`.

## Migration Rules

- Keep current plugin storage unchanged until its exact SQL syntax is verified
  from installed configuration or primary documentation.
- Allocate unique database names or table prefixes for each server/plugin that
  writes persistent state. CMI explicitly warns against sharing table sets.
- Back up plugin data before moving it from file/SQLite storage to MariaDB.
- Record the successful connection, created schema, backend logs, and an
  in-game persistence check in the relevant contract evidence path.
- Re-enable HuskSync only in a dedicated change after a verified database
  connection and a player-data rollback test.

## Rollback And Reset

- To stop the database without removing data: `docker compose stop mariadb`.
- The named `mariadb_data` volume holds persistent staging data. Removing it
  with `docker compose down -v` is destructive and requires an explicit backup
  decision.

## Next Work

The first database consumer should be selected from the current contract
registry. Validate one consumer and one persistence flow end-to-end before
enabling another plugin.
