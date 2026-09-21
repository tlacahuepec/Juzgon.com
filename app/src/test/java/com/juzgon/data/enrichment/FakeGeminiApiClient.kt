package com.juzgon.data.enrichment

class FakeGeminiApiClient : GeminiApiClient() {
    var nextResponse: String = ""
    var nextGroundingMetadata: GeminiGroundingMetadata? = null
    var nextException: Exception? = null
    var lastUseGrounding: Boolean = false

    override suspend fun generateContentWithMetadata(
        apiKey: String,
        prompt: String,
        useGrounding: Boolean,
    ): GeminiContentResult {
        lastUseGrounding = useGrounding
        nextException?.let { throw it }
        return GeminiContentResult(
            text = nextResponse,
            groundingMetadata = nextGroundingMetadata,
        )
    }

    override suspend fun generateContent(
        apiKey: String,
        prompt: String,
        useGrounding: Boolean,
    ): String = generateContentWithMetadata(apiKey, prompt, useGrounding).text
}
