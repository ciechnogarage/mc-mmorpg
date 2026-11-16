const w = require('./lib/w')
;(async () => {
  const bot = await w.connect('WyborQA')
  const czat = []
  bot.on('message', m => czat.push(m.toString()))
  await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 12000)
  await w.click(bot, /^(★ )?Aldor /); await w.sleep(5000)
  bot.dialog = null; await w.rcon('cc wybor WyborQA lista'); await w.waitDialog(bot, d => /Twoje/.test(w.plain(d.title)), 5000)
  await w.click(bot, /^Brakk /); await w.sleep(5000)
  await w.rcon('cc wybor WyborQA'); await w.sleep(5000)
  await w.quit(bot)
  console.log([...new Set(czat)].join('\n'))
  process.exit(0)
})().catch(e => { console.error('ERR', e); process.exit(1) })
