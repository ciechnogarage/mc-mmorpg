'use strict';

// Etap 6 (plan companion-bridge): deterministyczny acceptance runner.
// LLM nie jest elementem krytycznej sciezki tych testow — kazdy scenariusz
// to zwykly skrypt Node uzywajacy packages/qa (runScenario/reporter) tak
// samo jak apps/smoke.
//
// Uzycie: node apps/acceptance/runner.js <scenariusz> [--flagi...]
// Werdykt PASS/FAIL/BLOCKED trafia do runs/ (patrz packages/qa) i ustawia
// kod wyjscia procesu (0 = PASS, 1 = inny werdykt) — nadaje sie do CI.

const fs = require('fs');
const path = require('path');

const SCENARIOS_DIR = path.join(__dirname, 'scenarios');

function listScenarios() {
  return fs.readdirSync(SCENARIOS_DIR)
    .filter(f => f.endsWith('.js'))
    .map(f => f.replace(/\.js$/, ''));
}

async function main() {
  const [name, ...rest] = process.argv.slice(2);
  if (!name) {
    console.error('Uzycie: node apps/acceptance/runner.js <scenariusz> [--flagi...]');
    console.error('Dostepne scenariusze:', listScenarios().join(', '));
    process.exit(2);
  }

  const scenarioPath = path.join(SCENARIOS_DIR, `${name}.js`);
  if (!fs.existsSync(scenarioPath)) {
    console.error(`Nieznany scenariusz: ${name}`);
    console.error('Dostepne scenariusze:', listScenarios().join(', '));
    process.exit(2);
  }

  const scenario = require(scenarioPath);
  const result = await scenario.run(rest);
  console.log(`\nWerdykt: ${result.verdict}`);
  console.log(`Raport: ${result.file}`);
  process.exit(result.verdict === 'PASS' ? 0 : 1);
}

main().catch(error => {
  console.error('Acceptance runner crash:', error);
  process.exit(1);
});
