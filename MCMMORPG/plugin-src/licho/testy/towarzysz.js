'use strict';
// Drugi gracz do oględzin rundy klientem QA: bot wchodzi z menu (Q -> Drużyna -> LICHO -> Dołącz).
// Jako Licho chowa się w snopku przy bramie i stoi; jako Zamawiacz tylko czeka. Koniec, gdy runda się skończy.
//   /opt/docker/mc/narzedzia/bot/run.sh licho testy/towarzysz.js
const { scenariusz, rcon, czekaj, sleep, stan, postac, otworzHub, kliknijSlot, ruchGracza, idz, tytulOkna } = require('/bot/lib');
const SNOPEK = { x: -6, y: 64, z: 19 };

scenariusz('licho-towarzysz', async ({ w, polacz }) => {
  const nick = 'LiT' + (Date.now() % 10000);
  const bot = await polacz({ nick });
  await postac.wejdz(bot, { imie: 'Towa' + nick.slice(-4) });
  await czekaj(async () => !/creator_limbo/.test(await stan.wymiar(nick)) || null, 15000, 500);
  await sleep(1500);
  let hub = null;
  for (let i = 0; i < 3 && !hub; i++) hub = await otworzHub(bot).catch(async () => { await sleep(1500); return null; });
  // Q po kreatorze bywa martwe u bota (znalezisko poza Lichem, patrz runda.js)
  if (!hub) { await rcon(`dm open hub_glowne ${nick}`); await czekaj(() => bot.currentWindow, 4000, 100); }
  const t = () => (tytulOkna(bot.currentWindow) || '-').slice(0, 12);
  console.log('hub', hub ? 'Q' : 'dm', t());
  await kliknijSlot(bot, 38); console.log('po 38', t());
  await kliknijSlot(bot, 25); console.log('po 25', t());
  await kliknijSlot(bot, 5, { noweOkno: false });
  w.sprawdz('wejscie', !!await czekaj(async () => (await stan.wymiar(nick)) === 'minecraft:licho' || null, 8000, 400), 'w zagrodzie');
  const s = await czekaj(async () => { const t = (await rcon('licho stan')).replace(/\s+/g, ' '); return /faza=ROZPROSZENIE/.test(t) ? t : null; }, 240000, 1000);
  const licho = s && new RegExp(`${nick} LICHO`).test(s);
  w.sprawdz('rola', !!s, licho ? 'LICHO' : 'ZAMAWIACZ');
  if (licho) {
    ruchGracza(bot);
    await idz(bot, SNOPEK.x + 2, SNOPEK.y, SNOPEK.z, { r: 1, ms: 20000 });
    const z = Object.values(bot.entities).find(e => e.name === 'interaction' && e.position.distanceTo({ x: SNOPEK.x + 0.5, y: SNOPEK.y, z: SNOPEK.z + 0.5 }) < 0.6);
    if (z) { await bot.lookAt(z.position.offset(0, 0.5, 0)); bot.activateEntity(z); }
    bot.clearControlStates();
  }
  await czekaj(async () => /Brak rundy|faza=KONIEC/.test(await rcon('licho stan')) || null, 300000, 2000);
  await sleep(10000);
}, { limitMs: 600000 });
