'use strict';

// BotCore: opakowanie mineflayer z bezpiecznym parsowaniem, auto-reconnect
// i zdarzeniami sesji. Zawsze łączy się przez Velocity (nigdy bezpośrednio
// do backendu — wymagane jest jawne wskazanie celu runtime).

const mineflayer = require('mineflayer');
const { attachSafeProtocol } = require('@mmorpg-bots/safe-protocol');

const DEFAULTS = {
  host: process.env.MC_HOST || '127.0.0.1',
  port: Number(process.env.MC_PORT || 25565),
  version: process.env.MC_VERSION || '1.21.11',
  auth: 'offline',
  viewDistance: 'tiny', // plan D: mniejsza ekspozycja na problematyczne entity metadata
  hideErrors: false
};

class BotSession {
  constructor(opts = {}) {
    this.opts = { ...DEFAULTS, ...opts };
    if (!this.opts.username) throw new Error('BotSession: username wymagany');
    this.log = this.opts.log || console;
    this.bot = null;
    this.protocolStats = null;
    this.stopped = false;
    this.reconnect = {
      enabled: this.opts.reconnect !== false,
      attempts: 0,      // bieżąca seria (zeruje się po sukcesie — steruje limitem)
      total: 0,         // kumulatywnie od startu sesji (kryteria QA/bench)
      max: this.opts.maxReconnects ?? 5,
      delayMs: this.opts.reconnectDelayMs ?? 5000
    };
  }

  async connect() {
    this.stopped = false;
    return this._connectOnce();
  }

  async _connectOnce() {
    const { username, host, port, version, auth, viewDistance, hideErrors } = this.opts;
    this.log.info(`[bot-core] ${username} -> ${host}:${port} (${version})`);
    const bot = mineflayer.createBot({ username, host, port, version, auth, viewDistance, hideErrors });
    this.bot = bot;
    this.protocolStats = attachSafeProtocol(bot, this.log);

    bot.on('kicked', reason => {
      this.log.warn(`[bot-core] ${username} kicked: ${JSON.stringify(reason).slice(0, 300)}`);
    });
    bot.on('end', reason => {
      this.log.warn(`[bot-core] ${username} end: ${reason}`);
      if (!this.stopped && this.reconnect.enabled && this.reconnect.attempts < this.reconnect.max) {
        this.reconnect.attempts += 1;
        this.reconnect.total += 1;
        this.log.info(`[bot-core] reconnect ${this.reconnect.attempts}/${this.reconnect.max} za ${this.reconnect.delayMs}ms`);
        setTimeout(() => { if (!this.stopped) this._connectOnce().catch(() => {}); }, this.reconnect.delayMs);
      }
    });

    await this.waitForSpawn();
    this.reconnect.attempts = 0;
    return bot;
  }

  waitForSpawn(timeout = this.opts.spawnTimeoutMs || 30000) {
    const bot = this.bot;
    if (bot.entity && bot.game && bot.game.dimension !== undefined && bot._spawned) return Promise.resolve();
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        bot.removeListener('spawn', onSpawn);
        reject(new Error('spawn timeout'));
      }, timeout);
      function onSpawn() {
        bot._spawned = true;
        clearTimeout(timer);
        resolve();
      }
      bot.once('spawn', onSpawn);
    });
  }

  async stop() {
    this.stopped = true;
    if (this.bot) {
      try { this.bot.quit(); } catch {}
      this.bot = null;
    }
  }
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function waitFor(fn, timeout = 30000, step = 300) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    const value = await fn();
    if (value) return value;
    await sleep(step);
  }
  throw new Error('waitFor timeout');
}

module.exports = { BotSession, sleep, waitFor, DEFAULTS };
