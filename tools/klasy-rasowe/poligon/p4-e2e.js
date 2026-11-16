'use strict'
// Partia 4: 5 sygnatur + akolita (Opieka, Rozkaz) na klonie. Cel = husk 200 HP (NoAI) — wilk 8 HP ginął od razu.
const mineflayer = require('/g/node_modules/mineflayer'); const { attach } = require('/e/lib/resourcepack.js'); const { rcon } = require('/s/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
let pass = 0, fail = 0
const ok = (w, opis, v) => { w ? pass++ : fail++; console.log((w ? 'PASS ' : 'FAIL ') + opis + ' — ' + v) }
const num = s => { const m = /(-?[\d.]+)[a-z]?\s*$/.exec(String(s).trim()); return m ? +m[1] : NaN }
const F = bot => bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 })
const CEL = '@e[name="Korzeniak",limit=1]'  // cel = mob MythicMobs (czary MythicLib nie ranią waniliowego huska z /summon)
const cel = async (z, noai = 1) => { await rcon('minecraft:kill @e[name="Korzeniak"]'); await rcon(`mm mobs spawn GAJ_KORZENIAK 1 world,300.5,-59,${z}`); await sleep(700); if (noai) await rcon(`execute as ${CEL} run data merge entity @s {NoAI:1b}`); await sleep(300) }
const hp = async () => num(await rcon(`data get entity ${CEL} Health`)); const zc = async () => num(await rcon(`data get entity ${CEL} Pos[2]`))
async function postac (nick, rasa, arch, poz) {
  const bot = mineflayer.createBot({ host: process.env.MC_HOST, username: nick, auth: 'offline', version: '1.21.11' }); attach(bot._client)
  bot.czat = []; bot.on('messagestr', m => bot.czat.push(m))
  await new Promise(r => bot.once('spawn', r)); await sleep(5000); await rcon(`cc testpostac ${nick} ${rasa} ${arch}`); await sleep(15000)
  for (let l = 2; l <= poz; l++) { await rcon(`mmocore admin level set ${nick} main ${l}`); await sleep(1500) }
  await sleep(2000); bot.pas = []; for (let s = 1; s <= 5; s++) bot.pas.push((await rcon(`papi parse ${nick} %mmocore_bound_${s}%`)).trim())
  bot.bron = bot.quickBarSlot; bot.wolne = [...Array(9).keys()].filter(s => s !== bot.bron)
  bot.rzuc = async nazwa => { const k = bot.pas.findIndex(p => p.includes(nazwa)); if (k < 0) return false; F(bot); await sleep(300); bot.setQuickBarSlot(bot.wolne[k]); await sleep(300); F(bot); await sleep(300); if (bot.quickBarSlot !== bot.bron) bot.setQuickBarSlot(bot.bron); return true }
  bot.celuj = async () => { await sleep(300); const c = bot.nearestEntity(e => e.name === 'zombie'); if (c) await bot.lookAt(c.position.offset(0, 1.2, 0), true) }
  bot.na = async () => { await rcon(`minecraft:tp ${nick} 300.5 -59 300.5 0 0`); await sleep(900) }
  bot.zasob = async v => { await rcon(`mmocore admin resource-mana set ${nick} ${v}`); await sleep(200) }
  console.log(nick, (await rcon(`papi parse ${nick} %mmocore_class_id%`)).trim(), bot.pas.join('|')); return bot
}
;(async () => {
  await rcon('forceload add 280 280 320 320'); await rcon('execute in minecraft:overworld run place template przeciwnicy:arena_v1 286 -61 286'); await rcon('minecraft:kill @e[type=!player,x=286,y=-63,z=286,dx=28,dy=12,dz=28]')
  // 1. Upiór — Pętla Grobu przyciąga (cel z AI)
  let b = await postac('Q4upior', 'nieumarly', 'ninja', 7); ok(b.pas[0].includes('Pętla'), 'Upiór: Pętla Grobu w slocie 1', b.pas[0])
  await b.na(); await cel(308.5, 0); await b.celuj(); await b.zasob(100); const z0 = await zc(); b.czat.length = 0; await b.rzuc('Pętla'); await sleep(1500)
  ok((await zc()) < z0 - 1.5, 'Upiór: Pętla przyciąga cel', `${z0} -> ${await zc()}`); ok(b.czat.some(m => /Znami/.test(m)), 'Upiór: Pętla daje Znamię', JSON.stringify(b.czat.filter(m => /Znami/.test(m)).slice(-1)))
  b.quit(); await sleep(2500)
  // 2. Szaman — Oko Burzy rani cyklicznie
  b = await postac('Q4szaman', 'ork', 'mag', 7); ok(b.pas[0].includes('Oko'), 'Szaman: Oko Burzy w slocie 1', b.pas[0])
  await b.na(); await cel(306.5); await b.celuj(); await b.zasob(200); const s0 = await hp(); await b.rzuc('Oko'); await sleep(1200); const s1 = await hp(); await sleep(3000); const s2 = await hp()
  ok(s1 < s0, 'Szaman: Oko Burzy trafia', `${s0} -> ${s1}`); ok(s2 < s1, 'Szaman: punkt burzy rani dalej', `${s1} -> ${s2}`)
  b.quit(); await sleep(2500)
  // 3. Magister — Pieczęć + Wybuch mocniejszy niż sam Wybuch
  b = await postac('Q4magister', 'czlowiek', 'mag', 7); ok(b.pas[0].includes('Pieczęć'), 'Magister: Pieczęć Wieży w slocie 1', b.pas[0])
  await b.na(); await cel(305.5); await b.celuj(); await b.zasob(200); let h0 = await hp(); await b.rzuc('Wybuch'); await sleep(2000); const bez = h0 - await hp()
  await sleep(6500); await cel(305.5); await b.celuj(); await b.zasob(200); await b.rzuc('Pieczęć'); await sleep(1200); h0 = await hp(); await b.rzuc('Wybuch'); await sleep(2000); const z = h0 - await hp()
  ok(z > bez * 1.3, 'Magister: Wybuch na pieczęci mocniejszy', `bez ${bez.toFixed(1)} vs z pieczęcią ${z.toFixed(1)}`)
  b.quit(); await sleep(2500)
  // 4. Klinga Gaju — Przelot kończy za celem, +15 Szału
  b = await postac('Q4klinga', 'elf', 'wojownik', 7); ok(b.pas[0].includes('Przelot'), 'Klinga Gaju: Przelot Gaju w slocie 1', b.pas[0])
  await b.na(); await cel(305.5); await b.celuj(); await b.zasob(0); const p0 = await hp(); await b.rzuc('Przelot'); await sleep(1300)
  ok(b.entity.position.z > 305.6, 'Klinga: kończy za celem', b.entity.position.z.toFixed(1)); ok(num(await rcon('papi parse Q4klinga %mmocore_mana%')) >= 14, 'Klinga: +15 Szału', await rcon('papi parse P4klinga %mmocore_mana%')); ok((await hp()) < p0, 'Klinga: Przelot rani', `${p0} -> ${await hp()}`)
  b.quit(); await sleep(2500)
  // 5. Nosiciel Relikwii — Straż + Opieka + Rozkaz
  b = await postac('Q4nosiciel', 'czlowiek', 'akolita', 7); ok(b.pas[0].includes('Straż'), 'Nosiciel: Straż Relikwii w slocie 1', b.pas[0])
  ok(['Żniwo', 'Opieka', 'Rozkaz'].every(n => b.pas.some(p => p.includes(n))), 'Nosiciel: Żniwo, Opieka, Rozkaz na pasku', b.pas.join('|'))
  await b.na(); await b.zasob(10); const g0 = num((await rcon('execute if entity @e[type=iron_golem]')).replace(/\D+(\d+)$/, '$1')) || 0
  await b.rzuc('Straż'); await sleep(2000); const g1 = num((await rcon('execute if entity @e[type=iron_golem]')).replace(/\D+(\d+)$/, '$1')) || 0
  ok(g1 > g0, 'Nosiciel: Straż przywołuje obrońcę', `${g0} -> ${g1}`)
  await rcon('mmocore admin resource-health set Q4nosiciel 8'); await sleep(1200); const o0 = b.health; await b.zasob(10); await b.rzuc('Opieka'); await sleep(1500)
  ok(b.health > o0, 'Nosiciel: Opieka leczy', `${o0} -> ${b.health}`)
  await cel(307.5); await b.celuj(); const r0 = await hp(); await b.rzuc('Rozkaz'); await sleep(6000)
  ok((await hp()) < r0, 'Nosiciel: Rozkaz — sługa atakuje wskazany cel', `${r0} -> ${await hp()}`)
  b.quit(); await sleep(1500)
  // 6. regresja: obszarowe na mobie ZOMBIE (Korzeniak) — Wybuch elfiego maga, Rozpłatanie orka
  b = await postac('Q4tkacz', 'elf', 'mag', 7); await b.na(); await cel(305.5); await b.celuj(); await b.zasob(200); let w0 = await hp(); await b.rzuc('Wybuch'); await sleep(2000)
  ok((await hp()) < w0, 'Tkacz Arkanów: Wybuch rani Korzeniaka', `${w0} -> ${await hp()}`); b.quit(); await sleep(2500)
  b = await postac('Q4wodz', 'ork', 'wojownik', 7); await b.na(); await cel(302.5); await b.celuj(); await b.zasob(60); w0 = await hp(); await b.rzuc('Rozpłatanie'); await sleep(1200)
  ok((await hp()) < w0, 'Wódz Wojny: Rozpłatanie rani Korzeniaka', `${w0} -> ${await hp()}`); b.quit(); await sleep(1500)
  console.log(`\nWYNIK: ${pass} PASS, ${fail} FAIL`); process.exit(fail ? 1 : 0)
})().catch(e => { console.error('BLAD', e); process.exit(2) })
