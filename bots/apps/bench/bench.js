'use strict';

// Benchmark stabilności protokołu (Etap 0, przestawiony przed companion-plugin).
// Bot roamuje po backendzie (domyślnie world — najwięcej entity metadata z
// ModelEngine) przez --minutes i zbiera próbki co minutę.
//
// Kryteria PASS (zdefiniowane wprost):
//  1. zero fatal (proces żyje do końca),
//  2. 0 reconnectów,
//  3. recoverable nierosnące monotonicznie (ostatnia 1/3 okna: brak przyrostu
//     lub przyrost malejący vs środkowa 1/3 — rosnące = leak/backpressure),
//  4. RSS stabilne (ostatnia próbka <= 1.5x próbki z 5. minuty),
//  5. sanity-probe modelu świata: liczba encji u bota vs ground-truth z RCON
//     (raportowane; twardy próg dopiero z companion-pluginem).
//
// Użycie: node apps/bench/bench.js --domain world --minutes 30 [--view 8]

const { BotSession, sleep } = require('@mmorpg-bots/bot-core');
const { installProcessGuards } = require('@mmorpg-bots/safe-protocol');
const bridge = require('@mmorpg-bots/mmo-bridge');
const { createReporter } = require('@mmorpg-bots/qa');

function arg(name, fallback) {
  const idx = process.argv.indexOf(`--${name}`);
  return idx !== -1 && process.argv[idx + 1] ? process.argv[idx + 1] : fallback;
}

const DOMAIN = arg('domain', 'world');
const MINUTES = Number(arg('minutes', 30));
const VIEW = Number(arg('view', 8)); // celowo NIE tiny — bench ma maksymalizować ekspozycję na metadata
const USER = arg('user', `BNC${Date.now().toString().slice(-8)}`).slice(0, 16);

installProcessGuards(console, err => {
  console.error('[bench] FATAL — kryterium 1 złamane:', err.message);
  process.exit(2);
});

