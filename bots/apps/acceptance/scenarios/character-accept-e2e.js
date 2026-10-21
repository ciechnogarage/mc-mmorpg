'use strict';

// Etap 6, scenariusz: character-accept-e2e (CoreTools + MMOCore, domena "items").
//
// Cel: swiezy gracz przechodzi cala sciezke kreatora postaci v2 przez
// PRAWDZIWE klikniecia w GUI (nie /corecast) — wybor rasy, wybor klasy,
// klikniecie Accept — i konczy w CITY_ONBOARDING zamiast w
// creator_already_finalized.
//
// Kontekst (2026-07-15): root cause historycznego blockera byl operator="="
// zamiast operator="==" w Comparatorach CoreTools (98 wystapien w 3 plikach
// character-creator). Naprawione + zweryfikowane przez /corecast, ale
// oryginalny bug report dotyczyl realnego GUI flow, wiec ten scenariusz
// domyka dowod na wlasciwej sciezce.
//
// Sloty GUI (plugins/CoreTools/MenuCreator/foundation_creator_intro.yml,
// size: 6, top inventory 0-53):
//   race_czlowiek=19 race_ork=20 race_elf=21 race_krasnolud=22 race_nieumarly=23
//   class_warrior=28 class_rogue=29 class_marksman=30 class_mage=31 class_paladin=32
//   accept=50 reset=49 back=51
//
// Kontekst (2026-07-20): flow wejscia postaci zostal przeprojektowany. Join
// stawia gracza w scenie CharacterStage (postumenty, intercept_join=true).
// MMOProfiles ma default-profile=true, wiec swiezy gracz dostaje juz
// auto-utworzony profil "Main" ZANIM interakcja z postumentem sie odbedzie —
// slot 1 jest wiec zawsze "occupied", nie "empty". Klik postumentu odpala
// occupied_actions ("mmoprofiles open %player%"), ktore otwiera realne GUI
// MMOProfiles (plugins/MMOProfiles/language/gui/profile-list.yml, 45 slotow,
// tez nazwane "Kompania Bohaterow" — stad kolizja z nazwa sceny CharacterStage).
// Profil "Main" siedzi w slocie 10 (wariant `current`, PLAYER_HEAD). Klik na
// niego re-selectuje profil -> odpala natywny ProfileSelectedEvent MMOProfiles
// -> nowy trigger PlayerProfileSelectedCreatorResume (CoreTools EventsManager)
// dopiero wtedy otwiera foundation_creator_intro (dane profilu sa juz
// zaladowane, wiec %mmocore_class% jest poprawne — usuwa race condition,
// ktora psula stary PlayerJoinCreatorResume na samym PlayerJoinEvent).
//
// Uzycie: node apps/acceptance/runner.js character-accept-e2e
//   [--user qa_char_accept] [--race czlowiek] [--class paladin]

const { BotSession, sleep, waitFor } = require('@mmorpg-bots/bot-core');
const bridge = require('@mmorpg-bots/mmo-bridge');
const { runScenario, assertTruthy } = require('@mmorpg-bots/qa');
const { clickSlot, awaitStableWindow } = require('@mmorpg-bots/skills/src/gui');
const { gotoXYZ } = require('@mmorpg-bots/skills/src/move');
const { Vec3 } = require('vec3');

const RACE_SLOTS = { czlowiek: 19, ork: 20, elf: 21, krasnolud: 22, nieumarly: 23 };
const CLASS_SLOTS = { warrior: 28, rogue: 29, marksman: 30, mage: 31, paladin: 32 };
const ACCEPT_SLOT = 50;
const PROFILE_LIST_MAIN_SLOT = 10;

function arg(name, fallback) {
  const idx = process.argv.indexOf(`--${name}`);
  return idx !== -1 && process.argv[idx + 1] ? process.argv[idx + 1] : fallback;
}

