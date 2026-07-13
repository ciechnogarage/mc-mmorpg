const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const { startViewer } = require('./foundation_viewer');
const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

const HOST = '127.0.0.1';
const PORT = 25565;
const RPORT = 25575;
const RPASS = process.env.RCON_PASS;
const USER = 'Camera';
const WORLD = 'l1view';
const VPORT = 3012;
const SHOTS = path.join(__dirname, 'shots');

console.warn('[UNSUPPORTED_VISUAL_PROOF] Prismarine Viewer does not resolve ModelEngine resource-pack item models. These screenshots must not be used for model art approval.');

const STAGE = {
  cx: 0,
  cy: 64,
  cz: 0
};

const VIEWS = [
  ['guardian_stage_front', 0, 72, 20, 0, 70, 0],
  ['guardian_stage_front_close', 0, 71, 12, 0, 69, 0],
  ['guardian_stage_left', 15, 71, 11, 0, 69, 0],
  ['guardian_stage_right', -15, 71, 11, 0, 69, 0],
  ['guardian_stage_three_quarter', 12, 73, 18, 0, 70, 0]
];

const sleep = ms => new Promise(r => setTimeout(r, ms));
const waitFor = async (fn, to = 60000, st = 300) => {
  const t = Date.now();
  while (Date.now() - t < to) {
    if (await fn()) return true;
    await sleep(st);
  }
  return false;
};

function lookAt(cx, cy, cz, tx, ty, tz) {
  const dx = tx - cx;
  const dy = ty - cy;
  const dz = tz - cz;
  const h = Math.hypot(dx, dz);
  const yaw = Math.atan2(-dx, dz) * 180 / Math.PI;
  const pitch = -Math.atan2(dy, h) * 180 / Math.PI;
  return { yaw, pitch };
}

async function cmd(rcon, command) {
  const res = await rcon.send(command).catch(e => `ERR ${e.message}`);
  console.log('>', command);
  console.log(String(res).replace(/\u00a7[0-9A-FK-ORX]/gi, ''));
  return res;
}

async function tpCamera(rcon, x, y, z, yaw, pitch) {
  await cmd(rcon, `minecraft:tp ${USER} ${x} ${y} ${z} ${yaw.toFixed(1)} ${pitch.toFixed(1)}`);
}

(async () => {
  fs.mkdirSync(SHOTS, { recursive: true });
  const rcon = await Rcon.connect({ host: HOST, port: RPORT, password: RPASS });
  const bot = mineflayer.createBot({ host: HOST, port: PORT, username: USER, auth: 'offline', version: '1.21.11' });
  bot.on('resourcePack', () => bot.acceptResourcePack());
  bot.on('error', e => console.log('[ERR]', e.message));
  bot._client.on('error', () => {});
  process.on('uncaughtException', e => {
    if (!/PartialReadError|Read error/.test(String(e))) console.log('[UNC]', e.message);
  });

  const WD = setTimeout(() => {
    console.log('[FATAL] watchdog');
    process.exit(8);
  }, 240000);

  if (!await waitFor(() => !!bot.entity, 60000, 500)) {
    console.log('[FATAL] no spawn');
    process.exit(7);
  }

  await cmd(rcon, `op ${USER}`);
  await sleep(500);
  let spawns = 0;
  bot.on('spawn', () => spawns++);
  const before = spawns;
  bot.chat(`/mvtp ${WORLD}`);
  await waitFor(() => spawns > before, 15000, 300);
  await sleep(1500);

  await cmd(rcon, `minecraft:time set noon`);
  await cmd(rcon, `minecraft:weather clear`);
  await cmd(rcon, `forceload add -8 -8 8 8`);
  await cmd(rcon, `fill -24 60 -24 24 110 24 minecraft:air`);
  await cmd(rcon, `fill -20 63 -20 20 63 20 minecraft:moss_block`);
  await cmd(rcon, `fill -22 64 -22 22 64 22 minecraft:air`);
  await cmd(rcon, `fill -18 64 -18 18 64 18 minecraft:mud`);
  await cmd(rcon, `fill -16 65 -16 16 65 16 minecraft:moss_carpet`);
  await cmd(rcon, `fill -20 64 -20 -20 84 20 minecraft:dark_oak_log`);
  await cmd(rcon, `fill 20 64 -20 20 84 20 minecraft:dark_oak_log`);
  await cmd(rcon, `fill -20 64 -20 20 84 -20 minecraft:dark_oak_log`);
  await cmd(rcon, `fill -20 64 20 20 84 20 minecraft:dark_oak_log`);
  await cmd(rcon, `fill -10 66 -19 10 68 -18 minecraft:leaves[persistent=true]`);
  await cmd(rcon, `fill -10 66 18 10 68 19 minecraft:leaves[persistent=true]`);
  await cmd(rcon, `kill @e[type=minecraft:zombie,x=${STAGE.cx},y=${STAGE.cy},z=${STAGE.cz},distance=..30]`);
  await cmd(rcon, `kill @e[type=minecraft:text_display,x=${STAGE.cx},y=${STAGE.cy},z=${STAGE.cz},distance=..50]`);
  await cmd(rcon, `kill @e[type=minecraft:item_display,x=${STAGE.cx},y=${STAGE.cy},z=${STAGE.cz},distance=..50]`);
  await cmd(rcon, `mm mobs spawn level_1_grove_guardian 1 ${WORLD},${STAGE.cx},${STAGE.cy},${STAGE.cz}`);
  await sleep(2000);
  await cmd(rcon, `data merge entity @e[type=minecraft:zombie,x=${STAGE.cx},y=${STAGE.cy},z=${STAGE.cz},distance=..10,sort=nearest,limit=1] {NoAI:1b,Silent:1b}`);
  await cmd(rcon, `minecraft:tp ${USER} 0 72 20 180 6`);

  const viewer = startViewer(bot, { enabled: true, port: VPORT, firstPerson: true, viewDistance: 10 });
  await sleep(2000);
  const browser = await puppeteer.launch({
    headless: 'new',
    args: [
      '--no-sandbox',
      '--use-gl=swiftshader',
      '--enable-webgl',
      '--ignore-gpu-blocklist',
      '--enable-unsafe-swiftshader',
      '--window-size=1280,720'
    ]
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 720 });
  await page.goto(`http://127.0.0.1:${VPORT}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
  await sleep(5000);

  for (const [name, cx, cy, cz, tx, ty, tz] of VIEWS) {
    const { yaw, pitch } = lookAt(cx, cy, cz, tx, ty, tz);
    await cmd(rcon, `minecraft:gamemode creative ${USER}`);
    await tpCamera(rcon, cx, cy, cz, yaw, pitch);
    await waitFor(() => bot.entity && Math.abs(bot.entity.position.x - cx) < 1.5 && Math.abs(bot.entity.position.y - cy) < 2 && Math.abs(bot.entity.position.z - cz) < 1.5, 6000, 200);
    if (bot.entity) {
      console.log('[bot-pos]', bot.entity.position);
    }
    await sleep(1200);
    await page.reload({ waitUntil: 'domcontentloaded', timeout: 30000 });
    await sleep(5000);
    const out = path.join(SHOTS, `${name}.png`);
    await page.screenshot({ path: out });
    console.log('[shot]', out);
  }

  await browser.close();
  viewer.close();
  await cmd(rcon, `forceload remove all`);
  await rcon.end();
  bot.quit();
  clearTimeout(WD);
  console.log('[done]');
  process.exit(0);
})().catch(e => {
  console.error('FATAL', e);
  process.exit(1);
});
