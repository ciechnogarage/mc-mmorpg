import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const creatorScriptPaths = [
  'MCMMORPG/plugins/CoreTools/Scripts/foundation_character_creator_v2.yml',
  'MCMMORPG/docker/servers/items/plugins/CoreTools/Scripts/foundation_character_creator_v2.yml',
];

const creatorMenuPaths = [
  'MCMMORPG/plugins/CoreTools/MenuCreator/foundation_creator_intro.yml',
  'MCMMORPG/docker/servers/items/plugins/CoreTools/MenuCreator/foundation_creator_intro.yml',
];

test('creator scripts use list-shaped Conditions and Mechanics entries', () => {
  for (const file of creatorScriptPaths) {
    const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
    for (let index = 0; index < lines.length; index += 1) {
      if (/^ Conditions:$/.test(lines[index])) {
        assert.match(lines[index + 1] || '', /^  - Comparator\{/,
          `${file}:${index + 2} must contain a list item after Conditions`);
      }
      if (/^ Mechanics:$/.test(lines[index])) {
        let cursor = index + 1;
        while (cursor < lines.length && lines[cursor].trim() !== '') {
          assert.match(lines[cursor], /^  - /,
            `${file}:${cursor + 1} must contain a list item after Mechanics`);
          cursor += 1;
        }
      }
    }
  }
});

test('creator menu scripts are explicit YAML lists in source and runtime copy', () => {
  for (const file of creatorMenuPaths) {
    const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
    for (let index = 0; index < lines.length; index += 1) {
      if (/^\s+Scripts:$/.test(lines[index])) {
        assert.match(lines[index + 1] || '', /^\s+- creator_/,
          `${file}:${index + 2} must contain a list script action`);
      }
    }
  }
});

test('creator source and runtime copies are byte-identical', () => {
  assert.equal(
    fs.readFileSync(creatorScriptPaths[0], 'utf8'),
    fs.readFileSync(creatorScriptPaths[1], 'utf8')
  );
  assert.equal(
    fs.readFileSync(creatorMenuPaths[0], 'utf8'),
    fs.readFileSync(creatorMenuPaths[1], 'utf8')
  );
});

test('creator Accept path exposes staged diagnostics without bypassing conditions', () => {
  const source = fs.readFileSync(creatorScriptPaths[0], 'utf8');
  for (const marker of [
    'CREATOR_ACCEPT_CLICK',
    'CREATOR_SELECTED_RACE={pending_race_id}',
    'CREATOR_SELECTED_CLASS={pending_class_id}',
    'CREATOR_RACE_CONDITION=',
    'CREATOR_CLASS_CONDITION=',
    'CREATOR_ACCEPT_CONDITION=',
    'CREATOR_DISPATCH_TARGET=',
    'CREATOR_DISPATCH_EXECUTOR=',
    'CREATOR_DISPATCH_RESULT=',
  ]) {
    assert.match(source, new RegExp(marker.replace(/[{}]/g, '\\$&')),
      `creator Accept diagnostics must include ${marker}`);
  }
  assert.match(source, /creator_v2_accept_pending_character:\n  Conditions:\n  - Comparator\{value1="\{pending_race_id\}"/);
});
