'use strict';
// Test BetonQuest 3.2.0 (Akt 1, etap 1) na klonie: gracz BEZ OP przechodzi zadanie testowe pakietu akt1test.
// Scena (NPC, miecz) przygotowana RCON-em PRZED wejściem; sama ścieżka bez tp/give.
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh <klon> <ten plik>
const { scenariusz, rcon, czekaj, sleep, Dialogi, obserwuj, ruchGracza, idz, przedmioty, tytulOkna, sloty } = require('/bot/lib');

const SOLTYS = { x: 4.5, y: -59, z: 0.5 }, STRAZ = { x: -3.5, y: -59, z: 0.5 };
const tagi = async n => (await rcon(`bq tag ${n} list`)).toLowerCase();

const sprzataj = async () => {
  for (const m of ['AKT1_NPC_PROSTY', 'AKT1_NPC_MODEL', 'AKT1_NPC_ZOMBIE', 'AKT1_CEL']) await rcon(`mm m kill ${m}`);
  await rcon('minecraft:kill @e[type=minecraft:zombie]');
  await sleep(500);
  await rcon('minecraft:kill @e[type=minecraft:item]');
};

scenariusz('akt1-betonquest', async ({ w, polacz }) => {
  const nick = 'BotBq' + (Date.now() % 100000);
  const bot = await polacz({ nick });
  // scena sprzątana dopiero z botem w świecie: od 1.21.9 bez gracza chunki sceny nie są wczytane i kill nic nie trafia
  // (wcześniej NPC i cele zbierały się z przebiegu na przebieg). NPC są Invincible -> mm m kill po typie (mobs-options.md:86).
  await sleep(2000);
  await sprzataj();
  // NPC przyzwane, gdy gracz już jest blisko (mc-mythicmobs: spawn z konsoli zanim ktoś jest w pobliżu bywa „martwy” na klik)
  await rcon(`mm mobs spawn AKT1_NPC_PROSTY 1 world,${SOLTYS.x},${SOLTYS.y},${SOLTYS.z}`);
  await rcon(`mm mobs spawn AKT1_NPC_MODEL 1 world,${STRAZ.x},${STRAZ.y},${STRAZ.z}`);
  const e = obserwuj(bot), dlg = new Dialogi(bot);
  await rcon(`minecraft:give ${nick} minecraft:iron_sword`);
  await sleep(1500);
  ruchGracza(bot);
  const npcPrzy = p => Object.values(bot.entities).filter(x => x !== bot.entity && x.position.distanceTo(new (require('vec3'))(p.x, p.y, p.z)) < 1.5);

  // 1. rozmowa z NPC bez modelu (styl dialog) -> przyjęcie zadania
  await idz(bot, SOLTYS.x - 1.5, SOLTYS.y, SOLTYS.z, { r: 0.5 });
  const s = npcPrzy(SOLTYS);
  w.sprawdz('soltys_widoczny', s.length > 0, `encje przy sołtysie: ${s.map(x => x.name).join(',')}`);
  if (s[0]) { await bot.lookAt(s[0].position.offset(0, 1.6, 0)); bot.activateEntityAt(s[0], s[0].position.offset(0, 1, 0)); bot.activateEntity(s[0]); }
  const d1 = await dlg.czekajNa(/./, 8000);
  w.sprawdz('dialog_otwarty', !!d1, `tytuł "${dlg.tytul(d1)}", przyciski ${JSON.stringify(dlg.przyciski(d1).map(b => b.etykieta))}`);
  if (d1) { dlg.kliknij(/Pomog/); await sleep(1500); }
  if (dlg.biezacy) { w.sprawdz('dialog_dalej', true, `po kliknięciu: "${dlg.tytul()}" ${JSON.stringify(dlg.przyciski().map(b => b.etykieta))}`); try { dlg.kliknij(0); } catch (_) {} }
  await sleep(1000);
  w.sprawdz('zadanie_wziete', (await tagi(nick)).includes('wziete'), await tagi(nick));
  w.sprawdz('pasek_akcji', /Zadanie/.test(JSON.stringify(e.historia)), 'action bar po przyjęciu');

  // 2. zabij 3 cele (miecz w ręce, ciosy LPM)
  const miecz = bot.inventory.items().find(i => /sword/.test(i.name));
  if (miecz) await bot.equip(miecz, 'hand').catch(() => {});
  // tylko zombie przy punkcie przyzwania celów (mspawnmob 0;-59;10), żeby bot nie gonił obcych mobów świata klona
  const CELE = new (require('vec3'))(0, -59, 10);
  const celWZasiegu = x => x.name === 'zombie' && x.position.distanceTo(CELE) < 20;
  await sleep(1000);
  const zWidziane = Object.values(bot.entities).filter(x => x.name === 'zombie').map(x => `(${x.position.floored()}) d=${x.position.distanceTo(CELE).toFixed(0)}`);
  w.sprawdz('cele_widoczne', zWidziane.length >= 3, `zombie widziane: ${zWidziane.join(' ') || 'brak'} | AKT1_CEL przy punkcie: ${await rcon('execute positioned 0 -59 10 if entity @e[type=minecraft:zombie,distance=..20]')}`);
  const t0 = Date.now();
  while (Date.now() - t0 < 90000 && !(await tagi(nick)).includes('zabite')) {
    const z = bot.nearestEntity(celWZasiegu);
    if (!z) { await sleep(500); continue; }
    if (bot.entity.position.distanceTo(z.position) > 2.5) { await idz(bot, z.position.x, z.position.y, z.position.z, { r: 1.8, ms: 8000 }); continue; }
    await bot.lookAt(z.position.offset(0, 1.2, 0)); bot.attack(z); await sleep(650);
  }
  w.sprawdz('cel_zabij', (await tagi(nick)).includes('zabite'), `${Math.round((Date.now() - t0) / 1000)} s | cel BQ: ${(await rcon(`bq objective ${nick} list`)).replace(/\s+/g, ' ').slice(0, 160)} | HP ${bot.health}`);
  await sleep(1000);
  w.sprawdz('dropy_po_walce', true, `na ziemi: ${await rcon('execute if entity @e[type=minecraft:item]')} | ścięgien u gracza: ${await przedmioty.ile(nick, 'PROF_SINEW')}`);

  // 3. zbierz 2 ścięgna z ziemi
  const t1 = Date.now();
  while (Date.now() - t1 < 40000 && !(await tagi(nick)).includes('zebrane')) {
    const it = bot.nearestEntity(x => x.name === 'item');
    if (!it) { await sleep(500); continue; }
    await idz(bot, it.position.x, it.position.y, it.position.z, { r: 0.3, ms: 8000 }); await sleep(400);
  }
  const zost = Object.values(bot.entities).filter(x => x.name === 'item').map(x => `(${x.position.x.toFixed(1)},${x.position.y.toFixed(1)},${x.position.z.toFixed(1)})`);
  w.sprawdz('cel_zbierz', (await tagi(nick)).includes('zebrane'), `leżą: ${zost.join(' ') || '-'} bot ${bot.entity.position.floored()} | ścięgien u gracza: ${await przedmioty.ile(nick, 'PROF_SINEW')}`);

  // 4. dojdź do znacznika
  const r = await idz(bot, 0.5, -59, -14.5, { r: 1, ms: 30000 }); await sleep(1500);
  w.sprawdz('cel_dojdz', (await tagi(nick)).includes('doszedl'), r.opis);

  // 5. powrót do sołtysa -> nagroda
  const sc0 = await przedmioty.ile(nick, 'PROF_SINEW');
  const lvl0 = await rcon(`mmocore admin exp check ${nick} main`).catch(() => '');
  await idz(bot, SOLTYS.x - 1.5, SOLTYS.y, SOLTYS.z, { r: 0.5 });
  const s2 = npcPrzy(SOLTYS)[0];
  if (s2) { await bot.lookAt(s2.position.offset(0, 1.6, 0)); bot.activateEntityAt(s2, s2.position.offset(0, 1, 0)); bot.activateEntity(s2); }
  const d2 = await dlg.czekajNa(/./, 8000);
  w.sprawdz('dialog_nagroda', !!d2, `tytuł "${dlg.tytul(d2)}", ${JSON.stringify(dlg.przyciski(d2).map(b => b.etykieta))}`);
  if (d2) { try { dlg.kliknij(0); } catch (_) {} }
  await sleep(1500);
  const tg = await tagi(nick);
  w.sprawdz('nagrodzony', tg.includes('nagrodzony'), tg);
  const sc1 = await przedmioty.ile(nick, 'PROF_SINEW');
  w.sprawdz('sciegna_zabrane', sc0 - sc1 === 2, `przed ${sc0}, po ${sc1} (take sciegno:2)`);
  w.sprawdz('exp_info', true, `przed: ${lvl0} / po: ${await rcon(`mmocore admin exp check ${nick} main`).catch(() => '')}`);
  w.sprawdz('dziennik', true, await rcon(`bq journal ${nick} list`));

  // 6. NPC z modelem ModelEngine (styl chest): PPM tak jak gracz. Pauza: BQ trzyma gracza w rozmowie jeszcze ~2,5-6 s po
  // ostatniej kwestii NPC (diag-koniec.js; zgodne z conversation.interceptor.delay: 50 w config.yml BQ).
  await sleep(5000);
  await idz(bot, STRAZ.x + 2, STRAZ.y, STRAZ.z, { r: 1 });
  const st = npcPrzy(STRAZ);
  w.sprawdz('straz_encje', true, `encje widziane przy straży: ${st.map(x => `${x.name}#${x.id}`).join(',') || 'brak'}`);
  for (const x of st) { await bot.lookAt(x.position.offset(0, 1.2, 0)); bot.activateEntityAt(x, x.position.offset(0, 1, 0)); bot.activateEntity(x); await sleep(800); }
  const okno = await czekaj(() => bot.currentWindow, 5000);
  w.sprawdz('straz_rozmowa', (await tagi(nick)).includes('rozmowastraz'), `okno: ${okno ? tytulOkna(okno) + ' ' + JSON.stringify(sloty(okno).map(x => x.nazwa)) : 'brak'}`);
  if (okno) bot.closeWindow(okno);
  w.sprawdz('ekran', true, e.opis());
  await sprzataj();
}, { limitMs: 280000 });
