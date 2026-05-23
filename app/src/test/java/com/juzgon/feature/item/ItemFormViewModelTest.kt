package com.juzgon.feature.item

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.usecase.ValidateRatingsUseCase
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ItemFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var ratedItemRepository: FakeRatedItemRepository
    private lateinit var viewModel: ItemFormViewModel

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        ratedItemRepository = FakeRatedItemRepository()
        viewModel = ItemFormViewModel(categoryRepository, ratedItemRepository, ValidateRatingsUseCase())
    }

    @Test
    fun loadCategoryCreatesOneScoreInputPerAttribute() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)

            viewModel.loadCategory("Cars")

            assertEquals("Cars", currentState.categoryName)
            assertEquals(false, currentState.isLoading)
            assertEquals(listOf("Speed", "Brakes"), currentState.scores.map { it.attribute.id })
        }

    @Test
    fun editModeLoadsExistingItemValues() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            ratedItemRepository.item.value =
                RatedItem(
                    id = "Roadster",
                    notes = "weekend car",
                    scores =
                        listOf(
                            ScoreEntry(speed, 6),
                            ScoreEntry(brakes, 10),
                        ),
                    createdAt = 100,
                    updatedAt = 200,
                )

            viewModel.loadCategory("Cars", itemId = "Roadster")

            assertEquals(ItemFormMode.Edit, currentState.mode)
            assertEquals("Roadster", currentState.title)
            assertEquals("weekend car", currentState.notes)
            assertEquals(listOf("6", "10"), currentState.scores.map { it.scoreText })
            assertTrue(currentState.saveEnabled)
        }

    @Test
    fun missingTitleAndScoresBlockSave() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")

            viewModel.onSaveClick()

            assertEquals(null, ratedItemRepository.savedItem)
            assertTrue(currentState.showValidationErrors)
            assertFalse(currentState.saveEnabled)
            assertEquals("Title is required", currentState.titleError)
            assertEquals(listOf("Score is required", "Score is required"), currentState.scoreErrors.map { it.score })
        }

    @Test
    fun boundaryScoresOneAndTenAreValid() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")

            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Speed", "1")
            viewModel.onScoreChanged("Brakes", "10")

            assertTrue(currentState.saveEnabled)

            viewModel.onSaveClick()

            assertEquals(
                RatedItem(
                    id = "Roadster",
                    scores =
                        listOf(
                            ScoreEntry(speed, 1),
                            ScoreEntry(brakes, 10),
                        ),
                ),
                ratedItemRepository.savedItem,
            )
        }

    @Test
    fun invalidScoreInputBlocksSave() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")
            viewModel.onTitleChanged("Roadster")

            viewModel.onScoreChanged("Speed", "abc")
            viewModel.onScoreChanged("Brakes", "8")

            assertFalse(currentState.saveEnabled)
            assertEquals("Score must be a whole number", currentState.scoreErrors.first().score)

            viewModel.onScoreChanged("Speed", "11")

            assertFalse(currentState.saveEnabled)
            assertEquals("Score must be between 1 and 10", currentState.scoreErrors.first().score)
            viewModel.onSaveClick()
            assertEquals(null, ratedItemRepository.savedItem)
        }

    @Test
    fun validSaveTrimsTitleAndNotesAndPersistsScores() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")

            viewModel.onTitleChanged("  Roadster  ")
            viewModel.onNotesChanged("  weekend car  ")
            viewModel.onScoreChanged("Speed", "9")
            viewModel.onScoreChanged("Brakes", "8")
            viewModel.onSaveClick()

            assertEquals(
                RatedItem(
                    id = "Roadster",
                    notes = "weekend car",
                    scores =
                        listOf(
                            ScoreEntry(speed, 9),
                            ScoreEntry(brakes, 8),
                        ),
                ),
                ratedItemRepository.savedItem,
            )
            assertTrue(currentState.saveCompleted)
        }

    @Test
    fun editSaveUpdatesExistingItemWithoutDuplicateError() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            ratedItemRepository.item.value =
                RatedItem(
                    id = "Roadster",
                    notes = "old notes",
                    scores =
                        listOf(
                            ScoreEntry(speed, 6),
                            ScoreEntry(brakes, 7),
                        ),
                    createdAt = 100,
                    updatedAt = 200,
                )
            viewModel.loadCategory("Cars", itemId = "Roadster")

            viewModel.onNotesChanged("  track day  ")
            viewModel.onScoreChanged("Speed", "9")
            viewModel.onSaveClick()

            assertEquals(
                RatedItem(
                    id = "Roadster",
                    notes = "track day",
                    scores =
                        listOf(
                            ScoreEntry(speed, 9),
                            ScoreEntry(brakes, 7),
                        ),
                ),
                ratedItemRepository.savedItem,
            )
            assertTrue(currentState.saveCompleted)
        }

    @Test
    fun editModeAllowsTitleChangeForRename() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            ratedItemRepository.item.value =
                RatedItem(id = "Jetta", scores = listOf(ScoreEntry(speed, 6), ScoreEntry(brakes, 7)))
            viewModel.loadCategory("Cars", itemId = "Jetta")

            viewModel.onTitleChanged("GLI")

            assertEquals("GLI", currentState.title)
        }

    @Test
    fun editSaveRenamesItemAndPreservesScoresAndValues() =
        runTest {
            val photo = Attribute("Photo", type = AttributeType.IMAGE, isRequired = false)
            categoryRepository.categories.value = listOf(Category("Cars", attributes = listOf(speed, brakes, photo)))
            ratedItemRepository.itemsById.value =
                mapOf(
                    "Jetta" to
                        RatedItem(
                            id = "Jetta",
                            notes = "daily driver",
                            scores = listOf(ScoreEntry(speed, 6), ScoreEntry(brakes, 7)),
                            values = listOf(ItemAttributeValue(photo, "content://images/jetta")),
                            createdAt = 100,
                            updatedAt = 200,
                        ),
                )
            viewModel.loadCategory("Cars", itemId = "Jetta")
            advanceUntilIdle()

            viewModel.onTitleChanged("GLI")
            viewModel.onSaveClick()
            advanceUntilIdle()

            assertEquals("Jetta", ratedItemRepository.renamedOriginalId)
            val renamedItem = ratedItemRepository.renamedItem
            assertEquals("GLI", renamedItem?.id)
            assertEquals("daily driver", renamedItem?.notes)
            assertEquals(listOf(ScoreEntry(speed, 6), ScoreEntry(brakes, 7)), renamedItem?.scores)
            val persistedImageValue =
                renamedItem
                    ?.values
                    ?.single()
                    ?.value
                    .orEmpty()
            val decodedImageReferences = decodeItemImageReferences(persistedImageValue)
            assertEquals(listOf("content://images/jetta"), decodedImageReferences.map { it.sourceUri })
            assertEquals(null, ratedItemRepository.savedItem)
            assertTrue(currentState.saveCompleted)
        }

    @Test
    fun duplicateRenameShowsErrorAndDoesNotRename() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            ratedItemRepository.itemsById.value =
                mapOf(
                    "Jetta" to RatedItem(id = "Jetta", scores = listOf(ScoreEntry(speed, 6), ScoreEntry(brakes, 7))),
                    "Passat" to RatedItem(id = "Passat", scores = listOf(ScoreEntry(speed, 8), ScoreEntry(brakes, 8))),
                )
            viewModel.loadCategory("Cars", itemId = "Jetta")

            viewModel.onTitleChanged("Passat")
            viewModel.onSaveClick()

            assertEquals("Item already exists", currentState.errorMessage)
            assertEquals(null, ratedItemRepository.renamedItem)
            assertFalse(currentState.saveCompleted)
        }

    @Test
    fun saveFailureShowsErrorAndDoesNotComplete() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            ratedItemRepository.errorOnSave = IllegalStateException("Unable to save item")
            viewModel.loadCategory("Cars")
            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Speed", "9")
            viewModel.onScoreChanged("Brakes", "8")

            viewModel.onSaveClick()

            assertEquals("Unable to save item", currentState.errorMessage)
            assertFalse(currentState.saveCompleted)
        }

    @Test
    fun onScoreIncrement_incrementsByOne() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")
            viewModel.onScoreChanged("Speed", "6")

            viewModel.onScoreIncrement("Speed")

            assertEquals("7", currentState.scores.first { it.attribute.id == "Speed" }.scoreText)
        }

    @Test
    fun onScoreIncrement_clampsAtMaxScore() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")
            viewModel.onScoreChanged("Speed", "10")

            viewModel.onScoreIncrement("Speed")

            assertEquals("10", currentState.scores.first { it.attribute.id == "Speed" }.scoreText)
        }

    @Test
    fun onScoreDecrement_decrementsByOne() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")
            viewModel.onScoreChanged("Speed", "6")

            viewModel.onScoreDecrement("Speed")

            assertEquals("5", currentState.scores.first { it.attribute.id == "Speed" }.scoreText)
        }

    @Test
    fun onScoreDecrement_clampsAtMinScore() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")
            viewModel.onScoreChanged("Speed", "1")

            viewModel.onScoreDecrement("Speed")

            assertEquals("1", currentState.scores.first { it.attribute.id == "Speed" }.scoreText)
        }

    @Test
    fun onScoreIncrement_setsToMinWhenBlank() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")

            viewModel.onScoreIncrement("Speed")

            assertEquals("1", currentState.scores.first { it.attribute.id == "Speed" }.scoreText)
        }

    @Test
    fun onScoreDecrement_setsToMinWhenBlank() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")

            viewModel.onScoreDecrement("Speed")

            assertEquals("1", currentState.scores.first { it.attribute.id == "Speed" }.scoreText)
        }

    @Test
    fun onDeleteClick_opensConfirmationDialog() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            ratedItemRepository.item.value =
                RatedItem(id = "Roadster", scores = listOf(ScoreEntry(speed, 6), ScoreEntry(brakes, 7)))
            viewModel.loadCategory("Cars", itemId = "Roadster")

            viewModel.onDeleteClick()

            assertTrue(currentState.showDeleteDialog)
        }

    @Test
    fun onDeleteCancel_closesDialog() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            ratedItemRepository.item.value =
                RatedItem(id = "Roadster", scores = listOf(ScoreEntry(speed, 6), ScoreEntry(brakes, 7)))
            viewModel.loadCategory("Cars", itemId = "Roadster")
            viewModel.onDeleteClick()

            viewModel.onDeleteCancel()

            assertFalse(currentState.showDeleteDialog)
        }

    @Test
    fun onDeleteConfirm_deletesItemAndCompletes() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            ratedItemRepository.item.value =
                RatedItem(id = "Roadster", scores = listOf(ScoreEntry(speed, 6), ScoreEntry(brakes, 7)))
            viewModel.loadCategory("Cars", itemId = "Roadster")
            viewModel.onDeleteClick()

            viewModel.onDeleteConfirm()

            assertEquals("Roadster", ratedItemRepository.deletedItemId)
            assertTrue(currentState.deleteCompleted)
        }

    @Test
    fun onDeleteClick_isNoOpInCreateMode() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")

            viewModel.onDeleteClick()

            assertFalse(currentState.showDeleteDialog)
        }

    private class FakeCategoryRepository : CategoryRepository {
        val categories = MutableStateFlow(emptyList<Category>())

        override fun observeCategories(): Flow<List<Category>> = categories

        override fun observeCategory(name: String): Flow<Category?> =
            categories.map { categories -> categories.firstOrNull { it.name == name } }

        override suspend fun saveCategory(category: Category) {
            error("ItemFormViewModel does not save categories")
        }

        override suspend fun renameCategory(
            originalName: String,
            category: Category,
            renamedAttributeIds: Map<String, String>,
        ) {
            error("ItemFormViewModel does not rename categories")
        }

        override suspend fun deleteCategory(name: String) {
            error("ItemFormViewModel does not delete categories")
        }
    }

    private class FakeRatedItemRepository : RatedItemRepository {
        val item = MutableStateFlow<RatedItem?>(null)
        val itemsById = MutableStateFlow(emptyMap<String, RatedItem>())
        var savedItem: RatedItem? = null
        var renamedOriginalId: String? = null
        var renamedItem: RatedItem? = null
        var deletedItemId: String? = null
        var errorOnSave: Throwable? = null

        override fun observeRatedItems(): Flow<List<RatedItem>> {
            error("ItemFormViewModel does not observe all rated items")
        }

        override fun observeRatedItem(id: String): Flow<RatedItem?> =
            itemsById.map { items ->
                if (items.isEmpty()) {
                    item.value
                } else {
                    items[id]
                }
            }

        override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> {
            error("ItemFormViewModel does not observe ranked items")
        }

        override suspend fun saveRatedItem(ratedItem: RatedItem) {
            errorOnSave?.let { throw it }
            savedItem = ratedItem
        }

        override suspend fun renameRatedItem(
            originalId: String,
            ratedItem: RatedItem,
        ) {
            renamedOriginalId = originalId
            renamedItem = ratedItem
        }

        override suspend fun deleteRatedItem(id: String) {
            deletedItemId = id
        }
    }

    private val currentState: ItemFormUiState
        get() = viewModel.state.value

    private companion object {
        val speed = Attribute("Speed")
        val brakes = Attribute("Brakes")
        val carsCategory = Category(name = "Cars", attributes = listOf(speed, brakes))
    }

    @Test
    fun loadCategoryCreatesScoreInputsOnlyForNumberAttributes() =
        runTest {
            val colorAttr = Attribute("Color", type = AttributeType.BOOLEAN)
            val detailsAttr = Attribute("Details", type = AttributeType.NOTES)
            val imageAttr = Attribute("Photo", type = AttributeType.IMAGE)
            categoryRepository.categories.value =
                listOf(Category(name = "Mixed", attributes = listOf(speed, colorAttr, detailsAttr, imageAttr)))

            viewModel.loadCategory("Mixed")

            assertEquals(listOf("Speed"), currentState.scores.map { it.attribute.id })
            assertEquals(listOf("Color", "Details", "Photo"), currentState.values.map { it.attribute.id })
        }

    @Test
    fun optionalNumberAttributeWithBlankScoreAllowsSave() =
        runTest {
            val optionalSpeed = Attribute("Speed", isRequired = false)
            val requiredBrakes = Attribute("Brakes", isRequired = true)
            categoryRepository.categories.value =
                listOf(Category("Cars", attributes = listOf(optionalSpeed, requiredBrakes)))
            viewModel.loadCategory("Cars")
            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Brakes", "8")

            assertTrue(currentState.saveEnabled)
        }

    @Test
    fun requiredNonNumberAttributeWithBlankValueBlocksSave() =
        runTest {
            val requiredNotes = Attribute("Details", type = AttributeType.NOTES, isRequired = true)
            categoryRepository.categories.value =
                listOf(Category("Mixed", attributes = listOf(speed, requiredNotes)))
            viewModel.loadCategory("Mixed")
            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Speed", "7")

            assertFalse(currentState.saveEnabled)
        }

    @Test
    fun optionalNonNumberAttributeWithBlankValueAllowsSave() =
        runTest {
            val optionalNotes = Attribute("Details", type = AttributeType.NOTES, isRequired = false)
            categoryRepository.categories.value =
                listOf(Category("Mixed", attributes = listOf(speed, optionalNotes)))
            viewModel.loadCategory("Mixed")
            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Speed", "7")

            assertTrue(currentState.saveEnabled)
        }

    @Test
    fun savePersistsItemWithTypedValues() =
        runTest {
            val notesAttr = Attribute("Details", type = AttributeType.NOTES, isRequired = true)
            categoryRepository.categories.value =
                listOf(Category("Mixed", attributes = listOf(speed, notesAttr)))
            viewModel.loadCategory("Mixed")
            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Speed", "7")
            viewModel.onValueChanged("Details", "Very fast car")

            viewModel.onSaveClick()

            val saved = ratedItemRepository.savedItem!!
            assertEquals(listOf(ScoreEntry(speed, 7)), saved.scores)
            assertEquals(1, saved.values.size)
            assertEquals("Details", saved.values[0].attribute.id)
            assertEquals("Very fast car", saved.values[0].value)
        }

    @Test
    fun onImagesSelectedStoresMetadataAndPersistsImageValue() =
        runTest {
            val imageAttr = Attribute("Photo", type = AttributeType.IMAGE, isRequired = true)
            categoryRepository.categories.value =
                listOf(Category("Mixed", attributes = listOf(speed, imageAttr)))
            viewModel.loadCategory("Mixed")
            advanceUntilIdle()
            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Speed", "7")

            viewModel.onImagesSelected(
                attributeId = "Photo",
                selectedImages =
                    listOf(
                        SelectedImageMetadata(
                            sourceUri = "content://images/roadster",
                            mimeType = "image/png",
                            sizeBytes = IMAGE_MAX_SIZE_BYTES,
                            width = 1200,
                            height = 800,
                            displayName = "roadster.png",
                        ),
                    ),
            )

            val imageInput = currentState.values.single()
            assertEquals(1, imageInput.imageReferences.size)
            assertEquals("content://images/roadster", imageInput.imageReferences.single().sourceUri)
            assertEquals("image/png", imageInput.imageReferences.single().mimeType)
            assertEquals(IMAGE_MAX_SIZE_BYTES, imageInput.imageReferences.single().sizeBytes)
            assertEquals("roadster.png", imageInput.imageReferences.single().displayName)

            viewModel.onSaveClick()
            advanceUntilIdle()

            val saved = ratedItemRepository.savedItem!!
            val savedImage = saved.values.single()
            assertEquals("Photo", savedImage.attribute.id)
            assertTrue(savedImage.value.startsWith("imgref:v1|"))
            assertFalse(savedImage.value.contains("byteArray", ignoreCase = true))
            val decoded = decodeItemImageReferences(savedImage.value)
            assertEquals(1, decoded.size)
            assertEquals("content://images/roadster", decoded.single().sourceUri)
            assertEquals("image/png", decoded.single().mimeType)
        }

    @Test
    fun oversizedSelectedImageBlocksSave() =
        runTest {
            val imageAttr = Attribute("Photo", type = AttributeType.IMAGE, isRequired = true)
            categoryRepository.categories.value =
                listOf(Category("Mixed", attributes = listOf(speed, imageAttr)))
            viewModel.loadCategory("Mixed")
            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Speed", "7")

            viewModel.onImagesSelected(
                attributeId = "Photo",
                selectedImages =
                    listOf(
                        SelectedImageMetadata(
                            sourceUri = "content://images/roadster",
                            mimeType = "image/png",
                            sizeBytes = IMAGE_MAX_SIZE_BYTES + 1,
                            width = null,
                            height = null,
                            displayName = "roadster.png",
                        ),
                    ),
            )

            assertFalse(currentState.saveEnabled)
            assertEquals("Image must be 5 MB or smaller", currentState.valueErrors.single().value)
        }

    @Test
    fun imageSelectionFailureShowsDurableAccessError() =
        runTest {
            val imageAttr = Attribute("Photo", type = AttributeType.IMAGE, isRequired = true)
            categoryRepository.categories.value =
                listOf(Category("Mixed", attributes = listOf(speed, imageAttr)))
            viewModel.loadCategory("Mixed")

            viewModel.onImageSelectionFailed()

            assertEquals("Unable to keep access to one or more selected images", currentState.errorMessage)
            assertFalse(currentState.saveCompleted)
            assertEquals(emptyList<ItemImageReference>(), currentState.values.single().imageReferences)
        }

    @Test
    fun onImageRemovedClearsOptionalImageValueAndMetadata() =
        runTest {
            val imageAttr = Attribute("Photo", type = AttributeType.IMAGE, isRequired = false)
            categoryRepository.categories.value =
                listOf(Category("Mixed", attributes = listOf(speed, imageAttr)))
            viewModel.loadCategory("Mixed")
            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Speed", "7")
            viewModel.onImagesSelected(
                attributeId = "Photo",
                selectedImages =
                    listOf(
                        SelectedImageMetadata(
                            sourceUri = "content://images/roadster",
                            mimeType = "image/png",
                            sizeBytes = IMAGE_MAX_SIZE_BYTES,
                            width = 1200,
                            height = 800,
                            displayName = "roadster.png",
                        ),
                    ),
            )
            val selectedImageId =
                currentState.values
                    .single()
                    .imageReferences
                    .single()
                    .id

            viewModel.onImageRemoved("Photo", selectedImageId)

            val imageInput = currentState.values.single()
            assertEquals(emptyList<ItemImageReference>(), imageInput.imageReferences)
            assertTrue(currentState.saveEnabled)
        }

    @Test
    fun duplicateImageAssignmentIsSkippedWithoutMutatingUnrelatedInputs() =
        runTest {
            val imageAttr = Attribute("Photo", type = AttributeType.IMAGE, isRequired = false)
            val notesAttr = Attribute("Details", type = AttributeType.NOTES, isRequired = false)
            categoryRepository.categories.value =
                listOf(Category("Mixed", attributes = listOf(speed, imageAttr, notesAttr)))
            viewModel.loadCategory("Mixed")
            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Speed", "7")
            viewModel.onValueChanged("Details", "Original note")

            val selectedImage =
                SelectedImageMetadata(
                    sourceUri = "content://images/roadster",
                    mimeType = "image/png",
                    sizeBytes = 42L,
                    width = 10,
                    height = 10,
                    displayName = "roadster.png",
                )
            viewModel.onImagesSelected(attributeId = "Photo", selectedImages = listOf(selectedImage))
            val firstImageId =
                currentState.values
                    .first { it.attribute.id == "Photo" }
                    .imageReferences
                    .single()
                    .id

            viewModel.onImagesSelected(attributeId = "Photo", selectedImages = listOf(selectedImage))

            val imageInput = currentState.values.first { it.attribute.id == "Photo" }
            val notesInput = currentState.values.first { it.attribute.id == "Details" }
            assertEquals(1, imageInput.imageReferences.size)
            assertEquals(firstImageId, imageInput.imageReferences.single().id)
            assertEquals("Original note", notesInput.valueText)
            assertEquals("1 duplicate image assignment skipped", currentState.errorMessage)
        }

    @Test
    fun editModeLoadsExistingScoresAndValues() =
        runTest {
            val notesAttr = Attribute("Details", type = AttributeType.NOTES, isRequired = false)
            categoryRepository.categories.value =
                listOf(Category("Mixed", attributes = listOf(speed, notesAttr)))
            ratedItemRepository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores = listOf(ScoreEntry(speed, 8)),
                    values = listOf(ItemAttributeValue(notesAttr, "Very fast")),
                )

            viewModel.loadCategory("Mixed", itemId = "Roadster")

            assertEquals(listOf("8"), currentState.scores.map { it.scoreText })
            assertEquals(listOf("Very fast"), currentState.values.map { it.valueText })
        }
}
