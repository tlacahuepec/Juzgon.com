package com.juzgon.domain.enrichment

class FakeSecureApiKeyStore : SecureApiKeyStore {
    var savedKey: String? = null

    override suspend fun saveGeminiApiKey(apiKey: String) {
        savedKey = apiKey
    }

    override suspend fun getGeminiApiKey(): String? = savedKey

    override suspend fun deleteGeminiApiKey() {
        savedKey = null
    }

    override suspend fun hasGeminiApiKey(): Boolean = savedKey != null
}
