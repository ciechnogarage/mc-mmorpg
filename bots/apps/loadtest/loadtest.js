'use strict';

// Load-test floty (plan Etap 2 / sekcje F+I): N botów w osobnych procesach
// (worker-walk.js, jak orchestrator), skorelowane z TPS mierzonym po stronie
// serwera (RCON /tps — nie ma jeszcze companion-pluginu/spark) i z detekcją
// stalli ładowania chunków (plan sekcja D, objaw #3 — regres PacketEvents
// 1.21.10 build 113: bot się porusza, ale liczba chunków przestaje rosnąć).
//
// Użycie:
//   node apps/loadtest/loadtest.js --count 5 --domain world --minutes 5 \
//     --join-delay 750 --tps-interval 30 --tps-fail 15 --tps-warn 18.5
//
// Kryteria werdyktu (zdefiniowane wprost, jak w bench.js):
//  1. TPS: żadna próbka < tps-fail => FAIL; próbka < tps-warn => ostrzeżenie (raport, nie FAIL).
//  2. Flota: żaden bot nie wyczerpał max-restarts (permanentny ubytek floty) => FAIL.
//  3. Chunk-stall: bot poruszający się (moved>=2 przez 3 kolejne próbki ~10s),
//     u którego liczba chunków nie rośnie w tym oknie => zdarzenie stall.
//     >= stall-threshold takich zdarzeń u JEDNEGO bota => FAIL (realny regres,
//     nie szum); mniej => raportowane informacyjnie, werdykt nie spada.

const { spawn } = require('child_process');
const path = require('path');
const bridge = require('@mmorpg-bots/mmo-bridge');
const { createReporter } = require('@mmorpg-bots/qa');

function arg(name, fallback) {
  const idx = process.argv.indexOf(`--${name}`);
  return idx !== -1 && process.argv[idx + 1] ? process.argv[idx + 1] : fallback;
}

