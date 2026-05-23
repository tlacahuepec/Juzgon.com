package com.juzgon.feature.item

import com.juzgon.domain.AttributeRankSnapshot
import com.juzgon.domain.AttributeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemDetailModelsTest {
    @Test
    fun formatAttributeValueFormatsBooleanAndDateValues() {
        assertEquals("Yes", formatAttributeValue(AttributeType.BOOLEAN, "true"))
        assertEquals("No", formatAttributeValue(AttributeType.BOOLEAN, "false"))
        assertEquals("Jan 2, 2026", formatAttributeValue(AttributeType.DATE, "2026-01-02"))
    }

    @Test
    fun formatAttributeValueLeavesUnknownDateTextUnchanged() {
        assertEquals("next friday", formatAttributeValue(AttributeType.DATE, "next friday"))
    }

    @Test
    fun itemAttributeDiamondChartPointsUsesIncludedNumericScoresInConfiguredOrder() {
        val points =
            itemAttributeDiamondChartPoints(
                listOf(
                    ItemDetailAttributeScore(label = "Speed", score = 8, diamondOrder = 2),
                    ItemDetailAttributeScore(label = "Brakes", score = 10, diamondOrder = 1),
                    ItemDetailAttributeScore(label = "Photo", score = 7, displayInDiamond = false),
                ),
            )

        assertEquals(listOf("Brakes", "Speed"), points.map { it.label })
        assertEquals(listOf(10, 8), points.map { it.value })
        assertEquals(listOf(1.0f, 0.8f), points.map { it.fraction })
    }

    @Test
    fun itemAttributeDiamondChartPointsClampsScoresToTenPointScale() {
        val points =
            itemAttributeDiamondChartPoints(
                listOf(
                    ItemDetailAttributeScore(label = "Too high", score = 12),
                    ItemDetailAttributeScore(label = "Too low", score = -1),
                    ItemDetailAttributeScore(label = "Normal", score = 5),
                ),
            )

        assertEquals(listOf(10, 5, 0), points.map { it.value }.sortedDescending())
    }

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

    @Test
    fun rankedAttributeCardsCalculatesRankAndValueMovementFromPreviousSnapshots() {
        val cards =
            rankedAttributeCards(
                attributeScores =
                    listOf(
                        ItemDetailAttributeScore(label = "Speed", score = 9),
                        ItemDetailAttributeScore(label = "Brakes", score = 8),
                        ItemDetailAttributeScore(label = "Control", score = 6),
                    ),
                previousSnapshots =
                    listOf(
                        AttributeRankSnapshot(
                            itemId = "Roadster",
                            capturedAt = 100L,
                            attributeId = "Speed",
                            value = 7,
                            rank = 2,
                        ),
                        AttributeRankSnapshot(
                            itemId = "Roadster",
                            capturedAt = 100L,
                            attributeId = "Brakes",
                            value = 9,
                            rank = 1,
                        ),
                        AttributeRankSnapshot(
                            itemId = "Roadster",
                            capturedAt = 100L,
                            attributeId = "Control",
                            value = 6,
                            rank = 3,
                        ),
                    ),
            )

        assertEquals(AttributeMovementDirection.Improved, cards[0].movement?.rank)
        assertEquals(AttributeMovementDirection.Improved, cards[0].movement?.value)
        assertEquals(AttributeMovementDirection.Declined, cards[1].movement?.rank)
        assertEquals(AttributeMovementDirection.Declined, cards[1].movement?.value)
        assertEquals(AttributeMovementDirection.Unchanged, cards[2].movement?.rank)
        assertEquals(AttributeMovementDirection.Unchanged, cards[2].movement?.value)
    }

    @Test
    fun rankedAttributeCardsOmitsMovementWithoutPreviousSnapshots() {
        val cards =
            rankedAttributeCards(
                listOf(ItemDetailAttributeScore(label = "Speed", score = 8)),
            )

        assertNull(cards.single().movement)
    }

    @Test
    fun rankedAttributeCardsOmitsMovementWhenPreviousAttributeIsMissing() {
        val cards =
            rankedAttributeCards(
                attributeScores = listOf(ItemDetailAttributeScore(label = "Speed", score = 8)),
                previousSnapshots =
                    listOf(
                        AttributeRankSnapshot(
                            itemId = "Roadster",
                            capturedAt = 100L,
                            attributeId = "Brakes",
                            value = 6,
                            rank = 1,
                        ),
                    ),
            )

        assertNull(cards.single().movement)
    }

    @Test
    fun latestPreviousAttributeRankSnapshotsUsesMostRecentSnapshotBeforeCurrentUpdate() {
        val snapshots =
            listOf(
                AttributeRankSnapshot(
                    itemId = "Roadster",
                    capturedAt = 100L,
                    attributeId = "Speed",
                    value = 6,
                    rank = 2,
                ),
                AttributeRankSnapshot(
                    itemId = "Roadster",
                    capturedAt = 200L,
                    attributeId = "Speed",
                    value = 7,
                    rank = 2,
                ),
                AttributeRankSnapshot(
                    itemId = "Roadster",
                    capturedAt = 300L,
                    attributeId = "Speed",
                    value = 9,
                    rank = 1,
                ),
            )

        val previousSnapshots =
            latestPreviousAttributeRankSnapshots(
                snapshots = snapshots,
                currentUpdatedAt = 300L,
            )

        assertEquals(listOf(200L), previousSnapshots.map { it.capturedAt }.distinct())
        assertEquals(7, previousSnapshots.single().value)
    }

    @Test
    fun formatAttributeValueForNationalityResolvesCodeToFlagPlusName() {
        assertEquals(
            "\uD83C\uDDF2\uD83C\uDDFD Mexican",
            formatAttributeValue(AttributeType.NATIONALITY, "MX"),
        )
    }

    @Test
    fun formatAttributeValueForNationalityWithUnknownCodeReturnsRawValue() {
        assertEquals("ZZZZZ", formatAttributeValue(AttributeType.NATIONALITY, "ZZZZZ"))
    }

    @Test
    fun formatAttributeValueForNationalityIsCaseInsensitive() {
        assertEquals(
            "\uD83C\uDDFA\uD83C\uDDF8 American",
            formatAttributeValue(AttributeType.NATIONALITY, "us"),
        )
    }
}
