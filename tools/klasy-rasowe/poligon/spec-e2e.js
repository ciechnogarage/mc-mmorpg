'use strict'
// Partia 10: 32 skille specjalizacji (poz. 10, slot 5). Bot na klasę bazową, wybór specjalizacji przez
// `mmocore class select`, rzut slotu 5 w Korzeniaka (NoAI, 4 bl.). PASS gdy skill na pasku i widać skutek:
// HP/efekty celu, osłona/efekty rzucającego albo nowy byt (sługa/strefa).
const mineflayer = require('/g/node_modules/mineflayer'); const { attach } = require('/e/lib/resourcepack.js'); const { rcon } = require('/s/rcon.js')
const Vec3 = require('/g/node_modules/vec3'); const sleep = ms => new Promise(r => setTimeout(r, ms))
let pass = 0, fail = 0; const ok = (w, o, v) => { w ? pass++ : fail++; console.log((w ? 'PASS ' : 'FAIL ') + o + ' — ' + v) }
const num = s => { const m = /(-?[\d.]+)[a-z]?\s*$/.exec(String(s).trim()); return m ? +m[1] : NaN }
const F = bot => bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 })
const Z = 303, CEL = `@e[name="Manekin",x=300.5,y=-59,z=${Z},distance=..2.5,limit=1]`
const czysc = () => rcon('minecraft:kill @e[type=!player,x=286,y=-63,z=286,dx=28,dy=12,dz=28]')
const ile = async () => num((await rcon('execute if entity @e[type=!player,type=!item,type=!experience_orb,name=!Manekin,x=286,y=-63,z=286,dx=28,dy=12,dz=28]')).replace(/\D+(\d+)$/, '$1')) || 0
const KLASY = [['czlowiek','wojownik','BASTION','CZEMPION'],['czlowiek','ninja','WIDMO','JASTRZAB'],['czlowiek','mag','ELEMENTALISTA','PRZYWOLYWACZ'],['czlowiek','akolita','SERAFIN','HIEROFANT'],
  ['elf','wojownik','STRAZNIK_CIERNI','TANCERZ_OSTRZY'],['elf','ninja','CIEN_LISCI','SOKOL'],['elf','mag','TKACZ_ZYWIOLOW','STRAZNIK_ZYWIOLOW'],['elf','akolita','OSTRZE_KSIEZYCA','OPIEKUN_DUCHOW'],
  ['ork','wojownik','TYRAN','KRWIOBURCA'],['ork','ninja','LOWCA_BESTII','LOWCA_KOSCI'],['ork','mag','BURZOWNIK','TOTEMISTA'],['ork','akolita','AWATAR_WOJNY','SZEPT_PRZODKOW'],
  ['nieumarly','wojownik','STRAZNIK_GROBU','ZNIWIARZ'],['nieumarly','ninja','CIEN','STRZELEC_GROBU'],['nieumarly','mag','DUSZOZERCA','NEKROMANTA'],['nieumarly','akolita','AWATAR_OTCHLANI','PASTERZ_UMARLYCH']]
