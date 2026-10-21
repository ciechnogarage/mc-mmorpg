import fs from 'node:fs';
import path from 'node:path';

const DEFAULT_SEVERITY = 'P2';

const TOKEN_PATTERNS = [
  /(\d[\d,._ ]*)\s+tokens used/gi,
  /tokens used[:=]?\s*(\d[\d,._ ]*)/gi,
  /total tokens[:=]?\s*(\d[\d,._ ]*)/gi,
  /prompt tokens[:=]?\s*(\d[\d,._ ]*)/gi,
  /completion tokens[:=]?\s*(\d[\d,._ ]*)/gi,
  /output tokens[:=]?\s*(\d[\d,._ ]*)/gi,
  /input tokens[:=]?\s*(\d[\d,._ ]*)/gi,
];

const PATH_PATTERN = /(?:\.{0,2}\/)?[A-Za-z0-9_.-]+(?:\/[A-Za-z0-9_. -]+)+/g;
const BACKTICK_PATH_PATTERN = /`([^`\n]+\/[^`\n]+)`/g;
const CLAIM_PATTERN = /\b(done|fixed|implemented|verified|completed|solved|finished|ready)\b/gi;
const UNCERTAINTY_PATTERN = /\b(cannot|can't|could not|unable|unclear|uncertain|not sure|likely|probably|guess|assume|blocked)\b/gi;
const VERIFICATION_PATTERN = /\b(test|tested|verify|verified|validation|smoke|checked|exit code 0|succeeded|pass(?:ed|ing)?)\b/gi;
const ACTION_PATTERN = /\b(exec|apply_patch|write|wrote|updated|changed|implemented|diff --git|created|added|removed|ran)\b/gi;
const RUMINATION_PATTERN = /\b(i will|i'm going to|i going to|need to|should|let me|thinking|plan to|about to|first i(?:'ll)?|now i(?:'ll)?)\b/gi;
const FILE_REPORT_PATTERN = /^[-*]\s+`?[^`\n]+`?/m;
const REAL_VERIFICATION_COMMAND_PATTERN = /^(?:node\s+--test|npm\s+test|pytest\b|vitest\b|jest\b|(?:node|python3?|bash)\s+[^\n]*\b(?:check_[a-z0-9_.-]*|check_modelengine_[a-z0-9_.-]*|validate[a-z0-9_.:-]*|smoke[a-z0-9_.:-]*)\b|(?:node|npm(?:\s+--prefix)?|python3?)\s+[^\n]*_validation\/[^\n]*\b(?:check|validate)[a-z0-9_.:-]*\b|tail\s+-n\s+\d+\s+[^\n]*latest\.log\b|(?:\.\/mc|mcenv|mcrcon|rcon)[^\n]*\b(?:log|logs|status|players|say|cmd)\b|mineflayer\b|mm\s+mobs\s+list\b)/i;
const REAL_VERIFICATION_RESULT_PATTERN = /\b(exit code 0|all tests passed|tests? \d+|0 fail|validation passed|smoke passed|mineflayer verified|rcon verified|latest\.log clean)\b/i;

function clamp(value, min = 0, max = 100) {
  return Math.max(min, Math.min(max, value));
}

function ratio(numerator, denominator) {
  return denominator ? numerator / denominator : 0;
}

function toInt(value) {
  if (!value) return null;
  const normalized = String(value).replace(/[,_ .]/g, '');
  const parsed = Number.parseInt(normalized, 10);
  return Number.isFinite(parsed) ? parsed : null;
}

function collectMatches(text, pattern) {
  return [...text.matchAll(pattern)];
}

function normalizeCommand(command) {
  return command.replace(/\s+/g, ' ').trim();
}

function severityRank(severity = DEFAULT_SEVERITY) {
  if (severity === 'P0') return 0;
  if (severity === 'P1') return 1;
  return 2;
}

function findingsSeverityWeight(findings) {
  return findings.reduce((sum, finding) => {
    if (finding.severity === 'P0') return sum + 3;
    if (finding.severity === 'P1') return sum + 2;
    return sum + 1;
  }, 0);
}

function extractBriefFromLog(text) {
  const userIndex = text.indexOf('\nuser\n');
  const hookIndex = text.indexOf('\nhook:');

  if (userIndex !== -1 && hookIndex !== -1 && hookIndex > userIndex) {
    return text.slice(userIndex + '\nuser\n'.length, hookIndex).trim();
  }

  const briefHeader = text.match(/#\s*BRIEF[\s\S]{0,12000}/i);
  return briefHeader ? briefHeader[0] : '';
}

function extractWorkSection(text) {
  const hookIndex = text.indexOf('\nhook:');
  return hookIndex !== -1 ? text.slice(hookIndex + 1) : text;
}

function summarizeTokens(text) {
  const evidence = [];
  let total = 0;

  for (const pattern of TOKEN_PATTERNS) {
    for (const match of text.matchAll(pattern)) {
      const value = toInt(match[1]);
      if (value !== null) {
        total += value;
        evidence.push({ match: match[0], value });
      }
    }
  }

  return { total, evidence };
}

function extractCommands(text) {
  const commands = [];
  const execPattern = /exec\s*\n?\/bin\/bash\s+-lc\s+(['"])([\s\S]*?)\1\s+in\s+[^\n]+/g;
  const fallbackPattern = /exec\s+([^\n]+?)\s+in\s+[^\n]+/g;

  for (const match of text.matchAll(execPattern)) {
    commands.push(normalizeCommand(match[2]));
  }

  if (!commands.length) {
    for (const match of text.matchAll(fallbackPattern)) {
      commands.push(normalizeCommand(match[1]));
    }
  }

  return commands;
}

function extractPaths(text) {
  const found = new Set();

  for (const match of text.matchAll(PATH_PATTERN)) {
    found.add(match[0].trim());
  }

  for (const match of text.matchAll(BACKTICK_PATH_PATTERN)) {
    found.add(match[1].trim());
  }

  return [...found];
}

function parseScopeRules(briefText) {
  const allowed = [];
  const forbidden = [];
  const requiredOutputs = [];

  const onlyMatch = briefText.match(/ONLY files\s+`([^`]+)`/i);
  if (onlyMatch) allowed.push(onlyMatch[1]);

  for (const match of briefText.matchAll(/DO NOT touch(?: anything under)?\s+`([^`]+)`/gi)) {
    forbidden.push(match[1]);
  }

  for (const match of briefText.matchAll(/Write\s+`([^`]+)`/gi)) {
    requiredOutputs.push(match[1]);
  }

  for (const match of briefText.matchAll(/Deliverable[\s\S]*?`([^`]+)`/gi)) {
    requiredOutputs.push(match[1]);
  }

  return {
    allowed,
    forbidden,
    requiredOutputs: [...new Set(requiredOutputs)],
  };
}

function matchScope(candidate, scope) {
  return scope.endsWith('/**') ? candidate.includes(scope.slice(0, -3)) : candidate.includes(scope);
}

function classifyCommand(command) {
  return {
    isRead: /(^| )((rtk read|cat|sed -n|find |ls|rg |grep |rtk ls|rtk proxy sed -n))/.test(command),
    isWrite: /(apply_patch|python3 .*write|node .*--output|tee |echo |cp |mv |rm |git diff)/.test(command),
    isVerify: REAL_VERIFICATION_COMMAND_PATTERN.test(command),
    isGit: /\bgit\b|rtk git/.test(command),
  };
}

function analyzeCommands(commands) {
  const normalized = commands.map((command) => command.toLowerCase());
  const counts = new Map();
  const categories = { read: 0, write: 0, verify: 0, git: 0, other: 0 };

  for (const command of normalized) {
    counts.set(command, (counts.get(command) || 0) + 1);
    const flags = classifyCommand(command);
    if (flags.isRead) categories.read += 1;
    if (flags.isWrite) categories.write += 1;
    if (flags.isVerify) categories.verify += 1;
    if (flags.isGit) categories.git += 1;
    if (!flags.isRead && !flags.isWrite && !flags.isVerify && !flags.isGit) categories.other += 1;
  }

  const repeated = [...counts.entries()]
    .filter(([, count]) => count > 1)
    .map(([command, count]) => ({ command, count }))
    .sort((left, right) => right.count - left.count || left.command.localeCompare(right.command));

  const topCommands = [...counts.entries()]
    .map(([command, count]) => ({ command, count }))
    .sort((left, right) => right.count - left.count || left.command.localeCompare(right.command))
    .slice(0, 10);

  return {
    total: commands.length,
    repeated,
    readCommands: categories.read,
    writeCommands: categories.write,
    commandBreakdown: {
      byCategory: categories,
      topCommands,
    },
  };
}

function classifyVerificationEvidence(value) {
  const normalized = value.toLowerCase();

  if (/\b(mineflayer|mcrcon|rcon|mm mobs list|mcenv|\.\/mc\b[^\n]*\b(players|status|logs?)\b)\b/.test(normalized)) {
    return { type: 'runtime-check', weight: 5 };
  }

  if (
    normalized.includes('_validation/check') ||
    normalized.includes('_validation/validate') ||
    normalized.includes('check_modelengine_') ||
    /\b(node --test|npm test|pytest|vitest|jest)\b/.test(normalized)
  ) {
    return { type: 'validation-script', weight: 4 };
  }

  if (/\btail -n \d+ [^\n]*latest\.log\b|latest\.log clean\b/.test(normalized)) {
    return { type: 'log-check', weight: 2 };
  }

  if (/\b(exit code 0|all tests passed|tests? \d+|0 fail|validation passed|smoke passed)\b/.test(normalized)) {
    return { type: 'result-signal', weight: 3 };
  }

  return { type: 'other-check', weight: 1 };
}

function detectRealVerification(commands, text) {
  const rawEvidence = [];

  for (const command of commands) {
    if (REAL_VERIFICATION_COMMAND_PATTERN.test(command)) {
      rawEvidence.push(normalizeCommand(command));
    }
  }

  const resultPattern = new RegExp(REAL_VERIFICATION_RESULT_PATTERN.source, 'gi');
  for (const match of text.matchAll(resultPattern)) {
    rawEvidence.push(match[0]);
  }

  const evidence = [...new Set(rawEvidence)].slice(0, 10).map((value) => ({
    value,
    ...classifyVerificationEvidence(value),
  }));

  return {
    count: evidence.length,
    totalWeight: evidence.reduce((sum, entry) => sum + entry.weight, 0),
    evidence,
  };
}

function classifyVerificationQuality(realVerification) {
  const hasRuntime = realVerification.evidence.some((entry) => entry.type === 'runtime-check');
  const hasScript = realVerification.evidence.some((entry) => entry.type === 'validation-script');
  const hasLog = realVerification.evidence.some((entry) => entry.type === 'log-check');

  if (hasRuntime) return 'runtime-verified';
  if (hasScript || realVerification.totalWeight >= 4) return 'script-verified';
  if (hasLog || realVerification.totalWeight >= 2) return 'log-verified';
  return 'unverified';
}

function verificationQualityRank(quality) {
  if (quality === 'runtime-verified') return 4;
  if (quality === 'script-verified') return 3;
  if (quality === 'log-verified') return 2;
  return 1;
}

function scoreRumination(text, commandStats) {
  const lines = text.split('\n').filter(Boolean);
  const planningPhrases = collectMatches(text, RUMINATION_PATTERN).length;
  const claims = collectMatches(text, CLAIM_PATTERN).length;
  const actions = collectMatches(text, ACTION_PATTERN).length + commandStats.writeCommands + commandStats.readCommands;
  const planningRatio = ratio(planningPhrases, Math.max(lines.length, 1));
  const actionRatio = ratio(actions + claims, planningPhrases + actions + claims + 1);

  return {
    score: clamp(100 - Math.round(planningRatio * 500) + Math.round(actionRatio * 120)),
    planningPhrases,
    planningRatio,
    actionRatio,
  };
}

function scorePromptCompliance(text, scopeRules) {
  if (!scopeRules.allowed.length && !scopeRules.forbidden.length && !scopeRules.requiredOutputs.length) {
    return {
      score: 50,
      findings: [],
      evidence: ['No explicit scope rules detected in prompt/brief.'],
    };
  }

  const findings = [];
  const evidence = [];
  const paths = extractPaths(text);
  let penalty = 0;

  for (const forbidden of scopeRules.forbidden) {
    const touched = paths.filter((candidate) => matchScope(candidate, forbidden));
    if (touched.length) {
      penalty += 30;
      findings.push({
        severity: 'P1',
        title: `Transcript touched forbidden scope: ${forbidden}`,
        detail: touched.slice(0, 5).join(', '),
      });
    }
  }

  for (const allowed of scopeRules.allowed) {
    const touched = paths.filter((candidate) => matchScope(candidate, allowed));
    if (touched.length) {
      evidence.push(`Detected in-scope paths for ${allowed}.`);
    } else {
      penalty += 10;
      findings.push({
        severity: 'P2',
        title: `No evidence of work in allowed scope: ${allowed}`,
        detail: 'Transcript did not mention paths matching the declared writable area.',
      });
    }
  }

  for (const required of scopeRules.requiredOutputs) {
    if (text.includes(required)) {
      evidence.push(`Detected required output path ${required}.`);
    } else {
      penalty += 20;
      findings.push({
        severity: 'P1',
        title: `Missing required output mention: ${required}`,
        detail: 'Prompt asked for a concrete report/output path that is not detected in transcript.',
      });
    }
  }

  return { score: clamp(100 - penalty), findings, evidence };
}

function scoreCompletion(text, scopeRules) {
  const missing = [];
  const evidence = [];
  let achieved = 0;
  let total = 1;

  if (scopeRules.requiredOutputs.length) total += scopeRules.requiredOutputs.length;

  for (const output of scopeRules.requiredOutputs) {
    if (text.includes(output)) {
      achieved += 1;
      evidence.push(`Output path mentioned: ${output}`);
    } else {
      missing.push(`Missing output artifact mention: ${output}`);
    }
  }

  total += 1;
  if (FILE_REPORT_PATTERN.test(text)) {
    achieved += 1;
    evidence.push('Detected file-by-file bullet-style report structure.');
  } else {
    missing.push('No structured changed-files deliverable list detected.');
  }

  total += 1;
  if (VERIFICATION_PATTERN.test(text)) {
    achieved += 1;
    evidence.push('Detected verification/test language.');
  } else {
    missing.push('No verification language detected.');
  }

  total += 1;
  if (/\b(could not verify|couldn't verify|not verify|unverified|headless|blocked|boundary)\b/i.test(text)) {
    achieved += 1;
    evidence.push('Detected explicit verification boundary or unknowns reporting.');
  } else {
    missing.push('No explicit verification boundary/uncertainty section detected.');
  }

  return {
    percent: clamp(Math.round((achieved / total) * 100)),
    achieved,
    total,
    missing,
    evidence,
  };
}

function scoreHallucinationRisk(text, commandStats) {
  const claims = collectMatches(text, CLAIM_PATTERN).length;
  const uncertainty = collectMatches(text, UNCERTAINTY_PATTERN).length;
  const verificationSignals = collectMatches(text, VERIFICATION_PATTERN).length;
  const fileSpecificity = extractPaths(text).length;
  const riskDisclosure = uncertainty > 0;
  const unsupportedClaims = Math.max(0, claims - verificationSignals - Math.min(fileSpecificity, claims));
  const repeatedCommandPenalty = Math.min(commandStats.repeated.length * 4, 16);

  return {
    score: clamp(100 - unsupportedClaims * 12 - repeatedCommandPenalty + (riskDisclosure ? 6 : 0)),
    claims,
    uncertainty,
    verificationSignals,
    fileSpecificity,
    unsupportedClaims,
    riskDisclosure,
  };
}

function scoreOptimality(tokenSummary, commandStats, rumination) {
  let score = 100;
  score -= tokenSummary.total > 0 ? Math.min(Math.round(tokenSummary.total / 1500), 18) : 8;
  score -= Math.min(commandStats.repeated.length * 5, 20);
  score -= Math.round((1 - rumination.actionRatio) * 15);

  return {
    score: clamp(score),
    repeatedCommands: commandStats.repeated.length,
    tokenCostObserved: tokenSummary.total,
  };
}

function scoreSolutionSense(promptCompliance, completion, hallucinationRisk, hasVerification) {
  return {
    score: clamp(
      Math.round(
        promptCompliance.score * 0.35 +
        completion.percent * 0.3 +
        hallucinationRisk.score * 0.2 +
        (hasVerification ? 15 : 0),
      ),
    ),
    hasVerification,
  };
}

function buildFindings({ tokenSummary, commandStats, promptCompliance, completion, rumination, hallucinationRisk, verificationQuality }) {
  const findings = [...promptCompliance.findings];

  if (!tokenSummary.evidence.length) {
    findings.push({
      severity: 'P1',
      title: 'No token usage evidence detected',
      detail: 'Transcript does not expose token counters, so spend cannot be measured reliably.',
    });
  }

  if (commandStats.repeated.length) {
    findings.push({
      severity: 'P2',
      title: 'Repeated commands suggest inefficiency',
      detail: commandStats.repeated.slice(0, 3).map(({ command, count }) => `${count}x ${command}`).join('; '),
    });
  }

  if (completion.percent < 70) {
    findings.push({
      severity: 'P1',
      title: 'Low completion score against detected deliverables',
      detail: completion.missing.slice(0, 4).join('; '),
    });
  }

  if (rumination.score < 55) {
    findings.push({
      severity: 'P2',
      title: 'Transcript shows elevated rumination signals',
      detail: `Planning phrases detected ${rumination.planningPhrases} times.`,
    });
  }

  if (hallucinationRisk.score < 60) {
    findings.push({
      severity: 'P1',
      title: 'Claim-to-evidence balance indicates hallucination risk',
      detail: `Unsupported claims estimate: ${hallucinationRisk.unsupportedClaims}.`,
    });
  }

  if (completion.percent >= 75 && verificationQuality === 'unverified') {
    findings.push({
      severity: 'P1',
      title: 'High completion reported without hard verification',
      detail: `Completion is ${completion.percent}% but no real verification evidence was detected.`,
    });
  }

  findings.sort((left, right) => severityRank(left.severity) - severityRank(right.severity));
  return findings;
}

function buildStyleProfile(label, report) {
  const commands = report.metrics.commands;
  const byCategory = commands.commandBreakdown.byCategory;
  const totalCommands = Math.max(commands.total, 1);
  const tokenCost = Math.max(report.metrics.tokenUsage.total, 1);
  const verificationWeight = report.metrics.realVerification.totalWeight;
  const verificationQuality = report.metrics.verificationQuality;
  const hasVerification = verificationWeight > 0;
  const readRatio = ratio(byCategory.read, totalCommands);
  const penalties = {
    excessiveReads: readRatio >= 0.8 && byCategory.read >= 12 ? 10 : readRatio >= 0.65 && byCategory.read >= 8 ? 5 : 0,
    lowWrites: byCategory.write === 0 ? 12 : byCategory.write === 1 ? 6 : 0,
    noRealVerification: hasVerification ? 0 : 14,
    repeatedCommands: commands.repeated.length * 4,
  };
  const bonuses = {
    verification: Math.min(verificationWeight * 2, 12),
    writeActivity: Math.min(byCategory.write * 3, 10),
  };
  const baseScore =
    report.scores.optimality.score * 0.4 +
    report.scores.rumination.score * 0.2 +
    report.metrics.completion.percent * 0.2;
  const styleScore = clamp(
    Math.round(
      baseScore +
      bonuses.verification +
      bonuses.writeActivity -
      penalties.excessiveReads -
      penalties.lowWrites -
      penalties.noRealVerification -
      penalties.repeatedCommands,
    ),
  );

  const styleTags = [];
  if (readRatio >= 0.6) styleTags.push('read-heavy');
  if (byCategory.write <= 2 && byCategory.read > byCategory.write * 4) styleTags.push('write-light');
  if (hasVerification) styleTags.push('verification-aware');
  if (verificationWeight >= 5) styleTags.push('runtime-verified');
  if (commands.repeated.length > 0 || byCategory.write >= 2) styleTags.push('iterative');
  if (byCategory.git >= 3) styleTags.push('git-heavy');
  if (!styleTags.length) styleTags.push('mixed');

  return {
    label,
    tokenCost: report.metrics.tokenUsage.total,
    completionPer1kTokens: Math.round((report.metrics.completion.percent / tokenCost) * 1000 * 100) / 100,
    styleScore,
    completionPercent: report.metrics.completion.percent,
    optimalityScore: report.scores.optimality.score,
    ruminationScore: report.scores.rumination.score,
    verificationQuality,
    verificationWeight,
    repeatedCommands: commands.repeated.length,
    commandMix: byCategory,
    styleScoreComponents: {
      baseScore: Math.round(baseScore),
      bonuses,
      penalties,
    },
    styleTags,
  };
}

function addRanks(entries, valueKey, direction = 'desc') {
  const sorted = [...entries].sort((left, right) => {
    if (direction === 'asc') {
      return left[valueKey] - right[valueKey] || left.label.localeCompare(right.label);
    }
    return right[valueKey] - left[valueKey] || left.label.localeCompare(right.label);
  });

  return sorted.map((entry, index) => ({ rank: index + 1, ...entry }));
}

function riskyCompletionRank(profile, report) {
  return {
    riskEligible: profile.verificationQuality === 'unverified' ? 1 : 0,
    completionPercent: profile.completionPercent,
    severityWeight: findingsSeverityWeight(report.findings),
  };
}

function buildAuditSummaryEntry({ label, input, report }) {
  let verdict = 'needs-review';

  if (report.metrics.verificationQuality === 'runtime-verified' && report.summary.topSeverity === 'P2' && report.metrics.completion.percent >= 80) {
    verdict = 'strongly-verified';
  } else if (report.metrics.verificationQuality !== 'unverified' && report.metrics.completion.percent >= 70) {
    verdict = 'verified-progress';
  } else if (report.metrics.completion.percent >= 75 && report.metrics.verificationQuality === 'unverified') {
    verdict = 'risky-complete';
  } else if (report.metrics.completion.percent < 70) {
    verdict = 'incomplete';
  }

  return {
    label,
    input,
    verdict,
    completionPercent: report.metrics.completion.percent,
    verificationQuality: report.metrics.verificationQuality,
    topSeverity: report.summary.topSeverity,
  };
}

function topSeverity(findings) {
  return findings.length ? findings[0].severity : DEFAULT_SEVERITY;
}

function compareAuditBundles(currentBundle, baselineBundle) {
  const baselineByLabel = new Map(baselineBundle.reports.map((entry) => [entry.label, entry]));
  const deltas = [];
  let improved = 0;
  let regressed = 0;
  let mixed = 0;

  for (const current of currentBundle.reports) {
    const baseline = baselineByLabel.get(current.label);
    if (!baseline) continue;

    const completionDelta = current.report.metrics.completion.percent - baseline.report.metrics.completion.percent;
    const tokenDelta = current.report.metrics.tokenUsage.total - baseline.report.metrics.tokenUsage.total;
    const styleDelta = current.report.scores.optimality.score - baseline.report.scores.optimality.score;
    const currentEfficiency = current.report.metrics.tokenUsage.total > 0
      ? current.report.metrics.completion.percent / current.report.metrics.tokenUsage.total
      : 0;
    const baselineEfficiency = baseline.report.metrics.tokenUsage.total > 0
      ? baseline.report.metrics.completion.percent / baseline.report.metrics.tokenUsage.total
      : 0;
    const efficiencyDelta = currentEfficiency - baselineEfficiency;
    const verificationQualityChanged = current.report.metrics.verificationQuality !== baseline.report.metrics.verificationQuality;
    const currentSeverity = topSeverity(current.report.findings);
    const baselineSeverity = topSeverity(baseline.report.findings);
    const severityDelta = severityRank(baselineSeverity) - severityRank(currentSeverity);

    let direction = 'mixed';
    const tokenOnlyRegression = tokenDelta > 0 && completionDelta <= 0;
    const inefficientGrowthRegression = tokenDelta >= 50 && efficiencyDelta < 0;
    const positiveSignals = Number(completionDelta > 0) + Number(severityDelta > 0) + Number(verificationQualityChanged) + Number(tokenDelta < 0);
    const negativeSignals = Number(tokenOnlyRegression || completionDelta < 0) + Number(inefficientGrowthRegression) + Number(severityDelta < 0);

    if (negativeSignals > 0 && positiveSignals === 0) {
      direction = 'regressed';
      regressed += 1;
    } else if (positiveSignals > 0 && negativeSignals === 0) {
      direction = 'improved';
      improved += 1;
    } else {
      mixed += 1;
    }

    deltas.push({
      label: current.label,
      input: current.input,
      baselineInput: baseline.input,
      direction,
      completionDelta,
      tokenDelta,
      optimalityDelta: styleDelta,
      efficiencyDelta: Math.round(efficiencyDelta * 10000) / 10000,
      verificationQualityFrom: baseline.report.metrics.verificationQuality,
      verificationQualityTo: current.report.metrics.verificationQuality,
      topSeverityFrom: baselineSeverity,
      topSeverityTo: currentSeverity,
      verdictFrom: baselineBundle.auditSummary.find((entry) => entry.label === current.label)?.verdict || 'unknown',
      verdictTo: currentBundle.auditSummary.find((entry) => entry.label === current.label)?.verdict || 'unknown',
    });
  }

  return {
    comparedAgents: deltas.length,
    summary: { improved, regressed, mixed },
    deltas: deltas.sort((left, right) => left.label.localeCompare(right.label)),
  };
}

export function analyzeAgentTranscript(logText, options = {}) {
  const briefText = options.briefText || extractBriefFromLog(logText);
  const workText = extractWorkSection(logText);
  const scopeRules = parseScopeRules(briefText);
  const tokenUsage = summarizeTokens(logText);
  const commands = extractCommands(workText);
  const commandStats = analyzeCommands(commands);
  const realVerification = detectRealVerification(commands, workText);
  const verificationQuality = classifyVerificationQuality(realVerification);
  const promptCompliance = scorePromptCompliance(workText, scopeRules);
  const completion = scoreCompletion(logText, scopeRules);
  const rumination = scoreRumination(workText, commandStats);
  const hallucinationRisk = scoreHallucinationRisk(workText, commandStats);
  const optimality = scoreOptimality(tokenUsage, commandStats, rumination);
  const solutionSense = scoreSolutionSense(promptCompliance, completion, hallucinationRisk, realVerification.totalWeight > 0);
  const findings = buildFindings({
    tokenSummary: tokenUsage,
    commandStats,
    promptCompliance,
    completion,
    rumination,
    hallucinationRisk,
    verificationQuality,
  });

  return {
    summary: {
      promptCoverage: promptCompliance.score,
      completionPercent: completion.percent,
      topSeverity: findings[0]?.severity || DEFAULT_SEVERITY,
    },
    scores: {
      optimality,
      solutionSense,
      rumination,
      hallucinationRisk,
    },
    metrics: {
      tokenUsage,
      commands: commandStats,
      realVerification,
      verificationQuality,
      promptCompliance,
      completion,
      scopeRules,
    },
    findings,
    realVerificationEvidence: realVerification.evidence,
    evidence: {
      realVerification: realVerification.evidence.map((entry) => `${entry.type}:${entry.weight} ${entry.value}`),
      promptCompliance: promptCompliance.evidence,
      completion: completion.evidence,
      tokens: tokenUsage.evidence.map((entry) => `${entry.value} from "${entry.match}"`),
    },
  };
}

export function analyzeAgentLogs(entries, options = {}) {
  const reports = entries.map(({ input, text }) => ({
    input,
    label: path.basename(input),
    report: analyzeAgentTranscript(text, options),
  }));
  const auditSummary = reports.map(buildAuditSummaryEntry);
  const profiles = reports.map(({ input, label, report }) => ({ input, ...buildStyleProfile(label, report) }));

  let comparison = null;
  if (reports.length > 1) {
    comparison = {
      comparedLogs: reports.length,
      ranking: {
        lowestTokenCost: addRanks(
          profiles.map((profile) => ({ label: profile.label, input: profile.input, tokenCost: profile.tokenCost })),
          'tokenCost',
          'asc',
        ),
        highestStyleScore: addRanks(
          profiles.map((profile) => ({ label: profile.label, input: profile.input, styleScore: profile.styleScore, styleTags: profile.styleTags })),
          'styleScore',
          'desc',
        ),
        highestCompletionPerToken: addRanks(
          profiles.map((profile) => ({
            label: profile.label,
            input: profile.input,
            completionPer1kTokens: profile.completionPer1kTokens,
            completionPercent: profile.completionPercent,
            tokenCost: profile.tokenCost,
          })),
          'completionPer1kTokens',
          'desc',
        ),
        bestVerifiedAgent: profiles
          .map((profile) => {
            const report = reports.find((entry) => entry.label === profile.label)?.report;
            return {
              label: profile.label,
              input: profile.input,
              verificationQuality: profile.verificationQuality,
              verificationWeight: profile.verificationWeight,
              verificationRank: verificationQualityRank(profile.verificationQuality),
              findingsSeverityWeight: findingsSeverityWeight(report?.findings || []),
            };
          })
          .sort((left, right) =>
            right.verificationRank - left.verificationRank ||
            right.verificationWeight - left.verificationWeight ||
            left.findingsSeverityWeight - right.findingsSeverityWeight ||
            left.label.localeCompare(right.label),
          )
          .map((entry, index) => ({
            rank: index + 1,
            label: entry.label,
            input: entry.input,
            verificationQuality: entry.verificationQuality,
            verificationWeight: entry.verificationWeight,
            findingsSeverityWeight: entry.findingsSeverityWeight,
          })),
        mostRiskyCompletion: reports
          .map(({ input, label, report }) => {
            const profile = profiles.find((entry) => entry.label === label);
            const risk = riskyCompletionRank(profile, report);
            return {
              label,
              input,
              verificationQuality: profile.verificationQuality,
              completionPercent: profile.completionPercent,
              severityWeight: risk.severityWeight,
              riskEligible: risk.riskEligible,
            };
          })
          .sort((left, right) =>
            right.riskEligible - left.riskEligible ||
            right.completionPercent - left.completionPercent ||
            right.severityWeight - left.severityWeight ||
            left.label.localeCompare(right.label),
          )
          .map((entry, index) => ({ rank: index + 1, ...entry })),
      },
      profiles,
    };
  }

  const bundle = {
    generatedAt: new Date().toISOString(),
    auditSummary,
    reports,
    comparison,
  };

  if (options.compareToEntries?.length) {
    const baselineBundle = analyzeAgentLogs(options.compareToEntries, { ...options, compareToEntries: [] });
    bundle.timeComparison = compareAuditBundles(bundle, baselineBundle);
  }

  return bundle;
}

export function renderMarkdownReport(report, label = 'agent log') {
  const lines = [];

  lines.push(`# Agent Audit: ${label}`);
  lines.push('');
  lines.push(`- Summary: completion ${report.metrics.completion.percent}%, verification ${report.metrics.verificationQuality}, severity ${report.summary.topSeverity}, tokens ${report.metrics.tokenUsage.total}`);
  lines.push(`- Scores: optimality ${report.scores.optimality.score}, solution ${report.scores.solutionSense.score}, rumination ${report.scores.rumination.score}, hallucination ${report.scores.hallucinationRisk.score}, prompt ${report.metrics.promptCompliance.score}`);
  lines.push('');
  lines.push('## Findings');
  lines.push('');

  if (!report.findings.length) lines.push('- None');
  else for (const finding of report.findings) lines.push(`- ${finding.severity} ${finding.title}: ${finding.detail}`);

  lines.push('');
  lines.push('## Evidence');
  lines.push('');
  lines.push(`- Commands: ${report.metrics.commands.total} total, ${report.metrics.commands.repeated.length} repeated, ${report.metrics.commands.commandBreakdown.byCategory.read} read, ${report.metrics.commands.commandBreakdown.byCategory.write} write, ${report.metrics.commands.commandBreakdown.byCategory.verify} verify`);
  lines.push(`- Deliverables: ${report.metrics.completion.achieved}/${report.metrics.completion.total}`);
  lines.push(`- Token evidence: ${report.metrics.tokenUsage.evidence.length} entries`);
  if (report.metrics.completion.missing.length) {
    lines.push(`- Missing deliverables/signals: ${report.metrics.completion.missing.join('; ')}`);
  }

  lines.push('');
  lines.push('## Verification Evidence');
  lines.push('');
  if (report.realVerificationEvidence.length) {
    for (const evidence of report.realVerificationEvidence) {
      lines.push(`- [${evidence.type} w=${evidence.weight}] ${evidence.value}`);
    }
  } else {
    lines.push('- None');
  }

  return lines.join('\n');
}

export function renderMarkdownComparison(bundle) {
  if (!bundle.comparison) return '';

  const lines = [];
  lines.push('# Agent Audit Comparison');
  lines.push('');
  lines.push(`- Compared logs: ${bundle.comparison.comparedLogs}`);
  lines.push('');
  lines.push('## Audit Summary');
  lines.push('');
  for (const entry of bundle.auditSummary) {
    lines.push(`- ${entry.label}: ${entry.verdict} (${entry.completionPercent}%, ${entry.verificationQuality}, ${entry.topSeverity})`);
  }
  lines.push('');
  lines.push('## Verdict Legend');
  lines.push('');
  lines.push('- `strongly-verified`: high completion with strong verification evidence and no major findings.');
  lines.push('- `verified-progress`: meaningful progress with some hard verification, but not at the strongest bar.');
  lines.push('- `risky-complete`: high completion reported without hard verification evidence.');
  lines.push('- `incomplete`: completion is still materially below the expected finish line.');
  if (bundle.timeComparison) {
    lines.push('');
    lines.push('## Time Comparison');
    lines.push('');
    lines.push(`- Agents compared: ${bundle.timeComparison.comparedAgents}`);
    lines.push(`- Change summary: improved ${bundle.timeComparison.summary.improved}, regressed ${bundle.timeComparison.summary.regressed}, mixed ${bundle.timeComparison.summary.mixed}`);
    for (const entry of bundle.timeComparison.deltas) {
      lines.push(`- ${entry.label}: ${entry.direction}, completion ${entry.completionDelta >= 0 ? '+' : ''}${entry.completionDelta}, tokens ${entry.tokenDelta >= 0 ? '+' : ''}${entry.tokenDelta}, verification ${entry.verificationQualityFrom} -> ${entry.verificationQualityTo}, verdict ${entry.verdictFrom} -> ${entry.verdictTo}`);
    }
  }
  lines.push('');
  lines.push('## Rankings');
  lines.push('');
  lines.push('### Lowest Token Cost');
  lines.push('');
  for (const entry of bundle.comparison.ranking.lowestTokenCost) lines.push(`- ${entry.rank}. ${entry.label}: ${entry.tokenCost} tokens`);
  lines.push('');
  lines.push('### Highest Style Score');
  lines.push('');
  for (const entry of bundle.comparison.ranking.highestStyleScore) lines.push(`- ${entry.rank}. ${entry.label}: ${entry.styleScore}/100 (${entry.styleTags.join(', ')})`);
  lines.push('');
  lines.push('### Best Completion Per Token');
  lines.push('');
  for (const entry of bundle.comparison.ranking.highestCompletionPerToken) lines.push(`- ${entry.rank}. ${entry.label}: ${entry.completionPer1kTokens} completion/1k tokens (${entry.completionPercent}% @ ${entry.tokenCost} tokens)`);
  lines.push('');
  lines.push('### Best Verified Agent');
  lines.push('');
  for (const entry of bundle.comparison.ranking.bestVerifiedAgent) lines.push(`- ${entry.rank}. ${entry.label}: ${entry.verificationQuality} (weight ${entry.verificationWeight}, findings ${entry.findingsSeverityWeight})`);
  lines.push('');
  lines.push('### Most Risky Completion');
  lines.push('');
  for (const entry of bundle.comparison.ranking.mostRiskyCompletion) lines.push(`- ${entry.rank}. ${entry.label}: ${entry.completionPercent}% (${entry.verificationQuality}, severity ${entry.severityWeight})`);
  lines.push('');
  lines.push('## Agent Profiles');
  lines.push('');
  for (const profile of bundle.comparison.profiles) {
    const verdict = bundle.auditSummary.find((entry) => entry.label === profile.label)?.verdict;
    lines.push(`- ${profile.label}: verdict ${verdict}, completion ${profile.completionPercent}%, verification ${profile.verificationQuality}, style ${profile.styleScore}, cost ${profile.tokenCost}, efficiency ${profile.completionPer1kTokens}/1k, tags ${profile.styleTags.join(', ')}`);
  }

  return lines.join('\n');
}

function buildBundle(entries, options = {}) {
  return analyzeAgentLogs(entries, options);
}

function parseArgs(argv) {
  const args = {
    inputs: [],
    compareToInputs: [],
    brief: null,
    format: 'json',
    output: null,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const current = argv[index];
    if (current === '--brief') {
      args.brief = argv[index + 1];
      index += 1;
      continue;
    }
    if (current === '--format') {
      args.format = argv[index + 1] || 'json';
      index += 1;
      continue;
    }
    if (current === '--output') {
      args.output = argv[index + 1];
      index += 1;
      continue;
    }
    if (current === '--compare-to') {
      args.compareToInputs.push(argv[index + 1]);
      index += 1;
      continue;
    }
    args.inputs.push(current);
  }

  return args;
}

function main(argv) {
  const args = parseArgs(argv);
  if (!args.inputs.length) {
    throw new Error('Usage: node scripts/agent-audit.mjs <log...> [--compare-to file ...] [--brief file] [--format json|md] [--output file]');
  }

  const briefText = args.brief ? fs.readFileSync(args.brief, 'utf8') : null;
  const entries = args.inputs.map((input) => ({ input, text: fs.readFileSync(input, 'utf8') }));
  const compareToEntries = args.compareToInputs.map((input) => ({ input, text: fs.readFileSync(input, 'utf8') }));
  const bundle = buildBundle(entries, { briefText, compareToEntries });

  let rendered;
  if (args.format === 'md') {
    const parts = [];
    const comparison = renderMarkdownComparison(bundle);
    if (comparison) parts.push(comparison);
    for (const { input, report } of bundle.reports) parts.push(renderMarkdownReport(report, path.basename(input)));
    rendered = parts.join('\n\n---\n\n');
  } else {
    rendered = JSON.stringify(bundle, null, 2);
  }

  if (args.output) fs.writeFileSync(args.output, rendered);
  else process.stdout.write(`${rendered}\n`);
}

const isEntrypoint = process.argv[1] && path.resolve(process.argv[1]) === path.resolve(new URL(import.meta.url).pathname);

if (isEntrypoint) {
  try {
    main(process.argv.slice(2));
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 1;
  }
}
