'use strict';
// Zadanie startowe zielarza (herbalism: zbierz PROF_MEDICINAL_HERB x5) liczy zbior dziurawca ZE SCENY (Zielarz.daj -> Zlecenia.zebrano).
// Przed poprawka 16.11 scena nie wolala zebrano(): zadanie startowe stalo na 0 mimo pelnego kosza.
// Klon --z-kreatorem z terenem zielarza (przygotuj_klon.sh; kepy dziurawca 42,42 45,44 48,41 44,48 50,46 na Y -59).
// Uruchom: LIMIT_MS=300000 /opt/docker/mc/narzedzia/bot/run.sh <klon> zadanie-startowe.js
// Gracz bez OP. Przez RCON tylko przygotowanie: przyjecie zadania (rzemioslo start), sierp, tp, odrost, celowanie kamery.
// Sesje bot zaczyna PPM w blok kepy, pedy tnie LPM (machniecie) — ta sama droga co gracz.
// Postep zadania sprawdz na hoście: plugins/Rzemioslo/dane/<profil>.yml -> start.herbalism.postep (scenariusz wypisuje nick).
const { scenariusz, sleep, postac } = require('/bot/lib');
const Vec3 = require('vec3');

const KEPY = [[42, 42], [45, 44], [48, 41], [44, 48], [50, 46]];
const Y = -59;

scenariusz('zielarz-zadanie-startowe', async ({ w, polacz, rcon }) => {
  const sfx = String(Date.now() % 10000).padStart(4, '0'), nick = `ZS${sfx}`;
  const bot = await polacz({ nick, czasMs: 90000 });
  const d = await postac.wejdz(bot, { imie: `Zs${sfx}` });
  w.sprawdz('wejscie', !!bot.entity, `${nick} droga=${d.droga}`);
  console.log('NICK', nick);
  console.log('START', await rcon(`rzemioslo start ${nick} herbalism`));
  await rcon(`rzemioslo narzedzie ${nick} sierp 1`);
  await sleep(1000);
  const herb = () => bot.inventory.items().filter(i => JSON.stringify(i.components || i.nbt || '').includes('PROF_MEDICINAL_HERB')).reduce((a, i) => a + i.count, 0);
  let sesje = 0, ciec = 0, zPpm = 0;
  for (const [x, z] of KEPY) {
    if (herb() >= 5) break;
    await rcon(`rzemioslo odrost ${nick}`);
    await rcon(`execute in minecraft:overworld run minecraft:teleport ${nick} ${x + 0.5} ${Y} ${z - 1.5} 0 40`);
    await sleep(1500);
    const blok = bot.blockAt(new Vec3(x, Y, z));
    let st = '';
    if (blok) {
      await bot.lookAt(new Vec3(x + 0.5, Y + 0.3, z + 0.5), true);
      try { await bot.activateBlock(blok); } catch (e) { console.log('activateBlock', String(e.message || e).slice(0, 80)); }
      await sleep(1500);
      st = await rcon(`rzemioslo okaz ${nick}`);
    }
    if (!st || /brak sesji/.test(st)) {   // PPM nie otworzyl sceny: zapasowo komenda (odnotowane w wyniku)
      console.log('PPM nie otworzyl sesji przy', x, z, '-> kepa-start');
      st = await rcon(`rzemioslo kepa-start ${nick} ${x} ${Y} ${z}`);
      await sleep(1500);
    } else zPpm++;
    if (/brak sesji/.test(await rcon(`rzemioslo okaz ${nick}`))) { w.pomin(`sesja-${x}-${z}`, st.slice(0, 120)); continue; }
    sesje++;
    for (let i = 0; i < 12; i++) {
      const c = await rcon(`rzemioslo celuj ${nick} ped -1 0.5`);
      if (/brak/.test(c)) break;
      const h = Number((c.match(/h=([\d.,]+)/) || [, '0.8'])[1].replace(',', '.'));
      const c2 = await rcon(`rzemioslo celuj ${nick} ped -1 ${(h * 0.55).toFixed(2)}`);   // strefa kwiatow (segment 3 z 4)
      const m = c2.match(/cel=([-\d.]+),([-\d.]+),([-\d.]+)/);
      if (m) await bot.lookAt(new Vec3(+m[1], +m[2], +m[3]), true);
      await sleep(400);
      bot.swingArm('right'); ciec++;
      await sleep(900);
    }
    await sleep(3000);   // lot do reki, koniec sesji
    if (!/brak sesji/.test(await rcon(`rzemioslo okaz ${nick}`))) { bot.setControlState('sneak', true); await sleep(300); bot.setControlState('sneak', false); await sleep(1500); }
  }
  const ile = herb();
  w.sprawdz('zbior', ile > 0, `dziurawiec w EQ: ${ile} (sesje ${sesje}, z PPM ${zPpm}, ciec ${ciec})`);
  console.log('HERB', ile);
  await sleep(2000);
  try { bot.quit(); } catch {}
  await sleep(2000);
}, { limitMs: Number(process.env.LIMIT_MS || 300000) });
