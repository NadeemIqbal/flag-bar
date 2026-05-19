package io.github.nadeemiqbal.flagbar

/**
 * Base class for a single feature-flag declaration. Concrete subclasses:
 *
 * - [BoolFlag] — toggles (`true` / `false`)
 * - [IntFlag] — integer values (timeouts, retry counts, batch sizes)
 * - [StringFlag] — free-form strings (URLs, tokens, identifiers)
 * - [EnumFlag] — typed enums (themes, modes)
 * - [VariantFlag] — A/B/n variant assignment via deterministic hashing of user-id + key
 *
 * Flags are declared as static `val`s on a singleton (typical pattern):
 *
 * ```
 * object Flags {
 *     val newCheckout = BoolFlag("new_checkout", default = false)
 *     val maxRetries = IntFlag("max_retries", default = 3)
 *     val theme = EnumFlag("theme", default = Theme.SYSTEM, options = Theme.entries)
 * }
 * ```
 *
 * The [key] is the string identifier used for:
 *  - Local overrides persisted in `multiplatform-settings` under `"flagbar.override.$key"`.
 *  - Remote payload lookup (a `Map<String, Any>` returned by `FlagSource.fetch()`).
 *  - The drawer UI label (humanised by the renderer).
 */
sealed class Flag<T> {
    /** Unique identifier — should be `snake_case` and stable across releases. */
    abstract val key: String

    /** Fallback when no override/remote value is set. */
    abstract val default: T

    /** Parse a raw value (from the remote source) into this flag's `T`, or `null` if invalid. */
    internal abstract fun parseRaw(raw: Any?): T?

    /** Parse a raw String (from local override storage) into this flag's `T`, or `null` if invalid. */
    internal abstract fun parseString(raw: String): T?

    /** Serialise an override value back to a String for storage. */
    internal abstract fun serialize(value: T): String
}

/** Boolean feature flag. Accepts `true`/`false`, `"true"`/`"false"`, `1`/`0` from remote sources. */
data class BoolFlag(
    override val key: String,
    override val default: Boolean,
) : Flag<Boolean>() {
    override fun parseRaw(raw: Any?): Boolean? = when (raw) {
        is Boolean -> raw
        is Number -> raw.toInt() != 0
        is String -> raw.toBooleanStrictOrNull() ?: raw.toIntOrNull()?.let { it != 0 }
        else -> null
    }
    override fun parseString(raw: String): Boolean? = raw.toBooleanStrictOrNull()
    override fun serialize(value: Boolean): String = value.toString()
}

/** Integer feature flag. Accepts `Number` or numeric `String` from remote sources. */
data class IntFlag(
    override val key: String,
    override val default: Int,
) : Flag<Int>() {
    override fun parseRaw(raw: Any?): Int? = when (raw) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull()
        else -> null
    }
    override fun parseString(raw: String): Int? = raw.toIntOrNull()
    override fun serialize(value: Int): String = value.toString()
}

/** String feature flag. */
data class StringFlag(
    override val key: String,
    override val default: String,
) : Flag<String>() {
    override fun parseRaw(raw: Any?): String? = raw?.toString()
    override fun parseString(raw: String): String? = raw
    override fun serialize(value: String): String = value
}

/**
 * Enum flag — value type is your enum. Pass `options = MyEnum.entries` so the override drawer can
 * render a picker; raw values from remote sources resolve by case-insensitive name.
 */
data class EnumFlag<E : Enum<E>>(
    override val key: String,
    override val default: E,
    val options: List<E>,
) : Flag<E>() {
    override fun parseRaw(raw: Any?): E? = parseString(raw?.toString() ?: return null)
    override fun parseString(raw: String): E? = options.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    override fun serialize(value: E): String = value.name
}

/**
 * A/B/n variant flag. The resolved value is one of [variants], picked by:
 *  1. A local override (force a specific variant — set via the drawer)
 *  2. Deterministic hash of `userId + key` → bucket → variant via [weights]
 *  3. [default] if `userId` is null (anonymous fallback)
 *
 * [weights] must have the same size as [variants] and should sum close to 1.0; non-normalised
 * inputs are normalised at evaluation time. Sample call:
 *
 * ```
 * VariantFlag(
 *     key = "checkout_variant",
 *     variants = listOf("control", "v1", "v2"),
 *     weights = listOf(0.5, 0.25, 0.25),  // 50% / 25% / 25%
 *     default = "control",
 * )
 * ```
 */
data class VariantFlag(
    override val key: String,
    val variants: List<String>,
    val weights: List<Double>,
    override val default: String = variants.first(),
) : Flag<String>() {
    init {
        require(variants.isNotEmpty()) { "VariantFlag requires at least one variant" }
        require(weights.size == variants.size) { "weights must match variants in length" }
        require(weights.all { it >= 0.0 }) { "weights must be non-negative" }
        require(weights.sum() > 0.0) { "weights must sum > 0" }
        require(default in variants) { "default ('$default') must be one of variants ($variants)" }
    }

    override fun parseRaw(raw: Any?): String? = parseString(raw?.toString() ?: return null)
    override fun parseString(raw: String): String? = if (raw in variants) raw else null
    override fun serialize(value: String): String = value
}
