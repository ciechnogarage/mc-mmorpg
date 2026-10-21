'use strict';

// Most do stanu server-side (plan sekcja J).
// Poziom 0: RCON przez ./docker/mc (legacy-rcon-provider.js) — kontrolowane
// give/tp/spawn oraz odczyt TPS/MSPT bez nadawania botowi uprawnień.
// Poziom 2: CompanionBridge (Paper plugin, companion-provider.js) — odczyt
// strukturalnego stanu gracza/serwera przez HTTP, patrz
// MCMMORPG/plugin-src/companion-bridge/ i contracts/companion-bridge/v1/.
//
// createProvider() wybiera i opakowuje jednego z powyższych za wspólnym
// interfejsem { mode, getTps(), getPlayerSnapshot(uuid) }. Reszta eksportów
// (rcon/safeRcon/getTps/registerStateChannel/DOCKER_DOMAIN) zostaje bez
// zmian dla wstecznej kompatybilności z istniejącymi wywołaniami.

const {
  rcon,
  safeRcon,
  getTps,
  registerStateChannel,
  DOCKER_DOMAIN,
  createLegacyRconProvider
} = require('./legacy-rcon-provider');
const { createCompanionProvider } = require('./companion-provider');

async function createProvider({
  mode = process.env.MC_BRIDGE_MODE || 'auto',
  domain = DOCKER_DOMAIN,
  companion = {},
  log = console
} = {}) {
  const rconProvider = createLegacyRconProvider({ domain });
  if (mode === 'rcon') {
    return rconProvider;
  }

  const companionProvider = createCompanionProvider(companion);
  if (mode === 'companion') {
    return companionProvider;
  }

  // auto: jedna sonda health() przy starcie decyduje o trybie startowym;
  // każde wywołanie ma i tak własny fallback na wypadek awarii w trakcie.
  try {
    await companionProvider.health();
  } catch (error) {
    log.warn(`[mmo-bridge] companion-bridge nieosiągalny (${error.message}) — auto wybiera RCON`);
    return rconProvider;
  }

  return {
    mode: 'companion+rcon-fallback',
    async getTps() {
      try {
        return await companionProvider.getTps();
      } catch (error) {
        log.warn(`[mmo-bridge] companion getTps() padł (${error.message}) — fallback RCON`);
        return rconProvider.getTps();
      }
    },
    async getPlayerSnapshot(uuid) {
      try {
        return await companionProvider.getPlayerSnapshot(uuid);
      } catch (error) {
        log.warn(`[mmo-bridge] companion getPlayerSnapshot() padł (${error.message}) — fallback RCON`);
        return rconProvider.getPlayerSnapshot(uuid);
      }
    }
  };
}

module.exports = {
  rcon,
  safeRcon,
  getTps,
  registerStateChannel,
  DOCKER_DOMAIN,
  createProvider,
  createLegacyRconProvider,
  createCompanionProvider
};
