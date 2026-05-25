package com.juzgon.data.enrichment

import com.juzgon.domain.AttributeType
import com.juzgon.domain.CatalogType
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiPromptBuilderTest {
    private val builder = GeminiPromptBuilder()

    @Test
    fun build_includesCatalogDescription() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("Professional soccer players"))
    }

    @Test
    fun build_includesCatalogType() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("PERSON"))
    }

    @Test
    fun build_includesItemName() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("Lionel Messi"))
    }

    @Test
    fun build_includesExistingAttributes() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("Nationality"))
        assertTrue(prompt.contains("Argentina"))
    }

    @Test
    fun build_includesTargetAttributeLabel() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("Birth Date"))
    }

    @Test
    fun build_includesDateFormatInstructions() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("ISO-8601"))
        assertTrue(prompt.contains("YYYY-MM-DD"))
    }

    @Test
    fun build_includesJsonResponseFormat() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("\"status\""))
        assertTrue(prompt.contains("\"value\""))
        assertTrue(prompt.contains("\"confidence\""))
        assertTrue(prompt.contains("\"sources\""))
    }

    @Test
    fun build_includesRules() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("Do not guess"))
        assertTrue(prompt.contains("NOT_FOUND"))
        assertTrue(prompt.contains("CONFLICT"))
    }

    @Test
    fun build_handlesNullDescription() {
        val request = requestWithAllFields().copy(catalogDescription = null)

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Lionel Messi"))
    }

    @Test
    fun build_handlesNullCatalogType() {
        val request = requestWithAllFields().copy(catalogType = null)

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Lionel Messi"))
    }

    @Test
    fun build_handlesEmptyExistingAttributes() {
        val request = requestWithAllFields().copy(existingAttributes = emptyMap())

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Lionel Messi"))
        assertTrue(!prompt.contains("Known attributes"))
    }

    @Test
    fun build_numberType_showsNumericValue() {
        val request =
            requestWithAllFields().copy(
                targetAttributeType = AttributeType.NUMBER,
                targetAttributeLabel = "Height (cm)",
            )

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Numeric value"))
    }

    private fun requestWithAllFields() =
        AttributeEnrichmentRequest(
            catalogId = "cat-1",
            catalogDescription = "Professional soccer players",
            catalogType = CatalogType.PERSON,
            itemId = "item-1",
            itemName = "Lionel Messi",
            existingAttributes = mapOf("Nationality" to "Argentina"),
            targetAttributeKey = "birthDate",
            targetAttributeLabel = "Birth Date",
            targetAttributeType = AttributeType.DATE,
        )
}
