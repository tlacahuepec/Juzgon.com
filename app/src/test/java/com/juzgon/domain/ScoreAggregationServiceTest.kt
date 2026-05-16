package com.juzgon.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreAggregationServiceTest {
    private val service = ScoreAggregationService()

    @Test(expected = IllegalArgumentException::class)
    fun `rejects score lower than one`() {
        ScoreEntry(attribute = Attribute("quality"), score = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects score greater than ten`() {
        ScoreEntry(attribute = Attribute("quality"), score = 11)
    }

    @Test
    fun `calculates weighted aggregate correctly`() {
        val ratingSystem =
            RatingSystem(
                attributes =
                    listOf(
                        Attribute("quality", weight = 0.6),
                        Attribute("value", weight = 0.4),
                    ),
            )

        val ratedItem =
            RatedItem(
                id = "item-1",
                scores =
                    listOf(
                        ScoreEntry(Attribute("quality"), 8),
                        ScoreEntry(Attribute("value"), 6),
                    ),
            )

        val result = service.calculateAggregate(ratingSystem, ratedItem)

        assertEquals(7.2, result, 0.0001)
    }

    @Test
    fun `ignores missing attribute scores and normalizes by available weights`() {
        val ratingSystem =
            RatingSystem(
                attributes =
                    listOf(
                        Attribute("quality", weight = 0.6),
                        Attribute("value", weight = 0.4),
                    ),
            )

        val ratedItem =
            RatedItem(
                id = "item-2",
                scores = listOf(ScoreEntry(Attribute("quality"), 8)),
            )

        val result = service.calculateAggregate(ratingSystem, ratedItem)

        assertEquals(8.0, result, 0.0001)
    }

    @Test
    fun `returns zero when rated item has no scores matching rating system attributes`() {
        val ratingSystem =
            RatingSystem(
                attributes = listOf(Attribute("quality", weight = 1.0)),
            )
        val ratedItem =
            RatedItem(
                id = "item-4",
                scores = listOf(ScoreEntry(Attribute("value"), 7)),
            )

        val result = service.calculateAggregate(ratingSystem, ratedItem)

        assertEquals(0.0, result, 0.0001)
    }

    @Test
    fun `rounds aggregate according to configured decimals`() {
        val twoDecimalService = ScoreAggregationService(decimals = 2)
        val ratingSystem =
            RatingSystem(
                attributes =
                    listOf(
                        Attribute("quality", weight = 1.0),
                        Attribute("value", weight = 1.0),
                        Attribute("speed", weight = 1.0),
                    ),
            )
        val ratedItem =
            RatedItem(
                id = "item-5",
                scores =
                    listOf(
                        ScoreEntry(Attribute("quality"), 7),
                        ScoreEntry(Attribute("value"), 8),
                        ScoreEntry(Attribute("speed"), 8),
                    ),
            )

        val result = twoDecimalService.calculateAggregate(ratingSystem, ratedItem)

        assertEquals(7.67, result, 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects duplicate rating system attribute ids`() {
        RatingSystem(
            attributes =
                listOf(
                    Attribute("quality", weight = 0.6),
                    Attribute("quality", weight = 0.4),
                ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects duplicate rated item score attribute ids`() {
        RatedItem(
            id = "item-3",
            scores =
                listOf(
                    ScoreEntry(Attribute("quality"), 8),
                    ScoreEntry(Attribute("quality"), 6),
                ),
        )
    }
}
