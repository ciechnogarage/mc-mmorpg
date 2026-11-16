'use strict'
// Etap 0 poprawek (2026-09-30): archetyp (zmienna CoreTools) + formuła obrony MythicLib (malejące zwroty)
// + Szał 100. Tylko klon (CC_TEST_WEJSCIE=1). node:20, /e = harness kreatora, /s = katalog z rcon.js.
const mineflayer = require('/e/node_modules/mineflayer'); const { attach } = require('/e/lib/resourcepack.js'); const { rcon } = require('/s/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const num = s => { const m = /(-?[\d.]+)[a-z]?\s*$/.exec(s.trim()); return m ? +m[1] : NaN }
let pass = 0, fail = 0
const ok = (w, opis, v) => { w ? pass++ : fail++; console.log((w ? 'PASS ' : 'FAIL ') + opis + (v !== undefined ? ' — ' + v : '')) }
const hp = async n => num(await rcon(`data get entity ${n} Health`))
const V = async (n, p) => (await rcon(`papi parse ${n} ${p}`)).trim()
async function cios (n, dmg) {
  await rcon(`mmocore admin resource-health set ${n} 1000`); await sleep(1200)
  await rcon(`clear ${n}`); await sleep(300)
  console.log('  pancerz:', num(await rcon(`attribute ${n} minecraft:armor get`)), 'twardosc:', num(await rcon(`attribute ${n} minecraft:armor_toughness get`)))
  console.log('  manekin:', (await rcon('execute if entity @e[tag=manekin]')).trim())
  const przed = await hp(n)
  console.log('  damage ->', (await rcon(`damage ${n} ${dmg} minecraft:mob_attack by @e[tag=manekin,limit=1]`)).trim()); await sleep(600)
  return +(przed - await hp(n)).toFixed(3)
}
;(async () => {
  await rcon('forceload add 143 191 155 203'); await rcon('fill 143 97 191 155 97 203 minecraft:stone')
  await rcon('kill @e[tag=manekin]')
  await rcon('summon minecraft:armor_stand 152 98 197 {NoGravity:1b,Invisible:1b,Tags:["manekin"]}')
  const bot = mineflayer.createBot({ host: process.env.MC_HOST, username: 'Obrona', auth: 'offline', version: '1.21.11' }); attach(bot._client)
  await new Promise(r => bot.once('spawn', r)); await sleep(6000)
  await rcon('cc testpostac Obrona ork wojownik'); await sleep(9000)
  console.log('niewrazliwy po wyjsciu z kreatora?', (await rcon('data get entity Obrona Invulnerable')).trim()); await sleep(Number(process.env.CZEKAJ || 12000))
  ok(await V('Obrona', '%mmocore_class_id%') === 'WODZ_WOJNY', 'testpostac ork/wojownik = WODZ_WOJNY', await V('Obrona', '%mmocore_class_id%'))
  ok(await V('Obrona', '%coretools_var_player_klasa_archetyp%') === 'woj', 'archetyp = woj', await V('Obrona', '%coretools_var_player_klasa_archetyp%'))
  const bez = await cios('Obrona', 4)
  ok(Math.abs(bez - 4) < 0.05, 'bez obrony (Wódz Wojny, defense 0): 4 obrażeń -> 4', bez)
  await rcon('mmocore class select Obrona TYRAN'); await sleep(4000)
  const def = +(await V('Obrona', '%mythiclib_stat_defense%'))
  ok(def === 5, 'Tyran poz. 1: defense 5', def)
  const z = await cios('Obrona', 4)
  const ocz = 4 * 50 / (50 + def)
  ok(Math.abs(z - ocz) < 0.05, `z obroną ${def}: 4 obrażeń -> ${ocz.toFixed(2)} (dawniej 0)`, z)
  ok(await V('Obrona', '%coretools_var_player_klasa_archetyp%') === 'woj', 'specjalizacja TYRAN dziedziczy archetyp woj')
  const maks = await V('Obrona', '%mythiclib_stat_max_mana%'); ok(/^100(\.0)?$/.test(maks), 'Szał Tyrana max 100', maks)
  await rcon('kill @e[tag=manekin]')
  console.log(`\nWYNIK: ${pass} PASS, ${fail} FAIL`)
  bot.quit(); await sleep(500); process.exit(fail ? 1 : 0)
})().catch(e => { console.error('BLAD', e); process.exit(2) })
