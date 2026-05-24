package com.juzgon.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.AttributeType
import com.juzgon.domain.ScoringDirection
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.usecase.ValidateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class CategoryFormViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val ratedItemRepository: RatedItemRepository,
        private val validateCategoryUseCase: ValidateCategoryUseCase,
    ) : ViewModel() {
        private var nextAttributeKey = 1L
        private val mutableState = MutableStateFlow(CategoryFormReducer.createState())
        private var dirtyAttributeKeys = emptySet<Long>()

        val state: StateFlow<CategoryFormUiState> = mutableState

        fun loadCategory(name: String) {
            val current = mutableState.value
            if (current.mode == CategoryFormMode.Edit && current.originalName == name) {
                return
            }

            viewModelScope.launch {
                val category = categoryRepository.observeCategory(name).first()
                if (category == null) {
                    mutableState.update {
                        it.copy(
                            mode = CategoryFormMode.Edit,
                            originalName = name,
                            name = name,
                            attributes = emptyList(),
                            errorMessage = "Category not found",
                        )
                    }
                } else {
                    nextAttributeKey = category.attributes.size.toLong()
                    val editState = CategoryFormReducer.editState(category)
                    mutableState.value = editState

                    val dirtyAttributeIds =
                        ratedItemRepository
                            .observeRankedItems(name)
                            .first()
                            .flatMap { it.item.scores }
                            .map { it.attribute.id }
                            .toSet()

                    dirtyAttributeKeys =
                        editState.attributes
                            .filter { it.sourceId in dirtyAttributeIds }
                            .map { it.key }
                            .toSet()
                }
            }
        }

        fun onNameChanged(name: String) {
            mutableState.update { it.copy(name = name, saveCompleted = false, errorMessage = null) }
        }

        fun onAttributeNameChanged(
            key: Long,
            name: String,
        ) {
            updateAttribute(key) { it.copy(name = name) }
        }

        fun onAttributeWeightChanged(
            key: Long,
            weightText: String,
        ) {
            updateAttribute(key) { it.copy(weightText = weightText) }
        }

        fun onAttributeTypeChanged(
            key: Long,
            type: AttributeType,
        ) {
            if (key in dirtyAttributeKeys) {
                mutableState.update {
                    it.copy(
                        showTypeChangeWarning = true,
                        pendingTypeChange = type,
                        pendingTypeChangeKey = key,
                    )
                }
            } else {
                updateAttribute(key) { it.withType(type) }
            }
        }

        fun onAttributeRequiredChanged(
            key: Long,
            isRequired: Boolean,
        ) {
            updateAttribute(key) { it.copy(isRequired = isRequired) }
        }

        fun onAttributeDisplayInDiamondChanged(
            key: Long,
            displayInDiamond: Boolean,
        ) {
            updateAttribute(key) { attribute ->
                if (attribute.type == AttributeType.NUMBER) {
                    attribute.copy(displayInDiamond = displayInDiamond)
                } else {
                    attribute
                }
            }
        }

        fun onAttributeDiamondOrderChanged(
            key: Long,
            diamondOrderText: String,
        ) {
            updateAttribute(key) { attribute ->
                if (attribute.type == AttributeType.NUMBER) {
                    attribute.copy(diamondOrderText = diamondOrderText)
                } else {
                    attribute
                }
            }
        }

        fun onAttributeScoringDirectionChanged(
            key: Long,
            scoringDirection: ScoringDirection?,
        ) {
            updateAttribute(key) { attribute ->
                if (attribute.type == AttributeType.DATE) {
                    attribute.copy(scoringDirection = scoringDirection)
                } else {
                    attribute
                }
            }
        }

        fun onTypeChangeConfirmed() {
            val state = mutableState.value
            val key = state.pendingTypeChangeKey ?: return
            val type = state.pendingTypeChange ?: return
            updateAttribute(key) { it.withType(type) }
            mutableState.update {
                it.copy(
                    showTypeChangeWarning = false,
                    pendingTypeChange = null,
                    pendingTypeChangeKey = null,
                )
            }
        }

        fun onTypeChangeDeclined() {
            mutableState.update {
                it.copy(
                    showTypeChangeWarning = false,
                    pendingTypeChange = null,
                    pendingTypeChangeKey = null,
                )
            }
        }

        fun addAttribute() {
            val attribute = CategoryAttributeInput(key = nextAttributeKey++)
            mutableState.update {
                it.copy(
                    attributes = it.attributes + attribute,
                    saveCompleted = false,
                    errorMessage = null,
                )
            }
        }

        fun removeAttribute(key: Long) {
            if (key in dirtyAttributeKeys) {
                mutableState.update {
                    it.copy(showAttributeDeleteWarning = true, pendingDeleteKey = key)
                }
            } else {
                doRemoveAttribute(key)
            }
        }

        fun onAttributeDeleteConfirmed() {
            val key = mutableState.value.pendingDeleteKey ?: return
            dirtyAttributeKeys = dirtyAttributeKeys - key
            doRemoveAttribute(key)
            mutableState.update { it.copy(showAttributeDeleteWarning = false, pendingDeleteKey = null) }
        }

        fun onAttributeDeleteDeclined() {
            mutableState.update { it.copy(showAttributeDeleteWarning = false, pendingDeleteKey = null) }
        }

        fun moveAttributeUp(key: Long) {
            moveAttribute(key = key, offset = -1)
        }

        fun moveAttributeDown(key: Long) {
            moveAttribute(key = key, offset = 1)
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
                    val category = current.toCategory()
                    val renamedAttributeIds =
                        current.attributes
                            .mapNotNull { attribute ->
                                val originalId = attribute.sourceId ?: return@mapNotNull null
                                val updatedId = "${current.name.trim()}/${attribute.name.trim()}"
                                if (originalId == updatedId) {
                                    null
                                } else {
                                    originalId to updatedId
                                }
                            }.toMap()
                    validateCategoryUseCase(category)
                    val originalName = current.originalName
                    if (originalName != null) {
                        categoryRepository.renameCategory(
                            originalName = originalName,
                            category = category,
                            renamedAttributeIds = renamedAttributeIds,
                        )
                    } else {
                        categoryRepository.saveCategory(category)
                    }
                }.onSuccess {
                    mutableState.update {
                        it.copy(
                            isSaving = false,
                            saveCompleted = true,
                            originalName = it.name.trim(),
                            mode = CategoryFormMode.Edit,
                        )
                    }
                }.onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Unable to save category",
                        )
                    }
                }
            }
        }

        private fun doRemoveAttribute(key: Long) {
            mutableState.update {
                it.copy(
                    attributes = it.attributes.filterNot { attribute -> attribute.key == key },
                    saveCompleted = false,
                    errorMessage = null,
                )
            }
        }

        private fun updateAttribute(
            key: Long,
            transform: (CategoryAttributeInput) -> CategoryAttributeInput,
        ) {
            mutableState.update {
                it.copy(
                    attributes =
                        it.attributes.map { attribute ->
                            if (attribute.key == key) transform(attribute) else attribute
                        },
                    saveCompleted = false,
                    errorMessage = null,
                )
            }
        }

        private fun CategoryAttributeInput.withType(type: AttributeType): CategoryAttributeInput =
            copy(
                type = type,
                displayInDiamond = type == AttributeType.NUMBER && displayInDiamond,
                diamondOrderText = if (type == AttributeType.NUMBER) diamondOrderText else "",
                scoringDirection = if (type == AttributeType.DATE) scoringDirection else null,
            )

        private fun moveAttribute(
            key: Long,
            offset: Int,
        ) {
            mutableState.update { state ->
                val fromIndex = state.attributes.indexOfFirst { it.key == key }
                val toIndex = fromIndex + offset
                if (fromIndex == -1 || toIndex !in state.attributes.indices) {
                    state
                } else {
                    val reordered = state.attributes.toMutableList()
                    val attribute = reordered.removeAt(fromIndex)
                    reordered.add(toIndex, attribute)
                    state.copy(
                        attributes = reordered,
                        saveCompleted = false,
                        errorMessage = null,
                    )
                }
            }
        }
    }
