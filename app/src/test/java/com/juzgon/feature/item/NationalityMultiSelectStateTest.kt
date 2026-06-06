package com.juzgon.feature.item

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NationalityMultiSelectStateTest {
    @Test
    fun `initial value empty produces empty selectedCodes`() {
        val state = NationalityMultiSelectState("")
        assertEquals(emptyList<String>(), state.selectedCodes)
    }

    @Test
    fun `initial value single code produces single-element list`() {
        val state = NationalityMultiSelectState("BR")
        assertEquals(listOf("BR"), state.selectedCodes)
    }

    @Test
    fun `initial value multi codes produces list in order`() {
        val state = NationalityMultiSelectState("BR,IT")
        assertEquals(listOf("BR", "IT"), state.selectedCodes)
    }

    @Test
    fun `addNationality appends code and invokes callback`() {
        val state = NationalityMultiSelectState("BR")
        var callbackValue = ""
        state.addNationality("IT", "attr1") { _, value -> callbackValue = value }

        assertEquals(listOf("BR", "IT"), state.selectedCodes)
        assertEquals("BR,IT", callbackValue)
    }

    @Test
    fun `addNationality deduplicates existing code`() {
        val state = NationalityMultiSelectState("BR,IT")
        var callbackCalled = false
        state.addNationality("BR", "attr1") { _, _ -> callbackCalled = true }

        assertEquals(listOf("BR", "IT"), state.selectedCodes)
        assertFalse(callbackCalled)
    }

    @Test
    fun `removeNationality removes code and invokes callback`() {
        val state = NationalityMultiSelectState("BR,IT")
        var callbackValue = ""
        state.removeNationality("BR", "attr1") { _, value -> callbackValue = value }

        assertEquals(listOf("IT"), state.selectedCodes)
        assertEquals("IT", callbackValue)
    }

    @Test
    fun `removeNationality all codes produces empty callback`() {
        val state = NationalityMultiSelectState("BR")
        var callbackValue = ""
        state.removeNationality("BR", "attr1") { _, value -> callbackValue = value }

        assertEquals(emptyList<String>(), state.selectedCodes)
        assertEquals("", callbackValue)
    }

    @Test
    fun `suggestions excludes already selected codes`() {
        val state = NationalityMultiSelectState("BR")
        val suggestions = state.suggestions
        assertTrue(suggestions.none { it.code == "BR" })
    }

    @Test
    fun `searchQuery filters suggestions`() {
        val state = NationalityMultiSelectState("")
        state.onSearchQueryChange("Ital")
        val suggestions = state.suggestions
        assertTrue(suggestions.any { it.code == "IT" })
        assertTrue(suggestions.size <= 10)
    }

    @Test
    fun `updateFromExternalValue updates selectedCodes`() {
        val state = NationalityMultiSelectState("BR")
        state.updateFromExternalValue("BR,IT,US")
        assertEquals(listOf("BR", "IT", "US"), state.selectedCodes)
    }
}
