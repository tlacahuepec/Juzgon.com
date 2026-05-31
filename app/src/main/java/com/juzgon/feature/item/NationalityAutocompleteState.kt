package com.juzgon.feature.item

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.juzgon.domain.NationalityDataset
import com.juzgon.domain.NationalityOption

private const val MAX_AUTOCOMPLETE_RESULTS = 10

/**
 * State holder for the nationality autocomplete.
 * Extracted from NationalityAutocompleteField to remove the LongMethod suppression
 * and make the logic testable and reusable.
 */
@Stable
class NationalityAutocompleteState(
    initialValue: String,
) {
    var searchQuery by mutableStateOf(initialDisplay(initialValue))
        private set

    var expanded by mutableStateOf(false)
        private set

    val suggestions: List<NationalityOption>
        get() {
            val resolvedDisplay = resolvedDisplayForCurrentValue()
            return if (searchQuery.isBlank() || resolvedDisplay == searchQuery) {
                NationalityDataset.all.take(MAX_AUTOCOMPLETE_RESULTS)
            } else {
                NationalityDataset
                    .search(searchQuery.trimStart { it.isWhitespace() || !it.isLetter() })
                    .take(MAX_AUTOCOMPLETE_RESULTS)
            }
        }

    private var currentValueCode: String = initialValue

    fun onSearchQueryChange(
        newQuery: String,
        @Suppress("UnusedParameter") onValueSelected: (String) -> Unit,
    ) {
        searchQuery = newQuery
        expanded = true
    }

    fun onOptionSelected(
        option: NationalityOption,
        attributeId: String,
        onValueChange: (String, String) -> Unit,
    ) {
        searchQuery = "${option.flagEmoji} ${option.nationality}"
        currentValueCode = option.code
        onValueChange(attributeId, option.code)
        expanded = false
    }

    fun onExpandedChange(isExpanded: Boolean) {
        expanded = isExpanded
    }

    fun updateFromExternalValue(newValue: String) {
        if (newValue != currentValueCode) {
            currentValueCode = newValue
            searchQuery = initialDisplay(newValue)
        }
    }

    private fun initialDisplay(code: String): String =
        NationalityDataset
            .findByCode(code)
            ?.let { "${it.flagEmoji} ${it.nationality}" }
            ?: code

    private fun resolvedDisplayForCurrentValue(): String? =
        NationalityDataset
            .findByCode(currentValueCode)
            ?.let { "${it.flagEmoji} ${it.nationality}" }
}
