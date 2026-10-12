'use strict'
/**
 * Eval Wiar na KLONIE world1 — ścieżka gracza bez OP, przez menu (Q -> Postać -> Wiara), bez komend na czacie.
 * Konsola (RCON) tylko zakłada postać testową (/cc testpostac, zamknięte trzema zamkami) i OBSERWUJE.
 *
 *  1. ork: hub pod Q -> Postać ma kafelek Wiara -> menu Wiary pokazuje 3 Starszych + 3 bóstwa orków, nie cudze,
 *  2. opisy łaski mają policzone liczby (żadnego surowego %wiary_...%), modlitwa zablokowana bez patrona,
 *  3. klik w Gorratha = patron; klik Módl się = łaska: max_health rośnie, komunikat na czacie,
 *  4. druga modlitwa w trakcie łaski i zmiana patrona w ciągu 24 h są odmawiane,
 *  5. bóstwo obcej rasy odrzucone także z konsoli; gracz nie wywoła /wiary z czatu,
 *  6. relog: w scenie wyboru łaska zdjęta; po wyborze orka (klik w manekina) patron i łaska wracają,
 *  7. DRUGA postać tego samego konta (elf, zakładana ze sceny) nie ma patrona orka i widzi bóstwa elfów.
 */
const H = require('/e/lib/harness')
const { rcon } = require('/s/rcon.js')

const sleep = H.sleep
const num = s => { const m = /(-?[\d.]+)\s*$/.exec(String(s).trim()); return m ? +m[1] : NaN }
const papi = async (nick, ph) => (await rcon(`papi parse ${nick} ${ph}`)).trim()
const maxHp = async nick => num(await rcon(`attribute ${nick} minecraft:max_health get`))

function wcisnijQ(bot) {
  bot._client.write('block_dig', { status: 4, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 })
}

async function wezCosDoReki(bot) {
  for (let i = 0; i < 9; i++) {
    if (bot.inventory.slots[36 + i]) { bot.setQuickBarSlot(i); await sleep(400); return true }
  }
  return false
}

/** Linie opisu przedmiotu jako czysty tekst (komponent lore z 1.21). */
function lore(item) {
  if (!item) return []
  let l = item.customLore
  if (!l && item.components) {
    const c = item.components.find(x => x.type === 'lore')
    l = c && (c.data || c.value)
  }
  if (!Array.isArray(l)) return []
  return l.map(x => {
    if (typeof x === 'string') { try { x = JSON.parse(x) } catch { return x.replace(/§./g, '') } }
    return H.plainText ? H.plainText(x) : JSON.stringify(x)
  }).map(s => String(s).replace(/§./g, ''))
}

const etykieta = (win, slot) => { const s = H.windowSlots(win).find(x => x.slot === slot); return s ? s.label : null }
const czatOd = (bot, n) => bot.evalLog.slice(n).join(' | ')

async function otworzWiare(bot, r, opis) {
  await wezCosDoReki(bot)
  wcisnijQ(bot)
  const menu = await H.waitForWindow(bot, { timeout: 12000, titleIncludes: 'Menu' })
  if (!r.assert(!!menu, `${opis}: Q otwiera menu główne`, menu ? H.windowTitle(menu) : 'brak okna')) return null
  await H.click(bot, 10)
  const postac = await H.waitForWindow(bot, { timeout: 8000, titleIncludes: 'Posta' })
  if (!r.assert(!!postac, `${opis}: kafelek Postać`, postac ? H.windowTitle(postac) : 'brak')) return null
  r.assert(/Wiara/.test(etykieta(postac, 14) || ''), `${opis}: w Postaci jest kafelek Wiara (slot 14)`, etykieta(postac, 14))
  await H.click(bot, 14)
  const w = await H.waitForWindow(bot, { timeout: 8000, titleIncludes: 'Wiara' })
  r.assert(!!w, `${opis}: kafelek Wiara otwiera menu Wiary`, w ? H.windowTitle(w) : 'brak')
  return w
}

function sprawdzBostwa(r, w, opis, oczekiwane) {
  const got = [10, 11, 12, 14, 15, 16].map(s => etykieta(w, s))
  r.assert(JSON.stringify(got) === JSON.stringify(oczekiwane), `${opis}: bóstwa w slotach 10-12 i 14-16`, JSON.stringify(got))
  const obce = H.windowSlots(w).filter(s => s.slot < 27 && ![4, 10, 11, 12, 14, 15, 16, 18, 22, 26].includes(s.slot))
  r.assert(obce.length === 0, `${opis}: brak przedmiotów poza układem`, JSON.stringify(obce))
}

