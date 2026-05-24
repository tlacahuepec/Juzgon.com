package com.juzgon.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Suppress("MaxLineLength")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CrossCategoryAttributeIntegrityTest {
    private lateinit var database: JuzgonDatabase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var ratedItemRepository: RatedItemRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, JuzgonDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        categoryRepository = RoomCategoryRepository(database)
        ratedItemRepository = RoomRatedItemRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saving category B with same-named attribute does not affect category A`() =
        runTest {
            categoryRepository.saveCategory(
                Category(
                    name = "People",
                    attributes = listOf(Attribute("People/Nationality", type = AttributeType.NATIONALITY, isRequired = false)),
                ),
            )

            categoryRepository.saveCategory(
                Category(
                    name = "Drinks",
                    attributes = listOf(Attribute("Drinks/Nationality", type = AttributeType.NATIONALITY, isRequired = false)),
                ),
            )

            val observedA = categoryRepository.observeCategory("People").first()
            assertEquals(1, observedA?.attributes?.size)
            assertEquals("People/Nationality", observedA?.attributes?.get(0)?.id)
        }

    @Test
    fun `item values are preserved when sibling category adds same-named attribute`() =
        runTest {
            val nationalityAttr = Attribute("People/Nationality", type = AttributeType.NATIONALITY, isRequired = false)
            val ratingAttr = Attribute("People/Charisma")
            categoryRepository.saveCategory(
                Category(name = "People", attributes = listOf(ratingAttr, nationalityAttr)),
            )
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = "person-1",
                    scores = listOf(ScoreEntry(ratingAttr, 8)),
                    values = listOf(ItemAttributeValue(nationalityAttr, "MX")),
                ),
            )

            categoryRepository.saveCategory(
                Category(
                    name = "Drinks",
                    attributes = listOf(Attribute("Drinks/Nationality", type = AttributeType.NATIONALITY, isRequired = false)),
                ),
            )

            val item = ratedItemRepository.observeRatedItem("person-1").first()
            val nationalityValue = item?.values?.firstOrNull { it.attribute.type == AttributeType.NATIONALITY }
            assertEquals("MX", nationalityValue?.value)
        }

    @Test
    fun `ratings are preserved when sibling category adds same-named attribute`() =
        runTest {
            val qualityA = Attribute("Food/Quality")
            categoryRepository.saveCategory(Category(name = "Food", attributes = listOf(qualityA)))
            ratedItemRepository.saveRatedItem(
                RatedItem(id = "pizza", scores = listOf(ScoreEntry(qualityA, 9))),
            )

            val qualityB = Attribute("Drinks/Quality")
            categoryRepository.saveCategory(Category(name = "Drinks", attributes = listOf(qualityB)))

            val item = ratedItemRepository.observeRatedItem("pizza").first()
            assertEquals(9, item?.scores?.firstOrNull()?.score)
            assertEquals(
                "Food/Quality",
                item
                    ?.scores
                    ?.firstOrNull()
                    ?.attribute
                    ?.id,
            )
        }

    @Test
    fun `editing and re-saving a category preserves all item data`() =
        runTest {
            val taste = Attribute("Food/taste")
            val service = Attribute("Food/service")
            val category = Category(name = "Food", attributes = listOf(taste, service))
            categoryRepository.saveCategory(category)
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = "restaurant-1",
                    scores = listOf(ScoreEntry(taste, 7), ScoreEntry(service, 9)),
                ),
            )

            categoryRepository.saveCategory(category)

            val item = ratedItemRepository.observeRatedItem("restaurant-1").first()
            assertEquals(2, item?.scores?.size)
            val scores = item?.scores?.associate { it.attribute.id to it.score }
            assertEquals(7, scores?.get("Food/taste"))
            assertEquals(9, scores?.get("Food/service"))
        }

    @Test
    fun `toCategory generates scoped attribute IDs`() =
        runTest {
            val formState =
                com.juzgon.feature.category.CategoryFormUiState(
                    name = "Food",
                    attributes =
                        listOf(
                            com.juzgon.feature.category
                                .CategoryAttributeInput(key = 0L, name = "Taste"),
                            com.juzgon.feature.category.CategoryAttributeInput(
                                key = 1L,
                                name = "Nationality",
                                type = AttributeType.NATIONALITY,
                                isRequired = false,
                            ),
                        ),
                )

            val category = formState.toCategory()
            assertEquals("Food/Taste", category.attributes[0].id)
            assertEquals("Food/Nationality", category.attributes[1].id)
        }
}
