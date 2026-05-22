package com.juzgon.feature.item

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemDetailModelsTest {
    @Test
    fun rankedAttributeCardsSortsScoresHighestFirst() {
        val cards =
            rankedAttributeCards(
                listOf(
                    ItemDetailAttributeScore(label = "Speed", score = 8),
                    ItemDetailAttributeScore(label = "Brakes", score = 10),
                    ItemDetailAttributeScore(label = "Control", score = 6),
                ),
            )

        assertEquals(listOf("Brakes", "Speed", "Control"), cards.map { it.label })
        assertEquals(listOf(1, 2, 3), cards.map { it.rank })
    }

    @Test
    fun rankedAttributeCardsMapsTopFiveRanksToDistinctSizeVariants() {
        val cards =
            rankedAttributeCards(
                listOf(
                    ItemDetailAttributeScore(label = "One", score = 10),
                    ItemDetailAttributeScore(label = "Two", score = 9),
                    ItemDetailAttributeScore(label = "Three", score = 8),
                    ItemDetailAttributeScore(label = "Four", score = 7),
                    ItemDetailAttributeScore(label = "Five", score = 6),
                    ItemDetailAttributeScore(label = "Six", score = 5),
                    ItemDetailAttributeScore(label = "Seven", score = 4),
                ),
            )

        assertEquals(
            listOf(
                AttributeRankSizeVariant.Rank1,
                AttributeRankSizeVariant.Rank2,
                AttributeRankSizeVariant.Rank3,
                AttributeRankSizeVariant.Rank4,
                AttributeRankSizeVariant.Rank5,
                AttributeRankSizeVariant.Standard,
                AttributeRankSizeVariant.Standard,
            ),
            cards.map { it.sizeVariant },
        )
    }

    @Test
    fun attributeRankSizeVariantUsesStandardVariantForInvalidRanks() {
        assertEquals(AttributeRankSizeVariant.Standard, attributeRankSizeVariant(0))
        assertEquals(AttributeRankSizeVariant.Standard, attributeRankSizeVariant(6))
    }

    @Test
    fun rankedAttributeCardsCalculatesProgressFromTenPointScale() {
        val cards =
            rankedAttributeCards(
                listOf(ItemDetailAttributeScore(label = "Speed", score = 8)),
            )

        assertEquals("8", cards.single().valueText)
        assertEquals("10", cards.single().maxText)
        assertEquals(80, cards.single().progressPercent)
        assertEquals(0.8f, cards.single().progressFraction)
    }

    @Test
    fun rankedAttributeCardsClampsDisplayValuesOutsideTenPointScale() {
        val cards =
            rankedAttributeCards(
                listOf(
                    ItemDetailAttributeScore(label = "Too high", score = 12),
                    ItemDetailAttributeScore(label = "Too low", score = -2),
                ),
            )

        assertEquals("10", cards[0].valueText)
        assertEquals(100, cards[0].progressPercent)
        assertEquals("0", cards[1].valueText)
        assertEquals(0, cards[1].progressPercent)
    }

    @Test
    fun rankedAttributeCardsOrdersTiesByLabel() {
        val cards =
            rankedAttributeCards(
                listOf(
                    ItemDetailAttributeScore(label = "Speed", score = 8),
                    ItemDetailAttributeScore(label = "Agility", score = 8),
                ),
            )

        assertEquals(listOf("Agility", "Speed"), cards.map { it.label })
    }
}
