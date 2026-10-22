'use strict'
/**
 * Eval WARSZTATOW MMOItems przeciwko ZYWEMU world1.
 *
 * Lancuch sprawdzany dla kazdej z 13 profesji:
 *   ustaw poziom -> wyczysc plecak -> wydaj skladniki -> potwierdz, ze skladniki SA
 *   -> otworz warsztat komenda -> potwierdz, ze receptura jest w slocie
 *   -> kliknij -> (ew. odbierz z kolejki) -> WYROB W PLECAKU -> XP PROFESJI WZROSLO
 *
 * Tabela stacji nie jest przepisana recznie - leci z stacje.json, ktory generuje
 * parser YAML z plikow serwera. Dzieki temu test nie moze "przejsc" na literowce.
 *
 * Czego sie nauczylismy (nie zmieniac bez powodu):
 *  - CMI przechwytuje /tp, /kill, /clear -> uzywamy prefiksu minecraft:.
 *  - /mi give <TYP> <ID> <gracz> <ilosc> - gracz jest TRZECI.
 *  - Swiezy nick na kazdy przebieg (run.sh), inaczej kreator nie przechodzi.
 *  - Odczyt XP przyrostowo: "has N EXP in <nazwa>". Wartosc bezwzgledna nic nie mowi.
 *
 * Sabotaz (dowod, ze test UMIE oblac):
 *   EVAL_SABOTAGE=skladniki  - nie wydaje skladnikow, craft ma polec
 *   EVAL_SABOTAGE=poziom     - ustawia poziom 0, warunek profession{} ma odrzucic
 */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const STACJE = require('/w/stacje.json')
const { odczytXp: czytajXp } = require('/w/xp.js')

const NICK = process.env.EVAL_NICK || 'EvWarsztat'
const SABOTAZ = (process.env.EVAL_SABOTAGE || '').toLowerCase()
const POZIOM = 46           // spelnia najwyzszy warunek (45) i nie grozi awansem przy +12 XP
const sleep = ms => new Promise(r => setTimeout(r, ms))

const label = i => { try { return H.itemLabel(i) } catch { return (i && i.displayName) || '' } }
const invList = bot => bot.inventory.items().map(i => `${i.count}x ${label(i)}`).join(' | ') || '(pusty)'
const countLabel = (bot, nazwa) => bot.inventory.items()
  .filter(i => (label(i) || '').trim() === nazwa.trim())
  .reduce((a, i) => a + i.count, 0)

class R {
  constructor() { this.rows = [] }
  assert(c, l, e) { this.rows.push({ pass: !!c, l, e }); return !!c }
  print() {
    console.log('\n================ WYNIKI WARSZTATOW ================')
    for (const x of this.rows) console.log(`${x.pass ? 'PASS' : 'FAIL'} ${x.l}${x.e ? ' — ' + x.e : ''}`)
    const f = this.rows.filter(x => !x.pass).length
    console.log(f ? `\nWYNIK: ${f} NIEPOWODZEN / ${this.rows.length}` : `\nWYNIK: wszystko PASS (${this.rows.length})`)
    return f
  }
}

/** Komenda z czatu bota. Bot jest OP (nadaje run.sh). */
async function cmd(bot, c, wait = 700) { bot.chat('/' + c); await sleep(wait) }

const odczytXp = (bot, profesja) => czytajXp(bot, NICK, profesja)

async function zamknij(bot) {
  if (bot.currentWindow) { try { bot.closeWindow(bot.currentWindow) } catch {} await sleep(600) }
}

/** Zajete sloty okna jako czytelna lista - to jest DOWOD, nie domysl. */
function zrzutOkna(win) {
  if (!win) return '(brak okna)'
  return H.windowSlots(win).map(s => `${s.slot}:${(s.label || s.name || '').slice(0, 34)}`).join(' | ')
}

