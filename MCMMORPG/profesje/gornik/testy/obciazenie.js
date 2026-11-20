'use strict';
// Obciazenie gornika: N botow naraz w jednym chodniku: opukanie, urabianie zyly kilofem, przebranie urobku, w kolko przez CZAS.
// Zyly i strop losowe (jak u graczy: zawaly sie zdarzaja, difficulty peaceful, zeby boty nie ginely).
// Pomiar MSPT, encji (ItemDisplay bryl, BlockDisplay gruzu) i pakietow u obserwatora (bot 0, stoi w tym samym chodniku).
// Klon --z-kreatorem z chodnikiem z ../przygotuj_klon.sh. To pomiar kosztu serwera, NIE test sciezki gracza (to urobek.js).
// Uruchom: LIMIT_MS=900000 run.sh <klon> /sciezka/obciazenie.js [botow=4] [praca_s=90] [spoczynek_s=30]
const { scenariusz, sleep, postac } = require('/bot/lib');
const { Vec3 } = require('vec3');

const [, , aN, aCzas, aSpocz] = process.argv;
const N = Number(aN || 4), CZAS = Number(aCzas || 90) * 1000, SPOCZ = Number(aSpocz || 30) * 1000;
const Y = -59;
const PUNKTY = [[201, 99], [208, 99], [201, 105], [208, 105]];   // miekka skala, 4 przodki
const msptTekst = (t) => (t.match(/([\d.]+\/[\d.]+\/[\d.]+, [\d.]+\/[\d.]+\/[\d.]+, [\d.]+\/[\d.]+\/[\d.]+)/) || [, t.trim()])[1];

