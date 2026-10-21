# Bugfix Root Cause Playbook

Use for bugs where behavior can be reproduced.

1. Normalize the task into Goal, Context, Constraints, and Done when.
2. Reproduce the bug with the smallest command, test, log, or UI path.
3. Add a failing test or minimal repro when practical.
4. Confirm the failure is for the expected reason.
5. Implement the smallest fix.
6. Run the targeted test, then nearby regression checks.
7. Review the diff for scope, security, and accidental behavior changes.
8. Report root cause, files changed, verification, and residual risk.