async function testStacji(bot, r, st) {
  // EVAL_IDX wybiera recepture w stacji: 0 = pierwsza (wymog poziomu 0/1),
  // 'ost' = ostatnia widoczna w GUI (zwykle wymog poziomu 45). Dzieki temu
  // testujemy takze gorny koniec drabinki, nie tylko najtansza pozycje.
  const widoczne = st.receptury.filter(x => x.slot !== null)
  const idxRaw = process.env.EVAL_IDX || '0'
  const idx = idxRaw === 'ost' ? widoczne.length - 1 : Math.min(+idxRaw, widoczne.length - 1)
  const rec = widoczne[idx]
  const P = `${st.cmd}/${rec.klucz}`
  console.log(`\n---------- ${P} ----------`)

  await zamknij(bot)

  // 1. poziom profesji.
  //    'poziom'  - 0, ale uwaga: MMOCore moze miec podloge na 1, wiec to slaby sabotaz
  //    'warunek' - 1: mocny sabotaz dla receptury wymagajacej poziomu 45.
  //                Jesli mimo to da sie wytworzyc, warunek profession{} jest martwy.
  const lvl = SABOTAZ === 'poziom' ? 0 : SABOTAZ === 'warunek' ? 1 : POZIOM
  await cmd(bot, `mmocore admin level set ${NICK} ${rec.profesja} ${lvl}`, 900)

  // 2. czysty plecak - inaczej nie odroznimy wyrobu od resztek
  await cmd(bot, `minecraft:clear ${NICK}`, 900)

  // 3. skladniki (z zapasem x2, zeby brak 1 sztuki nie przebral sie za brak receptury)
  if (SABOTAZ !== 'skladniki') {
    for (const s of rec.skladniki) {
      await cmd(bot, `mi give ${s.typ} ${s.id} ${NICK} ${s.ilosc * 2}`, 900)
    }
  }
  await sleep(800)

  const skladnikiOk = SABOTAZ === 'skladniki' ? true : rec.skladniki.every(s => countLabel(bot, s.nazwa) >= s.ilosc)
  if (!r.assert(skladnikiOk, `${P}: skladniki w plecaku`,
      rec.skladniki.map(s => `${s.nazwa}: ${countLabel(bot, s.nazwa)}/${s.ilosc}`).join(', ') + ' || ' + invList(bot))) {
    return   // bez skladnikow dalsze asercje nic nie znacza
  }

  const xpPrzed = await odczytXp(bot, rec.profesja)
  if (!r.assert(Number.isFinite(xpPrzed), `${P}: odczyt XP dziala`, `zwrocono ${xpPrzed}`)) return

  // 4. otwarcie warsztatu komenda
  bot.chat('/' + st.cmd)
  const win = await H.waitForWindow(bot, { timeout: 9000 })
  if (!r.assert(!!win, `${P}: warsztat otwiera sie komenda /${st.cmd}`,
      bot.evalLog.slice(-2).join(' / '))) return
  const tytul = H.windowTitle(win)
  r.assert(tytul.includes(st.gui.replace(/\(.*\)/, '').trim()) || tytul.includes('Warsztat'),
    `${P}: tytul okna`, `"${tytul}" (oczekiwano "${st.gui}")`)

  const przed = zrzutOkna(win)
  console.log(`  okno PRZED: ${przed}`)

  // 5. czy receptura naprawde siedzi w slocie i nie jest "Brak receptury"
  const slotItem = win.slots[rec.slot]
  const slotLabel = slotItem ? (label(slotItem) || '') : ''
  const dostepna = !!slotItem && !/Brak receptury/i.test(slotLabel)
  r.assert(dostepna, `${P}: receptura w slocie ${rec.slot}`, `"${slotLabel}"`)
  if (!dostepna) { await zamknij(bot); return }

  // 6. klik
  await bot.clickWindow(rec.slot, 0, 0)
  await sleep(1000 * (rec.czas || 1) + 3000)

  const po = bot.currentWindow ? zrzutOkna(bot.currentWindow) : '(okno zamkniete)'
  console.log(`  okno PO   : ${po}`)

  // 7. jesli wyrob wyladowal w kolejce - sprobuj go odebrac (klik w kazdy nowy slot)
  let odebranoZKolejki = false
  if (bot.currentWindow && po !== przed) {
    const przedMap = new Map(H.windowSlots(win).map(s => [s.slot, s.label]))
    for (const s of H.windowSlots(bot.currentWindow)) {
      if (przedMap.get(s.slot) === s.label) continue
      if (st.recipeSlots && st.recipeSlots.includes(s.slot)) continue
      console.log(`  nowy/zmieniony slot ${s.slot}: "${s.label}" -> klikam (proba odbioru)`)
      await bot.clickWindow(s.slot, 0, 0); await sleep(1200); odebranoZKolejki = true
    }
  }

  await zamknij(bot)
  await sleep(1200)

  // 8. DOWOD: wyrob w plecaku
  const ile = countLabel(bot, rec.outNazwa)
  r.assert(ile > 0, `${P}: WYROB "${rec.outNazwa}" w plecaku`,
    `sztuk=${ile}; plecak: ${invList(bot).slice(0, 220)}${odebranoZKolejki ? ' [probowano odbioru z kolejki]' : ''}`)

  // 9. DOWOD: XP profesji
  const xpPo = await odczytXp(bot, rec.profesja)
  r.assert(Number.isFinite(xpPo) && xpPo > xpPrzed, `${P}: XP ${rec.profesja} wzroslo`,
    `${xpPrzed} -> ${xpPo} EXP (receptura obiecuje +${rec.xp})`)
  if (Number.isFinite(xpPo) && xpPo > xpPrzed) {
    r.assert(Math.abs((xpPo - xpPrzed) - rec.xp) < 0.001, `${P}: XP zgadza sie z trigger exp{amount=${rec.xp}}`,
      `przyrost ${xpPo - xpPrzed}`)
  }
}

