package com.juzgon.feature.settings

enum class GeminiKeyState {
    NO_KEY,
    CONFIGURED,
    ENTERING,
}

data class GeminiKeySettingsUiState(
    val keyState: GeminiKeyState = GeminiKeyState.NO_KEY,
    val maskedKey: String? = null,
    val inputKey: String = "",
    val errorMessage: String? = null,
    val saveCompleted: Boolean = false,
)
