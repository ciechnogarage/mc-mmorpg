# Wallet Writer-Freeze Manifest Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a machine-readable pre-cutover writer-freeze snapshot and a deterministic two-mode validator that keeps the current state structurally valid but readiness-blocked.

**Architecture:** A canonical YAML audit manifest records every capability and applicable backend tuple. A single Python CLI validates structure/drift in its default mode and adds strict readiness gates under `--require-ready`; Node tests exercise the CLI through real subprocesses and temporary manifest variants.

**Tech Stack:** YAML 1.2-compatible documents parsed with PyYAML, Python 3 standard library plus existing PyYAML, Node.js built-in test runner.

## Global Constraints

- Do not modify plugin configuration, runtime deployment, balances, permissions, databases, player data, worlds, or secrets.
- Do not start Paper or Docker backends; this increment produces static evidence only.
- Required backends are exactly `items`, `world`, and `hub`.
- Required central provider is `MCMMORPGCentralWalletVaultProvider`, contract version `2`, write mode `context_required`.
- Generic Vault writes must be proven to fail with `WRITE_CONTEXT_REQUIRED` before readiness can pass.
- The initial real manifest must remain `BLOCKED`; only a synthetic test fixture may be strict-ready.
- Structural invalidity always exits `1`; missing readiness evidence exits `0` by default and `1` with `--require-ready`.

---

### Task 1: Freeze manifest shape and current blocked snapshot

**Files:**
- Create: `tests/wallet-writer-freeze.test.mjs`
- Create: `docs/audits/2026-08-12/wallet-writer-freeze-manifest.yaml`

**Interfaces:**
- Consumes: capability IDs from `docs/ai/cmi-vault-gold-writer-capability-matrix.md`; plugin versions from `docs/plugin_manifest.yaml`.
- Produces: YAML keys `schema_version`, `manifest_id`, `readiness`, `sources`, `required_backend_set`, `provider_requirement`, `capabilities`, `freeze_records`, `backend_acknowledgements`, and `legacy_snapshot` for the validator.

- [ ] **Step 1: Write the failing default-mode subprocess test**

Create `tests/wallet-writer-freeze.test.mjs` with a helper that runs:

```js
const runValidator = (args = []) => spawnSync(
  'python3',
  ['scripts/check-wallet-writer-freeze.py', ...args],
  { cwd: ROOT, encoding: 'utf8' }
);

test('current writer-freeze manifest is structurally valid and readiness blocked', () => {
  const result = runValidator();
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /STRUCTURE PASS/);
  assert.match(result.stdout, /READINESS BLOCKED/);
});
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `node --test tests/wallet-writer-freeze.test.mjs`

Expected: FAIL because `scripts/check-wallet-writer-freeze.py` does not exist.

- [ ] **Step 3: Create the complete blocked manifest**

Add all 28 catalog IDs from the matrix. Expand every applicable backend into a
unique freeze record. Use `PENDING`/`MISSING`, null runtime hashes/provider
facts, and `readiness: BLOCKED`; never invent a proof. Store the current matrix
SHA-256 and exact declared plugin versions.

Representative catalog entry:

```yaml
- capability_id: GOLD-WRITER-006
  classification: ACTIVE_CONFIGURED
  plugin_name: CMI
  plugin_version: 9.8.4.14
  applicable_backends: [items, hub]
  mutation_route: console_scheduler
  observed_state: configured_active_or_backend_default_unverified
  required_cutover_state: disabled_or_migrated_first_party
  permission_only_freeze_allowed: false
```

Representative freeze record:

```yaml
- backend_id: hub
  capability_id: GOLD-WRITER-006
  plugin_name: CMI
  plugin_version: 9.8.4.14
  mutation_route: console_scheduler
  effective_exposure: pending
  vendor_jar_sha256: null
  freeze_status: PENDING
  proofs:
    exact_jar_scan: {status: MISSING, evidence_ref: null}
    effective_permission: {status: MISSING, evidence_ref: null}
    provider_identity: {status: MISSING, evidence_ref: null}
    focused_mutation_rejection: {status: MISSING, evidence_ref: null}
