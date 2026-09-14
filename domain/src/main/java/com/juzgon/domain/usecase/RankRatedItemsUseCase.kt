package com.juzgon.domain.usecase

import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.RatingSystem
import com.juzgon.domain.ScoreAggregationService
import javax.inject.Inject

class RankRatedItemsUseCase
    @Inject
    constructor() {
        private val scoreAggregationService = ScoreAggregationService()

        operator fun invoke(
            ratingSystem: RatingSystem,
            ratedItems: List<RatedItem>,
        ): List<RankedRatedItem> =
            ratedItems
                .map { ratedItem ->
                    RankedRatedItem(
                        item = ratedItem,
                        aggregateScore = scoreAggregationService.calculateAggregate(ratingSystem, ratedItem),
                    )
                }.sortedWith(
                    compareByDescending<RankedRatedItem> { it.aggregateScore }
                        .thenBy { it.item.id },
                )
    }
