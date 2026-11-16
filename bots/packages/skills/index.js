'use strict';

// Skill library — atomowe, testowalne akcje (plan sekcja E).
// Jedyne miejsce wykonujące akcje na bocie; command-bus mapuje na te funkcje.

module.exports = {
  items: require('./src/items'),
  gui: require('./src/gui'),
  chat: require('./src/chat'),
  move: require('./src/move'),
  text: require('./src/text'),
  combat: require('./src/combat')
};
