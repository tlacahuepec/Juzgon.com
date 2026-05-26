package com.juzgon.data.enrichment

class FakeGeminiApiClient : GeminiApiClient() {
    var nextResponse: String = ""
    var nextException: Exception? = null

    override suspend fun generateContent(
        apiKey: String,
        prompt: String,
    ): String {
        nextException?.let { throw it }
        return nextResponse
    }
}
