'use strict';
// Obciazenie do pomiaru lagow (napad pomiar): bot dolacza do pierwszego napadu bez cial (drugi bot
// puszczony chwile pozniej trafia do innego), walczy ~70 s jak gracz (miecz, podejscie, cios), potem wychodzi.
// Bez aktywnego napadu bot stoi 70 s online (pomiar kontrolny: koszt samego gracza, bez napadow).
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh <klon> <sciezka>/testy/pomiar.js  (najwyzej 2 naraz, osobne run.sh)
const { scenariusz, czekaj, sleep, stan } = require('/bot/lib');
const { pathfinder, goals, Movements } = require('mineflayer-pathfinder');

scenariusz('napady-pomiar', async ({ w, polacz, rcon }) => {
  const nick = 'BotPom' + (Date.now() % 100000);
  const bot = await polacz({ nick, plugins: [pathfinder] });
  const mv = new Movements(bot); mv.canDig = false; mv.allow1by1towers = false; bot.pathfinder.setMovements(mv);
  await rcon(`minecraft:give ${nick} minecraft:iron_sword`);
  await rcon(`minecraft:gamemode survival ${nick}`);
  await sleep(1000);
  const linie = (await rcon('napad stan')).split('\n').filter((l) => /^#\d+ /.test(l) && !/KONIEC/.test(l));
  const wolny = linie.find((l) => /\| lod SYM /.test(l)) || linie[0];
  const id = wolny && wolny.match(/^#(\d+)/)[1];
  if (!id) {
    w.sprawdz('kontrola-bez-napadu', true, 'brak napadu, bot stoi 70 s');
    await sleep(70000);
    return;
  }
  bot.chat(`/napad dolacz ${id} obrona`);
  const tam = await czekaj(async () => (await stan.wymiar(nick)) === 'minecraft:napady' && 'ok', 20000, 300);
  w.sprawdz('dolaczyl', !!tam, `napad ${id}`);
  const sword = bot.inventory.items().find((i) => i.name === 'iron_sword');
  if (sword) await bot.equip(sword, 'hand');
  const helm = (e) => (e.equipment && e.equipment[5] && e.equipment[5].name) || '';
  let ciosy = 0;
  const koniec = Date.now() + 70000;
  while (Date.now() < koniec) {
    if (bot.game.gameMode === 'spectator' || !bot.entity) { await sleep(1000); continue; }
    const wrogowie = Object.values(bot.entities).filter((e) => e !== bot.entity && /chainmail_helmet/.test(helm(e)));
    const cel = wrogowie.sort((a, b) => a.position.distanceTo(bot.entity.position) - b.position.distanceTo(bot.entity.position))[0];
    if (!cel) { await sleep(500); continue; }
    if (cel.position.distanceTo(bot.entity.position) < 3) { await bot.lookAt(cel.position.offset(0, 1.5, 0)); bot.attack(cel); ciosy++; await sleep(650); }
    else { bot.pathfinder.setGoal(new goals.GoalNear(cel.position.x, cel.position.y, cel.position.z, 2)); await sleep(400); }
  }
  bot.pathfinder.setGoal(null);
  w.sprawdz('walczyl', true, `ciosy ${ciosy}, napad ${id}`);
  bot.chat('/napad opusc');
  await sleep(1500);
});
