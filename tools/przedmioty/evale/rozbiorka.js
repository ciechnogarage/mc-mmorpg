'use strict'
// Rozbiorka: bot przeciaga noz rozbiorkowy na przedmiot z lupu -> przedmiot znika, pojawiaja sie surowce PROF_*.
// Czesc setu (bez tabeli rozbiorki) zostaje nietknieta.
const { opis, rcon, H } = require('/h/evale/wspolne.js')
module.exports = async function (r) {
  const nick = 'ItmB' + Date.now() % 10000
  const bot = await H.connect(nick); await H.sleep(3000)
  await rcon(`mmocore admin level set ${nick} main 10`)
  const inv = () => bot.inventory.items().map(i => ({ i, o: opis(i) })).filter(x => !/Sakwa|Dziennik|^(Naszyjnik|Ring \d|Dlonie|Pas|Charm)$/.test(x.o.nazwa))
  const czekaj = async (fn, ms = 6000) => { for (let t = 0; t < ms; t += 300) { const v = fn(); if (v) return v; await H.sleep(300) } return null }
  const przeciagnij = async (zrodlo, cel) => { await bot.clickWindow(zrodlo.slot, 0, 0); await H.sleep(300); await bot.clickWindow(cel.slot, 0, 0); await H.sleep(1500)
    if (bot.inventory.cursorItem) { const w = bot.inventory.firstEmptyInventorySlot(); if (w != null) await bot.clickWindow(w, 0, 0); await H.sleep(400) } }
  for (const [typ, id, etyk] of [['SWORD', 'PRZ_WOJ_MIECZ_3', 'baza z łupu'], ['ARMOR', 'PRZ_SET_CALUN_NOCNEGO_LISA_KLATA', 'część setu']]) {
    await rcon('minecraft:clear ' + nick); await H.sleep(400)
    await rcon(id.includes('SET') ? `mi give ${typ} ${id} ${nick} 1` : `mi generate ${nick} -type:${typ} -id:${id} -level:6`)
    await rcon(`mi give CONSUMABLE PRZ_NOZ_ROZBIORKI ${nick} 1`)
    const noz = await czekaj(() => inv().find(x => x.o.nazwa.includes('Nóż rozbiórkowy')))
    const cel = await czekaj(() => inv().find(x => !x.o.nazwa.includes('Nóż') && x.o.lore.length))
    if (!noz || !cel) { r.fail(`${etyk}: brak noża lub przedmiotu`); continue }
    const przed = cel.o.nazwa, rz = cel.o.lore[0]
    await przeciagnij(noz.i, cel.i)
    const po = inv().map(x => x.o.nazwa).concat(bot.inventory.cursorItem ? [opis(bot.inventory.cursorItem).nazwa] : [])
    const jest = po.includes(przed), prof = po.filter(n => !n.includes('Nóż') && n !== przed)
    if (id.includes('SET')) {
      const kursor = /passed/i.test(await rcon(`minecraft:execute if items entity ${nick} player.cursor *[minecraft:custom_data~{MMOITEMS_ITEM_ID:"${id}"}]`))
      const eq = /passed/i.test(await rcon(`minecraft:execute if items entity ${nick} container.* *[minecraft:custom_data~{MMOITEMS_ITEM_ID:"${id}"}]`))
      r.assert(kursor || eq, `${etyk}: nie da się rozebrać, przedmiot zostaje (w ekwipunku lub na kursorze)`, `eq ${eq}, kursor ${kursor}`)
    }
    else r.assert(!jest && prof.length > 0, `${etyk} (${rz}): rozebrana na surowce`, `${przed} -> ${prof.join(', ')}`)
  }
  bot.quit()
}
