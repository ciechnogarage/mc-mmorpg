'use strict';

// Orchestrator floty (plan sekcja F): każdy bot = OSOBNY proces
// (mineflayer #2749 — wiele botów w jednym procesie jest zawodne).
// Staggered join (joinDelay), auto-restart z limitem, zbiorczy status.
//
// Użycie: node apps/orchestrator/orchestrator.js --count 5 \
//           --script apps/orchestrator/worker-walk.js --join-delay 750

const { spawn } = require('child_process');
const path = require('path');

function arg(name, fallback) {
  const idx = process.argv.indexOf(`--${name}`);
  return idx !== -1 && process.argv[idx + 1] ? process.argv[idx + 1] : fallback;
}

const COUNT = Number(arg('count', 3));
const SCRIPT = path.resolve(arg('script', path.join(__dirname, 'worker-walk.js')));
const JOIN_DELAY = Number(arg('join-delay', 750));
const MAX_RESTARTS = Number(arg('max-restarts', 3));
const PREFIX = arg('prefix', 'LOAD');

const fleet = new Map();

function spawnBot(index, restarts = 0) {
  const username = `${PREFIX}${String(index).padStart(3, '0')}`;
  const child = spawn(process.execPath, [SCRIPT], {
    env: { ...process.env, BOT_USERNAME: username, BOT_INDEX: String(index) },
    stdio: ['ignore', 'pipe', 'pipe']
  });
  fleet.set(index, { username, pid: child.pid, restarts, startedAt: Date.now() });
  const tag = `[${username}]`;
  child.stdout.on('data', d => process.stdout.write(`${tag} ${d}`));
  child.stderr.on('data', d => process.stderr.write(`${tag}! ${d}`));
  child.on('exit', (code, signal) => {
    console.log(`${tag} exit code=${code} signal=${signal}`);
    fleet.delete(index);
    if (!shuttingDown && code !== 0 && restarts < MAX_RESTARTS) {
      console.log(`${tag} restart ${restarts + 1}/${MAX_RESTARTS}`);
      setTimeout(() => spawnBot(index, restarts + 1), 3000);
    }
  });
}

let shuttingDown = false;
process.on('SIGINT', () => {
  shuttingDown = true;
  console.log('\n[orchestrator] SIGINT — zamykam flotę');
  for (const [, info] of fleet) {
    try { process.kill(info.pid, 'SIGTERM'); } catch {}
  }
  setTimeout(() => process.exit(0), 2000);
});

(async () => {
  console.log(`[orchestrator] flota ${COUNT} botów, skrypt=${SCRIPT}, joinDelay=${JOIN_DELAY}ms`);
  for (let i = 1; i <= COUNT; i++) {
    spawnBot(i);
    await new Promise(r => setTimeout(r, JOIN_DELAY));
  }
  setInterval(() => {
    const alive = [...fleet.values()].map(f => f.username).join(', ');
    console.log(`[orchestrator] żywych: ${fleet.size}/${COUNT} (${alive})`);
  }, 30000);
})();
