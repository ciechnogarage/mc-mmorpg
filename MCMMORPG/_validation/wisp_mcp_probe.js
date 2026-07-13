// Probe: weryfikacja bindingu ModelEngine (pakietowego) dla level_1_glimmer_wisp.
// Bot mineflayer łączy się przez velocity, RCON przez docker exec (backendy bez mapowanych portów).
const mineflayer = require('mineflayer');
const { execFileSync } = require('child_process');
const H = '127.0.0.1', P = 25565, U = 'WispProbe';
const sleep = ms => new Promise(r => setTimeout(r, ms));
const rcon = c => execFileSync('docker', ['exec', '-i', 'srv-world', 'rcon-cli', c], { encoding: 'utf8' }).replace(/§[0-9a-fklmnorx]/g, '').trim();
const wf = async (f, to = 40000, st = 400) => { const t = Date.now(); while (Date.now() - t < to) { const v = await f(); if (v) return v; await sleep(st); } return false; };
(async () => {
  const bot = mineflayer.createBot({ host: H, port: P, username: U, auth: 'offline', version: '1.21.11' });
  bot.on('resourcePack', () => bot.acceptResourcePack());
  bot.on('error', () => {}); bot.on('end', () => {}); process.on('uncaughtException', () => {});
  if (!await wf(() => bot.entity)) { console.log('FAIL: bot nie zespawnował się'); process.exit(7); }
  rcon(`op ${U}`); await sleep(500);
  rcon(`tp ${U} 8 88 8`); await sleep(1500);
  // ME/MM rozgrzewają się po boocie — bez karencji trafiają się fałszywe FAIL (brak displayów)
  let counts = {};
  for (let attempt = 0; attempt < 3; attempt++) {
    rcon('mm mobs spawn level_1_glimmer_wisp 1 world,8,88,8'); await sleep(3000);
    const near = Object.values(bot.entities)
      .filter(e => e.position && e.position.distanceTo(bot.entity.position) < 24 && e.username !== U)
      .map(e => e.name || e.type);
    counts = {};
    near.forEach(n => counts[n] = (counts[n] || 0) + 1);
    if ((counts.item_display || 0) >= 1) break;
    rcon('mm mobs killall'); await sleep(8000);
  }
  console.log('entities<24b po spawnie:', JSON.stringify(counts));
  const displays = (counts.item_display || 0) + (counts.block_display || 0) + (counts.armor_stand || 0);
  const husk = counts.husk || 0;
  console.log(husk >= 1 ? 'OK: husk (baza MM) widoczny' : 'FAIL: brak huska');
  console.log(displays >= 1 ? `OK: ModelEngine binding — ${displays} encji display u klienta` : 'FAIL: brak encji display (model niepodpięty?)');
  rcon('mm mobs killall'); process.exit(displays >= 1 && husk >= 1 ? 0 : 3);
})();
