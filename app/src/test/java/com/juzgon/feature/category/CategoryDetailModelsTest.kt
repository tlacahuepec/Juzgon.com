package com.juzgon.feature.category

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.usecase.CalculateProfileRankedItemsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryDetailModelsTest {
    private val speed = Attribute("Speed")
    private val brakes = Attribute("Brakes")
    private val photo = Attribute("Photo", type = AttributeType.IMAGE)
    private val nationality = Attribute("Nationality", type = AttributeType.NATIONALITY)

    private val carsCategory =
        Category(
            name = "Cars",
            attributes = listOf(speed, brakes, photo, nationality),
        )

    private fun rankedItem(
        id: String,
        score: Double = 8.0,
    ): RankedRatedItem =
        RankedRatedItem(
            item =
                RatedItem(
                    id = id,
                    scores =
                        listOf(
                            ScoreEntry(speed, 8),
                            ScoreEntry(brakes, 7),
                        ),
                ),
            aggregateScore = score,
        )

    @Test
    fun reduce_withNullCategory_returnsErrorState() {
        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Missing",
                category = null,
                rankedItems = emptyList(),
                sortOption = CategoryDetailSortOption.Score,
            )

        assertEquals("Missing", state.categoryName)
        assertEquals("Category not found", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun reduce_populatesItemsWithRankAndScoreText() {
        val items =
            listOf(
                rankedItem("Alpha", 9.2),
                rankedItem("Beta", 7.5),
            )

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Score,
            )

        assertEquals(2, state.items.size)
        assertEquals(1, state.items[0].rank)
        assertEquals("Alpha", state.items[0].id)
        assertEquals("9.2", state.items[0].averageScoreText)
        assertEquals(2, state.items[1].rank)
    }

    @Test
    fun reduce_appliesSearchFilter_caseInsensitive() {
        val items =
            listOf(
                rankedItem("Roadster"),
                rankedItem("Sedan"),
                rankedItem("Coupe"),
            )

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Score,
                searchQuery = "sed",
            )

        assertEquals(1, state.items.size)
        assertEquals("Sedan", state.items.single().id)
    }

    @Test
    fun reduce_includesAllSortOptions_plusAttributeOptions() {
        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = listOf(rankedItem("X")),
                sortOption = CategoryDetailSortOption.Score,
            )

        val labels = state.sortOptions.map { it.label }
        assertTrue(labels.contains("Score"))
        assertTrue(labels.contains("Name"))
        assertTrue(labels.any { it.contains("Speed") })
        assertTrue(labels.any { it.contains("Brakes") })
        // Image attributes are excluded from sort options
        assertFalse(labels.any { it.contains("Photo") })
    }

    @Test
    fun reduce_resolvesPrimaryImageValue_whenImageAttributePresent() {
        val itemWithPhoto =
            RankedRatedItem(
                item =
                    RatedItem(
                        id = "Roadster",
                        scores = emptyList(),
                        values =
                            listOf(
                                ItemAttributeValue(
                                    attribute = photo,
                                    value = """imgref:v1|id=1;src=content://images/roadster||""",
                                ),
                            ),
                    ),
                aggregateScore = 9.0,
            )

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = listOf(itemWithPhoto),
                sortOption = CategoryDetailSortOption.Score,
            )

        assertNotNull(state.items.single().imageValue)
        assertTrue(
            state.items
                .single()
                .imageValue!!
                .startsWith("content://"),
        )
    }

    @Test
    fun reduce_resolvesNationalityBadge_whenValidCodePresent() {
        val itemWithNat =
            RankedRatedItem(
                item =
                    RatedItem(
                        id = "GermanCar",
                        scores = emptyList(),
                        values = listOf(ItemAttributeValue(nationality, "DE")),
                    ),
                aggregateScore = 8.5,
            )

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = listOf(itemWithNat),
                sortOption = CategoryDetailSortOption.Score,
            )

        // DE should resolve to German flag emoji
        assertNotNull(state.items.single().nationalityBadge)
    }

    @Test
    fun reduce_usesProfileRanking_whenActiveProfileProvided() {
        val profile =
            ScoreProfile(
                id = "p1",
                name = "Speed Focused",
                categoryName = "Cars",
                includedAttributeIds = listOf(speed.id, brakes.id),
            )
        val useCase = CalculateProfileRankedItemsUseCase()

        val items =
            listOf(
                RankedRatedItem(
                    item =
                        RatedItem(
                            id = "Fast",
                            scores = listOf(ScoreEntry(speed, 10), ScoreEntry(brakes, 8)), // higher avg on the profile's included attrs
                        ),
                    aggregateScore = 7.5,
                ),
                RankedRatedItem(
                    item =
                        RatedItem(
                            id = "Balanced",
                            scores = listOf(ScoreEntry(speed, 7), ScoreEntry(brakes, 8)),
                        ),
                    aggregateScore = 7.6,
                ),
            )

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Score,
                profiles = listOf(profile),
                activeProfileId = profile.id,
                calculateProfileRankedItems = useCase,
            )

        // Profile ranking applied (subsets to includedAttributeIds with equal weights=1.0 per current model;
        // "Fast" now wins on the re-computed aggregate for the Speed Focused profile).
        // TDD extension during #231 final pass: documents actual post-model behavior (no per-profile weights).
        assertEquals("Fast", state.items.first().id)
        assertNotNull(state.activeProfileLabel)
        assertTrue(state.activeProfileLabel!!.contains("Speed Focused"))
    }

    @Test
    fun reduce_fallsBackToOriginalRanking_whenNoActiveProfile() {
        val items =
            listOf(
                rankedItem("Second", 8.0),
                rankedItem("First", 9.0),
            )

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Score,
            )

        // Default order by aggregate descending
        assertEquals("First", state.items[0].id)
        assertEquals("Second", state.items[1].id)
    }
}
