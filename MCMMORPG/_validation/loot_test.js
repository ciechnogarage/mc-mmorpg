// Serwerowy proof dropow dla level_1. Nie polega na encjach klienta.
// Uzycie: node loot_test.js HARD
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');

const HOST = '127.0.0.1';
const PORT = 25565;
const RPORT = 25575;
const RPASS = process.env.RCON_PASS;
const USER = 'Dungeoneer';
const DIFF = (process.argv[2] || 'HARD').toUpperCase();

const sleep = ms => new Promise(r => setTimeout(r, ms));
const waitFor = async (fn, to = 25000, st = 400) => {
  const t = Date.now();
  while (Date.now() - t < to) {
    const v = await fn();
    if (v) return v;
    await sleep(st);
  }
  return false;
};

let rcon, bot;

const send = cmd => rcon.send(cmd).catch(e => `ERR ${e.message}`);
const dimOf = async () => String(await send(`data get entity ${USER} Dimension`)).match(/"([^"]+)"/)?.[1];
const inInst = d => d && d.includes('level_1_');
const worldOf = d => String(d || '').replace(/^minecraft:/, '');
const killCountOf = out => Number(String(out || '').match(/Killed (\d+)/i)?.[1] || 0);
const hasItemData = out => /Item:/i.test(String(out || '')) && !/No entity was found/i.test(String(out || ''));

(async () => {
  rcon = await Rcon.connect({ host: HOST, port: RPORT, password: RPASS });
  bot = mineflayer.createBot({ host: HOST, port: PORT, username: USER, auth: 'offline', version: '1.21.11' });
  bot.on('resourcePack', () => bot.acceptResourcePack());

  if (!await waitFor(() => bot.entity, 60000, 500)) {
    console.log('NO-SPAWN');
    process.exit(7);
  }

  await send(`op ${USER}`);
  await sleep(600);

  const currentDim = await dimOf();
  if (inInst(currentDim)) {
    bot.chat('/md leave');
    await waitFor(async () => !inInst(await dimOf()), 15000, 500);
  }

  bot.chat(`/md play level_1:${DIFF}`);
  const inst = await waitFor(async () => {
    const d = await dimOf();
    return inInst(d) ? d : false;
  }, 20000, 500);
  if (!inst) {
    console.log('ENTER-FAIL');
    process.exit(3);
  }

  const world = worldOf(inst);
  await send(`gamemode creative ${USER}`);
  await send(`effect give ${USER} minecraft:resistance 999 4 true`);

  await send(`execute in ${inst} run kill @e[type=minecraft:item,distance=..80]`);
  await send(`execute in ${inst} run kill @e[type=minecraft:zombie,distance=..80]`);
  await sleep(1000);

  const spawnOut = await send(`mm mobs spawn level_1_grove_guardian 1 ${world},0.5,64,0.5`);
  console.log(`[SPAWN] ${spawnOut}`);

  await sleep(3500);
  const killOut = await send(`execute in ${inst} run kill @e[type=minecraft:zombie,distance=..160]`);
  console.log(`[KILL] ${killOut}`);
  await sleep(2500);

  const firstItem = await send(`execute in ${inst} run data get entity @e[type=minecraft:item,sort=nearest,limit=1,distance=..80] Item`);
  const dropped = killCountOf(await send(`execute in ${inst} run kill @e[type=minecraft:item,distance=..80]`));
  console.log(
    `RESULT-LOOT ${DIFF} dropped_entities=${dropped} first_item_present=${hasItemData(firstItem)} first_item_nbt=${String(firstItem).slice(0, 260)} stillInInst=${inInst(await dimOf())}`
  );

  await send(`minecraft:execute in minecraft:world run tp ${USER} 0 103 0`);
  await sleep(1000);
  try { await rcon.end(); } catch {}
  try { bot.quit(); } catch {}
  process.exit(0);
})().catch(e => {
  console.error('FATAL', e.message);
  process.exit(1);
});
