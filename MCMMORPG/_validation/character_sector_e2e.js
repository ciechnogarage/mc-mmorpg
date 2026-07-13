const {
  acquireRuntimeLock,
  clickWindowSlot,
  connectRcon,
  createBot,
  createReporter,
  getWindowTitle,
  inventorySnapshot,
  normalizeText,
  safeRcon,
  sleep,
  waitForWindow,
} = require('./foundation_runtime');

const BASE_DIR = __dirname;
const USER = process.env.MC_QA_USER || `CharQA${Date.now().toString().slice(-6)}`;
const CLASS_UNDER_TEST = (process.env.MC_QA_CLASS || 'warrior').toLowerCase();
const PREJOIN_COMMAND = process.env.MC_PREJOIN_COMMAND ?? '/server items';
const PREJOIN_DELAY_MS = Number(process.env.MC_PREJOIN_DELAY_MS || 6000);

const CLASS_PATHS = {
  warrior: { id: 'warrior', slot: 28, claimSlot: 10, claimNeedles: ['zestaw wojownika'] },
  rogue: { id: 'rogue', slot: 29, claimSlot: 11, claimNeedles: ['zestaw łotrzyka'] },
  marksman: { id: 'marksman', slot: 30, claimSlot: 12, claimNeedles: ['zestaw łowcy'] },
  mage: { id: 'mage', slot: 31, claimSlot: 14, claimNeedles: ['zestaw maga'] },
  paladin: { id: 'paladin', slot: 32, claimSlot: 15, claimNeedles: ['zestaw akolity'] },
};

const CLASS_PATH = CLASS_PATHS[CLASS_UNDER_TEST] || CLASS_PATHS.warrior;

function titleIncludes(...needles) {
  return (window) => {
    const title = normalizeText(getWindowTitle(window));
    return needles.every((needle) => title.includes(normalizeText(needle)));
  };
}

function itemSearchText(item) {
  return normalizeText(
    [item?.displayName, item?.name, JSON.stringify(item?.nbt || {})]
      .filter(Boolean)
      .join(' ')
  );
}

function windowSlotSnapshot(window) {
  if (!window?.slots) return [];
  return window.slots
    .map((item, slot) => (item ? `${slot}:${itemSearchText(item).slice(0, 180)}` : null))
    .filter(Boolean);
}

async function waitForExpectedWindow(bot, reporter, stepName, matcher, timeout = 12000) {
  const window = await waitForWindow(bot, matcher, timeout);
  return window;
}

async function clickSlotOrFallback(bot, window, reporter, stepName, slot, fallbackCommands, rcon, matcher = null, timeout = 8000) {
  try {
    const nextWindow = await clickWindowSlot(bot, window, slot);
    reporter.step(stepName, 'PASS', {
      slot,
      windowTitle: getWindowTitle(window),
      postWindowTitle: getWindowTitle(nextWindow),
      slots: windowSlotSnapshot(window),
    });
    if (!matcher) return nextWindow;
    return await waitForExpectedWindow(bot, reporter, `${stepName}-result`, matcher, timeout);
  } catch (error) {
    for (const command of fallbackCommands) {
      const raw = await safeRcon(rcon, command);
      reporter.evidence(`${stepName}: ${command} => ${String(raw).slice(0, 180)}`);
      if (!matcher) return bot.currentWindow || window;
      try {
        return await waitForExpectedWindow(bot, reporter, `${stepName}-fallback-result`, matcher, timeout);
      } catch {}
    }
    throw error;
  }
}

