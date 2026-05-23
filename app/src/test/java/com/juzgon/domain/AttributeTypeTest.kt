package com.juzgon.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttributeTypeTest {
    @Test
    fun `NATIONALITY exists in AttributeType entries`() {
        assertTrue(AttributeType.entries.any { it.name == "NATIONALITY" })
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
}
