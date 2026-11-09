# Android task monitoring POC plan

## Objective

Let the owner inspect a task's research/review progress, understand failures, control active work, and open the accepted report directly from the task.

## Scope

Add a task detail screen reachable from Chat and Tasks using the existing visual design and existing task/file HTTP endpoints. Include current status, event timeline, review summaries, actionable failure copy, refresh, pause/resume/cancel, and report access. Exclude new backend contracts, streaming, memory, notifications, browser actions, and computer controls. This completes the next POC increment toward FR-11, not full PRD acceptance.

## Assumptions

Use five-second foreground polling. Preserve historical events after resume. Refresh the open task immediately after a task action. Display review categories and safe summaries only. Reuse the report viewer and system document picker. Build/install on the user's existing Android emulator.

## TDD Plan

The user explicitly requested no new tests for this POC. Add none. Validate by compiling, running Android lint, and manually exercising the public UI against the existing backend and task records.

## Security Review

- A01 Broken Access Control / CWE-862: all detail and report requests retain the existing bearer session and server owner checks. Artifact identifiers are path-encoded, never treated as arbitrary URLs. Validate owner-protected routes through the actual app.
- A02 Security Misconfiguration / CWE-200: preserve HTTPS/restricted-debug endpoints and no-redirect requests; display safe HTTP errors rather than raw network exception details.
- A04 Insecure Design / CWE-770: poll only the selected task while foregrounded, stop polling terminal tasks, prevent duplicate actions/downloads, and cancel work on navigation. Reuse bounded backend tasks and responses. Foreground lifecycle and refresh are manually checked.
- A07 Injection: render task, event and failure text as Compose text, never HTML or executable content. Keep report download on the configured API origin. No new model prompt or tool capability exists.
- A08 Security Logging and Monitoring Failures: no new credential/event logging. Show existing owner-protected summaries without model reasoning.
- A03 Software Supply Chain Failures and A10 Vulnerable and Outdated Components: no package or version changes; existing Gradle resolution and lint apply, no new dependency-scan target.
- A05 Cryptographic Failures: no key, password-hash, encryption, or randomness changes.
- A06 Identification and Authentication Failures: no session/authentication changes; preserve encrypted token storage and clear selected task/report on logout or endpoint change.
- A09 Server-Side Request Forgery: no new backend outbound URL fetch. Download uses a fixed API path and an encoded identifier.

Other CWE groups: no SQL, shell, code execution, uploads or filesystem path handling is added. Compose renders text rather than HTML (CWE-79); native bearer requests do not introduce a browser-cookie CSRF contract (CWE-352). Managed Kotlin avoids native memory-lifetime issues. Validate optional event fields and defaults at the JSON boundary (CWE-20). Previously reviewed MITRE guidance for CWE-20/200/770/862 applies unchanged. NIST SSDF PW.1/PW.2 cover this boundary review, PW.5 the typed client API, and PW.7/PW.8 compilation/lint/manual UI review.

## Implementation Notes

- `AgentApi.kt`: typed task details/events and owner-authenticated detail fetch.
- `TaskDetailScreen.kt`: lifecycle-aware polling, timeline, failure guidance, task actions and report access.
- `TaskDetailState.kt`: selected-task fetching, safe connection errors, action state and monotonic updates.
- `MainActivity.kt`: task navigation, direct file opening, and shared incumbent colors.
- Preserve the current five destinations and the existing list/report behavior.

## Validation

Compile `:app:assembleDebug`, run `:app:lintDebug`, install the APK, and inspect the incumbent and updated screens on `Medium_Phone_API_36.1`. Check a completed task, stage/review history, direct report open/back, and task navigation. Use one batched visual inspection and at most one correction pass. Do not claim unseen live states as manually verified.

## Completion Criteria

Tasks open a detail screen. Stage and review history is readable. Failures offer recovery guidance. Active task controls reflect server state. An accepted report opens directly. Build/lint results and any manual-verification gaps are reported honestly.

## Implementation review

New request/state functions are focused and keep branching small. There is no configured Kotlin cyclomatic-complexity analyzer. The pre-existing `AgentOSApp` composable retains a documented POC exception to the ten-branch guideline: it combines session routing and workspace callback wiring. A full state-holder/navigation refactor is deferred to avoid expanding this monitoring increment. The new task detail state is separated from rendering and status/event label tables avoid large conditional chains. Android compilation, lint and public UI inspection provide verification; no new tests were written, per the user's instruction.

## Verification result

- `:app:assembleDebug :app:lintDebug` passed on the final source with JVM 17 targets. Android lint reported zero errors and twelve non-blocking warnings in existing app metadata, dependencies and older API usage. No new tests were added.
- Installed `app-debug.apk` on `Medium_Phone_API_36.1`. The initial snapshot/software renderer stalled; a cold boot with host graphics preserved the AVD data and allowed installation.
- Manually verified owner sign-in against the live Docker backend, the Chat task entry, task-details navigation, completed status, accepted-review summary, ordered research/review events and timings, and direct report download/display with Save copy and Done actions visible.
- Fixed the observed inherited black text on dark backgrounds by providing the shared Material Surface content color.
- Active pause/resume/cancel, failure recovery, offline/reconnect behavior, and lifecycle polling have implementation review but were not all exercised as live emulator scenarios in this pass. No new paid research task was needed for the UI check. This is POC evidence, not full FR-11 or MVP acceptance.
