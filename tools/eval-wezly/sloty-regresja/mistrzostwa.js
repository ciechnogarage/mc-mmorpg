'use strict'
/**
 * TEST REGRESJI MISTRZOSTW PROFESJI (plugin Mistrzostwa, 2026-09-23).
 *
 * Gracz: konto BEZ OP (OP falszuje hasPermission). Sam klika menu (/profesje, PPM, okna
 * potwierdzen), sam uderza wezel, stawia i rozbija blok, lowi i wytwarza w warsztacie.
 * RCON (konsola) sluzy WYLACZNIE do przygotowania danych i do pomiarow.
 *
 * Scenariusze (kazdy ma kontrole negatywna: SABOTAZ=<regula> w run-mistrz.sh -> FAIL):
 *  Z1..Z5  profesja bez mistrzostwa STOI na 50 przy XP z kazdego z pieciu zrodel:
 *          Z1 komendy admina, Z2 wezel MythicMobs, Z3 blok Nexo, Z4 receptura MMOItems,
 *          Z5 natywne lowienie MMOCore. Kazde ma kontrole pozytywna na tej samej sciezce
 *          (ponizej 50 XP rosnie) — dowod, ze zrodlo dziala, a stoi tylko przez blokade.
 *          Z1 dodatkowo: zastrzyk przy 49 ladnie dokladnie na 50/0 (bez przeskoku 49->55).   [blokada]
 *  W       wybor mistrzostwa w menu (PPM -> "Czy na pewno?" -> Tak): licznik 1/2, zaden
 *          poziom ani XP nie zmienia sie od samego wyboru
 *  M       z mistrzostwem profesja rosnie do 100 (i ani poziomu wyzej)
 *  K       krzywa 50..100: progi co do 1 XP zgodne z plikiem, identyczne przy klasie 1 i 40
 *  O       ogolne (gotowanie, budownictwo) rosna do 100 bez wyboru i nie zajmuja limitu
 *  D       dwa klikniecia "Tak" w jednym ticku nie daja dwoch skutkow                         [klikniecia]
 *  T       trzecie mistrzostwo odrzucone (menu i obejscie menu komenda z konsoli)             [limit]
 *  P       brak pieniedzy blokuje platna zmiane z jasnym komunikatem, saldo bez zmian         [koszt]
 *  Z       platna zmiana: pobiera dokladnie koszt raz, ZADEN poziom/XP nie spada,
 *          porzucona stoi, nowa rosnie
 *  C       cooldown: druga zmiana odrzucona, takze po ponownym wejsciu do gry               [cooldown]
 *  R       po odzyskaniu porzuconego mistrzostwa rosnie dalej od swojego poziomu
 *  F       druga postac tego samego konta nie ma mistrzostw pierwszej; powrot je przywraca   [kontekst]
 */
