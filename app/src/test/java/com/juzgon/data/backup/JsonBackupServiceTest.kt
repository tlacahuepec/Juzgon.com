package com.juzgon.data.backup

import com.juzgon.data.local.dao.CategoryDao
import com.juzgon.data.local.dao.CategoryItemCount
import com.juzgon.data.local.dao.CategoryWithAttributes
import com.juzgon.data.local.dao.ItemDao
import com.juzgon.data.local.dao.ItemWithRatings
import com.juzgon.data.local.dao.RankedItemWithRatings
import com.juzgon.data.local.dao.ScoreProfileDao
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.data.local.entity.ScoreProfileAttributeEntity
import com.juzgon.data.local.entity.ScoreProfileEntity
import com.juzgon.domain.backup.BackupException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private lateinit var scoreProfileDao: FakeScoreProfileDao
    private lateinit var service: JsonBackupService

    @Before
    fun setUp() {
        categoryDao = FakeCategoryDao()
        itemDao = FakeItemDao()
        scoreProfileDao = FakeScoreProfileDao()
        service = JsonBackupService(categoryDao, itemDao, scoreProfileDao)
    }

    @Test
    fun export_includesSchemaVersion2() =
        runTest {
            val json = JSONObject(service.export())

            assertEquals(2, json.getInt("version"))
        }

    @Test
    fun export_includesAppMetadata() =
        runTest {
            val json = JSONObject(service.export())

            assertEquals("Juzgon", json.getString("app"))
            assertTrue(json.has("exportedAt"))
            assertTrue(json.getString("exportedAt").isNotBlank())
        }

    @Test
    fun export_includesCategoryName() =
        runTest {
            categoryDao.state.value = listOf(CategoryWithAttributes(CategoryEntity("Cars"), emptyList()))

            val json = service.export()

            assertTrue(json.contains("Cars"))
        }

    @Test
    fun export_includesFullAttributeFields() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("Cars"),
                        listOf(
                            AttributeEntity(
                                id = "Speed",
                                categoryName = "Cars",
                                weight = 2.0,
                                position = 0,
                                type = "NUMBER",
                                isRequired = true,
                                displayInDiamond = true,
                                diamondOrder = 1,
                                scoringDirection = null,
                            ),
                        ),
                    ),
                )

            val json = JSONObject(service.export())
            val attr =
                json
                    .getJSONArray("categories")
                    .getJSONObject(0)
                    .getJSONArray("attributes")
                    .getJSONObject(0)

            assertEquals("Speed", attr.getString("id"))
            assertEquals(2.0, attr.getDouble("weight"), 0.001)
            assertEquals(0, attr.getInt("position"))
            assertEquals("NUMBER", attr.getString("type"))
            assertEquals(true, attr.getBoolean("isRequired"))
            assertEquals(true, attr.getBoolean("displayInDiamond"))
            assertEquals(1, attr.getInt("diamondOrder"))
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
                        emptyList(),
                    ),
                )

            val json = service.export()

            assertTrue(json.contains("Roadster"))
            assertTrue(json.contains("cool car"))
            assertTrue(json.contains("Speed"))
        }

    @Test
    fun export_includesItemAttributeValues() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("Cars"),
                        listOf(
                            AttributeEntity("Speed", "Cars"),
                            AttributeEntity("Details", "Cars", type = "NOTES"),
                        ),
                    ),
                )
            itemDao.state.value =
                listOf(
                    ItemWithRatings(
                        ItemEntity("Roadster", "", 100L, 200L),
                        listOf(RatingEntity("Roadster", "Speed", 9)),
                        listOf(ItemValueEntity("Roadster", "Details", "Very fast car")),
                    ),
                )

            val json = JSONObject(service.export())
            val item = json.getJSONArray("items").getJSONObject(0)
            val values = item.getJSONArray("values")

            assertEquals(1, values.length())
            assertEquals("Details", values.getJSONObject(0).getString("attributeId"))
            assertEquals("Very fast car", values.getJSONObject(0).getString("value"))
        }

    @Test
    fun export_includesScoreProfiles() =
        runTest {
            scoreProfileDao.profiles.value =
                listOf(
                    ScoreProfileEntity(
                        id = "profile-1",
                        categoryName = "Cars",
                        name = "Speed Focus",
                        createdAt = 100L,
                        updatedAt = 200L,
                    ),
                )
            scoreProfileDao.profileAttributes.value =
                listOf(
                    ScoreProfileAttributeEntity("profile-1", "Speed", 0),
                    ScoreProfileAttributeEntity("profile-1", "Brakes", 1),
                )

            val json = JSONObject(service.export())
            val profiles = json.getJSONArray("scoreProfiles")

            assertEquals(1, profiles.length())
            val profile = profiles.getJSONObject(0)
            assertEquals("profile-1", profile.getString("id"))
            assertEquals("Cars", profile.getString("categoryName"))
            assertEquals("Speed Focus", profile.getString("name"))
            assertEquals(100L, profile.getLong("createdAt"))
            assertEquals(200L, profile.getLong("updatedAt"))
            val attrIds = profile.getJSONArray("attributeIds")
            assertEquals(2, attrIds.length())
            assertEquals("Speed", attrIds.getString(0))
            assertEquals("Brakes", attrIds.getString(1))
        }

    @Test
    fun export_emptyDatabase_succeeds() =
        runTest {
            val json = JSONObject(service.export())

            assertEquals(2, json.getInt("version"))
            assertEquals(0, json.getJSONArray("categories").length())
            assertEquals(0, json.getJSONArray("items").length())
            assertEquals(0, json.getJSONArray("scoreProfiles").length())
        }

    @Test
    fun export_doesNotMutateSourceData() =
        runTest {
            categoryDao.state.value =
                listOf(CategoryWithAttributes(CategoryEntity("Cars"), emptyList()))

            service.export()

            assertFalse(categoryDao.writeMethodCalled)
            assertFalse(itemDao.writeMethodCalled)
            assertFalse(scoreProfileDao.writeMethodCalled)
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
                listOf(ItemWithRatings(ItemEntity("OldItem"), emptyList(), emptyList()))
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
                        emptyList(),
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
        var writeMethodCalled = false

        fun reset() {
            upsertedCategories.clear()
            upsertedAttributes.clear()
            deletedCategoryNames.clear()
            writeMethodCalled = false
        }

        override fun observeCategoriesWithAttributes(): Flow<List<CategoryWithAttributes>> = state

        override fun observeItemCountsByCategory(): Flow<List<CategoryItemCount>> = MutableStateFlow(emptyList())

        override suspend fun upsertCategory(category: CategoryEntity) {
            writeMethodCalled = true
            upsertedCategories += category
        }

        override suspend fun upsertAttributes(attributes: List<AttributeEntity>) {
            writeMethodCalled = true
            upsertedAttributes += attributes
        }

        override suspend fun deleteCategoryByName(name: String) {
            writeMethodCalled = true
            deletedCategoryNames += name
        }

        override suspend fun deleteAttributesForCategory(categoryName: String) {
            writeMethodCalled = true
        }

        override fun getCategoryWithAttributes(name: String): CategoryWithAttributes? = error("not used")

        override fun observeCategoryWithAttributes(name: String): Flow<CategoryWithAttributes?> = error("not used")

        override fun observeAttributes(): Flow<List<AttributeEntity>> = error("not used")

        override fun getAttributesForCategory(categoryName: String): List<AttributeEntity> = error("not used")

        override suspend fun deleteAttributesNotIn(
            categoryName: String,
            attributeIds: List<String>,
        ) = error("not used")

        override suspend fun renameAttributeIdInRatings(
            oldAttributeId: String,
            newAttributeId: String,
        ) = error("not used")

        override suspend fun renameAttributeIdInItemValues(
            oldAttributeId: String,
            newAttributeId: String,
        ) = error("not used")

        override suspend fun renameAttributeIdInRankSnapshots(
            oldAttributeId: String,
            newAttributeId: String,
        ) = error("not used")
    }

    private class FakeItemDao : ItemDao {
        val state = MutableStateFlow<List<ItemWithRatings>>(emptyList())
        val upsertedItems = mutableListOf<ItemEntity>()
        val upsertedRatings = mutableListOf<RatingEntity>()
        val deletedItemIds = mutableListOf<String>()
        var writeMethodCalled = false

        fun reset() {
            upsertedItems.clear()
            upsertedRatings.clear()
            deletedItemIds.clear()
            writeMethodCalled = false
        }

        override fun observeItemsWithRatings(): Flow<List<ItemWithRatings>> = state

        override suspend fun upsertItem(item: ItemEntity) {
            writeMethodCalled = true
            upsertedItems += item
        }

        override suspend fun upsertRatings(ratings: List<RatingEntity>) {
            writeMethodCalled = true
            upsertedRatings += ratings
        }

        override suspend fun deleteItemById(id: String) {
            writeMethodCalled = true
            deletedItemIds += id
        }

        override suspend fun deleteRatingsForItem(itemId: String) {
            writeMethodCalled = true
        }

        override suspend fun upsertItemValues(values: List<ItemValueEntity>) {
            writeMethodCalled = true
        }

        override suspend fun deleteItemValuesForItem(itemId: String) {
            writeMethodCalled = true
        }

        override fun getItemWithRatings(id: String): ItemWithRatings? = error("not used")

        override fun observeItemWithRatings(id: String): Flow<ItemWithRatings?> = error("not used")

        @Suppress("MaxLineLength")
        override fun observeRankedItemsForCategory(categoryName: String): Flow<List<RankedItemWithRatings>> {
            error("not used")
        }

        override fun getRatingsForItem(itemId: String): List<RatingEntity> = error("not used")
    }

    @Suppress("MaxLineLength")
    private class FakeScoreProfileDao : ScoreProfileDao {
        val profiles = MutableStateFlow<List<ScoreProfileEntity>>(emptyList())
        val profileAttributes = MutableStateFlow<List<ScoreProfileAttributeEntity>>(emptyList())
        var writeMethodCalled = false

        override fun observeAllProfiles(): Flow<List<ScoreProfileEntity>> = profiles

        override fun observeAllProfileAttributes(): Flow<List<ScoreProfileAttributeEntity>> = profileAttributes

        override fun observeProfilesForCategory(categoryName: String): Flow<List<ScoreProfileEntity>> = error("not used")

        override fun observeProfile(id: String): Flow<ScoreProfileEntity?> = error("not used")

        override suspend fun upsertProfile(profile: ScoreProfileEntity) {
            writeMethodCalled = true
        }

        override suspend fun deleteAttributesForProfile(profileId: String) {
            writeMethodCalled = true
        }

        override suspend fun upsertProfileAttributes(attributes: List<ScoreProfileAttributeEntity>) {
            writeMethodCalled = true
        }

        override fun observeAttributesForProfile(profileId: String): Flow<List<ScoreProfileAttributeEntity>> = error("not used")

        override fun observeAttributesForCategory(categoryName: String): Flow<List<ScoreProfileAttributeEntity>> = error("not used")

        override suspend fun deleteProfile(id: String) {
            writeMethodCalled = true
        }

        override suspend fun saveProfileWithAttributes(
            profile: ScoreProfileEntity,
            attributes: List<ScoreProfileAttributeEntity>,
        ) {
            writeMethodCalled = true
        }
    }
}
