'use strict';
// Obciazenie drwala: N botow scina naraz (wrab, rzaz, upadek BlockDisplayami widocznymi dla wszystkich, obrobka), pomiar MSPT
// i pakietow encji u obserwatora. Klon --z-kreatorem z terenem z MCMMORPG/profesje/drwal/przygotuj_klon.sh.
// Uruchom: LIMIT_MS=900000 run.sh <klon> /sciezka/obciazenie.js [botow=4] [scinka_s=90] [spoczynek_s=40]
// To pomiar kosztu serwera, NIE test sciezki gracza (sciezka: scinka.js). RCON: tp, siekiera, cel obrobki, mspt.
const { scenariusz, sleep, postac } = require('/bot/lib');
const { Vec3 } = require('vec3');

const [, , aN, aCzas, aSpocz] = process.argv;
const N = Number(aN || 4), CZAS = Number(aCzas || 90) * 1000, SPOCZ = Number(aSpocz || 40) * 1000;
const Y = -59;
const DRZEWA = [[110, 40], [125, 40], [140, 40], [110, 52], [125, 52], [110, 64], [125, 64], [140, 64]];   // pojedyncze, upadek na wschod
const msptTekst = (t) => (t.match(/([\d.]+\/[\d.]+\/[\d.]+, [\d.]+\/[\d.]+\/[\d.]+, [\d.]+\/[\d.]+\/[\d.]+)/) || [, t.trim()])[1];

