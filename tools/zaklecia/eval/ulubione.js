'use strict'
// Eval ulubionych: Shift + kolko myszy przelacza zaklecie w dloni. Konsola: przypisanie/ulubione (jak Ksiega) i pomiar.
setTimeout(() => { console.error('BLAD: limit czasu'); process.exit(2) }, 120000)
const mineflayer = require('mineflayer'); const { Rcon } = require('rcon-client'); const rp = require('./resourcepack')
const HOST = process.env.MC_HOST || 'zaklecia-proba'; const sleep = ms => new Promise(r => setTimeout(r, ms))
const clean = s => String(s).replace(/§./g, '').replace(/\x1b\[[0-9;]*m/g, '').trim()
const wynik = []; const ok = (n, c, d) => wynik.push(`${c ? 'PASS' : 'FAIL'} ${n}  ${d || ''}`)
;(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: 25575, password: 'zaklecia' }); const cmd = async c => clean(await rcon.send(c))
  const bot = mineflayer.createBot({ host: HOST, port: 25565, username: 'MagTest', version: '1.21.11' }); rp.attach(bot._client)
  await new Promise(r => bot.once('spawn', r)); await sleep(2000)
  const reka = async h => cmd(`papi parse MagTest %zaklecia_${h}%`)
  const slot = async () => (await cmd('data get entity MagTest SelectedItemSlot')).match(/(\d+)$/)?.[1]
  const kolko = async (z, na) => { bot.setControlState('sneak', z); await sleep(300); bot.setQuickBarSlot(na); await sleep(500); bot.setControlState('sneak', false); await sleep(200) }
  await cmd('minecraft:clear MagTest'); bot.setQuickBarSlot(0); await sleep(300)
  // wyczysc ulubione: przelacz az oba beda ☆
  for (const id of ['plomienie', 'odmrozenie']) if ((await cmd(`papi parse MagTest %zaklecia_ulubione_${id}%`)) === '★') await cmd(`zaklecia ulubione MagTest ${id}`)
  await cmd('zaklecia przypisz MagTest plomienie prawa'); await sleep(400)
  // 1. zwykle kolko
  await kolko(false, 1); ok('bez Shift: zwykla zmiana slotu', await slot() === '1' && await reka('prawa') === 'pusta', `slot=${await slot()} prawa=${await reka('prawa')}`)
  bot.setQuickBarSlot(0); await sleep(400)
  // 2. bez ulubionych: po wszystkich
  await kolko(true, 1); ok('Shift+kolko bez ulubionych: nastepne zaklecie, slot bez zmian', await slot() === '0' && await reka('prawa') === 'Odmrożenie', `slot=${await slot()} prawa=${await reka('prawa')}`)
  // 3. ulubione: tylko plomienie -> z odmrozenia wraca na plomienie, potem komunikat bez zmiany
  await cmd('zaklecia ulubione MagTest plomienie'); ok('placeholder gwiazdki', (await cmd('papi parse MagTest %zaklecia_ulubione_plomienie%')) === '★')
  await kolko(true, 1); ok('jedno ulubione: przejscie na nie', await reka('prawa') === 'Płomienie', await reka('prawa'))
  await kolko(true, 1); ok('jedno ulubione, juz w dloni: bez zmian', await reka('prawa') === 'Płomienie' && await slot() === '0', await reka('prawa'))
  // 4. dwa ulubione: zawijanie w obie strony
  await cmd('zaklecia ulubione MagTest odmrozenie')
  await kolko(true, 1); const a = await reka('prawa'); await kolko(true, 1); const b = await reka('prawa'); await kolko(true, 8); const c = await reka('prawa')
  ok('dwa ulubione: w dol, w dol (zawija), w gore', a === 'Odmrożenie' && b === 'Płomienie' && c === 'Odmrożenie', `${a} -> ${b} -> ${c}`)
  ok('nosnik po przelaczeniu', /1b/.test(await cmd('data get entity MagTest Inventory[{Slot:0b}].components."minecraft:custom_data".PublicBukkitValues."zaklecia:nosnik"')))
  // 5. miecz w prawej, zaklecie w lewej -> Shift+kolko zmienia lewa
  await cmd('zaklecia przypisz MagTest nic prawa'); await cmd('minecraft:item replace entity MagTest hotbar.0 with minecraft:iron_sword'); await cmd('zaklecia przypisz MagTest plomienie lewa'); await sleep(400)
  await kolko(true, 1); ok('miecz + lewa: Shift+kolko zmienia lewa', await reka('lewa') === 'Odmrożenie' && (await cmd('data get entity MagTest SelectedItem.id')).includes('iron_sword'), `lewa=${await reka('lewa')}`)
  // 6. rzucanie po przelaczeniu dziala
  await cmd('minecraft:clear MagTest'); bot.setQuickBarSlot(0); await sleep(300); await cmd('zaklecia przypisz MagTest plomienie prawa'); await kolko(true, 1)
  await cmd('mmocore admin resource-mana set MagTest 100'); const m0 = +(await cmd('papi parse MagTest %mmocore_mana%'))
  bot.activateItem(); await sleep(1100); bot.deactivateItem(); await sleep(400)
  ok('rzut przelaczonym zakleciem', +(await cmd('papi parse MagTest %mmocore_mana%')) < m0, `${m0} -> ${await cmd('papi parse MagTest %mmocore_mana%')}`)
  console.log(wynik.join('\n')); bot.quit(); await rcon.end(); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
