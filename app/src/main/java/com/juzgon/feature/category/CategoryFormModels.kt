package com.juzgon.feature.category

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.ScoringDirection
import java.util.Locale

private const val DEFAULT_WEIGHT = 1.0

enum class CategoryFormMode {
    Create,
    Edit,
}

data class CategoryAttributeInput(
    val key: Long,
    val sourceId: String? = null,
    val name: String = "",
    val weightText: String = "",
    val type: AttributeType = AttributeType.NUMBER,
    val isRequired: Boolean = true,
    val displayInDiamond: Boolean = type == AttributeType.NUMBER,
    val diamondOrderText: String = "",
    val scoringDirection: ScoringDirection? = null,
)

data class CategoryAttributeValidationError(
    val name: String? = null,
    val weight: String? = null,
    val diamondOrder: String? = null,
)

data class CategoryFormUiState(
    val mode: CategoryFormMode = CategoryFormMode.Create,
    val originalName: String? = null,
    val name: String = "",
    val attributes: List<CategoryAttributeInput> = listOf(CategoryAttributeInput(key = 0L)),
    val showValidationErrors: Boolean = false,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val errorMessage: String? = null,
    val showTypeChangeWarning: Boolean = false,
    val pendingTypeChange: AttributeType? = null,
    val pendingTypeChangeKey: Long? = null,
    val showAttributeDeleteWarning: Boolean = false,
    val pendingDeleteKey: Long? = null,
) {
    val nameError: String?
        get() = if (name.isBlank()) "Category name is required" else null

    val attributeErrors: List<CategoryAttributeValidationError>
        get() = CategoryFormValidator.attributeErrors(attributes)

    val formError: String?
        get() = if (attributes.isEmpty()) "Add at least one attribute" else null

    val saveEnabled: Boolean
        get() =
            !isSaving &&
                nameError == null &&
                formError == null &&
                attributeErrors.all { it.name == null && it.weight == null && it.diamondOrder == null }

    fun toCategory(): Category =
        Category(
            name = name.trim(),
            attributes =
                attributes.map { attribute ->
                    Attribute(
                        id = "${name.trim()}/${attribute.name.trim()}",
                        weight = attribute.parsedWeight(),
                        type = attribute.type,
                        isRequired = attribute.isRequired,
                        displayInDiamond = attribute.type == AttributeType.NUMBER && attribute.displayInDiamond,
                        diamondOrder = attribute.parsedDiamondOrder(),
                        scoringDirection =
                            if (attribute.type == AttributeType.DATE) attribute.scoringDirection else null,
                    )
                },
        )
}

object CategoryFormReducer {
    fun createState(): CategoryFormUiState = CategoryFormUiState()

    fun editState(category: Category): CategoryFormUiState =
        CategoryFormUiState(
            mode = CategoryFormMode.Edit,
            originalName = category.name,
            name = category.name,
            attributes =
                category.attributes.mapIndexed { index, attribute ->
                    CategoryAttributeInput(
                        key = index.toLong(),
                        sourceId = attribute.id,
                        name = attribute.displayName,
                        weightText = attribute.weight.toString(),
                        type = attribute.type,
                        isRequired = attribute.isRequired,
                        displayInDiamond = attribute.type == AttributeType.NUMBER && attribute.displayInDiamond,
                        diamondOrderText = attribute.diamondOrder?.toString().orEmpty(),
                        scoringDirection = attribute.scoringDirection,
                    )
                },
        )
}

private object CategoryFormValidator {
    fun attributeErrors(attributes: List<CategoryAttributeInput>): List<CategoryAttributeValidationError> {
        val normalizedCounts =
            attributes
                .map { it.name.trim().lowercase(Locale.ROOT) }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()

        return attributes.map { attribute ->
            val normalizedName = attribute.name.trim().lowercase(Locale.ROOT)
            CategoryAttributeValidationError(
                name =
                    when {
                        attribute.name.isBlank() -> "Attribute name is required"
                        normalizedCounts.getValue(normalizedName) > 1 -> "Attribute names must be unique"
                        else -> null
                    },
                weight =
                    when {
                        attribute.weightText.isBlank() -> null
                        attribute.weightText.toDoubleOrNull() == null -> "Weight must be a number"
                        attribute.weightText.toDouble() <= 0.0 -> "Weight must be greater than 0"
                        else -> null
                    },
                diamondOrder =
                    when {
                        attribute.type != AttributeType.NUMBER -> null
                        attribute.diamondOrderText.isBlank() -> null
                        attribute.diamondOrderText.toIntOrNull() == null -> "Diamond order must be a whole number"
                        attribute.diamondOrderText.toInt() <= 0 -> "Diamond order must be greater than 0"
                        else -> null
                    },
            )
        }
    }
}

private fun CategoryAttributeInput.parsedWeight(): Double =
    weightText
        .takeIf { it.isNotBlank() }
        ?.toDouble()
        ?: DEFAULT_WEIGHT

private fun CategoryAttributeInput.parsedDiamondOrder(): Int? =
    diamondOrderText
        .takeIf { type == AttributeType.NUMBER && it.isNotBlank() }
        ?.toInt()
