// Auto-screenshoty lochu level_1: bot wchodzi do instancji, prismarine-viewer serwuje
// web-view (WebGL), puppeteer (Chromium+SwiftShader) robi PNG z kilku kątów wokół (0,64,0).
// GRANICA: renderuje TYLKO bloki + vanilla-encje. NIE cząstki MythicMobs / model ModelEngine.
// Użycie: node shoot_level_1.js
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const { startViewer } = require('./foundation_viewer');
const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

const HOST = '127.0.0.1', PORT = 25565, RPORT = 25575, RPASS = process.env.RCON_PASS, USER = 'FableCam'; // unikalna nazwa: rownolegle sesje codex uzywaja wspolnych nazw botow
const VIEWER_PORT = 3000;
const SHOTS = path.join(__dirname, 'shots');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const waitFor = async (fn, to = 60000, st = 300) => { const t = Date.now(); while (Date.now() - t < to) { if (await fn()) return true; await sleep(st); } return false; };

// MD edit-mode zamraza teleport gracza -> kamere obracamy w miejscu (bot.look).
// yaw: 0=poludnie(+z), PI/2=zachod(-x), PI=polnoc(-z), -PI/2=wschod(+x). pitch: + patrzy w dol.
// Gracz wchodzi od poludnia (0,64,10) patrzac na polnoc (yaw=PI) -> tam Drzewo-Serce.
// To jest "money shot": widok z wejscia na scene. Dodane katy w gore lapia korone kolosa.
const ANGLES = [
  { name: '3_polnoc_drzewo', yaw: Math.PI,    pitch: 0.10 },  // pierwsze wrazenie z wejscia
  { name: '6_korona_gora',   yaw: Math.PI,    pitch: -0.45 }, // spojrzenie w gore na korone
  { name: '7_korona_full',   yaw: Math.PI,    pitch: -0.80 }, // pelna korona nad glowa
  { name: '1_poludnie',      yaw: 0,          pitch: 0.15 },  // za siebie (brama wejscia)
  { name: '2_zachod',        yaw: Math.PI/2,  pitch: 0.15 },
  { name: '4_wschod',        yaw: -Math.PI/2, pitch: 0.15 },  // strona skazenia (pln-wsch)
  { name: '5_wdol',          yaw: 0,          pitch: 1.2 },
];

