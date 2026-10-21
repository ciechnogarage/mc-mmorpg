'use strict';

// Normalizacja tekstu z komponentów chat/window (kody kolorów, JSON components).

function stripColors(value) {
  return String(value ?? '').replace(/§[0-9a-fk-orx]/gi, '');
}

// Tytuły okien w 1.21.x przychodzą jako otagowany NBT ({type,value});
// rozpakowanie do zwykłego obiektu przed spłaszczeniem.
function unwrapNbt(value) {
  if (value && typeof value === 'object' && typeof value.type === 'string' && 'value' in value) {
    try { return require('prismarine-nbt').simplify(value); } catch { return value.value; }
  }
  return value;
}

function flattenText(value) {
  value = unwrapNbt(value);
  if (value == null) return '';
  if (typeof value === 'string') return stripColors(value);
  if (typeof value.toString === 'function' && value.constructor?.name === 'ChatMessage') {
    return stripColors(value.toString());
  }
  if (Array.isArray(value)) return value.map(flattenText).join('');
  if (typeof value === 'object') {
    let out = '';
    if (value.text) out += value.text;
    if (value.extra) out += flattenText(value.extra);
    if (value.translate && !out) out = value.translate;
    return stripColors(out);
  }
  return stripColors(String(value));
}

function normalizeText(value) {
  return flattenText(value).replace(/\s+/g, ' ').trim().toLowerCase();
}

function getWindowTitle(window) {
  return flattenText(window?.title);
}

function itemText(item) {
  if (!item) return '';
  const custom = item.customName ?? item.nbt?.value?.display?.value?.Name?.value;
  return normalizeText(custom ? flattenText(typeof custom === 'string' ? safeJson(custom) ?? custom : custom) : item.displayName || item.name);
}

function safeJson(value) {
  try { return JSON.parse(value); } catch { return null; }
}

module.exports = { stripColors, flattenText, normalizeText, getWindowTitle, itemText };
