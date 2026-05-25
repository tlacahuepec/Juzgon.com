package com.juzgon.data.enrichment

import com.juzgon.domain.AttributeType
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
            appendLine("- If sources conflict, return CONFLICT.")
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
                  "sources": [{"title": "...", "url": "...", "snippet": "..."}]
                }
                """.trimIndent(),
            )
        }

        private fun formatExpectedType(type: AttributeType): String =
            when (type) {
                AttributeType.DATE -> "Date in ISO-8601 format (YYYY-MM-DD)"
                AttributeType.NUMBER -> "Numeric value"
                AttributeType.BOOLEAN -> "true or false"
                else -> type.name.lowercase()
            }
    }
