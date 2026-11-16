'use strict';

// Etap 6, scenariusz: dungeon-reset (MythicDungeons, domena "world").
//
// Cel: wejscie do instancji, stan podczas gry, opuszczenie, i dowod ze
// kolejne wejscie dostaje SWIEZA instancje (bez odziedziczonego stanu).
//
// Odkrycia po drodze (2026-07-14, zweryfikowane na zywo na srv-world):
// - `/md play <dungeon>` przez RCON/konsole zawsze wymaga jawnego gracza
//   ("From console this command requires you specify the player!").
// - Nawet z graczem, `/md play level_1` (bez trudnosci) NIC nie robi
//   obserwowalnego z zewnatrz — bo level_1 ma `EnableDifficultyMenu: true`
//   (config.yml), wiec otwiera GUI wyboru trudnosci zamiast startowac od
//   razu. Trzeba podac trudnosc jawnie: `/md play level_1:EASY`.
// - Tak jak w vault-economy-provider: admin-owe komendy MythicDungeons
//   przez RCON/konsole moga byc cicho odrzucane bez feedbacku — bot sam
//   wysyla komende na czacie (z tymczasowym op przez RCON).
// - Kazde wejscie dostaje instancje o inkrementowanej nazwie swiata
//   (`level_1_0`, `level_1_1`, ...) — dowod ze nie ma recyklingu starego
//   stanu miedzy wejsciami.
//
// Uzycie: node apps/acceptance/runner.js dungeon-reset
//   [--user qa_bridge_econ] [--dungeon level_1] [--difficulty EASY]

const { BotSession, sleep, waitFor } = require('@mmorpg-bots/bot-core');
const bridge = require('@mmorpg-bots/mmo-bridge');
const { createCompanionClient } = require('@mmorpg-bots/mmo-bridge/companion-client');
const { runScenario, assertTruthy } = require('@mmorpg-bots/qa');

function arg(name, fallback) {
  const idx = process.argv.indexOf(`--${name}`);
  return idx !== -1 && process.argv[idx + 1] ? process.argv[idx + 1] : fallback;
}

function offlineUuid(name) {
  const crypto = require('crypto');
  const hash = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  hash[6] = (hash[6] & 0x0f) | 0x30;
  hash[8] = (hash[8] & 0x3f) | 0x80;
  const hex = hash.toString('hex');
  return [hex.slice(0, 8), hex.slice(8, 12), hex.slice(12, 16), hex.slice(16, 20), hex.slice(20)].join('-');
}

async function run(argv = []) {
  const username = arg('user', 'qa_bridge_econ');
  const dungeon = arg('dungeon', 'level_1');
  const difficulty = arg('difficulty', 'EASY');
  const domain = 'world';
  const uuid = offlineUuid(username);
  const client = createCompanionClient({ baseUrl: 'http://127.0.0.1:25580' });

  let session;
  let firstInstanceWorld;
  const disconnect = async () => {
    if (session) { try { await session.stop(); } catch { /* best effort */ } }
  };
  process.on('exit', () => { if (session) session.stop().catch(() => {}); });

  const playOnce = async () => {
    await bridge.rcon(`op ${username}`, { domain });
    try {
      session.bot.chat(`/md play ${dungeon}:${difficulty}`);
      await sleep(4000);
    } finally {
      await bridge.rcon(`deop ${username}`, { domain });
    }
    return client.playerSnapshot(uuid);
  };

  return runScenario({
    name: 'dungeon-reset',
    steps: [
      {
        name: 'bot dolacza i przelacza sie na domene world',
        critical: true,
        run: async () => {
          session = new BotSession({ username, reconnect: false, log: { info: () => {}, warn: () => {} } });
          await session.connect();
          session.bot.chat('/server world');
          const snap = await waitFor(async () => {
            const s = await client.playerSnapshot(uuid);
            return s.player.online ? s : null;
          }, 15000, 500);
          return { world: snap.player.world, location: snap.player.location };
        }
      },
      {
        name: `pierwsze wejscie do ${dungeon}:${difficulty} tworzy instancje`,
        critical: true,
        run: async () => {
          const snap = await playOnce();
          assertTruthy(snap.player.online, 'gracz online w instancji');
          if (!snap.player.world || !snap.player.world.startsWith(dungeon)) {
            throw new Error(`oczekiwano swiata zaczynajacego sie od "${dungeon}", jest "${snap.player.world}"`);
          }
          firstInstanceWorld = snap.player.world;
          return { instanceWorld: snap.player.world, location: snap.player.location };
        }
      },
      {
        name: 'opuszczenie wraca gracza do domeny world',
        critical: true,
        run: async () => {
          session.bot.chat('/md leave');
          await sleep(2000);
          const snap = await client.playerSnapshot(uuid);
          if (snap.player.world !== 'world') {
            throw new Error(`oczekiwano powrotu do "world", jest "${snap.player.world}"`);
          }
          return { world: snap.player.world };
        }
      },
      {
        name: 'druga instancja ma inna nazwe swiata (brak recyklingu stanu)',
        critical: true,
        run: async () => {
          assertTruthy(firstInstanceWorld, 'nazwa pierwszej instancji zapamietana z poprzedniego kroku');
          const snap = await playOnce();
          const secondWorld = snap.player.world;
          session.bot.chat('/md leave');
          await sleep(2000);
          if (secondWorld === firstInstanceWorld) {
            throw new Error(`druga instancja ma te sama nazwe swiata co pierwsza (${secondWorld}) — brak dowodu resetu`);
          }
          return { firstInstance: firstInstanceWorld, secondInstance: secondWorld };
        }
      }
    ]
  }).finally(disconnect);
}

module.exports = { run };

if (require.main === module) {
  run(process.argv.slice(2)).then(result => {
    console.log(`Werdykt: ${result.verdict}`);
    process.exit(result.verdict === 'PASS' ? 0 : 1);
  });
}
