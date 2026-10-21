# Code Review

Use review mode for bugs, regressions, missing tests, and risk. Findings should
lead, ordered by severity, with file and line references where possible.

## Review Output

```md
Findings:

Open questions:

Verification:

Residual risk:
```

## Checklist

- [ ] The diff is limited to the requested scope.
- [ ] No unrelated refactor or formatting churn was introduced.
- [ ] Public API or contract changes are intentional and documented.
- [ ] Auth, permissions, billing, data, and security paths were reviewed when
      touched.
- [ ] Relevant tests were added or updated.
- [ ] Tests, lint, typecheck, build, or a reason for deferral are recorded.
- [ ] No secrets, PII, customer data, or sensitive logs were added.
- [ ] New dependencies are justified and reflected in lockfiles.
- [ ] Error handling is explicit and safe.
- [ ] Rollback or compatibility concerns are clear for risky changes.
