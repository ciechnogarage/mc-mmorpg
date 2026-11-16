// Test obciazenia v2 (po recenzji 02.10: v1 trzymal pusta mape 30000). TRYB=nic|reka|pelny.
// reka: po dostaniu mapy przelacz slot 0->7 (plugin podmienia 30000 na osobista), losowo 0-5x PPM (przyblizenie), chodz.
// pelny: jak reka, ale stoj, Shift+PPM otwiera pelny ekran, rozgladaj sie (kursor). nic: slot 0, chodz.
const mineflayer = require('mineflayer');
const N = +process.env.N || 10, HOST = process.env.HOST || 'mapa-proba', TRYB = process.env.TRYB || 'reka';
const CZAS = (+process.env.CZAS || 120) * 1000, PREFIX = process.env.PREFIX || 'Bot';
const czekaj = ms => new Promise(r => setTimeout(r, ms));
let gotowe = 0, zMapa = 0;
function bot(i) {
  const b = mineflayer.createBot({ host: HOST, port: 25565, username: PREFIX + i, version: '1.21.11', auth: 'offline' });
  let start = false;
  b.once('spawn', async () => {
    gotowe++;
    await czekaj(3000); b.setQuickBarSlot(0);
    if (TRYB === 'nic') return chodz(b);
    b.inventory.on('updateSlot', async (slot, stare, nowe) => {
      if (start || slot !== 43 || !nowe || nowe.name !== 'filled_map') return;
      start = true; zMapa++;
      await czekaj(500 + Math.random() * 2000); b.setQuickBarSlot(7);
      await czekaj(1500);
      if (TRYB === 'reka') {
        const k = Math.floor(Math.random() * 6);
        for (let j = 0; j < k; j++) { b.activateItem(); await czekaj(400); b.deactivateItem(); await czekaj(400); }
        chodz(b);
      } else {
        b.setControlState('sneak', true); await czekaj(300); b.activateItem(); await czekaj(200); b.deactivateItem();
        await czekaj(300); b.setControlState('sneak', false);
        await czekaj(2500);
        const ramki = Object.values(b.entities).filter(e => /item_frame/.test(e.name || '') && e.position.distanceTo(b.entity.position) < 8).length;
        console.log('pelny', i, 'ramki', ramki);
        setInterval(() => b.look(b.entity.yaw + (Math.random() - 0.5) * 0.6, (Math.random() - 0.5) * 0.5, false), 250);
      }
    });
  });
  b.on('error', e => console.log('blad', i, e.message));
  b.on('kicked', r => console.log('kick', i, JSON.stringify(r).slice(0, 120)));
  return b;
}
function chodz(b) {
  b.setControlState('forward', true); b.setControlState('sprint', Math.random() < 0.5);
  setInterval(() => { b.look(Math.random() * Math.PI * 2, 0, true); b.setControlState('jump', Math.random() < 0.3); }, 2500 + Math.random() * 1500);
}
const boty = [];
for (let i = 0; i < N; i++) setTimeout(() => boty.push(bot(i)), i * 400);
setInterval(() => console.log('online', gotowe, 'z mapa', zMapa), 15000);
setTimeout(() => { boty.forEach(b => b.quit()); console.log('koniec'); setTimeout(() => process.exit(0), 2000); }, N * 400 + CZAS);