```

- [ ] **Step 4: Keep the test RED for the correct remaining reason**

Run: `node --test tests/wallet-writer-freeze.test.mjs`

Expected: FAIL only because the validator is still missing; the manifest now
exists and parses with `python3 -c 'import yaml; yaml.safe_load(open(...))'`.

- [ ] **Step 5: Commit the manifest and RED test**

```bash
git add docs/audits/2026-08-12/wallet-writer-freeze-manifest.yaml tests/wallet-writer-freeze.test.mjs
git commit -m "Add blocked wallet writer-freeze snapshot"
```

### Task 2: Structural and drift validator

**Files:**
- Create: `scripts/check-wallet-writer-freeze.py`
- Modify: `tests/wallet-writer-freeze.test.mjs`

**Interfaces:**
- Consumes: `--manifest PATH` optional CLI argument, defaulting to the canonical audit manifest.
- Produces: exit `0` plus `STRUCTURE PASS` for a valid snapshot; exit `1` with prefix `wallet writer-freeze check failed:` for structural errors.
- Internal functions: `load_yaml(path: Path) -> dict`, `extract_matrix_ids(text: str) -> set[str]`, `validate_structure(data: dict, manifest_path: Path) -> list[str]`, `readiness_blockers(data: dict) -> list[str]`.

- [ ] **Step 1: Add structural mutation tests before implementation**

Add temporary-file tests that call `--manifest` and assert exit `1` for:

```js
test('rejects capability coverage drift', () => {
  const altered = canonical.replace(/\n- capability_id: GOLD-CAP-011[\s\S]*?(?=\n- capability_id:|\nfreeze_records:)/, '\n');
  const result = runManifest(altered);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /capability catalog does not exactly cover matrix IDs/);
});
```

Also add cases for duplicate `(backend_id, capability_id)` and a wrong matrix
SHA-256.

- [ ] **Step 2: Run tests and verify RED**

Run: `node --test tests/wallet-writer-freeze.test.mjs`

Expected: FAIL because the validator and structural checks are not implemented.

- [ ] **Step 3: Implement minimal structural validation**

Implement argument parsing and fail-closed checks for:

- schema and required mappings/lists;
- exact matrix hash and exact ID coverage extracted only from Markdown table
  rows matching `^| GOLD-(WRITER|CAP)-NNN |`;
- exact wallet contract ID/version;
- exact backend set;
- plugin name/version against `docs/plugin_manifest.yaml`;
- valid vocabularies and non-empty routes;
- complete unique backend/capability tuple coverage;
- local present evidence paths resolved under the repository root;
- SHA-256 formatting for non-null hashes.

Use:

```python
def fail(message: str) -> None:
    print(f"wallet writer-freeze check failed: {message}", file=sys.stderr)
    raise SystemExit(1)
