'use strict';

// Poziom 2: provider oparty o CompanionBridge (Paper plugin, patrz
// MCMMORPG/plugin-src/companion-bridge/). Kontrakt: contracts/companion-bridge/v1.

const { createCompanionClient } = require('./companion-client');

function createCompanionProvider(clientOpts = {}) {
  const client = createCompanionClient(clientOpts);
  return {
    mode: 'companion',
    async health() {
      return client.health();
    },
    async getTps() {
      const perf = await client.performance();
      return { raw: null, tps: [perf.tps.last1m, perf.tps.last5m, perf.tps.last15m] };
    },
    async getPlayerSnapshot(uuid) {
      const snapshot = await client.playerSnapshot(uuid);
      if (!snapshot.player || !snapshot.player.online) {
        return null;
      }
      return { ...snapshot.player, integrations: snapshot.integrations };
    }
  };
}

module.exports = { createCompanionProvider };
