// Combat probe: runtime'owa weryfikacja stanów level_1_glimmer_wisp w REALNEJ walce.
// Sprawdza: (1) mob podchodzi do gracza (locomotion/walk), (2) mob rani bota (~onAttack → state attack),
// (3) śmierć od realnych ciosów bota (NIE /kill — zasada death-trigger), (4) sprzątnięcie encji display po death.
const mineflayer = require('mineflayer');
const { execFileSync } = require('child_process');
const H = '127.0.0.1', P = 25565, U = 'WispDuel';
const sleep = ms => new Promise(r => setTimeout(r, ms));
const rcon = c => execFileSync('docker', ['exec', '-i', 'srv-world', 'rcon-cli', c], { encoding: 'utf8' }).replace(/§[0-9a-fklmnorx]/g, '').trim();
const wf = async (f, to = 40000, st = 400) => { const t = Date.now(); while (Date.now() - t < to) { const v = await f(); if (v) return v; await sleep(st); } return false; };
(async () => {
  const bot = mineflayer.createBot({ host: H, port: P, username: U, auth: 'offline', version: '1.21.11' });
  bot.on('resourcePack', () => bot.acceptResourcePack());
  bot.on('error', () => {}); bot.on('end', () => {}); process.on('uncaughtException', () => {});
  if (!await wf(() => bot.entity)) { console.log('FAIL: bot nie wszedł'); process.exit(7); }
  rcon(`op ${U}`); await sleep(400);
  rcon('time set night'); rcon('weather clear'); rcon('gamerule doMobSpawning false'); rcon('kill @e[type=husk]');   // UWAGA: gamerule zwraca "Incorrect argument" na tym buildzie Papera (no-op); selektory filtrują po name, więc naturalne huski nie psują testu
  rcon(`tp ${U} 8 90 8`); await sleep(2500);   // grunt ~y87 (forceloaded); bot opada bez fall damage
  const A = bot.entity.position.clone();        // kotwica: faktyczna pozycja po opadnięciu
  rcon(`give ${U} minecraft:iron_sword 1`); await sleep(300);
  rcon(`effect give ${U} minecraft:resistance 120 1`); await sleep(300);
  let hurtEver = false; const hp0 = bot.health;
  bot.on('health', () => { if (bot.health < hp0 - 0.4) hurtEver = true; });

  const displays = () => Object.values(bot.entities).filter(e => e.name === 'item_display' && e.position && e.position.distanceTo(bot.entity.position) < 24);
  const mob = () => Object.values(bot.entities).find(e => e !== bot.entity && e.username !== U && e.type === 'hostile' || (e.name === undefined && e.position && e.position.distanceTo(bot.entity.position) < 24 && e.id !== bot.entity.id));

  // spawn z retry (grace po boocie)
  let ok = false;
  for (let a = 0; a < 3 && !ok; a++) {
    rcon(`mm mobs spawn level_1_glimmer_wisp 1 world,${Math.round(A.x)+3},${Math.round(A.y)+1},${Math.round(A.z)}`); await sleep(3000);
    ok = displays().length >= 1;
    if (!ok) { rcon('mm mobs killall'); await sleep(8000); }
  }
  if (!ok) { console.log('FAIL: brak displayów po 3 próbach'); process.exit(3); }
  console.log(`OK binding: ${displays().length}x item_display`);

  // (1) podejście: pozycja SERVER-SIDE (displaye to pasażerowie — mineflayer nie aktualizuje ich pozycji!)
  const mobPos = () => { const m = rcon('execute as @e[type=husk,name=\"Migotliwy Duszek\",limit=1] run data get entity @s Pos').match(/\[([-\d.]+)d, ([-\d.]+)d, ([-\d.]+)d\]/); return m ? {x:+m[1], z:+m[3]} : null; };
  // mob spawnuje 3b od bota; doskok trwa <1s (zmierzone), więc pierwszy sample może już być w zwarciu.
  // Podejście = dystans <2.6b w dowolnym samplu (spawn był dalej, więc zbliżenie == locomotion działa).
  const p0 = mobPos(); const dist0 = p0 ? Math.hypot(p0.x-bot.entity.position.x, p0.z-bot.entity.position.z) : 99;
  const approached = await wf(() => { const p = mobPos(); return p && Math.hypot(p.x-bot.entity.position.x, p.z-bot.entity.position.z) < 2.6; }, 20000, 800);
  console.log(approached ? `OK walk: mob w zwarciu (pierwszy pomiar ${dist0.toFixed(1)}b, spawn na 3.0b)` : 'FAIL walk: mob się nie zbliżył (AI/locomotion?)');
  const centr = () => { const d = displays(); if (!d.length) return null; let x=0,z=0; d.forEach(e=>{x+=e.position.x;z+=e.position.z}); return {x:x/d.length,z:z/d.length}; };

  // (2) atak: bot traci HP (monitor działa też podczas walki poniżej)
  await wf(() => hurtEver, 15000, 300);

  // (3) realny kill mieczem: celuj w encję bazową przy centroidzie displayów
  let killed = false;
  const t0 = Date.now();
  while (Date.now() - t0 < 45000) {
    const c = centr();
    if (!c) { killed = true; break; }   // displaye zniknęły
    const target = Object.values(bot.entities)
      .filter(e => e.id !== bot.entity.id && e.username !== U && e.position && e.name !== 'item_display' && e.name !== 'text_display' && e.name !== 'area_effect_cloud' && e.name !== 'interaction')
      .sort((a,b) => Math.hypot(a.position.x-c.x,a.position.z-c.z) - Math.hypot(b.position.x-c.x,b.position.z-c.z))[0];
    if (target && target.position.distanceTo(bot.entity.position) < 3.5) {
      try { await bot.lookAt(target.position.offset(0,0.8,0), true); bot.attack(target); } catch(e){}
    } else if (target) {
      try { bot.setControlState('forward', true); await bot.lookAt(target.position, true); } catch(e){}
    }
    await sleep(550);
    bot.setControlState('forward', false);
  }
  const active = rcon('mm mobs listactive');
  const gone = /\(0 total\)/.test(active);
  console.log((killed || gone) ? 'OK death: mob zabity realnymi ciosami' : `FAIL death: mob wciąż żyje (${active.slice(0,60)})`);
  console.log(hurtEver ? `OK attack: bot raniony w trakcie walki (HP ${bot.health.toFixed(1)}) → ~onAttack/state attack odpalone` : 'FAIL attack: bot nie otrzymał obrażeń przez całą walkę');
  const hurt = hurtEver;

  // (4) cleanup displayów po animacji death
  await sleep(4000);
  const left = displays().length;
  console.log(left === 0 ? 'OK cleanup: encje display sprzątnięte po death' : `FAIL cleanup: zostało ${left} displayów`);
  rcon('mm mobs killall');
  process.exit((approached && hurt && (killed || gone) && left === 0) ? 0 : 4);
})();
