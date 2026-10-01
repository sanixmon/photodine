# 01: Project Scaffold

**What to build:** A compilable, green-CI Android project with all eight Gradle modules wired together and empty but correct module boundaries. A developer can clone the repo, run the build, and see all modules compile with zero errors. The repo is immediately contribution-ready: license, README, CONTRIBUTING.md, and ARCHITECTURE.md are in place.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Gradle multi-module project initialised with Version Catalog (`libs.versions.toml`): `:app`, `:core:engine`, `:core:ui`, `:feature:canvas`, `:feature:layers`, `:feature:tools`, `:feature:colorpicker`, `:feature:export`
- [ ] Each module has the correct `build.gradle.kts` (Android library except `:app` which is `com.android.application`), `minSdk 29`, `compileSdk` latest stable
- [ ] Kotlin + Jetpack Compose enabled in all feature/app modules; `:core:engine` is Android library with no Compose dependency
- [ ] Hilt (or Koin — choose one, document in ARCHITECTURE.md) wired for DI across modules
- [ ] `:app` hosts a single placeholder `MainActivity` with a Compose `NavHost` stub that compiles and launches on device/emulator
- [ ] GitHub Actions CI workflow: runs `./gradlew lint assembleDebug` on every push/PR; must pass green
- [ ] `LICENSE` (MIT), `README.md` (project description, build instructions, module map), `CONTRIBUTING.md` (PR guide, code style), `ARCHITECTURE.md` (module responsibilities, MVI pattern, GLES overview) committed at root
- [ ] `.editorconfig` and `detekt` or `ktlint` baseline configured and passing in CI
