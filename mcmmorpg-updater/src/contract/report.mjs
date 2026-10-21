// Auditable approval / authorization report.
//
// The report is the durable record of a decision. For an audit it must state, at
// minimum, the operator, the time, the plan id and the plan hash — plus enough
// to reconstruct which targets/operations were authorized and, on rejection, why.

/**
 * @param {object} params
 * @param {object} params.plan       validated plan (may be partial on hard failures)
 * @param {object|null} params.approval approval record, if one was presented
 * @param {'authorized'|'rejected'} params.decision
 * @param {string|null} [params.reasonCode]  machine code on rejection
 * @param {string|null} [params.reason]       human reason on rejection
 * @param {number} [params.now]      decision timestamp (epoch ms)
 */
export function buildApprovalReport({ plan, approval, decision, reasonCode = null, reason = null, now = Date.now() }) {
  const content = plan && typeof plan.content === 'object' ? plan.content : null;
  return {
    decision,
    reasonCode,
    reason,
    // Core auditable fields required by the acceptance criteria:
    operator: approval?.operator ?? content?.operator ?? null,
    decidedAt: new Date(now).toISOString(),
    planId: plan?.id ?? null,
    planHash: plan?.hash ?? null,
    // Supporting context:
    schemaVersion: plan?.schemaVersion ?? null,
    planState: plan?.state ?? null,
    nonce: approval?.nonce ?? null,
    approvalTtlSeconds: approval?.ttlSeconds ?? null,
    approvalIssuedAt: Number.isInteger(approval?.issuedAt) ? new Date(approval.issuedAt).toISOString() : null,
    targets: Array.isArray(content?.targets) ? content.targets.map((t) => t.target) : [],
    operations: Array.isArray(content?.operations)
      ? content.operations.map((o) => ({ op: o.op, target: o.target }))
      : [],
  };
}
