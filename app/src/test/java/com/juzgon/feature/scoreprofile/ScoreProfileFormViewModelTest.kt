package com.juzgon.feature.scoreprofile

import app.cash.turbine.test
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.ScoringDirection
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import com.juzgon.domain.usecase.ValidateScoreProfileUseCase
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ScoreProfileFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var scoreProfileRepository: FakeScoreProfileRepository
    private lateinit var viewModel: ScoreProfileFormViewModel

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        scoreProfileRepository = FakeScoreProfileRepository()
        viewModel =
            ScoreProfileFormViewModel(
                categoryRepository,
                scoreProfileRepository,
                ValidateScoreProfileUseCase(),
            )
    }

    @Test
    fun loadCategoryShowsOnlyRankableAttributes() =
        runTest {
            categoryRepository.category.value = foodCategory

            viewModel.state.test {
                awaitItem()

                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(2, state.attributes.size)
                assertEquals("taste", state.attributes[0].id)
                assertEquals("released", state.attributes[1].id)
                assertEquals(AttributeType.NUMBER, state.attributes[0].type)
                assertEquals(AttributeType.DATE, state.attributes[1].type)
                assertEquals(false, state.isLoading)
            }
        }

    @Test
    fun loadCategoryExcludesNonRankableAttributes() =
        runTest {
            categoryRepository.category.value = foodCategory

            viewModel.state.test {
                awaitItem()
                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertTrue(state.attributes.none { it.id == "photo" })
                assertTrue(state.attributes.none { it.id == "nationality" })
            }
        }

    @Test
    fun toggleAttributeUpdatesSelectionState() =
        runTest {
            categoryRepository.category.value = foodCategory

            viewModel.state.test {
                awaitItem()
                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onAttributeToggled("taste", true)
                state = awaitItem()

                assertTrue(state.attributes.first { it.id == "taste" }.isSelected)
                assertEquals(1, state.selectedCount)
            }
        }

    @Test
    fun saveWithValidDataCallsRepository() =
        runTest {
            categoryRepository.category.value = foodCategory

            viewModel.state.test {
                awaitItem()
                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onNameChanged("Physical Only")
                state = awaitItem()
                viewModel.onAttributeToggled("taste", true)
                state = awaitItem()
                viewModel.onSaveClick()
                state = awaitItem()
                if (!state.saveCompleted) state = awaitItem()

                assertTrue(state.saveCompleted)
                assertEquals(1, scoreProfileRepository.savedProfiles.size)
                assertEquals("Physical Only", scoreProfileRepository.savedProfiles[0].name)
                assertEquals(listOf("taste"), scoreProfileRepository.savedProfiles[0].includedAttributeIds)
            }
        }

    @Test
    fun saveWithEmptyNameShowsValidationError() =
        runTest {
            categoryRepository.category.value = foodCategory

            viewModel.state.test {
                awaitItem()
                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onAttributeToggled("taste", true)
                state = awaitItem()
                viewModel.onSaveClick()
                state = awaitItem()

                assertEquals("Profile name is required", state.nameError)
                assertEquals(true, state.showValidationErrors)
                assertEquals(0, scoreProfileRepository.savedProfiles.size)
            }
        }

    @Test
    fun saveWithNoSelectedAttributesShowsError() =
        runTest {
            categoryRepository.category.value = foodCategory

            viewModel.state.test {
                awaitItem()
                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onNameChanged("Test Profile")
                state = awaitItem()
                viewModel.onSaveClick()
                state = awaitItem()

                assertEquals("Select at least one attribute", state.selectionError)
                assertEquals(0, scoreProfileRepository.savedProfiles.size)
            }
        }

    @Test
    fun duplicateNameShowsError() =
        runTest {
            categoryRepository.category.value = foodCategory
            scoreProfileRepository.profiles.value =
                listOf(
                    ScoreProfile(
                        id = "existing",
                        categoryName = "Food",
                        name = "Physical Only",
                        includedAttributeIds = listOf("taste"),
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onNameChanged("Physical Only")
                state = awaitItem()
                viewModel.onAttributeToggled("taste", true)
                state = awaitItem()
                viewModel.onSaveClick()
                state = awaitItem()
                while (state.errorMessage == null && !state.saveCompleted) {
                    state = awaitItem()
                }

                assertTrue(state.errorMessage?.contains("already exists") == true)
                assertEquals(false, state.saveCompleted)
            }
        }

    @Test
    fun editModeLoadsExistingProfileData() =
        runTest {
            categoryRepository.category.value = foodCategory
            scoreProfileRepository.profile.value =
                ScoreProfile(
                    id = "p1",
                    categoryName = "Food",
                    name = "Physical Only",
                    includedAttributeIds = listOf("taste"),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.load("Food", profileId = "p1")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(ScoreProfileFormMode.Edit, state.mode)
                assertEquals("Physical Only", state.profileName)
                assertTrue(state.attributes.first { it.id == "taste" }.isSelected)
                assertTrue(!state.attributes.first { it.id == "released" }.isSelected)
            }
        }

    @Test
    fun missingCategoryShowsError() =
        runTest {
            categoryRepository.category.value = null

            viewModel.state.test {
                awaitItem()
                viewModel.load("Missing")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals("Category not found", state.errorMessage)
                assertNull(state.nameError)
            }
        }

    private class FakeCategoryRepository : CategoryRepository {
        val category = MutableStateFlow<Category?>(null)

        override fun observeCategories(): Flow<List<Category>> = error("Not used")

        override fun observeCategory(name: String): Flow<Category?> = category

        override suspend fun saveCategory(category: Category) = error("Not used")

        override suspend fun renameCategory(
            originalName: String,
            category: Category,
            renamedAttributeIds: Map<String, String>,
        ) = error("Not used")

        override suspend fun deleteCategory(name: String) = error("Not used")
    }

    private class FakeScoreProfileRepository : ScoreProfileRepository {
        val profiles = MutableStateFlow(emptyList<ScoreProfile>())
        val profile = MutableStateFlow<ScoreProfile?>(null)
        val savedProfiles = mutableListOf<ScoreProfile>()

        override fun observeProfilesForCategory(categoryName: String): Flow<List<ScoreProfile>> = profiles

        override fun observeProfile(id: String): Flow<ScoreProfile?> = profile

        override suspend fun saveProfile(profile: ScoreProfile) {
            savedProfiles.add(profile)
        }

        override suspend fun deleteProfile(id: String) = error("Not used")
    }

    private companion object {
        val foodCategory =
            Category(
                name = "Food",
                attributes =
                    listOf(
                        Attribute(id = "taste", type = AttributeType.NUMBER),
                        Attribute(
                            id = "released",
                            type = AttributeType.DATE,
                            scoringDirection = ScoringDirection.NEWER_IS_BETTER,
                        ),
                        Attribute(id = "photo", type = AttributeType.IMAGE, isRequired = false),
                        Attribute(id = "nationality", type = AttributeType.NATIONALITY, isRequired = false),
                    ),
            )
    }
}
