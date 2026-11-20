'use strict';
// Flaga postaci Rzemiosla (`rzemioslo flaga`, `%rzemioslo_flaga_<klucz>%`) jest per POSTAC (profil MMOProfiles), nie per konto.
// Klon `klon.sh start <n> --z-kreatorem`, bot bez OP. Dwie fazy (miedzy nimi `klon.sh restart <n>`):
//   run.sh <n> testy/flaga-postaci.js faza1 <nick>   A: ustaw -> tak; B (nowa postac, to samo konto): pusto; z powrotem A: tak
//   run.sh <n> testy/flaga-postaci.js faza2 <nick>   po restarcie: wejscie na A -> nadal tak; usun -> pusto; zly klucz odrzucony
// RCON tylko do ustawienia flagi (tak jak zrobi to BetonQuest z konsoli) i odczytu prawdy (papi parse).
const { scenariusz, postac, stan, sleep } = require('/bot/lib');

const [faza = 'faza1', nick = 'FlagaT' + (Date.now() % 10000)] = process.argv.slice(2);
const KLUCZ = 'kuznauka_test';
const PH = `%rzemioslo_flaga_${KLUCZ}%`;

scenariusz('flaga-postaci-' + faza, async ({ w, polacz, rcon }) => {
  const bot = await polacz({ nick });
  // bez OP: scenariusz nie nadaje OP; ops.json klona sprawdza sie z hosta (klon bierze ops.json z world1)
  const ph = async () => (await stan.papi(nick, PH)).trim();
  const ustaw = async (wart) => rcon(`rzemioslo flaga ${nick} ${KLUCZ}${wart ? ' ' + wart : ''}`);
  const profil = s => (s.match(/postac ([0-9a-f-]{36})/) || [])[1];

  if (faza === 'faza1') {
    const a = await postac.wejdz(bot, { imie: 'Flaga' + (Date.now() % 1000) });
    w.sprawdz('A-wejscie', !!a, `droga ${a && a.droga}, imie ${a && a.imie}`);
    await sleep(2000);
    w.sprawdz('A-przed', (await ph()) === '', `przed ustawieniem: "${await ph()}"`);
    const odp = await ustaw('tak');
    const profA = profil(odp);
    w.sprawdz('A-ustaw', /flaga kuznauka_test = tak/.test(odp) && !!profA, odp);
    w.sprawdz('A-tak', (await ph()) === 'tak', `placeholder "${await ph()}"`);

    await postac.zmien(bot, { nowa: true, imie: 'Flagb' + (Date.now() % 1000), rasa: 1, klasa: 0 });
    await sleep(3000);
    const profB = profil(await rcon(`rzemioslo flaga ${nick} sonda_b x`));
    await rcon(`rzemioslo flaga ${nick} sonda_b`);
    w.sprawdz('B-inny-profil', !!profB && profB !== profA, `A ${profA}, B ${profB}`);
    w.sprawdz('B-pusto', (await ph()) === '', `placeholder na B "${await ph()}"`);

    const r = await postac.zmien(bot, { postac: new RegExp(a.imie) });
    await sleep(3000);
    const profA2 = profil(await rcon(`rzemioslo flaga ${nick} sonda_a x`));
    await rcon(`rzemioslo flaga ${nick} sonda_a`);
    w.sprawdz('A2-ten-sam-profil', profA2 === profA, `A ${profA}, teraz ${profA2}, przyciski ${JSON.stringify(r && r.przyciski)}`);
    w.sprawdz('A2-tak', (await ph()) === 'tak', `placeholder po powrocie na A "${await ph()}"`);
    console.log('PROFIL_A ' + profA + ' PROFIL_B ' + profB + ' IMIE_A ' + a.imie);
  } else {
    const r = await postac.wejdz(bot, {});
    w.sprawdz('wejscie', !!r, `droga ${r && r.droga}`);
    await sleep(3000);
    const prof = profil(await rcon(`rzemioslo flaga ${nick} sonda_a x`));
    await rcon(`rzemioslo flaga ${nick} sonda_a`);
    console.log('PROFIL_PO_RESTARCIE ' + prof);
    w.sprawdz('po-restarcie-tak', (await ph()) === 'tak', `placeholder "${await ph()}" (profil ${prof})`);
    const zly = await rcon(`rzemioslo flaga ${nick} zly.klucz tak`);
    w.sprawdz('zly-klucz', /klucz flagi tylko/.test(zly), zly);
    await ustaw(null);
    w.sprawdz('usun', (await ph()) === '', `po usunieciu "${await ph()}"`);
    await ustaw('tak'); // zostaw stan do odczytu pliku z hosta
  }
}, { limitMs: 280000 });
