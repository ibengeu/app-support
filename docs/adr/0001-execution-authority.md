# ADR 0001: Exact-payload approval for a controlled local adapter

Status: implemented POC boundary; general execution authority remains a release gate.

## Decision

Only the fixed `fixture.submit` adapter can execute through this approval spike. Its destination is the private local demo inbox. The research model still has only read-only web search and cannot propose, approve, or invoke this adapter itself.

The owner prepares a task, reviews the exact message, destination and simulation mode, and approves its immutable intent. The canonical SHA-256 digest covers owner, task, version, tool/adapter, destination, payload and expiry. Editing creates a new intent and supersedes the previous one. Approvals expire after ten minutes. Owner/session/task checks and one-use consumption happen under a SQLite write transaction.

Consumption creates one uniquely keyed DISPATCHING execution before network dispatch. A successful duplicate approval returns the existing result. A concurrent or unconfirmed execution cannot send again. The private receiver also has a unique execution key and rejects mismatched payload reuse. These properties establish the controlled adapter's behavior; arbitrary websites do not acquire exactly-once semantics.

The gateway checks cancellation and session validity again before dispatch. Cancellation or revocation after dispatch begins cannot recall a remote commit. A late receipt cannot change a CANCELLED task back to COMPLETED.

Lost responses and interrupted dispatches become UNKNOWN / RECOVERY_REQUIRED. Startup never requeues them. Explicit receipt lookup can confirm the original execution without another submission. A missing receipt keeps the action blocked because a still-running request might commit later. Generic resume cannot bypass approval or recovery.

## Boundaries

The fixture has no published host port, receives no provider key, and uses a separate random credential. Dispatch and receipt URLs are fixed; redirects and inherited proxy configuration are disabled. Only plain bounded messages and an explicit local fault-simulation flag are supported. Payloads are owner-private application data. Events and bounded denial audit records omit message/token contents.

The API currently hosts both research and approval code in one trusted Python process. This is not the PRD's independent authority against a compromised arbitrary-code worker. Browser/shell tools remain unavailable. Before enabling them, move enforcement outside their trust boundary and prove there is no direct egress or input path around it.

## Evidence

Manual public HTTP checks against the private fixture observed zero submissions while pending; wrong hashes and superseded/rejected/cancelled approvals denied; one delivery under replay and concurrent approval; another owner's reads/decisions denied; and accepted-but-lost responses reconciled without another submission. Existing backend checks remain green. No new test suite was added, per the POC instruction.

The Android panel displays the fixed destination, full message, expiry, and explicit simulation mode. Its actions are Approve & submit, Reject request, Edit message, and Check receipt when delivery is unconfirmed. Actual session/approval expiry was also observed to block stale UI actions.

## Remaining production work

Separate enforcement from untrusted workers, integrate typed model action proposals, add supported real-site adapters with independently enforceable effects, enforce managed encrypted sensitive payload retention, and complete release-grade recovery, audit/export and approval UX acceptance. This demo does not satisfy full FR-10 acceptance.

Native verification: after renewing an expired session, the expired request was edited into a fresh version. The exact local payload was approved, the deliberately lost response showed Check receipt, and receipt lookup confirmed the original delivery. No extra submission was made by reconciliation.
