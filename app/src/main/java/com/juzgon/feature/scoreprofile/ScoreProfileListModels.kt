package com.juzgon.feature.scoreprofile

data class ScoreProfileSummary(
    val id: String,
    val name: String,
    val attributeCount: Int,
)

data class ScoreProfileListUiState(
    val categoryName: String = "",
    val profiles: List<ScoreProfileSummary> = emptyList(),
    val isLoading: Boolean = true,
    val showDeleteDialog: Boolean = false,
    val profileToDelete: String? = null,
)
