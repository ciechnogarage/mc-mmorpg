'use strict';
// Diagnoza: kiedy BetonQuest (styl dialog) puszcza gracza po rozmowie zakończonej kwestią NPC bez odpowiedzi
// („Dziękuję jeszcze raz.”). Po tym co 2 s próba nowej rozmowy (rozmowaS, styl chest) przez 20 s.
// Przygotowanie RCON-em (tag nagrodzony) to tylko scena; klik w NPC jak gracz (INTERACT_AT).
const { scenariusz, rcon, sleep, Dialogi, ruchGracza, idz } = require('/bot/lib');
const P = { x: 4.5, y: -59, z: 0.5 };

scenariusz('akt1-bq-koniec', async ({ w, polacz }) => {
  const nick = 'BotBk' + (Date.now() % 100000);
  const bot = await polacz({ nick });
  const dlg = new Dialogi(bot);
  ruchGracza(bot);
  await sleep(2000);
  await rcon('mm m kill AKT1_NPC_PROSTY');
  await rcon(`mm mobs spawn AKT1_NPC_PROSTY 1 world,${P.x},${P.y},${P.z}`);
  await rcon(`bq tag ${nick} add akt1test>nagrodzony`);
  await sleep(1500);
  await idz(bot, P.x - 1.5, P.y, P.z, { r: 0.5 });
  const s = bot.nearestEntity(x => x.name === 'villager');
  if (s) { await bot.lookAt(s.position.offset(0, 1.6, 0)); bot.activateEntityAt(s, s.position.offset(0, 1, 0)); }
  const d = await dlg.czekajNa(/./, 6000);
  w.sprawdz('dialog_koncowy', !!d, d ? `"${dlg.tytul(d)}" przyciski ${JSON.stringify(dlg.przyciski(d).map(b => `${b.etykieta}:${b.typ}`))} | klucze ${Object.keys(d).join(',')}` : 'brak');
  const t0 = Date.now();
  let wolny = null;
  while (Date.now() - t0 < 20000) {
    await rcon(`bq action ${nick} akt1test>rozmowaS`);
    await sleep(1500);
    if (bot.currentWindow) { wolny = Math.round((Date.now() - t0) / 1000); bot.closeWindow(bot.currentWindow); break; }
    await sleep(500);
  }
  w.sprawdz('nowa_rozmowa_mozliwa', wolny !== null, wolny !== null ? `po ${wolny} s` : 'po 20 s dalej "in conversation"');
  await rcon('mm m kill AKT1_NPC_PROSTY');
}, { limitMs: 90000 });
