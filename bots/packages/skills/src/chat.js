'use strict';

// Obserwacja chatu/logów po stronie klienta: oczekiwanie na wiadomości,
// wyłapywanie błędów serwera widocznych w chacie (plan sekcja I).

const { flattenText } = require('./text');

const ERROR_PATTERNS = [
  /internal (server )?error/i,
  /wystąpił błąd/i,
  /no permission|brak uprawnie/i,
  /unknown command|nieznana komenda/i
];

function watchChat(bot, { errorPatterns = ERROR_PATTERNS, log = console } = {}) {
  const captured = { messages: [], errors: [] };
  bot.on('messagestr', (message, position) => {
    const text = flattenText(message);
    captured.messages.push({ text, position, at: Date.now() });
    if (captured.messages.length > 500) captured.messages.shift();
    for (const re of errorPatterns) {
      if (re.test(text)) {
        captured.errors.push({ text, pattern: String(re), at: Date.now() });
        log.warn(`[chat] wzorzec błędu: ${text.slice(0, 200)}`);
        break;
      }
    }
  });
  return captured;
}

function waitForMessage(bot, regex, timeout = 10000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      bot.removeListener('messagestr', onMsg);
      reject(new Error(`brak wiadomości pasującej do ${regex} w ${timeout}ms`));
    }, timeout);
    function onMsg(message) {
      const text = flattenText(message);
      if (regex.test(text)) {
        clearTimeout(timer);
        bot.removeListener('messagestr', onMsg);
        resolve(text);
      }
    }
    bot.on('messagestr', onMsg);
  });
}

module.exports = { watchChat, waitForMessage, ERROR_PATTERNS };
