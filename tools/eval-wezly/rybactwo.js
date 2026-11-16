'use strict'
/**
 * Eval RYBACTWA (tabela on-fish w MMOCore/professions/fishing.yml) na ZYWYM world1.
 *
 * Jak dziala minigra MMOCore - odczytane z BAJTKODU
 * (net.Indyuce.mmocore.listener.profession.FishingListener$FishingData), nie zgadniete:
 *   1. PlayerFishEvent.State.BITE -> MMOCore tworzy FishingData, losuje lup z tabeli,
 *      losuje 'tugs' (rollTugs) i rejestruje wlasny listener.
 *   2. Kazdy kolejny PlayerFishEvent gracza w stanie CAUGHT_FISH / FAILED_ATTEMPT /
 *      REEL_IN jest ANULOWANY (splawik zostaje) i liczy sie jako pullOnce().
 *      Czyli JEDNO SZARPNIECIE = JEDEN PRAWY PRZYCISK NA WEDCE.
 *   3. pullOnce() zwraca true, gdy currentPulls >= fishStrength -> lootFish().
 *   4. isTimedOut(): last + 1000 ms < teraz  -> MASZ SEKUNDE MIEDZY SZARPNIECIAMI.
 *   5. lootFish() wymaga WEDKI W RECE (EquipmentSlot.HAND).
 * Wniosek: minigra JEST sterowalna mineflayerem - wystarczy klikac PPM co ~350 ms
 * po ugryzieniu. Nie trzeba trafiac w zadne okno czasowe poza limitem 1 s.
 *
 * Sprawdzamy: czy lowienie dziala, CO wychodzi z wody (maja byc wylacznie nasze
 * PROF_*), czy NIE wychodzi waniliowa ryba i czy rosnie XP profesji fishing.
 *
 * EVAL_SABOTAGE=bezszarpniec - nie szarpie po ugryzieniu; polow ma NIE wyjsc.
 */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const { odczytXp: czytajXp } = require('/w/xp.js')

const NICK = process.env.EVAL_NICK || 'EvRyba'
const SABOTAZ = (process.env.EVAL_SABOTAGE || '').toLowerCase()
const PROBY = +(process.env.EVAL_PROBY || 8)
const sleep = ms => new Promise(r => setTimeout(r, ms))

// Nasze surowce rybackie wg professions/fishing.yml -> nazwy z MMOItems/item/material.yml
const NASZE = {
  'Świeży połów': 'PROF_RAW_CATCH', 'Małż': 'PROF_CLAM', 'Rzadka ryba': 'PROF_RARE_FISH',
  'Esencja wody': 'PROF_WATER_ESSENCE', 'Perła': 'PROF_PEARL',
}
const WANILIA = ['cod', 'salmon', 'tropical_fish', 'pufferfish', 'nautilus_shell',
  'lily_pad', 'bowl', 'leather', 'stick', 'string', 'ink_sac', 'bone', 'rotten_flesh',
  'tripwire_hook', 'enchanted_book', 'name_tag', 'saddle']

const label = i => { try { return H.itemLabel(i) } catch { return (i && i.displayName) || '' } }
const invList = bot => bot.inventory.items().map(i => `${i.count}x ${label(i)} [${i.name}]`).join(' | ') || '(pusty)'

class R {
  constructor() { this.rows = [] }
  assert(c, l, e) { this.rows.push({ pass: !!c, l, e }); return !!c }
  print() {
    console.log('\n================ WYNIKI RYBACTWA ================')
    for (const x of this.rows) console.log(`${x.pass ? 'PASS' : 'FAIL'} ${x.l}${x.e ? ' — ' + x.e : ''}`)
    const f = this.rows.filter(x => !x.pass).length
    console.log(f ? `\nWYNIK: ${f} NIEPOWODZEN / ${this.rows.length}` : `\nWYNIK: wszystko PASS (${this.rows.length})`)
    return f
  }
}

const odczytXp = bot => czytajXp(bot, NICK, 'fishing')

/** Splawik jako encja - po nim poznajemy, ze rzut wyszedl. */
const splawik = bot => Object.values(bot.entities)
  .find(e => /fishing_bobber|fishing_hook/i.test(e.name || '') ||
             /fishing_bobber|fishing_hook/i.test((e.displayName || '')))

