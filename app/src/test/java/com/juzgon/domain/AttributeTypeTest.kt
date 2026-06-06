package com.juzgon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttributeTypeTest {
    @Test
    fun `NATIONALITY exists in AttributeType entries`() {
        assertTrue(AttributeType.entries.any { it.name == "NATIONALITY" })
    }

    @Test
    fun `SKIN_TYPE exists in AttributeType entries`() {
        assertTrue(AttributeType.entries.any { it.name == "SKIN_TYPE" })
    }

    @Test
    fun `isRankable false for SKIN_TYPE`() {
        val attribute = Attribute(id = "People/Skin Type", type = AttributeType.SKIN_TYPE)

        assertFalse(attribute.isRankable)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SKIN_TYPE with displayInDiamond true throws`() {
        Attribute(
            id = "People/Skin Type",
            type = AttributeType.SKIN_TYPE,
            displayInDiamond = true,
        )
    }

    @Test
    fun `SkinTypeValues expose six Fitzpatrick values in natural order`() {
        assertEquals(
            listOf("TYPE_I", "TYPE_II", "TYPE_III", "TYPE_IV", "TYPE_V", "TYPE_VI"),
            SkinTypeValues.entries.map { it.storedValue },
        )
        assertEquals("Type I, very light", SkinTypeValues.entries.first().displayLabel)
        assertEquals("Type VI, very dark", SkinTypeValues.entries.last().displayLabel)
    }

    @Test
    fun `SkinTypeValues normalize labels roman numerals and stored values`() {
        assertEquals(SkinTypeValues.typeI, SkinTypeValues.fromStoredValue("TYPE_I"))
        assertEquals(SkinTypeValues.typeI, SkinTypeValues.fromStoredValue("Type I"))
        assertEquals(SkinTypeValues.typeI, SkinTypeValues.fromStoredValue("I"))
        assertEquals(SkinTypeValues.typeVI, SkinTypeValues.fromStoredValue("type vi, very dark"))
    }

    @Test
    fun `SkinTypeValues return unknown display for invalid values`() {
        assertNull(SkinTypeValues.fromStoredValue("TYPE_X"))
        assertEquals("Unknown skin type", SkinTypeValues.displayLabelOrUnknown("TYPE_X"))
    }

    @Test
    fun `DATE exists in AttributeType entries`() {
        assertTrue(AttributeType.entries.any { it.name == "DATE" })
    }

    @Test
    fun `Attribute with NATIONALITY type defaults displayInDiamond to false`() {
        val attribute = Attribute(id = "nationality", type = AttributeType.NATIONALITY)
        assertFalse(attribute.displayInDiamond)
    }

    @Test
    fun `Attribute with NATIONALITY type is not required by default`() {
        val attribute = Attribute(id = "nationality", type = AttributeType.NATIONALITY, isRequired = false)
        assertFalse(attribute.isRequired)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `scoringDirection on NUMBER type throws`() {
        Attribute(id = "test", type = AttributeType.NUMBER, scoringDirection = ScoringDirection.NEWER_IS_BETTER)
    }

    @Test
    fun `scoringDirection on DATE type is valid`() {
        val attribute =
            Attribute(
                id = "released",
                type = AttributeType.DATE,
                scoringDirection = ScoringDirection.NEWER_IS_BETTER,
            )
        assertTrue(attribute.scoringDirection == ScoringDirection.NEWER_IS_BETTER)
    }

    @Test
    fun `isRankable true for NUMBER`() {
        val attribute = Attribute(id = "score", type = AttributeType.NUMBER)
        assertTrue(attribute.isRankable)
    }

    @Test
    fun `isRankable true for DATE with scoringDirection`() {
        val attribute =
            Attribute(
                id = "released",
                type = AttributeType.DATE,
                scoringDirection = ScoringDirection.OLDER_IS_BETTER,
            )
        assertTrue(attribute.isRankable)
    }

    @Test
    fun `isRankable false for DATE without scoringDirection`() {
        val attribute = Attribute(id = "released", type = AttributeType.DATE)
        assertFalse(attribute.isRankable)
    }

    @Test
    fun `isRankable false for NATIONALITY`() {
        val attribute = Attribute(id = "nationality", type = AttributeType.NATIONALITY)
        assertFalse(attribute.isRankable)
    }

    @Test
    fun `rankable DATE with displayInDiamond true does not throw`() {
        val attribute =
            Attribute(
                id = "released",
                type = AttributeType.DATE,
                scoringDirection = ScoringDirection.NEWER_IS_BETTER,
                displayInDiamond = true,
                diamondOrder = 1,
            )
        assertTrue(attribute.displayInDiamond)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-rankable DATE with displayInDiamond true throws`() {
        Attribute(
            id = "released",
            type = AttributeType.DATE,
            displayInDiamond = true,
        )
    }
}
