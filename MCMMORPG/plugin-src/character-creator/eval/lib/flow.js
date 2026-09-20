'use strict'
/** Kroki sciezki gracza, wspolne dla scenariuszy. Sloty wziete z ZYWEGO serwera, nie z kodu. */
const H = require('./harness')

const PROFILE_TITLE = 'Choose Your Character'
const CREATOR_TITLE = 'Imię'
const SLOT_CREATE_PROFILE = 10
const SLOT_NAME = 4
const SLOT_CONFIRM = 58
const RACE_SLOTS = { czlowiek: 0, ork: 18, elf: 27, nieumarly: 45 }
const CLASS_SLOTS = { wojownik: 8, mag: 26, akolita: 35, ninja: 53 }
const ROOM = { x: 0.5, y: 100, z: 0.5 }

const isProfileWindow = bot =>
  !!bot.currentWindow && H.windowTitle(bot.currentWindow).includes(PROFILE_TITLE)
const isCreatorWindow = bot =>
  !!bot.currentWindow && H.windowTitle(bot.currentWindow).includes(CREATOR_TITLE)

async function waitProfileScreen(bot, timeout = 25000) {
  return H.waitForWindow(bot, { timeout, titleIncludes: PROFILE_TITLE })
}

async function enterCreator(bot, timeout = 25000) {
  const w = await waitProfileScreen(bot, timeout)
  if (!w) return null
  await H.click(bot, SLOT_CREATE_PROFILE)
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    if (isCreatorWindow(bot)) return bot.currentWindow
    await H.sleep(300)
  }
  return null
}

/** Przechodzi kreator do konca. Imie bierzemy PRAWYM przyciskiem (losowe) — lewy otwiera
 *  dialog Papera, ktorego bot protokolowy nie obsluzy, a losowanie jest ta sama sciezka. */
async function completeCreator(bot, { race = 'ork', klass = 'wojownik' } = {}) {
  await H.click(bot, RACE_SLOTS[race])
  await H.click(bot, CLASS_SLOTS[klass])
  await bot.clickWindow(SLOT_NAME, 1, 0)   // prawy przycisk = losowe imie
  await H.sleep(800)
  await H.click(bot, SLOT_CONFIRM)
  // Ceremonia trwa 2.25 s, potem teleport wyjsciowy.
  await H.sleep(6000)
}

const inRoom = bot => {
  const p = bot.entity.position
  return Math.abs(p.x - ROOM.x) < 3 && Math.abs(p.y - ROOM.y) < 3 && Math.abs(p.z - ROOM.z) < 3
}

const itemCounts = bot => {
  const out = {}
  for (const it of bot.inventory.items()) out[it.name] = (out[it.name] || 0) + it.count
  return out
}

const totalItems = bot => bot.inventory.items().reduce((a, i) => a + i.count, 0)

module.exports = {
  PROFILE_TITLE, CREATOR_TITLE, SLOT_CREATE_PROFILE, SLOT_NAME, SLOT_CONFIRM,
  RACE_SLOTS, CLASS_SLOTS, ROOM, isProfileWindow, isCreatorWindow, waitProfileScreen,
  enterCreator, completeCreator, inRoom, itemCounts, totalItems,
}
