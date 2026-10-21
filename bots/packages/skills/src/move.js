'use strict';

// Ruch przez mineflayer-pathfinder (komponent ryzykowny — plan sekcja A/E:
// wersja przypięta, awarie pathfindera nie mogą wywracać scenariusza).

const { pathfinder, Movements, goals } = require('mineflayer-pathfinder');

function ensurePathfinder(bot) {
  if (!bot.pathfinder) bot.loadPlugin(pathfinder);
  return bot.pathfinder;
}

async function gotoXYZ(bot, x, y, z, range = 1, timeout = 30000) {
  ensurePathfinder(bot);
  bot.pathfinder.setMovements(new Movements(bot));
  const goal = new goals.GoalNear(x, y, z, range);
  return Promise.race([
    bot.pathfinder.goto(goal),
    new Promise((_, reject) => setTimeout(() => {
      try { bot.pathfinder.stop(); } catch {}
      reject(new Error('pathfinder timeout'));
    }, timeout))
  ]);
}

module.exports = { ensurePathfinder, gotoXYZ };
