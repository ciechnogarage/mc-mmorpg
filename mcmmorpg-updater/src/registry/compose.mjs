import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { dirname, isAbsolute, resolve } from 'node:path';

export async function inspectComposeFile(composePath) {
  const bytes = await readFile(composePath);
  return {
    ...parseComposeMapping(bytes.toString('utf8'), dirname(composePath)),
    configSha256: createHash('sha256').update(bytes).digest('hex'),
  };
}

export function parseComposeMapping(source, composeDirectory) {
  const lines = source.split(/\r?\n/);
  const nameLine = lines.find((line) => /^name:\s*/.test(line));
  const project = nameLine ? parseScalar(nameLine.replace(/^name:\s*/, '')) : null;
  const services = new Map();
  let inServices = false;
  let current = null;
  let inVolumes = false;

  for (const line of lines) {
    if (/^services:\s*$/.test(line)) {
      inServices = true;
      current = null;
      continue;
    }
    if (inServices && /^\S/.test(line)) {
      inServices = false;
      current = null;
    }
    if (!inServices || /^\s*(?:#|$)/.test(line)) continue;

    const serviceMatch = line.match(/^  ([A-Za-z0-9_-]+):\s*$/);
    if (serviceMatch) {
      current = { profile: null, volumes: [] };
      services.set(serviceMatch[1], current);
      inVolumes = false;
      continue;
    }
    if (!current) continue;

    const profileMatch = line.match(/^    profiles:\s*\[\s*["']?([^"'\],\s]+)["']?\s*\]\s*$/);
    if (profileMatch) {
      current.profile = profileMatch[1];
      continue;
    }
    if (/^    volumes:\s*$/.test(line)) {
      inVolumes = true;
      continue;
    }
    if (/^    \S/.test(line)) inVolumes = false;
    if (inVolumes) {
      const volumeMatch = line.match(/^      -\s+["']?([^:"']+):([^"']+)["']?\s*$/);
      if (volumeMatch) {
        const host = volumeMatch[1].trim();
        const container = volumeMatch[2].trim();
        current.volumes.push({
          host: isAbsolute(host) ? host : resolve(composeDirectory, host),
          container,
        });
      }
    }
  }

  return { project, services };
}

function parseScalar(value) {
  const withoutComment = value.replace(/\s+#.*$/, '').trim();
  if ((withoutComment.startsWith('"') && withoutComment.endsWith('"'))
      || (withoutComment.startsWith("'") && withoutComment.endsWith("'"))) {
    return withoutComment.slice(1, -1);
  }
  return withoutComment;
}
