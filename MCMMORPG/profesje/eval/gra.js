'use strict'
/**
 * Eval profesji v2 z perspektywy gracza (bot BEZ OP na klonie prof-test).
 * Kazdy scenariusz mierzy skutek, nie zamiar: przedmiot w plecaku, XP z placeholdera, tekst na pasku akcji,
 * stan okna. Ustawienia i pomiary przez RCON (tak jak robilby to admin), gra — protokolem jak klient.
 */
const H = require('/scen/h.js')
const Rcon = H.Rcon
const { Vec3 } = require('vec3')

const NICK = process.env.EVAL_NICK || 'Prof' + Math.floor(Math.random() * 90000 + 10000)
const ONLY = (process.argv[2] || process.env.EVAL_ONLY || '').toLowerCase()
const sleep = ms => new Promise(r => setTimeout(r, ms))
const R = new H.Results('profesje')
let bot, rc
const paski = []
const Y = -60          // grunt plaskiego swiata klonu: trawa na -60, gracz stoi na -59
const OX = 2000, OZ = 2000   // plac testowy poza regionami WorldGuard (przy spawnie 'you can't harm that here')

const label = i => { try { return H.itemLabel(i) || '' } catch { return (i && i.displayName) || '' } }
const ile = frag => bot.inventory.items().filter(i => label(i).toLowerCase().includes(frag.toLowerCase()))
  .reduce((n, i) => n + i.count, 0)
const pasekOd = n => { const x = paski.slice(n).filter((t, i, a) => !/\|{5}/.test(t) || i === a.length - 1); return x.slice(-3).join(' | ') }
async function r(cmd) { return (await rc.send(cmd)).trim() }
async function papi(ph) { return (await r(`papi parse ${NICK} ${ph}`)).trim() }
async function postepXp(prof) {   // monotonicznie: poziom * 1000 + procent (procent zeruje sie przy awansie)
  return (await liczba(`%mmocore_profession_${prof}%`)) * 1000 + (await liczba(`%mmocore_profession_percent_${prof}%`))
}
async function liczba(ph) { const t = await papi(ph); const m = t.replace(',', '.').match(/-?\d+(\.\d+)?/); return m ? +m[0] : NaN }
async function postaw(id, x, y, z) {
  await r(`setblock ${x + OX} ${y} ${z + OZ} air`)
  return r(`rzemioslo postaw ${id} world ${x + OX} ${y} ${z + OZ}`)
}
async function tp(x, z) { await r(`minecraft:tp ${NICK} ${x + OX + 0.5} ${Y + 1} ${z + OZ + 0.5} 0 0`); await sleep(1500) }
async function klik(x, y, z) {
  const b = bot.blockAt(new Vec3(x + OX, y, z + OZ))
  if (!b) throw new Error(`brak bloku ${x} ${y} ${z}`)
  await bot.lookAt(b.position.offset(0.5, 0.5, 0.5), true)
  await bot.activateBlock(b)
}
/** Jak gracz: przytrzymaj PPM przez czas spozycia. bot.consume() konczy sie przy kazdej aktualizacji slotu w rece,
 *  a serwer odswieza trzymany przedmiot kilka razy na sekunde — wiec mineflayer przerywal jedzenie po ~50 ms. */
/** Wez do reki jak gracz: przedmiot na pasku -> wybor slotu (bez klikania w oknie; equip klikal i przedmiot
 *  utykal na kursorze, bo czesc slotow trzymaja przyciski UI). */
async function trzymaj(frag, id) {
  // serwerowo (klient bota widzi przyciski UI Ksiegi tylko u siebie — numery slotow sie rozjezdzaja)
  if (id) { await r(`rzemioslo doreki ${NICK} ${id} 5`); await sleep(700) }
  const ok = bot.heldItem && label(bot.heldItem).includes(frag)
  if (!ok) console.log('trzymaj: brak w rece', frag, bot.inventory.slots.slice(36, 45).map(x => x ? label(x) : '-').join(','))
  return ok
}
async function zjedz(frag, id, ms = 2200) {
  if (!await trzymaj(frag, id)) return false
  bot.activateItem(); await sleep(ms); bot.deactivateItem(); await sleep(400)
  return true
}
async function daj(typ, id, n = 1) { await r(`mi give ${typ} ${id} ${NICK} ${n}`); await sleep(400) }
async function wyczysc() { await r(`clear ${NICK}`); await sleep(300) }
const czy = name => !ONLY || name.toLowerCase().includes(ONLY)

