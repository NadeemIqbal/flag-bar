package io.github.nadeemiqbal.flagbar

/**
 * Source for remote flag values. The library is vendor-neutral — wrap any backend (your own
 * HTTP endpoint, LaunchDarkly, ConfigCat, GrowthBook, Firebase Remote Config) in a
 * [FlagSource] implementation and pass it to [rememberFlagBar].
 *
 * Implementations should:
 *  - Return the latest known values from cache (don't block on network if the cache is fresh).
 *  - Throw or return an empty map on transient failures (the caller will keep the previous
 *    successful fetch and the default values).
 *
 * The map keys correspond to the [Flag.key] declarations. Values can be primitives (`Boolean`,
 * `Number`, `String`) or `Map<String, Any>` — each flag type knows how to parse what it expects.
 */
fun interface FlagSource {

    /**
     * Fetch the current remote payload. Called on a polling interval by `FlagBar` (default 10
     * min) and on demand via `flagBar.refresh()`.
     */
    suspend fun fetch(): Map<String, Any>

    companion object {
        /** No remote source. Flag values resolve from overrides + defaults only. */
        val Empty: FlagSource = FlagSource { emptyMap() }

        /** Always returns the same baked-in payload. Useful for tests and "snapshot" deployments. */
        fun static(values: Map<String, Any>): FlagSource = FlagSource { values }
    }
}
