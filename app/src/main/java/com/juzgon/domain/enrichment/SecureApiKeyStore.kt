package com.juzgon.domain.enrichment

interface SecureApiKeyStore {
    suspend fun saveGeminiApiKey(apiKey: String)

    suspend fun getGeminiApiKey(): String?

    suspend fun deleteGeminiApiKey()

    suspend fun hasGeminiApiKey(): Boolean
}
