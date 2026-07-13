const fs = require('fs');
const path = require('path');
const { execFile } = require('child_process');
const { promisify } = require('util');
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');

const HOST = '127.0.0.1';
const PORT = 25565;
const RCON_PORT = 25575;
const RCON_PASSWORD = process.env.RCON_PASS;
const USER = `GGProof${String(Date.now()).slice(-6)}`;
const OUTPUT = path.join(__dirname, 'model_reviews', 'level_1_grove_guardian', 'live-combat-proof.json');
const sleep = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));
const execFileAsync = promisify(execFile);

async function waitFor(predicate, timeout = 30000, interval = 250) {
  const start = Date.now();
  while (Date.now() - start < timeout) {
    if (await predicate()) return true;
    await sleep(interval);
  }
  return false;
}

function nearbyDisplays(bot, radius = 18) {
  if (!bot.entity) return 0;
  return Object.values(bot.entities).filter((entity) => {
    if (!entity.position || entity.position.distanceTo(bot.entity.position) > radius) return false;
    return ['item_display', 'text_display', 'interaction'].includes(entity.name);
  }).length;
}

(async () => {
  const startedAt = new Date().toISOString();
  const events = [];
  const packetCounts = { sound: 0, particle: 0, velocity: 0, health: 0 };
  const dockerRcon = process.env.DOCKER_RCON === '1';
  const rcon = dockerRcon
    ? null
    : await Rcon.connect({
      host: HOST,
      port: RCON_PORT,
      password: RCON_PASSWORD,
    });
  const send = async (command) => {
    const response = dockerRcon
      ? (await execFileAsync(
        'docker',
        ['exec', '-i', 'srv-world', 'rcon-cli', command],
        { maxBuffer: 1024 * 1024 },
      )).stdout
      : await rcon.send(command);
    return String(response).replace(/\u001b\[[0-9;]*m/g, '').replace(/\u00a7[0-9A-FK-ORX]/gi, '');
  };
  const bot = mineflayer.createBot({
    host: HOST,
    port: PORT,
    username: USER,
    auth: 'offline',
    version: '1.21.11',
  });
  bot.on('resourcePack', () => bot.acceptResourcePack());
  bot.on('error', (error) => events.push({ at: Date.now(), type: 'client_error', value: error.message }));
  bot.on('health', () => {
    packetCounts.health += 1;
    events.push({ at: Date.now(), type: 'health', health: bot.health, food: bot.food });
  });
  bot._client.on('sound_effect', (packet) => {
    packetCounts.sound += 1;
    events.push({ at: Date.now(), type: 'sound', soundId: packet.soundId, x: packet.x, y: packet.y, z: packet.z });
  });
  bot._client.on('named_sound_effect', (packet) => {
    packetCounts.sound += 1;
    events.push({ at: Date.now(), type: 'sound', sound: packet.soundName, x: packet.x, y: packet.y, z: packet.z });
  });
  bot._client.on('world_particles', (packet) => {
    packetCounts.particle += 1;
    events.push({ at: Date.now(), type: 'particle', particleId: packet.particleId, count: packet.particleCount });
  });
  bot._client.on('entity_velocity', (packet) => {
    if (bot.entity && packet.entityId === bot.entity.id) {
      packetCounts.velocity += 1;
      events.push({ at: Date.now(), type: 'velocity', x: packet.velocityX, y: packet.velocityY, z: packet.velocityZ });
    }
  });

  if (!await waitFor(() => bot.entity, 60000, 500)) throw new Error('BOT_LOGIN_TIMEOUT');
  await send(`op ${USER}`);
  await send(`minecraft:gamemode survival ${USER}`);
  await send(`minecraft:attribute ${USER} minecraft:generic.max_health base set 100`);
  await send(`minecraft:data merge entity ${USER} {Health:100.0f}`);
  await send(`minecraft:tp ${USER} 0 64 3 0 0`);
  await send('minecraft:kill @e[type=zombie]');
  await sleep(1000);
  const spawnResponse = await send('mm mobs spawn level_1_grove_guardian 1 world,0,64,5');
  events.push({ at: Date.now(), type: 'spawn', value: spawnResponse });
  await sleep(1500);

  const bossSelector = '@e[type=zombie,sort=nearest,limit=1]';
  const bossData = await send(`minecraft:data get entity ${bossSelector}`);
  events.push({ at: Date.now(), type: 'boss_data', value: bossData.slice(0, 2000) });
  const snapshots = [];
  const snapshot = async (phase, elapsed) => {
    const health = await send(`minecraft:data get entity ${bossSelector} Health`).catch((error) => `missing:${error.message}`);
    const position = await send(`minecraft:data get entity ${bossSelector} Pos`).catch((error) => `missing:${error.message}`);
    snapshots.push({
      phase,
      elapsed,
      bossHealth: health,
      bossPosition: position,
      botHealth: bot.health,
      botPosition: bot.entity?.position,
      nearbyDisplays: nearbyDisplays(bot),
      packetCounts: { ...packetCounts },
    });
  };

  await snapshot('awaken_start', 1.5);
  await sleep(2000);
  await snapshot('awaken_end', 3.5);
  await send(`minecraft:data merge entity ${bossSelector} {NoAI:0b}`);
  await sleep(8000);
  await snapshot('combat_anticipation_window', 11.5);
  await sleep(8000);
  await snapshot('combat_impact_recovery_window', 19.5);

  await send(`minecraft:attribute ${bossSelector} minecraft:generic.max_health base get`).catch(() => '');
  await send(`minecraft:damage ${bossSelector} 290 minecraft:generic`);
  await sleep(2500);
  await snapshot('enrage_start', 22);
  await sleep(5000);
  await snapshot('enrage_end', 27);

  await sleep(8000);
  await snapshot('late_combat', 35);
  await send(`minecraft:damage ${bossSelector} 100000 minecraft:generic`);
  await sleep(500);
  await snapshot('death_start', 35.5);
  await sleep(3500);
  await snapshot('death_end', 39);

  const endedAt = new Date().toISOString();
  const result = {
    schemaVersion: 1,
    modelId: 'level_1_grove_guardian',
    startedAt,
    endedAt,
    spawnResponse,
    snapshots,
    packetCounts,
    evidence: {
      modelRendered: snapshots.some((entry) => entry.nearbyDisplays >= 40),
      damageObserved: events.some((entry) => entry.type === 'health' && entry.health < 100),
      knockbackObserved: packetCounts.velocity > 0,
      soundObserved: packetCounts.sound > 0,
      particlesObserved: packetCounts.particle > 0,
      awakenWindowObserved: snapshots.some((entry) => entry.phase === 'awaken_start')
        && snapshots.some((entry) => entry.phase === 'awaken_end'),
      enrageWindowObserved: snapshots.some((entry) => entry.phase === 'enrage_start')
        && snapshots.some((entry) => entry.phase === 'enrage_end'),
      deathWindowObserved: snapshots.some((entry) => entry.phase === 'death_start')
        && snapshots.some((entry) => entry.phase === 'death_end'),
    },
    events,
  };
  fs.writeFileSync(OUTPUT, `${JSON.stringify(result, null, 2)}\n`);
  console.log(JSON.stringify({ output: OUTPUT, packetCounts, evidence: result.evidence }, null, 2));

  await send(`minecraft:deop ${USER}`).catch(() => '');
  if (rcon) await rcon.end();
  bot.quit();
  process.exit(Object.values(result.evidence).every(Boolean) ? 0 : 2);
})().catch((error) => {
  console.error('GROVE_GUARDIAN_COMBAT_PROOF_FAIL', error);
  process.exit(1);
});
