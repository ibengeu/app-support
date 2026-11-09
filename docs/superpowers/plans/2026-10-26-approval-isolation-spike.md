# Approval and isolation POC spike

## Objective

Demonstrate that a controlled local submission requires approval of its exact payload, cannot execute twice through approval replay, and stays blocked after an uncertain outcome. Establish what process/container isolation the current host can demonstrate before enabling computer tools.

## Scope

Add an authenticated local-inbox demo, immutable approvals, a durable execution ledger, safe receipt reconciliation, and native Android review controls. The fixture is a separate private Docker service with its own credential and durable receipts. Research remains read-only. Add a disposable credential-free isolation probe using the existing image. No external email, websites, purchases, arbitrary URLs, or shell tools become available to the agent.

A process-container probe is not evidence of Chromium sandboxing, Android desktop viewing, or exclusive real input takeover. Record those gates separately and do not claim them complete without actual evidence.

## Assumptions

One API/worker process and one active task remain the POC boundary. Approvals expire after ten minutes. Messages are limited to 2,000 characters. The destination and adapter are fixed in code. Revisions create a new approval version. Approval and dispatch use the active owner session. An explicit demo option may simulate losing a response after the inbox commits; it is part of the approved payload. Unknown outcomes cannot be retried or revised into a new execution; receipt lookup is read-only.

## TDD Plan

The user requested no new tests for this POC. Add no test suite. Use manual public API and Android flows against the controlled inbox to observe blocked pre-approval execution, approval replay, rejection, stale hashes/revisions, cancellation, ownership denial, and lost-response reconciliation. Run existing backend checks only as regression verification.

## Security Review

- A01 Broken Access Control / CWE-862/863/639: owner-bound task/approval/execution queries, active-session rechecks, immutable exact-payload hashes, atomic one-use consumption, cancellation/expiry/state checks, and no generic resume bypass. Manually inspect denial and replay outcomes through HTTP.
- A02 Security Misconfiguration / CWE-200: loopback API, private unexposed fixture, dedicated fixture credential, no provider credential in fixture or probe, no redirects, safe errors. Inspect Docker configuration without printing secrets.
- A03 Software Supply Chain Failures / A10 Vulnerable and Outdated Components: reuse the existing Python image and standard library; add no Python/Android dependency. Cached image inspection does not establish image provenance or a full vulnerability review.
- A04 Insecure Design / CWE-770: body/message limits, task quotas, fixed bounded five-second transport, durable dispatch reservation, no automatic retry after dispatch, receipt-only reconciliation. Interruptions become UNKNOWN. One active demo blocks ordinary work until resolved/cancelled.
- A05 Cryptographic Failures: use a random managed-environment fixture token and SHA-256 for payload integrity binding. SHA-256 is not used for password hashing or encryption. Compare credentials/digests with constant-time comparison.
- A06 Identification and Authentication Failures: authenticate every approval endpoint and recheck session validity in the dispatch transaction. Existing session expiry/revocation remains authoritative.
- A07 Injection / CWE-20/89: strict typed/size-limited JSON, parameterized SQL, fixed adapter/destination, plain Compose text, encoded identifiers. No shell or dynamic executable model input exists.
- A08 Security Logging and Monitoring Failures: bounded decision/execution/recovery events record identifiers and outcomes, not tokens or arbitrary messages; no HTTP body logging. Approval payload is owner-private application data, not a log.
- A09 Server-Side Request Forgery / CWE-918: only one constant private fixture origin can receive a dispatch/receipt lookup; caller URLs are rejected, proxy inheritance and redirects are disabled. The fixture has no general outbound-fetch capability. The process probe has network mode none.

