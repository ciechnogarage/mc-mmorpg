# Wallet writer-freeze manifest and validator design

Date: 2026-08-12

Status: approved design awaiting implementation

## Goal

Create a machine-readable pre-cutover snapshot of every gold mutation
capability recorded in
`docs/ai/cmi-vault-gold-writer-capability-matrix.md`, plus a deterministic
validator that separates document correctness from actual cutover readiness.

This change must not edit plugin configuration, start a backend, mutate a
balance, inspect player data, or claim runtime proof.

## Selected approach

Use a hand-maintained canonical YAML audit manifest and one dependency-light
Python validator. This follows existing repository patterns while avoiding a
brittle parser that would try to derive backend semantics from Markdown prose.

Rejected alternatives:

- Generating the manifest directly from Markdown would couple correctness to
  presentation formatting and ambiguous backend wording.
- JSON Schema plus a semantic checker would split the gate across two tools and
  add complexity without improving the current cutover decision.

## Files

- `docs/audits/2026-08-12/wallet-writer-freeze-manifest.yaml`: canonical audit
  snapshot.
- `scripts/check-wallet-writer-freeze.py`: structural and readiness validator.
- `tests/wallet-writer-freeze.test.mjs`: behavior tests for both modes and
  failure cases.
- `docs/ai/cmi-vault-gold-writer-capability-matrix.md`: remains the human source
  inventory and is not rewritten by this task.

## Manifest model

The top-level document uses `schema_version: 1` and contains:

- manifest identity and snapshot timestamp;
- source references and SHA-256 for the capability matrix;
- wallet contract ID and version;
- exact required backend set: `items`, `world`, `hub`;
- required central provider name, contract version, write mode, and generic
  write failure code;
- a capability catalog covering every `GOLD-WRITER-*` and `GOLD-CAP-*` ID in
  the matrix;
- one backend freeze record for every applicable `(backend_id,
  capability_id)` tuple;
- backend provider acknowledgements;
- global legacy snapshot and cutover-epoch evidence;
- an explicit `BLOCKED` readiness declaration for the initial snapshot.

Each capability catalog entry declares its classification, plugin, version,
applicable backends, mutation route, observed state, required cutover state, and
whether permission denial alone may count as a freeze. Console, API, scheduler,
lifecycle, and automatic reward writers must set this to false.

Each freeze record declares:

- `backend_id` and `capability_id`;
- exact plugin name and manifest version;
- expected mutation route;
- current evidence state;
- effective exposure (`pending`, `disabled`, `permission_denied`,
  `context_required`, `migrated_first_party`, or `not_gold_proven`);
- exact vendor JAR SHA-256 when proof is available;
- proof records for exact-JAR scan, effective permissions, provider identity,
  and focused mutation rejection;
- freeze verification status and evidence references.

Missing proof is represented as `MISSING` or `PENDING`, never as a blank or an
optimistic pass.

## Validator behavior

The default command is:

```text
python3 scripts/check-wallet-writer-freeze.py
```

It validates the manifest structure, source hashes, local references, contract
version, plugin versions, capability coverage, unique tuples, backend coverage,
status vocabulary, and internal consistency. A structurally valid but
incomplete snapshot exits `0` and prints a deterministic `BLOCKED` verdict with
the number and codes of readiness blockers.

The strict command is:

```text
python3 scripts/check-wallet-writer-freeze.py --require-ready
```

It performs every structural check and exits `1` unless all readiness
invariants pass. Cutover readiness requires:

1. every applicable capability has a verified freeze record;
2. each record has the required proof classes and valid evidence references;
3. exact JAR hashes are present for vendor capabilities;
4. permission-only is not accepted for writers that can run as console, API,
   scheduler, lifecycle hook, or automatic reward;
5. migrated first-party writers use contextual wallet mutations;
6. all required backends select exactly
   `MCMMORPGCentralWalletVaultProvider` contract version `2` in
   `context_required` mode;
7. generic Vault deposits and withdrawals are proven to fail with
   `WRITE_CONTEXT_REQUIRED`;
8. all backend acknowledgements use the same non-placeholder cutover epoch;
9. the legacy count/checksum and snapshot watermark evidence are present;
10. the manifest's declared readiness is `READY`.

Validation errors and readiness blockers are separate concepts. Invalid
structure always exits `1`, even without `--require-ready`.

## Drift rules

- The IDs extracted from Markdown table rows must exactly equal the capability
  catalog IDs.
- The stored matrix SHA-256 must match the current file.
- Plugin names and versions must match `docs/plugin_manifest.yaml`.
- Wallet contract ID/version must match
  `docs/contracts/central-account-wallet-contract.yaml`.
- Required backends must be exactly `items`, `world`, and `hub`.
- Every freeze tuple must be unique and reference an applicable backend.
- Every local evidence reference marked present must exist and remain under the
  repository root.

## Error handling and output

The validator prints one concise summary on standard output. Structural errors
use standard error, a stable `wallet writer-freeze check failed:` prefix, and
exit `1`. Readiness blockers have stable symbolic codes so CI and operators can
distinguish missing JAR proof, permission proof, provider proof, epoch mismatch,
and legacy snapshot proof.

The validator never reads environment files, credentials, databases, LuckPerms
user storage, player data, or worlds.

## Test strategy

Implementation follows red-green-refactor:

1. a test first expects the current manifest to pass structure and report
   `BLOCKED`;
2. a strict-mode test expects the same manifest to fail readiness;
3. temporary manifest variants prove rejection of a missing matrix ID, duplicate
   backend/capability tuple, and source-hash drift;
4. a synthetic complete variant proves the strict-ready success path without
   asserting that the real environment is ready;
5. the full relevant Node test suite and direct validator commands are run at
   completion.

No Docker or Paper runtime test is appropriate because this increment creates
only a static pre-cutover gate and records existing missing evidence.

## Rollback

Rollback is deletion of the three new implementation files and this design
document. No runtime state, economy balance, permission, plugin configuration,
or database record is changed.
