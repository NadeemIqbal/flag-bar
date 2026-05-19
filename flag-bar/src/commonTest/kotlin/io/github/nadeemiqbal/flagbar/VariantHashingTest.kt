package io.github.nadeemiqbal.flagbar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VariantHashingTest {

    private val flag = VariantFlag(
        key = "checkout_variant",
        variants = listOf("control", "v1", "v2"),
        weights = listOf(0.5, 0.25, 0.25),
        default = "control",
    )

    @Test fun anonymousReturnsNull() {
        assertNull(assignVariant(userId = null, flag = flag))
    }

    @Test fun assignmentReturnsOneOfTheVariants() {
        val v = assertNotNull(assignVariant(userId = "user-123", flag = flag))
        assertTrue(v in flag.variants, "got '$v', not in ${flag.variants}")
    }

    @Test fun sameUserSameKeyAlwaysSameVariant() {
        val a = assignVariant("user-123", flag)
        val b = assignVariant("user-123", flag)
        val c = assignVariant("user-123", flag)
        assertEquals(a, b)
        assertEquals(b, c)
    }

    @Test fun differentUsersGetIndependentAssignments() {
        // With a 50/25/25 split and 100 users, all of them landing in "control" would have
        // probability 0.5^100 — vanishingly unlikely. Spot-check we see at least 2 variants.
        val results = (0 until 100).map { assignVariant("user-$it", flag) }.toSet()
        assertTrue(results.size >= 2, "expected >=2 distinct variants over 100 users, got $results")
    }

    @Test fun roughlyMatchesConfiguredWeights() {
        val counts = mutableMapOf<String, Int>()
        val n = 10_000
        for (i in 0 until n) {
            val v = assignVariant("u-$i", flag)!!
            counts[v] = (counts[v] ?: 0) + 1
        }
        // 50/25/25 with N=10k → control ~ 5000±200, v1/v2 ~ 2500±200 each.
        // Generous tolerance because FNV-1a isn't a great hash; we just want bucket-fairness.
        val control = counts["control"]!!
        val v1 = counts["v1"]!!
        val v2 = counts["v2"]!!
        assertTrue(control in 4500..5500, "control=$control")
        assertTrue(v1 in 2200..2800, "v1=$v1")
        assertTrue(v2 in 2200..2800, "v2=$v2")
    }

    @Test fun fnv1aIsDeterministic() {
        val a = fnv1a32("hello".encodeToByteArray())
        val b = fnv1a32("hello".encodeToByteArray())
        assertEquals(a, b)
    }

    @Test fun fnv1aDiffersForDifferentInputs() {
        val a = fnv1a32("hello".encodeToByteArray())
        val b = fnv1a32("hello!".encodeToByteArray())
        assertTrue(a != b)
    }
}
