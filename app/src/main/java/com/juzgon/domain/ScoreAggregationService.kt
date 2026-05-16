package com.juzgon.domain

import kotlin.math.round

private const val DEFAULT_DECIMALS = 1
private const val ZERO = 0.0
private const val BASE_TEN = 10.0

class ScoreAggregationService(
    private val decimals: Int = DEFAULT_DECIMALS,
) {
    fun calculateAggregate(
        ratingSystem: RatingSystem,
        ratedItem: RatedItem,
    ): Double {
        val weightByAttribute = ratingSystem.attributes.associate { it.id to it.weight }
        val relevantScores =
            ratedItem.scores.filter { weightByAttribute.containsKey(it.attribute.id) }

        if (relevantScores.isEmpty()) return ZERO

        val weightedSum =
            relevantScores.sumOf { scoreEntry ->
                scoreEntry.score * (weightByAttribute[scoreEntry.attribute.id] ?: ZERO)
            }

        val totalWeight =
            relevantScores.sumOf { scoreEntry ->
                weightByAttribute[scoreEntry.attribute.id] ?: ZERO
            }

        return when {
            totalWeight == ZERO -> ZERO
            else -> roundToDecimals(weightedSum / totalWeight)
        }
    }

    private fun roundToDecimals(value: Double): Double {
        val factor = Math.pow(BASE_TEN, decimals.toDouble())
        return round(value * factor) / factor
    }
}
