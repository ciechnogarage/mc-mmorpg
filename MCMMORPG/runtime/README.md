# Runtime

This directory is reserved for non-versioned runtime state for the new environment standard.

- Per-stack secret env files belong at `runtime/<stack>/secrets/runtime.env`.
- Mutable container data should stay in Docker volumes, not in `source/`.
- This ticket does not copy or move any existing legacy world, MariaDB, or runtime data into `runtime/`.