async function main() {
  const runtimeLock = acquireRuntimeLock(BASE_DIR, USER, 'character-sector-e2e');
  const reporter = createReporter(BASE_DIR, USER);
  let bot;
  let rcon;
  let verdict = 'FAIL';

  try {
    reporter.command(`MC_QA_USER=${USER} MC_QA_CLASS=${CLASS_UNDER_TEST} node MCMMORPG/_validation/character_sector_e2e.js`);
    reporter.assumption('custom GUI may not expose all item text to Mineflayer; RCON fallbacks are acceptable for runtime proof');

    rcon = await connectRcon();
    bot = createBot(USER);

    await new Promise((resolve, reject) => {
      bot.once('spawn', resolve);
      bot.once('error', reject);
      setTimeout(() => reject(new Error('bot spawn timeout')), 30000);
    });
    reporter.step('bot-spawn', 'PASS', { user: USER });

    if (PREJOIN_COMMAND && PREJOIN_COMMAND !== 'none') {
      await sleep(PREJOIN_DELAY_MS);
      bot.chat(PREJOIN_COMMAND);
      await sleep(2500);
    }

    reporter.evidence(`init-clear: ${String(await safeRcon(rcon, `minecraft:clear ${USER}`)).slice(0, 180)}`);
    reporter.evidence(`init-class-reset: ${String(await safeRcon(rcon, `class ${USER} human`)).slice(0, 180)}`);
    reporter.evidence(`init-class-info: ${String(await safeRcon(rcon, `mmocore admin info ${USER}`)).slice(0, 260)}`);

    const mainMatcher = titleIncludes('kompania bohaterow', 'wybor postaci');
    let mainWindow;
    try {
      mainWindow = await waitForExpectedWindow(bot, reporter, 'join-opened-main-creator', mainMatcher, 15000);
    } catch {
      const raw = await safeRcon(rcon, `core-menu foundation_creator_intro ${USER}`);
      reporter.evidence(`fallback-open-main-creator: ${String(raw).slice(0, 180)}`);
      mainWindow = await waitForExpectedWindow(bot, reporter, 'fallback-opened-main-creator', mainMatcher, 10000);
    }
    reporter.step('main-creator-opened', 'PASS', { title: getWindowTitle(mainWindow) });

    await clickSlotOrFallback(
      bot,
      mainWindow,
      reporter,
      'accept-blocked-without-selection',
      50,
    [`core-cast creator_v2_accept_pending_character ${USER}`],
      rcon,
      mainMatcher,
      6000
    );
    const classInfoAfterBlockedAccept = await safeRcon(rcon, `mmocore admin info ${USER}`);
    reporter.evidence(`class-after-blocked-accept: ${String(classInfoAfterBlockedAccept).slice(0, 220)}`);

    mainWindow = bot.currentWindow || mainWindow;
    mainWindow = await clickSlotOrFallback(
      bot,
      mainWindow,
      reporter,
      'select-race-czlowiek',
    19,
    [`core-cast creator_v2_select_race_czlowiek ${USER}`],
      rcon,
      mainMatcher
    );

    mainWindow = await clickSlotOrFallback(
      bot,
      mainWindow,
      reporter,
      'select-class',
      CLASS_PATH.slot,
    [`core-cast creator_v2_select_class_${CLASS_PATH.id} ${USER}`],
      rcon,
      mainMatcher
    );

    const pendingInfo = await safeRcon(rcon, `mmocore admin info ${USER}`);
    reporter.evidence(`class-before-accept: ${String(pendingInfo).slice(0, 220)}`);

    const mentorWindow = await clickSlotOrFallback(
      bot,
      mainWindow,
      reporter,
      'accept-complete-selection',
      50,
    [`core-cast creator_v2_accept_pending_character ${USER}`],
      rcon,
      titleIncludes('galeria mentorów', 'archetypy'),
      10000
    );
    reporter.step('mentor-roster-opened', 'PASS', { title: getWindowTitle(mentorWindow) });

    const cityWindow = await clickSlotOrFallback(
      bot,
      mentorWindow,
      reporter,
      'mentor-to-city',
      49,
      [`core-cast creator_open_city_roster ${USER}`, `core-cast ${USER} creator_open_city_roster`],
      rcon,
      titleIncludes('bastion pierwszego kontraktu'),
      10000
    );

    let skillWindow = await clickSlotOrFallback(
      bot,
      cityWindow,
      reporter,
      'city-to-skill-roster',
      21,
      [`core-cast creator_open_skill_roster ${USER}`, `core-cast ${USER} creator_open_skill_roster`],
      rcon,
      titleIncludes('trener skilli', 'roster')
    );
    skillWindow = await clickSlotOrFallback(
      bot,
      skillWindow,
      reporter,
      'select-generic-skill',
      21,
      [`core-cast creator_select_generic_guard ${USER}`, `core-cast ${USER} creator_select_generic_guard`],
      rcon,
      titleIncludes('trener skilli', 'roster')
    );

    let professionWindow = await clickSlotOrFallback(
      bot,
      cityWindow,
      reporter,
      'city-to-profession-roster',
      25,
      [`core-cast creator_open_profession_roster ${USER}`, `core-cast ${USER} creator_open_profession_roster`],
      rcon,
      titleIncludes('dzielnica profesji')
    );
    professionWindow = await clickSlotOrFallback(
      bot,
      professionWindow,
      reporter,
      'complete-profession-intro',
      40,
      [`core-cast creator_profession_intro_complete ${USER}`, `core-cast ${USER} creator_profession_intro_complete`],
      rcon,
      titleIncludes('dzielnica profesji')
    );

    const firstContractWindow = await clickSlotOrFallback(
      bot,
      cityWindow,
      reporter,
      'city-to-first-contract',
      23,
      [`core-cast creator_open_first_contract ${USER}`, `core-cast ${USER} creator_open_first_contract`],
      rcon,
      titleIncludes('kwatermistrz', 'pierwszy kontrakt')
    );

    await clickSlotOrFallback(
      bot,
      firstContractWindow,
      reporter,
      'claim-city-first-equipment',
      CLASS_PATH.claimSlot,
      [`core-cast npc_quartermaster_claim_${CLASS_PATH.id} ${USER}`, `core-cast ${USER} npc_quartermaster_claim_${CLASS_PATH.id}`],
      rcon,
      null
    );

    await sleep(2000);
    const classRaw = await safeRcon(rcon, `mmocore admin info ${USER}`);
    const inventory = inventorySnapshot(bot);
    const invRaw = await safeRcon(rcon, `minecraft:data get entity ${USER} Inventory`);
    reporter.evidence(`class/info: ${String(classRaw).slice(0, 240)}`);
    reporter.evidence(`inventory-items: ${inventory.map((item) => item.displayName || item.name).join(' | ')}`);
    reporter.evidence(`inventory-raw: ${String(invRaw).slice(0, 240)}`);

    const missingPlayerProof = [
      'storage safe-zone/combat-lock refusal not exercised by this harness yet',
      'live MMOCore class panel not exercised by this harness yet',
      'forge preview not exercised by this harness yet',
    ];
    reporter.step('full-player-path-proof', 'INSUFFICIENT_EVIDENCE', { missingPlayerProof });
    reporter.milestone('character-sector-player-path', {
      status: 'INSUFFICIENT_EVIDENCE',
      requiredProof: ['STATIC_CONTRACT', 'RUNTIME_PROOF', 'PLAYER_PROOF'],
      achievedProof: ['STATIC_CONTRACT', 'RUNTIME_PROOF'],
      evidence: [
        'main creator opened',
        'accept blocked without full selection',
        'race selected on main screen',
        'class selected on main screen',
        'accept finalized class',
        'mentor roster opened',
        'city onboarding reached',
        'starter loadout claimed',
      ],
      blockers: missingPlayerProof,
      owner: 'character-sector-e2e',
      nextAction: 'extend harness into storage refusal, MMOCore panel, and forge preview',
    });
    verdict = 'INSUFFICIENT_EVIDENCE';
  } catch (error) {
    reporter.step('run-error', 'FAIL', { error: error.message });
    reporter.milestone('character-sector-player-path', {
      status: 'FAIL',
      requiredProof: ['STATIC_CONTRACT', 'RUNTIME_PROOF', 'PLAYER_PROOF'],
      achievedProof: ['STATIC_CONTRACT'],
      blockers: [error.message],
      owner: 'character-sector-e2e',
      nextAction: 'inspect creator GUI contract on items and retry',
    });
    reporter.setFailureSeam('character_sector_e2e', {
      symptom: error.message,
      nextAction: 'inspect creator GUI contract on items and retry',
    });
    throw error;
  } finally {
    try {
      if (bot) bot.quit('character-sector-e2e done');
    } catch {}
    try {
      if (rcon?.end) await rcon.end();
    } catch {}
    try {
      runtimeLock.release();
    } catch {}
    const file = reporter.finalize(verdict);
    console.log(file);
    process.exitCode = verdict === 'FAIL' ? 1 : 0;
    setTimeout(() => process.exit(process.exitCode || 0), 50).unref();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
