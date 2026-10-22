'use strict'
/**
 * Stopien 1 weryfikacji ikon profesyjnych.
 *
 * Bot dostaje 12 surowcow PROF_* i czyta z OTRZYMANEGO stacka komponent
 * minecraft:item_model. To dowodzi, ze stat MMOItems 'model' faktycznie zapisuje
 * komponent (czyli ze klucz w material.yml jest poprawny) - ale NIE dowodzi,
 * ze klient narysuje teksture. Od tego jest stopien 2 (klient QA + zrzuty).
 *
 * EVAL_NEGATYW=1 sprawdza dodatkowo kontrole negatywna: PROF_STEEL_TEST wskazuje
 * model, ktorego w packu nie ma - komponent ma byc obecny, ale inny.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const NICK = process.env.EVAL_NICK || 'EvIkony'
const sleep = ms => new Promise(r => setTimeout(r, ms))

const CELE = require('/w/ikony-lista.json')   // [[TYP, ID], ...] - generowane z plikow MMOItems

/** Wyciaga MMOITEMS_ITEM_ID z komponentu custom_data - po tym dopasowujemy stacki.
 *  Kolejnosc w plecaku NIE odpowiada kolejnosci /mi give, wiec indeks slotu klamie. */
function mmoId (it) {
  const comps = it.components || []
  const cd = comps.find(x => String(x.type).replace('minecraft:', '') === 'custom_data')
  try { return cd.data.value.MMOITEMS_ITEM_ID.value } catch { return null }
}

/** Wyciaga wartosc komponentu minecraft:item_model z przedmiotu mineflayera. */
function itemModel (it) {
  // 1.20.5+ : prismarine-item wystawia components jako liste {type,data}
  const comps = it.components || (it.nbt && it.nbt.value && it.nbt.value.components)
  if (Array.isArray(comps)) {
    const c = comps.find(x => String(x.type).replace('minecraft:', '') === 'item_model')
    if (c) return typeof c.data === 'string' ? c.data : JSON.stringify(c.data)
  }
  // awaryjnie: przeszukaj surowy zrzut
  const raw = JSON.stringify(it.components || it.nbt || {})
  const m = raw.match(/nexo:[a-z0-9_]+/)
  return m ? m[0] : null
}

;(async () => {
  const bot = await H.connect(NICK, { timeout: 90000 })
  try {
    try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) } catch (e) {}
    await sleep(3000)
    bot.chat(`/minecraft:execute in minecraft:overworld run tp ${NICK} 149 99 197`)
    await sleep(2000)
    bot.chat(`/minecraft:clear ${NICK}`)
    await sleep(1200)

    // Plecak ma tylko 36 slotow - przy 42 przedmiotach nadmiar wypada na ziemie
    // i eval falszywie raportuje "nie dostarczony". Stad porcje po 24 z czyszczeniem.
    const wgId = new Map()
    const PORCJA = 24
    for (let off = 0; off < CELE.length; off += PORCJA) {
      const porcja = CELE.slice(off, off + PORCJA)
      bot.chat(`/minecraft:clear ${NICK}`); await sleep(1500)
      for (const [typ, id] of porcja) { bot.chat(`/mi give ${typ} ${id} ${NICK} 1`); await sleep(400) }
      await sleep(2500)
      const items = bot.inventory.items()
      console.log(`porcja ${off / PORCJA + 1}: ${items.length} przedmiotow w plecaku`)
      for (const it of items) { const id = mmoId(it); if (id) wgId.set(id, it) }
    }

    let pass = 0
    const braki = []
    for (const [, id] of CELE) {
      const oczek = 'nexo:' + id.toLowerCase()
      const it = wgId.get(id)
      if (!it) { console.log(`FAIL ${id}: nie dostarczony`); braki.push(id); continue }
      const got = itemModel(it)
      const ok = got === oczek
      if (ok) pass++; else braki.push(id)
      console.log(`${ok ? 'PASS' : 'FAIL'} ${id.padEnd(20)} item_model=${got}  (oczek. ${oczek})`)
    }
    console.log(`WYNIK: ${pass}/${CELE.length}` + (braki.length ? '  braki: ' + braki.join(',') : ''))

    if (process.env.EVAL_NEGATYW) {
      bot.chat(`/minecraft:clear ${NICK}`); await sleep(1200)
      bot.chat(`/mi give MATERIAL PROF_ORE_CHUNK ${NICK} 1`); await sleep(2500)
      const it = bot.inventory.items()[0]
      console.log('KONTROLA NEGATYWNA item_model=' + (it ? itemModel(it) : 'brak przedmiotu'))
    }
  } finally { await H.quit(bot) }
  process.exit(0)
})().catch(e => { console.log('przerwane: ' + e.message); process.exit(2) })