scenariusz('gornik-obciazenie', async ({ w, polacz, rcon }) => {
  const sfx = String(Date.now() % 10000).padStart(4, '0'), boty = [];
  for (let i = 0; i < N; i++) {
    const nick = `GO${sfx}_${i}`;
    const bot = await polacz({ nick, czasMs: 90000 });
    try { await postac.wejdz(bot, { imie: `Goc${sfx}${i}` }); }
    catch (e) { w.pomin(`wejscie-${i}`, `kreator: ${String(e.message || e).slice(0, 120)}`); try { bot.quit(); } catch {} continue; }
    await rcon(`rzemioslo narzedzie ${nick} kilof 1`); await sleep(300);
    await rcon(`rzemioslo doreki ${nick} PROF_KILOF_1 5`);
    const [px, pz] = PUNKTY[i % PUNKTY.length];
    await rcon(`rzemioslo przodek world ${px} -58 ${pz}`);
    boty.push({ bot, nick, i, px, pz, sesje: 0, wyniki: 0, zawaly: 0, seq: 1 });
    w.sprawdz(`wejscie-${i}`, !!bot.entity, nick);
    await sleep(500);
  }
  const licz = {}; let zbieraj = false;
  boty[0].bot._client.on('packet', (d, meta) => { if (zbieraj) licz[meta.name] = (licz[meta.name] || 0) + 1; });
  await rcon('minecraft:difficulty peaceful');
  await rcon('rzemioslo gornik pomiar off');
  for (const b of boty) await rcon(`execute in minecraft:overworld run minecraft:teleport ${b.nick} ${b.px + 0.5} ${Y} ${b.pz === 99 ? 101.5 : 103.5}`);
  console.log('FAZA spoczynek', new Date().toISOString());
  await sleep(SPOCZ);
  const msptSpoczynek = msptTekst(await rcon('mspt'));
  console.log('MSPT spoczynek (5s,10s,1m avg/min/max):', msptSpoczynek);

  console.log('FAZA praca', new Date().toISOString());
  const koniec = Date.now() + CZAS;
  const jeden = async (b) => {
    const { bot, nick } = b;
    const st = async () => (await rcon(`rzemioslo gornik ${nick}`)).replace(/^\[Rzemioslo\] /, '').trim();
    const tp = (x, z) => rcon(`execute in minecraft:overworld run minecraft:teleport ${nick} ${x + 0.5} ${Y} ${z === 99 ? 101.5 : 103.5} ${z === 99 ? 180 : 0} 0`);
    const patrz = async (x, y, z) => { await bot.lookAt(new Vec3(x, y, z), false); await sleep(160); };
    const kursor = () => {
      const eye = bot.entity.position.offset(0, 1.62, 0), yaw = bot.entity.yaw, pitch = bot.entity.pitch;
      return bot.world.raycast(eye, new Vec3(-Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), -Math.cos(yaw) * Math.cos(pitch)), 5);
    };
    const ppm = () => { const c = kursor(); bot._client.write('block_place', { location: c ? c.position : bot.entity.position.floored(), direction: 3, hand: 0, cursorX: 0.5, cursorY: 0.5, cursorZ: 0.5, insideBlock: false, sequence: b.seq++, worldBorderHit: false }); bot.swingArm(); };
    const face = (z) => z === 99 ? 99.95 : 105.05;
    const zyla = (s) => [...((s.match(/zyla=([^|]*)/) || [])[1] || '').matchAll(/(-?\d+),(-?\d+),(-?\d+)([TM])([uxo-])/g)].map(m => ({ x: +m[1], y: +m[2], z: +m[3], f: m[5] }));
    const kupka = (s) => [...((s.match(/kupka=([^|]*)/) || [])[1] || '').matchAll(/(-?[\d.]+),(-?[\d.]+),(-?[\d.]+)([RKP])([r-])/g)].map(m => ({ x: +m[1], y: +m[2], z: +m[3], typ: m[4], r: m[5] === 'r' }));
    while (Date.now() < koniec) {
      await rcon(`rzemioslo gornik ${nick} odnow`);
      await tp(b.px, b.pz); await sleep(600);
      await patrz(b.px + 0.5, -57.5, face(b.pz)); ppm(); await sleep(500);
      let s = await st();
      if (!/faza=/.test(s)) { await sleep(1000); continue; }
      b.sesje++;
      for (const k of zyla(s)) {
        if (Date.now() > koniec) break;
        await tp(k.x, b.pz); await sleep(300);
        await patrz(k.x + 0.5, k.y + 0.5, face(b.pz)); ppm(); await sleep(350);
        for (let i = 0; i < 4; i++) { await patrz(k.x + 0.5, k.y + 0.5, face(b.pz)); bot.swingArm(); await sleep(470); }
        if (/zawal=true/.test(await st())) b.zawaly++;
      }
      await sleep(3500);   // ewentualny zawal (2,5 s trzeszczenia + obryw)
      for (let n = 0; n < 30 && Date.now() < koniec + 15000; n++) {
        s = await st();
        if (!/faza=PRZEBIERANIE/.test(s)) break;
        const k = kupka(s);
        const g = k.find(x => !x.r) || k.find(x => x.r && x.typ !== 'P');
        if (!g) { bot.setControlState('sneak', true); await sleep(300); bot.setControlState('sneak', false); break; }
        await patrz(g.x, g.y + 0.2, g.z);
        if (!g.r) bot.swingArm(); else ppm();
        await sleep(470);
      }
      if (/^brak sesji/.test(await st())) b.wyniki++;
    }
  };
  const maks = { item_display: 0, block_display: 0 };
  const encje = (async () => {
    const ile = async (typ) => Number(((await rcon(`execute in minecraft:overworld positioned 210 -59 102 if entity @e[type=minecraft:${typ},distance=..60]`)).match(/Count: (\d+)/) || [, 0])[1]);
    while (Date.now() < koniec) {
      maks.item_display = Math.max(maks.item_display, await ile('item_display'));
      maks.block_display = Math.max(maks.block_display, await ile('block_display'));
      await sleep(500);
    }
  })();
  zbieraj = true; const t0 = Date.now();
  await Promise.all([...boty.map(jeden), encje]);
  zbieraj = false; const sek = (Date.now() - t0) / 1000;
  const msptPraca = msptTekst(await rcon('mspt'));
  console.log('MSPT praca (5s,10s,1m avg/min/max):', msptPraca);
  const p = (n) => ((licz[n] || 0) / sek).toFixed(1);
  const enc = ['block_change', 'block_break_animation', 'spawn_entity', 'entity_metadata', 'entity_destroy', 'world_particles', 'sound_effect', 'action_bar', 'system_chat']
    .map(n => `${n}=${p(n)}/s`).join(' ');
  const wszystkie = (Object.values(licz).reduce((a, c) => a + c, 0) / sek).toFixed(0);
  console.log(`obserwator ${boty[0] && boty[0].nick}: ${enc} wszystkie=${wszystkie}/s`);
  console.log('praca:', boty.map(b => `${b.nick}=sesje ${b.sesje} wyniki ${b.wyniki} zawaly ${b.zawaly}`).join(' '), 'max naraz:', JSON.stringify(maks));
  w.sprawdz('pomiar', boty.some(b => b.wyniki > 0), `N=${boty.length} spoczynek ${msptSpoczynek} | praca ${msptPraca} | encje ${JSON.stringify(maks)} | pakiety ${wszystkie}/s`);
  for (const b of boty) { try { b.bot.quit(); } catch {} }
}, { limitMs: Number(process.env.LIMIT_MS || 900000) });