```

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `node --test tests/wallet-writer-freeze.test.mjs`

Expected: structural cases PASS; the canonical manifest reports
`STRUCTURE PASS` and `READINESS BLOCKED`.

- [ ] **Step 5: Refactor without behavior changes**

Extract repeated mapping/list/string checks into focused helpers, keep all
external behavior unchanged, and rerun the focused test.

- [ ] **Step 6: Commit structural validator**

```bash
git add scripts/check-wallet-writer-freeze.py tests/wallet-writer-freeze.test.mjs
git commit -m "Validate wallet writer-freeze structure"
```

### Task 3: Strict readiness gate and synthetic ready proof

**Files:**
- Modify: `scripts/check-wallet-writer-freeze.py`
- Modify: `tests/wallet-writer-freeze.test.mjs`

**Interfaces:**
- Consumes: `--require-ready` flag.
- Produces: default exit `0` with stable blocker codes; strict exit `1` while blocked; strict exit `0` only for a fully coherent ready manifest.

- [ ] **Step 1: Add failing strict-mode tests**

Add:

```js
test('strict mode rejects the current blocked snapshot', () => {
  const result = runValidator(['--require-ready']);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /READINESS BLOCKED/);
  assert.match(result.stderr, /MISSING_VENDOR_JAR_PROOF/);
  assert.match(result.stderr, /PROVIDER_ACK_NOT_VERIFIED/);
});
```

Create a test helper that uses Python/PyYAML to transform a temporary copy into
a synthetic ready document: use a non-placeholder 64-hex hash, mark all proof
classes `VERIFIED` with existing non-sensitive evidence paths, assign the
central provider v2/context-required to every backend, use one epoch, set legacy
snapshot proof, and set `readiness: READY`. Assert strict exit `0` and
`READINESS READY`.

Add a negative case proving `permission_denied` is rejected when
`permission_only_freeze_allowed: false`.

- [ ] **Step 2: Run strict tests and verify RED**

Run: `node --test tests/wallet-writer-freeze.test.mjs`

Expected: FAIL because readiness invariants and `--require-ready` do not exist.

- [ ] **Step 3: Implement readiness blocker calculation**

Return stable blocker codes for at least:

```text
FREEZE_RECORD_NOT_VERIFIED
MISSING_VENDOR_JAR_PROOF
MISSING_EFFECTIVE_PERMISSION_PROOF
MISSING_PROVIDER_IDENTITY_PROOF
MISSING_MUTATION_REJECTION_PROOF
PERMISSION_ONLY_FREEZE_FORBIDDEN
PROVIDER_ACK_NOT_VERIFIED
PROVIDER_IDENTITY_MISMATCH
PROVIDER_CONTRACT_VERSION_MISMATCH
PROVIDER_WRITE_MODE_MISMATCH
GENERIC_WRITE_FAILURE_UNPROVEN
CUTOVER_EPOCH_MISSING_OR_MISMATCHED
LEGACY_SNAPSHOT_PROOF_MISSING
DECLARED_READINESS_MISMATCH
```

Deduplicate and sort codes before output so CI results are deterministic.

- [ ] **Step 4: Implement two-mode exit behavior**

Default mode prints blocker count/codes and exits `0`. Strict mode prints the
same `READINESS BLOCKED` summary to standard error and exits `1`. A complete
coherent document prints `READINESS READY` and exits `0` in both modes.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run: `node --test tests/wallet-writer-freeze.test.mjs`

Expected: all focused tests PASS, including synthetic strict-ready and
permission-only rejection.

- [ ] **Step 6: Commit strict readiness gate**

```bash
git add scripts/check-wallet-writer-freeze.py tests/wallet-writer-freeze.test.mjs
git commit -m "Enforce wallet writer-freeze readiness"
```

### Task 4: Final verification

**Files:**
- Verify only: `docs/audits/2026-08-12/wallet-writer-freeze-manifest.yaml`
- Verify only: `scripts/check-wallet-writer-freeze.py`
- Verify only: `tests/wallet-writer-freeze.test.mjs`

**Interfaces:**
- Consumes: final validator commands.
- Produces: fresh structural, readiness, regression, and no-config-change evidence.

- [ ] **Step 1: Run fresh verification**

Run:

```bash
python3 scripts/check-wallet-writer-freeze.py
python3 scripts/check-wallet-writer-freeze.py --require-ready
node --test tests/wallet-writer-freeze.test.mjs
node --test tests/*.test.mjs
git diff --check
git status --short -- MCMMORPG/plugins/CMI MCMMORPG/plugins/Vault MCMMORPG/plugins/LuckPerms
```

Expected:

- default validator exits `0` with `STRUCTURE PASS` and `READINESS BLOCKED`;
- strict validator exits `1` with stable blockers because the real environment
  is intentionally not ready;
- focused and repository Node tests pass;
- no plugin configuration path appears modified by this task.

- [ ] **Step 2: Record the exact verification outputs in the handoff**

Report the default and strict exit codes separately. Do not describe the real
manifest as ready merely because structural validation and the synthetic ready
test pass.
