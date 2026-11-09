# Bounded Research Reflection Implementation Plan

> **For agentic workers:** Execute this plan task-by-task in this session using small red–green–refactor cycles. The user has authorized implementation.

**Goal:** Publish research reports only after a source-aware review, allowing one targeted revision and final review.

**Architecture:** Keep the existing SQLite worker and HTTP contracts. Separate provider research/review results from orchestration; record bounded stage and review summaries in existing owner-protected task events. Keep temporary drafts in memory and publish only an accepted report.

**Tech Stack:** Python standard library, SQLite, pytest, OpenAI Responses API.

## Objective

Improve report coverage and factual support while exposing clear failure outcomes and respecting task controls.

## Scope

Included: draft, structured review, one revision, final review, source metadata, usage summaries, per-task limits, safe failures, cancellation/pause checks, documentation, and deterministic public-boundary tests. Excluded: persistent cross-task memory, new frameworks, external writes, Android UI redesign, automatic restart recovery, and live paid evaluation without an available credential.

## Assumptions

- Continue using the configured model and read-only web search.
- A report that still fails its final review fails with `QUALITY_GATE_FAILED`; no unapproved draft becomes an artifact.
- Resume restarts research. In-flight provider requests cannot be instantly cancelled; discard their output and make no subsequent request after observing a stopped task.
- Provider usage is measured; enforce four requests, three built-in tool calls per request, 4,000 output tokens per request, bounded request bytes, and a 300-second elapsed deadline. The network timeout is at most 90 seconds and the remaining deadline.
- No Git metadata exists in this workspace, so worktree creation and commits do not apply.

## TDD Plan

Review `backend/tests/test_tasks_api.py` first (baseline: three passing API tests). Add one outcome at a time in `backend/tests/test_research_worker.py`, using real task HTTP operations, the real worker/runtime, and a scripted provider HTTP boundary. Never call private worker helpers.

- [x] Accepted draft produces exactly one downloadable report and a review event.
- [x] Rejected draft is corrected and accepted by the final review.
- [x] Repeated rejection fails with no artifact and stops at four requests.
- [x] Invalid review, refusal, missing evidence, and malformed provider output fail safely.
- [x] Pause, cancel, and pause/resume during a stage prevent stale work from continuing or publishing.
- [x] Elapsed, request-size, and usage limits fail safely.
- [x] Provider requests retain only read-only tools; injected content cannot grant capabilities.
- [x] Review events and files are unavailable to another owner and unauthenticated callers.

For every item: add one public outcome test, run it and confirm its expected failure, implement the minimum change, run the test and suite, then refactor while green.

## Security Review

Relevant mitigations selected from MITRE entries:

- A01 Broken Access Control / CWE-862: use existing owner-scoped event/file endpoints; test another owner cannot retrieve review data.
- A02 Security Misconfiguration / CWE-200: preserve safe provider errors, fixed HTTPS origin, no redirects, and `store: false`; never persist provider bodies or credentials in events.
- A04 Insecure Design / CWE-770: cap calls, built-in tools, input bytes, output tokens, response bytes and elapsed time; check stopped tasks between stages and before publication.
- A07 Injection / CWE-1427 (beyond the Top 25): keep source content, drafts and feedback in user data rather than trusted instructions; fixed tool allow-list in code; test attempted capability expansion. Prompts alone cannot guarantee factual integrity.
- A08 Security Logging and Monitoring Failures / CWE-200: persist stage, decision, issue category/count, timing, and usage, but no arbitrary provider prose or full draft in events; safe-error tests.
- CWE-20: validate all nested provider data, review enums/list bounds, citation URLs, and usage counts at the provider boundary.

Other OWASP categories:

- A03 Software Supply Chain Failures does not introduce a new dependency or build pipeline; keep standard-library runtime and no package additions.
- A05 Cryptographic Failures is not applicable: no changes to passwords, transport, keys, randomness or encryption.
- A06 Identification and Authentication Failures is not applicable: no identity/session changes; existing authentication protects events.
- A09 Server-Side Request Forgery creates no new server-side URL fetch: requests use one constant provider URL; web results never become backend fetch destinations.
- A10 Vulnerable and Outdated Components introduces no runtime/dependency version change; dependency scanning has no new package target.

Other CWE groups: SQL uses existing parameterized writes; there is no shell/code execution or unsafe deserialization. No new upload or user-controlled path is introduced. Python memory safety applies; explicit type and size checks address malformed data and resource bounds. Markdown remains a download, with no new HTML rendering or browser-cookie contract, so CWE-79/CWE-352 are not introduced.

NIST SSDF alignment: PW.1/PW.2 document trust boundaries and review design; PW.5 uses bounded safe interfaces; PW.7/PW.8 review code and test public security outcomes. This is a feature review, not a certification or full repository security audit.

Sources: https://cwe.mitre.org/top25/ ; https://cwe.mitre.org/data/definitions/770.html ; https://cwe.mitre.org/data/definitions/1427.html ; https://cwe.mitre.org/data/definitions/200.html ; https://cwe.mitre.org/data/definitions/20.html ; https://cwe.mitre.org/data/definitions/862.html

## Implementation Notes

- `backend/src/agentos/research.py`: provider transport, typed draft/review results, evidence and schema validation, bounded request parameters.
- `backend/src/agentos/worker.py`: existing durable task/artifact lifecycle plus stage orchestration and events. Use event sequence as an execution fence to detect pause/resume races.
- `backend/tests/test_research_worker.py`: deterministic HTTP task/file outcomes and provider-boundary scenarios.
- `backend/tests/test_tasks_api.py`: preserve existing tests; permit raw Markdown responses in the shared HTTP test helper.
- `README.md`, `backend/README.md`: reflection behavior, limits, cost and cancellation semantics.
- `docs/reflection-evaluation.md`: repeatable quality evaluation task set and equal-budget comparison method, without claiming measured gains.

## Validation

Run `python3 -m pytest backend/tests -q` after each cycle. Run Python compilation and cyclomatic-complexity analysis of changed functions; keep complexity at most ten. No Android build is needed because native source/contracts remain unchanged. No dependency scan applies to added dependencies because none are added. Provider calls are simulated in tests; report any missing live smoke test clearly.

## Completion Criteria

Only reviewed reports are downloadable. Unresolved quality failures produce a safe error with no artifact. One revision is the maximum. Stopped/stale work cannot advance or publish. Limits and privacy boundaries have public tests. The full backend suite passes and operational/evaluation documentation is current.

## Delivery note

The user clarified that this is a POC and further tests are unnecessary. Stop adding tests. The implementation and existing verification completed before that clarification cover the planned deterministic outcomes. No live credential was available, so paid provider verification and quality benchmarking remain unmeasured rather than claimed as passing.
