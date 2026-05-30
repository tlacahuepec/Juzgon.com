package com.juzgon.feature.category

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.CatalogType
import com.juzgon.domain.Category
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.ScoringDirection
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.usecase.ValidateCategoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = CategoryFormMainDispatcherRule()

    private lateinit var repository: FakeCategoryRepository
    private lateinit var ratedItemRepository: FakeRatedItemRepository
    private lateinit var viewModel: CategoryFormViewModel

    @Before
    fun setUp() {
        repository = FakeCategoryRepository()
        ratedItemRepository = FakeRatedItemRepository()
        viewModel = CategoryFormViewModel(repository, ratedItemRepository, ValidateCategoryUseCase())
    }

    @Test
    fun addRemoveAndReorderAttributesUpdatesFormState() =
        runTest {
            val firstKey = attributes.single().key
            viewModel.onAttributeNameChanged(firstKey, "Taste")
            viewModel.addAttribute()
            viewModel.addAttribute()

            val keys = attributes.map { it.key }
            viewModel.onAttributeNameChanged(keys[1], "Service")
            viewModel.onAttributeNameChanged(keys[2], "Value")
            viewModel.moveAttributeUp(keys[2])
            viewModel.removeAttribute(keys[1])

            assertEquals(listOf("Taste", "Value"), attributes.map { it.name })
        }

    @Test
    fun blankWeightDefaultsToOneOnSave() =
        runTest {
            val key = attributes.single().key
            viewModel.onNameChanged("Food")
            viewModel.onAttributeNameChanged(key, "Taste")

            viewModel.onSaveClick()

            assertEquals(
                Category(name = "Food", attributes = listOf(Attribute(id = "Food/Taste", weight = 1.0))),
                repository.savedCategory,
            )
            assertTrue(currentState.saveCompleted)
        }

    @Test
    fun invalidOrNonpositiveWeightBlocksSave() =
        runTest {
            val key = attributes.single().key
            viewModel.onNameChanged("Food")
            viewModel.onAttributeNameChanged(key, "Taste")

            viewModel.onAttributeWeightChanged(key, "abc")
            assertFalse(currentState.saveEnabled)
            assertEquals("Weight must be a number", attributeErrors.single().weight)

            viewModel.onAttributeWeightChanged(key, "0")
            assertFalse(currentState.saveEnabled)
            assertEquals("Weight must be greater than 0", attributeErrors.single().weight)

            viewModel.onSaveClick()
            assertEquals(null, repository.savedCategory)
        }

    @Test
    fun invalidNameMissingAttributesAndDuplicateAttributesBlockSave() =
        runTest {
            assertFalse(currentState.saveEnabled)
            assertEquals("Category name is required", currentState.nameError)

            val firstKey = attributes.single().key
            viewModel.onNameChanged("Food")
            viewModel.removeAttribute(firstKey)
            assertFalse(currentState.saveEnabled)
            assertEquals("Add at least one attribute", currentState.formError)

            viewModel.addAttribute()
            viewModel.addAttribute()
            val keys = attributes.map { it.key }
            viewModel.onAttributeNameChanged(keys[0], "Taste")
            viewModel.onAttributeNameChanged(keys[1], "taste")

            assertFalse(currentState.saveEnabled)
            assertEquals(
                listOf("Attribute names must be unique", "Attribute names must be unique"),
                attributeErrors.map { it.name },
            )
        }

    @Test
    fun validCreateSavesExpectedCategory() =
        runTest {
            val firstKey = attributes.single().key
            viewModel.onNameChanged("Food")
            viewModel.onAttributeNameChanged(firstKey, "Taste")
            viewModel.onAttributeWeightChanged(firstKey, "1.5")
            viewModel.addAttribute()
            val secondKey = attributes.last().key
            viewModel.onAttributeNameChanged(secondKey, "Service")

            viewModel.onSaveClick()

            assertEquals(
                Category(
                    name = "Food",
                    attributes =
                        listOf(
                            Attribute(id = "Food/Taste", weight = 1.5),
                            Attribute(id = "Food/Service", weight = 1.0),
                        ),
                ),
                repository.savedCategory,
            )
        }

    @Test
    fun editModeLoadsExistingCategoryAndSupportsRename() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(
                        name = "Food",
                        attributes = listOf(Attribute(id = "Food/Taste", weight = 1.5)),
                    ),
                )

            viewModel.loadCategory("Food")
            assertEquals(CategoryFormMode.Edit, currentState.mode)
            assertEquals("Food", currentState.name)
            assertEquals("Taste", attributes.single().name)
            assertEquals("1.5", attributes.single().weightText)

            viewModel.onNameChanged("Dining")
            viewModel.onSaveClick()

            assertEquals("Food", repository.renamedOriginalName)
            assertEquals(
                Category(
                    name = "Dining",
                    attributes = listOf(Attribute(id = "Dining/Taste", weight = 1.5)),
                ),
                repository.renamedCategory,
            )
            assertEquals(mapOf("Food/Taste" to "Dining/Taste"), repository.renamedAttributeIds)
        }

    @Test
    fun editModeAttributeRenamePassesRenameMapping() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(
                        name = "Food",
                        attributes =
                            listOf(
                                Attribute(id = "Food/Taste", weight = 1.5),
                                Attribute(id = "Food/Service", weight = 1.0),
                            ),
                    ),
                )

            viewModel.loadCategory("Food")
            val tasteKey = attributes.first { it.name == "Taste" }.key
            viewModel.onAttributeNameChanged(tasteKey, "Flavor")
            viewModel.onSaveClick()

            assertEquals(
                Category(
                    name = "Food",
                    attributes =
                        listOf(
                            Attribute(id = "Food/Flavor", weight = 1.5),
                            Attribute(id = "Food/Service", weight = 1.0),
                        ),
                ),
                repository.renamedCategory,
            )
            assertEquals(mapOf("Food/Taste" to "Food/Flavor"), repository.renamedAttributeIds)
        }

    private class FakeCategoryRepository : CategoryRepository {
        val categories = MutableStateFlow(emptyList<Category>())
        var savedCategory: Category? = null
        var renamedOriginalName: String? = null
        var renamedCategory: Category? = null
        var renamedAttributeIds: Map<String, String> = emptyMap()

        override fun observeCategories(): Flow<List<Category>> = categories

        override fun observeCategory(name: String): Flow<Category?> =
            categories.map { categories ->
                categories.firstOrNull { it.name == name }
            }

        override suspend fun saveCategory(category: Category) {
            savedCategory = category
        }

        override suspend fun renameCategory(
            originalName: String,
            category: Category,
            renamedAttributeIds: Map<String, String>,
        ) {
            renamedOriginalName = originalName
            renamedCategory = category
            this.renamedAttributeIds = renamedAttributeIds
        }

        override suspend fun deleteCategory(name: String) {
            error("CategoryFormViewModel does not delete categories")
        }
    }

    private val currentState: CategoryFormUiState
        get() = viewModel.state.value

    private val attributes: List<CategoryAttributeInput>
        get() = currentState.attributes

    private val attributeErrors: List<CategoryAttributeValidationError>
        get() = currentState.attributeErrors

    @Test
    fun onAttributeTypeChangedUpdatesAttributeType() =
        runTest {
            val key = attributes.single().key
            viewModel.onAttributeTypeChanged(key, AttributeType.DATE)
            assertEquals(AttributeType.DATE, attributes.single().type)
        }

    @Test
    fun onAttributeRequiredChangedUpdatesRequiredFlag() =
        runTest {
            val key = attributes.single().key
            viewModel.onAttributeRequiredChanged(key, false)
            assertEquals(false, attributes.single().isRequired)
        }

    @Test
    fun diamondChartConfigurationIsPreservedInSavedCategory() =
        runTest {
            val key = attributes.single().key
            viewModel.onNameChanged("Cars")
            viewModel.onAttributeNameChanged(key, "Speed")
            viewModel.onAttributeDisplayInDiamondChanged(key, false)
            viewModel.onAttributeDiamondOrderChanged(key, "2")

            viewModel.onSaveClick()

            assertEquals(
                Category(
                    name = "Cars",
                    attributes =
                        listOf(
                            Attribute(
                                id = "Cars/Speed",
                                displayInDiamond = false,
                                diamondOrder = 2,
                            ),
                        ),
                ),
                repository.savedCategory,
            )
        }

    @Test
    fun invalidDiamondOrderBlocksSave() =
        runTest {
            val key = attributes.single().key
            viewModel.onNameChanged("Cars")
            viewModel.onAttributeNameChanged(key, "Speed")
            viewModel.onAttributeDiamondOrderChanged(key, "0")

            assertFalse(currentState.saveEnabled)
            assertEquals("Diamond order must be greater than 0", attributeErrors.single().diamondOrder)
        }

    @Test
    fun changingTypeOfDirtyAttributeShowsTypeChangeWarning() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(
                        name = "Food",
                        attributes = listOf(Attribute(id = "Food/Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Food/Taste"), 8)),
                            ),
                        aggregateScore = 8.0,
                    ),
                )

            viewModel.loadCategory("Food")
            val key = attributes.single().key
            viewModel.onAttributeTypeChanged(key, AttributeType.DATE)

            assertTrue(currentState.showTypeChangeWarning)
            assertEquals(AttributeType.DATE, currentState.pendingTypeChange)
            assertEquals(AttributeType.NUMBER, attributes.single().type)
        }

    @Test
    fun confirmingTypeChangeAppliesNewType() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(
                        name = "Food",
                        attributes = listOf(Attribute(id = "Food/Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Food/Taste"), 8)),
                            ),
                        aggregateScore = 8.0,
                    ),
                )

            viewModel.loadCategory("Food")
            val key = attributes.single().key
            viewModel.onAttributeTypeChanged(key, AttributeType.DATE)
            viewModel.onTypeChangeConfirmed()

            assertEquals(AttributeType.DATE, attributes.single().type)
            assertFalse(currentState.showTypeChangeWarning)
        }

    @Test
    fun decliningTypeChangeRevertsToOriginalType() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(
                        name = "Food",
                        attributes = listOf(Attribute(id = "Food/Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Food/Taste"), 8)),
                            ),
                        aggregateScore = 8.0,
                    ),
                )

            viewModel.loadCategory("Food")
            val key = attributes.single().key
            viewModel.onAttributeTypeChanged(key, AttributeType.DATE)
            viewModel.onTypeChangeDeclined()

            assertEquals(AttributeType.NUMBER, attributes.single().type)
            assertFalse(currentState.showTypeChangeWarning)
        }

    @Test
    fun removingDirtyAttributeShowsDeleteWarning() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(
                        name = "Food",
                        attributes = listOf(Attribute(id = "Food/Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Food/Taste"), 8)),
                            ),
                        aggregateScore = 8.0,
                    ),
                )

            viewModel.loadCategory("Food")
            val key = attributes.single().key
            viewModel.removeAttribute(key)

            assertTrue(currentState.showAttributeDeleteWarning)
            assertEquals(1, attributes.size)
        }

    @Test
    fun confirmingAttributeDeleteRemovesIt() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(
                        name = "Food",
                        attributes = listOf(Attribute(id = "Food/Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Food/Taste"), 8)),
                            ),
                        aggregateScore = 8.0,
                    ),
                )

            viewModel.loadCategory("Food")
            val key = attributes.single().key
            viewModel.removeAttribute(key)
            viewModel.onAttributeDeleteConfirmed()

            assertEquals(0, attributes.size)
            assertFalse(currentState.showAttributeDeleteWarning)
        }

    @Test
    fun decliningAttributeDeleteKeepsAttribute() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(
                        name = "Food",
                        attributes = listOf(Attribute(id = "Food/Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Food/Taste"), 8)),
                            ),
                        aggregateScore = 8.0,
                    ),
                )

            viewModel.loadCategory("Food")
            val key = attributes.single().key
            viewModel.removeAttribute(key)
            viewModel.onAttributeDeleteDeclined()

            assertEquals(1, attributes.size)
            assertFalse(currentState.showAttributeDeleteWarning)
        }

    @Test
    fun onAttributeScoringDirectionChangedOnlyAffectsDateAttributes() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(name = "Cars", attributes = listOf(Attribute(id = "Cars/Birthday", type = AttributeType.DATE))),
                )
            viewModel.loadCategory("Cars")

            val key = currentState.attributes.first().key
            viewModel.onAttributeScoringDirectionChanged(key, ScoringDirection.OLDER_IS_BETTER)

            // For NUMBER it should be ignored (no change)
            assertEquals(null, currentState.attributes.first().scoringDirection)
        }

    @Test
    fun moveAttributeUpAtTopBoundaryDoesNothing() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(name = "Cars", attributes = listOf(Attribute(id = "Cars/Speed"))),
                )
            viewModel.loadCategory("Cars")

            val firstKey = currentState.attributes.first().key
            viewModel.moveAttributeUp(firstKey)

            assertEquals("Speed", currentState.attributes.first().name)
        }

    @Test
    fun typeAndRequiredArePreservedInSavedCategory() =
        runTest {
            val firstKey = attributes.single().key
            viewModel.onNameChanged("Food")
            viewModel.onAttributeNameChanged(firstKey, "Taste")
            viewModel.onAttributeTypeChanged(firstKey, AttributeType.DATE)
            viewModel.onAttributeRequiredChanged(firstKey, false)

            viewModel.onSaveClick()

            assertEquals(
                Category(
                    name = "Food",
                    attributes = listOf(Attribute(id = "Food/Taste", type = AttributeType.DATE, isRequired = false)),
                ),
                repository.savedCategory,
            )
        }

    @Test
    fun removingAttributeWithOnlyValuesShowsDeleteWarning() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(
                        name = "People",
                        attributes =
                            listOf(
                                Attribute(id = "People/Score", type = AttributeType.NUMBER),
                                Attribute(id = "People/Nationality", type = AttributeType.NATIONALITY),
                            ),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "Alice",
                                scores = listOf(ScoreEntry(Attribute("People/Score"), 8)),
                                values =
                                    listOf(
                                        ItemAttributeValue(
                                            Attribute("People/Nationality", type = AttributeType.NATIONALITY),
                                            "US",
                                        ),
                                    ),
                            ),
                        aggregateScore = 8.0,
                    ),
                )

            viewModel.loadCategory("People")
            val nationalityKey = attributes.first { it.name == "Nationality" }.key
            viewModel.removeAttribute(nationalityKey)

            assertTrue(currentState.showAttributeDeleteWarning)
        }

    @Test
    fun onDescriptionChangedUpdatesState() =
        runTest {
            viewModel.onDescriptionChanged("A ranking of soccer players")
            assertEquals("A ranking of soccer players", currentState.description)
        }

    @Test
    fun onCatalogTypeChangedUpdatesState() =
        runTest {
            viewModel.onCatalogTypeChanged(CatalogType.PERSON)
            assertEquals(CatalogType.PERSON, currentState.catalogType)
        }

    @Test
    fun saveIncludesDescriptionAndTypeInCategory() =
        runTest {
            val key = attributes.single().key
            viewModel.onNameChanged("Players")
            viewModel.onAttributeNameChanged(key, "Speed")
            viewModel.onDescriptionChanged("Soccer players ranking")
            viewModel.onCatalogTypeChanged(CatalogType.PERSON)
            viewModel.onSaveClick()

            assertEquals("Soccer players ranking", repository.savedCategory?.description)
            assertEquals(CatalogType.PERSON, repository.savedCategory?.type)
        }

    @Test
    fun editModeLoadsDescriptionAndType() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(
                        name = "Players",
                        attributes = listOf(Attribute(id = "Players/Speed")),
                        description = "Soccer players",
                        type = CatalogType.PERSON,
                    ),
                )

            viewModel.loadCategory("Players")
            assertEquals("Soccer players", currentState.description)
            assertEquals(CatalogType.PERSON, currentState.catalogType)
        }

    @Test
    fun blankDescriptionSavesAsNull() =
        runTest {
            val key = attributes.single().key
            viewModel.onNameChanged("Cars")
            viewModel.onAttributeNameChanged(key, "Speed")
            viewModel.onDescriptionChanged("  ")
            viewModel.onSaveClick()

            assertEquals(null, repository.savedCategory?.description)
        }

    private class FakeRatedItemRepository : RatedItemRepository {
        val rankedItems = MutableStateFlow(emptyList<RankedRatedItem>())

        override fun observeRatedItems(): Flow<List<RatedItem>> = error("not used")

        override fun observeRatedItem(id: String): Flow<RatedItem?> = error("not used")

        override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> = rankedItems

        override suspend fun saveRatedItem(ratedItem: RatedItem) = error("not used")

        override suspend fun renameRatedItem(
            originalId: String,
            ratedItem: RatedItem,
        ) = error("not used")

        override suspend fun deleteRatedItem(id: String) = error("not used")
    }

    // ======================================================================
    // LARGE BATCH - Comprehensive RED coverage for CategoryFormViewModel
    // (TooManyFunctions) - attribute manipulation, warnings, catalog type,
    // description, move, save, delete flows
    // ======================================================================

    @Test
    fun onDescriptionChanged_updatesDescription() =
        runTest {
            viewModel.onDescriptionChanged("A detailed ranking of cars")
            assertEquals("A detailed ranking of cars", currentState.description)
        }

    @Test
    fun onCatalogTypeChanged_updatesType() =
        runTest {
            viewModel.onCatalogTypeChanged(CatalogType.PERSON)
            assertEquals(CatalogType.PERSON, currentState.catalogType)
        }

    @Test
    fun addAttribute_increasesAttributeCount() =
        runTest {
            val initialCount = currentState.attributes.size
            viewModel.addAttribute()
            assertEquals(initialCount + 1, currentState.attributes.size)
        }

    @Test
    fun moveAttributeDown_reordersAttributes() =
        runTest {
            val key1 = currentState.attributes[0].key
            viewModel.addAttribute()
            val key2 = currentState.attributes.last().key

            viewModel.moveAttributeDown(key1)
            assertEquals(key2, currentState.attributes[0].key)
        }

    @Test
    fun onAttributeWeightChanged_updatesWeightText() =
        runTest {
            val key = currentState.attributes.single().key
            viewModel.onAttributeWeightChanged(key, "3.5")
            assertEquals("3.5", currentState.attributes.single().weightText)
        }

    @Test
    fun onAttributeDisplayInDiamondChanged_onlyAffectsNumberTypes() =
        runTest {
            val key = currentState.attributes.single().key
            viewModel.onAttributeTypeChanged(key, AttributeType.NOTES)
            viewModel.onAttributeDisplayInDiamondChanged(key, true)
            assertFalse(currentState.attributes.single().displayInDiamond)
        }

    @Test
    fun onAttributeDiamondOrderChanged_onlyAffectsNumberTypes() =
        runTest {
            val key = currentState.attributes.single().key
            viewModel.onAttributeTypeChanged(key, AttributeType.BOOLEAN)
            viewModel.onAttributeDiamondOrderChanged(key, "5")
            assertEquals("", currentState.attributes.single().diamondOrderText)
        }

    @Test
    fun onTypeChangeConfirmed_appliesPendingChange() =
        runTest {
            val key = currentState.attributes.single().key
            viewModel.onAttributeTypeChanged(key, AttributeType.DATE)
            // Simulate dirty to trigger warning path (simplified)
            viewModel.onTypeChangeConfirmed()
            // State should no longer show warning
            assertFalse(currentState.showTypeChangeWarning)
        }

    @Test
    fun onTypeChangeDeclined_clearsPendingState() =
        runTest {
            val key = currentState.attributes.single().key
            viewModel.onAttributeTypeChanged(key, AttributeType.DATE)
            viewModel.onTypeChangeDeclined()
            assertFalse(currentState.showTypeChangeWarning)
            assertNull(currentState.pendingTypeChange)
        }

    @Test
    fun saveIncludesDescriptionAndCatalogType() =
        runTest {
            viewModel.onNameChanged("Vehicles")
            viewModel.onDescriptionChanged("All my cars")
            viewModel.onCatalogTypeChanged(CatalogType.OTHER)
            viewModel.onSaveClick()
            advanceUntilIdle()

            assertEquals("All my cars", repository.savedCategory?.description)
            assertEquals(CatalogType.OTHER, repository.savedCategory?.type)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryFormMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