scenariusz('drwal-obciazenie', async ({ w, polacz, rcon }) => {
  const sfx = String(Date.now() % 10000).padStart(4, '0'), boty = [];
  for (let i = 0; i < N; i++) {
    const nick = `DO${sfx}_${i}`;
    const bot = await polacz({ nick, czasMs: 90000 });
    try { await postac.wejdz(bot, { imie: `Dro${sfx}${i}` }); }
    catch (e) { w.pomin(`wejscie-${i}`, `kreator: ${String(e.message || e).slice(0, 120)}`); try { bot.quit(); } catch {} continue; }
    await rcon(`rzemioslo narzedzie ${nick} siekiera 1`);
    await rcon(`rzemioslo doreki ${nick} PROF_SIEKIERA_1 5`);
    await rcon(`execute in minecraft:overworld run minecraft:teleport ${nick} 125 ${Y} 46 0 0`);
    boty.push({ bot, nick, i, sciete: 0, seq: 1 });
    w.sprawdz(`wejscie-${i}`, !!bot.entity, nick);
    await sleep(500);
  }
  const licz = {}; let zbieraj = false;
  boty[0].bot._client.on('packet', (d, meta) => { if (zbieraj) licz[meta.name] = (licz[meta.name] || 0) + 1; });
  await rcon('minecraft:difficulty peaceful');
  console.log('FAZA spoczynek', new Date().toISOString());
  await sleep(SPOCZ);
  const msptSpoczynek = msptTekst(await rcon('mspt'));
  console.log('MSPT spoczynek (5s,10s,1m avg/min/max):', msptSpoczynek);

  console.log('FAZA scinka', new Date().toISOString());
  const koniec = Date.now() + CZAS; let maxDisplay = 0;
  const jeden = async (b) => {
    const { bot, nick } = b;
    const patrz = async (x, y, z) => { await bot.lookAt(new Vec3(x, y, z), false); await sleep(160); };
    const st = async () => rcon(`rzemioslo drwal ${nick}`);
    let k = b.i * 2;
    while (Date.now() < koniec) {
      const [x, z] = DRZEWA[k++ % DRZEWA.length];
      await rcon(`execute in minecraft:overworld run minecraft:teleport ${nick} ${x + 1.8} ${Y} ${z + 0.5} 90 20`);
      await sleep(700);
      for (let c = 0; c < 6; c++) {   // wrab od wschodu
        await patrz(x + 0.5, Y + 0.55, z + 0.5);
        const bl = bot.blockAtCursor(5);
        if (bl) {
          bot._client.write('block_dig', { status: 0, location: bl.position, face: 1, sequence: b.seq++ });
          bot.swingArm(); await sleep(60);
          bot._client.write('block_dig', { status: 1, location: bl.position, face: 1, sequence: b.seq++ });
        }
        await sleep(450);
      }
      if (!/faza=WRAB/.test(await st())) { await sleep(1000); continue; }   // pniak z poprzedniej rundy / zajete
      await rcon(`execute in minecraft:overworld run minecraft:teleport ${nick} ${x - 1.4} ${Y} ${z + 0.5} 270 20`);
      await sleep(500);
      await patrz(x + 0.5, Y + 0.85, z + 0.5);
      const t = Date.now() + 3300;
      while (Date.now() < t) {
        const bl = bot.blockAtCursor(5); const loc = bl ? bl.position : bot.entity.position.floored();
        bot._client.write('block_place', { location: loc, direction: 1, hand: 0, cursorX: 0.5, cursorY: 0.5, cursorZ: 0.5, insideBlock: false, sequence: b.seq++, worldBorderHit: false });
        bot.swingArm(); await sleep(200);
      }
      const k0 = Date.now() + 9000; let s = '';
      while (Date.now() < k0) { s = await st(); if (/faza=(LEZY|WISI)|brak sesji/.test(s)) break; await sleep(300); }
      if (/faza=LEZY/.test(s)) {
        const seg = Number((s.match(/segmenty=(\d+)/) || [])[1] || 0);
        for (let g = 0; g < seg; g++) {
          const c = (await rcon(`rzemioslo drwal ${nick} cel ${g}`)).match(/cel=([-\d.]+),([-\d.]+),([-\d.]+) bok=([-\d.]+),([-\d.]+)/);
          if (!c) break;
          const [cx, cy, cz, bx, bz] = c.slice(1).map(Number);
          await rcon(`execute in minecraft:overworld run minecraft:teleport ${nick} ${cx + bx * 2} ${Y} ${cz + bz * 2}`);
          await sleep(400);
          const c2 = (await rcon(`rzemioslo drwal ${nick} cel ${g}`)).match(/cel=([-\d.]+),([-\d.]+),([-\d.]+)/);
          for (let i = 0; i < (g === 0 ? 3 : 2); i++) { await patrz(+c2[1], +c2[2], +c2[3]); bot.swingArm(); await sleep(450); }
        }
      }
      const k1 = Date.now() + 8000;
      while (Date.now() < k1) { if (/brak sesji/.test(await st())) { b.sciete++; break; } await sleep(400); }
    }
  };
  const encje = (async () => {
    while (Date.now() < koniec) {
      const t = await rcon('execute in minecraft:overworld positioned 125 -59 55 if entity @e[type=minecraft:block_display,distance=..60]');
      maxDisplay = Math.max(maxDisplay, Number((t.match(/Count: (\d+)/) || [, 0])[1]));
      await sleep(1000);
    }
  })();
  zbieraj = true; const t0 = Date.now();
  await Promise.all([...boty.map(jeden), encje]);
  zbieraj = false; const sek = (Date.now() - t0) / 1000;
  const msptScinka = msptTekst(await rcon('mspt'));
  console.log('MSPT scinka (5s,10s,1m avg/min/max):', msptScinka);
  const p = (n) => ((licz[n] || 0) / sek).toFixed(1);
  const enc = ['spawn_entity', 'entity_metadata', 'entity_destroy', 'entity_teleport', 'sync_entity_position', 'rel_entity_move', 'world_particles', 'block_change', 'sound_effect']
    .map(n => `${n}=${p(n)}/s`).join(' ');
  const wszystkie = (Object.values(licz).reduce((a, c) => a + c, 0) / sek).toFixed(0);
  console.log(`obserwator ${boty[0] && boty[0].nick}: ${enc} wszystkie=${wszystkie}/s`);
  console.log('scietych:', boty.map(b => `${b.nick}=${b.sciete}`).join(' '), 'max block_display naraz:', maxDisplay);
  w.sprawdz('pomiar', boty.some(b => b.sciete > 0), `N=${boty.length} spoczynek ${msptSpoczynek} | scinka ${msptScinka} | display max ${maxDisplay} | pakiety ${wszystkie}/s`);
  for (const b of boty) { try { b.bot.quit(); } catch {} }
}, { limitMs: Number(process.env.LIMIT_MS || 900000) });