const ZASOB = { wojownik: 100, ninja: 100, mag: 200, akolita: 10 }
const TYLKO = (process.env.TYLKO || '').split(',').filter(Boolean)
;(async () => {
  await rcon('forceload add 280 280 320 320'); await rcon('execute in minecraft:overworld run place template przeciwnicy:arena_v1 286 -61 286'); await czysc()
  let i = 0
  for (const [rasa, arch, ...specy] of KLASY) {
    i++; if (TYLKO.length && !TYLKO.includes(String(i))) continue
    const nick = ('SP' + i + rasa.slice(0, 3) + arch.slice(0, 3)).slice(0, 16)
    const bot = mineflayer.createBot({ host: process.env.MC_HOST, username: nick, auth: 'offline', version: '1.21.11' }); attach(bot._client)
    const czat = []; bot.on('messagestr', m => czat.push(m))
    await new Promise(r => bot.once('spawn', r)); await sleep(5000); await rcon(`cc testpostac ${nick} ${rasa} ${arch}`); await sleep(15000)
    for (let l = 2; l <= 10; l++) { await rcon(`mmocore admin level set ${nick} main ${l}`); await sleep(700) }
    for (const spec of specy) {
      console.log('##', nick, spec, (await rcon(`mmocore class select ${nick} ${spec}`)).trim().slice(0, 80)); await sleep(4000)
      await rcon(`mmocore admin level set ${nick} main 10`); await sleep(2500)
      const pas = []; for (let s = 1; s <= 5; s++) pas.push((await rcon(`papi parse ${nick} %mmocore_bound_${s}%`)).trim())
      const kl = (await rcon(`papi parse ${nick} %mmocore_class_id%`)).trim()
      ok(kl === spec, `${spec}: klasa wybrana`, kl); ok(!/No Skill/.test(pas[4]), `${spec}: skill w slocie 5`, pas.join('|'))
      const bron = bot.quickBarSlot, wolne = [...Array(9).keys()].filter(s => s !== bron)
      await czysc(); await rcon(`minecraft:tp ${nick} 300.5 -59 300.5 0 0`); await sleep(900)
      await rcon(`mm mobs spawn TEST_MANEKIN 1 world,300.5,-59,${Z}`); await sleep(700); await rcon(`execute as ${CEL} run data merge entity @s {NoAI:1b}`)
      await bot.lookAt(new Vec3(300.5, -57.8, Z), true); await rcon(`effect clear ${nick}`); await rcon(`mmocore admin resource-health set ${nick} 15`); await rcon(`mmocore admin resource-mana set ${nick} ${ZASOB[arch]}`); await sleep(1200)
      const st = async () => ({ hp: num(await rcon(`data get entity ${CEL} Health`)), ef: (await rcon(`data get entity ${CEL} active_effects`)).length, abs: num(await rcon(`data get entity ${nick} AbsorptionAmount`)), mef: (await rcon(`data get entity ${nick} active_effects`)).length, n: await ile(), mana: num(await rcon(`papi parse ${nick} %mmocore_mana%`)), oslSlug: num((await rcon('execute if entity @e[type=!player,x=286,y=-63,z=286,dx=28,dy=12,dz=28,nbt=!{AbsorptionAmount:0.0f}]')).replace(/\D+(\d+)$/, '$1')) || 0 })
      if (['HIEROFANT', 'OPIEKUN_DUCHOW', 'AWATAR_WOJNY'].includes(spec)) { F(bot); await sleep(300); bot.setQuickBarSlot(wolne[0]); await sleep(300); F(bot); await sleep(300); if (bot.quickBarSlot !== bron) bot.setQuickBarSlot(bron); await sleep(2500); await rcon(`mmocore admin resource-mana set ${nick} ${ZASOB[arch]}`); await sleep(500) }
      const a = await st(); czat.length = 0
      F(bot); await sleep(300); bot.setQuickBarSlot(wolne[4]); await sleep(300); F(bot); await sleep(300); if (bot.quickBarSlot !== bron) bot.setQuickBarSlot(bron)
      let b = a; for (let k = 0; k < 8; k++) { await sleep(400); b = await st(); if (b.hp < a.hp || b.abs > a.abs || b.n > a.n || b.oslSlug > a.oslSlug) break }
      const zm = []; if (b.hp < a.hp) zm.push(`HP celu ${a.hp}->${b.hp}`); if (b.ef !== a.ef) zm.push('efekty celu'); if (b.abs > a.abs) zm.push(`osłona ${a.abs}->${b.abs}`); if (b.mef !== a.mef) zm.push('efekty rzucającego'); if (b.n > a.n) zm.push(`byty ${a.n}->${b.n}`); if (b.oslSlug > a.oslSlug) zm.push(`osłona sług ${a.oslSlug}->${b.oslSlug}`)
      ok(zm.length > 0, `${spec} (${pas[4]}): widać skutek`, zm.join(', ') || 'brak') ; ok(b.mana < a.mana || arch === 'wojownik', `${spec}: pobrał zasób`, `${a.mana} -> ${b.mana}`)
      console.log('   czat:', JSON.stringify(czat.filter(m => !/❤/.test(m)).slice(-4)))
      await sleep(1500)
    }
    bot.quit(); await sleep(2000)
  }
  await czysc(); console.log(`\nWYNIK: ${pass} PASS, ${fail} FAIL`); process.exit(fail ? 1 : 0)
})().catch(e => { console.error('BLAD', e); process.exit(2) })
