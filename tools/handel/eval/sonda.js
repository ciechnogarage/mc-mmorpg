'use strict'
const L = require('/t/lib.js'); const { rconClient } = require('/t/rcon.js')
;(async () => {
  const rc = await rconClient()
  const nick = (process.env.EVAL_NICKS || 'HdS' + (Date.now() % 100000)).split(' ')[0]
  await rc.cmd(`deop ${nick}`)
  const bot = await L.wejdz(nick)
  await rc.cmd(`mi give MATERIAL PROF_OAK_TIMBER ${nick} 20`)
  await rc.cmd(`mi give MATERIAL PROF_MOONLEAF ${nick} 5`)
  await L.sleep(1000)
  const saldo = async () => L.strip(await rc.cmd(`papi parse ${nick} %coretools_eco_dollar%`))
  console.log('saldo start', await saldo())
  let w = await L.otworzHandel(bot); L.zrzut(w, 'Handel')
  await L.klik(bot, 10); w = await L.czekajOkno(bot, /Dom aukcyjny/); L.zrzut(w, 'AH')
  if (w) { await L.klik(bot, 49); w = await L.czekajOkno(bot, /Wystaw/); L.zrzut(w, 'Wystaw') }
  w = await L.otworzHandel(bot); await L.klik(bot, 12); w = await L.czekajOkno(bot, /Skup/); L.zrzut(w, 'Skup')
  w = await L.otworzHandel(bot); await L.klik(bot, 14); w = await L.czekajOkno(bot, /Sklep/); L.zrzut(w, 'Sklep')
  console.log('saldo koniec', await saldo())
  bot.quit(); rc.close(); process.exit(0)
})().catch(e => { console.log('BLAD', e); process.exit(1) })