async function run(argv = []) {
  // Nick MC ma limit 16 znaków — "qa_char_accept_NNNNN" (do 20) był ucinany przez
  // Velocity jako nieprawidłowy (socketClosed → spawn timeout). Trzymamy się ≤16.
  const username = arg('user', `qa_char_${Date.now() % 100000}`);
  const race = arg('race', 'czlowiek');
  const klass = arg('class', 'paladin');
  const domain = 'items';

  let session;
  const messages = [];
  const disconnect = async () => {
    if (session) { try { await session.stop(); } catch { /* best effort */ } }
  };
  process.on('exit', () => { if (session) session.stop().catch(() => {}); });

  return runScenario({
    name: 'character-accept-e2e',
    steps: [
      {
        name: 'swiezy bot dolacza przez Velocity na domene items',
        critical: true,
        run: async () => {
          session = new BotSession({ username, reconnect: false, spawnTimeoutMs: 60000, log: { info: () => {}, warn: () => {} } });
          await session.connect();
          session.bot.on('messagestr', text => messages.push(text));
          // Velocity already routes this acceptance runner to `items`.
          // Asking it to switch to the backend it is currently on is rejected
          // with "You are already connected to this server!" and interrupts
          // the creator flow before its GUI assertions can run.
          await sleep(2000);
          return { username };
        }
      },
      {
        name: 'interakcja z postumentem pierwszego slotu otwiera GUI profili MMOProfiles',
        critical: true,
        run: async () => {
          await gotoXYZ(session.bot, 185, 63, 209, 3);
          const pedestal = session.bot.blockAt(new Vec3(185, 63, 209));
          assertTruthy(pedestal, 'postument slotu 1 istnieje');
          await session.bot.activateBlock(pedestal);
          await sleep(500);
          await session.bot.activateBlock(pedestal);
          const profileList = await waitFor(async () => {
            const current = session.bot.currentWindow;
            return current && current.inventoryStart === 45 ? current : null;
          }, 10000, 250);
          return { windowTitle: profileList.title || null, slotCount: profileList.slots.length };
        }
      },
      {
        name: `klikniecie profilu "Main" (slot ${PROFILE_LIST_MAIN_SLOT}) re-selectuje i otwiera kreator postaci`,
        critical: true,
        run: async () => {
          await clickSlot(session.bot, await awaitStableWindow(session.bot.currentWindow), PROFILE_LIST_MAIN_SLOT);
          const creator = await waitFor(async () => {
            const current = session.bot.currentWindow;
            return current && current.inventoryStart === 54 ? current : null;
          }, 10000, 250);
          return { windowTitle: creator.title || null, slotCount: creator.slots.length };
        }
      },
      {
        name: `klikniecie rasy (${race}, slot ${RACE_SLOTS[race]})`,
        critical: true,
        run: async () => {
          assertTruthy(RACE_SLOTS[race] !== undefined, `nieznana rasa "${race}"`);
          await clickSlot(session.bot, await awaitStableWindow(session.bot.currentWindow), RACE_SLOTS[race]);
          await sleep(1200);
          return { clickedSlot: RACE_SLOTS[race] };
        }
      },
      {
        name: `klikniecie klasy (${klass}, slot ${CLASS_SLOTS[klass]})`,
        critical: true,
        run: async () => {
          assertTruthy(CLASS_SLOTS[klass] !== undefined, `nieznana klasa "${klass}"`);
          await clickSlot(session.bot, await awaitStableWindow(session.bot.currentWindow), CLASS_SLOTS[klass]);
          await sleep(1200);
          return { clickedSlot: CLASS_SLOTS[klass] };
        }
      },
      {
        name: 'klikniecie Accept (slot 50) finalizuje postac',
        critical: true,
        run: async () => {
          await clickSlot(session.bot, await awaitStableWindow(session.bot.currentWindow), ACCEPT_SLOT);
          await sleep(2500);
          const joined = messages.join(' | ');
          if (joined.includes('już zapisaną pieczęć i przysięgę')) {
            throw new Error(`REGRESJA: gracz wylądował w creator_already_finalized mimo świeżego profilu. Wiadomości: ${joined}`);
          }
          if (!joined.includes('Pieczęć i przysięga zapisane')) {
            throw new Error(`Brak oczekiwanego komunikatu sukcesu finalize. Wiadomości: ${joined}`);
          }
          return { messages: messages.slice() };
        }
      },
      {
        name: 'weryfikacja server-side przez RCON: klasa nie jest juz Origin',
        critical: true,
        run: async () => {
          const raw = await bridge.rcon(`papi parse ${username} %mmocore_class%`, { domain });
          const value = raw.replace(/\[[0-9;]*m/g, '').trim();
          if (value === 'Origin' || value === '') {
            throw new Error(`%mmocore_class% wciaz "${value}" po Accept — finalize nie zadzialal server-side`);
          }
          return { mmocore_class: value };
        }
      }
    ]
  }).finally(disconnect);
}

module.exports = { run };

if (require.main === module) {
  run(process.argv.slice(2)).then(result => {
    console.log(`Werdykt: ${result.verdict}`);
    process.exit(result.verdict === 'PASS' ? 0 : 1);
  });
}