async function scenariusze() {
  // ------------------------------------------------------------ teren testowy
  await r(`minecraft:tp ${NICK} ${OX + 0.5} ${Y + 1} ${OZ + 0.5}`); await sleep(2500)
  // czysty plac dla KAZDEGO scenariusza (run_all.sh uruchamia je osobno, swiezym botem)
  await r(`minecraft:kill @e[type=!minecraft:player,x=${OX},y=${Y},z=${OZ},distance=..30]`)
  await r(`fill ${OX - 8} ${Y - 3} ${OZ - 8} ${OX + 8} ${Y - 1} ${OZ + 8} stone`)
  await r(`fill ${OX - 6} ${Y + 1} ${OZ - 6} ${OX + 6} ${Y + 4} ${OZ + 6} air`)
  await r(`fill ${OX - 6} ${Y} ${OZ - 6} ${OX + 6} ${Y} ${OZ + 6} grass_block`)
  await r(`gamemode survival ${NICK}`)
  await r('difficulty peaceful')                         // plaski swiat klonu spawnuje szlamy (zabijaly bota); zajac jest pasywny
  await r('minecraft:kill @e[type=minecraft:slime]')
  await r(`effect give ${NICK} minecraft:saturation 600 1 true`)
  await r(`mmocore admin level set ${NICK} mining 1`)
  await wyczysc()

  if (czy('wezel')) {
    await postaw('wezel_t1_mining', 2, Y + 1, 0)
    await tp(0, 0)
    // 1. bez narzedzia
    let n = paski.length
    await klik(2, Y + 1, 0); await sleep(800)
    R.assert(ile('urobku') === 0 && /Potrzebujesz kilofa/i.test(pasekOd(n)), 'wezel bez narzedzia: nic nie daje, mowi czego brak', pasekOd(n))
    // 2. zlecenie startowe -> kilof I
    await r(`rzemioslo start ${NICK} mining`); await sleep(600)
    R.assert(ile('Kilof górnika I') === 1, 'zlecenie startowe wydaje Kilof górnika I', H.inventoryLabels(bot).join(', '))
    // 3. zbieranie: surowiec + XP + zuzycie; reka trzyma PUSTY slot (narzedzie gdzie indziej w plecaku)
    const pusty = [0, 1, 2, 3, 4, 5, 6, 7].find(i => !bot.inventory.slots[36 + i])
    if (pusty !== undefined) bot.setQuickBarSlot(pusty)
    await sleep(300)
    const xp0 = await liczba('%mmocore_profession_percent_mining%')
    n = paski.length
    const p0 = bot.pakietyEq.length
    await klik(2, Y + 1, 0); await sleep(1500)
    const wTrakcie = bot.pakietyEq.slice(p0)
    await sleep(2700)
    const poSesji = bot.pakietyEq.slice(p0 + wTrakcie.length)
    const idKilofa = bot.registry.itemsByName.iron_pickaxe.id
    // mineflayer nie obsluguje set_player_inventory (1.21.2+) — mierzymy pakiety, klient vanilla je stosuje
    R.assert(wTrakcie.includes(`set_player_inventory:${pusty}:${idKilofa}`) && poSesji.some(x => x.startsWith('window_items:0')),
      'kilof pokazany w dloni tylko na czas zbierania (pakiet do trzymanego slotu, potem resynchronizacja)',
      `w trakcie: ${wTrakcie.join(' ')}; po: ${poSesji.slice(0, 4).join(' ')}`)
    R.assert(ile('Bryła urobku') >= 1, 'PPM w zloze z kilofem w plecaku daje Bryłę urobku', pasekOd(n))
    const xp1 = await liczba('%mmocore_profession_percent_mining%')
    R.assert(xp1 > xp0, 'zbieranie daje XP gornictwa', `${xp0}% -> ${xp1}%`)
    const kilof = bot.inventory.items().find(i => label(i).includes('Kilof'))
    const zuz = kilof && (kilof.durabilityUsed || (kilof.components || []).find(c => c.type === 'damage')?.data || 0)
    R.assert(zuz >= 1, 'kilof zuzywa 1 pkt wytrzymalosci', `zuzyte=${zuz}`)
    // 4. wyczerpanie per gracz
    n = paski.length
    await klik(2, Y + 1, 0); await sleep(800)
    R.assert(/Wyczerpane/i.test(pasekOd(n)), 'wezel wyczerpany dla tego gracza po zebraniu', pasekOd(n))
    // 5. przerwanie ruchem
    await postaw('wezel_t1_mining', 2, Y + 1, 2)
    const przed = ile('Bryła urobku')
    n = paski.length
    await klik(2, Y + 1, 2); await sleep(600)
    bot.setControlState('back', true); await sleep(900); bot.setControlState('back', false); await sleep(2800)
    R.assert(ile('Bryła urobku') === przed && /Przerwano/i.test(pasekOd(n)), 'odejscie przerywa zbieranie bez nagrody', pasekOd(n))
    // 6. bramka poziomu i tieru narzedzia
    await postaw('wezel_t2_mining', -2, Y + 1, 0)
    await tp(0, 0)
    n = paski.length
    await klik(-2, Y + 1, 0); await sleep(800)
    R.assert(/wymaga/i.test(pasekOd(n)), 'wezel T2 przy poziomie 1: komunikat o wymaganym poziomie 11', pasekOd(n))
    await r(`mmocore admin level set ${NICK} mining 11`); await sleep(300)
    n = paski.length
    await klik(-2, Y + 1, 0); await sleep(800)
    R.assert(/tieru II \(masz I\)/i.test(pasekOd(n)), 'wezel T2 z kilofem I: wymaga kilofa II', pasekOd(n))
    await daj('TOOL', 'PROF_KILOF_2')
    n = paski.length
    await klik(-2, Y + 1, 0); await sleep(4200)
    R.assert(ile('solnego urobku') >= 1, 'wezel T2 z kilofem II daje surowiec T2', pasekOd(n))
    // 7. kara za nizszy tier: przy poziomie 11 (tier II) wezel T1 daje polowe XP (10 -> 5)
    await postaw('wezel_t1_mining', 0, Y + 1, -2)
    n = paski.length
    await klik(0, Y + 1, -2); await sleep(4200)
    R.assert(/· 5 XP/.test(pasekOd(n)), 'wezel T1 na poziomie 11 daje polowe XP (kara za nizszy tier)', pasekOd(n))
    // 8. dziala tez z narzedziem w rece i nie psuje zaklec/gardy (zdarzenie anulowane)
    R.assert(!/Zaklęcie|zaklęcie|Many/i.test(pasekOd(n)), 'PPM w wezel nie odpala zaklecia', pasekOd(n))
  }

  if (czy('zielarstwo')) {
    await r(`mmocore admin level set ${NICK} herbalism 1`)
    await postaw('wezel_t1_herbalism', 0, Y + 1, 2)
    await tp(0, 0)
    await r(`rzemioslo start ${NICK} herbalism`); await sleep(600)
    const n = paski.length
    await klik(0, Y + 1, 2); await sleep(4200)
    R.assert(ile('Zioło lecznicze') >= 1, 'kepa ziol (STRINGBLOCK) z sierpem daje Zioło lecznicze', pasekOd(n))
  }

  if (czy('buff')) {
    await wyczysc()
    await daj('CONSUMABLE', 'PROF_ELIKSIR_SILY_1')
    const st0 = await liczba('%mythiclib_stat_physical_damage%')
    bot.setQuickBarSlot(0)
    await zjedz('Eliksir', 'PROF_ELIKSIR_SILY_1')
    const info = await r(`rzemioslo info ${NICK}`)
    const st1 = await liczba('%mythiclib_stat_physical_damage%')
    R.assert(/eliksir: Eliksir siły I/.test(info), 'eliksir sily: aktywny buff grupy eliksir', info.slice(-120))
    R.assert(!isNaN(st1) && st1 > st0, 'eliksir sily podnosi PHYSICAL_DAMAGE (MythicLib)', `${st0} -> ${st1}`)
    // cooldown mikstur
    await daj('CONSUMABLE', 'PROF_MIKSTURA_LECZNICZA_1', 2)
    await zjedz('Mikstura lecznicza', 'PROF_MIKSTURA_LECZNICZA_1')
    await sleep(3000)
    const n = paski.length
    const c0 = bot.evalLog.length
    await zjedz('Mikstura lecznicza', 'PROF_MIKSTURA_LECZNICZA_1')
    const kom = pasekOd(n) + ' ' + bot.evalLog.slice(c0).join(' ')
    R.assert(ile('Mikstura lecznicza') === 1 && /Odnowienie/i.test(kom), 'druga mikstura w 30 s zablokowana (wspolny cooldown grupy)',
      `zostalo ${ile('Mikstura lecznicza')}; komunikaty: ${kom}`)
  }

  if (czy('zlecen')) {
    await wyczysc()
    await r(`mmocore admin level set ${NICK} smithing 1`)
    const tytul = await papi('%rzemioslo_zlecenie_smithing_1_tytul%')
    R.assert(/Dostarcz \d+× /.test(tytul), 'placeholder zlecenia dnia ma tresc', tytul)
    const m = tytul.match(/Dostarcz (\d+)× (.+)$/)
    const nazwa = m && m[2]
    const id = await papi('%rzemioslo_zlecenie_smithing_1_id%')
    const typ = await papi('%rzemioslo_zlecenie_smithing_1_typ%')
    if (/^[A-Z0-9_]+$/.test(id)) {
      await daj(typ, id, +m[1])
      const xp0 = await postepXp('smithing')
      await r(`rzemioslo zlecenie oddaj ${NICK} smithing 1`); await sleep(800)
      const xp1 = await postepXp('smithing')
      R.assert(ile(nazwa) === 0 && xp1 > xp0, 'oddanie zlecenia zabiera przedmioty i daje XP', `${xp0} -> ${xp1}`)
      const akcja = await papi('%rzemioslo_zlecenie_smithing_1_akcja%')
      R.assert(/Oddane/.test(akcja), 'zlecenie oznaczone jako oddane', akcja)
    } else {
      R.fail('zlecenie: nieznane id w tescie', tytul)
    }
  }

  if (czy('warsztat')) {
    await wyczysc()
    await r(`mmocore admin level set ${NICK} smithing 1`)
    await daj('MATERIAL', 'PROF_ORE_CHUNK', 3)
    const xp0 = await postepXp('smithing')
    await r(`mi stations open profession-smithing ${NICK}`)
    await H.waitForWindow(bot, { timeout: 8000 }); await sleep(1500)
    const win = bot.currentWindow
    console.log('sloty warsztatu:', H.windowSlots(win).filter(s => s.slot < 54).map(s => `${s.slot}:${s.name}:${s.label}`).join(' | ').slice(0, 900))
    const fs = H.findSlot(win, /Rafinowane/)
    const slot = fs ? fs.slot : null
    R.assert(slot != null && slot >= 0, 'warsztat kowala I pokazuje recepture Rafinowanego zelaza', `${H.windowTitle(win)} | ` + H.windowSlots(win).map(s => s.label).slice(0, 12).join(' / '))
    if (slot != null && slot >= 0) {
      await H.click(bot, slot); await sleep(1600)
      const w2 = bot.currentWindow
      const kolejka = w2 && H.findSlot(w2, /Rafinowane/) // receptura + pozycja w kolejce
      const sloty = w2 ? H.windowSlots(w2).filter(s => /Rafinowane/.test(s.label)).map(s => s.slot) : []
      for (const s of sloty.filter(s => s !== slot)) { await H.click(bot, s); await sleep(500) }
      bot.closeWindow(bot.currentWindow); await sleep(500)
      const xp1 = await postepXp('smithing')
      R.assert(ile('Rafinowane') >= 1, 'odbior z kolejki daje Rafinowane zelazo', H.inventoryLabels(bot).join(', '))
      R.assert(xp1 > xp0, 'XP kowalstwa za recepture (plugin, nie trigger)', `${xp0}% -> ${xp1}%`)
    }
  }

  if (czy('oprawianie')) {
    await wyczysc()
    await r(`mmocore admin level set ${NICK} hunting 1`)
    await r(`rzemioslo start ${NICK} hunting`); await sleep(500)
    await tp(0, 0)
    console.log('spawn:', await r(`mm mobs spawn BEAST_FOREST_HARE 1 world,${OX + 2.5},${Y + 1},${OZ + 0.5}`)); await sleep(1200)
    await r(`data merge entity @e[type=minecraft:rabbit,x=${OX},y=${Y},z=${OZ},distance=..24,limit=1,sort=nearest] {NoAI:1b,Health:1f}`)
    console.log('obok po spawnie:', Object.values(bot.entities).filter(e => e.position.distanceTo(bot.entity.position) < 8 && e !== bot.entity).map(e => e.name).join(','))
    const zaj = () => Object.values(bot.entities).find(e => e.position.distanceTo(bot.entity.position) < 6 && e.name === 'rabbit')
    // prawdziwy cios gracza (zajac unieruchomiony, 1 HP) — sciezka zabojstwa jak w grze
    for (let i = 0; i < 6 && zaj(); i++) { const z = zaj(); await bot.lookAt(z.position.offset(0, 0.3, 0), true); try { await bot.attack(z) } catch (x) { } await sleep(700) }
    await sleep(800)
    console.log('obok po walce:', Object.values(bot.entities).filter(e => e.position.distanceTo(bot.entity.position) < 8 && e !== bot.entity).map(e => e.name).join(','), '| czat:', bot.evalLog.slice(-3).join(' / '))
    const zwl = Object.values(bot.entities).find(e => e.name === 'interaction' && e.position.distanceTo(bot.entity.position) < 6)
    R.assert(!!zwl, 'zabicie bestii zostawia zwloki (Interaction)', zwl ? '' : 'brak encji')
    if (zwl) {
      const n = paski.length
      await bot.lookAt(zwl.position.offset(0, 0.2, 0), true)
      await bot.activateEntity(zwl); await sleep(3600)
      R.assert(ile('Łup z bestii') + ile('Surowe runo') >= 1, 'oprawianie nozem daje surowiec mysliwski', pasekOd(n))
    }
  }

  if (czy('lowienie')) {
    await wyczysc()
    await r(`mmocore admin level set ${NICK} fishing 1`)
    await r(`fill ${OX - 2} ${Y - 2} ${OZ - 6} ${OX + 2} ${Y} ${OZ - 3} water`)   // strona -z: bez wezlow (splawik trafial w zloza)
    await tp(0, 0)
    await r(`rzemioslo start ${NICK} fishing`); await sleep(600)
    await trzymaj('Wędka', 'PROF_WEDKA_1')
    await bot.lookAt(new Vec3(OX + 0.5, Y, OZ - 3.5), true)
    let ok = false
    for (let i = 0; i < 3 && !ok; i++) {
      try { await Promise.race([bot.fish(), sleep(40000).then(() => { throw new Error('timeout') })]) } catch (x) { console.log('fish:', String(x).slice(0, 100), '| reka:', bot.heldItem && label(bot.heldItem)) }
      await sleep(2500)
      // mineflayer nie podchodzi po przedmioty jak gracz — "podejscie" teleportem do najblizszego
      await r(`minecraft:tp ${NICK} @e[type=minecraft:item,x=${OX},y=${Y},z=${OZ},distance=..12,limit=1,sort=nearest]`); await sleep(1200)
      await tp(0, 0); await bot.lookAt(new Vec3(OX + 0.5, Y, OZ - 3.5), true)
      ok = ile('Świeży połów') + ile('Małż') + ile('Perła') + ile('Esencja wody') >= 1
      if (!ok) console.log('lowienie: komunikaty', bot.evalLog.slice(-3).join(' / '), '| przedmioty obok:', Object.values(bot.entities).filter(e => e.name === 'item').length)
    }
    R.assert(ok, 'wedka rybaka w zwyklej wodzie lowi surowiec T1', H.inventoryLabels(bot).join(', '))
  }

  if (czy('menu')) {
    await r(`dm open profession_main ${NICK}`)
    const win = await H.waitForWindow(bot, { timeout: 8000 })
    const s = H.windowSlots(win)
    const gorn = s.find(x => /Górnictwo/.test(x.label))
    R.assert(!!gorn, 'menu profesji ma kafel Górnictwo', s.map(x => x.label).join(' / ').slice(0, 200))
    bot.closeWindow(win); await sleep(400)
    await r(`dm open warsztat_smithing ${NICK}`)
    const w2 = await H.waitForWindow(bot, { timeout: 8000 })
    const l2 = H.windowSlots(w2).map(x => x.label)
    R.assert(l2.some(x => /Warsztat Kowala I$/.test(x)) && l2.some(x => /Warsztat Kowala II/.test(x)), 'menu warsztatu: tier I otwarty, II widoczny (zablokowany)', l2.join(' / ').slice(0, 220))
    bot.closeWindow(w2); await sleep(400)
    await r(`dm open zlecenia_mining ${NICK}`)
    const w3 = await H.waitForWindow(bot, { timeout: 8000 })
    const l3 = H.windowSlots(w3).map(x => x.label)
    R.assert(l3.some(x => /Dostarcz/.test(x)) && !l3.some(x => /%/.test(x)), 'menu zlecen: placeholdery rozwiazane', l3.join(' / ').slice(0, 220))
    bot.closeWindow(w3); await sleep(400)
  }

  if (czy('oboz')) {
    // logika obozu (ognisko, regeneracja poza walka) przez komende admina = ten sam kod co PPM;
    // samo PPM z przedmiotem sprawdza klient QA (bot nie bierze tu niezawodnie przedmiotu do reki: sloty okna E Ksiegi)
    await tp(-1, -1)
    const reg0 = await liczba('%mythiclib_stat_health_regeneration%')
    console.log('oboz:', await r(`rzemioslo oboz ${NICK} PROF_OBOZOWISKO_1`))
    await sleep(2500)
    const ogien = Object.values(bot.entities).find(e => e.name === 'block_display' && e.position.distanceTo(bot.entity.position) < 6)
    const reg1 = await liczba('%mythiclib_stat_health_regeneration%')
    R.assert(!!ogien, 'obozowisko: ognisko (block_display) stoi przy graczu')
    R.assert(reg1 > reg0, 'obozowisko: regeneracja zdrowia rosnie poza walka', `${reg0} -> ${reg1}`)
  }

  if (czy('naprawa')) {
    await wyczysc()
    await r(`mmocore admin level set ${NICK} mining 1`)
    await postaw('wezel_t1_mining', 2, Y + 1, 0)
    await tp(0, 0)
    await daj('TOOL', 'PROF_KILOF_1')
    await klik(2, Y + 1, 0); await sleep(4200)
    const zuz = () => { const k = bot.inventory.items().find(i => label(i).includes('Kilof')); return k ? (k.durabilityUsed || 0) : -1 }
    const przed = zuz()
    await daj('CONSUMABLE', 'PROF_ZESTAW_NAPRAWCZY_1')
    await r(`rzemioslo doreki ${NICK} PROF_ZESTAW_NAPRAWCZY_1 5`); await r(`rzemioslo doreki ${NICK} PROF_KILOF_1 6`); await sleep(800)
    await bot.clickWindow(41, 0, 0); await sleep(400); await bot.clickWindow(42, 0, 0); await sleep(1200)
    const po = zuz()
    R.assert(przed >= 1 && po < przed && ile('Zestaw naprawczy') === 0, 'zestaw naprawczy przeciagniety na kilof przywraca wytrzymalosc', `${przed} -> ${po}`)
  }

  if (czy('gniazdo')) {
    await wyczysc()
    await daj('GEM_STONE', 'PROF_KLEJNOT_ZDROWIA_1')
    await daj('HAMMER', 'RZM_PRZ_AKO_BULAWA_2')
    await r(`rzemioslo doreki ${NICK} PROF_KLEJNOT_ZDROWIA_1 5`); await r(`rzemioslo doreki ${NICK} RZM_PRZ_AKO_BULAWA_2 6`); await sleep(800)
    await bot.clickWindow(41, 0, 0); await sleep(400); await bot.clickWindow(42, 0, 0); await sleep(1200)
    const b = bot.inventory.items().find(i => /Buława/.test(label(i)))
    const lore = b ? JSON.stringify(b.components || b.nbt || '') : ''
    R.assert(ile('Klejnot zdrowia') === 0 && /Klejnot zdrowia/.test(lore), 'klejnot osadzony w gniezdzie sprzetu z warsztatu', lore.slice(0, 160))
  }

  if (czy('lowisko2')) {
    await wyczysc()
    await r(`mmocore admin level set ${NICK} fishing 15`)
    await r(`fill ${OX - 2} ${Y - 2} ${OZ - 6} ${OX + 2} ${Y} ${OZ - 3} water`)
    await r(`rzemioslo postaw lowisko_t2 world ${OX} ${Y - 2} ${OZ - 4}`)
    await tp(0, 0)
    await daj('TOOL', 'PROF_WEDKA_1')
    await trzymaj('Wędka', 'PROF_WEDKA_1')
    await bot.lookAt(new Vec3(OX + 0.5, Y, OZ - 3.5), true)
    const n = bot.evalLog.length
    try { await Promise.race([bot.fish(), sleep(40000).then(() => { throw new Error('timeout') })]) } catch (x) { }
    await sleep(1500)
    const kom = bot.evalLog.slice(n).join(' ') + ' ' + pasekOd(0)
    R.assert(/łowisko tieru II/.test(kom), 'lowisko II z wedka I: odmowa z podaniem wymaganego tieru', kom.slice(-160))
  }

  if (czy('mistrz')) {
    await r(`mmocore admin level set ${NICK} tailoring 50`)
    await r(`mmocore admin exp give ${NICK} tailoring 5000`); await sleep(500)
    const lvl = await liczba('%mmocore_profession_tailoring%')
    R.assert(lvl === 50, 'bez mistrzostwa krawiectwo stoi na 50 (plugin Mistrzostwa)', `poziom ${lvl}`)
    await r(`mmocore admin level set ${NICK} cooking 50`)
    await r(`mmocore admin exp give ${NICK} cooking 20000`); await sleep(500)
    const lc = await liczba('%mmocore_profession_cooking%')
    R.assert(lc > 50, 'gotowanie (ogolna) rosnie ponad 50', `poziom ${lc}`)
  }
}

;(async () => {
  rc = new Rcon()
  bot = await H.connect(NICK)
  bot.on('actionBar', m => paski.push(m.toString()))
  bot.pakietyEq = []
  let hp = 20
  bot.on('health', () => { if (bot.health < hp) console.log(`obrazenia: ${hp} -> ${bot.health} (pos ${bot.entity.position.toString()}, food ${bot.food})`); hp = bot.health })

  for (const n of ['set_slot', 'set_player_inventory', 'window_items']) bot._client.on(n, p => bot.pakietyEq.push(n + ':' + (p.slot ?? p.slotId ?? p.windowId) + ':' + (p.item?.itemId ?? p.contents?.itemId ?? '-')))
  bot._client.on('set_action_bar_text', p => { try { paski.push(JSON.stringify(p.text)) } catch { } })
  await sleep(2500)
  try {
    await scenariusze()
  } catch (e) {
    R.fail('wyjatek w scenariuszu', String(e && e.stack || e).slice(0, 300))
  }
  R.print()
  console.log(R.failed ? `\nWYNIK: ${R.failed} NIEPOWODZEN` : '\nWYNIK: wszystko PASS')
  await H.quit(bot); rc.close()
  process.exit(R.failed ? 1 : 0)
})()
