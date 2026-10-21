'use strict'
const H = require('/e/lib/harness'); const { rcon } = require('/k/rcon.js')
const PROBKI = []
module.exports = async function (r) {
  const nick = 'EvM' + Date.now() % 10000
  const ok = async (slot, pred) => (await rcon(`execute if items entity ${nick} ${slot} ${pred}`)).includes('passed')
  const id = x => `*[custom_data~{MMOITEMS_ITEM_ID:"${x}"}]`
  const gdzie = async x => { const o = []; if (await ok('weapon.offhand', id(x))) o.push('LEWA'); if (await ok('container.0', id(x))) o.push('PRAWA'); if (await ok('player.cursor', id(x))) o.push('kursor'); for (let k = 1; k < 36; k++) if (await ok('container.' + k, id(x))) o.push(k < 9 ? 'pasek' + k : 'plecak'); return o.join(',') || 'BRAK' }
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
      try { bot.closeWindow(bot.inventory) } catch (e) {}; await H.sleep(200)
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
  await czysc(); await wPlecak('BOW', 'COMPOSITE_BOW'); await bot.clickWindow(27, 0, 0); await H.sleep(300); await bot.clickWindow(36, 0, 0); await H.sleep(700); try { bot.closeWindow(bot.inventory) } catch (e) {}
  await F(); r.assert(await ok('container.0', id('COMPOSITE_BOW')) && !(await ok('weapon.offhand', id('COMPOSITE_BOW'))), 'F: luk z prawej nie przechodzi do lewej', await gdzie('COMPOSITE_BOW'))
  await czysc(); await wPlecak('SHIELD', 'PALADIN_SHIELD'); await bot.clickWindow(27, 0, 0); await H.sleep(300); await bot.clickWindow(45, 0, 0); await H.sleep(700); try { bot.closeWindow(bot.inventory) } catch (e) {}
  console.log('tarcza przed F:', await gdzie('PALADIN_SHIELD')); await F(); console.log('tarcza po F:', await gdzie('PALADIN_SHIELD')); r.assert(await ok('weapon.offhand', id('PALADIN_SHIELD')), 'F: tarcza z lewej nie przechodzi do prawej', await gdzie('PALADIN_SHIELD'))
  await czysc(); await wPlecak('DAGGER', 'STEEL_DAGGER'); await bot.clickWindow(27, 0, 0); await H.sleep(300); await bot.clickWindow(36, 0, 0); await H.sleep(700)
  await rcon(`mi give BOW COMPOSITE_BOW ${nick} 1`); await H.sleep(600)
  for (let k = 1; k < 36; k++) if (await ok('container.' + k, id('COMPOSITE_BOW'))) { if (k !== 5) { await rcon(`minecraft:item replace entity ${nick} container.5 from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`) } break }
  const kb = 5
  console.log('luk w pasku', kb, await gdzie('COMPOSITE_BOW')); await bot.clickWindow(45, kb, 2); await H.sleep(900); try { bot.closeWindow(bot.inventory) } catch (e) {}
  r.assert(!(await ok('weapon.offhand', id('COMPOSITE_BOW'))) && (await gdzie('COMPOSITE_BOW')) !== 'BRAK', 'klawisz 1-9 nad lewa: luk nie wchodzi do lewej', await gdzie('COMPOSITE_BOW'))
  await czysc(); await rcon(`mi give BOW COMPOSITE_BOW ${nick} 1`); await H.sleep(600)
  for (let k = 0; k < 36; k++) if (await ok('container.' + k, id('COMPOSITE_BOW'))) { await rcon(`minecraft:item replace entity ${nick} weapon.offhand from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`); break }
  bot.setQuickBarSlot(3); await H.sleep(400); bot.setQuickBarSlot(0); await H.sleep(1200)
  const gl = await gdzie('COMPOSITE_BOW'); console.log('luk wstawiony do lewej komenda ->', gl)
  r.assert(!gl.includes('LEWA') && gl !== 'BRAK', 'gracz z lukiem juz w lewej: luk wraca do ekwipunku, nie ginie', gl)
  await H.quit(bot)
}
