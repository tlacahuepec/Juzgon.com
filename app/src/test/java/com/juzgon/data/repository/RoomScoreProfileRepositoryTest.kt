package com.juzgon.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.domain.Attribute
import com.juzgon.domain.Category
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomScoreProfileRepositoryTest {
    private lateinit var database: JuzgonDatabase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var scoreProfileRepository: ScoreProfileRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, JuzgonDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        categoryRepository = RoomCategoryRepository(database)
        scoreProfileRepository = RoomScoreProfileRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveProfilePersistsAndCanBeObserved() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            scoreProfileRepository.saveProfile(physicalProfile())

            val observed = scoreProfileRepository.observeProfile("p1").first()
            assertEquals("p1", observed?.id)
            assertEquals("Food", observed?.categoryName)
            assertEquals("Physical Only", observed?.name)
            assertEquals(listOf("taste", "texture"), observed?.includedAttributeIds)
        }

    @Test
    fun observeProfilesForCategoryReturnsMatchingProfiles() =
        runTest {
            categoryRepository.saveCategory(foodCategory())
            scoreProfileRepository.saveProfile(physicalProfile())
            scoreProfileRepository.saveProfile(
                ScoreProfile(
                    id = "p2",
                    categoryName = "Food",
                    name = "Service Focus",
                    includedAttributeIds = listOf("service"),
                ),
            )

            val profiles = scoreProfileRepository.observeProfilesForCategory("Food").first()
            assertEquals(2, profiles.size)
            assertEquals("Physical Only", profiles[0].name)
            assertEquals("Service Focus", profiles[1].name)
        }

    @Test
    fun observeProfilesForCategoryReturnsEmptyForUnknownCategory() =
        runTest {
            val profiles = scoreProfileRepository.observeProfilesForCategory("Unknown").first()
            assertEquals(emptyList<ScoreProfile>(), profiles)
        }

    @Test
    fun deleteProfileRemovesProfile() =
        runTest {
            categoryRepository.saveCategory(foodCategory())
            scoreProfileRepository.saveProfile(physicalProfile())

            scoreProfileRepository.deleteProfile("p1")

            val observed = scoreProfileRepository.observeProfile("p1").first()
            assertNull(observed)
        }

    @Test
    fun deletingCategoryCascadesToProfiles() =
        runTest {
            categoryRepository.saveCategory(foodCategory())
            scoreProfileRepository.saveProfile(physicalProfile())

            categoryRepository.deleteCategory("Food")

            val profiles = scoreProfileRepository.observeProfilesForCategory("Food").first()
            assertEquals(emptyList<ScoreProfile>(), profiles)
        }

    @Test
    fun updateProfileChangesNameAndAttributes() =
        runTest {
            categoryRepository.saveCategory(foodCategory())
            scoreProfileRepository.saveProfile(physicalProfile())

            scoreProfileRepository.saveProfile(
                physicalProfile().copy(
                    name = "All Attributes",
                    includedAttributeIds = listOf("taste", "texture", "service"),
                ),
            )

            val observed = scoreProfileRepository.observeProfile("p1").first()
            assertEquals("All Attributes", observed?.name)
            assertEquals(listOf("taste", "texture", "service"), observed?.includedAttributeIds)
        }

    private fun foodCategory(): Category =
        Category(
            name = "Food",
            attributes =
                listOf(
                    Attribute("taste"),
                    Attribute("texture"),
                    Attribute("service"),
                ),
        )

    private fun physicalProfile(): ScoreProfile =
        ScoreProfile(
            id = "p1",
            categoryName = "Food",
            name = "Physical Only",
            includedAttributeIds = listOf("taste", "texture"),
            createdAt = 1000L,
            updatedAt = 2000L,
        )
}
