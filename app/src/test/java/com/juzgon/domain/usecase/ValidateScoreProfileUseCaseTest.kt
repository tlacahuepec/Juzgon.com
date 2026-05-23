package com.juzgon.domain.usecase

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.ScoringDirection
import org.junit.Assert.assertThrows
import org.junit.Test

class ValidateScoreProfileUseCaseTest {
    private val useCase = ValidateScoreProfileUseCase()

    private val rankableAttributes =
        listOf(
            Attribute(id = "taste", type = AttributeType.NUMBER),
            Attribute(id = "texture", type = AttributeType.NUMBER),
            Attribute(id = "released", type = AttributeType.DATE, scoringDirection = ScoringDirection.NEWER_IS_BETTER),
        )

    private val validProfile =
        ScoreProfile(
            id = "p1",
            categoryName = "Food",
            name = "Physical Only",
            includedAttributeIds = listOf("taste", "texture"),
        )

    @Test
    fun `valid profile passes`() {
        useCase(
            profile = validProfile,
            existingProfiles = emptyList(),
            categoryAttributes = rankableAttributes,
        )
    }

    @Test
    fun `duplicate name in same category fails`() {
        val existing = validProfile.copy(id = "p2", name = "Physical Only")
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                profile = validProfile,
                existingProfiles = listOf(existing),
                categoryAttributes = rankableAttributes,
            )
        }
    }

    @Test
    fun `duplicate name check ignores own profile on update`() {
        useCase(
            profile = validProfile,
            existingProfiles = listOf(validProfile),
            categoryAttributes = rankableAttributes,
        )
    }

    @Test
    fun `non-existent attribute id fails`() {
        val profile = validProfile.copy(includedAttributeIds = listOf("taste", "nonexistent"))
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                profile = profile,
                existingProfiles = emptyList(),
                categoryAttributes = rankableAttributes,
            )
        }
    }

    @Test
    fun `non-rankable attribute id fails`() {
        val attributes = rankableAttributes + Attribute(id = "photo", type = AttributeType.IMAGE)
        val profile = validProfile.copy(includedAttributeIds = listOf("taste", "photo"))
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                profile = profile,
                existingProfiles = emptyList(),
                categoryAttributes = attributes,
            )
        }
    }

    @Test
    fun `date attribute without scoring direction fails`() {
        val attributes = rankableAttributes + Attribute(id = "birthday", type = AttributeType.DATE)
        val profile = validProfile.copy(includedAttributeIds = listOf("taste", "birthday"))
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                profile = profile,
                existingProfiles = emptyList(),
                categoryAttributes = attributes,
            )
        }
    }
}