// Tryb 'play' (node shoot_level_1.js play): swieza instancja /md play -> tp po strefach
// jezora poludniowego (edit-mode zamraza tp, play NIE). yaw wg konwencji wyzej.
// Tryb 'proxy' (node shoot_level_1.js proxy): jak 'play', ale PRZED shotami podmienia w
// INSTANCJI (jednorazowy klon!) bloki bez tekstury w viewerze na renderowalne proxy o
// zblizonym vibe. /md leave niszczy instancje -> template NIETKNIETY, zero undo/autosave-risk.
// Dzieki temu PNG pokazuje realny uklad kolorow (mech=zielony, skazenie=ciemny teal),
// nie niebieskie kafle moss / biale "?" sculk. Finalna ocena i tak: uzytkownik w kliencie.
const MODE = process.argv[2] || 'edit';
const PLAYMODE = MODE === 'play' || MODE === 'proxy';
// [realny_blok, proxy_renderowalny] — proxy dobrane pod prismarine-viewer (ma teksture + zbizony kolor)
const PROXY_SWAP = [
  ['moss_block', 'lime_concrete'],           // mech -> zywa zielen (groundcover)
  ['moss_carpet', 'lime_carpet'],
  ['mossy_cobblestone', 'green_concrete_powder'],
  ['sculk', 'warped_planks'],                // skazenie -> ciemny teal
  ['sculk_vein', 'warped_planks'],
  ['sculk_catalyst', 'warped_wart_block'],
  ['sculk_shrieker', 'warped_wart_block'],
  ['sculk_sensor', 'warped_wart_block'],
];
// box obejmujacy caly build (wyspa r44 + korytarz z156 + komora nagrod z-42/y60 + korona kolosa)
const PROXY_BOX = { x1: -46, y1: 54, z1: -46, x2: 46, y2: 122, z2: 156 };
const ZONE_SHOTS = [
  { name: 'z1_oboz_total',     x: 0,  y: 68, z: 150, yaw: Math.PI,      pitch: 0.18 },
  { name: 'z1_oboz_ognisko',   x: 9,  y: 66, z: 137, yaw: Math.PI*0.75, pitch: 0.18 },
  { name: 'z2_sciezka_start',  x: 5,  y: 66, z: 120, yaw: Math.PI,      pitch: 0.10 },
  { name: 'z2_sciezka_skazona',x: 4,  y: 66, z: 106, yaw: Math.PI,      pitch: 0.10 },
  { name: 'z2_kapliczka',      x: -4, y: 66, z: 103, yaw: Math.PI*0.70, pitch: 0.15 },
  { name: 'z3_polana_total',   x: 0,  y: 69, z: 87,  yaw: Math.PI,      pitch: 0.22 },
  { name: 'z3_polana_krag',    x: 7,  y: 66, z: 78,  yaw: Math.PI*0.78, pitch: 0.12 },
  { name: 'z4_grota_wejscie',  x: 10, y: 66, z: 76,  yaw: -Math.PI/2,   pitch: 0.08 },
  { name: 'z4_grota_wnetrze',  x: 20, y: 66, z: 76,  yaw: -Math.PI/2,   pitch: 0.05 },
  { name: 'z5_most_total',     x: 0,  y: 67, z: 66,  yaw: Math.PI,      pitch: 0.15 },
  { name: 'z5_brama_korzeni',  x: 0,  y: 66, z: 52,  yaw: Math.PI,      pitch: 0.05 },
  { name: 'z6_komora_nagrod',  x: 0,  y: 60, z: -39, yaw: Math.PI,      pitch: 0.05 },
  { name: 'z6_komora_schody',  x: 0,  y: 60, z: -40, yaw: 0,            pitch: -0.1 },
  // --- QUALITY v3 (2026-07-04): wezly skazenia, sightliny, oslony, zielarz ---
  { name: 'q_node1_jezor',     x: 12, y: 67, z: -8,  yaw: Math.PI*1.15,  pitch: 0.30 },
  { name: 'q_node2_klin',      x: 15, y: 67, z: -16, yaw: -Math.PI*0.75, pitch: 0.28 },
  { name: 'q_node3_glab',      x: 13, y: 67, z: -25, yaw: -Math.PI*0.72, pitch: 0.25 },
  { name: 'q_sightline_polana',x: 0,  y: 66, z: 72,  yaw: Math.PI,       pitch: 0.02 },
  { name: 'q_sightline_most',  x: 0,  y: 67, z: 58,  yaw: Math.PI,       pitch: 0.0  },
  { name: 'q_cover_path_a',    x: 6,  y: 67, z: 119, yaw: Math.PI,       pitch: 0.22 },
  { name: 'q_zielarz',         x: -14, y: 67, z: 130, yaw: -Math.PI*0.3, pitch: 0.28 },
];

