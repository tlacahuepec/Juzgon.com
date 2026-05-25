package com.juzgon.domain.enrichment.usecase

import com.juzgon.domain.AttributeType
import com.juzgon.domain.CatalogType
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentConfidence
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentSource
import com.juzgon.domain.enrichment.EnrichmentStatus
import com.juzgon.domain.enrichment.FakeAttributeEnrichmentProvider
import com.juzgon.domain.enrichment.FakeSecureApiKeyStore
import com.juzgon.testutil.CapturingTimberTree
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SuggestAttributeValueUseCaseTest {
    private lateinit var fakeKeyStore: FakeSecureApiKeyStore
    private lateinit var fakeProvider: FakeAttributeEnrichmentProvider
    private lateinit var useCase: SuggestAttributeValueUseCase

    @Before
    fun setUp() {
        fakeKeyStore = FakeSecureApiKeyStore()
        fakeProvider = FakeAttributeEnrichmentProvider()
        useCase =
            SuggestAttributeValueUseCase(
                apiKeyStore = fakeKeyStore,
                provider = fakeProvider,
                validator = ValidateEnrichmentResultUseCase(),
            )
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun missingApiKey_returnsMissingApiKeyError() =
        runTest {
            val result = useCase(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.MISSING_API_KEY, result.failureCode)
        }

    @Test
    fun providerReturnsValidResult_returnsResult() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeProvider.nextResult =
                AttributeEnrichmentResult(
                    status = EnrichmentStatus.FOUND,
                    suggestedValue = "1987-06-24",
                    displayValue = "June 24, 1987",
                    confidence = EnrichmentConfidence.HIGH,
                    sources = listOf(EnrichmentSource(title = "Wikipedia")),
                )

            val result = useCase(testRequest())

            assertEquals(EnrichmentStatus.FOUND, result.status)
            assertEquals("1987-06-24", result.suggestedValue)
            assertEquals("June 24, 1987", result.displayValue)
            assertEquals(EnrichmentConfidence.HIGH, result.confidence)
        }

    @Test
    fun providerReturnsInvalidDate_returnsValidationFailed() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeProvider.nextResult =
                AttributeEnrichmentResult(
                    status = EnrichmentStatus.FOUND,
                    suggestedValue = "not-a-date",
                    confidence = EnrichmentConfidence.HIGH,
                )

            val result = useCase(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.VALIDATION_FAILED, result.failureCode)
        }

    @Test
    fun providerReturnsError_returnsErrorAsIs() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeProvider.nextResult =
                AttributeEnrichmentResult(
                    status = EnrichmentStatus.ERROR,
                    failureCode = EnrichmentFailureCode.NETWORK_ERROR,
                    reason = "Connection timeout",
                )

            val result = useCase(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.NETWORK_ERROR, result.failureCode)
            assertEquals("Connection timeout", result.reason)
        }

    @Test
    fun providerReturnsNotFound_returnsNotFound() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeProvider.nextResult =
                AttributeEnrichmentResult(
                    status = EnrichmentStatus.NOT_FOUND,
                    reason = "No reliable source found",
                )

            val result = useCase(testRequest())

            assertEquals(EnrichmentStatus.NOT_FOUND, result.status)
            assertEquals("No reliable source found", result.reason)
        }

    @Test
    fun providerReturnsLowConfidence_returnsValidationFailed() =
        runTest {
            fakeKeyStore.savedKey = "test-key"
            fakeProvider.nextResult =
                AttributeEnrichmentResult(
                    status = EnrichmentStatus.FOUND,
                    suggestedValue = "1987-06-24",
                    confidence = EnrichmentConfidence.LOW,
                )

            val result = useCase(testRequest())

            assertEquals(EnrichmentStatus.ERROR, result.status)
            assertEquals(EnrichmentFailureCode.VALIDATION_FAILED, result.failureCode)
        }

    @Test
    fun providerReturnsLowConfidence_logsRejected() =
        runTest {
            val tree = CapturingTimberTree()
            Timber.plant(tree)
            fakeKeyStore.savedKey = "test-key"
            fakeProvider.nextResult =
                AttributeEnrichmentResult(
                    status = EnrichmentStatus.FOUND,
                    suggestedValue = "1987-06-24",
                    confidence = EnrichmentConfidence.LOW,
                )

            useCase(testRequest())

            val rejectedLogs = tree.logs.filter { it.message.contains("rejected") }
            assertEquals(1, rejectedLogs.size)
            assertTrue(rejectedLogs[0].message.contains("attribute=birthDate"))
            assertTrue(rejectedLogs[0].message.contains("reason=VALIDATION_FAILED"))
            assertTrue(rejectedLogs[0].message.contains("confidence=LOW"))
        }

    @Test
    fun providerReturnsValidResult_doesNotLogRejected() =
        runTest {
            val tree = CapturingTimberTree()
            Timber.plant(tree)
            fakeKeyStore.savedKey = "test-key"
            fakeProvider.nextResult =
                AttributeEnrichmentResult(
                    status = EnrichmentStatus.FOUND,
                    suggestedValue = "1987-06-24",
                    displayValue = "June 24, 1987",
                    confidence = EnrichmentConfidence.HIGH,
                    sources = listOf(EnrichmentSource(title = "Wikipedia")),
                )

            useCase(testRequest())

            val rejectedLogs = tree.logs.filter { it.message.contains("rejected") }
            assertTrue(rejectedLogs.isEmpty())
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
