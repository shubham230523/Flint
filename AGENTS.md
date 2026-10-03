# Flint AI Engineering Rules

## Project

Flint is a Kotlin Multiplatform + Compose Multiplatform
AI Content Operating System.

Current major feature:

YouTube → Instagram Reels

Stage 1:
YouTube → Instagram content intelligence

Stage 2:
30-minute YouTube video → actual Instagram Reel

Stage 1 is already complete.

## NON-NEGOTIABLE DEVELOPMENT RULE

Never declare a feature complete because:

- the code compiles
- the app launches
- one happy-path test passes

A feature is complete only when:

1. Unit tests pass.
2. Integration tests pass.
3. UI tests pass where applicable.
4. End-to-end tests pass.
5. Realistic test data succeeds.
6. Error scenarios are tested.
7. Retry scenarios are tested.
8. App restart scenarios are tested.
9. Existing Flint regression tests pass.
10. Production-like build succeeds.
11. No critical Logcat/runtime errors exist.
12. The feature has been manually/visually verified through the running app.

## TDD

For every new behavior:

RED:
Write a failing test.

GREEN:
Implement the minimum code required.

REFACTOR:
Improve implementation without breaking tests.

Then continue.

Never skip tests simply because implementation appears simple.

## BUILD VERIFICATION

After meaningful changes:

Run the smallest relevant tests first.

Then:

- compile
- unit tests
- integration tests
- UI tests
- end-to-end tests

Do not stop after compilation.

## DESKTOP VERIFICATION

Flint Desktop is currently the primary development target.

When a feature affects Desktop:

1. Build Desktop.
2. Launch Desktop.
3. Exercise the feature.
4. Inspect visible UI.
5. Inspect logs.
6. Verify expected state transitions.
7. Shut down/restart if required.
8. Repeat after fixes.

Use deterministic test data whenever external services would make
tests unreliable.

## REEL PIPELINE

The YouTube → Instagram Reel pipeline must work:

YouTube Source
→ Metadata
→ Transcript
→ Video Analysis
→ Candidate Detection
→ Candidate Review
→ Clip Extraction
→ 9:16 Reframing
→ Captions
→ Hook
→ CTA
→ Rendering
→ Validation
→ Firebase Storage
→ Content Library

Every transition must be testable.

## NO FALSE SUCCESS

Never say:

"Looks good."

unless verification actually happened.

Never assume:

- FFmpeg succeeded
- video exists
- output video is playable
- subtitles are synchronized
- timestamps are correct
- Firebase upload succeeded
- generated ContentAsset is correct
- UI actually displays the result

Verify each one.

## REAL MEDIA TESTING

Use deterministic fixture videos for automated tests.

Use at least one realistic 30-minute test video for final
end-to-end verification.

The test video must contain:

- talking head
- multiple topics
- scene changes
- pauses
- meaningful statements
- screen content where possible

## EXTERNAL AI

AI responses must be mockable.

Unit tests must never depend on a live AI provider.

At least one integration test may use the configured local/open model
when the environment supports it.

## FAILURE TESTING

Explicitly test:

- malformed YouTube URL
- missing transcript
- network failure
- media download failure
- corrupt video
- FFmpeg failure
- AI timeout
- AI malformed JSON
- missing candidate
- invalid timestamp
- rendering failure
- upload failure
- Firebase failure
- insufficient disk space
- insufficient model resources
- application restart during processing

## RESUME

Long-running media jobs must be resumable.

Do not repeat expensive stages unnecessarily.

Example:

If:

Transcript = complete
Scene detection = complete
Candidate analysis = complete

and rendering fails,

DO NOT rerun transcription.

Resume from rendering.

## PERFORMANCE

Never optimize based on guesses.

Measure:

- processing time
- RAM
- GPU/VRAM
- disk
- model inference time
- FFmpeg time
- upload time

## SECURITY

Never expose:

- Firebase admin credentials
- private AI credentials
- model server credentials
- worker credentials

to the client.

Always enforce ownership server-side.

## CODE QUALITY

Do not:

- create duplicate repositories
- create duplicate ContentAsset models
- bypass existing AI Gateway
- bypass existing Firebase abstractions
- put business logic in Compose UI
- put heavy media processing inside the KMP client

## COMPLETION RULE

Do not mark a milestone COMPLETE until the
Definition of Done for that milestone has been verified.

If verification fails:

DO NOT move on.

Diagnose → Fix → Re-test → Repeat.

## FINAL RULE

When uncertain whether something works:

TEST IT.

Do not assume.