const mineflayer = require('mineflayer'); const { Rcon } = require('./rcon'); const { execSync } = require('child_process');
const sleep = ms => new Promise(r => setTimeout(r, ms));
(async () => {
  const bot = mineflayer.createBot({ host: 'lobby-test', port: 25565, username: 'KolejkaQA', auth: 'offline', version: '1.21.11' });
  const pl = []; bot._client.on('custom_payload', p => pl.push(Buffer.from(p.data||[]).toString('latin1').replace(/[^\x20-\x7e]/g,'.')));
  const titles = []; bot._client.on('set_title_text', t => titles.push(JSON.stringify(t)));
  let chat = []; bot.on('messagestr', m => chat.push(m));
  await new Promise(r => bot.once('spawn', () => { bot._client.write('custom_payload', { channel: 'minecraft:register', data: Buffer.from('bungeecord:main') }); r(); }));
  await sleep(9000);
  const queued = chat.some(m => m.includes('przeniosę cię sam'));
  console.log((queued ? 'PASS' : 'FAIL') + ' auto-kolejka przy wejsciu (serwer lezy)');
  const r = await Rcon.connect('lobby-test', 25575, 'test');
  process.stdout.write('>> przywracam host\n'); await r.send('lobbyhostback'); // znacznik w logu
  require('fs').writeFileSync('/p/flag-host', '1');
  const t0 = Date.now();
  while (Date.now() - t0 < 25000 && !pl.some(s => s.includes('Connect'))) await sleep(500);
  const ok = pl.some(s => s.includes('Connect') && s.includes('world1'));
  console.log((ok ? 'PASS' : 'FAIL') + ' po powrocie serwera Connect world1 po ' + ((Date.now() - t0) / 1000).toFixed(1) + ' s');
  console.log((titles.some(t => t.includes('wstał')) ? 'PASS' : 'FAIL') + ' tytul "Serwer wstal!"');
  r.end(); bot.quit(); process.exit(0);
})();
