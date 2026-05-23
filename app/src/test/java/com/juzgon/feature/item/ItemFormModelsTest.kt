package com.juzgon.feature.item

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
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

    private fun imageFormState(valueInput: ItemValueInput): ItemFormUiState =
        ItemFormUiState(
            title = "Roadster",
            values = listOf(valueInput),
            isLoading = false,
        )
}
