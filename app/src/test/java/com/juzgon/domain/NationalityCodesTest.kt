package com.juzgon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NationalityCodesTest {
    @Test
    fun `parse null returns empty list`() {
        assertEquals(emptyList<String>(), NationalityCodes.parse(null))
    }

    @Test
    fun `parse blank returns empty list`() {
        assertEquals(emptyList<String>(), NationalityCodes.parse(""))
        assertEquals(emptyList<String>(), NationalityCodes.parse("   "))
    }

    @Test
    fun `parse single code returns single-element list`() {
        assertEquals(listOf("BR"), NationalityCodes.parse("BR"))
    }

    @Test
    fun `parse multiple codes returns list in order`() {
        assertEquals(listOf("BR", "IT"), NationalityCodes.parse("BR,IT"))
        assertEquals(listOf("BR", "IT", "US"), NationalityCodes.parse("BR,IT,US"))
    }

    @Test
    fun `parse trims whitespace around codes`() {
        assertEquals(listOf("BR", "IT"), NationalityCodes.parse(" BR , IT "))
    }

    @Test
    fun `parse filters out empty segments`() {
        assertEquals(listOf("BR", "IT"), NationalityCodes.parse("BR,,IT"))
    }

    @Test
    fun `encode empty list returns empty string`() {
        assertEquals("", NationalityCodes.encode(emptyList()))
    }

    @Test
    fun `encode single code returns code`() {
        assertEquals("BR", NationalityCodes.encode(listOf("BR")))
    }

    @Test
    fun `encode multiple codes returns comma-separated`() {
        assertEquals("BR,IT", NationalityCodes.encode(listOf("BR", "IT")))
        assertEquals("BR,IT,US", NationalityCodes.encode(listOf("BR", "IT", "US")))
    }

    @Test
    fun `primary null returns null`() {
        assertNull(NationalityCodes.primary(null))
    }

    @Test
    fun `primary blank returns null`() {
        assertNull(NationalityCodes.primary(""))
    }

    @Test
    fun `primary single code returns that code`() {
        assertEquals("BR", NationalityCodes.primary("BR"))
    }

    @Test
    fun `primary multiple codes returns first`() {
        assertEquals("BR", NationalityCodes.primary("BR,IT"))
    }
}
