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
    fun `Attribute with NATIONALITY type defaults displayInDiamond to false`() {
        val attribute = Attribute(id = "nationality", type = AttributeType.NATIONALITY)
        assertFalse(attribute.displayInDiamond)
    }

    @Test
    fun `Attribute with NATIONALITY type is not required by default`() {
        val attribute = Attribute(id = "nationality", type = AttributeType.NATIONALITY, isRequired = false)
        assertFalse(attribute.isRequired)
    }
}
