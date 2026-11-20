'use strict';
// Obciazenie mysliwego: N botow naraz tropi, podchodzi, zabija i oprawia (5 ciec) dzika, w kolko przez CZAS.
// Pomiar MSPT, encji (zwierzeta, Interaction/ItemDisplay zwlok) i pakietow encji/czasteczek u obserwatora (bot 0).
// Klon --z-kreatorem z placem z ../przygotuj_klon.sh. To pomiar kosztu serwera, NIE test sciezki gracza (to lowy.js).
// Uruchom: LIMIT_MS=900000 run.sh <klon> /sciezka/obciazenie.js [botow=4] [lowy_s=90] [spoczynek_s=30]
const { scenariusz, sleep, postac } = require('/bot/lib');
const { Vec3 } = require('vec3');

const [, , aN, aCzas, aSpocz] = process.argv;
const N = Number(aN || 4), CZAS = Number(aCzas || 90) * 1000, SPOCZ = Number(aSpocz || 30) * 1000;
const Y = -59;
const msptTekst = (t) => (t.match(/([\d.]+\/[\d.]+\/[\d.]+, [\d.]+\/[\d.]+\/[\d.]+, [\d.]+\/[\d.]+\/[\d.]+)/) || [, t.trim()])[1];

scenariusz('mysliwy-obciazenie', async ({ w, polacz, rcon }) => {
  const sfx = String(Date.now() % 10000).padStart(4, '0'), boty = [];
  for (let i = 0; i < N; i++) {
    const nick = `MO${sfx}_${i}`;
    const bot = await polacz({ nick, czasMs: 90000 });
    try { await postac.wejdz(bot, { imie: `Moc${sfx}${i}` }); }
    catch (e) { w.pomin(`wejscie-${i}`, `kreator: ${String(e.message || e).slice(0, 120)}`); try { bot.quit(); } catch {} continue; }
    await rcon(`rzemioslo narzedzie ${nick} noz 1`);
    await rcon(`rzemioslo doreki ${nick} PROF_NOZ_1 5`);
    boty.push({ bot, nick, i, upolowane: 0, oprawione: 0, proby: 0, z: 75 + i * 35 });
    w.sprawdz(`wejscie-${i}`, !!bot.entity, nick);
    await sleep(500);
  }
  const licz = {}; let zbieraj = false;
  boty[0].bot._client.on('packet', (d, meta) => { if (zbieraj) licz[meta.name] = (licz[meta.name] || 0) + 1; });
  await rcon('minecraft:difficulty peaceful');
  await rcon('rzemioslo mysliwy pomiar off');
  for (const b of boty) await rcon(`execute in minecraft:overworld run minecraft:teleport ${b.nick} 240 ${Y} ${b.z}`);
  console.log('FAZA spoczynek', new Date().toISOString());
  await sleep(SPOCZ);
  const msptSpoczynek = msptTekst(await rcon('mspt'));
  console.log('MSPT spoczynek (5s,10s,1m avg/min/max):', msptSpoczynek);

  console.log('FAZA lowy', new Date().toISOString());
  const koniec = Date.now() + CZAS;
  const jeden = async (b) => {
    const { bot, nick } = b;
    const st = async () => (await rcon(`rzemioslo mysliwy ${nick}`)).replace(/^\[Rzemioslo\] /, '').trim();
    const pole = (s, k) => { const m = s.match(new RegExp(`${k}=([-\\w.,;@+]+)`)); return m ? m[1] : null; };
    const xyz = (t) => t.split(',').map(Number);
    const tp = (x, y, z) => rcon(`execute in minecraft:overworld run minecraft:teleport ${nick} ${x} ${y} ${z}`);
    const patrz = async (x, y, z) => { await bot.lookAt(new Vec3(x, y, z), false); await sleep(160); };
    const czekaj = async (re, ms) => { const k = Date.now() + ms; let s = ''; while (Date.now() < k) { s = await st(); if (re.test(s)) return s; await sleep(200); } return s; };
    let rx = 190, nr = 0;
    while (Date.now() < koniec) {
      rx = 190 + (nr++ % 8) * 14;   // rewir po uzyciu odpoczywa 20 s -> 8 rewirow w rzedzie
      await rcon(`rzemioslo rewir world ${rx} ${Y} ${b.z} BEAST_WILD_BOAR`);
      bot.setControlState('sneak', false);
      await tp(rx + 1.5, Y, b.z + 0.5); await sleep(500);
      bot.setControlState('sneak', true); await sleep(400); b.proby++;
      let s = await czekaj(/faza=TROP/, 3000);
      if (!/faza=TROP/.test(s)) { await sleep(1000); continue; }
      await rcon(`rzemioslo mysliwy ${nick} wiatr 0`);
      for (let i = 0; i < 8 && /faza=TROP/.test(s); i++) {
        const [tx, ty, tz] = xyz(pole(s, 'tropXYZ'));
        await tp(tx, ty, tz); await sleep(500);
        s = await st();
      }
      s = await czekaj(/faza=PODCHOD/, 3000);
      const t = pole(s, 'zwierzeXYZ');
      if (!t) continue;
      const [ax, ay, az] = xyz(t);
      await rcon(`rzemioslo mysliwy ${nick} wiatr 0`);
      await tp(ax + 3, ay, az); await sleep(500);
      await patrz(ax, ay + 0.4, az);
      const e = bot.nearestEntity(x => x.name === 'pig' && x.position.distanceTo(new Vec3(ax, ay, az)) < 3);
      if (!e) continue;
      await bot.attack(e); await sleep(600);
      s = await st();
      const zt = pole(s, 'zwlokiXYZ');
      if (!zt) continue;
      b.upolowane++;
      const zl = xyz(zt);
      bot.setControlState('sneak', false);
      await tp(zl[0] + 1.6, zl[1], zl[2]); await sleep(700);
      const it = bot.nearestEntity(x => x.name === 'interaction' && x.position.distanceTo(new Vec3(...zl)) < 1.5);
      for (let i = 0; i < 5 && it; i++) {
        const c = (pole(await st(), 'ciecia') || '').split(';').filter(Boolean)[i];
        if (!c) break;
        const [px, py, pz] = xyz(c.split('@')[1]);
        await patrz(px, py, pz); await sleep(150); await patrz(px, py, pz);
        bot._client.write('use_entity', { target: it.id, mouse: 0, sneaking: false, hand: 0, location: new Vec3(0, 0, 0) });
        await sleep(1150);
      }
      if (!/zwloki typ/.test(await st())) b.oprawione++;
    }
    bot.setControlState('sneak', false);
  };
  const maks = { zwierzeta: 0, interaction: 0, item_display: 0 };
  const encje = (async () => {
    const ile = async (typ) => Number(((await rcon(`execute in minecraft:overworld positioned 240 -59 130 if entity @e[type=minecraft:${typ},distance=..120]`)).match(/Count: (\d+)/) || [, 0])[1]);
    while (Date.now() < koniec) {
      maks.zwierzeta = Math.max(maks.zwierzeta, await ile('pig'));
      maks.interaction = Math.max(maks.interaction, await ile('interaction'));
      maks.item_display = Math.max(maks.item_display, await ile('item_display'));
      await sleep(500);
    }
  })();
  zbieraj = true; const t0 = Date.now();
  await Promise.all([...boty.map(jeden), encje]);
  zbieraj = false; const sek = (Date.now() - t0) / 1000;
  const msptLow = msptTekst(await rcon('mspt'));
  console.log('MSPT lowy (5s,10s,1m avg/min/max):', msptLow);
  const p = (n) => ((licz[n] || 0) / sek).toFixed(1);
  const enc = ['spawn_entity', 'entity_metadata', 'entity_destroy', 'entity_teleport', 'sync_entity_position', 'rel_entity_move', 'entity_move_look', 'entity_velocity', 'world_particles', 'sound_effect']
    .map(n => `${n}=${p(n)}/s`).join(' ');
  const wszystkie = (Object.values(licz).reduce((a, c) => a + c, 0) / sek).toFixed(0);
  console.log(`obserwator ${boty[0] && boty[0].nick}: ${enc} wszystkie=${wszystkie}/s`);
  console.log('lowy:', boty.map(b => `${b.nick}=${b.upolowane}/${b.proby} oprawione=${b.oprawione}`).join(' '), 'max naraz:', JSON.stringify(maks));
  w.sprawdz('pomiar', boty.some(b => b.oprawione > 0), `N=${boty.length} spoczynek ${msptSpoczynek} | lowy ${msptLow} | encje ${JSON.stringify(maks)} | pakiety ${wszystkie}/s`);
  for (const b of boty) { try { b.bot.quit(); } catch {} }
}, { limitMs: Number(process.env.LIMIT_MS || 900000) });
