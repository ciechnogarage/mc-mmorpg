// Loch 001: noc, pogoda i gamerule w INSTANCJI (poprawka minecraft: w skillu Loch_001, 90bfba5). Bot bez OP, wejscie jak gracz.
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh <klon --z-kreatorem> <repo>/kuznia/tools/sonda_l002/loch001_noc.js
const { scenariusz, postac, wejdzDoLochu, rcon, sleep } = require('/bot/lib');
scenariusz('loch001-noc', async ({ w, polacz }) => {
  const bot = await polacz({ nick: 'Noc001' });
  await postac.wejdz(bot, { imie: 'Noc' + Math.floor(Math.random() * 900 + 100) });
  const swiat = await wejdzDoLochu(bot, 'loch_001');
  w.sprawdz('wejscie', !!swiat && swiat.startsWith('loch_001_'), `swiat ${swiat}`);
  if (!swiat) return;
  await sleep(6000);   // TriggerDistance przy wejsciu odpala skill z regulami
  const ex = `minecraft:execute in minecraft:${swiat} run`;
  for (const [k, v] of [['fall_damage', 'false'], ['keep_inventory', 'true'], ['advance_time', 'false'], ['advance_weather', 'false'], ['mob_griefing', 'false']]) {
    const o = await rcon(`${ex} minecraft:gamerule ${k}`);
    w.sprawdz(`gamerule_${k}`, o.includes(v), o.trim());
  }
  const t = await rcon(`${ex} minecraft:time query daytime`);
  const m = /(\d+)/.exec(t); const dt = m ? +m[1] : -1;
  w.sprawdz('noc', dt >= 12000 && dt < 23000 || dt === 12000, t.trim());
  const t2 = (await sleep(3000), await rcon(`${ex} minecraft:time query daytime`));
  w.sprawdz('czas_stoi', t2.trim() === t.trim(), `${t.trim()} -> ${t2.trim()}`);
  console.log('mv gamerule list: ' + (await rcon(`mv gamerule list ${swiat}`)).slice(0, 600));
  bot.chat('/leave'); await sleep(1500);
}, { limitMs: 240000 });
