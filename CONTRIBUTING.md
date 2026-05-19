# Contributing to FlagBar

Thanks for your interest! Contributions of all kinds welcome.

## Project layout

```
flag-bar/                       The published Kotlin Multiplatform library.
  src/commonMain/
    Flag.kt                     Bool / Int / String / Enum / Variant flag types.
    FlagBar.kt                  Live resolver + rememberFlagBar.
    FlagOverrideDrawer.kt       Standalone Compose drawer (per-type editors).
    FlagBarSection.kt           debug-bar plugin.
    FlagSource.kt               FlagSource interface + Empty / static.
    OverrideStorage.kt          Pluggable persistence interface + InMemory.
    VariantHashing.kt           FNV-1a + bucket-by-weight assignment.
  src/commonTest/               Pure-logic tests.
  src/skikoTest/                Compose UI tests (run on Desktop + iOS).
sample/composeApp/              Shared sample app using flag-bar inside debug-bar.
sample/androidApp/              Android launcher.
sample/desktopApp/              Desktop launcher.
sample/webApp/                  Web (wasmJs) launcher.
sample/iosApp/                  iOS launcher (Xcode project).
```

## Build & test

```bash
./gradlew build                          # build + test everything
./gradlew :flag-bar:desktopTest          # fastest feedback
./gradlew :sample:desktopApp:run         # run the sample
```

## Design invariants — please preserve

- **Vendor-neutral.** `FlagSource` is `fun interface { suspend fun fetch(): Map<String, Any> }`.
  Don't add a hard dep on LaunchDarkly / Firebase / ConfigCat. Users wrap.
- **Local-first.** The library works offline by default. Remote sync is opt-in.
- **Type-safe declarations.** Stringly-typed flag keys (`flags.get("my_flag")`) are out of scope.
  Stick to the typed `Flag<T>` sealed hierarchy.
- **Prefix-stable variant assignment.** Same user + same flag key → same variant on every device.
  Don't change the hash algorithm without a deprecation cycle.
- **No platform reach-arounds.** All `commonMain`. Storage adapters live in user code.

## Releasing

Tag-driven via `publish.yml`, or local via
`./gradlew :flag-bar:publishAndReleaseToMavenCentral`.
