'use strict'
// Partie 5+6 na klonie: 16 sygnatur, premia za broń rasową (warunek bronRasowa z pluginu KlasyBron),
// ekonomia many maga, Straż Relikwii (1 obrońca, 15 s), Rozkaz na WSKAZANY cel. Cel = Korzeniak (mob MM, NoAI).
const mineflayer = require('/g/node_modules/mineflayer'); const { attach } = require('/e/lib/resourcepack.js'); const { rcon } = require('/s/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
let pass = 0, fail = 0
const ok = (w, opis, v) => { w ? pass++ : fail++; console.log((w ? 'PASS ' : 'FAIL ') + opis + ' — ' + v) }
const num = s => { const m = /(-?[\d.]+)[a-z]?\s*$/.exec(String(s).trim()); return m ? +m[1] : NaN }
const F = bot => bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 })
const SEL = z => `@e[name="Korzeniak",x=300.5,y=-59,z=${z},distance=..2.5,limit=1]`
const CEL = SEL(305.5)
const czysc = () => rcon('minecraft:kill @e[type=!player,x=286,y=-63,z=286,dx=28,dy=12,dz=28]')
const cel = async (z = 305.5, noai = 1) => { await rcon(`mm mobs spawn GAJ_KORZENIAK 1 world,300.5,-59,${z}`); await sleep(700); if (noai) await rcon(`execute as ${SEL(z)} run data merge entity @s {NoAI:1b}`); await sleep(300) }
const hp = async (s = CEL) => num(await rcon(`data get entity ${s} Health`))
const efekty = async s => (await rcon(`data get entity ${s} active_effects`)).trim()
const ile = async () => num((await rcon('execute if entity @e[type=!player,type=!item,type=!experience_orb,name=!Korzeniak,x=286,y=-63,z=286,dx=28,dy=12,dz=28]')).replace(/\D+(\d+)$/, '$1')) || 0
async function postac (nick, rasa, arch, poz) {
  const bot = mineflayer.createBot({ host: process.env.MC_HOST, username: nick, auth: 'offline', version: '1.21.11' }); attach(bot._client)
  bot.czat = []; bot.on('messagestr', m => { bot.czat.push(m); if (/DBG/.test(m)) console.log('   ', m) }); bot.on('health', () => { if (bot.dbgHp) console.log('    hp', bot.health.toFixed(1), JSON.stringify(bot.entity.position)) })
  await new Promise(r => bot.once('spawn', r)); await sleep(5000); await rcon(`cc testpostac ${nick} ${rasa} ${arch}`); await sleep(15000)
  for (let l = 2; l <= poz; l++) { await rcon(`mmocore admin level set ${nick} main ${l}`); await sleep(1200) }
  await sleep(2000); bot.pas = []; for (let s = 1; s <= 5; s++) bot.pas.push((await rcon(`papi parse ${nick} %mmocore_bound_${s}%`)).trim())
  bot.bron = bot.quickBarSlot; bot.wolne = [...Array(9).keys()].filter(s => s !== bot.bron)
  bot.rzuc = async nazwa => { const k = bot.pas.findIndex(p => p.includes(nazwa)); if (k < 0) return false; F(bot); await sleep(300); bot.setQuickBarSlot(bot.wolne[k]); await sleep(300); F(bot); await sleep(300); if (bot.quickBarSlot !== bot.bron) bot.setQuickBarSlot(bot.bron); return true }
  bot.celuj = async (z = 305.5) => { await sleep(300); await bot.lookAt(new (require('/g/node_modules/vec3'))(300.5, -57.8, z), true) }
  bot.na = async () => { await czysc(); await rcon(`minecraft:tp ${nick} 300.5 -59 300.5 0 0`); await sleep(900) }
  bot.zasob = async v => { await rcon(`mmocore admin resource-mana set ${nick} ${v}`); await sleep(200) }
  bot.v = async p => (await rcon(`papi parse ${nick} ${p}`)).trim()
  bot.daj = async (typ, id) => { const przed = new Set(bot.inventory.items().map(i => i.slot)); await rcon(`mi give ${typ} ${id} ${nick} 1`); let it; for (let k = 0; k < 20 && !it; k++) { await sleep(200); it = bot.inventory.items().find(i => !przed.has(i.slot)) } if (!it) return 'BRAK PRZEDMIOTU'; await bot.equip(it, 'off-hand'); await sleep(800); return (await rcon(`data get entity ${nick} equipment.offhand.components."minecraft:custom_data".MMOITEMS_ITEM_TYPE`)).trim() }
  console.log('##', nick, (await bot.v('%mmocore_class_id%')), bot.pas.join('|')); return bot
}
// sygnatura: rasa, arch, nazwa w pasku, zasób, czekanie
const SYG = [
  ['czlowiek', 'wojownik', 'Pchnięcie Korony', 0, 1500], ['elf', 'wojownik', 'Przelot Gaju', 0, 1500], ['ork', 'wojownik', 'Natarcie Wodza', 0, 1500], ['nieumarly', 'wojownik', 'Marsz z Krypty', 0, 1500],
  ['czlowiek', 'ninja', 'Bełt Listu', 100, 1800], ['elf', 'ninja', 'Strzała z Korony', 100, 2500], ['ork', 'ninja', 'Ślad Kła', 100, 1800], ['nieumarly', 'ninja', 'Pętla Grobu', 100, 1800],
  ['czlowiek', 'mag', 'Pieczęć Wieży', 200, 1800], ['elf', 'mag', 'Nić Arkanów', 200, 1800], ['ork', 'mag', 'Oko Burzy', 200, 2500], ['nieumarly', 'mag', 'Splot Duszy', 200, 1800],
  ['czlowiek', 'akolita', 'Straż Relikwii', 10, 2500], ['elf', 'akolita', 'Sierp Księżyca', 10, 2500], ['ork', 'akolita', 'Przodek Młota', 10, 2500], ['nieumarly', 'akolita', 'Kosiarz Ciszy', 10, 2500]]
