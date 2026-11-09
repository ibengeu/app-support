# Gemini Image Generation

## Objective

Let a signed-in Android user generate a 1K image with Gemini, preview it, and save it to the private workspace or device.

## Scope

Add an authenticated backend endpoint, persistent owner-scoped image files, a Gemini generation action in Files, preview, and device save. Use the current Nano Banana 2.1 Interactions API. Exclude image uploads/editing, multiple model and resolution choices, and automated image tasks.

## Assumptions

Gemini API image generation is billed and does not have a free API tier for the selected model. The server operator supplies `GEMINI_API_KEY`. Use 1K output by default. Keep Gemini credentials out of Android and response bodies. Limit each owner to 20 generation attempts in a rolling 24-hour period by default; allow the operator to raise or lower this server-side.

## TDD Plan

The user previously specified this POC does not need new tests. Do not add or run tests. Review existing public API behavior before changing the file contract. Implement one focused change, then use compile, Android build/lint, and Compose configuration checks where available.

## Security Review

- **A01:2025 Broken Access Control:** owner-scope generated images and all image file reads to the authenticated user. Relevant CWE-862/863/284/639: enforce authorization on each image endpoint and query.
- **A02:2025 Security Misconfiguration:** use safe provider errors, `no-store`, `nosniff`, and no redirects for provider calls.
- **A03:2025 Software Supply Chain Failures:** no new dependency; existing pinned application image remains in use.
- **A04:2025 Insecure Design:** restrict prompt size, provider timeout, response size, and rolling daily attempts to control resource use and unexpected charges. Relevant CWE-20/770: validate at the public boundary and cap work.
- **A05:2025 Cryptographic Failures:** keep the provider key in server environment configuration, use HTTPS, and never return or log the key.
- **A06:2025 Identification and Authentication Failures:** require the existing bearer session for generation and image access; do not add another credential scheme. Relevant CWE-306: authenticate generation requests.
- **A07:2025 Injection:** parameterize image metadata queries and treat the prompt only as JSON data. CWE-89 applies to SQL; use bound parameters.
- **A08:2025 Security Logging and Monitoring Failures:** avoid logging prompts, image bytes, bearer tokens, or provider responses.
- **A09:2025 Server-Side Request Forgery:** the server calls one fixed Google API host, with redirects disabled; the request has no user-selected destination.
- **A10:2025 Vulnerable and Outdated Components:** add no package. Use the existing runtime and container.
- CWE-79/352 do not apply to this native bearer-authenticated flow; there is no web page rendering prompt text or cookie-based state. CWE-78/77/94/502 do not apply; prompts are not executed, and no shell or untrusted deserialization is used. CWE-22/434 do not apply; no paths or uploaded files come from users. CWE-787/416/125/120/121/122/476 do not apply; this change uses Python and Kotlin managed APIs without manual memory operations.
- **NIST SSDF 1.1:** document trust boundaries, keep keys server-side, validate bounded inputs and outputs, review owner authorization and safe errors, and run build/configuration checks before delivery.

## Implementation Notes

Use the official Gemini Interactions REST endpoint and fixed Nano Banana 2.1 model. Store generated PNGs under the existing private artifact directory and return them through owner-checked Files APIs. Keep the API key and the image budget in Compose environment configuration. Show billing disclosure before submission.

## Validation

Run Python compilation, Android assemble/lint, and Docker Compose config validation. Do not run tests. Do not make a paid live generation request without a configured Gemini key and an explicit request to generate an image.

## Completion Criteria

The Android Files screen can submit a prompt, receive a generated image, show it, and save it through Android's document picker. The generated file remains private to its owner. Missing keys and provider failures produce safe, actionable messages. The API key never reaches the app.
