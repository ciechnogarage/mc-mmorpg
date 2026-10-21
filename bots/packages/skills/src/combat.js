'use strict';

// Gameplay-agent — walka melee (plan Etap 3).
// mineflayer-pvp jest porzucony (npm: "4 lata" bez update) — zamiast niego:
// zbliżenie przez mineflayer-pathfinder (utrzymywany, GoalFollow) + własna
// pętla bot.attack() z cooldownem. Podobnie mineflayer-statemachine jest
// porzucony — FSM to @mmorpg-bots/state-machine (plan sekcja E).
//
// UWAGA (memory: mc-death-trigger-real-kill): to jest walka NA PRAWDĘ —
// wynik 'killed' oznacza, że bot faktycznie zredukował HP celu do zera przez
// bot.attack(). Nigdy nie używaj RCON /kill do symulowania zabójstwa w testach
// korzystających z tego modułu.

const { pathfinder, Movements, goals } = require('mineflayer-pathfinder');
const { StateMachine } = require('@mmorpg-bots/state-machine');
const { ensurePathfinder } = require('./move');

const DEFAULT_HOSTILES = new Set([
  'zombie', 'husk', 'zombie_villager', 'drowned',
  'skeleton', 'stray', 'wither_skeleton',
  'spider', 'cave_spider', 'creeper', 'witch', 'phantom',
  'pillager', 'vindicator', 'evoker', 'ravager', 'vex'
]);

function findNearestHostile(bot, { radius = 24, predicate, hostileTypes = DEFAULT_HOSTILES } = {}) {
  const filter = predicate || (e => e.type === 'mob' && hostileTypes.has(e.name));
  let best = null;
  let bestDist = Infinity;
  for (const id in bot.entities) {
    const e = bot.entities[id];
    if (!e || e === bot.entity || !e.position || !filter(e)) continue;
    const d = e.position.distanceTo(bot.entity.position);
    if (d <= radius && d < bestDist) { best = e; bestDist = d; }
  }
  return best;
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

function targetGone(bot, target) {
  return target.isValid === false || bot.entities[target.id] === undefined;
}

// Pętla ataku pojedynczego celu. Zwraca dopiero gdy: cel zniknął (killed —
// jedyna droga w tym module to realne trafienia), bot musiał uciec
// (fleeHealth), cel zgubiony (poza zasięgiem/zniknął bez uśmiercenia — np.
// teleport) albo upłynął maxDurationMs.
async function meleeAttack(bot, target, opts = {}) {
  const {
    reach = 3,
    cooldownMs = 625, // wanilla attack speed ~0.625s; MMOItems/MMOCore mogą to zmieniać — nadpisz przy potrzebie
    fleeHealth = 6,
    maxDurationMs = 60000,
    log = console
  } = opts;

  ensurePathfinder(bot);
  bot.pathfinder.setMovements(new Movements(bot));
  bot.pathfinder.setGoal(new goals.GoalFollow(target, Math.max(1, reach - 1)));

  const startedAt = Date.now();
  let hits = 0;
  let result = 'timeout';

  try {
    while (Date.now() - startedAt < maxDurationMs) {
      if (targetGone(bot, target)) { result = 'killed'; break; }
      if (bot.health !== undefined && bot.health <= fleeHealth) { result = 'fled'; break; }
      const dist = target.position ? bot.entity.position.distanceTo(target.position) : null;
      if (dist === null) { result = 'lost-target'; break; }
      if (dist <= reach) {
        const aimY = target.position.y + (target.height ? target.height / 2 : 1);
        await bot.lookAt(target.position.offset(0, aimY - target.position.y, 0), true).catch(() => {});
        bot.attack(target);
        hits += 1;
        await sleep(cooldownMs);
      } else {
        await sleep(200);
      }
    }
  } finally {
    try { bot.pathfinder.setGoal(null); } catch {}
  }

  const durationMs = Date.now() - startedAt;
  log.info?.(`[combat] wynik=${result} hits=${hits} czas=${durationMs}ms cel=${target.name}`);
  return { result, hits, durationMs, targetName: target.name };
}

// FSM opakowanie (plan sekcja E: idle→task→verify→report). Nie steruje samą
// walką (to robi meleeAttack) — służy do raportowania fazy w QA/orchestracji.
function createCombatFSM({ log = console } = {}) {
  return new StateMachine({
    initial: 'idle',
    transitions: {
      idle: ['engage'],
      engage: ['attack', 'idle'],
      attack: ['flee', 'report', 'idle'],
      flee: ['report'],
      report: ['idle']
    },
    log
  });
}

module.exports = { findNearestHostile, meleeAttack, createCombatFSM, DEFAULT_HOSTILES };