const COUNT = Number(arg('count', 5));
const DOMAIN = arg('domain', 'world');
const MINUTES = Number(arg('minutes', 5));
// domyślnie > velocity login-ratelimit=3000ms (velocity.toml) — poniżej tego
// progu boty dostają kick "logging in too fast" i restartują się fałszywie
// (zweryfikowane empirycznie: 700ms => 2/3 botów kicked na starcie).
const JOIN_DELAY = Number(arg('join-delay', 3200));
const TPS_INTERVAL_MS = Number(arg('tps-interval', 30)) * 1000;
const TPS_FAIL = Number(arg('tps-fail', 15));
const TPS_WARN = Number(arg('tps-warn', 18.5));
const MAX_RESTARTS = Number(arg('max-restarts', 2));
const STALL_THRESHOLD = Number(arg('stall-threshold', 3));
const PREFIX = arg('prefix', 'LOAD');
const WORKER_SCRIPT = path.resolve(__dirname, '..', 'orchestrator', 'worker-walk.js');

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function main() {
  const reporter = createReporter({ name: `loadtest_${DOMAIN}_n${COUNT}_${MINUTES}m` });

  const preTps = await bridge.getTps({ domain: DOMAIN });
  reporter.step('pre-check: backend odpowiada na RCON', preTps.tps.length ? 'OK' : 'FAIL', preTps);
  if (!preTps.tps.length) {
    reporter.blocker(`domena ${DOMAIN} nie odpowiada na /tps przez RCON — backend nieaktywny?`);
    const file = reporter.finalize('BLOCKED');
    console.error(`[loadtest] BLOCKED — backend ${DOMAIN} nieosiągalny\nRaport: ${file}`);
    process.exit(3);
  }

  const fleet = new Map(); // index -> { username, pid, restarts, chunkHist: [{t,chunks,moved}] }
  const stallEvents = []; // { bot, at, chunks, moved }
  let shuttingDown = false;

  const chunkLine = /pos=\S+ moved=([\d.]+) chunks=(\d+|\?)/;

  function trackStall(index, moved, chunks) {
    if (chunks === '?') return;
    const info = fleet.get(index);
    if (!info) return;
    info.chunkHist.push({ t: Date.now(), moved: Number(moved), chunks: Number(chunks) });
    if (info.chunkHist.length > 3) info.chunkHist.shift();
    if (info.chunkHist.length === 3) {
      const [a, b, c] = info.chunkHist;
      const movingConsistently = a.moved >= 2 && b.moved >= 2 && c.moved >= 2;
      const chunksStalled = c.chunks <= a.chunks;
      if (movingConsistently && chunksStalled) {
        stallEvents.push({ bot: info.username, at: new Date().toISOString(), chunks: c.chunks, moved: c.moved });
      }
    }
  }

  function spawnBot(index, restarts = 0) {
    const username = `${PREFIX}${String(index).padStart(3, '0')}`;
    const child = spawn(process.execPath, [WORKER_SCRIPT], {
      env: { ...process.env, BOT_USERNAME: username, BOT_INDEX: String(index) },
      stdio: ['ignore', 'pipe', 'pipe']
    });
    fleet.set(index, { username, pid: child.pid, restarts, startedAt: Date.now(), chunkHist: [], exhausted: false });
    const tag = `[${username}]`;
    let buf = '';
    child.stdout.on('data', d => {
      buf += d;
      const lines = buf.split('\n');
      buf = lines.pop();
      for (const line of lines) {
        process.stdout.write(`${tag} ${line}\n`);
        const m = line.match(chunkLine);
        if (m) trackStall(index, m[1], m[2]);
      }
    });
    child.stderr.on('data', d => process.stderr.write(`${tag}! ${d}`));
    child.on('exit', (code, signal) => {
      console.log(`${tag} exit code=${code} signal=${signal}`);
      const info = fleet.get(index);
      fleet.delete(index);
      if (!shuttingDown && code !== 0) {
        if (restarts < MAX_RESTARTS) {
          console.log(`${tag} restart ${restarts + 1}/${MAX_RESTARTS}`);
          setTimeout(() => spawnBot(index, restarts + 1), 3000);
        } else if (info) {
          info.exhausted = true;
          fleet.set(index, info); // zostaw ślad wyczerpania w raporcie
        }
      }
    });
  }

  console.log(`[loadtest] flota ${COUNT} botów -> domena=${DOMAIN}, minuty=${MINUTES}, joinDelay=${JOIN_DELAY}ms`);
  for (let i = 1; i <= COUNT; i++) {
    spawnBot(i);
    await sleep(JOIN_DELAY);
  }

  // atrybucja: sprawdź, że przynajmniej część floty faktycznie wylądowała na DOMAIN
  await sleep(5000);
  const who = await bridge.safeRcon('minecraft:list', { domain: DOMAIN });
  const attributed = [...Array(COUNT)].filter((_, i) => String(who).includes(`${PREFIX}${String(i + 1).padStart(3, '0')}`)).length;
  reporter.step('backend-attribution', attributed > 0 ? 'OK' : 'FAIL', { attributed, oczekiwano: COUNT, list: String(who).slice(0, 200) });
  if (attributed === 0) {
    reporter.blocker(`żaden bot nie wylądował na domenie ${DOMAIN} — velocity try-order lub backend padł`);
  }

  const tpsSamples = [];
  const endAt = Date.now() + MINUTES * 60000;
  while (Date.now() < endAt) {
    await sleep(Math.min(TPS_INTERVAL_MS, Math.max(0, endAt - Date.now())));
    if (Date.now() >= endAt) break;
    const sample = await bridge.getTps({ domain: DOMAIN });
    const t1 = sample.tps[0] ?? null;
    tpsSamples.push({ at: new Date().toISOString(), tps: sample.tps, fleetAlive: fleet.size });
    console.log(`[loadtest] tps=${sample.tps.join('/')} fleet=${fleet.size}/${COUNT} stalls=${stallEvents.length}`);
    reporter.step('tps-sample', t1 !== null && t1 < TPS_FAIL ? 'FAIL' : 'OK', { tps: sample.tps, fleetAlive: fleet.size });
  }

  shuttingDown = true;
  for (const [, info] of fleet) { try { process.kill(info.pid, 'SIGTERM'); } catch {} }
  await sleep(1500);

  const minTps = tpsSamples.reduce((min, s) => {
    const v = s.tps[0];
    return v !== undefined && (min === null || v < min) ? v : min;
  }, null);
  const tpsFailOk = minTps === null || minTps >= TPS_FAIL;
  const tpsWarnHit = minTps !== null && minTps < TPS_WARN;

  const exhausted = [...fleet.values()].filter(f => f.exhausted).map(f => f.username);
  const fleetOk = exhausted.length === 0;

  const stallCounts = {};
  for (const ev of stallEvents) stallCounts[ev.bot] = (stallCounts[ev.bot] || 0) + 1;
  const worstStallBot = Object.entries(stallCounts).sort((a, b) => b[1] - a[1])[0];
  const chunkStallOk = !worstStallBot || worstStallBot[1] < STALL_THRESHOLD;

  reporter.step('kryterium: TPS >= tps-fail', tpsFailOk ? 'OK' : 'FAIL', { minTps, tpsFail: TPS_FAIL });
  reporter.step('kryterium: TPS >= tps-warn (informacyjne)', tpsWarnHit ? 'WARN' : 'OK', { minTps, tpsWarn: TPS_WARN });
  reporter.step('kryterium: flota bez trwałego ubytku', fleetOk ? 'OK' : 'FAIL', { exhausted });
  reporter.step('kryterium: brak powtarzalnych chunk-stalli', chunkStallOk ? 'OK' : 'FAIL', {
    stallEventsTotal: stallEvents.length, perBot: stallCounts, threshold: STALL_THRESHOLD
  });
  reporter.state.tpsSamples = tpsSamples;
  reporter.state.stallEvents = stallEvents;

  const pass = tpsFailOk && fleetOk && chunkStallOk;
  if (!pass) reporter.blocker('kryteria load-testu niespełnione — patrz kroki FAIL');
  const file = reporter.finalize(pass ? 'PASS' : 'FAIL');
  console.log(`\n[loadtest] Werdykt: ${pass ? 'PASS' : 'FAIL'}\nRaport: ${file}`);
  process.exit(pass ? 0 : 1);
}

main().catch(err => {
  console.error('[loadtest] fatal:', err);
  process.exit(2);
});