;(async () => {
  const r = new R()
  if (SABOTAZ) console.log(`### TRYB SABOTAZU: ${SABOTAZ} — te przypadki MAJA oblac ###`)
  const bot = await H.connect(NICK, { timeout: 90000 })
  try {
    // Restart serwera w trakcie przebiegu to NIE jest blad produktu - musimy go
    // odroznic od prawdziwej porazki, inaczej raport klamie.
    let rozlaczony = null
    bot.on('end', re => { rozlaczony = 'polaczenie zamkniete: ' + re })
    bot.on('kicked', re => { rozlaczony = 'kick: ' + JSON.stringify(re).slice(0, 120) })
    try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) }
    catch (e) { console.log('(kreator: ' + e.message + ')') }
    await sleep(3000)
    bot.chat(`/minecraft:execute in minecraft:overworld run tp ${NICK} 150.5 99 197.5`)
    await sleep(2500)
    // Przy spawnie chodza prawdziwe moby. Bot, ktory zginie w polowie przebiegu,
    // gubi plecak i produkuje FALSZYWE FAIL-e ("brak wyrobu"), ktore nie maja nic
    // wspolnego z warsztatem. Stad nietykalnosc na czas testu craftingu.
    bot.chat(`/minecraft:effect give ${NICK} minecraft:resistance 3600 4 true`); await sleep(500)
    bot.chat(`/minecraft:effect give ${NICK} minecraft:regeneration 3600 4 true`); await sleep(500)
    bot.chat(`/minecraft:effect give ${NICK} minecraft:fire_resistance 3600 0 true`); await sleep(500)
    bot.chat(`/minecraft:effect give ${NICK} minecraft:saturation 3600 4 true`); await sleep(500)
    console.log('pozycja bota: ' + (bot.entity && bot.entity.position))

    const tylko = (process.env.EVAL_ONLY || '').toLowerCase()
    for (const st of STACJE) {
      if (tylko && !tylko.split(',').some(t => st.cmd.toLowerCase().includes(t.trim()))) continue
      if (rozlaczony) { console.log(`PRZERWANO na ${st.cmd}: ${rozlaczony}`); break }
      try { await testStacji(bot, r, st) }
      catch (e) { r.assert(false, `${st.cmd}: przypadek przerwany wyjatkiem`, e.message) }
    }
  } finally { await H.quit(bot) }
  process.exit(r.print() ? 1 : 0)
})().catch(e => { console.log('FAIL przerwane — ' + e.message); process.exit(2) })
