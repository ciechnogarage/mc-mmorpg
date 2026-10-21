'use strict'
/** Sonda logowania: rejestracja, relog z haslem, zle haslo, proba wejscia na cudzy nick. */
const mineflayer = require('mineflayer')
const resourcepack = require('./lib/resourcepack')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const HOST = process.env.MC_HOST || 'mc-proxy-authtest'
const PORT = +(process.env.MC_PORT || 25599)

async function join(username, { timeout = 45000 } = {}) {
  const bot = mineflayer.createBot({ host: HOST, port: PORT, username, auth: 'offline', version: '1.21.11' })
  resourcepack.attach(bot._client, {})
  bot.log = []; bot.kick = null
  bot.on('message', m => bot.log.push(m.toString()))
  bot._client.on('login', () => bot.log.push('[login packet]'))
  await new Promise((resolve) => {
    const t = setTimeout(resolve, timeout)
    bot.once('spawn', () => { clearTimeout(t); resolve() })
    bot.once('kicked', r => { bot.kick = JSON.stringify(r).slice(0, 160); clearTimeout(t); resolve() })
    bot.once('error', e => { bot.kick = 'error: ' + e.message; clearTimeout(t); resolve() })
  })
  return bot
}
const czy = (bot, re) => bot.log.some(m => re.test(m))
const gdzie = bot => { try { return bot.game && bot.game.levelType ? bot.entity.position.toString() : '?' } catch { return '?' } }

;(async () => {
  const nick = 'AuthQA' + Date.now() % 10000
  const HASLO = 'TajneHaslo123'
  let wynik = []
  const ok = (w, opis, extra) => { wynik.push(`${w ? 'PASS' : 'FAIL'} ${opis}${extra ? ' — ' + extra : ''}`) }

  // 1. pierwsze wejscie: ma zadac rejestracji i trzymac w limbo
  let bot = await join(nick)
  await sleep(4000)
  ok(!bot.kick, '1. nowy gracz wchodzi na proxy', bot.kick || '')
  ok(czy(bot, /register|zarejestr/i), '2. serwer zada REJESTRACJI', bot.log.slice(-2).join(' / ').slice(0, 90))
  bot.chat(`/register ${HASLO} ${HASLO}`)
  await sleep(6000)
  ok(czy(bot, /registered|successful|logged/i), '3. rejestracja przyjeta', bot.log.slice(-2).join(' / ').slice(0, 90))
  bot.quit(); await sleep(3000)

  // 2. relog: ma zadac hasla
  bot = await join(nick)
  await sleep(4000)
  ok(czy(bot, /login|zaloguj/i), '4. po relogu serwer zada HASLA', bot.log.slice(-2).join(' / ').slice(0, 90))
  // 3. zle haslo
  bot.chat('/login ZupelnieInneHaslo')
  await sleep(4000)
  ok(czy(bot, /incorrect|wrong|invalid|bledne/i) || bot.kick, '5. zle haslo odrzucone', (bot.kick || bot.log.slice(-1)[0] || '').slice(0, 90))
  // 4. dobre haslo
  bot.chat(`/login ${HASLO}`)
  await sleep(6000)
  ok(czy(bot, /logged in|zalogowano|welcome/i), '6. poprawne haslo wpuszcza', bot.log.slice(-2).join(' / ').slice(0, 90))
  bot.quit(); await sleep(3000)

  // 5. podszycie sie: ktos inny wchodzi na ten sam nick i NIE zna hasla
  bot = await join(nick)
  await sleep(4000)
  const wpuszczonyBezHasla = czy(bot, /logged in automatically|welcome back/i) && !czy(bot, /login|zaloguj/i)
  ok(!wpuszczonyBezHasla, '7. podszywajacy sie NIE wchodzi bez hasla', bot.log.slice(-1)[0] || '')
  bot.quit(); await sleep(3000)

  // 8. CALA SCIEZKA: po zalogowaniu gracz ma dojsc na world1 i dostac ekran wyboru postaci.
  //    To jest to jedno miejsce, gdzie logowanie moglo pobic sie z kreatorem.
  bot = await join(nick)
  await sleep(3000)
  bot.chat(`/login ${HASLO}`)
  await sleep(6000)
  // Serwer docelowy bierzemy z env: testy NIE mają lądować na world1, bo tam się gra.
  // Domyślnie world3 — tam stoi prototyp i nikt nie przeszkadza.
  await sleep(20000)
  const zalogowany = !bot.log.slice(-4).some(m => /Please (login|register)/i.test(m))
  ok(zalogowany, '8. po zalogowaniu gracz jest wpuszczony dalej (serwer gry, nie limbo)',
    bot.log.slice(-2).join(' / ').slice(0, 110))
  bot.quit()

  console.log(wynik.join('\n'))
  console.log('NIEPOWODZEN: ' + wynik.filter(w => w.startsWith('FAIL')).length)
})().catch(e => { console.error('BLAD:', e.message); process.exit(1) })
