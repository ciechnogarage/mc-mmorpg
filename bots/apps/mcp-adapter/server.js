'use strict';

// Etap 4 (plan sekcja E): cienki serwer MCP owijający bot-core + skills.
// Świadomie NIE wstrzykuje kodu yuniko/Mindcraft — to jest własny, mały
// serwer MCP dla WŁASNEGO konta bota, uruchamiany jako osobny proces przez
// klienta MCP (Claude Desktop/Code — stdio, klient sam go spawnuje).
// Każdy proces = jeden bot = jedno konto (jak w orchestratorze/loadtest).
//
// Bezpieczeństwo (plan sekcja G): to narzędzie daje klientowi LLM REALNĄ
// kontrolę nad kontem w grze (ruch, walka, chat, GUI). Uruchamiaj wyłącznie
// na stagingu, z kontem na whiteliście bez OP — nigdy na produkcji z danymi
// graczy.

const { McpServer } = require('@modelcontextprotocol/sdk/server/mcp.js');
const { StdioServerTransport } = require('@modelcontextprotocol/sdk/server/stdio.js');
const { z } = require('zod');

const { BotSession, sleep } = require('@mmorpg-bots/bot-core');
const { installProcessGuards } = require('@mmorpg-bots/safe-protocol');
const skills = require('@mmorpg-bots/skills');

let session = null;
let bot = null;

function requireBot() {
  if (!bot) throw new Error('brak połączonego bota — najpierw wywołaj bot_connect');
  return bot;
}

function text(obj) {
  return { content: [{ type: 'text', text: JSON.stringify(obj) }] };
}

function statusSnapshot() {
  if (!bot || !bot.entity) return { connected: false };
  return {
    connected: true,
    username: bot.username,
    pos: bot.entity.position.toString(),
    health: bot.health,
    food: bot.food,
    heldItem: bot.heldItem?.name || null,
    windowTitle: bot.currentWindow ? skills.text.getWindowTitle(bot.currentWindow) : null
  };
}

const server = new McpServer({ name: 'mmorpg-bots-mcp-adapter', version: '0.1.0' });

server.registerTool(
  'bot_connect',
  {
    title: 'Połącz bota',
    description: 'Łączy nowego bota (przez Velocity) i czeka na spawn. Jeden bot na proces — kolejny bot_connect po już połączonym zwraca błąd.',
    inputSchema: { username: z.string().max(16) }
  },
  async ({ username }) => {
    if (bot) throw new Error(`bot już połączony jako ${bot.username} — ten proces obsługuje jedno konto`);
    session = new BotSession({ username, maxReconnects: 5 });
    bot = await session.connect();
    return text(statusSnapshot());
  }
);

server.registerTool(
  'bot_disconnect',
  { description: 'Rozłącza bota i kończy sesję.', inputSchema: {} },
  async () => {
    if (session) await session.stop();
    session = null;
    bot = null;
    return text({ disconnected: true });
  }
);

server.registerTool(
  'bot_status',
  { description: 'Zwraca pozycję, HP, głód, trzymany item i tytuł otwartego okna.', inputSchema: {} },
  async () => text(statusSnapshot())
);

server.registerTool(
  'bot_goto',
  {
    description: 'Idzie do współrzędnych XYZ przez mineflayer-pathfinder (GoalNear).',
    inputSchema: { x: z.number(), y: z.number(), z: z.number(), range: z.number().default(1), timeoutMs: z.number().default(30000) }
  },
  async ({ x, y, z: zc, range, timeoutMs }) => {
    const b = requireBot();
    await skills.move.gotoXYZ(b, x, y, zc, range, timeoutMs);
    return text({ arrived: true, pos: b.entity.position.toString() });
  }
);

server.registerTool(
  'bot_attack_nearest',
  {
    title: 'Atak — najbliższy wrogi mob',
    description: 'Znajduje najbliższego hostile moba w promieniu i walczy realnie (bot.attack w pętli — nigdy administracyjnie). Zwraca killed/fled/lost-target/timeout.',
    inputSchema: { radius: z.number().default(16), mob: z.string().optional(), timeoutMs: z.number().default(30000) }
  },
  async ({ radius, mob, timeoutMs }) => {
    const b = requireBot();
    const predicate = mob ? e => e.name === mob : undefined;
    const target = skills.combat.findNearestHostile(b, { radius, predicate });
    if (!target) return text({ result: 'no-target', radius, mob: mob || 'any' });
    const outcome = await skills.combat.meleeAttack(b, target, { maxDurationMs: timeoutMs });
    return text(outcome);
  }
);

