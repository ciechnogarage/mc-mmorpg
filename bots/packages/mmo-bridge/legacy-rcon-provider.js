'use strict';

// Poziom 0: RCON przez ./docker/mc — kontrolowane give/tp/spawn oraz
// odczyt TPS/MSPT bez nadawania botowi uprawnień. Logika przeniesiona
// 1:1 z dawnego index.js (patrz git blame) — createProvider() w index.js
// opakowuje ją w providera zgodnego z companion-provider.js.

const path = require('path');
const { execFile } = require('child_process');

const DOCKER_MC = process.env.MC_DOCKER_MC
  || path.resolve(__dirname, '..', '..', '..', 'MCMMORPG', 'docker', 'mc');
const DOCKER_DOMAIN = process.env.MC_DOCKER_DOMAIN || 'items';

function rcon(command, { domain = DOCKER_DOMAIN } = {}) {
  return new Promise((resolve, reject) => {
    execFile(
      DOCKER_MC,
      ['rcon', domain, command],
      { cwd: path.dirname(DOCKER_MC), maxBuffer: 1024 * 1024 },
      (error, stdout, stderr) => {
        if (error) return reject(new Error((stderr || stdout || error.message).trim()));
        resolve((stdout || stderr || '').trim());
      }
    );
  });
}

async function safeRcon(command, opts) {
  try {
    return await rcon(command, opts);
  } catch (error) {
    return `ERR ${error.message}`;
  }
}

// TPS/MSPT z serwera (nie z klienta) — parsowanie /tps przez RCON.
async function getTps(opts) {
  const raw = await rcon('tps', opts);
  const clean = raw.replace(/\x1b\[[0-9;]*m/g, '').replace(/§[0-9a-fk-orx]/gi, '');
  const after = clean.includes(':') ? clean.slice(clean.indexOf(':') + 1) : clean;
  const matches = after.match(/(\d+(?:\.\d+)?)/g) || [];
  return { raw: clean.trim(), tps: matches.slice(0, 3).map(Number) };
}

// Companion-plugin channel (Poziom 2) — bot rejestruje kanał i odbiera
// strukturalny stan MMO. Działa dopiero, gdy plugin serwerowy istnieje.
function registerStateChannel(bot, { channel = 'mmoqa:state', onState, log = console } = {}) {
  try {
    bot._client.registerChannel?.(channel, undefined, true);
  } catch (error) {
    log.warn(`[mmo-bridge] registerChannel: ${error.message}`);
  }
  bot._client.on(channel, data => {
    try {
      const parsed = JSON.parse(Buffer.isBuffer(data) ? data.toString('utf8') : String(data));
      onState?.(parsed);
    } catch (error) {
      log.warn(`[mmo-bridge] nieparsowalny payload ${channel}: ${error.message}`);
    }
  });
}

// Provider zgodny z interfejsem companion-provider.js (getTps/getPlayerSnapshot).
// RCON nie ma strukturalnego odczytu gracza bez parsowania NBT — snapshot
// jawnie zwraca unsupported zamiast zgadywać/parsować kruche `/data get`.
function createLegacyRconProvider({ domain = DOCKER_DOMAIN } = {}) {
  return {
    mode: 'rcon',
    async getTps() {
      return getTps({ domain });
    },
    async getPlayerSnapshot(uuid) {
      return {
        unsupported: true,
        uuid,
        reason: 'legacy RCON provider nie udostępnia strukturalnego snapshotu gracza (patrz plan companion-bridge, Etap 5)'
      };
    }
  };
}

module.exports = { rcon, safeRcon, getTps, registerStateChannel, DOCKER_DOMAIN, createLegacyRconProvider };
