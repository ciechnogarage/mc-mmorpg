// Sonda Lochu 002 (klon mc-l002): bot bez OP + RCON do USTAWIANIA pozycji i OBSERWACJI stanu instancji.
// To test funkcjonalny (czy mechaniki dzialaja), nie przejscie "jak gracz" - to robi potem harness gracza.
// Wynik: linie "PASS/FAIL <nazwa> <szczegoly>" + "WYNIK x/y".
// Uruchom (klon z wgranym lochem): /opt/docker/mc/narzedzia/bot/run.sh <klon> <repo>/kuznia/tools/sonda_l002/sonda.js
const { scenariusz } = require('/bot/lib');
const NICK = process.env.NICK || 'Sonda002';
const sleep = ms => new Promise(r => setTimeout(r, ms));

scenariusz('sonda-l002', async ({ w, polacz, rcon }) => {
  const ok = (n, pass, info = '') => w.sprawdz(n, pass, info);
  const R = { cmd: rcon };
  const bot = await polacz({ nick: NICK });
  const tytuly = [], czat = []; let kick = null, swiat = null;
  const txt = v => JSON.stringify(v).replace(/"(text|color|bold|italic|type|extra)":/g, '').replace(/[{}\[\]"]/g, ' ');
  bot._client.on('set_title_text', d => tytuly.push(txt(d)));
  bot._client.on('set_title_subtitle', d => tytuly.push(txt(d)));
  bot.on('message', m => czat.push(m.toString()));
  bot.on('kicked', r => { kick = String(r); console.log('KICK', kick); });
  bot.on('respawn', () => { swiat = bot.game.dimension; });
  await sleep(2000);
  const tp = async (x, y, z) => { await R.cmd(`execute in minecraft:${inst} run minecraft:tp ${NICK} ${x} ${y} ${z}`); await sleep(600);
    bot.setControlState('jump', true); await sleep(300); bot.setControlState('jump', false); await sleep(900); };   // TriggerDistance reaguje na RUCH
  const cnt = async (sel) => { const o = await R.cmd(`execute in minecraft:${inst} positioned 0 100 0 if entity ${sel}`); const m = o.match(/count: (\d+)/i); return m ? +m[1] : 0; };
  const blok = async (x, y, z, b) => (await R.cmd(`execute in minecraft:${inst} if block ${x} ${y} ${z} ${b}`)).includes('passed');
  const czeka = async (f, ms = 8000) => { const t0 = Date.now(); while (Date.now() - t0 < ms) { if (await f()) return true; await sleep(400); } return false; };
  const tyt = (re) => tytuly.some(t => re.test(t)) || czat.some(t => re.test(t));
  const blisko = (nazwaRe, r = 5) => Object.values(bot.entities).filter(e => e !== bot.entity && e.position && e.position.distanceTo(bot.entity.position) < r)
      .find(e => { try { const c = e.getCustomName && e.getCustomName(); return c && nazwaRe.test(c.toString()); } catch { return false; } });
  const klik = async (e) => { await bot.lookAt(e.position.offset(0, 1.5, 0)); try { bot.activateEntityAt(e, e.position.offset(0, 1, 0)); } catch {} await sleep(200); try { bot.activateEntity(e); } catch {} };
  const bij = async (e, n) => { for (let i = 0; i < n && e.isValid !== false && bot.entities[e.id]; i++) { await bot.lookAt(e.position.offset(0, 1.2, 0)); bot.attack(e); await sleep(650); } };

  // ---- 1. wejscie
  await R.cmd(`md play loch_002 ${NICK}`);
  // bot.game.dimension to TYP wymiaru ("overworld"), nie nazwa swiata - instancje szukamy RCON-em
  var inst = null;
  const weszl = await czeka(async () => { for (let n = 0; n < 8; n++) {
      const o = await R.cmd(`execute in minecraft:loch_002_${n} positioned 0 100 0 if entity @a[name=${NICK},distance=..2000]`);
      if (o.includes('passed')) { inst = `loch_002_${n}`; return true; } } return false; }, 20000);
  ok('wejscie', weszl, inst);
  await sleep(1500); bot.setControlState('forward', true); await sleep(600); bot.setControlState('forward', false);
  await sleep(8000);
  ok('bez_kicka_po_wejsciu', !kick, kick || '');
  ok('tytul_loch002', await czeka(async () => tyt(/Loch 002/), 6000));
  await R.cmd(`effect give ${NICK} minecraft:resistance infinite 4 true`);
  await R.cmd(`effect give ${NICK} minecraft:regeneration infinite 3 true`);
  // ---- 2. populacja
  await sleep(2000);
  const pop = { endermite: await cnt('@e[type=minecraft:endermite,distance=..400]'), drowned: await cnt('@e[type=minecraft:drowned,distance=..400]'),
    bogged: await cnt('@e[type=minecraft:bogged,distance=..400]'), witch: await cnt('@e[type=minecraft:witch,distance=..400]'),
    spider: await cnt('@e[type=minecraft:spider,distance=..400]'), villager: await cnt('@e[type=minecraft:villager,distance=..400]'),
    frog: await cnt('@e[type=minecraft:frog,distance=..400]'), stand: await cnt('@e[type=minecraft:armor_stand,distance=..400]') };
  ok('populacja', pop.endermite >= 8 && pop.drowned >= 5 && pop.bogged >= 4 && pop.witch >= 2 && pop.spider >= 1 && pop.villager >= 1, JSON.stringify(pop));
  // ---- 3a. na ladzie (koniec pomostu, z dala od mobow) nie truje
  await tp(-55.5, 102, 102.5); await sleep(3000);
  const trujeLad = !!Object.values(bot.entity.effects || {}).find(e => bot.registry.effectsByName.poison && e.id === bot.registry.effectsByName.poison.id);
  ok('na_ladzie_nie_truje', !trujeLad);
  // ---- 3. zgnila woda truje
  await tp(-51.5, 100, 64.5);
  const wodaPod = await blok(-52, 100, 64, 'minecraft:water');
  await sleep(2500);
  const truje = !!Object.values(bot.entity.effects || {}).find(e => e.id === 18 || e.id === 19 || (bot.registry.effectsByName.poison && e.id === bot.registry.effectsByName.poison.id));
  ok('zgnila_woda_truje', truje, `woda=${wodaPod} efekty=${JSON.stringify(Object.keys(bot.entity.effects || {}))}`);
  // ---- 4. zasadzka na kladkach
  const e0 = await cnt('@e[type=minecraft:endermite,distance=..400]');
  await tp(-37.5, 102, 67.5);
  ok('zasadzka_kladki', await czeka(async () => (await cnt('@e[type=minecraft:endermite,distance=..400]')) >= e0 + 3, 5000), `przed=${e0}`);
  // ---- 5. sluza 1 (klik w korbe)
  ok('krata_zamknieta', await blok(0, 103, 30, 'minecraft:iron_bars'));
  await tp(2.5, 102, 37.5);
  await sleep(1500);
  await tp(4.5, 102, 36.5);
  let k1 = await czeka(async () => !!blisko(/Kołowrót/, 5), 5000);
  ok('korba1_pojawia_sie', k1);
  if (k1) await klik(blisko(/Kołowrót/, 5));
  ok('sluza1_krata_otwarta', await czeka(() => blok(0, 103, 30, 'minecraft:air'), 6000));
  ok('sluza1_basen_osuszony', await czeka(() => blok(-10, 97, 15, 'minecraft:air'), 12000));
  // ---- 6. wrak na dnie
  await tp(-7.5, 97, 18.5);
  ok('sekret_wrak', await czeka(async () => tyt(/Wrak Rybak/), 5000));
  // ---- 7. sluza 2 + skrzynia
  await tp(-29.5, 102, -5.5); await sleep(1200); await tp(-35.5, 102, -4.5);
  let k2 = await czeka(async () => !!blisko(/Kołowrót/, 5), 5000);
  if (k2) await klik(blisko(/Kołowrót/, 5));
  ok('sluza2_basen_osuszony', await czeka(() => blok(-52, 97, -10, 'minecraft:air'), 14000), `korba=${k2}`);
  await tp(-50.5, 97, -5.5);
  ok('sekret_skrzynia', await czeka(async () => tyt(/Skrzynia Topielca/), 5000));
  // ---- 8. szeptucha z chaty + wywar
  await R.cmd(`execute in minecraft:${inst} positioned 38 102 -4 run minecraft:kill @e[type=minecraft:witch,distance=..4,limit=1]`);
  await sleep(1000);
  await tp(37.5, 102, -5.5);
  ok('sekret_wywar', await czeka(async () => tyt(/Wywar Szeptuchy/), 5000));
  // ---- 9. sluza 3 bez korby = odmowa
  await tp(63.5, 102, -7.5); await sleep(1200); await tp(70.5, 102, -5.5);
  let k3 = await czeka(async () => !!blisko(/Kołowrót/, 5), 5000);
  if (k3) await klik(blisko(/Kołowrót/, 5));
  ok('sluza3_bez_korby_odmowa', await czeka(async () => tyt(/zardzewia/), 4000) && await blok(85, 99, -10, 'minecraft:water'));
  // ---- 10. kokoniarnia: kokon na graczu + kokon Borka
  await tp(48.5, 102, -38.5);
  const kokonBorka = await czeka(async () => (await R.cmd(`execute in minecraft:${inst} positioned 61.5 102 -50.5 if entity @e[type=minecraft:zombie,distance=..2.5]`)).includes('passed'), 6000);
  ok('kokon_borka_pojawia_sie', kokonBorka);
  const t0 = Date.now(); let kokonNaGraczu = false;
  while (Date.now() - t0 < 20000) { if (blisko(/^Kokon.*bij!/, 3) || tyt(/Kokon oplata/)) { kokonNaGraczu = true; break; } await sleep(500); }
  ok('kokoniara_rzuca_kokon', kokonNaGraczu);
  if (kokonNaGraczu) { const kk = blisko(/^Kokon/, 3); if (kk) await bij(kk, 6); }
  // kokon Borka stoi w (61.5, y, -50.5) - szukamy zombie przy tym miejscu (nazwa wieloczesciowa nie zawsze czytelna)
  await tp(62.5, 102, -49.5);
  const kb2 = Object.values(bot.entities).find(e => e.name === 'zombie' && Math.hypot(e.position.x - 61.5, e.position.z + 50.5) < 1.5);
  if (kb2) await bij(kb2, 25);
  ok('borek_uwolniony', await czeka(async () => tyt(/Rybak Borek/), 6000));
  // ---- 11. sluza 3 z korba + dzwon + odpornosc
  await tp(70.5, 102, -5.5); await sleep(800);
  k3 = blisko(/Kołowrót/, 5); if (k3) await klik(k3);
  ok('sluza3_z_korba', await czeka(() => blok(85, 99, -10, 'minecraft:air'), 14000));
  await tp(83.5, 97, -5.5);
  ok('sekret_dzwon', await czeka(async () => tyt(/Dzwon Topielc/), 5000));
  await tp(-51.5, 100, 64.5); await sleep(3000);
  const truje2 = Object.keys(bot.entity.effects || {}).length && !!Object.values(bot.entity.effects).find(e => bot.registry.effectsByName.poison && e.id === bot.registry.effectsByName.poison.id);
  ok('po_dzwonie_woda_nie_truje', !truje2);
  // ---- 12. boss: studnie wg sluz
  await tp(22.5, 102, -63.5); await sleep(1000);
  await tp(10.5, 102, -74.5);
  ok('boss_intro', await czeka(async () => tyt(/Topielica/), 6000));
  await sleep(1500);
  const studnie = await cnt('@e[tag=l002_studnia,distance=..400]');
  ok('studnie_tylko_D', studnie === 1, `aktywnych=${studnie}`);
  ok('studnie_ABC_zasypane', await blok(-10, 98, -82, 'minecraft:mud') && await blok(-10, 98, -102, 'minecraft:mud') && await blok(10, 98, -102, 'minecraft:mud') && await blok(10, 98, -82, 'minecraft:water'));
  ok('boss_jest', (await cnt('@e[tag=l002_boss,distance=..400]')) === 1);
  // walka: bot stoi przy bossie, bossa zbijamy konsola do 55% (Skrzek) - obserwujemy Zanurzenie
  await tp(4.5, 102, -86.5);
  let nurt = false; const t1 = Date.now();
  while (Date.now() - t1 < 25000) {
    const o = await R.cmd(`execute in minecraft:${inst} positioned 0 100 -90 run data get entity @e[tag=l002_boss,distance=..60,limit=1] Pos`);
    const m = o.match(/\[([-\d.]+)d, ([-\d.]+)d, ([-\d.]+)d\]/);
    if (m && Math.hypot(+m[1] - 10.5, +m[3] + 81.5) < 2.5) { nurt = true; break; }
    const b = Object.values(bot.entities).find(e => e.name === 'drowned' && e.position.distanceTo(bot.entity.position) < 6); if (b) { await bot.lookAt(b.position.offset(0, 2, 0)); bot.attack(b); }
    await sleep(500);
  }
  ok('boss_zanurzenie_w_studni_D', nurt);
  await R.cmd(`execute in minecraft:${inst} positioned 0 100 -90 run damage @e[tag=l002_boss,distance=..60,limit=1] 95 minecraft:magic`);
  ok('boss_skrzek_faza2', await czeka(async () => (await cnt('@e[type=minecraft:zombie,distance=..400,name=!x]')) >= 1 && tyt(/skrzek/), 8000), `zombie=${await cnt('@e[type=minecraft:zombie,distance=..400]')}`);
  // ---- 13. koniec
  await R.cmd(`execute in minecraft:${inst} positioned 0 100 -90 run minecraft:kill @e[tag=l002_boss,distance=..60,limit=1]`);
  ok('final_jezioro_opada', await czeka(() => blok(0, 95, -120, 'minecraft:air'), 20000));
  ok('final_nagroda', await czeka(async () => tyt(/Łup z Bagien/), 20000), czat.filter(c => /Sekrety|sekrety/.test(c)).slice(-1)[0] || '');
  ok('wyjscie_po_finale', await czeka(async () => !(await R.cmd(`execute in minecraft:${inst} positioned 0 100 0 if entity @a[name=${NICK},distance=..2000]`)).includes('passed'), 30000));
  console.log('TYTULY', JSON.stringify(tytuly.slice(-30)));
  console.log('CZAT', JSON.stringify(czat.slice(-40)));
}, { limitMs: 420000 });
