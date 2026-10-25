'use strict';
// Diagnoza FAIL dialog_otwarty (zadanie.js): dlaczego PPM w NPC bez modelu nie startuje rozmowy BetonQuest.
// Dla NPC VILLAGER i ZOMBIE (oba NoAI, skill `~onInteract` mówi MM_KLIK_*): A = sam INTERACT, B = INTERACT_AT + INTERACT,
// z 1,5 bloku. Mierzy: czy MythicMobs zobaczył klik (czat MM_KLIK_*), czy przyszedł dialog BQ (show_dialog).
// C: start rozmowy akcją z konsoli (`bq action <nick> akt1test>rozmowa`) = kontrola stylu dialog.
const { scenariusz, rcon, sleep, Dialogi, obserwuj, ruchGracza, idz } = require('/bot/lib');

const NPC = [
  { mob: 'AKT1_NPC_PROSTY', typ: 'villager', p: { x: 4.5, y: -59, z: 0.5 } },
  { mob: 'AKT1_NPC_ZOMBIE', typ: 'zombie', p: { x: 4.5, y: -59, z: 6.5 } },
];

scenariusz('akt1-bq-dialog', async ({ w, polacz }) => {
  const nick = 'BotBd' + (Date.now() % 100000);
  await rcon('minecraft:kill @e[type=minecraft:villager]');
  await rcon('minecraft:kill @e[type=minecraft:zombie]');
  await sleep(500);
  const bot = await polacz({ nick });
  const e = obserwuj(bot), dlg = new Dialogi(bot);
  const czat = [];
  bot.on('messagestr', m => czat.push(m));
  ruchGracza(bot);
  await sleep(3000);
  // NPC przyzwane, gdy gracz już jest w pobliżu (mc-mythicmobs: spawn z konsoli na starcie bywa „martwy” na klik)
  for (const n of NPC) await rcon(`mm mobs spawn ${n.mob} 1 world,${n.p.x},${n.p.y},${n.p.z}`);
  await sleep(1500);

  for (const n of NPC) {
    await idz(bot, n.p.x - 1.5, n.p.y, n.p.z, { r: 0.5 });
    const ent = bot.nearestEntity(x => x.name === n.typ);
    w.sprawdz(`${n.typ}_widoczny`, !!ent, ent ? `odl ${bot.entity.position.distanceTo(ent.position).toFixed(1)}` : 'brak');
    if (!ent) continue;
    for (const [nazwa, fn] of [
      ['A_interact', () => bot.activateEntity(ent)],
      ['B_interact_at', () => { bot.activateEntityAt(ent, ent.position.offset(0, 1, 0)); bot.activateEntity(ent); }],
    ]) {
      czat.length = 0;
      await bot.lookAt(ent.position.offset(0, 1.6, 0), true);
      fn();
      const d = await dlg.czekajNa(/./, 4000);
      const mm = czat.filter(m => /MM_KLIK/.test(m));
      w.sprawdz(`${n.typ}_${nazwa}`, !!d, `MM widzi klik: ${mm.length ? 'TAK' : 'NIE'} | dialog: ${d ? `"${dlg.tytul(d)}" ${JSON.stringify(dlg.przyciski(d).map(b => b.etykieta))}` : 'brak'}`);
      await rcon(`bq action ${nick} akt1test>anuluj`);
      await sleep(1500);
    }
  }
  const r = await rcon(`bq action ${nick} akt1test>rozmowa`);
  const d = await dlg.czekajNa(/./, 4000);
  w.sprawdz('C_akcja_konsoli', !!d, `${r} | ${d ? dlg.tytul(d) : 'brak dialogu'}`);
  await rcon(`bq action ${nick} akt1test>anuluj`);
  w.sprawdz('ekran', true, e.opis());
}, { limitMs: 120000 });
