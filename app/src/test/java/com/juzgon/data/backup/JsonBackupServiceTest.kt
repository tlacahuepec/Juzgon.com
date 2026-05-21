package com.juzgon.data.backup

import com.juzgon.data.local.dao.CategoryDao
import com.juzgon.data.local.dao.CategoryItemCount
import com.juzgon.data.local.dao.CategoryWithAttributes
import com.juzgon.data.local.dao.ItemDao
import com.juzgon.data.local.dao.ItemWithRatings
import com.juzgon.data.local.dao.RankedItemWithRatings
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.domain.backup.BackupException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JsonBackupServiceTest {
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var itemDao: FakeItemDao
    private lateinit var service: JsonBackupService

    @Before
    fun setUp() {
        categoryDao = FakeCategoryDao()
        itemDao = FakeItemDao()
        service = JsonBackupService(categoryDao, itemDao)
    }

    @Test
    fun export_returnsJsonWithSchemaVersion() =
        runTest {
            val json = service.export()

            assertTrue("version field missing", json.contains("\"version\""))
        }

    @Test
    fun export_includesCategoryName() =
        runTest {
            categoryDao.state.value = listOf(CategoryWithAttributes(CategoryEntity("Cars"), emptyList()))

            val json = service.export()

            assertTrue(json.contains("Cars"))
        }

    @Test
    fun export_includesAttributeFields() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("Cars"),
                        listOf(AttributeEntity("Speed", "Cars", 2.0, 0)),
                    ),
                )

            val json = service.export()

            assertTrue(json.contains("Speed"))
            assertTrue(json.contains("\"weight\""))
            assertTrue(json.contains("\"position\""))
        }

    @Test
    fun export_includesItemAndRatings() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("Cars"),
                        listOf(AttributeEntity("Speed", "Cars")),
                    ),
                )
            itemDao.state.value =
                listOf(
                    ItemWithRatings(
                        ItemEntity("Roadster", "cool car", 100L, 200L),
                        listOf(RatingEntity("Roadster", "Speed", 9)),
                    ),
                )

            val json = service.export()

            assertTrue(json.contains("Roadster"))
            assertTrue(json.contains("cool car"))
            assertTrue(json.contains("Speed"))
        }

    @Test
    fun import_restoresCategoryAndAttributes() =
        runTest {
            val json =
                """{"version":1,"categories":[{"name":"Cars",""" +
                    """"attributes":[{"id":"Speed","weight":1.5,"position":2}]}],"items":[]}"""

            service.import(json)

            assertEquals(1, categoryDao.upsertedCategories.size)
            assertEquals("Cars", categoryDao.upsertedCategories[0].name)
            assertEquals(1, categoryDao.upsertedAttributes.size)
            assertEquals("Speed", categoryDao.upsertedAttributes[0].id)
            assertEquals(1.5, categoryDao.upsertedAttributes[0].weight, 0.001)
            assertEquals(2, categoryDao.upsertedAttributes[0].position)
        }

    @Test
    fun import_restoresItemsAndRatings() =
        runTest {
            val catJson =
                """{"name":"Cars","attributes":[{"id":"Speed","weight":1.0,"position":0}]}"""
            val itemJson =
                """{"id":"Roadster","categoryName":"Cars","notes":"cool",""" +
                    """"createdAt":100,"updatedAt":200,"ratings":[{"attributeId":"Speed","score":9}]}"""
            val json = """{"version":1,"categories":[$catJson],"items":[$itemJson]}"""

            service.import(json)

            assertEquals(1, itemDao.upsertedItems.size)
            assertEquals("Roadster", itemDao.upsertedItems[0].id)
            assertEquals("cool", itemDao.upsertedItems[0].notes)
            assertEquals(100L, itemDao.upsertedItems[0].createdAt)
            assertEquals(1, itemDao.upsertedRatings.size)
            assertEquals(9, itemDao.upsertedRatings[0].score)
        }

    @Test
    fun import_deletesExistingDataBeforeRestore() =
        runTest {
            categoryDao.state.value =
                listOf(CategoryWithAttributes(CategoryEntity("OldCategory"), emptyList()))
            itemDao.state.value =
                listOf(ItemWithRatings(ItemEntity("OldItem"), emptyList()))
            val json = """{"version":1,"categories":[],"items":[]}"""

            service.import(json)

            assertTrue(categoryDao.deletedCategoryNames.contains("OldCategory"))
            assertTrue(itemDao.deletedItemIds.contains("OldItem"))
        }

    @Test
    fun roundTrip_preservesAllData() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("Cars"),
                        listOf(
                            AttributeEntity("Speed", "Cars", 1.0, 0),
                            AttributeEntity("Brakes", "Cars", 2.0, 1),
                        ),
                    ),
                )
            itemDao.state.value =
                listOf(
                    ItemWithRatings(
                        ItemEntity("Roadster", "weekend car", 100L, 200L),
                        listOf(
                            RatingEntity("Roadster", "Speed", 9),
                            RatingEntity("Roadster", "Brakes", 8),
                        ),
                    ),
                )

            val json = service.export()

            categoryDao.reset()
            itemDao.reset()
            categoryDao.state.value = emptyList()
            itemDao.state.value = emptyList()

            service.import(json)

            assertEquals(1, categoryDao.upsertedCategories.size)
            assertEquals("Cars", categoryDao.upsertedCategories[0].name)
            assertEquals(2, categoryDao.upsertedAttributes.size)
            assertEquals("Speed", categoryDao.upsertedAttributes[0].id)
            assertEquals("Brakes", categoryDao.upsertedAttributes[1].id)
            assertEquals(1, itemDao.upsertedItems.size)
            assertEquals("Roadster", itemDao.upsertedItems[0].id)
            assertEquals("weekend car", itemDao.upsertedItems[0].notes)
            assertEquals(2, itemDao.upsertedRatings.size)
        }

    @Test
    fun import_emptyBackup_succeeds() =
        runTest {
            val json = """{"version":1,"categories":[],"items":[]}"""

            service.import(json)

            assertEquals(0, categoryDao.upsertedCategories.size)
            assertEquals(0, itemDao.upsertedItems.size)
        }

    @Test(expected = BackupException::class)
    fun import_invalidJson_throwsBackupException() =
        runTest {
            service.import("not json at all {{{")
        }

    @Test(expected = BackupException::class)
    fun import_missingVersionField_throwsBackupException() =
        runTest {
            service.import("""{"categories":[],"items":[]}""")
        }

    @Test(expected = BackupException::class)
    fun import_unsupportedVersion_throwsBackupException() =
        runTest {
            service.import("""{"version":999,"categories":[],"items":[]}""")
        }

    // --- Fakes ---

    private class FakeCategoryDao : CategoryDao {
        val state = MutableStateFlow<List<CategoryWithAttributes>>(emptyList())
        val upsertedCategories = mutableListOf<CategoryEntity>()
        val upsertedAttributes = mutableListOf<AttributeEntity>()
        val deletedCategoryNames = mutableListOf<String>()

        fun reset() {
            upsertedCategories.clear()
            upsertedAttributes.clear()
            deletedCategoryNames.clear()
        }

        override fun observeCategoriesWithAttributes(): Flow<List<CategoryWithAttributes>> = state

        override fun observeItemCountsByCategory(): Flow<List<CategoryItemCount>> = MutableStateFlow(emptyList())

        override suspend fun upsertCategory(category: CategoryEntity) {
            upsertedCategories += category
        }

        override suspend fun upsertAttributes(attributes: List<AttributeEntity>) {
            upsertedAttributes += attributes
        }

        override suspend fun deleteCategoryByName(name: String) {
            deletedCategoryNames += name
        }

        override suspend fun deleteAttributesForCategory(categoryName: String) = Unit

        override fun getCategoryWithAttributes(name: String): CategoryWithAttributes? = error("not used")

        override fun observeCategoryWithAttributes(name: String): Flow<CategoryWithAttributes?> = error("not used")

        override fun observeAttributes(): Flow<List<AttributeEntity>> = error("not used")

        override fun getAttributesForCategory(categoryName: String): List<AttributeEntity> = error("not used")

        override suspend fun deleteAttributesNotIn(
            categoryName: String,
            attributeIds: List<String>,
        ) = error("not used")
    }

    private class FakeItemDao : ItemDao {
        val state = MutableStateFlow<List<ItemWithRatings>>(emptyList())
        val upsertedItems = mutableListOf<ItemEntity>()
        val upsertedRatings = mutableListOf<RatingEntity>()
        val deletedItemIds = mutableListOf<String>()

        fun reset() {
            upsertedItems.clear()
            upsertedRatings.clear()
            deletedItemIds.clear()
        }

        override fun observeItemsWithRatings(): Flow<List<ItemWithRatings>> = state

        override suspend fun upsertItem(item: ItemEntity) {
            upsertedItems += item
        }

        override suspend fun upsertRatings(ratings: List<RatingEntity>) {
            upsertedRatings += ratings
        }

        override suspend fun deleteItemById(id: String) {
            deletedItemIds += id
        }

        override suspend fun deleteRatingsForItem(itemId: String) = Unit

        override fun getItemWithRatings(id: String): ItemWithRatings? = error("not used")

        override fun observeItemWithRatings(id: String): Flow<ItemWithRatings?> = error("not used")

        @Suppress("MaxLineLength")
        override fun observeRankedItemsForCategory(categoryName: String): Flow<List<RankedItemWithRatings>> {
            error("not used")
        }

        override fun getRatingsForItem(itemId: String): List<RatingEntity> = error("not used")
    }
}
