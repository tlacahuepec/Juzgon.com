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
import com.juzgon.domain.backup.BackupSchemaContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Suppress("MaxLineLength")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupContractTest {
    private lateinit var categoryDao: ContractCategoryDao
    private lateinit var itemDao: ContractItemDao
    private lateinit var scoreProfileDao: ContractScoreProfileDao
    private lateinit var service: JsonBackupService
    private lateinit var validator: JsonBackupValidator

    @Before
    fun setUp() {
        categoryDao = ContractCategoryDao()
        itemDao = ContractItemDao()
        scoreProfileDao = ContractScoreProfileDao()
        service = JsonBackupService(categoryDao, itemDao, scoreProfileDao)
        validator = JsonBackupValidator()
    }

    @Test
    fun export_containsAllRequiredMetadataFields() =
        runTest {
            val json = JSONObject(service.export())

            for (field in BackupSchemaContract.REQUIRED_METADATA_FIELDS) {
                assertTrue("Missing metadata field: $field", json.has(field))
            }
        }

    @Test
    fun export_versionMatchesContractCurrentVersion() =
        runTest {
            val json = JSONObject(service.export())

            assertEquals(BackupSchemaContract.CURRENT_VERSION, json.getInt("version"))
        }

    @Test
    fun export_containsAllRequiredArrays() =
        runTest {
            val json = JSONObject(service.export())

            for (key in BackupSchemaContract.REQUIRED_ARRAYS) {
                assertTrue("Missing required array: $key", json.has(key))
                json.getJSONArray(key)
            }
        }

    @Test
    fun export_categoriesPreserveStableId() =
        runTest {
            categoryDao.state.value =
                listOf(CategoryWithAttributes(CategoryEntity("Cars"), emptyList()))

            val json = JSONObject(service.export())
            val category = json.getJSONArray("categories").getJSONObject(0)

            assertTrue(category.has(BackupSchemaContract.CATEGORY_ID_FIELD))
            assertEquals("Cars", category.getString(BackupSchemaContract.CATEGORY_ID_FIELD))
        }

    @Test
    fun export_attributesPreserveStableId() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("Cars"),
                        listOf(AttributeEntity("Speed", "Cars")),
                    ),
                )

            val json = JSONObject(service.export())
            val attr =
                json
                    .getJSONArray("categories")
                    .getJSONObject(0)
                    .getJSONArray("attributes")
                    .getJSONObject(0)

            assertTrue(attr.has(BackupSchemaContract.ATTRIBUTE_ID_FIELD))
            assertEquals("Speed", attr.getString(BackupSchemaContract.ATTRIBUTE_ID_FIELD))
        }

    @Test
    fun export_attributesIncludeAllRequiredFields() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("Cars"),
                        listOf(
                            AttributeEntity(
                                id = "Speed",
                                categoryName = "Cars",
                                weight = 1.5,
                                position = 0,
                                type = "NUMBER",
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

            for (field in BackupSchemaContract.REQUIRED_ATTRIBUTE_FIELDS) {
                assertTrue("Missing required attribute field: $field", attr.has(field))
            }
        }

    @Test
    fun export_itemsPreserveStableId() =
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
                        ItemEntity("Roadster", "", 100L, 200L),
                        listOf(RatingEntity("Roadster", "Speed", 9)),
                        emptyList(),
                    ),
                )

            val json = JSONObject(service.export())
            val item = json.getJSONArray("items").getJSONObject(0)

            assertTrue(item.has(BackupSchemaContract.ITEM_ID_FIELD))
            assertEquals("Roadster", item.getString(BackupSchemaContract.ITEM_ID_FIELD))
        }

    @Test
    fun export_scoreProfilesPreserveStableId() =
        runTest {
            scoreProfileDao.profiles.value =
                listOf(
                    ScoreProfileEntity(
                        id = "profile-1",
                        categoryName = "Cars",
                        name = "Quick",
                        createdAt = 0L,
                        updatedAt = 0L,
                    ),
                )

            val json = JSONObject(service.export())
            val profile = json.getJSONArray("scoreProfiles").getJSONObject(0)

            assertTrue(profile.has(BackupSchemaContract.SCORE_PROFILE_ID_FIELD))
            assertEquals("profile-1", profile.getString(BackupSchemaContract.SCORE_PROFILE_ID_FIELD))
        }

    @Test
    fun export_includesSchemaVersionRequiredForImport() =
        runTest {
            val json = JSONObject(service.export())

            assertTrue(json.has("version"))
            assertTrue(
                BackupSchemaContract.SUPPORTED_VERSIONS.contains(json.getInt("version")),
            )
        }

    @Test
    fun export_optionalFieldsAbsentDoNotBreakValidation() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("Cars"),
                        listOf(
                            AttributeEntity(
                                id = "Speed",
                                categoryName = "Cars",
                                weight = 1.0,
                                position = 0,
                                type = "NUMBER",
                                diamondOrder = null,
                                scoringDirection = null,
                            ),
                        ),
                    ),
                )

            val exported = service.export()
            val result = validator.validate(exported)

            assertTrue("Validation failed: ${result.errors}", result.isValid)
        }

    @Test
    fun export_fullPayloadPassesValidation() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("Cars"),
                        listOf(
                            AttributeEntity("Speed", "Cars", 1.0, 0, "NUMBER"),
                            AttributeEntity("Notes", "Cars", 1.0, 1, "NOTES"),
                        ),
                    ),
                )
            itemDao.state.value =
                listOf(
                    ItemWithRatings(
                        ItemEntity("Roadster", "fast", 100L, 200L),
                        listOf(RatingEntity("Roadster", "Speed", 9)),
                        listOf(ItemValueEntity("Roadster", "Notes", "A note")),
                    ),
                )
            scoreProfileDao.profiles.value =
                listOf(
                    ScoreProfileEntity("p1", "Cars", "Default", 0L, 0L),
                )
            scoreProfileDao.profileAttributes.value =
                listOf(
                    ScoreProfileAttributeEntity("p1", "Speed", 0),
                )

            val exported = service.export()
            val result = validator.validate(exported)

            assertTrue("Validation failed: ${result.errors}", result.isValid)
        }

    // --- Minimal fakes for export path ---

    @Suppress("TooManyFunctions")
    private class ContractCategoryDao : CategoryDao {
        val state = MutableStateFlow<List<CategoryWithAttributes>>(emptyList())

        override fun observeCategoriesWithAttributes(): Flow<List<CategoryWithAttributes>> = state

        override fun observeItemCountsByCategory(): Flow<List<CategoryItemCount>> = MutableStateFlow(emptyList())

        override suspend fun upsertCategory(category: CategoryEntity) = error("not used")

        override suspend fun upsertAttributes(attributes: List<AttributeEntity>) = error("not used")

        override suspend fun deleteCategoryByName(name: String) = error("not used")

        override suspend fun deleteAttributesForCategory(categoryName: String) = error("not used")

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

        override suspend fun renameAttributeIdInScoreProfileAttributes(
            oldAttributeId: String,
            newAttributeId: String,
        ) = error("not used")
    }

    @Suppress("TooManyFunctions")
    private class ContractItemDao : ItemDao {
        val state = MutableStateFlow<List<ItemWithRatings>>(emptyList())

        override fun observeItemsWithRatings(): Flow<List<ItemWithRatings>> = state

        override suspend fun upsertItem(item: ItemEntity) = error("not used")

        override suspend fun upsertRatings(ratings: List<RatingEntity>) = error("not used")

        override suspend fun deleteItemById(id: String) = error("not used")

        override suspend fun deleteRatingsForItem(itemId: String) = error("not used")

        override suspend fun upsertItemValues(values: List<ItemValueEntity>) = error("not used")

        override suspend fun deleteItemValuesForItem(itemId: String) = error("not used")

        override suspend fun softDeleteItemValuesNotIn(
            itemId: String,
            keepAttributeIds: List<String>,
            deletedAt: Long,
        ) = error("not used")

        override suspend fun purgeOldSoftDeletedValues(cutoff: Long) = error("not used")

        override suspend fun purgeOrphanedRatings() = error("not used")

        override suspend fun purgeOrphanedSoftDeletedValues() = error("not used")

        override fun getItemWithRatings(id: String): ItemWithRatings? = error("not used")

        override fun observeItemWithRatings(id: String): Flow<ItemWithRatings?> = error("not used")

        override fun observeRankedItemsForCategory(categoryName: String): Flow<List<RankedItemWithRatings>> = error("not used")

        override fun getRatingsForItem(itemId: String): List<RatingEntity> = error("not used")
    }

    @Suppress("TooManyFunctions")
    private class ContractScoreProfileDao : ScoreProfileDao {
        val profiles = MutableStateFlow<List<ScoreProfileEntity>>(emptyList())
        val profileAttributes = MutableStateFlow<List<ScoreProfileAttributeEntity>>(emptyList())

        override fun observeAllProfiles(): Flow<List<ScoreProfileEntity>> = profiles

        override fun observeAllProfileAttributes(): Flow<List<ScoreProfileAttributeEntity>> = profileAttributes

        override fun observeProfilesForCategory(categoryName: String): Flow<List<ScoreProfileEntity>> = error("not used")

        override fun observeProfile(id: String): Flow<ScoreProfileEntity?> = error("not used")

        override suspend fun upsertProfile(profile: ScoreProfileEntity) = error("not used")

        override suspend fun deleteAttributesForProfile(profileId: String) = error("not used")

        override suspend fun upsertProfileAttributes(attributes: List<ScoreProfileAttributeEntity>) = error("not used")

        override fun observeAttributesForProfile(profileId: String): Flow<List<ScoreProfileAttributeEntity>> = error("not used")

        override fun observeAttributesForCategory(categoryName: String): Flow<List<ScoreProfileAttributeEntity>> = error("not used")

        override suspend fun deleteProfile(id: String) = error("not used")

        override suspend fun deleteOrphanedProfiles() = error("not used")

        override suspend fun saveProfileWithAttributes(
            profile: ScoreProfileEntity,
            attributes: List<ScoreProfileAttributeEntity>,
        ) = error("not used")
    }
}
