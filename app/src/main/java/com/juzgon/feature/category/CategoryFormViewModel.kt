@file:Suppress("TooManyFunctions")

package com.juzgon.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.AttributeType
import com.juzgon.domain.CatalogType
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
class CategoryFormViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val ratedItemRepository: RatedItemRepository,
        private val validateCategoryUseCase: ValidateCategoryUseCase,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(CategoryFormReducer.createState())
        private val attributesCoordinator = CategoryAttributesCoordinator(ratedItemRepository)

        init {
            // Wire the extracted coordinator for new category path (initial default row + sync).
            // This + the edit path wiring below restores pre-extraction observable behavior
            // expected by the (pre-extraction) characterization tests.
            attributesCoordinator.initializeForNewCategory()
            attributesCoordinator.addAttribute()
            syncAttributesFromCoordinator()
        }

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
                    val editState = CategoryFormReducer.editState(category)
                    mutableState.value = editState

                    // Initialize coordinator (populates dirty keys from ranked items for warning flows)
                    // + the ctor init above for new category.
                    attributesCoordinator.initializeForEdit(category.name, editState.attributes)
                }
            }
        }

        fun onNameChanged(name: String) {
            mutableState.update { it.copy(name = name, saveCompleted = false, errorMessage = null) }
        }

        fun onDescriptionChanged(description: String) {
            mutableState.update { it.copy(description = description, saveCompleted = false, errorMessage = null) }
        }

        fun onCatalogTypeChanged(catalogType: CatalogType?) {
            mutableState.update { it.copy(catalogType = catalogType, saveCompleted = false, errorMessage = null) }
        }

        // --- Attribute operations delegated to coordinator ---

        fun onAttributeNameChanged(
            key: Long,
            name: String,
        ) {
            attributesCoordinator.updateName(key, name)
            syncAttributesFromCoordinator()
        }

        fun onAttributeWeightChanged(
            key: Long,
            weightText: String,
        ) {
            attributesCoordinator.updateWeight(key, weightText)
            syncAttributesFromCoordinator()
        }

        fun onAttributeTypeChanged(
            key: Long,
            type: AttributeType,
        ) {
            when (val result = attributesCoordinator.updateType(key, type)) {
                is CategoryAttributesCoordinator.TypeChangeResult.RequiresConfirmation -> {
                    mutableState.update {
                        it.copy(
                            showTypeChangeWarning = true,
                            pendingTypeChange = result.newType,
                            pendingTypeChangeKey = result.key,
                        )
                    }
                }
                CategoryAttributesCoordinator.TypeChangeResult.Applied -> {
                    syncAttributesFromCoordinator()
                }
            }
        }

        fun onAttributeRequiredChanged(
            key: Long,
            isRequired: Boolean,
        ) {
            attributesCoordinator.updateRequired(key, isRequired)
            syncAttributesFromCoordinator()
        }

        fun onAttributeDisplayInDiamondChanged(
            key: Long,
            displayInDiamond: Boolean,
        ) {
            attributesCoordinator.updateDisplayInDiamond(key, displayInDiamond)
            syncAttributesFromCoordinator()
        }

        fun onAttributeDiamondOrderChanged(
            key: Long,
            diamondOrderText: String,
        ) {
            attributesCoordinator.updateDiamondOrder(key, diamondOrderText)
            syncAttributesFromCoordinator()
        }

        fun onAttributeScoringDirectionChanged(
            key: Long,
            scoringDirection: ScoringDirection?,
        ) {
            attributesCoordinator.updateScoringDirection(key, scoringDirection)
            syncAttributesFromCoordinator()
        }

        fun onTypeChangeConfirmed() {
            attributesCoordinator.confirmTypeChange()
            syncAttributesFromCoordinator()
            mutableState.update {
                it.copy(showTypeChangeWarning = false, pendingTypeChange = null, pendingTypeChangeKey = null)
            }
        }

        fun onTypeChangeDeclined() {
            attributesCoordinator.declineTypeChange()
            mutableState.update {
                it.copy(showTypeChangeWarning = false, pendingTypeChange = null, pendingTypeChangeKey = null)
            }
        }

        fun addAttribute() {
            attributesCoordinator.addAttribute()
            syncAttributesFromCoordinator()
        }

        fun removeAttribute(key: Long) {
            when (val result = attributesCoordinator.removeAttribute(key)) {
                is CategoryAttributesCoordinator.RemoveAttributeResult.RequiresConfirmation -> {
                    mutableState.update {
                        it.copy(showAttributeDeleteWarning = true, pendingDeleteKey = result.key)
                    }
                }
                CategoryAttributesCoordinator.RemoveAttributeResult.Removed -> {
                    syncAttributesFromCoordinator()
                }
            }
        }

        fun onAttributeDeleteConfirmed() {
            if (attributesCoordinator.confirmAttributeDeletion()) {
                syncAttributesFromCoordinator()
            }
            mutableState.update {
                it.copy(showAttributeDeleteWarning = false, pendingDeleteKey = null)
            }
        }

        fun onAttributeDeleteDeclined() {
            attributesCoordinator.declineAttributeDeletion()
            mutableState.update {
                it.copy(showAttributeDeleteWarning = false, pendingDeleteKey = null)
            }
        }

        fun moveAttributeUp(key: Long) {
            attributesCoordinator.moveAttributeUp(key)
            syncAttributesFromCoordinator()
        }

        fun moveAttributeDown(key: Long) {
            attributesCoordinator.moveAttributeDown(key)
            syncAttributesFromCoordinator()
        }

        private fun syncAttributesFromCoordinator() {
            mutableState.update {
                it.copy(attributes = attributesCoordinator.getCurrentAttributes())
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

        // Note: Most attribute logic has been extracted to CategoryAttributesCoordinator
        // for better separation of concerns and to reduce the "TooManyFunctions" smell.
    }
