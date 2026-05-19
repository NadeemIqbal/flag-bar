# FlagBar

**A vendor-neutral, local-first feature-flag library for Compose Multiplatform.** Type-safe
flag declarations (`BoolFlag` / `IntFlag` / `StringFlag` / `EnumFlag` / `VariantFlag`), deterministic
A/B variant assignment via hashing, optional HTTP remote source, and a built-in
`FlagOverrideDrawer` that lets QA toggle flags + variants at runtime. Persistent overrides via a
pluggable `OverrideStorage` interface (in-memory by default; wrap `multiplatform-settings` for
persistence). Pairs with [`debug-bar`](https://github.com/NadeemIqbal/debug-bar) — drop in
`FlagBarSection(flags)` for an integrated debug tab.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.nadeemiqbal/flag-bar)](https://central.sonatype.com/artifact/io.github.nadeemiqbal/flag-bar)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Build](https://github.com/NadeemIqbal/flag-bar/actions/workflows/build.yml/badge.svg)](https://github.com/NadeemIqbal/flag-bar/actions/workflows/build.yml)

## Why

Most CMP teams that want feature flags today have 3 bad choices:

| Existing | Problem |
|---|---|
| LaunchDarkly / ConfigCat / Unleash SDKs | Paid SaaS, vendor lock-in, often Android-only |
| Firebase Remote Config | Android+iOS only, no Desktop/Web KMP support |
| Hand-roll it | What most teams actually do |

The gap: a **vendor-neutral, local-first, OSS** library where flags live in Kotlin code, evaluate
offline, optionally sync from any HTTP endpoint, and ship with a built-in **debug-override drawer**.

## Install

```kotlin
dependencies {
    implementation("io.github.nadeemiqbal:flag-bar:0.1.0")
}
```

(`debug-bar` is pulled in transitively, so you don't need to add it separately.)

## Define flags

```kotlin
object Flags {
    val newCheckout = BoolFlag("new_checkout", default = false)
    val maxRetries = IntFlag("max_retries", default = 3)
    val apiBaseUrl = StringFlag("api_base_url", default = "https://api.example.com")
    val theme = EnumFlag("theme", default = Theme.System, options = Theme.entries)
    val checkoutVariant = VariantFlag(
        key = "checkout_variant",
        variants = listOf("control", "v1", "v2"),
        weights = listOf(0.5, 0.25, 0.25),   // 50% / 25% / 25%
        default = "control",
    )
}
```

## Use in your app

```kotlin
@Composable
fun App(userId: String) {
    val flags = rememberFlagBar(
        flags = listOf(
            Flags.newCheckout, Flags.maxRetries, Flags.apiBaseUrl,
            Flags.theme, Flags.checkoutVariant,
        ),
        userId = userId,
        remoteSource = FlagSource.static(mapOf("max_retries" to 5)),
    )

    // Reactive read — recomposes when flag changes (override or remote)
    val showNewCheckout = flags.collectFlagValue(Flags.newCheckout)
    val variant = flags.collectFlagValue(Flags.checkoutVariant)

    if (showNewCheckout) NewCheckout(variant) else OldCheckout()
}
```

Imperative reads work too:

```kotlin
class CheckoutVm(private val flags: FlagBar) {
    fun process() {
        repeat(flags.value(Flags.maxRetries)) { attempt(it) }
        when (flags.value(Flags.checkoutVariant)) {
            "control" -> oldFlow()
            "v1" -> newFlowA()
            "v2" -> newFlowB()
        }
    }
}
```

## Resolution chain

For every flag, value resolves in this order:

```
1. Local override   ← set via FlagOverrideDrawer, persisted to OverrideStorage
2. Remote value     ← last successful fetch from remoteSource (cached)
3. Default          ← what you declared in code
```

For **variant flags**:
```
1. Local override (force a variant from the drawer)
2. Deterministic hash(userId + flagKey) → bucket → variant by weights
3. Default (when userId is null)
```

Variant assignment is FNV-1a based, fast, no platform deps, deterministic per user.

## The override drawer

Use as a `debug-bar` tab (recommended):

```kotlin
DebugBar(
    sections = listOf(
        FlagBarSection(flags),
        NetworkLogSection(networkStore),
        // ...
    ),
) { MainAppContent() }
```

Or standalone (inside your own `ModalBottomSheet` / `Dialog`):

```kotlin
@Composable
fun MyOverlay() {
    Dialog(onDismissRequest = { ... }) {
        FlagOverrideDrawer(bar = flags)
    }
}
```

Per-type editors:

| Flag type | Editor |
|---|---|
| `BoolFlag` | Switch |
| `IntFlag` | Numeric text field |
| `StringFlag` | Free-form text field |
| `EnumFlag` | Dropdown of `options` |
| `VariantFlag` | Dropdown of `variants` + shows the hash-assigned default |

## Remote source (opt-in, vendor-neutral)

```kotlin
fun interface FlagSource { suspend fun fetch(): Map<String, Any> }

// Built-in:
FlagSource.Empty                                 // local-only mode
FlagSource.static(mapOf("key" to value))         // bake values in

// Custom — wrap any backend:
class MyApiFlagSource(private val client: HttpClient) : FlagSource {
    override suspend fun fetch(): Map<String, Any> =
        client.get("https://my-api.com/flags").body()
}
```

LaunchDarkly / ConfigCat / GrowthBook / Firebase Remote Config — same pattern, ~10 lines of wrapper.

## Override storage

```kotlin
interface OverrideStorage {
    fun getString(key: String): String?
    fun setString(key: String, value: String)
    fun remove(key: String)
}
```

Default: `OverrideStorage.InMemory()` — overrides reset on app restart.

For persistence, wrap `multiplatform-settings`:

```kotlin
class SettingsStorage(private val settings: Settings) : OverrideStorage {
    override fun getString(key: String): String? = settings.getStringOrNull(key)
    override fun setString(key: String, value: String) = settings.putString(key, value)
    override fun remove(key: String) = settings.remove(key)
}

val flags = rememberFlagBar(
    flags = AllFlags,
    storage = SettingsStorage(Settings()),  // multiplatform-settings
)
```

## Platforms

| Target | Status |
|---|---|
| Android (minSdk 24) | ✅ |
| iOS (x64, arm64, simulatorArm64) | ✅ |
| Desktop (JVM 11) | ✅ |
| Web (wasmJs) | ✅ |

## License

Apache 2.0 — see [LICENSE](LICENSE).
