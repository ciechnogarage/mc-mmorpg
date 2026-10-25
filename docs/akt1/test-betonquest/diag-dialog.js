'use strict';
// Diagnoza FAIL dialog_otwarty (zadanie.js): czy NPC bez modelu reaguje na PPM i czy styl `dialog` BQ dochodzi do klienta.
// A: PPM samym INTERACT; B: INTERACT_AT + INTERACT (jak NPC z modelem); C: `bq action <nick> akt1test>rozmowa` z konsoli.
const { scenariusz, rcon, sleep, Dialogi, obserwuj, ruchGracza, idz } = require('/bot/lib');

const SOLTYS = { x: 4.5, y: -59, z: 0.5 };

scenariusz('akt1-bq-dialog', async ({ w, polacz }) => {
  const nick = 'BotBd' + (Date.now() % 100000);
  await rcon('minecraft:kill @e[type=minecraft:villager]');
  await sleep(500);
  await rcon(`mm mobs spawn AKT1_NPC_PROSTY 1 world,${SOLTYS.x},${SOLTYS.y},${SOLTYS.z}`);
  const bot = await polacz({ nick });
  const e = obserwuj(bot), dlg = new Dialogi(bot);
  const pakiety = [];
  bot._client.on('packet', (d, m) => { if (/dialog|open_window|chat|system/.test(m.name)) pakiety.push(m.name); });
  ruchGracza(bot);
  await idz(bot, SOLTYS.x - 2, SOLTYS.y, SOLTYS.z, { r: 1 });
  const npc = bot.nearestEntity(x => x.name === 'villager');
  w.sprawdz('npc', !!npc, npc ? `odl ${bot.entity.position.distanceTo(npc.position).toFixed(1)}` : 'brak');
  const proba = async (nazwa, fn) => {
    pakiety.length = 0;
    await fn(); const d = await dlg.czekajNa(/./, 5000);
    w.sprawdz(nazwa, !!d, `dialog "${d ? dlg.tytul(d) : ''}" ${d ? JSON.stringify(dlg.przyciski(d).map(b => b.etykieta)) : ''} | pakiety: ${[...new Set(pakiety)].join(',')} | aktywne cele: ${(await rcon(`bq objective ${nick} list`)).slice(0, 120)}`);
    await sleep(1500);
  };
  await proba('A_ppm_interact', async () => { await bot.lookAt(npc.position.offset(0, 1.6, 0)); bot.activateEntity(npc); });
  await proba('B_ppm_interact_at', async () => { await bot.lookAt(npc.position.offset(0, 1.6, 0)); bot.activateEntityAt(npc, npc.position.offset(0, 1, 0)); bot.activateEntity(npc); });
  await proba('C_akcja_konsoli', async () => { w.sprawdz('C_rcon', true, await rcon(`bq action ${nick} akt1test>rozmowa`)); });
  w.sprawdz('ekran', true, e.opis());
}, { limitMs: 120000 });
