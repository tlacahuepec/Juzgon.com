package com.juzgon.feature.category

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
                Category(name = "Food", attributes = listOf(Attribute(id = "Taste", weight = 1.0))),
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
                            Attribute(id = "Taste", weight = 1.5),
                            Attribute(id = "Service", weight = 1.0),
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
                        attributes = listOf(Attribute(id = "Taste", weight = 1.5)),
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
                    attributes = listOf(Attribute(id = "Taste", weight = 1.5)),
                ),
                repository.renamedCategory,
            )
        }

    private class FakeCategoryRepository : CategoryRepository {
        val categories = MutableStateFlow(emptyList<Category>())
        var savedCategory: Category? = null
        var renamedOriginalName: String? = null
        var renamedCategory: Category? = null

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
        ) {
            renamedOriginalName = originalName
            renamedCategory = category
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
    fun changingTypeOfDirtyAttributeShowsTypeChangeWarning() =
        runTest {
            repository.categories.value =
                listOf(
                    Category(
                        name = "Food",
                        attributes = listOf(Attribute(id = "Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Taste"), 8)),
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
                        attributes = listOf(Attribute(id = "Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Taste"), 8)),
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
                        attributes = listOf(Attribute(id = "Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Taste"), 8)),
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
                        attributes = listOf(Attribute(id = "Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Taste"), 8)),
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
                        attributes = listOf(Attribute(id = "Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Taste"), 8)),
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
                        attributes = listOf(Attribute(id = "Taste", type = AttributeType.NUMBER)),
                    ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "espresso",
                                scores = listOf(ScoreEntry(Attribute("Taste"), 8)),
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
                    attributes = listOf(Attribute(id = "Taste", type = AttributeType.DATE, isRequired = false)),
                ),
                repository.savedCategory,
            )
        }

    private class FakeRatedItemRepository : RatedItemRepository {
        val rankedItems = MutableStateFlow(emptyList<RankedRatedItem>())

        override fun observeRatedItems(): Flow<List<RatedItem>> = error("not used")

        override fun observeRatedItem(id: String): Flow<RatedItem?> = error("not used")

        override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> = rankedItems

        override suspend fun saveRatedItem(ratedItem: RatedItem) = error("not used")

        override suspend fun deleteRatedItem(id: String) = error("not used")
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
