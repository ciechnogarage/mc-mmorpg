import test from 'node:test';
import assert from 'node:assert/strict';
import { CandidateSafetyError, inspectPluginJar } from '../src/downloads/index.mjs';
import { buildJar, validExampleJar } from './jar-fixtures.mjs';

function throwsCode(fn, code) {
  assert.throws(fn, (error) => {
    assert.ok(error instanceof CandidateSafetyError);
    assert.equal(error.code, code);
    return true;
  });
}

test('JAR validation reads exactly one root plugin.yml and validates plugin name/version', () => {
  const metadata = inspectPluginJar(validExampleJar(), { expectedPluginName: 'ExamplePlugin' });
  assert.deepEqual(metadata, {
    plugin_name: 'ExamplePlugin',
    plugin_version: '2.0.0',
    entry_count: 2,
    uncompressed_bytes: 76,
  });
});

test('JAR validation accepts the canonical Paper paper-plugin.yml descriptor', () => {
  const metadata = inspectPluginJar(buildJar([
    { name: 'paper-plugin.yml', body: 'name: Nexo\nversion: 1.25.1\nmain: com.nexomc.nexo.NexoPlugin\n' },
  ]), { expectedPluginName: 'Nexo' });
  assert.equal(metadata.plugin_name, 'Nexo');
  assert.equal(metadata.plugin_version, '1.25.1');
});

test('JAR validation blocks a plugin.yml name that does not match the trusted source mapping', () => {
  throwsCode(
    () => inspectPluginJar(validExampleJar(), { expectedPluginName: 'DifferentPlugin' }),
    'plugin-name-mismatch',
  );
});

test('JAR validation blocks duplicate plugin.yml entries', () => {
  const jar = buildJar([
    { name: 'plugin.yml', body: 'name: ExamplePlugin\nversion: 2.0.0\n' },
    { name: 'plugin.yml', body: 'name: ExamplePlugin\nversion: 9.9.9\n' },
  ]);
  throwsCode(() => inspectPluginJar(jar, { expectedPluginName: 'ExamplePlugin' }), 'jar-plugin-yml-duplicate');
});

test('JAR validation blocks forged zip-bomb expansion before inflating entry data', () => {
  const jar = buildJar(
    [{ name: 'plugin.yml', body: 'name: ExamplePlugin\nversion: 2.0.0\n' }],
    { declaredUncompressedSize: 300 * 1024 * 1024 },
  );
  throwsCode(() => inspectPluginJar(jar, { expectedPluginName: 'ExamplePlugin' }), 'jar-zip-bomb');
});
