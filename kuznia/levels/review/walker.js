// Pieszy przebieg bez teleportow: non-op bot idzie wzdluz osi sciezki od spawnu do areny.
const mineflayer = require('/e/node_modules/mineflayer');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const NICK = process.env.NICK || 'WalkBot';
const bot = mineflayer.createBot({ host: 'mc-forge', port: 25565, username: NICK, auth: 'offline' });
const chat = [], titles = []; let world = null, changes = 0;
bot._client.on('respawn', p => { changes++; world = p.worldState ? p.worldState.name : String(p.worldName); });
bot.on('messagestr', m => { if (m.trim()) { chat.push(m.slice(0, 140)); console.log('  [czat]', m.slice(0, 140)); } });
bot._client.on('set_title_text', d => titles.push(JSON.stringify(d).slice(0, 80)));
bot.on('death', () => console.log('  [SMIERC]', bot.entity && bot.entity.position.toString()));
const T0 = Date.now(); const t = () => ((Date.now() - T0) / 1000).toFixed(1) + 's';
bot.once('spawn', async () => {
  await sleep(2500);
  if (process.env.SELFPLAY === '1') bot.chat('/md play poziom_01');
  for (let i = 0; i < 80 && !(world && /poziom_01_/.test(world)); i++) await sleep(250);
  console.log(t(), 'swiat', world);
  for (let i = 0; i < 60 && !bot.entity.onGround; i++) await sleep(250);
  await sleep(3000);
  console.log(t(), 'wyladowal', bot.entity.position.toString(), 'hp', bot.health);
  // komendy gracza bez OP w lochu (AllowCommands: false)
  bot.chat('/me test_komendy'); bot.chat('/md lives'); bot.chat('/help'); await sleep(1500);
  const wps = []; for (let z = 57; z >= -38; z -= 3) wps.push([9 * Math.sin((z + 10) / 16), z]);
  let stuck = 0, maxStuck = 0; const tw = Date.now();
  for (const [x, z] of wps) {
    let tries = 0;
    while (true) {
      const p = bot.entity.position; const d = Math.hypot(p.x - x, p.z - z);
      if (d < 1.2) break;
      if (++tries > 120) { console.log(t(), `  NIE DOSZEDL do (${x.toFixed(1)},${z})`, p.toString()); stuck++; break; }
      await bot.lookAt(bot.entity.position.offset(x - p.x, 0, z - p.z).offset(0, 1.6, 0), true);
      bot.setControlState('forward', true); bot.setControlState('sprint', true);
      bot.setControlState('jump', !!bot.entity.isCollidedHorizontally || tries % 6 > 3);
      await sleep(150);
    }
  }
  bot.clearControlStates();
  console.log(t(), `marsz spawn->arena: ${((Date.now() - tw) / 1000).toFixed(1)} s, nieosiagniete punkty=${stuck}, pozycja ${bot.entity.position.toString()}, hp ${bot.health}`);
  await sleep(4000);
  const mobs = {}; for (const e of Object.values(bot.entities)) if (e !== bot.entity && e.position.distanceTo(bot.entity.position) < 30) { let n = e.name; try { const c = e.getCustomName && e.getCustomName(); if (c) n += ':' + c.toString(); } catch {} mobs[n] = (mobs[n] || 0) + 1; }
  console.log(t(), 'moby w 30:', JSON.stringify(mobs));
  console.log('tytuly:', titles.join(' | '));
  bot.chat('/md leave'); await sleep(3000); bot.quit(); setTimeout(() => process.exit(0), 500);
});
setTimeout(() => { console.log('[watchdog]'); process.exit(9); }, 240000);
