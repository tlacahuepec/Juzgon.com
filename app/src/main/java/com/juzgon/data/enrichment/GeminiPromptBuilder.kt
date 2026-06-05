package com.juzgon.data.enrichment

import com.juzgon.domain.AttributeType
import com.juzgon.domain.CatalogType
import com.juzgon.domain.NationalityCodes
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import javax.inject.Inject

class GeminiPromptBuilder
    @Inject
    constructor() {
        fun build(request: AttributeEnrichmentRequest): String =
            buildString {
                appendLine("You are extracting one factual attribute for a catalog item.")
                appendLine()
                appendCatalogContext(request)
                appendItemContext(request)
                appendTrustedReferenceSites(request)
                appendSocialMediaDisambiguation(request)
                appendNationalitySearchHint(request)
                appendTargetAttribute(request)
                appendRules(request)
                appendResponseFormat()
            }

        private fun StringBuilder.appendCatalogContext(request: AttributeEnrichmentRequest) {
            if (request.catalogDescription != null || request.catalogType != null) {
                appendLine("Catalog context:")
                request.catalogDescription?.let { appendLine("Description: $it") }
                request.catalogType?.let { appendLine("Type: ${it.name}") }
                appendLine()
            }
        }

        private fun StringBuilder.appendItemContext(request: AttributeEnrichmentRequest) {
            appendLine("Item name: ${request.itemName}")
            if (request.existingAttributes.isNotEmpty()) {
                appendLine()
                appendLine("Known attributes:")
                request.existingAttributes.forEach { (key, value) ->
                    appendLine("- $key: $value")
                }
            }
            appendLine()
        }

        private fun StringBuilder.appendTrustedReferenceSites(request: AttributeEnrichmentRequest) {
            if (request.catalogType != CatalogType.PERSON) return
            if (request.targetAttributeType != AttributeType.DATE) return

            appendLine("Reference sites:")
            appendLine(
                "- For date lookups on people, check these trusted sites first: " +
                    PERSON_DATE_REFERENCE_SITES.joinToString(", "),
            )
            appendLine("- Prefer information from these sites over generic search results when available.")
            appendLine()
        }

        private fun StringBuilder.appendSocialMediaDisambiguation(request: AttributeEnrichmentRequest) {
            val profiles =
                request.existingAttributes.filter { (key, value) ->
                    SOCIAL_MEDIA_PATTERNS.any { platform ->
                        key.lowercase().contains(platform) ||
                            value.lowercase().contains(platform)
                    }
                }
            if (profiles.isEmpty()) return

            appendLine("Disambiguation:")
            appendLine(
                "- Use these social media profiles to identify the correct person " +
                    "and avoid confusing them with someone who shares the same name:",
            )
            profiles.forEach { (label, value) ->
                appendLine("  - $label: $value")
            }
            appendLine()
        }

        private fun StringBuilder.appendNationalitySearchHint(request: AttributeEnrichmentRequest) {
            if (request.catalogType != CatalogType.PERSON) return
            val language =
                request.existingAttributes.entries
                    .firstOrNull { (key, _) -> key.lowercase().contains("nationality") }
                    ?.value
                    ?.let { NationalityCodes.primary(it)?.uppercase() }
                    ?.let { COUNTRY_TO_LANGUAGE[it] }
                    ?: return

            appendLine("Search strategy:")
            appendLine(
                "- Also search in $language for more accurate results about less globally-known people.",
            )
            appendLine()
        }

        private fun StringBuilder.appendTargetAttribute(request: AttributeEnrichmentRequest) {
            appendLine("Target attribute: ${request.targetAttributeLabel}")
            appendLine("Expected type: ${formatExpectedType(request.targetAttributeType)}")
            appendLine()
        }

        private fun StringBuilder.appendRules(request: AttributeEnrichmentRequest) {
            appendLine("Rules:")
            appendLine("- Use reliable public internet sources.")
            appendLine("- Do not guess.")
            appendLine("- If no reliable source is found, return NOT_FOUND.")
            appendLine(
                "- If sources conflict, return CONFLICT and populate candidateValues " +
                    "with each distinct value found, linking each to its source title.",
            )
            if (request.targetAttributeType == AttributeType.DATE) {
                appendLine("- Return dates in ISO-8601 format (YYYY-MM-DD).")
            }
            appendLine("- Return a single JSON object only.")
            appendLine()
        }

        private fun StringBuilder.appendResponseFormat() {
            appendLine("Required JSON response format:")
            appendLine(
                """
                {
                  "status": "FOUND | NOT_FOUND | CONFLICT",
                  "value": "the value or null",
                  "displayValue": "human-readable value or null",
                  "confidence": "HIGH | MEDIUM | LOW",
                  "reason": "brief explanation",
                  "sources": [{"title": "...", "url": "...", "snippet": "..."}],
                  "candidateValues": [{"value": "...", "displayValue": "...", "source": "source title"}]
                }
                """.trimIndent(),
            )
        }

        private fun formatExpectedType(type: AttributeType): String =
            when (type) {
                AttributeType.DATE -> "Date in ISO-8601 format (YYYY-MM-DD)"
                AttributeType.NUMBER -> "Numeric value"
                AttributeType.BOOLEAN -> "true or false"
                AttributeType.NATIONALITY ->
                    "ISO 3166-1 alpha-2 country code(s), comma-separated if multiple (e.g., \"BR,IT\")"
                else -> type.name.lowercase()
            }

        private companion object {
            val PERSON_DATE_REFERENCE_SITES = listOf("famousbirthdays.com", "sunoti.com")
            val SOCIAL_MEDIA_PATTERNS =
                listOf(
                    "instagram",
                    "facebook",
                    "twitter",
                    "tiktok",
                    "x.com",
                    "youtube",
                )
            val COUNTRY_TO_LANGUAGE =
                mapOf(
                    "BR" to "Portuguese",
                    "PT" to "Portuguese",
                    "AR" to "Spanish",
                    "MX" to "Spanish",
                    "ES" to "Spanish",
                    "CO" to "Spanish",
                    "CL" to "Spanish",
                    "PE" to "Spanish",
                    "VE" to "Spanish",
                    "EC" to "Spanish",
                    "UY" to "Spanish",
                    "PY" to "Spanish",
                    "BO" to "Spanish",
                    "CR" to "Spanish",
                    "CU" to "Spanish",
                    "DO" to "Spanish",
                    "GT" to "Spanish",
                    "HN" to "Spanish",
                    "NI" to "Spanish",
                    "PA" to "Spanish",
                    "SV" to "Spanish",
                    "FR" to "French",
                    "DE" to "German",
                    "IT" to "Italian",
                    "JP" to "Japanese",
                    "KR" to "Korean",
                    "CN" to "Chinese",
                    "TW" to "Chinese",
                    "RU" to "Russian",
                    "TR" to "Turkish",
                    "TH" to "Thai",
                    "VN" to "Vietnamese",
                    "ID" to "Indonesian",
                    "PL" to "Polish",
                    "NL" to "Dutch",
                    "SE" to "Swedish",
                    "NO" to "Norwegian",
                    "DK" to "Danish",
                    "FI" to "Finnish",
                    "CZ" to "Czech",
                    "RO" to "Romanian",
                    "HU" to "Hungarian",
                    "GR" to "Greek",
                    "IL" to "Hebrew",
                    "SA" to "Arabic",
                    "EG" to "Arabic",
                    "AE" to "Arabic",
                    "IN" to "Hindi",
                    "PH" to "Filipino",
                )
        }
    }
