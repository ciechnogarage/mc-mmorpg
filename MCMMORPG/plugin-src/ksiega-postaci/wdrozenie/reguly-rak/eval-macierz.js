'use strict'
const H = require('/e/lib/harness'); const { rcon } = require('/k/rcon.js')
const PROBKI = [['SWORD','LONG_SWORD','1r',false,false],['SWORD','FROZEN_BLADE','2r',true,false],['DAGGER','STEEL_DAGGER','1r',false,false],['SPEAR','SHARP_SPEAR','1r',false,false],['HAMMER','SANTAMMER','1r',false,false],['GAUNTLET','FIGHTING_GAUNTLETS','1r',false,false],['WHIP','WIND_WHIP','1r',false,false],['STAFF','PROF_UNAWAKENED_STAFF','1r',false,false],['STAFF','EARTH_STAFF','2r',true,false],['BOW','COMPOSITE_BOW','1r',true,false],['CROSSBOW','MEDIEVAL_CROSSBOW','1r',true,false],['MUSKET','SNIPER_RIFLE','1r',true,false],['MUSKET','MAKESHIFT_MUSKET','2r',true,false],['CATALYST','NATURES_GIFT','1r',false,false],['OFF_CATALYST','SCOPE','1r',false,true],['SHIELD','PALADIN_SHIELD','1r',false,true],['SHIELD','ABSORBING_SHIELD','2r',false,true],['AXE','EXECUTIONER_AXE','1r',false,false],['AXE','BONE_CRACKER','2r',true,false],['LONG_SWORD','DUAL_LONGSWORD','1r',false,false],['KATANA','FLOWING_KATANA','1r',false,false],['THRUSTING_SWORD','SKILLED_SWORD','1r',false,false],['GREATAXE','BARBARIAN_AXE','1r',true,false],['HALBERD','DOUBLE_OR_NOTHING','1r',true,false],['HALBERD','HALABARDA_STRAZNIKA','2r',true,false],['GREATHAMMER','UNRELIABLE_HAMMER','1r',true,false],['GREATHAMMER','MLOT_BOJOWY','2r',true,false],['SIERP','SIERP_KSIEZYCOWY','1r',false,false],['KOSA','KOSA_SPACZONA','2r',true,false],['FILAKTERIUM','FILAKTERIUM_KOSCIANE','1r',false,false],['GREATSWORD','CANDY_CANE_SWORD','1r',true,false],['GREATSWORD','HARDENED_STEEL_CLAYMORE','2r',true,false],['GREATSTAFF','STIFF_GREATSTAFF','1r',true,false],['GREATSTAFF','KOSTUR_ZYWEGO_DREWNA','2r',true,false],['TOME','AMPLIFICATION_TOME','1r',false,false],['TALISMAN','THUNDER_BLESSING','1r',false,false],['WAND','OAK_WAND','1r',false,false],['GREATBOW','LUK_DLUGI_KNIEI','2r',true,false],['LANCE','FIRE_GREATLANCE','2r',true,false]]; const STARE = [['SWORD','LONG_SWORD','1r'],['SWORD','FROZEN_BLADE','2r'],['DAGGER','STEEL_DAGGER','1r'],['SPEAR','SHARP_SPEAR','1r'],['HAMMER','SANTAMMER','1r'],['GAUNTLET','FIGHTING_GAUNTLETS','1r'],['WHIP','WIND_WHIP','1r'],['STAFF','PROF_UNAWAKENED_STAFF','1r'],['STAFF','EARTH_STAFF','2r'],['BOW','COMPOSITE_BOW','1r'],['CROSSBOW','MEDIEVAL_CROSSBOW','1r'],['MUSKET','SNIPER_RIFLE','1r'],['MUSKET','MAKESHIFT_MUSKET','2r'],['CATALYST','NATURES_GIFT','1r'],['OFF_CATALYST','SCOPE','1r'],['SHIELD','PALADIN_SHIELD','1r'],['SHIELD','ABSORBING_SHIELD','2r'],['AXE','EXECUTIONER_AXE','1r'],['AXE','BONE_CRACKER','2r'],['LONG_SWORD','DUAL_LONGSWORD','1r'],['KATANA','FLOWING_KATANA','1r'],['THRUSTING_SWORD','SKILLED_SWORD','1r'],['GREATAXE','BARBARIAN_AXE','1r'],['HALBERD','DOUBLE_OR_NOTHING','1r'],['HALBERD','HALABARDA_STRAZNIKA','2r'],['GREATHAMMER','UNRELIABLE_HAMMER','1r'],['GREATHAMMER','MLOT_BOJOWY','2r'],['SIERP','SIERP_KSIEZYCOWY','1r'],['KOSA','KOSA_SPACZONA','2r'],['FILAKTERIUM','FILAKTERIUM_KOSCIANE','1r'],['GREATSWORD','CANDY_CANE_SWORD','1r'],['GREATSWORD','HARDENED_STEEL_CLAYMORE','2r'],['GREATSTAFF','STIFF_GREATSTAFF','1r'],['GREATSTAFF','KOSTUR_ZYWEGO_DREWNA','2r'],['TOME','AMPLIFICATION_TOME','1r'],['TALISMAN','THUNDER_BLESSING','1r'],['WAND','OAK_WAND','1r'],['GREATBOW','LUK_DLUGI_KNIEI','2r'],['LANCE','FIRE_GREATLANCE','2r']]
module.exports = async function (r) {
  const nick = 'EvM' + Date.now() % 10000
  const ok = async (slot, pred) => (await rcon(`execute if items entity ${nick} ${slot} ${pred}`)).includes('passed')
  const id = x => `*[custom_data~{MMOITEMS_ITEM_ID:"${x}"}]`
  const gdzie = async x => { const o = []; if (await ok('weapon.offhand', id(x))) o.push('LEWA'); if (await ok('container.0', id(x))) o.push('PRAWA'); if (await ok('player.cursor', id(x))) o.push('kursor'); for (let k = 9; k < 36; k++) if (await ok('container.' + k, id(x))) o.push('plecak'); return o.join(',') || 'BRAK' }
  const wPlecak = async (typ, x) => { await rcon(`mi give ${typ} ${x} ${nick} 1`); await H.sleep(500); for (let k = 0; k < 36; k++) if (await ok('container.' + k, id(x))) { if (k !== 27) { await rcon(`minecraft:item replace entity ${nick} container.27 from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`) } return true } return false }
  const bot = await H.connect(nick); await H.sleep(3500)
  await rcon(`mmocore admin level set ${nick} main 100`)
  const wynik = []
  for (const [typ, x, rodzaj, dwu, tylkoLewa] of PROBKI) {
    for (const reka of ['P', 'L']) {
      await rcon(`minecraft:clear ${nick}`); await H.sleep(700); bot.setQuickBarSlot(0); await H.sleep(300)
      // druga reka: sztylet
      await wPlecak('DAGGER', 'STEEL_DAGGER'); await bot.clickWindow(27, 0, 0); await H.sleep(300); await bot.clickWindow(reka === 'P' ? 45 : 36, 0, 0); await H.sleep(700)
      if (!(await wPlecak(typ, x))) { wynik.push(`${typ} ${x} ${rodzaj} ${reka}: BRAK PRZEDMIOTU`); continue }
      await bot.clickWindow(27, 0, 0); await H.sleep(300); await bot.clickWindow(reka === 'P' ? 36 : 45, 0, 0); await H.sleep(900)
      const g = await gdzie(x), s = await gdzie('STEEL_DAGGER')
      if (bot.inventory.selectedItem || true) { const kur = await ok('player.cursor', '*'); if (kur) { await bot.clickWindow(-999, 0, 0); await H.sleep(200) } }
      await bot.closeWindow(bot.inventory); await H.sleep(200)
      const ocz = reka === 'P' ? (tylkoLewa ? 'odrzucony' : 'PRAWA') : (dwu ? 'odrzucony' : 'LEWA')
      const jest = reka === 'P' ? (g.includes('PRAWA') ? 'PRAWA' : 'odrzucony') : (g.includes('LEWA') ? 'LEWA' : 'odrzucony')
      const zgub = g === 'BRAK' || s === 'BRAK'
      const sztOcz = reka === 'P' ? (dwu && !tylkoLewa ? 'plecak' : 'LEWA') : 'PRAWA'
      r.assert(jest === ocz && !zgub && s.includes(sztOcz), `${typ} ${x} ${rodzaj} do ${reka}: ${ocz}`, `przedmiot=${g} sztylet=${s}`)
    }
  }
  // obejscia: F (zamiana rak), klawisz 1-9 nad lewa, gracz z lukiem juz w lewej
  const czysc = async () => { await rcon(`minecraft:clear ${nick}`); await H.sleep(700); bot.setQuickBarSlot(0); await H.sleep(300) }
  const F = async () => { bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 }); await H.sleep(900) }
  await czysc(); await wPlecak('BOW', 'COMPOSITE_BOW'); await bot.clickWindow(27, 0, 0); await H.sleep(300); await bot.clickWindow(36, 0, 0); await H.sleep(700); await bot.closeWindow(bot.inventory)
  await F(); r.assert(await ok('container.0', id('COMPOSITE_BOW')) && !(await ok('weapon.offhand', id('COMPOSITE_BOW'))), 'F: luk z prawej nie przechodzi do lewej', await gdzie('COMPOSITE_BOW'))
  await czysc(); await wPlecak('SHIELD', 'PALADIN_SHIELD'); await bot.clickWindow(27, 0, 0); await H.sleep(300); await bot.clickWindow(45, 0, 0); await H.sleep(700); await bot.closeWindow(bot.inventory)
  await F(); r.assert(await ok('weapon.offhand', id('PALADIN_SHIELD')), 'F: tarcza z lewej nie przechodzi do prawej', await gdzie('PALADIN_SHIELD'))
  await czysc(); await rcon(`mi give BOW COMPOSITE_BOW ${nick} 1`); await H.sleep(600)
  let kb = -1; for (let k = 1; k < 9; k++) if (await ok('container.' + k, id('COMPOSITE_BOW'))) kb = k
  if (kb < 0) { for (let k = 9; k < 36; k++) if (await ok('container.' + k, id('COMPOSITE_BOW'))) { await rcon(`minecraft:item replace entity ${nick} container.5 from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`); kb = 5; break } }
  await bot.clickWindow(45, kb, 2); await H.sleep(900); await bot.closeWindow(bot.inventory)
  r.assert(!(await ok('weapon.offhand', id('COMPOSITE_BOW'))) && (await gdzie('COMPOSITE_BOW')) !== 'BRAK', 'klawisz 1-9 nad lewa: luk nie wchodzi do lewej', await gdzie('COMPOSITE_BOW'))
  await czysc(); await rcon(`mi give BOW COMPOSITE_BOW ${nick} 1`); await H.sleep(600)
  for (let k = 0; k < 36; k++) if (await ok('container.' + k, id('COMPOSITE_BOW'))) { await rcon(`minecraft:item replace entity ${nick} weapon.offhand from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`); break }
  bot.setQuickBarSlot(3); await H.sleep(400); bot.setQuickBarSlot(0); await H.sleep(1200)
  const gl = await gdzie('COMPOSITE_BOW'); console.log('luk wstawiony do lewej komenda ->', gl)
  r.assert(!gl.includes('LEWA') && gl !== 'BRAK', 'gracz z lukiem juz w lewej: luk wraca do ekwipunku, nie ginie', gl)
  await H.quit(bot)
}
