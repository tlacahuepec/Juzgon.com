package com.juzgon.feature.settings

import com.juzgon.domain.enrichment.SecureApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiKeySettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeStore: FakeSecureApiKeyStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeStore = FakeSecureApiKeyStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_noKey_isNoKey() =
        runTest(testDispatcher) {
            val viewModel = GeminiKeySettingsViewModel(fakeStore)
            advanceUntilIdle()

            assertEquals(GeminiKeyState.NO_KEY, viewModel.state.value.keyState)
            assertNull(viewModel.state.value.maskedKey)
        }

    @Test
    fun initialState_withKey_isConfigured() =
        runTest(testDispatcher) {
            fakeStore.savedKey = "AIzaSyTestKey1234"
            val viewModel = GeminiKeySettingsViewModel(fakeStore)
            advanceUntilIdle()

            assertEquals(GeminiKeyState.CONFIGURED, viewModel.state.value.keyState)
            assertEquals("••••••••1234", viewModel.state.value.maskedKey)
        }

    @Test
    fun onAddKey_transitionsToEntering() =
        runTest(testDispatcher) {
            val viewModel = GeminiKeySettingsViewModel(fakeStore)
            advanceUntilIdle()

            viewModel.onAddKey()

            assertEquals(GeminiKeyState.ENTERING, viewModel.state.value.keyState)
        }

    @Test
    fun onKeyInputChanged_updatesInput() =
        runTest(testDispatcher) {
            val viewModel = GeminiKeySettingsViewModel(fakeStore)
            advanceUntilIdle()

            viewModel.onKeyInputChanged("abc123")

            assertEquals("abc123", viewModel.state.value.inputKey)
        }

    @Test
    fun onSaveKey_withValidKey_savesAndTransitionsToConfigured() =
        runTest(testDispatcher) {
            val viewModel = GeminiKeySettingsViewModel(fakeStore)
            advanceUntilIdle()
            viewModel.onKeyInputChanged("AIzaSyNewKey5678")

            viewModel.onSaveKey()
            advanceUntilIdle()

            assertEquals(GeminiKeyState.CONFIGURED, viewModel.state.value.keyState)
            assertEquals("••••••••5678", viewModel.state.value.maskedKey)
            assertEquals("AIzaSyNewKey5678", fakeStore.savedKey)
        }

    @Test
    fun onSaveKey_withEmptyKey_showsError() =
        runTest(testDispatcher) {
            val viewModel = GeminiKeySettingsViewModel(fakeStore)
            advanceUntilIdle()
            viewModel.onKeyInputChanged("   ")

            viewModel.onSaveKey()

            assertEquals("API key cannot be empty", viewModel.state.value.errorMessage)
        }

    @Test
    fun onDeleteKey_transitionsToNoKey() =
        runTest(testDispatcher) {
            fakeStore.savedKey = "somekey"
            val viewModel = GeminiKeySettingsViewModel(fakeStore)
            advanceUntilIdle()

            viewModel.onDeleteKey()
            advanceUntilIdle()

            assertEquals(GeminiKeyState.NO_KEY, viewModel.state.value.keyState)
            assertNull(fakeStore.savedKey)
        }

    @Test
    fun onReplaceKey_transitionsToEntering() =
        runTest(testDispatcher) {
            fakeStore.savedKey = "somekey"
            val viewModel = GeminiKeySettingsViewModel(fakeStore)
            advanceUntilIdle()

            viewModel.onReplaceKey()

            assertEquals(GeminiKeyState.ENTERING, viewModel.state.value.keyState)
            assertEquals("", viewModel.state.value.inputKey)
        }

    @Test
    fun maskKey_withLongKey_showsLastFourChars() {
        assertEquals("••••••••1234", GeminiKeySettingsViewModel.maskKey("AIzaSyTest1234"))
    }

    @Test
    fun maskKey_withShortKey_showsDotsOnly() {
        assertEquals("••••••••", GeminiKeySettingsViewModel.maskKey("abc"))
    }

    private class FakeSecureApiKeyStore : SecureApiKeyStore {
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
}
