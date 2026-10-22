'use strict';
// Bot-gracz do testu Trafien: wchodzi i stoi (celowanie i ciosy robi serwer: /trafienia celuj|cios = prawdziwy player.attack()).
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh trafienia stoi.js <nick> <sekundy>
const { scenariusz, sleep } = require('/bot/lib');
const nick = process.argv[2] || 'TrafBot', czas = +(process.argv[3] || 600);
scenariusz('trafienia-stoi', async ({ w, polacz }) => {
  const bot = await polacz({ nick });
  w.sprawdz('spawn', !!bot.entity, `${nick} na klonie`);
  await sleep(czas * 1000);
}, { limitMs: (czas + 60) * 1000 });
