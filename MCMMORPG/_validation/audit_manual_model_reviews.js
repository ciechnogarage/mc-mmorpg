const fs = require('fs');
const path = require('path');
const { auditReviewDatabase } = require('./manual_review_contract');

const root = path.join(__dirname, 'reference_corpus');
const index = JSON.parse(fs.readFileSync(path.join(root, 'manual_visual_reviews.json'), 'utf8'));
const cardsDocument = JSON.parse(fs.readFileSync(path.join(root, 'modelengine-model-cards.json'), 'utf8'));
const cards = cardsDocument.cards.map((card) => ({
  ...card,
  absoluteFile: path.resolve(cardsDocument.corpus, card.file),
}));
const batches = (index.batchFiles || []).map((file) =>
  JSON.parse(fs.readFileSync(path.join(root, file), 'utf8')));
const report = auditReviewDatabase(index, batches, cards, root);

if (process.argv.includes('--json')) {
  process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
} else {
  console.log(
    `MANUAL_REVIEW_AUDIT: strict=${report.counts.strictModels}/${cards.length} models, ` +
    `${report.counts.strictAnimations}/${index.progress.animationsTotal} animations; ` +
    `legacy=${report.counts.legacyModels} models/${report.counts.legacyAnimations} animations; ` +
    `stale=${report.counts.staleModels}; missing=${report.counts.missingModels}; errors=${report.errors.length}`,
  );
  const incompleteFamilies = Object.entries(report.familyCoverage)
    .filter(([, coverage]) => coverage.missing > 0 || coverage.legacy > 0)
    .sort((a, b) => b[1].total - a[1].total || a[0].localeCompare(b[0]));
  console.log(`INCOMPLETE_FAMILIES: ${incompleteFamilies.length}`);
  for (const [family, coverage] of incompleteFamilies.slice(0, 20)) {
    console.log(
      `- ${family}: strict=${coverage.strict}, legacy=${coverage.legacy}, ` +
      `missing=${coverage.missing}, total=${coverage.total}`,
    );
  }
}

if (report.errors.length > 0) {
  for (const error of report.errors) console.error(`ERROR: ${error}`);
  process.exitCode = 1;
}
