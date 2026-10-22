'use strict'
// Eval 03.10: pasek Shift+Q przy PUSTEJ prawej dloni — PPM ma wlozyc zaklecie do LEWEJ (wczesniej prawdziwy klient z pusta
// reka nie wysylal pakietu przy PPM w powietrze, wiec pasek nic nie dostawal). Pasek wklada nosnik "pusta" do dloni.
setTimeout(() => { console.error('BLAD: limit czasu'); process.exit(2) }, 90000)
const mineflayer = require('mineflayer'); const { Rcon } = require('rcon-client'); const rp = require('./resourcepack')
const HOST = process.env.MC_HOST || 'zaklecia-proba'; const sleep = ms => new Promise(r => setTimeout(r, ms))
const clean = s => String(s).replace(/§./g, '').replace(/\x1b\[[0-9;]*m/g, '').trim()
const wynik = []; const ok = (n, c, d) => wynik.push(`${c ? 'PASS' : 'FAIL'} ${n}  ${d || ''}`)
;(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: 25575, password: 'zaklecia' }); const cmd = async c => clean(await rcon.send(c))
  const bot = mineflayer.createBot({ host: HOST, port: 25565, username: 'MagTest', version: '1.21.11' }); rp.attach(bot._client)
  await new Promise(r => bot.once('spawn', r)); await sleep(2500)
  const reka = async h => cmd(`papi parse MagTest %zaklecia_${h}%`)
  const wDloni = async () => cmd('data get entity MagTest SelectedItem')
  const pasekQ = async () => { bot._client.write('player_input', { inputs: { shift: true } }); await sleep(300); await cmd('zaklecia q MagTest'); await sleep(300); bot._client.write('player_input', { inputs: {} }); await sleep(500) }
  const ppm = async () => { bot.activateItem(); await sleep(150); bot.deactivateItem(); await sleep(700) }
  await cmd('zaklecia przypisz MagTest nic prawa'); await cmd('zaklecia przypisz MagTest nic lewa')
  await cmd('minecraft:clear MagTest'); bot.setQuickBarSlot(2); await sleep(800)
  ok('start: obie dlonie puste, slot pusty', await reka('prawa') === 'pusta' && await reka('lewa') === 'pusta' && /no elements|found no|has no/i.test(await wDloni()), await wDloni())
  await pasekQ()
  const d1 = await wDloni()
  ok('pasek otwarty: w pustej dloni nosnik (klient wysle PPM)', /zaklecia/.test(d1) && !/^.*no elements/i.test(d1), d1.slice(0, 140))
  // pierwsza pozycja kola = pierwsze zaklecie; PPM -> lewa
  await ppm()
  const l = await reka('lewa'), p = await reka('prawa')
  ok('PPM przy pustej prawej: zaklecie w LEWEJ, prawa pusta', l !== 'pusta' && p === 'pusta', `lewa=${l} prawa=${p}`)
  await pasekQ(); await sleep(300)   // zamkniecie
  ok('po zamknieciu: lewa zostaje', await reka('lewa') === l, await reka('lewa'))
  // zdejmij lewa paskiem (pozycja "pusta" = ostatnia: kolko w tyl), zamknij -> slot pusty
  await pasekQ(); bot.setQuickBarSlot(1); await sleep(600); await ppm()
  ok('PPM na "pusta" zdejmuje lewa', await reka('lewa') === 'pusta', await reka('lewa'))
  await pasekQ(); await sleep(400)
  const d2 = await wDloni()
  ok('pasek zamkniety, lewa pusta: nosnik znika z dloni', /no elements|found no|has no/i.test(d2) || !/zaklecia/.test(d2), d2.slice(0, 140))
  // prawa dalej dziala LPM
  await pasekQ(); bot.swingArm('right'); await sleep(900)
  ok('LPM przy otwartym pasku -> prawa', await reka('prawa') !== 'pusta', `prawa=${await reka('prawa')}`)
  await pasekQ()
  await cmd('zaklecia przypisz MagTest nic prawa'); await cmd('zaklecia przypisz MagTest nic lewa')
  console.log(wynik.join('\n')); bot.quit(); await rcon.end(); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
