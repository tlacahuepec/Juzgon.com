package com.juzgon.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncryptedApiKeyStoreTest {
    private lateinit var store: EncryptedApiKeyStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_secure_keys", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        store = EncryptedApiKeyStore(prefs)
    }

    @Test
    fun hasGeminiApiKey_initially_returnsFalse() =
        runTest {
            assertFalse(store.hasGeminiApiKey())
        }

    @Test
    fun getGeminiApiKey_initially_returnsNull() =
        runTest {
            assertNull(store.getGeminiApiKey())
        }

    @Test
    fun saveAndGet_roundtrip_returnsKey() =
        runTest {
            store.saveGeminiApiKey("AIzaSyTestKey123")

            assertEquals("AIzaSyTestKey123", store.getGeminiApiKey())
        }

    @Test
    fun hasGeminiApiKey_afterSave_returnsTrue() =
        runTest {
            store.saveGeminiApiKey("somekey")

            assertTrue(store.hasGeminiApiKey())
        }

    @Test
    fun deleteGeminiApiKey_removesKey() =
        runTest {
            store.saveGeminiApiKey("somekey")
            store.deleteGeminiApiKey()

            assertFalse(store.hasGeminiApiKey())
            assertNull(store.getGeminiApiKey())
        }

    @Test
    fun saveGeminiApiKey_overwritesPreviousKey() =
        runTest {
            store.saveGeminiApiKey("first")
            store.saveGeminiApiKey("second")

            assertEquals("second", store.getGeminiApiKey())
        }
}
