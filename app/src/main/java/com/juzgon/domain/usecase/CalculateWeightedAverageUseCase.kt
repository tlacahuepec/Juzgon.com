package com.juzgon.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class CalculateWeightedAverageUseCase
@Inject
constructor() {
    private companion object {
        const val MIN_VALUE = 1.0
        const val MAX_VALUE = 10.0
        const val MIN_WEIGHT = 0.0
        const val ZERO_TOTAL_WEIGHT = 0.0
        const val DECIMAL_SCALE = 2
    }

    operator fun invoke(
        values: List<Double>,
        weights: List<Double>,
    ): Double {
        require(values.isNotEmpty()) { "Values must not be empty." }
        require(values.size == weights.size) { "Values and weights must have the same size." }
        require(values.all { it in MIN_VALUE..MAX_VALUE }) {
            "Values must be within range $MIN_VALUE..$MAX_VALUE."
        }
        require(weights.all { it >= MIN_WEIGHT }) { "Weights must be non-negative." }

        val totalWeight = weights.sum()
        if (totalWeight == ZERO_TOTAL_WEIGHT) {
            return 0.0
        }

        val weightedSum = values.zip(weights)
            .sumOf { (value, weight) -> value * weight }
        return BigDecimal(weightedSum / totalWeight)
            .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP)
            .toDouble()
    }
}
