'use strict'
// Test automatycznego paska umiejętności (CoreTools klasa_pasek_*). Tylko klon.
const mineflayer = require('/g/node_modules/mineflayer'); const { attach } = require('/e/lib/resourcepack.js'); const { rcon } = require('/s/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
let pass = 0, fail = 0
const ok = (w, opis, v) => { w ? pass++ : fail++; console.log((w ? 'PASS ' : 'FAIL ') + opis + ' — ' + v) }
const pasek = async n => (await rcon(`papi parse ${n} %mmocore_bound_1%|%mmocore_bound_2%|%mmocore_bound_3%|%mmocore_bound_4%|%mmocore_bound_5%`)).trim()
const wejdz = async (nick, rasa, arch) => { const bot = mineflayer.createBot({ host: process.env.MC_HOST, username: nick, auth: 'offline', version: '1.21.11' }); attach(bot._client); await new Promise(r => bot.once('spawn', r)); await sleep(5000); await rcon(`cc testpostac ${nick} ${rasa} ${arch}`); await sleep(14000); return bot }
;(async () => {
  let b = await wejdz('PasekW', 'ork', 'wojownik')
  let p = await pasek('PasekW'); ok(p.startsWith('Deep Wound|No Skill Bound'), 'nowy wojownik poz.1: Deep Wound w slocie 1', p)
  await rcon('mmocore admin slot bind PasekW 2 COMBO_ATTACK'); await sleep(500)  // wybór gracza
  for (let l = 2; l <= 5; l++) { await rcon(`mmocore admin level set PasekW main ${l}`); await sleep(2500) }
  p = await pasek('PasekW'); ok(p.split('|')[1] === 'Combo Attack', 'wybór gracza w slocie 2 nietknięty', p)
  ok(p.includes('Weaken') && p.includes('Empowered Attack'), 'poz.5: doszły Weaken i Empowered Attack w wolne sloty', p)
  ok(p.split('|').filter(x => x === 'Deep Wound').length === 1, 'bez duplikatów', p)
  b.quit(); await sleep(1500)
  b = await wejdz('PasekA', 'elf', 'akolita')
  p = await pasek('PasekA'); ok(p.startsWith('No Skill Bound|No Skill Bound|No Skill Bound|No Skill Bound'), 'akolita poz.1: pusto (pierwszy skill od 3)', p)
  for (let l = 2; l <= 3; l++) { await rcon(`mmocore admin level set PasekA main ${l}`); await sleep(2500) }
  p = await pasek('PasekA'); ok(p.startsWith('Minor Healings|'), 'akolita poz.3: Minor Healings (op >= działa)', p)
  b.quit(); await sleep(1500)
  b = await wejdz('PasekM', 'czlowiek', 'mag')
  for (let l = 2; l <= 10; l++) { await rcon(`mmocore admin level set PasekM main ${l}`); await sleep(2500) }
  p = await pasek('PasekM'); ok(p.split('|').slice(0, 4).every(x => x !== 'No Skill Bound'), 'mag poz.10: 4 sloty pełne', p)
  console.log(`\nWYNIK: ${pass} PASS, ${fail} FAIL`); b.quit(); await sleep(300); process.exit(fail ? 1 : 0)
})().catch(e => { console.error('BLAD', e); process.exit(2) })
