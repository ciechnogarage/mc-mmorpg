'use strict'
// Eval szybkiego wyboru v2.3: kolo z "pusta" (A), Shift+Ctrl = lewa (B), pasek z calym kolem (C),
// klawisze 1-9 przy Shift zwyczajnie (D), Q: Shift = okno ulubionych, bez = hub (E).
setTimeout(() => { console.error('BLAD: limit czasu'); process.exit(2) }, 150000)
const mineflayer = require('mineflayer'); const { Rcon } = require('rcon-client'); const rp = require('./resourcepack')
const HOST = process.env.MC_HOST || 'zaklecia-proba'; const sleep = ms => new Promise(r => setTimeout(r, ms))
const clean = s => String(s).replace(/§./g, '').replace(/\x1b\[[0-9;]*m/g, '').trim()
const wynik = []; const ok = (n, c, d) => wynik.push(`${c ? 'PASS' : 'FAIL'} ${n}  ${d || ''}`)
;(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: 25575, password: 'zaklecia' }); const cmd = async c => clean(await rcon.send(c))
  const bot = mineflayer.createBot({ host: HOST, port: 25565, username: 'MagTest', version: '1.21.11' }); rp.attach(bot._client)
  await new Promise(r => bot.once('spawn', r)); await sleep(2500)
  let pasek = ''; bot._client.on('set_action_bar_text', p => { pasek = JSON.stringify(p.text) }); bot._client.on('system_chat', p => { if (p.isActionBar) pasek = JSON.stringify(p.content) })
  let okno = ''; bot.on('windowOpen', w => { okno = JSON.stringify(w.title) })
  const reka = async h => cmd(`papi parse MagTest %zaklecia_${h}%`)
  const slot = async () => (await cmd('data get entity MagTest SelectedItemSlot')).match(/(\d+)$/)?.[1]
  const kolko = async (na, { shift = true, ctrl = false } = {}) => {
    // mineflayer wysyla w player_input tylko shift (sprint jako entity_action) — prawdziwy klient wysyla stan wszystkich
    // klawiszy, wiec Shift+Ctrl skladamy recznie tak jak klient
    bot._client.write('player_input', { inputs: { shift, sprint: ctrl } }); await sleep(350)
    bot.setQuickBarSlot(na); await sleep(500); bot._client.write('player_input', { inputs: {} }); await sleep(250) }
  await cmd('minecraft:clear MagTest'); bot.setQuickBarSlot(2); await sleep(300)
  for (const id of ['plomienie', 'odmrozenie']) if ((await cmd(`papi parse MagTest %zaklecia_ulubione_${id}%`)) === '★') await cmd(`zaklecia ulubione MagTest ${id}`)
  await cmd('zaklecia ulubione MagTest plomienie'); await cmd('zaklecia ulubione MagTest odmrozenie')
  await cmd('zaklecia przypisz MagTest plomienie prawa'); await sleep(400)
  // A + C: plomienie -> odmrozenie -> pusta -> plomienie
  await kolko(3); const a1 = await reka('prawa'); const pasekA = pasek
  await kolko(3); const a2 = await reka('prawa'); const s2 = await slot()
  await kolko(3); const a3 = await reka('prawa')
  ok('A: kolo Plomienie -> Odmrozenie -> pusta -> Plomienie', a1 === 'Odmrożenie' && a2 === 'pusta' && a3 === 'Płomienie' && s2 === '2', `${a1} -> ${a2} (slot ${s2}) -> ${a3}`)
  ok('C: pasek pokazuje cale kolo z wyborem', /Płomienie/.test(pasekA) && /pusta/.test(pasekA) && /\[.*Odmrożenie/.test(pasekA), pasekA.slice(0, 160))
  await kolko(1); ok('A: kolko w gore z Plomieni -> pusta (w tyl)', await reka('prawa') === 'pusta', await reka('prawa'))
  // D: cyfra przy Shift = zwykla zmiana slotu
  await kolko(6); ok('D: klawisz 7 przy Shift zmienia slot', await slot() === '6', `slot=${await slot()}`)
  bot.setQuickBarSlot(2); await sleep(400)
  // B: Shift+Ctrl = lewa dlon, przy pustej prawej i zakleciu... prawa pusta (slot zaklec), lewa pusta
  await kolko(3, { ctrl: true }); const b1 = await reka('lewa'); const b1p = await reka('prawa')
  await kolko(3, { ctrl: true }); const b2 = await reka('lewa')
  ok('B: Shift+Ctrl+kolko zmienia lewa, prawa bez zmian', b1 === 'Płomienie' && b2 === 'Odmrożenie' && b1p === 'pusta', `lewa ${b1} -> ${b2}, prawa ${b1p}`)
  // B przy zakleciu w prawej: Shift+Ctrl dalej lewa
  await kolko(3); const bp = await reka('prawa'); await kolko(3, { ctrl: true }); const bl = await reka('lewa')
  ok('B: z zakleciem w prawej Shift+Ctrl zmienia lewa', bp === 'Płomienie' && bl === 'pusta' && await reka('prawa') === 'Płomienie', `prawa=${await reka('prawa')} lewa=${bl}`)
  // B: lewa zajeta pochodnia -> komunikat, nic nie znika
  await cmd('minecraft:item replace entity MagTest weapon.offhand with minecraft:torch'); await kolko(3, { ctrl: true })
  ok('B: lewa zajeta — pochodnia zostaje', (await cmd('data get entity MagTest equipment.offhand.id')).includes('torch'))
  // E: zaklecia q (tak wola MenuKey)
  bot.setControlState('sneak', true); await sleep(400); okno = ''; await cmd('zaklecia q MagTest'); await sleep(1200); const e1 = okno; bot.setControlState('sneak', false)
  if (bot.currentWindow) bot.closeWindow(bot.currentWindow); await sleep(600)
  okno = ''; await cmd('zaklecia q MagTest'); await sleep(1200); const e2 = okno; if (bot.currentWindow) bot.closeWindow(bot.currentWindow)
  ok('E: Shift+Q = okno ulubionych', /Ulubione/.test(e1), e1.slice(0, 120)); ok('E: samo Q = hub', /Menu|hub/i.test(e2) && !/Ulubione/.test(e2), e2.slice(0, 120))
  console.log(wynik.join('\n')); bot.quit(); await rcon.end(); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
