'use strict'
// Partia 14. Uruchamiać wyłącznie na klonie world1 po skopiowaniu Test_Napastnik.yml
// do plugins/MythicMobs/mobs/ i /mm reload. TYLKO=1,4,16 ogranicza numery jak w spec-e2e.js.
// Wszystkie komendy (także party) idą przez RCON; boty wysyłają tylko pakiety F/slot.
const mineflayer = require('/g/node_modules/mineflayer')
const { attach } = require('/e/lib/resourcepack.js')
const { rcon } = require('/s/rcon.js')
const Vec3 = require('/g/node_modules/vec3')
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))

const KLASY = [
  ['czlowiek','wojownik','RYCERZ','BASTION','CZEMPION','WOJ_PCHNIECIE_KORONY','WOJ_ROZPLATANIE','WOJ_OKRZYK','WOJ_GNIEW'],
  ['czlowiek','ninja','AGENT','WIDMO','JASTRZAB','NIN_BELT_LISTU','NIN_CIECIE','NIN_KROK','NIN_EGZEKUCJA'],
  ['czlowiek','mag','MAG_AKADEMII','ELEMENTALISTA','PRZYWOLYWACZ','MAG_PIECZEC_WIEZY','MAG_BARIERA','MAG_WYBUCH','MAG_MRUGNIECIE'],
  ['czlowiek','akolita','SWIETY_AKOLITA','SERAFIN','HIEROFANT','AKO_STRAZ_RELIKWII','AKO_ZNIWO','AKO_OPIEKA','AKO_ROZKAZ'],
  ['elf','wojownik','OSTRZE_GAJU','STRAZNIK_CIERNI','TANCERZ_OSTRZY','WOJ_PRZELOT_GAJU','WOJ_ROZPLATANIE','WOJ_OKRZYK','WOJ_GNIEW'],
  ['elf','ninja','STRAZNIK_KNIEI','CIEN_LISCI','SOKOL','NIN_STRZALA_KORONY','NIN_CIECIE','NIN_KROK','NIN_EGZEKUCJA'],
  ['elf','mag','TKACZ_ARKANOW','TKACZ_ZYWIOLOW','STRAZNIK_ZYWIOLOW','MAG_NIC_ARKANOW','MAG_BARIERA','MAG_WYBUCH','MAG_MRUGNIECIE'],
  ['elf','akolita','WYROCZNIA','OSTRZE_KSIEZYCA','OPIEKUN_DUCHOW','AKO_SIERP_KSIEZYCA','AKO_ZNIWO','AKO_OPIEKA','AKO_ROZKAZ'],
  ['ork','wojownik','WODZ_WOJNY','TYRAN','KRWIOBURCA','WOJ_NATARCIE_WODZA','WOJ_ROZPLATANIE','WOJ_OKRZYK','WOJ_GNIEW'],
  ['ork','ninja','TROPICIEL','LOWCA_BESTII','LOWCA_KOSCI','NIN_SLAD_KLA','NIN_CIECIE','NIN_KROK','NIN_EGZEKUCJA'],
  ['ork','mag','SZAMAN','BURZOWNIK','TOTEMISTA','MAG_OKO_BURZY','MAG_BARIERA','MAG_WYBUCH','MAG_MRUGNIECIE'],
  ['ork','akolita','KAPLAN_KRWI','AWATAR_WOJNY','SZEPT_PRZODKOW','AKO_PRZODEK_MLOTA','AKO_ZNIWO','AKO_OPIEKA','AKO_ROZKAZ'],
  ['nieumarly','wojownik','RYCERZ_SMIERCI','STRAZNIK_GROBU','ZNIWIARZ','WOJ_MARSZ_KRYPTY','WOJ_ROZPLATANIE','WOJ_OKRZYK','WOJ_GNIEW'],
  ['nieumarly','ninja','WIDMO_GROBU','CIEN','STRZELEC_GROBU','NIN_PETLA_GROBU','NIN_CIECIE','NIN_KROK','NIN_EGZEKUCJA'],
  ['nieumarly','mag','MAG_GROBU','DUSZOZERCA','NEKROMANTA','MAG_SPLOT_DUSZY','MAG_BARIERA','MAG_WYBUCH','MAG_MRUGNIECIE'],
  ['nieumarly','akolita','SPACZONY_AKOLITA','AWATAR_OTCHLANI','PASTERZ_UMARLYCH','AKO_KOSIARZ_CISZY','AKO_ZNIWO','AKO_OPIEKA','AKO_ROZKAZ']
]
const SPEC = [
  ['SPEC_BASTION_MUR','SPEC_CZEMPION_WYZWANIE'],['SPEC_WIDMO_JAD','SPEC_JASTRZAB_LIST'],
  ['SPEC_ELEMENTALISTA_KRAG','SPEC_PRZYWOLYWACZ_ECHO'],['SPEC_SERAFIN_ZASLONA','SPEC_HIEROFANT_CHORAGIEW'],
  ['SPEC_STRAZNIK_CIERNI_MUR','SPEC_TANCERZ_CIECIE'],['SPEC_CIEN_LISCI_SIDLA','SPEC_SOKOL_DESZCZ'],
  ['SPEC_TKACZ_ZYWIOLOW_REFREN','SPEC_STRAZNIK_ZYWIOLOW_ZWIERCIADLO'],['SPEC_OSTRZE_KSIEZYCA_LUK','SPEC_OPIEKUN_DUCHOW_SWIT'],
  ['SPEC_TYRAN_ROZKAZ','SPEC_KRWIOBURCA_SZAL'],['SPEC_LOWCA_BESTII_WILK','SPEC_LOWCA_KOSCI_SALWA'],
  ['SPEC_BURZOWNIK_GROM','SPEC_TOTEMISTA_RYTM'],['SPEC_AWATAR_WOJNY_MLOT','SPEC_SZEPT_PRZODKOW_WEZWANIE'],
  ['SPEC_STRAZNIK_GROBU_PROG','SPEC_ZNIWIARZ_CIECIE'],['SPEC_CIEN_PETLA','SPEC_STRZELEC_GROBU_STRZALA'],
  ['SPEC_DUSZOZERCA_WIEZ','SPEC_NEKROMANTA_ZARAZA'],['SPEC_AWATAR_OTCHLANI_GLOD','SPEC_PASTERZ_UMARLYCH_GLOS']
]
const NAZWY = {
  AKO_OPIEKA:'Opieka',AKO_STRAZ_RELIKWII:'Straż Relikwii',WOJ_OKRZYK:'Okrzyk',
  SPEC_SERAFIN_ZASLONA:'Zasłona Relikwii',SPEC_HIEROFANT_CHORAGIEW:'Chorągiew Warty',
  SPEC_OPIEKUN_DUCHOW_SWIT:'Ścieżka Ogników',SPEC_TOTEMISTA_RYTM:'Rytm Przodków',
  SPEC_BASTION_MUR:'Mur Przysięgi',SPEC_TYRAN_ROZKAZ:'Żelazny Rozkaz'
}
const CZAS = {WOJ_OKRZYK:20,AKO_STRAZ_RELIKWII:10,AKO_OPIEKA:6,AKO_ZNIWO:8,
  AKO_SIERP_KSIEZYCA:10,AKO_PRZODEK_MLOTA:8,AKO_KOSIARZ_CISZY:8,AKO_ROZKAZ:3,
  SPEC_BASTION_MUR:16,SPEC_TYRAN_ROZKAZ:17}
