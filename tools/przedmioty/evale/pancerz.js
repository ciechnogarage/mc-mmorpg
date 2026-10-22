'use strict'
// Pancerz vs moby Aktu 1: sredni ubytek HP na trafienie dla rycerza poz. 5 bez zbroi i w zbroi (4 czesci bazy poz. 5, wersja 2).
const { opis, rcon, ph, H } = require('/h/evale/wspolne.js')
const MOBY = [['BEAST_WILD_BOAR', 'T1 dzik'], ['UNDEAD_CRYPT_GUARD', 'T2 strażnik krypty']]
module.exports = async function (r) {
  const wyn = []
  for (const zbroja of [false, true]) {
    for (const [mob, etyk] of MOBY) {
      const nick = 'ItmP' + Date.now() % 100000
      const bot = await H.connect(nick); await H.sleep(3000)
      await rcon(`mmocore class select ${nick} RYCERZ`); await rcon(`mmocore admin level set ${nick} main 5`); await rcon(`mmocore admin attribute give ${nick} strength 2`)
      await rcon('minecraft:time set day'); await rcon('minecraft:kill @e[type=!player,type=!item_display,type=!interaction]'); await H.sleep(800)
      if (zbroja) {
        for (const [cz, slot] of [['HELM', 'head'], ['KLATA', 'torso'], ['NOGI', 'legs'], ['BUTY', 'feet']]) {
          await rcon(`mi generate ${nick} -type:ARMOR -id:PRZ_WOJ_${cz}_3 -level:5`); await H.sleep(900)
          const it = bot.inventory.items().find(i => i.name.includes(slot === 'head' ? 'helmet' : slot === 'torso' ? 'chestplate' : slot === 'legs' ? 'leggings' : 'boots'))
          if (it) { try { await bot.equip(it, slot) } catch (e) { } await H.sleep(900) }
        }
      }
      const pancerz = (await ph(nick, 'mythiclib_stat_armor', 'mythiclib_stat_damage_reduction', 'mythiclib_stat_max_health')).join(' / ')
      await rcon(`minecraft:effect give ${nick} minecraft:regeneration 60 3 true`)
      const trafienia = []; let hp = bot.health
      bot.on('health', () => { if (bot.health < hp - 0.01) trafienia.push(hp - bot.health); hp = bot.health })
      const p = bot.entity.position
      await rcon(`mm mobs spawn ${mob} 1 world,${(p.x + 2).toFixed(1)},${p.y.toFixed(1)},${p.z.toFixed(1)}`)
      await H.sleep(20000)
      bot.quit(); await H.sleep(1500)
      const sr = trafienia.length ? trafienia.reduce((a, b) => a + b, 0) / trafienia.length : 0
      wyn.push({ zbroja, etyk, trafien: trafienia.length, srednio: +sr.toFixed(2), pancerz })
      console.log(`${zbroja ? 'W ZBROI' : 'BEZ ZBROI'} vs ${etyk}: ${trafienia.length} trafień, średnio ${sr.toFixed(2)} HP (pancerz/redukcja/HP: ${pancerz})`)
    }
  }
  for (const [, etyk] of MOBY) {
    const a = wyn.find(x => !x.zbroja && x.etyk === etyk), b = wyn.find(x => x.zbroja && x.etyk === etyk)
    r.assert(a.trafien > 0 && b.trafien > 0, `${etyk}: mob trafia (bez ${a.trafien}, w zbroi ${b.trafien})`)
    r.ok(`${etyk}: obrażenia na trafienie bez zbroi ${a.srednio} → w zbroi ${b.srednio} (${a.srednio ? Math.round(100 * (1 - b.srednio / a.srednio)) : 0}% mniej)`)
  }
}
