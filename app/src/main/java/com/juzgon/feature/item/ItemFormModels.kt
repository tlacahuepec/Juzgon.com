package com.juzgon.feature.item

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry

private const val MIN_SCORE = 1
internal const val SCORE_MIN = MIN_SCORE
private const val MAX_SCORE = 10
internal const val SCORE_MAX = MAX_SCORE

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

data class ItemValueInput(
    val attribute: Attribute,
    val valueText: String = if (attribute.type == AttributeType.BOOLEAN) "false" else "",
)

data class ItemValueValidationError(
    val value: String? = null,
)

data class ItemFormUiState(
    val mode: ItemFormMode = ItemFormMode.Create,
    val categoryName: String = "",
    val originalItemId: String? = null,
    val title: String = "",
    val notes: String = "",
    val scores: List<ItemScoreInput> = emptyList(),
    val values: List<ItemValueInput> = emptyList(),
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

    val valueErrors: List<ItemValueValidationError>
        get() =
            values.map { valueInput ->
                ItemValueValidationError(
                    value =
                        if (valueInput.attribute.isRequired && valueInput.valueText.isBlank()) {
                            "${valueInput.attribute.id} is required"
                        } else {
                            null
                        },
                )
            }

    val saveEnabled: Boolean
        get() =
            !isLoading &&
                !isSaving &&
                (scores.isNotEmpty() || values.isNotEmpty()) &&
                titleError == null &&
                scoreErrors.all { it.score == null } &&
                valueErrors.all { it.value == null }

    fun toRatedItem(): RatedItem =
        RatedItem(
            id = title.trim(),
            notes = notes.trim(),
            scores =
                scores
                    .filter { it.scoreText.isNotBlank() }
                    .map { scoreInput ->
                        ScoreEntry(
                            attribute = scoreInput.attribute,
                            score = checkNotNull(scoreInput.scoreText.toIntOrNull()),
                        )
                    },
            values =
                values
                    .filter { it.valueText.isNotBlank() }
                    .map { valueInput ->
                        ItemAttributeValue(
                            attribute = valueInput.attribute,
                            value = valueInput.valueText,
                        )
                    },
        )
}

private fun ItemScoreInput.scoreError(): String? {
    if (!attribute.isRequired && scoreText.isBlank()) return null
    val score = scoreText.toIntOrNull()
    return when {
        scoreText.isBlank() -> "Score is required"
        score == null -> "Score must be a whole number"
        score !in MIN_SCORE..MAX_SCORE -> "Score must be between 1 and 10"
        else -> null
    }
}
