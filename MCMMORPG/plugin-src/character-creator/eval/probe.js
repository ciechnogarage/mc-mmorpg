const mineflayer = require('mineflayer')
const bot = mineflayer.createBot({
  host: process.env.MC_HOST || 'mc-proxy-1',
  port: +(process.env.MC_PORT || 25577),
  username: process.env.MC_USER || 'EvalProbe',
  auth: 'offline',
  version: process.env.MC_VERSION || false,
})
const done = (code, msg) => { console.log(msg); try { bot.quit() } catch {} process.exit(code) }
setTimeout(() => done(1, 'TIMEOUT: brak spawn w 45 s'), 45000)
bot.once('spawn', () => {
  console.log('SPAWN ok | wersja=' + bot.version + ' | pozycja=' + bot.entity.position
    + ' | tryb=' + bot.game.gameMode + ' | swiat=' + bot.game.dimension)
  setTimeout(() => done(0, 'OK'), 3000)
})
bot.on('kicked', r => done(1, 'KICK: ' + JSON.stringify(r).slice(0, 300)))
bot.on('error', e => done(1, 'ERROR: ' + e.message))
