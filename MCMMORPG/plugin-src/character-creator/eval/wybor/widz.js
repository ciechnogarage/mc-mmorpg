// Zgloszenie 01.10 wieczor "znowu w kreatorze nie dziala LPM": Zaklecia v2.6+ wkladaja do wybranego slotu "pusta dlon"
// (DEBUG_STICK z attack_range min 64), gdy postac ma zaklecie w lewej. Klient z takim przedmiotem nie uderza niczego
// blizej niz 64 bl. — tarcza sceny (1,4 bl.) jest nieosiagalna, na serwer nie dochodzi nic. Bot tego nie odczuje
// (mineflayer nie liczy attack_range), wiec mierzymy to, co widzi KLIENT: reka bota probkowana co 50 ms przez 4 s.
// Wymagane: w scenie reka klienta pusta w KAZDEJ probce, zasieg istot >= 3; po wyjsciu ze sceny Zaklecia dzialaja dalej.
const w = require('./lib/w')
const R = new w.Results('widz')
const QA = process.env.QA || 'WyborQA'
const rep = (slot, item) => w.rcon(`minecraft:item replace entity ${QA} ${slot} with ${item}`)
const wyczysc = async () => { for (let s = 0; s < 36; s++) await rep(`container.${s}`, 'minecraft:air'); await rep('weapon.offhand', 'minecraft:air') }
const zasieg = async () => parseFloat((await w.rcon(`minecraft:attribute ${QA} minecraft:entity_interaction_range get`)).match(/is ([\d.]+)/)?.[1])

async function graj(bot) {
  for (let i = 0; i < 3 && bot.game.gameMode === 'spectator'; i++) { await w.lpm(bot); await w.sleep(2500) }
  return bot.game.gameMode !== 'spectator'
}

/** Zaklecie w lewej, wybrany slot 0 pusty — dokladnie stan postaci maga po v2.6. */
async function zaklecieWLewej(bot) {
  await wyczysc()
  await w.rcon(`mm items give ${QA} ZAKLECIE_PLOMIENIE`); await w.sleep(300)
  await w.rcon(`minecraft:item replace entity ${QA} weapon.offhand from entity ${QA} container.0`)
  await rep('container.0', 'minecraft:air')
  bot.setQuickBarSlot(0)
}

/** Probki reki klienta: ile razy cos bylo w rece i co. */
async function probki(bot, ms) {
  const co = {}; let n = 0, zajeta = 0
  for (const end = Date.now() + ms; Date.now() < end; n++) {
    const it = bot.heldItem
    if (it) { zajeta++; co[it.name] = (co[it.name] || 0) + 1 }
    await w.sleep(50)
  }
  return { n, zajeta, co: JSON.stringify(co) }
}

;(async () => {
  const bot = await w.connect(QA)
  await w.sleep(7000)
  R.assert(await graj(bot), 'wejscie: LPM w scenie -> gra', bot.game.gameMode)
  await w.sleep(4000)   // MMOProfiles wczytuje ekwipunek postaci po "Graj" — inaczej nadpisuje nasze zaklecie

  await zaklecieWLewej(bot); await w.sleep(1500)
  R.assert(bot.heldItem?.name === 'debug_stick', 'w grze: zaklecie w lewej -> Zaklecia daja pusta dlon (warunek bledu)', bot.heldItem?.name)

  const off = w.logOffset()
  await w.rcon(`cc wybor ${QA}`)
  await w.sleep(4000)
  R.assert(bot.game.gameMode === 'spectator', 'cc wybor -> scena', bot.game.gameMode)
  let p = await probki(bot, 4000)
  R.assert(p.zajeta === 0, `scena (wybor): reka klienta pusta w kazdej probce`, `${p.zajeta}/${p.n} zajete ${p.co}`)
  R.assert(await zasieg() >= 3, 'scena (wybor): zasieg istot >= 3', `${await zasieg()}`)

  // zaklecie dochodzi juz w scenie (jak ekwipunek przy wejsciu na serwer)
  await zaklecieWLewej(bot); await w.sleep(500)
  p = await probki(bot, 4000)
  R.assert(p.zajeta === 0, 'scena: zaklecie w lewej dochodzi w scenie -> reka dalej pusta', `${p.zajeta}/${p.n} zajete ${p.co}`)

  // pelny pasek + zaklecie w lewej: zostaje przelaczenie slotu i przeniesienie do plecaka
  for (let s = 0; s < 8; s++) await rep(`hotbar.${s}`, 'minecraft:paper')
  await w.sleep(500)
  p = await probki(bot, 3000)
  R.assert(p.zajeta === 0, 'scena: pelny pasek papieru + zaklecie w lewej -> reka pusta', `${p.zajeta}/${p.n} zajete ${p.co}`)
  for (let s = 0; s < 8; s++) await rep(`hotbar.${s}`, 'minecraft:air')

  R.assert(!/SEVERE|Exception/.test(w.logSince(off)), 'log bez bledow')
  R.assert(await graj(bot), 'LPM w scenie -> gra', bot.game.gameMode)
  const lewa = async () => !/Found no/.test(await w.rcon(`minecraft:data get entity ${QA} equipment.offhand.id`))
  for (let i = 0; i < 4; i++) {   // wczytanie ekwipunku postaci po "Graj" przychodzi z opoznieniem i nadpisuje lewa
    await w.sleep(3000); await zaklecieWLewej(bot); await w.sleep(1500)
    if (await lewa() && bot.heldItem) break
  }
  const stan = async () => `reka=${bot.heldItem?.name} slot=${bot.quickBarSlot} tryb=${bot.game.gameMode} lewa=${(await w.rcon(`minecraft:data get entity ${QA} equipment.offhand.id`)).replace(/.*data: /, '')}`
  R.assert(bot.heldItem?.name === 'debug_stick', 'po wyjsciu ze sceny Zaklecia dzialaja (pusta dlon wraca)', await stan())
  await wyczysc()
  await w.quit(bot)
  R.print(); console.log(`WYNIK: ${R.rows.length - R.failed}/${R.rows.length} PASS`); process.exit(R.failed)
})().catch(e => { R.print(); console.error('ERR', e); process.exit(99) })
