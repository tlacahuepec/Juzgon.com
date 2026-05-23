package com.juzgon.feature.scoreprofile

import com.juzgon.domain.AttributeType
import com.juzgon.domain.ScoringDirection

enum class ScoreProfileFormMode {
    Create,
    Edit,
}

data class RankableAttributeCheckbox(
    val id: String,
    val name: String,
    val type: AttributeType,
    val scoringDirection: ScoringDirection? = null,
    val isSelected: Boolean = false,
)

data class ScoreProfileFormUiState(
    val mode: ScoreProfileFormMode = ScoreProfileFormMode.Create,
    val categoryName: String = "",
    val profileId: String? = null,
    val profileName: String = "",
    val attributes: List<RankableAttributeCheckbox> = emptyList(),
    val showValidationErrors: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val errorMessage: String? = null,
) {
    val nameError: String?
        get() =
            if (showValidationErrors && profileName.isBlank()) "Profile name is required" else null

    val selectionError: String?
        get() =
            if (showValidationErrors && attributes.none { it.isSelected }) {
                "Select at least one attribute"
            } else {
                null
            }

    val selectedCount: Int
        get() = attributes.count { it.isSelected }

    val saveEnabled: Boolean
        get() =
            !isLoading &&
                !isSaving &&
                profileName.isNotBlank() &&
                attributes.any { it.isSelected }

    val screenTitle: String
        get() = if (mode == ScoreProfileFormMode.Create) "Create score profile" else "Edit score profile"
}
