'use strict';

// Prosty klient protokołu do pomiaru pakietów okna. Nie ładuje rejestrów
// Mineflayer, które w 1.21.11 mogą zatrzymać wejście do stanu gry.
const mc = require('minecraft-protocol');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const client = mc.createClient({
  host: 'ui-klon', port: 25565, username: 'KsieQA', version: '1.21.11', auth: 'offline'
});

const deadline = setTimeout(() => {
  console.error('TIMEOUT_70S', 'state=' + client.state);
  process.exit(2);
}, 70000);
const heartbeat = setInterval(() => console.log('STATE', client.state), 10000);

client.on('add_resource_pack', packet => {
  console.log('ADD_RESOURCE_PACK', client.state, packet.uuid);
  client.write('resource_pack_receive', { uuid: packet.uuid, result: 3 });
  client.write('resource_pack_receive', { uuid: packet.uuid, result: 0 });
});
client.on('packet', (packet, meta) => {
  if (client.state !== 'play' &&
      /login|configuration|known_packs|resource_pack|finish|registry_data|keep_alive/.test(meta.name))
    console.log('PACKET', client.state, meta.name);
});
client.on('error', error => { console.error('CLIENT_ERROR', error.stack || error); process.exit(1); });
client.on('end', reason => { console.error('CLIENT_END', reason); process.exit(1); });
client.on('kick_disconnect', packet => {
  console.error('KICKED', JSON.stringify(packet.reason).slice(0, 600));
  process.exit(1);
});

let windowId = null;
let pendingOpen = null;
let sample = null;
client.on('open_window', packet => {
  windowId = packet.windowId;
  console.log('OPEN_WINDOW', windowId);
  if (pendingOpen) { pendingOpen(windowId); pendingOpen = null; }
});
client.on('set_slot', packet => {
  if (sample && packet.windowId === sample.id) sample.setSlot++;
});
client.on('window_items', packet => {
  if (sample && packet.windowId === sample.id) sample.windowItems++;
});

async function measure(menu) {
  windowId = null;
  const opened = new Promise((resolve, reject) => {
    pendingOpen = resolve;
    setTimeout(() => { if (pendingOpen === resolve) {
      pendingOpen = null;
      reject(new Error('Brak okna ' + menu));
    } }, 7000);
  });
  client.chat('/dm open ' + menu);
  const id = await opened;
  await sleep(1000);
  sample = { id, setSlot: 0, windowItems: 0 };
  await sleep(10000);
  console.log('PAKIETY', menu, JSON.stringify(sample));
  sample = null;
}

client.once('playerJoin', async () => {
  try {
    console.log('PLAYER_JOIN', client.state);
    await sleep(1500);
    await measure('ksiega_zaklec');
    await measure('mapa_swiata');
    await measure('chowance_menazeria');
    clearTimeout(deadline);
    clearInterval(heartbeat);
    client.removeAllListeners('end');
    client.end();
    process.exit(0);
  } catch (error) {
    console.error('POMIAR_ERROR', error.stack || error);
    process.exit(1);
  }
});