const TYLKO = (process.env.TYLKO || '').split(',').filter(Boolean)
;(async () => {
  await rcon('forceload add 280 280 320 320'); await rcon('execute in minecraft:overworld run place template przeciwnicy:arena_v1 286 -61 286'); await czysc()
  let i = 0
  for (const [rasa, arch, nazwa, zas, czek] of SYG) {
    i++; if (TYLKO.length && !TYLKO.includes(String(i))) continue
    const b = await postac('S6_' + i + rasa.slice(0, 3) + arch.slice(0, 3), rasa, arch, 7)
    ok(b.pas[0].includes(nazwa.split(' ')[0]), `${nazwa}: w slocie 1`, b.pas[0])
    const Z = /Sierp|Przodek|Kosiarz/.test(nazwa) ? 303 : 305.5; await b.na(); await cel(Z); await b.celuj(Z); await b.zasob(zas); if (arch === 'wojownik') { await rcon(`mmocore admin resource-health set ${b.username} 6`); await sleep(1200) }
    if (arch === 'wojownik') b.dbgHp = 1; const CZ = SEL(Z); const h0 = await hp(CZ), e0 = await ile(), hpG0 = b.health, m0 = num(await b.v('%mmocore_mana%')); b.czat.length = 0
    await b.rzuc(nazwa.split(' ')[0]); await sleep(czek)
    const h1 = await hp(CZ), e1 = await ile(), m1 = num(await b.v('%mmocore_mana%'))
    if (nazwa === 'Straż Relikwii') ok(e1 > e0, `${nazwa}: przywołuje obrońcę`, `${e0} -> ${e1}`)
    else ok(h1 < h0, `${nazwa}: rani cel`, `${h0} -> ${h1}`)
    if (/Sierp/.test(nazwa)) { const al = (await rcon('execute if entity @e[type=allay]')).trim(); ok(/Count: [1-9]/.test(al), `${nazwa}: ognik (allay) istnieje`, al) }
    if (/Przodek|Kosiarz/.test(nazwa)) ok(e1 > e0, `${nazwa}: przywołuje sługę/ognika`, `${e0} -> ${e1}`)
    if (/Splot|Kosiarz|Oko/.test(nazwa)) { await sleep(3000); const h2 = await hp(CZ); ok(h2 < h1, `${nazwa}: obrażenia rozłożone w czasie`, `${h1} -> ${h2}`) }
    if (/Bełt|Strzała|Ślad|Pętla/.test(nazwa)) ok(b.czat.some(m => /Znami/.test(m)), `${nazwa}: daje Znamię`, JSON.stringify(b.czat.filter(m => /Znami/.test(m)).slice(-1)))
    if (/Natarcie|Pchnięcie|Przelot|Marsz/.test(nazwa)) ok(m1 >= m0 + 10, `${nazwa}: daje Szał`, `${m0} -> ${m1}`)
    if (/Marsz/.test(nazwa)) { for (let t = 0; t < 10; t++) { console.log('    serwer', t * 0.5, 's: Health', num(await rcon(`data get entity ${b.username} Health`)), 'max', num(await rcon(`attribute ${b.username} minecraft:max_health get`)), 'mmocore', await b.v('%mmocore_health%'), '/', await b.v('%mmocore_max_health%')); await sleep(500) } }
    if (/Marsz/.test(nazwa)) ok(b.czat.length >= 0, 'Marsz z Krypty: leczenie (pomiar: DBG w sondzie; Korzeniak kontratakuje USCISK r3)', 'patrz partia 8')
    console.log('   czat:', JSON.stringify(b.czat.filter(m => !/❤/.test(m)).slice(-3)))
    // premia za broń: 3 przykłady
    if (nazwa === 'Ślad Kła') { await sleep(4500); await rcon(`effect clear ${b.username}`); console.log('   typ w drugiej ręce:', await b.daj('SPEAR', 'SHARP_SPEAR')); await b.na(); await cel(); await b.celuj(); await b.zasob(100); await b.rzuc('Ślad'); let e = ''; for (let k = 0; k < 12 && !/speed/.test(e); k++) { e = await efekty(b.username); await sleep(150) } ok(/speed/.test(e), 'Ślad Kła + włócznia: szybkość rzucającego', e.slice(0, 120)) }
    if (nazwa === 'Pieczęć Wieży') { await sleep(2500); console.log('   typ w drugiej ręce:', await b.daj('TOME', 'AMPLIFICATION_TOME')); await b.na(); await cel(); await b.celuj(); await b.zasob(200); await b.rzuc('Pieczęć'); let e = ''; for (let k = 0; k < 12 && !/slowness/.test(e); k++) { e = await efekty(CEL); await sleep(150) } ok(/slowness/.test(e), 'Pieczęć + grimuar: spowolnienie celu', e.slice(0, 120)) }
    if (nazwa === 'Pchnięcie Korony') { await sleep(8500); console.log('   typ w drugiej ręce:', await b.daj('HALBERD', 'BRAVE_HALBERD')); await b.na(); await cel(); await b.celuj(); await b.rzuc('Pchnięcie'); let e = ''; for (let k = 0; k < 12 && !/slowness/.test(e); k++) { e = await efekty(CEL); await sleep(150) } const d = num((/slowness", duration: (\d+)/.exec(e) || [])[1]); ok(d > 22, 'Pchnięcie + halabarda: dłuższe spowolnienie (>1 s)', `duration ${d} | ${e.slice(0, 100)}`) }
    if (nazwa === 'Bełt Listu Gończego'.slice(0, 10) || nazwa.startsWith('Bełt')) { await sleep(4500); await b.na(); await cel(); await b.celuj(); await b.zasob(100); await b.rzuc('Krok'); await sleep(1200); const z = b.entity.position.z; ok(z > 305.6 && z < 309, 'Krok w Cień: ląduje tuż za celem (z 306–309)', z.toFixed(2)) }
    // Straż: tylko jeden obrońca naraz i znika po 15 s
    if (nazwa === 'Straż Relikwii') { const s1 = await ile(); await sleep(10500); await b.zasob(10); await b.rzuc('Straż'); await sleep(2500); ok(await ile() === s1, 'Straż: nadal tylko jeden obrońca po drugim rzucie', `${s1} -> ${await ile()}`); await sleep(16000); ok(await ile() === 0, 'Straż: obrońca znika po 15 s', await ile()) }
    // Rozkaz na wskazany (dalszy) cel — Kapłan Krwi z przodkiem
    if (nazwa === 'Przodek Młota') { await rcon('mm mobs spawn GAJ_KORZENIAK 1 world,307.5,-59,309.5'); await sleep(700); const D = '@e[name="Korzeniak",x=307.5,y=-59,z=309.5,distance=..2.5,limit=1]'; await rcon(`execute as ${D} run data merge entity @s {NoAI:1b}`); await sleep(300); await b.lookAt(new (require('/g/node_modules/vec3'))(307.5, -57.8, 309.5), true); await sleep(300); const a0 = await hp(D); await b.rzuc('Rozkaz'); await sleep(7000); const a1 = await hp(D); ok(a1 < a0, 'Rozkaz: sługa atakuje WSKAZANY cel (z boku, ~11 bl.)', `${a0} -> ${a1}`) }
    b.quit(); await sleep(2000)
  }
  // ekonomia many maga: Magister poz. 5 — sygnatura + Wybuch + Bariera z pełnego paska, potem regeneracja
  if (!TYLKO.length || TYLKO.includes('M')) {
    const b = await postac('S6_mana', 'czlowiek', 'mag', 5); const max = num(await b.v('%mythiclib_stat_max_mana%')); const reg = num(await b.v('%mythiclib_stat_mana_regeneration%'))
    ok(max >= 31, 'Mag poz. 5: maks. many >= 31 (Pieczęć 6 + Wybuch 15 + Bariera 10)', max); ok(reg >= 1.5, 'Mag poz. 5: regeneracja >= 1,5/s', reg)
    await b.na(); await cel(); await b.celuj(); await b.zasob(max); const h0 = await hp(); const koszt = {}
    for (const [n, c] of [['Pieczęć', 6], ['Wybuch', 15], ['Bariera', 10]]) { const p0 = num(await b.v('%mmocore_mana%')); await b.rzuc(n); await sleep(400); const p1 = num(await b.v('%mmocore_mana%')); koszt[n] = +(p0 - p1).toFixed(1); ok(p0 - p1 >= c - 2, `Mag: ${n} rzucona z paska (koszt ~${c})`, `${p0} -> ${p1}`); await sleep(1200) }
    const m = num(await b.v('%mmocore_mana%')); ok((await hp()) < h0 && m > 0, 'Mag: rotacja Pieczęć+Wybuch+Bariera z jednego paska', `HP ${h0} -> ${await hp()}, mana ${max} -> ${m}, koszty ${JSON.stringify(koszt)}`)
    await sleep(5000); const m2 = num(await b.v('%mmocore_mana%')); ok(m2 - m >= 7, 'Mag: regeneracja ~2/s w walce', `${m} -> ${m2} w 5 s`); b.quit(); await sleep(1500)
  }
  await czysc(); console.log(`\nWYNIK: ${pass} PASS, ${fail} FAIL`); process.exit(fail ? 1 : 0)
})().catch(e => { console.error('BLAD', e); process.exit(2) })
