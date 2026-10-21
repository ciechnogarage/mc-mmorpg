'use strict';
const { scenariusz, czekaj, sleep, plain } = require('/bot/lib');
const assertWorld = bot => String(bot.game.dimension || '').includes('arena');
async function enter(rcon, bot) {
  await rcon(`arena wejdz ${bot.username}`);
  return !!(await czekaj(() => assertWorld(bot) ? true : null, 10000, 250));
}
async function pos(rcon, bot, x, y, z) {
  await rcon(`minecraft:execute in minecraft:arena run minecraft:teleport ${bot.username} ${x} ${y} ${z}`);
  await sleep(500);
}
async function attack(source, target) {
  const entity = source.players[target.username]?.entity;
  if (!entity) return false;
  source.attack(entity);
  await sleep(600);
  return true;
}
async function pair(polacz, rcon) {
  const a = await polacz({nick: 'ArenaA'});
  const b = await polacz({nick: 'ArenaB'});
  await enter(rcon, a); await enter(rcon, b);
  return {a, b};
}
async function exp(rcon, bot) { return String(await rcon(`papi parse ${bot.username} %mmocore_experience%`)).trim(); }
module.exports = { scenariusz, czekaj, sleep, plain, assertWorld, enter, pos, attack, pair, exp };
