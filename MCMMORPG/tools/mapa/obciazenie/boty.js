// Test obciazenia mapy: N botow chodzi w losowych kierunkach, trzymajac (albo nie) mape w slocie 7.
// Uzycie (node:20, node_modules z /opt/docker/mc/forge/gracz): N=20 HOST=mapa-proba MAPA=1 CZAS=90 node boty.js
const mineflayer = require('mineflayer');
const N = +process.env.N || 10, HOST = process.env.HOST || 'mapa-proba', MAPA = process.env.MAPA !== '0';
const CZAS = (+process.env.CZAS || 90) * 1000, PREFIX = process.env.PREFIX || 'Bot';
let gotowe = 0;
function bot(i) {
  const b = mineflayer.createBot({ host: HOST, port: 25565, username: PREFIX + i, version: '1.21.11', auth: 'offline' });
  b.once('spawn', () => {
    gotowe++;
    setTimeout(() => {
      b.setQuickBarSlot(MAPA ? 7 : 0);   // serwer pamieta slot z poprzedniego przebiegu - ustaw zawsze
      b.setControlState('forward', true); b.setControlState('sprint', Math.random() < 0.5);
      setInterval(() => { b.look(Math.random() * Math.PI * 2, 0, true); b.setControlState('jump', Math.random() < 0.3); }, 2500 + Math.random() * 1500);
    }, 3000);
  });
  b.on('error', e => console.log('blad', i, e.message));
  b.on('kicked', r => console.log('kick', i, JSON.stringify(r).slice(0, 120)));
  return b;
}
const boty = [];
for (let i = 0; i < N; i++) setTimeout(() => boty.push(bot(i)), i * 400);
setInterval(() => console.log('online', gotowe), 15000);
setTimeout(() => { boty.forEach(b => b.quit()); console.log('koniec'); setTimeout(() => process.exit(0), 2000); }, N * 400 + CZAS);