async function entityGroundTruth(pos) {
  const raw = await bridge.safeRcon(
    `execute positioned ${Math.floor(pos.x)} ${Math.floor(pos.y)} ${Math.floor(pos.z)} if entity @e[distance=..64]`,
    { domain: DOMAIN }
  );
  const m = String(raw).replace(/\x1b\[[0-9;]*m/g, '').match(/count:\s*(\d+)/i);
  return m ? Number(m[1]) : null;
}

async function main() {
  const reporter = createReporter({ name: `bench_${DOMAIN}_${MINUTES}m_${USER}` });
  const session = new BotSession({ username: USER, viewDistance: VIEW, maxReconnects: 99 });
  const bot = await session.connect();
  reporter.step('connect', 'OK', { user: USER, domain: DOMAIN, view: VIEW, pos: bot.entity.position.toString() });

  // Velocity try-order może wysłać bota na inny backend niż mierzony —
  // twarda asercja, że bot jest na domenie, którą benchmarkujemy.
  await sleep(3000);
  const who = await bridge.safeRcon('minecraft:list', { domain: DOMAIN });
  if (!String(who).includes(USER)) {
    reporter.blocker(`bot nie jest na backendzie ${DOMAIN} (list: ${String(who).slice(0, 120)})`);
    reporter.finalize('BLOCKED');
    console.error(`[bench] BLOCKED — bot trafił na inny backend niż ${DOMAIN}`);
    process.exit(3);
  }
  reporter.step('backend-attribution', 'OK', { domain: DOMAIN });

  // roam: zmiana kierunku co 20 s, sprint+jump (dumb AI, niskie CPU)
  bot.setControlState('forward', true);
  bot.setControlState('sprint', true);
  bot.setControlState('jump', true);
  const roamTimer = setInterval(() => {
    bot.look(Math.random() * Math.PI * 2, 0, true).catch(() => {});
  }, 20000);

  const samples = [];
  const endAt = Date.now() + MINUTES * 60000;
  while (Date.now() < endAt) {
    await sleep(60000);
    const stats = session.protocolStats || { total: 0, byKind: {}, byPacket: {} };
    const pos = bot.entity?.position;
    const sample = {
      minute: samples.length + 1,
      rssMB: Math.round(process.memoryUsage().rss / 1048576),
      recoverable: (stats.byKind['partial-read'] || 0) + (stats.byKind['deserialization'] || 0),
      fatalKinds: { ...stats.byKind },
      reconnects: session.reconnect.total,
      entitiesBot: Object.keys(bot.entities || {}).length,
      entitiesRcon: pos ? await entityGroundTruth(pos) : null,
      pos: pos ? pos.floored().toString() : null
    };
    samples.push(sample);
    console.log(`[bench] m${sample.minute}/${MINUTES} rss=${sample.rssMB}MB recoverable=${sample.recoverable} reconnects=${sample.reconnects} entBot=${sample.entitiesBot} entRcon=${sample.entitiesRcon} pos=${sample.pos}`);
    // re-asercja atrybucji co 5 min: flip backendu w trakcie runu (velocity
    // try-order po reconnect) unieważnia pomiar — BLOCKED, nie ciche dane
    if (sample.minute % 5 === 0) {
      const stillHere = await bridge.safeRcon('minecraft:list', { domain: DOMAIN });
      if (!String(stillHere).includes(USER)) {
        reporter.blocker(`bot opuścił backend ${DOMAIN} w minucie ${sample.minute} (list: ${String(stillHere).slice(0, 100)})`);
        reporter.state.samples = samples;
        const file = reporter.finalize('BLOCKED');
        console.error(`[bench] BLOCKED — utrata atrybucji w m${sample.minute}\nRaport: ${file}`);
        await session.stop();
        process.exit(3);
      }
    }
  }
  clearInterval(roamTimer);

  // ewaluacja kryteriów
  const n = samples.length;
  const third = Math.max(1, Math.floor(n / 3));
  const rec = i => samples[i].recoverable;
  const growthMid = rec(2 * third - 1) - rec(third - 1);
  const growthLast = rec(n - 1) - rec(2 * third - 1);
  const recoverableOk = growthLast === 0 || growthLast < growthMid;
  const reconnectsOk = samples[n - 1].reconnects === 0;
  const rssBase = (samples[Math.min(4, n - 1)] || samples[0]).rssMB;
  const rssOk = samples[n - 1].rssMB <= rssBase * 1.5;

  reporter.step('kryterium: 0 fatal', 'OK', {});
  reporter.step('kryterium: 0 reconnect', reconnectsOk ? 'OK' : 'FAIL', { reconnects: samples[n - 1].reconnects });
  reporter.step('kryterium: recoverable nierosnące', recoverableOk ? 'OK' : 'FAIL', { total: rec(n - 1), growthMid, growthLast });
  reporter.step('kryterium: RSS stabilne', rssOk ? 'OK' : 'FAIL', { baseMB: rssBase, lastMB: samples[n - 1].rssMB });
  const probes = samples.filter(s => s.entitiesRcon != null);
  reporter.step('sanity-probe encji (raport, bez progu)', 'OK', {
    próbki: probes.length,
    śr_bot: Math.round(probes.reduce((a, s) => a + s.entitiesBot, 0) / (probes.length || 1)),
    śr_rcon: Math.round(probes.reduce((a, s) => a + s.entitiesRcon, 0) / (probes.length || 1))
  });
  reporter.step('per-pakiet', 'OK', session.protocolStats?.byPacket || {});
  reporter.state.samples = samples;

  const pass = reconnectsOk && recoverableOk && rssOk;
  if (!pass) reporter.blocker('kryteria stabilności niespełnione — patrz kroki FAIL');
  const file = reporter.finalize(pass ? 'PASS' : 'FAIL');
  await session.stop();
  console.log(`\n[bench] Werdykt: ${pass ? 'PASS' : 'FAIL'}\nRaport: ${file}`);
  process.exit(pass ? 0 : 1);
}

main().catch(err => {
  console.error('[bench] fatal:', err);
  process.exit(2);
});
