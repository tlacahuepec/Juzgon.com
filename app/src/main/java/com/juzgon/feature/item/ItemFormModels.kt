package com.juzgon.feature.item

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import java.util.Locale

private const val MIN_SCORE = 1
internal const val SCORE_MIN = MIN_SCORE
private const val MAX_SCORE = 10
internal const val SCORE_MAX = MAX_SCORE
internal const val IMAGE_MAX_SIZE_BYTES = 5L * 1024L * 1024L
private const val IMAGE_MAX_SIZE_LABEL = "5 MB"
private val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
private val SUPPORTED_IMAGE_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")

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
    val imageReferences: List<ItemImageReference> = emptyList(),
)

data class ItemValueValidationError(
    val value: String? = null,
)

data class SelectedImageMetadata(
    val sourceUri: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val width: Int?,
    val height: Int?,
    val displayName: String?,
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
    val enrichmentSheet: EnrichmentSheetState = EnrichmentSheetState.Hidden,
    val retryAttemptsUsed: Int = 0,
    val maxRetryAttempts: Int = 2,
) {
    val titleEditable: Boolean
        get() = true

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
                    value = valueInput.valueError(),
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
                    .mapNotNull { valueInput ->
                        when {
                            valueInput.attribute.type == AttributeType.IMAGE -> {
                                val encodedImages = encodeItemImageReferences(valueInput.imageReferences)
                                encodedImages
                                    .takeIf { it.isNotBlank() }
                                    ?.let { value ->
                                        ItemAttributeValue(
                                            attribute = valueInput.attribute,
                                            value = value,
                                        )
                                    }
                            }
                            valueInput.attribute.type == AttributeType.DATE && valueInput.valueText.isNotBlank() -> {
                                if (isValidDateFormat(valueInput.valueText)) {
                                    ItemAttributeValue(
                                        attribute = valueInput.attribute,
                                        value = valueInput.valueText.trim(),
                                    )
                                } else {
                                    null
                                }
                            }
                            valueInput.valueText.isNotBlank() ->
                                ItemAttributeValue(
                                    attribute = valueInput.attribute,
                                    value = valueInput.valueText.trim(),
                                )
                            else -> null
                        }
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

@Suppress("ReturnCount")
private fun ItemValueInput.valueError(): String? {
    if (attribute.type == AttributeType.IMAGE) {
        return imageListError()
    }
    if (attribute.type == AttributeType.DATE) {
        return dateError()
    }
    return if (attribute.isRequired && valueText.isBlank()) {
        "${attribute.displayName} is required"
    } else {
        null
    }
}

@Suppress("ReturnCount")
private fun ItemValueInput.dateError(): String? {
    if (attribute.type != AttributeType.DATE) return null
    if (!attribute.isRequired && valueText.isBlank()) return null
    if (attribute.isRequired && valueText.isBlank()) {
        return "${attribute.displayName} is required"
    }
    return if (valueText.isNotBlank() && !isValidDateFormat(valueText)) {
        "${attribute.displayName} must be YYYY-MM-DD format"
    } else {
        null
    }
}

private fun isValidDateFormat(dateText: String): Boolean {
    val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    return dateRegex.matches(dateText)
}

@Suppress("ReturnCount")
private fun ItemValueInput.imageListError(): String? {
    if (attribute.isRequired && imageReferences.isEmpty()) {
        return "${attribute.displayName} is required"
    }
    imageReferences.forEach { imageReference ->
        when {
            !imageReference.hasSupportedImageFormat() -> return "Image must be JPG, JPEG, PNG, or WEBP"
            imageReference.sizeBytes != null && imageReference.sizeBytes > IMAGE_MAX_SIZE_BYTES ->
                return "Image must be $IMAGE_MAX_SIZE_LABEL or smaller"
        }
    }
    return null
}

private fun ItemImageReference.hasSupportedImageFormat(): Boolean {
    val normalizedMimeType = mimeType?.lowercase(Locale.ROOT)
    if (normalizedMimeType != null) return normalizedMimeType in SUPPORTED_IMAGE_MIME_TYPES

    val extension = imageExtension(displayName) ?: imageExtension(sourceUri)
    return extension == null || extension in SUPPORTED_IMAGE_EXTENSIONS
}

private fun imageExtension(value: String?): String? {
    val fileName =
        value
            ?.substringBefore('?')
            ?.substringBefore('#')
            ?.substringAfterLast('/', missingDelimiterValue = "")
            ?.takeIf { it.contains('.') }
            ?: return null
    return fileName.substringAfterLast('.').lowercase(Locale.ROOT)
}
