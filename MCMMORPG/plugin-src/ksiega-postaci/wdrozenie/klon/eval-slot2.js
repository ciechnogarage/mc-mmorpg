'use strict'
const H = require('/e/lib/harness'); const { rcon } = require('/k/rcon.js')
module.exports = async function (r) {
  const nick = 'EvS2' + Date.now() % 10000
  const ok = async (slot, pred) => (await rcon(`execute if items entity ${nick} ${slot} ${pred}`)).includes('passed')
  const id = x => `*[custom_data~{MMOITEMS_ITEM_ID:"${x}"}]`
  const gdzie = async x => { const o = []; for (let k = 0; k < 36; k++) if (await ok('container.' + k, id(x))) o.push(k); if (await ok('weapon.offhand', id(x))) o.push('off'); if (await ok('player.cursor', id(x))) o.push('kursor'); return o.join(',') || '-' }
  const daj = async (x, cel) => { await rcon(`mi give SWORD ${x} ${nick} 1`); await H.sleep(700); for (let k = 0; k < 36; k++) if (await ok('container.' + k, id(x))) { if ('container.' + k !== cel) { await rcon(`minecraft:item replace entity ${nick} ${cel} from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`) } break } await H.sleep(500) }
  let bot = await H.connect(nick); bot.on('messagestr', m => { if (!m.includes('❤')) console.log('MSG', m) }); bot.on('actionBar', m => console.log('PASEK', m.toString())); await H.sleep(3500)
  await rcon(`mmocore admin attribute give ${nick} bieglosc-miecz 1`); await rcon(`mmocore admin level set ${nick} main 30`)
  bot.setQuickBarSlot(0); await H.sleep(300)
  await daj('PRZ_WOJ_MIECZ_1', 'container.0'); await daj('PRZ_WOJ_MIECZ_2', 'container.20')
  // A: pusta lewa, miecz 2 z plecaka na slot 2 paska (okno 37)
  await bot.clickWindow(20, 0, 0); await H.sleep(400); await bot.clickWindow(37, 0, 0); await H.sleep(1500)
  console.log('A M2@', await gdzie('PRZ_WOJ_MIECZ_2'))
  r.assert(await ok('weapon.offhand', id('PRZ_WOJ_MIECZ_2')), 'A pusta lewa: upuszczenie na slot 2 zaklada bron w lewa', await gdzie('PRZ_WOJ_MIECZ_2'))
  // B: zajeta lewa, miecz KAMIENNY z plecaka na slot 2
  await rcon(`mi give SWORD MIECZ_KAMIENNY ${nick} 1`); await H.sleep(700)
  let k0 = -1; for (const k of [5,6,7,9,10,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35]) if (await ok('container.' + k, id('MIECZ_KAMIENNY'))) { k0 = k; break }
  console.log('B kamienny w', k0)
  await bot.clickWindow(k0 < 9 ? k0 + 36 : k0, 0, 0); await H.sleep(500); await bot.clickWindow(37, 0, 0); await H.sleep(1500)
  console.log('B KAM@', await gdzie('MIECZ_KAMIENNY'), 'M2@', await gdzie('PRZ_WOJ_MIECZ_2'))
  r.assert(await ok('weapon.offhand', id('MIECZ_KAMIENNY')), 'B zajeta lewa: upuszczenie na slot 2 zamienia bron w lewej', await gdzie('MIECZ_KAMIENNY'))
  r.assert(await ok('player.cursor', id('PRZ_WOJ_MIECZ_2')), 'B stara lewa trafia na kursor', await gdzie('PRZ_WOJ_MIECZ_2'))
  // C: ta sama zamiana bezposrednio na off-hand (okno 45)
  await bot.clickWindow(45, 0, 0); await H.sleep(1500)
  console.log('C KAM@', await gdzie('MIECZ_KAMIENNY'), 'M2@', await gdzie('PRZ_WOJ_MIECZ_2'))
  await H.quit(bot)
}
