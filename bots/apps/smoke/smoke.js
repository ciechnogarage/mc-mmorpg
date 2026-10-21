'use strict';

// Smoke-test po restarcie (plan sekcja I):
// connect -> spawn -> RCON give MMOItem -> odczyt MMOITEMS_ITEM_ID z NBT
// -> otwarcie DeluxeMenu -> klik slotu -> TPS z serwera -> raport PASS/FAIL.
//
// Bot łączy się przez Velocity (localhost:25565); uprzywilejowane operacje
// (give) idą przez RCON, więc konto bota nie potrzebuje żadnych uprawnień.
//
// Użycie: node apps/smoke/smoke.js [--user NAZWA] [--domain items]
//         [--mmo-type SWORD --mmo-id LONG_SWORD] [--menu foundation --menu-title "nexus"]

const { BotSession, sleep } = require('@mmorpg-bots/bot-core');
const { installProcessGuards, recoverableInWindow } = require('@mmorpg-bots/safe-protocol');
const skills = require('@mmorpg-bots/skills');
const { CommandBus } = require('@mmorpg-bots/command-bus');
const bridge = require('@mmorpg-bots/mmo-bridge');
const { runScenario, assertTruthy, assertEquals } = require('@mmorpg-bots/qa');

function arg(name, fallback) {
  const idx = process.argv.indexOf(`--${name}`);
  return idx !== -1 && process.argv[idx + 1] ? process.argv[idx + 1] : fallback;
}

const USER = arg('user', `SMK${Date.now().toString().slice(-8)}`).slice(0, 16);
const DOMAIN = arg('domain', process.env.MC_DOCKER_DOMAIN || 'items');
const MMO_TYPE = arg('mmo-type', 'SWORD');
const MMO_ID = arg('mmo-id', 'LONG_SWORD');
const MENU_CMD = arg('menu', 'foundation');
const MENU_TITLE = arg('menu-title', 'nexus');

installProcessGuards(console);

async function main() {
  const session = new BotSession({ username: USER, reconnect: false });
  const bus = new CommandBus();
  let bot = null;
  let chatWatch = null;

  // Skille jako komendy magistrali — te same wywołania są dostępne dla
  // skryptu (tu), chat-controllera i adaptera LLM/MCP.
  bus.register('read-mmo-item', async ({ mmoId }) => {
    const item = await skills.items.waitForInventoryMMOItem(bot, mmoId, 10000);
    return {
      id: skills.items.getMMOItemId(item),
      type: skills.items.getMMOItemType(item),
      stats: Object.keys(skills.items.getMMOStats(item) || {})
    };
  });
  bus.register('open-menu', async ({ command, title }) => {
    const window = await skills.gui.openMenuByCommand(bot, command, title);
    return window;
  });

  const { verdict, file } = await runScenario({
    name: `smoke_${DOMAIN}_${USER}`,
    // korelacja: recoverable drop pakietu istotnego dla asercji => BLOCKED
    interference: (startMs, endMs, packets) =>
      recoverableInWindow(session.protocolStats || { events: [] }, startMs, endMs, packets),
    steps: [
      {
        name: 'connect+spawn (Velocity 25565)',
        critical: true,
        run: async () => {
          bot = await session.connect();
          chatWatch = skills.chat.watchChat(bot);
          await sleep(2000); // MMOProfiles/login flow
          return { username: bot.username, dimension: bot.game?.dimension, position: bot.entity?.position?.toString() };
        }
      },
      {
        name: 'rcon give MMOItem',
        critical: true,
        run: async () => {
          const out = await bridge.rcon(`mmoitems give ${MMO_TYPE} ${MMO_ID} ${USER}`, { domain: DOMAIN });
          if (/error|unknown|nie znaleziono/i.test(out)) throw new Error(`rcon: ${out.slice(0, 200)}`);
          return { rcon: out.slice(0, 160) };
        }
      },
      {
        name: 'kontrakt prismarine-item (custom_data w components)',
        critical: true,
        relevantPackets: ['window_items', 'set_slot'],
        run: async () => {
          const item = await skills.items.waitForInventoryMMOItem(bot, MMO_ID, 10000).catch(() => bot.inventory.items()[0]);
          return skills.items.assertComponentContract(item);
        }
      },
      {
        name: 'NBT: MMOITEMS_ITEM_ID z custom_data',
        critical: true,
        relevantPackets: ['window_items', 'set_slot'],
        run: async () => {
          const result = await bus.dispatch({ name: 'read-mmo-item', args: { mmoId: MMO_ID }, source: 'script' });
          assertEquals(result.id, MMO_ID, 'MMOITEMS_ITEM_ID');
          return result;
        }
      },
      {
        name: 'DeluxeMenus: otwarcie + stabilizacja + klik',
        relevantPackets: ['open_window', 'window_items', 'set_slot'],
        run: async () => {
          const window = await bus.dispatch({ name: 'open-menu', args: { command: MENU_CMD, title: MENU_TITLE }, source: 'script' });
          const filled = window.slots.slice(0, window.inventoryStart ?? 27).filter(Boolean).length;
          assertTruthy(filled > 0, 'liczba wypełnionych slotów menu');
          const firstSlot = window.slots.findIndex(Boolean);
          await skills.gui.clickSlot(bot, window, firstSlot);
          return { title: skills.text.getWindowTitle(window), filledSlots: filled, clickedSlot: firstSlot };
        }
      },
      {
        name: 'TPS/MSPT z serwera (RCON)',
        run: async () => {
          const { tps, raw } = await bridge.getTps({ domain: DOMAIN });
          assertTruthy(tps.length, 'odczyt TPS');
          return { tps, raw: raw.slice(0, 120) };
        }
      },
      {
        name: 'chat: brak wzorców błędów serwera',
        run: async () => {
          const errors = chatWatch?.errors ?? [];
          if (errors.length) throw new Error(`błędy w chacie: ${errors.map(e => e.text).join(' | ').slice(0, 300)}`);
          return { messagesSeen: chatWatch?.messages.length ?? 0 };
        }
      },
      {
        name: 'protokół: statystyki PartialReadError',
        run: async () => {
          const stats = session.protocolStats || { total: 0 };
          // partial-read nie failuje smoke — raportujemy skalę problemu
          return { total: stats.total, byKind: stats.byKind, byPacket: stats.byPacket };
        }
      }
    ]
  });

  await session.stop();
  console.log(`\nWerdykt: ${verdict}\nRaport: ${file}`);
  process.exit(verdict === 'PASS' ? 0 : 1);
}

main().catch(err => {
  console.error('smoke fatal:', err);
  process.exit(1);
});
