'use strict';

// QA-scenariusz Etapu 3: dowód, że pętla walki (@mmorpg-bots/skills.combat)
// faktycznie zabija w realnej walce, nie administracyjnie.
// RCON jest użyty WYŁĄCZNIE do setupu (give miecza, summon testowego moba —
// oba nieuprzywilejowane dla bota, jak w innych skryptach QA); sam kill musi
// wyjść z bot.attack() w meleeAttack. Zgodnie z memory mc-death-trigger-real-kill
// ten skrypt NIGDY nie używa RCON /kill do wymuszenia wyniku.
//
// Użycie: node apps/gameplay/combat-check.js --domain world [--mob zombie] [--timeout 45000]

const { BotSession, sleep } = require('@mmorpg-bots/bot-core');
const { installProcessGuards } = require('@mmorpg-bots/safe-protocol');
const bridge = require('@mmorpg-bots/mmo-bridge');
const { createReporter } = require('@mmorpg-bots/qa');
const { findNearestHostile, meleeAttack, createCombatFSM } = require('@mmorpg-bots/skills').combat;

function arg(name, fallback) {
  const idx = process.argv.indexOf(`--${name}`);
  return idx !== -1 && process.argv[idx + 1] ? process.argv[idx + 1] : fallback;
}

const DOMAIN = arg('domain', 'world');
const MOB = arg('mob', 'zombie');
const TIMEOUT_MS = Number(arg('timeout', 45000));
const USER = arg('user', `CBT${Date.now().toString().slice(-8)}`).slice(0, 16);

installProcessGuards(console, err => {
  console.error('[combat-check] FATAL:', err.message);
  process.exit(2);
});

async function main() {
  const reporter = createReporter({ name: `combat-check_${MOB}_${DOMAIN}` });
  const fsm = createCombatFSM({ log: console });

  const session = new BotSession({ username: USER, maxReconnects: 3 });
  const bot = await session.connect();
  reporter.step('connect', 'OK', { user: USER, domain: DOMAIN, pos: bot.entity.position.toString() });

  const who = await bridge.safeRcon('minecraft:list', { domain: DOMAIN });
  if (!String(who).includes(USER)) {
    reporter.blocker(`bot nie wylądował na domenie ${DOMAIN} (list: ${String(who).slice(0, 120)})`);
    const file = reporter.finalize('BLOCKED');
    console.error(`[combat-check] BLOCKED\nRaport: ${file}`);
    await session.stop();
    process.exit(3);
  }
  reporter.step('backend-attribution', 'OK', { domain: DOMAIN });

  // setup (RCON, nieuprzywilejowane dla bota): miecz + testowy mob 3 bloki przed botem
  const giveOut = await bridge.safeRcon(`give ${USER} minecraft:diamond_sword 1`, { domain: DOMAIN });
  reporter.step('setup: give diamond_sword', String(giveOut).startsWith('ERR') ? 'FAIL' : 'OK', { out: String(giveOut).slice(0, 150) });

  const pos = bot.entity.position;
  const sx = Math.floor(pos.x) + 3;
  const sy = Math.floor(pos.y);
  const sz = Math.floor(pos.z);
  const summonOut = await bridge.safeRcon(`summon minecraft:${MOB} ${sx} ${sy} ${sz} {Silent:1b}`, { domain: DOMAIN });
  reporter.step('setup: summon test mob', String(summonOut).startsWith('ERR') ? 'FAIL' : 'OK', { mob: MOB, at: `${sx} ${sy} ${sz}`, out: String(summonOut).slice(0, 150) });

  await sleep(2000); // pozwól klientowi zsynchronizować metadane encji
  await bot.equip(bot.inventory.items().find(i => i.name.includes('sword')) || null, 'hand').catch(() => {});

  fsm.go('engage');
  const target = findNearestHostile(bot, { radius: 12, predicate: e => e.name === MOB });
  if (!target) {
    reporter.blocker(`nie znaleziono testowego moba '${MOB}' w promieniu 12 bloków po summon`);
    const file = reporter.finalize('BLOCKED');
    console.error(`[combat-check] BLOCKED — brak targetu\nRaport: ${file}`);
    await session.stop();
    process.exit(3);
  }
  reporter.step('target znaleziony', 'OK', { name: target.name, id: target.id, dist: target.position.distanceTo(bot.entity.position).toFixed(1) });

  fsm.go('attack');
  const outcome = await meleeAttack(bot, target, { maxDurationMs: TIMEOUT_MS, log: console });
  if (outcome.result === 'fled') {
    fsm.go('flee', outcome);
    fsm.go('report', outcome);
  } else {
    fsm.go('report', outcome);
  }

  const killed = outcome.result === 'killed';
  reporter.step('walka: realny kill', killed ? 'OK' : 'FAIL', outcome);
  reporter.state.combatOutcome = outcome;
  reporter.state.hpAfter = bot.health;

  if (!killed) reporter.blocker(`brak realnego zabójstwa — wynik pętli: ${outcome.result} (hits=${outcome.hits})`);
  const file = reporter.finalize(killed ? 'PASS' : 'FAIL');
  await session.stop();
  console.log(`\n[combat-check] Werdykt: ${killed ? 'PASS' : 'FAIL'}\nRaport: ${file}`);
  process.exit(killed ? 0 : 1);
}

main().catch(err => {
  console.error('[combat-check] fatal:', err);
  process.exit(2);
});
