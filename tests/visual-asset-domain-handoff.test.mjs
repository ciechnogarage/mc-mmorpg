import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const CONTRACT = 'docs/contracts/visual-asset-domain-handoff-contract.yaml';

function loadYaml(relativePath) {
  const script = [
    'import json, pathlib, sys, yaml',
    'document = yaml.safe_load(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8"))',
    'print(json.dumps(document))',
  ].join('; ');
  const result = spawnSync('python3', ['-c', script, relativePath], {
    cwd: ROOT,
    encoding: 'utf8',
  });
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  return JSON.parse(result.stdout);
}

test('visual asset handoff declares canonical owners, consumers, profiles, and non-transitive gates', () => {
  const document = loadYaml(CONTRACT);

  assert.equal(document.schema_version, 1);
  assert.equal(document.contract.owner_domain, '15');
  assert.equal(document.contract.release_owner, '17');
  assert.deepEqual(document.contract.consumers, ['05', '06', '07', '12', '14']);
  assert.deepEqual(Object.keys(document.domain_boundaries).sort(), ['15', '17', '18']);

  for (const domain of document.contract.consumers) {
    const brief = document.consumer_brief_authority[domain];
    assert.ok(brief, `missing consumer brief authority for Domain ${domain}`);
    assert.ok(brief.required_brief_inputs.length > 0, `missing brief inputs for Domain ${domain}`);
    assert.match(brief.forbidden_transfer, /Domain 15 must not redefine/);
  }

  assert.ok(document.asset_profiles.asset_type.includes('boss'));
  assert.ok(document.asset_profiles.asset_type.includes('furniture_prop'));
  assert.deepEqual(
    document.asset_profiles.rig_archetype,
    ['biped', 'quadruped', 'flying', 'serpentine', 'multipart', 'static'],
  );
  assert.deepEqual(
    Object.keys(document.asset_profiles.delivery_profile).sort(),
    ['modelengine_entity', 'nexo_static'],
  );
  assert.deepEqual(
    Object.keys(document.profile_contracts).sort(),
    ['asset_family_profile', 'delivery_profile', 'rig_archetype_profile', 'style_profile'],
  );
  assert.ok(document.profile_contracts.style_profile.required_fields.includes('approved_reference_ids'));
  assert.ok(document.profile_contracts.style_profile.required_fields.includes('anti_reference_ids'));
  assert.ok(document.profile_contracts.rig_archetype_profile.required_fields.includes('required_bones_or_locators'));
  assert.ok(document.required_asset_spec.includes('profile_version_refs'));

  assert.equal(document.source_and_pack_policy.direct_bbmodel_json_write, 'forbidden');
  assert.equal(document.source_and_pack_policy.merged_resource_pack_owner, 'Nexo');
  assert.deepEqual(Object.keys(document.gate_contract), ['ART_PASS', 'INTEGRATION_PASS', 'RUNTIME_PASS']);
  assert.equal(document.handoffs.integration_evidence_to_gate.producer, '18');
  assert.equal(document.handoffs.integration_evidence_to_gate.consumer, '17');
  assert.equal(document.gate_contract.INTEGRATION_PASS.authority, '17');
  assert.ok(document.gate_contract.INTEGRATION_PASS.does_not_prove.includes('runtime behavior'));
});

test('ownership registry and dependency map route every asset consumer through Domains 15, 18, and 17', () => {
  const contract = loadYaml(CONTRACT);
  const ownership = loadYaml('docs/product/00-domain-ownership.yaml');
  const ownershipByDomain = Object.fromEntries(ownership.ownership.map((row) => [row.domain_id, row]));
  const dependencyMap = fs.readFileSync(path.join(ROOT, 'docs/product/00-dependency-map.md'), 'utf8');

  for (const consumer of contract.contract.consumers) {
    assert.ok(ownershipByDomain[consumer].allowed_consumers.includes('15'));
    assert.ok(ownershipByDomain['15'].allowed_consumers.includes(consumer));
    assert.ok(dependencyMap.includes(`| \`${consumer}\` | \`15\` |`));
  }

  assert.ok(ownershipByDomain['15'].allowed_consumers.includes('18'));
  assert.ok(ownershipByDomain['15'].allowed_consumers.includes('17'));
  assert.ok(ownershipByDomain['18'].allowed_consumers.includes('17'));
  assert.ok(dependencyMap.includes('| `15` | `18` |'));
  assert.ok(dependencyMap.includes('| `15` | `17` |'));
  assert.ok(dependencyMap.includes('| `18` | `17` |'));
});
