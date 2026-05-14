package com.juzgon.domain

import kotlin.math.round

class ScoreAggregationService(
    private val decimals: Int = 1,
) {

    fun calculateAggregate(ratingSystem: RatingSystem, ratedItem: RatedItem): Double {
        val weightByAttribute = ratingSystem.attributes.associate { it.id to it.weight }
        val relevantScores = ratedItem.scores.filter { weightByAttribute.containsKey(it.attribute.id) }

        if (relevantScores.isEmpty()) {
            return 0.0
        }

        val weightedSum = relevantScores.sumOf { scoreEntry ->
            scoreEntry.score * (weightByAttribute[scoreEntry.attribute.id] ?: 0.0)
        }

        val totalWeight = relevantScores.sumOf { scoreEntry ->
            weightByAttribute[scoreEntry.attribute.id] ?: 0.0
        }

        if (totalWeight == 0.0) {
            return 0.0
        }

        return roundToDecimals(weightedSum / totalWeight)
    }

    private fun roundToDecimals(value: Double): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return round(value * factor) / factor
    }
}
