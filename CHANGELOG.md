# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-05-19

### Added
- Initial release of `FlagBar` for Compose Multiplatform.
- `Flag` type system — sealed class with `BoolFlag`, `IntFlag`, `StringFlag`, `EnumFlag<E>`, and
  `VariantFlag` subclasses. Type-safe declarations with `default` + key-based parsing.
- `FlagBar` + `rememberFlagBar` — live resolver. Holds registered flags, local override map,
  remote cache, and the current `userId` (for variant assignment).
- Resolution chain: **override → remote → default** for value flags;
  **override → hash(userId+key) → default** for variant flags.
- Deterministic variant assignment via FNV-1a hashing of `userId + flagKey`. Same user always
  gets the same variant on every device. Configurable weights honoured.
- `FlagOverrideDrawer` — standalone Compose drawer with per-type editors (switch / numeric
  field / text field / dropdown).
- `FlagBarSection` — `debug-bar` plugin so flags get a tab inside the debug drawer with an
  override-count badge.
- `FlagSource` — `fun interface { suspend fun fetch(): Map<String, Any> }`. Built-ins: `Empty`,
  `static(...)`. Wrap any vendor SDK (LaunchDarkly, ConfigCat, Firebase Remote Config) yourself.
- `OverrideStorage` — pluggable persistence interface. `InMemory` default; wrap
  `multiplatform-settings` for cross-platform persistent overrides.
- `FlagBar.snapshot()` — `key=value (source)` list for `debug-bar`'s
  `ScreenshotBundleSection` and the drawer's display.
- `FlagBar.collectFlagValue(flag)` — Compose `@Composable` extension that recomposes when
  overrides or the remote cache change.
- 18+ pure-logic tests (flag parsing, variant hashing fairness, FNV-1a determinism).
- Targets: Android (minSdk 24), iOS (x64, arm64, simulatorArm64), Desktop (JVM 11), Web (wasmJs).
- Transitively depends on `io.github.nadeemiqbal:debug-bar:0.1.0` for the `FlagBarSection`
  integration — drop `flag-bar` into your build and `DebugBarSection` is on the classpath too.

[Unreleased]: https://github.com/NadeemIqbal/flag-bar/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/NadeemIqbal/flag-bar/releases/tag/v0.1.0
