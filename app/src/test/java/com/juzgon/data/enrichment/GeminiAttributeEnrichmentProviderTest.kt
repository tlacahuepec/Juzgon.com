package com.juzgon.data.enrichment

import com.juzgon.domain.AttributeType
import com.juzgon.domain.CatalogType
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentStatus
import com.juzgon.domain.enrichment.FakeSecureApiKeyStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiAttributeEnrichmentProviderTest {
    @Test
    fun enrichAttribute_missingApiKey_returnsMissingApiKeyError() =
        runTest {
            val fakeKeyStore = FakeSecureApiKeyStore()
            val provider =
                GeminiAttributeEnrichmentProvider(
                    apiKeyStore = fakeKeyStore,
                    promptBuilder = GeminiPromptBuilder(),
                    responseParser = GeminiResponseParser(),
                )

            val result = provider.enrichAttribute(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.MISSING_API_KEY, result.failureCode)
        }

    private fun testRequest() =
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
