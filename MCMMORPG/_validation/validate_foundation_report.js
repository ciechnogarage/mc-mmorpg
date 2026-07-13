const fs = require('fs');
const path = require('path');

function parseReport(content) {
  const verdictMatch = content.match(/^- verdict: `([^`]+)`/m);
  const verdict = verdictMatch ? verdictMatch[1] : '';
  const milestoneStatuses = [...content.matchAll(/^- `([^`]+)` ([A-Z_]+)/gm)].map(match => ({
    name: match[1],
    status: match[2]
  }));
  const blockerSection = content.match(/^## Blockers\n\n([\s\S]*?)(?:\n## |\s*$)/m);
  const blockers = blockerSection
    ? blockerSection[1].split('\n').filter(line => line.startsWith('- ')).map(line => line.slice(2).trim())
    : [];
  return { verdict, milestoneStatuses, blockers };
}

function validateParsedReport(report) {
  const hasBlockedMilestone = report.milestoneStatuses.some(entry => entry.status === 'BLOCKED');
  const hasFailMilestone = report.milestoneStatuses.some(entry => entry.status === 'FAIL');
  const hasInsufficientMilestone = report.milestoneStatuses.some(entry => entry.status === 'INSUFFICIENT_EVIDENCE');

  if (report.verdict === 'PASS' && (hasBlockedMilestone || hasFailMilestone || hasInsufficientMilestone || report.blockers.length)) {
    throw new Error('PASS report cannot contain blockers or non-pass milestones');
  }
  if (report.verdict === 'BLOCKED' && !hasBlockedMilestone && !report.blockers.length) {
    throw new Error('BLOCKED report must name at least one blocked milestone or blocker');
  }
  if (report.verdict === 'FAIL' && !hasFailMilestone) {
    throw new Error('FAIL report must contain a FAIL milestone');
  }
  if (report.verdict === 'INSUFFICIENT_EVIDENCE' && !hasInsufficientMilestone && !report.blockers.length) {
    throw new Error('INSUFFICIENT_EVIDENCE report must name missing evidence');
  }
}

function main() {
  const reportPath = process.argv[2];
  if (!reportPath) {
    console.error('Usage: node MCMMORPG/_validation/validate_foundation_report.js <report.md>');
    process.exit(1);
  }
  const absolutePath = path.resolve(reportPath);
  const content = fs.readFileSync(absolutePath, 'utf8');
  const parsed = parseReport(content);
  validateParsedReport(parsed);
  console.log(`FOUNDATION_REPORT_OK ${absolutePath}`);
}

if (require.main === module) {
  try {
    main();
  } catch (error) {
    console.error(`FOUNDATION_REPORT_INVALID ${error.message}`);
    process.exit(1);
  }
}
