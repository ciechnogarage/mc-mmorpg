'use strict'
/**
 * TEST REGRESJI SCIEZKI PROFESJI (dawny "system slotow").
 *
 * Gracz: konto BEZ OP, wchodzi przez kreator, sam wpisuje /profesje i /menu i sam klika.
 * Konsola (RCON) sluzy WYLACZNIE do przygotowania danych i do pomiarow.
 *
 * Co sprawdza (kazdy punkt umie OBLAC — patrz raport, kontrola negatywna):
 *  A  menu pokazuje KAZDA z 13 profesji z jej aktualnym poziomem (bledy 2 i 3)
 *  B  zadne klikniecie w menu profesji (wszystkie sciezki, 2 poziomy w glab)
 *     nie zmienia poziomu ani XP zadnej profesji (blad 1 — wybor kasowal postep)
 *  A2 po przejsciu wszystkich sciezek menu nadal pokazuje kazda profesje (blad 2 —
 *     po wyborze przycisk "zmiany" byl pusty dla 10 z 11 profesji)
 *  C  saldo nie zmienia sie od zadnego klikniecia (bledy 4 i 6 — platna zmiana / cooldown)
 *  D  podwojne klikniecie w tym samym ticku nie zmienia stanu
 *  E  z kafelka profesji gracz BEZ OP otwiera jej warsztat (jedyna droga do warsztatu)
 *  F  hub: kafelek Profesji nie obiecuje "dwoch profesji", a zloto pokazuje saldo
 *     waluty Vault (CoreTools dollar), nie wylaczonej ekonomii CMI
 */
const net = require('net')
const nbt = require('/e/node_modules/prismarine-nbt')
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const NICK = process.env.EVAL_NICK
const DEBUG = !!process.env.EVAL_DEBUG

// ---------- RCON (tylko pomiar i przygotowanie) ----------
function rconClient() {
  const sock = net.connect(25575, process.env.RCON_HOST || 'world1')
  let buf = Buffer.alloc(0); let id = 10; const pending = new Map()
  sock.on('data', d => {
    buf = Buffer.concat([buf, d])
    while (buf.length >= 4) {
      const len = buf.readInt32LE(0); if (buf.length < len + 4) break
      const rid = buf.readInt32LE(4); const body = buf.toString('utf8', 12, 4 + len - 2)
      buf = buf.subarray(4 + len)
      const p = pending.get(rid); if (p) { pending.delete(rid); p(body) }
    }
  })
  const send = (type, body) => new Promise(res => {
    const rid = ++id; const b = Buffer.from(body, 'utf8'); const pkt = Buffer.alloc(14 + b.length)
    pkt.writeInt32LE(10 + b.length, 0); pkt.writeInt32LE(rid, 4); pkt.writeInt32LE(type, 8); b.copy(pkt, 12)
    pending.set(rid, res); sock.write(pkt)
    setTimeout(() => { if (pending.has(rid)) { pending.delete(rid); res('') } }, 5000)
  })
  return new Promise((res, rej) => {
    sock.once('error', rej)
    sock.once('connect', async () => { await send(3, process.env.RCON_PW); res({ cmd: c => send(2, c), close: () => sock.end() }) })
  })
}
const strip = s => String(s).replace(/§./g, '').trim()

// ---------- dane ----------
const PROF = {
  mining: /G[óo]rni/, woodworking: /Drwal/, herbalism: /Ziela/, farming: /Farme/,
  fishing: /Ryba/, hunting: /My[śs]li/, jewelcrafting: /Jubil/, smithing: /Kowal/,
  tailoring: /Kraw/, alchemy: /Alchem/, enchanting: /Zaklin/, cooking: /Kuchar|Gotow/,
  building: /Budow/,
}
const IDS = Object.keys(PROF)

