// Walidacja generowania Iris imagemap (pack `empty`) botem mineflayer.
// Bot: wchodzi, generuje świat `dungeon_test` z dimensionu `empty`, leci nad
// footprintem 200x500 (centered, 1px=1blok => X∈[-100,100), Z∈[-250,250)),
// odczytuje blok powierzchni i klasyfikuje strefy wg LOCH_LAYOUT_CONTRACT.
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');

const HOST = '127.0.0.1', PORT = 25565;
const RCON_PORT = 25575, RCON_PASS = process.env.RCON_PASS;
const USER = 'Validator';
const WORLD = 'dungeon_test';

// kontrakt stref -> blok wierzchni
const ZONE = {
  grass_block: 'START', // dunstart #000040
  ice:         'MID',   // dunmid   #000080
  granite:     'END',   // dunend   #0000C0
  deepslate_tiles: 'EMPTY', // tlo poza obrazem
};
const sleep = ms => new Promise(r => setTimeout(r, ms));

async function waitFor(fn, timeout = 30000, step = 250) {
  const t0 = Date.now();
  while (Date.now() - t0 < timeout) { if (await fn()) return true; await sleep(step); }
  return false;
}

(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: RCON_PORT, password: RCON_PASS });
  console.log('[rcon] connected');

  const bot = mineflayer.createBot({ host: HOST, port: PORT, username: USER, auth: 'offline', version: '1.21.11' });
  const mcData = () => require('minecraft-data')(bot.version);

  // Nexo wysyła obowiązkowy resource pack pre-join (faza configuration) — bez
  // akceptacji serwer czeka w nieskończoność i bot nigdy nie spawnuje.
  bot.on('resourcePack', () => { console.log('[bot] resource pack -> accept'); bot.acceptResourcePack(); });
  bot.on('kicked', (r) => console.log('[bot] KICKED', JSON.stringify(r)));
  bot.on('error', (e) => console.log('[bot] ERROR', e.message));
  bot.on('message', (m) => { const s = m.toString().trim(); if (s) console.log('[chat]', s.slice(0, 200)); });

  // watchdog — nie wisieć w nieskończoność
  const WATCHDOG = setTimeout(() => { console.log('[FATAL] watchdog 300s'); process.exit(8); }, 300000);

  const spawned = await waitFor(async () => !!bot.entity, 90000, 500);
  if (!spawned) { console.log('[FATAL] brak spawnu w 90s'); process.exit(7); }
  console.log('[bot] spawned, dimension:', bot.game.dimension, 'ver', bot.version);
  WATCHDOG.unref && WATCHDOG.unref();

  await rcon.send(`op ${USER}`);
  await sleep(500);
  await rcon.send(`gamemode spectator ${USER}`);

  // Bot SAM tworzy świat (sender=gracz → Iris teleportuje go do nowego świata).
  // Zmianę świata wykrywamy kolejnym eventem 'spawn' (mineflayer emituje przy zmianie świata).
  let worldChanged = false;
  bot.on('spawn', () => { worldChanged = true; });
  console.log('[iris] bot tworzy świat', WORLD, 'type=empty ...');
  worldChanged = false;
  bot.chat(`/iris create ${WORLD} type=empty`);

  let arrived = await waitFor(async () => worldChanged, 120000, 500);
  if (!arrived) {
    console.log('[iris] brak auto-tp Iris, próbuję mvtp...');
    worldChanged = false;
    bot.chat(`/mvtp ${WORLD}`);
    arrived = await waitFor(async () => worldChanged, 25000, 500);
  }
  await sleep(2500);
  await rcon.send(`gamemode spectator ${USER}`);
  // potwierdzenie: jaki blok jest pod botem (powinien być płaski teren Iris)
  const under = (() => { const p = bot.entity && bot.entity.position; if (!p) return null;
    for (let y = Math.floor(p.y); y >= p.y - 25; y--) { const b = bot.blockAt(new (require('vec3').Vec3)(p.x, y, p.z)); if (b && b.name !== 'air') return `${b.name}@${y}`; } return null; })();
  console.log('[bot] arrived:', arrived, '| pozycja:', bot.entity && bot.entity.position, '| pod botem:', under);

  // odczyt bloku powierzchni w kolumnie (x,z)
  function surface(x, z) {
    for (let y = 80; y >= 40; y--) {
      const b = bot.blockAt(new (require('vec3').Vec3)(x + 0.5, y, z + 0.5));
      if (!b) continue;
      const n = b.name;
      if (n !== 'air' && n !== 'cave_air' && n !== 'void_air') return n;
    }
    return null;
  }

  // przelot po waypointach pokrywajacych footprint, sampling siatka
  const hist = {};
  const grid = {}; // klucz "X,Z" -> char strefy (do mapy ASCII)
  const wps = [[-180],[ -60],[60],[180],[240],[-240]].map(([z]) => [0, z]);
  const tag = n => ZONE[n] || (n === 'deepslate' ? 'rock' : n);

  for (const [wx, wz] of wps) {
    await rcon.send(`tp ${USER} ${wx} 100 ${wz}`);
    await sleep(2500);
    // poczekaj az chunk pod botem sie zaladuje
    await waitFor(async () => surface(wx, wz) !== null, 8000);
    for (let x = -110; x <= 110; x += 10) {
      for (let z = wz - 55; z <= wz + 55; z += 10) {
        const n = surface(x, z);
        if (!n) continue;
        const t = tag(n);
        hist[t] = (hist[t] || 0) + 1;
        grid[`${x},${z}`] = t;
      }
    }
    console.log(`[scan] waypoint z=${wz} ok`);
  }

  // mapa ASCII (Z malejaco u gory), X w poziomie
  const SY = { START: 'S', MID: 'M', END: 'E', EMPTY: '.', rock: '#' };
  console.log('\n=== MAPA (X: -110..110 step20, Z: 250..-250 step20) ===');
  console.log('     ' + Array.from({length: 12}, (_, i) => String(((-110 + i*20)/10|0)).padStart(3)).join(''));
  for (let z = 240; z >= -240; z -= 20) {
    let row = '';
    for (let x = -110; x <= 110; x += 20) {
      // najblizszy zsamplowany punkt
      let best = null, bd = 1e9;
      for (let dz = -9; dz <= 9; dz++) for (let dx = -9; dx <= 9; dx++) {
        const k = `${x+dx},${z+dz}`; if (grid[k] != null) { const d = dx*dx+dz*dz; if (d<bd){bd=d;best=grid[k];} }
      }
      row += (best ? (SY[best] || '?') : ' ');
    }
    console.log(String(z).padStart(5) + ' ' + row.split('').join('  '));
  }

  console.log('\n=== HISTOGRAM stref (liczba zsamplowanych kolumn) ===');
  console.log(hist);

  // weryfikacja proporcji wewnatrz footprintu (S:M:E ~ 40:40:20)
  const S = hist.START||0, M = hist.MID||0, E = hist.END||0, tot = S+M+E;
  console.log('\n=== WERDYKT ===');
  if (tot === 0) { console.log('❌ Brak stref dungeonu — imagemap NIE zadziałał.'); }
  else {
    const pS=(100*S/tot).toFixed(0), pM=(100*M/tot).toFixed(0), pE=(100*E/tot).toFixed(0);
    console.log(`Strefy obecne: START=${S} MID=${M} END=${E}  (% ${pS}/${pM}/${pE}, oczekiwane ~40/40/20)`);
    const zones = [S>0,M>0,E>0].filter(Boolean).length;
    const okProp = Math.abs(pS-40)<=12 && Math.abs(pM-40)<=12 && Math.abs(pE-20)<=12;
    console.log(zones===3 ? '✅ Trzy strefy wykryte' : `⚠️ Wykryto ${zones}/3 stref`);
    console.log(okProp ? '✅ Proporcje ~zgodne z obrazem 40/40/20' : '⚠️ Proporcje odbiegają (sprawdź orientację/sampling)');
  }
  console.log('EMPTY(tlo)=', hist.EMPTY||0, ' rock/inne=', JSON.stringify(Object.fromEntries(Object.entries(hist).filter(([k])=>!['START','MID','END','EMPTY'].includes(k)))));

  await rcon.end();
  bot.quit();
  process.exit(0);
})().catch(e => { console.error('FATAL', e); process.exit(1); });
