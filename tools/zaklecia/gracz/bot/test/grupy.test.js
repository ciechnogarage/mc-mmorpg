'use strict';
const test=require('node:test');const assert=require('node:assert/strict');
const {discoverGroups}=require('../lib/grupy');
test('odkrywa grupy z plików i filtruje także sondy N_*',()=>{
  const files=['F.js','A.js','N_N1.js','N_probe.js','README.md'];
  assert.deepEqual(discoverGroups(files,'N_N1'),['N_N1']);
  assert.deepEqual(discoverGroups(files,'AF'),['A','F']);
  assert.deepEqual(discoverGroups(files,''),['A','F','N_N1','N_probe']);
});
