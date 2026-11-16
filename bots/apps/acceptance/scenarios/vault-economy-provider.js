'use strict';

// Etap 6, scenariusz: vault-economy-provider.
//
// Cel: potwierdzic ze bridge raportuje zarejestrowanego providera Economy,
// ze wykonanie nagrody (przelew CMI economy) zmienia saldo widoczne przez
// companion-bridge, i ze saldo pozostaje poprawne po rozlaczeniu gracza
// (persystencja, nie tylko stan w pamieci).
//
// Odkrycia po drodze (2026-07-14, zweryfikowane na zywo na srv-items):
// - RCON konsola NIE MA uprawnien CMI do `/money give` — CMI wysyla
//   "You don't have permission!" jako komponent czatu, ktorego RCON nie
//   przechwytuje (cichy no-op, zero bledu). Dlatego bot sam wysyla
//   komende na czacie, tymczasowo op'owany przez RCON (`op`/`deop`) —
//   nie zgadujemy dokladnego wezla uprawnien CMI (nie jest to literalny
//   string w bytecode, generowany dynamicznie przez ich command framework).
// - Test wymaga gracza z istniejacym kontem (CMI/MMOCore/MMOProfiles nie
//   tworza danych dla UUID ktore nigdy nie dolaczylo) — jesli konto nie
//   istnieje, scenariusz najpierw robi krotki bootstrap-join.
//
// Uzycie: node apps/acceptance/runner.js vault-economy-provider
//   [--user qa_bridge_econ] [--amount 250] [--domain items]

const { BotSession, sleep } = require('@mmorpg-bots/bot-core');
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
  const amount = Number(arg('amount', '250'));
  const domain = arg('domain', 'items');
  const uuid = offlineUuid(username);
  const client = createCompanionClient();

  let session;
  const disconnect = async () => {
    if (session) {
      try { await session.stop(); } catch { /* best effort */ }
    }
  };
  process.on('exit', () => { if (session) session.stop().catch(() => {}); });

  return runScenario({
    name: 'vault-economy-provider',
    steps: [
      {
        name: 'capabilities raportuje zarejestrowanego providera Economy',
        critical: true,
        run: async () => {
          const caps = await client.capabilities();
          assertTruthy(caps.integrations.vault.available, 'Vault plugin obecny');
          return { vault: caps.integrations.vault };
        }
      },
      {
        name: 'bot dolacza (bootstrap konta, jesli potrzeba)',
        critical: true,
        run: async () => {
          session = new BotSession({ username, reconnect: false, log: { info: () => {}, warn: () => {} } });
          await session.connect();
          const snap = await client.playerSnapshot(uuid);
          assertTruthy(snap.player.online, 'bot online wg companion-bridge');
          return { uuid, balanceStart: snap.integrations.vault.balance };
        }
      },
      {
        name: 'wykonanie nagrody (RCON op -> bot /money give -> deop) zmienia saldo',
        critical: true,
        run: async reporter => {
          const before = await client.playerSnapshot(uuid);
          const balanceBefore = before.integrations.vault.balance;

          await bridge.rcon(`op ${username}`, { domain });
          try {
            session.bot.chat(`/money give ${username} ${amount}`);
            await sleep(1500);
          } finally {
            await bridge.rcon(`deop ${username}`, { domain });
          }

          const after = await client.playerSnapshot(uuid);
          const balanceAfter = after.integrations.vault.balance;
          if (balanceAfter !== balanceBefore + amount) {
            throw new Error(`saldo nie wzroslo o ${amount}: przed=${balanceBefore}, po=${balanceAfter}`);
          }
          return { balanceBefore, balanceAfter, amount };
        }
      },
      {
        name: 'saldo pozostaje poprawne po rozlaczeniu (persystencja)',
        critical: true,
        run: async () => {
          const balanceOnline = (await client.playerSnapshot(uuid)).integrations.vault.balance;
          await disconnect();
          await sleep(500);
          const balanceOffline = (await client.playerSnapshot(uuid)).integrations.vault.balance;
          if (balanceOffline !== balanceOnline) {
            throw new Error(`saldo zmienilo sie po rozlaczeniu: online=${balanceOnline}, offline=${balanceOffline}`);
          }
          return { balanceOnline, balanceOffline };
        }
      }
    ]
  });
}

module.exports = { run };

if (require.main === module) {
  run(process.argv.slice(2)).then(result => {
    console.log(`Werdykt: ${result.verdict}`);
    process.exit(result.verdict === 'PASS' ? 0 : 1);
  });
}
