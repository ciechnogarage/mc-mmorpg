# Strict Review Workflow

This workflow closes the loop from boss detection to reusable strict-review authoring.

## Order

1. Run `npm run modelengine:boss-reviews`.
2. Inspect `reference_corpus/boss_review_dashboard.md`.
3. Inspect `active_runtime_reviews/runtime_boss_review_templates.json`.
4. Use `active_runtime_reviews/runtime_boss_review_drafts.json` as the authoring scaffold for the next runtime boss review.
5. Promote only finished manual review content into
   `active_runtime_reviews/completed_runtime_boss_reviews.json`.
6. Run `npm run modelengine:boss-reviews:test` and `npm run modelengine:manual-reviews:test`.

## Outputs

- `reference_corpus/boss_review_pipeline.json`
  Runtime-aware queue state, confirmed-family strict queue, recommended confirmed-family batch, and candidate confirmation queue.
- `reference_corpus/boss_review_dashboard.md`
  Human-readable dashboard for corpus and runtime review state.
- `active_runtime_reviews/runtime_boss_review_templates.json`
  Generated runtime-boss template inventory with evidence anchors and per-animation checklists.
- `active_runtime_reviews/runtime_boss_review_drafts.json`
  Generated authoring scaffold for the next strict runtime review pass.
- `active_runtime_reviews/completed_runtime_boss_reviews.json`
  Evidence-backed strict reviews for active runtime bosses. Completion removes
  the boss from the pending draft queue but does not erase explicit runtime
  unknowns.

## Intent

- Keep `active runtime repo` review work separate from the corpus backlog.
- Use a `strict_ready` runtime boss as the golden path for the authoring process.
- Batch corpus work by confirmed families after the runtime workflow is proven, not before.
- Treat template and draft generation as required process artifacts, not optional helper output.
