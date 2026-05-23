package com.juzgon.domain.usecase

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.RatedItem
import com.juzgon.domain.RatingSystem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.ScoringDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateProfileRankedItemsUseCaseTest {
    private val useCase = CalculateProfileRankedItemsUseCase()

    private val taste = Attribute(id = "taste", weight = 1.0)
    private val service = Attribute(id = "service", weight = 1.0)
    private val ambiance = Attribute(id = "ambiance", weight = 1.0)

    private val fullSystem = RatingSystem(listOf(taste, service, ambiance))

    private fun profile(vararg attributeIds: String): ScoreProfile =
        ScoreProfile(
            id = "p1",
            categoryName = "Food",
            name = "Test Profile",
            includedAttributeIds = attributeIds.toList(),
        )

    private fun item(
        id: String,
        scores: List<ScoreEntry>,
    ): RatedItem = RatedItem(id = id, scores = scores)

    @Test
    fun `profile scoring uses only included attribute ids`() {
        val items =
            listOf(
                item("A", listOf(ScoreEntry(taste, 10), ScoreEntry(service, 2), ScoreEntry(ambiance, 10))),
                item("B", listOf(ScoreEntry(taste, 8), ScoreEntry(service, 8), ScoreEntry(ambiance, 1))),
            )

        val result = useCase(profile("taste", "service"), fullSystem, items)

        assertEquals(2, result.size)
        assertEquals("B", result[0].item.id)
        assertEquals(8.0, result[0].aggregateScore, 0.01)
        assertEquals("A", result[1].item.id)
        assertEquals(6.0, result[1].aggregateScore, 0.01)
    }

    @Test
    fun `excluded attributes do not affect profile scores`() {
        val items =
            listOf(
                item("A", listOf(ScoreEntry(taste, 5), ScoreEntry(service, 10), ScoreEntry(ambiance, 10))),
            )

        val result = useCase(profile("taste"), fullSystem, items)

        assertEquals(1, result.size)
        assertEquals(5.0, result[0].aggregateScore, 0.01)
    }

    @Test
    fun `derived rank order flips based on profile`() {
        val items =
            listOf(
                item("A", listOf(ScoreEntry(taste, 10), ScoreEntry(service, 2))),
                item("B", listOf(ScoreEntry(taste, 4), ScoreEntry(service, 10))),
            )
        val system = RatingSystem(listOf(taste, service))

        val tasteResult = useCase(profile("taste"), system, items)
        assertEquals("A", tasteResult[0].item.id)
        assertEquals("B", tasteResult[1].item.id)

        val serviceResult = useCase(profile("service"), system, items)
        assertEquals("B", serviceResult[0].item.id)
        assertEquals("A", serviceResult[1].item.id)
    }

    @Test
    fun `profile scoring does not mutate input items`() {
        val originalScores = listOf(ScoreEntry(taste, 8), ScoreEntry(service, 6), ScoreEntry(ambiance, 4))
        val items = listOf(item("A", originalScores))

        useCase(profile("taste"), fullSystem, items)

        assertEquals(3, items[0].scores.size)
        assertEquals(8, items[0].scores[0].score)
        assertEquals(6, items[0].scores[1].score)
        assertEquals(4, items[0].scores[2].score)
    }

    @Test
    fun `missing values use existing fallback`() {
        val items =
            listOf(
                item("A", listOf(ScoreEntry(taste, 8))),
                item("B", listOf(ScoreEntry(taste, 6), ScoreEntry(service, 10))),
            )
        val system = RatingSystem(listOf(taste, service))

        val result = useCase(profile("taste", "service"), system, items)

        assertEquals("A", result[0].item.id)
        assertEquals(8.0, result[0].aggregateScore, 0.01)
        assertEquals("B", result[1].item.id)
        assertEquals(8.0, result[1].aggregateScore, 0.01)
    }

    @Test
    fun `non-rankable attributes ignored even if in profile`() {
        val system = RatingSystem(listOf(taste))
        val items = listOf(item("A", listOf(ScoreEntry(taste, 7))))

        val result = useCase(profile("taste", "photo"), system, items)

        assertEquals(1, result.size)
        assertEquals(7.0, result[0].aggregateScore, 0.01)
    }

    @Test
    fun `tie behavior is deterministic`() {
        val items =
            listOf(
                item("Z", listOf(ScoreEntry(taste, 5))),
                item("A", listOf(ScoreEntry(taste, 5))),
                item("M", listOf(ScoreEntry(taste, 5))),
            )
        val system = RatingSystem(listOf(taste))

        val result = useCase(profile("taste"), system, items)

        assertEquals("A", result[0].item.id)
        assertEquals("M", result[1].item.id)
        assertEquals("Z", result[2].item.id)
    }

    @Test
    fun `returns empty list when no included attributes are rankable`() {
        val system = RatingSystem(listOf(taste, service))

        val result = useCase(profile("nonexistent"), system, listOf(item("A", listOf(ScoreEntry(taste, 8)))))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `date attribute scores participate when included`() {
        val released =
            Attribute(
                id = "released",
                type = AttributeType.DATE,
                scoringDirection = ScoringDirection.NEWER_IS_BETTER,
            )
        val system = RatingSystem(listOf(taste, released))
        val items =
            listOf(
                item("A", listOf(ScoreEntry(taste, 5), ScoreEntry(released, 9))),
                item("B", listOf(ScoreEntry(taste, 8), ScoreEntry(released, 3))),
            )

        val result = useCase(profile("released"), system, items)

        assertEquals("A", result[0].item.id)
        assertEquals(9.0, result[0].aggregateScore, 0.01)
        assertEquals("B", result[1].item.id)
        assertEquals(3.0, result[1].aggregateScore, 0.01)
    }
}
