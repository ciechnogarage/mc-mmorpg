'use strict';

// Odczyt danych MMOItems z NBT (plan sekcja B).
// W 1.20.5+ custom NBT pluginów żyje w komponencie minecraft:custom_data,
// który jest domyślnie synchronizowany do klienta (obfuskacja Paper: off).

const nbt = require('prismarine-nbt');

function getCustomData(item) {
  if (!item) return null;
  // 1.20.5+: dane komponentów są w item.components ([{type, data}]),
  // item.nbt zawiera już tylko legacy pola (np. Damage).
  if (Array.isArray(item.components)) {
    const comp = item.components.find(c => c && c.type === 'custom_data');
    if (comp) {
      try { return nbt.simplify(comp.data); } catch { return null; }
    }
  }
  if (!item.nbt) return null;
  let data;
  try {
    data = nbt.simplify(item.nbt);
  } catch {
    return null;
  }
  if (data && typeof data === 'object') {
    return data['minecraft:custom_data'] || data['custom_data'] || data;
  }
  return null;
}

function getMMOItemId(item) {
  const cd = getCustomData(item);
  return cd && typeof cd.MMOITEMS_ITEM_ID === 'string' ? cd.MMOITEMS_ITEM_ID : null;
}

function getMMOItemType(item) {
  const cd = getCustomData(item);
  return cd && typeof cd.MMOITEMS_ITEM_TYPE === 'string' ? cd.MMOITEMS_ITEM_TYPE : null;
}

// Wszystkie tagi MMOITEMS_* — do snapshotów regresji itemów.
function getMMOStats(item) {
  const cd = getCustomData(item);
  if (!cd) return null;
  const out = {};
  for (const [key, value] of Object.entries(cd)) {
    if (key.startsWith('MMOITEMS_')) out[key] = value;
  }
  return Object.keys(out).length ? out : null;
}

function findInventoryMMOItem(bot, mmoId) {
  return bot.inventory.items().find(item => getMMOItemId(item) === mmoId) || null;
}

async function waitForInventoryMMOItem(bot, mmoId, timeout = 10000) {
  const { waitFor } = require('@mmorpg-bots/bot-core');
  return waitFor(() => findInventoryMMOItem(bot, mmoId), timeout, 250);
}

// Twardy regression-assert kontraktu prismarine-item: minor bump potrafi
// przemeblować kształt komponentów i cicho wywalić wszystkie asercje itemów.
// Wołać na dowolnym itemie z custom_data zanim scenariusz zacznie asertować.
function assertComponentContract(item) {
  if (!item) throw new Error('kontrakt: brak itemu do sprawdzenia');
  if (!Array.isArray(item.components)) {
    throw new Error(`kontrakt prismarine-item złamany: item.components nie jest tablicą (${typeof item.components})`);
  }
  const comp = item.components.find(c => c && c.type === 'custom_data');
  if (!comp) throw new Error('kontrakt: brak komponentu custom_data (item bez custom NBT albo zmiana kształtu)');
  const simplified = nbt.simplify(comp.data);
  if (!simplified || typeof simplified !== 'object') {
    throw new Error('kontrakt: custom_data.data nie parsuje się do obiektu przez nbt.simplify');
  }
  return { componentCount: item.components.length, customDataKeys: Object.keys(simplified).length };
}

module.exports = { getCustomData, getMMOItemId, getMMOItemType, getMMOStats, findInventoryMMOItem, waitForInventoryMMOItem, assertComponentContract };
