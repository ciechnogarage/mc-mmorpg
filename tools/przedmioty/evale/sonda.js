'use strict'
// Sonda: co gracz naprawde dostaje — nazwa, lore, farba/ozdoba — dla kilku baz, rarity i poziomow.
const { opis, generuj, rcon, H } = require('/h/evale/wspolne.js')
const fs = require('fs')
module.exports = async function (r) {
  const nick = 'ItmS' + Date.now() % 10000
  const bot = await H.connect(nick); await H.sleep(3000)
  await rcon(`mmocore class select ${nick} RYCERZ`); await rcon(`mmocore admin level set ${nick} main 10`)
  const proby = [['ARMOR', 'PRZ_WOJ_HELM_3', 5, 'RZADKI'], ['ARMOR', 'PRZ_MAG_KLATA_5', 10, 'MAGICZNY'], ['SWORD', 'PRZ_WOJ_MIECZ_2', 3, 'RZADKI'],
                 ['RING', 'PRZ_NIN_RING_4', 8, 'RZADKI'], ['DAGGER', 'PRZ_NIN_SZTYLET_1', 1, 'ZWYKLY'], ['ARMOR', 'PRZ_SET_CALUN_NOCNEGO_LISA_KLATA', 5, 'ZESTAW'], ['CONSUMABLE', 'PRZ_OSELKA', 1, null]]
  const wynik = []
  for (const [typ, id, poz, tier] of proby) {
    await rcon('minecraft:clear ' + nick); await H.sleep(500)
    const odp = await generuj(nick, typ, id, poz, tier); await H.sleep(1200)
    const it = bot.inventory.items().find(i => !/Sakwa|Dziennik/.test(opis(i).nazwa))
    const o = opis(it); wynik.push({ id, poz, tier, odp, o, surowe: it ? it.components : null })
    r.assert(!!it, `${id} poz.${poz} ${tier || ''}: przedmiot w ekwipunku`, o ? o.nazwa + ' | ' + o.lore.join(' / ') : odp)
  }
  fs.writeFileSync('/w/sonda.json', JSON.stringify(wynik, null, 1))
  bot.quit()
}
