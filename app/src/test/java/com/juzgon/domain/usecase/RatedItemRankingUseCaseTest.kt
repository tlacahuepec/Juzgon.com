package com.juzgon.domain.usecase

import com.juzgon.domain.Attribute
import com.juzgon.domain.RatedItem
import com.juzgon.domain.RatingSystem
import com.juzgon.domain.ScoreEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class RatedItemRankingUseCaseTest {
    private val useCase = RankRatedItemsUseCase()

    @Test
    fun `sorts higher aggregate scores first`() {
        val rankedItems =
            useCase(
                ratingSystem = foodRatingSystem(),
                ratedItems =
                    listOf(
                        ratedItem("item-low", taste = 6, service = 6),
                        ratedItem("item-high", taste = 10, service = 8),
                    ),
            )

        assertEquals(listOf("item-high", "item-low"), rankedItems.map { it.item.id })
        assertEquals(listOf(9.0, 6.0), rankedItems.map { it.aggregateScore })
    }

    @Test
    fun `sorts tied aggregate scores by item id`() {
        val rankedItems =
            useCase(
                ratingSystem = foodRatingSystem(),
                ratedItems =
                    listOf(
                        ratedItem("item-b", taste = 8, service = 8),
                        ratedItem("item-a", taste = 8, service = 8),
                    ),
            )

        assertEquals(listOf("item-a", "item-b"), rankedItems.map { it.item.id })
        assertEquals(listOf(8.0, 8.0), rankedItems.map { it.aggregateScore })
    }

    @Test
    fun `normalizes by available weights when category scores are missing`() {
        val rankedItems =
            useCase(
                ratingSystem = foodRatingSystem(),
                ratedItems =
                    listOf(
                        ratedItem("complete", taste = 8, service = 4),
                        ratedItem("partial", taste = 8),
                    ),
            )

        assertEquals(listOf("partial", "complete"), rankedItems.map { it.item.id })
        assertEquals(listOf(8.0, 6.0), rankedItems.map { it.aggregateScore })
    }

    private fun foodRatingSystem(): RatingSystem =
        RatingSystem(
            attributes =
                listOf(
                    Attribute(id = TASTE, weight = 1.0),
                    Attribute(id = SERVICE, weight = 1.0),
                ),
        )

    private fun ratedItem(
        id: String,
        taste: Int? = null,
        service: Int? = null,
    ): RatedItem =
        RatedItem(
            id = id,
            scores =
                buildList {
                    taste?.let { add(ScoreEntry(attribute = Attribute(TASTE), score = it)) }
                    service?.let { add(ScoreEntry(attribute = Attribute(SERVICE), score = it)) }
                },
        )

    private companion object {
        const val TASTE = "taste"
        const val SERVICE = "service"
    }
}
