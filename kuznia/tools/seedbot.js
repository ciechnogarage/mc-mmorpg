// Bot testu ziarna: 2x wejscie do seedA (szablon Z level.dat) i 2x do seedB (BEZ level.dat).
// Odczyt ziarna robimy z logu Irisa ("Initializing Engine: <swiat>/probe2 ... Seed: N"),
// bot tylko wydaje komendy i loguje czat, zeby bylo widac co odpowiada MythicDungeons.
const mineflayer = require('/e/node_modules/mineflayer');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const bot = mineflayer.createBot({ host: process.env.MC_HOST || 'klon-kuznia', port: 25565,
                                   username: process.env.NICK || 'SeedBot', auth: 'offline' });
let worldChanges = 0;
bot._client.on('respawn', p => { worldChanges++;
  console.log(`[swiat] respawn #${worldChanges} ->`, JSON.stringify(p.worldState ? p.worldState.name : p.worldName || p)); });
bot.on('messagestr', m => { if (m.trim()) console.log('[czat]', m.slice(0, 160)); });
bot.on('kicked', r => console.log('[KICK]', r));
bot.on('error', e => console.log('[ERR]', e.message));
bot.once('spawn', async () => {
  console.log('[bot] wszedl na serwer');
  await sleep(3000);
  for (const d of ['seedA', 'seedA', 'seedB', 'seedB']) {
    const before = worldChanges;
    console.log(`\n=== /md play ${d} ===`);
    bot.chat(`/md play ${d}`);
    for (let i = 0; i < 40 && worldChanges === before; i++) await sleep(500);
    console.log(`[bot] zmiana swiata: ${worldChanges > before ? 'TAK' : 'NIE'}  pos=${bot.entity && bot.entity.position}`);
    await sleep(12000);                           // niech Iris wygeneruje chunki wokol gracza
    const b2 = worldChanges;
    bot.chat('/md leave');
    for (let i = 0; i < 40 && worldChanges === b2; i++) await sleep(500);
    await sleep(4000);
  }
  console.log('\n[bot] koniec'); bot.quit(); setTimeout(() => process.exit(0), 1500);
});
setTimeout(() => { console.log('[FATAL] watchdog'); process.exit(9); }, 240000);
