// Sonda walki: bot stoi (ADVENTURE, bez odpornosci), obok pojawia sie mob; logujemy HP bota i trafienia.
const mineflayer = require('/e/node_modules/mineflayer');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const MOB = process.env.MOB, HIT = process.env.HIT === '1', SECS = +(process.env.SECS || 20);
const bot = mineflayer.createBot({ host: 'mc-forge', port: 25565, username: 'SondaBot', auth: 'offline' });
const chat = []; bot.on('messagestr', m => { if (m.trim()) chat.push(m.slice(0, 120)); });
bot.once('spawn', async () => {
  await sleep(3000);
  const p = bot.entity.position; console.log('pozycja', p.toString(), 'hp', bot.health);
  const t0 = Date.now(); let last = bot.health, hits = 0, deaths = 0;
  bot.on('health', () => { if (bot.health < last) { hits++; console.log(`  t=${((Date.now()-t0)/1000).toFixed(1)}s hp ${last.toFixed(1)} -> ${bot.health.toFixed(1)}`); } last = bot.health; });
  bot.on('death', () => { deaths++; console.log(`  SMIERC t=${((Date.now()-t0)/1000).toFixed(1)}s`); });
  for (let i = 0; i < SECS * 2; i++) {
    await sleep(500);
    if (HIT) { const e = bot.nearestEntity(e => e.name === 'wolf' || e.name === 'pig'); if (e && e.position.distanceTo(bot.entity.position) < 4) { bot.lookAt(e.position.offset(0, 0.5, 0)); bot.attack(e); } }
  }
  const mobs = {}; for (const e of Object.values(bot.entities)) if (e !== bot.entity && e.position.distanceTo(bot.entity.position) < 30) mobs[e.name] = (mobs[e.name] || 0) + 1;
  console.log(`WYNIK ${MOB}: trafien=${hits} smierci=${deaths} hp_koniec=${bot.health} moby_w_30=${JSON.stringify(mobs)}`);
  console.log('czat:', chat.slice(-6).join(' || '));
  bot.quit(); setTimeout(() => process.exit(0), 500);
});
