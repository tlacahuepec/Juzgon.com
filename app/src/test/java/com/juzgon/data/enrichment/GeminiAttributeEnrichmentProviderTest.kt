package com.juzgon.data.enrichment

import com.juzgon.domain.AttributeType
import com.juzgon.domain.CatalogType
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import com.juzgon.domain.enrichment.EnrichmentConfidence
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentStatus
import com.juzgon.domain.enrichment.FakeSecureApiKeyStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GeminiAttributeEnrichmentProviderTest {
    private lateinit var fakeKeyStore: FakeSecureApiKeyStore
    private lateinit var fakeApiClient: FakeGeminiApiClient
    private lateinit var provider: GeminiAttributeEnrichmentProvider

    @Before
    fun setUp() {
        fakeKeyStore = FakeSecureApiKeyStore()
        fakeApiClient = FakeGeminiApiClient()
        provider =
            GeminiAttributeEnrichmentProvider(
                apiKeyStore = fakeKeyStore,
                promptBuilder = GeminiPromptBuilder(),
                responseParser = GeminiResponseParser(),
                apiClient = fakeApiClient,
            )
    }

    @Test
    fun enrichAttribute_missingApiKey_returnsMissingApiKeyError() =
        runTest {
            val result = provider.enrichAttribute(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.MISSING_API_KEY, result.failureCode)
        }

    @Test
    fun enrichAttribute_validResponse_returnsFoundResult() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeApiClient.nextResponse =
                """
                {"status":"FOUND","value":"1987-06-24","displayValue":"June 24, 1987",
                 "confidence":"HIGH","sources":[{"title":"Wikipedia","url":"https://en.wikipedia.org"}]}
                """.trimIndent()

            val result = provider.enrichAttribute(testRequest())

            assertEquals(EnrichmentStatus.FOUND, result.status)
            assertEquals("1987-06-24", result.suggestedValue)
            assertEquals("June 24, 1987", result.displayValue)
            assertEquals(EnrichmentConfidence.HIGH, result.confidence)
            assertEquals(1, result.sources.size)
        }

    @Test
    fun enrichAttribute_networkError_returnsNetworkError() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeApiClient.nextException = IOException("Connection refused")

            val result = provider.enrichAttribute(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.NETWORK_ERROR, result.failureCode)
        }

    @Test
    fun enrichAttribute_http401_returnsInvalidApiKey() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeApiClient.nextException = GeminiApiException(401, "Unauthorized")

            val result = provider.enrichAttribute(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.INVALID_API_KEY, result.failureCode)
        }

    @Test
    fun enrichAttribute_http403_returnsInvalidApiKey() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeApiClient.nextException = GeminiApiException(403, "Forbidden")

            val result = provider.enrichAttribute(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.INVALID_API_KEY, result.failureCode)
        }

    @Test
    fun enrichAttribute_http429_returnsRateLimited() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeApiClient.nextException = GeminiApiException(429, "Too many requests")

            val result = provider.enrichAttribute(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.RATE_LIMITED, result.failureCode)
        }

    @Test
    fun enrichAttribute_invalidJson_returnsInvalidFormat() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeApiClient.nextResponse = "not valid json at all"

            val result = provider.enrichAttribute(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.INVALID_RESPONSE_FORMAT, result.failureCode)
        }

    @Test
    fun enrichAttribute_http500_returnsProviderError() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeApiClient.nextException = GeminiApiException(500, "Internal error")

            val result = provider.enrichAttribute(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.PROVIDER_ERROR, result.failureCode)
        }

    @Test
    fun enrichAttribute_usesGroundingByDefault() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeApiClient.nextResponse =
                """{"status":"FOUND","value":"1987-06-24","confidence":"HIGH","sources":[]}"""

            provider.enrichAttribute(testRequest())

            assertTrue(fakeApiClient.lastUseGrounding)
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
