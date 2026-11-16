'use strict';
// Sonda wyscigu zmiany klasy (16.11): zmiana klasy -> mi give miecza dwurecznego -> PPM po `PAUZA` ms, PROBY razy.
//   run.sh <klon> qa/sonda-klasa.js [proby] [pauza_ms]   (debug: true; licz linie MOCNY/ZWOD bota w logu klona)
// Pomiar 16.11: bieglosc (%mythiclib_stat_additional_bieglosc_miecz_dwureczny%) jest 1 po 1 ms od admin class,
// pasek 0-8 bez zmian przez 3 s (umiejetnosci MMOCore nie sa przedmiotami w Inventory).
// Wynik 16.11: pauza 0 ms -> ciosy 0/4 (SONDA_PPM aktywny=AIR), pauza 600 ms -> 4/4. Przyczyna NIE klasa: przedmiot z komendy
// (mi give, nagroda komenda) nie wywoluje zdarzen, po ktorych AnimacjeBroni dokleja komponenty (wejscie, klik, podniesienie,
// zmiana slotu), wiec czeka na zegar dopasujDlonie co 10 t (AnimacjeBroni.java onEnable) -> do 0,5 s bez PPM.
const { scenariusz, sleep, postac, czekaj } = require('/bot/lib');
const PROBY = +(process.argv[2] || 4), PAUZA = +(process.argv[3] || 0);
const NICK = 'BotKl' + (Date.now() % 10000);
scenariusz('sonda-klasa', async ({ w, polacz, rcon }) => {
  const bot = await polacz({ nick: NICK });
  w.sprawdz('postac', !!(await postac.wejdz(bot, {})), NICK);
  await sleep(1500);
  await rcon(`minecraft:gamemode survival ${NICK}`);
  await rcon(`execute in minecraft:overworld run teleport ${NICK} 0.5 -59 0.5 0 0`);
  for (let i = 0; i < PROBY; i++) {
    await rcon(`mmocore admin class ${NICK} ${i % 2 ? 'JASTRZAB' : 'BASTION'}`); await sleep(2500);
    await rcon(`minecraft:clear ${NICK}`); await sleep(300);
    await rcon(`mmocore admin class ${NICK} RYCERZ_SMIERCI`);
    await rcon(`mi give GREATSWORD PRZ_WOJ_MIECZ_DWURECZNY_1 ${NICK} 1`);
    await czekaj(() => bot.heldItem && bot.heldItem.name === 'iron_sword', 3000, 50);
    await sleep(PAUZA);
    await rcon(`mmocore admin resource-stamina set ${NICK} 20`);
    await rcon(`say SONDA ${NICK} proba=${i}`);
    bot.activateItem(); await sleep(1400); bot.deactivateItem(); await sleep(800);
  }
  w.sprawdz('koniec', true, 'wynik w logu klona: MOCNY po kazdej linii SONDA');
}, { limitMs: 180000 });
