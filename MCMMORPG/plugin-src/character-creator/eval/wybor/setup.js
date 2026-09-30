/**
 * Przygotowanie konta testowego: 11 postaci zakladanych DROGA GRACZA (scena -> lista -> "+ Nowa postac"
 * -> kreator -> ksiega). Konsola klonu tylko do danych: poziom, ekwipunek, miejsce (to nie jest test,
 * to stan startowy dla scenariuszy wyboru/usuwania).
 */
const w = require('./lib/w')
const NAME = process.env.QA || 'WyborQA'
// [imie, przesuniecie rasy (D), przesuniecie klasy (S), poziom, ekwipunek, miejsce]
const CHARS = [
  ['Aldor', 0, 0, 12, 'diament', 'zatoka'],
  ['Brakk', 1, 0, 3, null, null],
  ['Elwen', 2, 1, 15, 'zelazo_luk', null],
  ['Morth', 3, 0, 1, null, null],
  ['Cedric', 0, 2, 4, 'skora', 'zatoka'],
  ['Grommash', 1, 1, 6, null, null],
  ['Lirael', 2, 0, 2, null, null],
  ['Vornas', 3, 1, 11, 'netheryt', null],
  ['Sarina', 0, 3, 1, null, null],
  ['Tharog', 1, 2, 2, null, null],
  ['Ilynde', 2, 3, 1, null, null],
]
const GEAR = {
  diament: ['armor.head diamond_helmet', 'armor.chest diamond_chestplate', 'armor.legs diamond_leggings', 'armor.feet diamond_boots', 'hotbar.0 diamond_sword'],
  zelazo_luk: ['armor.chest iron_chestplate', 'armor.legs iron_leggings', 'hotbar.0 bow', 'weapon.offhand shield'],
  skora: ['armor.chest leather_chestplate', 'hotbar.0 wooden_axe'],
  netheryt: ['armor.head netherite_helmet', 'armor.chest netherite_chestplate', 'hotbar.0 netherite_axe'],
}

async function waitMode(bot, pred, ms) {
  const end = Date.now() + ms
  while (Date.now() < end) { if (pred(bot.game.gameMode)) return true; await w.sleep(300) }
  return false
}

async function create(bot, [imie, r, c]) {
  for (let i = 0; i < r; i++) { await w.press(bot, 'right'); await w.sleep(900) }
  for (let i = 0; i < c; i++) await w.press(bot, 'backward')
  await w.sleep(500)
  await w.lpm(bot)
  const d = await w.waitDialog(bot, d => /Nowa postać/.test(w.plain(d.title)), 6000)
  if (!d) throw new Error('brak ksiegi dla ' + imie)
  await w.click(bot, /Stwórz postać/, { imie, kolor: ['czerwony', 'zielony', 'niebieski', 'zolty', 'fioletowy'][imie.length % 5] })
  // Po utworzeniu: przelot kamery, potem gra (survival/adventure).
  const ok = await waitMode(bot, m => m !== 'spectator', 40000)
  if (!ok) throw new Error('postac ' + imie + ' nie weszla do gry: ' + bot.log.slice(-5).join(' | '))
  await w.sleep(2500)
}

;(async () => {
  let bot = await w.connect(NAME)
  await w.sleep(5000)
  if (+(process.env.OD || 0) > 0 && bot.game.gameMode === 'spectator') {
    // Wchodzi do sceny wyboru — zeby petla (cc wybor) zadzialala tak samo, najpierw wybierz postac.
    await w.lpm(bot); await w.sleep(4000)
  }
  for (let i = +(process.env.OD || 0); i < +(process.env.DO || CHARS.length); i++) {
    const ch = CHARS[i]
    if (i > 0) {
      // "Zmien postac" z menu Q -> scena wyboru, potem lista i "+ Nowa postac".
      await w.rcon('cc wybor ' + NAME)
      await waitMode(bot, m => m === 'spectator', 15000)
      await w.sleep(4000)
      let d = null
      bot.dialog = null
      for (let t = 0; t < 4 && !d; t++) {
        await w.ppm(bot)
        d = await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 3000)
      }
      if (!d) throw new Error('brak listy przed ' + ch[0])
      if (!await w.click(bot, /Nowa postać/)) throw new Error('brak "+ Nowa postac": ' + w.buttons(bot).map(b => b.label).join('|'))
      await w.sleep(4000)
    }
    await create(bot, ch)
    const [imie, , , lvl, gear, miejsce] = ch
    if (lvl > 1) console.log(await w.rcon(`mmocore admin level set ${NAME} main ${lvl}`))
    for (const g of (GEAR[gear] || [])) { const [slot, item] = g.split(' '); await w.rcon(`minecraft:item replace entity ${NAME} ${slot} with minecraft:${item}`) }
    if (miejsce === 'zatoka') await w.rcon(`minecraft:spreadplayers 220 360 0 1 false ${NAME}`)
    await w.sleep(1500)
    console.log('OK', imie, 'gm', bot.game.gameMode, 'pos', bot.entity.position.floored())
  }
  await w.rcon('cc wybor ' + NAME)
  await w.sleep(4000)
  await w.quit(bot)
  process.exit(0)
})().catch(e => { console.error('ERR', e.message); process.exit(1) })
