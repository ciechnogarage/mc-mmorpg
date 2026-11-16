'use strict'
// Etap 2 end-to-end (klon): 4 rasy akolity z generatora — Dusze, pasek, Wezwanie (sługa rasy), Dusza ze śmierci, Żniwo.
const mineflayer = require('/g/node_modules/mineflayer'); const { attach } = require('/e/lib/resourcepack.js'); const { rcon } = require('/s/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
let pass = 0, fail = 0
const ok = (w, opis, v) => { w ? pass++ : fail++; console.log((w ? 'PASS ' : 'FAIL ') + opis + ' — ' + v) }
const TYPY = { nieumarly: 'skeleton', ork: 'zombified_piglin', elf: 'wolf', czlowiek: 'iron_golem' }
const F = bot => bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 })
const ile = async sel => { const r = (await rcon(`execute if entity @e[${sel},x=286,y=-63,z=286,dx=28,dy=12,dz=28]`)).trim(); const m = /Count: (\d+)/.exec(r); return m ? +m[1] : 0 }
;(async () => {
  await rcon('forceload add 280 280 320 320'); await rcon('execute in minecraft:overworld run place template przeciwnicy:arena_v1 286 -61 286')
  for (const [i, rasa] of ['nieumarly', 'ork', 'elf', 'czlowiek'].entries()) {
    const nick = 'E2' + rasa.slice(0, 5) + i; const V = async p => (await rcon(`papi parse ${nick} ${p}`)).trim()
    await rcon('minecraft:kill @e[type=!player,x=286,y=-63,z=286,dx=28,dy=12,dz=28]')
    const bot = mineflayer.createBot({ host: process.env.MC_HOST, username: nick, auth: 'offline', version: '1.21.11' }); attach(bot._client)
    await new Promise(r => bot.once('spawn', r)); await sleep(5000)
    await rcon(`cc testpostac ${nick} ${rasa} akolita`); await sleep(16000)
    const zasob = `${await V('%mmocore_mana_name%')} ${await V('%mmocore_mana%')}/${await V('%mythiclib_stat_max_mana%')}`
    ok(/^Dusze 10(\.0)?\/10(\.0)?$/.test(zasob), `${rasa}: zasób Dusze 10/10`, zasob)
    const p1 = await V('%mmocore_bound_1%'); ok(/Wezwanie/.test(p1), `${rasa}: Wezwanie samo na pasku (slot 1)`, p1)
    ok((await rcon(`execute if entity @a[name=${nick},tag=arch_ako]`)).includes('passed'), `${rasa}: tag arch_ako`, '')
    await rcon(`minecraft:tp ${nick} 300.5 -59 296.5 0 0`); await sleep(1500)
    const bron = bot.quickBarSlot; const wolne = [...Array(9).keys()].filter(s => s !== bron)
    const rzuc = async k => { F(bot); await sleep(300); bot.setQuickBarSlot(wolne[k]); await sleep(300); F(bot); await sleep(300); if (bot.quickBarSlot !== bron) bot.setQuickBarSlot(bron) }
    const typ = TYPY[rasa]; const przed = await ile(`type=${typ}`)
    await rzuc(0); await sleep(2500)
    ok(await ile(`type=${typ}`) === przed + 1, `${rasa}: Wezwanie przywołało ${typ}`, `${przed} -> ${await ile(`type=${typ}`)}`)
    await rcon(`mmocore admin resource-mana set ${nick} 5`)
    await rcon('mm mobs spawn GAJ_WILK 1 world,300.5,-59,303.5'); await sleep(1000)
    const wilk = bot.nearestEntity(e => e.name === 'wolf' && e.position.z > 301); if (wilk) { await bot.lookAt(wilk.position.offset(0, .4, 0), true); bot.attack(wilk) }
    const t0 = Date.now(); while (Date.now() - t0 < 30000 && (await ile('type=wolf,name="Wilk Gajowy"')) > 0) { const w = bot.nearestEntity(e => e.name === 'wolf' && e.position.z > 301); if (w) try { bot.attack(w) } catch {} ; await sleep(900) }
    const d = +(await V('%mmocore_mana%')); ok(d >= 6, `${rasa}: Dusza ze śmierci wilka (5 -> ${d})`, `wilk ${(await ile('type=wolf,name="Wilk Gajowy"')) ? 'żyje' : 'padł'} po ${((Date.now() - t0) / 1000).toFixed(0)} s`)
    for (let l = 2; l <= 3; l++) { await rcon(`mmocore admin level set ${nick} main ${l}`); await sleep(2500) }
    const pas = `${await V('%mmocore_bound_1%')}|${await V('%mmocore_bound_2%')}|${await V('%mmocore_bound_3%')}`; ok(/Żniwo/.test(pas), `${rasa}: Żniwo na pasku po 3. poz.`, pas)
    const k = pas.split('|').findIndex(x => /Żniwo/.test(x)); const s0 = await ile(`type=${typ}`), d0 = +(await V('%mmocore_mana%'))
    if (k >= 0) { await rzuc(k); await sleep(2000) }
    ok(await ile(`type=${typ}`) < s0 || s0 === 0, `${rasa}: Żniwo zabrało sługi`, `${s0} -> ${await ile(`type=${typ}`)}, Dusze ${d0} -> ${await V('%mmocore_mana%')}`)
    bot.quit(); await sleep(2000)
  }
  console.log(`\nWYNIK: ${pass} PASS, ${fail} FAIL`); process.exit(fail ? 1 : 0)
})().catch(e => { console.error('BLAD', e); process.exit(2) })
