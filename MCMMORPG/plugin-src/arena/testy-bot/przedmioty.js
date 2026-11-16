'use strict';
// I: przedmioty i swiat: wyrzucanie, bloki, wiadra, lodz, mikstury przez line.
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, TRYBUNY, W_RINGU, sleep } = require('./arena-lib');
const { Vec3 } = require('vec3');
scenariusz('arena-przedmioty', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const a = await polacz({ nick: 'ArenaA' }); const b = await polacz({ nick: 'ArenaB' });
  await t.przygotuj();
  for (const n of ['ArenaA', 'ArenaB']) if (!(await t.wymiar(n)).includes('arena')) { await t.wWorld(n, n === 'ArenaA' ? 1.5 : 4.5); await t.wejdz(n); }
  const M = 'minecraft:execute in minecraft:arena run minecraft:';
  const daj = async (n, item) => { await rcon(`minecraft:item replace entity ${n} container.0 with ${item}`); await sleep(500); a.setQuickBarSlot(0); b.setQuickBarSlot(0); await sleep(300); };
  const ma = async (n, pred) => ((await rcon(`execute if items entity ${n} container.* ${pred}`)).match(/count: (\d+)/i) || [0, '0'])[1];
  await t.tpArena('ArenaA', { ...RING, z: -5.5 }, 0); await t.tpArena('ArenaB', { ...RING, z: -1.5 }, 180); await sleep(6500);
  // wyrzucenie
  await daj('ArenaA', 'minecraft:dirt[minecraft:custom_name="qa"] 5');
  await a.tossStack(a.inventory.slots[36]).catch(() => {}); await sleep(1200);
  const naZiemi = await rcon(`${M}execute if entity @e[type=minecraft:item,x=0.5,y=64,z=-5.5,distance=..6]`);
  w.sprawdz('wyrzucenie', !/passed/i.test(naZiemi) && (await ma('ArenaA', 'minecraft:dirt')) === '5', 'wyrzucony przedmiot zostaje w ekwipunku', `${naZiemi} / ${await ma('ArenaA', 'minecraft:dirt')}`);
  // stawianie bloku na podlodze ringu
  await daj('ArenaA', 'minecraft:stone 8');
  const pod = a.blockAt(new Vec3(1, 63, -6)); await a.lookAt(pod.position.offset(0.5, 1, 0.5), true);
  await a.placeBlock(pod, new Vec3(0, 1, 0)).catch(e => console.log('place:', e.message)); await sleep(800);
  const blok = await rcon(`${M}execute if block 1 64 -6 minecraft:stone`);
  w.sprawdz('stawianie', !/passed/i.test(blok), 'nie da sie postawic bloku', blok);
  // kopanie podlogi
  await daj('ArenaA', 'minecraft:diamond_pickaxe');
  await rcon('minecraft:gamemode survival ArenaA');
  const cel = a.blockAt(new Vec3(1, 63, -6)); await a.dig(cel, true).catch(e => console.log('dig:', e.message)); await sleep(800);
  const podloga = await rcon(`${M}execute if block 1 63 -6 minecraft:air`);
  w.sprawdz('kopanie', !/passed/i.test(podloga), 'podlogi ringu nie da sie zniszczyc', podloga);
  // lawa z wiadra
  await daj('ArenaA', 'minecraft:lava_bucket');
  await a.lookAt(new Vec3(1.5, 64, -6.5), true); a.activateItem(); await sleep(800); a.deactivateItem();
  const lawa = await rcon(`${M}execute if blocks 0 64 -9 3 66 -3 0 64 -9 3 66 -3 all`); // tylko log
  const lawaJest = await rcon(`${M}execute if block 1 64 -6 minecraft:lava`);
  w.sprawdz('lawa', !/passed/i.test(lawaJest) && (await ma('ArenaA', 'minecraft:lava_bucket')) === '1', 'wiadro lawy nie dziala', lawaJest);
  // lodz
  await daj('ArenaA', 'minecraft:oak_boat'); a.activateItem(); await sleep(800);
  const lodz = await rcon(`${M}execute if entity @e[type=minecraft:oak_boat,x=0.5,y=64,z=-5.5,distance=..8]`);
  w.sprawdz('lodz', !/passed/i.test(lodz), 'lodzi nie da sie postawic', lodz);
  // mikstura krzywdy: z trybun do ringu (0), w ringu (rani)
  const rzut = async (opis, pa, pb) => {
    await t.tpArena('ArenaA', pa, 0); await t.tpArena('ArenaB', pb, 180); await sleep(6500); await t.uleczy('ArenaB');
    await daj('ArenaA', 'minecraft:splash_potion[minecraft:potion_contents={potion:"minecraft:harming"}]');
    const pc = await t.poz('ArenaB'); await a.lookAt(new Vec3(pc.x, pc.y + 0.5, pc.z), true);
    const h0 = await t.hp('ArenaB'); a.activateItem(); await sleep(1800); const h1 = await t.hp('ArenaB');
    console.log(opis, 'HP', h0, '->', h1); return h0 - h1;
  };
  const dz = await rzut('mikstura trybuny->ring', TRYBUNY, { ...W_RINGU, z: -16.5 });
  w.sprawdz('mikstura-zza-liny', dz === 0, 'mikstura krzywdy zza liny nie rani', dz);
  await sleep(1000);
  const dr = await rzut('mikstura ring->ring', { ...RING, z: -4.5 }, { ...RING, z: -2.5 });
  w.sprawdz('mikstura-ring', dr > 0, 'mikstura krzywdy w ringu rani (kontrola)', dr);
  for (const n of ['ArenaA', 'ArenaB']) { await rcon(`minecraft:item replace entity ${n} container.0 with minecraft:air`); await rcon(`minecraft:clear ${n} minecraft:dirt`); }
  await sleep(11000);
}, { limitMs: 200000 });
