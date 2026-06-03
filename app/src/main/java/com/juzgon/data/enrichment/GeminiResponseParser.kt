@file:Suppress("TooGenericExceptionCaught")

package com.juzgon.data.enrichment

import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentCandidateValue
import com.juzgon.domain.enrichment.EnrichmentConfidence
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentSource
import com.juzgon.domain.enrichment.EnrichmentStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GeminiResponseParser
    @Inject
    constructor() {
        private val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

        fun parse(responseText: String): AttributeEnrichmentResult {
            if (responseText.isBlank()) {
                return errorResult(EnrichmentFailureCode.INVALID_RESPONSE_FORMAT)
            }
            return try {
                val wire = json.decodeFromString<GeminiWireResponse>(responseText)
                wire.toDomain()
            } catch (_: Exception) {
                errorResult(EnrichmentFailureCode.INVALID_RESPONSE_FORMAT)
            }
        }

        private fun GeminiWireResponse.toDomain(): AttributeEnrichmentResult {
            val mappedStatus = mapStatus(status) ?: return errorResult(EnrichmentFailureCode.PROVIDER_ERROR)
            val mappedConfidence = mapConfidence(confidence)
            val mappedSources =
                sources?.map { source ->
                    EnrichmentSource(
                        title = source.title,
                        url = source.url,
                        snippet = source.snippet,
                    )
                } ?: emptyList()
            val mappedCandidates =
                candidateValues?.mapNotNull { candidate ->
                    candidate.value?.let { v ->
                        EnrichmentCandidateValue(
                            value = v,
                            displayValue = candidate.displayValue,
                            sourceLabel = candidate.source,
                        )
                    }
                } ?: emptyList()

            return AttributeEnrichmentResult(
                status = mappedStatus,
                suggestedValue = value,
                displayValue = displayValue,
                confidence = mappedConfidence,
                sources = mappedSources,
                reason = reason,
                candidateValues = mappedCandidates,
            )
        }

        private fun mapStatus(status: String?): EnrichmentStatus? =
            when (status?.uppercase()) {
                "FOUND" -> EnrichmentStatus.FOUND
                "NOT_FOUND" -> EnrichmentStatus.NOT_FOUND
                "CONFLICT" -> EnrichmentStatus.CONFLICT
                else -> null
            }

        private fun mapConfidence(confidence: String?): EnrichmentConfidence? =
            when (confidence?.uppercase()) {
                "HIGH" -> EnrichmentConfidence.HIGH
                "MEDIUM" -> EnrichmentConfidence.MEDIUM
                "LOW" -> EnrichmentConfidence.LOW
                else -> null
            }

        private fun errorResult(failureCode: EnrichmentFailureCode) =
            AttributeEnrichmentResult(
                status = EnrichmentStatus.ERROR,
                failureCode = failureCode,
            )
    }

@Serializable
internal data class GeminiWireResponse(
    val status: String? = null,
    val value: String? = null,
    @SerialName("displayValue") val displayValue: String? = null,
    val confidence: String? = null,
    val reason: String? = null,
    val sources: List<GeminiWireSource>? = null,
    val candidateValues: List<GeminiWireCandidateValue>? = null,
)

@Serializable
internal data class GeminiWireSource(
    val title: String? = null,
    val url: String? = null,
    val snippet: String? = null,
)

@Serializable
internal data class GeminiWireCandidateValue(
    val value: String? = null,
    val displayValue: String? = null,
    val source: String? = null,
)
