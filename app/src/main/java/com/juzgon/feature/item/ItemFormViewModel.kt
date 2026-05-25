package com.juzgon.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.data.enrichment.EnrichmentLogger
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentStatus
import com.juzgon.domain.enrichment.usecase.SuggestAttributeValueUseCase
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.usecase.ValidateRatingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class ItemFormViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val ratedItemRepository: RatedItemRepository,
        private val validateRatingsUseCase: ValidateRatingsUseCase,
        private val suggestAttributeValueUseCase: SuggestAttributeValueUseCase,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ItemFormUiState())
        private var loadedCategory: Category? = null

        val state: StateFlow<ItemFormUiState> = mutableState

        fun loadCategory(
            categoryName: String,
            itemId: String? = null,
        ) {
            val current = mutableState.value
            if (!current.isLoading && current.categoryName == categoryName && current.originalItemId == itemId) {
                return
            }

            mutableState.value =
                ItemFormUiState(
                    mode = if (itemId == null) ItemFormMode.Create else ItemFormMode.Edit,
                    categoryName = categoryName,
                    originalItemId = itemId,
                )
            viewModelScope.launch {
                loadFormState(categoryName, itemId)
            }
        }

        private suspend fun loadFormState(
            categoryName: String,
            itemId: String?,
        ) {
            val category = categoryRepository.observeCategory(categoryName).first()
            loadedCategory = category
            when {
                category == null -> showLoadError("Category not found")
                itemId == null ->
                    mutableState.value =
                        ItemFormUiState(
                            categoryName = category.name,
                            scores =
                                category.attributes
                                    .filter { it.type == AttributeType.NUMBER }
                                    .map { ItemScoreInput(it) },
                            values =
                                category.attributes
                                    .filter { it.type != AttributeType.NUMBER }
                                    .map { ItemValueInput(it) },
                            isLoading = false,
                        )
                else -> loadExistingItem(category, itemId)
            }
        }

        private suspend fun loadExistingItem(
            category: Category,
            itemId: String,
        ) {
            val item = ratedItemRepository.observeRatedItem(itemId).first()
            if (item == null) {
                showLoadError("Item not found")
                return
            }

            val numberAttrs = category.attributes.filter { it.type == AttributeType.NUMBER }
            val otherAttrs = category.attributes.filter { it.type != AttributeType.NUMBER }
            val scoresByAttributeId = item.scores.associateBy { score -> score.attribute.id }
            val valuesByAttributeId = item.values.associateBy { value -> value.attribute.id }
            mutableState.value =
                ItemFormUiState(
                    mode = ItemFormMode.Edit,
                    categoryName = category.name,
                    originalItemId = item.id,
                    title = item.id,
                    notes = item.notes,
                    scores =
                        numberAttrs.map { attribute ->
                            ItemScoreInput(
                                attribute = attribute,
                                scoreText = scoresByAttributeId[attribute.id]?.score?.toString().orEmpty(),
                            )
                        },
                    values =
                        otherAttrs.map { attribute ->
                            val existingValue = valuesByAttributeId[attribute.id]?.value
                            val defaultValue = if (attribute.type == AttributeType.BOOLEAN) "false" else ""
                            if (attribute.type == AttributeType.IMAGE) {
                                ItemValueInput(
                                    attribute = attribute,
                                    imageReferences = decodeItemImageReferences(existingValue.orEmpty()),
                                )
                            } else {
                                ItemValueInput(
                                    attribute = attribute,
                                    valueText = existingValue ?: defaultValue,
                                )
                            }
                        },
                    isLoading = false,
                )
        }

        private fun showLoadError(message: String) {
            mutableState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = message,
                    scores = emptyList(),
                    values = emptyList(),
                )
            }
        }

        fun onTitleChanged(title: String) {
            mutableState.update { it.copy(title = title, saveCompleted = false, errorMessage = null) }
        }

        fun onNotesChanged(notes: String) {
            mutableState.update { it.copy(notes = notes, saveCompleted = false, errorMessage = null) }
        }

        fun onScoreChanged(
            attributeId: String,
            scoreText: String,
        ) {
            mutableState.update {
                it.copy(
                    scores =
                        it.scores.map { score ->
                            if (score.attribute.id == attributeId) {
                                score.copy(scoreText = scoreText)
                            } else {
                                score
                            }
                        },
                    saveCompleted = false,
                    errorMessage = null,
                )
            }
        }

        fun onValueChanged(
            attributeId: String,
            valueText: String,
        ) {
            mutableState.update {
                it.copy(
                    values =
                        it.values.map { valueInput ->
                            if (valueInput.attribute.id == attributeId) {
                                valueInput.copy(valueText = valueText)
                            } else {
                                valueInput
                            }
                        },
                    saveCompleted = false,
                    errorMessage = null,
                )
            }
        }

        fun onImagesSelected(
            attributeId: String,
            selectedImages: List<SelectedImageMetadata>,
        ) {
            if (selectedImages.isEmpty()) return
            mutableState.update {
                var duplicateCount = 0
                it.copy(
                    values =
                        it.values.map { valueInput ->
                            if (valueInput.attribute.id == attributeId) {
                                val existingReferences = valueInput.imageReferences.toMutableList()
                                val existingUris =
                                    existingReferences.mapTo(mutableSetOf()) { reference ->
                                        reference.sourceUri
                                    }
                                selectedImages.forEach { selectedImage ->
                                    if (!existingUris.add(selectedImage.sourceUri)) {
                                        duplicateCount += 1
                                        return@forEach
                                    }
                                    existingReferences +=
                                        buildImageReference(
                                            sourceUri = selectedImage.sourceUri,
                                            mimeType = selectedImage.mimeType,
                                            sizeBytes = selectedImage.sizeBytes,
                                            width = selectedImage.width,
                                            height = selectedImage.height,
                                            displayName = selectedImage.displayName,
                                            createdAt = System.currentTimeMillis(),
                                        )
                                }
                                valueInput.copy(imageReferences = existingReferences)
                            } else {
                                valueInput
                            }
                        },
                    saveCompleted = false,
                    errorMessage =
                        if (duplicateCount > 0) {
                            "$duplicateCount duplicate image assignment skipped"
                        } else {
                            null
                        },
                )
            }
        }

        fun onImageSelectionFailed() {
            mutableState.update {
                it.copy(
                    saveCompleted = false,
                    errorMessage = "Unable to keep access to one or more selected images",
                )
            }
        }

        fun onImageRemoved(
            attributeId: String,
            imageId: String,
        ) {
            mutableState.update {
                it.copy(
                    values =
                        it.values.map { valueInput ->
                            if (valueInput.attribute.id == attributeId) {
                                valueInput.copy(
                                    imageReferences =
                                        valueInput.imageReferences.filterNot { imageReference ->
                                            imageReference.id == imageId
                                        },
                                )
                            } else {
                                valueInput
                            }
                        },
                    saveCompleted = false,
                    errorMessage = null,
                )
            }
        }

        fun onScoreIncrement(attributeId: String) {
            adjustScore(attributeId) { current -> (current + 1).coerceAtMost(SCORE_MAX) }
        }

        fun onScoreDecrement(attributeId: String) {
            adjustScore(attributeId) { current -> (current - 1).coerceAtLeast(SCORE_MIN) }
        }

        private fun adjustScore(
            attributeId: String,
            transform: (Int) -> Int,
        ) {
            mutableState.update {
                it.copy(
                    scores =
                        it.scores.map { score ->
                            if (score.attribute.id == attributeId) {
                                val current = score.scoreText.toIntOrNull()
                                val next = if (current == null) SCORE_MIN else transform(current)
                                score.copy(scoreText = next.toString())
                            } else {
                                score
                            }
                        },
                    saveCompleted = false,
                    errorMessage = null,
                )
            }
        }

        fun onDeleteClick() {
            if (mutableState.value.mode != ItemFormMode.Edit) return
            mutableState.update { it.copy(showDeleteDialog = true) }
        }

        fun onDeleteCancel() {
            mutableState.update { it.copy(showDeleteDialog = false) }
        }

        fun onDeleteConfirm() {
            val itemId = mutableState.value.originalItemId ?: return
            viewModelScope.launch {
                ratedItemRepository.deleteRatedItem(itemId)
                mutableState.update { it.copy(showDeleteDialog = false, deleteCompleted = true) }
            }
        }

        fun onSaveClick() {
            val current = mutableState.value.copy(showValidationErrors = true)
            mutableState.value = current

            if (!current.saveEnabled) {
                return
            }

            viewModelScope.launch {
                mutableState.update { it.copy(isSaving = true, errorMessage = null) }
                runCatching {
                    if (current.scores.isNotEmpty()) {
                        validateRatingsUseCase(
                            scoresByAttributeId =
                                current.scores
                                    .filter { it.scoreText.isNotBlank() }
                                    .associate { scoreInput ->
                                        scoreInput.attribute.id to checkNotNull(scoreInput.scoreText.toIntOrNull())
                                    },
                            requiredAttributeBounds =
                                current.scores
                                    .filter { it.attribute.isRequired }
                                    .map { scoreInput ->
                                        ValidateRatingsUseCase.AttributeBounds(scoreInput.attribute.id)
                                    },
                        )
                    }
                    saveCurrentItem(current)
                }.onSuccess {
                    mutableState.update {
                        it.copy(
                            isSaving = false,
                            saveCompleted = true,
                        )
                    }
                }.onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Unable to save item",
                            saveCompleted = false,
                        )
                    }
                }
            }
        }

        private suspend fun saveCurrentItem(current: ItemFormUiState) {
            val ratedItem = current.toRatedItem()
            when (current.mode) {
                ItemFormMode.Create -> {
                    require(ratedItemRepository.observeRatedItem(ratedItem.id).first() == null) {
                        "Item already exists"
                    }
                    ratedItemRepository.saveRatedItem(ratedItem)
                }
                ItemFormMode.Edit -> {
                    val originalId =
                        checkNotNull(current.originalItemId) {
                            "Item not found"
                        }
                    if (originalId == ratedItem.id) {
                        ratedItemRepository.saveRatedItem(ratedItem)
                    } else {
                        require(ratedItemRepository.observeRatedItem(ratedItem.id).first() == null) {
                            "Item already exists"
                        }
                        ratedItemRepository.renameRatedItem(originalId, ratedItem)
                    }
                }
            }
        }

        fun onSuggestClick(attributeId: String) {
            val category = loadedCategory ?: return
            val current = mutableState.value
            val targetAttribute =
                current.values.firstOrNull { it.attribute.id == attributeId }?.attribute ?: return

            mutableState.update { it.copy(enrichmentSheet = EnrichmentSheetState.Loading) }
            viewModelScope.launch {
                val request =
                    AttributeEnrichmentRequest(
                        catalogId = category.name,
                        catalogDescription = category.description,
                        catalogType = category.type,
                        itemId = current.originalItemId ?: current.title.trim(),
                        itemName = current.title.trim(),
                        existingAttributes =
                            current.values
                                .filter { it.valueText.isNotBlank() && it.attribute.id != attributeId }
                                .associate { it.attribute.displayName to it.valueText },
                        targetAttributeKey = attributeId,
                        targetAttributeLabel = targetAttribute.displayName,
                        targetAttributeType = targetAttribute.type,
                    )
                val result = suggestAttributeValueUseCase(request)
                mutableState.update { it.copy(enrichmentSheet = result.toSheetState(attributeId)) }
            }
        }

        fun onSuggestionAccepted() {
            val sheet = mutableState.value.enrichmentSheet
            if (sheet is EnrichmentSheetState.Found) {
                EnrichmentLogger.accepted(
                    attributeKey = sheet.attributeId,
                    itemId = mutableState.value.originalItemId ?: mutableState.value.title.trim(),
                    suggestedValue = sheet.suggestedValue,
                )
                onValueChanged(sheet.attributeId, sheet.suggestedValue)
                mutableState.update { it.copy(enrichmentSheet = EnrichmentSheetState.Hidden) }
            }
        }

        fun onSuggestionDismissed() {
            val sheet = mutableState.value.enrichmentSheet
            if (sheet is EnrichmentSheetState.Found) {
                EnrichmentLogger.dismissed(
                    attributeKey = sheet.attributeId,
                    itemId = mutableState.value.originalItemId ?: mutableState.value.title.trim(),
                )
            }
            mutableState.update { it.copy(enrichmentSheet = EnrichmentSheetState.Hidden) }
        }

        private fun AttributeEnrichmentResult.toSheetState(attributeId: String): EnrichmentSheetState =
            when {
                status == EnrichmentStatus.ERROR &&
                    failureCode == EnrichmentFailureCode.MISSING_API_KEY ->
                    EnrichmentSheetState.NoKey
                status == EnrichmentStatus.FOUND ->
                    EnrichmentSheetState.Found(
                        attributeId = attributeId,
                        suggestedValue = suggestedValue.orEmpty(),
                        displayValue = displayValue,
                        confidence = confidence,
                        sources = sources,
                    )
                status == EnrichmentStatus.NOT_FOUND ->
                    EnrichmentSheetState.NotFound(reason)
                status == EnrichmentStatus.CONFLICT ->
                    EnrichmentSheetState.Conflict(reason, sources)
                else ->
                    EnrichmentSheetState.Error(failureCode, reason)
            }
    }
