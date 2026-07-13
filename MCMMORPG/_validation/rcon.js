// Maly helper: odpala komendy RCON podane jako argumenty (jedna komenda = jeden arg).
// Uzycie: node rcon.js "md list" "iris create x type=empty"
const { Rcon } = require('rcon-client');
const HOST = '127.0.0.1';
const PORT = 25575;
const PASS = process.env.RCON_PASS;

if (!PASS) {
  console.error('RCON_PASS must be set before running this helper.');
  process.exit(2);
}

(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: PORT, password: PASS });
  for (const cmd of process.argv.slice(2)) {
    const res = await rcon.send(cmd);
    console.log(`> ${cmd}\n${res}\n---`);
  }
  await rcon.end();
})().catch(e => { console.error('RCON ERR', e.message); process.exit(1); });
