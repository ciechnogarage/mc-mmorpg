'use strict';
// Prawdziwe kliknięcie gracza (pakiet attack) w encję najbliższą punktowi — sprawdza pod-hitboxy ModelEngine.
// run.sh trafienia klik.js <nick> <x> <y> <z>
const { scenariusz, sleep } = require('/bot/lib');
const [nick, x, y, z] = [process.argv[2] || 'TrafKlik', +process.argv[3], +process.argv[4], +process.argv[5]];
scenariusz('trafienia-klik', async ({ w, polacz }) => {
  const bot = await polacz({ nick });
  await sleep(+(process.env.CZEKAJ_MS || 7000));   // test teleportuje bota pod Kosiarza
  const Vec3 = require('vec3');
  const p = new Vec3(x, y, z);
  const blisko = Object.values(bot.entities).filter(e => e !== bot.entity && e.position.distanceTo(p) < 2.5)
    .map(e => ({ e, d: e.position.offset(0, (e.height || 0) / 2, 0).distanceTo(p) })).sort((a, b) => a.d - b.d);
  console.log('ENCJE ' + blisko.map(b => `${b.e.id}:${b.e.name}:${b.e.type}:${b.d.toFixed(2)}`).join(' '));
  // pod-hitbox ME to encja nieznana mineflayerowi (name undefined) tuż przy punkcie części
  const cel = blisko.find(b => b.d < 0.6 && b.e.type !== 'player' && !b.e.name);
  w.sprawdz('hitbox-widoczny', !!cel, cel ? `${cel.e.name} id ${cel.e.id}` : 'brak encji pod-hitboxu przy punkcie');
  if (cel) {
    await bot.lookAt(p, true);
    await sleep(300);
    bot.attack(cel.e);
    await sleep(1500);
  }
}, { limitMs: 60000 });