(async () => {
  fs.mkdirSync(SHOTS, { recursive: true });
  const rcon = await Rcon.connect({ host: HOST, port: RPORT, password: RPASS });
  const bot = mineflayer.createBot({ host: HOST, port: PORT, username: USER, auth: 'offline', version: '1.21.11' });
  bot.on('resourcePack', () => bot.acceptResourcePack());
  bot.on('kicked', r => console.log('[KICK]', r));
  bot.on('error', e => console.log('[BOT-ERR]', e.message));
  // MD edit-mode zalewa custom-particlami -> mineflayer parser rzuca PartialReadError.
  // Zdław, zeby nie zrywalo synchronizacji swiata/kamery.
  bot._client.on('error', () => {});
  process.on('uncaughtException', e => { if (!/PartialReadError|Read error/.test(String(e))) console.log('[UNCAUGHT]', e.message); });

  if (!await waitFor(() => bot.entity, 60000)) { console.log('FATAL: brak spawnu bota'); process.exit(7); }
  await rcon.send(`op ${USER}`); await sleep(600);
  if (PLAYMODE) {
    // swieza instancja: tp dziala (w edit-mode jest zamrozony)
    const dimOf = async () => ((await rcon.send(`data get entity ${USER} Dimension`)).match(/"([^"]+)"/) || [])[1];
    if (/level_1_/.test((await dimOf()) || '')) { bot.chat('/md leave'); await sleep(5000); }
    console.log('[step] /md play level_1:EASY (swieza instancja, tryb zone-shots)');
    bot.chat(`/md play level_1:EASY ${USER}`);
    if (!await waitFor(async () => /level_1_/.test((await dimOf()) || ''), 30000, 600)) { console.log('FATAL: brak instancji'); process.exit(3); }
    await sleep(3000);
    if (MODE === 'proxy') {
      // podmiana proxy w TEJ instancji (klon) — template nietkniety, /md leave posprzata
      const B = PROXY_BOX;
      bot.chat(`//pos1 ${B.x1},${B.y1},${B.z1}`); await sleep(700);
      bot.chat(`//pos2 ${B.x2},${B.y2},${B.z2}`); await sleep(700);
      console.log('[proxy] selekcja', JSON.stringify(B), '— podmieniam', PROXY_SWAP.length, 'typow blokow');
      for (const [real, proxy] of PROXY_SWAP) {
        bot.chat(`//replace ${real} ${proxy}`);
        await sleep(2200); // async FAWE na duzym regionie
      }
      console.log('[proxy] podmiana zlecona — czekam na domkniecie FAWE');
      await sleep(4000);
    }
  } else {
    console.log('[step] /md edit level_1 (wejscie do swiata-template, nie instancji)');
    bot.chat('/md edit level_1');
    await sleep(6000); // wejscie do swiata template
  }
  console.log('[bot] swiat=', bot.game?.dimension, ' pos=', bot.entity.position.toString());

  // web-view na bocie
  const viewer = startViewer(bot, { enabled: true, port: VIEWER_PORT, firstPerson: true, viewDistance: 8 });
  await sleep(2500);

  // puppeteer (SwiftShader = software WebGL, bez GPU)
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--use-gl=swiftshader', '--enable-webgl',
           '--ignore-gpu-blocklist', '--enable-unsafe-swiftshader', '--window-size=1280,720'],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 720 });
  await page.goto(`http://127.0.0.1:${VIEWER_PORT}`, { waitUntil: 'networkidle2', timeout: 30000 });
  await sleep(4000); // pierwszy render chunków

  const SHOTLIST = PLAYMODE ? ZONE_SHOTS : ANGLES;
  for (const a of SHOTLIST) {
    if (PLAYMODE && a.x !== undefined) {
      bot.chat(`/minecraft:teleport ${USER} ${a.x} ${a.y} ${a.z}`);
      await sleep(4500); // tp + doladowanie chunkow w viewerze
    }
    await bot.look(a.yaw, a.pitch, true); // obrot w miejscu (edit-mode blokuje tp)
    await sleep(2500); // render nowego kata
    const out = path.join(SHOTS, `${a.name}.png`);
    await page.screenshot({ path: out });
    console.log(`[shot] ${a.name}  yaw=${bot.entity.yaw.toFixed(2)} pitch=${bot.entity.pitch.toFixed(2)} pos=${bot.entity.position}`);
  }
  if (PLAYMODE) { bot.chat('/md leave'); await sleep(2500); } // niszczy instancje (proxy znika, template czysty)

  await browser.close();
  try { viewer.close(); } catch {}
  try { await rcon.end(); } catch {}
  try { bot.quit(); } catch {}
  console.log('[done] screeny w _validation/shots/');
  process.exit(0);
})().catch(e => { console.error('FATAL', e); process.exit(1); });