server.registerTool(
  'bot_chat',
  { description: 'Wysyła wiadomość na czat.', inputSchema: { message: z.string().max(256) } },
  async ({ message }) => {
    const b = requireBot();
    b.chat(message);
    return text({ sent: true });
  }
);

server.registerTool(
  'bot_wait_for_message',
  {
    description: 'Czeka na wiadomość czatu pasującą do regexu (np. potwierdzenie questu/transakcji).',
    inputSchema: { pattern: z.string(), timeoutMs: z.number().default(10000) }
  },
  async ({ pattern, timeoutMs }) => {
    const b = requireBot();
    const msg = await skills.chat.waitForMessage(b, new RegExp(pattern), timeoutMs);
    return text({ matched: msg });
  }
);

server.registerTool(
  'bot_read_held_item',
  {
    description: 'Odczytuje MMOITEMS_ITEM_ID/TYPE/staty z NBT trzymanego itemu (custom_data).',
    inputSchema: {}
  },
  async () => {
    const b = requireBot();
    const item = b.heldItem;
    if (!item) return text({ held: false });
    return text({
      held: true,
      name: item.name,
      mmoId: skills.items.getMMOItemId(item),
      mmoType: skills.items.getMMOItemType(item),
      mmoStats: skills.items.getMMOStats(item)
    });
  }
);

server.registerTool(
  'bot_open_menu',
  {
    description: 'Wysyła komendę (np. "/warp shop") i czeka na okno GUI o pasującym (fragmencie) tytule, aż się ustabilizuje (DeluxeMenus przerysowuje placeholdery).',
    inputSchema: { command: z.string(), titleMatch: z.string(), timeoutMs: z.number().default(10000) }
  },
  async ({ command, titleMatch, timeoutMs }) => {
    const b = requireBot();
    const win = await skills.gui.openMenuByCommand(b, command, titleMatch, timeoutMs);
    return text({ title: skills.text.getWindowTitle(win), slots: win.slots.length });
  }
);

server.registerTool(
  'bot_find_slot_by_name',
  {
    description: 'Szuka slotu w aktualnie otwartym oknie po fragmencie nazwy/lore itemu. Zwraca {slot,item} albo null.',
    inputSchema: { nameFragment: z.string() }
  },
  async ({ nameFragment }) => {
    const b = requireBot();
    if (!b.currentWindow) throw new Error('brak otwartego okna — najpierw bot_open_menu');
    const found = skills.gui.findSlotByName(b.currentWindow, nameFragment);
    return text(found ? { slot: found.slot, itemName: found.item?.name } : { slot: null });
  }
);

server.registerTool(
  'bot_click_slot',
  {
    description: 'Klika slot w aktualnie otwartym oknie GUI (DeluxeMenus itp., z obejściem braku potwierdzeń transakcji). Zwraca stan okna po kliku (null jeśli się zamknęło).',
    inputSchema: { slot: z.number(), mouseButton: z.number().default(0), mode: z.number().default(0) }
  },
  async ({ slot, mouseButton, mode }) => {
    const b = requireBot();
    if (!b.currentWindow) throw new Error('brak otwartego okna — najpierw bot_open_menu');
    const after = await skills.gui.clickSlot(b, b.currentWindow, slot, { mouseButton, mode });
    return text({ clicked: slot, windowStillOpen: !!after, windowTitle: after ? skills.text.getWindowTitle(after) : null });
  }
);

async function main() {
  installProcessGuards(console, err => {
    console.error('[mcp-adapter] FATAL:', err.message);
    process.exit(2);
  });
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('[mcp-adapter] gotowy na stdio (jeden bot na proces)');
}

main().catch(err => {
  console.error('[mcp-adapter] fatal:', err);
  process.exit(1);
});
