'use strict';
const mineflayer = require('mineflayer');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

const deadline = setTimeout(() => {
  console.error('TIMEOUT_70S', 'state=', bot._client.state);
  process.exit(2);
}, 70000);
const bot = mineflayer.createBot({
  host: 'ui-klon', port: 25565, username: 'KsieQA', version: '1.21.11', auth: 'offline'
});

bot.on('resourcePack', () => {
  // Mineflayer odpowiada sam podczas konfiguracji, ale nie po dołączeniu do gry.
  console.log('RESOURCE_PACK', bot._client.state);
  if (bot._client.state !== 'configuration') bot.acceptResourcePack();
});
bot._client.on('add_resource_pack', packet => {
  console.log('ADD_RESOURCE_PACK', bot._client.state,
    'uuid=' + packet.uuid, 'url=' + Boolean(packet.url));
  bot._client.write('resource_pack_receive', { uuid: packet.uuid, result: 3 });
  bot._client.write('resource_pack_receive', { uuid: packet.uuid, result: 0 });
});
bot._client.on('resource_pack_send', packet => console.log('RESOURCE_PACK_SEND',
  bot._client.state, Boolean(packet.uuid), Boolean(packet.url)));
bot._client.on('packet', (packet, meta) => {
  if (bot._client.state !== 'play' &&
      /login|configuration|known_packs|resource_pack|finish|registry_data|custom_payload|keep_alive/.test(meta.name))
    console.log('PACKET', bot._client.state, meta.name);
});
bot.on('login', () => console.log('BOT_LOGIN', bot._client.state));
const heartbeat = setInterval(() => console.log('STATE', bot._client.state), 10000);
bot.on('error', error => { console.error('BOT_ERROR', error.stack || error); process.exit(1); });
bot.on('kicked', reason => { console.error('KICKED', JSON.stringify(reason)); process.exit(1); });
bot.on('end', reason => { console.error('END', reason); process.exit(1); });

let sample = null;
bot._client.on('set_slot', packet => {
  if (sample && packet.windowId === sample.id) sample.setSlot++;
});
bot._client.on('window_items', packet => {
  if (sample && packet.windowId === sample.id) sample.windowItems++;
});

async function open(menu) {
  if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await sleep(250); }
  const before = bot.currentWindow;
  bot.chat('/dm open ' + menu);
  for (let i = 0; i < 50; i++) {
    if (bot.currentWindow && bot.currentWindow !== before) return bot.currentWindow;
    await sleep(100);
  }
  throw new Error('Brak okna ' + menu);
}

async function measure(menu) {
  const window = await open(menu);
  await sleep(1000); // Pomijamy początkowe wysłanie zawartości okna.
  sample = { id: window.id, setSlot: 0, windowItems: 0 };
  await sleep(10000);
  console.log('PAKIETY', menu, JSON.stringify(sample), 'TYTUL',
    JSON.stringify(window.title).slice(0, 120));
  sample = null;
}

(async () => {
  await Promise.race([
    new Promise((resolve, reject) => { bot.once('spawn', resolve); bot.once('error', reject); }),
    sleep(42000).then(() => { throw new Error('Brak spawnu przez 42 s; state=' + bot._client.state); })
  ]);
  console.log('SPAWN', bot.entity.position.toString());
  await sleep(1000);
  await measure('ksiega_zaklec');
  await measure('mapa_swiata');
  await measure('chowance_menazeria');
  clearTimeout(deadline);
  clearInterval(heartbeat);
  bot.removeAllListeners('end');
  bot.quit();
  process.exit(0);
})().catch(error => { console.error('POMIAR_ERROR', error.stack || error); process.exit(1); });
