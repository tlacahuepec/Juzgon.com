package com.juzgon.data.enrichment

class GeminiApiException(
    val httpCode: Int,
    val body: String,
    cause: Throwable? = null,
) : Exception("Gemini API error $httpCode", cause)
