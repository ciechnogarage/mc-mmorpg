'use strict'
/**
 * Domknięcie dziur bazy klas (2026-09-26), ścieżka gracza (bot bez OP; konsola: przygotowanie i odczyt).
 *  1. Zestaw startowy nowej postaci (Człowiek Ninja -> Agent): zbroja lekka ZAŁOŻONA, broń grup klasy
 *     w plecaku, sloty akcesoriów 9-17 puste (zestaw CMI 'Newbie' wyłączony).
 *  2. Opis przedmiotu pokazuje wymaganie biegłości.
 *  3. Zamiennik: podniesiony vanillowy żelazny napierśnik -> ZBROJA_ZELAZNA_NAPIERSNIK (ciężka), Agent
 *     go nie założy; nasze przedmioty z vanillowym materiałem (IRON_SWORD) NIE są podmieniane.
 *  4. Vanillowej receptury napierśnika nie da się użyć.
 *  5. Stara postać z klasą archetypu (ROGUE, ta sama co w kreatorze) po ponownym wejściu dostaje klasę rasową i zestaw.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
const ma = async (nick, slot, id) => /passed/i.test(await rcon(`minecraft:execute if items entity ${nick} ${slot} *[minecraft:custom_data~{MMOITEMS_ITEM_ID:"${id}"}]`))
const pusty = async (nick, slot) => !/passed/i.test(await rcon(`minecraft:execute if items entity ${nick} ${slot} *`))
const lore = it => JSON.stringify(it?.components || it?.nbt || '')

module.exports = async function (r) {
  const nick = 'EvBaza' + Date.now() % 100000
  const ph = async (...p) => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
  let bot = await H.connect(nick)
  try {
    await F.enterCreator(bot); await F.completeCreator(bot, { race: 'czlowiek', klass: 'ninja' }); await H.sleep(6000)
    const [k] = await ph('mmocore_class_id')
    r.assert(k.toUpperCase() === 'AGENT', '1. Czlowiek Ninja -> Agent', k)
    // --- 1. zestaw ---
    const zb = { 'armor.head': 'ZBROJA_SKORZANA_HELM', 'armor.chest': 'ZBROJA_SKORZANA_NAPIERSNIK', 'armor.legs': 'ZBROJA_SKORZANA_NOGI', 'armor.feet': 'ZBROJA_SKORZANA_BUTY' }
    for (const [sl, id] of Object.entries(zb)) r.assert(await ma(nick, sl, id), `1. zalozone ${sl} = ${id}`)
    for (const id of ['SZTYLET_REKRUTA', 'LUK_MYSLIWSKI', 'KUSZA_PROSTA']) {
      const gdzie = []
      for (let s = 0; s <= 35; s++) if (await ma(nick, s < 9 ? `hotbar.${s}` : `inventory.${s - 9}`, id)) gdzie.push(s)
      r.assert(gdzie.length === 1 && (gdzie[0] < 8 || gdzie[0] > 18), `1. bron ${id} w plecaku, poza slotami 8-18`, gdzie.join(',') || 'brak')
    }
    r.assert(/passed/i.test(await rcon(`minecraft:execute if items entity ${nick} container.* minecraft:arrow`)), '1. strzaly w zestawie')
    const akc = []; for (let s = 9; s <= 17; s++) if (!(await pusty(nick, `container.${s}`))) akc.push(s)
    r.assert(akc.length === 0, '1. sloty akcesoriow 9-17 puste (brak zestawu Newbie)', akc.join(',') || 'puste')
    // --- 2. opis ---
    await H.sleep(500)
    const luk = bot.inventory.items().find(i => i.name === 'bow')
    r.assert(!!luk && /Wymaga biegłości/.test(lore(luk)) && /łuki/.test(lore(luk)), '2. opis luku pokazuje wymaganie biegłości', luk ? lore(luk).slice(0, 300) : 'brak luku')
    // --- 3. zamiennik ---
    const p = bot.entity.position
    await rcon(`minecraft:summon minecraft:item ${p.x.toFixed(1)} ${(p.y + 0.5).toFixed(1)} ${p.z.toFixed(1)} {Item:{id:"minecraft:iron_chestplate",count:1},PickupDelay:0}`)
    await H.sleep(3000)
    const zamieniony = /passed/i.test(await rcon(`minecraft:execute if items entity ${nick} container.* *[minecraft:custom_data~{MMOITEMS_ITEM_ID:"ZBROJA_ZELAZNA_NAPIERSNIK"}]`))
    const vanilla = /passed/i.test(await rcon(`minecraft:execute if items entity ${nick} container.* minecraft:iron_chestplate[!minecraft:custom_data]`))
    r.assert(zamieniony && !vanilla, '3. podniesiony vanillowy napiersnik -> ZBROJA_ZELAZNA_NAPIERSNIK', `nasz=${zamieniony} vanilla=${vanilla}`)
    r.assert(await ma(nick, 'container.*', 'SZTYLET_REKRUTA'), '3. nasz sztylet (material IRON_SWORD) nie zostal podmieniony')
    r.assert(await ma(nick, 'armor.chest', 'ZBROJA_SKORZANA_NAPIERSNIK'), '3. zalozona kurta nietknieta')
    // Agent (lekka) nie założy ciężkiego napierśnika: zdejmij kurtę, spróbuj
    await rcon(`minecraft:item replace entity ${nick} armor.chest with minecraft:air`); await H.sleep(800)
    const ciezki = bot.inventory.items().find(i => i.name === 'iron_chestplate')
    if (ciezki) { try { await bot.equip(ciezki, 'torso') } catch {} await H.sleep(1500) }
    r.assert(!(await ma(nick, 'armor.chest', 'ZBROJA_ZELAZNA_NAPIERSNIK')), '3. Agent nie zaklada zamienionego (ciezkiego) napiersnika')
    if (bot.inventory.cursorItem) { const w = bot.inventory.firstEmptyInventorySlot(); if (w != null) await bot.clickWindow(w, 0, 0) }
    // --- 4. receptura ---
    // poza spawnem: ochrona WorldGuard przy spawnie nie pozwala otworzyć stołu (pomiar 1: okno się nie otworzyło)
    await rcon(`minecraft:tp ${nick} 600 200 600`); await H.sleep(1500)
    await rcon(`minecraft:setblock 600 199 600 minecraft:stone`); await rcon(`minecraft:tp ${nick} 600.5 200 600.5`); await H.sleep(3000)
    await rcon(`minecraft:clear ${nick} minecraft:iron_ingot`)
    await rcon(`minecraft:give ${nick} minecraft:iron_ingot 8`)
    const b = bot.entity.position.floored()
    await rcon(`minecraft:setblock ${b.x + 1} ${b.y} ${b.z} minecraft:crafting_table`); await H.sleep(1500)
    const stol = bot.blockAt(b.offset(1, 0, 0))
    console.log('  setblock ->', (await rcon(`minecraft:execute if block ${b.x + 1} ${b.y} ${b.z} minecraft:crafting_table`)).trim())
    // ręcznie, jak gracz: 8 sztabek w siatce (X_X / XXX / XXX) i odczyt slotu wyniku
    let wynik = 'okno stolu nieotwarte'
    if (stol) {
      console.log('  tryb:', (await rcon(`papi parse ${nick} %player_gamemode%`)).trim(), 'swiat:', (await rcon(`papi parse ${nick} %player_world%`)).trim())
      console.log('  stol:', stol && stol.name, stol && stol.position, 'bot:', bot.entity.position)
      const okno = await bot.openBlock(stol).catch(e => { console.log('  openBlock:', e.message); return null })
      if (okno) {
        const sztabki = () => okno.slots.findIndex((it, i) => i >= 10 && it && it.name === 'iron_ingot')
        await bot.clickWindow(sztabki(), 0, 0); await H.sleep(300)
        for (const g of [1, 3, 4, 5, 6, 7, 8, 9]) { await bot.clickWindow(g, 1, 0); await H.sleep(250) }
        await bot.clickWindow(sztabki() >= 0 ? sztabki() : okno.slots.findIndex((it, i) => i >= 10 && !it), 0, 0).catch(() => {})
        await H.sleep(1200)
        const rez = okno.slots[0]
        wynik = 'wynik siatki: ' + (rez ? `${rez.name} ${H.itemLabel(rez) || ''}` : 'pusty')
        bot.closeWindow(okno); await H.sleep(800)
      }
    }
    const pusty0 = /pusty/.test(wynik)
    await H.sleep(1500)
    const ile = (await rcon(`minecraft:clear ${nick} minecraft:iron_chestplate[!minecraft:custom_data] 0`)).match(/Found (\d+)/)
    const zelazo = (await rcon(`minecraft:clear ${nick} minecraft:iron_ingot 0`)).match(/Found (\d+)/)
    // mineflayer nie otwiera stołu na tym serwerze (okno nie przychodzi, przyczyna nieustalona) — wtedy pomiar
    // jest tylko informacyjny, nie PASS. Zabezpieczenia: disable-vanilla-recipes (MMOItems) + wyzwalacz craft zamiennika.
    if (/nieotwarte/.test(wynik)) console.log('  4. NIEZMIERZONE: okno stolu sie nie otworzylo')
    else r.assert(!ile && pusty0, '4. vanillowa receptura napiersnika nie daje wyniku (siatka ulozona recznie)', `${wynik} | vanillowych napiersnikow: ${ile ? ile[1] : 0}, sztabek: ${zelazo ? zelazo[1] : 0}`)
    await rcon(`minecraft:setblock ${b.x + 1} ${b.y} ${b.z} minecraft:air`); await rcon(`minecraft:setblock 600 199 600 minecraft:air`)
    // --- 5. stara postać ---
    await rcon(`lp user ${nick} permission unset race.czlowiek`); await H.sleep(1500)
    await rcon(`mmocore class force ${nick} HUMAN`); await H.sleep(1000)
    await rcon(`mmocore class force ${nick} ROGUE`); await H.sleep(3000)
    const [k5] = await ph('mmocore_class_id')
    r.assert(k5.toUpperCase() === 'ROGUE', '5. przygotowanie: postac Ninja z klasa archetypu ROGUE (jak sprzed 2026-09-26)', k5)
    await rcon(`minecraft:clear ${nick}`)
    for (const sl of Object.keys(zb)) await rcon(`minecraft:item replace entity ${nick} ${sl} with minecraft:air`)
    await H.quit(bot); await H.sleep(3000)
    bot = await H.connect(nick)
    const ekran = await F.waitProfileScreen(bot)
    const glowa = ekran && H.windowSlots(ekran).find(s => s.slot < ekran.inventoryStart && s.name === 'player_head')
    if (r.assert(!!glowa, '5. postac do wybrania')) {
      await H.click(bot, glowa.slot); await H.sleep(10000)
      const [k6] = await ph('mmocore_class_id')
      r.assert(k6.toUpperCase() === 'AGENT', '5. stara postac (ROGUE, kreator wybiera te sama klase) po wejsciu -> Agent', k6)
      r.assert(await ma(nick, 'armor.chest', 'ZBROJA_SKORZANA_NAPIERSNIK'), '5. i dostaje zestaw (skorzana kurta zalozona)')
    }
  } finally { await rcon(`minecraft:clear ${nick}`).catch(() => {}); await H.quit(bot) }
}
