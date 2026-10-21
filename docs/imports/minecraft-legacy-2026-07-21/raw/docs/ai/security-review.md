# Security Review

Use this checklist for T3 security, data, auth, billing, infra, dependency, or
production-adjacent changes.

## Evidence

```md
Scope:
Data classification:
Auth/authz boundary:
Inputs validated:
Sensitive outputs/logs reviewed:
Dependency impact:
Commands run:
Residual risk:
Human approval needed:
```

## Checklist

- [ ] Data was classified as GREEN, YELLOW, or RED.
- [ ] No secrets, tokens, passwords, cookies, private keys, PII, PHI, or payment
      data were added to code, docs, logs, tests, screenshots, or prompts.
- [ ] Untrusted input is validated at boundaries.
- [ ] Output is encoded for the target context.
- [ ] Database access uses parameterized queries or safe ORM APIs.
- [ ] Authentication and authorization are enforced server-side.
- [ ] Errors fail safely and do not leak stack traces or sensitive fields.
- [ ] Logs use correlation IDs or safe summaries, not sensitive payloads.
- [ ] Dependencies are necessary, maintained, license-compatible, and checked.
- [ ] Migrations, deploys, IAM, publishing, and destructive actions have explicit
      human approval and a rollback path.
