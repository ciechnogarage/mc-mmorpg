// @ts-check
'use strict';

// Command bus (plan sekcja E): trzy źródła sterowania (skrypt, chat in-game,
// LLM/MCP) emitują te same komendy do jednej magistrali. Handlery to skille.

/**
 * Kontrakt komendy na magistrali — jedyna powierzchnia, którą widzą wszystkie
 * trzy źródła sterowania. Zmiany tutaj łamią skrypty, chat-controller i MCP.
 * @typedef {Object} Command
 * @property {string} name
 * @property {Object<string, any>} [args]
 * @property {string} [source] - 'script' | 'chat:<nick>' | 'mcp:<tool>'
 */

/** @typedef {(args: Object<string, any>, ctx: {source: string}) => Promise<any>} CommandHandler */

class CommandBus {
  constructor({ log = console } = {}) {
    this.log = log;
    this.handlers = new Map();
    this.middleware = [];
    this.history = [];
  }

  /** @param {string} name @param {CommandHandler} handler */
  register(name, handler, { description = '' } = {}) {
    if (this.handlers.has(name)) throw new Error(`komenda już zarejestrowana: ${name}`);
    this.handlers.set(name, { handler, description });
    return this;
  }

  use(fn) {
    this.middleware.push(fn);
    return this;
  }

  list() {
    return [...this.handlers.entries()].map(([name, { description }]) => ({ name, description }));
  }

  /** @param {Command} command */
  async dispatch(command) {
    const { name, args = {}, source = 'script' } = command;
    const entry = this.handlers.get(name);
    const record = { name, args, source, at: new Date().toISOString(), status: 'pending' };
    this.history.push(record);
    if (this.history.length > 1000) this.history.shift();
    if (!entry) {
      record.status = 'unknown-command';
      throw new Error(`nieznana komenda: ${name}`);
    }
    for (const mw of this.middleware) {
      const verdict = await mw(command);
      if (verdict === false) {
        record.status = 'rejected';
        throw new Error(`komenda odrzucona przez middleware: ${name} (source=${source})`);
      }
    }
    try {
      const result = await entry.handler(args, { source });
      record.status = 'ok';
      return result;
    } catch (error) {
      record.status = `error: ${error.message}`;
      throw error;
    }
  }
}

// Parser komend z chatu in-game: "!bot <komenda> <json-args?>" od zaufanych nicków.
function attachChatController(bot, bus, { prefix = '!bot ', allowedUsers = [], log = console } = {}) {
  bot.on('chat', async (username, message) => {
    if (username === bot.username) return;
    if (!message.startsWith(prefix)) return;
    if (allowedUsers.length && !allowedUsers.includes(username)) {
      log.warn(`[chat-controller] odrzucono komendę od ${username}`);
      return;
    }
    const body = message.slice(prefix.length).trim();
    const spaceIdx = body.indexOf(' ');
    const name = spaceIdx === -1 ? body : body.slice(0, spaceIdx);
    let args = {};
    if (spaceIdx !== -1) {
      try { args = JSON.parse(body.slice(spaceIdx + 1)); } catch { args = { raw: body.slice(spaceIdx + 1) }; }
    }
    try {
      await bus.dispatch({ name, args, source: `chat:${username}` });
      bot.chat(`[bot] ok: ${name}`);
    } catch (error) {
      bot.chat(`[bot] błąd: ${error.message.slice(0, 80)}`);
    }
  });
}

module.exports = { CommandBus, attachChatController };
