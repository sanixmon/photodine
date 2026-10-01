# Contributing to Photopia

## Getting started

1. Fork the repo and clone your fork.
2. Open the project in Android Studio (Koala or newer, JDK 17).
3. Run `./gradlew assembleDebug` — all eight modules must compile clean.

## Pull requests

- Keep PRs small and scoped to one ticket/issue.
- `feat(<ticket>): ...` commit style, e.g. `feat(02): engine compositor`.
- Every PR must keep `./gradlew lint assembleDebug detekt` green (CI enforces it).
- Respect module boundaries in ARCHITECTURE.md: features never depend on each
  other; UI state flows through per-feature MVI ViewModels.

## Code style

- Kotlin official style, 4 spaces, 120-column limit (see `.editorconfig`).
- detekt must report zero issues; update `config/detekt/baseline.xml` only with
  reviewer approval, never to silence new findings.
- Tests verify observable behavior, not internals. Feature tests go through MVI
  reducer state; engine tests assert output pixels, never shader source text.

## Reporting issues

Open an issue with repro steps, device/API level, and expected vs actual behavior.
