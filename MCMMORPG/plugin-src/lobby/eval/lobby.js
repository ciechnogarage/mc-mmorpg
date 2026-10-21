// Sonda lobby: bot wchodzi jak gracz (bez OP), sprawdza wyspe, menu, portal, pustke.
const mineflayer = require('mineflayer');
const { Rcon } = require('./rcon');
const HOST = process.env.MC_HOST || 'lobby-test';
const results = [];
const ok = (name, cond, info = '') => { results.push([cond ? 'PASS' : 'FAIL', name, info]); console.log((cond ? 'PASS ' : 'FAIL ') + name + (info ? '  — ' + info : '')); };
const sleep = ms => new Promise(r => setTimeout(r, ms));

(async () => {
  const bot = mineflayer.createBot({ host: HOST, port: 25565, username: 'LobbyQA', auth: 'offline', version: '1.21.11' });
  const payloads = [];
  bot._client.on('custom_payload', p => { payloads.push(p); });
  let bossbars = [];
  bot._client.on('boss_bar', p => bossbars.push(p));
  const chat = [];
  bot.on('messagestr', m => chat.push(m));
  await new Promise((res, rej) => { bot.once('spawn', () => { bot._client.write('custom_payload', { channel: 'minecraft:register', data: Buffer.from('bungeecord:main') }); res(); }); bot.once('kicked', r => rej(new Error('kick ' + JSON.stringify(r)))); bot.once('error', rej); });
  await sleep(2500);

  const pos = bot.entity.position;
  ok('spawn na placu', Math.abs(pos.x - 0.5) < 1 && Math.abs(pos.z - 6.5) < 1 && Math.abs(pos.y - 65) < 1.1, pos.toString());
  ok('tryb przygody', bot.game.gameMode === 'adventure', bot.game.gameMode);
  const below = bot.blockAt(pos.offset(0, -1, 0));
  ok('stoi na placu', below && below.name !== 'air', below && below.name);
  const compass = bot.inventory.slots[36 + 4];
  ok('kompas w slocie 5', compass && compass.name === 'compass', compass && compass.name);
  ok('wiadomosc powitalna', chat.some(m => m.includes('portal')), chat.slice(-3).join(' | '));

  const displays = Object.values(bot.entities).filter(e => e.name === 'text_display');
  ok('hologramy (>=4)', displays.length >= 4, 'text_display: ' + displays.length);
  const mobs = Object.values(bot.entities).filter(e => e.type === 'hostile' || e.type === 'mob' || e.type === 'animal');
  ok('zero mobow', mobs.length === 0, mobs.map(m => m.name).join(','));

  // teren: portal, fontanna, wodospad
  const portalBlock = bot.blockAt(bot.entity.position.clone().set(0, 67, -20));
  ok('tafla portalu glownego', portalBlock && portalBlock.name === 'nether_portal', portalBlock && portalBlock.name);
  const east = bot.blockAt(bot.entity.position.clone().set(20, 67, 0));
  ok('luk "Wkrotce" pusty', east && east.name === 'air', east && east.name);
  const fountain = bot.blockAt(bot.entity.position.clone().set(1, 64, 1));
  ok('fontanna z woda', fountain && fountain.name === 'water', fountain && fountain.name);

  // menu z kompasu
  await sleep(4500); // statusy: 3 pingi co 2 s
  bot.setQuickBarSlot(4);
  await sleep(200);
  const winP = new Promise(res => bot.once('windowOpen', res));
  bot.activateItem();
  const win = await Promise.race([winP, sleep(3000).then(() => null)]);
  ok('menu otwiera sie', !!win, win && JSON.stringify(win.title).slice(0, 80));
  if (win) {
    const it = win.slots[13];
    const name = it && (it.customName || it.displayName);
    const lore = it && it.customLore ? JSON.stringify(it.customLore) : '';
    ok('menu: swiat MMORPG w srodku', it && it.name === 'grass_block', (it && it.name) + ' ' + String(name).slice(0, 80));
    ok('menu: status w opisie', /Online|Offline|Uruchamia/.test(lore), lore.slice(0, 160));
    const w11 = win.slots[11];
    ok('menu: zamkniete sloty szare', w11 && w11.name === 'gray_dye', w11 && w11.name);
    bot.closeWindow(win);
  }

  // wejscie w portal na piechote (przez wyrzutnie)
  const before = payloads.length;
  { const r0 = await Rcon.connect(HOST, 25575, 'test'); await r0.send('minecraft:tp LobbyQA 0.5 65 -6.5 180 0'); r0.end(); await sleep(1500); }
  console.log('start', bot.entity.position.toString());
  await bot.look(0, 0, true); // yaw PI = polnoc w mineflayer
  bot.setControlState('forward', true);
  let minZ = 99;
  for (let i = 0; i < 80 && payloads.length === before; i++) {
    await sleep(100);
    minZ = Math.min(minZ, bot.entity.position.z);
  }
  bot.setControlState('forward', false);
  const pl = payloads.slice(before).map(p => p.channel + ':' + Buffer.from(p.data || []).toString('latin1').replace(/[^\x20-\x7e]/g, '.'));
  const connect = pl.find(s => s.includes('Connect') && s.includes('world1'));
  const queued = bossbars.length > 0;
  ok('portal: Connect world1 albo kolejka', !!connect || queued, (connect || ('bossbar=' + bossbars.length)) + ' minZ=' + minZ.toFixed(1));
  ok('wyrzutnia poniosla bota', minZ < -12, 'minZ=' + minZ.toFixed(1));

  // pustka: konsola testowego serwera zrzuca bota pod wyspe (obserwacja powrotu)
  const rcon = await Rcon.connect(HOST, 25575, 'test');
  await rcon.send('minecraft:tp LobbyQA 60 40 60');
  await sleep(3500);
  const p2 = bot.entity.position;
  ok('pustka: powrot na spawn', Math.abs(p2.x - 0.5) < 2 && Math.abs(p2.z - 6.5) < 2 && p2.y > 60, p2.toString());
  ok('zdrowie pelne', bot.health >= 20, 'hp=' + bot.health);
  rcon.end();

  const fails = results.filter(r => r[0] === 'FAIL').length;
  console.log(`\n${results.length - fails}/${results.length} PASS`);
  bot.quit();
  process.exit(fails ? 1 : 0);
})().catch(e => { console.error('BLAD', e); process.exit(2); });
