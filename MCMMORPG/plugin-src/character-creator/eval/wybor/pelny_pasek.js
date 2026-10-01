// Zgloszenie 01.10 "LPM w scenie nie dziala": reka w scenie ma byc PUSTA, a zasieg istot >= 3, cokolwiek gracz
// ma w ekwipunku. Przedmioty dochodza PO otwarciu sceny (jak przy wejsciu na serwer), sprawdzamy po 1,5 s.
// Przypadki: zwykla bron, bron z komponentami wloczni, Dziennik (slot 8) trzymany, nosnik zaklecia trzymany,
// zaklecie w lewej (Zaklecia: zasieg 0), wolny tylko slot Sakwy, pelny ekwipunek. Zero duplikatow i strat.
// Komendy z przestrzeni minecraft: — CMI przechwytuje clear/item.
const w = require('./lib/w')
const R = new w.Results('pelny-pasek')
const QA = process.env.QA || 'WyborQA'
const TOPOR = 'minecraft:iron_axe[minecraft:piercing_weapon={},minecraft:attack_range={min_reach:2.0f,max_reach:4.5f},minecraft:minimum_attack_charge=1.0f]'
const NOSNIK = 'minecraft:trial_key[minecraft:custom_data={PublicBukkitValues:{"zaklecia:nosnik":1b}}]'
const d = q => w.rcon(`minecraft:data get entity ${QA} ${q}`)
const ile = async id => { const m = (await w.rcon(`minecraft:clear ${QA} ${id} 0`)).match(/Found (\d+)/); return m ? +m[1] : 0 }
const num = t => parseFloat(t.replace(/.*data: /, ''))
const reka = () => d('SelectedItem.id')
const pusta = async () => /Found no elements/.test(await reka())
const slotId = async s => { const t = await d(`Inventory[{Slot:${s}b}].id`); return /Found no/.test(t) ? null : t.replace(/.*data: /, '').replace(/"/g, '') }
const zasieg = async () => parseFloat((await w.rcon(`minecraft:attribute ${QA} minecraft:entity_interaction_range get`)).match(/is ([\d.]+)/)?.[1])
const rep = (slot, item) => w.rcon(`minecraft:item replace entity ${QA} ${slot} with ${item}`)
const wyczysc = async () => { for (let s = 0; s < 36; s++) await rep(`container.${s}`, 'minecraft:air'); await rep('weapon.offhand', 'minecraft:air') }
const rcount = async () => { let n = 0; for (let s = 0; s < 36; s++) if (await slotId(s)) n++; return n }

async function graj(bot) {
  for (let i = 0; i < 3 && bot.game.gameMode === 'spectator'; i++) { await w.lpm(bot); await w.sleep(2500) }
  return bot.game.gameMode !== 'spectator'
}

/** Pelny pasek papieru, w slocie `sel` przedmiot `item`, wybrany `sel`. */
async function pasek(bot, sel, item) {
  for (let s = 0; s < 9; s++) await rep(`hotbar.${s}`, s === sel ? item : 'minecraft:paper')
  bot.setQuickBarSlot(sel); await w.sleep(1700)
}

;(async () => {
  const bot = await w.connect(QA)
  await w.sleep(7000)
  R.assert(await graj(bot), 'wejscie: LPM w scenie -> gra', bot.game.gameMode)

  // Dziura przed scena: zaklecie w lewej + pusta prawa = zasieg 0 (Zaklecia, tryb LPM)
  await wyczysc()
  await w.rcon(`mm items give ${QA} ZAKLECIE_PLOMIENIE`)
  await w.sleep(300)
  await w.rcon(`minecraft:item replace entity ${QA} weapon.offhand from entity ${QA} container.0`)
  await rep('container.0', 'minecraft:air')
  bot.setQuickBarSlot(0); await w.sleep(1500)
  const z0 = await zasieg()
  R.assert(z0 < 1, 'w grze: zaklecie w lewej + pusta prawa -> zasieg istot 0 (dziura istnieje)', `${z0}`)

  const off = w.logOffset()
  await w.rcon(`cc wybor ${QA}`)
  await w.sleep(4000)
  R.assert(bot.game.gameMode === 'spectator', 'cc wybor -> scena', bot.game.gameMode)

  // 1) zaklecie w lewej dochodzi w scenie
  await wyczysc()
  await w.rcon(`mm items give ${QA} ZAKLECIE_PLOMIENIE`); await w.sleep(300)
  await w.rcon(`minecraft:item replace entity ${QA} weapon.offhand from entity ${QA} container.0`)
  await rep('container.0', 'minecraft:air'); await w.sleep(1700)
  R.assert(await zasieg() >= 3, 'scena: zaklecie w lewej -> zasieg istot >= 3', `${await zasieg()}`)
  await rep('weapon.offhand', 'minecraft:air')

  // 2) zwykla bron (bez komponentow AnimacjeBroni) przy pelnym pasku
  await wyczysc(); await pasek(bot, 0, 'minecraft:iron_sword')
  R.assert(await pusta(), 'zwykly miecz: reka pusta', await reka())
  R.assert(await ile('minecraft:iron_sword') === 1, 'zwykly miecz: 1 sztuka, w plecaku')

  // 3) bron z komponentami wloczni
  await wyczysc(); await pasek(bot, 3, TOPOR)
  R.assert(await pusta(), 'topor-wlocznia: reka pusta', await reka())
  R.assert(await ile('minecraft:iron_axe') === 1, 'topor-wlocznia: 1 sztuka')

  // 4) trzymany slot 8 (Dziennik) — zostaje na miejscu, wychodzi inny przedmiot z paska
  await wyczysc(); await pasek(bot, 8, 'minecraft:book')
  R.assert(await pusta(), 'Dziennik trzymany: reka pusta', await reka())
  R.assert(await slotId(8) === 'minecraft:book', 'Dziennik zostaje w slocie 8', `${await slotId(8)}`)
  R.assert(await ile('minecraft:book') === 1 && await ile('minecraft:paper') === 8, 'Dziennik: bez duplikatow i strat')

  // 5) trzymany nosnik zaklecia — zostaje, wychodzi inny
  await wyczysc(); await pasek(bot, 2, NOSNIK)
  R.assert(await pusta(), 'nosnik trzymany: reka pusta', await reka())
  R.assert(await slotId(2) === 'minecraft:trial_key', 'nosnik zostaje w slocie 2', `${await slotId(2)}`)

  // 6) wolny tylko slot Sakwy (18) — nie wolno go zajac
  await wyczysc()
  for (let s = 9; s < 36; s++) if (s !== 18) await rep(`container.${s}`, 'minecraft:stone')
  const o6 = w.logOffset()
  await pasek(bot, 0, 'minecraft:iron_sword')
  R.assert(await slotId(18) === null, 'slot Sakwy 18 zostaje pusty', `${await slotId(18)}`)
  R.assert(/nie da sie jej oproznic/.test(w.logSince(o6)), 'brak miejsca -> WARN w logu')
  R.assert(await zasieg() >= 3, 'brak miejsca: zasieg dalej >= 3', `${await zasieg()}`)

  // 7) calkiem pelny ekwipunek — nic nie ginie
  await rep('container.18', 'minecraft:stone'); await w.sleep(1700)
  R.assert(await rcount() === 36 && await ile('minecraft:iron_sword') === 1, 'pelny ekwipunek: 36 slotow, nic nie zginelo')

  R.assert(!/SEVERE|Exception/.test(w.logSince(off)), 'log bez bledow')
  await wyczysc()
  R.assert(await graj(bot), 'LPM w scenie -> gra', bot.game.gameMode)
  const mod = await w.rcon(`minecraft:attribute ${QA} minecraft:entity_interaction_range modifier value get charactercreator:scena_zasieg`)
  R.assert(!/Value of modifier/.test(mod), 'po wyjsciu ze sceny: modyfikator zasiegu zdjety', mod)
  await w.quit(bot)
  R.print(); console.log(`WYNIK: ${R.rows.length - R.failed}/${R.rows.length} PASS`); process.exit(R.failed)
})().catch(e => { R.print(); console.error('ERR', e); process.exit(99) })
