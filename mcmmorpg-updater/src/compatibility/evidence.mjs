const EXACT_CLASS = 'exact-listed';
const RANGE_CLASS = 'range-inferred';
const CHANGELOG_CLASS = 'changelog-derived';
const FAMILY_CLASS = 'family-inferred';
const UNKNOWN_CLASS = 'unknown';
const NEGATIVE_CLASS = 'negative-listed';

const POSITIVE_CLASSES = new Set([EXACT_CLASS, RANGE_CLASS, CHANGELOG_CLASS, FAMILY_CLASS]);

export const COMPATIBILITY_CLASSES = Object.freeze([
  EXACT_CLASS,
  RANGE_CLASS,
  CHANGELOG_CLASS,
  FAMILY_CLASS,
  UNKNOWN_CLASS,
  NEGATIVE_CLASS,
]);

export function classifyCompatibilityEvidence({
  minecraftVersion,
  loaders = ['paper'],
  gameVersions = [],
  version,
  title,
  description,
  datePublished = null,
  source = 'metadata',
} = {}) {
  const normalizedMinecraft = normalizeMinecraftVersion(minecraftVersion);
  if (!normalizedMinecraft) {
    return evidence(UNKNOWN_CLASS, null, 'target Minecraft version unavailable', source, {
      minecraft_version: null,
      loaders: normalizeLoaders(loaders),
    });
  }

  const normalizedGameVersions = normalizeGameVersions(gameVersions);
  const exact = normalizedGameVersions.find((candidate) => candidate === normalizedMinecraft);
  if (exact) {
    return evidence(EXACT_CLASS, true, `explicitly lists ${normalizedMinecraft}`, source, {
      matched_versions: [exact],
      minecraft_version: normalizedMinecraft,
      loaders: normalizeLoaders(loaders),
    });
  }

  const negative = normalizedGameVersions.find((candidate) => isExplicitDifferentPatch(candidate, normalizedMinecraft));
  if (negative) {
    return evidence(NEGATIVE_CLASS, false, `explicitly lists ${negative}, not ${normalizedMinecraft}`, source, {
      matched_versions: [negative],
      minecraft_version: normalizedMinecraft,
      loaders: normalizeLoaders(loaders),
    });
  }

  const range = normalizedGameVersions.find((candidate) => rangeContains(candidate, normalizedMinecraft));
  if (range) {
    return evidence(RANGE_CLASS, true, `range ${range} contains ${normalizedMinecraft}`, source, {
      matched_versions: [range],
      minecraft_version: normalizedMinecraft,
      loaders: normalizeLoaders(loaders),
    });
  }

  const family = normalizedGameVersions.find((candidate) => familyContains(candidate, normalizedMinecraft));
  if (family) {
    return evidence(FAMILY_CLASS, true, `family ${family} contains ${normalizedMinecraft}`, source, {
      matched_versions: [family],
      minecraft_version: normalizedMinecraft,
      loaders: normalizeLoaders(loaders),
    });
  }

  const changelog = classifyChangelog({ minecraftVersion: normalizedMinecraft, version, title, description, datePublished, source });
  if (changelog.class !== UNKNOWN_CLASS) return changelog;

  if (normalizedGameVersions.length > 0) {
    return evidence(NEGATIVE_CLASS, false, `listed versions do not include ${normalizedMinecraft}`, source, {
      listed_versions: normalizedGameVersions,
      minecraft_version: normalizedMinecraft,
      loaders: normalizeLoaders(loaders),
    });
  }

  return evidence(UNKNOWN_CLASS, null, `no reliable compatibility evidence for ${normalizedMinecraft}`, source, {
    minecraft_version: normalizedMinecraft,
    loaders: normalizeLoaders(loaders),
  });
}

export function isEvidenceCompatible(compatibility) {
  return POSITIVE_CLASSES.has(compatibility?.class) && compatibility.compatible === true;
}

