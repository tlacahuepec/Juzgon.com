package com.juzgon.domain.usecase

import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.RatingSystem
import com.juzgon.domain.ScoreProfile
import javax.inject.Inject

class CalculateProfileRankedItemsUseCase
    @Inject
    constructor() {
        private val rankRatedItemsUseCase = RankRatedItemsUseCase()

        operator fun invoke(
            profile: ScoreProfile,
            ratingSystem: RatingSystem,
            ratedItems: List<RatedItem>,
        ): List<RankedRatedItem> {
            val includedIds = profile.includedAttributeIds.toSet()
            val profileAttributes =
                ratingSystem.attributes.filter { it.id in includedIds && it.isRankable }
            if (profileAttributes.isEmpty()) return emptyList()
            val profileRatingSystem = RatingSystem(profileAttributes)
            return rankRatedItemsUseCase(profileRatingSystem, ratedItems)
        }
    }