module.exports = async function (r) {
  const nick = 'Wiara' + (Date.now() % 100000)
  for (const c of ['forceload add 143 191 155 203', 'fill 143 97 191 155 97 203 minecraft:stone']) await rcon(c)
  let bot = await H.connect(nick)
  try {
    await sleep(6000)
    console.log('KONSOLA testpostac:', (await rcon(`cc testpostac ${nick} ork wojownik Gromka`)).trim())
    await sleep(8000)
    r.assert((await papi(nick, '%wiary_rasa%')) === 'ork', 'postać testowa jest orkiem (rasa z klasy MMOCore)',
      `klasa ${await papi(nick, '%mmocore_class_id%')}`)

    // --- 1-2. menu z perspektywy gracza --------------------------------------------------
    let w = await otworzWiare(bot, r, 'ork')
    if (!w) return
    sprawdzBostwa(r, w, 'ork', ['Ordhun', 'Nysara', 'Vharun', 'Gorrath', 'Kharzuk', 'Mogra'])
    const loreG = lore(w.slots[14])
    console.log('LORE Gorrath:', JSON.stringify(loreG))
    r.assert(loreG.some(l => /\+\d+(,\d)?% maksymalnego zdrowia/.test(l)), 'opis łaski Gorratha ma policzoną wartość', loreG.join(' / '))
    const wszystkieLore = H.windowSlots(w).flatMap(s => lore(w.slots[s.slot])).join(' ')
    r.assert(!/%wiary_/.test(wszystkieLore), 'żaden opis nie ma surowego placeholdera', wszystkieLore.match(/%wiary_[a-z_]*%/g) || '')
    r.assert(/Módl się/.test(etykieta(w, 22) || '') && /obierz patrona/i.test(lore(w.slots[22]).join(' ')),
      'bez patrona modlitwa mówi, co zrobić', lore(w.slots[22]).join(' / '))

    // --- 3. wybór patrona i modlitwa -------------------------------------------------------
    let n = bot.evalLog.length
    await H.click(bot, 14)
    await sleep(800)
    r.assert(/Twój patron: Gorrath, Pierwszy Wódz/.test(czatOd(bot, n)), 'klik w Gorratha = patron (czat)', czatOd(bot, n))
    r.assert((await papi(nick, '%wiary_patron%')) === 'gorrath', 'placeholder patrona = gorrath')
    w = bot.currentWindow
    r.assert(w && /Kliknij, aby się pomodlić/.test(lore(w.slots[22]).join(' ')), 'po wyborze Módl się jest aktywne', w ? lore(w.slots[22]).join(' / ') : 'okno zamknięte')
    r.assert(w && /Twój patron/.test(lore(w.slots[14]).join(' ')), 'Gorrath oznaczony jako twój patron', w ? lore(w.slots[14]).join(' / ') : '')

    const hp0 = await maxHp(nick)
    n = bot.evalLog.length
    await H.click(bot, 22)
    await sleep(1200)
    const hp1 = await maxHp(nick)
    r.assert(/Łaska Gorrath: \+\d+(,\d)?% maksymalnego zdrowia/.test(czatOd(bot, n)), 'modlitwa daje łaskę (czat)', czatOd(bot, n))
    r.assert(hp1 > hp0 && Math.abs(hp1 / hp0 - 1.03) < 0.02, 'łaska naprawdę podnosi max_health o ~3% (Wiara 0)', `${hp0} -> ${hp1}`)
    w = bot.currentWindow
    r.assert(w && /Łaska trwa/.test(etykieta(w, 22) || ''), 'w trakcie łaski slot 22 pokazuje Łaska trwa', w ? etykieta(w, 22) : '')
    console.log('KONSOLA info:', (await rcon(`wiary info ${nick}`)).trim())

    // --- 4. odmowy -------------------------------------------------------------------------
    n = bot.evalLog.length
    await H.click(bot, 22)
    await sleep(800)
    r.assert((await maxHp(nick)) === hp1, 'klik w Łaska trwa niczego nie dokłada', `${await maxHp(nick)}`)
    await H.click(bot, 15)
    await sleep(800)
    r.assert(/Patrona zmienisz za 2[34] h/.test(czatOd(bot, n)), 'zmiana patrona w ciągu 24 h odrzucona', czatOd(bot, n))
    r.assert((await papi(nick, '%wiary_patron%')) === 'gorrath', 'patron nadal gorrath')

    // --- 5. obca rasa i komenda z czatu ------------------------------------------------------
    console.log('KONSOLA solenna:', (await rcon(`wiary reset ${nick}`)).trim(), '| reset tylko na chwilę')
    n = bot.evalLog.length
    await rcon(`wiary wybierz ${nick} solenna`)
    await sleep(600)
    r.assert(/nie przyjmuje modlitw twojej rasy/.test(czatOd(bot, n)), 'bóstwo ludzi odrzucone dla orka nawet z konsoli', czatOd(bot, n))
    r.assert((await papi(nick, '%wiary_patron%')) === 'brak', 'po odmowie brak patrona')
    r.assert((await maxHp(nick)) === hp0, 'reset zdjął łaskę (max_health wróciło)', `${await maxHp(nick)} vs ${hp0}`)
    bot.closeWindow(bot.currentWindow)
    n = bot.evalLog.length
    bot.chat(`/wiary wybierz ${nick} gorrath`)
    await sleep(1200)
    r.assert((await papi(nick, '%wiary_patron%')) === 'brak', 'gracz bez OP nie wybierze patrona komendą z czatu', czatOd(bot, n))
    // przywróć stan: patron + łaska przez menu, jak gracz
    w = await otworzWiare(bot, r, 'ork ponownie')
    if (!w) return
    await H.click(bot, 14); await sleep(800); await H.click(bot, 22); await sleep(1200)
    const hpOrk = await maxHp(nick)
    r.assert(hpOrk > hp0, 'ork znów ma łaskę', `${hpOrk}`)

    // --- 6. relog: łaska zdjęta w scenie wyboru, wraca po wyborze orka -----------------------
    bot.quit(); await sleep(3000)
    bot = await H.connect(nick)
    await sleep(8000)
    const infoScena = (await rcon(`wiary info ${nick}`)).trim()
    console.log('RELOG info:', infoScena)
    r.assert(/profil=brak/.test(infoScena), 'w scenie wyboru postać nie ma aktywnej wiary', infoScena)
    const manekiny = Object.values(bot.entities).filter(e => e !== bot.entity && e.position.distanceTo(bot.entity.position) < 16)
      .map(e => ({ e, d: e.position.distanceTo(bot.entity.position) })).sort((x, y) => x.d - y.d)
    console.log('SCENA encje:', JSON.stringify(manekiny.slice(0, 8).map(x => [x.e.name, x.e.displayName, Math.round(x.d * 10) / 10])))
    let wybrano = false
    for (const { e } of manekiny.filter(x => /interaction|mannequin/i.test(x.e.name || '')).slice(0, 4)) {
      try { await bot.lookAt(e.position.offset(0, 1.5, 0)); await bot.activateEntity(e) } catch (err) { console.log('klik', err.message) }
      await sleep(3000)
      if (bot.currentWindow) console.log('SCENA okno po kliku:', H.windowTitle(bot.currentWindow), JSON.stringify(H.windowSlots(bot.currentWindow).slice(0, 8)))
      if ((await papi(nick, '%wiary_rasa%')) === 'ork') { wybrano = true; break }
    }
    if (wybrano) {
      await sleep(2500)
      const infoPowrot = (await rcon(`wiary info ${nick}`)).trim()
      console.log('PO WYBORZE info:', infoPowrot)
      r.assert(/patron=gorrath/.test(infoPowrot) && /modyfikatorów=1/.test(infoPowrot), 'po powrocie na orka patron i łaska wracają', infoPowrot)
      r.assert(Math.abs((await maxHp(nick)) - hpOrk) < 0.01, 'max_health orka znów z łaską', `${await maxHp(nick)} vs ${hpOrk}`)
      bot.quit(); await sleep(3000)
      bot = await H.connect(nick)
      await sleep(8000)
    } else {
      console.log('UWAGA: bot nie wybrał postaci w scenie — powrót łaski po relogu NIESPRAWDZONY')
    }

    // --- 7. druga postać tego samego konta (ze sceny wyboru) ------------------------------
    console.log('KONSOLA testpostac elf:', (await rcon(`cc testpostac ${nick} elf mag Srebrzyk`)).trim())
    await sleep(9000)
    const infoElf = (await rcon(`wiary info ${nick}`)).trim()
    console.log('KONSOLA info elf:', infoElf)
    r.assert((await papi(nick, '%wiary_rasa%')) === 'elf', 'druga postać jest elfem')
    r.assert((await papi(nick, '%wiary_patron%')) === 'brak', 'druga postać NIE dziedziczy patrona orka (dane per postać)')
    r.assert(/modyfikatorów=0/.test(infoElf), 'łaska orka nie przeszła na elfa', infoElf)
    w = await otworzWiare(bot, r, 'elf')
    if (w) sprawdzBostwa(r, w, 'elf', ['Ordhun', 'Nysara', 'Vharun', 'Aelyra', 'Sylvaen', 'Iridel'])
  } finally {
    await H.quit(bot)
  }
}
