'use strict';

// Minimalny, zaleznosciowo-wolny walidator kontraktow JSON Schema (subset:
// type/required/properties/const, plyto rekurencyjnie). Celowo bez ajv —
// packages/qa juz ma wlasny styl lekkich asercji (assertEquals/assertTruthy),
// wiec kolejny homegrown checker jest spojniejszy niz nowa zaleznosc npm
// tylko dla 4 plaskich schematow w contracts/companion-bridge/v1.
//
// Sprawdza: required (klucz MUSI istniec, nawet jesli wartosc to null —
// zgodnie z kontraktem, ktory rozroznia "brak klucza" od "null"), type,
// const. Nie sprawdza additionalProperties/format — wystarczajace dla
// scenariuszy acceptance, nie zastepuje pelnego ajv przy wiekszych zmianach
// kontraktu.
function validateContract(schema, data, path = '$') {
  const errors = [];

  function walk(node, value, at) {
    if (node.const !== undefined && value !== node.const) {
      errors.push(`${at}: oczekiwano const ${JSON.stringify(node.const)}, jest ${JSON.stringify(value)}`);
      return;
    }
    if (node.type === 'object' || (node.properties && !node.type)) {
      if (typeof value !== 'object' || value === null) {
        errors.push(`${at}: oczekiwano obiektu, jest ${JSON.stringify(value)}`);
        return;
      }
      for (const key of node.required || []) {
        if (!Object.prototype.hasOwnProperty.call(value, key)) {
          errors.push(`${at}.${key}: brakujacy wymagany klucz`);
        }
      }
      for (const [key, childSchema] of Object.entries(node.properties || {})) {
        if (Object.prototype.hasOwnProperty.call(value, key) && value[key] !== null) {
          walk(resolveRef(schema, childSchema), value[key], `${at}.${key}`);
        }
      }
    }
  }

  walk(schema, data, path);
  return { valid: errors.length === 0, errors };
}

function resolveRef(rootSchema, node) {
  if (!node || !node.$ref) return node;
  const pointer = node.$ref.replace(/^#\//, '').split('/');
  let cursor = rootSchema;
  for (const segment of pointer) cursor = cursor[segment];
  return cursor;
}

module.exports = { validateContract };
