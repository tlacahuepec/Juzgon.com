package com.juzgon.data.backup

import com.juzgon.data.local.dao.CategoryDao
import com.juzgon.data.local.dao.CategoryItemCount
import com.juzgon.data.local.dao.CategoryWithAttributes
import com.juzgon.data.local.dao.ItemDao
import com.juzgon.data.local.dao.ItemPurgeDao
import com.juzgon.data.local.dao.ItemWithRatings
import com.juzgon.data.local.dao.RankedItemWithRatings
import com.juzgon.data.local.dao.ScoreProfileAttributeDao
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
    private lateinit var itemPurgeDao: FakeItemPurgeDao
    private lateinit var scoreProfileDao: FakeScoreProfileDao
    private lateinit var scoreProfileAttributeDao: FakeScoreProfileAttributeDao
    private lateinit var service: JsonBackupService
    private var maintenanceRanCount = 0

    @Before
    fun setUp() {
        categoryDao = FakeCategoryDao()
        itemDao = FakeItemDao()
        itemPurgeDao = FakeItemPurgeDao()
        scoreProfileDao = FakeScoreProfileDao()
        scoreProfileAttributeDao = FakeScoreProfileAttributeDao()
        maintenanceRanCount = 0
        service =
            JsonBackupService(
                validator = JsonBackupValidator(),
                categoryDao = categoryDao,
                itemDao = itemDao,
                itemPurgeDao = itemPurgeDao,
                scoreProfileDao = scoreProfileDao,
                scoreProfileAttributeDao = scoreProfileAttributeDao,
                runInTransaction = { block -> block() },
                runPostImportMaintenance = { maintenanceRanCount++ },
            )
    }

    @Test
    fun export_includesSchemaVersion5() =
        runTest {
            val json = JSONObject(service.export())

            assertEquals(5, json.getInt("version"))
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
    fun roundTrip_preservesSkinTypeAttributeAndValue() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("People"),
                        listOf(
                            AttributeEntity("People/Score", "People", type = "NUMBER"),
                            AttributeEntity(
                                id = "People/Skin Type",
                                categoryName = "People",
                                type = "SKIN_TYPE",
                                isRequired = false,
                                displayInDiamond = false,
                            ),
                        ),
                    ),
                )
            itemDao.state.value =
                listOf(
                    ItemWithRatings(
                        ItemEntity("Alice", "", 100L, 200L),
                        listOf(RatingEntity("Alice", "People/Score", 8)),
                        listOf(ItemValueEntity("Alice", "People/Skin Type", "TYPE_I")),
                    ),
                )

            val exported = service.export()
            categoryDao.reset()
            itemDao.reset()

            service.import(exported)

            val skinTypeAttribute = categoryDao.upsertedAttributes.single { it.id == "People/Skin Type" }
            val skinTypeValue = itemDao.upsertedValues.single { it.attributeId == "People/Skin Type" }
            assertEquals("SKIN_TYPE", skinTypeAttribute.type)
            assertEquals("TYPE_I", skinTypeValue.valueText)
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
            scoreProfileAttributeDao.profileAttributes.value =
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

            assertEquals(5, json.getInt("version"))
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
                validImportJson(
                    categories =
                        """[{"name":"Cars","attributes":[{"id":"Speed","weight":1.5,"position":2,"type":"NUMBER"}]}]""",
                )

            service.import(json)

            assertEquals(1, categoryDao.upsertedCategories.size)
            assertEquals("Cars", categoryDao.upsertedCategories[0].name)
            assertEquals(1, categoryDao.upsertedAttributes.size)
            assertEquals("Cars/Speed", categoryDao.upsertedAttributes[0].id)
            assertEquals(1.5, categoryDao.upsertedAttributes[0].weight, 0.001)
            assertEquals(2, categoryDao.upsertedAttributes[0].position)
        }

    @Test
    fun import_restoresItemsAndRatings() =
        runTest {
            val json =
                validImportJson(
                    categories =
                        """[{"name":"Cars","attributes":[{"id":"Speed","weight":1.0,"position":0,"type":"NUMBER"}]}]""",
                    items =
                        """[{"id":"Roadster","categoryName":"Cars","notes":"cool",""" +
                            """"createdAt":100,"updatedAt":200,"ratings":[{"attributeId":"Speed","score":9}]}]""",
                )

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
            val json = validImportJson()

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
                            AttributeEntity("Cars/Speed", "Cars", 1.0, 0),
                            AttributeEntity("Cars/Brakes", "Cars", 2.0, 1),
                        ),
                    ),
                )
            itemDao.state.value =
                listOf(
                    ItemWithRatings(
                        ItemEntity("Roadster", "weekend car", 100L, 200L),
                        listOf(
                            RatingEntity("Roadster", "Cars/Speed", 9),
                            RatingEntity("Roadster", "Cars/Brakes", 8),
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
            assertEquals("Cars/Speed", categoryDao.upsertedAttributes[0].id)
            assertEquals("Cars/Brakes", categoryDao.upsertedAttributes[1].id)
            assertEquals(1, itemDao.upsertedItems.size)
            assertEquals("Roadster", itemDao.upsertedItems[0].id)
            assertEquals("weekend car", itemDao.upsertedItems[0].notes)
            assertEquals(2, itemDao.upsertedRatings.size)
        }

    @Test
    fun import_emptyBackup_succeeds() =
        runTest {
            val json = validImportJson()

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

    @Test
    fun import_invalidJsonLeavesExistingDataIntact() =
        runTest {
            categoryDao.state.value =
                listOf(CategoryWithAttributes(CategoryEntity("Cars"), emptyList()))
            itemDao.state.value =
                listOf(ItemWithRatings(ItemEntity("OldItem"), emptyList(), emptyList()))

            runCatching { service.import("not json at all {{{") }

            assertTrue(categoryDao.deletedCategoryNames.isEmpty())
            assertTrue(itemDao.deletedItemIds.isEmpty())
            assertFalse(categoryDao.writeMethodCalled)
            assertFalse(itemDao.writeMethodCalled)
        }

    @Test
    fun import_unsupportedVersionLeavesExistingDataIntact() =
        runTest {
            categoryDao.state.value =
                listOf(CategoryWithAttributes(CategoryEntity("Cars"), emptyList()))
            itemDao.state.value =
                listOf(ItemWithRatings(ItemEntity("OldItem"), emptyList(), emptyList()))

            runCatching { service.import("""{"version":999,"categories":[],"items":[]}""") }

            assertTrue(categoryDao.deletedCategoryNames.isEmpty())
            assertTrue(itemDao.deletedItemIds.isEmpty())
            assertFalse(categoryDao.writeMethodCalled)
            assertFalse(itemDao.writeMethodCalled)
        }

    @Test
    fun import_runsPostImportMaintenanceAfterSuccess() =
        runTest {
            val json =
                """{"version":5,"app":"Juzgon","exportedAt":"2026-01-01T00:00:00Z",""" +
                    """"categories":[],"items":[],"scoreProfiles":[]}"""

            service.import(json)

            assertEquals(1, maintenanceRanCount)
        }

    @Test
    fun import_doesNotRunMaintenanceOnFailure() =
        runTest {
            runCatching { service.import("not json at all {{{") }

            assertEquals(0, maintenanceRanCount)
        }

    @Test
    fun import_v1BareAttributeIdsAreScopedWithCategoryName() =
        runTest {
            val json =
                validImportJson(
                    categories =
                        """[{"name":"Food","attributes":[{"id":"Taste","weight":1.0,"position":0,"type":"NUMBER"}]}]""",
                    items =
                        """[{"id":"Pizza","categoryName":"Food","notes":"","createdAt":1,"updatedAt":2,""" +
                            """"ratings":[{"attributeId":"Taste","score":7}]}]""",
                )

            service.import(json)

            assertEquals("Food/Taste", categoryDao.upsertedAttributes[0].id)
            assertEquals("Food/Taste", itemDao.upsertedRatings[0].attributeId)
        }

    @Test
    fun roundTrip_preservesItemValues() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("People"),
                        listOf(
                            AttributeEntity("People/Score", "People", type = "NUMBER"),
                            AttributeEntity("People/Nationality", "People", type = "NATIONALITY"),
                            AttributeEntity("People/Birthday", "People", type = "DATE"),
                        ),
                    ),
                )
            itemDao.state.value =
                listOf(
                    ItemWithRatings(
                        ItemEntity("Alice", "friend", 100L, 200L),
                        listOf(RatingEntity("Alice", "People/Score", 8)),
                        listOf(
                            ItemValueEntity("Alice", "People/Nationality", "US"),
                            ItemValueEntity("Alice", "People/Birthday", "1990-05-15"),
                        ),
                    ),
                )

            val json = service.export()

            categoryDao.reset()
            itemDao.reset()
            categoryDao.state.value = emptyList()
            itemDao.state.value = emptyList()

            service.import(json)

            assertEquals(2, itemDao.upsertedValues.size)
            val nationalityValue = itemDao.upsertedValues.find { it.attributeId == "People/Nationality" }
            val birthdayValue = itemDao.upsertedValues.find { it.attributeId == "People/Birthday" }
            assertEquals("US", nationalityValue?.valueText)
            assertEquals("1990-05-15", birthdayValue?.valueText)
            assertEquals("Alice", nationalityValue?.itemId)
        }

    @Test
    fun roundTrip_preservesMultiNationalityValues() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("People"),
                        listOf(
                            AttributeEntity("People/Nationality", "People", type = "NATIONALITY"),
                        ),
                    ),
                )
            itemDao.state.value =
                listOf(
                    ItemWithRatings(
                        ItemEntity("DualCitizen", "", 100L, 200L),
                        emptyList(),
                        listOf(
                            ItemValueEntity("DualCitizen", "People/Nationality", "BR,IT"),
                        ),
                    ),
                )

            val json = service.export()

            categoryDao.reset()
            itemDao.reset()
            categoryDao.state.value = emptyList()
            itemDao.state.value = emptyList()

            service.import(json)

            val nationalityValue = itemDao.upsertedValues.find { it.attributeId == "People/Nationality" }
            assertEquals("BR,IT", nationalityValue?.valueText)
        }

    @Test
    fun export_import_roundTrips_socialNetworkJsonValue() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("People"),
                        listOf(
                            AttributeEntity("People/Score", "People", type = "NUMBER"),
                            AttributeEntity("People/Socials", "People", type = "SOCIAL_NETWORK"),
                        ),
                    ),
                )
            val socialJson =
                """[{"platform":"INSTAGRAM","handle":"@testuser"},{"platform":"TIKTOK","handle":"@tiktoker"}]"""
            itemDao.state.value =
                listOf(
                    ItemWithRatings(
                        ItemEntity("Influencer", "", 100L, 200L),
                        listOf(RatingEntity("Influencer", "People/Score", 9)),
                        listOf(
                            ItemValueEntity("Influencer", "People/Socials", socialJson),
                        ),
                    ),
                )

            val json = service.export()

            categoryDao.reset()
            itemDao.reset()
            categoryDao.state.value = emptyList()
            itemDao.state.value = emptyList()

            service.import(json)

            val socialValue =
                itemDao.upsertedValues.find { it.attributeId == "People/Socials" }
            assertEquals(socialJson, socialValue?.valueText)
        }

    @Test
    fun export_excludesSoftDeletedValues() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("People"),
                        listOf(
                            AttributeEntity("People/Score", "People", type = "NUMBER"),
                            AttributeEntity("People/Nationality", "People", type = "NATIONALITY"),
                        ),
                    ),
                )
            itemDao.state.value =
                listOf(
                    ItemWithRatings(
                        ItemEntity("Alice", "", 100L, 200L),
                        listOf(RatingEntity("Alice", "People/Score", 8)),
                        listOf(
                            ItemValueEntity("Alice", "People/Nationality", "US"),
                            ItemValueEntity("Alice", "People/OldAttr", "gone", deletedAt = 1000L),
                        ),
                    ),
                )

            val json = JSONObject(service.export())
            val item = json.getJSONArray("items").getJSONObject(0)
            val values = item.getJSONArray("values")

            assertEquals(1, values.length())
            assertEquals("People/Nationality", values.getJSONObject(0).getString("attributeId"))
        }

    @Test
    fun roundTrip_preservesScoreProfiles() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("People"),
                        listOf(
                            AttributeEntity("People/Score", "People", type = "NUMBER"),
                            AttributeEntity("People/Charisma", "People", type = "NUMBER"),
                        ),
                    ),
                )
            itemDao.state.value = emptyList()
            scoreProfileDao.profiles.value =
                listOf(
                    ScoreProfileEntity("sp1", "People", "Default", 100L, 200L),
                )
            scoreProfileAttributeDao.profileAttributes.value =
                listOf(
                    ScoreProfileAttributeEntity("sp1", "People/Score", 0),
                    ScoreProfileAttributeEntity("sp1", "People/Charisma", 1),
                )

            val json = service.export()

            categoryDao.reset()
            itemDao.reset()
            scoreProfileDao.reset()
            scoreProfileAttributeDao.reset()
            categoryDao.state.value = emptyList()
            itemDao.state.value = emptyList()
            scoreProfileDao.profiles.value = emptyList()
            scoreProfileAttributeDao.profileAttributes.value = emptyList()

            service.import(json)

            assertEquals(1, scoreProfileDao.upsertedProfiles.size)
            val profile = scoreProfileDao.upsertedProfiles[0]
            assertEquals("sp1", profile.id)
            assertEquals("People", profile.categoryName)
            assertEquals("Default", profile.name)
            assertEquals(100L, profile.createdAt)
            assertEquals(200L, profile.updatedAt)

            assertEquals(2, scoreProfileAttributeDao.upsertedAttributes.size)
            val attr0 = scoreProfileAttributeDao.upsertedAttributes.find { it.position == 0 }!!
            val attr1 = scoreProfileAttributeDao.upsertedAttributes.find { it.position == 1 }!!
            assertEquals("People/Score", attr0.attributeId)
            assertEquals("People/Charisma", attr1.attributeId)
        }

    @Test
    fun import_clearsExistingScoreProfiles() =
        runTest {
            scoreProfileDao.profiles.value =
                listOf(
                    ScoreProfileEntity("old-profile", "Cars", "Old", 0L, 0L),
                )
            categoryDao.state.value = emptyList()
            itemDao.state.value = emptyList()

            val json = validImportJson()
            service.import(json)

            assertTrue(scoreProfileDao.deletedProfileIds.contains("old-profile"))
        }

    @Test
    fun import_v4BackupSucceeds() =
        runTest {
            val json =
                validImportJson(
                    categories =
                        """[{"name":"Cars","attributes":[""" +
                            """{"id":"Cars/Speed","weight":1.0,"position":0,"type":"NUMBER"}]}]""",
                    items =
                        """[{"id":"Jetta","categoryName":"Cars","notes":"","createdAt":1,"updatedAt":2,""" +
                            """"ratings":[{"attributeId":"Cars/Speed","score":7}],"values":[]}]""",
                )

            service.import(json)

            assertEquals(1, itemDao.upsertedItems.size)
            assertEquals("Jetta", itemDao.upsertedItems[0].id)
        }

    @Test
    fun roundTrip_preservesAttributeMetadata() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("People"),
                        listOf(
                            AttributeEntity(
                                id = "People/Nationality",
                                categoryName = "People",
                                weight = 1.0,
                                position = 0,
                                type = "NATIONALITY",
                                isRequired = false,
                                displayInDiamond = false,
                                diamondOrder = 3,
                                scoringDirection = null,
                            ),
                            AttributeEntity(
                                id = "People/Birthday",
                                categoryName = "People",
                                weight = 1.5,
                                position = 1,
                                type = "DATE",
                                isRequired = true,
                                displayInDiamond = true,
                                diamondOrder = null,
                                scoringDirection = "OLDER_IS_BETTER",
                            ),
                        ),
                    ),
                )
            itemDao.state.value = emptyList()

            val json = service.export()

            categoryDao.reset()
            itemDao.reset()
            categoryDao.state.value = emptyList()
            itemDao.state.value = emptyList()

            service.import(json)

            val nationality = categoryDao.upsertedAttributes.find { it.id == "People/Nationality" }!!
            assertEquals("NATIONALITY", nationality.type)
            assertEquals(false, nationality.isRequired)
            assertEquals(false, nationality.displayInDiamond)
            assertEquals(3, nationality.diamondOrder)

            val birthday = categoryDao.upsertedAttributes.find { it.id == "People/Birthday" }!!
            assertEquals("DATE", birthday.type)
            assertEquals(true, birthday.isRequired)
            assertEquals(true, birthday.displayInDiamond)
            assertEquals("OLDER_IS_BETTER", birthday.scoringDirection)
        }

    @Test
    fun roundTrip_preservesDateAttributeScoringDirection() =
        runTest {
            categoryDao.state.value =
                listOf(
                    CategoryWithAttributes(
                        CategoryEntity("People"),
                        listOf(
                            AttributeEntity(
                                id = "People/Birthday",
                                categoryName = "People",
                                type = "DATE",
                                weight = 1.0,
                                position = 0,
                                isRequired = true,
                                scoringDirection = "OLDER_IS_BETTER",
                            ),
                        ),
                    ),
                )

            val exported = service.export()
            service.import(exported)

            val birthday = categoryDao.upsertedAttributes.find { it.id == "People/Birthday" }!!
            assertEquals("DATE", birthday.type)
            assertEquals("OLDER_IS_BETTER", birthday.scoringDirection)
        }

    @Test
    fun export_handlesEmptyScoreProfilesGracefully() =
        runTest {
            categoryDao.state.value = emptyList()
            itemDao.state.value = emptyList()
            scoreProfileDao.profiles.value = emptyList()
            scoreProfileAttributeDao.profileAttributes.value = emptyList()

            val json = service.export()
            val parsed = JSONObject(json)

            assertTrue(parsed.getJSONArray("scoreProfiles").length() == 0)
        }

    // --- Helpers ---

    private fun validImportJson(
        version: Int = 5,
        categories: String = "[]",
        items: String = "[]",
        scoreProfiles: String = "[]",
    ): String =
        """{"version":$version,"app":"Juzgon","exportedAt":"2026-01-01T00:00:00Z",""" +
            """"categories":$categories,"items":$items,"scoreProfiles":$scoreProfiles}"""

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

        override suspend fun renameAttributeIdInScoreProfileAttributes(
            oldAttributeId: String,
            newAttributeId: String,
        ) = error("not used")

        override suspend fun countDependentsForAttributes(attributeIds: List<String>): Int = error("not used")
    }

    private class FakeItemDao : ItemDao {
        val state = MutableStateFlow<List<ItemWithRatings>>(emptyList())
        val upsertedItems = mutableListOf<ItemEntity>()
        val upsertedRatings = mutableListOf<RatingEntity>()
        val upsertedValues = mutableListOf<ItemValueEntity>()
        val deletedItemIds = mutableListOf<String>()
        var writeMethodCalled = false

        fun reset() {
            upsertedItems.clear()
            upsertedRatings.clear()
            upsertedValues.clear()
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
            upsertedValues += values
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
        val upsertedProfiles = mutableListOf<ScoreProfileEntity>()
        val deletedProfileIds = mutableListOf<String>()
        var writeMethodCalled = false

        fun reset() {
            upsertedProfiles.clear()
            deletedProfileIds.clear()
            writeMethodCalled = false
        }

        override fun observeAllProfiles(): Flow<List<ScoreProfileEntity>> = profiles

        override fun observeProfilesForCategory(categoryName: String): Flow<List<ScoreProfileEntity>> = error("not used")

        override fun observeProfile(id: String): Flow<ScoreProfileEntity?> = error("not used")

        override suspend fun upsertProfile(profile: ScoreProfileEntity) {
            writeMethodCalled = true
            upsertedProfiles += profile
        }

        override suspend fun deleteProfile(id: String) {
            writeMethodCalled = true
            deletedProfileIds += id
        }

        override suspend fun deleteOrphanedProfiles(): Int {
            writeMethodCalled = true
            return 0
        }
    }

    private class FakeItemPurgeDao : ItemPurgeDao {
        var writeMethodCalled = false

        fun reset() {
            writeMethodCalled = false
        }

        override suspend fun softDeleteItemValuesNotIn(
            itemId: String,
            keepAttributeIds: List<String>,
            deletedAt: Long,
        ) {
            writeMethodCalled = true
        }

        override suspend fun purgeOldSoftDeletedValues(cutoff: Long): Int {
            writeMethodCalled = true
            return 0
        }

        override suspend fun purgeOrphanedRatings(): Int {
            writeMethodCalled = true
            return 0
        }

        override suspend fun purgeOrphanedSoftDeletedValues(): Int {
            writeMethodCalled = true
            return 0
        }
    }

    @Suppress("MaxLineLength")
    private class FakeScoreProfileAttributeDao : ScoreProfileAttributeDao {
        val profileAttributes = MutableStateFlow<List<ScoreProfileAttributeEntity>>(emptyList())
        val upsertedAttributes = mutableListOf<ScoreProfileAttributeEntity>()
        var writeMethodCalled = false

        fun reset() {
            upsertedAttributes.clear()
            writeMethodCalled = false
        }

        override fun observeAllProfileAttributes(): Flow<List<ScoreProfileAttributeEntity>> = profileAttributes

        override suspend fun deleteAttributesForProfile(profileId: String) {
            writeMethodCalled = true
        }

        override suspend fun upsertProfileAttributes(attributes: List<ScoreProfileAttributeEntity>) {
            writeMethodCalled = true
            upsertedAttributes += attributes
        }

        override fun observeAttributesForProfile(profileId: String): Flow<List<ScoreProfileAttributeEntity>> = error("not used")

        override fun observeAttributesForCategory(categoryName: String): Flow<List<ScoreProfileAttributeEntity>> = error("not used")

        override suspend fun saveProfileWithAttributes(
            profile: ScoreProfileEntity,
            attributes: List<ScoreProfileAttributeEntity>,
        ) {
            writeMethodCalled = true
            upsertedAttributes += attributes
        }
    }
}
