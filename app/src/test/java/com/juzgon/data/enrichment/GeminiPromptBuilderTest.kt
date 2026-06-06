package com.juzgon.data.enrichment

import com.juzgon.domain.AttributeType
import com.juzgon.domain.CatalogType
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiPromptBuilderTest {
    private val builder = GeminiPromptBuilder()

    @Test
    fun build_includesCatalogDescription() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("Professional soccer players"))
    }

    @Test
    fun build_includesCatalogType() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("PERSON"))
    }

    @Test
    fun build_includesItemName() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("Lionel Messi"))
    }

    @Test
    fun build_includesExistingAttributes() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("Nationality"))
        assertTrue(prompt.contains("Argentina"))
    }

    @Test
    fun build_includesTargetAttributeLabel() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("Birth Date"))
    }

    @Test
    fun build_includesDateFormatInstructions() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("ISO-8601"))
        assertTrue(prompt.contains("YYYY-MM-DD"))
    }

    @Test
    fun build_includesJsonResponseFormat() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("\"status\""))
        assertTrue(prompt.contains("\"value\""))
        assertTrue(prompt.contains("\"confidence\""))
        assertTrue(prompt.contains("\"sources\""))
    }

    @Test
    fun build_includesRules() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("Do not guess"))
        assertTrue(prompt.contains("NOT_FOUND"))
        assertTrue(prompt.contains("CONFLICT"))
    }

    @Test
    fun build_handlesNullDescription() {
        val request = requestWithAllFields().copy(catalogDescription = null)

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Lionel Messi"))
    }

    @Test
    fun build_handlesNullCatalogType() {
        val request = requestWithAllFields().copy(catalogType = null)

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Lionel Messi"))
    }

    @Test
    fun build_handlesEmptyExistingAttributes() {
        val request = requestWithAllFields().copy(existingAttributes = emptyMap())

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Lionel Messi"))
        assertTrue(!prompt.contains("Known attributes"))
    }

    @Test
    fun build_numberType_showsNumericValue() {
        val request =
            requestWithAllFields().copy(
                targetAttributeType = AttributeType.NUMBER,
                targetAttributeLabel = "Height (cm)",
            )

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Numeric value"))
    }

    private fun requestWithAllFields() =
        AttributeEnrichmentRequest(
            catalogId = "cat-1",
            catalogDescription = "Professional soccer players",
            catalogType = CatalogType.PERSON,
            itemId = "item-1",
            itemName = "Lionel Messi",
            existingAttributes = mapOf("Nationality" to "Argentina"),
            targetAttributeKey = "birthDate",
            targetAttributeLabel = "Birth Date",
            targetAttributeType = AttributeType.DATE,
        )

    @Test
    fun build_personDateType_includesTrustedSites() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("famousbirthdays.com"))
        assertTrue(prompt.contains("sunoti.com"))
        assertTrue(prompt.contains("Reference sites"))
    }

    @Test
    fun build_nonPersonType_excludesTrustedSites() {
        val request = requestWithAllFields().copy(catalogType = CatalogType.MOVIE)

        val prompt = builder.build(request)

        assertFalse(prompt.contains("famousbirthdays.com"))
    }

    @Test
    fun build_personNonDateType_excludesTrustedSites() {
        val request = requestWithAllFields().copy(targetAttributeType = AttributeType.NUMBER)

        val prompt = builder.build(request)

        assertFalse(prompt.contains("famousbirthdays.com"))
    }

    @Test
    fun build_withInstagramAttribute_includesDisambiguation() {
        val request =
            requestWithAllFields().copy(
                existingAttributes =
                    mapOf(
                        "Nationality" to "AR",
                        "Instagram" to "https://instagram.com/leomessi",
                    ),
            )

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Disambiguation"))
        assertTrue(prompt.contains("instagram.com/leomessi"))
    }

    @Test
    fun build_withSocialMediaInValue_includesDisambiguation() {
        val request =
            requestWithAllFields().copy(
                existingAttributes =
                    mapOf(
                        "Nationality" to "AR",
                        "Profile" to "https://tiktok.com/@leomessi",
                    ),
            )

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Disambiguation"))
        assertTrue(prompt.contains("tiktok.com/@leomessi"))
    }

    @Test
    fun build_noSocialMedia_excludesDisambiguation() {
        val prompt = builder.build(requestWithAllFields())

        assertFalse(prompt.contains("Disambiguation"))
    }

    @Test
    fun build_personWithNationality_includesSearchHint() {
        val request =
            requestWithAllFields().copy(
                existingAttributes = mapOf("Nationality" to "BR"),
            )

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Search strategy"))
        assertTrue(prompt.contains("Portuguese"))
    }

    @Test
    fun build_personWithMultiNationality_usePrimaryCodeForSearchHint() {
        val request =
            requestWithAllFields().copy(
                existingAttributes = mapOf("Nationality" to "BR,IT"),
            )

        val prompt = builder.build(request)

        assertTrue(prompt.contains("Search strategy"))
        assertTrue(prompt.contains("Portuguese"))
    }

    @Test
    fun build_nonPersonWithNationality_excludesSearchHint() {
        val request =
            requestWithAllFields().copy(
                catalogType = CatalogType.MOVIE,
                existingAttributes = mapOf("Nationality" to "BR"),
            )

        val prompt = builder.build(request)

        assertFalse(prompt.contains("Search strategy"))
    }

    @Test
    fun build_invalidNationalityCode_excludesSearchHint() {
        val request =
            requestWithAllFields().copy(
                existingAttributes = mapOf("Nationality" to "Unknown"),
            )

        val prompt = builder.build(request)

        assertFalse(prompt.contains("Search strategy"))
    }

    @Test
    fun build_allEnhancements_correctOrder() {
        val request =
            requestWithAllFields().copy(
                existingAttributes =
                    mapOf(
                        "Nationality" to "BR",
                        "Instagram" to "https://instagram.com/user",
                    ),
            )

        val prompt = builder.build(request)

        val refIdx = prompt.indexOf("Reference sites")
        val disIdx = prompt.indexOf("Disambiguation")
        val searchIdx = prompt.indexOf("Search strategy")
        val targetIdx = prompt.indexOf("Target attribute")

        assertTrue(refIdx < disIdx)
        assertTrue(disIdx < searchIdx)
        assertTrue(searchIdx < targetIdx)
    }

    @Test
    fun build_includesCandidateValuesInResponseFormat() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("\"candidateValues\""))
        assertTrue(prompt.contains("candidateValues"))
    }

    @Test
    fun build_conflictRuleIncludesCandidateValuesInstruction() {
        val prompt = builder.build(requestWithAllFields())

        assertTrue(prompt.contains("populate candidateValues"))
    }

    @Test
    fun build_socialNetworkExpectedTypeDescribesJsonFormat() {
        val request =
            AttributeEnrichmentRequest(
                catalogId = "cat1",
                catalogDescription = "Influencers",
                catalogType = CatalogType.PERSON,
                itemId = "item1",
                itemName = "Test Person",
                existingAttributes = emptyMap(),
                targetAttributeKey = "social_links",
                targetAttributeLabel = "Social Links",
                targetAttributeType = AttributeType.SOCIAL_NETWORK,
            )
        val prompt = builder.build(request)

        assertTrue(prompt.contains("JSON"))
    }

    @Test
    fun build_socialMediaDisambiguationIncludesSocialNetworkEntries() {
        val request =
            AttributeEnrichmentRequest(
                catalogId = "cat1",
                catalogDescription = "Influencers",
                catalogType = CatalogType.PERSON,
                itemId = "item1",
                itemName = "Test Person",
                existingAttributes =
                    mapOf(
                        "Social Network" to
                            """[{"platform":"INSTAGRAM","handle":"@testuser"}]""",
                    ),
                targetAttributeKey = "birth_date",
                targetAttributeLabel = "Birth Date",
                targetAttributeType = AttributeType.DATE,
            )
        val prompt = builder.build(request)

        assertTrue(prompt.contains("Disambiguation"))
        assertTrue(prompt.contains("@testuser"))
    }
}
