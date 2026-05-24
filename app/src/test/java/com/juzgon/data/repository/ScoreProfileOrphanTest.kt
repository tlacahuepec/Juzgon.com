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
class ScoreProfileOrphanTest {
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
    fun `profile with no remaining attributes is excluded from category observation`() =
        runTest {
            categoryRepository.saveCategory(threeAttributeCategory())
            scoreProfileRepository.saveProfile(profileUsingAllAttributes())

            categoryRepository.saveCategory(Category(name = CATEGORY_NAME, attributes = emptyList()))

            val profiles = scoreProfileRepository.observeProfilesForCategory(CATEGORY_NAME).first()
            assertEquals(emptyList<ScoreProfile>(), profiles)
        }

    @Test
    fun `profile with partial attribute removal retains remaining attributes`() =
        runTest {
            categoryRepository.saveCategory(threeAttributeCategory())
            scoreProfileRepository.saveProfile(profileUsingAllAttributes())

            categoryRepository.saveCategory(
                Category(name = CATEGORY_NAME, attributes = listOf(Attribute("taste"))),
            )

            val profiles = scoreProfileRepository.observeProfilesForCategory(CATEGORY_NAME).first()
            assertEquals(1, profiles.size)
            assertEquals(listOf("taste"), profiles[0].includedAttributeIds)
        }

    @Test
    fun `observeProfile returns null when all attributes are cascade-deleted`() =
        runTest {
            categoryRepository.saveCategory(threeAttributeCategory())
            scoreProfileRepository.saveProfile(profileUsingAllAttributes())

            categoryRepository.saveCategory(Category(name = CATEGORY_NAME, attributes = emptyList()))

            val profile = scoreProfileRepository.observeProfile(PROFILE_ID).first()
            assertNull(profile)
        }

    private fun threeAttributeCategory(): Category =
        Category(
            name = CATEGORY_NAME,
            attributes =
                listOf(
                    Attribute("taste"),
                    Attribute("texture"),
                    Attribute("service"),
                ),
        )

    private fun profileUsingAllAttributes(): ScoreProfile =
        ScoreProfile(
            id = PROFILE_ID,
            categoryName = CATEGORY_NAME,
            name = "All Attributes",
            includedAttributeIds = listOf("taste", "texture", "service"),
        )

    private companion object {
        const val CATEGORY_NAME = "Food"
        const val PROFILE_ID = "profile-1"
    }
}
