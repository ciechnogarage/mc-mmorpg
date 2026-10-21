'use strict';

// Interakcja z GUI chest-based (DeluxeMenus) — plan sekcja C.
// Pułapki obsłużone tutaj:
//  - window.requiresConfirmation = false (pluginy nie wysyłają potwierdzeń
//    transakcji, bez tego promise clickWindow nigdy się nie rozwiązuje),
//  - oczekiwanie na stabilizację slotów (DeluxeMenus przerysowuje placeholdery),
//  - asercja skutku kliknięcia po stronie wywołującego, nie tu.

const { sleep } = require('@mmorpg-bots/bot-core');
const { normalizeText, getWindowTitle, itemText } = require('./text');

async function waitForWindow(bot, matcher, timeout = 10000) {
  const match = typeof matcher === 'function'
    ? matcher
    : (w) => normalizeText(getWindowTitle(w)).includes(normalizeText(matcher));
  const initial = bot.currentWindow;
  if (initial && match(initial)) return initial;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      cleanup();
      reject(new Error(`window open timeout (matcher=${String(matcher).slice(0, 60)})`));
    }, timeout);
    const poller = setInterval(() => {
      const current = bot.currentWindow;
      if (current && match(current)) { cleanup(); resolve(current); }
    }, 200);
    function onOpen(window) {
      if (match(window)) { cleanup(); resolve(window); }
    }
    function cleanup() {
      clearTimeout(timer);
      clearInterval(poller);
      bot.removeListener('windowOpen', onOpen);
    }
    bot.on('windowOpen', onOpen);
  });
}

// Czeka aż zawartość okna przestanie się zmieniać (re-render placeholderów).
async function awaitStableWindow(window, { settleMs = 600, timeout = 5000 } = {}) {
  const snapshot = () => window.slots.map(s => (s ? `${s.type}:${s.count}` : '.')).join('|');
  const deadline = Date.now() + timeout;
  let last = snapshot();
  let stableSince = Date.now();
  while (Date.now() < deadline) {
    await sleep(150);
    const now = snapshot();
    if (now !== last) { last = now; stableSince = Date.now(); }
    if (Date.now() - stableSince >= settleMs) return window;
  }
  return window; // timeout stabilizacji nie jest błędem — zwróć co jest
}

function findSlotByName(window, nameFragment, { containerOnly = true } = {}) {
  const needle = normalizeText(nameFragment);
  const limit = containerOnly && typeof window.inventoryStart === 'number' ? window.inventoryStart : window.slots.length;
  for (let i = 0; i < limit; i++) {
    const item = window.slots[i];
    if (item && itemText(item).includes(needle)) return { slot: i, item };
  }
  return null;
}

async function clickSlot(bot, window, slot, { mouseButton = 0, mode = 0, postDelay = 400 } = {}) {
  window.requiresConfirmation = false;
  await bot.clickWindow(slot, mouseButton, mode);
  await sleep(postDelay);
  return bot.currentWindow || null;
}

// Otwiera menu komendą i czeka na okno o pasującym tytule.
async function openMenuByCommand(bot, command, titleMatcher, timeout = 10000) {
  bot.chat(command.startsWith('/') ? command : `/${command}`);
  const window = await waitForWindow(bot, titleMatcher, timeout);
  return awaitStableWindow(window);
}

module.exports = { waitForWindow, awaitStableWindow, findSlotByName, clickSlot, openMenuByCommand };
