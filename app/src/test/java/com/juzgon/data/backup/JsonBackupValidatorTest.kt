package com.juzgon.data.backup

import com.juzgon.domain.backup.BackupValidationResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Suppress("MaxLineLength")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JsonBackupValidatorTest {
    private lateinit var validator: JsonBackupValidator

    @Before
    fun setUp() {
        validator = JsonBackupValidator()
    }

    @Test
    fun validate_validMinimalPayload_returnsValid() {
        val json = validPayload()

        val result = validator.validate(json)

        assertTrue(result.isValid)
    }

    @Test
    fun validate_invalidJson_returnsError() {
        val result = validator.validate("not json {{{")

        assertFalse(result.isValid)
        assertContainsError(result, "Invalid JSON")
    }

    @Test
    fun validate_missingVersion_returnsError() {
        val json = """{"app":"Juzgon","exportedAt":"2026-01-01T00:00:00Z","categories":[],"items":[],"scoreProfiles":[]}"""

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "version")
    }

    @Test
    fun validate_unsupportedVersion_returnsError() {
        val json = validPayload(version = 999)

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "version")
    }

    @Test
    fun validate_missingApp_returnsError() {
        val json = """{"version":2,"exportedAt":"2026-01-01T00:00:00Z","categories":[],"items":[],"scoreProfiles":[]}"""

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "app")
    }

    @Test
    fun validate_missingExportedAt_returnsError() {
        val json = """{"version":2,"app":"Juzgon","categories":[],"items":[],"scoreProfiles":[]}"""

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "exportedAt")
    }

    @Test
    fun validate_missingCategoriesArray_returnsError() {
        val json = """{"version":2,"app":"Juzgon","exportedAt":"2026-01-01T00:00:00Z","items":[],"scoreProfiles":[]}"""

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "categories")
    }

    @Test
    fun validate_missingItemsArray_returnsError() {
        val json = """{"version":2,"app":"Juzgon","exportedAt":"2026-01-01T00:00:00Z","categories":[],"scoreProfiles":[]}"""

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "items")
    }

    @Test
    fun validate_missingScoreProfilesArray_returnsError() {
        val json = """{"version":2,"app":"Juzgon","exportedAt":"2026-01-01T00:00:00Z","categories":[],"items":[]}"""

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "scoreProfiles")
    }

    @Test
    fun validate_categoryMissingName_returnsError() {
        val json =
            validPayload(
                categories = """[{"attributes":[]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "name")
    }

    @Test
    fun validate_duplicateCategoryNames_returnsError() {
        val json =
            validPayload(
                categories = """[{"name":"Cars","attributes":[]},{"name":"Cars","attributes":[]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "Duplicate category")
    }

    @Test
    fun validate_attributeMissingId_returnsError() {
        val json =
            validPayload(
                categories =
                    """[{"name":"Cars","attributes":[{"weight":1.0,"position":0,"type":"NUMBER"}]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "id")
    }

    @Test
    fun validate_duplicateAttributeIdsInCategory_returnsError() {
        val json =
            validPayload(
                categories =
                    """[{"name":"Cars","attributes":[""" +
                        """{"id":"Speed","weight":1.0,"position":0,"type":"NUMBER"},""" +
                        """{"id":"Speed","weight":2.0,"position":1,"type":"NUMBER"}]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "Duplicate attribute")
    }

    @Test
    fun validate_itemMissingId_returnsError() {
        val json =
            validPayload(
                categories = """[{"name":"Cars","attributes":[]}]""",
                items = """[{"categoryName":"Cars","notes":"","createdAt":0,"updatedAt":0,"ratings":[],"values":[]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "id")
    }

    @Test
    fun validate_duplicateItemIds_returnsError() {
        val json =
            validPayload(
                categories = """[{"name":"Cars","attributes":[]}]""",
                items =
                    """[""" +
                        """{"id":"item1","categoryName":"Cars","notes":"","createdAt":0,"updatedAt":0,"ratings":[],"values":[]},""" +
                        """{"id":"item1","categoryName":"Cars","notes":"","createdAt":0,"updatedAt":0,"ratings":[],"values":[]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "Duplicate item")
    }

    @Test
    fun validate_itemReferencesNonexistentCategory_returnsError() {
        val json =
            validPayload(
                categories = """[{"name":"Cars","attributes":[]}]""",
                items =
                    """[{"id":"item1","categoryName":"Unknown","notes":"","createdAt":0,"updatedAt":0,"ratings":[],"values":[]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "category")
    }

    @Test
    fun validate_itemRatingReferencesNonexistentAttribute_returnsError() {
        val json =
            validPayload(
                categories =
                    """[{"name":"Cars","attributes":[{"id":"Speed","weight":1.0,"position":0,"type":"NUMBER"}]}]""",
                items =
                    """[{"id":"item1","categoryName":"Cars","notes":"","createdAt":0,"updatedAt":0,""" +
                        """"ratings":[{"attributeId":"Missing","score":5}],"values":[]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "attribute")
    }

    @Test
    fun validate_itemValueReferencesNonexistentAttribute_returnsError() {
        val json =
            validPayload(
                categories =
                    """[{"name":"Cars","attributes":[{"id":"Speed","weight":1.0,"position":0,"type":"NUMBER"}]}]""",
                items =
                    """[{"id":"item1","categoryName":"Cars","notes":"","createdAt":0,"updatedAt":0,""" +
                        """"ratings":[],"values":[{"attributeId":"Missing","value":"x"}]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "attribute")
    }

    @Test
    fun validate_scoreProfileReferencesNonexistentCategory_returnsError() {
        val json =
            validPayload(
                categories = """[{"name":"Cars","attributes":[]}]""",
                scoreProfiles =
                    """[{"id":"p1","categoryName":"Unknown","name":"P","createdAt":0,"updatedAt":0,"attributeIds":[]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "category")
    }

    @Test
    fun validate_scoreProfileReferencesNonexistentAttribute_returnsError() {
        val json =
            validPayload(
                categories =
                    """[{"name":"Cars","attributes":[{"id":"Speed","weight":1.0,"position":0,"type":"NUMBER"}]}]""",
                scoreProfiles =
                    """[{"id":"p1","categoryName":"Cars","name":"P","createdAt":0,"updatedAt":0,"attributeIds":["Missing"]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "attribute")
    }

    @Test
    fun validate_scoreProfileReferencesNonRankableAttribute_returnsError() {
        val json =
            validPayload(
                categories =
                    """[{"name":"Cars","attributes":[{"id":"Notes","weight":1.0,"position":0,"type":"NOTES"}]}]""",
                scoreProfiles =
                    """[{"id":"p1","categoryName":"Cars","name":"P","createdAt":0,"updatedAt":0,"attributeIds":["Notes"]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "rankable")
    }

    @Test
    fun validate_multipleViolations_returnsAllErrors() {
        val json =
            validPayload(
                categories = """[{"name":"Cars","attributes":[]},{"name":"Cars","attributes":[]}]""",
                items =
                    """[{"id":"item1","categoryName":"Unknown","notes":"","createdAt":0,"updatedAt":0,"ratings":[],"values":[]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertTrue(result.errors.size >= 2)
    }

    @Test
    fun validate_imageAssignmentReferencesNonexistentItem_returnsError() {
        val json =
            validPayload(
                extra = ""","imageAssignments":[{"itemId":"missing","uri":"file://img.png"}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "item")
    }

    private fun validPayload(
        version: Int = 2,
        categories: String = "[]",
        items: String = "[]",
        scoreProfiles: String = "[]",
        extra: String = "",
    ): String =
        """{"version":$version,"app":"Juzgon","exportedAt":"2026-01-01T00:00:00Z",""" +
            """"categories":$categories,"items":$items,"scoreProfiles":$scoreProfiles$extra}"""

    @Test
    fun validate_invalidAttributeType_returnsError() {
        val json =
            validPayload(
                categories =
                    """[{"name":"Cars","attributes":[{"id":"Speed","weight":1.0,"position":0,"type":"INVALID_TYPE"}]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "invalid type")
    }

    @Test
    fun validate_invalidScoringDirection_returnsError() {
        val json =
            validPayload(
                categories =
                    """[{"name":"Cars","attributes":[{"id":"Due","weight":1.0,"position":0,"type":"DATE","scoringDirection":"WRONG"}]}]""",
            )

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertContainsError(result, "invalid scoringDirection")
    }

    @Test
    fun validate_validSupportedTypes_returnsValid() {
        val json =
            validPayload(
                categories =
                    """[{"name":"Cars","attributes":[
                        {"id":"Speed","weight":1.0,"position":0,"type":"NUMBER"},
                        {"id":"Due","weight":1.0,"position":1,"type":"DATE","scoringDirection":"NEWER_IS_BETTER"},
                        {"id":"Notes","weight":1.0,"position":2,"type":"NOTES"},
                        {"id":"Photo","weight":1.0,"position":3,"type":"IMAGE"}
                    ]}]""",
            )

        val result = validator.validate(json)

        assertTrue(result.isValid)
    }

    private fun assertContainsError(
        result: BackupValidationResult,
        substring: String,
    ) {
        assertTrue(
            "Expected error containing '$substring' but got: ${result.errors}",
            result.errors.any { it.contains(substring, ignoreCase = true) },
        )
    }
}
