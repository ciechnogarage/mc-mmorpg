# Dependency Upgrade Playbook

Use for dependency changes.

1. Identify the package manager and lockfile.
2. Check whether an existing dependency or standard library API already solves
   the need.
3. Review release notes, breaking changes, license, and known vulnerabilities.
4. Keep the upgrade as narrow as possible.
5. Update manifest and lockfile consistently.
6. Run targeted tests plus typecheck/build when available.
7. Record compatibility risk and rollback notes.

Major upgrades are T3 and need plan-first review plus explicit human approval
for risky changes.
