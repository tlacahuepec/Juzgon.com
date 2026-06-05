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
    fun reduce_resolvesNationalityBadge_multipleCodesShowsMultipleFlags() {
        val itemWithMultiNat =
            RankedRatedItem(
                item =
                    RatedItem(
                        id = "DualCitizen",
                        scores = emptyList(),
                        values = listOf(ItemAttributeValue(nationality, "BR,IT")),
                    ),
                aggregateScore = 8.5,
            )

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = listOf(itemWithMultiNat),
                sortOption = CategoryDetailSortOption.Score,
            )

        val badge = state.items.single().nationalityBadge
        assertNotNull(badge)
        assertTrue(badge!!.contains("\uD83C\uDDE7\uD83C\uDDF7"))
        assertTrue(badge.contains("\uD83C\uDDEE\uD83C\uDDF9"))
    }

    @Test
    fun reduce_resolvesNationalityBadge_overflowShowsPlusN() {
        val itemWith4Nat =
            RankedRatedItem(
                item =
                    RatedItem(
                        id = "QuadCitizen",
                        scores = emptyList(),
                        values = listOf(ItemAttributeValue(nationality, "BR,IT,US,FR")),
                    ),
                aggregateScore = 8.5,
            )

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = listOf(itemWith4Nat),
                sortOption = CategoryDetailSortOption.Score,
            )

        val badge = state.items.single().nationalityBadge
        assertNotNull(badge)
        assertTrue(badge!!.contains("+1"))
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

    // region Visible Range Tests

    private fun rankedItems(count: Int): List<RankedRatedItem> =
        (1..count).map { i ->
            rankedItem("Item$i", score = (count - i + 1).toDouble())
        }

    @Test
    fun reduce_withVisibleRangeTop10_limitsOutputTo10Items() {
        val items = rankedItems(15)

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Score,
                visibleRange = CategoryDetailVisibleRange.Top10,
            )

        assertEquals(10, state.items.size)
    }

    @Test
    fun reduce_withVisibleRangeTop20_limitsOutputTo20Items() {
        val items = rankedItems(25)

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Score,
                visibleRange = CategoryDetailVisibleRange.Top20,
            )

        assertEquals(20, state.items.size)
    }

    @Test
    fun reduce_withVisibleRangeAll_returnsAllItems() {
        val items = rankedItems(15)

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Score,
                visibleRange = CategoryDetailVisibleRange.All,
            )

        assertEquals(15, state.items.size)
    }

    @Test
    fun reduce_visibleRangePreservesRankFromFullSortedList() {
        val items = rankedItems(15)

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Score,
                visibleRange = CategoryDetailVisibleRange.Top10,
            )

        assertEquals(1, state.items.first().rank)
        assertEquals(10, state.items.last().rank)
    }

    @Test
    fun reduce_changingSortOptionChangesTop10Items() {
        val items = rankedItems(15)

        val byScore =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Score,
                visibleRange = CategoryDetailVisibleRange.Top10,
            )

        val byName =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Name,
                visibleRange = CategoryDetailVisibleRange.Top10,
            )

        // Different sort orders produce different top-10 item sets
        assertFalse(byScore.items.map { it.id } == byName.items.map { it.id })
    }

    @Test
    fun reduce_visibleRangeOptionsEmptyForSmallCatalogs() {
        val items = rankedItems(8)

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Score,
                visibleRange = CategoryDetailVisibleRange.Top10,
            )

        assertTrue(state.visibleRangeOptions.isEmpty())
    }

    @Test
    fun reduce_visibleRangeDoesNotMutateItemData() {
        val items = rankedItems(15)
        val originalScores = items.map { it.aggregateScore }

        CategoryDetailReducer.reduce(
            categoryName = "Cars",
            category = carsCategory,
            rankedItems = items,
            sortOption = CategoryDetailSortOption.Score,
            visibleRange = CategoryDetailVisibleRange.Top10,
        )

        assertEquals(originalScores, items.map { it.aggregateScore })
    }

    @Test
    fun reduce_searchFiltersWithinVisibleRange() {
        val items = rankedItems(15)

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Score,
                visibleRange = CategoryDetailVisibleRange.Top10,
                searchQuery = "Item15",
            )

        // Item15 has the lowest score so it ranks last (rank 15),
        // outside Top10 visible range → not found
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun reduce_sortByNationality_usesPrimaryNationalityName() {
        val items =
            listOf(
                RankedRatedItem(
                    item =
                        RatedItem(
                            id = "ItalianBrazilian",
                            scores = emptyList(),
                            values = listOf(ItemAttributeValue(nationality, "IT,BR")),
                        ),
                    aggregateScore = 8.0,
                ),
                RankedRatedItem(
                    item =
                        RatedItem(
                            id = "Argentine",
                            scores = emptyList(),
                            values = listOf(ItemAttributeValue(nationality, "AR")),
                        ),
                    aggregateScore = 7.0,
                ),
            )

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Attribute(nationality.id),
            )

        assertEquals("Argentine", state.items[0].id)
        assertEquals("ItalianBrazilian", state.items[1].id)
    }

    @Test
    fun reduce_sortBySkinType_usesFitzpatrickNaturalOrder() {
        val skinType = Attribute("People/Skin Type", type = AttributeType.SKIN_TYPE, isRequired = false)
        val category = Category(name = "People", attributes = listOf(speed, skinType))
        val items =
            listOf(
                rankedItemWithValue("TypeSix", skinType, "TYPE_VI"),
                rankedItemWithValue("TypeOne", skinType, "TYPE_I"),
                rankedItemWithValue("TypeThree", skinType, "TYPE_III"),
                rankedItemWithValue("Unknown", skinType, "TYPE_X"),
                rankedItemWithValue("Missing", skinType, ""),
            )

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "People",
                category = category,
                rankedItems = items,
                sortOption = CategoryDetailSortOption.Attribute(skinType.id),
            )

        assertEquals(
            listOf("TypeOne", "TypeThree", "TypeSix", "Missing", "Unknown"),
            state.items.map { it.id },
        )
        assertEquals("Type I, very light", state.items.first().metricValueText)
        assertEquals("#F6D7C3", state.items.first().metricColorHex)
    }

    @Test
    fun reduce_searchMatchesSkinTypeDisplayLabel() {
        val skinType = Attribute("People/Skin Type", type = AttributeType.SKIN_TYPE, isRequired = false)
        val category = Category(name = "People", attributes = listOf(speed, skinType))

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "People",
                category = category,
                rankedItems = listOf(rankedItemWithValue("Alice", skinType, "TYPE_I")),
                sortOption = CategoryDetailSortOption.Score,
                searchQuery = "very light",
            )

        assertEquals(listOf("Alice"), state.items.map { it.id })
    }

    @Test
    fun reduce_skinTypeFilterChipUsesNaturalOrderAndFiltersItems() {
        val skinType = Attribute("People/Skin Type", type = AttributeType.SKIN_TYPE, isRequired = false)
        val category = Category(name = "People", attributes = listOf(speed, skinType))

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "People",
                category = category,
                rankedItems =
                    listOf(
                        rankedItemWithValue("Alice", skinType, "TYPE_III"),
                        rankedItemWithValue("Bob", skinType, "TYPE_I"),
                        rankedItemWithValue("Cleo", skinType, "TYPE_VI"),
                    ),
                sortOption = CategoryDetailSortOption.Score,
                activeFilters = listOf(AttributeFilter.SkinType(skinType.id, selectedValues = setOf("TYPE_I"))),
            )

        val chip = state.filterChips.single { it.attributeId == skinType.id }
        assertTrue(chip.isActive)
        assertEquals("Type I, very light", chip.activeLabel)
        assertEquals(
            listOf("TYPE_I", "TYPE_III", "TYPE_VI"),
            chip.availableValues,
        )
        assertEquals(listOf("Bob"), state.items.map { it.id })
    }

    private fun rankedItemWithValue(
        id: String,
        attribute: Attribute,
        value: String,
    ): RankedRatedItem =
        RankedRatedItem(
            item =
                RatedItem(
                    id = id,
                    scores = listOf(ScoreEntry(speed, 8)),
                    values =
                        value
                            .takeIf { it.isNotBlank() }
                            ?.let { listOf(ItemAttributeValue(attribute, it)) }
                            ?: emptyList(),
                ),
            aggregateScore = 8.0,
        )

    // endregion

    // region social badge icons

    @Test
    fun reduce_resolvesSocialBadgeIcons_forItemWithSocialNetworks() {
        val socialAttr = Attribute("Socials", type = AttributeType.SOCIAL_NETWORK)
        val category = Category(name = "People", attributes = listOf(speed, socialAttr))
        val json =
            """[{"platform":"INSTAGRAM","handle":"@user1"},{"platform":"TIKTOK","handle":"@user2"}]"""
        val item =
            RankedRatedItem(
                item =
                    RatedItem(
                        id = "Influencer",
                        scores = listOf(ScoreEntry(speed, 9)),
                        values = listOf(ItemAttributeValue(socialAttr, json)),
                    ),
                aggregateScore = 9.0,
            )

        val state =
            CategoryDetailReducer.reduce(
                categoryName = "People",
                category = category,
                rankedItems = listOf(item),
                sortOption = CategoryDetailSortOption.Score,
            )

        assertEquals(
            2,
            state.items
                .single()
                .socialBadgeIcons
                .size,
        )
    }

    @Test
    fun reduce_socialBadgeIcons_emptyWhenNoSocialAttribute() {
        val state =
            CategoryDetailReducer.reduce(
                categoryName = "Cars",
                category = carsCategory,
                rankedItems = listOf(rankedItem("Fast")),
                sortOption = CategoryDetailSortOption.Score,
            )

        assertTrue(
            state.items
                .single()
                .socialBadgeIcons
                .isEmpty(),
        )
    }

    // endregion
}
