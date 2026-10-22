'use strict'
const H = require('/e/lib/harness'), F = require('/e/lib/flow')
const { rconClient } = require('/t/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const NICK = process.env.EVAL_NICK
const strip = s => String(s).replace(/§./g, '').trim()
;(async () => {
  const rc = await rconClient()
  let bot = await H.connect(NICK, { timeout: 90000 })
  try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) } catch (e) { console.log('kreator', e.message) }
  await sleep(3000)
  for (const c of [`mistrzostwo info ${NICK}`, `papi parse ${NICK} %mmocore_profession_mining%`, `papi parse ${NICK} %coretools_eco_dollar%`,
    `core-economy set dollar 777 ${NICK}`, `papi parse ${NICK} %coretools_eco_dollar%`, `core-economy take dollar 7 ${NICK}`, `papi parse ${NICK} %coretools_eco_dollar%`,
    `papi parse ${NICK} %mistrz_opis_mining%|%mistrz_akcja_mining%|%mistrz_max_mining%|%mistrz_liczba%`,
    `mmocore admin level set ${NICK} mining 50`, `papi parse ${NICK} %mistrz_stan_mining%|%mistrz_akcja_mining%`,
    `mmocore admin exp give ${NICK} mining 500`, `papi parse ${NICK} %mmocore_profession_mining%/%mmocore_profession_experience_mining%`,
    `mmocore admin exp check ${NICK} mining`, `minecraft:gamemode survival ${NICK}`])
    console.log('>', c, '=>', strip(await rc.cmd(c)))
  bot.chat('/profesje'); const w = await H.waitForWindow(bot, { timeout: 6000, titleIncludes: 'Profesje' }); await sleep(1500)
  if (w) { const it = bot.currentWindow.slots[9]; console.log('kafelek 9:', H.itemLabel(it), JSON.stringify(it && it.customLore).slice(0, 900)) }
  bot.chat('/profesje') 
  console.log('czat:', bot.evalLog.filter(m => !/[❤♦⛨]/.test(m)).slice(-8).join(' | '))
  await H.quit(bot); await sleep(4000)
  bot = await H.connect(NICK, { timeout: 90000 })
  const pw = await F.waitProfileScreen(bot)
  console.log('ekran profili:', pw ? H.windowTitle(pw) : '-')
  if (pw) bot.currentWindow.slots.slice(0, 54).forEach((it, i) => { if (it) console.log('  slot', i, it.name, strip(H.itemLabel(it))) })
  await H.quit(bot); rc.close(); process.exit(0)
})().catch(e => { console.log('ERR', e.stack); process.exit(2) })