const ZASOB = {wojownik:100,ninja:100,mag:200,akolita:10}
const GRACZ = {x:300.5,y:-59,z:300.5}
const SOJUSZNIK = {x:302.5,y:-59,z:302.736} // dokładnie 3 bl. od gracza, 2.02 od manekina
const MANEKIN = {x:300.5,y:-59,z:303}
const NAPASTNIK = {x:303.6,y:-59,z:303.2} // bliżej sojusznika niż gracza
const ARENA = 'x=286,y=-63,z=286,dx=28,dy=12,dz=28'
const CEL = '@e[tag=para_manekin,limit=1]'
const WROG = '@e[tag=para_napastnik,limit=1]'
const TYLKO = new Set((process.env.TYLKO || '').split(',').map(s => s.trim()).filter(Boolean))
const ostatniRzut = new Map()
const porazki = []
let pass = 0, fail = 0, kompan
let naturalnaRegeneracja = 'true'

function wynik(warunek, opis, szczegoly) {
  const linia = `${warunek ? 'PASS' : 'FAIL'} ${opis} — ${szczegoly}`
  console.log(linia)
  if (warunek) pass++
  else { fail++; porazki.push(linia) }
}
function liczba(tekst) {
  const m = /(-?\d+(?:\.\d+)?)(?:[bdfsL])?\s*$/.exec(String(tekst).trim())
  return m ? Number(m[1]) : NaN
}
function pozycja(tekst) {
  const m = /\[\s*(-?[\d.]+)d?,\s*(-?[\d.]+)d?,\s*(-?[\d.]+)d?\s*\]/.exec(tekst)
  return m ? {x:+m[1],y:+m[2],z:+m[3]} : null
}
function odleglosc(a,b) { return a && b ? Math.hypot(a.x-b.x,a.z-b.z) : NaN }
const n = async (entity, field) => liczba(await rcon(`data get entity ${entity} ${field}`))
const str = async (entity, field) => rcon(`data get entity ${entity} ${field}`)
const pos = async entity => pozycja(await str(entity,'Pos'))
const efekt = (surowe, id) => new RegExp(`(?:minecraft:)?${id}(?:\\"|\\')`, 'i').test(surowe)
async function ile(selector) {
  const s = await rcon(`execute if entity ${selector}`)
  const m = /(?:count|liczba|entity|entities|byt(?:y|ów)?):?\s*(\d+)\s*$/i.exec(s)
  if (m) return +m[1]
  return /test passed|udany|znaleziono/i.test(s) ? 1 : 0
}
async function sprzataj(nick) {
  await rcon(`minecraft:kill @e[type=!player,${ARENA}]`)
  await rcon(`effect clear ${nick}`)
  await rcon(`effect clear ${kompan}`)
  // Osłony MythicMobs mogą trwać jeszcze po /effect clear; nie przenoś ich do nowej próby.
  if ((await n(nick,'AbsorptionAmount')) > 0 || (await n(kompan,'AbsorptionAmount')) > 0) await sleep(8500)
  await rcon(`minecraft:tp ${nick} ${GRACZ.x} ${GRACZ.y} ${GRACZ.z} 0 0`)
  await rcon(`minecraft:tp ${kompan} ${SOJUSZNIK.x} ${SOJUSZNIK.y} ${SOJUSZNIK.z} 0 0`)
  await rcon(`mmocore admin resource-health set ${nick} 15`)
  await rcon(`mmocore admin resource-health set ${kompan} 20`)
  await sleep(750)
}
async function manekin() {
  await rcon(`mm mobs spawn TEST_MANEKIN 1 world,${MANEKIN.x},${MANEKIN.y},${MANEKIN.z}`)
  await sleep(500)
  await rcon(`tag @e[type=zombie,x=${MANEKIN.x},y=${MANEKIN.y},z=${MANEKIN.z},distance=..1,sort=nearest,limit=1] add para_manekin`)
  await rcon(`execute as ${CEL} run data merge entity @s {NoAI:1b,Health:60.0f}`)
  const hp = await n(CEL,'Health')
  if (!(hp > 0)) throw new Error('TEST_MANEKIN nie został wczytany z Test_Napastnik.yml')
}
async function napastnik() {
  await rcon(`mm mobs spawn TEST_NAPASTNIK 1 world,${NAPASTNIK.x},${NAPASTNIK.y},${NAPASTNIK.z}`)
  await sleep(450)
  await rcon(`tag @e[type=zombie,tag=!para_manekin,x=${NAPASTNIK.x},y=${NAPASTNIK.y},z=${NAPASTNIK.z},distance=..1,sort=nearest,limit=1] add para_napastnik`)
  if (!(await n(WROG,'Health') > 0)) throw new Error('TEST_NAPASTNIK nie został wczytany z Test_Napastnik.yml')
}
async function probka(nick) {
  const [hp, hpK, hpS, abs, absS, ef, efS, efCel, mana, p, pK, sludzy] = await Promise.all([
    n(CEL,'Health'),n(nick,'Health'),n(kompan,'Health'),n(nick,'AbsorptionAmount'),n(kompan,'AbsorptionAmount'),
    str(nick,'active_effects'),str(kompan,'active_effects'),str(CEL,'active_effects'),rcon(`papi parse ${nick} %mmocore_mana%`),
    pos(nick),pos(kompan),ile(`@e[type=!player,tag=!para_manekin,tag=!para_napastnik,${ARENA}]`)
  ])
  return {hp,hpK,hpS,abs,absS,ef,efS,efCel,mana:liczba(mana),p,pK,sludzy}
}
function bot(nick) {
  const b = mineflayer.createBot({host:process.env.MC_HOST,username:nick,auth:'offline',version:'1.21.11'})
  attach(b._client)
  return b
}
async function wejscie(b) { await new Promise((resolve,reject) => { b.once('spawn',resolve); b.once('error',reject) }); await sleep(5000) }
const F = b => b._client.write('block_dig',{status:6,location:{x:0,y:0,z:0},face:0,sequence:0})
async function rzut(b,klasa,skill,slot,cel) {
  const nick = b.username, klucz = `${nick}:${skill}`
  const pozostalo = (CZAS[skill] || 23)*1000 + 1200 - (Date.now()-(ostatniRzut.get(klucz) || 0))
  if (pozostalo > 0) await sleep(pozostalo)
  await b.lookAt(new Vec3(cel.x,cel.y+1.2,cel.z),true)
  await sleep(200)
  const bron = b.quickBarSlot, wolne = [...Array(9).keys()].filter(s => s !== bron)
  console.log(`KADR ${nick} ${klasa} ${skill}`)
  F(b); await sleep(300); b.setQuickBarSlot(wolne[slot-1]); await sleep(300)
  F(b); ostatniRzut.set(klucz,Date.now()); await sleep(300)
  if (b.quickBarSlot !== bron) b.setQuickBarSlot(bron)
  await sleep(1500) // pomiar dopiero po kadrze, z zapasem na zrzut ~0,8 s
}
async function ustawDruzyne(nick, graczBot, allyBot) {
  // MMOCore ma party-view i party-creation; jego komendy wykonywane jako gracze przez RCON.
  // Jeśli składnia party na klonie jest inna, drugi bot i zespół vanilla nadal działają,
  // a pomiar wsparcia uczciwie pokaże FAIL zamiast deklarować nieistniejącą ochronę.
  await rcon('team add para_test')
  await rcon('team modify para_test friendlyFire false')
  await rcon(`team join para_test ${nick}`)
  await rcon(`team join para_test ${kompan}`)
  const zespol = await rcon('team list para_test')
  wynik(zespol.includes(nick) && zespol.includes(kompan),`${nick} i ${kompan}: wspólny zespół`,zespol.trim().slice(0,120))
  // Drużyna MMOCore tylko z poziomu gracza (konsola: "not a valid choice"); sprawdzone na klonie 02.10:
  // /party -> okno "Party Creation" slot 12 = utwórz; okno "Party (n/8)" slot 48 = zaproś (nick na czacie), slot 50 = opuść;
  // zaproszony: /party accept.
  const zamknij = async b => { try { if (b.currentWindow) b.closeWindow(b.currentWindow) } catch {} ; await sleep(400) }
  for (const b of [allyBot, graczBot]) { b.chat('/party'); await sleep(1500); const t = JSON.stringify(b.currentWindow && b.currentWindow.title || ''); if (/Party \(/.test(t)) { await b.clickWindow(50, 0, 0); await sleep(1200) } await zamknij(b) }
  graczBot.chat('/party'); await sleep(1500); if (graczBot.currentWindow) { await graczBot.clickWindow(12, 0, 0); await sleep(1500) }
  if (graczBot.currentWindow) { await graczBot.clickWindow(48, 0, 0); await sleep(1200); graczBot.chat(kompan); await sleep(2000) }
  await zamknij(graczBot); allyBot.chat('/party accept'); await sleep(2000)
  const ile = (await rcon(`papi parse ${kompan} %mmocore_party_count%`)).trim()
  wynik(ile === '2', `${nick} i ${kompan}: wspólna drużyna MMOCore (wsparcie przy włączonym PvP działa tylko w drużynie)`, `członków: ${ile}`)
}
const LECZENIE = new Set(['AKO_OPIEKA','SPEC_OPIEKUN_DUCHOW_SWIT'])
const OSLONA = new Set(['SPEC_SERAFIN_ZASLONA','SPEC_HIEROFANT_CHORAGIEW','SPEC_TOTEMISTA_RYTM'])
const PROWOKACJA = new Set(['SPEC_BASTION_MUR','SPEC_TYRAN_ROZKAZ'])
const BEZ_OBRAZEN = new Set(['WOJ_OKRZYK','MAG_BARIERA','MAG_MRUGNIECIE','AKO_STRAZ_RELIKWII','AKO_OPIEKA','AKO_ROZKAZ',
  'SPEC_BASTION_MUR','SPEC_SERAFIN_ZASLONA','SPEC_HIEROFANT_CHORAGIEW','SPEC_OPIEKUN_DUCHOW_SWIT',
  'SPEC_TYRAN_ROZKAZ','SPEC_TOTEMISTA_RYTM','SPEC_STRAZNIK_ZYWIOLOW_ZWIERCIADLO'])
async function przygotujSlugi(b, baza, arch, skill) {
  const id = baza[0]
  if (arch !== 'akolita') return
  const potrzebny = skill === 'AKO_ZNIWO' || skill === 'AKO_ROZKAZ' || skill === 'SPEC_SERAFIN_ZASLONA' ||
    skill === 'SPEC_HIEROFANT_CHORAGIEW' || skill === 'SPEC_OPIEKUN_DUCHOW_SWIT'
  if (!potrzebny) return
  await rcon(`mmocore admin resource-mana set ${b.username} 10`)
  await rzut(b,'PRZYGOTOWANIE',id,1,MANEKIN)
  await sleep(450)
}
async function sprawdzRzut(b,klasa,arch,skill,slot,baza,pas) {
  const nick = b.username, nazwa = NAZWY[skill] || skill
  await sprzataj(nick)
  await manekin()
  await przygotujSlugi(b,baza,arch,skill)
  const hpPrzedNapastnikiem = PROWOKACJA.has(skill) ? await n(kompan,'Health') : NaN
  if (PROWOKACJA.has(skill)) await napastnik()
  const leczy = LECZENIE.has(skill)
  if (leczy) await rcon(`mmocore admin resource-health set ${kompan} 10`)
  await rcon(`mmocore admin resource-mana set ${nick} ${ZASOB[arch]}`)
  const celRzutu = skill === 'AKO_OPIEKA' ? SOJUSZNIK : MANEKIN
  if (PROWOKACJA.has(skill)) await sleep(3300) // AI musi najpierw obrać bliższego SOJUSZNIKA
  const przed = await probka(nick)
  const przedWrog = PROWOKACJA.has(skill) ? await pos(WROG) : null
  if (PROWOKACJA.has(skill))
    wynik(przed.hpS < hpPrzedNapastnikiem,
      `${nazwa}: napastnik najpierw naciera na sojusznika`,
      `HP ${hpPrzedNapastnikiem}->${przed.hpS}, odległość ${odleglosc(przedWrog,przed.pK).toFixed(1)} bl.`)
  await rzut(b,klasa,skill,slot,celRzutu)
  const po = await probka(nick)
  const zmiany = []
  if (po.hp < przed.hp) zmiany.push(`HP manekina ${przed.hp} -> ${po.hp}`)
  if (po.abs > przed.abs) zmiany.push(`osłona gracza ${przed.abs} -> ${po.abs}`)
  if (po.absS > przed.absS) zmiany.push(`osłona sojusznika ${przed.absS} -> ${po.absS}`)
  if (po.hpS > przed.hpS) zmiany.push(`HP sojusznika ${przed.hpS} -> ${po.hpS}`)
  if (po.sludzy !== przed.sludzy) zmiany.push(`słudzy ${przed.sludzy} -> ${po.sludzy}`)
  if (po.ef !== przed.ef) zmiany.push('efekt gracza')
  if (po.efS !== przed.efS) zmiany.push('efekt sojusznika')
  if (po.efCel !== przed.efCel) zmiany.push('efekt manekina')
  if (odleglosc(po.p,przed.p) > 0.8) zmiany.push('przemieszczenie gracza')
  wynik(zmiany.length > 0,`${klasa} ${nazwa}: widoczny skutek`,zmiany.join(', ') || 'brak zmiany w RCON')
  if (arch !== 'wojownik' && !['AKO_ZNIWO','AKO_ROZKAZ'].includes(skill))
    wynik(po.mana < przed.mana,`${nazwa} pobiera zasób`,`${przed.mana} -> ${po.mana}`)
  wynik(Boolean(pas) && !/No Skill|%mmocore_bound_/i.test(pas),`${klasa} slot ${slot}: ${nazwa} na pasku`,pas || 'pusty slot')
  if (!BEZ_OBRAZEN.has(skill)) {
    wynik(po.hpS >= przed.hpS - 0.01,`${nazwa} nie rani sojusznika`,`${przed.hpS} -> ${po.hpS} HP; gracz i cel w zasięgu strefy`)
  }
  if (skill === 'AKO_OPIEKA' || skill === 'SPEC_OPIEKUN_DUCHOW_SWIT') {
    // Elfie/Świtowe leczenie ma kolejne tyki; obserwuj do 6,5 s od rzutu.
    if (po.hpS <= przed.hpS) { await sleep(4900); po.hpS = await n(kompan,'Health') }
    wynik(po.hpS > przed.hpS,`${nazwa} leczy sojusznika`,`${przed.hpS} -> ${po.hpS} HP`)
  }
  if (skill === 'AKO_STRAZ_RELIKWII') {
    const golem = await ile(`@e[type=iron_golem,x=${SOJUSZNIK.x},y=${SOJUSZNIK.y},z=${SOJUSZNIK.z},distance=..4]`)
    wynik(golem > 0,`${nazwa} stawia obrońcę przy sojuszniku`,`obrońcy przy sojuszniku: ${golem}`)
  }
  if (OSLONA.has(skill)) {
    wynik(po.absS > przed.absS,`${nazwa} osłania sojusznika`,`${przed.absS} -> ${po.absS} HP osłony`)
  }
  if (skill === 'WOJ_OKRZYK') {
    wynik(efekt(po.efS,'strength'),`${nazwa} wzmacnia sojusznika`,`Strength: ${efekt(po.efS,'strength')}`)
    if (baza[0] === 'WOJ_PCHNIECIE_KORONY')
      wynik(efekt(po.efS,'resistance'),`${nazwa} daje sojusznikowi odporność`,`Resistance: ${efekt(po.efS,'resistance')}`)
  }
  if (skill === 'SPEC_TOTEMISTA_RYTM') {
    wynik(efekt(po.efS,'speed'),`${nazwa} przyspiesza sojusznika`,`Speed: ${efekt(po.efS,'speed')}`)
  }
  if (skill === 'SPEC_HIEROFANT_CHORAGIEW') {
    const sluga = '@e[type=!player,type=!zombie,tag=!para_manekin,tag=!para_napastnik,x=300,y=-59,z=301,distance=..6,sort=nearest,limit=1]'
    const osl = await n(sluga,'AbsorptionAmount'), e = await str(sluga,'active_effects')
    wynik(osl > 0 && efekt(e,'speed'),`${nazwa} osłania i przyspiesza sługę`,`osłona ${osl}, Speed: ${efekt(e,'speed')}`)
  }
  if (skill === 'SPEC_SERAFIN_ZASLONA') {
    const sluga = '@e[type=iron_golem,x=300,y=-59,z=301,distance=..6,sort=nearest,limit=1]'
    const osl = await n(sluga,'AbsorptionAmount')
    wynik(osl > 0,`${nazwa} osłania obrońcę`,`osłona obrońcy: ${osl}`)
  }
  if (skill === 'SPEC_OPIEKUN_DUCHOW_SWIT') {
    const ognik = '@e[type=allay,x=302.5,y=-59,z=302.736,distance=..4,sort=nearest,limit=1]'
    const osl = await n(ognik,'AbsorptionAmount')
    wynik(osl > 0,`${nazwa} osłania ognik przy sojuszniku`,`osłona ognika: ${osl}`)
  }
  if (skill === 'SPEC_BASTION_MUR' || skill === 'SPEC_TYRAN_ROZKAZ')
    wynik(efekt(po.ef,'resistance'),`${nazwa} daje odporność graczowi`,`Resistance: ${efekt(po.ef,'resistance')}`)
  if (PROWOKACJA.has(skill)) {
    await sleep(2500) // razem ponad 4 s od rzutu
    const potem = await probka(nick), pW = await pos(WROG)
    const doGracza = odleglosc(pW,potem.p), doSojusznika = odleglosc(pW,potem.pK)
    const przedBlizejSojusznika = odleglosc(przedWrog,przed.pK) < odleglosc(przedWrog,przed.p)
    const zmienilCel = (potem.hpK < przed.hpK && potem.hpS >= przed.hpS - 0.01) ||
      (doGracza + 0.4 < doSojusznika)
    wynik(przedBlizejSojusznika && zmienilCel,`${nazwa} przejmuje napastnika od sojusznika`,
      `HP gracza ${przed.hpK}->${potem.hpK}, sojusznika ${przed.hpS}->${potem.hpS}; odległości ${doGracza.toFixed(1)}/${doSojusznika.toFixed(1)} bl.`)
  }
}

;(async () => {
  await rcon('forceload add 280 280 320 320')
  await rcon('execute in minecraft:overworld run place template przeciwnicy:arena_v1 286 -61 286')
  naturalnaRegeneracja = /\bfalse\b/i.test(await rcon('gamerule naturalRegeneration')) ? 'false' : 'true'
  await rcon('gamerule naturalRegeneration false')
  kompan = 'PA_Sojusznik'
  const ally = bot(kompan)
  await wejscie(ally)
  await rcon(`gamemode survival ${kompan}`)
  await rcon(`cc testpostac ${kompan} czlowiek wojownik`)
  await sleep(15000)
  for (let l=2;l<=10;l++) { await rcon(`mmocore admin level set ${kompan} main ${l}`); await sleep(700) }
  for (let i=0;i<KLASY.length;i++) {
    if (TYLKO.size && !TYLKO.has(String(i+1))) continue
    const [rasa,arch,bazowa,specA,specB,...baza] = KLASY[i]
    const nick = (`PA${i+1}${rasa.slice(0,3)}${arch.slice(0,3)}`).slice(0,16)
    const b = bot(nick)
    try {
      await wejscie(b)
      await rcon(`gamemode survival ${nick}`)
      await rcon(`cc testpostac ${nick} ${rasa} ${arch}`)
      await sleep(15000)
      for (let l=2;l<=10;l++) { await rcon(`mmocore admin level set ${nick} main ${l}`); await sleep(700) }
      await ustawDruzyne(nick, b, ally)
      const wybrana = (await rcon(`papi parse ${nick} %mmocore_class_id%`)).trim()
      wynik(wybrana === bazowa,`${nick}: klasa bazowa`,wybrana)
      for (let s=1;s<=4;s++) {
        const pas = (await rcon(`papi parse ${nick} %mmocore_bound_${s}%`)).trim()
        await sprawdzRzut(b,bazowa,arch,baza[s-1],s,baza,pas)
      }
      for (const [j,spec] of [specA,specB].entries()) {
        const odp = await rcon(`mmocore class select ${nick} ${spec}`)
        console.log('WYBOR',nick,spec,odp.trim().slice(0,100))
        await sleep(4000)
        await rcon(`mmocore admin level set ${nick} main 10`)
        await sleep(2500)
        const kl = (await rcon(`papi parse ${nick} %mmocore_class_id%`)).trim()
        wynik(kl===spec,`${nick}: specjalizacja ${spec}`,kl)
        const pas = (await rcon(`papi parse ${nick} %mmocore_bound_5%`)).trim()
        await sprawdzRzut(b,spec,arch,SPEC[i][j],5,baza,pas)
      }
    } catch (e) { wynik(false,`${nick}: błąd scenariusza`,e.message || String(e)) }
    finally { await sprzataj(nick).catch(()=>{}); b.quit(); await sleep(2000) }
  }
  await rcon(`minecraft:kill @e[type=!player,${ARENA}]`)
  await rcon(`effect clear ${kompan}`)
  await rcon(`gamerule naturalRegeneration ${naturalnaRegeneracja}`)
  await rcon('team remove para_test')
  ally.quit()
  console.log(`\nWYNIK: ${pass} PASS, ${fail} FAIL`)
  if (porazki.length) { console.log('LISTA FAIL:'); porazki.forEach(x=>console.log(x)) }
  process.exit(fail ? 1 : 0)
})().catch(async e => {
  console.error('BLAD',e)
  await rcon(`gamerule naturalRegeneration ${naturalnaRegeneracja}`).catch(()=>{})
  await rcon('team remove para_test').catch(()=>{})
  console.log(`WYNIK: ${pass} PASS, ${fail+1} FAIL`)
  console.log('LISTA FAIL:')
  porazki.forEach(x=>console.log(x))
  console.log(`FAIL błąd uruchomienia — ${e.message || e}`)
  process.exit(2)
})
