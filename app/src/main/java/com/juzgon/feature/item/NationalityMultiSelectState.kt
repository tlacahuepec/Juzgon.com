package com.juzgon.feature.item

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.juzgon.domain.NationalityCodes
import com.juzgon.domain.NationalityDataset
import com.juzgon.domain.NationalityOption

private const val MAX_AUTOCOMPLETE_RESULTS = 10

@Stable
class NationalityMultiSelectState(
    initialValue: String,
) {
    var selectedCodes: List<String> by mutableStateOf(NationalityCodes.parse(initialValue))
        private set

    var searchQuery by mutableStateOf("")
        private set

    var expanded by mutableStateOf(false)
        private set

    val suggestions: List<NationalityOption>
        get() {
            val excluded = selectedCodes.toSet()
            val candidates =
                if (searchQuery.isBlank()) {
                    NationalityDataset.all
                } else {
                    NationalityDataset.search(searchQuery.trimStart { it.isWhitespace() || !it.isLetter() })
                }
            return candidates.filter { it.code !in excluded }.take(MAX_AUTOCOMPLETE_RESULTS)
        }

    fun addNationality(
        code: String,
        attributeId: String,
        onValueChange: (String, String) -> Unit,
    ) {
        if (code in selectedCodes) return
        selectedCodes = selectedCodes + code
        searchQuery = ""
        expanded = false
        onValueChange(attributeId, NationalityCodes.encode(selectedCodes))
    }

    fun removeNationality(
        code: String,
        attributeId: String,
        onValueChange: (String, String) -> Unit,
    ) {
        selectedCodes = selectedCodes - code
        onValueChange(attributeId, NationalityCodes.encode(selectedCodes))
    }

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
        expanded = true
    }

    fun onExpandedChange(isExpanded: Boolean) {
        expanded = isExpanded
    }

    fun updateFromExternalValue(newValue: String) {
        val parsed = NationalityCodes.parse(newValue)
        if (parsed != selectedCodes) {
            selectedCodes = parsed
        }
    }
}
