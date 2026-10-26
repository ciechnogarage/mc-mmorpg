'use strict';
// F1 zielarza: odrost kepy przezywa wyjscie, szybki powrot, restart i kill klona (pliki per konto, zapis w tle).
// Klon --z-kreatorem z terenem zielarza (MCMMORPG/profesje/zielarz/przygotuj_klon.sh; suche kepy kozlka Y -59).
// Uruchom: run.sh <klon> <sciezka>/trwalosc.js zbierz <nick>   -> zbior, relog po 5 s i od razu, PPM = "odrasta"
//          run.sh <klon> <sciezka>/trwalosc.js zrzut <nick>    -> zbior, 75 s online, konto zapisane zrzutem co 60 s
//          (restart / docker kill klona z zewnatrz; przy zrzut: kill, gdy w logu "ZRZUT gotowy")
//          run.sh <klon> <sciezka>/trwalosc.js sprawdz <nick>  -> po starcie: PPM w ta sama kepe = nadal "odrasta"
//          run.sh <klon> <sciezka>/trwalosc.js pusty <nick>    -> sciezka nieidealna: plik konta zepsuty (podlozony z hosta przy
//          graczu offline) -> PPM nie wisi na "Chwila, rozgladasz sie", tylko kepa daje sie zebrac; zly wpis w pliku = faza sprawdz
// Przygotowanie (poziom, sierp, tp) i sam zbior przez RCON jak w obciazenie.js (scena nie jest tematem F1);
// sprawdzenie trwalosci = prawdziwy PPM gracza w kepe i komunikat na pasku akcji.
const { scenariusz, sleep, postac, obserwuj } = require('/bot/lib');

const [, , faza = 'zbierz', aNick] = process.argv;
const NICK = aNick || 'F1Zbior', IMIE = 'Zi' + (Date.now() % 100000), KX = 71, KY = -59, KZ = 50;   // kepa kozlka z przygotuj_klon.sh

