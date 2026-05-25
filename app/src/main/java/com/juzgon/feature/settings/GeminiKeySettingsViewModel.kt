package com.juzgon.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.enrichment.SecureApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeminiKeySettingsViewModel
    @Inject
    constructor(
        private val apiKeyStore: SecureApiKeyStore,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(GeminiKeySettingsUiState())
        val state: StateFlow<GeminiKeySettingsUiState> = mutableState

        init {
            loadKeyState()
        }

        fun onKeyInputChanged(key: String) {
            mutableState.update { it.copy(inputKey = key, errorMessage = null) }
        }

        fun onSaveKey() {
            val key = mutableState.value.inputKey.trim()
            if (key.isBlank()) {
                mutableState.update { it.copy(errorMessage = "API key cannot be empty") }
                return
            }
            viewModelScope.launch {
                apiKeyStore.saveGeminiApiKey(key)
                mutableState.value =
                    GeminiKeySettingsUiState(
                        keyState = GeminiKeyState.CONFIGURED,
                        maskedKey = maskKey(key),
                        saveCompleted = true,
                    )
            }
        }

        fun onDeleteKey() {
            viewModelScope.launch {
                apiKeyStore.deleteGeminiApiKey()
                mutableState.value = GeminiKeySettingsUiState(keyState = GeminiKeyState.NO_KEY)
            }
        }

        fun onReplaceKey() {
            mutableState.update {
                it.copy(keyState = GeminiKeyState.ENTERING, inputKey = "", errorMessage = null)
            }
        }

        fun onAddKey() {
            mutableState.update {
                it.copy(keyState = GeminiKeyState.ENTERING, inputKey = "", errorMessage = null)
            }
        }

        private fun loadKeyState() {
            viewModelScope.launch {
                val key = apiKeyStore.getGeminiApiKey()
                mutableState.value =
                    if (key != null) {
                        GeminiKeySettingsUiState(
                            keyState = GeminiKeyState.CONFIGURED,
                            maskedKey = maskKey(key),
                        )
                    } else {
                        GeminiKeySettingsUiState(keyState = GeminiKeyState.NO_KEY)
                    }
            }
        }

        companion object {
            private const val MASK_PREFIX = "••••••••"
            private const val VISIBLE_SUFFIX_LENGTH = 4

            internal fun maskKey(key: String): String =
                if (key.length > VISIBLE_SUFFIX_LENGTH) {
                    MASK_PREFIX + key.takeLast(VISIBLE_SUFFIX_LENGTH)
                } else {
                    MASK_PREFIX
                }
        }
    }
