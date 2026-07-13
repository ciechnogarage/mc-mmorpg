// HARNESS E2E lochu level_1 (samo-resetujaca, samo-weryfikujaca, server-side observe).
// Uzycie:  node dungeon_e2e.js <MODE> <DIFF>
//   MODE = spawn | full   (default spawn)   DIFF = EASY|NORMAL|HARD (default NORMAL)
// Robi PELNY reset: stop serwera -> rm swiatow instancji -> czyszczenie globalplayerdata
// (zrodlo bledu "unknown world" -> regeneruja sie przy join) -> start -> wait Done.
// Potem: /md play, server-side scan bossa, (full) melee z lapy + listener chatu finish,
// /md leave, asercja braku orphana. Na koncu JEDEN blok werdyktu + exit code.
//
// Obserwacja jest WYLACZNIE server-side (RCON+forceload) - bot tylko steruje.
// ZERO komend zabijajacych (kill/damage/mm kill) - finish wylacznie realnym bot.attack.
const { spawn, execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');

const ROOT = path.resolve(__dirname, '..');
const MODE = (process.argv[2] || 'spawn').toLowerCase();
const DIFF = (process.argv[3] || 'NORMAL').toUpperCase();
const H = '127.0.0.1', P = 25565, RP = 25575, RPASS = process.env.RCON_PASS, U = process.env.E2E_USER || 'FableProbe';
const LOG = process.env.E2E_LOG || '/tmp/claude-1000/-home-przemek/d21399fa-c84d-4436-b060-d4cdb74fb377/scratchpad/srv_e2e.log';
const JAVA_FLAGS = ['-Xms4G','-Xmx4G','-XX:+UseG1GC','-XX:+ParallelRefProcEnabled','-XX:MaxGCPauseMillis=200','-XX:+UnlockExperimentalVMOptions','-XX:+DisableExplicitGC','-XX:+AlwaysPreTouch','-XX:G1NewSizePercent=30','-XX:G1MaxNewSizePercent=40','-XX:G1HeapRegionSize=8M','-XX:G1ReservePercent=20','-XX:G1HeapWastePercent=5','-XX:G1MixedGCCountTarget=4','-XX:InitiatingHeapOccupancyPercent=15','-XX:G1MixedGCLiveThresholdPercent=90','-XX:G1RSetUpdatingPauseTimePercent=5','-XX:SurvivorRatio=32','-XX:+PerfDisableSharedMem','-XX:MaxTenuringThreshold=1','-Dusing.aikars.flags=https://mcflags.emc.gs','-Daikars.new.flags=true','-jar','paper.jar','--nogui'];

const sleep = ms => new Promise(r => setTimeout(r, ms));
const wf = async (f, to = 30000, st = 500) => { const t = Date.now(); while (Date.now() - t < to) { const v = await f(); if (v) return v; await sleep(st); } return false; };
const sh = c => { try { return execSync(c, { cwd: ROOT, stdio: ['ignore','pipe','pipe'] }).toString(); } catch (e) { return (e.stdout||'').toString() + (e.stderr||'').toString(); } };

async function rconStop() {
  try { const r = await Rcon.connect({ host: H, port: RP, password: RPASS }); await r.send('stop'); await r.end(); } catch (_) {}
}
async function waitJavaDown(to = 60000) {
  const t = Date.now();
  while (Date.now() - t < to) { const out = sh("pgrep -f 'paper.jar --nogui' || true").trim(); if (!out) return true; await sleep(2000); }
  return false;
}

(async () => {
  const V = { SPAWN:'SKIP', STARTGAME:'SKIP', FINISH:'SKIP', ORPHAN:'SKIP' };
  let detail = {};
  // ---- 1. RESET (korupcjo-odporny) ----
  console.error('[reset] stop serwera...');
  await rconStop();
  if (!await waitJavaDown()) { sh("pkill -9 -f 'paper.jar --nogui' || true"); await sleep(2000); }
  console.error('[reset] usuwam swiaty instancji + czyszcze globalplayerdata...');
  sh("rm -rf level_1_[0-9]*");
  sh("rm -f plugins/MythicDungeons/globalplayerdata/*.yml 2>/dev/null || true");
  console.error('[reset] start serwera...');
  try { fs.unlinkSync(LOG); } catch (_) {}
  const srv = spawn('java', JAVA_FLAGS, { cwd: ROOT, detached: true, stdio: ['ignore', fs.openSync(LOG, 'a'), fs.openSync(LOG, 'a')] });
  srv.unref();
  const ready = await wf(() => { try { return /Done \([0-9.]+s\)/.test(fs.readFileSync(LOG, 'utf8')); } catch { return false; } }, 180000, 2000);
  if (!ready) { console.log(verdict(V, { fatal: 'serwer nie wstal (brak Done w 180s)' })); process.exit(9); }
  console.error('[reset] serwer gotowy.');
  await sleep(3000);

  // ---- 2. DRIVE: bot + /md play ----
  const rcon = await Rcon.connect({ host: H, port: RP, password: RPASS });
  const send = async c => (await rcon.send(c)).replace(/§[0-9a-fklmnorx]/g, '');
  let completed = false;
  const bot = mineflayer.createBot({ host: H, port: P, username: U, auth: 'offline', version: '1.21.11' });
  bot.on('resourcePack', () => bot.acceptResourcePack());
  bot.on('error', () => {}); bot.on('end', () => {}); process.on('uncaughtException', () => {});
  bot.on('messagestr', (m) => { if (/you completed|ukoncz/i.test(m)) completed = true; });
  if (!await wf(() => bot.entity, 40000)) { console.log(verdict(V, { fatal: 'bot nie dolaczyl' })); process.exit(7); }
  await send(`op ${U}`); await sleep(700);
  const dimOf = async () => (await send(`data get entity ${U} Dimension`)).match(/"([^"]+)"/)?.[1];
  bot.chat(`/md play level_1:${DIFF} ${U}`);
  const dimIn = await wf(async () => { const d = await dimOf(); return d && /level_1_/.test(d) ? d : false; }, 25000, 600);
  if (!dimIn) { console.log(verdict(V, { fatal: 'nie wszedl do instancji (/md play)' })); try{await rcon.end();}catch{}; process.exit(3); }
  detail.dim = dimIn;

  // TriggerDistance (FunctionStartDungeon @ 0,64,138 r=6) odpala na RUCH gracza —
  // bot musi zrobic krok, inaczej gra nigdy nie startuje i boss sie nie spawnuje.
  bot.setControlState('forward', true); await sleep(900); bot.setControlState('forward', false);
  bot.setControlState('back', true); await sleep(600); bot.setControlState('back', false);

  // ---- startGame dyskryminator ----
  await sleep(1200);
  const pos = (await send(`data get entity ${U} Pos`)).match(/\[([^\]]+)\]/)?.[1] || '?';
  const gm = (await send(`data get entity ${U} playerGameType`)).match(/data: (\d)/)?.[1] ?? '?';
  detail.pos = pos.replace(/d/g,'').slice(0,30); detail.gm = gm;
  V.STARTGAME = (gm === '0') ? 'PASS' : 'FAIL'; // 0=SURVIVAL (config Gamemode: SURVIVAL)

  // ---- 3. OBSERVE server-side: boss ----
  for (let cx = -16; cx <= 16; cx += 16) await send(`execute in ${dimIn} run forceload add ${cx*16} -256 ${cx*16+255} 256`);
  const scanBoss = async () => {
    const r = await send(`execute in ${dimIn} run data get entity @e[type=minecraft:zombie,limit=1] CustomName`);
    if (/No entity/i.test(r)) return null;
    return (r.match(/CustomName: '?"?([^"']*)/) || [])[1] || 'zombie(bez nazwy)';
  };
  let bossName = null;
  for (const t of [2000, 2000, 2000, 2000]) { await sleep(t); bossName = await scanBoss(); if (bossName) break; }
  V.SPAWN = bossName ? 'PASS' : 'FAIL'; detail.boss = bossName || 'BRAK';

  // ---- 4. FULL: finish z lapy (realne uderzenia, bez komend) ----
  if (MODE === 'full' && bossName) {
    detail.finishNote = 'melee best-effort';
    const deadline = Date.now() + 60000;
    while (Date.now() < deadline && !completed) {
      const tgt = bot.nearestEntity(e => e && e.name === 'zombie');
      if (tgt) {
        const d = bot.entity.position.distanceTo(tgt.position);
        if (d > 3) { try { bot.lookAt(tgt.position.offset(0,1,0)); } catch {} bot.setControlState('forward', true); }
        else { bot.setControlState('forward', false); try { bot.lookAt(tgt.position.offset(0,1,0)); bot.attack(tgt); } catch {} }
      }
      await sleep(350);
    }
    bot.setControlState('forward', false);
    await wf(() => completed, 4000, 400);
    V.FINISH = completed ? 'PASS' : 'FAIL';
  }

  // ---- 5. CZYSTE wyjscie + asercja braku orphana ----
  bot.chat('/md leave');
  await wf(async () => { const d = await dimOf(); return d && !/level_1_/.test(d) ? d : false; }, 15000, 600);
  detail.outDim = await dimOf();
  await sleep(2500);
  const orphans = sh("ls -d level_1_[0-9]* 2>/dev/null || true").trim();
  V.ORPHAN = orphans ? 'FAIL' : 'PASS'; detail.orphans = orphans || 'brak';

  try { await rcon.end(); } catch {} try { bot.quit(); } catch {}
  console.log(verdict(V, detail));
  const fail = Object.entries(V).some(([k,v]) => v === 'FAIL');
  process.exit(fail ? 1 : 0);
})().catch(e => { console.error('FATAL', e.stack || e.message); process.exit(1); });

function verdict(V, d) {
  return ['==== WERDYKT dungeon_e2e ('+MODE+' '+DIFF+') ====',
    `SPAWN=${V.SPAWN} (boss=${d.boss||'?'}, dim=${d.dim||'?'})`,
    `STARTGAME=${V.STARTGAME} (gm=${d.gm||'?'}, pos=${d.pos||'?'})`,
    `FINISH=${V.FINISH}${d.finishNote?' ['+d.finishNote+']':''}`,
    `ORPHAN=${V.ORPHAN} (${d.orphans||'?'}, outDim=${d.outDim||'?'})`,
    d.fatal ? 'FATAL: '+d.fatal : ''].filter(Boolean).join('\n');
}
