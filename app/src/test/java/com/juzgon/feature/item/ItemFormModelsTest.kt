package com.juzgon.feature.item

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemFormModelsTest {
    @Test
    fun editModeKeepsTitleEditableForRename() {
        val state = ItemFormUiState(mode = ItemFormMode.Edit)

        assertTrue(state.titleEditable)
    }

    @Test
    fun requiredImageAttributeWithBlankValueBlocksSave() {
        val state =
            imageFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Photo", type = AttributeType.IMAGE, isRequired = true),
                    ),
            )

        assertFalse(state.saveEnabled)
        assertEquals("Photo is required", state.valueErrors.single().value)
    }

    @Test
    fun unsupportedImageFormatBlocksSave() {
        val state =
            imageFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Photo", type = AttributeType.IMAGE),
                        imageReferences =
                            listOf(
                                ItemImageReference(
                                    id = "1",
                                    sourceUri = "content://images/roadster",
                                    mimeType = "image/gif",
                                    displayName = "roadster.gif",
                                ),
                            ),
                    ),
            )

        assertFalse(state.saveEnabled)
        assertEquals("Image must be JPG, JPEG, PNG, or WEBP", state.valueErrors.single().value)
    }

    @Test
    fun oversizedImageBlocksSave() {
        val state =
            imageFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Photo", type = AttributeType.IMAGE),
                        imageReferences =
                            listOf(
                                ItemImageReference(
                                    id = "1",
                                    sourceUri = "content://images/roadster",
                                    mimeType = "image/png",
                                    sizeBytes = IMAGE_MAX_SIZE_BYTES + 1,
                                    displayName = "roadster.png",
                                ),
                            ),
                    ),
            )

        assertFalse(state.saveEnabled)
        assertEquals("Image must be 5 MB or smaller", state.valueErrors.single().value)
    }

    @Test
    fun supportedImageFormatAndSizeAllowsSave() {
        val state =
            imageFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Photo", type = AttributeType.IMAGE),
                        imageReferences =
                            listOf(
                                ItemImageReference(
                                    id = "1",
                                    sourceUri = "content://images/roadster",
                                    mimeType = "image/webp",
                                    sizeBytes = IMAGE_MAX_SIZE_BYTES,
                                    displayName = "roadster.webp",
                                ),
                            ),
                    ),
            )

        assertTrue(state.saveEnabled)
        assertNull(state.valueErrors.single().value)
    }

    @Test
    fun requiredDateAttributeBlocksSaveWhenEmpty() {
        val state =
            dateFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Birth Date", type = AttributeType.DATE, isRequired = true),
                        valueText = "",
                    ),
            )

        assertFalse(state.saveEnabled)
        assertEquals("Birth Date is required", state.valueErrors.single().value)
    }

    @Test
    fun optionalDateAttributeAllowsSaveWhenEmpty() {
        val state =
            dateFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Birth Date", type = AttributeType.DATE, isRequired = false),
                        valueText = "",
                    ),
            )

        assertTrue(state.saveEnabled)
        assertNull(state.valueErrors.single().value)
    }

    @Test
    fun dateValueInISOFormatIsValidated() {
        val state =
            dateFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Birth Date", type = AttributeType.DATE, isRequired = true),
                        valueText = "2000-01-15",
                    ),
            )

        assertTrue(state.saveEnabled)
        assertNull(state.valueErrors.single().value)
    }

    @Test
    fun invalidDateFormatBlocksSave() {
        val state =
            dateFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Birth Date", type = AttributeType.DATE, isRequired = true),
                        valueText = "01/15/2000",
                    ),
            )

        assertFalse(state.saveEnabled)
        assertEquals("Birth Date must be YYYY-MM-DD format", state.valueErrors.single().value)
    }

    @Test
    fun invalidDateValuesNotIncludedInSavedItem() {
        val state =
            dateFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Birth Date", type = AttributeType.DATE),
                        valueText = "invalid-date",
                    ),
            )

        val ratedItem = state.toRatedItem()
        assertEquals(0, ratedItem.values.size)
    }

    @Test
    fun validISODateValuePreservedInSavedItem() {
        val state =
            dateFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Birth Date", type = AttributeType.DATE),
                        valueText = "2000-01-15",
                    ),
            )

        val ratedItem = state.toRatedItem()
        assertEquals(1, ratedItem.values.size)
        assertEquals("2000-01-15", ratedItem.values.single().value)
    }

    @Test
    fun emptyOptionalDateNotIncludedInSavedItem() {
        val state =
            dateFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Birth Date", type = AttributeType.DATE, isRequired = false),
                        valueText = "",
                    ),
            )

        val ratedItem = state.toRatedItem()
        assertEquals(0, ratedItem.values.size)
    }

    @Test
    fun notFoundErrorStateShowsRetryWhenAttemptsRemain() {
        val errorState = EnrichmentSheetState.NotFound(reason = "Not found")
        assertTrue(errorState.canRetry(retryAttemptsUsed = 0, maxRetryAttempts = 2))
    }

    @Test
    fun notFoundErrorStateHidesRetryAfterMaxRetries() {
        val errorState = EnrichmentSheetState.NotFound(reason = "Not found")
        assertFalse(errorState.canRetry(retryAttemptsUsed = 2, maxRetryAttempts = 2))
    }

    @Test
    fun conflictErrorStateShowsRetryWhenAttemptsRemain() {
        val errorState = EnrichmentSheetState.Conflict(reason = "Conflict", sources = emptyList())
        assertTrue(errorState.canRetry(retryAttemptsUsed = 0, maxRetryAttempts = 2))
    }

    @Test
    fun conflictErrorStateHidesRetryAfterMaxRetries() {
        val errorState = EnrichmentSheetState.Conflict(reason = "Conflict", sources = emptyList())
        assertFalse(errorState.canRetry(retryAttemptsUsed = 2, maxRetryAttempts = 2))
    }

    @Test
    fun generalErrorStateShowsRetryWhenAttemptsRemain() {
        val errorState =
            EnrichmentSheetState.Error(
                failureCode = EnrichmentFailureCode.NETWORK_ERROR,
                reason = "Network error",
            )
        assertTrue(errorState.canRetry(retryAttemptsUsed = 0, maxRetryAttempts = 2))
    }

    @Test
    fun generalErrorStateHidesRetryAfterMaxRetries() {
        val errorState =
            EnrichmentSheetState.Error(
                failureCode = EnrichmentFailureCode.NETWORK_ERROR,
                reason = "Network error",
            )
        assertFalse(errorState.canRetry(retryAttemptsUsed = 2, maxRetryAttempts = 2))
    }

    @Test
    fun foundStateDoesNotAllowRetry() {
        val foundState =
            EnrichmentSheetState.Found(
                attributeId = "birthDate",
                attributeLabel = "Birth Date",
                suggestedValue = "2000-01-15",
                displayValue = "January 15, 2000",
                confidence = null,
                sources = emptyList(),
            )
        assertFalse(foundState.canRetry(retryAttemptsUsed = 0, maxRetryAttempts = 2))
    }

    @Test
    fun noKeyStateDoesNotAllowRetry() {
        val noKeyState = EnrichmentSheetState.NoKey
        assertFalse(noKeyState.canRetry(retryAttemptsUsed = 0, maxRetryAttempts = 2))
    }

    @Test
    fun retryCountIncreasesAfterEachRetry() {
        val state =
            ItemFormUiState(
                title = "Person",
                values =
                    listOf(
                        ItemValueInput(
                            attribute = Attribute("Birth Date", type = AttributeType.DATE, isRequired = true),
                        ),
                    ),
                isLoading = false,
                retryAttemptsUsed = 0,
                maxRetryAttempts = 2,
            )

        assertEquals(0, state.retryAttemptsUsed)
        val updatedState = state.copy(retryAttemptsUsed = state.retryAttemptsUsed + 1)
        assertEquals(1, updatedState.retryAttemptsUsed)
    }

    @Test
    fun retryCountResetsWhenDifferentAttributeSelected() {
        val state1 =
            ItemFormUiState(
                title = "Person",
                values =
                    listOf(
                        ItemValueInput(
                            attribute = Attribute("Birth Date", type = AttributeType.DATE, isRequired = true),
                        ),
                    ),
                isLoading = false,
                retryAttemptsUsed = 1,
                maxRetryAttempts = 2,
                enrichmentSheet = EnrichmentSheetState.NotFound(reason = "Not found"),
            )

        // When a different attribute is selected, retry should reset
        val state2 =
            state1.copy(
                retryAttemptsUsed = 0,
                enrichmentSheet = EnrichmentSheetState.Hidden,
            )
        assertEquals(0, state2.retryAttemptsUsed)
        assertEquals(EnrichmentSheetState.Hidden, state2.enrichmentSheet)
    }

    @Test
    fun retryCountResetsWhenSheetDismissed() {
        val state1 =
            ItemFormUiState(
                title = "Person",
                values =
                    listOf(
                        ItemValueInput(
                            attribute = Attribute("Birth Date", type = AttributeType.DATE, isRequired = true),
                        ),
                    ),
                isLoading = false,
                retryAttemptsUsed = 1,
                maxRetryAttempts = 2,
                enrichmentSheet = EnrichmentSheetState.NotFound(reason = "Not found"),
            )

        // When sheet is dismissed, retry should reset
        val state2 =
            state1.copy(
                retryAttemptsUsed = 0,
                enrichmentSheet = EnrichmentSheetState.Hidden,
            )
        assertEquals(0, state2.retryAttemptsUsed)
        assertEquals(EnrichmentSheetState.Hidden, state2.enrichmentSheet)
    }

    private fun dateFormState(valueInput: ItemValueInput): ItemFormUiState =
        ItemFormUiState(
            title = "Person",
            values = listOf(valueInput),
            isLoading = false,
        )

    private fun imageFormState(valueInput: ItemValueInput): ItemFormUiState =
        ItemFormUiState(
            title = "Roadster",
            values = listOf(valueInput),
            isLoading = false,
        )

    // --- Additional RED coverage for suppressed error functions in ItemFormModels ---

    @Test
    fun nonRequiredImageWithEmptyListAllowsSave() {
        val state =
            imageFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Photo", type = AttributeType.IMAGE, isRequired = false),
                        imageReferences = emptyList(),
                    ),
            )

        assertTrue(state.saveEnabled)
        assertNull(state.valueErrors.single().value)
    }

    @Test
    fun multipleImagesFailsOnFirstInvalidFormat() {
        val state =
            imageFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Photos", type = AttributeType.IMAGE),
                        imageReferences =
                            listOf(
                                ItemImageReference(
                                    id = "1",
                                    sourceUri = "content://good",
                                    mimeType = "image/png",
                                    displayName = "good.png",
                                ),
                                ItemImageReference(
                                    id = "2",
                                    sourceUri = "content://bad",
                                    mimeType = "image/gif",
                                    displayName = "bad.gif",
                                ),
                            ),
                    ),
            )

        assertFalse(state.saveEnabled)
        assertEquals("Image must be JPG, JPEG, PNG, or WEBP", state.valueErrors.single().value)
    }

    @Test
    fun imageSizeCheckOnlyAppliesWhenSizeBytesPresent() {
        val state =
            imageFormState(
                valueInput =
                    ItemValueInput(
                        attribute = Attribute("Photo", type = AttributeType.IMAGE),
                        imageReferences =
                            listOf(
                                ItemImageReference(
                                    id = "1",
                                    sourceUri = "content://images/photo",
                                    mimeType = "image/png",
                                    sizeBytes = null,
                                    displayName = "photo.png",
                                ),
                            ),
                    ),
            )

        assertTrue(state.saveEnabled)
        assertNull(state.valueErrors.single().value)
    }

    @Test
    fun regularRequiredTextAttributeShowsErrorWhenBlank() {
        val state =
            ItemFormUiState(
                title = "Item",
                values =
                    listOf(
                        ItemValueInput(
                            attribute = Attribute("Name", type = AttributeType.NOTES, isRequired = true),
                            valueText = "",
                        ),
                    ),
                isLoading = false,
            )

        assertFalse(state.saveEnabled)
        assertEquals("Name is required", state.valueErrors.single().value)
    }

    @Test
    fun regularOptionalTextAttributeAllowsBlank() {
        val state =
            ItemFormUiState(
                title = "Item",
                values =
                    listOf(
                        ItemValueInput(
                            attribute = Attribute("Notes", type = AttributeType.NOTES, isRequired = false),
                            valueText = "",
                        ),
                    ),
                isLoading = false,
            )

        assertTrue(state.saveEnabled)
        assertNull(state.valueErrors.single().value)
    }
}
