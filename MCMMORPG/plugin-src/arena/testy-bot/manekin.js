'use strict';
// F: manekin treningowy - licznik obrazen, brak ruchu/smierci/nagrod, odrodzenie, sprzatanie liczb.
const { scenariusz } = require('/bot/lib');
const { narzedzia, sleep, czekaj } = require('./arena-lib');
scenariusz('arena-manekin', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const a = await polacz({ nick: 'ArenaA' });
  const pasek = []; a.on('actionBar', m => pasek.push(String(m?.toString?.() ?? m))); a.on('messagestr', m => pasek.push(m));
  await t.przygotuj();
  if (!(await t.wymiar('ArenaA')).includes('arena')) { await t.wWorld('ArenaA'); await t.wejdz('ArenaA'); }
  const M = 'minecraft:execute in minecraft:arena run minecraft:';
  const sel = '@e[type=minecraft:iron_golem,x=35.5,y=65,z=0.5,distance=..1.5,limit=1]';
  const hpM = async () => { const m = (await rcon(`${M}data get entity ${sel} Health`)).match(/([\d.]+)f/); return m ? +m[1] : null; };
  const pozM = async () => { const m = (await rcon(`${M}data get entity ${sel} Pos`)).match(/\[([-\d.]+)d, ([-\d.]+)d, ([-\d.]+)d\]/); return m ? [+m[1], +m[2], +m[3]] : null; };
  const liczby = async () => ((await rcon(`${M}execute if entity @e[type=minecraft:text_display,x=35.5,y=67,z=0.5,distance=..3]`)).match(/count: (\d+)/i) || [0, '0'])[1];
  console.log('manekiny:', await rcon(`${M}execute if entity @e[type=minecraft:iron_golem]`));
  await rcon('minecraft:item replace entity ArenaA container.0 with minecraft:air');
  await t.tpArena('ArenaA', { x: 33.5, y: 65, z: 0.5 }, -90); await sleep(6500);
  const h0 = await hpM(), p0 = await pozM(), e0 = await t.exp('ArenaA');
  console.log('manekin HP', h0, 'poz', JSON.stringify(p0));
  w.sprawdz('manekin-jest', h0 !== null, 'manekin stoi na stacji 2');
  pasek.length = 0;
  for (let i = 0; i < 6; i++) {
    const m = a.nearestEntity(e => e.name === 'iron_golem'); a.setQuickBarSlot(0); await a.lookAt(m.position.offset(0, 1.6, 0), true); a.attack(m); await sleep(i < 3 ? 650 : 120);
  }
  await sleep(300); const ile = await liczby();
  await sleep(1500);
  const wpisy = pasek.filter(m => /Suma/.test(m));
  console.log('pasek:', JSON.stringify(wpisy.slice(-2)), 'liczby nad manekinem w trakcie:', ile);
  w.sprawdz('licznik', wpisy.length > 0 && /Trafienia [1-9]/.test(wpisy.at(-1)), 'pasek pokazuje cios, sume, DPS i trafienia', wpisy.at(-1));
  w.sprawdz('liczby-znikaja', (await liczby()) === '0', 'liczby nad manekinem znikaja po ~1 s', await liczby());
  w.sprawdz('manekin-nie-traci-hp', (await hpM()) === h0, 'HP manekina bez zmian', `${h0} -> ${await hpM()}`);
  const p1 = await pozM();
  w.sprawdz('manekin-nie-przesuwa', p1 && Math.hypot(p1[0] - p0[0], p1[2] - p0[2]) < 0.05, 'manekin nie odpycha sie', JSON.stringify(p1));
  w.sprawdz('bez-exp', (await t.exp('ArenaA')) === e0, 'bicie manekina nie daje EXP', `${e0} -> ${await t.exp('ArenaA')}`);
  // zabity manekin wraca, bez lupu
  await rcon(`${M}kill ${sel}`); await sleep(500);
  const lup = await rcon(`${M}execute if entity @e[type=minecraft:item,x=35.5,y=65,z=0.5,distance=..4]`);
  w.sprawdz('bez-lupu', !/passed/i.test(lup), 'zabity manekin nic nie upuszcza', lup);
  const wrocil = await czekaj(async () => (await hpM()) !== null ? true : null, 9000, 500);
  w.sprawdz('odrodzenie', !!wrocil, 'manekin wraca na stacje w <= 9 s');
  const golemy = ((await rcon(`${M}execute if entity @e[type=minecraft:iron_golem]`)).match(/count: (\d+)/i) || [0, '0'])[1];
  w.sprawdz('bez-duplikatow', golemy === '3', 'dokladnie 3 manekiny', golemy);
  // manekin po za stacja (np. odepchniety) jest usuwany i stawiany na nowo
  await rcon(`${M}teleport ${sel} 38.5 65 0.5`); await sleep(6000);
  const p2 = await pozM();
  w.sprawdz('powrot-na-stacje', p2 && Math.abs(p2[0] - 35.5) < 0.2, 'przesuniety manekin wraca na stacje', JSON.stringify(p2));
}, { limitMs: 150000 });
