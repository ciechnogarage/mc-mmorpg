'use strict';
// Obciazenie rybaka: N botow lowi naraz w stawie T1 (zapowiedz, hol z zrywami, wyskoki ryb ItemDisplayem widocznym
// dla wszystkich), pomiar MSPT i pakietow encji u obserwatora. Klon --z-kreatorem z terenem z ../przygotuj_klon.sh.
// Uruchom: LIMIT_MS=900000 run.sh <klon> /sciezka/obciazenie.js [botow=4] [lowienie_s=90] [spoczynek_s=40]
// To pomiar kosztu serwera, NIE test sciezki gracza (sciezka: lowienie.js). RCON: tp, wedka, wymus (okaz/ryba = najwiecej wyskokow), mspt.
const { scenariusz, sleep, postac } = require('/bot/lib');
const { Vec3 } = require('vec3');

const [, , aN, aCzas, aSpocz] = process.argv;
const N = Number(aN || 4), CZAS = Number(aCzas || 90) * 1000, SPOCZ = Number(aSpocz || 40) * 1000;
const Y = -59;
const msptTekst = (t) => (t.match(/([\d.]+\/[\d.]+\/[\d.]+, [\d.]+\/[\d.]+\/[\d.]+, [\d.]+\/[\d.]+\/[\d.]+)/) || [, t.trim()])[1];

scenariusz('rybak-obciazenie', async ({ w, polacz, rcon }) => {
  const sfx = String(Date.now() % 10000).padStart(4, '0'), boty = [];
  for (let i = 0; i < N; i++) {
    const nick = `RO${sfx}_${i}`;
    const bot = await polacz({ nick, czasMs: 90000 });
    try { await postac.wejdz(bot, { imie: `Rob${sfx}${i}` }); }
    catch (e) { w.pomin(`wejscie-${i}`, `kreator: ${String(e.message || e).slice(0, 120)}`); try { bot.quit(); } catch {} continue; }
    await rcon(`rzemioslo narzedzie ${nick} wedka 1`);
    await rcon(`rzemioslo doreki ${nick} PROF_WEDKA_1 5`);
    boty.push({ bot, nick, i, zlowione: 0, proby: 0, z: 34.5 + i * 6 });
    w.sprawdz(`wejscie-${i}`, !!bot.entity, nick);
    await sleep(500);
  }
  const licz = {}; let zbieraj = false;
  boty[0].bot._client.on('packet', (d, meta) => { if (zbieraj) licz[meta.name] = (licz[meta.name] || 0) + 1; });
  await rcon('minecraft:difficulty peaceful');
  for (const b of boty) await rcon(`execute in minecraft:overworld run minecraft:teleport ${b.nick} 108.5 ${Y} ${b.z} -90 20`);
  console.log('FAZA spoczynek', new Date().toISOString());
  await sleep(SPOCZ);
  const msptSpoczynek = msptTekst(await rcon('mspt'));
  console.log('MSPT spoczynek (5s,10s,1m avg/min/max):', msptSpoczynek);

  console.log('FAZA lowienie', new Date().toISOString());
  const koniec = Date.now() + CZAS; let maxDisplay = 0;
  const jeden = async (b) => {
    const { bot, nick } = b;
    const st = async () => (await rcon(`rzemioslo rybak ${nick}`)).replace(/^\[Rzemioslo\] /, '');
    const pole = (s, k) => { const m = s.match(new RegExp(`${k}=([-\\w.,]+)`)); return m ? m[1] : null; };
    const czekaj = async (re, ms) => { const k = Date.now() + ms; let s = ''; while (Date.now() < k) { s = await st(); if (re.test(s)) return s; await sleep(150); } return s; };
    let n = 0;
    while (Date.now() < koniec) {
      bot.setControlState('sneak', false);
      await bot.lookAt(new Vec3(116, -60, b.z), false); await sleep(160);
      if (!/brak sesji/.test(await st())) { bot.activateItem(); await sleep(500); }
      bot.activateItem(); b.proby++;
      if (!/faza=CZEKA/.test(await czekaj(/faza=CZEKA/, 4000))) { await sleep(500); continue; }
      await rcon(`rzemioslo rybak ${nick} wymus ${n++ % 2 ? 'ryba' : 'okaz'} 5`);
      if (!/faza=OKNO/.test(await czekaj(/faza=OKNO/, 5000))) continue;
      bot.activateItem();
      const k = Date.now() + 60000;
      while (Date.now() < k) {
        const s = await st();
        if (/brak sesji/.test(s)) break;
        const nap = Number(pole(s, 'nap')), kier = Number(pole(s, 'kier')), zryw = /zryw=true|zapow=true/.test(s);
        const [hx, hy, hz] = (pole(s, 'hak') || '0,0,0').split(',').map(Number);
        const p = bot.entity.position; let fx = hx - p.x, fz = hz - p.z; const l = Math.hypot(fx, fz) || 1; fx /= l; fz /= l;
        await bot.lookAt(zryw ? new Vec3(hx + fz * kier * 4, hy, hz - fx * kier * 4) : new Vec3(hx, hy, hz), false);
        bot.setControlState('sneak', !(nap > 0.62 || /zryw=true/.test(s)));
        await sleep(150);
      }
      bot.setControlState('sneak', false);
      b.zlowione++;
      await sleep(400);
    }
  };
  const encje = (async () => {
    while (Date.now() < koniec) {
      const t = await rcon('execute in minecraft:overworld positioned 120 -59 45 if entity @e[type=minecraft:item_display,distance=..60]');
      maxDisplay = Math.max(maxDisplay, Number((t.match(/Count: (\d+)/) || [, 0])[1]));
      await sleep(500);
    }
  })();
  zbieraj = true; const t0 = Date.now();
  await Promise.all([...boty.map(jeden), encje]);
  zbieraj = false; const sek = (Date.now() - t0) / 1000;
  const msptLow = msptTekst(await rcon('mspt'));
  console.log('MSPT lowienie (5s,10s,1m avg/min/max):', msptLow);
  const p = (n) => ((licz[n] || 0) / sek).toFixed(1);
  const enc = ['spawn_entity', 'entity_metadata', 'entity_destroy', 'entity_teleport', 'sync_entity_position', 'rel_entity_move', 'entity_velocity', 'world_particles', 'sound_effect']
    .map(n => `${n}=${p(n)}/s`).join(' ');
  const wszystkie = (Object.values(licz).reduce((a, c) => a + c, 0) / sek).toFixed(0);
  console.log(`obserwator ${boty[0] && boty[0].nick}: ${enc} wszystkie=${wszystkie}/s`);
  console.log('hole:', boty.map(b => `${b.nick}=${b.zlowione}/${b.proby}`).join(' '), 'max item_display naraz:', maxDisplay);
  w.sprawdz('pomiar', boty.some(b => b.zlowione > 0), `N=${boty.length} spoczynek ${msptSpoczynek} | lowienie ${msptLow} | display max ${maxDisplay} | pakiety ${wszystkie}/s`);
  for (const b of boty) { try { b.bot.quit(); } catch {} }
}, { limitMs: Number(process.env.LIMIT_MS || 900000) });
