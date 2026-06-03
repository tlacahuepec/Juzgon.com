package com.juzgon.data.enrichment

class FakeGeminiApiClient : GeminiApiClient() {
    var nextResponse: String = ""
    var nextException: Exception? = null
    var lastUseGrounding: Boolean = false

    override suspend fun generateContent(
        apiKey: String,
        prompt: String,
        useGrounding: Boolean,
    ): String {
        lastUseGrounding = useGrounding
        nextException?.let { throw it }
        return nextResponse
    }
}
