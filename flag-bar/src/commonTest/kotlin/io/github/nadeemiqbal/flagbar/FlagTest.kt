package io.github.nadeemiqbal.flagbar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FlagTest {

    // --- BoolFlag --------------------------------------------------------------------------

    @Test fun boolFlag_parsesBooleanRaw() {
        val flag = BoolFlag("k", default = false)
        assertEquals(true, flag.parseRaw(true))
        assertEquals(false, flag.parseRaw(false))
    }

    @Test fun boolFlag_parsesNumberRaw() {
        val flag = BoolFlag("k", default = false)
        assertEquals(true, flag.parseRaw(1))
        assertEquals(false, flag.parseRaw(0))
    }

    @Test fun boolFlag_parsesStringRaw() {
        val flag = BoolFlag("k", default = false)
        assertEquals(true, flag.parseRaw("true"))
        assertEquals(false, flag.parseRaw("false"))
        assertEquals(true, flag.parseRaw("1"))
        assertEquals(false, flag.parseRaw("0"))
    }

    @Test fun boolFlag_rejectsGarbage() {
        val flag = BoolFlag("k", default = false)
        assertNull(flag.parseRaw("not_a_bool"))
        assertNull(flag.parseRaw(null))
    }

    // --- IntFlag ---------------------------------------------------------------------------

    @Test fun intFlag_parsesNumeric() {
        val flag = IntFlag("k", default = 0)
        assertEquals(42, flag.parseRaw(42))
        assertEquals(42, flag.parseRaw(42.0))
        assertEquals(42, flag.parseRaw("42"))
        assertNull(flag.parseRaw("not_a_num"))
    }

    // --- StringFlag ------------------------------------------------------------------------

    @Test fun stringFlag_acceptsAnyNonNull() {
        val flag = StringFlag("k", default = "x")
        assertEquals("hi", flag.parseRaw("hi"))
        assertEquals("42", flag.parseRaw(42))
        assertNull(flag.parseRaw(null))
    }

    // --- EnumFlag --------------------------------------------------------------------------

    enum class Theme { System, Light, Dark }

    @Test fun enumFlag_parsesByName_caseInsensitive() {
        val flag = EnumFlag("theme", default = Theme.System, options = Theme.entries)
        assertEquals(Theme.Dark, flag.parseRaw("Dark"))
        assertEquals(Theme.Light, flag.parseRaw("light"))
        assertEquals(Theme.System, flag.parseRaw("SYSTEM"))
        assertNull(flag.parseRaw("unknown"))
    }

    // --- VariantFlag -----------------------------------------------------------------------

    @Test fun variantFlag_validatesInvariants() {
        // Empty variants: the `default = variants.first()` default arg trips a NoSuchElementException
        // before our `require()` even runs. Either way the construction is rejected — accept any
        // throwable here.
        kotlin.test.assertFails {
            VariantFlag("k", variants = emptyList(), weights = emptyList())
        }
        // For non-empty variants the `require()` blocks run and we get the expected IAE.
        assertFailsWith<IllegalArgumentException> {
            VariantFlag("k", variants = listOf("a", "b"), weights = listOf(0.5))
        }
        assertFailsWith<IllegalArgumentException> {
            VariantFlag("k", variants = listOf("a"), weights = listOf(-1.0))
        }
        assertFailsWith<IllegalArgumentException> {
            VariantFlag("k", variants = listOf("a"), weights = listOf(0.0))
        }
        assertFailsWith<IllegalArgumentException> {
            VariantFlag("k", variants = listOf("a"), weights = listOf(1.0), default = "z")
        }
    }

    @Test fun variantFlag_acceptsKnownString_rejectsUnknown() {
        val flag = VariantFlag("k", variants = listOf("a", "b"), weights = listOf(0.5, 0.5))
        assertEquals("a", flag.parseRaw("a"))
        assertEquals("b", flag.parseRaw("b"))
        assertNull(flag.parseRaw("z"))
    }
}
