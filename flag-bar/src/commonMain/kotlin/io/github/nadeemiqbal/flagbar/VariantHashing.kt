package io.github.nadeemiqbal.flagbar

/**
 * Deterministic variant assignment for [VariantFlag]:
 *  - Same `userId + key` → always picks the same variant on every device.
 *  - Different keys for the same user → independent assignments (so users in `flagA = v1` are not
 *    correlated with `flagB = v1`).
 *  - Anonymous (`userId == null`) → returns `null`, caller falls back to the flag's default.
 *
 * Hashing uses FNV-1a (32-bit) — fast, deterministic, no platform dependencies, no crypto.
 * Don't use this output for security; it's stable bucketing only.
 */
fun assignVariant(
    userId: String?,
    flag: VariantFlag,
): String? {
    if (userId == null) return null

    val composite = "${flag.key}:$userId".encodeToByteArray()
    val hash = fnv1a32(composite)
    // Normalise to [0.0, 1.0) — wrap to UInt-equivalent for unsigned semantics.
    val bucket = (hash.toLong() and 0xFFFFFFFFL).toDouble() / (1L shl 32).toDouble()

    val total = flag.weights.sum()
    var cumulative = 0.0
    for ((index, weight) in flag.weights.withIndex()) {
        cumulative += weight / total
        if (bucket < cumulative) return flag.variants[index]
    }
    // Rounding edge case — last variant catches it.
    return flag.variants.last()
}

internal fun fnv1a32(bytes: ByteArray): Int {
    // FNV-1a 32-bit:
    //   hash = OFFSET_BASIS
    //   for byte in bytes: hash = (hash XOR byte) * PRIME
    var hash = -0x7ee3623b           // 0x811c9dc5 as signed Int = the FNV-1a offset basis
    val prime = 0x01000193           // FNV-1a 32-bit prime
    for (b in bytes) {
        hash = hash xor (b.toInt() and 0xFF)
        hash *= prime
    }
    return hash
}
