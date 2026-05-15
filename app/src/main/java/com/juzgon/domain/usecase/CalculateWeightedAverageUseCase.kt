package com.juzgon.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class CalculateWeightedAverageUseCase
    @Inject
    constructor() {
        operator fun invoke(values: List<Double>, weights: List<Double>): Double {
            require(values.isNotEmpty()) { "Values must not be empty." }
            require(values.size == weights.size) { "Values and weights must have the same size." }
            require(values.all { it in 1.0..10.0 }) { "Values must be within range 1.0..10.0." }
            require(weights.all { it >= 0.0 }) { "Weights must be non-negative." }

            val totalWeight = weights.sum()
            if (totalWeight == 0.0) {
                return 0.0
            }

            val weightedSum = values.zip(weights).sumOf { (value, weight) -> value * weight }
            return BigDecimal(weightedSum / totalWeight)
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
        }
    }
