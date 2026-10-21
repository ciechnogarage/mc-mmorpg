'use strict';

// Etap 6, scenariusz podstawowy: companion-bridge-contract-smoke.
//
// Cel: potwierdzic, ze zywy CompanionBridge (Paper plugin) odpowiada
// zgodnie z kontraktem contracts/companion-bridge/v1 na wszystkich
// czterech endpointach — to jest fundament pod ktorym stoja pozostale
// scenariusze z planu (character-accept-e2e, vault-economy-provider,
// mmoprofiles-persistence, reward-idempotency, dungeon-reset). Te
// wymagaja realnego bota przechodzacego przez konkretne flow gry
// (DeluxeMenus/MMOProfiles GUI, Vault reward, MythicDungeons) i NIE sa
// jeszcze zaimplementowane — patrz README w tym katalogu.
//
// Uzycie: node apps/acceptance/runner.js companion-bridge-contract-smoke
//   [--player-uuid <uuid>]  (domyslnie syntetyczny UUID = gracz offline)

const fs = require('fs');
const path = require('path');
const { createCompanionClient } = require('@mmorpg-bots/mmo-bridge/companion-client');
const { runScenario, assertTruthy, validateContract } = require('@mmorpg-bots/qa');

const CONTRACTS_DIR = path.resolve(__dirname, '..', '..', '..', 'contracts', 'companion-bridge', 'v1');

function loadSchema(name) {
  return JSON.parse(fs.readFileSync(path.join(CONTRACTS_DIR, `${name}.schema.json`), 'utf8'));
}

function arg(name, fallback) {
  const idx = process.argv.indexOf(`--${name}`);
  return idx !== -1 && process.argv[idx + 1] ? process.argv[idx + 1] : fallback;
}

async function run(argv = []) {
  const playerUuid = arg('player-uuid', '11111111-1111-1111-1111-111111111111');
  const client = createCompanionClient();

  const checks = [
    ['health', () => client.health(), loadSchema('health')],
    ['capabilities', () => client.capabilities(), loadSchema('capabilities')],
    ['server/performance', () => client.performance(), loadSchema('performance')],
    [`players/${playerUuid}/snapshot`, () => client.playerSnapshot(playerUuid), loadSchema('player-snapshot')]
  ];

  return runScenario({
    name: 'companion-bridge-contract-smoke',
    steps: checks.map(([label, fetcher, schema]) => ({
      name: `GET /v1/${label}`,
      critical: true,
      run: async () => {
        const body = await fetcher();
        assertTruthy(body, `odpowiedz ${label}`);
        const { valid, errors } = validateContract(schema, body);
        if (!valid) {
          throw new Error(`kontrakt ${label} zlamany: ${errors.join('; ')}`);
        }
        return { contractVersion: body.contractVersion };
      }
    }))
  });
}

module.exports = { run };

if (require.main === module) {
  run(process.argv.slice(2)).then(result => {
    console.log(`Werdykt: ${result.verdict}`);
    process.exit(result.verdict === 'PASS' ? 0 : 1);
  });
}
