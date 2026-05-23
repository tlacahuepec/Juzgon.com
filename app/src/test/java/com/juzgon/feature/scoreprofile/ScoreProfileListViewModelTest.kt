package com.juzgon.feature.scoreprofile

import app.cash.turbine.test
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.repository.ScoreProfileRepository
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ScoreProfileListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeScoreProfileRepository
    private lateinit var viewModel: ScoreProfileListViewModel

    @Before
    fun setUp() {
        repository = FakeScoreProfileRepository()
        viewModel = ScoreProfileListViewModel(repository)
    }

    @Test
    fun loadCategoryEmitsProfiles() =
        runTest {
            repository.profiles.value =
                listOf(
                    ScoreProfile(
                        id = "p1",
                        categoryName = "Food",
                        name = "Physical Only",
                        includedAttributeIds = listOf("taste", "texture"),
                    ),
                    ScoreProfile(
                        id = "p2",
                        categoryName = "Food",
                        name = "Service Focus",
                        includedAttributeIds = listOf("service"),
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(2, state.profiles.size)
                assertEquals("Physical Only", state.profiles[0].name)
                assertEquals(2, state.profiles[0].attributeCount)
                assertEquals("Service Focus", state.profiles[1].name)
                assertEquals(1, state.profiles[1].attributeCount)
                assertFalse(state.isLoading)
            }
        }

    @Test
    fun emptyProfileListShowsEmptyState() =
        runTest {
            repository.profiles.value = emptyList()

            viewModel.state.test {
                awaitItem()
                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertTrue(state.profiles.isEmpty())
                assertFalse(state.isLoading)
            }
        }

    @Test
    fun deleteRequestShowsConfirmDialog() =
        runTest {
            viewModel.state.test {
                awaitItem()
                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onDeleteRequest("p1")
                state = awaitItem()

                assertTrue(state.showDeleteDialog)
                assertEquals("p1", state.profileToDelete)
            }
        }

    @Test
    fun confirmDeleteCallsRepositoryAndDismisses() =
        runTest {
            viewModel.state.test {
                awaitItem()
                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onDeleteRequest("p1")
                state = awaitItem()
                viewModel.onDeleteConfirmed()
                state = awaitItem()

                assertFalse(state.showDeleteDialog)
                assertNull(state.profileToDelete)
                assertEquals("p1", repository.deletedIds.first())
            }
        }

    @Test
    fun dismissDeleteClearsDialog() =
        runTest {
            viewModel.state.test {
                awaitItem()
                viewModel.load("Food")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onDeleteRequest("p1")
                state = awaitItem()
                viewModel.onDeleteDismissed()
                state = awaitItem()

                assertFalse(state.showDeleteDialog)
                assertNull(state.profileToDelete)
                assertTrue(repository.deletedIds.isEmpty())
            }
        }

    private class FakeScoreProfileRepository : ScoreProfileRepository {
        val profiles = MutableStateFlow(emptyList<ScoreProfile>())
        val deletedIds = mutableListOf<String>()

        override fun observeProfilesForCategory(categoryName: String): Flow<List<ScoreProfile>> = profiles

        override fun observeProfile(id: String): Flow<ScoreProfile?> = error("Not used")

        override suspend fun saveProfile(profile: ScoreProfile) = error("Not used")

        override suspend fun deleteProfile(id: String) {
            deletedIds.add(id)
        }
    }
}
