#!/usr/bin/env node
// Provisions the ignored Docker world runtime for the PetCore live acceptance.
// Secrets are read from MCMMORPG/docker/.env and never written to tracked files.
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';

const ROOT = path.resolve(import.meta.dirname, '..');
const DOCKER = path.join(ROOT, 'MCMMORPG', 'docker');
const WORLD_PLUGINS = path.join(DOCKER, 'servers', 'world', 'plugins');
const ENV_FILE = path.join(DOCKER, '.env');

function readEnv(file) {
  const result = {};
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const match = line.match(/^([A-Z0-9_]+)=(.*)$/);
    if (!match) continue;
    result[match[1]] = match[2].replace(/^['"]|['"]$/g, '');
  }
  return result;
}

function required(env, key) {
  if (!env[key] || env[key] === 'change-me') throw new Error(`missing usable ${key} in docker/.env`);
  return env[key];
}

function runDocker(args, env = {}) {
  const result = spawnSync('docker', ['compose', ...args], {
    cwd: DOCKER,
    env: { ...process.env, ...env },
    stdio: 'inherit'
  });
  if (result.status !== 0) throw new Error(`docker compose failed (${result.status})`);
}

function runMariaDb(sql, rootPassword) {
  const result = spawnSync('docker', ['exec', '-e', 'MYSQL_PWD', 'mcmmorpg-mariadb', 'mariadb', '-uroot', '-e', sql], {
    cwd: DOCKER,
    env: { ...process.env, MYSQL_PWD: rootPassword },
    stdio: 'inherit'
  });
  if (result.status !== 0) throw new Error(`MariaDB provisioning failed (${result.status})`);
}

function replaceLine(text, key, value) {
  const expression = new RegExp(`^(  ${key}:).*$`, 'm');
  if (!expression.test(text)) throw new Error(`missing YAML key: ${key}`);
  return text.replace(expression, `$1 ${value}`);
}

function provisionDatabase(rootPassword, appPassword) {
  const sql = [
    'CREATE DATABASE IF NOT EXISTS petcore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;',
    "CREATE USER IF NOT EXISTS 'petcore'@'%' IDENTIFIED BY '" + appPassword.replaceAll("'", "''") + "';",
    "ALTER USER 'petcore'@'%' IDENTIFIED BY '" + appPassword.replaceAll("'", "''") + "';",
    "GRANT ALL PRIVILEGES ON petcore.* TO 'petcore'@'%';",
    'FLUSH PRIVILEGES;'
  ].join('\n');
  runMariaDb(sql, rootPassword);
}

function configureMmoProfiles(appPassword) {
  const source = fs.readFileSync(path.join(ROOT, 'MCMMORPG', 'plugins', 'MMOProfiles', 'config.yml'), 'utf8');
  const block = source.match(/^mysql:\n[\s\S]*?(?=^profile-selection-location:)/m);
  if (!block) throw new Error('MMOProfiles mysql block not found');
  let mysql = block[0];
  mysql = replaceLine(mysql, 'enabled', 'true');
  mysql = replaceLine(mysql, 'host', 'mariadb');
  mysql = replaceLine(mysql, 'port', '3306');
  mysql = replaceLine(mysql, 'database', 'mcmmorpg');
  mysql = replaceLine(mysql, 'user', 'mcmmorpg');
  mysql = replaceLine(mysql, 'pass', JSON.stringify(appPassword));
  const output = source.replace(block[0], mysql);
  const target = path.join(WORLD_PLUGINS, 'MMOProfiles');
  fs.mkdirSync(target, { recursive: true });
  fs.writeFileSync(path.join(target, 'config.yml'), output);
  fs.copyFileSync(
    path.join(ROOT, 'MCMMORPG', 'plugins', 'MMOProfiles-1.2-20260325.193503-28.jar'),
    // Paper 1.21's remapper reliably discovers this legacy plugin under a
    // stable filename; the versioned filename is otherwise silently omitted
    // from the provider set on this staging image.
    path.join(WORLD_PLUGINS, 'MMOProfiles.jar')
  );
}

function configurePetCore(appPassword) {
  const target = path.join(WORLD_PLUGINS, 'PetCore');
  fs.mkdirSync(target, { recursive: true });
  const configPath = path.join(target, 'config.yml');
  let config = fs.readFileSync(path.join(ROOT, 'MCMMORPG', 'plugin-src', 'petcore', 'src', 'main', 'resources', 'config.yml'), 'utf8');
  config = config.replace('  enabled: false', '  enabled: true');
  config = config.replace('  username: "petcore"', '  username: "petcore"');
  config = config.replace('  password: "CHANGE_ME"', `  password: ${JSON.stringify(appPassword)}`);
  config = config.replace('  staging-enabled: false', '  staging-enabled: true');
  config = config.replace('  mode: UNKNOWN', '  mode: OPEN_PVE');
  fs.writeFileSync(configPath, config);
  fs.copyFileSync(
    path.join(ROOT, 'MCMMORPG', 'plugin-src', 'petcore', 'build', 'libs', 'petcore-0.1.0-SNAPSHOT.jar'),
    path.join(WORLD_PLUGINS, 'petcore-0.1.0-SNAPSHOT.jar')
  );
}

function main() {
  const env = readEnv(ENV_FILE);
  const rootPassword = required(env, 'MARIADB_ROOT_PASSWORD');
  const appPassword = required(env, 'MARIADB_PASSWORD');
  provisionDatabase(rootPassword, appPassword);
  configureMmoProfiles(appPassword);
  configurePetCore(appPassword);
  console.log('Provisioned ignored world runtime: MMOProfiles + PetCore authority (secrets not printed).');
}

main();