const fs = require('fs')
const nbt = require('/e/node_modules/prismarine-nbt')
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const { rconClient } = require('/t/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const NICK = process.env.EVAL_NICK
const DEBUG = !!process.env.EVAL_DEBUG
const ONLY = (process.env.EVAL_ONLY || '').toLowerCase().split(',').map(s => s.trim()).filter(Boolean)
const run = k => !ONLY.length || ONLY.includes(k)
const strip = s => String(s).replace(/§./g, '').trim()
const KOSZT = 2500

const PROF = {
  mining: /G[óo]rni/, woodworking: /Drwal/, herbalism: /Ziela/, farming: /Farme/,
  fishing: /Ryba/, hunting: /My[śs]li/, jewelcrafting: /Jubil/, smithing: /Kowal/,
  tailoring: /Kraw/, alchemy: /Alchem/, enchanting: /Zaklin/, cooking: /Gotow/, building: /Budow/,
}
const IDS = Object.keys(PROF)
const krzywa = n => fs.readFileSync(`/krzywe/${n}.txt`, 'utf8').trim().split('\n').map(Number)
const ZB = krzywa('zbieractwo')

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

const R = new H.Results('mistrzostwa')
let failedAny = false
const check = (cond, label, extra) => { if (!cond) failedAny = true; return R.assert(!!cond, label, extra) }

;(async () => {
  const rc = await rconClient()
  const cmd = async c => strip(await rc.cmd(c))
  const papi = async ph => strip(await rc.cmd(`papi parse ${NICK} ${ph}`))
  const lvl = async p => parseInt(await papi(`%mmocore_profession_${p}%`), 10)
  const xp = async p => parseFloat(await papi(`%mmocore_profession_experience_${p}%`))
  const stan = async () => { const s = {}; for (const p of IDS) s[p] = `${await lvl(p)}/${await xp(p)}`; return s }
  const roznice = (a, b) => IDS.filter(p => a[p] !== b[p]).map(p => `${p} ${a[p]} -> ${b[p]}`)
  const ustaw = async (p, L, X = 0) => { await cmd(`mmocore admin level set ${NICK} ${p} ${L}`); await cmd(`mmocore admin exp set ${NICK} ${p} ${X}`) }
  const saldo = async () => parseFloat(await papi('%coretools_eco_dollar%'))
  const info = async () => {
    const t = await cmd(`mistrzostwo info ${NICK}`)
    const m = t.match(/mistrzostwa=\[([^\]]*)\]/); const pr = t.match(/profil=(\S+)/)
    return { m: m ? m[1].split(',').map(s => s.trim()).filter(Boolean).sort() : null, profil: pr ? pr[1] : null, raw: t }
  }
  const czat = () => bot.evalLog.map(strip).filter(l => l && !/[❤♦⛨]/.test(l))
  const tp = async (x, y, z) => { await cmd(`minecraft:execute in minecraft:overworld run tp ${NICK} ${x} ${y} ${z}`); await sleep(2500) }

  let bot
  const wejdz = async ({ nowaPostac = false, profilSlot = 10 } = {}) => {
    bot = await H.connect(NICK, { timeout: 90000 })
    const w = await F.waitProfileScreen(bot).catch(() => null)
    if (w) {
      const sloty = bot.currentWindow.slots
      if (nowaPostac || !sloty[profilSlot] || /Create/i.test(name(sloty[profilSlot]))) {
        const create = sloty.findIndex(it => it && /Create New Character/i.test(name(it)))
        await H.click(bot, create)
        for (let i = 0; i < 60 && !F.isCreatorWindow(bot); i++) await sleep(300)
        if (F.isCreatorWindow(bot)) await F.completeCreator(bot)
      } else {
        await H.click(bot, profilSlot); await sleep(4000)
      }
    }
    await sleep(2500)
    await cmd(`minecraft:gamemode survival ${NICK}`)
  }
  const wyjdz = async () => { await H.quit(bot); await sleep(3500) }

  const otworzMenu = async () => {
    try { if (bot.currentWindow) bot.closeWindow(bot.currentWindow) } catch {}
    await sleep(300)
    bot.chat('/profesje')
    await H.waitForWindow(bot, { timeout: 6000, titleIncludes: 'Profesje' })
    await sleep(1400)
    return bot.currentWindow && H.windowTitle(bot.currentWindow).includes('Profesje') ? bot.currentWindow : null
  }
  const kafelek = (w, p) => tiles(w).find(x => PROF[p].test(x.name))
  const czekajOkno = async (frag, ms = 6000) => {
    const d = Date.now() + ms
    while (Date.now() < d) { if (bot.currentWindow && H.windowTitle(bot.currentWindow).includes(frag)) { await sleep(900); return bot.currentWindow } await sleep(200) }
    return null
  }
  const tytul = () => bot.currentWindow ? H.windowTitle(bot.currentWindow) : '-'
  /** PPM na kafelku profesji; zwraca okno z tytulem `frag` albo null */
  const ppm = async (p, frag) => {
    const w = await otworzMenu(); const t = w && kafelek(w, p)
    if (!t) return null
    await bot.clickWindow(t.slot, 1, 0).catch(() => {})
    return czekajOkno(frag)
  }
  const kliknijNazwe = async (re, podwojnie = false) => {
    const t = tiles(bot.currentWindow).find(x => re.test(x.name))
    if (!t) return false
    if (podwojnie) { bot.clickWindow(t.slot, 0, 0).catch(() => {}); bot.clickWindow(t.slot, 0, 0).catch(() => {}) }
    else await bot.clickWindow(t.slot, 0, 0).catch(() => {})
    await sleep(2200)
    return true
  }

  try {
    await wejdz({ nowaPostac: true })
    const prof1 = (await info()).profil
    console.log('profil 1:', prof1)
    // wszystkie profesje na znanym poziomie < 50; saldo znane
    for (const [i, p] of IDS.entries()) await ustaw(p, 10 + i, 3)
    await cmd(`core-economy set dollar 50 ${NICK}`)

    // ==================================================================== Z: piec zrodel
    if (run('zrodla')) {
      // ---- Z1 komendy admina (mining)
      await ustaw('mining', 50)
      await cmd(`mmocore admin exp give ${NICK} mining 5000`)
      await cmd(`mmocore admin level give ${NICK} mining 3`)
      const z1 = `${await lvl('mining')}/${await xp('mining')}`
      await ustaw('mining', 30)
      await cmd(`mmocore admin exp give ${NICK} mining 100`)
      const z1k = await xp('mining')
      check(z1 === '50/0' && z1k === 100, 'Z1: admin exp/level give — bez mistrzostwa stoi na 50 (kontrola: przy 30 rosnie)', `przy 50: ${z1}; przy 30: +100 -> ${z1k}`)
      await ustaw('mining', 49)
      await cmd(`mmocore admin exp give ${NICK} mining 999999`)
      const z1c = `${await lvl('mining')}/${await xp('mining')}`
      check(z1c === '50/0', 'Z1b: duzy zastrzyk przy 49 laduje dokladnie na 50/0 (bez przeskoku za prog)', `49 + 999999 XP -> ${z1c}`)
      const komunikat = czat().some(l => /stoi na 50/.test(l))
      check(komunikat, 'Z1c: gracz dostaje komunikat o blokadzie', czat().slice(-3).join(' | '))

      // ---- Z2 wezel MythicMobs (herbalism) — uderzenie INTERACT_AT
      const wezel = frag => Object.values(bot.entities)
        .filter(e => e.name === 'zombie' && e.position.distanceTo(bot.entity.position) < 9)
        .filter(e => { const j = JSON.stringify(e.metadata || {}).toLowerCase(); const plain = (j.match(/"text":"([^"]*)"/g) || []).map(x => x.slice(8, -1)).join(''); return j.includes(frag) || plain.includes(frag) })
        .sort((a, b) => a.position.distanceTo(bot.entity.position) - b.position.distanceTo(bot.entity.position))[0]
      const uderz = async (px, frag) => {
        await tp(px, 99, 197.5)
        const w = wezel(frag)
        if (!w) return false
        await bot.lookAt(w.position.offset(0, 1, 0), true); await sleep(300)
        await bot.activateEntityAt(w, w.position.offset(0, 0.5, 0)).catch(() => {})
        await sleep(5500)
        return true
      }
      const plecak = () => bot.inventory.items().map(i => `${i.count}x ${name(i)}`).join(', ')
      await ustaw('herbalism', 50)
      const przedPl = plecak()
      const byl = await uderz(152.5, 'ziół') || await uderz(152.5, 'grzybów') || await uderz(154.5, 'księżycowy')
      const z2 = `${await lvl('herbalism')}/${await xp('herbalism')}`
      const zebral = plecak() !== przedPl
      await ustaw('herbalism', 49)
      const byl2 = await uderz(154.5, 'księżycowy') || await uderz(152.5, 'grzybów') || await uderz(152.5, 'ziół')
      const z2k = await xp('herbalism')
      check(byl && zebral && z2 === '50/0' && byl2 && z2k > 0, 'Z2: wezel MythicMobs — bez mistrzostwa stoi na 50 (kontrola: przy 49 rosnie)',
        `wezel: ${byl}, surowiec: ${zebral}, przy 50: ${z2}; kontrola wezel: ${byl2}, XP przy 49: ${z2k}`)

      // ---- Z3 blok Nexo (mining) — postaw i rozbij
      await tp(150.5, 99, 197.5)
      await cmd(`minecraft:clear ${NICK}`)
      await cmd(`nexo give ${NICK} ruda_urobku 2`)
      await cmd(`minecraft:give ${NICK} minecraft:diamond_pickaxe 1`)
      await sleep(1500)
      const rozbij = async () => {
        const blk = bot.inventory.items().find(i => i.name === 'note_block')
        const pick = bot.inventory.items().find(i => i.name === 'diamond_pickaxe')
        if (!blk || !pick) return 'brak bloku/kilofa'
        const base = bot.entity.position.floored()
        let ref = null
        for (const d of [[2, -1, 0], [-2, -1, 0], [0, -1, 2], [0, -1, -2], [2, -1, 2], [-2, -1, -2]]) {
          const p = base.offset(...d); const b = bot.blockAt(p); const a = bot.blockAt(p.offset(0, 1, 0))
          if (b && b.boundingBox === 'block' && a && a.name === 'air') { ref = b; break }
        }
        if (!ref) return 'brak miejsca'
        await bot.equip(blk, 'hand'); await sleep(400)
        await bot.lookAt(ref.position.offset(0.5, 1, 0.5), true)
        try { await bot.placeBlock(ref, new (require('/e/node_modules/vec3'))(0, 1, 0)) } catch (e) {}
        await sleep(1200)
        const pos = ref.position.offset(0, 1, 0)
        const postawiony = bot.blockAt(pos)
        if (!postawiony || postawiony.name !== 'note_block') return 'nie postawiono: ' + (postawiony && postawiony.name)
        await bot.equip(pick, 'hand'); await sleep(400)
        try { await bot.dig(postawiony, true) } catch (e) { return 'dig: ' + e.message }
        await sleep(1500)
        return bot.blockAt(pos).name === 'air' ? 'ok' : 'nie rozbity'
      }
      await ustaw('mining', 50)
      const r3 = await rozbij()
      const z3 = `${await lvl('mining')}/${await xp('mining')}`
      await ustaw('mining', 49)
      const r3k = await rozbij()
      const z3k = await xp('mining')
      check(r3 === 'ok' && z3 === '50/0' && r3k === 'ok' && z3k > 0, 'Z3: blok Nexo — bez mistrzostwa stoi na 50 (kontrola: przy 49 rosnie)',
        `rozbicie: ${r3}, przy 50: ${z3}; kontrola: ${r3k}, XP przy 49: ${z3k}`)

      // ---- Z4 receptura MMOItems (herbalism: Suszone ziola, +12) przez menu -> warsztat
      const wytworz = async () => {
        await cmd(`mi give MATERIAL PROF_MEDICINAL_HERB ${NICK} 3`); await sleep(800)
        const w = await otworzMenu(); const t = w && kafelek(w, 'herbalism')
        if (!t) return 'brak kafelka'
        await bot.clickWindow(t.slot, 0, 0).catch(() => {}); await sleep(1800)
        const rec = tiles(bot.currentWindow).find(x => /Suszone/i.test(x.name))
        if (!rec) return 'brak receptury w oknie ' + tytul()
        await bot.clickWindow(rec.slot, 0, 0).catch(() => {}); await sleep(2600)
        const q = tiles(bot.currentWindow).find(x => /Kolejka:/.test(x.name))
        if (q) { await bot.clickWindow(q.slot, 0, 0).catch(() => {}); await sleep(1500) }
        try { if (bot.currentWindow) bot.closeWindow(bot.currentWindow) } catch {}
        return bot.inventory.items().some(i => /Suszone/i.test(name(i))) ? 'ok' : 'brak wyrobu'
      }
      await cmd(`minecraft:clear ${NICK}`)
      await ustaw('herbalism', 50)
      const r4 = await wytworz()
      const z4 = `${await lvl('herbalism')}/${await xp('herbalism')}`
      await cmd(`minecraft:clear ${NICK}`)
      await ustaw('herbalism', 49)
      const r4k = await wytworz()
      const z4k = await xp('herbalism')
      check(r4 === 'ok' && z4 === '50/0' && r4k === 'ok' && z4k === 12, 'Z4: receptura MMOItems exp{} — bez mistrzostwa stoi na 50 (kontrola: przy 49 +12)',
        `wyrob: ${r4}, przy 50: ${z4}; kontrola: ${r4k}, XP przy 49: ${z4k}`)

      // ---- Z5 natywne lowienie MMOCore (on-fish)
      await tp(150.5, 99, 197.5)
      const woda = bot.findBlock({ matching: b => b && b.name === 'water', maxDistance: 64 })
      let z5 = 'brak wody', z5k = NaN, r5 = 0, r5k = 0
      if (woda) {
        const p = woda.position
        await tp(p.x + 0.5, p.y + 1, p.z + 3.5)
        const lowienie = async (maxProb = 10) => {
          await cmd(`minecraft:clear ${NICK}`); await cmd(`minecraft:give ${NICK} minecraft:fishing_rod`); await sleep(1200)
          const rod = bot.inventory.items().find(i => i.name === 'fishing_rod'); if (!rod) return 0
          await bot.equip(rod, 'hand'); await sleep(500)
          let bite = false
          const onS = n => { const s = String(n || ''); if (/splash/i.test(s) && !/throw|retrieve/i.test(s)) bite = true }
          bot.on('soundEffectHeard', onS)
          let zlowione = 0
          for (let proba = 0; proba < maxProb && !zlowione; proba++) {
            await bot.lookAt(woda.position.offset(0.5, 0.5, 0.5), true); await sleep(300)
            bite = false
            const przed = bot.inventory.items().reduce((a, i) => a + i.count, 0)
            bot.activateItem(); await sleep(900)
            const d = Date.now() + 40000
            while (!bite && Date.now() < d) await sleep(200)
            if (!bite) { bot.activateItem(); await sleep(800); continue }
            for (let t = 0; t < 60; t++) {
              bot.activateItem(); await sleep(120)
              if (bot.inventory.items().reduce((a, i) => a + i.count, 0) > przed) { zlowione++; break }
            }
            await sleep(1200)
          }
          bot.removeListener('soundEffectHeard', onS)
          return zlowione
        }
        await ustaw('fishing', 50)
        r5 = await lowienie()
        z5 = `${await lvl('fishing')}/${await xp('fishing')}`
        await ustaw('fishing', 49)
        r5k = await lowienie()
        z5k = await xp('fishing')
      }
      check(r5 > 0 && z5 === '50/0' && r5k > 0 && z5k > 0, 'Z5: natywne lowienie MMOCore — bez mistrzostwa stoi na 50 (kontrola: przy 49 rosnie)',
        `polow przy 50: ${r5}, stan: ${z5}; kontrola polow: ${r5k}, XP przy 49: ${z5k}`)
      await tp(150.5, 99, 197.5)
    }

    // ==================================================================== W: wybor w menu
    for (const [i, p] of IDS.entries()) await ustaw(p, 10 + i, 3)
    for (const p of ['mining', 'woodworking', 'herbalism', 'farming']) await ustaw(p, 50, 0)
    if (run('wybor') || run('reszta')) {
      const s0 = await stan()
      let w = await otworzMenu()
      const tm = w && kafelek(w, 'mining')
      check(tm && tm.lore.some(l => /Stoi na 50/.test(l)) && tm.lore.some(l => /PPM: wybierz mistrzostwo/.test(l)) && tm.lore.some(l => /Poziom:\s*50\/50/.test(l)),
        'W0: kafelek profesji na 50 pokazuje blokade i "PPM: wybierz mistrzostwo"', tm ? tm.lore.join(' / ') : 'brak kafelka')
      const ksiega = tiles(w).find(x => /Mistrzostwa:/.test(x.name))
      check(ksiega && /0\s*\/\s*2/.test(ksiega.name), 'W0b: licznik "Mistrzostwa: 0/2"', ksiega ? ksiega.name : 'brak')
      const tc = w && kafelek(w, 'cooking')
      check(tc && tc.lore.some(l => /ogólna/.test(l)) && tc.lore.some(l => /\/100/.test(l)), 'W0c: gotowanie oznaczone jako ogolne, sufit /100', tc ? tc.lore.join(' / ') : '-')
      const okno = await ppm('mining', 'Mistrzostwo')
      const info13 = okno ? tiles(okno).find(x => x.slot === 13) : null
      check(!!okno && info13 && info13.lore.some(l => /Czy na pewno/.test(l)) && info13.lore.some(l => /2500/.test(l)),
        'W1: PPM otwiera okno "Czy na pewno?" z konsekwencjami (limit, koszt zmiany)', okno ? `${tytul()} :: ${info13 && info13.lore.join(' / ')}` : `okno: ${tytul()}`)
      if (okno) await kliknijNazwe(/Tak, wybieram/)
      const i1 = await info()
      const s1 = await stan()
      check(JSON.stringify(i1.m) === '["mining"]', 'W2: po "Tak" mining jest mistrzostwem (1/2)', i1.raw)
      check(roznice(s0, s1).length === 0, 'W3: wybor nie zmienia ZADNEGO poziomu ani XP', roznice(s0, s1).join('; '))
      check(/Profesje/.test(tytul()), 'W4: po wyborze wraca menu profesji', tytul())
      w = await otworzMenu()
      const k2 = tiles(w).find(x => /Mistrzostwa:/.test(x.name))
      const tm2 = kafelek(w, 'mining')
      check(k2 && /1\s*\/\s*2/.test(k2.name) && tm2 && tm2.lore.some(l => /Twoje mistrzostwo/.test(l)) && tm2.lore.some(l => /\/100/.test(l)),
        'W5: menu pokazuje "Mistrzostwa: 1/2" i mining jako mistrzostwo do 100', `${k2 && k2.name} | ${tm2 && tm2.lore.join(' / ')}`)
    }

    // ==================================================================== M + K
    if (run('krzywa') || run('reszta')) {
      await ustaw('mining', 50)
      await cmd(`mmocore admin exp give ${NICK} mining 100000000`)
      const m = `${await lvl('mining')}/${await xp('mining')}`
      check(m === '100/0', 'M: z mistrzostwem rosnie do 100 i ani poziomu wyzej', `50 + 1e8 XP -> ${m}`)
      // K: progi co do 1 XP przy klasie 1 i 40
      const wyniki = {}
      for (const K of [1, 40]) {
        await cmd(`mmocore admin level set ${NICK} main ${K}`)
        const kl = await papi('%mmocore_level%')
        const out = []
        for (const L of [1, 2, 49, 50, 51, 75, 98, 99]) {
          const prog = ZB[L - 1]
          await ustaw('mining', L)
          await cmd(`mmocore admin exp give ${NICK} mining ${prog - 1}`)
          const a = await lvl('mining')
          await cmd(`mmocore admin exp give ${NICK} mining 1`)
          const b = await lvl('mining')
          out.push(`${L}:${a === L && b === L + 1 ? 'ok' : `ZLE(${a},${b})`}`)
        }
        wyniki[kl] = out.join(' ')
      }
      await cmd(`mmocore admin level set ${NICK} main 1`)
      const w = Object.values(wyniki)
      check(w.length === 2 && w[0] === w[1] && !w[0].includes('ZLE'), 'K: progi 1..100 zgodne z zbieractwo.txt co do 1 XP i identyczne przy klasie 1 i 40', JSON.stringify(wyniki))
    }

    // ==================================================================== O: ogolne
    if (run('ogolne') || run('reszta')) {
      const przed = (await info()).m
      await ustaw('cooking', 50); await ustaw('building', 50)
      await cmd(`mmocore admin exp give ${NICK} cooking 100000000`)
      await cmd(`mmocore admin exp give ${NICK} building 100000000`)
      const o = `${await lvl('cooking')}/${await lvl('building')}`
      const po = (await info()).m
      check(o === '100/100' && JSON.stringify(przed) === JSON.stringify(po), 'O: gotowanie i budownictwo rosna do 100 bez wyboru i nie zajmuja limitu', `poziomy ${o}, mistrzostwa ${JSON.stringify(po)}`)
    }

    // ==================================================================== D: podwojne klikniecie przy wyborze
    if (run('reszta')) {
      const okno = await ppm('woodworking', 'Mistrzostwo')
      if (okno) await kliknijNazwe(/Tak, wybieram/, true)
      await sleep(1500)
      const i2 = await info()
      check(JSON.stringify(i2.m) === '["mining","woodworking"]', 'D1: dwa klikniecia "Tak" w jednym ticku -> jedno mistrzostwo (2/2)', i2.raw)

      // ================================================================== T: trzecie odrzucone
      const okT = await ppm('herbalism', 'mistrzostwa')      // ma otworzyc ZAMIANE, nie wybor
      const tytT = tytul()
      await cmd(`mistrzostwo wybierz ${NICK} herbalism`)      // obejscie menu z konsoli
      await sleep(800)
      const i3 = await info()
      check(!!okT && /Zamiana/.test(tytT) && JSON.stringify(i3.m) === '["mining","woodworking"]',
        'T: trzecie mistrzostwo odrzucone (menu kieruje do zamiany, komenda z pominieciem menu nic nie daje)', `okno: ${tytT}; ${i3.raw}`)

      // ================================================================== P: brak pieniedzy
      await ustaw('mining', 73, 5); await ustaw('woodworking', 61, 7); await ustaw('herbalism', 50, 0)
      await cmd(`core-economy set dollar 100 ${NICK}`)
      const sal0 = await saldo()
      bot.evalLog.length = 0
      const zamien = async (z, podwojnie = false) => {
        const o = await ppm('herbalism', 'Zamiana')
        if (!o) return 'brak okna zamiany: ' + tytul()
        const t = tiles(o).find(x => new RegExp('Porzuć:\\s*' + (z === 'mining' ? 'G' : 'D')).test(x.name))
        if (!t) return 'brak przycisku porzuc ' + z + ' :: ' + tiles(o).map(x => x.name).join(',')
        await bot.clickWindow(t.slot, 0, 0).catch(() => {})
        if (!await czekajOkno('Czy na pewno')) return 'brak okna potwierdzenia: ' + tytul()
        await kliknijNazwe(/Tak, zamieniam/, podwojnie)
        await sleep(1200)
        return 'ok'
      }
      const rP = await zamien('mining')
      const iP = await info(); const salP = await saldo()
      const msgP = czat().find(l => /Brakuje/.test(l)) || ''
      check(rP === 'ok' && JSON.stringify(iP.m) === '["mining","woodworking"]' && salP === sal0 && /2500/.test(msgP),
        'P: brak pieniedzy blokuje zmiane z jasnym komunikatem, saldo i mistrzostwa bez zmian', `${rP}; ${iP.raw}; saldo ${sal0}->${salP}; komunikat: "${msgP}"`)

      // ================================================================== Z: platna zmiana + podwojne klikniecie
      await cmd(`core-economy set dollar 6000 ${NICK}`)
      const sal1 = await saldo()
      const sZ0 = await stan()
      const rZ = await zamien('mining', true)
      await sleep(1500)
      const iZ = await info(); const salZ = await saldo(); const sZ1 = await stan()
      check(rZ === 'ok' && JSON.stringify(iZ.m) === '["herbalism","woodworking"]', 'Z1: zmiana mining -> herbalism po potwierdzeniu', `${rZ}; ${iZ.raw}`)
      check(salZ === sal1 - KOSZT, 'D2: dwa klikniecia "Tak, zamieniam" w jednym ticku pobieraja koszt RAZ', `saldo ${sal1} -> ${salZ} (oczekiwane ${sal1 - KOSZT})`)
      check(roznice(sZ0, sZ1).length === 0, 'Z2: zmiana nie obniza (ani nie zmienia) zadnego poziomu ani XP', roznice(sZ0, sZ1).join('; '))
      await cmd(`mmocore admin exp give ${NICK} mining 5000`)
      await cmd(`mmocore admin exp give ${NICK} herbalism 5000`)
      const mPor = `${await lvl('mining')}/${await xp('mining')}`, hNowa = await lvl('herbalism')
      check(mPor === '73/5' && hNowa > 50, 'Z3: porzucone mistrzostwo stoi (73), nowe rosnie ponad 50', `mining ${mPor}, herbalism ${hNowa}`)
      let w = await otworzMenu()
      const tmP = kafelek(w, 'mining')
      check(tmP && tmP.lore.some(l => /Porzucone mistrzostwo.*73/.test(l)), 'Z4: kafelek porzuconej profesji mowi, ze stoi na 73', tmP ? tmP.lore.join(' / ') : '-')

      // ================================================================== C: cooldown
      bot.evalLog.length = 0
      const salC0 = await saldo()
      const oC = await ppm('mining', 'Zamiana')
      let rC = 'brak okna'
      if (oC) {
        const t = tiles(oC).find(x => /Porzuć:\s*D/.test(x.name))
        if (t) { await bot.clickWindow(t.slot, 0, 0).catch(() => {}); if (await czekajOkno('Czy na pewno')) { await kliknijNazwe(/Tak, zamieniam/); rC = 'ok' } }
      }
      const iC = await info(); const salC = await saldo()
      const msgC = czat().find(l => /Następna zmiana za/.test(l)) || ''
      check(rC === 'ok' && JSON.stringify(iC.m) === '["herbalism","woodworking"]' && salC === salC0 && !!msgC,
        'C1: druga zmiana w cooldownie odrzucona z komunikatem, saldo bez zmian', `${rC}; ${iC.raw}; saldo ${salC0}->${salC}; "${msgC}"`)
      // ponowne wejscie nie zeruje cooldownu
      await wyjdz(); await wejdz({ profilSlot: 10 })
      const iC2a = await info()
      await cmd(`mistrzostwo zmien ${NICK} woodworking mining`)
      await sleep(800)
      const iC2 = await info(); const salC2 = await saldo()
      check(iC2a.profil === prof1 && JSON.stringify(iC2.m) === '["herbalism","woodworking"]' && salC2 === salC0, 'C2: po ponownym wejsciu cooldown nadal blokuje zmiane', `profil ${iC2a.profil}; ${iC2.raw}; saldo ${salC2}`)

      // ================================================================== R: odzyskanie porzuconego
      await cmd(`mistrzostwo zdejmij-cooldown ${NICK}`)
      await cmd(`mistrzostwo zmien ${NICK} woodworking mining`)
      await sleep(800)
      const iR = await info()
      await cmd(`mmocore admin exp give ${NICK} mining 20000`)
      const mR = await lvl('mining')
      check(JSON.stringify(iR.m) === '["herbalism","mining"]' && mR > 73, 'R: odzyskane mistrzostwo rosnie dalej od 73', `${iR.raw}; mining ${mR}`)

      // ================================================================== F: druga postac
      await wyjdz(); await wejdz({ nowaPostac: true })
      const iF = await info()
      await ustaw('mining', 50); await ustaw('herbalism', 50)
      await cmd(`mmocore admin exp give ${NICK} mining 5000`); await cmd(`mmocore admin exp give ${NICK} herbalism 5000`)
      const fF = `${await lvl('mining')}/${await lvl('herbalism')}`
      w = await otworzMenu()
      const kF = w && tiles(w).find(x => /Mistrzostwa:/.test(x.name))
      check(iF.profil && iF.profil !== prof1 && JSON.stringify(iF.m) === '[]' && fF === '50/50' && kF && /0\s*\/\s*2/.test(kF.name),
        'F1: druga postac tego konta nie ma mistrzostw pierwszej (stoi na 50, menu 0/2)', `profil2 ${iF.profil} vs ${prof1}; ${iF.raw}; mining/herbalism ${fF}; ${kF && kF.name}`)
      await wyjdz(); await wejdz({ profilSlot: 10 })
      const iF2 = await info()
      check(iF2.profil === prof1 && JSON.stringify(iF2.m) === '["herbalism","mining"]', 'F2: powrot na pierwsza postac przywraca jej mistrzostwa', iF2.raw)
    }
  } catch (e) {
    check(false, 'PRZERWANE', e.stack)
  } finally {
    R.print()
    try { await H.quit(bot) } catch {}
    rc.close()
  }
  console.log(failedAny ? 'WYNIK: FAIL' : 'WYNIK: PASS')
  process.exit(failedAny ? 1 : 0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
