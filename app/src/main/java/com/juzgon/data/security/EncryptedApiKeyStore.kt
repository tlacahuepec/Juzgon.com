package com.juzgon.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.juzgon.domain.enrichment.SecureApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedApiKeyStore private constructor(
    private val prefs: Lazy<SharedPreferences>,
) : SecureApiKeyStore {
    constructor(context: Context) : this(
        prefs = lazy { createEncryptedPrefs(context) },
    )

    @VisibleForTesting
    internal constructor(prefs: SharedPreferences) : this(prefs = lazyOf(prefs))

    override suspend fun saveGeminiApiKey(apiKey: String) =
        withContext(Dispatchers.IO) {
            prefs.value
                .edit()
                .putString(KEY_GEMINI_API_KEY, apiKey)
                .apply()
        }

    override suspend fun getGeminiApiKey(): String? =
        withContext(Dispatchers.IO) {
            prefs.value.getString(KEY_GEMINI_API_KEY, null)
        }

    override suspend fun deleteGeminiApiKey() =
        withContext(Dispatchers.IO) {
            prefs.value
                .edit()
                .remove(KEY_GEMINI_API_KEY)
                .apply()
        }

    override suspend fun hasGeminiApiKey(): Boolean =
        withContext(Dispatchers.IO) {
            prefs.value.contains(KEY_GEMINI_API_KEY)
        }

    companion object {
        internal const val PREFS_FILE_NAME = "juzgon_secure_keys"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            return EncryptedSharedPreferences.create(
                PREFS_FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