// ---------- pomocnicze: tekst przedmiotu ----------
function txt(n) {
  if (n == null) return ''
  if (typeof n === 'string') { try { if (n.startsWith('{') || n.startsWith('[')) return txt(JSON.parse(n)) } catch {} return n }
  if (Array.isArray(n)) return n.map(txt).join('')
  if (typeof n === 'object' && n.type && 'value' in n) { try { return txt(nbt.simplify(n)) } catch { return txt(n.value) } }
  let o = ''
  if (n.text != null) o += txt(n.text)
  if (n[''] != null) o += txt(n[''])
  if (n.extra) o += txt(n.extra)
  return o
}
const lore = it => { const l = it && it.customLore; return Array.isArray(l) ? l.map(x => strip(txt(x))) : [] }
const name = it => strip(H.itemLabel(it) || '')
const tiles = w => w ? w.slots.slice(0, w.inventoryStart ?? (w.slots.length - 36))
  .map((it, slot) => it ? { slot, name: name(it), lore: lore(it) } : null).filter(Boolean) : []

// ---------- wynik ----------
const R = new H.Results('sloty')
let failedAny = false
const check = (cond, label, extra) => { if (!cond) failedAny = true; return R.assert(cond, label, extra) }

;(async () => {
  const rc = await rconClient()
  const papi = async ph => strip((await rc.cmd(`papi parse ${NICK} ${ph}`)))
  const stan = async () => {
    const s = {}
    for (const p of IDS) s[p] = `${await papi(`%mmocore_profession_${p}%`)}/${await papi(`%mmocore_profession_experience_${p}%`)}`
    return s
  }
  const roznice = (a, b) => IDS.filter(p => a[p] !== b[p]).map(p => `${p} ${a[p]} -> ${b[p]}`)
  const saldo = () => papi('%coretools_eco_dollar%')

  const bot = await H.connect(NICK, { timeout: 90000 })
  try {
    try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) }
    catch (e) { console.log(`(kreator: ${e.message})`) }
    await sleep(2500)
    // -------- przygotowanie: kazda profesja na innym, znanym poziomie i z niezerowym XP --------
    const LVL = {}
    IDS.forEach((p, i) => { LVL[p] = 3 + i })   // 3..15 — rozne, zeby pomylka byla widoczna
    for (const p of IDS) {
      await rc.cmd(`mmocore admin level set ${NICK} ${p} ${LVL[p]}`)
      await rc.cmd(`mmocore admin exp set ${NICK} ${p} 7`)
    }
    await sleep(500)
    const base = await stan()
    const saldo0 = await saldo()
    console.log('stan startowy:', JSON.stringify(base), '| saldo', saldo0)
    check(IDS.every(p => base[p] === `${LVL[p]}/7`), 'przygotowanie: 13 profesji na zadanych poziomach', roznice(Object.fromEntries(IDS.map(p => [p, `${LVL[p]}/7`])), base).join('; '))

    const otworzMenu = async () => {
      try { if (bot.currentWindow) bot.closeWindow(bot.currentWindow) } catch {}
      await sleep(300)
      bot.chat('/profesje')
      const w = await H.waitForWindow(bot, { timeout: 6000, titleIncludes: 'Profesje' })
      await sleep(1300) // update_interval / pierwsze odswiezenie placeholderow
      return bot.currentWindow && H.windowTitle(bot.currentWindow).includes('Profesje') ? bot.currentWindow : w
    }
    const sprawdzWidocznosc = (w, etykieta) => {
      const t = tiles(w); const brak = []
      for (const p of IDS) {
        const tile = t.find(x => PROF[p].test(x.name))
        const ok = tile && tile.lore.some(l => new RegExp(`Poziom:?\\s*${LVL[p]}\\b`).test(l))
        if (!ok) brak.push(`${p}${tile ? '(bez poziomu ' + LVL[p] + ')' : '(brak kafelka)'}`)
      }
      check(brak.length === 0, `${etykieta}: menu pokazuje kazda z 13 profesji z aktualnym poziomem`, brak.length ? `brakuje ${brak.length}: ${brak.join(', ')}` : '')
      return t
    }

    // -------- A --------
    let w = await otworzMenu()
    check(!!w, 'A0: /profesje otwiera menu (gracz bez OP)', w ? H.windowTitle(w) : 'brak okna')
    const tMain = sprawdzWidocznosc(w, 'A')
    if (DEBUG) console.log(JSON.stringify(tMain, null, 1))
    const mainTitle = w ? H.windowTitle(w) : ''

    // -------- B: kazda sciezka menu, 2 poziomy w glab --------
    const zmiany = []
    let klikniec = 0
    const slotyMain = tMain.map(x => x.slot)
    for (const s of slotyMain) {
      const lbl = (tMain.find(x => x.slot === s) || {}).name
      if (/Zamknij|Wstecz|Powr/.test(lbl || '')) continue
      w = await otworzMenu(); if (!w) break
      await bot.clickWindow(s, 0, 0).catch(() => {}); klikniec++; await sleep(1300)
      let now = await stan(); let d = roznice(base, now)
      if (d.length) { zmiany.push(`[${lbl}] ${d.join(', ')}`); for (const p of IDS) base[p] = now[p] }
      const sub = bot.currentWindow; const st = sub ? H.windowTitle(sub) : ''
      if (sub && /profes/i.test(st) && st !== mainTitle) {
        const tSub = tiles(sub)
        for (const t2 of tSub) {
          if (/Zamknij|Wstecz|Powr/.test(t2.name)) continue
          w = await otworzMenu(); if (!w) break
          await bot.clickWindow(s, 0, 0).catch(() => {}); await sleep(1300)
          if (!bot.currentWindow || H.windowTitle(bot.currentWindow) !== st) continue
          await bot.clickWindow(t2.slot, 0, 0).catch(() => {}); klikniec++; await sleep(1500)
          now = await stan(); d = roznice(base, now)
          if (d.length) { zmiany.push(`[${lbl} > ${t2.name}] ${d.join(', ')}`); for (const p of IDS) base[p] = now[p] }
        }
      }
    }
    check(klikniec > 0 && zmiany.length === 0, `B: ${klikniec} klikniec we wszystkie sciezki menu profesji nie zmienia poziomu ani XP`, zmiany.join(' | '))

    // -------- A2 --------
    for (const p of IDS) { await rc.cmd(`mmocore admin level set ${NICK} ${p} ${LVL[p]}`); await rc.cmd(`mmocore admin exp set ${NICK} ${p} 7`) }
    await sleep(400)
    w = await otworzMenu()
    sprawdzWidocznosc(w, 'A2 (po przejsciu wszystkich sciezek)')

    // -------- C --------
    const saldo1 = await saldo()
    check(saldo1 === saldo0, 'C: zadne klikniecie w menu profesji nie pobiera pieniedzy', `${saldo0} -> ${saldo1}`)

    // -------- D: podwojne klikniecie w jednym ticku --------
    const s0 = await stan()
    w = await otworzMenu()
    const herb = tiles(w).find(x => PROF.herbalism.test(x.name))
    if (herb) { bot.clickWindow(herb.slot, 0, 0).catch(() => {}); bot.clickWindow(herb.slot, 0, 0).catch(() => {}); await sleep(2000) }
    const d2 = roznice(s0, await stan())
    check(!!herb && d2.length === 0, 'D: podwojne klikniecie kafelka w jednym ticku nie zmienia stanu', herb ? d2.join(', ') : 'brak kafelka zielarstwa')

    // -------- E: warsztat z kafelka (gracz bez OP) --------
    const warsztaty = { mining: 'Górnika', woodworking: 'Drwala', herbalism: 'Zielarza', farming: 'Farmera',
      fishing: 'Rybaka', hunting: 'Myśliwego', jewelcrafting: 'Jubilera', smithing: 'Kowala', tailoring: 'Krawca',
      alchemy: 'Alchemika', enchanting: 'Zaklinacza', cooking: 'Kucharza', building: 'Budowniczego' }
    for (const [p, frag] of Object.entries(warsztaty)) {
      let t, tt = '-'
      // jedna powtorka TYLKO gdy okno w ogole nie doszlo: mineflayer potrafi nie sparsowac
      // pakietu okna (blad protodef na stderr) — serwer okno otwiera, co sprawdza probe-stacja.js
      for (let proba = 0; proba < 2 && tt === '-'; proba++) {
        w = await otworzMenu()
        t = tiles(w).find(x => PROF[p].test(x.name))
        if (t) { await bot.clickWindow(t.slot, 0, 0).catch(() => {}); await sleep(1800) }
        tt = bot.currentWindow ? H.windowTitle(bot.currentWindow) : '-'
      }
      check(!!t && /Warsztat/.test(tt) && tt.includes(frag), `E: kafelek ${p} otwiera warsztat profesji`, `okno: ${tt}`)
    }

    // -------- G: pelna petla gracza bez OP: menu -> warsztat -> wyrob -> XP profesji --------
    // Racja podroznika (gotowanie, poziom 0): 2x Dzikie ziele przyprawowe, +12 XP gotowania, 1 s.
    await rc.cmd(`mi give MATERIAL PROF_WILD_SPICE ${NICK} 2`); await sleep(800)
    const xpG0 = await papi('%mmocore_profession_experience_cooking%')
    w = await otworzMenu()
    const tc = tiles(w).find(x => PROF.cooking.test(x.name))
    if (tc) { await bot.clickWindow(tc.slot, 0, 0).catch(() => {}); await sleep(1800) }
    let ws = bot.currentWindow
    const rec = ws ? tiles(ws).find(x => /Racja/i.test(x.name)) : null
    if (rec) { await bot.clickWindow(rec.slot, 0, 0).catch(() => {}); await sleep(2500) }
    ws = bot.currentWindow
    const q = ws ? tiles(ws).find(x => /Kolejka:/.test(x.name)) : null
    if (q) { await bot.clickWindow(q.slot, 0, 0).catch(() => {}); await sleep(1500) }
    const xpG1 = await papi('%mmocore_profession_experience_cooking%')
    const maRacje = bot.inventory.items().some(i => /Racja/i.test(name(i)))
    check(parseFloat(xpG1) - parseFloat(xpG0) === 12 && maRacje,
      'G: gracz bez OP: menu profesji -> warsztat -> wyrob daje przedmiot i XP profesji',
      `XP gotowania ${xpG0} -> ${xpG1}, racja w plecaku: ${maRacje}, receptura: ${rec ? rec.name : '-'}, kolejka: ${q ? q.name : '-'}`)

    // -------- F: hub --------
    try { if (bot.currentWindow) bot.closeWindow(bot.currentWindow) } catch {}
    // saldo startowe to 50 w OBU ekonomiach, wiec porownanie przy 50 niczego nie dowodzi —
    // zmieniamy saldo Vault na nieokragla wartosc i dopiero wtedy czytamy hub
    await rc.cmd(`core-economy give dollar 123 ${NICK}`)
    const saldo2 = await saldo()
    await sleep(300); bot.chat('/menu')
    const hub = await H.waitForWindow(bot, { timeout: 6000, titleIncludes: 'Menu' }); await sleep(1200)
    const th = tiles(hub)
    const tp = th.find(x => /Profesje/.test(x.name))
    check(!!tp && !tp.lore.some(l => /Dwie profesje|zmiana|monet/i.test(l)), 'F1: kafelek Profesje w hubie nie obiecuje slotow ani platnej zmiany', tp ? tp.lore.join(' / ') : 'brak kafelka')
    const te = th.find(x => x.lore.some(l => /Złoto|Zloto/.test(l)))
    const linia = te ? te.lore.find(l => /Złoto|Zloto/.test(l)) : ''
    const num = s => { const m = String(s).replace(',', '.').match(/-?\d+(\.\d+)?/); return m ? parseFloat(m[0]) : NaN }
    check(!!te && num(linia) === num(saldo2) && num(saldo2) !== num(saldo1), "F2: hub pokazuje saldo waluty Vault (CoreTools dollar) po jego zmianie", `hub: "${linia}" | saldo Vault: ${saldo1} -> ${saldo2}`)
  } catch (e) {
    check(false, 'PRZERWANE', e.stack)
  } finally {
    R.print()
    await H.quit(bot)
    rc.close()
  }
  console.log(failedAny ? 'WYNIK: FAIL' : 'WYNIK: PASS')
  process.exit(failedAny ? 1 : 0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
