package io.github.nadeemiqbal.flagbar

/**
 * Persistence layer for local overrides. The library ships [InMemory] (default, transient) and
 * an interface so hosts can plug in `multiplatform-settings`, DataStore, or any KV store of
 * their choice.
 *
 * Example adapter for `multiplatform-settings`:
 * ```
 * class MultiplatformSettingsStorage(private val settings: Settings) : OverrideStorage {
 *     override fun getString(key: String): String? = settings.getStringOrNull(key)
 *     override fun setString(key: String, value: String) { settings.putString(key, value) }
 *     override fun remove(key: String) { settings.remove(key) }
 * }
 * ```
 */
interface OverrideStorage {
    /** Read the override value stored under [key]. Returns `null` when nothing is stored. */
    fun getString(key: String): String?

    /** Persist [value] under [key]. */
    fun setString(key: String, value: String)

    /** Delete the value at [key]. No-op when nothing is stored. */
    fun remove(key: String)

    /**
     * Transient in-memory storage — the default. Suitable for tests, for apps that don't need
     * overrides to survive process death, and for demos. Wrap `multiplatform-settings` or
     * DataStore if you need persistence.
     */
    class InMemory : OverrideStorage {
        private val map = mutableMapOf<String, String>()
        override fun getString(key: String): String? = map[key]
        override fun setString(key: String, value: String) { map[key] = value }
        override fun remove(key: String) { map.remove(key) }
    }
}
