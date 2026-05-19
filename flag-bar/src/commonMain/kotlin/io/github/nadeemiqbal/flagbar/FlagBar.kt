package io.github.nadeemiqbal.flagbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Live feature-flag resolver. Holds:
 *  - The list of registered [Flag] declarations (so the drawer can render them).
 *  - The local-override map (persisted to [OverrideStorage]).
 *  - The latest successful remote payload (cached in memory; survives until app restart).
 *  - The current `userId` for variant assignment.
 *
 * Construct via [rememberFlagBar] from a composable, or directly for tests.
 */
class FlagBar(
    val flags: List<Flag<*>>,
    var userId: String?,
    private val source: FlagSource = FlagSource.Empty,
    private val storage: OverrideStorage = OverrideStorage.InMemory(),
    private val keyPrefix: String = "flagbar.override.",
) {

    /** In-memory remote cache. Reset on every successful [refresh]. */
    private val remoteState = MutableStateFlow<Map<String, Any>>(emptyMap())

    /** All current local overrides, keyed by flag key. */
    private val overridesState = MutableStateFlow<Map<String, String>>(loadOverrides())

    init {
        // Validate uniqueness — duplicate keys silently shadow each other otherwise.
        val duplicates = flags.groupBy { it.key }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate flag keys: $duplicates" }
    }

    /** Most-recent successful remote fetch payload. */
    val remote: StateFlow<Map<String, Any>> = remoteState.asStateFlow()

    /** Current local overrides. */
    val overrides: StateFlow<Map<String, String>> = overridesState.asStateFlow()

    /** Fetch the latest remote payload now. Errors are swallowed (cached value retained). */
    suspend fun refresh() {
        runCatching { source.fetch() }.getOrNull()?.let { remoteState.value = it }
    }

    /** Resolve a flag's current value via the override → remote (or variant hash) → default chain. */
    fun <T> value(flag: Flag<T>): T {
        // 1. Override (highest priority).
        overridesState.value[flag.key]?.let { raw -> flag.parseString(raw)?.let { return it } }

        // 2. Variant flags use deterministic hashing instead of remote-value parsing.
        if (flag is VariantFlag) {
            assignVariant(userId, flag)?.let {
                @Suppress("UNCHECKED_CAST")
                return it as T
            }
        }

        // 3. Remote value.
        remoteState.value[flag.key]?.let { raw -> flag.parseRaw(raw)?.let { return it } }

        // 4. Default.
        return flag.default
    }

    /** Set a local override. Persisted to [OverrideStorage] under "${keyPrefix}{flag.key}". */
    fun <T> setOverride(flag: Flag<T>, value: T) {
        val raw = flag.serialize(value)
        storage.setString(keyPrefix + flag.key, raw)
        overridesState.update { it + (flag.key to raw) }
    }

    /** Clear the override for a single flag (revert to remote / default). */
    fun clearOverride(flag: Flag<*>) {
        storage.remove(keyPrefix + flag.key)
        overridesState.update { it - flag.key }
    }

    /** Clear every override for every registered flag. */
    fun clearAllOverrides() {
        flags.forEach { storage.remove(keyPrefix + it.key) }
        overridesState.value = emptyMap()
    }

    /** True if [flag] currently has an override set. */
    fun hasOverride(flag: Flag<*>): Boolean = overridesState.value.containsKey(flag.key)

    /**
     * Snapshot the current resolved state of every flag as a `key=value (source)` list. Used by
     * `debug-bar`'s `ScreenshotBundleSection` and by the override drawer's display.
     */
    fun snapshot(): List<String> = flags.map { flag ->
        @Suppress("UNCHECKED_CAST")
        val v = value(flag as Flag<Any?>)
        val src = when {
            hasOverride(flag) -> "override"
            flag is VariantFlag && userId != null -> "hash"
            remoteState.value.containsKey(flag.key) -> "remote"
            else -> "default"
        }
        "${flag.key} = $v ($src)"
    }

    private fun loadOverrides(): Map<String, String> {
        val out = mutableMapOf<String, String>()
        for (flag in flags) {
            val raw = storage.getString(keyPrefix + flag.key)
            if (raw != null) out[flag.key] = raw
        }
        return out
    }
}

/**
 * Create + remember a `FlagBar`. If [remoteSource] is non-empty and [fetchInterval] is positive,
 * a polling coroutine refreshes the remote cache periodically.
 */
@Composable
fun rememberFlagBar(
    flags: List<Flag<*>>,
    userId: String? = null,
    remoteSource: FlagSource = FlagSource.Empty,
    storage: OverrideStorage = OverrideStorage.InMemory(),
    fetchInterval: Duration = 10.minutes,
): FlagBar {
    val bar = remember(flags, userId, remoteSource, storage) {
        FlagBar(flags = flags, userId = userId, source = remoteSource, storage = storage)
    }
    LaunchedEffect(bar, fetchInterval) {
        bar.refresh()
        while (fetchInterval.isPositive()) {
            delay(fetchInterval)
            bar.refresh()
        }
    }
    return bar
}

/**
 * Reactive read of a single flag — recomposes when overrides or the remote cache change.
 *
 * Combines [FlagBar.overrides] + [FlagBar.remote] into a single flow that emits the freshly
 * resolved value each time either changes, then converts to a Compose `State` via
 * [collectAsState].
 */
@Composable
fun <T> FlagBar.collectFlagValue(flag: Flag<T>): T {
    val combined = remember(this, flag) {
        kotlinx.coroutines.flow.combine(overrides, remote) { _, _ -> value(flag) }
    }
    val state = combined.collectAsState(initial = value(flag))
    return state.value
}