function classifyChangelog({ minecraftVersion, version, title, description, datePublished, source }) {
  const text = [title, description].filter((part) => typeof part === 'string').join('\n');
  if (!text) return evidence(UNKNOWN_CLASS, null, `no reliable compatibility evidence for ${minecraftVersion}`, source, {
    minecraft_version: minecraftVersion,
  });

  const escaped = escapeRegExp(minecraftVersion);
  const supportPattern = new RegExp(`\\b(?:support|supports|compatible(?:\\s+with)?|added\\s+support\\s+for|paper)\\b[^\\n.]{0,80}\\b${escaped}\\b`, 'i');
  const removalPattern = new RegExp(`\\b(?:drop(?:ped)?|remove(?:d)?|no\\s+longer\\s+support(?:s|ed)?|incompatible)\\b[^\\n.]{0,80}\\b${escaped}\\b`, 'i');

  if (removalPattern.test(text)) {
    return evidence(NEGATIVE_CLASS, false, `changelog states ${minecraftVersion} support was removed or is incompatible`, source, {
      minecraft_version: minecraftVersion,
      source_update: sourceUpdate(version, datePublished),
    });
  }

  if (supportPattern.test(text)) {
    return evidence(CHANGELOG_CLASS, true, `changelog states support for ${minecraftVersion}`, source, {
      minecraft_version: minecraftVersion,
      source_update: sourceUpdate(version, datePublished),
    });
  }

  return evidence(UNKNOWN_CLASS, null, `no reliable compatibility evidence for ${minecraftVersion}`, source, {
    minecraft_version: minecraftVersion,
  });
}

function evidence(className, compatible, reason, source, extra = {}) {
  return Object.freeze({
    class: className,
    compatible,
    confidence: confidenceFor(className),
    reason,
    source,
    ...extra,
  });
}

function confidenceFor(className) {
  if (className === EXACT_CLASS) return 'high';
  if (className === RANGE_CLASS) return 'medium-high';
  if (className === CHANGELOG_CLASS) return 'medium';
  if (className === FAMILY_CLASS) return 'low';
  if (className === NEGATIVE_CLASS) return 'high';
  return 'none';
}

function normalizeMinecraftVersion(value) {
  return typeof value === 'string' && /^\d+\.\d+(?:\.\d+)?$/.test(value) ? value : null;
}

function normalizeGameVersions(values) {
  return [...new Set((Array.isArray(values) ? values : [])
    .filter((value) => typeof value === 'string')
    .map((value) => value.trim())
    .filter(Boolean))];
}

function normalizeLoaders(loaders) {
  return [...new Set((Array.isArray(loaders) ? loaders : [])
    .filter((loader) => typeof loader === 'string' && loader.trim().length > 0)
    .map((loader) => loader.trim().toLowerCase()))];
}

function isExplicitDifferentPatch(candidate, target) {
  return /^\d+\.\d+(?:\.\d+)?$/.test(candidate) && !familyContains(candidate, target);
}

function familyContains(candidate, target) {
  const family = /^(\d+\.\d+)\.x$/i.exec(candidate);
  if (family) return target.startsWith(`${family[1]}.`);
  return candidate === target;
}

function rangeContains(candidate, target) {
  const range = /^(\d+\.\d+(?:\.\d+)?)\s*-\s*(\d+\.\d+(?:\.\d+)?)$/.exec(candidate);
  if (!range) return false;
  return compareVersions(target, range[1]) >= 0 && compareVersions(target, range[2]) <= 0;
}

function compareVersions(left, right) {
  const leftParts = left.split('.').map(Number);
  const rightParts = right.split('.').map(Number);
  const length = Math.max(leftParts.length, rightParts.length);
  for (let index = 0; index < length; index += 1) {
    const diff = (leftParts[index] ?? 0) - (rightParts[index] ?? 0);
    if (diff !== 0) return diff;
  }
  return 0;
}

function sourceUpdate(version, datePublished) {
  return {
    version: typeof version === 'string' && version.length > 0 ? version : null,
    date: typeof datePublished === 'string' && datePublished.length > 0 ? datePublished : null,
  };
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
