// Zgloszenie 01.10: wojownik z pelnym paskiem trzyma topor z komponentami wloczni (AnimacjeBroni) -> LPM w scenie
// nie dochodzi. Strona serwera: przedmioty dochodza PO otwarciu sceny (jak przy wejsciu na serwer) -> w ciagu
// sekundy reka pusta, topor w plecaku, nic nie zginelo ani sie nie zdublowalo; zwykly przedmiot zostaje w rece.
// Komendy z przestrzeni minecraft: — CMI przechwytuje clear/item.
const w = require('./lib/w')
const R = new w.Results('pelny-pasek')
const QA = process.env.QA || 'WyborQA'
const TOPOR = 'minecraft:iron_axe[minecraft:piercing_weapon={},minecraft:attack_range={min_reach:2.0f,max_reach:4.5f},minecraft:minimum_attack_charge=1.0f,minecraft:custom_name="TOPOR_QA"]'
const d = q => w.rcon(`minecraft:data get entity ${QA} ${q}`)
const ile = async id => { const m = (await w.rcon(`minecraft:clear ${QA} ${id} 0`)).match(/Found (\d+)/); return m ? +m[1] : 0 }
const reka = () => d('SelectedItem.id')
const num = t => parseInt(t.replace(/.*data: /, ''))

async function graj(bot) {
  for (let i = 0; i < 3 && bot.game.gameMode === 'spectator'; i++) { await w.lpm(bot); await w.sleep(2500) }
  return bot.game.gameMode !== 'spectator'
}

/** Pelny pasek, w trzymanym slocie `trzymany`. */
async function pelnyPasek(trzymany) {
  const sel = num(await d('SelectedItemSlot'))
  for (let s = 0; s < 9; s++) await w.rcon(`minecraft:item replace entity ${QA} hotbar.${s} with ${s === sel ? trzymany : 'minecraft:paper'}`)
  return sel
}

;(async () => {
  const bot = await w.connect(QA)
  await w.sleep(7000)
  R.assert(await graj(bot), 'wejscie: LPM w scenie -> gra', bot.game.gameMode)
  const off = w.logOffset()
  await w.rcon(`cc wybor ${QA}`)
  await w.sleep(4000)
  R.assert(bot.game.gameMode === 'spectator', 'cc wybor -> scena', bot.game.gameMode)

  // 1) topor z komponentami wloczni dochodzi do reki w scenie
  const sel = await pelnyPasek(TOPOR)
  await w.sleep(1500)
  R.assert(/Found no elements/.test(await reka()), 'po 1,5 s: reka pusta', await reka())
  const slot = num(await d('Inventory[{components:{"minecraft:custom_name":"TOPOR_QA"}}].Slot'))
  R.assert(slot >= 9 && slot < 36, 'topor w plecaku (slot 9-35)', `${slot}`)
  R.assert(await ile('minecraft:iron_axe') === 1, 'dokladnie 1 topor (bez duplikatu)')
  R.assert(await ile('minecraft:paper') === 8, '8 papierow na pasku nietknietych')

  // 2) zwykly przedmiot przy pelnym pasku zostaje w rece (stale sloty nie moga sie dublowac)
  await w.rcon(`minecraft:clear ${QA} minecraft:iron_axe`)
  await w.rcon(`minecraft:item replace entity ${QA} hotbar.${sel} with minecraft:book`)
  await w.sleep(1500)
  R.assert(/book/.test(await reka()), 'zwykly przedmiot: zostaje w rece', await reka())
  R.assert(await ile('minecraft:book') >= 1, 'zwykly przedmiot: jest')
  R.assert(!/SEVERE|Exception/.test(w.logSince(off)), 'log bez bledow')

  await w.rcon(`minecraft:clear ${QA} minecraft:paper`)
  await w.rcon(`minecraft:clear ${QA} minecraft:book`)
  R.assert(await graj(bot), 'LPM w scenie -> gra', bot.game.gameMode)
  await w.quit(bot)
  R.print(); console.log(`WYNIK: ${R.rows.length - R.failed}/${R.rows.length} PASS`); process.exit(R.failed)
})().catch(e => { R.print(); console.error('ERR', e); process.exit(99) })
