# ADR 0002: Computer isolation feasibility on the current Docker host

Status: process isolation measured; browser sandbox and actual takeover are unresolved gates.

## Observed result

The optional `isolation-probe` profile runs the existing Python image with UID/GID 10001, dropped capabilities, no-new-privileges, a read-only root filesystem, bounded tmpfs workspace, PID/memory limits, no host mounts or Docker socket, no credentials, and `network_mode: none`.

Runtime recorded: Docker Desktop, Linux kernel `6.11.11-linuxkit`, architecture `aarch64`.

The diagnostic confirmed all twelve configured process/container properties: non-root; zero effective capabilities; no new privileges; read-only application files; a writable private workspace; no Docker socket; no provider/fixture credential; and no route to the API, fixture, host API or public network.

A child process then attempted an unprivileged user namespace. It failed with EPERM (errno 1). This establishes that user namespaces are unavailable under this exact runtime configuration. It does not identify whether the cause is seccomp, a kernel setting, or another host policy. The cached tools image inspected in this session contains no Chromium executable. No actual Chromium sandbox, desktop stream, Android input mapping, or exclusive takeover was proven.

## Decision

Keep computer tools disabled. Do not use `--no-sandbox`, `--privileged`, host networking, the Docker socket, or a blanket unconfined profile to get past this gate. Preserve the measured conservative profile as a diagnostic baseline.

The networkless process profile cannot perform public browsing. A real browser requires a separate narrowly controlled egress design, not simply attaching this container to the API network.

## Next gate

1. Select/configure a Linux worker runtime that supports the required sandbox namespace operations under an audited restrictive policy. Launch actual Chromium non-root with its sandbox enabled and record evidence.
2. Keep browser control credentials, provider keys, API/DB networks, and management endpoints outside the untrusted browser's reach. Use a controlled egress boundary that handles resolved addresses and redirects; prove private/metadata/control-plane destinations are unreachable.
3. Show the actual browser in an authenticated Android viewer. Measure touch, keyboard/IME, orientation, connection and reconnect behavior.
4. Route all agent and user input through one exclusive authority. Takeover must drain or fence in-flight actions, invalidate queued input and increment the control generation before acknowledging control. Lost manual connections must leave automation paused. Return to automation requires explicit reinspection.
5. Demonstrate stale-input rejection and a slow in-flight action on the real browser. A flag-only input simulator is insufficient evidence.

These are implementation prerequisites for FR-04/06/07. The process probe is useful baseline evidence and does not complete those requirements.

## Reproduce the measured baseline

```sh
docker compose build agentos-api
docker compose --profile isolation run --rm --no-deps isolation-probe
```

The JSON separates process checks from browser namespace feasibility. The command succeeds when the process controls hold; inspect `browser_user_namespace` independently before drawing any browser conclusion.
