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
const VPORT = 3011;
const SHOTS = path.join(__dirname, 'shots');

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

const TARGET = { x: 0, y: 64, z: 0 };
const VIEWS = [
  ['guardian_hero_front', 0, 71, 18, TARGET.x, 68, TARGET.z],
  ['guardian_hero_left', 15, 70, 10, TARGET.x, 68, TARGET.z],
  ['guardian_hero_right', -15, 69, 10, TARGET.x, 68, TARGET.z],
  ['guardian_close_face', 0, 69, 8, TARGET.x, 73, TARGET.z],
  ['guardian_top_oblique', 10, 84, 18, TARGET.x, 69, TARGET.z]
];

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

  await rcon.send(`op ${USER}`);
  await sleep(500);
  let spawns = 0;
  bot.on('spawn', () => spawns++);
  const before = spawns;
  bot.chat(`/mvtp ${WORLD}`);
  await waitFor(() => spawns > before, 15000, 300);
  await sleep(2000);
  await rcon.send(`minecraft:gamemode spectator ${USER}`);
  await sleep(300);

  await rcon.send(`execute in ${WORLD} run kill @e[type=minecraft:zombie,x=0,y=64,z=0,distance=..40]`);
  await rcon.send(`execute in ${WORLD} run kill @e[type=minecraft:text_display,x=0,y=64,z=0,distance=..60]`);
  await rcon.send(`execute in ${WORLD} run forceload add -16 -16 16 16`);
  await sleep(500);
  const spawnRes = await rcon.send(`mm mobs spawn level_1_grove_guardian 1 ${WORLD},0,64,0`).catch(e => `ERR ${e.message}`);
  console.log('[spawn]', String(spawnRes).replace(/\u00a7[0-9A-FK-ORX]/gi, ''));
  await sleep(2500);

  const viewer = startViewer(bot, { enabled: true, port: VPORT, firstPerson: true, viewDistance: 8 });
  await sleep(2500);
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
    await rcon.send(`minecraft:gamemode spectator ${USER}`);
    await rcon.send(`minecraft:tp ${USER} ${cx} ${cy} ${cz} ${yaw.toFixed(1)} ${pitch.toFixed(1)}`);
    await waitFor(() => bot.entity && Math.abs(bot.entity.position.x - cx) < 1.5 && Math.abs(bot.entity.position.y - cy) < 2 && Math.abs(bot.entity.position.z - cz) < 1.5, 6000, 200);
    await sleep(1200);
    await page.reload({ waitUntil: 'domcontentloaded', timeout: 30000 });
    await sleep(5000);
    const out = path.join(SHOTS, `${name}.png`);
    await page.screenshot({ path: out });
    console.log('[shot]', out);
  }

  await browser.close();
  viewer.close();
  await rcon.send(`execute in ${WORLD} run forceload remove all`).catch(() => {});
  await rcon.end();
  bot.quit();
  clearTimeout(WD);
  console.log('[done]');
  process.exit(0);
})().catch(e => {
  console.error('FATAL', e);
  process.exit(1);
});
