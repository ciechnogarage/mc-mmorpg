'use strict'
// Eval rzucania oburacz + nosnika. UWAGA: progi many (r1.mana > 4, proporcje 2,1-2,9) skalibrowane pod KOSZT DOCELOWY
// (2 / 2,5 na tik); przy koszcie testowym 0,5 ubytki to ulamki i regeneracja many rozjezdza proporcje — FAIL nie znaczy bledu.
// Konsola: przypisanie (jak menu), kukla, pomiary. Bot: klawisze gracza.
setTimeout(() => { console.error('BLAD: limit czasu'); process.exit(2) }, 240000)
const mineflayer = require('mineflayer'); const { Rcon } = require('rcon-client'); const rp = require('./resourcepack')
const HOST = process.env.MC_HOST || 'zaklecia-proba'; const sleep = ms => new Promise(r => setTimeout(r, ms))
const clean = s => String(s).replace(/§./g, '').replace(/\x1b\[[0-9;]*m/g, '').trim()
const wynik = []; const ok = (n, c, d) => wynik.push(`${c ? 'PASS' : 'FAIL'} ${n}  ${d || ''}`)
;(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: 25575, password: 'zaklecia' }); const cmd = async c => clean(await rcon.send(c))
  const bot = mineflayer.createBot({ host: HOST, port: 25565, username: 'MagTest', version: '1.21.11' }); rp.attach(bot._client)
  await new Promise(r => bot.once('spawn', r)); await sleep(2000)
  const num = s => +(String(s).match(/(-?\d+(?:\.\d+)?)f?\s*$/)?.[1] ?? NaN)
  const mana = async () => +(await cmd('papi parse MagTest %mmocore_mana%'))
  const hp = async () => num(await cmd('data get entity @e[type=zombie,limit=1] Health'))
  const typ = async slot => (await cmd(`data get entity MagTest ${slot}`)).match(/id: "minecraft:([a-z_]+)"/)?.[1] || 'pusto'
  const nosnik = async slot => /1b/.test(await cmd(`data get entity MagTest ${slot}.components."minecraft:custom_data".PublicBukkitValues."zaklecia:nosnik"`))
  const reset = async () => {
    await cmd('minecraft:kill @e[type=zombie]'); await cmd('minecraft:tp MagTest 0.5 -60 0.5 0 0')
    await cmd('mm mobs spawn ZAK_KUKLA 1 world,0.5,-60,4.5'); await cmd('mmocore admin resource-mana set MagTest 100')
    await sleep(900); const z = Object.values(bot.entities).find(e => e.name === 'zombie'); if (z) await bot.lookAt(z.position.offset(0, 1.2, 0), true)
  }
  const rzut = async (ms, lewa = false) => { const m0 = await mana(), h0 = await hp(); bot.activateItem(lewa); await sleep(ms); bot.deactivateItem(); await sleep(700); return { mana: m0 - await mana(), hp: h0 - await hp() } }
  await cmd('minecraft:clear MagTest'); await cmd('minecraft:gamemode survival MagTest')
  // --- przypisanie
  bot.setQuickBarSlot(0); await sleep(300)
  await cmd('zaklecia przypisz MagTest plomienie prawa'); await sleep(500)
  ok('przypisz prawa: nosnik w wybranym slocie', await typ('Inventory[{Slot:0b}]') === 'trial_key' && await nosnik('Inventory[{Slot:0b}]'), await typ('Inventory[{Slot:0b}]'))
  // --- 1: prawa
  await reset(); const r1 = await rzut(1100)
  ok('prawa: obrazenia i mana', r1.hp > 5 && r1.mana > 4, JSON.stringify(r1))
  // --- 2: podwojne (plomienie x2)
  await cmd('zaklecia przypisz MagTest plomienie lewa'); await sleep(400)
  ok('przypisz lewa: nosnik w drugiej rece', await typ('equipment.offhand') === 'trial_key', await typ('equipment.offhand'))
  await reset(); const r2 = await rzut(1100)
  ok('podwojne: ~2,2x obrazen', r2.hp >= 1.7 * r1.hp, `${r2.hp.toFixed(1)} vs ${r1.hp.toFixed(1)} (x${(r2.hp / r1.hp).toFixed(2)})`)
  ok('podwojne: ~2,5x many', r2.mana >= 2.1 * r1.mana && r2.mana <= 2.9 * r1.mana, `${r2.mana.toFixed(1)} vs ${r1.mana.toFixed(1)} (x${(r2.mana / r1.mana).toFixed(2)})`)
  // --- 3: dwa rozne (ogien prawa, mroz lewa)
  await cmd('zaklecia przypisz MagTest odmrozenie lewa'); await reset(); const r3 = await rzut(1100)
  const mroz = num(await cmd('data get entity @e[type=zombie,limit=1] TicksFrozen')); const ogien = await cmd('data get entity @e[type=zombie,limit=1] Fire')
  ok('dwa rozne: ogien i mroz naraz', mroz > 0 && /\d+s/.test(ogien) && !/ 0s/.test(ogien), `mroz=${mroz} ${ogien}`)
  ok('dwa rozne: mana = suma rak (~2,25x prawej)', r3.mana >= 1.9 * r1.mana && r3.mana <= 2.6 * r1.mana, `${r3.mana.toFixed(1)} (x${(r3.mana / r1.mana).toFixed(2)})`)
  // --- 4: miecz w prawej + zaklecie w lewej
  await cmd('minecraft:item replace entity MagTest hotbar.1 with minecraft:iron_sword'); bot.setQuickBarSlot(1); await sleep(400)
  await reset(); const r4 = await rzut(2100, true)
  const mroz4 = num(await cmd('data get entity @e[type=zombie,limit=1] TicksFrozen'))
  ok('miecz + lewa: rzuca lewa dlon', r4.hp > 5 && mroz4 > 0 && r4.mana > 3, JSON.stringify(r4) + ` mroz=${mroz4}`)
  // --- 5: puszczenie zatrzymuje mane
  bot.setQuickBarSlot(0); await sleep(300); await reset(); bot.activateItem(); await sleep(800); bot.deactivateItem(); await sleep(300)
  const m5 = await mana(); await sleep(1500); ok('puszczenie: mana nie ubywa', await mana() >= m5, `${m5} -> ${await mana()}`)
  // --- 6: brak many
  await reset(); await cmd('mmocore admin resource-mana set MagTest 3'); bot.activateItem(); await sleep(1500)
  const m6 = await mana(); await sleep(1000); const m6b = await mana(); bot.deactivateItem()
  ok('brak many: rzut gasnie, mana nie schodzi ponizej zera', m6 >= 0 && m6b >= m6, `${m6} -> ${m6b}`)
  // --- 7: blokady nosnika
  bot.setQuickBarSlot(0); await sleep(300)
  try { await bot.tossStack(bot.inventory.slots[36]) } catch {} ; await sleep(600)
  ok('wyrzucenie (Q): nosnik zostaje', await typ('Inventory[{Slot:0b}]') === 'trial_key', await typ('Inventory[{Slot:0b}]'))
  try { await bot.clickWindow(36, 0, 0); await bot.clickWindow(20, 0, 0) } catch {} ; await sleep(600)
  if (bot.inventory.selectedItem) {} ; await bot.closeWindow?.(bot.inventory)
  ok('przeniesienie w ekwipunku: nosnik zostaje w slocie 1', await typ('Inventory[{Slot:0b}]') === 'trial_key' && await typ('Inventory[{Slot:11b}]') === 'pusto', `slot0=${await typ('Inventory[{Slot:0b}]')} slot11=${await typ('Inventory[{Slot:11b}]')}`)
  // --- 8: przypisanie bez nadpisywania cudzych rzeczy
  await cmd('minecraft:clear MagTest'); for (let s = 0; s < 9; s++) await cmd(`minecraft:item replace entity MagTest hotbar.${s} with minecraft:stone`)
  const odp = await cmd('zaklecia przypisz MagTest plomienie prawa')
  const kamienie = (await cmd('minecraft:clear MagTest minecraft:stone 0')).match(/(\d+)/)?.[1]
  ok('pelny pasek: odmowa, nic nie znika', kamienie === '9' && !/trial_key/.test(await cmd('data get entity MagTest Inventory')), `kamienie=${kamienie}`)
  await cmd('minecraft:clear MagTest'); await cmd('minecraft:item replace entity MagTest weapon.offhand with minecraft:torch')
  await cmd('zaklecia przypisz MagTest odmrozenie lewa'); await sleep(300)
  const pochodnie = (await cmd('minecraft:clear MagTest minecraft:torch 0')).match(/(\d+)/)?.[1]
  ok('lewa zajeta pochodnia: pochodnia do plecaka, zaklecie w lewej', pochodnie === '1' && await typ('equipment.offhand') === 'trial_key', `pochodnie=${pochodnie}`)
  await cmd('zaklecia przypisz MagTest nic lewa'); await sleep(300)
  ok('oproznij lewa', await typ('equipment.offhand') === 'pusto', await typ('equipment.offhand'))
  console.log(wynik.join('\n')); bot.quit(); await rcon.end(); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
