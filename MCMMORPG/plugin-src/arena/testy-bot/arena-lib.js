'use strict';
// Wspolne pomiary Areny: prawda z serwera (RCON data get), nie ze stanu bota.
const { sleep, czekaj } = require('/bot/lib');
const RING = { x: 0.5, y: 64, z: 0.5 };            // srodek ringu (podloga y=63)
const TRYBUNY = { x: 0.5, y: 64, z: -20.5 };       // poza ringiem (r=21), za lina przy bramie polnocnej
const W_RINGU = { x: 0.5, y: 64, z: -17.5 };       // w ringu przy krawedzi (r=18)
const TARAS = { x: 0.5, y: 68, z: -25.5 };          // taras widzow (y 67)

function narzedzia(rcon) {
  const t = {};
  t.hp = async n => { const m = (await rcon(`data get entity ${n} Health`)).match(/([\d.]+)f/); return m ? +m[1] : null; };
  t.wymiar = async n => (await rcon(`data get entity ${n} Dimension`)).replace(/.*data: /, '').replace(/"/g, '');
  t.poz = async n => { const m = (await rcon(`data get entity ${n} Pos`)).match(/\[([-\d.]+)d, ([-\d.]+)d, ([-\d.]+)d\]/); return m ? { x: +m[1], y: +m[2], z: +m[3] } : null; };
  t.exp = async n => (await rcon(`papi parse ${n} %mmocore_level%/%mmocore_experience%`)).trim();
  t.efekty = async n => (await rcon(`data get entity ${n} active_effects`));
  t.tpArena = async (n, p, yaw = 0) => rcon(`minecraft:execute in minecraft:arena run minecraft:teleport ${n} ${p.x} ${p.y} ${p.z} ${yaw} 0`);
  t.tpWorld = async (n, p, yaw = 0) => rcon(`minecraft:execute in minecraft:overworld run minecraft:teleport ${n} ${p.x} ${p.y} ${p.z} ${yaw} 0`);
  t.uleczy = async n => { await rcon(`minecraft:effect give ${n} minecraft:instant_health 1 5 true`); await sleep(400); };
  // swiat world: dzien, bez mobow i upadku; platforma pod punktem wyjscia
  t.przygotuj = async () => {
    for (const g of ['fall_damage false', 'spawn_monsters false', 'spawn_mobs false', 'advance_time false']) await rcon(`minecraft:gamerule ${g}`);
    await rcon('minecraft:time set 6000');
    for (const n of ['ArenaA', 'ArenaB']) { await rcon(`mmocore admin class ${n} HUMAN`); await rcon(`zaklecia przypisz ${n} nic lewa`); await rcon(`zaklecia przypisz ${n} nic prawa`); await rcon(`minecraft:item replace entity ${n} container.0 with minecraft:air`); }
    await rcon('minecraft:execute in minecraft:overworld run minecraft:kill @e[type=#minecraft:undead]');
    await rcon('minecraft:execute in minecraft:overworld run minecraft:kill @e[type=minecraft:creeper]');
    await rcon('minecraft:execute in minecraft:overworld run minecraft:kill @e[type=minecraft:spider]');
    await rcon('minecraft:execute in minecraft:overworld run minecraft:fill -2 -61 -2 12 -61 6 minecraft:stone');
  };
  t.wWorld = async (n, x = 1.5) => {        // gracz stoi na platformie w world (punkt powrotu)
    if ((await t.wymiar(n)).includes('arena')) { await rcon(`arena wyjdz ${n}`); await sleep(1500); }
    await t.tpWorld(n, { x, y: -60, z: 2.5 }); await sleep(1200);
  };
  t.wejdz = async n => { const r = await rcon(`arena wejdz ${n}`); await sleep(1500); return (await t.wymiar(n)).includes('arena'); };
  // A bije B (cel z serwera A); zwraca ubytek HP B; czeka po teleporcie na koniec nietykalnosci CMI
  t.cios = async (a, nickB, opis) => {
    const ps = await t.poz(nickB);
    const e = await czekaj(() => { const x = a.players[nickB]?.entity; return x && ps && Math.hypot(x.position.x - ps.x, x.position.z - ps.z) < 1.5 ? x : null; }, 6000, 200);
    if (!e) { console.log(opis, `${nickB} niewidoczny dla ${a.username}`); return null; }
    a.setQuickBarSlot(0); await a.lookAt(e.position.offset(0, 1.4, 0), true); await sleep(700);
    const h0 = await t.hp(nickB); a.attack(e); await sleep(900); const h1 = await t.hp(nickB);
    console.log(opis, `| HP ${nickB}`, h0, '->', h1, '|', JSON.stringify(await t.poz(a.username)), JSON.stringify(await t.poz(nickB)));
    return +(h0 - h1).toFixed(2);
  };
  return t;
}
module.exports = { narzedzia, RING, TRYBUNY, W_RINGU, TARAS, sleep, czekaj };
