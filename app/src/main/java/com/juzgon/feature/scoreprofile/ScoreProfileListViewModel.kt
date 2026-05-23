package com.juzgon.feature.scoreprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.repository.ScoreProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScoreProfileListViewModel
    @Inject
    constructor(
        private val scoreProfileRepository: ScoreProfileRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ScoreProfileListUiState())
        val state: StateFlow<ScoreProfileListUiState> = mutableState

        fun load(categoryName: String) {
            mutableState.update { it.copy(categoryName = categoryName) }
            viewModelScope.launch {
                scoreProfileRepository
                    .observeProfilesForCategory(categoryName)
                    .collect { profiles ->
                        mutableState.update { state ->
                            state.copy(
                                isLoading = false,
                                profiles =
                                    profiles.map { profile ->
                                        ScoreProfileSummary(
                                            id = profile.id,
                                            name = profile.name,
                                            attributeCount = profile.includedAttributeIds.size,
                                        )
                                    },
                            )
                        }
                    }
            }
        }

        fun onDeleteRequest(profileId: String) {
            mutableState.update { it.copy(showDeleteDialog = true, profileToDelete = profileId) }
        }

        fun onDeleteConfirmed() {
            val profileId = mutableState.value.profileToDelete ?: return
            mutableState.update { it.copy(showDeleteDialog = false, profileToDelete = null) }
            viewModelScope.launch {
                scoreProfileRepository.deleteProfile(profileId)
            }
        }

        fun onDeleteDismissed() {
            mutableState.update { it.copy(showDeleteDialog = false, profileToDelete = null) }
        }
    }