Other CWE groups: no uploads, caller paths, executable deserialization, or active HTML previews are added. Kotlin/Python memory-safe APIs and input limits address null/data/resource risks; there is no new native memory manipulation. Bearer-authenticated native API calls introduce no browser-cookie CSRF contract. Existing report links are unchanged. MITRE Potential Mitigations for CWE-862/863/639 select server-side business/record authorization; CWE-770 selects explicit limits; previously reviewed CWE-20/200 guidance selects a strict parsing boundary and safe compartmentalization. NIST SSDF PW.1/PW.2 document boundaries, PW.5 uses safe APIs, and PW.7/PW.8 cover code review and public-boundary manual validation.

Sources: https://cwe.mitre.org/data/definitions/862.html ; https://cwe.mitre.org/data/definitions/863.html ; https://cwe.mitre.org/data/definitions/639.html ; https://cwe.mitre.org/data/definitions/770.html ; https://cwe.mitre.org/data/definitions/918.html

## Implementation Notes

- `backend/src/agentos/approvals.py`: schema, immutable intents, authorization, execution ledger, fixed fixture transport, recovery and receipt reconciliation.
- `backend/src/agentos/approval_fixture.py`: private authenticated inbox with idempotent durable receipts and an explicit lost-response simulation.
- `backend/src/agentos/server.py`: owner-authenticated demo/approval routes and current approval in task details.
- `backend/src/agentos/worker.py`: hold queued research while an approval/uncertain execution owns the active slot.
- `compose.yaml`: private fixture network/service using the existing image; dedicated non-provider credential; optional networkless isolation profile.
- Android API/state/task detail files: exact message/destination/expiry review; approve/reject/edit; check receipt for uncertainty. A clearly labelled local-only demo is available from Tasks.

## Validation

Compile Python, run the existing backend suite, enforce at most ten branches in new Python functions, validate Compose without displaying environment values, rebuild using cached layers, manually exercise the HTTP contracts and native app, and run the disposable isolation diagnostic. Assemble/lint/install Android and verify the native approval panel. Record what actual browser/takeover work remains rather than substituting process isolation evidence.

## Completion Criteria

The local inbox receives no unapproved submission. Approving a matching live intent sends once; replay does not send again. Old, rejected, cancelled, expired, or edited approvals cannot dispatch. An accepted-but-lost response becomes UNKNOWN and receipt reconciliation resolves it without another submission. Private data remains owner-bound. The demo is usable from Android. Isolation evidence states its exact boundary and unresolved desktop gates.

## Verified delivery

- Existing backend suite: 46 passing checks; no new test suite written. Python compilation passed. AST branching counts for the new modules and approval route helpers are at most nine.
- Android `:app:assembleDebug :app:lintDebug` passed and the updated APK is installed on the existing emulator. Non-blocking existing lint/deprecation warnings remain.
- Manual API/fixture checks: pending produces zero submissions; wrong hash and superseded/rejected/cancelled approval blocked; a valid approval and replay produce one submission; concurrent decisions return HTTP 200/409 with one execution; another authenticated owner's approval/task reads and decisions return 404; anonymous access returns 401.
- A committed submission with a deliberately lost response became UNKNOWN / RECOVERY_REQUIRED. Reapproval was denied. Receipt reconciliation completed it without another submission. Final deployed denial-audit recording was verified. Pending/expired state and receipts survived container recreation.
- Android: local demo creation and exact message/destination/simulation/expiry review verified. Actual session and approval expiry blocked a stale action. After reauthentication, Edit message created a fresh approval version; approval showed delivery unconfirmed; Check receipt confirmed the original submission. The emulator is left showing confirmed delivery.
- Docker API and fixture are healthy. The fixture has no published host port. All twelve process-isolation checks passed on Docker Desktop / Linux 6.11.11-linuxkit / aarch64. User-namespace creation failed with EPERM. Real Chromium sandbox, desktop viewing, and actual exclusive-input takeover remain unproven; see ADR 0002.

This completes the controlled local approval increment and records the process-isolation feasibility finding. It does not complete the full PRD's approval, computer or takeover requirements. The next gate is an actual sandboxed Chromium worker and exclusive-control proof on a compatible restricted Linux runtime.
