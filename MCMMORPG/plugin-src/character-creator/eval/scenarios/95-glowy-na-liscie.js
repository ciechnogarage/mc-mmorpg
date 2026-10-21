'use strict'
/**
 * Glowy na ekranie wyboru postaci maja pokazywac SKORKE POSTACI, a nie domyslna glowe.
 *
 * To DRUGIE miejsce, w ktorym skorka postaci jest widoczna (pierwsze to sam gracz, patrz
 * scenariusz 70) — i dotad jedyne nie objete zadnym testem. Glowy dokladamy do CUDZEGO okna
 * (MMOProfiles) juz po jego otwarciu, wiec zalezy to i od ukladu slotow w jego configu,
 * i od tego, czy jego wlasne przerysowanie nam ich nie skasuje; jedno i drugie moze sie
 * zmienic przy aktualizacji MMOProfiles, bez slowa w logu.
 *
 * Bot nie renderuje, wiec czyta komponent 'profile' glowy: nazwe profilu (cc_<skorka>)
 * i wartosc wlasnosci 'textures', porownana z skins.yml — tak samo jak scenariusz 70.
 * Sprawdzamy oba konce: ze glowa skorke DOSTAJE (i jak szybko) i ze ja po chwili NADAL ma.
 */
const fs = require('fs')
const H = require('../lib/harness'); const F = require('../lib/flow')

const SKINS_YML = '/res/skins.yml'
/** Sloty pozycji profilu w GUI MMOProfiles (language/gui/profile-list.yml). */
const PROFILE_SLOTS = [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25]

function expectedSkins() {
  if (!fs.existsSync(SKINS_YML)) return null
  const out = {}
  let current = null
  for (const line of fs.readFileSync(SKINS_YML, 'utf8').split('\n')) {
    const head = line.match(/^ {2}([a-z0-9_]+):\s*$/)
    if (head) { current = head[1]; out[current] = {}; continue }
    const value = line.match(/^ {4}value:\s*'(.+)'\s*$/)
    if (value && current) out[current].value = value[1]
  }
  return out
}

/** Komponent 'profile' glowy w danym slocie — albo null, gdy glowa jest domyslna. */
function headProfile(window, slot) {
  const item = window.slots[slot]
  if (!item || item.name !== 'player_head') return null
  const profile = (item.components || []).find(c => c.type === 'profile')
  return profile ? profile.data : null
}

module.exports = async function (r) {
  const skins = expectedSkins()
  if (!r.assert(!!skins && !!skins.ork_mag, 'skins.yml ma skorke ork_mag')) return

  const name = 'EvGlow' + Date.now() % 100000
  let bot = await H.connect(name)
  try {
    const w = await F.enterCreator(bot)
    if (!r.assert(!!w, 'kreator otwiera sie')) return
    await F.completeCreator(bot, { race: 'ork', klass: 'mag' })
    if (!r.assert(bot.evalLog.some(m => m.includes('Postać utworzona')), 'postac utworzona',
      bot.evalLog.slice(-3).join(' / '))) return
  } finally { await H.quit(bot) }

  // Przerwa jak w scenariuszu 70: MMOProfiles zwalnia profil asynchronicznie.
  await H.sleep(5000)
  bot = await H.connect(name)
  try {
    // PIERWSZE otwarcie ekranu — to jedyne, ktore widzi gracz wchodzacy do gry.
    const list = await F.waitProfileScreen(bot, 45000)
    if (!r.assert(!!list, 'po relogu wraca ekran wyboru postaci')) return

    // Glowy dokladamy do JUZ OTWARTEGO okna, wiec w chwili jego pojawienia sie ich tam
    // jeszcze nie ma — i nie o to chodzi. Pytanie brzmi: czy gracz, ktory na to okno
    // patrzy, zobaczy skorke swojej postaci. Dajemy na to 5 s i mierzymy, ile zajelo.
    // (Mierzone: 0.0–0.3 s. Gdyby kiedys uroslo do sekund, to juz jest usterka do zbadania,
    // bo tyle wlasnie trwa spojrzenie na liste.)
    const deadline = Date.now() + 5000
    let heads = []
    while (Date.now() < deadline) {
      const window = bot.currentWindow || list
      heads = PROFILE_SLOTS.map(s => [s, headProfile(window, s)]).filter(([, p]) => p)
      if (heads.length > 0) break
      await H.sleep(250)
    }
    const waited = ((5000 - (deadline - Date.now())) / 1000).toFixed(1)
    if (!r.assert(heads.length > 0, 'na liscie pojawia sie glowa z wlasnym profilem skorki',
      'przez 5 s zadna glowa nie dostala komponentu profile — to dokladnie objaw regresji '
      + '(glowy Steve\'a przy pierwszym otwarciu)')) {
      return
    }
    r.assert(true, 'glowa dostala skorke po ' + waited + ' s od otwarcia ekranu')
    const [slot, profile] = heads[0]
    r.assert(profile.name === 'cc_ork_mag',
      'glowa postaci nosi profil jej skorki (cc_ork_mag)',
      'slot ' + slot + ' ma profil ' + profile.name)
    const textures = (profile.properties || []).find(p => p.name === 'textures')
    r.assert(!!textures && textures.value === skins.ork_mag.value,
      'tekstura glowy to dokladnie skorka ork_mag z skins.yml',
      textures ? textures.value.slice(0, 24) + '…' : 'brak wlasnosci textures')

    // Druga polowa: glowa ma tam ZOSTAC. Przerysowanie okna przez MMOProfiles/MythicLib
    // (placeholdery ida z bazy asynchronicznie) jest jedyna rzecza, ktora moze ja skasowac
    // po naszych dwoch probach — i stalo by sie to bez sladu w logu.
    await H.sleep(4000)
    const after = headProfile(bot.currentWindow || list, slot)
    r.assert(!!after && after.name === 'cc_ork_mag',
      'glowa NADAL ma skorke 4 s pozniej (przerysowanie okna jej nie kasuje)',
      after ? 'profil ' + after.name : 'glowa wrocila do domyslnej')
  } finally { await H.quit(bot) }
}
