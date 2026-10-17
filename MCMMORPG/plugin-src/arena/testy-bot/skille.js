'use strict';
// Luka 3: skille klasy MMOCore (tryb paska: F + klawisze 1-4) na graczu: ring (rani), za lina i w world (zero).
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, TRYBUNY, W_RINGU, sleep } = require('./arena-lib');
const { Vec3 } = require('vec3');
scenariusz('arena-skille', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const m = await polacz({ nick: 'ArenaMag' }); const b = await polacz({ nick: 'ArenaB' });
  const czat = []; m.on('actionBar', x => czat.push(String(x?.toString?.() ?? x))); m.on('messagestr', x => czat.push(x));
  await t.przygotuj();
  await rcon('zaklecia przypisz ArenaMag nic lewa'); await rcon('zaklecia przypisz ArenaMag nic prawa');
  await rcon('mmocore admin class ArenaMag MAG_AKADEMII'); await rcon('mmocore admin level set ArenaMag main 30'); await sleep(1500);
  await rcon('mmocore admin nocd ArenaMag');
  for (const [i, sk] of [[1, 'MAG_PIECZEC_WIEZY'], [2, 'MAG_BARIERA'], [3, 'MAG_WYBUCH'], [4, 'MAG_MRUGNIECIE']]) { await rcon(`mmocore admin slot unlock ArenaMag ${i}`); console.log('bind', i, sk, (await rcon(`mmocore admin slot bind ArenaMag ${i} ${sk}`)).trim().slice(0, 90)); }
  for (let s = 1; s <= 4; s++) console.log('slot', s, (await rcon(`papi parse ArenaMag %mmocore_bound_${s}%`)).trim());
  const F = () => m._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 });
  const seria = async (opis, pm, pb) => {
    for (const [n, p] of [['ArenaMag', pm], ['ArenaB', pb]]) {
      const swiat = p.swiat || 'arena';
      if (swiat === 'arena' && !(await t.wymiar(n)).includes('arena')) { await t.wWorld(n, n === 'ArenaMag' ? 1.5 : 4.5); await t.wejdz(n); }
      if (swiat === 'world' && (await t.wymiar(n)).includes('arena')) { await rcon(`arena wyjdz ${n}`); await sleep(1500); }
      if (swiat === 'arena') await t.tpArena(n, p); else await t.tpWorld(n, p);
    }
    await sleep(6500); await t.uleczy('ArenaB');
    let suma = 0;
    for (let s = 0; s < 4; s++) {
      await rcon('mmocore admin resource-mana set ArenaMag 1000');
      const pc = await t.poz('ArenaB'); await m.lookAt(new Vec3(pc.x, pc.y + 1.0, pc.z), true); await sleep(200);
      czat.length = 0; F(); await sleep(400); m.setQuickBarSlot(s); await sleep(300); F(); await sleep(1800);
      const h1 = await t.hp('ArenaB'); const d = +(20 - h1).toFixed(2); suma += d;
      console.log(opis, 'skill', s + 1, '| HP B', h1, '| pasek:', JSON.stringify(czat.filter(x => x.trim()).slice(-2)).slice(0, 140));
      await t.uleczy('ArenaB'); m.setQuickBarSlot(0); await sleep(600);
    }
    return +suma.toFixed(2);
  };
  const dr = await seria('ring', { ...RING, z: -3.5 }, { ...RING, z: -0.5 });
  w.sprawdz('skille-ring', dr > 0, 'skille klasy ranią gracza w ringu', dr);
  await sleep(11000);
  const dt = await seria('za lina', { ...W_RINGU, z: -16.5 }, TRYBUNY);
  w.sprawdz('skille-za-lina', dr > 0 && dt === 0, 'skille z ringu nie ranią widza za lina', dt);
  const dw = await seria('world', { x: 1.5, y: -60, z: -0.5, swiat: 'world' }, { x: 1.5, y: -60, z: 2.5, swiat: 'world' });
  w.sprawdz('skille-world', dr > 0 && dw === 0, 'skille w world nie ranią gracza', dw);
  await rcon('mmocore admin class ArenaMag HUMAN'); await sleep(11000);
}, { limitMs: 300000 });