scenariusz('zielarz-trwalosc-' + faza, async ({ w, polacz, rcon }) => {
  const wejdz = async (imie) => {   // imie tylko dla nowego konta (zajete imie = dialog kreatora stoi)
    const bot = await polacz({ nick: NICK, czasMs: 90000 });
    const e = obserwuj(bot);
    const d = await postac.wejdz(bot, { imie });
    await rcon(`execute in minecraft:overworld run minecraft:teleport ${NICK} ${KX - 2 + 0.5} ${KY} ${KZ + 0.5} -90 30`);
    await sleep(1500);
    return { bot, e, d };
  };
  // PPM jak gracz: patrzy na kepe i klika; wynik czyta z paska akcji
  const ppm = async (bot, e, ms = 4000) => {
    const b = bot.blockAt(bot.entity.position.offset(2, 0, 0).floored());
    const cel = b && b.position.x === KX && b.position.z === KZ ? b : bot.blockAt(new (require('vec3'))(KX, KY, KZ));
    await bot.lookAt(cel.position.offset(0.5, 0.2, 0.5), true);
    e.historia.length = 0;
    await bot.activateBlock(cel).catch(() => {});
    const t0 = Date.now();
    while (Date.now() - t0 < ms) {
      const a = (e.historia.map(h => h.actionbar || '').concat([e.actionbar || ''])).join(' | ');
      if (/odrasta|rozgl|Cofnij|Podejd|Nie znasz/.test(a)) return a;
      await sleep(150);
    }
    return e.actionbar || '';
  };
  const magazyn = async () => (await rcon('rzemioslo magazyn')).replace(/^\[Rzemioslo\]\s*/, '');

  if (faza === 'zbierz' || faza === 'zrzut') {
    let { bot, e, d } = await wejdz(IMIE);
    await rcon(`mmocore admin level set ${NICK} herbalism 10`);
    await rcon(`rzemioslo narzedzie ${NICK} sierp 1`);
    w.sprawdz('wejscie', !!bot.entity, `droga=${d.droga}; ${await magazyn()}`);
    await rcon(`rzemioslo odrost ${NICK}`);
    let zebrano = false;
    for (let proba = 0; proba < 3 && !zebrano; proba++) {
      const st = await rcon(`rzemioslo kepa-start ${NICK} ${KX} ${KY} ${KZ}`);
      if (!/sesja/.test(st)) { console.log('kepa-start:', st, '| pasek:', e.actionbar); await sleep(1500); continue; }
      await sleep(1200);
      const ok = await rcon(`rzemioslo okaz ${NICK} pelny`);
      const m = ok.match(/boczne=\[([^\]]*)\]/);
      const bocz = m ? m[1].split(',').map(v => v.trim() === 'true') : [];
      for (let i = 0; i < bocz.length; i++) {
        if (!bocz[i]) continue;
        await rcon(`rzemioslo celuj ${NICK} gleba ${i} 0.5`); await sleep(500);
        bot.swingArm('right'); await sleep(700); bot.swingArm('right'); await sleep(700);
      }
      await rcon(`rzemioslo celuj ${NICK} rozeta 0 0.05`); await sleep(400);
      const t0 = Date.now();
      while (Date.now() - t0 < 16000) {
        const hold = Date.now() + 550;
        while (Date.now() < hold) { bot.activateItem(); await sleep(150); }
        bot.deactivateItem(); await sleep(400);
        if (/brak sesji/.test(await rcon(`rzemioslo okaz ${NICK}`))) { zebrano = true; break; }
      }
    }
    await sleep(3000);
    const poZbiorze = await magazyn();
    w.sprawdz('zbior', zebrano, poZbiorze);
    w.sprawdz('brudne-bez-zapisu', /brudne [1-9]/.test(poZbiorze), 'zbior oznacza konto, nie pisze od razu: ' + poZbiorze);
    let a = await ppm(bot, e);
    w.sprawdz('ppm-po-zbiorze', /odrasta/.test(a), a);
    if (faza === 'zrzut') {   // gracz zostaje w grze: zrzut co 60 s ma zapisac konto bez wyjscia (potem docker kill z zewnatrz)
      await sleep(75000);
      const m = await magazyn();
      w.sprawdz('zrzut-60s', /brudne 0/.test(m), m);
      console.log('ZRZUT gotowy, gracz online', new Date().toISOString());
      await sleep(600000);   // trzymaj online do kill klona
      return;
    }

    // wyjscie i powrot po 5 s: konto zapisane przy wyjsciu, wczytane w tle przy wejsciu
    bot.quit(); await sleep(5500);
    const poWyjsciu = await magazyn();
    w.sprawdz('wyjscie-zwalnia', /konta w pamieci 0/.test(poWyjsciu), poWyjsciu);
    ({ bot, e } = await wejdz(IMIE));
    a = await ppm(bot, e);
    w.sprawdz('relog-5s', /odrasta/.test(a), a + ' | ' + await magazyn());

    // sciezka nieidealna: powrot tuz po wyjsciu (najkrocej, ile puszcza connection-throttle 4 s; odczyt w tej samej kolejce co zapis)
    bot.quit(); await sleep(4200);
    ({ bot, e } = await wejdz(IMIE));
    a = await ppm(bot, e);
    w.sprawdz('relog-od-razu', /odrasta/.test(a), a + ' | ' + await magazyn());
    console.log('KONIEC zbierz', new Date().toISOString());
  } else if (faza === 'pusty') {
    const { bot, e, d } = await wejdz(IMIE);
    await rcon(`mmocore admin level set ${NICK} herbalism 10`);
    await rcon(`rzemioslo narzedzie ${NICK} sierp 1`);
    await sleep(2000);   // odczyt konta w tle juz dawno skonczony
    const a1 = await ppm(bot, e), a2 = await ppm(bot, e);
    w.sprawdz('nie-wisi', !/rozgl/.test(a1 + a2), `droga=${d.droga}; ${a1} || ${a2} | ${await magazyn()}`);
  } else {
    const { bot, e, d } = await wejdz(IMIE);
    await rcon(`mmocore admin level set ${NICK} herbalism 10`);   // po kill klona postac MMOProfiles bywa nowa (odrost jest per KONTO)
    await rcon(`rzemioslo narzedzie ${NICK} sierp 1`);
    const a = await ppm(bot, e);
    w.sprawdz('po-starcie', /odrasta/.test(a), `droga=${d.droga}; ${a} | ${await magazyn()}`);
  }
}, { limitMs: Number(process.env.LIMIT_MS || 300000) });
