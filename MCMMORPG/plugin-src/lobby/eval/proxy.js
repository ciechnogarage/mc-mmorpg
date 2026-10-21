const mineflayer = require('mineflayer'); const sleep = ms => new Promise(r => setTimeout(r, ms));
(async () => {
  const bot = mineflayer.createBot({ host: 'mc-proxy-1', port: 25577, username: 'LobbyQA', auth: 'offline', version: '1.21.11' });
  let spawns = 0; bot.on('spawn', () => spawns++);
  bot._client.on('add_resource_pack', p => { bot._client.write('resource_pack_receive', { uuid: p.uuid, result: 3 }); bot._client.write('resource_pack_receive', { uuid: p.uuid, result: 0 }); });
  bot.on('kicked', r => console.log('KICK', JSON.stringify(r).slice(0, 200)));
  await new Promise(r => bot.once('spawn', r)); await sleep(8000);
  const p = bot.entity.position; console.log('lobby pos', p.toString(), 'gm', bot.game.gameMode, 'holo', Object.values(bot.entities).filter(e => e.name === 'text_display').length);
  // spacer jak gracz: obejscie fontanny od wschodu, potem prosto na polnoc przez wyrzutnie
  const go = async (x, z, ms) => { await bot.lookAt(bot.entity.position.clone().set(x, 66.6, z), true); bot.setControlState('forward', true); const t = Date.now(); while (Date.now() - t < ms && Math.hypot(bot.entity.position.x - x, bot.entity.position.z - z) > 0.6 && spawns === 1) await sleep(50); bot.setControlState('forward', false); };
  await go(4.5, 4.5, 3000); await go(4.5, -4.5, 4000); await go(0.5, -7.0, 3000); await go(0.5, -30, 7000);
  await sleep(4000);
  console.log('spawny', spawns, 'swiat pos', bot.entity.position.toString(), spawns >= 2 ? 'PASS przerzut na inny serwer' : 'FAIL brak przerzutu');
  bot.quit(); process.exit(0);
})().catch(e => { console.error(e); process.exit(2); });
