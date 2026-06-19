# Flaky Test Debug Playbook

Use when tests fail intermittently.

1. Capture the exact command, environment, and failure output.
2. Run the smallest relevant test repeatedly if runtime allows.
3. Look for timing, ordering, randomness, shared state, network, file-system,
   timezone, and resource leaks.
4. Prefer deterministic waits, isolated state, seeded randomness, and explicit
   cleanup over broad retries.
5. Add regression coverage for the identified cause.
6. Report reproduction rate before and after the fix.
