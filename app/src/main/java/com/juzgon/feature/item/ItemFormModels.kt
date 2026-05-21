package com.juzgon.feature.item

import com.juzgon.domain.Attribute
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry

private const val MIN_SCORE = 1
private const val MAX_SCORE = 10

enum class ItemFormMode {
    Create,
    Edit,
}

data class ItemScoreInput(
    val attribute: Attribute,
    val scoreText: String = "",
)

data class ItemScoreValidationError(
    val score: String? = null,
)

data class ItemFormUiState(
    val mode: ItemFormMode = ItemFormMode.Create,
    val categoryName: String = "",
    val originalItemId: String? = null,
    val title: String = "",
    val notes: String = "",
    val scores: List<ItemScoreInput> = emptyList(),
    val showValidationErrors: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val deleteCompleted: Boolean = false,
    val errorMessage: String? = null,
) {
    val titleEditable: Boolean
        get() = mode == ItemFormMode.Create

    val titleError: String?
        get() = if (title.isBlank()) "Title is required" else null

    val scoreErrors: List<ItemScoreValidationError>
        get() =
            scores.map { scoreInput ->
                ItemScoreValidationError(score = scoreInput.scoreError())
            }

    val saveEnabled: Boolean
        get() =
            !isLoading &&
                !isSaving &&
                scores.isNotEmpty() &&
                titleError == null &&
                scoreErrors.all { it.score == null }

    fun toRatedItem(): RatedItem =
        RatedItem(
            id = title.trim(),
            notes = notes.trim(),
            scores =
                scores.map { scoreInput ->
                    ScoreEntry(
                        attribute = scoreInput.attribute,
                        score = checkNotNull(scoreInput.scoreText.toIntOrNull()),
                    )
                },
        )
}

private fun ItemScoreInput.scoreError(): String? {
    val score = scoreText.toIntOrNull()
    return when {
        scoreText.isBlank() -> "Score is required"
        score == null -> "Score must be a whole number"
        score !in MIN_SCORE..MAX_SCORE -> "Score must be between 1 and 10"
        else -> null
    }
}
