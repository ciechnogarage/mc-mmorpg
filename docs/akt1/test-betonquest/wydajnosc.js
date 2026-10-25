'use strict';
// Koszt BetonQuesta przy wielu mobach MM: BQ (NPC typu mythicmobs) słucha EntityMoveEvent, a Paper wywołuje je
// dla KAŻDEJ ruchomej encji dopiero, gdy ktoś słucha (docs/ref/betonquest/mythicmobs-npc.md:26).
// Ten sam przebieg na klonie z BQ i bez jego jara; porównanie MSPT (Paper `mspt`, okno 1 min).
// Gracz w creative = moby chodzą swobodnie, nie biją. 200 x AKT1_CEL (AI, MovementSpeed 0.15) w 4 kępach ~16 bl.
const { scenariusz, rcon, sleep } = require('/bot/lib');

const mspt = async () => {
  const t = (await rcon('mspt')).replace(/§.|\x1b\[[0-9;]*m/g, '');
  const m = t.match(/([\d.]+)\/([\d.]+)\/([\d.]+),\s*([\d.]+)\/([\d.]+)\/([\d.]+),\s*([\d.]+)\/([\d.]+)\/([\d.]+)/);
  return m ? { avg1m: +m[7], max1m: +m[9], avg10s: +m[4] } : { surowe: t };
};

scenariusz('akt1-bq-wydajnosc', async ({ w, polacz }) => {
  const nick = 'BotPerf' + (Date.now() % 10000);
  const bot = await polacz({ nick });
  await rcon(`minecraft:gamemode creative ${nick}`);
  await sleep(2000);
  await rcon('mm m kill AKT1_CEL'); await rcon('minecraft:kill @e[type=minecraft:item]');
  const bq = /BetonQuest/.test(await rcon('plugins'));
  await sleep(65000);
  const pusto = await mspt();
  for (const [x, z] of [[16, 0], [-16, 0], [0, 16], [0, -16]]) await rcon(`mm mobs spawn AKT1_CEL 50 world,${x},-59,${z}`);
  await sleep(3000);
  const ile = await rcon('execute if entity @e[type=minecraft:zombie]');
  await sleep(65000);
  const z200 = await mspt();
  w.sprawdz('pomiar', true, JSON.stringify({ betonquest: bq, moby: ile, bez_mobow: pusto, z_mobami: z200 }));
  await rcon('mm m kill AKT1_CEL'); await rcon('minecraft:kill @e[type=minecraft:item]');
}, { limitMs: 200000 });
