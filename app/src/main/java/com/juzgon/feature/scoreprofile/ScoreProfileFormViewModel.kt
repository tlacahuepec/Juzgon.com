package com.juzgon.feature.scoreprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import com.juzgon.domain.usecase.ValidateScoreProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ScoreProfileFormViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val scoreProfileRepository: ScoreProfileRepository,
        private val validateScoreProfileUseCase: ValidateScoreProfileUseCase,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ScoreProfileFormUiState())
        val state: StateFlow<ScoreProfileFormUiState> = mutableState

        fun load(
            categoryName: String,
            profileId: String? = null,
        ) {
            val current = mutableState.value
            if (!current.isLoading && current.categoryName == categoryName && current.profileId == profileId) {
                return
            }
            mutableState.value =
                ScoreProfileFormUiState(
                    mode = if (profileId == null) ScoreProfileFormMode.Create else ScoreProfileFormMode.Edit,
                    categoryName = categoryName,
                    profileId = profileId,
                )
            viewModelScope.launch {
                val category = categoryRepository.observeCategory(categoryName).first()
                if (category == null) {
                    mutableState.update { it.copy(isLoading = false, errorMessage = "Category not found") }
                    return@launch
                }

                val rankableAttributes =
                    category.attributes
                        .filter { it.isRankable }
                        .map { attribute ->
                            RankableAttributeCheckbox(
                                id = attribute.id,
                                name = attribute.displayName,
                                type = attribute.type,
                                scoringDirection = attribute.scoringDirection,
                            )
                        }

                if (profileId != null) {
                    val profile = scoreProfileRepository.observeProfile(profileId).first()
                    if (profile != null) {
                        val includedIds = profile.includedAttributeIds.toSet()
                        mutableState.update {
                            it.copy(
                                isLoading = false,
                                profileName = profile.name,
                                attributes =
                                    rankableAttributes.map { attr ->
                                        attr.copy(isSelected = attr.id in includedIds)
                                    },
                            )
                        }
                    } else {
                        mutableState.update { it.copy(isLoading = false, errorMessage = "Profile not found") }
                    }
                } else {
                    mutableState.update {
                        it.copy(isLoading = false, attributes = rankableAttributes)
                    }
                }
            }
        }

        fun onNameChanged(name: String) {
            mutableState.update { it.copy(profileName = name, saveCompleted = false, errorMessage = null) }
        }

        fun onAttributeToggled(
            attributeId: String,
            isSelected: Boolean,
        ) {
            mutableState.update { state ->
                state.copy(
                    attributes =
                        state.attributes.map { attr ->
                            if (attr.id == attributeId) attr.copy(isSelected = isSelected) else attr
                        },
                    saveCompleted = false,
                    errorMessage = null,
                )
            }
        }

        fun onSaveClick() {
            val current = mutableState.value.copy(showValidationErrors = true)
            mutableState.value = current
            if (!current.saveEnabled) return

            viewModelScope.launch {
                mutableState.update { it.copy(isSaving = true, errorMessage = null) }
                runCatching {
                    val profileId = current.profileId ?: UUID.randomUUID().toString()
                    val includedIds = current.attributes.filter { it.isSelected }.map { it.id }
                    val profile =
                        ScoreProfile(
                            id = profileId,
                            categoryName = current.categoryName,
                            name = current.profileName.trim(),
                            includedAttributeIds = includedIds,
                        )
                    val category = categoryRepository.observeCategory(current.categoryName).first()!!
                    val existingProfiles =
                        scoreProfileRepository.observeProfilesForCategory(current.categoryName).first()
                    validateScoreProfileUseCase(
                        profile = profile,
                        existingProfiles = existingProfiles,
                        categoryAttributes = category.attributes,
                    )
                    scoreProfileRepository.saveProfile(profile)
                }.onSuccess {
                    mutableState.update { it.copy(isSaving = false, saveCompleted = true) }
                }.onFailure { error ->
                    mutableState.update {
                        it.copy(isSaving = false, errorMessage = error.message ?: "Unable to save profile")
                    }
                }
            }
        }
    }