;(async () => {
  const r = new R()
  if (SABOTAZ) console.log(`### TRYB SABOTAZU: ${SABOTAZ} ###`)
  const bot = await H.connect(NICK, { timeout: 90000 })
  try {
    try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) }
    catch (e) { console.log('(kreator: ' + e.message + ')') }
    await sleep(3000)

    // ustawienia startowe: profesja na sensownym poziomie, survival, pelne zycie
    bot.chat(`/mmocore admin level set ${NICK} fishing 10`); await sleep(800)
    bot.chat(`/minecraft:gamemode survival ${NICK}`); await sleep(800)
    bot.chat(`/minecraft:execute in minecraft:overworld run tp ${NICK} 150.5 99 197.5`); await sleep(3000)
    console.log('pozycja startowa: ' + bot.entity.position)

    // --- znajdz wode ---
    const woda = bot.findBlock({ matching: b => b && b.name === 'water', maxDistance: 64, count: 1 })
    if (!woda) {
      // bez wody nie ma czego testowac - mowimy to wprost, nie udajemy
      r.assert(false, 'RYBACTWO: jest woda w zasiegu 64 kratek od spawnu',
        'nie znaleziono zadnego bloku wody - lowienie nietestowalne w tym miejscu; bot stal na ' + bot.entity.position)
      throw new Error('BRAK-WODY')
    }
    console.log('woda znaleziona: ' + woda.position)
    r.assert(true, 'RYBACTWO: woda w zasiegu', 'blok wody ' + woda.position)

    // stan obok wody i patrz w nia
    const p = woda.position
    bot.chat(`/minecraft:execute in minecraft:overworld run tp ${NICK} ${p.x + 0.5} ${p.y + 1} ${p.z + 3.5}`)
    await sleep(2500)
    // Czysty plecak: bot zbiera po drodze smieci z terenu (kamien, deski), a one
    // podszywaja sie pod "lup z wody" i psuja asercje o zawartosci polowu.
    bot.chat(`/minecraft:clear ${NICK}`); await sleep(1200)
    bot.chat(`/minecraft:give ${NICK} minecraft:fishing_rod`); await sleep(1200)

    const rod = bot.inventory.items().find(i => i.name === 'fishing_rod')
    if (!r.assert(!!rod, 'RYBACTWO: bot ma wedke', invList(bot))) throw new Error('BRAK-WODY')
    await bot.equip(rod, 'hand'); await sleep(600)

    // --- wykrycie UGRYZIENIA ---
    // PULAPKA, na ktora juz raz wpadlem: rzut wedka tez gra dzwiek zawierajacy
    // "fishing_bobber" (entity.fishing_bobber.throw), wiec regex /fishing_bobber/
    // uznawal KAZDY rzut za ugryzienie, bot zaczynal szarpac od razu i tylko
    // zwijal wedke. Ugryzienie to WYLACZNIE '...bobber.splash'.
    let bite = false
    const dzwieki = new Set()
    const onSound = (name) => {
      const s = String(name || '')
      if (s) dzwieki.add(s)
      if (/splash/i.test(s) && !/throw|retrieve/i.test(s)) bite = true
    }
    bot.on('soundEffectHeard', n => onSound(n))
    // MMOCore rysuje nad splawikiem wskaznik CAUGHT_FISH - drugi, niezalezny sygnal
    let hologramy = 0
    bot.on('entitySpawn', e => {
      if (/armor_stand|text_display|item_display/i.test(e.name || '')) { hologramy++; bite = true }
    })

    const zdobyte = []
    let ugryzien = 0, rzutow = 0
    const xpPrzed = await odczytXp(bot)
    r.assert(Number.isFinite(xpPrzed), 'RYBACTWO: odczyt XP fishing dziala', `zwrocono ${xpPrzed}`)

    for (let proba = 1; proba <= PROBY; proba++) {
      await bot.lookAt(woda.position.offset(0.5, 0.5, 0.5), true); await sleep(300)
      bite = false
      const przedIlosc = bot.inventory.items().reduce((a, i) => a + i.count, 0)

      bot.activateItem(); rzutow++            // rzut
      await sleep(900)
      const sp = splawik(bot)
      if (proba === 1) r.assert(!!sp, 'RYBACTWO: splawik lata po rzucie', sp ? 'encja ' + sp.name : 'brak encji splawika')

      // czekamy na ugryzienie do 40 s
      const deadline = Date.now() + 40000
      while (!bite && Date.now() < deadline) await sleep(200)
      if (!bite) { console.log(`proba ${proba}: brak ugryzienia w 40 s, zwijam`); bot.activateItem(); await sleep(800); continue }
      ugryzien++
      console.log(`proba ${proba}: UGRYZIENIE`)

      if (SABOTAZ === 'bezszarpniec') { await sleep(2500); bot.activateItem(); await sleep(800); continue }

      // szarpiemy: PPM co 350 ms, limit MMOCore to 1000 ms miedzy szarpnieciami.
      // Najdrozszy lup wymaga 15 szarpniec - dajemy zapas.
      // Kluczowa diagnostyka: czy splawik PRZEZYWA szarpniecie. Jesli znika po
      // pierwszym PPM, znaczy ze MMOCore NIE anulowal zdarzenia, czyli minigra
      // w ogole nie wystartowala i szarpanie nie ma sensu.
      // WAZNE odkrycie z diagnostyki: po szarpnieciu splawik i tak znika (vanilla
      // usuwa haczyk przy zwijaniu, mimo anulowania zdarzenia przez MMOCore).
      // Jedno SZARPNIECIE kosztuje wiec DWA klikniecia: zwiniecie (liczy sie jako
      // pull) i ponowny rzut. A MMOCore daje tylko 1000 ms miedzy pullami
      // (isTimedOut: last + 1000 < now). Stad tempo musi byc szybkie - przy 350 ms
      // na klik odstep miedzy pullami wychodzil ~1,7 s i minigra wygasala.
      // TRYB=wanilia: po ugryzieniu JEDNO klikniecie, tak jak zwykly gracz bez
      // minigry. To rozstrzyga, czy MMOCore w ogole przechwytuje polow:
      //   wypadla waniliowa ryba -> tabela on-fish NIE dziala, MMOCore nie przechwytuje
      //   nie wypadlo nic         -> MMOCore przechwytuje, ale minigra sie nie konczy
      if ((process.env.EVAL_TRYB || '') === 'wanilia') {
        bot.activateItem()
        await sleep(2500)
        console.log(`  [wanilia] po jednym kliknieciu: ${invList(bot)}`)
        continue
      }
      const TEMPO = +(process.env.EVAL_TEMPO || 120)
      for (let t = 0; t < 60; t++) {
        bot.activateItem()
        await sleep(TEMPO)
        const teraz = bot.inventory.items().reduce((a, i) => a + i.count, 0)
        if (teraz > przedIlosc) { console.log(`  lup po ${t + 1} broniach/klikach (tempo ${TEMPO} ms)`); break }
      }
      await sleep(1200)

      for (const it of bot.inventory.items()) {
        const l = (label(it) || '').trim()
        if (!zdobyte.some(z => z.l === l)) zdobyte.push({ l, mat: it.name, n: it.count })
      }
      console.log(`  plecak: ${invList(bot)}`)
      // zwin ewentualny wiszacy splawik
      if (splawik(bot)) { bot.activateItem(); await sleep(600) }
    }

    console.log(`\nrzutow=${rzutow} ugryzien=${ugryzien} hologramow=${hologramy}`)
    console.log('uslyszane dzwieki: ' + [...dzwieki].join(', ').slice(0, 400))
    r.assert(ugryzien > 0, 'RYBACTWO: ryba w ogole bierze (BITE)', `ugryzien ${ugryzien} / rzutow ${rzutow}`)

    const lupy = bot.inventory.items().filter(i => i.name !== 'fishing_rod')
    const nasze = lupy.filter(i => NASZE[(label(i) || '').trim()])
    const obce = lupy.filter(i => !NASZE[(label(i) || '').trim()])

    r.assert(nasze.length > 0, 'RYBACTWO: z wody wychodza NASZE surowce PROF_*',
      nasze.map(i => `${i.count}x ${label(i)}`).join(', ') || `nic naszego; caly plecak: ${invList(bot)}`)
    r.assert(obce.length === 0, 'RYBACTWO: z wody NIE wychodzi nic obcego (wanilia)',
      obce.map(i => `${i.count}x ${label(i)} [${i.name}]`).join(', ') || '(czysto)')
    const wanilia = lupy.filter(i => WANILIA.includes(i.name) && !NASZE[(label(i) || '').trim()])
    r.assert(wanilia.length === 0, 'RYBACTWO: brak waniliowego lupu wedkarskiego',
      wanilia.map(i => i.name).join(', ') || '(brak)')

    const xpPo = await odczytXp(bot)
    r.assert(Number.isFinite(xpPo) && xpPo > xpPrzed, 'RYBACTWO: XP fishing wzroslo',
      `${xpPrzed} -> ${xpPo} EXP`)

    console.log('\nWSZYSTKO, CO WYSZLO Z WODY: ' + (zdobyte.map(z => z.l + ' [' + z.mat + ']').join(', ') || '(nic)'))
  } catch (e) {
    if (e.message !== 'BRAK-WODY') r.assert(false, 'RYBACTWO: przebieg przerwany wyjatkiem', e.message)
  } finally { await H.quit(bot) }
  process.exit(r.print() ? 1 : 0)
})().catch(e => { console.log('FAIL przerwane — ' + e.message); process.exit(2) })
